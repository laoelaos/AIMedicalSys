# 诊断报告：OOD Phase1_B 实现审查问题分析

## 概述

基于审查报告 `Harness/reviews/202606261757_ood_phase1_B_code_review/todo.md` 所列 27 项问题（T1-T27），逐项核查代码实现与 OOD 设计文档 `Docs/05_ood_phase1_B.md` 的一致性。核查范围包括 `AIMedical/backend/modules/common-module/common-module-impl/` 下的实现代码及 `AIMedical/backend/common/` 下的基础设施代码。

---

## 逐项诊断

### T1: JwtTokenProvider@PostConstruct 缺少启动验证

- **判定：真实缺陷**
- **根因：实现编码偏差**
- **分析：**
  - `JwtTokenProvider.init()`（`auth/jwt/JwtTokenProvider.java:31-36`）直接调用 `Base64.getDecoder().decode(secret)`，未执行以下任一启动验证：
    - null/空值检查（secret 为 null 时 decode() 抛出 NPE，非规范 IllegalStateException）
    - Base64 解码后字节长度 >= 32 检查（`Keys.hmacShaKeyFor(keyBytes)` 虽隐式校验但异常信息为库默认文本，不符合 OOD 4.7 要求的明确消息）
    - Base64 URL-safe 字符集检查
  - `JwtUtil.init()`（`jwt/JwtUtil.java:46-60`）已完整实现 OOD 4.7 的所有三项验证，但 `JwtTokenProvider` 作为 OOD 指定的集中提供者未复用该逻辑。
  - **OOD 4.7 节要求：** secret 为空时、字节不足 32 时、含非法字符时均抛出明确的 `IllegalStateException`。
- **影响范围：** JwtTokenProvider 启动阶段，错误信息不清晰，不利于运维排障。

### T2: JwtAuthenticationFilter 依赖 JwtUtil（旧代码）而非 JwtTokenProvider

- **判定：真实缺陷**
- **根因：实现编码偏差**
- **分析：**
  - `JwtAuthenticationFilter`（`auth/security/JwtAuthenticationFilter.java:5,34,60`）注入并使用 `JwtUtil`（`jwt/JwtUtil.java`）完成 token 验证和解析。
  - OOD 1.3 明确将 `JwtTokenProvider` 定义为 JWT 令牌生成、解析、验证的集中提供者。
  - `JwtTokenProvider.validateToken()` 支持 type claim 验证（`validateToken(token, expectedType)`），但 Filter 未使用此方法，而是用 `JwtUtil.validateTokenAndGetClaims()`（无 type 过滤），然后在 Filter 层手动重复 type 检查逻辑（`auth/security/JwtAuthenticationFilter.java:68-74`）。
- **影响范围：** token 类型校验逻辑分散在两处，违反集中职责原则。

### T3: ACCOUNT_LOCKED 消息模板未解析，客户端收到模板原文

- **判定：真实缺陷**
- **根因：实现编码偏差**
- **分析：**
  - `GlobalErrorCode.ACCOUNT_LOCKED`（`common/.../GlobalErrorCode.java:13`）的消息定义为模板字符串 `"账户已锁定，请{锁定时间}后重试"`。
  - `AuthServiceImpl.login()` 在 IP 锁定和用户名锁定场景（`service/impl/AuthServiceImpl.java:95,99`）调用 `new BusinessException(GlobalErrorCode.ACCOUNT_LOCKED, "请30分钟后重试")` 传入动态参数，但 `BusinessException` 的 args 参数从未被消费。
  - `GlobalExceptionHandler.handleBusinessException()`（`common/.../GlobalExceptionHandler.java:27`）调用 `Result.fail(errorCode)` → `errorCode.getMessage()`，直接返回模板原文 `"账户已锁定，请{锁定时间}后重试"`，args 被丢弃。
  - **因果链：** OOD 10.2 定义了 ACCOUNT_LOCKED 的动态消息机制（IP 锁定 30 分钟 vs 用户名锁定 15 分钟），但 BusinessException → GlobalExceptionHandler → Result 的整个消息处理管线未实现模板参数插值。
- **影响范围：** 所有使用 ACCOUNT_LOCKED 并传入 args 的调用点（当前 login() 中两处）。

### T4: UNAUTHORIZED 消息与设计规范不一致

- **判定：真实缺陷**
- **根因：实现编码偏差**
- **分析：**
  - `GlobalErrorCode.UNAUTHORIZED`（`GlobalErrorCode.java:9`）消息为 `"未认证"`。
  - OOD 10.2 节明确规定 UNAUTHORIZED 消息为 `"未认证或令牌已失效"`，3.3 节 AuthenticationEntryPoint 行为契约亦有相同描述。
  - 两者不一致。
- **影响范围：** 所有返回 UNAUTHORIZED 错误的客户端响应消息。

### T5: 刷新流程中用户禁用/删除时未递增 IP 失败计数

- **判定：真实缺陷**
- **根因：实现编码偏差**
- **分析：**
  - `AuthServiceImpl.refreshToken()`（`service/impl/AuthServiceImpl.java:180-184`）在发现用户 `!enabled` 或 `deleted` 时直接抛出 `BusinessException(TOKEN_REFRESH_FAILED)`，未调用 `loginAttemptTracker.recordIpFailure(clientIp)`。
  - OOD 3.1.3 步骤 7 明确规定："若用户已被禁用或被删除，需递增 LoginAttemptTracker IP 维度的失败计数，然后返回 TOKEN_REFRESH_FAILED"。
  - 缺少此调用降低了刷新场景下 IP 维度攻击检测的完整性。
- **影响范围：** AuthServiceImpl.refreshToken() 中被禁用/删除用户的 IP 失败计数丢失。

### T6: ProfileUpdate 端点路径与设计不一致

- **判定：真实缺陷**
- **根因：实现编码偏差**
- **分析：**
  - `AuthController`（`controller/AuthController.java:70`）使用 `@PutMapping("/me")`，实际映射为 `PUT /api/auth/me`。
  - OOD 4.4 保护清单和 6.1 接口清单均定义资料更新端点为 `PUT /api/auth/profile`。
  - 路径与设计契约不一致。
- **影响范围：** 前端需调整请求路径，否则请求 404。

### T7: changePassword 端点跳过 SecurityContext，直接依赖 JwtTokenProvider

- **判定：真实缺陷**
- **根因：实现编码偏差**
- **分析：**
  - `AuthController.changePassword()`（`controller/AuthController.java:82-91`）在 Controller 层直接调用 `jwtTokenProvider.validateToken()` 和 `jwtTokenProvider.getUserIdFromClaims()` 获取用户 ID。
  - OOD 3.1.6 步骤 2 要求：JwtAuthenticationFilter 已验证 Access Token，Controller 应从 SecurityContext（或 CurrentUser 接口）获取当前用户 ID。
  - 此做法与 M4 修复方案不一致（M4 明确要求引入 CurrentUser 接口，Controller 通过 SecurityContext 获取用户 ID，消除对 JwtTokenProvider 的直接依赖）。
- **影响范围：** AuthController.changePassword() 端点，代码与 OOD 设计架构偏离。

### T8: Logout 服务方法忽略 refreshToken 请求体

- **判定：真实缺陷**
- **根因：实现编码偏差**
- **分析：**
  - `AuthService` 接口（`service/AuthService.java:13`）定义 `void logout(String token)`，未接收 refreshToken 参数。
  - `AuthServiceImpl.logout()`（`service/impl/AuthServiceImpl.java:147-164`）同样只接受 token，未处理 refreshToken。
  - `AuthController.logout()`（`controller/AuthController.java:43-52`）虽接收了 `refreshTokenRequest` 参数，但未将其传递给 `authService.logout()`。
  - OOD 3.1.4 步骤 1 和 4.4 规定：登出请求可选携带 refreshToken 字段，Phase 1 应记录安全日志。
- **影响范围：** 登出时 refreshToken 信息丢失，无法记录安全日志。

### T9: UserConverter 与 UserFacadeImpl 存在重复且不一致的转换逻辑

- **判定：真实缺陷**
- **根因：实现编码偏差**
- **分析：**
  两处维护了几乎相同的 `User → UserInfoResponse` 转换逻辑（`auth/converter/UserConverter.java:35-44` 和 `auth/UserFacadeImpl.java:57-66`），但存在三处不一致：
  1. **角色过滤器：** `UserConverter.resolveRole()` 未按 `Role::getEnabled` 过滤已禁用角色；`UserFacadeImpl.resolvePrimaryRole()` 有 `filter(Role::getEnabled)`。
  2. **NPE 风险：** `UserConverter.resolveRole()` 使用 `Comparator.comparingInt(Role::getSort)`（sort 为 null 时 NPE）；`UserFacadeImpl.resolvePrimaryRole()` 使用 `Comparator.nullsLast(Comparator.naturalOrder())`。
  3. **权限过滤器：** `UserConverter.resolvePermissions()` 未过滤 `PermissionFunction::getEnabled`；`UserFacadeImpl.resolvePermissions()` 有 `filter(f -> Boolean.TRUE.equals(f.getEnabled()))`。
- **影响范围：** 通过 UserConverter 获取的用户信息可能包含已禁用角色和已禁用权限，`UserFacadeImpl` 行为正确。

### T10: JwtConfig.validate() 检查的是原始字符串长度而非解码后字节长度

- **判定：真实缺陷**
- **根因：实现编码偏差**
- **分析：**
  - `JwtConfig.validate()`（`jwt/JwtConfig.java:55-58`）检查 `secret.length() < 32`——检查的是 Base64 原始字符串的字符长度。
  - OOD 4.7 节要求"Base64 解码后的字节长度 ≥ 32"（HMAC-SHA256 最小 256 位密钥）。
  - Base64 编码后长度约为解码后字节长度的 4/3 倍。原始字符串长 32 仅对应约 24 字节，不满足 256 位最小密钥要求。
  - 正确的检查应为 `Base64.getDecoder().decode(secret).length < 32`（`JwtUtil.init()` 已正确实现此检查）。
- **影响范围：** JWT 密钥强度检查标准过低，可能接受弱密钥。

### T11: RestAuthenticationEntryPoint 使用消息字符串匹配识别 ACCOUNT_DISABLED

- **判定：真实缺陷**
- **根因：实现编码偏差**
- **分析：**
  - `RestAuthenticationEntryPoint`（`auth/security/RestAuthenticationEntryPoint.java:16,27`）通过 `authException.getMessage().contains(ACCOUNT_DISABLED_MESSAGE)` 判断是否为禁用账户（`ACCOUNT_DISABLED_MESSAGE = GlobalErrorCode.ACCOUNT_DISABLED.getMessage()` = `"账户已被管理员停用"`）。
  - 若 `GlobalErrorCode.ACCOUNT_DISABLED` 的消息因国际化或多语言需求变更，此检测逻辑静默失效，禁用用户将收到 `"UNAUTHORIZED"` 而非正确的 `"ACCOUNT_DISABLED"` 响应。
  - OOD 3.3 规定 AuthenticationEntryPoint 的行为契约依赖错误码区分，但代码实现依赖消息字符串匹配，与设计意图不符。
- **影响范围：** ACCOUNT_DISABLED 错误识别对消息文本产生硬依赖。

### T12: userId 提取逻辑重复（C3 修复未完成）

- **判定：真实缺陷**
- **根因：实现编码偏差**
- **分析：**
  - `JwtAuthenticationFilter.extractUserId()`（`auth/security/JwtAuthenticationFilter.java:142-150`）与 `JwtTokenProvider.getUserIdFromClaims()`（`auth/jwt/JwtTokenProvider.java:93-101`）方法体完全一致（Integer→Long 转换逻辑）。
  - OOD 8.1 节 C3 明确提出抽取 `JwtTokenProvider.getUserIdFromClaims(Claims)` 消除重复。
  - Filter 未复用后者。
- **影响范围：** 若提取逻辑变更需修改两处。

### T13: AuthServiceImpl.login() 使用 encode() 替代 matches() 进行 dummy BCrypt 比对

- **判定：真实缺陷**
- **根因：实现编码偏差**
- **分析：**
  - `AuthServiceImpl.login()` 第 105 行：`passwordEncoder.encode("dummy")`。
  - OOD 3.1.1 节步骤 5/6 要求"对虚拟哈希值执行 dummy BCrypt 比对"以消除响应时间差异。
  - `encode()` 生成新 salt 并计算 hash，通常比 `matches()` 慢（BCrypt 的 salt 生成和 hash 计算是完整操作）；`matches("dummy", "dummy_hash")` 仅执行一次 BCrypt 比对，且语义上符合"比对"的设计意图。
  - `encode()` 可能引入反向的时序差异（比真实 matches() 场景更慢），且语义上不符合"比对"的设计意图。
- **影响范围：** 用户名不存在和用户禁用/删除场景的 dummy 操作时间开销不匹配设计意图。

### T14: SlidingWindowCounter 中 ReentrantLock 作用域不完整

- **判定：真实缺陷**
- **根因：OOD 设计与实现不一致**
- **分析：**
  - `SlidingWindowCounter`（`auth/rateLimit/SlidingWindowCounter.java:14`）的 `ReentrantLock` 仅在 `cleanup()` 方法中加锁（`auth/rateLimit/SlidingWindowCounter.java:54-60`），`tryAcquire()` 方法完全未使用该锁。
  - OOD 4.1 节要求"ReentrantLock 保护窗口内的排序集合"。
  - 实际 `tryAcquire()` 使用 `ConcurrentHashMap.compute()` 已经提供了原子性保证，锁在 tryAcquire 中并非必需。但 OOD 设计明确选择了 ReentrantLock 作为锁策略，实现仅部分遵循设计，存在锁策略不一致。
  - **补注：** 此问题的严重性为 Medium。`ConcurrentHashMap.compute()` 的原子性已满足 `tryAcquire` 的需求，锁缺失不影响正确性，但违反设计约定。

### T15: AuthServiceTest 缺少 deleted 用户登录场景

- **判定：真实缺陷**
- **根因：测试覆盖不完整**
- **分析：**
  - `AuthServiceTest`（`service/AuthServiceTest.java:141-155`）的 `login_shouldThrowUserDisabled()` 仅测试了 `enabled=false` 的禁用用户场景。
  - OOD 3.1.1 节步骤 6 明确要求 `deleted == true` 也应采用同等的安全策略（dummy BCrypt 比对 + 双维度失败计数 + LOGIN_FAILED 错误码）。
  - 从代码实现看，`AuthServiceImpl.login()` 第 112 行使用 `Boolean.TRUE.equals(user.getDeleted())` 检查 deleted 状态，其行为路径与 enabled=false 相同，但缺少独立测试覆盖。
- **影响范围：** deleted 用户登录场景的测试覆盖缺失。

### T16: LoginAttemptTrackerTest 缺少锁定消息内容验证

- **判定：真实缺陷**
- **根因：测试覆盖不完整**
- **分析：**
  - `LoginAttemptTrackerTest`（`auth/login/LoginAttemptTrackerTest.java`）验证了 `isUsernameLocked()` 和 `isIpLocked()` 的 true/false 返回值，但未验证锁定后返回给用户的错误消息内容。
  - OOD 10.2 节定义：IP 维度锁定返回 `"账户已锁定，请 30 分钟后重试"`，用户名维度锁定返回 `"账户已锁定，请 15 分钟后重试"`。
  - 消息内容的生成位于 `AuthServiceImpl.login()` 中（通过 `BusinessException` 的 args 传递），因此此测试缺口实际上跨越了 `LoginAttemptTrackerTest` 和 `AuthServiceTest` 两个测试类：前者未验证消息参数传递，后者未验证消息内容正确性。
- **影响范围：** 锁定场景的消息内容无测试断言。

### T17: MenuServiceTest 未覆盖多级菜单树构建

- **判定：真实缺陷**
- **根因：测试覆盖不完整**
- **分析：**
  - `MenuServiceTest`（`service/MenuServiceTest.java:75-114`）的 `getUserMenuTree` 测试仅包含单层菜单返回验证（无 parent-child 关系）。
  - OOD 5.2 节定义 `MenuResponse` 支持递归 `children` 结构；6.1 节定义 `/api/menu/tree` 返回树形菜单。
  - 当前测试未覆盖：
    - parent-child 关系的树构建逻辑
    - 同级排序（sort 字段）
    - 多级嵌套场景（如 3 层以上）
    - 多个父菜单、多个子菜单的混合场景
- **影响范围：** 树形菜单构建逻辑无测试覆盖。

### T18: SecurityConfigPhase1Test 未测试 Filter 执行顺序

- **判定：真实缺陷**
- **根因：测试覆盖不完整**
- **分析：**
  - `SecurityConfigPhase1Test`（`auth/security/SecurityConfigPhase1Test.java`）仅验证了各 Bean 是否被创建（非 null 断言）。
  - OOD 3.3 节明确规定了 Filter 执行顺序：`GlobalRateLimitFilter` 最先 → `JwtAuthenticationFilter` → `PasswordChangeCheckFilter` 最后。
  - 当前测试未通过 `HttpSecurity` 配置验证 Filter 链的注册顺序。顺序错误会导致安全漏洞（例如限流在 JWT 认证之后执行）。
- **影响范围：** Filter 链顺序无自动化验证保障。

### T19: 菜单更新端点使用 PUT 而非 PATCH 方法

- **判定：真实缺陷**
- **根因：实现编码偏差**
- **分析：**
  - `MenuController.update()`（`controller/MenuController.java:117`）使用 `@PutMapping("/{id}")`。
  - OOD 4.4 节和 6.1 节要求 `PATCH /api/menu/{id}` 实现局部更新语义（RFC 7231 §4.3.4）。
  - `MenuUpdateRequest` 采用 POJO + `@JsonInclude(NON_NULL)` 设计，明确为局部更新 PATCH 语义服务，与 HTTP 方法矛盾。
  - PUT 语义是完整替换，若客户端未提供所有字段，未提供的字段将被覆盖为空值（尽管当前实现中通过 null 检查避免了此问题）。
- **影响范围：** HTTP 方法语义与设计契约不一致。

### T20: 删除菜单时错误码使用 PARAM_INVALID 而非设计约定的 CHILDREN_EXIST

- **判定：真实缺陷**
- **根因：实现编码偏差**
- **分析：**
  - `MenuServiceImpl.deleteMenu()`（`service/impl/MenuServiceImpl.java:165`）使用 `GlobalErrorCode.PARAM_INVALID`。
  - OOD 10.1 节和 6.1 节要求有子菜单阻止删除时返回 `ErrorCode.CHILDREN_EXIST`（HTTP 400）。
  - 前端无法据此区分"参数错误"和"子菜单阻止删除"两种场景，无法给出针对性的用户提示。
- **影响范围：** 前端对删除失败场景的错误识别能力降低。

### T21: MenuController 缺少路径 id 与请求体 id 的一致性校验

- **判定：真实缺陷**
- **根因：实现编码偏差**
- **分析：**
  - `MenuController.update()`（`controller/MenuController.java:117-125`）接收路径参数 `{id}` 和请求体 `MenuUpdateRequest`，但未校验 `request.getId()` 与路径 `id` 是否一致。
  - OOD 5.2 节要求：`PATCH /api/menu/{id}` 的路径参数 `{id}` 与请求体中的 `id` 字段（若携带）必须相同，不一致时返回 400（PARAM_INVALID）。
  - 当前实现完全忽略了此一致性校验。
- **影响范围：** 路径 id 与请求体 id 不一致时静默接受路径 id，可能掩盖前端错误。

### T22: PermissionFunction 实体缺少 component 字段映射

- **判定：真实缺陷**
- **根因：实现编码偏差**
- **分析：**
  - `PermissionFunction` 实体（`permission/PermissionFunction.java`）未声明 `component` 字段。
  - `sys_function` 表存在 `component` 列（存储前端组件路径如 `Layout`、`system/user/index`），但实体未映射。
  - 导致：
    - `MenuResponse` 中的 `component` 始终为 `null`（`MenuServiceImpl.convertToMenuResponse()` 硬编码 `null`）
    - `MenuUpdateRequest` 中的 `component` 变更无法持久化
    - `data.sql` 中写入的 `component` 值无法被业务代码读取
- **影响范围：** 前端菜单渲染无法获取组件路径，菜单导航功能受限。

### T23: getUserMenuTree 未使用 @EntityGraph，存在 N+1 查询风险

- **判定：真实缺陷**
- **根因：实现编码偏差**
- **分析：**
  - `MenuServiceImpl.getUserMenuTree()`（`service/impl/MenuServiceImpl.java:44`）使用 `userRepository.findById(userId)`（无 EntityGraph），随后遍历 `user.getPosts()`（触发懒加载 N 次查询）和 `post.getFunctions()`（触发额外 N 次查询）。
  - `UserRepository`（`permission/UserRepository.java:16-17`）已定义 `findWithDetailsById` 方法带有 `@EntityGraph(attributePaths = {"roles", "posts"})`，但代码未使用。
  - 对于一个用户有 M 个岗位、每个岗位有 N 个功能的场景，查询次数为 `1 + M + M×N`。
- **影响范围：** 菜单树加载时性能低下，随数据量增长可导致明显的延迟。

### T25: RATE_LIMITED / ACCOUNT_LOCKED HTTP 状态码映射缺失 429

- **判定：真实缺陷**
- **根因：实现编码偏差**
- **分析：**
  - `GlobalExceptionHandler.resolveHttpStatus()`（`common/.../GlobalExceptionHandler.java:38-57`）仅映射了 `UNAUTHORIZED`(401)、`FORBIDDEN`(403)、`NOT_FOUND`(404)、`PARAM_INVALID`(400)、`SYSTEM_ERROR`(500) 五种错误码，其余全部走默认 `HttpStatus.BAD_REQUEST`(400)。
  - OOD 10.1 节规定：
    - `RATE_LIMITED` 应返回 HTTP 429
    - `ACCOUNT_LOCKED` 应返回 HTTP 429
    - `TOKEN_REFRESH_FAILED` 应映射到 401
  - 当前三种错误码均被映射为 400。
- **影响范围：** 前端无法通过 HTTP 状态码区分限流、锁定、刷新失败和普通参数错误。

### T26: AuthModuleConfig 与 SecurityConfigPhase1 存在重复的 TokenBlacklist Bean 定义

- **判定：真实缺陷**
- **根因：实现编码偏差**
- **分析：**
  - `AuthModuleConfig`（`auth/config/AuthModuleConfig.java:19-22`）定义了 `@Bean TokenBlacklist tokenBlacklist()`，无 profile 限制。
  - `SecurityConfigPhase1`（`auth/security/SecurityConfigPhase1.java:41-44`）定义了 `@Bean TokenBlacklist tokenBlacklist()`，且带有 `@Profile("phase1")`。
  - Spring Boot 3.x 默认禁止 bean 覆盖（`spring.main.allow-bean-definition-overriding=false`），当 phase1 profile 激活时两个同名的 `tokenBlacklist` Bean 定义冲突，导致 `BeanDefinitionOverrideException` 启动失败。
  - `RateLimitGuard` Bean 仅在 `AuthModuleConfig` 中定义，未出现重复。
- **影响范围：** phase1 profile 下应用启动失败。

### T27: FORBIDDEN 消息与设计约定不一致

- **判定：真实缺陷**
- **根因：实现编码偏差**
- **分析：**
  - `GlobalErrorCode.FORBIDDEN`（`GlobalErrorCode.java:10`）消息为 `"无权限"`。
  - OOD 10.2 节规定 FORBIDDEN 的错误消息为 `"无权限访问"`。
- **影响范围：** 授权失败时客户端收到的消息与设计规范不一致。

---

## 汇总

| 编号 | 判定 | 根因归属 | 影响模块 |
|------|------|---------|---------|
| T1 | 真实缺陷 | 实现编码偏差 | JwtTokenProvider |
| T2 | 真实缺陷 | 实现编码偏差 | JwtAuthenticationFilter |
| T3 | 真实缺陷 | 实现编码偏差 | GlobalExceptionHandler / GlobalErrorCode |
| T4 | 真实缺陷 | 实现编码偏差 | GlobalErrorCode |
| T5 | 真实缺陷 | 实现编码偏差 | AuthServiceImpl |
| T6 | 真实缺陷 | 实现编码偏差 | AuthController |
| T7 | 真实缺陷 | 实现编码偏差 | AuthController |
| T8 | 真实缺陷 | 实现编码偏差 | AuthService / AuthServiceImpl |
| T9 | 真实缺陷 | 实现编码偏差 | UserConverter / UserFacadeImpl |
| T10 | 真实缺陷 | 实现编码偏差 | JwtConfig |
| T11 | 真实缺陷 | 实现编码偏差 | RestAuthenticationEntryPoint |
| T12 | 真实缺陷 | 实现编码偏差 | JwtAuthenticationFilter |
| T13 | 真实缺陷 | 实现编码偏差 | AuthServiceImpl |
| T14 | 真实缺陷 | 设计与实现不一致 | SlidingWindowCounter |
| T15 | 真实缺陷 | 测试覆盖不完整 | AuthServiceTest |
| T16 | 真实缺陷 | 测试覆盖不完整 | LoginAttemptTrackerTest / AuthServiceTest |
| T17 | 真实缺陷 | 测试覆盖不完整 | MenuServiceTest |
| T18 | 真实缺陷 | 测试覆盖不完整 | SecurityConfigPhase1Test |
| T19 | 真实缺陷 | 实现编码偏差 | MenuController |
| T20 | 真实缺陷 | 实现编码偏差 | MenuServiceImpl |
| T21 | 真实缺陷 | 实现编码偏差 | MenuController |
| T22 | 真实缺陷 | 实现编码偏差 | PermissionFunction |
| T23 | 真实缺陷 | 实现编码偏差 | MenuServiceImpl |
| T25 | 真实缺陷 | 实现编码偏差 | GlobalExceptionHandler |
| T26 | 真实缺陷 | 实现编码偏差 | AuthModuleConfig / SecurityConfigPhase1 |
| T27 | 真实缺陷 | 实现编码偏差 | GlobalErrorCode |

**统计：** 27/27 项确认为真实缺陷（0 项误报）。其中 24 项根因归属于实现编码偏差，1 项归属于 OOD 设计与实现不一致（T14），3 项归属于测试覆盖不完整（T15/T16/T17/T18）。

**模式识别：** 缺陷集中在以下几类：
1. **GlobalErrorCode 消息定义偏差：** T4、T27 的消息文本与 OOD 10.2 不一致；T3 的消息模板机制未与异常处理管线集成。
2. **OOD 设计规范的编码遗漏：** T1、T5、T10、T21、T22 均为 OOD 明确要求但实现未遵循。
3. **旧代码残留：** T2、T12 反映 `JwtUtil` 旧代码未被 `JwtTokenProvider` 集中替换。
4. **重复代码：** T9（UserConverter vs UserFacadeImpl）、T12（userId 提取）、T26（TokenBlacklist Bean）。
5. **测试缺口：** T15-T18 四类场景缺少测试断言。
