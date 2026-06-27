# OOD 设计方案审查报告（v14）

## 审查结果

REJECTED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 设计中的类型形态选择（interface / class / record / enum / @Component class / OncePerRequestFilter 子类）与 Java 17 类型系统完全匹配。interface 多实现、class 单继承、record 不可变 DTO、enum 枚举 ErrorCode、Exception 继承层次（PasswordChangeRequiredException extends AccessDeniedException）均在 Java 类型系统能力范围内。泛型使用（Result\<T\>）符合 Java 泛型规范。

**[通过]** 抽象之间的继承和实现关系合理。Filter 继承 OncePerRequestFilter（单继承），Service 实现 interface（多实现），无违反约束的关系。

**[通过]** 协作关系中的类型交互模式可在 Java 中实现。Service 注入 Repository/Provider 的依赖注入模式、Filter 通过 request attribute 传递状态（setAttribute/getAttribute）、SecurityContextHolder 静态线程上下文，均为 Spring/Java 标准实践。

### 2. 标准库与生态覆盖

**[通过]** 设计所需能力均在标准库或常用库覆盖范围内：Spring Security（认证授权/Filter 链/BCrypt/SessionManagement）、Spring Data JPA（@EntityGraph 查询优化）、JWT（jjwt 或同类库）、Java 并发库（ConcurrentHashMap/ReentrantLock/ScheduledExecutorService）、Jakarta Validation（@NotBlank/@Size/@Pattern/@Email）、Jackson（record 序列化）。无超出生态范围的能力要求。

**[通过]** 设计中假设的库能力（BCrypt 加密、JWT 签名验证、@EntityGraph 延迟加载优化、record 的 Jackson 反序列化）均为合理假设，有成熟实现。

**[通过]** 设计中已有利用标准库简化自定义抽象的例子。例如使用 ConcurrentHashMap + ReentrantLock 替代自定义并发 Map；使用 Spring Security 的 AuthenticationEntryPoint/AccessDeniedHandler 替代手写异常处理 Filter；使用 @EntityGraph 替代手写 fetch join 控制。

### 3. 语言特性可行性

**[通过]** 错误处理策略与 Java 异常机制匹配：BusinessException（业务错误/HTTP 200）、AuthenticationException（Spring Security 认证失败）、AccessDeniedException（权限不足/403）、PasswordChangeRequiredException（自定义异常 extends AccessDeniedException）。全局异常由 GlobalExceptionHandler + AuthenticationEntryPoint + AccessDeniedHandler 分层处理，策略清晰可行。

**[通过]** 并发设计使用 Java 标准并发原语：ConcurrentHashMap + compute 原子操作（LoginAttemptTracker 计数更新）、ReentrantLock 公平锁（滑动窗口跨操作原子性）、ScheduledExecutorService 定时清理（TokenBlacklist/SlidingWindowCounter 过期条目）。粒度设计合理（按 IP key 独立锁定），无超出 Java 并发模型的能力要求。

**[通过]** 资源管理方案可行：Phase 1 使用堆内存数据结构，有明确的内存占用估算（Access Token 黑名单 ~6.5MB）和定时回收策略，在 JVM 可承受范围内。

**[通过]** 模块/包结构符合 Maven 多模块工程组织方式。common-module-api（接口契约）与 common-module-impl（实现）分离，遵循 Spring Boot 项目规范。

### 4. 设计一致性

**[通过]** 各抽象职责描述清晰无歧义。1.3 节核心抽象一览对每个抽象的职责定位和类型形态有明确说明，3.3 节对三个 Filter 的行为契约有完整的步骤级描述。

**[通过]** 协作关系形成闭环。认证流程（3.1.1 登录 → 3.1.2 已认证请求 → 3.1.3 刷新 → 3.1.4 登出 → 3.1.5 获取当前用户 → 3.1.6 密码变更）覆盖认证生命周期所有环节。各 Filter 的注册顺序（GlobalRateLimitFilter → JwtAuthenticationFilter → PasswordChangeCheckFilter）形成完整的请求处理链。

**[通过]** 行为契约描述完整。Filter 行为契约明确规定了每个步骤的输入、判断分支、输出（返回响应 / chain.doFilter / 抛出异常），足以指导后续编码实现。ErrorCode 表（10.2 节）覆盖了所有业务场景的错误码定义和使用边界。

**[通过]** 模块间依赖方向合理，无循环依赖。common-module-api ← common-module-impl ← application 的单向依赖链清晰。业务模块（patient/doctor/admin）→ common-module-api（接口），不依赖 common-module-impl，符合分层原则。

**[一般]** 2.1 节目录结构中 `auth/security/CurrentUser.java`（interface）列在 `common-module-impl` 的目录树中，但 2.2 节及迁移步骤明确要求"业务模块通过 `CurrentUser` 接口（位于 `common-module-api` 中）获取当前登录用户信息"以及"在 `common-module-api` 中新增 `CurrentUser` 接口"。两处对 `CurrentUser` 接口的物理存放位置存在矛盾。建议将 2.1 节目录结构调整为：`common-module-api` 目录树中列出 `CurrentUser.java` interface，`common-module-impl` 中仅列出 `CurrentUserImpl.java`；或明确说明 `CurrentUser.java` 在 common-module-api 和 common-module-impl 中分别存在（前者供业务模块引用，后者是 impl 模块实现依赖），若如此需补充两个接口的关系说明。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则。三个 Filter 职责清晰分离：JwtAuthenticationFilter（JWT 鉴权）、PasswordChangeCheckFilter（业务规则检查）、GlobalRateLimitFilter（全局限流）。各 Service 接口职责内聚（AuthService 认证边界、PasswordPolicy 密码策略、PasswordChangeService 密码变更策略、LoginAttemptTracker 登录锁定追踪）。

**[通过]** 抽象层次恰当。接口设计（RateLimitGuard / TokenBlacklist / PasswordPolicy / PasswordChangeService / CurrentUser）提供了合理的扩展点且便于测试。未过度设计（如并非所有内部类都接口化）。DTO 使用 record 精确表达了数据传输的不可变语义。

**[通过]** 设计便于后续实现。每个接口的方法签名已完整定义（PasswordPolicy.validate、PasswordChangeService.isChangeRequired/markChangeRequired/clearChangeRequired、CurrentUser.getUserId/getUsername/getUserType、SlidingWindowCounter.tryAcquire），编码阶段有明确的契约可遵循。Filter 行为契约以结构化步骤描述，可直接映射为 Java 代码。

**[通过]** 设计便于单元测试。接口抽象可 mock（RateLimitGuard / TokenBlacklist / PasswordPolicy / UserRepository / PasswordChangeService / CurrentUser）。CurrentUser 接口消除了 Controller 层对 SecurityContextHolder 的直接静态依赖。UserConverter 提取为独立转换器，支持单独测试验证。

## 修改要求（REJECTED）

### 问题 1（一般）— 目录结构与依赖规则矛盾

- **问题**：2.1 节 `common-module-impl` 目录树中列出了 `auth/security/CurrentUser.java`（interface），但 2.2 节依赖规则和迁移步骤均明确要求 `CurrentUser` 接口放在 `common-module-api` 中供业务模块引用。两处描述自相矛盾，实施阶段可能导致开发者将接口放入错误模块。

- **原因**：`currentUser` 接口的物理位置直接影响编译期依赖关系——若放在 `common-module-impl` 中，业务模块（patient/doctor/admin）将不得不依赖 `common-module-impl`，违反 2.2 节"业务模块不依赖 common-module-impl"的设计约束。此不一致可能导致模块依赖关系在实现阶段被意外破坏。

- **建议方向**：
  1. 将 2.1 节目录结构调整为在 `common-module-api` 的目录树中放置 `auth/CurrentUser.java`（interface），`common-module-impl` 中仅保留 `auth/security/CurrentUserImpl.java`（实现类）；或
  2. 若设计意图是 `CurrentUser` 接口在 `common-module-impl` 和 `common-module-api` 中各有一份（接口复制/桥接），则需在 2.2 节补充两接口的关系及使用说明；或
  3. 在 2.1 节补充说明 `CurrentUser.java` 的实际物理路径跨两个模块，目录树仅为逻辑上的内聚关系展示而非物理布局。
