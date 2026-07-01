# Phase 2/3 OOD 设计方案 质量审查报告（v8 首轮）

---

## 审查发现

### 问题 1

**问题描述**：DoctorFacade 接口在设计文本 §2.1 目录结构中放置于 `common-module/common-module-api/.../auth/DoctorFacade.java`，但实际代码中 `common-module-api` 的 `auth/` 包下仅有 UserFacade / CurrentUser / UserInfoResponse，且该包的语义定位是"用户认证相关门面"。DoctorFacade 是跨模块医生信息查询门面，其语义属于"医生排班/可用性查询"，与"认证"包名不匹配。此外，设计文本 §1.3 核心抽象一览中声明 DoctorFacade "定义在 common-module-api 中"，但 §2.1 目录结构将其与 UserFacade 并列于 `auth/` 子包，两个位置存在语义矛盾——要么 DoctorFacade 应放在独立于 auth 的子包（如 `commonmodule/doctor/DoctorFacade.java`），要么 §1.3 不应将其与 UserFacade 混为一谈地描述为"与 UserFacade 模式一致"。

**所在位置**：§2.1 目录结构 `common-module-api/.../auth/UserFacade.java, DoctorFacade.java`；§1.3 核心抽象一览 DoctorFacade 条目

**严重程度**：一般

**改进建议**：(a) 在 common-module-api 下为 DoctorFacade 创建独立子包（如 `commonmodule/doctor/DoctorFacade.java`），与 auth 包解耦，或 (b) 若确认与 UserFacade 共包，则在 §1.3 补充说明 DoctorFacade 与 UserFacade 共享 auth 包的理由，并同步更新 §2.1 目录结构中的包名注释。

---

### 问题 2

**问题描述**：需求文档 3.4.1 规定 TriageRequest 输入契约中 `additional_responses` 与 `chief_complaint` 为"二选一使用，不同时提供"（即互斥），设计文本 §3.1 DialogueCreateRequest 和 §4.1 也声明了互斥语义。但需求文档 3.1.3.1 第 2 段"多轮调用方式"约定"后续追问通过同一 session_id 增量传入 additional_responses 数组"，且 3.4.1 第 1 段明确"chief_complaint（主诉文本，string，必填，字符数 5–500）"——需求文档中 `chief_complaint` 始终为必填，`additional_responses` 是增量补充而非替代。设计文本将二者定义为"互斥"与需求文档语义矛盾——需求文档的语义是首轮传 chief_complaint（必填）+ 后续轮增量传 additional_responses，而非"后续轮不传 chiefComplaint"。

**所在位置**：§3.1 DialogueCreateRequest "chiefComplaint 与 additionalResponses 的互斥语义"；§4.1 "首轮仅传 chiefComplaint，后续轮仅传 additionalResponses + sessionId，二选一互斥"

**严重程度**：严重

**改进建议**：(a) 重新审视需求文档 3.4.1 和 3.1.3.1 的组装约定——需求明确 `chief_complaint` 为必填，`additional_responses` 为可选增量字段（"二选一"仅指组装方式选择：单次调用直接拼合 vs 多轮调用增量传入），而非字段级互斥。修改 DialogueCreateRequest 的互斥语义：chiefComplaint 为必填（与需求文档对齐），additionalResponses 为可选增量字段，后续多轮调用时 chiefComplaint 仍传首轮主诉（或服务端从 DialogueSession 中获取）。(b) 若确需互斥，应在设计文本 §7 设计决策中显式说明与需求文档的偏差及理由。

---

### 问题 3

**问题描述**：需求文档 3.4.1 输出契约中 `matched_rules` 数组的每项结构定义为 `rule_id / rule_name / confidence / reason`（含 confidence 和 reason），设计文本 §1.3 MatchedRule DTO 仅含 `ruleId / ruleName / score`。confidence 字段被替换为 score 但映射关系未说明，reason 字段完全缺失。需求文档 §3.4.1 原文"matched_rules（匹配规则列表，array，每项含 rule_id / rule_name / confidence / reason）"——confidence 与 reason 的缺失导致前端无法展示每条匹配规则的置信度和匹配理由，与需求文档字段级对齐不完整。

**所在位置**：§1.3 核心抽象一览 MatchedRule DTO

**严重程度**：一般

**改进建议**：MatchedRule 补充 confidence（float，可选）和 reason（string，可选）字段，与需求文档 3.4.1 matched_rules 对齐；若 score 与 confidence 语义等价，在 §7 设计决策中说明映射关系。

---

### 问题 4

**问题描述**：需求文档 3.4.3 规定病历生成输出契约包含 7 个结构化字段，其中 `missing_fields` 定义为"缺失字段提示，array，string"——即缺失字段的标识符列表。设计文本中 RecordGenerateResponse 使用 `missingFieldHints`（List\<FieldMissingHint\>，含 missingField / promptMessage / suggestedAction）替代了需求文档的 `missing_fields`（简单字符串数组）。虽然 §3.3 和 §7 设计决策说明了 missingFieldHints 是 missing_fields 的"结构化升级版本"，但需求文档 3.4.3 同时输出的 `missing_fields` 原始字段在设计文本的 RecordGenerateResponse 中完全消失——仅保留了升级版 missingFieldHints，丢失了对需求文档原始契约字段的响应。前端若依赖 `missing_fields` 原始字段进行简单渲染，将无法获取。

**所在位置**：§3.3 RecordGenerateResponse；§1.3 核心抽象一览 RecordGenerateResponse 条目

**严重程度**：一般

**改进建议**：在 RecordGenerateResponse 中保留 missingFields（List\<String\>）字段以对齐需求文档 3.4.3 原始输出契约，同时保留 missingFieldHints 作为结构化增强版本。二者关系在 §7 设计决策中已有说明，但字段级对齐需二者同时存在。

---

### 问题 5

**问题描述**：设计文本 §3.4 DosageThresholdService "四级均未命中，降级返回 WARN 级 DosageAlert 并携带 RX_ASSIST_DOSE_STANDARD_NOT_FOUND 错误码"，但 DosageAlert 类定义仅包含 alertLevel / alertMessage / drugCode / 当前剂量 / 建议值，缺乏承载错误码的字段。RX_ASSIST_DOSE_STANDARD_NOT_FOUND 错误码无法通过 DosageAlert 本身传递给前端，需依赖 DosageCheckResponse 的额外字段或 HTTP 响应头，但设计文本未定义此传递路径。与 §3.4 `/assist` 主端点的 RX_ASSIST_AI_NO_RECOMMENDATION 标记方式类似，错误码的载体不明确。

**所在位置**：§3.4 DosageAlert；§4.4 check-dose 流程；§8.4 匹配优先级

**严重程度**：一般

**改进建议**：(a) 在 DosageAlert 中增加 errorCode（String，可选）字段，统一承载各类剂量校验相关错误码；或 (b) 在 DosageCheckResponse 中增加 errorCode 字段（与 AiSuggestionResult 的错误码传递模式一致）；或 (c) 在 §4.4 check-dose 流程中显式定义 RX_ASSIST_DOSE_STANDARD_NOT_FOUND 的传递路径（如通过 DosageCheckResponse.errorCode 字段）。

---

### 问题 6

**问题描述**：需求文档 3.4.2 规定处方审核本地规则校验的"药品禁忌检查"（最小检查项集 #2）需检查"处方药品是否与患者的过敏史冲突"和"是否与患者的合并症冲突"两个子项。设计文本 §3.2 AllergyCheckRule 仅实现了过敏史冲突检查，但合并症冲突检查（即"药品是否与患者合并症如肾功能不全、肝功能不全等禁忌冲突"）没有对应的规则实现。需求文档 3.4.2 明确将合并症检查纳入本地规则最小检查项集 #2，但设计中 AllergyCheckRule 的命名和实现范围仅覆盖过敏（allergy），合并症检查被遗漏。

**所在位置**：§3.2 LocalRuleEngine 实现范围表 + AllergyCheckRule 描述

**严重程度**：严重

**改进建议**：(a) 将 AllergyCheckRule 扩展为 AllergyAndContraindicationCheckRule（或新增 ContraindicationCheckRule），补充合并症-药品禁忌检查子项：遍历处方药品列表，对每个药品查询其禁忌症列表，与 patientInfo.comorbidities 做交集比对。命中时产出 WARN 或 BLOCK 级 LocalRuleResult，severity 判定逻辑与 AllergyCheckRule 类似（有结构化数据时按严重程度分级，缺省时保守 BLOCK）。(b) 或在 AllergyCheckRule 中补充合并症检查逻辑，修正规则名称或描述以反映其双职责。需同步更新 §3.2 实现范围表和 §7 设计决策。

---

### 问题 7

**问题描述**：设计文本 §4.2 定义了处方提交端点 `POST /api/prescription/submit`，其中 forceSubmit=true 时需校验 `auditRecordId` 对应的 AuditRecord 的 riskLevel=WARN 且 isLatest=true。但此端点的请求参数中 `prescriptionItems`（当前待提交处方的药品列表）与 AuditRecord.originalPrescription（JSON TEXT 快照）的版本校验比较逻辑存在盲区：设计仅说明"比较当前处方与 AuditRecord.originalPrescription 是否一致"，但未定义"一致"的比较语义——是基于 JSON 全文二进制比较还是按药品 ID + 剂量 + 频率等关键业务字段做 semantically equivalent 比较？JSON 文本缩排顺序、null 与空字符串差异、数值精度差异等均可能导致文本级不一致但业务意图等价的情况，从而产生误判。

**所在位置**：§4.2 处方提交端点 "处方版本校验" 逻辑

**严重程度**：一般

**改进建议**：在 §4.2 处方版本校验逻辑中补充"一致"的比较语义定义。推荐方案：按业务字段做结构化比较（drugId + dose + frequency + duration + route 五字段组合比对），忽略 JSON 文本级的格式差异。补充说明：药品增删、剂量变化等业务实质变更判为"不一致"；仅 JSON 序列化格式差异（如字段顺序、null 与缺失字段）判为"一致"。

---

### 问题 8

**问题描述**：设计文本 §5.1 错误码表中"开方辅助"前缀为 `RX_ASSIST_`，但同时存在 `RX_ASSIST_DOSE_STANDARD_NOT_FOUND`、`RX_ASSIST_AI_NO_RECOMMENDATION` 等错误码。需求文档 3.4.10 错误码定义为 `RX_ASSIST_AI_TIMEOUT / RX_ASSIST_AI_UNAVAILABLE / RX_ASSIST_AI_INPUT_INVALID / RX_ASSIST_AI_NO_RECOMMENDATION`，全部携带 `_AI_` 中段，符合 3.4 节错误码命名约定。设计文本 §5.1 中列出的 `RX_ASSIST_DOSE_STANDARD_NOT_FOUND` 和"单位不匹配"等错误码不含 `_AI_` 中段——若这些错误码为非 AI 业务错误码，则设计文本未明确区分"AI 错误码"和"业务错误码"的命名规则。

**所在位置**：§5.1 错误码表；§3.4 DosageThresholdService

**严重程度**：一般

**改进建议**：在 §5.1 开方辅助类别下增加子分类或前缀说明，区分 AI 错误码（`RX_ASSIST_AI_*`）和业务逻辑错误码（如 `RX_ASSIST_DOSE_STANDARD_NOT_FOUND`），明确非 AI 错误码不强制携带 `_AI_` 中段。

---

### 问题 9

**问题描述**：设计文本 §3.4 PrescriptionAssistRequest（业务层 /assist 端点请求）的 patientInfo 包含 allergyHistory（string）和 allergyDetails（List\<AllergyDetail\>），但 allergyHistory 按需求文档 §3.1.6 映射约定应为"从健康档案的 allergen 列表以中文逗号拼接"——即 allergyHistory 是一个需要从健康档案实体提炼的拼接字段，而非患者自由输入文本。设计文本中 allergyHistory 的来源（前端传入 vs 后端从健康档案拼接）未明确。若 allergyHistory 由前端传入，则后端无法保证其与患者健康档案的一致性；若由后端从健康档案自动拼接，则 PrescriptionAssistRequest 的 allergyHistory 字段应为只读/后端填充而非前端必传。

**所在位置**：§3.4 PrescriptionAssistRequest.patientInfo；§3.2 AuditRequest.patientInfo；需求文档 §3.1.6 映射约定

**严重程度**：一般

**改进建议**：明确 allergyHistory 和 comorbidities 的数据来源语义：(a) 推荐方案：前端仅传 patientId，后端在 Service 层从健康档案实体自动提取并拼接 allergyHistory + comorbidities（与需求文档 §3.1.6 映射约定一致）；(b) 若允许前端传入以满足离线/缓存场景，则后端须增加与健康档案实体的一致性校验。在此场景下 health archive data 应作为"single source of truth"，前端传入值仅作为临时覆盖。在 §3.4 和 §3.2 补充数据来源说明。

---

### 问题 10

**问题描述**：需求文档 3.4.2 输出契约中 alerts 数组每项含 `alert_code / alert_message / severity`，设计文本 AuditAlert DTO 与此完全对齐。但需求文档中 alerts 的 severity 字段枚举值未在 3.4.2 中显式定义（"风险提示"条目仅列出字段名但未限定枚举值域），设计文本也未定义 AuditAlert.severity 的枚举类型或值域。与 AuditRiskLevel（PASS/WARN/BLOCK）的关系不明确——alerts 中的 severity 是否与 riskLevel 采用同一枚举？还是采用独立的提示严重程度枚举（如 INFO/WARNING/CRITICAL）？此模糊性将导致编码时随意假设。

**所在位置**：§1.3 AuditAlert DTO；§3.2 AuditResponse

**严重程度**：一般

**改进建议**：在 §3.2 AuditAlert 定义中补充 severity 字段的类型和值域。推荐方案：severity 类型为 String 或独立枚举（如 AlertSeverity: INFO / WARNING / CRITICAL），与 AuditRiskLevel 是不同的概念维度——AuditRiskLevel 是处方的整体风险判定，AuditAlert.severity 是单条提示的严重程度。并说明 severity=WARN 与 riskLevel=WARN 不要求等价。

---

### 问题 11

**问题描述**：需求文档 5.1 明确定义"分诊记录"实体包含"推荐科室、推荐医生、患者标识、时间戳、是否降级等"，设计文本 §3.1 TriageRecord 实体包含 aiRecommendedDepartments（JSON TEXT）、ruleMatchedDepartments（JSON TEXT）等字段，但缺少需求文档 5.1 中明确的"推荐医生"快照字段（recommendedDoctors）。TriageRecord 保存了 AI 推荐科室快照和规则匹配科室快照，但未保存通过 DoctorFacade 查询生成的推荐医生列表快照——推荐医生列表是 TriageResponse 的核心输出之一（需求文档 3.4.1 required output），若不在 TriageRecord 中持久化，则分诊结果的推荐医生数据将无法事后追溯，与需求文档 3.4.1 "分诊结果需持久化以供后续统计与质量分析"的可观测性约束和 5.1 "分诊记录"实体定义均不一致。

**所在位置**：§3.1 TriageRecord 实体字段列表

**严重程度**：严重

**改进建议**：在 TriageRecord 实体中增加推荐医生快照字段，如 recommendedDoctors（JSON TEXT，存储 TriageResponse.doctors 列表快照，含 doctorId/doctorName/departmentId/availableSlotCount/score）。需求文档 5.1 明确"推荐医生"为分诊记录核心字段，设计应与之对齐。

---

### 问题 12

**问题描述**：设计文本 §3.1 TriageRecord 写入时机为"分诊结果返回响应前同步写入"，finalDepartmentId 在患者挂号后通过"挂号模块发布的事件（RegistrationEvent）由事件监听器补充写入"。但 RegistrationEvent 的来源跨模块（挂号属 registration 模块，分诊属 consultation 模块），设计中仅提及"事件"但未定义事件的数据契约、发布端和消费端的模块归属。跨模块事件的传递依赖 Spring ApplicationEvent，但 consultation 模块不依赖 registration 模块（§2.2 "三个新模块之间不允许互相依赖"）——事件如何从 registration 模块传递到 consultation 模块的监听器，需要 application 模块聚合后的事件传播机制支撑，与 §9.3 ConfigChangeLog 的跨模块事件传递类似但更复杂（因为此处是业务模块间而非业务模块→admin 模块）。

**所在位置**：§3.1 TriageService "TriageRecord 写入时机"；§4.1 "finalDepartmentId 初始为空，患者挂号后通过事件补充写入"

**严重程度**：一般

**改进建议**：(a) 定义 RegistrationEvent 的事件契约（字段至少含 registrationId、patientId、departmentId、doctorId、registrationType、eventTime），事件类定义在 common-module-api 中；(b) 注册事件发布端为 registration 模块（挂号成功时发布），消费端为 consultation 模块（监听后更新 TriageRecord.finalDepartmentId），事件在 application 模块聚合后跨模块传播（与 TemplateConfigChangeEvent 模式一致）；(c) 在 §2.2 依赖方向或 §6 并发设计中补充跨模块事件传递机制的通用说明。

---

### 问题 13

**问题描述**：AiResult 代码中无 partialData 字段。设计文本 §3.3 声明"AI 超时时 AiResult.data 携带部分生成结果（AiResult 新增 partialData 字段或使用现有 data 字段承载部分结果）"，但实际代码 AiResult.java 仅有 success / data / errorCode / degraded / fallbackReason 五个字段，无 partialData。设计文本标注"新增 partialData 字段"但未明确说明这是对 ai-api 层 AiResult 类的修改——如果修改 AiResult 类，将影响全部 13 项 AI 能力的返回类型；如果仅在 MedicalRecordService 层面处理，则需明确 AiResult.data 在超时场景下可承载部分结果（使用现有 data 字段），而非新增字段。

**所在位置**：§3.3 MedicalRecordService "非流式超时降级路径"；§7 设计决策 "AiResult 超时降级模式"

**严重程度**：严重

**改进建议**：(a) 推荐方案：使用现有 AiResult.data 字段承载部分结果（避免修改全部 AI 能力共享的 AiResult 类），超时时 AI 实现将部分生成的 MedicalRecordGenResponse 放入 data 字段返回（此时 success=false + degraded=true + data 含部分结果）。在 §3.3 和 §7 中明确此设计选择并删除"新增 partialData"的歧义描述。(b) 若确需新增 partialData，则必须说明对 AiResult 类的影响范围及所有 AI 能力的兼容性验证。

---

### 问题 14

**问题描述**：设计文本多处引用"需求文档 3.4.x"但未在文档中提供需求文档的路径或引用标识。对于编码实现者而言，需要频繁对照需求文档验证字段级对齐，但设计文本仅从第 8 轮迭代开始积累了大量"对齐需求文档"的描述，缺乏一个集中化的字段级对齐映射表。当前字段对齐信息散布在 §1.3（核心抽象一览各 DTO 行的"对齐需求文档 3.4.x"备注）、§4.5（Converter 映射逻辑）、§10（ai-api DTO 扩展规格）三处，读者需交叉对比才能确认单个字段的对齐状态，增加遗漏风险。

**所在位置**：全文多处

**严重程度**：一般

**改进建议**：在文档末尾或独立附录中增加"需求契约对齐矩阵"——以表格形式集中列出设计 DTO 每个字段与需求文档 3.4.x 字段的映射关系，标注"完全对齐 / 语义升级 / 设计补充 / 偏差待确认"四类对齐状态。此表作为编码实现者的快速核验清单，避免散点式对齐信息遗漏。

---

### 问题 15

**问题描述**：设计文本 §4.4 /assist 主端点流程中"本地即时校验"包含"过敏冲突检查（patientInfo.allergyDetails 优先 / allergyHistory 回退 + prescriptionDraft.drugs 交叉比对）"，但此过敏冲突检查与处方审核（§3.2 AllergyCheckRule）存在逻辑重复——两者都会对处方药品列表与患者过敏信息做交叉比对。设计文本未说明这两处过敏检查的关系：是冗余的双重校验（辅助开方做一次、处方审核又做一次），还是各有侧重（辅助开方侧重于提示性告警、处方审核侧重阻断性审核）？从需求文档 3.4.10 看，辅助开方的 `allergy_warnings` 输出含 severity 字段（含 HIGH 级别），需明确 HIGH 级别告警在被处方审核消费时是否转换为 BLOCK 审核结果。

**所在位置**：§3.4 PrescriptionAssistService "本地即时校验"；§4.4 /assist 主端点流程；§3.2 AllergyCheckRule

**严重程度**：一般

**改进建议**：在 §3.4 或 §7 设计决策中补充"辅助开方过敏告警与处方审核过敏检查的关系"说明：明确辅助开方的 allergyWarnings 为即时提示性质（面向医生编辑期间的实时反馈），处方审核的 AllergyCheckRule 为提交时的正式审核判定（面向处方提交闭环），二者独立执行不互斥；辅助开方 allergyWarnings.severity=HIGH 不直接等价于处方审核 AuditRiskLevel=BLOCK，二者分属不同判定维度。

---

### 问题 16

**问题描述**：需求文档 3.1.3.1 规定"AI 服务不可用时提示用户并回退到传统按科室选择医生模式"，3.4.1 降级路径为"超时或不可用时后端返回降级标记 degraded=true，业务层据此回退到按科室选择医生模式"。设计文本 §3.1 TriageService 降级链为 AI→规则引擎→兜底科室，TriageResponse 仅返回 degraded=true 标记。但需求文档描述的降级目标是"回退到传统按科室选择医生模式"——即前端展示科室列表让患者手动选择，而非后端仍返回 AI/规则推荐的科室。设计文本的降级链与需求文档的降级目标语义不完全一致：设计文本的降级路径是后端仍返回推荐结果（通过规则或兜底），而需求文档的降级目标是前端切换到手动选择模式。二者并不矛盾但设计文本未说明前端在收到 degraded=true 后应如何调整 UI 交互。

**所在位置**：§3.1 TriageService 降级链；§4.1 降级判定；§1.3 TriageResponse.degraded 字段

**严重程度**：轻微

**改进建议**：在 §4.1 降级判断后补充前端行为说明——TriageResponse.degraded=true 时，前端显示降级提示"AI 暂不可用，以下为规则匹配结果"并仍渲染推荐科室列表（规则匹配/兜底结果），同时提供"手动选择科室"入口。明确 degraded 标记驱动的两类前端行为：(1) 降级提示文案展示；(2) "手动选择科室"入口出现。

---

## 整体评价

经过 8 轮迭代审议，本设计文档在需求响应完整度、架构一致性和落地可编码性方面已显著提升。多数核心问题（如 DialogueSession 可变性、BLOCK 阻断执行机制、DTO 字段定义、ai-api 层映射机制、强耦合处理等）在前序迭代中已被识别并修复。本轮审查发现的 4 项严重问题（chiefComplaint/additionalResponses 互斥语义与需求文档矛盾、AllergyCheckRule 遗漏合并症检查、TriageRecord 缺少推荐医生快照、AiResult partialData 增改影响范围不明确）属于前序内部审议未充分覆盖的维度——需求响应充分度和字段级对齐一致性。其余问题为一般/轻微级别，主要涉及枚举值域定义、数据来源语义明确化、跨模块事件契约补充等编码实现前的细节完善。整体而言，修复上述严重问题后，本文档可直接指导编码实现。
