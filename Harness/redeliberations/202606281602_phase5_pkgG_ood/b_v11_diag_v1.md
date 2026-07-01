## 质量审查报告（v11）

### 一、需求响应充分度

产出充分响应了用户需求，完整覆盖 Phase 5 包 G 的 OOD 核心要素：类图（§2.3）、核心职责（§3 各组件定义）、协作关系（§3 各组件协作对象段落及 §2.3 类图关联线）、关键接口（§1.3 核心抽象一览、§3.x 接口方法签名）、状态模型（§3.2 CredentialProvider/ModelEndpointHealthManager、§3.8 CircuitBreakerDegradationStrategy 等）、迁移路径（§9）。整体结构参照 Phase0/Phase1ABD 风格一致。

### 二、存在的问题

#### 问题 1：`scheduledTaskExecutor` Bean 命名与 `@Scheduled` 自动装配约定不一致

- **位置**：§3.9 `AiPlatformConfig` 核心 @Bean 方法签名段，`@Bean("scheduledTaskExecutor")` 定义（行 2411–2426）
- **问题描述**：Spring `@EnableScheduling` 在启动期查找 `TaskScheduler` 类型 Bean 时，默认的行为是按 **Bean 名称 `taskScheduler`** 查找（`ScheduledAnnotationBeanPostProcessor` 的默认策略）。当前 Bean 名称为 `scheduledTaskExecutor`，与约定名称不匹配，导致该自定义线程池不会被 `@Scheduled` 注解自动使用。底座中 3 处 `@Scheduled` 定时任务（§3.5 的 DDL 分区清理、§3.2 CredentialProvider 退避探测、§6.1 ModelRouter 轮询刷新）将退回到 Spring 默认的单线程 `TaskScheduler`（poolSize=1），与设计意图"poolSize=3 确保三个任务不互相阻塞"相悖。这是 v10 迭代修正引入的方案，但 Bean 命名细节被遗漏。
- **严重程度**：重要
- **改进建议**：将 `@Bean("scheduledTaskExecutor")` 改为 `@Bean` 并将方法名改为 `taskScheduler`，使 Spring Boot 自动配置识别此 Bean 并替换默认单线程调度器；或保留现有名称但显式实现 `SchedulingConfigurer` 注入该 Executor。推荐方案 A（改名），改动量最小且与 Spring 约定一致。

#### 问题 2：`ExperimentGroup` 类图节点缺失（图-文不一致）

- **位置**：§2.3 类图（行 518–531），§3.4 文本定义（行 1816–1835）
- **问题描述**：§3.4 已完整定义 `ExperimentGroup` JPA Entity 的字段表（含 id/experiment/group_id/percentage/target_model_id/target_prompt_version 等）、流量分配算法约束（千分比哈希分桶）及 `@OneToMany` 关联映射。但 §2.3 类图中仅 `Experiment` 类存在字段 `+List<ExperimentGroup> groups`，缺失 `ExperimentGroup` 类节点及其与 `Experiment` 的关联线。实现者以类图为主要参考时将误认为 `ExperimentGroup` 是简单类型或元组，忽略其 JPA Entity 身份和字段约束。
- **严重程度**：一般
- **改进建议**：在 §2.3 类图中新增 `ExperimentGroup` 类节点（含关键字段 `id`/`groupId`/`percentage`/`targetModelId`/`targetPromptVersion`），并添加 `Experiment "1" --> "*" ExperimentGroup : has` 关联线。

#### 问题 3：`AiCallLogStats` 类图节点缺失（图-文不一致）

- **位置**：§2.3 类图（行 571–599），§3.5 文本定义（行 2040–2084）
- **问题描述**：§3.5 完整定义了 `AiCallLogStats` JPA Entity 的字段表（id/capability_id/stat_month/total_calls/success_count/degraded_count/failure_count/avg_elapsed_ms/p50_elapsed_ms/p95_elapsed_ms/p99_elapsed_ms）、查询索引及聚合 SQL，但 §2.3 类图中未出现此类。v3 迭代第 8 项审查意见已指出此缺口并在文本中补充了定义，但类图未同步更新。
- **严重程度**：一般
- **改进建议**：在 §2.3 类图的 `metrics/` 子包分区中新增 `AiCallLogStats` 类节点（含 key 字段），并标注其与 `AiCallLogRepository` 的关联关系（统计数据的持久化目标）。

#### 问题 4：`StructuredOutputParser.parse()` 独立超时未体现于 §4.1 伪代码

- **位置**：§3.2（行 1508）文本描述 vs §4.1（行 2901）伪代码
- **问题描述**：§3.2 明确声明"parse() 步骤本身有独立的简单超时控制（StructuredOutputParser.parse() 调用包裹 CompletableFuture.supplyAsync().get(5s)）"，但在 §4.1 `doExecuteInternal()` 的 chat() 回退路径中，`structuredOutputParser.parse(chatResponse, outputType)` 调用（行 2901）直接内联执行，无任何超时包裹。实现者仅参照 §4.1 伪代码将遗漏此 5s 超时保护，解析步骤可能因大 JSON 或异常输入无限挂起，消耗调用线程栈。
- **严重程度**：一般
- **改进建议**：在 §4.1 行 2901 处补充伪代码包裹逻辑，例如：
  ```
  parseFuture = CompletableFuture.supplyAsync(() -> structuredOutputParser.parse(chatResponse, outputType))
  parsedResult = parseFuture.get(5, TimeUnit.SECONDS)
  ```
  并在 catch `TimeoutException` 时走降级路径。
