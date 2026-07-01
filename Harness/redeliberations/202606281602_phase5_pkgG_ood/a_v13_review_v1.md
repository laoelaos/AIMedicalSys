# OOD 设计方案审查报告（v13）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 设计中所有类型形态选择（interface / abstract class / class / enum / JPA @Entity）均与 Java 类型系统能力匹配。`CapabilityExecutor<T,R>`、`StructuredChatResult<T>`、`LocalRuleFallback<T,R>` 等泛型抽象的使用方式在 Java 泛型能力范围内。继承关系（单继承 + 多接口实现）遵守 Java 约束：`AbstractCapabilityExecutor` 作为单一抽象基类，各 CapabilityExecutor 实现通过 `extends` 继承；多个接口（`AiService`、`ApplicationContextAware` 等）通过 `implements` 组合。

**[通过]** 协作关系中描述的类型交互模式（CompletableFuture 异步管线、Bean 注入、事件驱动缓存刷新）均可在 Java 中实现。枚举类型（`ClientType`、`AuthType`、`LlmChatMessageRole`、`DegradationReason`）提供编译期类型安全。

**[轻微]** `LocalRuleFallback<T,R>` 在 `doDegrade()` 中的调用因泛型擦除存在编译期 unchecked 转换风险。设计已识别此风险并通过 `inputType.isInstance(request)` 运行时类型检查 + ClassCastException 兜底防御，建议在实现阶段补充单元测试覆盖类型不匹配场景。

### 2. 标准库与生态覆盖

**[通过]** 设计中引用的所有库能力均在标准 Java 和 Spring Boot 生态内：Spring Data JPA（持久化）、Jackson（序列化/防御性拷贝）、Caffeine（缓存）、Guava RateLimiter（限流）、Micrometer（指标）、OkHttp/WebClient（HTTP 调用）、Reactor Core（流式 Flux，可选）、Spring AI（可选）。无依赖空洞或假设性库引用。

**[通过]** `@ConfigurationProperties` 拆分（AiRouterProperties / AiExecutionProperties / AiDegradationProperties 等 8 个独立配置类）职责清晰，与 Spring Boot 推荐实践一致。

**[通过]** `EnvironmentPostProcessor` 通过 `META-INF/spring.factories` 注册是 Spring 框架的标准 SPI 机制。

### 3. 语言特性可行性

**[通过]** 错误处理策略（CompletableFuture 异步返回 + AiResult.success/failure/degraded 包装模式）与 Java 异步编程模型匹配。预期异常通过 AiResult 而非传播路径处理，非预期异常由 try-catch 兜底后转换为 AiResult.failure()，设计合理。

**[通过]** 并发设计（CompletableFuture.supplyAsync + 专用线程池分离、ConcurrentHashMap + synchronized 锁协议、AtomicReference CAS 状态转换）均为标准 Java 并发原语，与 JVM 内存模型兼容。

**[通过]** 资源管理方案（线程池 CallerRunsPolicy / DiscardPolicy 拒绝策略、超时兜底 orTimeout、防御性拷贝）在 Java 资源管理模式下可行。

**[通过]** 模块/包结构（ai-api / ai-impl 分层，ai-impl 内部 orchestrator/client/metrics/template/experiment 等子包）符合 Maven 多模块项目组织方式。

**[轻微]** `@RefreshScope`（SlidingWindowMetricsStore 的备选热加载方案）需引入 `spring-cloud-context` 依赖，但 Maven 依赖清单（§8）未包含此项。当前自定义定时刷新为主方案、`@RefreshScope` 为回退，建议在 §9.5 对应 YAML 注释或 §8 条件性依赖清单中补充说明此依赖前提。

### 4. 设计一致性

**[通过]** 各抽象的职责描述清晰无歧义。核心抽象（AiOrchestrator / CapabilityExecutor / AbstractCapabilityExecutor / LlmChatService / ModelRouter / PromptTemplateManager / ExperimentManager / AiMetricsCollector）的职责边界明确，协作关系形成闭环。

**[通过]** 模块间依赖方向合理（ai-api ← ai-impl，ai-impl 内部 orchestrator 为顶层编排依赖其余子包），无循环依赖。

**[通过]** 行为契约的描述完整到足以指导后续实现。§4 的伪代码详细程度高，覆盖了正常管线、降级路径、薄适配器特化等全部流程。

**[通过]** v13 要求的 4 项迭代变更（薄适配器分析缺口补齐、实验分流哨兵 groupId、配置热刷新机制、千分比决策理由）均已正确定位到对应章节。

**[轻微]** `Phase4ServiceMetaProvider` 接口定义于 §3.1，但其不在 §1.3 核心抽象一览表中列出，也未出现在 §2.3 类图中。建议在 §1.3 和 §2.3 中补充此可选契约接口（标注为薄适配器特有，非核心管线组件），保持设计文档各章节的一致性。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则。每个抽象有明确的职责边界（AiOrchestrator=路由委托、CapabilityExecutor=管线执行、LlmChatService=LLM 调用抽象、ModelRouter=模型路由选择等）。

**[通过]** 抽象层次恰当。模板方法模式（AbstractCapabilityExecutor 的 execute() 固定骨架 + doExecuteInternal() 子类特化）既不过度抽象也不设计不足，13 个实现类的公共代码得到复用。

**[通过]** 设计便于后续详细设计和实现。实施拓扑顺序（§1.7）按依赖关系分 7 个批次排列，每批次的验收标准明确。

**[通过]** 设计便于单元测试。§11 提供了完整的测试策略覆盖单元测试、集成测试、并发竞争测试、配置热刷新测试、状态恢复路径验证，充分考虑了可 mock、可隔离。

## 修改要求（REJECTED 时存在）

无
