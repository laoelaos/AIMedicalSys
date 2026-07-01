# 任务指令（v9）

## 动作
RETRY

## 任务描述
修复 T8 v8 验证失败的缺陷：TriageServiceImpl.triage() 中 fallbackHint 不生效

## 选择理由
T8（consultation 模块）的 85 个测试中仅 1 个失败，其他 84 个全部通过。失败原因清晰定位为 aiFailCount 未在非异常 AI 失败路径递增，属于可修复的代码缺陷。修复后 T8 应全部通过。

## 任务上下文
测试 `shouldSetFallbackHintAfterThreeAiFailures` 要求连续 3 次 AI 失败后，返回的响应中 `fallbackHint` 为 `"AI service has been continuously unavailable"`。当前行为：3 次调用后 `fallbackHint` 仍为 `null`。

## 已有代码上下文
- `TriageServiceImpl.java`: 第 105-133 行为 fallback 逻辑块（aiResult == null || !aiResult.isSuccess()），其中第 120 行检查 `session.getAiFailCount() >= MAX_AI_FAIL_COUNT` 决定是否设置 `fallbackHint`
- `AiResult.java`: `failure()` → `isDegraded()=false`，`degraded()` → `isDegraded()=true`
- `handleAiFailure(session)`: 递增 `aiFailCount` 并返回 `AiResult.degraded(...)`，仅在 catch 块中调用
- 修正已在 `TriageServiceImpl.java:110-111` 添加（在 fallback 块入口处，当 aiResult 非 degraded 时递增 `aiFailCount`）

## RETRY 说明
**失败原因摘要**：handleAiFailure() 仅在 AI 未来抛出异常时调用，但测试中 AI 正常返回 `AiResult.failure("AI_ERROR")`（非异常路径），导致 aiFailCount 永远不会递增，fallbackHint 始终条件不满足。

**已实施的修正**：在 `TriageServiceImpl.java` 的 fallback 块入口添加：
```java
if (aiResult == null || !aiResult.isDegraded()) {
    session.setAiFailCount(session.getAiFailCount() + 1);
}
```
该修正确保：非异常 AI 失败（failure/degraded 但未被异常处理过）也会递增 aiFailCount，同时避免已由异常 handler 递增过（isDegraded=true）的重复计数。

**验证方式**：运行 `mvn test -pl modules/consultation -am`，确认 85 个测试全部通过，其中 `shouldSetFallbackHintAfterThreeAiFailures` 的 assertEquals 断言应为 green。
