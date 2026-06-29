# 待办事项

---

- [ ] T1: [严重] Access Token 无 type claim 导致 JwtAuthenticationFilter 拒绝所有已认证请求 — 来源：review_v1_A + review_v2_F，位置：`JwtTokenProvider.java:52-63`
- [ ] T2: [严重] 异常刷新检测仅记录日志，未拒绝请求（log.warn 后仍返回新 Token） — 来源：review_v2_F，位置：`AuthServiceImpl.java:270-283`
- [ ] T3: [严重] 测试文件路径偏差：GlobalExceptionHandlerTest/GlobalErrorCodeTest/EntityMappingIT 实际路径与范围文件描述不一致 — 来源：review_v2_D
- [ ] T4: [严重] PasswordChangeCheckFilterTest 未测试 SecurityContext 存在但 principal 为 null/非 Long 类型场景 — 来源：review_v2_D，位置：`PasswordChangeCheckFilterTest.java`
- [ ] T5: [严重] UserFacadeImplTest 完全 mock UserConverter，无法发现真实转换逻辑缺陷 — 来源：review_v2_D，位置：`UserFacadeImplTest.java`
- [ ] T6: [严重] 缺失异常刷新检测（Suspicious Refresh Detection）的单元测试 — 来源：review_v2_D，位置：`AuthServiceTest.java`
- [ ] T7: [严重] 缺少 SecurityAuditLogger 写入失败降级路径的测试 — 来源：review_v2_D，位置：`LoggingSecurityAuditLoggerTest.java`

---

- [ ] T8: [一般] AuthServiceImpl.login() ACCOUNT_LOCKED args 与设计文档示例不一致（传递"30分钟"vs"请30分钟后重试"） — 来源：review_v1_A，位置：`AuthServiceImpl.java:104,110`
- [ ] T9: [一般] AuthServiceImpl.logout() 对过期 token 提前返回，跳过审计日志 — 来源：review_v1_A，位置：`AuthServiceImpl.java:183-185`
- [ ] T10: [一般] AuthServiceImpl.logout() 重复 JWT 解析（先 validateToken 获 claims，又 getJtiFromToken 重解析） — 来源：review_v1_A + review_v2_F，位置：`AuthServiceImpl.java:182,187`
- [ ] T11: [一般] AuthService.getCurrentUser(String token) 直接验证 token 参数而非依赖 SecurityContext — 来源：review_v1_A，位置：`AuthService.java:17, AuthServiceImpl.java:303-314`
- [ ] T12: [一般] JwtTokenProvider Base64 字符集校验使用标准 Base64 而非设计规定的 URL-safe 字符集 — 来源：review_v1_B，位置：`JwtTokenProvider.java:37-38`
- [ ] T13: [一般] SlidingWindowCounter 使用全局锁而非设计规定的"每个 IP 独立加锁" — 来源：review_v1_B，位置：`SlidingWindowCounter.java:36,53-55`
- [ ] T14: [一般] JwtUtil.generateToken 仍包含 role/position claims 且缺 jti，违反 3.2 节设计约束 — 来源：review_v1_C + review_v2_F，位置：`JwtUtil.java:71-89`
- [ ] T15: [一般] LoginAttemptTracker record* 方法缺少窗口过期防御（若绕过 isLocked 直接调用 record 可致永久锁定） — 来源：review_v1_C + review_v2_F，位置：`LoginAttemptTracker.java:32-49`
- [ ] T16: [一般] GlobalExceptionHandler.formatMessage() 对命名占位符先尝试 MessageFormat 产生异常开销 — 来源：review_v1_A + review_v2_F，位置：`GlobalExceptionHandler.java:38-48`
- [ ] T17: [一般] RestAuthenticationEntryPoint/RestAccessDeniedHandler 的 Result 未经过消息插值管线 — 来源：review_v2_F，位置：`RestAuthenticationEntryPoint.java:33, RestAccessDeniedHandler.java:34`
- [ ] T18: [一般] abnormal refresh 检测窗口（ConcurrentHashMap<Long, Deque<Long>>）无过期清理，不活跃用户条目永久驻留 — 来源：review_v2_F，位置：`AuthServiceImpl.java:270-283`
- [ ] T19: [一般] MenuController 直接操作 SecurityContextHolder 而非使用 CurrentUser 接口 — 来源：review_v2_E，位置：`MenuController.java:152-161`
- [ ] T20: [一般] GlobalExceptionHandlerTest 未覆盖消息插值管线回退路径（args=null、无占位符模板等） — 来源：review_v2_D，位置：`GlobalExceptionHandlerTest.java`
- [ ] T21: [一般] AuthServiceImpl.login() deleted 分支测试中预设 passwordEncoder.matches=true 但不必要的 mock — 来源：review_v2_D，位置：`AuthServiceTest.java:186-209`
- [ ] T22: [一般] AuthControllerTest PUT /api/auth/profile 使用路径 /me 而非 OOD 定义的 /profile — 来源：review_v2_D，位置：`AuthControllerTest.java:224-251`
- [ ] T23: [一般] AuthControllerTest.changePassword 未覆盖 SecurityContext 为空的异常路径 — 来源：review_v2_D，位置：`AuthControllerTest.java:258-291`
- [ ] T24: [一般] SlidingWindowCounterTest 并发测试使用 <= limit 而非严格验证并发下的精确限流 — 来源：review_v2_D，位置：`SlidingWindowCounterTest.java:84-103`
- [ ] T25: [一般] SlidingWindowCounterTest 锁释放测试通过反射间接验证，对实现变更脆弱 — 来源：review_v2_D，位置：`SlidingWindowCounterTest.java:127-152`
- [ ] T26: [一般] PasswordPolicyImplTest 缺少全字符集（4/4 类型）边界测试 — 来源：review_v2_D，位置：`PasswordPolicyImplTest.java`
- [ ] T27: [一般] UserConverterTest 缺少角色 sort=null + enabled=false 组合场景 — 来源：review_v2_D，位置：`UserConverterTest.java`
- [ ] T28: [一般] UserFacadeImplTest 未测试 Repository 查询失败（抛出异常）场景 — 来源：review_v2_D，位置：`UserFacadeImplTest.java`
- [ ] T29: [一般] CurrentUserImplTest 缺少 principal 类型不匹配（非 Long）测试 — 来源：review_v2_D，位置：`CurrentUserImplTest.java`
- [ ] T30: [一般] SecurityConfigPhase1Test Filter 注册顺序测试依赖反射操作 HttpSecurity.filterOrders，对版本变更脆弱 — 来源：review_v2_D，位置：`SecurityConfigPhase1Test.java:89-98`
- [ ] T31: [一般] EntityMappingIT 未覆盖 Role.enabled NOT NULL 约束验证 — 来源：review_v2_D，位置：`EntityMappingIT.java`
- [ ] T32: [一般] PasswordChangeRequest 缺少 oldPassword 边界值（1 字符最小长度）测试 — 来源：review_v2_D，位置：`PasswordChangeRequestTest.java`
- [ ] T33: [一般] MenuServiceTest shouldNotFilterDeletedInJavaLayer 测试名称语义与设计意图不符 — 来源：review_v2_D，位置：`MenuServiceTest.java:393-416`
- [ ] T34: [一般] RoleTest 缺少 sort 字段 NOT NULL 约束验证 — 来源：review_v2_D，位置：`RoleTest.java`
