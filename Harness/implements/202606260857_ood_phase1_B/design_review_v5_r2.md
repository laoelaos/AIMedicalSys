# 设计审查报告（v5 r2）

## 审查结果
APPROVED

## 发现

### [轻微] 1. 用例 12 步骤 (d) 描述与实际状态不一致：声称 "failures = 6 >= 5"，但实际上经过步骤 (c) 的惰性清除，步骤 (d) 的 `recordUsernameFailure` 会创建一个全新的 entry（failures=1），而非在原 entry 上累加

设计 219 行和 240 行的用例 12 步骤 (d) 描述为：

> 步骤 (d)：再调用一次 `recordUsernameFailure`("user")，断言 `isUsernameLocked("user") == false`（**此时 failures = 6 >= 5**，但 firstFailureTime 已被本次失败重置为当前时刻 → 锁定应被解除）

但根据设计 124 行的惰性清除契约（`isXxxLocked` 中检测到过期时 compute 闭包返回 null → 移除该 key），步骤 (c) 断言时 entry 已被惰性移除。步骤 (d) 的 `recordUsernameFailure` 走 compute 闭包时 `prev == null`，会创建新 `AttemptRecord(1, now)`，并非在 (5, t1) 上累加为 (6, newTime)。

**影响范围**：
- 测试断言 `assertFalse(isUsernameLocked(key))` 仍能通过（failures=1 < 5），用例 13 通过反射 `readFirstFailureTime` 断言新时间戳 > 旧时间戳也能通过（新 entry 的 firstFailureTime = now > t1）
- 不影响实施和测试通过，但描述与设计契约（`firstFailureTime 一旦记录不再改变（仅在锁定到期惰性清除后由下次失败重新记录）`）存在自相矛盾之处
- 不构成实现障碍，仅是文档准确性问题

**建议修正**：步骤 (d) 描述改为 "此时原 entry 已被惰性清除，本次失败创建新 entry（failures=1, firstFailureTime=now），新计数不足以触发锁定"。或者更简洁地删除 "failures = 6 >= 5" 这一中间推理，保留 "验证过期后下次失败重新记录 firstFailureTime 语义" 即可——此语义已由用例 13 通过反射精确验证。

### [轻微] 2. 用例 13（`shouldResetFirstFailureTimeAfterExpiry`）与用例 12 步骤 (d) 存在功能重叠

用例 13 表层描述为"通过反射断言过期后再失败时 map 中的 firstFailureTime 是新时间戳"。但用例 12 步骤 (d) 已经隐式覆盖了"过期后再失败"这一行为，只是没有反射断言 firstFailureTime 值。

**影响范围**：
- 用例 13 增加了反射断言 `readFirstFailureTime`，更精确地验证了 firstFailureTime 被重置
- 用例 13 是用例 12 步骤 (d) 的精确化补充，保留两个用例有助于测试可读性（一个验证行为，一个验证内部状态）
- 不影响实施和测试通过

**建议修正**：可在用例 13 的描述中明确"独立于用例 12，专注于通过反射验证 firstFailureTime 时间戳确实被重置"，避免读者误以为这是冗余用例。

## 审查要点确认

### v5 r1 三个 [一般] 问题是否已全部解决
- ✅ [一般] 1. 并发测试断言方式与字段可见性自相矛盾 → 已通过 259-281 行的完整反射 helper 代码模板（`readMapSize` / `readFailures` / `readFirstFailureTime` / `readRecord`）和 283-304 行的并发测试代码模板解决；删除了"推荐前者，更精确"的不实描述
- ✅ [一般] 2. "过期重置"用例无法真正验证惰性清除 → 用例 12 步骤 (a) 增加了"5 次失败后断言 `isUsernameLocked==true`"的前置条件；步骤 (c) 增加了"关键断言：失败次数 = 5 仍 >= 阈值 5"的关键性论述；步骤 (d) 验证过期后 firstFailureTime 重置；用例 13 增加反射精确断言
- ✅ [一般] 3. 错误处理表中 null-key 行为与 JDK 实际契约不符 → 107-108 行统一为"保持 JDK 一致行为：所有公开方法对 null key 一致抛 NPE 自然传播"；用例 16-18 补全了 isUsernameLocked / isIpLocked / clearUsername 的 NPE 用例

### v5 r1 三个 [轻微] 问题是否已全部解决
- ✅ [轻微] 1. 未知 key 用例未真正验证"不创建 entry" → 用例 1/2 增加 `readMapSize` 反射断言
- ✅ [轻微] 2. Thread.sleep(150) 余量偏紧 → 改为 300ms（100ms 窗口 + 200ms 安全余量）
- ✅ [轻微] 3. 未覆盖"过期后再失败重置 firstFailureTime"语义 → 用例 12 步骤 (d) + 用例 13

### OOD 4.1 / 3.1.1 契约一致性
- ✅ 第二层失败计数：username 5次/15min、IP 20次/30min，与 OOD 4.1 表格一致
- ✅ 第三层账户锁定：`isXxxLocked` 返回 true 触发锁定，调用方 AuthServiceImpl 负责生成"30 分钟后重试" / "15 分钟后重试"消息
- ✅ 步骤 3 双维度检查：`isIpLocked` + `isUsernameLocked` 均提供
- ✅ 步骤 5/6/7 失败计数递增：`recordUsernameFailure` + `recordIpFailure` 均提供（步骤 5/6 调 IP 维度，步骤 6/7 调 username 维度）
- ✅ 步骤 8 登录成功清除：`clearUsername` + `clearIp` 均提供
- ✅ OOD 9.3 并发安全：所有读写走 `ConcurrentHashMap.compute` 闭包原子操作
- ✅ "连续"语义：惰性清除机制与"超过锁定时间后惰性清除"描述一致

### 与 SlidingWindowCounter / InMemoryRateLimitGuard 风格一致性
- ✅ 字段可见性 `private final ConcurrentHashMap<...>`：与 SlidingWindowCounter 的 `private final ConcurrentHashMap<String, Deque<Long>>` 一致
- ✅ 测试 class 形态（JUnit 5，无 @SpringBootTest 注解）：与 SlidingWindowCounterTest / InMemoryRateLimitGuardTest 一致
- ✅ 并发测试使用 `CountDownLatch` + `CyclicBarrier`：与 v4 SlidingWindowCounterTest 并发测试模式一致

### Java 21 record 适用性
- ✅ `private static record AttemptRecord(int failures, long firstFailureTime)`：record 形式简洁，自动生成 accessor（`failures()` / `firstFailureTime()`），与 compute 闭包内"new AttemptRecord(...) 创建新不可变快照"的更新模式天然契合
- ✅ 反射 helper `record.getClass().getMethod("failures")` 调用的是 record 生成的 public accessor，Java 21 同包内可正常访问

### 接口契约完整性
- ✅ 6 个公开方法（3 对 recordXxx / isXxxLocked / clearXxx）覆盖 OOD 4.1 / 3.1.1 所有调用场景
- ✅ 无参构造器与 AuthModuleConfig @Bean 注册兼容
- ✅ Package-private 测试构造器提供短窗口能力（100ms）以验证过期逻辑，不破坏对外封装性

### 错误处理
- ✅ 所有公开方法对 null key 一致抛 NPE（与 ConcurrentHashMap 契约一致）
- ✅ 不抛业务异常（`BusinessException` / `GlobalErrorCode` 调用由 AuthServiceImpl 负责）
- ✅ 不存在 key 的 `clearXxx` / `isXxxLocked` 通过 `get()` 短路返回 false，不创建 entry
- ✅ 已过期 key 通过 compute 闭包返回 null 触发 ConcurrentHashMap 自动移除

### 测试设计完整性
- ✅ 20 个用例覆盖：未知 key（2）+ 阈值触发/持续/清除（username 3 + IP 3）+ 状态隔离（2）+ 过期重置（2）+ 错误路径（5）+ 并发安全（2）
- ✅ 反射 helper 集中在测试类私有静态方法中，对外暴露 3 个简洁 API
- ✅ 并发测试模板（283-304 行）和过期重置测试模板（305-323 行）可直接复制使用

## 结论

设计 v5 r2 全面解决了 v5 r1 提出的 3 个 [一般] 问题和 3 个 [轻微] 问题，OOD 4.1 / 3.1.1 / 9.3 契约覆盖完整，与项目现有工具类（SlidingWindowCounter / InMemoryRateLimitGuard）风格一致，Java 21 record 使用恰当，测试设计（20 用例 + 完整代码模板）可执行性强。发现的两个 [轻微] 描述不准确问题不影响实施和测试通过，可作为后续优化项。

APPROVED
