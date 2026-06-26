# OOD 设计方案审查报告（v10）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 设计中的类型形态选择（interface / class / @Component / record / enum）均与 Java 17 类型系统能力完全匹配。interface 用于定义可替换策略（RateLimitGuard、TokenBlacklist、PasswordPolicy、PasswordChangeService、AuthService），class + @Component 用于 Spring 托管组件（JwtTokenProvider、SecurityConfigPhase1、Filter 等），Java 17 record 用于 DTO，enum 用于 ErrorCode——选择均恰当。继承关系合规：Filter 继承 OncePerRequestFilter（抽象类单继承）、多接口实现无冲突。泛型使用规范（Result<T>、JpaRepository<...> 等均在 Spring Data 框架支持范围内）。CurrentUser interface 作为 SecurityContextHolder 的轻量级门面，模式成熟可行。

**[通过]** 协作关系中描述的类型交互（Service → Repository → Entity、Filter → TokenBlacklist/RateLimitGuard 等）均可在 Java 类型系统中直接实现。ConcurrentHashMap、ReentrantLock、ScheduledExecutorService、Deque 等并发容器使用符合 JDK 标准库类型约束。

### 2. 标准库与生态覆盖

**[通过]** 设计中的全部能力均在 Java 17 + Spring Boot 3.x + Spring Security 的标准生态覆盖范围内：
- JWT 处理可通过 jjwt (io.jsonwebtoken) 或 Spring Security 内置的 OAuth2 Resource Server 实现
- BCrypt 密码编码由 Spring Security 的 BCryptPasswordEncoder 直接提供
- Bean Validation（@NotBlank、@Size、@Pattern、@Email）由 Jakarta Validation + Hibernate Validator 提供，Java 17 record 已支持
- 并发工具（ConcurrentHashMap、ReentrantLock、ScheduledExecutorService）均为 JDK 标准库
- @EntityGraph 用于 JPA fetch join 优化，标准 Spring Data JPA 能力
- Jackson record 序列化/反序列化由 Spring Boot 3.x 自动配置（需确认 jackson-datatype-jdk8Module 注册）

**[通过]** 未发现不合理的库能力假设。所有第三方依赖（Spring、jjwt、jakarta.validation）均为后端项目的常用依赖。自定义抽象的设计层次适当，未将标准库可提供的能力不必要地重复发明。

### 3. 语言特性可行性

**[通过]** 错误处理策略与 Java 异常体系完全匹配：BusinessException（运行时异常）配合 ErrorCode 枚举 + GlobalExceptionHandler（@ControllerAdvice）提供统一错误响应；AuthenticationException（Spring Security 内置）用于认证异常；AccessDeniedException 用于授权异常。分类清晰且与 Spring Security 异常链兼容。

**[通过]** 并发设计合理：ConcurrentHashMap 原子操作方法（compute、computeIfAbsent）保证 LoginAttemptTracker 和 InMemoryTokenBlacklist 的线程安全；滑动窗口使用 ReentrantLock 保护排序集合（公平锁 + tryLock 超时）；ScheduledExecutorService 单线程执行器处理定期清理，不与业务线程并发冲突。所有并发控制均在 JDK 标准并发工具能力范围内。

**[通过]** 资源管理方案可行：Phase 1 内存数据结构（InMemoryTokenBlacklist ~6.5MB、LoginAttemptTracker 按活跃用户线性增长）均在 JVM 堆可承受范围内；定时清理任务保证过期条目回收。Phase 2 迁移 Redis 时接口不变——设计已预留扩展点。

**[通过]** 模块/包结构清晰：common-module-impl/auth/ 下按职责划分子包（controller/service/jwt/security/rateLimit/login/blacklist/password/dto/converter），符合 Java 包命名规范和 Spring 项目组织惯例。依赖方向明确单向（auth → permission, menu → permission, security → jwt），无循环依赖。

### 4. 设计一致性

**[通过]** 各抽象的职责描述清晰无歧义。18 个核心抽象（AuthService、JwtTokenProvider、JwtAuthenticationFilter、PasswordChangeCheckFilter、GlobalRateLimitFilter、RateLimitGuard、LoginAttemptTracker、TokenBlacklist、PasswordPolicy、PasswordChangeService、SecurityConfigPhase1、CurrentUser 等）职责边界明确，符合单一职责原则。核心抽象一览表（1.3 节）提供了快速全局理解。

**[通过]** 协作关系形成闭环：
- 登录流程（3.1.1）：RateLimitGuard → LoginAttemptTracker → AuthServiceImpl(login) → JwtTokenProvider → PasswordChangeService → LoginResponse，11 步完整无缺失
- Token 刷新流程（3.1.3）：JwtTokenProvider(验证) → DB 加载用户 → LoginAttemptTracker(锁定检查) → enabled/deleted/tokenVersion 检查 → 轮换签发新 token → 前端 GET /api/auth/me → 完整闭环
- 登出流程（3.1.4）：jti 黑名单 → finally 清除前端 → 兜底设计闭环
- 密码变更流程（3.1.6）：旧密码验证 → 复杂度校验 → encode → tokenVersion 递增 → 前端恢复流程（GET /api/auth/me → GET /api/menu/tree → 跳转首页）— 完整

**[通过]** 行为契约描述充分：JwtAuthenticationFilter（9 步）、PasswordChangeCheckFilter（3 步）均有详细的行为契约（doFilterInternal 步骤清单）。每个 Filter 的静默跳过策略、异常抛出条件、SecurityContext 设置规则均有明确文档。

**[通过]** 模块间依赖方向合理：auth/ → permission/（单向）、menu/ → permission/（单向）、security/ → jwt/（单向）。无循环依赖。跨模块依赖通过 common-module-api 门面接口隔离（业务模块不直接依赖 common-module-impl）。

**[通过]** 四个历史问题（来自第 9 轮审查）均已在 v10 中解决：
- 问题 1（"连续"语义）：4.1 节已简化为"时间窗口内累计失败次数"，与 AttemptRecord 数据结构一致——已解决
- 问题 2（刷新端点锁定检查）：3.1.3 节步骤 6 新增账户锁定检查——已解决
- 问题 3（版本号不一致）：文档标题已统一为 v10——已解决
- 问题 4（SecurityContext 描述）：3.1.6 节步骤 10 已修改为准确描述——已解决

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则：
- JwtAuthenticationFilter：仅负责 JWT 鉴权（提取 token、验证、查库、装配 SecurityContext）
- PasswordChangeCheckFilter：仅负责 passwordChangeRequired 阻断检查
- GlobalRateLimitFilter：仅负责全局限流
- InMemoryRateLimitGuard：仅负责登录端点限流
- LoginAttemptTracker：仅负责失败计数 + 锁定状态管理
- TokenBlacklist：仅负责黑名单增删查
- 每个抽象职责边界清晰，无职责重叠

**[通过]** 抽象层次恰当：
- 策略接口（RateLimitGuard、TokenBlacklist、PasswordPolicy、PasswordChangeService）提供 Phase 1 → Phase 2 的平滑迁移路径，不过早抽象
- Filter 继承 OncePerRequestFilter 是 Spring Security 标准实践，无需额外抽象
- CurrentUser 接口消除 Controller 层对 SecurityContextHolder 的直接操作，适当的间接层
- 未出现"接口泛滥"（每个接口都有明确的 Phase 2 演进需求）或"设计不足"

**[通过]** 设计便于后续详细设计和实现：每个核心抽象在 2.1 节有明确的目录位置和文件名，3.3 节有行为契约步骤详解，5.2 节有完整 DTO record 定义（含字段和注解），6 节有完整 API 接口清单和 JSON 示例。实现者可直接将文档中的定义映射为代码。

**[通过]** 设计便于单元测试：
- 所有核心策略均为 interface（AuthService、RateLimitGuard、TokenBlacklist、PasswordPolicy、PasswordChangeService），可 mock
- InMemory 实现可独立测试（InMemoryTokenBlacklistTest、InMemoryRateLimitGuardTest、LoginAttemptTrackerTest）
- JwtTokenProvider 可用已知密钥测试
- UserConverter 已从 AuthServiceImpl 提取为独立转换器（M1 修复），可单独单元测试
- Filter 测试可使用 Spring MockMvc + MockBean 策略注入

## 修改要求

无。本设计 APPROVED，不存在需要驳回的严重或一般问题。

所有四个历史问题均已在 v10 修订中妥善解决：
1. "连续"语义定义与 AttemptRecord 实现机制不匹配 → 4.1 节已修正为时间窗口累计计数语义
2. 刷新端点未检查账户锁定状态 → 3.1.3 节步骤 6 已新增锁定检查
3. 版本号不一致 → 文档标题已统一
4. SecurityContext 清除说明不准确 → 3.1.6 节步骤 10 已修正
