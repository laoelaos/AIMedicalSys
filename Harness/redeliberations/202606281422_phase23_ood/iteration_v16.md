# 再审议判定报告（v16）

## 判定结果

RETRY

## 判定理由

诊断报告（b_v16_diag_v1）识别出 6 个问题，其中 4 个严重（问题 1-4）、2 个一般（问题 5-6），符合 RETRY 条件。质询报告（b_v16_challenge_v1）结果为 LOCATED，实际轮次（1）小于最大轮次（12），表明审查结论被确认且提前终止。由于诊断报告中存在严重和一般等级的问题，组件 A 需要重新运行以修复上述问题。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：correctedChiefComplaint 传递路径未闭环，DialogueCreateRequest 中未定义该字段，缺少 AI 隐式识别判定规则
- **所在位置**：§3.1 DialogueSession 字段定义、§3.1 TriageService "AI 调用上下文传递策略"
- **严重程度**：严重
- **改进建议**：在 DialogueCreateRequest 中增加 correctedChiefComplaint 可选字段，或明确定义 TriageServiceImpl 的 AI 回复检测规则

- **问题描述**：TriageRecord.finalDepartmentId 补充写入未覆盖手动选科场景，降级路径下 finalDepartmentId 将永久为 null
- **所在位置**：§3.1 TriageRecord 写入时机描述、§2.2 RegistrationEvent 消费机制、§4.1 降级时前端行为指引
- **严重程度**：严重
- **改进建议**：增加 POST /api/triage/select-department 端点或扩展 TriageService 补充手动选科的 finalDepartmentId 写入方法

- **问题描述**：提交端点阻断判定时序未定义，forceSubmit=true 与 CRITICAL 阻断的交互关系不清晰
- **所在位置**：§4.2 处方提交端点行为三步描述
- **严重程度**：严重
- **改进建议**：明确定义执行顺序，推荐 CRITICAL 阻断检查作为步①优先于 forceSubmit 判定

- **问题描述**：AiResult.failure()/degraded() 重载方法缺乏实现归属，实现者无法定位代码位置
- **所在位置**：§2.3 AiService 接口定义、§7 设计决策、§2.1 目录结构
- **严重程度**：严重
- **改进建议**：在 §2.1 目录结构中补充 AiResult 类的归属位置，或在 §2.3 首段补充其包路径和方法签名

- **问题描述**：PrescriptionDraftContext 与 AiSuggestionResult TTL 不一致导致状态残差，30 分钟后出现"任务已过期但阻断标记仍生效"的矛盾
- **所在位置**：§3.4 PrescriptionDraftContext 生命周期管理、§3.4 AiSuggestionResult、§4.4 check-dose 流程
- **严重程度**：一般
- **改进建议**：将 AiSuggestionResult 的 TTL 调整为 60 分钟以保持一致，或说明不对称性的业务合理性

- **问题描述**：contextCriticalCount 前端消费行为未定义，前端开发者无法确定何时执行何种 UI 操作
- **所在位置**：§4.4 check-dose 响应、§3.4 PrescriptionDraftContext "前端同步协商机制"
- **严重程度**：一般
- **改进建议**：补充 contextCriticalCount 变化时前端推荐行为，如 N→0 时恢复提交按钮，0→N 时禁用按钮并展示阻断原因
