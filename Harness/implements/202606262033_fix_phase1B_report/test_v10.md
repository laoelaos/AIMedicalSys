# 测试报告（v10）

## 验证结论

现有测试文件 `AuthControllerTest.java` 已覆盖详细设计 v10 的全部行为契约，无需新增或修改测试代码。

## 行为契约覆盖矩阵

| 契约 | 测试方法 | 状态 |
|------|---------|------|
| 正常路径：密码修改成功返回 SUCCESS | `shouldReturnSuccessForChangePassword()` L246-L262 | ✅ 已覆盖，`doNothing().when()` 语法正确 |
| 错误路径：密码修改失败抛出 BusinessException | `shouldThrowBusinessExceptionWhenChangePasswordFails()` L264-L279 | ✅ 已覆盖，L273-L274 已修正为 `doThrow().when()` 语法正确 |

## Mockito 语法验证

- `authService.changePassword(Long, String, String)` 声明返回类型 void
- L255：`doNothing().when(authService).changePassword(...)` — 正确
- L273-L274：`doThrow(...).when(authService).changePassword(...)` — 已修正，正确

## 文件

`AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/controller/AuthControllerTest.java`（281 行，无改动）
