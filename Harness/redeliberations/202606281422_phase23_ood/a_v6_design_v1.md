# Phase 2/3 包C/D-AI1/D-AI2/E — 架构级 OOD 设计方案

## 1. 概述

### 1.1 设计目标

本设计覆盖 Phase 2 包C（智能分诊）与 Phase 3 包D-AI1（处方审核）、包D-AI2（病历生成）、包E（辅助开方）四个业务包。核心目标如下：

- **底座直接落地**：四个包均直接以 Maven 模块形式落地在 AIMedical 后端底座上，遵循 common → modules → application 的分层架构，规避后续迁移至 Phase 5 AI 进阶底座的重构成本
- **架构风格一致**：四个包的模块结构、依赖方向和抽象层次与 Phase 0（骨架模块）、Phase 1（认证模块）的风格保持一致
- **强耦合同步落地**：包E（辅助开方）与包D-AI1（处方审核）共享处方领域数据和业务规则，设计为同一模块内的两个子域，同步开发、同步发布
- **AI 能力集成标准化**：四个包的业务逻辑均通过 ai-api 中的 AiService 接口调用 AI 能力，隔离 AI 实现细节；AI 不可用时的降级路径由各自模块的本地规则兜底，与 AiService 的降级框架协同
- **需求文档契约对齐**：所有模块的 API 输入/输出契约与需求文档 §3.4.x 定义的字段级契约严格对齐，确保验收可追溯

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

### 1.3 核心抽象一览

#### 包C（智能分诊）

| 抽象 | 类型形态 | 职责定位 |
|------|---------|---------|
| TriageController | class | 分诊对话 REST 端点，接收前端提交的主诉/追问答复，返回推荐科室与推荐医生列表 |
| TriageService | interface | 智能分诊业务契约，封装单轮/多轮对话的分诊逻辑、对话状态管理和规则回退 |
| DialogueSession | class | 多轮对话会话状态对象，维护当前 session 的对话上下文、已收集的症状信息和当前轮次、规则版本快照 |
| DialogueSessionManager | class | 对话会话生命周期管理器，负责 session 的创建、查找、更新和过期清理，承担并发访问控制职责；统一生成 sessionId（UUID v4） |
| TriageRuleEngine | interface | 分诊规则引擎契约，根据症状匹配规则返回推荐科室；支持可配置规则源 |
| DepartmentFallbackProvider | interface | 兜底科室列表提供者契约，当 AI 无结果且规则匹配也无结果时返回静态兜底科室列表或基于简单规则的匹配结果 |
| TriageResponse | class（DTO） | 分诊响应值对象，包含推荐科室列表（departments）、推荐医生列表（doctors，List\<RecommendedDoctor\>）、推荐理由（reason，必填）、会话标识（sessionId，多轮场景）、是否需要追问标记（needFollowUp）及追问内容（followUpQuestion）、置信度confidence（可选） |
| RecommendedDoctor | class（DTO） | 推荐医生值对象，每项含 doctorId、doctorName、departmentId、availableSlotCount、score |
| DialogueCreateRequest | class（DTO） | 分诊对话创建请求值对象，包含患者主诉（chiefComplaint）、患者基本信息（patientId、age、gender）、会话标识（sessionId，多轮场景复用时携带；首轮请求时 sessionId 为空，服务端新建会话后返回 sessionId）、规则版本号（ruleVersion，可选）、追问回答数组（additionalResponses，可选，List\<AdditionalResponse\>，每项含 question / answer / answeredAt） |

#### 包D-AI1（处方审核）

| 抽象 | 类型形态 | 职责定位 |
|------|---------|---------|
| PrescriptionAuditController | class | 处方审核 REST 端点，接收待审核处方，返回审核结果与风险等级；在收到 BLOCK 结果时拒绝处方提交，通过 HTTP 422 返回阻断详情 |
| PrescriptionAuditService | interface | 处方审核业务契约，协调 AI 审核调用与本地规则回退；审计结果为最终判定，调用方需据此执行阻断决策 |
| AuditRiskLevel | enum | 风险等级枚举，定义 PASS（通过，对应需求文档"低风险"）、WARN（警告，对应需求文档"中风险"）、BLOCK（阻断，对应需求文档"高风险"）三个级别；BLOCK 级别在后端具有强制阻断语义 |
| AuditRecord | JPA @Entity | 审核记录持久化实体，保存每次审核的原始处方、AI 结果、风险等级和时间戳，以及处方单号、医生 ID、患者 ID 等业务关联标识；含强制提交留痕字段（forceSubmitted、forceSubmitTime） |
| LocalRuleEngine | interface | 本地规则校验引擎契约，封装药物相互作用、过敏检查、剂量上限等规则；AI 超时或不可用时作为降级回退 |
| PrescriptionAuditEnforcer | interface | 阻断执行策略契约，定义 BLOCK 结果的强制阻断行为；默认实现返回阻断错误码和阻断原因集合，供 Controller 层组装响应 |
| AuditRequest | class（DTO） | 处方审核请求值对象，包含处方药品列表（prescriptionItems，每项含 drugId/drugName/dose/frequency/duration/route 六个字段）、患者个体信息（patientInfo，含 patientId/age/gender/allergyHistory/comorbidities）、处方标识（prescriptionId） |
| AuditResponse | class（DTO） | 处方审核响应值对象，包含风险等级（riskLevel，AuditRiskLevel）、审核问题列表（issues，List\<AuditIssue\>）、药物相互作用结果（interactions，List\<DrugInteraction\>）、用药建议（suggestions，List\<Suggestion\>）、是否降级标记（fromFallback） |
| DrugInteraction | class（DTO） | 药物相互作用结果值对象，每项含 drugPair（药物对标识）、severity（严重程度）、description（描述） |
| Suggestion | class（DTO） | 用药建议值对象，每项含 suggestionCode（建议编码）、suggestionText（建议文本） |
| AuditIssue | class（DTO） | 审核问题条目值对象，包含问题字段名（fieldName）、问题描述（issueDescription）、触发规则标识（ruleId）、严重程度（severity） |
| BlockResponse | class（DTO） | 阻断响应值对象，包含阻断原因列表、阻断码、阻断时间 |
| LocalRuleResult | class（DTO） | 本地规则校验结果值对象，包含规则标识（ruleId）、是否通过（passed）、消息文本（message）、严重程度（severity）；多条规则结果聚合为列表后参与风险等级判定 |

#### 包D-AI2（病历生成）

| 抽象 | 类型形态 | 职责定位 |
|------|---------|---------|
| MedicalRecordController | class | 病历生成 REST 端点，接收医生端对话文本，返回结构化病历 |
| MedicalRecordService | interface | 病历生成业务契约，协调 AI 结构化输出与科室模板配置 |
| DepartmentTemplateConfig | class | 科室模板配置值对象，按科室标识管理病历生成规则和模板版本 |
| TemplateConfigManager | interface | 科室模板配置管理器契约，支持模板的运行时查询、缓存和热加载；科室标识不存在时兜底返回 DEFAULT 模板 |
| MissingFieldDetector | interface | 关键字段缺失检测器契约，基于科室模板的必填字段列表与 AI 输出实际包含字段进行差集比对，输出缺失字段集合；不修改 AI 产出 |
| FieldMissingHint | class（DTO） | 字段缺失提示值对象，包含缺失字段标识（missingField，MedicalRecordField 枚举）、提示消息（promptMessage）、建议操作（suggestedAction） |
| RecordGenerateRequest | class（DTO） | 病历生成请求值对象，包含对话文本（dialogueText）、患者 ID（patientId）、就诊标识（encounterId，映射需求文档 encounter_id；当 encounterId 存在时后端自动获取科室信息，departmentId 不再需要显式传入） |

#### 包E（辅助开方）

| 抽象 | 类型形态 | 职责定位 |
|------|---------|---------|
| PrescriptionAssistController | class | 辅助开方 REST 端点；提供两个主端点：(1) POST /api/prescription/assist 作为 3.4.10 AI 辅助开方主端点，接收完整输入返回完整结构化输出；(2) POST /api/prescription/assist/check-dose 用于剂量阈值即时校验；以及异步 AI 建议查询端点 |
| PrescriptionAssistService | interface | 辅助开方业务契约，覆盖两个子域：(a) AI 辅助开方完整流程——接收诊断结论/检查检验结果/患者信息/已有处方，委托 AiService.prescriptionAssist() 生成处方草案；包括剂量告警和过敏告警的本地即时校验作为 AI 调用的前置/补充；(b) 剂量阈值即时校验——委托 DosageThresholdService |
| DosageThresholdService | class | 剂量阈值校验服务，按药品编码、给药途径、年龄范围、体重范围维度匹配剂量标准，检查剂量是否超限；支持日剂量校验 |
| DosageAlert | class | 剂量告警值对象，封装告警级别（alertLevel，DosageAlertLevel 枚举）、告警消息、药品编码、当前剂量、建议值 |
| DosageAlertLevel | enum | 剂量告警级别枚举，定义 INFO/WARN/CRITICAL 三个级别；CRITICAL 写入处方草稿上下文，供处方提交时 BLOCK 判定消费 |
| AiSuggestionResult | class | 异步 AI 结果值对象，封装 taskId、suggestion（String，COMPLETED 时填充）、status（AiSuggestionStatus 枚举：PENDING / COMPLETED / FAILED）、createTime、failReason（可选）；内部字段更新通过 ConcurrentHashMap.compute() 原子操作保护，同一 taskId 的状态更新保证幂等 |
| PrescriptionAssistResponse | class（DTO） | AI 辅助开方主端点响应值对象，包含处方草案（prescriptionDraft，含 drugs 数组，每项含 drugId/drugName/dose/frequency/duration/route）、剂量告警列表（doseWarnings，每项含 drugId/warningType/message/severity）、过敏冲突告警（allergyWarnings，每项含 drugId/allergen/severity）、免责声明标记（disclaimerRequired） |
| DosageCheckResponse | class（DTO） | 剂量阈值即时校验响应值对象，包含告警列表（alerts，List\<DosageAlert\>）和异步任务标识（taskId） |

---

## 2. 模块划分

### 2.1 目录结构

```
backend/modules/
├── consultation/                            # 包C 智能分诊
│   └── src/main/java/com/aimedical/modules/consultation/
│       ├── api/TriageController.java
│       ├── dto/TriageResponse.java, RecommendedDoctor.java, DialogueCreateRequest.java, AdditionalResponse.java
│       ├── service/
│       │   ├── TriageService.java
│       │   └── impl/TriageServiceImpl.java
│       ├── dialogue/
│       │   ├── DialogueSession.java         # 可变，由 DialogueSessionManager 管理；含 ruleVersion 快照
│       │   └── DialogueSessionManager.java  # 生命周期管理 + 并发控制 + sessionId 生成（UUID v4）
│       ├── rule/
│       │   ├── TriageRuleEngine.java
│       │   └── DefaultTriageRuleEngine.java # 数据库规则源 + 定时缓存刷新
│       ├── fallback/
│       │   ├── DepartmentFallbackProvider.java
│       │   └── StaticDepartmentFallbackProvider.java
│       ├── repository/TriageRecordRepository.java
│       ├── entity/TriageRecord.java        # 含 AI 推荐科室快照、规则匹配科室、最终选择科室、置信度、降级标记等
│       └── converter/TriageConverter.java
│
├── prescription/                            # 包D-AI1 处方审核 + 包E 辅助开方
│   └── src/main/java/com/aimedical/modules/prescription/
│       ├── api/
│       │   ├── audit/PrescriptionAuditController.java
│       │   └── assist/PrescriptionAssistController.java  # 含 POST /assist 主端点 + POST /assist/check-dose + GET /assist/suggestion/{taskId}
│       ├── dto/
│       │   ├── audit/AuditRequest.java, AuditResponse.java, AuditIssue.java, BlockResponse.java,
│       │   │       DrugInteraction.java, Suggestion.java
│       │   └── assist/
│       │       ├── PrescriptionAssistRequest.java  # AI 辅助开方主端点请求（3.4.10 完整输入）
│       │       ├── PrescriptionAssistResponse.java # AI 辅助开方主端点响应（3.4.10 完整输出）
│       │       ├── DosageCheckRequest.java  # 含给药途径参数和用药频率
│       │       ├── DosageCheckResponse.java # 剂量校验即时响应
│       │       ├── DosageAlert.java, DosageAlertLevel.java
│       │       ├── AiSuggestionResult.java  # 含 FAILED 状态和 failReason；并发安全保护
│       │       └── DoseWarning.java, AllergyWarning.java
│       ├── service/
│       │   ├── audit/PrescriptionAuditService.java + impl/
│       │   ├── audit/AuditRiskLevel.java
│       │   ├── audit/PrescriptionAuditEnforcer.java + impl/
│       │   └── assist/PrescriptionAssistService.java + impl/
│       │   └── assist/DosageThresholdService.java # 含单位一致性校验、年龄体重分级支持、日剂量校验
│       ├── rule/
│       │   ├── LocalRuleEngine.java
│       │   ├── DrugInteractionRule.java, AllergyCheckRule.java, DosageLimitRule.java
│       │   ├── DrugInteractionPair.java      # 药物相互作用数据实体（Phase 3 待实现）
│       │   ├── DrugAllergyMapping.java       # 药物过敏映射数据实体（Phase 3 待实现）
│       │   └── LocalRuleResult.java
│       ├── repository/
│       │   ├── AuditRecordRepository.java
│       │   └── DosageStandardRepository.java # 只读，继承 Repository 非 JpaRepository
│       ├── entity/AuditRecord.java           # 含 prescriptionOrderId, doctorId, patientId, forceSubmitted, forceSubmitTime
│       └── converter/AuditConverter.java, AssistConverter.java
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
│       └── converter/MedicalRecordConverter.java

backend/modules/admin/
└── src/main/java/com/aimedical/modules/admin/
    ├── entity/ConfigChangeLog.java       # 配置变更审计日志实体（规则/模板变更时记录）
    └── repository/ConfigChangeLogRepository.java

backend/common/src/main/java/com/aimedical/common/
└── entity/
    └── DosageStandard.java               # 跨模块共享，admin 写入，prescription 只读；含年龄范围、体重范围、日剂量上限字段
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

各模块通过注入 AiService 接口调用 AI 能力。AI 降级时各模块以本地规则兜底。

---

## 3. 核心抽象

### 3.1 包C：智能分诊

#### TriageService（interface）

职责：定义智能分诊的核心业务边界，覆盖单轮分诊、多轮追问和会话管理。

协作对象：被 TriageController 调用（前端仅发送 sessionId 和新内容）；委托 AiService.triage() 进行 AI 分诊，AI 无结果时降级至 TriageRuleEngine.match()，规则匹配无结果时继续调用 DepartmentFallbackProvider.getFallbackDepartments() 获取兜底科室列表；管理 DialogueSessionManager。降级判定语义：AiResult.success=false 或 AiResult.degraded=true 时判定 AI 不可用、触发降级；AiResult.success=true 但推荐列表为空视为有效结果（AI 明确判断无匹配科室），跳过规则引擎。

为何使用 interface：分诊业务可能存在多种实现（普通分诊 vs 急诊分诊），interface 允许扩展而不影响调用方。

#### DialogueSession（class，可变）

职责：封装一次多轮分诊的完整上下文（sessionId、主诉、症状列表、追问轮次、会话状态、规则版本快照 ruleVersion）。首轮创建时，DialogueSessionManager 记录当前规则引擎的版本号存入 DialogueSession.ruleVersion；后续追问使用该快照版本对应的规则，确保对话内分诊逻辑前后一致。

协作：被 DialogueSessionManager 创建/修改/持久化。

为何使用可变 class：每轮追问追加 QA，可变避免频繁对象复制。DialogueSessionManager 承担并发控制——同 session 请求串行，不同 session 独立。

#### DialogueSessionManager（class）

职责：管理 DialogueSession 的创建、查找、更新和过期清理。承担并发访问的唯一控制点。**sessionId 生成**：统一由 DialogueSessionManager 生成，采用 UUID v4 格式（36 字符，含连字符），首轮请求时 sessionId 为空（前端不传），服务端创建新会话后返回 sessionId，后续多轮请求必填 sessionId。

协作：被 TriageService 调用。内部维护 ConcurrentHashMap + ScheduledExecutorService（TTL 30 分钟）。findOrCreate 三种分支：不存在→创建（同时生成 sessionId 并记录当前 ruleVersion），有效→恢复，已超时→TRIAGE_SESSION_EXPIRED 错误。

为何使用 class 而非 interface：存储策略虽有变更可能，但管理器职责边界稳定，interface 抽象收益在当前阶段不抵实现复杂度。

#### TriageRuleEngine（interface）

职责：症状→科室匹配规则引擎。规则存储在数据库，支持热加载。提供 currentRuleVersion() 方法返回当前规则版本号，供 DialogueSessionManager 在创建会话时快照。

协作：被 TriageService 在 AI 不可用时调用。配合 Caffeine refreshAfterWrite（默认 60 秒）定时刷新规则缓存。

为何使用 interface：规则源可能从数据库演化为分布式规则引擎。

#### DepartmentFallbackProvider（interface）

职责：AI 和规则引擎均无法决策时返回静态兜底科室列表。

#### TriageResponse（class，DTO）

职责：封装分诊响应的结构化数据。包含推荐科室列表（departments）、**推荐医生列表（doctors，List\<RecommendedDoctor\>，对齐需求文档 3.4.1 recommended_doctors 字段）**、**推荐理由（reason，必填字段，对齐需求文档 3.4.1 reason 字段；AI 无法生成有意义的推荐理由时输出默认文案如"根据症状分析，建议就诊XX科室"，不可为空字符串或 null）**、会话标识（sessionId，多轮追问场景携带）、是否需要追问标记（needFollowUp）及追问内容（followUpQuestion）、置信度标量（confidence，可选，AI 模式时由 AiService.triage() 填充）。单轮场景 sessionId 为空。

#### RecommendedDoctor（class，DTO）

职责：封装推荐医生值对象，对齐需求文档 3.4.1 recommended_doctors 每项结构。包含 doctorId、doctorName、departmentId、availableSlotCount（可预约时段数）、score（匹配评分）。

#### DialogueCreateRequest（class，DTO）

职责：封装分诊对话请求值对象。包含患者主诉（chiefComplaint）、患者基本信息（patientId、age、gender）、会话标识（sessionId，多轮追问场景复用已有 sessionId，通过 DialogueSessionManager 恢复上下文；**首轮请求时 sessionId 为空，由服务端生成 UUID v4 并返回**——此设计与需求文档 3.4.1 session_id"必填"约束的映射关系为：需求文档的"必填"指多轮场景下必填，首轮由服务端生成后后续必填）、**规则版本号（ruleVersion，可选，对齐需求文档 3.4.1 rule_version 字段；前端可携带管理端下发的规则版本号，未携带时 DialogueSessionManager 取当前引擎版本）**、**追问回答数组（additionalResponses，可选，List\<AdditionalResponse\>，对齐需求文档 3.4.1 additional_responses 字段；每项含 question / answer / answeredAt，对应 3.1.3.1 "多轮调用"组装约定）**。

#### AdditionalResponse（class，DTO）

职责：封装单条追问回答值对象，对齐需求文档 3.4.1 additional_responses 数组每项结构。包含 question（追问问题文本）、answer（患者回答文本）、answeredAt（回答时间，ISO 日期格式）。

#### TriageRecord（JPA @Entity）

职责：分诊结果持久化实体，满足需求文档 3.4.1"分诊结果需持久化以供统计与质量分析"的可观测性约束。包含以下统计必需字段：sessionId（会话标识）、patientId（患者 ID）、chiefComplaint（主诉快照）、aiRecommendedDepartments（AI 推荐科室快照，JSON 文本）、ruleMatchedDepartments（规则匹配科室快照，JSON 文本）、finalDepartmentId（最终选择科室 ID）、finalDepartmentName（最终选择科室名称）、confidence（置信度）、degraded（降级标记，boolean——该次分诊是否经过规则引擎或兜底提供者降级）、ruleVersion（使用的规则版本号）、triageTime（分诊时间戳）。

### 3.2 包D-AI1：处方审核

#### PrescriptionAuditService（interface）

职责：处方审核核心流程：接收处方→调用 AI→AI 不可用时回退本地规则→返回风险等级。审计结果的 BLOCK 等级具有强制阻断语义，调用方（Controller 或 Orchestrator）必须根据 AuditRiskLevel 执行阻断决策，不得将 BLOCK 处方提交至下一环节。

协作：正常路径委托 AiService.prescriptionCheck()，降级路径委托 LocalRuleEngine；无论正常或降级路径，均在返回前写入 AuditRecord 持久化（降级路径的 AuditRecord 携带 fromFallback=true 标记）；返回结果中包含 riskLevel、interactions、suggestions 和 issues。

为何使用 interface：审核策略未来可能演进（如分级审核、人工审核兜底），interface 隔离变更影响。

超时配置：ai.timeout.prescription-audit=6s（对齐需求文档 3.4.2 硬超时 ≤ 6 秒）。

#### AuditRiskLevel（enum）

PASS / WARN / BLOCK，固定有限分类。术语与需求文档的映射关系：
- **PASS**（对应需求文档"低风险"，3.2.2.7 中 LOW）：仅给出建议性提示，不影响提交
- **WARN**（对应需求文档"中风险"，3.2.2.7 中 MEDIUM）：需显著告警，允许医生强制提交并留痕（前端弹窗展示警告信息，医生可选择强制提交/修改后重新审核/撤销审核三种操作）
- **BLOCK**（对应需求文档"高风险"，3.2.2.7 中 HIGH）：业务强制拒绝，后端确保 BLOCK 处方不会被提交

BLOCK 的语义从设计层面定义为"业务强制拒绝"，不是前端展示性标记。后端必须确保 BLOCK 处方不会被提交。

#### AuditRecord（JPA @Entity）

持久化审核全部元数据，包含以下业务关联标识作为必填字段：
- prescriptionOrderId：关联的处方单号，用于追溯审核对应的处方订单
- doctorId：开方医生 ID，用于按医生维度分析审核问题分布
- patientId：患者 ID，用于按患者维度查询历史审核记录
- auditTime：审核时间戳
- fromFallback：boolean 标记，为 true 表示该审核结果来自本地规则降级而非 AI 审核，用于区分统计 AI 可用率与规则命中率
- **forceSubmitted**：Boolean，标记医生是否对该 WARN 级审核结果执行了强制提交（null 表示未触发 WARN 流程）
- **forceSubmitTime**：LocalDateTime，强制提交的时间戳（仅 forceSubmitted=true 时填充）
- originalPrescription、riskLevel、aiResult、auditIssues 等审核结果数据

这些关联标识和留痕字段使 AuditRecord 具备完整的业务可追溯性，支持按医生、患者、处方单号三个维度的查询和分析，fromFallback 标记辅助 AI 可用率统计，forceSubmitted/forceSubmitTime 满足"强制提交留痕"合规要求。

#### LocalRuleEngine（interface）

封装 DrugInteractionRule、AllergyCheckRule、DosageLimitRule 等独立规则。每条规则独立实现/测试/启用/禁用。

**Phase 2/3 实现范围说明**：Phase 2/3 仅实现 DosageLimitRule（剂量范围检查，对应需求文档 3.4.2 本地规则校验项 1），数据来源为 DosageStandard 实体。DrugInteractionRule（药物相互作用）和 AllergyCheckRule（过敏检查）标注为 Phase 3 后续实现项，所需数据实体 DrugInteractionPair 和 DrugAllergyMapping 在目录中预留骨架但不参与 Phase 2/3 运行时校验。

#### LocalRuleResult（class，DTO）

职责：封装单条本地规则的校验结果。包含规则标识（ruleId）、是否通过（passed）、消息文本（message）、严重程度（severity）。多条 LocalRuleResult 聚合为列表后，由 PrescriptionAuditService 实现根据各 ruleResult 的 severity 聚合为最终的 AuditRiskLevel：若任一规则 severity 为 BLOCK 则整体判定为 BLOCK；若无 BLOCK 但存在 WARN 则整体判定为 WARN；全部 PASSED 则判定为 PASS。

#### AuditRequest（class，DTO）

职责：封装处方审核请求值对象，对齐需求文档 3.4.2 输入契约。包含处方药品列表（prescriptionItems，每项含 drugId/drugName/**dose**/oods**frequency**/oods**duration**/oods**route** 六个字段）、患者个体信息（patientInfo，含 patientId/age/gender/**allergyHistory**（string，过敏史文本）/oods**comorbidities**（List\<String\>，合并症列表））、处方标识（prescriptionId，必填）。

#### AuditResponse（class，DTO）

职责：封装处方审核响应值对象，对齐需求文档 3.4.2 输出契约。包含风险等级（riskLevel，AuditRiskLevel 枚举）、审核问题列表（issues，List\<AuditIssue\>）、**药物相互作用结果（interactions，List\<DrugInteraction\>，对齐需求文档 3.4.2 interactions 字段；每项含 drugPair / severity / description）**、**用药建议（suggestions，List\<Suggestion\>，对齐需求文档 3.4.2 suggestions 字段；每项含 suggestionCode / suggestionText）**、是否降级标记（fromFallback，boolean）。

#### DrugInteraction（class，DTO）

职责：封装药物相互作用结果值对象，对齐需求文档 3.4.2 interactions 数组每项结构。包含 drugPair（药物对标识，如"A-B"）、severity（严重程度）、description（相互作用描述）。

#### Suggestion（class，DTO）

职责：封装用药建议值对象，对齐需求文档 3.4.2 suggestions 数组每项结构。包含 suggestionCode（建议编码）、suggestionText（建议文本）。

#### AuditIssue（class，DTO）

职责：封装单条审核问题条目。包含问题字段名（fieldName，如药品名称或剂量字段）、问题描述（issueDescription，自然语言描述问题内容）、触发规则标识（ruleId，标识触发该问题的规则来源）、严重程度（severity，与 AuditRiskLevel 对齐，用于前端按严重程度分级渲染）。

#### BlockResponse（class，DTO）

职责：封装阻断响应结构。包含阻断原因列表（每个条目含字段名、问题描述、匹配规则标识）、阻断码、阻断时间。

#### PrescriptionAuditEnforcer（interface）

职责：定义 BLOCK 阻断的执行策略。当 PrescriptionAuditService 返回 AuditRiskLevel.BLOCK 时，由该组件将阻断语义转化为具体的业务阻断行为。

为何使用 interface：不同场景可能需要不同的阻断行为——某些场景需要直接拒绝提交并返回 422，某些场景可能需要记录阻断日志后转人工审核。interface 支持阻断策略切换而不影响审核核心流程。

默认阻断行为：
1. 识别并收集所有触发 BLOCK 的审核问题条目
2. 封装为 BlockResponse（含阻断原因列表、阻断码、阻断时间）
3. Controller 层将 BlockResponse 以 HTTP 422 返回前端，前端据此阻断提交流程并展示阻断原因

### 3.3 包D-AI2：病历生成

#### MedicalRecord（JPA @Entity）

职责：结构化病历持久化实体。核心字段包含：记录标识、患者 ID（patientId）、就诊标识（visitId，必填字段，关联 Registration/Visit 记录，确保病历数据可追溯至具体就诊）、就诊科室、病历内容结构化 JSON、医生 ID、创建时间。病历内容通过 MedicalRecordField 枚举标识顶层字段，每个字段对应病历中的一个语义段落。

#### MedicalRecordField（enum）

职责：定义病历结构化输出的顶层字段标识符枚举。条目包括：CHIEF_COMPLAINT（主诉）、PRESENT_ILLNESS（现病史）、PAST_HISTORY（既往史）、PHYSICAL_EXAM（体格检查）、AUXILIARY_EXAM（辅助检查）、PRELIMINARY_DIAGNOSIS（初步诊断）、TREATMENT_ADVICE（治疗意见）。此枚举是 MissingFieldDetector 差集比对和 DepartmentTemplateConfig.requiredFields 的类型基础，使字段标识符与模板内容一致。

#### RecordGenerateRequest（class，DTO）

职责：病历生成请求值对象，对齐需求文档 3.4.3 输入契约。包含对话文本（dialogueText）、患者 ID（patientId）、就诊标识（**encounterId，映射需求文档 3.4.3 encounter_id 字段；visitId 与 encounter_id 为同一概念，设计中统一命名为 encounterId 以与需求文档术语一致**）。**encounterId 与 departmentId 的关系**：当 encounterId 存在时，后端通过 encounterId 关联就诊记录自动获取科室信息，departmentId 不再需要作为显式参数；若 encounterId 缺失（边缘场景），后端从请求上下文的医生端会话中获取科室信息作为兜底。

为何删除 departmentId 显式参数：需求文档 3.4.3 未定义 departmentId 字段，只定义了 encounter_id；科室信息可由后端从 encounter 关联数据自动获取，无需前端显式传入，减少数据冗余和不一致风险。

#### RecordGenerateResponse（class，DTO）

职责：病历生成响应值对象。包含结构化病历内容（fields，以 MedicalRecordField 为键的键值映射）、缺失字段提示列表（missingFieldHints，List\<FieldMissingHint\>）、生成状态标记（如降级标记 fromFallback）。

#### FieldMissingHint（class，DTO）

职责：封装单个必填字段缺失的提示信息。包含缺失字段标识（missingField，MedicalRecordField 枚举）、提示消息（promptMessage，面向医生的友好提示文案）、建议操作（suggestedAction，指导医生如何补全）。

#### MedicalRecordService（interface）

对话文本→结构化病历。委托 AiService.generateMedicalRecord()、TemplateConfigManager（科室不存在时兜底 DEFAULT）、MissingFieldDetector。返回 RecordGenerateResponse。

超时配置：ai.timeout.medical-record-generate=12s（对齐需求文档 3.4.3 非流式硬超时 ≤ 12 秒）。

#### DepartmentTemplateConfig（class）

科室模板配置值对象（科室标识、版本号、字段映射、必填字段列表 requiredFields 类型为 List\<MedicalRecordField\>、Prompt 模板）。

#### TemplateConfigManager（interface）

getTemplate(departmentId)：优先返回指定科室模板，不存在时返回 DEFAULT 兜底。Caffeine refreshAfterWrite + 事件驱动缓存失效。

#### MissingFieldDetector（interface）

职责：检测 AI 输出是否缺少必填字段。采用差集比对逻辑：取科室模板的必填字段集合与 AI 输出实际包含字段集合的差集，输出缺失字段集合及补全提示。不修改 AI 产出的结构化内容。

为何使用 interface：字段缺失检测逻辑未来可能引入语义分析（如字段值虽存在但为空字符串或占位符）或按科室定制规则，interface 支持策略替换。

降级策略说明（分层保护）：
- 当 AI 完全不可用时，AI 返回空字段集（而非结构化病历）
- MedicalRecordService 调用 MissingFieldDetector 识别出所有必填字段均缺失
- 保留已提取的结构化字段（此处为空字段集，分层保护规则仍然生效），仅标记缺失字段
- 前端对现有字段正常渲染，缺失字段显示补全提示输入框
- 此策略确保在任意降级场景下，已提取的字段数据不丢失，前端只需处理缺失标记即可

### 3.4 包E：辅助开方

#### PrescriptionAssistService（interface）

职责：**AI 辅助开方完整流程**——接收诊断结论（diagnosis）、检查检验结果（examResults）、患者信息（patientInfo）、已有处方（existingPrescription），委托 AiService.prescriptionAssist() 生成处方草案；本地即时校验（剂量阈值 DosageThresholdService + 过敏冲突本地检查）作为 AI 调用的前置补充，产出 doseWarnings 和 allergyWarnings。**剂量阈值即时校验子域**——接收单药品剂量检查参数，委托 DosageThresholdService 做即时阈值校验，接收异步 AI 建议查询。

API 端点定位（对齐需求文档 3.4.10）：
- **POST /api/prescription/assist**：AI 辅助开方主端点，接收 3.4.10 完整输入（diagnosis / examResults / patientInfo / existingPrescription / encounterId），返回 3.4.10 完整结构化输出（prescriptionDraft / doseWarnings / allergyWarnings / disclaimerRequired）
- **POST /api/prescription/assist/check-dose**：剂量阈值即时校验子端点，接收单药品参数（drugCode / dosage / unit / routeOfAdministration / patientAge / patientWeight / frequency），返回即时剂量告警和异步 AI 建议 taskId
- **GET /api/prescription/assist/suggestion/{taskId}**：异步 AI 建议查询端点，遵循四分支模式

为何将 /assist 定义为主端点：需求文档 3.4.10 定义的 AI 辅助开方能力是完整的"诊断→处方草案"流程，check-dose 仅是其中一个即时校验子功能。主端点承载完整的输入输出契约，前端在"AI 辅助开方"入口调用 /assist；医生逐药品编辑剂量时调用 /assist/check-dose 做即时反馈。两端点协同覆盖了 3.4.10 的全部能力范围。

#### PrescriptionAssistRequest（class，DTO）

职责：AI 辅助开方主端点请求值对象，对齐需求文档 3.4.10 输入契约。包含诊断结论（diagnosis，string，必填）、检查检验结果摘要（examResults，List\<ExamResultItem\>，可选，每项含 itemName / value / unit / referenceRange / status）、患者基础信息（patientInfo，含 patientId / age / gender / allergyHistory / comorbidities）、已有处方（existingPrescription，可选，含 drugs 数组）、就诊标识（encounterId，可选）。

#### PrescriptionAssistResponse（class，DTO）

职责：AI 辅助开方主端点响应值对象，对齐需求文档 3.4.10 输出契约。包含处方草案（prescriptionDraft，含 drugs 数组，每项含 drugId / drugName / dose / frequency / duration / route）、剂量告警列表（doseWarnings，List\<DoseWarning\>，每项含 drugId / warningType / message / severity，warningType 枚举含 OVER_SINGLE_DOSE / OVER_DAILY_DOSE / OVER_DURATION）、过敏冲突告警（allergyWarnings，List\<AllergyWarning\>，每项含 drugId / allergen / severity）、免责声明标记（disclaimerRequired，bool，固定 true）。

#### DoseWarning（class，DTO）

职责：封装剂量告警值对象，对齐需求文档 3.4.10 dose_warnings 数组每项结构。包含 drugId、warningType（枚举：OVER_SINGLE_DOSE / OVER_DAILY_DOSE / OVER_DURATION）、message、severity。

#### AllergyWarning（class，DTO）

职责：封装过敏冲突告警值对象，对齐需求文档 3.4.10 allergy_warnings 数组每项结构。包含 drugId、allergen（过敏原名称）、severity。

#### DosageThresholdService（class）

按 drugCode + routeOfAdministration + ageRange + weightRange 查询 DosageStandard，比较剂量阈值。匹配优先级策略：精确匹配（所有维度均匹配）→ 年龄范围匹配（年龄范围匹配 + 体重不限）→ 体重范围匹配（体重范围匹配 + 年龄不限）→ 无分级默认阈值 → **标准不存在**（四级均未命中，降级返回 WARN 级 DosageAlert 并携带 RX_ASSIST_DOSE_STANDARD_NOT_FOUND 错误码）。内含单位一致性校验（可转换则自动转换，不可转换则返回错误码）。日剂量校验分支：当 DosageCheckRequest 携带 frequency 且 DosageStandard 存在 dailyMax 时，校验单次剂量 × 频率是否超出日剂量上限。

为何使用 class 而非 interface：剂量阈值的查找与比较逻辑稳定，无多实现需求；部门内部方法较多，class 封装可隐藏内部细节。

#### DosageAlert（class）

告警信息值对象，封装告警级别（alertLevel，DosageAlertLevel 枚举）、告警消息、药品编码、当前剂量、建议值。alertLevel 决定前端渲染策略和行为约束：
- INFO：信息提示样式（如蓝色提示），不影响流程
- WARN：警告样式（如黄色告警），需医生确认后方可继续
- CRITICAL：危险样式（如红色阻断），写入处方草稿上下文，供处方提交审核时 BLOCK 判定消费

#### DosageAlertLevel（enum）

定义剂量告警的三个级别，与前端渲染策略和业务流程约束直接关联：

- **INFO**：信息提示级别，表示剂量在正常范围内但存在建议性信息（如"建议剂量为 X"），不影响医生开方流程
- **WARN**：警告级别，表示剂量接近上限或存在需注意的事项，前端以显著告警样式展示，医生需二次确认后方可继续提交
- **CRITICAL**：危险级别，表示剂量严重超标或存在明确禁忌，前端强制阻断当前剂量提交；**CRITICAL 级别不直接调用 PrescriptionAuditService**，而是写入处方草稿上下文（PrescriptionDraftContext），处方提交时审核服务从草稿上下文读取 CRITICAL 标记作为 BLOCK 判定的输入之一，确保剂量告警与处方审核在提交环节统一处理

为何使用 enum：告警级别为固定有限分类，且前端渲染策略与后端阻断逻辑均依赖此枚举值，enum 提供编译期安全和可穷举的分支覆盖。

#### AiSuggestionResult（class）

异步 AI 结果值对象，封装 taskId、suggestion（String，COMPLETED 时填充）、status（AiSuggestionStatus 枚举：PENDING / COMPLETED / FAILED）、createTime、failReason（String，可选，FAILED 时填充失败原因描述）。

**并发安全策略**：内部字段更新通过 ConcurrentHashMap.compute() 原子操作保护——对同一 taskId 的状态转换（PENDING→COMPLETED / PENDING→FAILED）在 compute 闭包内完成，保证 value 对象状态更新的原子性。同一 taskId 的状态更新操作保证幂等：COMPLETED→COMPLETED 和 FAILED→FAILED 为合法的幂等操作，COMPLETED→FAILED 和 FAILED→COMPLETED 为非法转换（compute 内校验状态前置条件，非法转换静默忽略）。

查询时遵循四分支模式：
- 不存在（TTL 过期或被清理）：返回 RX_ASSIST_AI_SUGGESTION_NOT_FOUND 错误码，前端展示降级文案"AI 建议暂不可用"
- 存在且状态为 PENDING：返回 status=PENDING，前端显示"AI 建议生成中"
- 存在且状态为 COMPLETED：返回完整建议内容
- 存在且状态为 FAILED：返回 status=FAILED + failReason，前端展示"AI 建议生成失败：{failReason}"

**预创建→更新模式**：check-dose 响应返回 taskId 后，服务端即以 PENDING 状态预创建 AiSuggestionResult 条目并存入 ConcurrentHashMap（通过 computeIfAbsent 原子操作）。异步 AI 调用完成后通过 compute() 原子更新为 COMPLETED 或 FAILED。CompletableFuture 的 exceptionally 处理器捕获异常时，将预创建条目更新为 FAILED 状态（填充 failReason），而非保留 PENDED 僵尸条目。

AiSuggestionResult 增加 TTL（同 suggestion TTL 30 分钟），由 ScheduledExecutorService 定期清理过期条目。

#### DosageCheckResponse（class，DTO）

职责：封装剂量阈值即时校验同步响应值对象。包含告警列表（alerts，List\<DosageAlert\>，由 DosageThresholdService 产出的剂量告警）和异步任务标识（taskId，用于前端后续查询 AI 建议）。

#### PrescriptionDraftContext（class）

职责：处方草稿上下文，在医生编辑处方期间暂存 CRITICAL 级别剂量告警标记。当医生在 check-dose 流程中收到 CRITICAL 告警时，该告警标记被写入 PrescriptionDraftContext（按处方会话标识或 encounterId 关联）；处方提交时 PrescriptionAuditService 从 PrescriptionDraftContext 读取 CRITICAL 标记作为 BLOCK 判定的输入。

协作：由 DosageThresholdService 写入（CRITICAL 告警时），由 PrescriptionAuditService 读取（处方提交审核时）。生命周期与处方编辑会话一致，处方提交或取消后清理。

为何使用 class：草稿上下文为临时性内存数据结构，仅用于解耦 check-dose 即时反馈与处方提交终审两个时点，无需持久化。

---

## 4. 关键行为契约

### 4.1 智能分诊场景

```
POST /api/triage/consult
单轮: { chiefComplaint: "胸痛伴气短", patientId: "P001", age: 45, gender: "男" }
多轮: { sessionId: "xxx", chiefComplaint: "胸痛", additionalResponses: [{question: "...", answer: "...", answeredAt: "..."}] }

降级判定:
  AiResult.success=false 或 AiResult.degraded=true → 判定 AI 不可用，触发降级
  AiResult.success=true 且推荐列表为空 → AI 有效结果（跳过规则引擎）
  AiResult.success=true 且推荐列表非空 → 正常返回

正常: AiService.triage(session context) → 推荐科室 + 推荐医生 + 推荐理由
降级链: AiService 不可用 → TriageRuleEngine.match() → 匹配成功返回科室
        → 规则匹配无结果 → DepartmentFallbackProvider.getFallbackDepartments() → 兜底科室
Session 超时: TRIAGE_SESSION_EXPIRED 错误

持久化: 分诊完成后写入 TriageRecord（含 AI 推荐科室快照、规则匹配科室、最终选择科室、置信度、降级标记等）
规则版本: DialogueSessionManager 创建会话时记录 TriageRuleEngine.currentRuleVersion()，后续追问使用快照版本
```

### 4.2 处方审核场景

```
POST /api/prescription/audit
超时配置: ai.timeout.prescription-audit=6s

正常: AiService.prescriptionCheck() → AuditRecord 持久化（fromFallback=false） → 返回 AuditResponse
      AuditResponse 包含: riskLevel + issues + interactions + suggestions + fromFallback=false
降级: LocalRuleEngine.check()（Phase 2/3 仅 DosageLimitRule 启用；DrugInteractionRule / AllergyCheckRule 待实现）
      → 各条规则产出 LocalRuleResult（ruleId, passed, message, severity）
      → 聚合多条 LocalRuleResult 为最终 AuditRiskLevel
      → AuditRecord 持久化（fromFallback=true） → 返回 AuditResponse（riskLevel + issues + interactions（降级时为空列表） + suggestions（降级时为空列表） + fromFallback=true）

审核结果处理:
  PASS（低风险） → 响应建议性审核意见，不影响提交
  WARN（中风险） → 响应警告信息，前端弹窗提示医生确认
                    医生三种可选操作：
                    1. 强制提交并留痕 → AuditRecord.forceSubmitted=true, forceSubmitTime=now
                    2. 修改后重新审核
                    3. 撤销审核
  BLOCK（高风险） → PrescriptionAuditEnforcer 执行阻断 → 返回 HTTP 422 + BlockResponse
                    前端弹窗展示阻断原因列表，禁止提交；后端不执行处方落单

处方提交审核时：
  PrescriptionAuditService 从 PrescriptionDraftContext 读取 CRITICAL 剂量告警标记
  → CRITICAL 标记存在时，在审核结果中追加一条 BLOCK 级别的 AuditIssue（来源为剂量告警）
  → CRITICAL 标记与 AI/本地规则审核结果聚合为最终 AuditRiskLevel
```

### 4.3 病历生成场景

```
POST /api/medical-record/generate
{ dialogueText, patientId, encounterId }
超时配置: ai.timeout.medical-record-generate=12s

正常: TemplateConfigManager.getTemplate → AiService.generateMedicalRecord → MissingFieldDetector
兜底: 科室不存在时使用 DEFAULT 模板
降级（分层保护）:
  AI 完全不可用 → 返回空字段集
  差集比对 → 产出 FieldMissingHint 列表（含 missingField + promptMessage + suggestedAction）
  前端: 已提取字段正常渲染 + 缺失字段显示补全提示输入框

encounterId 映射说明:
  RecordGenerateRequest.encounterId 映射需求文档 3.4.3 encounter_id
  当 encounterId 存在时，后端通过它关联就诊记录自动获取科室信息
  visitId 与 encounter_id 为同一概念，设计中统一命名为 encounterId
```

### 4.4 辅助开方场景

```
[主端点] POST /api/prescription/assist
请求: { diagnosis, examResults, patientInfo, existingPrescription, encounterId }
→ 本地即时校验: 过敏冲突检查（patientInfo.allergyHistory + prescriptionDraft.drugs 交叉比对）
→ DosageThresholdService 对处方草案中每项药品做剂量阈值校验
→ 返回 PrescriptionAssistResponse: { prescriptionDraft, doseWarnings, allergyWarnings, disclaimerRequired }

[即时校验子端点] POST /api/prescription/assist/check-dose
请求: { drugCode, dosage, unit, routeOfAdministration, patientAge, patientWeight, frequency }
→ 单位一致性校验
→ DosageThresholdService.check(drugCode, route, dosage, age, weight, frequency)
   匹配优先级: 精确(drug+route+age+weight) → 年龄范围 → 体重范围 → 默认 → 标准不存在(WARN + RX_ASSIST_DOSE_STANDARD_NOT_FOUND)
   日剂量校验: 若 frequency 存在且 DosageStandard.dailyMax 存在 → 校验 dosage × frequency ≤ dailyMax
→ CRITICAL 告警写入 PrescriptionDraftContext
→ 返回 DosageCheckResponse: { alerts: [...DosageAlert], taskId }
→ 同时以 PENDING 状态预创建 AiSuggestionResult（通过 computeIfAbsent 原子操作）
→ 异步 AiService.prescriptionAssist() → 成功则通过 compute() 原子更新为 COMPLETED，异常则更新为 FAILED

[异步建议查询] GET /api/prescription/assist/suggestion/{taskId}
→ 不存在 / TTL 过期: RX_ASSIST_AI_SUGGESTION_NOT_FOUND → 前端降级文案
→ PENDING: { status: "PENDING" } → 前端"AI 建议生成中"
→ COMPLETED: { status: "COMPLETED", suggestion: "..." }
→ FAILED: { status: "FAILED", failReason: "..." } → 前端"AI 建议生成失败：{failReason}"
```

日剂量校验分层策略说明：check-dose API 层面的日剂量校验属于即时性快速校验（基于本地 DosageStandard 数据），覆盖"单次剂量 × 频率 > 日剂量上限"的显式越界场景。处方审核阶段的剂量上限检查（3.4.2 本地规则校验项 1）覆盖更全面——包括单次剂量上限、日剂量上限、儿童/老年人群特殊剂量调整等，属于提交时终审校验。两层校验互为补充：即时校验提供实时反馈，终审校验确保最终安全。

**CRITICAL 与 BLOCK 的联动机制**：DosageAlertLevel.CRITICAL 不在 check-dose 时直接调用 PrescriptionAuditService，而是将 CRITICAL 标记写入 PrescriptionDraftContext。处方提交时 PrescriptionAuditService 从 PrescriptionDraftContext 读取 CRITICAL 标记作为 BLOCK 判定的输入，与 AI/本地规则审核结果聚合为最终 AuditRiskLevel。此设计消弭了"剂量检查时点"与"处方提交时点"的矛盾——CRITICAL 告警在编辑阶段即时反馈（前端阻断当前剂量），在提交阶段统一终审（后端 BLOCK 阻断）。

---

## 5. 错误处理策略

### 5.1 模块级错误码

| 错误类别 | 前缀 | 代表场景 |
|---------|------|---------|
| 分诊 | TRIAGE_ | TRIAGE_SESSION_EXPIRED、TRIAGE_AI_TIMEOUT、TRIAGE_AI_UNAVAILABLE、参数缺失 |
| 审核 | RX_AUDIT_ | 处方格式不合法、药品编码不存在、BLOCK 阻断；RX_AUDIT_AI_TIMEOUT、RX_AUDIT_AI_UNAVAILABLE |
| 病历 | MR_ | 对话文本过短、科室模板不存在；MR_GEN_AI_TIMEOUT、MR_GEN_AI_UNAVAILABLE |
| 开方辅助 | RX_ASSIST_ | 剂量标准不存在（RX_ASSIST_DOSE_STANDARD_NOT_FOUND）、单位不匹配、AI 建议不存在、AI 建议生成失败；RX_ASSIST_AI_TIMEOUT、RX_ASSIST_AI_UNAVAILABLE、RX_ASSIST_AI_NO_RECOMMENDATION |

### 5.2 AI 降级作为正常业务流程

各模块的本地规则回退视为正常业务流程。处方审核降级时，AuditRecord 的 fromFallback=true 标记用于区分 AI 审核与本地规则结果，响应体 AuditResponse 的 fromFallback 标记同步告知前端。病历生成降级时 RecordGenerateResponse 同样携带 fromFallback 标记。

### 5.3 BLOCK 阻断作为独立错误类别

BLOCK 阻断响应通过 BlockResponse 封装，使用 HTTP 422 状态码，与业务异常体系正交。BlockResponse 包含阻断原因列表（每个 AuditIssue 条目含字段名、问题描述、匹配规则标识），前端据此精确展示阻断原因。

### 5.4 与已有异常处理框架的集成

复用 GlobalExceptionHandler + ErrorCode 接口 + BusinessException 体系。BLOCK 阻断不经过异常框架（属于正常业务流程中的阻断分支），直接通过 Controller 返回 422 响应。

### 5.5 AI 超时配置

| AI 能力 | 配置键 | 阈值 | 来源 |
|--------|-------|------|------|
| 处方审核 | ai.timeout.prescription-audit | 6s | 需求文档 3.4.2 硬超时 ≤ 6 秒 |
| 病历生成 | ai.timeout.medical-record-generate | 12s | 需求文档 3.4.3 非流式硬超时 ≤ 12 秒 |
| 辅助开方 | ai.timeout.prescription-assist | 10s | 需求文档 3.4.10 硬超时 ≤ 10 秒 |
| 智能分诊 | ai.timeout.triage | 8s | 需求文档 3.4.1 硬超时 ≤ 8 秒 |

---

## 6. 并发设计

### 6.1 对话会话并发管理

ConcurrentHashMap 存储，同 session 请求串行（前端等待响应），不同 session 独立。ScheduledExecutorService 每 5 分钟扫描清理超时 30 分钟的 session。

### 6.2 AI 调用并发

统一 CompletableFuture<AiResult<T>>，Service 层同步等待 AI 结果。

### 6.3 包E 的异步 AI 建议

@Async / CompletableFuture.runAsync() 调用 AiService.prescriptionAssist()。主响应返回 taskId 时，服务端通过 ConcurrentHashMap.computeIfAbsent() 以 PENDING 状态原子预创建 AiSuggestionResult 条目。异步调用完成后通过 ConcurrentHashMap.compute() 原子更新状态：
- 成功：在 compute 闭包内校验前置状态为 PENDING，更新为 COMPLETED，填充 suggestion
- 异常：CompletableFuture.exceptionally() 处理器在 compute 闭包内校验前置状态为 PENDING，更新为 FAILED，填充 failReason

并发安全保证：
- ConcurrentHashMap.compute() / computeIfAbsent() 在闭包内对 value 对象的读写是原子操作，消除了预创建→更新模式下多线程同时读写 AiSuggestionResult 字段的数据竞争
- 同一 taskId 的状态更新保证幂等：PENDING→COMPLETED 和 PENDING→FAILED 为合法转换；非法转换（如 COMPLETED→FAILED）在 compute 内校验前置条件后静默忽略

AI 建议暂存 ConcurrentHashMap<String, AiSuggestionResult>，TTL 30 分钟。前端通过 GET /api/prescription/assist/suggestion/{taskId} 查询，遵循四分支模式：不存在（含 TTL 过期）→ RX_ASSIST_AI_SUGGESTION_NOT_FOUND、PENDING、COMPLETED、FAILED。

### 6.4 处方草稿上下文并发管理

PrescriptionDraftContext 按 encounterId 或处方编辑会话标识关联，内部使用 ConcurrentHashMap<String, List<DosageAlert>> 存储。同一处方编辑会话的 CRITICAL 告警写入操作与处方提交时的读取操作通过 ConcurrentHashMap 的线程安全特性保证一致性。处方提交或取消后清理对应条目。

---

## 7. 设计决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 模块粒度 | 3 个模块 | 包C、包D-AI2 独立；包D-AI1 与包E 共享数据合并 |
| 模块结构 | 扁平模块 | 与已有模块一致 |
| 多轮对话存储 | 内存（ConcurrentHashMap + TTL 30 分钟） | 对话短时完成，Phase 5 迁移数据库 |
| 分诊规则源 | 数据库 + Caffeine 定时缓存刷新 | 支持非开发人员动态调整，热加载无需重启 |
| 本地规则形态 | 多条独立规则（LocalRuleEngine 链） | 独立实现/测试，开闭原则 |
| 科室模板配置 | 数据库 + TemplateConfigManager | 差异化配置，管理界面调整 |
| 包E AI 调用时机 | 剂量同步 + AI 异步 + 查询端点 | 即时性分级，异步不阻塞 |
| MissingFieldDetector | 差集比对检测模式 | 避免自动补全引入错误，保留已提取字段完整性 |
| 病历降级策略 | 分层保护（保留已提取字段，仅标记缺失） | 降级场景下已提取字段不丢失，前端按字段粒度假定正常渲染 |
| DosageStandard 位置 | common 模块 | 避免跨模块编译期依赖 |
| 科室模板兜底 | DEFAULT 兜底 | 新科室未配置时仍可生成基础病历 |
| 模板缓存刷新 | 定时刷新 + 事件驱动失效 | 最终一致性，管理更新可立即生效 |
| BLOCK 阻断执行 | PrescriptionAuditEnforcer 策略接口 | 阻断行为可定制（拒绝提交、转人工、记录日志等），不侵入审核核心流程 |
| BLOCK 响应形式 | HTTP 422 + BlockResponse | 与业务异常体系正交，阻断原因是结构化数据，前端精确展示 |
| AuditRecord 关联标识 | prescriptionOrderId + doctorId + patientId 必填 | 支持按处方、医生、患者三个维度的审核追溯 |
| AiSuggestionResult 查询模式 | 预创建→更新 + 四分支 | 与 DialogueSessionManager 模式一致，增加 FAILED 状态覆盖异步异常场景，消除 PENDING 僵尸条目 |
| DosageStandard 年龄/体重分级 | 内联年龄范围+体重范围字段，五级匹配优先级 | 避免子实体增加查询复杂度；匹配优先级明确可测试；第 5 级"标准不存在"提供确定性降级行为 |
| 剂量单位分组 | DosageUnitGroup 枚举（MASS/VOLUME/IU），组内换算，跨组报错 | 换算规则集中定义，消除实现随意假设的风险 |
| AuditRecord 降级标记 | fromFallback boolean 字段 | 区分 AI 审核与本地规则结果，支持 AI 可用率统计 |
| 分诊降级链 | 线性三级：AI → 规则引擎 → 兜底提供者 | 覆盖 AI 空且规则空的全路径，行为确定无二义 |
| 配置变更审计 | ConfigChangeLog 实体（admin 模块）+ 事件处理 | 规则/模板变更可追溯，满足合规审计要求 |
| 病历字段定义 | MedicalRecordField 枚举定义顶层字段标识 | 字段标识与模板内容一致，缺失检测有确定枚举基础 |
| DosageAlert 告警级别 | DosageAlertLevel 枚举（INFO/WARN/CRITICAL） | 告警级别决定前端渲染策略和后端阻断行为，CRITICAL 通过草稿上下文联动 BLOCK 审核确保双重阻断 |
| 日剂量校验分层 | check-dose 即时校验 + 处方审核终审校验 | 即时校验提供实时反馈，终审校验确保最终安全，两层互为补充 |
| AiSuggestionResult 预创建模式 | check-dose 响应前以 PENDING 预创建 | 消除异步异常导致的 PENDING 僵尸条目，FAILED 状态提供明确的失败反馈 |
| MedicalRecord 就诊关联 | visitId(encounterId) 必填字段 | 病历可追溯至具体就诊记录，消除数据追溯缺口 |
| AuditRiskLevel 术语映射 | PASS/WARN/BLOCK 对应需求文档 LOW/MEDIUM/HIGH | 确保设计术语与需求文档验收术语对齐，消除验证歧义 |
| WARN 级医生操作 | 允许强制提交并留痕 | 对齐需求 3.2.2.7 中 MEDIUM 风险下医生三种可选操作 |
| 包E 主端点定位 | POST /api/prescription/assist 承载 3.4.10 完整输入输出 | check-dose 仅覆盖"即时剂量阈值校验"子集，主端点覆盖完整的 AI 辅助开方能力（诊断→处方草案+告警），两端点协同消除与需求文档 3.4.10 契约脱节 |
| TriageResponse 契约对齐 | 增加 doctors + reason 必填字段 | 对齐需求文档 3.4.1 recommended_doctors + reason 输出契约 |
| DialogueCreateRequest 契约对齐 | 增加 ruleVersion + additionalResponses | 对齐需求文档 3.4.1 rule_version + additional_responses 输入契约 |
| AuditRequest 契约对齐 | prescriptionItems 每项含 dose/frequency/duration/route；patientInfo 含 allergyHistory + comorbidities | 对齐需求文档 3.4.2 输入契约完整字段 |
| AuditResponse 契约对齐 | 增加 interactions + suggestions | 对齐需求文档 3.4.2 输出契约 interactions + suggestions 字段 |
| RecordGenerateRequest 契约对齐 | encounterId 映射 encounter_id，删除 departmentId 显式参数 | 科室信息由后端从 encounter 关联数据自动获取，消除数据冗余 |
| session_id 必填/可选语义 | 首轮 sessionId 为空由服务端生成，后续多轮必填 | 需求文档"必填"指多轮场景下必填；首轮由服务端生成 UUID v4 后返回 |
| sessionId 生成策略 | DialogueSessionManager 统一生成，UUID v4 格式 | 后端生成消除前端构造不一致风险，UUID v4 全局唯一无需协调 |
| AiSuggestionResult 并发安全 | ConcurrentHashMap.compute() 原子更新 value 对象 | 消除预创建→更新模式下多线程同时读写 AiSuggestionResult 字段的数据竞争 |
| CRITICAL→BLOCK 联动机制 | CRITICAL 写入 PrescriptionDraftContext，提交时读取作为 BLOCK 输入 | 解耦 check-dose 即时反馈与处方提交终审两个时点，消除触发时机矛盾 |
| TriageRecord 持久化 | 新增实体满足 3.4.1 可观测性要求 | 支持分诊命中率统计与质量分析 |
| AuditRecord 留痕 | 增加 forceSubmitted + forceSubmitTime | 承载 WARN 级"强制提交并留痕"需求 |
| AI 超时配置 | 4 项 AI 能力超时阈值映射为可配置项 | 对齐需求文档 3.4.x 超时阈值要求，配置化便于环境差异化调参 |
| 分诊降级判定语义 | AiResult.success=false 或 degraded=true 触发降级；success=true + 空列表为有效结果跳过规则引擎 | 消除 AI 无结果与规则无结果的判定边界歧义 |
| 对话内规则一致性 | DialogueSession 快照 ruleVersion，后续追问使用快照版本 | 规则刷新不影响进行中的对话分诊逻辑 |
| DosageThresholdService 同优先级多条 | 取 dosageMax 最小者 + 种子数据初始化时检测重复报错 | 最保守阈值策略兼顾安全性，初始化校验消除误报根因 |
| 本地规则 Phase 2/3 实现范围 | 仅 DosageLimitRule 启用，DrugInteractionRule/AllergyCheckRule 骨架预留 | DosageLimitRule 有现有 DosageStandard 数据支撑可实际运行；药物相互作用和过敏检查需 DrugInteractionPair/DrugAllergyMapping 数据支撑，标注为待实现 |

---

## 8. 剂量标准初始化与药品编码规范

### 8.1 初始化方案

SQL 种子脚本：backend/modules/prescription/src/main/resources/db/seed/R__dosage_standards.sql
使用 MERGE / INSERT ... ON DUPLICATE KEY UPDATE 实现幂等执行。

同优先级多条记录检测：种子脚本在初始化时检测同优先级（同一 drugCode + routeOfAdministration + age 范围 + weight 范围组合）是否存在重复记录，若存在则报错中断初始化（SQL 脚本通过 UNIQUE 约束 + 预校验查询实现），避免异常小的 dosageMax 导致正常剂量被误判为严重超标。

### 8.2 药品编码规范

DosageStandard.drugCode 采用国药准字号（如 H109601234）。

### 8.3 单位一致性校验

DosageThresholdService 校验前比对单位。定义 DosageUnitGroup 枚举对兼容单位进行分组：

- MASS_GROUP：mcg ↔ mg ↔ g，按标准换算系数自动转换（如 1000mcg=1mg，1000mg=1g）
- VOLUME_GROUP：ml ↔ L，按标准换算系数自动转换（1000ml=1L）
- IU_GROUP：IU（国际单位），同组内仅同单位兼容，不做数值换算
- 其他特殊单位可单独分组或定义为不可换算

校验规则：同组内自动换算后比较剂量阈值；跨组单位（如 mg 与 ml）返回 RX_ASSIST_UNIT_MISMATCH。换算系数以 DosageThresholdService 常量形式固化，后续可根据业务需要迁移至数据库配置表。

### 8.4 年龄/体重分级剂量支持

DosageStandard 实体新增以下可选分级字段（可为空，为空表示不限制）：
- ageMin / ageMax：适用年龄范围（单位：岁），允许一端开口（如 ageMin=12 且 ageMax=null 表示 12 岁及以上）
- weightMin / weightMax：适用体重范围（单位：kg），允许一端开口
- dailyMax：日剂量上限（可选，为空表示不限制日剂量；与 frequency 配合用于日剂量校验）

剂量阈值查找匹配优先级（DosageThresholdService 实现）：
1. 精确匹配：drugCode + routeOfAdministration + age范围（患者年龄在范围内）+ weight范围（患者体重在范围内）
2. 年龄范围匹配：drugCode + routeOfAdministration + age范围 + weight为空（不限体重）
3. 体重范围匹配：drugCode + routeOfAdministration + age为空（不限年龄）+ weight范围
4. 默认值：drugCode + routeOfAdministration + age为空 + weight为空（无分级限制的通用剂量）
5. 标准不存在：四级均未命中时返回 DosageAlert（alertLevel=WARN，消息携带 RX_ASSIST_DOSE_STANDARD_NOT_FOUND 错误码），前端展示"该药品剂量标准未配置，请核实剂量"的降级提示

同一优先级返回多条时（如多个年龄范围段均命中），取 dosageMax 最小的条目作为最保守阈值。种子数据初始化时通过 UNIQUE 约束检测同优先级重复记录并报错，在数据层面消除误报根因。

---

## 9. 科室模板初始数据集与模板管理接口

### 9.1 初始模板数据集

- DEFAULT：通用模板（主诉、现病史、既往史、体格检查、辅助检查、初步诊断、治疗意见）
- 最少一个示例科室模板（如 INTERNAL_MEDICINE）

### 9.2 模板管理接口定义

CRUD 由 admin 模块管理界面完成。模板更新后通过 ApplicationEventPublisher 发布 TemplateConfigChangeEvent，TemplateConfigManager 监听使缓存失效。TemplateConfigChangeEvent 携带变更前后快照（oldTemplate 与 newTemplate），以供 ConfigChangeLog 记录新旧值语义。

### 9.3 配置变更审计日志

为规则变更和模板更新提供审计溯源能力：

- **ConfigChangeLog 实体**：归属 admin 模块（admin/entity/ConfigChangeLog.java），包含实体类型（configType，如 TEMPLATE/RULE）、实体标识（entityId）、变更前内容快照（oldValue，JSON 文本）、变更后内容快照（newValue，JSON 文本）、操作人（operatorId）、操作时间（operateTime）
- **写入触发点**：(a) TemplateConfigChangeEvent 处理链中由 admin 模块监听器写入 ConfigChangeLog；(b) 规则变更事件处理链中由 admin 模块监听器写入 ConfigChangeLog
- **配置数据的可追溯性**：ConfigChangeLog 记录完整的变更前后快照，支持按实体类型和时间范围查询变更历史
- **实现阶段**：Phase 2 先以日志文件方式记录，后续迁移至数据库审计表由 ConfigChangeLogRepository 持久化

TemplateConfigChangeEvent 由 admin 模块的模板管理 Service 发布（携带 oldTemplate 与 newTemplate），TemplateConfigManager 监听后使缓存失效、admin 模块监听器写入变更日志。两个监听器职责独立，互不依赖。medical-record 模块的 TemplateConfigManager 通过 Spring ApplicationEvent 机制接收事件，无需直接依赖 admin 模块。

规则变更审计原则同上：规则引擎的规则变更事件由规则管理 Service 发布，admin 模块监听器写入 ConfigChangeLog。

---

## 修订说明（v3）

| 审查意见 | 修改措施 |
|---------|---------|
| 1. BLOCK 风险等级的强制阻断执行机制未定义 | 新增 PrescriptionAuditEnforcer 接口（§3.2），定义 BLOCK 阻断执行策略；Controller 层在收到 BLOCK 时返回 HTTP 422 + BlockResponse（§4.2, §5.3）；阻断响应含结构化阻断原因列表，前端精确展示并禁止提交 |
| 2. AuditRecord 缺少业务关联标识 | AuditRecord 增加 prescriptionOrderId、doctorId、patientId 作为必填字段（§3.2），支持按处方/医生/患者三维度追溯 |
| 3. AiSuggestionResult 内存存储未覆盖服务重启场景 | AiSuggestionResult 查询对标 DialogueSessionManager 的 findOrCreate 三分支模式（§3.4）：不存在/TTL过期→RX_ASSIST_AI_SUGGESTION_NOT_FOUND + 前端降级文案，PENDING→轮询等待，COMPLETED→返回建议；新增 TTL 30 分钟 + 定时清理 |
| 4. DosageStandard 实体结构未定义年龄/体重分级剂量支持 | DosageStandard 新增 ageMin/ageMax/weightMin/weightMax 可空字段（§8.4），DosageThresholdService 按四级优先级查找（精确→年龄范围→体重范围→通用默认），同级多条取 dosageMax 最小者 |
| 5. 病历生成降级策略不合理（原方案：返回空病历框架+全字段缺失） | 改为分层保护策略（§3.3, §4.3）：AI 完全不可用时返回空字段集，MissingFieldDetector 做差集比对，仅标记缺失字段；保留已提取字段的完整性，前端按字段粒度假定正常渲染，缺失字段显示补全提示输入框 |

## 修订说明（v4）

| 审查意见 | 修改措施 |
|---------|---------|
| 1. §4.2 降级流程中 AuditRecord 落库被遗漏，与 §3.2 行为契约矛盾 | §4.2 降级路径显式补充 AuditRecord 持久化步骤（含 fromFallback=true 标记）；§3.2 PrescriptionAuditService 协作描述同步更新；§3.2 AuditRecord 增加 fromFallback 字段描述；§5.2 补充 fromFallback 语义说明 |
| 2. §4.1 分诊降级链路不完整——AI 空且规则空时行为未定义 | §4.1 将三条分支改为线性降级链（AI → 规则引擎 → 兜底提供者）；§3.1 TriageService 协作描述同步明确三步链式调用；§1.3 DepartmentFallbackProvider 职责更新为"AI 无结果且规则匹配也无结果时"调用 |
| 3. §2.1 目录 AgeBandedDosage 残留条目 | 从 §2.1 目录结构中移除 AgeBandedDosage.java 条目 |
| 4. TriageResponse DTO 字段结构未定义 | §1.3 核心抽象一览补充 TriageResponse 和 DialogueCreateRequest 定义；§3.1 新增两个 DTO 的独立职责描述，包含 departments/sessionId/needFollowUp/followUpQuestion/confidence 字段 |
| 5. §8.3 剂量单位转换规则集未定义 | §8.3 补充 DosageUnitGroup 枚举定义（MASS_GROUP/VOLUME_GROUP/IU_GROUP），明确组内自动换算、跨组返回 RX_ASSIST_UNIT_MISMATCH 的规则；换算系数以常量形式固化 |
| 6. §3.3 MedicalRecord 实体字段未定义 | §3.3 新增 MedicalRecord 实体描述、MedicalRecordField 枚举定义（7 个顶层字段标识）、RecordGenerateRequest/Response DTO 描述；DepartmentTemplateConfig.requiredFields 明确类型为 List\<MedicalRecordField\> |
| 7. §3.1/§3.3 规则/模板配置变更缺少审计溯源 | §9.3 新增 ConfigChangeLog 实体定义（归属 admin 模块）、写入触发点说明（TemplateConfigChangeEvent/规则变更事件处理链）、TemplateConfigChangeEvent 携带 oldTemplate/newTemplate 快照；§2.1 目录补充 admin 模块 entity/repository 条目 |
| 8. §2.1 ConfigChangeLog 目录归属与文本推荐不一致 | §2.1 目录中 ConfigChangeLog 归入 admin 模块而非 prescription 模块；§9.3 明确 admin 模块监听器通过 Spring ApplicationEvent 接收事件，medical-record 模块无需直接依赖 admin 模块 |

## 修订说明（v5）

| 审查意见 | 修改措施 |
|---------|---------|
| 1.[严重] AiSuggestionResult 状态枚举缺少 FAILED 状态，异步 AI 失败时产生"幽灵 pending"场景 | AiSuggestionResult 的 status 新增 FAILED 枚举值和 failReason 可选字段（§3.4）；查询模式从三分支扩展为四分支（§3.4, §4.4, §6.3）；引入"预创建→更新"模式：check-dose 响应前以 PENDING 预创建条目，异步回调完成后更新为 COMPLETED 或 FAILED，CompletableFuture.exceptionally() 处理器捕获异常时写入 FAILED 而非留 PENDING 僵尸条目 |
| 2.[中] TriageRequest DTO 存在于目录但设计文本无定义，与 DialogueCreateRequest 关系不明确 | 从 §2.1 目录中移除 TriageRequest.java，统一使用 DialogueCreateRequest 作为分诊请求 DTO；§3.1 DialogueCreateRequest 补充 sessionId 字段说明（多轮场景复用时携带，首轮为空） |
| 3.[中] LocalRuleResult / AuditIssue / AuditRequest / AuditResponse / FieldMissingHint 等关键 DTO 存在于目录但设计文本无定义 | §3.2 补充 AuditRequest（处方药品列表+患者信息+处方标识）、AuditResponse（riskLevel+issues+fromFallback）、AuditIssue（fieldName+issueDescription+ruleId+severity）、LocalRuleResult（ruleId+passed+message+severity 及聚合方式说明）、BlockResponse（阻断原因列表+阻断码+阻断时间）的完整定义；§3.3 补充 FieldMissingHint（missingField+promptMessage+suggestedAction）定义；§1.3 核心抽象一览同步补充上述 DTO；RecordGenerateResponse 的 missingFields 类型从 List\<MedicalRecordField\> 更新为 List\<FieldMissingHint\> |
| 4.[中] DosageThresholdService 四级匹配策略未定义"记录不存在"时的行为 | §8.4 匹配优先级新增第 5 级"标准不存在"：四级均未命中时返回 DosageAlert（alertLevel=WARN，携带 RX_ASSIST_DOSE_STANDARD_NOT_FOUND 错误码），前端展示"该药品剂量标准未配置，请核实剂量"；§4.4 check-dose 流程补充此分支路径 |
| 5.[中] DosageAlert 告警级别（alertLevel）枚举值未定义 | §3.4 新增 DosageAlertLevel 枚举定义（INFO/WARN/CRITICAL），明确各级别的语义、前端渲染策略和业务流程约束；CRITICAL 级别联动触发 PrescriptionAuditService 的 BLOCK 审核；§1.3 核心抽象一览补充 DosageAlertLevel |
| 6.[中] check-dose 请求缺乏用药频率（frequency）字段，无法支持日剂量上限校验 | DosageCheckRequest 新增 frequency 字段（§4.4, §2.1）；DosageStandard 实体新增 dailyMax 可选字段（§8.4）；DosageThresholdService.check() 新增日剂量校验分支（§3.4, §4.4）；§4.4 补充日剂量校验分层策略说明（check-dose 即时校验 vs 处方审核终审校验互为补充） |
| 7.[中] MedicalRecord 实体缺少就诊关联标识（visitId/registrationId），病历数据存在追溯缺口 | MedicalRecord 实体新增 visitId 必填字段（§3.3），关联 Registration/Visit 记录；RecordGenerateRequest 同步新增 visitId 必填参数（§3.3, §4.3） |
| 8.[低] PrescriptionAssistResponse 字段定义不完整 | §2.1 目录注释修正为"含 alerts 和 taskId"；§3.4 新增 PrescriptionAssistResponse 完整定义（alerts 列表 + taskId）；§1.3 核心抽象一览补充 PrescriptionAssistResponse |
| 9.[低] 设计文本中缺少 AuditRiskLevel 与需求文档风险等级术语的映射说明 | §3.2 AuditRiskLevel 定义处增加与需求文档术语映射说明：PASS↔低风险(LOW)、WARN↔中风险(MEDIUM)、BLOCK↔高风险(HIGH)；§4.2 WARN 分支显式说明"允许医生强制提交并留痕"以对齐需求 3.2.2.7 中 MEDIUM 风险下医生三种可选操作；§7 设计决策表新增 AuditRiskLevel 术语映射和 WARN 级医生操作两条决策记录 |

## 修订说明（v6）

| 审查意见 | 修改措施 |
|---------|---------|
| 1.[严重] AiSuggestionResult 不支持并发更新——预创建→更新模式在异步场景下存在数据竞争 | AiSuggestionResult 内部字段更新改为通过 ConcurrentHashMap.compute() 原子操作保护（§3.4, §6.3）；预创建使用 computeIfAbsent() 原子操作；同一 taskId 的状态更新保证幂等——非法状态转换（如 COMPLETED→FAILED）在 compute 内校验前置条件后静默忽略（§3.4）；§6.3 补充并发安全保证的完整描述 |
| 2.[严重] 包E check-dose 请求与需求文档 3.4.10 AI 辅助开方输入契约完全脱节 | 新增 POST /api/prescription/assist 作为 3.4.10 主端点，接收完整输入（diagnosis / examResults / patientInfo / existingPrescription / encounterId）返回完整结构化输出（prescriptionDraft / doseWarnings / allergyWarnings / disclaimerRequired）（§3.4, §4.4）；新增 PrescriptionAssistRequest 承载完整输入、PrescriptionAssistResponse 承载完整输出（含 prescriptionDraft / doseWarnings / allergyWarnings / disclaimerRequired），对齐需求文档 3.4.10 输出契约（§3.4）；check-dose 明确定位为即时剂量阈值校验子端点，主端点覆盖完整的 AI 辅助开方能力；§2.1 目录补充 PrescriptionAssistRequest / DoseWarning / AllergyWarning / DosageCheckResponse 等新增 DTO；§7 设计决策表新增"包E 主端点定位"决策记录 |
| 3.[严重] TriageResponse 与需求文档 3.4.1 输出契约不对齐——缺少推荐医生列表和推荐理由 | TriageResponse 增加 doctors 字段（List\<RecommendedDoctor\>，每项含 doctorId/doctorName/departmentId/availableSlotCount/score）和 reason 字段（必填，AI 无法生成时输出默认文案，不可为空字符串或 null）（§3.1）；新增 RecommendedDoctor DTO（§3.1, §1.3）；DialogueCreateRequest 增加 ruleVersion（可选）和 additionalResponses（可选，List\<AdditionalResponse\>，每项含 question/answer/answeredAt）（§3.1）；新增 AdditionalResponse DTO（§2.1）；§1.3 核心抽象一览同步更新 TriageResponse 和 DialogueCreateRequest |
| 4.[严重] AuditRequest 与需求文档 3.4.2 输入契约不对齐——缺少完整字段映射 | AuditRequest.prescriptionItems 每项显式包含 dose/frequency/duration/route 六个字段；AuditRequest.patientInfo 显式包含 allergyHistory（string）和 comorbidities（List\<String\>）（§3.2） |
| 5.[严重] RecordGenerateRequest 与需求文档 3.4.3 输入契约不对齐——缺少 encounter_id 映射说明 | RecordGenerateRequest 中 visitId 重命名为 encounterId，明确映射需求文档 3.4.3 encounter_id（§3.3）；删除 departmentId 显式参数——科室信息由后端通过 encounterId 自动获取，encounterId 缺失时从医生端会话上下文获取科室兜底（§3.3）；§7 设计决策表新增"RecordGenerateRequest 契约对齐"决策记录 |
| 6.[严重] 需求文档 3.4.1 定义 session_id 为必填，但设计文档允许首轮请求 sessionId 为空，构成契约必填/可选语义矛盾 | 在设计文档中明确首轮请求时 sessionId 为空的合理性：需求文档 3.4.1 的"必填"指多轮场景下必填，首轮由服务端生成 UUID v4 后返回，后续必填（§3.1, §3.1 DialogueSessionManager）；sessionId 由 DialogueSessionManager 统一生成，采用 UUID v4 格式（36 字符含连字符）（§3.1）；§7 设计决策表新增"session_id 必填/可选语义"和"sessionId 生成策略"两条决策记录 |
| 7.[一般] WARN 级处方"强制提交并留痕"的留痕数据结构未定义 | AuditRecord 增加 forceSubmitted（Boolean，null 表示未触发 WARN 流程）和 forceSubmitTime（LocalDateTime，仅 forceSubmitted=true 时填充）字段（§3.2）；§4.2 WARN 分支显式补充留痕步骤；§7 设计决策表新增"AuditRecord 留痕"决策记录 |
| 8.[一般] sessionId 生成策略和格式未定义 | sessionId 由 DialogueSessionManager 统一生成，采用 UUID v4 格式（§3.1, §3.1 DialogueSessionManager）；首轮请求时 sessionId 为空，服务端创建新会话后生成 UUID v4 并返回（§3.1）；§7 设计决策表新增"sessionId 生成策略"决策记录 |
| 9.[一般] DosageAlertLevel.CRITICAL 与 AuditRiskLevel.BLOCK 的联动触发机制调用链路未定义 | CRITICAL 级别剂量告警不直接调用 PrescriptionAuditService，而是写入处方草稿上下文 PrescriptionDraftContext（§3.4 新增 PrescriptionDraftContext 抽象）；处方提交时 PrescriptionAuditService 从草稿上下文读取 CRITICAL 标记作为 BLOCK 判定输入（§4.2）；§4.4 补充 CRITICAL→BLOCK 联动机制说明；§6.4 补充草稿上下文并发管理说明；§7 设计决策表新增"CRITICAL→BLOCK 联动机制"决策记录 |
| 10.[一般] 本地规则引擎 AllergyCheckRule 和 DrugInteractionRule 的数据来源未定义 | §3.2 LocalRuleEngine 补充 Phase 2/3 实现范围说明——仅 DosageLimitRule 启用，DrugInteractionRule/AllergyCheckRule 标注为待实现项；§2.1 目录补充 DrugInteractionPair 和 DrugAllergyMapping 骨架实体（标注 Phase 3 待实现）；§7 设计决策表新增"本地规则 Phase 2/3 实现范围"决策记录 |
| 11.[一般] 分诊降级链中 AI 无结果与规则无结果的判定边界不清晰 | §3.1 TriageService 协作描述明确降级判定语义：AiResult.success=false 或 AiResult.degraded=true 时判定 AI 不可用、触发降级；AiResult.success=true 但推荐列表为空视为有效结果（AI 明确判断无匹配科室），跳过规则引擎（§3.1, §4.1）；§7 设计决策表新增"分诊降级判定语义"决策记录 |
| 12.[一般] 处方审核 AI 超时阈值和病历生成 AI 超时阈值未在设计文本中体现 | §3.2 PrescriptionAuditService 补充超时配置 ai.timeout.prescription-audit=6s；§3.3 MedicalRecordService 补充超时配置 ai.timeout.medical-record-generate=12s；§5.5 新增"AI 超时配置"章节，含 4 项 AI 能力超时阈值映射表；§7 设计决策表新增"AI 超时配置"决策记录 |
| 13.[一般] AuditResponse 与需求 3.4.2 输出契约对齐缺失——缺少 interactions 和 suggestions 字段 | AuditResponse 补充 interactions（List\<DrugInteraction\>，每项含 drugPair/severity/description）和 suggestions（List\<Suggestion\>，每项含 suggestionCode/suggestionText）字段（§3.2）；新增 DrugInteraction 和 Suggestion 两个 DTO（§1.3, §2.1）；§4.2 正常路径和降级路径的 AuditResponse 描述同步更新 |
| 14.[一般] TriageRecord 持久化字段定义缺失，无法满足需求 3.4.1 可观测性约束 | §3.1 新增 TriageRecord（JPA @Entity）完整字段定义：sessionId、patientId、chiefComplaint（主诉快照）、aiRecommendedDepartments（AI 推荐科室快照 JSON）、ruleMatchedDepartments（规则匹配科室快照 JSON）、finalDepartmentId、finalDepartmentName、confidence、degraded（降级标记）、ruleVersion、triageTime（§3.1）；§4.1 分诊流程补充"持久化"步骤——分诊完成后写入 TriageRecord；§7 设计决策表新增"TriageRecord 持久化"决策记录 |
| 15.[轻微] 分诊规则引擎缓存刷新时间与对话会话 TTL 的交互影响未分析 | DialogueSession 新增 ruleVersion 快照字段（§3.1）；DialogueSessionManager 在创建会话时记录 TriageRuleEngine.currentRuleVersion()（§3.1）；TriageRuleEngine 新增 currentRuleVersion() 方法返回当前规则版本号（§3.1）；后续追问使用快照版本对应的规则，确保对话内分诊逻辑前后一致（§3.1）；§7 设计决策表新增"对话内规则一致性"决策记录 |
| 16.[轻微] DosageThresholdService 同优先级多条结果"取 dosageMax 最小者"策略可能导致误报 | 种子数据初始化时检测同优先级重复记录并报错中断——通过 UNIQUE 约束 + 预校验查询实现（§8.1）；保留"取 dosageMax 最小者"策略作为运行时兜底（数据录入未走种子脚本时）；§7 设计决策表新增"DosageThresholdService 同优先级多条"决策记录 |
