# 再审议判定报告（v6）

## 判定结果

RETRY

## 判定理由

组件B诊断报告识别出14个问题，其中3个严重、9个一般、2个轻微。质询报告结论为LOCATED（实际轮次1 < 最大轮次12，提前终止），所有问题的证据充分性、逻辑完整性、覆盖完备性均通过质询，仅发现2处轻微审查意见（问题5与6部分重叠、matched_rules子字段为推断结构），均不影响问题成立。

严重问题清单：
- 问题1：AuditResponse.issues与需求文档3.4.2 alerts字段命名与结构完全不一致
- 问题5：病历生成缺少流式输出生成模式的完整设计
- 问题8：本地规则校验仅实现1/4最小检查项集，直接影响AI不可用时的用药安全兜底

一般问题9个（问题2/3/4/6/7/9/10/11/12），覆盖字段缺失、结构不对齐、生命周期管理缺失等。严重及一般问题合计12个，不满足PASS条件，需重新运行组件A。

## 需要解决的问题

- **问题描述**：AuditResponse.issues 与需求文档 3.4.2 alerts 字段命名与结构不一致——字段名不同（issues vs alerts），子字段结构完全不同（fieldName/issueDescription/ruleId vs alert_code/alert_message）
- **所在位置**：§3.2 AuditResponse + AuditIssue 定义；§1.3 核心抽象一览
- **严重程度**：严重
- **改进建议**：在 AuditResponse 中同时保留 alerts（对齐需求契约）和 issues（设计自有结构）并说明映射关系，或将 AuditIssue 字段名对齐为 alertCode/alertMessage；在 §3.2 或 §7 补充设计决策说明

- **问题描述**：DialogueCreateRequest 缺少 ruleSetId 字段，与需求文档 3.4.1 输入契约不对齐
- **所在位置**：§3.1 DialogueCreateRequest 定义；§1.3 核心抽象一览
- **严重程度**：一般
- **改进建议**：增加 ruleSetId（可选，String），对齐需求文档 rule_set_id，补充消费语义

- **问题描述**：TriageResponse 缺少 matchedRules 字径，与需求文档 3.4.1 输出契约不对齐
- **所在位置**：§3.1 TriageResponse 定义；§1.3 核心抽象一览
- **严重程度**：一般
- **改进建议**：增加 matchedRules（可选，List<MatchedRule>），对齐需求文档 matched_rules

- **问题描述**：TriageResponse.departments 每项字段结构未定义，与需求文档 3.4.1 recommended_departments 不完整对齐
- **所在位置**：§3.1 TriageResponse；§1.3 核心抽象一览
- **严重程度**：一般
- **改进建议**：新增 RecommendedDepartment DTO（含 departmentId、departmentName、score），与需求文档对齐

- **问题描述**：病历生成缺少流式输出生成模式的完整设计——无流式端点、无流式超时配置、无分片/错误处理机制
- **所在位置**：§3.3 MedicalRecordController / MedicalRecordService；§4.3 病历生成场景；§5.5 AI 超时配置
- **严重程度**：严重
- **改进建议**：若当前不实现流式，显式标注"流式待后续迭代"并补充 stream 字段和流式超时配置项；若实现流式则补充完整流式契约（SSE/WebSocket端点、流式超时、错误分片）

- **问题描述**：RecordGenerateRequest 缺少 stream 字段，与需求文档 3.4.3 输入契约不对齐
- **所在位置**：§3.3 RecordGenerateRequest 定义
- **严重程度**：一般
- **改进建议**：增加 stream（bool，可选，默认 false），对齐需求文档 3.4.3；若不实现流式则标注"Phase 3 仅支持非流式"

- **问题描述**：RecordGenerateResponse 的 fields 映射结构与需求文档 3.4.3 输出契约字段命名不一致（如 TREATMENT_ADVICE vs treatment_plan），missing_fields 与 missingFieldHints 映射关系未说明
- **所在位置**：§3.3 RecordGenerateResponse；§3.3 MedicalRecordField 枚举
- **严重程度**：一般
- **改进建议**：补充 MedicalRecordField 枚举与需求文档字段名的映射说明；说明 missingFieldHints 是 missing_fields 的结构化升级版本

- **问题描述**：本地规则校验仅实现 1/4 最小检查项集，药品禁忌检查和重复用药检查具备本地实现条件但未纳入
- **所在位置**：§3.2 LocalRuleEngine；§4.2 处方审核降级路径
- **严重程度**：严重
- **改进建议**：将药品禁忌检查和重复用药检查纳入 Phase 3 最低交付范围；若确有困难，显式标注为降级安全缺口并增加风险提示

- **问题描述**：PrescriptionDraftContext 的 key 定义不清（encounterId vs 处方编辑会话标识），创建/清理时机未定义
- **所在位置**：§3.4 PrescriptionDraftContext；§6.4 处方草稿上下文并发管理
- **严重程度**：一般
- **改进建议**：明确 key 为 prescriptionId；定义创建时机（首次 check-dose 请求）和清理时机（提交成功/取消/TTL 过期）

- **问题描述**：分诊场景缺少 AI 连续失败 3 次的兜底提示机制
- **所在位置**：§3.1 DialogueSessionManager；§4.1 智能分诊场景
- **严重程度**：一般
- **改进建议**：在 DialogueSession 增加 aiFailCount 字段，TriageServiceImpl 降级后检查 aiFailCount≥3 时附加兜底提示

- **问题描述**：AuditRecord 缺少审核次序（auditSequence）和是否最新（isLatest）字段，无法按序追溯和快速定位最新审核
- **所在位置**：§3.2 AuditRecord 实体定义
- **严重程度**：一般
- **改进建议**：增加 auditSequence（int，递增）和 isLatest（boolean，仅最新一条为 true）字段

- **问题描述**：DosageStandard 与药品基础信息实体的关系未定义，数据录入路径不明确
- **所在位置**：§2.2 DosageStandard；§8.1 初始化方案；§8.2 药品编码规范
- **严重程度**：一般
- **改进建议**：说明两者通过 drugCode 关联但为独立实体；明确 admin 模块维护入口和 drugCode 主键关联关系
