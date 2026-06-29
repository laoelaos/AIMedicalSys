# 任务指令（v9）

## 动作
NEW

## 任务描述
修复 T6+T7：AuthController 路径修复 + SecurityContext 重构（1 个源文件）

**文件：**
`AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/controller/AuthController.java`

**T6（路径修复）：** 将 `@PutMapping("/me")`（L70）改为 `@PutMapping("/profile")`，使端点映射为 `PUT /api/auth/profile`，与 OOD 4.4/6.1 一致。

**T7（SecurityContext 重构）：** 将 `changePassword()` 方法（L82-91）从直接调用 `jwtTokenProvider.validateToken()` + `jwtTokenProvider.getUserIdFromClaims()` 重构为从 `SecurityContextHolder` 获取用户 ID。具体：
1. 新增私有方法 `getCurrentUserId()`（参考 `MenuController.getCurrentUserId()` L148-158 模式），从 `Authentication.getPrincipal()` 获取 userId（principal 为 Long/Integer 类型）
2. 修改 `changePassword()`，删除手动 token 解析，改为调用 `getCurrentUserId()`
3. 删除 `JwtTokenProvider jwtTokenProvider` 字段及构造注入参数（T7 修复后 AuthController 不再直接依赖 JwtTokenProvider）
4. 删除不再使用的 import：`io.jsonwebtoken.Claims`、`JwtTokenProvider`

## 选择理由
Batch 5（Controller 层修复）首项，T6+T7 均修改 `AuthController.java`，需同步提交以避免同一文件的修改冲突。P1 优先级。T6 修正 API 路径与设计契约偏差。T7 消除 Controller 层架构性违规——JwtAuthenticationFilter 已验证 Access Token 并将 userId 设为 SecurityContext principal，Controller 不应重复解析令牌。批次 4（T3+T13+T5+T25）已全部完成，无阻塞依赖。

## 任务上下文
**T6：** OOD 4.4 保护清单和 6.1 接口清单均定义资料更新端点为 `PUT /api/auth/profile`。当前代码使用 `@PutMapping("/me")`（`AuthController.java:70`），映射为 `PUT /api/auth/me`，与设计契约不一致。此为 API 路径变更，需同时确认前端请求 URL 的同步调整。可将旧路径保留为临时兼容（备选方案：可添加 `@RequestMapping(path = "/profile", method = RequestMethod.PUT)` 在旧路径保留期间提供重定向）。

**T7：** OOD 3.1.6 步骤 2 明确要求：JwtAuthenticationFilter 已验证 Access Token，Controller 应从 SecurityContext 获取当前用户 ID。当前 `changePassword()`（`AuthController.java:82-91`）在 Controller 层直接调用 `jwtTokenProvider.validateToken(token, null)` 和 `jwtTokenProvider.getUserIdFromClaims(claims)`，跳过 SecurityContext，违反架构分层。参照 `MenuController.getCurrentUserId()`（L148-158）的模式修复——该方法从 `SecurityContextHolder.getContext().getAuthentication()` 获取 principal，支持 Long 和 Integer 类型，抛出 `IllegalStateException`。

## 已有代码上下文
**当前 AuthController.java：**
```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;  // 将被删除

    public AuthController(AuthService authService, JwtTokenProvider jwtTokenProvider) {
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;     // 将被删除
    }

    // ... login, logout, refresh 方法不变 ...

    @GetMapping("/me")               // GET 端点保持不动
    ...

    @PutMapping("/me")               // L70 — 改为 @PutMapping("/profile")
    public Result<UserInfoResponse> updateMe(...) {
        // 方法体不变，仅改注解路径
    }

    @PutMapping("/password")         // L82 — 改为使用 getCurrentUserId()
    public Result<Void> changePassword(...) {
        String token = extractToken(authHeader);
        Claims claims = jwtTokenProvider.validateToken(token, null);  // 删除
        Long userId = jwtTokenProvider.getUserIdFromClaims(claims);   // 删除
        // 替换为: Long userId = getCurrentUserId();
        authService.changePassword(userId, ...);
    }

    private String extractToken(...) { ... }   // 保留
}
```

**参考——MenuController.getCurrentUserId()：**
```java
private Long getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Object principal = authentication.getPrincipal();
    if (principal instanceof Long) {
        return (Long) principal;
    }
    if (principal instanceof Integer) {
        return ((Integer) principal).longValue();
    }
    throw new IllegalStateException("无法从SecurityContext获取用户ID");
}
```

**需删除的 import：** `io.jsonwebtoken.Claims`（L14）、`com.aimedical.modules.commonmodule.auth.jwt.JwtTokenProvider`（L4）
**需新增的 import：** `org.springframework.security.core.Authentication`、`org.springframework.security.core.context.SecurityContextHolder`

## 受影响的测试文件（新增修改范围）

### AuthControllerTest.java
`common-module-impl/src/test/java/com/aimedical/modules/commonmodule/controller/AuthControllerTest.java`

**T6 影响（无需修改测试逻辑）：** `@PutMapping("/me")` → `@PutMapping("/profile")` 仅改变注解路径，Java 方法名 `updateMe()` 不变，测试类直接调用方法而非 HTTP 请求，因此 T6 不会破坏任何现有测试。`UpdateMeTests` 的 `@DisplayName` 可同步更新。

**T7 影响（需要修改测试）：**
1. 删除 `@Mock JwtTokenProvider jwtTokenProvider` 字段（L37-38）——T7 修复后 AuthController 不再注入 JwtTokenProvider
2. 修改 `setUp()`（L47）：`new AuthController(authService, jwtTokenProvider)` → `new AuthController(authService)`
3. 删除 import：`io.jsonwebtoken.Claims`（L15）、`com.aimedical.modules.commonmodule.auth.jwt.JwtTokenProvider`（L6）
4. 修改 `ChangePasswordTests`（L244-262）：
   - 删除 `Claims claims = mock(Claims.class)` 和 `when(jwtTokenProvider.validateToken(...))` / `when(jwtTokenProvider.getUserIdFromClaims(...))`
   - 改为 mock `SecurityContextHolder`：设置 `Authentication` 的 `principal` 为 `Long` 类型 `1L`
   - 调用 `authController.changePassword(request)`（删除 `authHeader` 参数——T7 重构后不再接收）
   - 验证 `authService.changePassword(1L, "oldPass", "newPass123")` 被调用

**需要新增的 import：** `org.springframework.security.core.Authentication`、`org.springframework.security.core.context.SecurityContextHolder`

**测试文件中不涉及修改的其他测试：**
- `LoginTests` — 无变化
- `LogoutTests` — 无变化
- `RefreshTests` — 无变化
- `MeTests` — 无变化
- `UpdateMeTests` — 方法调用不变，仅更新 `@DisplayName` 可选

## 安全覆盖确认：JwtAuthenticationFilter 对 `/api/auth/password` 的覆盖

**结论：JwtAuthenticationFilter 已覆盖 `/api/auth/password`（PUT）路径，T7 重构基于此安全保证。**

验证依据：
1. **Filter 注册范围：** `JwtAuthenticationFilter` 继承 `OncePerRequestFilter`，未覆写 `shouldNotFilter()`，对所有进入 DispatcherServlet 的请求均执行过滤逻辑。只要请求携带有效的 `Authorization: Bearer <token>` 头，Filter 即进行 token 验证并设置 `SecurityContext`。
2. **SecurityConfig 路径保护：** `SecurityConfigPhase1.java:75` 配置 `.requestMatchers("/api/auth/**").authenticated()`，`/api/auth/password` 被该通配符覆盖，未认证请求会被拒绝。
3. **Token 类型正确：** `JwtAuthenticationFilter.java:61` 使用 `jwtTokenProvider.validateToken(token, "access")` 验证 Access Token，`changePassword()` 业务流程要求 Access Token 认证，符合预期。
4. **Principal 类型适配：** Filter 在 `doFilterInternal()` L103-L105 将 `userId`（Long）设为 `UsernamePasswordAuthenticationToken` 的 principal，T7 中 `getCurrentUserId()` 从 `SecurityContextHolder` 获取 principal 时匹配 `principal instanceof Long` 分支，类型兼容。

### 不需要修改的文件
- `AuthService.java` — 接口签名不变
- `AuthServiceImpl.java` — 实现不变
- `JwtAuthenticationFilter.java` — 已覆盖所有认证路径，无需修改
- `SecurityConfigPhase1.java` — 路径保护已到位

## 修订说明（v9 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| AuthControllerTest.java 修改范围不完整 | 补充完整的 AuthControllerTest.java 修改范围：删除 `@Mock JwtTokenProvider` 字段、修改 `setUp()` 构造参数、删除不再使用的 import、将 `ChangePasswordTests` 从 mock JwtTokenProvider 改为 mock SecurityContextHolder |
| 未确认 JwtAuthenticationFilter 对 `/api/auth/password` 路径的覆盖 | 新增「安全覆盖确认」章节，从 Filter 注册范围（OncePerRequestFilter 无排除）、SecurityConfig 路径保护（`/api/auth/**`.authenticated()）、Token 类型（access token）、Principal 类型（Long）四个维度确认覆盖无误 |
