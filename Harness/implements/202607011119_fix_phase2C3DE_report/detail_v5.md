# 详细设计（v5）

## 概述

修复 prescription 模块两个 P1 缺陷：P01（异步 AI 调度线程池绑定 + 异常处理，使用虚拟线程隔离 AI 任务）和 S03（DedupTaskScheduler 跨 key 竞态）。涉及 3 个源文件修改、1 个新增文件和 2 个测试文件更新。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `prescription/.../config/PrescriptionThreadPoolConfig.java` | 新建 | 定义 `@Bean("aiTaskExecutor")` ExecutorService，使用虚拟线程工厂 |
| `prescription/.../service/assist/impl/PrescriptionAssistServiceImpl.java` | 修改 | 新增 `ExecutorService aiTaskExecutor` 字段 + 构造函数 `@Qualifier` 注入 + `supplyAsync` 绑定 + `exceptionally` 处理 |
| `prescription/.../service/assist/DedupTaskScheduler.java` | 修改 | `put(candidateTaskId, newResult)` 提前至 `createIfNotExists`/`compute` 之前无条件执行 |
| `prescription/.../service/assist/impl/PrescriptionAssistServiceImplTest.java` | 修改 | 构造函数新增 `ExecutorService` mock 参数（参数计数 10→11）；10 个异步测试增加 `aiTaskExecutor.execute` mock |
| `prescription/.../service/assist/DedupTaskSchedulerTest.java` | 修改 | 新增 S03 竞态场景测试；5 个现有测试 `never().put` → `times(1).put` |

## 类型定义

### `PrescriptionThreadPoolConfig`（新建配置类）

**形态**：class
**包路径**：`com.aimedical.modules.prescription.config`
**职责**：定义 prescription 模块级 AI 异步任务线程池 Bean，使用虚拟线程隔离 AI 任务与 Web 请求线程池

```java
package com.aimedical.modules.prescription.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class PrescriptionThreadPoolConfig {

    @Bean("aiTaskExecutor")
    public ExecutorService aiTaskExecutor() {
        return Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("ai-task-").factory()
        );
    }
}
```

**JDK 版本决策**：JDK 21，虚拟线程已正式 GA。使用虚拟线程的优势：
- 轻量级，不与 ForkJoinPool.commonPool() 竞争
- 适合 IO 密集型 AI 调用（等待网络响应时不阻塞平台线程）
- 无需手动配置线程池大小

**公开接口**：无（纯配置 Bean，不对外暴露方法）
**构造方式**：Spring `@Configuration` + `@Bean` 注解自动实例化
**类型关系**：无

### `PrescriptionAssistServiceImpl`（已有类，修改）

**包路径**：`com.aimedical.modules.prescription.service.assist.impl`
**职责**：辅助开方服务实现；新增 ExecutorService 绑定 + exceptionally 异常处理

**新增导入**：
```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CompletionException;
import org.springframework.beans.factory.annotation.Qualifier;
```

**新增字段**：
```java
private final ExecutorService aiTaskExecutor;
```

**构造函数修改**：参数列表末尾新增 `@Qualifier("aiTaskExecutor") ExecutorService aiTaskExecutor`，赋值 `this.aiTaskExecutor = aiTaskExecutor;`

构造函数签名变化（参数计数 10→11）：
```java
// 修改后（第12个参数）：
public PrescriptionAssistServiceImpl(AiService aiService,
                                      AssistConverter assistConverter,
                                      AllergyCheckRule allergyCheckRule,
                                      DosageThresholdService dosageThresholdService,
                                      PrescriptionDraftContext prescriptionDraftContext,
                                      DedupTaskScheduler dedupTaskScheduler,
                                      SuggestionStore suggestionStore,
                                      ObjectMapper objectMapper,
                                      @Value("${ai.timeout.prescription-assist:8}") long aiTimeout,
                                      DrugFacade drugFacade,
                                      @Qualifier("aiTaskExecutor") ExecutorService aiTaskExecutor) {
    // ...
    this.aiTaskExecutor = aiTaskExecutor;
}
```

**修改 `scheduleSuggestionAsync()` 方法**（第343-381行）：

```java
// 修改前：
CompletableFuture.supplyAsync(() -> { ... });  // 默认 ForkJoinPool，CF 未消费

// 修改后：
CompletableFuture.supplyAsync(() -> {
    // ... 完全相同 lambda body（第344-380行）
}, aiTaskExecutor).exceptionally(ex -> {
    log.warn("Async AI suggestion task failed for taskId={}: {}", taskId,
             ex instanceof CompletionException && ex.getCause() != null
                 ? ex.getCause().getMessage() : ex.getMessage());
    return null;
});
```

**变更要点**：
1. `supplyAsync(supplier, aiTaskExecutor)` — 绑定虚拟线程池
2. `.exceptionally(...)` — 捕获异步执行过程中未在 lambda 内处理的异常（如 OutOfMemoryError、uncaught RuntimeException），记录 WARN 日志并返回 null，防止异常静默丢失
3. `CompletionException` 解包 — 当 `supplyAsync` lambda 内部抛出异常时，`CompletableFuture` 包装为 `CompletionException`，需解包获取真实原因

### `DedupTaskScheduler.schedule()`（已有类，修改）

**包路径**：`com.aimedical.modules.prescription.service.assist`
**职责**：异步任务去重调度器；消除跨 key 竞态窗口

**修改 `schedule()` 方法**：将 `put(candidateTaskId, newResult)` 从两处分支内提前至 `createIfNotExists` 之前无条件执行

```java
// 修改前：第39-43行、第63-67行两处 put 在 createIfNotExists/compute 分支内部

// 修改后：
public String schedule(String prescriptionId) {
    String dedupKey = DEDUP_KEY_PREFIX + prescriptionId;
    String candidateTaskId = UUID.randomUUID().toString();

    Object existing = suggestionStore.get(dedupKey);
    if (existing instanceof AiSuggestionResult r) {
        if (r.getStatus() == AiSuggestionStatus.PENDING
                || r.getStatus() == AiSuggestionStatus.PROCESSING
                || (r.getStatus() == AiSuggestionStatus.COMPLETED && !r.isConsumed())) {
            return r.getTaskId();
        }
    }

    AiSuggestionResult newResult = new AiSuggestionResult();
    newResult.setTaskId(candidateTaskId);
    newResult.setStatus(AiSuggestionStatus.PENDING);
    newResult.setCreateTime(LocalDateTime.now());

    // ★ 提前 put：candidateTaskId 的数据在 dedupKey 被声明前就可供 get(key) 读取
    suggestionStore.put(candidateTaskId, newResult);

    Object oldValue = suggestionStore.createIfNotExists(dedupKey, newResult);
    if (oldValue == null) {
        return candidateTaskId;  // ★ 简化：不再重复 put
    }

    if (oldValue instanceof AiSuggestionResult r) {
        if (r.getStatus() == AiSuggestionStatus.PENDING
                || r.getStatus() == AiSuggestionStatus.PROCESSING
                || (r.getStatus() == AiSuggestionStatus.COMPLETED && !r.isConsumed())) {
            return r.getTaskId();
        }
    }

    Object result = suggestionStore.compute(dedupKey, (key, currentValue) -> {
        // ... 相同 lambda body ...
    });

    if (result == newResult) {
        return candidateTaskId;  // ★ 简化：不再重复 put
    } else if (result instanceof AiSuggestionResult winner) {
        return winner.getTaskId();
    } else {
        throw new IllegalStateException("Unexpected value type for dedupKey: " + result);
    }
}
```

**行为变化**：
- `put(candidateTaskId, newResult)` 现在无条件执行一次（在 createIfNotExists 之前）
- 如果其他线程已通过 dedupKey 读到 candidateTaskId 并调用 `get(candidateTaskId)`，数据已可读
- 如果其他线程的 compute 将 newResult 替换为其他值，candidateTaskId 条目变成孤立数据——但 get(key) 找不到时调用方返回 RX_ASSIST_SUGGESTION_NOT_FOUND，不会导致数据损坏

## 错误处理

- **P01**：`exceptionally` 回调使用 `log.warn` 记录异步任务未捕获异常，返回 `null`（CompletableFuture<Void> 等价）。lambda 内部已通过 try-catch 覆盖所有预期异常（TimeoutException/ExecutionException/InterruptedException/Exception），`exceptionally` 仅作为兜底安全网
- **S03**：孤立 candidateTaskId 条目的 `get(candidateTaskId)` 返回 null 时，调用方 `PrescriptionAssistServiceImpl.getSuggestion()` 抛出 `BusinessException(RX_ASSIST_SUGGESTION_NOT_FOUND)`，不会导致 NPE 或数据损坏

## 行为契约

1. **P01**：`scheduleSuggestionAsync()` 异步任务不再使用 ForkJoinPool.commonPool()，使用独立的虚拟线程池 `aiTaskExecutor`。Web 请求线程与 AI 调度线程互相隔离。
2. **P01**：异步任务中任何未在 lambda try-catch 中捕获的异常（如 StackOverflowError）由 `exceptionally` 捕获并记录 WARN 日志，不再静默丢失。
3. **S03**：`put(candidateTaskId, newResult)` 在执行 `createIfNotExists(dedupKey, ...)` 前完成，确保 createIfNotExists 成功时刻起、其他线程通过 dedupKey 读到 candidateTaskId 后即可 `get(candidateTaskId)` 获取到数据。
4. **S03**：孤立 candidateTaskId 条目不会在 schedule() 中清理。当对应 dedupKey 的 task 最终完成时，该条目通过 suggestionStore.put(taskId, completedResult) 正常更新，无需特殊清理。

## 依赖关系

- **P01**：新增对 `java.util.concurrent.ExecutorService`、`java.util.concurrent.CompletionException`、`org.springframework.beans.factory.annotation.Qualifier` 的编译期依赖
- **S03**：无新增依赖，仅内部行重排
- 新增 `PrescriptionThreadPoolConfig` 类依赖 `org.springframework.context.annotation.Configuration` 和 `org.springframework.context.annotation.Bean`，prescription 模块 pom.xml 已包含 spring-context 依赖，无需新增 Maven 依赖

## 测试影响

### `PrescriptionAssistServiceImplTest`（构造函数参数计数 10→11）

1. **新增字段和 mock**：
   ```java
   import java.util.concurrent.ExecutorService;
   @Mock private ExecutorService aiTaskExecutor;
   ```

2. **构造函数调用修改**（`setUp()` 第56-57行）：
   ```java
   // 修改前：
   service = new PrescriptionAssistServiceImpl(aiService, assistConverter, allergyCheckRule,
           dosageThresholdService, prescriptionDraftContext, dedupTaskScheduler, suggestionStore, objectMapper, 8L, drugFacade);
   // 修改后：
   service = new PrescriptionAssistServiceImpl(aiService, assistConverter, allergyCheckRule,
           dosageThresholdService, prescriptionDraftContext, dedupTaskScheduler, suggestionStore, objectMapper, 8L, drugFacade, aiTaskExecutor);
   ```

3. **`constructorShouldAcceptNineParameters` 测试**（第861-865行）：方法名保持但语义变更为 11 参数：
   ```java
   assertEquals(11, constructor[0].getParameterCount());  // 10 → 11
   ```
   方法名可考虑重命名为 `constructorShouldAcceptElevenParameters`，但保持原名也可（仅修改断言值）。

4. **10 个异步测试增加 `aiTaskExecutor.execute` mock**：
   所有异步测试在 setup 或 test 方法中添加：
   ```java
   doAnswer(invocation -> {
       Runnable r = invocation.getArgument(0);
       r.run();
       return null;
   }).when(aiTaskExecutor).execute(any(Runnable.class));
   ```
   
   `CompletableFuture.supplyAsync(supplier, executor)` 内部调用 `executor.execute(Runnable)`，需 mock `execute` 方法以便 lambda 同步执行。

   受影响测试清单（均在 `asyncSuggestionShouldStore*` 系列中）：
   1. `asyncSuggestionShouldStoreCompletedOnAiSuccess`（第469行）
   2. `asyncSuggestionShouldStoreCompletedWithSerializedSuggestion`（第499行）
   3. `asyncSuggestionShouldStoreFailedWhenAsyncAiThrows`（第533行）
   4. `asyncSuggestionShouldStoreFailedWithTruncatedReason`（第570行）
   5. `asyncSuggestionShouldStoreFailedOnTimeoutException`（第610行）
   6. `asyncSuggestionShouldStoreFailedWhenAiResultNotSuccess`（第647行）
   7. `asyncSuggestionShouldStoreFailedWhenAiResultDataIsNull`（第682行）
   8. `asyncSuggestionShouldStoreTimeoutOnTimeoutException`（第719行）
   9. `asyncSuggestionShouldStoreFailedOnInterruptedException`（第759行）
   10. `asyncSuggestionShouldStoreFailedWhenSerializationFails`（第882行）

   **mock 添加方式建议**：在 `@BeforeEach` `setUp()` 中统一添加全局 `execute` mock（使用 `lenient()` 避免非异步测试中不必要的 stub 警告），或在每个异步测试方法内添加。

   **`asyncSuggestionShouldStoreFailedWhenSerializationFails` 特殊处理**：该测试构造 `failingMapper` 和新的 `testService` 实例（第905-907行），需在新实例构造函数中传入 `aiTaskExecutor` 参数。

5. **`assistShouldTriggerAsyncSchedulingWhenSyncAiSucceeds` 测试**（第409行）：该测试验证 `assist()` 触发 `dedupTaskScheduler.schedule()` 调用，不验证异步结果。若使用了全局 `execute` mock 则不受影响；若未使用全局 mock，此测试无需 `execute` stub（`supplyAsync` 被调用时 executor.execute 仅被调用但不影响测试断言）。建议使用 `lenient()` 统一 stub。

### `DedupTaskSchedulerTest`（S03 竞态场景 + 5 个现有测试修改）

1. **新增测试：S03 竞态场景验证**
   ```java
   @Test
   void shouldGuaranteeCandidateTaskIdVisibilityAfterCreateIfNotExists() {
       // 验证 put(candidateTaskId) 在 createIfNotExists(dedupKey) 之前执行
       // 确保另一线程通过 dedupKey 读到 candidateTaskId 后，get(candidateTaskId) 立即有数据
       when(suggestionStore.get("suggestion-dedup:rx-001")).thenReturn(null);
       when(suggestionStore.createIfNotExists(eq("suggestion-dedup:rx-001"), any(AiSuggestionResult.class)))
               .thenReturn(null);
       
       // 验证 put 调用顺序：先 put(candidateTaskId) 后 createIfNotExists(dedupKey)
       InOrder inOrder = inOrder(suggestionStore);
       scheduler.schedule("rx-001");
       inOrder.verify(suggestionStore).put(anyString(), any(AiSuggestionResult.class));
       inOrder.verify(suggestionStore).createIfNotExists(anyString(), any(AiSuggestionResult.class));
   }
   ```

2. **以下 5 个测试 `verify(..., never()).put(...)` → `verify(..., times(1)).put(...)`**（因 put 提前无条件执行，非快路径重用时 always 执行一次）：
   1. `shouldReuseProcessingTaskViaCreateIfNotExists`（第277行）
   2. `shouldReuseTaskViaCreateIfNotExistsWhenOldValueIsPending`（第204行）
   3. `shouldReuseTaskViaCreateIfNotExistsWhenOldValueIsCompletedUnconsumed`（第222行）
   4. `shouldReuseExistingTaskWhenComputeFindsReusableValue`（第158行）
   5. `shouldReuseProcessingTaskViaCompute`（第301行）

   修改示例：
   ```java
   // 修改前：
   verify(suggestionStore, never()).put(anyString(), any());
   // 修改后：
   verify(suggestionStore, times(1)).put(anyString(), any(AiSuggestionResult.class));
   ```
