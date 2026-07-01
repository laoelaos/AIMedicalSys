# OOD 设计方案审查报告（v15）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 设计中的类型形态选择（interface / abstract class / class / enum / JPA @Entity）全部在 Java 类型系统能力范围内。`CapabilityExecutor<T,R>` 和 `LocalRuleFallback<T,R>` 的泛型使用方式在 Java 泛型系统能力范围内。继承关系符合单继承+多接口实现约束：`AbstractCapabilityExecutor` 实现 `CapabilityExecutor`，13 个具体执行器扩展 `AbstractCapabilityExecutor`；`AiOrchestrator` / `FallbackAiService` / `MockAiService` 各自实现 `AiService`。`CredentialProvider.Credential` 嵌套接口类定义合法。`DegradationReason` 枚举常量与字符串拼接的降级原因传递方式在 Java 中可行。

**[轻微]** `TemplateStatus`、`ExperimentStatus`、`EndpointState` 等枚举在类图中引用但未在正文中显式定义，建议在对应章节补充枚举常量定义以确保完整性。

**[轻微]** `AbstractCapabilityExecutor.doDegrade()` 方法签名参数较多（12 个参数），虽不影响类型可行性，但实现时建议考虑 Builder 模式或参数对象封装以提升可读性。

### 2. 标准库与生态覆盖

**[通过]** `CompletableFuture`、`ConcurrentHashMap`、`AtomicReference`、`Duration`、`LocalDateTime` 等均在 Java 标准库覆盖范围内。Spring Framework 生态（`@Component`、`@Qualifier`、`@ConditionalOnProperty`、`@Async`、`@PostConstruct`、`@EventListener`、`ObjectProvider`、`EnvironmentPostProcessor`）完整覆盖 Bean 装配与配置转发需求。Spring Data JPA 覆盖 `AiCallLogEntity`/`PromptTemplate`/`Experiment` 的持久化。Micrometer / Spring Boot Actuator 覆盖指标推送与端点暴露。Jackson 覆盖 DTO 序列化/防御性拷贝。Caffeine/Guava Cache 覆盖凭证缓存策略。上述依赖均在 `ai-impl/pom.xml` 中显式声明（含 Actuator），生态覆盖完整。

**[轻微]** `CompletableFuture.orTimeout()` 自 Java 9 引入，若项目仍基于 Java 8 需替换为 `get(timeout, unit)` + `completeExceptionally()` 组合。建议在实现约束中注明最低 Java 版本要求。

### 3. 语言特性可行性

**[通过]** 错误处理策略与 Java try-catch + CompletableFuture 异常传播机制完全匹配：LLM 调用失败/解析失败等预期异常通过降级路径处理（不抛出业务异常），`CompletableFuture.orTimeout()` 超时兜底 + `.exceptionally()` 处理器覆盖非预期异常，模板渲染失败/实验分流异常通过 try-catch 包裹后回退。并发设计兼容 Java 并发模型：`CompletableFuture.supplyAsync()` 委托线程池、`ConcurrentHashMap` 保证滑动窗口并发安全、`AtomicReference` + CAS 保证熔断器状态转换原子性、`synchronized` 互斥防止 ModelRouter 并发刷新。资源管理方案（线程池拒绝策略 `CallerRunsPolicy` / `DiscardPolicy`、连接池、超时阈值）均在 Java 资源管理模式内可行。模块/包结构按单向依赖分层（orchestrator → router/template/experiment/client/metrics/parser/fallback/degradation），`ai-api` ← `ai-impl`，无循环依赖。

### 4. 设计一致性

**[通过]** 各抽象职责描述清晰无歧义：`AiOrchestrator` 为路由层（不介入管线内部步骤），`CapabilityExecutor` / `AbstractCapabilityExecutor` 持有完整管线。协作关系形成闭环：handle() → execute() → doExecuteInternal() → 实验/模板/路由/健康检查/LLM调用/解析/指标采集 → 降级路径全部覆盖。行为契约完整（§4.1-§4.6 伪代码 + §5.1 错误分类表 + §11 测试策略），足以指导后续实现。模块间依赖方向合理，`ai-impl` 薄适配器到 Phase 4 模块的出向依赖明确标注且使用 `provided` 作用域治理。

**[轻微]** 管线伪代码中 `doDegrade()` 同时被 `execute()` 模板方法（降级预检路径）和 `doExecuteInternal()`（运行时降级路径）调用，两处 `promptVersion` 的来源规则不同（预检路径为 null，运行时路径来自 `assignment.getTargetPromptVersion()`），建议在方法 Javadoc 或实现注释中明确标注此行为差异。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则：路由（AiOrchestrator）、执行管线（CapabilityExecutor）、模型路由（ModelRouter）、LLM 调用（LlmClient）、模板管理（PromptTemplateManager）、实验管理（ExperimentManager）、指标采集（AiMetricsCollector）、滑动窗口（SlidingWindowMetricsStore）各自职责内聚。抽象层次恰当——`AbstractCapabilityExecutor` 模板方法提取公共降级预检+指标采集+超时兜底，子类仅特化 `doExecuteInternal()`，既不过度设计也不设计不足。接口设计便于后续详细实现和单元测试（`@MockBean` 模拟下游依赖，模板方法使骨架可独立测试）。测试策略覆盖完整（§11.1-§11.3，含降级路径预检前置验证、防御性拷贝验证、异步上下文传播验证等管线收敛验证项）。

## 修改要求（REJECTED 时存在）

无。设计通过全部五个维度的审查，无严重或一般问题。

## 备注

本轮迭代需求（a_v13_iteration_requirement.md）所列 8 项问题（含 Phase 4 Maven 依赖范围、`CompletableFuture.cancel(true)` 超时路径、`doDegrade()` 缺 `promptVersion` 参数、`AiOrchestrator.handle()` 同步异常传播、`ExperimentAssignment` 构造方式、`ModelRoute` 密钥获取接口、薄适配器 `retryCount=0` 可观测性、`Experiment` 数据生命周期）已全部在 v15 版本中闭环修正。
