# 质量审查报告：Phase 2/3 包C/D-AI1/D-AI2/E OOD 设计方案（v4）

## 审查范围

- 审查视角：需求响应充分度、事实错误与逻辑矛盾、深度与完整性、实际落地可行性
- 审查前提：技术可行性等维度已由内部审议（设计-验证循环）覆盖，本报告侧重内部审议未充分覆盖的维度
- 当前迭代：第 4 轮

---

## 发现的问题

### 问题 1：AiSuggestionResult 状态枚举缺少 FAILED 状态，异步 AI 失败时产生"幽灵 pending"场景

- **所在位置**：§3.4 AiSuggestionResult 查询契约 + §6.3 异步 AI 建议
- **严重程度**：严重
- **问题描述**：AiSuggestionResult 定义了 PENDING / COMPLETED 状态，加上 NOT_FOUND（TTL 过期或被清理）共三分支，但缺少 FAILED 状态。当 `@Async / CompletableFuture.runAsync()` 调用的 `AiService.prescriptionAssist()` 执行过程中抛出异常（AI 服务 500、网络超时、JSON 解析失败等），CompletableFuture 将 exceptionally complete，其结果是：
  - 若实现侧在异步调用前创建 PENDING 条目（合理路径），该条目将永久停留在 PENDING 状态，前端轮询时持续展示"AI 建议生成中"且永不完成；
  - 若实现侧仅在异步成功后才创建条目，则 AI 调用失败时条目根本不存在，前端看到 NOT_FOUND，无从区分"AI 建议暂不可用"与"从未发起过 AI 调用"。
  两种后果均不满足医疗场景的可观测性要求。医生需要明确知道"AI 建议生成失败"而非"仍在生"或"不可用"的模糊状态。内部审议第 2 轮仅覆盖了服务重启场景（内存丢失），未覆盖执行期异常场景。
- **改进建议**：在 AiSuggestionResult 中新增 FAILED 状态枚举值，含可选的 failReason 字段。实现侧采用"预创建→更新"模式：check-dose 响应前以 PENDING 状态预创建 AiSuggestionResult 条目，异步回调完成后更新为 COMPLETED 或 FAILED；异常被 CompletableFuture exceptionally 处理器捕获时同样写入 FAILED 状态而非留下 PENDING 僵尸条目。前端根据 FAILED 状态展示明确失败提示。

### 问题 2：TriageRequest DTO 存在于目录结构但设计文本中无定义，与 DialogueCreateRequest 关系不明确

- **所在位置**：§2.1 目录列出了 `dto/TriageRequest.java`，但 §1.3 和 §3.1 的核心抽象定义中仅有 DialogueCreateRequest 而无 TriageRequest
- **严重程度**：中
- **问题描述**：设计文本定义了 DialogueCreateRequest（含 chiefComplaint、patientId、age、gender），API 示例如 §4.1 中 POST /api/triage/consult 的请求体。但目录中同时存在 TriageRequest.java 文件。开发者无法判断：
  - TriageRequest 与 DialogueCreateRequest 是同一事物的两个名字？（冗余）
  - TriageRequest 是通用的分诊请求 DTO，DialogueCreateRequest 是其子集或别名？
  - 多轮追问场景的第二次及后续请求应该使用哪个 DTO？
  目录结构与文本定义不一致直接导致编码阶段出现二义性。
- **改进建议**：任选以下清除路径之一：
  - 路径 A（推荐）：从目录中删除 TriageRequest.java 条目，统一使用 DialogueCreateRequest 作为分诊请求 DTO，并在 §3.1 DialogueCreateRequest 的说明中补充 sessionId 字段（当前仅隐含提及"复用已有 sessionId"但未列为字段）。
  - 路径 B：保留 TriageRequest 作为分诊请求的顶层 DTO（含 sessionId、chiefComplaint 等字段），明确其与 DialogueCreateRequest 的差异和各自的适用场景，在 §3.1 中补充定义。

### 问题 3：LocalRuleResult / AuditIssue / AuditRequest / AuditResponse / FieldMissingHint 等关键 DTO 存在于目录但设计文本无定义

- **所在位置**：§2.1 目录列出了以下文件但全文未定义其字段：
  - `prescription/rule/LocalRuleResult.java`
  - `prescription/dto/audit/AuditIssue.java`
  - `prescription/dto/audit/AuditRequest.java`
  - `prescription/dto/audit/AuditResponse.java`
  - `medical-record/dto/FieldMissingHint.java`
- **严重程度**：中
- **问题描述**：这些 DTO 是处方案例审核和病历生成的核心接口契约。其中：
  - `LocalRuleResult` 是 LocalRuleEngine.check() 的返回类型，定义了本地规则校验的结果结构（单条规则结果？多条聚合？如何映射到 AuditIssue？），设计文本未定义其字段导致编码实现面临随意假设风险；
  - `AuditIssue` 是审核问题的结构化表达（字段名、问题描述、匹配规则标识），处方审核场景 §4.2 和阻断响应 §5.3 依赖此结构，但无定义；
  - `AuditRequest`/`AuditResponse` 是审核 API 的输入/输出契约，缺少字段定义使下游消费者（前端）无法确定接口规范；
  - `FieldMissingHint` 是病历生成缺失字段提示 DTO，缺少定义使前端补全提示交互无契约约束。
- **改进建议**：在 §3.2（处方审核）和 §3.3（病历生成）中补充上述 DTO 的字段定义。至少应明确：
  - `LocalRuleResult`：每条规则独立的检查结果（ruleId、passed、message、severity），及多条规则的聚合方式（全部通过=通过，任一阻断=阻断）；
  - `AuditIssue`：字段名（fieldName）、问题描述（issueDescription）、匹配规则标识（ruleId）、严重程度（severity）；
  - `AuditResponse`：riskLevel、issues（AuditIssue 列表）、fromFallback 标记；
  - `FieldMissingHint`：missingField（MedicalRecordField）、promptMessage、suggestedAction。

### 问题 4：DosageThresholdService 四级匹配策略未定义"记录不存在"时的行为，RX_ASSIST_DOSE_STANDARD_NOT_FOUND 触发场景不明

- **所在位置**：§8.4 匹配优先级 + §4.4 check-dose 流程 + §5.1 错误码表
- **严重程度**：中
- **问题描述**：§8.4 定义了四级剂量标准匹配优先级，第 4 级降级为"无分级限制的通用剂量"。但此前提是 DosageStandard 表中存在 drugCode + routeOfAdministration 的记录且 ageMin/weightMin 均为 null。当某药品完全没有配置 DosageStandard 记录（新药上线、种子数据遗漏等场景），四级匹配均无法命中时的行为未定义——DosageThresholdService 应返回空警报、触发 RX_ASSIST_DOSE_STANDARD_NOT_FOUND 错误码、还是抛异常？§5.1 错误码表虽列出了"剂量标准不存在"（RX_ASSIST_ 前缀），但 §4.4 check-dose 流程和 §8.4 匹配逻辑中均未引用该错误码，编码者无法确定正确的错误响应路径。
- **改进建议**：在 §8.4 中补充第 5 种结局：四级均未命中时返回 DosageAlert（level=WARN）并携带 RX_ASSIST_DOSE_STANDARD_NOT_FOUND 错误码，同时在前端展示"该药品剂量标准未配置，请核实剂量"的降级提示。在 §4.4 check-dose 流程中补充此分支路径。

### 问题 5：DosageAlert 告警级别（alertLevel）枚举值未定义，影响前端渲染策略

- **所在位置**：§3.4 DosageAlert 类描述字段"级别"
- **严重程度**：中
- **问题描述**：DosageAlert 描述中注明含"级别"字段，但设计文本未定义允许的枚举值。实际编码时开发者可能随意定义（如使用字符串"WARNING"/"CRITICAL"或整数 1/2/3），导致前端告警级别与后端不匹配。前端需要根据告警级别决定渲染颜色（红/黄/橙）和行为（阻断/提示/仅信息），无定义造成前后端对接风险。
- **改进建议**：在 §3.4 中补充 DosageAlert.alertLevel 枚举定义。建议值：INFO（信息提示，不影响流程）、WARN（警告剂量，需确认）、CRITICAL（危险剂量，强制阻断）。并明确 alertLevel=CRITICAL 时是否联动触发 PrescriptionAuditService 的 BLOCK 审核。

### 问题 6：check-dose 请求缺乏用药频率（frequency）字段，无法支持日剂量上限校验

- **所在位置**：§4.4 check-dose API 请求参数 `{ drugCode, dosage, unit, routeOfAdministration, patientAge, patientWeight }`
- **严重程度**：中
- **问题描述**：需求文档 §3.2.2.6 明确要求剂量校验需同时覆盖"单次剂量上限"和"日剂量上限"（"AI 推荐剂量若超出药品知识库标定的'单次剂量上限'或'日剂量上限'，前端需明显告警"）。当前 check-dose 请求仅包含单次剂量（dosage）而无用药频率（如 tid/bid/qd 或具体日频次），DosageThresholdService 仅能校验单次剂量是否超限，无法计算日剂量。日剂量超限的校验场景在设计中完全被遗漏。处方审核（Audit）可能覆盖日剂量，但 check-dose 作为实时辅助开方工具的需求未得到满足。
- **改进建议**：在 DosageCheckRequest 中增加 frequency 字段（枚举或整数次/日），DosageStandard 实体增加 dailyMax 字段（日剂量上限），DosageThresholdService.check() 中增加日剂量校验分支：`dosage * frequency > dailyMax` 时触发告警。若团队确认日剂量校验仅在处方审核阶段进行，则应在设计文本中显式说明此分层策略，并说明 check-dose 仅校验单次剂量。

### 问题 7：MedicalRecord 实体缺少就诊关联标识（visitId/registrationId），病历数据存在追溯缺口

- **所在位置**：§3.3 MedicalRecord 实体字段描述 + RecordGenerateRequest DTO
- **严重程度**：中
- **问题描述**：MedicalRecord 实体定义了 patientId、departmentId、doctorId，但缺少 visitId 或 registrationId（就诊/挂号记录标识）。在平台语境下，同一患者可在同一科室由同一医生多次就诊，无 visitId 则无法将病历关联到特定就诊。Phase 5 参考设计（`AiRequestBase`）中已包含 visitId 作为跨能力共享字段，证明系统有就诊上下文概念的规划。MedicalRecord 缺乏此关联标识将导致：
  - 病历列表无法按就诊维度组织展示；
  - 跨模块数据追溯断裂（处方、审核记录、病历三者无法以统一就诊标识关联）；
  - 后续 Phase 5 迁移时需倒填 visitId 字段，增加迁移成本。
  RecordGenerateRequest 同样缺少 visitId，导致服务层无法获取该上下文。
- **改进建议**：在 MedicalRecord 实体中增加 visitId（必填字段，关联 Registration/Visit 记录），在 RecordGenerateRequest 中也增加 visitId 作为必填参数。此修改对 Phase 2/3 影响最小（数据库加列、DTO 加字段），避免 Phase 5 迁移时倒填。

### 问题 8：PrescriptionAssistResponse 字段定义不完整，§4.4 响应体与目录注释存在差异

- **所在位置**：§2.1 目录注释 `# 含 taskId` + §4.4 响应 `{ alerts, taskId }`
- **严重程度**：低
- **问题描述**：§2.1 中 PrescriptionAssistResponse.java 仅标注"含 taskId"，但 §4.4 check-dose 的响应体包含 `{ alerts, taskId }` 两个字段。目录注释未提及 alerts 字段，开发者可能只实现 taskId 而遗漏 alerts（同步剂量检查的告警列表）。虽然 alerts 字段可以从流程推断，但显式文档缺失增加了编码遗漏风险。
- **改进建议**：修正 §2.1 中 PrescriptionAssistResponse.java 的注释为  `# 含 alerts（DosageAlert 列表）和 taskId`，并在 §3.4 中新增 PrescriptionAssistResponse 的字段定义。

### 问题 9：设计文本中缺少 AuditRiskLevel 与需求文档风险等级术语的映射说明

- **所在位置**：§3.2 AuditRiskLevel 枚举定义
- **严重程度**：低
- **问题描述**：需求文档 §3.2.2.7 使用 高风险（HIGH）/ 中风险（MEDIUM）/ 低风险（LOW）三级风险等级描述业务语义，并在 §3.2.2.7 中明确定义了 MEDIUM 等级的三种医生操作（强制提交/修改重审/撤销审核）。设计文本以 BLOCK / WARN / PASS 替代，虽语义可映射（BLOCK↔HIGH、WARN↔MEDIUM、PASS↔LOW），但未显式说明映射关系。编码和前端对接阶段，需求的理解者可能在 BLOCK vs HIGH 的命名差异上产生混淆，尤其影响需求验收环节对风险等级行为的判断。
- **改进建议**：在 §3.2 AuditRiskLevel 定义处增加一行消息映射说明，如 `BLOCK（对应需求文档的"高风险"）`、`WARN（对应"中风险"）`、`PASS（对应"低风险"）`，并在 §4.2 WARN 分支中显式说明"允许医生强制提交并留痕"以对齐需求中 MEDIUM 等级的三种可选操作。

---

## 整体评价

该设计文档历经 3 轮内部审议后，核心架构逻辑清晰，关键接口定义趋于稳定，技术方案成熟度较高。上述 9 个问题中，严重级别 1 个、中等级别 6 个、低级别 2 个。最值得关注的是**异步 AI 建议的 FAILED 状态缺失**（问题 1）和**DosageStandard 记录不存在时的未定义行为**（问题 4），前者直接影响医疗场景的可观测性和用户体验，后者在无人值守的生产环境中可能导致静默错误。其余问题集中在 DTO 定义完整性和需求术语对齐方面，修复成本较低但能显著降低编码阶段的沟通成本和返工风险。
