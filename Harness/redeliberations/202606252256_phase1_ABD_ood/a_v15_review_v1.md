# OOD 设计方案审查报告（v15）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 接口抽象（AuthService、RateLimitGuard、TokenBlacklist、PasswordPolicy、PasswordChangeService、CurrentUser、UserFacade）全部使用 Java `interface`，与类型系统匹配

**[通过]** OncePerRequestFilter 过滤器继承、PasswordChangeRequiredException 继承 AccessDeniedException 均符合 Java 单继承约束，Spring Security 框架模式标准

**[通过]** Java 17 record 用于 DTO（LoginRequest、LoginResponse、RefreshTokenRequest 等），Jackson 反序列化兼容性已确认；MenuUpdateRequest 显式选择传统 POJO 以支持 PATCH 局部更新语义，决策合理

**[通过]** 泛型使用仅在 `Result<T>` 统一响应封装和 `ConcurrentHashMap` 集合层面，均在 Java 泛型系统能力范围内

**[通过]** UserType 枚举位于 `common-module-api`（`com.aimedical.modules.commonmodule.api.UserType`，已存在），CurrentUser.getUserType() 可直接引用，类型依赖路径正确

### 2. 标准库与生态覆盖

**[通过]** Spring Security 生态覆盖 JWT 认证过滤器、SecurityFilterChain 配置、AuthenticationEntryPoint/AccessDeniedHandler、BCryptPasswordEncoder、@PreAuthorize 注解——全部为 Spring Security 标准能力

**[通过]** Spring Data JPA 覆盖 UserRepository（含 @EntityGraph）、实体变更——标准能力

**[通过]** Jakarta Bean Validation（@NotBlank、@Size、@Pattern、@Email）覆盖 DTO 校验——标准能力

**[通过]** Jackson 覆盖 record/POJO 序列化——Spring Boot 默认集成

**[通过]** Java 标准库并发工具（ConcurrentHashMap、ReentrantLock、ScheduledExecutorService）覆盖内存状态管理——无需第三方依赖

**[通过]** JJWT 库覆盖 JWT 令牌生命周期管理——业界标准选择，已在项目中存在

### 3. 语言特性可行性

**[通过]** 错误处理分层合理：BusinessException 用于业务错误（HTTP 200），AuthenticationException 由 Spring Security 处理（HTTP 401），AccessDeniedException/PermissionChangeRequiredException 由 AccessDeniedHandler 处理（HTTP 403），RateLimitExceededException 由自定义处理（HTTP 429）

**[通过]** 并发设计规范：ConcurrentHashMap.compute 原子操作、ReentrantLock 细粒度锁、ScheduledExecutorService 定时清理——全部在 Java 并发工具范围内，策略选择合理

**[通过]** 资源管理：内存方案（黑名单、速率计数、登录失败计数）使用 ConcurrentHashMap，依赖 JVM GC + 定时清理线程回收过期条目——在 JVM 堆内存可承受范围内（~6.5MB 峰值）

**[通过]** 模块结构：Maven 多模块（common-module-api / common-module-impl）符合 Java 项目标准组织方式；api 模块仅暴露接口和共享类型，impl 模块实现业务逻辑，编译期依赖方向清晰

**[通过]** @Profile("phase1") 隔离两阶段 SecurityConfig——Spring 标准机制

**[通过]** Optional<User> 作为 Repository 返回类型——Java 8+ 标准做法

### 4. 设计一致性

**[通过]** 用户禁用场景的错误码处理闭环清晰：登录流程（3.1.1 步骤 6）→ 统一 LOGIN_FAILED 消息（防用户名枚举）；已认证请求（3.1.2 步骤 4）→ ACCOUNT_DISABLED（已认证场景下给出具体原因不引入风险）；刷新流程（3.1.3 步骤 7）→ TOKEN_REFRESH_FAILED + 递增 IP 计数。三场景语义一致无矛盾

**[通过]** 各抽象职责描述清晰无歧义：
- CurrentUser（会话级轻量身份标识）vs UserFacade（数据级完整用户查询）职责分工明确
- JwtAuthenticationFilter（JWT 鉴权）vs PasswordChangeCheckFilter（密码变更阻断）单一职责分离
- InMemoryRateLimitGuard（登录端点专用限流）vs GlobalRateLimitFilter（全局限流）职责范围清晰，计数器实例独立

**[通过]** 协作关系形成闭环：
- 登录流程 11 个步骤完整，从限流→锁定检查→用户加载→密码校验→状态检查→Token 签发→响应返回
- 密码变更流程 11 个步骤完整，含旧密码验证、复杂度校验、tokenVersion 递增、SecurityContext 清理、前端恢复流程
- 刷新流程 12 个步骤完整，含 Refresh Token 验证、用户状态检查、锁定检查、tokenVersion 比对、新 Token 签发

**[通过]** 模块间依赖方向合理：业务模块（patient/doctor/admin）→ common-module-api（仅共享类型）→ common-module-impl（门面实现），无循环依赖

**[通过]** ACCOUNT_LOCKED 错误消息（3.1.1 步骤 3 vs 10.2 ErrorCode 表）已统一为"根据锁定维度动态生成"，两处语义一致

**[通过]** tokenVersion 比对步骤（3.1.3 步骤 5 vs 步骤 9）歧义已消除：步骤 5 明确"暂不加载 tokenVersion"，步骤 9 明确"重新从 DB 加载…单独查询，保证强一致性"

**[通过]** SlidingWindowCounter 作为 public 工具类被 InMemoryRateLimitGuard 和 GlobalRateLimitFilter 两处复用，机制一致

**[通过]** IP 维度失败计数器清空的前置假设已明确记录：依赖 RequestHelper.getClientIp() 统一获取来源 IP，局限性已在 11 节设计决策表记录

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则：Filter 层职责分离（速率限制→JWT 鉴权→密码变更检查三阶段链）、服务层职责分离（认证服务 vs 密码策略 vs 密码变更策略 vs 限流策略）、接口抽象职责单一

**[通过]** 抽象层次恰当：
- TokenBlacklist/RateLimitGuard 接口+内存实现（Phase 1）+Redis 实现规划（Phase 2），不过度工程化
- PasswordPolicy/PasswordChangeService 接口化支持策略切换，不引入不必要的泛型层级
- 未引入 CQRS、事件驱动等非 Phase 1 需求的技术复杂度

**[通过]** 便于详细设计实现：3.1.1-3.1.6 六组流程步骤级描述、3.3 节 Filter 行为契约伪代码级约束、5.2 节 DTO 完整定义（含 JSR-380 注解）、4.3 节密码规则逐条表格化——所有关键设计点均已落到可实现的粒度

**[通过]** 便于单元测试：
- 接口抽象使各模块可独立 mock（AuthService、RateLimitGuard、TokenBlacklist、PasswordPolicy、PasswordChangeService）
- UserConverter 提取为独立 converter 层，支持单独测试（M1 修复方案）
- 工具类 SlidingWindowCounter 为 public，支持独立测试
- 包内依赖均为接口注入，Spring Boot 测试可复用现有测试基础设施

**[轻微]** 建议在后续迭代中为 `SlidingWindowCounter` 内部的线程安全策略（ConcurrentHashMap.compute + ReentrantLock 双保险）补充注释说明，避免实现者混淆或不必要的重复加锁

## 修改要求

无。设计中的 9 项历史问题（ACCOUNT_LOCKED 消息不一致、tokenVersion 比对歧义、UserFacade 定义缺失、PasswordChangeCheckFilter 路径匹配策略、MenuUpdateRequest PATCH 语义、Refresh 端点 IP 计数、IP 计数器清空前置条件、ProfileUpdateRequest.phone 可选性、PasswordChangeRequest.oldPassword 约束）全部已在当前版本中修正，无阻塞性问题。
