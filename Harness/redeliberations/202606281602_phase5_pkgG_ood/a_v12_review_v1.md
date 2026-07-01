# OOD 设计方案审查报告（v12）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 设计中的类型形态选择（interface / abstract class / class / enum / JPA @Entity / sealed-like enum 模式）全部与 Java 类型系统能力匹配。关键类型关系包括：

- `CapabilityExecutor<T,R>` 泛型接口 → `AbstractCapabilityExecutor<T,R>` 抽象类实现 → 13 个具体子类 extends，符合 Java 单继承 + 多接口实现约束
- `AiService` 接口被 `AiOrchestrator`、`FallbackAiService`、`MockAiService` 三个类实现，符合多态需求
- 枚举 `ClientType` / `AuthType` / `DegradationReason` / `LlmChatMessageRole` / `ExperimentStatus` / `TemplateStatus` 等提供编译期类型安全
- 泛型使用方式（`CapabilityExecutor<T,R>`、`StructuredChatResult<T>`、`LocalRuleFallback<T,R>`）在 Java 泛型系统能力范围内
- JPA 注解（`@Entity`、`@OneToMany`、`@ManyToOne`）使用正确，关联关系映射符合 JPA 规范
- 线程安全模型（`ConcurrentHashMap`、`AtomicReference`、`synchronized` 块、CAS）均为 Java 标准并发原语，实现可行

### 2. 标准库与生态覆盖

**[通过]** 设计中引用的标准库和第三方库均在 Java/Spring 生态覆盖范围内：

- Spring Boot（DI、`@ConditionalOnProperty`、`@Async`、`@EventListener`、`EnvironmentPostProcessor`）— 项目已有依赖
- Spring Data JPA（`JpaRepository`、`@Entity` 映射）— 项目已有依赖
- Spring Security（`SecurityContextHolder`）— 项目已有依赖
- Jackson（`ObjectMapper`、`@JsonCreator`、`@JsonProperty`）— Spring Boot 默认引入
- Caffeine（内存缓存，`Cache<String, Boolean>`、`Expiry` 接口）— 标准配置
- Guava（`RateLimiter` 令牌桶）— 标准配置
- Micrometer（Timer、Counter、Gauge）— Spring Boot Actuator 提供
- Reactor（`Flux`）— 仅 `LlmChatStreamService` 编译期依赖，声明为 optional
- OkHttp / Spring AI — 均为可选依赖，设计已处理可选性
- CompletableFuture / ForkJoinPool / ThreadPoolExecutor — Java 标准库

所有假设的库能力（如 Caffeine 的 Expiry 接口、Guava RateLimiter 的线程安全）均合理且已验证。

### 3. 语言特性可行性

**[通过]** 关键语言特性使用方案在 Java 中均可行：

- **错误处理**：`AiResult<T>` 模式（success / failure / degraded）替代异常路径传递业务结果；`RuntimeException` 子类（`StructuredOutputNotSupportedException`、`LlmInfrastructureException`）用于控制流；catch 块设计合理（CompletionException 拆解模式处理 `orTimeout().join()` 的异常包装）。`AiOrchestrator.handle()` catch 块兜底捕获同步异常。
- **并发设计**：`CompletableFuture.supplyAsync()` + 自定义线程池（`LlmCallExecutor`）隔离容器线程与 LLM 调用线程；入口处 ThreadLocal 提取 + 闭包捕获传播模式解决异步上下文丢失；`SlidingWindowMetricsStore` 的 `synchronized(deque)` 锁协议 + 惰性淘汰策略在 Java 中可行；`CircuitBreakerDegradationStrategy` 的 `AtomicReference` + CAS 状态转换符合 Java 并发最佳实践。
- **资源管理**：线程池（`ThreadPoolTaskExecutor`、`ThreadPoolTaskScheduler`）通过 Spring 生命周期管理；滑动窗口事件队列有 `max-events-per-capability` 上限防止内存泄漏；数据库分区 `DROP PARTITION` 在低峰期执行。
- **模块结构**：`ai-api` / `ai-impl` 分层清晰，子包（`orchestrator/`、`client/`、`router/`、`template/`、`experiment/`、`metrics/` 等）职责边界明确。

### 4. 设计一致性

**[通过]** 各维度内部一致性良好：

- 各抽象职责描述清晰无歧义，所有核心抽象均使用"角色/职责/协作对象/类型形态选择理由"四元组描述
- 协作关系形成完整闭环：业务模块 → `AiService` → `FallbackAiService` → `AiOrchestrator` → `CapabilityExecutor` → `ModelRouter` / `LlmChatService` / `PromptTemplateManager` / `ExperimentManager` / `AiMetricsCollector` / `SlidingWindowMetricsStore` → 外部 LLM 服务 / JPA 数据库，无缺失环节
- 行为契约（§4.1–§4.7 伪代码）完整到足以指导实现
- 模块间依赖方向合理，无循环依赖：`ai-api ← ai-impl`，`ai-impl` 内部各子包单向依赖 `orchestrator → {router, template, experiment, client, fallback, degradation, metrics}`
- **迭代问题验证**：v12 已全部修复 v11 识别的 8 个问题（callerRole 一致性 → §3.10 RequestContextUtils 统一实现；PatientInfo 未定义 → §3.11.2 补充字段表；TimeoutException 死代码 → §4.1 拆解 CompletionException 模式；ExperimentGroup/AiCallLogStats 类图缺失 → §2.3 补充节点；parse() 超时未体现 → §4.1 包裹 `supplyAsync().get(5s)`；患者数据建模不一致 → §3.11.4 统一为 PatientInfo；ObjectMapper 来源未定义 → §3.1 构造器补充参数）
- 类图（§2.3）与正文契约（§3）已对齐：`LlmChatOptions` 补充了 `topP`/`frequencyPenalty`/`presencePenalty`；`ModelRoute` 补充了 `authType` 及 `AuthType` 关联；`LlmChatRequest` 补充了 `tools` 字段；`AbstractCapabilityExecutor` 补充了 `thinAdapterPerCapabilityConfig` 字段；`doDegrade` 方法签名末尾包含 `modelId` 参数

### 5. 设计质量

**[通过]** 设计整体质量较高，职责划分合理：

- **单一职责原则**：各组件职责内聚——`AiOrchestrator` 仅做路由委托，`AbstractCapabilityExecutor` 封装通用降级预检和指标采集，`SlidingWindowMetricsStore` 专注调用指标存储，`ModelEndpointHealthManager` 专注端点健康状态，`RequestContextUtils` 专注 HTTP Header 提取
- **抽象层次恰当**：未过度设计（如未为 13 个几乎相同的管线创建额外的抽象层），也未设计不足（AbstractCapabilityExecutor 模板方法模式提供足够的代码复用同时允许子类特化）
- **便于后续实现**：§1.7 实施拓扑顺序（7 批次）明确各组件开发顺序和依赖前提；§1.6 API Surface 状态表标注每个 API 的创建状态；构造器签名的详细伪代码可直接转换为 Java 代码
- **便于单元测试**：§11 测试策略详细覆盖了单元测试（@MockBean 模拟下游依赖验证各降级路径触发条件）、集成测试（@SpringBootTest 验证完整管线）、状态恢复路径验证（反射操作状态字段预置特定值）、并发竞争验证（CountDownLatch + CyclicBarrier）四个层次
- **设计决策记录完整**：§7 设计决策表覆盖 30+ 项关键决策，每项含"决策/选项/选择/理由"四列，便于后续实现者理解设计意图
