# 测试审查报告（v18 r1）

## 审查结果
REJECTED

## 发现

- **[严重]** `PrescriptionAuditServiceImplTest.java:724-744` — `submitShouldRequireForceSubmitWhenWarnAndPrescriptionUnchanged` 测试将在运行时失败。测试创建 `AuditRecord latest` 对象时（L724-729）未调用 `latest.setId(...)`，但新增的断言 L743 要求 `result.getWarnResult().getAuditRecordId()` 非空。实现代码 `buildWarnResultFromRecord`（impl L542）直接取 `record.getId()`，因 ID 为 null 导致断言失败。同一文件中其他类似测试（L752, L779, L830 等）均正确调用了 `latest.setId(100L)`。

- **[一般]** 无

- **[轻微]** 无

## 修改要求（仅 REJECTED 时）

### 严重问题

**文件**：`PrescriptionAuditServiceImplTest.java` L724-729
**问题**：`AuditRecord latest` 未设 `id`，导致 L743 `assertNotNull(result.getWarnResult().getAuditRecordId())` 运行时失败。
**原因**：`buildWarnResultFromRecord`（impl L542）调用 `record.getId()` 返回 null，`WarnResult.auditRecordId` 为 null，断言不通过。该测试属于 WARN 路径的 `handleStepThree` 流程（L232-236），已被设计契约覆盖。
**期望修正**：在 L724-729 的 setup 中增加 `latest.setId(100L)`（或任意非 null 值），使断言通过。
