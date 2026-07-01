# Phase 5 包 G OOD 设计文档（v20）质量审查报告 v2

审查范围：`a_v20_copy_from_v19.md`（v20，基于 v19 的拷贝）
审查视角：需求响应充分度、整体深度和完整性、异常场景与边界条件系统性评估
审查依据：需求文档、迭代历史（19 轮反馈）、上一轮诊断（b_v20_diag_v1）、上一轮质询（b_v20_challenge_v1）、Phase0（`Docs/04_ood_phase0.md`）、Phase1ABD（`Docs/05_ood_phase1_B.md`）

**说明**：v1 诊断已识别 12 个技术性细节问题（签名匹配、字段遗漏、注入矛盾等），v1 质询指出其重心与任务要求的"需求响应充分度、整体深度和完整性"维度偏离。本 v2 报告侧重质询指出的未覆盖维度，不与 v1 问题重复。

---

## 整体评价：需求响应充分度

**需求要素逐项对照**：

| 需求要素 | 覆盖状态 | 说明 |
|---------|---------|------|
| 参 Phase0/Phase1ABD 设计风格一致性 | 部分覆盖 | §1.2 末段声明参照但未验证，详见问题 1 |
| 类图 | ✓ 覆盖 | §2.3 完整的 Mermaid classDiagram |
| 核心职责 | ✓ 覆盖 | §3 逐抽象描述 |
| 协作关系 | 部分覆盖 | 静态类图 ✓，特定场景序列图 ✓，但缺少端到端运行时协作视图，详见问题 5 |
| 关键接口 | ✓ 覆盖 | 方法签名、契约、协作对象均覆盖 |
| 状态模型 | ✓ 覆盖 | Experiment/PromptTemplate/CircuitBreaker/EndpointHealthManager 均有状态机 |

---

## 问题 1（重要，架构缺口 — 深度不足）：`LlmChatService` 多实现实例的选择机制未定义

- **所在位置**：§2.3 类图 `CapabilityExecutor --> LlmChatService : uses`（单条关联线）、§3.2 `LlmChatService` 接口与实现节、§4.1 管线伪代码第 1973 行/第 1979 行
- **问题描述**：设计定义了 `HttpApiLlmChatService` 和 `SpringAiLlmChatService` 两个 `LlmChatService` 实现，`ModelRoute` 值对象包含 `clientType`（`HTTP_API` / `SPRING_AI`）用于区分路由目标协议。但 `CapabilityExecutor` 管线中仅持有单一的 `LlmChatService` 注入依赖（§2.3 类图）并发起 `llmChatService.chat(chatRequest)` 调用（§4.1 第 1979 行），不存在按 `ModelRoute.clientType` 选择实现的机制。由此产生三个确定性问题：
  - (a) **Bean 装配二义性**：两个 `@Component` 实现同时注册时，`CapabilityExecutor` 单字段注入（无 `@Qualifier`、无 `@Primary`）将导致 `NoUniqueBeanDefinitionException`，容器启动失败
  - (b) **路由与实现的脱节**：即使通过 `@Primary` 解决了二义性（如固定使用 `HttpApiLlmChatService`），`ModelRoute.clientType = SPRING_AI` 的路由条目将请求错误协议，产生运行时调用失败
  - (c) **设计意图未冻结**：v17 引入双实现的目的是支撑两种接入方式共存，但组件间缺少一个"按 clientType 派发到正确实现"的分发层（如 `DelegatingLlmChatService` 内部持有两个实现并按 `ModelRoute.clientType` 委托）
- **严重程度**：重要——直接阻碍编码实现，实现者无法确定应注入哪个 Bean 及如何完成运行时派发
- **改进建议**：补充 `DelegatingLlmChatService` 分发层——内部持有两个实现实例，在 `chat()` 入口处按 `ModelRoute.clientType` 委托到对应实现；或冻结设计承诺为"部署时通过 `@ConditionalOnProperty` 确保仅一种实现实例化"并删除 `ModelRoute.clientType` 字段以避免虚假的扩展预期

## 问题 2（重要，完整性不足 — 异常边界）：`structuredChat()` 回退路径的异常捕获粒度过粗

- **所在位置**：§4.1 `doExecuteInternal()` 管线伪代码第 1972-1978 行
- **问题描述**：`structuredChat()` 调用外层使用 `catch (Exception e)` 捕获所有异常后进入 `chat()` 回退路径。此捕获粒度未区分异常类型：
  - 若失败因基础设施不可用（HTTP 5xx、连接超时、凭据过期），`chat()` 将对同一不可用端点发出第二次请求，几乎必然同样失败，导致端到端耗时翻倍且无收益
  - 若失败因模型不支持 `structuredChat` 格式（如 tool_use 能力缺失），`chat()` 回退可正常工作——是合理场景
  - 设计文档 §5.1 错误分类表中"LLM 调用不可用"和"结构化输出解析失败"是两个独立类别，但管线伪代码中将两者合并为同一 catch 路径，异常分类表与实现路径实质性矛盾
  - 此问题与 §5.1 的错误分类承诺（LLM 不可用 → 重试 1 次后降级；结构化解析失败 → 降级）不一致：当前伪代码对基础设施异常不重试直接回退 chat()，对格式异常也不会在回退前标记区分
- **严重程度**：重要——异常分类表与实现不一致，且在基础设施故障场景下增加不必要的延迟和成本
- **改进建议**：将单一 catch 拆分为两个分支：(a) `catch (StructuredChatNotSupportedException)` 或格式类异常 → chat() 回退（合理）；(b) `catch (InfrastructureException | HttpServerErrorException)` 等基础设施异常 → 跳过 chat() 回退，直接进入降级路径（降级原因标记 `DegradationReason.INFRASTRUCTURE_ERROR`）。需在 `LlmChatService` 接口层面定义对应的异常类型层级

## 问题 3（中等，完整性不足 — 文档一致）：`RouteConfigChangedEvent` 事件定义缺失

- **所在位置**：§6.1 第 2181 行提到"管理端发布 `RouteConfigChangedEvent`"但全文无正式定义
- **问题描述**：§3.3 和 §3.4 分别为 `TemplateChangedEvent` 和 `ExperimentChangedEvent` 提供了完整的 Payload 定义（包含 capabilityId、changeType、changedAt 等字段），但 §6.1 提及的 `RouteConfigChangedEvent` 仅出现名称而无任何字段定义。实现者无法根据文档实现事件类、无法确定事件体应包含哪些信息（路由条目列表全量 / 增量 patch / 仅变更标识）、无法确定发布/消费方的时序契约
- **严重程度**：中等——与已有的事件定义风格不一致，导致实现者必须自行推测事件结构
- **改进建议**：在 §3.2 `ModelRouter` 段落末尾或 §6 末尾补充 `RouteConfigChangedEvent` 的 Payload 定义（建议字段：`affectedCapabilityIds: List<String>`、`changeType: ChangeType`、`changedAt: Instant`），明确刷新策略（增量更新还是全量替换）以及 `@EventListener` 消费的异常行为

## 问题 4（中等，完整性不足 — 运维深度）：`CircuitBreakerDegradationStrategy` 状态可观测性缺失

- **所在位置**：§3.8 `CircuitBreakerDegradationStrategy` 定义段、§3.5 `AiMetricsCollector` 指标采集段
- **问题描述**：`ModelEndpointHealthManager` 提供了 `getState()` 方法暴露端点健康状态（CONNECTED/DEGRADED/UNAVAILABLE），但 `CircuitBreakerDegradationStrategy`（CLOSED/OPEN/HALF_OPEN 状态机）未提供任何状态查询接口。运维/监控系统无法获取熔断器实时状态。设计的五个 Micrometer 指标（`ai-medical.ai.request.duration`、`request.count`、`degradation.count`、`token.usage`）均为调用级指标，不包含熔断器瞬时状态。当熔断器 OPEN 导致大量请求被静默降级时，运维只能从 degradation count 的间接增长推断状态，无法直接查询熔断器是 CLOSED 还是 OPEN
- **严重程度**：中等——不影响首次功能实现，但生产部署时排除熔断器相关告警需依赖日志模式推测
- **改进建议**：(a) 在 `CircuitBreakerDegradationStrategy` 中补充 `getState(capabilityId): CircuitState` 接口方法；(b) 通过 Micrometer `Gauge` 或自定义指标暴露每个能力标识的熔断器状态值（如 0=CLOSED, 1=OPEN, 2=HALF_OPEN），注册到 `AiMetricsCollector` 的 `@PostConstruct` 阶段

## 问题 5（中等，逻辑矛盾 — 需求响应）：X-Department-ID HTTP Header 提取的依赖假设

- **所在位置**：§3.10 `extractFromRequestContext()` 实现（第 1804-1808 行）、§4.1 `AiOrchestrator.handle()` catch 块（第 1862-1868 行）、§3.1 薄适配器 `doExtractDepartmentId()`（第 814 行）
- **问题描述**：多处伪代码依赖 `extractFromRequestContext("X-Department-ID")` 等 HTTP Header 提取就诊上下文。此机制隐含以下前提：
  - (a) 调用入口必须是 HTTP 请求（RequestContextHolder 可用）
  - (b) 前端/客户端必须在每个 AI 能力请求中携带 `X-Department-ID`、`X-Visit-ID`、`X-Patient-ID`、`X-Session-ID` 四个 Header
  - ⊙ 两个前提均未在设计中显式约定。若调用来自内部 MQ 消息消费、定时任务（batch job）或管理端批量触发，`RequestContextHolder` 将返回 null，Header 提取全部失败，提取到的结果为 null，薄适配器降级路径的 AiCallRecord 中全部就诊上下文字段为空白
  - 更关键的是，**前端/客户端无任何契约文档要求携带这些 Header**。设计文档在 §9.5 的 YAML 配置、§4 行为契约中均未定义"调用方必须在 HTTP Header 中传递 X-Department-ID 等就诊上下文"的 API 契约。实现者将继承这一隐含假设，但前端或其他调用方不会自动知道要传递这些 Header
- **严重程度**：中等——非阻塞（空值也可运行），但丢失就诊上下文的 AiCallRecord 将无法用于按科室/就诊维度的统计分析，这是 §1.1 承诺的"性能观测内建化"目标的实质性降级
- **改进建议**：在 §4 开头或 §1 概述中新增"调用方 Header 契约"小节，显式列出调用方需携带的 HTTP Header 清单（`X-Department-ID`、`X-Visit-ID`、`X-Patient-ID`、`X-Session-ID`），明确 Optional（缺失不导致调用失败但统计分析维度降级）。在 §3.10 注释中补充非 HTTP 场景（如定时任务）的替代提取路径说明（如从 `AiRequestBase` 字段获取而非仅依赖 `RequestContextHolder`）

---

## 整体评价

该文档经过 19 轮迭代，在技术可行性、接口契约、降级策略、线程模型等维度已达到较高成熟度。v1 诊断报告的 12 个技术性细节问题已覆盖了大多数字段级和签名级的不一致。

在需求响应充分度方面，文档覆盖了用户要求的大部分 OOD 核心要素（类图 ✓、核心职责 ✓、关键接口 ✓、状态模型 ✓），但"协作关系"维度仍以伪代码和静态类图为主，缺少端到端运行时协作视图。

在整体深度方面，以上 5 个问题集中于：(1) 双 `LlmChatService` 实现的选择机制缺口（架构级，影响编码启动）；(2) 异常分类与实现路径的不一致；(3) 事件定义的一致性遗漏；(4) 运维可观测性缺失；(5) HTTP Header 依赖的隐含假设。

建议在 v21 中优先处理问题 1（LlmChatService 选择机制），该问题直接影响 Bean 装配正确性和模型路由的运行时可用性，其余问题可在后续轮次跟进修复。
