# 实现报告（v17）

## 概述

实现了 `SecurityAuditLogger` 安全审计日志系统，包含接口、事件值对象、事件类型枚举、Logback 文件实现，并在 `AuthServiceImpl.login()/logout()/refreshToken()/changePassword()` 中调用审计日志。同步修改了 `AuthService` 接口（`logout` 增加 `refreshToken` 参数）、`AuthController`（透传 refreshToken）及对应测试。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新建 | `common-module-impl/.../auth/audit/SecurityAuditEventType.java` | 审计事件类型枚举 |
| 新建 | `common-module-impl/.../auth/audit/SecurityAuditEvent.java` | 审计事件值对象 record |
| 新建 | `common-module-impl/.../auth/audit/SecurityAuditLogger.java` | 审计日志接口 |
| 新建 | `common-module-impl/.../auth/audit/LoggingSecurityAuditLogger.java` | Phase 1 Logback 文件实现 |
| 新建 | `application/src/main/resources/logback-spring.xml` | SECURITY_AUDIT logger 的 RollingFileAppender 配置 |
| 修改 | `common-module-impl/.../auth/config/AuthModuleConfig.java` | 新增 `@Bean securityAuditLogger()` |
| 修改 | `common-module-impl/.../service/AuthService.java` | `logout(String)` → `logout(String, String)` |
| 修改 | `common-module-impl/.../service/impl/AuthServiceImpl.java` | 注入 SecurityAuditLogger，四方法添加审计调用 |
| 修改 | `common-module-impl/.../controller/AuthController.java` | logout 透传 refreshToken |
| 新建 | `common-module-impl/.../auth/audit/LoggingSecurityAuditLoggerTest.java` | 测试日志格式和异常降级 |
| 修改 | `common-module-impl/.../service/AuthServiceTest.java` | 适配 logout 签名 + mock SecurityAuditLogger |
| 修改 | `common-module-impl/.../controller/AuthControllerTest.java` | 适配 logout 调用签名 |

## 编译验证

`mvn test -pl modules/common-module/common-module-impl -am` — BUILD SUCCESS, Tests run: 368, Failures: 0, Errors: 0

## 设计偏差说明

无偏差
