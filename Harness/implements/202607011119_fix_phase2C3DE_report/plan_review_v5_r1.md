# 计划审查报告（v5 r1）

## 审查结果
REJECTED

## 发现

### **[严重] PrescriptionAssistServiceImpl 构造函数缺少 @Qualifier("aiTaskExecutor")**

plan.md R5 节（第87-88行）指定 PrescriptionAssistServiceImpl 构造函数新增 `ExecutorService aiTaskExecutor` 参数，并在配置类中定义 `@Bean("aiTaskExecutor")`。但构造函数参数未标注 `@Qualifier("aiTaskExecutor")`。

**风险**：Spring Boot 自动装配可能提供多个 `ExecutorService` 类型 bean（如 `ThreadPoolTaskScheduler` 实现 `ScheduledExecutorService` → `ExecutorService`）。缺少 `@Qualifier` 时，如果上下文存在多个同类型 bean，Spring 抛出 `NoUniqueBeanDefinitionException`，导致应用启动失败。即使当前仅此一个 bean，这种写法脆弱且违反最佳实践。

**期望修正**：构造函数参数添加 `@Qualifier("aiTaskExecutor")`，确保与同名的 bean 精确绑定。

---

### **[一般] S03 修复将破坏 5 个现有 DedupTaskSchedulerTest 测试**

plan.md S03 伪代码（第128-129行）将 `suggestionStore.put(candidateTaskId, newResult)` 移至 `createIfNotExists` 之前无条件执行。但现有 5 个测试断言 `verify(suggestionStore, never()).put(anyString(), any())`：

1. `shouldReuseProcessingTaskViaCreateIfNotExists`（第278行）
2. `shouldReuseTaskViaCreateIfNotExistsWhenOldValueIsPending`（第206行）
3. `shouldReuseTaskViaCreateIfNotExistsWhenOldValueIsCompletedUnconsumed`（第224行）
4. `shouldReuseExistingTaskWhenComputeFindsReusableValue`（第158行）
5. `shouldReuseProcessingTaskViaCompute`（第301行）

S03 修复后，这些路径中 `put` 将在 `createIfNotExists` 前执行，导致 `never()` 断言失败。plan.md 未识别此冲击。

**期望修正**：在计划中列出上述受影响的测试，明确标注需将 `never().put()` 改为 `times(1).put()`。

---

### **[一般] aiTaskExecutor 的 Mock 策略未被明确指定**

plan.md 第162行笼统提及 "构造函数新增 ExecutorService mock 参数 + 测试适配"，但未指定 mock 方式。事实上，`CompletableFuture.supplyAsync(supplier, executor)` 内部调用 `executor.execute(Runnable)`（而非 `submit`），因此 mock 必须使用 `execute`：

```java
doAnswer(invocation -> {
    Runnable r = invocation.getArgument(0);
    r.run();
    return null;
}).when(aiTaskExecutor).execute(any(Runnable.class));
```

缺少此 mock 时，supplyAsync 中的异步任务不会执行，依赖于 `Thread.sleep(300)` 等待异步完成的测试（如 `asyncSuggestionShouldStoreCompletedOnAiSuccess`、`asyncSuggestionShouldStoreFailedWhenAsyncAiThrows` 等）将永远得不到结果，导致断言失败。

task_v5.md 第125行建议的 `submit` mock 方案不正确。

**期望修正**：计划应明确指定使用 `execute`（而非 `submit`）的 mock 策略，并列出所有受 mock 影响的测试。

---

### **[轻微] 配置类所在包路径未指定**

plan.md 第91行写 "新增/修改配置类（定义 aiTaskExecutor bean）"，未指定配置类的具体包路径和类名。prescription 模块目前无 `*Config.java`，plan 应指明是新建 `com.aimedical.modules.prescription.config.PrescriptionThreadPoolConfig` 还是在已有配置类中追加。

**期望修正**：明确配置类的包路径和类名，确保组件扫描能发现。

---

### **[轻微] S03 修复在非成功路径产生孤立 candidateTaskId 条目**

plan.md 第105行已承认此行为变更：当 `createIfNotExists` 返回可复用任务或 `compute` 判定其他线程胜利时，`candidateTaskId` 对应的条目成为孤立数据（当前代码中此路径不产生孤立条目）。plan 虽提及 TTL 会清理，但建议将此行为变更明确记录到详细设计的行为契约中，避免后续排查困扰。

**期望修正**：无需修改 plan，建议在 detail_v5.md 的行为契约中显式记录。

## 修改要求

1. **[严重]** PrescriptionAssistServiceImpl 构造函数参数添加 `@Qualifier("aiTaskExecutor")`
2. **[一般]** 明确列出 5 个受 S03 影响需修改的 DedupTaskSchedulerTest 测试
3. **[一般]** 明确指定 aiTaskExecutor 的 mock 策略为 `execute` 而非 `submit`
4. **[轻微]** 指定 aiTaskExecutor 配置类的包路径和类名
