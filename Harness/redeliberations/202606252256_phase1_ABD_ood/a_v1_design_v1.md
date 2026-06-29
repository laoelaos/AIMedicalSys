# Phase 1 包 A/B/D 统一修复与包 B OOD 设计方案

## 1. 概述

### 1.1 设计目标

本设计面向 Phase 1 三个耦合包（包 A 数据实体、包 B 统一认证模块、包 D 前端登录与菜单）的缺陷修复与架构补全。核心目标如下：

- **补全包 B 设计契约**：包 B（统一认证模块）已有骨架代码但缺乏独立 OOD 文档，需从架构层面定义认证流程、令牌体系、安全策略、模块边界，使后续开发、审查和维护有契约可依
- **修复已发现的设计缺陷**：基于审查报告中的 19+4 项问题（含后端 B 包、前端 D 包、包 A 数据建模），在 OOD 层面逐一给出修复方案，消除安全漏洞和设计反模式
- **明确 A/B/D 协作边界**：包 A（数据实体）→ 包 B（认证服务）→ 包 D（前端消费）的依赖链在架构层面显式定义，减少跨包耦合风险

### 1.2 整体架构思路

包 B 作为统一认证模块，定位为 **common-module 的子模块**，归属 `common-module-impl` 实现层。采用**无状态 JWT 为主、辅助服务端状态管理为辅**的混合策略：

- **主要认证机制**：JWT Bearer Token，无状态认证，避免 Session 管理复杂度
- **辅助状态**：引入轻量级内存/Redis 令牌黑名单（Phase 1 内存方案，Phase 2 迁移 Redis），解决登出和刷新场景的状态需求
- **防暴力破解**：基于 IP 的速率限制 + 登录失败计数 + 账户临时锁定三层防护
- **角色/权限动态刷新**：关键操作（访问受保护资源、刷新 token）强制查库验证用户状态

### 1.3 核心抽象一览

| 抽象 | 类型形态 | 职责定位 |
|------|---------|---------|
| `AuthService` | interface | 统一认证服务的业务契约，定义登录、登出、刷新、获取当前用户、更新资料的边界 |
| `JwtTokenProvider` | class | JWT 令牌生成、解析、验证的集中提供者；隐含 SecretKey 缓存和 Claims 提取工具方法 |
| `JwtAuthenticationFilter` | class | OncePerRequestFilter 实现，拦截已认证请求，提取并验证 Token，装配 SecurityContext |
| `RateLimitGuard` | interface | 速率限制策略契约，支持 IP 级别和用户级别的限流判定 |
| `LoginAttemptTracker` | class | 登录失败计数与账户临时锁定逻辑的封装 |
| `TokenBlacklist` | interface | Token 失效状态查询的抽象，登出后标记 token 为不可用 |
| `PasswordPolicy` | interface | 密码复杂度策略契约，定义校验方法 |
| `SecurityConfigPhase1` | class | Spring Security 配置聚合点：SecurityFilterChain、密码编码器、CORS、异常处理入口 |
| `CurrentUser` | interface | 当前登录用户的轻量级类型化访问器，消除 Controller 层对 SecurityContext 的直接操作 |

## 2. 模块划分

### 2.1 目录结构

```
backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/
├── auth/
│   ├── controller/
│   │   └── AuthController.java              # 认证 REST 端点
│   ├── service/
│   │   ├── AuthService.java                 # 认证服务接口
│   │   └── impl/
│   │       └── AuthServiceImpl.java          # 认证服务实现
│   ├── jwt/
│   │   ├── JwtConfig.java                   # JWT 配置属性绑定
│   │   └── JwtTokenProvider.java            # Token 生命周期管理
│   ├── security/
│   │   ├── CurrentUser.java                 # 当前用户访问器（interface）
│   │   ├── CurrentUserImpl.java             # 基于 SecurityContextHolder 的实现
│   │   ├── JwtAuthenticationFilter.java      # JWT 鉴权过滤器
│   │   └── SecurityConfigPhase1.java        # Security 配置
│   ├── rateLimit/
│   │   ├── RateLimitGuard.java              # 限流策略接口
│   │   └── InMemoryRateLimitGuard.java      # 内存限流实现（Phase 1）
│   ├── login/
│   │   ├── LoginAttemptTracker.java         # 登录失败计数 + 锁定
│   │   └── LoginAttemptCleaner.java         # 定时清理过期计数
│   ├── blacklist/
│   │   ├── TokenBlacklist.java              # Token 黑名单接口
│   │   └── InMemoryTokenBlacklist.java      # 内存黑名单实现（Phase 1）
│   ├── password/
│   │   └── PasswordPolicy.java              # 密码复杂度策略
│   ├── dto/
│   │   ├── request/
│   │   │   ├── LoginRequest.java            # 登录请求
│   │   │   ├── RefreshTokenRequest.java     # 刷新请求（Phase 2 扩展）
│   │   │   └── ProfileUpdateRequest.java    # 资料更新请求
│   │   └── response/
│   │       ├── LoginResponse.java           # 登录响应（含 access + refresh token）
│   │       ├── UserInfoResponse.java        # 当前用户信息
│   │       └── TokenRefreshResponse.java    # 刷新响应
│   └── converter/
│       └── UserConverter.java              # User ↔ DTO 转换
├── menu/
│   ├── controller/
│   │   └── MenuController.java
│   ├── service/
│   │   ├── MenuService.java
│   │   └── impl/
│   │       └── MenuServiceImpl.java
│   ├── dto/
│   │   └── MenuResponse.java               # 菜单响应 DTO
│   └── converter/
│       └── MenuConverter.java              # Function → MenuResponse 转换
├── permission/
│   ├── User.java                            # 用户实体（包 A）
│   ├── Role.java                            # 角色实体（包 A）
│   ├── Post.java                            # 岗位实体（包 A）
│   ├── PermissionFunction.java              # 功能实体（包 A，原名 Function，修复类名冲突）
│   ├── UserRepository.java
│   ├── RoleRepository.java
│   └── PostRepository.java
├── config/
│   └── AuthModuleConfig.java                # Bean 装配（RateLimitGuard、TokenBlacklist 等）
```

### 2.2 模块依赖方向

```
application/
  └── common-module-impl/ ───────────────> common
                                              └── common-module-api (UserType, PositionEnum)

application/ ─────────────────────> common-module-impl
                                          ├── auth/ ────────> permission/ (User, Role, Post)
                                          ├── menu/ ────────> permission/
                                          └── security/ ───> jwt/

modules/patient, doctor, admin/ ──> common-module-api (仅 UserType, PositionEnum 等共享类型)
                                     [不依赖 common-module-impl]
```

**依赖规则**：
- 包 B 的认证代码全部归属 `common-module-impl` 内部 `auth/` 包，对业务模块（patient/doctor/admin）完全不可见
- 业务模块若需获取当前用户身份，通过 `common-module-api` 中的门面接口（Phase 1 新增）获取，不直接接触认证实现
- `JwtAuthenticationFilter` 放置在 `common-module-impl/auth/security/` 下而非 `application` 模块，因其与 JwtTokenProvider 有强内聚关系；application 模块通过 `@ComponentScan` 或 `@Import` 引入该 Filter 并注册到 SecurityFilterChain
- `TokenBlacklist` 和 `RateLimitGuard` 在 Phase 1 使用内存实现（`ConcurrentHashMap`），不引入外部存储依赖。Phase 2 迁移到 Redis 时，增加 Redis 实现类并注入即可，接口不变

## 3. 核心设计

### 3.1 认证流程

#### 3.1.1 登录流程

1. 前端提交 `{ username, password }` 到 `POST /api/auth/login`
2. RateLimitGuard 检查请求来源 IP 是否触发限流（同 IP 每 10 秒最多 5 次尝试）
3. LoginAttemptTracker 检查该用户名是否处于临时锁定状态（连续失败 5 次后锁定 15 分钟）
4. UserRepository.findByUsername() 以 `Optional<User>` 形式加载用户
5. `Optional` 为空 → 递增 LoginAttemptTracker 失败计数 → 抛出 BusinessException（统一消息"用户名或密码错误"）
6. 用户 `enabled == false` 或 `deleted == true` → 抛出 BusinessException（"账户已停用"）
7. `passwordEncoder.matches(password, user.password)` 失败 → 递增 LoginAttemptTracker 失败计数 → 抛出 BusinessException
8. 成功 → LoginAttemptTracker 清除该用户名的失败计数
9. JwtTokenProvider 生成 Access Token（短有效期 15 分钟）和 Refresh Token（长有效期 7 天，携带唯一 jti 标识）
10. 返回 LoginResponse（含 accessToken、refreshToken、expiresIn、user 基本信息）

#### 3.1.2 已认证请求流程

1. JwtAuthenticationFilter 从 `Authorization: Bearer <token>` 提取 token
2. JwtTokenProvider.validateToken(token) 解析并验证签名、有效期
3. TokenBlacklist.isBlacklisted(token) 检查是否已被登出标记
4. 从 DB 加载用户完整信息：检查 `enabled == true` 且 `deleted == false`
5. 装配 `UsernamePasswordAuthenticationToken`（principal = 用户 ID，authorities = 从 DB 读取的角色+功能权限）
6. 设置 `SecurityContextHolder`
7. `chain.doFilter()` 放行

**关键设计决策**：步骤 4 查库验证 enabled 状态是必须的。无状态 JWT 无法撤销，若仅依赖 token 内的 claims 判断用户状态，禁用用户后旧 token 仍可使用。此查库操作仅发生在 Filter 层，对已认证的后续请求不重复查库。

#### 3.1.3 Token 刷新流程

1. 前端提交 Refresh Token 到 `POST /api/auth/refresh`
2. JwtTokenProvider 验证 Refresh Token 的有效性（签名、有效期、jti 是否在黑名单中）
3. TokenBlacklist 将当前 Refresh Token 的 jti 加入黑名单（轮换：旧 token 立即失效）
4. 从 DB 重新加载用户最新状态（enabled、roles、posts）
5. 生成新的 Access Token（15 分钟）和 Refresh Token（7 天，新 jti）
6. 返回 TokenRefreshResponse

**刷新时机**：前端 axios 响应拦截器监测到 401 时，自动触发静默刷新。刷新成功后重放原始请求，失败则清除认证数据并跳转登录页。

#### 3.1.4 登出流程

1. 前端提交当前 Access Token 到 `POST /api/auth/logout`
2. 后端将 Access Token（或其 jti）加入 TokenBlacklist
3. 若请求同时携带 Refresh Token，也将 Refresh Token 加入黑名单
4. 返回成功响应
5. 前端 finally 块清除 localStorage 中的 token 和 user 信息

#### 3.1.5 获取当前用户

1. `GET /api/auth/me` 从 SecurityContext 获取当前用户 ID
2. 查库加载用户完整信息（含角色、岗位、权限列表）
3. 返回 UserInfoResponse

### 3.2 JWT 令牌设计

#### Claims 结构

```
Access Token:
{
  "sub": "username",
  "userId": 1,                    // 用户 ID（Long）
  "userType": "DOCTOR",          // 用户类型枚举
  "iat": 1718000000,
  "exp": 1718000900,
  "jti": "uuid-access-xxx"      // 唯一标识，用于黑名单
}

Refresh Token:
{
  "sub": "username",
  "userId": 1,
  "type": "refresh",             // 明确区分 token 类型
  "iat": 1718000000,
  "exp": 1718604800,
  "jti": "uuid-refresh-yyy"     // 唯一标识，用于轮换+黑名单
}
```

**设计要点**：
- Access Token 中不包含 role/position/authorities claims，避免角色变更后 claims 与 DB 不一致。当前用户权限始终从 DB 读取，以 SecurityContext 中的 authorities 为准
- Refresh Token 通过 `type` claim 与 Access Token 区分，JwtTokenProvider 验证时拒绝用 Refresh Token 作为 Access Token 使用
- `jti`（JWT ID）为标准 claim，作为黑名单和轮换的索引键
- 签名算法：HMAC-SHA256（HS256），SecretKey 从 `JWT_SECRET` 环境变量派生

#### 过期策略

| Token 类型 | 有效期 | 刷新窗口 | 说明 |
|-----------|--------|---------|------|
| Access Token | 15 分钟 | 无 | 短有效期减小泄露窗口；到期前通过 Refresh Token 续期 |
| Refresh Token | 7 天 | 过期前 1 天开始提示续期 | 浏览器会话持久化；支持同时多设备登录 |

### 3.3 Spring Security 配置

#### SecurityFilterChain

```
SecurityConfigPhase1 (Phase 1 配置)
  @Profile("phase1")
  @EnableMethodSecurity
  @Configuration

  SecurityFilterChain:
    ├── csrf.disable()                     // Bearer Token 模式，CSRF 天然防御
    ├── sessionManagement.sessionCreationPolicy(STATELESS)
    ├── cors(Customizer.withDefaults())
    ├── exceptionHandling
    │   ├── authenticationEntryPoint       // 返回统一 Result.fail("UNAUTHORIZED")
    │   └── accessDeniedHandler            // 返回统一 Result.fail("FORBIDDEN")
    ├── authorizeHttpRequests
    │   ├── /api/auth/login           → permitAll
    │   ├── /api/auth/refresh         → permitAll
    │   ├── /api/auth/logout          → authenticated
    │   ├── /api/auth/**              → authenticated
    │   ├── /api/menu/**              → authenticated
    │   ├── /actuator/health          → permitAll
    │   ├── /actuator/info            → permitAll
    │   ├── /actuator/**              → denyAll (Phase 1)
    │   ├── /swagger-ui/**            → denyAll (Phase 1 → 生产禁用)
    │   ├── /v3/api-docs/**           → denyAll (Phase 1 → 生产禁用)
    │   ├── /error                    → permitAll
    │   └── /**                       → authenticated
    └── addFilterBefore(JwtAuthenticationFilter, UsernamePasswordAuthenticationFilter)

  PasswordEncoder:
    └── @Bean BCryptPasswordEncoder (Strength 10)
```

**设计要点**：
- 两阶段 `SecurityConfig` 通过 `@Profile("phase0")` 和 `@Profile("phase1")` 隔离。Phase 0 的 SecurityConfig 放行所有请求（`permitAll`），仅作为骨架占位；Phase 1 的 SecurityConfig 激活 JWT 认证。`application.yml` 中 profiles 列表在 Phase 1 应移除 `phase0`，确保两个 `SecurityFilterChain` bean 不会冲突
- `AuthenticationEntryPoint` 统一返回 `Result.fail("UNAUTHORIZED", "未认证")`，确保 JWT 过期、无效、缺失时响应体结构与业务异常一致
- `@EnableMethodSecurity` 启用，为后续 `@PreAuthorize` 方法级权限控制预留

#### JwtAuthenticationFilter 行为契约

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
     a. 用户不存在 / enabled=false / deleted=true → 清除 SecurityContext → chain.doFilter 放行
  5. 收集用户权限（从 roles + posts → functions 派生 authority 列表）
  6. 装配 UsernamePasswordAuthenticationToken(principal=userId, credentials=null, authorities=权限列表)
  7. SecurityContextHolder.getContext().setAuthentication(authentication)
  8. chain.doFilter(request, response)
```

**静默跳过策略**：Filter 在 token 无效时不直接返回 401，而是跳过（不设 SecurityContext），由 Spring Security 的 `ExceptionTranslationFilter` 或目标 Controller 的 `@PreAuthorize` 触发认证异常。此策略确保非受保护 API（如 `/api/auth/refresh`）即使在 Filter 层 token 无效也能继续处理。

### 3.4 用户状态管理

#### Enabled 禁用检查

每次请求的 Filter 层从 DB 加载用户完整状态（而非仅依赖 token claims）。这是禁用用户的即时生效机制：管理员禁用用户后，该用户的下一次请求在 Filter 层即被拒绝。

检查链：
1. JwtAuthenticationFilter 从 JWT 提取 userId
2. UserRepository.findById(userId) 加载用户
3. 检查 `user.enabled == true && user.deleted == false`
4. 任一条件不满足 → 清除 SecurityContext → 401 响应

#### 角色/权限动态刷新

每次请求从 DB 重新加载用户的角色和权限列表并装配到 SecurityContext 中。这意味着：
- 管理员修改用户角色/岗位后，该用户的下一次请求即反映变更
- 无需依赖 JWT claims 中的角色数据（JWT 仅用于身份标识和防篡改）
- 代价是每次请求多一次 DB 查询，通常在 1-5ms 内，可接受

#### Refresh Token 轮换策略

每次使用 Refresh Token 时：
1. 验证 Refresh Token 有效
2. 将旧 Refresh Token 的 jti 加入黑名单
3. 签发新的 Access Token + Refresh Token（含全新 jti）
4. 客户端收到新 token 后替换本地存储

若攻击者窃取了 Refresh Token 并在合法用户之前使用，合法用户的后续刷新请求将因旧 jti 在黑名单中而失败，合法用户被迫重新登录。此设计符合 RFC 6749 的 Refresh Token Rotation 最佳实践。

## 4. 安全设计

### 4.1 防暴力破解方案

采用三层防护：

**第一层：IP 级速率限制（RateLimitGuard）**

| 维度 | 阈值 | 窗口 | 超额处理 |
|------|------|------|---------|
| 同一 IP（/api/auth/login） | 5 次 | 10 秒滑动窗口 | 429 Too Many Requests，Result.code="RATE_LIMITED" |
| 同一 IP（任意 API 路径） | 100 次 | 60 秒 | 429 Too Many Requests |

实现方式：`InMemoryRateLimitGuard` 使用 `ConcurrentHashMap<String, SlidingWindowCounter>`，键为 IP 地址，值为滑动窗口计数器。滑动窗口以 `LinkedHashMap` 或 `TreeMap` 存储时间戳有序集合，窗口过期条目惰性清除。生产环境 Phase 2 迁移到 Redis + Lua 脚本。

**第二层：登录失败计数（LoginAttemptTracker）**

| 维度 | 阈值 | 锁定时间 | 重置时机 |
|------|------|---------|---------|
| 同一用户名 | 连续 5 次失败 | 15 分钟 | 锁定到期/登录成功 |
| 同一 IP | 连续 20 次失败 | 30 分钟 | 锁定到期 |

实现方式：`LoginAttemptTracker` 使用 `ConcurrentHashMap<String, AttemptRecord>`，AttemptRecord 记录失败次数和首次失败时间戳。超过锁定时间后惰性清除。

**第三层：临时账户锁定**

当 LoginAttemptTracker 触发锁定后，`AuthServiceImpl.login()` 在第一步即检查锁定状态，被锁定的账户即使提供正确密码也无法登录。这是防止凭据暴力猜测的最后防线。

### 4.2 Token 黑名单 / 轮换设计

#### 黑名单数据结构

```
InMemoryTokenBlacklist:
  ConcurrentHashMap<String, Long>  // key=jti, value=expirationTime (epoch ms)
  ScheduledExecutorService         // 每 5 分钟清理过期条目
```

- 登出时：将当前 Access Token 和 Refresh Token 的 jti 加入黑名单
- 刷新时：将旧 Refresh Token 的 jti 加入黑名单
- 验证时：JwtAuthenticationFilter 和 AuthServiceImpl 中的相关方法在验证 token 有效后，检查其 jti 是否在黑名单中
- 黑名单条目不永不过期，以 token 原始过期时间为 TTL，通过定时任务回收内存

#### 黑名单大小估算

假设每秒 100 次请求，Access Token 15 分钟过期，Refresh Token 7 天过期：
- Access Token 黑名单峰值：100 × 900 = 90,000 条
- Refresh Token 黑名单峰值：100 × 604800 = 60,480,000 条（Phase 1 可接受，Phase 2 需 Redis）

Phase 1 内存方案适用于单实例和小规模部署。多实例部署或高并发场景 Phase 2 必须迁移 Redis。

### 4.3 密码策略

#### 复杂度要求

密码校验规则（由 `PasswordPolicy` 接口封装，`AuthServiceImpl` 在注册/修改密码时调用）：

| 规则 | 要求 | 错误码 |
|------|------|--------|
| 最小长度 | 8 字符 | PASSWORD_TOO_SHORT |
| 最大长度 | 64 字符 | PASSWORD_TOO_LONG |
| 字符种类 | 至少包含大写字母、小写字母、数字、特殊字符中的 **3 种** | PASSWORD_WEAK |
| 用户名包含 | 密码不得包含用户名（大小写不敏感） | PASSWORD_CONTAINS_USERNAME |
| 常见密码 | 不得为 Top 10000 常见密码（Phase 2 实现） | PASSWORD_COMMON |

#### 加密

使用 BCryptPasswordEncoder（Strength 10），Spring Security 标准实现。

#### NOT NULL 约束

`User.java` 中 `password` 字段标注 `@Column(nullable = false)`，与 `schema.sql` 中对应列的 `NOT NULL` 保持一致。

### 4.4 API 端点保护清单

| 端点 | HTTP | 认证要求 | 限流 | 其他 |
|------|------|---------|------|------|
| `/api/auth/login` | POST | 匿名 | IP 5次/10秒 + 用户 5次/15分钟 | — |
| `/api/auth/logout` | POST | JWT | 无 | 黑名单记录 |
| `/api/auth/refresh` | POST | JWT | 无 | 黑名单+轮换 |
| `/api/auth/me` | GET | JWT | 无 | 查库验证 enabled |
| `/api/auth/profile` | PUT | JWT | 无 | — |
| `/api/menu/tree` | GET | JWT | 无 | — |
| `/api/menu/**` | GET/POST/PUT/DELETE | JWT | 无 | ADMIN 角色权限 |
| `/actuator/health` | GET | 匿名 | 无 | — |
| `/actuator/info` | GET | 匿名 | 无 | — |
| `/actuator/**` | GET | 拒绝 | — | Phase 1 禁用非 health/info 端点 |
| `/swagger-ui/**` | GET | 拒绝 | — | Phase 1 生产环境禁用 |
| `/v3/api-docs/**` | GET | 拒绝 | — | Phase 1 生产环境禁用 |

### 4.5 CSRF 策略

Phase 1 延续 `csrf.disable()`。JWT Bearer Token 通过 `Authorization` header（非 cookie）传递，天然不受 CSRF 攻击影响。但在代码中显式注释说明：Phase 2 若迁移到 httpOnly cookie 方案，必须在 SecurityConfig 中启用 CSRF 保护（`csrf(Customizer.withDefaults())`）。

## 5. 数据模型

### 5.1 实体变更（包 A 修复）

#### User.java

| 变更 | 当前状态 | 修复方案 |
|------|---------|---------|
| password NOT NULL | `@Column(nullable = false)` 缺失 | 添加 `@Column(nullable = false)` + `schema.sql` 对应列改为 `NOT NULL` |
| enabled 默认值 | `private Boolean enabled;` | `private Boolean enabled = true;` |
| 继承 BaseEntity | — | deleted 列已由 BaseEntity 提供 `@Column(nullable = false)` + `deleted = false`，schema.sql 需对齐 |

#### Role.java

| 变更 | 当前状态 | 修复方案 |
|------|---------|---------|
| enabled 默认值 | `private Boolean enabled;` | `private Boolean enabled = true;` |

#### Post.java

| 变更 | 当前状态 | 修复方案 |
|------|---------|---------|
| enabled 默认值 | `private Boolean enabled;` | `private Boolean enabled = true;` |

#### Function.java → PermissionFunction.java

| 变更 | 当前状态 | 修复方案 |
|------|---------|---------|
| 类名冲突 | `Function` 与 `java.util.function.Function` 冲突 | 重命名为 `PermissionFunction`；所有引用处同步更新（关联表名 `sys_function` 不变，仅 Java 类名变更） |
| enabled 默认值 | `private Boolean enabled;` | `private Boolean enabled = true;` |
| visible 默认值 | `private Boolean visible;` | `private Boolean visible = true;` |

#### UserRepository.java

| 变更 | 当前状态 | 修复方案 |
|------|---------|---------|
| 返回类型 | `User findByUsername(String username)` | `Optional<User> findByUsername(String username)` |

### 5.2 新增实体与 DTO（包 B）

本阶段不新增 JPA 实体。认证所需的状态管理（黑名单、登录计数）使用内存数据结构，不持久化到数据库。Phase 2 引入 `sys_token_blacklist` 表和 Redis 时再新增实体。

#### LoginRequest (Java 17 record)

```java
public record LoginRequest(
    @NotBlank String username,
    @NotBlank @Size(min = 1, max = 64) String password
) {}
```

#### LoginResponse (Java 17 record)

```java
public record LoginResponse(
    String accessToken,
    String refreshToken,
    String tokenType,       // "Bearer"
    long expiresIn,         // access token 过期秒数
    UserInfoResponse user
) {}
```

#### TokenRefreshResponse (Java 17 record)

```java
public record TokenRefreshResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn
) {}
```

#### UserInfoResponse (Java 17 record)

```java
public record UserInfoResponse(
    Long id,
    String username,
    String nickname,
    String phone,
    String email,
    String userType,
    String position,
    Set<String> permissions
) {}
```

#### ProfileUpdateRequest (Java 17 record)

```java
public record ProfileUpdateRequest(
    @Size(max = 50) String nickname,
    @Pattern(regexp = "^1[3-9]\\d{9}$") String phone,  // 手机号格式校验
    @Email @Size(max = 100) String email
) {}
```

## 6. API 接口设计

### 6.1 接口清单

| 方法 | 路径 | 请求体/参数 | 成功响应码 | 说明 |
|------|------|------------|-----------|------|
| POST | `/api/auth/login` | LoginRequest | 200 | 登录，返回 access + refresh token |
| POST | `/api/auth/logout` | — | 200 | 登出，将 token 加入黑名单 |
| POST | `/api/auth/refresh` | — | 200 | 刷新 token，旧 refresh token 轮换失效 |
| GET | `/api/auth/me` | — | 200 | 获取当前用户信息（含权限列表） |
| PUT | `/api/auth/profile` | ProfileUpdateRequest | 200 | 更新个人资料（nickname/phone/email） |
| PUT | `/api/auth/password` | PasswordChangeRequest | 200 | 修改密码（需旧密码验证） |
| GET | `/api/menu/tree` | — | 200 | 获取当前用户菜单树 |
| GET | `/api/menu/all` | — | 200 | 获取所有菜单（ADMIN） |
| POST | `/api/menu` | MenuCreateRequest | 200 | 创建菜单（ADMIN） |
| PUT | `/api/menu/{id}` | MenuUpdateRequest | 200 | 更新菜单（ADMIN） |
| DELETE | `/api/menu/{id}` | — | 200 | 删除菜单（ADMIN） |

### 6.2 登录响应格式

```json
{
  "code": "SUCCESS",
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "id": 1,
      "username": "doctor001",
      "nickname": "张医生",
      "phone": "13800138000",
      "email": "doctor@example.com",
      "userType": "DOCTOR",
      "position": "OUTPATIENT",
      "permissions": ["registration:view", "prescription:create"]
    }
  }
}
```

### 6.3 错误响应格式（统一）

```json
{
  "code": "UNAUTHORIZED",
  "message": "未认证或令牌已失效",
  "data": null
}
```

所有 401（未认证）、403（无权限）、429（限流）等错误均使用此统一格式，由 `GlobalExceptionHandler` 和 `AuthenticationEntryPoint` 共同保证响应结构一致。

## 7. 包 A/B/D 协作关系

### 7.1 包 A → 包 B 依赖

包 A 提供的数据实体（User、Role、Post、PermissionFunction）是包 B 认证功能的基石：

```
包 A 实体层 (permission/)               包 B 认证层 (auth/)
  User.java ─────────────────────────> AuthServiceImpl.login()
    ├── username（登录识别）                  ├── 查询用户 → 校验密码
    ├── password（BCrypt 加密存储）            └── 校验 enabled/deleted 状态
    ├── enabled（账户启用状态）
    └── deleted（软删除标记）

  User.java.roles (Set<Role>) ──────> JwtAuthenticationFilter
  User.java.posts (Set<Post>) ──────>   ├── 从 DB 加载角色和岗位
  Role.java ─────────────────────────>   ├── 聚合 Function 权限码
  Post.java.functions ──────────────>   └── 装配 SecurityContext authorities
  PermissionFunction.java ──────────>

  UserRepository ──────────────────> AuthServiceImpl, JwtAuthenticationFilter, MenuServiceImpl
    ├── findByUsername() ────────────── 登录查询
    ├── findById() ──────────────────── 每次请求查库验证 enabled
    └── 级联查询 ────────────────────── 权限收集
```

**依赖规则**：包 B 作为 common-module-impl 的内部模块，直接使用同模块 `permission/` 包下的实体和 Repository。这是同模块内部依赖，不构成跨模块耦合。

### 7.2 包 B → 包 D 契约

包 B 的后端 API 是包 D 前端消费的数据来源。前后端契约在以下方面对齐：

| 契约项 | 后端（包 B） | 前端（包 D） |
|--------|------------|------------|
| 认证头格式 | `Authorization: Bearer <token>` | axios 拦截器自动设置 |
| Token 存储 | 不存储（无状态） | localStorage（Phase 1） |
| 401 响应格式 | `{ code: "UNAUTHORIZED", ... }` | axios 拦截器识别 401 → 静默刷新 |
| 刷新触发时机 | 后端验证 token 过期返回 401 | 前端 401 拦截器 → refreshToken → 重放请求 |
| 响应体结构 | `Result<T>` 泛型 | `apiGet<T>` / `apiPost<T>` 泛型封装 |
| 菜单树结构 | `List<MenuResponse>`（递归 children） | `convertMenusToRoutes`（递归版本） |
| 用户信息 | `UserInfoResponse` | `UserInfo` TypeScript interface |

**包 B 对包 D 前端的向下兼容承诺**：
- 新增 API 端点不修改现有端点语义
- 响应体中现有字段不删除、不改变类型
- 错误码枚举值一经发布不修改含义
- 分页查询参数格式（0-based page, size, sort）保持不变

### 7.3 包 A 实体变更对包 B 认证逻辑的影响

| 包 A 变更 | 对包 B 的影响 | 处理策略 |
|-----------|-------------|---------|
| User.password NOT NULL | 无运行时影响（种子数据已有密码） | 确保 DDL 变更前已清理 NULL 数据 |
| User.enabled 默认值 true | 无影响（enabled 语义不变） | — |
| PermissionFunction 类名变更 | 所有引用 `Function.java` 的文件需同步更新 | 修改包 B 中所有 import 和引用 |
| PermissionFunction.visible 默认值 true | 菜单查询结果语义不变 | — |
| schema.sql deleted NOT NULL | 16 张表的 ALTER TABLE 需在 Phase 1 上线前执行 | 数据库迁移脚本按报告中的步骤执行 |
| UserRepository 返回 Optional | AuthServiceImpl.login() 调用处理方式变更 | 从 null 检查改为 Optional.map/orElseThrow |

### 7.4 包 D 前端对包 B 的补偿机制

由于包 B 的 Token 黑名单在 Phase 1 使用内存实现，面临多实例不一致问题。包 D 前端需配合以下补偿策略：

- 登出时即使后端返回失败（网络原因），前端 `finally` 块仍清除本地 token
- 401 静默刷新连续 3 次失败后清除所有认证数据并跳转登录页（防死循环）
- 用户信息不在前端持久化权限决策（菜单渲染依赖 `GET /api/menu/tree`，不从 localStorage 读取 `permissions`）

## 8. 审查问题修复方案汇总

### 8.1 包 B 后端问题

| # | 问题 | 当前状态 | 修复方案 | 潜在副作用 | 影响范围 |
|---|------|---------|---------|-----------|---------|
| C1 | 用户禁用后 token 仍然有效 | Filter 和 Service 未检查 enabled | JwtAuthenticationFilter 每次请求从 DB 加载用户并检查 enabled + deleted | 每次认证请求增加一次 DB 查询 | JwtAuthenticationFilter |
| C2 | 登录无防暴力破解 | 无速率限制 | 三层防护：IP 限流 + 失败计数 + 账户锁定 | 误锁定合法用户（需配置合理阈值） | AuthServiceImpl, 新增 RateLimitGuard, LoginAttemptTracker |
| C3 | userId 提取逻辑重复 4 次 | Claims Integer→Long 兼容代码重复 | 抽取 `JwtTokenProvider.getUserIdFromClaims(Claims)` 静态方法 | 无 | JwtTokenProvider |
| C4 | 登出 No-Op | logout() 为空方法 | 引入 TokenBlacklist，登出时将 jti 加入黑名单 | 内存占用随登出量增长 | AuthServiceImpl, InMemoryTokenBlacklist |
| C5 | Access Token 自刷新无轮换 | 用 access token 自身刷新 | 引入 Refresh Token + 轮换机制 | 刷新流程变复杂，需前后端同步改造 | JwtTokenProvider, AuthServiceImpl, 前端 auth store |
| H1 | findByUsername 返回非 Optional | 返回 User | 改为 `Optional<User>` | 编译期影响所有调用方 | UserRepository, AuthServiceImpl |
| H2 | JWT SecretKey 未缓存 | 每次构造 | `@PostConstruct` 缓存 SecretKey 字段 | 无 | JwtTokenProvider |
| H8 | 密码复杂度弱 | 仅 `@Size(min=6)` | 新增 PasswordPolicy 校验（3/4 字符种类 + 8-64 长度） | 已有弱密码用户修改密码时需适应性变更 | AuthServiceImpl, PasswordPolicy |
| H11 | ProfileUpdateRequest 无手机号校验 | 仅限制最大长度 | 添加 `@Pattern(regexp = "^1[3-9]\\d{9}$")` | 已有非 1 开头手机号的用户无法通过校验 | ProfileUpdateRequest |
| H12 | 用户/角色变更后 token 有效 | JWT claims 不可撤销 | Filter 层每次从 DB 加载权限（不再依赖 claims 中的角色） | 每次请求多一次 DB 查询 | JwtAuthenticationFilter |
| M3 | TokenStore 死代码 | admin/entity/TokenStore.java 未使用 | Phase 1 保留但不注册为 @Entity，Phase 2 启用时恢复 | 无 | 无 |
| M4 | AuthController 依赖 JwtUtil | 直接在 Controller 层提取 token | 引入 CurrentUser 接口，Controller 通过 SecurityContext 获取 | Controller 层需调整 token 提取方式 | AuthController, CurrentUser |
| M6 | DTO 未用 record | 传统 POJO | 全部认证相关 DTO 改为 Java 17 record | Jackson 序列化兼容（record 默认 immutable，需确认反序列化行为） | dto/package |
| M7 | Entity 风格混用 | 手写 getter/setter 与 Lombok 混用 | 统一为手写 getter/setter（与 Phase 0 约定一致） | 无 | User, Role, Post, PermissionFunction |
| M9 | login() 全量查询 + 懒加载 | 查询完整 User 实体 | 查询时使用 `@EntityGraph(attributePaths = {"roles", "posts"})` 显式控制 fetch join，避免 N+1 | 需在 UserRepository 新增方法 | UserRepository, AuthServiceImpl |
| M10 | Phase0/Phase1 SecurityConfig 并发激活 | 两个 PasswordEncoder bean | Phase 1 的 application.yml profiles 列表移除 `phase0` | 需同步确保 Phase 0 配置类不会影响生产 | application.yml |
| M12 | 用户信息明文存 localStorage | 完整 UserInfo 明文存储 | 仅存储最小标识（userId + username），权限数据不从 localStorage 读取 | 需调整前端权限判断逻辑，由 API 返回为准 | 前端 auth store |

### 8.2 包 D 前端问题

| # | 问题 | 当前状态 | 修复方案 | 潜在副作用 | 影响范围 |
|---|------|---------|---------|-----------|---------|
| C6 | logout() 缺少 try-finally | API 抛异常时 clearAuthData 不执行 | 包裹 try-finally，finally 中清除认证数据 | 无 | createAuthStore.logout() |
| C7 | 导航守卫竞态 | 并发 fetchMenus() 导致重复 addRoute | fetchMenus() 内部加互斥锁（fetching 标志 + busy-wait 或 Promise 队列） | 增加约 20 行代码 | createMenuStore.fetchMenus() |
| H3 | 401 不触发自动刷新 | 仅 initializeAuth 中刷新 | axios 响应拦截器中添加 401 → refreshToken → retry 逻辑，失败 3 次后清除 | 刷新逻辑复杂度增加，需防无限重试 | api/index.ts, auth store |
| H4 | convertMenusToRoutes 硬编码 'Layout' | 父路由名写死 | 将 Layout 路由名称参数化，通过 createMenuStore 配置参数传入 | 现有调用方需传入参数 | createMenuStore, doctor/admin router |
| H5 | 菜单仅两级 | children 不递归 | convertMenusToRoutes 改为递归，任意深度菜单均可注册路由 | 路由 name 生成需防冲突 | createMenuStore |
| H6 | 循环依赖脆弱 | 顶层 import router → import store | 使用延迟加载函数替代顶层 import，或通过 `getCurrentInstance()` 获取 router | 突破现有懒初始化模式的额外复杂度 | doctor/admin stores/menu.ts |
| H7 | 导航守卫阻塞 UI | await fetchMenus 期间所有导航挂起 | 添加 loading 状态 + 全局 loading 组件（如 NProgress 或骨架屏） | 需引入 UI loading 组件 | router/index.ts |
| H9 | Swagger/API 文档公开暴露 | permitAll | 生产环境 `springdoc.api-docs.enabled=false` + SecurityConfig 拒绝（denyAll） | 开发环境需单独配置 | SecurityConfigPhase1, application-prod.yml |
| H10 | JWT Filter 静默跳过无效 token | 不返回 401 | 改为 Filter 发送统一 401 响应（确保响应体与 AuthenticationEntryPoint 一致） | 需调整非认证端点的访问路径 | JwtAuthenticationFilter |
| M11 | activeMenu 状态冗余 | 同时依赖 activeMenu 和 route.path | 删除 activeMenu 状态，完全依赖 `useRoute().path` 判断激活菜单 | SidebarBase 重构 | createMenuStore, SidebarBase |
| M12 | SidebarBase 直接用 useRoute() | 与注入 routerInstance 模式不一致 | SidebarBase 通过 props 接收当前路径，由父组件通过 useRoute() 传入 | 父组件工作量增加 | SidebarBase, LayoutBase |
| M13 | apiGet catch 类型不安全 | `error as BusinessError` | 运行时类型收窄：先 `instanceof` 或 `isBusinessError()` 检查，再断言 | 多一层检查开销（可忽略） | api/index.ts |
| M14 | BusinessError.isBusinessError 可选 | 编译期无法强制校验 | 改为必选属性 `isBusinessError: true` | 无 | types/index.ts |
| M15 | 无前端输入校验 | LoginBase 无校验 | 在 LoginBase 中添加基础校验（username 非空、password 最小长度 6） | 与后端校验重复但安全 | LoginBase.vue |
| M16 | 用户信息明文存 localStorage | role/permissions 暴露 | 存储最小标识（userId + username），权限数据通过 API 获取 | 需调整 auth store 和 menu store 的初始化流程 | createAuthStore |

### 8.3 包 A 数据建模问题

| # | 问题 | 当前状态 | 修复方案 | 潜在副作用 | 影响范围 |
|---|------|---------|---------|-----------|---------|
| A1 | password 无 NOT NULL | JPA 和 DDL 均缺约束 | 添加 `@Column(nullable=false)` + DDL NOT NULL | 需先清理已有 NULL 密码数据 | User.java, schema.sql |
| A2 | deleted 列 NOT NULL 不一致 | 16 张表 DDL 缺少 NOT NULL | schema.sql 全部修正为 `NOT NULL DEFAULT 0` | 16 张表 ALTER TABLE 风险 | schema.sql, 生产迁移 |
| A3 | enabled/visible 无 Java 默认值 | 四个实体 5 个字段缺默认值 | `private Boolean enabled = true;` / `visible = true;` | 已有 NULL 数据不受影响 | User, Role, Post, PermissionFunction |
| A4 | 缺少集成测试 | EntityMappingIT 未覆盖 User/Role/Post | 新增三组集成测试（见诊断报告中的模板） | 无 | EntityMappingIT.java |

## 9. 并发设计

### 9.1 Token 黑名单并发安全

`InMemoryTokenBlacklist` 使用 `ConcurrentHashMap<String, Long>`，所有操作（add/containsKey）均为线程安全。定时清理任务使用 `ScheduledExecutorService`，在单线程执行器中串行执行，不会与业务线程产生并发冲突。

### 9.2 速率限流并发安全

`InMemoryRateLimitGuard` 的滑动窗口算法使用 `synchronized` 或 `ReentrantLock` 保护窗口内的排序集合。每个 IP 的窗口对象独立，细粒度锁减少竞争。限流检查仅发生在登录请求，并发量低，锁竞争可忽略。

### 9.3 登录失败并发安全

`LoginAttemptTracker` 使用 `ConcurrentHashMap`，单个用户名的失败计数更新通过 `compute` 方法原子性操作。锁定状态的检查和更新在同一个 `compute` 闭包中完成，确保线程安全。

## 10. 错误处理策略

### 10.1 错误分类

| 类别 | 处理方式 | 响应码 | HTTP 状态码 |
|------|---------|-------|------------|
| 认证失败（无效 token、过期） | 抛出 BusinessException 或 AuthenticationException | UNAUTHORIZED | 401 |
| 授权失败（无权限） | 抛出 AccessDeniedException | FORBIDDEN | 403 |
| 参数校验失败 | @Valid 触发 MethodArgumentNotValidException | PARAM_INVALID | 400 |
| 业务逻辑错误（用户禁用等） | 抛出 BusinessException | 见各 ErrorCode | 200（业务错误仍为 200） |
| 速率限制 | 抛出 RateLimitExceededException | RATE_LIMITED | 429 |
| 系统异常 | 由 GlobalExceptionHandler 捕获 | SYSTEM_ERROR | 500 |

### 10.2 认证相关 ErrorCode

| 枚举值 | code | message | 触发场景 |
|-------|------|---------|---------|
| UNAUTHORIZED | UNAUTHORIZED | "未认证或令牌已失效" | token 缺失/无效/过期/黑名单 |
| FORBIDDEN | FORBIDDEN | "无权限访问" | 角色权限不足 |
| ACCOUNT_DISABLED | ACCOUNT_DISABLED | "账户已停用" | enabled=false |
| ACCOUNT_LOCKED | ACCOUNT_LOCKED | "账户已锁定，请 15 分钟后重试" | 登录失败超限锁定 |
| RATE_LIMITED | RATE_LIMITED | "请求过于频繁，请稍后重试" | IP 限流触发 |
| PASSWORD_TOO_WEAK | PASSWORD_TOO_WEAK | "密码不符合复杂度要求" | 密码策略校验失败 |
| TOKEN_REFRESH_FAILED | TOKEN_REFRESH_FAILED | "令牌刷新失败，请重新登录" | Refresh Token 黑名单或过期 |
| PASSWORD_CHANGE_REQUIRED | PASSWORD_CHANGE_REQUIRED | "需要修改密码" | 首次登录或管理员强制要求 |

## 11. 设计决策

| 决策 | 选项 | 选择 | 理由 |
|------|------|------|------|
| Token 黑名单实现 | 内存 vs Redis | Phase 1 内存 | Phase 1 为单实例部署，内存方案足够；避免 Phase 2 前的 Redis 引入成本 |
| Refresh Token 轮换 | 轮换 vs 不轮换 | 轮换 | 轮换是最佳实践（RFC 6749），在 Phase 1 即引入避免后续安全改造 |
| Access Token 有效期 | 15 分钟 vs 30 分钟 vs 24 小时 | 15 分钟 | 短有效期最小化泄露窗口，配合 Refresh Token 轮换提供安全性和用户体验的平衡 |
| JWT claims 中是否包含权限 | 包含 vs 不包含 | 不包含 | 权限从 DB 实时加载，角色变更即时生效；避免 JWT claims 与 DB 不一致 |
| Filter 中是否查库验证 enabled | 查库 vs 仅依赖 claims | 查库 | 确保禁用用户即时失效，这是安全审计的硬性要求 |
| DTO 形态 | record vs 传统 POJO | record（Java 17） | 减少样板代码，不可变 DTO 适合数据传输场景 |
| 密码策略接口化 | interface vs 硬编码 | PasswordPolicy interface | 支持未来切换复杂度规则策略而不改核心代码 |
| 速率限制维度 | IP 维度 vs 用户维度 vs 双维度 | 双维度 | IP 维度防御基础设施攻击，用户维度防御凭据猜测 |
| 登出清理范围 | 仅前端 vs 前后端联动 | 前后端联动 | 后端黑名单确保 token 不可复用，前端 finally 清理兜底 |
| PermissionFunction 类名 | 保留 Function vs 改名 | PermissionFunction | 避免与 `java.util.function.Function` 的 IDE 混淆；DB 表名 `sys_function` 不变 |
| SecurityContext 装配内容 | 仅 role vs role + 功能权限 | role + 功能权限 | `@PreAuthorize` 的方法级控制需要完整的 authority 列表 |

## 12. 修订说明（v1）

首版设计。
