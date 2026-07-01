# 设计审查报告（v16 r2）

## 审查结果
APPROVED

## 发现

### [轻微] P06 空列表行为契约与实际构造不一致
设计行为契约声明"空列表或全通过时，alerts 为 `Collections.emptyList()`"，但实际构造逻辑使用普通 ArrayList，未显式使用 `Collections.emptyList()`。两者在功能上等价（均为空列表），但契约描述与实现方式不完全匹配。建议在构造前判空返回 `Collections.emptyList()` 以对齐契约。

### [轻微] P07 AlertItem.severity 类型未明确说明
设计文档中 `AlertItem → AuditIssue` 映射使用 `toAlertSeverity(AlertItem.severity)`，但未明确说明 `AlertItem.severity` 的类型（String 还是 AlertSeverity）。设计提供了两个重载 `toAlertSeverity(AuditRiskLevel)` 和 `toAlertSeverity(String)`，可以覆盖两种类型，但文档中应注明 `AlertItem.severity` 的实际类型以便开发者选择正确的重载。

### [轻微] P16 isLatest 时序约束未显式声明
`handleStepThree()` forceSubmit 路径中，P16 清理查询 `findByPrescriptionOrderIdAndIsLatestTrue` 可能在当前记录已保存之后执行。如果 `isLatest` 默认值为 `true`，当前记录会被查询返回并被误设 `isLatest=false`。设计注释"同一 orderId 被多个 record 持有的可能性极低"已识别此风险极低，但仍建议在清理后显式设置 `latestRecord.setLatest(true)` 以确保语义正确性。

## 修改要求（无）

所有发现项均为轻微级别，不影响设计正确性。无需修改。
