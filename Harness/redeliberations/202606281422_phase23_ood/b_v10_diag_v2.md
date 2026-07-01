# 质量审查报告：Phase 2/3 OOD 设计方案（v10）— 第2版

## 审查范围

- 审查对象：`a_v10_copy_from_v9.md`
- 审查视角：落地可编码性、需求响应充分度、事实正确性、完整性
- 重点侧重：内部审议未充分覆盖的外部维度（需求响应充分度、整体深度和完整性、异常边界、事实正确性）

---

## 第一部分：上一轮发现问题（仍有效，产出未更新）

### 1. [严重] CRITICAL 剂量告警在提交流程的阻断链路不完整

**问题描述**：
§3.4 定义 DosageAlertLevel.CRITICAL 写入 PrescriptionDraftContext，"供处方提交时 BLOCK 判定消费"。但 §4.2 处方提交端点的行为契约中，只定义了"最新审核结果为 BLOCK 时拒绝提交"，完全没有提及 PrescriptionDraftContext 的消费路径。即：check-dose 产出的 CRITICAL 级别告警实际没有在提交流程中被消费，阻断链路存在断裂。

此外，PrescriptionDraftContext 的更新语义未定义——当 check-dose 陆续返回不同结果（如首检 CRITICAL、调整剂量后复检正常）时，旧 CRITICAL 标记是否会因新检查结果而被清除，缺乏契约定义。若不做清除，old stale CRITICAL 可能长期残留，导致正常处方被误阻断。

**所在位置**：
- §3.4 DosageAlertLevel 职责描述
- §4.2 处方提交端点行为（第 1/2/3 条）
- §3.4 PrescriptionDraftContext 清理时机

**改进建议**：
1. 在 §4.2 处方提交端点行为中补充：提交前检查 PrescriptionDraftContext 中该 prescriptionId 是否存在 CRITICAL 级别告警
2. 在 §4.4 check-dose 流程中明确每次调用时根据结果重新计算并覆盖 CRITICAL 标记
3. 在 §3.4 DosageThresholdService 或 PrescriptionDraftContext 中补充此覆盖更新行为契约

---

### 2. [严重] POST /api/prescription/submit 端点缺少 Controller 归属

**问题描述**：
§4.2 定义了 `POST /api/prescription/submit` 端点的完整契约（含 forceSubmit、处方版本校验、BLOCK 阻断等），这是一个新增的 REST 端点。但 §2.1 目录结构中只列出了 `PrescriptionAuditController`（audit 子包）和 `PrescriptionAssistController`（assist 子包），submit 端点没有被分配至任何一个 Controller。

**所在位置**：
- §4.2 处方提交端点行为契约
- §2.1 目录结构 `prescription/api/` 下两个 Controller 均未包含 submit 端点

**改进建议**：
在 §2.1 目录结构中新增 `PrescriptionSubmitController` 或明确将 submit 端点归入现有 Controller（推荐归入 PrescriptionAuditController）。

---

### 3. [一般] AllergyWarningItem 与 AllergyWarning 命名不一致，且业务层 DTO 字段定义缺失

**问题描述**：
§2.1 目录列出 `AllergyWarning.java`，但 §3.4 PrescriptionAssistResponse 引用的是 `AllergyWarningItem`。此外，业务层 AllergyWarning DTO 的字段定义在各处均未出现。

**所在位置**：
- §2.1 目录：`dto/assist/DoseWarning.java, AllergyWarning.java`
- §3.4 PrescriptionAssistResponse：`allergyWarnings，List\<AllergyWarningItem\>`
- §10.4：仅定义 ai-api 层 AllergyWarningItem

**改进建议**：
统一命名，并在 §3.4 或 §1.3 中补充业务层 AllergyWarning DTO 的完整字段定义。

---

### 4. [一般] DoseWarning 业务层 DTO 字段定义缺失

**问题描述**：
§3.4 PrescriptionAssistResponse 包含 `doseWarnings，List\<DoseWarning\>`，§2.1 目录列出 `DoseWarning.java`，但设计文本中没有任何节段定义业务层 DoseWarning 的字段结构。

**所在位置**：
- §3.4 PrescriptionAssistResponse 字段描述仅提及"剂量告警列表（doseWarnings，List\<DoseWarning\>）"，无字段定义
- §1.3 核心抽象一览缺失 DoseWarning 条目

**改进建议**：
在 §3.4 或 §1.3 中补充 DoseWarning DTO 字段定义，至少包含：drugId、warningType（枚举，对齐需求文档 3.4.10 的 `OVER_SINGLE_DOSE`/`OVER_DAILY_DOSE`/`OVER_DURATION`）、message、severity。

---

### 5. [轻微] MedicalRecord 实体部分字段缺少显式字段名

**问题描述**：
§3.3 MedicalRecord 字段描述中，"记录标识"、"就诊科室"、"医生 ID" 三个字段只有中文含义说明，没有给出 Java 字段名。

**所在位置**：
- §3.3 MedicalRecord 核心字段描述

**改进建议**：
补齐 Java 字段名：记录标识 → `recordId`、就诊科室 → `departmentId`、医生 ID → `doctorId`。

---

## 第二部分：本轮新增问题（基于系统化需求响应验证）

> 本部分针对上一轮质询的反馈，对设计进行了需求响应充分度和事实正确性的专项验证。

### 6. [严重 - 事实正确性] 业务层 DosageAlert 缺少 warningType 字段，导致需求 3.4.10 字段级契约断裂

**问题描述**：
需求文档 3.4.10 的输出契约明确定义 `dose_warnings` 数组中每项包含 `warning_type`（枚举值：`OVER_SINGLE_DOSE` / `OVER_DAILY_DOSE` / `OVER_DURATION`）。设计文档在 §10.4 ai-api 层定义了 `DoseWarningItem：drugId / warningType / message / severity`，正确包含了 warningType。

然而，§3.4 业务层 `DosageAlert` 只定义了 `alertLevel`（DosageAlertLevel 枚举：INFO/WARN/CRITICAL）、`alertMessage`、`drugCode`、`currentDosage`、`suggestedValue`、`errorCode`——**没有任何 warningType 字段**。`DosageThresholdService`（§3.4）的描述只提到"比较剂量阈值"和"日剂量校验"，没有提及告警类型的分类逻辑或输出路径。

这意味着：
- 业务层 DosageAlert 无法携带告警类型（单次超量/日剂量超量/疗程超量），Converter 无数据来源填充 DoseWarningItem.warningType
- `DosageThresholdService` 的剂量校验结果无法区分`OVER_SINGLE_DOSE`、`OVER_DAILY_DOSE` 和 `OVER_DURATION`，即使内部逻辑能区分阈值来源，其输出值对象（DosageAlert）也未提供承载字段
- `OVER_DURATION`（疗程时长超限）在整个设计中没有对应实现——DosageThresholdService 仅描述"日剂量校验"，未提及疗程时长检查

**所在位置**：
- §3.4 DosageAlert 字段定义
- §3.4 DosageThresholdService 职责描述
- §10.4 ai-api 层 DoseWarningItem.warningType

**严重程度**：严重——编码者实现 DosageAlert 后，Converter 无法完成 `DosageAlert` → `DoseWarningItem` 的 warningType 映射，编译时不会报错但运行时 warningType 永远为 null，导致前端无法按告警类别区分展示。

**改进建议**：
1. 在 `DosageAlert` 中增加 `warningType` 字段（建议独立枚举 `DoseWarningType：OVER_SINGLE_DOSE / OVER_DAILY_DOSE / OVER_DURATION`）
2. 在 `DosageThresholdService` 的描述中明确剂量校验的三种输出路径及对应的 warningType 赋值规则
3. 补充 `OVER_DURATION` 的实现说明——若 Phase 2/3 暂不实现疗程时长检查，应在设计决策中显式声明，并在 ai-api 层说明该类型当前不会产生

---

### 7. [一般 - 需求响应充分度] RecordGenerateRequest 缺少 dialogueText 的 50–10000 字符约束

**问题描述**：
需求文档 3.4.3 输入契约明确规定 `dialogue_text` 的字符数约束为 50–10000。§3.3 RecordGenerateRequest 的描述只列出了字段名称（dialogueText、patientId、encounterId、stream），未提及该字符数约束。

对比之下，DialogueCreateRequest（§3.1）的 chiefComplaint 字段明确标注了"字符数 5–500"的约束。RecordGenerateRequest 的约束缺失导致编码者不清楚是否需要在 Controller/Service 层面做输入校验。

**所在位置**：
- §3.3 RecordGenerateRequest 描述
- 需求文档 3.4.3 输入契约

**严重程度**：一般——约束缺失可能导致数据库/API 层面收到超长文本时出现未预期行为，但编码者在实现时可通过查阅需求文档补全。

**改进建议**：
在 §3.3 RecordGenerateRequest 的 dialogueText 字段后追加"(必填，字符数 50–10000)"约束说明。

---

### 8. [一般 - 事实正确性] Phase 5 迁移透明性断言缺少条件限定

**问题描述**：
§1.1 设计目标中声明："业务模块仅依赖 ai-api 的 AiService 接口（编译期依赖），Phase 5 迁移时仅需替换 ai-impl 内的 AiService 实现类，业务模块代码无须修改"。

此断言成立的前提是：Phase 5 的 ai-api 层 DTO（作为 AiService 方法参数）不发生变化。然而，§10 本身已说明 ai-api 层 DTO 的扩展是在本设计阶段"从空壳类扩展完整字段"——这证明 ai-api DTO 是会演变的。§4.5 进一步说明各模块的 Converter 类依赖于 ai-api DTO 结构。如果 Phase 5 需要修改 ai-api DTO（例如调整字段结构以适配底座编排层），则业务模块的 Converter 也需要同步修改，"无须修改"的断言将不再成立。

Phase 5 包 G 的参考 OOD（`a_v22_copy_from_v21.md`）的 §1.2 明确表达"在现有 AiService 接口不变的前提下"——这确实支持了 "AiService 接口"不变的前景，但 DTO 字段的变化是独立于接口签名变化的问题。接口不变不一定意味着 DTO 结构不变。

**所在位置**：
- §1.1 设计目标："业务模块代码无须修改"
- §10 ai-api 层 DTO 扩展规格（暗示 DTO 会演进）
- §4.5 Converter 类对 ai-api DTO 的依赖

**严重程度**：一般——这不是一个会阻止实现的错误，但该断言当前以绝对语气表述，对架构决策者可能产生误导。真实情况是：Phase 5 迁移对业务模块的改动量取决于 ai-api DTO 的变化范围。

**改进建议**：
将"业务模块代码无须修改"修订为有条件的表述，例如："业务模块编译期依赖仅限 ai-api 的 AiService 接口；若 Phase 5 保持 AiService 接口签名和 DTO 字段结构不变，业务模块代码无须修改"。

---

## 第三部分：需求响应充分度系统验证摘要

以下为对需求文档 4 个包功能要求及 4 条关键约束的逐项验证结果：

### 功能要求覆盖检查

| 包 | 需求条目 | 设计对应 | 覆盖状态 |
|---|---------|---------|---------|
| 包C | 单轮/多轮双对话模式 | TriageService + DialogueSession + DialogueSessionManager | ✅ 充分覆盖 |
| 包C | 规则可配置 | TriageRuleEngine + TriageRule 实体（数据库 + Caffeine） | ✅ 充分覆盖 |
| 包C | Mock 兜底回退科室列表 | StaticDepartmentFallbackProvider | ✅ 充分覆盖 |
| 包C | 架构约束：落地底座 | consultation 模块 | ✅ 满足 |
| 包D-AI1 | 风险等级差异化阻断 | AuditRiskLevel enum + PrescriptionAuditEnforcer + HTTP 422 | ✅ 充分覆盖 |
| 包D-AI1 | AI 超时回退本地规则校验打标 | LocalRuleEngine + 5 规则 + fromFallback | ✅ 充分覆盖 |
| 包D-AI1 | 架构约束：落地底座 | prescription 模块 | ✅ 满足 |
| 包D-AI2 | 对话转结构化病历 | MedicalRecordService + AiService + MedicalRecordField | ✅ 充分覆盖 |
| 包D-AI2 | 按科室配置规则 | DepartmentTemplateConfig + TemplateConfigManager | ✅ 充分覆盖 |
| 包D-AI2 | 关键字段缺失提示补全 | MissingFieldDetector + FieldMissingHint | ✅ 充分覆盖 |
| 包D-AI2 | 架构约束：落地底座 | medical-record 模块 | ✅ 满足 |
| 包E | 剂量阈值告警 | DosageThresholdService + DosageAlert | ✅ 覆盖（但见问题 #6） |
| 包E | 与处方审核强耦合同步落地 | 同一 prescription 模块 | ✅ 满足 |
| 包E | 架构约束：落地底座 | prescription 模块 | ✅ 满足 |

### 关键约束检查

| 约束 | 设计对应 | 状态 |
|-----|---------|------|
| 所有包落地在底座上（非独立接入） | 三模块均为底座 Maven 模块 | ✅ 满足 |
| 包E与包D-AI1强耦合同步落地 | 共享 prescription 模块，DosageStandard 迁至 common | ✅ 满足 |
| 与 Phase0/Phase1ABD 设计风格一致 | 章节结构、抽象粒度、类型形态选择逻辑、模块结构一致 | ✅ 满足 |
| 参考 Docs 目录文档 | §1.1 提及 Phase 1 架构风格，§7 设计决策参考了已有模式 | ✅ 满足 |

### 关键约束检查补充说明

**约束 #4 参考 Docs 目录文档**：设计文档在 §1.1 声明"架构风格与 Phase 0（骨架模块）、Phase 1（认证模块）的风格保持一致"，实际对比 Phase 1 OOD（`05_ood_phase1_B.md`），本设计的章节结构（概述→模块划分→核心抽象→关键行为契约→并发设计→设计决策）、抽象描述粒度（职责+协作+类型形态选择理由）和设计决策记录格式（决策/选择/理由表）均保持一致。§5.5 AI 超时配置的来源标注了需求文档章节号。§7 设计决策覆盖的错误码命名规则、allergy_details 过渡方案均对接了需求文档约定。整体满足约束要求。

---

## 整体质量评价

该设计文档经 10 轮审议迭代后，在需求对齐、异常覆盖和细节完备性上已达到较高成熟度。经过本轮对需求响应充分度和事实正确性的专项验证，确认全部 14 项功能要求及 4 条关键约束均有对应覆盖设计。新增的 3 项问题集中在：（a）DosageAlert 缺少 warningType 字段导致的字段级契约断裂，（b）RecordGenerateRequest 字符约束缺失，（c）Phase 5 迁移透明性断言的条件限定不足。严重级问题从上一轮的 2 项增至 3 项（新增问题 #6），建议优先修复问题 #6 和问题 #1（CRITICAL→BLOCK 链路断裂），这两个问题涉及处方安全闭环的完整性，修复后可使提交流程的阻断语义形成完整链路。

---

## 修订说明（v2）

| 质询意见 | 回应 |
|---------|------|
| 报告声明覆盖"需求响应充分度"与"事实正确性"但未产生对应维度发现 | 已在本版报告新增第三部分"需求响应充分度系统验证摘要"，对全部 14 项功能要求和 4 条关键约束进行逐项核查并生成验证结论表。同时新增 3 个问题（#6/#7/#8）覆盖事实正确性维度，其中问题 #6（DosageAlert 缺少 warningType）为严重级，直接对应需求 3.4.10 字段级契约的遗漏 |
| 整体评价"需求对齐已达到较高成熟度"缺乏证据支撑 | 已在本版报告第三部分以系统验证表形式提供逐项证据，确认全部 14 项功能要求和 4 条关键约束均有对应覆盖；同时指明约束 #4 的具体验证过程 |
