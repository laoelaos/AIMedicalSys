# OOD 设计方案审查报告（v15）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 设计中的类型形态选择（interface / abstract class / class / enum / JPA @Entity）均与 Java 类型系统能力完全匹配。单继承（`AbstractCapabilityExecutor` 抽象基类）+ 多接口实现（各接口独立定义）的模式正确使用了 Java 的继承约束。泛型 `CapabilityExecutor<T, R>` 和 `LocalRuleFallback<T, R>` 的使用方式在 Java 泛型系统能力范围内。协作关系中描述的类型交互模式（接口依赖、构造器注入、`ObjectProvider` 延迟解析、`List<CapabilityExecutor>` 自动注入构建 Map）均可在 Java / Spring 中实现。

**[通过]** Java 8+ default method (`DegradationStrategy.getOrder()`) 的使用确保了二进制兼容性，不破坏 Phase 0 已有实现。

### 2. 标准库与生态覆盖

**[通过]** 设计依赖的标准库能力均在 Java / Spring Boot 生态覆盖范围内：Spring DI（`@Component`, `@Qualifier`, `@ConditionalOnProperty`, `ObjectProvider`, `ApplicationContextAware`, `EnvironmentPostProcessor`）、Jackson（`ObjectMapper.convertValue`, `@JsonCreator`, `@ConstructorProperties`）、Spring Data JPA（`@Entity`, `@Repository`, 索引声明）、Micrometer（Timer, Counter, DistributionSummary）、Caffeine / Guava Cache（凭据缓存 TTL）、`CompletableFuture`（异步编排、`orTimeout()`、`exceptionally()`）。设计中假设的库能力均合理。

### 3. 语言特性可行性

**[通过]** 错误处理策略（try-catch 捕获预期异常 + CompletableFuture 降级 + AiResult.failure() 非异常路径）与 Java 异常处理能力完全匹配。并发设计（`AtomicReference` + CAS 状态转换、`ConcurrentHashMap` + 写锁滑动窗口、`CompletableFuture.supplyAsync()` + 自定义线程池）均在 Java 并发模型内兼容。异步上下文传播策略（入口处 ThreadLocal 提取 + 闭包捕获 + 显式参数传递）是 Java 中处理线程池上下文丢失问题的标准模式。模块/包结构（Maven 多模块、`ai-api`/`ai-impl` 分离、`provided` 作用域薄适配器依赖）符合 Spring Boot 项目组织方式。

**[通过]** `CallerRunsPolicy` 提供自然背压、`DiscardPolicy` 避免指标写入阻塞 LLM 调用路径的设计均正确且可在 Java `ThreadPoolExecutor` 中实现。

### 4. 设计一致性

**[通过]** 各抽象职责描述清晰无歧义：`AiOrchestrator` 为路由层（仅查找委托）、`CapabilityExecutor` 为完整管线持有者、`AbstractCapabilityExecutor` 为骨架模板方法。协作关系形成完整闭环——`AiOrchestrator.handle()` 捕获意外异常 → 查找 executor → 委托 execute() → execute() 降级预检 → doExecuteInternal()（实验分流 → 模板渲染 → 模型路由 → 端点健康检查 → LLM 调用 → 解析 → 指标）→ 降级兜底 doDegrade()。行为契约（§4.1~§4.6）以伪代码形式完整定义，足以指导后续实现。模块间依赖方向清晰且无循环依赖（`ai-api` ← `ai-impl`，`ai-impl` 内部 orchestrator 单向依赖其余子包）。

**[通过]** 迭代需求中提出的 7 个问题均已在当前设计版本中得到显式覆盖：问题 1（catch 块 Phase 4 DTO 就诊上下文提取）在 §4.1 第 1473-1485 行通过 `else` 分支 `RequestContextHolder` 路径解决；问题 2（ParseFailure outputSummary 丢失）在 §4.1 第 1574-1582 行通过先赋 `outputSummary = llmResponse.getText()` 再传入 `doDegrade()` 解决；问题 3（DEPRECATED 版本回退）在 §4.4 第 1652 行显式定义 WARN 日志 + 回退 ACTIVE 策略；问题 4（ExecutionException 原始类型丢失）在 §3.1 第 756-758 行通过拆解 `e.getCause()` 获取原始异常类型解决；问题 5（PAUSED 状态显式处理）在 §4.3 第 1637 行显式过滤 `status = PAUSED`；问题 6（DEPRECATED→ACTIVE 回退路径）在 §3.3 第 1093 行增加状态转换；问题 7（不可变 DTO + 防御性拷贝兼容性）在 §3.1 第 628 行补充 Jackson 注解要求说明及三种处理策略。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则：路由（`AiOrchestrator`）、执行（`CapabilityExecutor`）、路由策略（`ModelRouter`）、模板管理（`PromptTemplateManager`）、实验管理（`ExperimentManager`）、指标采集（`AiMetricsCollector`）、结构化解析（`StructuredOutputParser`）、降级判定（`DegradationStrategy`）各自独立。抽象层次恰当——未过度设计为 Pipeline Step 链（留待 Phase 6），也未将 13 项能力塞入单一实现。设计便于后续详细设计和实现（骨架模板方法 + 子类仅特化 `doExecuteInternal()`）。设计便于单元测试（`@MockBean` 模拟所有下游依赖、降级预检前置验证、HTTP Header 提取测试均有明确策略）。

## 修改要求

无（APPROVED，无严重或一般问题）
