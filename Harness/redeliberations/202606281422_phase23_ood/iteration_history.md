## 迭代第 1 轮

1. **问题描述**：DialogueSession 不可变声明与可变追加操作的逻辑矛盾
   - 所在位置：§3.1 DialogueSession — "为何使用不可变 class" 说明 + 协作描述
   - 严重程度：严重
   - 改进建议：选择以下任一路径并更新文档：(a) 改为可变 class，DialogueSession 内部状态可变，DialogueSessionManager 负责并发访问控制，删除不可变理由；(b) 保持不可变，显式补充 copy-on-write 机制：DialogueSession.withNewRound(...) 返回新实例，DialogueSessionManager 负责原子替换
2. **问题描述**：包E 异步 AI 建议缺少消费路径
   - 所在位置：§6.3 "包E 的异步 AI 建议" + §3.4 PrescriptionAssistController
   - 严重程度：严重
   - 改进建议：补充异步 AI 建议的消费机制，至少包括：(a) 定义 GET /api/prescription/assist/suggestion/{taskId} 查询端点及其响应结构；(b) 或定义事件推送机制（WebSocket/SSE）以及前端订阅方式；(c) 明确 check-dose 响应中是否需要返回 taskId 用于后续查询
3. **问题描述**：多轮分诊中对话历史的维护责任与一致性不明确
   - 所在位置：§4.1 "多轮分诊流程" + §3.1 TriageRequest DTO 定义
   - 严重程度：严重
   - 改进建议：明确对话历史的单一真相来源：(a) 推荐方案：服务端 DialogueSession 为单一真相来源，前端只需在首轮请求时携带 chiefComplaint，后续请求仅携带 sessionId，TriageRequest.history 字段移至服务端内部不暴露至 API 契约；(b) 如确需前端维护历史，则需明确 DialogueSession 只维护状态元数据而不存储对话内容，并删除"会话上下文"的职责描述
4. **问题描述**：DosageCheckRequest 缺少给药途径参数
   - 所在位置：§3.4 DosageCheckRequest DTO 声明
   - 严重程度：严重
   - 改进建议：在 DosageCheckRequest 中增加 routeOfAdministration 字段（枚举类型，建议值：ORAL、IV、IM、TOPICAL 等），DosageStandard 实体也需相应增加给药途径维度，查询时 drugCode + routeOfAdministration 联合定位唯一阈值记录
5. **问题描述**：prescription 模块内 DosageStandard 实体的写权限归属未定义
   - 所在位置：§2.2 "包D-AI1 与包E 的强耦合处理"
   - 严重程度：严重
   - 改进建议：(a) 明确定义 DosageStandard 的数据归属：推荐由管理员端（Phase 5）或独立的数据维护层作为唯一写入者，D-AI1 和 E 仅持有读取权限；(b) 若确有写入需求，定义明确的写入协调机制（如事件驱动的版本递增或最后写入者胜出策略），并补充写入场景的描述
6. **问题描述**：分诊规则配置变更的生效机制未定义
   - 所在位置：§3.1 TriageRuleEngine + §7 设计决策 "分诊规则源"
   - 严重程度：严重
   - 改进建议：(a) 明确定义规则缓存策略：若需热加载，在 TriageRuleEngine 实现中引入定时刷新或通过 ApplicationEventPublisher 发布规则变更事件触发缓存失效；(b) 若接受重启生效，则修正 §7 的选型理由描述，删除"非开发人员动态调整"的表述；(c) 补充规则变更的前端/管理员操作路径说明
7. **问题描述**：科室模板配置的 CRUD 管理和默认兜底缺失
   - 所在位置：§3.3 DepartmentTemplateConfig / TemplateConfigManager + §2.1 目录结构 DeptTemplateConfigRepository
   - 严重程度：严重
   - 改进建议：(a) 补充默认模板兜底方案：在系统中维护一个 DEFAULT 科室条目，任何未匹配科室标识时回退到该通用模板；(b) 定义 getTemplate(departmentId) 的契约签名（返回 Optional<DepartmentTemplateConfig> 或抛 BusinessException）；(c) 明确模板管理接口；(d) 补充初始模板数据集
8. **问题描述**：对话会话内存存储未覆盖服务重启场景
   - 所在位置：§3.1 DialogueSessionManager + §7 设计决策 "多轮对话存储" 条目
   - 严重程度：一般
   - 改进建议：(a) 在 DialogueSessionManager.findOrCreate 中明确 "session 不存在" 时的处理策略（如返回 Optional.empty() 或抛 BusinessException）；(b) 补充统一的错误码 TRIAGE_SESSION_EXPIRED 及前端提示文案；(c) 在 API 文档中说明 session 有效期的限制
9. **问题描述**：新模块依赖声明未包含 common-module-api
   - 所在位置：§1.2 "各模块仅依赖 common 和 ai-api" + §2.2 依赖关系图
   - 严重程度：一般
   - 改进建议：在 §1.2 的依赖声明和 §2.2 的依赖关系图中，补充三个新模块对 common-module-api 的依赖（compile scope），与现有 patient/doctor/admin 模块保持一致
10. **问题描述**：剂量标准数据初始化方案和编码规范缺失
    - 所在位置：§3.4 DosageThresholdService + DosageStandard entity + DosageStandardRepository
    - 严重程度：一般
    - 改进建议：(a) 补充剂量标准数据的初始化方案，至少包含开发/测试用的种子数据 SQL 脚本路径和基础药品条目；(b) 明确药品编码规范；(c) 在 DosageThresholdService 中补充单位一致性校验逻辑

## 迭代第 2 轮

1. **问题描述**：BLOCK 风险等级缺少后端强制阻断执行机制，处方审核安全性依赖前端履约
   - 所在位置：§3.2 AuditRiskLevel 职责定位；§4.2 审核场景流程
   - 严重程度：严重
   - 改进建议：在业务层定义独立强制阻断切入机制，推荐路径 A（预检/提交端点分离）或路径 B（Controller 层拦截返回 403/422）
2. **问题描述**：AuditRecord 缺少处方级关联标识（prescriptionOrderId、doctorId、patientId），审计追踪能力受限
   - 所在位置：§3.2 AuditRecord 协作描述
   - 严重程度：严重
   - 改进建议：在 AuditRecord entity 中增加 prescriptionOrderId、doctorId、patientId 必填字段
3. **问题描述**：AiSuggestionResult 使用 ConcurrentHashMap 暂存，服务重启后所有结果丢失且无错误提示
   - 所在位置：§6.3 "包E 的异步 AI 建议"
   - 严重程度：一般
   - 改进建议：参照 §3.1 findOrCreate 三分支模式，补充建议不存在时的明确错误码及 TTL 说明
4. **问题描述**：DosageStandard 未定义支持年龄/体重分级的剂量存储结构，影响剂量校验实现
   - 所在位置：§2.1 common/entity/DosageStandard.java；§3.4 DosageThresholdService
   - 严重程度：一般
   - 改进建议：明确定义年龄/体重分级支持方式（方式A：添加年龄体重范围字段；方式B：拆分 AgeBandedDosage 子实体）
5. **问题描述**：病历生成降级策略为"返回空病历框架 + 全字段缺失"，丢弃已提取的结构化字段
   - 所在位置：§4.3 病历生成场景——降级流程
   - 严重程度：一般
   - 改进建议：改为"分层保护"策略，保留已提取字段，仅标记缺失字段

## 迭代第 3 轮

1. **问题描述**：AuditRecord落库在降级路径中被遗漏，与§3.2行为契约矛盾
   - 所在位置：§4.2 "处方审核场景"——降级流程描述
   - 严重程度：严重
   - 改进建议：在§4.2降级流程中显式补充AuditRecord持久化步骤，或与正常路径合并为一个统一表述以消除歧义。同时确认：本地规则校验结果写入AuditRecord的riskLevel字段时，是否需额外标记fromFallback=true以区分AI结果与本地规则结果。
2. **问题描述**：分诊降级链路不完整——AI为空且规则也为空时的行为未定义
   - 所在位置：§4.1 "智能分诊场景"
   - 严重程度：一般
   - 改进建议：将三条路径改为线性降级链：AI无结果→规则匹配无结果→FallbackProvider兜底。在§4.1中明确TriageRuleEngine.match()返回空列表时，应继续调用DepartmentFallbackProvider.getFallbackDepartments()；TriageService的协作描述也需同步更新。
3. **问题描述**：TriageResponse DTO字段结构未定义，无法支撑编码实现
   - 所在位置：§3.1 包C核心抽象；§2.1目录
   - 严重程度：一般
   - 改进建议：补充TriageResponse的字段定义，至少包含：departments（推荐科室列表）、sessionId（多轮场景）、needFollowUp（是否需要追问）、followUpQuestion（追问内容）、confidence（可选置信度标量）。同时明确DialogueCreateRequest是否为首轮请求DTO，若是则说明其字段。
4. **问题描述**：剂量单位转换规则集未定义，实现面临随意假设风险
   - 所在位置：§8.3 "单位一致性校验"
   - 严重程度：一般
   - 改进建议：补充剂量单位兼容分组的枚举或表格定义。推荐方案：定义一个DosageUnitGroup枚举（如MASS_GROUP: mcg↔mg↔g、VOLUME_GROUP: ml↔L等），各组内支持自动换算，跨组返回RX_ASSIST_UNIT_MISMATCH。对于IU等非质量/体积单位可单独分组或定义为不可换算。转换系数以常量形式固化在DosageThresholdService中，后续可扩展为数据库可配置。
5. **问题描述**：MedicalRecord实体字段未定义，病历结构化输出模型缺失
   - 所在位置：§3.3 包D-AI2；§2.1目录 entity/MedicalRecord.java
   - 严重程度：一般
   - 改进建议：在§3.3中补充核心字段枚举或值对象定义（如MedicalRecordField枚举），列出病历结构化输出的顶层字段标识。RecordGenerateRequest和RecordGenerateResponse补充字段定义。DepartmentTemplateConfig.requiredFields的类型明确为List<MedicalRecordField>。这与§9.1中DEFAULT模板字段列表形成呼应，使字段标识符与模板内容一致。
6. **问题描述**：规则/模板配置变更缺少审计溯源能力
   - 所在位置：§3.1 TriageRuleEngine（规则热加载）；§3.3 TemplateConfigManager（模板事件驱动刷新）；§9（模板更新）
   - 严重程度：一般
   - 改进建议：在规则变更和模板更新的事件处理中添加配置变更日志记录。推荐方案：(a) 定义ConfigChangeLog实体（归属admin模块或common模块）；(b) 在TemplateConfigChangeEvent和规则变更的处理链中写入审计日志；(c) 事件发布者提供新旧值语义。Phase 2可先以日志文件方式实现，后续迁移至数据库审计表。

## 迭代第 4 轮

1. **问题描述**：AiSuggestionResult 状态枚举缺少 FAILED 状态，异步 AI 失败时产生"幽灵 pending"场景
   - 所在位置：§3.4 AiSuggestionResult 查询契约 + §6.3 异步 AI 建议
   - 严重程度：严重
   - 改进建议：在 AiSuggestionResult 中新增 FAILED 状态枚举值，含可选的 failReason 字段。实现侧采用"预创建→更新"模式：check-dose 响应前以 PENDING 状态预创建 AiSuggestionResult 条目，异步回调完成后更新为 COMPLETED 或 FAILED；异常被 CompletableFuture exceptionally 处理器捕获时同样写入 FAILED 状态而非留下 PENDING 僵尸条目。前端根据 FAILED 状态展示明确失败提示。
2. **问题描述**：TriageRequest DTO 存在于目录结构但设计文本中无定义，与 DialogueCreateRequest 关系不明确
   - 所在位置：§2.1 目录列出了 `dto/TriageRequest.java`，但 §1.3 和 §3.1 的核心抽象定义中仅有 DialogueCreateRequest 而无 TriageRequest
   - 严重程度：一般
   - 改进建议：任选以下清除路径之一：路径 A（推荐）— 从目录中删除 TriageRequest.java 条目，统一使用 DialogueCreateRequest 作为分诊请求 DTO，并在 §3.1 DialogueCreateRequest 的说明中补充 sessionId 字段；路径 B — 保留 TriageRequest 作为分诊请求的顶层 DTO，明确其与 DialogueCreateRequest 的差异和各自的适用场景，在 §3.1 中补充定义。
3. **问题描述**：LocalRuleResult / AuditIssue / AuditRequest / AuditResponse / FieldMissingHint 等关键 DTO 存在于目录但设计文本无定义
   - 所在位置：§2.1 目录列出但全文未定义：`prescription/rule/LocalRuleResult.java`、`prescription/dto/audit/AuditIssue.java`、`prescription/dto/audit/AuditRequest.java`、`prescription/dto/audit/AuditResponse.java`、`medical-record/dto/FieldMissingHint.java`
   - 严重程度：一般
   - 改进建议：在 §3.2（处方审核）和 §3.3（病历生成）中补充上述 DTO 的字段定义。至少应明确：LocalRuleResult 每条规则独立的检查结果（ruleId、passed、message、severity）及聚合方式；AuditIssue 的 fieldName、issueDescription、ruleId、severity；AuditResponse 的 riskLevel、issues（AuditIssue 列表）、fromFallback；FieldMissingHint 的 missingField、promptMessage、suggestedAction。
4. **问题描述**：DosageThresholdService 四级匹配策略未定义"记录不存在"时的行为，RX_ASSIST_DOSE_STANDARD_NOT_FOUND 触发场景不明
   - 所在位置：§8.4 匹配优先级 + §4.4 check-dose 流程 + §5.1 错误码表
   - 严重程度：一般
   - 改进建议：在 §8.4 中补充第 5 种结局：四级均未命中时返回 DosageAlert（level=WARN）并携带 RX_ASSIST_DOSE_STANDARD_NOT_FOUND 错误码，同时在前端展示"该药品剂量标准未配置，请核实剂量"的降级提示。在 §4.4 check-dose 流程中补充此分支路径。
5. **问题描述**：DosageAlert 告警级别（alertLevel）枚举值未定义，影响前端渲染策略
   - 所在位置：§3.4 DosageAlert 类描述字段"级别"
   - 严重程度：一般
   - 改进建议：在 §3.4 中补充 DosageAlert.alertLevel 枚举定义。建议值：INFO（信息提示，不影响流程）、WARN（警告剂量，需确认）、CRITICAL（危险剂量，强制阻断）。并明确 alertLevel=CRITICAL 时是否联动触发 PrescriptionAuditService 的 BLOCK 审核。
6. **问题描述**：check-dose 请求缺乏用药频率（frequency）字段，无法支持日剂量上限校验
   - 所在位置：§4.4 check-dose API 请求参数 `{ drugCode, dosage, unit, routeOfAdministration, patientAge, patientWeight }`
   - 严重程度：一般
   - 改进建议：在 DosageCheckRequest 中增加 frequency 字段（枚举或整数次/日），DosageStandard 实体增加 dailyMax 字段（日剂量上限），DosageThresholdService.check() 中增加日剂量校验分支。若确认日剂量校验仅在处方审核阶段进行，则应在设计文本中显式说明此分层策略。
7. **问题描述**：MedicalRecord 实体缺少就诊关联标识（visitId/registrationId），病历数据存在追溯缺口
   - 所在位置：§3.3 MedicalRecord 实体字段描述 + RecordGenerateRequest DTO
   - 严重程度：一般
   - 改进建议：在 MedicalRecord 实体中增加 visitId（必填字段，关联 Registration/Visit 记录），在 RecordGenerateRequest 中也增加 visitId 作为必填参数。
8. **问题描述**：PrescriptionAssistResponse 字段定义不完整，§4.4 响应体与目录注释存在差异
   - 所在位置：§2.1 目录注释 + §4.4 响应体
   - 严重程度：轻微
   - 改进建议：修正 §2.1 中 PrescriptionAssistResponse.java 的注释为 `# 含 alerts（DosageAlert 列表）和 taskId`，并在 §3.4 中新增 PrescriptionAssistResponse 的字段定义。
9. **问题描述**：设计文本中缺少 AuditRiskLevel 与需求文档风险等级术语的映射说明
   - 所在位置：§3.2 AuditRiskLevel 枚举定义
   - 严重程度：轻微
   - 改进建议：在 §3.2 AuditRiskLevel 定义处增加一行消息映射说明，如 `BLOCK（对应需求文档的"高风险"）`、`WARN（对应"中风险"）`、`PASS（对应"低风险"）`，并在 §4.2 WARN 分支中显式说明"允许医生强制提交并留痕"以对齐需求中 MEDIUM 等级的三种可选操作。

## 迭代第 5 轮

1. **问题描述**：AiSuggestionResult预创建→更新模式在异步并发场景下存在数据竞争，ConcurrentHashMap不保证value对象状态一致性
   - 所在位置：§3.4 AiSuggestionResult + §6.3 "包E 的异步 AI 建议"
   - 严重程度：严重
   - 改进建议：AiSuggestionResult内部使用volatile/AtomicReference/CAS保护状态更新，或使用ConcurrentHashMap.compute()原子更新；同一taskId状态更新保证幂等

2. **问题描述**：包E check-dose请求与需求文档3.4.10 AI辅助开方输入契约完全脱节，仅实现单药品剂量检查而非完整AI辅助开方能力
   - 所在位置：§3.4 PrescriptionAssistService / DosageThresholdService + §4.4 check-dose API + §6.3 异步AI建议
   - 严重程度：严重
   - 改进建议：定义POST /api/prescription/assist作为3.4.10主端点接收完整输入，或显式标注当前设计仅覆盖剂量阈值告警子集并预留扩展结构

3. **问题描述**：TriageResponse缺少需求文档3.4.1的推荐医生列表(recommended_doctors)和推荐理由(reason)字段，DialogueCreateRequest缺少ruleVersion/additionalResponses字段
   - 所在位置：§3.1 TriageResponse + DialogueCreateRequest + §4.1 智能分诊场景
   - 严重程度：严重
   - 改进建议：TriageResponse增加doctors和reason字段；DialogueCreateRequest增加ruleVersion和additionalResponses字段

4. **问题描述**：AuditRequest.patientInfo未显式列出allergy_history和comorbidities字段结构，prescriptionItems每项字段定义不完整
   - 所在位置：§3.2 AuditRequest + §4.2 处方审核场景
   - 严重程度：严重
   - 改进建议：patientInfo显式列出allergy_history(string)和comorbidities(List<String>)，prescriptionItems每项包含drugId/drugName/dose/frequency/duration/route六字段

5. **问题描述**：RecordGenerateRequest.visitId与需求文档encounter_id的映射关系未说明，departmentId引入理由未说明
   - 所在位置：§3.3 RecordGenerateRequest + §4.3 病历生成场景
   - 严重程度：严重
   - 改进建议：明确visitId与encounter_id映射关系，说明departmentId引入理由和与encounter_id的优先级

6. **问题描述**：WARN级处方"强制提交并留痕"的留痕数据结构未定义（缺forceSubmitted/forceSubmitTime字段）
   - 所在位置：§3.2 AuditRecord + §4.2 WARN分支
   - 严重程度：一般
   - 改进建议：AuditRecord增加forceSubmitted(Boolean)和forceSubmitTime(LocalDateTime)字段，或显式定义其他承载实体

7. **问题描述**：sessionId生成策略、格式规范未定义，前端/后端生成职责不清
   - 所在位置：§3.1 DialogueSession / DialogueSessionManager + §4.1 智能分诊场景
   - 严重程度：一般
   - 改进建议：明确sessionId由后端DialogueSessionManager统一生成，采用UUID v4格式，首轮请求sessionId为空

8. **问题描述**：需求文档3.4.1定义session_id为必填，但设计文档§3.1允许首轮请求sessionId为空，构成契约必填/可选语义矛盾
   - 所在位置：§3.1 DialogueCreateRequest + 需求文档3.4.1输入契约
   - 严重程度：严重
   - 改进建议：在设计文档中明确首轮请求时sessionId传空值的合理性，或与需求方澄清首轮场景下session_id的可选性

9. **问题描述**：DosageAlertLevel.CRITICAL与AuditRiskLevel.BLOCK联动触发机制调用链路未定义，存在触发时机矛盾
   - 所在位置：§3.4 DosageAlertLevel.CRITICAL描述 + §2.2 包D-AI1与包E强耦合处理
   - 严重程度：一般
   - 改进建议：CRITICAL剂量告警写入处方草稿上下文而非直接调用审核服务，提交时从草稿上下文读取CRITICAL标记作为BLOCK判定输入

10. **问题描述**：DrugInteractionRule和AllergyCheckRule所需的数据实体(DrugInteractionPair/DrugAllergyMapping)完全缺失
    - 所在位置：§2.1 目录结构 + §3.2 LocalRuleEngine
    - 严重程度：一般
    - 改进建议：补充DrugInteractionPair和DrugAllergyMapping数据实体定义，或标注Phase 2/3仅实现DosageLimitRule其余待实现

11. **问题描述**：分诊降级链中"AI无结果"判定边界不清晰——AI返回空列表是否视为有效结果未定义
    - 所在位置：§4.1 智能分诊场景 + §3.1 TriageService
    - 严重程度：一般
    - 改进建议：明确"AI无结果"判定条件：success=false/degraded=true降级；success=true但空列表视为有效结果跳过规则引擎

12. **问题描述**：处方审核AI超时阈值(6s)和病历生成AI超时阈值(12s)未在设计文本中体现
    - 所在位置：§3.2 PrescriptionAuditService + §3.3 MedicalRecordService + §4.2/§4.3
    - 严重程度：一般
    - 改进建议：在§4.2/§4.3补充超时配置说明，定义ai.timeout.prescription-audit=6s和ai.timeout.medical-record-generate=12s

13. **问题描述**：AuditResponse与需求3.4.2输出契约不对齐——缺少interactions(药物相互作用结果)和suggestions(用药建议)字段
    - 所在位置：§3.2 AuditResponse + §4.2 处方审核场景
    - 严重程度：一般
    - 改进建议：AuditResponse补充interactions和suggestions结构化输出字段，与需求3.4.2输出契约对齐

14. **问题描述**：TriageRecord实体字段定义缺失，无法满足需求3.4.1"分诊结果需持久化以供统计与质量分析"的可观测性约束
    - 所在位置：§2.1 TriageRecord + 需求文档3.4.1服务质量要求
    - 严重程度：一般
    - 改进建议：补充TriageRecord字段定义，至少包含AI推荐科室、规则匹配科室、最终选择科室、置信度、降级标记等统计必需字段

## 迭代第 6 轮

1. **问题描述**：AuditResponse.issues 与需求文档 3.4.2 alerts 字段命名与结构不一致——字段名不同（issues vs alerts），子字段结构完全不同（fieldName/issueDescription/ruleId vs alert_code/alert_message）
   - 所在位置：§3.2 AuditResponse + AuditIssue 定义；§1.3 核心抽象一览
   - 严重程度：严重
   - 改进建议：在 AuditResponse 中同时保留 alerts（对齐需求契约）和 issues（设计自有结构）并说明映射关系，或将 AuditIssue 字段名对齐为 alertCode/alertMessage；在 §3.2 或 §7 补充设计决策说明
2. **问题描述**：DialogueCreateRequest 缺少 ruleSetId 字段，与需求文档 3.4.1 输入契约不对齐
   - 所在位置：§3.1 DialogueCreateRequest 定义；§1.3 核心抽象一览
   - 严重程度：一般
   - 改进建议：增加 ruleSetId（可选，String），对齐需求文档 rule_set_id，补充消费语义
3. **问题描述**：TriageResponse 缺少 matchedRules 字径，与需求文档 3.4.1 输出契约不对齐
   - 所在位置：§3.1 TriageResponse 定义；§1.3 核心抽象一览
   - 严重程度：一般
   - 改进建议：增加 matchedRules（可选，List<MatchedRule>），对齐需求文档 matched_rules
4. **问题描述**：TriageResponse.departments 每项字段结构未定义，与需求文档 3.4.1 recommended_departments 不完整对齐
   - 所在位置：§3.1 TriageResponse；§1.3 核心抽象一览
   - 严重程度：一般
   - 改进建议：新增 RecommendedDepartment DTO（含 departmentId、departmentName、score），与需求文档对齐
5. **问题描述**：病历生成缺少流式输出生成模式的完整设计——无流式端点、无流式超时配置、无分片/错误处理机制
   - 所在位置：§3.3 MedicalRecordController / MedicalRecordService；§4.3 病历生成场景；§5.5 AI 超时配置
   - 严重程度：严重
   - 改进建议：若当前不实现流式，显式标注"流式待后续迭代"并补充 stream 字段和流式超时配置项；若实现流式则补充完整流式契约（SSE/WebSocket端点、流式超时、错误分片）
6. **问题描述**：RecordGenerateRequest 缺少 stream 字段，与需求文档 3.4.3 输入契约不对齐
   - 所在位置：§3.3 RecordGenerateRequest 定义
   - 严重程度：一般
   - 改进建议：增加 stream（bool，可选，默认 false），对齐需求文档 3.4.3；若不实现流式则标注"Phase 3 仅支持非流式"
7. **问题描述**：RecordGenerateResponse 的 fields 映射结构与需求文档 3.4.3 输出契约字段命名不一致（如 TREATMENT_ADVICE vs treatment_plan），missing_fields 与 missingFieldHints 映射关系未说明
   - 所在位置：§3.3 RecordGenerateResponse；§3.3 MedicalRecordField 枚举
   - 严重程度：一般
   - 改进建议：补充 MedicalRecordField 枚举与需求文档字段名的映射说明；说明 missingFieldHints 是 missing_fields 的结构化升级版本
8. **问题描述**：本地规则校验仅实现 1/4 最小检查项集，药品禁忌检查和重复用药检查具备本地实现条件但未纳入
   - 所在位置：§3.2 LocalRuleEngine；§4.2 处方审核降级路径
   - 严重程度：严重
   - 改进建议：将药品禁忌检查和重复用药检查纳入 Phase 3 最低交付范围；若确有困难，显式标注为降级安全缺口并增加风险提示
9. **问题描述**：PrescriptionDraftContext 的 key 定义不清（encounterId vs 处方编辑会话标识），创建/清理时机未定义
   - 所在位置：§3.4 PrescriptionDraftContext；§6.4 处方草稿上下文并发管理
   - 严重程度：一般
   - 改进建议：明确 key 为 prescriptionId；定义创建时机（首次 check-dose 请求）和清理时机（提交成功/取消/TTL 过期）
10. **问题描述**：分诊场景缺少 AI 连续失败 3 次的兜底提示机制
    - 所在位置：§3.1 DialogueSessionManager；§4.1 智能分诊场景
    - 严重程度：一般
    - 改进建议：在 DialogueSession 增加 aiFailCount 字段，TriageServiceImpl 降级后检查 aiFailCount≥3 时附加兜底提示
11. **问题描述**：AuditRecord 缺少审核次序（auditSequence）和是否最新（isLatest）字段，无法按序追溯和快速定位最新审核
    - 所在位置：§3.2 AuditRecord 实体定义
    - 严重程度：一般
    - 改进建议：增加 auditSequence（int，递增）和 isLatest（boolean，仅最新一条为 true）字段
12. **问题描述**：DosageStandard 与药品基础信息实体的关系未定义，数据录入路径不明确
    - 所在位置：§2.2 DosageStandard；§8.1 初始化方案；§8.2 药品编码规范
    - 严重程度：一般
    - 改进建议：说明两者通过 drugCode 关联但为独立实体；明确 admin 模块维护入口和 drugCode 主键关联关系

## 迭代第 7 轮

1. **问题描述**：ai-api 层 DTO 与业务层 DTO 之间存在严重的字段级契约缺口，设计未定义二者间的映射/转换机制
   - 所在位置：§1.2 AiService接口描述 + §3.1/3.2/3.3/3.4 各Service协作描述 + §2.2 "与 AI 模块的协作关系"
   - 严重程度：严重
   - 改进建议：定义ai-api层DTO扩展策略，补充完整字段定义与需求文档对齐；明确业务层DTO与ai-api层DTO的转换规则和转换责任归属

2. **问题描述**：PrescriptionAssistRequest（ai-api层）与业务层 PrescriptionAssistRequest（assist DTO）同名且职责不同，设计未区分
   - 所在位置：§3.4 PrescriptionAssistService + §2.2 "与 AI 模块的协作关系" + AiService 接口签名
   - 严重程度：严重
   - 改进建议：明确区分ai-api层DTO与业务层DTO的字段差异和命名空间关系；补充ai-api层DTO完整字段定义或复用策略

3. **问题描述**：allergy_details 扩展容器完全未纳入设计，与需求文档过敏信息扩展性方案脱节
   - 所在位置：§3.2 AuditRequest.patientInfo + §3.4 PrescriptionAssistRequest.patientInfo + §3.2 AllergyCheckRule
   - 严重程度：严重
   - 改进建议：在patientInfo中增加allergyDetails可选字段；AllergyCheckRule存在时优先按结构化过敏信息做精确匹配

4. **问题描述**：RX_ASSIST_AI_NO_RECOMMENDATION 错误码已定义但消费场景未描述，/assist主端点AI返回无可推荐药品时的行为不明确
   - 所在位置：§3.4 PrescriptionAssistService + §4.4 辅助开方场景 + §5.1 错误码表
   - 严重程度：严重
   - 改进建议：在/assist主端点流程中补充"AI返回无可推荐药品"场景的处理路径

5. **问题描述**：多轮分诊场景下TriageServiceImpl如何将DialogueSession的上下文传递给AiService.triage()未定义
   - 所在位置：§3.1 TriageService协作描述 + §4.1 智能分诊场景 + AiService.triage()签名
   - 严重程度：严重
   - 改进建议：补充AiService.triage()的调用组装说明，扩展ai-api层TriageRequest增加additionalResponses字段，明确全量拼接策略

6. **问题描述**：推荐医生的推荐机制和数据来源未定义，RecommendedDoctor的score计算和availableSlotCount获取路径未说明
   - 所在位置：§3.1 TriageResponse.doctors + RecommendedDoctor + §2.2 依赖规则 + §4.1
   - 严重程度：严重
   - 改进建议：明确推荐医生列表生成机制；若需跨模块查询则补充cross-module数据获取机制

7. **问题描述**：TriageRecord写入时机和触发方未在行为契约中体现，finalDepartmentId赋值时机不明确
   - 所在位置：§3.1 TriageService + TriageRecord + §4.1 智能分诊场景
   - 严重程度：一般
   - 改进建议：在TriageService接口职责中明确TriageRecord写入步骤和时机；说明finalDepartmentId的补充写入机制

8. **问题描述**：本地规则聚合逻辑缺少精确的"风险等级判定规则表"，AllergyCheckRule一律输出BLOCK无法区分严重程度
   - 所在位置：§3.2 LocalRuleEngine + LocalRuleResult聚合逻辑 + AllergyCheckRule/DosageLimitRule
   - 严重程度：一般
   - 改进建议：明确LocalRuleResult.severity类型为AuditRiskLevel；为每条规则补充severity判定细节

9. **问题描述**：WARN级处方"强制提交并留痕"的前后端交互链路不完整，前端如何通知后端未定义
   - 所在位置：§3.2 AuditRecord.forceSubmitted + §4.2 WARN分支 + PrescriptionAuditController
   - 严重程度：一般
   - 改进建议：在处方提交端点增加forceSubmit参数，补充后端校验逻辑

10. **问题描述**：病历生成非流式超时场景的"部分保留"行为契约缺少对AiResult降级模式的定义
    - 所在位置：§3.3 MedicalRecordService + §4.3 病历生成场景 + §5.5 AI超时配置
    - 严重程度：一般
    - 改进建议：补充非流式超时降级路径，AiResult增加partialData字段承载部分结果

11. **问题描述**：WARN级处方审核与强制提交的时序竞态未防护
    - 所在位置：§3.2 AuditRecord + §4.2 WARN分支
    - 严重程度：一般
    - 改进建议：在强制提交路径中补充处方版本校验，比较当前处方内容与AuditRecord.originalPrescription

12. **问题描述**：MedicalRecord实体缺少MedicalRecordField级字段存储形式定义
    - 所在位置：§3.3 MedicalRecord实体 + MedicalRecordField枚举 + MedicalRecordRepository
    - 严重程度：一般
    - 改进建议：明确病历内容存储形式为单列JSON TEXT；补充Repository查询方法列表和增量更新语义

13. **问题描述**：配置变更审计日志跨模块事件传递的事务边界未定义
    - 所在位置：§9.2/§9.3 TemplateConfigChangeEvent + ConfigChangeLog + §2.2
    - 严重程度：一般
    - 改进建议：使用@TransactionalEventListener(phase=AFTER_COMMIT)；补充事件丢失补偿机制说明

14. **问题描述**：consultation模块AI分诊首次调用缺少对需求文档session_id必填语义的完整对齐
    - 所在位置：§3.1 DialogueCreateRequest + §7 设计决策
    - 严重程度：一般
    - 改进建议：首轮请求也要求前端生成并传入sessionId，消除"首轮为空"特殊分支

15. **问题描述**：DrugInteractionPair和DrugCompositionDict实体缺少持久化层定义
    - 所在位置：§2.1 目录结构 + §3.2 LocalRuleEngine数据来源
    - 严重程度：一般
    - 改进建议：将实体移至entity/包下；补充核心字段定义和对应Repository

16. **问题描述**：分诊规则引擎的规则数据实体和TriageRule匹配模型未定义
    - 所在位置：§3.1 TriageRuleEngine + §4.1 + §7
    - 严重程度：一般
    - 改进建议：补充TriageRule实体核心字段；补充match()方法签名；说明规则版本存储位置

17. **问题描述**：处方提交端点不在当前设计范围内，但BLOCK阻断和WARN留痕的端到端闭环依赖此端点
    - 所在位置：§3.2 PrescriptionAuditController + §3.4 PrescriptionAssistController + §4.2
    - 严重程度：一般
    - 改进建议：显式标注处方提交端点的设计边界；若不在范围内则补充待办标注

18. **问题描述**：ConcurrentHashMap跨实例不共享，水平扩展时session丢失
    - 所在位置：§6.1 + §6.3 + §6.4 + §7
    - 严重程度：一般
    - 改进建议：补充部署约束说明（单实例或sticky session）；标注Phase 5迁移节点

## 迭代第 8 轮

1. **问题描述**：DoctorFacade 放置于 auth 包下，与"认证"包语义不匹配，影响编码者模块定位判断
   - 所在位置：§2.1 目录结构 `common-module-api/.../auth/UserFacade.java, DoctorFacade.java`；§1.3 核心抽象一览 DoctorFacade 条目
   - 严重程度：一般
   - 改进建议：在 common-module-api 下为 DoctorFacade 创建独立子包（如 `commonmodule/doctor/DoctorFacade.java`），与 auth 包解耦

2. **问题描述**：需求文档 §3.4.1 matched_rules 子结构未定义，设计侧主动定义了 ruleId/ruleName/score 但未在设计决策中论证选择理由
   - 所在位置：§1.3 核心抽象一览 MatchedRule DTO；§10.1 ai-api 层 MatchedRuleItem；§7 设计决策表（缺少 matched_rules 子字段设计决策条目）
   - 严重程度：一般
   - 改进建议：在 §7 设计决策中增加 matched_rules 子字段的设计决策条目，说明需求侧缺口及设计侧主动选择理由

3. **问题描述**：需求文档 §3.4.2 检查项 #2 明确要求合并症-药品禁忌检查，设计仅实现过敏史检查，合并症检查无对应规则实现
   - 所在位置：§3.2 LocalRuleEngine 实现范围表 + AllergyCheckRule 描述
   - 严重程度：严重
   - 改进建议：新增 ContraindicationCheckRule 或扩展 AllergyCheckRule，补充合并症-药品禁忌检查；同步更新 §3.2 实现范围表、§2.1 目录结构和 §7 设计决策

4. **问题描述**：AiResult 的 failure()/degraded() 工厂方法均将 data 设为 null，无法同时传递错误码和部分数据；设计文本对"新增 partialData 字段"与"使用现有 data 字段"存在歧义，且仅覆盖 degraded() 路径遗漏 failure() 路径
   - 所在位置：§3.3 MedicalRecordService "非流式超时降级路径"；§7 设计决策 "AiResult 超时降级模式"
   - 严重程度：严重
   - 改进建议：明确使用现有 AiResult.data 字段承载部分结果；为 failure() 和 degraded() 各增加携带 partialData 的重载；删除"新增 partialData 字段"歧义描述

5. **问题描述**：需求文档 §5.1 分诊记录实体明确含"推荐医生"字段，设计 TriageRecord 未持久化推荐医生数据
   - 所在位置：§3.1 TriageRecord 实体字段列表
   - 严重程度：严重
   - 改进建议：在 TriageRecord 中增加 recommendedDoctors（JSON TEXT）字段

6. **问题描述**：AuditAlert.severity 字段未定义类型和值域，与 AuditRiskLevel 维度不同但未明确区分
   - 所在位置：§1.3 AuditAlert DTO；§3.2 AuditResponse；§10.2 ai-api 层 AlertItem
   - 严重程度：一般
   - 改进建议：补充 severity 字段类型和值域定义，推荐为独立枚举 AlertSeverity/INFO/WARNING/CRITICAL

7. **问题描述**：DosageAlert 无承载错误码的字段，RX_ASSIST_DOSE_STANDARD_NOT_FOUND 传递路径不明确
   - 所在位置：§3.4 DosageAlert 类定义；§3.4 DosageCheckResponse；§4.4 check-dose 流程
   - 严重程度：一般
   - 改进建议：在 DosageAlert 或 DosageCheckResponse 中增加 errorCode（String，可选）字段

8. **问题描述**：处方提交 WARN 级强制提交时"处方与 AuditRecord.originalPrescription 一致"的比较语义未定义
   - 所在位置：§4.2 处方提交端点"处方版本校验"逻辑
   - 严重程度：一般
   - 改进建议：补充结构化比较语义定义（drugId + dose + frequency + duration + route 五字段组合比对）

9. **问题描述**：allergyHistory/allergyDetails 数据来源语义不一致，v2 问题9和问题16改进建议相互矛盾
   - 所在位置：§3.2 AuditRequest.patientInfo；§3.4 PrescriptionAssistRequest.patientInfo；§10.2 / §10.4 ai-api 层 DTO
   - 严重程度：一般
   - 改进建议：统一为后端优先从健康档案提取+前端存入作为补充的双通道语义，Service 层定义来源优先级规则

10. **问题描述**：辅助开方过敏告警与处方审核过敏检查的逻辑重叠关系未说明
    - 所在位置：§3.4 PrescriptionAssistService "本地即时校验"；§3.2 AllergyCheckRule
    - 严重程度：一般
    - 改进建议：补充二者关系说明——即时提示 vs 提交时正式审核，独立执行不互斥

11. **问题描述**：RegistrationEvent 跨模块事件契约未定义
    - 所在位置：§3.1 TriageService "TriageRecord 写入时机"；§4.1 持久化说明
    - 严重程度：一般
    - 改进建议：定义 RegistrationEvent 事件契约及跨模块事件传递机制说明

12. **问题描述**：TriageResponse.degraded=true 时前端应如何调整 UI 未说明
    - 所在位置：§3.1 TriageService 降级链；§1.3 TriageResponse.degraded 字段
    - 严重程度：轻微
    - 改进建议：补充 degraded=true 时前端行为说明——仍渲染推荐科室列表同时显示降级提示并提供手动选择入口

13. **问题描述**：设计目标"规避 Phase 5 迁移成本"未显式论证
    - 所在位置：§1.1 设计目标"底座直接落地"
    - 严重程度：轻微
    - 改进建议：在 §1.1 或 §7 增加底座落地与 Phase 5 迁移兼容性设计决策条目

14. **问题描述**：§5.1 错误码表混合含 _AI_ 中段和不含 _AI_ 中段的错误码，分类命名规则不明确
    - 所在位置：§5.1 错误码表
    - 严重程度：一般
    - 改进建议：在 §5.1 增加 AI 能力错误码与本地业务错误码的命名区分规则说明

15. **问题描述**：DialogueSession TTL 清理竞态和规则快照失效场景处理未定义
    - 所在位置：§6.1 对话会话并发管理；§3.1 DialogueSession ruleVersion/ruleSetId 快照机制
    - 严重程度：一般
    - 改进建议：补充 TTL 清理竞态处理说明和规则快照失效降级处理

16. **问题描述**：§5.1 错误码表遗漏需求文档明确定义的 RX_ASSIST_AI_NO_RECOMMENDATION、RX_AUDIT_AI_INPUT_INVALID、MR_GEN_AI_INPUT_INVALID、MR_GEN_AI_OUTPUT_INCOMPLETE
    - 所在位置：§5.1 错误码表
    - 严重程度：一般
    - 改进建议：补齐需求文档 §3.4.x 明确定义的全部 AI 能力错误码

## 迭代第 9 轮

1. **问题描述**：辅助开方"医生确认后生效"的完整流程缺失，AI建议的采纳/修改/拒绝决策机制及确认记录均未定义
   - 所在位置：§3.4 PrescriptionAssistService 协作描述；§4.4 辅助开方场景
   - 严重程度：严重
   - 改进建议：定义"医生确认"动作的承载实体（如 SuggestionConfirmation），或显式标注 Phase 2/3 范围边界
2. **问题描述**：规则配置的 admin 管理接口契约未定义，包括版本发布/回滚、批量启用/禁用等 API 缺口，影响并行开发
   - 所在位置：§3.1 TriageRuleEngine；§9.2 模板管理接口定义部分
   - 严重程度：严重
   - 改进建议：在设计中补充 admin 模块规则管理接口简要契约定义，或标注由 admin 模块 OOD 独立定义并交叉引用
3. **问题描述**：跨模块事件传递（RegistrationEvent）消费端缺少失败补偿策略，finalDepartmentId 可能永久为空
   - 所在位置：§2.2 跨模块事件传递机制；§3.1 TriageRecord 写入时机
   - 严重程度：一般
   - 改进建议：定义重试策略或 polling 补偿机制
4. **问题描述**：PrescriptionAssistResponse 缺少 errorCode 字段承载 AI 无可推荐药品的错误码传递
   - 所在位置：§3.4 PrescriptionAssistResponse DTO 定义；§4.4 /assist 主端点场景描述
   - 严重程度：一般
   - 改进建议：在 PrescriptionAssistResponse 中新增 errorCode 顶层字段
5. **问题描述**：AllergyWarningItem.severity 类型和值域未定义
   - 所在位置：§10.4 AllergyWarningItem DTO 定义；§3.4 allergyWarnings 描述
   - 严重程度：一般
   - 改进建议：定义 AllergyWarningSeverity 枚举并更新类型声明
6. **问题描述**：encounterId/visitId 命名映射未显式说明
   - 所在位置：§3.3 RecordGenerateRequest（encounterId）；§3.3 MedicalRecord 实体字段（visitId）
   - 严重程度：轻微
   - 改进建议：在字段说明中标注映射关系或 Service 层转换逻辑
7. **问题描述**：§9.1 科室模板初始数据集为占位符，内容缺失
   - 所在位置：§9.1 初始模板数据集
   - 严重程度：轻微
   - 改进建议：补充 DEFAULT 模板的必填字段列表

## 迭代第 10 轮

1. **问题描述**：CRITICAL 剂量告警在提交流程的阻断链路不完整，且 PrescriptionDraftContext 的 CRITICAL 标记缺乏覆盖/清理语义
   - 所在位置：§3.4 DosageAlertLevel 职责描述、§4.2 处方提交端点行为（第 1/2/3 条）、§3.4 PrescriptionDraftContext 清理时机
   - 严重程度：严重
   - 改进建议：在 §4.2 补充提交前检查 CRITICAL 告警；在 §4.4 check-dose 流程中明确每次重新计算并覆盖标记；补充清理契约
2. **问题描述**：POST /api/prescription/submit 端点缺少 Controller 归属
   - 所在位置：§4.2 处方提交端点行为契约、§2.1 目录结构
   - 严重程度：严重
   - 改进建议：新增 PrescriptionSubmitController 或将 submit 端点归入 PrescriptionAuditController
3. **问题描述**：业务层 DosageAlert 缺少 warningType 字段，导致需求 3.4.10 字段级契约断裂
   - 所在位置：§3.4 DosageAlert 字段定义、§3.4 DosageThresholdService 职责描述、§10.4 ai-api 层 DoseWarningItem.warningType
   - 严重程度：严重
   - 改进建议：DosageAlert 增加 warningType 字段；DosageThresholdService 明确三种输出路径及 warningType 赋值规则；补充 OVER_DURATION 实现说明
4. **问题描述**：AllergyWarningItem 与 AllergyWarning 命名不一致，且业务层 DTO 字段定义缺失
   - 所在位置：§2.1 目录、§3.4 PrescriptionAssistResponse、§10.4
   - 严重程度：一般
   - 改进建议：统一命名，补充业务层 AllergyWarning DTO 完整字段定义
5. **问题描述**：DoseWarning 业务层 DTO 字段定义缺失
   - 所在位置：§3.4 PrescriptionAssistResponse 字段描述、§1.3 核心抽象一览
   - 严重程度：一般
   - 改进建议：补充 DoseWarning DTO 字段定义（drugId、warningType、message、severity）
6. **问题描述**：RecordGenerateRequest 缺少 dialogueText 的 50–10000 字符约束
   - 所在位置：§3.3 RecordGenerateRequest 描述
   - 严重程度：一般
   - 改进建议：追加"(必填，字符数 50–10000)"约束说明
7. **问题描述**：Phase 5 迁移透明性断言缺少条件限定
   - 所在位置：§1.1 设计目标、§10 ai-api 层 DTO 扩展规格、§4.5 Converter 依赖
   - 严重程度：一般
   - 改进建议：将"业务模块代码无须修改"修订为有条件的表述

## 迭代第 11 轮

1. **问题描述**：check-dose 端点请求参数缺少 prescriptionId，无法支撑 PrescriptionDraftContext 写入行为
   - 所在位置：§4.4 "即时校验子端点"段落
   - 严重程度：严重
   - 改进建议：在 check-dose 请求参数中增加 prescriptionId（必填，String），同步更新 §一.3 包E DosageCheckRequest 字段说明；若设计为无状态端点则从行为描述中删除 CRITICAL 写入逻辑
2. **问题描述**：AdditionalResponse 业务层 DTO 缺少字段定义
   - 所在位置：§3.1 DialogueCreateRequest 字段表——additionalResponses
   - 严重程度：严重
   - 改进建议：在 §3.1 或 §1.3 包C 核心抽象中补充 AdditionalResponse 字段定义（question、answer、answeredAt），与 ai-api 层 AdditionalResponseItem 保持映射一致
3. **问题描述**：TriageRule 实体 conditions 字段 JSON 结构未定义
   - 所在位置：§3.1 TriageRule（JPA @Entity）——conditions 字段
   - 严重程度：一般
   - 改进建议：给出 conditions 的 JSON schema 示例（如 List<ConditionItem> 含 keyword、weight、matchType），在 §7 设计决策中补充结构选择理由
4. **问题描述**：全量拼接策略在多轮长对话场景下的 token 超限风险未评估
   - 所在位置：§3.1 TriageService——"AI 调用上下文传递策略"
   - 严重程度：一般
   - 改进建议：补充 token 超限风险评估，选择截断/摘要/标注延期三种策略之一写入设计决策
5. **问题描述**：错误码 RX_ASSIST_AI_SUGGESTION_NOT_FOUND 命名违反自身定义的命名规则
   - 所在位置：§5.1 错误码表——"开方辅助（非AI）"行
   - 严重程度：一般
   - 改进建议：将命名改为 RX_ASSIST_SUGGESTION_NOT_FOUND（去掉 _AI_）并修正引用处，或改移至"开方辅助（AI）"行
6. **问题描述**：全量降级路径中前端无法区分"AI 完全不可用"与"AI 明确返回空"
   - 所在位置：§4.3 病历生成场景——完全降级路径描述；§3.3 RecordGenerateResponse 字段
   - 严重程度：一般
   - 改进建议：在 RecordGenerateResponse 中增加 fallbackReason 可选字段，或定义更细粒度降级错误码

## 迭代第 12 轮

1. **问题描述**：「与前一版一致」文档参照缺陷，设计不可独立使用
   - 所在位置：§8.1、§8.2、§8.3、§8.4、§9.4
   - 严重程度：严重
   - 改进建议：逐项展开定义缺失内容，包括同优先级检测语义、实体关联关系、单位组内换算系数、字段完整定义等
2. **问题描述**：AiService 接口方法缺少正式定义，ai-api 模块契约来源不完整
   - 所在位置：全文（§3.1–§3.4、§4.5、§6.2、§6.3）
   - 严重程度：严重
   - 改进建议：在 §2.2 或新增独立章节中给出 AiService 接口的正式方法签名定义
3. **问题描述**：DosageUnitGroup 缺少组内单位清单和换算系数表
   - 所在位置：§8.3
   - 严重程度：一般
   - 改进建议：补充 DosageUnitGroup 的单位映射表
4. **问题描述**：CRITICAL 剂量告警与 BLOCK 审核阻断的隔离边界缺少一条执行路径说明
   - 所在位置：§4.2 处方提交端点行为
   - 严重程度：一般
   - 改进建议：明确 BLOCK 阻断后 CRITICAL 检查的执行路径
5. **问题描述**：SpecialPopulationDosageRule 未纳入核心抽象一览表
   - 所在位置：§1.3 核心抽象一览表；§3.2 LocalRuleEngine 实现范围表
   - 严重程度：一般
   - 改进建议：在 §1.3 补充 Phase 2/3 覆盖的 5 条本地规则列表
6. **问题描述**：文档缺失 ai-api 层实现前提说明：当前 ai-api DTO 为空壳类的状态与扩展时序
   - 所在位置：§10 ai-api 层 DTO 扩展规格
   - 严重程度：一般
   - 改进建议：补充 ai-api DTO 扩展的完成状态、依赖关系和接口冻结时间点

## 迭代第 13 轮

1. **问题描述**：`@DltHandler` 与 `@TransactionalEventListener` 技术栈不匹配，死信处理方案不可执行，直接指导编码将导致编译错误
   - 所在位置：§2.2 "跨模块事件传递机制"——RegistrationEvent 消费失败补偿策略
   - 严重程度：严重
   - 改进建议：将 `@DltHandler` 替换为 `@Recover`（Spring Retry），在 recover 方法中手动写入 dead_letter_event 表；或引入消息中间件后全面升级事件传递机制
2. **问题描述**：DuplicateCheckRule 依赖的 DrugCompositionDict 缺少成分编码定义，字符串匹配会导致临床漏报和假阳性
   - 所在位置：§3.2 DuplicateCheckRule 逻辑描述 + §2.1 DrugCompositionDict 实体定义
   - 严重程度：严重
   - 改进建议：在 ingredients JSON 中增加 ingredientCode 字段（统一编码），补充检测边界说明
3. **问题描述**：PrescriptionDraftContext TTL 清理机制缺少扫描实现方案，异常退出场景下 CRITICAL 标记会残留至多 60 分钟
   - 所在位置：§3.4 PrescriptionDraftContext 生命周期管理
   - 严重程度：一般
   - 改进建议：补充 ScheduledExecutorService 定期扫描机制，或采用懒清理策略并补充说明
4. **问题描述**：dead_letter_event 表和定时补偿任务的模块归属未定义，影响编码阶段模块划分
   - 所在位置：§2.2 "跨模块事件传递机制"——死信事件表与补偿任务
   - 严重程度：一般
   - 改进建议：补充实体归属（建议 consultation 模块）、补偿任务类路径及 Repository 接口声明
5. **问题描述**：SpecialPopulationDosageRule 年龄阈值硬编码，未暴露为配置参数
   - 所在位置：§3.2 SpecialPopulationDosageRule 描述
   - 严重程度：一般
   - 改进建议：提取为 application.yml 配置项，或在设计决策中显式说明硬编码决策及适用场景
6. **问题描述**：AiResult 超时降级重载的泛型参数与 ai-api DTO 空壳类的时序依赖在接口定义处未标注
   - 所在位置：§2.3 AiService 接口定义（AiResult 超时降级重载）
   - 严重程度：一般
   - 改进建议：在 §2.3 补充约束标记或显式交叉引用 §10 的时序依赖说明
7. **问题描述**：配置变更事件丢失补偿机制在不同实体上覆盖不一致，事件类定义缺失或描述需修正
   - 所在位置：§9.3 规则管理接口描述 + §3.2 数据来源说明
   - 严重程度：一般
   - 改进建议：明确是否需要事件驱动缓存失效，统一修正描述或补全事件类定义
8. **问题描述**：DosageAlertLevel/AlertSeverity/AllergyWarningSeverity 三个枚举命名约定不一致，存在编码阶段类型误用风险
   - 所在位置：§1.3 包E/包D-AI1 枚举条目
   - 严重程度：一般
   - 改进建议：统一枚举值命名风格（WARN→WARNING, HIGH→CRITICAL），调整排序方向，评估是否可复用同一枚举



## 迭代第 14 轮

1. **问题描述**：sessionId 生成责任归属矛盾——§1.3 声称 DialogueSessionManager 统一生成，§3.1 同时出现"统一生成"和"前端生成传入"两种表述
   - 所在位置：§1.3 L53、§3.1 L345、§3.1 L387
   - 严重程度：严重
   - 改进建议：统一表述——要么(a)后端生成，sessionId 改为返回体字段；要么(b)前端生成，删除"DialogueSessionManager 统一生成"描述

2. **问题描述**：Phase 5 迁移"代码无须修改"断言与设计决策矛盾——§1.1 断言"业务模块代码无须修改"，§7 明确指出 Converter 需同步更新
   - 所在位置：§1.1 L9、§7 L977
   - 严重程度：严重
   - 改进建议：修订为有条件表述，明确 Converter 层需随 ai-api DTO 字段变更同步更新

3. **问题描述**：BLOCK 阻断处方的 AuditRecord isLatest 管理缺失——BLOCK 处方的 prescriptionOrderId 为空，无法通过 prescriptionOrderId 找到上一条记录清除 isLatest 标记
   - 所在位置：§3.2 L415–L430、§4.2 L696–L698
   - 严重程度：严重
   - 改进建议：prescriptionOrderId 为空时按 prescriptionId 分组执行相同的 isLatest 清理逻辑

4. **问题描述**：MedicalRecordController 声称支持流式输出与 Phase 2/3 范围矛盾
   - 所在位置：§1.3 L94
   - 严重程度：严重
   - 改进建议：改为"支持非流式输出（Phase 2/3），流式输出预留到 Phase 4"

5. **问题描述**：RX_AUDIT_BLOCK 错误码已定义但无消费路径
   - 所在位置：§5.1 L862
   - 严重程度：一般
   - 改进建议：从错误码表删除 RX_AUDIT_BLOCK 或将其加入 BlockResponse 字段定义

6. **问题描述**：DrugInteractionPairChangeEvent 在 Phase 2/3 范围内无实际意义
   - 所在位置：§9.3 L1078
   - 严重程度：一般
   - 改进建议：删除相关引用，标注 DrugInteractionPair 相关接口为 Phase 4

7. **问题描述**：AllergyWarningSeverity 枚举值排序与其他同类枚举不一致
   - 所在位置：§1.3 L119、§3.4 L588–L589
   - 严重程度：一般
   - 改进建议：调整为 INFO/WARNING/CRITICAL 顺序

8. **问题描述**：forceSubmit=false 时 WARN 级处方的提交流程存在循环重审风险
   - 所在位置：§4.2 L689
   - 严重程度：一般
   - 改进建议：补充已存在 WARN 审核结果且处方未变更时的跳过机制或最大重审次数

9. **问题描述**：§1.3 DialogueCreateRequest 的 AdditionalResponse 引用缺少字段定义说明
   - 所在位置：§1.3 L61
   - 严重程度：轻微
    - 改进建议：将 AdditionalResponse 条目提前或增加交叉引用

## 迭代第 15 轮

1. **问题描述**：DosageStandard 五级匹配策略存在未覆盖路径——当一条记录同时设置 ageRange 和 weightRange 且患者条件不满足精确匹配要求时，该记录被跳过至 Level 5 报"标准不存在"，导致临床正确剂量规则被错误跳过
   - 所在位置：§8.4 五级匹配优先级
   - 严重程度：严重
   - 改进建议：补充 Level 2.5 或重新定义匹配优先级，或采用评分制，或补充文字说明 ageRange 和 weightRange 同时非 null 时的处理方式

2. **问题描述**：DoctorFacade 跨模块同步调用缺失降级保护——DoctorFacade 作为强制同步调用路径，超时阈值、失败处理、服务不可用时的替代策略均未定义，与同一模块中 RegistrationEvent 消费端完整的四层保护机制形成实质性设计不对称
   - 所在位置：§3.1 TriageService 协作描述 + §4.1 推荐医生列表生成
   - 严重程度：严重
   - 改进建议：补充超时配置（建议默认 2s）、调用失败时将 TriageResponse.doctors 置空列表并说明原因、补充日志级别和可观测性标记

3. **问题描述**：MissingFieldDetector 的 null/空字符串判定语义未定义——AI 返回空字符串、字段缺失、显式 null 三种情况是否视为"缺失"未明确，差集比对基于 keySet 存在性还是值非空非 null 未定义
   - 所在位置：§3.3 MissingFieldDetector 职责 + §4.3 病历生成场景 + §10.3 MedicalRecordGenResponse
   - 严重程度：一般
   - 改进建议：明确判定策略为"基于字段值的非空非 null 存在性"——不存在、值为 null、值为空字符串均视为缺失

4. **问题描述**：多轮分诊中 DialogueSession 不支持主诉修正——DialogueSession 首轮固定记录 chiefComplaint，患者修正主诉后无法更新，全量拼接时首轮主诉保持原样导致 AI 收到矛盾上下文
   - 所在位置：§3.1 DialogueSession 职责 + 全量拼接策略
   - 严重程度：一般
   - 改进建议：增加 correctedChiefComplaint 可选字段，或在全量拼接策略中说明由 AI 自身处理修正语义

5. **问题描述**：forceSubmit=false 路径缺少对处方已修改的感知——forceSubmit=false 时直接返回当前 WARN 审核结果，但若处方已在 WARN 审核后发生了修改，返回的仍是基于旧处方的审核结果
   - 所在位置：§4.2 处方提交端点行为
   - 严重程度：一般
   - 改进建议：在 forceSubmit=false 路径中增加轻量级内容变更检测（结构化 MD5/哈希比对），发现不一致时提示"处方内容已变更，请重新审核"

6. **问题描述**：CRITICAL 告警前端同步更新的竞态窗口——PrescriptionDraftContext 全量覆盖语义下，两次 check-dose 之间 CRITICAL 告警的清除与前端展示存在时序窗口，前后端状态不一致
    - 所在位置：§3.4 PrescriptionDraftContext 覆盖更新行为 + §4.2 CRITICAL 阻断合并判定
    - 严重程度：一般
    - 改进建议：在 §4.4 check-dose 流程中补充 PrescriptionDraftContext 状态变更通知机制，每次响应携带 `contextCriticalCount` 字段供前端判断是否需要同步刷新

## 迭代第 16 轮

1. **问题描述**：correctedChiefComplaint 传递路径未闭环，DialogueCreateRequest 中未定义该字段，缺少 AI 隐式识别判定规则
   - 所在位置：§3.1 DialogueSession 字段定义、§3.1 TriageService "AI 调用上下文传递策略"
   - 严重程度：严重
   - 改进建议：在 DialogueCreateRequest 中增加 correctedChiefComplaint 可选字段，或明确定义 TriageServiceImpl 的 AI 回复检测规则
2. **问题描述**：TriageRecord.finalDepartmentId 补充写入未覆盖手动选科场景，降级路径下 finalDepartmentId 将永久为 null
   - 所在位置：§3.1 TriageRecord 写入时机描述、§2.2 RegistrationEvent 消费机制、§4.1 降级时前端行为指引
   - 严重程度：严重
   - 改进建议：增加 POST /api/triage/select-department 端点或扩展 TriageService 补充手动选科的 finalDepartmentId 写入方法
3. **问题描述**：提交端点阻断判定时序未定义，forceSubmit=true 与 CRITICAL 阻断的交互关系不清晰
   - 所在位置：§4.2 处方提交端点行为三步描述
   - 严重程度：严重
   - 改进建议：明确定义执行顺序，推荐 CRITICAL 阻断检查作为步①优先于 forceSubmit 判定
4. **问题描述**：AiResult.failure()/degraded() 重载方法缺乏实现归属，实现者无法定位代码位置
   - 所在位置：§2.3 AiService 接口定义、§7 设计决策、§2.1 目录结构
   - 严重程度：严重
   - 改进建议：在 §2.1 目录结构中补充 AiResult 类的归属位置，或在 §2.3 首段补充其包路径和方法签名
5. **问题描述**：PrescriptionDraftContext 与 AiSuggestionResult TTL 不一致导致状态残差，30 分钟后出现"任务已过期但阻断标记仍生效"的矛盾
   - 所在位置：§3.4 PrescriptionDraftContext 生命周期管理、§3.4 AiSuggestionResult、§4.4 check-dose 流程
   - 严重程度：一般
   - 改进建议：将 AiSuggestionResult 的 TTL 调整为 60 分钟以保持一致，或说明不对称性的业务合理性
6. **问题描述**：contextCriticalCount 前端消费行为未定义，前端开发者无法确定何时执行何种 UI 操作
   - 所在位置：§4.4 check-dose 响应、§3.4 PrescriptionDraftContext "前端同步协商机制"
   - 严重程度：一般
    - 改进建议：补充 contextCriticalCount 变化时前端推荐行为，如 N→0 时恢复提交按钮，0→N 时禁用按钮并展示阻断原因

## 迭代第 17 轮

1. **问题描述**：AiResult 字段定义与设计决策自相矛盾——§2.3 声称含 partialData 六字段，§7 决议不新增 partialData 字段
   - 所在位置：§2.3（第285行附近）、§7 设计决策（第1014行附近）
   - 严重程度：严重
   - 改进建议：统一表述，推荐删除 §2.3 中 partialData 字段引用，改为五字段描述
2. **问题描述**：chiefComplaint 与 additionalResponses 互斥违规的处理未定义——§1.3/§3.1/§4.1 声明互斥规则但未定义违规时的后端行为
   - 所在位置：§1.3 DialogueCreateRequest 字段说明、§3.1 互斥语义段落、§4.1 API 契约行
   - 严重程度：一般
   - 改进建议：在 §3.1 或 §5.1 补充校验违规错误码定义（如 `TRIAGE_FIELD_COMBINATION_INVALID`），后端返回 400 + 该错误码
3. **问题描述**：PrescriptionAssistRequest 未列入 §1.3 包E 核心抽象一览表，对比包D-AI1 的 AuditRequest 已列入
   - 所在位置：§1.3 包E 核心抽象一览表
   - 严重程度：一般
   - 改进建议：在 §1.3 包E 核心抽象表新增 PrescriptionAssistRequest 条目

## 迭代第 18 轮

1. **问题描述**：§2.3 "AiResult<T> 泛型要点"段落将 partialData 列为 AiResult "统一包含"的 6 项内容之一，但 §2.3 首段和 §7 设计决策已明确 AiResult 仅含 5 字段，partialData 作为重载工厂方法入参传入并写入 data 字段而非 AiResult 类的独立属性。两处表述矛盾未完全消除。
   - 所在位置：§2.3 AiService 接口定义，AiResult<T> 泛型要点段落
   - 严重程度：一般
   - 改进建议：将该段落中 "partialData（T，可选，通过 failure()/degraded() 重载传入超时/降级部分结果）" 从字段列表中移除，改为说明性表述如 "超时/降级场景下 partialData 通过重载工厂方法 failure(String errorCode, T partialData)/degraded(String fallbackReason, T partialData) 入参传入并写入 data 字段"。
