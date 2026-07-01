# 再审议判定报告（v7）

## 判定结果

RETRY

## 判定理由

组件B诊断报告识别出 18 个质量问题，其中 6 个为严重等级（问题1/2/3/4/6/9）、12 个为一般等级（问题5/7/8/10/11/12/13/14/15/16/17/18）、0 个轻微等级。质询报告状态为 LOCATED（实际轮次1 < 最大轮次12，提前确认），对全部18个问题的证据充分性、逻辑完整性、覆盖完备性审查均为"通过"，确认问题判定准确、建议可行。

严重等级问题直接阻塞编码实现：ai-api层DTO与业务层DTO间字段级契约缺口及映射机制缺失（问题1/2）、allergy_details扩展容器未纳入与需求脱节（问题3）、RX_ASSIST_AI_NO_RECOMMENDATION错误码消费场景未定义（问题4）、多轮分诊上下文传递到AiService的方式未定义（问题6）、推荐医生数据来源和跨模块获取机制未定义（问题9）。这些缺口将导致开发者无法确定AiService调用的入参出参结构和异常场景处理路径。

根据判定标准，审查报告包含严重和一般等级问题，判定为RETRY。

## 需要解决的问题

- **问题描述**：ai-api 层 DTO 与业务层 DTO 之间存在严重的字段级契约缺口，设计未定义二者间的映射/转换机制
- **所在位置**：§1.2 AiService接口描述 + §3.1/3.2/3.3/3.4 各Service协作描述 + §2.2 "与 AI 模块的协作关系"
- **严重程度**：严重
- **改进建议**：定义ai-api层DTO扩展策略，补充完整字段定义与需求文档对齐；明确业务层DTO与ai-api层DTO的转换规则和转换责任归属

- **问题描述**：PrescriptionAssistRequest（ai-api层）与业务层 PrescriptionAssistRequest（assist DTO）同名且职责不同，设计未区分
- **所在位置**：§3.4 PrescriptionAssistService + §2.2 "与 AI 模块的协作关系" + AiService 接口签名
- **严重程度**：严重
- **改进建议**：明确区分ai-api层DTO与业务层DTO的字段差异和命名空间关系；补充ai-api层DTO完整字段定义或复用策略

- **问题描述**：allergy_details 扩展容器完全未纳入设计，与需求文档过敏信息扩展性方案脱节
- **所在位置**：§3.2 AuditRequest.patientInfo + §3.4 PrescriptionAssistRequest.patientInfo + §3.2 AllergyCheckRule
- **严重程度**：严重
- **改进建议**：在patientInfo中增加allergyDetails可选字段；AllergyCheckRule存在时优先按结构化过敏信息做精确匹配

- **问题描述**：RX_ASSIST_AI_NO_RECOMMENDATION 错误码已定义但消费场景未描述，/assist主端点AI返回无可推荐药品时的行为不明确
- **所在位置**：§3.4 PrescriptionAssistService + §4.4 辅助开方场景 + §5.1 错误码表
- **严重程度**：严重
- **改进建议**：在/assist主端点流程中补充"AI返回无可推荐药品"场景的处理路径

- **问题描述**：多轮分诊场景下TriageServiceImpl如何将DialogueSession的上下文传递给AiService.triage()未定义
- **所在位置**：§3.1 TriageService协作描述 + §4.1 智能分诊场景 + AiService.triage()签名
- **严重程度**：严重
- **改进建议**：补充AiService.triage()的调用组装说明，扩展ai-api层TriageRequest增加additionalResponses字段，明确全量拼接策略

- **问题描述**：推荐医生的推荐机制和数据来源未定义，RecommendedDoctor的score计算和availableSlotCount获取路径未说明
- **所在位置**：§3.1 TriageResponse.doctors + RecommendedDoctor + §2.2 依赖规则 + §4.1
- **严重程度**：严重
- **改进建议**：明确推荐医生列表生成机制；若需跨模块查询则补充cross-module数据获取机制

- **问题描述**：TriageRecord写入时机和触发方未在行为契约中体现，finalDepartmentId赋值时机不明确
- **所在位置**：§3.1 TriageService + TriageRecord + §4.1 智能分诊场景
- **严重程度**：一般
- **改进建议**：在TriageService接口职责中明确TriageRecord写入步骤和时机；说明finalDepartmentId的补充写入机制

- **问题描述**：本地规则聚合逻辑缺少精确的"风险等级判定规则表"，AllergyCheckRule一律输出BLOCK无法区分严重程度
- **所在位置**：§3.2 LocalRuleEngine + LocalRuleResult聚合逻辑 + AllergyCheckRule/DosageLimitRule
- **严重程度**：一般
- **改进建议**：明确LocalRuleResult.severity类型为AuditRiskLevel；为每条规则补充severity判定细节

- **问题描述**：WARN级处方"强制提交并留痕"的前后端交互链路不完整，前端如何通知后端未定义
- **所在位置**：§3.2 AuditRecord.forceSubmitted + §4.2 WARN分支 + PrescriptionAuditController
- **严重程度**：一般
- **改进建议**：在处方提交端点增加forceSubmit参数，补充后端校验逻辑

- **问题描述**：病历生成非流式超时场景的"部分保留"行为契约缺少对AiResult降级模式的定义
- **所在位置**：§3.3 MedicalRecordService + §4.3 病历生成场景 + §5.5 AI超时配置
- **严重程度**：一般
- **改进建议**：补充非流式超时降级路径，AiResult增加partialData字段承载部分结果

- **问题描述**：WARN级处方审核与强制提交的时序竞态未防护
- **所在位置**：§3.2 AuditRecord + §4.2 WARN分支
- **严重程度**：一般
- **改进建议**：在强制提交路径中补充处方版本校验，比较当前处方内容与AuditRecord.originalPrescription

- **问题描述**：MedicalRecord实体缺少MedicalRecordField级字段存储形式定义
- **所在位置**：§3.3 MedicalRecord实体 + MedicalRecordField枚举 + MedicalRecordRepository
- **严重程度**：一般
- **改进建议**：明确病历内容存储形式为单列JSON TEXT；补充Repository查询方法列表和增量更新语义

- **问题描述**：配置变更审计日志跨模块事件传递的事务边界未定义
- **所在位置**：§9.2/§9.3 TemplateConfigChangeEvent + ConfigChangeLog + §2.2
- **严重程度**：一般
- **改进建议**：使用@TransactionalEventListener(phase=AFTER_COMMIT)；补充事件丢失补偿机制说明

- **问题描述**：consultation模块AI分诊首次调用缺少对需求文档session_id必填语义的完整对齐
- **所在位置**：§3.1 DialogueCreateRequest + §7 设计决策
- **严重程度**：一般
- **改进建议**：首轮请求也要求前端生成并传入sessionId，消除"首轮为空"特殊分支

- **问题描述**：DrugInteractionPair和DrugCompositionDict实体缺少持久化层定义
- **所在位置**：§2.1 目录结构 + §3.2 LocalRuleEngine数据来源
- **严重程度**：一般
- **改进建议**：将实体移至entity/包下；补充核心字段定义和对应Repository

- **问题描述**：分诊规则引擎的规则数据实体和TriageRule匹配模型未定义
- **所在位置**：§3.1 TriageRuleEngine + §4.1 + §7
- **严重程度**：一般
- **改进建议**：补充TriageRule实体核心字段；补充match()方法签名；说明规则版本存储位置

- **问题描述**：处方提交端点不在当前设计范围内，但BLOCK阻断和WARN留痕的端到端闭环依赖此端点
- **所在位置**：§3.2 PrescriptionAuditController + §3.4 PrescriptionAssistController + §4.2
- **严重程度**：一般
- **改进建议**：显式标注处方提交端点的设计边界；若不在范围内则补充待办标注

- **问题描述**：ConcurrentHashMap跨实例不共享，水平扩展时session丢失
- **所在位置**：§6.1 + §6.3 + §6.4 + §7
- **严重程度**：一般
- **改进建议**：补充部署约束说明（单实例或sticky session）；标注Phase 5迁移节点
