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
| DepartmentFallbackProvider | interface | 兜底科室列表提供者契约，当 AI 不可用时返回静态部门列表或基于简单规则的匹配结果 |

#### 包D-AI1（处方审核）

| 抽象 | 类型形态 | 职责定位 |
|------|---------|---------|
| PrescriptionAuditController | class | 处方审核 REST 端点，接收待审核处方，返回审核结果与风险等级 |
| PrescriptionAuditService | interface | 处方审核业务契约，协调 AI 审核调用与本地规则回退 |
| AuditRiskLevel | enum | 风险等级枚举，定义 PASS（通过）、WARN（警告）、BLOCK（阻断）三个级别，决定前端行为 |
| AuditRecord | JPA @Entity | 审核记录持久化实体，保存每次审核的原始处方、AI 结果、风险等级和时间戳 |
| LocalRuleEngine | interface | 本地规则校验引擎契约，封装药物相互作用、过敏检查、剂量上限等规则；AI 超时或不可用时作为降级回退 |

#### 包D-AI2（病历生成）

| 抽象 | 类型形态 | 职责定位 |
|------|---------|---------|
| MedicalRecordController | class | 病历生成 REST 端点，接收医生端对话文本，返回结构化病历 |
| MedicalRecordService | interface | 病历生成业务契约，协调 AI 结构化输出与科室模板配置 |
| DepartmentTemplateConfig | class | 科室模板配置值对象，按科室标识管理病历生成规则和模板版本 |
| TemplateConfigManager | interface | 科室模板配置管理器契约，支持模板的运行时查询、缓存和热加载；科室标识不存在时兜底返回 DEFAULT 模板 |
| MissingFieldDetector | interface | 关键字段缺失检测器契约，识别 AI 输出中缺失的必填字段并生成补全提示 |

#### 包E（辅助开方）

| 抽象 | 类型形态 | 职责定位 |
|------|---------|---------|
| PrescriptionAssistController | class | 辅助开方 REST 端点，接收医生开方过程中的药品、剂量和给药途径信息，返回剂量告警和合理性建议；提供异步 AI 建议查询端点 |
| PrescriptionAssistService | interface | 辅助开方业务契约，剂量阈值检查与处方合理性分析 |
| DosageThresholdService | class | 剂量阈值校验服务，按药品编码、给药途径、年龄、体重维度检查剂量是否超限 |
| DosageAlert | class | 剂量告警值对象，封装告警级别、告警消息和建议调整值 |
| AiSuggestionResult | class | AI 合理性建议结果值对象，封装异步 AI 调用的建议内容、状态和时间戳 |

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
│       │   ├── audit/AuditRequest.java, AuditResponse.java, AuditIssue.java
│       │   └── assist/
│       │       ├── DosageCheckRequest.java  # 含给药途径参数
│       │       ├── DosageAlert.java
│       │       ├── AiSuggestionResult.java  # 新增：AI 建议结果
│       │       └── PrescriptionAssistResponse.java # 含 taskId
│       ├── service/
│       │   ├── audit/PrescriptionAuditService.java + impl/
│       │   ├── audit/AuditRiskLevel.java
│       │   └── assist/PrescriptionAssistService.java + impl/
│       │   └── assist/DosageThresholdService.java # 含单位一致性校验
│       ├── rule/
│       │   ├── LocalRuleEngine.java
│       │   ├── DrugInteractionRule.java, AllergyCheckRule.java, DosageLimitRule.java
│       │   └── LocalRuleResult.java
│       ├── repository/
│       │   ├── AuditRecordRepository.java
│       │   └── DosageStandardRepository.java # 只读，继承 Repository 非 JpaRepository
│       ├── entity/AuditRecord.java
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

backend/common/src/main/java/com/aimedical/common/
└── entity/DosageStandard.java               # 跨模块共享，admin 写入，prescription 只读
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

协作对象：被 TriageController 调用（前端仅发送 sessionId 和新内容）；委托 AiService.triage()、TriageRuleEngine、DepartmentFallbackProvider；管理 DialogueSessionManager。

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

### 3.2 包D-AI1：处方审核

#### PrescriptionAuditService（interface）

职责：处方审核核心流程：接收处方→调用 AI→AI 不可用时回退本地规则→返回风险等级。

协作：委托 AiService.prescriptionCheck()、LocalRuleEngine；写入 AuditRecord。

#### AuditRiskLevel（enum）

PASS / WARN / BLOCK，固定有限分类。

#### AuditRecord（JPA @Entity）

持久化审核全部元数据。

#### LocalRuleEngine（interface）

封装 DrugInteractionRule、AllergyCheckRule、DosageLimitRule 等独立规则。每条规则独立实现/测试/启用/禁用。

### 3.3 包D-AI2：病历生成

#### MedicalRecordService（interface）

对话文本→结构化病历。委托 AiService.generateMedicalRecord()、TemplateConfigManager（科室不存在时兜底 DEFAULT）、MissingFieldDetector。

#### DepartmentTemplateConfig（class）

科室模板配置值对象（科室标识、版本号、字段映射、必填字段、Prompt 模板）。

#### TemplateConfigManager（interface）

getTemplate(departmentId)：优先返回指定科室模板，不存在时返回 DEFAULT 兜底。Caffeine refreshAfterWrite + 事件驱动缓存失效。

#### MissingFieldDetector（interface）

检测 AI 输出是否缺少必填字段，生成补全提示。

### 3.4 包E：辅助开方

#### PrescriptionAssistService（interface）

剂量阈值检查 + 异步 AI 建议查询。委托 DosageThresholdService、AiService.prescriptionAssist()（异步）。check-dose 响应返回 taskId，前端通过 GET /api/prescription/assist/suggestion/{taskId} 查询 AI 建议。

#### DosageThresholdService（class）

按 drugCode + routeOfAdministration 查询 DosageStandard，比较剂量阈值。内含单位一致性校验（可转换则自动转换，不可转换则返回错误码）。

#### DosageAlert（class）

告警信息值对象（级别、消息、药品编码、当前剂量、建议值）。

#### AiSuggestionResult（class）

异步 AI 结果值对象（taskId、suggestion、status、createTime）。

---

## 4. 关键行为契约

### 4.1 智能分诊场景

```
POST /api/triage/consult
单轮: { chiefComplaint: "胸痛伴气短" }
多轮: { sessionId: "xxx", chiefComplaint: "胸痛" }  // 前端仅发 sessionId 和新内容

正常: AiService.triage(session context) → 推荐科室
AI 空: TriageRuleEngine.match() → 规则匹配科室
AI 不可用: DepartmentFallbackProvider.getFallbackDepartments() → 静态兜底
Session 超时: TRIAGE_SESSION_EXPIRED 错误
```

### 4.2 处方审核场景

```
POST /api/prescription/audit
正常: AiService.prescriptionCheck() → AuditRecord 持久化 → 返回风险等级
降级: LocalRuleEngine.check()（DrugInteraction → AllergyCheck → DosageLimit）
```

### 4.3 病历生成场景

```
POST /api/medical-record/generate
正常: TemplateConfigManager.getTemplate → AiService.generateMedicalRecord → MissingFieldDetector
兜底: 科室不存在时使用 DEFAULT 模板
降级: 返回空病历框架 + 全字段缺失
```

### 4.4 辅助开方场景

```
POST /api/prescription/assist/check-dose
{ drugCode, dosage, unit, routeOfAdministration, patientAge, patientWeight }
→ 单位一致性校验 → DosageThresholdService.check(drugCode, route, dosage, info)
→ 异步 AiService.prescriptionAssist() → 返回 { alerts, taskId }

GET /api/prescription/assist/suggestion/{taskId}
→ AiSuggestionResult { taskId, suggestion, status, createTime }
```

---

## 5. 错误处理策略

### 5.1 模块级错误码

| 错误类别 | 前缀 | 代表场景 |
|---------|------|---------|
| 分诊 | TRIAGE_ | TRIAGE_SESSION_EXPIRED、参数缺失 |
| 审核 | RX_AUDIT_ | 处方格式不合法、药品编码不存在 |
| 病历 | MR_ | 对话文本过短、科室模板不存在 |
| 开方辅助 | RX_ASSIST_ | 剂量标准不存在、单位不匹配 |

### 5.2 AI 降级作为正常业务流程

各模块的本地规则回退视为正常业务流程，响应体通过 fromFallback 标记告知前端。

### 5.3 与已有异常处理框架的集成

复用 GlobalExceptionHandler + ErrorCode 接口 + BusinessException 体系。

---

## 6. 并发设计

### 6.1 对话会话并发管理

ConcurrentHashMap 存储，同 session 请求串行（前端等待响应），不同 session 独立。ScheduledExecutorService 每 5 分钟扫描清理超时 30 分钟的 session。

### 6.2 AI 调用并发

统一 CompletableFuture<AiResult<T>>，Service 层同步等待 AI 结果。

### 6.3 包E 的异步 AI 建议

@Async / CompletableFuture.runAsync() 调用 AiService.prescriptionAssist()。主响应返回 taskId。AI 建议暂存 ConcurrentHashMap<String, AiSuggestionResult>。前端通过 GET /api/prescription/assist/suggestion/{taskId} 查询。

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
| MissingFieldDetector | 检测报告模式 | 避免自动补全引入错误 |
| DosageStandard 位置 | common 模块 | 避免跨模块编译期依赖 |
| 科室模板兜底 | DEFAULT 兜底 | 新科室未配置时仍可生成基础病历 |
| 模板缓存刷新 | 定时刷新 + 事件驱动失效 | 最终一致性，管理更新可立即生效 |

---

## 8. 剂量标准初始化与药品编码规范

### 8.1 初始化方案

SQL 种子脚本：backend/modules/prescription/src/main/resources/db/seed/R__dosage_standards.sql
使用 MERGE / INSERT ... ON DUPLICATE KEY UPDATE 实现幂等执行。

### 8.2 药品编码规范

DosageStandard.drugCode 采用国药准字号（如 H109601234）。

### 8.3 单位一致性校验

DosageThresholdService 校验前比对单位。可转换（mg↔g）则自动转换；不可转换（mg↔ml）返回 RX_ASSIST_UNIT_MISMATCH。

---

## 9. 科室模板初始数据集与模板管理接口

### 9.1 初始模板数据集

- DEFAULT：通用模板（主诉、现病史、既往史、体格检查、辅助检查、初步诊断、治疗意见）
- 最少一个示例科室模板（如 INTERNAL_MEDICINE）

### 9.2 模板管理接口定义

CRUD 由 admin 模块管理界面完成。模板更新后通过 ApplicationEventPublisher 发布 TemplateConfigChangeEvent，TemplateConfigManager 监听使缓存失效。

---

## 修订说明（v2）

| 审查意见 | 修改措施 |
|---------|---------|
| 1. DialogueSession 不可变声明与可变追加操作的逻辑矛盾 | DialogueSession 改为可变 class；DialogueSessionManager 承担并发控制 |
| 2. 包E 异步 AI 建议缺少消费路径 | 新增 GET /api/prescription/assist/suggestion/{taskId} 查询端点；新增 AiSuggestionResult DTO；check-dose 响应增加 taskId |
| 3. 多轮分诊中对话历史的维护责任与一致性不明确 | 删除 TriageRequest 中 history 字段声明；服务端 DialogueSession 为单一真相来源；前端仅发 sessionId |
| 4. DosageCheckRequest 缺少给药途径参数 | 新增 routeOfAdministration 枚举；DosageStandard 增加给药途径维度；查询键改为 drugCode + routeOfAdministration |
| 5. DosageStandard 实体的写权限归属未定义 + 实体位置矛盾 | DosageStandard 迁移至 common 模块；prescription Repository 改为只读（继承 Repository 非 JpaRepository）；admin 为唯一写入者 |
| 6. 对话会话内存存储未覆盖服务重启场景 | 明确 findOrCreate 三种分支；补充 TRIAGE_SESSION_EXPIRED 错误码；TTL 30 分钟可配置 |
| 7. 新模块依赖声明未包含 common-module-api | 三新模块均补充 common-module-api（compile scope） |
| 8. 分诊规则配置变更的生效机制未定义 | 改为 Caffeine refreshAfterWrite 定时缓存刷新（默认 60 秒） |
| 9. 科室模板配置的 CRUD 管理和默认兜底缺失 | TemplateConfigManager.getTemplate 增加 DEFAULT 兜底；新增 §9 描述初始模板数据集和管理接口；缓存增加事件驱动失效 |
| 10. 剂量标准数据初始化方案和编码规范缺失 | 新增 §8 描述初始化方案（SQL 种子脚本）、药品编码规范（国药准字号）、单位一致性校验逻辑 |
