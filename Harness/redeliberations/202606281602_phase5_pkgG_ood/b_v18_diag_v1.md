# 质量审查报告 — Phase 5 包G OOD 设计 v18

## 审查范围与视角

本次审查聚焦内部审议（设计-验证循环）未充分覆盖的维度：需求响应充分度、整体深度与完整性、实际落地可行性（直接指导编码实现、接口定义支持下游消费者、异常场景和边界条件覆盖）。已确认 v1~v18 共 18 轮内部审议已覆盖技术可行性、降级/超时/重试策略、类图文一致性等维度，本报告不再重复验证已确认的维度。

---

## 发现的问题

### 问题 1：Phase4ServiceMetaProvider 存在并发安全设计缺陷

| 维度 | 内容 |
|------|------|
| **问题描述** | `Phase4ServiceMetaProvider` 接口（§3.1, lines 1050-1076）的三个方法 `getUsedModelId()`、`getUsedPromptVersion()`、`getRetryCount()` 定义在 Phase 4 服务的单例 Bean 实例级别，语义为"返回**最近一次**内部调用使用的元数据"。在并发环境中，多个请求线程同时调用同一 Phase 4 服务，`getRetryCount()` 等接口返回的"最近一次调用"数据可能属于不同线程的请求，导致 `AiCallRecord` 中的元数据跨请求污染。例如：线程 A 发起诊断请求（重试 3 次成功），线程 B 紧跟的请求尚未完成，但线程 A 的 `getRetryCount()=3` 被线程 B 的薄适配器错误地读取为 B 的重试次数 |
| **所在位置** | §3.1 Phase4ServiceMetaProvider 接口定义（lines 1050-1076）、§4.2 薄适配器成功路径元数据提取（lines 3449-3459） |
| **严重程度** | **严重** — 此问题导致并发场景下 `AiCallRecord.modelId`/`promptVersion`/`retryCount` 三个字段的数值错误地混入其他请求的元数据，使性能分析报表（模型维度降级率、Token 用量归因等）产生系统性偏差。内部审议 18 轮中均未识别此并发安全隐患 |
| **改进建议** | 方案 A（推荐）：将元数据返回方式从"服务实例级接口"改为"请求/响应级绑定"——Phase 4 服务在响应 DTO 中内嵌元数据字段（`modelId`/`promptVersion`/`retryCount`），薄适配器直接读取响应 DTO 的元数据，不依赖服务实例状态。方案 B：将 `Phase4ServiceMetaProvider` 改为请求级上下文对象（如 `RequestContext<MetaData>`），通过方法参数传递而非服务实例级别的 `getXxx()` 交互 |

---

### 问题 2：类图缺少 `doDegrade` 方法，且 v18 修订声明与实际内容不一致

| 维度 | 内容 |
|------|------|
| **问题描述** | v18 修订说明第 2 条（文档尾部 lines 4273-4274）明确声明"§2.3 类图 doDegrade 方法签名补充 sentinelReason 第 15 个参数"。但实际读取的 §2.3 类图中 `AbstractCapabilityExecutor` 类节点（lines 448-465）只包含 `execute`、`doExecuteInternal`、`extractVariables`、`doExtractDepartmentId/VisitId/PatientId/SessionId`、`extractOutputSummary` 共计 8 个方法，**没有 `doDegrade` 方法的任何声明**。无论是 v17 版本还是 v18 版本，被扩展方法本身都不存在于类图中，添加参数的前提不成立 |
| **所在位置** | §2.3 类图 AbstractCapabilityExecutor（lines 448-465）与 §4.1 `doDegrade()` 伪代码（lines 3276-3303）及全篇多处 doDegrade 调用点 |
| **严重程度** | **重要** — 类图作为 OOD 设计的核心视觉产出，缺少一个在全篇伪代码中被调用 10+ 次的关键方法，且修订声明声称已修复但实际未修复。实现者仅依赖类图将完全忽略 `doDegrade` 的存在，影响对降级路径的整体理解 |
| **改进建议** | 在 §2.3 类图 `AbstractCapabilityExecutor` 类节点中新增 `doDegrade(startTime, degradeReason, request, capabilityId, departmentId, callerRole, callerId, visitId, patientId, sessionId, inputSummary, outputSummary, promptVersion, modelId, sentinelReason): AiResult~R~~` 方法声明（与 §4.1 伪代码签名一致）。同步修正 v18 修订说明的表述 |

---

### 问题 3：DiscussionConclusionCapabilityExecutor 前置 LLM 压缩调用缺少模型路由设计

| 维度 | 内容 |
|------|------|
| **问题描述** | §4.1 DiscussionConclusionCapabilityExecutor 特化伪代码（lines 3330-3346）中，前置压缩调用通过 `LlmChatService.chat(compressRequest)` 发送压缩摘要请求。但 `compressRequest` 的 `clientType` 字段仅标注注释"* 从 ModelRouter 获取或使用默认 *"（line 3339）。这里存在设计缺口：压缩调用发生在实验分流/模板渲染/模型路由**之前**（它是 `doExecuteInternal()` 中变量提取阶段的一部分），此时 `ModelRouter.route()` 尚未执行，**无法获知目标模型端点和 clientType**。即使选择"使用默认"，也未定义"默认"是什么——默认模型？默认端点？默认 `HttpApiLlmChatService`？ |
| **所在位置** | §4.1 DiscussionConclusionCapabilityExecutor.doExecuteInternal() 特化伪代码（lines 3307-3388），特别是 line 3339 |
| **严重程度** | **重要** — 压缩调用是讨论结论能力的必经路径（transcripts 超 3000 tokens 时每次都会触发），不解决模型路由问题，该特化伪代码不可执行。实现者将在此处陷入设计真空 |
| **改进建议** | 方案 A：为压缩调用引入一个固定的轻量模型配置（如硬编码 endpoint + 低成本的摘要模型），定义在 `DiscussionConclusionCapabilityExecutor` 的内部常量或配置项中。方案 B：在 `extractVariables()` 阶段之前允许一次独立的 `ModelRouter` 调用（用 `capabilityId="DISCUSSION_CONCLUSION"` 但以特殊标记绕过实验分流），路由结果缓存供压缩和主调用复用。无论哪种方案，需在 §3.11.7 或 §4.1 伪代码中以显式方式给出 |

---

### 问题 4：`estimateTokens()` 方法未定义，实现者无法编码

| 维度 | 内容 |
|------|------|
| **问题描述** | §4.1 DiscussionConclusionCapabilityExecutor 伪代码（line 3326）引用了 `estimateTokens(transcripts)` 方法，该方法仅在注释中说明为"按字符数/Token 比例粗略估算"。设计文档未给出：使用何种 Tokenizer（tiktoken? LLM 提供商 SDK?）、中文字符到 Token 的换算比例（中文医疗文本约 1 字 = 1.5~3 tokens，不同模型差异大）、是否计入角色标记开销等具体信息。实现者无法直接编码此方法 |
| **所在位置** | §4.1 DiscussionConclusionCapabilityExecutor.doExecuteInternal() 伪代码（line 3326） |
| **严重程度** | **重要** — 此方法是"是否触发前置压缩"的唯一判定条件（`> 3000 tokens` 阈值），判定不准将导致：阈值过松→大量未超限的 transcripts 也被压缩（浪费 Token 和延迟），阈值过紧→超长 transcripts 未被压缩（超出模型 context window 限制）。 |
| **改进建议** | 在 §3.11.7 或 §4.1 伪代码补充 `estimateTokens()` 的具体实现策略：(a) 明确使用的 Tokenizer 方案（推荐 tiktoken 或 LLM 提供商 SDK 的 `count_tokens` 方法）；(b) 给出中文医疗文本的保守换算比例（如将 `transcripts.toString().length() * 2` 作为快速估算值，精确值使用 Tokenizer）；(c) `> 3000` 阈值的决策依据（基于何种模型的 context window 限制） |

---

### 问题 5：薄适配器异常分类的字符串匹配存在维护脆弱性

| 维度 | 内容 |
|------|------|
| **问题描述** | §4.2 薄适配器 catch 块（lines 3432-3433）通过字符串数组 `["DiagnosisException", "InspectionException", ...]` 匹配 6 个已知 Phase 4 业务异常类名，以区分"业务异常"（返回 `AiResult.failure()`）与"基础设施异常"（走降级路径）。但此实现存在以下风险：(1) 若 Phase 4 模块重构时重命名异常类（如 `DiagnosisException` → `DiagnosisServiceException`），薄适配器将静默将业务异常归类为基础设施异常，走错误路径；(2) 若新增 Phase 4 模块（如 Phase 6 的某项能力），其异常类名不在白名单中，同样被静默错误归类 |
| **所在位置** | §4.2 薄适配器特化管线伪代码（lines 3424-3445） |
| **严重程度** | **中等** — 此问题不阻塞底座切流（Phase 4 接口在短期内稳定的前提下可正常运作），但构成运行时维护陷阱。一旦 Phase 4 异常类发生变更，问题会在无告警的情况下暴露 |
| **改进建议** | 方案 A（推荐）：建立 Phase 4 业务异常的公共基类约定（如 `Phase4BusinessException extends RuntimeException`），薄适配器通过 `instanceof` 而非字符串匹配分类。方案 B：若无法推动 Phase 4 公共基类，则将 6 个异常类的全限定名存入配置项，通过 `Class.forName()` 加 `isInstance()` 实现可配置的匹配，至少可由运维在异常名变更时调整配置而非改代码 |

---

### 问题 6：`structuredChat`→`chat` 回退路径的 `retryCount` 覆盖不一致未声明

| 维度 | 内容 |
|------|------|
| **问题描述** | §4.1 `doExecuteInternal()` 伪代码中存在两条成功路径：structuredChat 路径（line 3155）`retryCount = structuredChatResult.getRetryCount()` 和 chat 回退路径（line 3198）`retryCount = chatResponse.getRetryCount()`。两处的 `retryCount` 含义不同——structuredChat 路径的计数**包含** structuredChat 实现层内部重试（含首次结构化调用失败后的自动重试），而 chat 回退路径的计数**仅包含** chat 实现层的重试（原 structuredChat 调用阶段的内部重试已被丢失）。此差异意味着：当 `structuredChat()` 因 `StructuredOutputNotSupportedException` 回退到 `chat()` 时，A/B 实验效果分析报表中的 `retryCount` 维度数据偏低（缺失结构化阶段的重试次数），且未在任何文档位置说明此差异 |
| **所在位置** | §4.1 doExecuteInternal() 伪代码（lines 3154-3155 structuredChat 成功路径，lines 3198-3199 chat 回退路径） |
| **严重程度** | **中低** — 不影响功能正确性（两条路径均能返回正确业务结果），但影响可观测性数据的完整性和可比性。运维分析时可能误判回退路径的 LLM 调用质量 |
| **改进建议** | 方案 A：在 chat 回退路径的 `outputSummary` 提取注释中补充说明：`retryCount` 仅反映 chat 层重试次数，不反映结构化调用阶段的内部重试。方案 B：在 `StructuredOutputNotSupportedException` 异常中加入 `originalRetryCount` 字段，chat 回退路径在捕获时累加此值。 |

---

## 整体评价

产出在**存量需求响应**（13 项能力的完整 OOD 设计）和**技术深度**（线程安全、状态模型、超时层级约束、非功能性质量分析等）层面已达到较高完成度，18 轮迭代积累了丰富的历史经验教训。但本次审查发现了 6 个内部审议未充分覆盖的问题，其中 3 个（问题 1、3、4）属于**设计缺口或事实错误**，会直接影响编码实施可行性。建议在产出交付前优先修复问题 1 和问题 3（阻塞性设计缺口），问题 2（类图缺失）和问题 4（方法未定义）可在同一 Changelist 中同步补充。

