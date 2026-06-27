# R4: Phase 1 包 B 测试覆盖审查

审查时间：2026-06-26

### 审查范围

本次审查覆盖模块六（测试覆盖）的全部 40 个测试文件：

**Auth 测试：**
- `auth/UserFacadeImplTest.java`
- `auth/blacklist/InMemoryTokenBlacklistTest.java`
- `auth/config/AuthModuleConfigTest.java`
- `auth/converter/UserConverterTest.java`
- `auth/exception/PasswordChangeRequiredExceptionTest.java`
- `auth/jwt/JwtTokenProviderTest.java`
- `auth/login/LoginAttemptTrackerTest.java`
- `auth/password/PasswordChangeServiceImplTest.java`
- `auth/password/PasswordPolicyImplTest.java`
- `auth/rateLimit/InMemoryRateLimitGuardTest.java`
- `auth/rateLimit/SlidingWindowCounterTest.java`
- `auth/security/CurrentUserImplTest.java`
- `auth/security/GlobalRateLimitFilterTest.java`
- `auth/security/JwtAuthenticationFilterTest.java`
- `auth/security/PasswordChangeCheckFilterTest.java`
- `auth/security/RestAccessDeniedHandlerTest.java`
- `auth/security/RestAuthenticationEntryPointTest.java`
- `auth/security/SecurityConfigPhase1Test.java`

**Controller/Service 测试：**
- `controller/AuthControllerTest.java`
- `controller/MenuControllerTest.java`
- `service/AuthServiceTest.java`
- `service/MenuServiceTest.java`

**DTO 测试：**
- `dto/request/LoginRequestTest.java`
- `dto/request/MenuCreateRequestTest.java`
- `dto/request/MenuUpdateRequestTest.java`
- `dto/request/PasswordChangeRequestTest.java`
- `dto/request/ProfileUpdateRequestTest.java`
- `dto/request/RefreshTokenRequestTest.java`
- `dto/response/LoginResponseTest.java`
- `dto/response/MenuResponseTest.java`
- `dto/response/TokenRefreshResponseTest.java`
- `dto/response/UserInfoResponseTest.java`

**JWT 测试：**
- `jwt/JwtConfigTest.java`
- `jwt/JwtUtilTest.java`

**Entity 测试：**
- `permission/UserTest.java`
- `permission/RoleTest.java`
- `permission/PostTest.java`
- `permission/PermissionFunctionTest.java`
- `permission/UserRepositoryTest.java`

**其他：**
- `common/exception/GlobalErrorCodeTest.java`（位于 common 模块）

**设计依据：** `Docs/05_ood_phase1_B.md`

### 发现

#### [一般] JwtAuthenticationFilter 依赖旧 JwtUtil 而非新 JwtTokenProvider

- **位置**：`auth/security/JwtAuthenticationFilterTest.java:39`
- **描述**：测试中模拟 `JwtUtil` 作为依赖注入，但设计文档 1.3 节明确定义 `JwtTokenProvider` 为集中式 JWT 提供者（含 `@PostConstruct` 启动验证，`validateToken` 类型检查等功能），3.3 节也要求 Filter 层通过 JwtTokenProvider 进行令牌验证。当前测试和实现依赖于旧的 `JwtUtil`，绕过了新的抽象层，使 H2（SecretKey 缓存）、C3（userId 提取优化）、Section 4.7（启动验证）等修复失效。
- **建议**：将 `JwtAuthenticationFilter` 的依赖从 `JwtUtil` 替换为 `JwtTokenProvider`，更新测试中的 mock 和 verify 调用。

#### [一般] AuthController.changePassword 直接解析 JWT 而非使用 CurrentUser

- **位置**：`controller/AuthControllerTest.java:250-261`
- **描述**：`changePassword` 方法在 Controller 层通过 `JwtTokenProvider.validateToken()` 和 `getUserIdFromClaims()` 直接从 token 中提取 userId，而非使用设计文档 1.3 节定义的 `CurrentUser` 接口（或 SecurityContext）。设计文档 M4 修复方案明确要求"引入 CurrentUser 接口，Controller 通过 SecurityContext 获取"。
- **建议**：`changePassword` 应通过 `@AuthenticationPrincipal` 或 `CurrentUser.getUserId()` 获取当前用户 ID，移除 Controller 层手动解析 JWT 的逻辑。

#### [一般] AuthServiceTest 缺少 deleted 用户登录场景

- **位置**：`service/AuthServiceTest.java:141-155`
- **描述**：测试覆盖了 `enabled=false` 的禁用用户场景，但设计文档 3.1.1 节步骤 6 明确要求 `deleted == true` 也应采用同等的安全策略（dummy BCrypt 比对 + 双维度失败计数 + LOGIN_FAILED 错误码）。`deleted` 字段在 BaseEntity 中定义，该场景未单独测试。
- **建议**：补充 `login_shouldThrowUserDeleted` 测试用例，设置 `user.setDeleted(true)`，验证 dummy BCrypt 和双维度计数调用。

#### [一般] LoginAttemptTrackerTest 缺少锁定消息内容验证

- **位置**：`auth/login/LoginAttemptTrackerTest.java:41-48, 82-89`
- **描述**：测试验证了 `isUsernameLocked()` 和 `isIpLocked()` 返回 `true/false`，但未验证锁定后返回给用户的错误消息内容。设计文档 10.2 节定义：IP 维度锁定返回"账户已锁定，请 30 分钟后重试"，用户名维度锁定返回"账户已锁定，请 15 分钟后重试"。当前测试依赖调用方（AuthServiceImpl）构造消息，但调用侧的测试也未覆盖消息格式。
- **建议**：在 `AuthServiceTest` 的 `login_shouldThrowIpLocked` 和 `login_shouldThrowUsernameLocked` 中补充验证 `BusinessException.getMessage()` 的格式内容。

#### [一般] RestAuthenticationEntryPointTest 依赖硬编码 message 字符串判断

- **位置**：`auth/security/RestAuthenticationEntryPointTest.java:20-32`
- **描述**：测试通过 `authException.getMessage().equals("账户已被管理员停用")` 判断是否返回 ACCOUNT_DISABLED。此测试绑定到 `GlobalErrorCode.ACCOUNT_DISABLED.getMessage()` 的硬编码值，如果枚举消息变更（如国际化）则测试失效。设计文档 3.3 节要求 AuthenticationEntryPoint 通过异常类型或 ErrorCode 枚举值判断，而非 message 字符串。
- **建议**：考虑引入自定义异常类型（如 `AccountDisabledException extends AuthenticationException`），替代通过 message 字符串的 duck-typing 判断方式。

#### [一般] MenuServiceTest 未覆盖多级菜单树构建

- **位置**：`service/MenuServiceTest.java:75-114`
- **描述**：`getUserMenuTree` 测试仅包含单层菜单返回验证。设计文档 5.2 节定义 `MenuResponse` 支持递归 `children` 结构，Section 6.1 定义 `/api/menu/tree` 返回树形菜单。当前测试未覆盖 parent-child 关系的树构建逻辑、同级排序（sort 字段）、多级嵌套场景。
- **建议**：补充具有父-子关系的多级菜单树构建测试，验证 `children` 递归结构和 `sort` 排序。

#### [一般] SecurityConfigPhase1Test 未测试 Filter 执行顺序

- **位置**：`auth/security/SecurityConfigPhase1Test.java`
- **描述**：当前测试仅验证了各 Bean 是否被创建（非 null），但未验证 Filter 链的注册顺序。设计文档 3.3 节明确规定了 Filter 执行顺序：`GlobalRateLimitFilter` 最先 → `JwtAuthenticationFilter` → `PasswordChangeCheckFilter` 最后。顺序错误会导致安全漏洞（如限流在认证之后执行）。
- **建议**：补充验证 `SecurityFilterChain` 中 Filter 的顺序注册，使用 `assertThat(filterChain.getFilters()).extracting(Filter::getClass).containsExactly(...)` 模式。

#### [轻微] PasswordChangeCheckFilterTest 未覆盖白名单路径的错误 HTTP 方法

- **位置**：`auth/security/PasswordChangeCheckFilterTest.java:69-120`
- **描述**：白名单路径测试使用了正确的 HTTP 方法（PUT for password, POST for logout/refresh），但未验证错误 HTTP 方法是否被正确拒绝（如 `GET /api/auth/password` 不应在白名单中）。
- **建议**：补充对白名单路径使用不匹配 HTTP 方法的拒绝测试。

#### [轻微] UserTest 未验证 enabled 默认值

- **位置**：`permission/UserTest.java`
- **描述**：`User.java` 中 `private Boolean enabled = true`，但 UserTest 未像 PostTest（`shouldDefaultEnabledIsTrue`）那样验证 enabled 的默认值是否为 `true`。
- **建议**：补充 `shouldDefaultEnabledIsTrue` 测试验证 `getEnabled()` 默认返回 `true`。

#### [轻微] AuthControllerTest 跳过 Authorization header 提取路径

- **位置**：`controller/AuthControllerTest.java:65-73, 140-147, 168-173`
- **描述**：Controller 测试直接以方法参数传递 `"Bearer mock-token"` 字符串，跳过了从 `HttpServletRequest` 的 `Authorization` header 中提取 token 的逻辑。提取逻辑由 Controller 内部方法（如 `extractToken`）实现，未在测试中覆盖。
- **建议**：考虑使用 `MockHttpServletRequest` 添加 `Authorization` header，或在 Controller 测试中增加对 `extractToken` 方法的单元测试。

#### [轻微] MenuUpdateRequestTest 未覆盖 equals/hashCode/toString

- **位置**：`dto/request/MenuUpdateRequestTest.java`
- **描述**：`MenuUpdateRequest` 是传统 POJO（非 record），设计文档 5.2 节说明其需支持 PATCH 局部更新语义。当前测试覆盖了 getter/setter 和 JSON 序列化，但未测试 equals/hashCode/toString。该 POJO 可能用于集合操作或在日志中输出。
- **建议**：补充 equals/hashCode 和 toString 的验证，或在确定不需要时显式标注无需覆盖。

#### [轻微] JwtTokenProviderTest 缺少 tokenVersion 边界值测试

- **位置**：`auth/jwt/JwtTokenProviderTest.java:50-57`
- **描述**：`generateRefreshToken` 测试验证了 `tokenVersion=0` 的合法情况，但未测试 `getTokenVersionFromClaims` 对 null、负数等边界值的处理。
- **建议**：补充 `getTokenVersionFromClaims` 对 null claims 和负数 version 的边界测试。

#### [轻微] GlobalRateLimitFilterTest 未测试白名单路径大小写敏感性

- **位置**：`auth/security/GlobalRateLimitFilterTest.java:56-114`
- **描述**：白名单路径测试均使用精确大小写匹配。设计文档中路径统一为小写，但未测试路由匹配是否大小写敏感（如 `/API/AUTH/LOGIN` 是否被正确识别为白名单）。
- **建议**：补充大小写变化的路径测试用例。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 0 |
| 一般 | 7 |
| 轻微 | 6 |

### 总评

测试覆盖整体质量良好。40 个测试文件覆盖了所有实现类，AuthServiceTest 对登录流程的安全场景覆盖充分（限流、IP/用户名锁定、禁用用户、假名用户、密码不匹配、dummy BCrypt 验证）。DTO 校验测试覆盖完善，JwtUtilTest 对密钥验证覆盖充分。并发安全测试（InMemoryTokenBlacklistTest、LoginAttemptTrackerTest、SlidingWindowCounterTest）质量较高。

主要问题集中在架构一致性上：`JwtAuthenticationFilter` 仍依赖旧的 `JwtUtil` 而非新 `JwtTokenProvider`，`AuthController.changePassword` 在 Controller 层直接解析 JWT 而非通过 `CurrentUser` 接口——这两项使设计文档中 M4、H2、C3、4.7 节的修复在测试层面未被验证生效。安全场景方面，`deleted=true` 用户登录场景缺测；Filter 执行顺序未在配置测试中验证。建议优先修复架构一致性问题（JwtTokenProvider 替代 JwtUtil、CurrentUser 替代手动 JWT 解析），其次补充缺失的边界测试。
