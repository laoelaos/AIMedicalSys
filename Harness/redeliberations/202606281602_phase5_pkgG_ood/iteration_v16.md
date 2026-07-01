# 再审议判定报告（v16）

## 判定结果

RETRY

## 判定理由

质询报告结果为 LOCATED，确认诊断报告的审查结论成立。诊断报告包含 1 项严重问题（问题A：降级路径系统性双重计数）、2 项重要问题（问题B：AiCallRecord 工厂方法缺少哨兵参数；问题C：structuredChat 内部超时与 orTimeout 的时间竞争）和 3 项一般问题（问题D/F/E），均属严重或一般等级。组件B内部循环实际轮次（2）未达最大轮次（12），审查已提前确认完毕。根据判定标准，审查报告包含严重或一般等级问题，应判定为 RETRY。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：降级路径系统性双重计数——异常处理分支先预记 metricsCollector.record(failure) + slidingWindowMetricsStore.recordFailure()，再调用 doDegrade() 重复记录 degraded + failure，同一次调用计入"失败"和"降级"两个分类
- **所在位置**：§4.1 结构化超时降级路径、chat回退超时降级路径、LlmInfrastructureException 降级路径
- **严重程度**：严重
- **改进建议**：异常处理分支在调用 doDegrade() 前移除所有预记录调用，将指标和滑动窗口的记录职责完全委托给 doDegrade() 统一承担

- **问题描述**：AiCallRecord 工厂方法缺少哨兵参数——§3.4 定义的 prompt_version=-1 哨兵值标记方案未在 §3.5 工厂方法签名和 §4.1 伪代码中落地
- **所在位置**：§3.4、§3.5、§4.1
- **严重程度**：重要
- **改进建议**：在 AiCallRecord 工厂方法中增加 sentinelReason 可选参数，使实验分流异常降级路径能标记 promptVersion=-1

- **问题描述**：structuredChat 内部超时与 orTimeout 的时间竞争——固定 60%/40% 比例未考虑前置步骤消耗的时间，orTimeout 剩余时间可能显著小于 structuredChatTimeout
- **所在位置**：§4.1 doExecuteInternal()、§3.2 structuredChat() 回退路径
- **严重程度**：重要
- **改进建议**：在 supplyAsync() lambda 入口处即时计算剩余时间，按剩余时间分配超时阈值而非使用原始 capabilityTimeout 的固定比例

- **问题描述**：parseTimeout 与 chatFallbackTimeout 的层级约束未定义——两者无约束关系可能导致超时被掩盖
- **所在位置**：§4.1、§9.5、§3.2
- **严重程度**：一般
- **改进建议**：在设计中补充 parseTimeout <= chatFallbackTimeout 约束，并在配置校验中增加比较逻辑

- **问题描述**：分布式重构优先级遗漏熔断器-滑动窗口依赖链——CircuitBreakerDegradationStrategy 依赖 SlidingWindowMetricsStore.getFailureRate() 但重构优先级未体现此依赖
- **所在位置**：§1.5.3、§3.8
- **严重程度**：一般
- **改进建议**：在 §1.5.3 中补充依赖链说明，熔断器重构需与滑动窗口同步或顺序在前

- **问题描述**：DiscussionConclusionCapabilityExecutor 前置压缩调用的超时控制未在伪代码中体现
- **所在位置**：§3.11.7、§4.1、§9.5
- **严重程度**：一般
- **改进建议**：在 §4.1 中补充特化伪代码段覆盖前置压缩调用的超时、截断回退条件、隔离线程池提交逻辑
