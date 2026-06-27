# 设计审查报告（v5 r1）

## 审查结果
REJECTED

## 发现

### [一般] 1. 并发测试断言方式与字段可见性自相矛盾，导致测试代码无法编译

设计在 222-229 行"测试策略"和 254-258 行"并发测试设计参考"中给出并发测试的关键断言表达式：

```java
assertEquals(threadCount * callsPerThread, /* failures via reflection or package access */);
```

并附文：`并发计数的断言方式：因 usernameAttempts 字段为 private，并发测试可通过 package-private 访问或通过观察 isUsernameLocked("concurrent-user") 在达到阈值后的行为间接验证（推荐前者，更精确）`。

但同时在 37 行、46 行的类型定义中，`usernameAttempts` 与 `ipAttempts` 被显式声明为 `private final ConcurrentHashMap<...>`（不是 package-private）。`"package-private 访问"`这一表述在 Java 语义下无法成立——同一包内的测试类只能访问 package-private / protected / public 字段，不能访问 private 字段。结果是：

- 实施者按设计写出 `tracker.usernameAttempts.get("concurrent-user").failures()` 必然编译失败
- 注释 `推荐前者，更精确` 推荐的方案实际不可行
- 退而求其次的"间接观察 `isUsernameLocked`"只能验证 `failures >= 5`（阈值），无法精确断言 `failures == 1000`，因此说"更精确"也是错的

期望修正方向：三选一明确：
- (a) 把 `usernameAttempts` / `ipAttempts` 改为 package-private（牺牲少许封装换取可测性，与项目其它工具类的可见性约定一致后再权衡），并在测试策略里给出具体断言表达式
- (b) 实施阶段明确走反射（`Field f = LoginAttemptTracker.class.getDeclaredField("usernameAttempts"); f.setAccessible(true); ...`）并把反射代码写进测试模板
- (c) 用间接观察但要诚实标注精度：例如在 1000 次并发后多次调用 `recordUsernameFailure` 与 `isUsernameLocked` 组合验证，或仅断言 `failures >= 5`（即只能验证"至少达到锁定阈值"而非"精确等于调用次数"）

### [一般] 2. "过期重置"用例的描述无法真正验证惰性清除逻辑

设计在 213 行和 225 行的"过期重置（用例 12，方案 A）"中描述：

> 调用 `recordUsernameFailure("user")` → `Thread.sleep(150)` → 断言 `isUsernameLocked("user") == false`

但 `recordUsernameFailure("user")` 仅触发 1 次失败，`failures == 1`，而构造参数 `usernameThreshold = 5`。`isUsernameLocked` 在 `failures < threshold` 时本就直接返回 false，**与"是否过期"无关**——此断言通过并不能证明惰性清除生效，只能证明阈值逻辑生效。

期望修正方向：用例 12 应描述为：
1. 连续调用 `recordUsernameFailure("user")` 5 次，触发 `isUsernameLocked("user") == true` 的锁定状态（先验证前置条件成立）
2. `Thread.sleep(150)`（或更大的安全余量）
3. 断言 `isUsernameLocked("user") == false`（此时才验证惰性过期清除真正生效）
4. 可选：再调用一次 `recordUsernameFailure("user")`，断言 `isUsernameLocked("user") == false`（因新一次失败重置 `firstFailureTime`、`failures == 1`，验证"过期后重置为首败"语义）

### [一般] 3. 错误处理表中关于 ConcurrentHashMap null-key 行为的描述与 JDK 实际契约不符

设计在 111-113 行的错误处理表中断言：

| 方法 | key 为 null 时 |
|------|---------------|
| `isUsernameLocked` / `isIpLocked` | `ConcurrentHashMap.get` 允许 null key，返回 null 后该方法返回 false（与 ConcurrentHashMap 契约一致） |
| `clearUsername` / `clearIp` | `ConcurrentHashMap.remove` 允许 null key，no-op 返回 null |

但 `java.util.concurrent.ConcurrentHashMap` 明确不支持 null key——其 `get` / `remove` / `compute` / `computeIfAbsent` / `merge` 等方法均在 Javadoc 中声明 `Throws: NullPointerException - if the specified key is null`。`HashMap` 允许 null key，但 `ConcurrentHashMap` 与 `Hashtable` 一致不允许。

这导致：
- 表内 `recordXxxFailure`（走 `compute`）声明会抛 NPE，`isXxxLocked`（走 `get`）声明不抛 NPE，`clearXxx`（走 `remove`）声明不抛 NPE——三者在 `ConcurrentHashMap` 的实际行为上**应一致抛 NPE**，而设计给出了不一致的语义
- 设计 37 行的"key 为 null 时：ConcurrentHashMap.compute 抛出 NullPointerException，自然传播"已经与错误处理表自相矛盾

期望修正方向：明确以下之一并相应更新测试用例：
- (a) **保持 JDK 一致行为**：全部走 ConcurrentHashMap 的 NPE 自然传播，`isXxxLocked(null)` 和 `clearXxx(null)` 也抛 NPE；同时新增 `shouldThrowNpeWhenIsUsernameLockedWithNull` / `shouldThrowNpeWhenClearUsernameWithNull` 用例
- (b) **显式 null 守卫（与 ConcurrentHashMap 行为分歧）**：在三个方法入口加 `if (key == null) throw new NullPointerException("...")`，使语义显式可控；并把错误处理表里的错误描述统一改为"显式 NPE"

### [轻微] 1. 未知 key 用例未真正验证"不创建 entry"的契约

设计在 221 行"测试策略 1."中称：未知 key 用例"同时验证契约语义（`isLocked` 不创建 entry）"。但用例 1 `shouldReturnFalseForUnknownUsername` 与用例 2 `shouldReturnFalseForUnknownIp` 的验证点仅是"isXxxLocked 返回 false"，并未对 `usernameAttempts` / `ipAttempts` 的大小做断言。若实施者误用 `compute` 闭包创建空 entry，断言仍会通过。

期望修正方向：要么在用例 1/2 内增加 `assertEquals(0, tracker.usernameAttempts.size())`（同样受一般性问题 1 的可见性约束，需先解决字段访问问题）；要么在行为契约中明确放松：仅要求 `isXxxLocked` 不抛异常、返回 false，不再承诺"不创建 entry"（但这与现有设计文本冲突，不推荐）。

### [轻微] 2. 并发测试 `Thread.sleep(150)` 在慢 CI 上 50ms 余量偏紧

短窗口实例 `usernameLockDurationMs = 100L` + `Thread.sleep(150)` 仅留 50ms 边界。CI 偶发调度延迟可能造成 100ms 窗口尚未真正"过期"而 `System.currentTimeMillis()` 已进入下一窗口的概率虽低但非零。建议改为 `Thread.sleep(200)` 或 `Thread.sleep(300)`，将余量放大到 100-200ms。

### [轻微] 3. 未覆盖"过期后再失败重置 firstFailureTime"语义

行为契约 130 行写明 `firstFailureTime 一旦记录不再改变（仅在锁定到期惰性清除后由下次失败重新记录）`，但 16 个用例中没有任何一个显式覆盖该语义。可考虑在用例 12 中追加断言（见一般性问题 2 的修正建议）。

## 修改要求

### 对一般性问题 1（必须修正）
- 在测试策略章节明确并发测试断言方式，并给出完整可编译的代码模板
- 若选反射，把反射代码写进并发测试模板（参考 240-260 行的现有伪代码）
- 若选间接观察，删除"推荐前者，更精确"等不实描述

### 对一般性问题 2（必须修正）
- 修订用例 12 的验证点描述为：先触发锁定（5 次失败），再等待过期，再断言解锁
- 在 240-260 行的并发测试模板下方补充用例 12 的代码模板

### 对一般性问题 3（必须修正）
- 二选一并修订错误处理表（111-113 行）
- 若选保持 JDK 一致行为，行为契约章节（130-145 行）也要相应更新
- 在测试用例清单中考虑追加对应的 NPE 用例
