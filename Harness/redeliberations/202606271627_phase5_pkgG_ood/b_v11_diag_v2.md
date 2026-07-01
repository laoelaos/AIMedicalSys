# 质量审查报告 — Phase 5 包 G OOD 设计（v11）

## 审查范围

- **待审查产出**：`a_v11_copy_from_v10.md`
- **用户需求**：`requirement.md`
- **迭代轮次**：第 11 次（质询后修订版）
- **审查视角**：需求响应充分度、事实/逻辑正确性、深度与完整性（侧重内部审议未充分覆盖的维度）

---

## 维度一：需求响应充分度

### 评估结论：满足

产出充分响应了用户需求中明确的三个要求：

1. **与 Phase0/Phase1ABD 设计风格和结构的一致性**（要求 1）：
   - §1.2 末尾显式声明参照 Phase0（降级策略体系）和 Phase1ABD（能力管线编排）的章节结构、抽象描述粒度、类型形态选择逻辑与设计决策记录格式
   - 章节编排（§1 概述→§2 模块划分→§3 核心抽象→§4 行为契约→§5 错误处理→§6 并发设计→§7 设计决策→§8 依赖声明→§9 迁移路径→§10 协作边界）与 Phase0/Phase1ABD 保持一致
   - §7 设计决策表格式与 Phase0 一致，采用"决策→选项→选择→理由"四栏结构

2. **Phase5 包 G 完整 OOD 设计**（要求 2）：
   - 覆盖 13 项 AI 能力的编排框架，7 项底座完整管线 + 6 项 Phase 4 薄适配器
   - 包括模型对接层（LlmClient/ModelRouter）、模板管理（PromptTemplateManager）、A/B 实验（ExperimentManager）、性能观测（AiMetricsCollector/SlidingWindowMetricsStore）、结构化输出解析（StructuredOutputParser）、本地规则降级（LocalRuleFallback）、降级策略扩展（Timeout/CircuitBreaker）等全部子领域
   - §9 提供完整的 13 项能力迁移路径表和 FallbackAiService 迁移步骤
   - §10 定义了与包 F、包 E 等其他包的协作边界

3. **OOD 核心要素覆盖**（要求 3）：
   - 类图：§2.3 提供完整的 Mermaid classDiagram，覆盖全部核心抽象及其继承/组合/依赖关系
   - 核心职责：§1.3 核心抽象一览表 + §3 各组件详细职责描述
   - 协作关系：§2.2 模块依赖方向图 + §2.3 类图关联 + §3 各组件"协作对象"段落
   - 关键接口：CapabilityExecutor、ModelRouter、LlmClient、PromptTemplateManager、ExperimentManager、AiMetricsCollector、StructuredOutputParser 等均定义了完整方法签名
   - 状态模型：PromptTemplate（DRAFT→ACTIVE→DEPRECATED）、Experiment（DRAFT→ACTIVE→PAUSED→COMPLETED）、ModelEndpointHealthManager（CONNECTED↔DEGRADED↔UNAVAILABLE）、CircuitBreakerDegradationStrategy（CLOSED↔OPEN↔HALF_OPEN）均明确定义

### 未发现需求层面的关键遗漏。设计范围界定清晰——13 项 AI 能力的底座化方案完整，薄适配器与完整管线的边界明确，迁移路径可操作。

---

## 维度二：事实错误与逻辑矛盾

### 问题 1：[事实错误] `LlmResponse` 缺少 `retryCount` 字段定义，但管线伪代码中直接调用 `getRetryCount()`

- **所在位置**：§2.3 类图 `LlmResponse`（第 290-294 行）、§3.2 `LlmResponse` 文本定义（第 964-966 行）、§4.1 `doExecuteInternal()` 伪代码第 1473 行
- **严重程度**：严重
- **问题描述**：`LlmResponse` 在类图和文本段落中仅定义了 `text`、`tokenUsage`、`modelId` 三个字段。但 §4.1 管线伪代码第 1473 行调用 `llmResponse.getRetryCount()` 并赋值给 `retryCount` 变量，随后传入 `AiCallRecord.success()` 工厂方法。§3.5 字段填充策略（第 1130 行）说明 `retryCount` "由 `LlmClient` 内部重试计数器提供"——据此语义，`LlmClient` 应在构造 `LlmResponse` 时将重试次数写入 `retryCount` 字段。但 `LlmResponse` 当前缺少此字段，导致实现阶段出现编译错误。
- **改进建议**：在 `LlmResponse` 的类图和文本定义中补充 `retryCount: int` 字段，说明其由 `LlmClient.invoke()` 内部填充。或将 `retryCount` 从 `LlmResponse` 中移除，改为由 `LlmClient` 通过其他方式（如方法返回值包装类或 `AiCallRecord` 构建器参数）向调用方提供重试次数。

### 问题 2：[事实错误] `doExecuteInternal()` 中调用 `extractParsedSummary()` 和 `extractOutputSummary()` 但两方法均未定义

- **所在位置**：§4.1 `doExecuteInternal()` 伪代码第 1477 行（`extractParsedSummary`）；`doDegrade()` 伪代码第 1497 行（`extractOutputSummary`）
- **严重程度**：严重
- **问题描述**：成功管线中，`structuredOutputParser.parse()` 成功后调用 `outputSummary = extractParsedSummary(parsedResult)`（第 1477 行）；降级路径中，`localRuleFallback.fallback()` 成功后调用 `outputSummary = extractOutputSummary(result)`（第 1497 行）。两个辅助方法均未在 `AbstractCapabilityExecutor`、`CapabilityExecutor` 接口、任何 helper 类或工具方法中定义。此外，两个方法名不统一（`extractParsedSummary` vs `extractOutputSummary`），进一步增加实现者的理解负担。
- **改进建议**：统一命名并在 `AbstractCapabilityExecutor` 中定义默认实现，例如 `extractOutputSummary(R result)` 默认使用 `StringUtils.truncate(result.toString(), 500)`。或在伪代码中直接使用 `outputSummary = StringUtils.truncate(parsedResult.toString(), 500)` 避免引入未定义的 helper 方法。

### 问题 3：[逻辑矛盾] 薄适配器型 `CapabilityExecutor` 在已运行于 `llmCallExecutor` 线程池的上下文中嵌套提交 `supplyAsync()` 到同一线程池

- **所在位置**：§3.1 薄适配器伪代码第 730-731 行，结合 §4.1 `AbstractCapabilityExecutor.execute()` 第 1424-1426 行
- **严重程度**：重要
- **问题描述**：`AbstractCapabilityExecutor.execute()` 通过 `supplyAsync(() -> doExecuteInternal(...), llmCallExecutor)` 提交管线到共享线程池。薄适配器的 `doExecuteInternal()` 内部再次调用 `supplyAsync(() -> phase4ServiceDelegate.execute(request), llmCallExecutor)` 提交到**同一个** `llmCallExecutor`，然后通过 `.get(thinAdapterTimeout)` 阻塞等待。这创造了线程池嵌套消费模式：外层占有一个池线程，内层从同一池申请另一线程。虽然 `CallerRunsPolicy` 在池满时会将内层回退到外层线程运行（避免了死锁），但此模式有两个问题：(1) 每个薄适配器调用消耗 2 个池线程，减少了并发容量；(2) 设计文档未说明此嵌套模式的正确性依据，实现者可能误改为不同的拒绝策略（如 `AbortPolicy`）导致运行时死锁。
- **改进建议**：(a) 将薄适配器的 `phase4ServiceDelegate.execute(request)` 直接在 `doExecuteInternal()` 中同步调用，仅用 `CompletableFuture.supplyAsync(() -> phase4ServiceDelegate.execute(request)).get(timeout)` 使用公共 `ForkJoinPool` 而非 `llmCallExecutor`；或 (b) 在文档中显式说明嵌套 `supplyAsync` + `get()` 模式的设计理由及其对线程池容量的影响，并强调 `CallerRunsPolicy` 是此模式的前提条件。

### 问题 4：[事实错误] ModelRoute 字段扩展表中 `authentication` 类型标注为"(设计占位)"

- **所在位置**：§3.2 ModelRoute 字段扩展表第 991 行
- **严重程度**：重要
- **问题描述**：`ModelRoute` 字段扩展表中 `authentication` 字段的类型标注为 `(设计占位)`，而非具体的 Java 类型。下游实现者无法据此生成 POJO 定义——它是 String？一个内部类？一个引用接口？文档声称认证凭据通过 Vault 按 `endpointId` 查询，但 `ModelRoute` 中仍需一个字段来指示该端点的认证方式（如 API-Key / OAuth2 / 无认证），而非直接存储密钥。此占位符自 v9（或更早）起已存在，历经多轮迭代仍未解决。
- **改进建议**：将 `authentication` 改为具体类型声明，例如 `authType: AuthType enum (API_KEY / OAUTH2 / NONE)`，或从字段表中删除并改为在 `LlmClient` 的实现说明中描述认证凭据获取机制。不应保留不可实现的占位符。

### 问题 5：[逻辑矛盾] §3.1 与 §4.1 对 `AbstractCapabilityExecutor.execute()` 模板方法的描述不一致——降级预检位置相互矛盾

- **所在位置**：§3.1 模板方法模式伪代码第 765-793 行 vs §4.1 `AbstractCapabilityExecutor.execute()` 伪代码第 1395-1438 行
- **严重程度**：重要
- **问题描述**：两处描述同一个 `AbstractCapabilityExecutor.execute()` 方法，但在核心逻辑上存在以下不一致：(a) **降级预检位置**：§3.1 将降级预检放置在 `CompletableFuture.supplyAsync()` lambda **内部**执行（第 784-789 行），§4.1 则将降级预检放置在 `supplyAsync()` **之前**的容器线程执行（第 1415-1421 行），并提供 `CompletableFuture.completedFuture()` 直接返回降级结果；(b) **inputSummary 定义时机**：§3.1 在 lambda 内部定义（第 782 行），§4.1 在 lambda 外部定义（第 1413 行）；(c) **超时兜底**：§4.1 有 `future.orTimeout()` 机制（第 1428-1438 行），§3.1 无。此矛盾使实现者无法确定正确的实现方案——按 §3.1 实现则降级请求需排队入池，按 §4.1 实现则优化了降级响应延迟。v13 修订说明（Q5, 第 2058 行）确认降级预检已前移至 `supplyAsync` 之前，但 §3.1 未同步更新。
- **改进建议**：同步 §3.1 的模板方法伪代码，使与 §4.1 一致——降级预检在容器线程执行、不入线程池排队；补充 `orTimeout()` 超时兜底；统一 `inputSummary` 定义时机。两处描述应指向同一份权威伪代码，避免副本不一致。

### 问题 6：[逻辑矛盾] `inputSummary` 在 `execute()` 中定义为局部变量，但 `doDegrade()` 作为独立方法依赖闭包捕获，Java 语法上不可行

- **所在位置**：§4.1 `AbstractCapabilityExecutor.execute()` 第 1413 行（`inputSummary` 定义）、第 1421 行（`doDegrade()` 调用）、第 1492 行注释（声称"通过闭包捕获"）
- **严重程度**：重要
- **问题描述**：`execute()` 方法在 `supplyAsync()` 之前定义 `inputSummary = StringUtils.truncate(request.toString(), 500)`（第 1413 行）。当降级预检命中时，第 1421 行调用 `doDegrade(startTime, ..., sessionId)`——这是一个**实例方法调用**而非 lambda 表达式。Java 中实例方法无法访问调用者的局部变量（`inputSummary`），此模式在 Java 语法层面不可行。第 1492 行注释"inputSummary 由 execute() 入口处定义并通过闭包捕获，所有调用点均可访问"的表述仅对 lambda 内部代码成立，对 `doDegrade()` 和 `doExecuteInternal()` 等独立方法不成立。同样的矛盾也存在于正常管线中：`doExecuteInternal()` 内部使用 `inputSummary`（第 1482 行），但该变量只定义在 `execute()` 方法中（第 1413 行），未作为参数传入。
- **改进建议**：将 `inputSummary` 作为参数加入 `doDegrade()` 和 `doExecuteInternal()` 的方法签名，或改为在方法体内重新定义（如 `StringUtils.truncate(request.toString(), 500)`）。选择方案需在伪代码中明确，确保实现者不需要自行推断。

---

## 维度三：深度与完整性

### 问题 7：[完整性不足] YAML 配置示例中 7 项底座能力的超时配置只覆盖了 3 项

- **所在位置**：§9.5 YAML 配置第 1822-1832 行
- **严重程度**：一般
- **问题描述**：`execution.timeout.per-capability` 显式列出了 7 项底座能力中的 3 项（TRIAGE、RX_AUDIT、MEDICAL_RECORD_GEN），其余 4 项（RX_ASSIST、KB_QUERY、SCHEDULE、DISCUSSION_CONCLUSION）依赖 `default: 60s`。虽不构成设计错误，但"各能力独立配置"的承诺在 YAML 示例中未兑现。对于 `KB_QUERY` 这种"首次真实实现于底座"的能力，60 秒默认值是否合理无从判断。6 项薄适配器的超时也全部依赖 `thin-adapter-default: 30s`。
- **改进建议**：在注释中为每种能力给出超时值选择依据，或完整填充全部 13 项能力的超时配置（即使部分与默认值相同），让实现者看到完整的配置形态。

### 问题 8：[完整性不足] 未提供测试策略或可验证性指导

- **所在位置**：全文
- **严重程度**：一般
- **问题描述**：设计文档覆盖了 13 个 CapabilityExecutor、5 个基础设施组件、2 层编排，但未提供任何关于如何单元测试、集成测试或模拟组件的指导。例如：(1) `AbstractCapabilityExecutor` 的模板方法如何单测？（子类 mock + 验证降级预检不可绕过）；(2) `LlmClient` 的 HTTP 调用如何在测试中模拟？（WireMock / Mockito）；(3) `AiPlatformConfig` 的条件 Bean 装配如何在测试环境配置？(4) 管线伪代码中的降级路径如何通过测试覆盖全部组合？
- **改进建议**：在文档末尾新增"测试策略"章节（或扩充 §8），建议至少覆盖：(a) 单元测试模式——每个 CapabilityExecutor 使用 `@MockBean` 模拟所有下游依赖，验证降级路径触发条件；(b) 集成测试模式——`@SpringBootTest` + `@TestConfiguration` 模拟底座激活状态；(c) 管线收敛验证——关键设计决策（降级判定前置、防御性拷贝、异步上下文传播）的可测试性证明。

---

## 整体质量评价

产出已达到较高成熟度，覆盖了 13 项 AI 能力的编排框架、模型对接、模板管理、A/B 实验、性能观测等核心设计维度，类图、伪代码、状态模型、设计决策等 OOD 要素基本完整。经过多轮迭代审议，多数重度问题已在历次修订中解决。

在**需求响应充分度**维度上，产出充分满足用户需求——设计风格与 Phase0/Phase1ABD 保持一致，结构化要素完整，Phase 5 包 G 的 OOD 覆盖全面。

当前版本（v11）的主要质量短板集中在**跨章节表述一致性**和**伪代码到 Java 的可映射性**上。问题 1（retryCount 字段缺失）、问题 2（未定义的 helper 方法）、问题 6（inputSummary 闭包假设不成立）是实现阻塞项，修复前无法直接编码。问题 3（线程池嵌套）和问题 5（§3.1 与 §4.1 矛盾）降低实现可靠性，需在编码前统一。建议修复者在进入编码前先解决问题 1~6。

---

## 修订说明（v2）

| 质询意见 | 回应 |
|---------|------|
| **维度1（需求响应充分度）缺失**：报告完全未覆盖维度1，导致审查结论无法覆盖任务的核心关切 | **已补充**：新增"维度一：需求响应充分度"章节，从风格一致性、OOD 要素覆盖、完整范围三个子维度逐一评估，结论为"充分响应需求"。同时确认了关键证据：§1.2 风格一致性声明、§2.3 类图、§1.3 核心抽象表、§3 各组件状态模型等 |
| **问题7（版本号不一致）不应纳入**：违反 quality-challenger.md §4 中"不要关注文档校对、统计行数、数行号、文档版本号等与任务描述关系不大的细节" | **已移除**：问题 7 已从报告中删除 |
| **确认其余 6 个问题的证据准确性和严重程度评级** | **已验证确认**：问题 1~4（事实/逻辑类）经查阅 v11 产出文件中对应位置，问题仍然存在，严重程度评级维持不变。问题 2 额外发现 `extractOutputSummary()`（第 1497 行）同样未定义，已合入更新后的问题 2 描述。问题 5（YAML 配置不完整）和问题 6（缺测试策略）仍存在且评级合理。**新增问题 5**（§3.1 vs §4.1 不一致，严重）和**问题 6**（inputSummary 闭包假设不成立，重要），均为 v1 报告遗漏的逻辑矛盾类问题 |
