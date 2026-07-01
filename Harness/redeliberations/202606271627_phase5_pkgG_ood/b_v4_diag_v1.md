# Phase 5 包 G OOD 设计 — 质量审查报告（第 4 轮）

## 审查范围与视角

- **审查对象**: `a_v4_design_v2.md`（Phase 5 包 G — AI 进阶底座 架构级 OOD 设计方案）
- **审查重点**: 需求响应充分度、事实错误与逻辑矛盾、深度与完整性（可落地编码视角）
- **注意**: 前 3 轮已覆盖技术可行性、Bean 装配、方法签名、状态模型等维度，本轮审查避免重复

---

## 问题清单

### P1 [严重] 降级路径中 recordSuccess 导致熔断器统计失准

- **所在位置**: §4.1 降级路径伪代码第 979 行 + §3.5 `SlidingWindowMetricsStore` 方法签名 + §2.3 类图第 368-373 行
- **问题描述**: degrade 分支中 localRuleFallback 成功后调用 `slidingWindowMetricsStore.recordSuccess(capabilityId, elapsedMs)`，但 `recordSuccess` 方法的签名（类图行 368-369）仅接受 `capabilityId` 和 `elapsedMs`，不接收 `degraded` 标记。这意味着降级后本地规则兜底的调用被等价于一次正常成功调用来记录。其后果是：当某能力的 AI 服务持续失败但每次都被本地规则成功兜底时，失败率不会上升（每次都 recordSuccess），熔断器永远不会触发，整个熔断机制被架空。
- **严重程度**: 严重
- **改进建议**: 为 `SlidingWindowMetricsStore` 增加重载方法 `recordSuccess(String capabilityId, long elapsedMs, boolean degraded)`，或新增 `recordDegradedFallback(String capabilityId, long elapsedMs)`。`getFailureRate()` 应将降级后的成功排除在正常成功计数之外，可由计算公式 `failureCount / (successCount(非降级) + failureCount)` 替代当前的 `failureCount / totalCount`。

---

### P2 [严重] departmentId 在标准管线中无可用来源

- **所在位置**: §3.5 AiRequestBase 字段定义（第 737-743 行）+ §4.1 管线伪代码第 951 行 + §4.4 PromptTemplateManager.render() 签名
- **问题描述**: §4.1 管线伪代码第 951 行调用 `request.getDepartmentId()` 作为科室标识入参传给 `PromptTemplateManager.render()`，但 `AiRequestBase`（§3.5）仅定义了 `visitId`/`patientId`/`sessionId` 三个字段，**未包含 `departmentId`**。所有业务请求 DTO 若继承 `AiRequestBase`，均无法通过基类获取 `departmentId`。这意味着要么每个 CapabilityExecutor 实现各自从 DTO 中自行获取（破坏统一性），要么 template rendering 步骤在所有能力上都无法正确执行。
- **严重程度**: 严重
- **改进建议**: 在 `AiRequestBase` 中增加 `String departmentId` 字段（可空），或明确 `departmentId` 从 Spring `RequestContext`/`SecurityContext` 中提取，并在 §3.5 字段填充策略中补充说明。

---

### P3 [严重] 异步边界不明确 — LlmClient.invoke() 同步阻塞与 CompletableFuture 返回值矛盾

- **所在位置**: §2.3 类图 `LlmClient.invoke()` 返回值类型（第 248 行）+ §3.1 CapabilityExecutor.execute() 方法签名 + §4.1 伪代码第 966 行
- **问题描述**: `CapabilityExecutor.execute()` 返回 `CompletableFuture<AiResult<R>>` 表明为异步契约，但 §4.1 伪代码中整个管线（包括第 966 行的 `llmClient.invoke()` HTTP 网络调用）以同步方式顺序执行。`LlmClient.invoke()` 类图签名返回 `LlmResponse`（非 `CompletableFuture`），故 LLM 调用会在调用者线程上同步阻塞。这意味着 `execute()` 的 `CompletableFuture` 在实现中未做 `supplyAsync()` 包装时，调用方线程会直接阻塞在管线执行上，异步语义名存实亡。
- **严重程度**: 严重
- **改进建议**: 明确实现策略之一：(a) 将 `LlmClient.invoke()` 改为 `CompletableFuture<LlmResponse>`，实现真异步调用链；或 (b) `CapabilityExecutor.execute()` 内部包裹 `CompletableFuture.supplyAsync(() -> { ... 管线代码 ... }, executorThreadPool)`，将阻塞操作提交到独立线程池；或 (c) 若接受同步语义，则将 `execute()` 返回值改为 `AiResult<R>`（去掉 CompletableFuture 包装）。选择后在对应章节显式说明。

---

### P4 [严重] 降级策略 Bean 注入机制从 YAML 配置到 @Qualifier 的转换路径未定义

- **所在位置**: §3.1 第 569 行 + §3.8 第 914 行 + §9.2 配置示例
- **问题描述**: 设计描述"策略列表通过 Spring `@Qualifier("capabilityId")` 按能力标识注入，由 `AiPlatformConfig` 配置驱动"。但 YAML 配置 `ai.degradation.strategies.TRIAGE: [timeout, circuit-breaker]` 中的字符串列表如何转化为实际的 `DegradationStrategy` Bean 引用，**没有任何机制定义**。Spring 不会自动将 YAML 中的 bean 名称字符串列表转换为 `@Qualifier` 注解对应的 Bean 集合。缺少 `AiPlatformConfig` 中 `@Bean` 工厂方法的逻辑说明或伪代码示例。
- **严重程度**: 严重
- **改进建议**: 在 §3.1 或 §7 设计决策中补充完整的策略 Bean 装配机制。建议方案：`AiPlatformConfig` 中为每个能力声明 `@Bean` 方法，通过 `ApplicationContext.getBean()` / `BeanFactory` 按名称查找策略实例并组装为 `List<DegradationStrategy>`，或通过自定义 `BeanPostProcessor` 按配置注入。需要至少提供伪代码级说明以确保可编码。

---

### P5 [重要] 薄适配器型 CapabilityExecutor 的执行行为未定义

- **所在位置**: §3.1 第 505-508 行 + §4.1 伪代码
- **问题描述**: §3.1 称 Phase 4 的 6 项能力使用"薄适配器型 CapabilityExecutor"，其 `execute()` 方法"直接委托给 Phase 4 现有业务服务接口，不做底座管线绕行"。但 §4.1 仅提供了一条完整的 8 步管线伪代码（模板渲染→实验分流→模型路由→LLM 调用→结果解析→指标采集），**未提供薄适配器版本的管线定义**。开发者无法从设计文档中获知薄适配器是否应执行降级预检？是否应走健康检查？是否应记录指标？何时调用 `localRuleFallback`？这些行为空白将导致 6 个适配器实现不一致。
- **严重程度**: 重要
- **改进建议**: 在 §4.1 中新增"薄适配器 CapabilityExecutor 管线"小节，提供独立的简化伪代码，明确其执行模式。建议为：降级预检 → 委托 Phase 4 服务 → 指标采集（含成功/失败/降级标记）。

---

### P6 [重要] AiOrchestrator 方法到能力标识的映射约定未定义

- **所在位置**: §3.1 AiOrchestrator 职责 + §4.1 第 931-936 行
- **问题描述**: `AiOrchestrator` 的 13 个 `AiService` 方法各自需要知道对应的 `capabilityId` 字符串才能在 `executorMap` 中查找正确的 `CapabilityExecutor`。但设计文档**未定义**每个方法对应的 capabilityId 字符串值（如 `triage()` → `"TRIAGE"`、`prescriptionCheck()` → `"RX_AUDIT"` 等）。此映射看似简单，但若未显式约定，各实现者可能使用不同字符串值引入运行时配置错误。
- **严重程度**: 重要
- **改进建议**: 在 §3.1 或新增附录中显式列出 `AiService` 全部 13 个方法到 capabilityId 的映射表。建议将 capabilityId 定义为 `CapabilityId` 常量类或 `enum` 统一管理。

---

### P7 [重要] FallbackAiService.applyStrategies() 残留代码的迁移路径未在主文档中定义

- **所在位置**: §3.8 第 864 行 + §5 错误处理，对比 `FallbackAiService.java:183-194`（现有代码库）
- **问题描述**: 现有代码库中 `FallbackAiService.applyStrategies()`（第 183-194 行）**仍然存在**，继续创建空值 `DegradationContext` 并遍历策略列表。设计在修订说明（v4，第 1280 行）中声称该方法"被移除"和"FallbackAiService 不再持有或管理 DegradationStrategy 列表"，但主文档未将其列为 Phase 5 代码迁移的具体操作项。开发者若仅阅读主文档 §1-§10，不会知道需要修改 `FallbackAiService` 的现有代码。
- **严重程度**: 重要
- **改进建议**: 在 §9 迁移路径表中增加一行，明确 `FallbackAiService` 的三项迁移操作：(1) 移除 `applyStrategies()` 方法； (2) 移除构造函数的 `List<DegradationStrategy>` 参数（或保留签名但废弃）； (3) 修改每个 `AiService` 方法不再调用 `thenApply(this::applyStrategies)`。同时更新现有测试（`FallbackAiServiceTest.java:52-85` 的策略触发测试需移除或重构）。

---

### P8 [重要] ModelEndpointHealthManager 与 CircuitBreakerDegradationStrategy 的交互优先级未定义

- **所在位置**: §3.2 第 607 行 + §3.8 第 882-895 行 + §4.1 伪代码第 943-965 行
- **问题描述**: 两个组件独立维护健康状态——`ModelEndpointHealthManager` 按端点的 `CONNECTED/DEGRADED/UNAVAILABLE` 管理，`CircuitBreakerDegradationStrategy` 按能力的 `CLOSED/OPEN/HALF_OPEN` 管理。§4.1 伪代码中降级预检（策略链）在先、健康检查在后，但两者在语义上存在交互重叠：若电路已 OPEN 是否还需检查端点健康？若电路 CLOSED 但端点 UNAVAILABLE 谁负责降级？设计仅称"协作但不耦合"（第 607 行）但未定义协作规则，可能导致双重降级或相互抵消。
- **严重程度**: 重要
- **改进建议**: 在 §4.1 或 §6 中明确定义两个机制的交互优先级。建议：降级预检（含熔断器）优先，若策略链已判降级，不再执行健康检查；若策略链通过，再由健康检查判定端点是否可用。同时在 §6.2 或 §7 中增加一项设计决策表行说明这一优先级选择。

---

### P9 [重要] CapabilityExecutor 线程安全性未在 §6.1 中覆盖

- **所在位置**: §6.1 线程模型（第 1073-1084 行）
- **问题描述**: §6.1 覆盖了 `AiOrchestrator`、`LlmClient`、`ModelRouter`、`PromptTemplateManager`、`ExperimentManager`、`AiMetricsCollector` 的线程安全模型，但**未提及 `CapabilityExecutor` 实现**。多个业务线程可并发调用同一 `CapabilityExecutor` 实例的 `execute()` 方法。虽然其内部持有的注入组件（`List<DegradationStrategy>`、各 Manager/Router 引用）通常是只读的，但若任一实现类引入实例级可变状态（如计数器、缓存、临时集合），线程安全性将无保障。
- **严重程度**: 重要
- **改进建议**: 在 §6.1 中补充 `CapabilityExecutor` 的线程安全契约：要求所有实现为"无状态线程安全"或显式声明同步策略。可增加一条设计决策："CapabilityExecutor 实现应无状态，所有可变数据通过方法参数传递，禁止实例级缓存模式"。

---

### P10 [中等] userId 在 ExperimentManager.assign() 管线中的来源未定义

- **所在位置**: §4.1 伪代码第 952 行 + §3.4 ExperimentManager.assign() 方法签名（第 285 行）
- **问题描述**: 管线伪代码调用 `experimentManager.assign(capabilityId, userId, sessionId)` 但 `userId` 的来源在伪代码中没有任何定义。`AiRequestBase` 无 `userId` 字段，`AiCallRecord` 字段填充策略（§3.5）中 `callerId` 从 `SecurityContext` 提取但 `userId` 不等于 `callerId`。这导致实验分流的确定性分桶所需入参不可用。
- **严重程度**: 中等
- **改进建议**: 明确 `userId` 的来源定义：(a) 从 Spring `SecurityContext` 提取当前认证用户 ID；或 (b) 在 `AiRequestBase` 中增加 `userId` 字段；或 (c) 改为使用 `callerId` 作为分桶键（需评估业务语义是否一致）。同时在 §3.5 AiRequestBase 字段表或 §3.4 实验管理职责中说明提取方式。

---

### P11 [中等] ModelRouter 运行时刷新的线程安全性不完整

- **所在位置**: §6.1 第 1081 行 + §4.2 第 999 行（多模型按权重随机选择）
- **问题描述**: §6.1 称路由表存储于 `ConcurrentHashMap` 并"支持运行时刷新"。但 §4.2 的第 3 步"多模型负载均衡：按权重随机选择"需要原子性地读取完整路由表。若在刷新过程中（替换 Map 的部分条目而非整体替换），读取线程可能看到部分条目已被更新而其他条目未更新的不一致状态。
- **严重程度**: 中等
- **改进建议**: 使用 `AtomicReference<Map<String, List<ModelRoute>>>` 替代直接使用 `ConcurrentHashMap`，刷新时构建新 Map 后原子性替换引用；或使用 `CopyOnWriteArrayList` 存储路由条目列表，确保读操作总看到一致的快照。

---

### P12 [中等] AiOrchestrator.handle() 缺少异常捕获

- **所在位置**: §4.1 伪代码第 931-936 行
- **问题描述**: `AiOrchestrator.handle()` 伪代码在步骤 3 直接调用 `executor.execute(request, capabilityId)` 并将返回值返回，**未对 `execute()` 的可能异常做 try-catch**。虽然 §3.1 CapabilityExecutor 的契约说"不抛出业务异常"，但非受检异常（如 NullPointerException、IllegalStateException、数据库连接异常等）仍可能发生。当前缺少防御性捕获，异常将直接传播到 `FallbackAiService` 的 `thenApply()` 中导致 `CompletionException`，最终以 HTTP 500 返回而非业务友好的降级响应。
- **严重程度**: 中等
- **改进建议**: 在 `AiOrchestrator.handle()` 伪代码中增加 try-catch 块，捕获 `Exception` 后记录错误指标、调用 `slidingWindowMetricsStore.recordFailure()`，并返回 `AiResult.degraded("InternalExecutionError")`。也可将此防御性逻辑统一抽象为 AiOrchestrator 内部的包装方法。

---

## 整体质量评价

当前文档（v4/v5 迭代）相比首轮已有实质性改进——完整类图、状态模型、方法签名、降级管线均已补充。但在**可编码准备度**和**边界条件覆盖**维度仍存在 4 个严重问题（P1-P4）和 5 个重要问题（P5-P9），主要体现在：

1. **接口到实现的映射存在缺口**：降级策略的 YAML→Bean 注入机制无定义（P4）、AiOrchestrator 方法→capabilityId 映射无约定（P6）、薄适配器管线行为空白（P5）——三个缺口叠加，开发者无法直接编码。
2. **异步契约与实现方式不匹配**（P3）：CompletableFuture 返回类型与同步阻塞实现矛盾，是设计中最容易被忽视但后果最严重的问题。
3. **关键边界条件未覆盖**：降级路径的指标污染（P1）、departmentId 无来源（P2）、异常传播安全网缺失（P12）。
4. **已有代码迁移操作遗漏**：现有代码库的 `FallbackAiService.applyStrategies()` 仍待移除，但设计主文档未提及（P7）。
