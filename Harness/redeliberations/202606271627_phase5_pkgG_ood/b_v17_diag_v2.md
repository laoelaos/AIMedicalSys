# v17 质量审查报告

## 审查范围

审查文件：`a_v17_copy_from_v16.md`（以下简称"产出"）
审查维度：需求响应充分度、事实正确性、逻辑一致性、深度与完整性
审查依据：`requirement.md`、`diag.md`（外部诊断 v16→v17 修复基准）、`iteration_history.md`

---

## 外部诊断（diag.md）7 项问题的修复评估

| 问题 | 严重程度 | 修复状态 | 说明 |
|------|---------|---------|------|
| P1: AiPlatformConfig 核心配置类缺失 | 严重 | ✓ 已修复 | §3.9 新增完整定义，含 @Bean 方法签名、EnvironmentPostProcessor 实现 |
| P2: LlmClient 未采纳 v5 LlmChatService | 严重 | ✓ 已修复 | §3.2 重写为 LlmChatService（chat + structuredChat）+ LlmChatStreamService，配套 DTO 全部新增 |
| P3: 线程池 @Bean 定义缺失 | 重要 | ✓ 已修复 | §3.9 给出 llmCallExecutor 和 metricsAsyncExecutor 的 @Bean 定义伪代码 |
| P4: handle() 与 13 方法映射关系未显式定义 | 重要 | ✓ 已修复 | §4.1 新增注释块，明确委托模式 |
| P5: 薄适配器 doExecuteInternal() 仅在 §3.1 | 重要 | ✓ 已修复 | §4.2 新增薄适配器特化管线伪代码 |
| P6: AiOrchestrator 持有 ModelEndpointHealthManager 但未使用 | 重要 | ✓ 已修复 | 从 AiOrchestrator 移除，下沉到 CapabilityExecutor.doExecuteInternal() |
| P7: extractHeader() 命名不一致 | 一般 | ✓ 已修复 | §3.10 新增 extractFromRequestContext() 统一实现 |

**结论**：外部诊断指出的 7 项问题在 v17 中已全部修复，产出对用户需求（特别是 diag.md 中提出的修复要求）的响应是充分的。

---

## 审查发现问题

### 问题 1【严重，事实错误】§4.1 `doExecuteInternal()` 中 `chatResponse` 和 `retryCount` 变量在 `structuredChat` 成功路径中未定义

- **描述**：`doExecuteInternal()` 伪代码（§4.1 完整管线实现）中，LLM 调用部分使用 try-catch 区分两条路径：`structuredChat()` 优先路径（try 块）和 `chat()` + `parse()` 回退路径（catch 块）。但 `chatResponse`（line ~1865）和 `retryCount`（line ~1866）仅在 catch 块内定义，而 try-catch 之后的公共指标采集代码（line ~1876-1878）引用这两个变量：
  ```
  tokenUsage = chatResponse != null ? chatResponse.getUsage() : null
  metricsCollector.record(AiCallRecord.success(..., tokenUsage != null ? retryCount : 0, ...))
  ```
  当 `structuredChat()` 成功返回（try 块正常完成）时，`chatResponse` 和 `retryCount` 均未定义，构成变量引用错误。

- **位置**：§4.1（以文件行号约 1862-1878 区域），是 `doExecuteInternal()` 完整管线伪代码中 LLM 调用及后续指标采集部分。

- **严重程度**：严重 — 伪代码中的变量作用域错误将导致实现者在编码时困惑，或产出与实际可执行代码不一致。

- **改进建议**：
  1. 重构伪代码，将 `chatResponse` 和 `retryCount` 的定义移至 try 块之前（初始化为 null/0），在成功路径和回退路径中分别赋值。
  2. 或为 `structuredChat()` 设计返回值包裹重试计数（如 `StructuredChatResult<T>` 含 `retryCount`），使成功路径也能获取重试信息。

### 问题 2【重要，设计缺口】`structuredChat` 成功路径无法获取 Token 用量和重试计数

- **描述**：`structuredChat(LlmChatRequest, Class<T>)` 返回 `T`（解析后的 DTO），不包含 `retryCount` 或 `tokenUsage` 等元数据。当 `structuredChat()` 直接成功（不走 `chat()` + `parse()` 回退路径）时，伪代码中后续的 `AiCallRecord.success()` 工厂方法调用的 `tokenUsage` 和 `retryCount` 参数无从获取。当前伪代码通过 catch 块中 `chatResponse`（来自 `chat()` 的 `LlmChatResponse`）获取这些值，但该路径仅在 `structuredChat` 失败时执行。

  **后果**：`structuredChat` 成功场景下，指标记录的 Token 用量和重试次数将恒为 null/0，丢失关键可观测性数据。

- **位置**：§4.1 `doExecuteInternal()` 伪代码 LLM 调用段（约 line 1859-1883）。

- **严重程度**：重要 — 影响 A/B 实验效果分析（Token 用量缺失）和运维排障（重试计数缺失）。

- **改进建议**：
  1. 将 `structuredChat()` 的返回值从裸 `T` 修改为包装类型 `StructuredChatResult<T>`（含 `T data`、`int retryCount`、`LlmChatUsage usage`），使其同时携带解析后的 DTO 与调用元数据。在 §3.2 `LlmChatService` 接口方法签名中同步更新。
  2. 或规定 `structuredChat()` 作为模型原生结构化模式时，Token 用量从底层响应中提取注入结果对象；重试计数由 `LlmChatService` 实现内部填充后以元数据形式返回。
  3. 同步更新 §2.3 类图中 `LlmChatService.structuredChat()` 的返回类型，并在 §4.1 伪代码中体现获取路径。

### 问题 3【一般，完整性】§1.3 核心抽象表遗漏 `CredentialProvider` 和 `EndpointRateLimiter`

- **描述**：`CredentialProvider`（§3.2，约 line 1172-1203）和 `EndpointRateLimiter`（§3.2，约 line 980-988）是模型对接层的重要组件，在 §3.2 中有完整定义，但 §1.3 核心抽象一览表中未列出，读者无法从概览章节建立完整组件认知。

- **位置**：§1.3 核心抽象表（约 line 44-72）。

- **严重程度**：一般 — 不影响设计正确性，但降低文档的导航效率和完整性。

- **改进建议**：在 §1.3 中补充 `CredentialProvider`（凭据查询接口）、`EndpointRateLimiter`（端点限流器）两行。

### 问题 4【一般，完整性不足】`LlmChatOptions` 与 `ModelRoute.parameters` 的字段映射覆盖逻辑未定义

- **描述**：`ModelRoute.parameters`（`Map<String, Object>`，包含 temperature、maxTokens 等 LLM 生成参数）和 `LlmChatOptions`（强类型字段，含 temperature、maxTokens 等）两者均声明为 LLM 调用参数的来源。§3.2 仅说明 "`LlmChatOptions` 构造时从此 Map 提取参数值设置到对话配置"，但未定义：
  - 哪些 Map key 映射到哪个 `LlmChatOptions` 字段？
  - 当 `LlmChatOptions` 构造后，调用方又在 `options` 上设置了同名字段时，谁的优先级更高？
  - 是否支持合并（route.parameters 提供低优先级默认值，options 提供高优先级覆盖值）？

- **位置**：§3.2 `ModelRoute` 字段扩展表（约 line 1163-1170）和 `LlmChatOptions` 定义段（约 line 1126-1131）。

- **严重程度**：一般 — 实现者可根据常见模式推断，但引入歧义和实现不一致的风险。

- **改进建议**：在 §3.2 中明确两阶段填充策略（或合并规则）：
  - 方案 A：`ModelRoute.parameters` 作为配置源，`LlmChatService` 实现构造 `LlmChatOptions` 时从此 Map 提取标准 key 填充字段，`CapabilityExecutor` 传入的 `options` 覆盖已填充值。
  - 方案 B：`ModelRoute.parameters` 中的字段作为 `LlmChatService` 的底层参数，直接设置到 HTTP 请求体/Spring AI 配置中，不经过 `LlmChatOptions`。
  - 在 §4.1 伪代码第 1857 行的 `new LlmChatOptions(...)` 部分体现最终的定义。

### 问题 5【一般，完整性】`PromptTemplateManager.getFallbackPrompt()` 的返回值约束未定义

- **描述**：`getFallbackPrompt(capabilityId)` 在模板渲染异常时作为兜底使用，§3.3 提到其为"能力内硬编码兜底 Prompt"，但未定义：
  - 返回值是否可能为 null？
  - 返回的兜底 Prompt 是否与正常渲染后的 Prompt 格式一致（即后续 LLM 调用是否无需区分来源）？
  - 各能力是否都必须实现自己的兜底 Prompt？

- **位置**：§3.3 `PromptTemplateManager` 职责描述（约 line 1230）。

- **严重程度**：一般 — 实现者可能做出不同假设，导致测试遗漏。

- **改进建议**：在 §3.3 或 §4.5 渲染契约中补充：`getFallbackPrompt()` 返回值约定为非 null 非空字符串，格式与 `render()` 正常输出一致（但不含 DTO 变量注入）；各能力兜底 Prompt 作为配置项管理而非硬编码到 `PromptTemplateManager` 实现中。

---

## 整体质量评价

v17 产出对外部诊断 7 项问题的修复是完整且充分的，文档在完整性、一致性和深度上相比 v16 有显著提升。核心架构决策明确，接口定义清晰，异常场景已基本覆盖。存在的主要问题集中在：
1. **伪代码中 `structuredChat` 成功路径的变量作用域错误**（问题 1）和**设计层面的 Token 用量/重试计数获取缺口**（问题 2）——这两个问题相互关联，建议在修复时一并进行设计补全。
2. **概览层面组件列表的完整性**（问题 3-5）属于组织层面的改进项目。

以上 5 个问题修复后，产出将可直接指导编码实现。

---

## 修订说明（v2）

| 质询意见 | 回应 |
|---------|------|
| 问题 3（版本号不一致）被标记为"重要"严重程度，属于不必要的细节问题。quality-challenger 指令明确要求"关注任务，不要陷入不必要的细节"，并列举"文档版本号等与任务描述关系不大的细节"作为不应关注的示例。建议移除问题 3 或降级。 | **已采纳**。移除原问题 3（版本号不一致），其余问题编号前移对齐。该问题确属文档管理流程事项，与 OOD 设计质量的核心审查维度（接口定义、异常场景、可落地性等）无关，以"重要"级别呈现会误导作者精力分配。 |
