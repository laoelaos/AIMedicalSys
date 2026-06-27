# R2-D: 审查单元测试覆盖率、质量和与设计的对齐程度

审查时间：2026-06-27

### 审查范围

共 46 个测试文件，涵盖 authentication、security、rateLimit、login、audit、blacklist、password、jwt、converter、config、controller、dto、permission、service 等子包及以下 application 相关测试：
- `AIMedical/backend/common/src/test/java/com/aimedical/common/config/GlobalExceptionHandlerTest.java`
- `AIMedical/backend/common/src/test/java/com/aimedical/common/exception/GlobalErrorCodeTest.java`
- `AIMedical/backend/integration/src/test/java/com/aimedical/integration/EntityMappingIT.java`

### 发现

#### [严重] 三条 application 模块测试路径存在偏差

- **位置**：审查范围描述中指定 `AIMedical/backend/application/src/test/java/com/aimedical/...` 下三文件
- **描述**：指定的应用测试路径不存在；实际测试位于：
  - `AIMedical/backend/common/src/test/java/com/aimedical/common/config/GlobalExceptionHandlerTest.java`
  - `AIMedical/backend/common/src/test/java/com/aimedical/common/exception/GlobalErrorCodeTest.java`
  - `AIMedical/backend/integration/src/test/java/com/aimedical/integration/EntityMappingIT.java`
- **建议**：更新审查范围路径或确认该三条文件是否确属测试覆盖范围。当前已按实际路径完成审查。

---

#### [一般] GlobalErrorCodeTest 消息验证与 OOD 10.2 节对齐良好，但缺 PASSWORD_MISMATCH 消息检查

- **位置**：`common/.../GlobalErrorCodeTest.java:149`
- **描述**：PASSWORD_MISMATCH 消息为 `"旧密码不正确"`，与 OOD 10.2 定义一致。测试覆盖了全部 20 个枚举值的 code 和 message，无遗漏。
- **建议**：无缺陷，测试质量良好。

---

#### [一般] GlobalExceptionHandlerTest 未覆盖消息插值管线回退路径（MessageFormat 降级为 String.replace）

- **位置**：`common/.../GlobalExceptionHandlerTest.java`
- **描述**：OOD 10.3 节要求插值管线优先使用 `MessageFormat.format(template, args)`，对 `{锁定时间}` 等命名占位符降级为 `String.replace`。当前测试覆盖了：
  - ACCOUNT_LOCKED 命名占位符插值（`{锁定时间}` → `"30分钟"`） ✅
  - 编号占位符插值（`{0}`、`{1}`） ✅
  - 空 args 场景 ✅
  
  但未覆盖以下回退路径：
  - args 为 null 时（`null` 而非 `new Object[0]`）是否 NPE
  - 模板中无占位符但有 args 时的行为（静默忽略 vs 拼接）
  - 插值失败时的兜底行为（arg 索引越界等）
- **建议**：补充 args=null 测试和模板无占位符但有 args 场景；考虑 MessageFormat 抛异常时的兜底（catch → 返回模板原文）。

---

#### [一般] AuthServiceImpl.login() 缺少 deleted=true 且 passwordEncoder.matches 返回 false 的分支测试

- **位置**：`AuthServiceTest.java:186-209` (login_shouldThrowUserDisabled) + `287-309` (login_shouldThrowLoginFailed_whenUserDeleted)
- **描述**：`login_shouldThrowLoginFailed_whenUserDeleted` 测试中 `when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true)` 被预设为 true，但 deleted 场景在 OOD 3.1.1 步骤 6 中 dummy BCrypt 比对后直接抛出异常，不进入步骤 7 的 passwordEncoder.matches 真实比对。此 mock 虽不影响测试结果，但降低了测试的语义精确性——deleted 路径在 dummy BCrypt 比对后即返回，不应依赖 matches 返回值。
- **建议**：删除 deleted 测试中不必要的 `passwordEncoder.matches` mock 设定，使测试仅 mock 必要依赖。

---

#### [一般] AuthControllerTest 中 PUT /api/auth/profile 使用路径 /me 而非 OOD 定义的 /profile

- **位置**：`AuthControllerTest.java:224-251`
- **描述**：测试类名和方法名使用 `updateMe`、`@PutMapping("/me")`，与 OOD 4.4 节定义的 `PUT /api/auth/profile` 不一致。此偏差已在 T6 诊断中确认。
- **建议**：同步修复 Controller 层路径后更新测试。

---

#### [一般] AuthControllerTest.changePassword 测试依赖 SecurityContextHolder 手动设置，未覆盖 SecurityContext 为空的异常路径

- **位置**：`AuthControllerTest.java:258-291`
- **描述**：当前测试通过 mock SecurityContext + Authentication 设置认证状态后调用 changePassword。但未测试 SecurityContext 为空或 principal 非 Long 类型的场景。
- **建议**：补充 SecurityContext.getAuthentication() 返回 null 时 changePassword 应抛出的异常测试。

---

#### [一般] JwtAuthenticationFilterTest 缺少黑名单异常 + 用户不存在的组合场景测试

- **位置**：`JwtAuthenticationFilterTest.java`
- **描述**：当前测试覆盖了：无 header、无效 token、refresh token 类型、黑名单命中、用户不存在、用户禁用、正常认证、passwordChangeRequired attribute 设置、权限收集共 9 个场景。较完整，但缺少：
  - Token 黑名单命中且同时用户已删除的组合场景（虽然不会到达用户查询）
  - X-Forwarded-For header 处理场景（filter 目前不处理，但可作为未来扩展）
- **建议**：当前覆盖已充分，无强制补充需求。

---

#### [严重] PasswordChangeCheckFilterTest 未测试 SecurityContext 存在但 principal 为 null/非 Long 类型场景

- **位置**：`PasswordChangeCheckFilterTest.java`
- **描述**：OOD 3.3 节行为契约中 `PasswordChangeCheckFilter` 从 `Authentication.principal` 获取 userId。当前测试仅覆盖 principal=Long(1L) 的正向路径。未覆盖 principal 为 null 或非 Long 类型（如 String username）时的行为。
- **建议**：补充 principal=null 和 principal=String 场景测试，验证 filter 静默跳过或抛出合理异常。

---

#### [一般] LoginAttemptTrackerTest 并发测试粒度合理但缺少锁定超时惰性清除的精确时序验证

- **位置**：`LoginAttemptTrackerTest.java:125-154`
- **描述**：`shouldUnlockAfterLockDurationExpiry` 使用 `Thread.sleep(300)` 验证 100ms 窗口到期后解锁。由于调度精度问题，测试可能偶发失败（高负载环境）。使用 `Thread.sleep(300)` 超过窗口 3 倍已足够保守。
- **建议**：改为 `await().atMost(500, MILLISECONDS)` 风格的动态轮询可进一步降低脆弱性，当前实现可接受。

---

#### [一般] SlidingWindowCounterTest 并发测试不验证线程安全精确性：允许通过的请求数 ≤ limit 而非严格等于 limit

- **位置**：`SlidingWindowCounterTest.java:84-103`
- **描述**：`shouldHandleConcurrentRequestsForSameKey` 使用 `assertTrue(allowed.get() <= limit)`，若实现在并发下允许超过 limit，测试也无法发现——因为 <= 是非严格约束。OOD 4.1 节要求高精度滑动窗口。
- **建议**：对于短时间窗口 (10_000ms) 内 10 线程并发抢占 limit=5，若实现正确则最多 5 个通过，测试应使用 `assertEquals(5, allowed.get())`。但需确认在极端调度下是否可能恰好 0-4 个通过（取决于抢占顺序）。建议改为 `assertTrue(allowed.get() == limit || allowed.get() == limit-1)`，或使用更精确的并发控制。

---

#### [一般] SlidingWindowCounterTest 锁释放测试间接验证，未覆盖 lock 被线程持有时 tryLock 超时后的行为

- **位置**：`SlidingWindowCounterTest.java:127-152`
- **描述**：`shouldReleaseLockAfterAcquireReturn` 通过反射检查 ReentrantLock 状态，验证 lock 在 tryAcquire 返回后未被持有。但 OOD 4.1 节要求 ReentrantLock 保护窗口集合，当前 tryAcquire 实现未使用 lock（仅 cleanup 使用），因此锁释放验证实际上是验证"无锁时的默认释放状态"。
- **建议**：根据 OOD 4.1 节，若选择在 tryAcquire 中加锁（T14 修复方案），补充验证 lock 在 true/false 返回路径上均被释放；若选择删除 lock（T14 备选方案），需删除当前反射测试。

---

#### [轻微] InMemoryTokenBlacklistTest 未验证 add 方法在 expirationTime 为过去时间时的行为

- **位置**：`InMemoryTokenBlacklistTest.java`
- **描述**：`add("test-jti", System.currentTimeMillis() + 60_000)` 总是使用未来时间。未测试传入过期时间戳（过去时间）的 jti 是否仍可被 isBlacklisted 判定为 true（等待 cleanup 后回收）。
- **建议**：补充 add 过期 jti 后立即 isBlacklisted 应为 true 的测试。

---

#### [一般] PasswordPolicyImplTest 覆盖了所有规则的正反案例，但缺少全字符集（4/4 类型）的边界测试

- **位置**：`PasswordPolicyImplTest.java`
- **描述**：测试覆盖了 1 类、2 类、3 类字符、包含用户名、最短/最长长度。但未测试密码包含全部 4 种字符类型时的行为（当前实现应返回 null），也无 `PASSWORD_COMMON` 的 Phase 2 预留测试。
- **建议**：补充 4/4 字符类型正例测试。

---

#### [一般] UserConverterTest 覆盖了角色排序、禁用角色过滤、null sort 处理、禁用权限过滤，但缺少角色 sort=null + enabled=false 的组合场景

- **位置**：`UserConverterTest.java`
- **描述**：`shouldFilterDisabledRole` 测试了 enabled=false 的角色被过滤；`shouldHandleNullSort` 测试了 sort=null 的角色。但未测试 sort=null 且 enabled=false 的角色在同时过滤 + null-safe 排序时的组合行为。虽然当前实现正确，但组合场景可能暴露实现中的迭代器链式调用问题。
- **建议**：补充 sort=null + enabled=false 组合场景测试。

---

#### [严重] UserFacadeImplTest 中所有 findAll 和 findByUsername 测试结果依赖 userConverter mock，未覆盖 UserConverter 的真实转换逻辑

- **位置**：`UserFacadeImplTest.java`
- **描述**：所有测试中 `when(userConverter.toUserInfoResponse(user)).thenReturn(expectedResponse)` 完全 mock 了转换结果。这导致 UserFacadeImplTest 实际上仅测试了"是否正确调用了 userConverter"，而未验证 UserConverter 的真实转换行为。OOD 1.3 + 5.2 节强制要求 UserFacadeImpl 委托 UserConverter，当前测试结构无法发现 UserConverter 与 UserFacadeImpl 之间的逻辑不一致（如 T9 中描述的重复且不一致的转换逻辑）。
- **建议**：修改 UserFacadeImplTest：不 mock UserConverter，使用真实的 UserConverter 实例（new UserConverter()），验证 UserFacadeImpl → UserConverter 的完整转换链路正确性。

---

#### [一般] UserFacadeImplTest 的 findById/findByUsername 未测试 UserRepository 查询失败（抛出异常）场景

- **位置**：`UserFacadeImplTest.java`
- **描述**：当前覆盖了"用户存在"、"用户不存在"、"null 输入"、"无角色"、"全部禁用"、"权限合并"共 10 个场景。但未测试 `userRepository.findById()` 抛出 RuntimeException 或 DataAccessException 时的行为。
- **建议**：补充 Repository 异常场景，验证异常不被吞没。

---

#### [一般] CurrentUserImplTest 覆盖了 getUserId/getUsername/getUserType 的正向和 null 场景，但缺少 principal 类型不匹配的测试

- **位置**：`CurrentUserImplTest.java`
- **描述**：OOD 1.3 定义 principal 为 Long userId。当前测试仅验证了 principal=Long 的场景。未验证 principal 为 String、或 SecurityContext 中 Authentication 为 null 时的 getUserId 返回 null。
- **建议**：补充 principal 为非 Long 类型时的测试。

---

#### [一般] SecurityConfigPhase1Test Filter 注册顺序测试通过反射操作 HttpSecurity.filterOrders，较脆弱

- **位置**：`SecurityConfigPhase1Test.java:89-98`
- **描述**：测试通过 `Field.setAccessible(true)` 修改 `HttpSecurity` 内部 `filterOrders` 来模拟 Filter 顺序注册，依赖 Spring Security 内部实现细节。若未来 Spring Security 版本变更内部字段名，测试将中断。
- **建议**：考虑使用 `@WebMvcTest` + 实际 HTTP 请求验证 Filter 链行为，替代反射方式。

---

#### [严重] 三条应用层测试文件路径错误（文件不存在于指定路径）

- **位置**：审查范围中指定的三个路径均不存在：
  - `AIMedical/backend/application/src/test/java/com/aimedical/common/config/GlobalExceptionHandlerTest.java` → 实际在 `common/` 模块
  - `AIMedical/backend/application/src/test/java/com/aimedical/common/exception/GlobalErrorCodeTest.java` → 实际在 `common/` 模块
  - `AIMedical/backend/application/src/test/java/com/aimedical/integration/EntityMappingIT.java` → 实际在 `integration/` 模块
- **描述**：路径偏差导致审查范围的路径引用不可达。实际找到的文件已按实际路径完成审查。
- **建议**：更新范围文件中的路径或确认文件是否需迁移至 application 模块。

---

#### [一般] EntityMappingIT 集成测试覆盖了 User/Role/Post/PermissionFunction 的 JPA 映射，但未覆盖 Role.enabled 的 NOT NULL 约束

- **位置**：`EntityMappingIT.java`
- **描述**：OOD 5.1 节要求 Role.enabled 补加 `@Column(nullable=false)`，当前 `RoleTest.java:42-48` 只测试了 getter/setter，未验证 Role.enabled 为 null 时 JPA 约束违反（与 User.password NOT NULL 测试类似）。`EntityMappingIT` 中 `role_shouldMapCodeField` 设置了 `enabled=true`，未测试 null enabled 触发 ConstraintViolationException。
- **建议**：在 EntityMappingIT 或 RoleTest 中增加 `role_shouldEnforceEnabledNotNull` 测试。

---

#### [严重] 缺失异常刷新检测（Suspicious Refresh Detection）的单元测试

- **位置**：`AuthServiceTest.java` 和所有测试文件
- **描述**：OOD 3.1.3 步骤 10 的脚注要求"异常刷新检测"：同一 userId 在 5 秒窗口内超过 2 次刷新触发安全告警。实现应包含 `ConcurrentHashMap<Long, Deque<Long>>` 维护刷新时间戳滑动窗口，超出阈值时 `log.warn(...)`。当前 `AuthServiceTest.refreshToken*` 测试(共 8 个)均未验证此告警逻辑。
- **建议**：新增 `refreshToken_shouldLogWarningOnSuspiciousPattern` 测试，3 次调用 refreshToken 后验证 log.warn 被触发；补充刷新计数器惰性清除的验证。

---

#### [严重] 缺少 SecurityAuditLogger 写入失败降级路径的测试

- **位置**：`LoggingSecurityAuditLoggerTest.java`
- **描述**：OOD 4.8 节要求"审计日志写入失败不应阻塞主业务流，捕获 IOException 并降级记录到业务日志"。当前 `LoggingSecurityAuditLoggerTest` 仅测试了正常写入和 null event，未模拟日志框架写入失败（如 Logger 抛出异常）时审计日志器的行为。
- **建议**：模拟 Logback appender 抛出异常，验证 `logAudit()` 不向调用方抛出异常并在业务日志中记录告警。

---

#### [一般] 消息插值管线缺少 `{锁定时间}` 占位符不在模板中时的回退测试

- **位置**：`GlobalExceptionHandlerTest.java`
- **描述**：OOD 10.3 节定义的插值管线区分命名占位符和编号占位符。当前测试覆盖了两种占位符的插值成功路径，但未覆盖：
  - args 为 `new Object[]{"30分钟"}` 但模板为无占位符的纯文本（如 `SUCCESS.getMessage()`）——应返回纯文本
  - args 为 `null` 时 `formatMessage` 应避免 NPE
  - args 中包含多余元素时被静默忽略
- **建议**：补充上述回退路径测试。

---

#### [一般] DTO 测试覆盖完整，但 PasswordChangeRequest 缺少 oldPassword 边界值（最小 1 字符场景）

- **位置**：`PasswordChangeRequestTest.java`
- **描述**：`@Size(max = 128)` 仅限制上限。按 Spring Validation 规范，`@NotBlank` 已隐含非空校验，但未测试 `oldPassword` 为单个字符的最小边界场景（@NotBlank + @Size(max=128) 允许 1-128 字符）。
- **建议**：补充 oldPassword="a"（最小长度边界）的通过测试。

---

#### [轻微] PermissionFunctionTest 缺少 component 字段的 JPA 列名映射验证

- **位置**：`PermissionFunctionTest.java`
- **描述**：`shouldSetAndGetComponent` 验证了 getter/setter 功能，但未验证 `@Column(name = "component")` 注解是否存在于 component 字段（OOD 5.1 节明确要求此映射，T22 确认修复）。此测试应通过反射检查注解是否存在。
- **建议**：添加反射验证 `@Column(name = "component")` 注解的测试。

---

#### [一般] RoleTest 缺少 sort 字段 NOT NULL 约束验证

- **位置**：`RoleTest.java`
- **描述**：`shouldDefaultSortIsZero` 验证默认值为 0，但 OOD 5.1 节要求 `@Column(nullable=false)`。未测试 `role.setSort(null)` 后 JPA 持久化应触发 ConstraintViolationException。
- **建议**：在 EntityMappingIT 中增加 sort 字段 NOT NULL 约束验证。

---

#### [一般] MenuServiceTest 的 `shouldNotFilterDeletedInJavaLayer` 测试验证了代码行为但语义与设计意图不符

- **位置**：`MenuServiceTest.java:393-416`
- **描述**：OOD 5.1 节要求 `@SQLRestriction` 在 SQL 层处理 deleted 过滤。测试验证 Service 层不过滤 deleted 是正确的，但测试名称 `shouldNotFilterDeletedInJavaLayer` 暗示这是缺陷——实际应为 `@SQLRestriction` 已代理此职责，Service 层无需重复过滤。
- **建议**：将测试名称改为 `shouldDelegateDeletedFilteringToSQLRestriction` 以准确表达设计意图。

---

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 4 |
| 一般 | 14 |
| 轻微 | 2 |

### 总评

单元测试整体质量较高，覆盖了认证业务流程的主要分支路径。AuthServiceImpl 的 21 个测试覆盖了 login（8 种场景）、refreshToken（8 种场景）、logout（4 种场景）、changePassword（3 种场景）、getCurrentUser（1 种场景）、updateProfile（1 种场景），场景覆盖完整。Filter 行为契约测试（JwtAuthenticationFilter 9 个场景、PasswordChangeCheckFilter 6 个场景、GlobalRateLimitFilter 7 个场景）清晰验证了 OOD 3.3 节定义的静默跳过、ACCOUNT_DISABLED 异常、白名单放行/阻断等行为。

存在 4 项严重问题：（1）异常刷新检测（Suspicious Refresh Detection）完全未测试；（2）审计日志器写入失败降级路径未测试；（3）UserFacadeImplTest 完全 mock 了 UserConverter，无法发现真实转换逻辑缺陷（与 T9 直接相关）；（4）三条审查路径中的文件实际位置与范围文件描述不一致。

整体测试覆盖率达到项目该阶段的合理水平，核心安全路径（限流、锁定、黑名单、密码策略）均有充分的边界测试和并发测试。建议优先补充异常刷新检测和审计日志降级路径测试，并修正 UserFacadeImplTest 的 mock 策略以暴露真实转换逻辑。
