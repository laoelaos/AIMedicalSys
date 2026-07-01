根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

**问题1（重要 — 逻辑矛盾）**：§2.3 类图中 `HttpApiLlmChatService` / `SpringAiLlmChatService` 的双接口实现与 reactor-core 隔离设计意图矛盾——两个实现类同时实现 `LlmChatService` 和 `LlmChatStreamService`，导致非流式场景也必须在编译期依赖 `reactor-core`，与 `LlmChatStreamService` 独立设计的理由直接冲突。

- **改进建议**：方案A（推荐）：分离接口实现——`HttpApiLlmChatService` 仅实现 `LlmChatService`，新增 `HttpApiLlmChatStreamService` 仅实现 `LlmChatStreamService`。方案B：修改 §3.2 设计说明，明确承认 `reactor-core` 为 `ai-impl` 的非可选编译期依赖。

**问题2（重要 — 设计缺口/完整性不足）**：`thinAdapterTimeout` 与 `capabilityTimeoutConfig` 的注入/初始化机制未定义——类图声明了这两个字段但全文未定义注入来源，`capabilityTimeoutConfig` 在 `AiPlatformConfig` 有 `@Bean` 声明但 `AbstractCapabilityExecutor` 如何获取未说明，`thinAdapterTimeout` 完全没有注入来源文档。

- **改进建议**：在 `AbstractCapabilityExecutor` 的构造函数伪代码中显式补充两个字段的注入方式，推荐在构造函数参数中使用 `@Value` 或通过 `AiPlatformConfig` 的 `@Bean` 方法注入。

**问题3（重要 — 设计缺口/实际落地风险）**：薄适配器型 CapabilityExecutor 缺少条件化 Bean 注册保护——6 个薄适配器标注为 `@Component` 且构造器直接注入 Phase 4 业务服务接口，即使 `ai.platform.enabled=false` 仍会被扫描实例化，Phase 4 服务不可用时容器启动失败。

- **改进建议**：为每个薄适配器添加 `@ConditionalOnProperty(name = "ai.platform.enabled", havingValue = "true")`，或增加 `@ConditionalOnBean` / `@ConditionalOnClass` 守卫。

**问题4（中等 — 逻辑矛盾/完整性不足）**：薄适配器构造函数包含不必要的基础设施依赖——`DiagnosisCapabilityExecutor` 构造函数注入了 `PromptTemplateManager`、`ModelRouter`、`LlmChatService`、`StructuredOutputParser` 等组件，但薄适配器管线不使用这些组件，增加内存开销和启动耦合风险。

- **改进建议**：为薄适配器引入独立的构造器签名，仅注入实际使用的依赖（Phase 4 Service、`AiMetricsCollector`、`SlidingWindowMetricsStore`、`List<DegradationStrategy>`、`LocalRuleFallback`）。

**问题5（中等 — 完整性不足/可实施性）**：`@Async` 指标采集线程池拒绝策略配置与 `@Bean` 定义不一致——§3.5 要求"`DiscardPolicy` + 日志 WARN"，但 §3.9 中 `metricsAsyncExecutor` `@Bean` 方法使用 `ThreadPoolExecutor.DiscardPolicy`（静默丢弃，无日志）。

- **改进建议**：在 `@Bean` 方法伪代码中改为自定义继承 `DiscardPolicy` 并重写 `rejectedExecution()` 以输出 WARN 日志，或在注释中说明生产实现需自定义。

**问题6（一般 — 完整性不足）**：`AiOrchestrator.handle()` catch 块中 `request instanceof AiRequestBase` 的判断逻辑未处理 request 因 CGLIB 代理或其他原因被篡改的场景。

- **改进建议**：在 catch 块中增加双重提取策略——优先 `instanceof` 提取，若提取到的上下文字段均为 null 则降级到 `extractFromRequestContext()` 作为兜底。

## 历史迭代回顾

分析历史反馈与当前反馈的关系：

- **持续存在的问题（在多轮反馈中反复出现，需重点解决）**：
  - `thinAdapterTimeout` / `capabilityTimeoutConfig` 注入机制未定义：第14轮第6项已指出"capabilityTimeoutConfig 字段在类图和构造器中均未定义"，本轮问题2再次提出同样问题，需在构造函数伪代码中补充注入定义
  - `AiOrchestrator.handle()` catch 块异常处理不完善：第10轮第3项（丢失就诊上下文）、第13轮第1项（薄适配器 DTO instanceof 失效）、第15轮第6项（extractHeader() 未定义）持续涉及此区域，本轮问题6进一步指出 instanceof 代理场景缺陷

- **新发现的问题（本轮新识别）**：
  - 问题1：接口实现与 reactor-core 隔离设计意图矛盾
  - 问题3：薄适配器缺少条件化 Bean 注册保护
  - 问题4：薄适配器构造函数含不必要基础设施依赖
  - 问题5：指标采集线程池拒绝策略不一致

## 上一轮产出路径
C:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606271627_phase5_pkgG_ood/a_v19_copy_from_v18.md

## 用户需求
C:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606271627_phase5_pkgG_ood/requirement.md
