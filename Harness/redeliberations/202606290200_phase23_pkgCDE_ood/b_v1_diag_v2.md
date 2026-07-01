# 质量审查报告 — Phase 2/3 包CDE OOD 设计（第 1 轮重审议）

审查日期：2026-06-29  
审查范围：需求响应充分度、事实正确性、逻辑一致性、深度完整性、可落地性  
审查视角：本报告侧重内部审议（v8~v19 修订）未充分覆盖的维度，如需求响应充分度和整体落地完整性

---

## P1 [严重] RegistrationEvent 缺少 sessionId 字段，事件驱动的 finalDepartmentId 写入路径无法闭合

**问题描述**：  
§1.3 定义 RegistrationEvent 事件契约包含 registrationId、patientId、departmentId、doctorId、eventTime 五个字段，**未包含 sessionId**。§2.2 说明 RegistrationEventListener"通过 sessionId 关联找到 TriageRecord 后也调用此方法完成写入"——但 RegistrationEvent 中无 sessionId，事件监听器无法定位到对应的 TriageRecord。若通过 patientId 关联，同一患者可能多次分诊时无法确定对应关系。手动选科路径（selectDepartment）虽可覆盖降级场景，但作为设计文档核心流程之一的"挂号事件自动写入 finalDepartmentId"路径在事件契约层面即已断裂。

**所在位置**：§1.3 跨模块事件表（line 125）；§2.2 跨模块事件传递机制段（line 274）  
**严重程度**：严重 — 核心事件驱动链路在数据接口层面无法闭合，编码阶段必然发现但涉及事件契约变更（需改动 registration 模块的事件发布和 common-module-api 的事件定义）  
**改进建议**：  
1. 在 RegistrationEvent 中新增 sessionId（可选，String）字段，由 registration 模块在发布事件时从分诊上下文或请求参数获取  
2. 或重新定义映射关系：RegistrationEventListener 通过 patientId + 最新分诊时间关联 TriageRecord（需评估同一患者多次分诊的边界情况）  
3. 同步更新 §1.3 跨模块事件表字段描述和 §2.2 的写入路径说明

---

## P2 [严重] AllergyWarningSeverity 枚举值与需求文档 3.4.10 "HIGH 级别告警"语义不匹配

**问题描述**：  
需求文档 3.4.10 过敏冲突告警明确要求"给出 HIGH 级别告警"——§3.4 line 558 也引用了此要求（"severity 含 HIGH 级别，对齐需求文档 3.4.10 过敏冲突"给出 HIGH 级别告警""）。但 AllergyWarningSeverity 枚举（§3.4 line 619）定义的三个值为 INFO / WARNING / **CRITICAL**，无 HIGH。设计用 CRITICAL 承载 HIGH 语义但不相等——医学术语中 HIGH 表示严重程度（高/中/低），CRITICAL 表示危急程度（严重/警告/信息），二者并非同一分类体系。设计文档未说明 CRITICAL 与 HIGH 之间的映射关系。

**所在位置**：§3.4 AllergyWarningSeverity 定义（line 619）；§3.4 PrescriptionAssistService 职责描述（line 558）  
**严重程度**：严重 — 需求追溯链断裂，验收时无法对齐"ALLERGY_WARNINGS.SEVERITY=HIGH"的需求断言  
**改进建议**：  
1. 将 AllergyWarningSeverity 枚举值由 INFO/WARNING/CRITICAL 改为 INFO/WARNING/HIGH，与需求文档 3.4.10 的术语一致  
2. 或在 §3.4 显式声明 CRITICAL 等价于需求文档的 HIGH 并说明理由，同时建议未来版本统一术语

---

## P3 [重要] §3.4 DosageThresholdService 匹配优先级层级标注与 §8.4 不一致（滚动自上一轮审查 P3）

**问题描述**：  
§3.4 line 577 写"四级均未命中"并列出"精确匹配→年龄范围匹配→体重范围匹配→无分级默认阈值→标准不存在"共 4 级匹配策略。§8.4 line 1087-1094 已扩展为 6 级匹配优先级（v16 修订新增了 Level 2"同时范围匹配"）。§3.4 表述遗漏了 Level 2，且"四级均未命中"与 §8.4 的 5 级匹配（不含"标准不存在"）不一致。

**所在位置**：§3.4 DosageThresholdService 描述（line 577）  
**严重程度**：重要 — 实现者按 §3.4 编码会遗漏"同时范围匹配"这一优先级层级  
**改进建议**：将 §3.4 的匹配优先级序列同步更新为 §8.4 的 6 级描述，删除"四级均未命中"表述，改为引用 §8.4 的完整定义

---

## P4 [重要] PrescriptionAssistService 中过敏冲突检查的实现归属未定义

**问题描述**：  
§3.4 和 §4.4 均提到辅助开方主端点流程中会进行"过敏冲突本地检查"（"本地即时校验: 过敏冲突检查（patientInfo.allergyDetails 优先 / allergyHistory 回退 + prescriptionDraft.drugs 交叉比对）"）。但文档未说明此检查是复用 PrescriptionAuditService 的 AllergyCheckRule 规则类，还是在 PrescriptionAssistServiceImpl 中独立实现。复查方式选择直接影响代码复用度和维护一致性——复用则两个模块共享同一份过敏匹配逻辑（确保审核和辅助开方的过敏检查结果一致），独立实现则存在逻辑分叉风险。

**所在位置**：§3.4 PrescriptionAssistService 职责描述（line 556）；§4.4 辅助开方主端点流程（line 781）  
**严重程度**：重要 — 影响编码阶段的实现选择和测试策略设计  
**改进建议**：明确说明过敏冲突检查的实现方式——建议复用 AllergyCheckRule（同模块内直接引用，无跨模块依赖），或在 PrescriptionAssistServiceImpl 中独立实现时说明理由及与 AllergyCheckRule 的差异

---

## P5 [重要] encounterId → visitId 转换未定义具体实现路径

**问题描述**：  
§3.3 RecordGenerateRequest 说明"encounterId（此字段映射为 MedicalRecord.visitId——Service 层在生成病历记录时执行 encounterId → visitId 转换，确保 encounter_id 与 visit 概念的一致性）"。文档仅声明了转换的需求（encounterId → visitId），但未定义具体实现方式——是通过远程服务调用查询就诊模块获取 visitId、是同一概念的两套命名无需转换仅需字段重命名、还是通过某 Repository 映射表查询？转换路径的缺失影响 MedicalRecordService 实现者的设计判断，也可能引入对就诊模块未声明的编译期或运行时依赖。

**所在位置**：§3.3 RecordGenerateRequest 条目（line 544）；§3.3 MedicalRecord 实体描述（line 512）  
**严重程度**：重要 — 转换实现方式影响模块依赖关系和 Service 代码结构  
**改进建议**：补充具体转换策略，例如：(a) 若 encounterId 与 visitId 为同一标识的不同命名——在 RecordGenerateRequest DTO 中直接将 encounterId 映射为 visitId，无需额外转换；(b) 若需查询——定义跨模块门面接口 VisitFacade（在 common-module-api 中）并提供 findVisitIdByEncounterId() 方法；(c) 标注对就诊模块的依赖关系

---

## P6 [重要] DialogueSession 状态更新与 TriageRecord 持久化缺少事务一致性保障

**问题描述**：  
§4.1 分诊流程中，DialogueSession（内存 ConcurrentHashMap）在 AiService 调用前后被更新（如 aiFailCount 递增、QA 历史追加、correctedChiefComplaint 写入等），随后 TriageRecord 同步写入数据库。如果数据库写入失败（如网络故障、主键冲突），DialogueSession 已被修改而 TriageRecord 写入未成功。用户重试时，DialogueSession 中的 aiFailCount 和 QA 历史处于不一致状态（例如 aiFailCount 已计次但 TriageRecord 未持久化，重试时可能误判连续失败次数）。文档未讨论此场景的回滚或补偿策略。

**所在位置**：§3.1 TriageRecord 写入时机描述（line 343）；§4.1 分诊持久化说明（line 699-700）  
**严重程度**：重要 — 数据库写入失败时系统进入不一致状态，重试可能导致下游数据重复或错误计数  
**改进建议**：  
1. 定义"先写 TriageRecord（数据库），再更新 DialogueSession（内存）"的写入顺序——数据库写入成功后再更新内存状态，失败时内存不受影响  
2. 或补充说明业务接受的数据丢失窗口（如 TriageRecord 写入失败时不返回错误，仅记录日志，由补偿任务兜底）

---

## P7 [一般] §1.1 Phase 5 迁移"Service 代码无须修改"断言与 §6.1 内存存储迁移需求矛盾

**问题描述**：  
§1.1 设计目标声明"若 Phase 5 保持 AiService 接口签名不变，业务模块的核心 Service 和 Controller 代码无须修改"。但 §6.1 明确说明三项内存存储（DialogueSessionManager 的 ConcurrentHashMap、AiSuggestionResult 存储、PrescriptionDraftContext 的 ConcurrentHashMap）在 Phase 5 需替换为 Redis 分布式缓存。这三项存储的引用关系已嵌入 Service 实现代码中（如 TriageServiceImpl 注入 DialogueSessionManager、DosageThresholdService 注入 PrescriptionDraftContext），迁移时 Service 实现代码必然需要修改以替换内存存储为 Redis 客户端。§1.1 的"代码无须修改"断言存在误导性。

**所在位置**：§1.1 设计目标（line 9）；§6.1 部署约束说明（line 955-959）  
**严重程度**：一般 — §6.1 已显式说明迁移需求，但 §1.1 的无条件断言会误导高层决策者  
**改进建议**：将 §1.1 的"核心 Service 和 Controller 代码无须修改"改为"核心 Service 的业务逻辑代码无须修改，但内存存储依赖部分需替换为分布式缓存适配层，建议在 Service 实现中引入 Store 抽象层以隔离存储实现"

---

## P8 [一般] §1.3 LocalRuleEngine 规则计数与 §3.2 不一致（滚动自上一轮审查 P7）

**问题描述**：  
§1.3 包D-AI1 核心抽象表（line 72）写"封装 6 条独立规则"，列举中包含了 DrugInteractionRule（DDI，Phase 2/3 预留骨架）。§3.2（line 452-454）列举 5 条规则，且 line 475 明确说明"DrugInteractionRule（DDI）不在 Phase 2/3 本地规则范围内"。§1.3 未标注 DrugInteractionRule 为预留，读者可能误认为 6 条规则均参与 Phase 2/3 运行时。

**所在位置**：§1.3 LocalRuleEngine 条目（line 72）；§3.2 实现范围说明（line 475）  
**严重程度**：一般 — 细读可理解，但 §1.3 作为核心抽象一览表具有索引性质，不一致会影响快速查阅  
**改进建议**：将 §1.3 的"封装 6 条独立规则"改为"封装 5 条运行时规则 + 1 条预留骨架（DrugInteractionRule，Phase 2/3 不启用）"

---

## P9 [一般] §4.2 步①与步②的 CRITICAL/BLOCK 阻断竞态仅描述响应策略，未提供防护手段（滚动自上一轮审查 P9）

**问题描述**：  
§4.2（line 744）识别了"步①判定为无 CRITICAL 后并发写入 CRITICAL"的竞态场景，但仅描述了合并阻断的响应策略（"响应中同时包含剂量告警阻断原因和审核阻断原因"），未提供任何实质防护手段。CRITICAL 状态在步①检查通过后到步②执行前可能被并发写入，导致步①的"通过"结论失效。文档未说明是否接受此竞态风险，也未给出明确的防护方向。

**所在位置**：§4.2 阻断合并语义段（line 744）  
**严重程度**：一般 — 单实例同步环境下概率低，但作为设计文档未给出明确的接受风险声明或防护方向  
**改进建议**：补充说明 (a) 采用快照比较——将步①检查时的 CRITICAL 列表快照传入后续步骤；或 (b) 在步②后增加二次 CRITICAL 验证——若步②通过后 CRITICAL 状态变化则触发阻断；或 (c) 明确声明此竞态风险在设计层面被接受，并说明理由

---

## P10 [轻微] §1.3 PrescriptionAssistResponse 缺少 errorCode 字段定义

**问题描述**：  
§3.4（line 615）PrescriptionAssistResponse 包含"顶层错误码（errorCode，可选，String）"字段，并说明 AI 返回无可推荐药品时填充 RX_ASSIST_AI_NO_RECOMMENDATION。但 §1.3 包E 核心抽象表（line 115-116）对 PrescriptionAssistResponse 的字段描述末包含此 errorCode 字段。导致 §1.3 作为快速索引表的信息不完整。

**所在位置**：§1.3 PrescriptionAssistResponse 条目（line 115-116）  
**严重程度**：轻微 — 不影响实现决策，但降低核心抽象一览表的索引价值  
**改进建议**：在 §1.3 PrescriptionAssistResponse 条目中补充 errorCode（可选，String）字段说明

---

## 需求响应充分度验证（逐项对照）

| 需求项 | 预期行为 | 设计响应位置 | 充分度评估 |
|--------|---------|-------------|-----------|
| 包C：单轮/多轮双对话 | DialogueSession 管理多轮状态，首轮/后续轮请求处理 | §1.3/§3.1/§4.1 | 充分 |
| 包C：规则可配置 | 数据库规则源 + 热加载 + 管理端 CRUD | §1.3/§3.1/§9.3 | 充分 |
| 包C：Mock 兜底回退科室列表 | 静态兜底科室列表 | §1.3/§3.1 | 充分 |
| 包D-AI1：风险等级差异化阻断 | PASS/WARN/BLOCK 三级处理（建议/留痕/阻断） | §1.3/§3.2/§4.2 | 充分 |
| 包D-AI1：AI 超时回退本地规则校验打标 | LocalRuleEngine + fromFallback 标记 | §1.3/§3.2/§4.2 | 充分 |
| 包D-AI2：对话转结构化病历 | MedicalRecordService + MedicalRecordField 7 个顶层字段 | §1.3/§3.3/§4.3 | 充分 |
| 包D-AI2：按科室配置规则 | DepartmentTemplateConfig + TemplateConfigManager + DEFAULT 兜底 | §1.3/§3.3/§9.1 | 充分 |
| 包D-AI2：关键字段缺失提示补全 | MissingFieldDetector + FieldMissingHint | §1.3/§3.3/§4.3 | 充分 |
| 包E：剂量阈值告警 | DosageThresholdService + 六级匹配 + 三种告警类型 | §1.3/§3.4/§8.4 | 充分 |
| 包E：与处方审核强耦合同步落地 | 同一模块 prescription/，共享 DosageStandard 实体 | §1.1/§2.1/§2.2 | 充分 |
| 约束：所有包直接落地底座 | 直接 Maven 模块，无独立接入层 | §1.1/§2.1 | 充分 |
| 约束：与处方审核强耦合同步落地 | 同模块、同发布 | §1.1/§2.2 | 充分 |

**需求响应总体评价**：12 项需求/约束全部获得显式设计响应，无需求遗漏。但 **RegistrationEvent 缺少 sessionId（P1）** 和 **AllergyWarningSeverity 枚举值不匹配（P2）** 属于已响应但未正确落地的需求缺陷，需在后续修订中修正。

---

## 整体评价

该 OOD 设计文档经历了多轮内部审议（v8~v19），在模块划分、核心抽象、API 契约和行为流程方面完整度较高，多数需求已获得充分响应。主要剩余问题集中在以下三类：

1. **需求响应落地裂隙（P1、P2）**：需求功能点已被识别并设计响应，但实现路径在契约层面存在断裂——RegistrationEvent 缺少 sessionId 导致事件驱动写入路径无法闭合；AllergyWarningSeverity 枚举值 CRITICAL 与需求指定的 HIGH 语义不匹配。这两类问题需在修订中优先修复。

2. **设计深度不足（P4、P5、P6）**：三个关键实现路径（过敏检查复用关系、encounterId 转换方式、内存 vs 数据库一致性）仅有声明性描述，缺少可指导编码的具体实现策略。

3. **内部一致性残留（P3、P8）**：两处跨章节表述不一致（匹配优先级层级数、规则计数）在前序审议中已标识但未完全修正。

修复 P1/P2 后，该文档即可进入编码阶段。P4/P5/P6 建议在编码启动前补充明确，以避免实现阶段的反复。

---

## 修订说明（v2）

| 质询意见 | 回应 |
|---------|------|
| （首轮审查，无质询输入） | - |
