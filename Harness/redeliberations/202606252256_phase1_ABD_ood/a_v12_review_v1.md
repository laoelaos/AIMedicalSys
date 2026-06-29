# OOD 设计方案审查报告（v12）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 设计中使用的类型形态（interface / @Component class / Java 17 record / enum / abstract class）与 Java 17 + Spring Boot 3.x 类型系统完全匹配。interface 定义的契约抽象（AuthService、RateLimitGuard、TokenBlacklist、PasswordPolicy、PasswordChangeService、CurrentUser）为标准的 Java 接口继承与多态模式。Java 17 record 用于 DTO 层，与 Jackson 序列化兼容（record 的不可变性与 @NotBlank/@Pattern 等 Jakarta Validation 注解可共存）。@Column/@Entity/@EntityGraph 等 JPA 注解方案在 Spring Data JPA 中已成熟支持。ConcurrentHashMap、Deque、ScheduledExecutorService 均为标准 Java 并发集合，使用方式正确。PasswordChangeRequiredException 继承 AccessDeniedException 在 Spring Security 体系中可行。

**[通过]** 单继承+多接口实现的约束被正确遵循——所有实体继承 BaseEntity，Filter 类各自独立继承 OncePerRequestFilter 或实现特定接口。

**[通过]** 泛型使用方式（如 Result<T>、List<MenuResponse> 递归结构）均在 Java 泛型系统能力范围内。

### 2. 标准库与生态覆盖

**[通过]** 设计依赖的全部组件均在 Spring Boot 标准生态覆盖范围内：Spring Security（JWT Filter、SecurityFilterChain、AuthenticationEntryPoint、AccessDeniedHandler、@EnableMethodSecurity、BCryptPasswordEncoder）、Spring Data JPA（@EntityGraph 避免 N+1、Optional 返回类型）、Jakarta Bean Validation（@NotBlank/@Size/@Pattern/@Email）、Jackson（record 序列化/反序列化）、并发工具（ConcurrentHashMap、ScheduledExecutorService、ReentrantLock）。JWT 操作通过 JJWT 库实现，为 Spring Boot 生态中主流选择。

**[通过]** 设计假设的库能力（BCrypt 加密、滑动窗口限流、内存黑名单）均可通过 Spring Security 和标准 Java 并发库实现，假设合理。

**[通过]** 设计正确利用了标准库能力简化自定义抽象：如利用 Spring Security 的 ExceptionTranslationFilter 处理 401 静默跳过策略、利用 SecurityContextHolder 管理认证状态。

### 3. 语言特性可行性

**[通过]** 错误处理策略与 Java/Spring 能力匹配：BusinessException + GlobalExceptionHandler 模式、AuthenticationException（Spring Security）由 AuthenticationEntryPoint 处理、AccessDeniedException（含自定义 PasswordChangeRequiredException 子类）由 AccessDeniedHandler 处理、RateLimitExceededException 可直接在 Filter 层处理。分类清晰，与 Spring Security 异常处理体系集成良好。

**[通过]** 并发设计完整可行：InMemoryTokenBlacklist 使用 ConcurrentHashMap + ScheduledExecutorService 定时清理，InMemoryRateLimitGuard 使用 ReentrantLock 保护滑动窗口内排序集合（选择 ReentrantLock 的理由充分——公平锁特性防止窗口清理线程被长期抢占、tryLock 支持超时机制），LoginAttemptTracker 通过 ConcurrentHashMap.compute 原子性操作。细粒度锁设计（每个 IP 独立锁）合理，竞态分析符合 Java 内存模型。

**[通过]** 资源管理方案可行：内存黑名单定时通过 ScheduledExecutorService 清理过期条目（每 5 分钟）、限流窗口惰性清理（每 60 秒）。估算的内存上限（Access Token 黑名单 ~6.5MB）在典型 JVM 堆范围内容忍。

**[通过]** 模块/包结构设计符合 Maven/Gradle 多模块项目组织标准：common-module-impl 内含 auth/、menu/、permission/ 等子包，依赖方向清晰（auth → permission, security → jwt），业务模块仅依赖 common-module-api。迁移步骤说明（旧 Filter 引用删除 → 新 Filter 替代）具体可落地。

### 4. 设计一致性

**[通过]** 各抽象职责描述清晰无歧义：核心抽象一览表（1.3 节）为每种抽象明确类型形态和职责定位，后续章节逐一定义行为契约（JwtAuthenticationFilter 9 步契约、PasswordChangeCheckFilter 4 步契约、SlidingWindowCounter 4 项契约）。

**[通过]** 协作关系形成闭环：登录流程（3.1.1）→ 认证请求流程（3.1.2）→ Token 刷新流程（3.1.3）→ 登出流程（3.1.4）→ 获取当前用户（3.1.5）→ 密码变更流程（3.1.6）覆盖了认证模块全生命周期，无缺失环节。

**[通过]** 行为契约完整可指导实现：每个 Filter 的 doFilterInternal 以结构化步骤定义，SecurityConfig 以伪代码形式定义 FilterChain 注册顺序，API 接口清单（6.1 节）包含方法/路径/请求体/响应码/说明。DTO 定义以 Java record 形式给出。

**[通过]** 模块间依赖方向合理无循环依赖：application → common-module-impl → common-module-api，且明确标注业务模块不依赖 common-module-impl。包内 auth → permission 为同模块内部依赖，不构成跨模块耦合。

**[确认]** v11 迭代中的三个持续问题（账户锁定返回行为缺失、IP 维度检查缺失、M17/M18/M19 追踪表缺失）已在 v12 中全部修复：3.1.1 节步骤 3 已补充双维度锁定检查及 ACCOUNT_LOCKED 返回行为；8.1 节已补充 M17/M18/M19 追踪行。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则：JwtAuthenticationFilter（仅负责 JWT 鉴权）、PasswordChangeCheckFilter（仅负责密码变更业务规则检查，通过 request attribute 避免重复查库）、GlobalRateLimitFilter（仅负责全局限流）、RateLimitGuard/LoginAttemptTracker/TokenBlacklist 各自封装独立职责。JwtTokenProvider 集中管理 JWT 生命周期，PasswordPolicy 隔离密码规则。

**[通过]** 抽象层次恰当：Phase 1 使用内存实现（InMemoryRateLimitGuard、InMemoryTokenBlacklist）避免过度设计；设计文档中明确 Phase 2 迁移 Redis 的接口不变方案（仅新增 Redis 实现类通过 Profile/ @ConditionalOnProperty 切换），体现适度的前瞻设计。

**[通过]** 设计便于后续实现：行为契约结构化，DTO 以 record 定义可直接复制为代码，SecurityConfig 以伪代码形式给出 Filter 顺序。模块依赖说明中包含具体迁移步骤（旧 Filter 删除→新 Filter 替代）。

**[通过]** 设计便于单元测试：AuthService/PasswordPolicy/RateLimitGuard/TokenBlacklist/PasswordChangeService 均为 interface，可轻松 Mock。UserConverter 抽取为独立类（M1 修复方案）。JwtTokenProvider 的 SecretKey 缓存和验证逻辑可独立测试。CurrentUser 接口封装 SecurityContextHolder 访问，Controller 层可 Mock。

**[轻微]** 建议考虑为 SlidingWindowCounter 补充单元测试契约（在后续实现阶段），以确保滑动窗口算法在不同并发竞争条件下的正确性。该建议为可改进但不阻塞。
