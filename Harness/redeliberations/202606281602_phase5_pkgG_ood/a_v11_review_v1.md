# OOD 设计方案审查报告（v11）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 所有抽象的类型形态选择（interface / abstract class / class / enum / exception）均与 Java 类型系统能力匹配。

**[通过]** 继承层次符合 Java 单继承约束：`AbstractCapabilityExecutor` 为唯一抽象基类，13 个 `XxxCapabilityExecutor` 均直接继承它；`AiRequestBase` 为 DTO 抽象基类，各能力 DTO 直接继承。

**[通过]** 接口多实现机制使用正确：`AiService` 由 `AiOrchestrator`、`FallbackAiService` 分别实现，互不冲突；`LlmChatService` 由 `DelegatingLlmChatService`、`HttpApiLlmChatService`、`SpringAiLlmChatService` 实现，分发模式合理。

**[通过]** 泛型使用在 Java 泛型能力范围内：`CapabilityExecutor<T,R>`、`AbstractCapabilityExecutor<T,R>`、`LocalRuleFallback<T,R>`、`StructuredChatResult<T>` 均为标准泛型用法。

**[通过]** `DegradationContext` 实现 `Serializable` 接口，新增字段使用包装类型 `Integer` 替代原始 `int`，确保 Jackson 反序列化旧 JSON 时缺失字段为 null 而非 0。

### 2. 标准库与生态覆盖

**[通过]** 设计所依赖的能力均在 Java / Spring Boot 生态覆盖范围内：
- Spring Boot Starter（容器、AOP、配置绑定）
- Spring Data JPA + 数据库驱动（模板/实验/指标持久化）
- Jackson（DTO 序列化/防御性拷贝）
- Caffeine（模板/实验/凭据缓存）
- Guava RateLimiter（端点限流）
- Micrometer（指标采集）
- Reactor Core（流式对话，可选依赖）
- Spring AI ChatModel（可选依赖）

**[通过]** `LlmChatStreamService` 的 `Flux` 返回类型要求 reactor-core 为编译期强制依赖，设计已明确标注其为条件性依赖并在 §8.2 声明 `optional=true`。

**[通过]** Micrometer 依赖通过显式声明 `spring-boot-starter-actuator` 引入而非依赖自动配置，避免缺失时的静默失败。

### 3. 语言特性可行性

**[通过]** 错误处理策略与 Java 异常体系匹配：
- `StructuredOutputNotSupportedException` 和 `LlmInfrastructureException` 继承 `RuntimeException`，符合非受检异常模式
- `CompletableFuture` + `.exceptionally()` 统一处理异步异常，不传播原始异常到 Tomcat 容器线程
- `orTimeout()` 兜底超时符合 Java 8+ CompletableFuture 标准用法

**[通过]** 并发模型设计与 Java 并发原语匹配：
- `CompletableFuture.supplyAsync()` + 自定义线程池实现异步管线
- `ConcurrentHashMap` + `synchronized` 块保护滑动窗口数据
- `AtomicReference` + CAS 实现熔断器状态转换
- `CallerRunsPolicy` 用于 LLM 调用线程池自然背压，`DiscardPolicy` 用于指标采集线程池避免阻塞主路径
- 异步上下文传播采用"入口处提取 + 闭包捕获"模式，避免 ThreadLocal 在线程池中丢失

**[通过]** 资源管理模式（Spring Bean 生命周期管理 + 线程池 + JPA EntityManager）均为标准 Spring Boot 实践，无不可行假设。

**[通过]** 模块/包结构遵循标准 Maven 多模块布局，依赖方向清晰单向（`ai-impl` → `ai-api`），无循环依赖。

**[通过]** `thin-adapter/` 子包通过 `provided` 作用域引用 Phase 4 模块，设计已明确记录此模式的运行时风险及 Uber-JAR 部署的约束。

### 4. 设计一致性

**[通过]** §2.3 类图中 `AbstractCapabilityExecutor.doDegrade` 方法签名的 14 个参数（含 `modelId`）与 §4.1 伪代码定义及全部调用点一致——三个遗留不一致问题已在本版本全部修复。

**[通过]** 薄适配器构造器 `super()` 调用参数数量（12 个）与 `AbstractCapabilityExecutor` 构造器签名（12 个参数）完全匹配。

**[通过]** §4.2 薄适配器 catch 块已将 `BusinessException` 引用替换为通过异常类名匹配 6 个已知 Phase 4 业务异常类型的统一策略，消除未验证的异常类型引用风险。

**[通过]** 各抽象职责描述清晰，协作关系形成闭环：
- `AiService` → `FallbackAiService` → `AiOrchestrator` → `CapabilityExecutor` → 基础设施组件构成完整调用链
- 降级预检、实验分流、模板渲染、模型路由、LLM 调用、解析、指标采集七步骤完整覆盖
- 错误处理表（§5.1）覆盖了全部 12 个错误场景及其处理方式

**[通过]** 模块间依赖方向均为单向：`orchestrator/` 依赖所有其余子包，其余子包之间不互相依赖。

### 5. 设计质量

**[通过]** 职责划分符合 SRP：
- `AiOrchestrator`：仅负责路由委托
- `CapabilityExecutor`：负责单项能力的完整管线
- `AbstractCapabilityExecutor`：封装公共模板方法
- `LlmChatService` / `LlmChatStreamService`：隔离 LLM 调用差异
- `SlidingWindowMetricsStore`：统一指标数据源

**[通过]** 抽象层次恰当——定位为架构级设计而非实现规格，不包含具体字段/方法体实现细节是合理的。

**[通过]** 设计便于后续详细设计和实现：模板方法模式使新增能力只需创建新 `CapabilityExecutor` 子类并特化 `doExecuteInternal()`。

**[通过]** 设计便于单元测试：§11 明确给出了各组件（熔断器、滑动窗口、端点健康管理器、凭据提供者等）的可 mock 策略和并发竞争测试方法。

## 修改要求

无严重或一般问题。
