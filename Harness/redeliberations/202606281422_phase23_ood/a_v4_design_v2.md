# Phase 2/3 包C/D-AI1/D-AI2/E — 架构级 OOD 设计方案

## 1. 概述

### 1.1 设计目标

本设计覆盖 Phase 2 包C（智能分诊）与 Phase 3 包D-AI1（处方审核）、包D-AI2（病历生成）、包E（辅助开方）四个业务包。核心目标如下：

- **底座直接落地**：四个包均直接以 Maven 模块形式落地在 AIMedical 后端底座上，遵循 common → modules → application 的分层架构，规避后续迁移至 Phase 5 AI 进阶底座的重构成本
- **架构风格一致**：四个包的模块结构、依赖方向和抽象层次与 Phase 0（骨架模块）、Phase 1（认证模块）的风格保持一致
- **强耦合同步落地**：包E（辅助开方）与包D-AI1（处方审核）共享处方领域数据和业务规则，设计为同一模块内的两个子域，同步开发、同步发布
- **AI 能力集成标准化**：四个包的业务逻辑均通过 ai-api 中的 AiService 接口调用 AI 能力，隔离 AI 实现细节；AI 不可用时的降级路径由各自模块的本地规则兜底，与 AiService 的降级框架协同

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
| TriageController | class | 分诊对话 REST 端点，接收前端提交的主诉/追问答复，返回推荐科室列表 |
| TriageService | interface | 智能分诊业务契约，封装单轮/多轮对话的分诊逻辑、对话状态管理和规则回退 |
| DialogueSession | class | 多轮对话会话状态对象，维护当前 session 的对话上下文、已收集的症状信息和当前轮次 |
| DialogueSessionManager | class | 对话会话生命周期管理器，负责 session 的创建、查找、更新和过期清理，承担并发访问控制职责 |
| TriageRuleEngine | interface | 分诊规则引擎契约，根据症状匹配规则返回推荐科室；支持可配置规则源 |
| DepartmentFallbackProvider | interface | 兜底科室列表提供者契约，当 AI 无结果且规则匹配也无结果时返回静态兜底科室列表或基于简单规则的匹配结果 |
| TriageResponse | class（DTO） | 分诊响应值对象，包含推荐科室列表（departments）、会话标识（sessionId，多轮场景）、是否需要追问标记（needFollowUp）及追问内容（followUpQuestion）、置信度confidence（可选） |
| DialogueCreateRequest | class（DTO） | 分诊对话创建请求值对象，包含患者主诉（chiefComplaint）、患者基本信息（patientId、age、gender），多轮场景复用时通过 sessionId 关联已有会话 |

#### 包D-AI1（处方审核）

| 抽象 | 类型形态 | 职责定位 |
|------|---------|---------|
| PrescriptionAuditController | class | 处方审核 REST 端点，接收待审核处方，返回审核结果与风险等级；在收到 BLOCK 结果时拒绝处方提交，通过 HTTP 422 返回阻断详情 |
| PrescriptionAuditService | interface | 处方审核业务契约，协调 AI 审核调用与本地规则回退；审计结果为最终判定，调用方需据此执行阻断决策 |
| AuditRiskLevel | enum | 风险等级枚举，定义 PASS（通过）、WARN（警告）、BLOCK（阻断）三个级别，决定前端行为；BLOCK 级别在后端具有强制阻断语义 |
| AuditRecord | JPA @Entity | 审核记录持久化实体，保存每次审核的原始处方、AI 结果、风险等级和时间戳，以及处方单号、医生 ID、患者 ID 等业务关联标识 |
| LocalRuleEngine | interface | 本地规则校验引擎契约，封装药物相互作用、过敏检查、剂量上限等规则；AI 超时或不可用时作为降级回退 |
| PrescriptionAuditEnforcer | interface | 阻断执行策略契约，定义 BLOCK 结果的强制阻断行为；默认实现返回阻断错误码和阻断原因集合，供 Controller 层组装响应 |

#### 包D-AI2（病历生成）

| 抽象 | 类型形态 | 职责定位 |
|------|---------|---------|
| MedicalRecordController | class | 病历生成 REST 端点，接收医生端对话文本，返回结构化病历 |
| MedicalRecordService | interface | 病历生成业务契约，协调 AI 结构化输出与科室模板配置 |
| DepartmentTemplateConfig | class | 科室模板配置值对象，按科室标识管理病历生成规则和模板版本 |
| TemplateConfigManager | interface | 科室模板配置管理器契约，支持模板的运行时查询、缓存和热加载；科室标识不存在时兜底返回 DEFAULT 模板 |
| MissingFieldDetector | interface | 关键字段缺失检测器契约，基于科室模板的必填字段列表与 AI 输出实际包含字段进行差集比对，输出缺失字段集合；不修改 AI 产出 |

#### 包E（辅助开方）

| 抽象 | 类型形态 | 职责定位 |
|------|---------|---------|
| PrescriptionAssistController | class | 辅助开方 REST 端点，接收医生开方过程中的药品、剂量和给药途径信息，返回剂量告警和合理性建议；提供异步 AI 建议查询端点 |
| PrescriptionAssistService | interface | 辅助开方业务契约，剂量阈值检查与处方合理性分析 |
| DosageThresholdService | class | 剂量阈值校验服务，按药品编码、给药途径、年龄范围、体重范围维度匹配剂量标准，检查剂量是否超限 |
| DosageAlert | class | 剂量告警值对象，封装告警级别、告警消息和建议调整值 |
| AiSuggestionResult | class | AI 合理性建议结果值对象，封装异步 AI 调用的建议内容、状态和时间戳；查询时支持三种状态分支：不存在（AI_SUGGESTION_NOT_FOUND）、处理中（PENDING）、已完成（COMPLETED） |

---

## 2. 模块划分

### 2.1 目录结构

```
backend/modules/
├── consultation/                            # 包C 智能分诊
│   └── src/main/java/com/aimedical/modules/consultation/
│       ├── api/TriageController.java
│       ├── dto/TriageRequest.java, TriageResponse.java, DialogueCreateRequest.java
│       ├── service/
│       │   ├── TriageService.java
│       │   └── impl/TriageServiceImpl.java
│       ├── dialogue/
│       │   ├── DialogueSession.java         # 可变，由 DialogueSessionManager 管理
│       │   └── DialogueSessionManager.java  # 生命周期管理 + 并发控制
│       ├── rule/
│       │   ├── TriageRuleEngine.java
│       │   └── DefaultTriageRuleEngine.java # 数据库规则源 + 定时缓存刷新
│       ├── fallback/
│       │   ├── DepartmentFallbackProvider.java
│       │   └── StaticDepartmentFallbackProvider.java
│       ├── repository/TriageRecordRepository.java
│       ├── entity/TriageRecord.java
│       └── converter/TriageConverter.java
│
├── prescription/                            # 包D-AI1 处方审核 + 包E 辅助开方
│   └── src/main/java/com/aimedical/modules/prescription/
│       ├── api/
│       │   ├── audit/PrescriptionAuditController.java
│       │   └── assist/PrescriptionAssistController.java
│       ├── dto/
│       │   ├── audit/AuditRequest.java, AuditResponse.java, AuditIssue.java, BlockResponse.java
│       │   └── assist/
│       │       ├── DosageCheckRequest.java  # 含给药途径参数
│       │       ├── DosageAlert.java
│       │       ├── AiSuggestionResult.java  # 新增：AI 建议结果
│       │       └── PrescriptionAssistResponse.java # 含 taskId
│       ├── service/
│       │   ├── audit/PrescriptionAuditService.java + impl/
│       │   ├── audit/AuditRiskLevel.java
│       │   ├── audit/PrescriptionAuditEnforcer.java + impl/  # 阻断执行策略
│       │   └── assist/PrescriptionAssistService.java + impl/
│       │   └── assist/DosageThresholdService.java # 含单位一致性校验、年龄体重分级支持
│       ├── rule/
│       │   ├── LocalRuleEngine.java
│       │   ├── DrugInteractionRule.java, AllergyCheckRule.java, DosageLimitRule.java
│       │   └── LocalRuleResult.java
│       ├── repository/
│       │   ├── AuditRecordRepository.java
│       │   └── DosageStandardRepository.java # 只读，继承 Repository 非 JpaRepository
│       ├── entity/AuditRecord.java           # 含 prescriptionOrderId, doctorId, patientId
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
    └── DosageStandard.java               # 跨模块共享，admin 写入，prescription 只读；含年龄范围、体重范围字段
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

协作对象：被 TriageController 调用（前端仅发送 sessionId 和新内容）；委托 AiService.triage() 进行 AI 分诊，AI 无结果时降级至 TriageRuleEngine.match()，规则匹配无结果时继续调用 DepartmentFallbackProvider.getFallbackDepartments() 获取兜底科室列表；管理 DialogueSessionManager。

为何使用 interface：分诊业务可能存在多种实现（普通分诊 vs 急诊分诊），interface 允许扩展而不影响调用方。

#### DialogueSession（class，可变）

职责：封装一次多轮分诊的完整上下文（sessionId、主诉、症状列表、追问轮次、会话状态）。

协作：被 DialogueSessionManager 创建/修改/持久化。

为何使用可变 class：每轮追问追加 QA，可变避免频繁对象复制。DialogueSessionManager 承担并发控制——同 session 请求串行，不同 session 独立。

#### DialogueSessionManager（class）

职责：管理 DialogueSession 的创建、查找、更新和过期清理。承担并发访问的唯一控制点。

协作：被 TriageService 调用。内部维护 ConcurrentHashMap + ScheduledExecutorService（TTL 30 分钟）。findOrCreate 三种分支：不存在→创建，有效→恢复，已超时→TRIAGE_SESSION_EXPIRED 错误。

为何使用 class 而非 interface：存储策略虽有变更可能，但管理器职责边界稳定，interface 抽象收益在当前阶段不抵实现复杂度。

#### TriageRuleEngine（interface）

职责：症状→科室匹配规则引擎。规则存储在数据库，支持热加载。

协作：被 TriageService 在 AI 不可用时调用。配合 Caffeine refreshAfterWrite（默认 60 秒）定时刷新规则缓存。

为何使用 interface：规则源可能从数据库演化为分布式规则引擎。

#### DepartmentFallbackProvider（interface）

职责：AI 和规则引擎均无法决策时返回静态兜底科室列表。

#### TriageResponse（class，DTO）

职责：封装分诊响应的结构化数据。包含推荐科室列表（departments）、会话标识（sessionId，多轮追问场景携带）、是否需要追问标记（needFollowUp）及追问内容（followUpQuestion）、置信度标量（confidence，可选，AI 模式时由 AiService.triage() 填充）。单轮场景 sessionId 为空。

#### DialogueCreateRequest（class，DTO）

职责：封装单轮分诊请求或新对话创建请求。包含患者主诉（chiefComplaint）、患者基本信息（patientId、age、gender）。多轮追问场景复用已有 sessionId，通过 DialogueSessionManager 恢复上下文。

### 3.2 包D-AI1：处方审核

#### PrescriptionAuditService（interface）

职责：处方审核核心流程：接收处方→调用 AI→AI 不可用时回退本地规则→返回风险等级。审计结果的 BLOCK 等级具有强制阻断语义，调用方（Controller 或 Orchestrator）必须根据 AuditRiskLevel 执行阻断决策，不得将 BLOCK 处方提交至下一环节。

协作：正常路径委托 AiService.prescriptionCheck()，降级路径委托 LocalRuleEngine；无论正常或降级路径，均在返回前写入 AuditRecord 持久化（降级路径的 AuditRecord 携带 fromFallback=true 标记）；返回结果中包含 riskLevel 和阻断原因集合。

为何使用 interface：审核策略未来可能演进（如分级审核、人工审核兜底），interface 隔离变更影响。

#### AuditRiskLevel（enum）

PASS / WARN / BLOCK，固定有限分类。BLOCK 的语义从设计层面定义为"业务强制拒绝"，不是前端展示性标记。后端必须确保 BLOCK 处方不会被提交。

#### AuditRecord（JPA @Entity）

持久化审核全部元数据，包含以下业务关联标识作为必填字段：
- prescriptionOrderId：关联的处方单号，用于追溯审核对应的处方订单
- doctorId：开方医生 ID，用于按医生维度分析审核问题分布
- patientId：患者 ID，用于按患者维度查询历史审核记录
- auditTime：审核时间戳
- fromFallback：boolean 标记，为 true 表示该审核结果来自本地规则降级而非 AI 审核，用于区分统计 AI 可用率与规则命中率
- originalPrescription、riskLevel、aiResult、auditIssues 等审核结果数据

这些关联标识使 AuditRecord 具备完整的业务可追溯性，支持按医生、患者、处方单号三个维度的查询和分析，fromFallback 标记辅助 AI 可用率统计。

#### LocalRuleEngine（interface）

封装 DrugInteractionRule、AllergyCheckRule、DosageLimitRule 等独立规则。每条规则独立实现/测试/启用/禁用。

#### PrescriptionAuditEnforcer（interface）

职责：定义 BLOCK 阻断的执行策略。当 PrescriptionAuditService 返回 AuditRiskLevel.BLOCK 时，由该组件将阻断语义转化为具体的业务阻断行为。

为何使用 interface：不同场景可能需要不同的阻断行为——某些场景需要直接拒绝提交并返回 422，某些场景可能需要记录阻断日志后转人工审核。interface 支持阻断策略切换而不影响审核核心流程。

默认阻断行为：
1. 识别并收集所有触发 BLOCK 的审核问题条目
2. 封装为 BlockResponse（含阻断原因列表、阻断码、阻断时间）
3. Controller 层将 BlockResponse 以 HTTP 422 返回前端，前端据此阻断提交流程并展示阻断原因

### 3.3 包D-AI2：病历生成

#### MedicalRecord（JPA @Entity）

职责：结构化病历持久化实体。核心字段包含：记录标识、患者 ID、就诊科室、病历内容结构化 JSON、医生 ID、创建时间。病历内容通过 MedicalRecordField 枚举标识顶层字段，每个字段对应病历中的一个语义段落。

#### MedicalRecordField（enum）

职责：定义病历结构化输出的顶层字段标识符枚举。条目包括：CHIEF_COMPLAINT（主诉）、PRESENT_ILLNESS（现病史）、PAST_HISTORY（既往史）、PHYSICAL_EXAM（体格检查）、AUXILIARY_EXAM（辅助检查）、PRELIMINARY_DIAGNOSIS（初步诊断）、TREATMENT_ADVICE（治疗意见）。此枚举是 MissingFieldDetector 差集比对和 DepartmentTemplateConfig.requiredFields 的类型基础，使字段标识符与模板内容一致。

#### RecordGenerateRequest（class，DTO）

职责：病历生成请求值对象。包含对话文本（dialogueText）、科室标识（departmentId）、患者 ID（patientId）。

#### RecordGenerateResponse（class，DTO）

职责：病历生成响应值对象。包含结构化病历内容（fields，以 MedicalRecordField 为键的键值映射）、缺失字段列表（missingFields，List<MedicalRecordField>）、生成状态标记（如降级标记 fromFallback）。

#### MedicalRecordService（interface）

对话文本→结构化病历。委托 AiService.generateMedicalRecord()、TemplateConfigManager（科室不存在时兜底 DEFAULT）、MissingFieldDetector。返回 RecordGenerateResponse。

#### DepartmentTemplateConfig（class）

科室模板配置值对象（科室标识、版本号、字段映射、必填字段列表 requiredFields 类型为 List<MedicalRecordField>、Prompt 模板）。

#### TemplateConfigManager（interface）

getTemplate(departmentId)：优先返回指定科室模板，不存在时返回 DEFAULT 兜底。Caffeine refreshAfterWrite + 事件驱动缓存失效。

#### MissingFieldDetector（interface）

职责：检测 AI 输出是否缺少必填字段。采用差集比对逻辑：取科室模板的必填字段集合与 AI 输出实际包含字段集合的差集，输出缺失字段集合。不修改 AI 产出的结构化内容。

为何使用 interface：字段缺失检测逻辑未来可能引入语义分析（如字段值虽存在但为空字符串或占位符）或按科室定制规则，interface 支持策略替换。

降级策略说明（分层保护）：
- 当 AI 完全不可用时，AI 返回空字段集（而非结构化病历）
- MedicalRecordService 调用 MissingFieldDetector 识别出所有必填字段均缺失
- 保留已提取的结构化字段（此处为空字段集，分层保护规则仍然生效），仅标记缺失字段
- 前端对现有字段正常渲染，缺失字段显示补全提示输入框
- 此策略确保在任意降级场景下，已提取的字段数据不丢失，前端只需处理缺失标记即可

### 3.4 包E：辅助开方

#### PrescriptionAssistService（interface）

剂量阈值检查 + 异步 AI 建议查询。委托 DosageThresholdService、AiService.prescriptionAssist()（异步）。check-dose 响应返回 taskId，前端通过 GET /api/prescription/assist/suggestion/{taskId} 查询 AI 建议。

#### DosageThresholdService（class）

按 drugCode + routeOfAdministration + ageRange + weightRange 查询 DosageStandard，比较剂量阈值。匹配优先级策略：精确匹配（所有维度均匹配）→ 年龄范围匹配（年龄范围匹配 + 体重不限）→ 体重范围匹配（体重范围匹配 + 年龄不限）→ 无分级默认阈值。内含单位一致性校验（可转换则自动转换，不可转换则返回错误码）。

为何使用 class 而非 interface：剂量阈值的查找与比较逻辑稳定，无多实现需求；部门内部方法较多，class 封装可隐藏内部细节。

#### DosageAlert（class）

告警信息值对象（级别、消息、药品编码、当前剂量、建议值）。

#### AiSuggestionResult（class）

异步 AI 结果值对象（taskId、suggestion、status、createTime）。查询时遵循 findOrCreate 三分支模式：
- 不存在（TTL 过期或被清理）：返回 RX_ASSIST_AI_SUGGESTION_NOT_FOUND 错误码，前端展示降级文案"AI 建议暂不可用"
- 存在且状态为 PENDING：返回 status=PENDING，前端显示"AI 建议生成中"
- 存在且状态为 COMPLETED：返回完整建议内容

AiSuggestionResult 增加 TTL（同 suggestion TTL 30 分钟），由 ScheduledExecutorService 定期清理过期条目。

---

## 4. 关键行为契约

### 4.1 智能分诊场景

```
POST /api/triage/consult
单轮: { chiefComplaint: "胸痛伴气短" }
多轮: { sessionId: "xxx", chiefComplaint: "胸痛" }  // 前端仅发 sessionId 和新内容

正常: AiService.triage(session context) → 推荐科室
降级链: AiService 无结果 → TriageRuleEngine.match() → 匹配成功返回科室
       → 规则匹配无结果 → DepartmentFallbackProvider.getFallbackDepartments() → 兜底科室
Session 超时: TRIAGE_SESSION_EXPIRED 错误
```

### 4.2 处方审核场景

```
POST /api/prescription/audit
正常: AiService.prescriptionCheck() → AuditRecord 持久化（fromFallback=false） → 返回 riskLevel + 阻断原因
降级: LocalRuleEngine.check()（DrugInteraction → AllergyCheck → DosageLimit） → AuditRecord 持久化（fromFallback=true） → 返回 riskLevel + 阻断原因

审核结果处理:
  PASS → 响应建议性审核意见，不影响提交
  WARN → 响应警告信息，前端弹窗提示医生确认
  BLOCK → PrescriptionAuditEnforcer 执行阻断 → 返回 HTTP 422 + BlockResponse
        前端弹窗展示阻断原因列表，禁止提交；后端不执行处方落单
```

### 4.3 病历生成场景

```
POST /api/medical-record/generate
正常: TemplateConfigManager.getTemplate → AiService.generateMedicalRecord → MissingFieldDetector
兜底: 科室不存在时使用 DEFAULT 模板
降级（分层保护）:
  AI 完全不可用 → 返回空字段集
  差集比对 → 仅标记缺失字段（现有字段完整保留）
  前端: 已提取字段正常渲染 + 缺失字段显示补全提示输入框
```

### 4.4 辅助开方场景

```
POST /api/prescription/assist/check-dose
{ drugCode, dosage, unit, routeOfAdministration, patientAge, patientWeight }
→ 单位一致性校验
→ DosageThresholdService.check(drugCode, route, dosage, age, weight)
   匹配优先级: 精确(drug+route+age+weight) → 年龄范围 → 体重范围 → 默认
→ 异步 AiService.prescriptionAssist() → 返回 { alerts, taskId }

GET /api/prescription/assist/suggestion/{taskId}
→ 不存在 / TTL 过期: RX_ASSIST_AI_SUGGESTION_NOT_FOUND → 前端降级文案
→ PENDING: { status: "PENDING" } → 前端"AI 建议生成中"
→ COMPLETED: { status: "COMPLETED", suggestion: "..." }
```

---

## 5. 错误处理策略

### 5.1 模块级错误码

| 错误类别 | 前缀 | 代表场景 |
|---------|------|---------|
| 分诊 | TRIAGE_ | TRIAGE_SESSION_EXPIRED、参数缺失 |
| 审核 | RX_AUDIT_ | 处方格式不合法、药品编码不存在、BLOCK 阻断 |
| 病历 | MR_ | 对话文本过短、科室模板不存在 |
| 开方辅助 | RX_ASSIST_ | 剂量标准不存在、单位不匹配、AI 建议不存在 |

### 5.2 AI 降级作为正常业务流程

各模块的本地规则回退视为正常业务流程。处方审核降级时，AuditRecord 的 fromFallback=true 标记用于区分 AI 审核与本地规则结果，响应体的 fromFallback 标记同步告知前端。病历生成降级时 RecordGenerateResponse 同样携带 fromFallback 标记。

### 5.3 BLOCK 阻断作为独立错误类别

BLOCK 阻断响应通过 BlockResponse 封装，使用 HTTP 422 状态码，与业务异常体系正交。BlockResponse 包含阻断原因列表（每个条目含字段名、问题描述、匹配规则标识），前端据此精确展示阻断原因。

### 5.4 与已有异常处理框架的集成

复用 GlobalExceptionHandler + ErrorCode 接口 + BusinessException 体系。BLOCK 阻断不经过异常框架（属于正常业务流程中的阻断分支），直接通过 Controller 返回 422 响应。

---

## 6. 并发设计

### 6.1 对话会话并发管理

ConcurrentHashMap 存储，同 session 请求串行（前端等待响应），不同 session 独立。ScheduledExecutorService 每 5 分钟扫描清理超时 30 分钟的 session。

### 6.2 AI 调用并发

统一 CompletableFuture<AiResult<T>>，Service 层同步等待 AI 结果。

### 6.3 包E 的异步 AI 建议

@Async / CompletableFuture.runAsync() 调用 AiService.prescriptionAssist()。主响应返回 taskId。AI 建议暂存 ConcurrentHashMap<String, AiSuggestionResult>，TTL 30 分钟。前端通过 GET /api/prescription/assist/suggestion/{taskId} 查询，遵循三分支模式：不存在（含 TTL 过期）→ RX_ASSIST_AI_SUGGESTION_NOT_FOUND、PENDING、COMPLETED。

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
| AiSuggestionResult 查询模式 | findOrCreate 三分支 | 与 DialogueSessionManager 模式一致，统一不存在/处理中/已完成的处理语义 |
| DosageStandard 年龄/体重分级 | 内联年龄范围+体重范围字段，四级匹配优先级 | 避免子实体增加查询复杂度；匹配优先级明确可测试 |
| 剂量单位分组 | DosageUnitGroup 枚举（MASS/VOLUME/IU），组内换算，跨组报错 | 换算规则集中定义，消除实现随意假设的风险 |
| AuditRecord 降级标记 | fromFallback boolean 字段 | 区分 AI 审核与本地规则结果，支持 AI 可用率统计 |
| 分诊降级链 | 线性三级：AI → 规则引擎 → 兜底提供者 | 覆盖 AI 空且规则空的全路径，行为确定无二义 |
| 配置变更审计 | ConfigChangeLog 实体（admin 模块）+ 事件处理 | 规则/模板变更可追溯，满足合规审计要求 |
| 病历字段定义 | MedicalRecordField 枚举定义顶层字段标识 | 字段标识与模板内容一致，缺失检测有确定枚举基础 |

---

## 8. 剂量标准初始化与药品编码规范

### 8.1 初始化方案

SQL 种子脚本：backend/modules/prescription/src/main/resources/db/seed/R__dosage_standards.sql
使用 MERGE / INSERT ... ON DUPLICATE KEY UPDATE 实现幂等执行。

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

剂量阈值查找匹配优先级（DosageThresholdService 实现）：
1. 精确匹配：drugCode + routeOfAdministration + age范围（患者年龄在范围内）+ weight范围（患者体重在范围内）
2. 年龄范围匹配：drugCode + routeOfAdministration + age范围 + weight为空（不限体重）
3. 体重范围匹配：drugCode + routeOfAdministration + age为空（不限年龄）+ weight范围
4. 默认值：drugCode + routeOfAdministration + age为空 + weight为空（无分级限制的通用剂量）

同一优先级返回多条时（如多个年龄范围段均命中），取 dosageMax 最小的条目作为最保守阈值。

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
| 6. §3.3 MedicalRecord 实体字段未定义 | §3.3 新增 MedicalRecord 实体描述、MedicalRecordField 枚举定义（7 个顶层字段标识）、RecordGenerateRequest/Response DTO 描述；DepartmentTemplateConfig.requiredFields 明确类型为 List<MedicalRecordField> |
| 7. §3.1/§3.3 规则/模板配置变更缺少审计溯源 | §9.3 新增 ConfigChangeLog 实体定义（归属 admin 模块）、写入触发点说明（TemplateConfigChangeEvent/规则变更事件处理链）、TemplateConfigChangeEvent 携带 oldTemplate/newTemplate 快照；§2.1 目录补充 admin 模块 entity/repository 条目 |
| 8. §2.1 ConfigChangeLog 目录归属与文本推荐不一致 | §2.1 目录中 ConfigChangeLog 归入 admin 模块而非 prescription 模块；§9.3 明确 admin 模块监听器通过 Spring ApplicationEvent 接收事件，medical-record 模块无需直接依赖 admin 模块 |
