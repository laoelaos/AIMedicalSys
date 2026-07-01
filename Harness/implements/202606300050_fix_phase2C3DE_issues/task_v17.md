# 任务指令（v17）

## 动作
RETRY

## 任务描述
修复 R16 中 `PrescriptionAuditServiceImplTest.auditShouldHandleAiDataNull` 测试的 NPE 问题。

### 修复项
- **文件**：`modules/prescription/.../service/audit/impl/PrescriptionAuditServiceImplTest.java:455`
- **变更**：`AiResult.success(null)` → `AiResult.failure("AI_UNAVAILABLE")`
- **理由**：R9 A07 变更后 `AiResult.success()` 使用 `Objects.requireNonNull(data)` 拒绝 null 参数。该测试意图验证 AI 返回 null 数据时的优雅降级处理，应使用 `AiResult.failure("AI_UNAVAILABLE")` 语义表达 AI 不可用，与 R10 修复一致。

## 选择理由
R16 生产代码正确（P06/P07/P08/P16 实现已验证通过 prescription 模块全部 175 个测试），仅此 1 个测试因 `AiResult.success(null)` 与 A07 契约不兼容而 NPE 失败。此修复为纯测试适配，不涉及生产代码变更。

## 任务上下文
### 失败测试代码（L453-473）
```java
@Test
void auditShouldHandleAiDataNull() throws Exception {
    AiResult<PrescriptionCheckResponse> aiResult = AiResult.success(null);   // ← NPE: requireNonNull
    // ...
    service.audit(auditRequest);
    ArgumentCaptor<AuditRecord> captor = ArgumentCaptor.forClass(AuditRecord.class);
    verify(auditRecordRepository).save(captor.capture());
    assertNull(captor.getValue().getAuditIssues());
}
```

### 修复后代码
```java
    AiResult<PrescriptionCheckResponse> aiResult = AiResult.failure("AI_UNAVAILABLE");
```

## 已有代码上下文
- `AiResult.success(T data)` 在 R9 (A07) 新增了 `Objects.requireNonNull(data)`，拒绝 null 参数
- `AiResult.failure(String errorCode)` 返回 `success=false` 的 AiResult，不含 data
- R10 已对 TriageConverterTest 中相同的 `AiResult.success(null)` 模式做了同样修复

## RETRY 说明
- **失败原因**：R16 实现后，`PrescriptionAuditServiceImplTest.auditShouldHandleAiDataNull`（L455）调用 `AiResult.success(null)`，R9 A07 变更后该方法通过 `Objects.requireNonNull(data)` 拒绝 null，导致 NPE
- **修正方向**：`AiResult.success(null)` → `AiResult.failure("AI_UNAVAILABLE")`（单行变更，1 个文件）

## 涉及文件
| 文件 | 操作 | 说明 |
|------|------|------|
| `modules/prescription/.../service/audit/impl/PrescriptionAuditServiceImplTest.java` | 修改 | L455: AiResult.success(null) → AiResult.failure("AI_UNAVAILABLE") |
