# R2-B: 测试代码审查 — Controller 层、Filter 层与 Security 配置测试

审查时间：2026-06-27

### 审查范围

1. `controller/AuthControllerTest.java`
2. `controller/MenuControllerTest.java`
3. `auth/security/JwtAuthenticationFilterTest.java`
4. `auth/security/PasswordChangeCheckFilterTest.java`
5. `auth/security/GlobalRateLimitFilterTest.java`
6. `auth/security/RestAccessDeniedHandlerTest.java`
7. `auth/security/RestAuthenticationEntryPointTest.java`
8. `auth/security/SecurityConfigPhase1Test.java`
9. `auth/security/SecurityConfigPhase1CoexistenceTest.java`
10. `auth/security/CurrentUserImplTest.java`
11. `dto/request/LoginRequestTest.java`
12. `dto/request/MenuCreateRequestTest.java`
13. `dto/request/MenuUpdateRequestTest.java`
14. `dto/request/PasswordChangeRequestTest.java`
15. `dto/request/ProfileUpdateRequestTest.java`
16. `dto/request/RefreshTokenRequestTest.java`
17. `dto/response/LoginResponseTest.java`
18. `dto/response/MenuResponseTest.java`
19. `dto/response/TokenRefreshResponseTest.java`
20. `dto/response/UserInfoResponseTest.java`

### 发现

#### [一般] JwtAuthenticationFilterTest 存在重复测试用例

- **位置**：`auth/security/JwtAuthenticationFilterTest.java:78-93`
- **描述**：`shouldSkipWhenRefreshTokenType()` 测试（行 78-93）与 `shouldSkipWhenInvalidToken()`（行 62-76）在 mock 设置和断言行上完全一致：两者均设置 `jwtTokenProvider.validateToken()` 返回 `null`，然后验证 `chain.doFilter()` 被调用且 `SecurityContextHolder` 为空。测试名称中的 "RefreshTokenType" 名不符实——被测 Filter 源码（`JwtAuthenticationFilter.java:61`）调用 `validateToken(token, "access")`，无论 token 实际类型如何，只要 provider 返回 null 就走同一分支。该测试并未引入 refresh-token 类型的专用 mock 数据（如 `getTokenType()` 返回不同值或 `validateToken` 在 type="refresh" 时有不同行为），因此与前者完全等价。
- **建议**：删除行 78-93 的冗余测试，或修改为真正验证 refresh token 被拒绝的场景（例如构造一个 `validateToken` 对 access type 返回 null 但对 refresh type 返回 claims 的场景）。

#### [一般] SecurityConfigPhase1CoexistenceTest 未验证实际 Filter 链共存行为

- **位置**：`auth/security/SecurityConfigPhase1CoexistenceTest.java:19-41`
- **描述**：按照审查重点，该测试应验证 phase0/phase1 并存时的 Filter 链行为。但当前实现仅检查了两个配置类的 bean 方法能否被独立调用（`authModuleConfig.tokenBlacklist()` + `securityConfigPhase1.jwtAuthenticationFilter()` 创建成功，及各自 bean 方法可正常调用），未验证：
  - 两个配置类的 `SecurityFilterChain` 是否会冲突（如 duplicate filter registration）
  - 当两个配置同时被 Spring 加载时，filter chain 的合并/覆盖行为
  - 运行时 phase1 的 filter 能否正确拦截请求而不受 phase0 filter 影响
- **建议**：补充集成测试，使用 `@SpringBootTest` + `@ActiveProfiles({"phase0", "phase1"})` 加载两个配置，验证 filter chain 中 filter 的类型列表和顺序是否符合预期。

#### [轻微] MenuControllerTest 缺少 /tree 和 /all 端点测试

- **位置**：`controller/MenuControllerTest.java`
- **描述**：审查重点要求覆盖所有 API 端点，包括 tree/all/create/update/delete。但 MenuControllerTest 仅测试了 get/update/create/delete 四个端点，未包含：
  - `GET /api/menu/tree`（所有已认证用户可访问的菜单树接口）
  - `GET /api/menu/all`（仅 ADMIN 角色的全量菜单接口）
  虽然 `/all` 的权限由 `@PreAuthorize("hasRole('ADMIN')")` 在 Spring Security 层面控制、单元测试中无法直接测试，但 `/tree` 是业务核心端点，应在 Controller 单元测试中覆盖。
- **建议**：为 `tree()` 方法补充测试（mock menuService.getUserMenuTree + 验证 Result 结构）。

#### [轻微] AuthControllerTest 缺少 refresh 失败场景的 BusinessException 测试

- **位置**：`controller/AuthControllerTest.java`
- **描述**：`LoginTests` 测试了 login 成功和 login 失败（BusinessException），但 `RefreshTests` 仅测试了 refresh 成功和 BusinessException（UNAUTHORIZED——"令牌无效"）场景。缺失 `TOKEN_REFRESH_FAILED` 错误码的 BusinessException 测试。OOD 6.3 及 GlobalErrorCode 定义了 `TOKEN_REFRESH_FAILED("TOKEN_REFRESH_FAILED", "令牌刷新失败，请重新登录")` 场景。
- **建议**：补充一个测试，mock `authService.refreshToken()` 抛出 `BusinessException(GlobalErrorCode.TOKEN_REFRESH_FAILED)` 并验证。

#### [轻微] MenuUpdateRequestTest 缺少 @Size 约束验证测试

- **位置**：`dto/request/MenuUpdateRequestTest.java`
- **描述**：该测试验证了 getter/setter、null 默认值、JSON 序列化行为（`@JsonInclude(NON_NULL)`），但未像其他 DTO 测试（如 `ProfileUpdateRequestTest`、`PasswordChangeRequestTest`）那样验证 Bean Validation 注解。`MenuUpdateRequest` 源码中定义了 `@Size(max = 50)` 在 `name`、`@Size(max = 100)` 在 `permission` 和 `component`/`icon`、`@Size(max = 255)` 在 `path` 字段——这些约束均未被测试覆盖。
- **建议**：补充 `MenuUpdateRequest` 的 Validator 测试，覆盖各 `@Size` 约束的越界场景。

#### [轻微] GlobalRateLimitFilterTest 未验证 Response Content-Type 含 charset=UTF-8

- **位置**：`auth/security/GlobalRateLimitFilterTest.java:156`
- **描述**：`shouldReturnRateLimitExceededResponseBody` 在行 156 断言 `blockedResp.getContentType()` 等于 `"application/json"`，但实际 `GlobalRateLimitFilter.java:55` 中设置了 `response.setContentType("application/json")`（未设置 charset）。而 OOD 3.3 节 AuthenticationEntryPoint 和 AccessDeniedHandler 的 response 格式均要求 `charset=UTF-8`。虽然此处 Content-Type 缺少 charset 是 Filter 源码行为，但测试应更准确地反映实际值，或作为 Filter 源码修复的提示。
- **建议**：确认 GlobalRateLimitFilter 是否需要补充 `response.setCharacterEncoding("UTF-8")`（与 RestAuthenticationEntryPoint/AccessDeniedHandler 保持一致），测试相应地更新为期望 `"application/json;charset=UTF-8"`。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 0 |
| 一般 | 2 |
| 轻微 | 4 |

### 总评

测试代码整体质量良好，覆盖度较高。AuthControllerTest 覆盖了 login/logout/refresh/me/profile/password 全部 6 个端点，包括成功、失败及边界场景（无 token、空字符串 token、非 Bearer 前缀、principal 类型异常等）。MenuControllerTest 覆盖了 get/create/update/delete 及 id 一致性校验。Filter 测试全面覆盖了 `JwtAuthenticationFilter`（有效/无效/过期/黑名单/禁用/删除/passwordChangeRequired）、`PasswordChangeCheckFilter`（全部 3 个白名单路径 + 阻断场景）、`GlobalRateLimitFilter`（白名单/限流/不同 IP/X-Forwarded-For）。Handler 测试正确验证了 OOD 6.3 节定义的响应体结构（`ACCOUNT_DISABLED`/`UNAUTHORIZED`/`PASSWORD_CHANGE_REQUIRED`/`FORBIDDEN`）。DTO 测试方面，`LoginRequestTest`、`MenuCreateRequestTest`、`PasswordChangeRequestTest`、`ProfileUpdateRequestTest` 对 `@NotBlank`/`@Size`/`@Pattern`/`@Email` 等注解的验证较为全面；`MenuUpdateRequestTest` 正确验证了 `@JsonInclude(NON_NULL)` 的 PATCH 语义。SecurityConfigPhase1Test 正确验证了三个 Filter 的注册顺序（Global < Jwt < PasswordChangeCheck）。

存在的主要改进空间：(1) 两个 `JwtAuthenticationFilter` 测试用例完全重复应去重；(2) `SecurityConfigPhase1CoexistenceTest` 需要从组件级验证提升为集成级 Filter 链行为验证；(3) 部分 DTO 测试（`MenuUpdateRequest`）缺少 Bean Validation 覆盖。
