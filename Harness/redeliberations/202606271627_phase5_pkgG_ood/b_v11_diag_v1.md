# 质量审查报告 — Phase 5 包 G OOD 设计（v11）

## 审查范围

- **待审查产出**：`a_v11_copy_from_v10.md`
- **用户需求**：`requirement.md`
- **迭代轮次**：第 11 次
- **审查视角**：需求响应充分度、事实/逻辑正确性、深度与完整性（侧重内部审议未覆盖的维度）

---

## 问题列表

### 问题 1：[事实错误] `LlmResponse` 缺少 `retryCount` 字段定义，但管线伪代码中直接调用 `getRetryCount()`

- **所在位置**：§2.3 类图 `LlmResponse`（第 290-294 行）、§3.2 `LlmResponse` 文本定义（第 964-966 行）、§4.1 `doExecuteInternal()` 伪代码第 1473 行
- **严重程度**：严重
- **问题描述**：`LlmResponse` 在类图和文本段落中仅定义了 `text`、`tokenUsage`、`modelId` 三个字段。但 §4.1 管线伪代码第 1473 行调用 `llmResponse.getRetryCount()` 并赋值给 `retryCount` 变量，随后传入 `AiCallRecord.success()` 工厂方法。缺少该字段定义将导致实现阶段出现编译错误或遗漏关键调用信息。
- **改进建议**：在 `LlmResponse` 的类图和文本定义中补充 `retryCount: int` / `getRetryCount()` 字段，并说明其填充来源（由 `LlmClient` 内部重试计数器提供，类图中无需暴露方法，但字段应存在）。

### 问题 2：[事实错误] `doExecuteInternal()` 中调用 `extractParsedSummary()` 但该方法在所有契约中均未定义

- **所在位置**：§4.1 `doExecuteInternal()` 伪代码第 1477 行
- **严重程度**：严重
- **问题描述**：成功管线中，`structuredOutputParser.parse()` 成功后调用 `outputSummary = extractParsedSummary(parsedResult)` 将输出摘要从解析后的 DTO 中提取。但 `extractParsedSummary()` 未在 `AbstractCapabilityExecutor`、`CapabilityExecutor` 接口、任何 helper 类或工具方法中定义。实现者无法确认该方法的签名、返回值语义（可选还是必选？截断长度？）以及它在不同能力间的复用方式。
- **改进建议**：在 `AbstractCapabilityExecutor` 中定义一个默认的 `extractParsedSummary(R parsedResult)` 方法（默认使用 `parsedResult.toString()` 截断 500 字符，与 `inputSummary` 逻辑一致），子类可按需重写。或在 `doExecuteInternal()` 中直接复用 `outputSummary = StringUtils.truncate(parsedResult.toString(), 500)` 避免引入未定义的 helper 方法。

### 问题 3：[逻辑矛盾] 薄适配器型 `CapabilityExecutor` 在已运行于 `llmCallExecutor` 线程池的上下文中嵌套提交 `supplyAsync()` 到同一线程池

- **所在位置**：§3.1 薄适配器伪代码第 730-731 行，结合 §4.1 `AbstractCapabilityExecutor.execute()` 第 1424-1426 行
- **严重程度**：重要
- **问题描述**：`AbstractCapabilityExecutor.execute()` 通过 `supplyAsync(() -> doExecuteInternal(...), llmCallExecutor)` 提交管线到共享线程池。薄适配器的 `doExecuteInternal()` 内部再次调用 `supplyAsync(() -> phase4ServiceDelegate.execute(request), llmCallExecutor)` 提交到**同一个** `llmCallExecutor`，然后通过 `.get(thinAdapterTimeout)` 阻塞等待。这创造了线程池嵌套消费模式：外层占有一个池线程，内层从同一池申请另一线程。虽然 `CallerRunsPolicy` 在池满时会将内层回退到外层线程运行（避免了死锁），但此模式有两个问题：(1) 正常情况下每个薄适配器调用消耗 2 个池线程，减少了并发容量；(2) 设计文档未说明此嵌套模式的正确性依据，实现者可能误改为不同的拒绝策略（如 `AbortPolicy`）导致运行时死锁。
- **改进建议**：(a) 将薄适配器的 `phase4ServiceDelegate.execute(request)` 直接在外层线程同步调用（无需内层 `supplyAsync`），仅包裹 `CompletableFuture.supplyAsync(() -> phase4ServiceDelegate.execute(request)).get(timeout)` 使用公共 `ForkJoinPool` 而非 `llmCallExecutor`；或 (b) 在文档中显式说明嵌套 `supplyAsync` + `get()` 模式的设计理由及其对线程池容量的影响，并强调 `CallerRunsPolicy` 是此模式的前提条件。

### 问题 4：[关键遗漏] `ModelRoute.authentication` 字段类型为"(设计占位)"，无法直接指导编码实现

- **所在位置**：§3.2 `ModelRoute` 字段扩展表第 991 行
- **严重程度**：重要
- **问题描述**：`ModelRoute` 字段扩展表中 `authentication` 字段的类型标注为 `(设计占位)`，而非具体的 Java 类型。下游实现者无法据此生成 POJO 定义——它是一个 String？一个内部类？一个引用接口？设计文档声称认证凭据通过 Vault 按 `endpointId` 查询，但 `ModelRoute` 中仍需要一个字段或标记来指示该端点的认证方式（如 API-Key / OAuth / 无认证），而非直接存储密钥。
- **改进建议**：将 `authentication` 改为具体类型声明，例如 `authType: AuthType enum (API_KEY / OAUTH2 / NONE)`，或从字段表中删除并改为在 `LlmClient` 的实现说明中描述认证凭据获取机制。不应保留不可实现的占位符。

### 问题 5：[完整性不足] YAML 配置示例中 7 项底座能力的超时配置只覆盖了 3 项

- **所在位置**：§9.5 YAML 配置第 1822-1832 行
- **严重程度**：一般
- **问题描述**：`execution.timeout.per-capability` 显式列出了 7 项底座能力中的 3 项（TRIAGE、RX_AUDIT、MEDICAL_RECORD_GEN），其余 4 项（RX_ASSIST、KB_QUERY、SCHEDULE、DISCUSSION_CONCLUSION）依赖 `default: 60s`。虽不构成设计错误，但 "各能力独立配置" 的承诺在 YAML 示例中未兑现。对于 `KB_QUERY` 这种"首次真实实现于底座"的能力，60 秒默认值是否合理无从判断。6 项薄适配器的超时也全部依赖 `thin-adapter-default: 30s`。
- **改进建议**：至少在注释中为每种能力给出超时值选择依据，或完整填充全部 13 项能力的超时配置（即使部分与默认值相同），让实现者看到完整的配置形态。

### 问题 6：[完整性不足] 未提供任何测试策略或可验证性指导

- **所在位置**：全文
- **严重程度**：一般
- **问题描述**：设计文档覆盖了 13 个 CapabilityExecutor、5 个基础设施组件、2 层编排，但未提供任何关于如何单元测试、集成测试或模拟组件的指导。例如：(1) `AbstractCapabilityExecutor` 的模板方法如何单测？（子类 mock + 验证降级预检不可绕过）；(2) `LlmClient` 的 HTTP 调用如何在测试中模拟？（WireMock / Mockito）；(3) `AiPlatformConfig` 的条件 Bean 装配如何在测试环境配置？(4) 管线伪代码中的降级路径如何通过测试覆盖全部组合？设计文档作为实现指导，缺少测试策略将影响落地质量。
- **改进建议**：在文档末尾新增"测试策略"章节（或扩充 §8），建议至少覆盖：(a) 单元测试模式——每个 CapabilityExecutor 使用 `@MockBean` 模拟所有下游依赖，验证降级路径触发条件；(b) 集成测试模式——`@SpringBootTest` + `@TestConfiguration` 模拟底座激活状态；(c) 管线收敛验证——关键决策（降级判定前置、防御性拷贝、异步上下文传播）的可测试性证明。

### 问题 7：[逻辑矛盾/事实错误] 文档标题版本号与文件路径、迭代轮次不一致

- **所在位置**：§ 标题第 1 行（"v13"）vs 文件路径 `a_v11_copy_from_v10.md` vs 任务描述的"第 11 次迭代"
- **严重程度**：一般
- **问题描述**：文档标题标注为 `（v13）`，修订说明最新条目为 `修订说明（v13）`，但文件名为 `a_v11_copy_from_v10.md`（暗示版本为 v11）。此问题在历史迭代中已被指出并声称修正（迭代 5 Q12、迭代 7 Q12），至今仍未解决。版本号混乱使下游读者无法确定当前文档的迭代基底。
- **改进建议**：统一版本号标识——文件路径使用 `a_v11_...` 则标题和修订说明应为 `（v11）`，反之亦然。建议采用语义化版本或与迭代轮次一一对应的版本标签。

---

## 整体质量评价

产出已达到较高成熟度，覆盖了 13 项 AI 能力的编排框架、模型对接、模板管理、A/B 实验、性能观测等核心设计维度，类图、伪代码、状态模型、设计决策等 OOD 要素基本完整。经过多轮迭代审议，多数重度问题已在历次修订中解决。

当前版本（v11）的主要质量短板集中在**具体抽象的字段完整性**和**异常设计模式的正确性证明**上——即"设计说清楚了，但部分细节未定义到可编码的程度"。问题 1、2、3 是必须解决的实现阻塞项；问题 4 是编码歧义项；问题 5、6 是深度不足项。建议修复者在进入编码前先解决问题 1~4。
