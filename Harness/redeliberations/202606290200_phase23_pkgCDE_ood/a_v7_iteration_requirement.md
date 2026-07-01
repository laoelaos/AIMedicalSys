根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 问题 1：DrugFacade 同步跨模块调用缺失降级保护
**严重程度**：一般
**描述**：DrugFacade 作为 prescription 模块同步调用的跨模块门面接口，既无超时配置边界，也无任何错误处理或降级策略定义。当 drug 模块不可用或调用超时时，prescription 模块的行为完全未定义，存在运行时服务级联故障风险。
**所在位置**：§2.2（line 298）；§1.3 DrugFacade 核心抽象条目（line 143）；§3.4 PrescriptionAssistService 职责描述
**改进建议**：为 DrugFacade 补充对标 DoctorFacade/VisitFacade 的降级保护——至少包含超时阈值（如 2s）、调用失败后的回退行为（如返回空药品信息 + WARN 日志，不阻断辅助开方/处方审核主流程），并在对应行为契约中标注。建议在 §8.2 药品编码规范段同步补充 DrugFacade 降级声明。

### 问题 2：§8.4 六级匹配优先级文本编号与实际层级数不符
**严重程度**：一般
**描述**：Line 1432 中描述 DosageThresholdService 匹配优先级文本为"按优先级 1→2→3→4→5 依次尝试"，但实际列表中展开了 6 级优先级。编号序列与总层级数不一致，属于事实性错误。
**所在位置**：§8.4（line 1432）
**改进建议**：将文本"1→2→3→4→5"修正为"1→2→3→4→5→6"以与实际列出的 6 级优先级匹配。

### 问题 3：RX_AUDIT_FORCE_SUBMIT_INVALID 错误码缺失于 §5.1 错误码表
**严重程度**：一般
**描述**：该错误码在 §4.2 处方提交端点行为描述中使用——"forceSubmit=true 在此场景下无效（返回 RX_AUDIT_FORCE_SUBMIT_INVALID 错误码）"，但 §5.1 模块级错误码表的"审核（非AI）"行中未列出此错误码。导致错误码定义分散，无法指导编码阶段一次性还原完整错误码列表。
**所在位置**：§4.2 处方提交端点（line 832）；§5.1 错误码表（line 1250）
**改进建议**：在 §5.1 审核（非AI）错误码行中补充 RX_AUDIT_FORCE_SUBMIT_INVALID，并标注使用场景说明。

### 问题 4：AiSuggestionResult TTL 清理扫描间隔未显式定义
**严重程度**：轻微
**描述**：DialogueSessionManager 和 PrescriptionDraftContext 均已给出明确的 TTL 清理扫描间隔（均标注"每 5 分钟扫描一次"），但 AiSuggestionResult 仅描述为"由 ScheduledExecutorService 定期清理过期条目"，未给出具体的清理间隔参数。与另外两项内存存储的描述存在不对称。
**所在位置**：§3.4 AiSuggestionResult 条目（line 655）；§6.3 异步 AI 建议与去重段（line 1303）；§4.4 check-dose 流程（line 894）
**改进建议**：在 AiSuggestionResult 的 TTL 描述中补充清理周期（建议统一为 5 分钟扫描间隔），标注"由 ScheduledExecutorService 每 5 分钟扫描清理 TTL 超过 60 分钟的条目"，与 DialogueSessionManager 和 PrescriptionDraftContext 的清理机制描述一致。

## 历史迭代回顾

### 已解决的问题
（无）— 第 6 轮反馈的 4 个问题在本轮审查中均继续出现，尚未解决。

### 持续存在的问题
- **问题 1（DrugFacade 降级缺失）**：第 6 轮已提出，本轮审查中问题仍然存在，且质询已确认。需重点解决。
- **问题 2（§8.4 编号不符）**：第 6 轮已提出，本轮审查中文本仍未被修正。需解决。
- **问题 3（RX_AUDIT_FORCE_SUBMIT_INVALID 缺失）**：第 6 轮已提出，错误码仍未补入 §5.1。需解决。
- **问题 4（AiSuggestionResult TTL 间隔缺失）**：第 6 轮已提出，参数仍未补充。需解决。

### 新发现的问题
（无）

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606290200_phase23_pkgCDE_ood\a_v6_copy_from_v5.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606290200_phase23_pkgCDE_ood\requirement.md
