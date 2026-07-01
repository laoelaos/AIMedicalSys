根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 问题 1（严重）—「与前一版一致」文档参照缺陷，设计不可独立使用
文档中 5 处引用了"与前一版一致"的表述，注明内容与"前一版"一致但未在当前文档中定义，导致下游读者无法从当前文档独立理解该节内容、直接指导编码。
- **所在位置**：§8.1、§8.2、§8.3、§8.4、§9.4
- **改进建议**：逐项展开定义缺失内容——§8.1 说明"同优先级多条记录检测"的处理策略；§8.2 明确 DosageStandard.drugCode 与药品基础信息实体的关联关系；§8.3 明确每个 DosageUnitGroup 包含的具体单位列表及其组内换算系数；§8.4 展开 DosageStandard 实体的年龄/体重分级字段完整定义及五级匹配优先级流程图或伪代码；§9.4 补充 ConfigChangeLog 实体的完整字段定义。

### 问题 2（严重）—AiService 接口方法缺少正式定义，ai-api 模块契约来源不完整
文档在 §3.1–§3.4 中分散提及 AiService 的 4 个方法，在 §4.5 描述了各方法的 DTO 映射关系，但缺少一份集中的 AiService 接口成员方法签名定义。
- **所在位置**：全文（§3.1 TriageService → AiService.triage()；§3.2 PrescriptionAuditService → AiService.prescriptionCheck()；§3.3 MedicalRecordService → AiService.generateMedicalRecord()；§3.4 PrescriptionAssistService → AiService.prescriptionAssist()；§4.5 各 Converter 映射；§6.2 CompletableFuture<AiResult<T>>；§6.3 @Async / CompletableFuture.runAsync()）
- **改进建议**：在 §2.2 或新增独立章节中，给出 AiService 接口的正式方法签名定义（含方法名、参数类型、返回类型、泛型约束、异常声明）。至少应包含 triage()、prescriptionCheck()、generateMedicalRecord()、prescriptionAssist() 四个方法，并说明 AiResult<T> 的泛型参数在各方法中的具体类型绑定。

### 问题 3（一般）—DosageUnitGroup 缺少组内单位清单和换算系数表
§8.3 定义了 DosageUnitGroup 枚举的三个值，但未定义每个分组包含哪些具体单位及其组内换算系数。
- **所在位置**：§8.3 DosageUnitGroup 枚举
- **改进建议**：补充 DosageUnitGroup 的单位映射表，格式为 | 枚举值 | 包含单位 | 基准单位 | 换算系数 |，并说明跨组单位比较时统一输出 RX_ASSIST_UNIT_MISMATCH 错误码。

### 问题 4（一般）—CRITICAL 剂量告警与 BLOCK 审核阻断的隔离边界缺少一条执行路径说明
§4.2 处方提交流程的第 3 步（检查 BLOCK）和第 4 步（检查 CRITICAL）为串行执行，但文档未说明当第 3 步 BLOCK 命中后是否跳过第 4 步。
- **所在位置**：§4.2 处方提交端点行为——第 3 条（BLOCK 阻断）与第 4 条（CRITICAL 阻断）
- **改进建议**：明确以下行为之一——路径 A（先完整收集后阻断）：提交前同时检查 BLOCK 和 CRITICAL，合并返回单一 BlockResponse；路径 B（短路优先）：BLOCK 命中后直接阻断，不再检查 CRITICAL。推荐路径 A。

### 问题 5（一般）—特殊人群剂量规则（SpecialPopulationDosageRule）未纳入核心抽象一览表
§1.3 包D-AI1 核心抽象一览表列出了 ContraindicationCheckRule，但未列出 DuplicateCheckRule、DosageLimitRule、SpecialPopulationDosageRule 等本地规则类。
- **所在位置**：§1.3 核心抽象一览表；§3.2 LocalRuleEngine 实现范围表
- **改进建议**：在 §1.3 核心抽象一览表的 LocalRuleEngine 条目下，以列表形式补充说明 Phase 2/3 覆盖的 5 条本地规则，或新增一行汇总说明。

### 问题 6（一般）—文档缺失 ai-api 层实现前提说明：当前 ai-api DTO 为空壳类的状态与扩展时序
§10 开头注明了 ai-api 层 DTO 当前为空壳类，但未说明 ai-api 层 DTO 扩展与业务模块开发的时序依赖关系。
- **所在位置**：§10 ai-api 层 DTO 扩展规格——开篇说明
- **改进建议**：补充 ai-api 层 DTO 扩展的完成状态、业务模块开发与 ai-api DTO 扩展的依赖关系，若 ai-api 模块由独立团队维护则标注接口冻结时间点。

## 历史迭代回顾

### 已解决的问题
以下 12 轮反馈中记录的问题在本轮诊断报告中不再提及，视为已解决：
- 首轮 10 个问题（DialogueSession 不可变矛盾、异步 AI 消费路径、对话历史维护责任、DosageCheckRequest 缺少给药途径、DosageStandard 写权限归属、分诊规则生效机制、科室模板 CRUD 兜底、会话内存重启场景、common-module-api 依赖缺失、剂量标准初始化方案）
- 第 2 轮 5 个问题（BLOCK 强制阻断机制、AuditRecord 处方关联标识、AiSuggestionResult 重启丢失、DosageStandard 年龄体重分级、病历生成降级策略）
- 第 3 轮 6 个问题（AuditRecord 降级落库、分诊降级链路完整、TriageResponse 字段、剂量单位转换规则、MedicalRecord 实体字段、配置变更审计溯源）
- 第 4 轮 9 个问题（AiSuggestionResult FAILED 状态、TriageRequest DTO 冗余、关键 DTO 缺失定义、DosageThresholdService 未命中行为、DosageAlert 枚举、check-dose frequency 字段、MedicalRecord visitId、PrescriptionAssistResponse 字段、AuditRiskLevel 映射说明）
- 第 5 轮 14 个问题（AiSuggestionResult 并发竞争、包E check-dose 脱节、TriageResponse 缺失字段、AuditRequest 字段不完整、visitId 映射、WARN 留痕字段、sessionId 生成策略、session_id 必填矛盾、CRITICAL 联动触发、数据实体缺失、AI 无结果判定边界、AI 超时阈值、AuditResponse 字段缺失、TriageRecord 字段定义）
- 第 6 轮 12 个问题（AuditResponse issues/alerts 命名、ruleSetId 字段、matchedRules 字段、RecommendedDepartment DTO、流式输出模式、stream 字段、MedicalRecordField 映射、本地规则检查项集扩展、PrescriptionDraftContext key 定义、AI 连续失败兜底、auditSequence/isLatest 字段、DosageStandard 实体关系）
- 第 7 轮 18 个问题（ai-api DTO 缺口、DTO 命名冲突、allergy_details 扩展容器、AI 无推荐场景、AI 上下文传递策略、推荐医生机制、TriageRecord 写入时机、本地规则聚合逻辑、WARN 强制提交交互、超时部分保留、WARN 时序竞态、MedicalRecord 存储形式、配置变更事务边界、session_id 对齐、DrugInteractionPair/DrugCompositionDict 持久化、TriageRule 匹配模型、处方提交端点边界、ConcurrentHashMap 水平扩展）
- 第 8 轮 16 个问题（DoctorFacade 包位置、matched_rules 子结构、ContraindicationCheckRule、AiResult partialData、TriageRecord 推荐医生、AuditAlert.severity 类型、DosageAlert errorCode、处方版本校验语义、allergyHistory/allergyDetails 语义、辅助开方与审核过敏检查重叠、RegistrationEvent 契约、degraded 前端行为、迁移成本论证、错误码命名规则、TTL 清理竞态、错误码遗漏）
- 第 9 轮 7 个问题（医生确认流程、规则管理接口、事件消费补偿、PrescriptionAssistResponse errorCode、AllergyWarningItem.severity、encounterId/visitId 映射、初始模板数据集）
- 第 10 轮 7 个问题（CRITICAL 阻断链路、submit 端点归属、DosageAlert warningType、AllergyWarningItem 命名、DoseWarning DTO、dialogueText 字符约束、Phase5 迁移断言）
- 第 11 轮 6 个问题（check-dose prescriptionId、AdditionalResponse 字段、TriageRule conditions JSON、全量拼接 token 超限、错误码命名违规、全量降级前端区分）

### 持续存在的问题
以下 6 个问题在上一轮（第 12 轮）审查中已被指出，本轮审查再次发现，属于持续存在的问题，需重点解决：
1. 「与前一版一致」文档参照缺陷——已连续 2 轮反馈（第 12 轮问题 1 → 本轮问题 1），需逐项展开定义
2. AiService 接口方法缺少正式定义——已连续 2 轮反馈（第 12 轮问题 2 → 本轮问题 2），需补充集中式方法签名定义
3. DosageUnitGroup 缺少组内换算系数表——已连续 2 轮反馈（第 12 轮问题 3 → 本轮问题 3），需补充单位映射表
4. CRITICAL/BLOCK 隔离边界执行路径——已连续 2 轮反馈（第 12 轮问题 4 → 本轮问题 4），需明确行为路径
5. SpecialPopulationDosageRule 未纳入核心抽象——已连续 2 轮反馈（第 12 轮问题 5 → 本轮问题 5），需补充规则列表
6. ai-api 层实现前提说明缺失——已连续 2 轮反馈（第 12 轮问题 6 → 本轮问题 6），需补充扩展时序说明

### 新发现的问题
本轮无新发现的问题。

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281422_phase23_ood\a_v12_copy_from_v11.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281422_phase23_ood\requirement.md
