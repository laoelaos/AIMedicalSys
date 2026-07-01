# 质量审查报告：Phase 2/3 OOD 设计方案（v10）

## 审查范围

- 审查对象：`a_v10_copy_from_v9.md`
- 审查视角：落地可编码性、需求响应充分度、事实正确性、完整性
- 重点侧重：内部审议未充分覆盖的外部维度（需求响应充分度、整体深度和完整性、异常边界）

---

## 发现问题

### 1. [严重] CRITICAL 剂量告警在提交流程的阻断链路不完整

**问题描述**：
§3.4 定义 DosageAlertLevel.CRITICAL 写入 PrescriptionDraftContext，"供处方提交时 BLOCK 判定消费"。但 §4.2 处方提交端点的行为契约中，只定义了"最新审核结果为 BLOCK 时拒绝提交"，完全没有提及 PrescriptionDraftContext 的消费路径。即：check-dose 产出的 CRITICAL 级别告警实际没有在提交流程中被消费，阻断链路存在断裂。

此外，PrescriptionDraftContext 的更新语义未定义——当 check-dose 陆续返回不同结果（如首检 CRITICAL、调整剂量后复检正常）时，旧 CRITICAL 标记是否会因新检查结果而被清除，缺乏契约定义。若不做清除，old stale CRITICAL 可能长期残留，导致正常处方被误阻断。

**所在位置**：
- §3.4 DosageAlertLevel 职责描述："CRITICAL 写入处方草稿上下文，供处方提交时 BLOCK 判定消费"
- §4.2 处方提交端点行为（第 1/2/3 条）：仅检查 AuditRecord.riskLevel
- §3.4 PrescriptionDraftContext 清理时机：仅定义提交成功/取消/TTL，未定义 check-dose 覆盖更新

**改进建议**：
1. 在 §4.2 处方提交端点行为中补充第 4 条：提交前检查 PrescriptionDraftContext 中该 prescriptionId 是否存在 CRITICAL 级别告警，存在时视为 BLOCK 等效状态拒绝提交
2. 在 §4.4 check-dose 流程中明确 PrescriptionDraftContext 的更新语义——每次 check-dose 调用时，根据结果重新计算并覆盖该 prescriptionId 对应的 CRITICAL 标记（即有新的非 CRITICAL 结果时移除原标记）
3. 在 §3.4 DosageThresholdService 或 PrescriptionDraftContext 中补充此重复覆盖更新的行为契约

---

### 2. [严重] POST /api/prescription/submit 端点缺少 Controller 归属

**问题描述**：
§4.2 定义了 `POST /api/prescription/submit` 端点的完整契约（含 forceSubmit、处方版本校验、BLOCK 阻断等），这是一个新增的 REST 端点。但 §2.1 目录结构中只列出了 `PrescriptionAuditController`（audit 子包）和 `PrescriptionAssistController`（assist 子包），submit 端点没有被分配至任何一个 Controller。编码者拿到设计文档后无法确定该端点该放在哪个 Controller 中，将产生实现时的随机决策或后续重构。

**所在位置**：
- §4.2 处方提交端点行为契约
- §2.1 目录结构 `prescription/api/` 下两个 Controller 均未包含 submit 端点

**改进建议**：
1. 在 §2.1 目录结构中新增 `PrescriptionSubmitController` 或明确将该端点归入现有 Controller（推荐归入 PrescriptionAuditController，因为 submit 涉及 forceSubmit 与 AuditRecord 联动），并在 §4.2 元素处标注 `@PrescriptionAuditController`
2. 若确需保留为独立 Controller，在 §2.1 目录和 §1.3 核心抽象一览中补充 PrescriptionSubmitController 条目

---

### 3. [一般] AllergyWarningItem 与 AllergyWarning 命名不一致，且业务层 DTO 字段定义缺失

**问题描述**：
§2.1 目录结构列出 `AllergyWarning.java`（位于 `dto/assist/` 包），但 §3.4 PrescriptionAssistResponse 的文本中引用的是 `AllergyWarningItem`（`allergyWarnings，List\<AllergyWarningItem\>`），与目录文件名明显不一致。此外，无论在 §1.3 核心抽象一览、§3.4 文本还是 §10.4 中，均未出现对业务层 `AllergyWarning` DTO 的字段定义（§10.4 只定义了 ai-api 层的 `AllergyWarningItem`）。编码者无法确定业务层 AllergyWarning 结构体该包含哪些字段。

需求文档 3.4.10 输出契约规定 allergy_warnings 每项包含 `drug_id` / `allergen` / `severity`，设计侧需要确认业务层 DTO 是否与 ai-api 层 DTO 保持字段一致。

**所在位置**：
- §2.1 目录：`dto/assist/DoseWarning.java, AllergyWarning.java`
- §3.4 PrescriptionAssistResponse：`allergyWarnings，List\<AllergyWarningItem\>`
- §10.4：仅定义 ai-api 层 AllergyWarningItem

**改进建议**：
1. 统一命名：将目录文件名与文本引用统一为 `AllergyWarningItem` 或 `AllergyWarning`。推荐统一为业务层 `AllergyWarning`（与 DoseWarning 风格一致），ai-api 层保留 `AllergyWarningItem` 命名
2. 在 §3.4 或 §1.3 中补充业务层 AllergyWarning DTO 的完整字段定义（至少包含 drugId、allergen、severity（AllergyWarningSeverity 枚举））
3. 在 §4.5 或 §7 中补充分层 DTO 字段差异说明（若业务层与 ai-api 层 AllergyWarning 字段完全一致则直接复用类型）

---

### 4. [一般] DoseWarning 业务层 DTO 字段定义缺失

**问题描述**：
§3.4 PrescriptionAssistResponse 包含 `doseWarnings，List\<DoseWarning\>`，§2.1 目录列出 `DoseWarning.java`，但设计文本中没有任何节段定义业务层 DoseWarning 的字段结构。需求文档 3.4.10 规定 dose_warnings 每项包含 `drug_id` / `warning_type`（枚举：`OVER_SINGLE_DOSE` / `OVER_DAILY_DOSE` / `OVER_DURATION`）/ `message` / `severity`，但设计文本未确认业务层 DoseWarning 是否与此对齐。§10.4 只定义了 ai-api 层的 `DoseWarningItem`。

**所在位置**：
- §3.4 PrescriptionAssistResponse 字段描述仅提及"剂量告警列表（doseWarnings，List\<DoseWarning\>）"，无字段定义
- §1.3 核心抽象一览缺失 DoseWarning 条目
- §2.1 目录列出的 DoseWarning.java 无对应设计文本定义

**改进建议**：
1. 在 §3.4 或 §1.3 中补充 DoseWarning DTO 字段定义，至少包含：drugId、warningType（枚举或字符串，对齐需求文档 3.4.10 的 `OVER_SINGLE_DOSE`/`OVER_DAILY_DOSE`/`OVER_DURATION`）、message、severity
2. 若 business 层 DoseWarning 与 ai-api 层 DoseWarningItem 字段映射关系不是一一对应，在 §4.5 AssistConverter 中补充说明
3. DoseWarning 的 severity 字段类型需明确指定（建议为 DosageAlertLevel 或独立枚举），避免与 DosageAlert.alertLevel 混淆

---

### 5. [轻微] MedicalRecord 实体部分字段缺少显式字段名

**问题描述**：
§3.3 MedicalRecord 字段描述中，"记录标识"、"就诊科室"、"医生 ID" 三个字段只有中文含义说明，没有给出 Java 字段名（与其他字段如 "患者 ID（patientId）"、"就诊标识（visitId，必填）" 的格式不一致）。编码者需要猜测 JPA 实体字段名（如 `id` vs `recordId`，`departmentId` vs `departmentName`，`doctorId` vs `physicianId`），可能导致不同模块实现者采用不同命名约定。

**所在位置**：
- §3.3 MedicalRecord 核心字段描述："核心字段包含：记录标识、患者 ID（patientId）、就诊标识（visitId，必填）、就诊科室、病历内容（contentJson）...、医生 ID、创建时间、更新时间"

**改进建议**：
在 §3.3 中为上述字段补齐 Java 字段名，推荐：
- 记录标识 → `recordId` 或 `id`
- 就诊科室 → `departmentId`（与 TriageRecord 风格一致）
- 医生 ID → `doctorId`（与 AuditRecord 风格一致）

---

## 整体质量评价

该设计文档经 9 轮审议迭代后，在需求对齐、异常覆盖和细节完备性上已达到较高成熟度。以上 5 个问题属于本轮审查新发现的残余缺口，集中在（a）提交流程中 CRITICAL→BLOCK 的安全链路断裂,（b）新增 submit 端点的 Controller 归属,（c）AllergyWarning/DoseWarning 两个 DTO 的字段定义和命名一致性问题。前两项为严重级别，建议在下轮迭代中优先修复，后三项可同步修复。
