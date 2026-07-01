# OOD 设计方案审查报告（v10）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 设计中的类型形态选择与 Java 类型系统能力完全匹配：

- `interface` 用于多实现契约（`CapabilityExecutor<T,R>`、`ModelRouter`、`LlmClient`、`PromptTemplateManager`、`ExperimentManager`、`AiMetricsCollector`、`StructuredOutputParser`、`LocalRuleFallback<T,R>`）——正确使用 Java 单继承多接口实现约束
- `abstract class` 用于实现复用（`AbstractCapabilityExecutor<T,R>` 封装降级预检 + 指标采集模板方法，`AiRequestBase` 封装跨能力公共 DTO 字段）——符合 abstract class 的设计意图
- 泛型使用方式均在 Java 泛型系统能力范围内：`CapabilityExecutor<T,R>` 的上界协变、`LocalRuleFallback<T,R>` 的类型参数、`Map<String, CapabilityExecutor>` 的 wildcard 边界（虽伪代码写 raw type，实现中可通过 `Map<String, ? extends CapabilityExecutor<?, ?>>` 避免 raw type 警告，属于实现级决策而非设计不可行）
- 继承/实现关系正确：子类 extends AbstractCapabilityExecutor，AbstractCapabilityExecutor implements CapabilityExecutor，符合 Java 约束
- 协作关系中的类型交互模式（CompletableFuture 异步返回、泛型接口注入、@Qualifier 字符串匹配注入）均可在 Java 中实现

### 2. 标准库与生态覆盖

**[通过]** 设计所依赖的能力均在 Java/Spring 生态标准覆盖范围内：

- 异步执行：`CompletableFuture` + `ExecutorService`（Java 标准库）
- 线程安全：`ConcurrentHashMap`、`AtomicReference`、`AtomicLong`（`java.util.concurrent`）
- 持久化：JPA `@Entity` + Repository（Spring Data JPA）
- 认证上下文：`SecurityContextHolder`（Spring Security）
- 条件装配：`@ConditionalOnProperty` + `EnvironmentPostProcessor`（Spring Boot）
- 指标采集：Micrometer（Spring Boot Actuator）
- JSON 处理：Jackson（Spring Boot 默认）
- 序列化：`Serializable` + `serialVersionUID` 机制（Java 标准库）
- 大模型接入：Spring AI 或 HTTP 客户端（生态覆盖）
- 依赖假设合理，无超出生态范围的第三方依赖假设

### 3. 语言特性可行性

**[通过]** 错误处理、并发、资源管理方案均与 Java 语言特性兼容：

- **错误处理**：`ParseException` / `BusinessException` 等 checked/unchecked 异常使用恰当；伪代码中 `structuredOutputParser.parse()` 已包裹 try-catch（问题1已修复）；`AiOrchestrator.handle()` 外层 catch 兜底捕获意外异常返回 `AiResult.failure()`
- **并发设计**：分级线程隔离策略合理——`LlmCallExecutor` 线程池执行 LLM 调用，专用指标采集线程池（core=1, max=2, queue=1000）使用 `DiscardPolicy` 避免阻塞主路径（问题7的饥饿风险已修复）；`SlidingWindowMetricsStore` 使用 `ConcurrentHashMap` + 写锁；`CircuitBreakerDegradationStrategy` 使用 `AtomicReference<CircuitState>` + CAS；`ModelRouter` 使用 `AtomicReference<Map>` 全量替换
- **资源管理**：HTTP 连接池（LlmClient 无状态）；线程池生命周期由 Spring 管理；数据库连接由 HikariCP 管理
- **模块/包结构**：Maven 模块划分清晰（`ai-api` / `ai-impl`），符合 Spring Boot 项目组织惯例；单向依赖方向正确

### 4. 设计一致性

**[通过]** 各抽象职责清晰，协作关系闭环，行为契约完整：

- 7 项审查意见全部修复（v10修订说明已验证）：
  - 问题1：`parse()` try-catch 已添加
  - 问题2：`SecurityContextHolder` null-safe 提取已处理，回退 `"SYSTEM"`
  - 问题3：`AbstractCapabilityExecutor` 类图已补充 `doExecuteInternal()`、`extractVariables()`、`doExtractDepartmentId()`
  - 问题4：`execute()` 模板方法与 `doExecuteInternal()` 已明确拆分为两段伪代码
  - 问题5：`extractVariables()` 已出现在类图和模板方法定义中
  - 问题6：薄适配器伪代码已删除错误的路由声明
  - 问题7：`LocalRuleFallback` 已改为泛型 `<T,R>`
- 模块依赖方向：`ai-api ← ai-impl` 单向依赖，各子包之间无循环依赖
- 行为契约（§4 伪代码）足以指导实现

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则，抽象层次恰当：

- `AiOrchestrator` 仅负责路由委托，不介入管线步骤——单一职责
- `AbstractCapabilityExecutor` 抽取公共模板方法消除 13 个实现类的重复代码——复用合理
- 薄适配器模式避免 Phase 4 已有实现的重写——符合开闭原则
- `SlidingWindowMetricsStore` + 策略链的解耦设计使降级策略可独立测试
- 接口抽象使所有组件可 mock、可隔离测试
- 设计未过度工程化（如 Pipeline Step 链已明确推迟至 Phase 6）

## 修改要求

无。设计已通过全部五个维度的审查，无严重或一般问题。

## 备注

- 本审查仅关注**设计可行性**。`Map<String, CapabilityExecutor>` 中 `CapabilityExecutor` 为 raw type 的问题可在实现阶段通过 `Map<String, ? extends CapabilityExecutor<?, ?>>` 消除，不构成设计级问题。
- `StructuredOutputParser.parse()` 内部的 JSON 片段提取重试逻辑（§4.5 第1-3步）与 §5.1 错误分类表一致，CapabilityExecutor 伪代码中该方法的 try-catch 捕获 `ParseException` 后直接降级，与 §4.5 描述的"内部重试完毕仍失败后抛出 ParseException"的流程吻合。
