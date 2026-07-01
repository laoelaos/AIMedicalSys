# 质量审查报告：a_v9_design_v1.md

## 审查范围

本次审查聚焦内部审议（设计-验证循环）未充分覆盖的维度：需求响应充分度、整体深度和完整性、实际落地可行性。不对技术实现细节、性能指标等内部审议已确认的维度重复审查。

## 发现的问题

### 问题1：辅助开方"医生确认后生效"的完整流程缺失

- **问题描述**：需求文档 3.4.10 AI 辅助开方的完整流程包含"AI 生成处方草案 → 医生审查/修改 → 医生确认签名生效"三个环节。当前设计覆盖了 AI 生成草案（POST /assist 返回 PrescriptionAssistResponse）和处方整体提交（POST /submit），但缺失"医生对 AI 建议的采纳/修改/拒绝"的明确决策机制。处方提交端点（POST /submit）是针对已确认处方的整体提交流程，并非"AI 建议确认"的专用决策通道。医生编辑期间对 AI 建议的逐项接受/拒绝/修改状态和确认记录均未定义承载实体或状态机。
- **所在位置**：§3.4 PrescriptionAssistService 协作描述；§4.4 辅助开方场景
- **严重程度**：严重
- **改进建议**：(a) 定义"医生确认"动作的承载实体（如 SuggestionConfirmation），记录每项 AI 建议（药品推荐、剂量调整）的确认状态（accepted/rejected/modified）和医生修改内容；(b) 或显式标注 Phase 2/3 仅实现"AI 生成→人工手动录入/调整"的分离模式，不实现"逐项确认"的交互流程，并在设计文本中补充此范围的线说明

### 问题2：规则配置的 admin 管理接口契约未定义，影响端到端交付

- **问题描述**：需求要求包C"规则可配置"和包D-AI2"按科室配置规则"，当前设计将规则配置 CRUD 完全交由 admin 模块承担（§3.1 TriageRuleEngine 定义数据库规则源 + 热加载，§9.2 标注"CRUD 由 admin 模块管理界面完成"），但 admin 模块侧缺乏具体的规则管理 API 契约和操作流程定义。具体缺口包括：规则集版本发布/回滚的 API 未定义、规则启用/禁用的批量操作未定义、规则变更与热加载之间的最终一致性窗口未说明、规则变更审计日志的写入端点在 admin 模块但 admin Service 方法签名未定义。这导致 Phase 2/3 范围内 admin 模块的并行开发缺乏可依据的接口契约。
- **所在位置**：§3.1 TriageRuleEngine；§9.2 模板管理接口定义部分
- **严重程度**：严重
- **改进建议**：在设计中补充 admin 模块规则管理接口的简要契约定义（至少包含：规则 CRUD 端点、规则集发布/回滚端点、版本查询端点），或显式标注规则 CRUD 接口的设计由 admin 模块 OOD 文档独立定义并建立交叉引用

### 问题3：跨模块事件传递的失败补偿机制缺失

- **问题描述**：RegistrationEvent 由 registration 模块发布、consultation 模块消费，用于补充写入 TriageRecord.finalDepartmentId。当前设计（§2.2 跨模块事件传递机制）描述了正常路径，但未定义消费端（RegistrationEventListener）处理失败时的补偿策略——如果事件监听器抛出异常、事件丢失或 consultation 模块不可用，TriageRecord.finalDepartmentId 将永久为空。§9.3 虽然为配置变更提供了"定时刷新补偿"机制，但 RegistrationEvent 缺少类似的补偿策略。需求文档要求分诊结果"供统计与质量分析"使用，finalDepartmentId 空值将严重影响后续分析的准确性。
- **所在位置**：§2.2 跨模块事件传递机制；§3.1 TriageRecord 写入时机
- **严重程度**：一般
- **改进建议**：(a) 定义 RegistrationEvent 消费失败的重试策略（如 @Retryable + 死信队列或日志补偿）；(b) 或定义 polling 补偿机制——TriageRecord 中 finalDepartmentId 为空时，提供定时任务查询挂号表补充写入；(c) 在 TriageRecord 字段定义中补充说明 finalDepartmentId 可能为空的场景及处理指引

### 问题4：PrescriptionAssistResponse 缺少 errorCode 字段承载 AI 无可推荐药品的错误码传递

- **问题描述**：§4.4 定义"AI 返回无可推荐药品"场景返回 RX_ASSIST_AI_NO_RECOMMENDATION 标记，但承载方式描述为"在 AuditAlert 或 response header 中标记"——"或"表示二选一未决。PrescriptionAssistResponse DTO（§1.3）字段列表中无 errorCode 或类似承载字段。DosageAlert 已有 errorCode 字段，但 PrescriptionAssistResponse 作为 /assist 主端点的响应体，缺少顶层错误码字段。开发者阅读此设计后无法确定该标记的最终传递路径。
- **所在位置**：§3.4 PrescriptionAssistResponse DTO 定义；§4.4 /assist 主端点场景描述
- **严重程度**：一般
- **改进建议**：在 PrescriptionAssistResponse 中新增 errorCode（可选，String）顶层字段，统一承载 RX_ASSIST_AI_NO_RECOMMENDATION 错误码，消除"AuditAlert 或 header"的二选一歧义

### 问题5：AllergyWarningItem.severity 类型和值域未定义

- **问题描述**：§3.4 文本描述辅助开方过敏告警时使用 "severity=HIGH"，但 §10.4 AllergyWarningItem DTO 的 severity 字段仅标注名称未定义类型和值域（是 String？枚举？值范围是什么？）。与之对比，DosageAlert.alertLevel 明确使用 DosageAlertLevel 枚举（INFO/WARN/CRITICAL），AuditAlert.severity 明确使用 AlertSeverity 枚举（INFO/WARNING/CRITICAL）。开发者编码时将面临随意假设的风险。
- **所在位置**：§10.4 AllergyWarningItem DTO 定义；§3.4 allergyWarnings 描述
- **严重程度**：一般
- **改进建议**：定义 AllergyWarningSeverity 枚举（建议值：LOW/MEDIUM/HIGH 或 INFO/WARNING/CRITICAL）并更新 AllergyWarningItem.severity 的类型声明；在 §3.4 中补充说明 HIGH 与处方审核 AuditRiskLevel.BLOCK 的语义区分关系

### 问题6：encounterId/visitId 命名映射未显式说明

- **问题描述**：RecordGenerateRequest 使用 encounterId（对齐需求文档 3.4.3 输入契约字段名），MedicalRecord 实体使用 visitId（对齐已有模块的 visit 概念）。第 5 轮迭代已指出此映射关系需说明，但 §3.3 中二者仍分别以不同字段名出现，未显式说明 encounterId = visitId 的映射关系或转换规则。编码者可能误认为这是两个不同标识。
- **所在位置**：§3.3 RecordGenerateRequest（encounterId）；§3.3 MedicalRecord 实体字段（visitId）
- **严重程度**：轻微
- **改进建议**：在 RecordGenerateRequest 字段说明中显式标注"encounterId 映射为 MedicalRecord.visitId"，或在 Service 层契约的协作描述中说明转换逻辑

### 问题7：§9.1 科室模板初始数据集为占位符，内容缺失

- **问题描述**：§9.1 的内容仅为"与前一版一致"，但前几轮迭代的文件不可追溯——当前设计文档是独立分发的产出，其"前一版"上下文对新读者不可见。DEFAULT 科室模板的具体字段列表（在迭代第 3 轮 #5 中提及的"§9.1 中 DEFAULT 模板字段列表"）在 §9.1 中无实际内容。
- **所在位置**：§9.1 初始模板数据集
- **严重程度**：轻微
- **改进建议**：补充 DEFAULT 模板的必填字段列表（从 MedicalRecordField 枚举中选择），至少包含 CHIEF_COMPLAINT、SYMPTOM_DESCRIPTION、PRELIMINARY_DIAGNOSIS、TREATMENT_ADVICE 等基线字段

## 总体评价

该设计文档经过 9 轮迭代，在技术实现细节、并发安全、异常路径等方面已相当完善。主要缺口集中在**需求全流程闭环的完整性**（辅助开方"医生确认"环节缺失、规则管理 admin API 缺口）和**DTO 契约的精确度**（AllergyWarningItem.severity 类型未定义、PrescriptionAssistResponse 缺 errorCode 字段、encounterId/visitId 映射未说明）。上述问题均可在不改变现有架构的前提下以较小代价修复，不影响已确认的模块划分、依赖方向和技术方案。
