# 任务指令（v16）

## 动作
NEW

## 任务描述
重构 `AuthServiceImpl`（位于 `common-module-impl/service/impl/`）全量重写为核心认证服务，集成 R4-R15 所有已建基础设施，实现 OOD 3.1 认证流程完整行为契约。

**修改文件**：
1. `modules/common-module/common-module-impl/src/main/java/.../service/AuthService.java` — 接口新增 `changePassword` 方法
2. `modules/common-module/common-module-impl/src/main/java/.../service/impl/AuthServiceImpl.java` — 全量重写
3. `modules/common-module/common-module-impl/src/test/java/.../service/AuthServiceTest.java` — 重写单元测试

## 选择理由
- AuthServiceImpl 是 Stage 3 核心任务（OOD 第 12 节 3.6），连接所有已建基础设施
- 依赖链：R12(CurrentUser) → R13(UserFacade) → R14(PasswordPolicy/PasswordChangeService) → R15(JwtTokenProvider/UserConverter/AuthModuleConfig) 全部就位
- 当前实现存在多处缺陷：无速率限制、无 TokenBlacklist 登出、无 Refresh Token 轮换、无 dummy BCrypt 时序防护、无 tokenVersion、无 passwordChangeRequired 集成
- changePassword 当前为 no-op，需完整实现
- 单个复杂行为类型，符合粒度原则

## 任务上下文

### 注入依赖（通过构造函数注入，AuthModuleConfig 已提供 @Bean）
```java
private final UserRepository userRepository;
private final PasswordEncoder passwordEncoder;
private final JwtTokenProvider jwtTokenProvider;
private final UserConverter userConverter;
private final PasswordPolicy passwordPolicy;
private final PasswordChangeService passwordChangeService;
private final RateLimitGuard rateLimitGuard;
private final TokenBlacklist tokenBlacklist;
private final LoginAttemptTracker loginAttemptTracker;
```

### AuthService 接口变更
当前接口缺少 `changePassword`，新增：
```java
void changePassword(Long userId, String oldPassword, String newPassword);
```

### OOD 3.1.1 登录流程（11 步）
```
1. rateLimitGuard.tryAcquire(clientIp) — IP 5次/10秒，失败抛 BusinessException(RATE_LIMITED)
2. LoginAttemptTracker 双维度锁定检查：
   - IP 维度：isLocked(clientIp, 20, 30min) → 锁定抛 BusinessException(ACCOUNT_LOCKED, "请30分钟后重试")
   - 用户名维度：isLocked(username, 5, 15min) → 锁定抛 BusinessException(ACCOUNT_LOCKED, "请15分钟后重试")
3. userRepository.findByUsername(username) → Optional<User>
4. Optional 为空 → dummy BCrypt(passwordEncoder.encode("dummy")) → loginAttemptTracker.recordFailure(clientIp) → throw BusinessException(LOGIN_FAILED)
5. user.enabled==false || user.deleted==true → dummy BCrypt → loginAttemptTracker 双维度 recordFailure(clientIp, username) → throw BusinessException(LOGIN_FAILED)
6. passwordEncoder.matches(password, user.password) 失败 → loginAttemptTracker.recordFailure(username) → throw BusinessException(LOGIN_FAILED)
7. 成功 → loginAttemptTracker 清除 clientIp 和 username 的失败计数
8. passwordChangeService.isChangeRequired(userId) → 决定 LoginResponse.passwordChangeRequired
9. JwtTokenProvider 签发双 token（含 tokenVersion from user.tokenVersion）
10. userConverter.toUserInfoResponse(user) → UserInfoResponse
11. 返回 LoginResponse
```

### OOD 3.1.3 Token 刷新流程（12 步）
```
1. jwtTokenProvider.validateToken(refreshToken, "refresh") → Claims，失败抛 TOKEN_REFRESH_FAILED
2. jwtTokenProvider.getUserIdFromClaims(claims) → userId
3. jwtTokenProvider.getJtiFromToken(refreshToken) → jti（供后续黑名单用）
4. userRepository.findById(userId) → User（检查 enabled/deleted，禁用抛 TOKEN_REFRESH_FAILED）
5. loginAttemptTracker 检查 username 是否锁定，锁定抛 TOKEN_REFRESH_FAILED
6. passwordChangeService.isChangeRequired(userId) → true → 抛 PasswordChangeRequiredException
7. 单独查询 user.tokenVersion（确保强一致性）：userRepository.findTokenVersionById(userId)
8. jwtTokenProvider.getTokenVersionFromClaims(claims) vs DB tokenVersion，不一致→TOKEN_REFRESH_FAILED
9. 异常刷新检测：记录刷新时间戳到 ConcurrentHashMap<Long, Deque<Long>>，5秒内超过2次→log.warn
10. JwtTokenProvider 签发新双 token（含最新 tokenVersion）
11. 返回 TokenRefreshResponse
```

### OOD 3.1.4 登出
```
1. jwtTokenProvider.validateToken(token, null) → Claims（不检查 type）
2. 提取 jti → tokenBlacklist.add(jti)
3. log.info 记录登出日志
```

### OOD 3.1.5 获取当前用户
```
1. jwtTokenProvider.validateToken(token, null) → Claims
2. jwtTokenProvider.getUserIdFromClaims(claims) → userId
3. userRepository.findById(userId) → User（不存在抛 NOT_FOUND）
4. userConverter.toUserInfoResponse(user) → UserInfoResponse
```

### OOD 3.1.6 密码变更
```
1. userRepository.findById(userId) → User
2. passwordEncoder.matches(oldPassword, user.password) → 失败抛 PASSWORD_MISMATCH
3. passwordPolicy.validate(newPassword, user.getUsername()) → 不合规抛对应 ErrorCode
4. BCrypt 编码新密码：passwordEncoder.encode(newPassword)
5. user.setPassword(encodedPassword)
6. user.setTokenVersion(user.getTokenVersion() + 1)
7. user.setPasswordChangeRequired(false)
8. 清除 SecurityContext：SecurityContextHolder.clearContext()
9. log.info 记录
```

### 更新资料（M2 修复）
```
1. jwtTokenProvider.validateToken(token, null) → Claims
2. jwtTokenProvider.getUserIdFromClaims(claims) → userId
3. userRepository.findById(userId) → User
4. request 中非 null 字段 → user.setXxx(value)
5. 移除显式 userRepository.save()，利用 JPA 脏检查
6. userConverter.toUserInfoResponse(user) → UserInfoResponse
```

### 客户端 IP 获取
```java
private String getClientIp() {
    // 从 RequestContextHolder 或 HttpServletRequest 获取
    // Phase 1: request.getRemoteAddr()
    // 如有反向代理，取 X-Forwarded-For
}
```
注：Phase 1 中简单实现，直接从当前请求获取 RemoteAddr。若 RequestContextHolder 不可用，AuthServiceImpl 方法签名改为接收 HttpServletRequest 参数，由 Controller 传入。

## ErrorCode 映射

| 场景 | ErrorCode | HTTP |
|------|-----------|------|
| 登录限流 | RATE_LIMITED | 429 |
| 账户锁定（IP 维度） | ACCOUNT_LOCKED | 429 |
| 账户锁定（用户名维度） | ACCOUNT_LOCKED | 429 |
| 登录失败（用户不存在/禁用/密码错） | LOGIN_FAILED | 200 |
| 旧密码不匹配 | PASSWORD_MISMATCH | 200 |
| 密码策略不满足 | PASSWORD_TOO_SHORT / PASSWORD_TOO_LONG / PASSWORD_WEAK / PASSWORD_CONTAINS_USERNAME | 200 |
| Token 刷新失败 | TOKEN_REFRESH_FAILED | 401 |
| 密码变更需要 | PASSWORD_CHANGE_REQUIRED | 403 |
| 令牌无效 | UNAUTHORIZED | 401 |

## 已有代码上下文

### 当前 AuthServiceImpl（`service/impl/AuthServiceImpl.java`）
- 包：`com.aimedical.modules.commonmodule.service.impl`
- 3 个注入：UserRepository, PasswordEncoder, JwtUtil
- login：直接查库（非 Optional），无限流/锁定/dummy BCrypt，用 JwtUtil 生成单 token
- logout：no-op
- refreshToken：用 JwtUtil 验证+生成单 token，无轮换/tokenVersion/异常检测
- getCurrentUser：从 token 解析 userId，查库，调用私有 buildUserInfoResponse
- updateProfile：同 getCurrentUser，含显式 save()
- changePassword：未实现（接口中不存在，Controller 中 no-op）
- buildUserInfoResponse（私有）：硬编码 position 解析和权限收集
- 重复 userId 提取代码 4 处（Integer→Long 转换）

### 当前 AuthService 接口（`service/AuthService.java`）
```java
LoginResponse login(LoginRequest request);
void logout(String token);
TokenRefreshResponse refreshToken(String token);
UserInfoResponse getCurrentUser(String token);
UserInfoResponse updateProfile(String token, ProfileUpdateRequest request);
```

### AuthController 当前调用方式
- login：authService.login(request)
- logout：authService.logout(token)
- refresh：authService.refreshToken(request.refreshToken())
- me：authService.getCurrentUser(token)
- updateMe：authService.updateProfile(token, request)
- changePassword：直接返回 Result.success(null)（no-op）

### 已有基础设施（已通过验证）
- JwtTokenProvider（`auth/jwt/`）：`@Component`，提供 generateAccessToken/generateRefreshToken/validateToken/getUserIdFromClaims/getJtiFromToken/getTokenVersionFromClaims/getAccessTokenExpirationMs
- UserConverter（`auth/converter/`）：`@Component`，提供 toUserInfoResponse(User)
- AuthModuleConfig（`auth/config/`）：`@Configuration`，提供 rateLimitGuard/tokenBlacklist/loginAttemptTracker bean
- PasswordPolicy（`auth/password/`）：validate(password, username) → ErrorCode
- PasswordChangeService（`auth/password/`）：isChangeRequired/markChangeRequired/clearChangeRequired
- RateLimitGuard + InMemoryRateLimitGuard（`auth/rateLimit/`）
- TokenBlacklist + InMemoryTokenBlacklist（`auth/blacklist/`）
- LoginAttemptTracker（`auth/login/`）
- PasswordChangeRequiredException（`auth/exception/`）

## 测试设计

### AuthServiceTest 重写

**形态**：JUnit 5，纯单元测试，使用 Mockito mock 所有依赖。

**Mock 清单**：
- UserRepository（含 findById、findByUsername、findTokenVersionById）
- PasswordEncoder
- JwtTokenProvider
- UserConverter
- PasswordPolicy
- PasswordChangeService
- RateLimitGuard
- TokenBlacklist
- LoginAttemptTracker

**测试用例**（至少 15 个）：

| # | 方法 | 场景 | 验证 |
|---|------|------|------|
| 1 | login | 正常登录 | 返回 LoginResponse，含双 token、user 信息、passwordChangeRequired=false |
| 2 | login | IP 限流触发 | RateLimitGuard.tryAcquire 返回 false → 抛 BusinessException(RATE_LIMITED) |
| 3 | login | IP 维度锁定 | LoginAttemptTracker.isLocked(IP) 返回 true → 抛 BusinessException(ACCOUNT_LOCKED) |
| 4 | login | 用户名维度锁定 | LoginAttemptTracker.isLocked(IP) 返回 false，isLocked(username) 返回 true → 抛 BusinessException(ACCOUNT_LOCKED) |
| 5 | login | 用户不存在 | Optional.empty → dummy BCrypt + IP recordFailure + 抛 BusinessException(LOGIN_FAILED) |
| 6 | login | 用户已禁用 | enabled=false → dummy BCrypt + 双维度 recordFailure + 抛 BusinessException(LOGIN_FAILED) |
| 7 | login | 密码错误 | matches 返回 false → username recordFailure + 抛 BusinessException(LOGIN_FAILED) |
| 8 | login | 首次登录需改密 | passwordChangeService.isChangeRequired 返回 true → response.passwordChangeRequired=true |
| 9 | refreshToken | 正常刷新 | 验证 claims、查用户、比对 tokenVersion、签发新双 token、返回 TokenRefreshResponse |
| 10 | refreshToken | Refresh Token 无效 | validateToken 返回 null → 抛 BusinessException(TOKEN_REFRESH_FAILED) |
| 11 | refreshToken | 用户已禁用 | enabled=false → 抛 BusinessException(TOKEN_REFRESH_FAILED) |
| 12 | refreshToken | tokenVersion 不一致 | claims tokenVersion ≠ DB tokenVersion → 抛 BusinessException(TOKEN_REFRESH_FAILED) |
| 13 | refreshToken | passwordChangeRequired=true | isChangeRequired 返回 true → 抛 PasswordChangeRequiredException |
| 14 | logout | 正常登出 | validateToken → 提取 jti → tokenBlacklist.add → 无异常 |
| 15 | getCurrentUser | 正常获取 | validateToken → getUserIdFromClaims → findById → toUserInfoResponse |
| 16 | updateProfile | 更新资料 | 更新字段 → 无 save 调用 → 返回 UserInfoResponse（验证 save 未被调用） |
| 17 | changePassword | 正常改密 | matches 成功 → validate 返回 null → 更新 password/tokenVersion/passwordChangeRequired → clearContext |
| 18 | changePassword | 旧密码错误 | matches 返回 false → 抛 BusinessException(PASSWORD_MISMATCH) |
| 19 | changePassword | 密码策略不满足 | passwordPolicy.validate 返回 ErrorCode → 抛 BusinessException(对应 ErrorCode) |

## 验证

测试命令：
```bash
mvn test -pl modules/common-module/common-module-impl -am -Dtest="AuthServiceTest" -Dsurefire.failIfNoSpecifiedTests=false
```
