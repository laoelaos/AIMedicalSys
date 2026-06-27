# 测试报告（v9）

## 测试文件

`modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/controller/AuthControllerTest.java`

## 变更说明

### 1. UpdateMeTests DisplayName 更新

`@DisplayName("PUT /api/auth/me (updateProfile)")` → `@DisplayName("PUT /api/auth/profile")`

T6 将端点从 `/me` 变更为 `/profile`，测试类描述同步更新。

### 2. ChangePasswordTests 新增 BusinessException 错误路径用例

新增 `shouldThrowBusinessExceptionWhenChangePasswordFails()` 测试，验证：

- **前置条件**：SecurityContext 已设置（principal=1L）
- **输入**：错误旧密码 `"wrongOldPass"`
- **模拟行为**：`authService.changePassword()` 抛出 `BusinessException(GlobalErrorCode.PASSWORD_MISMATCH)`
- **验证**：`assertThrows(BusinessException.class, ...)` 确认异常传播
- **验证**：`authService.changePassword(1L, "wrongOldPass", "newPass123")` 被正确调用

与项目已有测试模式一致（参考 LoginTests、RefreshTests 中的 BusinessException 用例）。

## 覆盖维度

| 维度 | 状态 |
|------|------|
| 正常路径 | ✓ `shouldReturnSuccessForChangePassword()` |
| 错误路径 | ✓ `shouldThrowBusinessExceptionWhenChangePasswordFails()` |
| 边界条件 | N/A（无边界条件变更） |
| 状态交互 | ✓ SecurityContext mock 注入 |

## 修订说明

无。
