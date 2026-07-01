# v24 质量审查报告

## 审查概要

审查范围：Phase 5 包 G — AI 进阶底座 架构级 OOD 设计方案 v24
审查视角：从实际落地（编码实现）视角评估
审查重点：事实一致性、编译可行性、上下位矛盾、关键遗漏

整体评价：v24 完整吸纳了 v23 审查的 6 项诊断结论，修订说明证实了每项问题的消解方案。核心抽象定义与伪代码覆盖度达到较高成熟度。以下发现聚焦于新轮次中识别的新问题。

---

## 发现问题

### 1. chat() 回退路径中 retryCount 和 tokenUsage 引用不存在的 AiResult/LlmChatResponse 方法

**描述**：§4.1 `doExecuteInternal()` 伪代码（第 2327-2329 行）在 `StructuredOutputNotSupportedException` catch 分支中：

```
chatResponse = llmChatService.chat(chatRequest)
retryCount = chatResponse.getRetryCount()
tokenUsage = chatResponse.getUsage()
```

`LlmChatService.chat()` 返回类型为 `CompletableFuture<AiResult<LlmChatResponse>>`（§3.2 类图第 312 行）。但：
- `AiResult` 在 §1.3 第 96 行标注"不变"，无 `getRetryCount()` 方法
- `AiResult` 同样无 `getUsage()` 方法
- `LlmChatResponse` 字段契约（§3.2 第 1345-1348 行）仅含 `content`、`usage`、`modelId`，无 `retryCount` 字段

`LlmChatResponse` 虽有 `usage` 字段（第 1347 行），但伪代码中 `chatResponse` 的类型语义为 `AiResult<LlmChatResponse>`（若遵循 chat() 返回类型），需通过 `getData().getUsage()` 或 `getData().usage` 导航而非直接在 `AiResult` 上调用 `getUsage()`。对比 `structuredChat()` 的成功路径（第 2320-2324 行）可发现，`structuredResult` 被作为 `StructuredChatResult<T>` 使用（而非 `AiResult<StructuredChatResult<T>>`），成功路径的 `getData()`/`getRetryCount()`/`getUsage()` 属于 `StructuredChatResult<T>` 的合法方法。chat() 回退路径采用了同样的伪代码约定但 `LlmChatResponse` 类缺少 `retryCount` 字段。

**根因**：`StructuredChatResult<T>` 专为 `structuredChat()` 设计以携带 retryCount/usage，但 `chat()` 的返回值 `AiResult<LlmChatResponse>` 没有对应的重试计数载体。管线中共用变量 `retryCount`（第 2310 行声明 `retryCount = 0`）在 chat() 回退路径中无法从返回值获取真实值。

**位置**：§4.1 doExecuteInternal() 第 2327-2329 行；§3.2 LlmChatResponse 字段契约第 1345-1348 行；§3.2 AiResult 类型定位 §1.3 第 96 行

**严重程度**：严重

**改进建议**：以下方案择一：

- **方案(a)**（推荐）：在 `LlmChatResponse` 中新增 `retryCount` 字段（int，默认 0），`LlmChatService.chat()` 实现层在内部重试后填充此字段。这样 chat 回退路径可通过 `chatResponse.getRetryCount()` 获取重试计数，与 `$LlmChatResponse.usage` 的导航路径一致。同步更新 §2.3 类图 `LlmChatResponse` 的字段定义。

- **方案(b)**：明确约定 chat() 回退路径中 `retryCount` 恒为 0（`LlmChatService.chat()` 实现层的内部重试不对外暴露），将第 2328 行改为 `retryCount = 0`（或去掉赋值，保持第 2310 行的初始化值）。同时在 §4.1 注释中记录此限制的理由——"chat() 未暴露重试计数，回退路径下 retryCount 始终为 0"。

无论选择哪一方案，第 2329 行 `tokenUsage = chatResponse.getUsage()` 需要调整：若 `chatResponse` 按伪代码约定视为 `LlmChatResponse`（而非 `AiResult<LlmChatResponse>`），则应使用 `chatResponse.getUsage()` 或 `chatResponse.usage`（取决于 getter 命名）。建议在伪代码中统一导航路径，与 `structuredResult.getUsage()` 风格一致。若 `chatResponse` 视为 `AiResult<LlmChatResponse>`，则应改为 `chatResponse.getData() != null ? chatResponse.getData().getUsage() : null`。当前 `getUsage()` 在 `LlmChatResponse` 上有合理对应（第 1347 行 `usage` 字段），但建议在修订说明中明确约定伪代码中 `chatResponse` 的类型语义。

---

### 2. AbstractCapabilityExecutor 类图缺少构造器注入的核心依赖字段

**描述**：§2.3 类图中 `AbstractCapabilityExecutor`（第 251-265 行）仅列示了 `inputType`、`capabilityTimeoutConfig`、`thinAdapterTimeout` 三个字段，以及一组方法签名。但 `doExecuteInternal()` 伪代码（第 2243-2361 行）实际使用了以下通过构造器注入的依赖字段，且这些字段在 §3.1 构造器伪代码（第 1076-1096 行）中明确列出：

| 伪代码引用的变量名 | 来源（§3.1 构造器） |
|---|---|
| `experimentManager` | 构造器参数注入 |
| `promptTemplateManager` | 构造器参数注入 |
| `modelRouter` | 构造器参数注入 |
| `llmChatService` | 构造器参数注入 |
| `structuredOutputParser` | 构造器参数注入 |
| `endpointHealthManager` | 构造器参数注入 |
| `metricsCollector` | 构造器参数注入 |
| `slidingWindowMetricsStore` | AiOrchestrator 协作对象 + CapabilityExecutor 构造器参数 |
| `localRuleFallback` | 构造器参数注入 |

类图仅通过 `CapabilityExecutor` 的 uses-association（第 606-616 行）间接表达了部分依赖关系，但未在 `AbstractCapabilityExecutor` 的类图字段区中体现。类图作为 OOD 设计的可视化核心载体，字段列表的不完整使得实现者无法从类图获取 `AbstractCapabilityExecutor` 的完整依赖视图。

**位置**：§2.3 类图 AbstractCapabilityExecutor 第 251-265 行；§3.1 构造器伪代码第 1076-1096 行；§4.1 doExecuteInternal() 第 2243-2361 行多处引用

**严重程度**：重要

**改进建议**：在 §2.3 类图 `AbstractCapabilityExecutor` 中补充以下 protected/private 字段（使用 `#` 或 `-` 前缀）：

```
#ExperimentManager experimentManager
#PromptTemplateManager promptTemplateManager
#ModelRouter modelRouter
#LlmChatService llmChatService
#StructuredOutputParser structuredOutputParser
#ModelEndpointHealthManager endpointHealthManager
#AiMetricsCollector metricsCollector
#SlidingWindowMetricsStore metricsStore
#LocalRuleFallback~T,R~ localRuleFallback
```

此类图中的字段声明与 §3.1 构造器列表和 §4.1 伪代码引用保持一致，使实现者在一个视图中即可了解 `AbstractCapabilityExecutor` 的完整结构。

---

### 3. doDegrade() 中 localRuleFallback 成功路径的 outputSummary 提取精度不足

**描述**：§4.1 `doDegrade()` 方法（第 2386 行）在 `localRuleFallback` 成功路径中计算 `outputSummary` 时：

```
outputSummary = outputSummary != null ? outputSummary : StringUtils.truncate(result.toString(), 500)
```

`result` 为 `localRuleFallback.fallback(request)` 的返回值，类型为 `AiResult<R>`（§2.3 类图第 574 行）。`AiResult.toString()` 输出的是 AiResult 封装层的信息（成功/失败状态、消息等），而非内部业务响应数据的具体内容。`outputSummary` 在设计语义上（§3.5 AiCallRecord 字段表第 1642 行 "输出摘要"）应反映实际业务输出的概要信息，而非 AiResult 封装层的状态文本。

**位置**：§4.1 doDegrade() 第 2386 行；§2.3 类图 LocalRuleFallback 返回类型第 574 行；§3.5 AiCallRecord 字段表第 1642 行

**严重程度**：轻微

**改进建议**：将第 2386 行改为提取 AiResult 内部数据的内容：

```
outputSummary = outputSummary != null ? outputSummary :
    (result.getData() != null ? StringUtils.truncate(result.getData().toString(), 500) : StringUtils.truncate(result.toString(), 500))
```

同步更新 §3.1 模板方法伪代码中 `extractOutputSummary()` 默认实现描述（第 1027-1029 行），使其在处理 `AiResult<R>` 时优先提取内部数据 toString() 而非封装层 toString()。

---

DIAG_WRITTEN:C:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606271627_phase5_pkgG_ood/b_v24_diag_v1.md
主Agent请勿阅读产出文件内容，直接将路径转发给相关方。
