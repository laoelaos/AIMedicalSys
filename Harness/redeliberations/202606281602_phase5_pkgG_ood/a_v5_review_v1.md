# OOD 设计方案审查报告（v5）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 设计中的类型形态选择（interface / abstract class / class / enum / JPA @Entity / static inner class）均与 Java 类型系统能力匹配。

**[通过]** 继承关系在约束范围内：`AbstractCapabilityExecutor` 继承单一抽象类，13 个 `CapabilityExecutor` 实现继承 `AbstractCapabilityExecutor`；`FallbackAiService`、`AiOrchestrator`、`MockAiService` 均实现 `AiService` 接口；`LlmChatService` 由 `DelegatingLlmChatService`、`HttpApiLlmChatService`、`SpringAiLlmChatService` 分别实现——单继承 + 多接口实现模式正确。

**[通过]** 泛型使用（`CapabilityExecutor<T, R>`、`StructuredChatResult<T>`、`LocalRuleFallback<T, R>`）符合 Java 泛型能力范围。设计已显式识别并处理泛型擦除带来的运行时类型安全问题（`doDegrade()` 中 `inputType.isInstance(request)` 防御检查）。

**[通过]** 协作关系中的类型交互模式（`CompletableFuture<AiResult<T>>` 异步返回、`Map<String, CapabilityExecutor>` 按能力标识路由、`ObjectProvider` 可选依赖注入）均可在 Java + Spring 生态中实现。

**[通过]** `StructuredOutputNotSupportedException` 和 `LlmInfrastructureException` 使用 `RuntimeException` 而非 checked exception，与管线中 `CompletableFuture` 异步异常传播模型一致。

### 2. 标准库与生态覆盖

**[通过]** 所有设计所需能力均在 Java 标准库或常用库覆盖范围内：
- Spring Boot：容器（`@Component`/`@Configuration`/`@ConditionalOnProperty`）、AOP（`@Async`）、事件机制（`ApplicationEvent`/`@EventListener`）、配置绑定（`@ConfigurationProperties`）、JPA（`@Entity`/`Repository`）
- Jackson：DTO 序列化/防御性拷贝（`ObjectMapper.convertValue()`）、JSON Schema 自动生成
- Caffeine：多组件缓存（模板/实验/凭据/路由）
- Guava：端点限流（`RateLimiter` 令牌桶）
- Micrometer：性能指标采集（`Timer`/`Counter`/`DistributionSummary`）
- Reactor（可选）：流式对话（`Flux`）

**[通过]** 设计在假设库能力方面合理——`ObjectMapper.convertValue()` 要求 DTO 具有正确 Jackson 注解，设计已提供详细的兼容性说明和三种测试场景。

**[通过]** 标准库能力可简化部分自定义抽象：`DegradationStrategy` 链排序通过 `default int getOrder()` 方法而非自定义排序逻辑；`CompletableFuture.orTimeout()`（Java 9+）替代了自定义超时实现。

**轻微** §3.5 聚合 SQL 伪代码中 `PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY elapsed_ms)` 是 PostgreSQL/Oracle 方言函数，MySQL 不支持此语法。Phase 5 当前使用 MySQL 分区表，需改为 MySQL 兼容的百分位计算方式（如应用层计算或 MySQL 8.0 窗口函数替代方案）。此问题不影响设计可行性，实现在选择具体聚合方案时解决。

### 3. 语言特性可行性

**[通过]** 错误处理策略与 Java 异常机制匹配：管线内预期异常通过 `AiResult` 包装返回（非异常路径），不可预知异常在 `AiOrchestrator.handle()` 层面兜底捕获不传播为 `CompletionException`；`CompletionException` 拆解 + 按原始异常类型分类路由的设计准确。

**[通过]** 并发设计合理：
- `SlidingWindowMetricsStore` 使用 `synchronized (deque)` 保护淘汰+写入；并设计了演进路径（`ReentrantReadWriteLock`）
- `CircuitBreakerDegradationStrategy` 使用 `AtomicReference` + CAS 保证状态转换原子性
- `ModelRouter` 使用 `AtomicReference<Map>` 全量替换缓存
- 上下文传播采用"入口处提取 + 闭包捕获"模式，正确解决 `CompletableFuture.supplyAsync()` 的 ThreadLocal 丢失问题
- 线程池隔离（`llmCallExecutor` vs `metricsAsyncExecutor`）合理

**[通过]** 资源管理方案可行：连接池（HTTP/DB）、凭据缓存（Caffeine Expiry + 状态机）、数据分区清理（月分区 + `DROP PARTITION`）。

**[通过]** 模块/包结构符合 Java 项目组织方式（`ai-api` / `ai-impl` 双模块，子包按职责划分）。

### 4. 设计一致性

**[通过]** 各抽象职责描述清晰无歧义——§3 核心抽象节中每项均采用一致的"角色/职责/协作对象/类型形态选择理由"四元组格式。

**[通过]** 协作关系形成闭环：`AiService` → `AiOrchestrator` → `CapabilityExecutor` → ModelRouter/LlmChatService/PromptTemplateManager/AiMetricsCollector 等；FallbackAiService 作为装饰器包裹 AiService；13 个 AiService 方法到 capabilityId 映射表完整覆盖。

**[通过]** 行为契约描述完整——§4 提供了完整管线伪代码（`AiOrchestrator.handle()` → `AbstractCapabilityExecutor.execute()` → `doExecuteInternal()` / `doDegrade()`），覆盖了正常路径、降级路径、超时兜底、异常分类处理等所有关键路径。

**[通过]** 模块间依赖方向合理无循环依赖：`ai-impl` 依赖 `ai-api`（单向）；`ai-impl` 内 `orchestrator/` 依赖其余子包，其余子包间互不依赖；`thin-adapter` 指向 Phase 4 模块的 provided 依赖为出向依赖。

**[通过]** §1.7 实施拓扑顺序提供 7 批次清晰实施路径，P0–P3 优先级合理。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则：`AiOrchestrator`（路由委托）、`CapabilityExecutor`（管线执行）、`ModelRouter`（模型路由）、`PromptTemplateManager`（模板管理）、`ExperimentManager`（实验分流）、`AiMetricsCollector`（指标采集）、`SlidingWindowMetricsStore`（窗口数据）、`ModelEndpointHealthManager`（端点健康）、`CredentialProvider`（凭据管理）、`EndpointRateLimiter`（限流）。

**[通过]** 抽象层次恰当：`AbstractCapabilityExecutor` 模板方法模式正确封装公共步骤（降级预检、时序提取、超时兜底），子类仅需特化 `doExecuteInternal()`；设计不陷入过度细节（无具体字段类型/方法签名强制规定），也不缺失关键约束。

**[通过]** 设计便于实现——§1.7 实施批次、P0-P3 优先级、§9 迁移路径（构造器迁移 → `applyStrategies()` 迁移 → 配置切换）均提供明确指导。

**[通过]** 设计便于单元测试——§11 提供详细测试策略（单元/集成/并发/状态恢复），组件均基于 interface 可 mock，`RequestContextUtils` 为静态工具类无容器依赖。

**轻微** §3.2 `DelegatingLlmChatService` 的 Bean 装配伪代码中，`ObjectProvider<LlmChatService> springAiProvider` 缺少 `@Qualifier("springAiLlmChatService")`。当 `springAiLlmChatService` 存在时，未限定的 `ObjectProvider.getIfAvailable()` 会同时看到 `httpApiLlmChatService` 和 `springAiLlmChatService` 两个候选 Bean 而返回 null，导致 `SPRING_AI` 分发项未被设置。修复方向：在 `ObjectProvider` 参数上补充 `@Qualifier("springAiLlmChatService")` 限定，使 `getIfAvailable()` 正确识别可选 Bean。此为 Bean 装配细节，不影响设计可行性。

## 修改要求

无。设计通过审查，不存在严重或一般问题。

## 关于迭代需求的说明

审查确认 v5 版本已逐一响应前轮反馈的全部 8 个问题：
1. [已修复] 修订说明已剥离归档，正文仅保留 4 行变更摘要
2. [已修复] 全篇无 `(v{N} 新增/修正)` 类过程标记
3. [已修复] §3.11 移至 §3.10 之后，编号 3.11.1–3.11.7 连续
4. [已修复] §1.7 新增 7 批次实施拓扑顺序
5. [已修复] §1.2 列出 5 条具体风格一致性规则并标注来源
6. [已修复] §4.1 doExecuteInternal() 新增 CompletionException 拆解、AiResult.isSuccess() 状态检查
7. [已修复] §1.3 AiService/FallbackAiService 补充线程安全契约
8. [已修复] §3.11 共同约束末补充指向 §3.5 的 Jackson 兼容性测试要求
