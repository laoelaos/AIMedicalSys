# 代码审查报告（v5 r2）

## 审查结果

APPROVED

## 发现

无严重、无一般问题。以下为轻微观察，不影响正确性：

- **[轻微]** `LoginAttemptTracker.java:33, 43` `recordUsernameFailure` / `recordIpFailure` 中 `long now = System.currentTimeMillis();` 在 `compute` 闭包**外**捕获，而非设计文档所述"在闭包内获取"。功能上等价——Java `final`/effectively-final 捕获保证闭包内看到的 `now` 与外层一致，且若 compute 因冲突重试也使用同一值（比"闭包内获取"更稳定）。建议设计文档措辞放宽或保持现状。
- **[轻微]** `LoginAttemptTrackerTest.java:141-153` `shouldResetFirstFailureTimeAfterExpiry` 与用例 12 `shouldUnlockAfterLockDurationExpiry` 的 `firstFailureTime` 校验逻辑高度重叠（均为过期后断言新时间戳 > 原时间戳）。此为设计 v5 r2 显式要求（"新增用例 13"），属于设计侧决策，不构成代码缺陷。
- **[轻微]** `LoginAttemptTracker.java:53-71, 75-93` `isUsernameLocked` / `isIpLocked` 中本地变量 `prev` 仅用于 `null` 判断，compute 闭包内另以 `current` 重新读取，命名略不一致。功能正确，属可读性小瑕疵。

## 已核对的正确性要点

1. 生产类形态、字段可见性（`private final`）、内部 `private static record AttemptRecord` 与设计 v5 r2 完全一致。
2. 6 个公开方法签名与行为契约一致；`recordUsernameFailure` / `recordIpFailure` 在 compute 闭包内做不可变快照更新；`isXxxLocked` 通过"先 `get` 判 null → 后 `compute` 完成读+过期+惰性清除"的双重检查模式正确实现；`clearXxx` 直接 `remove` 对不存在 key 安全。
3. `boolean[1]` 传出计算结果模式与同模块 `SlidingWindowCounter.tryAcquire` 风格一致。
4. 单元测试 20 用例全部按设计 v5 r2 表格实现（逐一对应 1–20 编号与验证点）。
5. 反射 helper（`readMapSize` / `readFailures` / `readFirstFailureTime` / `readRecord`）使用 Java record accessor (`failures()` / `firstFailureTime()`) 而非字段名，对 `private static record` 的访问正确。
6. 用例 12 步骤 (c) `assertFalse(tracker.isUsernameLocked(key))` 与用例 13 步骤 (d) 之间 `assertFalse(tracker.isUsernameLocked(key))` 已正确插入，触发惰性过期移除使后续 `recordUsernameFailure` 走 `prev == null` 分支——r2 关键修复已落实。
7. 用例 1/2 通过 `readMapSize` 反射断言 `map.size() == 0`，精确验证"未知 key 不创建 entry"契约。
8. 5 个 null key NPE 用例（14–18）覆盖全部公开方法，与 JDK `ConcurrentHashMap` 一致抛 NPE 自然传播契约一致。
9. 2 个并发用例（19/20）使用 `CyclicBarrier` + `CountDownLatch` 同步，通过反射精确读取 `failures` 值断言等于总调用次数。
10. 字段 `private final` 与项目内 `SlidingWindowCounter` / `InMemoryRateLimitGuard` 字段可见性约定一致；测试同包访问 `private` 字段不受 JDK 17+ `setAccessible` module 限制。