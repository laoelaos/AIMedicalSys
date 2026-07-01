# 质量审查诊断报告 — Phase 5 包G OOD v19

## 审查范围与视角

- **审查对象**：Phase 5 包G OOD 设计方案（v19）
- **审查视角**：需求响应充分度、整体深度与完整性、实际落地可实施性
- **前置说明**：本产出已通过组件A的多轮内部审议（覆盖技术可行性等维度），本报告侧重内部审议尚未充分覆盖的维度，避免重复验证已确认的技术问题

---

## 问题1（重要 — 逻辑矛盾）

**§2.3 类图中 `HttpApiLlmChatService` / `SpringAiLlmChatService` 的双接口实现与 reactor-core 隔离设计意图矛盾**

**问题描述**：§2.3 类图（第 583-586 行）中 `HttpApiLlmChatService` 和 `SpringAiLlmChatService` 同时实现 `LlmChatService` 和 `LlmChatStreamService` 两个接口。但 §3.2 明确独立 `LlmChatStreamService` 接口的设计理由是"`LlmChatService` 的方法签名不涉及 `Flux`，无需 `reactor-core` 编译期依赖"以及"`reactor-core` 仅作为流式使用方的可选依赖，非流式场景无需引入"。因为 `LlmChatStreamService.chatStream()` 返回 `Flux<LlmChatResponse>`，任何一个同时实现两个接口的实现类（包括仅需非流式调用的 `HttpApiLlmChatService`）都必须在编译期依赖 `reactor-core`，这与设计意图直接矛盾。

**所在位置**：§2.3 类图第 583-586 行；§3.2 `LlmChatStreamService` 职责描述段（"为何独立于 LlmChatService"）

**严重程度**：重要

**改进建议**：方案 A（推荐）：将两个接口的实现分离——`HttpApiLlmChatService` 仅实现 `LlmChatService`，新增 `HttpApiLlmChatStreamService`（或 Reactor 适配器）仅实现 `LlmChatStreamService`；`SpringAiLlmChatService` 同理。方案 B：若维护一致认为合并实现更符合项目实际（两个服务共用同一个 HTTP 客户端），则需修改 §3.2 的设计说明，明确承认 `reactor-core` 为 `ai-impl` 的非可选编译期依赖，并更新依赖管理说明。

---

## 问题2（重要 — 设计缺口/完整性不足）

**`thinAdapterTimeout` 与 `capabilityTimeoutConfig` 的注入/初始化机制未定义，无法直接指导编码实现**

**问题描述**：`AbstractCapabilityExecutor` 类图（§2.3 第 239-240 行）声明了 `#Duration thinAdapterTimeout` 和 `#Map<String, Duration> capabilityTimeoutConfig` 两个字段，§4.1 伪代码（第 1864、1999 行）中直接使用这两个字段。然而：
1. 全文未定义这两个字段的注入来源——既不在 `DiagnosisCapabilityExecutor` 构造函数（§3.1 第 773-784 行）中作为构造参数传入，也没有 `@Value` 或 `@ConfigurationProperties` 注解读取方式的伪代码示例
2. `capabilityTimeoutConfig` 在 `AiPlatformConfig` 有 `@Bean` 方法声明（§3.9 第 1719-1723 行），但 `AbstractCapabilityExecutor` 如何获取此 bean 未说明（@Inject? 构造器注入?）
3. `thinAdapterTimeout` 完全没有注入来源的文档，仅在 §9.5 YAML 中有 `thin-adapter-default: 30s` 配置项，但绑定路径未知

实现者在编码时将无法确定这两个关键超时配置如何初始化，需要自行推断注入方式。

**所在位置**：§2.3 类图第 239-240 行；§3.1 第 773-784 行（构造函数示例）；§3.1 第 987-991 行（超时机制描述）；§3.9 第 1719-1723 行（@Bean 声明）

**严重程度**：重要

**改进建议**：在 `AbstractCapabilityExecutor` 的构造函数伪代码中显式补充 `capabilityTimeoutConfig` 和 `thinAdapterTimeout` 的注入方式。推荐方案：(1) 在 `AbstractCapabilityExecutor` 构造函数中增加 `@Value("#{${ai.execution.timeout.per-capability}}") Map<String, Duration> capabilityTimeoutConfig` 和 `@Value("${ai.execution.timeout.thin-adapter-default:30s}") Duration thinAdapterTimeout` 参数；(2) 或在 `AiPlatformConfig` 中为这两个配置提供显式 `@Bean` 方法，并通过 `@Inject` 注入到 `AbstractCapabilityExecutor` 构造器。

---

## 问题3（重要 — 设计缺口/实际落地风险）

**薄适配器型 CapabilityExecutor 缺少条件化 Bean 注册保护，Phase 4 服务不可用时容器启动失败**

**问题描述**：6 个薄适配器 `CapabilityExecutor` 实现（如 `DiagnosisCapabilityExecutor`）标注为 `@Component` 且构造器直接注入 Phase 4 业务服务接口（如 `DiagnosisService`）。根据 §2.2 的设计，Phase 4 模块的 Maven 依赖为 `provided` 作用域，这意味着：
1. 即使 `ai.platform.enabled=false`（底座关闭），薄适配器 `@Component` 仍会被 Spring 扫描并尝试实例化
2. 若 Phase 4 服务模块未部署或接口 bean 不存在，Spring 上下文将在启动期直接失败（`UnsatisfiedDependencyException`），而非优雅降级
3. §2.2 仅讨论了 uber-JAR 打包的类加载风险，未涉及 Spring bean 不存在场景的处理策略

**所在位置**：§3.1 第 769 行（`@Component` 声明）；§3.1 第 757-768 行（依赖注入描述）；§2.2 第 188 行（provided 作用域与运行时风险说明）

**严重程度**：重要

**改进建议**：方案 A：为每个薄适配器 `CapabilityExecutor` 添加 `@ConditionalOnProperty(name = "ai.platform.enabled", havingValue = "true")`，当底座关闭时不实例化这些 bean，避免因 Phase 4 服务不可用导致启动失败。方案 B（更彻底的方案）：为薄适配器增加 `@ConditionalOnBean` 或 `@ConditionalOnClass` 守卫，自动跳过 Phase 4 模块未部署时的加载，同时配合 §10.3 的依赖隔离规则。

---

## 问题4（中等 — 逻辑矛盾/完整性不足）

**薄适配器构造函数包含不必要的基础设施依赖**

**问题描述**：`DiagnosisCapabilityExecutor` 构造函数（§3.1 第 773-784 行）注入了 `PromptTemplateManager`、`ModelRouter`、`LlmChatService`、`StructuredOutputParser`、`ModelEndpointHealthManager`、`LocalRuleFallback` 等基础设施组件。但根据 §3.1 薄适配器管线行为描述（第 751-755 行），薄适配器不包含模板渲染、模型路由、LLM 调用、结构化输出解析流程，`doExecuteInternal()` 特化伪代码（§4.2）也证实这些组件在薄适配器中不被使用。这些无用的依赖增加了内存开销和构造器参数复杂性，且当这些基础设施 bean 初始化异常时，会拖累薄适配器的启动成功率（即底座部分组件异常会波及薄适配器的实例化）。

**所在位置**：§3.1 第 773-784 行（构造函数示例）；§3.1 第 751-755 行（薄适配器行为描述）；§4.2 伪代码

**严重程度**：中等

**改进建议**：方案 A：为薄适配器引入独立的构造器签名，仅注入其实际使用的依赖（`Phase 4 Service`、`AiMetricsCollector`、`SlidingWindowMetricsStore`、`List<DegradationStrategy>`、`LocalRuleFallback`），避免不必要的依赖耦合。方案 B（最小改动）：将基础设施依赖的 `@Autowired` 标记为 `required = false`，并在文档中注明薄适配器不使用这些组件。

---

## 问题5（中等 — 完整性不足/可实施性）

**`@Async` 指标采集线程池拒绝策略配置与 `@Bean` 定义不一致**

**问题描述**：§3.5 指标采集的拒绝策略描述为"`DiscardPolicy` + 日志 WARN"，YAML 配置（§9.5 第 2371 行）也配置 `rejection-policy: Discard`。但 §3.9 `AiPlatformConfig` 中的 `metricsAsyncExecutor` `@Bean` 方法（第 1698-1710 行）使用 `new ThreadPoolExecutor.DiscardPolicy()`。`ThreadPoolExecutor.DiscardPolicy` 静默丢弃（无日志），不符合设计要求的"+ 日志 WARN"。实际编码时需要额外包裹 `DiscardPolicy` 或在拒绝时手动记录 WARN 日志，而设计文档未说明此差异。

**所在位置**：§3.5 第 1377 行（"+ 日志 WARN"描述）；§3.9 第 1706 行（`DiscardPolicy` 实例化）；§9.5 第 2371 行（YAML 配置）

**严重程度**：中等

**改进建议**：在 §3.9 的 `@Bean` 方法伪代码中将 `new ThreadPoolExecutor.DiscardPolicy()` 改为 `new ThreadPoolExecutor.DiscardPolicy()` 后追加日志记录的说明，例如：策略需自定义继承 `DiscardPolicy` 并重写 `rejectedExecution()` 方法以输出 WARN 级别日志，或在 `@Bean` 方法注释中指明"生产实现需自定义 DiscardPolicy 以记录 WARN 日志"。同时 §3.5 的文本描述保持一致。

---

## 问题6（一般 — 完整性不足）

**`AiOrchestrator.handle()` catch 块中 `request instanceof AiRequestBase` 的判断逻辑未处理 request 因其他原因被篡改或代理的场景**

**问题描述**：§4.1 第 1807 行 `if request instanceof AiRequestBase` 的判断在大多数场景下正确，但未考虑以下边缘情况：(1) 若 `request` 是 CGLIB 代理对象（某些 AOP 场景下），`instanceof` 可能返回 false 导致误入薄适配器提取路径，丢失就医上下文；(2) 若 `executor.execute()` 抛出异常时 `request` 已被 `doExecuteInternal()` 内部的防御性拷贝操作修改（虽契约禁止但无法强制执行），catch 块提取到的上下文可能不完整。此问题虽概率低但属于可预防的防御性设计缺口。

**所在位置**：§4.1 第 1807 行（catch 块 instanceof 判断）

**严重程度**：一般

**改进建议**：在 catch 块中增加双重提取策略：优先尝试 `request instanceof AiRequestBase` 提取，若提取到的所有上下文字段均为 null 则降级到 `extractFromRequestContext()` 提取路径作为兜底，而非仅依赖 `instanceof` 二分支判断。同时增加注释说明代理对象的 `instanceof` 局限性。

---

## 总体评价

本产出经过 19 轮迭代改进，文档结构完整、细节丰富，在技术方案层面已趋于成熟。发现的 6 个问题主要集中在接口实现一致性（问题 1）、配置注入完整性（问题 2）、条件化 Bean 注册（问题 3）、依赖合理性（问题 4）、配置与实现一致性（问题 5）、防御性边界覆盖（问题 6）等维度。这些问题均非根本性设计缺陷，修复后可满足编码实现和下游消费者集成的需求。
