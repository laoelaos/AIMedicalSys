# OOD 设计方案审查报告（v14）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 设计中的类型形态选择（interface / class / record / enum）与 Java 17 类型系统能力完全匹配。interface 定义的契约抽象（AuthService、RateLimitGuard、TokenBlacklist、PasswordPolicy、PasswordChangeService、CurrentUser）均遵循 Java 单继承约束与多接口实现模式。record 用于 DTO（LoginRequest、LoginResponse、RefreshTokenRequest 等）是 Java 17 的标准惯用法，Spring Boot 3.2.x 内嵌 Jackson 2.14+ 对 record 序列化/反序列化提供原生支持。UserFacade 门面接口位于 common-module-api、实现位于 common-module-impl 的 SPI 模式在 Java 模块系统中可行。

**[轻微]** 第 4.1 节 SlidingWindowCounter 描述"每个 key 的 Deque 在 `compute` 闭包内访问，`ReentrantLock` 保护跨窗口操作的原子性"——`ConcurrentHashMap.compute(key, ...)` 已保证指定 key 的原子性访问，额外 `ReentrantLock` 冗余。建议在实现阶段验证是否确实需要双重锁保护，若仅单 key 操作则可移除 `ReentrantLock`。

### 2. 标准库与生态覆盖

**[通过]** 设计依赖的所有库能力均为 Spring Boot 3.2.x + Java 17 标准生态覆盖范围：Spring Security（OncePerRequestFilter、SecurityFilterChain、AuthenticationEntryPoint、AccessDeniedHandler、BCryptPasswordEncoder、@EnableMethodSecurity）、Spring Data JPA（@Entity、@EntityGraph、JpaRepository）、Java Concurrency（ConcurrentHashMap、ReentrantLock、ScheduledExecutorService）、Jackson（JSON 序列化）、jjwt 或 io.jsonwebtoken（JWT HS256 签名/验证）。所有假设合理，无超出生态覆盖的设计。

### 3. 语言特性可行性

**[通过]** 错误处理策略（ErrorCode enum + BusinessException + GlobalExceptionHandler + Spring Security 异常层次）与 Java/Spring 错误处理能力完全匹配。PasswordChangeRequiredException 继承 `org.springframework.security.access.AccessDeniedException` 的路径在 Spring Security 框架内可行。并发设计（ConcurrentHashMap.compute 原子性、ScheduledExecutorService 定时清理）均为标准 Java 并发模型。资源管理（内存数据结构 + 惰性清理 + 定时回收）在 JVM 堆容量范围内可行。模块/包结构与 Maven 多模块项目组织方式一致。

### 4. 设计一致性

**[通过]** 各抽象职责描述清晰无歧义。协作关系形成完整闭环——登录→JWT 签发→Filter 鉴权→passwordChangeRequired 检查→登出/刷新/密码变更，流程完整。行为契约充分到足以指导实现。模块间依赖方向合理、无循环依赖——common-module-api 作为共享门面层，common-module-impl 实现层依赖 permission 内部实体，业务模块仅依赖 common-module-api。

**[轻微]** 文档标题标注"v14"但末尾修订说明已迭代至 v15（见第 26 节），存在版本号标称与修订记录不一致的情况，建议统一。第 2.1 节目录结构图中 `CurrentUser.java` 仍位于 `common-module-impl/auth/security/`，但第 26 节（v15 修订）已将其移至 `common-module-api`，目录图未同步更新。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则——JwtAuthenticationFilter 仅负责 JWT 鉴权、PasswordChangeCheckFilter 仅负责密码变更阻断、GlobalRateLimitFilter 仅负责全局限流、AuthServiceImpl 负责认证业务逻辑，各模块内聚度高。抽象层次恰当——接口化设计（TokenBlacklist、RateLimitGuard、PasswordPolicy、PasswordChangeService）支持 Phase 2 Redis 迁移和策略切换。设计便于单元测试——interface 可 mock、UserConverter 可单独测试、CurrentUser 抽象消除 SecurityContextHolder 直接依赖。

**[轻微]** 第 4.1 节 SlidingWindowCounter 的 `ReentrantLock` 使用存在过度设计的嫌疑，与 `ConcurrentHashMap.compute` 提供的原子性重复，建议实现阶段简化或通过注释说明跨窗口复合操作的具体场景。

## 修改要求（REJECTED 时存在）

N/A — 无严重和一般问题。

## 通过/驳回标准

- **APPROVED**：无严重和一般问题，可存在轻微级改进
