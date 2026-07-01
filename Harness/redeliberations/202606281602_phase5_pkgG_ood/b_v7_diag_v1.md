# 质量审查报告 — v7 产出

## 审查范围

本审查侧重需求响应充分度、落地可实施性、逻辑一致性与设计深度，避免重复验证组件 A 内部审议已覆盖的技术可行性维度。

## 发现的 7 个质量问题

### 1. [严重] §2.2 引用不存在的 §1.4 章节，事实性错误

**问题描述**：Line 277（§2.2 依赖方向章节）中声明"变更范围严格限定为 §1.4 变更总结表中列出的 4 项"，但文档中 **不存在 §1.4** 节。文档结构从 §1.3（核心抽象一览）直接跳至 §1.5（多实例行为约束）。该引用在 v7 剥离修订说明后未被同步修正，属于版本清理遗留的事实性错误。实施者将无法找到被引用的"变更总结表"，影响对 ai-api 变更范围的理解。

**所在位置**：§2.2，line 277

**严重程度**：严重

**改进建议**：
- 方案 A：在 §1.3 之后恢复 §1.4「ai-api 变更范围总结表」，内容可参考 §1.6（API Surface 状态表）中的 ai-api 部分及 §9.4 的变更验证
- 方案 B：将 line 277 的引用改为指向 §1.6（API Surface 状态表），并确保 §1.6 中的 ai-api 变更条目完整列出了所有 4 项变更

---

### 2. [重要] LlmChatRequest 类图缺失 `tools` 字段，图-文不一致

**问题描述**：§3.2 文本（line 1443）明确描述 `LlmChatRequest` 包含 `tools: List<ChatToolDefinition>` 字段，并在 §4.1（line 2718）伪代码中通过 `chatRequest.setTools(...)` 实际使用。但是 §2.3 类图中 `LlmChatRequest` 仅显示 3 个字段（`messages`、`options`、`clientType`），缺少 `tools` 字段。实现者若仅参看类图进行编码，将遗漏 `tools` 字段的声明及其与 `ChatToolDefinition` 的关联关系。

**所在位置**：§2.3 类图（line 421–425）；§3.2 文本（line 1443）；§4.1 伪代码（line 2718）

**严重程度**：重要

**改进建议**：在 §2.3 类图的 `LlmChatRequest` 中补充 `+List<ChatToolDefinition> tools` 字段，并添加 `LlmChatRequest → "0..*" ChatToolDefinition : has` 关联线

---

### 3. [重要] 薄适配器 per-capability 超时覆盖机制的伪代码未实现，设计缺口

**问题描述**：§3.1 文本（line 1204–1205）承诺"6 项薄适配器能力可各自覆盖默认超时值，通过在 YAML `ai.execution.timeout.thin-adapter.per-capability` 中按能力标识单独配置。未覆盖的能力使用 `thin-adapter-default` 值"。但 `AbstractCapabilityExecutor` 构造器伪代码（line 1219–1236）仅注入了一个全局 `Duration thinAdapterTimeout`（通过 `@Value("${ai.execution.timeout.thin-adapter-default:30s}")`），未注入 `Map<String, Duration> thinAdapterPerCapabilityConfig`。薄适配器 `doExecuteInternal()`（line 2884）直接使用 `thinAdapterTimeout.toMillis()` 作为委托超时，无运行时根据 `capabilityId` 查找 per-capability 覆盖值的逻辑。这是一个设计层面的缺口——文本承诺了配置灵活性但伪代码未体现实现路径，实现者无法确定 per-capability 超时覆盖应如何注入和解析。

**所在位置**：§3.1 AbstractCapabilityExecutor 构造器伪代码（line 1219–1236）；§3.1 薄适配器超时文本说明（line 1204–1205）；§4.2 薄适配器伪代码（line 2884）；§9.5 YAML 配置（line 3399–3406）

**严重程度**：重要

**改进建议**：在 `AbstractCapabilityExecutor` 构造器中新增 `@Qualifier("thinAdapterPerCapabilityConfig") Map<String, Duration> thinAdapterPerCapabilityConfig` 参数；在薄适配器的 `doExecuteInternal()` 中增加按 `capabilityId` 解析覆盖值的逻辑（例如 `Duration effectiveTimeout = thinAdapterPerCapabilityConfig.getOrDefault(capabilityId, thinAdapterTimeout)`）；在 `AiPlatformConfig` 中补充对应的 `@Bean("thinAdapterPerCapabilityConfig")` 方法

---

### 4. [重要] 降级策略解析逻辑与文本描述矛盾——`this.degradationStrategies` 字段未定义

**问题描述**：文本（line 863–880）描述降级策略注入为"每个 `CapabilityExecutor` 实现通过构造器注入 `Map<String, List<DegradationStrategy>>`，在 `execute()` 中按 `getCapabilityId()` 从 Map 中查找"。但 `execute()` 伪代码（line 1093）直接引用 `this.degradationStrategies` 进行遍历，而该字段在类图和构造器伪代码中均未定义。构造器伪代码（line 1228）注入的是名为 `degradationStrategyMap` 的 Map 类型字段（line 1233 `this.degradationStrategyMap = degradationStrategyMap`），并非 `degradationStrategies`。两个伪代码路径之间缺少了"从 Map 中按 `capabilityId` 解析当前能力策略列表并排序"的中间步骤，实施者无法确定这步逻辑应该放在构造器中还是在 `execute()` 中每次提取。

**所在位置**：§3.1 AbstractCapabilityExecutor execute() 伪代码（line 1093）；§3.1 降级策略注入机制文本（line 863–880）；构造器伪代码（line 1228, 1233）

**严重程度**：重要

**改进建议**：选择并明确两种方案之一——
- 方案 A（推荐）：在 `execute()` 伪代码中将 `this.degradationStrategies` 替换为 `this.degradationStrategyMap.getOrDefault(capabilityId, Collections.emptyList())`，使其与文本描述一致
- 方案 B：保留 `this.degradationStrategies` 字段，在构造器或 `@PostConstruct` 中增加从 Map 解析的步骤，如 `this.degradationStrategies = degradationStrategyMap.getOrDefault(getCapabilityId(), Collections.emptyList()).stream().sorted(Comparator.comparingInt(DegradationStrategy::getOrder)).collect(Collectors.toList())`

---

### 5. [中等] `extractDepartmentIdFromDto()` / `extractVisitIdFromDto()` 等辅助方法未定义

**问题描述**：薄适配器 `doExtractDepartmentId()` 伪代码（line 983）中调用 `extractDepartmentIdFromDto(request)`，类似地 `doExtractVisitId()`（line 990）调用 `extractVisitIdFromDto(request)`。这些作为薄适配器特有辅助方法的调用在伪代码中出现了但从未被正式定义——既不在 `AbstractCapabilityExecutor` 中（因其属于薄适配器特有逻辑），也不在薄适配器子类的伪代码中定义其方法签名和实现策略。实施者需要自行补充这些方法，但无从得知设计者预期的提取策略（反射、JSON 路径查找还是 `instanceof` 链式判断）。

**所在位置**：§3.1 薄适配器 doExtractDepartmentId()（line 983）、doExtractVisitId()（line 990）、doExtractPatientId()（line 997）

**严重程度**：中等

**改进建议**：在薄适配器伪代码前新增辅助方法契约定义段，明确 `extractDepartmentIdFromDto()` 等方法的签名和行为（例如 `protected String extractDepartmentIdFromDto(T request)`，内部使用反射或 `instanceof` 判断 DTO 是否有对应 getter，无则返回 null）。或简化设计，在薄适配器中直接返回 null（因为 Phase 4 DTO 当前为空类，无对应字段），通过注释说明"等 Phase 4 DTO 改造后补充实现"

---

### 6. [中等] `structuredChat()` 路径中 `outputSummary` 在降级前未提取导致部分降级场景丢失输出摘要

**问题描述**：§4.1 伪代码（line 2809）基于成功路径的 `parsedResult` 计算 `outputSummary`。但在 `structuredChat()` 回退到 `chat()+parse()` 后，若 `parse()` 抛出 `ParseException`（line 2777–2779），`outputSummary` 取自 `chatResponse.getContent()`（截断），这是合理的。但若 `structuredChat()` 调用成功但 `AiResult` 为非成功状态（line 2746–2752），降级路径中 `outputSummary` 被传入 null——而此时 LLM 已有回应的文本内容，丢失了可输出到 `AiCallRecord` 的关键信息。同样问题出现在 `LlmInfrastructureException` 捕获路径（line 2801）。这意味着部分降级场景下 `AiCallRecord` 的 `outputSummary` 字段为空，影响运维排查时的可观测性。

**所在位置**：§4.1 doExecuteInternal() 伪代码（line 2744–2752 structuredChat AiResult 非成功降级；line 2794–2807 LlmInfrastructureException 降级）

**严重程度**：中等

**改进建议**：在上述两个降级路径中，在调用 `doDegrade()` 之前添加 `outputSummary` 提取逻辑——例如从 `chatResponse`（若有）或异常消息中提取可用摘要。具体来说，在 line 2746 之前可尝试 `outputSummary = StringUtils.truncate(结构化输出中已提取的部分, 500)`；在 line 2801 中已有 `outputSummary = StringUtils.truncate(e.getMessage(), 500)`，但应将该值传入 `doDegrade()` 而非保持 null

---

### 7. [中等] `ModelEndpointHealthManager` 的 DEGRADED→CONNECTED 恢复路径缺少失败计数清零定义

**问题描述**：§3.2 状态转换表（line 1358）定义 DEGRADED→CONNECTED 的触发条件为"1 次调用正常（耗时 < 阈值）"，但未说明**失败计数**（即导致进入 DEGRADED 的连续超阈值计数）的清除策略。若状态设计使用计数器追踪超阈值次数（如"连续 3 次超阈值进入 DEGRADED"），则恢复路径仅关注单次正常调用是否足够重置计数器，还是需要连续 N 次正常调用才回退 CONNECTED。此模糊点可能导致状态抖动（一次正常调用后立即回退 CONNECTED，下一次稍慢再次进入 DEGRADED）。

**所在位置**：§3.2 ModelEndpointHealthManager 状态转换表（line 1353–1364）

**严重程度**：中等

**改进建议**：在状态转换表下方补充`计数器重置策略`说明——建议：DEGRADED→CONNECTED 恢复到连续 3 次正常调用（耗时 < 阈值）才正式回退，单次正常调用不重置计数器，或明确采用"单次正常即回退"并接受状态抖动风险

---

## 整体评价

文档结构完整、内容详实，经过 6 轮修复后在大面质量上已达到较高水平。以上 7 个问题中前 4 个（严重/重要级）属于**设计落地缺口**，即文本承诺与伪代码之间、不同章节之间存在可验证的不一致，修复者可在当前产出上直接定位和修正。后 3 个（中等级）属于设计和可观测性的辅助完善建议。
