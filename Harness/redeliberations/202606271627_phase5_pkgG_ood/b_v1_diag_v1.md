# Phase 5 包 G — AI 进阶底座 OOD 设计 质量审查报告

审查轮次：第 1 次
审查时间：2026-06-27
审查范围：需求响应充分度、事实错误/逻辑矛盾、深度与完整性（侧重内部审议未覆盖维度）

---

## 1. 需求响应充分度

### 1.1 [CRITICAL] 缺失类图

- **问题描述**：需求明确要求"设计应覆盖：类图、核心职责、协作关系、关键接口、状态模型等OOD核心要素"。当前产出以文本表格+目录结构描述抽象，未提供任何 UML 类图（如 PlantUML / Mermaid 或其他形式），无法直观表达类之间的继承、组合、依赖关系和 cardinality。
- **所在位置**：全文
- **严重程度**：严重（CRITICAL）
- **改进建议**：补充 UML 类图，至少覆盖核心抽象层：`AiService` → `AiOrchestrator`（实现关系）、`AiOrchestrator` → `CapabilityExecutor`（组合/依赖）、`CapabilityExecutor` → `ModelRouter` / `LlmClient` / `PromptTemplateManager` / `StructuredOutputParser` / `AiMetricsCollector`（依赖）、`DegradationStrategy` 接口及三个实现类的继承关系。

### 1.2 [MAJOR] 状态模型覆盖严重不足

- **问题描述**：需求明确要求"状态模型"。当前产出仅在 4.7 节提供了熔断器的状态转换模型（CLOSED → OPEN → HALF_OPEN → CLOSED），其余关键抽象均未定义状态模型。缺失项至少包括：
  - `Experiment` 的实验配置生命周期状态（草稿/运行中/已暂停/已结束）
  - `PromptTemplate` 的模板版本状态（草稿/已启用/已废弃）
  - `AiOrchestrator` 的运行状态（初始/正常运行/全局降级/不可用）
  - `LlmClient` 的连接/健康状态
  - `DegradationContext` 在不同阶段的演变路径
- **所在位置**：缺失，无对应章节
- **严重程度**：重要（MAJOR）
- **改进建议**：为上述关键抽象定义状态转换模型，至少补充 `Experiment`（草稿→运行中→已结束）和 `PromptTemplate`（草稿→已启用→已废弃）的状态图，与需求中的"可配置、可版本化管理"和"A/B 实验框架"形成闭环。

---

## 2. 事实错误与逻辑矛盾

### 2.1 [CRITICAL] `CapabilityExecutor` 接口缺少方法签名定义

- **问题描述**：3.1 节将 `CapabilityExecutor` 定义为 interface，描述了其职责和协作对象，但**未提供任何方法签名**。作为一个"定义单项 AI 能力的执行管线契约"的核心接口，缺少 `execute()` 或其等价方法的入参类型、返回值类型、异常声明。设计方案中该接口被 `AiOrchestrator` 的每个方法委托调用，但无法从当前定义推导出统一的调用协议。13 种能力输入/输出 DTO 各不相同，若缺乏泛型参数设计，每个实现类的方法签名无法保证一致性。
- **所在位置**：3.1 节 `CapabilityExecutor` 定义
- **严重程度**：严重（CRITICAL）—— 直接影响编码实现的可行性
- **改进建议**：显式定义 `CapabilityExecutor<Req, Res>` 接口的关键方法签名，例如：
  ```java
  public interface CapabilityExecutor<Req, Res> {
      AiResult<Res> execute(Req request, ExecutionContext context);
  }
  ```
  同时定义 `ExecutionContext` 承载能力标识、科室标识等跨步骤上下文。

### 2.2 [CRITICAL] `AiOrchestrator` 与 `FallbackAiService` 的 Spring Bean 装配存在二义性

- **问题描述**：设计指出 `AiOrchestrator` 实现 `AiService` 并被 `FallbackAiService` 委托调用。对照现有代码 `FallbackAiService` 标注 `@Service` 且实现 `AiService`，`AiOrchestrator` 也将标注 `@ConditionalOnProperty` 实现 `AiService`。当 `ai.mock.enabled=false` 时，`FallbackAiService` 和 `AiOrchestrator` 同时为 `AiService` 类型的 Spring Bean。业务模块按 `@Autowired AiService` 注入时，Spring 无法确定加载哪个 Bean（无 `@Primary`，无 `@Qualifier`），将抛出 `NoUniqueBeanDefinitionException`。设计声称"业务模块只按类型注入 AiService，不使用 @Qualifier"与"FallbackAiService无需变更"存在矛盾，当前代码中的 `SimpleBean factory` 机制无法自动解决此二义性。
- **所在位置**：1.2 节架构图 + 3.1 节 Bean 装配策略 + 现有 `FallbackAiService.java:43`
- **严重程度**：严重（CRITICAL）—— 启动期即报错，阻止任何功能验证
- **改进建议**：明确声明 `FallbackAiService` 为 `@Primary`，或在 `AiPlatformConfig` 中通过显式 `@Bean` 方法控制注入层次，确保业务模块始终获得 `FallbackAiService` 实例。

### 2.3 [MAJOR] 新增 `DegradationStrategy` 实现与 `FallbackAiService.applyStrategies()` 不兼容

- **问题描述**：`TimeoutDegradationStrategy` 和 `CircuitBreakerDegradationStrategy` 是 `DegradationStrategy` 接口的实现。作为 `@Component` 或 `@Service` 注册后，会通过 `List<DegradationStrategy>` 自动注入到 `FallbackAiService`（参见现有 `FallbackAiService.java:50-51`）。但 `FallbackAiService.applyStrategies()` 内创建的是**空值 `DegradationContext`**（`new DegradationContext()`，所有新扩展字段为 Java 默认零值），这意味着 `TimeoutDegradationStrategy` 无法获取调用耗时窗口数据，`CircuitBreakerDegradationStrategy` 无法获取失败率数据。两条策略在 `FallbackAiService` 中实际上无法做出有意义判定，形成死代码。
- **所在位置**：3.8 节新增策略 + 现有 `FallbackAiService.java:183-194`
- **严重程度**：重要（MAJOR）
- **改进建议**：两种方案之一：（A）将新增策略从全局 `DegradationStrategy` 体系分离，仅由 `AiOrchestrator`/`CapabilityExecutor` 内部使用，不注册为 `@Component`；（B）重构 `FallbackAiService.applyStrategies()`，将 `AiResult` 中的失败信息填充到 `DegradationContext` 后再传给策略。推荐方案 A，与 4.1 节管线中的降级判定步骤一致。

### 2.4 [MEDIUM] `AiOrchestrator` "无状态" 断言与设计事实不符

- **问题描述**：6.1 节声称 "`AiOrchestrator`：无状态，所有方法线程安全"。但 `AiOrchestrator` 编排的组件具有显著的可变状态：`CircuitBreakerDegradationStrategy` 维护每个能力标识的熔断状态（AtomicReference）和失败计数滑动窗口；`ExperimentManager` 和 `PromptTemplateManager` 持有 `ConcurrentHashMap` 缓存；`AiMetricsCollector` 持有线程池状态。`AiOrchestrator` 作为这些组件的协调者，虽自身无字段，但其线程安全性完全依赖于所有被编排组件的线程安全。将该结论简化为 "`AiOrchestrator` 无状态" 传递了错误的安全感。
- **所在位置**：6.1 节
- **严重程度**：中等（MEDIUM）
- **改进建议**：修改表述为 "`AiOrchestrator` 自身无可变字段，线程安全依赖于其编排的所有组件均为线程安全实现（见各组件并发设计）"，并对 `CircuitBreakerDegradationStrategy` 的 `AtomicReference` + CAS 状态转换做并发正确性验证。

---

## 3. 深度与完整性

### 3.1 [MEDIUM] `DegradationContext` 字段扩展的二进制兼容性风险未评估

- **问题描述**：3.8 节计划在已发布（Phase 0）的 `DegradationContext` 上新增 5 个字段，通过"全参构造器 + Builder 模式"扩展，声称保持向后兼容。但存在以下风险：
  - `DegradationContext` 在 `ai-api` 模块中未声明 `serialVersionUID`（当前代码无 `Serializable` 实现），若任何场景（分布式缓存、事件序列化）将其序列化/反序列化，新字段可能导致兼容问题；
  - `FallbackAiService.java:187` 处 `new DegradationContext()` 使用的是无参构造器，新字段全为默认值，若后续有代码依赖新字段非空（如 `invocationCount` 预期 ≥ 0），会产生静默错误；
  - `@ConditionalOnMissingBean(DegradationStrategy.class)` 在 `NoOpDegradationStrategy` 上，注册新增策略后 `NoOp` 被抑制，但 `FallbackAiService` 的策略列表行为会改变。
- **所在位置**：3.8 节 `DegradationContext` 扩展
- **严重程度**：中等（MEDIUM）
- **改进建议**：（1）明确 `DegradationContext` 是否实现 `Serializable`；（2）评估无参构造器产生的默认值是否被任何降级策略消费；（3）新增策略应单独管理注入范围，避免自动污染 `FallbackAiService`。

### 3.2 [MEDIUM] `AiCallLog` JPA 实体未定义

- **问题描述**：3.5 节定义了值对象 `AiCallRecord` 并声明"与 5.2 AI 调用日志实体字段一一对应"，但对应的 JPA Entity（供 `AiCallLogRepository` 持久化）未建模。值对象和 JPA 实体在注解、主键策略、关联关系、索引等方面的关注点不同，缺少实体定义意味着数据库表结构无契约可依。尤其 `AiCallRecord` 包含大量字段（调用时间、能力标识、患者标识等多达 15+ 字段），未定义表索引策略可能产生查询性能风险。
- **所在位置**：3.5 节 `AiCallRecord` + `AiCallLogRepository`
- **严重程度**：中等（MEDIUM）
- **改进建议**：补充 `AiCallLog` JPA 实体定义，包括主键策略、字段注解、索引声明（按能力标识+调用时间、就诊标识等查询高频维度建索引）、与 `BaseEntity` 的继承关系。

### 3.3 [LOW] `AiMetricsCollector` 异步队列溢出策略未定义

- **问题描述**：6.1 节描述 `@Async` 线程池配置（核心 2，最大 4，队列 1000），但未指定拒绝策略（AbortPolicy / CallerRunsPolicy / DiscardPolicy）。若突发调用峰值超过处理能力（如初始 2 线程、队列满后又满），Spring 默认 `AbortPolicy` 会抛出 `TaskRejectedException`，该异常在上层 `CapabilityExecutor` 管线中未被捕获，可能导致整条调用链异常终止而非降级处理。
- **所在位置**：6.1 节
- **严重程度**：低（LOW）
- **改进建议**：明确拒绝策略，推荐 `CallerRunsPolicy`（调用线程自行写入，不丢记录但增加调用延迟）或 `DiscardPolicy` + `WARN` 日志（优先保证主线调用不阻塞），并在 `CapabilityExecutor` 的降级处理中对 `TaskRejectedException` 做保护处理。

### 3.4 [LOW] Micrometer 依赖未确认

- **问题描述**：3.5 节和 4.6 节依赖 Micrometer 推送性能指标（`Timer`、`Counter`、`DistributionSummary`）。当前项目 POM 中未显式声明 `micrometer-core` 或 `spring-boot-starter-actuator` 依赖，Micrometer 功能依赖 Spring Boot 版本自动配置的可用性。若项目未启用 Actuator 或移除了相关自动配置，Micrometer 指标注册将静默失效。
- **所在位置**：3.5 / 4.6 节
- **严重程度**：低（LOW）
- **改进建议**：在 POM 依赖规划或移行计划中明确 `spring-boot-starter-actuator` 的引入方式，确保 `MeterRegistry` 在类路径上可用。

---

## 4. 整体评价

产出在架构分层、抽象接口定义、核心职责划分等方面质量较高，管线流程（4.1~4.7）设计清晰，设计决策（第 7 节）记录完整，迁移路径（第 8 节）可操作性强。但在以下三个维度存在显著短板：

1. **需求响应**：缺少明确的类图和充分的状态模型，未完全满足需求中"类图、核心职责、协作关系、关键接口、状态模型等OOD核心要素"的要求；
2. **逻辑一致性**：`CapabilityExecutor` 无方法签名、`AiOrchestrator` 与 `FallbackAiService` 的 Bean 装配二义性、新增降级策略与现有 `FallbackAiService.applyStrategies()` 的兼容性——这三项问题会直接阻碍编码实现落地；
3. **完整性**：关键持久化实体（AiCallLog）未建模、`DegradationContext` 扩展的兼容性边界未界定、异步组件溢出策略未规划。

建议修复上述 CRITICAL 和 MAJOR 问题后进入下一轮审议。
