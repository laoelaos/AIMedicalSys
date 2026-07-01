# 详细设计（v9）

## 概述

本设计仅针对 v8 验证失败的 1 个缺陷进行修复：`TriageServiceImpl.triage()` 中连续 3 次 AI 失败后 `fallbackHint` 不生效。v8 其余设计全部有效，本设计仅描述增量修改。

## 问题根因

`handleAiFailure()` 仅在 AI 异步调用抛出异常时（`InterruptedException`/`ExecutionException`，见 catch 块第 90-92 行）被调用以递增 `aiFailCount`。但测试场景中 AI 正常返回 `AiResult.failure("AI_ERROR")`（`isSuccess()=false`, `isDegraded()=false`），未进入 catch 块，导致 `aiFailCount` 始终为 0，第 123 行的 `session.getAiFailCount() >= MAX_AI_FAIL_COUNT` 条件永远不满足。

## 修正方案

在 fallback 块入口处（`if (aiResult == null || !aiResult.isSuccess())` 内），对非 degraded 的 AI 失败结果显式递增 `aiFailCount`。

### 涉及文件

| 文件路径 | 操作 | 修改位置 |
|---------|------|---------|
| `consultation/src/main/java/com/aimedical/modules/consultation/service/impl/TriageServiceImpl.java` | 修改 | fallback 块入口（第 110 行开始） |

### 代码逻辑变更

**原逻辑（v8 缺陷状态）**：
```
fallback 块入口 → 直接执行规则引擎/兜底降级 → 检查 aiFailCount（始终为 0）→ fallbackHint 不设置
```

**新逻辑（v9 已修正，对应当前源码第 110-136 行）**：
```
fallback 块入口 → 若 aiResult 非 degraded（即未被异常 handler 处理过）则递增 aiFailCount
                → 执行规则引擎/兜底降级
                → 检查递增后的 aiFailCount >= 3 则设置 fallbackHint
```

### 行为契约变更（delta）

1. **`triage()` AI 失败路径 — aiFailCount 递增规则**：
   - **异常路径**（`InterruptedException`/`ExecutionException`）：由 `handleAiFailure()` 递增 `aiFailCount` 并返回 `AiResult.degraded(...)`（`isDegraded()=true`），fallback 块中不再重复递增（`!isDegraded()` 条件为 false）
   - **非异常失败路径**（`AiResult.failure(...)` 返回，`isSuccess()=false`, `isDegraded()=false`）：fallback 块入口递增 `aiFailCount`
   - 此双路径设计确保两种 AI 失败场景均计入连续失败计数，且无重复计数

2. **`triage()` fallbackHint 设置**：在 fallback 块末尾（完成 `aiFailCount` 递增后），检查 `session.getAiFailCount() >= MAX_AI_FAIL_COUNT(3)`，满足条件时设置 `fallbackHint = "AI service has been continuously unavailable"`

### 验证方式

运行 `mvn test -pl modules/consultation -am`，确认 85 个测试全部通过，其中 `TriageServiceImplTest.shouldSetFallbackHintAfterThreeAiFailures` 断言 `result.getFallbackHint()` 等于 `"AI service has been continuously unavailable"`。

## 修订说明（v9 r1）

| 审查意见 | 修改措施 |
|---------|---------|
| [v8 verify] `shouldSetFallbackHintAfterThreeAiFailures` 失败：`fallbackHint` 为 null | 在 fallback 块入口添加非 degraded 路径的 `aiFailCount` 递增（第 111-112 行），确保 `AiResult.failure()` 非异常失败语义下连续计数正确 |
