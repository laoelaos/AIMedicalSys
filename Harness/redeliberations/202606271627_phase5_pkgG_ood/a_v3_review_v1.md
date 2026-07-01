# OOD 设计方案审查报告（v3）

## 审查结果

REJECTED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 所有类型形态选择（interface / abstract class / class / @Entity / enum）均与 Java 类型系统能力匹配

**[通过]** 继承与实现关系（单继承 + 多接口实现）符合 Java 约束

**[通过]** `CapabilityExecutor<T, R>` 泛型使用方式在 Java 泛型系统能力范围内

**[通过]** `default int getOrder() { return 0; }` 在接口中声明为 default 方法，Java 8+ 支持，现有 `NoOpDegradationStrategy` 等实现无需修改

**[通过]** 协作关系中的类型交互模式（CompletableFuture 异步回调、interface 依赖注入）在 Java 中可完整实现

### 2. 标准库与生态覆盖

**[通过]** 设计中需要的所有能力均在 Java 标准库或 Spring 生态覆盖范围内（JPA、Micrometer、Jackson、ConcurrentHashMap、AtomicReference、CompletableFuture、@Async 等）

**[通过]** Micrometer 依赖已在 §8 显式声明 `spring-boot-starter-actuator`，假设合理

**[通过]** 无标准库能力可以显著简化设计中的自定义抽象

### 3. 语言特性可行性

**[通过]** 错误处理策略（AiResult 包装返回 + 降级路径）与 Java 异常机制兼容

**[通过]** 并发设计（CompletableFuture、ConcurrentHashMap、CAS、@Async + CallerRunsPolicy）均与 Java 并发模型兼容

**[通过]** 资源管理方案（JPA Entity 生命周期、Spring Bean 生命周期、连接池）在 Java 资源管理模式内可行

**[通过]** 模块/包结构设计（ai-api / ai-impl 双模块）符合 Maven 项目组织方式

### 4. 设计一致性

**[通过]** AiOrchestrator、CapabilityExecutor、ModelRouter、PromptTemplateManager、ExperimentManager、AiMetricsCollector 等各抽象职责描述清晰

**[通过]** 协作关系形成闭环：AiOrchestrator → CapabilityExecutor → ModelRouter/LlmClient/PromptTemplateManager/ExperimentManager/StructuredOutputParser/AiMetricsCollector/SlidingWindowMetricsStore → degrade/return，无缺失环节

**[通过]** 行为契约（§4.1~§4.6）的伪代码完整到足以指导后续实现

**[通过]** 模块依赖方向单向（ai-api ← ai-impl，ai-impl 内部 orchestrator → 其余子包），无循环依赖

**[一般]** MockAiService 的 `@ConditionalOnProperty` 属性名未按修订说明更新 — §3.1 行 484 仍写 `@ConditionalOnProperty(name = "ai.platform.enabled", havingValue = "false", matchIfMissing = true)`，但 v3 修订说明明确应改为 `name = "ai.mock.enabled"` 以与现有代码兼容。实际文本与修订承诺不一致。

**[一般]** CapabilityExecutor.execute() 方法签名的文本描述与类图不一致 — §3.1 行 498 文本方法签名为 `AiResult<R> execute(T request, String capabilityId)`（同步），而 §2.3 类图行 197 为 `CompletableFuture<AiResult<R>>`（异步）。v3 修订说明声称已"同步修正"，但 §3.1 文本未实际更新。

**[一般]** AiOrchestrator 线程安全模型存在自相矛盾的陈述 — §3.1 行 479 称"`AiOrchestrator` 本身通过 `synchronized` 或 `ReentrantLock` 保护关键路径的指标存储写入"，但 §6.1 行 991 称"`AiOrchestrator` 本身不引入 `synchronized` 大锁；并发瓶颈由各子组件独立承担"。两处说法互斥。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则（编排/路由/模板/实验/指标/降级各自独立接口和实现）

**[通过]** 抽象层次恰当：定义了接口层面的契约和协作关系，未过度深入到具体字段级实现细节

**[通过]** 设计便于后续详细设计和实现：接口清晰、依赖方向明确、协作流程文档化

**[通过]** 设计便于单元测试：所有核心抽象均为 interface，可 mock；SlidingWindowMetricsStore 等有状态组件暴露清晰方法签名

## 修改要求（REJECTED）

### 问题 1：MockAiService @ConditionalOnProperty 属性名未按修订更新

- **问题**：§3.1 Bean 装配策略节，MockAiService 仍标注 `@ConditionalOnProperty(name = "ai.platform.enabled")`，但修订说明承诺改为 `name = "ai.mock.enabled"` 以与现有代码兼容。
- **原因**：实际设计文本与修订说明不一致。若按当前文本实现，现有代码中依赖 `ai.mock.enabled` 的条件逻辑将无法正确与底座切换交互。
- **建议方向**：将 MockAiService 的条件注解改为 `@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "true", matchIfMissing = true)`；在 §3.1 或 §7 补充 AiPlatformConfig 中从 `ai.platform.enabled` 到 `ai.mock.enabled` 的内部转发逻辑。

### 问题 2：CapabilityExecutor.execute() 方法签名文本与类图不一致

- **问题**：§3.1 行 498 文本签名写为 `AiResult<R> execute(T request, String capabilityId)`（同步），而 §2.3 类图行 197 写为 `CompletableFuture<AiResult<R>>`（异步）。v3 修订说明声称"同步修正"，但文本未改。
- **原因**：若后续实现者按文本签名编码，CapabilityExecutor 接口将编译通过但无法与 AiService 的 CompletableFuture 异步管线桥接，导致运行时类型不匹配。
- **建议方向**：将 §3.1 文本方法签名修正为 `CompletableFuture<AiResult<R>> execute(T request, String capabilityId)`，返回值说明相应更新为"异步返回包裹对应业务响应 DTO 的 CompletableFuture"；补充异步桥接说明段解释 CapabilityExecutor 内部如何使用 CompletableFuture.supplyAsync() 编排管线步骤。

### 问题 3：AiOrchestrator 线程安全模型自相矛盾

- **问题**：§3.1 行 479 声称使用 `synchronized` 或 `ReentrantLock` 保护关键路径写入，但 §6.1 行 991 明确声明"不引入 `synchronized` 大锁"。
- **原因**：两处互相矛盾的线程安全策略描述会导致后续实现者在线程安全模型的理解和执行上产生分歧，可能引入并发缺陷。
- **建议方向**：统一描述。推荐采用 §6.1 的策略（各子组件独立承担并发安全，编排层不引入大锁），删除 §3.1 中关于 `synchronized`/`ReentrantLock` 的表述，或将其准确限制在 `SlidingWindowMetricsStore` 等具体子组件的线程安全实现中。
