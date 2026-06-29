# 任务指令（v4）

## 动作
NEW

## 任务描述
修复 T2+T12：将 JwtAuthenticationFilter 从 JwtUtil 切换为 JwtTokenProvider，同时消除重复的 userId 提取逻辑。

**需修改的文件（6 个）：**

### 源文件（3 个）

1. **`common-module-impl/.../auth/jwt/JwtTokenProvider.java`**
   - 新增 `getTokenType()` 方法：`return jwtConfig.getTokenType()`

2. **`common-module-impl/.../auth/security/JwtAuthenticationFilter.java`**
   - `private final JwtUtil jwtUtil` → `private final JwtTokenProvider jwtTokenProvider`
   - 构造注入从 `JwtUtil` 改为 `JwtTokenProvider`
   - `extractToken()` 中 `JwtUtil.extractToken(authHeader, jwtUtil.getTokenType())` → `JwtUtil.extractToken(authHeader, jwtTokenProvider.getTokenType())`
   - `doFilterInternal()` 中 `jwtUtil.validateTokenAndGetClaims(token)` → `jwtTokenProvider.validateToken(token, "access")`
   - **删除**手动 type 检查（当前第 68-74 行：`String tokenType = claims.get("type", ...)` 到 `chain.doFilter`）
   - `extractUserId(claims)` → `jwtTokenProvider.getUserIdFromClaims(claims)`
   - **删除** `extractUserId()` 方法（当前第 142-150 行）
   - 更新 import：`JwtUtil` → `JwtTokenProvider`（保留 `JwtUtil` import 仅用于 `JwtUtil.extractToken` 静态调用）

3. **`common-module-impl/.../auth/security/SecurityConfigPhase1.java`**
   - `jwtAuthenticationFilter(JwtUtil jwtUtil, ...)` → `jwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, ...)`
   - `return new JwtAuthenticationFilter(jwtUtil, ...)` → `return new JwtAuthenticationFilter(jwtTokenProvider, ...)`
   - import 从 `JwtUtil` 改为 `JwtTokenProvider`

### 测试文件（3 个）

4. **`JwtAuthenticationFilterTest.java`**
   - `JwtUtil jwtUtil = mock(JwtUtil.class)` → `JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class)`
   - `new JwtAuthenticationFilter(jwtUtil, ...)` → `new JwtAuthenticationFilter(jwtTokenProvider, ...)`
   - 所有 `when(jwtUtil.getTokenType())` → `when(jwtTokenProvider.getTokenType())`
   - 所有 `when(jwtUtil.validateTokenAndGetClaims(token)).thenReturn(claims)` → `when(jwtTokenProvider.validateToken(token, "access")).thenReturn(claims)`
   - `when(claims.get("type", String.class))` 相关的 mock 可删除（因 type 检查委托给 JwtTokenProvider.validateToken）

5. **`SecurityConfigPhase1Test.java`**
   - `JwtUtil jwtUtil = mock(JwtUtil.class)` → `JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class)`
   - Bean 调用处同步更新

6. **`SecurityConfigPhase1CoexistenceTest.java`**
   - 同上适配

## 选择理由
T2+T12 是批次 3 剩余项，与刚完成的 T1+T10 同属 JWT 认证管线。OOD 1.3 节明确定义 JwtTokenProvider 为 JWT 令牌生成、解析、验证的集中提供者，Filter 依赖旧 JwtUtil 是编码偏差。两缺陷修改重叠于同一文件，必须同步提交。

## 任务上下文
摘自 04_phase1B_report.md：

### T2: JwtAuthenticationFilter 依赖 JwtUtil（旧代码）而非 JwtTokenProvider（P1）
- JwtAuthenticationFilter 注入并使用 JwtUtil 完成 token 验证和解析
- OOD 1.3 明确将 JwtTokenProvider 定义为 JWT 集中提供者
- JwtTokenProvider.validateToken() 支持 type claim 验证（`validateToken(token, expectedType)`），但 Filter 未使用此方法
- Filter 手动重复 type 检查逻辑（`auth/security/JwtAuthenticationFilter.java:68-74`）

### T12: userId 提取逻辑重复（P2）
- JwtAuthenticationFilter.extractUserId() 与 JwtTokenProvider.getUserIdFromClaims() 方法体完全一致
- OOD 8.1 节 C3 明确提出抽取到 JwtTokenProvider 消除重复
- 需在 T2 注入切换完成后同步删除 Filter 中的 extractUserId 方法

### 执行注意事项
- 删除手动 type 检查后，`claims.get("jti", String.class)` 和 `claims.get("userId")` 在后续代码中仍被使用，需确保这些 mock 在测试中保留
- extractToken 中 `JwtUtil.extractToken` 是静态调用，可保留（JwtUtil 不删除），但 `getTokenType()` 需改为从 JwtTokenProvider 获取
- 验证方式：运行 `mvn test -pl :common-module-impl` 确认全部测试通过

## 已有代码上下文
- JwtTokenProvider（已完成 T1 修复）提供：`validateToken(token, expectedType)`、`getUserIdFromClaims(Claims)`、`getJtiFromToken(token)`
- JwtTokenProvider 尚无 `getTokenType()` 方法，需新增（委托 `jwtConfig.getTokenType()`）
- JwtAuthenticationFilter 当前构造函数：`JwtAuthenticationFilter(JwtUtil jwtUtil, TokenBlacklist tokenBlacklist, UserRepository userRepository)`
- JwtAuthenticationFilter 中 `extractToken()` 使用 `JwtUtil.extractToken(authHeader, jwtUtil.getTokenType())` 静态方法
- SecurityConfigPhase1 当前 Bean 方法：`jwtAuthenticationFilter(JwtUtil jwtUtil, TokenBlacklist tokenBlacklist, UserRepository userRepository)`
