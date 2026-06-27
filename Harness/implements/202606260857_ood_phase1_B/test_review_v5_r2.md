# 测试审查报告（v5 r2）

## 审查结果

APPROVED

## 发现

- **[轻微]** `LoginAttemptTrackerTest.java:141-154` `shouldResetFirstFailureTimeAfterExpiry` 与 `LoginAttemptTrackerTest.java:124-139` `shouldUnlockAfterLockDurationExpiry` 后半段存在 `firstFailureTime` 重置验证逻辑重叠
  - 两个用例末尾均执行"捕获 originalFirstFailureTime → Thread.sleep(300) → 触发惰性过期 → 再 recordUsernameFailure → 断言 newFirstFailureTime > originalFirstFailureTime"的相同链路。差异仅在用例 12 多了前置 `assertTrue(isUsernameLocked)` 锁定断言与中间 `assertFalse(isUsernameLocked)` 惰性过期断言。
  - 不构成缺陷：用例 12 主语义为"惰性过期解除锁定"，用例 13 主语义为"过期后首败时间戳被重置"；test_v5.md 修订说明（`test_v5.md:13`）已显式记录"用例 13 同等机制内联于用例 12"的设计取舍。
  - 建议（不影响通过）：未来如需精简，可将用例 13 改为仅做首败时间戳对比（去掉中间 `assertFalse(isUsernameLocked)`），与用例 12 形成正交覆盖。

- **[轻微]** `detail_v5.md:201-224` 设计测试列表与测试实现存在条目编号漂移
  - 详细设计 v5 r2 表格中 6 个 null key 错误路径仅列 5 个（遗漏 `clearIp`，见 `detail_v5.md:217-224` 用例 18 后直接跳到用例 19/20 并发），但 v5 r1 审查（`test_review_v5_r1.md:8-10`）已指出该遗漏并在 r2 测试代码中补齐为用例 19 `shouldThrowNpeWhenClearIpWithNull`（`LoginAttemptTrackerTest.java:186-190`），原并发用例顺延为 20/21。
  - 行为层面正确：测试代码 + test_v5.md 用例明细表（`test_v5.md:58-60`）均与实现一致；详细设计 v5 r2 的测试列表本身（`detail_v5.md:201-224`）未同步更新以反映新增用例 19，造成"设计列 20 例、测试跑 21 例"的表面对不齐。
  - 建议（不影响通过）：在下一轮 r3 设计修订时将详细设计测试表格补齐为 21 例，并在用例 18 后插入 `shouldThrowNpeWhenClearIpWithNull` 行；本轮测试行为与设计错误处理表（`detail_v5.md:110-115`）声明的 6 个 null key 路径完全对齐，覆盖完整。

## 已核对的正确性要点

1. v5 r1 审查的 [一般] 1（缺失 `clearIp(null)` NPE 用例）已在 `LoginAttemptTrackerTest.java:186-190` 补齐为用例 19，与 `LoginAttemptTracker.clearIp`（`LoginAttemptTracker.java:100-102`）的 `ConcurrentHashMap.remove(null)` NPE 路径对齐。
2. v5 r1 审查的 [轻微] 2（并发测试 `latch.await` 无超时）已修正：`LoginAttemptTrackerTest.java:209, 230` 使用 `assertTrue(latch.await(10, TimeUnit.SECONDS))` 并补 `import java.util.concurrent.TimeUnit;`（`LoginAttemptTrackerTest.java:7`）。
3. v5 r1 审查的 [轻微] 1（未知 key 用例未验证"不创建 entry"）已修正：`LoginAttemptTrackerTest.java:22, 29` 增加 `assertEquals(0, readMapSize(tracker, "usernameAttempts"/"ipAttempts"))` 反射断言。
4. 21 个用例全部对应详细设计 v5 r2 行为契约的覆盖维度（未知 key / 阈值触发 / 锁定持续 / 清除 / 状态隔离 / 过期惰性清除 / 过期后首败重置 / 6 方法 null key NPE / 并发安全）。
5. 反射 helper（`readMapSize` / `readFailures` / `readFirstFailureTime` / `readRecord`，`LoginAttemptTrackerTest.java:234-254`）与 v5 r1 审查后 r2 设计确认的方案 B'（反射访问 `private final` 字段）一致，对 record accessor（`failures()` / `firstFailureTime()`）的 `getMethod` 调用正确。
6. 用例 12 步骤 (c) 显式调用 `isUsernameLocked` 触发 compute 闭包惰性过期移除（`LoginAttemptTrackerTest.java:134`），使后续 `recordUsernameFailure`（`LoginAttemptTrackerTest.java:135`）走 `prev == null` 分支正确创建新 entry，验证"过期后重新记录 firstFailureTime"语义——与实现报告 `code_v5.md:32` 标注的关键修复一致。
7. 并发测试用 `CyclicBarrier` + `CountDownLatch` 同步，10 线程 × 100 次/线程 = 1000 次并发 `recordXxxFailure` 反射读取 `failures` 精确断言，与 `SlidingWindowCounterTest` 既有并发用例风格一致。
8. 测试 class 形态（package-private）、JUnit 5 注解、反射访问 `private final` 字段的 `setAccessible` 调用与 `SlidingWindowCounterTest` / `InMemoryRateLimitGuardTest` 项目惯例一致。
9. 测试 v5 r2 报告（`test_v5.md:32`）声明 21 用例全部 PASS，编译警告仅为反射 `getMethod` 的原始类型未检查警告（`test_v5.md:72-82`），与同模块既有测试一致，不影响正确性。
