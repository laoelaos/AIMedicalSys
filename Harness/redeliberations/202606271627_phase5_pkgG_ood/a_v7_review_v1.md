# OOD 设计方案审查报告（v1）

## 审查结果

REJECTED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 所有抽象的类型形态选择（interface / abstract class / class / @Entity / enum）均与 Java 类型系统能力匹配。

**[通过]** 继承与实现关系正确：Java 单继承限制已遵循（AbstractCapabilityExecutor 提供唯一抽象基类，13 个子类 extends），多接口实现无冲突（AiOrchestrator implements AiService，同时关联多个接口引用）。

**[通过]** 泛型使用方式在 Java 泛型系统能力范围内：`CapabilityExecutor<T, R>` 及其 template method 模式、`StructuredOutputParser.parse(LlmResponse, Class<T>)`、`Map<String, CapabilityExecutor>`（原始类型使用是异构容器场景下的 Java 惯用模式）。

**[通过]** 协作关系中的类型交互模式（CompletableFuture 异步委托、DTO 只读契约、spring `@Qualifier` 按名称注入、`ObjectProvider` 延迟解析）均在 Java/Spring 框架支持下可行。

### 2. 标准库与生态覆盖

**[通过]** 设计中使用的能力均在 Java 标准库或 Spring 生态覆盖范围内：`CompletableFuture`、`ConcurrentHashMap`、`AtomicReference`、`ThreadPoolExecutor`（标准库）；`@Component`、`@Async`、`@Qualifier`、`@PostConstruct`、`@ConditionalOnProperty`、`@Primary`、`@ConfigurationProperties`、`JPA / Hibernate`、`EnvironmentPostProcessor`、`Micrometer`、`ObjectMapper`、`SecurityContextHolder`、`RequestContextHolder`（Spring 生态）。

**[通过]** 假设的库能力合理：设计中显式声明了 `spring-boot-starter-actuator` 依赖依赖（§8），没有隐含未确认的外部依赖。

**[通过]** 标准库/框架能力运用得当：`ConcurrentHashMap` 简化并发指标存储、`CompletableFuture.supplyAsync()` 处理异步边界、`AtomicReference` 实现免锁路由表刷新、Spring Event 机制解耦模板缓存失效。

### 3. 语言特性可行性

**[通过]** 错误处理策略与 Java 能力匹配：业务异常通过 `AiResult.failure()` 返回而非 throw（异常类型设计合理），LLM 调用失败通过降级路径处理，CompletableFuture 异步异常边界在 `supplyAsync` lambda 内兜底。`AiOrchestrator.handle()` 的 try-catch 捕获不可预知异常并返回 `AiResult.failure()` 而非传播 CompletionException，做法正确。

**[通过]** 并发设计符合 Java 并发模型：`CompletableFuture.supplyAsync(llmCallExecutor)` 委托阻塞调用到专用线程池、`ConcurrentHashMap` + CAS 的无锁并发方案、`@Async` 配合独立线程池与 `DiscardPolicy` 拒绝策略、`synchronized` 仅在刷新路由表时互斥。设计中的并发边界划分清晰正确。

**[通过]** 资源管理方案在 Java/Spring 资源管理模式内可行：JPA Entity 自动管理连接、HTTP 连接池由底层客户端管理、线程池通过 Spring 声明式配置生命周期。

**[通过]** 模块/包结构设计符合 Java 项目和 Maven 模块组织方式：`ai-api`（接口/DTO 模块）← `ai-impl`（实现模块）的单向依赖方向正确；子包按功能正交划分（orchestrator / router / client / template / experiment / metrics / parser / degradation / fallback / config）；依赖规则（client 为唯一引入外部依赖的子包、template/experiment/metrics 各自拥有独立的 JPA Repository）合理。

### 4. 设计一致性

**[通过]** 各抽象的职责描述清晰无歧义：`AiOrchestrator`（路由委托）、`CapabilityExecutor`（完整管线）、`AbstractCapabilityExecutor`（模板骨架）、`LlmClient`（模型调用抽象）、`ModelRouter`（路由决策）等职责边界明确。

**[通过]** 协作关系形成闭环，无缺失环节：CapabilityExecutor 管线 8 步流程（降级预检→实验分流→模板渲染→模型路由→健康检查→LLM 调用→结果解析→指标采集）中各步骤的协作对象均已定义；薄适配器管线的简化流程也已定义。

**[通过]** 模块间依赖方向合理，无循环依赖：`ai-api ← ai-impl`，`ai-impl` 内部 `orchestrator → {router, template, experiment, client, fallback, degradation, metrics}` 单向，无反向依赖。

**[一般]** §4.1 管线伪代码中 `AiCallRecord` 工厂方法参数顺序与 §3.5 定义的显式工厂签名不一致：
- `AiCallRecord.success(capabilityId, elapsedMs, ...)` — 第 2 个可见实参为 `elapsedMs(long)`，但工厂签名第 2 参数为 `callTime(LocalDateTime)`
- `AiCallRecord.degraded(capabilityId, elapsedMs, degradeReason, ...)` — 第 2/3 实参与工厂签名参数位置错位
- `AiOrchestrator.handle()` 中 `AiCallRecord.failure(capabilityId, LocalDateTime.now(), "InternalError:..." + e.getClass().getSimpleName(), ...)` — 第 3 个实参为 `String` 但工厂签名第 3 参数为 `long elapsedMs`
虽然伪代码使用 `...` 表示省略，但可见参数序列与实际工厂签名不匹配，可能导致实现者产出编译错误或运行时行为错误的代码。
- **建议方向**：统一管线伪代码中的参数顺序与 §3.5 工厂签名一致，或明确标注省略的 `LocalDateTime.now()` 参数位置。

**[一般]** §3.1 降级策略注入机制的 `@Qualifier` Bean name 推导规则与示例不一致：
- 规则描述为 `StringUtils.uncapitalize(capabilityId).toLowerCase() + "Strategies"`
- 对 `"RX_AUDIT"` 应用此规则得到 `"rx_auditStrategies"`（含下划线），但示例声称得到 `"rxAuditStrategies"`
- 规则未说明如何处理含下划线的 `capabilityId`，导致实现者可能产出错误的 Bean name 从而使 Spring DI 在运行时失败
- **建议方向**：在推导规则中补充下划线处理步骤，如先按 `_` 切分后转小写驼峰拼接，或明确约定 `capabilityId` 均不使用下划线（如 `"RX_AUDIT"` → `"RXAUDIT"`），使规则与示例一致。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则：每个抽象有清晰且收敛的职责边界，没有混入无关职责的抽象。

**[通过]** 抽象层次恰当：`AbstractCapabilityExecutor` 的引入恰当解决了 13 个实现类中降级预检和指标采集重复的问题；同时避免了过度设计（如 Pipeline Step 链留待 Phase 6）。

**[通过]** 设计便于详细实现和测试：大多数核心抽象为 interface（可 mock），依赖通过构造注入（可隔离），`DegradationStrategy` 实现无状态（可独立测试），`SlidingWindowMetricsStore` 有清晰的读/写方法（可单元测试）。

**[轻微]** §2.3 `ModelRoute` 类图仅列出 `modelId`/`endpoint`/`clientType` 字段，但 §3.2 `ModelEndpointHealthManager` 说明中提及"由 `ModelRoute.endpointId` 标识"，且 §4.1 伪代码调用 `modelRoute.getEndpointId()`。建议在 `ModelRoute` 中补充 `endpointId` 字段定义，或明确说明 `endpoint` 字段同时作为标识符使用。

**[轻微]** §3.1 `AbstractCapabilityExecutor` 类图中声明了 `#doPreDegradeCheck()` 和 `#doRecordMetrics()` 两个 protected 方法，但 §3.1 模板方法伪代码中将降级预检循环和指标采集内联在 `execute()` 中。建议同步：要么从类图中移除这两个方法（改为内联实现），要么在模板方法伪代码中调用它们，使设计文档与类图一致。

## 修改要求（REJECTED）

### Issue 1: §4.1 管线伪代码 AiCallRecord 工厂方法参数顺序与 §3.5 签名不一致

- **问题**：`AiCallRecord.success()/degraded()/failure()` 在 §4.1 伪代码中的参数顺序与 §3.5 定义的工厂方法签名不匹配，`elapsedMs`、`callTime` 等参数错位。failure 调用中甚至将 String 值传入 `long` 参数位置。
- **原因**：伪代码使用 `...` 缩写导致参数位置不明确，实现者若照搬伪代码中的参数序列将产出编译错误，或产生 `elapsedMs` 被当作 `callTime` 使用的运行时逻辑错误。
- **建议方向**：管线伪代码中统一参数的排列顺序使其与 §3.5 工厂签名一一对应，或在伪代码注释中明确标注 `// callTime, elapsedMs, ...` 等参数语义。

### Issue 2: §3.1 @Qualifier Bean name 推导规则与示例不一致

- **问题**：`StringUtils.uncapitalize(capabilityId).toLowerCase() + "Strategies"` 规则对 `"RX_AUDIT"` 产生 `"rx_auditStrategies"`，但示例声称 `"rxAuditStrategies"`；规则未定义含下划线的 `capabilityId` 如何处理。
- **原因**：Java 变量名和 Spring Bean name 惯用驼峰命名，含下划线的 Bean name 虽合法但极易因拼写错误导致 DI 失败。规则与示例的二义性使实现者无法确定正确行为。
- **建议方向**：方案 A — 在规则中添加下划线处理步骤（如按 `_` 切分 → 各段转小写 → 驼峰拼接），确保 `"RX_AUDIT"` → `"rxAuditStrategies"`；方案 B — 约定 `capabilityId` 不使用下划线（如 `"RX_AUDIT"` 改为 `"RX_AUDIT"` → `"RXAUDIT"` 或 `"PRESCRIPTION_AUDIT"` → `"PRESCRIPTION_AUDIT"` 使用完整驼峰 `"PrescriptionAudit"`），简化推导规则。
