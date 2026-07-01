# Phase 2/3 OOD 设计质量审查报告（第 6 轮）

## 审查发现

### 1. AuditResponse.issues 与需求文档 3.4.2 alerts 字段命名与结构不一致

**问题描述**：需求文档 3.4.2 输出契约定义风险提示字段名为 `alerts`（每项含 `alert_code` / `alert_message` / `severity`），设计文档 §3.2 AuditResponse 使用 `issues`（List\<AuditIssue\>，每项含 `fieldName` / `issueDescription` / `ruleId` / `severity`）。两者不仅是命名差异（alerts vs issues），字段结构也完全不同——需求文档的 `alerts` 按 `alert_code` / `alert_message` 组织，设计文档的 `AuditIssue` 按 `fieldName` / `issueDescription` / `ruleId` 组织。这构成契约不对齐。

**所在位置**：§3.2 AuditResponse + AuditIssue 定义；§1.3 核心抽象一览 AuditResponse

**严重程度**：严重

**改进建议**：(a) 在 AuditResponse 中同时保留 `alerts`（对齐需求文档 3.4.2 契约，每项含 alertCode / alertMessage / severity）和 `issues`（设计自有结构，承载更细粒度的审核问题信息），并说明二者关系；或 (b) 将 AuditIssue 字段名对齐为 alertCode/alertMessage，或显式说明 issues 与 alerts 的映射关系。同时建议在 §3.2 或 §7 补充设计决策说明此差异的合理性。

### 2. DialogueCreateRequest 缺少 ruleSetId 字段，与需求文档 3.4.1 输入契约不对齐

**问题描述**：需求文档 3.4.1 输入契约定义 `rule_set_id`（配置规则集标识，string，可选），设计文档 §3.1 DialogueCreateRequest 已补充 `ruleVersion`（对应 `rule_version`），但缺失 `ruleSetId`（对应 `rule_set_id`）。这两个是独立字段，rule_version 标识规则版本号，rule_set_id 标识规则集标识，两者语义不同。

**所在位置**：§3.1 DialogueCreateRequest 定义；§1.3 核心抽象一览

**严重程度**：一般

**改进建议**：在 DialogueCreateRequest 中增加 `ruleSetId`（可选字段，String 类型），对齐需求文档 3.4.1 rule_set_id。并在 §3.1 TriageRuleEngine / §4.1 补充 ruleSetId 的消费语义（如按 ruleSetId 选择对应规则集匹配）。

### 3. TriageResponse 缺少 matchedRules 字段，与需求文档 3.4.1 输出契约不对齐

**问题描述**：需求文档 3.4.1 输出契约定义 `matched_rules` 字段，设计文档 §3.1 TriageResponse 已补充 departments/doctors/reason/sessionId/needFollowUp/confidence，但缺失 `matchedRules` 字段。matched_rules 承载规则命中调试信息，对分诊质量分析和规则调优具有直接价值。

**所在位置**：§3.1 TriageResponse 定义；§1.3 核心抽象一览

**严重程度**：一般

**改进建议**：在 TriageResponse 中增加 `matchedRules`（可选字段，List\<MatchedRule\>，每项含 ruleId / ruleName / score），对齐需求文档 3.4.1 输出契约的 matched_rules 字段。

### 4. TriageResponse.departments 每项字段与需求文档 3.4.1 recommended_departments 结构不完整对齐

**问题描述**：需求文档 3.4.1 输出契约定义 recommended_departments 每项含 `department_id` / `department_name` / `score`，设计文档 §3.1 TriageResponse 的 departments 列表仅描述为"推荐科室列表"，但未在 §1.3 核心抽象一览或 §3.1 中定义 departments 每项的结构。虽然 RecommendedDoctor 已有完整字段定义，departments 每项缺少独立的 DTO 定义或字段说明，是否包含 departmentId / departmentName / score 均不明确。

**所在位置**：§3.1 TriageResponse；§1.3 核心抽象一览

**严重程度**：一般

**改进建议**：新增 RecommendedDepartment DTO（含 departmentId、departmentName、score），或在 TriageResponse 描述中显式声明 departments 每项的字段结构，与需求文档 3.4.1 对齐。

### 5. 病历生成缺少流式输出生成模式的设计，与需求文档 3.4.3 契约不对齐

**问题描述**：需求文档 3.4.3 定义了流式输出模式（stream=true 时分片返回，含 chunk_index / is_final 错误处理等完整机制），设计文档 §3.3 医疗记录生成场景和 §4.3 仅描述了非流式路径，超时配置仅设定 ai.timeout.medical-record-generate=12s（对应非流式），流式模式的首字响应 ≤2s、总时长 ≤30s、分片间隔 ≤5s 等阈值未涉及，MedicalRecordController 和 MedicalRecordService 未定义流式端点和流式处理契约。

**所在位置**：§3.3 MedicalRecordController / MedicalRecordService；§4.3 病历生成场景；§5.5 AI 超时配置

**严重程度**：严重

**改进建议**：(a) 若 Phase 3 不实现流式，应在设计文档中显式标注"流式输出待后续迭代实现"，并在 §7 设计决策中说明理由；同时补充 RecordGenerateRequest 的 stream 字段（bool，可选，默认 false），并在 §5.5 补充流式超时配置项（流式首字 2s、总时长 30s、分片间隔 5s）；(b) 若同步实现流式，则需补充流式端点（SSE / WebSocket）设计、流式超时处理、流式错误分片结构等完整契约。

### 6. RecordGenerateRequest 缺少 stream 字段，与需求文档 3.4.3 输入契约不对齐

**问题描述**：需求文档 3.4.3 输入契约定义 `stream`（是否流式输出，bool，可选，默认 false）字段，设计文档 §3.3 RecordGenerateRequest 仅包含 dialogueText / patientId / encounterId，缺少 stream 字段。即使当前仅实现非流式，也需保留 stream 字段以对齐契约。

**所在位置**：§3.3 RecordGenerateRequest 定义

**严重程度**：一般

**改进建议**：在 RecordGenerateRequest 中增加 `stream`（bool，可选，默认 false），对齐需求文档 3.4.3 输入契约。若当前不实现流式，在字段描述中标注"Phase 3 仅支持非流式（stream=false），流式待后续迭代"。

### 7. RecordGenerateResponse 的 fields 映射结构与需求文档 3.4.3 输出契约字段命名不一致

**问题描述**：需求文档 3.4.3 输出契约使用扁平字段命名：`chief_complaint` / `symptom_description` / `present_illness` / `past_history` / `physical_exam` / `preliminary_diagnosis` / `treatment_plan` / `missing_fields`。设计文档 §3.3 RecordGenerateResponse 使用 `fields`（Map\<MedicalRecordField, String\>）映射结构 + `missingFieldHints`（List\<FieldMissingHint\>）。虽 MedicalRecordField 枚举条目覆盖了需求文档字段，但 (a) 枚举中 PRESENT_ILLNESS / PAST_HISTORY / PHYSICAL_EXAM / PRELIMINARY_DIAGNOSIS / TREATMENT_ADVICE 与需求文档命名存在差异（如 TREATMENT_ADVICE vs treatment_plan）；(b) 需求文档的 `missing_fields` 为简单字符串数组，设计文档用结构化 FieldMissingHint 替代，需说明映射关系。

**所在位置**：§3.3 RecordGenerateResponse；§3.3 MedicalRecordField 枚举

**严重程度**：一般

**改进建议**：(a) 在 MedicalRecordField 枚举定义处补充与需求文档 3.4.3 输出契约字段名的映射说明（如 TREATMENT_ADVICE 对应 treatment_plan）；(b) 说明 missing_fields（string 数组）与 missingFieldHints（List\<FieldMissingHint\>）的映射关系——missingFieldHints 是 missing_fields 的结构化升级版本。

### 8. 本地规则校验范围与需求文档 3.4.2 最小检查项集不匹配——仅实现 1/4

**问题描述**：需求文档 3.4.2 明确规定本地规则校验最小检查项集含 4 项：①剂量范围检查、②药品禁忌检查、③重复用药检查、④儿童/老年人群特殊剂量检查。设计文档 §3.2 LocalRuleEngine 标注"Phase 2/3 仅实现 DosageLimitRule"，仅覆盖第 1 项，第 2/3/4 项被推迟至 Phase 3 后续实现。但需求文档 3.4.2 的"降级路径"明确要求这些为本地规则校验的"最小检查项集合，由后端业务层在 AI 不可用时执行"，属于降级安全底线。

**所在位置**：§3.2 LocalRuleEngine；§4.2 处方审核降级路径

**严重程度**：严重

**改进建议**：(a) 重新评估实现范围——药品禁忌检查（AllergyCheckRule，依赖 DrugAllergyMapping + 患者过敏史/合并症）和重复用药检查依赖的是药品成分字典数据而非 AI，具备本地实现条件，应纳入 Phase 3 最低交付范围；儿童/老年人群特殊剂量检查可借用 DosageThresholdService 的年龄/体重分级机制实现。若确有数据获取困难（如 DrugAllergyMapping 数据初始化），应显式标注为降级安全缺口并在 §4.2 中增加"本地规则仅覆盖剂量范围检查，其余 3 项待实现"的风险提示，而非仅以"骨架预留"一笔带过——这直接影响 AI 不可用时的用药安全兜底能力。

### 9. PrescriptionDraftContext 生命周期管理缺失——处方编辑会话标识未定义，清理时机不明确

**问题描述**：§3.4 和 §6.4 描述 PrescriptionDraftContext "按 encounterId 或处方编辑会话标识关联"，处方提交或取消后清理。但 encounterId 和"处方编辑会话标识"是两个不同概念——一次就诊（encounter）期间医生可能多次编辑不同处方（修改→放弃→重开），同一 encounterId 下可能同时存在多个处方编辑会话。当前设计未定义：(a) PrescriptionDraftContext 的 key 到底是 encounterId 还是处方编辑会话标识？(b) 处方编辑会话标识由谁生成、何时创建？(c) "处方提交或取消后清理"由谁触发、如何保证清理不遗漏？

**所在位置**：§3.4 PrescriptionDraftContext；§6.4 处方草稿上下文并发管理；§4.2 CRITICAL 与 BLOCK 联动

**严重程度**：一般

**改进建议**：(a) 明确 PrescriptionDraftContext 的 key 为 prescriptionId（处方编辑会话标识 = 处方草稿 ID），而非 encounterId——避免一次就诊内多个处方编辑会话的 CRITICAL 告警互相干扰；(b) 定义 PrescriptionDraftContext 的创建时机（首次 check-dose 请求时）和清理时机（处方提交成功 / 处方取消 / TTL 过期 3 种清理路径），清理触发方为 PrescriptionAssistService（提交成功时）或定时扫描（TTL 过期时）。

### 10. 分诊场景缺少 AI 连续失败 3 次的兜底提示机制

**问题描述**：需求文档 §3.1.3.1 明确定义异常分支："单一问诊会话中 AI 调用失败次数达 3 次后，自动给出'建议直接联系线下接诊窗口'的兜底文案"。设计文档 §3.1 DialogueSessionManager 和 §4.1 智能分诊场景未涉及 AI 调用失败计数和阈值触发机制。

**所在位置**：§3.1 DialogueSessionManager；§4.1 智能分诊场景

**严重程度**：一般

**改进建议**：在 DialogueSession 中增加 `aiFailCount`（int）字段，每次 AI 调用降级时递增。TriageServiceImpl 在调用 AiService.triage() 降级后检查 aiFailCount >= 3 时，在 TriageResponse.reason 或新增字段中附加"建议直接联系线下接诊窗口"兜底提示。在 §4.1 分诊流程中补充该判定分支。

### 11. AuditRecord 缺少"审核次序"和"是否最新"字段，与需求文档 3.4.2 持久化要求不对齐

**问题描述**：需求文档 3.4.2 明确要求"同一处方的多次审核按 5.3 处方 1—N 处方审核记录关系分别保存一条记录，并通过'审核次序'与'是否最新'字段标识"。设计文档 §3.2 AuditRecord 包含 prescriptionOrderId / doctorId / patientId / fromFallback / forceSubmitted / forceSubmitTime 等字段，但缺少 `auditSequence`（审核次序）和 `isLatest`（是否最新）字段。没有这两个字段，同一处方的多次审核记录无法按序追溯，也无法快速定位最新审核结果。

**所在位置**：§3.2 AuditRecord 实体定义

**严重程度**：一般

**改进建议**：在 AuditRecord 中增加 `auditSequence`（int，必填，同一 prescriptionOrderId 下递增）和 `isLatest`（boolean，必填，同一 prescriptionOrderId 下仅最新一条为 true）字段。在 §4.2 处方审核场景中说明每次新审核写入时，将同处方的旧记录 isLatest 更新为 false。

### 12. DosageStandard 实体与需求文档 5.1 药品基础信息实体的关系未定义

**问题描述**：需求文档 3.4.2 明确指出本地规则校验的数据来源"药品知识库（'单次剂量上限''日剂量上限'来源于 5.1 药品基础信息实体）"。设计文档将 DosageStandard 迁移至 common 模块作为独立实体，但未说明 DosageStandard 与药品基础信息实体（应在 patient/doctor 模块或 admin 模块中）的关系——是同一实体拆分到 common？还是独立实体通过 drugCode 关联？管理员在 3.3.4 综合管理中维护剂量数据时操作的是哪个实体？这直接影响数据录入路径和代码实现。

**所在位置**：§2.2 DosageStandard 位置说明；§8.1 初始化方案；§8.2 药品编码规范

**严重程度**：一般

**改进建议**：在 §2.2 或 §8 中显式说明 DosageStandard 与药品基础信息实体的关系：(a) 两者通过 drugCode 关联但为独立实体——药品基础信息实体承载通用药品属性，DosageStandard 承载剂量阈值专用属性；(b) 明确 admin 模块"药品字典维护"操作的是药品基础信息实体，而"DosageStandard 管理"是独立的剂量标准维护入口（或在药品详情页中内嵌剂量标准子表）；(c) 明确 DosageStandard.drugCode 与药品基础信息实体的主键关联关系。

### 13. 分诊请求中 chiefComplaint 字符数约束（5–500）未在设计文档中定义

**问题描述**：需求文档 3.4.1 输入契约明确 `chief_complaint` 字符数约束为 5–500，设计文档 §3.1 DialogueCreateRequest 定义了 chiefComplaint 字段但未标注输入约束。缺少字符数约束将导致编码阶段缺少校验依据，可能实现为无限制长度的字符串。

**所在位置**：§3.1 DialogueCreateRequest；§4.1 智能分诊场景

**严重程度**：轻微

**改进建议**：在 DialogueCreateRequest.chiefComplaint 字段描述中补充"字符数 5–500"约束，对齐需求文档 3.4.1。同步在 §4.1 或 §5 错误处理中补充违反约束时的校验错误码（如 TRIAGE_AI_INPUT_INVALID）。

### 14. 多轮分诊模式下 chief_complaint 与 additional_responses 的互斥/组合语义未说明

**问题描述**：需求文档 3.4.1 输入契约定义 `additional_responses` 与单字段 `chief_complaint` "二选一使用，不同时提供"。设计文档 §3.1 DialogueCreateRequest 中 chiefComplaint 和 additionalResponses 均存在，但未说明二者的互斥/组合语义——多轮场景下首轮请求仅传 chiefComplaint，后续追问是否仍需传 chiefComplaint？两者是否可同时存在？

**所在位置**：§3.1 DialogueCreateRequest；§4.1 智能分诊场景

**严重程度**：轻微

**改进建议**：在 DialogueCreateRequest 描述中补充：(a) 首轮请求：chiefComplaint 必填，additionalResponses 不传；(b) 后续多轮追问：chiefComplaint 仍传递（但服务端以 DialogueSession 中已保存的主诉为准，前端可省略或传原值），additionalResponses 必填。并在需求文档"二选一"语义与设计文档"可共存"语义间给出明确映射说明。
