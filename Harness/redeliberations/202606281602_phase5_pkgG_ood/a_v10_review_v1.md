# OOD 设计方案审查报告（v10）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 所有类型形态选择（interface / abstract class / class / enum / generic / JPA @Entity）均与 Java 17 类型系统完全匹配。`AiService` 作为 interface 定义 13 个方法的契约，`FallbackAiService` 作为 decorator class 实现装饰模式，`AiOrchestrator` 作为 class 实现路由委托——决策理由充分。`CapabilityExecutor<T, R>` 泛型接口正确处理了 13 项能力输入/输出类型各不相同的类型安全需求。`AbstractCapabilityExecutor<T, R>` 作为 abstract class 提供模板方法模式，共享降级预检和指标采集的公共实现。`LlmChatMessageRole`、`ClientType`、`AuthType`、`EndpointState` 等 enum 提供了编译期类型安全替代字符串字面量。`LlmChatResponse` 内嵌 `LlmChatUsage` 静态类的组合关系合理。

**[通过]** 继承和实现关系均在 Java 约束范围内：`AbstractCapabilityExecutor` 继承自 `CapabilityExecutor` 接口、`AbstractCapabilityExecutor` 作为单一抽象基类、13 个具体 executor 通过 `extends AbstractCapabilityExecutor` 获取公共骨架。`AiOrchestrator` 和 `FallbackAiService` 各自独立实现 `AiService` 接口。

**[通过]** 泛型使用方式（`CapabilityExecutor<T, R>`、`StructuredChatResult<T>`、`LocalRuleFallback<T, R>`、`AiResult<T>`）均在 Java 泛型系统能力范围内，无自引用泛型或通配符递归等复杂模式。

**[通过]** 协作关系中的类型交互模式（Map 查找、委派、策略链、事件驱动）均可在 Java 中实现。

### 2. 标准库与生态覆盖

**[通过]** Spring Boot 3.2.5 框架完整覆盖了全部基础设施需求：DI（`@Component`/`@Bean`/`@Autowired`）、条件化装配（`@ConditionalOnProperty`/`@ConditionalOnClass`）、配置绑定（`@ConfigurationProperties`）、AOP（`@Async`/`@Scheduled`）、事件驱动（`ApplicationEvent`/`@EventListener`）、JPA（`@Entity`/`@EnableJpaRepositories`）。

**[通过]** Jackson（`ObjectMapper.convertValue`/`@JsonProperty`/`@JsonCreator`/`@JsonIgnoreProperties`）覆盖所有 DTO 序列化/反序列化/防御性拷贝需求。Spring Data JPA 覆盖持久化。Caffeine 覆盖缓存。Guava RateLimiter 覆盖令牌桶限流。Micrometer + Actuator 覆盖指标采集和暴露。

**[通过]** 设计的依赖假设全部合理：`ai-impl/pom.xml` 中声明的编译期强制依赖和条件性依赖（Reactor、Spring AI、OkHttp）均与 Spring Boot 3.2.5 生态兼容。Phase 4 模块使用 `provided` 作用域的运行时风险已在 §2.2 显式记录并给出了部署约束说明。

**[通过]** 标准库能力可简化部分自定义抽象：例如 `ModelEndpointHealthManager` 的健康探测机制可参考 Spring Retry / Resilience4j 的断路器实现，但当前设计自行实现状态机的理由（与 CircuitBreakerDegradationStrategy 共用统一探测路径、端点维度与能力维度的健康视图分离）充分合理。

### 3. 语言特性可行性

**[通过]** 错误处理策略与 Java 异常机制匹配：`StructuredOutputNotSupportedException` 和 `LlmInfrastructureException` 继承 `RuntimeException`，`CompletableFuture` + `AiResult<T>` 模式将异常路径收敛为值类型返回，避免异常传播导致 HTTP 500。`doDegrade()` 中的防御性类型安全检查（`inputType.isInstance(request)`）正确处理了泛型擦除带来的 ClassCastException 风险。

**[通过]** 并发设计完全兼容 Java 17+ 并发模型：`CompletableFuture.supplyAsync()` / `orTimeout()`（Java 9+ 可用，目标平台 Java 17）覆盖异步执行和超时兜底。`ConcurrentHashMap` + `synchronized` 保护滑动窗口数据一致性。`AtomicReference` + CAS 实现熔断器状态转换。`ThreadPoolExecutor` + `CallerRunsPolicy` 提供 LLM 调用线程池的自然背压。

**[通过]** 资源管理方案可行：`@Scheduled` 清理任务使用 `ThreadPoolTaskScheduler(poolSize=3)` 避免 DDL 阻塞。指标采集使用独立的 `@Async` 线程池 + `DiscardPolicy` 隔离 LLM 主路径。`AiPlatformEnvironmentPostProcessor` 在 Spring 启动早期完成配置转发，生命周期与 Bean 容器分离。

**[通过]** 模块/包结构设计符合 Maven 多模块项目组织：`ai-api` / `ai-impl` 分离接口与实现，`ai-impl` 内部按功能子包（orchestrator/、client/、router/、template/、experiment/、metrics/、parser/、fallback/、degradation/、config/）划分职责，依赖方向清晰单向。

### 4. 设计一致性

**[通过]** 各抽象的职责描述清晰无歧义。每个核心抽象（§3.1–§3.9）均以"角色/职责/协作对象/类型形态选择理由"四元组格式展开，与 Phase0/Phase1ABD 的设计风格一致。

**[通过]** 协作关系形成闭环：`AiOrchestrator → CapabilityExecutor → (ModelRouter / LlmChatService / PromptTemplateManager / ExperimentManager / AiMetricsCollector / SlidingWindowMetricsStore / DegradationStrategy / LocalRuleFallback) → AiResult`，无缺失环节。降级路径从预检到兜底的完整链路在 §4.1 伪代码中完整覆盖。

**[通过]** 行为契约完整：§4.1–§4.7 共 7 个契约节点（handle/execute/doExecuteInternal/route/assign/render/parse/record），输入输出、异常处理路径、空值安全策略均有明确定义。§4.1 伪代码覆盖了 structuredChat 成功路径、StructuredOutputNotSupportedException 回退路径、LlmInfrastructureException 直接降级路径、超时降级路径、预检降级路径等全部分支。

**[通过]** 模块间依赖方向合理，无循环依赖：`ai-api ← ai-impl`，`ai-impl` 内部 `orchestrator → {router, template, experiment, client, fallback, degradation, metrics}`，各子包之间不互相依赖。`thin-adapter` 出向依赖 Phase 4 模块，方向明确。

**[通过]** 迭代要求（v10）中 4 个持续存在的问题均已正确修正：
  1. §3.7 PrescriptionLocalRuleFallback 的过敏史数据来源已修正为 `request.patientInfo.getAllergyInfo()`，方法签名已注明。
  2. §4.1 doExecuteInternal() 中 `parsedResult` 已在第一个 try 块前声明并初始化为 null，line 2935 已添加 null 守卫检查。
  3. §3.5 聚合 SQL 中的 PERCENTILE_CONT 已替换为注释说明的 ROW_NUMBER() + COUNT(*) 兼容方案。
  4. §3.9 AiPlatformConfig 中已补充 `@Bean("scheduledTaskExecutor")` ThreadPoolTaskScheduler(poolSize=3) 定义。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则：`AiOrchestrator` 仅做路由委托，`AbstractCapabilityExecutor` 封装共享模板方法，`CapabilityExecutor` 特化各能力管线，`DelegatingLlmChatService` 仅做分发，`SlidingWindowMetricsStore` 仅维护滑动窗口数据。配置属性拆分为 8 个独立 ConfigurationProperties 类而非单一大配置类。

**[通过]** 抽象层次恰当：`CapabilityExecutor<T, R>` 接口层定义管线契约，`AbstractCapabilityExecutor<T, R>` 骨架层提供公共模板方法，13 个具体 Executor 仅特化 `doExecuteInternal()`。降级策略（`DegradationStrategy`）通过 `getOrder()` default method 实现策略链排序，保持二进制兼容。

**[通过]** 设计便于详细设计和实现：§1.7 的实施拓扑顺序（7 批次）、§1.6 的 API Surface 状态表（✓/△/⊕）、§9.5 的完整 YAML 配置示例、§2.2 的 provided 作用域运行时风险说明、§3.1 的 Phase 4 模块异常契约逐一验证表——均为实现者提供了清晰的指导。

**[通过]** 设计便于单元测试：§11 覆盖了单元测试模式（@MockBean 模拟下游依赖）、集成测试模式（@SpringBootTest）、管线收敛验证（滑动窗口/防御性拷贝/异步上下文传播）、状态恢复路径验证（4 个状态机）、并发竞争验证（5 个竞态场景）。`RequestContextUtils` 设计为 static 工具类便于 MockHttpServletRequest 测试。

**[轻微]** `AbstractCapabilityExecutor` 构造器参数较多（14 个），但这是模板方法模式集中管理依赖的合理代价，且薄适配器的简化构造器已部分缓解此问题。可在实施阶段考虑引入 Builder 模式重构构造器以提升可读性。

**[轻微]** `doDegrade()` 方法的参数列表长达 14 个，有引入参数对象（如 `DegradeContext`）的改进空间。当前设计以显式参数传递保证了上下文更新的可追踪性，但增加了调用点的阅读负担。建议在实施阶段引入参数值对象封装上下文。

## 修改要求

本审查结果 APPROVED，无严重或一般问题。以上轻微改进建议可选择性采纳，不阻塞通过。
