根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 问题 1【严重，事实错误】§4.1 `doExecuteInternal()` 中 `chatResponse` 和 `retryCount` 变量在 `structuredChat` 成功路径中未定义

- **描述**：`doExecuteInternal()` 伪代码（§4.1 完整管线实现）中，LLM 调用部分使用 try-catch 区分两条路径：`structuredChat()` 优先路径（try 块）和 `chat()` + `parse()` 回退路径（catch 块）。但 `chatResponse` 和 `retryCount` 仅在 catch 块内定义，而 try-catch 之后的公共指标采集代码引用这两个变量。当 `structuredChat()` 成功返回时，`chatResponse` 和 `retryCount` 均未定义，构成变量引用错误。

- **改进建议**：
  1. 重构伪代码，将 `chatResponse` 和 `retryCount` 的定义移至 try 块之前（初始化为 null/0），在成功路径和回退路径中分别赋值。
  2. 或为 `structuredChat()` 设计返回值包裹重试计数（如 `StructuredChatResult<T>` 含 `retryCount`），使成功路径也能获取重试信息。

### 问题 2【重要，设计缺口】`structuredChat` 成功路径无法获取 Token 用量和重试计数

- **描述**：`structuredChat(LlmChatRequest, Class<T>)` 返回 `T`（解析后的 DTO），不包含 `retryCount` 或 `tokenUsage` 等元数据。当 `structuredChat()` 直接成功时，后续 `AiCallRecord.success()` 工厂方法调用的 `tokenUsage` 和 `retryCount` 参数无从获取。后果是 `structuredChat` 成功场景下指标记录的 Token 用量和重试次数恒为 null/0，丢失关键可观测性数据。

- **改进建议**：
  1. 将 `structuredChat()` 的返回值从裸 `T` 修改为包装类型 `StructuredChatResult<T>`（含 `T data`、`int retryCount`、`LlmChatUsage usage`），在 §3.2 `LlmChatService` 接口方法签名中同步更新。
  2. 同步更新 §2.3 类图中 `LlmChatService.structuredChat()` 的返回类型，并在 §4.1 伪代码中体现获取路径。

### 问题 3【一般，完整性】§1.3 核心抽象表遗漏 `CredentialProvider` 和 `EndpointRateLimiter`

- **描述**：`CredentialProvider`（§3.2）和 `EndpointRateLimiter`（§3.2）是模型对接层的重要组件，在 §3.2 中有完整定义，但 §1.3 核心抽象一览表中未列出。

- **改进建议**：在 §1.3 中补充 `CredentialProvider`（凭据查询接口）、`EndpointRateLimiter`（端点限流器）两行。

### 问题 4【一般，完整性不足】`LlmChatOptions` 与 `ModelRoute.parameters` 的字段映射覆盖逻辑未定义

- **描述**：`ModelRoute.parameters`（`Map<String, Object>`，含 temperature、maxTokens 等）和 `LlmChatOptions`（强类型字段）两者均声明为 LLM 调用参数的来源。§3.2 仅说明 "`LlmChatOptions` 构造时从此 Map 提取参数值设置到对话配置"，但未定义：哪些 Map key 映射到哪个 `LlmChatOptions` 字段？构造后调用方又在 `options` 上设置了同名字段时优先级如何？是否支持合并？

- **改进建议**：在 §3.2 中明确两阶段填充策略（或合并规则），在 §4.1 伪代码第 1857 行的 `new LlmChatOptions(...)` 部分体现最终的定义。

### 问题 5【一般，完整性】`PromptTemplateManager.getFallbackPrompt()` 的返回值约束未定义

- **描述**：`getFallbackPrompt(capabilityId)` 在模板渲染异常时作为兜底使用，§3.3 提到其为"能力内硬编码兜底 Prompt"，但未定义返回值是否可能为 null、返回的兜底 Prompt 是否与正常渲染后的 Prompt 格式一致、各能力是否都必须实现自己的兜底 Prompt。

- **改进建议**：在 §3.3 或 §4.5 渲染契约中补充：`getFallbackPrompt()` 返回值约定为非 null 非空字符串，格式与 `render()` 正常输出一致（但不含 DTO 变量注入）；各能力兜底 Prompt 作为配置项管理而非硬编码到 `PromptTemplateManager` 实现中。

## 历史迭代回顾

### 已解决的问题（出现在历史反馈但当前反馈中不再提及的问题）
- **外部诊断 7 项问题全部修复**：AiPlatformConfig 核心配置类定义（v15 -> P1）、LlmClient 替换为 LlmChatService（v15 -> P2）、线程池 @Bean 定义（v15 -> P3）、handle() 与 13 方法映射关系（v15 -> P4）、薄适配器 doExecuteInternal()（v15 -> P5）、ModelEndpointHealthManager 从 AiOrchestrator 移除（v15 -> P6）、extractHeader() 命名统一（v15 -> P7）
- **doDegrade() 参数完整性问题**（v13 问题 2）：已通过签名扩展解决
- **防御性拷贝变量重赋值问题**（v14 问题 1）：已修复
- **薄适配器提取方法命名不一致**（v14 问题 2）：已统一
- **Maven 依赖作用域矛盾**（v14 问题 3）：已统一为 provided
- **ModelRoute 缺少 parameters 字段**（v14 问题 4）：已补充
- **AiPlatformConfig 类图缺失**（v15 问题 1）：已补充
- **线程池 @Bean 定义缺失**（v15 问题 2）：已补充
- **handle() 映射关系未显式定义**（v15 问题 3）：已补充注释块
- **薄适配器行为仅在 §3.1**（v15 问题 4）：§4.2 已新增特化伪代码
- **AiOrchestrator 持有未使用的 ModelEndpointHealthManager**（v15 问题 5）：已移除
- **extractHeader() 命名不一致**（v15 问题 6）：已统一

### 持续存在的问题（在多轮反馈中反复出现的问题，需重点解决）
- **Token 用量和重试计数的可观测性缺口**：自 v9（TokenUsage 未建模）→ v11（LlmResponse 缺少 retryCount）→ v12（doDegrade 缺少 promptVersion）→ 当前问题 1 + 2。structuredChat 返回值的元数据承载能力始终未从设计层面解决，本轮问题 2 从 API 签名层面暴露了根本原因。建议本轮彻底修复：引入 `StructuredChatResult<T>` 包装类型，一劳永逸地解决 retryCount 和 tokenUsage 在成功路径的获取问题。
- **伪代码变量定义完整性**：自 v9（inputSummary/outputSummary 未定义）→ v11（extractParsedSummary/ extractOutputSummary 未定义）→ v14（request 变量重赋值编译错误）→ 当前问题 1。伪代码中变量作用域和定义来源的完整性在多轮迭代中反复出现，建议本轮系统性审查 §4.1 全部伪代码中的变量定义。

### 新发现的问题（本轮新识别的问题）
- **问题 1**：`chatResponse` 和 `retryCount` 的变量作用域错误，直接导致伪代码不可执行
- **问题 2**：`structuredChat()` 返回值无法携带调用元数据，属于 API 签名层面的设计缺口
- **问题 3**：§1.3 遗漏 `CredentialProvider` 和 `EndpointRateLimiter`
- **问题 4**：`LlmChatOptions` 与 `ModelRoute.parameters` 的字段映射覆盖逻辑未定义
- **问题 5**：`PromptTemplateManager.getFallbackPrompt()` 返回值约束未定义

## 上一轮产出路径
C:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606271627_phase5_pkgG_ood/a_v17_copy_from_v16.md

## 用户需求
C:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606271627_phase5_pkgG_ood/requirement.md
