# Phase 2/3 包C/D-AI1/D-AI2/E — 架构级 OOD 设计方案

## 1. 概述

### 1.1 设计目标

本设计覆盖 Phase 2 包C（智能分诊）与 Phase 3 包D-AI1（处方审核）、包D-AI2（病历生成）、包E（辅助开方）四个业务包。核心目标如下：

- **底座直接落地**：四个包均直接以 Maven 模块形式落地在 AIMedical 后端底座上，遵循 common → modules → application 的分层架构，规避后续迁移至 Phase 5 AI 进阶底座的重构成本。业务模块仅依赖 ai-api 的 AiService 接口（编译期依赖）；若 Phase 5 保持 AiService 接口签名不变，业务模块的核心 Service 和 Controller 的业务逻辑代码无须修改；但内存存储依赖部分（DialogueSessionManager 会话存储、AiSuggestionResult 存储、PrescriptionDraftContext）需配合分布式缓存迁移，必须增加 Store 抽象层（SessionStore、SuggestionStore、DraftContextStore 接口）以隔离存储实现，Phase 2/3 使用 ConcurrentHashMapStore 实现，Phase 5 替换为 RedisStore 实现；Converter 层需随 ai-api DTO 字段变更同步更新
- **架构风格一致**：四个包的模块结构、依赖方向和抽象层次与 Phase 0（骨架模块）、Phase 1（认证模块）的风格保持一致
- **强耦合同步落地**：包E（辅助开方）与包D-AI1（处方审核）共享处方领域数据和业务规则，设计为同一模块内的两个子域，同步开发、同步发布
- **AI 能力集成标准化**：四个包的业务逻辑均通过 ai-api 中的 AiService 接口调用 AI 能力，隔离 AI 实现细节；AI 不可用时的降级路径由各自模块的本地规则兜底，与 AiService 的降级框架协同
- **需求文档契约对齐**：所有模块的 API 输入/输出契约与需求文档 §3.4.x 定义的字段级契约严格对齐，确保验收可追溯
- **ai-api 层 DTO 与业务层 DTO 的契约完整性**：ai-api 层 DTO 承载 AI 推理所需的完整输入/输出语义，业务层 DTO 面向前端 API 契约，二者通过各模块 Converter 类完成映射/转换，确保字段级对齐

### 1.1a 外部依赖与前提条件

本设计依赖以下当前代码库中不存在或未在本设计范围内实现的外部模块：

| 外部模块 | 依赖内容 | 提供的接口/事件 | 实施时间线要求 | 风险说明 |
|---------|---------|---------------|--------------|---------|
| registration 模块 | RegistrationEvent 的发布能力 + 接收前端传递的 sessionId | 发布 RegistrationEvent（定义在 common-module-api），前端在分诊结束后保留 sessionId，进入挂号界面时将 sessionId 作为请求参数传入 registration 模块，registration 模块接收后填充至事件 | 与 consultation 模块同步开发，最迟在挂号功能上线前完成 | 前端未传递 sessionId 时事件中 sessionId 为空，事件-driven finalDepartmentId 写入路径降级为按 patientId 关联最近分诊记录，不影响手动选科路径 |
| visit 模块 | VisitFacade 接口的实现（含超时与降级） | 实现 VisitFacade（定义在 common-module-api/visit/），提供 findVisitIdByEncounterId(encounterId) 等方法；超时阈值默认 2s，调用失败时 MedicalRecordService 执行降级：尝试将 encounterId 直接作为 visitId 的 fallback 写入，若不可行则返回 MR_GEN_VISIT_NOT_FOUND 错误码 + 已生成的部分病历内容 | 与 medical-record 模块同步开发，最迟在病历生成功能上线前完成 | VisitFacade 不可用/超时时以降级模式返回部分病历内容 + MR_GEN_VISIT_NOT_FOUND 错误码，不影响病历已生成字段的返回 |

| admin 模块 | 规则管理接口（TriageRule、DrugContraindicationMapping、DrugAllergyMapping、DrugCompositionDict 的 CRUD 接口及变更事件发布） | CRUD API + ApplicationEventPublisher 事件发布 | 与 prescription 模块同步开发，最迟在处方审核功能上线前完成 | 若 admin 模块 OOD 滞后导致规则管理接口未就绪，prescription 模块的本地规则引擎需通过种子脚本初始化运行，上线后切换为管理接口管理；TriageRule 的分诊规则同理 |

**时间线约束**：三个外部依赖模块需在本设计对应的四个业务模块上线前完成接口实现，否则对应功能需以降级模式上线（如挂号事件缺失时降级为仅支持手动选科，病历生成缺失 visitId 时以降级模式返回部分病历内容 + 错误码，规则管理接口缺失时降级使用种子脚本初始化）。

### 1.1b 与已有 OOD 草案的继承关系

本设计继承自 Phase 23 已有 OOD 草案（`Harness/redeliberations/202606281422_phase23_ood/a_v19_copy_from_v18.md`），在此基础之上经历 23 轮（v8→v23）定向修订。继承时核心架构决策保持一致：三模块（consultation/prescription/medical-record）划分、扁平 Maven 模块结构、包D-AI1 与包E 共享 prescription 模块的强耦合策略、ai-api 接口调用模式、备忘录模式的分诊对话状态管理、本地规则链降级模式。主要演进方向包括：Store 抽象层从"建议"升级为"强制"、ai-api 层 DTO 扩展规格补充、并发安全策略强化、ApiResult 超时降级模式收敛、错误码命名规范统一以及审计/阻断链路的完整性闭环。与草案存在差异的设计决策已在 §7 设计决策表中标注。

### 1.2 整体架构思路

```
application（启动聚合层）
  │
  ├── modules/consultation/       包C 智能分诊
  │     └── 依赖: common, common-module-api, ai-api
  │
  ├── modules/prescription/       包D-AI1 处方审核 + 包E 辅助开方
  │     └── 依赖: common, common-module-api, ai-api
  │
  ├── modules/medical-record/     包D-AI2 病历生成
  │     └── 依赖: common, common-module-api, ai-api
  │
  ├── modules/ai/ai-impl          AI 实现（含 Mock/降级/底座管线）
  ├── modules/common-module/      公共业务模块（认证/权限/用户）
  ├── modules/patient/            患者模块
  ├── modules/doctor/             医生模块
  ├── modules/admin/              管理员模块
  └── common/                     共享基类、契约、跨模块领域实体
```

三个新模块均为扁平 Maven 模块（不拆分为 api/impl 子模块），与 patient/doctor/admin 的结构一致。每个模块内部按职责分包：api、service、repository、entity、dto、converter。各模块依赖 common、common-module-api 和 ai-api，不依赖其他业务模块的 impl 层。

DosageStandard 实体迁移至 common 模块（common/entity/DosageStandard.java），使 prescription 模块和 admin 模块均可引用而不产生跨模块编译期依赖。prescription 模块通过仅查询 Repository 访问，admin 模块通过管理端 Service 执行写入。

RegistrationEvent 事件契约定义在 common-module-api 中（com.aimedical.modules.commonmodule.event.RegistrationEvent），供 consultation 模块消费、registration 模块发布，application 模块聚合后跨模块传播。

### 1.3 核心抽象一览

#### 包C（智能分诊）

| 抽象 | 类型形态 | 职责定位 |
|------|---------|---------|
| TriageController | class | 分诊对话 REST 端点，接收前端提交的主诉/追问答复，返回推荐科室与推荐医生列表；同时提供手动选科写入端点 POST /api/triage/select-department，供前端在降级场景或患者主动选择科室时写入最终选择结果 |
| TriageService | interface | 智能分诊业务契约，封装单轮/多轮对话的分诊逻辑、对话状态管理和规则回退；补充手动选科写入方法 selectDepartment(sessionId, departmentId, departmentName)，供手动选科端点调用或 RegistrationEvent 消费路径复用 |
| DialogueSession | class | 多轮对话会话状态对象，维护当前 session 的对话上下文、已收集的症状信息、当前轮次、规则版本与规则集快照、AI 连续失败计数 |
| DialogueSessionManager | class | 对话会话生命周期管理器，负责 session 的创建、查找、更新和过期清理，承担并发访问控制职责；内部依赖 SessionStore 接口进行存储，Phase 2/3 使用 ConcurrentHashMapStore 实现，Phase 5 替换为 RedisStore；接受前端传入的 UUID v4 并验证格式，首次使用时创建会话 |
| TriageRuleEngine | interface | 分诊规则引擎契约，根据症状匹配规则返回推荐科室；支持可配置规则源 |
| DepartmentFallbackProvider | interface | 兜底科室列表提供者契约，当 AI 无结果且规则匹配也无结果时返回静态兜底科室列表或基于简单规则的匹配结果 |
| DoctorFacade | interface | 跨模块医生信息查询门面，定义在 common-module-api 中，供 consultation 模块查询推荐医生列表和医生排班信息 |
| TriageResponse | class（DTO） | 分诊响应值对象，对齐需求文档 3.4.1 输出契约。包含推荐科室列表（departments，List\<RecommendedDepartment\>，每项含 departmentId / departmentName / score，0–3 项）、推荐医生列表（doctors，List\<RecommendedDoctor\>，每项含 doctorId / doctorName / departmentId / availableSlotCount / score，0–5 项）、推荐理由（reason，必填，字符数 ≥ 1，AI 无法生成时输出默认文案如"根据症状分析，建议就诊XX科室"，不可为空字符串或 null）、匹配规则列表（matchedRules，List\<MatchedRule\>，每项含 ruleId / ruleName / score）、会话标识（sessionId，多轮场景）、是否需要追问标记（needFollowUp）及追问内容（followUpQuestion）、置信度 confidence（可选）、降级标记（degraded，对齐需求文档 3.4.1 degraded 输出字段）、兜底提示（fallbackHint，AI 连续失败 3 次时携带"建议直接联系线下接诊窗口"文案）、规则版本不匹配标记（ruleVersionMismatch，可选，规则快照失效命中时为 true） |
| RecommendedDepartment | class（DTO） | 推荐科室值对象，对齐需求文档 3.4.1 recommended_departments 每项结构。包含 departmentId、departmentName、score |
| RecommendedDoctor | class（DTO） | 推荐医生值对象，对齐需求文档 3.4.1 recommended_doctors 每项结构。包含 doctorId、doctorName、departmentId、availableSlotCount（可预约时段数）、score（匹配评分）。推荐医生列表由后端根据 AI 推荐科室从 DoctorFacade 查询排班数据生成——AI 返回推荐科室后，TriageServiceImpl 对每个推荐科室调用 DoctorFacade 查询有排班的医生列表，按匹配评分排序取前 5 名，availableSlotCount 由 DoctorFacade 实时返回 |
| MatchedRule | class（DTO） | 匹配规则值对象，对齐需求文档 3.4.1 matched_rules 数组每项结构。包含 ruleId、ruleName、score |
| AdditionalResponse | class（DTO） | 追问回答值对象，对齐需求文档 3.4.1 additional_responses 数组每项结构。包含 question（string，追问问题）、answer（string，患者回答）、answeredAt（string，ISO 日期时间，可选） |
| DialogueCreateRequest | class（DTO） | 分诊对话创建请求值对象，对齐需求文档 3.4.1 输入契约。包含患者主诉（chiefComplaint，必填，字符数 5–500，对齐需求文档 3.4.1 约束）、患者基本信息（patientId、age、gender）、会话标识（sessionId，必填——首轮请求由前端生成 UUID v4 传入，消除"首轮为空"特殊分支，对齐需求文档 3.4.1 session_id 必填语义）、规则版本号（ruleVersion，可选，对齐需求文档 3.4.1 rule_version）、规则集标识（ruleSetId，可选，对齐需求文档 3.4.1 rule_set_id；与 ruleVersion 语义不同——ruleVersion 标识规则版本号，ruleSetId 标识规则集实体标识）、追问回答数组（additionalResponses，可选，List\<AdditionalResponse\>，对齐需求文档 3.4.1 additional_responses；与 chiefComplaint 互斥——首轮请求仅传 chiefComplaint，后续多轮追问仅传 additionalResponses + sessionId，不同时提供）、主诉修正（correctedChiefComplaint，可选，String——主诉修正显式路径，前端在后续追问轮次中传此字段触发主诉修正，TriageServiceImpl 优先采用此值覆盖主诉意图；与 additionalResponses 兼容共存——同一请求中可同时携带 correctedChiefComplaint 和 additionalResponses） |

#### 包D-AI1（处方审核）

| 抽象 | 类型形态 | 职责定位 |
|------|---------|---------|
| PrescriptionAuditController | class | 处方审核 REST 端点，接收待审核处方，返回审核结果与风险等级；在收到 BLOCK 结果时拒绝处方提交，通过 HTTP 422 返回阻断详情 |
| PrescriptionAuditService | interface | 处方审核业务契约，协调 AI 审核调用与本地规则回退；审计结果的 BLOCK 等级具有强制阻断语义，调用方需据此执行阻断决策 |
| AuditRiskLevel | enum | 风险等级枚举，定义 PASS（通过）、WARN（警告）、BLOCK（阻断）三个级别；BLOCK 级别在后端具有强制阻断语义 |
| AuditRecord | JPA @Entity | 审核记录持久化实体，含 auditId（Long，@Id @GeneratedValue）自增主键；保存每次审核的原始处方快照（JSON 文本）、AI 结果、风险等级和时间戳，以及处方单号、医生 ID、患者 ID 等业务关联标识；含强制提交留痕字段和审核次序管理字段；撤销审核时该 AuditRecord 的 isLatest 回退为 false，确保后续查询最新审核结果不会返回已撤销的记录 |
| LocalRuleEngine | interface | 本地规则校验引擎契约，封装 5 条运行时独立规则 + 1 条预留骨架（DrugInteractionRule，Phase 2/3 不启用）——AllergyCheckRule（过敏检查）、ContraindicationCheckRule（合并症禁忌检查）、DuplicateCheckRule（重复用药检查）、DosageLimitRule（剂量范围检查）、SpecialPopulationDosageRule（特殊人群剂量检查）；AI 超时或不可用时作为降级回退。各规则独立实现/测试/启用/禁用，具体职责见 §3.2 实现范围表 |
| PrescriptionAuditEnforcer | interface | 阻断执行策略契约，定义 BLOCK 结果的强制阻断行为；默认实现返回阻断错误码和阻断原因集合，供 Controller 层组装响应 |
| AuditRequest | class（DTO） | 处方审核请求值对象，对齐需求文档 3.4.2 输入契约。包含处方药品列表（prescriptionItems，每项含 drugId/drugName/dose/frequency/duration/route 六个字段）、患者个体信息（patientInfo，含 patientId/age/gender/allergyHistory（string，过敏史文本）/allergyDetails（List\<AllergyDetail\>，可选扩展容器）/comorbidities（List\<String\>，合并症列表））、处方标识（prescriptionId，必填） |
| AllergyDetail | class（DTO） | 结构化过敏信息值对象，对齐需求文档 3.1.6 过渡期 allergy_details 扩展容器每项结构。包含 allergen（string，必填）、reactionType（string，可选）、severity（AllergySeverity 枚举：MILD/MODERATE/SEVERE，可选）、occurredAt（string，ISO 日期，可选） |
| AuditResponse | class（DTO） | 处方审核响应值对象，对齐需求文档 3.4.2 输出契约。包含风险等级（riskLevel，AuditRiskLevel）、风险提示列表（alerts，List\<AuditAlert\>）、药物相互作用结果（interactions，List\<DrugInteraction\>）、用药建议（suggestions，List\<Suggestion\>）、是否降级标记（fromFallback） |
| AuditAlert | class（DTO） | 风险提示值对象，对齐需求文档 3.4.2 alerts 数组每项结构。包含 alertCode、alertMessage、severity（AlertSeverity 枚举：INFO / WARNING / CRITICAL，与 AuditRiskLevel 是不同维度——AuditRiskLevel 表征处方整体风险等级，AlertSeverity 表征单条提示的严重程度） |
| AlertSeverity | enum | 风险提示严重程度枚举，定义 INFO / WARNING / CRITICAL 三个级别。与 AuditRiskLevel 是不同维度——AuditRiskLevel 表征处方整体风险等级，AlertSeverity 表征单条提示的严重程度 |
| DrugInteraction | class（DTO） | 药物相互作用结果值对象，对齐需求文档 3.4.2 interactions 数组每项结构。包含 drugPair、severity、description |
| Suggestion | class（DTO） | 用药建议值对象，对齐需求文档 3.4.2 suggestions 数组每项结构。包含 suggestionCode、suggestionText |
| AuditIssue | class（DTO） | 审核问题条目值对象，设计内部结构，供 AuditRecord 持久化和后端阻断逻辑消费。包含 fieldName、issueDescription、ruleId、severity。与 AuditAlert 的映射关系：alertCode ← ruleId + issueDescription 的编码化表达；alertMessage ← issueDescription；severity 共用 |
| BlockResponse | class（DTO） | 阻断响应值对象，包含阻断原因列表、阻断码、阻断时间 |
| ContraindicationCheckRule | class | 合并症-药品禁忌检查规则，遍历处方药品列表，对每个药品查询其禁忌症列表（DrugContraindicationMapping），与患者合并症做交集比对；命中时按禁忌级别分级输出 LocalRuleResult |
| DuplicateCheckRule | class | 重复用药检查规则，遍历处方药品列表，对每个药品查询 DrugCompositionDict 获取成分列表，构建 drugCode → ingredientSet 映射后检测成分交集；存在两种及以上药品共享相同成分时输出 WARN 级别 LocalRuleResult |
| DosageLimitRule | class | 剂量范围检查规则，按药品编码、给药途径、年龄范围、体重范围维度匹配 DosageStandard，检查剂量是否超限；超过剂量上限 2 倍及以上时输出 BLOCK 级别 LocalRuleResult，超过上限但未达 2 倍时输出 WARN 级别 |
| SpecialPopulationDosageRule | class | 特殊人群剂量检查规则，根据患者年龄判断是否属于特殊人群（儿童 ≤ 14 岁、老年 ≥ 65 岁），若是则查询 DosageStandard 中对应年龄/体重分级的剂量标准，与处方中实际剂量做比对；超出该人群特殊剂量上限时输出 BLOCK 级别 LocalRuleResult |
| DrugContraindicationMapping | JPA @Entity | 药品禁忌症映射数据实体，drugCode 主键，关联禁忌症列表（contraindications，JSON TEXT——每项含 diseaseName / level / description）；由管理员维护，种子脚本初始化 |
| LocalRuleResult | class（DTO） | 本地规则校验结果值对象，包含规则标识（ruleId）、是否通过（passed）、消息文本（message）、严重程度（severity，类型为 AuditRiskLevel 枚举——值域为 WARN 或 BLOCK，PASS 级别的 LocalRuleResult 由 passed=true 隐含表达，passed=false 时 severity 必为 WARN 或 BLOCK） |

#### 包D-AI2（病历生成）

| 抽象 | 类型形态 | 职责定位 |
|------|---------|---------|
| MedicalRecordController | class | 病历生成 REST 端点，接收医生端对话文本，返回结构化病历；支持非流式输出（Phase 2/3），流式输出预留到 Phase 4 以实现 |
| MedicalRecordService | interface | 病历生成业务契约，协调 AI 结构化输出与科室模板配置；支持流式与非流式两种生成模式 |
| DepartmentTemplateConfig | class | 科室模板配置值对象，按科室标识管理病历生成规则、模板版本以及每字段的提示模板；每个 MedicalRecordField 可选包含 promptMessage 和 suggestedAction 字段，用于 MissingFieldDetector 组装 FieldMissingHint |
| TemplateConfigManager | interface | 科室模板配置管理器契约，支持模板的运行时查询、缓存和热加载；科室标识不存在时兜底返回 DEFAULT 模板 |
| MissingFieldDetector | interface | 关键字段缺失检测器契约，基于科室模板的必填字段列表与 AI 输出实际包含字段进行差集比对，输出缺失字段集合；检测到缺失后从 DepartmentTemplateConfig 读取对应字段的预定义提示模板（promptMessage + suggestedAction），组装为 FieldMissingHint 列表返回；不修改 AI 产出 |
| FieldMissingHint | class（DTO） | 字段缺失提示值对象，包含缺失字段标识（missingField，MedicalRecordField 枚举）、提示消息（promptMessage，来源于 DepartmentTemplateConfig 中对应字段的预定义提示文案）、建议操作（suggestedAction，来源于 DepartmentTemplateConfig 中对应字段的预定义建议操作模板） |
| RecordGenerateRequest | class（DTO） | 病历生成请求值对象，对齐需求文档 3.4.3 输入契约。包含对话文本（dialogueText）、患者 ID（patientId）、就诊标识（encounterId，映射为 MedicalRecord.visitId——后端 Service 层通过 VisitFacade（common-module-api 中定义的门面接口，由就诊模块实现）调用 findVisitIdByEncounterId(encounterId) 完成转换）、流式输出标记（stream，bool，可选，默认 false；Phase 2/3 仅支持非流式模式，stream=true 时返回 MR_GEN_STREAM_NOT_SUPPORTED 错误码） |
| RecordGenerateResponse | class（DTO） | 病历生成响应值对象，对齐需求文档 3.4.3 输出契约。包含结构化病历内容（fields，以 MedicalRecordField 为键的键值映射）、缺失字段提示列表（missingFieldHints，List\<FieldMissingHint\>）、生成状态标记（降级标记 fromFallback；同时增加 degraded 布尔字段，true 表示 AI 完全不可用/降级，false 表示 AI 明确返回空结果——前端据此区分展示：degraded=true 时显示降级提示"AI 病历生成暂时不可用"，degraded=false 时显示字段补全提示） |

#### 包E（辅助开方）

| 抽象 | 类型形态 | 职责定位 |
|------|---------|---------|
| PrescriptionAssistController | class | 辅助开方 REST 端点；提供两个主端点：(1) POST /api/prescription/assist 作为 3.4.10 AI 辅助开方主端点；(2) POST /api/prescription/assist/check-dose 用于剂量阈值即时校验；以及异步 AI 建议查询端点 |
| PrescriptionAssistService | interface | 辅助开方业务契约，覆盖两个子域：(a) AI 辅助开方完整流程——接收诊断结论/检查检验结果/患者信息/已有处方，委托 AiService.prescriptionAssist() 生成处方草案；包括剂量告警和过敏告警的本地即时校验作为 AI 调用的前置/补充；(b) 剂量阈值即时校验——委托 DosageThresholdService |
| DosageThresholdService | class | 剂量阈值校验服务，按药品编码、给药途径、年龄范围、体重范围维度匹配剂量标准，检查剂量是否超限；支持日剂量校验和疗程剂量校验。输出三种告警类型：单次剂量超限→OVER_SINGLE_DOSE、日剂量超限→OVER_DAILY_DOSE、疗程剂量超限→OVER_DURATION（Phase 2/3 预留，暂不实现） |
| DosageAlert | class | 剂量告警值对象，封装告警级别（alertLevel，DosageAlertLevel 枚举）、告警类型（warningType，DoseWarningType 枚举：OVER_SINGLE_DOSE/OVER_DAILY_DOSE/OVER_DURATION）、告警消息、药品编码、当前剂量、建议值、错误码（errorCode，String，可选——承载剂量校验相关错误码，如 RX_ASSIST_DOSE_STANDARD_NOT_FOUND） |
| DosageAlertLevel | enum | 剂量告警级别枚举，定义 INFO/WARNING/CRITICAL 三个级别；CRITICAL 写入处方草稿上下文，供处方提交时 BLOCK 判定消费 |
| DoseWarningType | enum | 剂量告警类型枚举，定义 OVER_SINGLE_DOSE（单次剂量超限）/ OVER_DAILY_DOSE（日剂量超限）/ OVER_DURATION（疗程剂量超限）三个值，对齐需求文档 3.4.10 warning_type 字段契约 |
| AiSuggestionResult | class | 异步 AI 结果值对象，封装 taskId、suggestion（String，COMPLETED 时填充）、status（AiSuggestionStatus 枚举：PENDING / COMPLETED / FAILED）、createTime、failReason（可选）、partialData（可选，AI 超时或部分生成时携带部分结果）；AiSuggestionResult 实例由 SuggestionStore 接口存储（Phase 2/3 ConcurrentHashMapStore 实现，Phase 5 RedisStore），通过 compute() 原子替换整个实例保证并发安全，同一 taskId 的状态更新保证幂等 |
| PrescriptionAssistRequest | class（DTO） | AI 辅助开方主端点请求值对象，对齐需求文档 3.4.10 输入契约。包含 diagnosis/examResults/patientInfo/existingPrescription/encounterId |
| PrescriptionAssistResponse | class（DTO） | AI 辅助开方主端点响应值对象，对齐需求文档 3.4.10 输出契约。包含处方草案（prescriptionDraft，含 drugs 数组，每项含 drugId/drugName/dose/frequency/duration/route）、剂量告警列表（doseWarnings，List\<DoseWarning\>，每项含 drugId/warningType/message/severity）、过敏冲突告警（allergyWarnings，List\<AllergyWarningItem\>，每项含 drugId/allergen/severity）、顶层错误码（errorCode，可选，String——AI 返回无可推荐药品时填充 RX_ASSIST_AI_NO_RECOMMENDATION）、免责声明标记（disclaimerRequired） |
| DosageCheckRequest | class（DTO） | 剂量阈值即时校验请求值对象，对齐需求文档 3.4.10 check-dose 输入契约。包含处方标识（prescriptionId，主路径必填——空值时由后端自动生成并回写，String——作为 PrescriptionDraftContext 写入键；**分配时机**：前端预创建为主路径——患者进入处方编辑界面时前端生成 UUID v4 传入所有 check-dose 调用；后端按需生成为 fallback——首次 check-dose 调用时 prescriptionId 为空时后端自动生成并通过 DosageCheckResponse.prescriptionId 回写）、药品编码（drugCode）、剂量（dosage）、单位（unit）、给药途径（routeOfAdministration）、患者年龄（patientAge，可选）、患者体重（patientWeight，可选）、用药频率（frequency，可选，用于日剂量校验） |
| DosageCheckResponse | class（DTO） | 剂量阈值即时校验响应值对象，包含告警列表（alerts，List\<DosageAlert\>，每项含 warningType/alertLevel/message/drugCode/currentDose/suggestedValue/errorCode）和异步任务标识（taskId） |
| DoseWarning | class（DTO） | 剂量告警值对象，对齐需求文档 3.4.10 dose_warnings 数组每项结构。包含 drugId（药品编码）、warningType（DoseWarningType 枚举：OVER_SINGLE_DOSE/OVER_DAILY_DOSE/OVER_DURATION）、message（告警消息）、severity（DosageAlertLevel 枚举） |
| AllergyWarningItem | class（DTO） | 过敏冲突告警值对象，对齐需求文档 3.4.10 allergy_warnings 数组每项结构。包含 drugId（药品编码）、allergen（过敏原）、severity（AllergyWarningSeverity 枚举：INFO/WARNING/HIGH） |

#### 跨模块门面与事件

| 抽象 | 类型形态 | 职责定位 |
|------|---------|---------|
| DoctorFacade | interface | 跨模块医生信息查询门面，定义在 common-module-api 中，供 consultation 模块查询推荐医生列表和医生排班信息 |
| DrugFacade | interface | 跨模块药品信息查询门面，定义在 common-module-api 中（com.aimedical.modules.commonmodule.drug.DrugFacade），提供 findByDrugCode(drugCode) 等方法，供 prescription 模块查询药品名称/规格信息；与 DoctorFacade 一致的跨模块门面模式 |
| RegistrationEvent | class（事件） | 挂号事件契约，定义在 common-module-api 中。包含 registrationId、patientId、sessionId（可选，String——由前端在分诊结束后保留并传入挂号流程，registration 模块接收后填充至事件，供 RegistrationEventListener 通过 sessionId 精确关联 TriageRecord）、departmentId、doctorId、eventTime。事件发布端为 registration 模块，消费端为 consultation 模块，在 application 模块聚合后跨模块传播 |

---

## 2. 模块划分

### 2.1 目录结构

```
backend/modules/
├── consultation/                            # 包C 智能分诊
│   └── src/main/java/com/aimedical/modules/consultation/
│       ├── api/TriageController.java          # 含 POST /api/triage/consult + POST /api/triage/select-department
│       ├── dto/TriageResponse.java, RecommendedDepartment.java, RecommendedDoctor.java,
│       │       MatchedRule.java, DialogueCreateRequest.java, AdditionalResponse.java
│       ├── service/
│       │   ├── TriageService.java
│       │   └── impl/TriageServiceImpl.java
│       ├── dialogue/
│       │   ├── DialogueSession.java         # 可变，由 DialogueSessionManager 管理；含 ruleVersion + ruleSetId 快照 + aiFailCount
│       │   └── DialogueSessionManager.java  # 生命周期管理 + 并发控制 + sessionId 校验（前端生成 UUID v4）
│       ├── rule/
│       │   ├── TriageRuleEngine.java
│       │   ├── DefaultTriageRuleEngine.java # 数据库规则源 + 定时缓存刷新
│       │   └── entity/TriageRule.java       # 分诊规则实体（JPA @Entity）
│       ├── fallback/
│       │   ├── DepartmentFallbackProvider.java
│       │   └── StaticDepartmentFallbackProvider.java
│       ├── repository/TriageRecordRepository.java, TriageRuleRepository.java, DeadLetterEventRepository.java
│       ├── entity/TriageRecord.java         # 含 recordId（@Id）、AI 推荐科室快照、推荐医生快照、规则匹配科室、最终选择科室、置信度、降级标记等
│       ├── entity/DeadLetterEvent.java      # 死信事件实体（含 id/@Id、eventPayload、state、retryCount 等字段）
│       ├── event/RegistrationEventListener.java  # 监听 RegistrationEvent 补充写入 finalDepartmentId
│       └── converter/TriageConverter.java    # 业务DTO ↔ ai-api DTO 映射
│
├── prescription/                            # 包D-AI1 处方审核 + 包E 辅助开方
│   └── src/main/java/com/aimedical/modules/prescription/
│       ├── api/
│       │   ├── audit/PrescriptionAuditController.java  # 含 POST /api/prescription/audit + POST /api/prescription/submit
│       │   └── assist/PrescriptionAssistController.java  # 含 POST /assist 主端点 + POST /assist/check-dose + GET /assist/suggestion/{taskId}
│       ├── dto/
│       │   ├── audit/AuditRequest.java, AuditResponse.java, AuditAlert.java,
│       │   │       AlertSeverity.java, AuditIssue.java, BlockResponse.java, AllergyDetail.java,
│       │   │       DrugInteraction.java, Suggestion.java
│       │   └── assist/
│       │       ├── PrescriptionAssistRequest.java  # 业务层 AI 辅助开方请求（3.4.10 完整输入）
│       │       ├── PrescriptionAssistResponse.java # 业务层 AI 辅助开方响应（3.4.10 完整输出）
│       │       ├── DosageCheckRequest.java  # 含 prescriptionId、给药途径参数和用药频率
│       │       ├── DosageCheckResponse.java # 剂量校验即时响应
│       │       ├── DosageAlert.java, DosageAlertLevel.java
│       │       ├── AiSuggestionResult.java  # 含 FAILED 状态和 failReason + partialData；并发安全保护
│       │       └── DoseWarning.java, AllergyWarningItem.java
│       ├── service/
│       │   ├── audit/PrescriptionAuditService.java + impl/
│       │   ├── audit/AuditRiskLevel.java
│       │   ├── audit/PrescriptionAuditEnforcer.java + impl/
│       │   └── assist/PrescriptionAssistService.java + impl/
│       │   └── assist/DosageThresholdService.java # 含单位一致性校验、年龄体重分级支持、日剂量校验
│       ├── rule/
│       │   ├── LocalRuleEngine.java
│       │   ├── DrugInteractionRule.java, AllergyCheckRule.java, ContraindicationCheckRule.java,
│       │   │       DuplicateCheckRule.java, DosageLimitRule.java
│       │   ├── SpecialPopulationDosageRule.java    # 儿童/老年人群特殊剂量检查
│       │   ├── entity/DrugInteractionPair.java      # JPA @Entity 药物相互作用数据实体
│       │   ├── entity/DrugAllergyMapping.java       # JPA @Entity 药物过敏映射数据实体
│       │   ├── entity/DrugContraindicationMapping.java # JPA @Entity 药品禁忌症映射数据实体
│   │   ├── entity/DrugCompositionDict.java     # JPA @Entity 药品成分字典实体（含 ingredients JSON TEXT，每项含 ingredientCode/ingredientName）
│       │   └── LocalRuleResult.java
│       ├── context/
│       │   └── PrescriptionDraftContext.java # 按 prescriptionId 关联
│       ├── repository/
│       │   ├── AuditRecordRepository.java
│       │   ├── DrugCompositionDictRepository.java
│       │   ├── DrugAllergyMappingRepository.java
│       │   ├── DrugContraindicationMappingRepository.java
│       │   └── DosageStandardRepository.java # 只读，继承 Repository 非 JpaRepository
│       ├── entity/AuditRecord.java           # 含 prescriptionOrderId, doctorId, patientId, forceSubmitted, forceSubmitTime, auditSequence, isLatest, originalPrescription(JSON TEXT)
│       └── converter/AuditConverter.java, AssistConverter.java  # 业务DTO ↔ ai-api DTO 映射
│
├── medical-record/                          # 包D-AI2 病历生成
│   └── src/main/java/com/aimedical/modules/medicalrecord/
│       ├── api/MedicalRecordController.java
│       ├── dto/RecordGenerateRequest.java, RecordGenerateResponse.java, FieldMissingHint.java
│       ├── service/MedicalRecordService.java + impl/
│       ├── template/
│       │   ├── TemplateConfigManager.java   # getTemplate 兜底返回 DEFAULT
│       │   ├── DepartmentTemplateConfig.java # 含 requiredFields + 每字段可选 promptMessage/suggestedAction 提示模板
│       │   └── DatabaseTemplateConfigManager.java # 定时/事件驱动缓存刷新
│       ├── parser/MissingFieldDetector.java
│       ├── repository/MedicalRecordRepository.java, DeptTemplateConfigRepository.java
│       ├── entity/MedicalRecord.java, DeptTemplateConfig.java
│       └── converter/MedicalRecordConverter.java  # 业务DTO ↔ ai-api DTO 映射

backend/modules/admin/
└── src/main/java/com/aimedical/modules/admin/
    ├── entity/ConfigChangeLog.java       # 配置变更审计日志实体
    └── repository/ConfigChangeLogRepository.java

backend/common/src/main/java/com/aimedical/common/
└── entity/
    └── DosageStandard.java               # 跨模块共享，admin 写入，prescription 只读；含年龄范围、体重范围、日剂量上限字段

backend/modules/ai/
├── ai-api/src/main/java/com/aimedical/modules/ai/api/
│   ├── AiService.java               # AI 能力统一调用接口
│   ├── AiResult.java                # AI 调用结果封装（含 success/data/errorCode/degraded/fallbackReason/partialData；超时/降级重载工厂方法 failure()/degraded()）
│   └── dto/                         # ai-api 层 DTO（分诊/审核/病历/辅助开方）
│       ├── triage/                  # TriageRequest, TriageResponse, RecommendedDepartment, AdditionalResponseItem 等
│       ├── prescription/            # PrescriptionCheckRequest/Response, PrescriptionAssistRequest/Response 等
│       └── medicalrecord/           # MedicalRecordGenRequest/Response 等
│
backend/modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/
├── auth/UserFacade.java                       # 跨模块用户认证门面接口
├── doctor/DoctorFacade.java                   # 跨模块医生信息查询门面接口
├── drug/DrugFacade.java                      # 跨模块药品信息查询门面接口（提供 findByDrugCode(drugCode) 等方法）
├── event/RegistrationEvent.java              # 挂号事件契约
├── store/
│   ├── SessionStore.java                    # 会话存储接口，DialogueSessionManager 依赖此接口
│   ├── SuggestionStore.java                 # AI 建议结果存储接口，DedupTaskScheduler 依赖此接口
│   ├── DraftContextStore.java              # 处方草稿上下文存储接口，PrescriptionDraftContext 依赖此接口
│   └── impl/ConcurrentHashMapStore.java     # Phase 2/3 内存实现，三项 Store 的初始实现
└── api/
```

### 2.2 模块职责与依赖方向

#### 各模块依赖关系

```
modules/consultation ───> common, common-module-api, ai-api
modules/prescription ───> common, common-module-api, ai-api
modules/medical-record ───> common, common-module-api, ai-api
application ───> 三新模块 + patient, doctor, admin, ai-impl, common-module-impl
```

依赖规则：
- 三个新模块均以 compile scope 依赖 common、common-module-api 和 ai-api
- 三个新模块之间不允许互相依赖。跨模块协作通过门面接口或事件驱动解耦
- application 模块引入三新模块依赖，Spring Boot 包扫描已覆盖

#### 包D-AI1 与包E 的强耦合处理

- DosageStandard 实体迁移至 common 模块，prescription 通过只读 Repository 查询
- admin 模块为 DosageStandard 的唯一写入者
- PrescriptionAssistServiceImpl 可注入 PrescriptionAuditService 直接调用（同模块无循环依赖）
- 辅助开方建议展示在处方编辑界面底部，审核结果展示在提交弹窗

#### 与 AI 模块的协作关系

各模块通过注入 AiService 接口调用 AI 能力。AI 降级时各模块以本地规则兜底。各模块 Converter 类负责业务层 DTO 与 ai-api 层 DTO 之间的映射/转换，转换操作在各 Service 实现类中显式调用。

#### 跨模块数据获取机制

consultation 模块需获取推荐医生列表和医生排班数据，通过 common-module-api 中定义的 DoctorFacade 接口解耦。DoctorFacade 由 doctor 模块的 impl 层实现，application 模块聚合后 Spring 自动注入。此模式与 UserFacade 一致。

prescription 模块需获取药品名称和规格信息，通过 common-module-api 中定义的 DrugFacade 接口解耦。DrugFacade 由 drug 模块的 impl 层实现，遵循与 DoctorFacade 一致的跨模块门面模式。

#### 跨模块事件传递机制

RegistrationEvent 定义在 common-module-api 中（com.aimedical.modules.commonmodule.event.RegistrationEvent），包含 registrationId、patientId、sessionId（可选，String——由前端在分诊结束后保留，进入挂号界面时将 sessionId 作为请求参数传入 registration 模块，registration 模块接收后填充至事件，作为事件消费端定位 TriageRecord 的精确关联键）、departmentId、doctorId、eventTime。事件发布端为 registration 模块（患者在挂号模块完成挂号后发布事件），消费端为 consultation 模块的 RegistrationEventListener（接收事件后通过 sessionId 定位对应的 TriageRecord，补充写入 finalDepartmentId 和 finalDepartmentName）。事件在 application 模块聚合后通过 Spring ApplicationEvent 机制跨模块传播。

**sessionId 跨模块传播机制**：分诊流程结束后，前端保留 TriageResponse.sessionId。当患者进入挂号流程时，前端将 sessionId 作为挂号请求的扩展参数传入 registration 模块。registration 模块接收 sessionId 后填充至 RegistrationEvent.sessionId 字段。此机制不要求 consultation 模块与 registration 模块之间存在任何后端依赖——sessionId 通过前端侧完成跨模块传递。若前端未传递 sessionId（如分诊降级场景前端未保存），RegistrationEvent 中 sessionId 为空，RegistrationEventListener 降级为按 patientId 查询最近的分诊记录进行关联。手动选科路径（POST /api/triage/select-department）作为 RegistrationEvent 自动写入路径的补充——RegistrationEventListener 写入的 finalDepartmentId 来源于挂号模块的 departmentId，手动选科写入的 finalDepartmentId 来源于前端提交的 departmentId。覆盖优先级强制执行机制：RegistrationEventListener 在调用 selectDepartment 前先检查 TriageRecord.finalDepartmentId 是否为空，仅当为空时才写入（即事件路径仅在手动选择未发生时生效）；手动选科端点调用 selectDepartment 时始终覆盖写入。此机制确保手动选科值优先于 RegistrationEvent 写入值，因为手动选科代表患者的最新最终选择。

**RegistrationEvent 消费失败补偿策略**：RegistrationEventListener 采用 @Retryable（Spring Retry，重试间隔 2s，最多 3 次，exponential backoff）处理事件消费失败。若重试仍失败，异常由 @Recover 方法（RetryOperationsInterceptor 的 recover 回调）接收，写入 dead_letter_event 表（含 id、eventPayload、failReason、failTime、state、retryCount、maxRetryCount），由定时补偿任务（每 30 分钟扫描重试未成功的死信事件）兜底。

**dead_letter_event 表模块归属**：DeadLetterEvent 实体定义在 consultation 模块（com.aimedical.modules.consultation.entity.DeadLetterEvent），对应 Repository 为 DeadLetterEventRepository（com.aimedical.modules.consultation.repository.DeadLetterEventRepository）。DeadLetterEvent 完整字段定义如下：

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | Long | @Id @GeneratedValue | 自增 | 死信事件主键 |
| eventPayload | TEXT | NOT NULL | - | 事件原始载荷（JSON 文本，序列化的 RegistrationEvent） |
| failReason | VARCHAR(500) | NOT NULL | - | 失败原因描述 |
| failTime | LocalDateTime | NOT NULL | - | 首次失败时间戳 |
| state | VARCHAR(20) | NOT NULL | 'FAILED' | 补偿状态，值域：FAILED / COMPENSATED / EXPIRED |
| retryCount | Integer | NOT NULL | 0 | 已重试次数 |
| maxRetryCount | Integer | NOT NULL | 3 | 最大重试次数上限 |

定时补偿任务封装为 DeadLetterCompensationService（com.aimedical.modules.consultation.service.DeadLetterCompensationService），由 ScheduledExecutorService 每 30 分钟调度一次，扫描 state=FAILED 且 retryCount < maxRetryCount 的死信事件重新投递，完成后更新 state=COMPENSATED 或递增 retryCount。补偿任务在 consultation 模块内部完成，不依赖外部模块。

**TriageRecord.finalDepartmentId 为空场景处理**：当 RegistrationEvent 未到达或消费失败时，TriageRecord.finalDepartmentId 保持为 null。查询端返回分诊记录时 finalDepartmentId=null 表示"患者尚未完成挂号确认"或"挂号事件尚未同步完成"。前端根据 finalDepartmentId=null 显示"待挂号确认"提示，并提供"手动选择科室"入口直接填写科室。此设计确保 RegistrationEvent 消费链路的中断不影响分诊记录查询功能。

**手动选科写入路径**：当患者在前端通过"手动选择科室"入口确认科室时，前端调用 POST /api/triage/select-department 端点，请求体包含 { sessionId, departmentId, departmentName }。TriageController 调用 TriageService.selectDepartment(sessionId, departmentId, departmentName)，该方法按 sessionId 查找 TriageRecord 并补充写入 finalDepartmentId 和 finalDepartmentName。此路径作为 RegistrationEvent 自动写入路径的补充，覆盖降级场景和患者主动选科场景。

### 2.3 AiService 接口定义

AiService 接口定义在 ai-api 模块（com.aimedical.modules.ai.api.AiService），是各业务模块调用 AI 能力的统一入口。AiResult 类定义在同包（com.aimedical.modules.ai.api.AiResult），封装 AI 调用结果（含 success/data/errorCode/degraded/fallbackReason 五字段），提供 failure()/degraded() 等重载工厂方法和超时降级重载（AiResult.failure(String errorCode, T partialData) / AiResult.degraded(String fallbackReason, T partialData)，partialData 通过重载工厂方法入参传入，数据写入 data 字段）。各方法使用泛型 `CompletableFuture<AiResult<T>>` 封装异步 AI 调用结果，具体方法签名及泛型绑定如下：

**triage(TriageRequest request)**
- 参数：ai-api 层 TriageRequest（含 chiefComplaint、additionalResponses、patientId、sessionId、ruleVersion、ruleSetId）
- 返回：`CompletableFuture<AiResult<TriageResponse>>`，泛型 T 绑定为 ai-api 层 TriageResponse
- 职责：AI 智能分诊推理，返回推荐科室列表、推荐理由、追问标记等
- 超时配置：ai.timeout.triage=8s
- 降级判定：AiResult.success=false 或 AiResult.degraded=true 触发降级

**prescriptionCheck(PrescriptionCheckRequest request)**
- 参数：ai-api 层 PrescriptionCheckRequest（含 prescriptionItems、patientInfo、prescriptionId）
- 返回：`CompletableFuture<AiResult<PrescriptionCheckResponse>>`，泛型 T 绑定为 ai-api 层 PrescriptionCheckResponse
- 职责：AI 处方审核推理，返回风险等级（LOW/MEDIUM/HIGH）、药物相互作用、用药建议等
- 超时配置：ai.timeout.prescription-audit=6s

**generateMedicalRecord(MedicalRecordGenRequest request)**
- 参数：ai-api 层 MedicalRecordGenRequest（含 dialogueText、patientId、encounterId、stream、departmentId）
- 返回：`CompletableFuture<AiResult<MedicalRecordGenResponse>>`，泛型 T 绑定为 ai-api 层 MedicalRecordGenResponse
- 职责：AI 病历生成推理，返回结构化病历的 7 个顶层字段
- 超时配置：ai.timeout.medical-record-generate=12s（非流式）
- 超时/降级：通过 AiResult.failure(errorCode, partialData) 或 AiResult.degraded(fallbackReason, partialData) 重载返回部分已生成字段

**prescriptionAssist(PrescriptionAssistRequest request)**
- 参数：ai-api 层 PrescriptionAssistRequest（含 diagnosis、examResults、patientInfo、existingPrescription、encounterId）
- 返回：`CompletableFuture<AiResult<PrescriptionAssistResponse>>`，泛型 T 绑定为 ai-api 层 PrescriptionAssistResponse
- 职责：AI 辅助开方推理，返回处方草案、剂量告警、过敏冲突告警
- 超时配置：ai.timeout.prescription-assist=10s
- 异步模式：包E check-dose 主端点使用 @Async / CompletableFuture.runAsync() 异步调用此方法

**AiResult<T> 泛型要点**：各方法的泛型 T 分别绑定为上述 4 个 ai-api 层 Response DTO；AiResult 统一包含 success（boolean）、data（T，成功时填充）、errorCode（String，可选）、degraded（boolean）、fallbackReason（String，可选）五字段。**契约约束**：当 success=true 时 data 必须非 null（AI 成功返回有效结果）；data=null 仅允许在 success=false 场景下出现（如超时 failure 或降级 degraded）。各 Service 实现类可依赖此契约——success=true 时解引用 data 前无需额外判空。若 AI 实现违反此契约（success=true 但 data=null），视为 AI 实现缺陷，由 ai-impl 模块的质量保障流程覆盖。超时/降级场景下 partialData 通过重载工厂方法 failure(String errorCode, T partialData) / degraded(String fallbackReason, T partialData) 入参传入并写入 data 字段。超时/降级重载方法的泛型参数 T 绑定的 ai-api 层 DTO 当前为空壳类（仅含默认构造器，完整字段定义待 §10 扩展），使用前需先完成 ai-api 层 DTO 的字段扩展——此为前提条件，时序依赖已在 §10 首段标注。

### MockAiService 实现契约

MockAiService 定义在 ai-impl 模块（com.aimedical.modules.ai.impl.mock.MockAiService），作为 AiService 接口的 Mock 实现，服务于开发/测试/演示环境。Mock 实现的行为契约如下：

**激活方式**：通过 Spring @Profile("mock") 条件注解激活——application 模块在 application-mock.yml 中配置 `spring.profiles.active=mock` 时注入 MockAiService 而非生产实现。生产环境不应激活此 profile。

**返回策略**：MockAiService 支持三种返回模式，通过配置项 `ai.mock.response-strategy` 切换：
1. `STATIC`（默认）：对每个 AiService 方法返回预设的静态响应数据（在 MockAiService 类中以常量定义，覆盖正常业务场景的全部输出字段）。Data 字段始终非 null，满足 §2.3 AiResult.success=true → data 非 null 契约。
2. `AI_UNAVAILABLE`：模拟 AI 完全不可用——所有方法返回 `AiResult.failure("MOCK_AI_UNAVAILABLE", emptyDto)`，触发各模块的降级路径。适用于验证降级流程。
3. `TIMEOUT`：模拟 AI 调用超时——所有方法在固定延迟（默认 `ai.mock.timeout-delay=8s`，配置项 `ai.mock.timeout-delay`）后返回 `AiResult.failure("MOCK_AI_TIMEOUT", partialData)`，partialData 为对应 DTO 的空壳实例。适用于验证超时降级路径。

**切换机制**：运行时通过 `GET /api/admin/ai/mock/strategy?mode={STATIC|AI_UNAVAILABLE|TIMEOUT}` 端点（注册在 ai-impl 模块的 MockAdminController 中）动态切换，无需重启。切换后生效于后续所有 AiService 调用。此端点仅在 mock profile 激活时注册，生产环境无此端点。

**返回数据文档**：预设 STATIC 响应数据的字段值定义在 MockAiService 类内的常量块中，以字段级注释标注，不单独输出文档。

---



## 3. 核心抽象

### 3.1 包C：智能分诊

#### TriageService（interface）

职责：定义智能分诊的核心业务边界，覆盖单轮分诊、多轮追问和会话管理。

协作对象：被 TriageController 调用（前端仅发送 sessionId 和新内容）；委托 AiService.triage() 进行 AI 分诊，AI 无结果时降级至 TriageRuleEngine.match()，规则匹配无结果时继续调用 DepartmentFallbackProvider.getFallbackDepartments() 获取兜底科室列表；管理 DialogueSessionManager。降级判定语义：AiResult.success=false 或 AiResult.degraded=true 时判定 AI 不可用、触发降级；AiResult.success=true 但推荐列表为空视为有效结果（AI 明确判断无匹配科室），跳过规则引擎。

**AI 连续失败兜底机制**：每次 AI 调用失败（含超时和不可用）时，DialogueSession.aiFailCount 递增。TriageServiceImpl 在降级后检查 aiFailCount，当 aiFailCount >= 3 时，在 TriageResponse 中附加 fallbackHint 字段，携带"建议直接联系线下接诊窗口"兜底文案。AI 调用成功时 aiFailCount 重置为 0。

**aiFailCount 持久化约束**：aiFailCount 存储于 DialogueSession（内存 ConurrentHashMap，TTL 30 分钟），不持久化至数据库。这意味着 aiFailCount 在以下场景会重置：(a) 会话 TTL 超时后清理，下轮新会话 aiFailCount 归零；(b) 服务重启后所有会话丢失，aiFailCount 归零。此设计是有意为之——aiFailCount 仅服务于当前对话会话的生命周期内，跨会话的 AI 失败计数无业务意义（不同患者、不同科室的 AI 调用相互独立），服务重启后归零是合理降级。若后续需要全局 AI 可用性监控，由基础设施层（如 Prometheus + AlertManager）而非业务层计数承担。

**AI 调用上下文传递策略**：TriageServiceImpl 在调用 AiService.triage() 前，将多轮对话完整上下文组装为 ai-api 层 TriageRequest。组装内容：从 DialogueSession 取主诉和全部追问 QA 历史，拼接到 TriageRequest.chiefComplaint 字段（首轮仅主诉，多轮时将主诉+历次 QA 按模板格式拼合为完整推理 prompt）；若 DialogueSession 中 correctedChiefComplaint 存在，则将其替换原始 chiefComplaint 作为推理上下文起点；TriageRequest.additionalResponses 字段填充当轮新增的追问回答列表。采用"全量拼接"策略——每次调用将完整上下文传入 AiService，AI 不依赖自身侧会话状态，确保无状态 AI 实现的可行性。

**主诉修正识别策略**：支持两条触发路径——(a) 显式路径：前端在 DialogueCreateRequest 中直接传递 correctedChiefComplaint 可选字段（String），TriageServiceImpl 检测到后直接覆盖写入 DialogueSession.correctedChiefComplaint；(b) 隐式路径：ai-api 层 TriageResponse 增加 correctedChiefComplaint 可选字段（String），AI 在推理输出中识别到患者修正主诉语义时填充此字段（如追问回复包含"患者主诉有误"、"实为"等语义标记），TriageServiceImpl 在 AiService.triage() 返回后检测 AiResult 中 ai-api TriageResponse.correctedChiefComplaint 非空则写入 DialogueSession.correctedChiefComplaint。两条路径优先级：显式路径优先于隐式路径——若前端传入 correctedChiefComplaint，忽略 ai-api 返回的隐式识别结果。

**全量拼接 Token 超限风险评估**：全量拼接策略在多轮长对话场景下，累计对话文本长度可能超出 AI 模型的上下文窗口（如 4K/8K token 限制）。为控制此风险，DialogueSession 在追加每轮 QA 时维护累计字符数，当累计字符数超过预配置阈值（默认 3000 字符，可配置参数 `triage.max-context-chars`）时，触发"上下文截断"策略——保留首轮主诉 + 最近 N 轮 QA（窗口策略），丢弃中间轮次。截断后前端继续显式展示完整对话历史，AI 调用仅传入精简后的推理上下文。**AI 感知截断标记**：在拼接上下文中首轮主诉和最近 N 轮 QA 之间显式插入截断标记 `[NOTE: 部分对话内容因长度限制已省略]`，使 AI 感知到中间轮次已被丢弃，避免因信息缺口产生错误推理。此截断风险在 Phase 2/3 分诊场景（通常 2-5 轮）下发生概率低，但设计层面做前瞻性备案，阈值参数预留可配置扩展。

**推荐医生列表生成机制**：AI 调用返回推荐科室列表后，TriageServiceImpl 对每个推荐科室调用 DoctorFacade.findAvailableDoctorsByDepartment(departmentId) 查询当前有排班的医生列表，按匹配评分排序取前 5 名，填充 availableSlotCount 和 score 字段。AI 不直接返回推荐医生列表——AI 聚焦科室级推理，医生匹配由后端基于排班数据生成。DoctorFacade.findAvailableDoctorsByDepartment() 返回医生基础信息 + 当日可预约时段数，实时查询确保 availableSlotCount 的时效性。

**TriageRecord 写入时机**：分诊结果返回响应前同步写入 TriageRecord（含 AI 推荐科室快照、推荐医生快照、规则匹配科室、置信度、降级标记等）。首次写入时 finalDepartmentId 为空——患者未挂号前尚无最终选择科室；finalDepartmentId 在患者挂号后通过 RegistrationEvent 由事件监听器补充写入。

**DialogueSession 与 TriageRecord 的事务一致性策略**：DialogueSession（内存 ConcurrentHashMap）的状态更新与 TriageRecord 数据库写入存在一致性问题——若 DialogueSession 先更新（如 aiFailCount 递增、QA 历史追加）后 TriageRecord 数据库写入失败，用户重试时 DialogueSession 处于不一致状态。采用"先写数据库再更新内存"策略——所有分诊流程中，TriageRecord 的数据库写入操作先于 DialogueSession 的内存更新执行。具体而言：TriageServiceImpl 在 AiService 调用/降级完成后，先通过 TriageRecordRepository.save() 写入 TriageRecord（在 @Transactional 事务内），事务提交成功后，再更新 DialogueSession（aiFailCount 递增、QA 历史追加等）。若数据库写入失败（抛异常），DialogueSession 保持之前未更新的状态，用户下次重试时一切从头执行。此策略接受如下数据丢失窗口：事务提交成功到内存更新之间的进程崩溃会导致 TriageRecord 已持久化但 DialogueSession 未同步更新（如 aiFailCount 未递增），但下次请求时会重新创建 DialogueSession（会话超时或匹配不到 session 时恢复初始状态），aiFailCount 丢失不影响业务正确性（仅影响连续失败计数，重置后可重新计数）。

**降级时前端行为指引**：TriageResponse.degraded=true 时，前端仍渲染推荐科室列表（规则匹配/兜底结果），同时显示降级提示文案（"AI 分诊暂时不可用，推荐结果基于规则匹配"）并提供"手动选择科室"入口，对齐需求文档 3.1.3.1"回退到按科室选择医生模式"降级路径。

**手动选科写入方法 selectDepartment**：TriageService 补充 selectDepartment(sessionId, departmentId, departmentName) 方法，按 sessionId 查找对应的 TriageRecord，若记录存在则更新 finalDepartmentId 和 finalDepartmentName，若记录不存在则返回 TRIAGE_SESSION_NOT_FOUND 错误。此方法由手动选科端点 TriageController.selectDepartment() 和 RegistrationEventListener 共同调用——RegistrationEventListener 通过 sessionId 关联找到 TriageRecord 后也调用此方法完成写入，避免重复逻辑。

**覆盖优先级强制执行语义**：RegistrationEventListener 在调用 selectDepartment 前先检查 TriageRecord.finalDepartmentId 是否为空，仅当为空时调用（事件写入仅在手动选科未发生时生效）；手动选科端点 TriageController.selectDepartment() 调用 selectDepartment 时始终覆盖写入。此机制确保手动选科值永久优先于 RegistrationEvent 自动写入值。

为何使用 interface：分诊业务可能存在多种实现（普通分诊 vs 急诊分诊），interface 允许扩展而不影响调用方。

#### DialogueSession（class，可变）

职责：封装一次多轮分诊的完整上下文（sessionId、主诉、症状列表、追问轮次、会话状态、规则版本快照 ruleVersion、规则集快照 ruleSetId、AI 连续失败计数 aiFailCount）。首轮创建时，DialogueSessionManager 记录当前规则引擎的版本号存入 DialogueSession.ruleVersion 和规则集标识存入 DialogueSession.ruleSetId；后续追问使用该快照版本对应的规则，确保对话内分诊逻辑前后一致。

**主诉修正支持**：DialogueSession 增加 correctedChiefComplaint 可选字段。首轮记录原始 chiefComplaint；多轮场景中若患者主动修正主诉（如"我之前说的不对，其实是头痛"），TriageServiceImpl 按两条触发路径识别并填充 correctedChiefComplaint：(a) 显式路径——前端在 DialogueCreateRequest 中直接传递 correctedChiefComplaint 字段（可选，String），TriageServiceImpl 检测到后直接覆盖写入；(b) 隐式路径——ai-api 层 TriageResponse 增加 correctedChiefComplaint 可选字段（String），AI 在推理输出中识别到患者修正主诉语义时填充此字段（如 AI 追问回复包含"患者主诉有误"、"实为"等语义标记），TriageServiceImpl 在 AiService.triage() 返回后检测 ai-api TriageResponse.correctedChiefComplaint 非空则写入 DialogueSession。两条路径优先级：显式路径优先于隐式路径——若前端传入 correctedChiefComplaint，忽略 ai-api 返回的隐式识别结果。全量拼接时若 correctedChiefComplaint 存在则替换原始 chiefComplaint 作为推理上下文起点。

协作：被 DialogueSessionManager 创建/修改/持久化。

为何使用可变 class：每轮追问追加 QA，可变避免频繁对象复制。DialogueSessionManager 承担并发控制——同 session 请求串行，不同 session 独立。

#### DialogueSessionManager（class）

职责：管理 DialogueSession 的创建、查找、更新和过期清理。承担并发访问的唯一控制点。**sessionId 校验与会话创建**：接受前端传入的 UUID v4 格式（36 字符，含连字符），验证格式有效性后通过 findOrCreate() 完成创建或恢复——若 sessionId 对应的会话不存在则创建（记录当前 ruleVersion 和 ruleSetId），有效则恢复，已超时则返回 TRIAGE_SESSION_EXPIRED 错误。首轮请求时前端生成 sessionId 传入（对齐需求文档 3.4.1 session_id 必填语义），DialogueSessionManager 不自行生成 sessionId。

协作：被 TriageService 调用。内部依赖 SessionStore 接口（com.aimedical.modules.commonmodule.store.SessionStore）进行会话存储，Phase 2/3 由 ConcurrentHashMapStore 实现（com.aimedical.modules.commonmodule.store.impl.ConcurrentHashMapStore），Phase 5 替换为 RedisStore；配合 ScheduledExecutorService（TTL 30 分钟）执行过期清理。

为何使用 class 而非 interface：管理器职责边界稳定且承担 TTL 调度和并发控制等生命周期管理职责；存储策略已通过 SessionStore 接口隔离，管理器本身无抽象化必要。

**TTL 清理竞态处理**：SessionStore 的后端存储（ConcurrentHashMap 或 Redis）提供原子删除语义——若定时清理线程先于业务请求删除 session，业务线程发现 session 不存在后返回 TRIAGE_SESSION_EXPIRED 错误由前端处理。此竞态路径被覆盖，无需额外加锁。

#### TriageRuleEngine（interface）

职责：症状→科室匹配规则引擎。规则源为数据库 TriageRule 实体，支持热加载。提供 currentRuleVersion() 方法返回当前规则版本号，提供 currentRuleSetId() 方法返回当前规则集标识，供 DialogueSessionManager 在创建会话时快照。提供 match() 方法：输入主诉文本 + ruleVersion + ruleSetId，输出 List\<RecommendedDepartment\>。

**规则快照失效处理**：当 match() 使用快照版本（ruleVersion + ruleSetId）查询无结果时（管理员已删除/禁用对应的规则集），降级使用当前最新版本规则集重新匹配，并在 TriageResponse 中标记 ruleVersionMismatch=true（可选），避免因规则管理操作导致分诊完全失败。

协作：被 TriageService 在 AI 不可用时调用。配合 Caffeine refreshAfterWrite（默认 60 秒）定时刷新规则缓存。

为何使用 interface：规则源可能从数据库演化为分布式规则引擎。

#### DepartmentFallbackProvider（interface）

职责：AI 和规则引擎均无法决策时返回静态兜底科室列表。

#### TriageRule（JPA @Entity）

职责：分诊规则持久化实体，承载 TriageRuleEngine 的规则数据来源。核心字段：ruleId（规则标识）、ruleSetId（所属规则集标识）、ruleVersion（规则版本号）、conditions（规则匹配条件，JSON 文本，JSON 结构为 `{"keywords": ["胸痛", "胸闷"], "logic": "AND"}`, 其中 keywords 为症状关键词列表、logic 为多关键词的逻辑组合方式（AND/OR），规则引擎按此结构与患者主诉做关键词匹配）、resultDepartmentId（命中后推荐科室 ID）、resultDepartmentName（推荐科室名称）、score（匹配评分权重）、enabled（是否启用）、createTime、updateTime。按 ruleSetId + ruleVersion 组织规则集，支持规则版本化管理和热加载。

#### DoctorFacade（interface）

职责：跨模块医生信息查询门面，定义在 common-module-api 中。提供 findAvailableDoctorsByDepartment(departmentId) 方法——查询指定科室当前有排班的医生列表，返回 List\<AvailableDoctor\>（每项含 doctorId / doctorName / departmentId / availableSlotCount），为 TriageServiceImpl 生成推荐医生列表提供数据来源。availableSlotCount 实时查询排班数据，确保时效性。

协作：在 common-module-api 中定义接口，由 doctor 模块实现，application 聚合后 Spring 自动注入。与 UserFacade 模式一致。

**跨模块调用降级保护**：DoctorFacade 作为强制同步跨模块调用路径，配置独立超时阈值（默认设置为 `consultation.doctor-facade.timeout=2s`，通过 @Value 或 feign/restTemplate 超时配置注入）。调用超时或 DoctorFacade 抛出异常时，TriageServiceImpl 捕获异常并将 TriageResponse.doctors 置为空列表（不阻断分诊主流程），在响应中记录日志（WARN 级别，含调用耗时、异常类型和科室 ID）供可观测性消费。此降级策略确保 DoctorFacade 不可用时不影响分诊科室推荐的正常返回——科室推荐仍由 AI/规则/兜底完成，仅医生列表为空。

为何使用 interface：解耦 consultation 模块与 doctor 模块的编译期依赖，遵循模块间通过门面接口协作的架构约束。

为何放在 doctor 子包而非 auth 子包：DoctorFacade 是跨模块医生排班/可用性查询门面，与用户认证（auth）职责不相关。独立子包（commonmodule/doctor/）使接口的语义定位与包名一致。

#### TriageResponse（class，DTO）

职责：封装分诊响应的结构化数据，对齐需求文档 3.4.1 输出契约。包含推荐科室列表（departments，List\<RecommendedDepartment\>，0–3 项）、推荐医生列表（doctors，List\<RecommendedDoctor\>，0–5 项）、推荐理由（reason，必填字段，字符数 ≥ 1）、匹配规则列表（matchedRules，List\<MatchedRule\>）、会话标识（sessionId）、是否需要追问标记（needFollowUp）及追问内容（followUpQuestion）、置信度（confidence，可选）、降级标记（degraded）、兜底提示（fallbackHint，可选）、规则版本不匹配标记（ruleVersionMismatch，可选，规则快照失效命中时为 true）。

#### DialogueCreateRequest（class，DTO）

职责：封装分诊对话请求值对象，对齐需求文档 3.4.1 输入契约。包含患者主诉（chiefComplaint，必填，字符数 5–500）、患者基本信息（patientId、age、gender）、会话标识（sessionId，必填——首轮请求由前端生成 UUID v4 传入，消除首轮为空特殊分支，与需求文档 3.4.1 session_id 必填语义对齐）、规则版本号（ruleVersion，可选）、规则集标识（ruleSetId，可选）、追问回答数组（additionalResponses，可选，List\<AdditionalResponse\>；与 chiefComplaint 互斥）、主诉修正（correctedChiefComplaint，可选，String——显式主诉修正路径，前端在后续追问轮次中传递此字段触发主诉修正）。

**chiefComplaint 与 additionalResponses 的互斥语义**：首轮请求仅传 chiefComplaint（必填），additionalResponses 不传；后续多轮追问仅传 additionalResponses + sessionId，chiefComplaint 不传（或传空）。correctedChiefComplaint 与 additionalResponses 兼容共存——同一请求中可同时携带 correctedChiefComplaint 和 additionalResponses，TriageServiceImpl 在处理时校验正确的字段组合规则。当检测到违规（同时提供二者或均未提供）时，返回 HTTP 400 + 错误码 `TRIAGE_FIELD_COMBINATION_INVALID`。

#### TriageRecord（JPA @Entity）

职责：分诊结果持久化实体。包含以下字段：recordId（Long，@Id @GeneratedValue，自增主键）、sessionId（会话标识）、patientId（患者 ID）、chiefComplaint（主诉快照）、aiRecommendedDepartments（AI 推荐科室快照，JSON 文本）、recommendedDoctors（推荐医生快照，JSON TEXT——存储 TriageResponse.doctors 列表快照，每项含 doctorId / doctorName / departmentId / availableSlotCount / score；对齐需求文档 §5.1 分诊记录核心字段"推荐医生"）、ruleMatchedDepartments（规则匹配科室快照，JSON 文本）、finalDepartmentId（最终选择科室 ID，nullable——首次写入时为空，患者挂号后通过事件/回调补充写入）、finalDepartmentName（最终选择科室名称，nullable）、confidence（置信度）、degraded（降级标记，boolean）、ruleVersion（使用的规则版本号）、ruleSetId（使用的规则集标识）、triageTime（分诊时间戳）。

### 3.2 包D-AI1：处方审核

#### PrescriptionAuditService（interface）

职责：处方审核核心流程：接收处方→调用 AI→AI 不可用时回退本地规则→返回风险等级。审计结果的 BLOCK 等级具有强制阻断语义，调用方（Controller 或 Orchestrator）必须根据 AuditRiskLevel 执行阻断决策。

协作：正常路径委托 AiService.prescriptionCheck()，降级路径委托 LocalRuleEngine；无论正常或降级路径，均在返回前写入 AuditRecord 持久化；返回结果中以 alerts 字段对齐需求文档 3.4.2 输出契约。

超时配置：ai.timeout.prescription-audit=6s。

#### AuditRiskLevel（enum）

PASS / WARN / BLOCK，固定有限分类。术语与需求文档的映射关系：
- **PASS**（对应"低风险"）：仅给出建议性提示，不影响提交
- **WARN**（对应"中风险"）：需显著告警，允许医生强制提交并留痕
- **BLOCK**（对应"高风险"）：业务强制拒绝，后端确保 BLOCK 处方不会被提交

#### AuditRecord（JPA @Entity）

持久化审核全部元数据。包含以下业务关联标识作为必填字段：
- auditId（Long，@Id @GeneratedValue，自增主键）
- prescriptionId：处方标识（UUID，草稿创建时分配，审核时必填）
- prescriptionOrderId：处方单号（业务单据号，如 RX202606280001，提交成功时分配——BLOCK 阻断的审核记录此字段可为空；prescriptionId 与 prescriptionOrderId 的关系为 1:1——prescriptionId 是系统层 UUID 标识，贯穿草稿→审核→提交全流程；prescriptionOrderId 是提交成功后分配的业务单据号；BLOCK 阻断时只存在 prescriptionId 无 prescriptionOrderId）
- doctorId：开方医生 ID
- patientId：患者 ID
- auditTime：审核时间戳
- fromFallback：boolean 标记，为 true 表示降级路径
- **forceSubmitted**：Boolean，标记医生是否执行了强制提交
- **forceSubmitTime**：LocalDateTime，强制提交时间戳
- **auditSequence**：int，必填，审核次序
- **isLatest**：boolean，必填，是否最新——同一业务主键下仅最新一条记录 isLatest=true
- **originalPrescription**：JSON TEXT，存储完整处方快照——用于 WARN 级强制提交时的处方版本校验（比较当前处方与 AuditRecord.originalPrescription 是否一致，防止审核后处方被篡改再提交）
- riskLevel、aiResult、auditIssues 等审核结果数据

**撤销审核时 isLatest 处理**：撤销审核通过 POST /api/prescription/audit/{auditId}/revoke 端点触发。请求参数：auditId（路径参数，Long，对应 AuditRecord.auditId）。响应：200（撤销成功，返回已撤销的 AuditRecord 摘要）、404（auditId 不存在或最近 AuditRecord 非 WARN 级别——仅 WARN 级审核允许撤销）、409（该 auditId 对应的审核已被撤销——isLatest 已为 false，拒绝重复撤销）。逻辑时序：后端按 auditId 查询 AuditRecord，校验 isLatest=true 且 riskLevel=WARN 通过后，将该 AuditRecord 的 isLatest 回退为 false（而非删除该记录——审核记录仍持久化保存），确保后续查询最新审核结果不会返回已撤销的记录。撤销后处方状态回退至"草稿"。

**同一处方多次审核时的 isLatest 管理**：采用"按业务主键分组"模式——优先按 prescriptionOrderId 分组清理 isLatest 标记；当 prescriptionOrderId 为空（BLOCK 阻断场景）时，按 prescriptionId 分组执行相同的 isLatest 清理逻辑。每次新审核写入 AuditRecord 时，先将同一业务主键下已有的 isLatest=true 的记录更新为 isLatest=false，再插入新记录并置 auditSequence = 上一条记录的 auditSequence + 1，isLatest=true。此操作在事务内完成保证一致性。

#### LocalRuleEngine（interface）

封装 DosageLimitRule、AllergyCheckRule、ContraindicationCheckRule、DuplicateCheckRule、SpecialPopulationDosageRule 等独立规则。每条规则独立实现/测试/启用/禁用。

**Phase 2/3 实现范围说明**：

本地规则校验的最小检查项集对齐需求文档 3.4.2 规定的 4 项：

| 检查项 | 对应需求文档 3.4.2 | Phase 2/3 规则 | 数据来源 | 实现状态 |
|--------|--------------------|----------------|---------|---------|
| ①剂量范围检查 | 最小检查项集 #1 | DosageLimitRule | DosageStandard 实体（common 模块） | 完整实现 |
| ②药品禁忌检查 | 最小检查项集 #2 | AllergyCheckRule + ContraindicationCheckRule | DrugAllergyMapping + 患者过敏史/合并症 + allergyDetails；DrugContraindicationMapping + 患者合并症 | 完整实现 |
| ③重复用药检查 | 最小检查项集 #3 | DuplicateCheckRule | DrugCompositionDict（药品成分字典） | 完整实现 |
| ④儿童/老年人群特殊剂量检查 | 最小检查项集 #4 | SpecialPopulationDosageRule | DosageStandard 年龄/体重分级字段 | 完整实现 |

**AllergyCheckRule 严重程度分级**：当 patientInfo 中存在 allergyDetails（List\<AllergyDetail\>）时，AllergyCheckRule 优先按结构化过敏信息做精确匹配——检查过敏原命中且 severity=SEVERE 时输出 BLOCK 级别 LocalRuleResult，severity=MODERATE/MILD 时输出 WARN 级别；当 allergyDetails 缺失时回退到 allergyHistory 文本匹配，文本匹配命中时一律输出 BLOCK 级别（保守策略，无法区分严重程度时按最严重处理）。

**ContraindicationCheckRule 合并症禁忌检查**：遍历处方药品列表，对每个药品查询 DrugContraindicationMapping 获取其禁忌症列表，与 patientInfo.comorbidities 做交集比对。命中时产出 WARN 或 BLOCK 级别 LocalRuleResult——分级策略为：禁忌症标注为"绝对禁忌"时输出 BLOCK，标注为"相对禁忌/慎用"时输出 WARN；DrugContraindicationMapping 的 contraindications JSON 中每项含 diseaseName（疾病名称）和 level（ABSOLUTE_CONTRAINDICATION / RELATIVE_CONTRAINDICATION），ContraindicationCheckRule 根据命中项的 level 决定 severity。

**检查项 #2 职责拆分说明**：需求文档 3.4.2 检查项 #2 包含两个子要求——（a）药品与患者过敏史冲突和（b）药品与患者合并症冲突。AllergyCheckRule 负责（a），ContraindicationCheckRule 负责（b），二者独立实现/测试，分别产出 LocalRuleResult。

**DosageLimitRule 严重程度分级**：根据超标程度区分——超过剂量上限 2 倍及以上时输出 BLOCK 级别，超过上限但未达 2 倍时输出 WARN 级别。此分级规则确保严重超标被强制阻断，轻度超标仅警告。

DrugInteractionRule（DDI）不在 Phase 2/3 本地规则范围内，DrugInteractionPair 数据实体在目录中预留骨架但不参与 Phase 2/3 运行时校验。

数据来源说明：
- DrugAllergyMapping：JPA @Entity（prescription/rule/entity/DrugAllergyMapping.java），drugCode 主键，关联过敏原列表。由管理员维护；Phase 2/3 以种子脚本初始化。DrugAllergyMappingRepository 提供按 drugCode 查询能力
- DrugContraindicationMapping：JPA @Entity（prescription/rule/entity/DrugContraindicationMapping.java），drugCode 主键，含 contraindications（禁忌症列表，JSON TEXT——每项含 diseaseName / level / description）。由管理员维护；Phase 2/3 以种子脚本初始化。DrugContraindicationMappingRepository 提供按 drugCode 查询能力
- DrugCompositionDict：JPA @Entity（prescription/rule/entity/DrugCompositionDict.java），drugCode 主键，含 ingredients（成分列表，JSON TEXT——每项含 ingredientCode（成分编码，string，必填）、ingredientName（成分名称，string，必填））。由管理员维护；Phase 2/3 以种子脚本初始化。DrugCompositionDictRepository 提供按 drugCode 查询能力
- DuplicateCheckRule 逻辑：遍历处方药品列表，对每个药品查询 DrugCompositionDict 获取成分编码（ingredientCode）集合，构建 drugCode → ingredientCodeSet 映射后检测成分编码交集——存在两种及以上药品共享相同成分编码时输出 WARN 级别 LocalRuleResult。采用 ingredientCode（成分编码）而非 ingredientName（成分名称）做匹配，可避免同一成分不同名称导致的临床漏报以及复方制剂共享成分导致的假阳性
- SpecialPopulationDosageRule 逻辑：根据患者年龄判断是否属于特殊人群（儿童年龄上限可配置，默认 ≤ 14 岁；老年年龄下限可配置，默认 ≥ 65 岁；对应配置项为 special-population.child-age-max=14、special-population.elderly-age-min=65），通过 @Value 或 @ConfigurationProperties 注入配置值。若是特殊人群则查询 DosageStandard 中对应年龄/体重分级的剂量标准，与处方中实际剂量做比对——超出该人群特殊剂量上限时产出 BLOCK 级别 LocalRuleResult

**数据源变更事件通知**：DrugAllergyMapping、DrugContraindicationMapping、DrugCompositionDict 三个数据实体的管理端变更通过 ApplicationEventPublisher 发布对应事件（DrugAllergyMappingChangeEvent、DrugContraindicationChangeEvent、DrugCompositionDictChangeEvent），消费端（LocalRuleEngine 中各规则类）监听后刷新本地缓存。Caffeine 定时刷新（refreshAfterWrite，默认 60 秒）作为事件丢失补偿覆盖。事件定义为独立 event 类，位于 prescription/event/ 包下，与 §9.3 所述规则变更事件框架一致。

#### LocalRuleResult（class，DTO）

职责：封装单条本地规则的校验结果。包含规则标识（ruleId）、是否通过（passed）、消息文本（message）、严重程度（severity，类型为 AuditRiskLevel 枚举——值域为 WARN 或 BLOCK；passed=true 时 severity 语义为 PASS，passed=false 时 severity 必为 WARN 或 BLOCK）。多条 LocalRuleResult 聚合为列表后，由 PrescriptionAuditService 实现类根据各 ruleResult 的 severity 聚合为最终的 AuditRiskLevel：若任一规则 severity 为 BLOCK 则整体判定为 BLOCK；若无 BLOCK 但存在 WARN 则整体判定为 WARN；全部 PASSED 则判定为 PASS。

#### AuditRequest（class，DTO）

职责：封装处方审核请求值对象，对齐需求文档 3.4.2 输入契约。包含处方药品列表（prescriptionItems，每项含 drugId/drugName/dose/frequency/duration/route 六个字段）、患者个体信息（patientInfo，含 patientId/age/gender/allergyHistory（string，过敏史文本——由后端从健康档案实体拼接 allergen 名称列表为中文逗号分隔字符串，为 single source of truth）/allergyDetails（List\<AllergyDetail\>，可选扩展容器——后端 Service 层优先从健康档案实体自动提取为 single source of truth，前端传入值仅作为 fallback/离线场景覆盖）/comorbidities（List\<String\>，合并症列表——由后端从健康档案慢病列表自动提取 diseaseName））、处方标识（prescriptionId，必填）。

**allergyHistory 与 allergyDetails 数据来源优先级**：(a) allergyHistory 由后端从健康档案实体拼接（与需求文档 §3.1.6 第1层一致）；(b) allergyDetails 按需求文档 §3.1.6 过渡方案第3层——允许前端存入，但后端 Service 层优先从健康档案实体自动提取 allergyDetails 为 single source of truth，前端传入值仅作为 fallback/离线场景覆盖（如健康档案不可达时）；(c) 此优先级规则同样适用于 PrescriptionAssistRequest.patientInfo 中的 allergyHistory / allergyDetails。

#### AllergyDetail（class，DTO）

职责：结构化过敏信息值对象，对齐需求文档 3.1.6 allergy_details 扩展容器每项结构。包含 allergen（string，必填）、reactionType（string，可选）、severity（AllergySeverity 枚举：MILD/MODERATE/SEVERE，对齐已有 patient 模块 AllergySeverity 枚举，可选）、occurredAt（string，ISO 日期，可选）。当 allergyDetails 存在时 AllergyCheckRule 优先按结构化过敏信息做精确匹配，缺失时回退到 allergyHistory 文本匹配。

#### AuditResponse（class，DTO）

职责：封装处方审核响应值对象，对齐需求文档 3.4.2 输出契约。包含风险等级（riskLevel，AuditRiskLevel）、风险提示列表（alerts，List\<AuditAlert\>）、药物相互作用结果（interactions，List\<DrugInteraction\>）、用药建议（suggestions，List\<Suggestion\>）、是否降级标记（fromFallback）。

#### PrescriptionAuditEnforcer（interface）

职责：定义 BLOCK 阻断的执行策略。默认阻断行为：Controller 层将 BlockResponse 以 HTTP 422 返回前端。

### 3.3 包D-AI2：病历生成

#### MedicalRecord（JPA @Entity）

职责：结构化病历持久化实体。核心字段包含：记录标识（recordId）、患者 ID（patientId）、就诊标识（visitId，必填）、就诊科室（departmentId）、病历内容（contentJson，TEXT 类型 + JPA @Convert/Jackson 序列化，存储为单列 JSON）、医生 ID（doctorId）、创建时间、更新时间。病历内容通过 MedicalRecordField 枚举标识顶层字段，每个字段对应病历中的一个语义段落。**并发写保护**：MedicalRecord 实体增加 `@Version` 乐观锁字段（version，Integer），JPA 乐观锁机制自动在更新时校验版本号。

**病历内容存储形式**：采用单列 JSON TEXT（contentJson 字段）存储，通过 JPA @Convert（AttributeConverter）+ Jackson 实现 Java 对象与 JSON 文本的自动双向转换。此设计将 7 个结构化字段聚合为单列存储，避免频繁的列级 DDL 变更，与 MedicalRecordField 枚举的扩展性保持一致。

**MedicalRecordRepository 核心查询方法**：findByVisitId(visitId)——按就诊标识查询病历、findByPatientId(patientId)——按患者 ID 查询病历列表、findById(recordId)——按记录标识查询病历详情。病历更新方法支持增量更新语义——读取 contentJson、合并变更字段、写回——确保部分字段更新时其他字段不受影响。更新操作使用 JPA 乐观锁机制——若更新时版本号与数据库中的版本号不一致（并发写冲突），JPA 抛出 OptimisticLockException，Repository 层捕获后转换为 MR_GEN_CONCURRENT_MODIFICATION 错误码返回，由前端提示用户刷新后重试。save() 和 saveAndFlush() 的乐观锁校验由 JPA @Version 注解自动完成，无需手动编写版本比对逻辑。

#### MedicalRecordField（enum）

职责：定义病历结构化输出的顶层字段标识符枚举。条目及与需求文档 3.4.3 输出契约字段名的映射关系：

| 枚举值 | 需求文档 3.4.3 字段名 | 中文含义 |
|--------|---------------------|---------|
| CHIEF_COMPLAINT | chief_complaint | 主诉 |
| SYMPTOM_DESCRIPTION | symptom_description | 症状描述 |
| PRESENT_ILLNESS | present_illness | 现病史 |
| PAST_HISTORY | past_history | 既往史 |
| PHYSICAL_EXAM | physical_exam | 体格检查 |
| PRELIMINARY_DIAGNOSIS | preliminary_diagnosis | 初步诊断 |
| TREATMENT_ADVICE | treatment_plan | 治疗意见 |

#### MedicalRecordService（interface）

对话文本→结构化病历。委托 AiService.generateMedicalRecord()、TemplateConfigManager、MissingFieldDetector。返回 RecordGenerateResponse。

**MissingFieldDetector 缺失判定策略**：采用"基于字段值的非空非 null 存在性"判定——字段不存在于响应中、值为 null、值为空字符串（含全空白字符串）均视为缺失，差集比对时计入缺失集合。仅当字段存在且值非空非 null 时视为已填充。

**FieldMissingHint 生成策略**：MissingFieldDetector 完成缺失字段差集比对后，对每个缺失字段从 DepartmentTemplateConfig 中读取对应 MedicalRecordField 的预定义提示模板——promptMessage（如"请输入{{fieldName}}"）和 suggestedAction（如"请补充{{fieldName}}信息"）。若模板中存在该字段的预定义提示内容，则使用模板值组装 FieldMissingHint；若模板中该字段无预定义提示，则使用默认文案——promptMessage 默认值"{{fieldName}}字段缺失"，suggestedAction 默认值"请补充{{fieldName}}信息"。模板内容由管理员在科室模板配置界面管理（admin 模块模板管理），TemplateConfigManager 在加载科室模板时一并缓存提示模板内容，与 §9 所述模板管理框架一致。

**非流式超时降级路径**：使用现有 AiResult.data 字段承载部分生成结果（不新增 partialData 字段）。AiResult 在超时场景下承载 failure+errorCode+partialData 组合——通过新增重载 AiResult.failure(String errorCode, T partialData) 实现（或由 AI 实现直接构造 AiResult(success=false, partialData, errorCode, degraded=false, fallbackReason=null)）。MedicalRecordService 在超时时从 AiResult.data 中提取已生成的部分字段，按分层保护策略返回——保留已提取字段 + 缺失字段标记 + MR_GEN_AI_TIMEOUT 错误码。前端对已生成字段正常渲染，缺失字段显示补全提示。降级场景（success=false + degraded=true + data 含部分结果 + fallbackReason）通过新增重载 AiResult.degraded(String fallbackReason, T partialData) 实现。

超时配置：ai.timeout.medical-record-generate=12s。

#### RecordGenerateRequest（class，DTO）

职责：病历生成请求值对象，对齐需求文档 3.4.3 输入契约。包含对话文本（dialogueText，必填，字符数 50–10000）、患者 ID（patientId）、就诊标识（encounterId，此字段映射为 MedicalRecord.visitId——Service 层在生成病历记录时通过 VisitFacade.findVisitIdByEncounterId(encounterId) 执行 encounterId → visitId 转换。VisitFacade 定义在 common-module-api 中（com.aimedical.modules.commonmodule.visit.VisitFacade），由就诊模块实现，返回 visitId（String）。MedicalRecordService 依赖 VisitFacade 接口而非实现，编译期不依赖就诊模块。**VisitFacade 调用降级策略**：VisitFacade 配置独立超时阈值（默认 `medical-record.visit-facade.timeout=2s`，通过 @Value 或 RestTemplate 超时配置注入）。调用超时或失败时，MedicalRecordService 依次尝试降级：(a) 将 encounterId 直接作为 visitId 的 fallback 写入（当 encounterId 非空且满足 visitId 格式约束时）；(b) 若 (a) 不可行，返回 MR_GEN_VISIT_NOT_FOUND 错误码并携带已生成的部分病历内容。降级场景下记录 WARN 级别日志（含调用耗时、异常类型）。此降级策略确保 VisitFacade 不可用时病历内容的已生成部分不丢失）、流式输出标记（stream，bool，可选，默认 false）。

**流式输出 Phase 2/3 实现决策**：Phase 2/3 仅实现非流式模式。stream=true 时返回 MR_GEN_STREAM_NOT_SUPPORTED 错误码。

#### RecordGenerateResponse（class，DTO）

职责：病历生成响应值对象，对齐需求文档 3.4.3 输出契约。包含结构化病历内容（fields）、缺失字段提示列表（missingFieldHints，为需求文档 missing_fields 的结构化升级版本）、生成状态标记（fromFallback）、降级状态标记（degraded，boolean——true 表示 AI 完全不可用/降级路径，false 表示 AI 明确返回空结果字段；前端据此区分展示：degraded=true 显示"AI 病历生成暂时不可用"提示，degraded=false 显示字段补全提示输入框）。

### 3.4 包E：辅助开方

#### PrescriptionAssistService（interface）

职责：**AI 辅助开方完整流程**——接收诊断结论、检查检验结果、患者信息、已有处方，委托 AiService.prescriptionAssist() 生成处方草案；本地即时校验（剂量阈值 DosageThresholdService + 过敏冲突本地检查）作为 AI 调用的前置补充，产出 doseWarnings 和 allergyWarnings。**过敏冲突本地检查实现路径**：PrescriptionAssistServiceImpl 直接复用同模块的 AllergyCheckRule 规则类（PrescriptionAssistServiceImpl 与 AllergyCheckRule 同属 prescription 模块，无跨模块依赖），对 AI 处方草案中每项药品执行过敏冲突匹配。复用逻辑包括结构化过敏信息精确匹配（allergyDetails 优先）和文本过敏史回退匹配。此复用确保辅助开方阶段与处方审核阶段共享同一套过敏匹配逻辑，避免逻辑分叉。**剂量阈值即时校验子域**——接收单药品剂量检查参数，委托 DosageThresholdService 做即时阈值校验。

**辅助开方过敏告警与处方审核过敏检查的关系**：辅助开方的 allergyWarnings 为即时提示性质（面向医生编辑期间的实时反馈，severity 含 HIGH 级别，对齐需求文档 3.4.10 过敏冲突"给出 HIGH 级别告警"），处方审核的 AllergyCheckRule 为提交时的正式审核判定（输出 AuditRiskLevel.BLOCK 或 WARN），二者独立执行不互斥。辅助开方 allergyWarnings.severity=HIGH 不直接等价于处方审核 AuditRiskLevel=BLOCK——二者分属不同模块、不同时机、不同判定维度。

API 端点定位（对齐需求文档 3.4.10）：
- **POST /api/prescription/assist**：AI 辅助开方主端点
- **POST /api/prescription/assist/check-dose**：剂量阈值即时校验子端点
- **GET /api/prescription/assist/suggestion/{taskId}**：异步 AI 建议查询端点

**/assist 主端点 AI 返回无可推荐药品场景**：当 AiResult.success=true 但 AI 返回空处方草案（无推荐药品）时，PrescriptionAssistResponse 中 prescriptionDraft.drugs 为空列表，本地校验结果正常返回（doseWarnings 和 allergyWarnings 为空列表），响应通过 PrescriptionAssistResponse.errorCode 字段承载 RX_ASSIST_AI_NO_RECOMMENDATION 错误码，前端据此展示"AI 暂无可推荐药品，请手动开方"提示。此场景不作为业务异常返回 4xx——它是 AI 的有效推理结果，药品推荐为空不等同于调用失败。此错误码与 check-dose 异步建议 FAILED 状态关联：二者均表示 AI 未产出有效结果，但语义不同——NO_RECOMMENDATION 表示 AI 明确判断无推荐（positive empty），FAILED 表示 AI 过程异常（negative failure）。

**Phase 2/3 范围说明**：辅助开方的"AI 生成处方草案 → 医生确认后生效"流程中，Phase 2/3 仅实现"AI 生成 → 人工手动录入/调整"的分离模式。AI 生成的处方草案作为医生编辑的起始内容，医生在处方编辑界面查看/修改后通过提交审核流程完成"确认"。完整的 AI 建议确认状态跟踪（每项建议的 ACCEPTED/MODIFIED/REJECTED 状态、医生修改内容记录、签名确认等）属于 Phase 4 范围，本阶段不实现。如需在 AI 建议层面跟踪确认状态，Phase 4 需引入 SuggestionConfirmation 实体（含 suggestionId、确认状态 enum：PENDING / ACCEPTED / MODIFIED / REJECTED、医生修改内容 diff、确认时间戳、确认人 ID），本设计仅预留此扩展方向。

#### PrescriptionAssistRequest（class，DTO — 业务层 /assist 端点请求）

职责：业务层 AI 辅助开方主端点请求值对象，对齐需求文档 3.4.10 输入契约。包含诊断结论（diagnosis）、检查检验结果摘要（examResults，List\<ExamResultItem\>）、患者基础信息（patientInfo，含 patientId / age / gender / allergyHistory / allergyDetails（List\<AllergyDetail\>，可选）/ comorbidities）、已有处方（existingPrescription，可选）、就诊标识（encounterId，可选）。

**allergyHistory 与 allergyDetails 数据来源优先级**：与 AuditRequest.patientInfo 一致——allergyHistory 由后端从健康档案实体拼接；allergyDetails 后端优先从健康档案实体提取，前端传入值仅作为 fallback/离线场景覆盖。

#### DosageThresholdService（class）

按 drugCode + routeOfAdministration + ageRange + weightRange 查询 DosageStandard，比较剂量阈值。匹配优先级策略参见 §8.4 六级定义：精确匹配→同时范围匹配→年龄范围匹配→体重范围匹配→无分级默认阈值→标准不存在（六级均未命中，降级返回 WARN 级 DosageAlert 并携带 errorCode=RX_ASSIST_DOSE_STANDARD_NOT_FOUND）。三种输出路径及对应的 warningType 赋值规则：(1) 单次剂量超过上限→DoseWarningType.OVER_SINGLE_DOSE；(2) 日剂量校验分支：当 DosageCheckRequest 携带 frequency 且 DosageStandard 存在 dailyMax 时，校验单次剂量 × 频率是否超出日剂量上限→DoseWarningType.OVER_DAILY_DOSE；(3) 疗程剂量超限校验→DoseWarningType.OVER_DURATION（Phase 2/3 预留，暂不实现）。

**异步 AI 调用去重策略**：同一 prescriptionId 在 check-dose 高频调用下可能产生大量并发异步 AI 调用。定义 prescriptionId 级去重规则——(a) 创建新异步 AI task 前查询该 prescriptionId 下最近 AiSuggestionResult：若存在 PENDING 状态 task 则复用已有 taskId（不创建新 AI 调用）；若存在 COMPLETED 且未被前端消费的 task 则复用已有 taskId 与结果（不创建新 AI 调用）；仅当前次 task 为 FAILED 或 COMPLETED 已被消费时创建新 task。(b) **已消费判定**：前端通过 GET /api/prescription/assist/suggestion/{taskId} 读取结果后标记为已消费——AiSuggestionResult 增加 consumed 布尔标记（默认 false），查询端点返回结果时将 consumed 置为 true。(c) 前端配合 300ms 防抖合并连续剂量调整请求。去重检查为同步前置操作（在主响应 DosageCheckResponse 返回前执行），不影响即时剂量校验的实时性。去重实现建议封装为 DedupTaskScheduler 辅助类（com.aimedical.modules.prescription.service.assist.DedupTaskScheduler）。

#### AiSuggestionResult（class）

异步 AI 结果值对象，封装 taskId、suggestion（String，COMPLETED 时填充）、status（AiSuggestionStatus 枚举：PENDING / COMPLETED / FAILED）、createTime、failReason（可选）、consumed（boolean，默认 false——标记前端是否已通过异步查询端点读取结果，用于去重判定）、partialData（可选，JSON 文本，AI 超时或部分生成时携带部分推荐结果）。

并发安全策略：AiSuggestionResult 实例由 SuggestionStore 接口存储（Phase 2/3 ConcurrentHashMapStore 实现），通过 compute() 原子替换整个实例保证并发安全；Phase 5 替换为 RedisStore 后由 Redis 原子操作（如 SETNX/Lua 脚本）提供等价并发保证。

查询时遵循四分支模式：不存在→RX_ASSIST_SUGGESTION_NOT_FOUND、PENDING、COMPLETED、FAILED + failReason。

预创建→更新模式与前一版一致。

AiSuggestionResult 增加 TTL（60 分钟），由 ScheduledExecutorService 定期清理过期条目。

**TTL 过期与 consumed 标记的协调策略**：AiSuggestionResult 有两个释放条件——TTL 过期（60 分钟自动清理）和前端消费（consumed=true）。二者协调规则如下：
- **已消费优先**：前端通过 GET /api/prescription/assist/suggestion/{taskId} 读取结果后标记 consumed=true，此时即使 TTL 未过期，去重逻辑仍视其为"已消费"触发新 task 创建。
- **TTL 兜底**：若前端在 60 分钟内未查询（如用户离开编辑器），TTL 过期清理该条目。清理后前端使用旧 taskId 查询时返回 RX_ASSIST_SUGGESTION_NOT_FOUND。
- **前端 TTL 过期处理策略**：前端在收到 RX_ASSIST_SUGGESTION_NOT_FOUND 时执行以下操作：(a) 检查当前处方草稿 session 是否仍有效——若用户仍在编辑器内，自动发起新的 check-dose 调用以获取新 taskId；(b) 若用户已离开编辑器（如关闭浏览器），忽略此错误（已无需展现建议结果）。
- **TTL 与 consumed 的优先级关系**：consumed=true 不阻止 TTL 清理——清理周期到达时 consumed=true 的条目同样被清理，因为已消费的条目无需保留。此设计确保内存不会因已消费条目无限积累。

**AiResult → AiSuggestionResult 映射规则**：AiService.prescriptionAssist() 返回的 `CompletableFuture<AiResult<PrescriptionAssistResponse>>` 与 AiSuggestionResult 之间的映射规则定义如下：

| AiResult 状态 | AiResult.success | AiResult.degraded | AiResult.data | AiSuggestionResult.status | AiSuggestionResult.suggestion | AiSuggestionResult.failReason | AiSuggestionResult.partialData |
|--------------|-----------------|------------------|--------------|--------------------------|------------------------------|------------------------------|-------------------------------|
| 正常完成 | true | false | 非 null——完整 PrescriptionAssistResponse | COMPLETED | 完整推荐内容（JSON 序列化后的 prescriptionDraft.drugs 等全量字段） | null | 不写入 |
| 降级完成（degraded） | true | true | 非 null——AI 降级但仍返回有效结果（如使用精简模型或缓存数据） | COMPLETED | data 中可用字段的 JSON 序列化 | null | 不写入（降级结果本身即为有效结果，无需 partialData） |
| 明确失败（success=false） | false | false | null 或部分数据（partial result） | FAILED | null | AiResult.fallbackReason（如 "AI 服务不可用"）或 AiResult.errorCode 的描述映射 | 若 data 携带部分结果（如已生成的处方片段），序列化为 JSON TEXT 写入 partialData |
| 超时（failure + partialData） | false | false | 非 null——partialData 为已经生成的部分推荐结果 | FAILED | null | AiResult.errorCode 映射文案（如 "AI 超时"） | data 的 JSON 序列化（部分推荐结果） |
| DEGRADED + partialData | false | true | 非 null——partialData 为降级时已生成的部分结果 | COMPLETED | partialData 中可用字段的 JSON 序列化（去除非有效噪声） | null | 不写入（降级结果已作为 suggestion 写入） |

**partialData 写入时机与格式**：
- 写入时机：仅在上述"明确失败"和"超时"两种路径中写 partialData；"正常完成"和"降级完成"路径不写。
- 格式：JSON TEXT，结构为 PrescriptionAssistResponse 的字段级子集——仅包含 AI 已成功生成的部分字段（如 `{"prescriptionDraft": {"drugs": [...]}}`），缺失字段不在 JSON 中出现。
- **DEGRADED 归入 COMPLETED**：当 AiResult.success=true（无论 degraded=true/false）且 data 非空时，一律归入 COMPLETED。degraded 标记的业务语义仅为"AI 使用了降级路径"，不影响结果状态判定。
- **partialData 为空时的 FAILED 处理**：若 FAILED 路径下 partialData 为空（AI 完全无任何生成即失败），AiSuggestionResult.failReason 中明确标注"AI 无任何部分结果生成"，前端按"AI 建议生成失败"提示。

#### PrescriptionDraftContext（class）

职责：处方草稿上下文，在医生编辑处方期间暂存 CRITICAL 级别剂量告警标记。内部依赖 DraftContextStore 接口（com.aimedical.modules.commonmodule.store.DraftContextStore）进行存储，Key 为 prescriptionId（系统层 UUID 标识，草稿创建时分配——与 AuditRecord.prescriptionId 是同一标识，贯穿草稿→审核→提交全流程；prescriptionOrderId 是提交成功后分配的业务单据号，在草稿编辑阶段尚不存在，因此 PrescriptionDraftContext 以 prescriptionId 为键）。

生命周期管理：创建时机（首次 check-dose 请求时——DraftContextStore 通过 ConcurrentHashMapStore 的 computeIfAbsent() 惰性创建 Entry，不存在空引用风险；写入操作通过 Store 接口的 put() 方法委托给后端存储的原子写入）、写入时机（CRITICAL 告警时，每次 check-dose 重算后覆盖该 prescriptionId 下已有的所有 CRITICAL 标记——若新结果无 CRITICAL 则清除旧标记）、清理时机（处方提交成功/处方取消/TTL 过期 60 分钟）。**TTL 过期清理由 ScheduledExecutorService 定期扫描实现（每 5 分钟扫描一次，遍历 DraftContextStore 清理最后更新时间超过 60 分钟的条目）**，确保异常退出场景下 CRITICAL 标记不会无限残留。

**实例化保证**：PrescriptionDraftContext 作为 Spring @Component 单例 Bean，其内部持有的 DraftContextStore（Phase 2/3 为 ConcurrentHashMapStore 实现）在构造器执行时完成初始化，不存在空指针风险。所有写入操作在对应 Service 方法内同步执行，无需外部队列或回调保证。

**prescriptionId 分配时机**：prescriptionId 在处方编辑生命周期开始前由前端预创建（UUID v4），首次 check-dose 调用时即传入——前端在患者进入处方编辑界面时生成本次处方的 prescriptionId，在后续所有 check-dose 和 /assist 调用中携带此 ID。若极端场景下前端未预创建 prescriptionId（如离线客户端异常），后端在首次 check-dose 调用检测到 prescriptionId 为空时自动生成 UUID v4，并通过 DosageCheckResponse.prescriptionId 字段（新增可选 String 字段）回写给前端。此双路径——前端预创建（主路径）> 后端按需生成（fallback）——确保 prescriptionId 在 check-dose 请求链路中始终存在。**API 验证层在 check-dose 端点应对 prescriptionId 字段跳过 @NotNull 校验**（仅允许空值时触发后端自动生成路径），主路径校验由业务层在 Service 方法内检查非空而非 API 层拦截；/assist 主端点和处方审核（audit）端点的 prescriptionId 做常规 @NotNull API 层校验。

覆盖更新行为契约：每次 check-dose 请求执行后，DosageThresholdService 将当前校验结果中 alertLevel=CRITICAL 的告警列表写入 PrescriptionDraftContext（以 prescriptionId 为键覆盖写入），非 CRITICAL 告警不入上下文。若当前校验结果无 CRITICAL 告警，则清除该 prescriptionId 对应的上下文条目。此"全量覆盖"语义确保旧 CRITICAL 标记不会因新的检查结果而残留。

**前端同步协商机制**：PrescriptionDraftContext 提供 getContextCriticalCount(prescriptionId) 方法返回指定 prescriptionId 下当前 CRITICAL 告警数量，由 DosageThresholdService 在 check-dose 响应中携带 contextCriticalCount 字段供前端判断是否需要同步刷新状态。每次覆盖写入后 contextCriticalCount 随 CRITICAL 告警列表大小实时更新，确保前端与后端 CRITICAL 状态之间的时序窗口最小化。

**contextCriticalCount 前端消费规则**：check-dose 响应中 contextCriticalCount 字段表征当前 prescriptionId 下后端 PrescriptionDraftContext 中 CRITICAL 级别告警数量。前端在每次 check-dose 请求（用户修改剂量时触发）的响应中读取 contextCriticalCount，按以下规则更新 UI 状态：
- N（>0）→ 0（从有告警变为无告警）：前端恢复"提交"按钮的可用状态，清除之前展示的 CRITICAL 阻断提示，将处方提交端点步①的 CRITICAL 阻断检查标记为"可通过"
- 0 → N（从无告警变为有告警）：前端禁用"提交"按钮，展示 CRITICAL 阻断提示（展示最新 check-dose 响应中 alertLevel=CRITICAL 的告警消息列表），将处方提交端点步①的 CRITICAL 阻断检查标记为"阻断中"
- 0 → 0（持续无告警）：不做任何 UI 变更，提交按钮维持可用
- N → M（告警数量变化，均 >0）：前端同步更新阻断提示中告警列表的展示内容，提交按钮维持禁用状态

#### DoseWarningType（enum）

职责：剂量告警类型枚举，定义 OVER_SINGLE_DOSE（单次剂量超限）、OVER_DAILY_DOSE（日剂量超限）、OVER_DURATION（疗程剂量超限）三个值。此枚举与 DosageAlertLevel（告警严重程度维度）正交——warningType 标识"什么类型的剂量超标"，alertLevel 标识"超标严重程度"。OVER_DURATION 在 Phase 2/3 预留暂不实现。

#### PrescriptionAssistResponse（class，DTO）

职责：AI 辅助开方主端点响应值对象，对齐需求文档 3.4.10 输出契约。包含处方草案（prescriptionDraft，含 drugs 数组，每项含 drugId/drugName/dose/frequency/duration/route）、剂量告警列表（doseWarnings，List\<DoseWarning\>，每项含 drugId/warningType（DoseWarningType 枚举）/message/severity（DosageAlertLevel 枚举））、过敏冲突告警（allergyWarnings，List\<AllergyWarningItem\>，每项含 drugId/allergen/severity（AllergyWarningSeverity 枚举））、顶层错误码（errorCode，可选，String——AI 返回无可推荐药品时填充 RX_ASSIST_AI_NO_RECOMMENDATION）、免责声明标记（disclaimerRequired，boolean，固定 true）。

#### AllergyWarningSeverity（enum）

职责：过敏告警严重程度枚举，定义 INFO / WARNING / HIGH 三个级别（从轻到重）。HIGH 表示严重过敏冲突（语义对应 AuditRiskLevel.BLOCK 但不等同——辅助开方 allergyWarnings.severity=HIGH 为编辑阶段即时提示，处方审核的 BLOCK 为提交时正式阻断判定，二者独立执行不互斥），WARNING 表示中度冲突，INFO 表示轻微提示。此枚举与 DosageAlertLevel（INFO/WARNING/CRITICAL）和 AlertSeverity（INFO/WARNING/CRITICAL）分属不同告警维度，AllergyWarningSeverity 采用 INFO/WARNING/HIGH 命名与需求文档 3.4.10"HIGH 级别告警"术语一致。

#### DoseWarning（class，DTO）

职责：剂量告警值对象，对齐需求文档 3.4.10 dose_warnings 数组每项结构。包含以下字段：
- drugId（string，药品编码）
- warningType（DoseWarningType 枚举：OVER_SINGLE_DOSE / OVER_DAILY_DOSE / OVER_DURATION，告警类型）
- message（string，告警消息）
- severity（DosageAlertLevel 枚举：INFO / WARNING / CRITICAL，告警严重程度）

#### AllergyWarningItem（class，DTO）

职责：过敏冲突告警值对象，对齐需求文档 3.4.10 allergy_warnings 数组每项结构。包含以下字段：
- drugId（string，药品编码）
- allergen（string，过敏原）
- severity（AllergyWarningSeverity 枚举：INFO / WARNING / HIGH，告警严重程度）

---

## 4. 关键行为契约

### 4.1 智能分诊场景

```
POST /api/triage/consult
单轮: { chiefComplaint: "胸痛伴气短", patientId: "P001", age: 45, gender: "男", sessionId: "前端生成UUID v4" }
      chiefComplaint 字符数约束: 5–500
多轮: { sessionId: "xxx", additionalResponses: [{question: "...", answer: "...", answeredAt: "..."}] }
       首轮仅传 chiefComplaint，后续轮仅传 additionalResponses + sessionId，二选一互斥
       错误响应: 字段组合违规（同时提供二者或均未提供）时返回 HTTP 400 + TRIAGE_FIELD_COMBINATION_INVALID

AI 调用上下文组装:
  TriageServiceImpl 调用 AiService.triage() 前，从 DialogueSession 取完整对话上下文
  → 首轮: TriageConverter.toAiTriageRequest() 将 chiefComplaint 映射为 ai-api TriageRequest.chiefComplaint
  → 多轮: TriageConverter.toAiTriageRequest() 将主诉+历次 QA 按模板格式拼合为 TriageRequest.chiefComplaint
       + 当轮 additionalResponses 映射为 TriageRequest.additionalResponses
  ai-api TriageRequest 扩展 additionalResponses 字段（List<AdditionalResponseItem>，
  每项含 question/answer/answeredAt），对齐需求文档 3.4.1 多轮调用组装约定

推荐医生列表生成:
  AI 返回推荐科室列表后，TriageServiceImpl 对每个推荐科室调用 DoctorFacade.findAvailableDoctorsByDepartment(departmentId)
  → 超时配置: consultation.doctor-facade.timeout=2s（默认）
  → 获取当前有排班的医生列表，按匹配评分排序取前 5 名
  → AvailableDoctor.doctorId/doctorName/departmentId/availableSlotCount 映射为 RecommendedDoctor
  → 降级保护: DoctorFacade 调用超时/异常时捕获并记录 WARN 日志，TriageResponse.doctors 置为空列表，
    不阻断分诊主流程——科室推荐由 AI/规则/兜底正常返回，仅医生列表为空

降级判定:
  AiResult.success=false 或 AiResult.degraded=true → 判定 AI 不可用，触发降级；DialogueSession.aiFailCount++
  AiResult.success=true 且推荐列表为空 → AI 有效结果（跳过规则引擎）；aiFailCount 重置
  AiResult.success=true 且推荐列表非空 → 正常返回；aiFailCount 重置

AI 连续失败兜底:
  aiFailCount >= 3 时，TriageResponse 附加 fallbackHint = "建议直接联系线下接诊窗口"

正常: AiService.triage(TriageRequest) → 推荐科室 + 推荐理由 + 匹配规则 → 后端生成推荐医生列表
降级链: AiService 不可用 → TriageRuleEngine.match(chiefComplaint, ruleVersion, ruleSetId)
        → 匹配成功返回科室 → 后端生成推荐医生列表
        → 规则匹配无结果 → DepartmentFallbackProvider.getFallbackDepartments() → 兜底科室

降级时前端行为:
   TriageResponse.degraded=true → 前端仍渲染推荐科室列表 + 降级提示文案 + "手动选择科室"入口
   对齐需求文档 3.1.3.1"回退到按科室选择医生模式"

手动选科:
   POST /api/triage/select-department
   { sessionId, departmentId, departmentName }
   → TriageController 调用 TriageService.selectDepartment(sessionId, departmentId, departmentName)
   → 按 sessionId 查找 TriageRecord，更新 finalDepartmentId 和 finalDepartmentName
   → 手动选科路径与 RegistrationEvent 路径共存：手动选科值优先于 RegistrationEvent 写入值（覆盖语义），
     代表患者的最新最终选择
   → 如果前端调用此端点导致 TriageRecord 已存在的 finalDepartmentId 被覆盖，
     TriageRecord 记录 previousFinalDepartmentId 辅助审计（可选）
   → TriageRecord 不存在时返回 TRIAGE_SESSION_NOT_FOUND

规则快照失效处理:
  TriageRuleEngine.match(chiefComplaint, ruleVersion, ruleSetId) 使用快照版本查询无结果时
  → 降级使用当前最新版本规则集重新匹配
  → TriageResponse 标记 ruleVersionMismatch=true

持久化: 分诊完成后同步写入 TriageRecord（含推荐医生快照 recommendedDoctors，
        finalDepartmentId 为空，患者挂号后通过 RegistrationEvent 补充写入）
```

### 4.2 处方审核场景

```
POST /api/prescription/audit
超时配置: ai.timeout.prescription-audit=6s

正常: AiService.prescriptionCheck() → AuditConverter.toAuditResponse(AiResult) → AuditRecord 持久化 → 返回 AuditResponse
降级: LocalRuleEngine.check()（5 条规则完整执行：DosageLimitRule + AllergyCheckRule + ContraindicationCheckRule + DuplicateCheckRule + SpecialPopulationDosageRule）
      → 各条规则产出 LocalRuleResult（ruleId, passed, message, severity 为 AuditRiskLevel.WARN 或 BLOCK）
      → 聚合多条 LocalRuleResult 为最终 AuditRiskLevel
      → AuditRecord 持久化（fromFallback=true） → 返回 AuditResponse

审核结果处理:
  PASS（低风险） → 响应建议性审核意见，不影响提交
  WARN（中风险） → 响应警告信息，前端弹窗提示医生确认
                    医生三种可选操作：
                    1. 强制提交并留痕 → 前端调用处方提交端点携带 forceSubmit=true + auditRecordId
                       → 后端校验：存在 WARN 级最新 AuditRecord（auditRecordId 对应记录 isLatest=true）
                       → 校验处方版本：比较当前处方与 AuditRecord.originalPrescription 是否一致（防篡改）
                         一致性语义定义：按业务字段做结构化比较（drugId + dose + frequency + duration + route 五字段组合比对），忽略 JSON 文本级的格式差异（字段顺序、null 与缺失字段、数值精度等）。药品增删、剂量变化等业务实质变更判为"不一致"；仅 JSON 序列化格式差异判为"一致"
                       → 一致则写入 AuditRecord.forceSubmitted=true, forceSubmitTime=now → 处方提交成功
                       → 不一致则返回 RX_AUDIT_PRESCRIPTION_MODIFIED 错误码，提示"处方已变更，请重新审核"
                    2. 修改后重新审核
                     3. 撤销审核 → POST /api/prescription/audit/{auditId}/revoke（路径参数 auditId；响应 200 成功/404 不存在或非 WARN/409 已撤销）。后端将该 AuditRecord.isLatest 回退为 false，处方状态回退至"草稿"
  BLOCK（高风险） → PrescriptionAuditEnforcer 执行阻断 → 返回 HTTP 422 + BlockResponse
                     前端弹窗展示阻断原因列表，禁止提交

处方提交端点设计边界:
   POST /api/prescription/submit（Phase 2/3 简要契约定义）
   请求: { prescriptionId, prescriptionItems, forceSubmit(bool, 默认 false), auditRecordId(可选) }
   行为（执行顺序严格定义）:
     步① CRITICAL 阻断检查（最高优先级）：检查 PrescriptionDraftContext 中该 prescriptionId 是否存在 CRITICAL 级别剂量告警。若存在，立即返回 HTTP 422 + BlockResponse（阻断原因列表仅含剂量告警内容），不执行后续任何步骤。
     步② 审核结果阻断检查：若步①通过（无 CRITICAL 阻断），检查该处方的最新 AuditRecord 是否为 BLOCK 级别。若为 BLOCK，返回 HTTP 422 + BlockResponse（阻断原因列表含审核阻断原因），跳过步③。若最新审核结果为 WARN 或更低，进入步③。
     步③ forceSubmit 判定（仅在步①②均通过且最新审核结果为 WARN 时生效）：
       - forceSubmit=false：执行常规审核流程（若无最新审核结果则先调用审核）；若已有 WARN 级别最新审核结果，对当前处方做轻量级内容变更检测（drugId+dose+frequency+duration+route 五字段结构化哈希比对，与 AuditRecord.originalPrescription 的哈希比较），若检测到内容变更则返回 RX_AUDIT_PRESCRIPTION_MODIFIED 错误码并提示"处方内容已变更，请重新审核"，防止返回基于旧处方的已过期审核结果；若哈希一致则直接返回当前 WARN 审核结果并提示医生选择强制提交或修改处方，避免无意义重复审核
       - forceSubmit=true：校验 auditRecordId 对应 AuditRecord.riskLevel=WARN 且 isLatest=true
         + 处方版本校验（当前处方与 originalPrescription 一致）
         → 校验通过：写入 forceSubmitted/forceSubmitTime → 处方落单
         → 校验失败：返回错误码
       - 最新审核结果为 PASS 或无审核结果：直接执行常规提交流程，forceSubmit=true 在此场景下无效（返回 RX_AUDIT_FORCE_SUBMIT_INVALID 错误码）

    阻断合并语义：步①和步②的阻断合并仅在超时/竞态场景下发生——若步①判定为无 CRITICAL 后并发写入 CRITICAL 且步②同时判定为 BLOCK，响应中同时包含剂量告警阻断原因和审核阻断原因。正常时序下步①先于步②，步①阻断则不执行步②，阻断原因不合并。

    阻断竞态防护手段：为消除步①判定通过后到步③执行前 PrescriptionDraftContext 可能被并发写入 CRITICAL 的风险，步③（forceSubmit 判定）执行前增加二次 CRITICAL 验证——在步②通过后、步③执行前，重新查询 PrescriptionDraftContext 中该 prescriptionId 的 CRITICAL 告警列表，与步①检查时的快照做比对。若发现新增 CRITICAL 告警，则立即中止提交流程并返回阻断响应。二次验证在 forceSubmit=false 路径下同样生效——哈希比对前先做二次 CRITICAL 检查，确保提交时刻的处方安全状态与步①检查时一致。

同一处方多次审核：
  每次审核写入 AuditRecord 时，同一 prescriptionOrderId 下已有记录 isLatest→false
  新记录 auditSequence 递增、isLatest=true
```

### 4.3 病历生成场景

```
POST /api/medical-record/generate
{ dialogueText, patientId, encounterId, stream }
超时配置: ai.timeout.medical-record-generate=12s（非流式）
VisitFacade 超时配置: medical-record.visit-facade.timeout=2s（默认）

stream=false（Phase 2/3 仅支持非流式）:
  正常: VisitFacade.findVisitIdByEncounterId(encounterId) → TemplateConfigManager.getTemplate → AiService.generateMedicalRecord → MissingFieldDetector
  VisitFacade 降级: 超时/异常时依次尝试 (a) encounterId 直接作为 visitId fallback → 继续后续流程；(b) 返回 MR_GEN_VISIT_NOT_FOUND + 已生成部分病历内容
  兜底: 科室不存在时使用 DEFAULT 模板
  缺失判定: MissingFieldDetector 采用"基于字段值的非空非 null 存在性"策略——字段不存在、null、空字符串均视为缺失
  超时降级: AiResult.data（使用现有 data 字段承载部分结果）携带部分生成结果
    → MedicalRecordService 提取已生成部分字段 → 保留已生成字段 + 标记缺失字段
    → 返回部分 RecordGenerateResponse + missingFieldHints + MR_GEN_AI_TIMEOUT 标记
    → 前端: 已生成字段正常渲染 + 缺失字段显示补全提示输入框
   完全降级: AI 完全不可用 → 返回空字段集 + degraded=true
     → 差集比对 → 产出 FieldMissingHint 列表
     → 前端: degraded=true 显示"AI 病历生成暂时不可用"提示
     : degraded=false + 空字段 → 显示字段补全提示输入框

stream=true（Phase 2/3 暂不支持）:
  → 返回错误码 MR_GEN_STREAM_NOT_SUPPORTED
```

### 4.4 辅助开方场景

```
[主端点] POST /api/prescription/assist
请求: { diagnosis, examResults, patientInfo, existingPrescription, encounterId }
→ AssistConverter.toAiPrescriptionAssistRequest() 组装 ai-api 层 PrescriptionAssistRequest
→ 本地即时校验: 过敏冲突检查（PrescriptionAssistServiceImpl 复用 AllergyCheckRule 规则类——同模块内直接引用，无跨模块依赖；匹配逻辑与审计流程一致：结构化过敏信息精确匹配优先，缺省回退文本匹配）
→ DosageThresholdService 对处方草案中每项药品做剂量阈值校验
→ 返回 PrescriptionAssistResponse: { prescriptionDraft, doseWarnings, allergyWarnings, disclaimerRequired }

AI 返回无可推荐药品场景:
  AiResult.success=true 且 prescriptionDraft.drugs 为空列表
  → 返回空 prescriptionDraft + 本地校验结果 + PrescriptionAssistResponse.errorCode=RX_ASSIST_AI_NO_RECOMMENDATION
  → 前端展示"AI 暂无可推荐药品，请手动开方"

[即时校验子端点] POST /api/prescription/assist/check-dose
请求: { prescriptionId, drugCode, dosage, unit, routeOfAdministration, patientAge, patientWeight, frequency }
→ 单位一致性校验
→ DosageThresholdService.check(drugCode, route, dosage, age, weight, frequency)
→ CRITICAL 告警写入 PrescriptionDraftContext（key=prescriptionId，全量覆盖——每次重算后清除该 prescriptionId 旧标记，写入当前校验结果中的 CRITICAL 告警列表；若当前结果无 CRITICAL 告警则清除对应条目）
→ 异步 AI 去重检查：查询该 prescriptionId 下最近 AiSuggestionResult——若存在 PENDING 或 COMPLETED 未消费的 task，不创建新 task，复用已有 taskId 返回；仅当前次 task 为 FAILED 或 COMPLETED 已消费时创建新 task
→ 生成 taskId（UUID v4，36 字符含连字符——若去重检查命中则复用已有 taskId 而非新生成），作为异步 AI 建议查询标识
→ 以 PENDING 状态预创建 AiSuggestionResult（key=taskId，createTime=now，consumed=false）
→ 返回 DosageCheckResponse: { alerts, taskId, contextCriticalCount（Integer——当前 prescriptionId 下 PrescriptionDraftContext 中 CRITICAL 告警数量，供前端判断是否需要同步刷新状态，0 表示无 CRITICAL 告警） }
→ 异步 AI 调用完成后更新 AiSuggestionResult 状态为 COMPLETED 或 FAILED；若去重检查命中已有 COMPLETED 结果，直接填充已有结果，不走异步 AI 调用
→ AiSuggestionResult TTL 60 分钟，由 ScheduledExecutorService 定期清理过期条目
→ 六级均未命中时 DosageAlert.errorCode=RX_ASSIST_DOSE_STANDARD_NOT_FOUND

[异步建议查询] GET /api/prescription/assist/suggestion/{taskId}
→ 不存在 / TTL 过期: RX_ASSIST_SUGGESTION_NOT_FOUND（前端按 TTL 过期处理策略自动重试——检查当前编辑 session 有效性后发起新 check-dose 调用获取新 taskId）
→ PENDING: 前端继续轮询（间隔 2s，最多 30s 超时后停止轮询并提示"AI 建议生成超时"）
→ COMPLETED: 返回完整 suggestion 内容，前端标记 consumed=true 通知后端
→ FAILED: 返回 failReason，前端停止轮询并显示失败提示
```

### 4.5 ai-api 层 DTO 与业务层 DTO 的映射机制

各模块 Converter 类负责业务层 DTO 与 ai-api 层 DTO 之间的映射/转换，转换操作在各 Service 实现类中显式调用。

#### TriageConverter

映射方向：consultation 业务层 DTO ↔ ai-api 层 TriageRequest / TriageResponse

ai-api 层 TriageRequest 扩展字段（与需求文档 3.4.1 对齐）：
- chiefComplaint（string，必填）——单轮时为主诉文本，多轮时为主诉+历次 QA 拼合后的完整推理 prompt
- additionalResponses（List\<AdditionalResponseItem\>，可选）——当轮新增追问回答；ai-api 层 AdditionalResponseItem 含 question / answer / answeredAt

ai-api 层 TriageResponse 扩展字段（与需求文档 3.4.1 对齐）：
- recommendedDepartments（List\<ai-api RecommendedDepartment\>，每项含 departmentId / departmentName / score）
- recommendedDoctors（List\<ai-api RecommendedDoctor\>，每项含 doctorId / doctorName / departmentId / availableSlotCount / score）——ai-api 层推荐医生为 AI 直接返回（若 AI 实现支持）；业务层在此基础上通过 DoctorFacade 补充/增强推荐医生列表
- reason（string，必填）
- matchedRules（List\<ai-api MatchedRuleItem\>，每项含 ruleId / ruleName / score）
- needFollowUp（bool，可选）
- followUpQuestion（string，可选）
- confidence（float，可选）
- degraded（bool，可选）
- sessionId（string，可选）
- correctedChiefComplaint（string，可选——主诉修正隐式路径，AI 在推理输出中识别到患者修正主诉语义时填充此字段；TriageServiceImpl 在 AiService.triage() 返回后检测此字段并写入 DialogueSession.correctedChiefComplaint；仅在 DialogueCreateRequest 未携带 correctedChiefComplaint 显式值时生效）

ai-api 层 RecommendedDepartment 扩展字段：departmentId、departmentName、score（原仅有 departmentName）

映射逻辑：
- TriageConverter.toAiTriageRequest(DialogueCreateRequest, DialogueSession)：将业务层请求转换为 ai-api TriageRequest，首轮仅映射 chiefComplaint，多轮时拼合完整上下文
- TriageConverter.toTriageResponse(AiResult\<ai-api TriageResponse\>, List\<RecommendedDoctor\>)：将 ai-api 响应转换为业务层 TriageResponse，需补充 DoctorFacade 查询的推荐医生列表

#### AuditConverter

映射方向：prescription 业务层 DTO ↔ ai-api 层 PrescriptionCheckRequest / PrescriptionCheckResponse

ai-api 层 PrescriptionCheckRequest 扩展字段（与需求文档 3.4.2 对齐）：
- prescriptionItems（List\<PrescriptionCheckItem\>，每项含 drugId / drugName / dose / frequency / duration / route）
- patientInfo（object，含 patientId / age / gender / allergyHistory / allergyDetails(List\<AllergyDetailItem\>) / comorbidities）
- prescriptionId（string，必填）

ai-api 层 PrescriptionCheckResponse 扩展字段（与需求文档 3.4.2 对齐）：
- riskLevel（enum：LOW / MEDIUM / HIGH）
- interactions（List\<DrugInteractionItem\>，每项含 drugPair / severity / description）
- alerts（List\<AlertItem\>，每项含 alertCode / alertMessage / severity）
- suggestions（List\<SuggestionItem\>，每项含 suggestionCode / suggestionText）

ai-api 层与业务层 DTO 命名空间区分：
- ai-api 层包名：com.aimedical.modules.ai.api.dto.prescription.PrescriptionCheckRequest/Response
- 业务层包名：com.aimedical.modules.prescription.dto.audit.AuditRequest/AuditResponse
- 二者名称不同、字段语义对应但结构有差异（如 ai-api 层 riskLevel=LOW/MEDIUM/HIGH，业务层 AuditRiskLevel=PASS/WARN/BLOCK）

映射逻辑：
- AuditConverter.toAiPrescriptionCheckRequest(AuditRequest)：将业务层 AuditRequest 转换为 ai-api PrescriptionCheckRequest，含 allergyDetails 转换
- AuditConverter.toAuditResponse(AiResult\<PrescriptionCheckResponse\>)：将 ai-api 风险等级 LOW/MEDIUM/HIGH 映射为业务层 PASS/WARN/BLOCK

#### AssistConverter

映射方向：prescription 业务层 DTO ↔ ai-api 层 PrescriptionAssistRequest / PrescriptionAssistResponse

ai-api 层 PrescriptionAssistRequest 扩展字段（与需求文档 3.4.10 对齐）：
- diagnosis（string，必填）
- examResults（List\<ExamResultItem\>，可选）
- patientInfo（object，含 patientId / age / gender / allergyHistory / allergyDetails / comorbidities）
- existingPrescription（object，可选，含 drugs 数组）
- encounterId（string，可选）

ai-api 层 PrescriptionAssistResponse 扩展字段（与需求文档 3.4.10 对齐）：
- prescriptionDraft（object，含 drugs 数组）
- doseWarnings（List\<DoseWarningItem\>）
- allergyWarnings（List\<AllergyWarningItem\>）
- disclaimerRequired（bool）

ai-api 层与业务层 DTO 命名空间区分：
- ai-api 层：com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistRequest/Response
- 业务层：com.aimedical.modules.prescription.dto.assist.PrescriptionAssistRequest/Response
- 二者同名但包名不同——实现时通过 import 语句明确区分，Converter 中的转换方法签名以不同类型参数消除混肴

#### MedicalRecordConverter

映射方向：medical-record 业务层 DTO ↔ ai-api 层 MedicalRecordGenRequest / MedicalRecordGenResponse

ai-api 层 MedicalRecordGenRequest 扩展字段（与需求文档 3.4.3 对齐）：
- dialogueText（string，必填）
- patientId（string，必填）
- encounterId（string，可选）
- stream（bool，可选）
- departmentId（string，可选，后端从 encounterId 关联获取后填入）

ai-api 层 MedicalRecordGenResponse 扩展字段（与需求文档 3.4.3 对齐）：
- chiefComplaint / symptomDescription / presentIllness / pastHistory / physicalExam / preliminaryDiagnosis / treatmentPlan（各 string）
- missingFields（List\<string\>）
- partialContent（object，可选，超时降级时携带部分生成结果）

映射逻辑：MedicalRecordConverter 将 ai-api MedicalRecordGenResponse 的各字段映射为业务层 RecordGenerateResponse 的 fields Map（以 MedicalRecordField 枚举为键）。

### 4.6 结构化 API 契约

#### 智能分诊端点

**POST /api/triage/consult**

请求 JSON 示例（首轮）：
```json
{
  "chiefComplaint": "胸痛伴气短3天",
  "patientId": "P001",
  "age": 45,
  "gender": "男",
  "sessionId": "550e8400-e29b-41d4-a716-446655440001",
  "ruleVersion": "v2.1",
  "ruleSetId": "RS001"
}
```

请求 JSON 示例（多轮追问）：
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440001",
  "additionalResponses": [
    {
      "question": "胸痛是否与活动相关？",
      "answer": "上楼时加重",
      "answeredAt": "2026-06-28T10:30:00"
    }
  ],
  "correctedChiefComplaint": "胸痛伴气短、上楼加重3天"
}
```

响应 JSON 示例：
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440001",
  "departments": [
    {"departmentId": "CARDIO", "departmentName": "心血管内科", "score": 0.92},
    {"departmentId": "RESP", "departmentName": "呼吸内科", "score": 0.45}
  ],
  "doctors": [
    {"doctorId": "D001", "doctorName": "张医生", "departmentId": "CARDIO", "availableSlotCount": 3, "score": 0.92}
  ],
  "reason": "根据症状分析，建议就诊心血管内科",
  "matchedRules": [{"ruleId": "R001", "ruleName": "胸痛分诊规则", "score": 0.85}],
  "needFollowUp": true,
  "followUpQuestion": "胸痛是否放射至左肩？",
  "confidence": 0.88,
  "degraded": false,
  "ruleVersionMismatch": false
}
```

**POST /api/triage/select-department**

请求 JSON 示例：
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440001",
  "departmentId": "CARDIO",
  "departmentName": "心血管内科"
}
```

#### 处方审核端点

**POST /api/prescription/audit**

请求 JSON 示例：
```json
{
  "prescriptionId": "RX-001-20260628",
  "prescriptionItems": [
    {"drugId": "DRG001", "drugName": "阿莫西林", "dose": 500, "frequency": "tid", "duration": 7, "route": "po"}
  ],
  "patientInfo": {
    "patientId": "P001",
    "age": 45,
    "gender": "男",
    "allergyHistory": "青霉素",
    "allergyDetails": [{"allergen": "青霉素", "reactionType": "皮疹", "severity": "MODERATE", "occurredAt": "2020-01-01"}],
    "comorbidities": ["高血压", "糖尿病"]
  }
}
```

响应 JSON 示例（BLOCK）：
```json
{
  "riskLevel": "BLOCK",
  "alerts": [
    {"alertCode": "ALLERGY_001", "alertMessage": "患者对阿莫西林有过敏史", "severity": "CRITICAL"}
  ],
  "interactions": [],
  "suggestions": [],
  "fromFallback": false
}
```

**POST /api/prescription/submit**

请求 JSON 示例（WARN 强制提交）：
```json
{
  "prescriptionId": "RX-001-20260628",
  "prescriptionItems": [
    {"drugId": "DRG001", "drugName": "阿莫西林", "dose": 500, "frequency": "tid", "duration": 7, "route": "po"}
  ],
  "forceSubmit": true,
  "auditRecordId": 1001
}
```

#### 病历生成端点

**POST /api/medical-record/generate**

请求 JSON 示例：
```json
{
  "dialogueText": "患者胸痛伴气短3天，上楼加重。既往高血压病史10年。",
  "patientId": "P001",
  "encounterId": "ENC001",
  "stream": false
}
```

响应 JSON 示例：
```json
{
  "fields": {
    "CHIEF_COMPLAINT": "胸痛伴气短3天",
    "SYMPTOM_DESCRIPTION": "上楼时加重，休息后缓解",
    "PRESENT_ILLNESS": "患者3天前无明显诱因出现胸痛...",
    "PRELIMINARY_DIAGNOSIS": "冠心病？",
    "TREATMENT_ADVICE": "建议完善心电图、心肌酶谱检查"
  },
  "missingFieldHints": [
    {"missingField": "PAST_HISTORY", "promptMessage": "既往史字段缺失", "suggestedAction": "请补充既往史信息"}
  ],
  "fromFallback": false,
  "degraded": false
}
```

#### 辅助开方端点

**POST /api/prescription/assist**

请求 JSON 示例：
```json
{
  "diagnosis": "上呼吸道感染",
  "examResults": [{"itemName": "WBC", "value": "12.5", "unit": "10^9/L", "referenceRange": "4-10", "status": "HIGH"}],
  "patientInfo": {
    "patientId": "P001",
    "age": 45,
    "gender": "男",
    "allergyHistory": "青霉素"
  },
  "encounterId": "ENC001"
}
```

响应 JSON 示例：
```json
{
  "prescriptionDraft": {
    "drugs": [
      {"drugId": "DRG002", "drugName": "头孢克肟", "dose": 100, "frequency": "bid", "duration": 5, "route": "po"}
    ]
  },
  "doseWarnings": [],
  "allergyWarnings": [{"drugId": "DRG002", "allergen": "青霉素", "severity": "HIGH"}],
  "errorCode": null,
  "disclaimerRequired": true
}
```

**POST /api/prescription/assist/check-dose**

请求 JSON 示例：
```json
{
  "prescriptionId": "550e8400-e29b-41d4-a716-446655440002",
  "drugCode": "DRG001",
  "dosage": 1000,
  "unit": "mg",
  "routeOfAdministration": "po",
  "patientAge": 45,
  "patientWeight": 70,
  "frequency": "tid"
}
```

响应 JSON 示例：
```json
{
  "alerts": [
    {"warningType": "OVER_SINGLE_DOSE", "alertLevel": "WARNING", "message": "单次剂量超过推荐范围", "drugCode": "DRG001", "currentDose": 1000, "suggestedValue": 500, "errorCode": null}
  ],
  "taskId": "660e8400-e29b-41d4-a716-446655440003",
  "contextCriticalCount": 0
}
```

#### HTTP 错误响应包装格式

所有 4xx/5xx 错误响应（BLOCK 阻断返回的 HTTP 422 除外）使用统一包装格式：
```json
{
  "errorCode": "TRIAGE_SESSION_EXPIRED",
  "message": "会话已超时，请重新开始分诊",
  "timestamp": "2026-06-28T10:30:00",
  "path": "/api/triage/consult",
  "details": {}
}
```

- HTTP 状态码：统一由 GlobalExceptionHandler 管理，与 BusinessException 体系的错误码联动
- Content-Type：`application/json; charset=UTF-8`
- 认证头：通过 Spring Security 统一管理，不在设计范围内
- **BLOCK 阻断（HTTP 422）**：使用独立的 BlockResponse 格式，不经过统一错误包装：
```json
{
  "blockCode": "RX_BLOCK_CRITICAL_DOSE",
  "reasons": [
    {"alertCode": "DOSE_CRITICAL_001", "message": "药品DRG001单次剂量1000mg超出安全上限（500mg）的2倍"}
  ],
  "blockTime": "2026-06-28T10:30:00"
}
```

#### API 契约获取方式

前端/QA 获取完整 API 契约的方式：(a) 开发环境下通过 SpringDoc OpenAPI（swagger-ui.html）自动生成接口文档；(b) 验收阶段从前端集成测试用例反向映射；(c) 本设计提供各端点的关键字段约束表（见 §1.3 各 DTO 定义）和 JSON 示例，作为契约参考基线；详细 JSON Schema 在 OpenAPI 注解中自动推导。

---

## 5. 错误处理策略

### 5.1 模块级错误码

**错误码命名规则**：AI 相关错误码遵循 `<前缀>_AI_<类型>` 命名约定（如 TRIAGE_AI_TIMEOUT、RX_AUDIT_AI_TIMEOUT），非 AI 业务逻辑错误码使用 `<前缀>_<类型>` 命名（如 TRIAGE_SESSION_EXPIRED、RX_ASSIST_DOSE_STANDARD_NOT_FOUND、RX_AUDIT_PRESCRIPTION_MODIFIED），二者的区分规则：凡涉及 AI 推理调用（超时、不可用、输入校验失败、无可推荐结果、输出不完整等）的错误码均保留 `_AI_` 中段，纯业务逻辑错误码不含 `_AI_`。

| 错误类别 | 前缀 | 代表场景 |
|---------|------|---------|
| 分诊（AI） | TRIAGE_ | TRIAGE_AI_TIMEOUT、TRIAGE_AI_UNAVAILABLE、TRIAGE_AI_INPUT_INVALID |
| 分诊（非AI） | TRIAGE_ | TRIAGE_SESSION_EXPIRED、TRIAGE_FIELD_COMBINATION_INVALID、TRIAGE_SESSION_NOT_FOUND、TRIAGE_DOCTOR_FACADE_UNAVAILABLE |
| 审核（AI） | RX_AUDIT_ | RX_AUDIT_AI_TIMEOUT、RX_AUDIT_AI_UNAVAILABLE、RX_AUDIT_AI_INPUT_INVALID |
| 审核（非AI） | RX_AUDIT_ | RX_AUDIT_PRESCRIPTION_MODIFIED（处方版本校验不一致）、RX_AUDIT_CONCURRENT_SUBMIT |
| 病历（AI） | MR_GEN_ | MR_GEN_AI_TIMEOUT、MR_GEN_AI_UNAVAILABLE、MR_GEN_AI_INPUT_INVALID、MR_GEN_AI_OUTPUT_INCOMPLETE |
| 病历（非AI） | MR_GEN_ | MR_GEN_STREAM_NOT_SUPPORTED、MR_GEN_VISIT_NOT_FOUND、MR_GEN_CONCURRENT_MODIFICATION、MR_GEN_TEMPLATE_LOAD_FAILED |
| 开方辅助（AI） | RX_ASSIST_ | RX_ASSIST_AI_TIMEOUT、RX_ASSIST_AI_UNAVAILABLE、RX_ASSIST_AI_INPUT_INVALID、RX_ASSIST_AI_NO_RECOMMENDATION、RX_ASSIST_AI_SUGGEST_CREATE_FAILED |
| 开方辅助（非AI） | RX_ASSIST_ | RX_ASSIST_DOSE_STANDARD_NOT_FOUND、RX_ASSIST_SUGGESTION_NOT_FOUND、RX_ASSIST_UNIT_MISMATCH |

### 5.2 AI 降级作为正常业务流程

各模块的本地规则回退视为正常业务流程。处方审核降级时 AuditRecord 的 fromFallback=true 标记用于区分，响应体同步携带 fromFallback。病历生成降级时 RecordGenerateResponse 同样携带 fromFallback 标记。

### 5.3 BLOCK 阻断作为独立错误类别

BLOCK 阻断响应通过 BlockResponse 封装，使用 HTTP 422 状态码，与业务异常体系正交。

### 5.4 与已有异常处理框架的集成

复用 GlobalExceptionHandler + ErrorCode 接口 + BusinessException 体系。BLOCK 阻断不经过异常框架，直接通过 Controller 返回 422 响应。

### 5.5 AI 超时配置

| AI 能力 | 配置键 | 阈值 | 来源 |
|--------|-------|------|------|
| 处方审核 | ai.timeout.prescription-audit | 6s | 需求文档 3.4.2 |
| 病历生成（非流式） | ai.timeout.medical-record-generate | 12s | 需求文档 3.4.3 |
| 病历生成（流式首字） | ai.timeout.medical-record-generate-stream | 2s | Phase 2/3 预留 |
| 病历生成（流式总时长） | ai.timeout.medical-record-stream-total | 30s | Phase 2/3 预留 |
| 病历生成（流式分片间隔） | ai.timeout.medical-record-stream-chunk-interval | 5s | Phase 2/3 预留 |
| 辅助开方 | ai.timeout.prescription-assist | 10s | 需求文档 3.4.10 |
| 智能分诊 | ai.timeout.triage | 8s | 需求文档 3.4.1 |
| 分诊上下文截断阈值 | triage.max-context-chars | 3000 | 对话上下文累计字符数阈值，超出后截断中间轮次 |

---

## 6. 并发设计

### 6.1 对话会话并发管理

ConcurrentHashMap 存储，同 session 请求串行（前端等待响应），不同 session 独立。ScheduledExecutorService 每 5 分钟扫描清理超时 30 分钟的 session。

**TTL 清理竞态处理**：ConcurrentHashMap.remove(key) 为原子操作——若定时清理线程先于业务请求线程执行 remove，业务线程发现 session 不存在后返回 TRIAGE_SESSION_EXPIRED 错误由前端处理，此竞态路径被覆盖，无需额外加锁。

**部署约束与水平扩展**：Phase 2/3 假设单实例部署或 sticky session（通过负载均衡器配置），ConcurrentHashMap 存储在此假设下可正常工作。若需多实例部署，三项内存存储（DialogueSessionManager 会话存储、AiSuggestionResult 存储、PrescriptionDraftContext）均需替换为分布式缓存（如 Redis）。Phase 5 迁移节点：三项内存存储统一迁移至持久化或分布式缓存，届时仅需替换 Store 接口的底层实现类：
- SessionStore：ConcurrentHashMapStore → RedisSessionStore（Redis Hash + TTL）
- SuggestionStore：ConcurrentHashMapStore → RedisSuggestionStore（Redis String + TTL）
- DraftContextStore：ConcurrentHashMapStore → RedisDraftContextStore（Redis Hash + TTL）
此迁移在 Phase 5 AI 进阶底座落地时同步执行，与"Phase 2~4 各阶段 AI 能力统一迁移至底座"的路线图一致。Phase 2/3 的设计强制项：三项存储必须通过 Store 接口（SessionStore、SuggestionStore、DraftContextStore，均定义在 common-module-api/com.aimedical.modules.commonmodule.store/ 包下）间接访问，业务 Service 代码只依赖 Store 接口。Phase 2/3 的 ConcurrentHashMapStore 实现和 Phase 5 的 RedisStore 实现均实现同一组 Store 接口，业务 Service 在接口不变的前提下无需修改。

### 6.2 AI 调用并发

统一 CompletableFuture<AiResult<T>>，Service 层同步等待 AI 结果。

### 6.3 包E 的异步 AI 建议与去重

@Async / CompletableFuture.runAsync() 调用 AiService.prescriptionAssist()。主响应返回 taskId 时预创建→更新模式与前一版一致。

**prescriptionId 级去重**：同一 prescriptionId 下存在 PENDING 状态的 task 时不重复创建新 AI 调用；存在 COMPLETED 且未被前端消费（consumed=false）的 task 时复用已有结果；仅 FAILED 或已消费的结果触发新 task 创建。实现封装为 DedupTaskScheduler 辅助类（com.aimedical.modules.prescription.service.assist.DedupTaskScheduler），内部依赖 SuggestionStore 接口进行 AiSuggestionResult 的存取；去重检查为同步前置操作（在主响应线程中执行，DosageCheckResponse 返回前完成），异步 AI 调度在去重通过后触发。

**前端节流配合**：前端对 check-dose 调用做 300ms 防抖（Debounce），合并用户连续剂量调整请求为单次校验调用，减少后端去重负担。后端限流（如计数器/令牌桶）作为可选补充层，Phase 2/3 暂不实现。

### 6.4 处方草稿上下文并发管理

PrescriptionDraftContext 按 prescriptionId 关联，内部使用 ConcurrentHashMap 存储。处方提交成功/处方取消/TTL 过期（60 分钟）后清理对应条目。

---

## 7. 设计决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 模块粒度 | 3 个模块 | 包C、包D-AI2 独立；包D-AI1 与包E 共享数据合并 |
| 模块结构 | 扁平模块 | 与已有模块一致 |
| 多轮对话存储 | 内存（ConcurrentHashMap + TTL 30 分钟） | 对话短时完成，Phase 5 迁移分布式缓存 |
| 分诊规则源 | 数据库 TriageRule 实体 + Caffeine 定时缓存刷新 | 支持非开发人员动态调整，热加载无需重启 |
| 推荐医生列表生成 | AI 返回科室→后端通过 DoctorFacade 查询医生排班 | AI 聚焦科室级推理，医生匹配基于实时排班数据，availableSlotCount 时效性有保障；DoctorFacade 解耦 consultation 与 doctor 模块 |
| 本地规则形态 | 多条独立规则（LocalRuleEngine 链） | 独立实现/测试，开闭原则 |
| 科室模板配置 | 数据库 + TemplateConfigManager | 差异化配置，管理界面调整 |
| 包E AI 调用时机 | 剂量同步 + AI 异步 + 查询端点 | 即时性分级，异步不阻塞 |
| MissingFieldDetector | 差集比对检测模式 | 避免自动补全引入错误，保留已提取字段完整性 |
| 病历降级策略 | 分层保护（保留已提取字段，仅标记缺失） | 降级场景下已提取字段不丢失 |
| DosageStandard 位置 | common 模块 | 避免跨模块编译期依赖 |
| 科室模板兜底 | DEFAULT 兜底 | 新科室未配置时仍可生成基础病历 |
| 模板缓存刷新 | 定时刷新 + 事件驱动失效 | 最终一致性，管理更新可立即生效 |
| BLOCK 阻断执行 | PrescriptionAuditEnforcer 策略接口 | 阻断行为可定制，不侵入审核核心流程 |
| BLOCK 响应形式 | HTTP 422 + BlockResponse | 与业务异常体系正交 |
| DosageStandard 年龄/体重分级 | 内联年龄范围+体重范围字段，五级匹配优先级 | 避免子实体增加查询复杂度 |
| 剂量单位分组 | DosageUnitGroup 枚举（MASS/VOLUME/IU） | 换算规则集中定义 |
| 配置变更审计 | ConfigChangeLog 实体 + @TransactionalEventListener(phase=AFTER_COMMIT) | 事务提交后发布事件，避免事务回滚时事件已发送；Spring ApplicationEvent 在 application 模块聚合后可跨模块传播；TemplateConfigManager 定时刷新覆盖补偿事件丢失 |
| 病历内容存储 | 单列 JSON TEXT（contentJson + JPA @Convert/Jackson） | 避免 7 个字段频繁 DDL 变更，与枚举扩展性一致 |
| prescription 文本存储 | AuditRecord.originalPrescription 为 JSON TEXT | 完整处方快照用于 WARN 强制提交时的版本校验 |
| ai-api 层 DTO 与业务层 DTO 映射 | 各模块 Converter 类负责 | 职责明确——Converter 封装字段映射/类型转换/命名空间转换逻辑，Service 仅调用 Converter 方法 |
| ai-api 层与业务层同名 DTO 区分 | 不同包名 + Converter 显式类型参数 | PrescriptionCheckRequest/Response 和 PrescriptionAssistRequest/Response 在 ai-api 层和业务层同名但包名不同——com.aimedical.modules.ai.api.dto.prescription vs com.aimedical.modules.prescription.dto.audit/assist；Converter 方法签名以不同类型参数消除混肴 |
| allergy_details 扩展容器 | 业务层 patientInfo 增加 allergyDetails 可选字段（List\<AllergyDetail\>）；AllergyCheckRule 优先结构化匹配，缺省回退文本匹配 | 对齐需求文档 3.1.6 过渡方案——结构化过敏信息优先用于精确匹配区分严重程度（SEVERE→BLOCK, MODERATE/MILD→WARN），缺省回退文本匹配按保守策略一律 BLOCK |
| /assist AI 返回无可推荐药品 | 返回空 prescriptionDraft + RX_ASSIST_AI_NO_RECOMMENDATION 标记 | AI 明确判断无推荐不等同于调用失败，属有效结果 |
| 多轮分诊上下文传递 | 全量拼接策略——每次调用 AiService.triage() 将完整上下文组装传入 ai-api TriageRequest | AI 无状态化，不依赖自身侧会话状态；全量拼接确保推理一致性 |
| 推荐医生数据来源 | 后端通过 DoctorFacade 查询医生排班数据生成 | availableSlotCount 实时性保障；AI 聚焦科室级推理，医生匹配由后端基于排班完成 |
| TriageRecord 写入时机 | 返回响应前同步写入，finalDepartmentId 初始为空，患者挂号后通过事件补充写入 | 分诊结果可追溯；最终科室选择在挂号环节确定 |
| sessionId 必填/可选语义 | 首轮由前端生成 UUID v4 传入，消除"首轮为空"特殊分支 | 对齐需求文档 3.4.1 session_id 必填语义；前端生成消除服务端首轮/后续分支差异 |
| 处方提交端点 | POST /api/prescription/submit，Phase 2/3 简要契约定义 | BLOCK 阻断和 WARN 留痕的端到端闭环依赖此端点——forceSubmit=true + auditRecordId + 处方版本校验实现闭环 |
| WARN 级强制提交前后端交互 | 处方提交端点增加 forceSubmit(bool) + auditRecordId 参数 | 前端将医生"强制提交"选择通过提交端点显式传递后端；auditRecordId + originalPrescription 版本校验防时序竞态 |
| WARN 强制提交时序竞态防护 | AuditRecord.originalPrescription(JSON TEXT 快照)+ 提交时比较当前处方与快照一致性 | 确保审核后处方未被篡改；不一致则要求重新审核 |
| LocalRuleResult.severity 类型 | AuditRiskLevel 枚举（值域 WARN/BLOCK） | 与最终风险等级判定类型一致，消除类型转换 |
| AllergyCheckRule 严重程度分级 | allergyDetails 存在时分 SEVERE→BLOCK / MODERATE+MILD→WARN；缺省回退一律 BLOCK | 需求文档 §3 过渡方案预留结构化过敏信息，精确匹配优于保守一致 BLOCK |
| DosageLimitRule 严重程度分级 | 超标 ≥ 2 倍→BLOCK / 超标但 < 2 倍→WARN | 区分严重超标与轻度超标，与临床风险判断一致 |
| 撤销审核 isLatest 处理 | 撤销后 isLatest 回退为 false | 确保后续查询最新审核不返回已撤销记录；已撤销 AuditRecord 仍持久化保存 |
| 内存存储部署约束 | Phase 2/3 假设单实例/sticky session；Phase 5 迁移分布式缓存 | 三项 ConcurrentHashMap 存储在水平扩展时需全量迁移 |
| TriageRule 匹配模型 | match(chiefComplaint, ruleVersion, ruleSetId) → List\<RecommendedDepartment\> | 规则按规则集+版本组织；支持版本化管理和热加载 |
| AiSuggestionResult partialData | 增加 partialData 字段（可选，JSON TEXT） | 非流式超时降级时携带部分生成结果，与病历分层保护策略一致 |
| AiResult 超时降级模式 | 使用现有 AiResult.data 字段承载部分结果（不新增 partialData 字段）；新增 AiResult.failure(String errorCode, T partialData) 重载覆盖超时场景（failure+errorCode+partialData）；新增 AiResult.degraded(String fallbackReason, T partialData) 重载覆盖降级场景（success=false + degraded=true + data 含部分结果 + fallbackReason） | 需求文档 §3.4.3 超时场景需同时携带 errorCode 和部分数据（partial_content）；现有 failure() 和 degraded() 工厂方法均将 data 设为 null，新增重载使 data 在 failure/degraded 路径可承载部分结果；使用现有 data 字段而非新增 partialData 避免冗余字段 |
| DrugCompositionDict/DrugAllergyMapping/DrugContraindicationMapping 持久化 | JPA @Entity（entity/ 包下）+ Repository + 种子数据脚本 | 与 TriageRule 实体一致的数据实体模式 |
| 检查项 #2 职责拆分 | AllergyCheckRule + ContraindicationCheckRule | 需求文档 3.4.2 检查项 #2 含两个子要求——过敏冲突与合并症禁忌，拆分为独立规则便于独立实现/测试/分级；AllergyCheckRule 按过敏严重程度分级，ContraindicationCheckRule 按禁忌级别（绝对/相对）分级 |
| TriageRecord 推荐医生快照 | recommendedDoctors 字段（JSON TEXT）存储 TriageResponse.doctors 列表快照 | 对齐需求文档 §5.1 分诊记录核心字段"推荐医生"；确保推荐医生数据的可观测性和事后追溯完整性 |
| RegistrationEvent 事件契约 | 事件定义在 common-module-api 中，registration 模块发布、consultation 模块消费 | 跨模块事件通过门面接口层解耦；与 DoctorFacade/UserFacade 模式一致——接口定义在 common-module-api，实现在各业务模块，application 聚合后跨模块传播 |
| AuditAlert.severity 类型 | 独立 AlertSeverity 枚举（INFO / WARNING / CRITICAL），与 AuditRiskLevel 是不同维度 | AuditRiskLevel 表征处方整体风险等级（3 级），AlertSeverity 表征单条提示的严重程度（3 级）；二者语义不同不应复用同一枚举，避免混淆 |
| DosageAlert 错误码 | DosageAlert 增加 errorCode 可选字段 | RX_ASSIST_DOSE_STANDARD_NOT_FOUND 等错误码通过 DosageAlert.errorCode 统一承载，在 check-dose 流程中明确传递路径 |
| 处方版本校验一致性语义 | 按业务字段做结构化比较（drugId + dose + frequency + duration + route 五字段组合比对） | 忽略 JSON 文本级格式差异（字段顺序、null 与缺失字段、数值精度等）；药品增删/剂量变化等业务实质变更判为"不一致"，仅 JSON 序列化格式差异判为"一致" |
| allergyHistory/allergyDetails 数据来源优先级 | allergyHistory 由后端从健康档案实体拼接（single source of truth）；allergyDetails 后端优先从健康档案实体提取，前端传入值仅作为 fallback/离线场景覆盖 | 对齐需求文档 §3.1.6 过渡方案三层行为——(1) allergy_history 后端拼接；(2) allergy_details 默认缺省；(3) 前端存入但后端优先提取；确保数据来源一致性 |
| 辅助开方过敏告警与处方审核过敏检查关系 | 辅助开方 allergyWarnings 为即时提示性质，处方审核 AllergyCheckRule 为提交时正式审核判定，二者独立执行不互斥；allergyWarnings.severity=HIGH 不直接等价于 AuditRiskLevel=BLOCK | 二者分属不同模块、不同时机、不同判定维度，独立执行确保双重校验的互补性 |
| 错误码 AI/非 AI 分类规则 | AI 相关错误码含 `_AI_` 中段（如 TRIAGE_AI_TIMEOUT），非 AI 业务逻辑错误码不含 `_AI_` 中段（如 TRIAGE_SESSION_EXPIRED） | 对齐需求文档 §3.4 错误码命名约定——"所有错误码必须保留 _AI_ 中段以与非 AI 错误码区分"；非 AI 错误码按 `<前缀>_<类型>` 命名 |
| matched_rules 子字段设计 | 设计侧主动定义 ruleId / ruleName / score 三个子字段 | 需求文档 §3.4.1 matched_rules 字段后无子结构定义（需求侧缺口）；设计侧主动定义三个子字段，其中 score 统一承载匹配评分；若后续需求补充 confidence / reason 子字段，可通过 DTO 扩展兼容 |
| DoctorFacade 包名 | commonmodule/doctor/ 子包（与 auth 包解耦） | DoctorFacade 是跨模块医生排班/可用性查询门面，与用户认证（auth）职责不相关；独立子包使接口语义定位与包名一致 |
| 底座落地与 Phase 5 迁移兼容性 | 业务模块仅依赖 ai-api 的 AiService 接口（编译期依赖）；若 Phase 5 保持 AiService 接口签名不变，业务模块的核心 Service 和 Controller 的业务逻辑代码无须修改；内存存储依赖部分必须通过 Store 抽象层（SessionStore、SuggestionStore、DraftContextStore 接口）隔离存储实现——Phase 2/3 使用 ConcurrentHashMapStore，Phase 5 替换为 RedisStore，业务 Service 代码不感知存储变更 | 接口不变是迁移关键前提——AiService 接口定义在 ai-api 模块，业务模块通过接口调用 AI 能力，实现类替换对业务模块透明；三项内存储存（DialogueSessionManager、AiSuggestionResult、PrescriptionDraftContext）必须引入 Store 抽象层（SessionStore、SuggestionStore、DraftContextStore），Phase 2/3 用 ConcurrentHashMapStore 实现，Phase 5 替换为 RedisStore 实现，Store 抽象层为设计强制项而非建议；业务 Service 代码仅依赖 Store 接口，存储实现替换对业务层透明；若 Phase 5 需扩展 DTO 字段，业务模块的 Converter 需同步更新 |
| TTL 清理竞态 | ConcurrentHashMap.remove() 原子性保证下若 session 已被清理，findOrCreate() 返回 TRIAGE_SESSION_EXPIRED 错误 | 竞态路径被覆盖——清理先于访问则视为 session 已超时，访问先于清理则使用已获取的引用 |
| CRITICAL 剂量告警阻断链路 | PrescriptionDraftContext 按 prescriptionId 全量覆盖存储 CRITICAL 告警；每次 check-dose 重算后覆盖写入；提交端点提交前检查 PrescriptionDraftContext 中是否存在 CRITICAL 告警 | CRITICAL 阻断与审核 BLOCK 阻断分属不同判断维度，任一命中均拒绝提交，确保处方安全闭环；全量覆盖语义避免旧 CRITICAL 残留 |
| OVER_DURATION 实现范围 | Phase 2/3 预留暂不实现，DosageThresholdService 中标记为预留路径 | 疗程剂量超限检查依赖疗程持续时间参数和累积剂量计算，业务规则复杂度较高，Phase 2/3 核心需求优先级聚焦单次剂量和日剂量，疗程剂量推迟至 Phase 4 实现 |
| 规则快照失效处理 | match() 使用快照版本查询无结果时降级使用当前最新版本规则集重新匹配 | 避免因管理员删除/禁用规则集导致分诊完全失败；ruleVersionMismatch 标记供前端提示 |
| 降级时前端行为 | TriageResponse.degraded=true 时前端仍渲染推荐科室列表 + 降级提示文案 + "手动选择科室"入口 | 对齐需求文档 3.1.3.1 / §6.3"回退到按科室选择医生模式"降级路径 |
| 全量拼接 Token 超限防护 | 对话上下文累计字符超预配置阈值（默认 3000 字符）时触发"保留首轮+最近 N 轮"截断策略 | Phase 2/3 分诊通常 2-5 轮，发生概率低，但预留可配置参数 `triage.max-context-chars` 为后续扩展做准备 |
| 病历降级前端区分 | RecordGenerateResponse 增加 degraded 布尔字段：true=AI不可用/降级，false=AI明确返回空 | 前端根据 degraded 值展示不同提示文案——降级时提示"AI病历生成暂时不可用"而非字段补全 |
| prescriptionId/prescriptionOrderId 语义 | prescriptionId 系统层 UUID，贯穿草稿→审核→提交全流程；prescriptionOrderId 提交成功后分配的业务单据号 | BLOCK 阻断时仅有 prescriptionId 无 prescriptionOrderId；AuditRecord 保留二者关联追溯 |
| aiFailCount 持久化策略 | 不持久化、不跨会话/跨重启保持 | aiFailCount 仅服务于当前对话会话生命周期，跨会话无业务意义；全局 AI 可用性由基础设施层监控 |
| RegistrationEvent sessionId 传播机制 | 前端侧传递——分诊结束后前端保留 sessionId，进入挂号流程时作为参数传入 registration 模块 | consultation 与 registration 模块不允许互相依赖，sessionId 通过前端跨模块传递，zero 后端依赖 |
| VisitFacade 降级保护 | 独立超时阈值 2s + 降级路径：(a) encounterId 作为 visitId fallback (b) 返回 MR_GEN_VISIT_NOT_FOUND + 部分病历内容 | 与 DoctorFacade 对称的降级保护设计，确保 VisitFacade 不可用时病历已生成字段不丢失 |
| FieldMissingHint 内容来源 | 基于 DepartmentTemplateConfig 中每字段的预定义提示模板（promptMessage + suggestedAction） | 由管理员在科室模板中配置提示内容，MissingFieldDetector 差集比对后读取模板组装提示；未配置时使用默认文案 |
| MedicalRecord.contentJson 并发写保护 | MedicalRecord 实体增加 @Version 乐观锁字段，更新操作使用版本号校验，写冲突时返回 MR_GEN_CONCURRENT_MODIFICATION 错误码 | "单列 JSON TEXT + 读取→合并→写回"模式的并发防护，防止后提交写入覆盖前一个变更 |
| Phase 23 OOD 草案继承关系 | 本设计继承自 Phase 23 OOD 草案（a_v19）并经历 23 轮修订 | 核心架构决策（三模块划分、扁平模块、强耦合策略、ai-api 模式、备忘录模式、本地规则链）与草案保持一致；Store 抽象层从"建议"升级为"强制"、AiResult 超时降级模式收敛等为定向演进方向 |

---

## 8. 剂量标准初始化与药品编码规范

### 8.1 初始化方案

SQL 种子脚本：
- backend/modules/prescription/src/main/resources/db/seed/R__dosage_standards.sql
- backend/modules/prescription/src/main/resources/db/seed/R__drug_allergy_mapping.sql
- backend/modules/prescription/src/main/resources/db/seed/R__drug_contraindication_mapping.sql
- backend/modules/prescription/src/main/resources/db/seed/R__drug_composition_dict.sql

使用 MERGE / INSERT ... ON DUPLICATE KEY UPDATE 实现幂等执行。

同优先级多条记录检测策略：当同 drugCode + routeOfAdministration 在相同匹配优先级（如同时命中年龄范围匹配）存在多条记录时，DosageThresholdService 按以下规则选择——(a) 优先选择 updateTime 最新的记录；(b) 记录匹配过程日志（包含命中优先级、匹配记录数、最终选择记录标识）。此场景属于管理员数据维护异常，admin 模块在保存 DosageStandard 时应校验同一优先级层级的唯一性并给出告警。

### 8.2 药品编码规范

DosageStandard.drugCode 采用国药准字号，关联药品基础信息实体（drug_base_info，位于 drug/master 模块）。关联方式：DosageStandard.drugCode 作为逻辑外键引用 drug_base_info.drugCode，不建立 JPA @ManyToOne 物理关联以避免 prescription 模块对 drug 模块的编译期依赖。跨模块的药品名称/规格信息查询通过 Service 层按需调用 DrugFacade（定义在 common-module-api 中）获取。DosageStandard 的写入端（admin 模块）和读取端（prescription 模块）均通过 drugCode 与药品基础信息实体做业务关联。

### 8.3 单位一致性校验

DosageUnitGroup 枚举定义如下单位分组及组内换算系数：

| 枚举值 | 包含单位 | 基准单位 | 组内换算系数 |
|--------|---------|---------|-------------|
| MASS_GROUP | mg, g, kg, mcg(μg) | mg | 1 mg = 1mg；1 g = 1000mg；1 kg = 1,000,000mg；1 mcg(μg) = 0.001mg |
| VOLUME_GROUP | ml, l | ml | 1 ml = 1ml；1 l = 1000ml |
| IU_GROUP | IU, kIU | IU | 1 IU = 1IU；1 kIU = 1000IU |

跨组单位比较规则：当 DosageCheckRequest 中的单位与 DosageStandard 中的单位不属于同一 DosageUnitGroup 时，DosageThresholdService 无法进行剂量换算比较，统一输出 DosageAlert（warningType=OVER_SINGLE_DOSE，alertLevel=WARN）并携带 errorCode=RX_ASSIST_UNIT_MISMATCH，提示"单位类型不匹配，无法进行剂量校验"。

### 8.4 年龄/体重分级剂量支持

DosageStandard 实体分级字段定义：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| ageRangeStart | Integer（nullable） | 适用年龄下限（含，单位：岁） |
| ageRangeEnd | Integer（nullable） | 适用年龄上限（含，单位：岁） |
| weightRangeStart | BigDecimal（nullable） | 适用体重下限（含，单位：kg） |
| weightRangeEnd | BigDecimal（nullable） | 适用体重上限（含，单位：kg） |
| dailyMax | BigDecimal（nullable） | 日剂量上限 |
| singleMax | BigDecimal | 单次剂量上限 |
| unit | String | 剂量单位，值域限定为 DosageUnitGroup 枚举中各分组的单位值（如 mg、g、ml、IU 等）；DosageThresholdService 单位校验时按 DosageUnitGroup 分组做跨组换算检查 |

六级匹配优先级（DosageThresholdService.check() 按优先级 1→2→3→4→5 依次尝试，命中即返回，不再继续检查更低优先级）：

1. **精确匹配**：ageRangeStart = ageRangeEnd = 患者年龄 AND weightRangeStart = weightRangeEnd = 患者体重
2. **同时范围匹配**：ageRange 与 weightRange 均非 null 但未达到精确匹配——检查 ageRangeStart ≤ 患者年龄 ≤ ageRangeEnd AND weightRangeStart ≤ 患者体重 ≤ weightRangeEnd，两项同时命中时使用该记录；若任一项未命中则继续检查下一优先级
3. **年龄范围匹配**：ageRangeStart ≤ 患者年龄 ≤ ageRangeEnd，且 weightRange 均为 null
4. **体重范围匹配**：weightRangeStart ≤ 患者体重 ≤ weightRangeEnd，且 ageRange 均为 null
5. **无分级默认阈值**：ageRange 和 weightRange 均为 null（代表该药品/给药途径的通用剂量上限）
6. **标准不存在**：五级均未命中 → 降级返回 WARN 级 DosageAlert 并携带 errorCode=RX_ASSIST_DOSE_STANDARD_NOT_FOUND

---

## 9. 科室模板初始数据集与模板管理接口

### 9.1 初始模板数据集

DEFAULT 模板的必填字段列表如下（MedicalRecordField 枚举）：

| MedicalRecordField | 是否必填 | 说明 |
|--------------------|---------|------|
| CHIEF_COMPLAINT | 必填 | 主诉 |
| SYMPTOM_DESCRIPTION | 必填 | 症状描述 |
| PRELIMINARY_DIAGNOSIS | 必填 | 初步诊断 |
| TREATMENT_ADVICE | 必填 | 治疗意见 |
| PRESENT_ILLNESS | 可选（DEFAULT 模板建议必填） | 现病史，留作科室自定义空间 |
| PAST_HISTORY | 可选（DEFAULT 模板建议必填） | 既往史，同上 |
| PHYSICAL_EXAM | 可选 | 体格检查，同上 |

科室级模板可覆盖 DEFAULT 的必填配置。TemplateConfigManager.getTemplate() 在科室标识不存在时返回 DEFAULT 模板。

### 9.2 模板管理接口定义

模板 CRUD 由 admin 模块管理界面完成。模板更新后通过 ApplicationEventPublisher 发布 TemplateConfigChangeEvent。

### 9.3 规则管理接口定义

规则管理覆盖以下实体：TriageRule（分诊规则）、DrugContraindicationMapping（药品禁忌症映射）、DrugAllergyMapping（药物过敏映射）、DrugCompositionDict（药品成分字典）。DrugInteractionPair（药物相互作用数据）相关的管理接口属于 Phase 4 范围，本阶段不实现。各实体的管理接口由 admin 模块 OOD 文档独立定义，本处定义各接口的简要契约：

| 接口 | 端点 | 说明 |
|------|------|------|
| 规则 CRUD | POST/PUT/DELETE /api/admin/rules/{entityType} | entityType 区分各规则实体类型 |
| 规则集发布 | POST /api/admin/rules/{entityType}/publish | 将指定规则集的待发布版本标记为当前生效版本 |
| 规则集回滚 | POST /api/admin/rules/{entityType}/rollback/{version} | 回滚至指定历史版本 |
| 规则版本查询 | GET /api/admin/rules/{entityType}/versions | 返回指定规则集的所有版本列表 |
| 配置变更审计日志 | GET /api/admin/audit/change-log | 查询 ConfigChangeLog 记录 |

规则变更后通过 ApplicationEventPublisher 发布对应变更事件（TriageRuleChangeEvent、DrugContraindicationChangeEvent、DrugAllergyMappingChangeEvent、DrugCompositionDictChangeEvent；DrugInteractionPair 相关的 CRUD 和变更事件属于 Phase 4 范围），消费端（如 DefaultTriageRuleEngine、LocalRuleEngine 中各规则类）监听后刷新缓存。TemplateConfigChangeEvent 与规则变更事件复用同一事件发布/监听框架。

### 9.4 配置变更审计日志

ConfigChangeLog 实体完整字段定义：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Long（自增主键） | 审计记录标识 |
| entityType | String | 变更实体类型（枚举值：TRIAGE_RULE / DRUG_CONTRAINDICATION / DRUG_ALLERGY / DRUG_COMPOSITION / DRUG_INTERACTION / DOSAGE_STANDARD / TEMPLATE_CONFIG 等） |
| entityId | String | 变更实体标识（如 ruleId、drugCode） |
| changeType | enum | 变更操作类型（CREATE / UPDATE / DELETE / PUBLISH / ROLLBACK） |
| oldValue | JSON TEXT（nullable） | 变更前快照 |
| newValue | JSON TEXT（nullable） | 变更后快照 |
| operatorId | String | 操作人 ID |
| operatorName | String | 操作人姓名 |
| changeTime | LocalDateTime | 变更时间戳 |
| changeReason | String（nullable） | 变更原因（管理员填写，可选） |

**事务边界与事件发布保序**：规则/模板变更事件由各自的 admin 模块管理 Service 在事务内发布，使用 @TransactionalEventListener(phase=AFTER_COMMIT) 确保仅在事务成功提交后发布事件，避免事务回滚时事件已发送导致数据不一致。Spring ApplicationEvent 在 application 模块聚合后可跨模块传播——medical-record 模块的 TemplateConfigManager（监听 TemplateConfigChangeEvent）和 admin 模块的 ConfigChangeLog 写入监听器均可正常接收事件。**事件丢失补偿机制**：TemplateConfigManager、DefaultTriageRuleEngine 以及 LocalRuleEngine 中各规则类（AllergyCheckRule、ContraindicationCheckRule、DuplicateCheckRule）的 Caffeine 定时刷新（refreshAfterWrite）作为兜底——即使事件丢失，缓存最多在刷新间隔（默认 60 秒）后自动从数据库加载最新配置。DrugContraindicationMapping/DrugAllergyMapping/DrugCompositionDict 三个数据实体的运行时缓存与 TriageRule 及模板配置遵循相同的"事件驱动刷新 + 定时刷新"双重失效策略，确保覆盖一致性。

---

## 10. ai-api 层 DTO 扩展规格

ai-api 层 DTO 当前为空壳类（仅含默认构造器），需扩展完整字段定义以对齐需求文档 3.4.x 契约。扩展方式：保留现有类结构，在各 DTO 中补充字段，使其承载 AI 推理所需的完整输入/输出语义。以下列出各 DTO 需扩展的完整字段集。

**ai-api 层 DTO 扩展与业务模块开发的时序依赖关系**：ai-api 层 DTO 扩展为处方审核、辅助开方、病历生成、智能分诊四个业务模块开发的前置依赖——业务模块的 Converter、Service 实现依赖 ai-api 层 DTO 的完整字段定义。ai-api 模块与业务模块由同一团队在同一迭代中同步开发，不涉及跨团队接口冻结时间点。建议开发顺序：先完成 ai-api 层 DTO 扩展（含字段定义、注释、基础类型），再并行开发各业务模块的 Converter 映射和 Service 逻辑。

**跨阶段兼容风险**：本设计为 AiResult 新增了 failure(String errorCode, T partialData) 和 degraded(String fallbackReason, T partialData) 重载工厂方法（见 §2.3），这与 Phase 5 包 G OOD 中"AiResult.java 不变"的假设存在潜在冲突。若 Phase 5 团队保持 AiResult.java 不变，本设计的重载方法将在编译期冲突。建议将此扩展放在 ai-api 模块的独立演进基线中，与 Phase 5 团队沟通标记好兼容性承诺（如通过接口默认方法或新增静态工厂类隔离扩展）。

### 10.1 分诊 ai-api DTO

#### TriageRequest（ai-api 层）

扩展字段：chiefComplaint（string，必填）、additionalResponses（List\<AdditionalResponseItem\>，可选，每项含 question/answer/answeredAt）、patientId（string，可选）、sessionId（string，可选）、ruleVersion（string，可选）、ruleSetId（string，可选）。

#### TriageResponse（ai-api 层）

扩展字段：recommendedDepartments（List\<RecommendedDepartment\>，每项含 departmentId/departmentName/score）、recommendedDoctors（List\<RecommendedDoctor\>，每项含 doctorId/doctorName/departmentId/availableSlotCount/score）、reason（string，必填）、matchedRules（List\<MatchedRuleItem\>，每项含 ruleId/ruleName/score）、needFollowUp（bool，可选）、followUpQuestion（string，可选）、confidence（float，可选）、degraded（bool，可选）、sessionId（string，可选）、correctedChiefComplaint（string，可选——主诉修正隐式路径，AI 识别到患者修正主诉语义时填充）。

#### RecommendedDepartment（ai-api 层）

扩展字段：departmentId（string）、departmentName（string）、score（float）。

#### 新增 ai-api DTO（分诊相关）

- AdditionalResponseItem：question（string）/ answer（string）/ answeredAt（string）
- RecommendedDoctor：doctorId / doctorName / departmentId / availableSlotCount / score
- MatchedRuleItem：ruleId / ruleName / score

### 10.2 处方审核 ai-api DTO

#### PrescriptionCheckRequest（ai-api 层）

扩展字段：prescriptionItems（List\<PrescriptionCheckItem\>，每项含 drugId/drugName/dose/frequency/duration/route）、patientInfo（object，含 patientId/age/gender/allergyHistory/allergyDetails(List\<AllergyDetailItem\>)/comorbidities）、prescriptionId（string，必填）。

#### PrescriptionCheckResponse（ai-api 层）

扩展字段：riskLevel（enum：LOW/MEDIUM/HIGH）、interactions（List\<DrugInteractionItem\>，每项含 drugPair/severity/description）、alerts（List\<AlertItem\>，每项含 alertCode/alertMessage/severity）、suggestions（List\<SuggestionItem\>，每项含 suggestionCode/suggestionText）。

#### 新增 ai-api DTO（处方审核相关）

- PrescriptionCheckItem：drugId / drugName / dose / frequency / duration / route
- AllergyDetailItem：allergen / reactionType / severity / occurredAt
- DrugInteractionItem：drugPair / severity / description
- AlertItem：alertCode / alertMessage / severity
- SuggestionItem：suggestionCode / suggestionText

### 10.3 病历生成 ai-api DTO

#### MedicalRecordGenRequest（ai-api 层）

扩展字段：dialogueText（string，必填）、patientId（string，必填）、encounterId（string，可选）、stream（bool，可选，默认 false）、departmentId（string，可选，后端从 encounterId 关联获取后填入）。

#### MedicalRecordGenResponse（ai-api 层）

扩展字段：chiefComplaint（string）、symptomDescription（string）、presentIllness（string）、pastHistory（string）、physicalExam（string）、preliminaryDiagnosis（string）、treatmentPlan（string）、missingFields（List\<string\>）、partialContent（object，可选，超时降级时携带部分生成结果各字段）。

### 10.4 辅助开方 ai-api DTO

#### PrescriptionAssistRequest（ai-api 层）

扩展字段：diagnosis（string，必填）、examResults（List\<ExamResultItem\>，可选，每项含 itemName/value/unit/referenceRange/status）、patientInfo（object，含 patientId/age/gender/allergyHistory/allergyDetails/comorbidities）、existingPrescription（object，可选，含 drugs 数组）、encounterId（string，可选）。

#### PrescriptionAssistResponse（ai-api 层）

扩展字段：prescriptionDraft（object，含 drugs 数组每项含 drugId/drugName/dose/frequency/duration/route）、doseWarnings（List\<DoseWarningItem\>，每项含 drugId/warningType/message/severity）、allergyWarnings（List\<AllergyWarningItem\>，每项含 drugId/allergen/severity）、disclaimerRequired（bool，固定 true）。

#### 新增 ai-api DTO（辅助开方相关）

- ExamResultItem：itemName / value / unit / referenceRange / status
- DoseWarningItem：drugId / warningType / message / severity
- AllergyWarningItem：drugId / allergen / severity

---

## 修订说明（v8）

| 审查意见 | 修改措施 |
|---------|---------|
| 1.[严重] ai-api 层 DTO 与业务层 DTO 存在字段级契约缺口，缺少映射/转换机制 | 新增 §4.5 ai-api 层 DTO 与业务层 DTO 的映射机制——定义各模块 Converter 类职责和映射方向；新增 §10 ai-api 层 DTO 扩展规格——列出 6 个 ai-api DTO 需扩展的完整字段集（TriageRequest/Response、PrescriptionCheckRequest/Response、MedicalRecordGenRequest/Response、PrescriptionAssistRequest/Response）及新增子 DTO；ai-api TriageRequest 新增 additionalResponses 字段对齐多轮调用；ai-api RecommendedDepartment 扩展 departmentId/score 字段；§1.1 补充"ai-api 层 DTO 与业务层 DTO 契约完整性"设计目标 |
| 2.[严重] PrescriptionAssistRequest（ai-api 层）与业务层同名且职责不同 | §4.5 明确 ai-api 层与业务层 DTO 命名空间区分——com.aimedical.modules.ai.api.dto.prescription vs com.aimedical.modules.prescription.dto.audit/assist；Converter 方法签名以不同类型参数消除混肴；§7 新增"ai-api 层与业务层同名 DTO 区分"设计决策；§10 详细列出两层 DTO 字段差异 |
| 3.[严重] allergy_details 扩展容器未纳入设计 | 业务层 AuditRequest.patientInfo 和 PrescriptionAssistRequest.patientInfo 新增 allergyDetails 可选字段（List\<AllergyDetail\>）；新增 AllergyDetail DTO（对齐需求文档 3.1.6 结构）；AllergyCheckRule 补充严重程度分级逻辑——allergyDetails 存在时按 SEVERE→BLOCK / MODERATE+MILD→WARN 精确匹配，缺省回退一律 BLOCK；ai-api 层 DTO 同步新增 allergyDetails 字段；§7 新增"allergy_details 扩展容器"设计决策 |
| 4.[严重] RX_ASSIST_AI_NO_RECOMMENDATION 消费场景未描述 | §3.4 和 §4.4 /assist 主端点补充 AI 返回无可推荐药品场景——返回空 prescriptionDraft + 本地校验结果 + RX_ASSIST_AI_NO_RECOMMENDATION 标记，不作为 4xx 业务异常，与 check-dose FAILED 状态关联说明（positive empty vs negative failure）；§7 新增"/assist AI 返回无可推荐药品"设计决策 |
| 5.[严重] 多轮分诊 TriageServiceImpl 如何将 DialogueSession 上下文传递给 AiService.triage() 未定义 | §3.1 TriageService 补充 AI 调用上下文传递策略——全量拼接：每次调用 AiService.triage() 前，从 DialogueSession 取完整对话上下文，通过 TriageConverter.toAiTriageRequest() 组装为 ai-api TriageRequest（chiefComplaint 拼合 + additionalResponses 填充当轮新增）；ai-api TriageRequest 新增 additionalResponses 字段；§4.1 补充组装说明；§7 新增"多轮分诊上下文传递"设计决策 |
| 6.[严重] 推荐医生的推荐机制和数据来源未定义 | §1.3 和 §3.1 明确推荐医生列表生成机制——AI 返回推荐科室后，TriageServiceImpl 通过 DoctorFacade.findAvailableDoctorsByDepartment() 查询排班数据生成医生列表，availableSlotCount 实时查询；common-module-api 新增 DoctorFacade 接口定义；§2.1 和 §2.2 补充 DoctorFacade 位置和跨模块数据获取机制；§7 新增"推荐医生数据来源"设计决策 |
| 7.[一般] TriageRecord 写入时机和 finalDepartmentId 赋值时机不明确 | §3.1 TriageRecord 补充——分诊完成返回响应前同步写入，finalDepartmentId 首次写入时为空（nullable），患者挂号后通过事件/回调补充写入；§4.1 补充持久化说明；§7 新增"TriageRecord 写入时机"设计决策 |
| 8.[一般] 本地规则聚合逻辑缺少风险等级判定规则表，AllergyCheckRule 一律输出 BLOCK | LocalRuleResult.severity 明确类型为 AuditRiskLevel 枚举（值域 WARN/BLOCK）；AllergyCheckRule 补充严重程度分级——allergyDetails 存在时 SEVERE→BLOCK / MODERATE+MILD→WARN，缺省回退一律 BLOCK；DosageLimitRule 补充严重程度分级——超标 ≥ 2 倍→BLOCK / 超标 < 2 倍→WARN；§3.2 补充分级逻辑；§7 新增两条设计决策 |
| 9.[一般] WARN 级处方"强制提交并留痕"前后端交互链路不完整 | §4.2 WARN 分支补充完整前后端→后端交互链路——前端调用处方提交端点携带 forceSubmit=true + auditRecordId；§4.2 补充处方提交端点 POST /api/prescription/submit 简要契约定义（Phase 2/3）；§7 新增"WARN 级强制提交前后端交互"和"处方提交端点"设计决策 |
| 10.[一般] 病历生成非流式超时"部分保留"行为契约缺少 AiResult 降级模式定义 | AiSuggestionResult 新增 partialData 字段（可选，JSON TEXT，超时降级时携带部分结果）；§3.3 MedicalRecordService 补充非流式超时降级路径——AiResult.data 承载部分生成结果，MedicalRecordService 提取部分字段返回；§4.3 补充超时降级行为契约；§7 新增"AiResult 超时降级模式"和"AiSuggestionResult partialData"设计决策 |
| 11.[一般] WARN 级处方审核与强制提交的时序竞态未防护 | AuditRecord.originalPrescription 明确为 JSON TEXT 存储完整处方快照；§4.2 处方提交端点补充处方版本校验——提交时比较当前处方与 AuditRecord.originalPrescription 是否一致，不一致返回 RX_AUDIT_PRESCRIPTION_MODIFIED；§7 新增"WARN 强制提交时序竞态防护"设计决策；§5.1 错误码补充 RX_AUDIT_PRESCRIPTION_MODIFIED |
| 12.[一般] MedicalRecord 实体缺少 MedicalRecordField 级字段检索/更新机制定义 | §3.3 MedicalRecord 明确病历内容存储形式为单列 JSON TEXT（contentJson + JPA @Convert/Jackson）；补充 MedicalRecordRepository 查询方法列表（findByVisitId / findByPatientId / findById）；补充病历更新方法的增量更新语义；§7 新增"病历内容存储"设计决策 |
| 13.[一般] 配置变更审计日志跨模块事件传递的事务边界未定义 | §9.3 补充——使用 @TransactionalEventListener(phase=AFTER_COMMIT) 确保事务提交后发布事件；确认 Spring ApplicationEvent 在 application 聚合后可跨模块传播；补充事件丢失补偿机制——TemplateConfigManager/DefaultTriageRuleEngine 定时刷新覆盖；§7 更新"配置变更审计"设计决策 |
| 14.[一般] consultation 模块 AI 分诊首次调用缺少 session_id 必填语义对齐 | DialogueCreateRequest.sessionId 修订为必填——首轮请求由前端生成 UUID v4 传入，消除"首轮为空"特殊分支；§3.1 和 §4.1 同步更新；§7 新增"sessionId 必填/可选语义"设计决策，明确对齐需求文档 3.4.1 session_id 必填 |
| 15.[一般] DrugInteractionPair 和 DrugCompositionDict 实体缺少持久化层定义 | DrugCompositionDict 和 DrugAllergyMapping 从 rule/ 目录移至 entity/ 包下作为 JPA @Entity；补充核心字段（drugCode 主键、ingredients JSON TEXT 等）；补充 DrugCompositionDictRepository 和 DrugAllergyMappingRepository；§8.1 补充种子数据脚本路径；§2.1 目录结构同步更新 |
| 16.[一般] 分诊规则引擎的 TriageRule 匹配模型未定义 | 新增 TriageRule JPA @Entity（ruleId/ruleSetId/ruleVersion/conditions/resultDepartmentId/resultDepartmentName/score/enabled 等）；新增 TriageRuleRepository；TriageRuleEngine.match() 输入补充 ruleVersion+ruleSetId，输出 List\<RecommendedDepartment\>；§2.1 目录结构补充；§7 新增"TriageRule 匹配模型"设计决策 |
| 17.[一般] 处方提交端点不在设计范围内但闭环依赖 | §4.2 补充处方提交端点 POST /api/prescription/submit 简要契约定义（Phase 2/3 范围内）；含 forceSubmit + auditRecordId + 处方版本校验完整链路；§7 新增"处方提交端点"设计决策 |
| 18.[一般] 分诊场景对话会话存储 ConcurrentHashMap 跨实例部署问题 | §6.1 补充部署约束说明——Phase 2/3 假设单实例部署或 sticky session；三项内存存储水平扩展时需替换为分布式缓存；标注 Phase 5 迁移节点；§7 新增"内存存储部署约束"设计决策 |
| 19.[质询] AuditRecord 撤销审核时 isLatest 处理逻辑不完整 | §3.2 AuditRecord 补充撤销审核时 isLatest 处理——撤销后该 AuditRecord 的 isLatest 回退为 false，确保后续查询最新审核不返回已撤销记录；已撤销 AuditRecord 仍持久化保存；§7 新增"撤销审核 isLatest 处理"设计决策 |

## 修订说明（v9）

| 审查意见 | 修改措施 |
|---------|---------|
| 1.[严重] 合并症-药品禁忌检查遗漏——AllergyCheckRule 仅实现过敏史冲突检查，合并症-药品禁忌检查无对应规则 | 新增 ContraindicationCheckRule 独立规则类——遍历处方药品列表，对每个药品查询 DrugContraindicationMapping 获取禁忌症列表，与 patientInfo.comorbidities 做交集比对；命中时按禁忌级别分级（绝对禁忌→BLOCK，相对禁忌→WARN）；新增 DrugContraindicationMapping JPA @Entity（drugCode + contraindications JSON TEXT 含 diseaseName/level/description）；新增 DrugContraindicationMappingRepository；§3.2 实现范围表检查项 #2 拆分为 AllergyCheckRule + ContraindicationCheckRule；§2.1 目录结构补充；§8.1 补充种子脚本；§7 新增"检查项 #2 职责拆分"设计决策 |
| 2.[严重] AiResult 超时降级路径歧义——"新增 partialData 字段"与"使用现有 data 字段"二选一未决，且仅覆盖 degraded() 遗漏 failure() 路径 | 明确使用现有 AiResult.data 字段承载部分结果，删除"新增 partialData 字段"；覆盖两条路径：(a) 超时场景新增重载 AiResult.failure(String errorCode, T partialData)；(b) 降级场景新增重载 AiResult.degraded(String fallbackReason, T partialData)；§3.3 和 §7 同步更新 |
| 3.[严重] TriageRecord 缺推荐医生快照字段——需求文档 §5.1 定义"推荐医生"为核心字段 | TriageRecord 新增 recommendedDoctors 字段（JSON TEXT），存储 TriageResponse.doctors 列表快照（含 doctorId/doctorName/departmentId/availableSlotCount/score）；§1.3 和 §3.1 TriageRecord 同步更新；§7 新增"TriageRecord 推荐医生快照"设计决策 |
| 4.[一般] DoctorFacade 包名不匹配——放在 auth 包下与认证语义无关 | DoctorFacade 从 auth 包迁移至独立子包 commonmodule/doctor/DoctorFacade.java；§2.1 目录结构更新；§1.3 DoctorFacade 条目更新包路径；§3.1 补充"为何放在 doctor 子包"说明；§7 新增"DoctorFacade 包名"设计决策 |
| 5.[一般] matched_rules 子字段设计决策缺失——需求文档未定义子结构，设计侧主动定义但未论证 | §7 新增"matched_rules 子字段设计"设计决策条目——说明需求侧缺口、设计侧主动定义 ruleId/ruleName/score 三个子字段的理由、后续扩展兼容策略 |
| 6.[一般] AuditAlert.severity 字段类型和值域未定义——与 AuditRiskLevel 混淆 | 新增 AlertSeverity 枚举（INFO / WARNING / CRITICAL），与 AuditRiskLevel 是不同维度；§1.3 新增 AlertSeverity 条目；AuditAlert.severity 类型更新为 AlertSeverity；§7 新增"AuditAlert.severity 类型"设计决策 |
| 7.[一般] DosageAlert 无错误码字段——RX_ASSIST_DOSE_STANDARD_NOT_FOUND 无法传递 | DosageAlert 新增 errorCode 可选字段（String）；§1.3 DosageAlert 条目更新；§3.4 DosageThresholdService 描述更新；§4.4 check-dose 流程补充 DosageAlert.errorCode 传递路径；§7 新增"DosageAlert 错误码"设计决策 |
| 8.[一般] 处方版本校验"一致"的比较语义未定义 | §4.2 处方提交端点补充一致性语义定义——按业务字段做结构化比较（drugId + dose + frequency + duration + route 五字段组合比对），忽略 JSON 文本级格式差异；§7 新增"处方版本校验一致性语义"设计决策 |
| 9.[一般] allergyHistory/allergyDetails 数据来源语义不一致 | §3.2 AuditRequest 和 §3.4 PrescriptionAssistRequest 补充数据来源优先级说明——allergyHistory 后端拼接为 single source of truth；allergyDetails 后端优先从健康档案实体提取，前端传入值仅作为 fallback/离线场景覆盖；§7 新增"allergyHistory/allergyDetails 数据来源优先级"设计决策 |
| 10.[一般] 辅助开方过敏告警与处方审核过敏检查关系不明确 | §3.4 PrescriptionAssistService 补充关系说明——辅助开方 allergyWarnings 为即时提示性质，处方审核 AllergyCheckRule 为提交时正式审核判定，二者独立执行不互斥；allergyWarnings.severity=HIGH 不直接等价于 AuditRiskLevel=BLOCK；§7 新增"辅助开方过敏告警与处方审核过敏检查关系"设计决策 |
| 11.[一般] RegistrationEvent 跨模块事件契约未定义 | §1.3 新增跨模块事件一览表（RegistrationEvent：含 registrationId/patientId/departmentId/doctorId/eventTime）；§2.1 目录结构新增 event/RegistrationEvent.java 和 RegistrationEventListener.java；§2.2 新增"跨模块事件传递机制"章节；§3.1 TriageRecord 写入时机更新为 RegistrationEvent 补充写入；§7 新增"RegistrationEvent 事件契约"设计决策 |
| 12.[一般] §5.1 错误码表 AI/非 AI 分类命名规则不明确 | §5.1 新增错误码命名规则说明——AI 相关错误码含 `_AI_` 中段，非 AI 业务逻辑错误码不含 `_AI_` 中段，补充区分规则和适用范围；错误码表拆分为 AI/非AI 两行分类展示 |
| 13.[一般] 边界场景处理缺失——(1) TTL 清理竞态 (2) 规则快照失效 | (1) §3.1 DialogueSessionManager 新增"TTL 清理竞态处理"——ConcurrentHashMap.remove() 原子性保证，竞态路径被覆盖；§7 新增"TTL 清理竞态"设计决策。(2) §3.1 TriageRuleEngine 新增"规则快照失效处理"——快照版本查询无结果时降级使用当前最新版本规则集重新匹配，TriageResponse 标记 ruleVersionMismatch=true；§7 新增"规则快照失效处理"设计决策 |
| 14.[一般] §5.1 错误码表遗漏需求文档明确定义的 AI 能力错误码 | §5.1 错误码表补齐：RX_ASSIST_AI_NO_RECOMMENDATION（已存在）、RX_AUDIT_AI_INPUT_INVALID（已存在）、MR_GEN_AI_INPUT_INVALID（已存在）、MR_GEN_AI_OUTPUT_INCOMPLETE（已存在）；全部 AI 错误码已在 v8/v9 表中列出 |
| 15.[轻微] TriageResponse.degraded=true 时前端行为未说明 | §3.1 TriageService 新增"降级时前端行为指引"——degraded=true 时前端仍渲染推荐科室列表 + 降级提示文案 + "手动选择科室"入口；TriageResponse 新增 ruleVersionMismatch 可选标记；§4.1 降级路径补充前端行为说明；§7 新增"降级时前端行为"设计决策 |
| 16.[轻微] "底座直接落地"与 Phase 5 迁移兼容性未显式论证 | §1.1 设计目标"底座直接落地"补充论证——业务模块仅依赖 ai-api 的 AiService 接口（编译期依赖），Phase 5 迁移时仅需替换 ai-impl 内的 AiService 实现类，业务模块代码无须修改；§7 新增"底座落地与 Phase 5 迁移兼容性"设计决策 |

## 修订说明（v12）

| 审查意见 | 修改措施 |
|---------|---------|
| P1.[严重] check-dose 端点请求参数缺少 prescriptionId，CRITICAL 写入 PrescriptionDraftContext 行为契约与请求参数不匹配 | §4.4 check-dose 请求参数增加 `prescriptionId`（必填，String）；§1.3 新增 DosageCheckRequest DTO 条目（含 prescriptionId 字段）；§2.1 目录注释同步更新 |
| P2.[严重] AdditionalResponse 业务层 DTO 缺少字段定义，additionalResponses 引用未展开 | §1.3 新增 AdditionalResponse DTO 条目（含 question/answer/answeredAt 字段），对齐需求文档 3.4.1 additional_responses 数组每项结构 |
| P3.[一般] TriageRule 实体 conditions 字段 JSON 结构未定义，规则引擎核心输入格式缺失 | §3.1 TriageRule conditions 字段补充 JSON schema 定义：`{"keywords": ["胸痛", "胸闷"], "logic": "AND"}`，明确 keywords 为数组/ logic 为 AND/OR |
| P4.[一般] 全量拼接策略在多轮长对话下 token 超限风险未评估 | §3.1 TriageService 新增"全量拼接 Token 超限风险评估"段落，定义上下文截断策略（保留首轮+最近 N 轮，阈值默认 3000 字符可配置 `triage.max-context-chars`）；§7 新增设计决策 |
| P5.[一般] 错误码 RX_ASSIST_AI_SUGGESTION_NOT_FOUND 命名含 _AI_ 中段，违反自身命名规则 | 重命名为 `RX_ASSIST_SUGGESTION_NOT_FOUND`（去除 `_AI_` 中段），同步更新 §5.1 错误码表、§3.4 AiSuggestionResult 查询分支、§4.4 异步建议查询端点 |
| P6.[一般] 全量降级路径中前端无法区分"AI 完全不可用"与"AI 明确返回空" | RecordGenerateResponse 增加 `degraded` 布尔字段（true=AI不可用/降级，false=AI明确返回空）；更新 §1.3、§3.3 和 §4.3 的行为描述；§7 新增"病历降级前端区分"设计决策 |
| P7.[轻微] check-dose 端点 taskId 生成策略和生命周期未定义 | §4.4 check-dose 流程补充 taskId 生成（UUID v4）、AiSuggestionResult 预创建（PENDING）、生命周期（TTL 30 分钟 + ScheduledExecutorService 定期清理）和状态更新路径 |
| P8.[轻微] PrescriptionDraftContext 缺少写入前的实例化保证 | §3.4 PrescriptionDraftContext 补充实例化保证说明——作为 Spring @Component 单例 Bean，ConcurrentHashMap 在构造时初始化（声明 `new ConcurrentHashMap<>()`），不存在空指针风险 |
| P9.[轻微] prescriptionId 与 prescriptionOrderId 语义关系未定义 | §3.2 AuditRecord 增加 prescriptionId 字段，明确 prescriptionId 为系统层 UUID（贯穿全流程）与 prescriptionOrderId 为提交后业务单据号的 1:1 关系；§3.4 PrescriptionDraftContext 补充键语义说明；§7 新增设计决策 |
| P10.[轻微] TriageService AI 调用失败 aiFailCount 缺少跨 TTL/重启的持久化说明 | §3.1 TriageService 新增"aiFailCount 持久化约束"段落，明确 aiFailCount 不持久化、不跨会话/重启保持的设计意图；§7 新增"aiFailCount 持久化策略"设计决策 |

## 修订说明（v13）

| 审查意见 | 修改措施 |
|---------|---------|
| 1.[严重]「与前一版一致」5 处文档参照缺陷——§8.1、§8.2、§8.3、§8.4、§9.4 各一处"与前一版一致"导致设计不可独立使用 | §8.1 展开定义"同优先级多条记录检测策略"——按 updateTime 最新选择 + 日志告警；§8.2 展开定义 DosageStandard.drugCode 与药品基础信息实体的逻辑外键关联关系及跨模块查询方式；§8.3 展开 DosageUnitGroup 单位映射表（含枚举值/包含单位/基准单位/换算系数四列）及跨组单位比较的 RX_ASSIST_UNIT_MISMATCH 错误码产出规则；§8.4 展开 DosageStandard 实体年龄/体重分级字段完整定义（ageRangeStart/ageRangeEnd/weightRangeStart/weightRangeEnd/dailyMax/singleMax/unit）及五级匹配优先级伪代码；§9.4 展开 ConfigChangeLog 实体完整字段表（id/entityType/entityId/changeType/oldValue/newValue/operatorId/operatorName/changeTime/changeReason） |
| 2.[严重] AiService 接口方法缺少正式定义——全文分散提及 4 个方法但无集中签名 | 新增 §2.3 AiService 接口定义——集中给出 triage()、prescriptionCheck()、generateMedicalRecord()、prescriptionAssist() 四个方法的参数类型、返回类型 CompletableFuture\<AiResult\<T\>\> 及各自的泛型 T 绑定（TriageResponse/PrescriptionCheckResponse/MedicalRecordGenResponse/PrescriptionAssistResponse）、超时配置、降级语义；补充 AiResult\<T\> 泛型要点说明 |
| 3.[一般] DosageUnitGroup 缺少组内单位清单和换算系数表 | §8.3 补充单位映射表（MASS_GROUP/VOLUME_GROUP/IU_GROUP 三组，含基准单位 mg/ml/IU 及组内换算系数）；补充跨组单位比较时统一输出 RX_ASSIST_UNIT_MISMATCH 错误码的规则 |
| 4.[一般] CRITICAL 剂量告警与 BLOCK 审核阻断的隔离边界缺少执行路径说明——§4.2 第 3 步和第 4 步未明确是否短路 | §4.2 处方提交流程第 3 条修订为"阻断合并判定（路径 A——先完整收集后阻断）"——同时检查 BLOCK 和 CRITICAL，合并产生单一 BlockResponse；任一命中即拒绝提交 |
| 5.[一般] SpecialPopulationDosageRule 等本地规则类未纳入 §1.3 核心抽象一览表 | §1.3 包D-AI1 核心抽象表新增 DuplicateCheckRule、DosageLimitRule、SpecialPopulationDosageRule 三个条目（class 形态，含职责描述）；LocalRuleEngine 条目同步更新为"封装 6 条独立规则"并展开列出 |
| 6.[一般] §10 ai-api 层实现前提说明缺失——未说明 DTO 扩展完成状态与业务模块开发时序依赖 | §10 开篇补充时序依赖说明——ai-api DTO 扩展为四个业务模块开发的前置依赖；ai-api 模块与业务模块由同一团队同步开发，不涉及跨团队接口冻结；建议开发顺序为先 DTO 扩展再 Converter/Service 并行 |

## 修订说明（v15）

| 审查意见 | 修改措施 |
|---------|---------|
| 1.[严重] sessionId 生成责任归属矛盾——§1.3 L53 声明 DialogueSessionManager"统一生成"，§3.1 L345 同时出现"统一生成"和"前端生成"两种表述 | §1.3 DialogueSessionManager 条目改为"接受前端传入的 UUID v4 并验证格式，首次使用时创建会话"；§3.1 DialogueSessionManager 职责描述改为"接受前端传入的 UUID v4，验证格式有效性后通过 findOrCreate() 完成创建或恢复，不自行生成 sessionId" |
| 2.[严重] Phase 5 迁移"代码无须修改"断言与设计决策矛盾——§1.1 无条件断言与 §7 有条件的"需要更新"直接矛盾 | §1.1 设计目标修订为有条件表述——"业务模块的核心 Service 和 Controller 代码在 AiService 接口签名不变的情况下无须修改；Converter 层需随 ai-api DTO 字段变更同步更新" |
| 3.[严重] BLOCK 阻断处方 prescriptionOrderId 可为空，但 isLatest 清理逻辑基于 prescriptionOrderId 分组，空值时无法找到上一条记录 | §3.2 AuditRecord 补充"按业务主键分组"模式——优先按 prescriptionOrderId 分组；prescriptionOrderId 为空时按 prescriptionId 分组执行相同 isLatest 清理逻辑 |
| 4.[严重] MedicalRecordController 声称支持流式输出与 §3.3/§4.3 "Phase 2/3 仅非流式"矛盾 | §1.3 MedicalRecordController 条目改为"支持非流式输出（Phase 2/3），流式输出预留到 Phase 4 以实现" |
| 5.[一般] AiSuggestionResult 并发安全描述混淆对象与容器——称"内部字段更新通过 ConcurrentHashMap.compute() 原子操作保护"但 AiSuggestionResult 本身非 ConcurrentHashMap | §1.3 和 §3.4 AiSuggestionResult 的并发安全描述改为"AiSuggestionResult 实例由 ConcurrentHashMap 存储，通过 compute() 原子替换整个实例保证并发安全" |
| 6.[一般] RX_AUDIT_BLOCK 错误码已定义但无消费路径——BLOCK 阻断通过 HTTP 422 + BlockResponse 返回，BlockResponse 无 errorCode 字段 | §5.1 错误码表中删除 RX_AUDIT_BLOCK |
| 7.[一般] DrugInteractionPairChangeEvent 在 Phase 2/3 范围内无实际意义——DrugInteractionRule 不在 Phase 2/3 本地规则范围内 | §9.3 规则管理覆盖实体列表中标注 DrugInteractionPair 相关管理接口为 Phase 4 范围；变更事件列表中删除 DrugInteractionPairChangeEvent，标注为 Phase 4 |
| 8.[一般] AllergyWarningSeverity 枚举值排序（CRITICAL/WARNING/INFO）与其他同类枚举（INFO/WARNING/CRITICAL）不一致，违反自身声明 | §1.3、§3.4 AllergyWarningSeverity 枚举及 AllergyWarningItem 中 severity 字段排序统一调整为 INFO / WARNING / CRITICAL |
| 9.[一般] forceSubmit=false 时若处方已有 WARN 结果，重新审核可能导致循环重审 | §4.2 处方提交端点补充——forceSubmit=false 且已有 WARN 最新审核结果时，直接返回当前结果并提示医生选择强制提交或修改，避免无意义重复 AI 审核 |
| 10.[轻微] DialogueCreateRequest 在 AdditionalResponse 之前引用，影响 §1.3 快速查阅效率 | §1.3 将 AdditionalResponse 条目提前至 DialogueCreateRequest 之前定义 |

## 修订说明（v16）

| 审查意见 | 修改措施 |
|---------|---------|
| 1.[严重] DosageStandard 五级匹配策略存在未覆盖路径——当一条记录同时设置 ageRange 和 weightRange 且患者条件不满足精确匹配要求时，该记录被跳过至 Level 5 报"标准不存在" | §8.4 将五级匹配优先级扩展为六级，新增 Level 2"同时范围匹配"——当 ageRange 与 weightRange 均非 null 但未达到精确匹配时，同时检查年龄范围和体重范围，两项均命中则使用该记录 |
| 2.[严重] DoctorFacade 跨模块同步调用缺失降级保护——超时阈值、失败处理、替代策略均未定义 | §3.1 DoctorFacade 补充跨模块调用降级保护——独立超时配置（默认 2s），调用超时/异常时捕获并记录 WARN 日志，TriageResponse.doctors 置为空列表不阻断分诊主流程；§4.1 推荐医生列表生成流程同步补充降级保护说明 |
| 3.[一般] MissingFieldDetector 的 null/空字符串判定语义未定义——差集比对基于 keySet 存在性还是值非空非 null 未定义 | §3.3 MedicalRecordService 补充 MissingFieldDetector 缺失判定策略——采用"基于字段值的非空非 null 存在性"：字段不存在、null、空字符串均视为缺失；§4.3 病历生成流程同步补充判定策略说明 |
| 4.[一般] 多轮分诊中 DialogueSession 不支持主诉修正——首轮固定记录 chiefComplaint，患者修正主诉后无法更新 | §3.1 DialogueSession 增加 correctedChiefComplaint 可选字段——全量拼接时若存在则替换原始 chiefComplaint 作为推理上下文起点；§3.1 AI 调用上下文传递策略同步补充 correctedChiefComplaint 替换逻辑 |
| 5.[一般] forceSubmit=false 路径缺少对处方已修改的感知——返回旧 WARN 结果但处方可能已被修改 | §4.2 处方提交端点 forceSubmit=false 路径增加轻量级内容变更检测——drugId+dose+frequency+duration+route 五字段结构化哈希比对，发现不一致时返回 RX_AUDIT_PRESCRIPTION_MODIFIED 错误码提示重新审核 |
| 6.[一般] CRITICAL 告警前端同步更新的竞态窗口——PrescriptionDraftContext 全量覆盖语义下前后端状态不一致 | §4.4 check-dose 响应新增 contextCriticalCount 字段（Integer，当前 prescriptionId 下 CRITICAL 告警数量）；§3.4 PrescriptionDraftContext 补充 getContextCriticalCount() 方法和前端同步协商机制说明 |

## 修订说明（v17）

| 审查意见 | 修改措施 |
|---------|---------|
| 1.[严重] correctedChiefComplaint 传递路径未闭环——DialogueCreateRequest 中未定义此字段，前端无法传递主诉修正信息；TriageServiceImpl 从 AI 回复中检测修正主诉的判定规则未定义 | 路径A：§1.3 DialogueCreateRequest 条目和 §3.1 DialogueCreateRequest 详细描述中增加 correctedChiefComplaint（可选，String）字段；路径B：§3.1 主诉修正识别策略明确定义两条触发路径——显式路径（DialogueCreateRequest.correctedChiefComplaint）优先于隐式路径（ai-api TriageResponse.correctedChiefComplaint）；AI 隐式识别规则：AI 在推理输出中识别到"患者主诉有误"、"实为"等语义标记时填充 ai-api TriageResponse.correctedChiefComplaint；§4.1 AI 调用上下文传递策略后新增主诉修正识别策略段落；§4.5 TriageConverter 中 ai-api TriageResponse 增加 correctedChiefComplaint 字段；§10.1 ai-api TriageResponse 同步补充 correctedChiefComplaint |
| 2.[严重] TriageRecord.finalDepartmentId 补充写入未覆盖手动选科场景——仅定义了 RegistrationEvent 一种写入路径，但降级时前端"手动选择科室"入口产生的 finalDepartmentId 如何写入 TriageRecord 完全未定义 | §1.3 TriageController 条目补充手动选科端点描述；§1.3 TriageService 条目补充 selectDepartment 方法；§2.1 TriageController 注释补充 POST /api/triage/select-department；§2.2 新增手动选科写入路径段落——POST /api/triage/select-department 含 sessionId + departmentId + departmentName，TriageService.selectDepartment() 按 sessionId 查找 TriageRecord 并补充写入 finalDepartmentId/finalDepartmentName；§2.2 RegistrationEvent 段落补充手动选科路径覆盖语义说明；§3.1 TriageService 补充 selectDepartment 方法定义；§4.1 新增手动选科端点行为契约 |
| 3.[严重] 提交端点阻断判定时序未定义——三条行为（forceSubmit=false 常规审核、forceSubmit=true 强制提交校验、阻断合并判定）的执行顺序未明确 | §4.2 处方提交端点重新定义三步顺序：步① CRITICAL 阻断检查（最高优先级，有则立即 422 不执行后续）→ 步② 审核结果阻断检查（步①通过后检查 BLOCK）→ 步③ forceSubmit 判定（仅在步①②均通过且为 WARN 时生效）；阻断合并语义说明仅限超时/竞态场景 |
| 4.[严重] AiResult.failure()/degraded() 重载方法缺乏实现归属——AiResult 类不在 §2.1 目录结构的任何模块中出现，未在任何章节中标注其包路径 | §2.1 目录结构新增 ai/ai-api 模块子树——标注 AiResult.java（com.aimedical.modules.ai.api.AiResult）及 ai-api DTO 子包结构；§2.3 首段补充 AiResult 类包路径（com.aimedical.modules.ai.api.AiResult）和六字段说明（success/data/errorCode/degraded/fallbackReason/partialData）及重载方法 |
| 5.[一般] PrescriptionDraftContext 与 AiSuggestionResult TTL 不一致导致状态残差——二者 TTL 分别为 60 分钟和 30 分钟，30 分钟后 AiSuggestionResult 被清理但 PrescriptionDraftContext 中 CRITICAL 标记仍存在 | 将 AiSuggestionResult TTL 调整为 60 分钟，与 PrescriptionDraftContext TTL 一致——§3.4 AiSuggestionResult 条目的 TTL 值更新为 60 分钟；§4.4 check-dose 异步建议流程中 AiSuggestionResult TTL 同步更新为 60 分钟；消除状态残差风险 |
| 6.[一般] contextCriticalCount 前端消费行为未定义——check-dose 响应新增 contextCriticalCount 字段但未定义前端消费规则 | §3.4 PrescriptionDraftContext 补充 contextCriticalCount 前端消费规则——N→0 恢复提交按钮并清除阻断提示，0→N 禁用提交按钮并展示阻断原因，0→0 不做变更，N→M 同步更新告警列表；覆盖四种状态变迁场景 |

## 修订说明（v18）

| 审查意见 | 修改措施 |
|---------|---------|
| §2.3 AiResult 字段描述与 §7 设计决策自相矛盾——声称含 partialData 六字段，但 §7 明确不新增 partialData 字段，使用 data 字段承载部分结果 | 删除 §2.3 中 partialData 字段引用，改为"含 success/data/errorCode/degraded/fallbackReason 五字段"，并在重载方法说明处补充"partialData 通过重载工厂方法入参传入，数据写入 data 字段" |
| chiefComplaint 与 additionalResponses 互斥违规的处理未定义——TriageServiceImpl 校验字段组合规则但未定义违规时返回何种错误码 | 在 §3.1 DialogueCreateRequest 描述中补充违规时返回 HTTP 400 + TRIAGE_FIELD_COMBINATION_INVALID；在 §4.1 API 契约中增加错误响应说明行；在 §5.1 错误码表中分诊（非AI）行补充 TRIAGE_FIELD_COMBINATION_INVALID |
| PrescriptionAssistRequest 未列入 §1.3 包E 核心抽象一览 | 在 §1.3 包E 核心抽象表中新增 PrescriptionAssistRequest 条目（class DTO，含 diagnosis/examResults/patientInfo/existingPrescription/encounterId） |
| triage.max-context-chars 配置项未列入 §5.5 配置表 | 在 §5.5 AI 超时配置表中追加一行：分诊上下文截断阈值 | triage.max-context-chars | 3000 | 对话上下文累计字符数阈值，超出后截断中间轮次 |

## 修订说明（v19）

| 审查意见 | 修改措施 |
|---------|---------|
| §2.3 "AiResult\<T\> 泛型要点"段落将 partialData 列为 AiResult 统一包含的 6 项内容之一，但 §2.3 首段和 §7 设计决策已明确 AiResult 仅含 5 字段 | 将该段落中 partialData 字段从统一包含列表中移除，改为说明性表述——"超时/降级场景下 partialData 通过重载工厂方法 failure()/degraded() 入参传入并写入 data 字段" |

## 修订说明（v20）

| 审查意见 | 修改措施 |
|---------|---------|
| P1.[严重] RegistrationEvent 缺少 sessionId 字段，事件驱动的 finalDepartmentId 写入路径无法闭合 | §1.3 RegistrationEvent 核心抽象表新增 sessionId（可选，String）字段及获取来源说明；§2.2 RegistrationEvent 描述补充 sessionId 定义（作为事件消费端定位 TriageRecord 的精确关联键）|
| P2.[严重] AllergyWarningSeverity 枚举值 INFO/WARNING/CRITICAL 与需求文档 3.4.10 "HIGH 级别告警"语义不匹配 | §1.3 AllergyWarningItem 条目 severity 改为 INFO/WARNING/HIGH；§3.4 AllergyWarningSeverity 枚举改为 INFO/WARNING/HIGH，更新职责描述中对齐需求文档 3.4.10 术语；§3.4 AllergyWarningItem severity 同步更新 |
| P3.[重要] §3.4 DosageThresholdService 匹配优先级标注"四级均未命中"与 §8.4 的 6 级描述不一致 | §3.4 DosageThresholdService 匹配优先级改为"参见 §8.4 六级定义"并删除内联四级别表；§4.4 check-dose 流程"四级均未命中"改为"六级均未命中" |
| P4.[重要] PrescriptionAssistService 中过敏冲突检查的实现归属未定义 | §3.4 PrescriptionAssistService 补充过敏冲突检查实现路径说明——PrescriptionAssistServiceImpl 直接复用同模块 AllergyCheckRule 规则类；§4.4 本地即时校验注释同步更新为复用说明 |
| P5.[重要] encounterId → visitId 转换未定义具体实现路径 | §1.3 RecordGenerateRequest 条目改为通过 VisitFacade.findVisitIdByEncounterId() 转换；§3.3 RecordGenerateRequest 详细描述补充 VisitFacade 接口定义位置及跨模块依赖关系说明 |
| P6.[重要] DialogueSession 状态更新与 TriageRecord 持久化缺少事务一致性保障 | §3.1 TriageRecord 写入时机后新增"DialogueSession 与 TriageRecord 的事务一致性策略"段落，定义"先写数据库再更新内存"策略及可接受数据丢失窗口说明 |
| P7.[一般] §1.1 Phase 5 迁移"Service 代码无须修改"断言与 §6.1 内存存储迁移需求矛盾 | §1.1 设计目标补充"内存存储依赖部分需引入 Store 抽象层以隔离存储实现"限定条件；§7 设计决策同步更新为提出 Store 抽象层方案 |
| P8.[一般] §1.3 LocalRuleEngine 规则计数"6 条"与 §3.2 的 5 条不一致 | §1.3 LocalRuleEngine 改为"5 条运行时独立规则 + 1 条预留骨架（DrugInteractionRule，Phase 2/3 不启用）" |
| P9.[一般] §4.2 步①与步②的 CRITICAL/BLOCK 阻断竞态仅描述响应策略，未提供实质防护手段 | §4.2 阻断竞态段落补充"阻断竞态防护手段"——步③前增加二次 CRITICAL 验证（重新查询 PrescriptionDraftContext 并与步①快照比对），覆盖 forceSubmit=true 和 forceSubmit=false 两路径 |
| P10.[轻微] §1.3 PrescriptionAssistResponse 缺少 errorCode 字段定义 | §1.3 PrescriptionAssistResponse 条目补充"顶层错误码（errorCode，可选，String——AI 返回无可推荐药品时填充 RX_ASSIST_AI_NO_RECOMMENDATION）" |

## 修订说明（v21）

| 审查意见 | 修改措施 |
|---------|---------|
| 1.[严重] TriageRecord、AuditRecord、DeadLetterEvent 实体未定义 JPA @Id 主键字段 | §3.1 TriageRecord 补充 recordId（Long，@Id @GeneratedValue）；§3.2 AuditRecord 补充 auditId（Long，@Id @GeneratedValue）；§2.2 DeadLetterEvent 补充 id（Long，@Id @GeneratedValue）；§2.1 目录结构补充 DeadLetterEvent.java 条目；§1.3 AuditRecord 条目补充 auditId 主键说明 |
| 2.[严重] 手动选科与 RegistrationEvent 自动写入的覆盖优先级缺少强制执行机制 | §2.2 和 §3.1 补充覆盖优先级强制执行机制——RegistrationEventListener 在调用 selectDepartment 前检查 finalDepartmentId 是否为空，仅为空时写入；手动选科端点始终覆盖写入 |
| 3.[严重] 全量拼接上下文截断缺少 AI 感知截断标记 | §3.1 "全量拼接 Token 超限风险评估"补充 AI 感知截断标记——截断后在首轮主诉和最近 N 轮 QA 之间插入 `[NOTE: 部分对话内容因长度限制已省略]` |
| 4.[严重] DrugFacade 接口有引用无定义 | §1.3 核心抽象表新增 DrugFacade 条目（interface，定义在 common-module-api/drug/）；§2.1 目录结构新增 drug/DrugFacade.java；§2.3 跨模块数据获取机制补充 DrugFacade 说明（与 DoctorFacade 一致的模式） |
| 5.[严重] Registration 模块和就诊模块作为外部依赖未在设计范围中说明 | §1.1 后新增"1.1a 外部依赖与前提条件"章节，显式列出 registration 模块（RegistrationEvent 发布 + sessionId 填充）和 visit 模块（VisitFacade 实现）的依赖内容和时间线要求 |
| 6.[一般] DeadLetterEvent 实体缺少精确的 JPA 字段定义 | §2.2 dead_letter_event 表模块归属段落扩展 DeadLetterEvent 字段表（id/eventPayload/failReason/failTime/state/retryCount/maxRetryCount 完整定义，参照 §9.4 ConfigChangeLog 格式） |
| 7.[一般] AiResult 新增工厂方法设计与 Phase 5 的"AiResult 不变"假设存在潜在冲突 | §10 时序依赖说明中增加跨阶段兼容风险标注——标注本设计新增 AiResult 重载工厂方法与 Phase 5 包 G 的"AiResult.java 不变"假设的冲突风险，建议放在 ai-api 模块独立演进基线中，与 Phase 5 团队沟通标记兼容性承诺 |

## 修订说明（v22）

| 审查意见 | 修改措施 |
|---------|---------|
| 1.[严重] "规避 Phase 5 迁移成本"目标在内存存储维度未闭合——三项内存存储使用 ConcurrentHashMap 实现无隔离层 | §1.1 "建议引入 Store 抽象层"升级为"必须增加 Store 抽象层（SessionStore/SuggestionStore/DraftContextStore 接口），Phase 2/3 ConcurrentHashMapStore 实现，Phase 5 RedisStore 实现"；§6.1 补充"三项存储必须通过 Store 接口间接访问，Store 抽象层为设计强制项"；§7 "底座落地与 Phase 5 迁移兼容性"决策条目升级为"Store 抽象层为设计强制项" |
| 2.[严重] check-dose 端点高频调用下异步 AI 去重/节流策略完全缺失 | §3.4 新增"异步 AI 调用去重策略"——prescriptionId 级去重规则（PENDING/COMPLETED 未消费复用，FAILED/已消费创建新 task），consumed 标记，前端 300ms 防抖配合；§4.4 check-dose 流程增加"异步 AI 去重检查"步骤；§6.3 扩展为"异步 AI 建议与去重"补充 prescriptionId 级去重和前端节流配合 |
| 3.[一般] "规则可配置"依赖 admin 模块 OOD，无时间线协调 | §1.1a 外部依赖表新增 admin 模块行（规则管理接口依赖，与 prescription 模块同步开发，最迟在处方审核上线前完成）并标注风险说明；时间线约束从"两个"更新为"三个"外部依赖模块 |
| 4.[一般] AiResult.success=true 且 data=null 的边界处理未定义 | §2.3 AiResult 泛型要点中补充契约约束——success=true 时 data 必须非 null，data=null 仅允许在 success=false 场景出现；各 Service 层依赖此契约无需额外判空 |
| 5.[一般] "撤销审核"的操作触发端点未定义 | §3.2 撤销审核 isLatest 处理中补充端点定义——POST /api/prescription/audit/{auditId}/revoke（参数 auditId 路径参数，响应 200/404/409）及时序逻辑；§4.2 处方审核场景补充端点说明 |
| 6.[一般] prescriptionId 在 check-dose 请求链路中的传递路径未闭环 | §1.3 DosageCheckRequest 条目补充 prescriptionId 分配时机——前端预创建 UUID v4 为主路径，后端按需生成（DosageCheckResponse.prescriptionId 回写）为 fallback；§3.4 PrescriptionDraftContext 新增"prescriptionId 分配时机"段落定义双路径 |
| 7.[一般] 缺少 AI Mock 实现的行为契约 | §2.3 AiService 接口定义后新增"MockAiService 实现契约"段落——定义三种策略模式（STATIC/AI_UNAVAILABLE/TIMEOUT）、激活方式（@Profile("mock")）、动态切换端点 |
| 8.[轻微] §4.5 命名空间区分说明处有多余字符"：：" | §4.5 删除多余冒号，改为"命名空间区分：" |
| 9.[轻微] DosageStandard 单位字段 unit 缺少枚举约束 | §8.4 unit 字段描述更新为"值域限定为 DosageUnitGroup 枚举中各分组的单位值；DosageThresholdService 按 DosageUnitGroup 分组做跨组换算检查" |

## 修订说明（v23）

| 审查意见 | 修改措施 |
|---------|---------|
| 1.[严重] RegistrationEvent sessionId 跨模块传播路径在架构层面未闭合——consultation 与 registration 模块不允许互相依赖，产出未定义 sessionId 从 DialogueSession 传递到 registration 模块的具体路径 | §1.1a 外部依赖表 registration 模块行——"sessionId 填充逻辑"改为"接收前端传递的 sessionId"；§1.3 RegistrationEvent 条目——sessionId 来源改为"前端保留并传入挂号流程，registration 模块接收后填充"；§2.2 RegistrationEvent 描述——更新 sessionId 来源为前端传入，新增"sessionId 跨模块传播机制"段落明确定义前端侧传递路径；§2.2 时间线约束同步更新降级描述；§7 新增"RegistrationEvent sessionId 传播机制"设计决策 |
| 2.[一般] VisitFacade 调用失败无降级策略——缺少超时阈值和失败处理行为，未与 DoctorFacade 对称 | §1.1a 外部依赖表 visit 模块行——补充超时阈值（默认 2s）和降级策略（encounterId fallback / MR_GEN_VISIT_NOT_FOUND + 部分内容）；§3.3 RecordGenerateRequest——补充"VisitFacade 调用降级策略"段落，定义超时配置、双路径降级和日志记录；§4.3——在正常流程和 §5.5 超时配置表中补充 VisitFacade 超时配置行，正常路径增加 VisitFacade 调用步骤、补充降级分支；§5.1 错误码表——病历（非AI）行新增 MR_GEN_VISIT_NOT_FOUND；§7 新增"VisitFacade 降级保护"设计决策 |
| 3.[一般] FieldMissingHint 字段生成规则未定义——promptMessage 和 suggestedAction 缺少内容来源策略 | §1.3 DepartmentTemplateConfig 条目——补充每字段可选 promptMessage/suggestedAction 提示模板字段；§1.3 MissingFieldDetector 条目——补充读取模板组装 FieldMissingHint 的职责；§1.3 FieldMissingHint 条目——补充 promptMessage/suggestedAction 来源于模板配置；§2.1 目录结构——DeptTemplateConfig.java 注释补充提示模板说明；§3.3 MissingFieldDetector——新增"FieldMissingHint 生成策略"段落，定义模板读取、默认文案兜底和管理员配置来源；§7 新增"FieldMissingHint 内容来源"设计决策 |
| 4.[一般] 部分错误码遗漏于 §5.1 错误码表——RX_ASSIST_UNIT_MISMATCH、TRIAGE_SESSION_NOT_FOUND、MR_GEN_VISIT_NOT_FOUND 未进表 | §5.1——分诊（非AI）行新增 TRIAGE_SESSION_NOT_FOUND；病历（非AI）行新增 MR_GEN_VISIT_NOT_FOUND、MR_GEN_CONCURRENT_MODIFICATION；开方辅助（非AI）行新增 RX_ASSIST_UNIT_MISMATCH |
| 5.[一般] MedicalRecord.contentJson 并发写更新丢失未处理——"单列 JSON TEXT + 读取→合并→写回"模式存在后提交覆盖前变更的风险 | §3.3 MedicalRecord 实体——增加 `@Version` 乐观锁字段说明；MedicalRecordRepository——补充更新操作的乐观锁校验机制，写冲突时返回 MR_GEN_CONCURRENT_MODIFICATION 错误码；§5.1 错误码表——病历（非AI）行新增 MR_GEN_CONCURRENT_MODIFICATION；§7 新增"MedicalRecord.contentJson 并发写保护"设计决策 |

## 修订说明（v24）

| 审查意见 | 修改措施 |
|---------|---------|
| 1.[严重] §3.1 DialogueSessionManager 详细设计与 §1.1/§6.1 Store 抽象层强制约束矛盾——内部维护 ConcurrentHashMap 且含否定 interface 抽象价值的论证 | §1.3 DialogueSessionManager 条目补充 SessionStore 依赖说明；§3.1 DialogueSessionManager 将"内部维护 ConcurrentHashMap"改为"内部依赖 SessionStore 接口"，删除否定 interface 抽象的论证，改为"存储策略已通过 SessionStore 接口隔离，管理器本身无抽象化必要"；§6.1 补充 Store 接口包路径（common-module-api/store/）和具体实现类名；§2.1 common-module-api 目录新增 store/ 子目录（含 SessionStore/SuggestionStore/DraftContextStore 接口及 ConcurrentHashMapStore 实现）；Store 接口包路径同步补充至 AiSuggestionResult（SuggestionStore）和 PrescriptionDraftContext（DraftContextStore）的 §1.3/§3.4 描述中；DedupTaskScheduler 依赖 SuggestionStore 的描述同步更新 |
| 2.[严重] 异步 AI 建议流程缺少 AiResult → AiSuggestionResult 映射逻辑——COMPLETED/FAILED/DEGRADED 三路径覆盖不完整，partialData 写入时机和格式未定义 | §3.4 AiSuggestionResult 条目后新增"AiResult → AiSuggestionResult 映射规则"表格——覆盖正常完成/降级完成/明确失败/超时/DEGRADED+partialData 五路径；明确 partialData 写入时机（仅失败和超时路径写，正常和降级完成路径不写）和 JSON 格式（PrescriptionAssistResponse 字段级子集）；DEGRADED 归入 COMPLETED 的判定规则 |
| 3.[严重] 产出未参考需求中列明的 Phase 23 已有 OOD 草案 | §1.1 后新增"§1.1b 与已有 OOD 草案的继承关系"章节——明确继承自 a_v19 草案，列举一致的核心架构决策和定向演进方向；§7 设计决策表新增"Phase 23 OOD 草案继承关系"条目 |
| 4.[一般] §1.3 DosageCheckRequest.prescriptionId "必填"约束与 §3.4 后端自动生成 fallback 路径矛盾 | §1.3 DosageCheckRequest 将"必填"改为"主路径必填——空值时由后端自动生成并回写"；§3.4 "prescriptionId 分配时机"段落补充 API 验证层 @NotNull 校验跳过说明——check-dose 端点跳过 @NotNull 以允许 fallback 路径触发，/assist 和 audit 端点维持常规 @NotNull 校验 |
| 5.[一般] 所有 API 端点缺少结构化契约定义——缺少 JSON 示例、错误响应包装格式、HTTP 状态码枚举、认证头说明 | 新增 §4.6 结构化 API 契约章节——提供分诊/审核/病历/辅助开方各端点的完整请求/响应 JSON 示例；明确定义统一错误响应包装格式（errorCode/message/timestamp/path/details）和 BLOCK 阻断的独立 BlockResponse 格式；说明 API 契约获取方式（OpenAPI + 本设计 DTO 定义） |
| 6.[一般] AiSuggestionResult TTL 与 PrescriptionDraftContext TTL 一致性已修复，但 DedupTaskScheduler 去重判定仍可能因 TTL 差异导致逻辑残差——TTL 过期后前端仍可能持有旧 taskId 发起查询导致 RX_ASSIST_SUGGESTION_NOT_FOUND | §3.4 AiSuggestionResult 定义后新增"TTL 过期与 consumed 标记的协调策略"段落——定义 consumed=true 优先释放和 TTL 兜底双层逻辑；前端 TTL 过期处理策略（收到 NOT_FOUND 时自动发起新 check-dose 调用获取新 taskId）；§4.4 异步建议查询端点补充前端轮询行为和 TTL 过期重试策略 |
| 7.[一般] §5.1 错误码表缺少若干边界场景的错误码——TRIAGE_DOCTOR_FACADE_UNAVAILABLE、MR_GEN_TEMPLATE_LOAD_FAILED、RX_ASSIST_AI_SUGGEST_CREATE_FAILED、RX_AUDIT_CONCURRENT_SUBMIT | §5.1 错误码表补充四个错误码——分诊（非AI）行新增 TRIAGE_DOCTOR_FACADE_UNAVAILABLE；审核（非AI）行新增 RX_AUDIT_CONCURRENT_SUBMIT；病历（非AI）行新增 MR_GEN_TEMPLATE_LOAD_FAILED；开方辅助（AI）行新增 RX_ASSIST_AI_SUGGEST_CREATE_FAILED |
| 8.[轻微] §2.1 目录结构遗漏 DeadLetterEventRepository.java | §2.1 consultation/repository/ 目录行补充 DeadLetterEventRepository.java |
