# 实现报告（v22）

## 概述
在 `PrescriptionAssistServiceImpl.assist()` 同步 AI 调用成功后，新增异步 AI 调用管线，包含 `scheduleSuggestionAsync()` 私有方法及 `assist()` 内触发逻辑。

## 文件变更清单
| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `prescription/service/assist/impl/PrescriptionAssistServiceImpl.java` | 新增 `scheduleSuggestionAsync()` 方法；`assist()` 中同步 AI 成功后触发异步调度 |

## 变更详情

### 1. 新增导入
- `import java.util.concurrent.CompletableFuture;`

### 2. 修改 assist() 方法
在 `if (aiResult.isSuccess())` 块内末尾插入：
```java
String taskId = dedupTaskScheduler.schedule(request.getPrescriptionId());
scheduleSuggestionAsync(taskId, request);
```

### 3. 新增 scheduleSuggestionAsync 方法
**签名**：`private void scheduleSuggestionAsync(String taskId, PrescriptionAssistRequest request)`

**行为**：
- 使用 `CompletableFuture.supplyAsync()` 异步执行
- 构建 AI 请求 → 调用 `aiService.prescriptionAssist(aiRequest).get(aiTimeout, TimeUnit.SECONDS)`
- 成功时：`status = COMPLETED`, `suggestion = objectMapper.writeValueAsString(aiResult.getData())`
- 失败/异常时：`status = FAILED`, `failReason = 异常类名 + ": " + 消息前 200 字符`
- 最终 `suggestionStore.put(taskId, result)` 持久化

**异常处理**：
| 异常 | 处理 |
|------|------|
| ExecutionException / TimeoutException | FAILED + 类名:消息 |
| InterruptedException | 恢复中断标记 + FAILED |
| 其他 Exception | FAILED + 类名:消息 |

## 编译验证
`mvn compile -pl modules/prescription -am` 编译通过，无错误。

## 设计偏差说明
无偏差。
