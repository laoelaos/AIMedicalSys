根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 严重问题

1. **AuditResponse.issues 与需求文档 3.4.2 alerts 字段命名与结构不一致**（§3.2 AuditResponse + AuditIssue 定义；§1.3 核心抽象一览）
   - 需求文档 3.4.2 输出契约定义风险提示字段名为 `alerts`（每项含 alert_code / alert_message / severity），设计文档 §3.2 AuditResponse 使用 `issues`（List\<AuditIssue\>，每项含 fieldName / issueDescription / ruleId / severity）。两者不仅是命名差异，字段结构也完全不同。
   - 改进建议：在 AuditResponse 中同时保留 `alerts`（对齐需求文档 3.4.2 契约，每项含 alertCode / alertMessage / severity）和 `issues`（设计自有结构），并说明二者关系；或将 AuditIssue 字段名对齐为 alertCode/alertMessage，并显式说明 issues 与 alerts 的映射关系。在 §3.2 或 §7 补充设计决策说明此差异的合理性。

2. **病历生成缺少流式输出生成模式的设计，与需求文档 3.4.3 契约不对齐**（§3.3 MedicalRecordController / MedicalRecordService；§4.3 病历生成场景；§5.5 AI 超时配置）
   - 需求文档 3.4.3 定义了流式输出模式（stream=true 时分片返回，含 chunk_index / is_final 错误处理等完整机制），设计文档仅描述了非流式路径，流式模式的首字响应 ≤2s、总时长 ≤30s、分片间隔 ≤5s 等阈值未涉及。
   - 改进建议：若 Phase 3 不实现流式，应在设计文档中显式标注"流式输出待后续迭代实现"，并在 §7 设计决策中说明理由；同时补充 RecordGenerateRequest 的 stream 字段（bool，可选，默认 false），并在 §5.5 补充流式超时配置项（流式首字 2s、总时长 30s、分片间隔 5s）。若同步实现流式，则需补充流式端点设计、流式超时处理、流式错误分片结构等完整契约。

3. **本地规则校验范围与需求文档 3.4.2 最小检查项集不匹配——仅实现 1/4**（§3.2 LocalRuleEngine；§4.2 处方审核降级路径）
   - 需求文档 3.4.2 明确规定本地规则校验最小检查项集含 4 项：①剂量范围检查、②药品禁忌检查、③重复用药检查、④儿童/老年人群特殊剂量检查。设计文档仅实现 DosageLimitRule（第 1 项）。
   - 改进建议：重新评估实现范围——药品禁忌检查（AllergyCheckRule，依赖 DrugAllergyMapping + 患者过敏史/合并症）和重复用药检查依赖药品成分字典数据而非 AI，具备本地实现条件，应纳入 Phase 3 最低交付范围；儿童/老年人群特殊剂量检查可借用 DosageThresholdService 的年龄/体重分级机制实现。若确有数据获取困难，应显式标注为降级安全缺口并在 §4.2 中增加风险提示，而非仅以"骨架预留"一笔带过。

### 一般问题

4. **DialogueCreateRequest 缺少 ruleSetId 字段，与需求文档 3.4.1 输入契约不对齐**（§3.1 DialogueCreateRequest 定义）
   - 需求文档 3.4.1 输入契约定义 `rule_set_id`（配置规则集标识，string，可选），设计文档已有 `ruleVersion`（对应 rule_version），但缺失 `ruleSetId`（对应 rule_set_id），两者语义不同。
   - 改进建议：在 DialogueCreateRequest 中增加 `ruleSetId`（可选字段，String 类型），并在 §3.1 TriageRuleEngine / §4.1 补充 ruleSetId 的消费语义。

5. **TriageResponse 缺少 matchedRules 字段，与需求文档 3.4.1 输出契约不对齐**（§3.1 TriageResponse 定义）
   - 需求文档 3.4.1 输出契约定义 `matched_rules` 字段，设计文档缺失。
   - 改进建议：在 TriageResponse 中增加 `matchedRules`（可选字段，List\<MatchedRule\>，每项含 ruleId / ruleName / score）。

6. **TriageResponse.departments 每项字段与需求文档 3.4.1 recommended_departments 结构不完整对齐**（§3.1 TriageResponse）
   - 需求文档 3.4.1 定义 recommended_departments 每项含 department_id / department_name / score，设计文档未定义 departments 每项的 DTO 结构。
   - 改进建议：新增 RecommendedDepartment DTO（含 departmentId、departmentName、score）。

7. **RecordGenerateRequest 缺少 stream 字段，与需求文档 3.4.3 输入契约不对齐**（§3.3 RecordGenerateRequest 定义）
   - 需求文档 3.4.3 输入契约定义 `stream`（bool，可选），设计文档缺失。
   - 改进建议：在 RecordGenerateRequest 中增加 `stream`（bool，可选，默认 false），若当前不实现流式则标注"Phase 3 仅支持非流式"。

8. **RecordGenerateResponse 的 fields 映射结构与需求文档 3.4.3 输出契约字段命名不一致**（§3.3 RecordGenerateResponse；§3.3 MedicalRecordField 枚举）
   - MedicalRecordField 枚举命名与需求文档存在差异（如 TREATMENT_ADVICE vs treatment_plan）；missing_fields（string 数组）与 missingFieldHints 的映射关系未说明。
   - 改进建议：补充 MedicalRecordField 枚举与需求文档字段名的映射说明；说明 missingFieldHints 是 missing_fields 的结构化升级版本。

9. **PrescriptionDraftContext 生命周期管理缺失——key 定义不清，清理时机不明确**（§3.4 PrescriptionDraftContext；§6.4 处方草稿上下文并发管理）
   - encounterId 与"处方编辑会话标识"概念混淆，一次就诊可有多个处方编辑会话。
   - 改进建议：明确 key 为 prescriptionId（处方编辑会话标识 = 处方草稿 ID）；定义创建时机（首次 check-dose 请求时）和清理时机（处方提交成功 / 处方取消 / TTL 过期）。

10. **分诊场景缺少 AI 连续失败 3 次的兜底提示机制**（§3.1 DialogueSessionManager；§4.1 智能分诊场景）
    - 需求文档 §3.1.3.1 定义："AI 调用失败次数达 3 次后，自动给出'建议直接联系线下接诊窗口'的兜底文案"。
    - 改进建议：在 DialogueSession 中增加 `aiFailCount`（int）字段，TriageServiceImpl 降级后检查 aiFailCount >= 3 时附加兜底提示。

11. **AuditRecord 缺少审核次序（auditSequence）和是否最新（isLatest）字段**（§3.2 AuditRecord 实体定义）
    - 需求文档 3.4.2 要求"同一处方的多次审核按审核次序分别保存一条记录，并通过'审核次序'与'是否最新'字段标识"。
    - 改进建议：在 AuditRecord 中增加 `auditSequence`（int，必填，同一 prescriptionOrderId 下递增）和 `isLatest`（boolean，必填，仅最新一条为 true）字段。

12. **DosageStandard 实体与需求文档 5.1 药品基础信息实体的关系未定义**（§2.2 DosageStandard；§8.1 初始化方案；§8.2 药品编码规范）
    - 改进建议：显式说明两者通过 drugCode 关联但为独立实体；明确 admin 模块"药品字典维护"操作的是药品基础信息实体，"DosageStandard 管理"是独立的剂量标准维护入口；明确 drugCode 与药品基础信息实体的主键关联关系。

### 轻微问题

13. **分诊请求中 chiefComplaint 字符数约束（5–500）未在设计文档中定义**（§3.1 DialogueCreateRequest）
    - 改进建议：在 DialogueCreateRequest.chiefComplaint 字段描述中补充"字符数 5–500"约束，对齐需求文档 3.4.1。

14. **多轮分诊模式下 chiefComplaint 与 additionalResponses 的互斥/组合语义未说明**（§3.1 DialogueCreateRequest；§4.1 智能分诊场景）
    - 需求文档 3.4.1 定义 additional_responses 与 chief_complaint"二选一使用，不同时提供"，设计文档未说明此语义。
    - 改进建议：补充首轮请求（chiefComplaint 必填，additionalResponses 不传）和后续多轮追问的语义说明，并在需求文档"二选一"语义与设计文档"可共存"语义间给出明确映射说明。

## 历史迭代回顾

### 已解决的问题（出现在历史反馈但当前反馈中不再提及的问题）

- **DialogueSession 不可变声明与可变追加操作的逻辑矛盾**（第1轮问题1）：已在 v2 改为可变 class
- **包E 异步 AI 建议缺少消费路径**（第1轮问题2）：已补充 GET /assist/suggestion/{taskId} 端点
- **多轮分诊中对话历史的维护责任与一致性不明确**（第1轮问题3）：已改为服务端 DialogueSession 为单一真相来源
- **DosageCheckRequest 缺少给药途径参数**（第1轮问题4）：已补充 routeOfAdministration
- **prescription 模块内 DosageStandard 实体的写权限归属未定义**（第1轮问题5）：已明确 admin 唯一写入者
- **分诊规则配置变更的生效机制未定义**（第1轮问题6）：已补充定时缓存刷新 + 事件驱动失效
- **科室模板配置的 CRUD 和默认兜底缺失**（第1轮问题7）：已补充 DEFAULT 兜底和 TemplateConfigManager
- **对话会话内存存储未覆盖服务重启场景**（第1轮问题8）：已补充 TRIAGE_SESSION_EXPIRED 错误码
- **新模块依赖声明未包含 common-module-api**（第1轮问题9）：已在 §1.2 补充
- **剂量标准数据初始化方案和编码规范缺失**（第1轮问题10）：已补充 §8 种子脚本和药品编码规范
- **BLOCK 风险等级缺少后端强制阻断执行机制**（第2轮问题1）：已补充 PrescriptionAuditEnforcer
- **AuditRecord 缺少处方级关联标识**（第2轮问题2）：已补充 prescriptionOrderId / doctorId / patientId
- **AiSuggestionResult ConcurrentHashMap 暂存重启丢失**（第2轮问题3）：已补充四分支模式 + TTL + NOT_FOUND 错误码
- **DosageStandard 年龄/体重分级剂量结构缺失**（第2轮问题4）：已补充年龄/体重范围字段和匹配优先级
- **病历生成降级策略不合理**（第2轮问题5）：已改为分层保护策略
- **AuditRecord 降级路径落库遗漏**（第3轮问题1）：已显式补充
- **分诊降级链路不完整**（第3轮问题2）：已改为线性三级降级链
- **TriageResponse DTO 字段结构未定义**（第3轮问题3）：已补充定义
- **剂量单位转换规则集未定义**（第3轮问题4）：已补充 DosageUnitGroup
- **MedicalRecord 实体字段未定义**（第3轮问题5）：已补充 MedicalRecordField 枚举
- **规则/模板配置变更缺少审计溯源**（第3轮问题6）：已补充 ConfigChangeLog
- **AiSuggestionResult 缺少 FAILED 状态**（第4轮问题1）：已补充 FAILED + 预创建→更新模式
- **TriageRequest DTO 与 DialogueCreateRequest 关系不明**（第4轮问题2）：已移除 TriageRequest
- **关键 DTO 存在于目录但文本无定义**（第4轮问题3）：已补充完整定义
- **DosageThresholdService 匹配策略未定义"记录不存在"行为**（第4轮问题4）：已补充第5级
- **DosageAlert 告警级别枚举未定义**（第4轮问题5）：已补充 INFO/WARN/CRITICAL
- **check-dose 缺少 frequency 字段**（第4轮问题6）：已补充
- **MedicalRecord 缺少就诊关联标识**（第4轮问题7）：已补充 visitId
- **PrescriptionAssistResponse 字段定义不完整**（第4轮问题8）：已修正
- **AuditRiskLevel 与需求文档术语映射缺失**（第4轮问题9）：已补充映射说明
- **AiSuggestionResult 并发竞争**（第5轮问题1）：已改为 compute() 原子更新
- **包E check-dose 与需求 3.4.10 脱节**（第5轮问题2）：已补充 /assist 主端点
- **TriageResponse 缺少 doctors/reason 字段**（第5轮问题3）：已补充
- **AuditRequest 字段不完整**（第5轮问题4）：已补充完整字段
- **RecordGenerateRequest.visitId 映射未说明**（第5轮问题5）：已映射 encounterId
- **WARN 级留痕数据结构未定义**（第5轮问题6）：已补充 forceSubmitted/forceSubmitTime
- **sessionId 生成策略未定义**（第5轮问题7）：已明确 UUID v4 后端生成
- **session_id 必填/可选语义矛盾**（第5轮问题8）：已说明首轮为空合理性
- **CRITICAL/BLOCK 联动调用链路未定义**（第5轮问题9）：已补充 PrescriptionDraftContext 联动机制
- **DrugInteractionPair/DrugAllergyMapping 缺失**（第5轮问题10）：已预留骨架
- **AI 无结果判定边界不清晰**（第5轮问题11）：已明确判定语义
- **AI 超时阈值未体现**（第5轮问题12）：已补充 §5.5 配置表
- **AuditResponse 缺少 interactions/suggestions**（第5轮问题13）：已补充
- **TriageRecord 实体字段缺失**（第5轮问题14）：已补充完整字段

### 持续存在的问题（在多轮反馈中反复出现的问题，需重点解决）

- **需求契约字段对齐问题**：本轮问题1、4-8 均为需求文档与设计文档字段级不对齐的延续——自第 5 轮以来持续暴露。第 6 轮已修复大部分不对齐项（如 doctors/reason/interactions/suggestions），但本轮仍发现 issues vs alerts 命名差异、ruleSetId 缺失、matchedRules 缺失、departments 每项结构缺失、stream 缺失、字段命名映射缺失等 6 处新不对齐。这属于对齐审查方法的系统性遗漏——每轮仅修复诊断报告指出的具体字段，未对齐需求文档做全量扫描。**建议本轮一次性完成需求文档 3.4.1/3.4.2/3.4.3/3.4.10 全字段对齐审查**，消除后续迭代反复暴露同类问题。
- **本地规则校验范围**：第 6 轮问题 8（本地规则仅实现 1/4）在第 5 轮已提出（第 5 轮问题 8 的 DrugInteractionRule/AllergyCheckRule 建议），但第 6 轮仅增加骨架预留标注，未实质纳入实现范围。本轮诊断再次标记为严重。**建议本轮对 AllergyCheckRule 和重复用药检查做明确的实现或风险标注决策**。

### 新发现的问题（本轮新识别的问题）

- 问题1（issues vs alerts 命名结构不一致）：第 5 轮审查了 AuditResponse 的 interactions/suggestions 缺失，但未深入比对 issues 与需求文档 alerts 的命名/结构差异
- 问题4（ruleSetId 缺失）：第 5 轮补充了 ruleVersion 但遗漏 ruleSetId
- 问题5（matchedRules 缺失）和问题6（departments 每项结构缺失）：TriageResponse 在第 5 轮已补充 doctors/reason，但 matchedRules 和 departments 内部结构被遗漏
- 问题7（stream 缺失）和问题8（字段命名映射）：病历生成流式模式在本轮首次识别
- 问题9（PrescriptionDraftContext key 定义）：第 5 轮补充了 CRITICAL→BLOCK 联动机制但 encounterId vs 处方编辑会话标识的概念混淆未被识别
- 问题10（AI 连续失败 3 次兜底）：本轮首次识别，此前未涉及
- 问题11（AuditRecord 审核次序/是否最新）：本轮首次识别
- 问题12（DosageStandard 与药品基础信息实体关系）：本轮首次识别
- 问题13（chiefComplaint 字符数约束）：本轮首次识别
- 问题14（chiefComplaint 与 additionalResponses 互斥/组合语义）：本轮首次识别

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281422_phase23_ood\a_v6_design_v1.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281422_phase23_ood\requirement.md
