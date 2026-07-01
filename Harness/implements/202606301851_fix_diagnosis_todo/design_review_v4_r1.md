# 设计审查报告（v4 r1）

## 审查结果
APPROVED

## 发现

### **[轻微]** P14: `clearCriticalAlerts` 在 AI 失败时清除旧 CRITICAL 告警的语义正确性需谨慎

设计的 `clearCriticalAlerts` 方法在 5 个失败/降级路径中统一调用 `prescriptionDraftContext.updateCriticalAlerts(prescriptionId, Collections.emptyList())` 清除旧 CRITICAL 告警。OOD 语义分析表明 CRITICAL 告警来源是 DosageThresholdService 剂量阈值校验结果，AI 失败时不应保留已过时的旧告警（处方草案可能已变更，旧告警不再适用），也不应写入伪 CRITICAL 告警。设计的处理方式（清除而非写入伪告警）与 OOD §4.4 "无 CRITICAL 时清除对应条目"语义一致，逻辑自洽。标记为轻微仅为提醒：如果业务上要求"AI 不可用时保留上次有效 CRITICAL 告警作为安全保守策略"，则清除行为可能与该策略冲突。但设计文档已明确论证此场景下清除的合理性，且任务上下文中 OOD 语义分析也支持此决策，因此不构成缺陷。

### **[轻微]** DraftContextCleanupTask 迁移后 `recordWrite()` 无生产调用方

当前代码中 `DraftContextCleanupTask.recordWrite(key, timestamp)` 仅在测试代码中被调用，`PrescriptionDraftContext.updateCriticalAlerts()` 未调用 `recordWrite()` 记录写入时间戳。这意味着 `cleanupExpiredDrafts()` 在生产环境中因 `writeTimestamps` 始终为空而实际不清理任何条目。设计将此类原样迁移（仅改包路径），保留了这一现状。虽然迁移本身不应引入行为变更，但此问题属于 TODO 4.2 中"清理的是 DraftContextStore 通用键空间"的延伸——`DraftContextStore` 中的条目实际上永不过期清理。这不影响本次迁移的正确性（迁移不应同时修复此问题），但值得在后续任务中关注。

### **[轻微]** PrescriptionAuditServiceImplTest 中 DrugFacade 相关测试用例名称需确认

设计提到移除 `auditShouldCallDrugFacadeForEachItem`、`auditShouldNotBlockWhenDrugFacadeThrows`、`auditShouldLogWarnWhenDrugFacadeFails`、`auditShouldSkipItemsWithNullDrugId` 共 4 个测试用例。与实际代码中的测试名称一致（line 569/592/611/639）。此外，`PrescriptionAuditServiceImplTest` 中还有 2 处构造器调用（line 77、404、464）需移除 `drugFacade` 和 `2L` 参数，设计已覆盖。`PrescriptionAssistServiceImplTest` 中的构造器调用点（line 59、110、851）同样已覆盖。无遗漏。

### **[轻微]** 移除 DrugFacade 后 `drugFacadeTimeout` 配置项残留

设计在"错误处理"节已提到 `prescription.drug-facade.timeout` 配置项可在 application.yml 中保留（Spring 不报错），建议后续清理。这是合理的渐进式处理策略，不影响功能正确性。
