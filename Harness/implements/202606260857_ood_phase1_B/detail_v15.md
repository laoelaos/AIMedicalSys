# 详细设计（v15）

## 概述

在 `common-module-impl` 下新建 3 组基础类型（共 6 个文件，含测试），为 Stage 3 AuthServiceImpl 全量重组提供前置基础设施：

1. **JwtTokenProvider** — 集中式 JWT 提供者，封装 Access/Refresh Token 的全生命周期操作
2. **UserConverter** — User 实体 → UserInfoResponse DTO 的转换器
3. **AuthModuleConfig** — `@Configuration` Bean 装配

## 文件规划

| 文件路径（相对 `AIMedical/backend/`） | 操作 | 职责 |
|---------|------|------|
| `modules/common-module/common-module-impl/src/main/java/.../auth/jwt/JwtTokenProvider.java` | 新建 | JWT 令牌提供者 `@Component` |
| `modules/common-module/common-module-impl/src/main/java/.../auth/converter/UserConverter.java` | 新建 | User→UserInfoResponse 转换 `@Component` |
| `modules/common-module/common-module-impl/src/main/java/.../auth/config/AuthModuleConfig.java` | 新建 | Bean 装配 `@Configuration` |
| `modules/common-module/common-module-impl/src/test/java/.../auth/jwt/JwtTokenProviderTest.java` | 新建 | JwtTokenProvider 单元测试（8 用例） |
| `modules/common-module/common-module-impl/src/test/java/.../auth/converter/UserConverterTest.java` | 新建 | UserConverter 单元测试（5 用例） |
| `modules/common-module/common-module-impl/src/test/java/.../auth/config/AuthModuleConfigTest.java` | 新建 | AuthModuleConfig 上下文验证测试 |

## 类型定义

### JwtTokenProvider

**形态**：`@Component` class
**包路径**：`com.aimedical.modules.commonmodule.auth.jwt`
**职责**：集中式 JWT 令牌提供者，封装 Access Token 和 Refresh Token 的全生命周期操作。

```java
package com.aimedical.modules.commonmodule.auth.jwt;

import com.aimedical.modules.commonmodule.jwt.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final long ACCESS_TOKEN_EXPIRATION_MS = 900_000L;   // 15 min
    private static final long REFRESH_TOKEN_EXPIRATION_MS = 604_800_000L; // 7 days

    private final JwtConfig jwtConfig;
    private SecretKey secretKey;

    public JwtTokenProvider(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    @PostConstruct
    public void init() {
        String secret = jwtConfig.getSecret();
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        // keyBytes 长度 ≥ 32 由 JwtConfig.validate() 保障
        this.secretKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Long userId, String username, String userType, String jti) {
        // Jwts.builder() + claims + signWith(secretKey) + compact
    }

    public String generateAccessToken(Long userId, String username, String userType) {
        return generateAccessToken(userId, username, userType, UUID.randomUUID().toString());
    }

    public String generateRefreshToken(Long userId, String username, String userType, int tokenVersion, String jti) {
        // Jwts.builder() + claims（含 type="refresh", tokenVersion）+ signWith(secretKey) + compact
    }

    public String generateRefreshToken(Long userId, String username, String userType, int tokenVersion) {
        return generateRefreshToken(userId, username, userType, tokenVersion, UUID.randomUUID().toString());
    }

    public Claims validateToken(String token, String expectedType) {
        // 返回 null 不抛异常
    }

    public Long getUserIdFromClaims(Claims claims) {
        // Object userId → Integer → Long 兼容
    }

    public String getJtiFromToken(String token) {
        // 从 token 中提取 jti claim
    }

    public int getTokenVersionFromClaims(Claims claims) {
        // 从 claims 中提取 tokenVersion
    }

    public long getAccessTokenExpirationMs() {
        return ACCESS_TOKEN_EXPIRATION_MS;
    }
}
```

**公开接口**：

| 方法签名 | 返回 | 说明 |
|---------|------|------|
| `String generateAccessToken(Long userId, String username, String userType, String jti)` | `String` | 生成 Access Token，claims 含 sub=username, userId, userType, jti, iat, exp；有效期 15 分钟；不含 type claim |
| `String generateAccessToken(Long userId, String username, String userType)` | `String` | 同上，jti 由 `UUID.randomUUID().toString()` 自动生成 |
| `String generateRefreshToken(Long userId, String username, String userType, int tokenVersion, String jti)` | `String` | 生成 Refresh Token，claims 含 sub=username, userId, userType, type="refresh", tokenVersion, jti, iat, exp；有效期 7 天 |
| `String generateRefreshToken(Long userId, String username, String userType, int tokenVersion)` | `String` | 同上，jti 由 `UUID.randomUUID().toString()` 自动生成 |
| `Claims validateToken(String token, String expectedType)` | `Claims` | 验证签名+有效期，检查 type claim（expectedType 为 null 时跳过）；失败返回 null 不抛异常 |
| `Long getUserIdFromClaims(Claims claims)` | `Long` | 统一提取 userId，处理 Integer→Long 兼容 |
| `String getJtiFromToken(String token)` | `String` | 从 token 中提取 jti claim |
| `int getTokenVersionFromClaims(Claims claims)` | `int` | 从 claims 中提取 tokenVersion |
| `long getAccessTokenExpirationMs()` | `long` | 返回 900000L（15 分钟毫秒值） |

**构造方式**：通过 `@Component` + 构造器注入 `JwtConfig`，`@PostConstruct init()` 从 `JwtConfig.getSecret()` Base64 解码后构造 `SecretKey` 并缓存为字段。

**类型关系**：无接口实现，纯 `@Component` class。

---

### UserConverter

**形态**：`@Component` class
**包路径**：`com.aimedical.modules.commonmodule.auth.converter`
**职责**：User 实体 → UserInfoResponse DTO 的转换逻辑抽取（M1 修复），替代 AuthServiceImpl 中的私有 `buildUserInfoResponse()` 方法。

```java
package com.aimedical.modules.commonmodule.auth.converter;

import com.aimedical.modules.commonmodule.auth.UserInfoResponse;
import com.aimedical.modules.commonmodule.permission.PermissionFunction;
import com.aimedical.modules.commonmodule.permission.Post;
import com.aimedical.modules.commonmodule.permission.Role;
import com.aimedical.modules.commonmodule.permission.User;
import org.springframework.stereotype.Component;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserConverter {

    public UserInfoResponse toUserInfoResponse(User user) {
        // id → user.getId()
        // username → user.getUsername()
        // realName → user.getNickname()
        // phone → user.getPhone()
        // email → user.getEmail()
        // role → 主角色 code（按 Role.sort 升序取第一个；无角色返回 ""）
        // position → 用户岗位 code（取第一个非空元素的 code；无岗位返回 ""）
        // permissions → Set<String>，从所有 posts→functions 及 roles→posts→functions 聚合 function.getCode()
    }

    private String resolveRole(User user) {
        // roles 为 null 或空返回 ""
        // 否则按 sort 升序排序取第一个的 code
    }

    private String resolvePosition(User user) {
        // posts 为 null 或空返回 ""
        // 否则取第一个非空元素 code
    }

    private Set<String> resolvePermissions(User user) {
        // 从 posts→functions 及 roles→posts→functions 聚合 function.getCode()
    }
}
```

**公开接口**：

| 方法签名 | 返回 | 说明 |
|---------|------|------|
| `UserInfoResponse toUserInfoResponse(User user)` | `UserInfoResponse` | 将 User 实体转换为 UserInfoResponse record |

**转换规则**：
- `id` → `user.getId()`
- `username` → `user.getUsername()`
- `realName` → `user.getNickname()`（OOD 5.2 字段映射）
- `phone` → `user.getPhone()`
- `email` → `user.getEmail()`
- `role` → 主角色 code：按 `Role.sort` 升序排序（值越小优先级越高）取第一个；用户无角色时返回空字符串 `""`
- `position` → 用户岗位 code：取 `user.getPosts()` 第一个非空元素的 `code`；无岗位时返回空字符串 `""`
- `permissions` → `Set<String>`：从所有 posts→functions 聚合 `function.getCode()`；同时从 roles→posts→functions 聚合（角色通过岗位携带功能权限）

**构造方式**：无参构造器（无注入依赖），由 Spring `@Component` 扫描自动创建单例。

**类型关系**：无接口，`@Component` 注入。

---

### AuthModuleConfig

**形态**：`@Configuration` class
**包路径**：`com.aimedical.modules.commonmodule.auth.config`
**职责**：Bean 装配定义，提供给 `AuthServiceImpl` 等组件注入所需的 RateLimitGuard、TokenBlacklist 等。

```java
package com.aimedical.modules.commonmodule.auth.config;

import com.aimedical.modules.commonmodule.auth.blacklist.InMemoryTokenBlacklist;
import com.aimedical.modules.commonmodule.auth.blacklist.TokenBlacklist;
import com.aimedical.modules.commonmodule.auth.login.LoginAttemptTracker;
import com.aimedical.modules.commonmodule.auth.rateLimit.InMemoryRateLimitGuard;
import com.aimedical.modules.commonmodule.auth.rateLimit.RateLimitGuard;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthModuleConfig {

    @Bean
    public RateLimitGuard rateLimitGuard() {
        return new InMemoryRateLimitGuard();
    }

    @Bean
    public TokenBlacklist tokenBlacklist() {
        return new InMemoryTokenBlacklist();
    }

    @Bean
    public LoginAttemptTracker loginAttemptTracker() {
        return new LoginAttemptTracker();
    }
}
```

**公开接口**：

| Bean 名称 | 类型 | 说明 |
|-----------|------|------|
| `rateLimitGuard` | `RateLimitGuard` | `new InMemoryRateLimitGuard()` 内存实现 |
| `tokenBlacklist` | `TokenBlacklist` | `new InMemoryTokenBlacklist()` 内存实现 |
| `loginAttemptTracker` | `LoginAttemptTracker` | `new LoginAttemptTracker()` 默认构造 |

**构造方式**：`@Configuration` + `@Bean` 方法，无字段注入。

**类型关系**：纯 `@Configuration` 配置类，无接口实现。

## 错误处理

### JwtTokenProvider

| 方法 | 策略 |
|------|------|
| `validateToken(String, String)` | 失败返回 null，不抛异常。内部 catch `ExpiredJwtException`、`SignatureException`、`MalformedJwtException`、`IllegalArgumentException` 等 |
| `getJtiFromToken(String)` | token 解析失败返回 null |
| `getUserIdFromClaims(Claims)` | claims 中无 userId 或无合适类型返回 null |
| `getTokenVersionFromClaims(Claims)` | claims 中无 tokenVersion 返回 0 |
| 生成方法 | 参数为 null 时可能抛 NPE（调用方保证非 null） |

### UserConverter

| 条件 | 行为 |
|------|------|
| user 为 null | 调用方保证非 null；若为 null 抛 NPE |
| user.roles 为 null 或空 | response.role = `""` |
| user.posts 为 null 或空 | response.position = `""`；response.permissions 为空 Set |

### AuthModuleConfig

无错误处理逻辑，纯 Bean 声明。`@Bean` 方法均使用无参构造，不会失败。

## 行为契约

### JwtTokenProvider.init()

| 项 | 值 |
|------|------|
| 前置条件 | `jwtConfig.getSecret()` 返回非 null 且 Base64 解码后 ≥ 32 字节 |
| 后置条件 | `secretKey` 字段被初始化为有效的 `SecretKey` 实例 |
| 异常 | `IllegalArgumentException`（Base64 解码失败或密钥字节不足） |

### JwtTokenProvider.generateAccessToken()

| 项 | 值 |
|------|------|
| 前置条件 | userId、username、userType 均非 null |
| claims 内容 | sub=username, userId, userType, jti, iat, exp |
| type claim | **不包含** type claim |
| 有效期 | iat + 900s = exp |

### JwtTokenProvider.generateRefreshToken()

| 项 | 值 |
|------|------|
| 前置条件 | userId、username、userType 均非 null |
| claims 内容 | sub=username, userId, userType, type="refresh", tokenVersion, jti, iat, exp |
| 有效期 | iat + 604800s = exp |

### JwtTokenProvider.validateToken()

| 项 | 值 |
|------|------|
| 前置条件 | token 可为 null（返回 null） |
| expectedType=null | 不检查 type claim，仅验证签名和有效期 |
| expectedType非null | 验证签名+有效期+type claim 匹配；不匹配返回 null |
| 异常处理 | 捕获所有解析/验证异常，全部返回 null |

### JwtTokenProvider.getUserIdFromClaims()

| 项 | 值 |
|------|------|
| Integer 输入 | `((Integer) userId).longValue()` |
| Long 输入 | `(Long) userId` |
| 其他/null | 返回 null |

### UserConverter.toUserInfoResponse()

| 项 | 值 |
|------|------|
| 前置条件 | user 非 null |
| 后置条件 | 不修改 user 实体，纯函数式转换 |
| 角色排序 | 按 `Role.sort` 升序（自然顺序）；sort 值相等时任意选一个 |
| 权限聚合 | 遍历 posts→functions 及 roles→posts→functions，收集所有 `PermissionFunction.code` 去重 |

### AuthModuleConfig @Bean 方法

| 项 | 值 |
|------|------|
| 每次调用 | 返回新实例（非单例代理） |
| Bean 生命周期 | 由 Spring 容器管理单例 |
| 前置条件 | 无（无参构造） |

## 依赖关系

### 新建类型

| 类型 | 所在包 | 说明 |
|------|--------|------|
| `JwtTokenProvider` | `com.aimedical.modules.commonmodule.auth.jwt` | `@Component`，新建 |
| `UserConverter` | `com.aimedical.modules.commonmodule.auth.converter` | `@Component`，新建 |
| `AuthModuleConfig` | `com.aimedical.modules.commonmodule.auth.config` | `@Configuration`，新建 |
| `JwtTokenProviderTest` | `com.aimedical.modules.commonmodule.auth.jwt` | 8 用例，JUnit 5 |
| `UserConverterTest` | `com.aimedical.modules.commonmodule.auth.converter` | 5 用例，JUnit 5 |
| `AuthModuleConfigTest` | `com.aimedical.modules.commonmodule.auth.config` | 上下文验证 |

### 依赖的已有类型

| 类型 | 所在包 | 被谁依赖 | 说明 |
|------|--------|---------|------|
| `JwtConfig` | `com.aimedical.modules.commonmodule.jwt` | `JwtTokenProvider` | 构造器注入，提供 secret、expiration |
| `User` | `com.aimedical.modules.commonmodule.permission` | `UserConverter` | 输入实体，含 roles/posts 集合 |
| `Role` | `com.aimedical.modules.commonmodule.permission` | `UserConverter` | 含 sort 字段用于优先级排序 |
| `Post` | `com.aimedical.modules.commonmodule.permission` | `UserConverter` | 含 functions 集合用于权限收集 |
| `PermissionFunction` | `com.aimedical.modules.commonmodule.permission` | `UserConverter` | 含 code 字段用于权限列表 |
| `UserInfoResponse` | `com.aimedical.modules.commonmodule.auth` | `UserConverter` | 输出 DTO（record） |
| `RateLimitGuard` | `com.aimedical.modules.commonmodule.auth.rateLimit` | `AuthModuleConfig` | 接口，@Bean 返回类型 |
| `InMemoryRateLimitGuard` | `com.aimedical.modules.commonmodule.auth.rateLimit` | `AuthModuleConfig` | 具体实现 |
| `TokenBlacklist` | `com.aimedical.modules.commonmodule.auth.blacklist` | `AuthModuleConfig` | 接口，@Bean 返回类型 |
| `InMemoryTokenBlacklist` | `com.aimedical.modules.commonmodule.auth.blacklist` | `AuthModuleConfig` | 具体实现 |
| `LoginAttemptTracker` | `com.aimedical.modules.commonmodule.auth.login` | `AuthModuleConfig` | @Bean 返回类型 |

### 框架依赖

| 类型/依赖 | 用途 |
|-----------|------|
| `org.springframework.stereotype.Component` | JwtTokenProvider、UserConverter |
| `org.springframework.context.annotation.Configuration` | AuthModuleConfig |
| `org.springframework.context.annotation.Bean` | AuthModuleConfig @Bean 方法 |
| `jakarta.annotation.PostConstruct` | JwtTokenProvider.init() |
| `io.jsonwebtoken.Jwts` | JWT 构建与解析 |
| `io.jsonwebtoken.Claims` | JWT claims 体 |
| `io.jsonwebtoken.security.Keys` | SecretKey 构造 |
| `javax.crypto.SecretKey` | JWT 签名密钥类型 |
| `java.util.UUID` | jti 生成 |
| `java.util.Base64` | SecretKey Base64 解码 |
| `org.junit.jupiter.api.Test` | 测试注解 |

### 暴露给后续任务的公开接口

| 类型 | 后续使用者 |
|------|-----------|
| `JwtTokenProvider` | `AuthServiceImpl.login()`、`AuthServiceImpl.refreshToken()`、`AuthServiceImpl.logout()`、`JwtAuthenticationFilter` |
| `UserConverter` | `AuthServiceImpl.login()`（替换私有 `buildUserInfoResponse()`） |
| `AuthModuleConfig` | 自动注入到 `AuthServiceImpl` 等依赖方 |

## 单元测试设计

### JwtTokenProviderTest

**形态**：class（JUnit 5），纯单元测试，使用真实 JwtConfig（构造时注入测试用 secret）。

**包路径**：`com.aimedical.modules.commonmodule.auth.jwt`

**测试夹具**：
```java
class JwtTokenProviderTest {
    private JwtConfig jwtConfig;
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig();
        jwtConfig.setSecret("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="); // 44 chars base64 → 32 bytes
        jwtTokenProvider = new JwtTokenProvider(jwtConfig);
        jwtTokenProvider.init();
    }
}
```

**测试方法清单**（8 用例）：

| # | 测试方法 | Arrange | Assert |
|---|---------|---------|--------|
| 1 | `generateAccessToken_shouldCreateValidToken` | 调用 `generateAccessToken(1L, "testuser", "DOCTOR", "test-jti")` | token 非 null；解析后 sub="testuser", userId=1L, userType="DOCTOR", jti="test-jti"；exp - iat = 900s |
| 2 | `generateRefreshToken_shouldContainTypeAndVersion` | 调用 `generateRefreshToken(1L, "testuser", "DOCTOR", 0, "test-jti")` | claims 中 type="refresh", tokenVersion=0 |
| 3 | `validateToken_withCorrectType_shouldReturnClaims` | 生成 access token 后调用 `validateToken(token, null)` | 返回非 null Claims |
| 4 | `validateToken_withWrongType_shouldReturnNull` | 生成 refresh token，调用 `validateToken(token, "access")` | 返回 null |
| 5 | `validateToken_withExpiredToken_shouldReturnNull` | 构造极短过期时间 token（手动构建过期 claims），调用 `validateToken(token, null)` | 返回 null |
| 6 | `getUserIdFromClaims_withInteger_shouldReturnLong` | Claims 中 userId=Integer(1) | `getUserIdFromClaims(claims)` → 1L |
| 7 | `getUserIdFromClaims_withLong_shouldReturnLong` | Claims 中 userId=Long(1L) | `getUserIdFromClaims(claims)` → 1L |
| 8 | `getJtiFromToken_shouldReturnCorrectJti` | 生成 token 时传入特定 jti | `getJtiFromToken(token)` → 匹配传入值 |

**测试关键细节**：
- 用例 1：验证 exp - iat = 900s 确认有效期正确
- 用例 3：expectedType=null 跳过类型检查
- 用例 4：refresh token 的 type="refresh" 与 "access" 不匹配 → null
- 用例 5：可通过 io.jsonwebtoken.Jwts.builder() 手动设置已过期的 exp 构造测试 token
- 用例 6/7：可通过 `MockClaims` 或直接构造含 Integer/Long 的 claims map

### UserConverterTest

**形态**：class（JUnit 5），纯单元测试，手动构造 User 实体。

**包路径**：`com.aimedical.modules.commonmodule.auth.converter`

**测试夹具**：
```java
class UserConverterTest {
    private final UserConverter converter = new UserConverter();
}
```

**测试方法清单**（5 用例）：

| # | 测试方法 | Arrange | Assert |
|---|---------|---------|--------|
| 1 | `toUserInfoResponse_shouldMapBasicFields` | User: id=1L, username="test", nickname="Test", phone="13800138000", email="test@test.com", enabled=true；roles=null；posts=null | response.id=1L, .username="test", .realName="Test", .phone="13800138000", .email="test@test.com", .role="", .position="", .permissions=empty set |
| 2 | `toUserInfoResponse_shouldMapRoleBySortPriority` | User: roles=[Role(sort=2, code="admin"), Role(sort=1, code="doctor")] | response.role="doctor"（sort=1 更小） |
| 3 | `toUserInfoResponse_shouldMapRoleToEmptyWhenNoRoles` | User: roles=null | response.role="" |
| 4 | `toUserInfoResponse_shouldMapPositionFromFirstPost` | User: posts=[Post(code="OUTPATIENT"), Post(code="INPATIENT")] | response.position="OUTPATIENT" |
| 5 | `toUserInfoResponse_shouldCollectPermissions` | User: posts=[Post(functions=[PermissionFunction(code="menu:view"), PermissionFunction(code="user:create")])] | response.permissions.contains("menu:view", "user:create")；size=2 |

**测试关键细节**：
- 用例 1：基本字段映射，role/position/permissions 均应为默认值
- 用例 2：验证 Role.sort 升序排序取主角色
- 用例 4：取 posts 集合第一个元素（非排序逻辑，依赖 Set 顺序或固定构造）
- 用例 5：验证 posts→functions 聚合；可扩展测试 roles→posts→functions 聚合场景

### AuthModuleConfigTest

**形态**：class（JUnit 5），`@SpringBootTest` 或纯构造验证。

**包路径**：`com.aimedical.modules.commonmodule.auth.config`

**测试策略**：通过 `@SpringBootTest(classes = AuthModuleConfig.class)` 加载配置上下文，验证每个 @Bean 方法返回非 null 实例；或通过直接 new AuthModuleConfig() 验证。

**测试方法清单**（3 断言，可合并为一个方法）：

| # | 测试方法 | Arrange | Assert |
|---|---------|---------|--------|
| 1 | `rateLimitGuard_shouldReturnNonNullInstance` | 创建 AuthModuleConfig 实例 | `rateLimitGuard()` 返回非 null |
| 2 | `tokenBlacklist_shouldReturnNonNullInstance` | 同上 | `tokenBlacklist()` 返回非 null |
| 3 | `loginAttemptTracker_shouldReturnNonNullInstance` | 同上 | `loginAttemptTracker()` 返回非 null |

## 验证

测试命令：
```bash
mvn test -pl modules/common-module/common-module-impl -am -Dtest="JwtTokenProviderTest,UserConverterTest,AuthModuleConfigTest" -Dsurefire.failIfNoSpecifiedTests=false
```
