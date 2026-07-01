# OOD 设计方案审查报告（v1）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 设计中使用 interface（AiService、CapabilityExecutor、LlmChatService 等）、abstract class（AbstractCapabilityExecutor）、class（AiOrchestrator、各种值对象）、enum（ClientType、DegradationReason 等）、JPA @Entity、泛型接口 CapabilityExecutor<T,R> 等类型形态，均与 Java 类型系统完美匹配。单继承+多接口实现的约束关系清晰（AbstractCapabilityExecutor 为抽象基类，各具体执行器 extends 它并 implements CapabilityExecutor 接口）。泛型使用正确（T/R 类型参数绑定 DTO 类型）。协作关系描述的类型交互模式在 Java 中均可实现。

**[通过]** 枚举常量使用（ClientType.HTTP_API/SPRING_AI、AuthType.API_KEY/OAUTH2/NONE、DegradationReason 等）符合 Java 枚举最佳实践。

**[通过]** CompletableFuture 作为异步返回类型，与 Java 并发 API 完全匹配。

### 2. 标准库与生态覆盖

**[通过]** 设计中的能力均在 Java/Spring Boot 生态的标准库或常用库覆盖范围内：Spring DI（@Component、@Configuration、@Bean）、Spring Web（RequestContextHolder）、Spring Data JPA（@Entity、Repository）、Jackson（DTO 序列化/防御性拷贝）、Caffeine（缓存）、Guava（RateLimiter）、Micrometer（指标采集）、Spring AI（可选 LLM 客户端）、OkHttp/WebClient（HTTP 调用）。

**[通过]** 对库能力的假设合理（如 Guava RateLimiter 线程安全、Caffeine 支持 TTL、Jackson ObjectMapper 支持 convertValue）。

**[通过]** 设计合理利用了标准库能力（如 CompletableFuture.orTimeout() 用于超时控制、@Scheduled 用于定时刷新、@Async 用于异步采集），减少自定义抽象。

### 3. 语言特性可行性

**[通过]** 错误处理策略与 Java 能力匹配：CompletableFuture 异步结果包装（AiResult.success/failure/degraded）+ 异常分类（StructuredOutputNotSupportedException、LlmInfrastructureException、Phase4BusinessException）+ try-catch 异常捕获 + orTimeout() 超时兜底。CompletableFuture.exceptionally() 中的 CompletionException 包装模式正确。

**[通过]** 并发设计兼容 Java 并发模型：CompletableFuture.supplyAsync() + 自定义 ThreadPoolExecutor、AtomicReference CAS 状态转换、ConcurrentHashMap 并发容器、volatile 内存可见性、synchronized 临界区（SlidingWindowMetricsStore Deque 级别写锁）、@Async + 独立线程池。线程安全契约描述完整。

**[通过]** 资源管理模式在 Java 内可行：线程池有界队列（LinkedBlockingQueue + CallerRunsPolicy/DiscardPolicy）、JPA EntityManager 由 Spring 管理、HTTP 连接池（HikariCP）、Caffeine 缓存 TTL + 惰性淘汰。

**[通过]** 模块/包结构设计符合 Java/Spring Boot 项目组织方式：ai-api（接口/DTO 模块）与 ai-impl（实现模块）分离，内部按功能域划分子包（orchestrator/client/router/template/experiment/metrics/parser/fallback/degradation/config）。Maven 依赖管理清晰，provided/compile/optional 作用域使用合理。

### 4. 设计一致性

**[通过]** 各抽象的职责描述清晰无歧义。§3 核心抽象章节对每个组件使用"角色/职责/协作对象/类型形态选择理由"四元组格式，描述一致。

**[通过]** 协作关系形成闭环：AiOrchestrator → CapabilityExecutor → 各基础设施组件（ModelRouter/PromptTemplateManager/ExperimentManager/LlmChatService/StructuredOutputParser/AiMetricsCollector/SlidingWindowMetricsStore/DegradationStrategy/EndpointHealthManager），各组件职责完整无缺失。

**[通过]** 行为契约的伪代码完整到足以指导后续实现。§4 提供了详细的伪代码（降级预检、实验分流、模板渲染、模型路由、LLM 调用、结构化解析、指标采集全流程），以及薄适配器特化管线。

**[通过]** 模块间依赖方向合理，无循环依赖：ai-api → ai-impl（单向），ai-impl 内部 orchestrator 依赖其余子包，其余子包之间不互相依赖。thin-adapter 依赖 Phase 4 模块（出向依赖）。

**[轻微]** doDegrade() 方法的伪代码定义（§4.1 第 3357 行）显示为 7 参数 CallContext 新签名形式，但所有调用点（如第 3086、3109 行）仍使用旧的多参数签名（15 参数），两者不一致。按 v22 修订方案 A（保持旧参数签名），定义应与调用点对齐。此问题不影响设计可行性，实施阶段可同步修正。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则：AiOrchestrator 仅负责路由委托，CapabilityExecutor 负责管线执行，LlmChatService 负责 LLM 调用，各策略独立实现等。装饰器、模板方法、分发层等设计模式使用恰当。

**[通过]** 抽象层次恰当：AbstractCapabilityExecutor 抽取了降级预检和指标采集等公共步骤，子类仅需特化 doExecuteInternal()。不过度设计（如未引入额外的 SPI 层），也不设计不足。

**[通过]** 设计便于后续详细设计和实现。实施拓扑顺序（§1.7）清晰，7 个批次的依赖关系和验收标准明确。

**[通过]** 设计便于单元测试（§11 测试策略详细）：各组件依赖接口，可轻松使用 @MockBean 模拟下游依赖；降级路径触发条件测试覆盖熔断、超时、端点不可用、路由不可用、解析失败等场景；并发竞争验证使用 CountDownLatch/CyclicBarrier。

## 修改要求

无严重或一般问题，不存在修改要求。
