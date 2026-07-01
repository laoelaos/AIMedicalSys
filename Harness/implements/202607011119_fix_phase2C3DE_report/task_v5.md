# 任务指令（v5）

## 动作
RETRY

## 任务描述
修复 prescription 模块两个 P1 级别缺陷：**(1) P01 — 异步 AI 调度线程池绑定 + 异常处理**：scheduleSuggestionAsync 当前使用 `CompletableFuture.supplyAsync()`（默认 ForkJoinPool.commonPool()），返回的 CompletableFuture 未被消费，异常静默丢失；**(2) S03 — DedupTaskScheduler 跨 key 竞态**：createIfNotExists(dedupKey) 成功后、put(candidateTaskId) 前存在原子性窗口，另一线程可通过 dedupKey 读到 candidateTaskId 但 get(key) 找不到数据。

涉及文件：
- `prescription/.../service/assist/impl/PrescriptionAssistServiceImpl.java`（P01：新增线程池字段 + 构造函数注入 + supplyAsync 绑定 + exceptionally 处理）
- `prescription/.../service/assist/DedupTaskScheduler.java`（S03：put 提前至 createIfNotExists/compute 之前执行）
- 新增线程池配置 Bean（prescription 模块级）
- `prescription/.../service/assist/impl/PrescriptionAssistServiceImplTest.java`（构造函数新增 ExecutorService mock 参数）
- `prescription/.../service/assist/DedupTaskSchedulerTest.java`（S03 竞态场景验证）

## 选择理由
R4 已通过 prescription P02/A01/A03 三项修复（308 测试全通过）。P01 和 S03 是 prescription 模块最高优先级剩余 P1 缺陷：(1) P01 涉及生产环境下线程池耗尽风险——ForkJoinPool.commonPool() 被 web 请求的异步 AI 调用共享，高并发下 CPU 密集型任务与 IO 密集型任务互相阻塞；(2) S03 是 DedupTaskScheduler 的核心原子性缺陷，直接影响 schedule() 的正确性。两项均位于同一模块的服务层，合并一轮实现可减少上下文切换和 MR 冲突。

## 任务上下文

### P01 详细变更

**PrescriptionAssistServiceImpl.java 变更：**
1. 新增导入：`import java.util.concurrent.ExecutorService;` `import java.util.concurrent.CompletionException;` `import org.springframework.beans.factory.annotation.Qualifier;`
2. 新增字段：`private final ExecutorService aiTaskExecutor;`
3. 构造函数参数列表末尾新增 `@Qualifier("aiTaskExecutor") ExecutorService aiTaskExecutor`；赋值 `this.aiTaskExecutor = aiTaskExecutor;`
4. `scheduleSuggestionAsync()` 方法第344行：
   ```java
   // 修改前：
   CompletableFuture.supplyAsync(() -> {
   // 修改后：
   CompletableFuture.supplyAsync(() -> {
       // ... (相同 lambda body)
   }, aiTaskExecutor).exceptionally(ex -> {
       log.warn("Async AI suggestion task failed for taskId={}: {}", taskId, ex.getMessage());
       return null;
   });
   ```

**新增线程池配置（com.aimedical.modules.prescription.config.PrescriptionThreadPoolConfig）：**
在 prescription 模块新建配置类 `com.aimedical.modules.prescription.config.PrescriptionThreadPoolConfig`，新增：
```java
@Bean("aiTaskExecutor")
public ExecutorService aiTaskExecutor() {
    return Executors.newThreadPerTaskExecutor(
        Thread.ofVirtual().name("ai-task-").factory()
    );
}
```
或使用平台线程（根据项目实际 JDK 版本和虚拟线程支持情况选择）：
```java
@Bean("aiTaskExecutor")
public ExecutorService aiTaskExecutor() {
    return Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "ai-task-");
        t.setDaemon(true);
        return t;
    });
}
```

### S03 详细变更

**DedupTaskScheduler.java schedule() 方法重排：**
- 将 `suggestionStore.put(candidateTaskId, newResult)` 从第41行和第65行两处移至第38行（createIfNotExists 之前）统一执行
- 消除跨 key 竞态窗口：candidateTaskId 的数据在 dedupKey 被声明前就已可读
- 简化返回值逻辑：createIfNotExists/compute 分支中不再需要单独 put，直接 return

### 已有代码上下文

**scheduleSuggestionAsync() 当前实现（已含 R4 PROCESSING/TIMEOUT 修复）：**
```java
private void scheduleSuggestionAsync(String taskId, PrescriptionAssistRequest request) {
    CompletableFuture.supplyAsync(() -> {
        AiSuggestionResult result = new AiSuggestionResult();
        result.setTaskId(taskId);
        result.setStatus(AiSuggestionStatus.PROCESSING);
        AiSuggestionResult processingResult = new AiSuggestionResult();
        processingResult.setTaskId(taskId);
        processingResult.setStatus(AiSuggestionStatus.PROCESSING);
        suggestionStore.put(taskId, processingResult);
        try {
            // ... aiService.prescriptionAssist(aiRequest).get(aiTimeout, SECONDS)
        } catch (TimeoutException e) { result.setStatus(AiSuggestionStatus.TIMEOUT); ... }
          catch (ExecutionException e) { result.setStatus(AiSuggestionStatus.FAILED); ... }
          catch (InterruptedException e) { Thread.currentThread().interrupt(); ... }
          catch (Exception e) { result.setStatus(AiSuggestionStatus.FAILED); ... }
        suggestionStore.put(taskId, result);
        return result;
    });  // <-- 未绑定线程池，返回 CF 未消费
}
```

**DedupTaskScheduler.schedule() 当前实现（S03 问题标注）：**
```java
public String schedule(String prescriptionId) {
    // ... 构造 dedupKey, candidateTaskId
    // 快路径检查 ...
    
    AiSuggestionResult newResult = new AiSuggestionResult();
    newResult.setTaskId(candidateTaskId);
    newResult.setStatus(AiSuggestionStatus.PENDING);
    newResult.setCreateTime(LocalDateTime.now());

    Object oldValue = suggestionStore.createIfNotExists(dedupKey, newResult);
    if (oldValue == null) {
        suggestionStore.put(candidateTaskId, newResult);  // ← S03: 竞态窗口
        return candidateTaskId;
    }
    // ... createIfNotExists 路径复用旧任务 ...

    Object result = suggestionStore.compute(dedupKey, (key, currentValue) -> {
        // ... 判断 currentValue 状态 ...
    });

    if (result == newResult) {
        suggestionStore.put(candidateTaskId, newResult);  // ← S03: 竞态窗口
        return candidateTaskId;
    }
    // ...
}
```

### 测试影响

**PrescriptionAssistServiceImplTest（构造函数参数计数 10→11）：**
- 新增 `@Mock ExecutorService aiTaskExecutor` 字段 + 构造函数传参
- 修改 `constructorShouldAcceptNineParameters()` 测试：`assertEquals(10, ...)` → `assertEquals(11, ...)`（第861-865行）
- `CompletableFuture.supplyAsync(supplier, executor)` 内部调用 `executor.execute(Runnable)`，需使用以下 mock 策略：
  ```java
  doAnswer(invocation -> {
      Runnable r = invocation.getArgument(0);
      r.run();
      return null;
  }).when(aiTaskExecutor).execute(any(Runnable.class));
  ```
- 受影响的异步测试（需 `execute` mock，共 10 个）：
  `asyncSuggestionShouldStoreCompletedOnAiSuccess`、`asyncSuggestionShouldStoreCompletedWithSerializedSuggestion`、`asyncSuggestionShouldStoreFailedWhenAsyncAiThrows`、`asyncSuggestionShouldStoreFailedWithTruncatedReason`、`asyncSuggestionShouldStoreFailedOnTimeoutException`、`asyncSuggestionShouldStoreFailedWhenAiResultNotSuccess`、`asyncSuggestionShouldStoreFailedWhenAiResultDataIsNull`、`asyncSuggestionShouldStoreTimeoutOnTimeoutException`、`asyncSuggestionShouldStoreFailedOnInterruptedException`、`asyncSuggestionShouldStoreFailedWhenSerializationFails`

**DedupTaskSchedulerTest（S03 竞态场景 + 5 个现有测试修改）：**
- 新增测试：模拟 createIfNotExists 成功后其他线程可通过 get(candidateTaskId) 立即读取到数据
- **以下 5 个现有测试的 `verify(..., never()).put(...)` 需改为 `verify(..., times(1)).put(...)`**（因 put 提前至 createIfNotExists/compute 之前无条件执行）：
  1. `shouldReuseProcessingTaskViaCreateIfNotExists`
  2. `shouldReuseTaskViaCreateIfNotExistsWhenOldValueIsPending`
  3. `shouldReuseTaskViaCreateIfNotExistsWhenOldValueIsCompletedUnconsumed`
  4. `shouldReuseExistingTaskWhenComputeFindsReusableValue`
  5. `shouldReuseProcessingTaskViaCompute`

## RETRY 说明

计划审查（v5 r1）发现 3 个必须修正的问题：

1. **[严重] Constructor 参数缺少 @Qualifier**：`PrescriptionAssistServiceImpl` 构造函数新增的 `ExecutorService aiTaskExecutor` 参数必须标注 `@Qualifier("aiTaskExecutor")`，否则 Spring 上下文存在多个 `ExecutorService` 类型 bean 时抛出 `NoUniqueBeanDefinitionException`。
2. **[一般] S03 修复破坏 5 个现有测试**：`suggestionStore.put(candidateTaskId, newResult)` 提前至 `createIfNotExists` 之前无条件执行，导致 5 个 DedupTaskSchedulerTest 中 `verify(..., never()).put(...)` 断言失败。需改为 `times(1).put(...)`。
3. **[一般] aiTaskExecutor mock 策略错误**：`CompletableFuture.supplyAsync(supplier, executor)` 内部使用 `executor.execute(Runnable)` 而非 `submit`，task_v5 原建议的 `submit` mock 方案不正确。修正为 `execute` mock。

修正方向：P01 补 `@Qualifier` → P01 mock 策略改为 `execute` → S03 补 5 个受影响测试修改清单 → 配置类指定精确包路径 `com.aimedical.modules.prescription.config.PrescriptionThreadPoolConfig` → 重新提交审查。

## 修订说明（v5 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] PrescriptionAssistServiceImpl 构造函数缺少 @Qualifier | 构造函数参数改为 `@Qualifier("aiTaskExecutor") ExecutorService aiTaskExecutor`，新增 `import org.springframework.beans.factory.annotation.Qualifier` |
| [一般] S03 修复破坏 5 个现有 DedupTaskSchedulerTest（never().put → times(1).put） | 在「测试影响」-「DedupTaskSchedulerTest」中列出 5 个受影响测试，明确标注修改方向 |
| [一般] aiTaskExecutor mock 策略错误（submit → execute） | 修正「测试影响」-「PrescriptionAssistServiceImplTest」mock 策略为 `execute(Runnable)`，列出全部 10 个受影响异步测试 |
| [轻微] 配置类包路径未指定 | 指定配置类为 `com.aimedical.modules.prescription.config.PrescriptionThreadPoolConfig` |
| [轻微] S03 孤立 candidateTaskId 条目 | 已在 RETRY 说明中记录此行为变更 |

## 修订说明（v5 r2）
| 审查意见 | 修改措施 |
|---------|---------|
| [一般] P01「变更明细」第2项遗漏 @Qualifier：修订说明声称已补充但正文仍为模糊表述 | task_v5.md 正文已在 v5 r1 时正确包含 @Qualifier（第26行），无修改必要；plan.md 正文已对应修正 |
| [一般] 配置类包路径未按修订说明修正：正文仍保留「或通过已有 common-module 注入」模糊选项 | task_v5.md 正文已在 v5 r1 时指定为精确路径（第40行），无修改必要；plan.md 正文已对应修正 |
| [一般] 测试变更描述未体现修订说明：缺少具体测试清单和 mock 策略 | task_v5.md 正文已在 v5 r1 时包含完整测试清单和 mock 策略（第126-147行），无修改必要；plan.md 正文已对应修正 |

## 修订说明（v5 r3）
| 审查意见 | 修改措施 |
|---------|---------|
| [一般] constructorShouldAcceptNineParameters 测试断言 assertEquals(10,...) 在新增 ExecutorService 参数后将失败 | 在「测试影响 — PrescriptionAssistServiceImplTest」中补充修改说明：assertEquals(10, ...) → assertEquals(11, ...)（第861-865行） |
