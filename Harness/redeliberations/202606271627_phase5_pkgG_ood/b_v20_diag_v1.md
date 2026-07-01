# Phase 5 包 G OOD 设计文档（v20）质量审查报告

审查范围：`a_v20_copy_from_v19.md`（v20，基于 v19 的拷贝）
审查视角：需求响应充分度、事实正确性、逻辑一致性、落地完整性（编码指导、接口定义、异常边界）
审查依据：需求文档、迭代历史（共 19 轮反馈）、现有代码库 `AiService.java`/`FallbackAiService.java`/`DegradationStrategy.java`/`AiResult.java`/`DegradationContext.java`/`MockAiService.java`

---

## 问题 1（严重，事实错误）：`LlmChatService.chat()` 返回 `CompletableFuture` 但管线伪代码中当作同步 `LlmChatResponse` 使用，缺少 `.join()` 调用

- **所在位置**：§3.2 第 1030 行（契约声明）vs §4.1 第 1979-1986 行（调用点）
- **问题描述**：§3.2 明确契约 `chat(LlmChatRequest)` → `CompletableFuture<AiResult<LlmChatResponse>>`，但 §4.1 伪代码第 1979 行 `chatResponse = llmChatService.chat(chatRequest)` 直接赋值，后续第 1981 行 `chatResponse.getRetryCount()`、第 1986 行 `chatResponse.getContent()` 均当作裸 `LlmChatResponse` 使用。此处缺少至少一步解包：需 `.join()` 获得 `AiResult<LlmChatResponse>`，再检查 `isSuccess()` 后获取 `data`。同样的问题存在于第 1973 行 `structuredChat()` 返回 `CompletableFuture<AiResult<StructuredChatResult<T>>>` 的调用点上。
- **严重程度**：严重——伪代码与接口契约矛盾，实现者无法直接推导出正确的调用序列。
- **改进建议**：统一两种方案之一：(A) 将 `LlmChatService.chat()`/`structuredChat()` 改为同步阻塞方法（内部等待 `CompletableFuture`），使管线伪代码保持线性；或 (B) 在管线伪代码中显式补充 `.join()`/`.get()` 调用和 `AiResult` 解包步骤。当前方案存在伪代码与接口签名的结构性矛盾。

## 问题 2（严重，事实错误）：`LlmChatResponse` 类图中无 `retryCount` 字段，但管线伪代码中调用 `getRetryCount()`

- **所在位置**：§2.3 第 334-338 行 `LlmChatResponse` 类图定义 vs §4.1 第 1981 行调用点
- **问题描述**：类图 §2.3 中 `LlmChatResponse` 仅声明 `content`、`usage`、`modelId` 三个字段，无 `retryCount`。但 §4.1 第 1981 行 `chatResponse.getRetryCount()` 调用了该类不存在的方法。`retryCount` 存在于 `StructuredChatResult`（第 345 行），但未在 `LlmChatResponse` 中出现。若设计意图是 `retryCount` 仅由 `structuredChat()` 返回的 `StructuredChatResult` 携带，则 `chat()` 回退路径无法获取重试计数。
- **严重程度**：严重——编译将不通过，且 `chat()` 回退路径的重试计数来源未定义。
- **改进建议**：(A) 在 `LlmChatResponse` 中补充 `retryCount: int` 字段并说明由实现层填充；或 (B) 在回退路径中将 `retryCount` 硬编码为 0 并在注释中说明限制（类似薄适配器方案）。

## 问题 3（严重，事实错误 | 逻辑矛盾）：`DegradationContext` 类图和字段表中均无 `departmentId` 字段，但伪代码调用 `setDepartmentId()`

- **所在位置**：§2.3 第 513-525 行 `DegradationContext` 类图 vs §3.8 第 1668-1676 字段扩展表 vs §4.1 第 1901 行调用
- **问题描述**：§4.1 伪代码第 1901 行调用 `context.setDepartmentId(departmentId)`，但 `DegradationContext` 的类图（§2.3）和字段扩展表（§3.8）中均未定义 `departmentId` 字段或 `setDepartmentId()` 方法。这是一个未建模的设计元素——管线代码依赖它，但核心抽象的契约文档中不存在。
- **严重程度**：严重——实现者根据类图和字段表编码时将遗漏此字段。
- **改进建议**：在 §2.3 类图 `DegradationContext` 中补充 `departmentId: String` 字段，在 §3.8 字段扩展表中补充对应行，明确其用途（供降级策略按科室维度做差异化判定）。

## 问题 4（重要，逻辑矛盾 | 深度不足）：`structuredChat()` 成功路径的 `chatResponse` 和 `retryCount` 变量在 try 块开始之前未初始化

- **所在位置**：§4.1 第 1966-1968 行 vs 第 1972-1986 行
- **问题描述**：伪代码第 1967-1968 行初始化 `retryCount = 0; tokenUsage = null`，但 `chatResponse` 变量仅在第 1979 行（catch 块内）定义。然而第 1979 行的 `chatResponse` 类型是 `LlmChatResponse`（假设问题 1 已修正），而第 1973 行 `structuredChat()` 成功路径返回 `StructuredChatResult`。两路径的变量类型不同，但在第 1988-1998 行的公共路径中仅涉及 `parsedResult`（来自 `structuredResult.getData()` 或 `parse()`），不直接引用 `chatResponse`，所以这不构成变量定义问题。但审查需指出：两路径的变量赋值路径存在类型分裂，实现者需使用 `Object` 基类或引入独立变量。
- **严重程度**：重要——虽不是直接编译错误，但类型分裂增加实现复杂度。
- **改进建议**：统一设计——将 `chat()` 回退路径的 `retryCount` 和 `tokenUsage` 也通过类似 `StructuredChatResult` 的包装返回，或明确说明回退路径中 `retryCount` 为 0、`tokenUsage` 通过 `chatResponse.getUsage()` 获取。

## 问题 5（重要，逻辑矛盾 | 深度不足）：`AiOrchestrator.handle()` 与 `AbstractCapabilityExecutor.execute()` 对 capabilityId 未注册的检查存在重复

- **所在位置**：§3.1 第 639 行 vs §4.1 第 1838-1843 行
- **问题描述**：§3.1 第 639 行声明"未注册对应执行器的能力标识在启动期 fail-fast（抛出配置异常）"，但 §4.1 handle() 伪代码第 1838-1843 行又将 null executor 处理为运行时返回失败结果 `AiResult.failure("未注册能力标识: " + capabilityId)`。两处对同一场景的处理策略矛盾——启动期 fail-fast 意味着启动即抛出异常阻止容器启动，运行时不可能有 null executor 的场景。handle() 中的 null 检查实质上是死代码。
- **严重程度**：重要——设计意图不统一，实现者不确定以哪个为准。
- **改进建议**：统一策略为启动期 fail-fast（`@PostConstruct` 中校验所有 13 个 capabilityId 均有对应 Bean），删除 §4.1 handle() 中的 null 检查和运行时回退路径，或反之将运行时路径提升为唯一策略并在启动期移除 fail-fast 校验。

## 问题 6（重要，逻辑矛盾）：`AbstractCapabilityExecutor` 构造器接收 `List<DegradationStrategy>` 但注入方式与 `@Qualifier` 规则矛盾

- **所在位置**：§3.1 第 1013 行 `@Qualifier("{capabilityId}Strategies")` vs §3.1 第 737 行推导规则 vs §3.9 第 1757-1762 行 `degradationStrategyMap`
- **问题描述**：§3.1 第 737 行和 §3.9 第 1757-1762 行说 `CapabilityExecutor` 注入一个 `Map<String, List<DegradationStrategy>>`（`degradationStrategyMap`，由 `AiPlatformConfig` 的 `@Bean` 暴露），各实现通过 `getCapabilityId()` 从 Map 中选择自己的策略列表。但 §3.1 第 1013 行构造器伪代码却使用 `@Qualifier("{capabilityId}Strategies")` 直接注入 `List<DegradationStrategy>`。这是两种不同的注入机制：全局 Map 查找 vs 按 Qualifier 注入。两者不可共存。
- **严重程度**：重要——构造器签名与装配策略不一致，实现者产生二义性。
- **改进建议**：统一为一种注入机制。推荐保留 Qualifier 注入（类型安全），删除 `degradationStrategyMap` bean 方法；或反之保留 Map + `getCapabilityId()` 查找，从构造器签名中移除 @Qualifier strategies 参数并改用 Map 注入 + 内部查找。

## 问题 7（重要，深度不足 | 遗漏）：`EndpointRateLimiter` 限流后的熔断器状态未更新

- **所在位置**：§3.2 第 1042 行 vs §3.8 第 1642-1662 行
- **问题描述**：§3.2 第 1042 行规定限流超时不重试、直接降级，降级原因 `DegradationReason.INFRASTRUCTURE_ERROR + ":RateLimitExceeded"`。但降级路径（§4.1 `doDegrade()`）中调用了 `slidingWindowMetricsStore.recordDegraded()` 或 `recordFailure()`，却未涉及 `CircuitBreakerDegradationStrategy` 的失败记录更新。限流降级本质上是"客户端节流"而非"LLM 调用失败"，但对其是否计入熔断器失败率、是否影响 `ModelEndpointHealthManager` 的端点健康状态，设计文档未定义。
- **严重程度**：重要——限流与熔断机制的交互边界未冻结。
- **改进建议**：在 §3.2 限流段或 §3.8 熔断器段明确：限流降级是否计入 `CircuitBreakerDegradationStrategy` 的失败率（建议：不计入，限流是客户端控制面行为，不应影响对服务端健康的判定），以及是否更新 `ModelEndpointHealthManager`。

## 问题 8（中等，完整性不足）：`AiCallRecord` 字段填充策略中 `modelId` 在降级路径中的来源未定义

- **所在位置**：§3.5 第 1470 行 vs §4.1 `doDegrade()` 第 2011/2018 行
- **问题描述**：`AiCallRecord.degraded()` 工厂方法签名包含 `String modelId` 参数（§3.5 第 1463 行），但 `doDegrade()` 中两处调用均传入 `null`（第 2011 行 `departmentId, null, inputSummary, ...` 中的 `null` 对应 modelId 参数）。在熔断器预检降级和路由不可用降级场景中，模型路由尚未执行，modelId 无来源，传 null 合理。但在端点健康检查降级场景（§4.1 第 1956 行）中，`modelRoute` 已知（第 1948 行），应传入 `modelRoute.getModelId()` 而非 null，以记录"目标模型在调用前已降级"的信息。当前设计对此无区分。
- **严重程度**：中等——降级路径的 modelId 可观测性丢失。
- **改进建议**：在 `doDegrade()` 方法签名中增加 `modelId` 参数，端点不可用降级路径传入 `modelRoute.getModelId()`，预检降级和超时降级传入 null。

## 问题 9（中等，逻辑矛盾 | 深度不足）：§6.1 声明 `ModelRouter` 路由表全量替换使用 `synchronized` 互斥，与 `AtomicReference` CAS 语义冲突

- **所在位置**：§6.1 第 2181 行
- **问题描述**：§6.1 第 2181 行描述 `ModelRouter` 刷新时"通过 `synchronized` 互斥防止并发刷新"且"通过 `AtomicReference` CAS 一次性替换引用"。`AtomicReference` 的 CAS 语义本身就是线程安全的无锁操作，在其外围加 `synchronized` 不仅冗余，还将刷新路径从无锁降级为有锁。若三个触发方式（`@Scheduled`、管理端 API、`@EventListener`）可能并发执行，使用 `synchronized` 确实可防止重复刷新，但设计未说明此选择是"防刷新冲突"而非"防读取冲突"——读取路径走 `AtomicReference.get()` 无锁，不应被 synchronized 保护。
- **严重程度**：中等——描述不清，实现者可能错误地使用 `synchronized` 也保护读取路径。
- **改进建议**：明确 `synchronized` 仅保护刷新方法的互斥进入（防多触发源同时刷新），读取路径保持无锁。补充注释区分"写写互斥"与"读写自由"。

## 问题 10（中等，设计缺口）：容灾场景下 `AiOrchestrator` 为单点故障

- **所在位置**：§3.1 第 671 行（class 形态选择理由）、全局
- **问题描述**：设计文档将 `AiOrchestrator` 定位为 class（非 interface），理由为"编排器是唯一的运行时实现实例，不需要多态"。但 13 项 AI 能力的全部调用均经过 `AiOrchestrator` 路由，其作为单点不可替换实例——若需实现灰度切换（新旧路由表对比）、A/B 测试两支管线并行运行、或故障时回退到降级路由，当前无扩展点的设计将被迫修改核心编排类代码。
- **严重程度**：中等——初始阶段可以接受，但设计中未记录此取舍的后果及未来演进方向。
- **改进建议**：在 §7 设计决策表对应行中补充风险记录：单 class 形态的代价——若未来需多路由策略共存或 A/B 管线，需重构为 interface + 策略模式，成本中等（影响范围：AiOrchestrator 类 + Bean 装配 + 测试）。

## 问题 11（中等，需求响应不足）：§2.3 类图中丢失 `CapabilityExecutor` 到 `EndpointRateLimiter` 的依赖关系

- **所在位置**：§2.3 第 574-584 行 `CapabilityExecutor` 关联关系段
- **问题描述**：§3.2 第 1046 行明确 `EndpointRateLimiter` 在 `LlmChatService` 实现类中调用，限流逻辑在 `LlmChatService.chat()/structuredChat()` 入口处。但 §2.3 类图中 `LlmChatService` 及其实现类均未展示与 `EndpointRateLimiter` 的依赖关系。类图同步性不足。
- **严重程度**：中等——类图是核心 OOD 产物，遗漏依赖关系降低其对编码的指导准确性。
- **改进建议**：在 §2.3 类图中为 `LlmChatService`（或 `HttpApiLlmChatService`/`SpringAiLlmChatService`）增加 --> `EndpointRateLimiter` 依赖关系。

## 问题 12（轻微，完整性不足）：`StructuredOutputParser.parse()` 的参数类型 `LlmResponse` 已被设计弃用

- **所在位置**：§2.3 第 537 行 `StructuredOutputParser` 类图 vs §3.2 第 1225 行
- **问题描述**：§2.3 第 537 行 `StructuredOutputParser.parse()` 方法签名接受 `LlmResponse` 参数，但设计已在 §3.2 用 `LlmChatResponse` 全面替换了 `LlmResponse`。类图未同步更新，属于 v17 替换后的残留。
- **严重程度**：轻微——但显式不一致会误导实现者关于解析器使用什么响应类型。
- **改进建议**：将 §2.3 第 537 行 `+parse(LlmResponse response, Class<T> targetClass) T` 中的 `LlmResponse` 替换为 `LlmChatResponse`。

## 整体评价

该文档经过 19 轮迭代，在需求覆盖度、结构完整性和多数技术细节上已达到较高成熟度。类图、状态模型、接口契约、伪代码管线、错误分类、线程模型、设计决策、迁移路径、配置示例和测试策略等 OOD 核心要素均有覆盖。但以上 12 个问题中，问题 1-3 为严重级别的事实错误/逻辑矛盾，主要集中于 `LlmChatService.chat()/structuredChat()` 的异步契约在伪代码中的表达方式、`LlmChatResponse.retryCount` 字段缺失、以及 `DegradationContext.departmentId` 字段未建模。建议在 v21 中优先修复这三个问题，然后再处理其余重要问题。修复后文档可直接指导编码实现。
