# 再审议判定报告（v4）

## 判定结果

RETRY

## 判定理由

组件B诊断报告（v4）识别出 9 项质量问题，其中严重 1 项、一般 6 项、轻微 2 项。质询报告结论为 LOCATED（实际轮次 1/最大轮次 12，提前终止于审查确认），表明全部审查结论已被确认有效。因诊断报告包含严重及一般等级的问题，不符合 PASS 条件，需重新运行组件A进行修订。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：AiSuggestionResult 状态枚举缺少 FAILED 状态，异步 AI 失败时产生"幽灵 pending"场景
- **所在位置**：§3.4 AiSuggestionResult 查询契约 + §6.3 异步 AI 建议
- **严重程度**：严重
- **改进建议**：在 AiSuggestionResult 中新增 FAILED 状态枚举值，含可选的 failReason 字段。实现侧采用"预创建→更新"模式：check-dose 响应前以 PENDING 状态预创建 AiSuggestionResult 条目，异步回调完成后更新为 COMPLETED 或 FAILED；异常被 CompletableFuture exceptionally 处理器捕获时同样写入 FAILED 状态而非留下 PENDING 僵尸条目。前端根据 FAILED 状态展示明确失败提示。

- **问题描述**：TriageRequest DTO 存在于目录结构但设计文本中无定义，与 DialogueCreateRequest 关系不明确
- **所在位置**：§2.1 目录列出了 `dto/TriageRequest.java`，但 §1.3 和 §3.1 的核心抽象定义中仅有 DialogueCreateRequest 而无 TriageRequest
- **严重程度**：一般
- **改进建议**：任选以下清除路径之一：路径 A（推荐）— 从目录中删除 TriageRequest.java 条目，统一使用 DialogueCreateRequest 作为分诊请求 DTO，并在 §3.1 DialogueCreateRequest 的说明中补充 sessionId 字段；路径 B — 保留 TriageRequest 作为分诊请求的顶层 DTO，明确其与 DialogueCreateRequest 的差异和各自的适用场景，在 §3.1 中补充定义。

- **问题描述**：LocalRuleResult / AuditIssue / AuditRequest / AuditResponse / FieldMissingHint 等关键 DTO 存在于目录但设计文本无定义
- **所在位置**：§2.1 目录列出但全文未定义：`prescription/rule/LocalRuleResult.java`、`prescription/dto/audit/AuditIssue.java`、`prescription/dto/audit/AuditRequest.java`、`prescription/dto/audit/AuditResponse.java`、`medical-record/dto/FieldMissingHint.java`
- **严重程度**：一般
- **改进建议**：在 §3.2（处方审核）和 §3.3（病历生成）中补充上述 DTO 的字段定义。至少应明确：LocalRuleResult 每条规则独立的检查结果（ruleId、passed、message、severity）及聚合方式；AuditIssue 的 fieldName、issueDescription、ruleId、severity；AuditResponse 的 riskLevel、issues（AuditIssue 列表）、fromFallback；FieldMissingHint 的 missingField、promptMessage、suggestedAction。

- **问题描述**：DosageThresholdService 四级匹配策略未定义"记录不存在"时的行为，RX_ASSIST_DOSE_STANDARD_NOT_FOUND 触发场景不明
- **所在位置**：§8.4 匹配优先级 + §4.4 check-dose 流程 + §5.1 错误码表
- **严重程度**：一般
- **改进建议**：在 §8.4 中补充第 5 种结局：四级均未命中时返回 DosageAlert（level=WARN）并携带 RX_ASSIST_DOSE_STANDARD_NOT_FOUND 错误码，同时在前端展示"该药品剂量标准未配置，请核实剂量"的降级提示。在 §4.4 check-dose 流程中补充此分支路径。

- **问题描述**：DosageAlert 告警级别（alertLevel）枚举值未定义，影响前端渲染策略
- **所在位置**：§3.4 DosageAlert 类描述字段"级别"
- **严重程度**：一般
- **改进建议**：在 §3.4 中补充 DosageAlert.alertLevel 枚举定义。建议值：INFO（信息提示，不影响流程）、WARN（警告剂量，需确认）、CRITICAL（危险剂量，强制阻断）。并明确 alertLevel=CRITICAL 时是否联动触发 PrescriptionAuditService 的 BLOCK 审核。

- **问题描述**：check-dose 请求缺乏用药频率（frequency）字段，无法支持日剂量上限校验
- **所在位置**：§4.4 check-dose API 请求参数 `{ drugCode, dosage, unit, routeOfAdministration, patientAge, patientWeight }`
- **严重程度**：一般
- **改进建议**：在 DosageCheckRequest 中增加 frequency 字段（枚举或整数次/日），DosageStandard 实体增加 dailyMax 字段（日剂量上限），DosageThresholdService.check() 中增加日剂量校验分支。若确认日剂量校验仅在处方审核阶段进行，则应在设计文本中显式说明此分层策略。

- **问题描述**：MedicalRecord 实体缺少就诊关联标识（visitId/registrationId），病历数据存在追溯缺口
- **所在位置**：§3.3 MedicalRecord 实体字段描述 + RecordGenerateRequest DTO
- **严重程度**：一般
- **改进建议**：在 MedicalRecord 实体中增加 visitId（必填字段，关联 Registration/Visit 记录），在 RecordGenerateRequest 中也增加 visitId 作为必填参数。

- **问题描述**：PrescriptionAssistResponse 字段定义不完整，§4.4 响应体与目录注释存在差异
- **所在位置**：§2.1 目录注释 + §4.4 响应体
- **严重程度**：轻微
- **改进建议**：修正 §2.1 中 PrescriptionAssistResponse.java 的注释为 `# 含 alerts（DosageAlert 列表）和 taskId`，并在 §3.4 中新增 PrescriptionAssistResponse 的字段定义。

- **问题描述**：设计文本中缺少 AuditRiskLevel 与需求文档风险等级术语的映射说明
- **所在位置**：§3.2 AuditRiskLevel 枚举定义
- **严重程度**：轻微
- **改进建议**：在 §3.2 AuditRiskLevel 定义处增加一行消息映射说明，如 `BLOCK（对应需求文档的"高风险"）`、`WARN（对应"中风险"）`、`PASS（对应"低风险"）`，并在 §4.2 WARN 分支中显式说明"允许医生强制提交并留痕"以对齐需求中 MEDIUM 等级的三种可选操作。
