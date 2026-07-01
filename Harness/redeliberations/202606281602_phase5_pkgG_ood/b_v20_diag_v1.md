# 质量审查报告

**审查对象**: a_v20_copy_from_v19.md（Phase 5 包 G — AI 进阶底座 架构级 OOD 设计方案）
**审查轮次**: 第 1 轮（首次诊断）
**审查视角**: 需求响应充分度、事实正确性、逻辑一致性、深度与完整性、可落地性

---

## 查询 1：@RefreshScope 标记 SlidingWindowMetricsStore 将丢失全部滑动窗口数据

**问题描述**：§3.9「运行时配置热加载机制」表中将 `SlidingWindowMetricsStore` 标记为 `@RefreshScope` 以支持 `sliding-window.window-seconds` 的热加载（line 2548）。但 `SlidingWindowMetricsStore` 是持有运行时可变状态的核心组件——每个能力标识维护独立的 `Deque<WindowedEvent>`，累计 13 项能力在 60 秒窗口内最多可达 13 万条事件。`@RefreshScope` 的工作原理是销毁并重建 Bean 实例，这将**清空全部滑动窗口数据**，包括：所有能力的成功/失败/降级事件队列、熔断器的实时失败率判定依据、超时策略的耗时分布参考。

影响范围：
- 刷新后 `getFailureRate()` / `getEffectiveFailureRate()` / `getAverageElapsed()` 全部返回 0
- `buildDegradationContext()` 返回空上下文，降级预检循环在此期间无数据判定熔断状态
- 重新累积到有意义的窗口数据需要最多 `window-seconds`（默认 60 秒）的全新事件填充
- 在这 60 秒窗口内，熔断器因无历史数据而保持 CLOSED，请求可能全部放行到已经降级的端点

文档中 §3.9 定义了其他 3 项配置（timeout、strategies）使用自定义定时刷新 + `AtomicReference` 全量替换方式避免 Bean 重建，唯独 `sliding-window.window-seconds` 采用了 `@RefreshScope` 方案。`window-seconds` 是一个极少需要运行态调整的静态配置参数，不值得为此牺牲滑动窗口数据的完整性。

**所在位置**：§3.9「运行时配置热加载机制」表（line 2548）、`SlidingWindowMetricsStore` 构造器注解说该 Bean 标注 `@RefreshScope`（line 2548）

**严重程度**：严重

**改进建议**：
1. **方案 A（推荐）**：移除 `SlidingWindowMetricsStore` 上的 `@RefreshScope`，改为通过 `AtomicLong` 持有 `windowSeconds` 值。在 `AiPlatformConfig` 中新增定时刷新方法（与 `refreshCapabilityTimeoutConfig()` 风格一致），每 60 秒从 `Environment` 重新读取 `windowSeconds` 并通过 `AtomicReference` 或 `AtomicLong` 原子更新。惰性淘汰阈值 `System.currentTimeMillis() - windowSeconds * 1000` 在每次 `record*()` 和 `get*()` 方法执行时读取最新值。
2. **方案 B（折衷）**：声明 `SlidingWindowMetricsStore` 不支持热加载，添加 `[静态·重启生效]` 标记，与 §9.5 中的 `lightweight-endpoint` 等同级处理。`window-seconds` 的变化频率极低（通常仅在部署时确定），要求重启实例是可接受的代价。
3. **方案 C（备选）**：将 `SlidingWindowMetricsStore` 拆分为配置持有层（无状态，`@RefreshScope`）和数据存储层（有状态，普通 `@Component`）。配置持有层通过观察者模式通知存储层更新窗口参数，数据存储层内的过期事件在惰性淘汰时使用新窗口阈值。

---

## 查询 2：Phase4BusinessException 缺少过渡期兼容性——未迁移的 Phase 4 模块业务异常将被误分类为基础设施异常

**问题描述**：§4.2 薄适配器 catch 块（line 3467）使用 `instanceof Phase4BusinessException` 区分业务异常与基础设施异常。此协议要求全部 6 个 Phase 4 模块的业务异常类统一继承新增的 `Phase4BusinessException` 抽象基类。但文档明确说明此基类为"新增"（line 1099），6 个 Phase 4 模块当前的异常类（`DiagnosisException`、`InspectionException`、`LabTestException`、`ImageAnalysisException`、`ExaminationException`、`ExecutionOrderException`）均未继承此类。

文档列出了 6 个模块的异常继承状态（line 1088-1095），其中 3 个标注为 "△ 需验证"，但 catch 块代码中没有遗留任何基于字符串匹配的过渡期回退。在底座切流后、Phase 4 模块完成异常基类继承改造前的过渡窗口内：
- 所有未继承 `Phase4BusinessException` 的 Phase 4 业务异常（如参数校验失败、数据不存在）将落入 `else` 分支（line 3476）
- 被归类为基础设施异常 `DegradationReason.INFRASTRUCTURE_ERROR`
- 触发薄适配器降级路径（`doDegrade()`）
- 本应返回 `AiResult.failure()` 的非降级业务失败被错误降级

**所在位置**：§3.1「异常处理规则统一契约」（line 1097-1101）、§4.2 薄适配器特化管线伪代码 catch 块（line 3462-3479）

**严重程度**：严重

**改进建议**：
在 `instanceof Phase4BusinessException` 主分支前增加过渡期回退检查。回退策略可以复用文档中已定义的"异常类名回退"机制（§3.1 line 1100）——在 catch 块中检查 `originalCause` 的简单类型名是否匹配已知的 Phase 4 业务异常类名列表。当 `Phase4BusinessException` 检测失败时，fallback 到字符串匹配。此回退在跨包协作会议确认全部 Phase 4 模块完成异常基类继承后移除。过渡期伪代码示意：

```
if originalCause instanceof Phase4BusinessException:
    // Phase 4 模块已完成基类继承改造后的主路径
    ...
else if originalType in ["DiagnosisException", "InspectionException", ..., "ExecutionOrderException"]:
    // 过渡期回退：Phase 4 模块尚未迁移到 Phase4BusinessException 基类
    elapsedMs = System.currentTimeMillis() - startTime
    errorCode = "PHASE4_" + originalType
    ...
else:
    // 基础设施异常
    ...
```

---

## 查询 3：LocalRuleFallback.fallback() 空返回值未加 null 保护

**问题描述**：`doDegrade()` 方法（line 3287-3288）在调用 `localRuleFallback.fallback(request)` 后直接对返回值调用 `.toString()`：

```java
result = localRuleFallback.fallback(request)
outputSummary = outputSummary != null ? outputSummary : StringUtils.truncate(result.toString(), 500)
```

以下任一场景中 `result` 可能为 null：
- 实现类未在 `fallback()` 方法中返回非 null 值（`LocalRuleFallback` 接口契约缺少非 null 约定）
- 默认实现（无 `LocalRuleFallback` 时）不经过此分支——OK
- 但自定义实现因编程错误意外返回 null

`result.toString()` 在 result 为 null 时将抛出 NPE，导致整个 `doDegrade()` 方法异常退出，异常传播到 `execute()` 的 `.exceptionally()` 回调，最终因异常类型非 `TimeoutException` 而执行 `throw new CompletionException(ex)`，端点健康状态无法更新，指标也无法记录。

**所在位置**：§4.1 doDegrade() 方法（line 3287-3288）

**严重程度**：重要

**改进建议**：
在 `result = localRuleFallback.fallback(request)` 后增加 null 守卫：

```
result = localRuleFallback.fallback(request)
if result == null:
    log.error("LocalRuleFallback 返回 null: capabilityId={}, fallbackClass={}", 
        capabilityId, localRuleFallback.getClass().getSimpleName())
    // 跳过本地规则结果，直接返回 AiResult.degraded()
    outputSummary = outputSummary != null ? outputSummary : null
    metricsCollector.record(AiCallRecord.degraded(..., outputSummary, ...))
    slidingWindowMetricsStore.recordFailure(capabilityId)
    return AiResult.degraded(degradeReason)
```

同时应在 `LocalRuleFallback` 接口的 Javadoc 中以 `@return` 明确约定"返回非 null 的 `AiResult<R>` 实例"。

---

## 查询 4：@ConditionalOnClass 引用的 Spring AI ChatModel 包路径可能不准确

**问题描述**：§3.2 `DelegatingLlmChatService` 的 Spring AI 可选依赖保护（line 1486）和 Bean 装配方式（line 1510）中，使用 `@ConditionalOnClass(name = "org.springframework.ai.chat.ChatModel")` 检测 Spring AI 是否在 classpath 上。在 Spring AI 1.0.x 版本中，`ChatModel` 接口的完整限定名为 `org.springframework.ai.chat.model.ChatModel`（注意 `model` 子包），而非 `org.springframework.ai.chat.ChatModel`。

若项目中引入的 Spring AI 版本使用 `chat.model.ChatModel` 包路径，此 `@ConditionalOnClass` 条件将**永不匹配**，`SpringAiLlmChatService` 和 `SpringAiLlmChatStreamService` 始终不注册，即使 Spring AI 依赖已正确引入。底座将始终使用 `HttpApiLlmChatService`，无法利用 Spring AI ChatModel 的抽象能力。

**所在位置**：§3.2 DelegatingLlmChatService（line 1486）、AiPlatformConfig @Bean 装配（line 1510）

**严重程度**：一般

**改进建议**：
将条件声明改为：
```java
@ConditionalOnClass(name = {"org.springframework.ai.chat.model.ChatModel", "org.springframework.ai.chat.ChatModel"})
```
即同时兼容两种可能的包路径——Spring AI 1.0.x 的 `chat.model.ChatModel` 和更早期版本的 `chat.ChatModel`。或在注释中标注当前使用的 Spring AI 版本号及对应的准确包路径，由实现者根据项目实际引入的版本确认。

---

## 查询 5：DiscussionConclusionCapabilityExecutor 前置压缩失败与主流程超时叠加时降级原因优先级未定义

**问题描述**：§4.1 `DiscussionConclusionCapabilityExecutor.doExecuteInternal()` 特化伪代码（line 3317-3422）中，前置压缩调用（`transcriptSummary`，超时阈值 15s）失败时回退到截断文本（不再降级主流程）。但存在以下边界场景未定义：

当 `transcriptSummaryTimeout`（15s）消耗了大量 `capabilityTimeout` 窗口（例如 `DISCUSSION_CONCLUSION` 配置为 90s），压缩调用在 14.5s 时超时失败，主流程 LLM 调用启动时仅剩余约 75.5s。但模板渲染 + 实验分流 + 模型路由等步骤已在压缩阶段之前完成，因此实际影响不大。

更关键的边界场景：当 `transcriptSummaryTimeout + 主流程 LLM 调用耗时 > capabilityTimeout` 时，`orTimeout()` 兜底超时触发，降级原因记录为 `DegradationReason.TIMEOUT`。但此时的前置压缩调用可能已成功完成（返回了摘要），而失败的是主 LLM 调用。运维排查时看到 `TIMEOUT` 降级原因，无法区分是"压缩阶段占用了过多时间导致主流程超时"还是"主 LLM 调用本身超时"。两场景的根因完全不同——前者应调整压缩超时或模型性能，后者应调整能力超时阈值或模型路由。

**所在位置**：§4.1 DiscussionConclusionCapabilityExecutor 特化伪代码（line 3317-3422）、§3.11.7 模板变量提取策略

**严重程度**：一般

**改进建议**：
在 `doExecuteInternal()` 的标准 `exceptionally()` 回调中，增加超时降级原因的细化判断逻辑：

```
exceptionally(ex -> {
    if ex instanceof TimeoutException:
        // 检查压缩阶段是否已消耗了大部分超时窗口
        if elapsedBeforeCompression + transcriptSummaryTimeout.toMillis() 
           > capabilityTimeout.toMillis() * 0.8:
            // 降级原因指示"压缩阶段超时挤占主流程窗口"
            return doDegrade(..., DegradationReason.TIMEOUT + ":transcriptSummaryCrowding", ...)
        else:
            // 主流程 LLM 调用本身超时
            return doDegrade(..., DegradationReason.TIMEOUT + ":primaryLlmTimeout", ...)
})
```

或在日志中区分两种场景，确保运维排障时可以从日志中定位根因。同时在 §5.1 错误分类表中补充此边界场景的说明。

---

## 查询 6：AiCallRecord 工厂方法 16 参数在实现阶段的高阶编码风险

**问题描述**：§3.5 定义的 `AiCallRecord.success()` 工厂方法包含 16 个参数（line 2081-2086）。类似地，`AiCallRecord.failure()` 15 参数、`AiCallRecord.degraded()` 16 参数、`AbstractCapabilityExecutor` 构造器 13+ 参数、`doDegrade()` 15 参数。文档本身在 §3.1（line 1435）的"参数数量说明"段落中承认此为"已知设计约束"，建议 Phase 6 或实施期引入上下文参数对象。

实际编码实现时的风险：
- 16 个参数均为 Java 方法参数，编译器不提供**命名参数检查**——`visitId` 和 `patientId` 同为 `String` 类型，调用方极易混淆且编译器无法检测
- `promptVersion`（`Integer` 可空）与 `sentinelReason`（`@Nullable String`）类型不同但语义耦合，传错时编译器仅对 `sentinelReason` 做类型检查而 `promptVersion` 的 null/非 null 全靠人工核对
- 16 个参数中约 10 个（`departmentId`、`callerRole`、`callerId`、`visitId`、`patientId`、`sessionId`、`inputSummary`、`outputSummary`、`promptVersion`、`sentinelReason`）作为一组"业务上下文"在多个方法间传递，排参顺序不一致——`doDegrade()` 的参数顺序为 `..., callerRole, callerId, visitId, patientId, sessionId, inputSummary, outputSummary, promptVersion, modelId, sentinelReason`，而 `AiCallRecord.success()` 的顺序为 `..., modelId, retryCount, promptTokens, completionTokens, inputSummary, outputSummary, visitId, patientId, sessionId, callerRole, callerId, promptVersion, sentinelReason`，两者排列不一致，跨方法传参时极易错位

**所在位置**：§3.5 工厂方法签名（line 2081-2098）、§3.1 构造器（line 1409-1424）、§4.1 doDegrade() 签名（line 3274）

**严重程度**：重要

**改进建议**：
**在 Phase 5 实施期（非 Phase 6）立即引入上下文对象**。具体建议：
1. 抽取 `CallContext` 值对象，聚合以下 9 个字段：`departmentId`、`callerRole`、`callerId`、`visitId`、`patientId`、`sessionId`、`inputSummary`、`outputSummary`、`promptVersion`、`sentinelReason`（10 个参数压缩为 1 个值对象）
2. `AiCallRecord` 工厂方法签名从 16 参数降维至 `success(capabilityId, callTime, elapsedMs, modelId, retryCount, tokenUsage, CallContext)`（约 7 参数）
3. `doDegrade()` 方法签名从 15 参数降维至 `doDegrade(startTime, degradeReason, request, capabilityId, CallContext, modelId)`（约 6 参数）
4. 上下文对象的构建发生在 `execute()` 入口处（`supplyAsync()` 之前），通过 Builder 模式确保必填字段不可遗漏

理由：文档将此重构推到 Phase 6，但 Phase 5 是实际编码阶段，13 个 CapabilityExecutor 实现、6 个薄适配器实现、doDegrade() 多路径调用点将产生大量 16 参数调用代码。15 个参数顺序跨越多个方法不一致，是经过 20 轮迭代验证的高概率编码错误源，不值得为了"上下文更新的可追踪性"（文档所述理由）承受此风险。

---

## 总结

| 序号 | 严重程度 | 问题领域 | 是否影响可落地性 |
|------|---------|---------|----------------|
| 1 | 严重 | SlidingWindowMetricsStore 数据完整性 | 是——刷新后熔断器保护失效 |
| 2 | 严重 | Phase4BusinessException 过渡期兼容性 | 是——未迁移模块的业务异常被错误降级 |
| 3 | 重要 | LocalRuleFallback 空指针风险 | 是——可导致 doDegrade() 异常退出 |
| 4 | 一般 | Spring AI 条件类名准确性 | 是——条件判断可能永远不匹配 |
| 5 | 一般 | 压缩调用的超时降级原因二义性 | 否——可通过日志辅助定位 |
| 6 | 重要 | 16 参数工厂方法的编码实施风险 | 是——参数错位是概率性编码错误 |

**整体评价**：文档经过 20 轮迭代后整体质量较高，核心架构思路清晰、抽象层次合理、设计决策记录完整。上述问题集中在过渡期兼容性设计（查询 1、2、3）和编码实施阶段的可操作细节（查询 3、4、6）两个维度——这是内部审议（侧重技术正确性）可能未充分覆盖的领域。建议在实施阶段优先处理查询 1、2、3、6，查询 4、5 可在实现过程中确认修复。
