# OOD 设计方案审查报告（v4）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]**
- 设计中所有类型形态选择（interface / class / abstract class / JPA @Entity）均与 Java 类型系统能力匹配
- 继承与实现关系遵循 Java 单继承 + 多接口实现约束（`AiRequestBase` 为 abstract class，其余契约均使用 interface）
- 泛型使用方式（`CapabilityExecutor<T, R>`、`StructuredOutputParser.parse(LlmResponse, Class<T>)`）均在 Java 泛型系统能力范围内，类型擦除场景通过显式 `Class<T>` 参数解决
- `CompletableFuture<AiResult<R>>` 异步返回模式为标准 Java 8+ 模式

### 2. 标准库与生态覆盖

**[通过]**
- 集合（Map/List/Deque）、并发（ConcurrentHashMap/AtomicLong/AtomicReference/CompletableFuture）均为标准库覆盖
- Spring Boot 生态覆盖全部框架能力（`@ConditionalOnProperty`、`@Primary` + `ObjectProvider`、`@PostConstruct`、`@Qualifier`、`EnvironmentPostProcessor`、`@Async` + 自定义线程池、`@Component`、JPA Repository）
- Micrometer 指标体系通过 `spring-boot-starter-actuator` 显式依赖声明覆盖
- Jackson 序列化/反序列化通过 Spring Boot 自动配置覆盖
- 设计中的库能力假设（如 Spring AI ChatModel、HTTP 客户端连接池）均合理且在常见使用范围内

### 3. 语言特性可行性

**[通过]**
- 错误处理策略合理：业务异常通过降级路径以 `AiResult.degraded()` 完成，不抛出业务异常；启动期配置缺失通过 fail-fast 异常暴露（`IllegalStateException`）
- 并发设计成熟：编排层无大锁，各子组件独立承担并发安全（`ConcurrentHashMap` + CAS + 读写分离快照）；异步指标采集使用 `CallerRunsPolicy` 避免 TaskRejectedException 传播
- 资源管理合理：HTTP 连接池由 LlmClient 实现管理；数据库连接由 Spring Data JPA 管理；无资源泄漏风险
- 模块/包结构符合 Java/Spring Boot 项目组织惯例（`ai-api` / `ai-impl` 分层，内部按职责分子包）

### 4. 设计一致性

**[通过]**
- 各抽象职责描述清晰：AiOrchestrator 为路由层，CapabilityExecutor 持有完整管线，其余组件各司其职
- 协作关系形成闭环：AiOrchestrator → CapabilityExecutor → {ModelRouter, LlmClient, PromptTemplateManager, ExperimentManager, StructuredOutputParser, AiMetricsCollector, SlidingWindowMetricsStore, DegradationStrategy, LocalRuleFallback, ModelEndpointHealthManager}，所有依赖在图和文本中均有体现
- 行为契约完整：§4.1-§4.6 提供了管线、路由、实验、模板渲染、解析、指标采集的详细伪代码，足以指导后续实现
- 模块依赖方向合理：`ai-api ← ai-impl`，`ai-impl` 内部 `orchestrator → 其余子包`，其余子包间不互相依赖，无循环依赖
- **v4/v5 修订已解决此前全部问题**：能力覆盖补全（13/13）、管线所有权统一、降级策略注入路径、elapsedMs 定义、Null ModelRoute 检查、映射机制定义、AiRequestBase 影响评估、applyStrategies() 移除、变量提取约定、配置转发机制

**[轻微]** 改进建议 — `AiOrchestrator` 内部持有 `SlidingWindowMetricsStore` 和 `ModelEndpointHealthManager`（§3.1 协作对象列表），但 AiOrchestrator 的行为描述中从未使用 `ModelEndpointHealthManager`；实际使用方为 `CapabilityExecutor`（§4.1 伪代码）。可考虑将 `ModelEndpointHealthManager` 从 AiOrchestrator 的字段列表中移除，或说明其在 AiOrchestrator 中的用途。

**[轻微]** 改进建议 — §3.1 CapabilityExecutor 文字描述"内部使用注入的 ..."中列举的依赖不完整，缺失 `ExperimentManager` 和 `DegradationStrategy`（类图和伪代码中均已包含二者）。建议补全描述以保持一致性。

### 5. 设计质量

**[通过]**
- 职责划分遵循 SRP：编排路由（AiOrchestrator）、管线执行（CapabilityExecutor）、模型路由（ModelRouter）、LLM 调用（LlmClient）、模板管理（PromptTemplateManager）、实验管理（ExperimentManager）、指标采集（AiMetricsCollector）、降级判定（DegradationStrategy）、滑动窗口存储（SlidingWindowMetricsStore）、端点健康管理（ModelEndpointHealthManager），各有清晰边界
- 抽象层次恰当：架构级设计，不包含实现细节（如具体字段、方法体），但方法签名、协作关系、状态模型、伪代码契约充足
- 便于后续实现：接口清晰、包结构完整、依赖方向明确、关键伪代码可直译为实现
- 便于测试：接口化设计可 mock、DI 注入可隔离、每个 CapabilityExecutor 独立可测、SlidingWindowMetricsStore 和 DegradationStrategy 均可独立单元测试

## 修改要求（REJECTED 时存在）

N/A — 已 APPROVED

---

**审查结论**：无严重或一般问题，存在 2 项轻微改进建议，不阻塞通过。
