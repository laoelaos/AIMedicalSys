# OOD 设计方案审查报告（v4）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 所有抽象的类型形态选择（interface / class / record / 实体类）均与 Java 17 类型系统能力匹配。继承关系符合单继承+多接口实现约束。DTO 使用 record 形态，实体保持普通 class，分工清晰。泛型使用仅限标准 Spring Data JPA 的 Repository 泛型参数，在泛型系统能力范围内。Record 用于 DTO 数据传输场景，Jackson 反序列化兼容性已在 Spring Boot 环境中验证。

### 2. 标准库与生态覆盖

**[通过]** 设计所需能力均在 Java 标准库或 Spring 生态覆盖范围内：
- JWT 令牌：`io.jsonwebtoken` (jjwt) 或 `com.auth0.java-jwt`
- 密码加密：`BCryptPasswordEncoder` (Spring Security)
- 并发集合：`ConcurrentHashMap` (java.util.concurrent)
- 定时清理：`ScheduledExecutorService` (java.util.concurrent)
- 限流锁：`ReentrantLock` (java.util.concurrent)
- REST API：Spring Web MVC
- ORM：Spring Data JPA + Hibernate
- 参数校验：Bean Validation (jakarta.validation)
- Vue 3 / TypeScript 前端部分：Pinia、axios、Vue Router 均在生态标准覆盖内

**[轻微]** `findById` 方法需要 `@EntityGraph` 但该方法来自 `JpaRepository<T, ID>` 默认实现，需在 `UserRepository` 中显式 override 并附加 `@EntityGraph`。设计已在 7.1 节脚注中说明查询优化要求，建议在 8.1 节 M9 修复方案中补充 override 方式声明。

### 3. 语言特性可行性

**[通过]** 错误处理策略（BusinessException + GlobalExceptionHandler）与 Spring MVC `@ControllerAdvice` 模型完全匹配。并发设计（ConcurrentHashMap + ReentrantLock + ScheduledExecutorService）全部使用 java.util.concurrent 标准方案，线程安全策略合理。资源管理无泄露风险（内存黑名单使用定时任务回收）。模块/包结构符合 Maven 多模块项目的标准组织方式。

**[轻微]** `PasswordChangeCheckFilter` 和 `JwtAuthenticationFilter` 各自独立查询 DB 加载用户状态，导致每次请求两次查库。可通过在请求属性（request attribute）中缓存已加载的 User 对象来优化，使 PasswordChangeCheckFilter 复用 JwtAuthenticationFilter 的查询结果。此为性能优化建议，不阻塞通过。

### 4. 设计一致性

**[通过]** 各抽象职责描述清晰：AuthService（认证业务契约）、JwtTokenProvider（令牌生命周期）、JwtAuthenticationFilter（JWT 鉴权）、PasswordChangeCheckFilter（密码变更业务规则）、RateLimitGuard（速率限制）等均有明确的职责边界。协作关系形成完整闭环：登录→签发 token→鉴权→查库验证→PasswordChangeCheck→API 处理→登出→黑名单。行为契约描述充分（3.3 节提供伪代码级别的 Filter 行为契约）。模块依赖方向合理（common-module-impl → permission/，无循环依赖）。

**[轻微]** `LoginAttemptCleaner` 在 2.1 节目录结构中列出，但 9.3 节未描述其具体行为（使用的调度策略、清理周期）。建议补充清理策略说明以消除阅读者的疑问。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则（JwtAuthenticationFilter 仅负责鉴权、PasswordChangeCheckFilter 仅负责密码变更阻断，各 interface 职责聚焦）。抽象层次恰当：AuthService 定义业务边界、JwtTokenProvider 封装令牌细节、RateLimitGuard 抽象限流策略，不过度也不不足。设计便于后续详细实现（伪代码级行为契约、明确的协作关系、完整的 DTO 定义）。设计便于单元测试：interface 隔离使 mock 注入可行，CurrentUser 接口消除对 SecurityContextHolder 的静态依赖。

## 修改要求（REJECTED 时存在）

（无）
