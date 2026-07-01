# 代码审查报告（v16 r1）

## 审查结果
REJECTED

## 发现

- **[严重]** `PrescriptionAuditServiceImpl.java:256-269` — P16 分组清理会在 `save(latestRecord)` 之后执行 `findByPrescriptionOrderIdAndIsLatestTrue(orderId)` 查询。JPA 默认 `FlushModeType.AUTO` 会在查询前 flush，导致刚保存的 `latestRecord`（其 `prescriptionOrderId=orderId`, `isLatest=true`）也被查询返回并被置为 `isLatest=false`，违反设计契约"确保该 orderId 下仅此记录保留 isLatest=true"。

## 修改要求

### [严重] P16 分组清理顺序导致 latestRecord 的 isLatest 被错误清掉

**位置**：`PrescriptionAuditServiceImpl.java:256-269`

**问题**：代码顺序为：`save(latestRecord)` → `findByPrescriptionOrderIdAndIsLatestTrue` → `setLatest(false)` → `saveAll`。由于 flush 发生在查询之前，`latestRecord` 也会被查询返回并被设 `isLatest=false`。

**期望修正方向**：将 `save(latestRecord)` 移到分组清理查询的后面，即先清理旧记录再保存新 record：

```
String orderId = "RX-" + System.currentTimeMillis();
latestRecord.setPrescriptionOrderId(orderId);
latestRecord.setForceSubmitted(true);
latestRecord.setForceSubmitTime(LocalDateTime.now());

// 先清理
List<AuditRecord> orderRecords = auditRecordRepository
        .findByPrescriptionOrderIdAndIsLatestTrue(orderId);
for (AuditRecord orderRecord : orderRecords) {
    orderRecord.setLatest(false);
}
if (!orderRecords.isEmpty()) {
    auditRecordRepository.saveAll(orderRecords);
}

// 后保存
auditRecordRepository.save(latestRecord);
```

**测试补充**：现有测试 `submitShouldCleanupIsLatestByOrderIdWhenForceSubmit` 中 mock 返回的 `oldOrderRecord` 与 `latestRecord` 不同，未覆盖该 bug。修复后应新增断言验证 `latestRecord.isLatest()` 仍为 `true`。
