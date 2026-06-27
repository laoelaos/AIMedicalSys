# R2: Round 1 修复验证 + 跨模块集成深度审查

审查时间：2026-06-27

### 审查范围

- `JwtTokenProvider.java`
- `JwtAuthenticationFilter.java`
- `AuthServiceImpl.java`
- `PasswordChangeServiceImpl.java`
- `LoginAttemptTracker.java`
- `UserFacadeImpl.java`
- `UserFacade.java`（api）
- `SecurityConfigPhase1.java`（module-impl）
- `GlobalExceptionHandler.java`
- `GlobalErrorCode.java`
- `Result.java`
- `RestAuthenticationEntryPoint.java`
- `RestAccessDeniedHandler.java`
- `BusinessException.java`
- `PasswordChangeService.java`
- `schema.sql`
- `data.sql`
- `JwtUtil.java`

### 发现

#### [严重] 关键缺陷未修复：Access Token 仍然缺少 type claim，JwtAuthenticationFilter 阻断所有已认证请求

- **位置**：`JwtTokenProvider.java:52-63`、`JwtTokenProvider.java:88-105`、`JwtAuthenticationFilter.java:61`
- **描述**：Round 1 报告（review_v1_A）发现的严重问题在本次审查的代码中**未被修复**。`generateAccessToken()`（第 52-63 行）仍未设置 `type="access"` claim。`JwtAuthenticationFilter`（第 61 行）仍以 `validateToken(token, "access")` 方式调用，`validateToken()`（第 95-99 行）在 `expectedType != null` 时检查 `claims.get("type")`，Access Token 中 type 为 null 导致 `"access".equals(null)` 返回 false，`validateToken` 返回 null。所有需 JWT 认证的 API（`/api/auth/me`、`/api/auth/logout`、`/api/auth/password`、`/api/auth/profile`）全部被 Filter 阻断。
- **建议**：立即修复。二选一方案：(a) 在 `generateAccessToken()` 中增加 `.claim("type", "access")`；(b) 修改 `validateToken()` 逻辑：当 `expectedType="access"` 时，若 claims 中 type 为 null 视为合法（符合 OOD 3.2 节 Access Token 无 type 设计），仅当 type 明确为 "refresh" 时才拒绝。

#### [严重] 异常刷新检测仅记录日志，未拒绝请求

- **位置**：`AuthServiceImpl.java:270-283`
- **描述**：`refreshToken()` 方法中的异常刷新检测（`ConcurrentHashMap<Long, Deque<Long>>` + 5 秒/2 次窗口）在检测到刷新频率超限后仅执行 `log.warn()`（第 280 行），未抛出异常或返回错误。这意味着即使刷新频率异常（如 5 秒内 3 次以上），刷新操作仍会成功完成并返回新令牌，异常检测形同虚设，无法起到防滥用作用。
- **建议**：在 `deque.size() > REFRESH_MAX_COUNT` 分支中增加抛出 `BusinessException(GlobalErrorCode.TOKEN_REFRESH_FAILED)` 或 `BusinessException(GlobalErrorCode.RATE_LIMITED)`，使异常刷新被实际阻断。

#### [一般] abnormal refresh 检测窗口未及时清理导致内存泄漏

- **位置**：`AuthServiceImpl.java:270-283`
- **描述**：`refreshTimestamps.compute()` 闭包在每次刷新时向 `deque` 添加时间戳并清理旧条目。然而对于**正常使用**的用户（刷新频率未超限且不再刷新），其 `deque` 对象在内存中永久保有，无过期清理机制。`ConcurrentHashMap` 本身无自动清理，随着用户基数增长，已注销或不活跃用户的 `Deque<Long>` 持续驻留，造成潜在内存泄漏。
- **建议**：方法一：在 size > REFRESH_MAX_COUNT 拒绝后将 deque 置空或设为 null 使其被 GC；方法二：每次操作后若 deque 为空则 `return null` 清除 key；方法三：增加 TTL 定时清理（如 30 分钟无操作自动移除）。

#### [一般] `logout()` 重复 JWT 解析未修复

- **位置**：`AuthServiceImpl.java:182,187`
- **描述**：Round 1（review_v1_A）已指出的问题——`logout()` 先通过 `validateToken()` 获得 `claims`（第 182 行），紧接着又通过 `getJtiFromToken(token)`（第 187 行）重新解析 token，且 `getJtiFromToken()` 内部再次调用 `Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token)`。重复解析浪费 CPU。当前代码中该问题未被修复。
- **建议**：将第 187 行 `jwtTokenProvider.getJtiFromToken(token)` 改为直接从已有 `claims` 中提取 jti：`claims.get("jti", String.class)`，消除重复解析。

#### [一般] `GlobalExceptionHandler.formatMessage()` 对命名占位符先尝试 MessageFormat 产生不必要的异常开销

- **位置**：`GlobalExceptionHandler.java:38-48`
- **描述**：`formatMessage()` 先尝试 `MessageFormat.format(template, args)`，对于 `ACCOUNT_LOCKED` 的模板 `"账户已锁定，请{锁定时间}后重试"`，`MessageFormat` 会因 `{锁定时间}` 不是合法索引格式（如 `{0}`）而抛出 `IllegalArgumentException`，然后被回退路径的 `replaceFirst` 正确解析。每次 `ACCOUNT_LOCKED` 异常路径均产生一次 `IllegalArgumentException` 构造、填充堆栈的开销。Round 1（review_v1_A）已指出，未修复。
- **建议**：先检测模板是否包含 `{0}`、`{1}` 等索引占位符，若仅含命名占位符（如 `{锁定时间}`）则直接使用 `String.replaceFirst` 回退逻辑，避免 `MessageFormat` 异常路径。

#### [一般] `RestAuthenticationEntryPoint` 和 `RestAccessDeniedHandler` 的 Result 未经过消息插值管线

- **位置**：`RestAuthenticationEntryPoint.java:33`、`RestAccessDeniedHandler.java:34`、`GlobalExceptionHandler.java:31`
- **描述**：Security Filter 链中的 `AuthenticationEntryPoint` 和 `AccessDeniedHandler` 直接使用 `Result.fail(errorCode)`（输出模板原文），而 `GlobalExceptionHandler` 使用 `Result.fail(errorCode.getCode(), message)`（输出插值后消息）。虽然当前这些 Handler 使用的 ErrorCode（`UNAUTHORIZED`、`ACCOUNT_DISABLED`、`FORBIDDEN`、`PASSWORD_CHANGE_REQUIRED`）均不包含命名占位符，因此结果是正确的。但未来若在 Security 过滤器链中引入含模板的 ErrorCode，会出现客户端收到模板原文而非插值后消息的问题。这是一个设计级的不一致隐患。
- **建议**：为 `RestAuthenticationEntryPoint` 和 `RestAccessDeniedHandler` 注入同一个 `formatMessage` 工具方法或 `GlobalExceptionHandler`，确保所有异常响应体的消息均经过模板插值；或定义 `ErrorCode.isTemplate()` 方法以区分。

#### [一般] `LoginAttemptTracker.record*()` 方法未处理窗口过期（Round 1 遗留问题）

- **位置**：`LoginAttemptTracker.java:32-49`
- **描述**：Round 1（review_v1_C）已指出的问题。`recordUsernameFailure()` 和 `recordIpFailure()` 在递增失败计数时始终保留 `prev.firstFailureTime()`，未检查当前窗口是否已过期。若未来某个调用方直接调用 `record*` 而不先调用 `is*Locked`，计数器可能跨窗口累积不重置，导致永久锁定风险。当前代码中该问题未被修复。
- **建议**：在 `record*` 方法的 `compute` 闭包中增加窗口过期判断：若 `now - prev.firstFailureTime() >= lockDuration` 则重置为 `new AttemptRecord(1, now)` 而非继续累加。

#### [一般] `JwtUtil.generateToken()` 仍包含 role/position claims 且缺 jti，未被弃用

- **位置**：`JwtUtil.java:71-89`
- **描述**：Round 1（review_v1_C）已指出的问题。`JwtUtil.generateToken()` 仍将 `role` 和 `position` 放入 claims，未生成 `jti`，违反 OOD 3.2 节规定。当前代码该问题未被修复，`JwtUtil` 亦未被标注 `@Deprecated`。需确认是否还存在对 `JwtUtil.generateToken()` 的调用方；若已完全被 `JwtTokenProvider` 替代，应标注 `@Deprecated` 并在后续移除。
- **建议**：(a) 标注 `@Deprecated`；(b) 确认无残留调用后移除；(c) 若仍有调用，按 OOD 3.2 节修正 claims 结构。

#### [轻微] `UserFacade` 接口无 `findAllByIds`/`findAllByUsernames` 方法——设计与描述一致

- **位置**：`UserFacade.java:3-7`、`Docs/05_ood_phase1_B.md:38`
- **描述**：审查要点要求验证 `UserFacadeImpl` 的批量查询方法。经核查，OOD 1.3 节定义 `UserFacade` 的方法签名仅为 `findById`、`findByUsername`、`existsById` 三个方法，设计文档中不存在 `findAllByIds`/`findAllByUsernames` 的方法签名。`UserFacadeImpl` 的实现与设计一致，无偏差。不属于需要报告的问题。
- **建议**：无。

#### [轻微] `SecurityConfigPhase1` 迁移完毕，旧 Filter 无残留引用

- **位置**：`SecurityConfigPhase1.java`（module-impl）、`application/src/main/java/com/aimedical/config/SecurityConfigPhase0.java`
- **描述**：application 层已无 `SecurityConfigPhase1` 或 `JwtAuthenticationFilter`，旧文件已被删除。新 `SecurityConfigPhase1` 位于 `common-module-impl/auth/security/`。Filter 注册顺序（`globalRateLimitFilter → JwtAuthenticationFilter → passwordChangeCheckFilter`）与设计 3.3 节一致。本次检查无发现问题。

#### [轻微] Schema 和种子数据约束验证通过

- **位置**：`schema.sql:21-22`、`data.sql:82-84`
- **描述**：`sys_user` 表的 `enabled`（NOT NULL DEFAULT 1）和 `password_change_required`（NOT NULL DEFAULT 0）的 DEFAULT/NOT NULL 约束符合设定。种子数据中三个用户（admin、doctor01、13900000003）的 `password_change_required=1`，管理员和医生首次登录均需强制修改密码，符合设计预期。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 2 |
| 一般 | 5 |
| 轻微 | 0 |

### 总评

本轮审查揭示了一个严重问题：**Round 1 发现的 Access Token 缺少 type claim 的缺陷未被修复**，且异常刷新检测仅记录日志不拒绝请求，两个严重问题均需立即修复。此外 Round 1 报告的多个一般性问题（重复 JWT 解析、formatMessage 异常开销、LoginAttemptTracker 窗口过期、JwtUtil 未弃用）均未被修复。`SecurityConfigPhase1` 迁移、Schema/种子数据、Filter 注册顺序等检查通过。建议在部署前优先修复两个严重问题，随后修复数量较多的一般性问题。
