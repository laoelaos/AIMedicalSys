# OOD 设计方案审查报告（v5）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** `CapabilityExecutor<T, R>` 泛型接口设计合理——Java 泛型支持无界类型参数，`getInputType() Class<T>` / `getOutputType() Class<R>` 是运行时获取泛型类型的标准模式，配合 `instanceof` 或类型令牌可安全使用

**[通过]** `AiRequestBase` 使用 abstract class 而非 interface 是合理选择——Java 单继承限制下，abstract class 为 13 个 DTO 提供公共字段继承，不影响各 DTO 已有的其他继承需求

**[通过]** `DegradationStrategy.getOrder()` 使用 `default int getOrder() { return 0; }` 完全兼容 Java 8+，现有实现无需修改

**[通过]** 接口/抽象类/枚举/Entity 的类型形态选择均与 Java 类型系统匹配：interface（多实现契约）、abstract class（字段继承）、JPA @Entity（持久化）、enum（状态模型）

**[通过]** 类图中的继承关系（单继承 interface extends、多实现 `<|..`、组合 `o-->`）均在 Java 语法范围内

### 2. 标准库与生态覆盖

**[通过]** Spring Framework 能力完全覆盖设计中的 Bean 装配需求：`@Component`、`@Bean`、`@Qualifier`、`@Primary`、`@ConditionalOnProperty`、`@PostConstruct`、`@Async`、`ObjectProvider` 均为 Spring 核心功能

**[通过]** YAML 配置到 Bean 引用的 6 步装配路径（`AiPlatformConfig` + `ApplicationContext.getBeansOfType()`）是 Spring 编程式 Bean 注册的标准模式，已在众多项目中验证可行

**[通过]** 并发模型依赖的 `ConcurrentHashMap`、`AtomicReference`、`AtomicLong`、`CompletableFuture`、`ThreadPoolExecutor` 均为 Java 标准库；`CallerRunsPolicy` 为 `ThreadPoolExecutor` 内置拒绝策略

**[通过]** JPA 体系（`@Entity`、JPA Repository）完全覆盖 `AiCallLogEntity`、`PromptTemplate`、`Experiment` 的持久化需求

**[轻微]** `Micrometer` 指标推送（Timer/Counter/DistributionSummary）是 `spring-boot-starter-actuator` 标准能力，设计与 §8 显式声明依赖，避免自动配置缺失风险，处理得当

### 3. 语言特性可行性

**[通过]** 错误处理策略与 Java 异常机制匹配——预期异常（LLM 超时、解析失败）在 `CapabilityExecutor.execute()` 内部捕获并走降级路径（非异常路径），意外异常（NPE、序列化失败）由 `AiOrchestrator.handle()` 的 try-catch 兜底返回 `AiResult.error()`，防止 `CompletionException` 传播导致 HTTP 500

**[通过]** 异步边界清晰：`LlmClient.invoke()` 保持同步阻塞（符合 LLM HTTP 调用本质上是阻塞 RPC 的特性），`CapabilityExecutor` 通过 `CompletableFuture.supplyAsync()` + 共享 `LlmCallExecutor` 线程池包装为异步契约，不阻塞 Tomcat 容器线程

**[通过]** 线程安全模型可行——`SlidingWindowMetricsStore` 使用 `ConcurrentHashMap` + 写锁 + 快照读；`ModelEndpointHealthManager` 使用 `AtomicReference` 状态转换；`CircuitBreakerDegradationStrategy` 使用 `AtomicReference` + CAS + 重试；`ModelRouter` 使用 `AtomicReference<Map>` 全量替换避免增量更新的不一致问题

**[通过]** 模块/包结构符合 Maven 单模块组织方式，依赖方向单向（`orchestrator → {router, template, experiment, client, ...}`），无循环依赖

### 4. 设计一致性

**[通过]** 各抽象职责描述清晰无歧义：`AiOrchestrator` 为路由委托层、`CapabilityExecutor` 持有完整 8 步管线、各子组件（`ModelRouter`/`LlmClient`/`PromptTemplateManager`/`ExperimentManager`/`AiMetricsCollector`）职责单一明确

**[通过]** 协作关系形成闭环——从 `AiService` → `FallbackAiService` → `AiOrchestrator` → `CapabilityExecutor` → 各子组件 → `SlidingWindowMetricsStore`/`DegradationStrategy` → 降级路径，每个环节的输入输出和异常路径均有定义

**[通过]** 行为契约完整——§4.1 完整管线伪代码覆盖正常路径和降级路径；§4.2-§4.6 分别定义模型路由、A/B 实验、模板渲染、结构化解析、指标采集的契约

**[通过]** 模块间依赖方向合理——`ai-impl` 内部各子包单向依赖 `orchestrator/`；`template/`/`experiment/`/`metrics/` 独立拥有 JPA Repository + Entity，无交叉依赖

**[通过]** 12 个 Round 4 问题（P1-P12）在 v5 中均已定位并有对应解决措施：P1 `recordDegraded()` 三分类存储、P2 `departmentId` 加入 `AiRequestBase`、P3 异步边界明确化、P4 YAML→Bean 6 步装配路径、P5 薄适配器伪代码、P6 能力标识映射表、P7 `applyStrategies()` 迁移路径、P8 健康检查与熔断器交互优先级、P9 CapabilityExecutor 线程安全段、P10 userId 来源定义、P11 `AtomicReference<Map>` 全量替换、P12 `AiOrchestrator.handle()` try-catch

### 5. 设计质量

**[通过]** 职责划分符合单一职责原则——13 个 `CapabilityExecutor` 各自封装单项能力的完整管线；`AiOrchestrator` 仅做路由委托；`SlidingWindowMetricsStore` 仅做指标存储与上下文构建；各子组件职责独立不重叠

**[通过]** 抽象层次恰当——管线步骤未过度抽象为 Pipeline Step 链（v2.2 已评估并决定推迟至 Phase 6），13 种能力在当前阶段使用完整管线 + 薄适配器两种模式，未引入不必要的泛化层级

**[通过]** 便于后续实现——§4.1 提供完整伪代码（包括异常处理路径），每个 `CapabilityExecutor` 实现的步骤和协作对象明确，实现者可直接参照编码

**[通过]** 便于单元测试——`CapabilityExecutor` 依赖的 `ModelRouter`/`LlmClient`/`PromptTemplateManager`/`AiMetricsCollector` 等均为 interface，可 mock；`SlidingWindowMetricsStore` 为独立 class 可通过构造函数注入 mock 依赖

**[通过]** 设计决策表（§7）完整记录 22 项决策及理由，便于后续维护者理解设计取舍

## 修改要求

无
