# OOD 设计方案审查报告（v18）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 设计方案中类型形态选择（interface / abstract class / class / enum / JPA @Entity）与 Java 类型系统能力完全匹配。泛型使用在 Java 泛型系统能力范围内：`CapabilityExecutor<T,R>`、`StructuredChatResult<T>`、`LocalRuleFallback<T,R>` 等泛型抽象定义正确，嵌套泛型 `CompletableFuture<AiResult<StructuredChatResult<T>>>` 处于 Java 编译器可推导范围内。继承关系（单继承+多接口实现）符合 Java 约束——AbstractCapabilityExecutor 作为 abstract class 提供模板方法，子类 extends 继承；各 interface 实现关系清晰。枚举类型（LlmChatMessageRole、DegradationReason 等）使用正确。

**[通过]** 类型交互模式（`supplyAsync` 传入 lambda、`CompletableFuture.orTimeout()`链式调用、`AtomicReference` CAS 状态转换）在 Java 中完全可行。

**[通过]** `AbstractCapabilityExecutor` 的 template method 模式（`execute()` 声明 final 防止子类绕过降级预检）是 Java abstract class 的标准用法。

### 2. 标准库与生态覆盖

**[通过]** 设计中依赖的库能力均在 Spring Boot / Java 生态覆盖范围内：Spring AI（LlmChatService 实现）、Micrometer（AiMetricsCollector 指标推送）、JPA / Hibernate（AiCallLogEntity 持久化）、Jackson（StructuredOutputParser / 防御性拷贝）、Caffeine（模板/实验配置缓存）、Guava RateLimiter（EndpointRateLimiter 限流）、Reactor Flux（LlmChatStreamService 流式）。所有假设均合理。

**[通过]** `CompletableFuture`、`ThreadPoolExecutor`、`ConcurrentHashMap`、`AtomicReference` 等并发工具均为 Java 标准库原生支持。`ForkJoinPool.commonPool()` 使用符合 Java 标准约定。

**[通过]** `@ConfigurationProperties` + `EnvironmentPostProcessor` 的配置绑定与转发生态路径成熟。Maven `provided` 作用域的使用及相应的运行时风险（Spring Boot uber-JAR）已显式记录为已知约束。

### 3. 语言特性可行性

**[通过]** 错误处理策略：采用 `AiResult.success()/failure()/degraded()` 模式包装返回值而非使用异常表示业务错误，符合 Java 社区实践。`DegradationReason` 枚举集中管理降级原因，消除字符串字面量散落。LLM 调用失败/超时/解析失败均通过降级路径完成而非传播异常，与 Java `CompletableFuture` 异步契约一致。

**[通过]** 并发设计：`CompletableFuture.supplyAsync()` 委派到共享线程池的异步边界清晰；入口处提取 ThreadLocal + 闭包捕获的上下文传播模式是 Spring 异步场景的标准解法。线程池隔离（LLM 调用线程池与指标采集线程池分离）+ 差异化拒绝策略（`CallerRunsPolicy` vs `DiscardPolicy`）考虑充分。`AtomicReference<Map>` 全量替换的路由刷新策略保证读取一致性。

**[通过]** 资源管理：线程池配置有界队列 + 显式拒绝策略，避免无界资源消耗。防御性拷贝通过 `ObjectMapper.convertValue()` 保护 DTO 不可变性，兼容性分析覆盖不可变/可变/Jackson 注解三种场景。

**[通过]** 模块/包结构设计：`ai-api`（接口+DTO，零实现依赖）与 `ai-impl`（实现）的分离遵循 Maven 模块化最佳实践。包内子包（orchestrator/ router/ client/ template/ experiment/ metrics/ parser/ fallback/ degradation/ config/ mock）职责划分清晰，依赖方向合理。

### 4. 设计一致性

**[通过]** 本轮迭代要求中列出的 5 个问题（变量作用域、StructuredChatResult 设计缺口、§1.3 缺失抽象、LlmChatOptions 合并策略、getFallbackPrompt 返回值约束）均已在 v18 中得到完整修复。历史迭代中各轮严重问题和重要问题的修复措施均在设计文本中有对应修改记录。

**[通过]** 核心抽象的职责描述清晰无歧义，各个接口/类的定位、协作关系、使用方和提供方均有明确定义。管线流程从 AiOrchestrator.handle() → CapabilityExecutor.execute() → AbstractCapabilityExecutor 模板方法 → doExecuteInternal() 的委托链完整闭环。

**[通过]** 类图、伪代码、文本描述三方一致。类图中的方法签名与伪代码中的参数列表对应。§4.1 完整管线伪代码与 §4.2 薄适配器特化管线的差异点通过注释显式标注。

**[通过]** 模块间依赖方向合理，无循环依赖：ai-api ← ai-impl ← Phase 4 业务模块（出向 provided 依赖）；ai-impl 内部 orchestrator → 其余子包；client/ 为唯一引入外部大模型依赖的子包。

**[轻微]** §4.2 薄适配器 `doExecuteInternal()` 超时降级路径使用字符串字面量 `"TIMEOUT:ThinAdapterTimeout"`（第 1991 行），与 §3.8 `DegradationReason` 枚举体系中"降级路径中不再使用字符串字面量"的约定不一致——同一方法第 2003 行的基础设施异常路径已正确使用 `DegradationReason.INFRASTRUCTURE_ERROR + ":" + originalType`。建议对齐为 `DegradationReason.TIMEOUT + ":ThinAdapterTimeout"`。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则：AiOrchestrator（纯路由委托）、CapabilityExecutor（管线执行）、LlmChatService（LLM 交互抽象）、PromptTemplateManager（模板管理）、ExperimentManager（A/B 实验）、AiMetricsCollector（指标采集）、SlidingWindowMetricsStore（窗口指标存储）、ModelEndpointHealthManager（端点健康）、EndpointRateLimiter（限流）——每个组件职责互不重叠。

**[通过]** 抽象层次恰当：引入 AbstractCapabilityExecutor 作为模板方法骨架抽取公共降级预检和指标采集逻辑，避免 13 个实现的代码重复，而非过度设计 Pipeline Step 链（已在 §7 和修订说明 v2.2 中明确评估决定留待 Phase 6 引入）。

**[通过]** 设计便于后续详细设计和实现：13 项能力的 CapabilityExecutor 实现均已有明确的伪代码模板和类图映射；伪代码中关键步骤（实验分流、模板渲染、模型路由、健康检查、LLM 调用、结构化解析、指标采集、降级兜底）的 try-catch 异常处理均已覆盖。

**[通过]** 设计便于单元测试：所有关键抽象均为 interface，可通过 `@MockBean` 轻松模拟。§11 测试策略章节覆盖了各降级路径触发条件验证、线程池排队验证、HTTP Header 提取验证、异步上下文传播验证等场景。

## 修改要求（REJECTED 时存在）

N/A — 审查结果为 APPROVED，无修改要求。
