# 任务指令（v17）

## 动作
NEW

## 任务描述
修复 T8（P1）：按 OOD 1.3 + 3.1.4 + 4.8 节设计，实现完整的安全审计日志系统（SecurityAuditLogger），并在 AuthServiceImpl 的 login()/logout()/refreshToken()/changePassword() 方法中调用记录审计事件。同步修改 AuthService 接口、AuthController 及对应测试。

预期文件变更：

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新建 | `common-module-impl/.../auth/audit/SecurityAuditLogger.java` | 接口 `void logAudit(SecurityAuditEvent event)` |
| 新建 | `common-module-impl/.../auth/audit/SecurityAuditEvent.java` | 值对象，字段见 OOD 4.8 节 |
| 新建 | `common-module-impl/.../auth/audit/LoggingSecurityAuditLogger.java` | Phase 1 文件实现 |
| 新建 | `common-module-impl/.../auth/config/AuthModuleConfig.java` | 注册 SecurityAuditLogger Bean |
| 修改 | `common-module-impl/.../service/AuthService.java` | logout() 增加 String refreshToken 参数 |
| 修改 | `common-module-impl/.../service/impl/AuthServiceImpl.java` | 注入 SecurityAuditLogger，在 login/logout/refreshToken/changePassword 中记录审计事件 |
| 修改 | `common-module-impl/.../controller/AuthController.java` | logout() 将 refreshToken 传给 authService.logout() |
| 新建 | `common-module-impl/.../auth/audit/LoggingSecurityAuditLoggerTest.java` | 测试日志文件写入和异常降级 |
| 修改 | `common-module-impl/.../service/AuthServiceTest.java` | 适配 logout 签名变更，验证审计调用 |
| 修改 | `common-module-impl/.../controller/AuthControllerTest.java` | 适配 logout 签名变更 |

## 选择理由
T8 是批次 6 中最后一个核心基础设施修复项，优先级 P1。T9（UserConverter 委托）已独立完成且无日志交叉依赖——T9 设计选择不引入审计日志，因此 T8 可独立实现，不依赖 T9。批次 6 的 T22/T23/T11/T9 均已验收通过，T8 完成后批次 6 全量交付，即可启动批次 7（测试补充 T15-T18）。

## 任务上下文
### 诊断报告 §T8
- AuthService.logout() 当前签名 `void logout(String token)` 未接收 refreshToken，未记录审计日志
- OOD 3.1.4 步骤 4 明确要求调用 `SecurityAuditLogger.logAudit(...)` 记录登出事件
- OOD 1.3 定义 SecurityAuditLogger 为审计日志接口，4.8 节详细定义接口、事件字段、Phase 1 日志文件实现

### 审计事件调用入口（OOD 4.8 §调用入口约定）
- `login()`：登录成功 → `LOGIN_SUCCESS`；登录失败 → `LOGIN_FAILED`（failureReason 区分 USER_NOT_FOUND / BAD_CREDENTIALS / ACCOUNT_DISABLED / ACCOUNT_DELETED / IP_LOCKED / USERNAME_LOCKED）
- `logout()`：登出完成 → `LOGOUT`
- `refreshToken()`：刷新成功 → `TOKEN_REFRESH_SUCCESS`；刷新拒绝 → `TOKEN_REFRESH_REJECTED`（reason 区分 LOCKED / DISABLED / DELETED / TOKEN_VERSION_MISMATCH）
- `changePassword()`：密码修改成功 → `PASSWORD_CHANGED`

### 设计要点
- SecurityAuditEvent.eventType 为 enum，包含：LOGIN_SUCCESS, LOGIN_FAILED, LOGOUT, TOKEN_REFRESH_SUCCESS, TOKEN_REFRESH_REJECTED, PASSWORD_CHANGED, ACCOUNT_LOCKED
- 审计日志写入失败不阻塞主业务流，捕获异常降级为 log.warn
- LoggingSecurityAuditLogger 写入 `${LOG_DIR}/audit/security-audit.log`（可通过 `audit.security.file-path` 配置），格式：`timestamp=<ISO-8601> eventType=<type> userId=<id> username=<name> clientIp=<ip> success=<bool> [failureReason=<reason>] [refreshTokenMasked=<mask>] [newJti=<jti>]`
- 线程安全：Logback RollingFileAppender 内部保证 append 原子性
- refreshTokenMasked 取前 8 位 + "***"

## 已有代码上下文
- `AuthService.java:13` — 当前签名 `void logout(String token)`，需改为 `void logout(String token, String refreshToken)`
- `AuthServiceImpl.java:148-165` — logout() 仅校验 token + 加入黑名单，无审计日志
- `AuthController.java:42-51` — logout() 接收 refreshTokenRequest 但未传递给 service
- `AuthModuleConfig.java` — 已有 RateLimitGuard / TokenBlacklist / LoginAttemptTracker Bean 注册模式
- 当前 auth 包目录结构：`auth/audit/` 目录不存在（需新建），`auth/config/`、`auth/blacklist/`、`auth/jwt/` 等已有
- `RefreshTokenRequest` 为 record `{ @NotBlank String refreshToken }`，Controller 已有 `@RequestBody(required=false) RefreshTokenRequest refreshTokenRequest`
- `AuthServiceImpl` 已注入 UNAUTHORIZED、NOT_FOUND、LOGIN_FAILED 等在 login 流程中使用的错误码
