# OOD 设计方案审查报告（v21）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 设计中的类型形态选择（interface / abstract class / class / enum / sealed-like exception hierarchy / JPA @Entity / value object）与 Java 类型系统完全匹配：

- `CapabilityExecutor<T,R>` / `AbstractCapabilityExecutor<T,R>` 泛型使用方式正确，泛型边界在 Java 泛型系统能力范围内（无通配符误用、无自引用泛型）
- 抽象继承关系符合 Java 约束：abstract class 单继承（各 `XxxCapabilityExecutor extends AbstractCapabilityExecutor`）、interface 多实现（`DegradationStrategy` 实现可同时实现其他接口）、enum 不可继承但设计无此需求
- 协作关系中描述的 `instanceof` 类型检测（`Phase4BusinessException`、`Phase4ServiceMetaCapable`）在 Java 中完整可行
- 不可变值对象（`CallContext`、`ModelRoute` 等）使用 `final` 字段 + Builder 模式的约定符合 Java 最佳实践
- 异常分类体系（`StructuredOutputNotSupportedException` / `LlmInfrastructureException` / `Phase4BusinessException`）继承自 `RuntimeException`，类型层次合理

### 2. 标准库与生态覆盖

**[通过]** 设计中需要的所有能力均在 Java/Spring Boot 生态的标准库覆盖范围内：

- Spring Framework 核心（IoC、AOP、`@Async`、`@Scheduled`、`EventListener`/`ApplicationEvent`、`EnvironmentPostProcessor`、`@ConditionalOnProperty/Class`）
- Spring Data JPA + Hibernate ORM（Entity、Repository、查询、分区、索引）
- Jackson（DTO 序列化/反序列化、防御性拷贝 `ObjectMapper.convertValue()`、JSON Schema 生成）
- Caffeine（模板/实验/凭据缓存，含 `Expiry` 接口细粒度 TTL 控制）
- Guava `RateLimiter`（令牌桶限流）
- Micrometer（Timer / Counter / DistributionSummary / Gauge 指标采集，含 `MeterRegistry` 注册）
- Reactor Core（`Flux` 流式输出，`LlmChatStreamService` 接口引用）
- Spring Security（`SecurityContextHolder`、`Authentication` 提取）
- CompletableFuture（异步管线、`orTimeout()`、`exceptionally()`）
- Java 并发工具（`ConcurrentHashMap`、`AtomicLong/AtomicReference`、`ThreadPoolExecutor`、`ReentrantReadWriteLock` 演进路径提及）
- JPA 分页/排序（`Pageable`、`Sort`、`@Query` 聚合查询）
- 所有依赖均在 §8 清单中列出，作用域和可选性明确标注，假设合理

### 3. 语言特性可行性

**[通过]**

- **错误处理**：使用 `instanceof` 按异常类型分类路由（`StructuredOutputNotSupportedException` 回退、`LlmInfrastructureException` 降级），`CompletionException` 拆解 + `TimeoutException` 检测，`Phase4BusinessException` + 包路径前缀回退两阶段判定——全部在 Java 异常处理能力范围内
- **并发模型**：`CompletableFuture.supplyAsync()` 异步管线 + 专用线程池隔离（`llmCallExecutor` / `metricsAsyncExecutor` / `transcriptSummaryExecutor` / `scheduledTaskExecutor`），`AtomicReference` CAS 状态机（熔断器 CLOSED/OPEN/HALF_OPEN），`synchronized` 保护 `Deque` 操作——全部是 Java 标准并发手段
- **资源管理**：有界线程池（`LinkedBlockingQueue` + 拒绝策略）、HikariCP 连接池评估、滑动窗口内存估算（~9.2 MB）、数据库按月分区清理——方案在 Java Heap 和连接池约束下完全可行
- **模块/包结构**：Maven 模块 `ai-api` ← `ai-impl`，`ai-impl` 内部按功能子包（`orchestrator/`、`router/`、`client/`、`template/`、`experiment/`、`metrics/`、`parser/`、`fallback/`、`degradation/`、`config/`），依赖方向清晰，无循环依赖

### 4. 设计一致性

**[通过]**

- 各抽象职责描述清晰：`AiOrchestrator`（路由编排）/ `CapabilityExecutor`（单项能力管线）/ `AbstractCapabilityExecutor`（模板方法骨架）/ `DelegatingLlmChatService`（客户端分发）等均有明确边界
- 协作关系形成闭环：`AiOrchestrator` → `CapabilityExecutor.execute()` → `doExecuteInternal()`（实验分流→模板渲染→模型路由→健康检查→LLM调用→结构化解析→指标采集）→ `doDegrade()` 降级兜底，无缺失环节
- 行为契约完整：§4.1（统一调用管线伪代码）、§4.2（薄适配器特化管线）、§4.3（模型路由契约）、§4.4（A/B实验分流契约）、§4.5（Prompt模板渲染契约）、§4.6（结构化输出解析契约）、§4.7（性能指标采集契约）——契约覆盖所有关键管线步骤
- 模块间依赖方向合理：`ai-api ← ai-impl`，`ai-impl` 内部 `orchestrator/` 依赖其余子包，其余子包互不依赖，无循环依赖
- v21 已修复迭代需求中识别的 6 个问题（@RefreshScope 滑动窗口丢失、Phase4BusinessException 过渡期兼容、LocalRuleFallback null 保护、Spring AI 包路径、超时原因二义性、工厂方法 16 参数）

### 5. 设计质量

**[通过]**

- **单一职责**：各项抽象职责清晰收敛——`AiOrchestrator` 只做路由委托、`AbstractCapabilityExecutor` 封装公共骨架、`DelegatingLlmChatService` 只做客户端分发、`ModelRouter` 只做路由决策、`CredentialProvider` 只做凭据查询
- **抽象层次恰当**：模板方法模式（`execute()` final + `doExecuteInternal()` 可重写）在强制公共步骤（降级预检不可绕过）和允许子类特化之间取得平衡；设计粒度足以指导实现但不过度抽象
- **便于实现**：13 个 `CapabilityExecutor` 子类仅需特化 `doExecuteInternal()` + 变量提取策略即可完成新能力接入，实施拓扑顺序（7 批次）明确
- **便于单元测试**：依赖注入 + 接口隔离使 `@MockBean` 可模拟所有下游依赖；§11 单元测试模式覆盖 7 条降级路径触发条件 + 并发竞争（5 场景）+ 状态恢复路径（5 组件 6 路径）+ 配置热刷新（4 场景）——可测试性设计充分

## 修改要求

无（已批准）
