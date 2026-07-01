# 质量审查报告 — Phase 5 包 G OOD 设计（v13）

## 审查范围
- 待审查产出：`a_v13_copy_from_v12.md`
- 审查视角：实际落地评估，侧重需求响应充分度、整体深度和完整性、事实错误和逻辑矛盾
- 已排除维度：组件A内部审议已覆盖的技术可行性等维度

---

## 发现的问题

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

### 问题 3：`doDegrade()` 方法 14 参数和 `AbstractCapabilityExecutor` 构造器 13+ 参数的实现可靠性风险

**问题描述**：`doDegrade()` 方法签名包含 14 个参数（`startTime, degradeReason, request, capabilityId, departmentId, callerRole, callerId, visitId, patientId, sessionId, inputSummary, outputSummary, promptVersion, modelId`），`AbstractCapabilityExecutor` 构造器包含 13+ 参数（含 `@Autowired(required = false)` 和多个 `@Qualifier` 注入）。此设计在 v10 迭代中已暴露出调用点参数数量不匹配的编译问题（薄适配器构造器 `super()` 调用缺少参数）。尽管已在本文档中通过详细注释和伪代码逐一标注，但面向实现者时，14 参数的顺序依赖性和构造器的庞大参数列表仍构成显著的实施风险——参数顺序调整、异构调用点遗漏、测试桩参数构造复杂等。此问题不影响设计正确性，但影响编码阶段的交付效率和质量。

**所在位置**：§3.1 AbstractCapabilityExecutor 构造器（行 1324-1344）；§4.1 `doDegrade()` 方法签名（行 3107-3115）；§4.1 调用点（行 2854、2869、2995、3011、3020、3042、3045、3055、3068、3072、3086、3093 等约 16 处）。

**严重程度**：重要

**改进建议**：考虑抽取 `ExecutionContext` 或 `CallContext` 上下文对象聚合 `departmentId/callerRole/callerId/visitId/patientId/sessionId/inputSummary/outputSummary/promptVersion/modelId` 等参数，将 14 参数方法降维至 6~7 参数。此重构不改变设计语义，仅改善编码体验和可维护性。建议在本文档的"并发设计"章节或设计决策表中记录此重构方向并标记为 Phase 6 或实现期可选优化。

---

### 问题 4：YAML 配置中 `client` 字段到 `ClientType` 枚举的隐式绑定约束未说明

**问题描述**：§9.5 YAML 配置中路由条目使用 `client: "HTTP_API"` 字符串值，对应 Java 枚举 `ClientType.HTTP_API`。Spring Boot 的 `@ConfigurationProperties` 默认通过 `name()` 匹配枚举常量，但 YAML 中使用了字符串全大写写法，而 Spring Boot 的宽松绑定（Relaxed Binding）对枚举类型的处理存在平台差异——某些版本要求枚举值与 YAML 字符串完全匹配（区分大小写），某些版本允许 kebab-case 转换。当前文档未说明此转换约束和回退策略，实现者可能因枚举绑定失败导致启动期异常。

**所在位置**：§9.5 路由配置 YAML（行 3689-3691）；§3.2 `ClientType` 枚举定义（行 447-450）；§3.2 `DelegatingLlmChatService` 分发机制。

**严重程度**：一般

**改进建议**：在 §3.2 `ClientType` 枚举定义或 §9.5 YAML 配置处补充枚举值绑定的转换说明，明确 YAML 中使用全大写枚举名称（`HTTP_API`、`SPRING_AI`），并建议实现层在 `ModelRoute` 的 `clientType` setter 中增加防御性字符串转换（如 `ClientType.valueOf(value.toUpperCase())`）或 `@Converter` 注解。若项目已有统一的枚举转换器，标注引用即可。

---

## 整体质量评价

产出整体质量较高，历经 12 轮迭代后设计成熟度已达到较高的落地准备水平。架构分层清晰（编排层 → 管线层 → LLM 调用层），核心抽象职责明确，异常场景覆盖完整，迁移路径和测试策略均已详尽定义。上述 4 个问题中 2 个为严重级别（§5.1 与设计不一致、前置 LLM 调用线程模型缺口），1 个重要（参数过多影响可维护性），1 个一般（枚举绑定约束说明）。建议优先修复问题 1（已确认是 v13 修订遗漏），其余问题可作为 Phase 6 或实现期优化项。
