# 质量审查报告 — Phase 5 包 G OOD 设计（v13，修订版）

## 审查范围
- 待审查产出：`a_v13_copy_from_v12.md`
- 审查视角：实际落地评估，侧重需求响应充分度、整体深度和完整性、事实错误和逻辑矛盾；补充下游消费者视角
- 已排除维度：组件A内部审议已覆盖的技术可行性等维度

---

## 一、需求响应充分度评估

### 1.1 用户需求逐条对照

| 用户需求约束 | 产出覆盖状态 | 说明 |
|-------------|------------|------|
| 完成 Phase5 包 G 的完整 OOD 设计 | ✅ 已覆盖 | 产出为完整 OOD 设计文档（3983 行），覆盖 13 项 AI 能力的底座化设计、7 项底座能力 + 6 项薄适配器的完整定义 |
| 覆盖类图 | ✅ 已覆盖 | §2.3 提供了完整的 Mermaid 类图（约 490 行），包含 35+ 核心类/接口及其关联关系 |
| 覆盖核心职责 | ✅ 已覆盖 | §3 各子节为每个核心抽象明确定义了"角色/职责/协作对象/类型形态选择理由"四元组，与 Phase0 风格一致 |
| 覆盖协作关系 | ✅ 已覆盖 | §3 协作对象段落显式描述了各抽象的依赖关系；§4 伪代码以调用栈形式展示了完整的协作流程；§2.2 提供了模块依赖方向 |
| 覆盖关键接口 | ✅ 已覆盖 | `LlmChatService`、`CapabilityExecutor`、`ModelRouter`、`ExperimentManager` 等核心接口均完整定义了方法签名、入参/返回值契约及异常声明 |
| 覆盖状态模型 | ✅ 已覆盖 | `ModelEndpointHealthManager`（3 态）、`CircuitBreakerDegradationStrategy`（3 态）、`CredentialProvider`（3 态）、`PromptTemplate`（3 态）、`Experiment`（4 态）等均完整定义了状态图和转换表 |
| 保持与 Phase0/Phase1ABD 风格一致 | ✅ 已覆盖 | §1.2 明确列出了 5 条风格一致性规则并标注来源；§7 设计决策表采用与 Phase0 §5 一致的四列表格式 |

**结论**：产出充分响应了用户需求的全部核心约束。OSS 要素覆盖完整，风格一致性要求有显式规则和对照。

### 1.2 整体完整性评价

产出历经13轮迭代，文档结构完整（概述→模块→核心抽象→行为契约→错误处理→并发设计→设计决策→依赖→迁移→协作边界→测试策略），章节顺序合理。各核心抽象的"职责/协作对象/为何选择此形态"三段式描述与已发布的 Phase0/Phase1ABD 成果保持术语体系和表述粒度一致，可实现跨阶段文档的便捷对照。

---

## 二、发现的问题

### 问题 1：§5.1 错误分类表"实验分流异常"处理方式与 v13 实际设计矛盾

**问题描述**：§5.1 错误分类表中「实验分流异常」行的处理方式描述为"日志 WARN + 降级到 default 分组（`new ExperimentAssignment(null, "default", null, null)`）"，但 v13 已按迭代第 12 轮的审议结论将异常降级方法统一改为 `ExperimentAssignment.createErrorFallback()`（返回 `groupId="experiment-error"` 哨兵分组）。实际设计（§3.4、§4.1、§4.4）已全部更新为 `createErrorFallback()`，但 §5.1 和 §11.1 未同步修订，导致核心错误分类参考表与设计行为不一致。实现者若按 §5.1 实现，将导致实验分流异常的降级数据与正常基线数据不可区分，违背 v13 的核心修复意图。

**所在位置**：§5.1 错误分类表（行 3301）；§11.1 单元测试模式（行 3836）亦有相同问题。

**严重程度**：严重

**改进建议**：将 §5.1 和 §11.1 中的"降级到 default 分组（`new ExperimentAssignment(null, "default", null, null)`）"替换为"使用 `ExperimentAssignment.createErrorFallback()`（`groupId="experiment-error"`）"；同步修正 §11.1 中实验分流异常的测试验证断言。

---

### 问题 2：`DiscussionConclusionCapabilityExecutor` 前置 LLM 压缩调用的线程池资源风险评估不完整

**问题描述**：§3.11.7 定义 `DiscussionConclusionCapabilityExecutor` 在 `doExecuteInternal()` 中通过 `LlmChatService.chat()` 调用前置压缩摘要 LLM（独立超时 15 秒），该调用在 `llmCallExecutor` 线程池的 Worker 线程内执行（`doExecuteInternal()` 运行在 `supplyAsync()` lambda 内部）。这意味着每次讨论结论请求都会在「整体管线端到端超时 90 秒」之外，额外阻塞 llmCallExecutor 线程最多 15 秒。若高并发场景下多个讨论结论请求同时进入，可能导致 llmCallExecutor 线程池饥饿，影响其他能力（分诊、处方审核等）的 LLM 调用。文档仅定义了独立超时阈值和失败回退逻辑，但未评估线程池阻塞风险，也未给出并发度建议或隔离方案。

**所在位置**：§3.11.7 `DiscussionConclusionCapabilityExecutor` 特化设计表；§9.5 YAML 配置中的 `transcript-summary: 15s`；对比 §6.1 线程模型章节。

**严重程度**：重要

**改进建议**：补充前置 LLM 调用的线程模型分析——(a) 评估 llmCallExecutor 线程池在并发讨论结论请求下的阻塞影响；(b) 建议为该调用引入独立线程池（如 `transcriptSummaryExecutor`）或改为异步重试机制，避免阻塞 llmCallExecutor Worker 线程；(c) 在 §9.5 YAML 配置的 `transcript-summary: 15s` 注释中补充线程池隔离说明。

---

### 问题 3：`doDegrade()` 方法 14 参数和 `AiCallRecord` 工厂方法 15~16 参数的可维护性风险

**问题描述**：`doDegrade()` 方法签名包含 14 个参数（`startTime, degradeReason, request, capabilityId, departmentId, callerRole, callerId, visitId, patientId, sessionId, inputSummary, outputSummary, promptVersion, modelId`），`AbstractCapabilityExecutor` 构造器包含 13+ 参数。此外，`AiCallRecord` 的成功/失败/降级三个工厂方法同样包含 15~16 个参数（`callTime, elapsedMs, capabilityId, departmentId, modelId, retryCount, promptTokens, completionTokens, inputSummary, outputSummary, visitId, patientId, sessionId, callerRole, callerId, promptVersion`）。此设计在 v10 迭代中已暴露出调用点参数数量不匹配的编译问题（薄适配器构造器 `super()` 调用缺少参数）。尽管通过详细注释和伪代码逐一标注，但面向实现者时，14 参数的顺序依赖性和构造器/工厂方法的庞大参数列表仍构成显著的实施风险——参数顺序调整、异构调用点遗漏、测试桩参数构造复杂等。此问题不影响设计正确性，但影响编码阶段的交付效率和质量。

**所在位置**：§3.1 AbstractCapabilityExecutor 构造器（行 1324-1344）；§4.1 `doDegrade()` 方法签名（行 3107-3115）；§3.5 `AiCallRecord` 工厂方法签名（行 1988-2004）；§4.1 约 16 处 `doDegrade()` 调用点、§4.1 约 10 处 `AiCallRecord` 工厂方法调用点。

**严重程度**：重要

**改进建议**：考虑抽取 `ExecutionContext` 或 `CallContext` 上下文对象聚合 `departmentId/callerRole/callerId/visitId/patientId/sessionId/inputSummary/outputSummary/promptVersion/modelId` 等参数，将 14 参数方法降维至 6~7 参数。`AiCallRecord` 工厂方法同样可引入 `CallRecordContext` 参数对象。此重构不改变设计语义，仅改善编码体验和可维护性。建议在本文档的"并发设计"章节或设计决策表中记录此重构方向并标记为 Phase 6 或实现期可选优化。

---

### 问题 4：YAML 配置中 `client` 字段到 `ClientType` 枚举的隐式绑定约束未说明

**问题描述**：§9.5 YAML 配置中路由条目使用 `client: "HTTP_API"` 字符串值，对应 Java 枚举 `ClientType.HTTP_API`。Spring Boot 的 `@ConfigurationProperties` 默认通过 `name()` 匹配枚举常量，但 YAML 中使用了字符串全大写写法，而 Spring Boot 的宽松绑定（Relaxed Binding）对枚举类型的处理存在平台差异——某些版本要求枚举值与 YAML 字符串完全匹配（区分大小写），某些版本允许 kebab-case 转换。当前文档未说明此转换约束和回退策略，实现者可能因枚举绑定失败导致启动期异常。

**所在位置**：§9.5 路由配置 YAML（行 3689-3691）；§3.2 `ClientType` 枚举定义（行 447-450）；§3.2 `DelegatingLlmChatService` 分发机制。

**严重程度**：一般

**改进建议**：在 §3.2 `ClientType` 枚举定义或 §9.5 YAML 配置处补充枚举值绑定的转换说明，明确 YAML 中使用全大写枚举名称（`HTTP_API`、`SPRING_AI`），并建议实现层在 `ModelRoute` 的 `clientType` setter 中增加防御性字符串转换（如 `ClientType.valueOf(value.toUpperCase())`）或 `@Converter` 注解。若项目已有统一的枚举转换器，标注引用即可。

---

### 问题 5：`LlmChatRequest` 字段构造方式不一致（构造器 + setter 混用），`tools` 字段易被遗漏

**问题描述**：`LlmChatRequest` 的 `messages`、`options`、`clientType` 三个字段在构造器中一次性赋值（`new LlmChatRequest(messages, options, clientType)`），但核心字段 `tools`（结构化输出的 Tool 定义列表）却通过独立的 setter 方法在构造之后设置（`chatRequest.setTools(...)`）。此构造方式存在以下风险：(1) `tools` 容易被遗忘——实现者在复制伪代码时可能只复制构造器部分而遗漏后续的 setTools 调用；(2) 构造后 `tools` 可能为 null，但 `LlmChatService` 接口契约未定义 `tools` 为 null 时的默认行为（是视为"不使用结构化输出"还是抛出异常？）。对于 downstream consumer 而言，若 `tools` 被遗漏，`structuredChat()` 将因缺少 JSON Schema 定义而退化为非结构化调用，导致下游解析失败。

**所在位置**：§4.1 `doExecuteInternal()` 伪代码（行 2940-2948）；§3.2 `LlmChatRequest` 字段级契约（行 1558）；`LlmChatRequest` 类图（行 455-460）。

**严重程度**：一般

**改进建议**：(1) 建议将 `tools` 纳入 `LlmChatRequest` 构造器参数，改为全参构造 `new LlmChatRequest(messages, options, clientType, tools)`，消除遗漏风险；(2) 若因向后兼容保持当前设计，应在 `LlmChatRequest` 的类注释和 `LlmChatService.structuredChat()` 的契约中显式声明 `tools` 为 null 时的默认行为（如"视为不使用 tool_use/function_call，回退到 JSON mode"）。

---

### 问题 6：`LlmChatService` 实现层与 `DelegatingLlmChatService` 的异常包装策略未定义

**问题描述**：§4.1 伪代码中 `CapabilityExecutor` 的调用路径如下：`llmChatService.structuredChat(chatRequest, outputType)` → `DelegatingLlmChatService.structuredChat()` → `HttpApiLlmChatService.structuredChat()`。按照 §4.1 的异常处理逻辑，`structuredChat()` 调用返回 `CompletableFuture<AiResult<StructuredChatResult<T>>>`，但 `LlmChatService` 接口的 `structuredChat()` 方法签名同时标注为可能抛出 `StructuredOutputNotSupportedException` 和 `LlmInfrastructureException`（`@throws` 文档标注）。这里存在一个异常处理路径的不确定性：`DelegatingLlmChatService` 作为分发层，其 `structuredChat()` 方法是否捕获并重新包装底层实现抛出的异常？若底层抛出 `LlmInfrastructureException`，分发层是否应直接透传还是在 `CompletableFuture` 中包装后再传播？文档未定义此包装契约。对于 downstream consumer（`CapabilityExecutor`），需要在同步异步两套异常路径中都正确捕获异常，存在遗漏风险。

**所在位置**：§3.2 `LlmChatService` 接口契约（行 1356）；§3.2 `DelegatingLlmChatService` 分发机制（行 1392-1401）；§4.1 异常处理伪代码（行 2962-3086）。

**严重程度**：一般

**改进建议**：在 §3.2 `LlmChatService` 接口契约或 `DelegatingLlmChatService` 章节中补充异常传播契约：(a) 声明 `DelegatingLlmChatService` 是否透传底层异常还是包装为 `CompletionException`；(b) 若底层 `Ll mChatService` 实现以同步方式抛出异常（而非通过 `CompletableFuture` 完成），分发层如何捕获和转换；(c) 建议统一约定：所有 LLM 实现层的同步异常均在实现层内部捕获并转换为 `CompletableFuture.completedFuture(AiResult.failure(...))` 或以 `CompletableFuture` 完成异常，确保 `CapabilityExecutor` 仅在 `CompletableFuture` 回调中处理异常。

---

## 三、下游消费者视角评估

### 3.1 AiService 接口契约完整性

`AiService`（ai-api）作为下游业务模块的唯一依赖接口，其契约完整性如下：

| 评估维度 | 状态 | 说明 |
|---------|------|------|
| 请求/响应字段定义 | ✅ 完整 | 13 个方法各自的 DTO 均在 §3.11 特化表中完整定义，字段含 ⊕ 需新增/△ 需改造/✓ 已有标记 |
| 序列化兼容性 | ✅ 有规划 | §3.5 定义了 3 个场景的 Jackson 兼容性验证测试，过渡策略清晰（7 项底座 DTO 先行改造，6 项薄适配器 DTO 暂不改造） |
| 错误码体系 | ✅ 完整 | §3.8 `DegradationReason` 枚举集中管理所有降级原因，§5.1 错误分类表覆盖 11 种错误场景 |
| 版本兼容性 | ✅ 已评估 | §1.6.1 列出 ai-api 的 4 项变更及状态（已有/骨架/新增），变更影响分析详细 |
| 线程安全契约 | ✅ 已定义 | §1.3 核心抽象表补充了 `AiService` 和 `FallbackAiService` 的线程安全约定 |

**结论**：从 AiService 接口维度看，下游消费者可获得完整的契约信息，迁移路径清晰。

### 3.2 核心接口的潜在消费者痛点

**痛点 1（已知）**：`doDegrade()` 和 `AiCallRecord` 工厂方法的参数过多（已作为问题 3 报告）。

**痛点 2（已知）**：`LlmChatRequest` 构造方式不一致（已作为问题 5 报告）。

**痛点 3**：`AiResult<T>` 仅定义了 `success(T)`、`failure(String)`、`degraded(String)` 三个工厂方法。当管线内部发生意外异常时（§4.1 catch 块），调用 `AiResult.failure(errorCode, errorMessage)` 返回。但 `failure()` 的 `errorCode` 参数类型为 `String`，与 `DegradationReason` 枚举无编译期绑定——调用方可能传任意字符串，导致错误码体系在编译期不可检查。

**改进建议**：考虑将 `AiResult.failure()` 的 `errorCode` 参数类型改为 `DegradationReason`（或新增 `AiResult.failure(DegradationReason, String)` 重载），使降级/失败路径在编译期类型安全。此变更不影响现有调用方（新增重载方法，二进制兼容）。

---

## 整体质量评价

产出整体质量高，历经 13 轮迭代后设计成熟度已达到较高的落地准备水平。架构分层清晰、核心抽象职责明确、异常场景覆盖完整、迁移路径和测试策略均已详尽定义。需求响应充分度评估确认所有用户约束均被满足；下游消费者视角评估确认核心接口契约完整。

当前 6 个问题中，1 个严重（§5.1 与设计不一致），1 个重要（前置 LLM 调用线程模型缺口），1 个重要（参数数量影响可维护性），3 个一般（枚举绑定、构造不一致、异常包装契约）。建议优先修复问题 1（确认是 v13 修订遗漏），问题 2、3、5、6 可作为实现期优化项。

---

## 修订说明（v2）

| 质询意见 | 回应 |
|---------|------|
| **质询点1：需求响应充分度审查维度缺失**——报告未独立评估产出是否充分响应了用户需求（完整的OOD要素覆盖 × 风格一致性） | **已修正**。新增「一、需求响应充分度评估」独立章节（§1.1 用户需求逐条对照 + §1.2 整体完整性评价），逐条验证了用户需求的 6 项约束（完整OOD设计、类图、核心职责、协作关系、关键接口、状态模型、风格一致性），结论为全部已覆盖。补充了与Phase0/Phase1ABD风格一致性的对照说明。 |
| **质询点2：接口定义对下游消费者的支撑评估缺失**——报告未从"接口定义是否足以支持下游消费者"视角评估 | **已修正**。新增「三、下游消费者视角评估」独立章节（§3.1 AiService 接口契约完整性 + §3.2 核心接口的潜在消费者痛点），系统评估了请求/响应字段定义、序列化兼容性、错误码体系、版本兼容性、线程安全契约等维度，识别出 1 个新问题（AiResult.failure() 的 errorCode 参数缺少编译期类型安全）作为改进建议。 |
