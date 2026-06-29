# 任务指令（v15）

## 动作
NEW

## 任务描述

在 `common-module-impl` 下新建 3 个基础类型（共 6 个文件，含测试），为 Stage 3 AuthServiceImpl 全量重组提供前置基础设施：

### 1. JwtTokenProvider （`auth/jwt/JwtTokenProvider.java`）

**形态**：`@Component` class
**包路径**：`com.aimedical.modules.commonmodule.auth.jwt`
**职责**：集中式 JWT 令牌提供者，封装 Access Token 和 Refresh Token 的全生命周期操作。OOD 3.9 / C3。

**公开接口**：

| 方法 | 返回 | 说明 |
|------|------|------|
| `String generateAccessToken(Long userId, String username, String userType, String jti)` | `String` | 生成 Access Token，claims 含 sub=username, userId, userType, jti, iat, exp；有效期 15 分钟（900s）；不含 type claim |
| `String generateRefreshToken(Long userId, String username, String userType, int tokenVersion, String jti)` | `String` | 生成 Refresh Token，claims 含 sub=username, userId, userType, type="refresh", tokenVersion, jti, iat, exp；有效期 7 天（604800s） |
| `Claims validateToken(String token, String expectedType)` | `Claims` | 验证签名+有效期，检查 type claim（expectedType 为 null 时跳过类型检查）；失败返回 null 不抛异常 |
| `Long getUserIdFromClaims(Claims claims)` | `Long` | 统一提取 userId（处理 Integer→Long 兼容），解决 C3 重复提取问题 |
| `String getJtiFromToken(String token)` | `String` | 从 token 中提取 jti claim |
| `int getTokenVersionFromClaims(Claims claims)` | `int` | 从 claims 中提取 tokenVersion |
| `long getAccessTokenExpirationMs()` | `long` | Access Token 有效期毫秒值 |

**构造方式**：通过 `@Component` + 构造器注入 `JwtConfig`，`@PostConstruct init()` 从 `JwtConfig.getSecret()` Base64 解码后构造 `SecretKey` 并缓存为字段。SecretKey 校验同 JwtUtil 现有逻辑（≥32 字节、Base64 URL-safe）。

**设计要点**：
- 使用 `UUID.randomUUID().toString()` 生成 jti，调用方可不传 jti（重载方法自动生成）
- 与 JwtUtil 共存（JwtUtil 保留向后兼容，JwtTokenProvider 是增强版本）
- 验证签名使用 `Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token)`
- Access Token 不含 type claim，Refresh Token 含 `type=refresh` 用于区分

### 2. UserConverter (`auth/converter/UserConverter.java`)

**形态**：`@Component` class
**包路径**：`com.aimedical.modules.commonmodule.auth.converter`
**职责**：User 实体 → UserInfoResponse DTO 的转换逻辑抽取（M1 修复），替代 AuthServiceImpl 中的私有 `buildUserInfoResponse()` 方法。OOD 3.5。

**公开接口**：

| 方法 | 返回 | 说明 |
|------|------|------|
| `UserInfoResponse toUserInfoResponse(User user)` | `UserInfoResponse` | 将 User 实体转换为 UserInfoResponse record |

**转换规则**：
- `id` → `user.getId()`
- `username` → `user.getUsername()`
- `realName` → `user.getNickname()`（OOD 5.2 字段映射）
- `phone` → `user.getPhone()`
- `email` → `user.getEmail()`
- `role` → 主角色 code。按 `Role.sort` 升序排序（值越小优先级越高）取第一个；用户无角色时返回空字符串 `""`
- `position` → 用户岗位 code。取 `user.getPosts()` 第一个非空元素的 code；无岗位时返回空字符串 `""`
- `permissions` → `Set<String>`，从所有 posts→functions 聚合 `function.getCode()`；同时从 roles→functions 聚合（角色也可能携带功能权限）

**注意**：OOD 5.2 规定 role 取主角色（按 sort 优先级），不再是 `user.getUserType().getCode()`。

**类型关系**：无接口，`@Component` 注入。

### 3. AuthModuleConfig (`auth/config/AuthModuleConfig.java`)

**形态**：`@Configuration` class
**包路径**：`com.aimedical.modules.commonmodule.auth.config`
**职责**：Bean 装配定义，提供给 `AuthServiceImpl` 等组件注入所需的 RateLimitGuard、TokenBlacklist 等。OOD 3.10。

**Bean 定义**：

| Bean 名称 | 类型 | 说明 |
|-----------|------|------|
| `rateLimitGuard` | `RateLimitGuard` | `new InMemoryRateLimitGuard()`内存实现 |
| `tokenBlacklist` | `TokenBlacklist` | `new InMemoryTokenBlacklist()`内存实现 |
| `loginAttemptTracker` | `LoginAttemptTracker` | `new LoginAttemptTracker()`默认构造 |

**构造方式**：`@Configuration` + `@Bean` 方法。

### 测试文件

#### JwtTokenProviderTest
- **包路径**：`com.aimedical.modules.commonmodule.auth.jwt`
- **形态**：JUnit 5 纯单元测试，使用真实 JwtConfig（构造时注入测试用 secret）
- **测试夹具**：创建 JwtConfig 设置 secret="AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="（44字符 base64，解码后32字节），`@BeforeEach` 显式调用 `jwtTokenProvider.init()`
- **测试用例**（8 个）：

| # | 方法 | Arrange | Assert |
|---|------|---------|--------|
| 1 | `generateAccessToken_shouldCreateValidToken` | 构造含 userId+username+userType+jti 的 access token | token 可解析，claims 中 sub/userId/userType/jti 正确，exp-iat=900s |
| 2 | `generateRefreshToken_shouldContainTypeAndVersion` | 构造含 tokenVersion=0 的 refresh token | claims 中 type="refresh"，tokenVersion=0 |
| 3 | `validateToken_withCorrectType_shouldReturnClaims` | 生成 access token 后调用 `validateToken(token, null)` | 返回非 null Claims |
| 4 | `validateToken_withWrongType_shouldReturnNull` | 生成 refresh token 后调用 `validateToken(token, null)` 验证不检查 type | 返回非 null Claims；但若传入 expectedType="access" 应返回 null |
| 5 | `validateToken_withExpiredToken_shouldReturnNull` | 构造极短过期时间的 token（exp 已过） | 返回 null |
| 6 | `getUserIdFromClaims_withInteger_shouldReturnLong` | Claims 中 userId 为 Integer(1) | 返回 Long(1L) |
| 7 | `getUserIdFromClaims_withLong_shouldReturnLong` | Claims 中 userId 为 Long(1L) | 返回 Long(1L) |
| 8 | `getJtiFromToken_shouldReturnCorrectJti` | 生成 token 时传入特定 jti | 提取的 jti 与传入一致 |

#### UserConverterTest
- **包路径**：`com.aimedical.modules.commonmodule.auth.converter`
- **形态**：JUnit 5 纯单元测试，手动构造 User 实体
- **测试用例**（5 个）：

| # | 方法 | Arrange | Assert |
|---|------|---------|--------|
| 1 | `toUserInfoResponse_shouldMapBasicFields` | User 设 id=1L, username="test", nickname="Test", phone="13800138000", email="test@test.com", enabled=true | response.id=1, .username="test", .realName="Test", .phone="13800138000", .email="test@test.com" |
| 2 | `toUserInfoResponse_shouldMapRoleBySortPriority` | User 设 roles=[Role(sort=2, code="admin"), Role(sort=1, code="doctor")] | response.role="doctor"（sort 值小者优先） |
| 3 | `toUserInfoResponse_shouldMapRoleToEmptyWhenNoRoles` | User 设 roles=null | response.role="" |
| 4 | `toUserInfoResponse_shouldMapPositionFromFirstPost` | User 设 posts=[Post(code="OUTPATIENT"), Post(code="INPATIENT")] | response.position="OUTPATIENT" |
| 5 | `toUserInfoResponse_shouldCollectPermissions` | User 设 posts=[Post(functions=[PermissionFunction(code="menu:view"), PermissionFunction(code="user:create")])] | response.permissions.contains("menu:view", "user:create") |

#### AuthModuleConfigTest
- **简单验证**：确认 `@Configuration` 加载上下文时各 @Bean 方法返回非 null 实例（非必须，可用 `@SpringBootTest` 或纯构造验证）

## 选择理由

- 是 Stage 3 剩余工作的基础设施层，AuthServiceImpl 全量重组前必须先就位
- 三者均为新建文件，不修改已有生产代码，风险最低
- 对应 OOD 第 12 节任务 3.5（UserConverter）、3.9（JwtTokenProvider）、3.10（AuthModuleConfig）
- 解决审查问题 M1（私有方法不可测试 → 抽出 UserConverter）、C3（userId 提取重复 4 次 → JwtTokenProvider 统一提取）
- 粒度合理：3 个生产文件 + 3 个测试文件，功能独立

## 任务上下文

### 来自 OOD 的需求摘要

**JwtTokenProvider（OOD 3.9 / C3）**：
- `@Component` 提供集中式 JWT 操作（生成、解析、验证）
- SecretKey `@PostConstruct` 缓存
- Claims 结构见 OOD 3.2：
  - Access Token: sub, userId, userType, iat, exp, jti（无 type claim）
  - Refresh Token: sub, userId, type="refresh", tokenVersion, iat, exp, jti
- getUserIdFromClaims(Claims) 统一处理 Integer→Long 兼容

**UserConverter（OOD 3.5 / M1）**：
- 从 AuthServiceImpl 私有方法 `buildUserInfoResponse` 抽出为独立 @Component
- UserInfoResponse 字段映射：
  - realName = User.nickname（OOD 5.2 第 743 行）
  - role = 主角色 code，按 Role.sort 升序取第一个；无角色返回空字符串（OOD 5.2 第 744 行）
  - position = 岗位 code，取 posts 第一个；无岗位返回空字符串
  - permissions = posts→functions + roles→functions 聚合

**AuthModuleConfig（OOD 3.10）**：
- Bean 装配：RateLimitGuard（InMemoryRateLimitGuard）、TokenBlacklist（InMemoryTokenBlacklist）
- LoginAttemptTracker（默认构造参数）
- 位置：`auth/config/AuthModuleConfig.java`

### 已有代码上下文

| 类型 | 位置 | 与本任务关系 |
|------|------|-------------|
| `JwtConfig` | `common-module-impl/.../jwt/JwtConfig.java` | JwtTokenProvider 注入此配置获取 secret/expiration |
| `JwtUtil` | `common-module-impl/.../jwt/JwtUtil.java` | 已有工具类，JwtTokenProvider 是增强版本（两者共存） |
| `User` | `common-module-impl/.../permission/User.java` | UserConverter 的输入实体，含 roles/posts 集合 |
| `Role` | `common-module-impl/.../permission/Role.java` | 含 sort 字段用于优先级排序 |
| `Post` | `common-module-impl/.../permission/Post.java` | 含 functions 集合用于权限收集 |
| `PermissionFunction` | `common-module-impl/.../permission/PermissionFunction.java` | 含 code 字段用于权限列表 |
| `UserInfoResponse` | `common-module-api/.../auth/UserInfoResponse.java` | UserConverter 的输出 DTO（record） |
| `RateLimitGuard` | `common-module-impl/.../auth/rateLimit/RateLimitGuard.java` | AuthModuleConfig 装配此接口 |
| `InMemoryRateLimitGuard` | `common-module-impl/.../auth/rateLimit/InMemoryRateLimitGuard.java` | 具体实现 |
| `TokenBlacklist` | `common-module-impl/.../auth/blacklist/TokenBlacklist.java` | AuthModuleConfig 装配此接口 |
| `InMemoryTokenBlacklist` | `common-module-impl/.../auth/blacklist/InMemoryTokenBlacklist.java` | 具体实现 |
| `LoginAttemptTracker` | `common-module-impl/.../auth/login/LoginAttemptTracker.java` | AuthModuleConfig 装配此 bean |
| `AuthServiceImpl.buildUserInfoResponse()` | `common-module-impl/.../service/impl/AuthServiceImpl.java` | 私有方法，R16 时将替换为 UserConverter 调用 |

## 验证

测试命令：
```bash
mvn test -pl modules/common-module/common-module-impl -am -Dtest="JwtTokenProviderTest,UserConverterTest" -Dsurefire.failIfNoSpecifiedTests=false
```

## 修订说明（v15 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [一般] 计划上下文遗漏 JwtTokenProvider 的两个公开方法（getTokenVersionFromClaims、getAccessTokenExpirationMs） | 在 plan.md 第 154 行 JwtTokenProvider 封装方法列表中追加这两个方法 |
| [一般] 新建文件清单未明确列出测试文件名（仅写"对应的 3 个测试文件"） | 在 plan.md 中展开列出全部 6 个文件路径（含 3 个测试文件完整路径） |
| [轻微] 计划将 UserConverter 描述为"接口"与任务定义不符 | 在 plan.md 中将"UserConverter 接口"改为"UserConverter 转换器" |
