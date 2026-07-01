# 质量审查诊断报告 — Phase5 包G OOD 设计 v24

## 审查范围
- **审查视角**：落地实现视角，侧重需求响应充分度、整体深度与完整性、内部审议未充分覆盖的维度
- **待审查产出**：`a_v1_imported.md`（v24，3375 行）
- **用户需求**：Phase5 包G 的完整 OOD 设计（类图、核心职责、协作关系、关键接口、状态模型）

---

## 发现问题

### 问题1：[严重] `doExecuteInternal()` 伪代码中 `LlmChatService` 方法调用的返回类型错误

**位置**：§4.1 `doExecuteInternal()` 伪代码，structuredChat 和 chat 调用段（对应行号约 2308-2350）

**问题描述**：
`llmChatService.structuredChat()` 和 `llmChatService.chat()` 的方法签名（§2.3 类图、§3.2）均声明返回 `CompletableFuture<AiResult<X>>`，但 §4.1 伪代码直接将返回值作为裸对象使用：

- `structuredResult = llmChatService.structuredChat(chatRequest, outputType)` → 后续直接调用 `structuredResult.getData()`
- `chatResponse = llmChatService.chat(chatRequest)` → 后续直接调用 `chatResponse.getRetryCount()` / `chatResponse.getUsage()`

伪代码缺少两层解包步骤：先对 `CompletableFuture` 执行 `.join()` 或 `.get()`，再对 `AiResult` 执行 `.getData()` 获取内部负载。这与实际接口签名相矛盾。结合已存在的 `AiResult.java` 和 `AiService.java` 代码验证，`AiResult` 无 `getData()` 之外的负载提取方法，`CompletableFuture` 必须通过 `.join()` 阻塞获取。

**影响**：实现者在编码阶段按伪代码直接翻译将产生编译错误。此问题跨越结构化调用主路径和 chat() 回退路径两条代码分支，影响整个管线的核心 LLM 交互逻辑。

**严重程度**：严重

**改进建议**：将伪代码修正为显式解包模式。示例：
```
// 修正后
AiResult<StructuredChatResult<T>> structuredResultWrapper = llmChatService.structuredChat(chatRequest, outputType).join();
StructuredChatResult<T> structuredResult = structuredResultWrapper.getData();
```
chat() 回退路径同理。同时在 §3.2 `LlmChatService` 章节补充调用方职责说明：CapabilityExecutor 在 `supplyAsync` 上下文中通过 `.join()` 同步等待 LLM 调用完成。

---

### 问题2：[严重] `LlmChatResponse` 缺少 `retryCount` 字段

**位置**：
- §2.3 类图 `LlmChatResponse` 字段列表
- §3.2 `LlmChatResponse` 字段级契约
- §4.1 `doExecuteInternal()` catch 回退路径

**问题描述**：
§4.1 `doExecuteInternal()` 伪代码中 `structuredChat()` 回退到 `chat()` 的 catch 路径调用了 `chatResponse.getRetryCount()`，但：
1. `chat()` 返回 `CompletableFuture<AiResult<LlmChatResponse>>`，`LlmChatResponse` 的类图（§2.3）和字段契约（§3.2）仅定义了 `content`、`usage`、`modelId` 三个字段，**无 `retryCount` 字段**
2. 即使假设 `retryCount` 存在，结合问题1，`chatResponse` 的类型也不直接是 `LlmChatResponse`

对比：`structuredChat()` 的 retryCount 通过 `StructuredChatResult<T>` 携带（返回值包裹层），设计完整。但 `chat()` 回退路径缺少对应机制。

**影响**：此路径是 `StructuredOutputNotSupportedException` 异常分支的 fallback 代码，虽然非主路径，但一旦触发将因 `getRetryCount()` 方法不存在而导致编译错误。

**严重程度**：严重

**改进建议**：
方案一（推荐）：在 `LlmChatResponse` 中增加 `retryCount: int` 字段（默认 0），由 `LlmChatService` 实现层在内部重试后填充。同步更新 §2.3 类图和 §3.2 字段契约。
方案二（备选）：删除 `chatResponse.getRetryCount()` 调用，回退路径硬编码 `retryCount = 0`，并在注释中说明"`chat()` 回退路径不感知 LLM 内部重试次数"。

---

### 问题3：[重要] 薄适配器超时默认值的业务基础缺失

**位置**：§3.1 `thinAdapterTimeout` 段落（行 1070）、§9.5 YAML 配置（行 2905-2914）

**问题描述**：
6 项 Phase 4 薄适配器能力（AI 智能诊断、检查报告、检验报告、影像分析、开立检查检验、执行顺序推荐）的 `thinAdapterTimeout` 统一默认 30 秒，`per-capability` 统一 35 秒。但：

1. **无差异化依据**：影像分析（3.4.7）通常需处理图像数据，其 Phase 4 服务的 P99 响应时间很可能显著高于检查报告（3.4.5），统一 30s 缺乏对不同能力特征的分析论证
2. **无配置覆盖机制**：YAML 中 `thin-adapter-default: 30s` 是所有薄适配器的默认值，但设计未提供按能力标识覆盖 `thinAdapterTimeout` 的配置键（如 `ai.execution.timeout.thin-adapter.DIAGNOSIS`），仅通过 `@Value` 注入单值
3. **无配置指导**：文档未给出如何确定合适 `thinAdapterTimeout` 值的方法论（如参考 Phase 4 服务的 P99/P95 响应时间）

**影响**：上线后可能出现两类运维事故：(a) 合法慢能力（如影像分析）被频繁误降级，引起用户投诉；(b) 为规避误降级而统一放大超时值，削弱了对真正故障的快速响应能力。

**严重程度**：重要

**改进建议**：
1. 提供按能力标识覆盖 `thinAdapterTimeout` 的配置机制，如 `ai.execution.timeout.thin-adapter.THIN_CAPABILITY_TIMEOUT` Map 配置
2. 在 §9.5 YAML 中为每项薄适配器能力标注期望的 Phase 4 服务 P99 响应时间参考值
3. 在 §11 测试策略中增加"薄适配器超时值与 Phase 4 服务 P99 匹配度验证"的集成测试用例

---

### 问题4：[重要] 未定义 Phase 4 业务异常的标准化映射策略

**位置**：§3.1 薄适配器伪代码（行 912-917）、§4.2 薄适配器特化管线伪代码

**问题描述**：
薄适配器伪代码（§4.2）中 `BusinessException` 被捕获后以 `AiResult.failure(e.getMessage())` 返回。但：

1. **异常体系未约定**：文档未定义哪些 Phase 4 异常应归类为 `BusinessException`，哪些应视为 `Exception` 进入降级路径。实际中 Phase 4 服务可能抛出非法参数异常、数据不存在异常、权限异常等多种业务异常，全部走同一条路径是否合理未论证。
2. **错误码传递丢失**：`AiResult.failure(String)` 的参数作为 `errorCode` 存储（`AiResult.java:26`），但传入的是 `e.getMessage()`（自然语言消息）而非标准化错误码。下游消费者无法通过错误码做结构化处理。
3. **与完整管线不一致**：完整管线的降级路径通过 `DegradationReason` 枚举传递标准化原因，但薄适配器的业务异常路径使用裸字符串，两套体系不统一。

**影响**：Phase 4 薄适配器的异常分类和错误码传递缺乏标准化，下游监控系统和管理端难以区分"处方审核:药品库存不足"与"处方审核:患者ID为空"等不同性质的业务错误。

**严重程度**：重要

**改进建议**：
1. 为薄适配器定义 Phase 4 异常分类表：明确哪些异常属于 `BusinessException`（应返回 `AiResult.failure`）、哪些属于 `InfrastructureException`（应降级）
2. 在 `AiResult` 或 `DegradationReason` 体系中扩展业务错误码映射，或将 `AiResult.failure()` 的语义扩展为支持结构化的错误码+错误消息组合
3. 同步更新 §5.1 错误分类表，增加薄适配器业务异常的分类条目

---

### 问题5：[中等] `CredentialProvider` 故障恢复缺少结果上报机制

**位置**：§3.2 `CredentialProvider` Vault 降级状态模型（行 1465-1493）

**问题描述**：
`CredentialProvider` 的状态机定义了 NORMAL→CACHE_ONLY→BACKOFF 的三级降级路径，以及 BACKOFF 窗口到期后的探测查询路径。但与 `ModelEndpointHealthManager`（拥有 `recordCallResult()` 反馈方法）对比：

1. 状态机缺少 `recordCallResult(endpointId, success, elapsedMs)` 等价的上报方法——当外界（`LlmChatService` 或管理端探测工具）验证凭据有效后，无法主动通知 CredentialProvider 重置状态
2. 当前仅 Vault 查询成功时清零连续失败计数器。但若 Vault 间歇性恢复后立即再故障，将重新经历完整的 CACHE_ONLY (5次fail) → BACKOFF 循环，放大了不可用窗口
3. BACKOFF→NORMAL 的探测路径仅由定时器驱动，缺少显式的凭据验证调用方反馈机制

**影响**：Vault 间歇性抖动的场景下，凭据不可用状态的恢复速度受限于 30s 退避窗口，且无法通过外部凭据验证成功加速恢复。

**严重程度**：中等

**改进建议**：
1. 在 `CredentialProvider` 中新增 `reportCredentialResult(endpointId, boolean valid)` 方法，可供 `LlmChatService` 在调用成功后反馈凭据有效性
2. 收到有效反馈后，从当前状态（CACHE_ONLY/BACKOFF）直接跳转到 NORMAL，无需等待 Vault 查询或定时器
3. 在 §4.1 `doExecuteInternal()` 的 `LlmChatService` 调用成功路径中补充 `credentialProvider.reportCredentialResult(endpointId, true)` 调用

---

## 整体评价

产出是一份极其详尽的 OOD 设计文档（3375 行），覆盖了类图、核心抽象、协作关系、关键接口、状态模型、错误处理、并发设计、迁移路径、测试策略等完整要素，框架完备性高。文档经历了从 v2 到 v24 的多轮审议迭代，大部分早期问题已得到修正。

但文档存在两类结构性弱点，值得在后续迭代中关注：

1. **伪代码精确性不足**：§4.1 核心管线的伪代码存在类型级错误（问题1、2），这些错误会直接导致实现阶段的编译失败。OOD 设计文档中的伪代码是"实现者第一参考"，其精确性要求应高于普通设计描述。

2. **历史债务积累过多**：修订说明占文档约 40% 篇幅（v2 至 v24 共 23 个修订块），当前有效决策散布在正文和修订说明中。建议定稿时将修订说明中仍生效的内容归并到正文对应章节，移除纯历史记录。
