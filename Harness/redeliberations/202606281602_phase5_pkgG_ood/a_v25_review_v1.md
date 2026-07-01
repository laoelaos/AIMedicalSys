# OOD 设计方案审查报告（v25）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 类型形态选择（interface / abstract class / class / enum / @Entity / static inner class）与 Java 类型系统能力完全匹配：`CapabilityExecutor<T,R>` 泛型接口、`AbstractCapabilityExecutor<T,R>` 抽象骨架类的单继承 + 多接口实现模式均为标准 Java 实践；`Class<T> inputType` 搭配 `isInstance()` 的运行时类型安全检查是应对泛型擦除的惯用方案；`@Component`/`@Bean` 注册方式与 Spring 容器兼容。继承与实现关系清晰（AbstractCapabilityExecutor implements CapabilityExecutor → 13 个子类 extends AbstractCapabilityExecutor），无多继承冲突。

### 2. 标准库与生态覆盖

**[通过]** 涉及的库能力均在 Java / Spring Boot 生态覆盖范围内：集合（`Map`/`List`/`Deque`/`ConcurrentHashMap`）为标准库；并发（`CompletableFuture`/`ThreadPoolExecutor`/`AtomicReference`/`ReentrantReadWriteLock` / `synchronized`）为标准库；持久化（Spring Data JPA + HikariCP）为 Spring Boot 标配；缓存（Caffeine）为常用库；限流（Guava RateLimiter）为常用库；指标（Micrometer + Actuator）为 Spring Boot 标配；HTTP 客户端（OkHttp / WebClient）为标准实践；Token 估算（jtokkit）为可选依赖，缺失回退方案也已定义。无超出生态范围的能力假设。

### 3. 语言特性可行性

**[通过]** 错误处理策略（checked/unchecked exception 区分、CompletableFuture.exceptionally() 回调、CompletionException 拆解、自定义异常分类 `StructuredOutputNotSupportedException`/`LlmInfrastructureException`/`Phase4BusinessException`）与 Java 异常机制完全匹配。并发设计（容器线程提取 ThreadLocal → 闭包捕获 → supplyAsync 线程池执行）是标准的异步上下文传播模式。资源管理（线程池由 Spring 容器管理生命周期、HTTP 连接池、Caffeine 缓存自动过期）均为 Java/Spring 成熟实践。模块包结构（ai-api / ai-impl 分层、orchestrator/router/client/template/experiment/metrics 等子包划分）符合 Maven 模块化组织方式。`@ConditionalOnProperty`/`@ConditionalOnClass`/`ObjectProvider`/`@Primary` 等条件装配机制均为 Spring 容器原生能力。

### 4. 设计一致性

**[通过]** 各抽象职责清晰无歧义。协作关系形成完整闭环（AiOrchestrator → CapabilityExecutor.execute() → 降级预检 → 实验分流 → 模板渲染 → 模型路由 → 健康检查 → LLM 调用 → 结构化解析 → 指标采集 → 返回 AiResult）。行为契约（§4 伪代码）完整到足以指导实现。模块间依赖方向合理（ai-api ← ai-impl，orchestrator 单向依赖下游子包），无循环依赖。迭代要求中指出的 5 个持续性问题在本轮中均已解决：(1) super() 调用参数与父类构造器签名已匹配（16 参数 v.s. 16 参数）；(2) `doDegrade` 15 参数签名已规划三期 CallContext 迁移计划；(3) §3.5 工厂方法已同时展示新旧签名表，§4.1 伪代码沿用旧签名的原因已在兼容性说明中交代；(4) `inputType` 字段已在类图和构造器中定义，`doDegrade()` 内的 null-safe 守卫已实现；(5) `KbQueryRequest` 的 `departmentId` 歧义已通过方案 A 解决（业务字段中移除，仅通过 AiRequestBase 继承）。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则（AiOrchestrator 仅做路由委托、CapabilityExecutor 持有管线、AbstractCapabilityExecutor 封装模板方法、DelegatingLlmChatService 仅做分发）。抽象层次恰当（13 个能力共用抽象骨架、子类仅特化 doExecuteInternal 差异化步骤，不过度抽取也不过于扁平）。设计便于后续实现（每能力独立子类可单独修改和测试，DTO 字段齐全、伪代码步骤详尽）。设计便于单元测试（通过 @MockBean 模拟下游依赖验证降级路径，§11.1 已列出具体测试模式）。迁移路径完整（从 Phase 2~4 到底座分批次渐进迁移，内含过渡期回退方案和风险缓解措施）。

## 注意事项

- §3.1 `thinAdapterPerCapabilityConfig` 构造器参数在 `AbstractCapabilityExecutor` 构造器参数列表中声明为 `@Qualifier("thinAdapterPerCapabilityConfig") Map<String, Duration>` 但未在类图中显式声明为字段——虽可通过 Bean 名称推断映射逻辑，建议在实现阶段确认该 Map 是否为独立 Bean 还是 `AiExecutionProperties` 的嵌套属性
