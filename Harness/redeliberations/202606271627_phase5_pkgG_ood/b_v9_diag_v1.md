# Phase 5 包 G — AI 进阶底座 OOD 质量审查报告（v9）

## 审查范围

- **审查轮次**：第 9 次迭代
- **审查维度**：需求响应充分度、完整性、逻辑一致性、落地可行性（侧重内部审议未充分覆盖的维度）
- **待审查产出**：`a_v9_copy_from_v8.md`（文档版本 v11）

---

## 发现的问题

### P1 [严重] 薄适配器 CapabilityExecutor 的 Phase 4 服务依赖机制未定义

- **问题描述**：产出定义 6 个薄适配器型 CapabilityExecutor（`DiagnosisCapabilityExecutor` 等）通过"直接委托 Phase 4 业务服务"执行。但产出**未定义 ai-impl 模块如何获取 Phase 4 服务的依赖**——是 ai-impl 直接添加 Phase 4 模块的 Maven 依赖？还是通过 OSGi/SPI/事件机制解耦？亦或是 Phase 4 服务实现某个统一接口？当前产出中仅写 `result = phase4ServiceDelegate.execute(request)` 而未定义 `phase4ServiceDelegate` 的注入来源和模块依赖方向，从实际编码视角看，开发者无法确定模块级构建关系。若 ai-impl 直接依赖 Phase 4 模块，将在模块间引入反向依赖（Phase 4 业务模块 → ai-api，而 ai-impl 依赖 Phase 4 模块），可能破坏模块分层。
- **所在位置**：§3.1 "薄适配器型 CapabilityExecutor 的管线行为"段落及伪代码；§2.2 模块依赖方向图（未绘制 ai-impl → Phase 4 模块的依赖箭头）
- **严重程度**：严重
- **改进建议**：在 §2.2 依赖方向图中明确 ai-impl 与 Phase 4 模块的依赖关系。若采用薄适配器模式，需定义 Phase 4 服务的 SPI 接口（归属 ai-api 或独立适配器包），或明确声明 ai-impl 直接依赖 Phase 4 模块。同时补充对应协作对象注入方式的说明（`@Autowired`、构造器注入、Bean 名称约定等）。

---

### P2 [严重] §4.1 核心伪代码中存在未定义变量

- **问题描述**：`doExecuteInternal()` 伪代码中使用了 `inputSummary`、`outputSummary`、`retryCount`、`outputType` 四个变量，这些变量在当前方法体和调用上下文中均未定义：
  - `inputSummary` 在 §3.5 描述为"取自请求 DTO 的 toString() 截断"，但伪代码中无截断调用
  - `outputSummary` 在 §3.5 描述为"取 LLM 响应的结构化输出后摘要截断"，但伪代码中无定义
  - `retryCount` 未在任何位置定义，LlmResponse 值对象不含此字段（§3.2 类图）
  - `outputType` 应来自 `getOutputType()` 但伪代码未显式获取
- **所在位置**：§4.1 `doExecuteInternal()` 伪代码（第 1230-1256 行）
- **严重程度**：严重
- **改进建议**：在伪代码中显式定义这四个变量的来源。例如：
  - `inputSummary = StringUtils.truncate(request.toString(), 500)`
  - `outputSummary = StringUtils.truncate(parsedResult.toString(), 500)`（成功时）或 `extractOutputSummary(result)`（降级时）
  - `retryCount = llmResponse.getRetryCount()`（若在 LlmResponse 中添加此字段）或 `= 0`（如无重试）
  - `outputType = getOutputType()` 在方法入口处赋值

---

### P3 [重要] 防御性拷贝合约在伪代码中未兑现

- **问题描述**：§3.1 "Request DTO 线程安全契约"段落明确声明：若现有 DTO 无法改为不可变，"则在 `AbstractCapabilityExecutor.execute()` 模板方法入口处对 `request` 执行防御性拷贝"。但 §4.1 的 `execute()` 伪代码中，`request` 被直接传入 lambda 和 `doExecuteInternal()`，**没有任何防御性拷贝步骤**。合约文本与行为定义不一致。
- **所在位置**：§3.1 vs §4.1 `AbstractCapabilityExecutor.execute()` 伪代码（第 1203-1225 行）
- **严重程度**：重要
- **改进建议**：在 §4.1 `execute()` 伪代码的 ThreadLocal 提取步骤之后、`supplyAsync()` 之前，增加防御性拷贝步骤：`request = objectMapper.convertValue(request, request.getClass())`。或以注释明确说明"如 DTO 已为不可变对象则跳过此步骤"。

---

### P4 [重要] TokenUsage 类未建模

- **问题描述**：LlmResponse 包含 `getTokenUsage()` 方法，返回值 `TokenUsage` 在各处被引用（`getTokenUsage().getPromptTokens()`、`getTokenUsage().getCompletionTokens()`），但 `TokenUsage` 既未出现在 §1.3 核心抽象一览表、§2.3 类图，也未在 §3.2 以独立值对象定义。开发者无法获知 `TokenUsage` 包含哪些字段及其类型。
- **所在位置**：§3.2 `LlmResponse` 类定义（第 49 行 `+TokenUsage tokenUsage`），§4.1 伪代码调用点
- **严重程度**：重要
- **改进建议**：在 §1.3 核心抽象表中补充 `TokenUsage` 行，在 §2.3 类图中补充 `TokenUsage` 类定义（字段：promptTokens/Integer、completionTokens/Integer、totalTokens/Integer），或在其值对象段落中补充定义。

---

### P5 [重要] ExperimentManager 未定义无实验时的返回值语义

- **问题描述**：§3.4 `ExperimentManager.assign()` 的返回类型为 `ExperimentAssignment`，§4.3 说"无 ACTIVE 实验 → 返回空 assignment（分组=default，targetPromptVersion=null）"。但 §4.1 伪代码中 `assignment.getTargetPromptVersion()` 被直接传入 `PromptTemplateManager.render()` 的 `promptVersion` 参数。若 assignment 为 null（空对象），则 `getTargetPromptVersion()` 调用将抛出 NPE；若 assignment 为非 null 但 targetPromptVersion 为 null，则使用默认模板（符合 §4.4 预期）。**问题在于 absence 语义未冻结**——"空 assignment"是指 `null` 引用还是 `group="default"` 且所有字段为 null 的非 null 对象？两种可能性的 NPE 风险不同。
- **所在位置**：§3.4 `ExperimentAssignment` 定义；§4.3 分流契约
- **严重程度**：重要
- **改进建议**：冻结"无实验命中"的返回值语义：明确返回非 null 的 `ExperimentAssignment`（`group="default"`，`targetModelId=null`，`targetPromptVersion=null`），并在类中说明调用者无需判空。或使用 `Optional<ExperimentAssignment>` 返回类型并在 §4.1 伪代码中体现 null 检查。

---

### P6 [中等] Phase 4 DTO 过渡策略与实际执行伪代码存在矛盾

- **问题描述**：§3.5 过渡策略声明"6 项 Phase 4 能力的 DTO 暂维持现状，不继承 AiRequestBase"，但 §3.1 薄适配器伪代码中 `doExecuteInternal()` 调用 `request.getVisitId()`、`request.getPatientId()`（第 667-683 行）——这些方法在 `AiRequestBase` 中定义，Phase 4 DTO 尚未继承，不存在这些方法。此外，`doExtractDepartmentId()` 返回的 `departmentId` 来自 RequestContextHolder，而薄适配器 doExecuteInternal 的 success/failure/degraded 路径中使用的 `request.getVisitId()`、`request.getPatientId()` 等字段也同样需要独立提取机制，但未定义。
- **所在位置**：§3.5 "过渡策略"段落 vs §3.1 薄适配器伪代码
- **严重程度**：中等
- **改进建议**：
  1. 统一薄适配器对各公共字段的提取策略：所有 Phase 4 DTO 上缺失的字段（visitId、patientId 等）均应通过独立的提取路径获取（如从 RequestContext/HTTP Header 提取，或 Phase 4 DTO 中自行提供 getter 适配器）。
  2. 在薄适配器 `doExecuteInternal()` 伪代码中体现此提取，而非假设 Phase 4 DTO 已有 `getVisitId()`/`getPatientId()` 方法。

---

### P7 [中等] 熔断器与端点健康管理器的统一探测机制缺少状态转换图

- **问题描述**：§3.2 "统一探测机制"描述了 `CircuitBreakerDegradationStrategy` 在 HALF_OPEN 状态下委托 `ModelEndpointHealthManager.tryProbe()` 裁定是否允许探测，此交互逻辑比两组件各自独立计时器更复杂，但设计仅以文字段描述，缺少时序图或决策树来明确**熔断器 HALF_OPEN 窗口到期 → 检查端点健康 → 决定是否探测**的完整路径。在编码实现中，开发者需自行推导各状态组合下的行为，易引入逻辑遗漏。
- **所在位置**：§3.2 "统一探测机制"段落（第 805-810 行）
- **严重程度**：中等
- **改进建议**：补充一张简化的时序图或决策表，覆盖以下核心断言：熔断器状态 OPEN/HALF_OPEN、端点健康状态 UNAVAILABLE/CONNECTED/DEGRADED、探测窗口是否到期的组合决策结果。

---

### P8 [轻微] 错误码以字符串字面量散落全篇，无集中定义

- **问题描述**：设计中使用的降级原因/错误码字符串（`"NoAvailableRoute"`、`"EndpointUnavailable"`、`"CircuitBreakerOpen"`、`"ParseFailure"`、`"InternalError"`）以字面量形式出现在 §4.1 伪代码和 §5.1 错误分类表中，无 enum 或常量类定义。多实现者可能拼写不一致（如 `"EndpointUnavailable"` vs `"ENDPOINT_UNAVAILABLE"`），增加维护成本。
- **所在位置**：§4.1 伪代码多处；§5.1 错误分类表
- **严重程度**：轻微
- **改进建议**：在 `ai-api` 或 `ai-impl` 中定义一个 `DegradationReason` 常量类或 enum，集中管理所有降级原因/错误码字符串。在伪代码中引用常量名而非字面量。

---

## 整体评价

产出经过 9 轮迭代和多轮内部审议（设计-验证循环），核心设计决策已比较成熟，技术可行性、类图完整性、状态模型覆盖等方面已有显著改善。上述 8 个问题的核心特征为：**合约文本与行为伪代码之间的不一致、上下游依赖机制未冻结、以及部分值对象的建模遗漏**。这些问题不否定整体设计的合理性，但若在编码前不解决，将导致开发阶段的反复修正和模块间耦合纠纷。建议优先处理 P1~P2（严重级）、P3~P5（重要级）共计 5 个问题，P6~P7 可在下一轮迭代中补充，P8 可留待编码前或代码审查阶段处理。
