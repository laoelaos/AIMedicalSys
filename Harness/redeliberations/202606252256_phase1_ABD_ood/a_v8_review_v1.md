# OOD 设计方案审查报告（v9）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 设计中的类型形态选择（interface / class / Java 17 record / @Component / @Configuration）均与 Java 17 + Spring Boot 类型系统能力完全匹配。单继承（OncePerRequestFilter）+ 多接口实现（interface）的使用模式符合约束。泛型使用仅限于标准库泛型（ConcurrentHashMap<K,V>、Deque<Long> 等），均在泛型系统能力范围内。协作关系中描述的 Filter 链、Service → Repository 交互、record DTO 传输等交互模式均可在 Java 中实现。

**[通过]** Refresh Token 检测使用 `ConcurrentHashMap<Long, Deque<Long>>` 以及滑动窗口模式，Java 类型系统完全支持。

### 2. 标准库与生态覆盖

**[通过]** 设计中所需的核心能力均在 Java/Spring Boot 标准库或生态范围内：
- JWT 令牌管理：io.jsonwebtoken (jjwt) 或 Spring Security OAuth2 Resource Server
- Spring Security：SecurityFilterChain、OncePerRequestFilter、BCryptPasswordEncoder、@EnableMethodSecurity、@Profile
- Spring Data JPA：@EntityGraph、Repository 泛型、JPA 脏检查
- 并发数据结构：ConcurrentHashMap、ReentrantLock、ScheduledExecutorService
- Bean Validation：jakarta.validation 注解（@NotBlank、@Size、@Pattern、@Email）
- Jackson：Java 17 record 序列化/反序列化（Spring Boot 已内置）
- Sliding window 限流：基于标准库自行实现（ConcurrentHashMap + Deque）

**[通过]** 设计中假设的库能力均是标准 Spring Boot 技术栈，无不合理假设。

**[通过]** 标准库能力已在多处简化设计（如 Spring Security Filter 链替代自定义认证处理、BCryptPasswordEncoder 替代自定义加密、JPA 脏检查替代显式 save()）。

### 3. 语言特性可行性

**[通过]** 错误处理策略与 Java/Spring 能力匹配：BusinessException + @RestControllerAdvice + AuthenticationEntryPoint + AccessDeniedHandler 的四层错误处理体系是 Spring Boot 标准实践。ErrorCode 枚举清晰划分了认证/授权/业务/限流类异常。

**[通过]** 并发设计与 Java 并发模型兼容：ConcurrentHashMap.compute 原子操作、ReentrantLock 保护滑动窗口、ScheduledExecutorService 定时清理，均为 Java 标准并发原语。

**[通过]** 资源管理方案可行：内存黑名单通过 ScheduledExecutorService 定期清理过期条目；登出流程使用 try-finally 确保资源释放。

**[通过]** 模块/包结构符合 Maven 多模块 + Spring Boot 项目组织方式：common-module-impl 内部按功能分包（auth/security/rateLimit/blacklist/password/dto/converter），清晰可控。Filter 迁移策略（application → common-module-impl）有明确的步骤说明。

### 4. 设计一致性

**[通过]** 各抽象的职责描述清晰无歧义。核心抽象一览表（1.3 节）明确了每种抽象的类型形态和职责定位。Filter 链的顺序和行为契约（3.3 节）对 JwtAuthenticationFilter、PasswordChangeCheckFilter、GlobalRateLimitFilter 各自职责和交互方式做了完整描述。

**[通过]** 协作关系形成闭环。7 个审查问题全部在本次迭代中闭环：
- 问题 1（异常刷新检测）：4.2 节补充了 5 秒时间窗口、2 次阈值、AuthServiceImpl.refresh() 实现位置、ConcurrentHashMap<Long, Deque<Long>> 数据结构、log.warn + Prometheus/AlertManager 告警方式
- 问题 2（inject('router')）：8.2 节 H6 替换为 createMenuStore(router, dynamicPageComponent) 工厂模式，引用已有实现
- 问题 3（管理员标记密码过期 API）：3.4 节场景 2 标注"属于管理端设计范围"
- 问题 4（@NotBlank）：5.2 节 ProfileUpdateRequest 已补充
- 问题 5（PUT 语义）：5.2 节 MenuUpdateRequest 声明为局部更新语义
- 问题 6（登出 token 过期行为）：3.1.4 节步骤 5 补充 finally 块清理
- 问题 7（LoginAttemptCleaner）：2.1 节目录结构中删除此条目

**[通过]** 行为契约完整：Filter 行为契约（3.3 节）包含完整的步骤序列。认证流程（3.1 节）对登录/登出/刷新/获取用户/密码变更均有结构化描述。

**[通过]** 模块间依赖方向合理，无循环依赖：auth → permission（同模块内部），application → common-module-impl，业务模块仅依赖 common-module-api。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则：
- JwtAuthenticationFilter：仅负责 JWT 鉴权
- PasswordChangeCheckFilter：仅负责 passwordChangeRequired 业务规则检查
- GlobalRateLimitFilter：仅负责全局限流
- PasswordPolicy interface：密码复杂度策略可替换
- PasswordChangeService interface：密码变更策略契约
- CurrentUser interface：消除 Controller 对 SecurityContextHolder 的直接操作

**[通过]** 抽象层次恰当：设计停留在架构级抽象（接口定义、Filter 链顺序、模块依赖、流程描述），未过度设计到具体字段级实现细节。DTO record 设计给出关键注解但未过度约束。

**[通过]** 设计便于后续详细设计和实现：接口定义清晰（AuthService 定义了完整的认证方法边界）、Filter 链顺序明确、模块依赖方向已固定。Phase 1 → Phase 2 的演进路径已在多处标注（Redis 迁移、Refresh Token 黑名单启用）。

**[通过]** 设计便于单元测试：UserConverter 可独立测试；PasswordPolicy 接口化便于 mock；Service 接口定义清晰可 mock；Filter 行为有明确输入/输出契约，可通过 MockHttpServletRequest/Response 测试。问题 M1（buildUserInfoResponse 不可测试）已在 8.1 节中明确修复方案（提取为包级私有或 UserConverter）。

## 修改要求

无。设计通过全部五个维度的审查。

## 注意事项

- 本设计为架构级 OOD，缺少的字段级实现细节（如具体方法签名、SQL 语句）属于正常范围，不应作为驳回理由
- 问题 3（管理员标记密码过期 API）的决策——标注为"管理端设计范围，本设计不做具体接口定义"——是一个合理的架构边界决策，防止包 B 设计僭越其职责范围
- 问题 5（MenuUpdateRequest PUT 语义）采用局部更新语义，这是合理的架构设计选择，与 RESTful 约定略有偏差但已在文档中明确声明，可被后续实现者理解
