# Phase 2/3 OOD 设计方案 质量审查报告（v8 第2轮）

---

## 审查发现

### 问题 1

**问题描述**：DoctorFacade 接口在设计文本 §2.1 目录结构中放置于 `common-module-api/.../auth/DoctorFacade.java`，但实际代码中 `common-module-api/.../auth/` 包下仅有 UserFacade / CurrentUser / UserInfoResponse（已验证源码），该包的语义定位是"用户认证相关门面"。DoctorFacade 是跨模块医生排班/可用性查询门面，与"认证"包名不匹配。

**所在位置**：§2.1 目录结构 `common-module-api/.../auth/UserFacade.java, DoctorFacade.java`；§1.3 核心抽象一览 DoctorFacade 条目

**严重程度**：一般

**改进建议**：在 common-module-api 下为 DoctorFacade 创建独立子包（如 `commonmodule/doctor/DoctorFacade.java`），与 auth 包解耦。

---

### 问题 2

**问题描述**：需求文档 §3.4.1 输出契约中 `matched_rules` 字段后无子结构定义——原文仅写"`matched_rules`。"，句号结束，未列出每项应含的子字段（与 alerts 显式列出 alert_code / alert_message / severity 的详略程度明显不同）。这是需求侧缺口——设计文档不能被动"对齐"一个不存在的子结构定义，而需主动定义 matched_rules 的子字段。当前设计文本 §1.3 MatchedRule DTO 定义为 ruleId / ruleName / score，但未在 §7 设计决策中说明这是设计侧的主动选择（而非被动的需求对齐），也未论证子字段选择理由（为何含 score 而不含 confidence/reason 等）。

**所在位置**：§1.3 核心抽象一览 MatchedRule DTO；§10.1 ai-api 层 MatchedRuleItem；§7 设计决策表（缺少 matched_rules 子字段设计决策条目）

**严重程度**：一般

**改进建议**：在 §7 设计决策中增加 matched_rules 子字段的设计决策条目，说明：(a) 需求文档 §3.4.1 matched_rules 子结构未定义（需求侧缺口）；(b) 设计侧主动定义 ruleId / ruleName / score 三个子字段，其中 score 统一承载匹配评分（语义上涵盖需求文档 3.4.1 中 recommended_departments.score 的同构字段）；(c) 若后续需求补充 confidence / reason 子字段，可通过 DTO 扩展兼容。

---

### 问题 3

**问题描述**：需求文档 §3.4.2 规定处方审核本地规则校验最小检查项集 #2 为"药品禁忌检查：检查处方药品是否与患者的过敏史冲突；是否与患者的合并症冲突"——合并症检查（即"药品是否与患者合并症如肾功能不全、肝功能不全等禁忌冲突"）是检查项 #2 的明确子要求。设计文本 §3.2 AllergyCheckRule 仅实现过敏史冲突检查，合并症-药品禁忌检查无对应规则实现，也无独立规则类覆盖。此为需求响应的明显遗漏。

**所在位置**：§3.2 LocalRuleEngine 实现范围表 + AllergyCheckRule 描述

**严重程度**：严重

**改进建议**：(a) 新增 ContraindicationCheckRule（或扩展 AllergyCheckRule 为 AllergyAndContraindicationCheckRule），补充合并症-药品禁忌检查子项：遍历处方药品列表，对每个药品查询其禁忌症列表，与 patientInfo.comorbidities 做交集比对。命中时产出 WARN 或 BLOCK 级 LocalRuleResult（severity 判定逻辑可参考 AllergyCheckRule 的分级策略）。(b) 需同步更新 §3.2 实现范围表、§2.1 目录结构和 §7 设计决策，补充禁忌症数据来源实体（如新增 DrugContraindicationMapping JPA @Entity 或复用现有 DrugAllergyMapping 的扩展结构）。

---

### 问题 4

**问题描述**：AiResult 代码确认仅含 success / data / errorCode / degraded / fallbackReason 五个字段（已验证 `AiResult.java` 源码），无 partialData 字段。设计文本 §3.3 声明"AI 超时时 AiResult.data 携带部分生成结果（AiResult 新增 partialData 字段或使用现有 data 字段承载部分结果）"，此描述具有歧义——既提到"新增 partialData 字段"又提到"使用现有 data 字段"，未做明确选择。若新增 partialData 字段，将修改全部 13 项 AI 能力共享的 AiResult 泛型类，影响范围远超病历生成模块；若使用现有 data 字段承载，则超时时 AiResult 的状态应为 success=false + degraded=true + data 含部分结果，但这与当前 AiResult.failure() / AiResult.degraded() 的工厂方法语义冲突（二者均将 data 设为 null）。编码实现者无法从当前描述中确定实现路径。

**所在位置**：§3.3 MedicalRecordService "非流式超时降级路径"；§7 设计决策 "AiResult 超时降级模式"

**严重程度**：严重

**改进建议**：明确选择一种实现路径并删除歧义描述。推荐方案：使用现有 AiResult.data 字段承载部分结果——超时时 AI 实现将部分生成的 MedicalRecordGenResponse 放入 data 字段返回（success=false + degraded=true + data 非空）。需同步说明：(a) AiResult.degraded() 工厂方法需新增重载 `degraded(String fallbackReason, T partialData)` 以支持携带部分数据；或 (b) 超时场景由 AI 实现直接构造 AiResult(success=false, partialData, null, true, fallbackReason)，不使用工厂方法。在 §3.3 和 §7 中明确此选择并删除"新增 partialData"描述。

---

### 问题 5

**问题描述**：需求文档 5.1 定义"分诊记录"实体核心字段为"主诉、推荐科室、推荐医生、患者标识、时间戳、是否降级等"。设计文本 §3.1 TriageRecord 实体包含 aiRecommendedDepartments（JSON TEXT）和 ruleMatchedDepartments（JSON TEXT），但缺少"推荐医生"快照字段（recommendedDoctors）。推荐医生列表是 TriageResponse 的核心输出之一，也是需求文档 5.1 分诊记录实体的明确字段。TriageRecord 未持久化推荐医生数据将导致分诊结果的可观测性和事后追溯不完整。

**所在位置**：§3.1 TriageRecord 实体字段列表

**严重程度**：严重

**改进建议**：在 TriageRecord 中增加 recommendedDoctors（JSON TEXT）字段，存储 TriageResponse.doctors 列表快照（含 doctorId / doctorName / departmentId / availableSlotCount / score）。

---

### 问题 6

**问题描述**：需求文档 §3.4.2 输出契约中 alerts 数组每项含 alert_code / alert_message / severity，需求文档未显式定义 alerts.severity 的枚举值域。设计文本 AuditAlert DTO §1.3 仅列出 alertCode / alertMessage / severity 三个字段名，也未定义 severity 的类型或值域。编码实现时 severity 应采用 AuditRiskLevel（PASS/WARN/BLOCK）还是独立枚举（如 INFO/WARNING/CRITICAL）不明确。从需求文档 §3.2.2.7 "风险等级规则"的三级定义（高风险/中风险/低风险）和 §3.4.2 risk_level 枚举（LOW/MEDIUM/HIGH）看，alerts.severity 应是单条提示的严重程度（而非处方的整体风险等级），二者概念维度不同。

**所在位置**：§1.3 AuditAlert DTO；§3.2 AuditResponse；§10.2 ai-api 层 AlertItem

**严重程度**：一般

**改进建议**：在 §1.3 / §3.2 AuditAlert 定义中补充 severity 字段的类型和值域。推荐方案：severity 类型为 String 或独立枚举（如 AlertSeverity: INFO / WARNING / CRITICAL），与 AuditRiskLevel 是不同维度——AuditRiskLevel 是处方整体风险判定，AuditAlert.severity 是单条提示的严重程度。需同步更新 §10.2 ai-api 层 AlertItem.severity 定义。

---

### 问题 7

**问题描述**：设计文本 §3.4 DosageThresholdService 描述"四级均未命中，降级返回 WARN 级 DosageAlert 并携带 RX_ASSIST_DOSE_STANDARD_NOT_FOUND 错误码"，但 DosageAlert 类定义（§1.3 / §3.4）仅含 alertLevel / alertMessage / drugCode / 当前剂量 / 建议值，无承载错误码的字段。RX_ASSIST_DOSE_STANDARD_NOT_FOUND 错误码的传递路径不明确——无法通过 DosageAlert 本身传递，DosageCheckResponse 也只有 alerts + taskId 两个字段。

**所在位置**：§3.4 DosageAlert 类定义；§3.4 DosageCheckResponse；§4.4 check-dose 流程

**严重程度**：一般

**改进建议**：在 DosageAlert 或 DosageCheckResponse 中增加 errorCode（String，可选）字段，统一承载各类剂量校验相关错误码。在 §4.4 check-dose 流程中明确 RX_ASSIST_DOSE_STANDARD_NOT_FOUND 的传递路径。

---

### 问题 8

**问题描述**：设计文本 §4.2 处方提交端点 WARN 级强制提交时需校验"当前处方与 AuditRecord.originalPrescription 是否一致"，但未定义"一致"的比较语义——JSON 全文二进制比较 vs 业务字段结构化比较。JSON 文本级差异（字段顺序、null 与缺失字段、数值精度）可能产生误判。

**所在位置**：§4.2 处方提交端点"处方版本校验"逻辑

**严重程度**：一般

**改进建议**：补充"一致"的比较语义定义：按业务字段做结构化比较（drugId + dose + frequency + duration + route 五字段组合比对），忽略 JSON 文本级的格式差异。药品增删、剂量变化等业务实质变更判为"不一致"；仅 JSON 序列化格式差异判为"一致"。

---

### 问题 9

**问题描述**：设计文本 §3.4 和 §3.2 中 AuditRequest.patientInfo 和 PrescriptionAssistRequest.patientInfo 均包含 allergyHistory（string）和 allergyDetails（List<AllergyDetail>），但 allergyHistory 的数据来源未明确——由前端传入还是后端从健康档案自动拼接？需求文档 §3.1.6 映射约定规定 allergyHistory 应"从健康档案的 allergen 列表以中文逗号拼接"，即后端是实现者。若前端传入，后端无法保证其与健康档案一致性；若后端拼接，则前端不应传此字段。

**所在位置**：§3.2 AuditRequest.patientInfo；§3.4 PrescriptionAssistRequest.patientInfo

**严重程度**：一般

**改进建议**：明确 allergyHistory 和 comorbidities 的数据来源语义。推荐方案：前端仅传 patientId，后端在 Service 层从健康档案实体自动提取并拼接 allergyHistory + comorbidities（与需求文档 §3.1.6 映射约定一致）。在 §3.2 和 §3.4 补充数据来源说明。

---

### 问题 10

**问题描述**：设计文本 §3.4 /assist 主端点流程中"本地即时校验"包含过敏冲突检查（allergyDetails 优先 / allergyHistory 回退 + prescriptionDraft.drugs 交叉比对），与处方审核 §3.2 AllergyCheckRule 存在逻辑重叠——两者均对处方药品与患者过敏信息做交叉比对。设计文本未说明二者的关系：是冗余双重校验还是各有侧重？从需求文档 §3.4.10 看，辅助开方 allergy_warnings 的 severity 含 HIGH 级别，需明确 HIGH 级别告警在被处方审核消费时是否转换为 BLOCK 审核结果。

**所在位置**：§3.4 PrescriptionAssistService "本地即时校验"；§3.2 AllergyCheckRule

**严重程度**：一般

**改进建议**：在 §3.4 或 §7 补充"辅助开方过敏告警与处方审核过敏检查的关系"说明：明确辅助开方的 allergyWarnings 为即时提示性质（面向医生编辑期间的实时反馈），处方审核的 AllergyCheckRule 为提交时的正式审核判定，二者独立执行不互斥；辅助开方 allergyWarnings.severity=HIGH 不直接等价于处方审核 AuditRiskLevel=BLOCK。

---

### 问题 11

**问题描述**：设计文本 §3.1 TriageRecord 写入时机为"返回响应前同步写入"，finalDepartmentId 在患者挂号后通过"挂号模块发布的事件（RegistrationEvent）由事件监听器补充写入"。但 RegistrationEvent 作为跨模块事件，其事件契约（字段、发布端、消费端）未被定义。consultation 模块不依赖 registration 模块（§2.2），事件传播依赖 application 模块聚合后的 Spring ApplicationEvent 机制，设计文本需补充此跨模块事件的契约定义。

**所在位置**：§3.1 TriageService "TriageRecord 写入时机"；§4.1 持久化说明

**严重程度**：一般

**改进建议**：(a) 定义 RegistrationEvent 事件契约（字段至少含 registrationId / patientId / departmentId / doctorId / eventTime），事件类定义在 common-module-api 中；(b) 注册事件发布端为 registration 模块，消费端为 consultation 模块，事件在 application 模块聚合后跨模块传播；(c) 在 §2.2 或 §6 补充跨模块事件传递机制说明。

---

### 问题 12

**问题描述**：需求文档 §6.3 降级总表明确 3.4.1 智能分诊的降级路径为"回退到按科室选择医生模式（患者端 3.1.3.1 手动选择科室与医生）"——即前端需切换到手动选择科室 UI。设计文本 §3.1 降级链为 AI→规则引擎→兜底科室，TriageResponse 仅返回 degraded=true 标记。二者不矛盾但设计文本未说明前端收到 degraded=true 后应如何调整 UI——是仍渲染规则/兜底推荐结果，还是切换到纯手动选择模式。此降级目标语义的模糊性影响前端实现。

**所在位置**：§3.1 TriageService 降级链；§1.3 TriageResponse.degraded 字段

**严重程度**：轻微

**改进建议**：在 §4.1 降级判定后补充前端行为说明——degraded=true 时前端仍渲染推荐科室列表（规则匹配/兜底结果），同时显示降级提示文案并提供"手动选择科室"入口。

---

### 问题 13

**问题描述**：设计文本声称"规避后续迁移至 Phase 5 AI 进阶底座的重构成本"但未充分论证此约束的实际实现方式。设计文本中三个新模块均直接依赖 ai-api 并通过 AiService 接口调用 AI，Phase 5 OOD 设计（已参考）显示 AI 进阶底座在 AiService 接口不变的前提下替换 MockAiService 为 FallbackAiService 装饰器 + AiOrchestrator 管线。因此"接口不变"是迁移的关键前提——本设计直接落地在底座上并通过 AiService 抽象确实规避了迁移成本（业务模块代码无须修改），但此结论需要在设计文本中显式论证，而非仅声明性给出。

**所在位置**：§1.1 设计目标"底座直接落地"

**严重程度**：轻微

**改进建议**：在 §1.1 或 §7 设计决策中增加"底座落地与 Phase 5 迁移兼容性"设计决策条目，说明：业务模块仅依赖 ai-api 的 AiService 接口（编译期依赖），Phase 5 迁移时仅需替换 ai-impl 内的 AiService 实现类（MockAiService → FallbackAiService 装饰器），业务模块代码无须修改。Converter 类迁移影响范围：仅映射逻辑不变，ai-api 层 DTO 字段在 Phase 5 可能扩展但向后兼容。

---

### 问题 14

**问题描述**：设计文本 §5.1 错误码表中"开方辅助"前缀为 `RX_ASSIST_`，但同时存在 `RX_ASSIST_DOSE_STANDARD_NOT_FOUND`、`RX_ASSIST_AI_TIMEOUT` 两类错误码——前者不含 `_AI_` 中段，后者含 `_AI_` 中段。需求文档 §3.4 引言层约定"所有错误码必须保留 `_AI_` 中段以与非 AI 错误码区分"。设计文本未明确非 AI 业务错误码（如剂量标准不存在、单位不匹配）是否应遵循此命名约定或有无例外规则。

**所在位置**：§5.1 错误码表

**严重程度**：轻微

**改进建议**：在 §5.1 增加命名规则说明：AI 相关错误码遵循 `<前缀>_AI_<类型>` 命名约定（如 RX_ASSIST_AI_TIMEOUT），非 AI 业务逻辑错误码使用 `<前缀>_<类型>` 命名（如 RX_ASSIST_DOSE_STANDARD_NOT_FOUND），二者的区分规则和适用范围。

---

### 问题 15

**问题描述**：以下边界场景在设计中缺乏明确处理定义，从"可直接指导编码实现"视角应补充：(1) DialogueSession 超过 TTL 30 分钟后恰好有请求到达的竞态（请求到达瞬间 ScheduledExecutorService 清理该 session）——设计文本 §6.1 仅描述"每 5 分钟扫描清理超时 30 分钟的 session"但未说明清理与并发访问的竞态处理；(2) 多轮分诊场景下规则版本快照对应的规则集因管理员操作被删除/禁用后的行为——DialogueSession 持有 ruleVersion + ruleSetId 快照，但 TriageRuleEngine.match() 使用此快照查询时可能查不到对应规则集。

**所在位置**：§6.1 对话会话并发管理；§3.1 DialogueSession ruleVersion/ruleSetId 快照机制

**严重程度**：一般

**改进建议**：(1) 补充 TTL 清理的竞态处理说明——ScheduledExecutorService 清理与请求访问之间需使用 ConcurrentHashMap.remove() 的原子性保证，若 session 已被清理但请求到达，DialogueSessionManager.findOrCreate() 返回 TRIAGE_SESSION_EXPIRED 错误（与当前设计一致，但需明确此竞态路径被覆盖）。(2) 补充规则快照失效处理——当 TriageRuleEngine.match() 使用快照版本查询无结果时，降级使用当前最新版本规则集重新匹配（并在 TriageResponse 中标记），避免因规则管理操作导致分诊完全失败。

---

### 问题 16

**问题描述**：设计文本 §3.4 PrescriptionAssistRequest 和 §3.2 AuditRequest 中 patientInfo 包含 allergyDetails（List\<AllergyDetail\>），但 allergyDetails 与 patient 模块已有的健康档案（需求文档 5.1 健康档案实体）的关系不明确。需求文档 §3.1.6 明确 allergy_details 为"可选扩展容器"且"默认缺省，业务方确认纳入后再由后端拼接传递"，即 allergyDetails 应由后端从健康档案实体生成而非前端传入。设计文本允许 allergyDetails 由前端传入（AuditRequest/PrescriptionAssistRequest 作为前端请求 DTO），与 §3.1.6 的后端拼接语义不一致。

**所在位置**：§3.2 AuditRequest.patientInfo；§3.4 PrescriptionAssistRequest.patientInfo；§10.2 / §10.4 ai-api 层 DTO

**严重程度**：一般

**改进建议**：明确 allergyDetails 的数据流方向：(a) 推荐方案——前端请求 DTO 中 allergyDetails 为可选字段（用于离线/缓存场景的临时覆盖），后端 Service 层实现优先从健康档案实体自动生成 allergyDetails，前端传入值仅作为 fallback 或临时覆盖；(b) 在 §3.2 和 §3.4 补充数据来源说明，标注"健康档案为 single source of truth"。与问题 9（allergyHistory 来源）可合并处理。

---

## 整体评价

本轮审查在 v1 基础上，针对质询意见逐条核实后修订。核心变更：问题2（ chiefComplaint/additionalResponses 互斥语义）经核实需求文档 §3.4.1 原文"二选一使用，不同时提供"的互斥约束，确认设计文档互斥语义与需求文档一致，v1 标注的"严重——与需求文档矛盾"不成立，故撤回该问题。问题3（MatchedRule 字段缺失）重新定位：需求文档 §3.4.1 matched_rules 子结构确无子字段定义（需求侧缺口），v1 错误引用了不存在的 confidence/reason 子字段，现调整为"需求侧缺口下设计需主动定义子字段并论证选择"的一般问题。问题4（missingFields 缺失）的 v1 改进建议确有冗余——missingFieldHints 已含 missingField 标识，两者并存为纯冗余，调整为"missingFieldHints 是 missing_fields 的超集替代，需在设计决策中明确"。

当前 3 项严重问题（合并症检查遗漏、AiResult partialData 歧义、TriageRecord 缺推荐医生快照）均属影响编码实现的核心问题——问题3 直接导致需求文档明确要求的最小检查项集不完整，问题4 导致编码路径二义性，问题5 导致需求文档 5.1 实体定义字段缺失。修复三者后，本文档可直接指导编码实现。

---

## 修订说明（v2）

| 质询意见 | 回应 |
|---------|------|
| 问题2（chiefComplaint/additionalResponses 互斥语义与需求文档矛盾）核心证据与需求文档原文不符——需求文档 §3.4.1 显式声明"二选一使用，不同时提供"，设计文档的互斥定义与此一致 | **接受，撤回该问题。** 核实需求文档 §3.4.1 输入契约原文确为"与单字段 chief_complaint 二选一使用，不同时提供"，且 §3.1.3.1 多轮组装约定"首次调用仅传主诉、后续追问仅传 additionalResponses + sessionId"与此互斥约束一致。v1 将"互斥"误读为"矛盾"，此问题不成立，从本版报告中移除 |
| 问题3（MatchedRule 字段缺失）审查报告错误声称需求文档定义了 confidence / reason 子字段——需求文档 §3.4.1 matched_rules 后无子字段列举 | **接受，重新定位。** 核实需求文档 §3.4.1 输出契约原文，matched_rules 后确以句号结束，无子结构字段列表。v1 引用的 confidence / reason 子字段来源不成立。重新定位为"需求侧子结构定义缺失，设计文档需主动定义并论证选择"（见本版问题2，原问题3→问题2） |
| 问题4（RecordGenerateResponse 缺少 missingFields）改进建议引入冗余字段，与"结构化升级版本"定位矛盾——missingFieldHints 已含 missingField 标识，两字段并存为纯冗余 | **接受，调整改进建议。** v1 建议"两字段并存"确为冗余——missingFieldHints 中每个 FieldMissingHint.missingField 即可还原 missingFields 列表。调整改进建议为：在 §7 设计决策中明确 missingFieldHints 是 missing_fields 的超集替代而非并存字段。此问题严重程度降低为轻微，不再列入问题清单 |
| 覆盖完备性维度未评估"底座直接落地约束"的需求响应充分度 | **部分接受，降级为轻微。** Phase 5 OOD 设计显示业务模块仅依赖 ai-api 的 AiService 接口，迁移时仅需替换 ai-impl 内实现，业务模块代码无须修改——"底座落地规避迁移成本"的约束确实成立，但设计文本缺少显式论证。补充为问题13（轻微），建议在设计决策中增加此论证条目 |
| 覆盖完备性维度遗漏边界场景：TTL 清理竞态、AuditRecord 并发更新、规则快照版本失效 | **部分接受。** (1) DialogueSession TTL 清理竞态——ConcurrentHashMap.remove() 的原子性已隐含覆盖（findOrCreate 中 session 不存在时返回 EXPIRED 错误），但设计文本未显式说明此路径，补充到问题15。(2) AuditRecord 并发更新（多浏览器标签页）——需求文档和设计文本均无此场景的明确要求，Phase 2/3 单实例部署约束下此场景发生概率极低，不作为质量问题列出。(3) 规则快照版本失效——DialogueSession 持有的 ruleVersion/ruleSetId 对应的规则集可能被管理员删除/禁用，TriageRuleEngine.match() 使用快照版本查询无结果时的行为未定义，补充到问题15 |
