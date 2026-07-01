根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 问题1（重要 — 完整性不足）：§3.11 覆盖范围与需求不对称
§3.11 标题为"Phase 5 首次落地底座能力特化设计"，仅覆盖 4 项能力，但 PrescriptionAssist 源自 Phase 2~4 迁移而非首次落地，而真正的 3 项迁移能力（Triage、PrescriptionCheck、MedicalRecordGen）缺少同等级别的 9 维度特化设计表。
- **所在位置**：§3.11（第 3219-3281 行）
- **改进建议**：将 §3.11 标题改为"Phase 5 底座能力特化设计"并将 7 项底座能力全部覆盖；或新增 §3.12 覆盖 3 项迁移能力的特化设计表。

### 问题2（严重 — 事实错误/编译不可行）：`@Qualifier("{capabilityId}Strategies")` 无法在 Spring 容器中正确解析
Spring 的 `@Qualifier` 注解值是编译期常量字符串，不会按能力标识动态替换，容器会查找字面值 `"{capabilityId}Strategies"` 导致 NoSuchBeanDefinitionException。同时 §3.1 YAML→Bean 装配路径描述了完全不同的 Map 机制，两套互斥。
- **所在位置**：§3.1（第 834 行、第 1146-1162 行）、§3.8（第 2051-2060 行）
- **改进建议**：明确选择一套机制。推荐保留 AiPlatformConfig 中的 Map 构建路径，删除或修正构造器中的 @Qualifier 伪代码，改为注入 `Map<String, List<DegradationStrategy>>` 后在 `execute()` 中按 `getCapabilityId()` 查找。

### 问题3（重要 — 逻辑矛盾）：降级策略注入机制存在两套并行且互斥的描述
§3.1 第 834 行和 §3.8 第 2051 行描述 `@Qualifier` 按能力注入，但 §3.1 第 837-852 行描述了完全不同的 Map 装配路径。
- **所在位置**：§3.1（第 831-852 行）、§3.8（第 2048-2060 行）、AbstractCapabilityExecutor 构造器伪代码（第 1141-1162 行）
- **改进建议**：统一为一套 Map 方案，删除所有 `@Qualifier` 按能力注入的表述，将构造器伪代码中注入类型改为 `Map<String, List<DegradationStrategy>>`。

### 问题4（重要 — 逻辑矛盾/事实不匹配）：底座能力条件化注册机制描述与实际设计不一致
§3.1 第 899 行声称"在 AiPlatformConfig 中统一注册条件"，但 §3.9 的 AiPlatformConfig 并未显式注册任何 CapabilityExecutor，各 Executor 通过 `@Component` 自注册。
- **所在位置**：§3.1（第 899 行）、§3.9（第 2080-2201 行）
- **改进建议**：明确 Executor 注册策略。若选择 `@Component` 自注册则删除"在 AiPlatformConfig 中统一注册条件"的表述；若选择 `@Bean` 统一注册则在 §3.9 补充对应代码。

### 问题5（重要 — 事实错误）：薄适配器 `doExtractDepartmentId()` 回退描述引用不存在的 Prompt 模板
§3.1 第 903-904 行说明薄适配器无法获取 departmentId 时"Prompt 模板回退到通用模板"，但薄适配器不包含模板渲染步骤。
- **所在位置**：§3.1（第 903-904 行）
- **改进建议**：将"Prompt 模板回退到通用模板"改为"departmentId 保持 null，对应字段在 AiCallRecord 中可空"。

### 问题6（中等 — 逻辑矛盾）：§3.11 节标题称"首次落地"但包含迁移能力 PrescriptionAssist
- **所在位置**：§3.11 标题（第 3219 行）
- **改进建议**：标题改为"Phase 5 底座能力特化设计"并将范围扩展至 7 项底座能力；或保持 4 项并修正标题且补充说明其余 3 项遵循通用模板方法。

### 问题7（重要 — 完整性不足）：`ExperimentGroup` 实体未被正式定义
Experiment 类字段包含 `groups: List<ExperimentGroup>`，哈希分桶逻辑也隐式依赖 ExperimentGroup 的 percentage 字段，但从未在核心抽象表、类图或独立段落中定义。
- **所在位置**：§3.4 第 1670-1680 行、§4.4 第 2582-2590 行
- **改进建议**：在 §3.4 中补充 ExperimentGroup 的完整定义：字段表、JPA Entity 注解、与 Experiment 的关联关系、流量分配算法约束。

### 问题8（重要 — 完整性不足）：`AiCallLogStats` 汇总统计表未被正式定义
§3.5 第 1874 行描述的分区清理策略中，过期分区数据会汇总写入 ai_call_log_stats 表，但此表无字段结构、无索引策略、无 Entity 类引用。
- **所在位置**：§3.5（第 1874 行）
- **改进建议**：在 §3.5 中补充 ai_call_log_stats 表的完整字段定义、索引策略及写入/查询伪代码。

### 问题9（重要 — 完整性不足）：`PrescriptionLocalRuleFallback` 未定义"最小安全规则"的具体内容
§3.7 第 1955 行仅描述为"执行本地规则校验最小检查项"，未定义降级后具体执行哪些检查、规则数据来源、执行失败时的安全策略。
- **所在位置**：§3.7（第 1949-1955 行）
- **改进建议**：补充 PrescriptionLocalRuleFallback 的具体行为契约——列出至少 3~5 项最小检查规则、规则来源及执行失败时的安全策略。

### 问题10（重要 — 深度不足）：`ModelRoute.parameters` 未定义扩展点，常见 LLM 参数被静默忽略
LlmChatOptions 两阶段填充策略仅定义了 temperature、maxTokens、stopSequences 三个 key，topP、frequencyPenalty、presencePenalty 等被静默忽略。
- **所在位置**：§3.2（第 1401-1417 行）、§9.5 YAML 示例
- **改进建议**：在 LlmChatOptions 中至少补充 topP、frequencyPenalty、presencePenalty 三个字段，或在 §3.2 新增参数映射扩展点规则说明。

### 问题11（中等 — 深度不足）：DiscussionConclusionCapabilityExecutor 的 transcriptSummary 前置 LLM 调用未定义超时和失败策略
§3.11.4 第 3271 行描述若 transcripts 超过 3000 tokens 使用首轮 LLM 调用压缩，但此预处理调用未定义使用的方法、超时管理、失败降级行为。
- **所在位置**：§3.11.4（第 3271 行）、§3.1 的 extractVariables() 定义（第 1065-1068 行）
- **改进建议**：明确预处理 LLM 调用的契约——使用的 LlmChatService 方法、独立超时管理、失败时回退到截断原文。

### 问题12（中等 — 深度不足）：`extractCallerRole()` 未定义 Spring Security GrantAuthority 的过滤规则
§3.1 第 1098-1109 行取第一个 GrantedAuthority，但未定义 ROLE_ 前缀处理和多 authority 场景的拼接规则。
- **所在位置**：§3.1（第 1098-1109 行）
- **改进建议**：补充显式规则：取第一个以 "ROLE_" 开头的 GrantedAuthority 并去除前缀；若无 ROLE_ 前缀则取第一个 authority 的原始值。

## 历史迭代回顾

### 已解决的问题（出现在历史反馈中，当前反馈不再提及）
以下 6 项问题在第 2 轮审查中发现，在第 3 轮及当前审查中不再出现，说明已在第 3 轮迭代中得到解决：
1. "不变"声明与实质性变更之间的系统性矛盾（第 2 轮问题 1）
2. 多实例部署场景下三个组件的跨实例行为未定义（第 2 轮问题 2）
3. 伪代码引用未标注 API Surface 状态（第 2 轮问题 3）
4. Jackson 兼容性测试断言无法验证真实风险（第 2 轮问题 4）
5. structuredChat() 未定义 tool_use/JSON mode 检测/超时叠加风险（第 2 轮问题 5）
6. FallbackAiService.applyStrategies() 条件开关缺少 YAML 配置绑定（第 2 轮问题 6）

### 持续存在的问题（在多轮反馈中反复出现，需重点解决）
以下 12 项问题在第 3 轮审查中首次提出，在本轮（第 4 轮）审查中仍然存在，表明第 3 轮迭代未能有效修复：
- **问题2/3**（@Qualifier 动态解析不可行 + 降级策略注入机制互斥描述）：被视为"严重"级别的事实错误，需最优先修正。第 3 轮迭代后仍未清理伪代码或统一机制。
- **问题1/6**（§3.11 覆盖范围不对称 + 标题错误）：被视为"重要"级别，标题和范围均需修正，第 3 轮未做任何调整。
- **问题4**（条件化注册机制描述不一致）：需要在 "统一注册" 和 "自注册" 之间选择并修正对应表述。
- **问题5**（薄适配器 departmentId 回退描述）：文字层面的修正未执行。
- **问题7/8/9**（ExperimentGroup、AiCallLogStats、PrescriptionLocalRuleFallback 定义缺失）：三项实体/组件的完整定义在第 3 轮均未补充。
- **问题10**（LlmChatOptions 扩展点缺失）：字段补充或规则说明均未添加。
- **问题11/12**（DiscussionConclusion 预处理调用 + extractCallerRole 规则）：深度不足的问题未补充设计细节。

### 新发现的问题
本轮未识别出新的独立问题。所有问题均与第 3 轮审查保持一致，说明当前迭代的修复重点应全部集中在上述 12 项持续存在问题的彻底解决上。

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281602_phase5_pkgG_ood\a_v3_copy_from_v2.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281602_phase5_pkgG_ood\requirement.md
