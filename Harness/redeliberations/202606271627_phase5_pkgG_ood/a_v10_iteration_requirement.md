根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### P1 [严重] 薄适配器 CapabilityExecutor 的 Phase 4 服务依赖机制未定义
- **问题描述**：产出定义 6 个薄适配器型 CapabilityExecutor 通过"直接委托 Phase 4 业务服务"执行，但未定义 ai-impl 模块如何获取 Phase 4 服务的依赖——是直接添加 Maven 依赖？还是通过 OSGi/SPI/事件机制解耦？亦或是 Phase 4 服务实现某个统一接口？当前产出中仅写 `result = phase4ServiceDelegate.execute(request)` 而未定义 `phase4ServiceDelegate` 的注入来源和模块依赖方向。若 ai-impl 直接依赖 Phase 4 模块，将在模块间引入反向依赖，可能破坏模块分层。
- **所在位置**：§3.1 薄适配器型 CapabilityExecutor 的管线行为段落及伪代码；§2.2 模块依赖方向图
- **严重程度**：严重
- **改进建议**：在 §2.2 依赖方向图中明确 ai-impl 与 Phase 4 模块的依赖关系。若采用薄适配器模式，需定义 Phase 4 服务的 SPI 接口（归属 ai-api 或独立适配器包），或明确声明 ai-impl 直接依赖 Phase 4 模块。同时补充对应协作对象注入方式的说明（`@Autowired`、构造器注入、Bean 名称约定等）。

### P2 [严重] §4.1 核心伪代码中存在未定义变量
- **问题描述**：`doExecuteInternal()` 伪代码中使用了 `inputSummary`、`outputSummary`、`retryCount`、`outputType` 四个变量，这些变量在当前方法体和调用上下文中均未定义。
- **所在位置**：§4.1 `doExecuteInternal()` 伪代码（第 1230-1256 行）
- **严重程度**：严重
- **改进建议**：在伪代码中显式定义这四个变量的来源。例如：`inputSummary = StringUtils.truncate(request.toString(), 500)`、`outputSummary = StringUtils.truncate(parsedResult.toString(), 500)`、`retryCount = llmResponse.getRetryCount()`（若在 LlmResponse 中添加此字段）或 `= 0`、`outputType = getOutputType()` 在方法入口处赋值。

### P3 [重要] 防御性拷贝合约在伪代码中未兑现
- **问题描述**：§3.1 明确声明若现有 DTO 无法改为不可变，则在模板方法入口处对 `request` 执行防御性拷贝。但 §4.1 的 `execute()` 伪代码中 `request` 被直接传入 lambda 和 `doExecuteInternal()`，没有任何防御性拷贝步骤。
- **所在位置**：§3.1 vs §4.1 `AbstractCapabilityExecutor.execute()` 伪代码（第 1203-1225 行）
- **严重程度**：重要
- **改进建议**：在 §4.1 `execute()` 伪代码的 ThreadLocal 提取步骤之后、`supplyAsync()` 之前，增加防御性拷贝步骤：`request = objectMapper.convertValue(request, request.getClass())`。或以注释明确说明"如 DTO 已为不可变对象则跳过此步骤"。

### P4 [重要] TokenUsage 类未建模
- **问题描述**：LlmResponse 包含 `getTokenUsage()` 方法，返回值 `TokenUsage` 在各处被引用，但 `TokenUsage` 既未出现在 §1.3 核心抽象一览表、§2.3 类图，也未在 §3.2 以独立值对象定义。
- **所在位置**：§3.2 `LlmResponse` 类定义，§4.1 伪代码调用点
- **严重程度**：重要
- **改进建议**：在 §1.3 核心抽象表中补充 `TokenUsage` 行，在 §2.3 类图中补充 `TokenUsage` 类定义（字段：promptTokens/Integer、completionTokens/Integer、totalTokens/Integer），或在其值对象段落中补充定义。

### P5 [重要] ExperimentManager 未定义无实验时的返回值语义
- **问题描述**：§4.1 伪代码中 `assignment.getTargetPromptVersion()` 被直接传入 `PromptTemplateManager.render()`。若 assignment 为 null 则 `getTargetPromptVersion()` 将抛出 NPE；若为非 null 但 targetPromptVersion 为 null 则使用默认模板。"空 assignment"是指 `null` 引用还是 `group="default"` 且所有字段为 null 的非 null 对象——absence 语义未冻结。
- **所在位置**：§3.4 `ExperimentAssignment` 定义；§4.3 分流契约
- **严重程度**：重要
- **改进建议**：冻结"无实验命中"的返回值语义：明确返回非 null 的 `ExperimentAssignment`（`group="default"`，`targetModelId=null`，`targetPromptVersion=null`），并在类中说明调用者无需判空。或使用 `Optional<ExperimentAssignment>` 返回类型并在 §4.1 伪代码中体现 null 检查。

### P6 [中等] Phase 4 DTO 过渡策略与实际执行伪代码存在矛盾
- **问题描述**：§3.5 过渡策略声明"6 项 Phase 4 能力的 DTO 暂维持现状，不继承 AiRequestBase"，但 §3.1 薄适配器伪代码中 `doExecuteInternal()` 调用 `request.getVisitId()`、`request.getPatientId()`——这些方法在 `AiRequestBase` 中定义，Phase 4 DTO 尚未继承，不存在这些方法。
- **所在位置**：§3.5 "过渡策略"段落 vs §3.1 薄适配器伪代码
- **严重程度**：中等
- **改进建议**：1. 统一薄适配器对各公共字段的提取策略：所有 Phase 4 DTO 上缺失的字段（visitId、patientId 等）均应通过独立的提取路径获取（如从 RequestContext/HTTP Header 提取）。2. 在薄适配器 `doExecuteInternal()` 伪代码中体现此提取，而非假设 Phase 4 DTO 已有 `getVisitId()`/`getPatientId()` 方法。

### P7 [中等] 熔断器与端点健康管理器的统一探测机制缺少状态转换图
- **问题描述**：§3.2 描述了 `CircuitBreakerDegradationStrategy` 在 HALF_OPEN 状态下委托 `ModelEndpointHealthManager.tryProbe()` 裁定是否允许探测，此交互逻辑比两组件各自独立计时器更复杂，但设计仅以文字段描述，缺少时序图或决策树。
- **所在位置**：§3.2 "统一探测机制"段落（第 805-810 行）
- **严重程度**：中等
- **改进建议**：补充一张简化的时序图或决策表，覆盖以下核心断言：熔断器状态 OPEN/HALF_OPEN、端点健康状态 UNAVAILABLE/CONNECTED/DEGRADED、探测窗口是否到期的组合决策结果。

### P8 [轻微] 错误码以字符串字面量散落全篇，无集中定义
- **问题描述**：降级原因/错误码字符串（`"NoAvailableRoute"`、`"EndpointUnavailable"`、`"CircuitBreakerOpen"`、`"ParseFailure"`、`"InternalError"`）以字面量形式出现在 §4.1 伪代码和 §5.1 错误分类表中，无 enum 或常量类定义。
- **所在位置**：§4.1 伪代码多处；§5.1 错误分类表
- **严重程度**：轻微
- **改进建议**：在 `ai-api` 或 `ai-impl` 中定义一个 `DegradationReason` 常量类或 enum，集中管理所有降级原因/错误码字符串。在伪代码中引用常量名而非字面量。

## 历史迭代回顾

分析历史反馈（第 9 轮）与当前反馈（第 10 轮）的关系：

- **已解决的问题**：无。第 9 轮识别的 7 个问题在本轮诊断报告中全部继续存在，未有任何问题被解决。
- **持续存在的问题**：
  - P1（薄适配器 Phase 4 服务依赖机制未定义）— 自第 9 轮持续存在，本轮仍标记为严重
  - P2（核心伪代码未定义变量）— 自第 9 轮持续存在，本轮仍标记为严重
  - P3（防御性拷贝合约未兑现）— 自第 9 轮持续存在，本轮仍标记为重要
  - P4（TokenUsage 未建模）— 自第 9 轮持续存在，本轮仍标记为重要
  - P5（ExperimentManager 返回值语义未冻结）— 自第 9 轮持续存在，本轮仍标记为重要
  - P6（Phase 4 DTO 过渡策略与伪代码矛盾）— 自第 9 轮持续存在，本轮仍标记为中等
  - P7（统一探测机制缺少状态转换图）— 自第 9 轮持续存在，本轮仍标记为中等
- **新发现的问题**：
  - P8（错误码以字符串字面量散落全篇，无集中定义）— 本轮新识别的问题，严重程度：轻微

## 上一轮产出路径
C:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606271627_phase5_pkgG_ood/a_v9_copy_from_v8.md

## 用户需求
C:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606271627_phase5_pkgG_ood/requirement.md
