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
| DialogueSession | class | 多轮对话会话状态对象，维护当前 session 的对话上下文、已收集的症状信息、当前轮次、规则版本与规则集快照、AI 连续失败计数 |
| DialogueSessionManager | class | 对话会话生命周期管理器，负责 session 的创建、查找、更新和过期清理，承担并发访问控制职责；统一生成 sessionId（UUID v4） |
| TriageRuleEngine | interface | 分诊规则引擎契约，根据症状匹配规则返回推荐科室；支持可配置规则源 |
| DepartmentFallbackProvider | interface | 兜底科室列表提供者契约，当 AI 无结果且规则匹配也无结果时返回静态兜底科室列表或基于简单规则的匹配结果 |
| TriageResponse | class（DTO） | 分诊响应值对象，对齐需求文档 3.4.1 输出契约。包含推荐科室列表（departments，List\<RecommendedDepartment\>，每项含 departmentId / departmentName / score，0–3 项）、推荐医生列表（doctors，List\<RecommendedDoctor\>，每项含 doctorId / doctorName / departmentId / availableSlotCount / score，0–5 项）、推荐理由（reason，必填，字符数 ≥ 1，AI 无法生成时输出默认文案如"根据症状分析，建议就诊XX科室"，不可为空字符串或 null）、匹配规则列表（matchedRules，List\<MatchedRule\>，每项含 ruleId / ruleName / score）、会话标识（sessionId，多轮场景）、是否需要追问标记（needFollowUp）及追问内容（followUpQuestion）、置信度 confidence（可选）、降级标记（degraded，对齐需求文档 3.4.1 degraded 输出字段）、兜底提示（fallbackHint，AI 连续失败 3 次时携带"建议直接联系线下接诊窗口"文案） |
| RecommendedDepartment | class（DTO） | 推荐科室值对象，对齐需求文档 3.4.1 recommended_departments 每项结构。包含 departmentId、departmentName、score |
| RecommendedDoctor | class（DTO） | 推荐医生值对象，对齐需求文档 3.4.1 recommended_doctors 每项结构。包含 doctorId、doctorName、departmentId、availableSlotCount（可预约时段数）、score（匹配评分） |
| MatchedRule | class（DTO） | 匹配规则值对象，对齐需求文档 3.4.1 matched_rules 数组每项结构。包含 ruleId、ruleName、score |
| DialogueCreateRequest | class（DTO） | 分诊对话创建请求值对象，对齐需求文档 3.4.1 输入契约。包含患者主诉（chiefComplaint，必填，字符数 5–500，对齐需求文档 3.4.1 约束）、患者基本信息（patientId、age、gender）、会话标识（sessionId，多轮场景复用时携带；首轮请求时 sessionId 为空，服务端新建会话后返回 sessionId）、规则版本号（ruleVersion，可选，对齐需求文档 3.4.1 rule_version）、规则集标识（ruleSetId，可选，对齐需求文档 3.4.1 rule_set_id；与 ruleVersion 语义不同——ruleVersion 标识规则版本号，ruleSetId 标识规则集实体标识）、追问回答数组（additionalResponses，可选，List\<AdditionalResponse\>，对齐需求文档 3.4.1 additional_responses；与 chiefComplaint 互斥——首轮请求仅传 chiefComplaint，后续多轮追问仅传 additionalResponses + sessionId，不同时提供） |

#### 包D-AI1（处方审核）

| 抽象 | 类型形态 | 职责定位 |
|------|---------|---------|
| PrescriptionAuditController | class | 处方审核 REST 端点，接收待审核处方，返回审核结果与风险等级；在收到 BLOCK 结果时拒绝处方提交，通过 HTTP 422 返回阻断详情 |
| PrescriptionAuditService | interface | 处方审核业务契约，协调 AI 审核调用与本地规则回退；审计结果的 BLOCK 等级具有强制阻断语义，调用方需据此执行阻断决策 |
| AuditRiskLevel | enum | 风险等级枚举，定义 PASS（通过，对应需求文档"低风险"）、WARN（警告，对应需求文档"中风险"）、BLOCK（阻断，对应需求文档"高风险"）三个级别；BLOCK 级别在后端具有强制阻断语义 |
| AuditRecord | JPA @Entity | 审核记录持久化实体，保存每次审核的原始处方、AI 结果、风险等级和时间戳，以及处方单号、医生 ID、患者 ID 等业务关联标识；含强制提交留痕字段和审核次序管理字段 |
| LocalRuleEngine | interface | 本地规则校验引擎契约，封装药物相互作用、过敏检查、剂量上限等规则；AI 超时或不可用时作为降级回退 |
| PrescriptionAuditEnforcer | interface | 阻断执行策略契约，定义 BLOCK 结果的强制阻断行为；默认实现返回阻断错误码和阻断原因集合，供 Controller 层组装响应 |
| AuditRequest | class（DTO） | 处方审核请求值对象，对齐需求文档 3.4.2 输入契约。包含处方药品列表（prescriptionItems，每项含 drugId/drugName/dose/frequency/duration/route 六个字段）、患者个体信息（patientInfo，含 patientId/age/gender/allergyHistory/comorbidities）、处方标识（prescriptionId，必填） |
| AuditResponse | class（DTO） | 处方审核响应值对象，对齐需求文档 3.4.2 输出契约。包含风险等级（riskLevel，AuditRiskLevel）、风险提示列表（alerts，List\<AuditAlert\>，每项含 alertCode / alertMessage / severity）、药物相互作用结果（interactions，List\<DrugInteraction\>）、用药建议（suggestions，List\<Suggestion\>）、是否降级标记（fromFallback）。alerts 与 issues 的关系：alerts 直接对齐需求文档 3.4.2 输出契约的 alerts 字段（面向前端展示与验收）；issues 为设计内部结构（含 fieldName / ruleId 等细粒度审计信息），供 AuditRecord 持久化和后端阻断逻辑消费。AuditResponse 以 alerts 为主输出字段，issues 内部消费不暴露在响应 DTO 中 |
| AuditAlert | class（DTO） | 风险提示值对象，对齐需求文档 3.4.2 alerts 数组每项结构。包含 alertCode（风险编码）、alertMessage（风险提示文本）、severity（严重程度） |
| DrugInteraction | class（DTO） | 药物相互作用结果值对象，对齐需求文档 3.4.2 interactions 数组每项结构。包含 drugPair（药物对标识）、severity（严重程度）、description（相互作用描述） |
| Suggestion | class（DTO） | 用药建议值对象，对齐需求文档 3.4.2 suggestions 数组每项结构。包含 suggestionCode（建议编码）、suggestionText（建议文本） |
| AuditIssue | class（DTO） | 审核问题条目值对象，设计内部结构，供 AuditRecord 持久化和后端阻断逻辑消费。包含问题字段名（fieldName）、问题描述（issueDescription）、触发规则标识（ruleId）、严重程度（severity）。与 AuditAlert 的映射关系：alertCode ← ruleId + issueDescription 的编码化表达；alertMessage ← issueDescription；severity 共用。AuditAlert 面向需求契约输出，AuditIssue 面向内部审计持久化 |
| BlockResponse | class（DTO） | 阻断响应值对象，包含阻断原因列表、阻断码、阻断时间 |
| LocalRuleResult | class（DTO） | 本地规则校验结果值对象，包含规则标识（ruleId）、是否通过（passed）、消息文本（message）、严重程度（severity）；多条规则结果聚合为列表后参与风险等级判定 |

#### 包D-AI2（病历生成）

| 抽象 | 类型形态 | 职责定位 |
|------|---------|---------|
| MedicalRecordController | class | 病历生成 REST 端点，接收医生端对话文本，返回结构化病历；支持非流式与流式两种输出模式 |
| MedicalRecordService | interface | 病历生成业务契约，协调 AI 结构化输出与科室模板配置；支持流式与非流式两种生成模式 |
| DepartmentTemplateConfig | class | 科室模板配置值对象，按科室标识管理病历生成规则和模板版本 |
| TemplateConfigManager | interface | 科室模板配置管理器契约，支持模板的运行时查询、缓存和热加载；科室标识不存在时兜底返回 DEFAULT 模板 |
| MissingFieldDetector | interface | 关键字段缺失检测器契约，基于科室模板的必填字段列表与 AI 输出实际包含字段进行差集比对，输出缺失字段集合；不修改 AI 产出 |
| FieldMissingHint | class（DTO） | 字段缺失提示值对象，包含缺失字段标识（missingField，MedicalRecordField 枚举）、提示消息（promptMessage）、建议操作（suggestedAction） |
| RecordGenerateRequest | class（DTO） | 病历生成请求值对象，对齐需求文档 3.4.3 输入契约。包含对话文本（dialogueText）、患者 ID（patientId）、就诊标识（encounterId，映射需求文档 3.4.3 encounter_id）、流式输出标记（stream，bool，可选，默认 false，对齐需求文档 3.4.3 stream 字段；Phase 2/3 仅支持非流式模式，stream 字段接受传入但值为 true 时返回 MR_GEN_STREAM_NOT_SUPPORTED 错误码，流式输出待后续迭代实现） |
| RecordGenerateResponse | class（DTO） | 病历生成响应值对象，对齐需求文档 3.4.3 输出契约。包含结构化病历内容（fields，以 MedicalRecordField 为键的键值映射）、缺失字段提示列表（missingFieldHints，List\<FieldMissingHint\>，为需求文档 3.4.3 missing_fields 的结构化升级版本——missing_fields 为 string 数组仅含字段名，missingFieldHints 增加提示消息和建议操作，前者是后者的简化子集）、生成状态标记（如降级标记 fromFallback） |

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
│       │   ├── audit/AuditRequest.java, AuditResponse.java, AuditAlert.java,
│       │   │       AuditIssue.java, BlockResponse.java,
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
│       │   ├── DrugInteractionRule.java, AllergyCheckRule.java, DuplicateCheckRule.java, DosageLimitRule.java
│       │   ├── SpecialPopulationDosageRule.java    # 儿童/老年人群特殊剂量检查
│       │   ├── DrugInteractionPair.java      # 药物相互作用数据实体
│       │   ├── DrugAllergyMapping.java       # 药物过敏映射数据实体
│       │   ├── DrugCompositionDict.java      # 药品成分字典实体（重复用药检查数据来源）
│       │   └── LocalRuleResult.java
│       ├── context/
│       │   └── PrescriptionDraftContext.java # 按 prescriptionId 关联
│       ├── repository/
│       │   ├── AuditRecordRepository.java
│       │   └── DosageStandardRepository.java # 只读，继承 Repository 非 JpaRepository
│       ├── entity/AuditRecord.java           # 含 prescriptionOrderId, doctorId, patientId, forceSubmitted, forceSubmitTime, auditSequence, isLatest
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

**AI 连续失败兜底机制**：每次 AI 调用失败（含超时和不可用）时，DialogueSession.aiFailCount 递增。TriageServiceImpl 在降级后检查 aiFailCount，当 aiFailCount >= 3 时，在 TriageResponse 中附加 fallbackHint 字段，携带"建议直接联系线下接诊窗口"兜底文案。AI 调用成功时 aiFailCount 重置为 0。

为何使用 interface：分诊业务可能存在多种实现（普通分诊 vs 急诊分诊），interface 允许扩展而不影响调用方。

#### DialogueSession（class，可变）

职责：封装一次多轮分诊的完整上下文（sessionId、主诉、症状列表、追问轮次、会话状态、规则版本快照 ruleVersion、规则集快照 ruleSetId、AI 连续失败计数 aiFailCount）。首轮创建时，DialogueSessionManager 记录当前规则引擎的版本号存入 DialogueSession.ruleVersion 和规则集标识存入 DialogueSession.ruleSetId；后续追问使用该快照版本对应的规则，确保对话内分诊逻辑前后一致。

协作：被 DialogueSessionManager 创建/修改/持久化。

为何使用可变 class：每轮追问追加 QA，可变避免频繁对象复制。DialogueSessionManager 承担并发控制——同 session 请求串行，不同 session 独立。

#### DialogueSessionManager（class）

职责：管理 DialogueSession 的创建、查找、更新和过期清理。承担并发访问的唯一控制点。**sessionId 生成**：统一由 DialogueSessionManager 生成，采用 UUID v4 格式（36 字符，含连字符），首轮请求时 sessionId 为空（前端不传），服务端创建新会话后返回 sessionId，后续多轮请求必填 sessionId。

协作：被 TriageService 调用。内部维护 ConcurrentHashMap + ScheduledExecutorService（TTL 30 分钟）。findOrCreate 三种分支：不存在→创建（同时生成 sessionId 并记录当前 ruleVersion 和 ruleSetId），有效→恢复，已超时→TRIAGE_SESSION_EXPIRED 错误。

为何使用 class 而非 interface：存储策略虽有变更可能，但管理器职责边界稳定，interface 抽象收益在当前阶段不抵实现复杂度。

#### TriageRuleEngine（interface）

职责：症状→科室匹配规则引擎。规则存储在数据库，支持热加载。提供 currentRuleVersion() 方法返回当前规则版本号，提供 currentRuleSetId() 方法返回当前规则集标识，供 DialogueSessionManager 在创建会话时快照。

协作：被 TriageService 在 AI 不可用时调用。配合 Caffeine refreshAfterWrite（默认 60 秒）定时刷新规则缓存。

为何使用 interface：规则源可能从数据库演化为分布式规则引擎。

#### DepartmentFallbackProvider（interface）

职责：AI 和规则引擎均无法决策时返回静态兜底科室列表。

#### TriageResponse（class，DTO）

职责：封装分诊响应的结构化数据，对齐需求文档 3.4.1 输出契约。包含推荐科室列表（departments，List\<RecommendedDepartment\>，0–3 项）、推荐医生列表（doctors，List\<RecommendedDoctor\>，0–5 项）、推荐理由（reason，必填字段，字符数 ≥ 1，对齐需求文档 3.4.1 reason 字段；AI 无法生成有意义的推荐理由时输出默认文案如"根据症状分析，建议就诊XX科室"，不可为空字符串或 null）、匹配规则列表（matchedRules，List\<MatchedRule\>，对齐需求文档 3.4.1 matched_rules 字段）、会话标识（sessionId，多轮追问场景携带）、是否需要追问标记（needFollowUp）及追问内容（followUpQuestion）、置信度标量（confidence，可选，AI 模式时由 AiService.triage() 填充）、降级标记（degraded，对齐需求文档 3.4.1 degraded 输出字段）、兜底提示（fallbackHint，AI 连续失败 ≥ 3 次时携带"建议直接联系线下接诊窗口"文案，可选）。单轮场景 sessionId 为空。

#### RecommendedDepartment（class，DTO）

职责：封装推荐科室值对象，对齐需求文档 3.4.1 recommended_departments 每项结构。包含 departmentId、departmentName、score。

#### RecommendedDoctor（class，DTO）

职责：封装推荐医生值对象，对齐需求文档 3.4.1 recommended_doctors 每项结构。包含 doctorId、doctorName、departmentId、availableSlotCount（可预约时段数）、score（匹配评分）。

#### MatchedRule（class，DTO）

职责：封装匹配规则值对象，对齐需求文档 3.4.1 matched_rules 数组每项结构。包含 ruleId、ruleName、score。

#### DialogueCreateRequest（class，DTO）

职责：封装分诊对话请求值对象，对齐需求文档 3.4.1 输入契约。包含患者主诉（chiefComplaint，必填，字符数 5–500，对齐需求文档 3.4.1"主诉文本字符数 5–500"约束）、患者基本信息（patientId、age、gender）、会话标识（sessionId，多轮追问场景复用已有 sessionId，通过 DialogueSessionManager 恢复上下文；**首轮请求时 sessionId 为空，由服务端生成 UUID v4 并返回**——此设计与需求文档 3.4.1 session_id"必填"约束的映射关系为：需求文档的"必填"指多轮场景下必填，首轮由服务端生成后后续必填）、规则版本号（ruleVersion，可选，对齐需求文档 3.4.1 rule_version 字段）、规则集标识（ruleSetId，可选，对齐需求文档 3.4.1 rule_set_id 字段；与 ruleVersion 语义不同——ruleVersion 标识规则版本号，ruleSetId 标识规则集实体标识；TriageRuleEngine 使用 ruleSetId 确定加载哪套规则集，使用 ruleVersion 确定该规则集的具体版本）、追问回答数组（additionalResponses，可选，List\<AdditionalResponse\>，对齐需求文档 3.4.1 additional_responses 字段；每项含 question / answer / answeredAt，对应 3.1.3.1 "多轮调用"组装约定）。

**chiefComplaint 与 additionalResponses 的互斥语义**：对齐需求文档 3.4.1 定义的"与单字段 chief_complaint 二选一使用，不同时提供"约束。首轮请求仅传 chiefComplaint（必填），additionalResponses 不传；后续多轮追问仅传 additionalResponses + sessionId，chiefComplaint 不传（或传空）。TriageServiceImpl 在处理时校验此互斥规则：首轮（sessionId 为空时）chiefComplaint 必填且 additionalResponses 不允许携带；后续轮（sessionId 非空时）additionalResponses 必填且 chiefComplaint 忽略。

#### AdditionalResponse（class，DTO）

职责：封装单条追问回答值对象，对齐需求文档 3.4.1 additional_responses 数组每项结构。包含 question（追问问题文本）、answer（患者回答文本）、answeredAt（回答时间，ISO 日期格式）。

#### TriageRecord（JPA @Entity）

职责：分诊结果持久化实体，满足需求文档 3.4.1"分诊结果需持久化以供统计与质量分析"的可观测性约束。包含以下统计必需字段：sessionId（会话标识）、patientId（患者 ID）、chiefComplaint（主诉快照）、aiRecommendedDepartments（AI 推荐科室快照，JSON 文本）、ruleMatchedDepartments（规则匹配科室快照，JSON 文本）、finalDepartmentId（最终选择科室 ID）、finalDepartmentName（最终选择科室名称）、confidence（置信度）、degraded（降级标记，boolean——该次分诊是否经过规则引擎或兜底提供者降级）、ruleVersion（使用的规则版本号）、ruleSetId（使用的规则集标识）、triageTime（分诊时间戳）。

### 3.2 包D-AI1：处方审核

#### PrescriptionAuditService（interface）

职责：处方审核核心流程：接收处方→调用 AI→AI 不可用时回退本地规则→返回风险等级。审计结果的 BLOCK 等级具有强制阻断语义，调用方（Controller 或 Orchestrator）必须根据 AuditRiskLevel 执行阻断决策，不得将 BLOCK 处方提交至下一环节。

协作：正常路径委托 AiService.prescriptionCheck()，降级路径委托 LocalRuleEngine；无论正常或降级路径，均在返回前写入 AuditRecord 持久化（降级路径的 AuditRecord 携带 fromFallback=true 标记）；返回结果中以 alerts 字段对齐需求文档 3.4.2 输出契约，内部 issues 供持久化和阻断逻辑消费。

为何使用 interface：审核策略未来可能演进（如分级审核、人工审核兜底），interface 隔离变更影响。

超时配置：ai.timeout.prescription-audit=6s（对齐需求文档 3.4.2 硬超时 ≤ 6 秒）。

#### AuditRiskLevel（enum）

PASS / WARN / BLOCK，固定有限分类。术语与需求文档的映射关系：
- **PASS**（对应需求文档"低风险"，3.2.2.7 中 LOW）：仅给出建议性提示，不影响提交
- **WARN**（对应需求文档"中风险"，3.2.2.7 中 MEDIUM）：需显著告警，允许医生强制提交并留痕（前端弹窗展示警告信息，医生可选择强制提交/修改后重新审核/撤销审核三种操作）
- **BLOCK**（对应需求文档"高风险"，3.2.2.7 中 HIGH）：业务强制拒绝，后端确保 BLOCK 处方不会被提交

BLOCK 的语义从设计层面定义为"业务强制拒绝"，不是前端展示性标记。后端必须确保 BLOCK 处方不会被提交。

#### AuditRecord（JPA @Entity）

持久化审核全部元数据，对齐需求文档 5.1 处方审核记录实体定义。包含以下业务关联标识作为必填字段：
- prescriptionOrderId：关联的处方单号，用于追溯审核对应的处方订单
- doctorId：开方医生 ID，用于按医生维度分析审核问题分布
- patientId：患者 ID，用于按患者维度查询历史审核记录
- auditTime：审核时间戳
- fromFallback：boolean 标记，为 true 表示该审核结果来自本地规则降级而非 AI 审核，用于区分统计 AI 可用率与规则命中率
- **forceSubmitted**：Boolean，标记医生是否对该 WARN 级审核结果执行了强制提交（null 表示未触发 WARN 流程）
- **forceSubmitTime**：LocalDateTime，强制提交的时间戳（仅 forceSubmitted=true 时填充）
- **auditSequence**：int，必填，审核次序——同一 prescriptionOrderId 下多次审核按递增顺序编号（1、2、3...），对齐需求文档 5.1"审核次序"字段和 5.3 处方 1—N 审核记录关系
- **isLatest**：boolean，必填，是否最新——同一 prescriptionOrderId 下仅最新一条记录 isLatest=true，之前记录全部置为 false，对齐需求文档 5.1"是否最新"字段
- originalPrescription、riskLevel、aiResult、auditIssues 等审核结果数据

同一处方多次审核时的处理：每次新审核写入 AuditRecord 时，先将同一 prescriptionOrderId 下已有的 isLatest=true 的记录更新为 isLatest=false，再插入新记录并置 auditSequence = 上一条记录的 auditSequence + 1，isLatest=true。此操作在事务内完成保证一致性。

这些关联标识和留痕字段使 AuditRecord 具备完整的业务可追溯性，支持按医生、患者、处方单号三个维度的查询和分析，fromFallback 标记辅助 AI 可用率统计，forceSubmitted/forceSubmitTime 满足"强制提交留痕"合规要求，auditSequence/isLatest 满足"同一处方多次审核按次序分别保存"的需求。

#### LocalRuleEngine（interface）

封装 DosageLimitRule、AllergyCheckRule、DuplicateCheckRule、SpecialPopulationDosageRule 等独立规则。每条规则独立实现/测试/启用/禁用。

**Phase 2/3 实现范围说明**：

本地规则校验的最小检查项集对齐需求文档 3.4.2 规定的 4 项：

| 检查项 | 对应需求文档 3.4.2 | Phase 2/3 规则 | 数据来源 | 实现状态 |
|--------|--------------------|----------------|---------|---------|
| ①剂量范围检查 | 最小检查项集 #1 | DosageLimitRule | DosageStandard 实体（common 模块） | 完整实现 |
| ②药品禁忌检查 | 最小检查项集 #2 | AllergyCheckRule | DrugAllergyMapping + 患者过敏史/合并症 | 完整实现 |
| ③重复用药检查 | 最小检查项集 #3 | DuplicateCheckRule | DrugCompositionDict（药品成分字典） | 完整实现 |
| ④儿童/老年人群特殊剂量检查 | 最小检查项集 #4 | SpecialPopulationDosageRule | DosageStandard 年龄/体重分级字段 | 完整实现 |

DrugInteractionRule（药物相互作用 DDI）仍不在 Phase 2/3 本地规则范围内——需求文档 3.4.2 明确指出"药物-药物相互作用（DDI）的完整交叉检测"属于需要 AI 推理或外部知识库支撑的检查项，不在本地规则校验覆盖范围内。DrugInteractionRule 标注为后续迭代项，DrugInteractionPair 数据实体在目录中预留骨架但不参与 Phase 2/3 运行时校验。

数据来源说明：
- DrugAllergyMapping：药品过敏映射实体（drugCode → allergenList），由管理员在 admin 模块"药品字典维护"入口维护；Phase 2/3 以种子脚本初始化常见药品过敏原映射数据
- DrugCompositionDict：药品成分字典实体（drugCode → ingredientList），由管理员在 admin 模块维护；Phase 2/3 以种子脚本初始化常用药品成分数据
- AllergyCheckRule 逻辑：遍历处方药品列表，对每个药品查询 DrugAllergyMapping 获取该药品关联的过敏原列表，再与患者 allergyHistory 和 comorbidities 交叉比对——命中时产出 BLOCK 级别 LocalRuleResult
- DuplicateCheckRule 逻辑：遍历处方药品列表，对每个药品查询 DrugCompositionDict 获取成分列表，构建 drugCode → ingredientSet 映射后检测成分交集——存在两种及以上药品共享相同成分时产出 WARN 级别 LocalRuleResult
- SpecialPopulationDosageRule 逻辑：根据患者年龄判断是否属于特殊人群（儿童 ≤ 14 岁、老年 ≥ 65 岁），若是则查询 DosageStandard 中对应年龄/体重分级的剂量标准，与处方中实际剂量做比对——超出该人群特殊剂量上限时产出 BLOCK 级别 LocalRuleResult

#### LocalRuleResult（class，DTO）

职责：封装单条本地规则的校验结果。包含规则标识（ruleId）、是否通过（passed）、消息文本（message）、严重程度（severity）。多条 LocalRuleResult 聚合为列表后，由 PrescriptionAuditService 实现根据各 ruleResult 的 severity 聚合为最终的 AuditRiskLevel：若任一规则 severity 为 BLOCK 则整体判定为 BLOCK；若无 BLOCK 但存在 WARN 则整体判定为 WARN；全部 PASSED 则判定为 PASS。

#### AuditRequest（class，DTO）

职责：封装处方审核请求值对象，对齐需求文档 3.4.2 输入契约。包含处方药品列表（prescriptionItems，每项含 drugId/drugName/dose/frequency/duration/route 六个字段）、患者个体信息（patientInfo，含 patientId/age/gender/allergyHistory（string，过敏史文本）/comorbidities（List\<String\>，合并症列表））、处方标识（prescriptionId，必填）。

#### AuditResponse（class，DTO）

职责：封装处方审核响应值对象，对齐需求文档 3.4.2 输出契约。包含风险等级（riskLevel，AuditRiskLevel 枚举）、风险提示列表（**alerts，List\<AuditAlert\>，对齐需求文档 3.4.2 alerts 字段；每项含 alertCode / alertMessage / severity**）、药物相互作用结果（interactions，List\<DrugInteraction\>，对齐需求文档 3.4.2 interactions 字段；降级时为空列表）、用药建议（suggestions，List\<Suggestion\>，对齐需求文档 3.4.2 suggestions 字段；降级时为空列表）、是否降级标记（fromFallback，boolean，对应需求文档 3.4.2 "AI 不可用，已回退本地规则校验"标记）。

**alerts 与 issues 的关系说明**：需求文档 3.4.2 输出契约定义的风险提示字段名为 `alerts`（每项含 alert_code / alert_message / severity）。设计内部另定义 AuditIssue（含 fieldName / issueDescription / ruleId / severity）作为细粒度审核问题结构，供 AuditRecord 持久化和后端阻断逻辑消费。AuditResponse 以 alerts 为主输出字段对齐需求契约；AuditIssue 不暴露在 API 响应中，仅作为内部数据结构。二者映射关系：alertCode ← ruleId + 问题类型的编码化表达；alertMessage ← issueDescription；severity 共用。

#### AuditAlert（class，DTO）

职责：封装风险提示值对象，对齐需求文档 3.4.2 alerts 数组每项结构。包含 alertCode（风险编码，如"DOSAGE_LIMIT_EXCEEDED"、"ALLERGY_CONFLICT"）、alertMessage（风险提示文本，自然语言描述风险内容）、severity（严重程度）。

#### DrugInteraction（class，DTO）

职责：封装药物相互作用结果值对象，对齐需求文档 3.4.2 interactions 数组每项结构。包含 drugPair（药物对标识，如"A-B"）、severity（严重程度）、description（相互作用描述）。

#### Suggestion（class，DTO）

职责：封装用药建议值对象，对齐需求文档 3.4.2 suggestions 数组每项结构。包含 suggestionCode（建议编码）、suggestionText（建议文本）。

#### AuditIssue（class，DTO，设计内部结构）

职责：封装单条审核问题条目，供 AuditRecord 持久化和后端阻断逻辑消费。包含问题字段名（fieldName，如药品名称或剂量字段）、问题描述（issueDescription，自然语言描述问题内容）、触发规则标识（ruleId，标识触发该问题的规则来源）、严重程度（severity，与 AuditRiskLevel 对齐）。不暴露在 AuditResponse 中——AuditResponse 以 alerts 字段对齐需求契约输出。

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

枚举命名与需求文档字段名映射说明：TREATMENT_ADVICE 对应需求文档 treatment_plan（设计选用 TREATMENT_ADVICE 因"治疗意见"更贴合中文临床语义，与需求文档"治疗意见"表述一致）；其余枚举名采用 UPPER_SNAKE_CASE 风格的英文化命名，与需求文档 snake_case 字段名一一对应。此映射在 MissingFieldDetector 和 RecordGenerateResponse 的 fields 键值映射中统一使用。

此枚举是 MissingFieldDetector 差集比对和 DepartmentTemplateConfig.requiredFields 的类型基础，使字段标识符与模板内容一致。

#### RecordGenerateRequest（class，DTO）

职责：病历生成请求值对象，对齐需求文档 3.4.3 输入契约。包含对话文本（dialogueText）、患者 ID（patientId）、就诊标识（encounterId，映射需求文档 3.4.3 encounter_id；visitId 与 encounter_id 为同一概念，设计中统一命名为 encounterId 以与需求文档术语一致）、流式输出标记（stream，bool，可选，默认 false，对齐需求文档 3.4.3 stream 字段）。

**流式输出 Phase 2/3 实现决策**：Phase 2/3 仅实现非流式模式。stream 字段接受传入，但当 stream=true 时返回 MR_GEN_STREAM_NOT_SUPPORTED 错误码（归属于 MR_ 错误类别），前端据此提示"流式生成模式暂不支持，请使用非流式模式"。此决策理由：流式模式需要 SSE/WebSocket 端点改造、分片协议实现和流式错误分片结构（含 chunk_index / is_final / error.partial_content），实现复杂度较高且 Phase 2/3 优先保障核心非流式路径的完整交付。流式输出待后续迭代实现，届时需补充流式端点、流式超时处理和流式错误分片结构。

encounterId 与 departmentId 的关系：当 encounterId 存在时，后端通过 encounterId 关联就诊记录自动获取科室信息，departmentId 不再需要作为显式参数；若 encounterId 缺失（边缘场景），后端从请求上下文的医生端会话中获取科室信息作为兜底。

为何删除 departmentId 显式参数：需求文档 3.4.3 未定义 departmentId 字段，只定义了 encounter_id；科室信息可由后端从 encounter 关联数据自动获取，无需前端显式传入，减少数据冗余和不一致风险。

#### RecordGenerateResponse（class，DTO）

职责：病历生成响应值对象，对齐需求文档 3.4.3 输出契约。包含结构化病历内容（fields，以 MedicalRecordField 为键的键值映射）、缺失字段提示列表（missingFieldHints，List\<FieldMissingHint\>）、生成状态标记（降级标记 fromFallback）。

**missingFieldHints 与需求文档 missing_fields 的映射说明**：需求文档 3.4.3 输出契约定义 missing_fields 为 string 数组（仅含字段名），missingFieldHints 为其结构化升级版本——除字段名外增加 promptMessage（面向医生的友好提示文案）和 suggestedAction（指导医生如何补全），信息量严格超集。前端可从 missingFieldHints 中提取 missingField 列表以对齐需求文档的 missing_fields 字段名语义。

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

职责：AI 辅助开方主端点请求值对象，对齐需求文档 3.4.10 输入契约。包含诊断结论（diagnosis，string，必填，字符数 1–500）、检查检验结果摘要（examResults，List\<ExamResultItem\>，可选，每项含 itemName / value / unit / referenceRange / status）、患者基础信息（patientInfo，含 patientId / age / gender / allergyHistory / comorbidities）、已有处方（existingPrescription，可选，含 drugs 数组）、就诊标识（encounterId，可选）。

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

**预创建→更新模式**：check-dose 响应返回 taskId 后，服务端即以 PENDING 状态预创建 AiSuggestionResult 条目并存入 ConcurrentHashMap（通过 computeIfAbsent 原子操作）。异步 AI 调用完成后通过 compute() 原子更新为 COMPLETED 或 FAILED。CompletableFuture 的 exceptionally 处理器捕获异常时，将预创建条目更新为 FAILED 状态（填充 failReason），而非保留 PENDING 僵尸条目。

AiSuggestionResult 增加 TTL（同 suggestion TTL 30 分钟），由 ScheduledExecutorService 定期清理过期条目。

#### DosageCheckResponse（class，DTO）

职责：封装剂量阈值即时校验同步响应值对象。包含告警列表（alerts，List\<DosageAlert\>，由 DosageThresholdService 产出的剂量告警）和异步任务标识（taskId，用于前端后续查询 AI 建议）。

#### PrescriptionDraftContext（class）

职责：处方草稿上下文，在医生编辑处方期间暂存 CRITICAL 级别剂量告警标记。当医生在 check-dose 流程中收到 CRITICAL 告警时，该告警标记被写入 PrescriptionDraftContext（按处方编辑会话标识关联）；处方提交时 PrescriptionAuditService 从 PrescriptionDraftContext 读取 CRITICAL 标记作为 BLOCK 判定的输入。

协作：由 DosageThresholdService 写入（CRITICAL 告警时），由 PrescriptionAuditService 读取（处方提交审核时）。

**Key 定义与生命周期管理**：PrescriptionDraftContext 的 key 为 prescriptionId（处方编辑会话标识 = 处方草稿 ID），而非 encounterId。理由：一次就诊可产生多个处方编辑会话（如医生先开具一个处方并保存草稿，再为另一组药品开具新处方），encounterId 与处方编辑会话为 1—N 关系，用 prescriptionId 可精确关联到具体处方的草稿上下文。

生命周期管理：
- **创建时机**：首次 check-dose 请求时，若 PrescriptionDraftContext 中不存在该 prescriptionId 的条目，则自动创建空列表
- **写入时机**：DosageThresholdService 产出 CRITICAL 告警时，写入对应 prescriptionId 的草稿上下文
- **清理时机**：(a) 处方提交成功后清理（PrescriptionAuditService 读取并消费 CRITICAL 标记后移除）；(b) 处方取消时清理；(c) TTL 过期自动清理（默认 60 分钟，由 ScheduledExecutorService 定期扫描）

为何使用 class：草稿上下文为临时性内存数据结构，仅用于解耦 check-dose 即时反馈与处方提交终审两个时点，无需持久化。

---

## 4. 关键行为契约

### 4.1 智能分诊场景

```
POST /api/triage/consult
单轮: { chiefComplaint: "胸痛伴气短", patientId: "P001", age: 45, gender: "男" }
      chiefComplaint 字符数约束: 5–500
多轮: { sessionId: "xxx", additionalResponses: [{question: "...", answer: "...", answeredAt: "..."}] }
      首轮仅传 chiefComplaint，后续轮仅传 additionalResponses + sessionId，二选一互斥

降级判定:
  AiResult.success=false 或 AiResult.degraded=true → 判定 AI 不可用，触发降级；DialogueSession.aiFailCount++
  AiResult.success=true 且推荐列表为空 → AI 有效结果（跳过规则引擎）；aiFailCount 重置
  AiResult.success=true 且推荐列表非空 → 正常返回；aiFailCount 重置

AI 连续失败兜底:
  aiFailCount >= 3 时，TriageResponse 附加 fallbackHint = "建议直接联系线下接诊窗口"

正常: AiService.triage(session context) → 推荐科室 + 推荐医生 + 推荐理由 + 匹配规则
降级链: AiService 不可用 → TriageRuleEngine.match() → 匹配成功返回科室
        → 规则匹配无结果 → DepartmentFallbackProvider.getFallbackDepartments() → 兜底科室
Session 超时: TRIAGE_SESSION_EXPIRED 错误
degraded 标记: 降级时 TriageResponse.degraded=true，对齐需求文档 3.4.1

持久化: 分诊完成后写入 TriageRecord（含 AI 推荐科室快照、规则匹配科室、最终选择科室、置信度、降级标记等）
规则版本: DialogueSessionManager 创建会话时记录 TriageRuleEngine.currentRuleVersion() 和 currentRuleSetId()
         后续追问使用快照版本
```

### 4.2 处方审核场景

```
POST /api/prescription/audit
超时配置: ai.timeout.prescription-audit=6s

正常: AiService.prescriptionCheck() → AuditRecord 持久化（fromFallback=false） → 返回 AuditResponse
      AuditResponse 包含: riskLevel + alerts + interactions + suggestions + fromFallback=false
降级: LocalRuleEngine.check()（4 条规则完整执行）
      → 各条规则产出 LocalRuleResult（ruleId, passed, message, severity）
      → 聚合多条 LocalRuleResult 为最终 AuditRiskLevel
      → AuditRecord 持久化（fromFallback=true） → 返回 AuditResponse（riskLevel + alerts + interactions（降级时为空列表） + suggestions（降级时为空列表） + fromFallback=true）
      → 降级响应中 UI 须标示"本次未经过 AI 全面审核，药物相互作用等深度检查未执行"

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

同一处方多次审核：
  每次审核写入 AuditRecord 时，同一 prescriptionOrderId 下已有记录 isLatest→false
  新记录 auditSequence 递增、isLatest=true
```

### 4.3 病历生成场景

```
POST /api/medical-record/generate
{ dialogueText, patientId, encounterId, stream }
超时配置: ai.timeout.medical-record-generate=12s（非流式）
         ai.timeout.medical-record-generate-stream=2s（流式首字）
         ai.timeout.medical-record-stream-total=30s（流式总时长）
         ai.timeout.medical-record-stream-chunk-interval=5s（流式分片间隔）

stream=false（Phase 2/3 仅支持非流式）:
  正常: TemplateConfigManager.getTemplate → AiService.generateMedicalRecord → MissingFieldDetector
  兜底: 科室不存在时使用 DEFAULT 模板
  降级（分层保护）:
    AI 完全不可用 → 返回空字段集
    差集比对 → 产出 FieldMissingHint 列表（含 missingField + promptMessage + suggestedAction）
    前端: 已提取字段正常渲染 + 缺失字段显示补全提示输入框

stream=true（Phase 2/3 暂不支持）:
  → 返回错误码 MR_GEN_STREAM_NOT_SUPPORTED
  → 前端提示"流式生成模式暂不支持，请使用非流式模式"

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
→ CRITICAL 告警写入 PrescriptionDraftContext（key=prescriptionId）
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

**CRITICAL 与 BLOCK 的联动机制**：DosageAlertLevel.CRITICAL 不在 check-dose 时直接调用 PrescriptionAuditService，而是将 CRITICAL 标记写入 PrescriptionDraftContext（以 prescriptionId 为 key）。处方提交时 PrescriptionAuditService 从 PrescriptionDraftContext 读取 CRITICAL 标记作为 BLOCK 判定的输入，与 AI/本地规则审核结果聚合为最终 AuditRiskLevel。此设计消弭了"剂量检查时点"与"处方提交时点"的矛盾——CRITICAL 告警在编辑阶段即时反馈（前端阻断当前剂量），在提交阶段统一终审（后端 BLOCK 阻断）。处方提交成功后清理对应 PrescriptionDraftContext 条目。

---

## 5. 错误处理策略

### 5.1 模块级错误码

| 错误类别 | 前缀 | 代表场景 |
|---------|------|---------|
| 分诊 | TRIAGE_ | TRIAGE_SESSION_EXPIRED、TRIAGE_AI_TIMEOUT、TRIAGE_AI_UNAVAILABLE、TRIAGE_AI_INPUT_INVALID、参数缺失 |
| 审核 | RX_AUDIT_ | 处方格式不合法、药品编码不存在、BLOCK 阻断；RX_AUDIT_AI_TIMEOUT、RX_AUDIT_AI_UNAVAILABLE |
| 病历 | MR_ | 对话文本过短、科室模板不存在、MR_GEN_STREAM_NOT_SUPPORTED（流式模式暂不支持）；MR_GEN_AI_TIMEOUT、MR_GEN_AI_UNAVAILABLE、MR_GEN_AI_OUTPUT_INCOMPLETE |
| 开方辅助 | RX_ASSIST_ | 剂量标准不存在（RX_ASSIST_DOSE_STANDARD_NOT_FOUND）、单位不匹配、AI 建议不存在、AI 建议生成失败；RX_ASSIST_AI_TIMEOUT、RX_ASSIST_AI_UNAVAILABLE、RX_ASSIST_AI_NO_RECOMMENDATION |

### 5.2 AI 降级作为正常业务流程

各模块的本地规则回退视为正常业务流程。处方审核降级时，AuditRecord 的 fromFallback=true 标记用于区分 AI 审核与本地规则结果，响应体 AuditResponse 的 fromFallback 标记同步告知前端。病历生成降级时 RecordGenerateResponse 同样携带 fromFallback 标记。

处方审核降级时前端须在显著位置标示"本次未经过 AI 全面审核，药物相互作用等深度检查未执行"——对齐需求文档 3.4.2 降级路径要求。

### 5.3 BLOCK 阻断作为独立错误类别

BLOCK 阻断响应通过 BlockResponse 封装，使用 HTTP 422 状态码，与业务异常体系正交。BlockResponse 包含阻断原因列表（每个 AuditIssue 条目含字段名、问题描述、匹配规则标识），前端据此精确展示阻断原因。

### 5.4 与已有异常处理框架的集成

复用 GlobalExceptionHandler + ErrorCode 接口 + BusinessException 体系。BLOCK 阻断不经过异常框架（属于正常业务流程中的阻断分支），直接通过 Controller 返回 422 响应。

### 5.5 AI 超时配置

| AI 能力 | 配置键 | 阈值 | 来源 |
|--------|-------|------|------|
| 处方审核 | ai.timeout.prescription-audit | 6s | 需求文档 3.4.2 硬超时 ≤ 6 秒 |
| 病历生成（非流式） | ai.timeout.medical-record-generate | 12s | 需求文档 3.4.3 非流式硬超时 ≤ 12 秒 |
| 病历生成（流式首字） | ai.timeout.medical-record-generate-stream | 2s | 需求文档 3.4.3 流式首字响应 ≤ 2 秒（Phase 2/3 预留，暂不消费） |
| 病历生成（流式总时长） | ai.timeout.medical-record-stream-total | 30s | 需求文档 3.4.3 流式总时长 ≤ 30 秒（Phase 2/3 预留，暂不消费） |
| 病历生成（流式分片间隔） | ai.timeout.medical-record-stream-chunk-interval | 5s | 需求文档 3.4.3 单分片间隔 ≤ 5 秒（Phase 2/3 预留，暂不消费） |
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

PrescriptionDraftContext 按 prescriptionId（处方编辑会话标识 = 处方草稿 ID）关联，内部使用 ConcurrentHashMap<String, List<DosageAlert>> 存储。同一处方编辑会话的 CRITICAL 告警写入操作与处方提交时的读取操作通过 ConcurrentHashMap 的线程安全特性保证一致性。处方提交成功、处方取消或 TTL 过期（默认 60 分钟）后清理对应条目。

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
| TriageResponse 契约对齐 | 增加 departments(List\<RecommendedDepartment\>) + doctors + reason + matchedRules + degraded + fallbackHint | 全量对齐需求文档 3.4.1 输出契约：recommended_departments(department_id/department_name/score) + recommended_doctors + reason + matched_rules + degraded |
| DialogueCreateRequest 契约对齐 | 增加 ruleVersion + additionalResponses + ruleSetId + chiefComplaint 字符数约束 | 全量对齐需求文档 3.4.1 输入契约：chief_complaint(5–500) + additional_responses + patient_id + session_id + rule_version + rule_set_id |
| AuditResponse 契约对齐 | alerts(List\<AuditAlert\>) 替代 issues 作为主输出字段 | 全量对齐需求文档 3.4.2 输出契约 alerts(alert_code/alert_message/severity)；AuditIssue 降为设计内部结构供持久化和阻断逻辑消费 |
| RecordGenerateRequest 契约对齐 | 增加 stream 字段 + encounterId 映射 encounter_id | 全量对齐需求文档 3.4.3 输入契约：dialogue_text + patient_id + encounter_id + stream |
| MedicalRecordField 与需求文档字段映射 | 枚举名与 snake_case 字段名一一对应，TREATMENT_ADVICE→treatment_plan | 消除字段名映射歧义，前端可按映射关系直接消费 |
| missingFieldHints 与 missing_fields 映射 | missingFieldHints 为 missing_fields 的结构化升级版 | 严格超集关系：missingFieldHints 包含字段名+提示+建议操作；前端可提取字段名列表对齐 missing_fields 语义 |
| session_id 必填/可选语义 | 首轮 sessionId 为空由服务端生成，后续多轮必填 | 需求文档"必填"指多轮场景下必填；首轮由服务端生成 UUID v4 后返回 |
| sessionId 生成策略 | DialogueSessionManager 统一生成，UUID v4 格式 | 后端生成消除前端构造不一致风险，UUID v4 全局唯一无需协调 |
| AiSuggestionResult 并发安全 | ConcurrentHashMap.compute() 原子更新 value 对象 | 消除预创建→更新模式下多线程同时读写 AiSuggestionResult 字段的数据竞争 |
| CRITICAL→BLOCK 联动机制 | CRITICAL 写入 PrescriptionDraftContext，提交时读取作为 BLOCK 输入 | 解耦 check-dose 即时反馈与处方提交终审两个时点，消除触发时机矛盾 |
| TriageRecord 持久化 | 新增实体满足 3.4.1 可观测性要求 | 支持分诊命中率统计与质量分析 |
| AuditRecord 留痕 | 增加 forceSubmitted + forceSubmitTime | 承载 WARN 级"强制提交并留痕"需求 |
| AuditRecord 审核次序管理 | 增加 auditSequence + isLatest | 对齐需求文档 5.1 处方审核记录实体"审核次序"与"是否最新"字段，支持同一处方多次审核按次序保存 |
| AI 超时配置 | 4 项 AI 能力超时阈值映射为可配置项 + 3 项流式预留配置 | 对齐需求文档 3.4.x 超时阈值要求，流式配置预留供后续迭代消费 |
| 分诊降级判定语义 | AiResult.success=false 或 degraded=true 触发降级；success=true + 空列表为有效结果跳过规则引擎 | 消除 AI 无结果与规则无结果的判定边界歧义 |
| 对话内规则一致性 | DialogueSession 快照 ruleVersion + ruleSetId，后续追问使用快照版本 | 规则刷新不影响进行中的对话分诊逻辑 |
| DosageThresholdService 同优先级多条 | 取 dosageMax 最小者 + 种子数据初始化时检测重复报错 | 最保守阈值策略兼顾安全性，初始化校验消除误报根因 |
| 本地规则 Phase 2/3 实现范围 | 4 项最小检查项集完整实现；DDI 排除 | 对齐需求文档 3.4.2 最小检查项集——AllergyCheckRule/DuplicateCheckRule/SpecialPopulationDosageRule 有本地数据支撑可实际运行；DDI 需 AI 或外部知识库支撑，不在本地规则范围 |
| AI 连续失败兜底 | aiFailCount >= 3 时附加 fallbackHint | 对齐需求文档 3.1.3.1"AI 调用失败次数达 3 次后自动给出兜底文案" |
| PrescriptionDraftContext key | 以 prescriptionId 为 key | encounterId 与处方编辑会话为 1—N 关系，prescriptionId 精确关联处方草稿上下文 |
| 流式病历生成 | Phase 2/3 仅支持非流式，流式预留 stream 字段和超时配置 | 流式端点/分片协议/流式错误分片结构实现复杂度高，优先保障核心非流式路径交付 |
| DosageStandard 与药品基础信息实体关系 | 独立实体，通过 drugCode 关联 | admin 模块"药品字典维护"操作药品基础信息实体（名称/规格/成分/单位等），"DosageStandard 管理"是独立的剂量标准维护入口；二者通过 drugCode 关联但不共享主键——药品基础信息实体的主键为自身 ID，drugCode 为国药准字号（如 H109601234），DosageStandard 以 drugCode 为外键间接关联 |
| chiefComplaint 与 additionalResponses 互斥 | 首轮仅传 chiefComplaint，后续仅传 additionalResponses | 对齐需求文档 3.4.1"二选一使用，不同时提供" |
| TriageResponse.degraded | 降级时返回 degraded=true | 对齐需求文档 3.4.1 降级路径"后端返回降级标记 degraded=true" |

---

## 8. 剂量标准初始化与药品编码规范

### 8.1 初始化方案

SQL 种子脚本：backend/modules/prescription/src/main/resources/db/seed/R__dosage_standards.sql
使用 MERGE / INSERT ... ON DUPLICATE KEY UPDATE 实现幂等执行。

同优先级多条记录检测：种子脚本在初始化时检测同优先级（同一 drugCode + routeOfAdministration + age 范围 + weight 范围组合）是否存在重复记录，若存在则报错中断初始化（SQL 脚本通过 UNIQUE 约束 + 预校验查询实现），避免异常小的 dosageMax 导致正常剂量被误判为严重超标。

**DrugAllergyMapping 与 DrugCompositionDict 种子数据**：Phase 2/3 需同步初始化药品过敏映射和药品成分字典的种子数据，以支撑 AllergyCheckRule 和 DuplicateCheckRule 的运行时校验。

### 8.2 药品编码规范

DosageStandard.drugCode 采用国药准字号（如 H109601234）。

**DosageStandard 与药品基础信息实体的关系**：DosageStandard 与管理员模块中"药品字典维护"操作的药品基础信息实体为独立实体，二者通过 drugCode（国药准字号）关联但不共享主键。药品基础信息实体管理药品的基础属性（名称、规格、成分、单位、生产厂家、批准文号等），DosageStandard 管理药品的剂量标准数据（单次剂量上限、日剂量上限、年龄/体重分级等）。admin 模块"药品字典维护"操作的是药品基础信息实体，"DosageStandard 管理"是独立的剂量标准维护入口，两者在 admin 模块中分属不同的管理界面和数据表。drugCode 与药品基础信息实体的"批准文号"字段值为同一编码（国药准字号），DosageStandard 通过 drugCode 外键间接关联到药品基础信息实体。

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

## 修订说明（v7）

| 审查意见 | 修改措施 |
|---------|---------|
| 1.[严重] AuditResponse.issues 与需求文档 3.4.2 alerts 字段命名与结构不一致 | AuditResponse 主输出字段从 issues 改为 alerts（List\<AuditAlert\>，每项含 alertCode / alertMessage / severity），对齐需求文档 3.4.2 输出契约；新增 AuditAlert DTO（§1.3, §3.2）；AuditIssue 降为设计内部结构供 AuditRecord 持久化和阻断逻辑消费，不暴露在 API 响应中；§3.2 补充 alerts 与 issues 映射关系说明；§1.3 核心抽象一览同步更新 |
| 2.[严重] 病历生成缺少流式输出生成模式的设计 | RecordGenerateRequest 新增 stream 字段（bool，可选，默认 false，§1.3, §3.3）；§3.3 补充流式输出 Phase 2/3 实现决策——仅支持非流式，stream=true 时返回 MR_GEN_STREAM_NOT_SUPPORTED 错误码；§5.5 新增 3 项流式超时配置（流式首字 2s、总时长 30s、分片间隔 5s，标注 Phase 2/3 预留暂不消费）；§7 新增"流式病历生成"设计决策 |
| 3.[严重] 本地规则校验仅实现 1/4，不匹配需求文档 3.4.2 最小检查项集 | LocalRuleEngine Phase 2/3 实现范围从"仅 DosageLimitRule"扩展为 4 项最小检查项集完整实现：新增 AllergyCheckRule（药品禁忌检查，数据来源 DrugAllergyMapping + 患者过敏史/合并症）、DuplicateCheckRule（重复用药检查，数据来源 DrugCompositionDict 药品成分字典）、SpecialPopulationDosageRule（儿童/老年人群特殊剂量检查，复用 DosageStandard 年龄/体重分级字段）；DrugInteractionRule（DDI）确认排除因需求文档明确指出需 AI 或外部知识库支撑；§2.1 目录补充 DrugCompositionDict / DuplicateCheckRule / SpecialPopulationDosageRule；§3.2 补充 4 条规则的实现逻辑和数据来源说明；§8.1 补充 DrugAllergyMapping 和 DrugCompositionDict 种子数据初始化要求；§7 更新"本地规则 Phase 2/3 实现范围"设计决策 |
| 4.[一般] DialogueCreateRequest 缺少 ruleSetId 字段 | DialogueCreateRequest 新增 ruleSetId（可选，String，§1.3, §3.1）；DialogueSession 新增 ruleSetId 快照字段；TriageRuleEngine 新增 currentRuleSetId() 方法；DialogueSessionManager 创建会话时同步快照 ruleSetId；§1.3 补充 ruleSetId 与 ruleVersion 语义差异说明——ruleVersion 标识版本号，ruleSetId 标识规则集实体标识；§4.1 更新规则版本快照逻辑；TriageRecord 新增 ruleSetId 字段 |
| 5.[一般] TriageResponse 缺少 matchedRules 字段 | TriageResponse 新增 matchedRules 字段（List\<MatchedRule\>，每项含 ruleId / ruleName / score，§1.3, §3.1）；新增 MatchedRule DTO（§2.1, §1.3）；对齐需求文档 3.4.1 matched_rules 输出契约 |
| 6.[一般] TriageResponse.departments 每项字段与需求文档 recommended_departments 结构不完整对齐 | 新增 RecommendedDepartment DTO（含 departmentId、departmentName、score，§1.3, §3.1）；TriageResponse.departments 类型从简略描述改为 List\<RecommendedDepartment\>；对齐需求文档 3.4.1 recommended_departments 每项含 department_id / department_name / score |
| 7.[一般] RecordGenerateRequest 缺少 stream 字段 | 同问题2，RecordGenerateRequest 新增 stream 字段 |
| 8.[一般] RecordGenerateResponse 的 fields 映射结构与需求文档字段命名不一致 | MedicalRecordField 枚举补充与需求文档 3.4.3 输出契约字段名的完整映射表（§3.3），显式说明 TREATMENT_ADVICE→treatment_plan 的映射关系及命名理由；missingFieldHints 与 missing_fields 的映射关系说明补充为"严格超集"——missingFieldHints 包含字段名+提示+建议操作，前端可提取字段名列表对齐 missing_fields 语义（§3.3, §7） |
| 9.[一般] PrescriptionDraftContext key 定义不清，清理时机不明确 | PrescriptionDraftContext key 明确为 prescriptionId（处方编辑会话标识=处方草稿 ID），而非 encounterId；§3.4 补充理由——一次就诊可有多个处方编辑会话（1—N 关系）；§3.4 补充完整生命周期管理——创建时机（首次 check-dose 请求时）、写入时机（CRITICAL 告警时）、清理时机（处方提交成功/处方取消/TTL 过期 60 分钟）；§6.4 同步更新 key 定义和清理时机 |
| 10.[一般] 分诊场景缺少 AI 连续失败 3 次的兜底提示机制 | DialogueSession 新增 aiFailCount 字段（§3.1）；TriageServiceImpl 补充 AI 连续失败兜底逻辑——AI 调用失败时 aiFailCount++，成功时重置；aiFailCount >= 3 时 TriageResponse 附加 fallbackHint 字段携带"建议直接联系线下接诊窗口"文案（§3.1, §4.1）；TriageResponse 新增 fallbackHint 字段和 degraded 字段（对齐需求文档 3.4.1 degraded 输出字段）；§7 新增"AI 连续失败兜底"和"TriageResponse.degraded"设计决策 |
| 11.[一般] AuditRecord 缺少 auditSequence 和 isLatest 字段 | AuditRecord 新增 auditSequence（int，必填，同一 prescriptionOrderId 下递增）和 isLatest（boolean，必填，仅最新一条为 true）字段（§3.2）；补充同一处方多次审核时的更新逻辑——事务内先将已有 isLatest=true 的记录更新为 false，再插入新记录置 auditSequence 递增、isLatest=true；§4.2 补充多次审核场景的行为契约；§7 新增"AuditRecord 审核次序管理"设计决策 |
| 12.[一般] DosageStandard 与药品基础信息实体的关系未定义 | §8.2 补充 DosageStandard 与药品基础信息实体的显式关系说明——二者为独立实体、通过 drugCode 关联但不共享主键；admin 模块"药品字典维护"操作药品基础信息实体、"DosageStandard 管理"是独立维护入口；drugCode 与药品基础信息实体的"批准文号"字段值为同一编码（国药准字号）；§7 新增"DosageStandard 与药品基础信息实体关系"设计决策 |
| 13.[轻微] chiefComplaint 字符数约束（5–500）未定义 | DialogueCreateRequest.chiefComplaint 补充"字符数 5–500"约束（§1.3, §3.1），对齐需求文档 3.4.1"主诉文本字符数 5–500"；§4.1 分诊场景补充字符数约束 |
| 14.[轻微] chiefComplaint 与 additionalResponses 互斥/组合语义未说明 | §3.1 DialogueCreateRequest 补充互斥语义说明——对齐需求文档 3.4.1"二选一使用，不同时提供"；§4.1 补充首轮请求（chiefComplaint 必填、additionalResponses 不传）和后续轮（additionalResponses 必填、chiefComplaint 不传或忽略）的语义说明；TriageServiceImpl 补充互斥校验逻辑；§7 新增"chiefComplaint 与 additionalResponses 互斥"设计决策 |
