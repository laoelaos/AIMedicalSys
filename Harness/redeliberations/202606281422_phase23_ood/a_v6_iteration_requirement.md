根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 严重问题

1. **AiSuggestionResult 不支持并发更新——预创建→更新模式在异步场景下存在数据竞争**（§3.4 AiSuggestionResult + §6.3 "包E 的异步 AI 建议"）
   - ConcurrentHashMap 保证 map 级别线程安全但不保证 value 对象状态一致性，多个线程可能同时读写 AiSuggestionResult 的 status/suggestion/failReason 字段
   - 改进建议：明确 AiSuggestionResult 的并发安全策略——(a) 内部字段使用 volatile/AtomicReference/CAS 操作保护状态更新，或使用 ConcurrentHashMap.compute() 原子更新整个 value 对象；(b) 同一 taskId 的状态更新保证幂等

2. **包E check-dose 请求与需求文档 3.4.10 AI 辅助开方输入契约完全脱节**（§3.4 PrescriptionAssistService / DosageThresholdService + §4.4 check-dose API + §6.3 异步 AI 建议）
   - check-dose 仅接收单药品剂量检查参数，而非需求文档定义的完整 AI 辅助开方能力（含 diagnosis/exam_results/patient_info/existing_prescription 输入，prescription_draft/dose_warnings/allergy_warnings 输出）；异步 AI 建议查询返回文本 suggestion 而非需求文档定义的结构化输出
   - 改进建议：(a) 定义 POST /api/prescription/assist 作为 3.4.10 主端点，接收完整输入并返回完整结构化输出；(b) 或显式标注当前设计仅覆盖"剂量阈值告警"子集（与路线图 Phase 3 一致），但在文档中显式标注范围限制和后续扩展计划，并在 AiService.prescriptionAssist() 调用契约中预留完整输入输出结构

3. **TriageResponse 与需求文档 3.4.1 输出契约不对齐——缺少推荐医生列表和推荐理由**（§3.1 TriageResponse + DialogueCreateRequest + §4.1 智能分诊场景）
   - TriageResponse 缺少 recommended_doctors（推荐医生列表）和 reason（推荐理由文本）字段；DialogueCreateRequest 缺少 ruleVersion/ruleSetId 和 additionalResponses（结构化追问回答数组）字段
   - 改进建议：(a) TriageResponse 增加 doctors 字段（List<RecommendedDoctor>，每项含 doctorId/doctorName/departmentId/availableSlotCount/score）和 reason 字段（推荐理由文本，必填）；(b) DialogueCreateRequest 增加 ruleVersion（可选）和 additionalResponses（可选，List<AdditionalResponse>，每项含 question/answer/answeredAt）

4. **AuditRequest 与需求文档 3.4.2 输入契约不对齐——缺少完整字段映射**（§3.2 AuditRequest + §4.2 处方审核场景）
   - patientInfo 未显式列出 allergy_history 和 comorbidities 字段结构；prescriptionItems 每项缺少 dose/frequency/duration/route 四个独立字段定义
   - 改进建议：patientInfo 显式列出 allergy_history（string）和 comorbidities（List<String>）；处方药品列表每项包含 drugId/drugName/dose/frequency/duration/route 六个字段

5. **RecordGenerateRequest 与需求文档 3.4.3 输入契约不对齐——缺少 encounter_id 映射说明**（§3.3 RecordGenerateRequest + §4.3 病历生成场景）
   - visitId 与需求文档 encounter_id 是否为同一概念未做映射说明；需求文档输入契约未包含 departmentId，设计中额外添加的 departmentId 引入理由和与 encounter_id 的优先级未说明
   - 改进建议：(a) 明确 visitId 与 encounter_id 的映射关系并显式标注；(b) 说明 departmentId 的引入理由：encounter_id 存在时由后端自动获取科室，departmentId 仅在 encounter_id 缺失时作为显式参数——或直接使用 encounter_id 关联获取，删除 departmentId 显式参数

6. **需求文档 3.4.1 定义 session_id 为必填，但设计文档允许首轮请求 sessionId 为空，构成契约必填/可选语义矛盾**（§3.1 DialogueCreateRequest + 需求文档 3.4.1 输入契约）
   - 质询报告指出诊断报告遗漏了此必填/可选语义矛盾。需求文档定义 session_id 为必填字段，而设计允许首轮为空，直接影响 API 契约定义
   - 改进建议：在设计文档中明确首轮请求时 sessionId 传空值的合理性（如需求文档的"必填"指多轮场景下必填，首轮由服务端生成后后续必填），或与需求方澄清首轮场景下 session_id 的可选性

### 一般问题

7. **WARN 级处方"强制提交并留痕"的留痕数据结构未定义**（§3.2 AuditRecord + §4.2 WARN 分支）
   - AuditRecord 缺少 forceSubmitted（Boolean）和 forceSubmitTime（LocalDateTime）字段，无法承载"强制提交留痕"需求
   - 改进建议：AuditRecord 增加 forceSubmitted 和 forceSubmitTime 字段，或显式定义其他承载实体

8. **sessionId 生成策略和格式未定义**（§3.1 DialogueSession / DialogueSessionManager + §4.1 智能分诊场景）
   - sessionId 的生成策略（UUID/雪花ID/自增序列）、格式规范（长度限制/字符集）及前端/后端生成职责均未定义
   - 改进建议：明确 sessionId 由后端 DialogueSessionManager 统一生成，采用 UUID v4 格式，首轮请求时 sessionId 为空

9. **DosageAlertLevel.CRITICAL 与 AuditRiskLevel.BLOCK 的联动触发机制调用链路未定义**（§3.4 DosageAlertLevel.CRITICAL 描述 + §2.2 包D-AI1 与包E 强耦合处理）
   - 仅停留在语义描述层面，实际调用链路、入参构造和触发时机（剂量检查 vs 处方提交时机矛盾）均未定义
   - 改进建议：CRITICAL 级别剂量告警不直接调用 PrescriptionAuditService，而是写入处方草稿上下文，提交时从草稿上下文读取 CRITICAL 标记作为 BLOCK 判定输入

10. **本地规则引擎 AllergyCheckRule 和 DrugInteractionRule 的数据来源未定义**（§2.1 目录结构 + §3.2 LocalRuleEngine）
    - DrugInteractionRule 和 AllergyCheckRule 需要的 DrugInteractionPair、DrugAllergyMapping 数据实体完全缺失
    - 改进建议：(a) 补充 DrugInteractionPair 和 DrugAllergyMapping 数据实体定义；(b) 或明确 Phase 2/3 仅实现 DosageLimitRule，其余标注为待实现项

11. **分诊降级链中 AI 无结果与规则无结果的判定边界不清晰**（§4.1 智能分诊场景 + §3.1 TriageService）
    - AiService.triage() 返回空列表时是否视为"无结果"而降级到规则引擎未定义
    - 改进建议：明确判定条件——AiResult.success=false 或 degraded=true 时判定 AI 不可用降级；success=true 但空列表视为有效结果跳过规则引擎，或明确空列表也继续降级。无论选择哪种语义，需显式定义

12. **处方审核 AI 超时阈值和病历生成 AI 超时阈值未在设计文本中体现**（§3.2 PrescriptionAuditService + §3.3 MedicalRecordService + §4.2/§4.3）
    - 需求文档 3.4.2 定义处方审核硬超时 ≤ 6 秒，3.4.3 定义病历生成硬超时 ≤ 12 秒，设计文本未映射此配置
    - 改进建议：在 §4.2/§4.3 补充超时配置说明，定义 ai.timeout.prescription-audit=6s 和 ai.timeout.medical-record-generate=12s

13. **AuditResponse 与需求 3.4.2 输出契约对齐缺失——缺少 interactions 和 suggestions 字段**（§3.2 AuditResponse + §4.2 处方审核场景）
    - 质询报告指出此遗漏。需求 3.4.2 输出契约包含 interactions（药物相互作用结果）和 suggestions（用药建议），AuditResponse 仅有 riskLevel + issues + fromFallback
    - 改进建议：AuditResponse 补充 interactions 和 suggestions 结构化输出字段，与需求 3.4.2 输出契约对齐

14. **TriageRecord 持久化字段定义缺失，无法满足需求 3.4.1"分诊结果需持久化以供统计与质量分析"的可观测性约束**（§2.1 TriageRecord + 需求文档 3.4.1 服务质量要求）
    - 质询报告指出此遗漏。设计文档声明了 TriageRecord 实体但未定义字段，无法支撑统计查询
    - 改进建议：补充 TriageRecord 字段定义，至少包含 AI 推荐科室快照、规则匹配科室、最终选择科室、置信度、降级标记等统计必需字段

### 轻微问题

15. **分诊规则引擎缓存刷新时间与对话会话 TTL 的交互影响未分析**（§3.1 TriageRuleEngine + DialogueSessionManager）
    - 规则在对话进行中被刷新可能导致前后分诊逻辑不一致
    - 改进建议：(a) 在 DialogueCreateRequest 中支持 ruleVersion 字段（需求文档 3.4.1 已定义），首轮对话时记录当前规则版本号，后续追问使用快照规则版本；(b) 或在设计文本中说明接受对话内规则可能不一致为已知限制

16. **DosageThresholdService 同优先级多条结果"取 dosageMax 最小者"策略可能导致误报**（§8.4 年龄/体重分级剂量支持）
    - 数据冗余或录入错误时，异常小的 dosageMax 会导致正常剂量被判定为严重超标
    - 改进建议：(a) 种子数据初始化时检测同优先级重复记录并报错；(b) 或在策略中增加数据质量校验前置步骤

## 历史迭代回顾

### 已解决的问题（出现在历史反馈但当前反馈中不再提及的问题）

- **迭代1-问题1**：DialogueSession 不可变声明与可变追加操作的逻辑矛盾 → v2 已改为可变 class + DialogueSessionManager 并发控制，当前反馈不再提及
- **迭代1-问题2**：包E 异步 AI 建议缺少消费路径 → v2-v5 已补充查询端点、四分支模式、FAILED 状态，当前反馈不再提及此缺失
- **迭代1-问题3**：多轮分诊中对话历史维护责任与一致性不明确 → v2 已明确服务端 DialogueSession 为单一真相来源，后续轮仅携带 sessionId
- **迭代1-问题4**：DosageCheckRequest 缺少给药途径参数 → v2 已补充 routeOfAdministration 字段
- **迭代1-问题5**：DosageStandard 写权限归属未定义 → v2 已明确 admin 为唯一写入者，prescription 只读
- **迭代1-问题6**：分诊规则配置变更的生效机制未定义 → v2 已补充 Caffeine 定时缓存刷新 + 事件驱动失效
- **迭代1-问题7**：科室模板配置的 CRUD 管理和默认兜底缺失 → v2-v4 已补充 DEFAULT 兜底、getTemplate 契约、模板管理接口
- **迭代1-问题8**：对话会话内存存储未覆盖服务重启场景 → v2 已补充 findOrCreate 三分支模式和 TRIAGE_SESSION_EXPIRED 错误码
- **迭代1-问题9**：新模块依赖声明未包含 common-module-api → v2 已补充三个新模块对 common-module-api 的依赖
- **迭代1-问题10**：剂量标准数据初始化方案和编码规范缺失 → v2 已补充种子数据脚本路径和药品编码规范
- **迭代2-问题1**：BLOCK 风险等级缺少后端强制阻断执行机制 → v3 已新增 PrescriptionAuditEnforcer 接口和 HTTP 422 阻断机制
- **迭代2-问题2**：AuditRecord 缺少处方级关联标识 → v3 已补充 prescriptionOrderId/doctorId/patientId 必填字段
- **迭代3-问题1**：AuditRecord 落库在降级路径中被遗漏 → v4 已显式补充降级路径 AuditRecord 持久化步骤
- **迭代3-问题2**：分诊降级链路不完整 → v4 已改为线性三级降级链
- **迭代3-问题3**：TriageResponse DTO 字段结构未定义 → v4 已补充字段定义
- **迭代3-问题4**：剂量单位转换规则集未定义 → v4 已补充 DosageUnitGroup 枚举
- **迭代3-问题5**：MedicalRecord 实体字段未定义 → v4 已补充实体描述和 MedicalRecordField 枚举
- **迭代3-问题6**：规则/模板配置变更缺少审计溯源 → v4 已补充 ConfigChangeLog 实体
- **迭代4-问题1**：AiSuggestionResult 缺少 FAILED 状态 → v5 已补充 FAILED + failReason + 预创建→更新模式
- **迭代4-问题2**：TriageRequest 与 DialogueCreateRequest 关系不明确 → v5 已从目录移除 TriageRequest，统一使用 DialogueCreateRequest
- **迭代4-问题3**：关键 DTO 存在于目录但设计文本无定义 → v5 已补充 AuditRequest/AuditResponse/AuditIssue/LocalRuleResult/BlockResponse/FieldMissingHint 定义
- **迭代4-问题4**：DosageThresholdService 未定义"记录不存在"时的行为 → v5 已补充第5级"标准不存在"降级路径
- **迭代4-问题5**：DosageAlertLevel 枚举值未定义 → v5 已补充 INFO/WARN/CRITICAL 定义
- **迭代4-问题6**：check-dose 缺乏 frequency 字段 → v5 已补充 frequency + dailyMax + 日剂量校验分支
- **迭代4-问题7**：MedicalRecord 缺少 visitId → v5 已补充 visitId 必填字段
- **迭代4-问题8**：PrescriptionAssistResponse 字段定义不完整 → v5 已修正并补充完整定义
- **迭代4-问题9**：AuditRiskLevel 与需求文档术语映射缺失 → v5 已补充映射说明
- **迭代2-问题3**：AiSuggestionResult 内存存储未覆盖重启场景 → v3-v5 已补充四分支模式 + TTL + FAILED 状态
- **迭代2-问题4**：DosageStandard 未定义年龄/体重分级 → v4 已补充内联字段 + 四级匹配优先级
- **迭代2-问题5**：病历生成降级策略为"返回空病历框架" → v3 已改为分层保护策略

### 持续存在的问题（在多轮反馈中反复出现，需重点解决）

- **包E 与需求文档 3.4.10 AI 辅助开方契约对齐问题**：迭代4-问题6 补充了 frequency 但仅解决日剂量校验子问题；迭代5 再次提出 check-dose 与 3.4.10 完整契约脱节的根本问题——需要重新定位 check-dose 的角色或扩展包E 主端点能力。此问题已持续 2 轮，本轮需重点解决
- **需求文档契约对齐问题（分诊/审核/病历三类输入输出契约）**：迭代5 集中暴露了 TriageResponse、AuditRequest/Response、RecordGenerateRequest 与需求文档输出/输入契约的字段级不对齐问题。需系统性地对齐所有包的 API 契约与需求文档定义

### 新发现的问题（本轮新识别的问题）

- **问题6**：session_id 必填/可选语义矛盾（质询报告补充识别，诊断报告遗漏）
- **问题13**：AuditResponse 缺少 interactions/suggestions（质询报告补充识别）
- **问题14**：TriageRecord 字段定义缺失（质询报告补充识别）
- **问题1**：AiSuggestionResult 并发安全（v5 补充了 FAILED 状态但未解决并发更新保护问题，属于新发现的维度）

## 上一轮产出路径

C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281422_phase23_ood\a_v5_design_v1.md

## 用户需求

C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281422_phase23_ood\requirement.md