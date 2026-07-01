根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 问题 1：AuditRecord 落库在降级路径中被遗漏，与 §3.2 行为契约矛盾
- **所在位置**：§4.2 "处方审核场景"——降级流程描述
- **严重程度**：严重
- **改进建议**：在 §4.2 降级流程中显式补充 AuditRecord 持久化步骤，或与正常路径合并为一个统一表述以消除歧义。同时确认：本地规则校验结果写入 AuditRecord 的 riskLevel 字段时，是否需额外标记 fromFallback=true 以区分 AI 结果与本地规则结果。

### 问题 2：分诊降级链路不完整——AI 为空且规则也为空时的行为未定义
- **所在位置**：§4.1 "智能分诊场景"
- **严重程度**：一般
- **改进建议**：将三条路径改为线性降级链：AI 无结果 → 规则匹配无结果 → FallbackProvider 兜底。在 §4.1 中明确 TriageRuleEngine.match() 返回空列表时，应继续调用 DepartmentFallbackProvider.getFallbackDepartments()；TriageService 的协作描述也需同步更新。

### 问题 3：§2.1 目录中存在 AgeBandedDosage.java 残留条目，与已确定的实现方案矛盾
- **所在位置**：§2.1 目录结构
- **严重程度**：轻微
- **改进建议**：从 §2.1 目录中移除 AgeBandedDosage.java 条目，或明确标注"本阶段不使用，Phase 5 扩展时按需引入"。

### 问题 4：TriageResponse DTO 字段结构未定义，无法支撑编码实现
- **所在位置**：§3.1 包C 核心抽象
- **严重程度**：一般
- **改进建议**：补充 TriageResponse 的字段定义，至少包含：departments（推荐科室列表）、sessionId（多轮场景）、needFollowUp（是否需要追问）、followUpQuestion（追问内容）、confidence（可选置信度标量）。同时明确 DialogueCreateRequest 是否为首轮请求 DTO，若是则说明其字段。

### 问题 5：剂量单位转换规则集未定义，实现面临随意假设风险
- **所在位置**：§8.3 "单位一致性校验"
- **严重程度**：一般
- **改进建议**：补充剂量单位兼容分组的枚举或表格定义。推荐方案：定义一个 DosageUnitGroup 枚举（如 MASS_GROUP: mcg↔mg↔g、VOLUME_GROUP: ml↔L 等），各组内支持自动换算，跨组返回 RX_ASSIST_UNIT_MISMATCH。对于 IU 等非质量/体积单位可单独分组或定义为不可换算。转换系数以常量形式固化在 DosageThresholdService 中，后续可扩展为数据库可配置。

### 问题 6：MedicalRecord 实体字段未定义，病历结构化输出模型缺失
- **所在位置**：§3.3 包D-AI2
- **严重程度**：一般
- **改进建议**：在 §3.3 中补充核心字段枚举或值对象定义（如 MedicalRecordField 枚举），列出病历结构化输出的顶层字段标识。RecordGenerateRequest 和 RecordGenerateResponse 补充字段定义。DepartmentTemplateConfig.requiredFields 的类型明确为 List<MedicalRecordField>。这与 §9.1 中 DEFAULT 模板字段列表形成呼应，使字段标识符与模板内容一致。

### 问题 7：规则/模板配置变更缺少审计溯源能力
- **所在位置**：§3.1 TriageRuleEngine（规则热加载）；§3.3 TemplateConfigManager（模板事件驱动刷新）；§9（模板更新）
- **严重程度**：一般
- **改进建议**：在规则变更和模板更新的事件处理中添加配置变更日志记录。推荐方案：(a) 定义 ConfigChangeLog 实体（归属 admin 模块或 common 模块）；(b) 在 TemplateConfigChangeEvent 和规则变更的处理链中写入审计日志；(c) 事件发布者提供新旧值语义。Phase 2 可先以日志文件方式实现，后续迁移至数据库审计表。

## 历史迭代回顾
### 已解决的问题（出现在历史反馈但当前反馈中不再提及的问题）
- Round 1: DialogueSession 不可变/可变矛盾 → 已修复（v3 已改为可变 class + DialogueSessionManager 并发控制）
- Round 1: 包E 异步 AI 建议缺少消费路径 → 已修复（v3 补充了 GET 查询端点和三分支模式）
- Round 1: 多轮分诊对话历史维护责任不明确 → 已修复（v3 以服务端 DialogueSession 为单一真相来源）
- Round 1: DosageCheckRequest 缺少给药途径参数 → 已修复（v3 已补充 routeOfAdministration）
- Round 1: DosageStandard 写权限归属未定义 → 已修复（v3 明确 admin 模块为唯一写入者）
- Round 1: 分诊规则配置变更生效机制未定义 → 已修复（v3 定义了 Caffeine 定时刷新 + 事件驱动失效）
- Round 1: 科室模板配置 CRUD 管理和默认兜底缺失 → 已修复（v3 补充了 DEFAULT 兜底和模板管理接口）
- Round 1: 对话会话内存存储未覆盖服务重启场景 → 已修复（v3 补充了 findOrCreate 三分支 + TRIAGE_SESSION_EXPIRED）
- Round 1: 新模块依赖声明未包含 common-module-api → 已修复（v3 已补充）
- Round 1: 剂量标准数据初始化方案和编码规范缺失 → 已修复（v3 补充了 SQL 种子脚本和药品编码规范）
- Round 2: BLOCK 风险等级缺少后端强制阻断执行机制 → 已修复（v3 新增 PrescriptionAuditEnforcer + HTTP 422）
- Round 2: AuditRecord 缺少处方级关联标识 → 已修复（v3 补充 prescriptionOrderId/doctorId/patientId）
- Round 2: AiSuggestionResult 服务重启后结果丢失 → 已修复（v3 补充 TTL + findOrCreate 三分支）
- Round 2: DosageStandard 年龄/体重分级存储结构未定义 → 已修复（v3 定义内联字段 + 四级匹配）
- Round 2: 病历生成降级策略丢弃已提取字段 → 已修复（v3 改为分层保护策略）

### 持续存在的问题（在多轮反馈中反复出现的问题，需重点解决）
- **剂量单位相关**：Round 1 问题 10（单位一致性校验缺失）→ Round 3 问题 5（单位转换规则集未定义）。Round 1 仅要求"补充单位一致性校验逻辑"，v3 在 §8.3 补充了"可转换则自动转换，不可转换则返回错误码"的示意说明，但未定义完整的兼容单位分组表和换算系数。本轮需重点解决：定义 DosageUnitGroup 枚举及换算系数常量。

### 新发现的问题（本轮新识别的问题）
- 问题 1：AuditRecord 落库在降级路径中被遗漏
- 问题 2：分诊降级链路不完整（AI 空 + 规则空 → 未定义）
- 问题 3：§2.1 目录 AgeBandedDosage 残留条目
- 问题 4：TriageResponse DTO 字段结构未定义
- 问题 6：MedicalRecord 实体字段未定义
- 问题 7：规则/模板配置变更缺少审计溯源能力

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281422_phase23_ood\a_v3_design_v1.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281422_phase23_ood\requirement.md
