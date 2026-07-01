# 质量审查报告：Phase 2/3 OOD 设计方案 (v16)

## 审查概要

- **审查对象**: a_v16_copy_from_v15.md（Phase 2/3 包C/D-AI1/D-AI2/E 架构级 OOD 设计方案）
- **审查视角**: 需求响应充分度、整体深度与完整性、实际落地可行
- **前置处理**: 已通过 15 轮内部审议，当前为 v16 版本

---

## 发现问题

### 问题 1：[严重] correctedChiefComplaint 传递路径未闭环，实现者无法编码

- **所在位置**: §3.1 DialogueSession 字段定义（correctedChiefComplaint）、§3.1 TriageService "AI 调用上下文传递策略"
- **问题描述**: v16 新增了 correctedChiefComplaint 可选字段至 DialogueSession，并在全量拼接策略中说明"若 correctedChiefComplaint 存在则替换原始 chiefComplaint"。但 DialogueCreateRequest（前端入参 DTO）中未定义此字段，前端无法传递主诉修正信息。文档提及"由 AI 在追问回复中隐式识别"但未定义 TriageServiceImpl 从 AI 回复中检测修正主诉的判定规则——实现者既无法从前端获取 correctedChiefComplaint，也无法靠隐式识别逻辑完成编码。
- **改进建议**: 路径 A—在 DialogueCreateRequest 中增加 correctedChiefComplaint（可选，String）字段，前端在患者修正主诉时显式传入；路径 B—若坚持 AI 隐式识别，需明确定义 TriageServiceImpl 在 AI 回复中检测主诉修正语义的判定规则（如关键词匹配、语义变化检测等）和对应的 DialogueSession 更新触发器。

### 问题 2：[严重] TriageRecord.finalDepartmentId 补充写入未覆盖手动选科场景

- **所在位置**: §3.1 TriageRecord 写入时机描述、§2.2 RegistrationEvent 消费机制、§4.1 降级时前端行为指引
- **问题描述**: finalDepartmentId 的补充写入仅定义了 RegistrationEvent（挂号流程）一种路径。但 §4.1 定义了降级时前端的"手动选择科室"入口——患者绕过挂号流程直接在前端选择科室。此入口产生的 finalDepartmentId 如何写入 TriageRecord 完全未定义。这意味着降级场景下 finalDepartmentId 将永久为 null，与需求文档中分诊记录的可追溯性要求矛盾。
- **改进建议**: 在 TriageController 中增加 POST /api/triage/select-department 端点（含 sessionId + departmentId），或扩展 TriageService 补充手动选科的 finalDepartmentId 写入方法，并在 §4.1 降级路径中补充该路径说明。

### 问题 3：[严重] 提交端点阻断判定时序未定义，forceSubmit=true 与 CRITICAL 阻断的交互关系不清晰

- **所在位置**: §4.2 处方提交端点行为三步描述
- **问题描述**: 提交端点定义了三条行为：① forceSubmit=false 常规审核；② forceSubmit=true 强制提交校验；③ 阻断合并判定。但三步之间的执行顺序未明确定义：CRITICAL 阻断检查（步③）应在 forceSubmit 路径判定之前还是之后？如果 forceSubmit=true 通过了 WARN 版本校验（步②），但随后被 CRITICAL 阻断（步③）拒绝提交，流程上产生"已通过校验又被阻断"的矛盾路径——forceSubmit 语义被 CRITICAL 覆盖。开发者无法确定步③是步①/②的后续步骤还是并行判定路径。
- **改进建议**: 明确定义提交端点执行顺序。推荐方案：CRITICAL 阻断检查作为步①，优先于 forceSubmit 判定——若存在 CRITICAL 告警，无论 forceSubmit 取值如何均直接阻断。forceSubmit 判定仅在没有 CRITICAL 且为 WARN 级别时生效。在 §7 设计决策中补充此时序决策条目。

### 问题 4：[严重] AiResult.failure()/degraded() 重载方法缺乏实现归属，实现者无法定位代码位置

- **所在位置**: §2.3 AiService 接口定义、§7 设计决策"AiResult 超时降级模式"、§2.1 目录结构
- **问题描述**: §2.3 定义了 AiResult.failure(String errorCode, T partialData) 和 AiResult.degraded(String fallbackReason, T partialData) 两个新重载方法。但全文档目录结构中均未列出 AiResult 类的所在包路径、模块归属和现有方法签名。AiResult 类既不在 §2.1 目录结构的任何模块中出现，也未在任何章节中标注其包路径。实现者无法确定此类的代码位置，从而导致新重载方法无法落地。
- **改进建议**: 在 §2.1 目录结构中补充 AiResult 类的归属位置（建议归入 ai-api 模块），或在 §2.3 首段补充 AiResult 类的包路径和当前已有方法签名（success()/failure()/degraded()），使实现者清楚在何处添加新重载。

### 问题 5：[一般] PrescriptionDraftContext 与 AiSuggestionResult TTL 不一致导致状态残差

- **所在位置**: §3.4 PrescriptionDraftContext 生命周期管理（TTL 60 分钟）、§3.4 AiSuggestionResult（TTL 30 分钟）、§4.4 check-dose 流程
- **问题描述**: 一次 check-dose 请求同时向 PrescriptionDraftContext（写入 CRITICAL 标记）和 AiSuggestionResult（预创建 PENDING）写入数据，但二者的 TTL 分别为 60 分钟和 30 分钟。30 分钟后 AiSuggestionResult 被 ScheduledExecutorService 清理，前端查询 suggestion/{taskId} 得到 RX_ASSIST_SUGGESTION_NOT_FOUND；但 PrescriptionDraftContext 中的 CRITICAL 标记仍存在长达 30 分钟的残差期，造成"任务已过期但阻断标记仍生效"的状态矛盾。此不对称性虽在实践中概率较低（30 分钟后医生通常已完成处方编辑），但从状态一致性角度构成设计缺陷。
- **改进建议**: 将 AiSuggestionResult 的 TTL 调整为 60 分钟以与 PrescriptionDraftContext 一致，或在 §3.4 中增加说明二者 TTL 不对称性的业务合理性和接受此风险的明确理由。

### 问题 6：[一般] contextCriticalCount 前端消费行为未定义，前端开发者无法编码

- **所在位置**: §4.4 check-dose 响应、§3.4 PrescriptionDraftContext "前端同步协商机制"
- **问题描述**: check-dose 响应新增了 contextCriticalCount 字段，说明供"前端判断是否需要同步刷新状态"。但未定义 contextCriticalCount 在前端的具体消费规则——当 contextCriticalCount 从 N>0→0（CRITICAL 告警解除）或从 0→N>0（新的 CRITICAL 告警产生）时，前端应执行何种 UI 操作。前端开发者无从知道需要刷新什么、如何刷新。
- **改进建议**: 补充 contextCriticalCount 变化时前端推荐行为：当 N→0 时，恢复提交按钮至可点击状态并清除 CRITICAL 阻断提示；当 0→N 时，禁用提交按钮并展示新的阻断原因摘要。

---

## 整体质量评价

该文档经过 15 轮审议后成熟度显著提高，在需求覆盖度和架构设计的完整性方面已达到可实施水平。底座落地、强耦合同步、模块依赖方向、DTO 映射机制等核心架构决策清晰。上述 6 个问题集中于 v16 新增功能（correctedChiefComplaint、六级匹配、contextCriticalCount）的传递路径未闭环和时序定义模糊，属于最后阶段的"细节贯通"型问题，修复成本较低。建议修复后即可交付编码阶段。
