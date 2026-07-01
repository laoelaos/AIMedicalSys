# 质量审查报告：Phase 5 包 G OOD 设计（v12）

---

## 问题列表

### 问题 1：【严重】§5.1 错误分类表的异常容错承诺在 §4.1 伪代码中未兑现

**问题描述**：§5.1 错误分类表声明：
- 模板缺失/渲染失败 → "日志 WARN + 使用能力内硬编码兜底 Prompt"
- 实验分流异常 → "日志 WARN + 降级到 default 分组"

但 §4.1 `doExecuteInternal()` 伪代码中 `promptTemplateManager.render()` 和 `experimentManager.assign()` 调用均未包裹 try-catch。若这两个方法抛出异常（如数据库查询超时、模板解析 NPE），异常将穿透 `supplyAsync` lambda 边界成为 `CompletionException`，被 `AiOrchestrator.handle()` 的 catch 块统一拦截并返回 `AiResult.failure("AI服务暂时不可用")`。该行为与 §5.1 承诺的"继续调用 LLM"和"降级到 default 分组"矛盾，导致运行时在异常场景下的行为与设计意图不一致。

**所在位置**：§4.1 `doExecuteInternal()` 伪代码（`experimentManager.assign()` 调用行和 `promptTemplateManager.render()` 调用行）vs §5.1 错误分类表

**严重程度**：严重

**改进建议**：在 `doExecuteInternal()` 中为 `experimentManager.assign()` 和 `promptTemplateManager.render()` 分别添加 try-catch：
- 实验分流异常：catch → 日志 WARN，构造 `ExperimentAssignment` 默认实例（`groupId="default"`，`targetPromptVersion=null`），继续管线
- 模板渲染异常：catch → 日志 WARN，使用能力标识维度的硬编码兜底 Prompt 文本（或留空由 `LlmClient` 层面处理），继续管线
- 或在 §5.1 中如实修正承诺：将模板/实验异常归类为"不可恢复的基础设施异常"，改为走降级路径

---

### 问题 2：【严重】CapabilityExecutor 执行管线缺少整体兜底超时机制

**问题描述**：§4.1 `doExecuteInternal()` 伪代码包含实验分流、模板渲染、模型路由、健康检查、LLM 调用、结果解析等多个步骤。其中仅 `LlmClient.invoke()` 有超时控制（`connectionTimeout`/`readTimeout`）。但 `experimentManager.assign()`（JPA 查询）、`promptTemplateManager.render()`（数据库查询+模板渲染）、`extractVariables()`（Jackson 转换）等步骤均无超时保护。若这些步骤中的数据库查询挂起或 Jackson 处理大对象阻塞，将无限占用 `LlmCallExecutor` 线程池线程，且该线程池拒绝策略为 `CallerRunsPolicy`，进一步可能导致 Tomcat 容器线程也被阻塞。

**所在位置**：§4.1 `doExecuteInternal()` 伪代码；§3.2 LlmCallExecutor 线程池配置

**严重程度**：严重

**改进建议**：为 `CapabilityExecutor.execute()` 整体管线引入可配置的端到端超时机制：
- 通过 `CompletableFuture.orTimeout(timeout, TimeUnit)` 或 `CompletableFuture.completeOnTimeout()` 在 supplyAsync 返回的 Future 上附加超时
- 超时后自动进入降级路径，记录 `DegradationReason.TIMEOUT`
- 各能力可独立配置端到端超时阈值（默认 60 秒）
- 同步更新 §3.1 CapabilityExecutor 方法签名中的异常契约说明、§9.5 YAML 配置示例

---

### 问题 3：【重要】AiOrchestrator.handle() 异常记录丢失关键就诊上下文

**问题描述**：§4.1 `AiOrchestrator.handle()` 的 catch 块捕获意外异常时调用 `AiCallRecord.failure(capabilityId, LocalDateTime.now(), 0L, ...)`，对 `departmentId`、`visitId`、`patientId`、`sessionId`、`callerRole`、`callerId`、`inputSummary` 等参数均传入 null。这意味着当管线中发生不可预知异常（NPE、Jackson 序列化失败等）时，日志记录丢失了当前请求的就诊上下文和输入摘要。运维人员从数据库中无法追踪到受影响的具体患者/就诊/科室，降级率统计在该异常路径下也缺少按科室维度的聚合能力。

**所在位置**：§4.1 `AiOrchestrator.handle()` 伪代码 catch 块（第 1359-1362 行区域）

**严重程度**：重要

**改进建议**：在 catch 块中利用 `request` 对象提取可用的上下文字段——使用 `instanceof AiRequestBase` 检查后提取 `departmentId`、`visitId`、`patientId`、`sessionId`；对 `inputSummary` 使用 `StringUtils.truncate(request.toString(), 500)` 截取。若 `request` 非 `AiRequestBase` 子类（Phase 4 薄适配器场景），至少尝试提取 `toString()` 作为 `inputSummary`。同步更新所有对应的 `failure()` 调用参数。

---

### 问题 4：【重要】薄适配器型的 CapabilityExecutor 对 Phase 4 服务的端到端耗时不可控

**问题描述**：§3.1 薄适配器伪代码中 `phase4ServiceDelegate.execute(request)` 委托调用 Phase 4 业务服务，底座仅在外层设 try-catch 区分 BusinessException 与基础设施异常。但底座无法控制 Phase 4 服务内部的超时设置和重试策略。若 Phase 4 服务内部有 3 次重试（每次 30 秒），薄适配器路径的端到端耗时可能超过 90 秒，而底座的 `LlmCallExecutor` 线程池无此维度超时保护（见问题 2）。`startTime` 在 `execute()` 入口处记录，但底座无从知晓 Phase 4 调用是否已超过合理阈值。

**所在位置**：§3.1 薄适配器伪代码 `doExecuteInternal()`（`phase4ServiceDelegate.execute(request)` 调用行）；§9.5 YAML 配置

**严重程度**：重要

**改进建议**：在薄适配器的 `doExecuteInternal()` 中为 `phase4ServiceDelegate.execute(request)` 引入独立超时控制：
- 使用 `CompletableFuture.supplyAsync(() -> phase4ServiceDelegate.execute(request), timeoutExecutor).orTimeout(thinAdapterTimeout, TimeUnit)` 包装委托调用
- 超时后按基础设施异常走降级路径
- `thinAdapterTimeout` 在 §9.5 YAML 中作为底座配置项（默认 30 秒）
- 或在 §3.2 或 §7 中明确约定：薄适配器场景下，底座依赖 Phase 4 服务的超时机制（需 Phase 4 模块配合实现），底座不做二次超时——此方案需在文档中显式声明约束而非留空

---

### 问题 5：【中等】降级预检在线程池排队之后执行，高并发下降级响应延迟

**问题描述**：§4.1 `AbstractCapabilityExecutor.execute()` 伪代码中，降级预检（`buildDegradationContext()` + 策略链遍历）放置在 `CompletableFuture.supplyAsync()` 的 lambda 内部执行。当 `LlmCallExecutor` 线程池队列积压时，本应被熔断器拒绝的请求仍需排队等待线程池线程执行降级预检后才能被降级。这意味着熔断器 OPEN 状态下的请求也会经历队列等待时间（可能数百毫秒），而本应在容器线程即返回降级结果。

**对比**：当前时序为「容器线程 → 入队 → 线程池线程执行降级预检 → 降级/正常执行」。更优的时序应为「容器线程执行降级预检 → 若降级则立即返回 → 仅正常请求入队」。

**所在位置**：§4.1 `AbstractCapabilityExecutor.execute()` 模板方法伪代码（第 1384-1397 行）

**严重程度**：中等

**改进建议**：将降级预检步骤移至 `supplyAsync()` 之前、容器线程执行。具体修改：
- 在 `supplyAsync()` 调用前执行 `context = slidingWindowMetricsStore.buildDegradationContext(...)` 和策略链遍历
- 若任一策略判定降级，直接返回 `CompletableFuture.completedFuture(doDegrade(...))`，不入线程池排队
- 仅当所有策略均返回 false 时，`supplyAsync` 提交 `doExecuteInternal()`
- 注意：需确保 `buildDegradationContext()` 本身无阻塞 I/O（当前设计为内存操作），移动后不引入容器线程阻塞风险
- 同步更新 §3.1 的模板方法伪代码和 §3.8 的降级判定流程说明

---

## 整体评价

经过 9 轮内部审议和 12 版修订，产出在类图、核心职责、协作关系、关键接口、状态模型等 OOD 核心要素上已达到较高成熟度，技术可行性和实现细节已得到充分覆盖。上述 5 个问题属于内部审议未充分覆盖的维度（异常容错一致性、整体超时治理、运维可观测性、跨模块耗时控制、降级判定的响应及时性），建议修复后进入下一阶段。
