# Phase 2/3 OOD 设计质量审查诊断报告（v5）

## 审查发现

### 1. [严重] AiSuggestionResult 不支持并发更新——预创建→更新模式在异步场景下存在数据竞争

**问题描述**：§3.4 和 §6.3 描述 AiSuggestionResult 采用"预创建→更新"模式：check-dose 响应前以 PENDING 状态预创建条目存入 ConcurrentHashMap，异步 AI 调用完成后更新为 COMPLETED 或 FAILED。但在并发场景下，两个异步回调（如重试或前端重复请求触发的第二次 check-dose）可能同时更新同一个 taskId 的状态，ConcurrentHashMap 虽然保证 map 级别的线程安全，但不保证 value 对象的状态一致性——多个线程可能同时读写 AiSuggestionResult 的 status/suggestion/failReason 字段，产生数据竞争。

**所在位置**：§3.4 AiSuggestionResult + §6.3 "包E 的异步 AI 建议"

**严重程度**：严重

**改进建议**：明确 AiSuggestionResult 的并发安全策略，推荐以下任一路径：(a) AiSuggestionResult 内部字段使用 volatile 或 AtomicReference 保护状态更新，或将 status 更新封装为 CAS 操作（compareAndSet PENDING → COMPLETED/FAILED），失败则忽略（幂等）；(b) 使用 ConcurrentHashMap.compute() 方法原子更新整个 value 对象。同时在设计文本中明确：同一 taskId 的状态更新操作应保证幂等——重复的 COMPLETED/FAILED 更新不引发副作用。


### 2. [严重] 包E check-dose 请求与需求文档 3.4.10 AI 辅助开方输入契约完全脱节

**问题描述**：需求文档 3.4.10 定义的 AI 辅助开方输入契约为 `diagnosis`（诊断结论）、`exam_results`（检查/检验结果摘要）、`patient_info`（含 allergy_history/comorbidities）、`existing_prescription`（当前处方）、`encounter_id`。而本设计的 check-dose API（§4.4）仅接收 `drugCode, dosage, unit, routeOfAdministration, patientAge, patientWeight, frequency`——这是单药品剂量检查，而非需求文档所定义的 AI 辅助开方（生成完整处方草案 + 剂量告警 + 过敏冲突告警的 AI 能力）。设计的 DosageThresholdService 是本地纯规则校验，不调用 AiService.prescriptionAssist() 来接收诊断/检查结果等上下文信息，无法产出需求文档要求的 `prescription_draft` 和 `allergy_warnings`。

设计的 PrescriptionAssistService（§3.4）声称"委托 AiService.prescriptionAssist()（异步）"获取 AI 建议，但：(1) check-dose 同步响应仅返回剂量告警，不包含处方草案和过敏告警；(2) 异步 AI 建议的查询端点（GET suggestion/{taskId}）返回的是文本 `suggestion`，而非需求文档定义的 `prescription_draft` + `dose_warnings` + `allergy_warnings` + `disclaimer_required` 结构化输出；(3) 设计未说明如何将本地 DosageThresholdService 的剂量校验与 AI 辅助开方的处方草案、过敏冲突告警统一组装为需求文档 3.4.10 规定的完整输出结构。

**所在位置**：§3.4 PrescriptionAssistService / DosageThresholdService + §4.4 check-dose API + §6.3 异步 AI 建议

**严重程度**：严重

**改进建议**：重新设计包E 的 API 以覆盖需求文档 3.4.10 的完整能力定义。推荐路径：(a) 将 check-dose 仅定位为"本地剂量阈值即时校验"的内部/辅助端点（不对外暴露或仅作为处方编辑界面的动态校验钩子），同时定义 POST /api/prescription/assist 作为 3.4.10 的主端点，接收 diagnosis/exam_results/patient_info/existing_prescription，同步/异步调用 AiService.prescriptionAssist()，返回完整结构（prescription_draft + dose_warnings + allergy_warnings + disclaimer_required）(b) 或补充说明：3.4.10 的完整能力在下一次迭代中实现，当前设计仅覆盖"剂量阈值告警"子集（与路线图 Phase 3 中"3.4.10 剂量阈值告警"一致），但需在文档中显式标注此范围限制和后续扩展计划，并在 AiService.prescriptionAssist() 的调用契约中预留完整输入输出结构。


### 3. [严重] 分诊场景 TriageResponse 与需求文档 3.4.1 输出契约不对齐——缺少推荐医生列表和推荐理由

**问题描述**：需求文档 3.4.1 定义智能分诊的输出契约包含 `recommended_departments`（推荐科室列表，0-3 项）、`recommended_doctors`（推荐医生列表，0-5 项）、`reason`（推荐理由文本，必填）。本设计的 TriageResponse（§1.3、§3.1）仅有 `departments`（推荐科室列表）、`sessionId`、`needFollowUp`、`followUpQuestion`、`confidence`——缺少推荐医生列表字段和推荐理由文本字段。

同时，需求文档 3.4.1 输入契约包含 `session_id`（必填）、`rule_version`（可选）、`rule_set_id`（可选）、`patient_id`（可选）、`additional_responses`（可选），而 DialogueCreateRequest 仅包含 `chiefComplaint`、`patientId`、`age`、`gender`、`sessionId`——缺少 `ruleVersion`/`ruleSetId`（用于关联管理端 3.3.3 配置的规则版本）和 `additionalResponses`（多轮追问场景下的结构化追问回答数组，含 question/answer/answered_at）。

**所在位置**：§3.1 TriageResponse + DialogueCreateRequest + §4.1 智能分诊场景

**严重程度**：严重

**改进建议**：(a) TriageResponse 增加 `doctors` 字段（推荐医生列表，List<RecommendedDoctor>，每项含 doctorId/doctorName/departmentId/availableSlotCount/score）和 `reason` 字段（推荐理由文本，必填，char≥1）；(b) DialogueCreateRequest 增加 `ruleVersion`（可选）和 `additionalResponses`（可选，List<AdditionalResponse>，每项含 question/answer/answeredAt）以对齐需求文档 3.4.1 的输入契约。ruleVersion 和 ruleSetId 关联 3.3.3 管理端规则配置，是"规则可配置"需求的技术落地点。


### 4. [严重] 处方审核 AuditRequest 与需求文档 3.4.2 输入契约不对齐——缺少完整字段映射

**问题描述**：需求文档 3.4.2 定义处方审核输入契约包含 `prescription_items`（每项含 drug_id/drug_name/dose/frequency/duration/route）、`patient_info`（含 patient_id/age/gender/allergy_history/comorbidities）、`prescription_id`。本设计 AuditRequest（§3.2）包含 `prescriptionItems`、`patientInfo`、`prescriptionId`，但 `patientInfo` 的描述仅为"患者个体信息（含过敏史与合并症）"，未明确定义其字段结构——需求文档规定的 `allergy_history`（string 类型，过敏原名称以中文逗号拼接）与 `comorbidities`（array of string）是否作为独立字段存在于 patientInfo 中未显式声明。同时，`prescriptionItems` 每项是否包含需求文档要求的 `dose`/`frequency`/`duration`/`route` 四个独立字段也需明确。

**所在位置**：§3.2 AuditRequest + §4.2 处方审核场景

**严重程度**：严重

**改进建议**：在 AuditRequest 的 patientInfo 中显式列出 `allergy_history`（string，过敏原以逗号拼接）和 `comorbidities`（List<String>，合并症名称列表）字段。处方药品列表每项显式包含 `drugId`/`drugName`/`dose`/`frequency`/`duration`/`route` 六个字段。确保与需求文档 3.4.2 输入契约严格对齐。


### 5. [严重] 病历生成场景 RecordGenerateRequest 与需求文档 3.4.3 输入契约不对齐——缺少 encounter_id 支持

**问题描述**：需求文档 3.4.3 定义病历生成输入契约包含 `encounter_id`（就诊标识，可选）。本设计的 RecordGenerateRequest（§3.3）包含 `dialogueText`、`departmentId`、`patientId`、`visitId`——已增加 visitId 字段（这是一个积极改进），但需求文档中的字段名为 `encounter_id`（语义为"就诊标识"），而设计使用的 `visitId` 与 `encounter_id` 是否为同一概念未做映射说明。此外，需求文档的输入契约未包含 `departmentId`（科室标识），而是通过 `encounter_id` 间接关联科室信息——设计中额外添加的 `departmentId` 作为显式参数虽然合理，但应说明与 encounter_id 的关系和优先级。

**所在位置**：§3.3 RecordGenerateRequest + §4.3 病历生成场景

**严重程度**：严重

**改进建议**：(a) 明确 visitId 与需求文档 encounter_id 的映射关系（建议：visitId 即为 encounter_id 的设计侧命名，在文档中显式标注映射说明）；(b) 说明 departmentId 的引入理由：当 encounter_id 存在时由后端从就诊记录中自动获取科室信息，departmentId 仅在 encounter_id 缺失时作为显式参数使用——或直接使用 encounter_id 关联获取，删除 departmentId 显式参数。选择的方案需明确写入设计文本。


### 6. [一般] WARN 级处方"强制提交并留痕"的留痕数据结构未定义

**问题描述**：§3.2 和 §4.2 描述 WARN 级别处方"允许医生强制提交并留痕"——医生三种可选操作中的"强制提交"需要记录医生的强制提交决定。但 AuditRecord 实体（§3.2）仅定义了审核结果的持久化字段（prescriptionOrderId/doctorId/patientId/auditTime/fromFallback/originalPrescription/riskLevel/aiResult/auditIssues），未定义"强制提交留痕"所需的字段：强制提交标志（isForceSubmitted）、强制提交时间（forceSubmitTime）、医生强制提交时的操作类型标记。

需求文档 3.2.2.7 明确要求"审核结果需持久化保存，便于事后回溯与统计"，"强制提交并留痕"中的"留痕"需要具体的持久化载体。

**所在位置**：§3.2 AuditRecord + §4.2 WARN 分支

**严重程度**：一般

**改进建议**：在 AuditRecord 实体中增加 `forceSubmitted`（Boolean，是否强制提交，默认 false）和 `forceSubmitTime`（LocalDateTime，可选，强制提交时间）字段。或在处方实体本身（prescription 模块外部）增加强制提交标记——无论选择哪个实体承载留痕数据，需在设计文本中显式定义其字段和写入时机。


### 7. [一般] 分诊场景 session_id 的生成策略和格式未定义

**问题描述**：DialogueCreateRequest 和 TriageResponse 中均包含 sessionId，但 sessionId 的生成策略（UUID / 雪花 ID / 自增序列）、格式规范（长度限制、字符集）以及与 DialogueSessionManager 内部 ConcurrentHashMap key 的一致性要求均未定义。实现者面临随意假设的风险（如前端生成的 UUID vs 后端生成的雪花 ID），也可能导致 ConcurrentHashMap 的 key 冲突或 TTL 清理策略不一致。

**所在位置**：§3.1 DialogueSession / DialogueSessionManager + §4.1 智能分诊场景

**严重程度**：一般

**改进建议**：明确 sessionId 由后端 DialogueSessionManager 在创建会话时统一生成（前端不生成），采用 UUID v4 格式（避免雪花 ID 在分布式场景下的时钟回拨问题，且 ConcurrentHashMap 的 key 为 String 适合 UUID），在 TriageResponse 中返回给前端。首轮请求时 sessionId 为空（null 或空字符串），后端新建会话后填充。


### 8. [一般] DosageAlertLevel.CRITICAL 与 AuditRiskLevel.BLOCK 的联动触发机制未定义调用链路

**问题描述**：§3.4 和 §1.3 DosageAlertLevel 描述 CRITICAL 级别"联动触发 PrescriptionAuditService 的 BLOCK 审核"，但仅停留在语义描述层面。实际的调用链路未定义：(1) DosageThresholdService 检测到 CRITICAL 后如何触发 PrescriptionAuditService？直接注入调用？(2) 触发后传入 PrescriptionAuditService 的入参是什么——仅传入当前剂量告警相关的药品，还是组装为完整 AuditRequest？(3) 剂量检查是实时检查（医生在输入剂量时触发），处方审核是提交时审核——两者的触发时机不同，CRITICAL 剂量告警触发处方审核会导致时机上的矛盾（尚未提交处方即触发审核）。

**所在位置**：§3.4 DosageAlertLevel.CRITICAL 描述 + §2.2 包D-AI1 与包E 强耦合处理

**严重程度**：一般

**改进建议**：重新澄清 CRITICAL 与 BLOCK 的联动语义。推荐路径：CRITICAL 级别剂量告警不直接调用 PrescriptionAuditService，而是将该 DosageAlert 标记写入处方草稿上下文（如 prescription 模块内部的草稿状态对象），在医生最终提交处方触发处方审核时，PrescriptionAuditService 从草稿上下文读取 CRITICAL 标记，将其作为 BLOCK 判定的输入之一。这样既保留了"双重阻断"的安全语义，又避免了时机矛盾。在 §3.4 和 §4.4 中补充此联动链路的设计描述。


### 9. [一般] 本地规则引擎 AllergyCheckRule 和 DrugInteractionRule 的数据来源未定义

**问题描述**：§3.2 LocalRuleEngine 下属规则 DrugInteractionRule、AllergyCheckRule、DosageLimitRule 被定义为独立规则，但 DrugInteractionRule（药物相互作用检查）和 AllergyCheckRule（过敏冲突检查）需要查询外部数据源——DrugInteractionRule 需要药物-药物相互作用数据库，AllergyCheckRule 需要药品成分与过敏原的映射关系。DosageLimitRule 的数据来源已由 DosageStandard + DosageThresholdService 覆盖，但前两者需要的数据实体（如 DrugInteractionPair、DrugAllergyMapping）在设计文本中完全缺失。需求文档 3.4.2 提到"药品知识库"作为本地规则数据来源，但未在本设计中定义对应实体。

**所在位置**：§2.1 目录结构 + §3.2 LocalRuleEngine

**严重程度**：一般

**改进建议**：(a) 在 common 或 prescription 模块中补充本地规则所需的数据实体定义：DrugInteractionPair（药物相互作用对，含 drugA/drugB/severity/description）和 DrugAllergyMapping（药品过敏映射，含 drugCode/ingredient/allergen/severity/description）；(b) 或明确说明 Phase 2/3 的本地规则仅实现 DosageLimitRule（剂量上限检查），DrugInteractionRule 和 AllergyCheckRule 在数据实体就绪后再实现（需标注此为待实现项）；(c) 补充这些实体的 Repository 定义和初始化数据方案（与 DosageStandard 的种子数据方案类似）。


### 10. [一般] 分诊降级链中 AI 无结果与规则无结果的判定边界不清晰

**问题描述**：§4.1 定义线性降级链"AI 无结果 → 规则匹配无结果 → 兜底提供者"，但"AI 无结果"的判定条件不清晰——AiService.triage() 返回的 AiResult 在什么条件下被判定为"无结果"？若 AI 返回空列表（0 个推荐科室），是否视为"无结果"而降级到规则引擎？还是空列表本身就是一个有效的分诊结果（AI 判定症状不明确，无法推荐）？需求文档 3.4.1 定义的输出为 `recommended_departments`（0-3 项），0 项是否属于正常返回？

**所在位置**：§4.1 智能分诊场景 + §3.1 TriageService

**严重程度**：一般

**改进建议**：明确"AI 无结果"的判定条件：(a) AiService.triage() 返回 AiResult.success=false 或 degraded=true 时判定为 AI 不可用，进入降级链；(b) AiService.triage() 返回 AiResult.success=true 但推荐科室列表为空时，视为"AI 判定无法推荐"的有效结果，跳过规则引擎直接返回空推荐（因为规则引擎基于症状关键词匹配，AI 已经分析过症状后明确无法推荐，规则引擎的结果可信度更低）；(c) 或明确：AI 返回空列表时也继续降级到规则引擎尝试匹配（规则引擎可能通过关键词命中而 AI 理解不足时遗漏的科室）。无论选择哪种语义，需在设计文本中显式定义。


### 11. [一般] 处方审核 AI 超时阈值和病历生成 AI 超时阈值未在设计文本中体现

**问题描述**：需求文档 3.4.2 定义处方审核硬超时 ≤ 6 秒，3.4.3 定义病历生成非流式硬超时 ≤ 12 秒。本设计 PrescriptionAuditService（§3.2）调用 AiService.prescriptionCheck() 时未定义超时配置策略，MedicalRecordService（§3.3）调用 AiService.generateMedicalRecord() 时也未定义超时配置。超时阈值是降级路径触发的前置条件——硬超时后自动切换到本地规则校验（处方审核）或返回部分内容+缺失字段标记（病历生成），但设计文本未将需求文档的超时阈值映射为实现层的配置约定。

**所在位置**：§3.2 PrescriptionAuditService + §3.3 MedicalRecordService + §4.2/§4.3 流程描述

**严重程度**：一般

**改进建议**：在 §4.2 和 §4.3 的流程描述中补充超时阈值配置说明。推荐方式：在 application.yml 中定义 `ai.timeout.prescription-audit=6s` 和 `ai.timeout.medical-record-generate=12s`，Service 层通过 CompletableFuture.orTimeout() 或 AiService 调用层统一设置硬超时。超时后的降级行为在流程描述中已有覆盖，但需标注超时阈值的具体值来源（需求文档 3.4.2 / 3.4.3）。


### 12. [轻微] 分诊规则引擎缓存刷新时间与对话会话 TTL 的交互影响未分析

**问题描述**：§3.1 TriageRuleEngine 使用 Caffeine refreshAfterWrite（默认 60 秒）定时刷新规则缓存，DialogueSessionManager 使用 TTL 30 分钟管理对话会话。当规则在对话进行中被刷新（如管理员在 3.3.3 修改了规则），同一对话的后续追问可能使用新规则而非对话开始时的规则，导致对话前后分诊逻辑不一致。此交互影响未在设计文本中分析。

**所在位置**：§3.1 TriageRuleEngine + DialogueSessionManager

**严重程度**：轻微

**改进建议**：(a) 在 DialogueCreateRequest 中支持 ruleVersion 字段（需求文档 3.4.1 已定义此可选字段），首轮对话时记录当前规则版本号，后续追问（同 sessionId）使用快照的规则版本而非最新版本，保证对话内分诊逻辑一致性；(b) 或在设计文本中说明：当前设计接受对话内规则可能不一致（最终一致性），此为 Phase 2/3 的已知限制，Phase 5 AI 进阶底座落地后通过规则版本快照机制解决。无论选择哪种方案，需在文档中显式记录此设计取舍。


### 13. [轻微] DosageThresholdService 同优先级多条结果"取 dosageMax 最小者"策略可能导致误报

**问题描述**：§8.4 定义"同一优先级返回多条时取 dosageMax 最小的条目作为最保守阈值"。此策略在某些场景下可能产生误报——例如，同一药品存在 2 条默认阈值记录（数据冗余或录入错误），较小的 dosageMax 可能为异常数据。取最保守值虽安全，但若异常数据极小（如录入错误将 mg 写成 mcg），将导致正常剂量被判定为严重超标。

**所在位置**：§8.4 年龄/体重分级剂量支持

**严重程度**：轻微

**改进建议**：(a) 增加数据校验：种子数据初始化时检测同一 drugCode + routeOfAdministration 同优先级的重复记录并报错；(b) 或在策略中增加"去重"前置步骤——同一优先级存在多条时，先检查是否存在数据质量问题（如 dosageMax 差异超过 10 倍），有则告警并使用次小值或平均值，而非无条件取最小值。此校验逻辑可在实现阶段细化，但在设计文本中应标注此风险和应对原则。
