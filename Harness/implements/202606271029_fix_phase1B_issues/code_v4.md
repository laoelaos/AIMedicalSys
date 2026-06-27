# 实现报告（v4）

## 概述
修复 T11：去除 `AuthService.getCurrentUser(String token)` 的 token 参数，改为 `getCurrentUser(Long userId)`，由 Controller 从 SecurityContext 获取 userId 后传入。Service 层不再重新解析 JWT。涉及 5 个文件的修改。

## 文件变更清单
| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `common-module-impl/src/main/java/.../service/AuthService.java` | 接口方法签名 `getCurrentUser(String)` → `getCurrentUser(Long userId)` |
| 修改 | `common-module-impl/src/main/java/.../service/impl/AuthServiceImpl.java` | 移除 JWT 解析逻辑，通过 userId 直接查询用户 |
| 修改 | `common-module-impl/src/main/java/.../controller/AuthController.java` | `me()` 改用 `getCurrentUserId()` 获取 userId 后传入 Service |
| 修改 | `common-module-impl/src/test/java/.../service/AuthServiceTest.java` | `getCurrentUser_shouldSucceed()` 适配新签名 |
| 修改 | `common-module-impl/src/test/java/.../controller/AuthControllerTest.java` | `MeTests` 仅保留成功测试，适配 SecurityContext 方式 |

## 编译验证
编译通过，`AuthServiceTest` 和 `AuthControllerTest` 测试全部通过。

## 设计偏差说明
无偏差。
