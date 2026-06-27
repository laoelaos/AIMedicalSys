# R2: 模块二 Security & JWT 代码审查

审查时间：2026-06-26

### 审查范围

- `auth/jwt/JwtTokenProvider.java`
- `jwt/JwtConfig.java`
- `jwt/JwtUtil.java`
- `auth/security/JwtAuthenticationFilter.java`
- `auth/security/PasswordChangeCheckFilter.java`
- `auth/security/GlobalRateLimitFilter.java`
- `auth/security/CurrentUserImpl.java`
- `auth/security/SecurityConfigPhase1.java`
- `auth/security/RestAuthenticationEntryPoint.java`
- `auth/security/RestAccessDeniedHandler.java`
- `auth/exception/PasswordChangeRequiredException.java`

### 发现

#### [严重] JwtTokenProvider@PostConstruct 缺少启动验证

- **位置**：`JwtTokenProvider.java:31-36`
- **描述**：`init()` 方法直接调用 `Base64.getDecoder().decode(secret)`，未对 secret 做 null/空值检查，未验证 Base64 解码后字节长度 >= 32，未验证 Base64 URL-safe 字符集。设计文档 4.7 节要求：secret 为空时抛出 `IllegalStateException("JWT_SECRET must be configured")`；解码后字节 < 32 时抛出 `IllegalStateException("JWT_SECRET must be at least 256 bits (32 bytes) after Base64 decoding")`；不合法字符时抛出 `IllegalStateException("JWT_SECRET contains invalid characters")`。误配置时当前代码会抛出 `IllegalArgumentException`（Base64 decode 异常）而非明确的 `IllegalStateException`，掩盖真实原因。
- **建议**：在 `init()` 中增加三段验证逻辑，与 `JwtUtil.init()` 对齐（`JwtUtil.java:48-59` 已有正确的实现），并将异常包装为 `IllegalStateException`。

#### [严重] JwtAuthenticationFilter 依赖 JwtUtil（旧代码）而非 JwtTokenProvider（新提供者）

- **位置**：`JwtAuthenticationFilter.java:5,34,38,60,118`
- **描述**：设计文档 1.3 节定义 `JwtTokenProvider` 为 "JWT 令牌生成、解析、验证的集中提供者"；3.3 节步骤 2 明确 Filter 应调用 `JwtTokenProvider.validateToken(token)`。但当前 Filter 注入并使用 `JwtUtil`（旧工具类）完成 token 验证和提取。`JwtTokenProvider` 的 `validateToken()` 方法（含 type claim 验证逻辑）未被 Filter 使用。导致：① `JwtTokenProvider` 虽已注册为 `@Component`，但其核心验证逻辑在认证链中未生效；② token 类型检查与解析逻辑在 Filter 中重复实现（`JwtAuthenticationFilter.java:68-73`）；③ 若后续清理旧 `JwtUtil`，Filter 编译将失败。
- **建议**：JwtAuthenticationFilter 改为依赖 `JwtTokenProvider`，调用 `jwtTokenProvider.validateToken(token, null)` 解析 token，再通过 `"refresh".equals(claims.get("type"))` 判断类型。同时移除对 `JwtUtil` 的全部依赖。此变更与设计 C3 修复方向一致。

#### [一般] UNAUTHORIZED 错误消息与设计规范不一致

- **位置**：`GlobalErrorCode.java:9`
- **描述**：设计文档 3.3 节和 6.3 节要求 UNAUTHORIZED 场景返回消息 "未认证或令牌已失效"，10.2 节 ErrorCode 表也明确 `message = "未认证或令牌已失效"`。但 `GlobalErrorCode.UNAUTHORIZED` 消息定义为 "未认证"，缺少 "或令牌已失效" 后缀。前端 401 拦截器可能依赖此消息文本进行判断，与设计规范偏离。
- **建议**：将 `GlobalErrorCode.UNAUTHORIZED` 的消息改为 `"未认证或令牌已失效"`，与设计文档一致。

#### [一般] JwtConfig.validate() 检查的是原始字符串长度而非解码后字节长度

- **位置**：`JwtConfig.java:55-58`
- **描述**：设计文档 4.7 节要求 "Base64 解码后的字节长度 ≥ 32"。但 `JwtConfig.validate()` 检查的是 `secret.length() < 32`（原始 Base64 字符串长度）。Base64 编码后长度大于解码后字节长度，原始字符串长 32 仅对应约 24 字节，不满足 HMAC-SHA256 的 256 位最小密钥要求。且 `JwtTokenProvider` 未做此项检查，若 `JwtUtil` 未来被移除，此缺陷将暴露。
- **建议**：将校验逻辑移至 `JwtTokenProvider.init()`（职责集中），使用 `Base64.getDecoder().decode(secret).length < 32` 检查。`JwtConfig` 仅保留非空检查。

#### [一般] RestAuthenticationEntryPoint 使用消息字符串匹配识别 ACCOUNT_DISABLED

- **位置**：`RestAuthenticationEntryPoint.java:27`
- **描述**：通过 `authException.getMessage().contains(ACCOUNT_DISABLED_MESSAGE)` 判断是否为禁用账户。若 `GlobalErrorCode.ACCOUNT_DISABLED` 的消息因国际化或多语言需求变更，此检测逻辑静默失效，禁用用户将收到 "UNAUTHORIZED" 而非正确的 "ACCOUNT_DISABLED" 响应。
- **建议**：推荐方案：自定义 `AuthenticationException` 子类（如 `AccountDisabledException`），携带 `GlobalErrorCode` 字段，使用 `instanceof` 判断。替代方案：在 `JwtAuthenticationFilter` 中将错误码存入 request attribute，EntryPoint 从 attribute 读取。

#### [一般] userId 提取逻辑重复——C3 修复未完成

- **位置**：`JwtAuthenticationFilter.java:142-150`；`JwtTokenProvider.java:93-101`
- **描述**：设计文档 8.1 节 C3 明确提出 "抽取 `JwtTokenProvider.getUserIdFromClaims(Claims)` 静态方法" 消除重复。当前 `JwtAuthenticationFilter.extractUserId()` 与 `JwtTokenProvider.getUserIdFromClaims()` 方法体完全一致（Integer→Long 转换逻辑）。Filter 未复用后者。
- **建议**：`JwtAuthenticationFilter.extractUserId(claims)` 改为调用 `JwtTokenProvider.getUserIdFromClaims(claims)`。若 Filter 改为依赖 `JwtTokenProvider`（见严重问题），此重复自然消除。

#### [一般] AuthController 直接注入 JwtTokenProvider 用于获取过期时间

- **位置**：`AuthController.java`（不在本模块范围，备注说明）
- **描述**：`AuthController` 注入了 `JwtTokenProvider` 仅为了调用 `getAccessTokenExpirationMs()`。`expiresIn` 字段的填充职责应属于 Service 层，Controller 不应感知 JWT 实现细节。
- **建议**：（转模块一审查关注）将 `expiresIn` 的计算移至 `AuthServiceImpl`，`LoginResponse` 由 Service 层构建后返回 Controller。

#### [轻微] GlobalRateLimitFilter 缺少 X-Real-IP 头支持

- **位置**：`GlobalRateLimitFilter.java:45-48`
- **描述**：设计文档在 "IP 维度失败计数器清空的前置假设" 脚注中要求：`GlobalRateLimitFilter` 和 `LoginAttemptTracker` 统一从 `RequestHelper.getClientIp()` 获取 IP，支持 `X-Forwarded-For` 和 `X-Real-IP` 两种代理头。当前 Filter 仅处理 `X-Forwarded-For`，不检查 `X-Real-IP`。在仅配置 `X-Real-IP` 的反向代理环境中会退化到 `request.getRemoteAddr()`（通常为代理内网 IP），导致全局限流失效。
- **建议**：优先检查 `X-Forwarded-For`，不存在时回退到 `X-Real-IP`，最后才是 `request.getRemoteAddr()`。考虑抽取公共 `RequestHelper.getClientIp(request)` 工具方法供两处复用。

#### [轻微] Refresh Token 包含设计未要求的 userType claim

- **位置**：`JwtTokenProvider.java:60`
- **描述**：设计文档 3.2 节定义的 Refresh Token claims 范围是 `{sub, userId, type, tokenVersion, iat, exp, jti}`，不包含 `userType`。当前实现额外写入了 `userType` claim。虽然不产生安全问题，但偏离了设计契约。
- **建议**：从 `generateRefreshToken()` 中移除 `userType` claim，保持与设计文档一致。

#### [轻微] jti 使用自定义 claim 而非 JJWT 标准 API

- **位置**：`JwtTokenProvider.java:44,63`
- **描述**：使用 `.claim("jti", jti)` 而非 JJWT Builder 的标准 `.id(jti)` 方法。功能等价但不符合标准用法，`JwtTokenProvider.getJtiFromToken()` 从 `claims.get("jti")` 读取时无异常。
- **建议**：改为 `.id(jti)`，读取端同样使用 `.id()` 对应的标准 claim 路径（JJWT 的 `Claims.getId()`）。

#### [轻微] 认证/授权异常缺少安全审计日志

- **位置**：`RestAuthenticationEntryPoint.java:25-40`；`RestAccessDeniedHandler.java:24-39`
- **描述**：两个 Handler 仅输出 JSON 响应，未记录任何安全日志。对于被禁用账户的访问尝试、权限不足的越权请求，运维监控系统无法感知。相比之下，`JwtAuthenticationFilter`（用户禁用时 `log.warn`）和 `PasswordChangeCheckFilter`（阻断时 `log.warn`）已输出安全日志。
- **建议**：在 `RestAuthenticationEntryPoint` 和 `RestAccessDeniedHandler` 中增加 `log.warn()` 输出，记录 request URI、认证失败原因/异常类型。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 2 |
| 一般 | 4 |
| 轻微 | 4 |

### 总评

Security & JWT 模块整体遵循了设计文档 3.2–3.3 节的核心契约：Claims 结构对齐、过期策略匹配、Filter 链顺序正确、PasswordChangeCheckFilter 白名单逻辑准确、GlobalRateLimitFilter 限流参数符合设计。当前最突出的架构问题有二：① `JwtTokenProvider` 的 `@PostConstruct` 启动验证缺失，在密钥误配置时会导致模糊异常；② `JwtAuthenticationFilter` 仍依赖旧 `JwtUtil` 而非新的 `JwtTokenProvider`，是设计文档 1.3 节和 3.3 节的显式违规。建议优先修复此二项以恢复设计一致性。其余问题集中在消息对齐、IP 提取和日志审计，改造量较小可一并处理。
