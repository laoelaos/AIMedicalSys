# 代码审查报告（v5 r1）

## 审查结果

REJECTED

## 发现

- **[严重]** `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/login/LoginAttemptTrackerTest.java:141-152` — `shouldResetFirstFailureTimeAfterExpiry` 用例在 `Thread.sleep(300)` 后直接调用 `recordUsernameFailure(key)`，但缺少触发惰性过期检查的步骤（`isUsernameLocked` / `clearUsername`），导致生产代码在 `recordUsernameFailure` 的 compute 闭包内走 `prev != null` 分支，保持旧 `firstFailureTime`（生产实现严格遵循"首次失败时间戳不变"语义），最终 `newFirstFailureTime == originalFirstFailureTime`，断言 `assertTrue(newFirstFailureTime > originalFirstFailureTime)` 必定失败。

- **[轻微]** `LoginAttemptTrackerTest.java` 用例 12 步骤 (d) 的设计描述声称"此时 failures = 6 >= 5"，但实际在 `isUsernameLocked` 触发惰性清除后 entry 已被移除，第 6 次 `recordUsernameFailure` 通过 `prev == null` 分支创建新 entry，`failures = 1` 而非 6；测试断言 `assertFalse(isUsernameLocked(key))` 实际是因为 `1 < 5` 而非"firstFailureTime 被重置导致解除"。测试代码本身正确，但设计的 WHY 解释与实际机制不符，建议设计侧修订以避免后续维护误解。

- **[轻微]** `LoginAttemptTracker.java:57, 79` 使用 `boolean[1] locked` 数组从 compute 闭包传出结果，语义正确但风格稍显绕；可改用 `AtomicBoolean` 以提升可读性（不影响功能）。

## 修改要求（仅 REJECTED 时）

1. **[严重] `LoginAttemptTrackerTest.java:141-152` `shouldResetFirstFailureTimeAfterExpiry`** — 问题：测试在 `Thread.sleep(300)` 后直接调用 `recordUsernameFailure(key)`，此时 map 中的 entry 仍存在（未触发惰性过期检查/移除），生产代码在 compute 闭包内走 `prev != null` 分支并保留旧 `firstFailureTime`，最终 `newFirstFailureTime` 等于 `originalFirstFailureTime`，断言 `assertTrue(newFirstFailureTime > originalFirstFailureTime)` 失败。期望修正：在 `Thread.sleep(300)` 之后、`recordUsernameFailure(key)` 之前增加 `assertFalse(tracker.isUsernameLocked(key));`（或 `tracker.clearUsername(key);`），以触发惰性过期检查并移除 entry，使下一次 `recordUsernameFailure` 通过 `prev == null` 分支创建新 entry，从而正确验证"过期后 firstFailureTime 被重置"语义。
