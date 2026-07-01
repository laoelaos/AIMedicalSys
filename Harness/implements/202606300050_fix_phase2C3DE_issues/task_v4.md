# 任务指令（v4）

## 动作
RETRY

## 任务描述
修复 `TriageServiceImpl.saveTriageRecord` 中 `degraded` 标记赋值和科室路由判断逻辑：将 2 处 `aiResult.isDegraded()` 替换为 `response.isDegraded()`，使记录持久化使用业务层最终降级决策（`response` 对象持有，而非 `aiResult` 原始状态）。

**涉及的变更文件**：
- `consultation/service/impl/TriageServiceImpl.java`

## 选择理由
R3 实现 669 测试通过，1 失败。失败测试 `shouldSaveRuleMatchedDepartmentsWhenDegraded` 验证降级路径中记录应标记 `degraded=true` 且部门存入 `ruleMatchedDepartments`。当前代码使用 `aiResult.isDegraded()` 判断，但 `AiResult.failure("AI_ERROR")` 的 `isDegraded()` 返回 `false`（仅 `AiResult.degraded()` 返回 `true`），导致路由错误。

## 任务上下文
- **失败测试**：`TriageServiceImplTest.shouldSaveRuleMatchedDepartmentsWhenDegraded` 行 414
- **失败断言**：`assertTrue(recordRepository.record.getDegraded())` — expected true, was false
- **根本原因**：`AiResult.failure()` 构造 `new AiResult<>(false, null, errorCode, false, null)` — `degraded=false`。业务降级路径已通过 `response.setDegraded(true)` 正确标记，但 `saveTriageRecord` 未使用此值。

## 已有代码上下文

### TriageServiceImpl.saveTriageRecord 当前代码（需修复）
```java
// 第 230 行：degraded 标记
if (aiResult != null && aiResult.isDegraded()) {
    record.setDegraded(true);
} else {
    record.setDegraded(false);
}

// 第 237 行：科室路由
if (finalDepartmentsJson != null) {
    if (aiResult != null && aiResult.isDegraded()) {
        record.setRuleMatchedDepartments(finalDepartmentsJson);
    } else {
        record.setAiRecommendedDepartments(finalDepartmentsJson);
    }
}
```

### AiResult 工厂方法（ai-api）
```java
public static <T> AiResult<T> failure(String errorCode) {
    return new AiResult<>(false, null, errorCode, false, null);  // degraded=false
}
public static <T> AiResult<T> degraded(String fallbackReason) {
    return new AiResult<>(false, null, null, true, fallbackReason);  // degraded=true
}
```

### response.isDegraded()
`TriageResponse` 字段 `degraded`，在降级路径中设置为 `true`（行 141），成功路径由 `TriageConverter.toTriageResponse` 基于 `aiResult` 设置。

## RETRY 说明
**失败原因**：R3 实现 `saveTriageRecord` 中 `aiResult.isDegraded()` 无法正确反映降级状态。当 AI 返回 `AiResult.failure()`（非 `degraded()`）时，`isDegraded()` 为 `false`，但业务逻辑已确定走降级路径。

**修正方向**：
1. 将 `saveTriageRecord` 方法中 2 处 `aiResult != null && aiResult.isDegraded()` 替换为 `response.isDegraded()`
2. `response` 参数由调用方传入，降级路径已通过 `fallbackResponse.setDegraded(true)` 保证正确性
3. 无需修改测试代码 — 测试预期（`getDegraded()=true`, `getRuleMatchedDepartments()!=null`, `getAiRecommendedDepartments()=null`）正确，与修复后的实现匹配
