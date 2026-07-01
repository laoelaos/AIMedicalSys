# 代码审查报告（v4 r1）

## 审查结果
REJECTED

## 发现

- **[一般]** `PrescriptionAssistServiceImpl.java:347,375` — PROCESSING 状态设置后未被持久化，违反设计行为契约。第 347 行 `result.setStatus(AiSuggestionStatus.PROCESSING)` 设置的是局部对象；`suggestionStore.put(taskId, result)` 仅在 try-catch 块后的第 375 行执行，这意味着 PROCESSING 状态在存入 store 之前已被覆盖为 COMPLETED/FAILED/TIMEOUT。调用 `getSuggestion()` 永远无法观察到 PROCESSING 状态，只能看到 PENDING（来自 DedupTaskScheduler 的初始存入）或最终状态。

## 修改要求

### 问题 1：PROCESSING 状态未持久化（PrescriptionAssistServiceImpl.java）

**位置**：`scheduleSuggestionAsync()` 方法内，第 345-377 行。

**问题**：`result.setStatus(AiSuggestionStatus.PROCESSING)`（第 347 行）设置了局部变量，但 `suggestionStore.put(taskId, result)`（第 375 行）位于 try-catch 块之后。因此 PROCESSING 在写入 store 前已被后续的 setStatus 覆盖，process 状态对外不可见。

**期望的修正**：在第 347 行（`result.setStatus(...PROCESSING)`）之后立即插入一次 `suggestionStore.put(taskId, result)`，使 PROCESSING 状态对 `getSuggestion()` 可读；保留第 375 行的 put 以写入最终完成/失败/超时状态。修正后结构应为：

```java
result.setTaskId(taskId);
result.setStatus(AiSuggestionStatus.PROCESSING);
suggestionStore.put(taskId, result);  // 立即持久化 PROCESSING
try {
    // ... AI 调用 ...
} catch (...) {
    // ... 设置最终状态 ...
}
suggestionStore.put(taskId, result);  // 最终持久化
```
