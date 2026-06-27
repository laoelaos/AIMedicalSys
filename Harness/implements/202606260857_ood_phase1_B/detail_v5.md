# 详细设计（v5 r2）

## 概述

实现 Stage 2 登录失败计数与账户锁定基础设施：新建 1 个生产类型 `LoginAttemptTracker` 及对应单元测试 `LoginAttemptTrackerTest`，位于公共模块的 `auth.login` 子包下。属于 OOD 4.1 防暴力破解三层防护的第二层（登录失败计数）与第三层（账户临时锁定）。

设计目标：
- 封装失败计数与锁定状态查询的纯内存状态管理逻辑，与 SlidingWindowCounter / InMemoryRateLimitGuard 同属 Stage 2 底层工具
- 提供可注入的测试构造器（package-private），使单元测试能在 100ms 短窗口内验证惰性过期逻辑
- 接口与 OOD 4.1、3.1.1 步骤 3/5/6/7/8 的契约严格一致，供后续 AuthServiceImpl 集成调用

不在范围：业务集成（Stage 3 AuthServiceImpl 集成锁定/清除逻辑）、Filter 层配置、Redis 持久化（Phase 2）。

## 文件规划

| 文件路径（相对 `AIMedical/backend/`） | 操作 | 职责 |
|---------|------|------|
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/login/LoginAttemptTracker.java` | 新建 | 登录失败计数与账户锁定工具类 |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/login/LoginAttemptTrackerTest.java` | 新建 | LoginAttemptTracker 单元测试（20 用例） |

## 类型定义

### LoginAttemptTracker

**形态**：public class（工具类，非 Spring Bean；后续 AuthModuleConfig 可显式注册为 @Bean）
**包路径**：`com.aimedical.modules.commonmodule.auth.login`
**职责**：登录失败计数与锁定状态封装，提供 username / IP 双维度记录、查询、清除能力。

```java
public class LoginAttemptTracker {

    private static final int USERNAME_THRESHOLD = 5;
    private static final long USERNAME_LOCK_DURATION_MS = 15 * 60 * 1000L;
    private static final int IP_THRESHOLD = 20;
    private static final long IP_LOCK_DURATION_MS = 30 * 60 * 1000L;

    private final ConcurrentHashMap<String, AttemptRecord> usernameAttempts;
    private final ConcurrentHashMap<String, AttemptRecord> ipAttempts;
    private final int usernameThreshold;
    private final long usernameLockDurationMs;
    private final int ipThreshold;
    private final long ipLockDurationMs;

    private static record AttemptRecord(int failures, long firstFailureTime) { }
}
```

**阈值常量定义**（`private static final`，保证生产封装性）：
- `USERNAME_THRESHOLD = 5`（username 维度失败计数阈值）
- `USERNAME_LOCK_DURATION_MS = 15 * 60 * 1000L`（15 分钟，username 维度锁定时长）
- `IP_THRESHOLD = 20`（IP 维度失败计数阈值）
- `IP_LOCK_DURATION_MS = 30 * 60 * 1000L`（30 分钟，IP 维度锁定时长）

**内部类型 AttemptRecord**：
- 形态：Java 21 `record`（不可变快照）
- 字段：`int failures`（累计失败次数）、`long firstFailureTime`（首次失败时间戳 epoch ms）
- 选择 `record` 关键字的理由：项目已使用 Java 21（OOD 9.1），record 形式简洁、语义明确（不可变）、自动提供 equals/hashCode/toString；与 compute 闭包内"new AttemptRecord(prev.failures+1, prev.firstFailureTime) 创建新快照"的不可变更新模式天然契合
- 可见性：`private static`（仅 LoginAttemptTracker 内部可见，不对外暴露）

**字段说明**：
- `usernameAttempts` / `ipAttempts` — 均为 `private final ConcurrentHashMap<String, AttemptRecord>`（与 SlidingWindowCounter / InMemoryRateLimitGuard 的字段可见性约定一致；测试通过反射访问，详见单元测试设计章节）
- `usernameThreshold` / `usernameLockDurationMs` / `ipThreshold` / `ipLockDurationMs` — 由构造器注入的阈值与锁定时长（无参构造器传入默认值）

**构造器**：
```java
public LoginAttemptTracker()
LoginAttemptTracker(int usernameThreshold, long usernameLockDurationMs, int ipThreshold, long ipLockDurationMs)
```

- `public LoginAttemptTracker()` — 无参构造器，委托到全参构造器并传入 4 个默认常量（保持生产 API 形状不变）
- `LoginAttemptTracker(int, long, int, long)` — **package-private** 测试构造器，接受任意阈值和锁定时长；同包测试类 `LoginAttemptTrackerTest` 可见，可创建 100ms 短窗口实例验证过期逻辑
- 选择 package-private（非 public）的原因：测试构造器为测试便利而存在，对外公开 API 仅暴露无参构造器；同包测试类可见但不破坏对外封装性
- 两个构造器均创建两个空的 `ConcurrentHashMap<String, AttemptRecord>` 实例

**公开接口**（与 OOD 4.1 接口契约一致）：

| 方法签名 | 返回 | 说明 |
|---------|------|------|
| `void recordUsernameFailure(String username)` | void | 递增 username 维度失败计数；首次失败时 `firstFailureTime = now` |
| `void recordIpFailure(String ip)` | void | 递增 IP 维度失败计数；首次失败时 `firstFailureTime = now` |
| `boolean isUsernameLocked(String username)` | boolean | 锁定条件：`failures >= USERNAME_THRESHOLD` 且 `now - firstFailureTime < USERNAME_LOCK_DURATION_MS`；惰性清除过期条目 |
| `boolean isIpLocked(String ip)` | boolean | 锁定条件：`failures >= IP_THRESHOLD` 且 `now - firstFailureTime < IP_LOCK_DURATION_MS`；惰性清除过期条目 |
| `void clearUsername(String username)` | void | 登录成功时清除该 username 计数（步骤 8） |
| `void clearIp(String ip)` | void | 登录成功时清除该 ip 计数（步骤 8） |

**方法行为约束**：

`recordUsernameFailure(String username)` / `recordIpFailure(String ip)`：
- 在 `ConcurrentHashMap.compute(key, (k, prev) -> { ... })` 闭包内完成原子操作
- `prev == null` → 创建新 `AttemptRecord(1, now)`
- `prev != null` → 创建新 `AttemptRecord(prev.failures() + 1, prev.firstFailureTime())`（首次失败时间戳不变）
- `now = System.currentTimeMillis()` 在闭包内获取，确保首次失败时间戳一致性

`isUsernameLocked(String username)` / `isIpLocked(String ip)`：
- 未知 key 行为：先通过 `map.get(key)` 读取（不在 compute 闭包内触发 entry 创建）；`get` 返回 null 时直接返回 false
- 已知 key：通过 `map.compute(key, ...)` 原子完成"读取 + 过期检查 + 惰性清除"
  - `prev == null`（极罕见竞态：get 后被另一线程清除） → 返回 false，不创建 entry（compute 闭包返回 null）
  - `now - prev.firstFailureTime() >= lockDurationMs`（已过期） → 返回 false，compute 闭包返回 null 触发 `ConcurrentHashMap` 自动移除该 key
  - 其他 → 返回 `prev.failures() >= threshold`（锁定判定），compute 闭包返回 prev 保持 entry 不变

`clearUsername(String username)` / `clearIp(String ip)`：
- 直接调用 `map.remove(key)`；ConcurrentHashMap 对不存在的 key 的 remove 是 no-op，无需额外判断

**类型关系**：独立工具类，无继承/实现关系；不引用 Spring 框架任何注解；不与 SlidingWindowCounter / InMemoryRateLimitGuard 共享状态或数据结构。

## 错误处理

`java.util.concurrent.ConcurrentHashMap` 明确不支持 null key（`get` / `remove` / `compute` 等方法 Javadoc 均声明 `Throws: NullPointerException - if the specified key is null`），与 `HashMap` 允许 null key 行为不同。LoginAttemptTracker 选择**保持 JDK 一致行为**：所有方法对 null key 抛出 NPE 自然传播，不引入显式 null 守卫（避免与 ConcurrentHashMap 实际行为产生认知分歧）。调用方负责传入非 null 的 username / IP 字符串。

| 方法 | 错误场景 | 处理方式 |
|------|---------|---------|
| `recordUsernameFailure` / `recordIpFailure` | `username` / `ip` 为 null | `ConcurrentHashMap.compute` 抛出 `NullPointerException`，自然传播 |
| `isUsernameLocked` / `isIpLocked` | `username` / `ip` 为 null | `ConcurrentHashMap.get` 抛出 `NullPointerException`，自然传播 |
| `clearUsername` / `clearIp` | `username` / `ip` 为 null | `ConcurrentHashMap.remove` 抛出 `NullPointerException`，自然传播 |
| `isUsernameLocked` / `isIpLocked` | key 不存在 | `map.get(key)` 返回 null → 返回 false，不创建 entry |
| `isUsernameLocked` / `isIpLocked` | key 存在但已过期 | compute 闭包返回 null 触发 ConcurrentHashMap 自动移除 → 返回 false |

LoginAttemptTracker 本身不引用 `BusinessException` / `GlobalErrorCode`，锁定状态仅以 boolean 形式返回；锁定消息生成（"30 分钟后重试" vs "15 分钟后重试"）由调用方 AuthServiceImpl 根据维度决定。

## 行为契约

### LoginAttemptTracker 整体契约
- **线程安全**：所有读写通过 `ConcurrentHashMap.compute()` 闭包完成原子操作；`recordFailure` 在闭包内创建新 `AttemptRecord`（不可变快照）
- **惰性清除**：`isXxxLocked()` 中检测 `now - firstFailureTime >= lockDurationMs` 时，在 compute 闭包内返回 null（ConcurrentHashMap.compute 收到 null 返回值会移除该 key）；不引入后台清理线程
- **状态隔离**：username 维度与 IP 维度使用独立的两个 ConcurrentHashMap，互不影响
- **不抛业务异常**：LoginAttemptTracker 仅返回 boolean 状态；锁定消息生成由调用方负责
- **null key 一致性**：所有公开方法对 null key 行为一致（NPE 自然传播），不依赖 `HashMap` 允许 null key 的语义

### recordUsernameFailure(String username)
- **前置条件**：`username` 非 null（否则 NPE 自然传播，与 ConcurrentHashMap 契约一致）
- **后置条件**：username 维度 map 中该 username 对应的 failures 递增 1；首次失败时 firstFailureTime 记录为 now
- **不变量**：firstFailureTime 一旦记录不再改变（仅在锁定到期惰性清除后由下次失败重新记录）

### recordIpFailure(String ip)
- 同上，作用于 ipAttempts

### isUsernameLocked(String username)
- **前置条件**：`username` 非 null（否则 NPE 自然传播）
- **后置条件**：
  - 未知 username → 返回 false，不创建 entry
  - username 存在但 `failures < USERNAME_THRESHOLD` → 返回 false
  - username 存在且 `failures >= USERNAME_THRESHOLD` 且 `now - firstFailureTime < USERNAME_LOCK_DURATION_MS` → 返回 true（锁定中）
  - username 存在但已过期（`now - firstFailureTime >= USERNAME_LOCK_DURATION_MS`） → 返回 false，且惰性清除该 entry
- **副作用**：过期条目在检查时被惰性移除（无需后台线程）

### isIpLocked(String ip)
- 同上，作用于 ipAttempts 与 IP 维度阈值/时长

### clearUsername(String username)
- **前置条件**：`username` 非 null（否则 NPE 自然传播）
- **后置条件**：username 维度 map 中移除该 username 对应的 entry；不存在的 key 为 no-op
- **调用方**：AuthServiceImpl 步骤 8（登录成功时调用）

### clearIp(String ip)
- 同上，作用于 ipAttempts

### 并发场景
- 多线程并发 `recordUsernameFailure(username)`：compute 闭包串行化执行，最终 failures 等于总调用次数
- 多线程并发 `isUsernameLocked(username)` 与 `recordUsernameFailure(username)`：compute 闭包保证"读+判定+清除"的原子性，不会出现读到的 failures 与实际状态不一致

## 依赖关系

### 新增依赖（生产代码）
- `java.util.concurrent.ConcurrentHashMap` — 数据结构
- `java.lang.System` / `System.currentTimeMillis()` — 时间戳来源

### 内部新增类型
- `LoginAttemptTracker.AttemptRecord`（private static record）— 失败计数快照

### 已有依赖（不变）
- 无外部依赖；不引用 `BusinessException` / `GlobalErrorCode`（由调用方 AuthServiceImpl 抛出）
- 与 `SlidingWindowCounter` / `InMemoryRateLimitGuard` 无共享状态或数据结构

### 测试代码新增依赖
- `org.junit.jupiter.api.Test`
- `java.lang.reflect.Field` — 反射访问 `private final` map 字段以精确断言并发计数
- `java.util.concurrent.CountDownLatch` / `CyclicBarrier`（并发测试）
- `java.util.concurrent.atomic.AtomicInteger`（并发测试断言）

### 暴露给后续任务的公开接口
- `LoginAttemptTracker()` 无参构造器 — 供 AuthModuleConfig 显式注册为 @Bean
- `recordUsernameFailure(String)` / `recordIpFailure(String)` — 供 AuthServiceImpl 步骤 5/6/7 调用
- `isUsernameLocked(String)` / `isIpLocked(String)` — 供 AuthServiceImpl 步骤 3 调用
- `clearUsername(String)` / `clearIp(String)` — 供 AuthServiceImpl 步骤 8 调用
- **package-private 测试构造器** `LoginAttemptTracker(int, long, int, long)` — 仅供同包 `LoginAttemptTrackerTest` 使用

### 不在此范围
- AuthServiceImpl 集成调用（Stage 3 任务 3.6）
- GlobalRateLimitFilter（任务 2.2）— 不依赖 LoginAttemptTracker
- TokenBlacklist（任务 2.9）— 不依赖 LoginAttemptTracker
- Redis 持久化方案（Phase 2）

## 单元测试设计

### LoginAttemptTrackerTest

**形态**：class（JUnit 5），不引入 Spring 上下文
**包路径**：`com.aimedical.modules.commonmodule.auth.login.LoginAttemptTrackerTest`
**注解**：无（package-private class，与 InMemoryRateLimitGuardTest / SlidingWindowCounterTest 风格一致）

**测试方法清单**（20 用例）：

| # | 测试方法 | 覆盖维度 | 验证点 |
|---|---------|---------|--------|
| 1 | `shouldReturnFalseForUnknownUsername` | 契约 - 未知 key | 从未失败过的 username，`isUsernameLocked` 返回 false；通过反射断言 `usernameAttempts.size() == 0` 确认未创建 entry |
| 2 | `shouldReturnFalseForUnknownIp` | 契约 - 未知 key | 从未失败过的 ip，`isIpLocked` 返回 false；通过反射断言 `ipAttempts.size() == 0` |
| 3 | `shouldNotLockUsernameBelowThreshold` | 正常路径 | 4 次失败后 `isUsernameLocked=false` |
| 4 | `shouldLockUsernameWhenThresholdReached` | 锁定触发 | 第 5 次失败后 `isUsernameLocked=true` |
| 5 | `shouldRemainLockedAfterThreshold` | 锁定持续 | 第 6/7 次检查仍返回 true（持续锁定状态） |
| 6 | `shouldClearUsernameAfterSuccess` | 清除逻辑 | `clearUsername` 后 `isUsernameLocked=false`，可重新计数 |
| 7 | `shouldNotLockIpBelowThreshold` | IP 维度 | 19 次失败后 `isIpLocked=false` |
| 8 | `shouldLockIpWhenThresholdReached` | IP 维度锁定 | 第 20 次失败后 `isIpLocked=true` |
| 9 | `shouldClearIpAfterSuccess` | IP 维度清除 | `clearIp` 后 `isIpLocked=false` |
| 10 | `shouldMaintainIndependentUsernames` | 状态交互 | 不同 username 计数独立 |
| 11 | `shouldMaintainIndependentIps` | 状态交互 | 不同 ip 计数独立 |
| 12 | `shouldUnlockAfterLockDurationExpiry` | 过期重置（短窗口） | (a) 5 次失败后断言 `isUsernameLocked==true`（前置条件）；(b) `Thread.sleep(300)`；(c) 断言 `isUsernameLocked==false`（验证惰性清除生效）；(d) 再调用一次 `recordUsernameFailure`，断言 `isUsernameLocked==false`（验证过期后 firstFailureTime 已被重置） |
| 13 | `shouldResetFirstFailureTimeAfterExpiry` | 过期后再失败重置 | 通过反射断言过期后再失败时 map 中的 firstFailureTime 是新时间戳（验证"过期后下次失败重新记录"语义） |
| 14 | `shouldThrowNpeWhenRecordUsernameFailureWithNull` | 错误路径 | `recordUsernameFailure(null)` 抛 NPE |
| 15 | `shouldThrowNpeWhenRecordIpFailureWithNull` | 错误路径 | `recordIpFailure(null)` 抛 NPE |
| 16 | `shouldThrowNpeWhenIsUsernameLockedWithNull` | 错误路径 | `isUsernameLocked(null)` 抛 NPE |
| 17 | `shouldThrowNpeWhenIsIpLockedWithNull` | 错误路径 | `isIpLocked(null)` 抛 NPE |
| 18 | `shouldThrowNpeWhenClearUsernameWithNull` | 错误路径 | `clearUsername(null)` 抛 NPE |
| 19 | `shouldHandleConcurrentRecordUsernameFailure` | 并发安全 | 多线程并发 `recordUsernameFailure` 同一 username，通过反射读取 `usernameAttempts.get("concurrent-user").failures()` 精确断言 == 总调用次数 |
| 20 | `shouldHandleConcurrentRecordIpFailure` | 并发安全 | 多线程并发 `recordIpFailure` 同一 ip，通过反射读取 `ipAttempts.get("concurrent-ip").failures()` 精确断言 == 总调用次数 |

**测试策略**：

1. **未知 key 用例（用例 1/2）**：仅创建 `LoginAttemptTracker`，不调用 `recordFailure`，断言 `isXxxLocked("nonexistent-key") == false`；通过反射 helper `readMapSize(tracker, "usernameAttempts")` 进一步断言 map size 为 0（精确验证"isLocked 不创建 entry"的契约）。

2. **正常路径与阈值触发（用例 3-9）**：使用 `new LoginAttemptTracker()` 默认实例，循环调用 `recordXxxFailure` 至阈值附近，断言 `isXxxLocked` 返回值。

3. **过期重置（用例 12，方案 A）**：
   - 步骤 (a)：使用 package-private 测试构造器 `new LoginAttemptTracker(5, 100L, 20, 100L)` 创建 100ms 短窗口实例；连续调用 `recordUsernameFailure("user")` 5 次，断言 `isUsernameLocked("user") == true`（前置条件成立）
   - 步骤 (b)：`Thread.sleep(300)`（100ms 窗口 + 200ms 安全余量，避免慢 CI 偶发调度延迟导致边界条件）
   - 步骤 (c)：断言 `isUsernameLocked("user") == false`（**关键断言**：失败次数 = 5，仍 >= 阈值 5，但 firstFailureTime 距 now 已超 100ms → 锁定解除，证明惰性清除路径生效）
   - 步骤 (d)：再调用一次 `recordUsernameFailure("user")`，断言 `isUsernameLocked("user") == false`（此时 failures = 6 >= 5，但 firstFailureTime 已被本次失败重置为当前时刻 → 锁定应被解除，验证"过期后下次失败重新记录 firstFailureTime"语义）

4. **过期后 firstFailureTime 重置（用例 13）**：配合用例 12 的步骤 (d)，通过反射 helper `readFirstFailureTime(tracker, "usernameAttempts", "user")` 断言此时 entry 的 firstFailureTime 大于原 firstFailureTime（即被本次失败覆盖），精确验证"过期后重新记录首败时间"语义。

5. **错误路径（用例 14-18）**：使用 `assertThrows(NullPointerException.class, () -> tracker.method(null))` 验证 null key 在所有公开方法上一致抛 NPE。

6. **并发安全（用例 19/20）**：使用 `CountDownLatch` + `CyclicBarrier` 确保线程同时启动；通过反射 helper `readFailures(tracker, "usernameAttempts", "concurrent-user")` 精确读取 map entry 的 failures 值，断言等于 `threadCount * callsPerThread`。

**反射访问方案**：
- 生产类字段保持 `private final`（与 SlidingWindowCounter / InMemoryRateLimitGuard 字段可见性约定一致，不为可测性牺牲生产封装性）
- 测试类在同包内通过 `Field.setAccessible(true)` 访问 `private` 字段；Java 17+ 对 `setAccessible` 仅限制 module 访问，同 package reflective access 仍受支持
- 反射 helper 集中封装在测试类的私有静态方法中，对外暴露简洁的 `int readFailures(...)` / `int readMapSize(...)` / `long readFirstFailureTime(...)` 三个方法

**已排除方案**：
- 方案 A'（把 `usernameAttempts` / `ipAttempts` 改为 package-private）— 破坏与 SlidingWindowCounter / InMemoryRateLimitGuard 的字段可见性约定一致性
- 方案 B（`protected static` 常量）— 破坏封装性
- 方案 C（反射修改 final 常量）— JDK 17+ final 字段反射受限，且依赖实现细节
- 方案 D（实际等待 15+ 分钟）— CI 不可接受
- Clock 抽象注入 — 对当前任务属于过度设计；短窗口测试构造器已足以验证过期逻辑

**测试框架**：JUnit 5（与 `InMemoryRateLimitGuardTest` / `SlidingWindowCounterTest` 风格一致），不 mock 并发工具，不引入 Spring 上下文，测试与生产类同包以直接使用 package-private 测试构造器。

**反射 helper 代码模板**：
```java
private static int readMapSize(LoginAttemptTracker tracker, String fieldName) throws Exception {
    Field f = LoginAttemptTracker.class.getDeclaredField(fieldName);
    f.setAccessible(true);
    return ((ConcurrentHashMap<?, ?>) f.get(tracker)).size();
}

private static int readFailures(LoginAttemptTracker tracker, String fieldName, String key) throws Exception {
    Object record = readRecord(tracker, fieldName, key);
    return (int) record.getClass().getMethod("failures").invoke(record);
}

private static long readFirstFailureTime(LoginAttemptTracker tracker, String fieldName, String key) throws Exception {
    Object record = readRecord(tracker, fieldName, key);
    return (long) record.getClass().getMethod("firstFailureTime").invoke(record);
}

private static Object readRecord(LoginAttemptTracker tracker, String fieldName, String key) throws Exception {
    Field f = LoginAttemptTracker.class.getDeclaredField(fieldName);
    f.setAccessible(true);
    return ((ConcurrentHashMap<String, ?>) f.get(tracker)).get(key);
}
```

**并发测试代码模板**：
```java
void shouldHandleConcurrentRecordUsernameFailure() throws Exception {
    int threadCount = 10;
    int callsPerThread = 100;
    LoginAttemptTracker tracker = new LoginAttemptTracker();
    CountDownLatch latch = new CountDownLatch(threadCount);
    CyclicBarrier barrier = new CyclicBarrier(threadCount);

    for (int i = 0; i < threadCount; i++) {
        new Thread(() -> {
            try { barrier.await(); } catch (Exception e) { Thread.currentThread().interrupt(); }
            for (int j = 0; j < callsPerThread; j++) {
                tracker.recordUsernameFailure("concurrent-user");
            }
            latch.countDown();
        }).start();
    }
    latch.await();
    assertEquals(threadCount * callsPerThread, readFailures(tracker, "usernameAttempts", "concurrent-user"));
}
```

**过期重置测试代码模板**：
```java
void shouldUnlockAfterLockDurationExpiry() throws Exception {
    LoginAttemptTracker tracker = new LoginAttemptTracker(5, 100L, 20, 100L);
    String key = "user";
    for (int i = 0; i < 5; i++) {
        tracker.recordUsernameFailure(key);
    }
    assertTrue(tracker.isUsernameLocked(key));      // 前置条件：锁定成立
    long originalFirstFailureTime = readFirstFailureTime(tracker, "usernameAttempts", key);
    Thread.sleep(300);                              // 100ms 窗口 + 200ms 安全余量
    assertFalse(tracker.isUsernameLocked(key));     // 关键断言：惰性过期清除生效
    tracker.recordUsernameFailure(key);             // 过期后再失败
    assertFalse(tracker.isUsernameLocked(key));     // 验证 firstFailureTime 已被本次失败重置
    long newFirstFailureTime = readFirstFailureTime(tracker, "usernameAttempts", key);
    assertTrue(newFirstFailureTime > originalFirstFailureTime);  // 验证首败时间戳确实被覆盖
}
```

## 修订说明（v5 r2)

| 审查意见 | 修改措施 |
|---------|---------|
| [一般] 1. 并发测试断言方式与字段可见性自相矛盾（`usernameAttempts` 声明为 `private final`，但推荐"package-private 访问"——Java 不允许跨 `private` 边界的包级访问） | 选方案 B'（反射）+ 拒绝方案 A（改 `private` 为 package-private 会破坏与 SlidingWindowCounter / InMemoryRateLimitGuard 的字段可见性约定一致性）。在测试代码模板中提供完整的反射 helper（`readMapSize` / `readFailures` / `readFirstFailureTime` / `readRecord`）和并发测试代码模板（用例 19/20 改为基于反射的精确断言 `assertEquals(threadCount * callsPerThread, readFailures(...))`），并删除"推荐前者，更精确"等不实描述。同时用例 1/2 增加 `readMapSize` 断言精确验证"isLocked 不创建 entry"契约 |
| [一般] 2. "过期重置"用例（用例 12）描述只能验证阈值逻辑，无法真正验证惰性清除（仅 1 次失败时 `failures < threshold`，与"是否过期"无关） | 修订用例 12 步骤：(a) 先连续 5 次失败触发锁定并断言 `isUsernameLocked==true`（前置条件成立）；(b) `Thread.sleep(300)`（100ms 窗口 + 200ms 安全余量）；(c) 断言 `isUsernameLocked==false`（关键断言：此时 failures=5 仍 >= 阈值 5，必须靠过期分支才能返回 false，证明惰性清除生效）；(d) 再调用一次 `recordUsernameFailure` 并断言 `isUsernameLocked==false`（验证过期后 firstFailureTime 已被本次失败重置）。同时新增用例 13 `shouldResetFirstFailureTimeAfterExpiry`，通过反射 `readFirstFailureTime` 精确断言新时间戳 > 原时间戳 |
| [一般] 3. 错误处理表中 `isXxxLocked` / `clearXxx` 对 null key 的描述与 JDK 实际契约不符（ConcurrentHashMap 明确不支持 null key，所有方法均抛 NPE；HashMap 才允许 null） | 修订错误处理表，统一为"保持 JDK 一致行为：所有公开方法对 null key 一致抛 NPE 自然传播"，删除"map.get/remove 允许 null key"的错误描述。同步更新行为契约中"前置条件"小节，明确 isXxxLocked/clearXxx 的 null key 行为。新增 3 个对应 NPE 用例（用例 16-18：`shouldThrowNpeWhenIsUsernameLockedWithNull` / `shouldThrowNpeWhenIsIpLockedWithNull` / `shouldThrowNpeWhenClearUsernameWithNull`）覆盖被遗漏的 null 路径 |
| [轻微] 1. 未知 key 用例未真正验证"不创建 entry"的契约（仅断言 `isLocked` 返回 false，未断言 map size） | 用例 1/2 增加 `assertEquals(0, readMapSize(tracker, "usernameAttempts"))` 反射断言，精确验证"isLocked 不创建 entry"契约 |
| [轻微] 2. `Thread.sleep(150)` 在慢 CI 上 50ms 余量偏紧 | 短窗口仍为 100ms（保持原值），sleep 增大为 300ms（100ms 窗口 + 200ms 安全余量），适配慢 CI 偶发调度延迟 |
| [轻微] 3. 未覆盖"过期后再失败重置 firstFailureTime"语义 | 用例 12 步骤 (d) 已覆盖该语义（"再调用一次 `recordUsernameFailure` 后 `isUsernameLocked==false`"）；同时新增用例 13 `shouldResetFirstFailureTimeAfterExpiry`，通过反射 `readFirstFailureTime` 精确断言新时间戳 > 原时间戳 |
