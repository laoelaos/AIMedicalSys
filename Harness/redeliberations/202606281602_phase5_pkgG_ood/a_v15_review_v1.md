# OOD 设计方案审查报告（v15）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 设计中使用的全部类型形态（interface / abstract class / class / enum / JPA @Entity / static inner class）均与 Java 类型系统完全匹配。

**[通过]** 单继承 + 多接口实现的约束被正确遵循：`AbstractCapabilityExecutor` 作为单一抽象基类（abstract class），13 个子类 `extends AbstractCapabilityExecutor`；`AiService` 作为 interface 被 `AiOrchestrator`、`FallbackAiService`、`MockAiService` 各自 implements。

**[通过]** 泛型使用方式在 Java 泛型系统能力范围内：`CapabilityExecutor<T,R>` 的 `T`/`R` 类型参数用于方法签名和构造器注入的 `Class<T>` 字段，类型擦除后在 `doDegrade()` 中通过 `inputType.isInstance(request)` 运行时检查补偿。

**[通过]** 协作关系中描述的类型交互模式均可在 Java 中实现：`Map<String, CapabilityExecutor>` 路由查找、`CompletableFuture` 异步组合、`@Async` 线程池委派、`instanceof` 安全类型检测。

### 2. 标准库与生态覆盖

**[通过]** 设计依赖的标准库选型合理：
- Spring Boot / Spring Data JPA — 容器基础与持久化
- Spring Boot Actuator + Micrometer — 指标体系
- Caffeine — 模板/实验/凭据三级缓存
- Guava `RateLimiter` — 端点令牌桶限流
- Jackson — DTO 序列化与防御性拷贝
- Reactor Core（条件性）— 流式 `LlmChatStreamService` 的 `Flux` 返回
- OkHttp / WebClient（条件性）— HTTP API 客户端

**[通过]** 对库能力的假设合理：Caffeine 的 `Expiry` 接口用于凭据缓存的动态 TTL；Guava 的 `RateLimiter` 线程安全且支持 `tryAcquire(timeout)`。

**[通过]** 标准库能力对设计中的自定义抽象覆盖充分——无需要引入额外库来简化的自定义抽象。

### 3. 语言特性可行性

**[通过]** 错误处理策略与 Java 异常体系匹配：
- `RuntimeException` 体系（`StructuredOutputNotSupportedException`、`LlmInfrastructureException`、`ParseException`）
- `CompletableFuture.exceptionally()` 回调处理超时
- catch 块按异常类型分类路由（`StructuredOutputNotSupportedException` → 回退，`LlmInfrastructureException` → 降级）
- Phase 4 模块异常通过类名匹配识别（`DiagnosisException` 等 6 个已知类型）

**[通过]** 并发设计兼容 Java 并发模型：
- `CompletableFuture.supplyAsync()` + 共享线程池（`llmCallExecutor`）
- `ConcurrentHashMap` + `synchronized` 块（`SlidingWindowMetricsStore` 写锁）
- `AtomicReference` + CAS（`CircuitBreakerDegradationStrategy` 熔断状态转换）
- `@Async` + `DiscardPolicy`（`AiMetricsCollector` 异步写入隔离）
- `@Scheduled` + `ThreadPoolTaskScheduler`（定时任务线程池隔离）

**[通过]** 资源管理方案在 Java 资源管理模式内可行：
- HikariCP 连接池（配置化最大连接数）
- Caffeine 缓存（maximumSize + expireAfterWrite + Expiry 接口）
- 线程池（`CallerRunsPolicy` 自然背压 / `DiscardPolicy` 降级保活）

**[通过]** 模块/包结构符合 Maven 多模块项目惯例：
- `ai-api`（接口/DTO 模块，零实现依赖）
- `ai-impl`（实现模块，子包按职责分层：orchestrator/client/template/experiment/metrics/parser/fallback/degradation/config）
- `provided` 作用域管理薄适配器对 Phase 4 模块的编译期依赖

### 4. 设计一致性

**[通过]** 各抽象的职责描述清晰无歧义：
- `AiOrchestrator`：路由委托，不介入管线内部步骤
- `CapabilityExecutor`：单项能力完整管线持有者
- `AbstractCapabilityExecutor`：模板方法封装降级预检+超时+指标，子类仅特化 `doExecuteInternal()`
- `DelegatingLlmChatService`：按 `ClientType` 分发，不包装异常

**[通过]** 协作关系形成闭环：
- AiOrchestrator → CapabilityExecutor (via Map lookup)
- CapabilityExecutor → {ModelRouter, LlmChatService, PromptTemplateManager, ExperimentManager, StructuredOutputParser, AiMetricsCollector, SlidingWindowMetricsStore, ModelEndpointHealthManager, LocalRuleFallback, DegradationStrategy}
- DelegatingLlmChatService → {HttpApiLlmChatService, SpringAiLlmChatService} (via Map dispatch)

**[通过]** 依赖方向单向无循环：
- `orchestrator/` 为顶层编排，依赖所有其余子包
- 其余子包间无相互依赖
- `thin-adapter/` 出向依赖 Phase 4 模块（不产生循环依赖）

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则：每个组件聚焦一个功能域（编排/执行/路由/模板/实验/指标/健康/限流/降级/解析/回退）。

**[通过]** 抽象层次恰当：
- 架构级设计，不包含具体字段声明与方法体实现细节（如 `LlmChatService` 接口仅定义 `chat()`/`structuredChat()` 签名不含 HTTP 实现）
- 模板方法模式（`execute()` → `doExecuteInternal()`）提供可扩展骨架
- 设计决策表（§7）记录了 30+ 项关键决策的理由与权衡

**[通过]** 设计便于后续详细设计和实现：
- 新增能力 = 新增 `CapabilityExecutor` 实现 + DTO，自动注册
- 7 项底座能力的特化设计已逐项给出字段/策略/路由配置指引（§3.11）
- 实施拓扑顺序清晰（§1.7，分 7 批次）

**[通过]** 设计便于单元测试：
- 所有核心接口可 `@MockBean`（§11.1）
- 覆盖降级预检/超时/端点不可用/路由不可用/解析失败/模板异常/实验异常全路径
- 并发竞争测试覆盖滑动窗口/熔断器/端点健康/CompletableFuture 竞态（§11.5）
- 配置热刷新集成测试（§11.6）

**[通过]** [轻微] `AbstractCapabilityExecutor` 构造器包含 13 个参数。设计文档已在 §3.1 明确记录此约束并提出 Phase 6 或实施期抽取 `ExecutionContext`/`CallRecordContext` 上下文对象聚合的优化方向。此问题不阻塞通过。

## 对照迭代需求验证

已验证 v15 完整响应了迭代需求（a_v15_iteration_requirement.md）中列出的全部问题：

| 问题 | 严重度 | v15 响应 |
|------|-------|---------|
| 1.1 非功能性维度分析缺失 | 重要 | 新增 §1.8 完整章节 ✓ |
| 2.1 DEGRADED 双重计数 | 严重 | §4.1 移除 failure() 预记 ✓ |
| 2.2 ClientType 静默回退 | 重要 | §1.3 双重加固+ERROR 日志+告警 ✓ |
| 3.1 线程隔离未定稿 | 重要 | §3.9 新增 transcriptSummaryExecutor Bean + §3.11.7 定稿 ✓ |
| 3.2 DTO 工作量未量化 | 中等 | §3.5 新增工作量概览表 ✓ |
| 3.3 冷启动效应未分析 | 中等 | §1.8.1 冷启动分析+预热建议 ✓ |
| 3.4 parse() 超时硬编码 | 轻微 | §4.1 改为可配置 + §9.5 配置项 ✓ |
