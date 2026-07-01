# OOD 设计方案审查报告（v14）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 设计中的类型形态选择（interface / abstract class / class / enum / static class 等）与 Java 类型系统能力完全匹配。

**[通过]** 抽象之间的继承和实现关系符合 Java 约束（单继承 + 多接口实现）：`AbstractCapabilityExecutor` 继承链单一，`AiService` 接口由多个实现类各自独立实现。

**[通过]** 泛型使用（`CapabilityExecutor<T,R>`、`StructuredChatResult<T>`）均在 Java 泛型系统能力范围内。

**[通过]** `LocalRuleFallback<T,R>` 的泛型类型擦除导致的 unchecked 转换风险已在 §7 决策表记录，`AbstractCapabilityExecutor.doDegrade()` 中增加了 `inputType.isInstance(request)` 运行时类型检查，防御措施完整。

**[轻微]** `AiCallRecord` 工厂方法 15~16 参数虽不违反类型系统约束，但大量参数的类型签名增加了编译期类型匹配错误的概率。设计已记录此风险并提出 `CallRecordContext` 聚合方向，可在实施期按需优化。

### 2. 标准库与生态覆盖

**[通过]** 设计所需能力均在 Java 标准库和 Spring Boot 生态的覆盖范围内：
- 集合库（`ConcurrentHashMap`、`Deque`）— 标准库
- 异步编程（`CompletableFuture`、`ThreadPoolExecutor`）— 标准库
- JPA 持久化（`@Entity`、`@Repository`）— Spring Data JPA
- Jackson 序列化— Jackson
- 缓存（Caffeine）— Caffeine 库
- 限流（Guava `RateLimiter`）— Guava 库
- 指标采集（Micrometer）— Spring Boot Actuator
- HTTP 客户端 / Spring AI — 可选依赖

**[通过]** 设计中的库假设合理：Caffeine 的 `Expiry` 接口用于凭据缓存动态 TTL，Guava 令牌桶用于端点限流，Micrometer 用于指标暴露。

**[通过]** 标准库能力对自定义抽象的简化合理：`CompletableFuture` 统一异步边界，`AtomicReference` 实现线程安全状态转换，`ConcurrentHashMap` 提供无锁读性能。

### 3. 语言特性可行性

**[通过]** 错误处理策略与 Java 异常模型匹配：Checked/Unchecked 异常分类清晰（`RuntimeException` 子类），`StructuredOutputNotSupportedException` / `LlmInfrastructureException` 两异常分类设计合理，`CompletableFuture.exceptionally()` 统一异步异常处理。

**[通过]** 并发设计符合 Java 并发模型：
- `AbstractCapabilityExecutor` 使用 `CompletableFuture.supplyAsync()` + `orTimeout()` 异步管线
- `SlidingWindowMetricsStore` 使用 `ConcurrentHashMap` + `synchronized` 写锁
- `CircuitBreakerDegradationStrategy` 使用 `AtomicReference` + CAS
- `ThreadPoolExecutor` + `CallerRunsPolicy` / `DiscardPolicy` 拒绝策略合理

**[通过]** `DiscussionConclusionCapabilityExecutor` 前置 LLM 压缩调用导致的线程池饥饿风险已在 v14 中补充分析：§3.11.7 明确记录了阻塞风险评估、隔离方案建议（独立 `transcriptSummaryExecutor`）、YAML 注释说明。风险已识别并有缓解方向，不阻塞通过。

**[通过]** 模块/包结构设计符合 Java/Spring Boot 项目组织方式，`ai-api` / `ai-impl` 分层清晰，依赖方向单向。

**[通过]** 资源管理模式在 Java 范围内可行：线程池生命周期由 Spring 容器管理，凭据缓存的 `Expiry` 接口方案符合 Caffeine 库的最佳实践。

### 4. 设计一致性

**[通过]** 各抽象的职责描述清晰无歧义：§3 核心抽象中每个组件均以"角色/职责/协作对象/类型形态选择理由"四元组明确描述。

**[通过]** 协作关系形成闭环：`AiOrchestrator` → `CapabilityExecutor`（依赖 `ModelRouter` / `LlmChatService` / `PromptTemplateManager` / `ExperimentManager` / `AiMetricsCollector` / `SlidingWindowMetricsStore` 等）→ 降级/成功路径均有完整定义。管线步骤在 §4.1 伪代码中逐行可追踪。

**[通过]** §5.1 错误分类表已按 v14 迭代要求同步修正为 `ExperimentAssignment.createErrorFallback()`（`groupId="experiment-error"`），与 §3.4、§4.1、§4.4 保持一致。

**[通过]** §11.1 单元测试模式中实验分流异常的验证断言已同步修正。

**[通过]** 模块间依赖方向合理，无循环依赖：`ai-api ← ai-impl`，`ai-impl` 内部各子包单向依赖。

**[通过]** `DelegatingLlmChatService` 的异常传播契约已在 v14 中补充，底层异常原样透传，`CapabilityExecutor` 仅在回调中处理异常，同步/异步双路径异常覆盖已确认。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则：
- `AiOrchestrator` — 仅负责路由委托
- `CapabilityExecutor` — 仅负责单项能力的管線执行
- `AiMetricsCollector` — 仅负责指标采集与持久化
- `ModelRouter` — 仅负责模型路由决策

**[通过]** 抽象层次恰当：不包含实现细节（如具体字段名、SQL 语句、API 调用代码），保持在架构级设计粒度。

**[通过]** `doDegrade()` 14 参数、`AbstractCapabilityExecutor` 构造器 13+ 参数、`AiCallRecord` 工厂方法 15~16 参数的可维护性风险已在 v14 中记录（§3.1 构造器伪代码后的参数数量说明段落），建议抽取 `ExecutionContext` / `CallRecordContext` 上下文对象聚合通用参数，标记为 Phase 6 或实施期可选优化。此问题为已知设计约束不阻塞通过，但建议实施期优先处理。

**[通过]** `LlmChatRequest.tools` 字段构造不一致性风险已在 v14 中说明（§3.2 字段级契约），建议将 `tools` 纳入全参构造器或显式声明 null 时的默认行为。

**[通过]** 设计便于后续详细设计和实现：类图、伪代码、字段级契约、配置示例均已完备。

**[通过]** 设计便于单元测试：§11.1 详细列出了各降级路径的单元测试模式，§11.3~§11.5 覆盖了滑动窗口指标、防御性拷贝、异步上下文传播、并发竞争等场景。

## 修改要求（无严重/一般问题，不适用）

对轻微问题的实施建议已在上述对应维度中注明。
