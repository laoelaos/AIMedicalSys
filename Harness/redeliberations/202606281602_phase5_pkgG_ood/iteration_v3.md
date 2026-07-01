# 再审议判定报告（v3）

## 判定结果

RETRY

## 判定理由

组件B诊断报告识别出 12 个质量问题，质询报告确认 LOCATED（审查结论有效）。其中含 1 个严重等级问题（问题2：`@Qualifier("{capabilityId}Strategies")` 在 Spring 容器中无法动态解析，属编译期不可行事实错误），及多个重要/中等等级问题。根据判定标准，审查报告包含严重等级问题，须判定 RETRY。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：`AbstractCapabilityExecutor` 构造器伪代码中的 `@Qualifier("{capabilityId}Strategies")` 无法在 Spring 容器中正确解析，属编译期不可行的事实错误
- **所在位置**：§3.1（第 834 行、第 1146-1162 行）、§3.8（第 2051-2060 行）
- **严重程度**：严重
- **改进建议**：明确选择一套降级策略注入机制。推荐保留 `AiPlatformConfig` 中的 Map 构建路径，删除或修正构造器中的 `@Qualifier` 伪代码，改为注入 `Map<String, List<DegradationStrategy>>` 后在 `execute()` 中按 `getCapabilityId()` 查找

- **问题描述**：§3.11 覆盖范围与需求不对称，3 项迁移能力（Triage、PrescriptionCheck、MedicalRecordGen）缺少同等级别的 9 维度特化设计表
- **所在位置**：§3.11（第 3219-3281 行）
- **严重程度**：一般
- **改进建议**：将 §3.11 标题改为"Phase 5 底座能力特化设计"并覆盖全部 7 项底座能力，或新增 §3.12 覆盖 3 项迁移能力的特化设计表

- **问题描述**：降级策略注入机制存在两套并行且互斥的描述（`@Qualifier` 按能力注入 vs `AiPlatformConfig` 构建 Map）
- **所在位置**：§3.1（第 831-852 行）、§3.8（第 2048-2060 行）、`AbstractCapabilityExecutor` 构造器伪代码（第 1141-1162 行）
- **严重程度**：一般
- **改进建议**：统一为一套机制，推荐 Map 方案，删除所有 `@Qualifier` 按能力注入的表述

- **问题描述**：底座能力条件化注册机制描述与实际设计不一致——声称"在 AiPlatformConfig 中统一注册条件"但 §3.9 无对应统一注册逻辑
- **所在位置**：§3.1（第 899 行）、§3.9（第 2080-2201 行）
- **严重程度**：一般
- **改进建议**：明确 CapabilityExecutor 的注册策略（`@Component` 自注册 vs `@Bean` 统一注册），修正不一致的表述

- **问题描述**：薄适配器 `doExtractDepartmentId()` 的非 HTTP 回退描述引用 Prompt 模板回退，但薄适配器不使用 Prompt 模板
- **所在位置**：§3.1（第 903-904 行的 `doExtractDepartmentId()` 说明）
- **严重程度**：一般
- **改进建议**：将"Prompt 模板回退到通用模板"改为"departmentId 保持 null，对应字段在 AiCallRecord 中可空"

- **问题描述**：§3.11 节标题称"首次落地"但包含迁移能力 PrescriptionAssist，同时 3 项真正的迁移能力未被覆盖
- **所在位置**：§3.11 标题（第 3219 行）
- **严重程度**：一般
- **改进建议**：标题改为"Phase 5 底座能力特化设计"并扩展至 7 项，或保持 4 项并修正标题

- **问题描述**：`ExperimentGroup` 实体未被正式定义，实现者不知字段结构、JPA 映射或业务约束
- **所在位置**：§3.4 第 1670-1680 行、§4.4 第 2582-2590 行
- **严重程度**：一般
- **改进建议**：在 §3.4 或新增子章节中补充 `ExperimentGroup` 的完整定义（字段表、JPA Entity 注解、关联关系、流量分配算法约束）

- **问题描述**：`AiCallLogStats` 汇总统计表未被正式定义——无字段结构、无索引策略、无 Entity 类引用
- **所在位置**：§3.5（第 1874 行）
- **严重程度**：一般
- **改进建议**：在 §3.5 的"数据保留与清理策略"子节中补充 `AiCallLogStats` 的完整字段定义、索引策略及写入/查询伪代码

- **问题描述**：`PrescriptionLocalRuleFallback` 未定义"最小安全规则"的具体内容
- **所在位置**：§3.7（第 1949-1955 行）
- **严重程度**：一般
- **改进建议**：补充 `PrescriptionLocalRuleFallback` 的具体行为契约（至少 3~5 项最小检查规则）、规则来源及执行失败时安全策略

- **问题描述**：`ModelRoute.parameters` 未定义扩展点，`topP`、`frequencyPenalty`、`presencePenalty`、`seed` 等常见 LLM 参数被静默忽略
- **所在位置**：§3.2（第 1401-1417 行）、§9.5 YAML 示例
- **严重程度**：一般
- **改进建议**：在 `LlmChatOptions` 中补充 `topP`、`frequencyPenalty`、`presencePenalty` 字段，或在 §3.2 新增参数映射扩展点规则说明

- **问题描述**：`DiscussionConclusionCapabilityExecutor` 的 `transcriptSummary` 前置 LLM 调用未定义超时和失败策略
- **所在位置**：§3.11.4（第 3271 行）、§3.1 的 `extractVariables()` 定义（第 1065-1068 行）
- **严重程度**：一般
- **改进建议**：明确预处理 LLM 调用的契约——使用的方法、独立的超时管理、调用失败时的降级行为

- **问题描述**：`extractCallerRole()` 未定义 Spring Security `GrantAuthority` 的过滤规则（ROLE_ 前缀处理、多 authority 场景）
- **所在位置**：§3.1（第 1098-1109 行）
- **严重程度**：一般
- **改进建议**：补充显式规则：取第一个以 `"ROLE_"` 开头的 `GrantedAuthority` 并去除前缀；若无则取第一个原始值
