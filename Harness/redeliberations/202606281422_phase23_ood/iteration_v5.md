# 再审议判定报告（v5）

## 判定结果

RETRY

## 判定理由

组件B内部循环实际轮次为1，最大轮次为12，实际<最大说明提前终止；质询报告结论为LOCATED，表明审查问题经质询确认成立。

诊断报告识别了5个严重问题（问题1-5）和5个一般问题（问题6-11），质询报告对13个问题逐维度审查后确认：证据充分性整体通过，逻辑完整性整体通过，覆盖完备性发现3处遗漏。问题2的严重程度判定虽略显激进（未充分考虑路线图Phase 3范围约束），但设计文档自身未声明范围限制，改进建议合理，不构成驳回理由。问题5严重程度偏高但鉴于会造成实现歧义，尚可接受。

质询报告新识别的遗漏中，session_id必填/可选语义矛盾（需求3.4.1定义必填 vs 设计允许首轮为空）属严重级别的契约矛盾，此前未被发现；AuditResponse与需求3.4.2输出契约对齐缺失、TriageRecord持久化字段缺失属一般级别。这些遗漏进一步证实当前设计产出存在实质性的需求对齐缺陷。

综上，诊断报告含5个严重问题和5个一般问题，质询确认后补充识别1个严重和2个一般遗漏，满足RETRY标准。

## 需要解决的问题

- **问题描述**：AiSuggestionResult预创建→更新模式在异步并发场景下存在数据竞争，ConcurrentHashMap不保证value对象状态一致性
- **所在位置**：§3.4 AiSuggestionResult + §6.3 "包E 的异步 AI 建议"
- **严重程度**：严重
- **改进建议**：AiSuggestionResult内部使用volatile/AtomicReference/CAS保护状态更新，或使用ConcurrentHashMap.compute()原子更新；同一taskId状态更新保证幂等

- **问题描述**：包E check-dose请求与需求文档3.4.10 AI辅助开方输入契约完全脱节，仅实现单药品剂量检查而非完整AI辅助开方能力
- **所在位置**：§3.4 PrescriptionAssistService / DosageThresholdService + §4.4 check-dose API + §6.3 异步AI建议
- **严重程度**：严重
- **改进建议**：定义POST /api/prescription/assist作为3.4.10主端点接收完整输入，或显式标注当前设计仅覆盖剂量阈值告警子集并预留扩展结构

- **问题描述**：TriageResponse缺少需求文档3.4.1的推荐医生列表(recommended_doctors)和推荐理由(reason)字段，DialogueCreateRequest缺少ruleVersion/additionalResponses字段
- **所在位置**：§3.1 TriageResponse + DialogueCreateRequest + §4.1 智能分诊场景
- **严重程度**：严重
- **改进建议**：TriageResponse增加doctors和reason字段；DialogueCreateRequest增加ruleVersion和additionalResponses字段

- **问题描述**：AuditRequest.patientInfo未显式列出allergy_history和comorbidities字段结构，prescriptionItems每项字段定义不完整
- **所在位置**：§3.2 AuditRequest + §4.2 处方审核场景
- **严重程度**：严重
- **改进建议**：patientInfo显式列出allergy_history(string)和comorbidities(List<String>)，prescriptionItems每项包含drugId/drugName/dose/frequency/duration/route六字段

- **问题描述**：RecordGenerateRequest.visitId与需求文档encounter_id的映射关系未说明，departmentId引入理由未说明
- **所在位置**：§3.3 RecordGenerateRequest + §4.3 病历生成场景
- **严重程度**：严重
- **改进建议**：明确visitId与encounter_id映射关系，说明departmentId引入理由和与encounter_id的优先级

- **问题描述**：WARN级处方"强制提交并留痕"的留痕数据结构未定义（缺forceSubmitted/forceSubmitTime字段）
- **所在位置**：§3.2 AuditRecord + §4.2 WARN分支
- **严重程度**：一般
- **改进建议**：AuditRecord增加forceSubmitted(Boolean)和forceSubmitTime(LocalDateTime)字段，或显式定义其他承载实体

- **问题描述**：sessionId生成策略、格式规范未定义，前端/后端生成职责不清
- **所在位置**：§3.1 DialogueSession / DialogueSessionManager + §4.1 智能分诊场景
- **严重程度**：一般
- **改进建议**：明确sessionId由后端DialogueSessionManager统一生成，采用UUID v4格式，首轮请求sessionId为空

- **问题描述**：需求文档3.4.1定义session_id为必填，但设计文档§3.1允许首轮请求sessionId为空，构成契约必填/可选语义矛盾
- **所在位置**：§3.1 DialogueCreateRequest + 需求文档3.4.1输入契约
- **严重程度**：严重
- **改进建议**：在设计文档中明确首轮请求时sessionId传空值的合理性，或与需求方澄清首轮场景下session_id的可选性

- **问题描述**：DosageAlertLevel.CRITICAL与AuditRiskLevel.BLOCK联动触发机制调用链路未定义，存在触发时机矛盾
- **所在位置**：§3.4 DosageAlertLevel.CRITICAL描述 + §2.2 包D-AI1与包E强耦合处理
- **严重程度**：一般
- **改进建议**：CRITICAL剂量告警写入处方草稿上下文而非直接调用审核服务，提交时从草稿上下文读取CRITICAL标记作为BLOCK判定输入

- **问题描述**：DrugInteractionRule和AllergyCheckRule所需的数据实体(DrugInteractionPair/DrugAllergyMapping)完全缺失
- **所在位置**：§2.1 目录结构 + §3.2 LocalRuleEngine
- **严重程度**：一般
- **改进建议**：补充DrugInteractionPair和DrugAllergyMapping数据实体定义，或标注Phase 2/3仅实现DosageLimitRule其余待实现

- **问题描述**：分诊降级链中"AI无结果"判定边界不清晰——AI返回空列表是否视为有效结果未定义
- **所在位置**：§4.1 智能分诊场景 + §3.1 TriageService
- **严重程度**：一般
- **改进建议**：明确"AI无结果"判定条件：success=false/degraded=true降级；success=true但空列表视为有效结果跳过规则引擎

- **问题描述**：处方审核AI超时阈值(6s)和病历生成AI超时阈值(12s)未在设计文本中体现
- **所在位置**：§3.2 PrescriptionAuditService + §3.3 MedicalRecordService + §4.2/§4.3
- **严重程度**：一般
- **改进建议**：在§4.2/§4.3补充超时配置说明，定义ai.timeout.prescription-audit=6s和ai.timeout.medical-record-generate=12s

- **问题描述**：AuditResponse与需求3.4.2输出契约不对齐——缺少interactions(药物相互作用结果)和suggestions(用药建议)字段
- **所在位置**：§3.2 AuditResponse + §4.2 处方审核场景
- **严重程度**：一般
- **改进建议**：AuditResponse补充interactions和suggestions结构化输出字段，与需求3.4.2输出契约对齐

- **问题描述**：TriageRecord实体字段定义缺失，无法满足需求3.4.1"分诊结果需持久化以供统计与质量分析"的可观测性约束
- **所在位置**：§2.1 TriageRecord + 需求文档3.4.1服务质量要求
- **严重程度**：一般
- **改进建议**：补充TriageRecord字段定义，至少包含AI推荐科室、规则匹配科室、最终选择科室、置信度、降级标记等统计必需字段
