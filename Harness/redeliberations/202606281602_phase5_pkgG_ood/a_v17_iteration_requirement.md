根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 问题A：[严重] 降级路径系统性双重计数

异常处理分支（结构化超时降级路径、chat回退超时降级路径、LlmInfrastructureException降级路径）先主动记录 metricsCollector.record(AiCallRecord.failure(...)) + slidingWindowMetricsStore.recordFailure(capabilityId)，然后调用 doDegrade()，后者在无 LocalRuleFallback 时又记录 metricsCollector.record(AiCallRecord.degraded(...)) + slidingWindowMetricsStore.recordFailure(capabilityId)。导致同一次调用在指标系统中同时记入"失败"和"降级"两个分类。

**受影响位置**：§4.1 三条降级路径

**改进建议**：异常处理分支在调用 doDegrade() 前移除所有 metricsCollector.record() 和 slidingWindowMetricsStore.recordFailure() 的预记录调用，将指标和滑动窗口的记录职责完全委托给 doDegrade() 统一承担。

### 问题B：[重要] AiCallRecord 工厂方法缺少哨兵参数

§3.4 定义 AiCallRecord.promptVersion 空值守卫标记方案——实验分流异常场景写入 prompt_version=-1 哨兵值，但 §3.5 工厂方法签名中既无 sentinelReason 参数也无其他区分机制，实现者无从知晓如何区分"实验分流异常"与"无实验命中"。

**受影响位置**：§3.4、§3.5、§4.1

**改进建议**：在 AiCallRecord 工厂方法中增加 sentinelReason 可选参数；同步更新 §4.1 中所有实验分流异常降级调用点的伪代码。

### 问题C：[重要] structuredChat 内部超时与 orTimeout 的时间竞争

structuredChat 使用内部超时 structuredChatTimeout（capabilityTimeout 的 60%），但 orTimeout() 从 supplyAsync() 启动时即开始计时。若前置步骤消耗了可观时间，orTimeout 剩余时间可能显著小于 structuredChatTimeout，导致超时分类和降级原因记录失准。

**受影响位置**：§4.1 doExecuteInternal()、§3.2 structuredChat() 回退路径

**改进建议**：在 supplyAsync() lambda 入口处即时计算 remainingBeforeOrTimeout = capabilityTimeout - elapsedBeforeLambda，按剩余时间重新分配 structuredChat 和 chat 回退的内部超时阈值，而非使用原始 capabilityTimeout 的固定比例。

### 问题D：[一般] parseTimeout 与 chatFallbackTimeout 的层级约束未定义

parseTimeout（默认 5s）与 chatFallbackTimeout（capabilityTimeout 的 40%）无层级约束关系，当 parseTimeout > chatFallbackTimeout 时整体超时先到，降级原因被记为 chat 回退超时而非解析超时。

**受影响位置**：§4.1、§9.5、§3.2

**改进建议**：在 §3.2 或 §3.9 中补充 parseTimeout <= chatFallbackTimeout 约束；在 §9.5 配置注释中为 parse.per-capability 添加约束说明；在 AiPlatformConfig 的 @PostConstruct 配置校验中增加两者比较逻辑。

### 问题E：[一般] 分布式重构优先级遗漏熔断器-滑动窗口依赖链

§1.5.3 列出分布式重构优先级为 CircuitBreakerDegradationStrategy（最高）> EndpointRateLimiter（重要）> SlidingWindowMetricsStore（可选）。但 CircuitBreakerDegradationStrategy 依赖 SlidingWindowMetricsStore.getFailureRate() 提供实时失败率数据，此依赖关系未在重构优先级段或 §3.8 中记录。

**受影响位置**：§1.5.3、§3.8

**改进建议**：在 §1.5.3 中补充依赖链说明；在 §3.8 的"协作对象"段补充数据源依赖说明。

### 问题F：[一般] DiscussionConclusionCapabilityExecutor 前置压缩调用的超时控制未在伪代码中体现

§3.11.7 已定稿 transcriptSummaryExecutor 线程池隔离方案和 transcriptSummaryTimeout（15s）配置项，但 §4.1 的 doExecuteInternal() 伪代码中未体现 DiscussionConclusionCapabilityExecutor 的前置 LLM 压缩调用逻辑。

**受影响位置**：§3.11.7、§4.1、§9.5

**改进建议**：在 §4.1 中为 DiscussionConclusionCapabilityExecutor 补充特化的 doExecuteInternal() 伪代码段，覆盖前置压缩调用的超时、截断回退条件、隔离线程池提交逻辑。

## 历史迭代回顾

### 与第16轮的关系（所有6项问题均为持续性问题）

- **问题A（双重计数）**：持续存在。v15已修复DEGRADED预记问题，但v16中3处异常处理分支仍存在同类双重计数，未被v15修复覆盖。
- **问题B（哨兵参数）**：持续存在。v13已引入ExperimentAssignment.createErrorFallback()哨兵分组，但工厂方法签名始终未同步增加sentinelReason参数。
- **问题C（超时竞争）**：持续存在。v8已引入60%/40%内部超时拆分，但固定比例未考虑前置步骤消耗时间的时序竞争问题持续存在。
- **问题D（超时约束）**：持续存在。parseTimeout与chatFallbackTimeout的层级关系自引入parseTimeout配置以来从未定义。
- **问题E（依赖链）**：持续存在。§1.5.3分布式重构优先级自v6引入后未补充依赖链说明。
- **问题F（前置压缩伪代码）**：持续存在。DiscussionConclusionCapabilityExecutor前置压缩逻辑自v13引入线程隔离方案后始终未在§4.1伪代码中体现。

### 已解决的问题
第15轮及之前的所有问题（parseTimeoutConfig字段遗漏、文档声明矛盾、行号跳跃、DTO工作量估算表备注等）在第16轮中均不再被提及，视为已解决。

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281602_phase5_pkgG_ood\a_v16_copy_from_v15.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281602_phase5_pkgG_ood\requirement.md
