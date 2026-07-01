# OOD 设计方案审查报告（v3）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 所有类型形态选择（interface / abstract class / class / enum / static inner class）与 Java 类型系统能力完全匹配。单一继承 + 多接口实现约束被严格遵守——`AbstractCapabilityExecutor` 作为抽象基类提供模板方法，具体执行器通过继承获得公共实现；`AiService`、`DegradationStrategy` 等接口提供多态契约，子类可通过多接口实现扩展行为。

**[通过]** 泛型使用 (`CapabilityExecutor<T,R>`, `StructuredChatResult<T>`, `LocalRuleFallback<T,R>`) 均在 Java 泛型系统能力范围内。设计明确认识到类型擦除带来的 `unchecked` 转换风险，并通过 `Class<T> inputType` 运行时类型检查 (`inputType.isInstance(request)`) 提供了可落地的防御措施。

**[通过]** 协作关系中的类型交互模式——`DelegatingLlmChatService` 分发模式、`@Qualifier("{capabilityId}Strategies")` 命名推导规则、`ObjectProvider<T>` 处理可选依赖——均在 Spring DI 框架能力范围内，编译期与运行期行为均可预期。

**[通过]** 枚举类型 (`ClientType`, `LlmChatMessageRole`, `DegradationReason`) 提供编译期类型安全，替代字符串字面量的设计合理。异常类型 (`StructuredOutputNotSupportedException`, `LlmInfrastructureException`) 继承 `RuntimeException`，区分语义明确。

### 2. 标准库与生态覆盖

**[通过]** 设计中依赖的全部库能力在 Java 生态中均有成熟覆盖：Spring Boot（IoC、AOP、配置绑定、`@Async`）、Spring Data JPA（ORM、Repository）、Jackson（JSON 序列化/防御性拷贝）、Caffeine（模板/实验/凭据缓存）、Guava（令牌桶限流）、Micrometer（指标采集）、Reactor Core（Flux 流式响应）、Spring AI（可选 ChatModel 接入）。

**[通过]** 库能力假设合理。特别值得肯定的是：指标采集双写（数据库 + Micrometer）设计利用了 Spring Boot Actuator 的自动装配能力；Caffeine `Expiry` 接口的动态 TTL 延长方案符合该库的设计意图。

**[通过]** 自定义抽象（`LlmChatService` 接口替代直接使用 Spring AI）有充分理由——支持 HTTP API 直连与 Spring AI 两种接入方式共存，且使 Spring AI 成为可选依赖。`StructuredOutputParser` 的自定义设计同样合理（可适配 JSON/Markdown/自由文本多种输出格式）。

### 3. 语言特性可行性

**[通过]** 错误处理策略与 Java 能力匹配。`AiResult` 返回模式（success/failure/degraded）将预期业务结果与异常路径分离，符合 Java 中"用返回值而非异常表达业务预期"的最佳实践。异常分类（`StructuredOutputNotSupportedException` → 回退 / `LlmInfrastructureException` → 降级）语义清晰，`catch` 分支边界明确。

**[通过]** 并发设计充分利用 Java 并发工具链：`CompletableFuture` 异步编排、`ThreadPoolExecutor` 线程池隔离（LLM 调用池与指标采集池分离）、`ConcurrentHashMap` + `synchronized` 块保护滑动窗口、`AtomicReference` + CAS 实现熔断器状态转换。线程池拒绝策略（`DiscardPolicy` 用于指标路径、`CallerRunsPolicy` 用于 LLM 调用路径）兼顾了主路径可用性与副路径可丢失性。

**[通过]** 异步上下文传播方案（入口处提取 ThreadLocal → 闭包捕获 → 显式参数传递）是 Java `CompletableFuture` + 线程池场景下的标准实践。`SecurityContextHolder` 和 `RequestContextHolder` 的提取时机正确（`supplyAsync` 之前，容器线程）。

**[通过]** 模块/包结构设计合理：`ai-api` 作为纯接口/DTO 模块无实现依赖，`ai-impl` 内部分层清晰（orchestrator → client/router/template/experiment/metrics/parser/fallback/degradation）。`thin-adapter/` 的子包位置在目录结构和依赖关系图中已一致。

**[通过]** `AiPlatformEnvironmentPostProcessor` 与 `AiPlatformConfig` 的分离设计正确利用了 Spring `EnvironmentPostProcessor` 机制在容器初始化前完成配置转发，生命周期职责清晰。

### 4. 设计一致性

**[通过]** 各抽象的职责描述清晰无歧义。`AiOrchestrator` 定位为"路由层"而非"管线持有者"，`CapabilityExecutor` 持有完整管线，`AbstractCapabilityExecutor` 提供模板方法骨架——三层职责分离明确。

**[通过]** 协作关系形成闭环。降级预检 → 实验分流 → 模板渲染 → 模型路由 → 端点健康检查 → LLM 调用 → 结果解析 → 指标采集的 8 步管线路径完整，每个步骤的协作对象、异常处理、降级回退均有定义。薄适配器管线（降级预检 → Phase 4 委托 → 指标采集）的简化路径同样完整。

**[通过]** 行为契约描述充分。伪代码（第 4 章）覆盖了编排层、薄适配器、模型路由、A/B 实验、模板渲染、结构化解析、指标采集全部关键路径。超时层级关系（`capabilityTimeout` ≥ `thinAdapterTimeout` + 缓冲）已明确定义。

**[通过]** 模块间依赖方向合理，无循环依赖。`ai-api ← ai-impl` 的单向依赖、`ai-impl` 内部 orchestrator→其余子包的单向依赖、`thin-adapter` → Phase 4 模块的出向依赖均在 §2.2 中明确记录，`provided` 作用域及其运行时风险已充分评估。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则：`AiOrchestrator` 仅路由不介入管线、`ModelRouter` 仅路由不感知调用、`AiMetricsCollector` 仅采集不参与判定、`SlidingWindowMetricsStore` 仅提供数据不做策略决策。每个抽象有明确的职责边界。

**[通过]** 抽象层次恰当。设计未陷入实现细节（如具体的 HTTP 客户端选型、SQL 语句），也未停留在过于模糊的概念层。类图、状态模型、方法签名和伪代码提供了足够的架构级指引。

**[通过]** 设计便于后续的详细设计和实现。每项能力对应独立的 `CapabilityExecutor` 实现，新增能力只需新增一个子类；`AbstractCapabilityExecutor` 模板方法确保降级预检不可绕过；薄适配器与完整管线的区分降低了 Phase 4 能力的迁移门槛。

**[通过]** 设计便于单元测试（可 mock、可隔离）。§11 提供了详细的测试策略，包括单元测试模式（`@MockBean` 模拟所有下游依赖）、集成测试模式、并发竞争验证（`CountDownLatch` + `CyclicBarrier`）、状态恢复路径全覆盖。`RequestContextUtils` 被抽取为静态工具类便于 `MockHttpServletRequest` 测试。

## 修改要求

无。v3 设计已完整回应 v2 审查中识别的全部 9 个问题，未引入新的严重或一般等级问题。
