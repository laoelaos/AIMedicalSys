# R1: 核心认证业务逻辑与 OOD 设计一致性审查

审查时间：2026-06-27

### 审查范围

- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/impl/AuthServiceImpl.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/AuthService.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/controller/AuthController.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/request/LoginRequest.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/response/LoginResponse.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/request/RefreshTokenRequest.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/response/TokenRefreshResponse.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/request/PasswordChangeRequest.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/request/ProfileUpdateRequest.java`
- 辅助文件：`JwtTokenProvider.java`、`JwtAuthenticationFilter.java`、`PasswordChangeCheckFilter.java`、`LoginAttemptTracker.java`、`GlobalExceptionHandler.java`、`GlobalErrorCode.java`、`BusinessException.java`

### 发现

#### [严重] Access Token 无 type claim 导致 JwtAuthenticationFilter 拒绝所有已认证请求

- **位置**：`JwtTokenProvider.java:52-63`（generateAccessToken 未设置 type claim）与 `JwtAuthenticationFilter.java:61`（validateToken 传入 "access"）
- **描述**：OOD 3.2 节规定 Access Token 无 `type` claim，Refresh Token 携带 `type=refresh`，Filter 应拒绝将 Refresh Token 当作 Access Token 使用。`JwtTokenProvider.validateToken(token, "access")` 在 expectedType="access" 时检查 claims 中的 type 字段，但 Access Token 没有 type claim（`generateAccessToken` 未设置），导致 `claims.get("type", String.class)` 返回 null，`"access".equals(null)` 为 false，**validateToken 返回 null**。所有需要 JWT 认证的 API（`/api/auth/me`、`/api/auth/logout`、`/api/auth/password`、`/api/auth/profile`）全部被 Filter 阻断，仅 permitAll 端点和 Refresh Token 自验证端点正常工作。
- **建议**：修改 `generateAccessToken` 增加 `.claim("type", "access")`，或修改 `validateToken` 的 type 检查逻辑：当 `expectedType="access"` 且 claims 中 type 为 null 时视为合法（因为 OOD 设计 Access Token 无 type claim），仅当 type 明确为 "refresh" 时才拒绝。

#### [一般] `AuthServiceImpl.login()` 的 ACCOUNT_LOCKED args 与设计文档示例不一致

- **位置**：`AuthServiceImpl.java:104,110` vs `Docs/05_ood_phase1_B.md:155`
- **描述**：代码抛出 `BusinessException(GlobalErrorCode.ACCOUNT_LOCKED, "30分钟")` 和 `"15分钟"`，实际通过 `GlobalExceptionHandler.formatMessage()` 的 `String.replaceFirst` 回退路径产生正确消息 `"账户已锁定，请30分钟后重试"`。然而 OOD 3.1.1 节示例显示 args 应传递 `"请30分钟后重试"`（即包括固定语境框架）。若按设计文档传递，`replaceFirst` 会导致双重词缀（"请请30分钟后重试后重试"）。此外，`MessageFormat.format("账户已锁定，请{锁定时间}后重试", "30分钟")` 会抛出 `IllegalArgumentException`（因 `{锁定时间}` 不是合法的 MessageFormat 索引），虽靠回退机制正确工作，但每次锁定均会产生异常堆栈开销。
- **建议**：(a) 统一设计文档和代码的 args 语义为仅传递动态参数（如 `"30分钟"`）；(b) `formatMessage` 对已知命名占位符模板（如 `{锁定时间}`）直接使用 `String.replace` 而非先尝试 `MessageFormat` 再回退，避免每次异常路径触发异常构造开销。

#### [一般] `AuthServiceImpl.logout()` 对过期 token 提前返回，跳过审计日志

- **位置**：`AuthServiceImpl.java:183-185`
- **描述**：当 token 已过期时 `validateToken` 返回 null，方法直接 return，不记录审计日志。OOD 3.1.4 规定登出必须记录安全审计日志（`eventType=LOGOUT`），即使 token 过期也应尽最大努力记录（此时可通过从请求上下文获取 userId 等线索）。当前实现对于过期 token 登出场景完全丢失审计记录。
- **建议**：当 validateToken 因过期失败时，尝试从 `ExpiredJwtException` 中提取 claims 信息（`catch` 块中可通过 `exception.getClaims()` 获取），若仍无法获取则至少记录包含 clientIp 的匿名登出审计事件。

#### [一般] `AuthServiceImpl.logout()` 对已存在 claims 的对象重复解析 token

- **位置**：`AuthServiceImpl.java:182-187`
- **描述**：`logout()` 先调用 `validateToken` 获得 `claims`，紧接着又调用 `getJtiFromToken(token)` 重新解析 token（再次调用 `Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token)`）。OOD 未明确要求此处的实现细节，但从已解析的 claims 中可直接读取 jti（`claims.get("jti", String.class)`），重复解析浪费 CPU。
- **建议**：将第 187 行 `jwtTokenProvider.getJtiFromToken(token)` 改为直接从已有 `claims` 中提取 jti，消除重复 JWT 解析。

#### [一般] `AuthService.getCurrentUser(String token)` 直接验证 token 参数而非依赖 SecurityContext

- **位置**：`AuthService.java:17`、`AuthServiceImpl.java:303-314`
- **描述**：OOD 3.1.5 规定 `GET /api/auth/me` 应从 SecurityContext 获取当前用户 ID（已由 JwtAuthenticationFilter 装配），但实现采用 token 字符串参数传入后重新验证。这使得 `getCurrentUser` 方法绕过 Filter 层已经完成的用户状态检查（enabled/deleted/黑名单），且与 `changePassword` 方法（通过 SecurityContext 获取 userId）的签名风格不一致。`updateProfile` 方法也存在同样问题。
- **建议**：将 `getCurrentUser` 和 `updateProfile` 的方法签名改为接受 `Long userId`（从控制器 SecurityContext 提取），与 `changePassword` 保持一致。控制器负责从 SecurityContext 提取 userId。

#### [轻微] `AuthController.changePassword()` 的 getCurrentUserId 方法未处理无认证用户场景

- **位置**：`AuthController.java:89-99`
- **描述**：`getCurrentUserId()` 从 SecurityContext 取 principal，若 principal 不是 Long/Integer 类型直接抛出 `IllegalStateException`，返回 500。虽然此路径在正常认证流程中不应到达（PasswordChangeCheckFilter 已放行密码变更 API），但若 SecurityContext 被意外篡改或 Filter 链异常，返回 500 不如返回 401/403 友好。
- **建议**：改为在 principal 类型不匹配时通过 `Result.fail("UNAUTHORIZED")` 返回，而非抛出未处理的 RuntimeException。

#### [轻微] `AuthServiceImpl.login()` 中 `deleted` 和 `enabled` 分离为两个独立 if 块但逻辑行为一致

- **位置**：`AuthServiceImpl.java:125-141`
- **描述**：OOD 3.1.1 步骤 6 将 `enabled==false || deleted==true` 合并为一个检查点，代码因需要审计日志区分 `ACCOUNT_DISABLED` 和 `ACCOUNT_DELETED` 将其拆为两块。拆分本身合理，但两块的重复代码（各 5 行）可通过提取方法减少重复。
- **建议**：提取 `handleDisabledLoginAttempt(User user, String clientIp, String auditReason)` 方法，减少代码重复。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 1 |
| 一般 | 4 |
| 轻微 | 2 |

### 总评

核心认证业务逻辑在功能流程（限流→锁定→用户校验→密码匹配→token 签发）上与 OOD 设计高度一致，DTO、接口方法签名与设计文档基本对齐。存在一个**严重问题**：Access Token 缺少 `type` claim 导致 `JwtAuthenticationFilter` 拒绝所有已认证请求——这会在运行时直接阻断除 login/refresh 外全部 API，必须在部署前修复。其余问题集中在重复 JWT 解析、审计日志兜底、设计与代码参数示例不一致等方面，修复成本低。建议优先修复严重问题并补齐审计日志兜底逻辑。
