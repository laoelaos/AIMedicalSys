# 测试审查报告（v16 r1）

## 审查结果
REJECTED

## 发现

### **[一般]** `PrescriptionAuditServiceImplTest.java:311` — P07 AI 路径未验证 toAlertSeverity(String) 映射输出

**问题**：测试 `auditShouldWriteAuditIssuesToRecordFromAiAlertsAndInteractions` 使用 `assertTrue(saved.getAuditIssues().contains("..."))` 在原始 JSON 字符串上做子串匹配，验证了告警 code、message、ruleId、description 等字段，但**从未验证 severity 枚举值的映射是否正确**。

- `AlertItem.severity = "WARNING"` → `toAlertSeverity("WARNING")` 应该输出 `AlertSeverity.WARNING`
- `DrugInteractionItem.severity = "CRITICAL"` → `toAlertSeverity("CRITICAL")` 应该输出 `AlertSeverity.CRITICAL`
- 所有 4 条 `assertTrue(contains(...))` 断言均未检查 severity 值

**为什么是问题**：`toAlertSeverity(String)` 重载是 v16 r1 新增的映射方法（对应 DrugInteractionItem/AlertItem 的 String 类型 severity），具有 null/"WARN"/未知值等边缘情况。没有断言验证其输出，意味着若该方法因重构或失误返回了错误的 `AlertSeverity`（例如 `"WARNING"`→`INFO`），测试仍会通过。该缺陷直接导致测试对 auditIssues 的核心字段之一（severity）失效。

**期望的修正方向**：将 `saved.getAuditIssues()` JSON 反序列化为 `List<AuditIssue>`，对每个 `AuditIssue` 断言 `severity` 字段的枚举值正确，包括验证 `AlertItem` 和 `DrugInteractionItem` 各自的 severity 映射。

---

### **[轻微]** `PrescriptionAuditServiceImplTest.java` — 缺少 SuggestionItem 排除的负面测试

**问题**：详细设计明确声明 "SuggestionItem 未纳入 AuditIssue 构建——SuggestionItem 仅有 suggestionCode 和 suggestionText，无 severity 字段"。当前测试在 AI 路径中设置了 `checkResponse.setSuggestions(new ArrayList<>())`，但没有任何测试断言 SuggestionItem **不会**被转换为 AuditIssue。若后续开发者在 AuditIssue 构建循环中误加入了 SuggestionItem，无测试会拦截。

**期望的修正方向**：新增测试在 AI 路径的 `PrescriptionCheckResponse` 中放入非空 `suggestions` 列表，然后断言 saved record 的 auditIssues JSON 中不包含任何 suggestion 相关的内容。

---

### **[轻微]** `PrescriptionAuditServiceImplTest.java` — P07 AI 路径缺少部分边缘用例

**问题**：P07 AI 路径测试覆盖了 `alerts + interactions` 合并写入以及两者均为 null/空的场景，但缺少以下边缘用例：

1. AI 响应 `data == null`（`aiResult.isSuccess() == true` 但 `getData() == null`）
2. AI 响应仅含 interactions（`alerts == null`，`interactions` 非空）
3. AI 响应仅含 alerts（`interactions == null`，`alerts` 非空）

这些场景在实现中有独立的分支（`aiResult.getData().getAlerts() != null` 和 `aiResult.getData().getInteractions() != null` 是两个独立的 if 块），但无测试验证。

**期望的修正方向**：补充上述三个边缘场景的测试方法。

## 修改要求

### 对 `PrescriptionAuditServiceImplTest.java`:

1. **[一般]** `auditShouldWriteAuditIssuesToRecordFromAiAlertsAndInteractions` (line 311)：在 `verify` 捕获 saved record 后，将 `saved.getAuditIssues()` JSON 用 `objectMapper.readValue()` 反序列化为 `List<AuditIssue>`，对每个 AuditIssue 分别断言 `severity` 字段值正确。示例：
   ```java
   List<AuditIssue> issues = objectMapper.readValue(
       saved.getAuditIssues(),
       new com.fasterxml.jackson.core.type.TypeReference<List<AuditIssue>>() {});
   assertEquals(2, issues.size());
   assertEquals(AlertSeverity.WARNING, issues.get(0).getSeverity());
   assertEquals(AlertSeverity.CRITICAL, issues.get(1).getSeverity());
   ```

2. **[轻微]** 新增测试方法验证 SuggestionItem 不生成 AuditIssue，例如在 AI 路径中设置 `checkResponse.setSuggestions(List.of(new SuggestionItem("S1", "suggestion text")))` 并与空 `alerts`/`interactions` 组合，断言 `auditIssues` 为 null。

3. **[轻微]** 新增 3 个 AI 路径边缘用例测试覆盖 `data==null`、仅 interactions、仅 alerts。
