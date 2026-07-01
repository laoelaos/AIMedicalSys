# 任务指令（v22 r2）

## 动作
NEW

## 任务描述
**异步AI调度+PENDING→COMPLETED/FAILED状态映射（P01+A03）**

实现以下变更：

### P01: PrescriptionAssistServiceImpl 异步 AI 调用
1. PrescriptionAssistServiceImpl.assist() 在同步 AI 调用完成后（约 L106），通过 `CompletableFuture.supplyAsync()` 异步调用 `AiService.prescriptionAssist()`，将结果存入 SuggestionStore
   - 异步调用使用 `CompletableFuture.supplyAsync()`（Spring 托管的 `TaskExecutor` 或 `ForkJoinPool.commonPool()`）
   - 异步调用完成后根据结果更新 AiSuggestionResult 状态
     - 成功（aiResult.isSuccess() && aiResult.getData() != null）→ COMPLETED，回填返回数据（suggestion 字段序列化为 JSON）
     - 超时/异常 → FAILED，记录 failReason（异常类型+消息截断）
2. 异步前先调用 `dedupTaskScheduler.schedule(request.getPrescriptionId())` 创建 PENDING 条目并获取 taskId，异步回调中凭 taskId 通过 `suggestionStore.put(taskId, updatedResult)` 更新状态
3. DedupTaskScheduler.schedule() 不用变更（保持 `String schedule(String prescriptionId)` 签名，不做 AI 调用）

### A03: AiSuggestionResult 5 状态映射
1. 实现完整状态映射管线：
   - `PENDING → COMPLETED`：异步调用成功
   - `PENDING → FAILED`：超时或异常
   - `COMPLETED → consumed=true`：业务层（PrescriptionAssistServiceImpl.getSuggestion）读取结果后标记，供 TTL 清理任务判别
   - consumed 标记已有字段，无需新增
2. AiSuggestionResult 的 suggestion 字段在 COMPLETED 时写入 AI 返回序列化 JSON
3. failReason 字段在 FAILED 时写入异常摘要（异常类名+前 200 字符消息）

### 涉及文件
- `PrescriptionAssistServiceImpl.java`（prescription/service/assist/impl）— assist() 中异步 AI 调用+状态更新，新增私有方法 scheduleSuggestionAsync()
- `DedupTaskScheduler.java` — 不变（仅被 schedule() 调用方改为 assist()，也可保持被 checkDose() 调用）
- `AiSuggestionResult.java` — 无需变更（字段已完备）

## 选择理由
P01(P0) 异步 AI 调度完整管线缺失。当前 DedupTaskScheduler.schedule() 仅创建 PENDING 条目后立即返回，无实际 AI 调用。A03(P0) 5 状态映射是实现报告明确要求"此修复必须与 A03 同步实施"的耦合需求。

R21 已 PASSED（FallbackAiServiceTest 断言修复），R22 为计划中下一个 P0 轮次。

## 任务上下文
- 依赖 R19 A02（AI 超时配置 `ai.timeout.prescription-assist`）— 已完成
- 依赖 AiService.prescriptionAssist() 接口 — 已有 `CompletableFuture<AiResult<PrescriptionAssistResponse>> prescriptionAssist(PrescriptionAssistRequest request);`
- SuggestionStore 接口已有 compute/get/put 方法
- `PrescriptionAssistServiceImpl` 已注入 AiService（L48）、AssistConverter（L49）、SuggestionStore（L54）、ObjectMapper（L55）、aiTimeout（L58）— 无需新增依赖

## 已有代码上下文
### PrescriptionAssistServiceImpl.assist()（L85-177）
```java
public PrescriptionAssistResponse assist(PrescriptionAssistRequest request) {
    // ... 准备 aiRequest ...
    // 同步 AI 调用（L96）
    aiResult = aiService.prescriptionAssist(aiRequest).get(aiTimeout, TimeUnit.SECONDS);
    // ... 处理结果 ...
    return response;
}
```
assist() 已有完整 PrescriptionAssistRequest，所有所需依赖均已注入。

### DedupTaskScheduler.schedule()（45 行）
```java
public String schedule(String prescriptionId) {
    // 创建 PENDING 条目，返回 taskId
    // 不做 AI 调用（当前行为不变）
}
```

### AiSuggestionResult.java
已有字段：taskId, suggestion, status, createTime, failReason, consumed, partialData。无需新增。

### AiService.prescriptionAssist()
返回 `CompletableFuture<AiResult<PrescriptionAssistResponse>>`，已有超时配置 `ai.timeout.prescription-assist`。

### SessionStore (SuggestionStore 父接口)
```java
public interface SessionStore<K, V> {
    V get(K key); void put(K key, V value);
    V remove(K key); boolean containsKey(K key); Set<K> keySet();
}
```

## 实施要点
1. `assist()` 中同步 AI 调用完成后（约 L106 `if (aiResult.isSuccess())` 之后），调用 `dedupTaskScheduler.schedule(request.getPrescriptionId())` 获取 taskId
2. 用 `CompletableFuture.supplyAsync()` 封装异步 AI 调用，lambda 内部：
   - 构建 `PrescriptionAssistRequest` 的 AI 请求（复用已有 `assistConverter.toAiPrescriptionAssistRequest(request)`）
   - `aiService.prescriptionAssist(aiRequest).get(aiTimeout, TimeUnit.SECONDS)`
   - 成功 → `suggestionStore.put(taskId, updatedResult)`，COMPLETED + suggestion 序列化 JSON
   - 失败/超时 → FAILED + failReason
3. 异步任务内部用独立 try-catch 包围全部逻辑，确保任何异常映射到 FAILED 状态
4. COMPLETED 时 suggestion 字段写入 `objectMapper.writeValueAsString(aiResult.getData())`
5. FAILED 时 failReason 写入简短摘要，如 `"TimeoutException: AI did not respond within 8s"`
6. `checkDose()` 对 `dedupTaskScheduler.schedule()` 的调用保持不变（L201），仅创建 PENDING 条目提供 taskId，不做 AI 调用

## 修订说明（v22 r2）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] DedupTaskScheduler.schedule() 无法获取 PrescriptionAssistRequest 数据，无法实现异步 AI 调用 | 异步 AI 调用从 DedupTaskScheduler.schedule() 移至 PrescriptionAssistServiceImpl.assist()（选项 b）。assist() 已有完整 PrescriptionAssistRequest 和所有必需依赖（AiService、AssistConverter、ObjectMapper、SuggestionStore）。schedule() 保持原签名，仅创建 PENDING 条目+返回 taskId |
| [严重] DedupTaskSchedulerTest 因构造函数变更将编译失败 | 因采用选项(b)，DedupTaskScheduler 构造器不变，DedupTaskSchedulerTest 无需修改 |
| [一般] ObjectMapper 依赖未明确 | PrescriptionAssistServiceImpl 已注入 ObjectMapper（L55），无需额外处理 |
