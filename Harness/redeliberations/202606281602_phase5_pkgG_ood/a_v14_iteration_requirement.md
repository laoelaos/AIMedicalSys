根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 问题 1（严重）：§5.1 错误分类表"实验分流异常"处理方式与 v13 实际设计矛盾

**描述**：§5.1 错误分类表「实验分流异常」行处理方式描述为降级到 default 分组（`new ExperimentAssignment(null, "default", null, null)`），但 v13 已按第 12 轮审议结论统一改为 `ExperimentAssignment.createErrorFallback()`（`groupId="experiment-error"` 哨兵分组）。实际设计（§3.4、§4.1、§4.4）已全部更新，但 §5.1 和 §11.1 未同步修订。

**所在位置**：§5.1 错误分类表（行 3301）；§11.1 单元测试模式（行 3836）

**改进建议**：将 §5.1 和 §11.1 的相关描述替换为 `ExperimentAssignment.createErrorFallback()`（`groupId="experiment-error"`）；同步修正 §11.1 中实验分流异常的测试验证断言。

---

### 问题 2（重要）：`DiscussionConclusionCapabilityExecutor` 前置 LLM 压缩调用的线程池资源风险评估不完整

**描述**：§3.11.7 中 `doExecuteInternal()` 通过 `LlmChatService.chat()` 调用前置压缩摘要 LLM（独立超时 15 秒），该调用在 `llmCallExecutor` 线程池的 Worker 线程内执行（`doExecuteInternal()` 运行在 `supplyAsync()` lambda 内部）。每次讨论结论请求会额外阻塞 llmCallExecutor 线程最多 15 秒。高并发下可能导致 llmCallExecutor 线程池饥饿，影响其他能力的 LLM 调用。

**所在位置**：§3.11.7 `DiscussionConclusionCapabilityExecutor` 特化设计表；§9.5 YAML 配置中的 `transcript-summary: 15s`；对比 §6.1 线程模型章节

**改进建议**：补充前置 LLM 调用的线程模型分析——(a) 评估 llmCallExecutor 线程池在并发讨论结论请求下的阻塞影响；(b) 建议为该调用引入独立线程池（如 `transcriptSummaryExecutor`）或改为异步重试机制；(c) 在 §9.5 YAML 配置的 `transcript-summary: 15s` 注释中补充线程池隔离说明。

---

### 问题 3（重要）：`doDegrade()` 方法 14 参数和 `AiCallRecord` 工厂方法 15~16 参数的可维护性风险

**描述**：`doDegrade()` 包含 14 个参数，`AbstractCapabilityExecutor` 构造器包含 13+ 参数，`AiCallRecord` 工厂方法包含 15~16 个参数。参数顺序依赖性和调用点复杂度构成编码实施风险。

**所在位置**：§3.1 构造器（行 1324-1344）；§4.1 `doDegrade()` 方法签名（行 3107-3115）；§3.5 工厂方法签名（行 1988-2004）

**改进建议**：考虑抽取 `ExecutionContext` 或 `CallContext` 上下文对象聚合通用参数，将 14 参数方法降维至 6~7 参数。`AiCallRecord` 工厂方法同样可引入 `CallRecordContext` 参数对象。建议在本文档中记录此重构方向并标记为 Phase 6 或实现期可选优化。

---

### 问题 4（一般）：YAML 配置中 `client` 字段到 `ClientType` 枚举的隐式绑定约束未说明

**描述**：§9.5 YAML 配置中路由条目使用 `client: "HTTP_API"` 字符串值，Spring Boot 的 Relaxed Binding 对枚举类型的处理存在平台差异，当前文档未说明此转换约束和回退策略。

**所在位置**：§9.5 路由配置 YAML（行 3689-3691）；§3.2 `ClientType` 枚举定义（行 447-450）

**改进建议**：在 §3.2 `ClientType` 枚举定义或 §9.5 YAML 配置处补充枚举值绑定的转换说明，建议实现层在 `ModelRoute` 的 `clientType` setter 中增加防御性字符串转换或 `@Converter` 注解。

---

### 问题 5（一般）：`LlmChatRequest` 字段构造方式不一致，`tools` 字段易被遗漏

**描述**：`LlmChatRequest` 的 `messages`、`options`、`clientType` 在构造器中赋值，但核心字段 `tools` 通过独立 setter 设置。`tools` 易被遗漏导致结构化调用退化为非结构化。

**所在位置**：§4.1 伪代码（行 2940-2948）；§3.2 字段级契约（行 1558）；类图（行 455-460）

**改进建议**：(1) 将 `tools` 纳入构造器改为全参构造；(2) 若保持当前设计，在类注释和 `structuredChat()` 契约中显式声明 `tools` 为 null 时的默认行为。

---

### 问题 6（一般）：`LlmChatService` 实现层与 `DelegatingLlmChatService` 的异常包装策略未定义

**描述**：`DelegatingLlmChatService` 作为分发层，其异常处理路径存在不确定性：底层异常是否透传或包装后再传播未定义，`CapabilityExecutor` 存在同步/异步两套异常路径中遗漏捕获的风险。

**所在位置**：§3.2 `LlmChatService` 接口契约（行 1356）；§3.2 `DelegatingLlmChatService` 分发机制（行 1392-1401）；§4.1 异常处理伪代码（行 2962-3086）

**改进建议**：在 §3.2 补充异常传播契约，统一约定所有 LLM 实现层异常在内部捕获并转为 `CompletableFuture` 完成，确保 `CapabilityExecutor` 仅在回调中处理异常。

## 历史迭代回顾

### 已解决的问题（出现在历史反馈中但当前反馈不再提及）

| 迭代轮次 | 问题 | 解决方式 |
|---------|------|---------|
| 第 12 轮 | 薄适配器能力 AiCallRecord 中 modelId/promptVersion/retryCount 三个字段永远为空，近半数 AI 能力性能分析无法跨模型聚合 | v13 新增 §1.4.1 薄适配器性能观测维度缺口，系统分析了三个缺失维度的影响并给出了补齐时间线 |
| 第 12 轮 | 实验分流异常与无实验命中两种场景的 assignment 返回值语义重叠 | v13 新增 `ExperimentAssignment.createErrorFallback()` 及哨兵 `groupId="experiment-error"`，§4.1 实验分流异常 catch 块改用哨兵分组 |
| 第 12 轮 | execution.timeout 等关键运行时配置不支持热加载 | v13 新增 §3.9 RefreshScope 热加载追踪、§11 配置热刷新集成测试 |
| 第 12 轮 | ExperimentGroup.percentage 千分比与常见百分比表述不一致 | v13 补充千分比设计决策理由 |

### 持续存在的问题（在多轮反馈中反复出现，需重点解决）

- **问题 1**（严重）：§5.1 与 v13 设计矛盾——第 12 轮已提出实验分流异常语义重叠问题并在 v13 修复了 §3.4/§4.1/§4.4，但第 13 轮诊断发现 §5.1 和 §11.1 未同步更新，属于修订遗漏。需在本轮补齐。
- **问题 2**（重要）：前置 LLM 调用线程池饥饿——第 3 轮（§3.11.4 transcriptSummary 预处理 LLM 调用未定义超时和失败策略）→ 第 12 轮在 YAML 中补充了超时配置 → 第 13 轮评估为线程池饥饿风险仍未解决。需补充线程模型分析。
- **问题 3**（重要）：参数过多可维护性风险——第 10 轮暴露出薄适配器 `super()` 调用参数数量不匹配的编译问题，第 13 轮进一步确认 14~16 参数的调用复杂度。此问题对编码质量影响持续存在。
- **问题 4**（一般）：枚举绑定约束——首次在第 13 轮出现，属较轻微的设计缺口。
- **问题 5**（一般）：tools 构造不一致——首次在第 7 轮（类图缺失 tools 字段）→ 第 13 轮评估为构造方式不一致。问题经过演变但核心风险未完全消除。
- **问题 6**（一般）：异常包装策略——首次在第 13 轮出现，属新识别的契约定义缺口。

### 新发现的问题

本轮无全新发现的问题——全部 6 个问题均在第 13 轮历史反馈中已记录。问题 2 的严重程度从历史记录的"一般"升级为"重要"，反映对该问题影响的认知深化。

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281602_phase5_pkgG_ood\a_v13_copy_from_v12.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281602_phase5_pkgG_ood\requirement.md
