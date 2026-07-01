# 设计审查报告（v22 r1）

## 审查结果
APPROVED

## 发现

- **[轻微]** `scheduleSuggestionAsync()` 的设计未考虑 aiData 为 null 但与 aiResult.isSuccess()=true 同时出现的边缘情况（AiResult 契约违反，即 A07 问题）。若发生此情况，异步 AI 在 aiData=null 时仍被触发，而主路径在 L110 因 aiData==null 提前返回，导致不必要的异步调用。建议在插入点增加 `if (aiData != null)` 前置检查，或在异步方法内部通过参数传递 aiData 状态，避免契约违反时产生孤立异步任务。

- **[轻微]** 设计使用 `CompletableFuture.supplyAsync()` 默认的 `ForkJoinPool.commonPool()`，在 Spring Boot 生产环境中推荐注入自定义 `ThreadPoolTaskExecutor` 并传入 `supplyAsync(executor)`，避免异步 AI 调用与并行流等其他 ForkJoinPool 消费者争抢线程资源。任务描述允许"Spring 托管的 `TaskExecutor` 或 `ForkJoinPool.commonPool()`"两种方案，当前选择运行正确但建议在生产部署前评估独立线程池方案。

- **[轻微]** 异步 AI 调用复用的 `aiTimeout` 与同步调用相同（`ai.timeout.prescription-assist=8s`）。异步 AI 的超时语义可能与同步不同（异步路径有更长等待容忍度），建议评估是否引入独立的异步超时配置。

## 修改要求
无严重或一般问题。
