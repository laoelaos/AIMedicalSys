# 测试审查报告（v16 r1）

## 审查结果
REJECTED

## 发现

- **[一般]** `AuthServiceTest.java` — `refreshToken` 方法缺少"用户名维度锁定"场景的测试。行为契约 step 5 明确要求当 `loginAttemptTracker.isUsernameLocked()` 返回 true 时抛出 `BusinessException(TOKEN_REFRESH_FAILED)`。这是一个真实的业务场景（用户因登录失败次数过多被锁定后尝试刷新 token），且 `login` 方法已有对应测试（case 4），但 `refreshToken` 未覆盖。若实现中遗漏此检查，现有测试无法捕获。

- **[轻微]** `AuthServiceTest.java` — `refreshToken` 方法缺少 `findTokenVersionById` 返回 `Optional.empty()` 场景的测试（契约 step 7）。仅测试了版本不匹配（step 8，case 12），未覆盖"版本号数据不存在"的分支。

- **[轻微]** `AuthServiceTest.java` — `refreshToken` 方法缺少 `getUserIdFromClaims` 返回 null 场景的测试（契约 step 2）。

- **[轻微]** `AuthServiceTest.java` — `getCurrentUser` 和 `updateProfile` 方法缺少错误路径测试（UNAUTHORIZED 和 NOT_FOUND）。

- **[轻微]** `AuthServiceTest.java` — `logout` 方法仅测试了正常登出路径，缺少 null token 和无效 token 的边界测试。

## 修改要求（仅 REJECTED 时）

1. `AuthServiceTest.java` — 新增 `refreshToken_shouldThrowOnUsernameLocked` 测试方法：
   - **问题**：`refreshToken` 契约 step 5 中 `isUsernameLocked → true → TOKEN_REFRESH_FAILED` 未被测试。
   - **原因**：这是完整的业务路径，遗漏会导致回归测试盲区。
   - **修正方向**：参照 `login_shouldThrowUsernameLocked`（case 4）的模式，构造 token 有效、用户正常加载、`isUsernameLocked` 返回 true 的场景，断言 `BusinessException(TOKEN_REFRESH_FAILED)`。

2. `AuthServiceTest.java` — 建议新增 `refreshToken_shouldThrowOnTokenVersionNotFound` 测试方法：
   - **问题**：`findTokenVersionById` 返回 `Optional.empty()` 的分支未被测试。
   - **原因**：与版本不匹配（case 12）是两个独立的条件分支，应分别覆盖。
   - **修正方向**：在 case 12 基础上将 `findTokenVersionById` stub 改为 `Optional.empty()`，断言 `TOKEN_REFRESH_FAILED`。
