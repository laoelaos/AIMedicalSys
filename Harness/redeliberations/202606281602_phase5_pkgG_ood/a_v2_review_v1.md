# OOD 设计方案审查报告（v2）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 所有类型形态选择（interface、abstract class、enum、泛型 interface/class、static inner class、JPA @Entity）均与 Java 类型系统能力完全匹配。具体验证如下：
- `CapabilityExecutor<T, R>` 泛型 interface + `AbstractCapabilityExecutor<T, R>` 抽象骨架类 + 13 个具体子类 extends：遵循 Java 单继承规则，层次合理
- `DegradationStrategy` 新增 `default int getOrder() { return 0; }`：Java 8+ default method 机制完全支持，二进制兼容性已确认——现有 `DegradationStrategy` 接口仅含 `shouldDegrade()` 一个方法，无其他默认方法冲突风险
- `LlmChatService` / `LlmChatStreamService` 双 interface 分离：`LlmChatStreamService` 方法签名直接引用 `Flux<LlmChatResponse>`——需 reactor-core 编译期依赖，设计已通过 `optional` 依赖 + `@ConditionalOnClass` 保护明确隔离
- `enum ClientType`、`enum LlmChatMessageRole`、`enum CredentialProviderState` 提供编译期类型安全
- `StructuredChatResult<T>` 泛型值对象包裹解析后 DTO + 元数据：类型安全

**[通过]** 抽象之间的继承和实现关系均在约束范围内——`CapabilityExecutor` interface → `AbstractCapabilityExecutor` abstract class (implements) → 13 个具体子类 (extends)，继承树深度为 2，遵守单继承约束。`AiService` interface 被 `AiOrchestrator`、`FallbackAiService`、`MockAiService` 三个 class 同时实现，支持 Java 多接口实现特性

**[通过]** 泛型使用方式（`CapabilityExecutor<T, R>` 中 T/R 作为方法参数和返回值类型、`Class<T>` 作为运行时类型令牌传递给 `structuredChat()`）均在 Java 泛型系统能力范围内。由于泛型擦除，`LocalRuleFallback.fallback(request)` 在编译期为 unchecked 转换——设计已在 §7（决策 2706）和 §4.1 `doDegrade()` 伪代码中记录此风险并增加了 `Class<T> inputType` + `isInstance()` 运行时类型检查防御措施

**[通过]** 协作关系中描述的类型交互模式（interface 依赖注入、CompletableFuture 异步返回、泛型方法调用）均可在 Java/Spring Boot 中实现

### 2. 标准库与生态覆盖

**[通过]** 设计中需要的能力均在 Spring Boot + Java 标准生态覆盖范围内：
- **集合操作**：Java Collections Framework + Guava `RateLimiter`（令牌桶）
- **IO/HTTP**：OkHttp 或 Spring WebClient（设计明确指出二选一）
- **并发**：`CompletableFuture`、`ExecutorService`、`ConcurrentHashMap`、`AtomicReference`、`synchronized`——均为 Java 标准库
- **持久化**：Spring Data JPA + MySQL（分区策略 `RANGE COLUMNS`）
- **序列化**：Jackson（`ObjectMapper.convertValue()`、`@JsonIgnoreProperties`、`@JsonCreator`）
- **缓存**：Caffeine（`Expiry` 接口支持动态 TTL）
- **指标**：Spring Boot Actuator + Micrometer（`Timer`、`Counter`、`DistributionSummary`）
- **可选 LLM 框架**：Spring AI（`@ConditionalOnClass` 保护可选依赖）
- **事件驱动**：Spring `ApplicationEvent` + `@EventListener`
- **配置绑定**：`@ConfigurationProperties` + `EnvironmentPostProcessor`

**[通过]** 设计中对库能力的假设均合理——Caffeine 的 `Expiry` 接口在 caffeine 3.x 中存在，Guava `RateLimiter` 是成熟实现，Micrometer 与 Spring Boot Actuator 深度集成。无不可实现的假设

**[通过]** 标准库能力可简化某些自定义抽象——当前设计中的自定义抽象均已做出合理权衡（如 `LlmChatService` 自建接口层而非直接使用 Spring AI，是为了同时支持 HTTP API 和 Spring AI 两种模式，非过度设计）

### 3. 语言特性可行性

**[通过]** 错误处理策略与 Java 异常体系匹配：
- 两层异常分类——`StructuredOutputNotSupportedException`（回退重试）和 `LlmInfrastructureException`（直接降级）均 extends `RuntimeException`，支持 catch 分支区分
- `CompletableFuture` 非异常路径（降级路径通过 `completedFuture(degradedResult)` 完成）和异常路径（不可预知异常包装为 `CompletionException`）分离
- `AiResult.failure(errorCode, errorMessage)` 标准化错误码替代裸字符串——设计新增 `DegradationReason` 枚举消除字符串字面量分散问题

**[通过]** 并发设计模型与 Java 并发模型兼容：
- `CompletableFuture.supplyAsync()` + 自定义 `ThreadPoolExecutor`（`llmCallExecutor`）——标准异步边界模式
- `SlidingWindowMetricsStore` 内部 `ConcurrentHashMap<String, Deque>` + `synchronized` 块保护写入和惰性淘汰——Java 标准并发原语
- `CircuitBreakerDegradationStrategy` 使用 `AtomicReference` + CAS——标准无锁并发模式
- `ModelRouter` 使用 `AtomicReference<Map>` 全量替换——免锁读取路径
- `@Async("metricsAsyncExecutor")` + `DiscardPolicy`——Spring 线程池隔离

**[通过]** 资源管理方案可行：
- 线程池配置（核心线程数、队列容量、拒绝策略）均已定义
- 缓存 TTL（Caffeine expireAfterWrite + Expiry 接口）和最大容量均明确定义
- Credential 缓存使用 Caffeine `Expiry` 接口实现 CACHE_ONLY 状态下 TTL 延长——可行
- JPA 连接池由 Spring Boot 管理，无需在设计中定义

**[通过]** 模块/包结构符合 Maven 单体项目组织方式：
- `ai-api`（interface + DTO 模块，零外部依赖）← `ai-impl`（实现模块，内部子包按职责划分）
- 包内子包划分（orchestrator/、client/、router/、template/、experiment/、metrics/、parser/、fallback/、degradation/、config/）清晰，职责内聚
- `orchestrator/impl/` 为 13 个 CapabilityExecutor 实现提供独立子包，避免与编排器主类混放

### 4. 设计一致性

**[通过]** 各抽象的职责描述清晰无歧义——每个抽象在 §3 核心抽象中均有"职责"段落和"协作对象"段落，职责定位明确。§1.3 核心抽象一览表提供快速总览

**[通过]** 协作关系形成完整闭环：
- `AiOrchestrator` → `Map<String, CapabilityExecutor>` → `execute()` 内调用 `ModelRouter`、`LlmChatService`、`PromptTemplateManager`、`ExperimentManager`、`StructuredOutputParser`、`AiMetricsCollector`、`SlidingWindowMetricsStore`、`DegradationStrategy`、`LocalRuleFallback`
- 模型调用路径：`CapabilityExecutor` → `ModelRouter.route()` → `DelegatingLlmChatService.chat()` / `structuredChat()` → `EndpointRateLimiter.tryAcquire()` → `CredentialProvider.getCredential()` → `HttpApiLlmChatService` / `SpringAiLlmChatService`
- 降级路径：`SlidingWindowMetricsStore.buildDegradationContext()` → `DegradationStrategy.shouldDegrade()` → `doDegrade()` → `LocalRuleFallback.fallback()` / `AiResult.degraded()`
- 以上路径均形成清晰闭环，无缺失环节

**[通过]** 行为契约描述完整，足以指导后续实现——§4.1 ~ §4.7 提供了各组件关键交互的伪代码，包括异常分支、回退路径、指标采集调用点等

**[通过]** 模块间依赖方向合理，无循环依赖：
- `ai-api` ← `ai-impl`（单向，无反向依赖）
- `ai-impl` 内部：`orchestrator/` 依赖其他子包，其他子包间互不依赖（通过 interface 解耦）
- `ai-impl` → Phase 4 modules（出向依赖，通过 `provided` 作用域声明）
- 依赖规则在 §2.2 中明确记录

**[轻微]** `doDegrade()` 方法签名包含 16 个参数（startTime, degradeReason, request, capabilityId, departmentId, callerRole, callerId, visitId, patientId, sessionId, inputSummary, outputSummary, promptVersion），调用点冗长且易参数错位。虽然设计级别可接受此抽象形态，但实现时强烈建议提取为上下文值对象或 Builder 模式，降低认知负担和传参错误风险。当前设计中的 `DegradationContext` 类可扩展为此用途。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则：
- `AiOrchestrator`：仅负责路由委托（查找 executor → 调用），不介入管线内部步骤
- `CapabilityExecutor`：持有完整管线，每能力独立实现——单一能力职责
- `AbstractCapabilityExecutor`：封装公共模板方法（降级预检、指标采集）——公共职责抽取
- `ModelRouter`：仅负责模型路由决策
- `LlmChatService`：仅负责 LLM 对话接口抽象
- `AiMetricsCollector`：仅负责性能指标采集
- 各组件职责不重叠

**[通过]** 抽象层次恰当：
- 不过度设计：`AiOrchestrator` 使用 class 而非 interface（单一实现，无多态需求）；`FallbackAiService` 保留为装饰器模式
- 不设计不足：`CapabilityExecutor` 引入泛型 interface + abstract class 模板方法模式，13 个实现共享骨架；`DelegatingLlmChatService` 分发层消除双实现 Bean 装配二义性

**[通过]** 设计便于后续的详细设计和实现：
- 13 个 CapabilityExecutor 实现类通过 extends `AbstractCapabilityExecutor` 即可完成——只需特化 `doExecuteInternal()`、可选重写变量提取方法
- 新增 AI 能力只需新增一个 `CapabilityExecutor` 实现（`@Component`）即可自动注册到 `AiOrchestrator` 映射表
- 新增 LLM 客户端类型只需新增 `LlmChatService` 实现 + 在 `DelegatingLlmChatService` 的 `Map<ClientType, LlmChatService>` 中注册

**[通过]** 设计便于单元测试：
- `CapabilityExecutor` 通过构造器注入所有依赖——可 mock 所有下游组件验证降级路径
- `SlidingWindowMetricsStore`、`ModelRouter`、`LlmChatService` 等均为 interface 或 class 可 mock
- §11 测试策略提供了明确的测试模式（单元测试、集成测试、收敛验证）
- `RequestContextUtils` 为 `public static` 工具类，可直接注入 `MockHttpServletRequest` 测试

**[一般]** 降级预检循环中的排序时机未在设计中明确：§4.1 伪代码第 2254 行 `for each strategy in this.degradationStrategies (sorted by getOrder() asc)` 暗示每次 `execute()` 调用时排序。在 13 个 CapabilityExecutor × 高并发场景下，每次调用排序将产生不必要的 O(n log n) 开销（n = 策略数量，通常 2~3 个，实际影响有限但属设计规范性问题）。建议在 `AiPlatformConfig` 构建 `Map<String, List<DegradationStrategy>>` 时一次性完成预排序，或在 `AbstractCapabilityExecutor` 构造器/`@PostConstruct` 中执行一次排序。此问题不影响设计可行性，但应在进入实现前明确。

## 修改要求（REJECTED 时存在）

（不适用 — 审查结果为 APPROVED）
