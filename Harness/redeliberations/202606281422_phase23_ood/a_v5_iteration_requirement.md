根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

1. **[严重] AiSuggestionResult 状态枚举缺少 FAILED 状态，异步 AI 失败时产生"幽灵 pending"场景**
   - 所在位置：§3.4 AiSuggestionResult 查询契约 + §6.3 异步 AI 建议
   - 改进建议：在 AiSuggestionResult 中新增 FAILED 状态枚举值，含可选的 failReason 字段。实现侧采用"预创建→更新"模式：check-dose 响应前以 PENDING 状态预创建 AiSuggestionResult 条目，异步回调完成后更新为 COMPLETED 或 FAILED；异常被 CompletableFuture exceptionally 处理器捕获时同样写入 FAILED 状态而非留下 PENDING 僵尸条目。前端根据 FAILED 状态展示明确失败提示。

2. **[中] TriageRequest DTO 存在于目录结构但设计文本中无定义，与 DialogueCreateRequest 关系不明确**
   - 所在位置：§2.1 目录列出了 `dto/TriageRequest.java`，但 §1.3 和 §3.1 的核心抽象定义中仅有 DialogueCreateRequest 而无 TriageRequest
   - 改进建议：推荐路径 A——从目录中删除 TriageRequest.java 条目，统一使用 DialogueCreateRequest 作为分诊请求 DTO，并在 §3.1 DialogueCreateRequest 的说明中补充 sessionId 字段。

3. **[中] LocalRuleResult / AuditIssue / AuditRequest / AuditResponse / FieldMissingHint 等关键 DTO 存在于目录但设计文本无定义**
   - 所在位置：§2.1 目录列出但全文未定义：`prescription/rule/LocalRuleResult.java`、`prescription/dto/audit/AuditIssue.java`、`prescription/dto/audit/AuditRequest.java`、`prescription/dto/audit/AuditResponse.java`、`medical-record/dto/FieldMissingHint.java`
   - 改进建议：在 §3.2（处方审核）和 §3.3（病历生成）中补充上述 DTO 的字段定义。至少应明确：LocalRuleResult（ruleId、passed、message、severity 及聚合方式）；AuditIssue（fieldName、issueDescription、ruleId、severity）；AuditResponse（riskLevel、issues 列表、fromFallback 标记）；FieldMissingHint（missingField、promptMessage、suggestedAction）。

4. **[中] DosageThresholdService 四级匹配策略未定义"记录不存在"时的行为，RX_ASSIST_DOSE_STANDARD_NOT_FOUND 触发场景不明**
   - 所在位置：§8.4 匹配优先级 + §4.4 check-dose 流程 + §5.1 错误码表
   - 改进建议：在 §8.4 中补充第 5 种结局：四级均未命中时返回 DosageAlert（level=WARN）并携带 RX_ASSIST_DOSE_STANDARD_NOT_FOUND 错误码，前端展示"该药品剂量标准未配置，请核实剂量"的降级提示。在 §4.4 check-dose 流程中补充此分支路径。

5. **[中] DosageAlert 告警级别（alertLevel）枚举值未定义，影响前端渲染策略**
   - 所在位置：§3.4 DosageAlert 类描述字段"级别"
   - 改进建议：在 §3.4 中补充 DosageAlert.alertLevel 枚举定义。建议值：INFO（信息提示，不影响流程）、WARN（警告剂量，需确认）、CRITICAL（危险剂量，强制阻断）。并明确 alertLevel=CRITICAL 时是否联动触发 PrescriptionAuditService 的 BLOCK 审核。

6. **[中] check-dose 请求缺乏用药频率（frequency）字段，无法支持日剂量上限校验**
   - 所在位置：§4.4 check-dose API 请求参数 `{ drugCode, dosage, unit, routeOfAdministration, patientAge, patientWeight }`
   - 改进建议：在 DosageCheckRequest 中增加 frequency 字段（枚举或整数次/日），DosageStandard 实体增加 dailyMax 字段（日剂量上限），DosageThresholdService.check() 中增加日剂量校验分支。若确认日剂量校验仅在处方审核阶段进行，则应在设计文本中显式说明此分层策略。

7. **[中] MedicalRecord 实体缺少就诊关联标识（visitId/registrationId），病历数据存在追溯缺口**
   - 所在位置：§3.3 MedicalRecord 实体字段描述 + RecordGenerateRequest DTO
   - 改进建议：在 MedicalRecord 实体中增加 visitId（必填字段，关联 Registration/Visit 记录），在 RecordGenerateRequest 中也增加 visitId 作为必填参数。

8. **[低] PrescriptionAssistResponse 字段定义不完整，§4.4 响应体与目录注释存在差异**
   - 所在位置：§2.1 目录注释 + §4.4 响应体
   - 改进建议：修正 §2.1 中 PrescriptionAssistResponse.java 的注释为 `# 含 alerts（DosageAlert 列表）和 taskId`，并在 §3.4 中新增 PrescriptionAssistResponse 的字段定义。

9. **[低] 设计文本中缺少 AuditRiskLevel 与需求文档风险等级术语的映射说明**
   - 所在位置：§3.2 AuditRiskLevel 枚举定义
   - 改进建议：在 §3.2 AuditRiskLevel 定义处增加映射说明，如 `BLOCK（对应需求文档的"高风险"）`、`WARN（对应"中风险"）`、`PASS（对应"低风险"）`，并在 §4.2 WARN 分支中显式说明"允许医生强制提交并留痕"以对齐需求中 MEDIUM 等级的三种可选操作。

## 历史迭代回顾

### 已解决的问题（出现在历史反馈但当前反馈中不再提及的问题）

- 迭代第1轮 问题1（DialogueSession 不可变声明与可变追加操作矛盾）：v2 已将 DialogueSession 改为可变 class，v4 报告未再提及
- 迭代第1轮 问题2（包E 异步 AI 建议缺少消费路径）：v2 补充了 GET /api/prescription/assist/suggestion/{taskId} 查询端点，v4 报告未再提及该缺失
- 迭代第1轮 问题3（多轮分诊对话历史维护责任不明确）：v2 明确服务端 DialogueSession 为单一真相来源，v4 报告未再提及
- 迭代第1轮 问题4（DosageCheckRequest 缺少给药途径参数）：v2 已补充 routeOfAdministration 字段，v4 报告未再提及
- 迭代第1轮 问题5（DosageStandard 写权限归属未定义）：v3 明确 admin 模块为唯一写入者，v4 报告未再提及
- 迭代第1轮 问题6（分诊规则配置变更生效机制未定义）：v3 引入 Caffeine refreshAfterWrite + 事件驱动缓存刷新，v4 报告未再提及
- 迭代第1轮 问题7（科室模板配置 CRUD 和默认兜底缺失）：v3 补充 DEFAULT 兜底和 TemplateConfigManager，v4 报告未再提及
- 迭代第1轮 问题8（对话会话内存存储未覆盖服务重启场景）：v2/v3 引入 TRIAGE_SESSION_EXPIRED 错误码和 findOrCreate 三分支模式，v4 报告未再提及
- 迭代第1轮 问题9（新模块依赖声明未包含 common-module-api）：v3/v4 依赖关系图已补充，v4 报告未再提及
- 迭代第1轮 问题10（剂量标准数据初始化方案和编码规范缺失）：v3 补充 §8 种子脚本和药品编码规范，v4 报告未再提及
- 迭代第2轮 问题1（BLOCK 风险等级缺少后端强制阻断执行机制）：v3 新增 PrescriptionAuditEnforcer 接口，v4 报告未再提及
- 迭代第2轮 问题2（AuditRecord 缺少处方级关联标识）：v3 补充 prescriptionOrderId/doctorId/patientId 必填字段，v4 报告未再提及
- 迭代第2轮 问题3（AiSuggestionResult 内存存储服务重启丢失）：v3/v4 引入 TTL + NOT_FOUND 三分支处理，v4 报告未再提及重启场景（已转向 FAILED 状态缺失的新问题）
- 迭代第2轮 问题4（DosageStandard 未定义年龄/体重分级结构）：v3 新增 ageMin/ageMax/weightMin/weightMax 字段及四级匹配，v4 报告未再提及结构缺失
- 迭代第2轮 问题5（病历生成降级策略不合理）：v3 改为分层保护策略，v4 报告未再提及
- 迭代第3轮 问题1（AuditRecord 降级路径落库遗漏）：v4 显式补充降级路径 AuditRecord 持久化（fromFallback=true），v4 报告未再提及
- 迭代第3轮 问题2（分诊降级链路 AI 空且规则空时行为未定义）：v4 改为线性三级降级链，v4 报告未再提及
- 迭代第3轮 问题3（TriageResponse DTO 字段结构未定义）：v4 补充了 TriageResponse 和 DialogueCreateRequest 的完整字段描述，v4 报告未再提及
- 迭代第3轮 问题4（剂量单位转换规则集未定义）：v4 补充 DosageUnitGroup 枚举，v4 报告未再提及
- 迭代第3轮 问题5（MedicalRecord 实体字段未定义）：v4 新增 MedicalRecordField 枚举和实体字段描述，v4 报告未再提及
- 迭代第3轮 问题6（规则/模板配置变更缺少审计溯源）：v4 新增 ConfigChangeLog 实体和事件处理链，v4 报告未再提及

### 持续存在的问题（在多轮反馈中反复出现的问题，需重点解决）

- **DTO 定义完整性问题持续演进**：迭代1轮问题7（模板 CRUD）→迭代2轮问题2（AuditRecord 关联标识）→迭代3轮问题3（TriageResponse 字段）→迭代4轮问题2/3/8（TriageRequest 歧义、多个处方审核/病历生成 DTO 未定义、PrescriptionAssistResponse 注释不完整）。DTO 定义缺失是跨迭代反复出现的模式，本轮须系统性解决§2.1目录中列出的所有 DTO 的字段定义，不留遗漏。

### 新发现的问题（本轮新识别的问题）

- 问题1（FAILED 状态缺失）：之前迭代仅覆盖了服务重启场景（NOT_FOUND），本轮新发现异步执行期异常场景（CompletableFuture exceptionally complete）导致 PENDED 僵尸条目，属于之前审议未覆盖的边界场景
- 问题4（DosageStandard 记录不存在的第5种结局）：之前迭代定义了四级匹配策略，但四级均未命中的场景是新发现的空白
- 问题5（DosageAlert.alertLevel 枚举未定义）：新增字段缺乏枚举值定义
- 问题6（check-dose 缺乏 frequency 字段）：日剂量校验需求被遗漏
- 问题7（MedicalRecord 缺少 visitId）：跨模块数据追溯的关联标识缺失
- 问题9（AuditRiskLevel 与需求术语映射缺失）：需求验收层面的术语对齐问题

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281422_phase23_ood\a_v4_design_v2.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281422_phase23_ood\requirement.md