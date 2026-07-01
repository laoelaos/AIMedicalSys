# 质量审查诊断报告 — Phase 5 包 G OOD 设计（v8 审查）

## 审查说明

- **审查范围**：产出文件 `a_v8_copy_from_v7.md` 的质量审查
- **审查视角**：需求响应充分度、事实与逻辑正确性、深度与完整性（侧重内部审议未充分覆盖的维度）
- **审查轮次**：第 8 轮

---

## 发现的问题

### P1 [严重] 异步线程上下文传播未定义 — SecurityContext 和 RequestContext 在 supplyAsync 线程池中丢失

- **问题描述**：`AbstractCapabilityExecutor.execute()` 将整个管线通过 `CompletableFuture.supplyAsync()` 委派到 `LlmCallExecutor` 线程池执行。管线中多处依赖 Spring `ThreadLocal` 上下文：
  1. `SecurityContextHolder.getContext().getAuthentication()`（§4.1 第 1163 行）——用于提取 userId，传入 `ExperimentManager.assign()`
  2. `RequestContextHolder` / HTTP `X-Department-ID` Header（§3.1 薄适配器 `doExtractDepartmentId()`）——薄适配器型 CapabilityExecutor 的 departmentId 提取
  3. `callerRole`/`callerId` 提取（§3.5 字段填充策略第 903 行）——指标记录

  Spring 的 `SecurityContextHolder` 和 `RequestContextHolder` 默认使用 `ThreadLocal`（非 `InheritableThreadLocal`），在 `supplyAsync` 线程池的线程中**不可自动继承**。虽然 `SecurityContextHolder` 提供了 `MODE_INHERITABLETHREADLOCAL` 模式，但 `RequestContextHolder` 不自动传播到 `ForkJoinPool` 或自定义 `ThreadPoolExecutor` 的线程。

  **影响**：
  - userId 在异步执行中全部回退为 `"SYSTEM"`（§3.1 第 610 行的 null-safe 兜底），导致 A/B 实验的确定性分桶失效——所有请求的 hash 基于 `"SYSTEM"` 而非真实用户标识
  - 薄适配器型 CapabilityExecutor 的 departmentId 始终为 null
  - 调用方角色/标识在异步指标采集路径中缺失

- **所在位置**：§4.1 `AbstractCapabilityExecutor.execute()` 模板方法（第 1144-1157 行）+ §3.1 UserId/SessionId 上下文来源（第 609-612 行）+ §3.5 字段填充策略（第 903 行）+ §3.1 薄适配器 departmentId 提取（第 647 行）
- **严重程度**：严重
- **改进建议**：在 §6 并发设计中补充异步上下文传播机制，至少需明确以下三选一策略：
  (a) 在 `supplyAsync()` 之前捕获上下文快照，在 lambda 内部恢复（如通过 `ContextSnapshot` 工具类或 Spring `SecurityContext` 的异步支持）
  (b) 将异步边界从 `execute()` 层面下移到 LLM 调用层面——仅在 `llmClient.invoke()` 步骤使用 `supplyAsync`，其余管线步骤保持同步执行于调用线程（需评估对 Tomcat 线程池的影响）
  (c) 明确声明 userId/departmentId/callerInfo 在 `execute()` 入口处提取完毕再传入 lambda，而非在 lambda 内部依赖上下文

  推荐方案 (c) 或 (b)，因为方案 (a) 在 `CompletableFuture` 的嵌套 lambda 中易遗漏上下文传递且测试困难。

---

### P2 [严重] LlmCallExecutor 线程池拒绝策略未定义 — 高并发下线程池满载后的行为不可控

- **问题描述**：§3.2 定义了 `LlmCallExecutor` 线程池的`核心线程 = 可用模型端点数，最大线程 = 2x 核心线程`，但**未定义拒绝策略**。与之对比，指标采集线程池已在 §3.5 第 851 行显式定义拒绝策略为 `DiscardPolicy` + 日志 WARN。
  LlmCallExecutor 使用 `CompletableFuture.supplyAsync()`（内部调用 `ThreadPoolExecutor`），默认的 `ThreadPoolExecutor` 拒绝策略为 `AbortPolicy`。当同时请求数超过 maxPoolSize + queueCapacity 时，`AbortPolicy` 会抛出 `RejectedExecutionException`。该异常不会在 `supplyAsync` 的 `CompletableFuture` 中被任何地方捕获（`AiOrchestrator.handle()` 的 catch 块捕获的是 `executor.execute()` 内部抛出的异常，而非线程池拒绝异常），将导致 `CompletableFuture` 以异常状态完成，最终传播为 HTTP 500 或未处理的 `CompletionException`。

- **所在位置**：§3.2 LlmClient 线程模型（第 735 行）提到了共享线程池，但全文档均未定义其拒绝策略。§4.1 管线伪代码（第 1144-1157 行）对此无覆盖。
- **严重程度**：严重
- **改进建议**：在 §3.2 或 §6.1 中为 `LlmCallExecutor` 显式定义拒绝策略：
  - 建议方案：与指标线程池逻辑对称——`CallerRunsPolicy`（回退到 Tomcat 请求线程执行，增加请求响应时间的方差但保证请求不丢失）或 `DiscardPolicy` + 降级（请求降级，返回 `AiResult.degraded("SystemOverload")`）
  - 需评估 `CallerRunsPolicy` 对 Tomcat 线程池的阻塞影响（这是指标线程池改用 `DiscardPolicy` 时已经考虑的教训，§7 第 1355 行）
  - 建议同步补充线程池的 `queueCapacity` 定义（当前未指定）

---

### P3 [重要] doDegrade() 方法签名缺少 departmentId 参数 — 降级路径下 AiCallRecord 构造缺少科室标识

- **问题描述**：`AbstractCapabilityExecutor` 的 `doDegrade()` 方法签名仅为 `doDegrade(startTime, degradeReason, request, capabilityId)`，但在其内部调用 `AiCallRecord.degraded(capabilityId, LocalDateTime.now(), elapsedMs, degradeReason, departmentId, ...)` 时需要 `departmentId`。departmentId 在降级路径中不可见。
  - 从 `execute()` 模板方法调用 `doDegrade()`（第 1153 行）时，departmentId 存在于本地变量 `context`（第 1150 行 `context.setDepartmentId(doExtractDepartmentId(request))`）中但未提取传递
  - 从 `doExecuteInternal()` 调用 `doDegrade()`（第 1170、1176、1183 行）时，departmentId 作为方法参数存在但未传递
  - 薄适配器版本的 `doThinDegrade`（§3.1 第 669 行伪代码）则显式包含 `departmentId` 参数，与标准版形成不一致

- **所在位置**：§4.1 第 1153、1170、1176、1183 行的 `doDegrade()` 调用点；§4.1 第 1189-1200 行的 `doDegrade()` 方法体；§3.1 第 669 行薄适配器版 `doThinDegrade` 签名
- **严重程度**：重要
- **改进建议**：将 `doDegrade()` 方法签名扩展为 `doDegrade(startTime, degradeReason, request, capabilityId, departmentId)`，或在方法体内部调用 `doExtractDepartmentId(request)` 获取。建议前者（显式传参更清晰，且 `doExecuteInternal()` 中 departmentId 已存在无需重复提取）。相应更新所有调用点。

---

### P4 [重要] AiCallRecord 工厂方法签名不完整 — failure() 和 degraded() 签名未覆盖 callerRole/callerId 字段

- **问题描述**：§3.5 第 901-906 行定义了字段填充策略，明确 `callerRole` 和 `callerId` 应从 `SecurityContext`/`RequestContext` 中提取。但第 884-898 行定义的三个工厂方法中：
  - `success()` 签名未包含 `callerRole` 和 `callerId`
  - `failure()` 签名未包含 `callerRole`、`callerId`
  - `degraded()` 签名未包含 `callerRole`、`callerId`

  这导致在非成功场景下（failure/degraded），调用者角色和标识无法传入 `AiCallRecord`。字段填充策略声明这些字段来自 SecurityContext 提供了"机制说明"但未提供"代码入口"——工厂方法不接受这些参数，又无 setter/Builder 模式补充填充。

- **所在位置**：§3.5 工厂方法签名（第 884-898 行）；§3.5 字段填充策略第 903 行
- **严重程度**：重要
- **改进建议**：两种修复路径择一：
  (a) 在所有三个工厂方法签名中增加 `callerRole` 和 `callerId` 参数（显式填充）
  (b) 若字段填充策略要求在管线入口统一提取并自动注入，则需在 `AiCallRecord` 上定义 Builder 模式或 `withCallerInfo()` 变异方法，并在管线入口（如 `AbstractCapabilityExecutor.execute()` 模板方法或 `AiOrchestrator.handle()`）设置默认值后再传入

  推荐方案 (a)，原因是 SecurityContext 在异步调用中的可用性问题（见 P1）。在工厂方法中显式传参可迫使调用方明确处理 caller 信息的提取时机。

---

### P5 [重要] 薄适配器 departmentId 提取在 supplyAsync 异步上下文中无效

- **问题描述**：§3.1 第 647 行定义薄适配器型 CapabilityExecutor 的 `doExtractDepartmentId()` 为：
  ```
  doExtractDepartmentId(request):
      return extractFromRequestContext("X-Department-ID")
  ```
  此方法依赖 Spring `RequestContextHolder` 获取 HTTP 请求属性。但 `doExtractDepartmentId()` 在 `AbstractCapabilityExecutor.execute()` 模板方法（第 1150 行）中被调用，该调用位于 `CompletableFuture.supplyAsync()` lambda 内部。Spring `RequestContextHolder` 依赖 `RequestContextFilter`/`RequestContextListener` 在每个 HTTP 请求线程上绑定上下文，不会自动传播到线程池线程。

  **影响**：薄适配器在异步执行中始终无法获取 departmentId，即使通过 HTTP Header 传入了有效值。

- **所在位置**：§3.1 第 647 行 `doExtractDepartmentId()` 伪代码；§4.1 第 1149-1150 行调用位置
- **严重程度**：重要
- **改进建议**：在 §3.1 薄适配器部分增加说明，departmentId 提取的时机应在 `execute()` 调用之前（由 `AiOrchestrator` 在委托前注入 request 对象），或通过显式的 `RequestContextHolder` 传播机制（如 Spring `@Async` 的 `TaskDecorator`）在 async 线程启动时恢复上下文。推荐前者——在 `AiOrchestrator.handle()` 的委托入口处提取 departmentId 并注入到 `AiRequestBase.departmentId`（见 P1 建议）。

---

### P6 [重要] ModelEndpointHealthManager 状态机缺少 UNAVAILABLE→DEGRADED 和 CONNECTED→UNAVAILABLE 的直接转换路径

- **问题描述**：§3.2 第 750-756 行的状态模型定义了 `CONNECTED ←→ DEGRADED ←→ UNAVAILABLE`，但状态转换描述中：
  - 从 CONNECTED 到 UNAVAILABLE 的路径未明确——是否总是经过 DEGRADED，还是 5 次连续失败可直接从 CONNECTED 跳转到 UNAVAILABLE？
  - 从 UNAVAILABLE 恢复到 DEGRADED 的路径未定义——探测成功是回到 CONNECTED 还是先回到 DEGRADED？
  - 状态转换表缺少对"连续失败 N 次但不满足耗时阈值"场景的处理说明

  当端点突然彻底宕机（如 5xx 连续返回），按当前设计需先满足"近窗口内连续 3 次调用耗时 > 阈值"才进入 DEGRADED，再满足"连续 5 次调用失败"才进入 UNAVAILABLE。这意味着一个端点可能在显著异常的情况下仍被标记为 CONNECTED（如果失败是立即返回而非超时），熔断器（CircuitBreakerDegradationStrategy）在能力级别处理失败率，但端点健康管理器在端点级别看不到这种跳变路径。

- **所在位置**：§3.2 第 750-756 行；§3.8 第 1073-1084 行（CircuitBreakerDegradationStrategy 状态模型，作为对照）
- **严重程度**：重要
- **改进建议**：补充端点健康管理器的状态转换表，明确所有合法路径：
  - CONNECTED → UNAVAILABLE：连续 N 次调用失败（如 HTTP 5xx/连接拒绝）
  - UNAVAILABLE → CONNECTED：探测调用成功
  - 同时补充各转换的触发条件与阈值

---

### P7 [中等] 熔断器与端点健康管理器独立探测可能产生冲突

- **问题描述**：§3.2 第 763-766 行虽然定义了交互优先级（熔断器优先），但两个组件各自维护独立的探测机制：
  - `CircuitBreakerDegradationStrategy` 在 OPEN 状态 30 秒后允许 HALF_OPEN 探测（通过 `shouldDegrade()` 返回 false 放行一次调用）
  - `ModelEndpointHealthManager` 在 UNAVAILABLE 状态每 30 秒允许一次探测（通过 `tryProbe()` 返回 true）

  当一个能力同时触发熔断（能力级别）和端点不可用（端点级别）时，熔断器在 OPEN→HALF_OPEN 后放行的一次探测调用会经过 ModelEndpointHealthManager 的状态检查。若端点仍为 UNAVAILABLE，`tryProbe()` 可能返回 true（允许探测）或 false（阻止探测），取决于 30 秒窗口是否已到。两个探测计时器独立运行，可能导致：
  - 熔断器允许探测但端点管理器阻止（浪费一次熔断器的探测窗口）
  - 端点管理器已允许探测但熔断器阻止（浪费端点管理器的探测窗口）
  - 两个探测器交替重叠，恢复速度慢于预期

- **所在位置**：§3.2 第 756 行（探测调用触发机制）+ 第 763-766 行（交互优先级）
- **严重程度**：中等
- **改进建议**：统一探测机制——将端点健康探测决策权归并到一个组件。建议 `ModelEndpointHealthManager` 作为端点级别健康状态的单一信源，`CircuitBreakerDegradationStrategy` 的 HALF_OPEN 状态不再独立管理探测，而是委托给 `ModelEndpointHealthManager.getState()` + `tryProbe()` 的结果。或明确说明当前独立探测模式为 Phase 5 简化策略，并评估其在生产中的实际影响。

---

### P8 [中等] AiCallRecord 未记录 Prompt 版本号 — A/B 实验效果分析缺少关键维度

- **问题描述**：§4.3 A/B 实验流契约定义了 ExperimentAssignment 可携带 `targetPromptVersion`，管线中 `promptTemplateManager.render()` 已接收此参数。但 `AiCallRecord` 的字段表和工厂方法中均未包含 `promptVersion` 字段。这意味着：
  - 无法在事后分析中关联"调用结果质量"与"使用的 Prompt 版本"
  - A/B 实验效果的量化评估（Prompt 版本 A vs B 对分诊准确率的影响）缺少基础数据支持
  - 无法按 Prompt 版本维度分析 Token 消耗趋势

  这与 §1.1 设计目标"第 4 点 A/B 实验可控化"中承诺的"实验结果可观测"不完全匹配。

- **所在位置**：§3.5 AiCallRecord 字段表（第 860-882 行）；AiCallLogEntity 字段表（第 943-966 行）；§4.3 实验分流契约（第 1227 行提及 targetPromptVersion 传入 render 但未传入 record）
- **严重程度**：中等
- **改进建议**：在 `AiCallRecord` 和 `AiCallLogEntity` 中补充 `promptVersion` 字段（`Integer`，可为空），同步更新工厂方法和 JPA 映射。该字段在 `doExecuteInternal()` 中由 `assignment.getTargetPromptVersion()` 赋值。

---

### P9 [一般] AiCallRecord.degraded() 工厂方法缺少 outputSummary 参数 — 本地规则降级结果无法记录

- **问题描述**：当降级路径执行 `localRuleFallback.fallback(request)` 后，降级结果（本地规则输出）无法被记录到 `AiCallRecord` 中。`degraded()` 工厂方法（第 895-898 行）的签名包含 `inputSummary` 但不包含 `outputSummary`，而降级路径中的本地规则降级确实产生了业务输出（如处方审核的本地规则检查结果）。

  与之对比，`success()` 工厂方法同时包含 `inputSummary` 和 `outputSummary` 参数。

- **所在位置**：§3.5 第 895-898 行 `degraded()` 工厂方法签名；§4.1 第 1192-1196 行降级路径（本地规则降级后调用 `AiCallRecord.degraded(...)`）
- **严重程度**：一般
- **改进建议**：在 `degraded()` 工厂方法签名中补充 `String outputSummary` 参数（可空），并在 `doDegrade()` 中获取本地规则降级的输出摘要后传入。

---

### P10 [一般] 需求响应维度：未显式验证与 Phase0/Phase1ABD OOD 设计风格的一致性

- **问题描述**：用户需求明确的第 1 条要求"参考已有的 Phase0、Phase1ABD 的 OOD 设计成果，保持设计风格和结构一致性"。当前文档未引用或映射现有的 Phase0/Phase1ABD 设计结构（如命名约定、章节编排、抽象层级划分方式、设计决策表格式等）。虽然文档自身的结构和设计决策表格式看起来合理，但缺少对"一致性"的显式承诺或对比。

- **所在位置**：全局（缺少跨阶段设计风格一致性声明或映射表）
- **严重程度**：一般
- **改进建议**：在 §1 概述部分增加对 Phase0/Phase1ABD 设计风格和结构的引用说明，或至少说明本设计的结构差异背后的理由。可在 §7 设计决策表中补充一至两行关于阶段间设计风格一致性决策的记录。

---

### P11 [一般] ModelRoute 缺少端点级认证和超时配置定义

- **问题描述**：`ModelRoute`（§3.2 第 787 行）定义为封装 `endpointId`、`modelId`、`endpoint` URL、`clientType`、生成参数默认值与权重。但未包含：
  - API 认证信息（如 API Key 的引用方式、认证头模板）
  - 端点级超时配置（连接超时、读取超时）
  - 速率限制配置

  文档中多处在 YAML 示例（§9.5）和路由描述中暗示这些配置存在，但 `ModelRoute` 的数据模型未覆盖。这导致开发者在实现 `HttpApiLlmClient` 时无法确定认证密钥如何传递。

- **所在位置**：§3.2 ModelRoute 定义（第 787 行）；§9.5 YAML 路由配置示例（第 1459-1461 行）；§3.2 LlmRequest 定义（第 770 行）
- **严重程度**：一般
- **改进建议**：明确认证密钥和超时配置的存放位置：
  (a) 扩展到 `ModelRoute` 值对象中（增加 authentication、timeout 字段）
  (b) 或明确说明密钥通过 Vault/配置中心在 `LlmClient` 中按 `endpointId` 查询

  推荐方案 (a) 显式建模，避免认证信息的隐式传递路径。

---

## 整体质量评价

该产出经过多轮审议迭代，在核心抽象定义、类图完整性、状态模型覆盖率、伪代码正确性等维度上已达到较高成熟度。但审查发现，在**异步上下文传播**、**线程池完备性配置**、**降级路径方法签名一致性**等跨组件协同的细节维度上仍存在若干关键缺口，这些缺口的共性特征是在"组件内审议"中不易暴露，但会在生产环境中导致静默功能异常。上述 P1、P2 为最高优先级，建议优先修复后再进入编码阶段。
