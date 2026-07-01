# 再审议判定报告（v10）

## 判定结果

RETRY

## 判定理由

组件B诊断报告识别出 5 个问题，其中 2 个严重（问题1：§5.1 承诺与 §4.1 伪代码矛盾；问题2：整体管线缺少端到端超时机制），2 个重要（问题3：异常记录丢失就诊上下文；问题4：薄适配器耗时不可控），1 个中等（问题5：降级预检响应延迟）。质询报告结论为 LOCATED，四个维度全部通过，确认审查质量。由于存在严重和重要（一般）等级问题，符合 RETRY 条件。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：§5.1 错误分类表的异常容错承诺在 §4.1 伪代码中未兑现，`experimentManager.assign()` 和 `promptTemplateManager.render()` 均未包裹 try-catch
- **所在位置**：§4.1 `doExecuteInternal()` 伪代码 vs §5.1 错误分类表
- **严重程度**：严重
- **改进建议**：在 `doExecuteInternal()` 中为这两个调用添加 try-catch，按 §5.1 承诺的降级行为处理；或在 §5.1 中如实修正承诺

- **问题描述**：CapabilityExecutor 执行管线缺少整体兜底超时机制，仅 `LlmClient.invoke()` 有超时控制
- **所在位置**：§4.1 `doExecuteInternal()` 伪代码；§3.2 LlmCallExecutor 线程池配置
- **严重程度**：严重
- **改进建议**：引入可配置的端到端超时机制，通过 `CompletableFuture.orTimeout()` 附加超时，超时后进入降级路径

- **问题描述**：AiOrchestrator.handle() catch 块调用 `AiCallRecord.failure()` 时丢失 `departmentId`、`visitId`、`patientId`、`sessionId` 等关键就诊上下文
- **所在位置**：§4.1 catch 块（第 1359-1362 行区域）
- **严重程度**：重要
- **改进建议**：从 `request` 对象中提取可用上下文字段传入 `failure()` 调用

- **问题描述**：薄适配器型 CapabilityExecutor 对 Phase 4 服务的端到端耗时不可控，底座无法控制其内部超时设置和重试策略
- **所在位置**：§3.1 薄适配器伪代码 `doExecuteInternal()`；§9.5 YAML 配置
- **严重程度**：重要
- **改进建议**：为 `phase4ServiceDelegate.execute(request)` 引入独立超时控制，或明确约定依赖 Phase 4 模块自身的超时机制

- **问题描述**：降级预检在 `supplyAsync()` lambda 内部执行，线程池排队后才做降级判定，导致熔断器 OPEN 状态下请求仍需等待入队
- **所在位置**：§4.1 `AbstractCapabilityExecutor.execute()` 模板方法伪代码（第 1384-1397 行）
- **严重程度**：中等
- **改进建议**：将降级预检移至 `supplyAsync()` 之前、容器线程执行，降级请求直接返回 `CompletableFuture.completedFuture(doDegrade(...))`，不入线程池排队
