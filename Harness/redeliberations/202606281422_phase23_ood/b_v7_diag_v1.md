# 质量审查诊断报告 — Phase 2/3 OOD 设计方案 v7

## 审查范围

审查维度侧重内部审议未充分覆盖的需求响应充分度、整体深度和完整性，以及从实际落地视角评估设计是否可直接指导编码实现。

---

## 问题清单

### 1. ai-api 层 DTO 与业务层 DTO 之间存在严重的字段级契约缺口，设计未定义二者间的映射/转换机制

**问题描述**：设计文档定义了各业务模块的输入/输出 DTO（如 TriageResponse、AuditRequest、PrescriptionAssistRequest 等），但各业务模块通过 AiService 接口调用 AI 时使用的是 ai-api 层的 DTO（如 `com.aimedical.modules.ai.api.dto.triage.TriageResponse`）。经查阅代码，ai-api 层现有 DTO 仍为 Phase 0 骨架占位状态——例如 `AiService.triage()` 返回 `AiResult<TriageResponse>` 但 ai-api 层 TriageResponse 仅有 `recommendedDepartments`（且每项仅 `departmentName`）和 `reason` 两个字段，缺少 `recommendedDoctors`/`matchedRules`/`sessionId`/`confidence`/`needFollowUp`/`followUpQuestion`/`degraded`/`fallbackHint` 等业务层需要的关键字段。同理 `PrescriptionCheckRequest`/`Response`、`PrescriptionAssistRequest`/`Response`、`MedicalRecordGenRequest`/`Response` 均为空壳类。

设计文档未说明：(a) ai-api 层 DTO 将如何扩展以承载新的字段级契约；(b) 业务层 DTO 与 ai-api 层 DTO 之间的转换规则（哪个字段映射到哪个字段、谁负责转换、转换在哪个层级发生）；(c) 当 ai-api 层 DTO 作为 AiService 方法的输入/输出类型时，新增业务字段是直接添加到现有 ai-api 类中还是通过继承/组合扩展。此缺口将直接阻塞编码实现——开发者无法确定 AiService 调用的入参和出参结构。

**所在位置**：§1.2 "各模块通过注入 AiService 接口调用 AI 能力" 描述 + §3.1/3.2/3.3/3.4 各 Service 协作描述 + §2.2 "与 AI 模块的协作关系"

**严重程度**：严重

**改进建议**：(a) 在 §2.2 "与 AI 模块的协作关系" 或新增 §2.3 中定义 ai-api 层 DTO 扩展策略——在 ai-api 层的 `TriageResponse`/`PrescriptionCheckRequest`/`PrescriptionAssistRequest`/`MedicalRecordGenRequest`/`MedicalRecordGenResponse` 等类中补充完整的字段定义，与需求文档 3.4.x 输入/输出契约对齐；(b) 明确业务层 DTO 与 ai-api 层 DTO 的转换规则和转换责任归属（建议由各模块的 Converter 类负责转换）；(c) 补充 ai-api 层现有 DTO 的完整字段列表，确保与 §1.3 核心抽象一览和 §3.x 各 Service 协作描述中的字段引用一致。

### 2. PrescriptionAssistRequest（ai-api 层）与业务层 PrescriptionAssistRequest（assist DTO）同名且职责不同，但设计未区分

**问题描述**：§3.4 定义了业务层 `PrescriptionAssistRequest`（`modules/prescription/dto/assist/PrescriptionAssistRequest.java`，含 diagnosis/examResults/patientInfo/existingPrescription/encounterId 五个字段，对齐需求文档 3.4.10 输入契约），同时 `AiService.prescriptionAssist()` 方法签名使用 ai-api 层 `com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistRequest`（当前为空壳类）。二者同名为 `PrescriptionAssistRequest` 但归属不同包，在实现时容易混淆。设计文档未说明这两个同名类的职责边界和字段差异——ai-api 层的 PrescriptionAssistRequest 应包含哪些字段（是否与业务层完全一致，还是 AI 特有子集），以及 `PrescriptionAssistServiceImpl` 调用 `AiService.prescriptionAssist()` 时如何从业务层 DTO 转换到 ai-api 层 DTO。

同理，`PrescriptionCheckRequest`/`PrescriptionCheckResponse` 在 ai-api 层和业务层也存在相同问题。

**所在位置**：§3.4 PrescriptionAssistService + §2.2 "与 AI 模块的协作关系" + AiService 接口签名

**严重程度**：严重

**改进建议**：(a) 在 §3.4 或 §2.2 中明确区分 ai-api 层 DTO 与业务层 DTO 的字段差异和命名空间关系；(b) 在目录结构 §2.1 的 ai-api 包下补充完整的 DTO 类列表和字段定义，或在 §3.x 中补充对 ai-api 层 DTO 字段的引用说明；(c) 若 ai-api 层 DTO 字段与业务层完全一致，可说明"复用策略"——业务层 DTO 直接实现 ai-api 层 DTO 接口或继承，避免重复定义和转换开销。

### 3. allergy_details 扩展容器完全未纳入设计，与需求文档过敏信息扩展性方案脱节

**问题描述**：需求文档 §3 字段映射段（约行 393–398）预见了 `allergy_details` 扩展容器的过渡方案——现有 allergy_history 保持 string 类型不变，同时预留可选扩展容器 `allergy_details`（array of object，含 allergen/reaction_type/severity/occurred_at），AI 服务收到 allergy_details 时优先使用。当前设计文档中 AuditRequest.patientInfo 和 PrescriptionAssistRequest.patientInfo 均仅包含 `allergyHistory（string）`，完全未提及 allergy_details 扩展容器。这意味着：(a) 当前设计无法承载结构化过敏信息，当业务方确认纳入 allergy_details 时需重新设计 patientInfo 字段结构；(b) AllergyCheckRule 的"与患者 allergyHistory 和 comorbidities 交叉比对"逻辑仅基于文本匹配，无法利用 reaction_type/severity 做细粒度过敏风险判定。

**所在位置**：§3.2 AuditRequest.patientInfo + §3.4 PrescriptionAssistRequest.patientInfo + §3.2 AllergyCheckRule 逻辑描述

**严重程度**：严重

**改进建议**：(a) 在 AuditRequest.patientInfo 和 PrescriptionAssistRequest.patientInfo 中增加 allergyDetails 可选字段（List<AllergyDetail>，每项含 allergen/reactionType/severity/occurredAt），与需求文档 allergy_details 扩展容器对齐；(b) 在 AllergyCheckRule 逻辑描述中补充：当 allergyDetails 存在时优先按结构化过敏信息做精确匹配（可区分 MILD/MODERATE/SEVERE 严重程度影响 AuditRiskLevel 判定），仅 allergyDetails 缺失时回退到 allergyHistory 文本匹配；(c) 在设计决策 §7 中补充"过敏信息扩展性"条目。

### 4. RX_ASSIST_AI_NO_RECOMMENDATION 错误码已定义但消费场景未描述，/assist 主端点 AI 返回无可推荐药品时的行为不明确

**问题描述**：§5.1 错误码表中列出了 `RX_ASSIST_AI_NO_RECOMMENDATION` 错误码（"AI 建议生成失败"类别 + 需求文档 3.4.10 明确定义此错误码），但设计文档未说明该错误码在 /assist 主端点的触发条件和消费方式。当 AiService.prescriptionAssist() 返回无可推荐药品结果时，/assist 端点应如何响应？是返回带 prescriptionDraft 为空列表的 PrescriptionAssistResponse + doseWarnings/allergyWarnings（基于本地即时校验结果），还是返回包含 RX_ASSIST_AI_NO_RECOMMENDATION 错误码的 4xx 响应？此决策直接影响前端"无可推荐药品"场景的交互逻辑。

**所在位置**：§3.4 PrescriptionAssistService + §4.4 辅助开方场景 + §5.1 错误码表

**严重程度**：严重

**改进建议**：在 §4.4 /assist 主端点流程中补充"AI 返回无可推荐药品"场景的处理路径：明确是 (a) 返回空 prescriptionDraft + 本地校验结果 + RX_ASSIST_AI_NO_RECOMMENDATION 标记供前端友好展示"无可推荐药品，请手动开方"提示，或 (b) 作为业务异常返回 4xx/422 响应。同时说明此错误码与 /assist/check-dose 异步 AI 建议 AiSuggestionResult 的 FAILED 状态是否有关联。

### 5. TriageService 协作描述中"管理 DialogueSessionManager"的闭环缺失——分诊完成后 TriageRecord 的写入时机和触发方未在行为契约中体现

**问题描述**：§4.1 智能分诊场景行为契约末尾提及"持久化: 分诊完成后写入 TriageRecord"，但 TriageService 的接口职责描述中未包含此步骤。更关键的是：§3.1 TriageRecord 被定义为满足 3.4.1 可观测性约束的持久化实体，但 (a) 目录结构中 TriageRecordRepository 仅有仓库层，无对应的 Service 写入方法签名；(b) 行为契约中未说明 TriageRecord 写入是在 TriageServiceImpl 内部执行还是由其他机制触发（如事件监听）；(c) TriageRecord.finalDepartmentId 的赋值时机不明确——分诊响应只返回推荐科室列表，"最终选择科室"是患者挂号后才确定的，TriageRecord 何时补充此字段？若分诊完成时写入，则 finalDepartmentId 为空需后续补充。

**所在位置**：§3.1 TriageService + TriageRecord + §4.1 智能分诊场景

**严重程度**：一般

**改进建议**：(a) 在 TriageService 接口职责描述中明确 TriageRecord 写入步骤和时机（建议在 TriageServiceImpl 返回响应前同步写入，此时 finalDepartmentId 为空/nullable）；(b) 在 TriageRecord 实体描述或 §7 设计决策中说明 finalDepartmentId 的补充写入机制——患者完成挂号后，挂号模块通过事件或回调更新 TriageRecord.finalDepartmentId；(c) 考虑 finalDepartmentId 是否影响 TriageRecord 的创建——若分诊与挂号解耦，可将 finalDepartmentId 从"创建时写入"改为"后续补充写入"，在实体描述中标注 nullable。

### 6. 多轮分诊场景下 TriageServiceImpl 如何将 DialogueSession 的上下文传递给 AiService.triage() 未定义

**问题描述**：§3.1 TriageService 协作描述中写"委托 AiService.triage(session context)"，但 AiService.triage() 的方法签名为 `CompletableFuture<AiResult<TriageResponse>> triage(TriageRequest request)`，而 ai-api 层 TriageRequest 仅有 `chiefComplaint` 一个字段。多轮追问场景下，TriageServiceImpl 需要将 DialogueSession 中累积的完整对话上下文（chiefComplaint + 历次 additionalResponses + 当前轮追问）组装为 AiService 可消费的输入。设计文档未说明：(a) 多轮上下文如何组装到 ai-api 层 TriageRequest 中（是将所有 Q&A 拼接到 chiefComplaint 字段，还是 ai-api 层 TriageRequest 需扩展 additionalResponses 字段）；(b) 单次调用方式（拼接模式）与多轮调用方式（增量模式）在 AiService 调用层是否有差异。此缺口直接影响 TriageServiceImpl 的编码实现。

**所在位置**：§3.1 TriageService 协作描述 + §4.1 智能分诊场景 + AiService.triage() 签名

**严重程度**：严重

**改进建议**：(a) 在 §3.1 TriageService 中补充对 AiService.triage() 的调用组装说明：明确 TriageServiceImpl 在调用 AiService.triage() 前，将 DialogueSession 中的 chiefComplaint + 累计 additionalResponses（含当前轮）组装为 ai-api 层 TriageRequest 的完整输入（建议 ai-api 层 TriageRequest 扩展 additionalResponses 字段与需求文档 3.4.1 输入契约对齐）；(b) 在 §3.1 或 §7 中明确组装策略——推荐"全量拼接"策略（每次调用时将完整上下文传入 AiService，AI 服务侧无需维护会话状态），以及 DialogueSession 上下文到 ai-api 层 TriageRequest 的字段映射。

### 7. 从 TriageResponse.deprecated 风险等级到 AuditRiskLevel 的降级路径下本地规则聚合逻辑缺少精确的"风险等级判定规则表"

**问题描述**：§3.2 LocalRuleResult 聚合描述为"若任一规则 severity 为 BLOCK 则整体判定为 BLOCK；若无 BLOCK 但存在 WARN 则整体判定为 WARN；全部 PASSED 则判定为 PASS"。但此聚合规则存在边界歧义：(a) 本地规则 severity 使用的是 AuditRiskLevel 枚举还是独立的 severity 枚举？设计文本中 LocalRuleResult.severity 的类型描述仅为"严重程度"，未明确指向 AuditRiskLevel；(b) AllergyCheckRule 命中时产出 BLOCK 级别——但需求文档 3.2.2.7 定义的 HIGH/MEDIUM 风险规则为"严重过敏冲突→HIGH，轻度过敏→MEDIUM"，当前 AllergyCheckRule 一律输出 BLOCK 级别，无法区分严重程度；(c) 剂量相关规则（DosageLimitRule/SpecialPopulationDosageRule）已存在 WARN/BLOCK/CRITICAL 多级输出，但本地规则校验段的描述未说明剂量超过程度如何映射到 LocalRuleResult.severity。

**所在位置**：§3.2 LocalRuleEngine + LocalRuleResult 聚合逻辑 + AllergyCheckRule/DosageLimitRule/SpecialPopulationDosageRule 逻辑描述

**严重程度**：一般

**改进建议**：(a) 明确 LocalRuleResult.severity 的类型为 AuditRiskLevel 枚举（与聚合逻辑一致），在 §3.2 补充类型引用；(b) 为每条规则补充 severity 判定细节：AllergyCheckRule 命中时根据过敏冲突类型区分——严重过敏（如过敏性休克风险）输出 BLOCK、轻度过敏（如皮疹）输出 WARN；DosageLimitRule 根据 dosage / dosageMax 比率区分输出 BLOCK（严重超标）或 WARN（接近上限）；(c) 或定义独立的规则严重程度映射表，明确各规则在不同触发条件下输出的 LocalRuleResult.severity 值。

### 8. WARN 级处方"强制提交并留痕"的前后端交互链路不完整——前端如何通知后端"医生选择强制提交"未定义

**问题描述**：§3.2 和 §4.2 描述了 WARN 级别下医生可"强制提交并留痕"，AuditRecord 新增了 forceSubmitted/forceSubmitTime 字段。但行为契约中未定义：(a) 前端如何将"医生选择强制提交"的决定通知后端——是处方提交接口增加 forceSubmit 参数，还是独立的"确认强制提交"端点？(b) 如果是处方提交接口增加参数，那 WARN 级别的处方提交与正常提交是否走同一个 API 端点？(c) 后端收到强制提交请求后，如何校验该处方确实经过了 WARN 级审核（防止未经审核直接强制提交的绕过攻击）。此缺口影响前端交互逻辑和后端 API 设计。

**所在位置**：§3.2 AuditRecord.forceSubmitted + §4.2 WARN 分支 + PrescriptionAuditController

**严重程度**：一般

**改进建议**：(a) 在 §4.2 处方审核场景的 WARN 分支中补充前端→后端的交互链路：建议在处方提交端点（如 POST /api/prescription/submit）增加 forceSubmit（bool，可选）参数，当 forceSubmit=true 时后端校验该处方存在 WARN 级最新 AuditRecord 且 isLatest=true，通过后写入 AuditRecord.forceSubmitted=true；(b) 在 §3.2 PrescriptionAuditController 的端点列表中补充处方提交端点描述，或在 §4.2 中明确处方提交与处方审核的接口边界；(c) 补充后端校验逻辑：forceSubmit=true 时必须存在对应的 WARN 级 AuditRecord，否则返回错误码。

### 9. 分诊场景中"推荐医生"的推荐机制和数据来源未定义，RecommendedDoctor 的 score 计算和 availableSlotCount 获取路径未说明

**问题描述**：TriageResponse.doctors 字段要求包含 RecommendedDoctor（含 doctorId/doctorName/departmentId/availableSlotCount/score），但设计文档未说明推荐医生列表的数据来源和生成机制——AI 端是否返回推荐医生列表，还是后端根据 AI 返回的推荐科室从医生排班数据中查询？若 AI 返回推荐医生：(a) ai-api 层 TriageResponse 目前仅有 recommendedDepartments 和 reason，无 recommendedDoctors，需扩展 ai-api 层 DTO；(b) AI 返回的 availableSlotCount 是否可信，还是需要后端实时查询排班系统校验。若后端自行组装：(a) 根据推荐科室查询医生排班表获取 availableSlotCount 和 score 的具体逻辑未定义；(b) consultation 模块不依赖 doctor 模块（§2.2 依赖规则："三个新模块之间不允许互相依赖"），查询医生排班数据需跨模块，但设计未定义跨模块医生数据获取的机制（如医生模块提供门面接口或事件）。

**所在位置**：§3.1 TriageResponse.doctors + RecommendedDoctor + §2.2 依赖规则 + §4.1 智能分诊场景

**严重程度**：严重

**改进建议**：(a) 在 §3.1 TriageService 或 §4.1 智能分诊场景中明确推荐医生列表的生成机制——推荐由 AI 生成推荐医生列表（则需扩展 ai-api 层 TriageResponse 增加 recommendedDoctors 字段），或后端根据 AI 返回的推荐科室从医生排班数据中查询；(b) 若需跨模块查询医生排班数据，在 §2.2 中补充 cross-module 数据获取机制——建议在 common-module-api 中定义 DoctorFacade 接口（提供查询可挂号医生列表和时段数量），consultation 模块通过 common-module-api 调用，不直接依赖 doctor 模块 impl 层；(c) 明确 availableSlotCount 的数据来源和实时性要求。

### 10. 病历生成非流式超时场景的"部分保留"行为契约缺少对 AiResult 降级模式的定义

**问题描述**：需求文档 3.4.3 超时阈值服务要求定义了非流式模式下超时时需返回 partial_content（截至当前已生成的全部结构化字段快照），错误码 MR_GEN_AI_TIMEOUT。但当前设计中：(a) MedicalRecordService 的降级描述仅覆盖"AI 完全不可用"场景（返回空字段集 + missingFieldHints），未覆盖"AI 超时但已部分返回"场景；(b) AiResult 的结构中仅有 success/degraded/errorCode/fallbackReason/data 字段，无法承载 partial_content 语义——超时时 data 可能为 null（当前 AiResult.failure() 工厂方法将 data 设为 null），但需求要求返回已生成的字段快照；(c) §4.3 行为契约中未定义非流式超时场景下的降级路径，前端如何接收部分生成的病历内容。

**所在位置**：§3.3 MedicalRecordService + §4.3 病历生成场景 + §5.5 AI 超时配置

**严重程度**：一般

**改进建议**：(a) 在 §4.3 病历生成场景中补充非流式超时降级路径：AI 超时时 AiResult.success=false 但 data 携带部分生成结果（AiResult 需扩展以支持此场景，或设计 MedicalRecordService 在超时时返回 RecordGenerateResponse 携带已生成字段 + missingFieldHints + MR_GEN_AI_TIMEOUT 标记）；(b) 在 §5.2 或 §6.2 中补充 AiResult 对 partial_content 的承载方式——建议在 AiResult 中增加 partialData 字段（泛型 T），超时时 partialData 携带部分结果；(c) 确保"分层保护"降级策略同时覆盖超时部分返回和完全不可用两种降级场景。

### 11. 中间人攻击风险——WARN 级处方审核与强制提交的时序竞态未防护

**问题描述**：当前设计中，医生在 WARN 级审核结果下选择"强制提交"时，存在时序竞态风险：(a) 医生看到 WARN 审核结果并决定强制提交，但在提交前处方内容可能已被修改（如其他协作者或同医生的另一次编辑），此时 AuditRecord 中记录的强制提交是针对旧处方版本的审核结果；(b) AuditRecord 通过 prescriptionOrderId 关联处方，但 AuditRecord 中未保存 originalPrescription 的完整快照或 hash 摘要，无法在强制提交时校验处方内容是否与审核时一致。

**所在位置**：§3.2 AuditRecord + §4.2 WARN 分支

**严重程度**：一般

**改进建议**：(a) 在 AuditRecord 实体中确认 originalPrescription 字段是否保存完整处方快照（当前描述为"原始处方、riskLevel、aiResult、auditIssues 等审核结果数据"足以支撑，建议在字段列表中显式标注 originalPrescription 为 JSON 文本类型存储完整处方快照）；(b) 在 WARN 级强制提交路径中补充处方版本校验——后端收到 forceSubmit=true 请求时，比较当前处方内容与 AuditRecord.originalPrescription 是否一致，不一致时返回"处方已变更，请重新审核"错误提示；(c) 或在处方提交接口中要求携带 auditRecordId 参数，后端校验 auditRecordId 对应的审核结果是否为当前处方的最新审核。

### 12. MedicalRecord 实体缺少 MedicalRecordField 级字段检索/更新机制定义，编码时无法将 fields 映射落实到数据库

**问题描述**：MedicalRecord 实体字段描述为"病历内容结构化 JSON"，RecordGenerateResponse.fields 为"以 MedicalRecordField 为键的键值映射"。但设计未定义：(a) MedicalRecord 实体中病历内容字段的数据库存储形式——是单列 JSON TEXT 存储，还是按 MedicalRecordField 枚举值拆分为独立列？(b) 若为 JSON TEXT 存储，查询和更新的 SQL 层面如何操作单个字段？(c) MedicalRecord 更新时（如医生手动补全缺失字段），是整体覆盖还是按字段增量更新？此缺口将影响 JPA Entity 的字段定义和 Repository 查询方法的签名。

**所在位置**：§3.3 MedicalRecord 实体 + MedicalRecordField 枚举 + MedicalRecordRepository

**严重程度**：一般

**改进建议**：(a) 在 §3.3 MedicalRecord 实体描述中明确病历内容的存储形式——推荐单列 JSON TEXT（字段名为 contentJson），配合 JPA @Convert 或 Jackson 序列化/反序列化；(b) 补充 MedicalRecordRepository 的查询方法列表——至少包含 findByVisitId、findByPatientId 等；(c) 在 MedicalRecordService 接口描述中补充病历更新方法的签名和增量更新语义——医生手动补全缺失字段时，按字段粒度增量更新 JSON 中的对应键值。

### 13. 配置变更审计日志跨模块事件传递的事务边界未定义——规则变更事件由哪个模块发布

**问题描述**：§9.3 定义了 ConfigChangeLog 实体归属 admin 模块，规则变更和模板变更的审计日志由 admin 模块监听器写入。但：(a) 分诊规则（TriageRuleEngine）归属 consultation 模块，规则变更事件由 consultation 模块中的规则管理操作触发——但 consultation 模块不依赖 admin 模块（§2.2 依赖规则禁止跨业务模块依赖），事件从 consultation 模块发布到 admin 模块监听器依赖 Spring ApplicationEvent 的跨模块传播，设计需确认两者在同一 Spring 上下文（application 模块聚合启动时统一扫描）；(b) TemplateConfigChangeEvent 携带 oldTemplate 与 newTemplate 的前后快照——但事件发布发生在 admin 模块的模板管理 Service 中，而模板查询发生在 medical-record 模块，事务边界如何保证？若事件发布与数据持久化不在同一事务中，admin 模块持久化模板变更后进程崩溃，事件未发布将导致缓存未失效+AuditLog 缺失。

**所在位置**：§9.2/§9.3 TemplateConfigChangeEvent + ConfigChangeLog + §2.2 依赖规则

**严重程度**：一般

**改进建议**：(a) 在 §9.2 或 §6 中补充跨模块事件传播的事务一致性策略——推荐使用 @TransactionalEventListener(phase=AFTER_COMMIT) 确保仅在事务提交后发布事件，避免事务回滚导致缓存错误失效；(b) 确认 Spring ApplicationEvent 在 application 模块统一聚合启动后可跨模块传播，在 §2.2 或 §6 中补充说明；(c) 补充事件丢失的补偿机制——如 TemplateConfigManager 的定时刷新（refreshAfterWrite 60 秒）可覆盖事件丢失场景，但需在文档中明确此幂等兜底。

### 14. consultation 模块的 AI 分诊首次调用缺少对需求文档 3.4.1 `session_id` 必填语义的完整对齐方案

**问题描述**：设计文档在 §3.1 DialogueCreateRequest 中解释了 sessionId 首轮为空、后续必填的设计，并说明"需求文档的'必填'指多轮场景下必填，首轮由服务端生成 UUID v4 后返回"作为映射说明。然而需求文档 3.4.1 输入契约中 `session_id` 的约束为"string，必填"，且多轮场景首条消息的 `session_id` 语义为标识一个新对话的开始——前端应有义务生成或传递一个 session_id（即使是前端生成的 UUID），以确保请求契约的"必填"约束不被违反。当前设计让首轮请求 sessionId 为空，虽然业务逻辑可行，但与需求文档"必填"的显式契约存在语义偏差——需求文档未为首轮请求提供可选性说明。若前端严格按需求契约实现，首轮请求也会传入 session_id，此时设计中的"首轮 sessionId 为空"假设将导致逻辑冲突。

**所在位置**：§3.1 DialogueCreateRequest + §7 设计决策 "session_id 必填/可选语义"

**严重程度**：一般

**改进建议**：(a) 显式标注此设计与需求文档的差异点，并说明是否需要与需求方澄清——建议方案为：首轮请求也要求前端生成并传入 sessionId（如前端生成 UUID v4），消除"首轮为空"的特殊分支，使 DialogueCreateRequest.sessionId 始终为必填字段，与需求文档 3.4.1 完全对齐；(b) 若保留当前设计，应在设计决策中补充与需求方的确认记录或待办项，避免验收时因契约不一致被 blocking。

### 15. DrugInteractionPair 和 DrugCompositionDict 实体缺少持久化层定义

**问题描述**：§2.1 目录结构中列出了 `DrugInteractionPair.java`（药物相互作用数据实体）和 `DrugCompositionDict.java`（药品成分字典实体），但：(a) 两者的目录归属在 prescription/rule/ 包下而非 entity/ 或 repository/ 包下，与 JPA Entity 的惯例不一致（对比 AuditRecord 在 entity/ 包下）；(b) 两者未在 §3.2 核心抽象一览或正文中定义字段结构——DrugCompositionDict 仅描述为"drugCode → ingredientList"，但 ingredientList 的元素类型（String 还是结构化对象）未明确；(c) 缺少对应的 Repository 定义（目录中未列出 DrugCompositionDictRepository 或 DrugAllergyMappingRepository）；(d) DrugInteractionPair 在 Phase 2/3 标注为"预留骨架不参与运行时校验"，但 DrugCompositionDict 和 DrugAllergyMapping 需要查询接口来支撑 DuplicateCheckRule 和 AllergyCheckRule 的运行时校验。

**所在位置**：§2.1 目录结构 + §3.2 LocalRuleEngine 数据来源说明

**严重程度**：一般

**改进建议**：(a) 将 DrugCompositionDict 和 DrugAllergyMapping 移至 entity/ 包下作为 JPA Entity，或在 rule/ 包下新增数据层子包，保持与项目 JPA Entity 一致的存放位置；(b) 在 §3.2 或 §8 中补充 DrugCompositionDict 和 DrugAllergyMapping 的核心字段定义——至少包含主键（drugCode）、成分列表（ingredients，类型为 List<String> 或 JSON）、维护入口（admin 模块）；(c) 在 §2.1 目录结构中补充 DrugCompositionDictRepository 和 DrugAllergyMappingRepository；(d) §8.1 种子数据中补充 DrugCompositionDict 和 DrugAllergyMapping 的初始化脚本路径。

### 16. 分诊规则引擎的规则数据实体和 TriageRule 匹配模型未定义

**问题描述**：TriageRuleEngine 负责症状→科室匹配，规则存储在数据库并支持热加载。但设计未定义：(a) 规则的数据实体（TriageRule/TriageRuleCondition）字段结构——症状关键词如何映射到科室？是单条规则=一个症状关键词+一个科室，还是支持条件组合？(b) TriageRuleEngine.match() 方法的输入/输出签名——输入是什么类型（主诉文本、结构化症状列表？），输出是 List<RecommendedDepartment> 还是自定义类型？(c) 规则的 currentRuleVersion() 和 currentRuleSetId() 的数据来源——版本号和规则集标识存储在哪个数据表/实体中？

**所在位置**：§3.1 TriageRuleEngine + §4.1 智能分诊场景 + §7 "分诊规则源"设计决策

**严重程度**：一般

**改进建议**：(a) 在 §3.1 TriageRuleEngine 或新增 §8.x 中补充分诊规则的数据模型——至少定义 TriageRule 实体的核心字段（ruleId、ruleSetId、ruleVersion、conditions、resultDepartmentId、resultDepartmentName、score）；(b) 补充 TriageRuleEngine.match() 方法签名——输入为主诉文本/结构化症状 + ruleVersion + ruleSetId，输出为 List<RecommendedDepartment>；(c) 说明规则版本和规则集的存储位置（建议在 TriageRule 实体或独立的 TriageRuleSet 实体中存储 ruleVersion 和 ruleSetId）。

### 17. 处方审核与辅助开方场景中"处方提交"端点不在当前设计范围内，但 BLOCK 阻断和 WARN 留痕的端到端闭环依赖此端点

**问题描述**：当前设计覆盖了处方审核（/api/prescription/audit）和辅助开方（/api/prescription/assist + /assist/check-dose），但处方"提交"端点（如 POST /api/prescription/submit）不属于当前包范围。然而以下关键行为依赖处方提交端点：(a) BLOCK 阻断——处方审核返回 BLOCK 时阻止提交，但提交端点需校验是否有未解决的 BLOCK 级审核结果；(b) WARN 强制提交留痕——提交端点需接收 forceSubmit 参数并写入 AuditRecord；(c) PrescriptionDraftContext 读取和清理——提交成功后清理草稿上下文中的 CRITICAL 标记。设计文档当前默认这些行为"在提交端点中实现"但未显式定义提交端点的契约，也未标注此端点属于 design boundary 之外还是将在本设计范围内补充。

**所在位置**：§3.2 PrescriptionAuditController + §3.4 PrescriptionAssistController + §4.2 处方审核场景

**严重程度**：一般

**改进建议**：(a) 在 §1.1 设计目标或 §2.2 中显式标注"处方提交端点"的设计边界——若不在 Phase 2/3 范围，则在 §4.2 BLOCK/WARN 分支中补充"处方提交校验在处方提交端点中实现（待设计）"的显式标注，并在 §7 设计决策中记录待办项；(b) 若在本设计范围内，在 §3.2 PrescriptionAuditController 中补充处方提交端点的简要契约定义（至少包含端点路径、请求参数、BLOCK 校验逻辑、WARN 留痕逻辑、PrescriptionDraftContext 清理逻辑）。

### 18. 分诊场景对话会话存储 ConcurrentHashMap 的跨实例问题——水平扩展时 session 不共享

**问题描述**：DialogueSessionManager 使用 ConcurrentHashMap + ScheduledExecutorService 管理会话，AiSuggestionResult 同样使用 ConcurrentHashMap 暂存，PrescriptionDraftContext 也是 ConcurrentHashMap。这些内存存储方案在单 JVM 实例下可行，但若后端水平扩展为多实例（负载均衡），请求可能被路由到不同实例，导致 session 查不到、AI 建议丢失、草稿上下文不一致。设计文档在 §7 设计决策"多轮对话存储"中提及"Phase 5 迁移数据库"，但未说明多实例场景的中短期方案。

**所在位置**：§6.1 对话会话并发管理 + §6.3 异步 AI 建议 + §6.4 处方草稿上下文 + §7 "多轮对话存储"设计决策

**严重程度**：一般

**改进建议**：(a) 在 §7 设计决策或 §6 中补充水平扩展说明——Phase 2/3 假设单实例部署（或使用 sticky session），若需多实例部署则须将 ConcurrentHashMap 替换为分布式缓存（如 Redis）；(b) 在 §6.1 和 §6.3 和 §6.4 中补充部署约束说明——当前内存存储方案适用于单实例或 sticky session 的多实例部署；(c) 在设计决策中标注 Phase 5 迁移节点——三项内存存储均需在 Phase 5 迁移至持久化或分布式缓存。
