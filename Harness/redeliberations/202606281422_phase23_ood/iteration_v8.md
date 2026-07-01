# 再审议判定报告（v8）

## 判定结果

RETRY

## 判定理由

组件B诊断报告识别出16个问题，其中3项严重（问题3/4/5）、10项一般（问题1/2/6/7/8/9/10/11/14/15/16）、2项轻微（问题12/13）。质询报告结论为LOCATED，且实际循环轮次（3）< 最大轮次（12），说明组件B提前确认了审查结论而非循环耗尽退出。质询报告对证据充分性、逻辑完整性、覆盖完备性、报告必要性四维度均判定通过，仅指出一个轻微覆盖遗漏（DosageCheckRequest.frequency可选性与日剂量校验关系），不影响审查结论可信度。由于审查报告包含严重和一般等级的问题，判定为RETRY。

## 需要解决的问题

- **问题描述**：DoctorFacade 放置于 auth 包下，与"认证"包语义不匹配，影响编码者模块定位判断
- **所在位置**：§2.1 目录结构 `common-module-api/.../auth/UserFacade.java, DoctorFacade.java`；§1.3 核心抽象一览 DoctorFacade 条目
- **严重程度**：一般
- **改进建议**：在 common-module-api 下为 DoctorFacade 创建独立子包（如 `commonmodule/doctor/DoctorFacade.java`），与 auth 包解耦

- **问题描述**：需求文档 §3.4.1 matched_rules 子结构未定义，设计侧主动定义了 ruleId/ruleName/score 但未在设计决策中论证选择理由
- **所在位置**：§1.3 核心抽象一览 MatchedRule DTO；§10.1 ai-api 层 MatchedRuleItem；§7 设计决策表（缺少 matched_rules 子字段设计决策条目）
- **严重程度**：一般
- **改进建议**：在 §7 设计决策中增加 matched_rules 子字段的设计决策条目，说明需求侧缺口及设计侧主动选择理由

- **问题描述**：需求文档 §3.4.2 检查项 #2 明确要求合并症-药品禁忌检查，设计仅实现过敏史检查，合并症检查无对应规则实现
- **所在位置**：§3.2 LocalRuleEngine 实现范围表 + AllergyCheckRule 描述
- **严重程度**：严重
- **改进建议**：新增 ContraindicationCheckRule 或扩展 AllergyCheckRule，补充合并症-药品禁忌检查；同步更新 §3.2 实现范围表、§2.1 目录结构和 §7 设计决策

- **问题描述**：AiResult 的 failure()/degraded() 工厂方法均将 data 设为 null，无法同时传递错误码和部分数据；设计文本对"新增 partialData 字段"与"使用现有 data 字段"存在歧义，且仅覆盖 degraded() 路径遗漏 failure() 路径
- **所在位置**：§3.3 MedicalRecordService "非流式超时降级路径"；§7 设计决策 "AiResult 超时降级模式"
- **严重程度**：严重
- **改进建议**：明确使用现有 AiResult.data 字段承载部分结果；为 failure() 和 degraded() 各增加携带 partialData 的重载；删除"新增 partialData 字段"歧义描述

- **问题描述**：需求文档 §5.1 分诊记录实体明确含"推荐医生"字段，设计 TriageRecord 未持久化推荐医生数据
- **所在位置**：§3.1 TriageRecord 实体字段列表
- **严重程度**：严重
- **改进建议**：在 TriageRecord 中增加 recommendedDoctors（JSON TEXT）字段

- **问题描述**：AuditAlert.severity 字段未定义类型和值域，与 AuditRiskLevel 维度不同但未明确区分
- **所在位置**：§1.3 AuditAlert DTO；§3.2 AuditResponse；§10.2 ai-api 层 AlertItem
- **严重程度**：一般
- **改进建议**：补充 severity 字段类型和值域定义，推荐为独立枚举 AlertSeverity/INFO/WARNING/CRITICAL

- **问题描述**：DosageAlert 无承载错误码的字段，RX_ASSIST_DOSE_STANDARD_NOT_FOUND 传递路径不明确
- **所在位置**：§3.4 DosageAlert 类定义；§3.4 DosageCheckResponse；§4.4 check-dose 流程
- **严重程度**：一般
- **改进建议**：在 DosageAlert 或 DosageCheckResponse 中增加 errorCode（String，可选）字段

- **问题描述**：处方提交 WARN 级强制提交时"处方与 AuditRecord.originalPrescription 一致"的比较语义未定义
- **所在位置**：§4.2 处方提交端点"处方版本校验"逻辑
- **严重程度**：一般
- **改进建议**：补充结构化比较语义定义（drugId + dose + frequency + duration + route 五字段组合比对）

- **问题描述**：allergyHistory/allergyDetails 数据来源语义不一致，v2 问题9和问题16改进建议相互矛盾
- **所在位置**：§3.2 AuditRequest.patientInfo；§3.4 PrescriptionAssistRequest.patientInfo；§10.2 / §10.4 ai-api 层 DTO
- **严重程度**：一般
- **改进建议**：统一为后端优先从健康档案提取+前端存入作为补充的双通道语义，Service 层定义来源优先级规则

- **问题描述**：辅助开方过敏告警与处方审核过敏检查的逻辑重叠关系未说明
- **所在位置**：§3.4 PrescriptionAssistService "本地即时校验"；§3.2 AllergyCheckRule
- **严重程度**：一般
- **改进建议**：补充二者关系说明——即时提示 vs 提交时正式审核，独立执行不互斥

- **问题描述**：RegistrationEvent 跨模块事件契约未定义
- **所在位置**：§3.1 TriageService "TriageRecord 写入时机"；§4.1 持久化说明
- **严重程度**：一般
- **改进建议**：定义 RegistrationEvent 事件契约及跨模块事件传递机制说明

- **问题描述**：TriageResponse.degraded=true 时前端应如何调整 UI 未说明
- **所在位置**：§3.1 TriageService 降级链；§1.3 TriageResponse.degraded 字段
- **严重程度**：轻微
- **改进建议**：补充 degraded=true 时前端行为说明——仍渲染推荐科室列表同时显示降级提示并提供手动选择入口

- **问题描述**：设计目标"规避 Phase 5 迁移成本"未显式论证
- **所在位置**：§1.1 设计目标"底座直接落地"
- **严重程度**：轻微
- **改进建议**：在 §1.1 或 §7 增加底座落地与 Phase 5 迁移兼容性设计决策条目

- **问题描述**：§5.1 错误码表混合含 _AI_ 中段和不含 _AI_ 中段的错误码，分类命名规则不明确
- **所在位置**：§5.1 错误码表
- **严重程度**：一般
- **改进建议**：在 §5.1 增加 AI 能力错误码与本地业务错误码的命名区分规则说明

- **问题描述**：DialogueSession TTL 清理竞态和规则快照失效场景处理未定义
- **所在位置**：§6.1 对话会话并发管理；§3.1 DialogueSession ruleVersion/ruleSetId 快照机制
- **严重程度**：一般
- **改进建议**：补充 TTL 清理竞态处理说明和规则快照失效降级处理

- **问题描述**：§5.1 错误码表遗漏需求文档明确定义的 RX_ASSIST_AI_NO_RECOMMENDATION、RX_AUDIT_AI_INPUT_INVALID、MR_GEN_AI_INPUT_INVALID、MR_GEN_AI_OUTPUT_INCOMPLETE
- **所在位置**：§5.1 错误码表
- **严重程度**：一般
- **改进建议**：补齐需求文档 §3.4.x 明确定义的全部 AI 能力错误码
