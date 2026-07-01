# OOD 设计方案审查报告（v27）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 所有类型形态选择（interface / abstract class / enum / class）均与 Java 类型系统能力匹配：

- `AiService` / `CapabilityExecutor` / `LlmChatService` / `ModelRouter` 等接口契约使用 `interface`，支持多实现多态 ✓
- `AbstractCapabilityExecutor<T, R>` 使用 abstract class 封装公共模板方法，子类通过 `extends` 继承，符合 Java 单继承约束 ✓
- 13 个具体 `CapabilityExecutor` 均 extends `AbstractCapabilityExecutor`，类型层次清晰 ✓
- 泛型 `T` / `R` 使用受 Java 泛型系统完全支持，上下界约束符合类型系统能力 ✓
- `DelegatingLlmChatService` 实现 `LlmChatService` 接口 + 持有 `Map<ClientType, LlmChatService>` 委托映射 ✓
- `FallbackAiService` 作为 decorator 实现 `AiService`，同时标注 `@Primary` 和 `@ConditionalOnProperty`，类型与注解兼容 ✓
- 枚举类型（`ClientType`、`LlmChatMessageRole`、`DegradationReason`、`EndpointState`、`ExperimentStatus`、`TemplateStatus` 等）提供编译期类型安全 ✓

### 2. 标准库与生态覆盖

**[通过]** 设计所需的能力均在 Java / Spring Boot 生态覆盖范围内：

- Spring Framework：依赖注入（`@Component` / `@Autowired` / `@Qualifier`）、配置绑定（`@ConfigurationProperties`）、条件装配（`@ConditionalOnProperty` / `@ConditionalOnClass`）、事件机制（`ApplicationEvent` / `@EventListener`）、线程池（`ThreadPoolTaskExecutor` / `ThreadPoolTaskScheduler`）、`EnvironmentPostProcessor` ✓
- Spring Data JPA：`JpaRepository` / `@Entity` / `@OneToMany` / `@ManyToOne` / 分区索引 ✓
- Jackson：`ObjectMapper.convertValue()` / `@JsonCreator` / `@ConstructorProperties` / `@JsonIgnoreProperties` / `@JsonProperty` ✓
- Caffeine：本地缓存（`Cache<String, ...>` / `@Expiry` 接口），支持自定义过期策略 ✓
- CompletableFuture：`supplyAsync()` / `orTimeout()` / `exceptionally()` / `completeExceptionally()` ✓，注意 `.join()` 在 Spring Boot 3.x+ 中仍可用（非虚拟线程强制约束）
- Micrometer：`MeterRegistry` / `Gauge`，用于熔断器状态可观测性 ✓
- Guava：`RateLimiter` 令牌桶限流 ✓
- jtokkit：`cl100k_base` 编码用于精确 Token 计数 ✓
- Reactor：`Flux` 用于流式响应（`LlmChatStreamService`），`reactor-core` 为编译期可选依赖 ✓
- HikariCP：连接池管理，默认配置可满足底座数据库连接需求 ✓

### 3. 语言特性可行性

**[通过]** 错误处理、并发、资源管理模式均与 Java 语言特性匹配：

- 错误处理：`RuntimeException` 子类体系（`StructuredOutputNotSupportedException`、`LlmInfrastructureException`、`CredentialUnavailableException`、`Phase4BusinessException`），配合 `instanceof` 分类处理 ✓
- 并发模型：`CompletableFuture` 异步管线 + 专属线程池隔离（`llmCallExecutor` + `transcriptSummaryExecutor` + `metricsAsyncExecutor` + `scheduledTaskExecutor`）✓
- 线程安全：`ConcurrentHashMap` / `synchronized(deque)` / `AtomicReference` / `AtomicLong` / `volatile` 各司其职 ✓
- `volatile long elapsedInDoExecuteInternal` 用于跨线程可见性——`volatile` 保证 `doExecuteInternal()` 中对 `elapsedInDoExecuteInternal` 的写入对 `exceptionally()` 回调线程立即可见 ✓
- 资源管理：`try-catch` 包裹防御性拷贝和 LLM 调用；HikariCP 连接池由 Spring Boot 自动管理 ✓
- 模块/包结构：清晰的两模块分层（`ai-api` / `ai-impl`），`ai-impl` 内部按功能域划分子包，无循环依赖 ✓
- `@Scheduled` 定时任务通过 `scheduledTaskExecutor` 线程池隔离（poolSize=3），避免 Spring 默认单线程调度器阻塞 ✓
- `provided` 作用域的 Phase 4 模块依赖在运行时风险已在 §2.2 显式记录，并给出 uber-JAR 部署的适配方案 ✓
- CredentialProvider Vault 不可达降级的状态机设计（NORMAL → CACHE_ONLY → BACKOFF）合理，与 `ModelEndpointHealthManager` 一致 ✓

### 4. 设计一致性

**[通过]** 各抽象职责清晰，协作关系闭环，行为契约完整：

- `AiService` → `FallbackAiService` → `AiOrchestrator` → `CapabilityExecutor` → `AbstractCapabilityExecutor` 的委托链形成完整闭环 ✓
- `execute()` 模板方法（final）确保降级预检不可绕过，`doExecuteInternal()` 由子类特化 ✓
- `executeStandardPipeline()` 解决 v26 中 `super.doExecuteInternal()` 调用父类 abstract 方法的结构性缺陷 ✓
- 三个辅助方法（`preciseTokenCount` / `formatTranscripts` / `truncateTranscripts`）已在 §3.11.7 正式定义 ✓
- `extractVariables()` 职责归属明确（§3.11.7 vs §4.1），不再承担 LLM 调用职责 ✓
- `userId` 与 `callerId` 语义冗余问题已在 §3.1 详细说明，值相同但语义不同 ✓
- 防御性拷贝失败在 `execute()` 入口处增加 try-catch 回退，捕获异常后使用原始 request ✓
- 字符估算回退分支中硬编码跳跃值（2000）已替换为阈值提升方案（3000→4000）+ WARN 日志跟踪 ✓
- 模块依赖方向：`ai-api` ← `ai-impl`，`ai-impl` 内部 `orchestrator` → 其余子包，单向依赖 ✓
- DiscussionConclusionCapabilityExecutor 的前置压缩调用线程隔离方案已定稿 ✓

### 5. 设计质量

**[通过]** 职责划分合理，抽象层次恰当，便于测试和实现：

- 单一职责：`AiOrchestrator` 仅做路由，`AbstractCapabilityExecutor` 封装公共模板，各 `XxxCapabilityExecutor` 特化单一能力 ✓
- 适度设计：`CallContext` 值对象三期迁移计划使 15+ 参数的方法降维，渐进式重构而非一次性全量修改 ✓
- 可测试性：所有核心抽象为 interface 或 abstract class，可通过 Mockito 构造测试桩；`RequestContextUtils` 为 static 工具类，通过 `MockHttpServletRequest` 可独立测试 ✓
- 非功能性分析：§1.8 覆盖冷启动惊群效应、连接池压力、内存占用、启动延迟，分析完整且结论合理 ✓
- 热加载设计：degradation.strategies 通过 `AtomicReference` 全量替换 + @Scheduled 轮询 / ApplicationEvent 双机制，尊重 @RefreshScope 对 Map 字段的局限性 ✓
- 审计与可观测性：`AiCallRecord` / `AiCallLogEntity` / `AiCallLogStats` 完整覆盖调用链路指标；熔断器状态通过 Micrometer Gauge + JMX + 日志三重可观测 ✓
- 向后兼容：`DegradationContext` 无参构造器保留、`@Deprecated` 旧工厂方法共存、`@JsonIgnoreProperties(ignoreUnknown=true)` 保障序列化兼容 ✓

## 结论

本设计方案在 Java / Spring Boot 生态中实现可行性充分。前次审查（v26）发现的 8 个问题（含 2 个严重、3 个重要、3 个一般）在 v27 中已全部修复，且无新的严重或一般问题。设计方案**通过类型系统可行性、标准库覆盖、语言特性可行性、设计一致性和设计质量**五个维度的审查。
