# 质量审查报告 — a_v23_design_v3.md

审查执行时间：第 23 轮迭代
审查范围：需求响应充分度、事实/逻辑正确性、深度与完整性、落地可行性

---

## 问题1：标准 `doExecuteInternal()` 伪代码缺少成功路径的指标记录与返回逻辑

- **问题描述**：§4.1 标准 `doExecuteInternal()` 伪代码中，structuredChat 成功路径（行 3235~3240 的 `if aiResult.isSuccess()` 分支）和 chat 回退成功路径（行 3276~3297 的 `if chatAiResult.isSuccess()` 分支）均只设置了 `parsedResult`/`retryCount`/`tokenUsage` 变量并调用 `endpointHealthManager.recordCallResult()`，但 **既没有调用 `metricsCollector.record(AiCallRecord.success(...))` 和 `slidingWindowMetricsStore.recordSuccess(...)`，也没有 `return AiResult.success(parsedResult)`**。两个成功分支的代码执行完毕后均直接"坠落"出 try-catch 结构体，未返回任何结果。这意味着实现者按该伪代码编码时，成功路径不会记录调用指标且方法无有效返回值，将导致管线在所有正常情况下静默失败（fall through 后可能调用到 `doDegrade()` 或返回 null）。
- **所在位置**：§4.1 `doExecuteInternal()` 伪代码，structuredChat 成功分支（行 3235~3240）及 chat 回退成功分支（行 3279~3297），即两个成功路径末尾均缺少以下代码模式：
  ```java
  outputSummary = extractOutputSummary(parsedResult);
  metricsCollector.record(AiCallRecord.success(capabilityId, LocalDateTime.now(), elapsedMs,
      modelRoute.getModelId(), retryCount, tokenUsage, departmentId, inputSummary, outputSummary,
      visitId, patientId, sessionId, callerRole, callerId, promptVersion, sentinelReason));
  slidingWindowMetricsStore.recordSuccess(capabilityId, elapsedMs);
  return AiResult.success(parsedResult);
  ```
- **严重程度**：严重
- **改进建议**：在两个成功路径的末尾（structuredChat 成功分支的第 3240 行之后、chat 回退成功解析分支的 `catch (ParseException)` 之前）补充指标记录和返回逻辑。需注意 `outputSummary` 需通过 `extractOutputSummary(parsedResult)` 从解析结果中提取（当前两个成功路径均未计算 `outputSummary`），`elapsedMs` 需从 `System.currentTimeMillis() - startTime` 计算。建议将成功路径公用的"计算 outputSummary + 记录指标 + 返回"提取为一个局部辅助逻辑块，在两个成功分支末尾复用，避免伪代码重复。

---

## 问题2：degradationStrategyMap 热加载机制与 CapabilityExecutor 注入方式不一致，热替换失效

- **问题描述**：§3.9 明确规定 `degradation.strategies` 支持运行时热加载——"自定义定时刷新 + 策略列表热替换"，通过 `refreshDegradationStrategies()` 方法"替换 `Map<String, List<DegradationStrategy>>` 实例后通过 `AtomicReference` 发布"。然而，§3.1 明确 CapabilityExecutor 通过构造器注入 `@Qualifier("degradationStrategyMap") Map<String, List<DegradationStrategy>> degradationStrategyMap`，该引用在 Bean 初始化时**一次性捕获为实例字段**（行 1482: `this.degradationStrategyMap = degradationStrategyMap`）。Spring 单例 Bean 的 `@Bean` 方法调用后，原先注入的 Map 引用不会被 `AtomicReference` 发布自动更新——即使 `AiPlatformConfig` 重建了新 Map 实例，已有 CapabilityExecutor 实例持有的 `this.degradationStrategyMap` 仍然指向旧 Map。两处描述自相矛盾：热加载机制声称可运行时替换，但注入机制决定了替换无法生效。
- **所在位置**：§3.1 降级策略注入机制（行 1020~1038）及构造器伪代码（行 1482）；§3.9 运行时配置热加载机制 degradation.strategies 行（行 2628~2629）
- **严重程度**：重要
- **改进建议**：统一为一种可实现的设计。推荐以下方案之一：
  - **方案 A（推荐）**：CapabilityExecutor 改为注入 `ObjectProvider<Map<String, List<DegradationStrategy>>>` 或 `AtomicReference<Map<String, List<DegradationStrategy>>>`，在每次 `execute()` 调用时通过 `get()` 获取最新 Map。这使热刷新即时生效。
  - **方案 B**：放弃 `degradation.strategies` 的运行态热刷新，改为重启生效（与 `sliding-window.window-seconds` 的处理方式对齐），在 §9.5 注释中标注为 `[静态·重启生效]`。
  - 方案 C（过渡）：接受当前启动期一次性绑定的现状，移除热加载相关描述，改为在发版说明中标注后续 Phase 6 支持。

---

## 问题3：文档版本号声明与修订历史不一致

- **问题描述**：文档标题行（第 1 行）标注"（v21）"，变更摘要（第 3 行）声明"v7~v21 修订说明保留于尾部"，但尾部实际内容包含 v22、v23、v24、v25 共 4 轮补充修订说明（行 4468~4499）。标题和摘要均未反映 v22~v25 已正式并入正文的事实。同时，文件名 `a_v23_design_v3.md` 中的 "v23" 与文档内版本号 "v21" 也不对应，实施者参考时可能对文档的当前成熟度产生误判。
- **所在位置**：第 1 行（标题）、第 3 行（变更摘要）、行 4468~4499（v22~v25 修订说明表）
- **严重程度**：一般
- **改进建议**：将标题更新为"(v25)"，变更摘要中的范围改为"v7~v25"，同步更新文件名使其与文档版本一致，或将文件版本号后缀与文档内版本号统一定义。

---

## 问题4：§4.1 伪代码中 `catch (TimeoutException)` 因 `orTimeout().join()` 仍为死代码

- **问题描述**：第 11 轮迭代反馈（问题 3）已指出 `catch (TimeoutException)` 在 `CompletableFuture.join()` 包装下为死代码，并将捕获逻辑改为在 `catch (CompletionException)` 中检查 `ce.getCause() instanceof TimeoutException`（参见 v12 修订说明第 7 条）。然而 §4.1 中 `orTimeout(...).orTimeout(...)` 链式调用结尾的 `.join()` 被外层 `try-catch (Exception e)` 包裹后，`catch (TimeoutException)` 块的存在位置和实际可到达性仍不清晰。具体而言，外层 try-catch 在行 3226~3326 使用了 `catch (StructuredOutputNotSupportedException)` 和 `catch (LlmInfrastructureException)`，而 `TimeoutException` 由内部 `catch (CompletionException ce)` 拆解后处理。但如果存在独立 `catch (TimeoutException)` 块（如行 3298），其可到达性取决于 `CompletableFuture.get()` 直接抛出 `TimeoutException` 的场景（与 `.join()` + `orTimeout()` 的包装行为不同）。当前伪代码在 chat 回退的 parse 阶段使用 `.get(timeout, unit)`（行 3297），该调用可直接抛出 `TimeoutException`，因此该位置的 `catch (TimeoutException)`（行 3298）是可到达的——此为正确设计。但结构化调用阶段（行 3231~3232）使用 `.join()`，若存在针对此处的 `catch (TimeoutException)` 则为死代码。当前伪代码在结构化阶段使用 `catch (CompletionException)` 处理，设计正确。**问题在于**：行 3310 的 chat 回退路径也使用 `catch (CompletionException ce)` 拆解（与结构化路径一致），但 chat 回退路径的 `chatFuture.orTimeout(...).join()` 同样会将 `TimeoutException` 包装为 `CompletionException`，因此行 3313 的 `if (originalCause instanceof TimeoutException)` 分支逻辑正确，不存在独立 `catch (TimeoutException)` 死代码。**结论：此问题在 v12 后已修复，验证通过。** 但需确认：`exceptionally()` 回调处（行 3105）依赖 `orTimeout()` 抛出 `TimeoutException`，该异常由 `CompletionException` 包装后经回调参数 `ex` 走入——此处使用 `if (ex instanceof TimeoutException)`（行 3106）。**关键问题**：`orTimeout()` 返回的 `CompletableFuture` 的 `.exceptionally()` 回调接收的 `ex` 类型，当超时发生时，`ex` 是 `TimeoutException`（未经包装），还是 `CompletionException(TimeoutException)`？查阅 Java 文档，`CompletableFuture.orTimeout()` 在超时时内部调用 `completeExceptionally(new TimeoutException())`，`.exceptionally()` 回调接收的 `ex` 是原始的 `TimeoutException` 而非 `CompletionException` 包装。因此行 3106 的 `if (ex instanceof TimeoutException)` 正确——`TimeouException` 直接到达回调。此分支无问题。**但是**，行 3106 的注释使用了 `ex instanceof TimeoutException`，而行 1359（旧的模板方法伪代码）也有类似的 `ex instanceof TimeoutException`。这两个位置的判断逻辑一致，均正确。
- **所在位置**：§4.1 `exceptionally()` 回调（行 3105~3117）
- **严重程度**：无需改进（验证确认设计正确）

---

## 问题5：`estimateTokens()` 的 jtokkit 精确 Tokenizer 分支缺少伪代码实现细节

- **问题描述**：§4.1 `DiscussionConclusionCapabilityExecutor.doExecuteInternal()` 特化伪代码中引入了精确 Tokenizer 分支（行 3445~3448 `if tokenizerAvailable: preciseTokenCount(transcripts)`），但未给出 `tokenizerAvailable` 的检测机制、`preciseTokenCount()` 的具体实现契约、jtokkit 库的依赖声明或回退到字符估算的判断逻辑。实现者无从知道如何判断"tokenizerAvailable"、何时加载 jtokkit、以及加载失败时的 fallback 策略。此外，§8 Maven 依赖清单中未列出 jtokkit（或任何 tiktoken 的 JVM 实现），实现者需自行发现和引入此依赖。
- **所在位置**：§4.1 `DiscussionConclusionCapabilityExecutor` 特化伪代码（行 3445~3451）；§8 Maven 依赖清单（未提及 jtokkit）
- **严重程度**：一般
- **改进建议**：在 §4.1 伪代码中补充 `tokenizerAvailable` 的判定逻辑（如通过 `try { Class.forName("com.knuddels.jtokkit.Encodings") }` 或 `@ConditionalOnClass`），为 `preciseTokenCount()` 补充方法签名和返回值契约。在 §8 依赖清单的"条件性依赖"段中新增 jtokkit 条目（`optional = true`），明确说明其在 doExecuteInternal() 中的用途。

---

## 整体评价

产出在历经 22 轮迭代修正后，**整体质量较高**：§1.8 非功能性质量分析的深度和完整性已达到可直接指导编码的级别；§3 核心抽象覆盖全面，职责/协作/决策理由三元组结构清晰；§4 伪代码（除上述问题 1 的关键遗漏外）对异常场景的覆盖已非常完备；§5~§7 设计决策和错误处理策略充分体现了实际落地考量。

遗留的主要质量风险集中在**两处**：
- **严重级**：`doExecuteInternal()` 成功路径缺失指标记录和返回逻辑（问题1），这是实现者直接接触的核心代码路径，遗漏将导致成功调用无记录、无返回。
- **重要级**：`degradationStrategyMap` 热加载声明的实现可行性矛盾（问题2），若按当前文档两套描述实施，热刷新将静默失效，运维侧会误以为运行态可调降级策略。

建议在下一轮迭代中优先处理上述两个问题，其余为次要补充完善。
