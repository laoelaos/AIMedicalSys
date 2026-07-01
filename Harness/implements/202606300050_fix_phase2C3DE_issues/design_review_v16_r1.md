# 设计审查报告（v16 r1）

## 审查结果
REJECTED

## 发现

### **[严重]** P16 设计引用不存在的方法 — persistAuditRecord 中无法获取 prescriptionOrderId

**问题**：P16 节（§详细变更设计 → P16 → persistAuditRecord 分组清理）设计的代码如下：
```java
if (response.getPrescriptionOrderId() != null) {
    List<AuditRecord> orderRecords = auditRecordRepository
            .findByPrescriptionOrderIdAndIsLatestTrue(response.getPrescriptionOrderId());
```
但 `response` 是 `AuditResponse` 类型的参数，该类（`AuditResponse.java`）仅包含 `riskLevel`、`alerts`、`interactions`、`suggestions`、`fromFallback` 五个字段，**不存在 `getPrescriptionOrderId()` 方法**。`AuditRequest` 同样不包含此字段。

**为什么是问题**：按此设计编码无法通过编译。`persistAuditRecord` 被 `audit()` 调用，而 `audit()` 流程中尚无 prescriptionOrderId（该值在 `submit()`/`handleStepThree()` forceSubmit 路径中生成），因此即使修正方法签名，也无法在此位置获取 orderId。

**期望修正方向**：
- 方案 A：在 `persistAuditRecord` 签名中增加 `String prescriptionOrderId` 参数（允许 null），由调用方传入；`audit()` 中传入 null（prescriptionOrderId 尚不存在），`handleStepThree()` forceSubmit 路径中传入对应值。
- 方案 B：将按 prescriptionOrderId 清理 isLatest 的逻辑移至 `handleStepThree()` forceSubmit 路径中（在 `save(latestRecord)` 之后执行清理），而非放在 `persistAuditRecord()` 内。
- 方案 C：若坚持放在 `persistAuditRecord`，需明确说明如何获取 prescriptionOrderId（例如从 `request.getPrescriptionId()` 跨表查询已有 AuditRecord 中关联的 orderId），并在设计中补充该查询逻辑。

### **[一般]** P07 AI 路径未按任务要求从 DrugInteractionItem 构建 AuditIssue

**问题**：任务描述（task_v16.md §P07）明确要求 AI 路径"从 PrescriptionCheckResponse 各 alert/interaction 构建"AuditIssue 列表。但设计（§详细变更设计 → P07）仅从 `AlertItem` 构建，并在注释中说明"此处简单起见仅从 AlertItem 构建"，主动跳过了 `DrugInteractionItem` 和 `SuggestionItem` 的构建。

**为什么是问题**：任务 R16 明确将 interaction 列为 AuditIssue 数据来源。`DrugInteractionItem` 包含 `drugPair`、`severity`、`description` 等字段，具有等价于审核问题的语义信息。仅从 AlertItem 构建会导致审核记录中缺少药物相互作用相关的审核问题条目，造成信息不完整。

**期望修正方向**：
- 补充从 `DrugInteractionItem` 到 `AuditIssue` 的映射规则。建议映射：`description → issueDescription`、`drugPair → fieldName`、`severity (String) → severity (AlertSeverity)`、`ruleId` 可使用如 `"DRUG_INTERACTION_" + drugPair` 之类的约定值。
- 或至少在设计中明确说明不处理的理由（如"Phase 2/3 药物相互作用规则未启用，DrugInteractionItem 预期为空列表"），而非简单以"简单起见"跳过。

### **[轻微]** P16 分组清理顺序与任务描述不一致

**问题**：任务要求"在按 prescriptionId 清理 isLatest **之前**，新增按 prescriptionOrderId 分组清理逻辑"（即 prescriptionOrderId 清理在前、prescriptionId 清理在后）。但设计将 prescriptionOrderId 清理放在 prescriptionId 清理**之后**。

**为什么是问题**：虽然 isLatest=false 设置具有幂等性，顺序不影响最终正确性，但与任务明确指定的顺序不符。在后续维护时需要对齐任务约定的实施规范。

**期望修正方向**：将 prescriptionOrderId 清理逻辑移至 prescriptionId 清理之前，与任务要求保持一致；或补充说明为何调整顺序。
