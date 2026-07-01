# 测试审查报告（v11 r2）

## 审查结果
REJECTED

## 发现
- **[一般]** `PrescriptionAuditServiceImplTest.java` — forceSubmit=true + 处方已变更（五字段比对失败）代码路径无测试覆盖

## 修改要求（仅 REJECTED 时）

### 1. PrescriptionAuditServiceImplTest.java: 缺少 forceSubmit=true + 处方变更测试

**位置**：在 `submitShouldRejectForceSubmitWhenAuditRecordIdMismatch` 附近新增独立测试方法

**问题**：`PrescriptionAuditServiceImpl.java:224-229` 实现了 forceSubmit=true 路径的五字段比对失败分支（返回 `RX_AUDIT_PRESCRIPTION_MODIFIED`），该分支当前测试集中没有任何测试覆盖。已完成覆盖：
- `submitShouldForceSubmitWhenWarnAndValidForceRequest` — forceSubmit=true + 比对一致（成功路径）
- `submitShouldRejectForceSubmitWhenAuditRecordIdMismatch` — forceSubmit=true + auditRecordId 不匹配（返回 RX_AUDIT_FORCE_SUBMIT_INVALID）

但 forceSubmit=true + auditRecordId 匹配 + 处方已变更 → `RX_AUDIT_PRESCRIPTION_MODIFIED` 路径未被任何测试验证。

**期望**：新增测试方法（如 `submitShouldRejectForceSubmitWhenPrescriptionModified`），设置 `forceSubmit=true`、`auditRecordId` 与最新记录匹配、`prescriptionItems` 与 `originalPrescription` JSON 不一致，断言 `submitted=false` 且 `errorCode=RX_AUDIT_PRESCRIPTION_MODIFIED`。
