# Phase 5 包 G — AI 进阶底座 架构级 OOD 设计方案（v30）

> **变更摘要**：本文档为本设计的最终稳定版本。历史修订说明（v2~v26）已全部剥离归档（见 `design_evolution_log.md` 及 `iteration_history.md`），尾部仅保留 v27 修订说明作为最新变更追踪参考。首次阅读可安全跳过尾部修订说明直接阅读正文。主要变更包括：(1) 剥离 v2~v6 修订说明及过程性版本标记；(2) §3.11 移至 §3.10 之后并重新编号为连续序列；(3) 新增 §1.7 实施拓扑顺序；(4) §1.3 补充 AiService/FallbackAiService 线程安全契约；(5) §4.1 新增 CompletionException 异常传播说明与 AiResult 状态检查。**v13 修订**：(1) 新增 §1.4.1 薄适配器性能观测维度缺口，补齐 Phase 4 服务可选元数据接口契约；(2) §3.4 ExperimentAssignment 新增 createErrorFallback() 及哨兵 groupId="experiment-error"，§4.1 实验分流异常 catch 块改用哨兵分组；(3) §3.9 新增 RefreshScope 热加载追踪、§11 新增配置热刷新集成测试；(4) §3.4 ExperimentGroup.percentage 补充千分比设计决策理由。**v14 修订**：(1) §5.1 错误分类表及 §11.1 单元测试模式同步修正为 ExperimentAssignment.createErrorFallback()（groupId="experiment-error"）；(2) §3.11.7 补充 DiscussionConclusionCapabilityExecutor 前置 LLM 调用的线程模型分析与隔离建议；(3) §3.1 构造器参数说明处及 §4.1/§3.5 补充参数聚合上下文对象（ExecutionContext/CallRecordContext）的重构方向说明；(4) §1.3 ClientType 补充枚举值绑定约束与防御性转换说明；(5) §3.2 LlmChatRequest 补充 tools 字段构造不一致性风险与改进建议；(6) §3.2 DelegatingLlmChatService 补充异常传播契约与同步/异步双路径异常覆盖确认。**v15 修订**：(1) 新增 §1.8 非功能性质量分析章节（冷启动/连接池/内存/启动时间）；(2) §4.1 修复 DEGRADED 状态双重计数（移除 failure() 预记）；(3) §1.3 ClientType 防护策略升级（fail-fast + ERROR 日志 + 告警事件）；(4) §3.9 新增 transcriptSummaryExecutor 线程池 @Bean 定义；§3.11.7 线程隔离方案定稿（从"建议"改为具体设计）；(5) §3.5 新增 DTO 改造工作量概览表（~1,520 行）；(6) §4.1 parse() 超时硬编码改为可配置项（新增 execution.timeout.parse.default/per-capability 配置块）。**v17 修订**：(1) §4.1 移除异常降级路径中的系统性双重计数，降级记录统一由 doDegrade() 承担；(2) §3.4/§3.5/§4.1/§4.2 AiCallRecord 工厂方法增加 sentinelReason 参数，实验分流异常时将 prompt_version 写为 -1 哨兵值；(3) §4.1 structuredChatTimeout/chatFallbackTimeout 分配改为基于剩余超时窗口的动态比例；(4) §3.2/§3.9/§9.5 补充 parseTimeout <= chatFallbackTimeout 层级约束与启动校验；(5) §1.5.3/§3.8 补充熔断器-滑动窗口依赖链说明；(6) §4.1 新增 DiscussionConclusionCapabilityExecutor 前置压缩调用的特化伪代码段。**v18 修订**：(1) §3.1 Phase4ServiceMetaProvider 接口归属从 ai-impl/thin-adapter/ 迁移至 ai-api/dto/base/，消除依赖方向矛盾；(2) §2.3 类图 doDegrade 方法签名补充 sentinelReason 参数；(3) §4.1 伪代码中 experimentAssignFailed 补充 boolean 类型声明；(4) 文档头部版本范围更正为 v7~v17；(5) §2.1 目录结构新增 Phase4ServiceMetaProvider.java；(6) AiCallRecord callTime 补充时间来源约定；(7) 补充热加载双机制协同规则。**v19 修订**：(1) Phase4ServiceMetaProvider 并发安全修复——改为响应 DTO 级内嵌 Phase4ServiceMeta 值对象；(2) DiscussionConclusion 前置压缩 clientType 改用固定轻量模型配置；(3) 补充 estimateTokens() 实现策略定义；(4) 薄适配器异常分类改用 Phase4BusinessException 公共基类 instanceof；(5) 补充 retryCount 覆盖不一致说明；(6) 验证 §2.3 类图 doDegrade 方法已存在且签名匹配vider.java；(6) §3.5 AiCallRecord 工厂方法补充 callTime 时间来源说明与时区策略；(7) §3.9 补充热加载双机制协同规则（定时刷新与事件驱动之间的优先级与冲突解决规则）。。

## 1. 概述

### 1.1 设计目标

Phase 5 包 G 交付 AI 进阶底座（AI Advanced Platform），为平台全部 AI 能力提供统一的运行时基础设施。设计目标如下：

- **能力迁移统一化**：将 Phase 2~4 各阶段独立接入的核心 AI 能力（3.4.1/3.4.2/3.4.3/3.4.10）以及 Phase 5 首次落地的能力（3.4.8/3.4.12/3.4.13）统一迁移至本底座，消除分散接入导致的重复代码和不一致的降级/超时/重试策略；Phase 4 已独立接入的 6 项 AI 能力（3.4.4/3.4.5/3.4.6/3.4.7/3.4.9/3.4.11）不迁移至底座，通过薄适配器集成到 AiOrchestrator 路由框架
- **模型对接标准化**：实现大模型统一对接层，支持多供应商模型路由与切换，业务层不感知具体模型实现
- **对话模板可配置化**：AI 对话模板（Prompt Template）按能力/科室维度可配置、可版本化管理，支持运行时热加载
- **A/B 实验可控化**：提供轻量级 A/B 实验框架，支持按能力维度分配流量到不同模型或 Prompt 版本，实验结果可观测
- **性能观测内建化**：为全部 AI 能力提供统合的调用指标采集、耗时分布、降级率统计与告警能力

### 1.2 整体架构思路

AI 进阶底座定位为 **ai-impl 子模块内部的分层架构**，在现有 `AiService` 接口不变的前提下，将原来 `MockAiService` 的扁平实现替换为多层管线：

```
业务模块 → AiService 接口（ai-api，接口签名冻结，新增 default method & 字段扩展见变更范围表）
               ↓
          FallbackAiService（装饰器，接口签名冻结，内部装配策略变更）
              ↓
         AiOrchestrator（编排层）
              ↓
          ┌───────────────────────────────┐
          │  AI 进阶底座                   │
          │  ├── ModelRouter              │  模型路由
          │  ├── PromptTemplateManager    │  Prompt 模板管理
          │  ├── ExperimentManager         │  A/B 实验
          │  ├── AiMetricsCollector       │  性能观测
          │  ├── LlmChatService           │  大模型对话客户端（含 chat + structuredChat）
          │  ├── LlmChatStreamService     │  大模型流式对话客户端（Flux）
          │  └── ModelEndpointHealthManager
          └───────────────────────────────┘
               ↓
          外部大模型服务（HTTP API / Spring AI ChatModel）
```

**设计风格一致性**：本设计参照 Phase0（AiService 降级策略体系）和 Phase1ABD（能力管线编排）的 OOD 设计成果，保持跨阶段设计规范和术语体系的一致性。具体规则如下：(1) §3 核心抽象格式使用"角色/职责/协作对象/类型形态选择理由"四元组，与 Phase0 §3.x 一致；(2) §4 使用缩进式伪代码（Python 风格缩进 + 中文注释），与 Phase1ABD §4.x 一致；(3) §7 使用"决策/选项/选择/理由"四列表格，与 Phase0 §5 一致；(4) 每个核心抽象末尾标注"为何使用 X 而非 Y"的决策理由，与 Phase0 §3.x 一致；(5) §5.1 使用"错误类别/代表场景/处理方式/响应形态"四列表格，与 Phase1ABD §5.1 一致。

### 1.3 核心抽象一览

| 抽象 | 类型形态 | 职责定位 |
|------|---------|---------|
| `AiService` | interface | AI 能力调用统一接口，包含 13 个方法。线程安全契约：实现需为线程安全（如 `AiOrchestrator` 的 Map 查找+委托模式）；`request` 参数约定为只读，调用方无需额外同步；返回值 `CompletableFuture<AiResult<T>>` 完成后 `AiResult` 为不可变对象 |
| `FallbackAiService` | class (decorator) | `AiService` 降级装饰器，标注 `@Primary` 确保业务模块优先注入。线程安全：装饰器本身无状态（`AiService delegate` 注入后不变）；降级判定全权委托 `CapabilityExecutor` 管线，不持有可变共享状态 |
| `AiOrchestrator` | class | 统一编排路由层，实现 `AiService` 接口全部 13 个方法，通过能力标识映射表查找并委托给对应的 `CapabilityExecutor` 实例；持有共享基础设施（`SlidingWindowMetricsStore`），不介入管线内部步骤 |
| `CapabilityExecutor<T, R>` | interface | 单项 AI 能力的泛型执行契约，定义 `execute()` 方法签名 |
| `AbstractCapabilityExecutor<T, R>` | abstract class | CapabilityExecutor 的抽象骨架实现，封装降级预检（前置至容器线程执行，不入线程池排队）、整体端到端超时（`CompletableFuture.orTimeout()`）、指标采集等公共模板方法，子类仅需特化管线差异化步骤 |
| `ModelRouter` | interface | 模型路由契约，根据能力标识与实验分组决定本次调用使用哪个模型配置 |
| `ModelRoute` | class | 模型路由条目值对象，封装模型标识、端点地址、权重等路由元数据 |
| `ClientType` | enum | 模型客户端类型枚举，`HTTP_API`（直连 HTTP 调用）/ `SPRING_AI`（Spring AI ChatModel 封装），用于 `ModelRoute` 字段和 `DelegatingLlmChatService` 分发决策。⚠️ **枚举值绑定约束与防护策略**：YAML 配置中 `client: "HTTP_API"` 字符串值通过 Spring Boot Relaxed Binding 绑定到 `ClientType` 枚举，绑定区分大小写且要求字符串值与枚举常量名完全一致。未匹配值时抛出 `ConversionFailedException`。防护策略采用**双重加固**：(1) 在 `ModelRoute` 的 `clientType` setter 中增加防御性字符串转换（如 `ClientType.valueOf(value.toUpperCase())`），同时注册 `@Converter(autoApply = true)` 提供宽松匹配；(2) 在 `DelegatingLlmChatService` 的 `@PostConstruct` 阶段检查 `delegates` Map 中是否所有已注册的 `ClientType` 枚举值均有对应实现，若某种实现缺失（如 YAML 配置了 `SPRING_AI` 但 Spring AI 依赖未引入），则在启动期发出 ERROR 日志而非仅 WARN，且推送告警事件到健康检查体系。若上述双重防护均被绕过且分发层最终回退到默认实现（`HttpApiLlmChatService`），回退时的日志级别提升为 **ERROR**（而非 WARN）并通过健康检查端点暴露告警状态，确保运维可感知配置错误 |
| `LlmChatService` | interface | 大模型对话客户端接口，包含 `chat(LlmChatRequest)`（同步对话）和 `structuredChat(LlmChatRequest, Class<T>)`（结构化输出）两个方法，屏蔽 HTTP API / Spring AI ChatModel 等底层差异 |
| `LlmChatStreamService` | interface | 大模型流式对话客户端接口，提供 `chatStream(LlmChatRequest) → Flux<LlmChatResponse>` 流式方法；因方法签名直接引用 `Flux`，reactor-core 为编译期强制依赖 |
| `DelegatingLlmChatService` | class | LlmChatService 分发层实现，按 `ModelRoute.clientType` 将 `chat()`/`structuredChat()` 调用派发至 `HttpApiLlmChatService` 或 `SpringAiLlmChatService`，消除双实现 Bean 装配二义性 |
| `HttpApiLlmChatService` | class | HTTP API 同步对话实现，仅实现 `LlmChatService`（chat + structuredChat），不持有 reactor-core 依赖 |
| `HttpApiLlmChatStreamService` | class | HTTP API 流式对话实现，仅实现 `LlmChatStreamService`（chatStream），持有 reactor-core 依赖 |
| `SpringAiLlmChatService` | class | Spring AI ChatModel 同步对话实现，仅实现 `LlmChatService`（chat + structuredChat），不持有 reactor-core 依赖 |
| `SpringAiLlmChatStreamService` | class | Spring AI ChatModel 流式对话实现，仅实现 `LlmChatStreamService`（chatStream），持有 reactor-core 依赖 |
| `LlmChatRequest` | class | 大模型对话请求值对象，包含 `messages: List<LlmChatMessage>`（多轮消息列表）和 `options: LlmChatOptions`（模型参数配置） |
| `LlmChatMessage` | class | 对话消息值对象，封装消息角色（`LlmChatMessageRole`）与文本内容 |
| `LlmChatMessageRole` | enum | 对话消息角色枚举，含 `SYSTEM` / `USER` / `ASSISTANT` 三个常量 |
| `LlmChatOptions` | class | 对话参数配置值对象，封装 `modelId`、`temperature`、`maxTokens`、`stopSequences` 等模型参数，提供编译期类型安全替代 `Map<String, Object>` |
| `LlmChatResponse` | class | 大模型对话响应值对象，携带助手消息文本、Token 用量统计（内嵌 `LlmChatUsage` 静态类）、模型标识与 LLM 实现层内部重试计数（`retryCount`） |
| `LlmChatUsage` | static class | Token 用量静态内嵌类，封装 promptTokens/completionTokens/totalTokens 三个字段，作为 `LlmChatResponse` 的内嵌组件，不独立使用 |
| `PromptTemplateManager` | interface | Prompt 模板管理契约，支持按能力/科室/版本检索与渲染模板 |
| `PromptTemplate` | class | Prompt 模板值对象，含模板标识、能力标识、科室标识、模板内容、版本号与启用状态 |
| `ExperimentManager` | interface | A/B 实验管理契约，根据能力标识与上下文决定实验分组 |
| `ExperimentAssignment` | class | 实验分组结果值对象，含实验标识、分组标识与目标模型/Prompt版本 |
| `AiMetricsCollector` | interface | AI 性能指标采集契约，记录每次调用的能力标识、耗时、是否降级、Token 用量等 |
| `AiCallRecord` | class | 单次 AI 调用记录值对象，作为 `AiMetricsCollector` 的入参与 `AI 调用日志`（5.2）持久化的数据源 |
| `AiCallLogEntity` | JPA @Entity | AI 调用日志 JPA 实体，与 `AiCallRecord` 字段对等 |
| `CallContext` | class (value object) | （Phase 5 第二阶段重构目标）业务上下文聚合值对象（归属 `ai-api/dto/base/`），聚合 `departmentId/callerRole/callerId/visitId/patientId/sessionId/inputSummary/outputSummary/promptVersion` 共 9 个业务上下文字段，作为 `doDegrade()`、`doExecuteInternal()` 和 `AiCallRecord` 工厂方法的统一参数；不可变对象（所有字段 `final`），通过全参构造器或 Builder 赋值，提供 `withOutputSummary()`/`withPromptVersion()` 复制方法 |
| `StructuredOutputParser` | interface | 结构化输出解析契约，将 LLM 原始文本输出解析为各能力对应的 Java DTO |
| `DegradationStrategy` | interface | 降级策略契约（Phase 0 已定义，本阶段扩展 `DegradationContext` 字段） |
| `DegradationContext` | class | 降级判定上下文（Phase 0 已定义骨架，本阶段扩展字段，含 Builder 模式） |
| `LocalRuleFallback` | interface | 本地规则降级契约，为 3.4.2 处方审核等需要本地规则兜底的能力提供降级执行入口 |
| `SlidingWindowMetricsStore` | class | 每个能力标识的调用指标滑动窗口存储，为降级策略提供数据源 |
| `ModelEndpointHealthManager` | class | 模型端点健康状态管理器，维护每个端点的 CONNECTED/DEGRADED/UNAVAILABLE 状态与探测触发逻辑 |
| `CredentialProvider` | interface | 凭据查询接口，按 endpointId 从 Vault/配置中心查询认证凭据，返回含 API Key/OAuth2 Token 的 Credential 值对象 |
| `EndpointRateLimiter` | class | 端点限流器，基于 Guava 令牌桶算法按 endpointId 维度独立限流，在 LlmChatService 调用入口处执行速率检查 |
| `ChatToolDefinition` | class | Tool 定义值对象，封装 tool_use/function_call 的名称、描述与 JSON Schema 参数定义，作为 `LlmChatRequest.tools` 字段的列表元素 |
| `StructuredChatResult<T>` | class | 结构化调用结果值对象，包裹解析后的 DTO、LLM 重试计数与 Token 用量，使 structuredChat 成功路径也能携带调用元数据 |
| `StructuredOutputNotSupportedException` | class (extends RuntimeException) | 结构化输出格式不支持异常，语义为"模型不支持 JSON mode / tool_use，回退到 chat() + StructuredOutputParser.parse()"；由 LlmChatService 实现层在模型不支持结构化输出时抛出，在 CapabilityExecutor doExecuteInternal() 中被捕获并触发回退路径 |
| `LlmInfrastructureException` | class (extends RuntimeException) | LLM 基础设施异常，语义为"HTTP 5xx、连接超时、网络抖动等基础设施故障，直接降级不尝试 chat() 回退"；与 StructuredOutputNotSupportedException 区分，两个独立 catch 分支处理 |
| `AiPlatformEnvironmentPostProcessor` | class | 配置转发前置处理器，在 Spring 启动早期将 `ai.platform.enabled` 反向转发至 `ai.mock.enabled`，与 `AiPlatformConfig` 生命周期分离 |

### 1.4 底座切流初期已知限制

底座切流初期（Phase 4 DTO 补齐之前），以下 6 项薄适配器能力（3.4.4 / 3.4.5 / 3.4.6 / 3.4.7 / 3.4.9 / 3.4.11）因对应的 Phase 4 请求 DTO 为空类，其 `doExecuteInternal()` 将跳过委托调用直接降级。具体受限能力及依赖条件如下表：

| 能力标识 | 能力名称 | 依赖状态 | 预期补齐时间 |
|---------|---------|---------|------------|
| DIAGNOSIS | AI 智能诊断 | 🟡 依赖 Phase 4 DTO 改造 | Phase 4 DTO 改造完成后 |
| ANALYSIS_REPORT_INSPECTION | AI 智能检查报告 | 🟡 依赖 Phase 4 DTO 改造 | Phase 4 DTO 改造完成后 |
| ANALYSIS_REPORT_LABTEST | AI 智能检验报告 | 🟡 依赖 Phase 4 DTO 改造 | Phase 4 DTO 改造完成后 |
| IMAGE_ANALYSIS | AI 影像分析 | 🟡 依赖 Phase 4 DTO 改造 | Phase 4 DTO 改造完成后 |
| RECOMMEND_EXAM | AI 开立检查检验 | 🟡 依赖 Phase 4 DTO 改造 | Phase 4 DTO 改造完成后 |
| RECOMMEND_EXEC_ORDER | AI 执行顺序推荐 | 🟡 依赖 Phase 4 DTO 改造 | Phase 4 DTO 改造完成后 |

**已可用能力**：7 项底座能力（TRIAGE / RX_AUDIT / MEDICAL_RECORD_GEN / RX_ASSIST / KB_QUERY / SCHEDULE / DISCUSSION_CONCLUSION）在底座切流初期即完整可用，不受 Phase 4 DTO 状态影响。

**不可用时的行为**：不可用的薄适配器收到调用时，直接走降级路径，降级原因记录为 `DegradationReason.INFRASTRUCTURE_ERROR + ":Phase4DtoEmpty"`，详见 §3.1「Phase 4 服务接口契约定义」段。

#### 1.4.1 薄适配器性能观测维度缺口

6 项薄适配器能力在底座切流初期存在系统性性能观测维度缺口，影响「性能观测内建化」目标的达成：

| 缺失维度 | 影响范围 | 根因 | 预期补齐方式 |
|---------|---------|------|------------|
| `modelId` | 6 项薄适配器成功/降级路径均传入 null（见 §4.2 行 3116 和 §4.2 降级路径） | 薄适配器不经过底座模型路由，Phase 4 服务内部自行选择模型且当前响应 DTO 不暴露模型标识 | Phase 4 响应 DTO 内嵌 `Phase4ServiceMeta` 元数据值对象，薄适配器通过 `result instanceof Phase4ServiceMetaCapable` 提取 `getServiceMeta().getModelId()`；补齐前 AiCallRecord.modelId=null |
| `promptVersion` | 6 项薄适配器所有路径传入 null（成功/降级/超时均无实验分流） | 薄适配器不经过底座实验分流，Phase 4 服务内部无 Prompt 版本概念 | Phase 4 响应 DTO 内嵌 `Phase4ServiceMeta` 元数据值对象，薄适配器通过 `result instanceof Phase4ServiceMetaCapable` 提取 `getServiceMeta().getPromptVersion()`；补齐前 AiCallRecord.promptVersion=null |
| `retryCount` | 6 项薄适配器成功路径硬编码为 0 | 底座无法感知 Phase 4 服务内部重试次数 | Phase 4 响应 DTO 内嵌 `Phase4ServiceMeta` 元数据值对象，薄适配器通过 `result instanceof Phase4ServiceMetaCapable` 提取 `getServiceMeta().getRetryCount()`；补齐前 AiCallRecord.retryCount=0 |

**影响分析**：以上三个维度同时缺失，意味着所有薄适配器能力（占全部 AI 能力的 46%）的性能分析无法按模型、Prompt 版本、重试次数三个关键维度聚合，A/B 实验效果分析和成本归因均出现系统性缺口。此缺口在 §1.3「核心抽象一览」中 `AiCallRecord` 字段定义处和 §3.5「AiCallRecord」字段填充策略中以标注说明。

**补齐时间线**：薄适配器元数据补齐不阻塞底座切流，作为 Phase 4 DTO 改造（§3.1「Phase 4 DTO 改造最小验收标准」）的可选扩展项，在跨包协作会议上确认。元数据补齐前，底座侧在 AiCallRecord 字段中传入 null/0，并在指标报表中按 "modelId=null" 和 "promptVersion=null" 为独立聚合维度，使运维人员可区分缺失数据与真实数据。

#### 1.4.2 薄适配器 DTO 改造跨包依赖单一视图

6 项薄适配器 DTO 改造涉及跨包协作（底座侧 + Phase 4 模块侧），相关信息分散在 §3.1（Phase 4 DTO 改造最小验收标准）、§3.5（DTO 改造工作量概览表）、§9.1（迁移路径表）三处。为消除实施者交叉引用负担，以下提供集中视图：

| 维度 | 详情 | 归属章节 |
|------|------|---------|
| **改造归属** | 7 项底座能力 DTO 改造（~1,130 行）由底座侧承担；6 项薄适配器 DTO 改造（~390 行）归属 Phase 4 模块，底座仅提供改造模板和验收测试 | §3.5 DTO 改造工作量概览表 |
| **底座侧临时 fallback 方案** | Phase 4 模块尚未完成 DTO 改造时，薄适配器 `doExecuteInternal()` 在非 HTTP 场景下跳过委托调用直接降级，降级原因 `DegradationReason.INFRASTRUCTURE_ERROR + ":Phase4DtoEmpty"` | §3.1「Phase 4 服务接口契约定义」 |
| **Phase 4 DTO 改造最小验收标准** | (1) 每个 Phase 4 能力至少新增一个业务字段；(2) 新增字段标注 `@JsonProperty`；(3) 通过底座 4 个 null-safe 提取方法验证；(4) 验收测试通过 | §3.1「跨包协作会议」 |
| **跨包协作会议最晚截止时间** | 底座切流前 2 周（Phase 5 底座 P0 Batch 1~3 集成测试完成后） | §3.1「跨包协作会议的时间线与验收标准」 |
| **底座侧治理规则** | 薄适配器仅可引用 Phase 4 模块的 Service 层接口，不得引用 Entity/Repository/Controller | §10.3 |
| **迁移状态标记** | 6 项薄适配器在 §9.1 迁移路径表中标注 🟡 依赖 Phase 4 DTO 改造 | §9.1 |

### 1.5 多实例行为约束与部署形态

#### 1.5.1 部署形态前提

Phase 5 底座当前设计基于**单体部署**（单 JVM 实例），管理端与底座同进程。若未来引入多实例部署（水平扩展），以下三个组件的跨实例行为需重新审视。

#### 1.5.2 逐组件跨实例行为分析

| 组件 | 当前设计（单实例） | 多实例行为 | 约束/建议 |
|------|------------------|-----------|----------|
| `SlidingWindowMetricsStore` | 基于进程内 `ConcurrentHashMap`+`Deque` 维护滑动窗口，记录本实例的调用事件 | 每个实例各自维护独立的滑动窗口，各实例看到的失败率/耗时分布**不一致**。降级策略（熔断器/超时）的判定基准不同 | 若多实例部署且要求全局一致的降级判定，需替换为分布式滑动窗口（Redis Sorted Set + 时间窗口查询）。Phase 5 暂时接受 Per-instance 行为差异，通过统一配置保证各实例参数一致 |
| `EndpointRateLimiter` | 基于 Guava `RateLimiter` 的 Per-instance 令牌桶，按 endpointId 独立限流 | 每个实例各自维护限流器，N 个实例实际向供应商端点发送 N 倍许可数的请求 | 多实例下 Per-instance 限流仅提供**尽力而为**的约束。若供应商实施严格的全局速率限制，需引入分布式限流（Redis Lua 脚本 + 令牌桶） |
| `CircuitBreakerDegradationStrategy` | 基于进程内 `AtomicReference<CircuitState>` 维护熔断状态，HALF_OPEN 探测调用仅单实例触发 | 每个实例各自维护熔断状态，实例 A 已OPEN但实例 B 仍CLOSED（因各自的失败率窗口不同），导致同一端点可能承受来自部分实例的持续调用 | **最大风险组件**。建议在引入多实例部署时优先将熔断器升级为分布式实现（Redis 共享熔断状态），或在网关层引入集中式熔断 |

#### 1.5.3 迁移路径

多实例部署的分布式重构优先级：**CircuitBreakerDegradationStrategy（最高） > EndpointRateLimiter（重要） > SlidingWindowMetricsStore（可选）**。Phase 5 维持单体架构，上述约束记录在案，待 Phase 6 引入水平扩展时触发重构。

**注意依赖链**：`CircuitBreakerDegradationStrategy` 依赖 `SlidingWindowMetricsStore.getFailureRate()` 提供实时失败率数据以判定熔断状态。若 `SlidingWindowMetricsStore` 升级为分布式实现，`CircuitBreakerDegradationStrategy` 必须同步适配新的读取接口。反之，若单实例阶段已引入分布式熔断器（共享 Redis 状态），`SlidingWindowMetricsStore` 仍可维持 Per-instance 窗口——熔断器的 OPEN/CLOSED/HALF_OPEN 状态由全局事故率驱动，`SlidingWindowMetricsStore` 仅用于本地参考指标。此依赖链在 §3.8 中以协作对象显式记录。

#### 1.5.4 配置生效时间窗口差异约束分析

多实例部署下，底座热加载配置（§3.9「运行时配置热加载机制」）的生效时间在实例间存在差异，以下逐项分析影响：

| 热加载配置项 | 刷新方式 | 单实例生效延迟 | 多实例生效时间不一致窗口 | 对运维操作的影响 |
|------------|---------|-------------|---------------------|---------------|
| `execution.timeout.per-capability` | `@Scheduled(fixedDelay=60000)` 定时轮询 | 最长 60 秒 | N 个实例的 `@Scheduled` 定时器独立触发，时间窗口差异可能导致实例 A 已应用新超时时实例 B 仍使用旧值，最长接近一个轮询周期（60 秒） | **超时阈值调整**：将某项能力的超时从 60s 调整为 90s 时，实例 A 的 `orTimeout()` 已使用 90s 而实例 B 仍使用 60s——实例 B 上的请求可能在 60s 被降级而 A 正常完成。对端到端降级率指标产生约数分钟的过渡期扰动。**降级策略切换**：将某能力的策略白名单从 `[timeout]` 改为 `[timeout, circuit-breaker]` 时，实例 A 已启用熔断判定而实例 B 未启用，同能力在不同实例的降级行为不一致 |
| `degradation.strategies` | `@Scheduled(fixedDelay=60000)` 定时轮询 + `AtomicReference` 全量替换 | 最长 60 秒 | 同上，各实例独立性导致熔断器开关生效时间不一致 | **熔断器全局性**：全局熔断策略依赖于全局一致的失败率视角。Per-instance 的熔断状态差距加上配置生效时间不一致，极端场景下系统可能出现"部分实例熔断、部分实例正常"的长尾状态 |
| `sliding-window.window-seconds` | `@Scheduled(fixedDelay=60000)` 定时轮询 + `AtomicLong` | 最长 60 秒 | 同上述轮询机制 | `SlidingWindowMetricsStore` 窗口宽度不一致影响失败率计算的精确度，但各实例各自维护独立的窗口数据，窗口宽度差异不跨实例传播 |

**约束结论**：

1. **可接受的过渡期扰动**：上述配置生效时间不一致导致的过渡期扰动（最长 ~60 秒）在当前运维规程中是可接受的。底座不为此引入分布式协调机制（如分布式锁或配置中心 Watch 机制）。
2. **运维操作建议**：(a) 安全敏感配置（如 `degradation.strategies` 中启用熔断器、`execution.timeout.per-capability` 大幅缩短）建议在低峰期变更，并在变更后观察 2~3 个轮询周期（2~3 分钟）确认所有实例收敛后再进行验证测试；(b) 紧急配置变更（如因端点故障需立即启用熔断器）若无法接受 60 秒生效窗口，可通过 `/actuator/refresh` 手动触发全量实例刷新（需引入 `spring-boot-starter-actuator` + `spring-cloud-starter`），或扩缩容实例以绕过轮询等待。
3. **未来改进**：若多实例部署规模扩大（≥10 节点）且运维发现 60 秒生效窗口导致的过渡期扰动不可接受，可将 `@Scheduled(fixedDelay=60000)` 缩短为 `fixedDelay=30000`（代价为配置源读压力翻倍），或引入配置中心（Nacos/Apollo）的 Watch 机制实现实时推送。Phase 5 暂不引入。

### 1.6 API Surface 状态表

以下按接口分类标注设计文档伪代码中引用的关键 API 的当前实施状态，三种状态为：**✓ 已有实现**（代码库中存在，无需新建）、**△ 已有骨架**（接口部分声明或空实现，需补充方法体）、**⊕ 需新建**（代码库中不存在，需从零创建）。

#### 1.6.1 ai-api 模块（依赖方为零影响）

| API | 归属包 | 状态 | 说明 |
|-----|--------|------|------|
| `AiService`（13 方法） | `ai-api` | ✓ | 接口签名冻结，Phase 2~4 已有完整声明和业务模块引用 |
| `AiResult<T>` | `ai-api` | ✓ | 已有完整定义 |
| `DegradationStrategy` | `ai-api/degradation/` | △ | 已有接口，需新增 `default int getOrder()`（二进制兼容） |
| `DegradationContext` | `ai-api/degradation/` | △ | 已有骨架（零值构造器 + `elapsedTime`/`requestType` 字段），需扩展字段 |
| `DegradationReason` | `ai-api/degradation/` | ⊕ | 需新增枚举 |

#### 1.6.2 ai-impl 模块新建接口（按实施优先级排序）

| 优先级 | API | 归属包 | 状态 | 依赖前提 |
|-------|------|--------|------|---------|
| P0（核心管线依赖） | `SlidingWindowMetricsStore` | `metrics/` | ⊕ | 独立组件，无前置依赖 |
| P0 | `AbstractCapabilityExecutor<T,R>` | `orchestrator/` | ⊕ | 依赖 `SlidingWindowMetricsStore` |
| P0 | 7 项底座 `XxxCapabilityExecutor` | `orchestrator/impl/` | ⊕ | 依赖 `AbstractCapabilityExecutor` |
| P0 | 6 项薄适配器 `XxxCapabilityExecutor` | `thin-adapter/` | ⊕ | 依赖 `AbstractCapabilityExecutor` + Phase 4 Service 接口（已有） |
| P1（LLM 调用层） | `LlmChatService` + `DelegatingLlmChatService` | `client/` | ⊕ | 依赖 `CredentialProvider`、`EndpointRateLimiter` |
| P1 | `LlmChatStreamService` | `client/` | ⊕ | reactor-core 编译期强制依赖 |
| P1 | `HttpApiLlmChatService` | `client/` | ⊕ | 依赖 `CredentialProvider` |
| P2（基础服务） | `ModelRouter` + `DefaultModelRouter` | `router/` | ⊕ | 依赖 `AiRouterProperties` YAML 配置 |
| P2 | `PromptTemplateManager` + `DatabasePromptTemplateManager` | `template/` | ⊕ | 依赖 JPA + Caffeine |
| P2 | `ExperimentManager` + `HashBucketExperimentManager` | `experiment/` | ⊕ | 依赖 JPA + Caffeine |
| P2 | `AiMetricsCollector` + `LoggingMetricsCollector` | `metrics/` | ⊕ | 依赖 `@Async` 线程池 + JPA |
| P2 | `ModelEndpointHealthManager` | `metrics/` | ⊕ | 独立组件 |
| P2 | `EndpointRateLimiter` | `client/` | ⊕ | 依赖 Guava |
| P3（可延迟） | `StructuredOutputParser` + `JsonStructuredOutputParser` | `parser/` | ⊕ | 依赖 Jackson |
| P3 | `LocalRuleFallback` + `PrescriptionLocalRuleFallback` | `fallback/` | ⊕ | 仅处方审核能力使用 |
| P3 | `AiPlatformConfig` + `AiPlatformEnvironmentPostProcessor` | `config/` | ⊕ | 依赖所有上述组件完成后的配置装配 |
| P3 | `RequestContextUtils` | `orchestrator/` | ⊕ | 纯静态工具类 |

**优先级判定规则**：P0 = 核心管线执行路径必须；P1 = LLM 调用与模型交互路径必须；P2 = 基础服务与可观测性；P3 = 解析/配置/工具类（可在初期使用简化实现替代）。

### 1.7 实施拓扑顺序

建议按以下批次顺序实施，每批次基于前序批次构建，批次内组件可并行开发：

| 批次 | 实施组件 | 依赖前提 | 验收标准 |
|------|---------|---------|---------|
| 批次 1（核心骨架） | `SlidingWindowMetricsStore` → `DegradationStrategy` 骨架 → `AbstractCapabilityExecutor` | 无 | 单元测试覆盖 `recordSuccess/Degraded/Failure` 三分类、`getFailureRate/getEffectiveFailureRate`、惰性淘汰并发安全 |
| 批次 2（底座 7 项能力管线） | 7 项 `XxxCapabilityExecutor`（使用 `MockLlmChatService`） | 批次 1 | 完整管线集成测试：模板渲染→实验分流→模型路由→Mock LLM→指标采集回路通过 |
| 批次 3（LLM 调用层） | `LlmChatService` + 相关 DTO → `DelegatingLlmChatService` → `HttpApiLlmChatService` | 批次 2 | 真实 HTTP API 调用 LLM 集成测试通过 |
| 批次 4（基础服务） | `PromptTemplateManager`、`ModelRouter`、`ExperimentManager`、`AiMetricsCollector`、`ModelEndpointHealthManager`、`EndpointRateLimiter` | 批次 2 | 各组件独立集成测试通过；`AiPlatformConfig` Bean 装配验证通过 |
| 批次 5（薄适配器） | 6 项薄适配器 `XxxCapabilityExecutor` | 批次 1 + Phase 4 服务 | 薄适配器委托调用+超时降级路径集成测试通过 |
| 批次 6（配置装配） | `AiPlatformConfig` + `AiPlatformEnvironmentPostProcessor`、`FallbackAiService` 构造器迁移 | 批次 3 + 批次 4 | `ai.platform.enabled=true/false` 互斥验证、`EnvironmentPostProcessor` 转发验证通过 |
| 批次 7（可延迟） | `StructuredOutputParser` + `JsonStructuredOutputParser`、`LocalRuleFallback` + `PrescriptionLocalRuleFallback` | 批次 3 | 结构化输出解析+本地规则降级路径测试通过 |

### 1.8 非功能性质量分析

#### 1.8.1 多级缓存冷启动惊群效应与预热策略

底座共引入以下三级 Caffeine 缓存：

| 缓存位置 | 缓存内容 | 最大条目数 | 默认 TTL | 加载方式 |
|---------|---------|-----------|---------|---------|
| `DatabasePromptTemplateManager` | 按 (capabilityId, departmentId, promptVersion) 键的 PromptTemplate | 1000 | 5 分钟 | 惰性——首次 `render()` 触发 DB 查询 |
| `HashBucketExperimentManager` | 按 capabilityId 键的 Experiment 配置列表 | 100（13 项能力上限） | 5 分钟 | 惰性——首次 `assign()` 触发 JPA 查询 |
| `CredentialProvider` | 按 endpointId 键的 Credential（API Key / Token） | 1000 | 5 分钟（OAUTH2 按 expiresAt 动态） | 惰性——首次 `getCredential()` 触发 Vault 查询 |

**冷启动惊群效应**：底座首次启动时（或所有缓存同时过期时），若首个 AI 调用命中某项能力，该能力的 `doExecuteInternal()` 管线将连续触发以下级联延迟：`ExperimentManager.assign()`（JPA 查询 5~20ms）→ `PromptTemplateManager.render()`（JPA 查询 5~20ms）→ `ModelRouter.route()`（内存路由表无缓存延迟）→ `CredentialProvider.getCredential()`（Vault 查询 20~100ms）→ `LlmChatService.chat()`（LLM 调用 500~5000ms）。冷启动时首个请求的端到端延迟比稳态值增加约 30~140ms（DB+Vault 查询叠加），P99 退化幅度约 5%~15%。

13 项能力同时首次触发的极端场景（如底座重启后首个 HTTP 请求同时查询多项能力）：JPA Repository 层面 3 个表（`prompt_template` / `experiment` / `experiment_group`）的查询集中在同一时间窗口，数据库连接池（默认 HikariCP 10 连接）可能短暂耗尽（3 表 × 13 能力 = 39 个并发查询），触发连接排队，P99 进一步退化 50~200ms。

**预热建议**：在 `AiPlatformConfig` 的 `@PostConstruct` 阶段（或其他 `@PostConstruct` Bean 中）执行以下预热步骤：
1. **PromptTemplate 预热**：在 `DatabasePromptTemplateManager` 初始化后（`@PostConstruct`），主动查询全部 7 项底座能力 + 6 项薄适配器能力的 ACTIVE 模板（`SELECT * FROM prompt_template WHERE status=ACTIVE`），预填充 Caffeine 缓存，消除首次 `render()` 的 DB 查询延迟
2. **Experiment 预热**：在 `HashBucketExperimentManager` 初始化后，主动查询全部 7 项底座能力的 ACTIVE 实验配置（含 `ExperimentGroup`），预填充缓存
3. **路由表预热**：`DefaultModelRouter` 在 `@PostConstruct` 中从 YAML 配置源加载路由表后即完成预热（路由表不依赖 DB 惰性加载），无需额外操作
4. **凭据缓存预热**：`CredentialProvider` 不在启动期预热凭据缓存（避免无业务流量时频繁 Vault 查询），首个请求的 Vault 查询延迟 20~100ms 在可接受范围内

预热完成后，底座首个 AI 调用仅承担 LLM HTTP 调用的网络延迟（500~5000ms），缓存查询延迟降至 <1ms。

**冷启动与每小时热数据过期后的惊群效应对比**：Caffeine `expireAfterWrite=5min` 的 TTL 窗口导致缓存在底座运行后约 5 分钟时集中过期（启动预热的缓存条目在同一时间窗口到期），引发每小时约 12 次的"微型冷启动"——所有缓存在数秒间隔内先后过期，JPA 查询集中在同一时间窗口。缓解措施：(1) 为各缓存引入 `initDelay` 随机 offset（Caffeine 默认无此能力，可在预热触发时注入 jitter 组件）；(2) 或通过 `Caffeine.expireAfterWrite(5min, 1min)` 的随机化 TTL（Caffeine 仅支持固定 TTL，随机化需在写入时手动计算 `Duration.ofSeconds(300 + ThreadLocalRandom.current().nextInt(60))`——各缓存条目写入时加上随机偏移，使过期时间交错分散，避免集中过期风暴）。

#### 1.8.2 新增 JPA Repository 对数据库连接池的压力估算

底座新增 3 个 JPA Repository：

| Repository | Entity | 写入操作 | 读取操作 | 预期 QPS |
|-----------|--------|---------|---------|---------|
| `PromptTemplateRepository` | `PromptTemplate` | 管理端维护（写 QPS < 0.01） | 首次 `render()` 触发 + 缓存失效后重新加载（读 QPS ≈ 能力调用 QPS × 冷启动率） | 读 QPS < 1（缓存命中时无 DB 读取） |
| `ExperimentRepository` | `Experiment` + `ExperimentGroup` | 管理端维护（写 QPS < 0.01） | 首次 `assign()` 触发 + 缓存失效后重新加载（读 QPS ≈ 能力调用 QPS × 冷启动率） | 读 QPS < 1（缓存命中时无 DB 读取） |
| `AiCallLogRepository` | `AiCallLogEntity` | 每次 AI 调用完成后异步写入（写 QPS = 总 AI 调用 QPS） | 管理端统计分析 + 报表查询（读 QPS < 1） | 写 QPS = 总 AI 调用 QPS |
| `AiCallLogStatsRepository` | `AiCallLogStats` | 每日凌晨分区清理时写入（写 QPS < 0.01） | 管理端月度统计报表（读 QPS < 0.1） | 写 QPS < 0.01 |

**连接池压力估算**：
- HikariCP 默认最大连接数 = 10（`spring.datasource.hikari.maximum-pool-size=10`），底座新增的 3 个 Repository 中，**`AiCallLogRepository`** 是唯一可能产生显著并发写压力的 Repository（写 QPS 与总 AI 调用 QPS 正相关）
- 典型场景：若总 AI 调用 QPS = 20（13 项能力平均分布），`AiCallLogRepository` 的异步写入线程池（`MetricsAsyncExecutor`: 核心 1 线程 / 最大 2 线程 / 队列 1000）将写操作串行化到 1~2 个连接上，不会耗尽连接池。连接池的 10 个连接中，业务主流程（HTTP API / JPA 业务查询）占用约 4~6 个，底座新增操作占用约 1~2 个（`AiCallLogRepository` 异步写入），预留 2~5 个空闲连接
- **压力阈值**：若底座 AI 调用 QPS 超过 200（13 项能力合计），`AiCallLogRepository` 的异步写入队列（1000）可能填满，触发 `DiscardPolicy`（指标记录丢弃）。此时不会耗尽连接池——`DiscardPolicy` 阻止了更多写入任务进入线程池，从而限制了同时占用的 JDBC 连接数不超过 `metricsAsyncExecutor` 的最大线程数（=2）。连接池层面无需扩容
- **扩容建议**：若 AI 调用 QPS 持续 > 500，建议：(1) 将 `AiCallLogRepository` 的写入改为批量 INSERT（每 100 条或每秒合并写入一次），减少 JDBC 连接占用时间；(2) 或引入独立的消息队列（Kafka / RabbitMQ）替代 JPA 直接写入，消除连接池依赖；(3) 将 HikariCP `maximum-pool-size` 从 10 提升至 20

#### 1.8.3 滑动窗口内存占用估算

每个能力标识在 `SlidingWindowMetricsStore` 中维护一个 `Deque<WindowedEvent>`，`WindowedEvent` 结构如下：
- `type: EventType`（enum，1 字节，但 JVM 对象头 + 引用约 24 字节）
- `timestamp: long`（8 字节）
- `elapsedMs: long`（8 字节）
- JVM 对象头 + 填充 ≈ 24 字节（32 位压缩指针下约 16 字节）
- 合计 ≈ 48 字节/事件（压缩指针）~ 64 字节（非压缩）

13 项能力满载（`windowSeconds=60s`，`max-events-per-capability=10000`）：
- 每能力最多 10,000 事件 × 48 字节 = 480 KB
- 13 项能力合计：480 KB × 13 = 6,240 KB ≈ **6.1 MB**
- 典型负载（60 秒窗口内每能力平均 100 个事件）：100 × 48 字节 × 13 = 62.4 KB

**结论**：即使 13 项能力全部满载（每能力 166 events/s），滑动窗口总内存占用不超过 **6.5 MB**，对 JVM Heap（通常 ~512MB~2GB）几乎无影响。无需特殊优化。

**`ConcurrentHashMap` 额外开销**：
- `SlidingWindowMetricsStore` 内部 `ConcurrentHashMap<String, Deque<WindowedEvent>>` 的 Entry 开销约 64 字节/键值对
- 13 项能力 × 64 字节 = 832 字节（可忽略）
- 每个 `Deque<WindowedEvent>` 的 LinkedList Node 开销约 24 字节 × 10,000 × 13 = 3.12 MB
- **滑动窗口总内存占用合计（含 ConcurrentHashMap + LinkedList Node 开销）**：约 **9.2 MB**

#### 1.8.4 @PostConstruct 扫描和策略 Map 构建对启动时间的影响

底座在 `AiPlatformConfig` 中有以下 `@PostConstruct` / `@Bean` 初始化操作：

| 操作 | 触发方式 | 预期耗时 | 依赖外部系统 | 说明 |
|------|---------|---------|------------|------|
| 降级策略 Map 构建 | `AiPlatformConfig @PostConstruct` | < 10ms | 否（策略 Bean 已在容器中，仅为 Map 构建） | 从 ApplicationContext 获取策略 Bean，按 YAML 配置构建 `Map<String, List<DegradationStrategy>>` |
| PromptTemplate 预热 | `DatabasePromptTemplateManager @PostConstruct`（推荐新增） | 20~50ms | 是（JPA 查询） | 查询全部 ACTIVE 模板，预填充 Caffeine 缓存 |
| Experiment 预热 | `HashBucketExperimentManager @PostConstruct`（推荐新增） | 10~30ms | 是（JPA 查询） | 查询全部 ACTIVE 实验配置，预填充 Caffeine 缓存 |
| 路由表加载 | `DefaultModelRouter @PostConstruct` | < 5ms | 否（从 YAML 配置源加载） | 读取本地配置，构建不可变路由 Map |

**总启动延迟增量**：底座初始化对 Spring Application 启动总时间的增量约为 **30~90ms**（不含 JPA 预热查询）。预热查询依赖数据库可用性——若数据库在底座启动时尚未就绪（如多模块并行启动场景），预热查询失败不应阻塞容器启动，预热步骤需包裹 try-catch（WARN 日志 + 跳过预热，回退到惰性加载）。两种预热查询均发生在各自的 Bean 容器生命周期回调中，相互独立，不产生级联延迟。

**连接池就绪等待**：预热查询（JPA）需在 HikariCP 连接池初始化完成后才能执行。若 HikariCP `spring.datasource.hikari.initialization-fail-timeout=-1`（默认 -1 即无限等待），预热查询将自旋等待连接池就绪，此时底座启动被阻塞在 Bean 初始化阶段。**建议配置**：HikariCP `initialization-fail-timeout=1000`（最多等待 1 秒），预热查询 try-catch 捕获 `SQLException` 时 WARN 日志 + 跳过预热。

**CapabilityExecutor 扫描开销**：`AiOrchestrator` 的 `@PostConstruct` 阶段通过 `List<CapabilityExecutor>` 自动注入 + `getCapabilityId()` 构建 `Map<String, CapabilityExecutor>`。13 个 CapabilityExecutor Bean 的注入和 Map 构建总耗时 < 5ms（无外部依赖，纯内存操作）。

---

## 2. 模块划分

### 2.1 目录结构

```
backend/modules/ai/
├── ai-api/ src/main/java/com/aimedical/modules/ai/api/
│   ├── AiService.java                      # 接口签名冻结
│   ├── AiResult.java                       # 接口签名冻结
│   ├── degradation/
│   │   ├── DegradationStrategy.java         # 接口新增 default method（getOrder()，二进制兼容）
│   │   ├── DegradationContext.java          # 扩展字段（保持二进制兼容）
│   │   └── DegradationReason.java           # 新增枚举（降级原因/错误码）
│   └── dto/
│       └── base/
│           ├── AiRequestBase.java           # 新增抽象基类
│           ├── CallContext.java             # （Phase 5 第二阶段重构目标）新增业务上下文聚合值对象（departmentId/callerRole/callerId/visitId/patientId/sessionId/inputSummary/outputSummary/promptVersion）
│           ├── Phase4ServiceMeta.java         # 新增元数据值对象（modelId/promptVersion/retryCount），响应 DTO 内嵌后薄适配器通过 instanceof 提取
│           ├── Phase4ServiceMetaCapable.java  # 新增可选接口，响应 DTO 实现后薄适配器可通过 instanceof 获取内嵌的 Phase4ServiceMeta
│           └── Phase4BusinessException.java   # 新增抽象基类，Phase 4 模块业务异常统一继承此类，薄适配器通过 instanceof 识别业务异常
│
├── ai-impl/ src/main/java/com/aimedical/modules/ai/impl/
│   ├── orchestrator/
│   │   ├── AiOrchestrator.java             # 统一编排层，实现 AiService
│   │   ├── CapabilityExecutor.java         # 能力执行器泛型接口
│   │   ├── AbstractCapabilityExecutor.java # 能力执行器抽象骨架（封装降级预检、指标采集等公共模板方法）
│   │   └── impl/
│   │   ├── TriageCapabilityExecutor.java
│   │   ├── PrescriptionCheckCapabilityExecutor.java
│   │   ├── MedicalRecordGenCapabilityExecutor.java
│   │   ├── PrescriptionAssistCapabilityExecutor.java
│   │   ├── KbQueryCapabilityExecutor.java
│   │   ├── ScheduleCapabilityExecutor.java
│   │   └── DiscussionConclusionCapabilityExecutor.java
│   ├── thin-adapter/                      # 薄适配器子包（Phase 4 能力委托）
│   │   ├── DiagnosisCapabilityExecutor.java
│   │   ├── AnalysisReportForInspectionCapabilityExecutor.java
│   │   ├── AnalysisReportForLabTestCapabilityExecutor.java
│   │   ├── ImageAnalysisCapabilityExecutor.java
│   │   ├── RecommendExaminationCapabilityExecutor.java
│   │   └── RecommendExecutionOrderCapabilityExecutor.java
│   ├── router/
│   │   ├── ModelRouter.java                # 模型路由接口
│   │   ├── DefaultModelRouter.java         # 默认路由实现（基于能力标识 + 配置映射）
│   │   └── ModelRoute.java                # 路由条目值对象
│   ├── client/
│   │   ├── LlmChatService.java            # 大模型对话客户端接口（chat + structuredChat）
│   │   ├── LlmChatStreamService.java      # 大模型流式对话客户端接口（Flux）
│   │   ├── HttpApiLlmChatService.java     # HTTP API 同步对话实现（仅实现 LlmChatService）
│   │   ├── HttpApiLlmChatStreamService.java # HTTP API 流式对话实现（仅实现 LlmChatStreamService）
│   │   ├── SpringAiLlmChatService.java    # Spring AI ChatModel 同步对话实现（仅实现 LlmChatService）
│   │   ├── SpringAiLlmChatStreamService.java # Spring AI ChatModel 流式对话实现（仅实现 LlmChatStreamService）
│   │   ├── DelegatingLlmChatService.java # LlmChatService 分发层（按 ClientType 路由到对应实现）
│   │   ├── LlmChatRequest.java            # 对话请求值对象（messages + options）
│   │   ├── LlmChatMessage.java            # 对话消息值对象（role + content）
│   │   ├── LlmChatMessageRole.java        # 对话消息角色枚举
│   │   ├── LlmChatOptions.java            # 对话参数配置值对象（强类型）
│   │   ├── LlmChatResponse.java           # 对话响应值对象（含内嵌 LlmChatUsage）
│   │   ├── LlmChatUsage.java              # Token 用量内嵌静态类
│   │   ├── StructuredChatResult.java      # 结构化调用结果值对象（含 data/retryCount/usage）
│   │   ├── CredentialProvider.java        # 凭据查询接口
│   │   ├── EndpointRateLimiter.java       # 端点限流器（令牌桶）
│   │   └── exception/
│   │       ├── StructuredOutputNotSupportedException.java # 结构化输出格式不支持异常
│   │       └── LlmInfrastructureException.java             # LLM 基础设施异常
│   ├── template/
│   │   ├── PromptTemplateManager.java      # 模板管理接口
│   │   ├── PromptTemplate.java            # 模板值对象（JPA Entity）
│   │   ├── DatabasePromptTemplateManager.java # 数据库持久化实现
│   │   └── PromptTemplateRepository.java  # JPA Repository
│   ├── experiment/
│   │   ├── ExperimentManager.java          # 实验管理接口
│   │   ├── ExperimentAssignment.java       # 分组结果值对象
│   │   ├── Experiment.java                # 实验配置值对象（JPA Entity）
│   │   ├── ExperimentGroup.java           # 实验分组 JPA Entity
│   │   ├── ExperimentRepository.java      # JPA Repository
│   │   └── HashBucketExperimentManager.java # 哈希分桶实现
│   ├── metrics/
│   │   ├── AiMetricsCollector.java         # 指标采集接口
│   │   ├── AiCallRecord.java              # 调用记录值对象
│   │   ├── AiCallLogEntity.java           # AI 调用日志 JPA 实体（新增）
│   │   ├── AiCallLogRepository.java       # AI 调用日志 JPA Repository
│   │   ├── AiCallLogStats.java            # AI 调用日志月汇总 JPA 实体（新增）
│   │   ├── AiCallLogStatsRepository.java  # AI 调用日志月汇总 JPA Repository（新增）
│   │   ├── LoggingMetricsCollector.java   # 日志输出实现
│   │   ├── SlidingWindowMetricsStore.java # 调用指标滑动窗口存储（新增）
│   │   └── ModelEndpointHealthManager.java # 模型端点健康状态管理器（新增）
│   ├── parser/
│   │   ├── StructuredOutputParser.java     # 结构化输出解析接口
│   │   └── JsonStructuredOutputParser.java # JSON 结构化解析实现
│   ├── fallback/
│   │   ├── LocalRuleFallback.java         # 本地规则降级接口
│   │   └── PrescriptionLocalRuleFallback.java # 处方审核本地规则实现
│   ├── mock/
│   │   └── MockAiService.java             # 保留，Phase 5+ 仅用于开发/测试
│   ├── degradation/
│   │   ├── NoOpDegradationStrategy.java    # 保留
│   │   ├── TimeoutDegradationStrategy.java # 超时降级策略
│   │   └── CircuitBreakerDegradationStrategy.java # 熔断降级策略
│   ├── config/
│   │   ├── AiPlatformConfig.java                      # 底座 Bean 装配与配置属性绑定
│   │   └── AiPlatformEnvironmentPostProcessor.java    # 配置转发前置处理器（EnvironmentPostProcessor）
│   └── FallbackAiService.java             # 装饰器（装配策略变更）
```

### 2.2 模块依赖方向

```
ai-api ←──────────────────────────── ai-impl
  │                                      │
  │  (interface + DTO, no impl dep)     ├── orchestrator/ ──> router/, template/, experiment/, client/, fallback/, degradation/, metrics/
  │                                     ├── router/ ──> config (YaML/DB)
  │                                     ├── client/ ──> Spring AI / HTTP (外部依赖)
  │                                     ├── template/ ──> JPA Repository
  │                                     ├── experiment/ ──> JPA Repository
  │                                     ├── metrics/ ──> JPA Repository + Micrometer
  │                                     ├── parser/ ──> ai-api DTO
  │                                     ├── fallback/ ──> ai-api DTO + 业务规则
  │                                     └── thin-adapter/ ──> Phase 4 业务服务模块

**依赖规则**：
- `ai-api` 接口签名冻结（变更范围严格限定为 §1.6 API Surface 状态表中列出的 4 项 ai-api 变更），业务模块仅依赖 `ai-api`
- `ai-impl` 内部各子包之间按单向依赖：orchestrator 为顶层编排，依赖其余子包；其余子包之间不互相依赖
- `client/` 是唯一引入外部大模型依赖（Spring AI / HTTP 客户端）的子包，包含 `LlmChatService`、`LlmChatStreamService` 及消息 DTO 类型
- `template/`、`experiment/`、`metrics/` 各自拥有一套 JPA Repository + Entity，数据持久化独立
- `thin-adapter/` 中的 6 个薄适配器型 CapabilityExecutor 各持有对应 Phase 4 业务服务的引用。ai-impl 模块在 Maven 层面声明对 Phase 4 业务服务模块的依赖，作用域使用 `provided` 而非 `compile`。理由如下：(1) `provided` 避免 ai-impl 对 Phase 4 业务模块产生编译期强耦合——薄适配器作为 ai-impl 内部的可选组件，其编译期类型引用仅存在于薄适配器实现类中，`provided` 作用域确保 ai-impl 主体代码不感知 Phase 4 接口；(2) 运行时 Phase 4 业务模块的 JAR 已部署在同一 JVM 中（单体架构或同一 Spring Boot 容器），ClassLoader 可正常加载，不存在 `test` 作用域下的运行期类加载失败风险；(3) 若后续某项能力从薄适配器升级为底座完整管线（调用 `ModelRouter` + `LlmChatService`），只需移除 `provided` 依赖并交还 Maven 依赖管理权。Phase 4 业务服务接口不在 ai-api 中重新定义——薄适配器直接引用 Phase 4 模块中的现有业务接口（如 `com.aimedical.modules.diagnosis.service.DiagnosisService`），通过构造器注入获取服务实例。依赖方向为 `ai-impl → Phase 4 modules`（出向依赖，包 G 依赖外部模块），不产生循环依赖。⚠️ **Spring Boot uber-JAR 部署的运行时风险**：`spring-boot-maven-plugin` 默认不将 `provided` 作用域的依赖打包进 uber-JAR。使用 `provided` 意味着部署时需确保 Phase 4 业务模块的 JAR 作为外部依赖部署（如放置在 `WEB-INF/lib/` 或通过容器共享类路径加载）。若项目使用 `spring-boot-maven-plugin` 的 `repackage` 目标打包为可执行 JAR，需在构建配置中将 Phase 4 业务模块显式声明为 `compile` 作用域或通过 `extraLib` 机制将其纳入 uber-JAR。此风险在 §2.2 显式记录作为已知约束，实现者在搭建构建流水线时需根据部署方式选择适配方案

### 2.3 类图

```mermaid
classDiagram
    class AiService {
        <<interface>>
        +triage(TriageRequest) CompletableFuture~AiResult~TriageResponse~~
        +prescriptionCheck(PrescriptionCheckRequest) CompletableFuture~AiResult~PrescriptionCheckResponse~~
        +generateMedicalRecord(MedicalRecordGenRequest) CompletableFuture~AiResult~MedicalRecordGenResponse~~
        +prescriptionAssist(PrescriptionAssistRequest) CompletableFuture~AiResult~PrescriptionAssistResponse~~
        +knowledgeBaseQuery(KbQueryRequest) CompletableFuture~AiResult~KbQueryResponse~~
        +schedule(ScheduleRequest) CompletableFuture~AiResult~ScheduleResponse~~
        +discussionConclusion(DiscussionConclusionRequest) CompletableFuture~AiResult~DiscussionConclusionResponse~~
        +diagnosis(DiagnosisRequest) CompletableFuture~AiResult~DiagnosisResponse~~
        +analysisReportForInspection(AnalysisReportForInspectionRequest) CompletableFuture~AiResult~AnalysisReportForInspectionResponse~~
        +analysisReportForLabTest(AnalysisReportForLabTestRequest) CompletableFuture~AiResult~AnalysisReportForLabTestResponse~~
        +imageAnalysis(ImageAnalysisRequest) CompletableFuture~AiResult~ImageAnalysisResponse~~
        +recommendExamination(RecommendExaminationRequest) CompletableFuture~AiResult~RecommendExaminationResponse~~
        +recommendExecutionOrder(RecommendExecutionOrderRequest) CompletableFuture~AiResult~RecommendExecutionOrderResponse~~
    }

    class FallbackAiService {
        <<decorator>>
        <<@Primary>>
        -ObjectProvider~AiService~ delegateProvider
        -AiService delegate  // 由 @PostConstruct 从 delegateProvider.getIfUnique() 解析
        +triage() CompletableFuture~AiResult~TriageResponse~~
        +prescriptionCheck() CompletableFuture~AiResult~PrescriptionCheckResponse~~
        +... (委托给 delegate + 降级兜底)
    }

    class AiOrchestrator {
        -Map~String, CapabilityExecutor~ executorMap
        -SlidingWindowMetricsStore metricsStore
        -AiMetricsCollector metricsCollector
        +triage() CompletableFuture~AiResult~TriageResponse~~
        +prescriptionCheck() CompletableFuture~AiResult~PrescriptionCheckResponse~~
        +... (13 个方法统一委托 handle())
        +handle(capabilityId, request) CompletableFuture~AiResult~R~~
    }

    class CapabilityExecutor~T, R~ {
        <<interface>>
        +execute(T request, String capabilityId) CompletableFuture~AiResult~R~~
        +getCapabilityId() String
        +getInputType() Class~T~
        +getOutputType() Class~R~
    }

    class AbstractCapabilityExecutor~T, R~ {
        <<abstract>>
        #Class~T~ inputType
        #Map~String,Duration~ capabilityTimeoutConfig
        #Map~String,Duration~ thinAdapterPerCapabilityConfig
        #Map~String,Duration~ parseTimeoutConfig
        #Duration thinAdapterTimeout
        #Duration parseTimeoutDefault
        #volatile long elapsedInDoExecuteInternal  // 超时原因细化：supplyAsync lambda 入口处设置，供子类 refineTimeoutReason() 读取
        +AbstractCapabilityExecutor(inputType, promptTemplateManager, modelRouter, llmChatService, structuredOutputParser, metricsCollector, metricsStore, endpointHealthManager, degradationStrategyMapRef, localRuleFallback, capabilityTimeoutConfig, parseTimeoutConfig, parseTimeoutDefault, thinAdapterTimeout, thinAdapterPerCapabilityConfig, objectMapper)  // 16 参数全量构造器，完整签名见 §3.1 注入方式伪代码
        #doDegrade(startTime, degradeReason, request, capabilityId, departmentId, callerRole, callerId, visitId, patientId, sessionId, inputSummary, outputSummary, promptVersion, modelId, sentinelReason) AiResult~R~~
        #doDegrade(startTime, degradeReason, request, capabilityId, CallContext callContext, String modelId, String sentinelReason) AiResult~R~~  // 二期迁移目标，详见§3.1「三期分阶段迁移计划」
        +execute(T request, String capabilityId) CompletableFuture~AiResult~R~~ (template method)
        #doExecuteInternal(startTime, request, capabilityId, departmentId, userId, sessionId, callerRole, callerId, visitId, patientId, inputSummary) AiResult~R~~
        #extractVariables(T request) Map~String,Object~
        #doExtractDepartmentId(T request) String
        #doExtractVisitId(T request) String
        #doExtractPatientId(T request) String
        #doExtractSessionId(T request) String
        #extractOutputSummary(Object result) String
        #executeStandardPipeline(startTime, request, capabilityId, departmentId, userId, sessionId, callerRole, callerId, visitId, patientId, inputSummary, variables, promptVersion, sentinelReason) AiResult~R~~  // protected final 方法，封装标准管线步骤供子类调用
    }

    class TriageCapabilityExecutor {
    }
    class PrescriptionCheckCapabilityExecutor {
    }
    class MedicalRecordGenCapabilityExecutor {
    }
    class PrescriptionAssistCapabilityExecutor {
    }
    class KbQueryCapabilityExecutor {
    }
    class ScheduleCapabilityExecutor {
    }
    class DiscussionConclusionCapabilityExecutor {
        +String compressionLightweightEndpoint
        +ClientType compressionLightweightClientType
        +Duration transcriptSummaryTimeout
        +boolean tokenizerAvailable
    }
    class DiagnosisCapabilityExecutor {
    }
    class AnalysisReportForInspectionCapabilityExecutor {
    }
    class AnalysisReportForLabTestCapabilityExecutor {
    }
    class ImageAnalysisCapabilityExecutor {
    }
    class RecommendExaminationCapabilityExecutor {
    }
    class RecommendExecutionOrderCapabilityExecutor {
    }

    class ModelRouter {
        <<interface>>
        +route(String capabilityId, ExperimentAssignment assignment) ModelRoute
    }
    class DefaultModelRouter {
    }
    class ModelRoute {
        +String endpointId
        +String modelId
        +String endpoint
        +ClientType clientType
        +Duration connectionTimeout
        +Duration readTimeout
        +Map~String,Object~ parameters
        +AuthType authType
    }
    class AuthType {
        <<enum>>
        API_KEY
        OAUTH2
        NONE
    }
    ModelRoute --> "1" AuthType : has

    class LlmChatService {
        <<interface>>
        +chat(LlmChatRequest) CompletableFuture~AiResult~LlmChatResponse~~
        +structuredChat(LlmChatRequest, Class~T~) CompletableFuture~AiResult~StructuredChatResult~T~~~
    }
    class LlmChatStreamService {
        <<interface>>
        +chatStream(LlmChatRequest) Flux~LlmChatResponse~
    }
    class DelegatingLlmChatService {
        <<delegator>>
        -Map~ClientType, LlmChatService~ delegates
        +chat(LlmChatRequest) CompletableFuture~AiResult~LlmChatResponse~~
        +structuredChat(LlmChatRequest, Class~T~) CompletableFuture~AiResult~StructuredChatResult~T~~~
    }
    class ClientType {
        <<enum>>
        HTTP_API
        SPRING_AI
    }
    class HttpApiLlmChatService {
    }
    class SpringAiLlmChatService {
    }
    class LlmChatRequest {
        <<constructor>>
        +LlmChatRequest(messages, options, clientType, tools)
        +List~LlmChatMessage~ messages
        +LlmChatOptions options
        +ClientType clientType
        +List~ChatToolDefinition~ tools
    }
    class LlmChatMessage {
        +LlmChatMessageRole role
        +String content
    }
    class LlmChatMessageRole {
        <<enum>>
        SYSTEM
        USER
        ASSISTANT
    }
    class LlmChatOptions {
        +String modelId
        +Double temperature
        +Integer maxTokens
        +List~String~ stopSequences
        +Double topP
        +Double frequencyPenalty
        +Double presencePenalty
    }
    class LlmChatResponse {
        +String content
        +LlmChatUsage usage
        +String modelId
        +int retryCount
    }
    class LlmChatUsage {
        +int promptTokens
        +int completionTokens
        +int totalTokens
    }
    class StructuredChatResult~T~ {
        +T data
        +int retryCount
        +LlmChatUsage usage
    }
    class StructuredOutputNotSupportedException {
        <<exception>>
    }
    class LlmInfrastructureException {
        <<exception>>
    }
    LlmChatResponse *--> "1" LlmChatUsage : has
    LlmChatService ..> StructuredChatResult : returns
    LlmChatService ..> StructuredOutputNotSupportedException : throws
    LlmChatService ..> LlmInfrastructureException : throws
    LlmChatRequest *--> "1..*" LlmChatMessage : has
    LlmChatRequest --> "0..*" ChatToolDefinition : has
    LlmChatMessage --> "1" LlmChatMessageRole : uses

    class PromptTemplateManager {
        <<interface>>
        +render(String capabilityId, String departmentId, Map~String,Object~ variables, Integer promptVersion) String
    }
    class DatabasePromptTemplateManager {
    }
    class PromptTemplate {
        +Long id
        +String capabilityId
        +String departmentId
        +String content
        +int version
        +TemplateStatus status
    }
    class PromptTemplateRepository {
        <<JPA Repository>>
    }

    class ExperimentManager {
        <<interface>>
        +assign(String capabilityId, String userId, String sessionId) ExperimentAssignment
    }
    class HashBucketExperimentManager {
    }
    class ExperimentGroup {
        +Long id
        +String group_id
        +int percentage
        +String target_model_id
        +Integer target_prompt_version
    }
    Experiment "1" o--> "0..*" ExperimentGroup : has

    class Experiment {
        +Long id
        +String capabilityId
        +List~ExperimentGroup~ groups
        +ExperimentStatus status
        +LocalDateTime startTime
        +LocalDateTime endTime
    }
    class ExperimentAssignment {
        +String experimentId
        +String groupId
        +String targetModelId
        +Integer targetPromptVersion
    }

    class AiMetricsCollector {
        <<interface>>
        +record(AiCallRecord record) void
    }
    class LoggingMetricsCollector {
    }
    class AiRequestBase {
        <<abstract>>
        +String visitId
        +String patientId
        +String sessionId
        +String departmentId
    }

    class AiCallRecord {
        +LocalDateTime callTime
        +String capabilityId
        +String capabilityName
        +String visitId
        +String patientId
        +String departmentId
        +String callerRole
        +String callerId
        +String inputSummary
        +String outputSummary
        +boolean degraded
        +String degradationReason
        +long elapsedMs
        +String errorCode
        +String errorMessage
        +String modelId
        +int retryCount
        +String sessionId
        +Integer promptVersion
        +Integer promptTokens
        +Integer completionTokens
        +Integer totalTokens
    }
    class AiCallLogEntity {
        <<JPA @Entity>>
        +Long id
        +LocalDateTime callTime
        +String capabilityId
        +String capabilityName
        +String visitId
        +String patientId
        +String departmentId
        +String callerRole
        +String callerId
        +String inputSummary
        +String outputSummary
        +boolean degraded
        +String degradationReason
        +long elapsedMs
        +String errorCode
        +String errorMessage
        +String modelId
        +int retryCount
        +String sessionId
        +Integer promptVersion
        +Integer promptTokens
        +Integer completionTokens
        +Integer totalTokens
    }
    class AiCallLogRepository {
        <<JPA Repository>>
    }

    class AiCallLogStats {
        <<JPA @Entity>>
        +Long id
        +String capability_id
        +String stat_month
        +long total_calls
        +long success_count
        +long degraded_count
        +long failure_count
        +double avg_elapsed_ms
        +double p50_elapsed_ms
        +double p95_elapsed_ms
        +double p99_elapsed_ms
    }
    class AiCallLogStatsRepository {
        <<JPA Repository>>
    }

    class SlidingWindowMetricsStore {
        +recordSuccess(String capabilityId, long elapsedMs) void
        +recordDegraded(String capabilityId, long elapsedMs) void
        +recordFailure(String capabilityId) void
        +getFailureRate(String capabilityId) double
        +getEffectiveFailureRate(String capabilityId) double
        +getAverageElapsed(String capabilityId) double
        +buildDegradationContext(String capabilityId, String requestType) DegradationContext
    }

    class ModelEndpointHealthManager {
        +getState(String endpointId) EndpointState
        +recordCallResult(String endpointId, boolean success, long elapsedMs) void
        +tryProbe(String endpointId) boolean
    }

    class EndpointRateLimiter {
        +tryAcquire(String endpointId) boolean
    }

    class Credential {
        +AuthType authType
        +String credentialValue
        +Instant expiresAt
        +String vaultPath
    }
    class CredentialProvider {
        <<interface>>
        +getCredential(String endpointId) Credential
    }
    CredentialProvider ..> LlmChatService : authenticates
    CredentialProvider ..> Credential : returns

    class AiPlatformConfig {
        <<@Configuration>>
        <<@EnableConfigurationProperties>>
        +llmCallExecutor() Executor
        +metricsAsyncExecutor(MetricsAsyncProperties) Executor
        +degradationStrategyMapRef() AtomicReference
        +capabilityTimeoutConfig(AiExecutionProperties) Map
    }
    class AiPlatformEnvironmentPostProcessor {
        <<EnvironmentPostProcessor>>
        +postProcessEnvironment(ConfigurableEnvironment, SpringApplication) void
    }
    AiPlatformConfig ..> EndpointRateLimiter : configures

    class DegradationStrategy {
        <<interface>>
        +shouldDegrade(DegradationContext context) boolean
        +getOrder() int [default method returns 0]
    }
    class DegradationContext {
        <<Serializable>>
        +int invocationCount
        +long lastFailureTime
        +long elapsedTime
        +String requestType
        +int failureCount
        +String departmentId
        +long serializedTimestamp
        +postDeserializationValidate() void
        +isFresh() boolean
        +isInitialized() boolean
        +setDepartmentId(String departmentId) void
        +getDepartmentId() String
        +Builder builder()
    }
    class TimeoutDegradationStrategy {
        +shouldDegrade() boolean
    }
    class CircuitBreakerDegradationStrategy {
        +shouldDegrade() boolean
    }
    class NoOpDegradationStrategy {
    }

    class StructuredOutputParser {
        <<interface>>
        +parse(LlmChatResponse response, Class~T~ targetClass) T
    }
    class JsonStructuredOutputParser {
    }

    class LocalRuleFallback~T, R~ {
        <<interface>>
        +fallback(T request) AiResult~R~
    }
    class PrescriptionLocalRuleFallback {
    }

    AiService <|.. FallbackAiService : implements
    AiService <|.. AiOrchestrator : implements
    AiService <|.. MockAiService : implements

    FallbackAiService o--> AiService : wraps (delegate, resolved via ObjectProvider.getIfUnique())

    AiOrchestrator o--> "1..*" CapabilityExecutor : routes via Map
    AiOrchestrator o--> "1" SlidingWindowMetricsStore : has
    AiOrchestrator o--> "1" AiMetricsCollector : has

    CapabilityExecutor <|.. AbstractCapabilityExecutor : implements
    AbstractCapabilityExecutor <|-- TriageCapabilityExecutor : extends
    AbstractCapabilityExecutor <|-- PrescriptionCheckCapabilityExecutor : extends
    AbstractCapabilityExecutor <|-- MedicalRecordGenCapabilityExecutor : extends
    AbstractCapabilityExecutor <|-- PrescriptionAssistCapabilityExecutor : extends
    AbstractCapabilityExecutor <|-- KbQueryCapabilityExecutor : extends
    AbstractCapabilityExecutor <|-- ScheduleCapabilityExecutor : extends
    AbstractCapabilityExecutor <|-- DiscussionConclusionCapabilityExecutor : extends
    AbstractCapabilityExecutor <|-- DiagnosisCapabilityExecutor : extends
    AbstractCapabilityExecutor <|-- AnalysisReportForInspectionCapabilityExecutor : extends
    AbstractCapabilityExecutor <|-- AnalysisReportForLabTestCapabilityExecutor : extends
    AbstractCapabilityExecutor <|-- ImageAnalysisCapabilityExecutor : extends
    AbstractCapabilityExecutor <|-- RecommendExaminationCapabilityExecutor : extends
    AbstractCapabilityExecutor <|-- RecommendExecutionOrderCapabilityExecutor : extends

    CapabilityExecutor --> ModelRouter : uses
    CapabilityExecutor --> LlmChatService : uses
    CapabilityExecutor --> LlmChatStreamService : optional uses
    CapabilityExecutor --> PromptTemplateManager : uses
    CapabilityExecutor --> ExperimentManager : uses
    CapabilityExecutor --> StructuredOutputParser : uses
    CapabilityExecutor --> AiMetricsCollector : uses
    CapabilityExecutor --> LocalRuleFallback : optional uses
    CapabilityExecutor --> SlidingWindowMetricsStore : uses
    CapabilityExecutor --> DegradationStrategy : uses (per-capability injection)
    CapabilityExecutor --> ModelEndpointHealthManager : optional uses

    ModelRouter <|.. DefaultModelRouter
    ModelRouter --> ModelRoute : returns

    LlmChatService <|.. DelegatingLlmChatService : implements
    LlmChatService <|.. HttpApiLlmChatService
    LlmChatService <|.. SpringAiLlmChatService
    DelegatingLlmChatService --> "1" HttpApiLlmChatService : dispatches
    DelegatingLlmChatService --> "1" SpringAiLlmChatService : dispatches
    DelegatingLlmChatService ..> ClientType : dispatch key
    LlmChatStreamService <|.. HttpApiLlmChatStreamService
    LlmChatStreamService <|.. SpringAiLlmChatStreamService
    LlmChatService --> LlmChatRequest : receives
    LlmChatService --> LlmChatResponse : returns
    LlmChatService ..> StructuredOutputParser : optionale uses (structuredChat fallback)

    PromptTemplateManager <|.. DatabasePromptTemplateManager
    DatabasePromptTemplateManager --> PromptTemplateRepository : reads/writes
    PromptTemplateRepository --> PromptTemplate : manages

    ExperimentManager <|.. HashBucketExperimentManager
    HashBucketExperimentManager --> ExperimentRepository : reads
    ExperimentRepository --> Experiment : manages

    AiMetricsCollector <|.. LoggingMetricsCollector
    LoggingMetricsCollector --> AiCallLogRepository : writes
    AiCallLogRepository --> AiCallLogEntity : persists
    LoggingMetricsCollector --> AiCallLogStatsRepository : writes
    AiCallLogStatsRepository --> AiCallLogStats : persists

    DegradationStrategy <|.. NoOpDegradationStrategy
    DegradationStrategy <|.. TimeoutDegradationStrategy
    DegradationStrategy <|.. CircuitBreakerDegradationStrategy
    TimeoutDegradationStrategy --> SlidingWindowMetricsStore : reads
    CircuitBreakerDegradationStrategy --> SlidingWindowMetricsStore : reads

    StructuredOutputParser <|.. JsonStructuredOutputParser

    LocalRuleFallback <|.. PrescriptionLocalRuleFallback
```

---

## 3. 核心抽象

### 3.1 编排层

#### `AiOrchestrator` — 统一编排路由器（class，归属 `ai-impl/orchestrator/`）

**职责**：替代原 `MockAiService` 成为 `FallbackAiService` 的实际委托对象（`ai.platform.enabled=true` 时激活），实现 `AiService` 全部 13 个方法。每个方法的执行流程为按能力标识查找对应的 `CapabilityExecutor` 并委托其完成完整执行管线：

1. **查找执行器**：按能力标识从内部映射表（`Map<String, CapabilityExecutor>`）中查找匹配的 `CapabilityExecutor`
2. **委托执行**：调用 `executor.execute(request, capabilityId)` 返回 `CompletableFuture<AiResult<R>>`
3. **返回结果**：将执行器返回的 `AiResult` 直接返回给调用方

`AiOrchestrator` 不介入管线内部步骤（模板渲染、实验分流、模型路由、LLM 调用、结果解析、指标采集、降级判定与降级兜底），上述步骤由 `CapabilityExecutor.execute()` 在其内部完整执行。

**能力标识到 CapabilityExecutor 的映射机制**：
- 所有 `CapabilityExecutor` 实现注册为 Spring Bean（`@Component`），通过 `getCapabilityId()` 返回能力标识
- `AiOrchestrator` 在 `@PostConstruct` 阶段扫描 `List<CapabilityExecutor>` 自动注入，按 `getCapabilityId()` 构建 `Map<String, CapabilityExecutor>` 映射表
- 未注册对应执行器的能力标识在被 `AiOrchestrator` 接收时将抛出明确的配置异常（启动期 fail-fast 而非运行时静默降级）

**AiService 方法到能力标识的映射约定**：

| AiService 方法 | capabilityId | 归属 |
|---------------|-------------|------|
| triage() | `"TRIAGE"` | Phase 5 底座 |
| prescriptionCheck() | `"RX_AUDIT"` | Phase 5 底座 |
| generateMedicalRecord() | `"MEDICAL_RECORD_GEN"` | Phase 5 底座 |
| prescriptionAssist() | `"RX_ASSIST"` | Phase 5 底座 |
| knowledgeBaseQuery() | `"KB_QUERY"` | Phase 5 底座 |
| schedule() | `"SCHEDULE"` | Phase 5 底座 |
| discussionConclusion() | `"DISCUSSION_CONCLUSION"` | Phase 5 底座 |
| diagnosis() | `"DIAGNOSIS"` | Phase 4 薄适配器 |
| analysisReportForInspection() | `"ANALYSIS_REPORT_INSPECTION"` | Phase 4 薄适配器 |
| analysisReportForLabTest() | `"ANALYSIS_REPORT_LABTEST"` | Phase 4 薄适配器 |
| imageAnalysis() | `"IMAGE_ANALYSIS"` | Phase 4 薄适配器 |
| recommendExamination() | `"RECOMMEND_EXAM"` | Phase 4 薄适配器 |
| recommendExecutionOrder() | `"RECOMMEND_EXEC_ORDER"` | Phase 4 薄适配器 |

映射表在 `AiPlatformConfig` 中以常量 `Map<String, String>` 维护（非强制校验，仅用于文档和启动期配置检查），实际路由遵从 `CapabilityExecutor.getCapabilityId()` 返回值。

**Phase 4 能力的处理策略**：
- 13 项 AI 能力中，7 项归属 Phase 5 底座范围（3.4.1/3.4.2/3.4.3/3.4.8/3.4.10/3.4.12/3.4.13），使用完整 CapabilityExecutor 管线
- 其余 6 项（3.4.4 AI 智能诊断 / 3.4.5 AI 智能检查报告 / 3.4.6 AI 智能检验报告 / 3.4.7 AI 影像分析 / 3.4.9 AI 开立检查检验 / 3.4.11 AI 执行顺序推荐）已在 Phase 4 完成独立接入，不迁移至 Phase 5 底座
- 这 6 项能力在 `AiOrchestrator` 中注册为薄适配器型 CapabilityExecutor，其 `execute()` 方法直接委托给 Phase 4 的现有业务服务接口，不做底座管线绕行；底座仅为它们提供统一的降级判定入口与指标采集入口

**协作对象**：
- 实现 `AiService`，被 `FallbackAiService` 委托调用
- 内部持有 `Map<String, CapabilityExecutor>`（按能力标识索引的映射表）、`SlidingWindowMetricsStore`、`AiMetricsCollector`（用于兜底记录 handle() 层面意外异常的指标）
- 每个 `AiService` 方法的实现通过能力标识从映射表中查找对应的 `CapabilityExecutor` 并调用其 `execute()` 方法

**为何使用 class 而非 interface**：编排器是唯一的运行时实现实例，不需要多态；其核心职责是路由委托而非管线编排。Bean 装配通过 `AiPlatformConfig` 显式完成。

**线程安全模型**：AiOrchestrator 内部持有可变状态（通过 `SlidingWindowMetricsStore` 维护的滑动窗口），其线程安全性取决于被编排组件的线程安全性。`AiOrchestrator` 本身不引入 `synchronized` 大锁；并发瓶颈由各子组件独立承担。编码层面：`SlidingWindowMetricsStore` 内部使用 `ConcurrentHashMap` 和 `AtomicLong` 保证并发安全；`Map<String, CapabilityExecutor>` 在初始化后不再变更，读操作无竞争。

**Bean 装配策略**：
- `FallbackAiService`：标注 `@Primary`，通过 `ObjectProvider<AiService>` 延迟解析被装饰的 `AiService` 实例。由于 `@ConditionalOnProperty` 保证同时只有一个非装饰器 `AiService` 实现有效，`ObjectProvider.getIfUnique()` 可正确解析
- `AiOrchestrator`：标注 `@ConditionalOnProperty(name = "ai.platform.enabled", havingValue = "true")`
- `MockAiService`：标注 `@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "true", matchIfMissing = true)`，与现有代码兼容
- `AiPlatformEnvironmentPostProcessor`：作为独立配置转发前置处理器，通过 `EnvironmentPostProcessor` 机制在 Spring 启动早期（`@ConditionalOnProperty` 评估之前）将 `ai.platform.enabled` 反向转发到 `ai.mock.enabled` 配置项：`ai.platform.enabled=true` → `ai.mock.enabled=false`（底座激活时关闭 Mock），`ai.platform.enabled=false` → `ai.mock.enabled=true`（底座关闭时回退 Mock）。此反向转发确保 `MockAiService` 的条件注解与现有代码兼容，且两个开关互斥生效。转发在 `AiPlatformEnvironmentPostProcessor.postProcessEnvironment()` 中执行，早于任何 `@Bean` 初始化，与 YAML/PropertySource 属性的优先级关系为：EnvironmentPostProcessor 写入的值为最低优先级（在 YAML 和系统属性之后），仅当 PropertySource 中不存在 `ai.mock.enabled` 时才生效，不影响用户显式指定的覆盖值。`AiPlatformConfig` 不再承担此职责，专注容器内 Bean 装配
- `FallbackAiService` 不参与 `ai.platform.enabled` 条件，始终存在，且通过 `@Primary` 确保业务模块注入时优先选择

```yaml
ai:
  platform:
    enabled: true                     # true → AiOrchestrator 激活；false → 底座关闭
  mock:
    enabled: false                    # false → MockAiService 不激活（true 时激活，仅开发/测试）
```

#### `CapabilityExecutor<T, R>` — 能力执行泛型接口（interface，归属 `ai-impl/orchestrator/`）

**职责**：定义单项 AI 能力的完整执行管线契约。每个 AI 能力（如智能分诊、处方审核等）对应一个 `CapabilityExecutor` 实现，封装该能力的完整执行流程：

1. **降级预检**：通过 `SlidingWindowMetricsStore.buildDegradationContext()` 构建降级判定上下文，遍历注入的降级策略链；任一策略判定降级则直接跳至第 8 步降级兜底
2. **实验分流**：委托 `ExperimentManager` 判定当前请求是否命中 A/B 实验，若命中则返回分组信息（含目标 Prompt 版本号）
3. **模板渲染**：委托 `PromptTemplateManager` 按能力标识、科室标识和目标 Prompt 版本号检索模板，将业务 DTO 字段注入模板变量，生成渲染后 Prompt
4. **模型路由**：委托 `ModelRouter` 根据能力标识与实验分组选择目标模型配置（`ModelRoute`）；若返回 null 则直接跳至第 8 步降级兜底
5. **模型调用**：委托 `LlmChatService` 向目标模型发送对话请求（`chat()` 或 `structuredChat()`），获取响应结果；调用前设置硬超时
6. **结果解析**：委托 `StructuredOutputParser` 将原始文本输出解析为对应能力的 Java DTO
7. **指标采集**：委托 `AiMetricsCollector` 记录本次调用的能力标识、耗时、是否降级、Token 用量、错误码等；同时将耗时和结果记录到 `SlidingWindowMetricsStore`
8. **降级兜底**：若有 `LocalRuleFallback` 实现则执行本地规则降级，否则返回 `AiResult.degraded()`

**方法签名**：
```
CompletableFuture<AiResult<R>> execute(T request, String capabilityId)
    入参: T request — 能力对应的业务请求 DTO（如 TriageRequest）
    入参: String capabilityId — 能力标识（如 "TRIAGE"），用于路由、模板检索和指标上报
    返回值: CompletableFuture<AiResult<R>> — 异步返回包裹能力对应的业务响应 DTO，成功时通过 CompletableFuture.complete() 返回，失败或降级时以 CompletableFuture.complete(降级结果) 完成
    异常: 不抛出业务异常；LLM 调用失败、解析失败等均通过降级路径以 CompletableFuture 完成（非异常路径）

String getCapabilityId()
    返回值: 该执行器对应的能力标识字符串

Class<T> getInputType()
Class<R> getOutputType()
    返回值: 输入/输出 DTO 的 Class 对象，用于 AiOrchestrator 的泛型路由与参数校验
```

**Request DTO 线程安全契约**：`CapabilityExecutor.execute()` 接收的 `request` DTO 在整个执行管线中约定为**只读对象**，任何下游组件不得修改 `request` 或其嵌套字段。推荐各能力 DTO 设计为**不可变对象**（所有字段 `final`，无 setter 方法，Jackson 反序列化通过 `@ConstructorProperties` 或 `@JsonCreator` 完成）。若现有 DTO 由于历史原因无法改为不可变，则在 `AbstractCapabilityExecutor.execute()` 模板方法入口处对 `request` 执行防御性拷贝（通过 `ObjectMapper.convertValue(request, request.getClass())`），子类及下游组件操作拷贝副本。此约定在 `CapabilityExecutor` 接口的 Javadoc 中以 `@implNote` 形式声明。

⚠️ **不可变 DTO 与防御性拷贝的兼容性说明**：若 DTO 已按推荐设计为不可变（所有字段 `final`，无 setter 方法），则必须同时标注 Jackson 反序列化注解（`@JsonCreator` + `@ConstructorProperties`，或 Lombok 的 `@Jacksonized`），否则 `ObjectMapper.convertValue()` 将因无法构造实例而抛出异常。处理策略如下：(1) DTO 为不可变且标注了 Jackson 注解 → 防御性拷贝正常工作；(2) DTO 为可变（有 setter / 无参构造器） → 防御性拷贝正常工作；(3) DTO 为不可变但未标注 Jackson 注解 → 实现者须在 `doExtractVariables()` 或子类 `execute()` 中自行完成防御性拷贝（如手动构造副本），或在 DTO 上补全 Jackson 注解后依赖默认拷贝。此策略在 `CapabilityExecutor` 接口 Javadoc 中以 `@apiNote` 形式声明。

**UserId、SessionId、callerRole、callerId 的上下文来源与提取时机**：
- `userId` 在 `AbstractCapabilityExecutor.execute()` 入口处（`supplyAsync()` 调用之前）从 Spring `SecurityContextHolder.getContext().getAuthentication()` 提取当前操作用户标识。采用 null-safe 操作：若 `getAuthentication()` 返回 null（如定时任务、匿名访问场景），回退使用 `"SYSTEM"` 作为 userId。提取后以局部变量捕获到 lambda 闭包中，闭包内不再访问 ThreadLocal
- `sessionId` 从业务请求 DTO（`AiRequestBase.sessionId`）继承，或由 `AiOrchestrator` 在委托前从 HTTP Request 的 `X-Session-ID` Header 注入。在 `execute()` 入口处提取后传给管线
- `callerRole` 和 `callerId` 在 `execute()` 入口处从 `SecurityContext` / `RequestContext` 提取当前操作用户的角色和标识，与 userId 同步提取，传入 `AiCallRecord` 工厂方法用于指标记录
- **`userId` 与 `callerId` 语义区分**：两者均来自 `Authentication.getName()`，但语义和使用场景不同——`userId` 用于实验分流（`ExperimentManager.assign()` 的哈希分桶输入，要求同一用户在同一次会话中分桶结果一致）和 `AiCallRecord` 的上下文记录；`callerId` 专用于 `AiCallRecord` 的 `callerId` 字段（记录"谁调用了 AI 能力"的审计信息）和 `doDegrade()` 中的降级上下文追踪。两者值相同（均来自 `auth.getName()`），不存在同步问题。引入两个独立参数的理由：(1) 职责分离——`userId` 的语义是"业务用户标识"，`callerId` 的语义是"API 调用方标识"，虽然在当前实现中值相同，但若未来引入服务间调用（如网关层鉴权后调用底座），`callerId` 可能为系统账户而 `userId` 为真实患者/医生标识；(2) 管线参数显式化——通过命名区分使每个调用点的意图自解释，避免实现者误用同一值在两处不同语义的字段中。`AiCallRecord` 中 `callerRole` 和 `callerId` 字段独立记录，与 `userId` 不合并。
- 提取后的 userId、sessionId、callerRole、callerId 与 `inputSummary` 均在 `execute()` 入口处捕获为局部变量，以显式参数形式传入 `doExecuteInternal()` 和 `doDegrade()` 方法，方法体内不再通过闭包捕获局部变量
- `promptVersion` 在 `doExecuteInternal()` 中由 `experimentManager.assign()` 返回的 `assignment.getTargetPromptVersion()` 提供，以显式参数形式传入 `doDegrade()`；预检降级路径（实验分流前）和超时降级路径中 `promptVersion=null`

**协作对象**：
- 被 `AiOrchestrator` 在对应方法中通过 `getCapabilityId()` 匹配查找并调用 `execute()`
- 内部使用注入的 `PromptTemplateManager`、`ModelRouter`、`LlmChatService`、`StructuredOutputParser`、`AiMetricsCollector`、`SlidingWindowMetricsStore`、`ModelEndpointHealthManager` 完成管线
- 内部持有注入的 `List<DegradationStrategy>`（按能力标识配置的策略列表，由 `AiPlatformConfig` 按白名单分配，各实现不共享策略列表实例）
- 可选关联 `LocalRuleFallback`（仅处方审核等需要本地规则降级的能力）

**降级策略注入机制**：每个 `CapabilityExecutor` 实现通过构造器注入 `AtomicReference<Map<String, List<DegradationStrategy>>> degradationStrategyMapRef`（而非静态 Map 实例），在 `execute()` 中每轮调用均通过 `degradationStrategyMapRef.get()` 获取最新策略 Map 快照。此 `AtomicReference` 由 `AiPlatformConfig` 在 `@PostConstruct` 阶段初始化并作为 `@Bean` 暴露，支持运行时通过 `refreshDegradationStrategies()` 全量替换 Map 引用，实现策略列表的热加载。构建逻辑见下方 YAML→Bean 装配路径。

**YAML 配置到 Bean 引用的装配路径**：
```
1. application.yml → ai.degradation.strategies:
       TRIAGE: [timeout, circuit-breaker]
       RX_AUDIT: [timeout, noop]
2. AiPlatformConfig 通过 @ConfigurationProperties 绑定 YAML 到 Map<String, List<String>>
3. 注册 Strategy Bean：每个策略实现（TimeoutDegradationStrategy、CircuitBreakerDegradationStrategy、NoOpDegradationStrategy）作为 @Component 注册到 Spring 容器。
   ⚠️ Bean name 映射约定：YAML 配置简名（如 "timeout"、"circuit-breaker"、"noop"）与 @Component 默认生成的 Bean name（如 "timeoutDegradationStrategy"）不匹配。
   因此每个策略实现必须显式声明 @Component 的 Bean name 以与 YAML 引用名保持一致：
   - `@Component("timeout")` 对应 TimeoutDegradationStrategy
   - `@Component("circuit-breaker")` 对应 CircuitBreakerDegradationStrategy
   - `@Component("noop")` 对应 NoOpDegradationStrategy
   此约定确保 YAML 中的简名可直接用于 ApplicationContext.getBean("timeout", DegradationStrategy.class) 查找。
4. AiPlatformConfig 实现 ApplicationContextAware，在 @PostConstruct 阶段（上下文初始化完成后）注入 Map<String, DegradationStrategy>（Spring 自动按 Bean name 注入全部策略实现）
5. 根据 YAML 配置的能力→策略名称映射，从策略 Map 中查找并构建 Map<String, List<DegradationStrategy>>（key 为 capabilityId），存入实例字段
6. 通过 @Bean 方法暴露该 Map，CapabilityExecutor 在构造时注入此 Map，由 getCapabilityId() 选择对应的策略列表
```

⚠️ **时序风险缓解**：步骤 5 使用 `@PostConstruct`（此时所有 `@Component` 均已注册完成）替代 `@Bean` 方法内调用 `getBeansOfType()`，避免了 `@Bean` 初始化阶段 ApplicationContext 未完全就绪的问题。此机制在 `AiPlatformConfig` 中集中实现，确保策略装配路径可测试、可追踪。

**变量提取约定**：`CapabilityExecutor` 实现从业务请求 DTO 中提取 Prompt 模板变量，采用以下两种方式之一：
- **方式 A（默认）**：通过 Jackson `ObjectMapper.convertValue(request, Map.class)` 将 DTO 转为扁平键值对，适用于字段结构简单的 DTO
- **方式 B**：实现自定义 `extractVariables(T request)` 方法，适用于需要字段变换、拼接或条件过滤的场景
- 选择规则：若 DTO 字段名与模板变量名直接对应且无需预处理，使用方式 A；否则实现方式 B

**Phase 4 服务接口契约定义**：薄适配器直接委托的 Phase 4 业务服务接口（如 `DiagnosisService`）当前方法签名为 `XXXResponse execute(XXXRequest)`，入参和返回值均由各 Phase 4 模块自行定义。⚠️ **当前代码库现状**：6 项薄适配器对应的 Phase 4 请求 DTO（`DiagnosisRequest`、`AnalysisReportForInspectionRequest`、`AnalysisReportForLabTestRequest`、`ImageAnalysisRequest`、`RecommendExaminationRequest`、`RecommendExecutionOrderRequest`）均为**空类**（无业务字段），无法为 Phase 4 服务提供有意义的入参。此状态下薄适配器无法通过 `phase4ServiceDelegate.execute(request)` 获得有效调用结果。**底座侧处理策略**：
1. Phase 4 服务接口的入参/返回值 DTO 定义权归属各 Phase 4 模块，底座薄适配器不做重新定义或扩充——薄适配器直接引用 Phase 4 模块中现有接口签名
2. 若 Phase 4 服务的 `execute(EmptyRequest)` 方法无法在空 DTO 入参下返回真实结果，需由 Phase 4 模块补充业务字段或底座侧在委托调用前通过其他途径（如 HTTP API 直接调用 Phase 4 模块的内部服务）填充请求参数。此决策需在 Phase 4 与 Phase 5 的跨包 OOD 协作会议上确认。**跨包协作会议的时间线与验收标准**：
   - **最晚截止时间**：底座切流之前 2 周（即 Phase 5 底座 P0 Batch 1~3 集成测试完成后）。若截止时间前未达成共识，底座薄适配器采用以下临时 fallback 方案
   - **临时 fallback 方案（截止前未达成共识时的兜底行为）**：非 HTTP 场景（MQ/定时任务）下 `extractDepartmentIdFromDto()` 等字段提取方法返回 null 时，薄适配器 `doExecuteInternal()` 不阻塞等待 Phase 4 服务返回，而是直接走降级路径（`DegradationReason.INFRASTRUCTURE_ERROR + ":Phase4DtoEmpty"`），确保底座管线不因空 DTO 参数导致线程池阻塞或 NPE。此行为通过 `AbstractCapabilityExecutor.doExtractDepartmentId()` 和 `doExtractVisitId()` 等方法中的 null 检测实现：在非 HTTP 场景下若 `extractXxxFromDto()` 返回 null 且 DTO 为当前已知的空 DTO 类型（通过 `className` 或注册表判定），则跳过委派调用，直接触发对应降级路径
   - **Phase 4 DTO 改造最小验收标准**：(1) 每个 Phase 4 能力至少新增一个业务字段（如 `DiagnosisRequest.symptomDescription: String`），使 `execute(XXXRequest)` 可基于真实入参返回非空结果；(2) 新增字段需标注 `@JsonProperty` 确保 Jackson 序列化/反序列化正确；(3) 改造后的 DTO 需通过底座薄适配器 `doExtractDepartmentId()`/`doExtractVisitId()`/`doExtractPatientId()`/`doExtractSessionId()` 四方法的 null-safe 检测（提取到字段值时正确提取，提取不到时返回 null 而非 NPE）；(4) 验收测试通过标志：底座薄适配器在非 HTTP 场景（`RequestContextHolder.getRequestAttributes() == null`）下调用 `phase4ServiceDelegate.execute(request)` 时不再因请求 DTO 为空而返回无意义结果或抛出异常
3. **底座侧 DTO 现状记录**：6 项薄适配器能力的 DTO 当前状态如下——`AnalysisReportForInspectionRequest.java` / `AnalysisReportForLabTestRequest.java` / `ImageAnalysisRequest.java` / `RecommendExaminationRequest.java` / `RecommendExecutionOrderRequest.java` 代码库路径为 `modules/ai/ai-api/dto/`（与 §2.1 目录结构中的 `base/` 子包路径不同），均为空类，无业务字段。`DiagnosisRequest.java` 同为空类。底座不为其新增业务字段，字段补齐由 Phase 4 模块自行决定

**Phase 4 响应级元数据契约（替代原服务实例级接口）**：为补齐 §1.4.1 所述模型/版本/重试三维度分析缺口，且消除原 `Phase4ServiceMetaProvider` 服务实例级接口在多线程环境下的请求间元数据污染风险（多个请求线程调用同一 Phase 4 服务时，`getRetryCount()` 等接口返回的数据可能属于不同线程的请求），改为响应 DTO 内嵌元数据的方案。定义如下（**可选契约**，Phase 4 响应 DTO 可选择实现或不实现，底座通过 `instanceof` 检测后安全调用）：

```java
// 元数据值对象 — 归属 ai-api/dto/base/ 包（底座侧定义，Phase 4 模块引入）
class Phase4ServiceMeta {
    String modelId;           // 模型标识，服务未记录时为 null
    Integer promptVersion;    // Prompt 版本号，无 Prompt 版本概念时为 null
    int retryCount;           // 内部重试次数，无重试机制时为 0
}

// 可选接口 — Phase 4 响应 DTO 实现此接口后底座可通过 instanceof 检测并提取元数据
// ⚠️ 线程安全：元数据绑定在响应 DTO 实例上（请求级），多个请求线程各自持有独立的响应 DTO，
// 消除了原服务实例级接口的跨请求元数据污染问题。
interface Phase4ServiceMetaCapable {
    Phase4ServiceMeta getServiceMeta();
}
```

调用约定：
1. 薄适配器 `doExecuteInternal()` 中，在调用 `phase4ServiceDelegate.execute(request)` 返回 `result` 后，通过 `result instanceof Phase4ServiceMetaCapable` 检测响应 DTO 是否实现该接口
2. 若未实现：modelId=null、promptVersion=null、retryCount=0
3. 若实现：调用 `((Phase4ServiceMetaCapable) result).getServiceMeta()` 获取元数据值对象后提取各字段传入 `AiCallRecord` 工厂方法
4. 元数据查询本身不抛异常（`getServiceMeta()` 返回非 null 的 `Phase4ServiceMeta` 实例，各字段可能为 null/0），不存在因元数据查询导致薄适配器管线失败的风险
5. 薄适配器伪代码中在 `doExecuteInternal()` 成功路径返回前新增元数据提取步骤（见 §4.2 薄适配器特化管线伪代码注释补充）

**元数据补齐优先级与时间线**：此元数据契约为可选优化项，不阻塞底座切流。Phase 4 模块在跨包协作会议（§3.1「跨包协作会议的时间线与验收标准」中定义的最晚截止时间）中确认是否实现。若 Phase 4 模块选择不实现，三个维度持续为 null/0，底座侧在 §1.4.1 记录此缺口并在指标报表中以缺失维度聚合。

**薄适配器型 CapabilityExecutor 的管线行为**：
Phase 4 的 6 项能力（DIAGNOSIS / ANALYSIS_REPORT_INSPECTION / ANALYSIS_REPORT_LABTEST / IMAGE_ANALYSIS / RECOMMEND_EXAM / RECOMMEND_EXEC_ORDER）使用简化的薄适配器管线，与完整管线相比：
- **包含**：降级预检（同完整管线）、直接委托 Phase 4 业务服务（不含模型路由，Phase 4 服务内部自行处理模型调用）、指标采集与 `recordSuccess/Failure/Degraded`（同完整管线）。薄适配器**不包含**实验分流步骤——departmentId 通过 `doExtractDepartmentId()` 独立提取而非实验管理器获取
- **不包含**：模板渲染（`PromptTemplateManager.render()`）、模型路由到 LLM 调用的完整链路、结构化输出解析
- **降级路径**：同完整管线——降级预检命中后走本地规则降级或 `AiResult.degraded()`

**Phase 4 模块异常契约**：薄适配器伪代码中 catch `BusinessException` 并调用 `getErrorCode()` 方法，需验证 6 个 Phase 4 业务模块是否统一存在此异常类型。各模块异常契约如下：

| Phase 4 模块 | 业务异常类 | getErrorCode() 是否存在 | 回退错误码策略 |
|-------------|-----------|----------------------|--------------|
| 诊断（diagnosis） | `com.aimedical.modules.diagnosis.exception.DiagnosisException` | ✓ 已有 | `"PHASE4_DiagnosisException"` |
| 检查报告（inspection-report） | `com.aimedical.modules.inspection.exception.InspectionException` | ✓ 已有 | `"PHASE4_InspectionException"` |
| 检验报告（labtest-report） | `com.aimedical.modules.labtest.exception.LabTestException` | ✓ 已有 | `"PHASE4_LabTestException"` |
| 影像分析（image-analysis） | `com.aimedical.modules.image.exception.ImageAnalysisException` | △ 需验证（建议补充 `getErrorCode()`） | `"PHASE4_ImageAnalysisException"` |
| 开立检查检验（examination） | `com.aimedical.modules.examination.exception.ExaminationException` | △ 需验证 | `"PHASE4_ExaminationException"` |
| 执行顺序推荐（execution-order） | `com.aimedical.modules.execution.exception.ExecutionOrderException` | △ 需验证 | `"PHASE4_ExecutionOrderException"` |

**异常处理规则统一契约**：
- 所有 Phase 4 业务异常必须继承 `RuntimeException`，使薄适配器 catch 块类型安全（方法签名不声明 checked exception）
- 在 `ai-api/dto/base/` 包中新增 `Phase4BusinessException` 抽象基类（继承 `RuntimeException`），6 个 Phase 4 模块的业务异常类（`DiagnosisException`、`InspectionException`、`LabTestException`、`ImageAnalysisException`、`ExaminationException`、`ExecutionOrderException`）统一继承此类。薄适配器 catch 块通过 `instanceof Phase4BusinessException` 匹配业务异常，消除原字符串类名匹配在异常类重命名时的维护脆弱性
- **⚠️ 过渡期回退机制**：过渡期内（6 个 Phase 4 模块尚未统一继承 `Phase4BusinessException` 时），`instanceof Phase4BusinessException` 将返回 false，导致业务异常被误分类为基础设施异常触发错误降级。为此在 catch 块中引入两阶段判定：
  1. **主判定**：`originalCause instanceof Phase4BusinessException` — 检测通过则直接按业务异常处理（最终态）
  2. **过渡期回退**：主判定失败时调用 `isKnownPhase4BusinessException(originalCause)` 方法，通过异常类全名的包路径前缀匹配（如 `"com.aimedical.modules.diagnosis"`、`"com.aimedical.modules.inspection"` 等）回退识别 Phase 4 业务异常。包路径注册表在 `AbstractCapabilityExecutor` 中以 `Set<String>` 常量维护（初始化时从 §3.1 异常契约表扫描已知 Phase 4 模块包名）
  `isKnownPhase4BusinessException` 方法签名及行为如下：
  ```java
  boolean isKnownPhase4BusinessException(Throwable cause)：
      // 从 cause 的完整类名中提取包路径前缀，检查是否在已知 Phase 4 模块包路径注册表中
      className = cause.getClass().getName()  // e.g. "com.aimedical.modules.diagnosis.exception.DiagnosisException"
      for packagePrefix in knownPhase4Packages:
          if className.startsWith(packagePrefix):
              return true
      return false  // 不属于任何已知 Phase 4 模块，按基础设施异常处理

  // knownPhase4Packages 在 AbstractCapabilityExecutor 中以 static final Set<String> 定义：
  static final Set<String> knownPhase4Packages = Set.of(
      "com.aimedical.modules.diagnosis",
      "com.aimedical.modules.inspection",
      "com.aimedical.modules.labtest",
      "com.aimedical.modules.image",
      "com.aimedical.modules.examination",
      "com.aimedical.modules.execution"
  )
  ```
  3. **最终回退**：两阶段均未识别为业务异常时，按基础设施异常处理（走降级路径）。Phase 4 模块全部完成基类继承改造后，过渡期回退代码可移除（移除标记为 `@Deprecated`，留待 Phase 6 清理）
- `getErrorCode()` 为可选方法。若模块异常类不存在此方法，薄适配器 catch 块回退到异常类名作为错误码（如 `"PHASE4_DiagnosisException"`），通过 `e.getClass().getSimpleName()` 获取
- **验证计划与时间线**：3 项 △ 需验证的模块（ImageAnalysisException、ExaminationException、ExecutionOrderException）的验证由底座侧在跨包协作会议前 1 周发起 Code Review 工单，逐模块检查 `getErrorCode()` 存在性。验证方式：检查各模块异常类源代码（或 JAR 反编译），确认是否存在 `public String getErrorCode()` 方法或继承自公共基类的同名方法。验证结果记入 §3.1 异常契约表，"△ 需验证"替换为 "✓ 已有"（存在）或 "✗ 缺失"（不存在）。底座切流 P0 上线前必须完成此项验证，验证结论由底座侧和 Phase 4 模块侧双签确认
- **底座 P0 上线前的错误码格式一致性保障**：即使验证完成后全部 6 个模块均存在 `getErrorCode()`，底座的错误码聚合消费方（监控大盘、告警规则、指标报表）仍需建立防御性解析策略——在读取 `error_code` 字段时，按 `PHASE4_` 前缀分组聚合而非按具体错误码值匹配。此策略确保即使个别模块的错误码格式与底座约定存在细微偏差（如包含下划线/连字符/hash 前缀），聚合维度不中断

**Phase 4 业务服务注入方式**：
6 个薄适配器型 CapabilityExecutor 各自通过构造器注入对应的 Phase 4 业务服务。依赖获取方式如下：

```
// 示例：DiagnosisCapabilityExecutor 注入 Phase 4 的诊断服务
// Phase 4 业务服务接口归属其原有模块（com.aimedical.modules.diagnosis），ai-impl 在 Maven pom.xml 中声明对 Phase 4 各业务模块的 provided 依赖
// 使用 provided 作用域的理由：(1) 避免 ai-impl 对 Phase 4 业务模块产生编译期强耦合；(2) 运行时 Phase 4 业务模块的 JAR 已部署在同一 Spring Boot 容器中，ClassLoader 可正常加载。
// ⚠️ Spring Boot uber-JAR 部署中 provided 作用域的运行时风险：spring-boot-maven-plugin 默认不将 provided 作用域的依赖打包进 uber-JAR，
// 因此依赖的 Phase 4 业务模块 JAR 必须显式作为外部依赖部署（如放置在容器 lib/ 目录或通过 java -cp 手动指定）。
// 若项目使用 `spring-boot-maven-plugin` 的 `layers` 模式或 `repackage` 打包，需在构建配置中通过 `excludeGroupIds`/`executable` 或
// 显式添加 Phase 4 业务模块为 `compile` 作用域的运行时依赖来确保类加载正确性。
// 若运行期出现 ClassNotFoundException，回退方案为改为 compile 作用域 + 在 §7 设计决策表中记录耦合约束的附加决策理由。
@Component
@ConditionalOnProperty(name = "ai.platform.enabled", havingValue = "true")
class DiagnosisCapabilityExecutor extends AbstractCapabilityExecutor<DiagnosisRequest, DiagnosisResponse> {
    private final DiagnosisService diagnosisService;  // Phase 4 已有接口

    // 薄适配器构造器签名：仅注入实际使用的依赖，不注入 PromptTemplateManager/ModelRouter/LlmChatService/StructuredOutputParser/ModelEndpointHealthManager
    // 这些基础设施组件由 Phase 4 服务内部自行管理，底座薄适配器不直接调用
    // 降级策略通过 degradationStrategyMapRef AtomicReference 统一注入，execute() 时每轮调用
    // 通过 .get() 获取最新策略 Map 快照，确保运行时 degradation.strategies 热替换生效
    DiagnosisCapabilityExecutor(
            DiagnosisService diagnosisService,        // Phase 4 业务服务（必须）
            AiMetricsCollector metricsCollector,      // 指标采集（必须）
            SlidingWindowMetricsStore metricsStore,   // 滑动窗口（必须）
            AtomicReference<Map<String, List<DegradationStrategy>>> degradationStrategyMapRef,  // 降级策略 AtomicReference（必须）
            @Autowired(required = false) LocalRuleFallback<DiagnosisRequest, DiagnosisResponse> localRuleFallback,  // 本地规则降级（可选）
            @Qualifier("capabilityTimeoutConfig") Map<String, Duration> capabilityTimeoutConfig,  // 超时配置（必须）
            @Qualifier("parseTimeoutConfig") Map<String, Duration> parseTimeoutConfig,  // 解析超时配置（必须）
            @Value("${ai.execution.timeout.parse.default:5s}") Duration parseTimeoutDefault,  // 解析超时默认值（必须）
            @Value("${ai.execution.timeout.thin-adapter-default:30s}") Duration thinAdapterTimeout,  // 薄适配器默认超时（必须）
            @Qualifier("thinAdapterPerCapabilityConfig") Map<String, Duration> thinAdapterPerCapabilityConfig,  // 每能力超时覆盖（必须）
            @Autowired ObjectMapper objectMapper) {  // 防御性拷贝（必须）
        super(null, null, null, null, null,
              metricsCollector, metricsStore, null, degradationStrategyMapRef, localRuleFallback,
              capabilityTimeoutConfig, parseTimeoutConfig, parseTimeoutDefault, thinAdapterTimeout, thinAdapterPerCapabilityConfig, objectMapper);
        this.diagnosisService = diagnosisService;
    }
}
```

**其余 5 个薄适配器子类的构造器约束**：`AnalysisReportForInspectionCapabilityExecutor`、`AnalysisReportForLabTestCapabilityExecutor`、`ImageAnalysisCapabilityExecutor`、`RecommendExaminationCapabilityExecutor`、`RecommendExecutionOrderCapabilityExecutor` 的构造器签名与 `DiagnosisCapabilityExecutor` 完全一致（16 参数），同样注入对应的 Phase 4 业务服务接口及相同的底座基础设施依赖，`super()` 调用前 5 个参数传递 `null`（`inputType`=null + 4 个底座管线组件=null），后 11 个参数与父类构造器签名逐一对应。由于篇幅原因本文仅展示 `DiagnosisCapabilityExecutor` 完整示例，其余 5 个子类构造器按此模式实现。

**条件注册保护**：全部 13 个 CapabilityExecutor 均通过 `@Component` 自注册到 Spring 容器，同时标注 `@ConditionalOnProperty(name = "ai.platform.enabled", havingValue = "true")`，确保 `ai.platform.enabled=false` 时底座关闭，所有 CapabilityExecutor 不被实例化。完整管线型按 `@PostConstruct` 扫描 `List<CapabilityExecutor>` 自动构建映射表（见 §3.1 映射机制），薄适配器型在底座关闭时避免因 Phase 4 业务服务未加载导致容器启动失败。

**依赖隔离说明**：薄适配器直接依赖 Phase 4 模块的 service 接口，不在 ai-api 中额外定义 SPI。此设计基于以下考量：(1) Phase 4 服务接口的定义权归属各业务模块，包 G 作为消费方直接引用；(2) 薄适配器仅做一层委托转发，不引入多态替换需求，额外 SPI 层无法带来解耦收益但增加接口维护成本；(3) Maven 依赖为出向依赖（ai-impl → Phase 4 modules），不影响包 G 内部的模块分层。若后续 Phase 6 需要将某项能力从薄适配器升级为底座完整管线，只需将对应 CapabilityExecutor 实现从委托 Phase 4 服务改为调用底座管线组件（ModelRouter + LlmChatService），替换 Maven 依赖方向，CapabilityExecutor 接口本身不变。

**departmentId 提取策略**：`departmentId` 的提取在 `AbstractCapabilityExecutor.execute()` 入口处（`supplyAsync()` 之前）调用 `doExtractDepartmentId(request)` 完成，此时所在线程为 Tomcat 容器线程，Spring `RequestContextHolder` 上下文可用。Phase 4 薄适配器型 CapabilityExecutor 的 `request` DTO 尚未继承 `AiRequestBase` 基类，无 `getDepartmentId()` 方法，故重写 `doExtractDepartmentId()` 从 `RequestContextHolder` 或 HTTP `X-Department-ID` Header 中获取科室标识。若无法获取则 departmentId 保持 null，对应字段在 `AiCallRecord` 中可空。Phase 5 底座能力的 DTO 已继承 `AiRequestBase`，`doExtractDepartmentId()` 默认实现直接从 `request.getDepartmentId()` 返回，不依赖 RequestContextHolder。

薄适配器的模板方法特化伪代码：
```
// 辅助方法契约定义（Phase 4 DTO 字段提取）
// 以下三个方法由薄适配器 doExtractDepartmentId()/doExtractVisitId()/doExtractPatientId() 在非 HTTP 场景下调用，
// 通过反射或 instanceof 判断 DTO 是否有对应 getter，无则返回 null。
// 签名及行为：
//   String extractDepartmentIdFromDto(T request):
//     从 DTO 中尝试提取科室标识。优先通过 DTO 已知字段名（如 getDepartmentId()、getDeptId()、getDeptCode()）反射调用；
//     若 DTO 无对应 getter（当前 Phase 4 DTO 均为空类），返回 null。
//     由薄适配器子类在 doExtractDepartmentId() 中调用，调用前已确认 RequestContext 不可用。
//   String extractVisitIdFromDto(T request):
//     同上述策略，提取就诊标识（getVisitId()、getVisitNo()、getClinicId() 等），无匹配时返回 null。
//   String extractPatientIdFromDto(T request):
//     提取患者标识（getPatientId()、getPatientNo() 等），无匹配时返回 null。
// 返回 null 时调用方（doExtractXxx()）保持 null 并在后续管线中由 AiCallRecord 的空值安全策略处理。
// ⚠️ 临时 fallback：当 DTO 为已知空类（当前 Phase 4 DTO 均为空类，§3.1 有记录）时，
// 上述方法均返回 null。在 Phase 4 DTO 完成业务字段补齐前，非 HTTP 场景下所有字段提取方法均返回 null，
// 薄适配器 doExecuteInternal() 将跳过委托调用直接降级。
// 降级原因: DegradationReason.INFRASTRUCTURE_ERROR + ":Phase4DtoEmpty"

// ThinAdapterCapabilityExecutor — Phase 4 DTO 公共字段提取策略
// Phase 4 的 6 项能力的 DTO 尚未继承 AiRequestBase（参见 §3.5 过渡策略），因此 visitId/patientId/sessionId/departmentId 无法通过 DTO getter 获取。
// 提取策略：departmentId 通过 doExtractDepartmentId() 从 RequestContext / HTTP Header 独立提取；
// visitId/patientId/sessionId 同样从 RequestContext / HTTP Header 独立提取，或者在 execute() 入口处
// 通过 SecurityContext / RequestContext 获取当前就诊上下文。

// ThinAdapterCapabilityExecutor
// — 重写 departmentId 提取方式（同 doExtractDepartmentId），额外提取 visitId/patientId/sessionId
// 注意：此方法在 execute() 入口处（supplyAsync 之前，容器线程）调用，RequestContextHolder 可用
// 与 §3.10 非 HTTP 回退策略一致：HTTP 场景优先从 RequestContext 提取，非 HTTP 场景回退到 request 对象中由调用方显式传递的对应字段
doExtractDepartmentId(request):
    // Phase 4 DTO 尚未继承 AiRequestBase，从 RequestContext / HTTP Header 独立提取
    departmentId = RequestContextUtils.extractFromRequestContext("X-Department-ID")
    if departmentId == null:
        // 非 HTTP 场景（MQ/定时任务）下 RequestContext 不可用，回退到 request 对象中由调用方显式传递的对应字段
        // 具体实现根据 Phase 4 DTO 的实际类型决定提取方式：若 DTO 有 getDepartmentId() 方法则优先使用，
        // 否则尝试通过反射或通用字段映射获取；若均不存在则返回 null（AiCallRecord 中 departmentId 可空）
        // ⚠️ 临时 fallback：当 DTO 为已知空类（当前 Phase 4 DTO 均为空类，§3.1 有记录）时，
        // extractDepartmentIdFromDto() 等效返回 null。在 Phase 4 DTO 完成业务字段补齐前，
        // 非 HTTP 场景下所有字段提取方法均返回 null，薄适配器 doExecuteInternal() 将跳过委托调用直接降级
        // （降级原因 DegradationReason.INFRASTRUCTURE_ERROR + ":Phase4DtoEmpty"）。
        // 跨包协作会议最晚截止时间和 DTO 改造最小验收标准见 §3.1 "Phase 4 服务接口契约定义"段。
        departmentId = extractDepartmentIdFromDto(request)
    return departmentId

doExtractVisitId(request):
    // Phase 4 DTO 无 getVisitId()，从 RequestContext / HTTP Header / 当前就诊上下文独立提取
    visitId = RequestContextUtils.extractFromRequestContext("X-Visit-ID")
    if visitId == null:
        visitId = extractVisitIdFromDto(request)  // 非 HTTP 场景回退到 request 显式字段
    return visitId

doExtractPatientId(request):
    // Phase 4 DTO 无 getPatientId()，从 RequestContext / HTTP Header / 当前就诊上下文独立提取
    patientId = RequestContextUtils.extractFromRequestContext("X-Patient-ID")
    if patientId == null:
        patientId = extractPatientIdFromDto(request)  // 非 HTTP 场景回退到 request 显式字段
    return patientId

// ThinAdapterCapabilityExecutor.doExecuteInternal() — 薄适配器特化管线
// departmentId/callerRole/callerId 从 execute() 入口处传入（已在容器线程提取，线程安全）
// visitId/patientId/sessionId 在 execute() 入口处通过独立提取方法获取后传入
// inputSummary 由 execute() 入口处定义并以参数形式传入
doExecuteInternal(startTime, request, capabilityId, departmentId, userId, sessionId, callerRole, callerId,
                   visitId, patientId, inputSummary):
    try {
      // 薄适配器委托调用：使用独立的超时控制
      // phase4ServiceDelegate.execute(request) 在 Phase 4 服务内部可能包含多个重试步骤（如 3 次重试每次30秒可达 90 秒），
      // 底座无法控制其内部行为，故引入独立超时阈值，超时后降级
      // 使用公共 ForkJoinPool.commonPool()（默认线程池）而非 llmCallExecutor，避免嵌套提交到同一线程池产生的排队死锁风险
      // ⚠️ 阻塞等待约束：本方法运行在 llmCallExecutor 线程池的 Worker 线程中，对 ForkJoinPool.commonPool() 提交任务后通过
      // delegateFuture.get() 阻塞等待结果。此模式潜在线程饥饿风险——当 llmCallExecutor 线程池的活跃线程全部处于
      // ForkJoinPool 阻塞等待状态（核心线程 = 可用模型端点数，默认不超过 CPU 核数），且 ForkJoinPool 的并行度不足时，
      // 未完成的 Phase 4 委托任务可能因缺乏调度线程而得不到执行，形成"线程耗尽→无法推进→持续阻塞"循环。
      // 约束条件：(1) 薄适配器并发度 ≤ llmCallExecutor 核心线程数，避免所有 Worker 线程同时阻塞；
      // (2) ForkJoinPool.commonPool() 并行度（默认 = Runtime.availableProcessors() - 1）应大于薄适配器峰值并发数；
      // (3) 若 Phase 4 服务内部也依赖同一 ForkJoinPool（如使用 parallelStream），则需隔离到独立线程池；
      // (4) thinAdapterTimeout 不宜过短（低于 Phase 4 服务 P99 响应时间），否则频繁超时将放大阻塞效应。
      // 若生产环境出现线程饥饿指标（llmCallExecutor 队列堆积且活跃线程全部阻塞），应考虑将薄适配器改为
      // phase4ServiceDelegate 直接调用（同步阻塞当前 llmCallExecutor 线程，避免跨线程池阻塞），或为薄适配器
      // 引入独立线程池隔离（参考 §6.1 指标采集线程池隔离做法）。
      CompletableFuture<R> delegateFuture = CompletableFuture.supplyAsync(
          () -> phase4ServiceDelegate.execute(request))
      result = delegateFuture.get(thinAdapterTimeout.toMillis(), TimeUnit.MILLISECONDS)
    } catch (TimeoutException e):
      // 薄适配器委托超时：走降级路径
      // 注意：CompletableFuture.cancel(true) 仅标记 Future 为 cancelled 状态，不会产生线程中断或任务取消效果，
      // Phase 4 服务将在后台继续执行至完成（资源消耗由 Phase 4 服务内部自行管理）。底座通过 WARN 日志记录此场景，
      // 便于运维侧评估薄适配器超时导致的资源浪费——若生产环境此类 WARN 高频出现，表明 thinAdapterTimeout 阈值过紧
      // 或 Phase 4 服务性能退化，需在运行态监控大盘中配置针对 Phase 4 委托超时的告警规则
      log.warn("ThinAdapter 委托超时: capabilityId={}, thinAdapterTimeout={}ms, Phase 4 服务将在后台继续执行", capabilityId, thinAdapterTimeout.toMillis())
      // sentinelReason=null（薄适配器不经过实验分流，无实验分组上下文）
      return doDegrade(startTime, DegradationReason.TIMEOUT + ":ThinAdapterTimeout", request, capabilityId, departmentId, callerRole, callerId, visitId, patientId, sessionId, inputSummary, null, null, null, null)
    } catch (Exception e):
      // 拆解 ExecutionException（CompletableFuture.get() 包装的原始异常）
      originalCause = (e instanceof java.util.concurrent.ExecutionException) ? e.getCause() : e
      originalType = originalCause.getClass().getSimpleName()
      // 按 §3.1 Phase 4 模块异常契约区分业务异常与基础设施异常：
      //   - 6 项 Phase 4 模块的业务异常统一继承 `Phase4BusinessException` 公共基类
      //     （归属 ai-api/dto/base/，新增抽象类，Phase 4 模块按其继承）
      //   - 薄适配器优先通过 instanceof 检测，消除字符串匹配在异常类重命名时的维护脆弱性
      //   - ⚠️ 过渡期回退：过渡期内（Phase 4 模块尚未完成基类继承改造时），
      //     instanceof Phase4BusinessException 将返回 false，此时回退到 §3.1 已定义的
      //     "异常类名回退"机制——通过异常类全名中是否含已知 Phase 4 模块包路径前缀
      //     （如 "com.aimedical.modules.diagnosis"、"com.aimedical.modules.inspection" 等）
      //     来判定是否为业务异常；若仍无法判定则按基础设施异常处理
      phase4BusinessExceptionDetected = (originalCause instanceof Phase4BusinessException)
      if !phase4BusinessExceptionDetected:
          // 过渡期回退检查：复用异常类名回退机制
          // 检查 originalCause 的包路径是否属于已知 Phase 4 模块
          phase4BusinessExceptionDetected = isKnownPhase4BusinessException(originalCause)
      if phase4BusinessExceptionDetected:
          // Phase 4 业务异常（如参数校验失败、数据不存在）：直接返回失败，不走降级路径
          // 标准化错误码：优先使用 Phase 4 业务异常中自带的错误码，若不存在则回退到异常类名
          elapsedMs = System.currentTimeMillis() - startTime
          errorCode = "PHASE4_" + originalType
          metricsCollector.record(AiCallRecord.failure(capabilityId, LocalDateTime.now(), elapsedMs, errorCode, originalCause.getMessage(), departmentId, inputSummary, visitId, patientId, sessionId, callerRole, callerId, null))
          slidingWindowMetricsStore.recordFailure(capabilityId)
          return AiResult.failure(errorCode, originalCause.getMessage())
      else:
        // 基础设施异常（网络超时、服务不可用）：走降级路径（使用父类 doDegrade 方法）
        // 拆解 ExecutionException 确保降级原因记录真实异常类型而非统一的 ExecutionException
        // sentinelReason=null（薄适配器不经过实验分流，无实验分组上下文）
        return doDegrade(startTime, DegradationReason.INFRASTRUCTURE_ERROR + ":" + originalType, request, capabilityId, departmentId, callerRole, callerId, visitId, patientId, sessionId, inputSummary, null, null, null, null)

     elapsedMs = System.currentTimeMillis() - startTime
     outputSummary = StringUtils.truncate(result.toString(), 500)
     // ⚠️ 薄适配器 retryCount 限制：此处硬编码 retryCount=0，因为 Phase 4 服务的内部重试行为对底座不可见。
     // Phase 4 服务接口（如 DiagnosisService.execute()）当前未暴露重试计数或调用元数据，底座无法感知其内部重试次数。
     // 此限制的影响：(1) 底座指标系统无法区分"首次调用成功"与"重试 3 次后成功"，薄适配器成功调用的 retryCount 恒为 0；
     // (2) 降级判定（熔断器/超时策略）不受此限制影响——降级判定基于底座视角的超时/异常结果，不依赖 retryCount 字段。
     // 若后续 Phase 4 服务接口升级暴露重试元数据，AiCallRecord.success() 调用中的 0 应替换为从 Phase 4 响应中提取的重试计数
     metricsCollector.record(AiCallRecord.success(capabilityId, LocalDateTime.now(), elapsedMs, departmentId, null, 0, null, null, inputSummary, outputSummary, visitId, patientId, sessionId, callerRole, callerId, null))
     slidingWindowMetricsStore.recordSuccess(capabilityId, elapsedMs)
     return AiResult.success(result)
```

**为何使用泛型 interface + 独立实现**：
- 13 项能力输入/输出类型各不相同，泛型 `T` / `R` 使 `execute()` 方法签名类型安全，避免 Object 强制转型
- 独立实现使每个能力的降级预检、模板变量映射、输出解析逻辑可独立定制与单元测试
- 新增 AI 能力只需新增一个 `CapabilityExecutor` 实现即可自动注册到 `AiOrchestrator` 的映射表中

#### `AbstractCapabilityExecutor<T, R>` — 能力执行器抽象骨架（abstract class，归属 `ai-impl/orchestrator/`）

**职责**：作为 13 个 `CapabilityExecutor` 实现的公共抽象基类，封装降级预检和指标采集等所有实现均需执行的公共步骤，子类仅需特化管线中的差异化步骤。

**模板方法模式**：
```
AbstractCapabilityExecutor.execute(request, capabilityId):
  // 在 supplyAsync 之前提取 ThreadLocal 上下文，避免在线程池中丢失
  startTime = System.currentTimeMillis()
  auth = SecurityContextHolder.getContext().getAuthentication()
  userId = auth != null ? auth.getName() : "SYSTEM"
  departmentId = doExtractDepartmentId(request)    // 容器线程上调用，ThreadLocal 可用
  visitId = doExtractVisitId(request)              // base 从 AiRequestBase 获取，thin adapter 从 RequestContext 获取
  patientId = doExtractPatientId(request)          // 同上
  sessionId = doExtractSessionId(request)          // 同上
  callerRole = extractCallerRole()
  callerId = extractCallerId()

  // 防御性拷贝：将拷贝结果存入新局部变量 defensiveCopy，使原始 request 变量保持 effectively final，确保 lambda 可捕获
  // ⚠️ 防御性拷贝失败回退：不可变 DTO 未标注 Jackson 注解时 convertValue 抛出异常，
  // 捕获后回退到原始 request（接受 request 可能被下游修改的风险），日志 WARN 并建议补全注解
  defensiveCopy = null
  try:
    defensiveCopy = objectMapper.convertValue(request, request.getClass())
  catch (Exception e):
    log.warn("防御性拷贝失败: capabilityId={}, requestType={}, 回退到原始 request, error={}",
        capabilityId, request.getClass().getSimpleName(), e.getMessage())
    defensiveCopy = request

  // 预先计算 inputSummary（降级路径和正常管线均需使用，以参数形式传入 doDegrade 和 doExecuteInternal）
  inputSummary = defensiveCopy != null ? StringUtils.truncate(defensiveCopy.toString(), 500) : null

  // 降级预检（移至 supplyAsync 之前，容器线程执行，熔断器 OPEN 状态的请求无需排队等待线程池）
  context = slidingWindowMetricsStore.buildDegradationContext(capabilityId, this.getClass().getSimpleName())
  context.setDepartmentId(departmentId)
  // 运行时从 AtomicReference 每轮获取最新策略 Map 快照，支持 degradation.strategies 热加载
  Map<String, List<DegradationStrategy>> currentStrategyMap = this.degradationStrategyMapRef.get()
  for each strategy in currentStrategyMap.getOrDefault(capabilityId, Collections.emptyList()) (sorted by getOrder() asc):
    if strategy.shouldDegrade(context):
      // 预检降级路径中 promptVersion=null（降级发生在实验分流之前，无实验分组上下文）
      // modelId=null（降级发生在模型路由之前，modelId 不可用）
      // sentinelReason=null（降级发生在实验分流之前，无实验分组上下文）
      // 使用 DegradationReason.STRATEGY_TRIGGERED 拼接策略类名，与枚举体系一致
      return CompletableFuture.completedFuture(doDegrade(startTime, DegradationReason.STRATEGY_TRIGGERED + ":" + strategy.getClass().getSimpleName(), defensiveCopy, capabilityId, departmentId, callerRole, callerId, visitId, patientId, sessionId, inputSummary, null, null, null, null))

  // 正常请求入线程池执行
  future = CompletableFuture.supplyAsync(() -> {
    return doExecuteInternal(startTime, defensiveCopy, capabilityId, departmentId, userId, sessionId, callerRole, callerId, visitId, patientId, inputSummary)
  }, llmCallExecutor)

  // 整体管线端到端超时兜底（各能力独立配置，默认 60 秒）
  capabilityTimeout = capabilityTimeoutConfig.getOrDefault(capabilityId, Duration.ofSeconds(60))
  return future.orTimeout(capabilityTimeout.toMillis(), TimeUnit.MILLISECONDS)
    .exceptionally(ex -> {
      if (ex instanceof TimeoutException):
        log.warn("CapabilityExecutor 整体执行超时: capabilityId={}, timeout={}ms", capabilityId, capabilityTimeout.toMillis())
        // 超时降级路径中 promptVersion=null（整体超时可能发生在实验分流完成前或后，但超时场景下 promptVersion 来源不可靠，统一传 null）
        // modelId/sentinelReason 在超时场景下来源不可靠，统一传 null
        return doDegrade(startTime, DegradationReason.TIMEOUT, defensiveCopy, capabilityId, departmentId, callerRole, callerId, visitId, patientId, sessionId, inputSummary, null, null, null, null)
      throw new CompletionException(ex)
    })

// 子类需实现的步骤
abstract doExecuteInternal(startTime, request, capabilityId, departmentId, userId, sessionId, callerRole, callerId, visitId, patientId, inputSummary):
    // 模板渲染（完整管线）/ 直接委托（薄适配器）
    // 实验分流（完整管线）/ 跳过（薄适配器）
    // 模型路由
    // LLM 调用
    // 结果解析
    // 指标采集（通过参数接收的 callerRole/callerId/inputSummary 构建 AiCallRecord）

// 标准管线步骤（protected final，供子类在 doExecuteInternal() 中调用）
// 封装实验分流→模板渲染→模型路由→LLM 调用→结构化解析→指标采集的完整步骤
// 解决 DiscussionConclusionCapabilityExecutor 等需要前置阶段（如压缩摘要）的子类
// 无法通过 super.doExecuteInternal() 调用父类抽象方法的问题。
// 子类在完成前置阶段后调用此方法继续标准流程。
// ⚠️ 此方法声明为 final 防止子类重写覆盖标准管线行为。
protected final executeStandardPipeline(startTime, request, capabilityId, departmentId, userId, sessionId, callerRole, callerId, visitId, patientId, inputSummary, variables, promptVersion, sentinelReason):
    // 标准 doExecuteInternal 核心逻辑：模板渲染→模型路由→LLM 调用→结构化解析→指标采集
    // 完整伪代码见 §4.1 AbstractCapabilityExecutor.doExecuteInternal() 标准实现
    // DiscussionConclusionCapabilityExecutor 在完成前置压缩后以此方法复用标准管线

// 变量提取（子类可选重写——默认使用 ObjectMapper.convertValue 方式 A）
extractVariables(request):
    // 默认：ObjectMapper.convertValue(request, Map.class)
    // 复杂场景（字段变换、拼接、条件过滤）子类重写自定义逻辑

// 子类可选重写——departmentId 提取方式
doExtractDepartmentId(request):
    // 默认：从 AiRequestBase.getDepartmentId() 获取
    // 薄适配器重写：从 RequestContext 独立提取

// 子类可选重写——visitId 提取方式
doExtractVisitId(request):
    // 默认：从 AiRequestBase.getVisitId() 获取
    // 薄适配器重写：从 RequestContext / HTTP Header 独立提取

// 子类可选重写——patientId 提取方式
doExtractPatientId(request):
    // 默认：从 AiRequestBase.getPatientId() 获取
    // 薄适配器重写：从 RequestContext / HTTP Header 独立提取

// 子类可选重写——sessionId 提取方式
doExtractSessionId(request):
    // 默认：从 AiRequestBase.getSessionId() 获取
    // 薄适配器重写：从 RequestContext / HTTP Header 独立提取

// 超时原因细化（子类可选重写——默认返回 DegradationReason.TIMEOUT）
// DiscussionConclusionCapabilityExecutor 通过此方法区分超时原因：
// - transcriptSummaryElapsedMs > capabilityTimeout * 0.8 → ":transcriptSummaryCrowding"
// - 否则 → ":primaryLlmTimeout"
// elapsedInDoExecuteInternal 由 doExecuteInternal() 在 supplyAsync lambda 入口处赋值（System.currentTimeMillis() - startTime），
// 确保 exceptionally() 回调可获取管线已消耗时间。
// 该字段在 execute() 的 supplyAsync lambda 中被捕获（声明为 volatile long 保证可见性），
// doExecuteInternal() 作为子类实现可向其赋值。
refineTimeoutReason(capabilityId, elapsedInDoExecuteInternal, request):
    return DegradationReason.TIMEOUT

// 输出摘要提取（子类可选重写——默认使用 StringUtils.truncate(result.toString(), 500)）
// doExecuteInternal() 中的 parsedResult 和 doDegrade() 中的 localRuleFallback.fallback() 返回值
// 均通过此默认方式提取摘要，子类可按需重写以提取结构化输出中的关键字段而非整体 toString()
extractOutputSummary(result):
    return StringUtils.truncate(result.toString(), 500)

// 辅助方法——提取调用方角色（统一委托至 RequestContextUtils 的唯一规范实现）
// 原实现中的 ROLE_ 前缀过滤逻辑已迁移至 RequestContextUtils.extractCallerRole()，
// 此处只保留委托调用，确保 AiOrchestrator.handle() catch 块与 AbstractCapabilityExecutor
// 使用同一份 ROLE_ 前缀过滤逻辑，消除 callerRole 值格式不一致问题。
extractCallerRole():
    return RequestContextUtils.extractCallerRole()

// 辅助方法——提取调用方标识（统一委托至 RequestContextUtils 的唯一规范实现）
extractCallerId():
    return RequestContextUtils.extractCallerId()
```

**为何使用 abstract class 而非 interface**：子类共享降级预检循环、指标采集和 `doDegrade()` 方法等公共实现代码，abstract class 提供实现复用。同时声明 `execute()` 为模板方法（`final` 以防止子类重写跳过降级预检），仅暴露 `doExecuteInternal()` 给子类特化，确保降级预检不可绕过。

**子类分类**：
- **完整管线子类**（7 项底座能力）：`TriageCapabilityExecutor` 等——特化 `doExecuteInternal()` 实现模板渲染→实验分流→模型路由→LLM 调用→结果解析→指标采集全流程
- **薄适配器子类**（6 项 Phase 4 能力）：`DiagnosisCapabilityExecutor` 等——特化 `doExecuteInternal()` 实现降级预检→直接委托 Phase 4 业务服务→指标采集简化流程

**薄适配器构造器简化**：薄适配器 CapabilityExecutor 的构造器签名与完整管线子类不同，仅注入 Phase 4 Service、`AiMetricsCollector`、`SlidingWindowMetricsStore`、`List<DegradationStrategy>`、`LocalRuleFallback`，不注入 `PromptTemplateManager`、`ModelRouter`、`LlmChatService`、`StructuredOutputParser`、`ModelEndpointHealthManager` 等薄适配器管线不使用的基础设施组件。`AbstractCapabilityExecutor` 构造器以 null 接收这些无用参数，并在 `doExecuteInternal()` 中对 null 引用做防御（跳过对应的管线步骤而非调用时 NPE）。此设计消除不必要的 Bean 依赖和内存开销，且避免底座关闭时因基础设施 Bean 未初始化导致的构造失败。

**整体管线超时机制**：
- `AbstractCapabilityExecutor.execute()` 通过 `CompletableFuture.orTimeout()` 为整体执行管线引入端到端超时兜底，超时后进入降级路径记录 `DegradationReason.TIMEOUT`
- 覆盖场景：`experimentManager.assign()`（JPA 查询）、`promptTemplateManager.render()`（数据库+渲染）、`extractVariables()`（Jackson 转换）、`llmChatService.chat()`（LLM HTTP 调用）等所有管线步骤
- 超时阈值按能力独立配置，存储在 `capabilityTimeoutConfig`（`Map<String, Duration>`），通过 `AiPlatformConfig` 从 YAML 绑定（见 §9.5 `execution.timeout.per-capability` 配置块），默认值 60 秒。`capabilityTimeoutConfig` 在 `AbstractCapabilityExecutor` 构造器中注入，注入方式见下方构造器伪代码。YAML 中的配置键为 `ai.execution.timeout.per-capability`。类图 §2.3 已补充该字段声明
- 薄适配器子类额外引入 `thinAdapterTimeout` 独立超时阈值（默认 30 秒），通过 `@Value("${ai.execution.timeout.thin-adapter-default:30s}")` 注入，在 `doExecuteInternal()` 中通过 `CompletableFuture.supplyAsync(() -> phase4ServiceDelegate.execute(request)).get(thinAdapterTimeout.toMillis(), TimeUnit.MILLISECONDS)` 包裹委托调用，超时后同样走降级路径
- 6 项薄适配器能力可各自覆盖默认超时值，通过在 YAML `ai.execution.timeout.thin-adapter.per-capability` 中按能力标识单独配置。未覆盖的能力使用 `thin-adapter-default` 值。此机制与 `per-capability` 整体超时配置类似，按能力独立差异化，适应各 Phase 4 服务的不同 P99 响应时间（如影像分析耗时高，需 45s；检查报告解析较快，20s 即可）
  - **超时层级关系**：完整管线场景下 `capabilityTimeout`（通过 `orTimeout()` 设置）与薄适配器场景下 `thinAdapterTimeout`（通过 `delegateFuture.get()` 设置）同时存在时，两超时值的关系定义如下：
  - **薄适配器场景**：`capabilityTimeout` 应设置为 `thinAdapterTimeout` + 合理缓冲值（+5 秒），确保内部 `delegateFuture.get(thinAdapterTimeout)` 优先触发并进入已定义的降级路径，外层 `orTimeout` 仅作为兜底保护。§9.5 配置中各薄适配器能力的 `per-capability` 值均比 `thin-adapter.per-capability` 对应值多 5 秒缓冲，遵循此层级约束。`per-capability` 必须 >= `thinAdapterTimeout`，推荐保持 +5 秒缓冲，防止两超时同时触发导致同一降级原因被误记两次
  - **完整管线场景**（无薄适配器）：`capabilityTimeout` 是唯一的端到端超时保护，覆盖模板渲染→实验分流→模型路由→LLM 调用的全流程，`orTimeout` 按配置值直接生效
  - **配置约束**：§9.5 YAML 中薄适配器能力的 `per-capability` 超时值添加注释说明其与 `thin-adapter.per-capability` 的层级关系，指导运维配置

**启动期配置校验**：在 `AiPlatformConfig` 的 `@PostConstruct` 阶段新增配置校验逻辑，对所有薄适配器能力逐一验证 `per-capability >= thin-adapter.per-capability + 5s`。校验不通过时抛出 `IllegalStateException`（启动期 fail-fast），日志输出冲突的配置项名称和值，避免运行期降级行为与设计意图不一致。

此外新增 `parseTimeout <= chatFallbackTimeout` 层级约束校验：对所有能力（含默认值）逐一验证 `parseTimeout <= capabilityTimeout * 0.4`（`chatFallbackTimeout` 为 `capabilityTimeout` 的 40%）。解析超时若大于 chat 回退窗口，降级原因将被误记为"解析超时"而非"chat 回退超时"。校验不通过时抛出 `IllegalStateException`，日志同时输出 `parseTimeout`、`capabilityTimeout` 和 `chatFallbackTimeout` 三个值以辅助运维诊断。
- **注入方式伪代码**（AbstractCapabilityExecutor 构造器，v4 迭代修正——移除 @Qualifier 策略注入，改为 Map 统一注入）：
  ```
   // capabilityTimeoutConfig 通过 AiPlatformConfig 的 @Bean("capabilityTimeoutConfig") 注入
   // AiPlatformConfig 中绑定 YAML 配置 ai.execution.timeout.per-capability 为 Map<String, Duration>
   // thinAdapterTimeout 通过 @Value 注入默认 30 秒，薄适配器子类构造器可覆盖
   // degradationStrategyMapRef 通过 AiPlatformConfig 的 @Bean("degradationStrategyMapRef") 注入
    // execute() 运行时每轮均通过 degradationStrategyMapRef.get() 获取最新策略 Map 快照，
    // 确保 §3.9「运行时配置热加载机制」中 degradation.strategies 的热替换生效
    // objectMapper 在构造器中注入，用于 execute() 入口处的防御性拷贝
    // （objectMapper.convertValue(request, request.getClass())）
    AbstractCapabilityExecutor(
        Class<T> inputType,
        PromptTemplateManager promptTemplateManager,
        ModelRouter modelRouter,
        LlmChatService llmChatService,
        StructuredOutputParser structuredOutputParser,
        AiMetricsCollector metricsCollector,
        SlidingWindowMetricsStore metricsStore,
        ModelEndpointHealthManager endpointHealthManager,
        AtomicReference<Map<String, List<DegradationStrategy>>> degradationStrategyMapRef,
        @Autowired(required = false) LocalRuleFallback<T, R> localRuleFallback,
        @Qualifier("capabilityTimeoutConfig") Map<String, Duration> capabilityTimeoutConfig,
        @Qualifier("parseTimeoutConfig") Map<String, Duration> parseTimeoutConfig,
        @Value("${ai.execution.timeout.parse.default:5s}") Duration parseTimeoutDefault,
        @Value("${ai.execution.timeout.thin-adapter-default:30s}") Duration thinAdapterTimeout,
        @Qualifier("thinAdapterPerCapabilityConfig") Map<String, Duration> thinAdapterPerCapabilityConfig,
        @Autowired ObjectMapper objectMapper) {
      this.capabilityTimeoutConfig = capabilityTimeoutConfig;
      this.parseTimeoutConfig = parseTimeoutConfig;
      this.parseTimeoutDefault = parseTimeoutDefault;
      this.thinAdapterTimeout = thinAdapterTimeout;
      this.thinAdapterPerCapabilityConfig = thinAdapterPerCapabilityConfig;
      this.degradationStrategyMapRef = degradationStrategyMapRef;
      this.objectMapper = objectMapper;
      // ... 其余字段注入
   }

   **⚠️ 构造器参数数量说明**：当前构造器包含 16 个参数（含 `inputType`），`doDegrade()` 方法包含 15 个参数，`AiCallRecord` 工厂方法包含 15~17 个参数。大量参数构成编码实施风险（参数顺序依赖、调用点复杂度）。**以下通过三期分阶段迁移计划引入 `CallContext` 值对象降维**：

   **`CallContext` 值对象**（归属 `ai-api/dto/base/`）：聚合以下 9 个业务上下文字段：
   ```
   class CallContext {
       String departmentId    // 科室标识
       String callerRole      // 调用方角色
       String callerId        // 调用方标识
       String visitId         // 就诊标识
       String patientId       // 患者标识
       String sessionId       // 会话标识
       String inputSummary    // 输入摘要
       String outputSummary   // 输出摘要（可空，降级路径由 fallback 填充，成功路径由管线填充）
       @Nullable Integer promptVersion  // Prompt 版本号（可空，A/B 实验使用）
   }
   ```
   `CallContext` 为不可变对象（所有字段 `final`，通过全参构造器或 Builder 赋值），提供 `withOutputSummary(String)` 和 `withPromptVersion(Integer)` 的复制方法。

   **三期分阶段迁移计划**：

   | 阶段 | 目标 | 具体工作 | 验收里程碑 |
   |------|------|---------|-----------|
   | **一期**（底座切流前完成） | 定义 `CallContext` 值对象 + `AiCallRecord` 工厂方法新旧签名共存 | (1) 在 `ai-api/dto/base/` 中新增 `CallContext` 类；(2) `AiCallRecord` 新增 3 个重载工厂方法（以 `CallContext` 替代 9 个上下文字段），旧多参数工厂方法保留（标记 `@Deprecated`）；(3) `doDegrade()` 保持旧 15 参数签名不变（不改动任何调用点）；(4) 新增 `AbstractCapabilityExecutor` 的 `buildCallContext()` 辅助方法，供各子类按需使用 | YAML 配置 `ai.call-context.enabled: true` 时新工厂方法可用；`mvn compile` 通过（旧调用点零修改） |
   | **二期**（底座切流后 — 切流后第 1 个月） | `doDegrade()` 引入 `CallContext` 重载签名，逐个能力迁移调用点 | (1) `doDegrade()` 新增 `CallContext` 重载版本（7 参数），旧 15 参数签名保留（标记 `@Deprecated`）；(2) 逐个 CapabilityExecutor 子类迁移本类的 `doDegrade()` 调用点到新签名——每次迁移一个子类即提 PR，验证 CI 通过；(3) `doExecuteInternal()` 同步迁移到 `CallContext` 重载版本；(4) `AiCallRecord` 工厂方法全部切至新签名 | 13 个 CapabilityExecutor 子类的所有 `doDegrade()`/`doExecuteInternal()` 调用点全部迁移完成；全量集成测试通过 |
   | **三期**（切流后第 2 个月） | 清理旧签名 | (1) 移除 `AiCallRecord` 旧的 15~17 参数工厂方法；(2) 移除 `doDegrade()` 旧 15 参数签名；(3) 移除 `doExecuteInternal()` 旧 11 参数签名；(4) 移除 `AbstractCapabilityExecutor` 构造器中不再需要的 `@Value` 注解字段（部分上下文字段由 `CallContext` 承载后可移除构造参数）；(5) 清理 `@Deprecated` 标记 | `mvn compile` 通过且零 `@Deprecated` 引用 |

   `CallContext` 引入后降维目标：`AiCallRecord` 工厂方法从 15~17 参数降维至 6 参数（`success/failure/degraded` 各自以 `callContext` 替代 9 个散列字段）；`doDegrade()` 从 15 参数降维至 7 参数；`doExecuteInternal()` 从 11 参数降维至 6 参数。

   **一期与现行文档的兼容性**：一期仅新增重载方法和 `CallContext` 类，不修改任何现有调用点。本文档 §4.1 伪代码和 §4.2 薄适配器伪代码中所有 `AiCallRecord`/`doDegrade()`/`doExecuteInternal()` 的旧多参数签名调用点保持不变——一期验收通过前与当前设计完全一致。二期开始时，本文档的伪代码段需同步更新为 `CallContext` 新签名。

### 3.2 模型对接层

#### `LlmChatService` — 大模型对话客户端接口（interface，归属 `ai-impl/client/`）

**职责**：封装与大语言模型交互的核心对话能力，包括同步对话与结构化输出。屏蔽 HTTP API 直接调用与 Spring AI ChatModel 两种底层接入方式的差异，为上层（`CapabilityExecutor`）提供一致的调用体验。

**关键行为契约**：
- `chat(LlmChatRequest)` → `CompletableFuture<AiResult<LlmChatResponse>>`：同步对话方法，输入消息列表与可选配置，输出模型响应文本与使用统计
- `structuredChat(LlmChatRequest, Class<T>)` → `CompletableFuture<AiResult<StructuredChatResult<T>>>`：结构化输出方法，强制模型输出符合给定 Java 类型的结构化数据，优先使用模型原生 tool_use / function_call / JSON mode 能力。返回值 `StructuredChatResult<T>` 同时包含解析后的 DTO（`data`）、LLM 内部重试计数（`retryCount`）与 Token 用量（`usage`），使 `CapabilityExecutor` 管线在成功路径下也能完整获取调用元数据。输入校验与 chat() 相同，目标类型 `Class<T>` 必须可由 Jackson 反序列化
- **异常分类**：`structuredChat()` 实现层应区分两类异常——(1) 模型不支持结构化输出时的 `StructuredOutputNotSupportedException`（`@throws` 文档标注），语义为"回退到 `chat()` + `StructuredOutputParser.parse()`"；(2) 基础设施异常（HTTP 5xx、连接超时等）的 `LlmInfrastructureException`（`@throws` 文档标注），语义为"直接降级，不尝试回退"。`CapabilityExecutor` 在 `doExecuteInternal()` 中根据异常类型分别处理。两异常类型均归属 `ai-impl/client/exception/` 包，继承 `RuntimeException`

**输入校验契约**：`LlmChatRequest.messages` 列表不得为 null 或空列表，至少包含一条消息；`role=SYSTEM` 消息至多一条且必须位于列表首位；违反时返回 `AiResult.failure("LLM_AI_INPUT_INVALID")`。

**与 CapabilityExecutor 的协作**：`CapabilityExecutor.doExecuteInternal()` 中构建 `LlmChatRequest`（将模板渲染结果作为 SYSTEM 消息、DTO 数据作为 USER 消息），调用 `llmChatService.chat()` 或 `structuredChat()`。结构化输出优先走 `structuredChat()`，模型原生不支持时回退到 `chat()` + `StructuredOutputParser.parse()`。

**为何使用 interface**：模型接入方式存在 HTTP API 和 Spring AI 两种异构实现，且未来可能新增 gRPC 或其他协议接入方式。`chat()` 与 `structuredChat()` 合并到同一接口，因为结构化输出是对标准对话的补充，多数 LLM 供应商在同一 API 端点上同时支持两种模式；独立接口会导致调用方在每次调用前按能力判断使用哪个接口，增加不必要的路由复杂度。

**客户端限流保护**：为保护对供应商端点的调用不超过其速率限制（如每分钟请求数上限、每分钟 Token 数上限），引入 `EndpointRateLimiter` 组件，在 `LlmChatService.chat()/structuredChat()` 调用之前、`CredentialProvider` 凭据获取之后执行限流检查。设计要点如下：
- **限流器类型**：令牌桶算法（`RateLimiter.create()` Guava 实现或自定义滑动窗口实现），按 `endpointId` 维度独立限流。每个端点维护一个令牌桶实例，key 为 `endpointId`
- **配置维度**：`ai.rate-limiting.endpoints.{endpointId}.{permits-per-second / max-burst-seconds / queue-wait-millis}`，YAML 配置示例见 §9.5
- **执行位置**：`LlmChatService` 实现类入口处按 `ModelRoute.endpointId` 获取对应的 `RateLimiter` 实例，调用 `tryAcquire(timeout, unit)` 等待令牌。若在 `queueWaitMillis` 内获取到令牌则放行调用；若超时则抛出 `RateLimitExceededException`，由 `CapabilityExecutor` 捕获后走基础设施异常降级路径（降级原因 `DegradationReason.INFRASTRUCTURE_ERROR + ":RateLimitExceeded"`）
- **线程安全**：`EndpointRateLimiter` 使用 `ConcurrentHashMap<String, RateLimiter>` 存储各端点限流器实例，Guava `RateLimiter` 本身线程安全
- **限流后的请求重试**：限流超时**不重试**（限流导致超时表明已到达瞬时容量上限，重试将加剧拥塞），直接降级。此行为有别于 HTTP 5xx 调用的 1 次重试策略

**组件状态**：`EndpointRateLimiter` 作为独立 `@Component` 归属 `ai-impl/client/`，通过构造器注入 `AiPlatformConfig` 的限流配置。`LlmChatService` 实现类在调用中通过 `endpointRateLimiter.tryAcquire(endpointId)` 完成限流检查。

**线程模型**：`LlmChatService` 本身**无状态，线程安全**。HTTP 客户端基于连接池实现，每次调用不持有端点级别可变状态。`chat()` 与 `structuredChat()` 均返回 `CompletableFuture`，由实现层自行决定同步/异步执行。`CapabilityExecutor` 通过 `supplyAsync()` 包装以统一异步边界，确保不阻塞 Tomcat 容器线程。

#### `LlmChatStreamService` — 大模型流式对话客户端接口（interface，归属 `ai-impl/client/`）

**职责**：封装与大语言模型交互的流式对话能力，返回 Reactor `Flux` 逐字输出。此接口独立于 `LlmChatService`，但因方法签名直接引用 `Flux<LlmChatResponse>`，reactor-core 为编译期强制依赖。Phase 6 流式病历输出通过本接口获取 `Flux` 并转换为 Server-Sent Events。

**关键行为契约**：
- `chatStream(LlmChatRequest)` → `Flux<LlmChatResponse>`：流式输出方法，返回 Reactor Flux 支持背压控制
- 输入校验约束与 `LlmChatService.chat()` 相同；违反约束时返回 `Flux.error(AiAbilityInputInvalidException)`

**为何独立于 `LlmChatService`**：
- `LlmChatService` 的方法签名不涉及 `Flux`，使非流式代码保持零 Reactor 依赖
- 独立接口使流式能力与同步管线职责分离，CapabilityExecutor 管线根据能力类型选择调用 `LlmChatService` 或 `LlmChatStreamService`
- ⚠️ **reactor-core 依赖说明**：`LlmChatStreamService` 方法签名直接返回 `Flux<LlmChatResponse>`，因此 reactor-core 为编译期强制依赖（参见 §8.2 依赖清单）。同步管线（`LlmChatService`）仍不依赖 reactor-core

#### `DelegatingLlmChatService` — LlmChatService 分发层实现（class，归属 `ai-impl/client/`）

**职责**：作为 `LlmChatService` 接口的统一入口实现，根据 `ModelRoute.clientType`（`HTTP_API` / `SPRING_AI`）将 `chat()` 和 `structuredChat()` 调用派发至对应的底层实现（`HttpApiLlmChatService` 或 `SpringAiLlmChatService`）。消除双实现 Bean 装配的二义性——`CapabilityExecutor` 仅依赖 `LlmChatService` 接口并注入 `DelegatingLlmChatService` 实例（标注 `@Primary`），`HttpApiLlmChatService` 和 `SpringAiLlmChatService` 作为非 `@Primary` 的具名 Bean 注册于容器，不直接对外暴露。

**分发机制**：
- `DelegatingLlmChatService` 内部持有 `Map<ClientType, LlmChatService> delegates`，由 `AiPlatformConfig` 在启动期初始化
- `chat(LlmChatRequest)` 从 `request.getClientType()` 获取目标客户端类型，从 `delegates` 查找对应实现并转发
- 若 `clientType` 为 null 或 `delegates` 中无对应实现，回退到默认实现（优先 `HttpApiLlmChatService`），日志 WARN
- **Spring AI 可选依赖保护**：`springAiLlmChatService` 标注 `@ConditionalOnClass(name = "org.springframework.ai.chat.model.ChatModel")`，对应 Spring AI 1.0.x 稳定版本的包路径。项目在 `pom.xml` 中锁定 `spring-ai-bom` 版本，消除版本兼容性检测需求。`@ConditionalOnClass` 的 `name` 属性在 Spring Boot 中使用 AND 语义（所有指定 class 必须同时存在），因此仅指定一个目标 class 名称以确保条件可准确匹配。**⚠️ 防呆说明**：此处使用单 class 名称而非数组。`@ConditionalOnClass.name` 数组的 AND 语义意味着数组中的每个 class 都必须同时存在条件才匹配，而 OR 语义的期望是"存在任意一个即匹配"——这在 Spring Boot 中不成立。若后续维护者误添加第二个 class 名称到数组（如意图 OR 语义），条件将变得更严格（两 class 必须同时存在），导致 Spring AI 注册条件意外收紧。因此显式声明为单 class 名称，使用 AND 语义精准匹配而非数组 OR 的假想语义。项目未引入 Spring AI 依赖时该 Bean 不被注册。`delegatingLlmChatService` 通过 `ObjectProvider<LlmChatService>` 注入 `springAiLlmChatService`，`getIfAvailable()` 返回 null 时 `delegates` 中仅包含 `HTTP_API` 分发项。此时若请求的 `clientType=SPRING_AI`，分发层按"无对应实现"回退逻辑处理（日志 WARN 后使用 `HttpApiLlmChatService` 作为默认实现），确保底座在无 Spring AI 环境下完全可用

**协作对象**：
- 实现 `LlmChatService`，被 `CapabilityExecutor` 通过 `LlmChatService` 接口引用
- 持有 `HttpApiLlmChatService` 和 `SpringAiLlmChatService` 引用
- 与 `ModelRoute.clientType` 协作——`CapabilityExecutor` 在 `doExecuteInternal()` 中构造 `LlmChatRequest` 时，将 `modelRoute.getClientType()` 设置到 `request.clientType` 字段，供 `DelegatingLlmChatService` 分发

**异常传播契约**：`DelegatingLlmChatService` 作为分发层，不进行异常包装或转换。底层实现（`HttpApiLlmChatService` / `SpringAiLlmChatService`）抛出的任何异常（包括 `StructuredOutputNotSupportedException`、`LlmInfrastructureException`、超时、连接异常等）均原样透传，不包装为 `LlmChatServiceException` 等统一异常类型。此契约的理由：(a) `CapabilityExecutor` 的异常处理已按异常分类（`StructuredOutputNotSupportedException` 回退、`LlmInfrastructureException` 降级、其余归为基础设施异常）设计，包装将破坏分类路由；(b) 所有 LLM 实现层异常已在内部捕获并转为 `CompletableFuture` 完成（通过 `CompletableFuture.completedFuture(AiResult.failure(...))`），仅在 `structuredChat()` 路径中的 `StructuredOutputNotSupportedException` 和 `LlmInfrastructureException` 以异常形式传播供回调处理，确保 `CapabilityExecutor` 仅在回调中处理异常。⚠️ **同步/异步双路径异常覆盖确认**：`CapabilityExecutor` 的同步路径（`execute()` 中降级预检阶段）和异步路径（`supplyAsync()` lambda 内部）均需覆盖此两类异常——同步路径异常被 `AiOrchestrator.handle()` catch 块兜底，异步路径异常被 `CompletableFuture.orTimeout().exceptionally()` 捕获。

**为何采用分发层而非 `@Qualifier`**：
- `CapabilityExecutor` 在运行时才能从 `ModelRouter.route()` 返回的 `ModelRoute` 中获知本次调用应使用哪个客户端实现（`HTTP_API` 或 `SPRING_AI`），编译期无法通过 `@Qualifier` 固定注入
- 分发层使 `CapabilityExecutor` 无需感知客户端实现选择逻辑，保持其对 `LlmChatService` 接口的单一依赖视图
- `HttpApiLlmChatService` 和 `SpringAiLlmChatService` 作为非 `@Primary` 的具名 Bean 注册，不影响 `LlmChatService` 接口的自动装配

**线程安全**：`Map<ClientType, LlmChatService>` 初始化后不变，读操作无竞争；底层 `LlmChatService` 实现自身线程安全。

**Bean 装配方式**：
```java
@Configuration
class AiPlatformConfig {
    @Bean("httpApiLlmChatService")
    LlmChatService httpApiLlmChatService() { return new HttpApiLlmChatService(credentialProvider, endpointRateLimiter); }

    @Bean("springAiLlmChatService")
    @ConditionalOnClass(name = "org.springframework.ai.chat.model.ChatModel")
    LlmChatService springAiLlmChatService() { return new SpringAiLlmChatService(...); }

    @Primary
    @Bean
    LlmChatService delegatingLlmChatService(
            @Qualifier("httpApiLlmChatService") LlmChatService httpApi,
            ObjectProvider<LlmChatService> springAiProvider) {
        // springAiLlmChatService 标注了 @ConditionalOnClass，项目未引入 Spring AI 时该 Bean 不存在
        // 使用 ObjectProvider.getIfAvailable() 避免强制注入导致的 NoSuchBeanDefinitionException
        Map<ClientType, LlmChatService> delegates = new HashMap<>();
        delegates.put(ClientType.HTTP_API, httpApi);
        LlmChatService springAi = springAiProvider.getIfAvailable();
        if (springAi != null) {
            delegates.put(ClientType.SPRING_AI, springAi);
        }
        return new DelegatingLlmChatService(delegates);
    }
}
```

#### `HttpApiLlmChatService` / `SpringAiLlmChatService` — 同步对话实现（class，归属 `ai-impl/client/`）

**职责**：`LlmChatService` 接口的同步实现。`HttpApiLlmChatService` 基于 HTTP API 直连（OkHttp / WebClient），`SpringAiLlmChatService` 基于 Spring AI `ChatModel` 抽象。两实现**仅实现 `LlmChatService`**，不实现 `LlmChatStreamService`，确保非流式使用方无需引入 `reactor-core` 编译期依赖。两实现均不标注 `@Component` 或 `@Primary`，作为 `AiPlatformConfig` 中的具名 `@Bean` 方法注册。

#### `HttpApiLlmChatStreamService` / `SpringAiLlmChatStreamService` — 流式对话实现（class，归属 `ai-impl/client/`）

**职责**：`LlmChatStreamService` 接口的流式实现。两实现**仅实现 `LlmChatStreamService`**，持有 `reactor-core` 依赖以返回 `Flux<LlmChatResponse>`。流式能力仅在 Phase 6 引入，此隔离确保 Phase 5 同步管线无需引入 `reactor-core`。

#### `ModelEndpointHealthManager` — 模型端点健康状态管理器（class，归属 `ai-impl/metrics/`）

**职责**：为每个模型端点（由 `ModelRoute.endpointId` 标识）维护独立的健康状态，供 `CapabilityExecutor` 在管线执行中、模型调用之前判定目标模型端点是否健康。`LlmChatService` 本身不持有此状态，仅做调用转发。

**状态模型**：
```
每个模型端点维护独立的状态：
  CONNECTED ←→ DEGRADED ←→ UNAVAILABLE
  CONNECTED ──────────────────→ UNAVAILABLE (直接跳转)

状态定义：
  - CONNECTED: 正常状态，调用直接发送
  - DEGRADED: 近窗口内连续 3 次调用耗时 > 阈值，启动降级（仍尝试调用但上报告警）
  - UNAVAILABLE: 连续 5 次调用失败（HTTP 5xx / 连接拒绝），不再发送调用，直接返回失败

状态转换表：
  | 起始状态 | 触发条件 | 目标状态 | 说明 |
  |---------|---------|---------|------|
  | CONNECTED | 连续 3 次调用耗时 > 阈值 | DEGRADED | 性能退化，仍尝试调用 |
  | CONNECTED | 连续 N 次（可配置，默认 5 次）调用失败 | UNAVAILABLE | 直接跳转，跳过 DEGRADED 中间态 |
   | DEGRADED | 连续 3 次调用正常（每次耗时 < 阈值） | CONNECTED | 性能恢复，计数器重置策略见下方说明 |
  | DEGRADED | 累积失败次数 >= N（默认 5 次） | UNAVAILABLE | 从退化到不可用 |
  | UNAVAILABLE | 探测成功（tryProbe 返回 true，调用正常） | CONNECTED | 端点恢复，直接从 UNAVAILABLE 回到 CONNECTED |
  | UNAVAILABLE | 探测失败 | UNAVAILABLE | 重置 30 秒探测计时器，保持不可用 |

CONNECTED→UNAVAILABLE 直接跳转理由：当端点突然彻底宕机（如 HTTP 500 连续响应、连接拒绝），等待经过 DEGRADED 的耗时阈值再触发熔断会延长不可用持续时间。直接跳转使熔断决策仅依赖失败次数而非耗时阈值，对突发性故障响应更快。

**计数器重置策略**：
DEGRADED→CONNECTED 的恢复路径中，失败计数和耗时窗口计数器的清除策略如下：
- **失败计数清零**：成功调用（耗时 < 阈值）计数器仅在连续调用成功后逐次递减而非一次性清零。具体规则：连续 3 次正常调用（每次耗时 < 阈值）才回退到 CONNECTED 状态。若恢复过程中出现一次失败或超时调用，计数器重置回到 DEGRADED 起始状态，重新开始计数。此策略避免单次偶然成功导致状态抖动（如网络短暂抖动后的单次正常调用立即回退 CONNECTED，随后下一次调用又退化到 DEGRADED，形成反复震荡）。
- **耗时窗口计数器清零**：连续正常调用计数器在状态回退至 CONNECTED 时自动重置为 0，等待下一次退化触发条件。
- **替代方案**：若生产环境观察表明连续 3 次正常调用导致 DEGRADED→CONNECTED 恢复迟缓（如端点已稳定恢复但底座仍处于 DEGRADED 状态），可将阈值调整为连续 1 次正常调用即回退 CONNECTED，同时接受可能的"单次正常→回退→下一次超时→退化"状态抖动风险。此阈值可通过配置项 `ai.model-endpoint-health.recovery-success-count: 3` 在运行态调整，默认值 3。
```

**探测调用触发机制**：`CapabilityExecutor` 在管线执行中（模型路由之后、LLM 调用之前）调用 `ModelEndpointHealthManager.getState()` 检查目标模型端点的健康状态。若为 UNAVAILABLE 且距离上次探测超过 30 秒，`tryProbe()` 返回 true，允许一次探测调用（`LlmChatService.chat()` 正常发送，但超时阈值缩短为正常值的 50%）；探测成功则将状态回退至 CONNECTED，探测失败则重置 30 秒等待计时。若 `tryProbe()` 返回 false（未到探测窗口），则跳过 LLM 调用直接触发降级。

**与 CircuitBreakerDegradationStrategy 的交互优先级**：
- 两个组件维护不同维度的健康视图，但执行时序与探测逻辑统一：
  1. **降级预检阶段**（`CapabilityExecutor.execute()` 管线第一步）：`CircuitBreakerDegradationStrategy.shouldDegrade()` 评估熔断状态。若为 OPEN 状态，直接降级，`ModelEndpointHealthManager` 的健康检查步骤被跳过（LLM 调用不会发生）
  2. **端点健康检查阶段**（模型路由之后、LLM 调用之前）：若熔断器为 CLOSED（或 HALF_OPEN 允许探测），`ModelEndpointHealthManager.getState()` 检查端点级别健康。UNAVAILABLE 时同样触发降级
- **交互规则**：
  - 熔断器优先于端点健康检查——熔断器 OPEN 时端点检查不会执行
  - 两者互不覆盖：熔断器状态是针对"能力"级别的失败率统计，端点健康是针对"模型端点"级别的可用性探测，两个维度的判定各自独立
   - 降级原因标识区分两种来源：`CircuitBreakerDegradationStrategy` 命中时降级原因为 `DegradationReason.CIRCUIT_BREAKER_OPEN`，`EndpointUnavailable` 时降级原因为 `DegradationReason.ENDPOINT_UNAVAILABLE`
   - **`getOrder()` 方法说明**：`DegradationStrategy` 接口当前不存在 `getOrder()` 方法，Phase 5 需在 `ai-api` 模块的 `DegradationStrategy` 接口中新增 `default int getOrder() { return 0; }` 方法。此变更为**二进制兼容**——所有现有实现（`NoOpDegradationStrategy`、`TimeoutDegradationStrategy`、`CircuitBreakerDegradationStrategy` 等）无需修改即可编译通过，默认排序值 0 保证新旧策略按声明顺序排序。具体验证步骤见 §9.4
- **统一探测机制**：
  - `ModelEndpointHealthManager` 作为端点健康探测的**单一决策者**，维护端点级别的探测计时器（UNAVAILABLE 下每 30 秒允许一次探测调用）
  - `CircuitBreakerDegradationStrategy` 在 HALF_OPEN 状态下不再独立发起探测，而是委托 `ModelEndpointHealthManager.tryProbe()` 判断是否允许探测调用；若 `tryProbe()` 返回 false（未到探测窗口），熔断器暂不发送探测，保持 OPEN 状态
  - **时序**：熔断器状态 OPEN→HALF_OPEN 的窗口到期后，熔断器不立即发送请求，而是检查目标端点的 `ModelEndpointHealthManager` 健康状态——若端点回复 UNAVAILABLE 且未到探测窗口，熔断器暂不执行探测；若端点健康或允许探测，则熔断器发送探测调用
  - **统一后的探测路径**：无论降级原因来自熔断器还是端点健康管理器，探测调用均通过 `ModelEndpointHealthManager.tryProbe()` 统一裁定。CLOSED 路径的正常调用不受影响
  - 此举消除两个组件独立计时器可能产生的冲突（如图 A 允许探测但图 B 阻止，浪费探测窗口）

**统一探测决策表**：

| 熔断器状态 | 端点健康状态 | tryProbe() | 决策 |
|-----------|------------|-----------|------|
| CLOSED | CONNECTED | 不调用 | 正常调用，无需探测 |
| CLOSED | DEGRADED | 不调用 | 正常调用（DEGRADED 仍允许调用，仅告警） |
| CLOSED | UNAVAILABLE | true（到探测窗口） | 发送探测调用（超时阈值为 50%），成功则端点回 CONNECTED，失败继续 UNAVAILABLE |
| CLOSED | UNAVAILABLE | false（未到窗口） | 直接降级，降级原因 `DegradationReason.ENDPOINT_UNAVAILABLE` |
| OPEN | — | 不调用 | 直接降级，不进入端点检查，降级原因 `DegradationReason.CIRCUIT_BREAKER_OPEN` |
| HALF_OPEN | CONNECTED | true | 熔断器发送探测调用，成功回 CLOSED，失败回 OPEN |
| HALF_OPEN | DEGRADED | true | 同上（端点 DEGRADED 仍允许探测） |
| HALF_OPEN | UNAVAILABLE | true（到窗口） | 熔断器等待探测窗口，发送探测调用，成功回 CLOSED 且端点到 CONNECTED，失败熔断回 OPEN 且端点重置 30s 计时 |
| HALF_OPEN | UNAVAILABLE | false（未到窗口） | 熔断器暂不执行探测，保持 OPEN 状态；待 tryProbe() 允许后重试 |

**时序图**（统一探测流程）：

```mermaid
sequenceDiagram
    participant CE as CapabilityExecutor
    participant CB as CircuitBreakerDegradationStrategy
    participant HM as ModelEndpointHealthManager
    participant LC as LlmChatService

    Note over CE,LC: 场景 A：CLOSED + UNAVAILABLE + 可探测
    CE->>CB: shouldDegrade(context)
    CB-->>CE: false (CLOSED)
    CE->>HM: getState(endpointId)
    HM-->>CE: UNAVAILABLE
    CE->>HM: tryProbe(endpointId)
    HM-->>CE: true (到探测窗口)
    CE->>LC: chat() with 50% timeout
    LC-->>CE: response
    CE->>HM: recordCallResult(endpointId, true, elapsedMs)
    HM-->>CE: 状态 → CONNECTED
    CE->>CE: 继续正常流程

    Note over CE,LC: 场景 B：HALF_OPEN + UNAVAILABLE + 不可探测
    CE->>CB: shouldDegrade(context)
    CB-->>CE: false (HALF_OPEN)
    CE->>HM: getState(endpointId)
    HM-->>CE: UNAVAILABLE
    CE->>HM: tryProbe(endpointId)
    HM-->>CE: false (未到探测窗口)
    CE->>CE: 触发降级，原因 DegradationReason.ENDPOINT_UNAVAILABLE

    Note over CE,LC: 场景 C：OPEN（熔断器优先，不进入端点检查）
    CE->>CB: shouldDegrade(context)
    CB-->>CE: true (OPEN)
    CE->>CE: 触发降级，原因 DegradationReason.CIRCUIT_BREAKER_OPEN
```

#### `LlmChatRequest` — 大模型对话请求 DTO（class，归属 `ai-impl/client/`）

**职责**：封装完整的对话请求，包含消息列表与可选配置。替换原 `LlmRequest` 的扁平化设计（仅 `prompt: String` + `parameters: Map`），引入多轮消息支持与强类型配置。

**字段级契约**：
- `messages`（`List<LlmChatMessage>`，非空）：对话消息列表，有序排列，顺序即对话上下文顺序。至少包含一条消息（`role=USER`）
- `options`（`LlmChatOptions`，可空）：对话配置，为 null 时使用所有默认值
- `clientType`（`ClientType`，可空）：目标客户端类型枚举，`CapabilityExecutor` 在 `doExecuteInternal()` 中从 `ModelRoute.clientType` 设置，`DelegatingLlmChatService` 据此将请求派发至对应的底层实现（`HTTP_API` → `HttpApiLlmChatService`、`SPRING_AI` → `SpringAiLlmChatService`）。null 时由 `DelegatingLlmChatService` 回退到默认实现
- `tools`（`List<ChatToolDefinition>`，可空）：Tool 定义列表，仅 `structuredChat()` 路径使用。每个 `ChatToolDefinition` 定义结构化输出模式的 JSON schema 描述（对应 OpenAI function_call 的 `functions` 参数或 tool_use 的 `tools` 参数），由 `LlmChatService.chat()` 忽略、`structuredChat()` 实现层在处理 `StructuredOutputNotSupportedException` 回退前优先通过原生 tool_use/function_call 获取结构化输出。`CapabilityExecutor` 在构造 `LlmChatRequest` 时，将 `outputType`（目标解析 DTO 的 Class）通过 Jackson `SchemaFactory` 自动生成为 JSON Schema 后设置此字段。`ChatToolDefinition` 为内嵌值对象，字段包括 `name`、`description`、`parameters（JsonNode JSON Schema）`、`strict（boolean，默认 true）`。**`tools` 纳入全参构造器**：`messages`、`options`、`clientType`、`tools` 四个字段均在 `LlmChatRequest` 全参构造器中统一赋值（`tools` 为 `@Nullable` 参数，末尾位），消除通过独立 setter 设置导致 `tools` 被遗漏的结构性风险。`LlmChatRequest` 类定义如下：`public LlmChatRequest(List<LlmChatMessage> messages, LlmChatOptions options, ClientType clientType, @Nullable List<ChatToolDefinition> tools)`。`tools` 参数为 null 时等价于调用 `chat()` + `StructuredOutputParser.parse()`，`structuredChat()` 实现层直接抛出 `StructuredOutputNotSupportedException` 触发回退路径。类图同步更新（§2.3）

**JSON mode 检测时机与缓存策略**：
- `structuredChat()` 实现层在首次调用时检测目标模型端点是否支持原生 JSON mode（通过读取 `ModelRoute.parameters` 中的 `json_mode` 配置项或调用供应商 API 的能力发现接口），检测结果以 `Cache<String, Boolean>`（Caffeine，TTL=5 分钟）缓存，key = `endpointId + ":" + modelId`
- 检测时机：首次 `structuredChat()` 调用时运行期检测，不前置到启动期。理由：(1) 避免启动期因供应商 API 不可用导致底座启动失败；(2) 模型能力升级后可在缓存 TTL 过期后自动感知
- 缓存策略：`Cache<String, Boolean>`，key 为 `endpointId + ":" + modelId`，expireAfterWrite=5 分钟。首次检测成功后写入缓存，后续调用直接读取缓存避免重复检测
- 若缓存结果为"不支持 JSON mode"：`structuredChat()` 实现层直接抛出 `StructuredOutputNotSupportedException` 触发回退到 `chat()` + `StructuredOutputParser.parse()`，不再发送带有 `tools` 字段的调用，减少无效请求的 Token 消耗

**structuredChat() 回退路径超时叠加风险**：
- 当 `structuredChat()` 因 `StructuredOutputNotSupportedException` 回退到 `chat()` + `StructuredOutputParser.parse()` 时，存在**端到端超时叠加风险**：`CapabilityExecutor` 的整体管线超时 `capabilityTimeout` 需要容纳 `structuredChat()` 调用 + `chat()` 调用 + `parse()` 三段时间的总和。若 `structuredChat()` 已消耗了大量超时窗口，`chat()` 回退可能因剩余时间不足而被 `orTimeout()` 打断，从而进入降级路径
- 缓解措施：(1) `LlmChatService` 实现层在识别到 JSON mode 缓存为"不支持"后，直接抛出 `StructuredOutputNotSupportedException` 而非先尝试一次带 `tools` 的调用再失败，避免无效调用消耗超时窗口；(2) 若缓存不存在（首次调用），`structuredChat()` 实现层对带 `tools` 的调用设置内部较短超时（如 `capabilityTimeout` 的 60%），剩余 40% 窗口留给 `chat()` 回退路径；(3) `parse()` 步骤本身有独立的可配置超时控制（`StructuredOutputParser.parse()` 调用包裹 `CompletableFuture.supplyAsync().get(parseTimeout)`），通过 `parseTimeoutConfig` Map（`@Bean("parseTimeoutConfig")` 注入）按能力标识查找对应超时值，未配置的能力使用 `parseTimeoutDefault`（`@Value("${ai.execution.timeout.parse.default:5s}")` 注入默认值），避免 JSON 解析步骤无限挂起。**⚠️ parseTimeout 与 chatFallbackTimeout 的层级约束**：`parseTimeout` 必须小于等于 `chatFallbackTimeout`，否则当 `parseTimeout > chatFallbackTimeout` 时，解析步骤的超时将先于 chat 回退的整体窗口到期，降级原因被记为"解析超时"而非"chat 回退超时"，导致超时分类与降级原因记录失准。配置约束：`parseTimeout <= chatFallbackTimeout`。在 `AiPlatformConfig` 的 `@PostConstruct` 阶段新增校验逻辑，对所有能力逐一验证此约束（含默认值对比），校验不通过时抛出 `IllegalStateException` 启动期 fail-fast

**为何替换 `LlmRequest`**：原 `LlmRequest` 仅含 `prompt: String` + `modelId: String` + `parameters: Map<String, Object>`，无法表达多轮对话结构（SYSTEM/USER/ASSISTANT 角色），且 `parameters` 为弱类型 Map 丢失编译期类型保护。`LlmChatRequest` 以 `messages` 列表替代单一 prompt，以强类型 `LlmChatOptions` 替代弱类型 Map。

#### `LlmChatMessage` — 对话消息值对象（class，归属 `ai-impl/client/`）

**职责**：表示一次对话中的单条消息，区分用户、助手与系统三种角色。

**字段级契约**：
- `role`（`LlmChatMessageRole`，非空）：消息角色——SYSTEM / USER / ASSISTANT
- `content`（String，非空）：消息文本内容

#### `LlmChatMessageRole` — 对话消息角色枚举（enum，归属 `ai-impl/client/`）

**职责**：定义对话消息的角色类型，枚举值为 `SYSTEM`（系统提示）、`USER`（用户输入）、`ASSISTANT`（助手响应）。使用 enum 而非裸 String 提供编译期类型安全。

#### `ClientType` — 模型客户端类型枚举（enum，归属 `ai-impl/client/`）

**职责**：定义大模型对接的底层客户端实现类型，枚举值为 `HTTP_API`（通过 HTTP API 直连大模型）和 `SPRING_AI`（通过 Spring AI ChatModel 抽象接入）。作为 `ModelRoute.clientType` 字段的类型和 `DelegatingLlmChatService` 分发决策的依据，提供编译期类型安全替代字符串字面量。

**为何使用 enum 而非 string**：`ModelRoute` 作为路由配置值对象，`clientType` 影响 `DelegatingLlmChatService` 的运行时 Bean 选择，string 类型无法在编译期约束合法值，新增客户端实现类型后若配置层误填不存在的类型名将静默回退。enum 确保配置层和代码层使用同一封闭类型集合。

#### `LlmChatOptions` — 对话参数配置值对象（class，归属 `ai-impl/client/`）

**职责**：封装 LLM 调用的可调参数，使调用方可在默认值基础上按需定制。使用强类型字段替代 `Map<String, Object>`，提供编译期类型保护。

**字段级契约**：
- `modelId`（String，可空）：模型标识
- `temperature`（Double，可空）：控制输出随机性
- `maxTokens`（Integer，可空）：限制单次响应的最大 token 数
- `stopSequences`（`List<String>`，可空）：停止序列列表
- `topP`（Double，可空）：核采样参数，控制候选 token 的累积概率阈值（对应 OpenAI `top_p`），可空时由实现层使用默认值
- `frequencyPenalty`（Double，可空）：频率惩罚系数，减少已出现 token 的重复生成（对应 OpenAI `frequency_penalty`），可空时由实现层使用默认值
- `presencePenalty`（Double，可空）：存在惩罚系数，鼓励讨论新主题（对应 OpenAI `presence_penalty`），可空时由实现层使用默认值

**参数映射扩展点规则**：`LlmChatOptions` 与 `ModelRoute.parameters` 之间的 key 映射采用**白名单显式映射**模式。当前白名单包含 `temperature`、`maxTokens`、`stopSequences`、`topP`、`frequencyPenalty`、`presencePenalty` 六个 key。不在白名单中的 `parameters` key 不进入 `LlmChatOptions` 的强类型字段——若需新增参数，需同步在 `LlmChatOptions` 类中新增强类型字段并更新阶段一映射逻辑。此设计避免了 `Map<String, Object>` 的弱类型风险，但要求每新增一个 LLM 参数支持都需要修改 `LlmChatOptions` 类。若后续出现供应商特有参数（如阿里云的 `enable_search`、OpenAI 的 `logprobs`），可考虑在 `LlmChatOptions` 中新增 `Map<String, Object> extensionParameters` 兜底字段接收白名单外参数，由 `LlmChatService` 实现层按需读取。

**与 `ModelRoute.parameters` 的合并策略**（两阶段填充）：
`LlmChatOptions` 构造时采用两阶段策略将 `ModelRoute.parameters`（`Map<String, Object>`）映射为强类型字段：

1. **阶段一（基值填充）**：`CapabilityExecutor.doExecuteInternal()` 管线中构造 `LlmChatOptions` 时，以 `modelRoute.getParameters()` 返回的 Map 为基值，按约定 key 名提取并设置对应字段。key 名与字段名的映射关系为：`"temperature"` → `temperature`、`"maxTokens"` → `maxTokens`、`"stopSequences"` → `stopSequences`、`"topP"` → `topP`、`"frequencyPenalty"` → `frequencyPenalty`、`"presencePenalty"` → `presencePenalty`。Map 中不存在的 key 对应字段保持 null（`LlmChatService` 实现层使用各供应商 SDK 的默认值）。此阶段在 `new LlmChatOptions(...)` 构造时通过类似 `options.setTemperature((Double) parameters.get("temperature"))` 的转换逻辑完成
2. **阶段二（调用方覆盖）**：`CapabilityExecutor` 在阶段一构造完成后，可根据本次调用的上下文在 `options` 上调用 setter 覆盖特定字段。例如 A/B 实验中若 `ExperimentAssignment.getTargetModelId()` 指定了覆盖模型，则在 `options.setModelId(assignment.getTargetModelId())` 中覆盖。阶段二的赋值**始终优先生效**，即 `CapabilityExecutor` 的显式覆盖优先级高于 `ModelRoute.parameters` 中的值

**优先级总则**：`CapabilityExecutor` 调用方显式赋值 > `ModelRoute.parameters` Map 配置值 > `LlmChatOptions` 字段类型默认值（null，由实现层 SDK 使用默认值）。此策略使 `ModelRoute.parameters` 承载端点级别的参数默认值（如 `temperature: 0.3`），同时允许 `CapabilityExecutor` 按本次调用上下文（如实验分组、不同科室需求）覆盖特定参数，两阶段职责分离、覆盖优先级明确。

#### `LlmChatResponse` — 大模型对话响应 DTO（class，归属 `ai-impl/client/`）

**职责**：封装 LLM 的响应结果，包含助手消息文本、Token 用量统计（内嵌 `LlmChatUsage` 静态类）与模型标识。替换原 `LlmResponse`。

**字段级契约**：
- `content`（String，非空）：助手消息文本内容
- `usage`（`LlmChatUsage`，可空）：Token 使用统计
- `modelId`（String，可空）：实际响应的模型标识
- `retryCount`（int，默认 0）：LLM 实现层内部重试计数，由 `LlmChatService` 实现层在调用过程中填充（`chat()` 返回的 `LlmChatResponse` 中此字段由实现层负责赋值，该计数位于 `LlmChatResponse` 而非 `StructuredChatResult`，因为 `structuredChat()` 的重试计数通过 `StructuredChatResult.getRetryCount()` 返回，而 `chat()` 无 `StructuredChatResult` 包裹）

#### `LlmChatUsage` — Token 用量静态内嵌类（static class，归属 `LlmChatResponse` 内嵌）

**职责**：封装一次 LLM 调用的 Token 消耗详情，作为 `LlmChatResponse` 的内嵌静态类。Token 使用统计是 LLM 响应的附属信息，不存在独立使用的场景，内嵌到 `LlmChatResponse` 中更紧凑。管线代码通过 `chatResponse.getUsage().getPromptTokens()` 等调用获取 Token 用量。

**字段级契约**：
- `promptTokens`（int，默认 0）：输入 token 数
- `completionTokens`（int，默认 0）：输出 token 数
- `totalTokens`（int，默认 0）：总 token 数

#### `StructuredChatResult<T>` — 结构化调用结果值对象（class，归属 `ai-impl/client/`）

**职责**：封装 `LlmChatService.structuredChat()` 的完整返回结果，使成功路径也能携带调用元数据（重试计数、Token 用量），消除原设计中成功路径下 `retryCount` 和 `tokenUsage` 无法获取的设计缺口。

**字段级契约**：
- `data`（`T`，非空）：解析后的能力 DTO
- `retryCount`（int，默认 0）：`LlmChatService` 实现层内部重试次数
- `usage`（`LlmChatUsage`，可空）：Token 使用统计，模型未返回用量数据时为 null

#### `ModelRouter` — 模型路由契约（interface，归属 `ai-impl/router/`）

**职责**：根据能力标识与可选的实验分组信息，决定本次调用应使用哪个模型配置（端点地址、模型名称、客户端类型）。

**协作对象**：
- 被 `CapabilityExecutor` 在模型路由步骤中调用
- 返回 `ModelRoute` 值对象供 `LlmChatService` 选择实例与构建 `LlmChatRequest`
- 与 `ExperimentManager` 协作：若实验分组指定覆盖模型，优先采用分组的模型配置

**路由配置热加载**：管理端变更路由配置后，通过 Spring `ApplicationEvent` 触发 `ModelRouter` 缓存刷新。缓存失效事件 `RouteConfigChangedEvent` 定义如下：
```java
class RouteConfigChangedEvent extends ApplicationEvent {
    String endpointId;           // 变更的端点标识（null=影响全部端点）
    ChangeType changeType;       // CREATED / UPDATED / DELETED
    Instant changedAt;           // 变更时间戳
}
```
`DefaultModelRouter` 作为 `@EventListener` 接收 `RouteConfigChangedEvent`，清除对应 `endpointId` 的缓存条目。若 `endpointId=null` 则清空全部路由缓存。下次 `route()` 调用时从配置源重新加载。与 `TemplateChangedEvent`/`ExperimentChangedEvent` 一致的兜底策略——消费异常日志 ERROR，兜底 Caffeine expireAfterWrite 5 分钟自动过期。

#### `ModelRoute` — 模型路由条目（class，归属 `ai-impl/router/`）

**职责**：封装一条模型路由的目标信息，包括模型标识、端点 URL、客户端类型（HTTP_API / SPRING_AI）、生成参数默认值与权重、端点级超时配置。

**字段扩展**：
| 字段 | 类型 | 说明 |
|------|------|------|
| endpointId | String | 端点唯一标识，用于 ModelEndpointHealthManager 健康状态索引和 Vault 密钥查询 |
| connectionTimeout | Duration | 连接超时（默认 5s），LLM HTTP 调用连接建立的最大等待时间 |
| readTimeout | Duration | 读取超时（默认 30s），LLM HTTP 调用响应读取的最大等待时间 |
| parameters | Map<String, Object> | 模型调用参数集合，包含 temperature、maxTokens、topP 等 LLM 生成参数。`LlmChatOptions` 构造时从此 Map 提取参数值设置到对话配置。每个 endpointId 可独立配置，YAML 中在路由条目中以 `parameters: {temperature: 0.7, maxTokens: 2048}` 形式声明。空 Map 时 `LlmChatService` 使用内置默认值 |
| authType | AuthType enum (API_KEY / OAUTH2 / NONE) | 端点认证方式。API 密钥不直接存储在 ModelRoute 中，认证凭据通过 `endpointId` 从 Vault/配置中心按需查询，实现密钥与路由配置的分离。`LlmChatService` 实现类在调用 `chat()/structuredChat()` 时，通过 `endpointId` 从密钥存储中动态获取认证头 |

**凭据查询接口 `CredentialProvider`**（新增，归属 `ai-impl/client/`）：

```java
interface CredentialProvider {
    /**
     * 根据端点标识查询认证凭据。
     * @param endpointId 端点唯一标识（对应 ModelRoute.endpointId）
     * @return 包含 API Key / OAuth2 Token 的凭据对象，endpointId 不存在时返回 empty Optional
     */
    Optional<Credential> getCredential(String endpointId);

    /**
     * 凭据值对象，封装认证方式与凭证值
     */
    class Credential {
        AuthType authType;   // API_KEY / OAUTH2 / NONE
        String credentialValue; // API Key 明文 / OAuth2 Access Token / null (NONE时)
        Instant expiresAt;   // 凭证过期时间（OAUTH2 场景），可空
        String vaultPath;    // Vault 中的凭据路径，用于审计追踪
    }
}
```

**凭据缓存策略**：
- `CredentialProvider` 内部维护 `Cache<String, Credential>`（基于 Caffeine 或 Guava Cache），TTL 默认 5 分钟
- OAuth2 Token 根据 `Credential.expiresAt` 提前 60 秒自动过期，触发异步刷新
- 缓存未命中时查询 Vault/配置中心，查询结果写入缓存后返回
- 缓存刷新使用 `CompletableFuture.supplyAsync()` 异步执行，不阻塞调用线程

**凭据缓存 Caffeine 实现方案**：`Cache<String, Credential>` 基于 Caffeine，`expireAfterWrite` 固定 TTL 无法在 CACHE_ONLY 状态下动态延长。实现方案采用 Caffeine `Expiry` 接口替代 `expireAfterWrite`：

```java
Cache<String, Credential> cache = Caffeine.newBuilder()
    .maximumSize(1000)
    .expireAfter(new Expiry<String, Credential>() {
        @Override
        public long expireAfterCreate(String key, Credential credential, long currentTime) {
            // OAUTH2 场景：根据 expiresAt 动态计算过期时间（提前 60 秒），最长不超过 5 分钟
            if (credential.getExpiresAt() != null) {
                Duration ttl = Duration.between(Instant.now(), credential.getExpiresAt()).minusSeconds(60);
                return Math.min(ttl.toNanos(), Duration.ofMinutes(5).toNanos());
            }
            return Duration.ofMinutes(5).toNanos();
        }

        @Override
        public long expireAfterUpdate(String key, Credential credential, long currentTime, long currentDuration) {
            return currentDuration; // 更新不修改过期时间
        }

        @Override
        public long expireAfterRead(String key, Credential credential, long currentTime, long currentDuration) {
            // CACHE_ONLY 状态下的 TTL 延长：每次读取时额外延长 30 秒（不超过原始 TTL 的 2 倍）
            if (credentialProvider.getState() == CredentialProviderState.CACHE_ONLY) {
                return Math.min(currentDuration + Duration.ofSeconds(30).toNanos(),
                    Duration.ofMinutes(5).toNanos() * 2); // 上限为原始 TTL 的 2 倍（10 分钟）
            }
            return currentDuration;
        }
    })
    .build();
```

**备选方案**：若不希望引入 `Expiry` 接口的复杂度，可采用"重新 put 同一 key 以刷新 TTL"方案——CACHE_ONLY 状态下降级为：每次读取缓存命中后，将 `Credential` 重新 `cache.put(key, credential)` 以重置 `expireAfterWrite` 计时器。此方案更简单但牺牲了 `Expiry` 接口的精细化控制能力。Phase 5 推荐优先使用 `Expiry` 接口方案，将 CACHE_ONLY 的 TTL 延长逻辑封装在 `expireAfterRead()` 中，使缓存 TTL 管理集中在单一配置点。

**Vault 不可达降级行为 — 状态模型**：
`CredentialProvider` 内部维护 Vault 连接的状态机，与 `ModelEndpointHealthManager`（§3.2）和 `CircuitBreakerDegradationStrategy`（§3.8）一致的形式化程度：

```
状态定义：
  NORMAL:      正常状态，Vault 查询正常发送，缓存 TTL=5 分钟
  CACHE_ONLY:  Vault 连续不可达阈值触发，仅使用缓存数据，不再查询 Vault；缓存中旧凭据的 TTL 延长 30 秒
  BACKOFF:     Vault 连续不可达超过 5 次，进入 30 秒退避窗口，期间不查询 Vault，仅使用缓存数据

状态转换条件：
  | 起始状态 | 触发条件 | 目标状态 | 说明 |
  |---------|---------|---------|------|
  | NORMAL | Vault 查询超时（默认 2s），缓存中存在旧凭据 | CACHE_ONLY | 旧凭据 TTL 延长 30 秒，WARN 日志 |
  | NORMAL | Vault 查询超时，且缓存中无数据 | NORMAL | 抛出 CredentialUnavailableException，不改变状态 |
  | NORMAL | Vault 返回正常结果 | NORMAL | 写回缓存，重置连续失败计数器 |
  | CACHE_ONLY | Vault 查询超时，连续失败计数 < 5 | CACHE_ONLY | 仍使用缓存，WARN 日志递增失败计数 |
  | CACHE_ONLY | Vault 连续不可达计数达到 5 | BACKOFF | 启动 30 秒退避定时器 |
  | CACHE_ONLY | Vault 返回正常结果 | NORMAL | 重置连续失败计数器，恢复正常查询 |
  | BACKOFF | 30 秒退避窗口到期 | NORMAL | 窗口期结束后触发一次探测查询 |
  | BACKOFF | 窗口期内再次调用 | BACKOFF | 直接返回缓存数据，不查询 Vault |

并发请求处理策略：
  - NORMAL 状态：并发请求各自独立查询 Vault，Caffeine 缓存保证相同 endpointId 的查询结果仅写入一次
  - CACHE_ONLY 状态：并发请求全部通过缓存读取，不进入 Vault 查询路径；第一个检测到超时的线程触发状态转换
  - BACKOFF 状态：所有请求直接从缓存读取，退避定时器由单一 `@Scheduled` 或 `ScheduledExecutorService` 驱动，仅在定时器到期时发起一次探测查询；探测成功回 NORMAL，失败回 CACHE_ONLY（重置 5 次计数器）

凭据有效性反馈接口（新增）：
外部调用方（如 `CapabilityExecutor`、运维监控端点）可通过以下方法获取 `CredentialProvider` 的状态而非仅等待定时器驱动恢复：
```java
// 返回 Vault 连接当前状态
CredentialProviderState getState();

// 验证指定端点凭据的有效性：返回 true 表示凭据可用且 Vault 连接正常，
// false 表示凭据可能已过期或 Vault 不可达
boolean isCredentialValid(String endpointId);

// 触发一次主动探测：强制查询 Vault 以尝试恢复 NORMAL 状态。
// 返回 true 表示探测成功（Vault 可正常返回凭据），false 表示探测失败
boolean probeVault();

// CredentialProvider 状态枚举
enum CredentialProviderState {
    NORMAL,    // Vault 连接正常，缓存 TTL=5 分钟
    CACHE_ONLY, // Vault 不可达，使用缓存数据，TTL 延长 30 秒
    BACKOFF    // 连续失败超 5 次，30 秒退避窗口内不查询 Vault
}
```
`probeVault()` 使运维侧或定时器可主动触发探测恢复，与 `ModelEndpointHealthManager.tryProbe()` 设计一致——`CapabilityExecutor` 在监测到 `CredentialProvider.getState() != NORMAL` 且距离上次探测超过 30 秒时，可调用 `probeVault()` 发起一次探测，不依赖单向定时器驱动恢复。

故障计数清除时机：
  - 连续失败计数器在 Vault 返回正常结果时清零
  - 不设置时间窗口自动衰减（Phase 5 精简策略，避免 Vault 间歇性波动下状态反复抖动；若生产环境观察到抖动可引入窗口衰减机制）
```

**认证分离理由**：API 密钥属于敏感凭据，不应随路由配置明文存储在 YAML 或数据库中；Vault/配置中心提供密钥轮转、审计和权限控制能力。`ModelRoute` 作为纯值对象，不持有密钥引用。

**`LlmChatService` 密钥查询时序**：`LlmChatService.chat()/structuredChat()` 在构造 `LlmChatRequest` 之前，通过 `CredentialProvider.getCredential(modelRoute.getEndpointId())` 获取认证凭据。凭据查询失败时（`CredentialUnavailableException`），`LlmChatService` 将异常抛出至 `CapabilityExecutor`，由 `doExecuteInternal()` 的 catch 块捕获后走基础设施异常降级路径（`DegradationReason.INFRASTRUCTURE_ERROR`）。凭据查询不重试（避免放大 Vault 压力），由 `CapabilityExecutor` 的整体管线超时兜底。

### 3.3 Prompt 模板管理层

#### `PromptTemplateManager` — Prompt 模板管理契约（interface，归属 `ai-impl/template/`）

**职责**：
- 按能力标识与可选科室标识，以及可选的 Prompt 版本号检索当前生效的 Prompt 模板
- 支持模板渲染：将业务 DTO 字段注入模板变量占位符，生成最终 Prompt 文本
- 模板版本管理：同一能力+科室可存在多个版本，同一时刻仅一个版本为启用状态；A/B 实验可通过 `promptVersion` 参数指定版本号，实现实验分组级别的 Prompt 版本分流
- 支持运行时热加载：管理员在管理端修改模板后通过 Spring ApplicationEvent 通知缓存失效。缓存失效事件 `TemplateChangedEvent` 定义如下：
  - **Event Payload**：
    ```java
    class TemplateChangedEvent extends ApplicationEvent {
        String capabilityId;      // 模板所属能力标识
        String departmentId;      // 科室标识（null=影响全部科室）
        Integer promptVersion;    // 变更的版本号（null=全部版本）
        ChangeType changeType;    // CREATED / UPDATED / DELETED / STATUS_CHANGED
        Instant changedAt;        // 变更时间戳
    }
    ```
  - **重建策略**：`DatabasePromptTemplateManager` 作为 `@EventListener` 接收 `TemplateChangedEvent`，清除对应 `(capabilityId, departmentId)` 组合的缓存条目。若 `departmentId=null` 则清除该能力下所有科室的缓存。清除后下次 `render()` 调用时从数据库重新加载。缓存使用 `Caffeine`（expireAfterWrite=5 分钟 + maximumSize=1000），事件驱动清除为主动策略，过期自动清除为兜底策略
  - **发布/消费方时序**：管理端保存模板后发布 `TemplateChangedEvent`，`DatabasePromptTemplateManager` 同步消费（同一事务内），确保后续请求使用新模板。若事件消费失败（如消费者抛出异常），兜底策略为 Caffeine 的 expireAfterWrite 自动过期——最多容忍 5 分钟的不一致窗口。为减少异常窗口，`DatabasePromptTemplateManager` 的 `@EventListener` 使用 try-catch 包裹，消费异常时日志 ERROR 但不阻止事务提交
- 提供兜底 Prompt 获取方法 `getFallbackPrompt(capabilityId)`：当模板渲染异常时返回各能力内置的硬编码兜底 Prompt，确保 LLM 调用可继续执行

**`getFallbackPrompt(capabilityId)` 返回值契约**：
- **非空非空字符串**：`getFallbackPrompt()` 约定始终返回非 null 且非空（`length() > 0`）的字符串。`CapabilityExecutor` 调用方无需判空。实现层通过以下机制保证：各能力在数据库初始化时同步插入一条兜底 Prompt，或以能力 ID 为 key 在配置文件中声明兜底模板内容；若某能力既无兜底配置也无兜底初始化数据，`DatabasePromptTemplateManager` 返回一条通用兜底 Prompt（如 `"请根据以下信息提供{{capabilityName}}分析结果：{{userInput}}"`），确保永远不会返回 null
- **格式一致性**：返回的兜底 Prompt 与 `render()` 正常输出的格式一致（同为 SYSTEM 角色模板文本），但**不含 DTO 变量注入**（兜底场景发生在模板渲染异常之后，DTO 变量提取可能已失败，兜底模板中不引用 `{{key}}` 占位符）。`CapabilityExecutor` 使用兜底 Prompt 时需确保 USER message 包含足够完整的 DTO 输入信息（通过 `request.toString()`）以补偿缺失的变量注入
- **各能力兜底 Prompt 的管理方式**：兜底 Prompt 不作为硬编码字符串直接写在 `PromptTemplateManager` 实现类中，而是通过配置项管理——在 `ai.template.fallback.{capabilityId}` YAML 配置项中声明，或通过 `PromptTemplate` 表初始化 DRAFT 状态记录（status=DRAFT 的模板在渲染时不可用，但作为兜底配置可被 `getFallbackPrompt()` 读取）。设计理由：(1) 配置化使兜底 Prompt 可在运行态调整而无需重新编译；(2) 避免 `PromptTemplateManager` 实现类因持有所有能力的兜底文本而丧失单一职责

**协作对象**：
- 被 `CapabilityExecutor` 在模板渲染步骤中调用，由 `ExperimentAssignment.getTargetPromptVersion()` 提供目标版本号
- 从 `PromptTemplateRepository` 加载模板（数据库持久化）
- `getFallbackPrompt(capabilityId)` 在模板渲染异常时由 `CapabilityExecutor.doExecuteInternal()` 调用

#### `PromptTemplate` — Prompt 模板值对象 / JPA Entity（class，归属 `ai-impl/template/`）

**状态模型**：
```
DRAFT → ACTIVE → DEPRECATED
- DRAFT: 草稿状态，仅管理端可见，不参与运行时渲染
- ACTIVE: 启用状态，运行时以此版本渲染。同一 capabilityId + departmentId 组合同时仅一个 ACTIVE
- DEPRECATED: 废弃状态，历史数据保留但不再用于渲染
状态转换:
- DRAFT → ACTIVE（管理端发布）
- ACTIVE → DEPRECATED（新版本发布后旧版本自动废弃）
- DEPRECATED → ACTIVE（管理端手动回滚。用于新版本上线发现 Bug 时的紧急恢复场景——将上一个 ACTIVE 版本重新激活。回滚操作自动将当前 ACTIVE 版本降级为 DEPRECATED，保持"同一 capabilityId + departmentId 组合同时仅一个 ACTIVE"的不变量）
```

### 3.4 A/B 实验管理层

#### `ExperimentManager` — A/B 实验管理契约（interface，归属 `ai-impl/experiment/`）

**职责**：
- 判断当前请求是否命中某个 A/B 实验
- 若命中，返回实验分组信息（`ExperimentAssignment`），包括应使用的模型标识或 Prompt 版本
- 支持按能力标识维度配置实验，按用户标识或会话标识做确定性分桶
- 实验配置缓存：`HashBucketExperimentManager` 内部维护 `Caffeine<CacheKey, List<Experiment>>` 缓存（expireAfterWrite=5 分钟），管理端变更实验配置后通过 Spring ApplicationEvent 通知缓存失效。缓存失效事件 `ExperimentChangedEvent` 定义如下：
  - **Event Payload**：
    ```java
    class ExperimentChangedEvent extends ApplicationEvent {
        String capabilityId;      // 实验所属能力标识（null=影响全部能力）
        Long experimentId;        // 变更的实验 ID（null=影响该能力下全部实验）
        ChangeType changeType;    // CREATED / UPDATED / DELETED / STATUS_CHANGED
        Instant changedAt;        // 变更时间戳
    }
    ```
  - **重建策略**：`HashBucketExperimentManager` 作为 `@EventListener` 接收 `ExperimentChangedEvent`，清除对应 `capabilityId` 的缓存条目（若 `capabilityId=null` 则清空全部缓存）。下次 `assign()` 调用时从 `ExperimentRepository` 重新加载，重新执行哈希分桶
  - **发布/消费方时序**：管理端保存实验配置后发布 `ExperimentChangedEvent`，`HashBucketExperimentManager` 同步消费。与 TemplateChangedEvent 一致的兜底策略：事务内消费，消费异常日志 ERROR，兜底 Caffeine expireAfterWrite 5 分钟自动过期

#### `Experiment` — 实验配置值对象 / JPA Entity（class，归属 `ai-impl/experiment/`）

**状态模型**：
```
DRAFT → ACTIVE → PAUSED → COMPLETED
- DRAFT: 编辑中，不分流
- ACTIVE: 运行中，参与流量分配
- PAUSED: 暂停分流，所有流量回退到默认模型
- COMPLETED: 结束，所有流量回到默认模型
状态转换: DRAFT → ACTIVE（管理端启动）；ACTIVE → PAUSED（管理端暂停）；ACTIVE/PAUSED → COMPLETED（到达 endTime 或管理端手动结束）
```

#### `ExperimentGroup` — 实验分组 JPA Entity

**职责**：作为 `Experiment` 的子实体，定义实验下每个分组的流量分配比例和目标配置。与 `Experiment` 为一对多关联（`Experiment.groups`），通过 `@OneToMany(mappedBy = "experiment", cascade = ALL, orphanRemoval = true)` 映射。

**字段表**：

| 字段 | Java 类型 | 数据库类型 | 说明 |
|------|----------|-----------|------|
| id | Long | BIGINT (PK, AUTO) | 主键 |
| experiment | Experiment | BIGINT (FK) | 所属实验，`@ManyToOne` 关联 |
| group_id | String | VARCHAR(50) | 分组标识（如 "A"、"B"、"control"），同一实验内唯一 |
| percentage | int | INT | 流量分配百分比（0-1000，对应 0.0%-100.0%），所有分组百分比之和 = 1000 |
| target_model_id | String | VARCHAR(50) | 目标模型标识（可空，null 时使用默认模型） |
| target_prompt_version | Integer | INT | 目标 Prompt 版本号（可空，null 时不覆盖版本） |

**流量分配算法约束**：
- `percentage` 值域：[0, 1000]，语义为"千分比"（如 500 = 50.0%）
- 同实验所有分组 `percentage` 之和必须等于 1000（全量覆盖），由管理端保存时校验
- `HashBucketExperimentManager.assign()` 将 `userId/sessionId` 的哈希值模 1000 映射到 [0, 1000) 区间，遍历分组列表累加 `percentage`，找到哈希值落入的闭区间分组（首个 `cumulativePercentage > hash` 的分组）。此算法与 `percentage` 的千分比语义一致，确保流量分配按比例精确切分
- `group_id` 不可变（创建后不可修改），用于运行期日志和指标维度分组聚合

**千分比（0-1000）设计决策理由**：选择千分比而非百分比（0-100）的决策考量如下：
1. **整数精度**：千分比使用整数即可表示 0.1% 的精度（如 1‰ = 0.1%），而百分比若要达到相同精度需使用浮点数（如 0.5%），浮点累加比较在 `HashBucketExperimentManager.assign()` 的 `cumulativePercentage > hash` 分支判定中引入精度误差风险（如 `0.1 + 0.2 != 0.3` 场景）。千分比的整数累加 + 整数比较完全避免浮点精度问题
2. **值域与哈希模数一致**：哈希值 `userId/sessionId.hashCode() % 1000` 天然映射到 [0, 1000) 区间，使用千分比使 `percentage` 值域与哈希模数一致，无需额外缩放（若使用百分比 0-100，则需将哈希模值缩放到 [0, 100) 区间或修改哈希模数为 100，增加一次运算和边界风险）
3. **管理端配置复杂度权衡**：千分比 500 对应 50.0% 的管理端换算成本可通过配置界面自动换算缓解（管理端选中"50%"自动提交 500），实际运维中的换算负担可接受。若未来发现千分比导致频繁配置错误，可改为管理端存储百分比（0.0-100.0 浮点）后底座内部做 `int percentage = (int) Math.round(doublePercentage * 10)` 转换，底座侧 `ExperimentGroup` 的存储格式不变，仅管理端展示层做适配

#### `ExperimentAssignment` — 实验分组结果值对象（class，归属 `ai-impl/experiment/`）

**职责**：封装一次实验分流的结果。

**构造方式**：`ExperimentAssignment` 提供三种构造方法：
- **全参数构造器**：`ExperimentAssignment(String experimentId, String groupId, String targetModelId, Integer targetPromptVersion)`——所有字段通过构造器显式赋值，不提供 setter 方法，实例化为不可变对象。管线伪代码中使用此构造器显式传递全参数
- **无参默认工厂方法**：`ExperimentAssignment.createDefault()`——返回 `groupId="default"`、其余字段均为 null 的实例，语义为"无实验命中"。
- **异常降级工厂方法**：`ExperimentAssignment.createErrorFallback()`——返回 `groupId="experiment-error"`、其余字段均为 null 的实例，语义为"实验分流异常（非业务常态）"，与 `createDefault()` 使用可区分的哨兵 `groupId` 值。

**"无实验命中"与"实验分流异常"的返回值语义区分**：
- `ExperimentManager.assign()` 在未命中任何实验时返回 `ExperimentAssignment.createDefault()`（`groupId="default"`, `targetPromptVersion=null`）
- 实验分流异常（`assign()` 内部抛出异常）由调用方（`CapabilityExecutor.doExecuteInternal()`）捕获后返回 `ExperimentAssignment.createErrorFallback()`（`groupId="experiment-error"`, `targetPromptVersion=null`）
- 两场景的 `targetPromptVersion` 均为 null，但 `groupId` 值不同，下游实验效果分析报表通过 `groupId` 字段区分正常基线数据（`"default"`）与异常降级污染数据（`"experiment-error"`）。此区分规则在 §4.4 实验分流契约中显式声明

调用方（`CapabilityExecutor` 管线）无需对 `assignment` 做 null 检查，可直接调用 `assignment.getTargetPromptVersion()` 等方法。返回非 null 而非 `Optional` 的理由：(1) 消除所有调用点判空负担——管线中约 3 处直接引用了 `assignment` 的 getter，若使用 `Optional` 则每处均需 `.orElse(null)`，增加伪代码噪音且易遗漏；(2) "无实验"本身是业务常态而非边界异常，非 null 返回使调用方关注点落在"命中后的影响"而非"判空防卫"；(3) `targetPromptVersion = null` 作为"使用默认 ACTIVE 模板"的信号，在 `PromptTemplateManager.render()` 契约（§4.5）中已明确 null 时不指定版本号、回退默认模板检索策略，与 null 语义一致。

**AiCallRecord.promptVersion 空值守卫标记方案**：由于 `targetPromptVersion=null` 同时出现在"无实验命中"和"实验分流异常"两种场景，`AiCallRecord.promptVersion` 字段无法直接区分。底座采用以下标记方案——`AiCallRecord` 工厂方法中增加 `@Nullable String sentinelReason` 参数（位于参数列表末尾，现有编译调用点通过默认值 null 保持向后兼容）：
- 当 `promptVersion=null` 且 `groupId="experiment-error"` 时，工厂方法调用方传入 `sentinelReason="EXPERIMENT_ASSIGN_ERROR"`；工厂方法内部将 `prompt_version` 字段设置为 -1（哨兵值，不落入 [0, MAX] 有效范围），下游报表通过 `prompt_version = -1` 过滤异常行
- 当 `promptVersion=null` 且 `groupId="default"` 时，`sentinelReason=null`，`prompt_version` 字段为 null，表示正常基线数据
- `prompt_version` 列的索引策略（§3.5 AiCallLogEntity 索引策略）不变，-1 同样落入 `idx_prompt_version_call_time` 索引范围

#### `Experiment` 数据生命周期

实验数据在 COMPLETED 状态后的生命周期策略如下：

**数据保留策略**：
- 实验配置数据（`Experiment` + `ExperimentGroup`）：永久保留，用于历史实验效果回溯和审计
- 实验分流记录：不单独存储（`ExperimentManager` 为无状态哈希分桶，不维护分流记录表），调用记录中的 `promptVersion` 字段（归属 `AiCallLogEntity`）作为实验效果分析的数据来源

**历史分析支持能力**：
- 查询 COMPLETED 实验的执行历史时，通过 `ExperimentRepository` 的按时间范围查询方法 `findByStatusAndEndTimeBetween(ExperimentStatus status, LocalDateTime start, LocalDateTime end)` 获取在指定时间窗口内结束的实验列表
- 按能力标识查询该能力所有历史实验：`findByCapabilityIdOrderByEndTimeDesc(String capabilityId)`
- 实验效果分析报表通过 `AiCallLogRepository` 的聚合查询（`promptVersion` + `capabilityId` + `callTime` 维度）获取调用记录，结合实验分组配置进行对比分析

**索引策略**（`Experiment` + `ExperimentGroup` JPA 表）：
- `idx_experiment_capability_status` — `(capability_id, status)`：按能力标识+状态查询活跃实验
- `idx_experiment_end_time` — `(end_time DESC)`：按结束时间查询历史实验
- `idx_experiment_group_capability` — `(experiment_id, group_id)`：分组标识唯一索引

### 3.5 性能观测层

#### `AiMetricsCollector` — AI 性能指标采集契约（interface，归属 `ai-impl/metrics/`）

**职责**：
- 接收每次 AI 能力调用的完整记录（`AiCallRecord`），执行异步持久化写入 `AI 调用日志`
- 同时将关键指标推送到 Micrometer 指标体系
- Phase 5 默认实现为日志输出 + 数据库持久化

**异步队列溢出策略**：
- `AiMetricsCollector` 使用 Spring `@Async` + 专用指标采集线程池，与 `LlmCallExecutor` 线程池完全隔离，消除 `CallerRunsPolicy` 回退时阻塞 LLM 调用路径的风险
- 线程池配置：核心线程 1，最大线程 2，队列容量 1000
- **拒绝策略**：`DiscardPolicy` + 日志 WARN——队列满时丢弃指标记录而非阻塞调用路径。指标丢失在系统峰值时是可接受的降级行为，不应因此影响 LLM 调用主流程的可用性。此决策适用于 Phase 5 初始阶段；未来 Phase 6 可替换为独立的消息队列（Kafka / RabbitMQ）保障零丢失
- **指标记录调用方式**：`CapabilityExecutor` 管线中通过 `metricsCollector.record(record)` 调用后不等待异步写入完成即返回，写入操作在线程池中延迟执行。LLM 调用路径与指标写入路径完全解耦

#### `AiCallRecord` — AI 调用记录值对象（class，归属 `ai-impl/metrics/`）

**职责**：封装一次 AI 能力调用的全部可观测性字段，与 `AiCallLogEntity` 字段对等。作为 `AiMetricsCollector` 的输入参数与持久化数据源。

**字段定义**（与 `AiCallLogEntity` 对等，不包含 JPA 主键）：

| 字段 | 类型 | 说明 |
|------|------|------|
| callTime | LocalDateTime | 调用时间 |
| capabilityId | String | 能力标识 |
| capabilityName | String | 能力名称，由工厂方法内部通过 `capabilityNameMapping`（`Map<String, String>`，从 YAML `ai.capability.name-mapping` 加载或启动时从 `CapabilityExecutor.getCapabilityId()` 自动补全）根据 `capabilityId` 自动解析；解析失败时回退到 `capabilityId` 作为显示名 |
| visitId | String | 关联就诊标识（可空） |
| patientId | String | 患者标识（可空） |
| departmentId | String | 科室标识（可空），用于按科室维度分析 |
| callerRole | String | 调用方角色 |
| callerId | String | 调用方标识 |
| inputSummary | String | 输入摘要 |
| outputSummary | String | 输出摘要 |
| degraded | boolean | 是否降级 |
| degradationReason | String | 降级原因（可空） |
| elapsedMs | long | 耗时毫秒 |
| errorCode | String | 错误码（可空） |
| errorMessage | String | 错误消息（可空） |
| modelId | String | 模型标识 |
| retryCount | int | 重试次数。**结构化调用回退路径语义说明**：当 `structuredChat()` 因 `StructuredOutputNotSupportedException` 回退到 `chat()` + `StructuredOutputParser.parse()` 时，`retryCount` 仅反映 chat 回退阶段的 LLM 重试次数（如 HTTP 5xx 自动重试 1 次），**不包含** `structuredChat()` 阶段已发生的内部重试。此差异是已知的可观测性精度约束——回退路径的 `retryCount` 值偏低（相当于"最低重试次数"的下限估计），按 `retryCount` 维度聚合的模型性能分析将产生系统性偏低偏差。缓解措施：(1) 若需精确对齐，可在 `StructuredOutputNotSupportedException` 中内嵌 `originalRetryCount` 字段，回退路径累加此值；(2) 当前设计接受此差异，在指标报表中按 `capabilityId` 聚合时需注意回退路径占比高的能力（如模型原生不支持 JSON mode）的 `retryCount` 聚合值偏低 |
| sessionId | String | 会话标识（可空） |
| promptVersion | Integer | Prompt 模板版本号（可空），用于 A/B 实验效果分析 |
| promptTokens | Integer | Prompt Token 用量 |
| completionTokens | Integer | Completion Token 用量 |
| totalTokens | Integer | 总 Token 用量 |

**当前工厂方法签名（旧多参数版本）**——§4.1 伪代码全文当前使用此版本，`sentinelReason` 为可选参数，位于参数列表末尾，默认 null。当 `sentinelReason="EXPERIMENT_ASSIGN_ERROR"` 时内部将 `promptVersion` 覆写为 -1 哨兵值：
```java
// 当前旧多参数签名（§4.1 全部调用点使用此版本）
// capabilityName 由工厂方法内部根据 capabilityId 从能力名称映射表自动解析（见字段定义说明），
// 工厂方法签名中不单独列出该参数；§4.1 所有调用点传入 capabilityId 后 capabilityName 自动填充
static AiCallRecord success(String capabilityId, LocalDateTime callTime, long elapsedMs,
    String departmentId, String modelId, int retryCount,
    @Nullable Integer promptTokens, @Nullable Integer completionTokens,
    String inputSummary, String outputSummary,
    @Nullable String visitId, @Nullable String patientId, @Nullable String sessionId,
    String callerRole, String callerId,
    @Nullable Integer promptVersion, @Nullable String sentinelReason)

static AiCallRecord failure(String capabilityId, LocalDateTime callTime, long elapsedMs,
    String errorCode, String errorMessage,
    String departmentId, String inputSummary,
    @Nullable String visitId, @Nullable String patientId, @Nullable String sessionId,
    String callerRole, String callerId,
    @Nullable Integer promptVersion, @Nullable String sentinelReason)

static AiCallRecord degraded(String capabilityId, LocalDateTime callTime, long elapsedMs,
    String degradationReason, @Nullable String modelId,
    String departmentId, String inputSummary,
    @Nullable String visitId, @Nullable String patientId, @Nullable String sessionId,
    String callerRole, String callerId,
    @Nullable String outputSummary, @Nullable Integer promptVersion, @Nullable String sentinelReason)
```

**简化签名（Phase 5 第二阶段重构目标，三期迁移计划见 §3.1「构造器参数数量说明」）**——使用 `CallContext` 值对象降维，`sentinelReason` 为可选参数，位于参数列表末尾，默认 null：
```java
// 简化签名（使用 CallContext 聚合 9 个业务上下文字段）
// capabilityName 由工厂方法内部根据 capabilityId 自动解析（同旧多参数版本）
static AiCallRecord success(String capabilityId, LocalDateTime callTime, long elapsedMs,
    String modelId, int retryCount, @Nullable LlmChatUsage tokenUsage,
    CallContext callContext, @Nullable String sentinelReason)

static AiCallRecord failure(String capabilityId, LocalDateTime callTime, long elapsedMs,
    String errorCode, String errorMessage,
    CallContext callContext, @Nullable String sentinelReason)

static AiCallRecord degraded(String capabilityId, LocalDateTime callTime, long elapsedMs,
    String degradationReason, @Nullable String modelId,
    CallContext callContext, @Nullable String sentinelReason)
```
所有工厂方法的 `callTime` 参数类型统一为 `LocalDateTime`，管线伪代码中传入 `LocalDateTime.now()`（**时间来源约定**：使用系统本地时间（服务器时区 `Asia/Shanghai`），不要求 UTC 转换。`LocalDateTime.now()` 取系统默认时区的当前日期时间，时区策略在部署时统一约定为北京时间（CST, UTC+8）。若后续引入多时区部署（如跨地域数据中心），需在 `AiPlatformConfig` 中显式配置时区规则并通过 `Clock` 注入统一的时间源，确保各实例的 `callTime` 使用同一时区参考系）；`elapsedMs` 参数类型统一为 `long`，从 `System.currentTimeMillis() - startTime` 计算。

**字段填充策略**：
- `capabilityId`、`modelId`、`elapsedMs`、`degraded`：由管线执行过程中直接获取
- `capabilityName`：由 `AiCallRecord` 工厂方法内部根据 `capabilityId` 从映射表 `capabilityNameMapping` 自动解析。映射表在 `AiPlatformConfig` 构建（从 YAML `ai.capability.name-mapping` 加载，或启动时从所有 `CapabilityExecutor.getCapabilityId()` 扫描并生成同名字符串作为显示名称）。对于未在映射表中注册的 `capabilityId`（如过渡期新注册的能力），回退使用 `capabilityId` 本身作为 `capabilityName`
- `callerRole`、`callerId`：在 `AbstractCapabilityExecutor.execute()` 入口处（`supplyAsync()` 之前，容器线程）从 Spring `SecurityContextHolder` 中提取，以参数形式传入 `doExecuteInternal()` 和 `doDegrade()`，最终传入 `AiCallRecord` 工厂方法。不在 lambda 内部访问 ThreadLocal
- `inputSummary`：在 `AbstractCapabilityExecutor.execute()` 入口处（`supplyAsync()` 之前）调用 `StringUtils.truncate(request.toString(), 500)` 预计算，以参数形式传入 `doExecuteInternal()` 和 `doDegrade()`. ⚠️ **敏感信息治理**：`toString()` 实现必须显式排除敏感字段（如身份证号、电话号码、患者姓名等），通过 `@ToString.Exclude`（Lombok）或自定义 `toString()` 实现。各能力 DTO 的 `toString()` 在继承 `AiRequestBase` 时统一启用敏感字段排除
- `outputSummary`：取 LLM 响应的结构化输出后摘要截断；降级路径中来自 `LocalRuleFallback` 的输出摘要
- `visitId`、`patientId`、`sessionId`、`departmentId`：从业务请求 DTO 中提取（各能力 DTO 均继承 `AiRequestBase` 基类携带这些字段）
- `promptVersion`：来自 `ExperimentAssignment.getTargetPromptVersion()`，未命中实验时为空
- `errorCode`、`errorMessage`：LLM 调用失败或解析失败时记录，成功时为空
- `tokenUsage` 系列：从 `LlmResponse` 中提取
- `retryCount`：由 `LlmChatService` 实现层内部重试计数器提供

#### `AiRequestBase` — AI 能力请求基类（abstract class，归属 `ai-api/dto/base/`）

**职责**：所有 AI 能力业务请求 DTO 的基类，封装跨能力通用的上下文字段。各能力的具体请求 DTO（如 `TriageRequest`、`PrescriptionCheckRequest` 等）均继承此类。

**公共字段**：
| 字段 | 类型 | 说明 |
|------|------|------|
| visitId | String | 关联就诊标识（可空） |
| patientId | String | 患者标识（可空） |
| sessionId | String | 会话标识（可空） |
| departmentId | String | 科室标识（可空），用于 Prompt 模板按科室维度检索 |

**类型定位**：使用抽象类而非接口，因为字段需要被具体子类直接继承且允许增加公共构造/工厂方法。归属 `ai-api/dto/base/` 子包。

**现有 DTO 影响评估与向后兼容策略**：
- **现状**：当前代码库中 13 项 AI 能力的请求 DTO（`TriageRequest`、`DiagnosisRequest` 等）**均不存在** `visitId`/`patientId`/`sessionId`/`departmentId` 字段，且多为空类或仅含 1 个业务字段。这些 DTO 未继承任何公共基类，也未定义与就诊上下文相关的任何字段。`AiRequestBase` 基类当前**不存在于代码库中**，管线伪代码中 `request.getDepartmentId()`/`getVisitId()`/`getPatientId()`/`getSessionId()` 的调用隐含了对该基类存在的假设
- **迁移代价**：引入 `AiRequestBase` 需要完成以下操作（仅 7 项底座能力 DTO 在 Phase 5 切流时涉及，6 项 Phase 4 DTO 暂通过独立提取方法获取公共字段）：
   1. 在 `ai-api/dto/base/` 包中创建 `AiRequestBase` 抽象类，声明 `visitId`/`patientId`/`sessionId`/`departmentId` 四个 protected 字段及 public getter 方法
   2. 为 7 项底座能力 DTO（`TriageRequest`、`PrescriptionCheckRequest`、`MedicalRecordGenRequest`、`PrescriptionAssistRequest`、`KbQueryRequest`、`ScheduleRequest`、`DiscussionConclusionRequest`）**逐 DTO 新增 4 个公共字段**（`visitId`/`patientId`/`sessionId`/`departmentId`），修改 extends 声明为 `extends AiRequestBase`，并确保各 DTO 的构造器通过 `super(visitId, patientId, sessionId, departmentId)` 向上传递基类字段值
   3. 检查各 DTO 是否存在字段名/类型不兼容（如某 DTO 的 `visitId` 类型为 `Long` 而非 `String`），若发现则统一为 `String`
   4. 为 7 项底座能力 DTO **调整 Jackson 序列化配置**——在 `AiRequestBase` 上标注 `@JsonIgnoreProperties(ignoreUnknown = true)`，在各子类上验证 `@JsonCreator` + `@ConstructorProperties` 或 Lombok `@Jacksonized` 注解兼容性
    5. **更新现有业务调用方**——检查 7 项底座能力 DTO 的现有调用方（Phase 2~4 业务模块），确保新增字段不影响已有序列化/反序列化行为。具体影响评估见下方"业务模块影响范围"段落
- **过渡策略**：采用分步迁移而非一次性全量修改：
   - Phase 5 底座切流时仅要求 7 项底座能力的 DTO 完成基类继承改造（`TriageRequest`、`PrescriptionCheckRequest`、`MedicalRecordGenRequest`、`PrescriptionAssistRequest`、`KbQueryRequest`、`ScheduleRequest`、`DiscussionConclusionRequest`）
   - 其余 6 项 Phase 4 能力的 DTO 暂维持现状，通过 `AiCallRecord` 字段填充逻辑中的独立提取方法获取公共字段，待后续统一改造
   - 基类引入后不影响现有业务方接口（新增父类对接口契约无变更），序列化兼容性通过 Jackson `@JsonIgnoreProperties(ignoreUnknown = true)` 注解保障

- **DTO 业务字段补齐计划**：7 项底座能力 DTO 在继承 `AiRequestBase` 的同时，需根据 §3.11 各能力特化表定义的扩展字段完整补齐业务字段。当前代码库中这些 DTO 均不存在特化表所列的业务字段（如 `TriageRequest.complaint`、`PrescriptionCheckRequest.medications` 等），补齐属于**全新设计**而非"已有字段的继承迁移"。具体补齐方式：
   1. **新增业务字段声明**：各 DTO 继承 `AiRequestBase` 后，按特化表定义逐 DTO 新增 `@JsonProperty` 标注的业务字段及对应 getter/setter。若 DTO 使用 Lombok `@Data`，则新增字段自动生成 getter/setter，无需手动声明
   2. **构造器与 Jackson 注解适配**：若 DTO 使用全参构造器模式（含 `AiRequestBase` 基类字段），需确保 `@JsonCreator` + `@ConstructorProperties` 或 Lombok `@Jacksonized` 注解正确覆盖所有字段（含基类字段），避免 Jackson 反序列化时基类字段赋值失败
   3. **toString() 敏感字段排除**：在 `AiRequestBase` 及子类上通过 `@ToString.Exclude`（Lombok）排除敏感字段（如 `patientId`、就诊标识等），确保 `inputSummary` 计算不泄露患者隐私
   4. **各能力 DTO 字段状态标记**：§3.11 特化表中每个 DTO 的扩展字段按以下标记标注其状态：**⊕ 需新增**（当前代码库中不存在，为 Phase 5 底座新设计字段）、**△ 需改造**（代码库中存在但字段名/类型与设计不一致，需改造对齐）、**✓ 已有**（代码库中已有且与设计一致，无需修改）。7 项底座能力 DTO 当前代码库中**全部字段均为 ⊕ 需新增**状态
   - **`@JsonCreator` + `@ConstructorProperties` 继承兼容性说明**：`AiRequestBase` 作为 abstract class 应标注 `@JsonIgnoreProperties(ignoreUnknown = true)`。其子类（各能力 DTO）若使用 Lombok `@Jacksonized` + 全参数构造器模式，`@JsonCreator` 和 `@ConstructorProperties` 注解由 Lombok 自动生成在子类构造器上，Jackson 反序列化时基类字段通过子类全参构造器的对应参数完成赋值。若能力 DTO 不使用 Lombok 而手动声明构造器，则需保证子类构造器通过 `super(visitId, patientId, sessionId, departmentId)` 向上传递基类字段值，并在子类构造器上标注 `@JsonCreator`。此兼容性方案在以下 3 个测试场景中验证
   - **Jackson 兼容性测试要求**：在引入基类的同一 Changelist 中，为 7 项底座能力 DTO 分别覆盖以下 3 个真实兼容性验证场景：
     1. **旧 JSON → 新 DTO**：将旧形态 JSON（无 AiRequestBase 字段）反序列化为新 DTO 实例，断言 `departmentId`/`visitId`/`patientId`/`sessionId` 为 null（对应 `ignoreUnknown=true` 正确工作）且业务字段正确填充
     2. **新 JSON → 旧消费者**：将新形态 JSON（含 AiRequestBase 字段）反序列化为旧 DTO 实例，断言旧消费者侧不因新增字段产生异常——此场景验证前端/中间件在过渡期仍可消费旧形态
     3. **新 JSON → 新 DTO（含基类继承）**：验证 `AiRequestBase` 子类上 `@JsonCreator` + `@ConstructorProperties` 或 Lombok `@Jacksonized` 注解正确工作，反序列化后基类字段（`departmentId` 等）正确赋值而非 null
   场景 1 验证向后兼容（旧数据可读入新对象），场景 2 验证向前兼容（新数据不被旧消费者拒绝），场景 3 验证基类字段注解的正确性——三者共同覆盖了基类引入可能引发的全部序列化/反序列化风险。
- **业务模块影响范围**：引入 `AiRequestBase` 后，业务模块仍通过具体 DTO 类型（如 `TriageRequest`）引用实例，不直接引用 `AiRequestBase` 类型。影响仅限于以下场景：
  - 业务模块已有 `instanceof TriageRequest` 检查——不受影响（子类 instanceof 父类为 false，但 `TriageRequest instanceof AiRequestBase` 不引发编译错误，仅 JSON 序列化/反序列化行为改变）
  - 业务模块通过 `ObjectMapper` 自行序列化/反序列化 DTO——需确保 ObjectMapper 配置了 `@JsonIgnoreProperties(ignoreUnknown = true)`（Spring Boot 默认开启），否则 JSON 中不存在的基类字段将导致反序列化失败
  - 业务模块直接使用 DTO 的 `visitId`/`patientId`/`sessionId` getter——不受影响（仍通过子类继承获取）
  - 影响范围评估结论：基类引入对 13 项 DTO 的 90% 以上业务调用方无影响，仅需要更新各 DTO 的类声明（`extends AiRequestBase`）并确保 ObjectMapper 配置兼容
- **风险缓解**：引入基类前在单元测试层添加反序列化兼容性测试，验证新旧 DTO 形态下的 JSON 互读

**DTO 改造工作量概览表**（适用于 Phase 5 底座切流前的 7 项底座能力 + 6 项薄适配器 DTO 的初始补齐，Jackson 注解含 `@JsonIgnoreProperties(ignoreUnknown=true)` 及必要的 `@JsonProperty` / `@JsonCreator` / `@ConstructorProperties`；底座能力 DTO 还需继承 `AiRequestBase`。预计代码行数为保守估算，含字段声明、getter/setter、构造器及 Jackson 注解的模板代码）：

| 能力标识 | DTO 类型 | 新增字段数 | 需新增 Jackson 注解 | 需更新调用方 | 预计代码行数 | 备注 |
|---------|---------|-----------|-------------------|------------|------------|------|
| TRIAGE | `TriageRequest` | 4（complaint/symptomDescription/medicalHistory/vitalSigns）+ 4 AiRequestBase 继承字段 | `@JsonIgnoreProperties` + `@JsonCreator` + `@ConstructorProperties` | 否（新增字段，现有调用方不受影响） | ~80 行 | 继承 AiRequestBase |
| TRIAGE | `TriageResponse` | 1（urgencyLevel）+ 1 现有字段对齐（reason） | `@JsonIgnoreProperties(ignoreUnknown=true)` | 需评估下游消费者对新增字段的序列化兼容性 | ~40 行 | RecommendedDepartment 保持现有结构 |
| RX_AUDIT | `PrescriptionCheckRequest` | 3（medications/patientInfo/diagnosisCodes）+ 4 AiRequestBase 继承字段 | 同 TRIAGE | 否 | ~90 行 | 含 PatientInfo 值对象定义(~30行) |
| RX_AUDIT | `PrescriptionCheckResponse` | 3（overallStatus/checks/summary） | `@JsonIgnoreProperties(ignoreUnknown=true)` + `@JsonProperty` 在枚举字段上 | 否 | ~50 行 | 含 CheckResult 内嵌类(~20行) |
| MEDICAL_RECORD_GEN | `MedicalRecordGenRequest` | 6（chiefComplaint/presentIllness/pastHistory/physicalExamination/auxiliaryExaminations/diagnosis）+ 4 AiRequestBase 继承字段 | 同 TRIAGE | 否 | ~100 行 | auxiliaryExaminations 为 List 类型 |
| MEDICAL_RECORD_GEN | `MedicalRecordGenResponse` | 3（recordContent/structuredSections/generatedAt） | `@JsonIgnoreProperties(ignoreUnknown=true)` + `@JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")` 在 generatedAt 上 | 否 | ~60 行 | 含 RecordSection 内嵌类(~20行) + SectionType 枚举(~20行) |
| RX_ASSIST | `PrescriptionAssistRequest` | 3（diagnosisCode/diagnosisName/patientInfo/existingMedications）+ 4 AiRequestBase 继承字段 | 同 TRIAGE | 否 | ~90 行 | 重用 PatientInfo 值对象 |
| RX_ASSIST | `PrescriptionAssistResponse` | 1（recommendations） | `@JsonIgnoreProperties(ignoreUnknown=true)` | 否 | ~50 行 | 含 MedicationRecommendation 内嵌类(~30行) + ConfidenceLevel 枚举(~15行) |
| KB_QUERY | `KbQueryRequest` | 3（queryText/queryScope/maxResults）+ 4 AiRequestBase 继承字段（含 AiRequestBase.departmentId） | 同 TRIAGE | 否 | ~70 行 | queryScope 为 KbQueryScope 枚举(~15行)；departmentId 由 AiRequestBase 继承，不重复声明为扩展字段 |
| KB_QUERY | `KbQueryResponse` | 2（answer/sources） | `@JsonIgnoreProperties(ignoreUnknown=true)` | 否 | ~50 行 | 含 KbSource 内嵌类(~20行) |
| SCHEDULE | `ScheduleRequest` | 6（departmentId/scheduleDate/shiftType/requiredDoctors/preferredDoctors/constraints）+ 4 AiRequestBase 继承字段 | 同 TRIAGE + `@JsonFormat(pattern="yyyy-MM-dd")` 在 scheduleDate 上 | 否 | ~110 行 | 含 ShiftType 枚举(~15行) + ScheduleConstraint 值对象(~15行) |
| SCHEDULE | `ScheduleResponse` | 1（schedule） | `@JsonIgnoreProperties(ignoreUnknown=true)` | 否 | ~40 行 | 含 ShiftAssignment 内嵌类(~20行) |
| DISCUSSION_CONCLUSION | `DiscussionConclusionRequest` | 3（discussionId/discussionType/transcripts）+ 4 AiRequestBase 继承字段 | 同 TRIAGE + `@JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")` 在 transcript.timestamp 上 | 否 | ~110 行 | 含 DiscussionType 枚举(~15行) + DiscussionTranscript 值对象(~25行) |
| DISCUSSION_CONCLUSION | `DiscussionConclusionResponse` | 6（summary/diagnosisConclusion/treatmentRecommendations/followUpPlan/pendingItems/confidenceLevel） | `@JsonIgnoreProperties(ignoreUnknown=true)` | 否 | ~80 行 | 含 ConfidenceLevel 枚举（与 RX_ASSIST 共用） |
| DIAGNOSIS | `DiagnosisRequest` | 1（symptomDescription）+ 4 AiRequestBase 继承字段 | 同 TRIAGE | 否（当前为空类，新增字段仅底座使用） | ~60 行 | 薄适配器 DTO；由 Phase 4 模块自行决定是否继承 AiRequestBase；⚠️ 4 AiRequestBase 继承字段仅在 Phase 4 模块决定继承时生效，底座切流初期不纳入改造范围 |
| ANALYSIS_REPORT_INSPECTION | `AnalysisReportForInspectionRequest` | 1（reportData）+ 4 AiRequestBase 继承字段 | 同 TRIAGE | 否 | ~60 行 | 薄适配器 DTO；⚠️ 4 AiRequestBase 继承字段仅在 Phase 4 模块决定继承时生效，底座切流初期不纳入改造范围 |
| ANALYSIS_REPORT_LABTEST | `AnalysisReportForLabTestRequest` | 1（reportData）+ 4 AiRequestBase 继承字段 | 同 TRIAGE | 否 | ~60 行 | 薄适配器 DTO；⚠️ 4 AiRequestBase 继承字段仅在 Phase 4 模块决定继承时生效，底座切流初期不纳入改造范围 |
| IMAGE_ANALYSIS | `ImageAnalysisRequest` | 2（imageData/analysisType）+ 4 AiRequestBase 继承字段 | 同 TRIAGE | 否 | ~70 行 | 薄适配器 DTO；⚠️ 4 AiRequestBase 继承字段仅在 Phase 4 模块决定继承时生效，底座切流初期不纳入改造范围 |
| RECOMMEND_EXAM | `RecommendExaminationRequest` | 2（diagnosisCode/patientInfo）+ 4 AiRequestBase 继承字段 | 同 TRIAGE | 否 | ~70 行 | 薄适配器 DTO；⚠️ 4 AiRequestBase 继承字段仅在 Phase 4 模块决定继承时生效，底座切流初期不纳入改造范围 |
| RECOMMEND_EXEC_ORDER | `RecommendExecutionOrderRequest` | 2（examinationList/patientInfo）+ 4 AiRequestBase 继承字段 | 同 TRIAGE | 否 | ~70 行 | 薄适配器 DTO；⚠️ 4 AiRequestBase 继承字段仅在 Phase 4 模块决定继承时生效，底座切流初期不纳入改造范围 |

**工作量汇总**：7 项底座能力 DTO（含 input + output）合计约 **1,130 行**，6 项薄适配器 DTO 合计约 **390 行**，两项合计约 **1,520 行**。所有扩充码均新增在已声明的 DTO 类中，不涉及删除或修改现有业务代码的字段声明。调用方是否需要更新取决于该 DTO 的序列化方式（Jackson 反序列化时新增字段被 `@JsonIgnoreProperties(ignoreUnknown=true)` 安全忽略，不产生兼容性问题）。**底座承担范围**：底座侧仅负责 7 项底座能力 DTO 的改造（~1,130 行），6 项薄适配器 DTO 的改造归属 Phase 4 模块，底座仅提供改造模板和验收测试

#### 3.5.1 调用方数据准备指引

本小节集中说明 13 项能力 DTO 的 4 个公共字段（`visitId`/`patientId`/`sessionId`/`departmentId`）在各业务模块作为 `AiService` 调用方时的实际填充来源、过渡期为空时底座的行为以及现有模块兼容性保护。

**公共字段数据来源总表**：

| 字段 | 7 项底座能力 DTO（继承 `AiRequestBase`） | 6 项薄适配器能力 DTO（Phase 4 模块维护） | HTTP 调用方 | MQ/定时任务调用方 |
|------|--------------------------------------|--------------------------------------|------------|----------------|
| `visitId` | DTO 全参构造器传入（调用方显式填充） | RequestContext 优先 → MQ 消息体字段 → null | `X-Visit-ID` Header | 从消息体提取后写入 DTO |
| `patientId` | DTO 全参构造器传入（调用方显式填充） | RequestContext 优先 → MQ 消息体字段 → null | `X-Patient-ID` Header | 从消息体提取后写入 DTO |
| `sessionId` | DTO 全参构造器传入（调用方显式填充） | RequestContext 优先 → MQ 消息体字段 → null | `X-Session-ID` Header | 从消息体/任务上下文提取后写入 DTO |
| `departmentId` | DTO 全参构造器传入（调用方显式填充） | RequestContext 优先 → DTO 字段 → null | `X-Department-ID` Header | 从消息体提取后写入 DTO |

**过渡期为空时底座行为**：

底座侧对所有 4 个公共字段采用 **null 安全**处理策略——字段为 null 时不影响管线执行，仅在 `AiCallRecord` 中对应字段记录为 null（数据库 `NULL`）。具体影响如下：

| 场景 | 影响 |
|------|------|
| `departmentId=null` | Prompt 模板检索回退到通用模板（无科室级模板时使用兜底 Prompt）；指标报表中的科室维度聚合显示为"未指定" |
| `visitId=null` | `AiCallRecord.visitId` 为 null，就诊维度分析不可用；不影响管线执行 |
| `patientId=null` | `AiCallRecord.patientId` 为 null，患者维度分析不可用；不影响管线执行 |
| `sessionId=null` | `ExperimentManager.assign()` 的哈希分桶输入中 `sessionId=null` 时，分桶仅依赖 `userId`，同一用户在同一会话的不同请求可能落入不同分桶；且 `AiCallRecord.sessionId` 为 null，会话维度分析不可用 |
| 全部为 null | 管线正常执行，所有降级/超时/重试行为不受影响 |

**现有模块兼容性保护**：

- 7 项底座能力 DTO 继承 `AiRequestBase` 后，现有模块调用方**无需立即修改**：新增的 4 个公共字段通过全参构造器传入，现有调用方可在构造 DTO 时传 `null`。底座管线通过 null-safe 提取逻辑（`doExtractDepartmentId()`/`doExtractVisitId()`/`doExtractPatientId()`/`doExtractSessionId()` 均返回 null 而非 NPE）确保过渡期兼容
- 6 项薄适配器 DTO 当前由 Phase 4 模块维护，底座切流初期不要求这些 DTO 继承 `AiRequestBase`。底座通过独立提取方法（`RequestContextUtils.extractFromRequestContext(Xxx)` → 回退到 `request` 中由调用方显式传递的字段）获取公共字段，调用方无需为薄适配器能力准备这些字段
- **过渡期保护开关**：在 `AiPlatformConfig` 中新增 `ai.platform.compatibility.null-safe-fields: true` 配置项（默认 `true`），控制底座是否启用 null 安全字段处理。若关闭（`false`），底座将尝试使用 `request.getDepartmentId()` 等方法提取字段，未实现这些方法的 DTO 将抛出 `NullPointerException`（用于测试期暴露调用方未填充字段的问题）。默认 `true` 确保生产环境过渡期零影响

#### `AiCallLogEntity` — AI 调用日志 JPA 实体（新增 `@Entity`，归属 `ai-impl/metrics/`）

**职责**：与 `AiCallRecord` 字段一一对应的 JPA Entity，映射到 `ai_call_log` 表，为 AI 调用日志提供持久化契约。

**字段定义**（与 `AiCallRecord` 对等，含 JPA 主键）：
| 字段名 | Java 类型 | 数据库类型 | 说明 |
|--------|----------|-----------|------|
| id | Long | BIGINT (PK, AUTO) | 主键 |
| call_time | LocalDateTime | DATETIME(3) | 调用时间（毫秒精度，`@Column(columnDefinition = "DATETIME(3)")`） |
| capability_id | String | VARCHAR(50) | 能力标识 |
| capability_name | String | VARCHAR(100) | 能力名称 |
| visit_id | String | VARCHAR(50) | 关联就诊标识（可空） |
| patient_id | String | VARCHAR(50) | 患者标识（可空） |
| department_id | String | VARCHAR(50) | 科室标识（可空），用于按科室维度分析 |
| caller_role | String | VARCHAR(20) | 调用方角色 |
| caller_id | String | VARCHAR(50) | 调用方标识 |
| input_summary | String | VARCHAR(500) | 输入摘要 |
| output_summary | String | VARCHAR(500) | 输出摘要 |
| degraded | boolean | TINYINT(1) | 是否降级 |
| degradation_reason | String | VARCHAR(200) | 降级原因（可空） |
| elapsed_ms | long | BIGINT | 耗时毫秒 |
| error_code | String | VARCHAR(20) | 错误码（可空） |
| error_message | String | VARCHAR(500) | 错误消息（可空） |
| model_id | String | VARCHAR(50) | 模型标识 |
| retry_count | int | INT | 重试次数 |
| session_id | String | VARCHAR(50) | 会话标识（可空） |
| prompt_version | Integer | INT | Prompt 模板版本号（可空），用于 A/B 实验效果分析 |
| prompt_tokens | Integer | INT | Prompt Token 用量（可空） |
| completion_tokens | Integer | INT | Completion Token 用量（可空） |
| total_tokens | Integer | INT | 总 Token 用量（可空） |

> **数据库类型兼容性说明**：以上数据库类型声明以 **MySQL 方言**表述（如 `VARCHAR(50)`、`TINYINT(1)`、`DATETIME(3)`）。实际实现者需根据项目所选数据库（如 PostgreSQL 对应 `VARCHAR(50)` → `VARCHAR(50)` 兼容、`TINYINT(1)` → `BOOLEAN`、`DATETIME(3)` → `TIMESTAMP(3)`；Oracle 对应 `VARCHAR(50)` → `VARCHAR2(50)`、`TINYINT(1)` → `NUMBER(1)`、`BIGINT` → `NUMBER(19)`）替换为对应方言类型。此说明适用于本文档中所有 JPA Entity 字段定义表（含 `PromptTemplate`、`Experiment`、`ExperimentGroup` 等）。`@Column(columnDefinition = "...")` 注解中的方言值需在切换数据库时同步替换

**表索引策略**：
- `idx_call_time` — `(call_time DESC)`：按时间降序查询近期调用记录
- `idx_capability_call_time` — `(capability_id, call_time DESC)`：按能力标识 + 时间查询
- `idx_degraded_call_time` — `(degraded, call_time DESC)`：降级记录维度查询
- `idx_model_call_time` — `(model_id, call_time DESC)`：模型维度查询与统计
- `idx_caller_role_call_time` — `(caller_role, call_time DESC)`：角色维度查询与统计
- `idx_visit_id` — `(visit_id)`：按就诊标识查询关联的 AI 调用
- `idx_patient_id` — `(patient_id)`：按患者标识查询
- `idx_department_call_time` — `(department_id, call_time DESC)`：按科室维度查询与统计
- `idx_prompt_version_call_time` — `(prompt_version, call_time DESC)`：按 Prompt 版本号 + 时间降序查询与效果分析。`prompt_version` 为 Integer 类型（通常仅几十到几百个不同值），单列索引选择性差，追加 `call_time DESC` 后 MySQL 优化器可执行索引范围扫描 + 行定位，避免全表扫描
- `idx_capability_prompt_call_time` — `(capability_id, prompt_version, call_time DESC)`：覆盖 A/B 实验分析的核心查询模式——按能力维度查询指定 Prompt 版本号在时间范围内的调用记录。此索引支持 `(capability_id, prompt_version)` 等值过滤 + `(call_time)` 范围排序

**数据保留与清理策略**：`ai_call_log` 表随运行时间持续增长，需制定数据生命周期的治理方案。Phase 5 的初始策略如下：
- **分区策略**：按 `call_time` 按月分区（MySQL `RANGE COLUMNS` 分区，每月一个分区），分区命名格式 `p_202601`、`p_202602`。创建表时预先创建未来 3 个月的分区，并由定时任务每月自动创建下月分区。分区键与 `idx_call_time` 索引协同，按时间范围查询时自动分区裁剪
- **保留期限**：`ai_call_log` 数据保留 **180 天**（约 6 个月），超过保留期限的分区直接 `DROP PARTITION` 清理。180 天覆盖 AI 调用效果回溯（A/B 实验评估需跨 1~3 个月）、投诉审计（法务要求 90 天）和运维排障（30 天）的需求。清理操作通过 `@Scheduled(cron = "0 0 3 * * ?")`（每日凌晨 3 点）执行，调用 `ALTER TABLE ai_call_log DROP PARTITION p_yyyyMM` 删除超过 180 天的分区
- **清理的副作用控制**：`DROP PARTITION` 为 DDL 操作，执行期间会对表施加短暂的 Metadata Lock（MDL），影响同一表上的 DML 操作。缓解措施：(1) 清理安排在凌晨低峰期；(2) 在 `@Scheduled` 方法前检查当前连接数，高负载时跳过本次清理；(3) 使用 `pt-online-schema-change` 或直接 `DROP PARTITION`——Phase 5 选择直接 `DROP PARTITION`，因其执行时间通常在毫秒级（仅删除元数据不扫描数据页），对生产影响可接受
- **分区前的数据清理转储**：在删除分区前，`@Scheduled` 方法将过期分区的数据汇总为月度统计记录（按 capabilityId + call_time 月份聚合的成功/失败/降级计数 + P50/P95/P99 耗时分布）写入独立的 `ai_call_log_stats` 表（保留 3 年），确保精细统计数据在分区删除后仍有汇总级可查

**AiCallLogStats JPA Entity 定义**：
与 `ai_call_log_stats` 表映射的 JPA Entity，按月汇总维度存储：

| 字段名 | Java 类型 | 数据库类型 | 说明 |
|--------|----------|-----------|------|
| id | Long | BIGINT (PK, AUTO) | 主键 |
| capability_id | String | VARCHAR(50) | 能力标识（非空） |
| stat_month | String | VARCHAR(7) | 统计月份，格式 "YYYY-MM"（非空） |
| total_calls | long | BIGINT | 总调用次数 |
| success_count | long | BIGINT | 成功次数 |
| degraded_count | long | BIGINT | 降级次数 |
| failure_count | long | BIGINT | 失败次数 |
| avg_elapsed_ms | double | DOUBLE | 平均耗时（毫秒） |
| p50_elapsed_ms | double | DOUBLE | P50 耗时 |
| p95_elapsed_ms | double | DOUBLE | P95 耗时 |
| p99_elapsed_ms | double | DOUBLE | P99 耗时 |

**查询索引**：`idx_stats_capability_month( capability_id, stat_month DESC )` — 按能力维度查询月度统计。
**写入时机**：每日凌晨分区清理定时任务在 `DROP PARTITION` 之前执行聚合 INSERT。聚合 SQL 伪代码：
```sql
INSERT INTO ai_call_log_stats (capability_id, stat_month, total_calls,
    success_count, degraded_count, failure_count, avg_elapsed_ms,
    p50_elapsed_ms, p95_elapsed_ms, p99_elapsed_ms)
SELECT capability_id, DATE_FORMAT(call_time, '%Y-%m') AS stat_month,
    COUNT(*),
    SUM(CASE WHEN degraded = 0 AND error_code IS NULL THEN 1 ELSE 0 END),
    SUM(CASE WHEN degraded = 1 THEN 1 ELSE 0 END),
    SUM(CASE WHEN error_code IS NOT NULL AND degraded = 0 THEN 1 ELSE 0 END),
    AVG(elapsed_ms),
    -- 百分位数计算使用 MySQL 兼容的 ROW_NUMBER() + COUNT(*) 方式
    -- PERCENTILE_CONT 为 SQL Server/Oracle 函数，MySQL 8.0 原生不支持
    -- 替代实现：子查询中按 elapsed_ms 排序后计算行号比值，示例：
    --   SELECT capability_id, stat_month,
    --     MAX(CASE WHEN rn <= CEIL(0.5 * cnt) THEN elapsed_ms END) AS p50_elapsed_ms
    --   FROM (SELECT ..., ROW_NUMBER() OVER (PARTITION BY capability_id, stat_month ORDER BY elapsed_ms) AS rn,
    --               COUNT(*) OVER (PARTITION BY capability_id, stat_month) AS cnt FROM ai_call_log ...) t
    --   GROUP BY capability_id, stat_month
    -- 以下保留 PERCENTILE_CONT 语法作跨数据库兼容性参考，实际实现时替换为 MySQL 兼容写法
    0 AS p50_elapsed_ms,
    0 AS p95_elapsed_ms,
    0 AS p99_elapsed_ms
FROM ai_call_log
WHERE call_time >= #{partitionStart} AND call_time < #{partitionEnd}
GROUP BY capability_id, stat_month
```
> **百分位数实现说明**：上述 SQL 中的 `PERCENTILE_CONT` 用于 Oracle/PostgreSQL 方言，MySQL 8.0 原生不支持。MySQL 兼容实现方案：在子查询中通过 `ROW_NUMBER() OVER (PARTITION BY capability_id, stat_month ORDER BY elapsed_ms)` 计算排名，结合 `COUNT(*) OVER (PARTITION BY ...)` 获取总数，外层子查询通过 `MAX(CASE WHEN rn <= CEIL(0.5 * cnt) THEN elapsed_ms END)` 提取中位数。或使用用户变量 + 自连接方式计算。PostgreSQL 可直接使用 `PERCENTILE_CONT`。实现阶段根据实际数据库方言选择替换。

- **运行时监控**：`AiCallLogRepository` 暴露 `countByCallTimeBefore(LocalDateTime cutoff)` 查询方法，在监控大盘上显示 `ai_call_log` 表的数据量与清理延迟。若数据量超过 500 万行且清理任务连续 3 天未执行，触发告警

**为何定义为独立 Entity 而非共用 `AiCallRecord`**：`AiCallRecord` 是值对象，作为内存中的数据传输载体。JPA Entity 需要携带 JPA 注解和索引声明，与纯值对象的职责分离，避免污染 `ai-api` 模块的依赖。

**过渡期兼容保证**：`DegradationContext` 的无参构造器必须永久保留，以确保 `FallbackAiService.applyStrategies()`（在 Phase 5 `applyStrategies()` 被移除之前）的零值构造行为不被破坏。具体约定如下：
1. **无参构造器不可移除**：`DegradationContext()` 无参构造器是 Phase 0 代码冻结的接口——`FallbackAiService.applyStrategies()` 在第 187 行通过 `new DegradationContext()` 创建零值实例。Phase 5 不得修改此构造器的行为（新增字段均通过 Builder 或 setter 赋值，构造器内不包含任何初始化逻辑）
2. **新增字段默认值安全**：所有新增字段在无参构造器中取语言默认值（数值=0/0L，对象=null），确保 Phase 0 代码通过 `new DegradationContext()` 创建的空上下文不会产生字段初始化副作用
3. **`applyStrategies()` 移除的前置条件**：`FallbackAiService.applyStrategies()` 方法的移除必须以"Phase 5 底座已完全切流、Phase 0 代码冻结已解除"为前置条件，而非依赖概念冻结。底座切流验证通过前，`applyStrategies()` 方法体保留（标记 `@Deprecated`），避免降级路径在新旧代码共存期间出现行为空白

#### `SlidingWindowMetricsStore` — 调用指标滑动窗口存储（class，归属 `ai-impl/metrics/`）

**职责**：为每个能力标识维护独立的调用指标滑动窗口（时间窗口可配置，默认 60 秒），供 `DegradationStrategy` 实现读取实时调用数据。

**核心方法**：
```
recordSuccess(capabilityId, elapsedMs)
    记录一次正常成功调用（调用 LLM 后返回正常结果）

recordDegraded(capabilityId, elapsedMs)
    记录一次降级兜底调用（降级路径中本地规则执行成功）。与 recordSuccess 区分存储，使 CircuitBreakerDegradationStrategy 在计算失败率时只考虑"真正"的成功调用，不被降级路径的成功次数稀释

recordFailure(capabilityId)
    记录一次失败调用（LLM 调用失败且无本地规则兜底）

getFailureRate(capabilityId) → double
    返回当前窗口内的失败率（仅统计 recordSuccess 和 recordFailure，排除 recordDegraded）
    **使用场景**：供 `CircuitBreakerDegradationStrategy` 判定熔断状态。熔断器关注的是"LLM 调用本身的成功率"，降级兜底成功不应稀释失败率，否则熔断器无法真实反映模型端点的健康程度

getEffectiveFailureRate(capabilityId) → double
    返回有效失败率（recordFailure / (recordSuccess + recordDegraded + recordFailure)），将降级兜底成功计入分母
    **使用场景**：供 `TimeoutDegradationStrategy` 作为参考指标。超时策略关注的是"整体退化程度"而非纯粹的 LLM 成功率，降级兜底增多本身也是系统退化的信号

getAverageElapsed(capabilityId) → double
    返回当前窗口内的平均耗时

buildDegradationContext(capabilityId, requestType) → DegradationContext
    从窗口数据构建完整的 DegradationContext 实例，窗口指标包含 successCount、failureCount、degradedCount 等细分字段
```

**线程安全**：内部使用 `ConcurrentHashMap<String, Deque<WindowedEvent>>`（滑动窗口事件队列），每个能力标识的 Deque 操作使用独立的 `synchronized` 块统一保护。**锁协议**如下：
- **惰性淘汰与写入统一加锁**：`recordSuccess()/recordDegraded()/recordFailure()` 中惰性淘汰（从头移除过期事件）和尾部追加新事件在同一个 `synchronized (deque)` 块内执行，消除两线程的淘汰操作对同一 Deque 头/尾并发操作的竞争条件
- **读取路径惰性淘汰**：`getFailureRate()/getEffectiveFailureRate()/getAverageElapsed()/buildDegradationContext()` 等读取方法在快照复制前执行惰性淘汰，同样在 `synchronized (deque)` 块内完成淘汰 + 快照复制（通过 `toArray()` 或流式操作），确保读取端看到的窗口数据一致
- **写-读不互斥**：写入和读取的锁范围均覆盖惰性淘汰 + 数据访问两个操作，但写锁和读锁使用同一个 Deque 实例的 `synchronized` 块，写入和读取在 Deque 级别互斥。此设计优先保证数据一致性，写入延迟增加在可接受范围（Deque 操作均为 O(1) 或 O(k) 淘汰，k 为单次淘汰的过期事件数，典型场景下数量级为 O(1)）
- **锁粒度演进路径**：若性能测试显示写-读互斥成为瓶颈，可升级为 `ReentrantReadWriteLock` 分离读写路径——读锁允许多线程同时快照复制，写锁独占时执行淘汰+追加。Phase 5 初始阶段采用统一 `synchronized` 以降低复杂度，待压测数据后再决策是否升级

**WindowedEvent 值对象**：
```java
class WindowedEvent {
    EventType type;         // NORMAL_SUCCESS / DEGRADED / FAILURE 三类
    long timestamp;         // System.currentTimeMillis() 事件发生时间戳
    long elapsedMs;         // 0（FAILURE 场景）或实际耗时 ms（NORMAL_SUCCESS / DEGRADED）
}
```

**滑动窗口边界判定规则**：采用**惰性淘汰**策略——每次向 Deque 尾部追加新事件时，从头部开始移除 `timestamp < System.currentTimeMillis() - windowSeconds * 1000` 的过期事件。此淘汰动作在 `recordSuccess()/recordDegraded()/recordFailure()` 方法入口处同步执行，不引入定时器线程。读取方法（`getFailureRate()`、`getAverageElapsed()`）在快照复制前同样执行一次惰性淘汰，确保统计一致性。

**快照策略触发条件**：快照复制仅在 `getFailureRate()`、`getEffectiveFailureRate()`、`getAverageElapsed()`、`buildDegradationContext()` 等**读取方法**调用时触发。`recordSuccess()`/`recordDegraded()`/`recordFailure()` 等写操作方法**不触发快照复制**——它们在加锁写入后直接返回，不执行数组复制。此策略避免每次 record 调用产生 O(n) 的内存复制开销，仅在统计读取时（调用频率远低于写入频率）执行复制，实现读写分离的最终目标。

**`max-events-per-capability` 上限行为**：当某个能力标识的事件队列长度超过 `max-events-per-capability`（默认 10000）时，采用**丢弃最旧事件**策略——在新事件入队前，从 Deque 头部移除过剩事件，而非拒绝新事件记录。选择"丢弃最旧"而非"拒绝新记录"的理由：(1) 降级策略依赖最新窗口数据，丢弃旧事件对判定准确性影响最小；(2) 拒绝新记录将导致当前调用指标丢失，而这正是降级策略最需要的数据点。此上限作为安全阀防止内存泄漏，正常负载下事件量不应达到此值（`10000 events / 60s ≈ 166 events/s` 每能力）。

**为何新增此类**：v1 设计中 `DegradationStrategy` 无法获取实时调用数据，导致新增策略（超时/熔断）成为死代码。`SlidingWindowMetricsStore` 作为数据中枢，使所有策略可基于统一的实时指标窗口做出判定。

### 3.6 结构化输出解析层

#### `StructuredOutputParser` — 结构化输出解析契约（interface，归属 `ai-impl/parser/`）

**职责**：定义从 LLM 原始文本输出中解析出 Java DTO 的统一协议。入参类型 `LlmChatResponse`（见 §3.2 `client/` 包定义）而非已废弃的 `LlmResponse`。

**协作对象**：
- 被 `CapabilityExecutor` 在结果解析步骤中调用
- `JsonStructuredOutputParser`（默认实现）：假设 LLM 输出为 JSON 格式，基于 Jackson 反序列化

### 3.7 本地规则降级层

#### `LocalRuleFallback` — 本地规则降级契约（interface，归属 `ai-impl/fallback/`）

**职责**：定义 AI 能力降级到本地规则校验时的执行入口。仅特定能力需要实现此接口（当前仅为 3.4.2 AI 处方审核）。

**协作对象**：
- 被 `CapabilityExecutor` 在降级兜底步骤中调用
- `PrescriptionLocalRuleFallback`（实现）：执行 3.4.2 规定的本地规则校验最小检查项

#### `PrescriptionLocalRuleFallback` — 处方审核本地规则降级实现（class，归属 `ai-impl/fallback/`）

**职责**：当 AI 处方审核底座管线 LLM 调用失败或触发降级时，由 `PrescriptionCheckCapabilityExecutor.doDegrade()` 调用此 Fallback 执行本地规则校验，返回降级后的审核结果。

**最小安全规则列表**（至少执行以下 3~5 项检查，规则数据来源为本地配置的药品数据库 + 药品说明书结构化数据）：

| 序号 | 检查项 | 规则描述 | 数据来源 |
|------|-------|---------|---------|
| 1 | **配伍禁忌检查** | 检查处方中任意两种药品之间是否存在已知配伍禁忌（如头孢类+酒精类），若存在冲突则标记为"不通过"并注明冲突药品对 | 本地配伍禁忌数据库（按药品编码索引），由管理端维护，支持增量更新 |
| 2 | **剂量范围检查** | 检查每种药品的单次剂量和日总剂量是否在药品说明书中规定的安全范围内（如布洛芬成人单次不超过 400mg，日总剂量不超过 1200mg），超出范围则标记为"需复核"并标注推荐剂量 | 药品说明书结构化数据表（含药品编码、最小单次剂量、最大单次剂量、最小日剂量、最大日剂量、儿童剂量规则等字段） |
| 3 | **重复用药检查** | 检查处方中是否存在两种或以上成分相同或药理作用相同的药品（如不同品牌的布洛芬制剂联合使用），若存在则标记为"需复核"并列出重复药品 | 药品成分映射表（按 ATC 分类编码或成分编码索引） |
| 4 | **过敏史检查** | 根据患者过敏史字段（从 `request.patientInfo.getAllergyInfo()` 获取，方法签名 `PatientInfo.getAllergyInfo(): List<String>`），检查处方中是否存在患者已知过敏的药品成分，若存在则标记"不通过"并注明过敏药品 | 入参 `request.patientInfo.getAllergyInfo()`，不依赖外部数据源 |
| 5 | **儿童/孕妇用药警示** | 若患者为儿童（`request.patientInfo.getAge() < 18`）或孕妇（通过 `request.patientInfo.getPregnancyStatus()` 判定为 EARLY/MID/LATE_PREGNANCY），检查处方中是否存在该人群慎用或禁用的药品，禁用药标记"不通过"，慎用药标记"需复核" | 特殊人群用药警示表（药品编码 × 人群类型 → 禁忌级别），由管理端维护 |

**执行失败时的安全策略**：
- 若数据源查询异常（数据库不可用、缓存失效），`PrescriptionLocalRuleFallback` 按**白名单模式**处理——所有检查项因数据缺失无法判定时，默认返回"通过"（允许用药），同时在返回结果中标记 `dataSourceFailed: true` 和 `fallbackReason: "LOCAL_RULE_DATA_UNAVAILABLE"`。此策略确保 AI 底座降级后不因本地规则数据不可用导致处方流程阻塞（临床场景可用性优先）
- 若部分检查项的数据可用但部分不可用，对可用项执行检查并返回部分检查结果，不可用项的判定结果置为 `CHECK_SKIPPED` 并标注原因

**执行伪代码**：
```
PrescriptionLocalRuleFallback.fallback(request):
    result = new PrescriptionCheckResult()
    result.dataSourceFailed = false
    result.source = "LOCAL_RULE"
    for each rule in [配伍禁忌检查, 剂量范围检查, 重复用药检查, 过敏史检查, 儿童/孕妇用药警示]:
        try:
            checkResult = rule.execute(request.medications, request.patientInfo)
            result.addCheckResult(checkResult)
        catch (DataSourceException e):
            log.warn("本地规则数据源不可用: rule={}, error={}", rule.getName(), e.getMessage())
            result.addCheckResult(CheckResult.skipped(rule.getName(), "数据源不可用"))
            result.dataSourceFailed = true
    if result.dataSourceFailed && allChecksSkipped():
        result.fallbackReason = "LOCAL_RULE_DATA_UNAVAILABLE"
        // 所有检查均因数据源不可用而跳过 → 返回"通过"以确保处方流程不阻塞
        return AiResult.success(result)
    return AiResult.success(result)
```

### 3.8 降级策略扩展

#### `SlidingWindowMetricsStore` 与降级策略的协作模式

v1 设计中，`FallbackAiService.applyStrategies()` 创建空值 `DegradationContext`，导致策略实现无法获取调用耗时窗口数据或失败率数据，形成死代码。

v2 修正：降级判定移入编排层管线内部，`FallbackAiService` 剥离策略调用职责。流程如下：

```
CapabilityExecutor.execute(request):
  1. 调用前: SlidingWindowMetricsStore.buildDegradationContext(capabilityId, requestType) → context
  2. 遍历 DegradationStrategy 链（按 `default` 方法 `getOrder()` 升序）:
     - 任一 shouldDegrade(context) 返回 true → 跳过 LLM 调用，直接降级
     - 全部返回 false → 继续 LLM 调用
  3. LLM 调用后: 记录结果到 SlidingWindowMetricsStore.recordSuccess/Failure()
```

此模式下：
- `DegradationContext` 由 `SlidingWindowMetricsStore` 根据实时窗口数据填充，策略实现获取到真实的 `invocationCount`、`failureCount`、`elapsedTime`、`lastFailureTime`
- `FallbackAiService.applyStrategies()` 被移除，`FallbackAiService` 不再持有或管理 `DegradationStrategy` 列表，其职责收敛为：`AiService` 降级装饰（调用失败时返回 `AiResult.degraded()`） + `@Primary` Bean 装配协调。降级判定统一在 `CapabilityExecutor` 管线内完成

#### `TimeoutDegradationStrategy` — 超时降级策略（class，归属 `ai-impl/degradation/`）

**职责**：基于 `DegradationContext` 中的最近调用耗时信息判定是否触发降级。若某能力的最近 N 次（可配置）调用平均耗时超过其硬超时阈值的 80%，触发降级。

**协作对象**：
- 实现 `DegradationStrategy`
- 读取 `DegradationContext` 中的 `elapsedTime`、`requestType`、`invocationCount`、`lastFailureTime`
- 数据的实时性由 `SlidingWindowMetricsStore` 确保

#### `CircuitBreakerDegradationStrategy` — 熔断降级策略（class，归属 `ai-impl/degradation/`）

**职责**：当某能力的最近调用失败率超过阈值（可配置，默认 50%）时，触发熔断——在熔断窗口（默认 30 秒）内所有对该能力的调用直接走降级路径。

**协作对象**：
- 实现 `DegradationStrategy`
- 读取 `DegradationContext` 中的 `failureCount` 和 `invocationCount`；其中 `failureCount` 的数据源来自 `SlidingWindowMetricsStore.getFailureRate()`，熔断器通过 `buildDegradationContext()` 获取实时失败率。此依赖链在 §1.5.3 分布式重构优先级中显式记录
- 内部维护每个能力标识的熔断状态与失败计数滑动窗口

**状态模型**：
```
CLOSED ←→ OPEN ←→ HALF_OPEN
  - CLOSED: 正常通过，记录成功/失败
  - OPEN: 所有请求直接降级，不尝试 LLM 调用
  - HALF_OPEN: 允许一次探测请求通过
    - 探测成功 → 回到 CLOSED
    - 探测失败 → 回到 OPEN
  触发条件:
  - CLOSED → OPEN: getFailureRate(capabilityId) ≥ 阈值（默认 50%）
  - OPEN → HALF_OPEN: 熔断窗口到期（默认 30 秒），无需人工干预
```

**熔断器作用域粒度与多端点场景规则**：
`CircuitBreakerDegradationStrategy` 的熔断状态以 `endpointId` 为作用域粒度（而非 `capabilityId`），即每个 `endpointId` 拥有独立的 CLOSED/OPEN/HALF_OPEN 状态机和失败率滑动窗口。当 `ModelRouter.route()` 返回的 `ModelRoute` 指向多个 `endpointId`（多端点负载均衡场景）时，适用以下规则：

| 规则 | 描述 |
|------|------|
| **熔断器状态隔离** | 每个 endpoint 独立维护熔断状态。endpoint A 熔断不影响 endpoint B；`ModelRouter.route()` 返回多个 endpoint 时，`ModelEndpointHealthManager.getState()` 逐 endpoint 检查 |
| **路由层智能跳过** | `CapabilityExecutor.doExecuteInternal()` 在 `modelRouter.route()` 返回 `List<ModelRoute>` 时，逐 route 检查对应 endpoint 的健康状态与熔断器状态：若某 endpoint 为 OPEN 或 UNAVAILABLE，跳过当前 route 尝试下一个；若全部 route 均不可用，触发降级 `DegradationReason.NO_AVAILABLE_ROUTE` |
| **多端点变体（容灾）** | 为应对供应商端点单点故障，`ModelRouter.route()` 支持返回同一能力的多个 `ModelRoute`（如 qwen-plus 主端点 + 备用端点）。熔断器按 endpointId 独立熔断，避免一个端点不可用导致整个能力无法使用 |
| **熔断与端点健康管理的协作** | 当 `CircuitBreakerDegradationStrategy` 判定某个 endpoint 进入 OPEN 状态时，同步通知 `ModelEndpointHealthManager.recordCallResult(endpointId, false, elapsedMs)` 触发端点健康状态转换。`ModelEndpointHealthManager` 中的 DEGRADED→UNAVAILABLE 转换规则同样以 endpointId 独立作用，不跨 endpoint 传播 |

**设计意图**：endpoint 级别的熔断粒度使熔断器在以下场景表现正确：
- 能力 A 配置了 endpoint P1（主）和 P2（备用），P1 触发熔断后流量自动切换到 P2，能力 A 不降级
- 能力 A 和 B 共享同一 endpoint P3，P3 熔断时 A 和 B 同时降级（此行为符合预期——端点不可用时共享端点的所有能力均应容错）
- 若需要按 `capabilityId` 而非 `endpointId` 独立熔断（如能力 A 虽然与 B 共享 endpoint 但 A 的流量被其他端点的降级策略保护），可在 `CircuitBreakerDegradationStrategy` 中配置 `strategy-scope: per-capability` 覆盖默认的 per-endpoint 粒度。配置方式见 §9.5 `degradation.circuit-breaker.strategy-scope` 项

**状态可观测性**：
`CircuitBreakerDegradationStrategy` 新增 `getState(String endpointId)` 方法，返回熔断器指定端点的瞬时状态：

```java
// 熔断器状态枚举
enum CircuitBreakerState {
    CLOSED,
    OPEN,
    HALF_OPEN
}

// 状态查询接口
CircuitBreakerState getState(String capabilityId)
```

运维侧可通过以下方式观测熔断器状态：
- **Micrometer Gauge**：`CircuitBreakerDegradationStrategy` 在初始化时向 `MeterRegistry` 注册 `Gauge`，命名为 `aimedical.ai.circuit-breaker.state`，tag 为 `capabilityId`。Gauge 值映射：CLOSED=0、HALF_OPEN=1、OPEN=2。运维可在监控大盘上按能力维度查询熔断器实时状态
- **JMX**：通过 `@ManagedOperation` 暴露 `getState()` 方法，运维工具可直接查询
- **日志**：熔断器状态转换时（CLOSED→OPEN 或 OPEN→HALF_OPEN→CLOSED）输出 INFO 日志，包含能力标识、转换前状态、转换后状态及触发原因

#### `DegradationContext` — 降级判定上下文（class implements `Serializable`，归属 `ai-api/degradation/`，扩展）

**职责**：Phase 0 定义为零值构造器骨架。Phase 5 扩展以下字段（保持向后二进制兼容）。声明 `implements Serializable` 以确保在分布式缓存、事件序列化等场景下的二进制兼容性。

**扩展字段**（新增 int 类型字段均使用包装类型 `Integer`，确保 Jackson 反序列化旧 JSON 时缺失字段为 null 而非 0，避免策略误判）：
| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| invocationCount | Integer | null | 该能力近窗口内调用次数 |
| lastFailureTime | long | 0L | 最近一次失败时间戳 epoch ms |
| elapsedTime | long | 0L | 最近一次调用耗时 ms |
| requestType | String | null | 能力标识，如 "TRIAGE"/"RX_AUDIT" |
| failureCount | Integer | null | 该能力近窗口内失败次数。使用 `Integer` 而非 `int`，使 Jackson 反序化旧 JSON 时 getter 返回 null 而非 0，策略实现可通过 `failureCount == null` 判断字段不存在 |
| departmentId | String | null | 科室标识，供降级策略按科室维度差异化判定；由 AbstractCapabilityExecutor.execute() 入口处在降级预检前通过 context.setDepartmentId(departmentId) 设置 |

**二进制兼容性分析**：
- **serialVersionUID**：声明 `private static final long serialVersionUID = 1L`（或显式声明与 Phase 0 生成值一致），确保序列化兼容
- **无参构造器保留**：新增字段取语言默认值（0 / 0L / null），旧代码反序列化后不会因缺少字段而抛出异常
- **Jackson 反序列化兼容性**：`DegradationContext` 标注 `@JsonIgnoreProperties(ignoreUnknown = true)`，确保新旧 JSON 互读时未知字段安全忽略。新增 int 字段（如 `invocationCount`、`failureCount`）**使用包装类型 `Integer` 而非原始类型 `int`**，理由：Jackson 反序列化旧 JSON（不含这些字段）时，原始 `int` 默认值为 0，0 可能被下游策略误判为"有 0 次调用"而非"数据不存在"；包装类型 `Integer` 默认为 null，策略实现中可通过 `context.getInvocationCount() == null` 区分"字段不存在"与"有 0 次调用"两种场景
- **新增字段默认值静默下界问题**：若某字段（如 `failureCount`）在旧序列化数据反序列后默认为 0，可能导致 `shouldDegrade()` 统计失准（熔断器认为失败率为 0%）。**缓解措施**：
  1. **构造时保证**：`builder()` 工厂方法生成实例，确保字段显式赋值；新增 `serializedTimestamp` 字段记录序列化时间戳，反序列化后校验时间新鲜度，超过 `DegradationContext.TTL`（默认 60 秒）的缓存数据视为过期，由 `SlidingWindowMetricsStore` 重新构建
  2. **校验时补偿**：`DegradationContext` 新增 `postDeserializationValidate()` 后置校验方法——检测到所有统计字段（`invocationCount`、`failureCount`、`elapsedTime`）均为默认值（0/0L）时，将 `requestType` 置空标记为"未初始化"，降级策略实现中识别此标记后回退到保守策略（不触发降级）
  3. **策略层防御**：降级策略内部同时使用字段 > 0 判定（适用于二值场景，新增 binary+百分比双模式）和 `DegradationContext.isFresh()` / `DegradationContext.isInitialized()` 守卫判定。对于百分比阈值场景（如熔断器 50% 阈值），若 `invocationCount` 为 0 或 `isInitialized()` 为 false，则不触发熔断（回退到 CLOSED 状态）。**不依赖字段绝对值的百分比计算**
- **缓存淘汰机制**：在 `AiPlatformConfig` 中配置 `degradation.context-ttl-seconds: 60`，`SlidingWindowMetricsStore.buildDegradationContext()` 返回前将当前时间戳写入 `serializedTimestamp`。反序列化后的 `DegradationContext` 若超过 TTL，由调用方（`CapabilityExecutor` 的降级预检步骤）丢弃并重新从 `SlidingWindowMetricsStore` 构建
- **策略自动注册抑制**：新增的 `TimeoutDegradationStrategy` 和 `CircuitBreakerDegradationStrategy` 作为 `@Component` 不会自动注入 `FallbackAiService` 的策略列表（降级判定已移至 `CapabilityExecutor` 管线内部），而是由 `AiPlatformConfig` 按能力配置的策略白名单驱动，通过注入 `Map<String, List<DegradationStrategy>>` 到各 `CapabilityExecutor` 构造器实现。各能力的策略列表在 `AiPlatformConfig` 中按能力标识配置：

```yaml
ai:
  degradation:
    strategies:
      TRIAGE: [timeout, circuit-breaker]
      RX_AUDIT: [timeout, noop]
      # 未配置的能力默认使用 [noop]
```

#### `DegradationReason` — 降级原因/错误码枚举（enum，归属 `ai-api/degradation/`）

**职责**：集中管理所有降级原因和错误码常量，消除伪代码和设计文档中散落的字符串字面量。所有降级路径伪代码及 §5.1 错误分类表中涉及的降级原因均引用此枚举的常量而非字面量。

**枚举常量定义**：
| 常量名 | code 值 | 使用场景 |
|--------|---------|---------|
| `NO_AVAILABLE_ROUTE` | `"NoAvailableRoute"` | 模型路由无可用端点 |
| `ENDPOINT_UNAVAILABLE` | `"EndpointUnavailable"` | 模型端点健康状态为 UNAVAILABLE |
| `CIRCUIT_BREAKER_OPEN` | `"CircuitBreakerOpen"` | 熔断器处于 OPEN 状态 |
| `PARSE_FAILURE` | `"ParseFailure"` | LLM 输出解析失败 |
| `TIMEOUT` | `"Timeout"` | 超时场景：超时降级策略命中 / 整体管线 orTimeout 触发 / 薄适配器委托超时。伪代码中以 `DegradationReason.TIMEOUT + ":subType"` 形式拼接细分标识，细分标识包括：<br>`:structuredChatTimeout` — structuredChat 内部超时（剩余超时窗口的 60% 内超时，直接降级而不回退 chat()）；<br>`:chatFallbackTimeout` — structuredChat 回退到 chat() 后超时（剩余窗口的 40% 内超时）；<br>`:ThinAdapterTimeout` — 薄适配器委托调用超时；<br>`:transcriptSummaryCrowding` — 讨论结论前置压缩调用挤占主流程超时窗口（由 `refineTimeoutReason()` 判定）；<br>`:primaryLlmTimeout` — 讨论结论主 LLM 调用超时（非前置压缩挤占） |
| `STRATEGY_TRIGGERED` | `"StrategyTriggered"` | 降级预检循环中非特定策略触发降级（如自定义 DegradationStrategy），降级原因拼接为 `DegradationReason.STRATEGY_TRIGGERED + ":" + strategy.getClass().getSimpleName()`；特定策略（CircuitBreakerDegradationStrategy、TimeoutDegradationStrategy）仍使用各自对应的枚举值 |
| `INTERNAL_ERROR` | `"InternalError"` | 不可预知异常（AiOrchestrator.handle 层面） |
| `INFRASTRUCTURE_ERROR` | `"InfrastructureError"` | 基础设施异常（网络超时、服务不可用等） |

**伪代码引用约定**：降级路径中不再使用字符串字面量（如 `"NoAvailableRoute"`、`"ParseFailure"`），改为引用枚举常量 `DegradationReason.NO_AVAILABLE_ROUTE` 等。在伪代码中以 `DegradationReason.NO_AVAILABLE_ROUTE` 表示。策略类名称（如 `CircuitBreakerDegradationStrategy.getSimpleName()`）不属于降级原因枚举，仍使用字符串传递。此约定适用于 §4.1 所有降级调用点和 §5.1 错误分类表。

### 3.9 `AiPlatformConfig` — 底座配置装配类（class，归属 `ai-impl/config/`）

**职责**：作为 Phase 5 底座的统一配置入口，集中管理以下装配职责：
1. 通过 `@EnableConfigurationProperties` 引入多个独立的配置属性类（非单一 `@ConfigurationProperties(prefix = "ai")` 大配置类），各配置组职责分离
2. 通过 `@Bean` 方法暴露公共基础设施 Bean（线程池、策略 Map、超时配置等）
3. 实现 `ApplicationContextAware` 在 `@PostConstruct` 阶段构建降级策略映射表
4. `ai.platform.enabled` 到 `ai.mock.enabled` 的反向转发职责由独立类 `AiPlatformEnvironmentPostProcessor` 承担，与 `AiPlatformConfig` 分离

**运行时配置热加载机制**：以下 4 项关键运行时配置当前仅通过 `@ConfigurationProperties` 在启动期一次性绑定，不支持运行态热加载：
- `execution.timeout.per-capability`（绑定至 `AiExecutionProperties.perCapability`）
- `execution.timeout.thin-adapter.per-capability`（绑定至 `AiExecutionProperties.thinAdapter.perCapability`）
- `degradation.strategies`（绑定至 `AiDegradationProperties.strategies`）
- `sliding-window.window-seconds`（绑定至 `AiSlidingWindowProperties.windowSeconds`）

**热加载局限性分析说明**：以上配置项因绑定到 `Map<String, ...>` 类型字段，`@RefreshScope` 无法直接生效（Spring Cloud 对 `@ConfigurationProperties` 的 Map 字段刷新存在已知限制——Map Entry 的增删改在 `@RefreshScope` 重建 Bean 时重置为启动期初始值而非合并增量，且 Map 类型的泛型擦除导致刷新事件的类型安全检测失效）。因此底座采用以下复合策略：

| 配置组 | 绑定配置属性类 | 热加载机制 | 实现方式 |
|-------|-------------|-----------|---------|
| `execution.timeout.*` | `AiExecutionProperties` | 自定义定时刷新（基于 `@Scheduled` 轮询配置源）+ `@RefreshScope` 兼容性兜底 | 在 `AiPlatformConfig` 中新增 `scheduledConfigRefresh()` 定时任务，每 60 秒重新读取 `@Value` 注入的配置属性（通过 `Environment` 直接查询），若检测到版本变更则重新构建 `capabilityTimeoutConfig`、`thinAdapterPerCapabilityConfig` 和 `parseTimeoutConfig` 的 `@Bean` 缓存 |
| `degradation.strategies` | `AiDegradationProperties` | 自定义定时刷新 + `AtomicReference` 全量替换 | `AiPlatformConfig` 中定时重新构建不可变 `Map` 后通过 `AtomicReference.compareAndSet()` 原子替换引用；CapabilityExecutor 在每轮 `execute()` 中通过构造器注入的 `AtomicReference<Map<String, List<DegradationStrategy>>>` 调用 `.get()` 获取最新快照，消除原构造器注入静态 Map 下热替换静默失效的矛盾 |
| `sliding-window.window-seconds` | `AiSlidingWindowProperties` | `AtomicLong` + 自定义定时刷新（与 `refreshCapabilityTimeoutConfig()` 风格一致） | 移除 `@RefreshScope`，改为 `AtomicLong windowSecondsRef` 字段 + 自定义定时刷新 `refreshWindowSeconds()`（`@Scheduled(fixedDelay=60000)`），每次从 `Environment` 重新读取配置值并通过 `AtomicLong` 发布；避免 `@RefreshScope` 销毁重建 Bean 实例导致滑动窗口数据丢失（13 项能力在 60 秒窗口内最多 13 万条事件） |
| `router.routes` | `AiRouterProperties` | 事件驱动 + 定时轮询双机制 | 见 §3.2 ModelRouter 运行时刷新策略（`@Scheduled` + `RouteConfigChangedEvent` 双触发） |

**回退方案**：若自定义定时刷新在生产环境出现行为异常（如配置源连接超时导致构建中断），回退到 Spring Cloud 原生 `@RefreshScope` 方案——通过 Actuator 端点 `/actuator/refresh` 手动触发刷新（需引入 `spring-boot-starter-actuator` + `spring-cloud-starter`），各 `@ConfigurationProperties` 标注 `@RefreshScope`。此回退方案需要重启的调优场景只需 `/actuator/refresh` 调用（零重启），比原设计（仅启动期绑定，调优需重启实例）已经有了质的改善。

**热加载双机制协同规则**（定时刷新与事件驱动）：

`router.routes` 配置同时支持定时轮询（`@Scheduled(fixedDelay=60000)`）和事件驱动（`RouteConfigChangedEvent`）两种刷新触发方式。两条触发路径的协同规则如下：

| 规则维度 | 说明 |
|---------|------|
| **优先级** | 事件驱动刷新**优先于**定时轮询刷新。事件驱动立即触发缓存重建；定时轮询作为兜底，在事件丢失（消费者异常、网络闪断等）时保证配置在 60 秒内同步。两路径共享同一刷新方法（`synchronized` 互斥防止并发刷新） |
| **重复触发副作用** | 重复触发无副作用。刷新方法内部通过 CAS（`AtomicReference` 全量替换）保证幂等：相同配置值下两次刷新的最终状态一致。缓存重建期间持有读锁的线程继续使用旧快照，重建完成后的 `compareAndSet` 新引用一次性生效，不会出现"两个版本之间读取不一致"的窗口 |
| **事件丢失后兜底** | 事件驱动刷新路径内包裹 try-catch，消费异常时日志 ERROR 但不阻止发布方事务提交。丢失后，定时轮询在下一次 @Scheduled 周期（最晚 60 秒）内感知变更并触发刷新。兜底窗口长度由 `@Scheduled(fixedDelay)` 参数控制（默认 60s），若业务要求更短的不一致窗口，可将 fixedDelay 调整为 30s（代价为轮询频率翻倍，增加配置源读压力） |
| **两路径数据不一致保护** | 事件驱动和定时轮询各自独立从配置源（YAML / 配置中心）读取当前全量数据构造不可变 Map，然后通过 AtomicReference CAS 一次性替换。两者读取到的数据始终是同一配置源的最新快照，路径之间不存在"事件读到 version A，轮询读到 version B"的版本不一致问题——两路径不会同时持有不同的"增量视图" |

`TemplateChangedEvent` 和 `ExperimentChangedEvent`（§3.3 / §3.4）同理适用以上协同规则，区别在于模板/实验配置的事件驱动为缓存失效而非直接重建（事件触发后清除对应 Caffeine 缓存条目，下次请求惰性加载），因此不存在"事件与定时轮询竞争写入缓存"的问题。

**配置项热加载状态标记**：在 §9.5 YAML 配置节中，上述 4 项配置的注释行以 `# [动态]` 或 `# [静态·重启生效]` 标记标识热加载能力状态，使运维人员无需查阅设计文档即可判断某项配置修改后是否需要重启实例。

**配置属性类拆分**：将原本单一 `@ConfigurationProperties(prefix = "ai")` 拆分为以下独立配置属性类，`AiPlatformConfig` 通过 `@EnableConfigurationProperties` 逐个引入：

| 配置属性类 | 前缀 | 绑定配置组 | 说明 |
|-----------|------|-----------|------|
| `AiRouterProperties` | `ai.router` | `routes` 路由映射表 | 模型路由配置，各能力的 endpointId/model/clientType/parameters |
| `AiExecutionProperties` | `ai.execution` | `timeout.per-capability`、`timeout.thin-adapter-default`、`timeout.parse.default`、`timeout.parse.per-capability` | 执行超时配置，各能力独立超时阈值 |
| `AiDegradationProperties` | `ai.degradation` | `strategies`、`circuit-breaker.*`、`timeout.*`、`context-ttl-seconds` | 降级策略配置，策略白名单与参数 |
| `AiRateLimitingProperties` | `ai.rate-limiting` | `enabled`、`endpoints.*` | 端点限流配置，各 endpointId 独立限流参数 |
| `AiMetricsAsyncProperties` | `ai.metrics.async` | `core-pool-size`、`max-pool-size`、`queue-capacity`、`rejection-policy` | 指标采集异步线程池配置 |
| `AiPlatformProperties` | `ai.platform` | `enabled` | 底座启用开关 |
| `AiSlidingWindowProperties` | `ai.sliding-window` | `window-seconds`、`max-events-per-capability` | 滑动窗口配置 |
| `AiTemplateProperties` | `ai.template.fallback` | `{capabilityId}` 各能力兜底 Prompt | 兜底 Prompt 模板配置 |

拆分理由：单一 `@ConfigurationProperties(prefix = "ai")` 需绑定全部 8 个配置组的所有嵌套字段，`AiPlatformConfig` 将退化为数百行的巨型配置持有者，违反单一职责原则。拆分为独立属性类后，每个类只负责一个功能域（路由/超时/降级/限流/指标/滑动窗口/模板）的配置绑定，字段数量可控、职责清晰、易于单元测试。

**核心 @Bean 方法签名**：

```
@Bean("llmCallExecutor")
public Executor llmCallExecutor() {
    // 核心线程 = 可用模型端点数，最大线程 = 2x 核心线程
    // 队列容量 100，拒绝策略 CallerRunsPolicy（自然背压）
    return new ThreadPoolExecutor(coreSize, 2 * coreSize, 60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(100),
        new ThreadPoolExecutor.CallerRunsPolicy());
}

@Bean("metricsAsyncExecutor")
public Executor metricsAsyncExecutor(MetricsAsyncProperties metricsAsyncProperties) {
    // 从 @ConfigurationProperties(prefix = "ai.metrics.async") 绑定的 MetricsAsyncProperties 注入动态值
    // 而非硬编码。MetricsAsyncProperties 归属 ai-impl/config/，与 AiPlatformConfig 解耦。
    // 绑定属性对应 §9.5 YAML 配置 ai.metrics.async 块：
    //   core-pool-size: 1, max-pool-size: 2, queue-capacity: 1000, rejection-policy: Discard
    // 若绑定值不可用（配置缺失），使用 MetricsAsyncProperties 字段默认值（core=1, max=2, queue=1000）
    // 拒绝策略：DiscardPolicy + WARN 日志（指标丢失可接受，不阻塞主路径）
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(metricsAsyncProperties.getCorePoolSize());
    executor.setMaxPoolSize(metricsAsyncProperties.getMaxPoolSize());
    executor.setQueueCapacity(metricsAsyncProperties.getQueueCapacity());
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy() {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            log.warn("指标采集任务被丢弃: 队列已满(queueSize={}), 活跃线程={}", e.getQueue().size(), e.getActiveCount());
            super.rejectedExecution(r, e);
        }
    });
    executor.setThreadNamePrefix("metrics-async-");
    executor.initialize();
    return executor;
}

@Bean("degradationStrategyMapRef")
public AtomicReference<Map<String, List<DegradationStrategy>>> degradationStrategyMapRef() {
    // 返回 AtomicReference 包裹的策略 Map，使 CapabilityExecutor 在每轮 execute() 中
    // 通过 .get() 获取最新快照，支持 degradation.strategies 的运行时热替换
    // refreshDegradationStrategies() 定时刷新时构造新的不可变 Map 并通过
    // AtomicReference.compareAndSet() 原子替换引用，确保读取端无锁安全
    return this.strategyMapRef; // 类型: AtomicReference<Map<String, List<DegradationStrategy>>>, 由 @PostConstruct 初始化
}

@Bean("capabilityTimeoutConfig")
public Map<String, Duration> capabilityTimeoutConfig(AiExecutionProperties executionProperties) {
    // 从 AiExecutionProperties.perCapability 绑定
    return executionProperties.getPerCapability();
}

@Bean("thinAdapterPerCapabilityConfig")
public Map<String, Duration> thinAdapterPerCapabilityConfig(AiExecutionProperties executionProperties) {
    // 从 AiExecutionProperties 中提取 thin-adapter.per-capability 配置
    // 对应 YAML: ai.execution.timeout.thin-adapter.per-capability
    return executionProperties.getThinAdapter().getPerCapability();
}

@Bean("parseTimeoutConfig")
public Map<String, Duration> parseTimeoutConfig(AiExecutionProperties executionProperties) {
    // 从 AiExecutionProperties 中提取 timeout.parse.per-capability 配置
    // 对应 YAML: ai.execution.timeout.parse.per-capability
    return executionProperties.getParse().getPerCapability();
}

@Bean("transcriptSummaryExecutor")
public Executor transcriptSummaryExecutor() {
    // DiscussionConclusionCapabilityExecutor 前置 LLM 压缩调用的独立线程池
    // 避免压缩调用阻塞 llmCallExecutor 线程池导致其他能力无法执行 LLM 调用
    // 核心线程数 = min(availableProcessors, 4)，最大线程数 = 核心线程数 × 2
    // 队列容量 20，拒绝策略 DiscardPolicy + WARN 日志
    // 理由：CallerRunsPolicy 将压缩任务回退到提交线程（即 llmCallExecutor 的 Worker 线程），
    // 队列满时隔离设计完全失效，其他能力的 LLM 调用将被压缩任务阻塞。
    // DiscardPolicy + WARN 确保压缩任务被丢弃时通过 catch 块的回退逻辑（截断文本）保障主流程
    int coreSize = Math.min(Runtime.getRuntime().availableProcessors(), 4)
    return new ThreadPoolExecutor(coreSize, 2 * coreSize, 60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(20),
        new ThreadPoolExecutor.DiscardPolicy() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                log.warn("转录摘要压缩任务被丢弃: 队列已满(queueSize={}), 活跃线程={}, 压缩任务回退到截断文本",
                    e.getQueue().size(), e.getActiveCount());
                super.rejectedExecution(r, e);
            }
        })
}

@Bean("scheduledTaskExecutor")
public ThreadPoolTaskScheduler scheduledTaskExecutor() {
    // 底座内多个 @Scheduled 定时任务（指标清理@Scheduled(cron)、CredentialProvider退避定时器@Scheduled、
    // ModelRouter轮询@Scheduled(fixedDelay)等）共享此线程池，避免 Spring 默认单线程调度器阻塞
    // Pool size 设为 3，确保：1) 指标清理的 DDL 操作（DROP PARTITION）不阻塞 CredentialProvider 的 Vault 探测；
    // 2) ModelRouter 轮询刷新不因清理阻塞而延迟；
    // 3) poolSize ≥ 3 满足三个 @Scheduled 任务同时运行的需求
    // 命名前缀 "scheduled-task-" 便于日志和监控线程池排查
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(3);
    scheduler.setThreadNamePrefix("scheduled-task-");
    scheduler.setWaitForTasksToCompleteOnShutdown(true);
    scheduler.setAwaitTerminationSeconds(10);
    scheduler.initialize();
    return scheduler;
}
```

**`AiPlatformEnvironmentPostProcessor` — 配置转发前置处理器**（独立 class，归属 `ai-impl/config/`）

**职责**：在 Spring 启动早期（`@ConditionalOnProperty` 评估之前）完成 `ai.platform.enabled` 到 `ai.mock.enabled` 的反向转发，使两个开关互斥生效。与 `AiPlatformConfig` 分离为独立类的理由如下：
- `EnvironmentPostProcessor` 实例由 `SpringFactoriesLoader` 在 Spring 容器初始化之前创建，**非 Spring 管理**（无依赖注入、AOP 等容器能力）；`ApplicationContextAware` 实例由 Spring 容器管理，受容器生命周期约束。将两者合并到同一类意味着该类被两个生命周期不同的实例同时使用——EPP 实例在容器外执行转发后即废弃，bean 实例在容器内执行装配，虽然不产生运行时冲突，但单一对象表象与"两个不同实例"的本质不一致，增加代码阅读的认知负担
- 分离后职责清晰：`AiPlatformEnvironmentPostProcessor` 仅负责"早期配置转发"，生命周期终结于 Spring 环境准备阶段；`AiPlatformConfig` 专注"容器内 Bean 装配"，职责内聚
- 注册方式：`AiPlatformEnvironmentPostProcessor` 通过 `META-INF/spring.factories` 中的 `org.springframework.boot.env.EnvironmentPostProcessor` 条目注册，不标注任何 Spring 注解

```
AiPlatformEnvironmentPostProcessor.postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app):
    // 反向转发：ai.platform.enabled=true → ai.mock.enabled=false
    // ai.platform.enabled=false → ai.mock.enabled=true（回退 Mock）
    // 仅在 ai.mock.enabled 未在 PropertySource 中显式设置时生效
    platformEnabled = env.getProperty("ai.platform.enabled")
    if platformEnabled != null && env.getProperty("ai.mock.enabled") == null:
        mockEnabled = "true".equals(platformEnabled) ? "false" : "true"
        env.getPropertySources().addLast(
            new MapPropertySource("aiPlatformConfig", Map.of("ai.mock.enabled", mockEnabled)))
```

**JPA 配置与 Repository 扫描策略**：底座模块的 JPA Repository 分布在三个子包中（`template/`、`experiment/`、`metrics/`），需在 `AiPlatformConfig` 或其他顶层配置类上声明 `@EnableJpaRepositories` 和 `@EntityScan` 确保 Spring Data JPA 正确扫描到所有 Repository 和 Entity。配置方式如下：

```java
@Configuration
@EnableJpaRepositories(basePackages = "com.aimedical.modules.ai.impl")
@EntityScan(basePackages = "com.aimedical.modules.ai.impl")
// 或分别在 AiPlatformConfig 类上组合标注：
// @EnableJpaRepositories(basePackageClasses = {
//     PromptTemplateRepository.class,
//     ExperimentRepository.class,
//     AiCallLogRepository.class
// })
// @EntityScan(basePackageClasses = {
//     PromptTemplate.class,
//     Experiment.class,
//     AiCallLogEntity.class
// })
class AiPlatformConfig { ... }
```

**设计理由**：`basePackages = "com.aimedical.modules.ai.impl"` 覆盖 `template/`、`experiment/`、`metrics/` 三个子包下的所有 Repository 和 Entity，避免每新增一个 JPA 子包时都需要修改配置。使用包级扫描而非逐个枚举 `basePackageClasses`，使新增 JPA 子包时无需修改配置类。若希望更精确控制扫描范围，可选用 `basePackageClasses` 变体。

**`AiPlatformConfig` 类型定位**：使用 class 而非 interface，因配置装配为单一职责的具体实现，无多态替换需求。标注 `@Configuration` + `@EnableConfigurationProperties({AiRouterProperties.class, AiExecutionProperties.class, AiDegradationProperties.class, AiRateLimitingProperties.class, AiMetricsAsyncProperties.class})` + `implements ApplicationContextAware`。不再实现 `EnvironmentPostProcessor`。

**协作对象**：
- 被 Spring 容器在启动期自动调用
- 生成的线程池被 `AbstractCapabilityExecutor` 通过 `@Qualifier("llmCallExecutor")` 注入
- 生成的指标线程池被 `AiMetricsCollector` 标注 `@Async("metricsAsyncExecutor")` 引用
- 生成的策略 Map 被各 `CapabilityExecutor` 构造器注入

#### 3.9.1 启动期 Bean 初始化顺序依赖图

底座启动过程中涉及多条 Bean 初始化依赖链，以下按依赖顺序列出各约束关系，实施者需在 `AiPlatformConfig` 的 `@PostConstruct` 及参与组件各自的 `@PostConstruct` 中确保执行顺序正确：

```mermaid
graph TD
    A[AiPlatformEnvironmentPostProcessor] -->|EnvironmentPostProcessor 机制, Spring 环境就绪前执行| B[ai.mock.enabled 属性就绪]
    B -->|@ConditionalOnProperty 评估| C[AiOrchestrator @Bean 条件匹配]
    B -->|@ConditionalOnProperty 评估| D[MockAiService @Bean 条件匹配]
    
    subgraph 批次 1 核心骨架
        E[SlidingWindowMetricsStore @Component] -->|无前置依赖, 最早初始化| F[Deque<WindowedEvent> 就绪]
        F --> G[DegradationStrategy 各实现 @Component]
        G -->|按 YAML 简名注册| G
        F --> H[AbstractCapabilityExecutor 构造器注入 metricsStore]
        H -->|构造器注入| I[CapabilityExecutor 各子类注册为 @Component]
    end
    
    subgraph 配置装配
        J[AiPlatformConfig @Configuration] -->|ApplicationContextAware| K[ApplicationContext 注入]
        K -->|@PostConstruct| L[降级策略 Map 构建: 按 YAML 策略白名单从 ApplicationContext 获取策略 Bean]
        K -->|启动期配置校验| M[校验 per-capability >= thin-adapter.per-capability + 5s]
        K -->|启动期配置校验| N[校验 parseTimeout <= chatFallbackTimeout]
        L --> O[degradationStrategyMapRef @Bean 就绪]
        M --> P[CapabilityExecutor 构造器可用]
        N --> P
    end
    
    subgraph 预热阶段
        Q[DatabasePromptTemplateManager @PostConstruct] -->|JPA 查询全部 ACTIVE 模板| R[PromptTemplate Caffeine 缓存预填充]
        S[HashBucketExperimentManager @PostConstruct] -->|JPA 查询全部 ACTIVE 实验配置| T[Experiment Caffeine 缓存预填充]
        U[DefaultModelRouter @PostConstruct] -->|从 YAML 配置源加载路由表| V[路由表不可变 Map 就绪]
    end
    
    subgraph 启动期就绪验证
        W[AiOrchestrator @PostConstruct] -->|扫描 List<CapabilityExecutor>| X[executorMap 构建完成]
        X --> Y[AiOrchestrator 可接受请求]
        Z[DelegatingLlmChatService @PostConstruct] -->|检查所有 ClientType 枚举值均有对应实现| AA[分发层就绪]
    end

    P --> Q
    P --> S
    P --> U
    P --> W
    P --> Z
```

**四条关键约束链**：

| 约束链 | 依赖方向 | 违反后果 | 说明 |
|--------|---------|---------|------|
| **AiPlatformEnvironmentPostProcessor → @ConditionalOnProperty 评估** | AiPlatformEnvironmentPostProcessor.postProcessEnvironment() 在 Environment 就绪后立即执行 | `ai.platform.enabled=true` 时 `MockAiService` 仍可能被注册 → Bean 二义性 | EnvironmentPostProcessor 早于任何 `@Bean`/`@Component` 初始化，`postProcessEnvironment()` 中写入的属性在后续所有 `@ConditionalOnProperty` 评估时可见。此链无运行时风险，仅需确认 `AiPlatformEnvironmentPostProcessor` 通过 `META-INF/spring.factories` 正确注册 |
| **DegradationStrategy 组件初始化 → AiPlatformConfig.@PostConstruct** | `@Component("timeout")` 等策略 Bean 必须在 `AiPlatformConfig.@PostConstruct` 执行前完成注册 | 策略名称查找 `getBean("timeout")` 抛出 `NoSuchBeanDefinitionException` → 底座启动失败 | Spring 容器保证 `@Component` 在 `@Configuration` 的 `@PostConstruct` 之前完成注册（`@Configuration` 的 `@PostConstruct` 在依赖注入阶段之后触发，此时所有 `@Component` 已就绪）。需确保 YAML 策略简名与 `@Component` Bean name 一致（如 `"timeout"`、`"circuit-breaker"`、`"noop"`） |
| **CapabilityExecutor 初始化 → AiOrchestrator.@PostConstruct** | 13 个 `CapabilityExecutor @Component` 必须在 `AiOrchestrator` 自动注入 `List<CapabilityExecutor>` 之前完成注册 | `AiOrchestrator.executorMap` 遗漏部分能力 → 运行时 `executorMap.get(capabilityId)` 返回 null → 降级 | Spring `@Autowired List<CapabilityExecutor>` 收集所有实现，顺序由容器管理。各能力之间无依赖顺序（均为独立 Bean），批次内可并行初始化 |
| **HikariCP 连接池就绪 → @PostConstruct 预热查询** | DatabasePromptTemplateManager/HashBucketExperimentManager 的 JPA 预热查询需在 HikariCP 初始化完成后执行 | HikariCP `initialization-fail-timeout=-1` 时预热查询自旋等待连接池 → 底座启动阻塞 | 预热查询包裹 try-catch，连接未就绪时捕获 `SQLException`，WARN 日志 + 跳过预热。HikariCP 建议配置 `initialization-fail-timeout=1000` |

### 3.10 `RequestContextUtils` — HTTP Header 提取工具类（public static utility class，归属 `ai-impl/orchestrator/`）

**职责**：从当前 HTTP 请求上下文中提取指定 Header 值，供薄适配器型 CapabilityExecutor（`doExtractDepartmentId()`/`doExtractVisitId()`/`doExtractPatientId()`/`doExtractSessionId()`）和 `AiOrchestrator.handle()` catch 块共享使用。

**核心方法**：
```
public static String extractFromRequestContext(String headerName):
    RequestAttributes attrs = RequestContextHolder.getRequestAttributes()
    // ⚠️ 使用 instanceof 而非直接强转：当底座运行在非 Spring MVC 环境（如 WebFlux）下时，
    // RequestContextHolder.getRequestAttributes() 可能返回 ReactiveRequestContextHolder 或其他
    // 非 ServletRequestAttributes 实现，直接强转将抛出 ClassCastException
    if attrs instanceof ServletRequestAttributes:
        return ((ServletRequestAttributes) attrs).getRequest().getHeader(headerName)
    // 非 Servlet 环境下 RequestAttributes 无 getRequest() 方法，返回 null
    // 调用方（薄适配器 doExtractXxx()）在收到 null 后回退到 DTO 字段或 HTTP Header 兜底路径
    return null
```

**为何从 `AbstractCapabilityExecutor.protected` 提升为 `public static` 工具类**：
- `AiOrchestrator.handle()` catch 块同样需要提取 HTTP Header（Phase 4 DTO 兼容路径及 CGLIB 代理兜底），但 `AiOrchestrator` 不继承 `AbstractCapabilityExecutor`，无法访问 protected 方法，导致编译错误。将其抽取为独立的 `public static` 工具类后，`AbstractCapabilityExecutor` 子类（`doExtractDepartmentId()` 等）和 `AiOrchestrator` 均可统一调用 `RequestContextUtils.extractFromRequestContext(headerName)`
- 静态方法形态便于单元测试——在测试中通过 `MockHttpServletRequest` 注入 `RequestContextHolder` 后直接调用，无需创建继承 `AbstractCapabilityExecutor` 的测试桩
- 不定义为一个独立的 Spring Bean（无 `@Component`），避免不必要的容器管理开销；静态工具类形态适合纯请求上下文访问场景

**新增公共方法**（作为 `extractCallerRole()` / `extractCallerId()` 的唯一规范实现）：
```
public static String extractCallerRole():
    // 提取策略：从 SecurityContextHolder.getContext().getAuthentication() 获取认证对象，
    // 优先遍历 authority 列表查找第一个以 "ROLE_" 开头的 authority，去除 "ROLE_" 前缀后返回。
    // 若不存在 ROLE_ 前缀的 authority，则取第一个 authority 的原始值。
    // 与 AbstractCapabilityExecutor.extractCallerRole() 原实现一致，作为该方法的唯一规范实现。
    // 替换原 AbstractCapabilityExecutor.extractCallerRole() 和 AiOrchestrator.handle() catch 块中的
    // 直接 authority 提取逻辑，消除两处提取不一致问题。
    auth = SecurityContextHolder.getContext().getAuthentication()
    if auth != null && auth.getAuthorities() != null && !auth.getAuthorities().isEmpty():
        for authority in auth.getAuthorities():
            role = authority.getAuthority()
            if role != null && role.startsWith("ROLE_"):
                return role.substring(5)
        return auth.getAuthorities().iterator().next().getAuthority()
    return "SYSTEM"

public static String extractCallerId():
    // 提取策略：取 Authentication.getName() 返回值，null 时回退 "SYSTEM"
    auth = SecurityContextHolder.getContext().getAuthentication()
    return auth != null ? auth.getName() : "SYSTEM"
```

`AbstractCapabilityExecutor.extractCallerRole()` 和 `extractCallerId()` 的实现委托至 `RequestContextUtils`：
```
// AbstractCapabilityExecutor 中的 extractCallerRole() 和 extractCallerId() 方法
// 现委托至 RequestContextUtils 的唯一实现：
extractCallerRole():
    return RequestContextUtils.extractCallerRole()
extractCallerId():
    return RequestContextUtils.extractCallerId()
```

`AiOrchestrator.handle()` catch 块中的 callerRole/callerId 提取同样委托至 `RequestContextUtils`。

**调用方 Header 契约**：

| Header 名称 | 对应字段 | 必填性 | 说明 |
|------------|---------|-------|------|
| `X-Department-ID` | departmentId (String) | 可选 | 科室标识，Prompt 模板按科室维度检索时使用；缺失时回退到通用模板 |
| `X-Visit-ID` | visitId (String) | 可选 | 就诊标识，用于 `AiCallRecord` 关联分析和就诊上下文获取 |
| `X-Patient-ID` | patientId (String) | 可选 | 患者标识，用于 `AiCallRecord` 关联分析 |
| `X-Session-ID` | sessionId (String) | 可选 | 会话标识，用于 `ExperimentManager.assign()` 确定性分桶和 `AiCallRecord` 分析 |

**非 HTTP 场景的上下文提取路径**：
在非 HTTP 调用场景（MQ 消息消费、`@Scheduled` 定时任务等）下，`RequestContextHolder.getRequestAttributes()` 返回 null，`extractFromRequestContext()` 无法提取 Header 值。此时就诊上下文的提取由以下路径保障：

1. **DTO 字段继承**：Phase 5 底座能力的 DTO 已继承 `AiRequestBase`（含 `departmentId`/`visitId`/`patientId`/`sessionId` 字段），`AbstractCapabilityExecutor` 的 `doExtractXxx()` 默认实现直接从 DTO getter 返回，不依赖 `RequestContextHolder`
2. **MQ/定时任务的调用方责任**：MQ 消费者或定时任务在构造能力 DTO 时，必须从消息体或任务上下文中提取就诊上下文并填充 `AiRequestBase` 继承字段
3. **薄适配器 DTO 过渡策略**：Phase 4 薄适配器型 CapabilityExecutor 的 DTO 暂未继承 `AiRequestBase`（见 §3.5 过渡策略），在非 HTTP 场景下 `doExtractDepartmentId()` 等重写方法中回退到 `request` 对象中由调用方显式传递的对应字段（若 DTO 中无对应字段则返回 null，对应诊断字段在 `AiCallRecord` 中可空）
4. **`AiOrchestrator.handle()` 兜底保护**：catch 块中 `RequestContextUtils.extractFromRequestContext()` 返回 null 时，就诊上下文保持 null（`AiCallRecord` 对应字段可空，不影响主流程）

**调用方责任概要**：
- HTTP 调用方：在请求中携带 `X-Department-ID`、`X-Visit-ID`、`X-Patient-ID`、`X-Session-ID` Header（网关层或业务模块透传），使 `RequestContextUtils.extractFromRequestContext()` 可正常提取
- MQ 消费者/定时任务：在构造业务 DTO 时填充 `AiRequestBase` 继承字段（或 Phase 4 DTO 中对应的科室/就诊字段）

---

### 3.11 Phase 5 底座能力特化设计

以下 7 项 AI 能力在 Phase 5 底座中以完整管线型 CapabilityExecutor 实现，按来源分为三类：**迁移能力**（3 项：智能分诊、处方审核、病历生成）从 Phase 2~4 独立接入迁移至底座；**继承能力**（1 项：辅助开方）从 Phase 2~4 的已有实现迁入底座并升级为完整管线；**首次落地能力**（3 项：知识库问答、医生排班、讨论结论）在 Phase 5 首次真实实现于底座。各能力特化时需关注的差异点如下。

#### 3.11.1 `TriageCapabilityExecutor` — AI 智能分诊（迁移能力）

| 设计维度 | 说明 |
|---------|------|
| 背景 | 源自 Phase 2~4 独立接入，迁移至底座完整管线。迁移前后行为差异：原有独立实现中的降级/超时/重试策略替换为底座统一策略（`SlidingWindowMetricsStore` + `DegradationStrategy` 链）；模型调用改为通过 `ModelRouter` + `LlmChatService` 标准化路径 |
| 职责 | 根据患者主诉、症状描述、既往病史等信息，推荐最合适的就诊科室 |
| 输入 DTO | `TriageRequest`（继承 `AiRequestBase`，⊕ 新增），扩展字段：`complaint: String`（主诉，⊕ 需新增）、`symptomDescription: String`（症状描述，⊕ 需新增）、`medicalHistory: String`（既往史，可选，⊕ 需新增）、`vitalSigns: String`（生命体征，可选，⊕ 需新增） |
| 输出 DTO | `TriageResponse`（⊕ 新增），字段：`recommendedDepartments: List<RecommendedDepartment>`（推荐科室列表，△ 需改造——现有代码中此字段已存在但结构可能需对齐）、`reason: String`（推荐理由，⊕ 需新增）、`urgencyLevel: UrgencyLevel`（紧急程度枚举：EMERGENCY/URGENT/ROUTINE，⊕ 需新增）。`RecommendedDepartment` 内嵌字段：`departmentId: String`、`departmentName: String`、`recommendationWeight: Integer`（推荐权重，值越高优先级越大）。**⚠️ 现有代码兼容性说明**：当前代码库中 `TriageResponse` 已有 `recommendedDepartments: List<RecommendedDepartment>` 字段且无 `urgencyLevel` 字段，标注"△ 需改造"的为需对齐字段，标注"⊕ 需新增"的为 Phase 5 底座新设计字段。下游消费者在底座切流前需评估新增字段对序列化的兼容性影响——旧消费者通过 `@JsonIgnoreProperties(ignoreUnknown = true)` 安全忽略新增字段 |
| Prompt 模板结构 | SYSTEM 角色：分诊助手系统提示，要求输出推荐科室和紧急程度。模板变量：`{{complaint}}`、`{{symptomDescription}}`、`{{medicalHistory}}`、`{{vitalSigns}}`。USER 角色：包含患者主诉和症状描述 |
| 结构化解析目标类型 | `TriageResponse`（JSON 格式），`urgencyLevel` 为枚举映射 |
| 模板变量提取策略 | 方式 A（默认 `ObjectMapper.convertValue`）：从 `TriageRequest` DTO 直接映射为扁平键值对 |
| YAML 路由配置 | `TRIAGE: { model: "qwen-plus", endpoint: "...", client: "HTTP_API", parameters: {temperature: 0.3, maxTokens: 2048, topP: 0.9} }` |
| 降级策略 | `[timeout, circuit-breaker]` |

#### 3.11.2 `PrescriptionCheckCapabilityExecutor` — AI 处方审核（迁移能力）

| 设计维度 | 说明 |
|---------|------|
| 背景 | 源自 Phase 2~4 独立接入（含本地规则降级），迁移至底座完整管线。迁移前后行为差异：原有独立实现中的本地规则降级替换为 `PrescriptionLocalRuleFallback`（§3.7）；结构化输出解析改为 `LlmChatService.structuredChat()` + `StructuredOutputParser` 回退路径 |
| 职责 | 审核处方的合理性和安全性，包括配伍禁忌、剂量范围、重复用药等检查，返回审核意见 |
| 输入 DTO | `PrescriptionCheckRequest`（继承 `AiRequestBase`，⊕ 新增），扩展字段：`medications: List<Medication>`（处方药品列表，⊕ 需新增）、`patientInfo: PatientInfo`（患者信息，含年龄/体重/过敏史/孕产状态，⊕ 需新增）、`diagnosisCodes: List<String>`（诊断编码列表，⊕ 需新增）。**`PatientInfo` 值对象字段定义**：<br>`age: Integer`（患者年龄，岁）<br>`weight: Double`（患者体重，kg，可选）<br>`allergyInfo: List<String>`（过敏史列表）<br>`pregnancyStatus: PregnancyStatus`（孕产状态枚举：NONE / EARLY_PREGNANCY / MID_PREGNANCY / LATE_PREGNANCY / LACTATION，可选，null 表示未知） |
| 输出 DTO | `PrescriptionCheckResponse`（⊕ 新增），字段：`overallStatus: CheckStatus`（整体状态枚举：PASS/REVIEW_REQUIRED/REJECTED，⊕ 需新增）、`checks: List<CheckResult>`（逐项检查结果列表，⊕ 需新增）、`summary: String`（审核意见摘要，⊕ 需新增） |
| Prompt 模板结构 | SYSTEM 角色：处方审核药师系统提示，要求逐项检查配伍禁忌、剂量范围、重复用药、过敏史、特殊人群用药。模板变量：`{{medications}}`（药品列表格式化文本）、`{{patientInfo}}`（患者信息格式化文本）、`{{diagnosisCodes}}`。USER 角色：包含处方详情和患者信息 |
| 结构化解析目标类型 | `PrescriptionCheckResponse`（JSON 格式），`CheckResult` 内嵌字段：`itemName: String`（检查项名称）、`status: CheckStatus`、`detail: String`（详情说明） |
| 模板变量提取策略 | 方式 B（自定义）：从 `PrescriptionCheckRequest` 提取基本字段；将 `medications` 列表格式化为结构化药品清单文本（药品编码/名称/剂量/频次/途径）；将 `patientInfo` 对象展开为患者基本信息文本 |
| YAML 路由配置 | `RX_AUDIT: { model: "qwen-turbo", endpoint: "...", client: "HTTP_API", parameters: {temperature: 0.1, maxTokens: 1024} }` |
| 降级策略 | `[timeout, noop]`（处方审核场景因有本地规则降级兜底，noop 作为主策略，熔断不适用） |

#### 3.11.3 `MedicalRecordGenCapabilityExecutor` — AI 病历生成（迁移能力）

| 设计维度 | 说明 |
|---------|------|
| 背景 | 源自 Phase 2~4 独立接入，迁移至底座完整管线。迁移前后行为差异：原有独立实现中输出的病历格式为自由文本，迁移后改为结构化输出（`MedicalRecordGenResponse`）以支持下游消费方标准化解析 |
| 职责 | 根据就诊信息（主诉、现病史、既往史、体格检查、辅助检查等）生成结构化的病历文书 |
| 输入 DTO | `MedicalRecordGenRequest`（继承 `AiRequestBase`，⊕ 新增），扩展字段：`chiefComplaint: String`（主诉，⊕ 需新增）、`presentIllness: String`（现病史，⊕ 需新增）、`pastHistory: String`（既往史，⊕ 需新增）、`physicalExamination: String`（体格检查，⊕ 需新增）、`auxiliaryExaminations: List<String>`（辅助检查列表，可选，⊕ 需新增）、`diagnosis: String`（初步诊断，可选，⊕ 需新增） |
| 输出 DTO | `MedicalRecordGenResponse`（⊕ 新增），字段：`recordContent: String`（病历正文，⊕ 需新增）、`structuredSections: List<RecordSection>`（结构化段落列表，⊕ 需新增）、`generatedAt: LocalDateTime`（生成时间，⊕ 需新增）。`RecordSection` 内嵌字段：`sectionName: String`（段落名称，⊕ 需新增）、`content: String`（段落内容，⊕ 需新增）、`sectionType: SectionType`（段落类型枚举：CHIEF_COMPLAINT/PRESENT_ILLNESS/PAST_HISTORY/PHYSICAL_EXAM/AUXILIARY_EXAM/DIAGNOSIS，⊕ 需新增） |
| Prompt 模板结构 | SYSTEM 角色：病历生成助手系统提示，要求按标准病历格式输出结构化文段。模板变量：`{{chiefComplaint}}`、`{{presentIllness}}`、`{{pastHistory}}`、`{{physicalExamination}}`、`{{auxiliaryExaminations}}`、`{{diagnosis}}`。USER 角色：包含就诊信息全文 |
| 结构化解析目标类型 | `MedicalRecordGenResponse`（JSON 格式），`structuredSections` 列表按病历结构顺序排列 |
| 模板变量提取策略 | 方式 B（自定义）：从 `MedicalRecordGenRequest` 提取各字段；`auxiliaryExaminations` 列表格式化为编号文本；若某字段为空则跳过对应模板变量，由 `PromptTemplateManager.render()` 保留原始占位符并 WARN 日志 |
| YAML 路由配置 | `MEDICAL_RECORD_GEN: { model: "qwen-plus", endpoint: "...", client: "HTTP_API", parameters: {temperature: 0.2, maxTokens: 4096} }`（病历输出较长，maxTokens 使用 4096） |
| 降级策略 | `[timeout, circuit-breaker]` |

#### 3.11.4 `PrescriptionAssistCapabilityExecutor` — AI 辅助开方（继承能力）

| 设计维度 | 说明 |
|---------|------|
| 背景 | 源自 Phase 2~4 已有实现（`PrescriptionAssistService`），迁入底座并升级为完整管线。Phase 2~4 已有独立 HTTP 模型调用，迁移后改为统一路由（`ModelRouter`）+ 标准化 LLM 调用（`LlmChatService`） |
| 职责 | 根据患者诊断信息和用药历史，推荐合理的处方方案（药品、剂量、用法） |
| 输入 DTO | `PrescriptionAssistRequest`（继承 `AiRequestBase`，⊕ 新增），扩展字段：`diagnosisCode: String`（诊断编码，如 ICD-10，⊕ 需新增）、`diagnosisName: String`（诊断名称，⊕ 需新增）、`patientInfo: PatientInfo`（患者信息，⊕ 需新增；与 `PrescriptionCheckRequest` 共用同一 `PatientInfo` 值对象定义，保持一致的患者数据建模方式）、`existingMedications: List<Medication>`（当前用药列表，可选，⊕ 需新增） |
| 输出 DTO | `PrescriptionAssistResponse`（⊕ 新增），字段：`recommendations: List<MedicationRecommendation>`（推荐药品列表，⊕ 需新增）。`MedicationRecommendation` 内嵌字段：`drugCode: String`（⊕ 需新增）、`drugName: String`（⊕ 需新增）、`dosage: String`（如 "250mg"，⊕ 需新增）、`frequency: String`（如 "tid"，⊕ 需新增）、`route: String`（如 "po"，⊕ 需新增）、`duration: String`（如 "7d"，⊕ 需新增）、`rationale: String`（推荐理由，⊕ 需新增）、`confidenceLevel: ConfidenceLevel`（枚举：HIGH/MEDIUM/LOW，⊕ 需新增） |
| Prompt 模板结构 | SYSTEM 角色：临床药学助手系统提示，强调合理用药原则（禁忌症检查、剂量范围、药物相互作用）。模板变量：`{{diagnosisName}}`、`{{patientAge}}`、`{{patientWeight}}`、`{{allergyInfo}}`、`{{existingMedications}}`。USER 角色：包含诊断信息和患者基本情况 |
| 结构化解析目标类型 | `PrescriptionAssistResponse`（JSON 格式），每条推荐需包含完整的药品信息和推荐理由 |
| 模板变量提取策略 | 方式 B（自定义）：从 `PrescriptionAssistRequest` 提取 `diagnosisCode`、`diagnosisName` 基本字段；从 `patientInfo` 值对象中手工展开提取 `patientAge`、`patientWeight`、`allergyInfo` 等扁平变量，与 `PrescriptionCheckCapabilityExecutor`（§3.11.2）使用一致的自定义展开方式 |
| YAML 路由配置 | `RX_ASSIST: { model: "qwen-plus", endpoint: "...", client: "HTTP_API", parameters: {temperature: 0.2, maxTokens: 3072} }` |
| 降级策略 | `[timeout, circuit-breaker]` |

#### 3.11.5 `KbQueryCapabilityExecutor` — AI 知识库问答（首次落地能力）

| 设计维度 | 说明 |
|---------|------|
| 职责 | 基于患者症状/诊断信息，从医疗知识库中检索匹配的知识条目并生成自然语言回答 |
| 输入 DTO | `KbQueryRequest`（继承 `AiRequestBase`，⊕ 新增），扩展字段：`queryText: String`（查询文本，⊕ 需新增）、`queryScope: KbQueryScope`（查询范围枚举：DRUG/DIAGNOSIS/TREATMENT/GUIDELINE，默认 ALL，⊕ 需新增）、`maxResults: int`（返回条目上限，默认 5，⊕ 需新增）。**设计说明**：`departmentId` 通过继承自 `AiRequestBase` 的基类字段提供，不重复声明为扩展字段；查询范围由 `queryScope` 和 `AiRequestBase.departmentId` 联合表达——`departmentId` 作为科室维度过滤，`queryScope` 作为知识类型维度过滤，两者语义不重叠，消除字段歧义 |
| 输出 DTO | `KbQueryResponse`（⊕ 新增），字段：`answer: String`（综合回答文本，⊕ 需新增）、`sources: List<KbSource>`（知识来源列表，⊕ 需新增），`KbSource` 内嵌字段：`title: String`（⊕ 需新增）、`summary: String`（⊕ 需新增）、`relevanceScore: double`（⊕ 需新增）、`sourceUrl: String`（可选，⊕ 需新增） |
| Prompt 模板结构 | SYSTEM 角色：知识库问答系统提示，描述角色为"医疗知识库助手"，说明回答格式要求（引用来源、标注置信度）。模板变量：`{{queryText}}`、`{{queryScope}}`、`{{patientDescription}}`（从 DTO 中提取的患者简要描述）。USER 角色：包含 `queryText` + 患者上下文 |
| 结构化解析目标类型 | `KbQueryResponse`（JSON 格式），`sources` 列表中的每条来源需包含 `title` 和 `summary` |
| 模板变量提取策略 | 方式 B（自定义 `extractVariables()`）：从 `KbQueryRequest` 提取 `queryText`、`queryScope`；从 `patientId` 关联查询患者基本信息（年龄/性别/主要诊断等）后以 `patientDescription` 注入模板 |
| YAML 路由配置 | `KB_QUERY: { model: "qwen-plus", endpoint: "...", client: "HTTP_API", parameters: {temperature: 0.3, maxTokens: 2048} }` |
| 降级策略 | `[timeout, circuit-breaker]`（与 TRIAGE 一致） |

#### 3.11.6 `ScheduleCapabilityExecutor` — AI 医生排班（首次落地能力）

| 设计维度 | 说明 |
|---------|------|
| 职责 | 根据科室排班规则、医生可用性和工作量约束，生成或推荐排班方案 |
| 输入 DTO | `ScheduleRequest`（继承 `AiRequestBase`，⊕ 新增），扩展字段：`departmentId: String`（排班科室，⊕ 需新增）、`scheduleDate: LocalDate`（排班日期，⊕ 需新增）、`shiftType: ShiftType`（班次类型枚举：MORNING/AFTERNOON/NIGHT/ALL_DAY，可选，⊕ 需新增）、`requiredDoctors: int`（所需医生人数，可选，⊕ 需新增）、`preferredDoctors: List<String>`（优先排班的医生 ID 列表，可选，⊕ 需新增）、`constraints: List<ScheduleConstraint>`（排班约束，如"某医生当天不可排班"，⊕ 需新增） |
| 输出 DTO | `ScheduleResponse`（⊕ 新增），字段：`schedule: List<ShiftAssignment>`（排班结果列表，⊕ 需新增）。`ShiftAssignment` 内嵌字段：`doctorId: String`（⊕ 需新增）、`doctorName: String`（⊕ 需新增）、`shiftType: ShiftType`（⊕ 需新增）、`departmentId: String`（⊕ 需新增）、`date: LocalDate`（⊕ 需新增） |
| Prompt 模板结构 | SYSTEM 角色：排班助手系统提示，描述排班规则（每人每天至多一个班次、夜班后需休息 24 小时、优先考虑医生偏好等）。模板变量：`{{departmentName}}`、`{{scheduleDate}}`、`{{requiredDoctors}}`、`{{preferredDoctors}}`、`{{constraints}}`。USER 角色：包含排班请求详情 |
| 结构化解析目标类型 | `ScheduleResponse`（JSON 格式），`ShiftAssignment` 列表需完整覆盖所有班次 |
| 模板变量提取策略 | 方式 B（自定义）：从 `ScheduleRequest` 提取基本字段；使用 `departmentId` 查询科室名称和组织结构信息；将 `constraints` 列表格式化为自然语言约束描述字符串 |
| YAML 路由配置 | `SCHEDULE: { model: "qwen-plus", endpoint: "...", client: "HTTP_API", parameters: {temperature: 0.1, maxTokens: 2048} }` |
| 降级策略 | `[timeout, noop]`（排班场景可接受默认方案降级） |

#### 3.11.7 `DiscussionConclusionCapabilityExecutor` — AI 综合讨论结论（首次落地能力）

| 设计维度 | 说明 |
|---------|------|
| 职责 | 综合多轮会诊讨论记录，生成结构化讨论结论（诊断结论/治疗建议/随访计划等） |
| 输入 DTO | `DiscussionConclusionRequest`（继承 `AiRequestBase`），扩展字段：`discussionId: String`（讨论标识）、`discussionType: DiscussionType`（讨论类型枚举：MDT/CRITICAL_CARE/ROUTINE）、`transcripts: List<DiscussionTranscript>`（讨论记录列表）。`DiscussionTranscript` 内嵌字段：`speakerRole: String`（发言者角色）、`speakerName: String`、`content: String`（发言内容）、`timestamp: LocalDateTime`。可选字段：`previousConclusion: String`（既往结论，用于持续病案） |
| 输出 DTO | `DiscussionConclusionResponse`，字段：`summary: String`（讨论摘要）、`diagnosisConclusion: String`（诊断结论）、`treatmentRecommendations: List<String>`（治疗建议列表）、`followUpPlan: String`（随访计划，可选）、`pendingItems: List<String>`（待确认事项列表）、`confidenceLevel: ConfidenceLevel`（结论置信度） |
| Prompt 模板结构 | SYSTEM 角色：会诊总结助手系统提示，要求输出结构化结论（摘要→诊断→治疗建议→随访计划→待确认事项）。模板变量：`{{discussionType}}`、`{{speakerCount}}`（发言人数）、`{{transcriptSummary}}`（讨论记录摘要，因原始记录可能超 Token 限制，由 `doExecuteInternal()` 前置阶段先做摘要提取后注入）。USER 角色：包含完整的讨论记录或摘要 |
| 结构化解析目标类型 | `DiscussionConclusionResponse`（JSON 格式），`treatmentRecommendations` 和 `pendingItems` 为字符串列表 |
| 模板变量提取策略 | 方式 B（自定义）：`extractVariables()` 仅负责从 `DiscussionConclusionRequest` 提取 `discussionId`、`discussionType`、`speakerCount` 等基本字段，不做 LLM 调用。`transcriptSummary` 的生成（含 Token 估算、前置 LLM 压缩调用、截断回退）由 `doExecuteInternal()` 在实验分流之前的前置阶段统一执行（见 §4.1 DiscussionConclusionCapabilityExecutor.doExecuteInternal() 特化伪代码）。`extractVariables()` 不承担 LLM 调用职责，职责归属清晰。**前置 LLM 压缩调用契约**（归属 `doExecuteInternal()` 前置阶段）：使用 `LlmChatService.chat()` 方法（非 structuredChat）发送压缩摘要请求，不经过实验分流和模板渲染（直接使用硬编码压缩 Prompt "请将以下讨论记录压缩为 500 字以内的摘要，保留关键诊断意见和分歧点"）；设置独立超时 `transcriptSummaryTimeout`（默认 15 秒，通过 `@Value("${ai.execution.timeout.transcript-summary:15s}")` 注入），与主流程 `capabilityTimeout` 独立；若超时或调用失败，回退到截断 `transcripts` 原始文本至 2000 tokens 作为 `transcriptSummary`（确保主流程 LLM 调用仍可执行），日志 WARN 记录失败原因。**线程模型隔离方案**：(a) 该前置 LLM 调用在 `doExecuteInternal()` 内执行，而 `doExecuteInternal()` 运行在 `llmCallExecutor` 线程池的 Worker 线程中（`supplyAsync()` lambda）。每次讨论结论请求会额外阻塞 `llmCallExecutor` 线程最多 15 秒（`transcriptSummaryTimeout`），在整体 90s 端到端超时之上叠加阻塞。高并发下多个讨论结论请求同时进入时，`llmCallExecutor` 线程池可能被全部阻塞，影响其他能力（分诊、处方审核等）的 LLM 调用。**(b) 隔离方案（已定稿）**：引入独立线程池 `transcriptSummaryExecutor`，将前置压缩调用从 `doExecuteInternal()` 同步阻塞改为向 `transcriptSummaryExecutor` 提交 `CompletableFuture` 后异步等待。`transcriptSummaryExecutor` 配置见 `§3.9 AiPlatformConfig.transcriptSummaryExecutor()` Bean 定义（核心线程数 = min(availableProcessors, 4)，最大线程数 = 核心线程数 × 2，队列容量 = 20，拒绝策略 = CallerRunsPolicy）。此隔离确保讨论结论的前置压缩调用不占用 `llmCallExecutor` 线程，即使压缩调用全部阻塞，`llmCallExecutor` 仍可正常处理其他能力的 LLM 调用。**(c) YAML 配置隔离说明**：`transcript-summary: 15s` 所控制的超时作用于 `transcriptSummaryExecutor` 中提交的异步任务，与 `llmCallExecutor` 完全隔离。超时配置结合线程池隔离共同保障主调管线不受影响。**前置压缩调用轻量模型配置注入**：`compressionLightweightEndpoint` 和 `compressionLightweightClientType` 两个字段通过 `@Value("${ai.compression.lightweight-endpoint:compress-default}")` 和 `@Value("${ai.compression.lightweight-client-type:HTTP_API}")` 在 `DiscussionConclusionCapabilityExecutor` 构造器中注入，对应 YAML 配置 `ai.compression.lightweight-endpoint` 和 `ai.compression.lightweight-client-type`（见 §9.5）。主流程 YAML 配置示例：`DISCUSSION_CONCLUSION: { model: "qwen-max", endpoint: "...", client: "HTTP_API", parameters: {temperature: 0.2, maxTokens: 4096} }` 为完整管线主 LLM 调用配置，压缩调用使用独立的轻量模型配置（默认回退 `ClientType.HTTP_API`），两配置路径互不干扰 |
| YAML 路由配置 | `DISCUSSION_CONCLUSION: { model: "qwen-max", endpoint: "...", client: "HTTP_API", parameters: {temperature: 0.2, maxTokens: 4096} }`（讨论结论输出较长，maxTokens 使用 4096） |
| 降级策略 | `[timeout, circuit-breaker]` |
| 超时原因细化 | `DiscussionConclusionCapabilityExecutor` 重写 `refineTimeoutReason()`：在 `doExecuteInternal()` 中记录 `transcriptSummaryElapsedMs`（前置压缩调用耗时），`refineTimeoutReason()` 检查 `elapsedInDoExecuteInternal - transcriptSummaryElapsedMs < capabilityTimeout * 0.2`（即主流程剩余时间不足总超时的 20%）时，判定为前置压缩挤占超时，返回 `DegradationReason.TIMEOUT + ":transcriptSummaryCrowding"`；否则返回 `DegradationReason.TIMEOUT + ":primaryLlmTimeout"` |

**`DiscussionConclusionCapabilityExecutor` 构造器签名定义**（18 参数：父类 16 参数 + 2 个特有压缩配置参数）：

```
DiscussionConclusionCapabilityExecutor(
    @Autowired PromptTemplateManager promptTemplateManager,
    @Autowired ModelRouter modelRouter,
    @Autowired LlmChatService llmChatService,
    @Autowired StructuredOutputParser structuredOutputParser,
    @Autowired AiMetricsCollector metricsCollector,
    @Autowired SlidingWindowMetricsStore metricsStore,
    @Autowired ModelEndpointHealthManager endpointHealthManager,
    @Autowired AtomicReference<Map<String, List<DegradationStrategy>>> degradationStrategyMapRef,
    @Autowired(required = false) LocalRuleFallback<DiscussionConclusionRequest, DiscussionConclusionResponse> localRuleFallback,
    @Qualifier("capabilityTimeoutConfig") Map<String, Duration> capabilityTimeoutConfig,
    @Qualifier("parseTimeoutConfig") Map<String, Duration> parseTimeoutConfig,
    @Value("${ai.execution.timeout.parse.default:5s}") Duration parseTimeoutDefault,
    @Value("${ai.execution.timeout.thin-adapter-default:30s}") Duration thinAdapterTimeout,
    @Qualifier("thinAdapterPerCapabilityConfig") Map<String, Duration> thinAdapterPerCapabilityConfig,
    @Autowired ObjectMapper objectMapper,
    @Value("${ai.compression.lightweight-endpoint:compress-default}") String compressionLightweightEndpoint,
    @Value("${ai.compression.lightweight-client-type:HTTP_API}") ClientType compressionLightweightClientType,
    @Value("${ai.execution.timeout.transcript-summary:15s}") Duration transcriptSummaryTimeout) {
    // super()：传递父类 16 参数（前 16 个入参），后 2 个特有参数由本构造器直接赋值
    super(DiscussionConclusionRequest.class, promptTemplateManager, modelRouter, llmChatService,
          structuredOutputParser, metricsCollector, metricsStore, endpointHealthManager,
          degradationStrategyMapRef, localRuleFallback, capabilityTimeoutConfig, parseTimeoutConfig,
          parseTimeoutDefault, thinAdapterTimeout, thinAdapterPerCapabilityConfig, objectMapper);
    this.compressionLightweightEndpoint = compressionLightweightEndpoint;
    this.compressionLightweightClientType = compressionLightweightClientType;
    this.transcriptSummaryTimeout = transcriptSummaryTimeout;
}
```

**`DiscussionConclusionCapabilityExecutor` 特有字段声明**（对应 §2.3 类图补充）：

| 字段 | 类型 | 注入方式 | 说明 |
|------|------|---------|------|
| `compressionLightweightEndpoint` | String | `@Value("${ai.compression.lightweight-endpoint:compress-default}")` | 前置压缩调用的轻量模型端点标识，不经过 `ModelRouter` 路由 |
| `compressionLightweightClientType` | ClientType | `@Value("${ai.compression.lightweight-client-type:HTTP_API}")` | 前置压缩调用的客户端类型，默认 HTTP_API |
| `transcriptSummaryTimeout` | Duration | `@Value("${ai.execution.timeout.transcript-summary:15s}")` | 前置压缩调用独立超时，与主流程 `capabilityTimeout` 解耦 |

**辅助方法定义**：以下三个辅助方法在 §4.1 DiscussionConclusionCapabilityExecutor.doExecuteInternal() 特化伪代码中使用，本处提供正式方法签名与行为契约：

- **`preciseTokenCount(List<DiscussionTranscript> transcripts) → int`**：使用 jtokkit 库的 cl100k_base 编码对 transcripts 做精确 Token 计数。逐条遍历 `transcript.content`，调用 `encoding.encode(content).size()` 累加精确 Token 数，再加回每条消息首部固定开销 4 tokens（roleMarkerOverhead，如 "user: ", "assistant: "）。返回值是 transcripts 的精确 Token 计数，不含字符估算偏差。线程安全：jtokkit Encoding 实例不可变，`encoding.encode()` 为线程安全操作，可在 `parallelStream()` 中并行执行多条 transcript 的编码。实现位置：归属 `DiscussionConclusionCapabilityExecutor` 的 private 方法。

- **`formatTranscripts(List<DiscussionTranscript> transcripts) → String`**：将 `DiscussionTranscript` 列表格式化为 LLM 可读的对话文本。每条转录格式为 `"[{speakerRole}] {speakerName} ({timestamp}):\n{content}\n"`。最终返回所有转录拼接后的完整字符串。实现位置：归属 `DiscussionConclusionCapabilityExecutor` 的 private 方法。

- **`truncateTranscripts(List<DiscussionTranscript> transcripts, int maxTokens) → String`**：在压缩调用失败的回退场景中，截断 transcripts 至 `maxTokens` 以内的安全长度。实现策略：从 transcripts 列表末尾开始逐条移除（即优先保留较新的发言），移除后调用 `preciseTokenCount()`（jtokkit 可用时）或 `formatTranscripts()` + 字符估算（jtokkit 不可用时）检查剩余文本是否在 `maxTokens` 以内。返回截断后格式化的转录文本字符串（格式同 `formatTranscripts()`）。保证返回值不超过 `maxTokens` 的保守上限。实现位置：归属 `DiscussionConclusionCapabilityExecutor` 的 private 方法。

#### 上述 7 项能力的共同约束

- 所有 7 项能力的 DTO 均在 Phase 5 底座切流时完成 `AiRequestBase` 继承改造
- **DTO 扩展字段编码责任**：各能力特化表（§3.11.1–§3.11.7）列出的 DTO 扩展字段（标注"⊕ 需新增"的字段）均为 **Phase 5 底座新增设计**，当前代码库中全部不存在。实现者在完成 `AiRequestBase` 继承改造的同时，需根据特化表定义逐 DTO **新增这些业务字段及对应的 getter/setter/Jackson 注解**（或使用 Lombok `@Data` 自动生成）。底座编码侧负责补齐全部扩展字段，Phase 4 模块不参与 7 项底座能力 DTO 的字段定义。此约束与 §3.5 过渡策略中"7 项底座能力的 DTO 完成基类继承改造"一致——继承改造和业务字段补齐在同一 Changelist 中完成
- 结构化解析统一使用 `JsonStructuredOutputParser`（Jackson 反序列化），各 DTO 标注 `@JsonIgnoreProperties(ignoreUnknown = true)` 确保字段容错
- Prompt 模板的兜底策略：7 项能力均需在 `ai.template.fallback` 中配置兜底 Prompts（参见 §9.5），或通过 `PromptTemplate` 表初始化 DRAFT 状态记录作为兜底
- YAML 路由配置中的 `model`/`endpoint` 值为 Phase 5 初始推荐值，实际部署时需根据供应商可用性和成本策略调整
- 各能力 DTO 改造完成后需执行 §3.5 定义的 3 个兼容性验证场景（旧 JSON→新 DTO、新 JSON→旧消费者、新 JSON→新 DTO 含基类继承），确保 Jackson 序列化/反序列化正确性

---

### 3.12 薄适配器能力特化设计

以下 6 项薄适配器能力（Phase 4 独立接入，不迁移至底座完整管线）的薄适配器型 CapabilityExecutor 特化设计集中表，与 §3.11 底座能力特化格式一致：

#### 3.12.1 `DiagnosisCapabilityExecutor` — AI 智能诊断（薄适配器）

| 设计维度 | 说明 |
|---------|------|
| 能力标识 | `DIAGNOSIS` |
| Phase 4 服务接口 | `com.aimedical.modules.diagnosis.service.DiagnosisService.execute(DiagnosisRequest)` |
| 输入 DTO | `DiagnosisRequest`（当前为空类，🟡 依赖 Phase 4 DTO 改造补齐业务字段） |
| 输出 DTO | `DiagnosisResponse`（Phase 4 模块定义） |
| 底座管线步骤 | 降级预检 → 委托 Phase 4 服务（`thinAdapterTimeout` 超时控制，默认 30s） → 指标采集 |
| DTO 扩展字段 | `symptomDescription: String`（⊕ 需新增，由 Phase 4 模块负责）；`AiRequestBase` 继承字段 4 个（⚠️ 仅在 Phase 4 模块决定继承时生效） |
| 底座侧 Phase 4 Service 引用 | `diagnosisService`（构造器注入，`provided` 作用域） |
| 异常契约 | `DiagnosisException` ✓ 已有 `getErrorCode()`；降级原因 `DegradationReason.INFRASTRUCTURE_ERROR` |
| 依赖状态 | 🟡 依赖 Phase 4 DTO 改造 |
| 降级原因 | 委托超时: `DegradationReason.TIMEOUT + ":ThinAdapterTimeout"`；基础设施异常: `DegradationReason.INFRASTRUCTURE_ERROR`；业务异常: `AiResult.failure()` 返回 |

#### 3.12.2 `AnalysisReportForInspectionCapabilityExecutor` — AI 智能检查报告（薄适配器）

| 设计维度 | 说明 |
|---------|------|
| 能力标识 | `ANALYSIS_REPORT_INSPECTION` |
| Phase 4 服务接口 | `com.aimedical.modules.inspection.service.AnalysisReportForInspectionService.execute(AnalysisReportForInspectionRequest)` |
| 输入 DTO | `AnalysisReportForInspectionRequest`（当前为空类，🟡 依赖 Phase 4 DTO 改造） |
| 输出 DTO | `AnalysisReportForInspectionResponse`（Phase 4 模块定义） |
| 底座管线步骤 | 降级预检 → 委托 Phase 4 服务（`thinAdapterTimeout` 超时控制，默认 30s） → 指标采集 |
| DTO 扩展字段 | `reportData: String`（⊕ 需新增，由 Phase 4 模块负责） |
| 底座侧 Phase 4 Service 引用 | `analysisReportForInspectionService`（构造器注入） |
| 异常契约 | `InspectionException` ✓ 已有 `getErrorCode()` |
| 依赖状态 | 🟡 依赖 Phase 4 DTO 改造 |

#### 3.12.3 `AnalysisReportForLabTestCapabilityExecutor` — AI 智能检验报告（薄适配器）

| 设计维度 | 说明 |
|---------|------|
| 能力标识 | `ANALYSIS_REPORT_LABTEST` |
| Phase 4 服务接口 | `com.aimedical.modules.labtest.service.AnalysisReportForLabTestService.execute(AnalysisReportForLabTestRequest)` |
| 输入 DTO | `AnalysisReportForLabTestRequest`（当前为空类，🟡 依赖 Phase 4 DTO 改造） |
| 输出 DTO | `AnalysisReportForLabTestResponse`（Phase 4 模块定义） |
| 底座管线步骤 | 降级预检 → 委托 Phase 4 服务（`thinAdapterTimeout` 超时控制） → 指标采集 |
| DTO 扩展字段 | `reportData: String`（⊕ 需新增，由 Phase 4 模块负责） |
| 底座侧 Phase 4 Service 引用 | `analysisReportForLabTestService`（构造器注入） |
| 异常契约 | `LabTestException` ✓ 已有 `getErrorCode()` |
| 依赖状态 | 🟡 依赖 Phase 4 DTO 改造 |

#### 3.12.4 `ImageAnalysisCapabilityExecutor` — AI 影像分析（薄适配器）

| 设计维度 | 说明 |
|---------|------|
| 能力标识 | `IMAGE_ANALYSIS` |
| Phase 4 服务接口 | `com.aimedical.modules.image.service.ImageAnalysisService.execute(ImageAnalysisRequest)` |
| 输入 DTO | `ImageAnalysisRequest`（当前为空类，🟡 依赖 Phase 4 DTO 改造） |
| 输出 DTO | `ImageAnalysisResponse`（Phase 4 模块定义） |
| 底座管线步骤 | 降级预检 → 委托 Phase 4 服务（`thinAdapterTimeout` 45s，因影像分析耗时高） → 指标采集 |
| DTO 扩展字段 | `imageData: String`、`analysisType: String`（⊕ 需新增，由 Phase 4 模块负责） |
| 底座侧 Phase 4 Service 引用 | `imageAnalysisService`（构造器注入） |
| 异常契约 | `ImageAnalysisException` △ 需验证 `getErrorCode()` |
| 依赖状态 | 🟡 依赖 Phase 4 DTO 改造 |

#### 3.12.5 `RecommendExaminationCapabilityExecutor` — AI 开立检查检验（薄适配器）

| 设计维度 | 说明 |
|---------|------|
| 能力标识 | `RECOMMEND_EXAM` |
| Phase 4 服务接口 | `com.aimedical.modules.examination.service.RecommendExaminationService.execute(RecommendExaminationRequest)` |
| 输入 DTO | `RecommendExaminationRequest`（当前为空类，🟡 依赖 Phase 4 DTO 改造） |
| 输出 DTO | `RecommendExaminationResponse`（Phase 4 模块定义） |
| 底座管线步骤 | 降级预检 → 委托 Phase 4 服务（`thinAdapterTimeout` 20s，轻量推理） → 指标采集 |
| DTO 扩展字段 | `diagnosisCode: String`、`patientInfo: PatientInfo`（⊕ 需新增，由 Phase 4 模块负责） |
| 底座侧 Phase 4 Service 引用 | `recommendExaminationService`（构造器注入） |
| 异常契约 | `ExaminationException` △ 需验证 `getErrorCode()` |
| 依赖状态 | 🟡 依赖 Phase 4 DTO 改造 |

#### 3.12.6 `RecommendExecutionOrderCapabilityExecutor` — AI 执行顺序推荐（薄适配器）

| 设计维度 | 说明 |
|---------|------|
| 能力标识 | `RECOMMEND_EXEC_ORDER` |
| Phase 4 服务接口 | `com.aimedical.modules.execution.service.RecommendExecutionOrderService.execute(RecommendExecutionOrderRequest)` |
| 输入 DTO | `RecommendExecutionOrderRequest`（当前为空类，🟡 依赖 Phase 4 DTO 改造） |
| 输出 DTO | `RecommendExecutionOrderResponse`（Phase 4 模块定义） |
| 底座管线步骤 | 降级预检 → 委托 Phase 4 服务（`thinAdapterTimeout` 20s） → 指标采集 |
| DTO 扩展字段 | `examinationList: List<String>`、`patientInfo: PatientInfo`（⊕ 需新增，由 Phase 4 模块负责） |
| 底座侧 Phase 4 Service 引用 | `recommendExecutionOrderService`（构造器注入） |
| 异常契约 | `ExecutionOrderException` △ 需验证 `getErrorCode()` |
| 依赖状态 | 🟡 依赖 Phase 4 DTO 改造 |

**6 项薄适配器能力的共同约束**：

1. 底座侧不为其 DTO 新增业务字段或继承 `AiRequestBase`，字段补齐责任归属 Phase 4 模块
2. 底座切流初期所有 6 项薄适配器 DTO 均为空类，`doExecuteInternal()` 跳过委托调用直接降级，降级原因 `DegradationReason.INFRASTRUCTURE_ERROR + ":Phase4DtoEmpty"`
3. Phase 4 模块完成 DTO 改造且通过底座 4 个 null-safe 提取方法验证后，薄适配器恢复正常委托调用
4. 6 项薄适配器共享同一 `thinAdapterTimeout` 默认值（30s），各能力通过 `thin-adapter.per-capability` YAML 配置差异化覆盖（如影像分析 45s、开立检查检验 20s）
5. Phase 4 响应 DTO 的可选元数据接口 `Phase4ServiceMetaCapable` 由 Phase 4 模块决定是否实现，底座通过 `instanceof` 检测后安全调用

---

## 4. 关键行为契约

### 4.1 AI 能力统合调用管线

```
// AiOrchestrator 的 13 个 AiService 方法实现遵循统一委托模式：
//   @Override public CompletableFuture<AiResult<TriageResponse>> triage(TriageRequest req) {
//       return handle("TRIAGE", req);
//   }
//   @Override public CompletableFuture<AiResult<PrescriptionCheckResponse>> prescriptionCheck(PrescriptionCheckRequest req) {
//       return handle("RX_AUDIT", req);
//   }
//   ... 其余 11 个方法同理
// handle() 是 AiOrchestrator 的唯一实际执行入口，13 个 AiService 方法仅为
// 委托入口与方法签名实现。每个 AiService 方法通过硬编码的 capabilityId 调用
// handle()，与 §3.1 能力标识映射表一一对应。

AiOrchestrator.handle(capabilityId, request):
  1. try:
  2.   executor = executorMap.get(capabilityId)
  3.   if executor == null:
  4.     // 未注册能力标识：以 CompletableFuture.completedFuture() 包装失败结果返回，不抛出同步异常
  5.     // 确保调用方统一的 CompletableFuture 契约不被破坏，异常场景同样走 AiResult 包装路径
  6.     log.warn("未注册能力标识: {}, 返回失败结果", capabilityId)
  7.     return CompletableFuture.completedFuture(AiResult.failure("未注册能力标识: " + capabilityId))
  8.   result = executor.execute(request, capabilityId)
  9.   return result
  10. catch (Exception e):
  11.   // 捕获 executor.execute() 或下游管线中的不可预知异常
  12.   // 处理方式：记录错误日志 + 返回 AiResult.failure() 而非传播为 CompletionException
  13.   log.error("CapabilityExecutor 执行异常: capabilityId={}, error={}", capabilityId, e.getMessage(), e)
  14.  // 从 request 对象提取可用上下文，避免异常场景下丢失关键就诊信息（visitId/patientId/departmentId/sessionId）
  15.  inputSummary = request != null ? StringUtils.truncate(request.toString(), 500) : null
   16.  // 双重提取策略：优先从 DTO 提取（instanceof），若提取到的上下文字段均为 null
   17.  //（如 CGLIB 代理导致 instanceof 失效，或 DTO 中这些字段为空），降级到 HTTP Header 兜底
   18.  if request instanceof AiRequestBase:
   19.    departmentId = request.getDepartmentId()
   20.    visitId = request.getVisitId()
   21.    patientId = request.getPatientId()
   22.    sessionId = request.getSessionId()
   23.  // 第二重提取：CGLIB 代理场景下 instanceof 可能为 false（增强子类不继承基类类型信息），
   24.  // 或 AiRequestBase 的上下文字段因序列化/拷贝等原因丢失，此时从 HTTP Header 独立提取作为兜底
   25.  // 双重提取确保任何场景下 catch 块不会丢失就诊上下文
    26.  if departmentId == null || visitId == null || patientId == null || sessionId == null:
    27.    // Phase 4 薄适配器 DTO 暂不继承 AiRequestBase，从当前 HTTP 请求的 Header 中独立提取
    28.    // 此 catch 在 Tomcat 容器线程执行，RequestContextHolder 上下文可用
    29.    if departmentId == null: departmentId = RequestContextUtils.extractFromRequestContext("X-Department-ID")
    30.    if visitId == null: visitId = RequestContextUtils.extractFromRequestContext("X-Visit-ID")
    31.    if patientId == null: patientId = RequestContextUtils.extractFromRequestContext("X-Patient-ID")
    32.    if sessionId == null: sessionId = RequestContextUtils.extractFromRequestContext("X-Session-ID")
     33.    // 若 HTTP Header 也未携带，各字段保持 null（AiCallRecord 中对应字段可空）
     34.  // 提取 callerRole/callerId：统一委托至 RequestContextUtils，与 AbstractCapabilityExecutor.extractCallerRole()/extractCallerId()
     35.  // 共享同一份 ROLE_ 前缀过滤逻辑，消除两处提取方式不一致问题
      36.  callerRole = RequestContextUtils.extractCallerRole()
      37.  callerId = RequestContextUtils.extractCallerId()
       38.  metricsCollector.record(AiCallRecord.failure(capabilityId, LocalDateTime.now(), 0L,
       39.      DegradationReason.INTERNAL_ERROR + ":" + e.getClass().getSimpleName(), e.getMessage(),
       40.      departmentId, inputSummary, visitId, patientId, sessionId, callerRole, callerId, null, null))
      41.  slidingWindowMetricsStore.recordFailure(capabilityId)
      42.  return CompletableFuture.completedFuture(AiResult.failure("AI服务暂时不可用，请稍后重试"))
   // 注意：此 catch 仅捕获 AiOrchestrator 层面的意外异常
   // CapabilityExecutor 管线内部的预期异常（LLM 超时、解析失败等）已在 execute() 内部捕获并以降级路径处理，不会传播至此

// AbstractCapabilityExecutor.execute() — 模板方法（固定步骤，所有子类共享）
// 职责：线程池外提取 ThreadLocal 上下文 → 防御性拷贝 → 降级预检（前置，不入池排队）→ 整体超时兜底 → 调用子类特化的 doExecuteInternal()
AbstractCapabilityExecutor.execute(request, capabilityId):
  // 线程池外提取 ThreadLocal 上下文，避免 supplyAsync 中 SecurityContextHolder / RequestContextHolder 丢失
  startTime = System.currentTimeMillis()
  auth = SecurityContextHolder.getContext().getAuthentication()
  userId = auth != null ? auth.getName() : "SYSTEM"
  departmentId = doExtractDepartmentId(request)
  visitId = doExtractVisitId(request)
  patientId = doExtractPatientId(request)
  sessionId = doExtractSessionId(request)
  callerRole = extractCallerRole()
  callerId = extractCallerId()

  // 防御性拷贝：将拷贝结果存入新局部变量 defensiveCopy，使原始 request 变量保持 effectively final，确保 lambda 可捕获
  // ⚠️ 不可变 DTO 未标注 Jackson 注解时的回退：若 DTO 为不可变（所有字段 final，无 setter）且未标注
  // @JsonCreator/@ConstructorProperties/@Jacksonized 等注解，objectMapper.convertValue() 将抛出异常
  // （如 IllegalArgumentException/BeanDeserializationException），导致本应正常执行的能力调用被降级。
  // 处理策略：捕获异常后使用原始 request（防御性拷贝失败时放弃拷贝，接受 request 可能被下游修改的风险）。
  // 日志 WARN 输出异常信息并建议在 DTO 上补全 Jackson 注解。
  defensiveCopy = null
  try:
    defensiveCopy = objectMapper.convertValue(request, request.getClass())
  catch (Exception e):
    log.warn("防御性拷贝失败: capabilityId={}, requestType={}, 回退到原始 request, error={}",
        capabilityId, request.getClass().getSimpleName(), e.getMessage())
    defensiveCopy = request  // 回退到原始 request

  // 预先计算 inputSummary（降级路径和正常管线均需使用，在容器线程处定义避免 lambda 重复计算）
  inputSummary = defensiveCopy != null ? StringUtils.truncate(defensiveCopy.toString(), 500) : null

  // 降级预检（移至 supplyAsync 之前，容器线程执行，熔断器 OPEN 状态的请求无需排队等待线程池）
  // 若任一策略判定降级，直接返回 CompletableFuture.completedFuture() 完成降级结果，不入线程池
  context = slidingWindowMetricsStore.buildDegradationContext(capabilityId, this.getClass().getSimpleName())
  context.setDepartmentId(departmentId)
  // 运行时从 AtomicReference 每轮获取最新策略 Map 快照，支持 degradation.strategies 热加载
  Map<String, List<DegradationStrategy>> currentStrategyMap = this.degradationStrategyMapRef.get()
  for each strategy in currentStrategyMap.getOrDefault(capabilityId, Collections.emptyList()) (sorted by getOrder() asc):
    if strategy.shouldDegrade(context):
      // 降级预检命中：使用 DegradationReason.STRATEGY_TRIGGERED 拼接策略类名，与枚举体系一致
      // 预检降级路径中 modelId=null（降级发生在模型路由之前，目标模型未确定）
      // sentinelReason=null（降级发生在实验分流之前，无实验分组上下文）
      return CompletableFuture.completedFuture(doDegrade(startTime, DegradationReason.STRATEGY_TRIGGERED + ":" + strategy.getClass().getSimpleName(), defensiveCopy, capabilityId, departmentId, callerRole, callerId, visitId, patientId, sessionId, inputSummary, null, null, null, null))

  // 正常请求入线程池执行
  // elapsedInDoExecuteInternal 由 lambda 入口处设置（volatile long），
  // 供子类重写的 refineTimeoutReason() 在超时回调中读取管线已消耗时间（含排队等待）
  future = CompletableFuture.supplyAsync(() -> {
    this.elapsedInDoExecuteInternal = System.currentTimeMillis() - startTime
    return doExecuteInternal(startTime, defensiveCopy, capabilityId, departmentId, userId, sessionId, callerRole, callerId, visitId, patientId, inputSummary)
  }, llmCallExecutor)

    // 整体管线端到端超时兜底（各能力独立配置，默认 60 秒）
    // 覆盖场景：模板渲染（DB 查询）、实验分流（JPA 查询）、变量提取（Jackson 转换）等非 LLM 调用步骤的潜在挂起
    capabilityTimeout = capabilityTimeoutConfig.getOrDefault(capabilityId, Duration.ofSeconds(60))
    return future.orTimeout(capabilityTimeout.toMillis(), TimeUnit.MILLISECONDS)
      .exceptionally(ex -> {
        if (ex instanceof TimeoutException):
          log.warn("CapabilityExecutor 整体执行超时: capabilityId={}, timeout={}ms", capabilityId, capabilityTimeout.toMillis())
          // 超时原因细化：子类可重写 refineTimeoutReason() 提供更精确的超时原因。
          // 默认返回 DegradationReason.TIMEOUT，无额外细化标记。
          // DiscussionConclusionCapabilityExecutor 通过此方法区分"前置压缩挤占"与"主 LLM 调用超时"。
          refinedReason = refineTimeoutReason(capabilityId, elapsedInDoExecuteInternal, request)
          // 超时降级路径中 modelId=null（超时可能发生在管线任意步骤，模型路由可能已完成或未完成，modelId 不可靠，统一传 null）
          // sentinelReason=null（超时可能发生在实验分流前或后，不可靠，统一传 null）
          return doDegrade(startTime, refinedReason, defensiveCopy, capabilityId, departmentId, callerRole, callerId, visitId, patientId, sessionId, inputSummary, null, null, null, null)
        // 非超时异常（如执行中抛出的 CompletionException 包装的 RuntimeException）：传播为 CompletionException
        throw new CompletionException(ex)
      })

// AbstractCapabilityExecutor.doExecuteInternal() — 子类特化的正常管线实现
// 以下为完整管线子类（7 项底座能力）的典型实现：
// 注意：userId/sessionId/callerRole/callerId/visitId/patientId/inputSummary 由 execute() 入口处提取并以参数形式传入，方法体内不再访问 SecurityContextHolder
doExecuteInternal(startTime, request, capabilityId, departmentId, userId, sessionId, callerRole, callerId, visitId, patientId, inputSummary):
    outputType = getOutputType()
    variables = extractVariables(request)

    // 实验分流（包裹 try-catch，异常时日志 WARN + 降级到哨兵分组，与 §5.1 承诺一致）
    // ⚠️ 异常场景使用 ExperimentAssignment.createErrorFallback()（groupId="experiment-error"，
    // promptVersion=null），与 ExperimentManager.assign() 正常未命中时返回的
    // ExperimentAssignment.createDefault()（groupId="default"）可区分。
    // 下游实验效果分析报表通过 groupId 字段区分正常基线数据与异常降级污染数据。
    // 哨兵值 "experiment-error" 不落入任何合法的实验分组 group_id 范围，确保上游报表过滤安全
    boolean experimentAssignFailed = false
    try:
      assignment = experimentManager.assign(capabilityId, userId, sessionId)
    catch (Exception e):
      log.warn("实验分流异常: capabilityId={}, error={}", capabilityId, e.getMessage(), e)
      assignment = ExperimentAssignment.createErrorFallback()
      experimentAssignFailed = true

    // 从实验分组中提取 promptVersion（后续降级路径需传入 AiCallRecord，确保实验分流后的降级场景不丢失实验分组关联分析能力）
    // sentinelReason 用于 AiCallRecord 工厂方法——实验分流异常时将 prompt_version 写为 -1 哨兵值
    promptVersion = assignment.getTargetPromptVersion()
    sentinelReason = experimentAssignFailed ? "EXPERIMENT_ASSIGN_ERROR" : null

    // 模板渲染（包裹 try-catch，异常时日志 WARN + 使用能力内硬编码兜底 Prompt，与 §5.1 承诺一致）
    try:
      renderedPrompt = promptTemplateManager.render(capabilityId, departmentId, variables, promptVersion)
    catch (Exception e):
      log.warn("模板渲染失败: capabilityId={}, error={}", capabilityId, e.getMessage(), e)
      renderedPrompt = promptTemplateManager.getFallbackPrompt(capabilityId)

    modelRoute = modelRouter.route(capabilityId, assignment)
    if modelRoute == null:
      return doDegrade(startTime, DegradationReason.NO_AVAILABLE_ROUTE, request, capabilityId, departmentId, callerRole, callerId, visitId, patientId, sessionId, inputSummary, null, promptVersion, null, sentinelReason)

    // 健康检查（模型路由之后、LLM 调用之前）
    // endpointId 暂存以在 LLM 调用后传递给 recordCallResult()
    endpointId = modelRoute.getEndpointId()
    endpointState = endpointHealthManager.getState(endpointId)
    if endpointState == DEGRADED:
      // DEGRADED 语义：仍然尝试调用，仅日志 WARN + 推送到告警/健康检查体系
      // 不记录 AiCallRecord.failure()——本次调用尚未完成，不应预记为失败
      // 实际的调用结果（成功或失败）在 LLM 调用完成后按正常路径记录一次
      // 若在此处预记 failure() 而后 LLM 调用成功再记 success()，将导致同一次调用同时记入失败和成功两个分类，三个核心指标（失败率/成功率/总调用量）均失真
      log.warn("端点性能退化: endpointId={}, state=DEGRADED, 仍然尝试调用", endpointId)
    if endpointState == UNAVAILABLE:
      if not endpointHealthManager.tryProbe(endpointId):
        return doDegrade(startTime, DegradationReason.ENDPOINT_UNAVAILABLE, request, capabilityId, departmentId, callerRole, callerId, visitId, patientId, sessionId, inputSummary, null, promptVersion, modelRoute.getModelId(), sentinelReason)

    // 构造 LlmChatOptions：采用两阶段填充策略（参见 §3.2 LlmChatOptions 章节）
    // 阶段一（基值填充）：从 ModelRoute.parameters Map 按约定 key 名提取设置对应字段
    options = new LlmChatOptions()
    options.setModelId(modelRoute.getModelId())
    if modelRoute.getParameters() != null:
        if modelRoute.getParameters().containsKey("temperature"):
            options.setTemperature((Double) modelRoute.getParameters().get("temperature"))
        if modelRoute.getParameters().containsKey("maxTokens"):
            options.setMaxTokens((Integer) modelRoute.getParameters().get("maxTokens"))
        if modelRoute.getParameters().containsKey("stopSequences"):
            options.setStopSequences((List<String>) modelRoute.getParameters().get("stopSequences"))
        if modelRoute.getParameters().containsKey("topP"):
            options.setTopP((Double) modelRoute.getParameters().get("topP"))
        if modelRoute.getParameters().containsKey("frequencyPenalty"):
            options.setFrequencyPenalty((Double) modelRoute.getParameters().get("frequencyPenalty"))
        if modelRoute.getParameters().containsKey("presencePenalty"):
            options.setPresencePenalty((Double) modelRoute.getParameters().get("presencePenalty"))
    // 阶段二（调用方覆盖）：根据本次调用上下文（如实验分组）覆盖特定字段
    // assignment.getTargetModelId() 优先级高于 modelRoute.getModelId()
    if assignment != null && assignment.getTargetModelId() != null:
        options.setModelId(assignment.getTargetModelId())
    // 阶段二的赋值始终优先生效，即 CapabilityExecutor 显式覆盖 > ModelRoute.parameters Map 配置值 > 字段类型默认值

    // 构造 LlmChatRequest：渲染后的 Prompt 作为 SYSTEM message，DTO 数据转为 USER message
    // tools 字段通过全参构造器统一赋值（末尾 @Nullable 参数），消除独立 setter 遗漏风险
    chatRequest = new LlmChatRequest(
        messages: List.of(
            new LlmChatMessage(LlmChatMessageRole.SYSTEM, renderedPrompt),
            new LlmChatMessage(LlmChatMessageRole.USER, request.toString())),
        options: options,
        clientType: modelRoute.getClientType(),
        tools: ChatToolDefinition.fromOutputType(outputType))  // 仅 structuredChat() 路径使用，chat() 路径忽略

    // 声明变量，确保 try-catch 后的公共指标采集代码路径可访问
    // structuredChat 成功路径和 chat()+parse() 回退路径各自赋值 retryCount、tokenUsage 和 parsedResult
    retryCount = 0
    tokenUsage = null
    Object parsedResult = null

    // structuredChat 内部超时拆分：使用剩余超时窗口动态分配，而非原始 capabilityTimeout 的固定比例
    // 由于 supplyAsync() 启动后 orTimeout() 已开始计时，前置步骤（防御性拷贝、ThreadLocal 提取、降级预检、
    // 实验分流、模板渲染、模型路由、健康检查、LlmChatOptions 构造等）已消耗部分超时窗口。
    // 若仍按原始 capabilityTimeout 的 60%/40% 分配，当前置步骤消耗较大时 orTimeout 剩余时间
    // 可能显著小于 structuredChatTimeout，导致超时分类与降级原因记录失准。
    // 解法：在 lambda 入口处即时计算 remainingBeforeOrTimeout = capabilityTimeout - elapsedBeforeLambda，
    // 然后按 60%/40% 比例重新分配 structuredChat 和 chat 回退的内部超时阈值。
    elapsedBeforeLambda = System.currentTimeMillis() - startTime
    remainingBeforeOrTimeout = capabilityTimeout.toMillis() - elapsedBeforeLambda
    if remainingBeforeOrTimeout <= 0:
      // 前置步骤已耗尽超时窗口，直接降级
      return doDegrade(startTime, DegradationReason.TIMEOUT + ":前置步骤耗尽超时窗口", request, capabilityId, departmentId, callerRole, callerId, visitId, patientId, sessionId, inputSummary, null, promptVersion, modelRoute.getModelId(), sentinelReason)
    structuredChatTimeout = Duration.ofMillis((long)(remainingBeforeOrTimeout * 0.6))
    chatFallbackTimeout = Duration.ofMillis(remainingBeforeOrTimeout - structuredChatTimeout.toMillis())

    // 优先使用 structuredChat（模型原生结构化输出模式，如 tool_use / function_call / JSON mode）
    // structuredChat 失败时，区分三类异常：
    //   (1) 结构化格式不支持异常（StructuredOutputNotSupportedException）— 回退到 chat() + StructuredOutputParser.parse()
    //   (2) 基础设施异常（HTTP 5xx、超时等）— 直接降级，避免重复发送 chat() 请求徒增端到端耗时
    //   (3) structuredChat 内部超时（TimeoutException）— 直接降级，超时原因非格式不支持，chat 回退同样会超时
    // LLM 调用完成后无论成功/失败均调用 endpointHealthManager.recordCallResult() 更新端点健康状态，
    // 使 DEGRADED→CONNECTED（调用成功）或 CONNECTED/DEGRADED→UNAVAILABLE（连续失败）的状态转换路径可触发
    try:
      // structuredChat() 返回 CompletableFuture<AiResult<StructuredChatResult<T>>>，需两层解包
      // 使用独立内部超时（capabilityTimeout 的 60%），超时后直接降级而非回退 chat()
      // .join() 外层包裹 try-catch，处理 CompletionException（CompletableFuture 包装的原始异常）
      try:
        structuredChatFuture = llmChatService.structuredChat(chatRequest, outputType)
            .orTimeout(structuredChatTimeout.toMillis(), TimeUnit.MILLISECONDS)
        aiResult = structuredChatFuture.join()
        // AiResult 状态检查：getData() 前验证 isSuccess()
        if aiResult.isSuccess():
          structuredChatResult = aiResult.getData()
          parsedResult = structuredChatResult.getData()
          retryCount = structuredChatResult.getRetryCount()  // 含 structuredChat 内部实现层重试（如 HTTP 5xx 自动重试 1 次）
          tokenUsage = structuredChatResult.getUsage()
          endpointHealthManager.recordCallResult(endpointId, true, System.currentTimeMillis() - startTime)
          // → [shared_success_handler] — 控制流转至共用成功处理段
        else:
            // AiResult 非成功状态：走降级路径
            outputSummary = aiResult.getErrorMessage() != null ? StringUtils.truncate(aiResult.getErrorMessage(), 500) : null
            elapsedMs = System.currentTimeMillis() - startTime
            endpointHealthManager.recordCallResult(endpointId, false, elapsedMs)
            // 降级记录统一由 doDegrade() 承担，此处不预记
            return doDegrade(startTime, DegradationReason.INTERNAL_ERROR, request, capabilityId, departmentId, callerRole, callerId, visitId, patientId, sessionId, inputSummary, outputSummary, promptVersion, modelRoute.getModelId(), sentinelReason)
      catch (java.util.concurrent.CompletionException ce):
        // 拆解 CompletionException 获取原始异常，按分类重新路由
        // orTimeout().join() 将 TimeoutException 包装为 CompletionException(TimeoutException)，
        // 因此 catch(TimeoutException) 为死代码，统一在此处拆解后按原始异常类型分类处理
        originalCause = ce.getCause()
        if originalCause instanceof java.util.concurrent.TimeoutException:
          // structuredChat 内部超时（在 60% 窗口内）：直接降级，不尝试 chat() 回退
          // 超时原因可能是 LLM 响应缓慢而非结构化格式不支持，chat() 回退同样会超时
          elapsedMs = System.currentTimeMillis() - startTime
          endpointHealthManager.recordCallResult(endpointId, false, elapsedMs)
          // 降级记录统一由 doDegrade() 承担，此处不预记
          return doDegrade(startTime, DegradationReason.TIMEOUT + ":structuredChatTimeout", request, capabilityId, departmentId, callerRole, callerId, visitId, patientId, sessionId, inputSummary, null, promptVersion, modelRoute.getModelId(), sentinelReason)
        else if originalCause instanceof StructuredOutputNotSupportedException:
          throw (StructuredOutputNotSupportedException) originalCause
        else if originalCause instanceof LlmInfrastructureException:
          throw (LlmInfrastructureException) originalCause
        else:
          // 非已知异常类型，按基础设施异常处理
          elapsedMs = System.currentTimeMillis() - startTime
          endpointHealthManager.recordCallResult(endpointId, false, elapsedMs)
          // 降级记录统一由 doDegrade() 承担，此处不预记
          return doDegrade(startTime, DegradationReason.INFRASTRUCTURE_ERROR + ":" + originalCause.getClass().getSimpleName(), request, capabilityId, departmentId, callerRole, callerId, visitId, patientId, sessionId, inputSummary, null, promptVersion, modelRoute.getModelId(), sentinelReason)
    catch (StructuredOutputNotSupportedException e):
      // 模型不支持结构化输出（如 JSON mode 不可用）：回退到 chat() + 传统 JSON 文本解析
      // chat() 返回 CompletableFuture<AiResult<LlmChatResponse>>，同样需两层解包及 CompletionException 捕获
      // 使用独立内部超时（capabilityTimeout 剩余 40%），避免回退路径消耗整体 orTimeout 的剩余窗口
      try:
        chatFuture = llmChatService.chat(chatRequest)
            .orTimeout(chatFallbackTimeout.toMillis(), TimeUnit.MILLISECONDS)
        chatAiResult = chatFuture.join()
        if chatAiResult.isSuccess():
          chatResponse = chatAiResult.getData()
          retryCount = chatResponse.getRetryCount()  // ⚠️ 仅反映 chat 层重试次数（不含 structuredChat 阶段的重试）
          // 与 structuredChat 成功路径的 retryCount（含结构化实现层内部重试）语义不同。
          // 回退路径 retryCount 相当于"最低重试次数"（下限估计），语义定义统一见 §3.5 AiCallRecord.retryCount 字段说明。
          // 若需对齐，在 StructuredOutputNotSupportedException 中嵌入 structuredChat 阶段的 originalRetryCount，
          // 回退路径累加此值；当前设计接受此差异，在指标报表中按 capabilityId 维度聚合时需注意
          tokenUsage = chatResponse.getUsage()
          endpointHealthManager.recordCallResult(endpointId, true, System.currentTimeMillis() - startTime)
          try:
            // parse() 有独立超时控制（通过 @Value("${ai.execution.timeout.parse.default:5s}") 注入
            // structuredOutputParseTimeout，与 capabilityTimeoutConfig 的设计风格一致），使用
            // CompletableFuture.supplyAsync() + .get(structuredOutputParseTimeout) 包裹，
            // 避免 JSON 解析步骤无限挂起。各能力可通过 §9.5 execution.timeout.parse.per-capability
            // 按能力差异化配置解析超时，与 capabilityTimeoutConfig 共用相同的定时刷新机制
            structuredOutputParseTimeout = parseTimeoutConfig.getOrDefault(capabilityId, parseTimeoutDefault)
            parsedResult = CompletableFuture.supplyAsync(() ->
                structuredOutputParser.parse(chatResponse, outputType))
                .get(structuredOutputParseTimeout.toMillis(), TimeUnit.MILLISECONDS)
                // → [shared_success_handler] — 控制流转至共用成功处理段
          catch (java.util.concurrent.TimeoutException parseTe):
            outputSummary = StringUtils.truncate(chatResponse.getContent(), 500)
            return doDegrade(startTime, DegradationReason.PARSE_FAILURE + ":parseTimeout", request, capabilityId, departmentId, callerRole, callerId, visitId, patientId, sessionId, inputSummary, outputSummary, promptVersion, modelRoute.getModelId(), sentinelReason)
          catch (ParseException e2):
            outputSummary = StringUtils.truncate(chatResponse.getContent(), 500)
            return doDegrade(startTime, DegradationReason.PARSE_FAILURE + ":" + e2.getClass().getSimpleName(), request, capabilityId, departmentId, callerRole, callerId, visitId, patientId, sessionId, inputSummary, outputSummary, promptVersion, modelRoute.getModelId(), sentinelReason)
        else:
          // chat() 返回的 AiResult 非成功状态：降级路径
          elapsedMs = System.currentTimeMillis() - startTime
          endpointHealthManager.recordCallResult(endpointId, false, elapsedMs)
          // 降级记录统一由 doDegrade() 承担，此处不预记
          return doDegrade(startTime, DegradationReason.INTERNAL_ERROR, request, capabilityId, departmentId, callerRole, callerId, visitId, patientId, sessionId, inputSummary, null, promptVersion, modelRoute.getModelId(), sentinelReason)
      catch (java.util.concurrent.CompletionException ce):
        // 拆解 CompletionException，优先判断是否为 TimeoutException
        originalCause = ce.getCause()
        if originalCause instanceof java.util.concurrent.TimeoutException:
          // chat() 回退超时（在剩余 40% 窗口内）：直接降级
          elapsedMs = System.currentTimeMillis() - startTime
          endpointHealthManager.recordCallResult(endpointId, false, elapsedMs)
          // 降级记录统一由 doDegrade() 承担，此处不预记
          return doDegrade(startTime, DegradationReason.TIMEOUT + ":chatFallbackTimeout", request, capabilityId, departmentId, callerRole, callerId, visitId, patientId, sessionId, inputSummary, null, promptVersion, modelRoute.getModelId(), sentinelReason)
        else:
          elapsedMs = System.currentTimeMillis() - startTime
          endpointHealthManager.recordCallResult(endpointId, false, elapsedMs)
          // 降级记录统一由 doDegrade() 承担，此处不预记
          return doDegrade(startTime, DegradationReason.INFRASTRUCTURE_ERROR + ":" + originalCause.getClass().getSimpleName(), request, capabilityId, departmentId, callerRole, callerId, visitId, patientId, sessionId, inputSummary, null, promptVersion, modelRoute.getModelId(), sentinelReason)
    catch (LlmInfrastructureException e):
      // 基础设施异常（HTTP 5xx、连接超时、网络抖动）：直接降级，不尝试 chat() 回退
      // 基础设施故障下 chat() 同样会失败，重复调用徒增端到端耗时
      elapsedMs = System.currentTimeMillis() - startTime
      endpointHealthManager.recordCallResult(endpointId, false, elapsedMs)
      originalCause = (e.getCause() != null) ? e.getCause() : e
      originalType = originalCause.getClass().getSimpleName()
      outputSummary = StringUtils.truncate(e.getMessage(), 500)
      // 降级记录统一由 doDegrade() 承担，此处不预记
      return doDegrade(startTime, DegradationReason.INFRASTRUCTURE_ERROR + ":" + originalType, request, capabilityId, departmentId, callerRole, callerId, visitId, patientId, sessionId, inputSummary, outputSummary, promptVersion, modelRoute.getModelId(), sentinelReason)

    // [shared_success_handler]: — 两条成功路径（structuredChat 成功 + chat 回退+parse 成功）
    // 通过控制流 fall-through 到达此共用处理段，完成指标采集与结果返回
    if parsedResult == null:
      // parsedResult 为 null 表示两个成功路径均未赋值（结构化调用与 chat 回退均未抵达成功赋值点）
      // 此状态不应发生，但防御性处理：走降级路径
      log.warn("parsedResult 为 null: capabilityId={}, 两个成功路径均未赋值", capabilityId)
      elapsedMs = System.currentTimeMillis() - startTime
      return doDegrade(startTime, DegradationReason.INTERNAL_ERROR + ":parsedResultIsNull", request, capabilityId, departmentId, callerRole, callerId, visitId, patientId, sessionId, inputSummary, null, promptVersion, modelRoute.getModelId(), sentinelReason)
    outputSummary = StringUtils.truncate(parsedResult.toString(), 500)
    elapsedMs = System.currentTimeMillis() - startTime
    // 注意：visitId/patientId 使用 execute() 入口处提取的参数而非 request.getXxx()，确保薄适配器 DTO（未继承 AiRequestBase）的正确性
    metricsCollector.record(AiCallRecord.success(capabilityId, LocalDateTime.now(), elapsedMs,
        departmentId, modelRoute.getModelId(), retryCount,
        tokenUsage != null ? tokenUsage.getPromptTokens() : null,
        tokenUsage != null ? tokenUsage.getCompletionTokens() : null, inputSummary, outputSummary,
        visitId, patientId, sessionId, callerRole, callerId,
        assignment.getTargetPromptVersion(), sentinelReason))
    slidingWindowMetricsStore.recordSuccess(capabilityId, elapsedMs)
    return AiResult.success(parsedResult)

// 降级路径处理（提取为私有方法，被抽象骨架 execute() 和子类 doExecuteInternal() 共同调用）
// 参数说明：departmentId/callerRole/callerId/visitId/patientId/sessionId/inputSummary/outputSummary/promptVersion
// 由 execute() 入口处从 SecurityContextHolder 和 RequestContextHolder 提取后以独立参数传入，
// 方法体内不再访问 ThreadLocal 或 DTO getter
// sentinelReason 实验分流异常时的哨兵标记（如 "EXPERIMENT_ASSIGN_ERROR"），用于 AiCallRecord 工厂方法将 prompt_version 写为 -1
// ⚠️ LocalRuleFallback 类型安全：localRuleFallback.fallback(request) 调用在编译期为 unchecked 转换（T → R），
// 因泛型擦除，在 Phase 4/Phase 5 DTO 混合场景下可能抛出 ClassCastException。
// 防御措施：(1) AbstractCapabilityExecutor 构造器注入 Class<T> inputType 字段，在调用 fallback 前执行
//   inputType.isInstance(request) 检查；(2) 类型不匹配时日志 ERROR + 返回 AiResult.degraded() 而非传播 ClassCastException
// CallContext 为 Phase 5 第二阶段重构目标，当前保持多参数签名
doDegrade(startTime, degradeReason, request, capabilityId, departmentId, callerRole, callerId, visitId, patientId, sessionId, inputSummary, outputSummary, promptVersion, modelId, sentinelReason):
  elapsedMs = System.currentTimeMillis() - startTime
  effectiveOutputSummary = outputSummary
  if localRuleFallback != null:
    // ⚠️ 泛型类型安全检查：因泛型擦除，localRuleFallback.fallback(request) 在编译期为 unchecked 转换
    // 使用 inputType.isInstance(request) 在运行时验证 request 类型与 LocalRuleFallback 的 T 类型参数一致
    if inputType != null && !inputType.isInstance(request):
      log.error("LocalRuleFallback 类型不匹配: capabilityId={}, expectedType={}, actualType={}",
          capabilityId, inputType.getName(), request.getClass().getName())
      metricsCollector.record(AiCallRecord.failure(capabilityId, LocalDateTime.now(), elapsedMs,
          "LocalRuleFallbackTypeMismatch", "request type " + request.getClass().getName() + " not assignable to " + inputType.getName(),
          departmentId, inputSummary, visitId, patientId, sessionId, callerRole, callerId, null, null))
      slidingWindowMetricsStore.recordFailure(capabilityId)
      return AiResult.degraded(degradeReason)
    result = localRuleFallback.fallback(request)
    // ⚠️ null 保护：fallback() 返回值不应为 null（接口契约约定非 null 返回），
    // 但防御性检查避免 NPE 导致整个方法异常退出（端点和指标记录均无法更新）
    if result == null:
        log.error("LocalRuleFallback.fallback() 返回 null: capabilityId={}, 使用 AiResult.degraded() 兜底", capabilityId)
        metricsCollector.record(AiCallRecord.degraded(capabilityId, LocalDateTime.now(), elapsedMs,
            degradeReason, modelId, departmentId, inputSummary, visitId, patientId, sessionId, callerRole, callerId, effectiveOutputSummary, promptVersion, sentinelReason))
        slidingWindowMetricsStore.recordDegraded(capabilityId, elapsedMs)
        return AiResult.degraded(degradeReason)
    effectiveOutputSummary = effectiveOutputSummary != null
        ? effectiveOutputSummary
        : StringUtils.truncate(result.toString(), 500)
    metricsCollector.record(AiCallRecord.degraded(capabilityId, LocalDateTime.now(), elapsedMs,
        degradeReason, modelId, departmentId, inputSummary, visitId, patientId, sessionId, callerRole, callerId, effectiveOutputSummary, promptVersion, sentinelReason))
    slidingWindowMetricsStore.recordDegraded(capabilityId, elapsedMs)
    return result
  else:
    metricsCollector.record(AiCallRecord.degraded(capabilityId, LocalDateTime.now(), elapsedMs,
        degradeReason, modelId, departmentId, inputSummary, visitId, patientId, sessionId, callerRole, callerId, effectiveOutputSummary, promptVersion, sentinelReason))
    slidingWindowMetricsStore.recordFailure(capabilityId)
    return AiResult.degraded(degradeReason)
```

```
// DiscussionConclusionCapabilityExecutor.doExecuteInternal() — 特化实现
// 与标准管线的差异：
//   1. 前置 LLM 压缩调用（transcriptSummary）：在实验分流/模板渲染/模型路由之前，判断 transcripts
//      是否超长（> 3000 tokens），若是则先向 transcriptSummaryExecutor 提交压缩摘要任务
//   2. 使用独立线程池 transcriptSummaryExecutor 而非 llmCallExecutor，避免压缩调用阻塞
//      其他能力的 LLM 调用（线程池隔离方案，§3.11.7 (b) 已定稿）
//   3. 压缩失败回退：截断原始 transcripts 文本至 2000 tokens
//   4. transcriptSummaryExecutor 调用包裹独立超时 transcriptSummaryTimeout（默认 15s，
//      通过 @Value("${ai.execution.timeout.transcript-summary:15s}") 注入，
//      与主流程 capabilityTimeout 独立）
// 前置压缩阶段结束后，后续管线（实验分流→模板渲染→模型路由→LLM 调用→结构化解析→指标采集）
// 复用父类标准流程，仅有 transcriptSummary 作为模板变量之一注入
DiscussionConclusionCapabilityExecutor.doExecuteInternal(startTime, request, capabilityId,
    departmentId, userId, sessionId, callerRole, callerId, visitId, patientId, inputSummary):
    outputType = getOutputType()
    variables = extractVariables(request)

    // 前置压缩阶段：判断 transcripts 是否超长
    transcripts = request.getTranscripts()  // List<DiscussionTranscript>
    // estimateTokens() 实现策略：
    //   (a) Tokenizer 方案：不使用外部 Tokenizer 库（如 tiktoken），采用基于字符数的保守估算，
    //       原因：压缩调用有截断回退兜底，精确 Token 计数不是关键路径
    //   (b) 中文医疗文本保守换算比例：~1.8 字符/Token（对比通用英文 ~4 字符/Token），
    //       估算公式：estimatedTokens = totalChars / 1.8 + roleMarkerOverhead
    //       roleMarkerOverhead = 每条消息首部固定开销 4 tokens（如 "user: ", "assistant: "）
    //       ⚠️ **英文医学术语场景的字符-Token 换算偏差**：上述 1.8 字符/Token 的比例基于纯中文文本。
    //       当 transcripts 中包含高比例英文医学术语（如药物名 "Acetaminophen"、疾病编码 "ICD-10-CM"、
    //       检查名称 "MRI"、基因名 "BRCA1" 等）时，实际 Token 消耗远低于 1.8 字符/Token 的估算值，
    //       英文 Token 的典型比例约为 3~5 字符/Token（取决于术语长度和 Tokenizer 词表覆盖）。偏差后果：
    //       - **假阳性误触发**：中文-dominated 长文本使用 1.8 比例估算偏保守（估算值高于实际值），
    //         导致 transcripts 实际仅 ~2000 tokens 时被估算为 ~3500 tokens，误触发不必要的前置压缩调用，
    //         引入额外延迟（~15s transcriptSummaryTimeout）和 Token 消耗；
    //       - **漏触发风险**：英文-dominated 文本使用 1.8 比例估算值会显著高于实际值，此场景不会漏触发
    //         （估算值比实际值高，更早达到 3000 阈值），中文文本使用 1.8 比例估算同样不会漏触发。
    //       结论：1.8 字符/Token 的保守换算比例（基于字符数上限估算）在"避免漏触发"方向上是安全的，
    //       但中文-dominated 场景下可能产生约 30%~70% 的假阳性触发率。若生产环境观察到假阳性触发率
    //       显著影响讨论结论能力的 P99 延迟，建议在 doExecuteInternal() 中前置使用轻量 Tokenizer
    //       （如 tiktoken 的 cl100k_base 编码，JVM 生态可通过 jtokkit 库集成）对 transcripts 做一次精确计数，
    //       仅在精确计数超过 3000 时才执行前置压缩，将假阳性触发率降至接近 0%。
    //   (c) 阈值决策依据：>3000 tokens 触发压缩。基于 LlmChatRequest 的 maxTokens 默认值，
    //       3000 约为主窗口（maxTokens=4096）的 73%，预留足够窗口给主流程推理输出。
    //       若 transcripts 超过 3000 tokens，压缩后可以控制在 500 tokens 以内，
    //       显著降低主调用的输入窗口压力。
    // 优先使用精确 Tokenizer（jtokkit）消除 30%~70% 假阳性率；不可用时回退到字符估算
    // tokenizerAvailable 判定：在 DiscussionConclusionCapabilityExecutor 构造时或首次 doExecuteInternal()
    // 调用时通过 try { EncodingRegistry.get(EncodingType.CL100K_BASE) } 尝试初始化 jtokkit 核心 API。
    // 若成功（jtokkit 在 classpath 上）则将 boolean 标志位 tokenizerAvailable 置为 true
    // 并缓存 Encoding 实例（static final AtomicReference<Encoding>，避免每次调用反复创建 registry）；
    // 若抛出 NoClassDefFoundError（jtokkit 不在 classpath 上），tokenizerAvailable=false，
    // 后续调用不再重试初始化。
    if tokenizerAvailable:
        // 精确 Tokenizer 分支：使用 jtokkit（cl100k_base 编码）对 transcripts 做精确计数
        // preciseTokenCount(List<DiscussionTranscript> transcripts) 方法签名：
        //   int preciseTokenCount(List<DiscussionTranscript> transcripts)
        //   逐条遍历 transcript.content，调用 encoding.encode(content).size() 累加精确 Token 数，
        //   再加回 roleMarkerOverhead（每条消息首部固定开销 4 tokens，如 "user: ", "assistant: "）。
        //   返回值: int — transcripts 的精确 Token 计数，不含字符估算偏差。
        //   ⚠️ 线程安全: encoding.encode() 为线程安全操作（jtokkit Encoding 实例不可变），
        //   无需外部同步。多条 transcripts 的编码可在 parallelStream() 中并行执行。
        count = preciseTokenCount(transcripts)
        estimatedTokens = count
        compressionThreshold = 3000
    else:
        // 回退分支：基于字符数的保守估算
        // ⚠️ 假阳性防御：字符估算在中文-dominated 场景下可能产生 30%~70% 假阳性率。
        // 为缓解假阳性影响，回退分支的触发阈值从 3000 直接提升至 4000 tokens，
        // 使字符估算路径的触发条件更保守，减少不必要的前置压缩调用。
        // 同时每触发一次回退分支并超过 4000 阈值时，记录 WARN 日志 + Micrometer Counter 指标
        // （metric 名: aimedical.ai.discussion.tokenizer.fallback，tag: cause=char_estimation）
        // 便于运维评估精确 Tokenizer 引入的收益，辅助决策是否将 jtokkit 加入生产依赖。
        // ⚠️ 采用直接提升阈值而非 2000 硬编码跳跃值的理由：硬编码跳跃值（如 estimatedTokens=2000）
        // 缺乏依据且语义不透明，当触发阈值从 3000 提升到 4000 后，回退分支仅在超过 4000 时才触发，
        // 低于 4000 的估算值直接使用估算结果（因阈值提升已降低了假阳性率），无需额外的跳跃修正。
        estimatedTokens = estimateTokens(transcripts)
        compressionThreshold = 4000
        if estimatedTokens > 4000:
            log.warn("estimateTokens() 回退到字符估算且超过 4000 阈值: capabilityId={}, estimatedTokens={}, 假阳性风险 30%~70%",
                capabilityId, estimatedTokens)
    transcriptSummary = null
    // 前置压缩耗时追踪：用于超时原因细化（refineTimeoutReason 区分 transcriptSummaryCrowding vs primaryLlmTimeout）
    transcriptSummaryStartTime = System.currentTimeMillis()
    if estimatedTokens > compressionThreshold:
        // transcripts 超长，向 transcriptSummaryExecutor 提交压缩任务
        try:
            compressedFuture = CompletableFuture.supplyAsync(() -> {
                // 使用硬编码压缩 Prompt，不经过实验分流和模板渲染
                // 压缩调用使用固定的轻量模型配置（硬编码端点 + 低成本摘要模型），
                // 不经过 ModelRouter 路由选择。理由：(1) 压缩任务对模型能力要求低，
                // 轻量模型可满足摘要需求且成本更低；(2) 压缩调用发生在实验分流和模型路由之前，
                // 此时无法获知目标模型端点和 clientType，无法路由；(3) 压缩失败有截断回退兜底，
                // 模型选择对主流程的影响有限。
                // 轻量模型配置通过以下 @Value 在 DiscussionConclusionCapabilityExecutor 构造器中注入：
                //   @Value("${ai.compression.lightweight-endpoint:compress-default}") String compressionLightweightEndpoint
                //   @Value("${ai.compression.lightweight-client-type:HTTP_API}") ClientType compressionLightweightClientType
                // 对应 YAML 配置 ai.compression.lightweight-endpoint 和 ai.compression.lightweight-client-type（见 §9.5），
                // 未配置端点的回退值见 YAML default 值。
                compressEndpoint = compressionLightweightEndpoint  // 轻量模型端点标识（@Value 注入）
                compressClientType = compressionLightweightClientType // 轻量模型客户端类型（@Value 注入，默认 HTTP_API）
                compressRequest = new LlmChatRequest(
                    messages: List.of(
                        new LlmChatMessage(LlmChatMessageRole.SYSTEM,
                            "请将以下讨论记录压缩为 500 字以内的摘要，保留关键诊断意见和分歧点"),
                        new LlmChatMessage(LlmChatMessageRole.USER,
                            formatTranscripts(transcripts))),
                    options: new LlmChatOptions(), clientType: compressClientType)
                compressResult = llmChatService.chat(compressRequest).join()
                if compressResult.isSuccess():
                    return compressResult.getData().getContent()
                else:
                    throw new RuntimeException("压缩调用返回非成功状态")
            }, transcriptSummaryExecutor)
            transcriptSummary = compressedFuture.get(transcriptSummaryTimeout.toMillis(),
                TimeUnit.MILLISECONDS)
        catch (Exception e):
            // 压缩超时或失败：回退到截断原文
            log.warn("讨论记录压缩失败: capabilityId={}, error={}, 回退到截断原文本",
                capabilityId, e.getMessage())
            transcriptSummary = truncateTranscripts(transcripts, 2000)
            // 压缩失败不降级主流程（回退截断文本确保主 LLM 调用继续执行）
    else:
        transcriptSummary = formatTranscripts(transcripts)
    // 记录前置压缩耗时（含压缩成功、失败回退、以及无压缩直接格式化的完整经过时间）
    // 该值被 inherit 的 volatile elapsedInDoExecuteInternal 补偿计算后供 refineTimeoutReason() 判定
    transcriptSummaryElapsedMs = System.currentTimeMillis() - transcriptSummaryStartTime

    // 将压缩摘要注入模板变量
    variables.put("transcriptSummary", transcriptSummary)

    // 后续复用标准管线流程：调用父类 executeStandardPipeline() 完成实验分流→
    // 模板渲染→模型路由→LLM 调用→结构化解析→指标采集
    // 父类 AbstractCapabilityExecutor 提供 protected final executeStandardPipeline() 方法
    // 封装标准管线步骤（含实验分流/模板渲染/模型路由/LLM 调用/结构化解析/指标采集），
    // 子类在 doExecuteInternal() 的前置阶段完成后调用此方法继续标准流程。
    // 所有参数（variables/promptVersion/sentinelReason 等）已在前置阶段准备就绪，
    // 后续步骤的降级/超时处理由 executeStandardPipeline() 内部统一管理。
    return executeStandardPipeline(startTime, request, capabilityId, departmentId, userId, sessionId,
        callerRole, callerId, visitId, patientId, inputSummary, variables, null, null)
```

**LLM 调用层重试逻辑**：
- `LlmChatService.chat()/structuredChat()` 内部对可重试异常（HTTP 5xx、连接超时、网络抖动）自动重试 1 次（structuredChat 路径由底层实现决定是否重试）
- 幂等性由各能力的 DTO 输入保证（查询类天然幂等，指令类上游去重）
- 重试后仍然失败 → 调用方 `CapabilityExecutor` 捕获异常后进入降级路径

### 4.2 薄适配器 CapabilityExecutor 特化管线

薄适配器型 CapabilityExecutor（6 项 Phase 4 能力）的 `doExecuteInternal()` 特化实现如下，与完整管线的主要差异点用注释标注：

```
ThinAdapterCapabilityExecutor.doExecuteInternal(startTime, request, capabilityId, departmentId, userId, sessionId, callerRole, callerId, visitId, patientId, inputSummary):
    // 差异 1：不执行模板渲染（PromptTemplateManager.render()）和实验分流，
    // Phase 4 服务内部自行处理模型调用
    // 差异 2：使用公共 ForkJoinPool 而非 llmCallExecutor 执行 Phase 4 委托，
    // 避免嵌套提交到同一线程池产生排队死锁
    // 差异 3：独立 thinAdapterTimeout 超时控制（默认 30s，可通过 YAML 按能力配置，参见 §9.5 thin-adapter-default: 30s）
    // 差异 4：Phase 4 业务异常（BusinessException）包装为 AiResult.failure() 返回
    // 差异 5：基础设施异常（Exception）记录 DegradationReason.INFRASTRUCTURE_ERROR 进入降级
    // 差异 6：retryCount=0 限制（薄适配器不重试，重试由 Phase 4 服务内部控制）
    // 差异 7：thinAdapterTimeout 支持 per-capability 覆盖——优先从 thinAdapterPerCapabilityConfig 中按 capabilityId 查找对应值，未配置时使用全局 thinAdapterTimeout 默认值

    try:
        // 按能力标识解析 per-capability 超时覆盖值
        effectiveThinAdapterTimeout = thinAdapterPerCapabilityConfig.getOrDefault(capabilityId, thinAdapterTimeout)

        // 使用公共 ForkJoinPool（默认线程池），避免嵌套提交到 llmCallExecutor
        delegateFuture = CompletableFuture.supplyAsync(
            () -> phase4ServiceDelegate.execute(request))
        result = delegateFuture.get(effectiveThinAdapterTimeout.toMillis(), TimeUnit.MILLISECONDS)
    catch (TimeoutException e):
        log.warn("ThinAdapter 委托超时: capabilityId={}, timeout={}ms", capabilityId, effectiveThinAdapterTimeout.toMillis())
        // 薄适配器超时降级路径中 modelId=null（薄适配器不经过模型路由，modelId 不可用）
        // sentinelReason=null（薄适配器不经过实验分流，无实验分组上下文）
        return doDegrade(startTime, DegradationReason.TIMEOUT + ":ThinAdapterTimeout", request, capabilityId, departmentId, callerRole, callerId, visitId, patientId, sessionId, inputSummary, null, null, null, null)
    } catch (Exception e):
        // 拆解 ExecutionException（CompletableFuture.get() 包装的原始异常）
        originalCause = (e instanceof java.util.concurrent.ExecutionException) ? e.getCause() : e
        originalType = originalCause.getClass().getSimpleName()
        // 按 §3.1 Phase 4 模块异常契约采用两阶段异常检测区分业务异常与基础设施异常：
        //   阶段 1（主判定）：instanceof Phase4BusinessException — 检测通过则直接按业务异常处理
        //   阶段 2（过渡期回退）：主判定失败时调用 isKnownPhase4BusinessException()，
        //     通过异常类全名的包路径前缀匹配回退识别 Phase 4 业务异常
        //   - 两阶段均未识别时按基础设施异常处理（走降级路径）
        phase4BusinessExceptionDetected = (originalCause instanceof Phase4BusinessException)
        if !phase4BusinessExceptionDetected:
            phase4BusinessExceptionDetected = isKnownPhase4BusinessException(originalCause)
        if phase4BusinessExceptionDetected:
            // Phase 4 业务异常（如参数校验失败、数据不存在）：直接返回失败，不走降级路径
            // 标准化错误码：优先使用 Phase 4 业务异常中自带的错误码，若不存在则回退到异常类名
            elapsedMs = System.currentTimeMillis() - startTime
            errorCode = "PHASE4_" + originalType
            metricsCollector.record(AiCallRecord.failure(capabilityId, LocalDateTime.now(), elapsedMs,
                errorCode, originalCause.getMessage(), departmentId, inputSummary, visitId, patientId, sessionId, callerRole, callerId, null, null))
            slidingWindowMetricsStore.recordFailure(capabilityId)
            return AiResult.failure(errorCode, originalCause.getMessage())
        else:
            // 基础设施异常（网络超时、服务不可用等）：走降级路径
            // sentinelReason=null（薄适配器不经过实验分流）
            return doDegrade(startTime, DegradationReason.INFRASTRUCTURE_ERROR + ":" + originalType, request, capabilityId, departmentId, callerRole, callerId, visitId, patientId, sessionId, inputSummary, null, null, null, null)

     elapsedMs = System.currentTimeMillis() - startTime
    outputSummary = StringUtils.truncate(result.toString(), 500)
    // ⚠️ 薄适配器元数据提取：通过 instanceof 检测 Phase 4 响应 DTO 是否实现可选元数据接口
    // 若未实现（底座切流初期常态），modelId=null、promptVersion=null、retryCount=0
    // 若已实现，则从 Phase4ServiceMetaCapable.getServiceMeta() 获取内嵌元数据值对象
    // ⚠️ 线程安全：元数据绑定在响应 DTO 实例上（请求级），消除了原服务实例级接口跨请求污染风险
    modelId = null
    promptVersion = null
    retryCount = 0
    if result instanceof Phase4ServiceMetaCapable:
        meta = ((Phase4ServiceMetaCapable) result).getServiceMeta()
        modelId = meta.getModelId()             // 可能为 null
        promptVersion = meta.getPromptVersion() // 可能为 null
        retryCount = meta.getRetryCount()       // 默认为 0
    metricsCollector.record(AiCallRecord.success(capabilityId, LocalDateTime.now(), elapsedMs,
        departmentId, modelId, retryCount, null, null, inputSummary, outputSummary,
        visitId, patientId, sessionId, callerRole, callerId, promptVersion, null))
    slidingWindowMetricsStore.recordSuccess(capabilityId, elapsedMs)
    return AiResult.success(result)
```

### 4.3 模型路由契约

```
ModelRouter.route(capabilityId, assignment):
  1. 若 assignment 指定了覆盖模型 → 返回该模型对应的 ModelRoute
  2. 否则从路由映射表中查找 capabilityId 对应的默认 ModelRoute
  3. 若存在多个 ModelRoute（多模型负载均衡）：按权重随机选择
  4. 无可用路由 → 返回 null → 触发降级
```

### 4.4 A/B 实验分流契约

```
ExperimentManager.assign(capabilityId, userId, sessionId):
    1. 检索该能力标识下当前状态为 ACTIVE 的实验列表（显式过滤 status = PAUSED 的实验，确保 PAUSED 语义"暂停分流，所有流量回退到默认模型"由契约显式保证，不依赖"检索不到"的隐含假设）
    2. 无 ACTIVE 实验 → 返回 ExperimentAssignment.createDefault()（groupId="default"，targetPromptVersion=null）
    3. 有实验 → 按 userId/sessionId 哈希值 % 1000 映射到 [0, 1000) 区间
    4. 遍历分组列表，找到哈希值落在其流量百分比区间的分组
    5. 返回 ExperimentAssignment(实验标识, 分组标识, 目标模型, 目标Prompt版本)
    6. CapabilityExecutor 管线中，assignment.getTargetPromptVersion() 传入 PromptTemplateManager.render() 的 promptVersion 参数：非 null 时使用指定版本模板，null 回退到当前 ACTIVE 模板

**异常场景与正常场景的区分规则**：
- `ExperimentManager.assign()` 内部抛出异常时，由调用方（§4.1 `doExecuteInternal()` catch 块）捕获后返回 `ExperimentAssignment.createErrorFallback()`（groupId="experiment-error"，targetPromptVersion=null）
- 下游实验效果分析报表通过 `AiCallLogEntity.group_id` 字段区分两场景：
  - `"default"` → 正常基线数据（无实验命中）
  - `"experiment-error"` → 异常降级污染数据（实验分流异常）
  - 其他合法 `group_id` 值 → 实验分组数据（正常命中实验）
- `groupId="experiment-error"` 作为哨兵值，约定不落入任何合法实验分组的 `group_id` 命名范围（合法的 `group_id` 使用大写字母/数字/下划线，`experiment-error` 含连字符，天然不会冲突）
- `AiCallRecord.promptVersion` 的空值守卫标记方案见 §3.4「AiCallRecord.promptVersion 空值守卫标记方案」
```

### 4.5 Prompt 模板渲染契约

```
PromptTemplateManager.render(capabilityId, departmentId, variables, promptVersion):
    1. promptVersion 不为 null:
       a. 按 capabilityId + departmentId + promptVersion 查询模板（不限定 status）
       b. 若存在且 status = ACTIVE → 使用该版本模板
       c. 若存在但 status = DEPRECATED → 日志 WARN（指定版本已废弃），回退到 ACTIVE 模板检索策略
       d. 若不存在 → 回退到 ACTIVE 模板检索策略
    2. promptVersion 为 null → 直接走 ACTIVE 模板检索策略
    3. ACTIVE 模板检索策略: 按 capabilityId + departmentId + status=ACTIVE 查找
    4. departmentId 有值且存在科室级模板 → 优先使用科室级模板
    5. departmentId 无值或无科室级模板 → 使用通用模板
    6. 将 variables 中的键值对替换模板中的 {{key}} 占位符
    7. 缺失变量保留原始占位符文本 + 日志 WARN
    8. 返回渲染后 Prompt 文本
```

### 4.6 结构化输出解析契约

```
StructuredOutputParser.parse(llmChatResponse, targetClass):
  1. 从 llmChatResponse.content 提取 JSON 片段
  2. 若 LLM 输出为 Markdown 代码块包裹的 JSON → 提取代码块内容
  3. 若 LLM 输出为纯 JSON → 直接使用
  4. Jackson 反序列化到 targetClass
  5. 反序列化失败 → 抛出 ParseException → 由 CapabilityExecutor 捕获并触发降级
```

### 4.7 性能指标采集契约

```
AiMetricsCollector.record(AiCallRecord):
  1. @Async 异步写入 AiCallLogRepository（JPA INSERT）
  2. 同步推送 Micrometer 指标:
     - aimedical.ai.request.duration (Timer, tags: ability, status, model)
     - aimedical.ai.request.count (Counter, tags: ability, status)
     - aimedical.ai.degradation.count (Counter, tags: ability, reason)
     - aimedical.ai.token.usage (DistributionSummary, tags: ability, model, type)
```

---

## 5. 错误处理策略

### 5.1 错误分类（AI 底座层面新增）

| 错误类别 | 代表场景 | 处理方式（对应 §4.1 实现位置） | 响应形态 |
|---------|---------|---------|---------|
| 模板缺失/渲染失败 | 模板不存在、变量缺失致渲染异常 | `doExecuteInternal()` 中 try-catch 包裹 `promptTemplateManager.render()`：日志 WARN + `promptTemplateManager.getFallbackPrompt(capabilityId)` 获取能力内硬编码兜底 Prompt | 继续调用 LLM |
| 实验分流异常 | 实验配置非法 | `doExecuteInternal()` 中 try-catch 包裹 `experimentManager.assign()`：日志 WARN + 使用 `ExperimentAssignment.createErrorFallback()`（`groupId="experiment-error"`，哨兵分组，与正常基线数据可区分） | 继续调用 LLM |
| 模型路由失败 | 无可用模型路由 | 直接触发降级，降级原因 `DegradationReason.NO_AVAILABLE_ROUTE` | `AiResult.degraded()` |
| LLM 调用超时 | 超过能力硬超时阈值 | `LlmChatService.chat()/structuredChat()` 内部重试 1 次→仍失败则降级，降级原因 `DegradationReason.TIMEOUT` | `AiResult.degraded()` |
| 整体管线超时 | 非 LLM 步骤（DB 查询、Jackson 转换等）挂起 | `execute()` 中 `CompletableFuture.orTimeout()` 兜底，超时后降级，降级原因 `DegradationReason.TIMEOUT`。各能力独立配置超时阈值（默认 60 秒） | `AiResult.degraded()` |
| 薄适配器委托超时 | Phase 4 服务内部重试/超时导致端到端耗时不可控 | `doExecuteInternal()`（薄适配器）中 `delegateFuture.get(thinAdapterTimeoutMs, TimeUnit.MILLISECONDS)` 独立超时，超时后降级，降级原因 `DegradationReason.TIMEOUT + ":ThinAdapterTimeout"`。默认 30 秒 | `AiResult.degraded()` |
| LLM 调用不可用 | HTTP 5xx / 连接拒绝 | 重试 1 次→仍失败则降级，降级原因 `DegradationReason.INFRASTRUCTURE_ERROR` | `AiResult.degraded()` |
| 结构化输出解析失败 | LLM 返回非 JSON | `doExecuteInternal()` 中 try-catch 包裹 `structuredOutputParser.parse()`：提取 JSON 片段重试→仍失败降级，降级原因 `DegradationReason.PARSE_FAILURE` | `AiResult.degraded()` |
| 结构化输出格式不支持 | 模型不支持 JSON mode / tool_use | `doExecuteInternal()` 中 catch `StructuredOutputNotSupportedException`：回退到 `chat()` + `StructuredOutputParser.parse()`，不直接降级 | 继续调用 LLM（chat 回退） |
| 结构化输出基础设施异常 | structuredChat 中 HTTP 5xx / 超时 | `doExecuteInternal()` 中 catch `LlmInfrastructureException`：直接降级，降级原因 `DegradationReason.INFRASTRUCTURE_ERROR`，不尝试 chat() 回退 | `AiResult.degraded()` |
| 熔断触发 | 失败率超阈值 | `execute()` 中降级预检步骤（`supplyAsync` 之前）检查 `CircuitBreakerDegradationStrategy`：OPEN 状态直接在容器线程返回降级，不入线程池排队，降级原因 `DegradationReason.CIRCUIT_BREAKER_OPEN` | `AiResult.degraded()` |
| 端点不可用 | 模型端点连续失败被健康管理器标记为 UNAVAILABLE | `doExecuteInternal()` 中 `ModelEndpointHealthManager.getState()` 检查：UNAVAILABLE 且未到探测窗口则降级，降级原因 `DegradationReason.ENDPOINT_UNAVAILABLE` | `AiResult.degraded()` |

### 5.2 降级优先级

1. **熔断** > **超时** > **LLM 不可用** > **解析失败**
2. 有 `LocalRuleFallback` 的能力降级时返回本地规则结果；其余返回 `AiResult.degraded()`

---

## 6. 并发设计

### 6.1 线程模型

- `AiOrchestrator`：**非无状态**。线程安全性依赖于：
  - `Map<String, CapabilityExecutor>` 初始化后不变（无竞争读）
  - `SlidingWindowMetricsStore` 内部使用 `ConcurrentHashMap` + 队列写锁保证并发安全
  - 核心编排逻辑（按 capabilityId 查找 executor → 调用）无共享可变状态，线程安全
- `AiOrchestrator` 本身不引入 `synchronized` 大锁；并发瓶颈由各子组件独立承担
- `CapabilityExecutor`：**每个能力标识对应一个独立的 Bean 实例，实例内部持有该能力独享的 `List<DegradationStrategy>` 和可选的 `LocalRuleFallback` 引用**。其线程安全契约如下：
  - `execute()` 方法未被 `synchronized` 保护，同一能力的高并发请求以无锁方式并行执行
  - 管线步骤中访问的共享状态均由下游组件自行保证线程安全（`SlidingWindowMetricsStore` 的 `ConcurrentHashMap` + 写锁、`ModelEndpointHealthManager` 的 `AtomicReference` 状态转换）
  - `DegradationStrategy` 实现均为无状态（`TimeoutDegradationStrategy` 的配置来自构造注入）或内部线程安全（`CircuitBreakerDegradationStrategy` 的 `AtomicReference<CircuitState>` + CAS）
  - `localRuleFallback` 引用在初始化后不变，无竞争
  - LLM 调用通过 `CompletableFuture.supplyAsync()` 委派到共享的 `LlmCallExecutor` 线程池执行，不阻塞 CPU-intensive 的管线步骤
- `LlmChatService`：无状态，线程安全。HTTP 客户端基于连接池。`chat()` 与 `structuredChat()` 均返回 `CompletableFuture`，由实现层自行决定同步/异步执行
- `ModelRouter`：路由表统一存储为 `AtomicReference<Map<String, ModelRoute>>`，启动时从配置/DB 加载后封装为不可变 Map 实例，通过 CAS 一次性设置引用。读取路径通过 `AtomicReference.get()` 获取当前路由表快照，确保读取始终看到一致状态。支持运行时刷新，**刷新触发机制**：采用三种触发方式——（1）`@Scheduled(fixedDelay = 60000)` 定时轮询配置源，检测路由表版本变更；（2）管理端 API 调用显式触发刷新；（3）Spring `ApplicationEvent` 事件驱动（配置变更时由管理端发布 `RouteConfigChangedEvent`，`ModelRouter` 作为 `@EventListener` 响应刷新）。三种触发方式均复用同一刷新方法，通过 `synchronized` 互斥防止并发刷新。**全量替换模式**：刷新时先在本地构造完整的不可变 Map 副本，然后通过 `AtomicReference` CAS 一次性替换引用
- `PromptTemplateManager`：模板缓存使用 `ConcurrentHashMap`，管理端变更通过 Spring ApplicationEvent 通知缓存失效
- `ExperimentManager`：实验配置缓存同理
- `AiMetricsCollector`：异步写入，使用 Spring `@Async` + 专用指标采集线程池，与 `LlmCallExecutor` 线程池完全隔离。拒绝策略使用 `DiscardPolicy` + 日志 WARN，避免指标写入阻塞 LLM 调用路径

### 6.2 熔断器线程安全

- `CircuitBreakerDegradationStrategy` 内部每个能力标识维护独立的熔断状态实例
- 状态转换使用 `AtomicReference<CircuitState>` + CAS 保证原子性
- 滑动窗口使用 `ConcurrentHashMap<String, Deque<Long>>`
- 状态读取（`shouldDegrade()`）与状态转换（`recordSuccess/Failure()`）之间不设全局锁，转换失败（CAS 冲突）时重试

### 6.3 数据库写入竞争

- `AiCallLogRepository` 的写入为追加操作（INSERT），无竞争
- `PromptTemplateRepository` 和 `ExperimentRepository` 读写分离：读走缓存，写走管理端 API + 事件通知缓存失效

### 6.4 异步上下文传播机制

`AbstractCapabilityExecutor.execute()` 通过 `CompletableFuture.supplyAsync()` 将管线执行委派到 `LlmCallExecutor` 线程池，导致 Spring `ThreadLocal` 上下文（`SecurityContextHolder`、`RequestContextHolder`）在线程池线程中丢失。解决方案采用**入口处提取 + 闭包捕获**模式：

**传播策略**：
- `SecurityContext`（userId、callerRole、callerId）：在 `execute()` 入口处（`supplyAsync()` 调用之前）通过 `SecurityContextHolder.getContext()` 提取当前认证信息，以局部变量形式捕获到 lambda 闭包中。userId 回退 `"SYSTEM"` 的 null-safe 逻辑同样在入口处执行
- `RequestContextHolder`（thin adapter departmentId）：`doExtractDepartmentId()` 在 `execute()` 入口处调用（`supplyAsync()` 之前），此时所在线程为 Tomcat 容器线程，`RequestContextHolder` 上下文可用
- `AiRequestBase` 基类字段（`departmentId`、`sessionId`、`visitId`、`patientId`）：这些字段存储于 DTO 实例而非 ThreadLocal，在 lambda 内部通过 `request` 对象访问不受线程切换影响，无需特殊处理

**执行时序**：
```
AbstractCapabilityExecutor.execute(request, capabilityId):
  startTime = System.currentTimeMillis()           // 线程池外计时，包含排队等待时间
  auth = SecurityContextHolder.getContext().getAuthentication()
  userId = auth != null ? auth.getName() : "SYSTEM"
  departmentId = doExtractDepartmentId(request)     // 在容器线程上执行，ThreadLocal 可用
  sessionId = request.getSessionId()
  callerRole = extractCallerRole()                  // 从 SecurityContext/RequestContext 提取
  callerId = extractCallerId()

  return CompletableFuture.supplyAsync(() -> {
    // lambda 内部仅使用已捕获的局部变量，不再访问 ThreadLocal
    context = slidingWindowMetricsStore.buildDegradationContext(...)
    context.setDepartmentId(departmentId)
    ...
    return doExecuteInternal(startTime, request, capabilityId, departmentId, userId, sessionId, callerRole, callerId, visitId, patientId)
  }, llmCallExecutor)
```

**`doExecuteInternal()` 与 `doDegrade()` 的上下文契约**：两方法通过参数接收上下文值，不再在方法体内部访问 `SecurityContextHolder` 或 `RequestContextHolder`。v12 将 `visitId`/`patientId`/`sessionId` 也纳入参数传递，消除对 `request.getVisitId()`/`getPatientId()`/`getSessionId()` 的依赖，确保 Phase 4 薄适配器（DTO 尚未继承 `AiRequestBase`）的降级路径正确获取就诊上下文。

**字段填充路径对应关系**：

| 上下文来源 | 提取时机 | 使用位置 |
|-----------|---------|---------|
| userId (SecurityContext) | execute() 入口，supplyAsync 之前 | ExperimentManager.assign() |
| sessionId (DTO / HTTP Header / RequestContext) | execute() 入口，doExtractSessionId() | ExperimentManager.assign(), AiCallRecord, doDegrade() |
| departmentId (DTO / RequestContext) | execute() 入口，doExtractDepartmentId() | DegradationContext.setDepartmentId(), Prompt 模板渲染, AiCallRecord |
| callerRole/callerId (SecurityContext) | execute() 入口 | AiCallRecord 工厂方法 |
| visitId (DTO / HTTP Header / RequestContext) | execute() 入口，doExtractVisitId() | AiCallRecord 工厂方法, doDegrade() |
| patientId (DTO / HTTP Header / RequestContext) | execute() 入口，doExtractPatientId() | AiCallRecord 工厂方法, doDegrade() |

**设计决策理由**：方案 (c)（入口处提取）优于方案 (b)（异步边界下移到 LLM 调用层），因为：(1) 一次提取覆盖所有上下文依赖，减少后续维护认知负担；(2) 降级预检循环也在线程池外执行，确保降级判定使用的所有上下文完整；(3) `startTime` 在入口处记录包含线程池排队等待时间，更准确反映端到端耗时。

---

## 7. 设计决策

| 决策 | 选项 | 选择 | 理由 |
|------|------|------|------|
| 编排层形态 | 全新 AiService 实现 vs 在 FallbackAiService 内扩展 | 全新 `AiOrchestrator` 实现 `AiService`，替代 `MockAiService` | FallbackAiService 职责是降级装饰，不应混入编排逻辑 |
| 管线所有权 | AiOrchestrator 持有公共管线 vs CapabilityExecutor 持有完整管线 | `CapabilityExecutor` 持有完整管线 | 13 项能力的管线步骤高度一致但变量提取、策略白名单存在差异；CapabilityExecutor 持有管线使每项能力可独立定制局部步骤而不影响其他能力，同时保持管线骨架统一 |
| 降级策略注入目标 | 注入到 AiOrchestrator 全局 vs 注入到 CapabilityExecutor 按能力隔离 | 按能力注入到 `CapabilityExecutor` | 各能力的降级策略白名单不同（如 TRIAGE 使用 timeout+熔断，RX_AUDIT 使用 timeout+noop），全局注入导致策略对所有能力无条件生效；按能力注入使策略配置与能力绑定，且各实现不共享策略列表实例 |
| 能力映射机制 | 手动 Map 注册 vs @PostConstruct 扫描 vs 注解扫描 | `@PostConstruct` 扫描 `List<CapabilityExecutor>` 自动构建 `Map<String, CapabilityExecutor>` | 零配置——新增能力实现后自动注册，无需修改 AiOrchestrator 代码；@PostConstruct 时机构造期 fail-fast，未注册能力标识在启动时即暴露而非运行时静默失败 |
| 配置转发机制 | EnvironmentPostProcessor vs @ConditionalOnExpression vs 自定义 PropertySource | `EnvironmentPostProcessor` | Spring 启动早期执行，在 @ConditionalOnProperty 评估之前完成属性转发；单一职责、配置与代码分离 |
| 变量提取方式 | ObjectMapper.convertValue vs 自定义方法 vs 注解驱动 | 双模式：默认 `ObjectMapper.convertValue` + 可选的 `extractVariables()` 方法重写 | ObjectMapper 无需额外注解或配置即可工作；自定义方法为复杂场景提供扩展点；两种模式通过在 CapabilityExecutor 中重写方法切换，无需修改框架代码 |
| LLM 客户端抽象 | 直接使用 Spring AI vs 自定义抽象层 | 自定义 `LlmChatService` + `LlmChatStreamService` interface，Spring AI 作为可选实现 | 支持 HTTP API 调用和 Spring AI 两种方式共存；流式与同步接口分离隔离 Reactor 依赖 |
| Prompt 模板存储 | 配置文件 vs 数据库 | 数据库（JPA Entity）+ 内存缓存 | 管理端运行时修改需求要求；缓存解决查询性能 |
| A/B 实验分桶 | 哈希分桶 vs 多臂老虎机 | 哈希分桶（Phase 5） | 简单满足确定性分桶需求；高级策略留 Phase 6 |
| 指标采集方式 | 仅 Micrometer vs 仅数据库 vs 双写 | 双写：数据库 + Micrometer | 数据库满足可追溯性；Micrometer 满足实时指标需求 |
| 降级策略扩展 | 保留 NoOp + 新增超时/熔断 vs 全部替换 | 新增超时/熔断，按能力策略白名单配置 | v2：策略调用移入 CapabilityExecutor 管线，数据源由 `SlidingWindowMetricsStore` 提供，解决 v1 死代码问题；v4：按能力注入到各 CapabilityExecutor 实现 |
| 结构化输出方式 | 强制 JSON vs Spring AI vs 自定义 | 自定义 `StructuredOutputParser` interface | 可适配 JSON/Markdown/自由文本多种输出格式 |
| 能力执行器粒度 | 每能力一个 vs 全能力同管 | 每能力一个 `CapabilityExecutor<T,R>` 泛型实现 | 13 项能力输入/输出类型各不相同；泛型保证类型安全 |
| DegradationContext 扩展方式 | 新增字段 vs 新增子类 | 新增字段 + Builder 模式 | Phase 0 冻结方法签名；Builder 确保新字段显式赋值；声明 serialVersionUID 保序列化兼容 |
| Bean 装配策略 | `@ConditionalOnProperty` 互斥 vs 其他 | `FallbackAiService` 使用 `@Primary` + `ObjectProvider<AiService>` | 解决 v1 中的 NoUniqueBeanDefinitionException；互斥策略通过 `ai.platform.enabled` 配置实现 |
| 降级策略数据源 | FallbackAiService 创建空 DegradationContext vs SlidingWindowMetricsStore | `SlidingWindowMetricsStore` 提供实时指标数据 | v1 中策略实现无法获取调用数据形成死代码；滑动窗口存储使超时/熔断可基于真实数据判定 |
| [LLM 调用线程池] 异步队列溢出 | AbortPolicy vs CallerRunsPolicy | `CallerRunsPolicy` | 避免 `TaskRejectedException` 传播到调用链导致主流程失败。**⚠️ 适用组件说明**：此决策仅适用于 `LlmCallExecutor` 线程池（归属 `AiPlatformConfig` LLM 调用线程池，核心线程=可用模型端点数，队列容量=100，拒绝策略=CallerRunsPolicy）。指标采集线程池（`MetricsAsyncExecutor`）使用 `DiscardPolicy` + 日志 WARN 策略（见 §3.5/§6.1），两线程池拒绝策略不矛盾——CallerRunsPolicy 用于 LLM 调用线程池提供自然背压，DiscardPolicy 用于指标采集线程池避免阻塞主路径 |
| Micrometer 依赖 | 依赖 Spring Boot Actuator 自动配置 vs 显式声明 | 在 `ai-impl/pom.xml` 中显式声明 `spring-boot-starter-actuator` | 确保不可用依赖的底座功能不会因缺失 Actuator 而静默失败 |
| 降级路径指标记录方式 | `recordSuccess(degraded=true)` vs `recordDegraded()` 独立方法 | 独立 `recordDegraded()` 方法 | 使 `getFailureRate()` 只统计"真正"的调用失败，不被降级兜底的成功稀释；熔断器判定基于 `failureCount / (successCount + failureCount)` 而非 `failureCount / (successCount + degradedCount + failureCount)` |
| LlmChatService 异步契约 | LlmChatService.chat() 返回 CompletableFuture vs LlmClient.invoke() 同步 | 返回 `CompletableFuture`，CapabilityExecutor 通过 `supplyAsync()` 包装统一异步边界 | LLM HTTP 调用本质上是阻塞 RPC，但接口返回 `CompletableFuture` 便于调用方灵活选择同步/异步；异步边界在 CapabilityExecutor 层面，使用共享线程池隔离调用线程与容器线程 |
| ModelRouter 运行时刷新策略 | 增量更新 vs 全量替换 | `AtomicReference<Map>` 全量替换 | 避免增量更新读取端看到部分更新的不一致状态；全量替换牺牲短暂内存开销换取读取路径的免锁安全 |
| AiOrchestrator.handle() 异常处理 | 异常传播到 CompletableFuture vs 兜底捕获 | try-catch 捕获意外异常，返回 `AiResult.failure()` | 防止不可预知异常（NPE、序列化失败）传播为 `CompletionException` 导致 HTTP 500；预期异常已在 CapabilityExecutor.execute() 内部以降级路径处理。使用 `failure()` 而非 `error()` 因 `AiResult` 仅定义了 `success(T)`、`failure(String)`、`degraded(String)` 三个工厂方法，`error()` 不存在 |
| DegradationStrategy.getOrder() 扩展方式 | 抽象方法 vs Java 8 default method | `default int getOrder() { return 0; }` | 避免破坏现有 Phase 0 实现的编译兼容性（NoOp/Timeout/CircuitBreaker 无须修改即可编译通过）。**影响评估**：已存在的自定义 `DegradationStrategy` 实现（如有）将自动继承默认排序值 0，排序行为由 CapabilityExecutor 管线在 `execute()` 中统一执行。默认值 0 保证新旧策略按声明顺序排序，不影响现有业务逻辑。接口变更需在发版说明中标注为"新增 default method，二进制兼容" |
| YAML 策略配置到 Bean 引用的转换 | @Bean 方法内调用 getBeansOfType() vs @PostConstruct + ApplicationContextAware | `@PostConstruct` 阶段（上下文完全初始化后）注入 `Map<String, DegradationStrategy>`，按 YAML 名称查找并构建 `Map<String, List<DegradationStrategy>>` | 避免 Spring `@Bean` 初始化阶段 ApplicationContext 尚未完全就绪的时序风险。`@PostConstruct` + `ApplicationContextAware` 确保所有 `@Component` 策略实例均已完成注册 |
| Experiment PAUSED 状态语义 | 维护分配记录区分新旧流量 vs 简化为全量回退 | 简化为"暂停分流，所有流量回退到默认模型" | `HashBucketExperimentManager` 为无状态哈希分桶，无法区分新旧流量；维护分配记录表引入写路径复杂度和持久化成本。简化语义后实验暂停即停止分流，已进入实验的会话在 `PAUSED→ACTIVE` 转换前仍使用默认模型，切面更清晰 |
| 降级预检与指标采集复用度 | 每个 CapabilityExecutor 独立实现 vs 抽取 AbstractCapabilityExecutor 骨架 | 抽取 `AbstractCapabilityExecutor` 抽象骨架类，提供模板方法 | 13 个实现类中降级预检循环和指标采集步骤完全一致，人工同步 13 份相同代码的维护成本高；抽取为 abstract class 后子类仅需特化 `doExecuteInternal()`，降低新增能力的实现门槛；使用模板方法模式（`execute()` 声明为 final 防止子类绕过降级预检）替代组合模式，因子类仍需共享 doDegrade 方法实现 |
| 指标采集队列拒绝策略 | CallerRunsPolicy vs DiscardPolicy | `DiscardPolicy` + 日志 WARN | `CallerRunsPolicy` 在指标采集队列满时回退到调用者线程执行，而调用者为 `LlmCallExecutor` 线程池中的线程，执行写入将阻塞 LLM 调用路径，导致线程池饥饿；`DiscardPolicy` 牺牲指标记录的完整性换取 LLM 调用主流程的可用性，指标丢失在系统峰值时可接受 |
| Request DTO 线程安全 | 隐式约定不可变 vs 显式契约 + 防御性拷贝 | 显式只读契约 + 不可变 DTO 建议 + 防御性拷贝兜底 | execute() 中 request 对象被传递到多个下游组件（模板变量提取、Phase 4 委托、降级兜底），隐式依赖"不被修改"风险高；不可变 DTO（final 字段 + 无 setter）提供最强保证；防御性拷贝作为现有不可变改造前的过渡措施 |
| 异步上下文传播 | 入口处提取（方案c）vs 异步边界下移（方案b） | 入口处提取，闭包捕获传递 | 一次提取覆盖所有 ThreadLocal 上下文（SecurityContext、RequestContext），减少后续维护认知负担；startTime 在入口处记录包含排队等待时间，端到端耗时更准确 |
| Phase0/Phase1ABD 设计风格一致性 | 独立风格 vs 参照已有 | 参照 Phase0/Phase1ABD 的章节结构、抽象描述粒度、类型形态选择逻辑与设计决策记录格式 | 确保跨阶段设计规范统一，降低项目理解和维护成本；涉及：§1.2 架构图示风格、§3 核心抽象字段表、§4 行为契约伪代码、§7 设计决策表格式 |
| ModelRoute 认证与超时配置 | 在 ModelRoute 中标注字段 vs 通过 Vault/配置中心按 endpointId 查询 | 密钥通过 Vault/配置中心按 endpointId 查询，超时配置在 ModelRoute 中定义 | API 密钥属于敏感凭据，不应随路由配置明文存储；Vault/配置中心提供密钥轮转和审计能力；超时（connectionTimeout/readTimeout）是端点维度的运行时行为配置，归属 ModelRoute 合理 |
| LlmChatService 多实现分发方式 | `@Qualifier` 注入 vs `DelegatingLlmChatService` 分发层 | `DelegatingLlmChatService` 分发层 | CapabilityExecutor 在运行时才能从 `ModelRoute.clientType` 获知本次调用的客户端类型，编译期 `@Qualifier` 无法固定注入；分发层使 CapabilityExecutor 保持对 `LlmChatService` 接口的单一依赖视图，不感知底层实现选择逻辑；HttpApiLlmChatService 和 SpringAiLlmChatService 作为具名 Bean 注册，不直接对外暴露 |
| structuredChat 异常分类 | 统一 catch(Exception) vs 按异常类型拆分两个 catch 分支 | 按异常类型拆分：`StructuredOutputNotSupportedException` 回退到 chat()+parse()，`LlmInfrastructureException` 直接降级 | 统一捕获后回退到 chat() 在不支持结构化格式的场景下合理，但基础设施故障（HTTP 5xx、超时）下 chat() 同样会失败，重复调用徒增端到端耗时。拆分后两类异常路径互不干扰 |
| HTTP Header 提取方法归属 | AbstractCapabilityExecutor protected vs 独立 public static 工具类 | 独立 `RequestContextUtils` public static 工具类 | AiOrchestrator.handle() catch 块同样需要提取 Header，但 AiOrchestrator 不继承 AbstractCapabilityExecutor，protected 方法无法访问；静态工具类同时满足两个调用方且便于单元测试 |
| 非 HTTP 场景 Header 提取 | 无定义 vs 显式调用方 Header 契约 + 非 HTTP 回退路径 | 调用方 Header 契约 + 三层非 HTTP 回退路径（DTO 继承、MQ/调用方填充、薄适配器 DTO 回退） | 原设计隐含 HTTP 请求前提，非 HTTP 场景（MQ、定时任务）下 `RequestContextHolder` 返回 null 导致就诊上下文字段全部空白。显式契约和回退路径使设计对 HTTP 和非 HTTP 场景均适用 |
| Spring AI 依赖可选性 | `@Qualifier` 强制注入 vs `ObjectProvider.getIfAvailable()` | `ObjectProvider<T>` 注入，方法体内 `getIfAvailable()` 判断后决定是否加入分发 Map | `springAiLlmChatService` 标注 `@ConditionalOnClass`，项目未引入 Spring AI 时 Bean 不存在；强制 `@Qualifier` 注入将抛出 `NoSuchBeanDefinitionException`，导致整个底座不可用；`ObjectProvider` 安全处理可选依赖，底座在无 Spring AI 环境下以 HTTP API 模式正常运行 |
| CircuitBreakerDegradationStrategy 可观测性 | 无状态查询 vs 新增 `getState()` + Micrometer Gauge | `getState(String capabilityId)` + Micrometer Gauge + JMX + 日志状态转换 | ModelEndpointHealthManager 提供 `getState()` 但熔断器无对应接口，运维无法直接获取 CLOSED/OPEN/HALF_OPEN 瞬时状态。查询接口 + Gauge 暴露使熔断状态可纳入监控大盘 |
| RouteConfigChangedEvent 事件定义 | 无定义 vs 补充 Payload + 刷新策略 | 补充 `RouteConfigChangedEvent` 定义（endpointId/changeType/changedAt） | 与 TemplateChangedEvent、ExperimentChangedEvent 一致的设计风格，使路由配置热加载的事件驱动机制具有相同的正式定义和一致性 |
| 事件驱动刷新机制的部署形态约束 | 无记录 vs 显式约束记录 + 分布式兜底方案 | §7 记录约束 + §10 新增分布式兜底段 | `RouteConfigChangedEvent`、`TemplateChangedEvent`、`ExperimentChangedEvent` 三套缓存失效事件均基于 Spring `ApplicationEvent`（进程内事件总线），无法跨 JVM 传播。当前设计假设管理端与底座同进程部署（单体架构），此约束需明确记录。若未来管理端与底座分离部署，三套事件的替代方案为共享缓存（Redis Pub/Sub 或数据库轮询） |
| LocalRuleFallback 泛型类型安全 | 不记录风险 vs 在 §7 显式记录 unchecked 转换风险并增加防御措施 | 在 §7 记录风险 + 在 `AbstractCapabilityExecutor` 中增加 `Class<T>` 显式类型信息 + 在 `doDegrade()` 中增加类型检查日志 | `doDegrade()` 中 `localRuleFallback.fallback(request)` 调用在编译期为 unchecked 转换（`T` → `R`），因泛型擦除在 Phase 4/Phase 5 DTO 混合场景下可能抛出 `ClassCastException`。防御措施：(1) `AbstractCapabilityExecutor` 构造器注入 `Class<T> inputType` 字段，在 `doDegrade()` 中增加 `inputType.isInstance(request)` 类型检查日志；(2) 类型不匹配时日志 ERROR + 返回 `AiResult.degraded()` 而非传播 `ClassCastException`

---

## 8. ai-impl Maven 依赖清单

`ai-impl/pom.xml` 必须显式声明以下所有依赖。设计文档全篇引用的各组件所需的编译期/运行期依赖在此统一列出，标注作用域和可选性。

### 8.1 编译期强制依赖

```xml
<!-- Spring Boot 基础（容器、AOP、配置绑定） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
</dependency>

<!-- Spring Web（REST 调用、RequestContextHolder） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Spring Data JPA + 数据库驱动 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Spring Boot Actuator + Micrometer（指标采集） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Jackson（DTO 序列化/防御性拷贝） -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>

<!-- Caffeine（模板/实验/凭据缓存） -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>

<!-- Guava（RateLimiter 令牌桶） -->
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
</dependency>

<!-- 测试 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

### 8.2 条件性依赖

```xml
<!-- Reactor Core — 流式场景（LlmChatStreamService）的编译期强制依赖
     因 LlmChatStreamService 接口方法签名直接引用 Flux<LlmChatResponse>，
     任何引用此接口的模块（包括同步管线 CapabilityExecutor 的编译路径）在编译期
     均需要 reactor-core 在 classpath 上。已声明为 optional 避免传播给外部消费者，
     但底座模块自身编译时必须引入此依赖。
     注意：此 optional 仅阻止依赖传递，不改变"底座编译必须存在"的事实 -->
<dependency>
    <groupId>io.projectreactor</groupId>
    <artifactId>reactor-core</artifactId>
    <optional>true</optional>
</dependency>

<!-- Spring AI — 仅 SPRING_AI 客户端类型，可选 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-core</artifactId>
    <optional>true</optional>
</dependency>

<!-- jtokkit — 精确 Token 计数（DiscussionConclusionCapabilityExecutor 前置压缩判定）
     仅在讨论结论能力中用于 transcripts 的精确 Token 估算，消除 30%~70% 假阳性触发率。
     optional=true 确保不引入底座强制传递依赖，jtokkit 缺失时回退到字符估算（任务不受影响）。
     对应 §4.1 estimateTokens() 的精确 Tokenizer 分支 -->
<dependency>
    <groupId>com.knuddels</groupId>
    <artifactId>jtokkit</artifactId>
    <optional>true</optional>
</dependency>

<!-- OkHttp — HTTP API 客户端实现（或使用 WebClient 替代）
     若选择 OkHttp 则引入，若使用 Spring WebClient（spring-boot-starter-webflux）则替换
     二选一，建议 Phase 5 采用 OkHttp（轻量，无需 WebFlux 依赖） -->
<!--
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
</dependency>
-->
```

### 8.3 运行期依赖（provided）

```xml
<!-- Phase 4 业务服务模块（薄适配器引用）
     使用 provided 作用域避免编译期强耦合，运行时 Phase 4 模块 JAR 部署在同一容器中
     参见 §2.2 运行时风险说明 -->
<!--
<dependency>
    <groupId>com.aimedical.modules</groupId>
    <artifactId>diagnosis-service</artifactId>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>com.aimedical.modules</groupId>
    <artifactId>inspection-report-service</artifactId>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>com.aimedical.modules</groupId>
    <artifactId>labtest-report-service</artifactId>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>com.aimedical.modules</groupId>
    <artifactId>image-analysis-service</artifactId>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>com.aimedical.modules</groupId>
    <artifactId>examination-service</artifactId>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>com.aimedical.modules</groupId>
    <artifactId>execution-order-service</artifactId>
    <scope>provided</scope>
</dependency>
-->
```

### 8.4 Micrometer 端点暴露配置

`application.yml` 中暴露指标端点（可被 Prometheus 抓取）：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    tags:
      application: aimedical-ai
```

---

## 9. 迁移路径

### 9.1 从 Phase 2~4 独立接入迁移至底座

| 迁移项 | Phase 2~4 现状 | Phase 5 目标 | 迁移策略 |
|--------|---------------|-------------|---------|
| 3.4.1 智能分诊 | 独立 HTTP 调用或 Spring AI 实现 | 迁移至 `AiOrchestrator` 管线 | 新增 `TriageCapabilityExecutor` |
| 3.4.2 AI 处方审核 | 独立实现 + 本地规则降级 | 迁移至 `AiOrchestrator` 管线 | 新增 `PrescriptionCheckCapabilityExecutor` |
| 3.4.3 AI 病历生成 | 独立实现 | 迁移至 `AiOrchestrator` 管线 | 新增 `MedicalRecordGenCapabilityExecutor` |
| 3.4.10 AI 辅助开方 | 独立实现 | 迁移至 `AiOrchestrator` 管线 | 新增 `PrescriptionAssistCapabilityExecutor` |
| 3.4.8 AI 知识库问答 | Phase 2 Mock | 首次真实实现于底座 | 新增 `KbQueryCapabilityExecutor` |
| 3.4.12 AI 医生排班 | Phase 5 首次落地 | 首次真实实现于底座 | 新增 `ScheduleCapabilityExecutor` |
| 3.4.13 AI 综合讨论结论 | Phase 5 首次落地 | 首次真实实现于底座 | 新增 `DiscussionConclusionCapabilityExecutor` |
| 3.4.4 AI 智能诊断 | Phase 4 独立接入 | 不迁移至底座，保留 Phase 4 实现 | 新增 `DiagnosisCapabilityExecutor`（薄适配器，委托至 Phase 4 业务服务） 🟡 依赖 Phase 4 DTO 改造 |
| 3.4.5 AI 智能检查报告 | Phase 4 独立接入 | 不迁移至底座，保留 Phase 4 实现 | 新增 `AnalysisReportForInspectionCapabilityExecutor`（薄适配器） 🟡 依赖 Phase 4 DTO 改造 |
| 3.4.6 AI 智能检验报告 | Phase 4 独立接入 | 不迁移至底座，保留 Phase 4 实现 | 新增 `AnalysisReportForLabTestCapabilityExecutor`（薄适配器） 🟡 依赖 Phase 4 DTO 改造 |
| 3.4.7 AI 影像分析 | Phase 4 独立接入 | 不迁移至底座，保留 Phase 4 实现 | 新增 `ImageAnalysisCapabilityExecutor`（薄适配器） 🟡 依赖 Phase 4 DTO 改造 |
| 3.4.9 AI 开立检查/检验 | Phase 4 独立接入 | 不迁移至底座，保留 Phase 4 实现 | 新增 `RecommendExaminationCapabilityExecutor`（薄适配器） 🟡 依赖 Phase 4 DTO 改造 |
| 3.4.11 AI 执行顺序推荐 | Phase 4 独立接入 | 不迁移至底座，保留 Phase 4 实现 | 新增 `RecommendExecutionOrderCapabilityExecutor`（薄适配器） 🟡 依赖 Phase 4 DTO 改造 |

### 9.2 FallbackAiService 迁移路径

Phase 5 底座引入后，`FallbackAiService` 的代码形态与设计描述存在系统性偏差，需按照以下步骤分阶段迁移：

#### 阶段一：构造器迁移（Bean 装配正确性前提）

现有代码中 `FallbackAiService` 使用 `List<AiService>` 构造注入，Phase 5 需改为 `ObjectProvider<AiService>`：

1. **构造器签名变更**：将 `FallbackAiService(List<AiService> aiServices)` 改为 `FallbackAiService(ObjectProvider<AiService> aiServiceProvider)`
2. **内部委托解析逻辑变更**：
   ```java
   // 旧：delegate = aiServices.stream()
   //        .filter(s -> !(s instanceof FallbackAiService))
   //        .findFirst().orElseThrow(() -> new IllegalStateException("无可用 AiService 实现"))
   // 新：delegate = aiServiceProvider.getIfUnique()
   //        .orElseThrow(() -> new IllegalStateException("无唯一 AiService 实现"))
   ```
3. **`@Primary` 注解添加**：在 `FallbackAiService` 类级别标注 `@Primary`，确保业务模块注入 `AiService` 时优先选择 `FallbackAiService` 实例
4. **编译验证**：修改后执行 `mvn compile` 确认所有引用 `FallbackAiService` 构造器的测试和配置均通过编译
5. **过渡期双降级判定风险说明**：在底座切流过渡期（`ai.platform.enabled=true` 生效但部分业务模块仍直接引用旧 `AiService` 注入路径），`FallbackAiService` 的 `applyStrategies()` 旧路径与新 `CapabilityExecutor` 管线的降级预检可能同时对同一请求执行降级判定，导致"双降级"——`FallbackAiService` 在 `applyStrategies()` 中标记降级后，`CapabilityExecutor` 在管线预检中再次触发降级（或相反）。缓解措施：在过渡期内保持 `applyStrategies()` 方法体不变（通过 `if (aiPlatformEnabled)` 条件开关隔离新旧路径），确保同一请求仅由一条降级路径处理

#### 阶段二：applyStrategies() 迁移（业务逻辑清理）

`FallbackAiService.applyStrategies()` 在 Phase 5 底座激活后不再被调用。迁移步骤如下：

1. **保留方法体不变**：`applyStrategies()` 的 Java 方法体保留（不删除），避免现有分支代码的编译错误
2. **标记 @Deprecated**：在方法上添加 `@Deprecated` 注解，注明 "v5 起降级判定移入 CapabilityExecutor 管线，此方法不再被调用"
3. **调用方剥离开关**：`FallbackAiService.triage()/prescriptionCheck()/...` 中调用 `applyStrategies()` 的代码包裹 `if (aiPlatformEnabled)` 条件：
   ```java
   if (aiPlatformEnabled) {
       // v5+: 不做任何操作，降级由 CapabilityExecutor 管线处理
   } else {
       applyStrategies(context);  // 保留旧路径兼容
   }
   ```
4. **降级策略管理迁移**：旧路径中 `FallbackAiService` 持有的 `List<DegradationStrategy>`（通过构造器全局注入）在 Phase 5 底座切流后不再使用，改为由 `AiPlatformConfig` 按能力标识分别注入到各 `CapabilityExecutor` 实现。`FallbackAiService` 中的策略列表引用在过渡期内保留（赋值为空列表或 `Collections.emptyList()`），确保旧路径 compile 通过但不产生影响
5. **`DegradationContext` 构造迁移**：旧路径 `new DegradationContext()` 零值构造不再用于降级判定，过渡期内 `FallbackAiService` 中所有 `DegradationContext` 构造替换为 `null` 判断——`if (aiPlatformEnabled) { /* 不再构造 DegradationContext */ }`。零值构造器保持存在（不删除），以维护 Phase 0 代码冻结契约
6. **未来清理**：Phase 6 确认全部能力已迁移至底座后，移除 `applyStrategies()` 方法、条件分支中的旧路径、降级策略列表引用及 `DegradationContext` 零值构造器的相关代码

`aiPlatformEnabled` 条件与 `AiPlatformConfig` 的 `ai.platform.enabled` 属性绑定，底座激活时评估为 true。

**配置绑定方式**：`FallbackAiService` 中 `aiPlatformEnabled` 通过 `@Value("${ai.platform.enabled:false}")` 构造器注入，而非字段级 `@Value` 或从 `AiPlatformConfig` 间接引用。理由如下：
- **构造器注入优于字段级注入**：`@Value` 在字段上需搭配 `@Autowired` 才会触发，构造函数注入使配置依赖在构造期即明确，便于单元测试通过构造函数传参模拟不同开关状态
- **优于从 `AiPlatformConfig` 间接引用**：`FallbackAiService` 是 `ai-impl` 的入口装饰器，`AiPlatformConfig` 是底座内部配置类，`FallbackAiService` 直接依赖 `AiPlatformConfig` 会引入对底座内部配置细节的耦合。`@Value` 直接绑定 YAML 属性更轻量、更显式

**`ai.platform.enabled` 与 `ai.mock.enabled` 同时激活的风险**：若 `AiPlatformEnvironmentPostProcessor` 的转发被绕过（如用户显式在 YAML 中配置了 `ai.mock.enabled=true` 同时 `ai.platform.enabled=true`），则 `AiOrchestrator`（底座激活）和 `MockAiService`（Mock 激活）将同时注册为 `AiService` 的非装饰器实现，导致 `AiOrchestrator` 的 `@Bean` 直接注入 `AiService` 依赖时出现 `NoUniqueBeanDefinitionException`。缓解措施：
1. `AiPlatformEnvironmentPostProcessor` 仅在 `ai.mock.enabled` 未显式设置（`env.getProperty("ai.mock.enabled") == null`）时转发，用户显式设置的 `ai.mock.enabled` 不会被覆盖
2. `FallbackAiService` 标注 `@Primary` 确保业务模块注入不受影响，但 `AiPlatformConfig` 内部引用的 `AiService`（如 `AiOrchestrator` 未直接依赖 `AiService`）可能存在二义性
3. 防御措施：`AiPlatformEnvironmentPostProcessor` 在检测到两开关同时为 true 时输出 FATAL 日志，建议运维检查配置；`FallbackAiService` 构造器中同样检测此异常状态并输出 WARN 日志

### 9.3 构造器迁移与 applyStrategies() 迁移的解耦

两套迁移可独立进行，建议先完成构造器迁移（Bean 装配正确性前提，解决 `@Primary` 标注和 `ObjectProvider` 注入），再实施 `applyStrategies()` 剥离（业务逻辑清理）。构造器迁移完成后，`applyStrategies()` 仍以 `if (aiPlatformEnabled)` 条件开关隔离旧路径，确保过渡期编译通过且行为正确。

### 9.4 DegradationStrategy.getOrder() 接口变更影响评估与编译验证

`DegradationStrategy` 接口在 `ai-api` 模块中新增 `default int getOrder() { return 0; }` 方法。此变更为**二进制兼容**变更，不影响现有实现，但需完成以下验证：

1. **编译验证**：执行 `mvn compile -pl modules/ai/ai-api` 确认接口新增 default method 编译通过
2. **全量编译验证**：执行 `mvn compile` 确认所有依赖 `ai-api` 的模块（含项目外部引用方）编译通过，无"类需实现新方法"错误
3. **运行时排序验证**：`CapabilityExecutor` 在 `execute()` 中调用 `getOrder()` 对策略链排序。新增测试用例验证：
   - 未重写 `getOrder()` 的旧策略默认排序值为 0
   - 多个默认值 0 的策略保持声明顺序
   - 混合新旧策略时按 `getOrder()` 升序排列
4. **降级原因字符串稳定性验证**：由于 `DegradationStrategy.getOrder()` 被降级预检循环引用（§4.1），新增 default method 后需验证现有实现（`NoOpDegradationStrategy.isDefault()` 等）的 `getSimpleName()` 返回值不受接口变更影响。验证通过执行 `mvn test` 确认全部 `shouldDegrade()` 降级原因字符串（形如 `"StrategyTriggered:NoOpDegradationStrategy"`）与变更前一致。新增测试用例：断言各 `DegradationStrategy` 实现类的 `getClass().getSimpleName()` 返回值稳定，不受接口新增 default method 影响
5. **发版说明**：在 v5/v6 发版说明中标注此类变更类型为"接口新增 default method，完全向后兼容"；同时在变更日志中单独列出 `getOrder()` 方法的添加理由（为 CapabilityExecutor 降级策略链排序提供支持），便于外部项目引用方评估是否涉及自定义策略实现

### 9.5 配置切换

```yaml
ai:
  platform:
    enabled: true                     # true → AiOrchestrator 激活；false → 底座关闭
  mock:
    enabled: false                    # true → MockAiService 激活（开发/测试），与现有代码兼容
  router:
    routes:
      TRIAGE: { model: "qwen-plus", endpoint: "https://api.example.com/v1/chat/completions", client: "HTTP_API", parameters: {temperature: 0.3, maxTokens: 2048} }
      RX_AUDIT: { model: "qwen-turbo", endpoint: "https://api.example.com/v1/chat/completions", client: "HTTP_API", parameters: {temperature: 0.1, maxTokens: 1024} }
  execution:
    timeout:
      # [动态] 整体管线端到端超时（各能力独立配置，默认 60 秒）
      # 覆盖所有管线步骤（模板渲染、实验分流、变量提取、LLM 调用等）
      # 热加载支持：自定义定时刷新（§3.9「运行时配置热加载机制」），当前实现不支持 @RefreshScope（Map 字段类型限制）
      # 刷新方式：AiPlatformConfig.scheduledConfigRefresh() @Scheduled(fixedDelay=60000) 定时重建 Bean 缓存
      # 重启要求：修改后等待下次轮询周期（最长 60 秒）自动生效，无需重启实例
      per-capability:
        TRIAGE: 60s                     # 分诊管线：模板渲染 + LLM 调用，60s 足够
        RX_AUDIT: 60s                   # 处方审核：同上
        MEDICAL_RECORD_GEN: 90s         # 病历生成：输出较长，放宽至 90s
        PRESCRIPTION_ASSIST: 60s        # 辅助开方：同分诊
        KB_QUERY: 60s                   # 知识库问答：与 LLM 交互为主
        SCHEDULE: 60s                   # 排班：同上
        DISCUSSION_CONCLUSION: 90s      # 讨论结论：基于多轮对话，放宽至 90s
        # ⚠️ 以下薄适配器能力的 per-capability 超时值必须 >= 对应 thin-adapter.per-capability 且保持 +5s 缓冲
        # 层级约束：per-capability = thin-adapter.per-capability + 5s
        # 理由：外层 orTimeout 作为兜底保护，内部 thinAdapterTimeout（delegateFuture.get()）优先触发降级路径
        # 若两值相等，同一请求可能同时触发两超时，导致同一降级原因被误记两次
        DIAGNOSIS: 40s                  # 薄适配器；thin-adapter.per-capability=35s，per-capability=40s（+5s 缓冲）
        ANALYSIS_REPORT_INSPECTION: 30s # 薄适配器；thin-adapter.per-capability=25s，per-capability=30s（+5s 缓冲）
        ANALYSIS_REPORT_LABTEST: 30s    # 薄适配器；thin-adapter.per-capability=25s，per-capability=30s（+5s 缓冲）
        IMAGE_ANALYSIS: 50s             # 薄适配器；thin-adapter.per-capability=45s，per-capability=50s（+5s 缓冲）
        RECOMMEND_EXAM: 25s             # 薄适配器；thin-adapter.per-capability=20s，per-capability=25s（+5s 缓冲）
        RECOMMEND_EXEC_ORDER: 25s       # 薄适配器；thin-adapter.per-capability=20s，per-capability=25s（+5s 缓冲）
      default: 60s
      transcript-summary: 15s           # 讨论结论能力前置LLM调用（转录摘要压缩）的超时阈值，通过 @Value("${ai.execution.timeout.transcript-summary:15s}") 注入 DiscussionConclusionCapabilityExecutor；与主流程 capabilityTimeout 独立，压缩失败时回退到截断文本（不中断主流程）
      parse:                            # [动态] structuredOutputParser.parse() 独立超时配置，与 capabilityTimeoutConfig 共享同一自定义定时刷新机制
        default: 5s                     # 默认解析超时（通过 @Value("${ai.execution.timeout.parse.default:5s}") 注入）
        per-capability:                 # 按能力差异化配置，覆盖默认值；键值结构与 capabilityTimeoutConfig 一致
                                        # ⚠️ 层级约束：parseTimeout <= chatFallbackTimeout（capabilityTimeout 的 40%）
          MEDICAL_RECORD_GEN: 10s       # 病历生成：输出较长 JSON，解析时间较长
          DISCUSSION_CONCLUSION: 8s     # 讨论结论：结构化段落多，解析时间较长
      thin-adapter-default: 30s         # 薄适配器委托调用默认超时（未被 per-capability 或 thin-adapter.per-capability 覆盖时使用）
      thin-adapter:                     # [动态] 薄适配器按能力差异化超时配置
        per-capability:                 # 各薄适配器能力独立配置，说明同 §5.1 "薄适配器委托超时"
                                        # 热加载支持：同 per-capability（共享同一自定义定时刷新机制）
          DIAGNOSIS: 35s                # 诊断：Phase 4 服务 P99 ~25s，设 35s
          ANALYSIS_REPORT_INSPECTION: 25s  # 检查报告：Phase 4 服务 P99 ~15s，设 25s
          ANALYSIS_REPORT_LABTEST: 25s     # 检验报告：同上
          IMAGE_ANALYSIS: 45s           # 影像分析：上传+解析耗时高，Phase 4 P99 ~35s
          RECOMMEND_EXAM: 20s           # 开立检查检验：轻量推理，Phase 4 P99 ~12s
          RECOMMEND_EXEC_ORDER: 20s     # 执行顺序推荐：轻量推理，Phase 4 P99 ~12s
  degradation:
    # [动态] 降级策略白名单（各能力标识→策略名称列表）
    # 热加载支持：自定义定时刷新（§3.9），AiPlatformConfig.refreshDegradationStrategies() @Scheduled(fixedDelay=60000)
    # 热加载生效机制：refreshDegradationStrategies() 构建新 Map 后通过 AtomicReference.compareAndSet() 原子替换引用，
    # CapabilityExecutor 在每轮 execute() 中通过构造器注入的 AtomicReference.get() 获取最新快照，无需重启实例
    # 重启要求：修改后等待下次轮询周期自动生效，无需重启实例
    strategies:
      TRIAGE: [timeout, circuit-breaker]
      RX_AUDIT: [timeout, noop]
    circuit-breaker:
      failure-rate-threshold: 0.5
      window-seconds: 60
      open-duration-seconds: 30
    timeout:
      threshold-percentage: 0.8
      sample-size: 10
    context-ttl-seconds: 60                 # DegradationContext 序列化缓存 TTL，超时后重新构建
  metrics:
    async:
      core-pool-size: 1
      max-pool-size: 2
      queue-capacity: 1000
      rejection-policy: Discard
  rate-limiting:
    enabled: true                        # 是否启用客户端限流，默认 true
    endpoints:                           # 按 endpointId 维度配置限流参数
      "default":                         # 默认限流配置（未被 endpoints 列表覆盖的 endpointId 使用此配置）
        permits-per-second: 10           # 每秒许可数，即平均每秒最多 10 次调用
        max-burst-seconds: 2             # 突发时长（秒），突发时最多积累 20 个令牌
        queue-wait-millis: 50            # 等待令牌的超时时间（毫秒），超时后降级
      "qwen-plus":                       # qwen-plus 端点专用限流配置
        permits-per-second: 20
        max-burst-seconds: 1
        queue-wait-millis: 30
  compression:
    # [静态·重启生效] 讨论结论能力前置 LLM 压缩调用的轻量模型配置
    # compressionLightweightEndpoint — 轻量模型端点标识，通过 @Value("${ai.compression.lightweight-endpoint:compress-default}") 注入
    # compressionLightweightClientType — 轻量模型客户端类型，默认 HTTP_API，通过 @Value("${ai.compression.lightweight-client-type:HTTP_API}") 注入
    # 压缩任务不经过 ModelRouter 路由选择，理由：压缩调用对模型能力要求低，轻量模型可满足摘要需求且成本更低；
    # 压缩调用发生在实验分流和模型路由之前（此时无法获知目标模型端点），压缩失败有截断回退兜底
    lightweight-endpoint: compress-default
    lightweight-client-type: HTTP_API
  template:
    fallback:
      TRIAGE: "请根据以下患者信息进行智能分诊评估：患者主诉、症状描述、既往病史。请给出科室建议。"
      RX_AUDIT: "请根据以下处方信息进行合理用药审核：患者信息、用药清单、诊断结论。请给出审核意见。"
      MEDICAL_RECORD_GEN: "请根据以下就诊信息生成病历：患者基本情况、主诉、现病史、既往史、体格检查、辅助检查。请输出结构化病历。"
      # 各能力兜底 Prompt 配置项。配置方式：ai.template.fallback.{capabilityId}
      # - 可选/必填：可选。若某能力未配置兜底 Prompt 且数据初始化也未插入兜底记录，DatabasePromptTemplateManager
      #   返回通用兜底 Prompt（"请根据以下信息提供{{capabilityName}}分析结果：{{userInput}}"），确保永不返回 null
      # - 默认行为：兜底 Prompt 不含 DTO 变量注入（占位符 {{key}} 不会被替换），CapabilityExecutor 使用兜底 Prompt 时
      #   需确保 USER message 包含完整 DTO 输入信息以补偿缺失的变量注入
      # - 管理方式：兜底 Prompt 可通过 YAML 配置或通过 PromptTemplate 表的 DRAFT 状态记录初始化，运行态调整不重新编译
  sliding-window:
    # [动态] 滑动窗口时间宽度，默认 60 秒
    # 热加载支持：AtomicLong + 自定义定时刷新（AiPlatformConfig.refreshWindowSeconds() @Scheduled(fixedDelay=60000)）
    # 重启要求：修改后等待下次轮询周期（最长 60 秒）自动生效，无需重启实例
    # 注意：不使用 @RefreshScope，避免 Bean 销毁重建导致窗口内全部数据丢失
    window-seconds: 60                   # 滑动窗口时间宽度，默认 60 秒
    max-events-per-capability: 10000     # 每能力最大事件数，超出后丢弃旧事件
```

---

## 10. 与其他包的协作边界

### 10.1 包 G 与包 F（AI 分诊台规则）的协作

- 包 F 在管理端维护分诊规则（症状-科室映射），产出 `rule_version` / `rule_set_id`
- 包 G 的 `PromptTemplateManager` 在渲染分诊 Prompt 时，允许模板引用当前规则版本号变量

### 10.2 包 G 与包 E（AI 影像模型管理面）的协作

- 包 E 管理影像模型注册、岗位授权与调用审计
- 包 G 的 `ModelRouter` 路由表中，3.4.7 影像分析的 `ModelRoute` 由包 E 管理的模型注册数据驱动

### 10.3 ai-impl 与 Phase 4 业务模块的耦合约束治理规则

`ai-impl` 以 `provided` 作用域声明对 Phase 4 业务服务模块的 Maven 依赖（参见 §2.2），此设计在保持松耦合的同时需配合以下治理规则：

1. **接口引用范围限定**：薄适配器型 CapabilityExecutor 仅可引用 Phase 4 模块的 **Service 层接口**（`com.aimedical.modules.*.service.*`），不得引用 Entity、Repository、Controller 或其他内部类型。Code Review 阶段检查此约束，违反视为架构级缺陷
2. **依赖隔离**：薄适配器中引用的 Phase 4 接口类型**不允许**暴露到 `ai-impl` 的公共 API 中（即不在 `AiOrchestrator`、`AiMetricsCollector` 等公共组件的参数/返回值类型中出现），确保 Phase 4 依赖的类型泄漏范围局限在薄适配器实现类内部
3. **接口变更影响评估**：Phase 4 业务服务接口发生不兼容变更（方法签名、异常声明）时，需同步评估 `ai-impl` 中对应薄适配器实现的影响——若变更为新增方法，薄适配器无需修改；若变更为删除或修改方法签名，薄适配器需在 Phase 4 变更发布前完成适配。此评估纳入跨包 OOD 评审工单检查项
4. **升级计划**：若 `provided` 作用域在运行期因类加载顺序出现不可预测的行为（如 Phase 4 业务模块未先于 `ai-impl` 加载），回退方案为改为 `compile` 作用域 + 在 §7 设计决策表中记录耦合约束的附加决策理由

### 10.4 分布式部署兜底

当前设计中的三套缓存失效事件——`RouteConfigChangedEvent`（§3.2 路由配置热加载）、`TemplateChangedEvent`（§3.3 模板重建策略）、`ExperimentChangedEvent`（§3.4 实验重建策略）——均基于 Spring `ApplicationEvent`（进程内事件总线），仅支持管理端与底座同进程部署（单体架构）。此约束已在 §7 设计决策表中记录。

若未来架构演进为管理端与底座分离部署（跨 JVM 或多实例部署），上述事件驱动刷新机制无法跨进程传播，需替换为以下分布式方案：

| 替代方案 | 描述 | 优点 | 缺点 |
|---------|------|------|------|
| **Redis Pub/Sub** | 管理端发布变更事件到 Redis Channel，底座各实例订阅同一 Channel 消费 | 实时性高（毫秒级）；与现有事件驱动模式一致（发布-订阅） | 引入 Redis 依赖；消息丢失不重试 |
| **数据库轮询** | 底座各实例通过 `@Scheduled` 定时轮询配置表的 `updated_at` 时间戳，发现变更后刷新本地缓存 | 无需额外中间件；消息不丢失（基于数据库持久化） | 秒级延迟（轮询周期）；增加数据库读压力 |

**推荐演进路径**：Phase 5 维持单体架构，事件驱动基于进程内 `ApplicationEvent`；Phase 6 引入分布式部署时优先采用 Redis Pub/Sub 方案，保持事件驱动编程模型的一致性。三套事件可共用一个 Redis Channel（通过 Payload 的 `changeType` 字段区分事件类型），或各使用独立 Channel（路由/模板/实验互不干扰）。

### 10.5 包 G 与其他业务模块的协作

- 业务模块仍通过 `AiService` 接口调用 AI 能力，不感知底座内部变化
- `AiMetricsCollector` 写入的 AI 调用日志可被管理端统计分析
- `PromptTemplateManager` 的模板维护入口在管理端，归包 A（管理员端基础数据维护）

---

## 11. 测试策略

### 11.1 单元测试模式

每个 `CapabilityExecutor` 实现使用 `@MockBean` 模拟所有下游依赖（`PromptTemplateManager`、`ModelRouter`、`LlmChatService`、`StructuredOutputParser`、`AiMetricsCollector`、`SlidingWindowMetricsStore`、`ModelEndpointHealthManager`、`ExperimentManager`、`LocalRuleFallback`），验证以下降级路径触发条件：

- **熔断触发**：`CircuitBreakerDegradationStrategy.shouldDegrade()` 返回 true → 验证降级路径执行，`LlmChatService.chat()` 未被调用
- **超时触发**：`CompletableFuture.orTimeout()` 超时 → 验证降级记录 `DegradationReason.TIMEOUT`
- **端点不可用**：`ModelEndpointHealthManager.getState()` 返回 UNAVAILABLE 且 `tryProbe()` 返回 false → 验证降级路径
- **路由不可用**：`ModelRouter.route()` 返回 null → 验证降级记录 `DegradationReason.NO_AVAILABLE_ROUTE`
- **解析失败**：`StructuredOutputParser.parse()` 抛出 `ParseException` → 验证降级记录 `DegradationReason.PARSE_FAILURE`
- **模板渲染异常**：`PromptTemplateManager.render()` 抛出异常 → 验证 `getFallbackPrompt()` 被调用，管线继续执行
- **实验分流异常**：`ExperimentManager.assign()` 抛出异常 → 验证使用 `ExperimentAssignment.createErrorFallback()`（`groupId="experiment-error"`），管线继续执行

**降级预检前置验证**：编写独立测试验证 `AbstractCapabilityExecutor.execute()` 中降级预检位于 `supplyAsync()` 之前——验证当熔断器 OPEN 时，线程池队列长度无变化（降级请求未入池排队）。

**X-Department-ID / X-Visit-ID HTTP Header 提取测试**：薄适配器型 CapabilityExecutor 的 `doExtractDepartmentId()`/`doExtractVisitId()` 方法使用 `MockHttpServletRequest` 模拟 HTTP Header 注入，验证科室标识/就诊标识提取正确性，并覆盖 Header 缺失场景（返回 null 而非异常）。

### 11.2 集成测试模式

使用 `@SpringBootTest` + `@TestConfiguration` 模拟底座激活状态，覆盖以下场景：

- **完整管线集成**：7 项底座能力同时注入真实 `LlmChatService` 实现与模拟的 `PromptTemplateManager`，验证端到端管线在多组件协作下的正确性
- **薄适配器集成**：Phase 4 业务服务接口通过 `@MockBean` 注入，验证薄适配器委托调用的超时降级路径
- **Bean 装配互斥**：验证 `ai.platform.enabled=true` 时 `AiOrchestrator` 激活且 `MockAiService` 不激活，`ai.platform.enabled=false` 时反之
- **配置转发正确性**：验证 `ai.platform.enabled=true` → `ai.mock.enabled=false` 的 `EnvironmentPostProcessor` 转发逻辑

### 11.3 管线收敛验证

关键设计决策的可测试性证明：

- **滑动窗口指标**：`SlidingWindowMetricsStore` 的 `recordSuccess()`/`recordFailure()`/`recordDegraded()` 三分类存储独立验证：写入后通过 `getFailureRate()` 与 `getEffectiveFailureRate()` 的返回值差异验证降级兜底不稀释 LLM 失败率
- **防御性拷贝**：测试验证 `AbstractCapabilityExecutor.execute()` 入口处 `ObjectMapper.convertValue()` 生成的副本与原始 `request` 对象引用不同（`!=`），且修改副本不影响原始对象
- **异步上下文传播**：验证 `execute()` 入口处提取的 `userId`/`departmentId`/`callerRole`/`callerId` 在线程池线程中可正确通过参数传递，无需访问 `SecurityContextHolder`。通过构建匿名 `SecurityContext` 后调用 `execute()`，验证 lambda 内部取到的上下文值是否与入口处一致
- **orTimeout 兜底**：使用超短超时阈值（如 1ms）验证超时后降级路径被执行且 `DegradationReason.TIMEOUT` 正确记录
- **ModelRoute.authType**：验证 `AuthType` 枚举在 `ModelRoute` 中的序列化与反序列化正确性，以及 `authType=NONE` 时 `LlmChatService` 不附加认证头

### 11.4 状态恢复路径验证

所有具有状态机的组件需覆盖完整状态转换路径的验证，确保恢复路径可触发：

| 组件 | 状态机 | 需验证的恢复路径 |
|------|--------|----------------|
| `CircuitBreakerDegradationStrategy` | CLOSED ↔ OPEN ↔ HALF_OPEN | OPEN→HALF_OPEN 窗口到期后探测调用成功→CLOSED；HALF_OPEN 探测失败→OPEN |
| `ModelEndpointHealthManager` | CONNECTED ↔ DEGRADED ↔ UNAVAILABLE | UNAVAILABLE 下 tryProbe() 返回 true 后探测成功→CONNECTED；DEGRADED 下 1 次正常调用→CONNECTED |
| `CredentialProvider` | NORMAL ↔ CACHE_ONLY ↔ BACKOFF | CACHE_ONLY→NORMAL（Vault 恢复正常后探测查询成功，重置连续失败计数器）；BACKOFF→NORMAL（30 秒退避窗口到期触发探测查询成功，重置连续失败计数器） |
| `PromptTemplate` 状态 | DRAFT / ACTIVE / DEPRECATED | DEPRECATED→ACTIVE 回滚操作的正确性（自动将当前 ACTIVE 降级为 DEPRECATED） |
| `Experiment` 状态 | DRAFT / ACTIVE / PAUSED / COMPLETED | ACTIVE→PAUSED→ACTIVE 的完整回环 |

验证方式：通过反射直接操作状态字段（如 `AtomicReference<CircuitState>`）将状态预置为特定值，再执行触发操作并断言转换后的状态值和副作用正确。

### 11.5 并发竞争验证

以下并发竞争场景需通过多线程测试覆盖，使用 `CountDownLatch` + `CyclicBarrier` 控制线程执行时序：

| 竞争场景 | 涉及组件 | 验证目标 | 测试方法 |
|---------|---------|---------|---------|
| 滑动窗口多线程写入 | `SlidingWindowMetricsStore` | 多线程并发 `recordSuccess()`/`recordFailure()` 不导致窗口数据丢失或计数错误 | 20 线程各写入 100 次，验证读写方法返回的统计值（`getFailureRate()`/`getAverageElapsed()`）与单线程等效计算结果一致（允许 ±1 偏差） |
| 熔断器 CAS 边界竞争 | `CircuitBreakerDegradationStrategy` | OPEN→HALF_OPEN 转换时多个探测请求同时到达，仅一个能实际发送探测 | 构造 OPEN 状态 + HALF_OPEN 窗口到期，20 线程并发执行 `shouldDegrade()`，验证被允许探测的线程数不超过 1 |
| 熔断器 record 竞争 | `CircuitBreakerDegradationStrategy` | 并发 `recordSuccess()`/`recordFailure()` 不覆盖对方的状态转换判定 | 10 线程成功 + 10 线程失败，验证最终 CLOSED/OPEN 状态与顺序执行结果一致 |
| 端点健康状态并发转换 | `ModelEndpointHealthManager` | 多线程同时调用 `recordCallResult()` 和 `tryProbe()` 不导致状态跃迁丢失 | 20 线程并发混合调用，验证最终状态落在有效转换表范围内 |
 | `CompletableFuture.orTimeout()` 竞态 | `AbstractCapabilityExecutor` | 超时回调与正常完成回调同时触发时，不产生双重降级/双重记录 | 使用 1ms 超时阈值 + 慢速 mock LLM 调用，验证 `AiCallRecord` 仅记录一次调用结果（成功或降级之一） |

### 11.6 配置热刷新集成测试

为验证 §3.9「运行时配置热加载机制」中定义的自定义定时刷新逻辑的正确性，编写以下集成测试：

| 测试场景 | 测试方法 | 验证目标 |
|---------|---------|---------|
| `execution.timeout.per-capability` 热刷新 | 通过 `@TestConfiguration` 准备初始 `AiExecutionProperties`，调用 `AiPlatformConfig.refreshCapabilityTimeoutConfig()` 模拟刷新，更新 Environment 中的配置值后再次调用刷新方法 | 刷新后 `capabilityTimeoutConfig` Bean 中的值更新为新值（通过 `@Value` 重新读取），且 `AbstractCapabilityExecutor` 中使用的 `orTimeout()` 超时阈值同步为更新后的值 |
| `degradation.strategies` 热替换 | 模拟 YAML 中 degradation.strategies 配置变更，调用 `refreshDegradationStrategies()` 方法重建 `degradationStrategyMapRef`（`AtomicReference<Map<String, List<DegradationStrategy>>>`） | 重建后 AtomicReference 内的 Map 引用与旧 Map 不同（新构造的不可变 Map），已在运行的 `CapabilityExecutor` 实例在下次 `execute()` 中通过 `degradationStrategyMapRef.get()` 读取到新策略列表；验证新旧 Map 的 `==` 比较返回 false 且新 Map 中策略列表与配置一致 |
| `sliding-window.window-seconds` 刷新 | 通过 `Environment` 模拟配置变更后调用 `AiPlatformConfig.refreshWindowSeconds()`，验证 `SlidingWindowMetricsStore.windowSecondsRef`（`AtomicLong`）更新为新值，惰性淘汰阈值 `System.currentTimeMillis() - windowSeconds * 1000` 同步生效，且滑动窗口内的现存事件不丢失 |
| 无效配置刷新不降级 | 将 YAML 中 `execution.timeout.per-capability.TRIAGE` 改为非法值（如 `0s`），执行刷新后调用 `TriageCapabilityExecutor.execute()` | 刷新失败时回退到默认值（60s），不抛出 `IllegalStateException` 或导致管线中断 |

执行方式：`@SpringBootTest(properties = {"ai.platform.enabled=true"})` + `@TestConfiguration` 覆盖配置源。四组测试使用 `@Nested` 分组，独立刷新互不干扰。

## 修订说明（v27）

| 审查意见 | 修改措施 |
|---------|---------|
| 1. [严重] 薄适配器构造器 `super()` 调用参数与父类构造器签名不匹配——`DiagnosisCapabilityExecutor` 构造器中的 `super()` 调用传递了 13 个实参，但 `AbstractCapabilityExecutor` 构造器声明了 15 个形参，缺失 `parseTimeoutConfig` 和 `parseTimeoutDefault`，且实参位置错位 | (§3.1) `DiagnosisCapabilityExecutor` 构造器入参新增 `@Qualifier("parseTimeoutConfig") Map<String, Duration> parseTimeoutConfig` 和 `@Value("${ai.execution.timeout.parse.default:5s}") Duration parseTimeoutDefault`；`super()` 调用同步追加对应两实参，补齐为 15 个实参与父类 16 参数构造器（含 inputType）逐一对应（inputType 传 null）。新增注释说明其余 5 个薄适配器子类构造器按此模式实现 |
| 2. [重要] `doDegrade` 15 参数签名的高编码风险——自 v14 起连续被标记，v21 曾尝试引入 `CallContext` 但因改动量过大回退 | (§3.1) 「构造器参数数量说明」段重新设计为「三期分阶段迁移计划」：一期定义 `CallContext` 值对象 + `AiCallRecord` 工厂方法新旧签名共存（`@Deprecated`）；二期 `doDegrade()` 新增 CallContext 重载版本，逐个能力迁移调用点；三期移除旧签名。每期设定可验收的里程碑。计划替代原有的"一次性替换"方案 |
| 3. [重要] §3.5 工厂方法签名与 §4.1 伪代码调用点不一致——§3.5 仅以 CallContext 简化签名示出，但 §4.1 全文所有 `AiCallRecord` 调用仍使用旧多参数签名 | (§3.5) 将原单一段落拆分为「当前工厂方法签名（旧多参数版本）」和「简化签名（Phase 5 第二阶段重构目标）」两部分，先列出旧多参数完整签名表（与 §4.1 调用点一致），后列出 CallContext 简化签名作为重构目标。消除同一文档内不同章节给实现者不一致的信号 |
| 4. [一般] `inputType` 字段在类图和构造器中均未定义，但被 `doDegrade()` 引用——`inputType.isInstance(request)` 在 §4.1 `doDegrade()`（行 3379）中使用，但 `inputType` 仅出现在类图（行 454）而缺少构造器注入描述 | (§3.1) `AbstractCapabilityExecutor` 构造器参数列表首部新增 `Class<T> inputType` 参数，及对应注入说明；`DiagnosisCapabilityExecutor` 薄适配器构造器的 `super()` 调用首部补充 `null` 对应 inputType 位置 |
| 5. [一般] `KbQueryRequest` 同时以业务字段和继承字段声明 `departmentId`——§3.11.5 扩展字段列表与 `AiRequestBase` 均包含 `departmentId`，两处赋值路径可能不一致 | (§3.11.5) 采纳方案 A：`KbQueryRequest` 扩展字段中移除 `departmentId`，查询范围通过 `queryScope` 和 `AiRequestBase.departmentId` 联合表达，在设计说明中显式说明 `departmentId` 由基类继承提供，不重复声明。同步更新 §3.5 DTO 改造工作量概览表中 `KbQueryRequest` 的新增字段数（4→3） |
| 6. [严重] `extractVariables()` 职责归属矛盾——§3.11.7 要求 `extractVariables()` 承担 LLM 压缩调用，§4.1 将压缩逻辑实现在 `doExecuteInternal()` 中 | (§3.11.7) Prompt 模板结构行：`extractVariables()` 改为 `doExecuteInternal()` 前置阶段承担摘要提取职责；模板变量提取策略行：说明 `extractVariables()` 仅负责基本字段提取，`transcriptSummary` 生成由 `doExecuteInternal()` 前置阶段统一执行，职责归属清晰，消除矛盾 |
| 7. [严重] `super.doExecuteInternal()` 调用不可行——父类 `doExecuteInternal()` 声明为 abstract，无法通过 super 调用 | (§3.1/§4.1) 父类新增 `protected final executeStandardPipeline()` 非抽象方法封装标准管线步骤；`DiscussionConclusionCapabilityExecutor.doExecuteInternal()` 伪代码中 `super.doExecuteInternal()` 改为 `executeStandardPipeline()`，并添加注释说明 abstract 限制与替代方案 |
| 8. [重要] `preciseTokenCount()`/`formatTranscripts()`/`truncateTranscripts()` 三个辅助方法在类图、文档中无正式定义 | (§3.11.7) 在能力特化表后新增「辅助方法定义」段落，提供三个方法的完整方法签名、入参/返回值类型、行为契约和线程安全说明，并标注实现归属位置 |
| 9. [重要] 字符估算回退分支中 `estimatedTokens = 2000` 硬编码跳跃值缺乏依据 | (§4.1 `doExecuteInternal()` 回退分支) 移除 `estimatedTokens = 2000` 硬编码跳跃值，回退分支触发阈值直接从 3000 提升至 4000 tokens，较低估算值直接使用估算结果（因阈值提升已降低假阳性率），并补充设计理由说明直接提阈值优于跳跃值的语义透明性 |
| 10. [重要] §2.3 类图未展示 `AbstractCapabilityExecutor` 构造器签名 | (§2.3) `AbstractCapabilityExecutor` 类图中新增构造器声明行，包含 16 参数全量签名概要及注释说明完整签名见 §3.1；同时新增 `executeStandardPipeline()` 方法行 |
| 11. [一般] `userId` 与 `callerId` 语义冗余，来源相同（均来自 `auth.getName()`），产生同步认知负担 | (§3.1 `UserId/SessionId/callerRole/callerId` 段) 新增「`userId` 与 `callerId` 语义区分」子段，说明两值语义区别（业务用户标识 vs API 调用方标识）、当前值相同的原因（同一认证源）、未来潜在差异（服务间调用场景），以及各自在管线中的使用场景（userId 用于实验分流哈希分桶，callerId 用于 AiCallRecord 审计字段），消除同步疑虑 |
| 12. [一般] `convertValue()` 防御性拷贝失败（不可变 DTO 未标注 Jackson 注解时）导致能力调用直接降级 | (§3.1/§4.1) 所有 `objectMapper.convertValue()` 调用点包裹 try-catch：捕获异常后回退到原始 request（接受下游修改风险），日志 WARN 输出异常信息并建议补全 Jackson 注解。防御性拷贝失败不再是降级触发条件 |
| 13. [一般] `structuredChat()` 成功路径中 `fall-through` 到共享处理器的控制流不直观 | (§4.1 `doExecuteInternal()` 伪代码) 两处 `// fall-through to shared success handler` 替换为 `// → [shared_success_handler]` 结构化锚点标记；在共用成功处理段起始处添加 `// [shared_success_handler]:` 标签注释，形成 `goto` 风格的显式控制流映射。同时修正 `else:` 缩进与 `if:` 对齐 |

## 修订说明（v28）

| 审查意见 | 修改措施 |
|---------|---------|
| 1. [重要] `DiscussionConclusionCapabilityExecutor` 构造器未完整定义——缺少 `compressionLightweightEndpoint` 和 `compressionLightweightClientType` 两个配置参数的构造器签名、`super()` 调用示例及类图字段 | (§3.11.7) 在能力特化表之后新增完整的 18 参数构造器签名定义（含父类 16 参数 + 2 个特有压缩参数）及 `super()` 调用示例；新增特有字段声明表（`compressionLightweightEndpoint`/`compressionLightweightClientType`/`transcriptSummaryTimeout`）；(§2.3) 类图中 `DiscussionConclusionCapabilityExecutor` 节点补充 4 个特有字段声明 |
| 2. [重要] `executeStandardPipeline()` 抽象定义与 §4.1 伪代码实现不一致——`DiscussionConclusionCapabilityExecutor.doExecuteInternal()` 注释声称"复用标准流程"但实际重新实现了实验分流和模板渲染两个步骤的完整伪代码 | (§4.1) 采纳方案 A：将展开的伪代码替换为直接的 `executeStandardPipeline()` 调用，前置阶段完成压缩摘要后直接调用父类标准管线方法（`return executeStandardPipeline(...)`）。所有管线参数已在前置阶段准备就绪，降级/超时处理由 `executeStandardPipeline()` 内部统一管理 |
| 3. [重要] `transcriptSummaryExecutor` 的 `CallerRunsPolicy` 与线程池隔离目的在语义上存在矛盾——队列满时 `CallerRunsPolicy` 将压缩任务回退到 `llmCallExecutor` 的 Worker 线程，隔离设计完全失效 | (§3.9) `transcriptSummaryExecutor()` Bean 定义中拒绝策略从 `CallerRunsPolicy` 改为 `DiscardPolicy` + WARN 日志。被丢弃的压缩任务通过 `DiscussionConclusionCapabilityExecutor.doExecuteInternal()` 中 catch 块的 `truncateTranscripts()` 回退逻辑（截断文本）保障主流程 |
| 4. [中等] 13 项能力 DTO 的 4 个公共字段（`visitId`/`patientId`/`sessionId`/`departmentId`）的调用方填充责任未明确定义 | (§3.5) 新增 «3.5.1 调用方数据准备指引» 独立小节，集中说明：数据来源总表（按底座能力/薄适配器/HTTP 调用方/MQ 调用方四个维度）、过渡期为空时底座行为（null 安全策略及对各维度的影响）、现有模块兼容性保护（底座能力 DTO 可传 null、薄适配器独立提取、`null-safe-fields` 过渡期保护开关） |
| 5. [中等] 熔断器-端点健康管理器统一探测机制的多端点场景处理规则未定义——`ModelRouter.route()` 返回指向多个 `endpointId` 的 `ModelRoute` 时，决策表仅以单一 `endpointId` 为判断维度 | (§3.8) `CircuitBreakerDegradationStrategy` 新增«熔断器作用域粒度与多端点场景规则»段落，说明：熔断器以 `endpointId` 为作用域粒度（非 `capabilityId`）；`doExecuteInternal()` 逐 route 检查端点健康状态（跳过不可用端点继续尝试下一个）；设计意图解释（能力共享端点/多端点容灾等场景的行为正确性）；提供 `strategy-scope: per-capability` 配置覆盖选项 |
| 6. [轻微] `compressionLightweightEndpoint` 的 `@Value("${ai.compression.lightweight-endpoint}")` 缺少 `:` 默认值语法，YAML 配置缺失时 Spring 启动期抛出 `IllegalArgumentException` | (§3.11.7/§4.1/§9.5) 所有三处 `@Value("${ai.compression.lightweight-endpoint}")` 引用均改为 `@Value("${ai.compression.lightweight-endpoint:compress-default}")`，与 YAML 中 `lightweight-endpoint: compress-default` 配置值一致 |
|--:|:--|

## 修订说明（v29）

| 审查意见 | 修改措施 |
|---------|---------|
| 1. [重要] `doDegrade()` 参数签名在类图、迁移计划与伪代码之间呈现四态并存——§2.3 类图使用 15 参数旧签名，§3.1 迁移计划保留旧签名，§3.5 展示 CallContext 简化签名工厂方法，§4.1 伪代码使用旧 15 参数签名 | (§2.3) `AbstractCapabilityExecutor` 类图中 `doDegrade()` 方法新增 CallContext 重载签名行（7 参数），并标注 `// 二期迁移目标，详见§3.1「三期分阶段迁移计划」`，使类图显式指向迁移计划作为单一真理源 |
| 2. [中等] 缺少启动期 Bean 初始化顺序的整体依赖约束图——文档散布多处启动期时序依赖，但缺少整体化 Bean 初始化顺序依赖图 | (§3.9) 新增独立子章节 §3.9.1「启动期 Bean 初始化顺序依赖图」，提供 Mermaid 依赖图覆盖四条关键约束链：AiPlatformEnvironmentPostProcessor → @ConditionalOnProperty 评估、DegradationStrategy 初始化 → AiPlatformConfig.@PostConstruct、CapabilityExecutor 初始化 → AiOrchestrator.@PostConstruct、HikariCP 就绪 → @PostConstruct 预热查询 |
| 3. [中等] 6 项薄适配器能力的特化设计缺乏集中视图——行为描述分散在 §3.1、§3.5、§4.2 三处，缺少类似 §3.11 结构的薄适配器特化设计集中表 | (§3.12) 在 §3.11 之后新增独立子章节 §3.12「薄适配器能力特化设计」，按与 §3.11 一致的格式逐能力列出：能力标识、DTO 扩展字段、Phase 4 服务接口引用、异常契约、依赖状态。涵盖全部 6 项薄适配器（DiagnosisCapabilityExecutor / AnalysisReportForInspectionCapabilityExecutor / AnalysisReportForLabTestCapabilityExecutor / ImageAnalysisCapabilityExecutor / RecommendExaminationCapabilityExecutor / RecommendExecutionOrderCapabilityExecutor）及共同约束 |
| 4. [一般] `structuredChat()` 双降级路径的根因追溯缺口——`structuredChat` 内部超时回退到 `chat()`，`chat()` 也超时时，降级原因统一记录为 `DegradationReason.TIMEOUT`，无法区分 | (§3.9 DegradationReason 枚举定义) `TIMEOUT` 枚举值的使用场景说明中扩展为完整细分标识列表：`:structuredChatTimeout`（structuredChat 内部 60% 窗口超时）、`:chatFallbackTimeout`（chat 回退 40% 窗口超时）、`:ThinAdapterTimeout`（薄适配器委托超时）、`:transcriptSummaryCrowding` / `:primaryLlmTimeout`（讨论结论超时原因细化），使伪代码中现有的 `+ ":subType"` 拼接有正式枚举说明支撑 |
| 5. [一般] 多实例配置生效时间窗口差异未做约束分析——`@Scheduled` 定时刷新在多实例部署时配置生效时间不一致，`degradation.strategies` 等安全敏感配置在实例间存在最长接近一个轮询周期的配置差异窗口 | (§1.5) 新增子章节 §1.5.4「配置生效时间窗口差异约束分析」，逐项分析 `execution.timeout.per-capability`、`degradation.strategies`、`sliding-window.window-seconds` 三项热加载配置的多实例生效时间不一致窗口、对超时阈值调整/降级策略切换/熔断器全局性等运维操作的影响，以及运维操作建议和未来改进方向 |

## 修订说明（v30）

| 审查意见 | 修改措施 |
|---------|---------|
| 1. [重要] §4.2 薄适配器超时 WARN 日志使用全局默认值（`thinAdapterTimeout`）而非实际生效的 per-capability 覆盖值（`effectiveThinAdapterTimeout`），误导运维排障 | (§4.2) 薄适配器特化管线伪代码中 WARN 日志的 timeout 取值从 `thinAdapterTimeout.toMillis()` 改为 `effectiveThinAdapterTimeout.toMillis()`，使日志反映实际生效的超时值 |
| 2. [重要] LlmChatOptions 阶段一填充伪代码（§4.1）仅映射了 temperature/maxTokens/stopSequences 三个 key，与白名单声明（六个 key：temperature/maxTokens/stopSequences/topP/frequencyPenalty/presencePenalty）不一致，后三个参数被忽略 | (§4.1) LlmChatOptions 阶段一填充伪代码补充 `topP`、`frequencyPenalty`、`presencePenalty` 三个字段的映射逻辑，与 §3.2 白名单声明一致 |
| 3. [一般] PrescriptionAssistCapabilityExecutor 使用方式 A（`ObjectMapper.convertValue`）提取模板变量，但 DTO 将患者信息封装为 `PatientInfo` 内嵌值对象，`convertValue` 产生嵌套 key（如 `patientInfo.age`）而非扁平 key（如 `patientAge`），与 `PrescriptionCheckCapabilityExecutor`（方式 B 自定义手工展开）不一致 | (§3.11.4) 模板变量提取策略从「方式 A（默认 ObjectMapper.convertValue）」改为「方式 B（自定义）：从 `patientInfo` 值对象手工展开提取扁平变量」，与 PrescriptionCheckCapabilityExecutor 保持一致 |
| 4. [一般] AiCallRecord.capabilityName 字段存在字段定义（§3.5 字段表）但三个工厂方法签名均无对应入参，§4.1 所有调用点未传入，实施者无法获知填充来源 | (§3.5) 字段定义补充说明：`capabilityName` 由工厂方法内部根据 `capabilityId` 从 `capabilityNameMapping` 自动解析，工厂方法签名不单独列出该参数；字段填充策略新增 `capabilityName` 解析策略说明 |
| 5. [一般] 精确 Tokenizer 路径与字符估算回退路径共用同一 3000 阈值判断（§4.1），v27 修订意图要求估算路径阈值提升至 4000 但仅添加 WARN 日志未改变实际触发阈值 | (§4.1 DiscussionConclusionCapabilityExecutor 伪代码) 引入 `compressionThreshold` 变量按路径区分阈值——精确路径使用 3000，估算路径使用 4000，压缩触发判定统一使用 `compressionThreshold` |

