# Phase 2/3 包C/D-AI1/D-AI2/E — 架构级 OOD 设计方案

## 1. 概述

### 1.1 设计目标

本设计覆盖 Phase 2 包C（智能分诊）与 Phase 3 包D-AI1（处方审核）、包D-AI2（病历生成）、包E（辅助开方）四个业务包。核心目标如下：

- **底座直接落地**：四个包均直接以 Maven 模块形式落地在 AIMedical 后端底座上，遵循 common → modules → application 的分层架构，规避后续迁移至 Phase 5 AI 进阶底座的重构成本。业务模块仅依赖 ai-api 的 AiService 接口（编译期依赖）；若 Phase 5 保持 AiService 接口签名和 DTO 字段结构不变，业务模块代码无须修改
- **架构风格一致**：四个包的模块结构、依赖方向和抽象层次与 Phase 0（骨架模块）、Phase 1（认证模块）的风格保持一致
- **强耦合同步落地**：包E（辅助开方）与包D-AI1（处方审核）共享处方领域数据和业务规则，设计为同一模块内的两个子域，同步开发、同步发布
- **AI 能力集成标准化**：四个包的业务逻辑均通过 ai-api 中的 AiService 接口调用 AI 能力，隔离 AI 实现细节；AI 不可用时的降级路径由各自模块的本地规则兜底，与 AiService 的降级框架协同
- **需求文档契约对齐**：所有模块的 API 输入/输出契约与需求文档 §3.4.x 定义的字段级契约严格对齐，确保验收可追溯
- **ai-api 层 DTO 与业务层 DTO 的契约完整性**：ai-api 层 DTO 承载 AI 推理所需的完整输入/输出语义，业务层 DTO 面向前端 API 契约，二者通过各模块 Converter 类完成映射/转换，确保字段级对齐

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
| TriageController | class | 分诊对话 REST 端点，接收前端提交的主诉/追问答复，返回推荐科室与推荐医生列表 |
| TriageService | interface | 智能分诊业务契约，封装单轮/多轮对话的分诊逻辑、对话状态管理和规则回退 |
| DialogueSession | class | 多轮对话会话状态对象，维护当前 session 的对话上下文、已收集的症状信息、当前轮次、规则版本与规则集快照、AI 连续失败计数 |
| DialogueSessionManager | class | 对话会话生命周期管理器，负责 session 的创建、查找、更新和过期清理，承担并发访问控制职责；统一生成 sessionId（UUID v4） |
| TriageRuleEngine | interface | 分诊规则引擎契约，根据症状匹配规则返回推荐科室；支持可配置规则源 |
| DepartmentFallbackProvider | interface | 兜底科室列表提供者契约，当 AI 无结果且规则匹配也无结果时返回静态兜底科室列表或基于简单规则的匹配结果 |
| DoctorFacade | interface | 跨模块医生信息查询门面，定义在 common-module-api 中，供 consultation 模块查询推荐医生列表和医生排班信息 |
| TriageResponse | class（DTO） | 分诊响应值对象，对齐需求文档 3.4.1 输出契约。包含推荐科室列表（departments，List\<RecommendedDepartment\>，每项含 departmentId / departmentName / score，0–3 项）、推荐医生列表（doctors，List\<RecommendedDoctor\>，每项含 doctorId / doctorName / departmentId / availableSlotCount / score，0–5 项）、推荐理由（reason，必填，字符数 ≥ 1，AI 无法生成时输出默认文案如"根据症状分析，建议就诊XX科室"，不可为空字符串或 null）、匹配规则列表（matchedRules，List\<MatchedRule\>，每项含 ruleId / ruleName / score）、会话标识（sessionId，多轮场景）、是否需要追问标记（needFollowUp）及追问内容（followUpQuestion）、置信度 confidence（可选）、降级标记（degraded，对齐需求文档 3.4.1 degraded 输出字段）、兜底提示（fallbackHint，AI 连续失败 3 次时携带"建议直接联系线下接诊窗口"文案）、规则版本不匹配标记（ruleVersionMismatch，可选，规则快照失效命中时为 true） |
| RecommendedDepartment | class（DTO） | 推荐科室值对象，对齐需求文档 3.4.1 recommended_departments 每项结构。包含 departmentId、departmentName、score |
| RecommendedDoctor | class（DTO） | 推荐医生值对象，对齐需求文档 3.4.1 recommended_doctors 每项结构。包含 doctorId、doctorName、departmentId、availableSlotCount（可预约时段数）、score（匹配评分）。推荐医生列表由后端根据 AI 推荐科室从 DoctorFacade 查询排班数据生成——AI 返回推荐科室后，TriageServiceImpl 对每个推荐科室调用 DoctorFacade 查询有排班的医生列表，按匹配评分排序取前 5 名，availableSlotCount 由 DoctorFacade 实时返回 |
| MatchedRule | class（DTO） | 匹配规则值对象，对齐需求文档 3.4.1 matched_rules 数组每项结构。包含 ruleId、ruleName、score |
| DialogueCreateRequest | class（DTO） | 分诊对话创建请求值对象，对齐需求文档 3.4.1 输入契约。包含患者主诉（chiefComplaint，必填，字符数 5–500，对齐需求文档 3.4.1 约束）、患者基本信息（patientId、age、gender）、会话标识（sessionId，必填——首轮请求由前端生成 UUID v4 传入，消除"首轮为空"特殊分支，对齐需求文档 3.4.1 session_id 必填语义）、规则版本号（ruleVersion，可选，对齐需求文档 3.4.1 rule_version）、规则集标识（ruleSetId，可选，对齐需求文档 3.4.1 rule_set_id；与 ruleVersion 语义不同——ruleVersion 标识规则版本号，ruleSetId 标识规则集实体标识）、追问回答数组（additionalResponses，可选，List\<AdditionalResponse\>，对齐需求文档 3.4.1 additional_responses；与 chiefComplaint 互斥——首轮请求仅传 chiefComplaint，后续多轮追问仅传 additionalResponses + sessionId，不同时提供） |

#### 包D-AI1（处方审核）

| 抽象 | 类型形态 | 职责定位 |
|------|---------|---------|
| PrescriptionAuditController | class | 处方审核 REST 端点，接收待审核处方，返回审核结果与风险等级；在收到 BLOCK 结果时拒绝处方提交，通过 HTTP 422 返回阻断详情 |
| PrescriptionAuditService | interface | 处方审核业务契约，协调 AI 审核调用与本地规则回退；审计结果的 BLOCK 等级具有强制阻断语义，调用方需据此执行阻断决策 |
| AuditRiskLevel | enum | 风险等级枚举，定义 PASS（通过）、WARN（警告）、BLOCK（阻断）三个级别；BLOCK 级别在后端具有强制阻断语义 |
| AuditRecord | JPA @Entity | 审核记录持久化实体，保存每次审核的原始处方快照（JSON 文本）、AI 结果、风险等级和时间戳，以及处方单号、医生 ID、患者 ID 等业务关联标识；含强制提交留痕字段和审核次序管理字段；撤销审核时该 AuditRecord 的 isLatest 回退为 false，确保后续查询最新审核结果不会返回已撤销的记录 |
| LocalRuleEngine | interface | 本地规则校验引擎契约，封装药物相互作用、过敏检查、合并症禁忌检查、剂量上限等规则；AI 超时或不可用时作为降级回退 |
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
| DrugContraindicationMapping | JPA @Entity | 药品禁忌症映射数据实体，drugCode 主键，关联禁忌症列表（contraindications，JSON TEXT——每项含 diseaseName / level / description）；由管理员维护，种子脚本初始化 |
| LocalRuleResult | class（DTO） | 本地规则校验结果值对象，包含规则标识（ruleId）、是否通过（passed）、消息文本（message）、严重程度（severity，类型为 AuditRiskLevel 枚举——值域为 WARN 或 BLOCK，PASS 级别的 LocalRuleResult 由 passed=true 隐含表达，passed=false 时 severity 必为 WARN 或 BLOCK） |

#### 包D-AI2（病历生成）

| 抽象 | 类型形态 | 职责定位 |
|------|---------|---------|
| MedicalRecordController | class | 病历生成 REST 端点，接收医生端对话文本，返回结构化病历；支持非流式与流式两种输出模式 |
| MedicalRecordService | interface | 病历生成业务契约，协调 AI 结构化输出与科室模板配置；支持流式与非流式两种生成模式 |
| DepartmentTemplateConfig | class | 科室模板配置值对象，按科室标识管理病历生成规则和模板版本 |
| TemplateConfigManager | interface | 科室模板配置管理器契约，支持模板的运行时查询、缓存和热加载；科室标识不存在时兜底返回 DEFAULT 模板 |
| MissingFieldDetector | interface | 关键字段缺失检测器契约，基于科室模板的必填字段列表与 AI 输出实际包含字段进行差集比对，输出缺失字段集合；不修改 AI 产出 |
| FieldMissingHint | class（DTO） | 字段缺失提示值对象，包含缺失字段标识（missingField，MedicalRecordField 枚举）、提示消息（promptMessage）、建议操作（suggestedAction） |
| RecordGenerateRequest | class（DTO） | 病历生成请求值对象，对齐需求文档 3.4.3 输入契约。包含对话文本（dialogueText）、患者 ID（patientId）、就诊标识（encounterId，映射为 MedicalRecord.visitId——后端 Service 层在生成记录时执行此转换）、流式输出标记（stream，bool，可选，默认 false；Phase 2/3 仅支持非流式模式，stream=true 时返回 MR_GEN_STREAM_NOT_SUPPORTED 错误码） |
| RecordGenerateResponse | class（DTO） | 病历生成响应值对象，对齐需求文档 3.4.3 输出契约。包含结构化病历内容（fields，以 MedicalRecordField 为键的键值映射）、缺失字段提示列表（missingFieldHints，List\<FieldMissingHint\>）、生成状态标记（降级标记 fromFallback） |

#### 包E（辅助开方）

| 抽象 | 类型形态 | 职责定位 |
|------|---------|---------|
| PrescriptionAssistController | class | 辅助开方 REST 端点；提供两个主端点：(1) POST /api/prescription/assist 作为 3.4.10 AI 辅助开方主端点；(2) POST /api/prescription/assist/check-dose 用于剂量阈值即时校验；以及异步 AI 建议查询端点 |
| PrescriptionAssistService | interface | 辅助开方业务契约，覆盖两个子域：(a) AI 辅助开方完整流程——接收诊断结论/检查检验结果/患者信息/已有处方，委托 AiService.prescriptionAssist() 生成处方草案；包括剂量告警和过敏告警的本地即时校验作为 AI 调用的前置/补充；(b) 剂量阈值即时校验——委托 DosageThresholdService |
| DosageThresholdService | class | 剂量阈值校验服务，按药品编码、给药途径、年龄范围、体重范围维度匹配剂量标准，检查剂量是否超限；支持日剂量校验和疗程剂量校验。输出三种告警类型：单次剂量超限→OVER_SINGLE_DOSE、日剂量超限→OVER_DAILY_DOSE、疗程剂量超限→OVER_DURATION（Phase 2/3 预留，暂不实现） |
| DosageAlert | class | 剂量告警值对象，封装告警级别（alertLevel，DosageAlertLevel 枚举）、告警类型（warningType，DoseWarningType 枚举：OVER_SINGLE_DOSE/OVER_DAILY_DOSE/OVER_DURATION）、告警消息、药品编码、当前剂量、建议值、错误码（errorCode，String，可选——承载剂量校验相关错误码，如 RX_ASSIST_DOSE_STANDARD_NOT_FOUND） |
| DosageAlertLevel | enum | 剂量告警级别枚举，定义 INFO/WARN/CRITICAL 三个级别；CRITICAL 写入处方草稿上下文，供处方提交时 BLOCK 判定消费 |
| DoseWarningType | enum | 剂量告警类型枚举，定义 OVER_SINGLE_DOSE（单次剂量超限）/ OVER_DAILY_DOSE（日剂量超限）/ OVER_DURATION（疗程剂量超限）三个值，对齐需求文档 3.4.10 warning_type 字段契约 |
| AiSuggestionResult | class | 异步 AI 结果值对象，封装 taskId、suggestion（String，COMPLETED 时填充）、status（AiSuggestionStatus 枚举：PENDING / COMPLETED / FAILED）、createTime、failReason（可选）、partialData（可选，AI 超时或部分生成时携带部分结果）；内部字段更新通过 ConcurrentHashMap.compute() 原子操作保护，同一 taskId 的状态更新保证幂等 |
| PrescriptionAssistResponse | class（DTO） | AI 辅助开方主端点响应值对象，对齐需求文档 3.4.10 输出契约。包含处方草案（prescriptionDraft，含 drugs 数组，每项含 drugId/drugName/dose/frequency/duration/route）、剂量告警列表（doseWarnings，List\<DoseWarning\>，每项含 drugId/warningType/message/severity）、过敏冲突告警（allergyWarnings，List\<AllergyWarningItem\>，每项含 drugId/allergen/severity）、免责声明标记（disclaimerRequired） |
| DosageCheckResponse | class（DTO） | 剂量阈值即时校验响应值对象，包含告警列表（alerts，List\<DosageAlert\>，每项含 warningType/alertLevel/message/drugCode/currentDose/suggestedValue/errorCode）和异步任务标识（taskId） |
| DoseWarning | class（DTO） | 剂量告警值对象，对齐需求文档 3.4.10 dose_warnings 数组每项结构。包含 drugId（药品编码）、warningType（DoseWarningType 枚举：OVER_SINGLE_DOSE/OVER_DAILY_DOSE/OVER_DURATION）、message（告警消息）、severity（DosageAlertLevel 枚举） |
| AllergyWarningItem | class（DTO） | 过敏冲突告警值对象，对齐需求文档 3.4.10 allergy_warnings 数组每项结构。包含 drugId（药品编码）、allergen（过敏原）、severity（AllergyWarningSeverity 枚举：HIGH/WARNING/INFO） |

#### 跨模块事件

| 抽象 | 类型形态 | 职责定位 |
|------|---------|---------|
| RegistrationEvent | class（事件） | 挂号事件契约，定义在 common-module-api 中。包含 registrationId、patientId、departmentId、doctorId、eventTime。事件发布端为 registration 模块，消费端为 consultation 模块，在 application 模块聚合后跨模块传播 |

---

## 2. 模块划分

### 2.1 目录结构

```
backend/modules/
├── consultation/                            # 包C 智能分诊
│   └── src/main/java/com/aimedical/modules/consultation/
│       ├── api/TriageController.java
│       ├── dto/TriageResponse.java, RecommendedDepartment.java, RecommendedDoctor.java,
│       │       MatchedRule.java, DialogueCreateRequest.java, AdditionalResponse.java
│       ├── service/
│       │   ├── TriageService.java
│       │   └── impl/TriageServiceImpl.java
│       ├── dialogue/
│       │   ├── DialogueSession.java         # 可变，由 DialogueSessionManager 管理；含 ruleVersion + ruleSetId 快照 + aiFailCount
│       │   └── DialogueSessionManager.java  # 生命周期管理 + 并发控制 + sessionId 生成（UUID v4）
│       ├── rule/
│       │   ├── TriageRuleEngine.java
│       │   ├── DefaultTriageRuleEngine.java # 数据库规则源 + 定时缓存刷新
│       │   └── entity/TriageRule.java       # 分诊规则实体（JPA @Entity）
│       ├── fallback/
│       │   ├── DepartmentFallbackProvider.java
│       │   └── StaticDepartmentFallbackProvider.java
│       ├── repository/TriageRecordRepository.java, TriageRuleRepository.java
│       ├── entity/TriageRecord.java         # 含 AI 推荐科室快照、推荐医生快照、规则匹配科室、最终选择科室、置信度、降级标记等
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
│       │       ├── DosageCheckRequest.java  # 含给药途径参数和用药频率
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
│       │   ├── entity/DrugCompositionDict.java     # JPA @Entity 药品成分字典实体
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
│       │   ├── DepartmentTemplateConfig.java
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

backend/modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/
├── auth/UserFacade.java                       # 跨模块用户认证门面接口
├── doctor/DoctorFacade.java                   # 跨模块医生信息查询门面接口
├── event/RegistrationEvent.java              # 挂号事件契约
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

#### 跨模块事件传递机制

RegistrationEvent 定义在 common-module-api 中（com.aimedical.modules.commonmodule.event.RegistrationEvent），事件发布端为 registration 模块（患者在挂号模块完成挂号后发布事件），消费端为 consultation 模块的 RegistrationEventListener（接收事件后补充写入 TriageRecord.finalDepartmentId 和 finalDepartmentName）。事件在 application 模块聚合后通过 Spring ApplicationEvent 机制跨模块传播。

**RegistrationEvent 消费失败补偿策略**：RegistrationEventListener 采用 @Retryable（Spring Retry，重试间隔 2s，最多 3 次，exponential backoff）处理事件消费失败。若重试仍失败，异常由 @DltHandler（死信队列处理器）接收，写入 dead_letter_event 表（含 eventPayload、failReason、failTime），由定时补偿任务（每 30 分钟扫描重试未成功的死信事件）兜底。

**TriageRecord.finalDepartmentId 为空场景处理**：当 RegistrationEvent 未到达或消费失败时，TriageRecord.finalDepartmentId 保持为 null。查询端返回分诊记录时 finalDepartmentId=null 表示"患者尚未完成挂号确认"或"挂号事件尚未同步完成"。前端根据 finalDepartmentId=null 显示"待挂号确认"提示，并提供"手动选择科室"入口直接填写科室。此设计确保 RegistrationEvent 消费链路的中断不影响分诊记录查询功能。

---

## 3. 核心抽象

### 3.1 包C：智能分诊

#### TriageService（interface）

职责：定义智能分诊的核心业务边界，覆盖单轮分诊、多轮追问和会话管理。

协作对象：被 TriageController 调用（前端仅发送 sessionId 和新内容）；委托 AiService.triage() 进行 AI 分诊，AI 无结果时降级至 TriageRuleEngine.match()，规则匹配无结果时继续调用 DepartmentFallbackProvider.getFallbackDepartments() 获取兜底科室列表；管理 DialogueSessionManager。降级判定语义：AiResult.success=false 或 AiResult.degraded=true 时判定 AI 不可用、触发降级；AiResult.success=true 但推荐列表为空视为有效结果（AI 明确判断无匹配科室），跳过规则引擎。

**AI 连续失败兜底机制**：每次 AI 调用失败（含超时和不可用）时，DialogueSession.aiFailCount 递增。TriageServiceImpl 在降级后检查 aiFailCount，当 aiFailCount >= 3 时，在 TriageResponse 中附加 fallbackHint 字段，携带"建议直接联系线下接诊窗口"兜底文案。AI 调用成功时 aiFailCount 重置为 0。

**AI 调用上下文传递策略**：TriageServiceImpl 在调用 AiService.triage() 前，将多轮对话完整上下文组装为 ai-api 层 TriageRequest。组装内容：从 DialogueSession 取主诉和全部追问 QA 历史，拼接到 TriageRequest.chiefComplaint 字段（首轮仅主诉，多轮时将主诉+历次 QA 按模板格式拼合为完整推理 prompt）；TriageRequest.additionalResponses 字段填充当轮新增的追问回答列表。采用"全量拼接"策略——每次调用将完整上下文传入 AiService，AI 不依赖自身侧会话状态，确保无状态 AI 实现的可行性。

**推荐医生列表生成机制**：AI 调用返回推荐科室列表后，TriageServiceImpl 对每个推荐科室调用 DoctorFacade.findAvailableDoctorsByDepartment(departmentId) 查询当前有排班的医生列表，按匹配评分排序取前 5 名，填充 availableSlotCount 和 score 字段。AI 不直接返回推荐医生列表——AI 聚焦科室级推理，医生匹配由后端基于排班数据生成。DoctorFacade.findAvailableDoctorsByDepartment() 返回医生基础信息 + 当日可预约时段数，实时查询确保 availableSlotCount 的时效性。

**TriageRecord 写入时机**：分诊结果返回响应前同步写入 TriageRecord（含 AI 推荐科室快照、推荐医生快照、规则匹配科室、置信度、降级标记等）。首次写入时 finalDepartmentId 为空——患者未挂号前尚无最终选择科室；finalDepartmentId 在患者挂号后通过 RegistrationEvent 由事件监听器补充写入。

**降级时前端行为指引**：TriageResponse.degraded=true 时，前端仍渲染推荐科室列表（规则匹配/兜底结果），同时显示降级提示文案（"AI 分诊暂时不可用，推荐结果基于规则匹配"）并提供"手动选择科室"入口，对齐需求文档 3.1.3.1"回退到按科室选择医生模式"降级路径。

为何使用 interface：分诊业务可能存在多种实现（普通分诊 vs 急诊分诊），interface 允许扩展而不影响调用方。

#### DialogueSession（class，可变）

职责：封装一次多轮分诊的完整上下文（sessionId、主诉、症状列表、追问轮次、会话状态、规则版本快照 ruleVersion、规则集快照 ruleSetId、AI 连续失败计数 aiFailCount）。首轮创建时，DialogueSessionManager 记录当前规则引擎的版本号存入 DialogueSession.ruleVersion 和规则集标识存入 DialogueSession.ruleSetId；后续追问使用该快照版本对应的规则，确保对话内分诊逻辑前后一致。

协作：被 DialogueSessionManager 创建/修改/持久化。

为何使用可变 class：每轮追问追加 QA，可变避免频繁对象复制。DialogueSessionManager 承担并发控制——同 session 请求串行，不同 session 独立。

#### DialogueSessionManager（class）

职责：管理 DialogueSession 的创建、查找、更新和过期清理。承担并发访问的唯一控制点。**sessionId 生成**：统一由 DialogueSessionManager 生成，采用 UUID v4 格式（36 字符，含连字符）。首轮请求时前端生成 sessionId 传入（对齐需求文档 3.4.1 session_id 必填语义），服务端通过 DialogueSessionManager.findOrCreate() 完成创建或恢复——若 sessionId 对应的会话不存在则创建（同时生成/确认 sessionId 并记录当前 ruleVersion 和 ruleSetId），有效则恢复，已超时则返回 TRIAGE_SESSION_EXPIRED 错误。

协作：被 TriageService 调用。内部维护 ConcurrentHashMap + ScheduledExecutorService（TTL 30 分钟）。

为何使用 class 而非 interface：存储策略虽有变更可能，但管理器职责边界稳定，interface 抽象收益在当前阶段不抵实现复杂度。

**TTL 清理竞态处理**：ScheduledExecutorService 清理超时 session 与并发请求访问存在竞态。ConcurrentHashMap.remove(key) 为原子操作——若线程 A（定时清理）先于线程 B（业务请求）执行 remove，线程 B 的 findOrCreate() 发现 session 不存在后返回 TRIAGE_SESSION_EXPIRED 错误由前端处理。若线程 B 先获取到 session 引用后线程 A 执行清理，线程 B 使用的是已获取的引用（不会 NPE），后续该 session 不会再被其他请求访问。此竞态路径被覆盖，无需额外加锁。

#### TriageRuleEngine（interface）

职责：症状→科室匹配规则引擎。规则源为数据库 TriageRule 实体，支持热加载。提供 currentRuleVersion() 方法返回当前规则版本号，提供 currentRuleSetId() 方法返回当前规则集标识，供 DialogueSessionManager 在创建会话时快照。提供 match() 方法：输入主诉文本 + ruleVersion + ruleSetId，输出 List\<RecommendedDepartment\>。

**规则快照失效处理**：当 match() 使用快照版本（ruleVersion + ruleSetId）查询无结果时（管理员已删除/禁用对应的规则集），降级使用当前最新版本规则集重新匹配，并在 TriageResponse 中标记 ruleVersionMismatch=true（可选），避免因规则管理操作导致分诊完全失败。

协作：被 TriageService 在 AI 不可用时调用。配合 Caffeine refreshAfterWrite（默认 60 秒）定时刷新规则缓存。

为何使用 interface：规则源可能从数据库演化为分布式规则引擎。

#### DepartmentFallbackProvider（interface）

职责：AI 和规则引擎均无法决策时返回静态兜底科室列表。

#### TriageRule（JPA @Entity）

职责：分诊规则持久化实体，承载 TriageRuleEngine 的规则数据来源。核心字段：ruleId（规则标识）、ruleSetId（所属规则集标识）、ruleVersion（规则版本号）、conditions（规则匹配条件，JSON 文本，存储症状关键词匹配逻辑）、resultDepartmentId（命中后推荐科室 ID）、resultDepartmentName（推荐科室名称）、score（匹配评分权重）、enabled（是否启用）、createTime、updateTime。按 ruleSetId + ruleVersion 组织规则集，支持规则版本化管理和热加载。

#### DoctorFacade（interface）

职责：跨模块医生信息查询门面，定义在 common-module-api 中。提供 findAvailableDoctorsByDepartment(departmentId) 方法——查询指定科室当前有排班的医生列表，返回 List\<AvailableDoctor\>（每项含 doctorId / doctorName / departmentId / availableSlotCount），为 TriageServiceImpl 生成推荐医生列表提供数据来源。availableSlotCount 实时查询排班数据，确保时效性。

协作：在 common-module-api 中定义接口，由 doctor 模块实现，application 聚合后 Spring 自动注入。与 UserFacade 模式一致。

为何使用 interface：解耦 consultation 模块与 doctor 模块的编译期依赖，遵循模块间通过门面接口协作的架构约束。

为何放在 doctor 子包而非 auth 子包：DoctorFacade 是跨模块医生排班/可用性查询门面，与用户认证（auth）职责不相关。独立子包（commonmodule/doctor/）使接口的语义定位与包名一致。

#### TriageResponse（class，DTO）

职责：封装分诊响应的结构化数据，对齐需求文档 3.4.1 输出契约。包含推荐科室列表（departments，List\<RecommendedDepartment\>，0–3 项）、推荐医生列表（doctors，List\<RecommendedDoctor\>，0–5 项）、推荐理由（reason，必填字段，字符数 ≥ 1）、匹配规则列表（matchedRules，List\<MatchedRule\>）、会话标识（sessionId）、是否需要追问标记（needFollowUp）及追问内容（followUpQuestion）、置信度（confidence，可选）、降级标记（degraded）、兜底提示（fallbackHint，可选）、规则版本不匹配标记（ruleVersionMismatch，可选，规则快照失效命中时为 true）。

#### DialogueCreateRequest（class，DTO）

职责：封装分诊对话请求值对象，对齐需求文档 3.4.1 输入契约。包含患者主诉（chiefComplaint，必填，字符数 5–500）、患者基本信息（patientId、age、gender）、会话标识（sessionId，必填——首轮请求由前端生成 UUID v4 传入，消除首轮为空特殊分支，与需求文档 3.4.1 session_id 必填语义对齐）、规则版本号（ruleVersion，可选）、规则集标识（ruleSetId，可选）、追问回答数组（additionalResponses，可选，List\<AdditionalResponse\>；与 chiefComplaint 互斥）。

**chiefComplaint 与 additionalResponses 的互斥语义**：首轮请求仅传 chiefComplaint（必填），additionalResponses 不传；后续多轮追问仅传 additionalResponses + sessionId，chiefComplaint 不传（或传空）。TriageServiceImpl 在处理时校验此互斥规则。

#### TriageRecord（JPA @Entity）

职责：分诊结果持久化实体。包含以下字段：sessionId（会话标识）、patientId（患者 ID）、chiefComplaint（主诉快照）、aiRecommendedDepartments（AI 推荐科室快照，JSON 文本）、recommendedDoctors（推荐医生快照，JSON TEXT——存储 TriageResponse.doctors 列表快照，每项含 doctorId / doctorName / departmentId / availableSlotCount / score；对齐需求文档 §5.1 分诊记录核心字段"推荐医生"）、ruleMatchedDepartments（规则匹配科室快照，JSON 文本）、finalDepartmentId（最终选择科室 ID，nullable——首次写入时为空，患者挂号后通过事件/回调补充写入）、finalDepartmentName（最终选择科室名称，nullable）、confidence（置信度）、degraded（降级标记，boolean）、ruleVersion（使用的规则版本号）、ruleSetId（使用的规则集标识）、triageTime（分诊时间戳）。

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
- prescriptionOrderId：关联的处方单号
- doctorId：开方医生 ID
- patientId：患者 ID
- auditTime：审核时间戳
- fromFallback：boolean 标记，为 true 表示降级路径
- **forceSubmitted**：Boolean，标记医生是否执行了强制提交
- **forceSubmitTime**：LocalDateTime，强制提交时间戳
- **auditSequence**：int，必填，审核次序
- **isLatest**：boolean，必填，是否最新——同一 prescriptionOrderId 下仅最新一条记录 isLatest=true
- **originalPrescription**：JSON TEXT，存储完整处方快照——用于 WARN 级强制提交时的处方版本校验（比较当前处方与 AuditRecord.originalPrescription 是否一致，防止审核后处方被篡改再提交）
- riskLevel、aiResult、auditIssues 等审核结果数据

**撤销审核时 isLatest 处理**：当医生对 WARN 级审核结果执行"撤销审核"操作时，该 AuditRecord 的 isLatest 回退为 false（而非删除该记录——"审核记录仍持久化保存"），确保后续查询最新审核结果不会返回已撤销的记录。撤销后处方状态回退至"草稿"。

同一处方多次审核时的处理：每次新审核写入 AuditRecord 时，先将同一 prescriptionOrderId 下已有的 isLatest=true 的记录更新为 isLatest=false，再插入新记录并置 auditSequence = 上一条记录的 auditSequence + 1，isLatest=true。此操作在事务内完成保证一致性。

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
- DrugCompositionDict：JPA @Entity（prescription/rule/entity/DrugCompositionDict.java），drugCode 主键，含 ingredients（成分列表，JSON TEXT）。由管理员维护；Phase 2/3 以种子脚本初始化。DrugCompositionDictRepository 提供按 drugCode 查询能力
- DuplicateCheckRule 逻辑：遍历处方药品列表，对每个药品查询 DrugCompositionDict 获取成分列表，构建 drugCode → ingredientSet 映射后检测成分交集——存在两种及以上药品共享相同成分时产出 WARN 级别 LocalRuleResult
- SpecialPopulationDosageRule 逻辑：根据患者年龄判断是否属于特殊人群（儿童 ≤ 14 岁、老年 ≥ 65 岁），若是则查询 DosageStandard 中对应年龄/体重分级的剂量标准，与处方中实际剂量做比对——超出该人群特殊剂量上限时产出 BLOCK 级别 LocalRuleResult

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

职责：结构化病历持久化实体。核心字段包含：记录标识（recordId）、患者 ID（patientId）、就诊标识（visitId，必填）、就诊科室（departmentId）、病历内容（contentJson，TEXT 类型 + JPA @Convert/Jackson 序列化，存储为单列 JSON）、医生 ID（doctorId）、创建时间、更新时间。病历内容通过 MedicalRecordField 枚举标识顶层字段，每个字段对应病历中的一个语义段落。

**病历内容存储形式**：采用单列 JSON TEXT（contentJson 字段）存储，通过 JPA @Convert（AttributeConverter）+ Jackson 实现 Java 对象与 JSON 文本的自动双向转换。此设计将 7 个结构化字段聚合为单列存储，避免频繁的列级 DDL 变更，与 MedicalRecordField 枚举的扩展性保持一致。

**MedicalRecordRepository 核心查询方法**：findByVisitId(visitId)——按就诊标识查询病历、findByPatientId(patientId)——按患者 ID 查询病历列表、findById(recordId)——按记录标识查询病历详情。病历更新方法支持增量更新语义——读取 contentJson、合并变更字段、写回——确保部分字段更新时其他字段不受影响。

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

**非流式超时降级路径**：使用现有 AiResult.data 字段承载部分生成结果（不新增 partialData 字段）。AiResult 在超时场景下承载 failure+errorCode+partialData 组合——通过新增重载 AiResult.failure(String errorCode, T partialData) 实现（或由 AI 实现直接构造 AiResult(success=false, partialData, errorCode, degraded=false, fallbackReason=null)）。MedicalRecordService 在超时时从 AiResult.data 中提取已生成的部分字段，按分层保护策略返回——保留已提取字段 + 缺失字段标记 + MR_GEN_AI_TIMEOUT 错误码。前端对已生成字段正常渲染，缺失字段显示补全提示。降级场景（success=false + degraded=true + data 含部分结果 + fallbackReason）通过新增重载 AiResult.degraded(String fallbackReason, T partialData) 实现。

超时配置：ai.timeout.medical-record-generate=12s。

#### RecordGenerateRequest（class，DTO）

职责：病历生成请求值对象，对齐需求文档 3.4.3 输入契约。包含对话文本（dialogueText，必填，字符数 50–10000）、患者 ID（patientId）、就诊标识（encounterId，此字段映射为 MedicalRecord.visitId——Service 层在生成病历记录时执行 encounterId → visitId 转换，确保 encounter_id 与 visit 概念的一致性）、流式输出标记（stream，bool，可选，默认 false）。

**流式输出 Phase 2/3 实现决策**：Phase 2/3 仅实现非流式模式。stream=true 时返回 MR_GEN_STREAM_NOT_SUPPORTED 错误码。

#### RecordGenerateResponse（class，DTO）

职责：病历生成响应值对象，对齐需求文档 3.4.3 输出契约。包含结构化病历内容（fields）、缺失字段提示列表（missingFieldHints，为需求文档 missing_fields 的结构化升级版本）、生成状态标记（fromFallback）。

### 3.4 包E：辅助开方

#### PrescriptionAssistService（interface）

职责：**AI 辅助开方完整流程**——接收诊断结论、检查检验结果、患者信息、已有处方，委托 AiService.prescriptionAssist() 生成处方草案；本地即时校验（剂量阈值 DosageThresholdService + 过敏冲突本地检查）作为 AI 调用的前置补充，产出 doseWarnings 和 allergyWarnings。**剂量阈值即时校验子域**——接收单药品剂量检查参数，委托 DosageThresholdService 做即时阈值校验。

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

按 drugCode + routeOfAdministration + ageRange + weightRange 查询 DosageStandard，比较剂量阈值。匹配优先级策略：精确匹配→年龄范围匹配→体重范围匹配→无分级默认阈值→标准不存在（四级均未命中，降级返回 WARN 级 DosageAlert 并携带 errorCode=RX_ASSIST_DOSE_STANDARD_NOT_FOUND）。三种输出路径及对应的 warningType 赋值规则：(1) 单次剂量超过上限→DoseWarningType.OVER_SINGLE_DOSE；(2) 日剂量校验分支：当 DosageCheckRequest 携带 frequency 且 DosageStandard 存在 dailyMax 时，校验单次剂量 × 频率是否超出日剂量上限→DoseWarningType.OVER_DAILY_DOSE；(3) 疗程剂量超限校验→DoseWarningType.OVER_DURATION（Phase 2/3 预留，暂不实现）。

#### AiSuggestionResult（class）

异步 AI 结果值对象，封装 taskId、suggestion（String，COMPLETED 时填充）、status（AiSuggestionStatus 枚举：PENDING / COMPLETED / FAILED）、createTime、failReason（可选）、partialData（可选，JSON 文本，AI 超时或部分生成时携带部分推荐结果）。

并发安全策略：内部字段更新通过 ConcurrentHashMap.compute() 原子操作保护。

查询时遵循四分支模式：不存在→RX_ASSIST_AI_SUGGESTION_NOT_FOUND、PENDING、COMPLETED、FAILED + failReason。

预创建→更新模式与前一版一致。

AiSuggestionResult 增加 TTL（30 分钟），由 ScheduledExecutorService 定期清理过期条目。

#### PrescriptionDraftContext（class）

职责：处方草稿上下文，在医生编辑处方期间暂存 CRITICAL 级别剂量告警标记。Key 为 prescriptionId，内部使用 ConcurrentHashMap\<String, List\<DosageAlert\>\> 存储。

生命周期管理：创建时机（首次 check-dose 请求时）、写入时机（CRITICAL 告警时，每次 check-dose 重算后覆盖该 prescriptionId 下已有的所有 CRITICAL 标记——若新结果无 CRITICAL 则清除旧标记）、清理时机（处方提交成功/处方取消/TTL 过期 60 分钟）。

覆盖更新行为契约：每次 check-dose 请求执行后，DosageThresholdService 将当前校验结果中 alertLevel=CRITICAL 的告警列表写入 PrescriptionDraftContext（以 prescriptionId 为键覆盖写入），非 CRITICAL 告警不入上下文。若当前校验结果无 CRITICAL 告警，则清除该 prescriptionId 对应的上下文条目。此"全量覆盖"语义确保旧 CRITICAL 标记不会因新的检查结果而残留。

#### DoseWarningType（enum）

职责：剂量告警类型枚举，定义 OVER_SINGLE_DOSE（单次剂量超限）、OVER_DAILY_DOSE（日剂量超限）、OVER_DURATION（疗程剂量超限）三个值。此枚举与 DosageAlertLevel（告警严重程度维度）正交——warningType 标识"什么类型的剂量超标"，alertLevel 标识"超标严重程度"。OVER_DURATION 在 Phase 2/3 预留暂不实现。

#### PrescriptionAssistResponse（class，DTO）

职责：AI 辅助开方主端点响应值对象，对齐需求文档 3.4.10 输出契约。包含处方草案（prescriptionDraft，含 drugs 数组，每项含 drugId/drugName/dose/frequency/duration/route）、剂量告警列表（doseWarnings，List\<DoseWarning\>，每项含 drugId/warningType（DoseWarningType 枚举）/message/severity（DosageAlertLevel 枚举））、过敏冲突告警（allergyWarnings，List\<AllergyWarningItem\>，每项含 drugId/allergen/severity（AllergyWarningSeverity 枚举））、顶层错误码（errorCode，可选，String——AI 返回无可推荐药品时填充 RX_ASSIST_AI_NO_RECOMMENDATION）、免责声明标记（disclaimerRequired，boolean，固定 true）。

#### AllergyWarningSeverity（enum）

职责：过敏告警严重程度枚举，定义 HIGH / WARNING / INFO 三个级别。HIGH 表示严重过敏冲突（语义对应 AuditRiskLevel.BLOCK 但不等同——辅助开方 allergyWarnings.severity=HIGH 为编辑阶段即时提示，处方审核的 BLOCK 为提交时正式阻断判定，二者独立执行不互斥），WARNING 表示中度冲突，INFO 表示轻微提示。此枚举与 DosageAlertLevel（INFO/WARN/CRITICAL）和 AlertSeverity（INFO/WARNING/CRITICAL）分属不同告警维度。

#### DoseWarning（class，DTO）

职责：剂量告警值对象，对齐需求文档 3.4.10 dose_warnings 数组每项结构。包含以下字段：
- drugId（string，药品编码）
- warningType（DoseWarningType 枚举：OVER_SINGLE_DOSE / OVER_DAILY_DOSE / OVER_DURATION，告警类型）
- message（string，告警消息）
- severity（DosageAlertLevel 枚举：INFO / WARN / CRITICAL，告警严重程度）

#### AllergyWarningItem（class，DTO）

职责：过敏冲突告警值对象，对齐需求文档 3.4.10 allergy_warnings 数组每项结构。包含以下字段：
- drugId（string，药品编码）
- allergen（string，过敏原）
- severity（AllergyWarningSeverity 枚举：HIGH / WARNING / INFO，告警严重程度）

---

## 4. 关键行为契约

### 4.1 智能分诊场景

```
POST /api/triage/consult
单轮: { chiefComplaint: "胸痛伴气短", patientId: "P001", age: 45, gender: "男", sessionId: "前端生成UUID v4" }
      chiefComplaint 字符数约束: 5–500
多轮: { sessionId: "xxx", additionalResponses: [{question: "...", answer: "...", answeredAt: "..."}] }
      首轮仅传 chiefComplaint，后续轮仅传 additionalResponses + sessionId，二选一互斥

AI 调用上下文组装:
  TriageServiceImpl 调用 AiService.triage() 前，从 DialogueSession 取完整对话上下文
  → 首轮: TriageConverter.toAiTriageRequest() 将 chiefComplaint 映射为 ai-api TriageRequest.chiefComplaint
  → 多轮: TriageConverter.toAiTriageRequest() 将主诉+历次 QA 按模板格式拼合为 TriageRequest.chiefComplaint
       + 当轮 additionalResponses 映射为 TriageRequest.additionalResponses
  ai-api TriageRequest 扩展 additionalResponses 字段（List<AdditionalResponseItem>，
  每项含 question/answer/answeredAt），对齐需求文档 3.4.1 多轮调用组装约定

推荐医生列表生成:
  AI 返回推荐科室列表后，TriageServiceImpl 对每个推荐科室调用 DoctorFacade.findAvailableDoctorsByDepartment(departmentId)
  → 获取当前有排班的医生列表，按匹配评分排序取前 5 名
  → AvailableDoctor.doctorId/doctorName/departmentId/availableSlotCount 映射为 RecommendedDoctor

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
                    3. 撤销审核 → 该 AuditRecord.isLatest 回退为 false，处方状态回退至"草稿"
  BLOCK（高风险） → PrescriptionAuditEnforcer 执行阻断 → 返回 HTTP 422 + BlockResponse
                     前端弹窗展示阻断原因列表，禁止提交

处方提交端点设计边界:
  POST /api/prescription/submit（Phase 2/3 简要契约定义）
  请求: { prescriptionId, prescriptionItems, forceSubmit(bool, 默认 false), auditRecordId(可选) }
  行为:
    1. forceSubmit=false 时执行常规审核流程（若待提交处方无最新审核结果则先调用审核）
    2. forceSubmit=true 时校验 auditRecordId 对应 AuditRecord.riskLevel=WARN 且 isLatest=true
       + 处方版本校验（当前处方与 originalPrescription 一致）
       → 校验通过：写入 forceSubmitted/forceSubmitTime → 处方落单
       → 校验失败：返回错误码
    3. 最新审核结果为 BLOCK 时，无论 forceSubmit 值如何均拒绝提交（HTTP 422）
    4. 提交前检查 PrescriptionDraftContext 中该 prescriptionId 是否存在 CRITICAL 级别剂量告警：若存在则拒绝提交，返回 HTTP 422 + BlockResponse（阻断原因含"处方存在 CRITICAL 级别剂量告警，禁止提交"）。CRITICAL 阻断独立于审核 BLOCK 阻断——二者为不同判断维度，任一命中均拒绝提交

同一处方多次审核：
  每次审核写入 AuditRecord 时，同一 prescriptionOrderId 下已有记录 isLatest→false
  新记录 auditSequence 递增、isLatest=true
```

### 4.3 病历生成场景

```
POST /api/medical-record/generate
{ dialogueText, patientId, encounterId, stream }
超时配置: ai.timeout.medical-record-generate=12s（非流式）

stream=false（Phase 2/3 仅支持非流式）:
  正常: TemplateConfigManager.getTemplate → AiService.generateMedicalRecord → MissingFieldDetector
  兜底: 科室不存在时使用 DEFAULT 模板
  超时降级: AiResult.data（使用现有 data 字段承载部分结果）携带部分生成结果
    → MedicalRecordService 提取已生成部分字段 → 保留已生成字段 + 标记缺失字段
    → 返回部分 RecordGenerateResponse + missingFieldHints + MR_GEN_AI_TIMEOUT 标记
    → 前端: 已生成字段正常渲染 + 缺失字段显示补全提示输入框
  完全降级: AI 完全不可用 → 返回空字段集
    → 差集比对 → 产出 FieldMissingHint 列表
    → 前端: 缺失字段显示补全提示输入框

stream=true（Phase 2/3 暂不支持）:
  → 返回错误码 MR_GEN_STREAM_NOT_SUPPORTED
```

### 4.4 辅助开方场景

```
[主端点] POST /api/prescription/assist
请求: { diagnosis, examResults, patientInfo, existingPrescription, encounterId }
→ AssistConverter.toAiPrescriptionAssistRequest() 组装 ai-api 层 PrescriptionAssistRequest
→ 本地即时校验: 过敏冲突检查（patientInfo.allergyDetails 优先 / allergyHistory 回退 + prescriptionDraft.drugs 交叉比对）
→ DosageThresholdService 对处方草案中每项药品做剂量阈值校验
→ 返回 PrescriptionAssistResponse: { prescriptionDraft, doseWarnings, allergyWarnings, disclaimerRequired }

AI 返回无可推荐药品场景:
  AiResult.success=true 且 prescriptionDraft.drugs 为空列表
  → 返回空 prescriptionDraft + 本地校验结果 + PrescriptionAssistResponse.errorCode=RX_ASSIST_AI_NO_RECOMMENDATION
  → 前端展示"AI 暂无可推荐药品，请手动开方"

[即时校验子端点] POST /api/prescription/assist/check-dose
请求: { drugCode, dosage, unit, routeOfAdministration, patientAge, patientWeight, frequency }
→ 单位一致性校验
→ DosageThresholdService.check(drugCode, route, dosage, age, weight, frequency)
→ CRITICAL 告警写入 PrescriptionDraftContext（key=prescriptionId，全量覆盖——每次重算后清除该 prescriptionId 旧标记，写入当前校验结果中的 CRITICAL 告警列表；若当前结果无 CRITICAL 告警则清除对应条目）
→ 返回 DosageCheckResponse: { alerts, taskId }
→ 同时以 PENDING 状态预创建 AiSuggestionResult
→ 四级均未命中时 DosageAlert.errorCode=RX_ASSIST_DOSE_STANDARD_NOT_FOUND

[异步建议查询] GET /api/prescription/assist/suggestion/{taskId}
→ 不存在 / TTL 过期: RX_ASSIST_AI_SUGGESTION_NOT_FOUND
→ PENDING / COMPLETED / FAILED
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

ai-api 层与业务层 DTO 命名空间区分：：
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

---

## 5. 错误处理策略

### 5.1 模块级错误码

**错误码命名规则**：AI 相关错误码遵循 `<前缀>_AI_<类型>` 命名约定（如 TRIAGE_AI_TIMEOUT、RX_AUDIT_AI_TIMEOUT），非 AI 业务逻辑错误码使用 `<前缀>_<类型>` 命名（如 TRIAGE_SESSION_EXPIRED、RX_ASSIST_DOSE_STANDARD_NOT_FOUND、RX_AUDIT_PRESCRIPTION_MODIFIED），二者的区分规则：凡涉及 AI 推理调用（超时、不可用、输入校验失败、无可推荐结果、输出不完整等）的错误码均保留 `_AI_` 中段，纯业务逻辑错误码不含 `_AI_`。

| 错误类别 | 前缀 | 代表场景 |
|---------|------|---------|
| 分诊（AI） | TRIAGE_ | TRIAGE_AI_TIMEOUT、TRIAGE_AI_UNAVAILABLE、TRIAGE_AI_INPUT_INVALID |
| 分诊（非AI） | TRIAGE_ | TRIAGE_SESSION_EXPIRED |
| 审核（AI） | RX_AUDIT_ | RX_AUDIT_AI_TIMEOUT、RX_AUDIT_AI_UNAVAILABLE、RX_AUDIT_AI_INPUT_INVALID |
| 审核（非AI） | RX_AUDIT_ | RX_AUDIT_PRESCRIPTION_MODIFIED（处方版本校验不一致）、RX_AUDIT_BLOCK（BLOCK 阻断） |
| 病历（AI） | MR_GEN_ | MR_GEN_AI_TIMEOUT、MR_GEN_AI_UNAVAILABLE、MR_GEN_AI_INPUT_INVALID、MR_GEN_AI_OUTPUT_INCOMPLETE |
| 病历（非AI） | MR_GEN_ | MR_GEN_STREAM_NOT_SUPPORTED |
| 开方辅助（AI） | RX_ASSIST_ | RX_ASSIST_AI_TIMEOUT、RX_ASSIST_AI_UNAVAILABLE、RX_ASSIST_AI_INPUT_INVALID、RX_ASSIST_AI_NO_RECOMMENDATION |
| 开方辅助（非AI） | RX_ASSIST_ | RX_ASSIST_DOSE_STANDARD_NOT_FOUND、RX_ASSIST_AI_SUGGESTION_NOT_FOUND |

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

---

## 6. 并发设计

### 6.1 对话会话并发管理

ConcurrentHashMap 存储，同 session 请求串行（前端等待响应），不同 session 独立。ScheduledExecutorService 每 5 分钟扫描清理超时 30 分钟的 session。

**TTL 清理竞态处理**：ConcurrentHashMap.remove(key) 为原子操作——若定时清理线程先于业务请求线程执行 remove，业务线程发现 session 不存在后返回 TRIAGE_SESSION_EXPIRED 错误由前端处理，此竞态路径被覆盖，无需额外加锁。

**部署约束与水平扩展**：Phase 2/3 假设单实例部署或 sticky session（通过负载均衡器配置），ConcurrentHashMap 存储在此假设下可正常工作。若需多实例部署，三项内存存储（DialogueSessionManager 会话存储、AiSuggestionResult 存储、PrescriptionDraftContext）均需替换为分布式缓存（如 Redis）。Phase 5 迁移节点：三项内存存储统一迁移至持久化或分布式缓存，届时需：
- DialogueSessionManager：ConcurrentHashMap → Redis Hash + TTL
- AiSuggestionResult 存储：ConcurrentHashMap → Redis String + TTL
- PrescriptionDraftContext：ConcurrentHashMap → Redis Hash + TTL
此迁移在 Phase 5 AI 进阶底座落地时同步执行，与"Phase 2~4 各阶段 AI 能力统一迁移至底座"的路线图一致。

### 6.2 AI 调用并发

统一 CompletableFuture<AiResult<T>>，Service 层同步等待 AI 结果。

### 6.3 包E 的异步 AI 建议

@Async / CompletableFuture.runAsync() 调用 AiService.prescriptionAssist()。主响应返回 taskId 时预创建→更新模式与前一版一致。

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
| 底座落地与 Phase 5 迁移兼容性 | 业务模块仅依赖 ai-api 的 AiService 接口（编译期依赖）；若 Phase 5 保持 AiService 接口签名和 DTO 字段结构不变，业务模块代码无须修改 | 接口不变是迁移关键前提——AiService 接口定义在 ai-api 模块，业务模块通过接口调用 AI 能力，实现类替换对业务模块透明；若 Phase 5 需扩展 DTO 字段，业务模块的 Converter 需同步更新 |
| TTL 清理竞态 | ConcurrentHashMap.remove() 原子性保证下若 session 已被清理，findOrCreate() 返回 TRIAGE_SESSION_EXPIRED 错误 | 竞态路径被覆盖——清理先于访问则视为 session 已超时，访问先于清理则使用已获取的引用 |
| CRITICAL 剂量告警阻断链路 | PrescriptionDraftContext 按 prescriptionId 全量覆盖存储 CRITICAL 告警；每次 check-dose 重算后覆盖写入；提交端点提交前检查 PrescriptionDraftContext 中是否存在 CRITICAL 告警 | CRITICAL 阻断与审核 BLOCK 阻断分属不同判断维度，任一命中均拒绝提交，确保处方安全闭环；全量覆盖语义避免旧 CRITICAL 残留 |
| OVER_DURATION 实现范围 | Phase 2/3 预留暂不实现，DosageThresholdService 中标记为预留路径 | 疗程剂量超限检查依赖疗程持续时间参数和累积剂量计算，业务规则复杂度较高，Phase 2/3 核心需求优先级聚焦单次剂量和日剂量，疗程剂量推迟至 Phase 4 实现 |
| 规则快照失效处理 | match() 使用快照版本查询无结果时降级使用当前最新版本规则集重新匹配 | 避免因管理员删除/禁用规则集导致分诊完全失败；ruleVersionMismatch 标记供前端提示 |
| 降级时前端行为 | TriageResponse.degraded=true 时前端仍渲染推荐科室列表 + 降级提示文案 + "手动选择科室"入口 | 对齐需求文档 3.1.3.1 / §6.3"回退到按科室选择医生模式"降级路径 |

---

## 8. 剂量标准初始化与药品编码规范

### 8.1 初始化方案

SQL 种子脚本：
- backend/modules/prescription/src/main/resources/db/seed/R__dosage_standards.sql
- backend/modules/prescription/src/main/resources/db/seed/R__drug_allergy_mapping.sql
- backend/modules/prescription/src/main/resources/db/seed/R__drug_contraindication_mapping.sql
- backend/modules/prescription/src/main/resources/db/seed/R__drug_composition_dict.sql

使用 MERGE / INSERT ... ON DUPLICATE KEY UPDATE 实现幂等执行。

同优先级多条记录检测与前一版一致。

### 8.2 药品编码规范

DosageStandard.drugCode 采用国药准字号。与药品基础信息实体的关系与前一版一致。

### 8.3 单位一致性校验

DosageUnitGroup 枚举（MASS_GROUP / VOLUME_GROUP / IU_GROUP）与前一版一致。

### 8.4 年龄/体重分级剂量支持

DosageStandard 实体分级字段与匹配优先级策略与前一版一致。

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

规则管理覆盖以下实体：TriageRule（分诊规则）、DrugContraindicationMapping（药品禁忌症映射）、DrugAllergyMapping（药物过敏映射）、DrugCompositionDict（药品成分字典）、DrugInteractionPair（药物相互作用数据）。各实体的管理接口由 admin 模块 OOD 文档独立定义，本处定义各接口的简要契约：

| 接口 | 端点 | 说明 |
|------|------|------|
| 规则 CRUD | POST/PUT/DELETE /api/admin/rules/{entityType} | entityType 区分各规则实体类型 |
| 规则集发布 | POST /api/admin/rules/{entityType}/publish | 将指定规则集的待发布版本标记为当前生效版本 |
| 规则集回滚 | POST /api/admin/rules/{entityType}/rollback/{version} | 回滚至指定历史版本 |
| 规则版本查询 | GET /api/admin/rules/{entityType}/versions | 返回指定规则集的所有版本列表 |
| 配置变更审计日志 | GET /api/admin/audit/change-log | 查询 ConfigChangeLog 记录 |

规则变更后通过 ApplicationEventPublisher 发布对应事件（如 TriageRuleChangeEvent），消费端（如 DefaultTriageRuleEngine）监听后刷新缓存。TemplateConfigChangeEvent 与规则变更事件复用同一事件发布/监听框架。

### 9.4 配置变更审计日志

ConfigChangeLog 实体与前一版一致。

**事务边界与事件发布保序**：规则/模板变更事件由各自的 admin 模块管理 Service 在事务内发布，使用 @TransactionalEventListener(phase=AFTER_COMMIT) 确保仅在事务成功提交后发布事件，避免事务回滚时事件已发送导致数据不一致。Spring ApplicationEvent 在 application 模块聚合后可跨模块传播——medical-record 模块的 TemplateConfigManager（监听 TemplateConfigChangeEvent）和 admin 模块的 ConfigChangeLog 写入监听器均可正常接收事件。**事件丢失补偿机制**：TemplateConfigManager 和 DefaultTriageRuleEngine 的 Caffeine 定时刷新（refreshAfterWrite）作为兜底——即使事件丢失，缓存最多在刷新间隔（默认 60 秒）后自动从数据库加载最新配置。

---

## 10. ai-api 层 DTO 扩展规格

ai-api 层 DTO 当前为空壳类（仅含默认构造器），需扩展完整字段定义以对齐需求文档 3.4.x 契约。扩展方式：保留现有类结构，在各 DTO 中补充字段，使其承载 AI 推理所需的完整输入/输出语义。以下列出各 DTO 需扩展的完整字段集。

### 10.1 分诊 ai-api DTO

#### TriageRequest（ai-api 层）

扩展字段：chiefComplaint（string，必填）、additionalResponses（List\<AdditionalResponseItem\>，可选，每项含 question/answer/answeredAt）、patientId（string，可选）、sessionId（string，可选）、ruleVersion（string，可选）、ruleSetId（string，可选）。

#### TriageResponse（ai-api 层）

扩展字段：recommendedDepartments（List\<RecommendedDepartment\>，每项含 departmentId/departmentName/score）、recommendedDoctors（List\<RecommendedDoctor\>，每项含 doctorId/doctorName/departmentId/availableSlotCount/score）、reason（string，必填）、matchedRules（List\<MatchedRuleItem\>，每项含 ruleId/ruleName/score）、needFollowUp（bool，可选）、followUpQuestion（string，可选）、confidence（float，可选）、degraded（bool，可选）、sessionId（string，可选）。

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
