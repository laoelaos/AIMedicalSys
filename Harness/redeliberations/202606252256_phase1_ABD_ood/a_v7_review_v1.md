# OOD 设计方案审查报告（v7）

## 审查结果

REJECTED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 设计中所有类型形态选择（interface / class / record / enum / @Component class / OncePerRequestFilter 子类）均与 Java 17 类型系统能力匹配。单继承 + 多接口实现约束正确遵守。DTO 使用 Java 17 record 实现不可变数据传输，Jackson 序列化兼容。泛型使用方式（如 `Result<T>`、`Optional<User>`、`Set<String>`）均在泛型系统能力范围内。协作关系中的类型交互模式（Filter → TokenProvider → Blacklist → Repository → Entity）均可直接实现。

**[通过]** 设计中的抽象继承关系符合 Java 约束：Filters 继承 OncePerRequestFilter（单类继承），service 层面向接口编程（AuthService、TokenBlacklist、RateLimitGuard 等 interface 可多实现共存）。

### 2. 标准库与生态覆盖

**[通过]** 设计所需能力均在 Spring Boot + Java 17 标准生态覆盖范围内：
- Spring Security：SecurityFilterChain、OncePerRequestFilter、BCryptPasswordEncoder、AuthenticationEntryPoint、@EnableMethodSecurity、CSRF 配置
- Spring Data JPA：@Entity、@Column、@EntityGraph、Repository（含 Optional 返回）
- Bean Validation：@NotBlank、@Size、@Pattern、@Email、@Valid、@NotNull
- 并发集合：ConcurrentHashMap、ReentrantLock、ScheduledExecutorService
- Jackson：record 序列化/反序列化（record 不可变，需配置 Jackson 允许反序列化，但此属实现细节）
- Spring @Profile、@Component、@PostConstruct、@ConditionalOnProperty

**[通过]** 设计中的库能力假设合理——不假设任何第三方库或非标准能力。RateLimitGuard 的滑动窗口算法可通过标准 Java 集合实现。JWT 通过 `io.jsonwebtoken:jjwt` 或 `com.auth0:java-jwt` 等标准库即可实现。

### 3. 语言特性可行性

**[通过]** 错误处理策略与 Java 错误处理能力匹配：BusinessException 模式、AuthenticationException、GlobalExceptionHandler 统一捕获、Result<T> 统一响应格式均由 Spring 标准机制支持。

**[通过]** 并发设计可行：ConcurrentHashMap + compute 原子操作确保 LoginAttemptTracker 线程安全。ReentrantLock 保护滑动窗口排序集合。ScheduledExecutorService 单线程定时清理。Token 黑名单使用 ConcurrentHashMap 线程安全操作。

**[通过]** 资源管理方案可行：InMemoryTokenBlacklist 基于 ConcurrentHashMap，定时任务清理。内存估算（~6.5MB）合理。

**[通过]** 模块/包结构设计符合 Java + Spring Boot 多模块项目组织方式：common-module-impl 下 auth/menu/permission/config 子包划分清晰。依赖方向明确（application → common-module-impl → common-module-api），无循环依赖。业务模块仅依赖 common-module-api 中的共享类型。

### 4. 设计一致性

**[通过]** 各抽象职责描述清晰无歧义：1.3 节核心抽象一览表、3.3 节各 Filter 行为契约、3.1 节全流程步骤描述均足够具体以指导后续实现。

**[通过]** 协作关系形成闭环：登录（3.1.1）→ 已认证请求（3.1.2）→ 刷新（3.1.3）→ 登出（3.1.4）→ 获取当前用户（3.1.5）→ 密码变更（3.1.6）覆盖全部认证生命周期，无缺失环节。

**[通过]** 行为契约描述完整：JwtAuthenticationFilter 和 PasswordChangeCheckFilter 均提供完整的 doFilterInternal 步骤式契约。JwtTokenProvider 的 @PostConstruct 启动验证逻辑有具体实现约束（密钥长度、字符集）。

**[通过]** 模块间依赖方向合理：application → common-module-impl（含 auth/permission/menu）→ common-module-api（共享枚举）。无循环依赖。

**[一般]** 10.2 节 ErrorCode 表与 4.3 节密码策略 ErrorCode 定义不一致：
- 4.3 节密码策略表定义了 5 个具体错误码：`PASSWORD_TOO_SHORT`、`PASSWORD_TOO_LONG`、`PASSWORD_WEAK`、`PASSWORD_CONTAINS_USERNAME`、`PASSWORD_COMMON`
- 10.2 节仅列出 `PASSWORD_TOO_WEAK`，且命名与 4.3 节的 `PASSWORD_WEAK` 不统一
- 10.1 节错误分类表已正确列出 5 个具体错误码及其 HTTP 200 映射，但 10.2 节未同步更新
- 影响：实现者会因两个章节定义不一致而使用错误码名或遗漏具体错误码

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则：
- JwtAuthenticationFilter：仅负责 JWT 鉴权 + 用户 enabled 检查
- PasswordChangeCheckFilter：仅负责 passwordChangeRequired 业务阻断
- GlobalRateLimitFilter：仅负责全局限流
- AuthServiceImpl：仅负责认证业务逻辑
- JwtTokenProvider：仅负责 JWT 生命周期管理

**[通过]** 抽象层次恰当：
- TokenBlacklist / RateLimitGuard 使用接口抽象，支持 Phase 1（内存）→ Phase 2（Redis）平滑迁移
- 不过度：Phase 1 未引入 Redis 或独立 Token 持久化表
- 不不足：所有关键协作关系有明确抽象定义

**[通过]** 设计便于后续详细设计和实现：每个 Filter 有完整步骤式契约，DTO 以代码形式定义，流程步骤有明确输入输出。

**[通过]** 设计便于单元测试：关键依赖均通过接口注入（AuthService、TokenBlacklist、RateLimitGuard、PasswordPolicy），可 mock 隔离。M1（buildUserInfoResponse 私有方法不可测试）已通过抽取至 UserConverter 解决。

## 修改要求（REJECTED 时存在）

### 问题 1：10.2 节 ErrorCode 表与 4.3 节密码策略 ErrorCode 定义不一致

- **问题**：4.3 节密码策略定义了 5 个具体错误码（PASSWORD_TOO_SHORT、PASSWORD_TOO_LONG、PASSWORD_WEAK、PASSWORD_CONTAINS_USERNAME、PASSWORD_COMMON），但 10.2 节仅列出 PASSWORD_TOO_WEAK（命名也不一致），10.1 节已正确列出这些错误码。
- **原因**：设计文档内部不一致会导致实现者混淆：是按 10.2 节的单个 PASSWORD_TOO_WEAK 实现，还是按 4.3 节的 5 个具体错误码实现？ErrorCode 是前后端共享契约，不一致直接影响前后端协作。
- **建议方向**：10.2 节 ErrorCode 表按 4.3 节（或 10.1 节）的具体错误码列表同步更新，将 PASSWORD_TOO_WEAK 替换为 PASSWORD_TOO_SHORT、PASSWORD_TOO_LONG、PASSWORD_WEAK、PASSWORD_CONTAINS_USERNAME、PASSWORD_COMMON，并补充对应消息和触发场景说明。
