# OOD 设计方案审查报告（v2）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 设计中使用的类型形态（interface、class、Java 17 record）均与 Java 17 类型系统能力完全匹配。interface 定义的契约（AuthService、RateLimitGuard、TokenBlacklist、PasswordPolicy、PasswordChangeService、CurrentUser）均可在 Java 中通过 implements 实现。单继承 + 多接口实现的约束被严格遵守。DTO 统一使用 Java 17 record，Jackson 反序列化兼容性已有社区验证（需确保 record 有规范构造器或启用 Jackson 的 `@JsonCreator` 支持）。泛型使用方式（`Optional<User>`、`Result<T>`）完全在 Java 泛型系统能力范围内。各抽象间的继承和实现关系（OncePerRequestFilter → JwtAuthenticationFilter、interface → impl class）均为标准 Java 模式。

**[轻微]** `InMemoryRateLimitGuard` 的滑动窗口使用 `synchronized` 或 `ReentrantLock` 保护，若后续迁移至更高并发场景建议改用 `ConcurrentSkipListMap` + `LongAdder` 的组合减少锁竞争，但当前设计级别不需要此项优化。

### 2. 标准库与生态覆盖

**[通过]** 设计中依赖的所有核心能力均有标准库或 Spring 生态成熟覆盖：JWT 令牌管理（jjwt 或 Spring Security OAuth2 Resource Server）、密码加密（BCryptPasswordEncoder，Spring Security）、Bean 校验（Jakarta Bean Validation `@NotBlank`/`@Pattern`/`@Email`）、并发集合（ConcurrentHashMap、ScheduledExecutorService，JDK 标准库）、Web 安全配置（SecurityFilterChain、CORS、CSRF，Spring Security）、JPA/Hibernate 实体管理（`@EntityGraph` 解决 N+1 问题）、序列化（Jackson，Spring Boot 默认）。所有库假设合理，无超出生态覆盖范围的依赖。

### 3. 语言特性可行性

**[通过]** 错误处理策略与 Java 异常机制完全匹配：BusinessException（继承 RuntimeException）由 GlobalExceptionHandler 的 `@ExceptionHandler` 统一捕获；认证/授权异常由 Spring Security 的 AuthenticationEntryPoint 和 AccessDeniedHandler 处理；验证异常由 `@Valid` + MethodArgumentNotValidException 处理。并发设计使用 ConcurrentHashMap + `compute` 原子操作 + ScheduledExecutorService 定时清理，与 JDK 并发模型完全兼容。资源管理为纯内存操作（无文件句柄/网络连接等的显式管理），不存在资源泄漏风险。模块组织为 Maven 多模块 + `@Profile` 隔离，符合项目既有风格。

### 4. 设计一致性

**[通过]** 各抽象职责描述清晰且互不重叠：AuthService 专注认证业务编排、JwtTokenProvider 专注令牌生命周期、RateLimitGuard 专注限流策略、TokenBlacklist 专注 blacklist 查询、PasswordPolicy 专注密码校验规则、PasswordChangeService 专注密码变更策略。协作关系形成闭环：AuthServiceImpl 编排 LoginAttemptTracker、RateLimitGuard、PasswordChangeService、JwtTokenProvider、UserRepository；JwtAuthenticationFilter 编排 JwtTokenProvider.validateToken、TokenBlacklist.isBlacklisted、UserRepository.findById。行为契约完整到足以指导实现（Filter 的 8 步行为序列、登录的 11 步流程均已逐步骤描述）。模块依赖方向明确（application → common-module-impl → common-module-api），无循环依赖。前后端字段名对齐（realName/role）和 Breaking Change 声明形成一致契约。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则：每个抽象的行为边界清晰，无职责混杂。抽象层次恰当：未过度设计（如未为黑名单引入工厂模式、未为 JWT 增加策略模式），也未设计不足（防暴力破解三层防护的 RateLimitGuard/LoginAttemptTracker 均有独立抽象）。设计便于后续实现：核心核心流程（登录/刷新/登出/Filter）均以逐步序列描述，可直接转化为方法签名和调用链。设计便于单元测试：RateLimitGuard、TokenBlacklist、PasswordPolicy、PasswordChangeService、CurrentUser 均为 interface，可轻松 mock；JwtTokenProvider 可提供预配置 SecretKey 用于测试；Filter 逻辑可通过 SecurityContextHolder 的测试工具验证。

## 修改要求

无。所有维度通过审查。

## 备注

v2 设计方案已完整响应 a_v2_iteration_requirement.md 中列出的全部 12 条审查意见（P1-P14，不含编号跳跃），包括：修正事实性错误（P1/P9）、补全 PasswordChangeRequest DTO（P3）、改为仅黑名单 Access Token 的内存方案（P5）、新增 PasswordChangeService 密码过渡方案（P2）、定义 RefreshTokenRequest（P4）、对齐前后端字段名并声明 Breaking Change（P6/P7）、说明 Filter 迁移步骤（P8）、refresh 端点统一为 permitAll（P13）、指定菜单路由展平策略（P14）、明确 SidebarBase props 定义（P10）、合并重复问题条目（P11）、补加 Role/Post 的 `@Column(nullable=false)`（P12）。
