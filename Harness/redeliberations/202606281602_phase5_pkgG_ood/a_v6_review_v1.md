# OOD 设计方案审查报告（v6）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 设计中的类型形态选择（interface / abstract class / class / enum / JPA @Entity）与 Java 类型系统能力完全匹配。关键审查结论如下：

- **泛型参数使用**：`CapabilityExecutor<T,R>`、`AbstractCapabilityExecutor<T,R>`、`StructuredChatResult<T>`、`LocalRuleFallback<T,R>` 均使用无界类型参数，在 Java 泛型系统能力范围内。由于类型擦除，`LocalRuleFallback<T,R>` 的 `@Autowired(required=false)` 注入依赖于当前唯一实现 `PrescriptionLocalRuleFallback`，设计已在 `doDegrade()` 中通过 `inputType.isInstance(request)` 运行时类型检查防御 `ClassCastException`，这是对 Java 泛型擦除的合理妥协方案。

- **继承关系**：单继承链 `AbstractCapabilityExecutor → (13 个子类)` 符合 Java 单继承约束；`CapabilityExecutor` 接口被 `AbstractCapabilityExecutor` 实现，AiService 接口被 `AiOrchestrator`/`FallbackAiService`/`MockAiService` 分别实现，均在约束范围内。

- **运行时类型信息**：`getInputType()`/`getOutputType()` 返回 `Class<T>`/`Class<R>` 的模式是 Java 中解决泛型擦除问题的标准做法。构造器注入 `Class<T> inputType` 字段的设计正确。

- **CompletableFuture 与 Flux**：`CompletableFuture<AiResult<R>>` 为 Java 8+ 标准异步契约；`Flux<LlmChatResponse>` 引入 Reactor 依赖，依赖标注为 optional 并注明"编译期强制依赖"，方式正确。

- **枚举类型**：`ClientType`、`LlmChatMessageRole`、`DegradationReason` 等枚举使用恰当，提供编译期类型安全。

### 2. 标准库与生态覆盖

**[通过]** 设计中所有依赖假设均合理：

- **集合与并发**：`ConcurrentHashMap`、`Deque`、`AtomicReference`、`CompletableFuture`、`ThreadPoolExecutor` — 标准 Java 库。
- **Spring Boot 生态**：JPA Repository、`@Async`、`@ConditionalOnProperty`、`ObjectProvider`、`ApplicationEvent`、`EnvironmentPostProcessor` — 均为 Spring Boot 标准能力。
- **缓存**：Caffeine — Spring Boot 生态常用缓存库。
- **限流**：Guava `RateLimiter` — 令牌桶算法标准实现。
- **序列化**：Jackson — Spring Boot 默认序列化引擎。
- **指标采集**：Micrometer（通过 `spring-boot-starter-actuator`）— 标准指标体系。
- **HTTP 客户端**：OkHttp / WebClient — 均为 Spring Boot 生态常用选择。
- **可选依赖**：Reactor Core（Flux 场景）、Spring AI（Spring AI ChatModel 场景）— 合理标注 optional 作用域，`@ConditionalOnClass` 保护装配安全。
- **Phase 4 业务模块**：`provided` 作用域声明，运行时由同一容器保证类加载，风险已在 §2.2 显式记录。

### 3. 语言特性可行性

**[通过]** 

- **错误处理策略**：与 Java 异常机制匹配。`StructuredOutputNotSupportedException` 和 `LlmInfrastructureException` 继承 `RuntimeException`（非受检异常），在 `doExecuteInternal()` 中用两个独立 `catch` 分支分类处理，符合设计意图。`AiResult` 模式在业务层替代异常传播，是 Java 服务端设计的常见模式。

- **并发设计**：`CompletableFuture.supplyAsync()` + `orTimeout()` 模式为 Java 8+ 标准异步超时控制。`ThreadLocal` 上下文在 `supplyAsync()` 之前提取并通过闭包捕获传递，是 Java 异步编程中处理 SecurityContext/RequestContext 丢失问题的标准方案。线程池隔离（LLM 调用线程池与指标采集线程池独立）设计合理，拒绝策略差异（CallerRunsPolicy vs DiscardPolicy）有明确理由。

- **资源管理**：线程池配置有界队列 + 明确拒绝策略；HTTP 客户端连接池；Caffeine 缓存有界（maxSize=1000）；滑动窗口有事件上限（max-events-per-capability=10000）。所有资源均在可控范围内，无泄漏风险。

- **模块/包结构**：Maven 模块 `ai-api`（接口+DTO）与 `ai-impl`（实现）分离，`ai-impl` 内部按功能域分子包，依赖方向为单向（orchestrator → 其余子包），符合 Java 项目组织最佳实践。

### 4. 设计一致性

**[通过]**

- **职责覆盖**：所有核心抽象职责描述清晰，无歧义。协作关系形成闭环：`AiOrchestrator`（路由）→ `CapabilityExecutor`（管线）→ `PromptTemplateManager`/`ModelRouter`/`LlmChatService`/`StructuredOutputParser`/`AiMetricsCollector`/`DegradationStrategy`/`LocalRuleFallback`/`ModelEndpointHealthManager`（基础设施），无缺失环节。

- **依赖方向**：`ai-api ← ai-impl` 正确；`ai-impl` 内部 `orchestrator/` 依赖其余子包，子包间无互相依赖；`thin-adapter/` 出向依赖 Phase 4 模块（产生循环依赖）。方向合理。

- **行为契约完整性**：§4 提供了完整的伪代码描述管线行为，§3 各组件有职责/协作对象/类型形态选择理由，行为契约足以指导后续实现。

- **v5 审查意见修正一致性**：对迭代需求中列出的 9 个问题（2 严重、2 重要、5 中等），逐一核查：
  - 问题 1（严重，DTO 字段描述失实）：§3.5 已修正，明确当前为空类，迁移代价已扩展 ✓
  - 问题 2（严重，TriageResponse 字段不一致）：§3.11.1 已修正为 `recommendedDepartments: List<RecommendedDepartment>` ✓
  - 问题 3（重要，DTO 扩展字段未标注状态）：§3.5 新增"DTO 业务字段补齐计划"，§3.11 标记 ⊕/△/✓ ✓
  - 问题 4（重要，薄适配器空 DTO 矛盾）：§3.1 新增"Phase 4 服务接口契约定义"段落 ✓
  - 问题 5（中等，DB 方言兼容性）：§3.5 新增数据库类型兼容性说明 ✓
  - 问题 6（中等，决策表矛盾）：§7 补充 [LLM 调用线程池] 前缀 + 适用组件说明 ✓
  - 问题 7（中等，文件路径不一致）：§3.1 补充 DTO 现状记录 ✓
  - 问题 8（中等，Jackson 反序列化遗漏）：§3.8 补充 Jackson 反序列化兼容性分析 ✓
  - 问题 9（中等，共同约束未明确编码责任）：§3.11 新增"DTO 扩展字段编码责任"条目 ✓

### 5. 设计质量

**[通过]**

- **单一职责原则**：`AiOrchestrator`（仅路由）、`AbstractCapabilityExecutor`（模板方法骨架）、`DelegatingLlmChatService`（仅分发）、`AiPlatformConfig`（仅配置装配）、`AiPlatformEnvironmentPostProcessor`（仅配置转发）— 职责划分清晰，符合 SRP。

- **抽象层次恰当**：Interface 定义契约（`CapabilityExecutor`、`LlmChatService`、`DegradationStrategy` 等），Abstract class 提供公共实现（`AbstractCapabilityExecutor`），class 承载具体实现。不过度设计（未引入不必要的适配层或工厂层），也不设计不足（关键抽象均有清晰的接口定义）。

- **便于详细设计和实现**：§4 提供了完整的管线伪代码，§3 每个组件有字段表/状态模型/协作对象说明，§9.5 提供了可复用的 YAML 配置模板。实现者可直接参考。

- **可测试性**：所有关键组件均通过接口注入了依赖，可使用 `@MockBean` 模拟下游组件（见 §11）。滑动窗口、熔断器、端点健康管理器等状态机组件设计了独立的状态转换测试覆盖方法。并发竞争场景有显式的多线程测试方案（§11.5）。

- **迁移路径清晰**：§9 提供了从 Phase 2~4 到 Phase 5 的分步迁移方案，包括构造器迁移、`applyStrategies()` 剥离、接口变更影响评估、配置切换等，过渡期兼容性有明确保障。

## 修改要求

无。本设计通过全部五个维度的审查。
