# 质量审查报告 — Phase 5 包 G OOD 设计文档（v5）

## 审查概述

**审查范围**：`a_v5_copy_from_v4.md`（第 5 轮迭代产出）  
**审查视角**：需求响应充分度、事实准确性、逻辑一致性、深度与完整性（侧重内部审议未充分覆盖的维度）  
**审查方法**：对照实际代码库验证事实声明，分析逻辑链条完整性，评估落地可实施性  

---

## 严重问题

### Q1. `AiResult.error()` 工厂方法不存在

- **问题描述**：§4.1 `AiOrchestrator.handle()` 伪代码第 1042 行调用 `AiResult.error("AI服务暂时不可用，请稍后重试")`，但当前代码库 `AiResult.java` 仅定义了 `success(T)`, `failure(String)`, `degraded(String)` 三个静态工厂方法，不存在 `error(String)` 方法。这不是未来新增计划——设计文档中未在任何地方（包括 §3.5 AiResult 定义或 §7 设计决策）声明需要为 `AiResult` 新增 `error()` 工厂方法，属于对现有 API 的事实性引用错误。
- **所在位置**：§4.1（第 1042 行伪代码）；外部证据：`AiResult.java:23-32`
- **严重程度**：严重
- **改进建议**：在 §1.3 核心抽象表或 §3.5 中显式新增 `AiResult.error(String message)` 工厂方法的定义，或在所有伪代码中统一替换为 `AiResult.failure("SERVICE_UNAVAILABLE")`，并在 §7 设计决策中记录此变更。

### Q2. §4.1 伪代码返回类型与方法签名不一致

- **问题描述**：`CapabilityExecutor.execute()` 方法签名声明返回 `CompletableFuture<AiResult<R>>`（§3.1），但 §4.1 第 1081 行直接 `return AiResult.success(parsedResult)`、第 1094 行 `return AiResult.degraded(degradeReason)`，返回值类型是裸 `AiResult` 而非 `CompletableFuture`。实际代码 `AiService` 接口全部方法返回 `CompletableFuture`。虽然 §3.2 说明"异步边界在 CapabilityExecutor 层面"，但作为关键行为契约的伪代码展示错误的返回类型会误导实现者——若严格按照伪代码实现，编译无法通过。
- **所在位置**：§4.1 第 1081、1090、1094 行
- **严重程度**：严重
- **改进建议**：伪代码中所有 `return AiResult.success/degraded(...)` 改为 `return CompletableFuture.completedFuture(AiResult.success/degraded(...))`，或在伪代码开头注释明确"实际实现由 supplyAsync 包装"。

### Q3. Experiment PAUSED 状态语义与实际实现机制矛盾

- **问题描述**：§3.4 Experiment 状态模型定义 PAUSED 语义为"不再分流新流量；已分配的会话继续按实验分组执行"。但 §4.3 `ExperimentManager.assign()` 的实现是无状态的哈希分桶（`userId/sessionId 哈希值 % 1000`），不存在任何"已分配会话"的持久化状态记录。PAUSED 后无法区分"新流量"和"已分配的会话"——所有调用都走同样的哈希逻辑。此语义在当前架构下不可实现。
- **所在位置**：§3.4 Experiment 状态模型定义 vs §4.3 ExperimentManager.assign() 伪代码
- **严重程度**：严重
- **改进建议**：(a) 修正 PAUSED 语义为"暂停分流，所有流量回退到默认模型（分组=default）"，或 (b) 引入实验分配记录表持久化每次分流结果以支持"已分配会话继续"语义。Phase 5 建议采用方案 (a)，方案 (b) 的存储和查询成本过高。

### Q4. CapabilityExecutor 执行管线实际为同步阻塞，与异步契约矛盾

- **问题描述**：§3.1 和 §3.2 声明"CapabilityExecutor 使用 CompletableFuture.supplyAsync() 包装以对齐异步返回契约"、"确保不阻塞 Tomcat 容器线程"。但 §4.1 伪代码中，管线步骤（降级预检→模板渲染→实验分流→模型路由→健康检查→LLM 调用→结果解析）全部在 `execute()` 方法内顺序同步执行。这意味着 `execute()` 方法本身在 supplyAsync 提交到线程池之前就已经执行了所有步骤——supplyAsync 的任务反而是"包装已同步执行完毕的结果"。实际上，要么整个管线在 supplyAsync 中执行（确保不阻塞调用者线程），要么 `execute()` 是同步方法但调用端用 supplyAsync 包装。设计文档未明确实际异步边界在哪一层。
- **所在位置**：§3.1 CapabilityExecutor 异步说明 + §3.2 LlmClient 同步契约 + §4.1 伪代码整体结构
- **严重程度**：严重
- **改进建议**：明确异步策略为"`CapabilityExecutor.execute()` 内部将 LLM 调用步骤（步骤 5~7）通过 supplyAsync 委派到线程池，其余步骤在调用者线程同步执行"，或改为"整个管线通过 supplyAsync 提交到线程池执行，`execute()` 立即返回 CompletableFuture"。建议在 §4.1 伪代码中体现 supplyAsync 的调用边界。

### Q5. FallbackAiService 构造器迁移路径缺失

- **问题描述**：§3.1 Bean 装配策略要求 `FallbackAiService` 使用 `@Primary` + `ObjectProvider<AiService>` 延迟解析，但现有代码 `FallbackAiService.java:50-53` 使用 `List<AiService>` 构造注入并过滤掉自身实例。§9.2 的迁移路径仅覆盖了 `applyStrategies()` 方法的剥离，未涉及构造器从 `List<AiService>` 到 `ObjectProvider<AiService>` 的改造。直接叠加 `@Primary` 到现有构造方式上会导致：被过滤后的列表可能包含 `AiOrchestrator` 和 `MockAiService` 两个实例（当条件同时满足时），违反"只有一个非装饰器 AiService 实现有效"的假设。
- **所在位置**：§3.1 Bean 装配策略 vs §9.2 迁移路径；外部证据：`FallbackAiService.java:50-53`
- **严重程度**：严重
- **改进建议**：在 §9.2 迁移路径表格中补充第 0 步：将 FallbackAiService 构造器从 `List<AiService> aiServiceList` 改为 `ObjectProvider<AiService> aiServiceProvider`，同步修改内部过滤逻辑。同时评估是否保持 `List<DegradationStrategy>` 注入方式或也改为 ObjectProvider。

---

## 重要问题

### Q6. @Qualifier 命名约定不一致

- **问题描述**：§3.1 第 597 行描述降级策略注入方式为"通过 Spring `@Qualifier("capabilityId")` 按能力标识注入"，但第 611 行描述为"`CapabilityExecutor` 实现使用 `@Qualifier("triageStrategies")` 注入对应策略列表"。一处按原始 capabilityId 字符串作为 qualifier value，一处按"能力名+Strategies"模式。实现者无法确定正确用法。且第 608-610 行 `@Bean("triageStrategies")` / `@Bean("rxAuditStrategies")` 的 Bean name 与 capabiltyId 也无直接映射规则。
- **所在位置**：§3.1 第 597 行、第 608-611 行
- **严重程度**：重要
- **改进建议**：统一约定——YAML 中策略列表 bean 的命名模式为 `{capabilityId}Strategies`（如 `TRIAGEStrategies`），CapabilityExecutor 使用 `@Qualifier("{capabilityId}Strategies")` 注入。在 §3.1 中明确此命名规则并在配置示例中展示。

### Q7. 薄适配器型 CapabilityExecutor 缺少委托调用异常处理

- **问题描述**：§3.1 薄适配器伪代码中 `phase4ServiceDelegate.execute(request)` 无 try-catch 保护。若 Phase 4 业务服务抛出运行时异常（数据库连接失败、服务不可用、业务校验异常等），该异常将传播到 `AiOrchestrator.handle()` 的 catch 块（§4.1 第 1035 行），被统一捕获为 `InternalError` 并记录。但 Phase 4 服务异常可能是业务层面的预期异常（如参数校验失败）而非基础设施故障，不应混为一谈。且当前 `handle()` 的 catch 块中对异常仅记录并返回 `AiResult.error()`（该函数本身不存在，见 Q1），丢弃了异常中的业务语义。
- **所在位置**：§3.1 薄适配器伪代码（第 641 行）；§4.1 handle() 伪代码第 1035-1042 行
- **严重程度**：重要
- **改进建议**：薄适配器伪代码中包裹 try-catch：Phase 4 业务异常包装为 `AiResult.failure(bizErrorCode)` 返回，基础设施异常才传播或记录为 `InternalError`。同时修改 handle() 的异常处理策略以区分异常来源。

### Q8. AiOrchestrator 协作对象遗漏 AiMetricsCollector

- **问题描述**：§4.1 `AiOrchestrator.handle()` 伪代码第 1040 行调用 `metricsCollector.record()`，但 §3.1 AiOrchestrator 的协作对象列表中仅列出 `Map<String, CapabilityExecutor>`、`SlidingWindowMetricsStore`、`ModelEndpointHealthManager`，未包含 `AiMetricsCollector`。§2.3 类图中 AiOrchestrator 也没有关联到 AiMetricsCollector。这导致实现者不确定 handle() 中的 metricsCollector 从哪里注入。
- **所在位置**：§3.1 AiOrchestrator 协作对象段落；§2.3 类图 AiOrchestrator 区域
- **严重程度**：重要
- **改进建议**：在 §3.1 AiOrchestrator 协作对象中补充 `AiMetricsCollector`，在 §2.3 类图中补充 `AiOrchestrator --> AiMetricsCollector : uses` 依赖关系。

### Q9. `DegradationStrategy` 新增 `getOrder()` default method 的兼容性说明不足

- **问题描述**：设计文档在 §2.3 类图、§3.1、§3.8、§4.1 多处声明 `DegradationStrategy` 有一个 `default int getOrder() { return 0; }` 方法，但现有代码库中 `DegradationStrategy.java` 只定义了 `boolean shouldDegrade(DegradationContext context)`。虽然 Java 8 中为接口新增 default method 是二进制兼容的，但这对 `ai-api` 模块构成了接口变更。设计文档未在任何位置（包括 §7 设计决策表或 §9 迁移路径）记录此变更及对现有实现（`NoOpDegradationStrategy`）的影响评估。
- **所在位置**：§2.3 类图 DegradationStrategy 定义；§3.1 第 953 行；§3.8 第 963 行；外部证据：`DegradationStrategy.java:3-6`
- **严重程度**：重要
- **改进建议**：在 §7 设计决策表中补充"DegradationStrategy 接口扩展行"：记录新增 `default getOrder()` 的动机、影响范围（3 个现有实现：NoOp、Timeout、CircuitBreaker）和向后兼容性保证。在 §9 中补充接口变更的编译验证步骤。

### Q10. AiCallLogEntity 和 AiCallRecord 缺少 `departmentId` 字段

- **问题描述**：`AiRequestBase` 中包含 `departmentId` 字段（§3.5），Prompt 模板按科室维度检索需要 `departmentId`（§4.4），这是一个重要的分析维度。但 `AiCallLogEntity` 和 `AiCallRecord` 的字段列表中均无 `departmentId`，导致无法按科室维度分析 AI 调用日志（如"内科科室的 AI 调用降级率"）。索引策略中也无科室维度索引。
- **所在位置**：§3.5 AiCallRecord 字段表（第 790-811 行）、AiCallLogEntity 字段表（第 855-877 行）
- **严重程度**：重要
- **改进建议**：在 `AiCallRecord` 和 `AiCallLogEntity` 中补充 `departmentId`（`String`，可为空），在表索引策略中补充 `idx_department_call_time` 覆盖索引。

### Q11. YAML 策略配置到 Bean 装配路径中存在初始化时序风险

- **问题描述**：§3.1 第 604-607 步描述的装配流程中，`AiPlatformConfig` 的 `@Bean` 方法内部调用 `ApplicationContext.getBeansOfType(DegradationStrategy.class)` 按名称查找策略 Bean。但 `@Bean` 方法执行时 Spring 容器可能尚未完成所有 `DegradationStrategy` 实现的 Bean 创建（尤其当存在 `@DependsOn` 或循环依赖时）。设计文档未评估此时序风险及缓解措施（如使用 `@Lazy`、`ObjectProvider`、或 `BeanFactoryPostProcessor` 等替代方案）。
- **所在位置**：§3.1 第 604-611 行"YAML 配置到 Bean 引用的装配路径"
- **严重程度**：重要
- **改进建议**：在 §3.1 中补充时序风险评估，建议改为 `ApplicationContext.getBeansOfType()` 在 `@PostConstruct` 阶段执行（而非 `@Bean` 方法中），或使用 `ListableBeanFactory` 的延迟查找机制。

### Q12. 文档标题版本号与实际迭代轮次不符

- **问题描述**：文件首行标题为 `（v3）`（第 1 行），但修订历史中包含从 v2 到 v6 的 7 轮修订，实际迭代轮次为第 5 轮。版本标识混乱会导致文档引用和版本追溯困难——例如开发者在阅读时引用"v3 设计"但实际内容已迭代至 v5/v6 标准。
- **所在位置**：第 1 行标题
- **严重程度**：重要
- **改进建议**：修正标题版本号为 `（v5）` 或 `（修订版 v6）`，与文件路径 `a_v5_copy_from_v4.md` 和最新修订说明 `修订说明（v6）` 保持一致。

---

## 中等/一般问题

### Q13. SlidingWindowMetricsStore 窗口时间配置在 YAML 示例中缺失

- **问题描述**：§3.5 第 892 行说明"时间窗口可配置，默认 60 秒"，但 §9.3 的 YAML 配置示例中没有 `sliding-window-seconds` 或类似的配置项。实现者无法从文档中找到可配置的参数路径。
- **所在位置**：§3.5 第 892 行 vs §9.3 配置示例
- **严重程度**：中等
- **改进建议**：在 §9.3 YAML 示例中补充 `sliding-window-seconds: 60` 配置项，或注明该配置由 `SlidingWindowMetricsStore` 构造器参数提供。

### Q14. ModelRouter 运行时刷新触发机制未定义

- **问题描述**：§6.1 第 1197 行声明路由表"支持运行时刷新"，并给出了全量替换的线程安全策略（`AtomicReference<Map>`），但未定义刷新触发的具体机制——是定时轮询数据库/配置文件？管理端 API 调用事件通知？还是 Spring Cloud Config 的 RefreshScope？缺失触发机制定义使线程安全设计成为不可执行的文档。
- **所在位置**：§6.1 第 1197 行
- **严重程度**：中等
- **改进建议**：在 §3.2 ModelRouter 或 §6.1 中补充刷新触发机制的选型，建议 Phase 5 采用管理端 API + Spring ApplicationEvent 发布刷新事件，`DefaultModelRouter` 监听事件后执行全量替换。

### Q15. `getFailureRate()` 与 `getEffectiveFailureRate()` 使用场景未定义

- **问题描述**：§3.5 `SlidingWindowMetricsStore` 定义了 `getFailureRate()`（不含 degraded 计数）和 `getEffectiveFailureRate()`（含 degraded 计数）两个方法，§3.8 `CircuitBreakerDegradationStrategy` 使用 `getFailureRate()`。但设计文档未说明何时使用 `getEffectiveFailureRate()`，也没有在任何策略实现中引用该方法。该方法在当前设计中成为死代码。
- **所在位置**：§3.5 第 905-909 行
- **严重程度**：中等
- **改进建议**：删除 `getEffectiveFailureRate()` 或在 §5 错误处理或 §3.8 中补充其使用场景（如运维告警阈值评估）。

### Q16. `inputSummary` 通过 `toString()` 截断存在敏感信息泄露风险

- **问题描述**：§3.5 第 817 行定义 `inputSummary` 的填充策略为"取业务请求 DTO 的 toString() 截断（前 500 字符）"。但患者就诊请求（如 DiagnosisRequest）的 `toString()` 可能包含患者姓名、身份证号、诊断详情等敏感信息，直接记录到 `ai_call_log` 表可能导致合规风险（如 HIPAA/个人信息保护法）。设计文档未评估此风险。
- **所在位置**：§3.5 AiCallRecord 字段填充策略
- **严重程度**：中等
- **改进建议**：增加脱敏处理步骤——在截断前通过注解或白名单机制过滤敏感字段，或在 `AiRequestBase` 中定义 `toSafeString()` 抽象方法供各 DTO 实现。

---

## 整体评价

该文档经 4 轮内部审议后已大幅改进，整体结构完整、抽象层次清晰，核心组件职责和协作关系定义明确。但存在若干内部审议未充分覆盖的维度性问题：

1. **与实际代码的接口一致性**（Q1、Q4、Q5、Q9）——设计引用了不存在的 API 或未充分考虑与现有构造方式的兼容性，这些缺陷需通过回归验证代码库才能暴露。
2. **逻辑语义的落地可行性**（Q3）——PAUSED 状态语义在当前无状态架构下不可实现，属于设计层面未做实现可行性验证的典型遗漏。
3. **边界场景覆盖**（Q6、Q7、Q11、Q14）——异常处理路径、装配时序风险、刷新触发机制等实现者最需要的"落地细节"仍有缺口。
4. **可实施性**（Q2、Q8、Q10）——伪代码类型不匹配、协作对象遗漏、数据模型字段缺失，直接影响编码实现的准确性。

建议在下一轮迭代中优先修复 Q1~Q5（严重级别），再处理 Q6~Q12（重要级别），其余问题可安排在后续轮次。
