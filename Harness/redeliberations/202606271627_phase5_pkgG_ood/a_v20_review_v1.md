# OOD 设计方案审查报告（v20）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 所有类型形态选择（class / interface / abstract class / enum / sealed class / JPA @Entity）均与 Java 17 类型系统能力匹配。`CapabilityExecutor<T, R>`、`LocalRuleFallback<T, R>`、`StructuredChatResult<T>` 等泛型抽象的使用方式在 Java 泛型系统能力范围内；`getInputType()` → `Class<T>` 模式正确应对了类型擦除。继承关系（单继承 abstract class + 多 interface 实现）符合 Java 约束。`LlmChatService` / `LlmChatStreamService` 接口分离正确隔离了 `reactor-core` 依赖，v20 修复后 `HttpApiLlmChatService` / `SpringAiLlmChatService` 仅实现同步接口，`HttpApiLlmChatStreamService` / `SpringAiLlmChatStreamService` 仅实现流式接口。协作关系中描述的类型交互（CompletableFuture / Flux / JPA Repository / 注解驱动注入）均可实现。

### 2. 标准库与生态覆盖

**[通过]** 设计依赖的能力均在 Spring Boot 3.2.5 + Java 17 标准库或常用生态覆盖范围内：Spring `@ConditionalOnProperty` / `@Qualifier` / `@Async` / `@EventListener` / `EnvironmentPostProcessor` / `ObjectProvider`、Jackson `ObjectMapper.convertValue()`、JPA / Spring Data、Micrometer、Caffeine、Guava RateLimiter、CompletableFuture / ForkJoinPool。`AiPlatformEnvironmentPostProcessor` 通过 `spring.factories` 注册是标准做法。薄适配器 `provided` 作用域依赖 Phase 4 模块时，已充分文档 Spring Boot uber-JAR 部署的运行时风险及回退方案。

### 3. 语言特性可行性

**[通过]** 错误处理策略与 Java 异常体系匹配：各检查型异常在内部以降级路径处理（`doDegrade()`），`AiOrchestrator.handle()` 层面兜底不可预知异常。并发设计（`CompletableFuture.supplyAsync()`、`orTimeout()`、`ConcurrentHashMap`、`AtomicReference` CAS、`synchronized` 写锁）均在 Java 并发模型内兼容。`CallerRunsPolicy` 背压 + `DiscardPolicy` + WARN 日志的资源管理方案可行。`@PostConstruct` + `ApplicationContextAware` 的策略装配时序已规避 `@Bean` 阶段的容器未就绪风险。`@PostConstruct` 扫描 `List<CapabilityExecutor>` 构建映射表是标准 Spring 初始化模式。

### 4. 设计一致性

**[通过]** 各抽象职责描述清晰无歧义：`AiOrchestrator` 为纯路由委托层、`CapabilityExecutor` 为完整管线持有者、`AbstractCapabilityExecutor` 为模板方法骨架。协作关系形成闭环——上下文在 `execute()` 入口处从 `SecurityContextHolder` / `RequestContextHolder` 提取后以参数形式贯穿 `doExecuteInternal()` → `doDegrade()` 全路径。行为契约（§4.1 伪代码、§5.1 错误分类表、§3.8 DegradationReason 枚举）完整到足以指导实现。模块间依赖方向清晰：`ai-api ← ai-impl`，内部按单向依赖组织，`ai-impl → Phase 4 业务模块` 的出向依赖已定义作用域约束。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则：编排路由、管线执行、模型路由、模板管理、实验分流、指标采集、配置装配均有独立抽象。抽象层次恰当——13 个 `CapabilityExecutor` 各有独立 DTO 类型，泛型提供类型安全。设计便于详细实现和单元测试（`@MockBean` 注入模拟依赖、提取后参数传递模式消除 ThreadLocal 依赖、构造器注入便于隔离测试）。薄适配器构造器签名在 v20 中已消除不必要的 `PromptTemplateManager` / `ModelRouter` / `LlmChatService` / `StructuredOutputParser` 等基础设施依赖。`AbstractCapabilityExecutor` 以 null 接收薄适配器不用参数并防御性跳过对应管线步骤的模式虽非最优雅，但已充分文档且不影响可实施性。

## 修改要求

无需修改。
