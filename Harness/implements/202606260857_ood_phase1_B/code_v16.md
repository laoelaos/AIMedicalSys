# 实现报告（v16）

## 概述

根据详细设计 v16，重构 `AuthServiceImpl` 为核心认证服务，集成 R4–R15 所有已建基础设施，按 OOD 3.1 认证流程完整实现 6 个认证方法的行为契约。同步更新 `AuthService` 接口（新增 `changePassword`）、`UserRepository`（新增 `findTokenVersionById`）、`AuthController`（`changePassword` 端点接入 `JwtTokenProvider` 提取 userId），并重写 `AuthServiceTest` 为 19 用例。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `.../service/AuthService.java` | 接口新增 `changePassword(Long, String, String)` |
| 重写 | `.../service/impl/AuthServiceImpl.java` | 全量实现 OOD 3.1 认证流程，9 依赖注入 |
| 修改 | `.../permission/UserRepository.java` | 新增 `findTokenVersionById(Long)` JPQL 查询 |
| 修改 | `.../controller/AuthController.java` | `changePassword` 从 token 提取 userId 后传入 `authService`；新增 `JwtTokenProvider` 依赖 |
| 重写 | `.../service/AuthServiceTest.java` | 19 用例单元测试 |
| 修改 | `.../controller/AuthControllerTest.java` | 适配 `AuthController` 新增构造函数参数和 `changePassword` 签名变更 |

## 编译验证

编译成功，测试全部通过：
- `AuthServiceTest`: 19 tests run, 0 failures
- `AuthControllerTest`: 11 tests run, 0 failures

## 设计偏差说明

无偏差。所有实现严格遵循详细设计 v16 的类型定义、方法签名、行为契约和错误处理规范。
