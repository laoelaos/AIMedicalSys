# Phase 1 包 A/B/D 统一修复与包 B OOD 设计方案（v12）

## 1. 概述

### 1.1 设计目标

本设计面向 Phase 1 三个耦合包（包 A 数据实体、包 B 统一认证模块、包 D 前端登录与菜单）的缺陷修复与架构补全。核心目标如下：

- **补全包 B 设计契约**：包 B（统一认证模块）已有骨架代码但缺乏独立 OOD 文档，需从架构层面定义认证流程、令牌体系、安全策略、模块边界，使后续开发、审查和维护有契约可依
- **修复已发现的设计缺陷**：基于审查报告中的 19+4 项问题（含后端 B 包、前端 D 包、包 A 数据建模），在 OOD 层面逐一给出修复方案，消除安全漏洞和设计反模式
- **明确 A/B/D 协作边界**：包 A（数据实体）→ 包 B（认证服务）→ 包 D（前端消费）的依赖链在架构层面显式定义，减少跨包耦合风险

### 1.2 整体架构思路

包 B 作为统一认证模块，定位为 **common-module 的子模块**，归属 `common-module-impl` 实现层。采用**无状态 JWT 为主、辅助服务端状态管理为辅**的混合策略：

- **主要认证机制**：JWT Bearer Token，无状态认证，避免 Session 管理复杂度
- **辅助状态**：引入轻量级内存 Access Token 黑名单（Phase 1 仅黑名单 Access Token，避免 Refresh Token 黑名单的内存爆炸问题；Phase 2 迁移 Redis 后启用完整黑名单）
- **防暴力破解**：基于 IP 的速率限制 + 登录失败计数 + 账户临时锁定三层防护
- **角色/权限动态刷新**：关键操作（访问受保护资源、刷新 token）强制查库验证用户状态

### 1.3 核心抽象一览

| 抽象 | 类型形态 | 职责定位 |
|------|---------|---------|
| `AuthService` | interface | 统一认证服务的业务契约，定义登录、登出、刷新、获取当前用户、更新资料的边界 |
| `JwtTokenProvider` | @Component class | JWT 令牌生成、解析、验证的集中提供者；隐含 SecretKey 缓存和 Claims 提取工具方法；通过 `@Component` 注册为 Spring Bean 确保 `@PostConstruct` 启动验证生效 |
| `JwtAuthenticationFilter` | class | OncePerRequestFilter 实现，拦截已认证请求，提取并验证 Token，装配 SecurityContext |
| `PasswordChangeCheckFilter` | class | OncePerRequestFilter 实现，在 JwtAuthenticationFilter 之后检查 passwordChangeRequired 状态，对白名单之外的 API 返回 403 |
| `GlobalRateLimitFilter` | class | OncePerRequestFilter 实现，对所有非白名单 API 路径实施 IP 级速率限制（100 次/60 秒），在 JwtAuthenticationFilter 之前执行 |
| `RateLimitGuard` | interface | 速率限制策略契约，支持 IP 级别和用户级别的限流判定 |
| `LoginAttemptTracker` | class | 登录失败计数与账户临时锁定逻辑的封装 |
| `TokenBlacklist` | interface | Token 失效状态查询的抽象，登出后标记 token 为不可用 |
| `PasswordPolicy` | interface | 密码复杂度策略契约，定义校验方法 |
| `PasswordChangeService` | interface | 密码变更策略契约，定义首次登录强制修改密码和管理员标记密码过期的判定 |
| `SecurityConfigPhase1` | class | Spring Security 配置聚合点：SecurityFilterChain、密码编码器、CORS、异常处理入口 |
| `CurrentUser` | interface | 当前登录用户的轻量级类型化访问器，消除 Controller 层对 SecurityContextHolder 的直接操作 |

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
│   │   ├── GlobalRateLimitFilter.java       # 全局 IP 限流过滤器
│   │   ├── JwtAuthenticationFilter.java      # JWT 鉴权过滤器
│   │   ├── PasswordChangeCheckFilter.java    # 密码变更检查过滤器
│   │   └── SecurityConfigPhase1.java        # Security 配置
│   ├── rateLimit/
│   │   ├── RateLimitGuard.java              # 限流策略接口
│   │   └── InMemoryRateLimitGuard.java      # 内存限流实现（Phase 1）
│   ├── login/
│   │   └── LoginAttemptTracker.java         # 登录失败计数 + 锁定（含惰性清理）
│   ├── blacklist/
│   │   ├── TokenBlacklist.java              # Token 黑名单接口
│   │   └── InMemoryTokenBlacklist.java      # 内存黑名单实现（Phase 1，仅 Access Token）
│   ├── password/
│   │   ├── PasswordPolicy.java              # 密码复杂度策略
│   │   └── PasswordChangeService.java       # 密码变更策略（首次登录强制修改/管理员过期标记）
│   ├── dto/
│   │   ├── request/
│   │   │   ├── LoginRequest.java            # 登录请求
│   │   │   ├── RefreshTokenRequest.java     # 刷新请求（含 refreshToken 字段）
│   │   │   ├── PasswordChangeRequest.java   # 密码修改请求（含 oldPassword、newPassword）
│   │   │   └── ProfileUpdateRequest.java    # 资料更新请求
│   │   └── response/
│   │       ├── LoginResponse.java           # 登录响应（含 accessToken、refreshToken、user）
│   │       ├── UserInfoResponse.java        # 当前用户信息
│   │       └── TokenRefreshResponse.java    # 刷新响应
│   └── converter/
│       └── UserConverter.java              # User → DTO 转换
├── menu/
│   ├── controller/
│   │   └── MenuController.java
│   ├── service/
│   │   ├── MenuService.java
│   │   └── impl/
│   │       └── MenuServiceImpl.java
│   ├── dto/
│   │   ├── MenuResponse.java               # 菜单响应 DTO
│   │   ├── MenuCreateRequest.java           # 菜单创建请求
│   │   └── MenuUpdateRequest.java           # 菜单更新请求
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
- 业务模块若需获取当前用户身份，通过 `common-module-api` 中的门面接口获取，不直接接触认证实现
- `JwtAuthenticationFilter` 和 `PasswordChangeCheckFilter` 放置在 `common-module-impl/auth/security/` 下，与 JwtTokenProvider 有强内聚关系；application 模块通过 `@ComponentScan` 或 `@Import` 引入。迁移步骤：application 模块中原有 `JwtAuthenticationFilter` 的引用全部删除，包括 `SecurityConfigPhase1` 构造函数中的依赖注入（旧包名引用需改为新包名），由 common-module-impl 中 auth/security 包下的新 Filter 替代；application 模块仅保留 SecurityConfig 中的 Filter 注册配置
- `TokenBlacklist` 和 `RateLimitGuard` 在 Phase 1 使用内存实现（`ConcurrentHashMap`），不引入外部存储依赖。Phase 2 迁移到 Redis 时，增加 Redis 实现类并注入即可，接口不变

## 3. 核心设计

### 3.1 认证流程

#### 3.1.1 登录流程

1. 前端提交 `{ username, password }` 到 `POST /api/auth/login`
2. RateLimitGuard 检查请求来源 IP 是否触发限流（同 IP 每 10 秒最多 5 次尝试）
3. LoginAttemptTracker 双维度锁定状态检查：先检查请求来源 IP 是否处于临时锁定状态（连续失败 20 次后锁定 30 分钟），再检查该用户名是否处于临时锁定状态（连续失败 5 次后锁定 15 分钟）。若 IP 维度已锁定，返回 ErrorCode.ACCOUNT_LOCKED（HTTP 429），消息"账户已锁定，请 30 分钟后重试"；若用户名维度已锁定，返回 ErrorCode.ACCOUNT_LOCKED（HTTP 429），消息"账户已锁定，请 15 分钟后重试"。任一维度锁定命中均不继续执行步骤 4-11
4. UserRepository.findByUsername() 以 `Optional<User>` 形式加载用户
5. `Optional` 为空 → 对虚拟哈希值执行 dummy BCrypt 比对以消除与步骤 7 的响应时间差异 → 递增 LoginAttemptTracker **IP 维度**的失败计数（因无有效用户名作为 key） → 抛出 BusinessException（ErrorCode.LOGIN_FAILED，消息"用户名或密码错误"）
6. 用户 `enabled == false` 或 `deleted == true` → 对虚拟哈希值执行 dummy BCrypt 比对以消除与步骤 7 的响应时间差异 → 递增 LoginAttemptTracker **用户名和 IP 双维度**的失败计数 → 抛出 BusinessException（ErrorCode.LOGIN_FAILED，消息"用户名或密码错误"）
7. `passwordEncoder.matches(password, user.password)` 失败 → 递增 LoginAttemptTracker **用户名维度**的失败计数 → 抛出 BusinessException（ErrorCode.LOGIN_FAILED，消息"用户名或密码错误"）
8. 成功 → LoginAttemptTracker 清除该用户名的失败计数，同时清除该请求来源 IP 的失败计数
9. PasswordChangeService 检查用户是否需要修改密码（首次登录或管理员已标记密码过期），若需要则在 LoginResponse 中标记 `passwordChangeRequired = true`
10. JwtTokenProvider 从 `User.tokenVersion` 读取当前版本号，嵌入 Refresh Token claims 中的 `tokenVersion` 字段，然后生成 Access Token（短有效期 15 分钟）和 Refresh Token（长有效期 7 天，携带唯一 jti 标识和 tokenVersion）
11. 返回 LoginResponse（含 accessToken、refreshToken、expiresIn、passwordChangeRequired、user 基本信息）

**设计要点**：步骤 5/6/7 使用统一的 ErrorCode.LOGIN_FAILED 和消息"用户名或密码错误"，不泄露用户具体状态（是否存在、是否禁用）。此策略消除了用户名可枚举的安全风险。步骤 5 和步骤 6 分别在用户名不存在和用户被禁用/删除时执行 dummy BCrypt 比对，消除与步骤 7（真实 BCrypt 比对）之间的响应时间差异，防止攻击者通过时序侧信道枚举有效用户名。步骤 6 的禁用/删除场景攻击面较小（需要知道有效用户名才能触发），但为统一防御仍增加 dummy BCrypt 比对。

**IP 维度计数器关系说明**：步骤 5（用户名不存在）中递增的 IP 维度失败计数，与步骤 6/7（用户名存在但密码错误）中递增的 IP 维度失败计数，是**共用同一个 IP 维度计数器**。即 `LoginAttemptTracker` 中 IP 维度全局共享一个计数器，不区分触发步骤。用户名维度按用户名独立计数。因此步骤 5 虽然使用"虚拟哈希"不占用实际用户名 key 空间，但 IP 维度的计数仍然是真实有效的。

#### 3.1.2 已认证请求流程

1. JwtAuthenticationFilter 从 `Authorization: Bearer <token>` 提取 token
2. JwtTokenProvider.validateToken(token) 解析并验证签名、有效期
3. TokenBlacklist.isBlacklisted(token) 检查 Access Token 的 jti 是否已被登出标记
4. 从 DB 加载用户完整信息：检查 `enabled == true` 且 `deleted == false`
5. 装配 `UsernamePasswordAuthenticationToken`（principal = 用户 ID，authorities = 从 DB 读取的角色+功能权限）
6. 设置 `SecurityContextHolder`
7. PasswordChangeCheckFilter 检查用户 `passwordChangeRequired` 状态：若为 true 且请求路径不在白名单中（`/api/auth/password`、`/api/auth/logout`），返回 403 并清除 SecurityContext，不继续执行 `chain.doFilter()`
8. `chain.doFilter()` 放行

**关键设计决策**：
- 步骤 4 查库验证 enabled 状态是必须的。无状态 JWT 无法撤销，若仅依赖 token 内的 claims 判断用户状态，禁用用户后旧 token 仍可使用。此查库操作仅发生在 Filter 层，对已认证的后续请求不重复查库
- 用户被禁用（enabled=false）时抛出 AuthenticationException 携带 ACCOUNT_DISABLED 错误码，而非静默跳过。因已认证请求中攻击面已缩小，给出具体禁用原因不引入安全风险
- passwordChangeRequired 检查抽离为独立 Filter（PasswordChangeCheckFilter），遵循单一职责原则。JwtAuthenticationFilter 仅负责 JWT 鉴权，PasswordChangeCheckFilter 专注于业务规则检查。两 Filter 通过 SecurityConfig 中的顺序排列确保执行链正确
- 性能优化：JwtAuthenticationFilter 将 `passwordChangeRequired` 状态存入 request attribute，PasswordChangeCheckFilter 从 attribute 而非 DB 读取，避免同一请求中两次查询用户

#### 3.1.3 Token 刷新流程

1. 前端提交 `{ refreshToken }` 到 `POST /api/auth/refresh`
2. RefreshTokenRequest 中携带 refreshToken 字符串
3. JwtTokenProvider 验证 Refresh Token 的有效性（签名、有效期、type claim 必须为 "refresh"，claims 中含 tokenVersion）
4. 若 Phase 1 内存方案，不检查 Refresh Token 黑名单（因 Phase 1 仅黑名单 Access Token）；Phase 2 Redis 方案启用后在此步骤检查 Refresh Token jti 是否在黑名单中
5. 从 DB 重新加载用户最新状态（enabled、roles、posts、tokenVersion）
6. 检查 LoginAttemptTracker 中该用户名是否处于临时锁定状态（累计失败超限且窗口未到期）。若已锁定，返回 TOKEN_REFRESH_FAILED 错误并强制前端清除本地 token，防止账户锁定绕过
7. 若用户已被禁用或被删除，返回 TOKEN_REFRESH_FAILED 错误并强制前端清除本地 token
8. 验证 Refresh Token claims 中的 tokenVersion 与 DB 中当前 tokenVersion 一致，若不一致说明密码已变更，拒绝刷新并强制前端清除本地 token
9. 签发新的 Access Token（15 分钟）和 Refresh Token（7 天，新 jti，携带当前 tokenVersion），旧 Refresh Token 的黑名单操作说明见 4.2 节
10. 返回 TokenRefreshResponse（仅含 accessToken、refreshToken、tokenType、expiresIn，不含用户信息）
11. 前端收到刷新响应后，调用 `GET /api/auth/me` 获取最新用户信息（角色、权限等），更新本地用户状态

**刷新时机**：前端 axios 响应拦截器监测到 401 时，自动触发静默刷新。刷新成功后依次重放原始请求和 `/api/auth/me` 请求以更新本地用户信息，失败则清除认证数据并跳转登录页。

**安全说明**：Phase 1 中，旧 Refresh Token 在轮换后未加入黑名单，因此旧 token 技术上仍可被重复使用。引入 Refresh Token tokenVersion 机制后，密码变更场景下的旧 Refresh Token 即时失效，但密码未变更场景下的旧 Refresh Token 仍可重复使用。服务端不依赖于旧 Refresh Token 的一次性语义来保证安全。详细的风险补偿措施见 4.2 节。

#### 3.1.4 登出流程

1. 前端提交当前 Access Token 到 `POST /api/auth/logout`，请求体可选携带 `refreshToken` 字段（`RefreshTokenRequest` 格式，Controller 层使用 `@RequestBody(required=false)` 接收），供服务端记录登出关联的 Refresh Token
2. 后端将 Access Token 的 jti 加入 TokenBlacklist（Phase 1 仅黑名单 Access Token）
3. 若请求体携带了 `refreshToken`，Phase 1 不将其加入黑名单（内存方案不做 Refresh Token 黑名单），但记录安全日志；Phase 2 Redis 方案启用后加入
4. 返回成功响应
5. 前端 finally 块清除 localStorage 中的 token 和 user 信息。即使后端登出 API 因 token 过期/无效返回 401，前端仍应在 finally 块中清除本地认证数据。Token 黑名单的登出记录是"尽力而为"的最优努力，不应阻止本地登出

#### 3.1.5 获取当前用户

1. `GET /api/auth/me` 从 SecurityContext 获取当前用户 ID
2. 查库加载用户完整信息（含角色、岗位、权限列表）
3. 返回 UserInfoResponse

### 3.1.6 密码变更流程

密码变更 API（`PUT /api/auth/password`）的处理流程如下：

1. 前端提交 `{ oldPassword, newPassword }` 到 `PUT /api/auth/password`
2. JwtAuthenticationFilter 验证 Access Token 有效性（要求已登录）
3. PasswordChangeCheckFilter 放行此路径（密码变更在白名单中）
4. AuthServiceImpl.changePassword() 校验旧密码：`passwordEncoder.matches(oldPassword, user.password)` 失败 → 返回 ErrorCode.PASSWORD_MISMATCH，消息"旧密码不正确"
5. 校验新密码复杂度：PasswordPolicy 规则检查，不满足返回对应 ErrorCode
6. BCrypt 编码新密码：`passwordEncoder.encode(newPassword)`
7. 更新 User.password
8. User.tokenVersion 递增（+1），使已签发的旧 Refresh Token 即时失效
9. User.passwordChangeRequired 设为 false（若此前为 true）
10. 清除当前请求 SecurityContext（清除当前线程的安全上下文，避免密码变更操作后的请求误用旧上下文），客户端可继续使用旧 Access Token 至其自然过期（最长 15 分钟）
11. 返回成功响应

**设计说明**：
- Access Token 不检查 tokenVersion 是已知设计决策，非疏忽：Access Token 有效期仅 15 分钟，短窗口内即使旧 token 未被撤销，攻击面也有上限。查库验证 enabled 状态已提供基本保护。   完整的 tokenVersion 检查（含 Access Token 层面）将在 Phase 2 配合 Redis 黑名单方案一并引入。步骤 10 清除 SecurityContext 仅为清除当前请求安全上下文，不强制前端重新登录；客户端可继续使用旧 Access Token 至其自然过期
- 密码变更后不清除已登录用户的 Access Token。用户可继续使用当前会话至 Access Token 过期（最长 15 分钟）。这是有意为之的权衡——避免强制登出带来的用户体验损失，且 Access Token 短有效期已足够控制风险
- 前端在收到密码变更成功响应后，应由 `PUT /api/auth/password` 成功响应触发恢复流程：清除 `passwordChangeRequired` 标记 → 调用 `GET /api/auth/me` 刷新用户信息 → 调用 `GET /api/menu/tree` 获取最新菜单 → 跳转到系统首页。此流程已在 7.4 节定义

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
  "tokenVersion": 0,              // 当前 tokenVersion，密码变更后拒绝旧 Refresh Token
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
    ├── cors(Customizer.withDefaults())    // 开发默认；生产环境见 4.6 节
    ├── exceptionHandling
    │   ├── authenticationEntryPoint       // 行为契约：AuthenticationException 携带 ACCOUNT_DISABLED 时返回 Result.fail("ACCOUNT_DISABLED", "账户已被管理员停用")；其余未认证场景返回 Result.fail("UNAUTHORIZED", "未认证或令牌已失效")
    │   └── accessDeniedHandler            // 检查异常类型：PasswordChangeRequiredException → 返回 Result.fail("PASSWORD_CHANGE_REQUIRED", "需要修改密码", null, 403)；其余 AccessDeniedException → 返回 Result.fail("FORBIDDEN")
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
    └── addFilterBefore(GlobalRateLimitFilter, JwtAuthenticationFilter)
        addFilterBefore(JwtAuthenticationFilter, UsernamePasswordAuthenticationFilter)
        addFilterAfter(PasswordChangeCheckFilter, JwtAuthenticationFilter)

  PasswordEncoder:
    └── @Bean BCryptPasswordEncoder (Strength 10)
```

**设计要点**：
- 两阶段 `SecurityConfig` 通过 `@Profile("phase0")` 和 `@Profile("phase1")` 隔离。Phase 0 的 SecurityConfig 放行所有请求（`permitAll`），仅作为骨架占位；Phase 1 的 SecurityConfig 激活 JWT 认证。`application.yml` 中 profiles 列表在 Phase 1 应移除 `phase0`，确保两个 `SecurityFilterChain` bean 不会冲突
- `AuthenticationEntryPoint` 行为契约：当 AuthenticationException 携带 ErrorCode.ACCOUNT_DISABLED 时返回 `Result.fail("ACCOUNT_DISABLED", "账户已被管理员停用")`；其余未认证场景（token 缺失、无效、过期、黑名单）统一返回 `Result.fail("UNAUTHORIZED", "未认证或令牌已失效")`。确保 JWT 过期、无效、缺失时响应体结构与业务异常一致
- `@EnableMethodSecurity` 启用，为后续 `@PreAuthorize` 方法级权限控制预留
- `/api/auth/refresh` 在 SecurityConfig 和保护清单中统一为 `permitAll`，因其内部通过 Refresh Token 自验证，不需要前置 JWT 认证
- GlobalRateLimitFilter 注册在 JwtAuthenticationFilter 之前，对所有请求实施 IP 级全局限流（白名单路径放行），确保限流优先于认证检查
- PasswordChangeCheckFilter 注册在 JwtAuthenticationFilter 之后，确保 JWT 鉴权+用户 enabled 检查优先完成，然后再执行业务规则检查

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

#### PasswordChangeCheckFilter 行为契约

```
PasswordChangeCheckFilter.doFilterInternal(request, response, chain):
  1. 从 SecurityContextHolder 获取 Authentication（若无 → chain.doFilter 放行，由下游处理）
  2. 从 Authentication.principal 获取 userId
  3. 从请求 attribute 读取 passwordChangeRequired 状态（由 JwtAuthenticationFilter 步骤 5 写入）：
     a. false（或无 attribute）→ chain.doFilter 放行
     b. true → 检查请求路径是否在白名单中：
        - /api/auth/password（PUT）→ chain.doFilter 放行
        - /api/auth/logout（POST）→ chain.doFilter 放行
         - 其他 → 清除 SecurityContext，抛出 `PasswordChangeRequiredException`（扩展 `AccessDeniedException`），由 AccessDeniedHandler 捕获后返回 403 + ErrorCode.PASSWORD_CHANGE_REQUIRED
```

**设计要点**：此 Filter 在 JwtAuthenticationFilter 之后执行，确保只有已通过 JWT 鉴权的请求才会进入密码变更检查。通过 request attribute 读取 passwordChangeRequired，避免从 DB 重复查询同一用户。白名单之外的请求返回 403（PASSWORD_CHANGE_REQUIRED）而非标准 FORBIDDEN，用于前端识别"需要修改密码"的业务限制场景。

### 3.4 用户状态管理

#### Enabled 禁用检查

每次请求的 Filter 层从 DB 加载用户完整状态（而非仅依赖 token claims）。这是禁用用户的即时生效机制：管理员禁用用户后，该用户的下一次请求在 Filter 层即被拒绝。

检查链：
1. JwtAuthenticationFilter 从 JWT 提取 userId
2. UserRepository.findById(userId) 加载用户
3. 检查 `user.deleted == false` → 不满足则清除 SecurityContext，chain.doFilter 放行（静默跳过）
4. 检查 `user.enabled == true` → 不满足则抛出 AuthenticationException（ErrorCode.ACCOUNT_DISABLED），由 AuthenticationEntryPoint 返回 401
5. 两项检查均通过 → 继续执行后续 Filter 链

#### 角色/权限动态刷新

每次请求从 DB 重新加载用户的角色和权限列表并装配到 SecurityContext 中。这意味着：
- 管理员修改用户角色/岗位后，该用户的下一次请求即反映变更
- 无需依赖 JWT claims 中的角色数据（JWT 仅用于身份标识和防篡改）
- 代价是每次请求多一次 DB 查询，通常在 1-5ms 内，可接受

#### Refresh Token 轮换策略

每次使用 Refresh Token 时：
1. 验证 Refresh Token 有效（签名、有效期、type=refresh）
2. 从 DB 重新加载用户最新状态，若用户已禁用则拒绝刷新并强制前端清除 token
3. 签发新的 Access Token + Refresh Token（含全新 jti）
4. 客户端收到新 token 后替换本地存储

Phase 1 中旧 Refresh Token 不会被加入黑名单，因此旧 token 技术上仍可重复使用。安全补偿策略见 4.2 节。

#### 密码变更强制策略

PasswordChangeService 定义两种密码变更触发场景：
1. **首次登录强制修改**：种子数据中的初始密码标记 `passwordChangeRequired = true`，用户首次登录成功后，LoginResponse 中携带 `passwordChangeRequired: true` 标记，前端检测到此标记后强制跳转到修改密码页面。提交 `PUT /api/auth/password` 成功后清除标记。密码修改成功后前端恢复流程：清除 `passwordChangeRequired` 标记 → 调用 `GET /api/auth/me` 刷新用户信息 → 调用 `GET /api/menu/tree` 获取最新菜单 → 跳转到系统首页。**异常场景处理**：若恢复流程中 `GET /api/auth/me` 或 `GET /api/menu/tree` 任一请求失败，前端应在 catch 块中记录错误日志并重试最多 2 次（指数退避：1 秒、3 秒间隔）。重试耗尽后仍失败时，前端应清除 `passwordChangeRequired` 本地标记并跳转到系统首页（降级处理：用户失去部分功能菜单不影响正常工作流程），后续页面加载自动重试获取菜单。此降级策略确保密码变更流程不会因中间步骤的网络波动而阻塞用户进入系统。
2. **管理员标记密码过期**：系统管理员通过管理端对特定用户标记密码过期（此功能属于管理端设计范围，本设计不做具体接口定义），User 实体新增 `passwordChangeRequired` 字段（Boolean，默认 false），标记后用户下一次登录时感知并强制修改

#### passwordChangeRequired 访问控制

当用户的 `passwordChangeRequired = true` 时，除以下端点外的所有 API 请求应返回 403 FORBIDDEN：
- `PUT /api/auth/password` — 允许修改密码
- `POST /api/auth/logout` — 允许登出

控制策略在 PasswordChangeCheckFilter 中实现（独立于 JwtAuthenticationFilter），遵循单一职责原则。

## 4. 安全设计

### 4.1 防暴力破解方案

采用三层防护：

**第一层：IP 级速率限制（RateLimitGuard）**

| 维度 | 阈值 | 窗口 | 超额处理 |
|------|------|------|---------|
| 同一 IP（/api/auth/login 专用） | 5 次 | 10 秒滑动窗口 | 429 Too Many Requests，Result.code="RATE_LIMITED" |
| 同一 IP（排除白名单的所有 API 路径） | 100 次 | 60 秒 | 429 Too Many Requests，Result.code="RATE_LIMITED_GLOBAL" |

实现方式：`InMemoryRateLimitGuard` 使用 `ConcurrentHashMap<String, SlidingWindowCounter>`，键为 IP 地址，值为滑动窗口计数器。滑动窗口以 `ReentrantLock` 保护窗口内的排序集合，确保每个 IP 的窗口对象独立加锁。限流检查仅发生在登录请求，并发量低，锁竞争可忽略。

**全局 IP 限流（GlobalRateLimitFilter）**：对所有 API 路径（排除白名单：`/api/auth/login`、`/api/auth/refresh`、`/actuator/health`、`/actuator/info`）实施 IP 级限流。实现为 OncePerRequestFilter，在 JwtAuthenticationFilter 之前执行。白名单路径直接 `chain.doFilter` 放行；非白名单路径检查滑动窗口计数器（100 次/60 秒/IP），超限返回 429 + ErrorCode.RATE_LIMITED_GLOBAL。

**关于 `/api/auth/refresh` 无限流的说明**：Refresh 端点不实施任何速率限制，这是**有意为之的设计决策**。Refresh 端点的安全不依赖于速率限制，而是由以下机制提供保护：(a) Refresh Token 使用独立的类型 claim（`type=refresh`），Access Token 无法通过 refresh 端点获得新令牌；(b) Refresh Token 的 tokenVersion 机制确保密码变更后旧 Refresh Token 即时失效；(c) 每次刷新时强制从 DB 重新加载用户状态（enabled、锁定状态等）；(d) 异常刷新检测机制（5 秒窗口内超过 2 次触发告警）补偿缺失的速率限制。若上线后观察到 Refresh 端点遭受暴力攻击，可独立增加 `RateLimitGuard` 专用限流规则。

GlobalRateLimitFilter**不委托** `InMemoryRateLimitGuard`，而是独立维护自己的滑动窗口计数器（同样基于 `ConcurrentHashMap<String, SlidingWindowCounter>`），与 `InMemoryRateLimitGuard`（针对登录端点 `/api/auth/login` 的专用限流，阈值 5 次/10 秒）职责分工明确：全局限流覆盖所有 API 路径（白名单除外），登录专用限流仅覆盖登录端点。两者共用滑动窗口算法实现，但计数器实例独立，互不干扰。提取公共滑动窗口工具类 `SlidingWindowCounter` 供两处复用。

**SlidingWindowCounter 契约**（包级私有工具类，供 InMemoryRateLimitGuard 和 GlobalRateLimitFilter 共用）：
- **数据结构**：`ConcurrentHashMap<String, Deque<Long>>`，键为限流 key（IP 地址），值为时间戳滑动窗口 Deque
- **精度**：毫秒级时间戳，窗口清理精度 ±50ms（由 `ScheduledExecutorService` 每 60 秒执行一次过期条目回收）
- **线程安全**：每个 key 的 Deque 在 `compute` 闭包内访问，`ReentrantLock` 保护跨窗口操作的原子性（如"检查+新增"复合操作）
- **方法**：`boolean tryAcquire(String key, int limit, long windowMs)` — 若窗口内计数 < 阈值则递增并返回 true，否则返回 false；原子性由 `ConcurrentHashMap.compute` 保证

**ErrorCode 区分策略**：两套限流器使用不同错误码以支持前端识别限流来源——`InMemoryRateLimitGuard`（登录专用限流）返回 `RATE_LIMITED`；`GlobalRateLimitFilter`（全局 IP 限流）返回 `RATE_LIMITED_GLOBAL`。前端可根据错误码分别处理：登录页面专用错误提示 vs 全局限流提示。

生产环境 Phase 2 迁移到 Redis + Lua 脚本。

**第二层：登录失败计数（LoginAttemptTracker）**

| 维度 | 阈值 | 锁定时间 | 重置时机 |
|------|------|---------|---------|
| 同一用户名 | 连续 5 次失败 | 15 分钟 | 锁定到期/登录成功 |
| 同一 IP | 连续 20 次失败 | 30 分钟 | 锁定到期/登录成功 |

实现方式：`LoginAttemptTracker` 使用 `ConcurrentHashMap<String, AttemptRecord>`，AttemptRecord 记录失败次数和首次失败时间戳。超过锁定时间后惰性清除。

**"连续"语义定义**：IP 维度的"连续失败"定义为同一 IP 在指定时间窗口（30 分钟）内的累计登录失败次数。首次失败时记录时间戳，窗口期内任意数量的登录失败均计入累计次数。以下情况重置计数器：(a) 发起成功登录；(b) 窗口超时（惰性清除机制在下次检查时自动处理）。用户名维度的"连续失败"定义为同一用户名在指定时间窗口（15 分钟）内的累计登录失败次数，语义相同。此定义与 AttemptRecord 数据结构（失败次数 + 首次失败时间戳）及惰性清除机制一致，不追踪非登录请求。

**第三层：临时账户锁定**

当 LoginAttemptTracker 触发锁定后，`AuthServiceImpl.login()` 在第一步即检查双维度（IP 和用户名）锁定状态。任一维度被锁定时，即使提供正确密码也无法登录，并返回 ErrorCode.ACCOUNT_LOCKED（HTTP 429），消息根据锁定维度区分锁定时长。这是防止凭据暴力猜测的最后防线。

### 4.2 Token 黑名单 / 轮换设计

#### Phase 1 黑名单策略（仅 Access Token）

鉴于 Refresh Token 黑名单的内存占用（60,480,000 条峰值约 12GB+）远超典型 JVM 堆配置，Phase 1 采用**仅黑名单 Access Token** 的内存方案：

```
InMemoryTokenBlacklist:
  ConcurrentHashMap<String, Long>  // key=jti, value=expirationTime (epoch ms)
  ScheduledExecutorService         // 每 5 分钟清理过期条目
```

- 登出时：仅将当前 Access Token 的 jti 加入黑名单
- 刷新时：旧 Refresh Token 不加入黑名单（Phase 1 内存方案的限制）
- 验证时：JwtAuthenticationFilter 验证 Access Token 后，检查其 jti 是否在黑名单中
- 黑名单条目不永不过期，以 token 原始过期时间为 TTL，通过定时任务回收内存

#### 黑名单大小估算

Phase 1（仅 Access Token）：
- Access Token 黑名单峰值：100 req/s × 900s = 90,000 条
- 每条约 72 bytes（UUID 36 字符 + Long 8 字节 + ConcurrentHashMap 开销），总内存约 6.5MB
- 完全在 JVM 可承受范围内

Phase 2（Redis 方案）：
- 迁移至 Redis SET 或 Hash 存储，解决多实例一致性问题
- 同时黑名单 Access Token 和 Refresh Token
- 使用 Redis TTL 自动过期，无需定时清理任务
- 新增 `RedisTokenBlacklist` 实现类，通过 `@ConditionalOnProperty` 或 Profile 切换

#### Refresh Token 的安全补偿策略

Phase 1 中 Refresh Token 轮换后旧 token 不会被加入黑名单，因此旧 Refresh Token 技术上仍可被重复使用。这是一个已知的安全局限性。通过以下机制补偿：

- **Access Token 短有效期（15 分钟）**：即使 Refresh Token 泄露，攻击者获取的新 Access Token 也仅有 15 分钟有效期，缩短了可利用窗口
- **刷新时强制查库验证用户状态**：每次刷新都从 DB 重新加载用户 enabled 状态，若用户已被禁用立即拒绝刷新
- **异常刷新检测**：在 `AuthServiceImpl.refresh()` 方法中增加刷新频率检测逻辑。定义时间窗口为 5 秒、阈值为 2 次：若同一 `userId` 在 5 秒内出现 2 次以上刷新请求（暗示旧 Refresh Token 被重复使用），触发安全告警。检测逻辑使用 `ConcurrentHashMap<Long, Deque<Long>>` 维护每个用户的刷新时间戳滑动窗口，窗口过期条目惰性清除。告警方式为 `log.warn("Suspicious refresh pattern detected for userId: {}, {} refreshes in {} seconds", userId, count, window)` 输出安全日志，对接业务监控系统（如 Prometheus + AlertManager）消费此日志。Phase 2 配合 Redis 黑名单后此检测可作为辅助保留或移除
- **用户可主动撤销**：User 实体新增 `tokenVersion` 字段（Integer，初始值 0），Refresh Token claims 中携带此版本号。用户修改密码后 `tokenVersion` 递增（+1）。刷新时 JwtTokenProvider 将 Refresh Token claims 中的版本号与 DB 中当前值比对，不一致则拒绝刷新并强制前端清除 token。此机制确保密码变更后旧 Refresh Token 即时失效

这些限制将在 Phase 2 引入 Redis 黑名单后解除。届时 Refresh Token 的黑名单检查将提供完整的安全保障。

### 4.3 密码策略

#### 复杂度要求

密码校验规则（由 `PasswordPolicy` 接口封装，`AuthServiceImpl` 在注册/修改密码时调用）：

| 规则 | 要求 | 错误码 |
|------|------|--------|
| 最小长度 | 8 字符 | PASSWORD_TOO_SHORT |
| 最大长度 | 64 字符 | PASSWORD_TOO_LONG |
| 字符种类 | 至少包含大写字母、小写字母、数字、特殊字符中的 **3 种** | PASSWORD_WEAK |
| 用户名包含 | 密码不得包含用户名（大小写不敏感） | PASSWORD_CONTAINS_USERNAME |
| 常见密码 | 不得为 Top 10000 常见密码（Phase 2 启用，Phase 1 中不可达） | PASSWORD_COMMON |

#### 加密

使用 BCryptPasswordEncoder（Strength 10），Spring Security 标准实现。

#### NOT NULL 约束状态确认

| 实体 | 字段 | Java `@Column(nullable=false)` | Java 默认值 | DDL 状态 |
|------|------|-------------------------------|------------|---------|
| User | password | 已存在 | N/A（字符串非默认值） | schema.sql `NOT NULL` |
| User | enabled | 已存在 | `= true` | schema.sql `DEFAULT 1`（缺 NOT NULL，需补加） |
| User | passwordChangeRequired | 新增 `@Column(nullable=false, columnDefinition="BIT(1) DEFAULT 0")` | `= false` | schema.sql `NOT NULL DEFAULT 0` |
| User | tokenVersion | 新增 `@Column(nullable=false) private Integer tokenVersion = 0;` | `= 0` | schema.sql `NOT NULL DEFAULT 0` |
| Role | enabled | 缺失 | `= true` | schema.sql `DEFAULT 1`（缺 NOT NULL，需补加） |
| Post | enabled | 缺失 | `= true` | schema.sql `DEFAULT 1`（缺 NOT NULL，需补加） |
| Function | enabled | 已存在 | `= true` | schema.sql `DEFAULT 1`（缺 NOT NULL，需补加） |
| Function | visible | 已存在 | `= true` | schema.sql `DEFAULT 1`（缺 NOT NULL，需补加） |

**种子数据迁移策略**：Phase 1 新增复杂度规则（最小长度 8、3/4 字符种类、不得包含用户名）对已存在的种子数据密码构成约束。处理策略如下：(a) 所有种子用户的初始密码（如 `123456`）需更新为满足新规则的密码（建议使用 `Admin@123` 或等长随机密码）；(b) 更新后的种子密码通过 `passwordEncoder.encode()` 加密后写入 `schema.sql` 的种子数据 INSERT 语句；(c) 种子用户统一设置 `passwordChangeRequired = true`，强制首次登录修改密码；(d) 此迁移在 Phase 1 DDL 变更脚本中一并执行，与数据库表结构调整同步上线。

**修复方案**（全部为 DDL 变更，JPA 注解已有部分就位）：
- Role.java：补加 `@Column(nullable=false)` 到 enabled 字段 → 待修复
- Role.java：新增 `sort` 字段（`private Integer sort;`），用于角色优先级排序 → 新字段
- Post.java：补加 `@Column(nullable=false)` 到 enabled 字段 → 待修复
- User.java：新增 `passwordChangeRequired` 字段，`@Column(nullable=false, columnDefinition="BIT(1) DEFAULT 0") private Boolean passwordChangeRequired = false;`
- schema.sql：所有 `enabled` 和 `visible` 列由 `DEFAULT 1` 改为 `NOT NULL DEFAULT 1`
- schema.sql：`sys_user.passwordChangeRequired` 新增列 `NOT NULL DEFAULT 0`
- schema.sql：`sys_role.sort` 新增列 `INT DEFAULT 0`
- schema.sql：`sys_user.nickname` 由 `DEFAULT NULL` 改为 `NOT NULL`（对齐 User.java `@Column(nullable=false)`）
- schema.sql：`sys_function.visible` 由 `DEFAULT 1` 改为 `NOT NULL DEFAULT 1`（对齐 Function.java `@Column(nullable=false)`）
- schema.sql：更新种子数据密码为满足新复杂度规则的加密密码，同步设置 `passwordChangeRequired = true`

### 4.4 API 端点保护清单

| 端点 | HTTP | 认证要求 | 限流 | 其他 |
|------|------|---------|------|------|
| `/api/auth/login` | POST | 匿名（permitAll） | IP 5次/10秒 + 用户 5次/15分钟 | — |
| `/api/auth/logout` | POST | JWT（authenticated） | 无 | 黑名单记录（仅 Access Token jti）；可选请求体携带 refreshToken（@RequestBody(required=false)） |
| `/api/auth/refresh` | POST | 匿名（permitAll） | 无 | Refresh Token 自验证 + 轮换 |
| `/api/auth/me` | GET | JWT（authenticated） | 无 | 查库验证 enabled |
| `/api/auth/profile` | PUT | JWT（authenticated） | 无 | — |
| `/api/auth/password` | PUT | JWT（authenticated） | 无 | PasswordChangeRequest + 旧密码校验 |
| `/api/menu/tree` | GET | JWT（authenticated） | 无 | — |
| `/api/menu/**` | GET/POST/PUT/DELETE | JWT（authenticated） | 无 | ADMIN 角色权限 |
| `/actuator/health` | GET | 匿名（permitAll） | 无 | — |
| `/actuator/info` | GET | 匿名（permitAll） | 无 | — |
| `/actuator/**` | GET | 拒绝（denyAll） | — | Phase 1 禁用非 health/info 端点 |
| `/swagger-ui/**` | GET | 拒绝（denyAll） | — | Phase 1 生产环境禁用 |
| `/v3/api-docs/**` | GET | 拒绝（denyAll） | — | Phase 1 生产环境禁用 |

**说明**：
- `/api/auth/refresh` 在 SecurityConfig 和保护清单中统一为 `permitAll`（匿名）。Refresh 端点的认证逻辑由 Refresh Token 自身携带的签名和 jti 校验完成，不依赖 Spring Security 的 JWT Filter 前置认证
- `passwordChangeRequired` 阻断：当用户 `passwordChangeRequired = true` 时，除 `/api/auth/password` 和 `/api/auth/logout` 外的所有 API 请求在 PasswordChangeCheckFilter 层返回 403，不再执行 `chain.doFilter()`
- `/api/auth/logout` 请求体可选携带 `refreshToken` 字段，格式同 `RefreshTokenRequest`。Phase 1 中仅记录日志不加入黑名单；Phase 2 Redis 方案启用后加入黑名单

### 4.5 CSRF 策略

Phase 1 延续 `csrf.disable()`。JWT Bearer Token 通过 `Authorization` header（非 cookie）传递，天然不受 CSRF 攻击影响。但在代码中显式注释说明：Phase 2 若迁移到 httpOnly cookie 方案，必须在 SecurityConfig 中启用 CSRF 保护（`csrf(Customizer.withDefaults())`）。

### 4.6 CORS 配置策略

Phase 1 开发阶段使用 `cors(Customizer.withDefaults())` 作为开发默认配置（允许同源请求和常见开发跨域场景）。

**生产环境约束**：生产环境必须通过 `CorsConfigurationSource` bean 显式配置允许的来源（origin）白名单，不得依赖默认值。推荐配置：

```
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(Arrays.asList("https://admin.aimedical.com", "https://doctor.aimedical.com"));
    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

在 `SecurityConfigPhase1` 中通过 `@Profile("prod")` 条件配置此 bean，或以 `application-prod.yml` 中的配置属性驱动。开发环境保留 `cors(Customizer.withDefaults())` 的灵活性。

### 4.7 JWT 密钥配置与约束

JWT 签名密钥通过 `JWT_SECRET` 环境变量（或 `jwt.secret` 配置属性）注入，`JwtConfig` 类绑定。约束如下：

| 约束项 | 要求 | 说明 |
|--------|------|------|
| 最小长度 | HMAC-SHA256 需要至少 256 位密钥（32 字节） | 建议使用 64 字节以上以提供足够熵 |
| 合法字符集 | Base64 URL-safe 字符集（A-Z a-z 0-9 - _） | 避免 YAML/JSON 转义问题 |
| 生成方式 | `openssl rand -base64 48` 或等价的随机生成器 | 禁止手动输入、禁止使用常见字符串 |
| 启动验证 | `@PostConstruct` 方法验证密钥长度和字符集合法性 | 不满足时抛出 IllegalStateException，阻止应用启动 |
| 运行期不可变 | `JwtTokenProvider` 中 `SecretKey` 由 `@PostConstruct` 计算一次并缓存 | 运行期修改不生效 |

**Bean 注册说明**：`JwtTokenProvider` 标注 `@Component` 注册为 Spring Bean，确保 `@PostConstruct` 启动验证由 Spring 容器自动触发。也可通过 `AuthModuleConfig` 中显式 `@Bean` 方法注册，两种方式均可。

**application.yml 配置示例**：

yaml
jwt:
  secret: ${JWT_SECRET}          # 必须设置，无默认值
  access-token-expiration: 900   # 15 分钟
  refresh-token-expiration: 604800  # 7 天

**启动验证逻辑**：`JwtTokenProvider` 在 `@PostConstruct` 中对 `jwtConfig.getSecret()` 执行以下验证：
1. 检查 secret 是否为 null 或空字符串 → 抛出 IllegalStateException("JWT_SECRET must be configured")
2. 检查 Base64 解码后的字节长度 ≥ 32 → 不满足则抛出 IllegalStateException("JWT_SECRET must be at least 256 bits (32 bytes) after Base64 decoding")
3. 检查 secret 是否仅包含 Base64 URL-safe 字符 → 不满足则抛出 IllegalStateException("JWT_SECRET contains invalid characters")

## 5. 数据模型

### 5.1 实体变更（包 A 修复）

#### User.java

| 变更 | 当前状态 | 修复方案 |
|------|---------|---------|
| password NOT NULL | 代码已有 `@Column(nullable=false)` + schema.sql `NOT NULL` | 已完成 |
| nickname NOT NULL | 代码已有 `@Column(nullable=false)` | schema.sql `DEFAULT NULL` 改为 `NOT NULL`（对齐 User.java 注解） |
| enabled 默认值 + NOT NULL | 代码已有 `@Column(nullable=false)` + `= true` | schema.sql 补加 `NOT NULL DEFAULT 1` |
| passwordChangeRequired 新增字段 | 无此字段 | 新增 `@Column(nullable=false, columnDefinition="BIT(1) DEFAULT 0") private Boolean passwordChangeRequired = false;` + schema.sql `NOT NULL DEFAULT 0` |
| tokenVersion 新增字段 | 无此字段 | 新增 `@Column(nullable=false) private Integer tokenVersion = 0;` 字段 + schema.sql `sys_user.token_version INT NOT NULL DEFAULT 0`；用于密码变更后撤销旧 Refresh Token |
| enabled JPA 注解 | 已有 | 已完成 |
| 继承 BaseEntity | deleted 列已由 BaseEntity 提供 | schema.sql 中 deleted 已为 `NOT NULL DEFAULT 0`，已完成 |

#### Role.java

| 变更 | 当前状态 | 修复方案 |
|------|---------|---------|
| enabled 默认值 | `private Boolean enabled = true;` | 已完成 |
| enabled NOT NULL | 缺 `@Column(nullable=false)` | 待修复：补加注解 + schema.sql `NOT NULL DEFAULT 1` |
| sort 新增字段 | 无此字段 | 新增 `private Integer sort;` 字段，用于角色优先级排序；schema.sql `sys_role.sort` 新增列 `INT DEFAULT 0` |

#### Post.java

| 变更 | 当前状态 | 修复方案 |
|------|---------|---------|
| enabled 默认值 | `private Boolean enabled = true;` | 已完成 |
| enabled NOT NULL | 缺 `@Column(nullable=false)` | 待修复：补加注解 + schema.sql `NOT NULL DEFAULT 1` |

#### Function.java → PermissionFunction.java

| 变更 | 当前状态 | 修复方案 |
|------|---------|---------|
| 类名冲突 | `Function` 与 `java.util.function.Function` 冲突 | 重命名为 `PermissionFunction`；所有引用处同步更新（关联表名 `sys_function` 不变，仅 Java 类名变更） |
| enabled 默认值 + NOT NULL | 代码已有 `@Column(nullable=false)` + `= true` | 已完成 |
| visible 默认值 + NOT NULL | 代码已有 `@Column(nullable=false)` + `= true` | schema.sql 补加 `NOT NULL DEFAULT 1` |

#### UserRepository.java

| 变更 | 当前状态 | 修复方案 |
|------|---------|---------|
| 返回类型 | `User findByUsername(String username)` | `Optional<User> findByUsername(String username)` |

### 5.2 新增实体与 DTO（包 B）

本阶段不新增 JPA 实体（除 User.passwordChangeRequired 字段扩展外）。认证所需的状态管理（黑名单、登录计数、密码变更标记）使用内存数据结构，不持久化到数据库。Phase 2 引入 `sys_token_blacklist` 表和 Redis 时再新增实体。

#### LoginRequest (Java 17 record)

java
public record LoginRequest(
    @NotBlank String username,
    @NotBlank @Size(min = 1, max = 64) String password
) {}


#### LoginResponse (Java 17 record)

`expiresIn` 字段语义：access token 剩余有效秒数（从签发时计算），供前端用于预估下一次刷新时机。典型值为 900（15 分钟）。

java
public record LoginResponse(
    Long userId,
    String username,
    String accessToken,
    String refreshToken,
    String tokenType,           // "Bearer"
    long expiresIn,             // access token 剩余有效秒数（从签发时计算），供前端预估刷新时机
    boolean passwordChangeRequired,  // 首次登录或管理员标记密码过期时为 true
    UserInfoResponse user       // 始终返回，不因 passwordChangeRequired 而 null
) {}


#### RefreshTokenRequest (Java 17 record)

**使用说明**：本 record 同时用于 Refresh 和登出两个端点。Refresh 端点要求 `@NotBlank`（必选）；登出端点使用 `@RequestBody(required=false)` 接收，请求体可选携带，允许 null。Controller 层在登出端点使用 `@RequestBody(required=false)` 注解，null 表示未携带 refreshToken。

java
public record RefreshTokenRequest(
    @NotBlank String refreshToken
) {}


#### TokenRefreshResponse (Java 17 record)

`expiresIn` 字段语义：新签发的 access token 剩余有效秒数（从签发时计算），与 LoginResponse 中 expiresIn 语义一致。典型值为 900（15 分钟）。

java
public record TokenRefreshResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn
) {}


#### UserInfoResponse (Java 17 record)

前后端字段名对齐。前端 `UserInfo` 接口使用 `realName` 和 `role` 字段名，后端 DTO 对齐为相同的字段名：

java
public record UserInfoResponse(
    Long id,
    String username,
    String realName,
    String phone,
    String email,
    String role,
    String position,
    Set<String> permissions
) {}


**字段映射说明**：
- `realName` 对应后端 `User.nickname` 字段（数据库列 `nickname`），在 UserConverter 中转换时映射
- `role` 对应前端角色标识（取用户主角色的 code）。主角色判定策略：按角色优先级（Role 实体新增的 `sort` 字段，值越小优先级越高）排序取最高者；无角色时返回空字符串。不再引用不存在的 `primaryRole` 字段
- `position` 对应后端用户当前岗位的 code

#### PasswordChangeRequest (Java 17 record)

java
public record PasswordChangeRequest(
    @NotBlank String oldPassword,
    @NotBlank @Size(min = 8, max = 64) String newPassword
) {}


#### ProfileUpdateRequest (Java 17 record)

java
public record ProfileUpdateRequest(
    @NotBlank(message = "昵称不能为空") @Size(max = 50) String nickname,
    @Pattern(regexp = "^1[3-9]\d{9}$") String phone,  // 手机号格式校验
    @Email @Size(max = 100) String email
) {}


#### MenuCreateRequest (Java 17 record)

java
public record MenuCreateRequest(
    @NotBlank String name,
    @NotBlank String permission,
    Long parentId,
    String path,
    String component,
    String icon,
    Integer sort,
    @NotNull Boolean visible
) {}


#### MenuUpdateRequest (Java 17 record)

java
public record MenuUpdateRequest(
    Long id,           // 可选，请求体中携带时用于一致性校验（与路径 {id} 比对）
    String name,
    String permission,
    Long parentId,
    String path,
    String component,
    String icon,
    Integer sort,
    Boolean visible
) {}


**更新语义**：采用局部更新语义（RFC 7231 §4.3.4 PATCH），请求体中省略的字段保持不变，不覆盖为空值。**一致性校验**：`PATCH /api/menu/{id}` 的路径参数 `{id}` 与请求体中的 `id` 字段（若携带）必须相同。Controller 层在校验逻辑中：若请求体携带了 id，则比对两者是否一致，不一致时返回 400（PARAM_INVALID）；若请求体未携带 id，则跳过一致性校验。此设计与 PATCH 语义一致——id 不是被更新的字段，仅为可选的校验信息。


#### MenuResponse (Java 17 record)

java
public record MenuResponse(
    Long id,
    String name,
    String path,
    String component,
    String icon,
    String permission,
    Integer sort,
    List<MenuResponse> children
) {}


**字段对齐**：
- `permission` 对应后端的 permission code（前端用于路由 name 生成）
- `children` 递归结构，支持多级菜单树；叶子节点 children 为 `null` 或空列表
- `sort` 用于同级排序，值越小越靠前


## 6. API 接口设计

### 6.1 接口清单

| 方法 | 路径 | 请求体/参数 | 成功响应码 | 说明 |
|------|------|------------|-----------|------|
| POST | `/api/auth/login` | LoginRequest | 200 | 登录，返回 access + refresh token + passwordChangeRequired |
| POST | `/api/auth/logout` | RefreshTokenRequest（可选，@RequestBody(required=false)） | 200 | 登出，将 Access Token jti 加入黑名单；可选携带 refreshToken |
| POST | `/api/auth/refresh` | RefreshTokenRequest | 200 | 刷新 token，旧 refresh token 轮换失效 |
| GET | `/api/auth/me` | — | 200 | 获取当前用户信息（含权限列表） |
| PUT | `/api/auth/profile` | ProfileUpdateRequest | 200 | 更新个人资料（nickname/phone/email） |
| PUT | `/api/auth/password` | PasswordChangeRequest | 200 | 修改密码（需旧密码验证 + 新密码复杂度校验） |
| GET | `/api/menu/tree` | — | 200 | 获取当前用户菜单树 |
| GET | `/api/menu/all` | — | 200 | 获取所有菜单（ADMIN） |
| POST | `/api/menu` | MenuCreateRequest | 200 | 创建菜单（ADMIN） |
| PATCH | `/api/menu/{id}` | MenuUpdateRequest | 200 | 局部更新菜单（ADMIN，PATCH 语义） |
| DELETE | `/api/menu/{id}` | — | 200 | 删除菜单（ADMIN） |

### 6.2 登录响应格式

json
{
  "code": "SUCCESS",
  "message": "登录成功",
  "data": {
    "userId": 1,
    "username": "doctor001",
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",             // Bearer 类型标识
    "expiresIn": 900,                  // access token 剩余有效秒数（从签发时计算），供前端预估刷新时机
    "passwordChangeRequired": false,
    "user": {
      "id": 1,
      "username": "doctor001",
      "realName": "张医生",
      "phone": "13800138000",
      "email": "doctor@example.com",
      "role": "DOCTOR",
      "position": "OUTPATIENT",
      "permissions": ["registration:view", "prescription:create"]
    }
  }
}


### 6.3 错误响应格式（统一）

json
{
  "code": "UNAUTHORIZED",
  "message": "未认证或令牌已失效",
  "data": null
}


所有 401（未认证）、403（无权限）、429（限流）等错误均使用此统一格式，由 `GlobalExceptionHandler` 和 `AuthenticationEntryPoint` 共同保证响应结构一致。

### 6.4 Breaking Change 声明

相对于 Phase 0 的登录响应，Phase 1 有以下不向后兼容的变更：

| 变更项 | Phase 0 | Phase 1 | 说明 |
|--------|---------|---------|------|
| `token` 字段名 | `token` | `accessToken` | 明确区分 access/refresh，前端 `LoginResponse` 接口需同步更新 |
| 新增 `refreshToken` 字段 | 无 | `refreshToken` | Token 刷新所需，前端需存储 |
| 新增 `passwordChangeRequired` 字段 | 无 | `passwordChangeRequired` | 前端需检测此标记并跳转修改密码页 |
| 新增 `userId` / `username` 顶层字段 | 无 | `userId` (Long), `username` (String) | 提供快速身份标识 |
| `user` 对象字段名 | `nickname` / `userType` | `realName` / `role` | 对齐前端 `UserInfo` 接口字段名 |
| `TokenRefreshResponse` 字段变更 | `token`（旧刷新响应） | `accessToken`（新）；移除 `user` 字段 | 刷新端点响应字段名与登录响应对齐；刷新后前端需调用 `GET /api/auth/me` 获取最新用户信息 |

## 7. 包 A/B/D 协作关系

### 7.1 包 A → 包 B 依赖

包 A 提供的数据实体（User、Role、Post、PermissionFunction）是包 B 认证功能的基石：

```
包 A 实体层 (permission/)               包 B 认证层 (auth/)
  User.java ─────────────────────────> AuthServiceImpl.login()
    ├── username（登录识别）                  ├── 查询用户 → 校验密码
    ├── password（BCrypt 加密存储）            └── 校验 enabled/deleted 状态
    ├── enabled（账户启用状态）
    ├── passwordChangeRequired（密码变更标记） ──> PasswordChangeCheckFilter
    └── deleted（软删除标记）

  User.java.roles (Set<Role>) ──────> JwtAuthenticationFilter
  User.java.posts (Set<Post>) ──────>   ├── 从 DB 加载角色和岗位
  Role.java ─────────────────────────>   ├── 聚合 Function 权限码
  Post.java.functions ──────────────>   └── 装配 SecurityContext authorities
  PermissionFunction.java ──────────>

  UserRepository ──────────────────> AuthServiceImpl, JwtAuthenticationFilter,
                                       PasswordChangeCheckFilter, MenuServiceImpl
    ├── findByUsername() ────────────── 登录查询 (带 @EntityGraph 显式 fetch roles/posts)
    ├── findById() ──────────────────── 每次请求查库验证 enabled
    └── 级联查询 ────────────────────── 权限收集 (使用 @EntityGraph 避免 N+1)
```

> **脚注：查询优化** — `UserRepository` 中涉及 roles/posts 级联查询的方法（包括 `findByUsername` 和 `findById`）应使用 `@EntityGraph(attributePaths = {"roles", "posts"})` 显式控制 fetch join，避免 N+1 查询问题。此优化已在 8.1 节 M9 中确认。

**依赖规则**：包 B 作为 common-module-impl 的内部模块，直接使用同模块 `permission/` 包下的实体和 Repository。这是同模块内部依赖，不构成跨模块耦合。

### 7.2 包 B → 包 D 契约

包 B 的后端 API 是包 D 前端消费的数据来源。前后端契约在以下方面对齐：

| 契约项 | 后端（包 B） | 前端（包 D） |
|--------|------------|------------|
| 认证头格式 | `Authorization: Bearer <token>` | axios 拦截器自动设置 |
| Token 存储 | 不存储（无状态） | localStorage（Phase 1） |
| 401 响应格式 | `{ code: "UNAUTHORIZED", ... }` | axios 拦截器识别 401 → 静默刷新 |
| 刷新触发时机 | 后端验证 token 过期返回 401 | 前端 401 拦截器 → 刷新 → 重放请求 |
| 刷新请求体 | `RefreshTokenRequest{ refreshToken }` | POST body 中携带 refreshToken |
| 响应体结构 | `Result<T>` 泛型 | `apiGet<T>` / `apiPost<T>` 泛型封装 |
| 菜单树结构 | `List<MenuResponse>`（递归 children） | `convertMenusToRoutes`（递归展平版本） |
| 用户信息 | `UserInfoResponse{ realName, role }` | `UserInfo{ realName, role }`（字段名已对齐） |
| 登录响应字段 | `accessToken`, `refreshToken`, `passwordChangeRequired` | `LoginResponse{ accessToken, refreshToken, ... }`（字段名已对齐） |

**包 B 对包 D 前端的向下兼容承诺**：
- 新增 API 端点不修改现有端点语义
- 响应体中现有字段不删除、不改变类型
- 错误码枚举值一经发布不修改含义
- Phase 1 的字段名变更（token→accessToken, nickname→realName, userType→role）作为 Breaking Change 声明，前端需同步修改 `UserInfo` 和 `LoginResponse` 接口定义

### 7.3 包 A 实体变更对包 B 认证逻辑的影响

| 包 A 变更 | 对包 B 的影响 | 处理策略 |
|-----------|-------------|---------|
| User.password NOT NULL | 无运行时影响（种子数据已有密码） | 确保 DDL 变更前已清理 NULL 数据 |
| User.enabled Java 默认值 + NOT NULL | 已完成（代码已有） | — |
| User.passwordChangeRequired 新增字段 | 新增 PasswordChangeCheckFilter，检查此字段决定是否阻断请求 | 详见 3.3 节 PasswordChangeCheckFilter 行为契约 |
| Role.enabled 补加 `@Column(nullable=false)` | 无运行时影响 | 同步更新 schema.sql |
| Post.enabled 补加 `@Column(nullable=false)` | 无运行时影响 | 同步更新 schema.sql |
| PermissionFunction 类名变更 | 所有引用 `Function.java` 的文件需同步更新 | 修改包 B 中所有 import 和引用 |
| PermissionFunction.visible NOT NULL | 已完成（代码已有） | schema.sql 补加 NOT NULL |
| schema.sql deleted NOT NULL | 16 张表 ALTER TABLE 需在 Phase 1 上线前执行 | 数据库迁移脚本按报告中的步骤执行 |
| UserRepository 返回 Optional | AuthServiceImpl.login() 调用处理方式变更 | 从 null 检查改为 Optional.map/orElseThrow |
| User.nickname → realName 字段映射 | UserConverter 中需做字段名转换 | 输出 DTO 时 nickname 映射为 realName |
| UserType → role 字段映射 | 取用户主角色 code 填充 role 字段 | 按角色优先级（Role 实体新增的 `sort` 字段，值越小优先级越高）排序取最高者；无角色时返回空字符串。不依赖 `primaryRole` 字段 |

### 7.4 包 D 前端对包 B 的补偿机制

由于包 B 的 Token 黑名单在 Phase 1 使用内存实现且仅黑名单 Access Token（不含 Refresh Token），面临多实例不一致和 Refresh Token 泄露风险。包 D 前端需配合以下补偿策略：

- 登出时即使后端返回失败（网络原因），前端 `finally` 块仍清除本地 token
- 401 静默刷新连续 3 次失败后清除所有认证数据并跳转登录页（防死循环）
- Token 刷新成功后，前端立即调用 `GET /api/auth/me` 获取最新用户信息（角色、权限等），确保本地用户状态与 DB 一致（因 `TokenRefreshResponse` 不再包含 user 字段）
- 用户信息不在前端持久化权限决策（菜单渲染依赖 `GET /api/menu/tree`，不从 localStorage 读取 `permissions`）
- 本地存储的用户信息仅保留最小标识（userId + username），不存储 role/permissions 等权限敏感数据
- 多 Tab 并发刷新互斥：刷新逻辑使用 Promise 单例模式，确保同一时间仅执行一次刷新请求。后续并发 401 等待同一 Promise 完成，避免重复刷新导致 token 轮换竞争
- 401 静默刷新失败计数器采用全局共享策略：按 60 秒时间窗口统计单次 Promise 的失败次数，而非按每个等待请求独立计数。同一时间窗口内无论并发等待请求数量多少，失败次数仅记一次。60 秒后计数器重置
- axios 响应拦截器识别 `PASSWORD_CHANGE_REQUIRED` 错误码：HTTP 403 + ErrorCode.PASSWORD_CHANGE_REQUIRED 时，重定向到密码修改页面并终止原始请求（不触发 401 刷新逻辑）。此处理规则优先级高于 401 静默刷新，防止进入刷新循环
- 密码修改成功后恢复流程：`PUT /api/auth/password` 成功 → 清除 `passwordChangeRequired` 标记 → 调用 `GET /api/auth/me` 刷新用户信息 → 调用 `GET /api/menu/tree` 获取最新菜单 → 跳转到系统首页。**异常场景处理**：`GET /api/auth/me` 或 `GET /api/menu/tree` 任一请求失败时，前端应记录错误日志并重试最多 2 次（指数退避：1 秒、3 秒）。重试耗尽后仍失败，降级处理——清除 `passwordChangeRequired` 本地标记，跳转到首页，后续页面加载时自动重试获取菜单。此策略确保密码变更流程不因中间步骤的网络波动而阻塞。

### 7.5 前后端重复问题合并说明

原审查报告中，"用户信息明文存 localStorage" 问题同时出现在后端 B 包和前端 D 包的独立条目中。此问题统一归入包 D 前端补偿机制（7.4 节），后端仅负责不返回不必要的信息，前端的存储策略是实际整改方。不在后端和前端两处重复记录。

## 8. 审查问题修复方案汇总

### 8.1 包 B 后端问题

| # | 问题 | 当前状态 | 修复方案 | 潜在副作用 | 影响范围 |
|---|------|---------|---------|-----------|---------|
| C1 | 用户禁用后 token 仍然有效 | Filter 和 Service 未检查 enabled | JwtAuthenticationFilter 每次请求从 DB 加载用户并检查 enabled + deleted | 每次认证请求增加一次 DB 查询 | JwtAuthenticationFilter |
| C2 | 登录无防暴力破解 | 无速率限制 | 三层防护：IP 限流 + 失败计数 + 账户锁定 | 误锁定合法用户（需配置合理阈值） | AuthServiceImpl, 新增 RateLimitGuard, LoginAttemptTracker |
| C3 | userId 提取逻辑重复 4 次 | Claims Integer→Long 兼容代码重复 | 抽取 `JwtTokenProvider.getUserIdFromClaims(Claims)` 静态方法 | 无 | JwtTokenProvider |
| C4 | 登出 No-Op | logout() 为空方法 | 引入 TokenBlacklist，登出时将 Access Token jti 加入黑名单 | 内存占用可控（仅 Access Token，~6.5MB） | AuthServiceImpl, InMemoryTokenBlacklist |
| C5 | Access Token 自刷新无轮换 | 用 access token 自身刷新 | 引入 Refresh Token + 轮换机制 | 刷新流程变复杂，需前后端同步改造 | JwtTokenProvider, AuthServiceImpl, 前端 auth store |
| H1 | findByUsername 返回非 Optional | 返回 User | 改为 `Optional<User>` | 编译期影响所有调用方 | UserRepository, AuthServiceImpl |
| H2 | JWT SecretKey 未缓存 | 每次构造 | `@PostConstruct` 缓存 SecretKey 字段 | 无 | JwtTokenProvider |
| H8 | 密码复杂度弱 | 仅 `@Size(min=6)` | 新增 PasswordPolicy 校验（3/4 字符种类 + 8-64 长度） | 已有弱密码用户修改密码时需适应性变更 | AuthServiceImpl, PasswordPolicy |
| H11 | ProfileUpdateRequest 无手机号校验 | 仅限制最大长度 | 添加 `@Pattern(regexp = "^1[3-9]\d{9}$")` | 已有非 1 开头手机号的用户无法通过校验 | ProfileUpdateRequest |
| H12 | 用户/角色变更后 token 有效 | JWT claims 不可撤销 | Filter 层每次从 DB 加载权限（不再依赖 claims 中的角色） | 每次请求多一次 DB 查询 | JwtAuthenticationFilter |
| M1 | buildUserInfoResponse 私有方法不可测试 | AuthServiceImpl 中 buildUserInfoResponse 为私有方法，无法单独测试 | 提取为包级私有方法或抽取到 UserConverter，通过 UserConverterTest 覆盖 | 无 | UserConverter |
| M2 | updateProfile() 多余 save() 写回 | updateProfile() 中显式调用 userRepository.save() 多余 | 利用 JPA 脏检查机制，仅 set 变更字段后由 Hibernate 自动 flush，移除显式 save() 调用 | 需确认 Spring Data JPA 事务边界内的 flush 行为 | AuthServiceImpl |
| M3 | TokenStore 死代码 | admin/entity/TokenStore.java 未使用 | Phase 1 保留但不注册为 @Entity，Phase 2 启用时恢复 | 无 | 无 |
| M4 | AuthController 依赖 JwtUtil | 直接在 Controller 层提取 token | 引入 CurrentUser 接口，Controller 通过 SecurityContext 获取 | Controller 层需调整 token 提取方式 | AuthController, CurrentUser |
| M5 | 医生岗位取 iterator().next() | 医生岗位通过 posts.iterator().next() 取第一个元素，代码脆弱 | 将 `iterator().next()` 抽取为 `getFirstPost()` 工具方法，使用 `new ArrayList<>(posts).get(0)` 并判空 | 抽取出工具方法后需确保调用方一致性 | Post.java |
| M6 | DTO 未用 record | 传统 POJO | 全部认证相关 DTO 改为 Java 17 record | Jackson 序列化兼容（record 默认 immutable，需确认反序列化行为） | dto/package |
| M7 | Entity 风格混用 | 手写 getter/setter 与 Lombok 混用 | 统一为手写 getter/setter（与 Phase 0 约定一致） | 无 | User, Role, Post, PermissionFunction |
| M8 | Function.java 类名冲突 | Function 类名与 java.util.function.Function 冲突 | 重命名为 PermissionFunction；所有引用处同步更新（关联表名 sys_function 不变，仅 Java 类名变更） | 编译期影响所有 import Function 的文件 | PermissionFunction.java |
| M9 | login() 全量查询 + 懒加载 | 查询完整 User 实体 | 查询时使用 `@EntityGraph(attributePaths = {"roles", "posts"})` 显式控制 fetch join，避免 N+1 | 需在 UserRepository 新增方法 | UserRepository, AuthServiceImpl |
| M10 | Phase0/Phase1 SecurityConfig 并发激活 | 两个 PasswordEncoder bean | Phase 1 的 application.yml profiles 列表移除 `phase0` | 需同步确保 Phase 0 配置类不会影响生产 | application.yml |
| M17 | Actuator 端点暴露 | `/actuator/health` 和 `/actuator/info` 设为 permitAll，其余 `/actuator/**` 已配置 denyAll | 生产环境仅公开 health 和 info 端点，其余 Actuator 端点由 SecurityConfig 通过 denyAll 禁止访问（已在 3.3 节 SecurityConfig 和 4.4 节保护清单中实现） | 无（生产环境已完全关闭非必要端点） | SecurityConfigPhase1 |
| M18 | 401 响应体结构不一致 | JwtAuthenticationFilter 静默跳过不写 response（由 ExceptionTranslationFilter 处理），与 Controller 手动 `Result.fail` 格式不一致 | SecurityConfig 的 AuthenticationEntryPoint 统一处理所有未认证场景：AuthenticationException 携带 ACCOUNT_DISABLED 时返回 `Result.fail("ACCOUNT_DISABLED", "账户已被管理员停用")`，其余未认证场景统一返回 `Result.fail("UNAUTHORIZED", "未认证或令牌已失效")`（已在 3.3 节和 6.3 节中实现） | 需确认 ExceptionTranslationFilter 行为一致性（已在 8.2 节 H10 中验证） | SecurityConfig, JwtAuthenticationFilter |
| M19 | 用户不存在时菜单返回空数组 | MenuServiceImpl 中用户不存在时返回空数组，与 AuthController 返回 UNAUTHORIZED 的行为不一致 | 菜单 API 已要求 JWT 认证（authenticated），JwtAuthenticationFilter 在用户不存在或已删除时静默跳过，由 ExceptionTranslationFilter 返回 401。**行为变更声明**：用户不存在时从"返回空数组"变更为"返回 401 UNAUTHORIZED"，前端需确保菜单请求在 401 时不渲染菜单数据 | 已认证请求中不再有"用户不存在但返回空数组"的场景 | SecurityConfig, JwtAuthenticationFilter, 前端菜单组件 |

### 8.2 包 D 前端问题

**前端代码风格约定**：本项目的 Vue 3 前端统一使用 `<script setup>` 语法（Composition API）进行组件开发，不使用 Options API。H6 修复方案中已移除 `getCurrentInstance()` 示例，推荐使用依赖注入模式（`inject`/`provide`）或 `useRouter()` 获取路由实例，确保与 `<script setup>` 上下文兼容。

| # | 问题 | 当前状态 | 修复方案 | 潜在副作用 | 影响范围 |
|---|------|---------|---------|-----------|---------|
| C6 | logout() 缺少 try-finally | API 抛异常时 clearAuthData 不执行 | 包裹 try-finally，finally 中清除认证数据 | 无 | createAuthStore.logout() |
| C7 | 导航守卫竞态 | 并发 fetchMenus() 导致重复 addRoute | fetchMenus() 内部加互斥锁（fetching 标志 + Promise 队列或 loading 状态互斥） | 增加约 20 行代码 | createMenuStore.fetchMenus() |
| H3 | 401 不触发自动刷新 | 仅 initializeAuth 中刷新 | axios 响应拦截器中添加 401 → refreshToken → retry 逻辑，失败 3 次后清除 | 刷新逻辑复杂度增加，需防无限重试 | api/index.ts, auth store |
| H4 | convertMenusToRoutes 硬编码 'Layout' | 父路由名写死 | 将 Layout 路由名称参数化，通过 createMenuStore 配置参数传入 | 现有调用方需传入参数 | createMenuStore, doctor/admin router |
| H5 | 菜单仅两级 | children 不递归 | convertMenusToRoutes 改为递归展平版本。路由注册策略：递归遍历任意深度菜单树，每个菜单项生成独立展平路由（不嵌套 children），所有展平路由作为一级路由注册到 `router.addRoute()`（不带父级 Layout 嵌套），path 为从根到当前菜单的完整路径（如 `/system/user`）。Layout 包容机制：Layout 布局组件在 App.vue 中通过 `<router-view>` 包裹所有路由的顶级出口，展平后的路由不嵌套 Layout，而是由根路由（path === '/'）的 Layout 通过唯一 `<router-view>` 统一渲染；或展平路由作为 Layout 子路由注册时，在路由元数据中标记 `meta.layout` 以动态选择 Layout。与 `/` 跳过滤条件的兼容关系：根菜单（parentId === null）的 path 展平后以 `/` 开头，不影响首页跳转；path === '/' 的根路由仍单独注册并跳转到 Layout，展平路由不覆盖根路由。name 唯一性保证策略：在 `convertMenusToRoutes` 的递归遍历过程中，使用 `permissionCode` 按驼峰映射（如 `system:user` 映射为 `SystemUser`）时，直接通过添加前端应用前缀（如 `Doctor_SystemUser` 或 `Admin_SystemUser`）消除跨权限冲突，避免运行时按需修正。若仍保留运行时检测，须补充说明"检测到冲突时，拒绝注册而非覆盖" | 展平策略不产生路由层级嵌套；name 冲突需运行时检测补偿 | createMenuStore |
| H6 | 循环依赖脆弱 | 顶层 import router → import store | 保留现有 `createMenuStore(router, dynamicPageComponent)` 工厂模式。`shared/src/stores/menu.ts` 中的基础 store 函数接受 router 参数，app 专用 store 层（`apps/doctor/src/stores/menu.ts` 和 `apps/admin/src/stores/menu.ts`）通过延迟初始化解决循环依赖：在 `createMenuStore` 中将 router 实例作为参数传入，而非在 store 内部顶层 import router | 无（方案已在现有代码中验证可行） | doctor/admin stores/menu.ts |
| H7 | 导航守卫阻塞 UI | await fetchMenus 期间所有导航挂起 | 添加 loading 状态 + 全局 loading 组件（如 NProgress 或骨架屏） | 需引入 UI loading 组件 | router/index.ts |
| H9 | Swagger/API 文档公开暴露 | permitAll | 生产环境 `springdoc.api-docs.enabled=false` + SecurityConfig 拒绝（denyAll） | 开发环境需单独配置 | SecurityConfigPhase1, application-prod.yml |
| H10 | JWT Filter 静默跳过无效 token | 不返回 401 | 维持静默跳过策略（由 ExceptionTranslationFilter 处理），为 /api/auth/refresh 等 permitAll 端点保留无 token 访问能力 | 需确认 ExceptionTranslationFilter 行为一致性 | JwtAuthenticationFilter |
| M11 | activeMenu 状态冗余 | 同时依赖 activeMenu 和 route.path | 删除 activeMenu 状态，完全依赖 `useRoute().path` 判断激活菜单 | SidebarBase 重构 | createMenuStore, SidebarBase |
| M12_P10 | SidebarBase 直接用 useRoute() | 与注入 routerInstance 模式不一致 | SidebarBase 通过 props 接收当前路径（推荐 props 定义：`currentPath: { type: String, required: true }`），由父组件 LayoutBase 通过 `useRoute().path` 传入 | 父组件 LayoutBase 需修改模板传参 | SidebarBase, LayoutBase |
| M13 | apiGet catch 类型不安全 | `error as BusinessError` | 运行时类型收窄：先 `instanceof` 或 `isBusinessError()` 检查，再断言 | 多一层检查开销（可忽略） | api/index.ts |
| M14 | BusinessError.isBusinessError 可选 | 编译期无法强制校验 | 改为必选属性 `isBusinessError: true` | 无 | types/index.ts |
| M15 | 无前端输入校验 | LoginBase 无校验 | 在 LoginBase 中添加基础校验（username 非空、password 最小长度 6） | 与后端校验重复但安全 | LoginBase.vue |
| M16 | 用户信息明文存 localStorage | role/permissions 暴露 | 存储最小标识（userId + username），权限数据通过 API 获取 | 需调整 auth store 和 menu store 的初始化流程 | createAuthStore |

### 8.3 包 A 数据建模问题

| # | 问题 | 当前状态 | 修复方案 | 潜在副作用 | 影响范围 |
|---|------|---------|---------|-----------|---------|
| A1 | password 无 NOT NULL | 代码已有 `@Column(nullable=false)` + DDL NOT NULL | 已完成 | 无（DDL 已就位） | User.java, schema.sql |
| A2 | deleted 列 NOT NULL 不一致 | 16 张表 DDL 已全部为 `NOT NULL DEFAULT 0`，已完成 | schema.sql 已全部修正为 `NOT NULL DEFAULT 0` | 已在 schema.sql 中完成，无需额外修复 | schema.sql |
| A3 | enabled/visible 无 Java 默认值 | User/Function 已有 `=true`；Role/Post 已有 `=true` | 已完成（Java 默认值已就位），仅需补加 Role、Post 的 `@Column(nullable=false)` + schema.sql 全部 enabled/visible 列补 `NOT NULL` | 需同步更新 schema.sql 确保与 Java 注解一致 | Role.java, Post.java, schema.sql |
| A4 | 缺少集成测试 | EntityMappingIT 未覆盖 User/Role/Post | 新增三组集成测试，参考 `Harness/reviews/202606252200_phase1_pkgBD_review/report_integration_security.md` 中的集成测试模板 | 无 | EntityMappingIT.java |

## 9. 并发设计

### 9.1 Token 黑名单并发安全

`InMemoryTokenBlacklist` 使用 `ConcurrentHashMap<String, Long>`，所有操作（add/containsKey）均为线程安全。定时清理任务使用 `ScheduledExecutorService`，在单线程执行器中串行执行，不会与业务线程产生并发冲突。

### 9.2 速率限流并发安全

`InMemoryRateLimitGuard` 的滑动窗口算法使用 `ReentrantLock` 保护窗口内的排序集合。选择 `ReentrantLock` 而非 `synchronized` 的理由：`ReentrantLock` 支持公平锁特性（防止窗口清理线程被业务线程长时间抢占）和 `tryLock` 超时机制（限流检查可在指定时间内失败而非无限等待）。每个 IP 的窗口对象独立锁，细粒度锁减少竞争。限流检查仅发生在登录请求，并发量低，锁竞争可忽略。

### 9.3 登录失败并发安全

`LoginAttemptTracker` 使用 `ConcurrentHashMap`，单个用户名的失败计数更新通过 `compute` 方法原子性操作。锁定状态的检查和更新在同一个 `compute` 闭包中完成，确保线程安全。

## 10. 错误处理策略

### 10.1 错误分类

| 类别 | 处理方式 | 响应码 | HTTP 状态码 |
|------|---------|-------|------------|
| 认证失败（无效 token、过期、黑名单） | 静默跳过 → ExceptionTranslationFilter | UNAUTHORIZED | 401 |
| 已认证用户被禁用 | JwtAuthenticationFilter 抛出 AuthenticationException | ACCOUNT_DISABLED | 401 |
| 授权失败（无权限） | 抛出 AccessDeniedException | FORBIDDEN | 403 |
| 密码变更检查阻断 | PasswordChangeCheckFilter 抛出 PasswordChangeRequiredException → AccessDeniedHandler 捕获 | PASSWORD_CHANGE_REQUIRED | 403 |
| 账户锁定 | LoginAttemptTracker 检测锁定状态 | ACCOUNT_LOCKED | 429 |
| 令牌刷新失败 | Refresh Token 验证失败 | TOKEN_REFRESH_FAILED | 401 |
| 参数校验失败 | @Valid 触发 MethodArgumentNotValidException | PARAM_INVALID | 400 |
| 密码校验失败（旧密码不匹配） | AuthServiceImpl 校验 | PASSWORD_MISMATCH | 200 |
| 密码策略校验失败 | PasswordPolicy 拒绝 | PASSWORD_WEAK / PASSWORD_TOO_SHORT / PASSWORD_TOO_LONG / PASSWORD_CONTAINS_USERNAME | 200 |
| 常见密码校验（Phase 2） | PasswordPolicy 拒绝 | PASSWORD_COMMON（Phase 2 启用，Phase 1 中不可达） | 200 |
| 业务逻辑错误（登录失败等） | 抛出 BusinessException | 见各 ErrorCode | 200（业务错误仍为 200） |
| 速率限制 | 抛出 RateLimitExceededException | RATE_LIMITED | 429 |
| 系统异常 | 由 GlobalExceptionHandler 捕获 | SYSTEM_ERROR | 500 |

### 10.2 认证相关 ErrorCode

| 枚举值 | code | message | 触发场景 |
|-------|------|---------|---------|
| UNAUTHORIZED | UNAUTHORIZED | "未认证或令牌已失效" | token 缺失/无效/过期/黑名单 |
| FORBIDDEN | FORBIDDEN | "无权限访问" | 角色权限不足 |
| LOGIN_FAILED | LOGIN_FAILED | "用户名或密码错误" | 登录流程中用户名不存在/密码错误/账户禁用或删除 |
| ACCOUNT_DISABLED | ACCOUNT_DISABLED | "账户已被管理员停用" | 已认证请求中 JwtAuthenticationFilter 发现 enabled=false，抛出 AuthenticationException |
| ACCOUNT_LOCKED | ACCOUNT_LOCKED | "账户已锁定，请 15 分钟后重试" | 登录失败超限锁定 |
| RATE_LIMITED | RATE_LIMITED | "登录尝试过于频繁，请稍后重试" | 登录端点专用限流（InMemoryRateLimitGuard，5次/10秒/IP）触发 |
| RATE_LIMITED_GLOBAL | RATE_LIMITED_GLOBAL | "请求过于频繁，请稍后重试" | 全局 IP 限流（GlobalRateLimitFilter，100次/60秒/IP）触发 |
| PASSWORD_TOO_SHORT | PASSWORD_TOO_SHORT | "密码长度不能少于8位" | 密码长度小于8字符 |
| PASSWORD_TOO_LONG | PASSWORD_TOO_LONG | "密码长度不能超过64位" | 密码长度超过64字符 |
| PASSWORD_WEAK | PASSWORD_WEAK | "密码不符合复杂度要求" | 密码未包含3种以上字符类型 |
| PASSWORD_CONTAINS_USERNAME | PASSWORD_CONTAINS_USERNAME | "密码不能包含用户名" | 密码包含用户名 |
| PASSWORD_COMMON | PASSWORD_COMMON | "密码过于常见" | 密码在Top 10000常见密码列表中（Phase 2 启用，Phase 1 中不可达） |
| TOKEN_REFRESH_FAILED | TOKEN_REFRESH_FAILED | "令牌刷新失败，请重新登录" | Refresh Token 黑名单或过期 |
| PASSWORD_CHANGE_REQUIRED | PASSWORD_CHANGE_REQUIRED | "需要修改密码" | PasswordChangeCheckFilter 检测到 passwordChangeRequired=true 且请求不在白名单中 |
| PASSWORD_MISMATCH | PASSWORD_MISMATCH | "旧密码不正确" | 修改密码时旧密码验证失败 |

**ErrorCode 使用边界**：
- `LOGIN_FAILED` 仅用于登录流程的三个失败步骤（步骤 5/6/7），提供统一的"用户名或密码错误"消息，不区分具体失败原因
- `ACCOUNT_DISABLED` 的使用范围限定为"已认证请求中账户被禁用"场景（JwtAuthenticationFilter 中查库发现 enabled=false，抛出 AuthenticationException），此时用户已通过 token 验证，给出具体禁用原因不会引入安全风险
- `PASSWORD_CHANGE_REQUIRED` 专门用于 PasswordChangeCheckFilter 对白名单之外 API 的 403 阻断，与 `FORBIDDEN`（角色权限不足）语义区分。实现机制：PasswordChangeCheckFilter 抛出 `PasswordChangeRequiredException`（继承 `AccessDeniedException`），SecurityConfig 中的 `AccessDeniedHandler` 捕获该异常并返回对应错误码

**新增异常类**：
- `PasswordChangeRequiredException`：继承 `org.springframework.security.access.AccessDeniedException`，在 PasswordChangeCheckFilter 中抛出，由 SecurityConfig 中的 AccessDeniedHandler 捕获并返回统一错误响应。无需在 GlobalExceptionHandler 中额外处理，因 AccessDeniedHandler 已在 SecurityConfig 层面完成响应输出。

## 11. 设计决策

| 决策 | 选项 | 选择 | 理由 |
|------|------|------|------|
| Token 黑名单实现 | 内存 vs Redis | Phase 1 内存（仅 Access Token） | Refresh Token 黑名单需 12GB+ 堆内存，Phase 1 仅黑名单 Access Token（~6.5MB）；Phase 2 迁移 Redis 后启用完整黑名单 |
| Token 黑名单范围 | 全部 vs 仅 Access Token | Phase 1 仅 Access Token | Refresh Token 黑名单内存不可接受；通过 Refresh Token 轮换 + 短 Access Token 有效期补偿安全风险 |
| Refresh Token 轮换 | 轮换 vs 不轮换 | 轮换 | 轮换是最佳实践（RFC 6749），在 Phase 1 即引入即使无 Redis 黑名单也提供基本保护 |
| Access Token 有效期 | 15 分钟 vs 30 分钟 vs 24 小时 | 15 分钟 | 短有效期最小化泄露窗口，配合 Refresh Token 轮换提供安全性和用户体验的平衡 |
| JWT claims 中是否包含权限 | 包含 vs 不包含 | 不包含 | 权限从 DB 实时加载，角色变更即时生效；避免 JWT claims 与 DB 不一致 |
| Filter 中是否查库验证 enabled | 查库 vs 仅依赖 claims | 查库 | 确保禁用用户即时失效，这是安全审计的硬性要求 |
| passwordChangeRequired 控制位置 | JwtAuthenticationFilter vs 独立 Filter | 独立 PasswordChangeCheckFilter | 遵循单一职责原则：JwtAuthenticationFilter 只负责 JWT 鉴权，密码变更检查属于业务规则 |
| DTO 形态 | record vs 传统 POJO | record（Java 17） | 减少样板代码，不可变 DTO 适合数据传输场景 |
| 密码策略接口化 | interface vs 硬编码 | PasswordPolicy interface | 支持未来切换复杂度规则策略而不改核心代码 |
| 速率限制维度 | IP 维度 vs 用户维度 vs 双维度 | 双维度 | IP 维度防御基础设施攻击，用户维度防御凭据猜测 |
| 限流锁策略 | synchronized vs ReentrantLock | ReentrantLock | 支持公平锁和 tryLock 超时机制，窗口清理线程不易被抢占 |
| 登出清理范围 | 仅前端 vs 前后端联动 | 前后端联动 | 后端黑名单确保 Access Token 不可复用，前端 finally 清理兜底；登出可选携带 refreshToken 供 Phase 2 使用 |
| PermissionFunction 类名 | 保留 Function vs 改名 | PermissionFunction | 避免与 `java.util.function.Function` 的 IDE 混淆；DB 表名 `sys_function` 不变 |
| SecurityContext 装配内容 | 仅 role vs role + 功能权限 | role + 功能权限 | `@PreAuthorize` 的方法级控制需要完整的 authority 列表 |
| refresh 端点认证策略 | permitAll vs JWT 认证 | permitAll | refresh 端点依赖 Refresh Token 自验证，不需要前置 JWT 认证；统一为 permitAll 避免矛盾 |
| 菜单路由注册策略 | 嵌套 children vs 展平 | 展平（flatten） | 展平路由表避免递归路由深度限制和 name 冲突；每个路由独立 path 和 name，不依赖父级 Layout 嵌套 |
| 前后端字段名对齐 | 后端改 vs 前端改 vs 双向改 | 后端对向前端（Breaking Change） | 前端 `UserInfo` 接口已在使用中，后端调整 DTO 字段名成本更低；声明 Breaking Change 确保团队知悉 |
| 密码变更策略 | 无过渡 vs PasswordChangeService | PasswordChangeService interface | 支持首次登录强制修改和管理员过期标记两种场景，ErrorCode.PASSWORD_CHANGE_REQUIRED 不再悬空 |
| 主角色判定 | 依赖 primaryRole 字段 vs 按角色优先级 | 按角色优先级排序 | 在 Role 实体中新增 `sort` 字段（Integer），值小者优先级高；无角色时返回空字符串 |
| LoginResponse.user 可空性 | 可空 vs 始终返回 | 始终返回 | 消除三方不一致：DTO 定义、流程描述、JSON 示例统一；passwordChangeRequired 场景下仍返回用户标识 |
| Refresh Token 密码变更后撤销 | 无机制 vs tokenVersion 版本号 | User 实体 tokenVersion 字段 | 密码变更后 tokenVersion 递增，Refresh Token claims 携带此版本号，刷新时比对拒绝旧 token；不引入 Redis 依赖，Phase 1 即可实现；代价是每次刷新多查一个字段 |

## 12. 修订说明（v2）

| 审查意见 | 修改措施 |
|---------|---------|
| P1/P9：设计文档多处声称代码"当前状态"缺少 NOT NULL 约束/Java 默认值，实际已存在 | 修订 5.1 节实体变更表：User.java password/enabled 标注为"已完成"；Function.java enabled/visible 标注为"已完成"。修订 4.3 节新增"NOT NULL 约束状态确认"表格，逐字段标注当前实际状态和待修复项 |
| P3：`PUT /api/auth/password` 缺少 PasswordChangeRequest DTO 定义 | 新增 `PasswordChangeRequest` record 到 5.2 节，含 `oldPassword` 和 `newPassword` 字段 |
| P5：6048 万条 Refresh Token 黑名单需约 12GB+ 堆内存，内存方案不可行 | 修订 1.2 节和 4.2 节：Phase 1 改为仅黑名单 Access Token（~6.5MB 内存），Refresh Token 黑名单推迟至 Phase 2 Redis 方案。新增 4.2 节的"Refresh Token 轮换的安全补偿"说明 |
| P2：密码过渡方案缺失，PASSWORD_CHANGE_REQUIRED 未在任何流程中使用 | 新增 `PasswordChangeService` 接口到核心抽象一览。修订 3.1.1 节登录流程步骤 9（PasswordChangeService 检查）+ 步骤 11（LoginResponse.passwordChangeRequired）。新增 3.4 节"密码变更强制策略"子节。ErrorCode 表中 PASSWORD_CHANGE_REQUIRED 不再悬空 |
| P4：refresh 端点请求体未定义 | 新增 `RefreshTokenRequest{String refreshToken}` record 到 5.2 节。修订 6.1 节接口清单中 refresh 端点请求体列为 RefreshTokenRequest |
| P6/P7：UserInfoResponse 字段与前端 UserInfo 不匹配；LoginResponse 字段名变更未声明 | 修订 5.2 节 UserInfoResponse 字段名：`nickname` → `realName`，`userType` → `role`。修订 6.4 节新增 Breaking Change 声明表格，列出 token→accessToken, nickname→realName, userType→role 的变更 |
| P8：JwtAuthenticationFilter 迁移到 common-module-impl 后跨模块依赖建立方式未说明；JwtUtil→JwtTokenProvider 过渡方案缺失 | 修订 2.2 节模块依赖方向，增加迁移步骤说明（application 模块删除旧 Filter 引用，common-module-impl 中 auth/security 包下新 Filter 替代，application 模块保留 SecurityConfig 中的 Filter 注册配置） |
| P13：refresh 端点在 SecurityConfig 中设为 permitAll 但 4.4 节保护清单标注为 JWT，两处矛盾 | 修订 4.4 节保护清单：`/api/auth/refresh` 统一为"匿名（permitAll）"，并在清单下方增加说明段落。修订 3.3 节 SecurityConfig 中 refresh 端点为 permitAll 并在"设计要点"中说明 |
| P14：convertMenusToRoutes 递归化方案仅说"递归"，未说明路由注册策略需同步变更 | 修订 8.2 节 H5 修复方案：明确使用"递归展平版本"，路由注册策略为展平（flatten）而非嵌套 children，每个路由 path 为完整路径，name 使用 permission(code) 确保唯一 |
| P10：SidebarBase 修复方案模糊，未说明 props 名称、类型及受影响父组件 | 修订 8.2 节 M12_P10 修复方案：指定 props 定义为 `currentPath: { type: String, required: true }`，受影响父组件为 LayoutBase |
| P11：用户信息明文存 localStorage 在前后端重复列出 | 合并到 7.4 节（包 D 前端补偿机制）统一记录，8.1 节和 8.2 节中仅保留一条记录（前端 M16），删除后端重复条目 |
| P12：Role.java/Post.java enabled 缺少 `@Column(nullable=false)`（Function.java 已有） | 修订 5.1 节 Role.java/Post.java 变更表：补加 `@Column(nullable=false)` 注解。修订 4.3 节 NOT NULL 约束状态确认表，逐字段标注状态。schema.sql 补加 NOT NULL 到所有 enabled/visible 列 |

## 13. 修订说明（v3）

| 审查意见 | 修改措施 |
|---------|---------|
| 1. deleted 列当前状态描述与代码实际不符（8.3 节 A2 行） | 修订 8.3 节 A2 行：当前状态改为"16 张表 DDL 已全部为 `NOT NULL DEFAULT 0`，已完成"，消除事实错误 |
| 2. Phase 1 Refresh Token 轮换安全补偿机制存在错误逻辑（3.1.3 节） | 修订 3.1.3 节步骤 5：删除关于"jti 不冲突无法通过应用层校验"的错误描述，改为如实说明"存在真实风险：攻击者可任意使用" |
| 3. passwordChangeRequired 对其他 API 的访问控制缺失 | 新增 3.4 节"passwordChangeRequired 访问控制"子节，定义白名单（/api/auth/password、/api/auth/logout）及 Filter 层 403 阻断策略；4.4 节清单下方补充阻断说明 |
| 4. 4.3 节 Role/Post 补加 `@Column(nullable=false)` 标注"已完成"但与代码及 5.1 节矛盾 | 修订 4.3 节修复方案：Role.java 和 Post.java 的 `@Column(nullable=false)` 状态由"已完成"改为"待修复"；同步修订 5.1 节对应行的"修复方案"列标注 |
| 5. LoginResponse 未包含 userId/username 顶层字段 | 修订 5.2 节 LoginResponse record：新增 `Long userId` 和 `String username` 字段；修订 6.2 节 JSON 示例同步新增；修订 6.4 节 Breaking Change 声明新增 userId/username 条目 |
| 6. 菜单 CRUD 缺少 MenuCreateRequest 和 MenuUpdateRequest DTO | 新增 5.2 节：MenuCreateRequest record 和 MenuUpdateRequest record 定义 |
| 7. 登录错误消息差异化导致用户名可枚举（3.1.1 节步骤 6"账户已停用"） | 修订 3.1.1 节步骤 6：enabled=false 或 deleted=true 时统一返回"用户名或密码错误"，消除用户名可枚举风险 |
| 8. 8.3 节 A2 与 5.1 节对 deleted 列的完成状态矛盾 | 统一为"已完成"，修订 8.3 节 A2 行当前状态（见第 1 条） |
| 9. 7.5 节声明已合并重复条目，但 8.1 节仍保留 M12_P11 行 | 删除 8.1 节 M12_P11 行，替换为 HTML 注释说明已合并至 7.4 节 |
| 10. 多 Tab 并发 token 刷新竞态未处理 | 修订 7.4 节：新增"多 Tab 并发刷新互斥"补偿策略，使用 Promise 单例模式确保同一时间仅执行一次刷新 |
| 11. getPrimaryRoleCode() 未定义主角色判定策略 | 修订 7.3 节 UserType→role 字段映射行：补充显式策略（优先使用 primaryRole 字段，否则按角色优先级排序，无角色时返回空字符串） |
| 12. SecurityConfigPhase1 依赖注入的旧 Filter 未纳入迁移方案 | 修订 2.2 节迁移步骤：补充 `SecurityConfigPhase1` 构造函数中旧包名引用需改为新包名的说明 |

## 14. 修订说明（v4）

| 审查意见 | 修改措施 |
|---------|---------|
| 1. 4.2 节声称旧 Refresh Token"不可重复使用"，与 Phase 1 设计矛盾（Phase 1 跳过黑名单） | 修订 4.2 节安全补偿策略标题和正文：删除"旧 token 不可重复使用"的错误表述，改为"旧 Refresh Token 在 Phase 1 中仍可被重复使用"；补充异常刷新检测和告警机制作为补偿 |
| 2. passwordChangeRequired 字段未出现在 5.1 节实体变更表中 | 在 5.1 节 User.java 变更表新增 `passwordChangeRequired` 字段行，包含 Java 注解、默认值和 DDL 变更说明。同步更新 4.3 节 NOT NULL 约束状态确认表 |
| 3. 登录流程统一错误消息缺少对应的 ErrorCode，ACCOUNT_DISABLED 消息与设计冲突 | 新增 `LOGIN_FAILED` ErrorCode（消息"用户名或密码错误"）。修订 10.2 节：登录流程三个失败步骤使用 `LOGIN_FAILED`；限制 `ACCOUNT_DISABLED` 的使用范围为"已认证请求中账户被禁用"，消息改为"账户已被管理员停用" |
| 4. LoginResponse.user 可空性在 DTO、流程、JSON 中三方不一致 | 删除 5.2 节 LoginResponse 中 user 字段的可空注释；修订 6.2 节 JSON 示例 user 始终存在。统一决策：始终返回 user 对象 |
| 5. 7.3 节主角色判定策略引用不存在的 `primaryRole` 字段 | 删除对 `primaryRole` 字段的引用。主角色判定策略统一为：按角色优先级（Role 实体的 level/sort 字段）排序取最高者；无角色时返回空字符串。修订 7.3 节和 5.2 节 UserInfoResponse 字段映射说明 |
| 6. 9.2 节限流并发方案使用"或"连接两个选项，未做最终决策 | 选定 `ReentrantLock`，补充选择理由（公平锁特性、tryLock 超时机制）。修订 9.2 节和 11 节设计决策表 |
| 7. 2.1 节目录结构未包含 MenuCreateRequest / MenuUpdateRequest | 在 2.1 节 `menu/dto/` 下补充 `MenuCreateRequest.java` 和 `MenuUpdateRequest.java` |
| 8. 8.3 节 A4 引用"诊断报告中的模板"无具体路径 | 将引用路径明确为 `Harness/reviews/202606252200_phase1_pkgBD_review/report_integration_security.md` |
| 9. JWT_SECRET 配置缺少参考与约束 | 新增 4.7 节"JWT 密钥配置与约束"，包含最小长度要求（256 位）、合法字符集、启动时校验失败处理方式、application.yml 配置示例和启动验证逻辑 |
| 10. CORS 配置使用默认值存在生产安全隐患 | 修订 3.3 节 SecurityConfig 中 cors 配置的注释，标注"开发默认；生产环境见 4.6 节"。新增 4.6 节"CORS 配置策略"，明确生产环境必须通过 `CorsConfigurationSource` bean 配置白名单，并给出 recommended 配置示例 |
| 11. passwordChangeRequired 访问控制放置在 JwtAuthenticationFilter 违反单一职责 | 抽离为独立 `PasswordChangeCheckFilter`：更新 1.3 节核心抽象一览新增该 filter；更新 2.1 节目录结构新增文件；更新 3.3 节 SecurityFilterChain 注册顺序（addFilterAfter）；更新 3.3 节新增 PasswordChangeCheckFilter 行为契约；更新 11 节设计决策表 |
| 12. `/api/auth/logout` 请求体在接口清单和流程描述中不一致 | 统一决策：登出端点接受可选 `refreshToken` 字段（与 RefreshTokenRequest 格式一致）。修订 6.1 节接口清单请求体列更新为"RefreshTokenRequest（可选）"；修订 3.1.4 节登出流程步骤 1 描述可选字段；修订 4.4 节保护清单备注列 |
| 13. 步骤 7（密码不匹配）未指定错误消息 | 修订 3.1.1 节步骤 7：补上统一消息"用户名或密码错误"（ErrorCode.LOGIN_FAILED） |
| 14. 7.2 节分页兼容承诺与接口清单不匹配 | 删除 7.2 节兼容承诺第 4 条（"分页查询参数格式保持不变"），因为 Phase 1 接口清单中无任何分页查询端点 |
| 15. 8.2 节 H6 循环依赖修复方案过于模糊 | 给出具体实现示例：延迟 `import()` 函数 + `getCurrentInstance()` 获取 router；或 Pinia store 初始化时使用 `inject('router')` 依赖注入 |
| 16. User 查询优化在 7.1 节依赖图和 8.1 节 M9 之间不一致 | 在 7.1 节依赖图下方添加脚注，说明涉及 roles/posts 级联查询的方法应使用 `@EntityGraph` 显式控制 fetch join，避免 N+1 |

## 15. 修订说明（v5）

| 审查意见 | 修改措施 |
|---------|---------|
| P1：主角色判定策略引用 Role 实体不存在的 `level`/`sort` 字段 | 在 5.1 节 Role.java 变更表中新增 `sort` 字段行；同步更新 schema.sql 修复方案新增 `sys_role.sort INT DEFAULT 0`；修订 5.2 节字段映射说明、7.3 节影响表、11 节设计决策，统一明确使用 `sort`（值小者优先级高） |
| P2：`ACCOUNT_DISABLED` 定义了但 JwtAuthenticationFilter 行为使其不可达 | 修订 3.3 节 JwtAuthenticationFilter 步骤 4：disabled 情况与 deleted/不存在拆分，enabled=false 时抛出 AuthenticationException（ACCOUNT_DISABLED）。更新 10.1 节错误分类表、10.2 节 ErrorCode 表及使用边界说明 |
| P3：PasswordChangeCheckFilter 冗余查询用户 | 修订 3.3 节 JwtAuthenticationFilter 新增步骤 5（将 passwordChangeRequired 存入 request attribute）；修订 PasswordChangeCheckFilter 步骤 3 改为从 request attribute 读取。更新 3.1.2 节关键设计决策补充此优化说明 |
| P4：刷新端点 Breaking Change 未声明 | 在 6.4 节新增 `TokenRefreshResponse` 字段变更条目（`token`→`accessToken`，移除 `user`）及前端适配说明 |
| P5：`MenuResponse` 被引用但未定义 | 在 5.2 节新增 `MenuResponse` record 定义（含 id、name、path、component、icon、permission、sort、children）及字段对齐说明 |
| P6：全局 IP 频率限制缺少实现机制 | 新增 `GlobalRateLimitFilter`（OncePerRequestFilter）：添加至 1.3 节核心抽象、2.1 节目录结构、3.3 节 SecurityFilterChain（addFilterBefore JwtAuthenticationFilter）。更新 4.1 节实现方式说明全局 IP 限流的白名单和 Filter 机制 |
| P7：登录步骤 5 失败计数维度不明确 | 修订 3.1.1 节步骤 5：明确递增 IP 维度（无有效用户名）；步骤 6：明确递增用户名和 IP 双维度 |
| P8：`MenuUpdateRequest.id` 与路径 `{id}` 关系不明确 | 在 5.2 节 MenuUpdateRequest 中为 `id` 字段补充 `@NotNull` 注解，并新增一致性校验说明：路径参数与请求体 id 必须一致，否则返回 400 |
| P9：PasswordChangeCheckFilter 的 403 消息与 ErrorCode 不一致 | 修订 PasswordChangeCheckFilter 行为：返回 403 + ErrorCode.PASSWORD_CHANGE_REQUIRED。更新 10.2 节 ErrorCode 表：FORBIDDEN 仅用于角色权限不足，PASSWORD_CHANGE_REQUIRED 专用于密码变更阻断 |
| P10：刷新后获取用户信息流程缺失 | 在 3.1.3 节步骤 8 后新增步骤 9（前端调用 `GET /api/auth/me`）；更新刷新时机说明。在 7.4 节新增前端补偿策略条目（刷新成功后立即调用 `/api/auth/me`） |

## 16. 修订说明（v6）

| 审查意见 | 修改措施 |
|---------|---------|
| 1. 登录流程时序侧信道攻击风险（3.1.1 节步骤 5 vs 步骤 7）：步骤 5 不执行 BCrypt 比对，步骤 7 执行 passwordEncoder.matches()，攻击者可通过响应时间差异枚举有效用户名 | 3.1.1 节步骤 5：用户名不存在时对虚拟哈希值执行 dummy BCrypt 比对，消除与步骤 7 的响应时间差异；3.1.1 节设计要点补充时序侧信道防御说明 |
| 2. 密码变更后旧 Refresh Token 撤销机制未设计（4.2 节）：声称旧 token 在密码变更后被拒绝，但仅作为 TODO 占位 | 采用 tokenVersion 方案：User 实体新增 `tokenVersion` 字段（Integer，初始值 0），Refresh Token claims 携带版本号，密码变更后版本号递增，刷新时比对拒绝旧 token。修订 5.1 节 User.java 变更表新增 tokenVersion 行、4.3 节 NOT NULL 约束表新增 tokenVersion 行、4.2 节安全补偿策略替换 TODO 为具体设计、3.1.3 节刷新流程重构为步骤 5 先查用户 + 步骤 7 验证 tokenVersion + 步骤 8 签发携带 tokenVersion 的新 token、11 节设计决策表新增 tokenVersion 条目 |
| 3. 密码变更成功后前端恢复流程未定义（3.4 节、7.4 节）：仅定义清除 passwordChangeRequired 标记，未定义前端后置流程 | 3.4 节密码变更强制策略第 1 条末尾补充完整恢复流程：清除标记 → GET /api/auth/me → GET /api/menu/tree → 跳转首页。7.4 节新增密码修改成功后恢复流程条目 |
| 4. 菜单递归展平方案与路由注册策略存在冲突（8.2 节 H5）：递归展平生成的完整路径在 Vue Router 中的实际注册行为未明确 | 扩展 8.2 节 H5 修复方案：明确 (a) 路由注册策略为 `router.addRoute()` 一级注册，不嵌套 Layout，path 为完整路径；(b) 与 `/` 跳过滤条件的兼容关系：根路由单独注册不冲突；(c) name 唯一性保证策略：permissionCode 驼峰映射 + 运行时数字序号追加 |
| 5. 前端缺少 PASSWORD_CHANGE_REQUIRED 错误码处理（7.4 节）：HTTP 403 + ErrorCode PASSWORD_CHANGE_REQUIRED 在 axios 拦截器中无对应处理规则 | 7.4 节新增 axios 响应拦截器识别 PASSWORD_CHANGE_REQUIRED 错误码时重定向到密码修改页面并终止原始请求的补偿策略条目，且注明优先级高于 401 静默刷新 |
| 6. 登录流程步骤 7 失败计数维度未指定（3.1.1 节步骤 7）：仅说"递增 LoginAttemptTracker 失败计数" | 3.1.1 节步骤 7 补充为"递增 LoginAttemptTracker **用户名维度**的失败计数" |
| 7. JwtTokenProvider @PostConstruct 启动验证依赖未显式声明的 Bean 类型（4.7 节、2.1 节）：未说明 Spring Bean 注册方式 | 1.3 节核心抽象 JwtTokenProvider 类型形态由 `class` 改为 `@Component class`，职责定位补充 `@Component` 注册说明。4.7 节新增 Bean 注册说明段落，明确 `@Component` 确保 `@PostConstruct` 生效 |

## 17. 修订说明（v7）

| 审查意见 | 修改措施 |
|---------|---------|
| 1. 3.2 节 Refresh Token Claims 示例缺少 `tokenVersion` 字段 | 在 3.2 节 Refresh Token JSon 示例中补充 `"tokenVersion": 0` 字段，并添加注释说明其作用 |
| 2. 8.1 节缺陷追踪表遗漏 M1/M2/M5/M8 四条原始问题条目 | 在 8.1 节按编号顺序补充 M1（buildUserInfoResponse 私有方法不可测试）、M2（updateProfile 多余 save()）、M5（医生岗位 iterator().next()）、M8（Function.java 类名冲突）四条目的追踪行，包含当前状态、修复方案、潜在副作用、影响范围四列 |
| 3. 3.1.3 节步骤 6 错误使用 `LOGIN_FAILED` 作为刷新失败错误码 | 3.1.3 节步骤 6 将 `LOGIN_FAILED` 改为 `TOKEN_REFRESH_FAILED` |
| 4. 3.3 节 AuthenticationEntryPoint 描述与 10.2 节 ACCOUNT_DISABLED 的矛盾 | 修订 3.3 节 SecurityFilterChain exceptionHandling 注释：AuthenticationEntryPoint 行为契约拆分为两种场景——携带 ACCOUNT_DISABLED 时返回 `Result.fail("ACCOUNT_DISABLED", "账户已被管理员停用")`，其余返回 `Result.fail("UNAUTHORIZED", "未认证或令牌已失效")`。同步更新设计要点中的描述 |
| 5. 4.3 节与 5.1 节对 `nickname NOT NULL` 变更的描述不一致 | 在 5.1 节 User.java 变更表中新增 `nickname NOT NULL` 行，状态为"代码已有 `@Column(nullable=false)`"，修复方案为 schema.sql 改为 NOT NULL |
| 6. 8.2 节 H5 展平路由策略缺少 Layout 包容机制说明 | 在 8.2 节 H5 修复方案中补充 Layout 包容机制设计决策：Layout 在 App.vue 中通过 `<router-view>` 包裹路由出口，展平路由由根路由 Layout 统一渲染；或通过路由元数据 `meta.layout` 动态选择 Layout |
| 7. 登录流程步骤 6 存在时序侧信道缺口 | 3.1.1 节步骤 6（enabled=false/deleted=true）增加 dummy BCrypt 比对。设计要点补充说明步骤 6 攻击面较小但仍做统一防御 |
| 8. `LoginResponse` 中 `tokenType` 与 `expiresIn` 的定位不明确 | 在 5.2 节 LoginResponse 定义前补充 `expiresIn` 语义说明为"access token 剩余有效秒数（从签发时计算），供前端预估刷新时机"。同步更新 6.2 节 JSON 示例字段注释 |
| 9. 5.1 节 `User.tokenVersion` 缺少 `@Column(nullable=false)` 注解 | 5.1 节 User.java 变更表和 4.3 节 NOT NULL 约束表中 tokenVersion 字段补加 `@Column(nullable=false)` |
| 10. 密码变更 API 流程缺少独立完整描述 | 新增 3.1.6 节「密码变更流程」，将密码变更服务端处理逻辑整理为 11 步结构化描述，含旧密码校验、PasswordPolicy 检查、tokenVersion 递增、清除 SecurityContext 等步骤，并附设计说明 |
| 11. 10.1 节错误分类表未覆盖全部 ErrorCode 的 HTTP 状态映射 | 在 10.1 节补充缺失 ErrorCode：ACCOUNT_LOCKED（429）、TOKEN_REFRESH_FAILED（401）、PASSWORD_MISMATCH（200）、密码策略类 ErrorCode（200） |
| 12. `PUT /api/auth/password` 成功后 `tokenVersion` 递增对已登录 Access Token 的影响未说明 | 在新增的 3.1.6 节设计说明中明确：Access Token 不检查 tokenVersion 是已知设计决策（短有效期 + 查库验证 enabled 已提供基本保护），非疏忽。密码变更后不清除已登录 Access Token 是有意权衡 |
| 13. `<script setup>` 或 Options API 使用约定未说明 | 在 8.2 节开头补充前端代码风格约定说明：统一使用 `<script setup>` 语法（Composition API）。同步修订 H6 修复方案，移除 `getCurrentInstance()` 示例，改为推荐 `inject('router')` 依赖注入模式 |

## 18. 修订说明（v8）

| 审查意见 | 修改措施 |
|---------|---------|
| 10.2 节 ErrorCode 表与 4.3 节密码策略 ErrorCode 定义不一致：10.2 节仅列出 `PASSWORD_TOO_WEAK`，与 4.3 节的 5 个具体错误码命名不统一 | 10.2 节将 `PASSWORD_TOO_WEAK` 替换为 4.3 节定义的 5 个具体错误码（PASSWORD_TOO_SHORT、PASSWORD_TOO_LONG、PASSWORD_WEAK、PASSWORD_CONTAINS_USERNAME、PASSWORD_COMMON），分别补充对应消息和触发场景说明 |

## 19. 修订说明（v9）

| 审查意见 | 修改措施 |
|---------|---------|
| 问题 1（严重）：异常刷新检测机制缺少可落地的实现细节——未定义时间窗口、阈值、实现位置、类定义、告警方式 | 4.2 节异常刷新检测补充完整设计契约：定义 5 秒时间窗口和 2 次阈值；指定在 `AuthServiceImpl.refresh()` 中实现，使用 `ConcurrentHashMap<Long, Deque<Long>>` 维护滑动窗口；告警方式为 `log.warn` 输出安全日志，可对接 Prometheus + AlertManager |
| 问题 2（严重）：H6 修复方案推荐的 `inject('router')` 在 Pinia store 中不可用 | 8.2 节 H6 修复方案替换为引用现有的 `createMenuStore(router, dynamicPageComponent)` 工厂模式，通过参数传入 router 实例解决循环依赖 |
| 问题 3（重要）：管理员标记密码过期的 API 端点缺失——3.4 节描述功能但 6.1 节无对应端点 | 3.4 节场景 2 标注"此功能属于管理端设计范围，本设计不做具体接口定义" |
| 问题 4（一般）：ProfileUpdateRequest.nickname 缺少空白字符串校验 | 5.2 节 ProfileUpdateRequest 补充 `@NotBlank(message = "昵称不能为空")` 注解 |
| 问题 5（一般）：MenuUpdateRequest 的 PUT 语义不明确（全量替换 vs 部分更新） | 5.2 节 MenuUpdateRequest 补充更新语义声明：采用局部更新（PATCH 语义），省略的字段保持不变 |
| 问题 6（一般）：登出端点在 token 过期场景下的行为未定义 | 3.1.4 节登出流程步骤 5 补充说明：后端登出 API 因 token 过期/无效返回 401 时，前端 finally 块仍需清除本地 token 和用户数据 |
| 问题 7（一般）：LoginAttemptCleaner 类定义但无行为说明 | 2.1 节目录结构中删除 `LoginAttemptCleaner.java` 条目，`LoginAttemptTracker` 注释补充"含惰性清理"说明；惰性清理已足够覆盖需求 |

## 20. 修订说明（v10）

| 审查意见 | 修改措施 |
|---------|---------|
| 问题 1（严重）：3.1.6 节密码变更流程步骤 10"强制用户重新登录"与设计说明第二段"不清除已登录用户 Access Token"语义自相矛盾 | 3.1.6 节步骤 10 将"清除 SecurityContext（强制用户重新登录获取新令牌）"修改为"清除当前请求 SecurityContext（作为安全最佳实践），客户端可继续使用旧 Access Token 至其自然过期（最长 15 分钟）"。3.1.6 节设计说明第一段末句同步修改为"步骤 10 清除 SecurityContext 仅为清除当前请求安全上下文，不强制前端重新登录；客户端可继续使用旧 Access Token 至其自然过期"，消除语义矛盾 |
| 问题 2（严重）：PASSWORD_COMMON ErrorCode 在 Phase 1 中不可达，形成代码死分支 | 4.3 节密码策略表"常见密码"行标注"Phase 2 启用，Phase 1 中不可达"；10.1 节错误分类表将 PASSWORD_COMMON 拆为独立行并标注 Phase 2；10.2 节 ErrorCode 表 PASSWORD_COMMON 行触发场景补充"Phase 2 启用，Phase 1 中不可达" |
| 问题 3（严重）：登录流程（3.1.1 步骤 10）未从 User 实体获取 tokenVersion 用于 Refresh Token 签发，导致 tokenVersion 闭环断裂 | 3.1.1 节步骤 10 补充"JwtTokenProvider 从 `User.tokenVersion` 读取当前版本号，嵌入 Refresh Token claims 中的 `tokenVersion` 字段"，确保签发时携带当前 tokenVersion |
| 问题 4（重要）：展平路由 name 唯一性保证策略存在运行时竞态隐患 | 8.2 节 H5 修复方案的 name 唯一性保证策略改为确定性方案：permissionCode 转驼峰后添加前端应用前缀（如 `Doctor_SystemUser`）消除跨权限冲突，避免运行时按需修正。补充说明"若仍保留运行时检测，检测到冲突时拒绝注册而非覆盖" |
| 问题 5（重要）：登录成功（3.1.1 步骤 8）仅清除用户名维度的失败计数，IP 维度未处理 | 3.1.1 节步骤 8 补充"同时清除该请求来源 IP 的失败计数"。4.1 节 LoginAttemptTracker 表 IP 维度重置时机由"锁定到期"改为"锁定到期/登录成功" |
| 问题 6（重要）：多 Tab 并发刷新失败的连锁错误处理未定义——失败计数器是全局共享还是按等待请求独立计数 | 7.4 节新增失败计数策略说明：采用全局共享策略，按 60 秒时间窗口统计单次 Promise 的失败次数，同一时间窗口内无论并发等待请求数量多少，失败次数仅记一次 |
| 问题 7（重要）：`expiresIn` 在 TokenRefreshResponse 中的语义未明确，与 LoginResponse 的"从签发时计算"语义形成落差 | 5.2 节 TokenRefreshResponse record 定义前补充与 LoginResponse 一致的 expiresIn 语义说明：新签发的 access token 剩余有效秒数（从签发时计算），典型值 900 |
| 问题 8（重要）：GlobalRateLimitFilter 与 InMemoryRateLimitGuard 之间的委托关系未定义 | 4.1 节 GlobalRateLimitFilter 段明确：GlobalRateLimitFilter**不委托** InMemoryRateLimitGuard，两者职责分工明确——全局限流覆盖所有 API 路径（白名单除外），登录专用限流仅覆盖登录端点；计数器实例独立，互不干扰；提取公共滑动窗口工具类 `SlidingWindowCounter` 供两处复用 |
| 问题 9（一般）：IP 维度 LoginAttemptTracker 的"连续"语义未定义 | 4.1 节 LoginAttemptTracker 实现方式后补充"连续"语义定义：同一 IP 发起的登录请求连续失败，中间不得插入任何非登录请求或登录成功；非登录请求或登录成功均视为中断连续序列；用户名维度相同语义 |
| 问题 10（一般）：密码策略的 Phase 1 过渡方案缺少种子数据处理说明 | 4.3 节修复方案列表前新增"种子数据迁移策略"段落：种子密码更新为满足新规则的密码、加密后写入 schema.sql、统一设置 passwordChangeRequired=true、与 DDL 变更同步上线 |
| 问题 11（一般）：登录步骤 5 对无 username 的 IP 维度计数使用了"虚拟哈希"但不占用 key 空间，步骤 5 与步骤 7 的 IP 维度计数关系模糊 | 3.1.1 节设计要点末尾补充"IP 维度计数器关系说明"：步骤 5 中递增的 IP 维度失败计数与步骤 6/7 的 IP 维度计数**共用同一个全局计数器**，不区分触发步骤；用户名维度按用户名独立计数 |

## 21. 修订说明（v11）

| 审查意见 | 修改措施 |
|---------|---------|
| 问题 1（重要）：4.1 节"连续"语义定义（严格连续——中间不得插入非登录请求）与 AttemptRecord 实现机制不匹配，后者仅支持时间窗口累计计数 | 4.1 节"连续"语义定义简化为"时间窗口内累计失败次数"：IP 维度 30 分钟窗口、用户名维度 15 分钟窗口，登录成功或窗口超时后重置；移除"中间不得插入非登录请求"的不可实现表述，使语义定义与 AttemptRecord 数据结构及惰性清除机制一致 |
| 问题 2（一般）：3.1.3 节 Token 刷新流程未检查账户锁定状态，锁定后可绕过刷新 | 3.1.3 节刷新流程步骤 5 后新增步骤 6（账户锁定检查）：从 DB 加载用户后检查 LoginAttemptTracker 中该用户名是否处于锁定状态，若已锁定返回 TOKEN_REFRESH_FAILED；后续步骤顺延 |
| 问题 3（一般）：版本号不一致——文档标题（v9）、修订说明（v10）、文件名（v10） | 文档标题由"v9"统一为"v10"，对齐文件名和最新修订说明版本号 |
| 问题 4（一般）：3.1.6 节密码变更步骤 10"清除 SecurityContext 作为安全最佳实践"说法不准确，SecurityContext 为请求作用域 | 3.1.6 节步骤 10 将"作为安全最佳实践"修改为准确描述："清除当前线程的安全上下文，避免密码变更操作后的请求误用旧上下文" |

## 22. 修订说明（v11）

| 审查意见 | 修改措施 |
|---------|---------|
| Issue 1（严重）：4.1 节速率限制表头声称限流维度为「同一IP（任意API路径）」，但GlobalRateLimitFilter将`/api/auth/login`和`/api/auth/refresh`列入白名单排除；`/api/auth/refresh`既不受全局限流覆盖，也无独立限流策略 | 4.1 节速率限制表第二行"同一 IP（任意 API 路径）"改为"同一 IP（排除白名单的所有 API 路径）"，消除与白名单的矛盾。新增「关于 `/api/auth/refresh` 无限流的说明」段落，明确 refresh 端点不实施速率限制是有意设计决策，列出 Refresh Token 自验证、tokenVersion、异常刷新检测等补偿机制 |
| Issue 2（严重）：3.1.4/4.4/5.2/6.1 节文档一致描述登出端点请求体可选携带 refreshToken，但 5.2 节 RefreshTokenRequest record 标注 @NotBlank；Controller 层 @RequestBody(required=false) 未指定 | 5.2 节 RefreshTokenRequest 上方新增使用说明，明确 Refresh 端点要求 @NotBlank、登出端点使用 @RequestBody(required=false)。3.1.4 节登出流程步骤 1 补充"Controller 层使用 @RequestBody(required=false) 接收"。4.4 节保护清单登出行备注增加 @RequestBody(required=false)。6.1 节接口清单登出行请求体列补充 @RequestBody(required=false) |
| Issue 3（重要）：5.2 节 MenuUpdateRequest 声明局部更新语义（PATCH 语义），但端点方法仍为 PUT | 5.2 节 MenuUpdateRequest 更新语义行"PUT /api/menu/{id}"改为"PATCH /api/menu/{id}"，并补充 RFC 7231 §4.3.4 引用。6.1 节接口清单菜单更新行由 PUT 改为 PATCH |
| Issue 4（重要）：3.3/10.1/10.2 节 PasswordChangeCheckFilter 返回 403 的实现机制未定义 | 3.3 节 PasswordChangeCheckFilter 行为契约：返回 403 的方式明确为"抛出 `PasswordChangeRequiredException`（扩展 `AccessDeniedException`），由 AccessDeniedHandler 捕获后返回 403 + ErrorCode.PASSWORD_CHANGE_REQUIRED"。3.3 节 SecurityConfig exceptionHandling accessDeniedHandler 更新为区分 PasswordChangeRequiredException（返回 PASSWORD_CHANGE_REQUIRED）和其他 AccessDeniedException（返回 FORBIDDEN）。10.1 节错误分类表密码变更检查阻断行更新为抛出异常 → AccessDeniedHandler 捕获路径。10.2 节 ErrorCode 使用边界后新增「新增异常类」段落定义 PasswordChangeRequiredException |
| Issue 5（重要）：5.2 节 MenuUpdateRequest.id 标注 @NotNull 用于一致性校验，但与局部更新语义冲突 | 5.2 节 MenuUpdateRequest.id 移除 @NotNull 注解，改为可选携带。更新语义说明：若请求体携带 id 则与路径参数比对，不一致返回 400；若未携带则跳过一致性校验。此设计对齐 PATCH 语义（id 不是被更新字段，仅为可选校验信息） |
| Issue 6（一般）：4.1 节 SlidingWindowCounter 接口契约（ReentrantLock？窗口精度？）未定义；两套限流器返回相同 ErrorCode RATE_LIMITED 时前端无法区分触发来源 | 4.1 节 GlobalRateLimitFilter 段落与 Redis 段落之间新增「SlidingWindowCounter 契约」段落，定义数据结构（ConcurrentHashMap + Deque）、精度（毫秒级 ±50ms）、线程安全方式（compute + ReentrantLock）、核心方法 tryAcquire。新增「ErrorCode 区分策略」段落：InMemoryRateLimitGuard 返回 RATE_LIMITED；GlobalRateLimitFilter 返回 RATE_LIMITED_GLOBAL。4.1 节速率限制表全局限流行超额处理改为 RATE_LIMITED_GLOBAL。4.1 节 GlobalRateLimitFilter 超限返回错误码改为 RATE_LIMITED_GLOBAL。10.2 节 ErrorCode 表新增 RATE_LIMITED_GLOBAL 行 |
| Issue 7（一般）：3.4/7.4 节密码变更后前端恢复流程（清除标记→GET /api/auth/me→GET /api/menu/tree→跳转首页）未覆盖中间步骤失败的异常场景 | 3.4 节密码变更强制策略第 1 条末尾新增异常场景处理段落：GET /api/auth/me 或 GET /api/menu/tree 失败时重试最多 2 次（指数退避），重试耗尽后降级处理——清除 passwordChangeRequired 标记并跳转首页，后续自动重试。7.4 节密码修改成功后恢复流程条目同步更新相同异常处理逻辑 |
| Issue 8（一般）：8.3 节 A1 和 A3 行缺少「潜在副作用」和「影响范围」列，与 8.1/8.2 节格式不一致 | 8.3 节 A1 行补加"无（DDL 已就位）"和"User.java, schema.sql"。A3 行补加"需同步更新 schema.sql 确保与 Java 注解一致"和"Role.java, Post.java, schema.sql" |

## 23. 修订说明（v12）

| 审查意见 | 修改措施 |
|---------|---------|
| 问题1（重要）：登录流程未定义账户锁定时的返回行为。3.1.1 节步骤 2（即 LoginAttemptTracker 锁定状态检查）后未定义锁定命中分支；步骤 3 直接跳转到用户加载，暗示即使锁定也继续执行 | 3.1.1 节步骤 3（原 LoginAttemptTracker 检查行）补充锁定命中返回行为：新增双维度锁定检查结果分支。IP 维度锁定返回 ErrorCode.ACCOUNT_LOCKED（HTTP 429），消息"账户已锁定，请 30 分钟后重试"；用户名维度锁定返回 ErrorCode.ACCOUNT_LOCKED（HTTP 429），消息"账户已锁定，请 15 分钟后重试"。锁定命中时明确标注"不继续执行步骤 4-11" |
| 问题2（重要）：IP 维度锁定检查未在登录流程中显式体现。3.1.1 节步骤 2 仅检查用户名维度锁定状态，未提及 IP 维度 | 3.1.1 节步骤 3 将单一用户名维度检查扩展为双维度检查：先检查请求来源 IP 的锁定状态（20 次/30 分钟），再检查用户名的锁定状态（5 次/15 分钟）。同步修订 4.1 节第三层描述，将"检查锁定状态"明确为"检查双维度（IP 和用户名）锁定状态" |
| 问题3（一般）：M17/M18/M19 未出现在缺陷追踪表中，实施阶段可能遗漏回归验证 | 8.1 节 M10 行后补充 M17（Actuator 端点暴露）、M18（401 响应体结构不一致）、M19（用户不存在时菜单返回空数组）三条目的追踪行，包含当前状态、修复方案、潜在副作用、影响范围四列。M19 显式标注行为变更：从"返回空数组"变更为"返回 401 UNAUTHORIZED" |
