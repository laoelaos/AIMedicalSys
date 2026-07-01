# OOD 设计方案审查报告（v1）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** `AiOrchestrator` 作为 class 实现 `AiService` 接口——Java 单继承 + 多接口实现模型下完全可行：`AiService` 是 interface，`AiOrchestrator` 不需要继承其他类，直接 `implements AiService` 即可。其持有 8 个协作对象引用，通过构造器注入，符合 Spring 依赖注入惯例。

**[通过]** `CapabilityExecutor` 定义为 interface——不同 AI 能力的输入/输出 DTO 类型各异，interface 允许每个能力提供独立实现，Java 类型系统完全支持。各实现类无需共享父类状态，interface 是正确选择。

**[通过]** `ModelRouter`、`LlmClient`、`PromptTemplateManager`、`ExperimentManager`、`AiMetricsCollector`、`StructuredOutputParser`、`LocalRuleFallback`、`DegradationStrategy` 均定义为 interface——Java 单继承约束下，这些抽象层只需定义行为契约，不需要携带默认实现或字段，interface 形态正确。

**[通过]** 值对象（`ModelRoute`、`LlmRequest`、`LlmResponse`、`PromptTemplate`、`ExperimentAssignment`、`Experiment`、`AiCallRecord`）均采用 class——纯数据载体不需要多态，class 配合不可变或半不可变模式在 Java 中可行。`PromptTemplate` 和 `Experiment` 同时作为 JPA Entity，class 是 JPA 的强制要求。

**[通过]** `DegradationContext` 扩展方式——在现有零参构造器基础上新增字段和全参构造器，保留无参构造器确保 `shouldDegrade(DegradationContext)` 签名不变。Java 向后兼容字段扩展完全可行，现有调用方代码无需修改。

**[通过]** `TimeoutDegradationStrategy`、`CircuitBreakerDegradationStrategy` 作为 class 实现 `DegradationStrategy` interface——Java 单继承下，实现 interface 是唯一的多态扩展方式，class 实现符合类型系统约束。

**[通过]** `@ConditionalOnProperty` 互斥装配——同一配置键 `ai.mock.enabled` 的 `havingValue = "false"` 和 `havingValue = "true", matchIfMissing = true` 互斥条件，Spring Boot 条件注解机制原生支持，Bean 注册不会冲突。

**[轻微]** `CapabilityExecutor` 的 `execute()` 方法签名在设计中未明确定义——其输入参数如何接受各能力差异化的 Request DTO、输出如何返回对应 Response DTO，未描述是否采用泛型（如 `CapabilityExecutor<RQ, RS>`）或以 Object/AiResult 携带。当前不阻塞，因为这是架构级设计而非实现规格，但建议在后续详细设计中尽早明确。

**[轻微]** `ExperimentAssignment` 的空值对象描述"分组标识为 'default'"——属于实现惯例约定而非类型系统约束，但建议明确此空值对象是否通过 `ExperimentAssignment.DEFAULT` 常量或 null + Optional 表达，以避免下游判空歧义。

### 2. 标准库与生态覆盖

**[通过]** Micrometer 指标体系——`AiMetricsCollector` 使用 `MeterRegistry` 注册 Timer/Counter/DistributionSummary，Spring Boot Actuator + Micrometer 是 Spring Boot 生态的标准可观测性方案。指标命名约定（`aimedical.ai.request.duration` 等）与 Phase 0 §10.2 预留的 `aimedical.<module>.<metric>` 前缀约定一致。

**[通过]** JPA Repository 用于模板/实验/指标持久化——`PromptTemplateRepository`、`ExperimentRepository`、`AiCallLogRepository` 继承 Spring Data JPA 的 `JpaRepository`，是项目技术栈（`02_tech.md`）明确指定的持久化方案。

**[通过]** Jackson 用于结构化输出解析——`JsonStructuredOutputParser` 基于 Jackson 反序列化，是 Spring Boot 默认 JSON 库，无需额外引入依赖。

**[通过]** Spring ApplicationEvent 用于缓存失效——`PromptTemplateManager` 和 `ExperimentManager` 通过 Spring 事件机制通知缓存失效，是 Spring 生态原生能力。

**[通过]** HTTP 客户端选型——`HttpApiLlmClient` 使用 Apache HttpClient 或 OkHttp 连接池，这两种库在 Spring Boot 生态中均为常用选择，Spring Boot 默认提供 Apache HttpClient starter。

**[通过]** `@Async` 异步指标写入——`AiMetricsCollector` 使用 Spring `@Async` + 线程池，Spring 原生支持，配置核心线程 2、最大线程 4、队列容量 1000 的参数合理。

**[轻微]** Spring AI `ChatModel`/`ChatClient` 版本兼容性——设计方案提到 `SpringAiLlmClient` 基于 Spring AI 封装，但当前父 POM（Spring Boot 3.2.5）尚未声明 Spring AI 依赖。Spring AI 1.0.0-M1+ 需要 Spring Boot 3.2.x+，版本兼容性成立，但需注意 Spring AI 目前仍为 Milestone 版本，正式 GA 后 API 可能有变化。建议在实现阶段锁定具体 Spring AI 版本并在父 POM 中声明依赖管理。

**[轻微]** 熔断策略自研 vs Resilience4j——项目选择自研 `CircuitBreakerDegradationStrategy` 而非使用 Resilience4j（Spring Cloud 生态标准熔断库），设计决策的理由是"降级策略集成到 DegradationStrategy 框架"且"Phase 5 目标为基础 A/B 实验框架可冒烟"。自研方案在 Phase 5 范围内可行，但建议在 §7 设计决策表中显式记录"不采用 Resilience4j"的决策理由，避免后续实现者产生选型疑惑。

### 3. 语言特性可行性

**[通过]** 错误处理策略与 Java 异常体系匹配——设计方案对 LLM 调用超时/不可用/解析失败等场景的处理方式清晰：异常 → 降级判定 → 本地规则兜底或 `AiResult.degraded()`。异常不跨模块边界向上抛出到业务层，由 `CapabilityExecutor` 内部消化并转换为 `AiResult`，与 `FallbackAiService` 的 `applyStrategies` 逻辑一致。

**[通过]** 并发设计与 Java 并发原语匹配：
- `ConcurrentHashMap` 用于路由表/模板缓存/实验配置缓存的线程安全读写——标准 Java 并发容器
- `AtomicReference<CircuitState>` + CAS 用于熔断状态原子转换——Java `atomic` 包原语
- `ConcurrentHashMap<String, Deque<Long>>` 滑动窗口——`ConcurrentHashMap` + `ConcurrentLinkedDeque` 或 `ArrayDeque`（受同步保护）组合可达
- `@Async` 线程池配置合理，不阻塞调用链路

**[通过]** `CompletableFuture` 异步模型延续——`AiService` 全部 13 个方法返回 `CompletableFuture<AiResult<T>>`，与 Phase 0 冻结的接口签名一致。`AiOrchestrator` 的每个方法内部通过 `CapabilityExecutor` 同步执行管线后包装为 `CompletableFuture.completedFuture()`，或使用 `CompletableFuture.supplyAsync()` 实现异步化，两种方式均可行。

**[通过]** 模块/包结构设计符合 Maven 多模块规范——`ai-impl` 内部按 `orchestrator/router/client/template/experiment/metrics/parser/fallback/mock/degradation/config` 划分子包，包命名遵循 `com.aimedical.modules.ai.impl.<subpackage>` 约定，与 Phase 0 的包命名规范一致。

**[通过]** 资源管理方案——`LlmClient` 无状态 + HTTP 连接池由 Spring 容器管理生命周期；`@Async` 线程池由 Spring 管理；JPA Repository 事务由 Spring 事务管理器管理。所有资源生命周期均纳入 Spring 容器管理，无手动资源泄露风险。

**[通过]** `DegradationContext.Builder` 模式——在现有零参构造器基础上新增 Builder，Java Builder 模式标准实现，向后兼容。

**[轻微]** 熔断器 `HALF_OPEN` 探测请求的并发安全——设计描述"允许一次探测请求通过"，但在多线程环境下可能有多个请求同时判断为 HALF_OPEN 并同时通过。自研熔断器需使用 CAS + 计数器保证仅一个探测请求通过，建议在详细设计中明确并发控制方式（如 `AtomicBoolean` 标记探测中 + CAS 置位）。

### 4. 设计一致性

**[通过]** 各抽象职责描述清晰无歧义——18 个核心抽象（§1.3）均有明确的职责定位、协作对象说明和"为何选择该类型形态"的设计论证，职责边界清晰。

**[通过]** 协作关系形成闭环——调用管线（§4.1）从 `AiOrchestrator` → `CapabilityExecutor` → 7 个子组件 → 降级/兜底，形成完整闭环：
- 正常路径：模板渲染 → 实验分流 → 模型路由 → 模型调用 → 结果解析 → 指标采集 → 返回成功
- 异常路径：失败 → 降级判定 → 本地规则降级 / `AiResult.degraded()` → 指标采集 → 返回降级结果
- 无缺失环节

**[通过]** 行为契约描述完整——§4.1~§4.7 覆盖了核心管线的每一步骤契约，包括正常路径、异常路径、优先级规则和状态转换。契约详细到足以指导后续实现。

**[通过]** 模块间依赖方向合理，无循环依赖：
- `ai-api` ← `ai-impl`（单向，ai-api 无 impl 依赖）
- `orchestrator/` → 其余子包（顶层编排层依赖底层组件，单向）
- 其余子包之间不互相依赖（`router/`、`client/`、`template/`、`experiment/`、`metrics/`、`parser/`、`fallback/`、`degradation/` 两两之间无依赖）
- `parser/` → `ai-api` DTO（已标注）
- `fallback/` → `ai-api` DTO + 业务规则（已标注）
- 无循环依赖

**[通过]** 与 Phase 0 设计的兼容性——`AiService` 接口签名不变、`FallbackAiService` 装饰器逻辑不变、`DegradationStrategy`/`DegradationContext` 接口签名冻结（`DegradationContext` 仅扩展字段）、`@ConditionalOnProperty` 装配策略延续。所有 Phase 0 冻结的契约均未违反。

**[通过]** 迁移路径清晰——§8.1 逐能力迁移表列出 7 项迁移项的 Phase 2~4 现状、Phase 5 目标和迁移策略，迁移原则（接口不变、委托对象替换、逐能力切换）可操作。

**[通过]** 与其他包的协作边界——§9.1（包 G ↔ 包 F 分诊规则版本联动）、§9.2（包 G ↔ 包 E 影像模型路由联动）、§9.3（包 G ↔ 业务模块 通过 AiService 接口隔离）的协作边界定义清晰，耦合点明确且通过接口隔离。

**[轻微]** `CapabilityExecutor` 如何被 `AiOrchestrator` "注册"——设计描述"每个 AiService 方法的实现委托给一个内部 CapabilityExecutor 实例"，但未说明注册/路由机制（Map<能力标识, CapabilityExecutor>？还是直接字段引用？还是按类型 List<CapabilityExecutor> 遍历？）。建议在后续详细设计中明确。

**[轻微]** `FallbackAiService` 在新架构中的角色微调——当前 `FallbackAiService.applyStrategies()` 在 `AiResult` 失败且未降级时遍历策略判定。但 `AiOrchestrator` 内部的 `CapabilityExecutor` 已自行完成降级判定和本地规则兜底，`FallbackAiService` 的策略判定与 `CapabilityExecutor` 的降级逻辑可能重复执行。建议明确 `FallbackAiService` 在新的 AiOrchestrator 体系下的职责是否需要缩减（例如仅作为"最后兜底"而非"策略遍历"），或在设计决策表中显式声明二者职责边界。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则——每个核心抽象聚焦单一职责：`AiOrchestrator` 只做编排，`ModelRouter` 只做路由，`LlmClient` 只做调用，`PromptTemplateManager` 只做模板管理，`ExperimentManager` 只做实验分流，`AiMetricsCollector` 只做指标采集，`StructuredOutputParser` 只做解析，`LocalRuleFallback` 只做本地规则降级。

**[通过]** 抽象层次恰当——设计既未过度设计（如未引入 Spring AI Structured Output，而是自定义 `StructuredOutputParser`；未引入 Resilience4j，而是自研熔断），也未设计不足（提供了 7 步管线的完整编排框架）。"每能力一个 CapabilityExecutor"的粒度选择合理——13 个能力的输入/输出类型各不同，统一管会导致巨型类。

**[通过]** 设计便于后续的详细设计和实现——行为契约（§4）提供了清晰的实现框架：每个步骤的输入/输出/异常处理路径均已定义，实现者可按契约逐层编码。

**[通过]** 设计便于单元测试——所有 interface 抽象（`CapabilityExecutor`、`ModelRouter`、`LlmClient`、`PromptTemplateManager`、`ExperimentManager`、`AiMetricsCollector`、`StructuredOutputParser`、`LocalRuleFallback`）均可 Mock；`AiOrchestrator` 作为编排层可独立测试——注入 Mock 子组件验证管线调用顺序和异常分支。每个 `CapabilityExecutor` 实现可独立测试其模板变量映射、解析逻辑和降级行为。

**[通过]** 设计决策表完整——§7 列出 9 项设计决策，每项含选项、选择和理由，决策论证充分，与 Phase 0 设计决策风格一致。

**[轻微]** `AiCallRecord` 字段集较大（17 个字段），但与其作为"调用日志持久化数据源"的职责定位匹配，且与 5.2 AI 调用日志实体字段一一对应。建议在详细设计阶段确认是否所有 17 个字段都在 Phase 5 首期必须持久化，部分低优先级字段（如"输入摘要""输出摘要"）可考虑延迟实现以控制首期工作量。

**[轻微]** `AiOrchestrator` 内部持有 8 个协作对象引用——从测试角度看，构造器注入 8 个依赖虽在 Java/Spring 技术上完全可行，但每次实例化和测试配置的成本较高。建议在详细设计阶段评估是否可将部分低频使用的协作对象（如 `LocalRuleFallback` 列表、`DegradationStrategy` 列表）聚合为一个上下文对象，减少构造器参数数量。

## 修改要求（REJECTED 时存在）

无严重和一般问题，本项为空。
