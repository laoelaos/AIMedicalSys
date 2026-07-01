# 质量审查报告 — Phase 5 包 G AI 进阶底座 OOD 设计方案 v15

## 审查范围
- **审查维度**：需求响应充分度、整体深度与完整性、事实正确性与逻辑一致性（重点覆盖内部审议未充分覆盖的维度）
- **审查视角**：实际落地视角——设计是否可直接指导编码实现、接口定义是否足以支持下游消费者、异常场景和边界条件是否已考虑

---

## 问题清单

### 问题 1：AiPlatformConfig 核心配置类缺失正式定义

- **问题描述**：`AiPlatformConfig` 作为底座的核心配置装配类，在文档中被引用约 15 次以上，负责 YAML 配置绑定、策略注入 Map 构建、Bean 装配协调、EnvironmentPostProcessor 配置转发等功能。然而文档从未提供该类的类图、核心 @Bean 方法签名、@ConfigurationProperties 绑定前缀等正式定义。实现者无法据此确定 `AiPlatformConfig` 的完整职责边界和代码结构。
- **所在位置**：§2.1 目录结构（line 145）仅列出文件名无结构定义；§3.1 降级策略注入、Bean 装配策略、配置转发等 10+ 处引用均依赖该类但未定义其内部结构；§2.3 类图缺失该类型
- **严重程度**：严重 — 实现者无法确定此关键配置类的代码骨架
- **改进建议**：在 §2.3 类图中补充 `AiPlatformConfig` 类型，标注其 `implements ApplicationContextAware` 和注解（`@Configuration`、`@ConfigurationProperties(prefix = "ai")`），列出核心 @Bean 方法（如策略 Map 暴露、线程池定义等）及其返回类型；或在 §3 中新增一节专门描述该配置类的结构

### 问题 2：LlmCallExecutor 与指标采集线程池的 Spring Bean 定义缺失

- **问题描述**：`LlmCallExecutor` 线程池和指标采集专用线程池在 §3.2、§3.5、§6.1 中多处被引用并进行设计分析（核心线程数、队列容量、拒绝策略 `CallerRunsPolicy`/`DiscardPolicy`），但文档未提供这两个线程池的 Spring Bean 定义方式——在哪个配置类中定义？Bean name 是什么？如何被 `CapabilityExecutor` 和 `@Async` 引用？`CallerRunsPolicy` 的风险分析已具备但对应的 Bean 配置示例缺失。
  - 指标采集线程池的 `@Async` 配置也未定义——使用 Spring Boot 自动配置的默认线程池还是自定义 `AsyncConfigurer`？
- **所在位置**：§3.2（line 919）描述了 LlmCallExecutor 参数但无 Bean 定义；§3.5（lines 1215-1219）描述了指标采集线程池参数但无 Bean 定义；§6.1（line 1799）引用专用线程池隔离但不定义来源
- **严重程度**：重要 — 缺少 Bean 定义使线程池配置无法直接映射到代码
- **改进建议**：在 §3.1（或新增的 `AiPlatformConfig` 定义节）中补充两个线程池的 @Bean 定义伪代码：
  - `LlmCallExecutor`：`@Bean("llmCallExecutor")` + `ThreadPoolExecutor` 构造参数 + 配置绑定方式
  - 指标采集线程池：`@Bean("metricsAsyncExecutor")` + `ThreadPoolTaskExecutor` 参数，并说明 `@Async("metricsAsyncExecutor")` 的引用方式
  - YAML 配置中补充对应 `ai.execution.thread-pool.llm-call` 和 `ai.metrics.async` 配置块

### 问题 3：AiOrchestrator.handle() 与 AiService 13 方法的映射关系未显式定义

- **问题描述**：AiOrchestrator 被声明实现 `AiService` 接口（§2.3 类图 line 467），类图（lines 200-208）展示了 13 个具体方法如 `triage()`、`prescriptionCheck()`，但 §4.1 行为契约伪代码（line 1524）仅定义了一个泛化 `handle(capabilityId, request)` 方法。文档未显式说明 "13 个 AiService 方法各自通过能力标识映射表调用 `handle(capabilityId, request)`" 这一委托关系。能力标识映射表（§3.1 lines 557-572）虽然建立了方法名到 capabilityId 的对应关系，但缺乏与 `handle()` 方法的桥接说明。
- **所在位置**：§4.1（line 1524）handle() 方法 vs §2.3 类图（lines 200-208）13 个具体方法 vs §3.1 映射表（lines 557-572）
- **严重程度**：重要 — 类图与行为契约的表述不一致可能导致不同实现者对委托逻辑的理解产生分歧
- **改进建议**：在 §4.1 handle() 伪代码前新增注释块或说明段落，显式表述："AiOrchestrator 实现 AiService 的 13 个方法，每个方法内部通过能力标识映射表（见 §3.1 映射表）将方法入参转发到 handle(capabilityId, request)，handle() 是本节的契约主体"。或在伪代码下方补充 `triage()` 方法作为完整委托示例。

### 问题 4：薄适配器 CapabilityExecutor doExecuteInternal() 的行为契约仅存在于 §3.1 而非 §4.1

- **问题描述**：§4.1「关键行为契约」应完整覆盖所有 CapabilityExecutor 的执行行为类型。当前 §4.1 的 `doExecuteInternal()` 伪代码（lines 1608-1684）仅展示完整管线子类（7 项底座能力）的实现，薄适配器子类（6 项 Phase 4 能力）的简化和特化行为（独立超时控制、委托 Phase 4 服务、`retryCount=0` 限制等）仅出现在 §3.1（lines 742-794）的文本描述中。实现者在 §4.1 找不到薄适配器的行为契约，需跨章翻查，增加了实现时遗漏关键约束（如线程饥饿风险、`cancel(true)` 无作用）的风险。
- **所在位置**：§4.1 完整管线 (lines 1608-1684) vs §3.1 薄适配器 (lines 742-794)
- **严重程度**：重要 — 行为契约分散降低设计文档的权威性和可追踪性
- **改进建议**：在 §4.1 的 `doExecuteInternal()` 伪代码之后或之前，补充薄适配器子类的特化版伪代码，覆盖以下差异点：(1) 使用公共 ForkJoinPool 而非 llmCallExecutor；(2) 独立 `thinAdapterTimeout` 超时控制；(3) Phase 4 业务异常与基础设施异常分离处理；(4) `retryCount=0` 的限制注释

### 问题 5：AiOrchestrator 持有 ModelEndpointHealthManager 但 handle() 伪代码未使用

- **问题描述**：§2.3 类图（line 204）和 §3.1 协作对象列表（line 582）均将 `ModelEndpointHealthManager` 列为 AiOrchestrator 的字段/协作对象。但 §4.1 `AiOrchestrator.handle()` 伪代码（lines 1524-1560）中未对该组件做任何调用。实际使用 `ModelEndpointHealthManager` 的是 `CapabilityExecutor` 管线内部（§4.1 line 1637-1640）。AiOrchestrator 持有该组件仅在 `AiOrchestrator.handle()` 的 catch 块中可能作为兜底使用（但也未在伪代码中体现），形成事实上的死字段。
- **所在位置**：§2.3 类图（line 204）vs §4.1 handle()（lines 1524-1560）vs §4.1 doExecuteInternal()（line 1637-1640）
- **严重程度**：重要 — AiOrchestrator 的协作对象定义与行为伪代码不一致，干扰实现者判断
- **改进建议**：两个修正方向二选一：(a) 推荐方案——从 AiOrchestrator 类图和协作对象列表中移除 `ModelEndpointHealthManager`，因为 CapabilityExecutor 已直接注入并使用该组件；(b) 若 AiOrchestrator 确实需要持有以便 `handle()` 兜底，则在 handle() 伪代码中补充使用场景

### 问题 6：AiOrchestrator.handle() catch 块中 extractHeader() 工具方法未定义

- **问题描述**：§4.1 `AiOrchestrator.handle()` catch 块的 Phase 4 DTO 兼容提取路径（lines 1548-1552）中，4 次调用 `extractHeader(requestAttributes, "X-...")` 方法。但该工具方法在文档中从未定义——既不在任何类中声明，也未给出默认实现说明。同时 §3.1 薄适配器 `doExtractDepartmentId()` 伪代码（line 728）中使用 `extractFromRequestContext("X-Department-ID")` 实现类似目标，命名存在不一致。
- **所在位置**：§4.1（lines 1549-1552）vs §3.1（line 728）
- **严重程度**：一般 — 实现者需自行推断实现（`requestAttributes.getRequest().getHeader("X-...")`），但命名不一致增加认知负担
- **改进建议**：统一方法命名——建议统一为 `extractFromRequestContext(String headerName)`——并在 §3.1 或 §3.5 的工具方法段中给出默认实现说明，例如："默认实现：`requestAttributes.getRequest().getHeader(headerName)`，`requestAttributes` 来自 `RequestContextHolder.currentRequestAttributes()`"

---

## 整体评价

该设计文档经过 18 轮迭代审议，在技术可行性、组件职责、类图、状态模型、线程安全、降级策略、配置管理等维度已高度成熟，内部审议覆盖充分。文档的结构完整性和细节丰富度在同级 OOD 设计中属较高水平。

**但在"直接指导编码实现"视角下，仍存在 6 个障碍性问题**：`AiPlatformConfig` 未定义（问题 1）、两个线程池无 Bean 定义（问题 2）、类图与行为契约的方法表述不一致（问题 3）、薄适配器行为契约分散（问题 4）、组件协作关系伪代码不对应（问题 5）、工具方法未定义（问题 6）。这些问题均位于实现者从设计到代码的"最后一公里"环节，若不修复则实现者需要自行补充大量设计未冻结的决策。

建议在下一轮迭代中优先解决问题 1~4（严重/重要级别），问题 5~6 可同步修复。
