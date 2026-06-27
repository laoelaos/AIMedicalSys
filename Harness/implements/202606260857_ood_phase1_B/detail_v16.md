# 详细设计（v16）

## 概述

重构 `AuthServiceImpl` 为核心认证服务，集成 R4–R15 所有已建基础设施，按 OOD 3.1 认证流程完整实现 6 个认证方法的行为契约。同步更新 `AuthService` 接口（新增 `changePassword`）和 `AuthServiceTest`（重写为 19 用例）。

## 文件规划

| 文件路径（相对 `AIMedical/backend/`） | 操作 | 职责 |
|---------|------|------|
| `modules/common-module/common-module-impl/src/main/java/.../service/AuthService.java` | 修改 | 接口新增 `changePassword(Long, String, String)` |
| `modules/common-module/common-module-impl/src/main/java/.../service/impl/AuthServiceImpl.java` | 重写 | 全量实现 OOD 3.1 认证流程 |
| `modules/common-module/common-module-impl/src/test/java/.../service/AuthServiceTest.java` | 重写 | 19 用例单元测试 |

### 附带修改（非主任务文件，编码时按需）

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `.../permission/UserRepository.java` | 修改 | 新增 `findTokenVersionById(Long)` 查询方法 |
| `.../controller/AuthController.java` | 修改 | `changePassword` 从 token 提取 userId 并传入 `authService` |

## 类型定义

### AuthService（接口）

**形态**：`interface`
**包路径**：`com.aimedical.modules.commonmodule.service`
**职责**：认证服务顶层契约。

```java
package com.aimedical.modules.commonmodule.service;

import com.aimedical.modules.commonmodule.dto.request.LoginRequest;
import com.aimedical.modules.commonmodule.dto.request.ProfileUpdateRequest;
import com.aimedical.modules.commonmodule.dto.response.LoginResponse;
import com.aimedical.modules.commonmodule.dto.response.TokenRefreshResponse;
import com.aimedical.modules.commonmodule.auth.UserInfoResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    void logout(String token);

    TokenRefreshResponse refreshToken(String token);

    UserInfoResponse getCurrentUser(String token);

    UserInfoResponse updateProfile(String token, ProfileUpdateRequest request);

    void changePassword(Long userId, String oldPassword, String newPassword);
}
```

**变更点**：新增 `changePassword(Long userId, String oldPassword, String newPassword)` 方法。userId 由调用方（Controller）从当前请求 token 中提取。

---

### AuthServiceImpl

**形态**：`@Service` class
**包路径**：`com.aimedical.modules.commonmodule.service.impl`
**职责**：全量实现 OOD 3.1 认证流程。

```java
package com.aimedical.modules.commonmodule.service.impl;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.common.exception.ErrorCode;
import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.modules.commonmodule.auth.UserInfoResponse;
import com.aimedical.modules.commonmodule.auth.blacklist.TokenBlacklist;
import com.aimedical.modules.commonmodule.auth.converter.UserConverter;
import com.aimedical.modules.commonmodule.auth.exception.PasswordChangeRequiredException;
import com.aimedical.modules.commonmodule.auth.jwt.JwtTokenProvider;
import com.aimedical.modules.commonmodule.auth.login.LoginAttemptTracker;
import com.aimedical.modules.commonmodule.auth.password.PasswordChangeService;
import com.aimedical.modules.commonmodule.auth.password.PasswordPolicy;
import com.aimedical.modules.commonmodule.auth.rateLimit.RateLimitGuard;
import com.aimedical.modules.commonmodule.dto.request.LoginRequest;
import com.aimedical.modules.commonmodule.dto.request.ProfileUpdateRequest;
import com.aimedical.modules.commonmodule.dto.response.LoginResponse;
import com.aimedical.modules.commonmodule.dto.response.TokenRefreshResponse;
import com.aimedical.modules.commonmodule.permission.User;
import com.aimedical.modules.commonmodule.permission.UserRepository;
import com.aimedical.modules.commonmodule.service.AuthService;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Deque;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private static final long REFRESH_WINDOW_MS = 5_000L;
    private static final int REFRESH_MAX_COUNT = 2;
    private static final int RATE_LIMIT_MAX = 5;
    private static final long RATE_LIMIT_WINDOW_MS = 10_000L;
    private static final int IP_LOCK_THRESHOLD = 20;
    private static final long IP_LOCK_DURATION_MS = 30 * 60 * 1000L;
    private static final int USERNAME_LOCK_THRESHOLD = 5;
    private static final long USERNAME_LOCK_DURATION_MS = 15 * 60 * 1000L;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserConverter userConverter;
    private final PasswordPolicy passwordPolicy;
    private final PasswordChangeService passwordChangeService;
    private final RateLimitGuard rateLimitGuard;
    private final TokenBlacklist tokenBlacklist;
    private final LoginAttemptTracker loginAttemptTracker;

    private final ConcurrentHashMap<Long, Deque<Long>> refreshTimestamps = new ConcurrentHashMap<>();

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            UserConverter userConverter,
            PasswordPolicy passwordPolicy,
            PasswordChangeService passwordChangeService,
            RateLimitGuard rateLimitGuard,
            TokenBlacklist tokenBlacklist,
            LoginAttemptTracker loginAttemptTracker) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userConverter = userConverter;
        this.passwordPolicy = passwordPolicy;
        this.passwordChangeService = passwordChangeService;
        this.rateLimitGuard = rateLimitGuard;
        this.tokenBlacklist = tokenBlacklist;
        this.loginAttemptTracker = loginAttemptTracker;
    }

    @Override
    public LoginResponse login(LoginRequest request) { /* 见行为契约 */ }

    @Override
    public void logout(String token) { /* 见行为契约 */ }

    @Override
    public TokenRefreshResponse refreshToken(String token) { /* 见行为契约 */ }

    @Override
    public UserInfoResponse getCurrentUser(String token) { /* 见行为契约 */ }

    @Override
    @Transactional
    public UserInfoResponse updateProfile(String token, ProfileUpdateRequest request) { /* 见行为契约 */ }

    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) { /* 见行为契约 */ }

    private String getClientIp() { /* 见行为契约 */ }
}
```

**注入依赖**：

| 字段 | 类型 | 来源 |
|------|------|------|
| `userRepository` | `UserRepository` | Spring Data JPA |
| `passwordEncoder` | `PasswordEncoder` | Spring Security |
| `jwtTokenProvider` | `JwtTokenProvider` | `@Component`（R14） |
| `userConverter` | `UserConverter` | `@Component`（R14） |
| `passwordPolicy` | `PasswordPolicy` | `@Component`（R14） |
| `passwordChangeService` | `PasswordChangeService` | `@Component`（R14） |
| `rateLimitGuard` | `RateLimitGuard` | `@Bean`（AuthModuleConfig） |
| `tokenBlacklist` | `TokenBlacklist` | `@Bean`（AuthModuleConfig） |
| `loginAttemptTracker` | `LoginAttemptTracker` | `@Bean`（AuthModuleConfig） |

**私有字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `refreshTimestamps` | `ConcurrentHashMap<Long, Deque<Long>>` | 异常刷新检测；userId → 最近刷新时间戳队列 |

**构造方式**：9 参数构造器注入。

---

### UserRepository（附带修改）

**形态**：`interface extends JpaRepository<User, Long>`
**包路径**：`com.aimedical.modules.commonmodule.permission`

新增方法：

```java
@Query("SELECT u.tokenVersion FROM User u WHERE u.id = :id")
Optional<Integer> findTokenVersionById(@Param("id") Long id);
```

---

### AuthController（附带修改）

**形态**：`@RestController`
**包路径**：`com.aimedical.modules.commonmodule.controller`

`changePassword` 端点改为调用 `authService`：

```java
@PutMapping("/password")
public Result<Void> changePassword(
        @RequestHeader("Authorization") String authHeader,
        @Valid @RequestBody PasswordChangeRequest request) {
    String token = extractToken(authHeader);
    Claims claims = jwtTokenProvider.validateToken(token, null);
    Long userId = jwtTokenProvider.getUserIdFromClaims(claims);
    authService.changePassword(userId, request.oldPassword(), request.newPassword());
    return Result.success(null);
}
```

依赖注入新增 `JwtTokenProvider` 字段（`JwtUtil` 保留或移除）。

---

## 方法行为契约

### login(LoginRequest)

```
前置条件：
  - request.username 非空
  - request.password 非空
  - 已确保 @Valid 校验通过

流程：
  1. getClientIp() → clientIp
  2. rateLimitGuard.tryAcquire(clientIp, RATE_LIMIT_MAX, RATE_LIMIT_WINDOW_MS)
     → false → throw BusinessException(RATE_LIMITED)
  3. loginAttemptTracker.isIpLocked(clientIp)
     → true → throw BusinessException(ACCOUNT_LOCKED, "请30分钟后重试")
  4. loginAttemptTracker.isUsernameLocked(request.username())
     → true → throw BusinessException(ACCOUNT_LOCKED, "请15分钟后重试")
  5. userRepository.findByUsername(request.username()) → Optional<User>
  6. Optional 为空：
     → passwordEncoder.encode("dummy") 执行 dummy BCrypt（时序防护）
     → loginAttemptTracker.recordIpFailure(clientIp)
     → throw BusinessException(LOGIN_FAILED)
  7. user.getEnabled() == false || user.getDeleted() == true：
     → passwordEncoder.encode("dummy") dummy BCrypt
     → loginAttemptTracker.recordIpFailure(clientIp)
     → loginAttemptTracker.recordUsernameFailure(user.getUsername())
     → throw BusinessException(LOGIN_FAILED)
  8. passwordEncoder.matches(request.password(), user.getPassword()) == false：
     → loginAttemptTracker.recordUsernameFailure(user.getUsername())
     → throw BusinessException(LOGIN_FAILED)
  9. 登录成功：
     → loginAttemptTracker.clearIp(clientIp)
     → loginAttemptTracker.clearUsername(user.getUsername())
 10. passwordChangeService.isChangeRequired(user.getId())
     → boolean passwordChangeRequired
 11. String accessToken = jwtTokenProvider.generateAccessToken(
         user.getId(), user.getUsername(), user.getUserType().getCode())
 12. String refreshToken = jwtTokenProvider.generateRefreshToken(
         user.getId(), user.getUsername(), user.getUserType().getCode(), user.getTokenVersion())
 13. UserInfoResponse userInfo = userConverter.toUserInfoResponse(user)
 14. Return new LoginResponse(user.getId(), user.getUsername(),
         accessToken, refreshToken, "Bearer",
         jwtTokenProvider.getAccessTokenExpirationMs(),
         passwordChangeRequired, userInfo)

异常：
  - RATE_LIMITED（步骤 2）
  - ACCOUNT_LOCKED（步骤 3/4）
  - LOGIN_FAILED（步骤 6/7/8）
  - 本方法不抛其他 BusinessException

后置条件：
  - 登录成功时：认证成功返回 LoginResponse，内含有效双 token
```

### logout(String)

```
前置条件：
  - token 可为 null（返回而不抛异常）

流程：
  1. token == null → return
  2. jwtTokenProvider.validateToken(token, null) → Claims claims
  3. claims == null → return（无效 token 不登出）
  4. String jti = jwtTokenProvider.getJtiFromToken(token)
  5. 若 jti != null：
     tokenBlacklist.add(jti, claims.getExpiration().getTime())
  6. log.info("用户登出成功")

异常：
  - 不抛异常

后置条件：
  - 如 token 有效，其 jti 被加入黑名单
```

### refreshToken(String)

```
前置条件：
  - token 非空

流程：
  1. jwtTokenProvider.validateToken(token, "refresh") → Claims claims
     → claims == null → throw BusinessException(TOKEN_REFRESH_FAILED)
  2. Long userId = jwtTokenProvider.getUserIdFromClaims(claims)
     → null → throw BusinessException(TOKEN_REFRESH_FAILED)
  3. String jti = jwtTokenProvider.getJtiFromToken(token)
  4. userRepository.findById(userId) → Optional<User>
     → 空或 user.getEnabled() == false 或 user.getDeleted() == true
       → throw BusinessException(TOKEN_REFRESH_FAILED)
  5. loginAttemptTracker.isUsernameLocked(user.getUsername())
     → true → throw BusinessException(TOKEN_REFRESH_FAILED)
  6. passwordChangeService.isChangeRequired(userId)
     → true → throw PasswordChangeRequiredException("需要修改密码")
  7. userRepository.findTokenVersionById(userId) → Optional<Integer>
     → 空或不存在 → throw BusinessException(TOKEN_REFRESH_FAILED)
  8. jwtTokenProvider.getTokenVersionFromClaims(claims) vs DB tokenVersion
     → 不匹配 → throw BusinessException(TOKEN_REFRESH_FAILED)
  9. 异常刷新检测：
     long now = System.currentTimeMillis()
     refreshTimestamps.compute(userId, (k, deque) -> {
         if (deque == null) deque = new ArrayDeque<>();
         deque.addLast(now);
         while (!deque.isEmpty() && deque.peekFirst() < now - REFRESH_WINDOW_MS) {
             deque.pollFirst();
         }
         if (deque.size() > REFRESH_MAX_COUNT) {
             log.warn("异常刷新检测: userId={}, 5秒内刷新{}次", userId, deque.size());
         }
         return deque;
     })
 10. String newAccessToken = jwtTokenProvider.generateAccessToken(
         user.get().getId(), user.get().getUsername(), user.get().getUserType().getCode())
 11. String newRefreshToken = jwtTokenProvider.generateRefreshToken(
         user.get().getId(), user.get().getUsername(), user.get().getUserType().getCode(),
         dbTokenVersion)
 12. Return new TokenRefreshResponse(newAccessToken, newRefreshToken,
         "Bearer", jwtTokenProvider.getAccessTokenExpirationMs())

异常：
  - TOKEN_REFRESH_FAILED（步骤 1/2/4/5/7/8）
  - PasswordChangeRequiredException（步骤 6）

后置条件：
  - 刷新成功：旧 refresh token 仍在有效期，但新双 token 已签发
```

### getCurrentUser(String)

```
前置条件：
  - token 非空（调用方保证，Controller 中 null 提前返回）

流程：
  1. jwtTokenProvider.validateToken(token, null) → Claims claims
     → claims == null → throw BusinessException(UNAUTHORIZED, "令牌无效")
  2. Long userId = jwtTokenProvider.getUserIdFromClaims(claims)
  3. userRepository.findById(userId) → Optional<User>
     → 空 → throw BusinessException(NOT_FOUND, "用户不存在")
  4. Return userConverter.toUserInfoResponse(user)

异常：
  - UNAUTHORIZED（令牌无效）
  - NOT_FOUND（用户不存在）

后置条件：
  - 返回 UserInfoResponse 包含完整用户信息和权限
```

### updateProfile(String, ProfileUpdateRequest)

```
前置条件：
  - token 非空
  - @Valid ProfileUpdateRequest 校验通过

流程：
  1. jwtTokenProvider.validateToken(token, null) → Claims claims
     → claims == null → throw BusinessException(UNAUTHORIZED)
  2. Long userId = jwtTokenProvider.getUserIdFromClaims(claims)
  3. userRepository.findById(userId) → Optional<User>
     → 空 → throw BusinessException(NOT_FOUND)
  4. 非 null 字段赋值：
     request.nickname() != null → user.setNickname(request.nickname())
     request.phone() != null → user.setPhone(request.phone())
     request.email() != null → user.setEmail(request.email())
  5. 不移除显式 userRepository.save()，利用 JPA 脏检查自动持久化
  6. Return userConverter.toUserInfoResponse(user)

异常：
  - UNAUTHORIZED
  - NOT_FOUND

后置条件：
  - 用户实体被更新（脏检查自动 flush）
  - save() 未被显式调用
```

### changePassword(Long, String, String)

```
前置条件：
  - userId 非 null
  - oldPassword 非空
  - newPassword 非空且满足 @Size(min=8, max=64)

流程：
  1. userRepository.findById(userId) → Optional<User>
     → 空 → throw BusinessException(NOT_FOUND)
  2. passwordEncoder.matches(oldPassword, user.getPassword()) == false
     → throw BusinessException(PASSWORD_MISMATCH)
  3. ErrorCode policyError = passwordPolicy.validate(newPassword, user.getUsername())
     → policyError != null → throw BusinessException(policyError)
  4. String encodedPassword = passwordEncoder.encode(newPassword)
  5. user.setPassword(encodedPassword)
  6. user.setTokenVersion(user.getTokenVersion() + 1)
  7. user.setPasswordChangeRequired(false)
  8. passwordChangeService.clearChangeRequired(userId)
  9. SecurityContextHolder.clearContext()
  10. log.info("用户密码修改成功，userId: {}", userId)

异常：
  - NOT_FOUND（步骤 1）
  - PASSWORD_MISMATCH（步骤 2）
  - PASSWORD_TOO_SHORT / PASSWORD_TOO_LONG / PASSWORD_WEAK / PASSWORD_CONTAINS_USERNAME（步骤 3）

后置条件：
  - 用户密码已更新为 BCrypt 新密码
  - tokenVersion 已递增
  - passwordChangeRequired 已重置为 false
  - SecurityContext 已清除
```

### getClientIp()

```
实现：
  1. ServletRequestAttributes attrs = (ServletRequestAttributes)
       RequestContextHolder.currentRequestAttributes()
  2. HttpServletRequest request = attrs.getRequest()
  3. String xff = request.getHeader("X-Forwarded-For")
  4. 若 xff 非空且非空白 → xff.split(",")[0].trim()
  5. 否则 → request.getRemoteAddr()
  6. RequestContextHolder 不可用 → return "unknown"

异常：
  - 不抛异常
```

---

## 错误处理

| 方法 | ErrorCode | 条件 | HTTP |
|------|-----------|------|------|
| login | RATE_LIMITED | IP 限流触发（5次/10秒） | 429 |
| login | ACCOUNT_LOCKED | IP 维度锁定（20次/30分钟） | 429 |
| login | ACCOUNT_LOCKED | 用户名维度锁定（5次/15分钟） | 429 |
| login | LOGIN_FAILED | 用户不存在 / 禁用 / 密码错误 | 200 |
| refreshToken | TOKEN_REFRESH_FAILED | token 无效 / 用户禁用 / 锁定 / tokenVersion 不一致 | 401 |
| refreshToken | PasswordChangeRequiredException | 需修改密码 | 403 |
| getCurrentUser | UNAUTHORIZED | token 无效 | 401 |
| getCurrentUser | NOT_FOUND | 用户不存在 | 404 |
| updateProfile | UNAUTHORIZED | token 无效 | 401 |
| updateProfile | NOT_FOUND | 用户不存在 | 404 |
| changePassword | NOT_FOUND | 用户不存在 | 404 |
| changePassword | PASSWORD_MISMATCH | 旧密码不匹配 | 200 |
| changePassword | PASSWORD_TOO_SHORT | 密码太短 | 200 |
| changePassword | PASSWORD_TOO_LONG | 密码太长 | 200 |
| changePassword | PASSWORD_WEAK | 密码复杂度不足 | 200 |
| changePassword | PASSWORD_CONTAINS_USERNAME | 密码含用户名 | 200 |

**BusinessException 传播**：所有 BusinessException 直接抛出，由 `GlobalExceptionHandler` 统一处理。

**PasswordChangeRequiredException**：继承 `AccessDeniedException`，由 Spring Security 异常处理链或 `GlobalExceptionHandler` 处理。

---

## 依赖关系

### 新建/修改类型

| 类型 | 包路径 | 说明 |
|------|--------|------|
| `AuthService` | `...service` | 接口，新增 `changePassword` |
| `AuthServiceImpl` | `...service.impl` | 全量重写，9 注入依赖 |
| `AuthServiceTest` | `...service` | 重写，19 用例 |

### 依赖的已有类型

| 类型 | 被谁依赖 | 方式 |
|------|---------|------|
| `UserRepository` | `AuthServiceImpl` | 构造器注入 |
| `PasswordEncoder` | `AuthServiceImpl` | 构造器注入 |
| `JwtTokenProvider` | `AuthServiceImpl` | 构造器注入 |
| `UserConverter` | `AuthServiceImpl` | 构造器注入 |
| `PasswordPolicy` | `AuthServiceImpl` | 构造器注入 |
| `PasswordChangeService` | `AuthServiceImpl` | 构造器注入 |
| `RateLimitGuard` | `AuthServiceImpl` | 构造器注入 |
| `TokenBlacklist` | `AuthServiceImpl` | 构造器注入 |
| `LoginAttemptTracker` | `AuthServiceImpl` | 构造器注入 |
| `User` | `AuthServiceImpl` | 实体查询结果 |
| `LoginRequest` / `LoginResponse` | `AuthServiceImpl` | 输入/输出 DTO |
| `TokenRefreshResponse` | `AuthServiceImpl` | 输出 DTO |
| `UserInfoResponse` | `AuthServiceImpl` | 输出 DTO |
| `Claims` | `AuthServiceImpl` | JWT 解析中间体 |
| `BusinessException` / `GlobalErrorCode` | `AuthServiceImpl` | 异常 |
| `PasswordChangeRequiredException` | `AuthServiceImpl` | 异常 |
| `RequestContextHolder` / `HttpServletRequest` | `AuthServiceImpl` | IP 获取 |

### 暴露给后续任务的公开接口

| 类型 | 后续使用者 |
|------|-----------|
| `AuthService` | `AuthController` （所有端点） |

---

## 单元测试设计

### AuthServiceTest

**形态**：class（JUnit 5），纯单元测试，使用 Mockito mock 所有 9 个依赖。

**包路径**：`com.aimedical.modules.commonmodule.service`

**Mock 清单**：
- `UserRepository`（R2）
- `PasswordEncoder`（R2）
- `JwtTokenProvider`（R14）
- `UserConverter`（R14）
- `PasswordPolicy`（R14）
- `PasswordChangeService`（R14）
- `RateLimitGuard`（R14）
- `TokenBlacklist`（R14）
- `LoginAttemptTracker`（R14）

**测试夹具**：

```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock UserConverter userConverter;
    @Mock PasswordPolicy passwordPolicy;
    @Mock PasswordChangeService passwordChangeService;
    @Mock RateLimitGuard rateLimitGuard;
    @Mock TokenBlacklist tokenBlacklist;
    @Mock LoginAttemptTracker loginAttemptTracker;

    private AuthServiceImpl authService;
    private User testUser;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
            userRepository, passwordEncoder, jwtTokenProvider, userConverter,
            passwordPolicy, passwordChangeService, rateLimitGuard,
            tokenBlacklist, loginAttemptTracker);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("$2a$10$...");
        testUser.setNickname("TestUser");
        testUser.setUserType(UserType.DOCTOR);
        testUser.setEnabled(true);
        testUser.setDeleted(false);
        testUser.setTokenVersion(1);
        testUser.setPasswordChangeRequired(false);
        // 设置 roles/posts 等按需
    }

    // 需要 mock 静态方法 RequestContextHolder 时，使用 try-with-resources MockedStatic
}
```

**关键技术约束**：
1. `login` 方法中 `getClientIp()` 使用 `RequestContextHolder` → 需通过 `MockedStatic<RequestContextHolder>` mock 或重构为 package-private 方法以测试。
2. 推荐策略：将 `getClientIp()` 提取为 package-private 方法，测试通过 mock `ServletRequestAttributes` 覆盖；或直接在 login 测试中使用 `try (MockedStatic<RequestContextHolder> ctx = mockStatic(...))`。

**测试用例**（19 个）：

| # | 测试方法 | 测试场景 | Arrange | Assert |
|---|---------|---------|---------|--------|
| 1 | `login_shouldSucceed` | 正常登录 | rateLimitGuard.tryAcquire → true；isIpLocked/isUsernameLocked → false；findByUsername → Optional.of(testUser)；matches → true；isChangeRequired → false；JwtTokenProvider 返回 mock token；UserConverter 返回 mock UserInfoResponse | LoginResponse 非 null；含 accessToken、refreshToken；passwordChangeRequired=false |
| 2 | `login_shouldThrowRateLimited` | IP 限流 | rateLimitGuard.tryAcquire → false | BusinessException(RATE_LIMITED) |
| 3 | `login_shouldThrowIpLocked` | IP 维度锁定 | rateLimitGuard.tryAcquire → true；isIpLocked → true | BusinessException(ACCOUNT_LOCKED) |
| 4 | `login_shouldThrowUsernameLocked` | 用户名维度锁定 | rateLimitGuard.tryAcquire → true；isIpLocked → false；isUsernameLocked → true | BusinessException(ACCOUNT_LOCKED) |
| 5 | `login_shouldThrowUserNotFound` | 用户不存在 | rateLimitGuard.tryAcquire → true；锁定检查 false；findByUsername → Optional.empty | 验证 dummy BCrypt 调用；recordIpFailure 调用；BusinessException(LOGIN_FAILED) |
| 6 | `login_shouldThrowUserDisabled` | 用户已禁用 | findByUsername → Optional.of(disabled user)；matches 不执行（dummy BCrypt） | dummy BCrypt；双维度 recordFailure；BusinessException(LOGIN_FAILED) |
| 7 | `login_shouldThrowPasswordMismatch` | 密码错误 | findByUsername → Optional.of(testUser)；matches → false | recordUsernameFailure；BusinessException(LOGIN_FAILED) |
| 8 | `login_shouldSetPasswordChangeRequired` | 首次登录需改密 | 正常登录 + isChangeRequired → true | LoginResponse.passwordChangeRequired == true |
| 9 | `refreshToken_shouldSucceed` | 正常刷新 | validateToken → Claims；getUserIdFromClaims → 1L；getJtiFromToken → "jti-xxx"；findById → Optional.of(testUser)；isUsernameLocked → false；isChangeRequired → false；findTokenVersionById → 1；getTokenVersionFromClaims → 1；generateAccessToken/RefreshToken → 新 token | TokenRefreshResponse 含新双 token |
| 10 | `refreshToken_shouldThrowOnInvalidToken` | Refresh Token 无效 | validateToken → null | BusinessException(TOKEN_REFRESH_FAILED) |
| 11 | `refreshToken_shouldThrowOnDisabledUser` | 用户已禁用 | validateToken → Claims；findById → Optional.of(disabled user) | BusinessException(TOKEN_REFRESH_FAILED) |
| 12 | `refreshToken_shouldThrowOnTokenVersionMismatch` | tokenVersion 不一致 | 登录检查通过；findTokenVersionById → 2；getTokenVersionFromClaims → 1 | BusinessException(TOKEN_REFRESH_FAILED) |
| 13 | `refreshToken_shouldThrowPasswordChangeRequired` | passwordChangeRequired=true | isChangeRequired → true | PasswordChangeRequiredException |
| 14 | `logout_shouldBlacklistToken` | 正常登出 | validateToken → Claims；getJtiFromToken → "jti-xxx" | tokenBlacklist.add("jti-xxx", expiration) 被调用；无异常 |
| 15 | `getCurrentUser_shouldSucceed` | 正常获取 | validateToken → Claims；getUserIdFromClaims → 1L；findById → Optional.of(testUser)；toUserInfoResponse → mock | UserInfoResponse 非 null |
| 16 | `updateProfile_shouldUpdateFieldsWithoutSave` | 更新资料 | validateToken → Claims；findById → Optional.of(testUser)；save → **未被调用**；toUserInfoResponse → mock | save 未被调用（verify(save, never())）；setNickname/setPhone/setEmail 被调用 |
| 17 | `changePassword_shouldSucceed` | 正常改密 | findById → Optional.of(testUser)；matches → true；validate → null；encode → 新 hash | setPassword/setTokenVersion(+1)/setPasswordChangeRequired(false) 被调用；clearContext 被调用 |
| 18 | `changePassword_shouldThrowOnOldPasswordMismatch` | 旧密码错误 | findById → Optional.of(testUser)；matches → false | BusinessException(PASSWORD_MISMATCH) |
| 19 | `changePassword_shouldThrowOnPolicyViolation` | 密码策略不满足 | matches → true；validate → PASSWORD_WEAK | BusinessException(PASSWORD_WEAK) |

**测试关键细节**：
- 用例 5-6：`passwordEncoder.encode("dummy")` 必须被调用（dummy BCrypt 时序防护）。使用 `verify(passwordEncoder).encode("dummy")` 验证。
- 用例 6：双维度 recordFailure 验证：`verify(loginAttemptTracker).recordIpFailure(anyString())` 和 `verify(loginAttemptTracker).recordUsernameFailure(anyString())`。
- 用例 8：passwordChangeRequired=true 时 LoginResponse.passwordChangeRequired 必须为 true。
- 用例 12：findTokenVersionById 返回与 claims 中不一致的值。
- 用例 16：显式 `verify(userRepository, never()).save(any())`。
- 用例 17：验证 `SecurityContextHolder.clearContext()` 被调用（使用 `MockedStatic<SecurityContextHolder>`）。
- getClientIp 测试：可通过将方法改为 package-private 后用 `@Spy` 控制，或直接集成 `MockedStatic<RequestContextHolder>` 的方式在 login 测试中间接覆盖。

---

## 行为契约补充说明

### login — dummy BCrypt 时序防护

当用户不存在或禁用时，不提前返回，而是先执行 `passwordEncoder.encode("dummy")` 消耗等量 BCrypt 计算时间，防止通过响应时间差异推断用户是否存在。密码错误场景不需要 dummy BCrypt（`matches` 本身已消耗等量 BCrypt 时间）。

### login — 清除锁定计数

登录成功时同时清除 `clientIp` 和 `username` 的失败计数。

### refreshToken — findTokenVersionById 强一致性

单独查询 `tokenVersion` 而非使用步骤 4 中 `findById` 结果，确保读取到最新提交的版本号（避免 JPA 一级缓存的脏读）。

### refreshToken — 异常刷新检测

使用 `ConcurrentHashMap<Long, Deque<Long>>` 记录每个 userId 最近 5 秒内的刷新时间戳。若超过 2 次记录 warn 日志，不阻断流程。

### updateProfile — 移除显式 save()

利用 JPA 实体管理器的脏检查（dirty checking）自动持久化变更。必须在 `@Transactional` 事务内执行，且方法完成后事务提交时自动 flush。

### changePassword — SecurityContext 清除

密码变更后调用 `SecurityContextHolder.clearContext()` 清除当前安全上下文，强制用户使用新密码重新认证。

---

## 验证

测试命令：
```bash
mvn test -pl modules/common-module/common-module-impl -am -Dtest="AuthServiceTest" -Dsurefire.failIfNoSpecifiedTests=false
```
