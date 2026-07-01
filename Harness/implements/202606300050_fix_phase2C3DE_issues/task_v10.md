# 任务指令（v10）

## 动作
NEW

## 任务描述
修复 TriageConverterTest 中因 A07 `AiResult.success(null)` requireNonNull 导致的 2 测试 ERROR

**预期变更文件**：
- `modules/consultation/src/test/java/com/aimedical/modules/consultation/TriageConverterTest.java` — 2 处 `AiResult.success(null)` → `AiResult.failure("AI_UNAVAILABLE")`

## 选择理由
R9 验证 FAILED。A07 `AiResult.success()` 增加 `Objects.requireNonNull(data)` 是正确契约变更，但 TriageConverterTest 中 2 个测试仍使用 `AiResult.success(null)` 触发 NPE。生产代码修复正确，仅测试需对齐。

## 任务上下文
### 失败测试 1: `shouldNotWriteBackCorrectedChiefComplaintWhenAiDataIsNull` (L150-155)
```java
void shouldNotWriteBackCorrectedChiefComplaintWhenAiDataIsNull() {
    DialogueSession session = new DialogueSession("session-001");
    com.aimedical.modules.consultation.dto.TriageResponse result = converter.toTriageResponse(AiResult.success(null), null, session);
    assertNull(session.getCorrectedChiefComplaint());
}
```
修改：`AiResult.success(null)` → `AiResult.failure("AI_UNAVAILABLE")`

### 失败测试 2: `shouldReturnEmptyDepartmentsForNullAiData` (L180-184)
```java
void shouldReturnEmptyDepartmentsForNullAiData() {
    AiResult<TriageResponse> aiResult = AiResult.success(null);
    com.aimedical.modules.consultation.dto.TriageResponse result = converter.toTriageResponse(aiResult, null, null);
    assertNull(result.getDepartments());
}
```
修改：`AiResult.success(null)` → `AiResult.failure("AI_UNAVAILABLE")`

## 说明
- `AiResult.success(null)` 不再合法（A07 增加 `Objects.requireNonNull(data)`）
- 语义上，AI 返回 null 等效于 AI 不可用/失败，`AiResult.failure("AI_UNAVAILABLE")` 正确表达该语义
- `toTriageResponse` 能正确处理 failure 结果（返回 null/降级 path）
- 仅测试文件变更，无需修改生产代码

## 修订说明（v10 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| plan.md 轮次编号不一致（重复 R10）需要修订，task_v10.md 任务内容正确无需变更 | plan.md 完成轮次递移修复；task_v10.md 内容保持不变，追加本修订说明 |
