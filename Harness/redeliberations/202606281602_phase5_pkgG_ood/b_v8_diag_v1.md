# 质量审查报告 — v8 产出诊断

## 审查范围

- 审查目标：Phase5 包 G OOD 设计文档（v8）
- 审查视角：实际落地评估——设计是否可直接指导编码实现、接口定义是否支持下游消费者、异常场景和边界条件是否已考虑
- 侧重维度：需求响应充分度、整体深度与完整性、内部审议未充分覆盖的维度
- 对照物：用户需求（requirement.md）、历史迭代反馈（iteration_history.md）、代码库现状（已根据 v7 反馈修正内容进行交叉验证）

---

## 发现的问题

### 问题 1：[严重] 抽象父类构造器上的 `@Value`/`@Qualifier` 注解在 Spring DI 中无法被解析

- **所在位置**：§3.1 AbstractCapabilityExecutor 构造器（行 1240–1258）
- **问题描述**：`AbstractCapabilityExecutor` 构造器参数上标注了 `@Value("${ai.execution.timeout.thin-adapter-default:30s}")`、`@Qualifier("capabilityTimeoutConfig")` 和 `@Qualifier("thinAdapterPerCapabilityConfig")`。Spring 依赖注入仅处理它用来**实例化 bean 的那个构造器**上的注解——即具体子类（如 `TriageCapabilityExecutor`）的构造器，而非父类构造器。子类构造器主动调用 `super()` 时，父类构造器参数上的注解**不会被 Spring 解析**。这意味着：
  - `capabilityTimeoutConfig` → `null`
  - `thinAdapterTimeout` → `null`（虽然在 `execute()` 中通过 `.getOrDefault()` 调用会 NPE）
  - `thinAdapterPerCapabilityConfig` → `null`

  薄适配器构造器（行 943–953）中调用 `super(null, null, null, null, metricsCollector, metricsStore, null, degradationStrategyMap, localRuleFallback)` 也完全未传递这三个参数，进一步印证了此设计矛盾。

- **严重程度**：严重（实现者按此设计编码将遇到编译通过但运行时 NPE 或降级行为与设计不一致的问题）
- **改进建议**：
  - 删除父类构造器上的 `@Value` 和 `@Qualifier` 注解（注解在父类构造器上无实际作用且产生误导）
  - 改为：父类通过 protected 字段注入，子类构造器参数上标注 `@Value`/`@Qualifier` 后通过 `super()` 传入已解析的值
  - 或在父类中使用 `@Autowired` 字段注入 + `@PostConstruct` 初始化替代构造器参数注入
  - 修正薄适配器 `super()` 调用，补全缺失的 3 个参数

---

### 问题 2：[严重] 薄适配器 `super()` 调用参数数量不匹配，父类构造器参数缺失

- **所在位置**：§3.1 薄适配器诊断代码段（行 949–950）；父类构造器（行 1240–1252）
- **问题描述**：`DiagnosisCapabilityExecutor` 构造器中 `super()` 调用传入 9 个参数，但 `AbstractCapabilityExecutor` 构造器共 12 个参数。缺失的 3 个参数为：
  1. `@Qualifier("capabilityTimeoutConfig") Map<String, Duration> capabilityTimeoutConfig`
  2. `@Value("${ai.execution.timeout.thin-adapter-default:30s}") Duration thinAdapterTimeout`
  3. `@Qualifier("thinAdapterPerCapabilityConfig") Map<String, Duration> thinAdapterPerCapabilityConfig`

  直接后果：`thinAdapterTimeout` 在行 2921 `getOrDefault()` 时 NPE；`capabilityTimeoutConfig` 在 `execute()` 模板方法行 2675 `capabilityTimeoutConfig.getOrDefault()` 时 NPE。此问题使薄适配器完全无法正常工作。

- **严重程度**：严重（编译正确但运行期必然 NPE，且此代码段是其他 6 个薄适配器的模板，需全部修正）
- **改进建议**：薄适配器 `super()` 调用必须补全全部 12 个参数。另一种方案是将薄适配器不需要的参数（如 `capabilityTimeoutConfig`）设为 `null` 并在父类 `execute()` 中对 `null` 做保护（但行 2675 当前未做 null 检查）。

---

### 问题 3：[重要] 7 项底座完整管线 CapabilityExecutor 的子类构造器未定义

- **所在位置**：§3.11.1–§3.11.7（行 2474–2577）
- **问题描述**：设计文档定义了 `AbstractCapabilityExecutor` 父类构造器（12 个参数），并给出了薄适配器的子类构造器示例（行 937–953）。但是 **7 项底座完整管线 CapabilityExecutor（Triage/PrescriptionCheck/MedicalRecordGen/PrescriptionAssist/KbQuery/Schedule/DiscussionConclusion）的构造器一个都未定义**。这些构造器需要注入 `PromptTemplateManager`、`ModelRouter`、`LlmChatService`、`StructuredOutputParser`、`ModelEndpointHealthManager` 等父类需要的全部基础设施以及 `LocalRuleFallback`（对 PrescriptionCheck 来说必需）、`capabilityTimeoutConfig` 等。这些注入方式与薄适配器显著不同，实施者缺乏明确的编码指导。

- **严重程度**：重要（实施者需要自行推断构造器签名和注入模式，增加了出错风险和认知负担）
- **改进建议**：至少提供一个完整管线子类的构造器伪代码（如 `TriageCapabilityExecutor`），展示 `PromptTemplateManager`、`ModelRouter`、`LlmChatService` 等如何通过构造器注入并传递给 `super()`。同时标注各子类构造器是否需要 `@Qualifier`/`@Value` 注解。

---

### 问题 4：[中等] `AbstractCapabilityExecutor` 类图缺少 `llmCallExecutor` 字段

- **所在位置**：§2.3 类图（行 330–345）；§4.1 `execute()` 伪代码（行 2670）
- **问题描述**：`AbstractCapabilityExecutor.execute()` 模板方法通过 `CompletableFuture.supplyAsync(() -> { ... }, llmCallExecutor)` 将管线执行委派到共享线程池。但类图中 `AbstractCapabilityExecutor` 只显示了 `capabilityTimeoutConfig`、`thinAdapterTimeout`、`thinAdapterPerCapabilityConfig` 三个字段，缺少 `Executor llmCallExecutor` 字段。该字段是异步执行模型的核心依赖，类图的缺失可能导致实施者忽略其注入方式。

- **严重程度**：中等（影响实施者理解异步边界和线程模型）
- **改进建议**：在 §2.3 类图 `AbstractCapabilityExecutor` 中补充 `#Executor llmCallExecutor` 字段，并标注注入方式（如 `@Qualifier("llmCallExecutor")`）。

---

### 问题 5：[中等] `transcriptSummaryTimeout` 未附着到任何类定义

- **所在位置**：§3.11.7（行 2565）；§9.5 YAML（行 3439）；父类构造器（行 1240–1252）
- **问题描述**：`DiscussionConclusionCapabilityExecutor` 的前置 LLM 调用使用独立的超时 `transcriptSummaryTimeout`（默认 15s），通过 `@Value("${ai.execution.timeout.transcript-summary:15s}")` 注入。但这个注入目标字段**在 `AbstractCapabilityExecutor` 的构造器/字段中不存在**，在 `DiscussionConclusionCapabilityExecutor` 的子类伪代码中也未定义。它是通过父类 `thinAdapterTimeout` 注入吗？还是子类单独声明字段？设计文本未明确。实施者无法确定如何注入和使用这个超时值。

- **严重程度**：中等（将导致实现者自行猜测注入方式，可能产生与设计不一致的实现）
- **改进建议**：在 `DiscussionConclusionCapabilityExecutor` 的子类设计段中补充 `transcriptSummaryTimeout` 字段声明和注入方式（推荐子类 `@Value` 构造器注入，作为子类独有字段而非父类公共字段），并展示其在 `doExecuteInternal()` 中的使用位置。

---

### 问题 6：[中等] 完整管线 `doExecuteInternal()` 中 `promptVersion` 未传入降级路径

- **所在位置**：§4.1 完整管线 `doExecuteInternal()`（行 2790、2817、2827、2845）
- **问题描述**：在完整管线的 `doExecuteInternal()` 伪代码中，多个降级路径调用 `doDegrade()` 时传入的 `promptVersion` 为 `null`（行 2790、2817、2827），即使此时实验分流已经执行完毕且 `promptVersion` 已从 `assignment.getTargetPromptVersion()` 获取（行 2700）。人工追踪发现行 2790 和 2827 两个降级位置发生在实验分流**之后**，`promptVersion` 已经可用，但伪代码传入了 `null`。这将导致降级场景的 `AiCallRecord` 丢失实验分流信息，影响 A/B 实验效果分析。

- **严重程度**：中等（降级路径下 A/B 实验分流数据丢失）
- **改进建议**：追踪实验分流完成后（行 2700）每个降级调用传参，确保 `promptVersion` 被正确传入。具体：
  - 行 2790（structuredChat AiResult 非成功）：应将 `promptVersion` 而非 `null` 传入 `doDegrade()`
  - 行 2827（chat AiResult 非成功）：同上
  - 行 2845（LlmInfrastructureException 降级）：同上
  - 行 2817（ParseFailure 降级）：同上（此时已走完了 chat 路径，promptVersion 仍然可用）

---

### 问题 7：[中等] 降级预检 `buildDegradationContext()` 调用顺序在类图与伪代码间不一致

- **所在位置**：§3.1 类图（行 592–593）；§4.1 `execute()` 伪代码（行 2661）
- **问题描述**：类图中 `SlidingWindowMetricsStore.buildDegradationContext()` 返回 `DegradationContext` 实例。此方法在 `execute()` 伪代码中（行 2661）在**防御性拷贝之后、降级预检之前**调用。但 `buildDegradationContext()` 的实现伪代码（行 2085）未明确是否对 `request` 有状态读取要求。如果 `buildDegradationContext()` 内部读取了 `request` 的字段但此时防御性拷贝已执行（行 2654），则使用 `defensiveCopy` 而非原始 `request` 是合理的；但行 2661 使用的是原始的 `request` 引用（已被 `defensiveCopy` 覆盖的方法体起始处并未重新赋值 `request`——行 2654 将 `convertValue` 结果存入新变量 `defensiveCopy`，`request` 本身未被覆盖）。实际上这是正确的，因为 `buildDegradationContext()` 只需要 `capabilityId` 和 `requestType`，不访问 `request`。但代码可读性上可以改进：由于 `request` 在行 2654 之后继续存在（未被覆盖），`buildDegradationContext()` 的行 2661 明确引用了 `request`——此时 `request` 仍然是原始 DTO 而非 `defensiveCopy`。建议将行 2661 的调用移入降级预检循环之前，并明确说明 `buildDegradationContext()` 是否需要访问 request。

- **严重程度**：中等（不影响正确性，但降低可读性和维护性）
- **改进建议**：在 §4.1 伪代码行 2661 后添加注释，说明 `buildDegradationContext()` 无需访问 `request` 字段、仅依赖 `capabilityId` 和 `requestType` 参数，因此防御性拷贝的时序不影响其正确性。

---

## 未发现显著问题的方面

- **需求响应充分度**：文档完整覆盖了用户需求中提及的 Phase5 包 G 全部 7 项底座能力和 6 项薄适配器能力，类图、核心职责、协作关系和状态模型均已覆盖。设计风格与 Phase0/Phase1ABD 一致，§1.2 已明确列出 5 条具体规则。
- **异常场景覆盖率**：§5.1 错误分类表覆盖了 12 种错误场景的响应行为，每个异常都有对应的降级路径和 `DegradationReason` 枚举值。
- **并发与线程安全**：§6 完全覆盖了线程模型、熔断器线程安全、异步上下文传播机制，且所有 ThreadLocal 传播场景均通过"入口处提取+闭包捕获"模式解决。
- **状态机完整性**：6 个状态机组件（`CircuitBreakerDegradationStrategy`、`ModelEndpointHealthManager`、`CredentialProvider`、`PromptTemplate`、`Experiment`、`DegradationContext`）均有完整的状态定义、转换表和恢复路径描述。
- **数据库索引与清理策略**：`AiCallLogEntity` 的 10 个索引定义合理，180 天保留期和按月分区清理策略清晰。
- **跨包协作**：§10 明确了与包 F、包 E、Phase 4 模块的协作边界和接口引用约束。

---

## 整体评价

文档在架构完整性、异常处理、并发模型等方面达到较高成熟度。核心问题集中在**实施指导层面**：父类构造器上的 Spring 注解不可达（问题 1）和子类 `super()` 参数缺失（问题 2）是必须修复的阻塞性问题。这些问题不影响设计方向的正确性，但实施者直接按当前伪代码编码将遇到运行期失败。建议在 v9 中优先解决这两个构造器注入问题，并补充至少一个完整管线子类的构造器示例。
