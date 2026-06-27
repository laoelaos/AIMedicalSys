# 测试报告（v4）

## 概述
为 T11 修复（`AuthService.getCurrentUser` 去除 token 参数，改为 `getCurrentUser(Long userId)`）编写/更新单元测试。

## 测试文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `AuthServiceTest.java` | `getCurrentUser_shouldSucceed()` 适配新签名（移除 JWT mock）；新增 `getCurrentUser_shouldThrowWhenUserNotFound`、`getCurrentUser_shouldThrowWhenUserIdNull` |
| 修改 | `AuthControllerTest.java` | `MeTests` 保留 `shouldReturnSuccessWhenGetCurrentUserSucceeds`（适配 SecurityContext），删除 4 个 token 守卫测试；新增 `shouldThrowWhenNoAuthentication`、`shouldThrowWhenPrincipalTypeInvalid` |

## 测试覆盖矩阵

### AuthServiceTest.getCurrentUser

| 测试方法 | 维度 | 验证点 |
|---------|------|--------|
| `getCurrentUser_shouldSucceed` | 正常路径 | userId 存在返回 UserInfoResponse，id 匹配 |
| `getCurrentUser_shouldThrowWhenUserNotFound` | 错误路径 | userId 不存在抛出 BusinessException(NOT_FOUND) |
| `getCurrentUser_shouldThrowWhenUserIdNull` | 边界条件 | userId 为 null 时抛出 BusinessException(NOT_FOUND) |

### AuthControllerTest.MeTests

| 测试方法 | 维度 | 验证点 |
|---------|------|--------|
| `shouldReturnSuccessWhenGetCurrentUserSucceeds` | 正常路径 | SecurityContext 正常时调用 service 并返回 SUCCESS |
| `shouldThrowWhenNoAuthentication` | 错误路径 | SecurityContext 无 Authentication 时抛出 IllegalStateException |
| `shouldThrowWhenPrincipalTypeInvalid` | 错误路径 | principal 类型非 Long/Integer 时抛出 IllegalStateException |

## 修订记录（v4 r1 审查后）
- **AuthController.java**：`getCurrentUserId()` 新增 `authentication == null` 守卫，抛出 `IllegalStateException`（之前因直接调用 `authentication.getPrincipal()` 导致 NPE，与设计不符）
- **AuthControllerTest.java**：`shouldThrowWhenNoAuthentication` 期望异常从 `NullPointerException` 修正为 `IllegalStateException`，对齐设计

## 设计偏差说明
无偏差。测试覆盖符合详细设计中的行为契约。
