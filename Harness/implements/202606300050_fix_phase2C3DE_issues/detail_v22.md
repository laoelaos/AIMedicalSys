# 详细设计（v22）

## 概述

在 `PrescriptionAssistServiceImpl.assist()` 同步 AI 调用完成后，新增异步 AI 调用管线：通过 `CompletableFuture.supplyAsync()` 异步调用 `AiService.prescriptionAssist()`，将结果存入 `SuggestionStore`，并实现 `PENDING→COMPLETED/FAILED` 完整状态映射。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `prescription/service/assist/impl/PrescriptionAssistServiceImpl.java` | 修改 | 新增 `scheduleSuggestionAsync()` 私有方法；`assist()` 中在同步 AI 成功后触发异步调度 |
| `prescription/service/assist/DedupTaskScheduler.java` | 不变 | `schedule()` 签名与行为不变 |
| `prescription/dto/assist/AiSuggestionResult.java` | 不变 | 字段已完备 |

## 类型定义

### PrescriptionAssistServiceImpl

**形态**：class（已有，仅修改）
**包路径**：`com.aimedical.modules.prescription.service.assist.impl`
**职责**：在同步 AI 调用成功后，新增异步 AI 调用并维护 AiSuggestionResult 生命周期

#### 新增私有方法

**方法签名**：
```java
private void scheduleSuggestionAsync(String taskId, PrescriptionAssistRequest request)
```

**职责**：使用 `CompletableFuture.supplyAsync()` 异步执行 AI 调用，根据结果创建 `AiSuggestionResult` 并通过 `suggestionStore.put(taskId, result)` 持久化

**行为细节**：
1. 构建 `PrescriptionAssistRequest` 的 AI 请求：`assistConverter.toAiPrescriptionAssistRequest(request)`
2. 调用 `aiService.prescriptionAssist(aiRequest).get(aiTimeout, TimeUnit.SECONDS)`
3. 成功时（`aiResult.isSuccess() && aiResult.getData() != null`）：
   - `status = COMPLETED`
   - `suggestion = objectMapper.writeValueAsString(aiResult.getData())`
4. 异常/超时时（`ExecutionException`、`TimeoutException`、`InterruptedException`、或其他 `Exception`）：
   - `status = FAILED`
   - `failReason = 异常类名 + ": " + 消息前 200 字符`
5. 调用 `suggestionStore.put(taskId, result)` 写入

#### 修改 assist() 方法

在 L108 之后（`if (aiResult.isSuccess())` 块结束处）插入：
```java
String taskId = dedupTaskScheduler.schedule(request.getPrescriptionId());
scheduleSuggestionAsync(taskId, request);
```

即同步 AI 成功后，创建 PENDING 条目获取 taskId，再启动异步 AI 调用。异步调用的结果会通过 taskId 回填到 SuggestionStore。

#### 新增导入

```java
import java.util.concurrent.CompletableFuture;
```

### AiSuggestionResult（不变）

**形态**：class
**包路径**：`com.aimedical.modules.prescription.dto.assist`
**职责**：AI 建议结果的存储模型

**已有字段**：
| 字段 | 类型 | 说明 |
|------|------|------|
| taskId | String | 任务唯一标识 |
| suggestion | String | COMPLETED 时保存 AI 返回序列化 JSON |
| status | AiSuggestionStatus | PENDING / COMPLETED / FAILED |
| createTime | LocalDateTime | 创建时间 |
| failReason | String | FAILED 时保存异常摘要 |
| consumed | boolean | 业务层读取后标记为已消费 |
| partialData | String | 部分数据（预留） |

### AiSuggestionStatus（已有，不变）

```java
public enum AiSuggestionStatus {
    PENDING, COMPLETED, FAILED
}
```

### DedupTaskScheduler（不变）

**形态**：class
**包路径**：`com.aimedical.modules.prescription.service.assist`
**职责**：保持现有 `schedule(String prescriptionId) → String taskId` 签名，仅创建 PENDING 条目，不做 AI 调用

## 错误处理

| 场景 | 处理方式 |
|------|---------|
| 异步 AI 调用中抛出 `ExecutionException` | 捕获，映射为 FAILED + failReason 包含异常类名与消息 |
| 异步 AI 调用中抛出 `TimeoutException` | 同上 |
| 异步 AI 调用中抛出 `InterruptedException` | 恢复中断标记，映射为 FAILED |
| `objectMapper.writeValueAsString()` 异常 | 捕获，映射为 FAILED |
| 其他未预期异常 | 外部 try-catch 兜底捕获 `Exception`，映射为 FAILED |

`scheduleSuggestionAsync()` 内部使用 try-catch 包围全部逻辑，确保任何异常都被捕获并映射到 FAILED 状态，不会使异步任务静默失败。

## 行为契约

### 状态映射管线

```
sync AI 成功
    → dedupTaskScheduler.schedule()  → PENDING 条目写入 SuggestionStore
    → CompletableFuture.supplyAsync() → 异步 AI 调用
        ├─ 成功 → COMPLETED + suggestion(JSON)
        └─ 失败 → FAILED + failReason

getSuggestion(taskId) 被调用时
    └─ status == COMPLETED → consumed = true（已有逻辑，L219-222）
```

### 前置条件
- `assist()` 收到的 `request.getPrescriptionId()` 非 null（已在 L87 兜底赋 UUID）
- `assist()` 的同步 AI 调用已成功完成（`aiResult.isSuccess()` 为 true）

### 后置条件
- 同步返回不受异步调度影响（异步完全独立）
- SuggestionStore 中最终存在 taskId 对应的 AiSuggestionResult，status 为 COMPLETED 或 FAILED

### 线程安全
- `CompletableFuture.supplyAsync()` 使用公共 ForkJoinPool（默认）
- `suggestionStore.put()` 的线程安全性由 SuggestionStore 实现保证

## 依赖关系

### 已有依赖（PrescriptionAssistServiceImpl 已注入，无需新增）

| 依赖 | 字段 | 用途 |
|------|------|------|
| `AiService` | aiService | 调用 `prescriptionAssist()` |
| `AssistConverter` | assistConverter | 转换请求 |
| `DedupTaskScheduler` | dedupTaskScheduler | 创建 PENDING 条目 |
| `SuggestionStore` | suggestionStore | 存储/更新 AiSuggestionResult |
| `ObjectMapper` | objectMapper | 序列化 AI 返回数据 |
| `aiTimeout` | aiTimeout | 异步 AI 调用超时 |

### 暴露给后续任务的公开接口
- `PrescriptionAssistServiceImpl.getSuggestion(String taskId)` → 读取 AiSuggestionResult（已有）
- `DedupTaskScheduler.schedule(String prescriptionId)` → 返回 taskId（已有）
