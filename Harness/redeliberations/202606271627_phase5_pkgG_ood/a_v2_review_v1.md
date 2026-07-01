# OOD 设计方案审查报告（v1）

## 审查结果

REJECTED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 全部抽象的类型形态选择（interface / class / JPA @Entity / 值对象）与 Java 类型系统能力完全匹配：

- `CapabilityExecutor<T, R>` 的泛型使用方式正确，`Class<T> getInputType()` 模式是 Java 泛型擦除下的标准运行时类型保留方案
- 单继承 + 多接口实现（`AiService <|.. AiOrchestrator : implements`）在 Java 约束范围内
- 枚举（`TemplateStatus`、`ExperimentStatus`、`ClientType`）在 Java 中是原生支持的类型形态
- 协作关系描述的 interface 依赖注入模式可在 Spring 中完全实现
- `AiResult<R>` 泛型返回值在 `AiService` 各方法与 `CapabilityExecutor.execute()` 之间的类型传递形成完整类型安全闭环

**[轻微]** `DegradationContext` 提及 `serialVersionUID` 暗示实现 `Serializable`，但设计中未显式声明。建议在定义中明确 `implements Serializable`。

### 2. 标准库与生态覆盖

**[通过]** 设计依赖的库能力均在 Spring Boot / JDK 标准生态覆盖范围内：

- JPA / Hibernate — Spring Data JPA 标准覆盖
- Jackson 反序列化 — `spring-boot-starter-web` 传递依赖覆盖
- Micrometer 指标体系 — `spring-boot-starter-actuator` 已显式声明（§8）
- 并发工具（`ConcurrentHashMap`、`AtomicReference`、`Deque`） — JDK 标准库
- `ObjectProvider`、`@ConditionalOnProperty`、`@Primary`、`@Async` — Spring Framework 原生支持
- HTTP 客户端连接池 — Spring 的 `RestTemplate` 或 `WebClient` 均支持
- Spring AI `ChatModel` — 作为可选实现，生态中存在

### 3. 语言特性可行性

**[通过]** 错误处理、并发设计、资源管理在 Java / Spring 中均可行：

- **错误处理**：以 `AiResult.degraded()` 返回值模式替代异常传播，避免异常在异步管线中丢失上下文，与 Java checked/unchecked exception 体系兼容；LLM 层重试一次后降级的策略与 Spring 的 `@Retryable` 或手动重试均兼容
- **并发设计**：
  - `ConcurrentHashMap` + `synchronized` 队列写 + 快照读的滑动窗口方案在 Java 中标准实现
  - `CircuitBreakerDegradationStrategy` 的 `AtomicReference<CircuitState>` + CAS 状态转换正确利用 Java `java.util.concurrent.atomic` 包
  - `@Async` + 自定义线程池 + `CallerRunsPolicy` 是 Spring 标准异步配置模式
- **资源管理**：
  - 数据库连接由 HikariCP（Spring Boot 默认连接池）管理
  - HTTP 连接由底层客户端库管理
  - Spring Bean 生命周期由 Spring 容器管理
- **模块/包结构**：`ai-api` / `ai-impl` 双模块，内部子包按单向依赖组织，无循环依赖，符合 Maven 多模块项目惯例

### 4. 设计一致性

**[通过]** 各抽象职责清晰，协作关系形成闭环，行为契约完整：

- 调用链 `业务模块 → FallbackAiService → AiOrchestrator → CapabilityExecutor → (子组件)` 形成完整端到端闭环
- §4 各行为契约（AI 能力统合调用管线、模型路由、A/B 实验分流、模板渲染、输出解析、指标采集）以伪代码形式清晰定义了执行步骤和边界条件
- 模块间依赖方向正确（`ai-api` ← `ai-impl` 无反向依赖；`ai-impl` 内部 orchestrator → 其余子包单向依赖）
- v2 迭代完全覆盖了上一轮 10 项审查意见的修订

**[一般]** `AiCallRecord` 类图仅显示 8 个字段（callTime, capabilityId, degraded, degradationReason, elapsedMs, tokenUsage, modelId, visitId, patientId），而 `AiCallLogEntity` 定义含 19 个字段（含 caller_role, caller_id, input_summary, output_summary, error_code, error_message, retry_count, session_id, prompt_tokens, completion_tokens, total_tokens 等）。设计文本称"与 AiCallRecord 字段一一对应"（§3.5），但实际字段集合不一致。这将导致 `AiMetricsCollector.record(AiCallRecord)` 无法获取到持久化所需全部字段。建议：(1) 扩展 `AiCallRecord` 字段列表以覆盖实体全部字段；或 (2) 新增 `AiCallRecord` 到 `AiCallLogEntity` 的映射/装配层并明确字段来源。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则，抽象层次恰当，便于后续实现与测试：

- 每个抽象有清晰的单一职责（编排/Prompt 模板/模型路由/实验分流/指标采集/输出解析/降级判定）
- `CapabilityExecutor` 泛型接口使各能力逻辑独立封装，新增能力只需新增实现 + 注册
- 所有核心接口均可通过 Mockito 轻松 mock，单元测试可隔离
- `SlidingWindowMetricsStore` 的引入解决了 v1 中策略死代码问题，是合理的设计改进
- v2 Bean 装配方案（`@Primary` + `ObjectProvider` + `@ConditionalOnProperty`）消除了二义性，设计优雅

**[轻微]** `CapabilityExecutor` 接口的典型实现可能需要注入多达 8 个依赖（ModelRouter, LlmClient, PromptTemplateManager, ExperimentManager, StructuredOutputParser, AiMetricsCollector, LocalRuleFallback, SlidingWindowMetricsStore），其中 `LocalRuleFallback` 仅对处方审核等特定能力有意义。建议考虑职责分离：将管线执行逻辑抽取为可组合的 Pipeline Step 链，各 Step 按能力按需装配，减少单一实现中的依赖数量。

## 修改要求

### 维度 4（设计一致性）— 一般问题

- **问题**：`AiCallRecord` 类图字段集合（8 个）与 `AiCallLogEntity` 字段集合（19 个）不一致，设计文本声称"字段一一对应"但实际不符
- **原因**：`AiMetricsCollector.record()` 以 `AiCallRecord` 为输入，若缺少实体所需的 `caller_role`、`caller_id`、`error_code`、`retry_count` 等字段，则 `LoggingMetricsCollector` 无法构建完整的 `AiCallLogEntity`，影响数据库记录的完整性
- **建议方向**：方案 A — 扩展 `AiCallRecord` 字段列表使之与 `AiCallLogEntity` 一一对应，将 `AiCallRecord` 定位为"全量可观测性记录"；方案 B — 在 `AiMetricsCollector` 与 `AiCallLogRepository` 之间增加 `AiCallLogAssembler` 装配层，从 `AiCallRecord` + `SecurityContext` / `RequestContext` 中补全实体所需字段，并在设计中阐明各额外字段的填充策略来源
