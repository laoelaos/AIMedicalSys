# OOD 设计方案审查报告（v21）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 设计的类型形态选择（interface / abstract class / class / enum / JPA @Entity）与 Java 类型系统完全匹配。单继承+多接口实现的继承体系正确（如 `AbstractCapabilityExecutor` 抽象基类 + 13 个子类 extends + `CapabilityExecutor` interface implements）。泛型使用合理：`CapabilityExecutor<T,R>` 类型安全地封装 13 项能力的异构输入输出；`StructuredChatResult<T>` 携带调用元数据。枚举类型（`ClientType`、`LlmChatMessageRole`、`DegradationReason`、`CircuitBreakerState`）提供编译期类型安全替代字符串字面量。

**[轻微]** `ChangeType`（`CREATED / UPDATED / DELETED / STATUS_CHANGED`）在三个 Event 类中引用但未作为独立类型声明。建议在 §1.3 核心抽象表或 §3.2/§3.3/§3.4 中统一定义。

### 2. 标准库与生态覆盖

**[通过]** 设计依赖的库能力均在 Java / Spring Boot 生态覆盖范围内：Spring IoC（`@Component`、`@Bean`、`@ConditionalOnProperty`、`@Primary`、`@Qualifier`、`@Value`、`@Async`）、Spring Data JPA（`@Entity`、`Repository`）、Spring MVC（`RequestContextHolder`）、Jackson（`ObjectMapper`、`@JsonCreator`）、Caffeine 缓存、Guava `RateLimiter`、Micrometer、Reactor（`Flux`）、`CompletableFuture` / `ConcurrentHashMap` / `AtomicReference` 等标准库组件。依赖假设合理。

**[轻微]** `RateLimiter` 实现依赖 Guava — 若项目当前未引入 Guava，需确认 Guava 作为间接依赖或显式声明。此不阻塞审批。

### 3. 语言特性可行性

**[通过]** 错误处理策略与 Java 异常体系匹配：设计将异常按语义分类（`StructuredOutputNotSupportedException` 回退 vs `LlmInfrastructureException` 直接降级），通过独立 catch 分支区分处理，消除 v20 统一 `catch(Exception)` 的粒度过粗问题。异步上下文传播采用"入口处提取 + 闭包捕获"模式解决 `SecurityContextHolder` / `RequestContextHolder` 在线程池中丢失的问题，是 Spring 生态中的标准实践。`CompletableFuture.orTimeout()` 整体超时兜底 + `.exceptionally()` 降级回掉模式可行。`EnvironmentPostProcessor` 早期配置转发 + `@PostConstruct` 延迟策略装配的时序协调合理。

**[通过]** 并发设计符合 Java 并发模型：`SlidingWindowMetricsStore` 使用 `ConcurrentHashMap` + 写锁策略；`CircuitBreakerDegradationStrategy` 使用 `AtomicReference<CircuitState>` + CAS；`ModelRouter` 使用 `AtomicReference<Map>` 全量替换；专用指标采集线程池隔离避免 LLM 调用路径被阻塞。`CallerRunsPolicy`（LLM 调用线程池）与 `DiscardPolicy` + WARN（指标线程池）的选择符合各自场景需求。

**[通过]** 资源管理方案可行：`EndpointRateLimiter` 令牌桶限流在 `LlmChatService` 入口处执行；`CredentialProvider` Caffeine 缓存 TTL 5 分钟 + Vault 不可达降级策略合理。

**[轻微]** `RequestContextUtils.extractFromRequestContext()` 依赖 `ServletRequestAttributes`，在某些非标准 Servlet 容器或 `WebApplication` 类型下行为可能不同。非 HTTP 场景的 DTO 回退路径已定义，可缓解此风险。

### 4. 设计一致性

**[通过]** 各抽象职责描述清晰无歧义。6 个 v20 审查问题均已修复：
- `DelegatingLlmChatService` 分发层消除了双实现 Bean 装配二义性
- `structuredChat()` 异常捕获拆分为 `StructuredOutputNotSupportedException`（回退）和 `LlmInfrastructureException`（直接降级）
- `RouteConfigChangedEvent` Payload 已定义（`endpointId/changeType/changedAt`）
- `CircuitBreakerDegradationStrategy.getState()` + Micrometer Gauge + JMX 补齐可观测性
- 非 HTTP 场景上下文提取路径（DTO 继承 / MQ 调用方填充 / 薄适配器回退 null）三级保障定义完整
- `RequestContextUtils` 提取为 `public static` 工具类消除跨类访问编译错误

**[通过]** 协作关系形成闭环：`AiOrchestrator` → `CapabilityExecutor` → [`ModelRouter`, `LlmChatService`, `PromptTemplateManager`, `ExperimentManager`, `DegradationStrategy`, `SlidingWindowMetricsStore`, `StructuredOutputParser`] 管线完整无缺失。`ModelRouter` → `ModelEndpointHealthManager` → `LlmChatService` 的端点健康检查 + 熔断器交互优先级清晰。依赖方向合理，无循环依赖。

**[轻微]** `DelegatingLlmChatService` 类图中仍显示到 `HttpApiLlmChatService` 和 `SpringAiLlmChatService` 的 `dispatches` 关联，但实际调度基于 `Map<ClientType, LlmChatService>` 而非直接引用单个实例。关联关系可更新为 `o--> Map<ClientType, LlmChatService>` 或保留当前简化表示。

### 5. 设计质量

**[通过]** 职责划分遵循 SRP：`AiOrchestrator` 仅负责路由委托；`AbstractCapabilityExecutor` 封装降级预检+指标采集公共模板方法；`DelegatingLlmChatService` 按 ClientType 分发；各 Value Object 职责内聚。抽象层次恰当：`AbstractCapabilityExecutor` 模板方法模式避免了 13 个实现类的代码重复，同时通过 `doExecuteInternal()` 抽象方法允许子类特化差异化步骤。

**[通过]** 设计便于测试：§11 定义了完整测试策略（单元测试 `@MockBean` 模拟下游依赖验证各降级路径、集成测试验证 Bean 装配互斥与配置转发、管线收敛验证涵盖滑动窗口三分类/防御性拷贝/异步上下文传播）。组件可 mock、可隔离。

**[通过]** `CircuitBreakerDegradationStrategy` 新增 `getState()` 接口 + Micrometer Gauge + JMX 三种可观测性暴露方式，提升运维深度。

## 修改要求（REJECTED 时存在）

（无 — 已 APPROVED）
