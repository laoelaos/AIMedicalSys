# OOD 设计方案审查报告（v7）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 设计中的全部类型形态选择（interface / abstract class / class / JPA @Entity / 值对象）均与 Java 类型系统能力匹配，且在 Spring 生态中有成熟的惯用用法。

**[通过]** 继承/实现关系严格遵守 Java 单继承和多接口实现约束：`CapabilityExecutor<T,R>` 作为 interface 由 `AbstractCapabilityExecutor<T,R>` 实现，13 个具体子类 extends 该抽象类；`AiOrchestrator` implements `AiService` 接口。所有继承链合理。

**[通过]** 泛型使用方式（`CapabilityExecutor<T,R>` 接口、`AbstractCapabilityExecutor<T,R>` 抽象类、具体子类绑定实际类型如 `TriageCapabilityExecutor`）均在 Java 泛型系统能力范围内，运行时类型擦除的补偿方案（`getInputType()/getOutputType()` 返回 `Class` 对象）是标准模式。

**[通过]** `CompletableFuture<AiResult<R>>` 异步契约、`Serializable` 序列化声明、`@Entity` JPA 注解等类型级决策均在 Java 语言能力范围内。

### 2. 标准库与生态覆盖

**[通过]** 设计依赖的标准库能力（`ConcurrentHashMap`、`AtomicReference`、`CompletableFuture`、`ThreadPoolExecutor`）全部来自 JDK 标准库。

**[通过]** 设计依赖的框架能力（Spring 的 `@ConditionalOnProperty`、`@Async`、`@Qualifier`、`ObjectProvider`、`EnvironmentPostProcessor`、`SecurityContextHolder`、`RequestContextHolder`、JPA、Micrometer、Jackson）全部来自成熟的 Spring 生态，假设合理。

**[通过]** 外部依赖（Spring AI / HTTP 客户端）通过统一的 `LlmClient` interface 隔离，不耦合具体实现。

**[通过]** 标准库能力可以覆盖设计中所有自定义抽象的需求，无需额外假设。

### 3. 语言特性可行性

**[通过]** 错误处理策略与 Java/Spring 的异常处理体系匹配：`CompletableFuture.supplyAsync()` 内 lambda 中的异常由 `CompletableFuture` 承载，`AiOrchestrator` 层面的 catch 兜底捕获意外异常返回 `AiResult.failure()`，符合"业务异常不抛、意外异常兜底"的 Java 实践。

**[通过]** 并发设计采用与 Java 并发模型兼容的模式：`CompletableFuture.supplyAsync()` + 自定义 `ThreadPoolExecutor`、`ConcurrentHashMap` + 写锁保护、`AtomicReference` + CAS 状态转换、`@Async` + 专用线程池隔离。无 `synchronized` 大锁，各组件独立承担线程安全。

**[通过]** 资源管理方案（HTTP 连接池、JPA 连接池、线程池管理）均在 Java/Spring Boot 的资源管理模式内可行。

**[通过]** 模块/包结构（`ai-api` / `ai-impl` 两层，`ai-impl` 内 orchestrator/router/client/template/experiment/metrics/parser/fallback/degradation 子包）与 Maven 多模块实践一致，依赖方向清晰。

### 4. 设计一致性

**[通过]** 各抽象的职责描述清晰无歧义。`AiOrchestrator` 负责路由委托、`CapabilityExecutor` 持有完整管线、`AbstractCapabilityExecutor` 提供模板方法骨架、其余各抽象（`ModelRouter`、`LlmClient`、`PromptTemplateManager`、`ExperimentManager`、`AiMetricsCollector` 等）各有明确职责边界。

**[通过]** 协作关系形成闭环：`AiOrchestrator` 委托 `CapabilityExecutor` → `CapabilityExecutor.execute()` 内部依次调用降级预检→实验分流→模板渲染→模型路由→端点健康检查→LLM 调用→结果解析→指标采集。Phase 4 薄适配器型走简化流程，同样闭环。

**[通过]** 行为契约完整到足以指导后续实现：§4 提供了 6 组关键路径的伪代码（统合调用管线、模型路由、A/B 实验分流、Prompt 模板渲染、结构化输出解析、性能指标采集），伪代码包含降级路径和异常分支处理。

**[通过]** 模块依赖方向合理，无循环依赖：`ai-api` ← `ai-impl`，`ai-impl` 内 orchestrator 为顶层编排依赖其余子包，其余子包之间无循环依赖。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则：`AiOrchestrator` 仅做路由委托、`CapabilityExecutor` 持管线但由 `AbstractCapabilityExecutor` 抽取公共骨架、`ModelRouter` 仅负责路由、`LlmClient` 仅负责 LLM 调用、`AiMetricsCollector` 仅负责指标采集。

**[通过]** 抽象层次恰当：未过度设计（如 pipeline step 链被明确搁置至 Phase 6），也无设计不足（`AbstractCapabilityExecutor` 合理解决了 13 个实现类的降级预检和指标采集模板代码复用）。`FallbackAiService` 的 `applyStrategies()` 标记 `@Deprecated` 但保留方法体，体现合理的迁移策略。

**[通过]** 设计便于后续详细设计和实现：类图、方法签名、状态模型、伪代码均提供了充足的实现指引。`AiPlatformConfig` 作为集中装配点降低了各组件间的显式依赖耦合。

**[通过]** 设计便于单元测试：`CapabilityExecutor` 依赖的组件（`PromptTemplateManager`、`ModelRouter`、`LlmClient`、`AiMetricsCollector` 等）均为 interface，可 mock；`SlidingWindowMetricsStore` 方法签名可独立测试；`DegradationStrategy` 实现接收 `DegradationContext` 而非直接依赖存储，易于隔离测试。

**[轻微]** `Experiment` 类图引用 `List<ExperimentGroup> groups`，但 `ExperimentGroup` 类型未在概要抽象表或正文中定义。建议在 §3.4 中补充对该值对象的简要说明，或标记为嵌套类型。

**[轻微]** 类图中 `TimeoutDegradationStrategy` 和 `CircuitBreakerDegradationStrategy` 标注了 `--> SlidingWindowMetricsStore : reads` 依赖，但实际管线设计中策略仅通过 `DegradationContext` 读取而非直接访问 `SlidingWindowMetricsStore`。建议删除或更新这两个依赖箭头以匹配实际运行契约，同时消除与 §2.2"其余子包之间不互相依赖"规则的隐含冲突。

## 修改要求

本设计无严重或一般问题，无需修改。
