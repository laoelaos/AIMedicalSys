# 质量审查报告 — Phase 5 包 G OOD 设计（v6，第2轮）

## 审查概要

**审查范围**：a_v6_copy_from_v5.md（3538行）
**审查视角**：实际落地评估——设计是否可直接指导编码实现、接口定义是否足以支持下游消费者、异常场景和边界条件是否已考虑
**审查重点**：需求响应充分度、整体深度与完整性（避免重复内部审议已覆盖的技术可行性维度）
**迭代背景**：第6轮审查，上一轮审查报告(b_v6_diag_v1.md)已处理v5的37项问题，但质询指出审查维度偏重于技术可行性，本轮侧重补齐需求响应充分度和完整性评估

---

## 发现问题

### 1. [重要] 薄适配器超时配置存在自相矛盾——YAML 示例违反文档自身定义的层级约束

**问题描述**：§3.1（第1171-1173行）明确定义了超时层级约束："per-capability 必须 >= thinAdapterTimeout，推荐保持 +5 秒缓冲，防止两超时同时触发导致同一请求被降级两次"。然而 §9.5 YAML 示例（第3350-3365行）的实际配置值违反此约束：

| 能力 | per-capability | thin-adapter.per-capability | 是否满足"per-capability > thinAdapterTimeout + 5s" |
|---|---|---|---|
| DIAGNOSIS | 35s | 35s | 否，buffer=0 |
| IMAGE_ANALYSIS | 35s | 45s | 否，thinAdapterTimeout(45s) > per-capability(35s)，负buffer |

- **IMAGE_ANALYSIS**：`thin-adapter.per-capability` 设为 45s，但 `per-capability` 仅 35s。运行时 `orTimeout(35s)` 将在 `delegateFuture.get(45s)` 超时之前先触发，外层 `TimeoutException` 被 `exceptionally()` 捕获后 `doDegrade()` 进入降级路径，而 `delegateFuture` 仍在后台执行 45s。这直接违反 §3.1 描述的"内部 thinAdapterTimeout 优先触发"的设计意图，且外层 orTimeout 触发时无法将 `DegradationReason` 精确标记为薄适配器超时而非整体管线超时。
- **DIAGNOSIS**：两值相等（均为 35s），文档注释（第3348行）明确指出"若两值相等，同一请求可能同时触发两超时，导致同一降级原因被误记两次"。

**所在位置**：§3.1 超时层级关系（行1171-1173）vs §9.5 YAML 配置（行3350-3355, 3360-3364）
**严重程度**：重要（配置级事实矛盾，直接导致运行时行为与设计意图不符）
**改进建议**：
- IMAGE_ANALYSIS：修正 `per-capability` 为 50s（thin-adapter 45s + 5s 缓冲），或修正 `thin-adapter.per-capability` 为 30s（使用 thin-adapter-default）并保持 per-capability 为 35s。
- DIAGNOSIS：`per-capability` 设为 40s（thin-adapter 35s + 5s 缓冲），或 `thin-adapter.per-capability` 设为 30s（使用默认值）。
- 建议新增配置校验注解（通过 `@ConfigurationProperties` 的 JSR-303 或自定义校验器），在启动期检测两配置值的层级关系。

---

### 2. [重要] 类图与正文契约字段不一致——实现者仅参类图将遗漏 4 个业务字段

**问题描述**：§2.3 类图中的两个类缺少 §3.2 正文中已明确定义的字段，实现者若以类图为主要编码参考将遗漏字段：

1. **`LlmChatOptions`**（行428-433）：类图仅显示 `modelId`、`temperature`、`maxTokens`、`stopSequences` 四个字段。但 §3.2 LlmChatOptions 字段级契约（行1442-1451）同时定义了 `topP`、`frequencyPenalty`、`presencePenalty` 三个额外字段，并在参数映射扩展点规则（行1451）中一并阐述。类图与正文描述不一致。

2. **`ModelRoute`**（行379-387）：类图缺少 `authType: AuthType` 字段。§3.2 ModelRoute 字段扩展表（行1512-1520）中定义了 `authType: AuthType enum`。虽然 `AuthType` 已在 `Credential` 类图中出现（行592），但 `ModelRoute` 到 `AuthType` 的使用关联线也未添加。

**所在位置**：§2.3 类图（行428-433, 379-387）vs §3.2（行1442-1451, 1512-1520）
**严重程度**：重要（类图作为设计交付的核心产出，与正文不一致降低整体交付质量）
**改进建议**：
- 在 §2.3 `LlmChatOptions` 类图中补充 `topP: Double`、`frequencyPenalty: Double`、`presencePenalty: Double` 三个字段。
- 在 §2.3 `ModelRoute` 类图中补充 `authType: AuthType` 字段及 `ModelRoute --> AuthType : uses` 关联线。

---

### 3. [重要] §9.5 YAML 配置示例缺少 `transcript-summary` 超时配置项——下游运维消费者无法发现此可调参数

**问题描述**：§3.11.7（行2492）定义了 `DiscussionConclusionCapabilityExecutor` 的前置 LLM 调用超时 `transcriptSummaryTimeout`，默认 15 秒，注入方式为 `@Value("${ai.execution.timeout.transcript-summary:15s}")`。但 §9.5 YAML 配置示例的 `execution.timeout` 块（行3334-3365）中未包含 `transcript-summary` 配置项。运维人员查阅配置示例时无法知悉此参数的存在及调整方法。

**所在位置**：§3.11.7（行2492）vs §9.5 YAML 配置（行3334-3365 execution.timeout 块）
**严重程度**：重要（下游消费者接口定义不完整）
**改进建议**：在 §9.5 YAML 的 `execution.timeout` 块中补充 `transcript-summary: 15s` 配置项，并添加注释说明用途。

---

### 4. [中等] 状态恢复路径验证缺少 `CredentialProvider`——凭据获取的可靠性验证被遗漏

**问题描述**：§11.4 "状态恢复路径验证"表（行3496-3503）覆盖了 `CircuitBreakerDegradationStrategy`、`ModelEndpointHealthManager`、`PromptTemplate`、`Experiment` 四个组件的完整状态转换路径验证。但 `CredentialProvider`（§3.2，行1586-1638）同样持有正式的 NORMAL ↔ CACHE_ONLY ↔ BACKOFF 状态机及其恢复路径（CACHE_ONLY 下 Vault 恢复→NORMAL，BACKOFF 退避到期探测成功→NORMAL），未被纳入 §11.4 验证表。凭据管理在生产环境中直接影响所有 LLM 调用的认证成功率，其状态恢复路径的测试覆盖缺失可能导致 Vault 故障恢复后凭据获取仍处于不可用状态的静默故障。

**所在位置**：§11.4 状态恢复路径验证表（行3496-3503）vs §3.2 CredentialProvider 状态机（行1586-1638）
**严重程度**：中等
**改进建议**：在 §11.4 状态恢复路径验证表中新增一行：

| 组件 | 状态机 | 需验证的恢复路径 |
|------|--------|----------------|
| `CredentialProvider` | NORMAL ↔ CACHE_ONLY ↔ BACKOFF | CACHE_ONLY 下 Vault 恢复正常→NORMAL；BACKOFF 退避到期探测成功→NORMAL |

---

### 5. [中等] 薄适配器非 HTTP 场景下 DTO 字段提取路径不可执行——跨包依赖解决方案无时间线

**问题描述**：§3.1（行941-949）描述了薄适配器型 CapabilityExecutor 的非 HTTP 场景 departmentId 提取策略：优先从 `RequestContext` 提取，若为 null（非 HTTP 场景如 MQ/定时任务），回退到 `extractDepartmentIdFromDto(request)`。然而同一节（行879-882）明确承认 6 项薄适配器对应的 Phase 4 请求 DTO 均为**空类**（无业务字段），`extractDepartmentIdFromDto()` 所能做的事情等效于返回 null。设计将解决方案推至"跨包 OOD 协作会议"确认（行881），但未定义：
- 会议时间线（属于 Phase 5 哪个迭代）
- 会议前的实现期 fallback 方案
- 验收标准（Phase 4 DTO 需达到什么状态才能支撑非 HTTP 场景）

这意味着 Phase 5 底座实现者按当前设计编码，薄适配器在非 HTTP 场景下所有就诊上下文字段（departmentId/visitId/patientId/sessionId）保持为 null，影响 `AiCallRecord` 完整性和 Prompt 模板按科室选择。

**所在位置**：§3.1 Phase 4 服务接口契约定义（行879-882）及薄适配器伪代码（行941-949, 951-963）
**严重程度**：中等（设计深度不足，跨包依赖未收敛）
**改进建议**：在 §3.1 底座侧处理策略中补充：
- 定义 Phase 5 实现期的临时 fallback 方案（如：Phase 5 为薄适配器定义 DTO 包装接口，在底座侧封装 Phase 4 空 DTO，为 `AiRequestBase` 字段预留 setter）
- 明确跨包 OOD 协作会议的最晚截止时间（如 Phase 5 批次 5 实施前）
- 定义 Phase 4 DTO 改造的最小验收标准（至少需有 `getDepartmentId()`/`getVisitId()`/`getPatientId()`/`getSessionId()` 四个方法）

---

### 6. [一般] `BusinessException` 异常类型在 6 个 Phase 4 模块中的存在性未验证

**问题描述**：薄适配器伪代码（§3.1 行998-1006、§4.2 行2848-2854）通过 `catch (BusinessException e)` 区分 Phase 4 业务异常（返回 `AiResult.failure()`）与基础设施异常（走降级路径）。但设计中未验证以下事项：
- 6 个 Phase 4 业务模块（`DiagnosisService`、`InspectionReportService` 等）是否统一存在名为 `BusinessException` 的异常类型
- 各模块的 `BusinessException` 是否都有 `getErrorCode()` 方法（伪代码行1003使用的 `e.getErrorCode()`）
- 不存在统一异常类型的模块应如何处理（伪代码未定义默认 catch 策略）

**所在位置**：§3.1 薄适配器伪代码（行998-1006）、§4.2 薄适配器管线伪代码（行2848-2854）
**严重程度**：一般
**改进建议**：在 §3.1 "Phase 4 服务接口契约定义"段落后新增异常契约子节，逐一列出 6 个 Phase 4 模块的异常类型及处理方法，或定义底座侧的 `Phase4ServiceException` 包装统一异常接口。若部分 Phase 4 模块无 `BusinessException`，明确其异常应归类为基础设施异常（走降级路径）还是业务异常（走 failure 路径）。

---

### 7. [一般] `LlmChatRequest.tools` 字段的构造逻辑在 §4.1 伪代码中未体现

**问题描述**：§3.2（行1406）明确定义了 `LlmChatRequest.tools` 字段（`List<ChatToolDefinition>`）并描述了 `CapabilityExecutor` 通过 Jackson `SchemaFactory` 从 `outputType` 自动生成 JSON Schema 后设置到此字段的机制。但 §4.1 `doExecuteInternal()` 伪代码（行2672-2678）在构造 `LlmChatRequest` 时仅设置了 `messages`、`options`、`clientType`，未展示 `tools` 字段的生成和设置步骤。实现者按 §4.1 伪代码编码将遗漏 tools 生成逻辑，导致 `structuredChat()` 调用时不携带 tool 定义，强制模型以纯文本格式输出而非原生 tool_use/function_call 模式。

**所在位置**：§4.1 LlmChatRequest 构造伪代码（行2672-2678）vs §3.2 LlmChatRequest.tools 字段定义（行1406）
**严重程度**：一般
**改进建议**：在 §4.1 伪代码的 LlmChatRequest 构造步骤后补充 `chatRequest.setTools(ChatToolDefinition.fromOutputType(outputType))` 调用，并在伪代码注释中说明 Jackson SchemaFactory 的生成逻辑（或标注"具体实现见 §3.2 tools 字段契约"的交叉引用）。

---

## 整体质量评价

文档经过 5 轮迭代和 37 项问题修复后，在核心抽象定义、管线编排、组件职责划分等维度已达到较高成熟度。需求中列出的类图、核心职责、协作关系、关键接口、状态模型等 OOD 核心要素均有覆盖，整体设计风格与 Phase0/Phase1ABD 保持了较好的一致性。

但本报告从需求响应充分度和整体完整性维度识别出 7 个质量问题，其中 **3 项重要**（超时配置自相矛盾、类图字段不一致、运维配置遗漏）、**2 项中等**（状态验证遗漏、跨包依赖未收敛）、**2 项一般**。最值得关注的问题依次为：

1. **超时配置自相矛盾**（问题 1）— 设计文档自身定义的约束与给出的配置示例冲突，IMAGE_ANALYSIS 的 `thinAdapterTimeout(45s) > per-capability(35s)` 将导致外层超时先触发，运行时行为与设计意图不一致。
2. **类图字段缺失**（问题 2）— 作为设计交付核心产出的类图与正文契约不一致，降低整体交付可信度。
3. **运维配置遗漏**（问题 3）— YAML 配置示例是运维团队唯一参考来源，遗漏 `transcript-summary` 使该参数不可被发现。

建议修复者优先处理问题 1（事实矛盾）和问题 2（类图完整度），其次处理问题 3~4（下游消费者接口完备性），问题 5~7 可编排至后续迭代或由跨包协作同步解决。
