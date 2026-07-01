# Phase 2/3 OOD 设计方案 质量审查报告（v8 第3轮）

---

## 审查发现

### 问题 1

**问题描述**：DoctorFacade 接口在设计文本 §2.1 目录结构中放置于 `common-module-api/.../auth/DoctorFacade.java`，但实际代码中 `common-module-api/.../auth/` 包下仅有 UserFacade / CurrentUser / UserInfoResponse（已验证源码），该包的语义定位是"用户认证相关门面"。DoctorFacade 是跨模块医生排班/可用性查询门面，与"认证"包名不匹配。

**所在位置**：§2.1 目录结构 `common-module-api/.../auth/UserFacade.java, DoctorFacade.java`；§1.3 核心抽象一览 DoctorFacade 条目

**严重程度**：一般

**改进建议**：在 common-module-api 下为 DoctorFacade 创建独立子包（如 `commonmodule/doctor/DoctorFacade.java`），与 auth 包解耦。

---

### 问题 2

**问题描述**：需求文档 §3.4.1 输出契约中 `matched_rules` 字段后无子结构定义——原文仅写"`matched_rules`。"，句号结束，未列出每项应含的子字段。这是需求侧缺口——设计文档不能被动"对齐"一个不存在的子结构定义，而需主动定义 matched_rules 的子字段。当前设计文本 §1.3 MatchedRule DTO 定义为 ruleId / ruleName / score，但未在 §7 设计决策中说明这是设计侧的主动选择（而非被动的需求对齐），也未论证子字段选择理由（为何含 score 而不含 confidence/reason 等）。

**所在位置**：§1.3 核心抽象一览 MatchedRule DTO；§10.1 ai-api 层 MatchedRuleItem；§7 设计决策表（缺少 matched_rules 子字段设计决策条目）

**严重程度**：一般

**改进建议**：在 §7 设计决策中增加 matched_rules 子字段的设计决策条目，说明：(a) 需求文档 §3.4.1 matched_rules 子结构未定义（需求侧缺口）；(b) 设计侧主动定义 ruleId / ruleName / score 三个子字段，其中 score 统一承载匹配评分；(c) 若后续需求补充 confidence / reason 子字段，可通过 DTO 扩展兼容。

---

### 问题 3

**问题描述**：需求文档 §3.4.2 规定处方审核本地规则校验最小检查项集 #2 为"药品禁忌检查：检查处方药品是否与患者的过敏史冲突；是否与患者的合并症冲突"——合并症检查（即"药品是否与患者合并症如肾功能不全、肝功能不全等禁忌冲突"）是检查项 #2 的明确子要求。设计文本 §3.2 AllergyCheckRule 仅实现过敏史冲突检查，合并症-药品禁忌检查无对应规则实现，也无独立规则类覆盖。此为需求响应的明显遗漏。

**所在位置**：§3.2 LocalRuleEngine 实现范围表 + AllergyCheckRule 描述

**严重程度**：严重

**改进建议**：(a) 新增 ContraindicationCheckRule（或扩展 AllergyCheckRule 为 AllergyAndContraindicationCheckRule），补充合并症-药品禁忌检查子项：遍历处方药品列表，对每个药品查询其禁忌症列表，与 patientInfo.comorbidities 做交集比对。命中时产出 WARN 或 BLOCK 级别 LocalRuleResult（severity 判定逻辑可参考 AllergyCheckRule 的分级策略）。(b) 需同步更新 §3.2 实现范围表、§2.1 目录结构和 §7 设计决策，补充禁忌症数据来源实体（如新增 DrugContraindicationMapping JPA @Entity 或复用现有 DrugAllergyMapping 的扩展结构）。

---

### 问题 4

**问题描述**：AiResult 源码（已验证 `AiResult.java`）仅含 success / data / errorCode / degraded / fallbackReason 五个字段，`failure()` 工厂方法将 data 设为 null，`degraded()` 工厂方法将 data 也设为 null。设计文本 §3.3 声明"AI 超时时 AiResult.data 携带部分生成结果（AiResult 新增 partialData 字段或使用现有 data 字段承载部分结果）"，此描述具有歧义——既提到"新增 partialData 字段"又提到"使用现有 data 字段"，未做明确选择。更关键的是，需求文档 §3.4.3（L871-883）明确超时场景需同时携带 errorCode（MR_GEN_AI_TIMEOUT）和部分数据（partial_content），而当前 AiResult 的 failure() 和 degraded() 工厂方法均将 data 设为 null，无法同时传递错误码和部分数据。设计文本 §7 仅建议为 degraded() 增加重载，遗漏了 failure() 路径——超时场景是 failure+errorCode+partialData 的组合（不是 degraded+fallbackReason+partialData），仅覆盖 degraded() 重载无法解决编码路径歧义。

**所在位置**：§3.3 MedicalRecordService "非流式超时降级路径"；§7 设计决策 "AiResult 超时降级模式"

**严重程度**：严重

**改进建议**：明确选择使用现有 AiResult.data 字段承载部分结果，并删除"新增 partialData"歧义描述。需覆盖两条路径：(a) 超时场景（failure+errorCode+partialData）：AiResult.failure() 新增重载 `failure(String errorCode, T partialData)` 或由 AI 实现直接构造 `AiResult(success=false, partialData, errorCode, degraded=false, fallbackReason=null)`；(b) 降级场景（success=false + degraded=true + data 含部分结果 + fallbackReason）：AiResult.degraded() 新增重载 `degraded(String fallbackReason, T partialData)`。在 §3.3 和 §7 中明确此选择并删除"新增 partialData 字段"描述。

---

### 问题 5

**问题描述**：需求文档 §5.1（L1287）定义"分诊记录"实体核心字段为"主诉、推荐科室、推荐医生、患者标识、时间戳、是否降级等"。设计文本 §3.1 TriageRecord 实体包含 aiRecommendedDepartments（JSON TEXT）和 ruleMatchedDepartments（JSON TEXT），但缺少"推荐医生"快照字段（recommendedDoctors）。推荐医生列表是 TriageResponse 的核心输出之一，也是需求文档 5.1 分诊记录实体的明确字段。TriageRecord 未持久化推荐医生数据将导致分诊结果的可观测性和事后追溯不完整。

**所在位置**：§3.1 TriageRecord 实体字段列表

**严重程度**：严重

**改进建议**：在 TriageRecord 中增加 recommendedDoctors（JSON TEXT）字段，存储 TriageResponse.doctors 列表快照（含 doctorId / doctorName / departmentId / availableSlotCount / score）。

---

### 问题 6

**问题描述**：设计文本 §1.3 / §3.2 AuditAlert DTO 列出 alertCode / alertMessage / severity 三个字段名，但未定义 severity 字段的类型和值域。编码实现时 severity 应采用 AuditRiskLevel（PASS/WARN/BLOCK）还是独立枚举（如 AlertSeverity: INFO / WARNING / CRITICAL）不明确。从需求文档看，alerts.severity 是单条提示的严重程度，与 AuditRiskLevel（处方整体风险等级）是不同维度的概念，不应复用同一枚举。

**所在位置**：§1.3 AuditAlert DTO；§3.2 AuditResponse；§10.2 ai-api 层 AlertItem

**严重程度**：一般

**改进建议**：在 §1.3 / §3.2 AuditAlert 定义中补充 severity 字段的类型和值域。推荐方案：severity 类型为 String 或独立枚举（如 AlertSeverity: INFO / WARNING / CRITICAL），与 AuditRiskLevel 是不同维度。需同步更新 §10.2 ai-api 层 AlertItem.severity 定义。

---

### 问题 7

**问题描述**：设计文本 §3.4 DosageThresholdService 描述"四级均未命中，降级返回 WARN 级 DosageAlert 并携带 RX_ASSIST_DOSE_STANDARD_NOT_FOUND 错误码"，但 DosageAlert 类定义仅含 alertLevel / alertMessage / drugCode / 当前剂量 / 建议值，无承载错误码的字段。RX_ASSIST_DOSE_STANDARD_NOT_FOUND 错误码的传递路径不明确——无法通过 DosageAlert 本身传递，DosageCheckResponse 也只有 alerts + taskId 两个字段。

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

**问题描述**：设计文本 §3.2 AuditRequest.patientInfo 和 §3.4 PrescriptionAssistRequest.patientInfo 均包含 allergyHistory（string）和 allergyDetails（List\<AllergyDetail\>），但二者的数据来源语义不一致且与需求文档 §3.1.6 存在偏差。需求文档 §3.1.6 规定三层行为：(1) allergy_history 保持 string 由后端拼接；(2) allergy_details 为可选扩展容器，"默认缺省，业务方确认纳入后再由后端拼接传递"；(3) 前端在健康档案编辑界面填写 reaction_type/severity 时，将结构化数据"存入 allergy_details 扩展容器"。v2 审查报告的问题9（"前端仅传 patientId"）和问题16（"allergyDetails 作为前端请求 DTO 可选字段"）给出了相互矛盾的改进建议——问题9建议前端不传 patientInfo，问题16建议 allergyDetails 作为前端请求 DTO 字段保留。二者无法同时执行。

**所在位置**：§3.2 AuditRequest.patientInfo；§3.4 PrescriptionAssistRequest.patientInfo；§10.2 / §10.4 ai-api 层 DTO

**严重程度**：一般

**改进建议**：统一 allergyHistory 和 allergyDetails 的数据来源语义：(a) allergyHistory 由后端从健康档案实体拼接（与 §3.1.6 第1层一致）；(b) allergyDetails 按 §3.1.6 过渡方案第3层——允许前端存入，但后端 Service 层优先从健康档案实体自动提取 allergyDetails 为 single source of truth，前端传入值仅作为 fallback/离线场景覆盖；(c) 在 §3.2 和 §3.4 补充来源优先级说明。本问题合并 v2 的问题9和问题16。

---

### 问题 10

**问题描述**：设计文本 §3.4 /assist 主端点流程中"本地即时校验"包含过敏冲突检查（allergyDetails 优先 / allergyHistory 回退 + prescriptionDraft.drugs 交叉比对），与处方审核 §3.2 AllergyCheckRule 存在逻辑重叠——两者均对处方药品与患者过敏信息做交叉比对。设计文本未说明二者的关系：是冗余双重校验还是各有侧重？辅助开方 allergy_warnings.severity 含 HIGH 级别（需求文档 §3.4.10），与处方审核 AuditRiskLevel.BLOCK 的映射关系未定义。

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

**问题描述**：需求文档 §6.3（L1654）降级总表明确 3.4.1 智能分诊的降级路径为"回退到按科室选择医生模式（患者端 3.1.3.1 手动选择科室与医生）"——即前端需切换到手动选择科室 UI。设计文本 TriageResponse 仅返回 degraded=true 标记，未说明前端收到 degraded=true 后应如何调整 UI——是仍渲染规则/兜底推荐结果，还是切换到纯手动选择模式。此降级目标语义的模糊性影响前端实现。

**所在位置**：§3.1 TriageService 降级链；§1.3 TriageResponse.degraded 字段

**严重程度**：轻微

**改进建议**：在 §4.1 降级判定后补充前端行为说明——degraded=true 时前端仍渲染推荐科室列表（规则匹配/兜底结果），同时显示降级提示文案并提供"手动选择科室"入口。

---

### 问题 13

**问题描述**：设计文本声称"规避后续迁移至 Phase 5 AI 进阶底座的重构成本"但未充分论证此约束的实际实现方式。Phase 5 OOD 设计显示 AI 进阶底座在 AiService 接口不变的前提下替换 MockAiService 为 FallbackAiService 装饰器 + AiOrchestrator 管线——"接口不变"是迁移的关键前提。本设计直接落地在底座上并通过 AiService 抽象确实规避了迁移成本，但此结论需要在设计文本中显式论证。

**所在位置**：§1.1 设计目标"底座直接落地"

**严重程度**：轻微

**改进建议**：在 §1.1 或 §7 设计决策中增加"底座落地与 Phase 5 迁移兼容性"设计决策条目，说明：业务模块仅依赖 ai-api 的 AiService 接口（编译期依赖），Phase 5 迁移时仅需替换 ai-impl 内的 AiService 实现类，业务模块代码无须修改。

---

### 问题 14

**问题描述**：设计文本 §5.1 错误码表中"分诊"类别仅有 TRIAGE_SESSION_EXPIRED、TRIAGE_AI_TIMEOUT、TRIAGE_AI_UNAVAILABLE、TRIAGE_AI_INPUT_INVALID 四项，但需求文档 §3.4.1（L828）明确定义的错误码为 TRIAGE_AI_TIMEOUT / TRIAGE_AI_UNAVAILABLE / TRIAGE_AI_INPUT_INVALID 三项。TRIAGE_SESSION_EXPIRED 是设计侧新增的非 AI 错误码，但其不含 `_AI_` 中段。按需求文档 §3.4 引言（L818）的命名约定，`_AI_` 中段限定范围为"3.4.x 所有 AI 能力的错误码"，非 AI 业务逻辑错误码不需要也不应携带 `_AI_` 中段。此问题本身不是命名违规，但设计文本 §5.1 未明确区分 AI 能力错误码与本地业务错误码的命名规则——同表中混合了含 `_AI_` 中段和不含 `_AI_` 中段的错误码（如分诊类别：TRIAGE_AI_TIMEOUT vs TRIAGE_SESSION_EXPIRED），编码实现者无法从表中判断哪些错误码应遵循 AI 命名约定。

**所在位置**：§5.1 错误码表

**严重程度**：一般

**改进建议**：在 §5.1 增加命名规则说明：AI 相关错误码遵循 `<前缀>_AI_<类型>` 命名约定（如 TRIAGE_AI_TIMEOUT），非 AI 业务逻辑错误码使用 `<前缀>_<类型>` 命名（如 TRIAGE_SESSION_EXPIRED、RX_ASSIST_DOSE_STANDARD_NOT_FOUND），二者的区分规则和适用范围需在表中以注释或分类方式标注。

---

### 问题 15

**问题描述**：以下边界场景在设计中缺乏明确处理定义，从"可直接指导编码实现"视角应补充：(1) DialogueSession 超过 TTL 30 分钟后恰好有请求到达的竞态（请求到达瞬间 ScheduledExecutorService 清理该 session）——设计文本 §6.1 仅描述"每 5 分钟扫描清理超时 30 分钟的 session"但未说明清理与并发访问的竞态处理；(2) 多轮分诊场景下规则版本快照对应的规则集因管理员操作被删除/禁用后的行为——DialogueSession 持有 ruleVersion + ruleSetId 快照，但 TriageRuleEngine.match() 使用此快照查询时可能查不到对应规则集。

**所在位置**：§6.1 对话会话并发管理；§3.1 DialogueSession ruleVersion/ruleSetId 快照机制

**严重程度**：一般

**改进建议**：(1) 补充 TTL 清理的竞态处理说明——ConcurrentHashMap.remove() 的原子性保证下若 session 已被清理，DialogueSessionManager.findOrCreate() 返回 TRIAGE_SESSION_EXPIRED 错误（与当前设计一致，但需明确此竞态路径被覆盖）。(2) 补充规则快照失效处理——当 TriageRuleEngine.match() 使用快照版本查询无结果时，降级使用当前最新版本规则集重新匹配（并在 TriageResponse 中标记），避免因规则管理操作导致分诊完全失败。

---

### 问题 16

**问题描述**：设计文本 §5.1 错误码表中"开方辅助"类别列有 RX_ASSIST_DOSE_STANDARD_NOT_FOUND 和 RX_ASSIST_AI_TIMEOUT / RX_ASSIST_AI_UNAVAILABLE / RX_ASSIST_AI_NO_RECOMMENDATION，但需求文档 §3.4.10（L994）明确定义的 RX_ASSIST_AI_NO_RECOMMENDATION 虽在 §3.4 和 §4.4 正文中已描述其行为，却未出现在 §5.1 的错误码表中。同样，需求文档 §3.4.2（L842）定义的 RX_AUDIT_AI_INPUT_INVALID 和 §3.4.3（L865）定义的 MR_GEN_AI_INPUT_INVALID / MR_GEN_AI_OUTPUT_INCOMPLETE 也未在 §5.1 错误码表中列出。这些错误码是需求文档明确定义的输出契约字段，遗漏影响编码实现的错误码定义完整性。

**所在位置**：§5.1 错误码表

**严重程度**：一般

**改进建议**：§5.1 错误码表需补齐需求文档 §3.4.x 明确定义的全部 AI 能力错误码，至少包括：RX_ASSIST_AI_NO_RECOMMENDATION、RX_AUDIT_AI_INPUT_INVALID、MR_GEN_AI_INPUT_INVALID、MR_GEN_AI_OUTPUT_INCOMPLETE。

---

## 整体评价

当前 3 项严重问题（合并症检查遗漏、AiResult 超时路径歧义、TriageRecord 缺推荐医生快照）均属影响编码实现的核心问题——问题3 直接导致需求文档明确要求的最小检查项集不完整，问题4 导致编码路径二义性（且 v2 改进建议遗漏了 failure() 路径），问题5 导致需求文档 5.1 实体定义字段缺失。修复三者后，本文档可直接指导编码实现。

---

## 修订说明（v2）

| 质询意见 | 回应 |
|---------|------|
| 问题2（chiefComplaint/additionalResponses 互斥语义与需求文档矛盾）核心证据与需求文档原文不符——需求文档 §3.4.1 显式声明"二选一使用，不同时提供"，设计文档的互斥定义与此一致 | **接受，撤回该问题。** 核实需求文档 §3.4.1 输入契约原文确为"二选一使用，不同时提供"，v1 将"互斥"误读为"矛盾"，此问题不成立 |
| 问题3（MatchedRule 字段缺失）审查报告错误声称需求文档定义了 confidence / reason 子字段——需求文档 §3.4.1 matched_rules 后无子字段列举 | **接受，重新定位。** 确认需求文档 §3.4.1 matched_rules 后确无子字段列举。重新定位为"需求侧子结构定义缺失，设计文档需主动定义并论证选择" |
| 问题4（RecordGenerateResponse 缺少 missingFields）改进建议引入冗余字段——missingFieldHints 已含 missingField 标识，两字段并存为纯冗余 | **接受，不再列出。** missingFieldHints 是 missing_fields 的超集替代而非并存字段 |

## 修订说明（v3）

| 质询意见 | 回应 |
|---------|------|
| 问题14（v2）将 RX_ASSIST_DOSE_STANDARD_NOT_FOUND 不含 _AI_ 中段判定为"违反需求文档 §3.4 引言命名约定"，但需求文档 §3.4 引言（L814-817）的命名约定限定范围为"3.4.x 所有 AI 能力的错误码"，错误类型枚举均为 AI 调用链路上的错误类型，不包含"本地业务数据缺失"类别。将非 AI 错误码强行套用 AI 命名约定属于过度泛化 | **接受，重新定位。** 核实需求文档 L814-818，`_AI_` 中段约定确限定于"3.4.x 所有 AI 能力的错误码"，RX_ASSIST_DOSE_STANDARD_NOT_FOUND 等非 AI 业务错误码不受此约束。v2 问题14 的核心判定依据有误——错将 AI 命名约定覆盖到本地业务错误码。重新定位为问题14（本版）：§5.1 错误码表需明确 AI 能力错误码与本地业务错误码的分类和命名区分规则，避免编码实现者混淆 |
| 问题9和问题16（v2）对 allergyHistory/allergyDetails 的数据来源分析存在内部逻辑不一致——问题9建议"前端仅传 patientId"与问题16建议"allergyDetails 作为前端请求 DTO 可选字段"相互矛盾，无法同时执行 | **接受，合并统一。** 需求文档 §3.1.6（L396-398）原文规定三层行为：(1) allergy_history 由后端拼接；(2) allergy_details 默认缺省，"业务方确认纳入后再由后端拼接传递"；(3) 前端在健康档案编辑界面填写 reaction_type/severity 时，将结构化数据"存入 allergy_details 扩展容器"。v2 问题9建议"前端仅传 patientId"与第3层冲突，问题16建议"allergyDetails 作为前端请求 DTO 可选字段"与问题9矛盾。合并为本版问题9：allergyHistory 由后端拼接、allergyDetails 按 §3.1.6 双通道语义（后端优先从健康档案提取，前端存入作为补充），Service 层定义来源优先级规则 |
| 问题4（v2）仅覆盖 AiResult.degraded() 工厂方法的改进建议，但遗漏了 AiResult.failure() 路径——超时场景需同时传递 errorCode 和 partialData，而 failure() 工厂方法将 data 设为 null。需求文档 §3.4.3（L883）明确 MR_GEN_AI_TIMEOUT 适用于所有超时场景并保留部分内容，即超时返回是 failure+errorCode+partialData 组合，不是 degraded+fallbackReason+partialData | **接受，补充 failure() 路径。** 核实需求文档 L871-883，非流式超时要求 error_code=MR_GEN_AI_TIMEOUT + partial_content 含部分结果，这是 failure 语义（success=false, errorCode 非空）而非 degraded 语义（degraded=true, fallbackReason 非空）。v2 仅建议 degraded() 重载遗漏了 failure() 路径，本版问题4改进建议补充：AiResult.failure() 也需新增重载 `failure(String errorCode, T partialData)` 或明确超时场景不使用现有工厂方法而直接构造 AiResult 对象 |
