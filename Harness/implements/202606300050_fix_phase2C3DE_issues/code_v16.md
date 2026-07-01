# 实现报告（v16）

## 概述

对 PrescriptionAuditServiceImpl + AuditRecordRepository 完成四项审核记录完善修复（P06/P07/P08/P16），包括：降级路径告警信息构建、auditIssues 写入 record、forceSubmit 回写 prescriptionOrderId、以及按 prescriptionOrderId 的分组清理逻辑。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `.../service/audit/impl/PrescriptionAuditServiceImpl.java` | P06/P07/P08/P16 共 4 处变更 + 2 个 toAlertSeverity 映射方法 |
| 修改 | `.../repository/AuditRecordRepository.java` | P16 新增 `findByPrescriptionOrderIdAndIsLatestTrue` 方法 |
| 修改 | `.../service/audit/impl/PrescriptionAuditServiceImplTest.java` | 新增 7 个测试方法覆盖 P06/P07/P08/P16 |

## 编译验证

BUILD SUCCESS（mvn compile -pl modules/prescription -am）

## 测试验证

Tests run: 37, Failures: 0, Errors: 0, Skipped: 0

新增 7 个测试方法：
- P06: `auditShouldPopulateAlertsFromLocalRuleResultsWhenFallback`, `auditShouldReturnEmptyAlertsWhenAllLocalRulesPass`
- P07: `auditShouldWriteAuditIssuesToRecordWhenFallback`, `auditShouldWriteAuditIssuesToRecordFromAiAlertsAndInteractions`
- P08: `submitShouldSetPrescriptionOrderIdOnRecordAndResponseWhenForceSubmit`
- P16: `submitShouldCleanupIsLatestByOrderIdWhenForceSubmit`

## 设计偏差说明

无偏差。

## 修订说明（v16 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| **[严重]** P16 分组清理在 `save(latestRecord)` 之后执行，JPA flush 导致 `latestRecord` 也被查询并设 `isLatest=false` | 调换顺序：先执行 `findByPrescriptionOrderIdAndIsLatestTrue` 清理旧记录，再执行 `save(latestRecord)`，确保 `latestRecord` 的 `isLatest` 不会被错误清除 |
| **[严重]** 测试 `submitShouldCleanupIsLatestByOrderIdWhenForceSubmit` 未断言 `latestRecord.isLatest()` 仍为 `true` | 新增 `assertTrue(latest.isLatest())` 断言 |
