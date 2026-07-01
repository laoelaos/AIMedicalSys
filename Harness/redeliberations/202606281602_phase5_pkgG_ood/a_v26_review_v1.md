# OOD 设计方案审查报告（v27）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]**

- `interface`（AiService、CapabilityExecutor<T,R>、LlmChatService、ModelRouter 等）、`abstract class`（AbstractCapabilityExecutor<T,R>、AiRequestBase、Phase4BusinessException）、`class`（AiOrchestrator、FallbackAiService、各值对象）、`enum`（ClientType、AuthType、CircuitBreakerState 等）的选择均与 Java 类型系统能力匹配
- 单继承（AbstractCapabilityExecutor → 各 CapabilityExecutor 子类）+ 多接口实现（AiService 被 AiOrchestrator 等实现）符合 Java 类继承约束
- 泛型接口 CapabilityExecutor<T,R> 使用方式在 Java 泛型系统能力范围内；AbstractCapabilityExecutor<T,R> 的泛型继承关系正确
- `LocalRuleFallback<T,R>` 的 unchecked 转换风险已在 §7 和 §4.1 中通过 `inputType.isInstance(request)` 运行时检查 + 降级兜底机制防御，设计层面可行
- 协变/逆变场景（如 `List<CapabilityExecutor>` 通配符注入）均为 Java 标准用法

### 2. 标准库与生态覆盖

**[通过]**

- Spring Boot（容器、AOP、@Async、@EventListener、@ConditionalOnProperty/Class）— 标准覆盖
- Spring Data JPA + Hibernate — 标准覆盖
- Jackson（jackson-databind）— DTO 序列化/防御性拷贝 — 标准覆盖
- Caffeine — 模板/实验/凭据缓存 — 标准覆盖
- Guava RateLimiter — 端点限流 — 标准覆盖
- Micrometer（Spring Boot Actuator）— 指标采集 — 标准覆盖
- reactor-core（optional）— Flux 流式响应 — 标准覆盖
- jtokkit（optional）— 精确 Token 计数 — 第三方库，标记为 optional，缺失时有字符估算回退
- 设计中对各库能力的假设合理，所有 API 使用方式符合库的惯用实践

### 3. 语言特性可行性

**[通过]**

- 错误处理策略与 Java 异常体系匹配：RuntimeException 子类（StructuredOutputNotSupportedException、LlmInfrastructureException、Phase4BusinessException）区分异常类型；CompletionException 拆解模式符合 CompletableFuture 的异常传播约定
- 并发设计兼容 Java 并发模型：CompletableFuture + ExecutorService 线程池隔离 + synchronized (Deque) + AtomicReference/CAS + volatile。线程池配置（CallerRunsPolicy vs DiscardPolicy）合理解决了背压与主路径阻塞的权衡
- 资源管理方案可行：线程池生命周期（@PreDestroy/DiscardPolicy）、HTTP 连接池、Caffeine 缓存自动淘汰
- 模块/包结构符合 Maven 项目组织方式；provided 作用域的运行时风险（uber-JAR 打包）已在 §2.2 显式记录

### 4. 设计一致性

**[通过]**

- 各抽象职责描述清晰无歧义：15 个核心抽象（§1.3）+ 7 项能力特化（§3.11）+ 6 项薄适配器，职责边界明确
- 协作关系形成完整闭环：AiService → FallbackAiService → AiOrchestrator → CapabilityExecutor → {ModelRouter, LlmChatService, PromptTemplateManager, ExperimentManager, ...}，无缺失环节
- 行为契约完整：§4 伪代码覆盖主管线、薄适配器管线、路由契约、实验分流契约、模板渲染契约、解析契约、指标采集契约
- 模块依赖方向合理，无循环依赖：ai-api ← ai-impl（单向），ai-impl 内部 orchestrator → 其余子包（单向）

### 5. 设计质量

**[通过]**

- 职责划分遵循单一职责原则：AiOrchestrator（路由委托）≠ CapabilityExecutor（管线执行）≠ FallbackAiService（降级装饰）≠ SlidingWindowMetricsStore（指标存储）
- 抽象层次恰当：AbstractCapabilityExecutor 模板方法模式提供骨架，子类仅特化差异化步骤；CallContext 三期迁移计划诚实记录了已知的参数膨胀问题
- 设计便于后续详细设计和实现：§1.7 实施拓扑顺序按批次组织，§1.6 API Surface 状态表标注实施状态
- 设计便于单元测试：§11 覆盖单元测试、集成测试、并发竞争测试、配置热刷新测试；组件通过构造器注入可 mock；`RequestContextUtils` 设计为静态工具类便于测试桩

## 修改要求

无。
