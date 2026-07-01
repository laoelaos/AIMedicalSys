# 质量审查诊断报告 — Phase5 包G OOD 设计 v24（第2轮）

## 审查范围
- **审查视角**：落地实现视角，侧重需求响应充分度、整体深度与完整性、内部审议未充分覆盖的维度
- **待审查产出**：`a_v1_imported.md`（v24，3375 行）
- **用户需求**：Phase5 包G 的完整 OOD 设计（类图、核心职责、协作关系、关键接口、状态模型）

---

## 发现问题（保留第1轮）

### 问题1：[严重] `doExecuteInternal()` 伪代码中 `LlmChatService` 方法调用的返回类型错误

**位置**：§4.1 `doExecuteInternal()` 伪代码，structuredChat 和 chat 调用段

**问题描述**：
`llmChatService.structuredChat()` 和 `llmChatService.chat()` 的方法签名（§2.3 类图、§3.2）均声明返回 `CompletableFuture<AiResult<X>>`，但 §4.1 伪代码直接将返回值作为裸对象使用。缺少两层解包步骤：先对 `CompletableFuture` 执行 `.join()` 或 `.get()`，再对 `AiResult` 执行 `.getData()`。

**影响**：实现者按伪代码直接翻译将产生编译错误。

**严重程度**：严重

**改进建议**：将伪代码修正为显式解包模式。

---

### 问题2：[严重] `LlmChatResponse` 缺少 `retryCount` 字段

**位置**：§2.3 类图、§3.2 字段契约、§4.1 chat() 回退路径

**问题描述**：`LlmChatResponse` 仅定义了 content、usage、modelId，但伪代码调用了 `chatResponse.getRetryCount()`。

**严重程度**：严重

**改进建议**：在 `LlmChatResponse` 中增加 `retryCount: int` 字段或删除此调用硬编码为 0。

---

### 问题3：[重要] 薄适配器超时默认值的业务基础缺失

**位置**：§3.1 `thinAdapterTimeout`、§9.5 YAML 配置

**问题描述**：6 项 Phase 4 薄适配器能力的 `thinAdapterTimeout` 统一 30s，无差异化依据、无按能力覆盖机制、无配置指导。

**严重程度**：重要

**改进建议**：提供按能力标识覆盖的机制，标注各能力 P99 参考值。

---

### 问题4：[重要] 未定义 Phase 4 业务异常的标准化映射策略

**位置**：§3.1 薄适配器伪代码、§4.2 特化管线伪代码

**问题描述**：Phase 4 业务异常以裸字符串 `e.getMessage()` 通过 `AiResult.failure()` 传递，缺乏标准化错误码和异常分类体系。

**严重程度**：重要

**改进建议**：定义 Phase 4 异常分类表，扩展 `AiResult` 或 `DegradationReason` 支持结构化错误码。

---

### 问题5：[中等] `CredentialProvider` 故障恢复缺少结果上报机制

**位置**：§3.2 `CredentialProvider` Vault 降级状态模型

**问题描述**：状态机缺少外部调用方可调用的凭据有效性反馈方法，恢复速度仅由定时器驱动。

**严重程度**：中等

**改进建议**：新增 `reportCredentialResult(endpointId, boolean valid)` 方法，调用成功后可从 CACHE_ONLY/BACKOFF 直接跳转至 NORMAL。

---

## 发现问题（第2轮新增）

### 问题6：[严重/整体深度与完整性] `DegradationStrategy` 接口缺少 `getOrder()` 方法，策略排序逻辑无法编译

**位置**：
- §4.1 `AbstractCapabilityExecutor.execute()` 伪代码（`for each strategy in this.degradationStrategies (sorted by getOrder() asc)`）
- §3.1 降级策略注入机制段
- §7 设计决策表（`DegradationStrategy.getOrder() 扩展方式` 行）
- 实际代码：`DegradationStrategy.java`（空接口，仅含 `boolean shouldDegrade(DegradationContext context)`）

**问题描述**：
通过实际代码验证发现：

1. `DegradationStrategy.java`（`ai-api` 模块）当前仅定义了一个方法 `boolean shouldDegrade(DegradationContext context)`，**不存在** `getOrder()` 方法，无论是否为 `default` 方法。设计 §4.1 伪代码中策略链排序逻辑 `sorted by getOrder() asc` 直接引用了一个不存在的 API。

2. 设计 §7 和 §9.4 虽已识别需要新增 `default int getOrder() { return 0; }`，但：
   - 未验证此变更在不同 `DegradationStrategy` 实现类上的兼容性——当前代码库存在 `NoOpDegradationStrategy` 实现（标注 `@ConditionalOnMissingBean`，说明可能被项目其他模块的自定义策略实现替代），新增 default method 本身是二进制兼容的，但设计未检查是否存在非 Java 8 编译目标的自定义策略或通过字节码增强（CGLIB）代理的策略实现
   - `ai-api` 模块的接口变更是公共 API 变更，影响所有依赖 `ai-api` 的模块（包括可能的项目外部引用方）。设计未列出需验证的模块清单

3. §4.1 伪代码的排序逻辑与 §3.8 `DegradationReason` 枚举的设计存在隐含矛盾——`STRATEGY_TRIGGERED`（降级预检非特定策略命中）的降级原因设计为 `DegradationReason.STRATEGY_TRIGGERED + ":" + strategy.getClass().getSimpleName()`，此设计依赖策略类名。若新增 `getOrder()` 后策略排序变化，将直接影响降级原因字符串的稳定性，但设计未分析此影响。

**影响**：
- 直接编译失败：`AbstractCapabilityExecutor.execute()` 中引用 `getOrder()` 的排序逻辑无法编译
- 间接影响：策略排序变化引起降级原因字符串变化，影响下游监控系统（基于降级原因做告警规则过滤的场景）
- 沟通成本：实现者需主动发现设计假设与 API 现状的差距，再补充接口定义后才能继续实现

**严重程度**：严重

**改进建议**：
1. 在 `DegradationStrategy` 接口上补充 `default int getOrder() { return 0; }` 的定义，作为 Phase 5 开发的第一步 API 变更
2. 在 §9.4 的"全量编译验证"步骤中明确列出所有需验证的模块路径（含可能的项目外部引用方），而非仅描述 "mvn compile"
3. 在 §4.1 伪代码的排序逻辑注释中说明 `getOrder()` 的作用边界——"排序仅用于决定降级预检循环中策略的执行顺序，不影响降级原因字符串"

---

### 问题7：[重要/整体深度与完整性] FallbackAiService 现有实现与设计迁移路径的起点不一致，底座切流时需同步重构

**位置**：
- §9.2 `FallbackAiService.applyStrategies()` 迁移路径
- §9.3 `FallbackAiService` 构造器迁移路径
- 实际代码：`FallbackAiService.java`

**问题描述**：
通过实际代码验证发现 `FallbackAiService.java` 的现有实现与设计描述的迁移"起点"存在以下系统性差异：

| 维度 | 设计描述的迁移目标（§9.2-§9.3） | 实际代码现状 |
|------|-------------------------------|-------------|
| 构造器注入方式 | `ObjectProvider<AiService>` | `List<AiService>` |
| 委托选择逻辑 | `aiServiceProvider.getIfUnique()` | `delegates.get(0)`（从 List 中排除自身后取第一个） |
| Bean 装配 | 标注 `@Primary` | 未标注 `@Primary` |
| `applyStrategies()` | 标记 `@Deprecated` + `if (aiPlatformEnabled)` 条件包裹 | 无条件在每个 AiService 方法后通过 `.thenApply(this::applyStrategies)` 调用 |
| 降级策略管理 | 降级判定移入 CapabilityExecutor 管线，FallbackAiService 不持有策略列表 | 持有 `List<DegradationStrategy> strategies` 构造注入 |
| 上下文构造 | 降级预检由 `SlidingWindowMetricsStore.buildDegradationContext()` 提供数据 | `new DegradationContext()` 创建空上下文 |

此偏差意味着底座切流不是简单的"底座激活时 AiOrchestrator 生效"的开关切换，而是需要同时对 FallbackAiService 进行两组重构（构造器迁移 + applyStrategies 剥离），且两组重构之间存在依赖关系（构造器迁移是 applyStrategies 剥离的前提）。

设计 §9.3 提到"两套迁移可独立进行，建议先完成构造器迁移"，但在实际代码中 `applyStrategies()` 是在 `thenApply()` 回调中调用的，与构造器迁移完全正交——即使构造器改为 ObjectProvider，`applyStrategies()` 仍可通过 `thenApply()` 继续工作。设计未分析此过渡期内两套降级逻辑（旧 applyStrategies + 新 CapabilityExecutor 降级）可能同时运行的状态：

- 若底座激活后 `applyStrategies()` 未被剥离，则 FallbackAiService 会在 AiOrchestrator 完成降级预检后，对返回的 `AiResult` 再次执行 `applyStrategies()` 降级判定，形成**双降级判定**。由于旧 applyStrategies 使用空 DegradationContext，所有策略的 `shouldDegrade()` 均返回 false（空上下文无指标数据），双降级不会导致误降级，但会造成不必要的每次调用开销。
- 若底座激活前 CapabilityExecutor 的降级预检已生效（由 `@ConditionalOnProperty` 控制），则存在底座未完全激活但 CapabilityExecutor 部分组件已实例化的风险。

**影响**：
- 实现者若严格遵循 §9.2-§9.3 的迁移路径，会在编译阶段即遭遇构造器签名不匹配问题
- 过渡期内双降级判定的性能损耗和潜在冲突未评估

**严重程度**：重要

**改进建议**：
1. 在 §9.2 中补充过渡期内双降级判定的影响分析：明确 `applyStrategies()` 在底座激活后是否产生副作用，是否需要添加 `if (!aiPlatformEnabled)` 守卫
2. 在 §9.3 中补充 FallbackAiService 现有代码的完整现状记录（构造器、@Primary、delegates 选择逻辑），使实现者能准确评估迁移工作量
3. 建议在底座开发初期先完成 FallbackAiService 的两个重构（构造器迁移 + applyStrategies 条件守卫），再进行 CapabilityExecutor 管线的实现，以减少过渡期的代码共存复杂度

---

### 问题8：[重要/需求响应充分度] `AiRequestBase` 基类不存在但管线伪代码已假设其存在

**位置**：
- §3.5 `AiRequestBase` 定义段
- §3.1 `doExtractDepartmentId()`/`doExtractVisitId()`/`doExtractPatientId()`/`doExtractSessionId()` 默认实现
- §4.1 `AiOrchestrator.handle()` catch 块中 `request instanceof AiRequestBase` 分支
- 实际代码：`AiRequestBase.java` 不存在

**问题描述**：
设计 §3.5 定义了 `AiRequestBase` 抽象类作为 13 项 AI 能力请求 DTO 的公共基类，并声称"Phase 5 底座切流时仅要求 7 项底座能力的 DTO 完成基类继承改造"。但通过代码验证发现：

1. **基类不存在**：`com.aimedical.modules.ai.api.dto.base.AiRequestBase` 既无 Java 文件也无可加载类。这意味着 13 个现有 DTO（`TriageRequest`、`DiagnosisRequest` 等）均未继承该基类，且设计师未评估创建一个抽象基类后如何影响 13 个已有 DTO 的 Jackson 序列化兼容性。

2. **管线伪代码已依赖基类存在**：
   - `doExtractDepartmentId()` 默认实现：`request.getDepartmentId()`（从 `AiRequestBase.getDepartmentId()` 获取）
   - `doExtractVisitId()` 默认实现：`request.getVisitId()`（从 `AiRequestBase.getVisitId()` 获取）
   - `AiOrchestrator.handle()` catch 块：`request instanceof AiRequestBase` 分支提取就诊上下文字段
   - 即使走"默认实现"路径，这些 DTO 在完成基类继承改造之前也无法编译

3. **过渡策略存在时序矛盾**：§3.5 的"过渡策略"说"7 项底座能力的 DTO 先行改造，6 项 Phase 4 能力 DTO 暂维持现状"，但 7 项底座能力的 DTO 在现有代码中属于 `ai-api/dto/*` 子包，由业务模块通过 `ai-api` 依赖使用。修改这些 DTO 的继承关系会：
   - 改变 DTO 的 JSON 序列化结构（新增父类字段若序列化策略不当会导致下游反序列化失败）
   - 需要同时修改业务模块中的 DTO 构造和测试用例
   - 设计 §3.5 虽提到了"反序列化兼容性测试"，但未评估此改造对现有业务模块的影响范围（哪些模块引用了哪些 DTO、引用方式如何）

**影响**：
- 管线伪代码在 7 项底座能力 DTO 完成 AiRequestBase 继承改造前无法编译
- 实现者需先完成底座能力 DTO 的基类继承改造，但设计未提供改造指南（如 AiRequestBase 的具体字段定义、Jackson 注解配置、与现有 DTO 构造器的兼容方案）
- 13 个 DTO 中已有的 `visitId`/`patientId`/`sessionId` 字段字段名与 `AiRequestBase` 基类字段名是否一致未经验证（如 `DiagnosisRequest` 是否使用 `visitId` 还是 `visitNo` 等不同命名）

**严重程度**：重要

**改进建议**：
1. 在 §3.5 中补充 `AiRequestBase` 的具体类定义（含字段声明、Jackson 反序列化注解、`@JsonIgnoreProperties` 策略），使实现者可直接编码
2. 补充对所有 13 个现有 DTO 的字段名兼容性核查——逐个验证现有 DTO 中公共上下文字段的字段名与 `AiRequestBase` 声明的字段名是否一致。若存在不一致（如某 DTO 使用 `visitNo` 而非 `visitId`），需定义重命名策略或提供映射方案
3. 在 §11 测试策略中增加"DTO 基类继承后的序列化兼容性测试"用例，覆盖新 DTO 序列化 → 旧客户端反序列化（前向兼容）和旧 DTO 序列化 → 新客户端反序列化（后向兼容）两个方向

---

### 问题9：[中等/整体深度与完整性] 4 项 Phase 5 首次落地底座能力缺少能力级别的特化设计

**位置**：
- §9.1 迁移路径表（`KbQueryCapabilityExecutor`、`ScheduleCapabilityExecutor`、`DiscussionConclusionCapabilityExecutor`、`PrescriptionAssistCapabilityExecutor` 行）
- §2.1 目录结构（上述 4 个 CapabilityExecutor 实现类文件）
- §4.1 通用管线伪代码

**问题描述**：
13 项 CapabilityExecutor 实现中，以下 4 项是 Phase 5 **首次实现**（非迁移或薄适配器），设计为其提供的"特化"内容为零：

| 能力 | 设计提供的特化程度 | 实现依赖 |
|------|------------------|---------|
| 3.4.8 AI 知识库问答 | 仅类名（继承 AbstractCapabilityExecutor）| 无现有实现，需从零构建 |
| 3.4.10 AI 辅助开方 | 仅类名 | 设计声明"迁移至底座"但未给出其接入管线内的实现策略 |
| 3.4.12 AI 医生排班 | 仅类名 | Phase 5 首次落地，无任何前序设计 |
| 3.4.13 AI 综合讨论结论 | 仅类名 | Phase 5 首次落地，无任何前序设计 |

对比之下，薄适配器（6 项 Phase 4 能力）有明确的委托目标（现有 Phase 4 业务服务）和特化伪代码（§4.2）；预设迁移能力（3.4.1/3.4.2/3.4.3）有现有实现可供参考。而这 4 项能力既无现有实现可参考、也无能力级设计描述，实现者拿到 OOD 后仍需自行完成以下设计决策：

- 输入/输出 DTO 结构（目前仅限于 `KbQueryRequest`/`KbQueryResponse` 等命名，无字段定义）
- Prompt 模板结构（哪些变量注入、系统角色提示词内容定位）
- 结构化输出解析的目标类型（`outputType` 参数传递的 Class 对象）
- 模板变量提取策略（哪些 DTO 字段映射到哪些模板变量）
- 可用模型路由配置（YAML 中无 KB_QUERY 等能力的路由条目）
- 兜底 Prompt 内容（§9.5 YAML fallback 块未覆盖这些能力）

**影响**：
- 4 项能力的实现质量将完全取决于实现者的个人判断，而非设计指引
- 各实现者的设计决策可能不一致（如 KB_QUERY 和 SCHEDULE 的 DTO 字段命名约定），导致后续维护困难
- 测试策略（§11）的"完整管线集成测试"因缺失能力级细节而无法编写

**严重程度**：中等

**改进建议**：
1. 为 4 项首次落地能力补充能力级别的管线特化描述，至少包含：输入/输出 DTO 的预期字段、Prompt 模板的预期结构（变量集合）、结构化解析的目标类型声明
2. 对于 3.4.10 AI 辅助开方，明确其"迁移至底座"是否意味着与 3.4.1/3.4.2/3.4.3 使用同一 LLM 管线后端（即已有 Prompt+模型），还是需要独立设计
3. 在 §9.5 YAML 路由配置中为这些能力补充路由条目（即使使用默认模型），使集成测试的最小配置可开箱运行

---

## 关于第1轮质询的回应

本报告保留第1轮 5 个问题的原因是：它们均为有效的技术质量问题，发现至今未在后续设计中得到修复。同时，第2轮新增的 4 个问题（问题6-9）补充了第1轮审查中缺失的两个评估维度：

1. **需求响应充分度**（问题8）：通过代码验证发现 `AiRequestBase` 基类不存在但管线伪代码已依赖其存在——设计对"如何将现有 13 个 DTO 迁移到新基类"的可行性分析不足
2. **整体深度与完整性**（问题6-7、9）：通过代码验证发现设计对现有 API（`DegradationStrategy`、`FallbackAiService`）的假设与实际情况不符，影响设计的实现指导能力；同时发现 4 项首次落地能力的特化设计缺失

第1轮审查聚焦于设计文档内部的局部技术正确性（伪代码类型错误、字段缺失等），第2轮补充审查聚焦于设计文档与现有代码库之间的对齐度、以及设计深度是否足以覆盖全部 13 项能力的实现需要。

