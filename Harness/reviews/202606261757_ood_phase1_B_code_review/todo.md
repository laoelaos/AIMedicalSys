# 待办事项

---

## 严重 (Critical)

- [ ] T1: JwtTokenProvider@PostConstruct 缺少启动验证
  - 位置：`JwtTokenProvider.java:31-36`
  - 描述：init() 方法直接调用 Base64.getDecoder().decode(secret)，未对 secret 做 null/空值检查，未验证 Base64 解码后字节长度 >= 32，未验证 Base64 URL-safe 字符集。设计文档 4.7 节要求 secret 为空时、字节不足 32 时、含非法字符时均抛出明确的 IllegalStateException。

- [ ] T2: JwtAuthenticationFilter 依赖 JwtUtil（旧代码）而非 JwtTokenProvider
  - 位置：`JwtAuthenticationFilter.java:5,34,38,60,118`
  - 描述：Filter 注入并使用 JwtUtil（旧工具类）完成 token 验证和提取。JwtTokenProvider 的 validateToken() 方法（含 type claim 验证逻辑）未被 Filter 使用，导致 token 类型检查与解析逻辑在 Filter 中重复实现。

- [ ] T3: ACCOUNT_LOCKED 消息模板未解析，客户端收到模板原文
  - 位置：`GlobalErrorCode.java:13`、`GlobalExceptionHandler.java:27`、`Result.java:38`
  - 描述：GlobalErrorCode.ACCOUNT_LOCKED 的消息定义为模板字符串"账户已锁定，请{锁定时间}后重试"。AuthServiceImpl 调用时传入动态参数，但 GlobalExceptionHandler.handleBusinessException() 调用 Result.fail(errorCode) 仅取 errorCode.getMessage()（模板原文），忽略 BusinessException 中存储的 args，客户端收到模板原文而非预期消息。

- [ ] T4: UNAUTHORIZED 消息与设计规范不一致
  - 位置：`GlobalErrorCode.java:9`
  - 描述：OOD 文档 10.2 节明确规定 UNAUTHORIZED 的错误消息为"未认证或令牌已失效"，3.3 节 AuthenticationEntryPoint 行为契约亦有相同描述。代码中该枚举消息定义为"未认证"，缺少"或令牌已失效"部分。

## 一般 (Medium)

### R1 - Auth 核心服务

- [ ] T5: 刷新流程中用户禁用/删除时未递增 IP 失败计数
  - 位置：`AuthServiceImpl.java:180-184`
  - 描述：OOD 3.1.3 步骤 7 规定：若用户已被禁用或被删除，需递增 LoginAttemptTracker IP 维度的失败计数，然后返回 TOKEN_REFRESH_FAILED。当前代码仅抛出 BusinessException，未调用 loginAttemptTracker.recordIpFailure(clientIp)，降低了刷新场景下 IP 维度攻击检测的完整性。

- [ ] T6: ProfileUpdate 端点路径与设计不一致
  - 位置：`AuthController.java:70`
  - 描述：OOD 4.4 保护清单和 6.1 接口清单均定义 PUT /api/auth/profile 为资料更新端点，但 Controller 使用 @PutMapping("/me")，实际映射为 PUT /api/auth/me，路径与设计契约不一致。

- [ ] T7: changePassword 端点跳过 SecurityContext，直接依赖 JwtTokenProvider
  - 位置：`AuthController.java:82-91`
  - 描述：OOD 3.1.6 步骤 2 要求 JwtAuthenticationFilter 验证 Access Token，Controller 应从 SecurityContext（或 CurrentUser 接口）获取当前用户 ID。实际代码在 Controller 层直接调用 jwtTokenProvider.validateToken() 和 jwtTokenProvider.getUserIdFromClaims()，绕过了 SecurityContext，与 M4 修复方案不一致。

- [ ] T8: Logout 服务方法忽略 refreshToken 请求体
  - 位置：`AuthController.java:43-52`, `AuthService.java:13`, `AuthServiceImpl.java:147-164`
  - 描述：OOD 3.1.4 步骤 1 和 4.4 规定登出请求可选携带 refreshToken 字段（@RequestBody(required=false)），Phase 1 应记录安全日志。Controller 已接收 refreshTokenRequest 参数，但 AuthService.logout(String token) 接口未接收该参数，导致 refreshToken 被丢弃，无法记录安全日志。

- [ ] T9: UserConverter 与 UserFacadeImpl 存在重复且不一致的转换逻辑
  - 位置：`UserConverter.java:35-44`, `UserFacadeImpl.java:57-66`
  - 描述：两处维护了几乎相同的 User → UserInfoResponse 转换逻辑，但行为不一致：(a) UserConverter.resolveRole 未按 Role::getEnabled 过滤已禁用角色，UserFacadeImpl 有过滤；(b) UserConverter 使用 Comparator.comparingInt(Role::getSort) 存在 NPE 风险，UserFacadeImpl 使用 Comparator.nullsLast 更安全；(c) UserConverter.resolvePermissions 未过滤 PermissionFunction::getEnabled，UserFacadeImpl 有过滤。

### R2 - Security & JWT

- [ ] T10: JwtConfig.validate() 检查的是原始字符串长度而非解码后字节长度
  - 位置：`JwtConfig.java:55-58`
  - 描述：设计文档 4.7 节要求"Base64 解码后的字节长度 ≥ 32"。但 JwtConfig.validate() 检查的是 secret.length() < 32（原始 Base64 字符串长度）。Base64 编码后长度大于解码后字节长度，原始字符串长 32 仅对应约 24 字节，不满足 HMAC-SHA256 的 256 位最小密钥要求。

- [ ] T11: RestAuthenticationEntryPoint 使用消息字符串匹配识别 ACCOUNT_DISABLED
  - 位置：`RestAuthenticationEntryPoint.java:27`
  - 描述：通过 authException.getMessage().contains(ACCOUNT_DISABLED_MESSAGE) 判断是否为禁用账户。若 GlobalErrorCode.ACCOUNT_DISABLED 的消息因国际化或多语言需求变更，此检测逻辑静默失效，禁用用户将收到"UNAUTHORIZED"而非正确的"ACCOUNT_DISABLED"响应。

- [ ] T12: userId 提取逻辑重复（C3 修复未完成）
  - 位置：`JwtAuthenticationFilter.java:142-150`；`JwtTokenProvider.java:93-101`
  - 描述：设计文档 8.1 节 C3 明确提出抽取 JwtTokenProvider.getUserIdFromClaims(Claims) 消除重复。当前 JwtAuthenticationFilter.extractUserId() 与 JwtTokenProvider.getUserIdFromClaims() 方法体完全一致（Integer→Long 转换逻辑），Filter 未复用后者。

### R3 - 支持基础设施

- [ ] T13: AuthServiceImpl.login() 使用 encode() 替代 matches() 进行 dummy BCrypt 比对
  - 位置：`service/impl/AuthServiceImpl.java:105`
  - 描述：设计文档 3.1.1 节步骤 5/6 要求"对虚拟哈希值执行 dummy BCrypt 比对"以消除响应时间差异。代码使用了 passwordEncoder.encode("dummy") 而非 passwordEncoder.matches("dummy", "dummy_hash")。encode() 会生成新 salt 并计算 hash，通常比 matches() 慢，可能引入反向的时序差异且语义上不符合"比对"的设计意图。

- [ ] T14: SlidingWindowCounter 中 ReentrantLock 作用域不完整
  - 位置：`auth/rateLimit/SlidingWindowCounter.java:14`
  - 描述：设计文档 4.1 节要求"ReentrantLock 保护窗口内的排序集合"，但代码中 lock 仅在 cleanup() 方法中加锁，tryAcquire() 方法完全未使用该锁，存在锁策略不一致。

### R4 - 测试覆盖

- [ ] T15: AuthServiceTest 缺少 deleted 用户登录场景
  - 位置：`service/AuthServiceTest.java:141-155`
  - 描述：测试覆盖了 enabled=false 的禁用用户场景，但设计文档 3.1.1 节步骤 6 明确要求 deleted == true 也应采用同等的安全策略（dummy BCrypt 比对 + 双维度失败计数 + LOGIN_FAILED 错误码），该场景未单独测试。

- [ ] T16: LoginAttemptTrackerTest 缺少锁定消息内容验证
  - 位置：`auth/login/LoginAttemptTrackerTest.java:41-48, 82-89`
  - 描述：测试验证了 isUsernameLocked() 和 isIpLocked() 返回 true/false，但未验证锁定后返回给用户的错误消息内容。设计文档 10.2 节定义：IP 维度锁定返回"账户已锁定，请 30 分钟后重试"，用户名维度锁定返回"账户已锁定，请 15 分钟后重试"。

- [ ] T17: MenuServiceTest 未覆盖多级菜单树构建
  - 位置：`service/MenuServiceTest.java:75-114`
  - 描述：getUserMenuTree 测试仅包含单层菜单返回验证。设计文档 5.2 节定义 MenuResponse 支持递归 children 结构，6.1 节定义 /api/menu/tree 返回树形菜单。当前测试未覆盖 parent-child 关系的树构建逻辑、同级排序、多级嵌套场景。

- [ ] T18: SecurityConfigPhase1Test 未测试 Filter 执行顺序
  - 位置：`auth/security/SecurityConfigPhase1Test.java`
  - 描述：当前测试仅验证了各 Bean 是否被创建（非 null），但未验证 Filter 链的注册顺序。设计文档 3.3 节明确规定了 Filter 执行顺序：GlobalRateLimitFilter 最先 → JwtAuthenticationFilter → PasswordChangeCheckFilter 最后。顺序错误会导致安全漏洞。

### R5 - 实体/仓库/Schema & 菜单模块

- [ ] T19: 菜单更新端点使用 PUT 而非 PATCH 方法
  - 位置：`MenuController.java:117`
  - 描述：设计文档 4.4 节和 6.1 节要求 PATCH /api/menu/{id} 实现局部更新语义（RFC 7231 §4.3.4），但 Controller 使用 @PutMapping("/{id}")。PUT 语义是完整替换，与 MenuUpdateRequest 的局部更新设计（省略字段保持不变的 PATCH 策略）矛盾。

- [ ] T20: 删除菜单时错误码使用 PARAM_INVALID 而非设计约定的 CHILDREN_EXIST
  - 位置：`MenuServiceImpl.java:165`
  - 描述：设计文档 10.1 节和 6.1 节要求有子菜单阻止删除时返回 ErrorCode.CHILDREN_EXIST（HTTP 400），实现中使用了 GlobalErrorCode.PARAM_INVALID。前端无法据此区分"参数错误"和"子菜单阻止删除"两种场景。

- [ ] T21: MenuController 缺少路径 id 与请求体 id 的一致性校验
  - 位置：`MenuController.java:117-125`
  - 描述：设计文档 5.2 节要求 PATCH /api/menu/{id} 的路径参数 {id} 与请求体中的 id 字段（若携带）必须相同，不一致时返回 400（PARAM_INVALID）。当前 update 方法未实现此校验。

- [ ] T22: PermissionFunction 实体缺少 component 字段映射
  - 位置：`PermissionFunction.java`
  - 描述：sys_function 表存在 component 列（存储前端组件路径如 Layout、system/user/index），但 PermissionFunction 实体未映射该字段。导致 MenuResponse 中的 component 始终为 null；MenuUpdateRequest 中的 component 变更无法持久化；data.sql 中写入的 component 值无法被业务代码读取。

- [ ] T23: getUserMenuTree 未使用 @EntityGraph，存在 N+1 查询风险
  - 位置：`MenuServiceImpl.java:44`
  - 描述：getUserMenuTree() 使用 userRepository.findById(userId)（无 EntityGraph），随后遍历 user.getPosts()（触发懒加载 N 次查询）和 post.getFunctions()（触发额外 N 次查询）。UserRepository 已定义 findWithDetailsById 带有 @EntityGraph，但未被使用。

### R6 - 集成验证

- [ ] T25: RATE_LIMITED / ACCOUNT_LOCKED HTTP 状态码映射缺失 429
  - 位置：`GlobalExceptionHandler.java:38-57`
  - 描述：resolveHttpStatus 方法仅映射了 UNAUTHORIZED(401)、FORBIDDEN(403)、NOT_FOUND(404)、PARAM_INVALID(400)、SYSTEM_ERROR(500) 五种错误码，其余全部走默认 HttpStatus.BAD_REQUEST（400）。OOD 10.1 节规定 RATE_LIMITED 和 ACCOUNT_LOCKED 应返回 HTTP 429，TOKEN_REFRESH_FAILED 应映射到 401。

- [ ] T26: AuthModuleConfig 与 SecurityConfigPhase1 存在重复的 TokenBlacklist Bean 定义
  - 位置：`AuthModuleConfig.java:20-22`、`SecurityConfigPhase1.java:42-44`
  - 描述：AuthModuleConfig（无 profile 限制）和 SecurityConfigPhase1（@Profile("phase1")）均定义了名为 tokenBlacklist 的 TokenBlacklist Bean。Spring Boot 3.x 默认禁止 bean 覆盖，当 phase1 profile 激活时会导致 BeanDefinitionOverrideException 启动失败。

- [ ] T27: FORBIDDEN 消息与设计约定不一致
  - 位置：`GlobalErrorCode.java:10`
  - 描述：OOD 10.2 节规定 FORBIDDEN 的错误消息为"无权限访问"，但代码中定义为 FORBIDDEN("FORBIDDEN", "无权限")。
