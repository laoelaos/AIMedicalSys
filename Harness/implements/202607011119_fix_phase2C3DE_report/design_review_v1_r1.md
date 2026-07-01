# 设计审查报告（v1 r1）

## 审查结果
REJECTED

## 发现

- **[一般]** `CompletableFuture.supplyAsync()` 未指定线程池，默认使用 `ForkJoinPool.commonPool()`。同份诊断报告（T48）已将此模式标记为 "Web 请求高并发下线程耗尽风险"。C06 的根因正是"下游服务阻塞 → 线程耗尽"，若 doctorFacade 的 HTTP 连接池耗尽导致调用阻塞，commonPool 线程将成为新的耗尽点。设计应注入一个专用的 `ExecutorService`（如 Spring 的 `@Bean` 或 `Executors.newCachedThreadPool()`）传入 `supplyAsync`，或在设计中明确说明为什么在此处使用 commonPool 是可接受的。

- **[一般]** `future.get(timeout, TimeUnit.SECONDS)` 抛出的 `InterruptedException` 被现有 `catch (Exception e)` 捕获后未调用 `Thread.currentThread().interrupt()` 恢复中断标志。但同文件第 125–127 行（`aiTimeout` 超时处理）正确执行了 `Thread.currentThread().interrupt()`。两处超时处理的线程中断行为不一致。应在 catch 块中统一恢复中断标志。

- **[轻微]** 任务要求 "将 TriageResponse.doctors 置为空列表"，设计解释为 "跳过该科室（医生列表不追加）"。两种语义不同——前者会清空其他科室已成功获取的医生，后者仅跳过故障科室。设计采用的后者（不追加）更合理但与任务字面不完全一致，建议在设计中明确此语义选择并确认与需求一致。

## 修改要求

1. **supplyAsync 线程池问题**：`findDoctorsForDepartments` 中的 `CompletableFuture.supplyAsync(...)` 应接收一个专用的 `ExecutorService` 参数，或在设计文档中论证 ForkJoinPool.commonPool() 在当前并发场景下是可接受的（如每个 triage 请求最多 3 次 supplyAsync、2s 超时兜底、commonPool 并行度足够等定量分析）。否则该实现方案可能引入新的线程耗尽风险点，与 C06 的修复目标相悖。

2. **InterruptedException 中断标志**：`future.get()` 的 `InterruptedException` 被 `catch (Exception e)` 捕获后，应补充 `Thread.currentThread().interrupt()`，保持与同文件中 `aiTimeout` 处的中断处理一致。这是 Java 并发编程的标准实践，否则线程池线程的中断状态被静默清除可能导致后续操作异常。
