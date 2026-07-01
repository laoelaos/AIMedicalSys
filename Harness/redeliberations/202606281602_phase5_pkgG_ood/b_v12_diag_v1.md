# 质量审查报告 — Phase 5 包G OOD 设计方案 v12

## 审查范围

- **任务**：Phase 5 包G AI 进阶底座 OOD 设计方案质量审查
- **审查视角**：实际落地评估——设计是否可直接指导编码实现、接口定义是否足以支持下游消费者、异常场景和边界条件是否已考虑
- **侧重维度**：需求响应充分度、事实正确性、逻辑一致性、深度与完整性（内部审议已覆盖技术可行性等维度，本审查避免重复验证）

---

## 问题列表

### 问题 1：[严重] §4.1 完整管线降级路径存在系统性指标双记，导致计数器严重膨胀

**问题描述**：
`AbstractCapabilityExecutor.doExecuteInternal()` 中，`structuredChat` 调用失败后的三个降级路径（AiResult 非成功状态、TimeoutException、LlmInfrastructureException）在调用 `doDegrade()` 之前均抢先执行了 `metricsCollector.record(AiCallRecord.failure(...))` 和 `slidingWindowMetricsStore.recordFailure(capabilityId)`。然而 `doDegrade()` 方法内部同样会记录指标（`AiCallRecord.degraded()` + `slidingWindowMetricsStore.recordDegraded()` 或 `recordFailure()`），导致**同一调用事件被记录两次**。

具体影响：
- `metricsCollector` 收到两条记录（`failure` + `degraded`），而应为一条
- `SlidingWindowMetricsStore` 的失败计数被膨胀（当无 `LocalRuleFallback` 时，一次结构化调用失败在滑动窗口中记为 2 次失败），可能触发熔断器 false positive
- 这与 §4.2 薄适配器的降级路径形成对比——薄适配器降级路径**不**主动预记录指标，仅通过 `doDegrade()` 统一记录，行为正确

**所在位置**：§4.1 `doExecuteInternal()`，具体行（按伪代码标注）：
  - 结构化调用 AiResult 非成功状态：行 2903–2908
  - `structuredChat` TimeoutException：行 2919–2924
  - `LlmInfrastructureException`：行 2994–2999

**严重程度**：严重

**改进建议**：统一降级路径的指标记录契约——**只有两个入口记录指标**：
1. 成功路径：`doExecuteInternal()` 末尾的 `metricsCollector.record(AiCallRecord.success(...))` + `slidingWindowMetricsStore.recordSuccess()`
2. 降级路径：统一由 `doDegrade()` 内部记录 `AiCallRecord.degraded()` + `slidingWindowMetricsStore.recordDegraded()/recordFailure()`
   
   删除上述三个降级路径中 `doDegrade()` 调用之前的所有 `metricsCollector.record()` 和 `slidingWindowMetricsStore.recordFailure()` 预记录代码。`endpointHealthManager.recordCallResult()` 的调用不受此约束，可保留在 `doDegrade()` 之前（因其记录端点级别的健康视图，非调用次数统计）。

---

### 问题 2：[重要] 文档头部「变更摘要」声明与正文内容存在事实性矛盾

**问题描述**：
文档头部 §变更摘要 明确声明"历史修订说明已剥离归档（见 `design_evolution_log.md`）"，但在文档尾部 §3774–§3850 仍然保留着 v6、v7、v8、v9、v10、v11、v12 共 7 个完整的「修订说明」块。读者无法判断"历史修订说明"的边界——是 v1~v5 已剥离而 v6+ 保留，还是全部应已剥离但遗漏。此矛盾影响文档作为"最终稳定版本"的可交付物严肃性。

**所在位置**：§变更摘要（行 3）vs §修订说明（v6）至 §修订说明（v12），行 3774–3850

**严重程度**：重要

**改进建议**：方案 A：执行声明内容，将 v6~v12 的修订说明全部剥离至 `design_evolution_log.md`，保留"§变更摘要"声明不变。方案 B：更新 §变更摘要 中的声明，精确描述保留范围（如"v1~v5 修订说明已剥离归档，v6~v12 迭代反馈记录保留于文档尾部"），消除矛盾。

---

### 问题 3：[一般] structuredChat AiResult 非成功状态的降级原因语义不清晰

**问题描述**：
§4.1 `doExecuteInternal()` 中，当 `structuredChat()` 返回的 `AiResult.isSuccess() == false` 时，降级原因使用 `DegradationReason.INTERNAL_ERROR`。但 `structuredChat` 本身调用成功（HTTP 返回 200、无异常抛出）只是返回结果标记为失败——这不属于"内部错误"语义。更准确地说，这是 LLM 返回了非结构化内容或内容校验失败的情况，应使用 `DegradationReason.PARSE_FAILURE` 或新增的原因常量。

此外，该路径还导致输出摘要 `outputSummary` 从 `aiResult.getErrorMessage()` 提取（行 2900），但 `doDegrade()` 中当 `localRuleFallback != null` 时又用 `result.toString()` 覆盖 `outputSummary`（行 3042），同一降级调用中 `outputSummary` 含义两次变更，增加日志排查时的认知负担。

**所在位置**：§4.1 `doExecuteInternal()`，行 2898–2908

**严重程度**：一般

**改进建议**：
1. 将降级原因从 `DegradationReason.INTERNAL_ERROR` 改为 `DegradationReason.PARSE_FAILURE + ":structuredChatResultNotSuccess"`，与 §5.1 错误分类表中"结构化输出解析失败"的分组语义一致
2. 明确 `outputSummary` 在此路径中的定位：若 `doDegrade()` 中 `localRuleFallback` 会重新生成输出，则不在预记录中设置 `outputSummary`（传 null 由 `doDegrade()` 按统一逻辑填充）；若此路径为纯降级（无 fallback），则在预记录中设置 `outputSummary` 并在 `doDegrade()` 中保留传入值

---

## 整体质量评价

该文档整体深度和完整度很高（3850 行），覆盖了架构概述、模块划分、类图、13 个核心抽象定义、关键行为伪代码、错误处理、并发设计、设计决策、Maven 依赖、迁移路径、YAML 配置、跨包协作边界和测试策略等 OOD 核心要素，可直接指导编码实现。需求响应充分——明确参照了 Phase0/Phase1ABD 的设计风格，所有 7 项底座能力和 6 项薄适配器能力均有对应的设计覆盖。

主要质量风险集中在 **§4.1 伪代码层面的指标双记问题**（问题 1），该问题会在运行时导致降级次数统计严重失真，应优先修复后再进入编码阶段。文档的交付物表述一致性（问题 2）和降级原因语义精度（问题 3）为次要问题，可在后续迭代中同步修正。
