# 任务指令（v9）

## 动作
NEW

## 任务描述

将 `application/src/main/java/com/aimedical/config/JwtAuthenticationFilter.java` 迁移重构至 `common-module-impl/auth/security/`，并按 OOD 3.3 行为契约大幅增强。

### 文件变更

**新建（1 个生产类 + 1 个测试类）：**

1. `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/JwtAuthenticationFilter.java`
   - 包：`com.aimedical.modules.commonmodule.auth.security`
   - 继承 `OncePerRequestFilter`
   - **禁止标注 `@Component`**，由 SecurityConfigPhase1 显式注册

2. `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/JwtAuthenticationFilterTest.java`
   - 包：`com.aimedical.modules.commonmodule.auth.security`
   - JUnit 5 + MockHttpServletRequest/Response，无 Spring 上下文

**删除：**

3. `application/src/main/java/com/aimedical/config/JwtAuthenticationFilter.java`（旧位置，需移除）

### 行为契约（OOD 3.3）

```
JwtAuthenticationFilter.doFilterInternal(request, response, chain):

1. 从 Authorization header 提取 token（无 header → chain.doFilter 放行）
2. JwtUtil.validateTokenAndGetClaims(token):
   a. 返回 null → 清除 SecurityContext → chain.doFilter 放行
   b. 验证 token type claim：若 claims 中存在 type=refresh → 清除 SecurityContext → chain.doFilter 放行（拒绝用 Refresh Token 作 Access 用途）
3. TokenBlacklist.isBlacklisted(token jti):
   a. 从 claims 中提取 jti
   b. 在黑名单中 → 清除 SecurityContext → chain.doFilter 放行
4. 从 DB 加载用户: userRepository.findById(userId):
   a. 用户不存在 / deleted=true → 清除 SecurityContext → chain.doFilter 放行
   b. enabled=false → 抛出 AuthenticationException（携带 ErrorCode.ACCOUNT_DISABLED.getMessage()），由 AuthenticationEntryPoint 返回 401
5. 将用户的 `passwordChangeRequired` 状态存入请求 attribute（key="passwordChangeRequired"），供 PasswordChangeCheckFilter 读取
6. 收集用户权限（从 roles + posts → functions 派生 authority 列表）
7. 装配 UsernamePasswordAuthenticationToken(principal=userId, credentials=null, authorities=权限列表)
8. SecurityContextHolder.getContext().setAuthentication(authentication)
9. chain.doFilter(request, response)
```

### 新 Filter 与旧 Filter 的关键差异

| 维度 | 旧 Filter（application 包） | 新 Filter（common-module-impl 包） |
|------|---------------------------|-----------------------------------|
| TokenBlacklist 检查 | 无 | 步骤 3：提取 jti，检查黑名单 |
| DB 用户状态验证 | 无 | 步骤 4：查 enabled/deleted |
| type claim 校验 | 无 | 步骤 2b：拒绝 type=refresh |
| passwordChangeRequired | 无 | 步骤 5：存入 request attribute |
| 权限装配 | 仅 `ROLE_` 单 authority | 步骤 6：从 roles+posts→functions 派生完整权限列表 |
| `@Component` | 有 | **无**（由 SecurityConfigPhase1 注册） |
| 包路径 | `com.aimedical.config` | `com.aimedical.modules.commonmodule.auth.security` |

### 所需已有依赖

- `JwtUtil` — `validateTokenAndGetClaims()`, `extractToken()`, `getTokenType()`, `getUserId()`, `getUsername()`（已有 ✅）
- `TokenBlacklist` — `isBlacklisted(String token)`（R6 ✅）
- `UserRepository` — `findById(Long)` 需返回带 `@EntityGraph(attributePaths = {"roles", "posts"})` 的完整用户，或拆分为两个方法（R1 ✅）
- `GlobalErrorCode.ACCOUNT_DISABLED` — 用于抛出 AuthenticationException（R2 ✅）
- `PasswordChangeRequiredException` — 仅类型引用，本 Filter 不定制抛出（R8 ✅）

### 类型要求

**JwtAuthenticationFilter：**
- 形态：`public class JwtAuthenticationFilter extends OncePerRequestFilter`
- 构造器：包级私有，`JwtAuthenticationFilter(JwtUtil jwtUtil, TokenBlacklist tokenBlacklist, UserRepository userRepository)`
  - 三个依赖通过构造器注入，**无 `@Autowired` / `@Component`**
- 重写 `doFilterInternal(HttpServletRequest, HttpServletResponse, FilterChain)`
- 私有辅助方法：
  - `extractToken(String authHeader)` — 委托 `JwtUtil.extractToken`
  - `extractJti(Claims claims)` — 从 claims 提取 jti
  - `collectAuthorities(User user)` — 遍历 user.roles → roles.posts → post.functions → `SimpleGrantedAuthority("FUNC_" + function.code)`
  - `throwAccountDisabled(String message)` — 抛出匿名 `AuthenticationException` 子类

### 测试覆盖（JwtAuthenticationFilterTest）

| # | 测试方法 | 场景 | 验证点 |
|---|---------|------|--------|
| 1 | `shouldSkipWhenNoAuthHeader` | 无 Authorization header | `chain.doFilter` 被调用，SecurityContext 为 null |
| 2 | `shouldSkipWhenInvalidToken` | token 解析失败 (validateTokenAndGetClaims=null) | chain.doFilter，SecurityContext 未设置 |
| 3 | `shouldSkipWhenRefreshTokenType` | token type=refresh | chain.doFilter，SecurityContext 未设置 |
| 4 | `shouldSkipWhenTokenBlacklisted` | jti 在黑名单中 | chain.doFilter，SecurityContext 未设置 |
| 5 | `shouldSkipWhenUserNotFound` | DB 中用户不存在 | chain.doFilter，SecurityContext 未设置 |
| 6 | `shouldThrowAccountDisabledWhenUserDisabled` | 用户 enabled=false | 抛出 AuthenticationException |
| 7 | `shouldAuthenticateSuccessfully` | 全部通过 | SecurityContext 设置了正确的 Authentication |
| 8 | `shouldSetPasswordChangeRequiredAttribute` | passwordChangeRequired=true | request attribute 正确设置 |
| 9 | `shouldPopulateAuthoritiesFromRolesAndFunctions` | 完整权限场景 | authorities 包含 FUNC_ 权限码 |

### 特殊注意事项

- **jti 提取**：旧 JwtUtil.generateToken() 不生成 jti，但本 Filter 需从 claims 中提取 jti 作为 TokenBlacklist 查询键。若 claims 中无 jti，isBlacklisted 返回 false（不阻断）。**行为约定**：`InMemoryTokenBlacklist.add()` 在登出时使用 Access Token 的 jti 作为 key，若 token 无 jti 则不加入黑名单（不触发 NPE）。
- **EntityGraph**：UserRepository 需要一个 `@EntityGraph(attributePaths = {"roles", "posts"})` 标注的 `findById()` 重载方法或独立方法。若当前 `findById()` 无 EntityGraph，需新增 `findWithDetailsById(Long id)`。
- **提取 authorities 算法**：`user.getRoles() → 每个 Role.getCode() → SimpleGrantedAuthority("ROLE_" + code)` + `user.getPosts() → 每个 Post.getFunctions() → SimpleGrantedAuthority("FUNC_" + function.getCode())`。Set 去重。
- **AuthenticationException 抛出方式**：`throw new AuthenticationException(message) {}`（匿名子类，因 AuthenticationException 是 abstract）。
- **清空 SecurityContext 方式**：`SecurityContextHolder.clearContext()`。

## 选择理由

- JwtAuthenticationFilter 是整个 Filter 链的核心节点：所有后续 Filter（PasswordChangeCheckFilter）和 SecurityConfigPhase1 的注册都依赖它
- 当前旧 Filter 位于 application 包且缺失所有安全增强（TokenBlacklist、DB 验证、权限装配），必须迁移并增强
- 所有依赖（TokenBlacklist、UserRepository、JwtUtil、ErrorCode、PasswordChangeRequiredException）均已就位
- 迁移与增强合并在单一任务中，避免先迁移再增强的两次冗余改动
- 完成此任务后 PasswordChangeCheckFilter（2.3）和 SecurityConfigPhase1（2.10）即可依次推进

## 任务上下文

### OOD 3.3 JwtAuthenticationFilter 行为契约原文

```
JwtAuthenticationFilter.doFilterInternal(request, response, chain):
  1. 从 Authorization header 提取 token（无 header → chain.doFilter 放行）
  2. JwtTokenProvider.validateToken(token):
     a. 解析 JWT，验证签名和有效期
     b. 验证 token type claim（拒绝 type=refresh 的 token 作 access 用途）
     c. 失败 → 清除 SecurityContext → chain.doFilter 放行（由 ExceptionTranslationFilter 处理 401）
  3. TokenBlacklist.isBlacklisted(token):
     a. 检查 jti 是否在黑名单中
     b. 在黑名单中 → 清除 SecurityContext → chain.doFilter 放行
  4. 从 DB 加载用户: userRepository.findById(userId):
     a. 用户不存在 / deleted=true → 清除 SecurityContext → chain.doFilter 放行
     b. enabled=false → 抛出 AuthenticationException（携带 ErrorCode.ACCOUNT_DISABLED），由 AuthenticationEntryPoint 返回 401
  5. 将用户的 `passwordChangeRequired` 状态存入请求 attribute（key="passwordChangeRequired"），供 PasswordChangeCheckFilter 读取
  6. 收集用户权限（从 roles + posts → functions 派生 authority 列表）
  7. 装配 UsernamePasswordAuthenticationToken(principal=userId, credentials=null, authorities=权限列表)
  8. SecurityContextHolder.getContext().setAuthentication(authentication)
  9. chain.doFilter(request, response)
```

**静默跳过策略**：Filter 在 token 无效/用户不存在/已删除时不直接返回 401，而是跳过（不设 SecurityContext），由 Spring Security 的 `ExceptionTranslationFilter` 或目标 Controller 的 `@PreAuthorize` 触发认证异常。此策略确保非受保护 API（如 `/api/auth/refresh`）即使在 Filter 层 token 无效也能继续处理。

**禁用用户特殊处理**：当用户被禁用（enabled=false）时，Filter 抛出 AuthenticationException（ACCOUNT_DISABLED），AuthenticationEntryPoint 识别此错误码并返回 401 + ErrorCode.ACCOUNT_DISABLED。此场景不适用静默跳过，因已认证请求中用户已被禁用应得到明确提示。

### 已有代码关键文件

| 文件 | 位置 |
|------|------|
| 旧 JwtAuthenticationFilter | `application/src/main/java/com/aimedical/config/JwtAuthenticationFilter.java` |
| JwtUtil | `common-module-impl/.../jwt/JwtUtil.java` |
| JwtConfig | `common-module-impl/.../jwt/JwtConfig.java` |
| TokenBlacklist | `common-module-impl/.../auth/blacklist/TokenBlacklist.java` |
| InMemoryTokenBlacklist | `common-module-impl/.../auth/blacklist/InMemoryTokenBlacklist.java` |
| UserRepository | `common-module-impl/.../permission/UserRepository.java` |
| User | `common-module-impl/.../permission/User.java` |
| Role | `common-module-impl/.../permission/Role.java` |
| Post | `common-module-impl/.../permission/Post.java` |
| PermissionFunction | `common-module-impl/.../permission/PermissionFunction.java` |
| RestAuthenticationEntryPoint | `common-module-impl/.../auth/security/RestAuthenticationEntryPoint.java` |
| RestAccessDeniedHandler | `common-module-impl/.../auth/security/RestAccessDeniedHandler.java` |
| GlobalErrorCode | `common/.../exception/GlobalErrorCode.java` |
| Result | `common/.../result/Result.java` |

### 依赖详情

**构造器注入**：
- `JwtUtil jwtUtil` — JWT 解析工具
- `TokenBlacklist tokenBlacklist` — Token 黑名单检查（接口，实为 InMemoryTokenBlacklist）
- `UserRepository userRepository` — 用户数据访问（需 `@EntityGraph` 方法）

**所需 UserRepository 新增方法**（若不存在）：
```java
@EntityGraph(attributePaths = {"roles", "posts"})
Optional<User> findWithDetailsById(Long id);
```

**authorities 派生逻辑**：
```java
Set<SimpleGrantedAuthority> authorities = new HashSet<>();
// 角色 authority
for (Role role : user.getRoles()) {
    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode()));
}
// 功能权限 authority
for (Post post : user.getPosts()) {
    for (PermissionFunction func : post.getFunctions()) {
        authorities.add(new SimpleGrantedAuthority("FUNC_" + func.getCode()));
    }
}
```

**passwordChangeRequired 存入 request attribute**：
```java
request.setAttribute("passwordChangeRequired", user.getPasswordChangeRequired());
```

### 测试上下文

- 使用 `MockHttpServletRequest` / `MockHttpServletResponse`（spring-boot-starter-test 已有）
- `JwtUtil` 通过 mock（`mock(JwtUtil.class)`）注入
- `TokenBlacklist` 通过 mock 注入
- `UserRepository` 通过 mock 注入
- 使用 `Mockito.when().thenReturn()` 控制各场景
- `SecurityContextHolder` 在 `@AfterEach` 中 `clearContext()` 清理
- 验证 Authentication 用 `SecurityContextHolder.getContext().getAuthentication()`
- 验证 authorities 用 `((UsernamePasswordAuthenticationToken) auth).getAuthorities()`
- 验证 request attribute 用 `request.getAttribute("passwordChangeRequired")`

### jti 为 null 的兼容约定

当前 JwtUtil.generateToken() 未生成 jti claim。因此新 Filter 在步骤 3 中，当 claims.get("jti") 为 null 时直接跳过黑名单检查（isBlacklisted = false 处理），不阻断流程。此兼容层确保与旧 token 向后兼容。后续 JwtTokenProvider（阶段 3 新增）将在生成 token 时加入 jti。

### 旧文件删除

旧 `JwtAuthenticationFilter.java` 在 `application/` 包中标注了 `@Component`，删除后需确认 application 模块不再引用此 bean。此工作由后续 SecurityConfigPhase1 任务（2.10）统一处理——新 SecurityConfigPhase1 将使用新 Filter 并移除对旧 Filter 的依赖注入。
