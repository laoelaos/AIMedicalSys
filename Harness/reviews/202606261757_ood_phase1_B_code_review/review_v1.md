# R1: 模块一 - Auth 核心服务代码审查

审查时间：2026-06-26

### 审查范围

- `AuthController.java`
- `AuthService.java`
- `AuthServiceImpl.java`
- `LoginRequest.java`
- `RefreshTokenRequest.java`
- `PasswordChangeRequest.java`
- `ProfileUpdateRequest.java`
- `LoginResponse.java`
- `TokenRefreshResponse.java`
- `CurrentUser.java` (api)
- `UserFacade.java` (api)
- `UserInfoResponse.java` (api)
- `UserFacadeImpl.java`
- `UserConverter.java`

### 发现

#### [一般] 刷新流程中用户禁用/删除时未递增 IP 失败计数

- **位置**：`AuthServiceImpl.java:180-184`
- **描述**：OOD 3.1.3 步骤 7 规定：若用户已被禁用或被删除，需递增 LoginAttemptTracker IP 维度的失败计数，然后返回 TOKEN_REFRESH_FAILED。当前代码仅抛出 BusinessException，未调用 `loginAttemptTracker.recordIpFailure(clientIp)`。这降低了刷新场景下 IP 维度攻击检测的完整性。
- **建议**：在 throw 之前追加 `loginAttemptTracker.recordIpFailure(getClientIp())` 调用。

#### [一般] ProfileUpdate 端点路径与设计不一致

- **位置**：`AuthController.java:70`
- **描述**：OOD 4.4 保护清单和 6.1 接口清单均定义 `PUT /api/auth/profile` 为资料更新端点，但 Controller 使用 `@PutMapping("/me")`，实际映射为 `PUT /api/auth/me`。虽与 `GET /api/auth/me` 方法不冲突，但路径与设计契约不一致。
- **建议**：将 `@PutMapping("/me")` 改为 `@PutMapping("/profile")`，保持与设计文档一致。

#### [一般] changePassword 端点跳过 SecurityContext，直接依赖 JwtTokenProvider

- **位置**：`AuthController.java:82-91`
- **描述**：OOD 3.1.6 步骤 2 要求 JwtAuthenticationFilter 验证 Access Token（该端点为 authenticated），Controller 应从 SecurityContext（或 CurrentUser 接口）获取当前用户 ID。实际代码在 Controller 层直接调用 `jwtTokenProvider.validateToken()` 和 `jwtTokenProvider.getUserIdFromClaims()`，绕过了 SecurityContext。这与 M4 修复方案（引入 CurrentUser 接口，消除 Controller 对 JwtUtil/JwtTokenProvider 的直接依赖）不一致，也与其他方法（如 getCurrentUser/updateProfile 在 Service 层提取 token）的模式不一致。
- **建议**：统一模式，由 AuthServiceImpl.changePassword 接收 token 字符串，在 Service 层提取 userId；或从 SecurityContextHolder 获取当前用户 ID。

#### [一般] Logout 服务方法忽略 refreshToken 请求体

- **位置**：`AuthController.java:43-52`, `AuthService.java:13`, `AuthServiceImpl.java:147-164`
- **描述**：OOD 3.1.4 步骤 1 和 4.4 规定登出请求可选携带 `refreshToken` 字段（`@RequestBody(required=false)`），Phase 1 应记录安全日志。Controller 已接收 `refreshTokenRequest` 参数，但 `AuthService.logout(String token)` 接口未接收该参数，导致 refreshToken 被丢弃，无法记录安全日志。
- **建议**：`AuthService.logout` 方法签名增加 `RefreshTokenRequest refreshTokenRequest` 参数（允许 null），在实现中记录安全日志。

#### [一般] UserConverter 与 UserFacadeImpl 存在重复且不一致的转换逻辑

- **位置**：`UserConverter.java:35-44`, `UserFacadeImpl.java:57-66`
- **描述**：两处维护了几乎相同的 User → UserInfoResponse 转换逻辑（resolveRole/resolvePosition/resolvePermissions），但行为不一致：(a) UserConverter.resolveRole 未按 `Role::getEnabled` 过滤已禁用角色，UserFacadeImpl 有过滤；(b) UserConverter 使用 `Comparator.comparingInt(Role::getSort)`，若 sort 为 null 存在 NPE 风险，UserFacadeImpl 使用 `Comparator.nullsLast` 更安全；(c) UserConverter.resolvePermissions 未过滤 `PermissionFunction::getEnabled`，UserFacadeImpl 有过滤。此重复也违背了 M1/M6 修复（抽取为 UserConverter）的设计意图——UserFacadeImpl 应委托 UserConverter 完成转换。
- **建议**：UserFacadeImpl.toUserInfoResponse 直接调用 `userConverter.toUserInfoResponse(user)` 消除重复，并在 UserConverter 中统一补充 enabled 过滤和 null-safe sort 处理。

#### [轻微] 登录流程 dummy BCrypt 使用 encode() 而非 matches()

- **位置**：`AuthServiceImpl.java:105,113`
- **描述**：OOD 3.1.1 步骤 5/6 要求"对虚拟哈希值执行 dummy BCrypt 比对"以消除与步骤 7 的响应时间差异。代码使用 `passwordEncoder.encode("dummy")`（哈希生成）替代 `passwordEncoder.matches("dummy", dummyHash)`（比对操作）。虽然两者均能消耗 BCrypt 计算时间，但 encode() 产生随机 salt 和完整哈希，计算开销略高于 matches()，且未持有预计算的虚拟哈希值供比对。严格而言不符合设计描述的"比对"语义。
- **建议**：预计算一个虚拟 BCrypt 哈希（如 `$2a$10$...`），改为 `passwordEncoder.matches("dummy", dummyHash)`，更精确匹配设计语义且减少无意义的盐值生成开销。

#### [轻微] 刷新流程中锁定检查与禁用检查顺序与设计不一致

- **位置**：`AuthServiceImpl.java:180-190`
- **描述**：OOD 3.1.3 步骤 6（检查用户名锁定）应在步骤 7（检查用户禁用/删除）之前执行。当前代码先检查用户禁用/删除（步骤 7 语义），再检查锁定（步骤 6 语义）。此顺序差异不影响功能结果（最终均返回 TOKEN_REFRESH_FAILED），但与设计流程不一致。
- **建议**：调整检查顺序对齐设计流程：先检查 loginAttemptTracker.isUsernameLocked，再检查用户 enabled/deleted。

#### [轻微] UserConverter 未过滤已禁用角色的权限

- **位置**：`UserConverter.java:58-89`
- **描述**：resolvePermissions 方法收集 `user.getRoles() -> role.getPosts() -> post.getFunctions()` 的权限码，但未检查 `Role::getEnabled`。已禁用的角色仍可能贡献权限码，可能导致用户拥有不应有的权限。UserFacadeImpl.resolveRole 过滤了 enabled 角色但 UserConverter 未做。
- **建议**：在遍历 roles 时，先通过 `role.getEnabled()` 过滤已禁用角色。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 0 |
| 一般 | 5 |
| 轻微 | 3 |

### 总评

模块一整体实现质量良好，AuthServiceImpl 的登录、刷新、密码变更三大流程与 OOD 3.1 节的设计基本一致。核心安全机制（限流检查、锁定状态判断、tokenVersion 验证、passwordChangeRequired 阻断）均已按设计实现。主要问题集中在：(1) 刷新流程中用户禁用场景遗漏 IP 失败计数；(2) Controller 端点路径 `/me` vs `/profile` 不一致；(3) changePassword 方法对 SecurityContext 的使用模式不统一；(4) UserConverter 与 UserFacadeImpl 的转换逻辑重复且行为不一致。建议修复一般问题后再进行模块二（Security & JWT）的集成验证。
