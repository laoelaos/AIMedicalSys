# R1: Package B 核心认证服务实现审查 — 验证是否遵循 Docs/05_ood_phase1_B.md 设计

审查时间：2026-06-27

### 审查范围

- `common-module-impl/auth/service/impl/AuthServiceImpl.java`
- `common-module-impl/auth/service/AuthService.java`
- `common-module-impl/auth/controller/AuthController.java`
- `common-module-impl/auth/jwt/JwtTokenProvider.java`
- `common-module-impl/auth/jwt/JwtConfig.java`
- `common-module-impl/auth/jwt/JwtUtil.java`
- `common-module-impl/auth/UserFacadeImpl.java`
- `common-module-api/auth/UserFacade.java`
- `common-module-impl/auth/converter/UserConverter.java`
- `common-module-impl/auth/config/AuthModuleConfig.java`
- `common-module-api/auth/CurrentUser.java`
- `common-module-impl/auth/security/CurrentUserImpl.java`
- `common-module-api/auth/UserInfoResponse.java`
- `common-module-impl/dto/response/LoginResponse.java`
- `common-module-impl/auth/audit/SecurityAuditEvent.java`
- `common-module-impl/auth/audit/SecurityAuditLogger.java`

审查依据：`Docs/05_ood_phase1_B.md（第3节认证流程、第3.2节JWT令牌设计、第4.3节密码策略、第4.8节安全审计、第5.2节UserConverter契约）`

### 发现

#### [一般] AuthController 直接使用 SecurityContextHolder 而非注入 CurrentUser

- **位置**：`AuthController.java:86-99`
- **描述**：OOD 设计（1.3 节）明确定义了 `CurrentUser` 接口作为 Controller 层获取当前用户身份的轻量访问器，消除对 `SecurityContextHolder` 的直接操作。当前 `AuthController` 使用私有方法 `getCurrentUserId()` 直接调用 `SecurityContextHolder.getContext().getAuthentication()`，未注入 `CurrentUser`。这导致 `me()` 和 `changePassword()` 方法均绕过了设计约定的抽象层。
- **建议**：注入 `CurrentUser currentUser`，将 `getCurrentUserId()` 替换为 `currentUser.getUserId()`。

#### [一般] AuthController.logout() 安全审计在 Token 过期时丢失用户身份

- **位置**：`AuthController.java:42-50` | `AuthServiceImpl.java:177-208`
- **描述**：OOD 3.1.4 节规定 "AuthController.logout() 负责从 SecurityContext 获取当前用户标识（userId、username）"，但 Controller 仅传递原始 Token 给 Service。Service 内部通过 `jwtTokenProvider.validateToken(token, null)` 解析 Token 以提取用户信息。若 Token 已过期，`validateToken` 返回 null，导致审计日志记录 `userId=null, username=null`。此外，黑名单添加操作也会被跳过（claims 为 null 时的分支未执行黑名单写入）。
- **建议**：Controller 从 SecurityContext（或 CurrentUser）获取 userId/username 并传入 Service；Service 仍负责 Token 黑名单写入（可从 Token 中提取 jti），但审计日志的用户身份不由 Token 解析提供。

#### [一般] JwtConfig.validate() 使用标准 Base64 解码器而非 URL-safe

- **位置**：`JwtConfig.java:58`
- **描述**：OOD 4.7 节规定密钥必须为 URL-safe Base64 字符集（`A-Z a-z 0-9 - _`）。`JwtTokenProvider.init()` 正确使用 `Base64.getUrlDecoder().decode(secret)`。但 `JwtConfig.validate()` 使用 `Base64.getDecoder()`（标准 Base64，接受 `+/=` 字符）。此不一致导致：URL-safe 密钥中含 `_` 时，`JwtConfig.validate()` 会抛出 `IllegalArgumentException`（标准解码器不识别 `_`），虽然 `JwtTokenProvider.init()` 能正确解码。启动验证失败会阻止应用启动，功能被破坏。
- **建议**：将 `JwtConfig.validate()` 中的 `Base64.getDecoder().decode(secret)` 改为 `Base64.getUrlDecoder().decode(secret)`。

#### [一般] LoginResponse.expiresIn 传入毫秒值而非秒值

- **位置**：`AuthServiceImpl.java:171` | `JwtTokenProvider.java:21`
- **描述**：OOD 6.2 节和 5.2 节定义 `expiresIn` 语义为 "access token 固定有效期（秒）"，典型值 900（15 分钟）。但 `AuthServiceImpl.login()` 传入 `jwtTokenProvider.getAccessTokenExpirationMs()`，其返回值 `ACCESS_TOKEN_EXPIRATION_MS = 900_000L` 是毫秒值。前端收到 `expiresIn: 900000` 后会误判刷新时机为 900000 秒（约 10.4 天）而非 15 分钟。
- **建议**：将 `AuthServiceImpl.java:171` 的 `jwtTokenProvider.getAccessTokenExpirationMs()` 改为 `jwtTokenProvider.getAccessTokenExpirationMs() / 1000`，或在新增方法中返回秒值。

#### [一般] UserConverter.resolveRole() 使用 Role::getEnabled 方法引用可能 NPE

- **位置**：`UserConverter.java:41`
- **描述**：`roles.stream().filter(Role::getEnabled)` 中，`Role::getEnabled` 返回 `Boolean`，`Stream.filter()` 自动拆箱为 `boolean`。若某条 Role 记录的 `enabled` 字段为 null，将抛出 NullPointerException。同一类的 `resolvePermissions()` 方法已正确使用 `Boolean.TRUE.equals(function.getEnabled())` 进行 null-safe 判断。两处风格不一致。
- **建议**：将 `.filter(Role::getEnabled)` 改为 `.filter(role -> Boolean.TRUE.equals(role.getEnabled()))` 以保持一致性和 null 安全。

#### [一般] refreshTimestamps 缺少 ScheduledExecutorService 定期清理

- **位置**：`AuthServiceImpl.java:68`
- **描述**：OOD 4.2 节明确要求 `refreshTimestamps` 的过期条目通过"惰性清除（在每次插入新时间戳时移除窗口外的旧条目）和 ScheduledExecutorService 定期清理（每 60 秒扫描一次，移除所有窗口外的过期时间戳）两种方式回收内存"。当前代码仅实现了惰性清除（`compute` 闭包内的 `while` 循环），缺少 `ScheduledExecutorService` 周期性清理。对于"仅刷新一次后不再请求"的用户，其 `userId` → `Deque` 条目将永久驻留内存。
- **建议**：添加 `ScheduledExecutorService` 定期任务（如 `@PostConstruct` 中启动），每 60 秒遍历并清理过期条目。

#### [一般] refreshTimestamps 使用 ConcurrentHashMap.compute() 抛出 BusinessException

- **位置**：`AuthServiceImpl.java:272-287`
- **描述**：`ConcurrentHashMap.compute()` 的映射函数内抛出了 `BusinessException`（第 283 行）。`ConcurrentHashMap.compute()` 约定映射函数不应抛出运行时异常，抛出异常会导致计算原子性中止，Map 状态回滚。虽不会损坏 Map 内部结构，但违反了 API 契约，未来 JDK 版本可能改变行为。
- **建议**：将检查逻辑与修改逻辑分离——先在 `compute` 闭包外判断队列大小，再在 `compute` 闭包内执行添加操作；或使用两阶段模式：先检查（原子读）、条件满足时抛异常（不进入 compute），否则在 compute 内安全修改。

#### [轻微] 异常刷新检测位置与设计描述不完全一致

- **位置**：`AuthServiceImpl.java:272-287`
- **描述**：OOD 4.2 节指定异常刷新检测应"在步骤 3（验证 Refresh Token 有效性）之后、步骤 4（检查黑名单）之前"。当前实现将其放置在第 8 步（tokenVersion 比对之后）而非第 3 步之后。虽然功能正确（仍能检测异常刷新），但违反了设计文档的执行顺序约定，且增加了拒绝前的无用查库开销。
- **建议**：将异常刷新检测逻辑移至 `validateToken(token, "refresh")` 和 `getUserIdFromClaims(claims)` 之后、`userRepository.findById(userId)` 之前。

#### [轻微] CurrentUserImpl 的 getUsername()/getUserType() 每次调用均查库

- **位置**：`CurrentUserImpl.java:29-44`
- **描述**：OOD 1.3 节定义 `CurrentUser` 为"当前登录用户的轻量身份标识"和"SecurityContext 驱动的会话级访问"——暗示信息应从 SecurityContext 提取而非数据库加载。但 `getUsername()` 和 `getUserType()` 每次调用都执行 `userRepository.findById()` 全量查询，这与"轻量"设计意图不一致。
- **建议**：将 username 和 userType 存入 JWT claims 中的 `sub` 和 `userType` 字段，并在 JwtAuthenticationFilter 装配 `UsernamePasswordAuthenticationToken` 时将这些信息存入 `details` 或自定义 principal 对象，使 `CurrentUserImpl` 可直接从 Authentication 对象提取，无需查库。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 0 |
| 一般 | 7 |
| 轻微 | 2 |

### 总评

核心认证服务整体实现良好，登录/刷新/登出/密码变更流程的功能逻辑基本符合 OOD 设计。安全审计日志覆盖了所有关键事件点，UserConverter 正确实现了角色/权限解析规则，UserFacadeImpl 正确委托了 UserConverter。主要问题集中在与设计文档的具体约定偏差上（如 Controller 未使用 CurrentUser、expiresIn 单位错误、异常刷新检测位置、refreshTimestamps 缺少定时清理等），以及一处 Base64 解码器不匹配的配置 bug。这些问题修复成本较低，建议在 Phase 1 B 完成前解决。
