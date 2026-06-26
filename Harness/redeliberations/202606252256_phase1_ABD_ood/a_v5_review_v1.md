# OOD 设计方案审查报告（v5）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 全部类型形态选择与 Java 17 类型系统能力匹配：
- DTO 统一使用 Java 17 record（不可变、紧凑、Jackson 兼容），Spring Boot 3.2.5 原生支持 record 序列化/反序列化
- 服务契约使用 interface（AuthService、RateLimitGuard、TokenBlacklist、PasswordPolicy、PasswordChangeService、CurrentUser），利于多实现切换和测试 mock
- Filter 继承 OncePerRequestFilter（JwtAuthenticationFilter、PasswordChangeCheckFilter、GlobalRateLimitFilter），符合 Spring Security 扩展机制
- 限流/黑名单/失败计数使用 class 直接实现（InMemoryRateLimitGuard、InMemoryTokenBlacklist、LoginAttemptTracker），无过度抽象
- 单继承 + 多接口实现约束完全满足
- MenuResponse 自引用（`List<MenuResponse> children`）在 Java record 中合法，Jackson 递归序列化/反序列化无阻碍
- ErrorCode 枚举语义清晰，使用范围边界已定义

### 2. 标准库与生态覆盖

**[通过]** 设计所需能力均在现有依赖范围内：
- `ConcurrentHashMap`、`ScheduledExecutorService`、`ReentrantLock` — java.util.concurrent 标准库，已有运行时依赖
- Spring Security 组件（SecurityFilterChain、OncePerRequestFilter、AuthenticationEntryPoint、BCryptPasswordEncoder、SecurityContextHolder）— `spring-boot-starter-security` 已声明
- JPA 注解（`@Column`、`@EntityGraph`）— `spring-boot-starter-data-jpa` 已声明
- jjwt 0.12.5 — 依赖已显式声明（api/impl/jackson 三件套）
- Bean Validation（`@NotBlank`、`@NotNull`、`@Size`、`@Pattern`、`@Email`）— `spring-boot-starter-validation` 已声明
- Java 17 语言特性（records、switch 等）— `<java.version>17</java.version>` 已配置

### 3. 语言特性可行性

**[通过]**
- **错误处理**：BusinessException + ErrorCode 枚举 + GlobalExceptionHandler + AuthenticationEntryPoint 的分层处理策略，符合 Spring Boot 惯例。ACCOUNT_DISABLED 在 JwtAuthenticationFilter 步骤 4b（enabled=false）抛出 AuthenticationException，由 AuthenticationEntryPoint 捕获返回 401 + 专用错误码，与 P2 修复后的设计一致，已消除不可达问题
- **并发设计**：ConcurrentHashMap.compute() 保证 LoginAttemptTracker 原子性；ReentrantLock 保护 InMemoryRateLimitGuard 滑动窗口；单线程 ScheduledExecutorService 运行清理任务，无并发竞争
- **资源管理**：InMemoryTokenBlacklist 约 90,000 条目 / 6.5MB 峰值内存（100 req/s × 15min），定时回收机制成熟；LoginAttemptTracker 条目数有限（用户数 + 攻击 IP 数），均在 JVM 可承受范围内
- **模块/包结构**：遵循现有 Maven 多模块布局（common-module-impl 内 auth/menu/permission 分包），不引入新的模块层

### 4. 设计一致性

**[通过]**
- 模块职责边界清晰：auth/ 负责认证流程与令牌管理，menu/ 负责菜单 CRUD，permission/ 负责实体与 Repository，security/ 负责 Filter 编排
- 依赖方向无环：auth/ → permission/，menu/ → permission/，security/ → jwt/；application 通过 @ComponentScan/@Import 引入 common-module-impl，业务模块不直接依赖认证实现
- 认证流程形成完整闭环：登录 → 颁发 JWT → Filter 鉴权 → 业务请求 → 刷新/登出，各环节衔接无缺失
- Filter 编排顺序明确：GlobalRateLimitFilter → JwtAuthenticationFilter → PasswordChangeCheckFilter → UsernamePasswordAuthenticationFilter，每个 Filter 的触发条件、跳过条件、异常处理均已定义
- 错误码与使用场景一一对应，无悬空 ErrorCode（ACCOUNT_DISABLED 的触发路径已在 P2 修复中闭合；PASSWORD_CHANGE_REQUIRED 已有专用 Filter 和专用 API 端点白名单）

### 5. 设计质量

**[通过]**
- **单一职责**：Filter 按关注点分离（限流/鉴权/密码变更检查）；AuthService 聚焦认证业务；JwtTokenProvider 聚焦令牌生命周期；限流策略接口化不耦合具体实现
- **抽象层次恰当**：RateLimitGuard/TokenBlacklist/PasswordPolicy 接口化为后续 Redis 迁移预留扩展点；LoginAttemptTracker 等无多实现需求的组件直接使用 class，无过度设计
- **可测试性**：全部服务依赖通过接口注入（AuthService、RateLimitGuard、TokenBlacklist、PasswordPolicy、PasswordChangeService、CurrentUser），DTO 为不可变 record，便于单元测试和 mock
- **可扩展性**：Profile 切换（phase0/phase1/prod）隔离不同环境 SecurityConfig；Phase 2 Redis 迁移只需新增实现类，接口不变；SecurityFilterChain 通过 addFilterBefore/addFilterAfter 组合，新 Filter 可在不修改现有 Filter 的前提下插入链

## 修改要求

无。设计通过全部五个维度的审查，无严重或一般问题。
