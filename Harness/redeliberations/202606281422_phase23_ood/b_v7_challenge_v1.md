# 质量质询报告（v7）

## 质询结果

LOCATED

## 逐维度审查

### 1. 证据充分性

**[通过]** 问题 1（ai-api 层 DTO 与业务层 DTO 契约缺口）：诊断报告声称 ai-api 层 TriageResponse 仅有 recommendedDepartments（且每项仅 departmentName）和 reason 两个字段，PrescriptionCheckRequest/Response、PrescriptionAssistRequest/Response、MedicalRecordGenRequest/Response 为空壳类。经代码核实，ai-api 层当前 TriageResponse 确实仅有 `recommendedDepartments` 和 `reason`；RecommendedDepartment 仅有 `departmentName`；PrescriptionAssistRequest/Response、PrescriptionCheckRequest/Response、MedicalRecordGenRequest/Response 确为空壳类。证据充分，问题判定准确。

**[通过]** 问题 2（同名 DTO 职责未区分）：PrescriptionAssistRequest 在 ai-api 层和业务层确实同名（`PrescriptionAssistRequest.java`），且 ai-api 层为空壳类、业务层含 5 字段完整定义，两者归属于不同包但命名相同。诊断报告准确识别了此混淆风险。

**[通过]** 问题 3（allergy_details 扩展容器未纳入）：需求文档 §3.1.6 明确定义了 allergy_details 过渡方案（array of object，含 allergen/reaction_type/severity/occurred_at），并指出"3.4.2 / 3.4.4 / 3.4.9 / 3.4.10 / 3.4.13 中 allergy_history 字段保持 string 类型不变，同时预留可选扩展容器 allergy_details"。当前设计文档中 AuditRequest.patientInfo 和 PrescriptionAssistRequest.patientInfo 仅有 allergyHistory(string)，未提及 allergy_details。证据充分。

**[通过]** 问题 4（RX_ASSIST_AI_NO_RECOMMENDATION 消费场景未描述）：§5.1 错误码表确实列出了该错误码，需求文档 3.4.10 也明确定义此错误码及"无可推荐药品"场景，但设计文档中 /assist 主端点流程未覆盖 AI 返回无可推荐药品时的处理路径。证据充分。

**[通过]** 问题 5（TriageRecord 写入时机和触发方不明确）：设计文档 §4.1 提及"分诊完成后写入 TriageRecord"，但 TriageService 接口职责描述中未包含此步骤，TriageRecord.finalDepartmentId 的赋值时机不明确。证据充分。

**[通过]** 问题 6（多轮分诊上下文传递未定义）：AiService.triage() 签名使用 ai-api 层 TriageRequest（仅含 chiefComplaint），而多轮场景 TriageServiceImpl 需要将 DialogueSession 的完整对话上下文组装传入。设计文档 §3.1 仅描述"委托 AiService.triage(session context)"但未定义具体组装方式。证据充分，代码层面 TriageRequest 仅有 chiefComplaint 一个字段可佐证。

**[通过]** 问题 7（风险等级判定规则表边界歧义）：LocalRuleResult.severity 类型为"严重程度"但未明确指向 AuditRiskLevel 枚举；AllergyCheckRule 的描述未区分过敏严重程度。证据充分。

**[通过]** 问题 8（WARN 级强制提交前后端交互链路不完整）：AuditRecord 已定义 forceSubmitted/forceSubmitTime，但前端如何通知后端"医生选择强制提交"的 API 机制未定义。证据充分。

**[通过]** 问题 9（推荐医生数据来源未定义）：TriageResponse.doctors 字段定义完整（含 doctorId/doctorName/departmentId/availableSlotCount/score），但生成机制未定义，且跨模块查询医生排班数据未定义获取路径。证据充分。

**[通过]** 问题 10（病历生成非流式超时 partial_content 未定义）：需求文档 3.4.3 定义了非流式超时时需返回 partial_content，AiResult.failure() 将 data 设为 null 无法承载此语义，§4.3 降级路径仅覆盖"AI 完全不可用"未覆盖"超时部分返回"。证据充分。

**[通过]** 问题 11（WARN 级处方审核时序竞态）：AuditRecord 中保存 originalPrescription 字段描述为"原始处方、riskLevel、aiResult、auditIssues 等审核结果数据"，但未显式标注是否保存完整处方快照用于版本校验。证据充分。

**[通过]** 问题 12（MedicalRecord JSON 存储形式未定义）：MedicalRecord.contentJson 字段类型和 JPA 存储形式（JSON TEXT vs 独立列）未明确。证据充分。

**[通过]** 问题 13（配置变更审计日志跨模块事件事务边界未定义）：§9.2/§9.3 定义了事件驱动机制，但未使用 @TransactionalEventListener(phase=AFTER_COMMIT) 等事务一致性策略。证据充分。

**[通过]** 问题 14（session_id 必填/可选语义映射不完整）：需求文档 3.4.1 session_id 标注为"必填"，但设计文档首轮请求 sessionId 为空。虽然设计文档 §3.1 和 §7 有映射说明，但需求文档未为首轮请求提供可选性说明。证据充分。

**[通过]** 问题 15（DrugInteractionPair/DrugCompositionDict 持久化层定义缺失）：§2.1 目录中 DrugInteractionPair.java 和 DrugCompositionDict.java 在 rule/ 包下而非 entity/ 包下，缺少对应 Repository 定义。证据充分。

**[通过]** 问题 16（分诊规则引擎 TriageRule 数据模型未定义）：TriageRuleEngine 使用数据库规则存储，但规则的数据实体和 match() 方法签名未定义。证据充分。

**[通过]** 问题 17（处方提交端点设计边界未标注）：是否在本设计范围内或 design boundary 之外未显式标注，但 BLOCK 阻断和 WARN 留痕的两个行为契约均依赖此端点。证据充分。

**[通过]** 问题 18（ConcurrentHashMap 水平扩展问题）：设计文档 §7 提及"Phase 5 迁移数据库"但未说明多实例部署场景的中间方案或部署约束。证据充分。

### 2. 逻辑完整性

**[通过]** 18 个问题之间不存在逻辑矛盾。问题 1 和问题 2 有关联性（均涉及 ai-api 层 DTO 与业务层 DTO 的关系），但视角不同——问题 1 关注字段级契约缺口，问题 2 关注同名类混淆风险，二者互补不矛盾。

**[通过]** 问题 8（WARN 级前后端交互链路）与问题 11（时序竞态防护）和问题 17（处方提交端点边界）三者形成递进关系：问题 8 关注交互机制缺失，问题 11 关注竞态风险，问题 17 关注端点设计范围。三者分别从不同维度审视同一流程的不同侧面，逻辑一致。

**[通过]** 改进建议均与对应问题一致且可行。问题 1 和问题 2 的改进建议中"ai-api 层 DTO 扩展或复用策略"方向清晰，问题 9 的改进建议提供了 AI 生成与后端组装两种路径及跨模块门面接口方案，问题 13 建议使用 @TransactionalEventListener(phase=AFTER_COMMIT) 具体可行。

### 3. 覆盖完备性

**[通过]** 任务描述要求从三个角度诊断：需求响应充分度、事实错误或逻辑矛盾、深度和完整性。诊断报告覆盖充分——问题 3/4/6/9/14 直接响应"需求响应充分度"；问题 1/2/5/7/8/10/11/12/13/15/16/17/18 直接响应"深度和完整性"；各问题经代码或文档核实未发现事实错误。

**[通过]** OOD 设计落地视角评估覆盖充分：问题 1/2/6 直接影响编码实现可行性（开发者无法确定 AiService 调用的入参和出参结构）；问题 5/12/15/16 影响接口定义和下游消费者；问题 8/11/17 影响异常场景和边界条件覆盖。

**[问题-轻微]** 诊断报告未覆盖一个可能存在的质量问题：设计文档中 AuditRiskLevel.WARN 分支下"撤销审核"操作的行为定义不够完整——§4.2 提及"处方状态回退至'草稿'且不保存本次审核结果（但已产生的审核记录仍持久化保存）"，但此处"不保存本次审核结果"与"审核记录仍持久化保存"在语义上需要更精确的澄清——AuditRecord 中 isLatest 字段的处理逻辑未说明：撤销后该 AuditRecord 的 isLatest 是否回退为 false？若不回退，后续查询最新审核结果可能指向一个已被撤销的记录。此问题虽属于边界细节，但在实际落地中可能导致"查询处方的最新审核结果"返回已撤销的记录。

**[建议]** 在诊断报告中补充此边界场景的审查意见，或在后续迭代中由设计方澄清。

### 4. 报告必要性

**[通过]** 诊断报告中所有 18 个问题均与任务描述（需求响应充分度、深度和完整性、落地视角评估）直接相关，不存在无关紧要的细节问题。问题严重程度分级合理——与编码实现直接阻塞相关的标记为"严重"（问题 1/2/3/4/6/9），影响接口定义或下游消费者但存在可推断方向的标记为"一般"。

## 质询要点

无（LOCATED）。
