# Phase 5 包 G OOD 设计质量审查报告（v3）

审查时间：2026-06-28
审查轮次：第 3 次
审查维度：需求响应充分度、事实错误与逻辑矛盾、深度与完整性（侧重内部审议未充分覆盖的方向）

---

## 1. 需求响应充分度

### 1.1 OOD 核心要素覆盖

需求明确要求覆盖"类图、核心职责、协作关系、关键接口、状态模型等 OOD 核心要素"。当前产出已提供完整的 Mermaid 类图（§2.3）、核心职责表格（§1.3）、协作关系（类图关联箭头 + §2.2 依赖方向图）、关键接口方法签名（§3 各章节）、状态模型（熔断器 §3.8、端点健康 §3.2、CredentialProvider §3.2、PromptTemplate §3.3、Experiment §3.4）。**OOD 要素覆盖充分。**

### 1.2 需求范围对应性

产出覆盖了需求要求的 Phase 5 包 G 全部 13 项 AI能力（7 项底座能力 + 6 项薄适配器能力），并给出了 §3.11 中 4 项首次落地能力的特化设计、§9.1 迁移路径表、§1.1 设计目标中的 5 个目标均有对应设计章节。**需求范围对应完整。**

### 1.3 深度评价

产出延续了 25 轮迭代的精炼程度，伪代码、YAML 配置示例、转换条件表、类图同步等方面均达到 OOD 阶段的较高深度。但以下方面的需求响应存在不足：

**问题 1（重要 — 完整性不足）：§3.11 覆盖范围与需求不对称**

- **描述**：§3.11 标题为"Phase 5 首次落地底座能力特化设计"，仅覆盖 4 项能力：KbQuery（3.4.8）、PrescriptionAssist（3.4.10）、Schedule（3.4.12）、DiscussionConclusion（3.4.13）。但其中 PrescriptionAssist（3.4.10）源自 Phase 2~4 迁移而非首次落地，而真正的 3 项迁移能力（Triage 3.4.1、PrescriptionCheck 3.4.2、MedicalRecordGen 3.4.3）缺少同等级别的 9 维度特化设计表。
- **所在位置**：§3.11（第 3219-3281 行）
- **严重程度**：重要
- **改进建议**：要么将 §3.11 标题改为"Phase 5 底座能力特化设计"并将 7 项底座能力全部覆盖；要么新增 §3.12 覆盖 3 项迁移能力的特化设计表。实施时这 3 项能力的 DTO 字段定义、Prompt 模板结构、结构化解析目标类型等依赖实现者自行推导，增加了实施偏差风险。

---

## 2. 事实错误与逻辑矛盾

### 问题 2（严重 — 事实错误 / 编译不可行）：`AbstractCapabilityExecutor` 构造器伪代码中的 `@Qualifier("{capabilityId}Strategies")` 无法在 Spring 容器中正确解析

- **描述**：§3.1 降级策略注入机制（第 834 行）和构造器伪代码（第 1154 行）显示 `@Qualifier("{capabilityId}Strategies") List<DegradationStrategy> strategies`。但 Spring 的 `@Qualifier` 注解值是编译期常量字符串，不会将 `"{capabilityId}Strategies"` 按能力标识动态替换——容器会查找名为字面值 `"{capabilityId}Strategies"` 的 Bean，该 Bean 不存在，导致 `NoSuchBeanDefinitionException`。与此同时，§3.1 YAML→Bean 装配路径（第 837-852 行）描述了完全不同的机制：`AiPlatformConfig` 构建 `Map<String, List<DegradationStrategy>>` 然后注入。两套机制互斥且 `@Qualifier` 机制在 Java 代码层面不可行。
- **所在位置**：§3.1（第 834 行、第 1146-1162 行）、§3.8（第 2051-2060 行）
- **严重程度**：严重
- **改进建议**：
  1. 明确选择一套机制并删除另一套的伪代码。推荐的方案是保留 `AiPlatformConfig` 中的 Map 构建路径（已给出 6 步过程），删除或修正构造器中的 `@Qualifier` 伪代码，改为注入 `Map<String, List<DegradationStrategy>>` 后在 `execute()` 中按 `getCapabilityId()` 查找。
  2. 如果坚持 `@Qualifier` 方案，则需为每个子类提供独立的构造器签名（如 `TriageCapabilityExecutor(@Qualifier("triageStrategies") List<DegradationStrategy> strategies, ...)`），并在 `AbstractCapabilityExecutor` 中消除该占位符。

### 问题 3（重要 — 逻辑矛盾）：降级策略注入机制存在两套并行且互斥的描述

- **描述**：§3.1 第 834 行和 §3.8 第 2051 行描述降级策略通过 `@Qualifier` 按能力标识注入到每个 `CapabilityExecutor`。但 §3.1 第 837-852 行描述了完全不同的策略装配路径——`AiPlatformConfig` 构建 `Map<String, List<DegradationStrategy>>` 并作为 `@Bean` 暴露，各 Executor 按 `getCapabilityId()` 选择。两套机制在最终实现层面二选一即可，但当前文档同时保留两者且未说明哪一套是最终的，实现者会困惑。
- **所在位置**：§3.1（第 831-852 行）、§3.8（第 2048-2060 行）、`AbstractCapabilityExecutor` 构造器伪代码（第 1141-1162 行）
- **严重程度**：重要
- **改进建议**：
  1. 统一为一套机制。推荐的 Map 方案（第 837-852 行）是明确可实现的，且已在 §7 设计决策表中记录。删除所有 `@Qualifier` 按能力注入的表述，改为"CapabilityExecutor 构造器注入 `Map<String, List<DegradationStrategy>>`，在 `execute()` 按 `getCapabilityId()` 查找对应策略列表"。
  2. 同步更新 `AbstractCapabilityExecutor` 构造器伪代码，将 `@Qualifier("{capabilityId}Strategies") List<DegradationStrategy> strategies` 替换为 `Map<String, List<DegradationStrategy>> degradationStrategyMap`。

### 问题 4（重要 — 逻辑矛盾 / 事实不匹配）：底座能力条件化注册机制的描述与实际设计不一致

- **描述**：§3.1 第 899 行声称"完整管线型 CapabilityExecutor（7 项底座能力）同样继承此条件（`AiPlatformConfig` 中统一注册条件，见 §3.9）"。但 §3.9 的 `AiPlatformConfig` 并未通过 `@Bean` 方法显式注册任何 `CapabilityExecutor`，各 Executor 通过 `@Component` 自注册。第 880 行的薄适配器示例显示 `@ConditionalOnProperty` 直接标注在类上。如果 7 项底座能力也使用同样的 `@Component` + `@ConditionalOnProperty` 模式，则"在 AiPlatformConfig 中统一注册"的描述与实际情况不符。
- **所在位置**：§3.1（第 899 行）、§3.9（第 2080-2201 行，无对应统一注册逻辑）
- **严重程度**：重要
- **改进建议**：
  1. 明确 CapabilityExecutor 的注册策略：13 个 Executor 都是 `@Component` + `@ConditionalOnProperty` 自注册，还是由 `AiPlatformConfig` 通过 `@Bean` 统一注册？如果选择 `@Component` 自注册，删除"在 AiPlatformConfig 中统一注册条件"的表述。
  2. 如果选择 `@Bean` 统一注册（即 `AiPlatformConfig` 中为 13 个 Executor 各写一个 `@Bean` 方法），则需在 §3.9 补充对应的 `@Bean` 方法代码。

### 问题 5（重要 — 事实错误）：薄适配器 `doExtractDepartmentId()` 的非 HTTP 回退描述引用 Prompt 模板回退，但薄适配器不使用 Prompt 模板

- **描述**：§3.1 第 903-904 行说明薄适配器 `doExtractDepartmentId()` 无法获取 departmentId 时"Prompt 模板回退到通用模板"。但 §3.1 第 861-865 行明确薄适配器**不包含**模板渲染步骤。因此 departmentId 不可用时的"Prompt 模板回退"描述在薄适配器场景下是误导性的——薄适配器中 departmentId 仅用于 `AiCallRecord` 记录，不涉及 Prompt 模板检索。
- **所在位置**：§3.1（第 903-904 行的`doExtractDepartmentId()` 说明）
- **严重程度**：重要
- **改进建议**：将"Prompt 模板回退到通用模板"改为"departmentId 保持 null，对应字段在 AiCallRecord 中可空"，与 §3.10 的薄适配器 DTO 过渡策略保持一致。

### 问题 6（中等 — 逻辑矛盾）：§3.11 节标题称"首次落地"但包含迁移能力

- **描述**：§3.11 标题为"Phase 5 首次落地底座能力特化设计"，覆盖的 4 项能力中 PrescriptionAssist（3.4.10）来源于 Phase 2~4 已有的能力迁移，并非"首次落地"。同时，3 项真正的迁移能力（Triage 3.4.1、PrescriptionCheck 3.4.2、MedicalRecordGen 3.4.3）未被覆盖。
- **所在位置**：§3.11 标题（第 3219 行）
- **严重程度**：中等
- **改进建议**：标题改为"Phase 5 底座能力特化设计"并将范围扩展至 7 项底座能力；或保持 4 项并修正标题为"Phase 5 底座能力特化设计（部分）"且补充说明其余 3 项能力遵循通用模板方法。

---

## 3. 深度与完整性

### 问题 7（重要 — 完整性不足）：`ExperimentGroup` 实体未被正式定义

- **描述**：`Experiment` 类（§3.4）字段中包含 `groups: List<ExperimentGroup>`，各实验的分组流量百分比分配依赖此实体。"哈希值 % 1000 映射到流量百分比区间"（§4.4）的逻辑也隐式依赖 `ExperimentGroup` 的 `percentage` 字段。但 `ExperimentGroup` 从未在核心抽象表（§1.3）、类图（§2.3）或独立段落中定义，实现者不知道此实体的字段结构、JPA 映射关系或业务约束。
- **所在位置**：§3.4 第 1670-1680 行的 Experiment 类定义、§4.4 第 2582-2590 行
- **严重程度**：重要
- **改进建议**：在 §3.4（Experiment 章节）或新增子章节中补充 `ExperimentGroup` 的完整定义：字段表（groupId、percentage、targetModelId、targetPromptVersion 等）、JPA Entity 注解、与 Experiment 的关联关系（`@OneToMany` 等）、流量分配算法约束（各分组百分比之和 = 100% 等）。

### 问题 8（重要 — 完整性不足）：`AiCallLogStats` 汇总统计表未被正式定义

- **描述**：§3.5 第 1874 行描述的分区清理策略中，过期分区数据在被删除前会汇总写入 `ai_call_log_stats` 表（保留 3 年）。但此表未被正式定义——无字段结构、无索引策略、无 Entity 类引用。考虑到该表是数据生命周期管理的关键组件，缺失定义将影响实现者对清理策略的落地。
- **所在位置**：§3.5（第 1874 行）
- **严重程度**：重要
- **改进建议**：在 §3.5 的"数据保留与清理策略"子节中补充 `ai_call_log_stats` 表的完整字段定义（主要字段：stat_month、capability_id、total_calls、success_count、degraded_count、failure_count、p50_elapsed_ms、p95_elapsed_ms、p99_elapsed_ms、created_at 等），索引策略，及写入/查询伪代码。

### 问题 9（重要 — 完整性不足）：`LocalRuleFallback` 未定义"最小安全规则"的具体内容

- **描述**：`PrescriptionLocalRuleFallback` 被引用为 3.4.2 处方审核的本地规则降级实现（§3.7 第 1955 行），仅描述为"执行 3.4.2 规定的本地规则校验最小检查项"。对于处方审核这一影响患者安全的医疗关键场景，未定义降级后执行的"最小检查项"具体是什么（如：配伍禁忌检查、剂量范围检查、重复用药检查等），也未定义本地规则的数据来源（硬编码规则 vs. 数据库配置规则）。
- **所在位置**：§3.7（第 1949-1955 行）
- **严重程度**：重要
- **改进建议**：在 §3.7 中补充 `PrescriptionLocalRuleFallback` 的具体行为契约——列出至少 3~5 项降级时必须执行的最小检查规则（如禁忌症检查、剂量上限检查、重复用药检查），说明规则来源（本地规则引擎 vs 配置化规则集），及在规则执行失败时是否返回"审核通过"还是"审核失败"的安全策略。

### 问题 10（重要 — 深度不足）：`ModelRoute.parameters` 未定义扩展点，常见 LLM 参数被静默忽略

- **描述**：§3.2 `LlmChatOptions` 的两阶段填充策略（第 1411-1417 行）仅定义了 `temperature`、`maxTokens`、`stopSequences` 三个 key 从 `ModelRoute.parameters` Map 映射到强类型字段。但 `topP`、`frequencyPenalty`、`presencePenalty`、`seed` 等常见 LLM 参数被静默忽略——Map 中存在这些 key 但 `LlmChatOptions` 无对应字段，不报错也不告警。这会在运维配置时产生"参数设了但没生效"的隐性问题。
- **所在位置**：§3.2（第 1401-1417 行）、§9.5 YAML 示例（仅配置了 temperature 和 maxTokens）
- **严重程度**：重要
- **改进建议**：
  1. 在 `LlmChatOptions` 中至少补充 `topP`（Double）、`frequencyPenalty`（Double）、`presencePenalty`（Double）三个常用字段。
  2. 或在 §3.2 新增说明规则：新增参数需同步更新 `LlmChatOptions` 的字段定义和两阶段填充逻辑，并在治理规则（如 §10.3）中新增"参数映射扩展点"检查项。
  3. 同时建议在 LlmChatService 实现层记录 WARN 日志，检测到 `ModelRoute.parameters` 中存在未映射的 key 时输出来辅助调试。

### 问题 11（中等 — 深度不足）：DiscussionConclusionCapabilityExecutor 的 transcriptSummary 前置 LLM 调用未定义超时和失败策略

- **描述**：§3.11.4 第 3271 行描述若 `transcripts` 原始文本超过 3000 tokens，使用"首轮 LLM 调用压缩为摘要"注入 `transcriptSummary`。但此预处理的 LLM 调用未经设计：使用 `chat()` 还是 `structuredChat()`？超时如何管理（是否消耗 `capabilityTimeout` 额度）？如果此预处理 LLM 调用本身失败，会导致主管线也失败吗？当前设计将其视为变量提取步骤的一部分，但变量提取发生在 `doExecuteInternal()` 的 `extractVariables()` 中，此方法在 §3.1 中被描述为"可选重写"，未定义其内可执行 LLM 调用。
- **所在位置**：§3.11.4（第 3271 行）、§3.1 的 `extractVariables()` 定义（第 1065-1068 行）
- **严重程度**：中等
- **改进建议**：
  1. 明确此预处理 LLM 调用的契约：使用的 `LlmChatService` 方法（建议 `chat()`）、独立的超时管理（不应消耗 `capabilityTimeout` 全局配额）、调用失败时的降级行为（回退到截断原文而非放弃整条管线）。
  2. 在 §3.11.4 中新增"transcriptSummary 预处理策略"子段落，涵盖上述设计。
  3. 在 `extractVariables()` 的伪代码或注释中补充说明：子类可在此方法内执行 LLM 调用，但需注意超时管理不依赖于外层 `orTimeout`（`extractVariables()` 在 `doExecuteInternal()` 内部，受外层 `orTimeout` 保护，但若预处理调用耗时过长将消耗全局超时窗口）。

### 问题 12（中等 — 深度不足）：`extractCallerRole()` 未定义 Spring Security GrantAuthority 的过滤规则

- **描述**：§3.1 第 1098-1109 行的 `extractCallerRole()` 取第一个 `GrantedAuthority.getAuthority()` 的返回值作为角色。但 Spring Security 典型配置中 `GrantedAuthority` 可能是 `"ROLE_DOCTOR"`（带 ROLE_ 前缀）或 `"SCOPE_read"`（OAuth2 范围）等非角色字符串。当前设计未定义是否过滤 ROLE_ 前缀，也未定义对于 `"ROLE_DOCTOR,ROLE_NURSE"` 等多 authority 场景的拼接规则（虽然注释说明了"多 authority 场景少见"选择了取第一个的策略）。
- **所在位置**：§3.1（第 1098-1109 行）
- **严重程度**：中等
- **改进建议**：在 `extractCallerRole()` 的注释或设计说明中补充一条显式规则：提取后是否需去除 `"ROLE_"` 前缀（若保留则 `AiCallLogEntity.caller_role` 字段的 VARCHAR(20) 长度需评估）。推荐方案：取第一个以 `"ROLE_"` 开头的 `GrantedAuthority` 并去除前缀；若无 ROLE_ 前缀的 authority 则取第一个 authority 的原始值。

---

## 4. 整体质量评价

产出经过 25 轮迭代，在技术深度、伪代码精确度、边界场景覆盖等方面已达到较高的 OOD 设计质量。类图与文本描述保持同步，状态模型定义形式化（转换条件表），测试策略章节覆盖了单元/集成/并发竞争/状态恢复等全方位覆盖。

**已由内部审议覆盖且无新增问题的维度**：技术可行性评估、模块职责划分、类图与协作关系完整性、核心接口方法签名定义、降级策略扩展机制、Bean 装配互斥策略、YAML 配置绑定、多实例部署约束记录——这些在第 2 轮已经历审议并修复的系统性问题未在本次审查中发现回归。

**本次审查发现的核心问题集中在**：

1. **需求响应偏差**：§3.11 标题与内容的范围不匹配（问题 6），3 项迁移能力的特化设计缺失（问题 1）。
2. **实现级事实错误**：`@Qualifier("{capabilityId}Strategies")` 在 Spring 中无法动态解析（问题 2），两个互斥的策略注入机制并行描述（问题 3），条件化注册机制描述与设计不一致（问题 4），薄适配器 departmentId 回退描述引用不存在的 Prompt 模板（问题 5）。
3. **实体/组件定义的遗漏**：`ExperimentGroup`（问题 7）、`AiCallLogStats`（问题 8）未被正式定义。
4. **关键场景的深度不足**：`PrescriptionLocalRuleFallback` 的最小安全规则未定义（问题 9），`ModelRoute.parameters` 的扩展点缺失（问题 10），DiscussionConclusion 的预处理 LLM 调用未定义超时和失败策略（问题 11），`extractCallerRole()` 的 GrantAuthority 规则未定义（问题 12）。

这些问题中，**问题 2、3、4** 属于实现级事实错误，修复者需优先关注以消除实现歧义。**问题 1、7、8、9** 属于完整性缺口，建议在下一轮补齐。
