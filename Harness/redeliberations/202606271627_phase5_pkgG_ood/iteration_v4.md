# 再审议判定报告（v4）

## 判定结果

RETRY

## 判定理由

组件B诊断报告共识别12个问题（P1-P4为严重、P5-P9为重要、P10-P12为中等），质询报告结果为LOCATED确认审查质量有效。组件B实际轮次1 < 最大轮次12，说明因LOCATED而提前终止。诊断报告中存在严重及一般等级问题，符合RETRY条件。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：降级路径中 recordSuccess 导致熔断器统计失准
- **所在位置**：§4.1 降级路径伪代码第979行 + §3.5 SlidingWindowMetricsStore 方法签名 + §2.3 类图第368-373行
- **严重程度**：严重
- **改进建议**：为 SlidingWindowMetricsStore 增加重载方法或新增降级标记专用方法，调整失败率计算公式

- **问题描述**：departmentId 在标准管线中无可用来源
- **所在位置**：§3.5 AiRequestBase 字段定义第737-743行 + §4.1 管线伪代码第951行 + §4.4 PromptTemplateManager.render() 签名
- **严重程度**：严重
- **改进建议**：在 AiRequestBase 中增加 String departmentId 字段，或明确从 RequestContext/SecurityContext 提取

- **问题描述**：异步边界不明确 — LlmClient.invoke() 同步阻塞与 CompletableFuture 返回值矛盾
- **所在位置**：§2.3 类图 LlmClient.invoke() 返回值类型第248行 + §3.1 CapabilityExecutor.execute() 方法签名 + §4.1 伪代码第966行
- **严重程度**：严重
- **改进建议**：明确异步策略：改 LlmClient.invoke() 为 CompletableFuture，或 execute() 内部包裹 supplyAsync()，或改为同步语义

- **问题描述**：降级策略 Bean 注入机制从 YAML 配置到 @Qualifier 的转换路径未定义
- **所在位置**：§3.1 第569行 + §3.8 第914行 + §9.2 配置示例
- **严重程度**：严重
- **改进建议**：在 §3.1 或 §7 中补充完整的策略 Bean 装配机制伪代码说明

- **问题描述**：薄适配器型 CapabilityExecutor 的执行行为未定义
- **所在位置**：§3.1 第505-508行 + §4.1 伪代码
- **严重程度**：重要
- **改进建议**：在 §4.1 中新增薄适配器 CapabilityExecutor 管线小节，提供独立简化伪代码

- **问题描述**：AiOrchestrator 方法到能力标识的映射约定未定义
- **所在位置**：§3.1 AiOrchestrator 职责 + §4.1 第931-936行
- **严重程度**：重要
- **改进建议**：显式列出 AiService 全部13个方法到 capabilityId 的映射表，建议使用 CapabilityId 常量类或 enum

- **问题描述**：FallbackAiService.applyStrategies() 残留代码的迁移路径未在主文档中定义
- **所在位置**：§3.8 第864行 + §5 错误处理
- **严重程度**：重要
- **改进建议**：在 §9 迁移路径表中明确 FallbackAiService 的三项迁移操作

- **问题描述**：ModelEndpointHealthManager 与 CircuitBreakerDegradationStrategy 的交互优先级未定义
- **所在位置**：§3.2 第607行 + §3.8 第882-895行 + §4.1 伪代码第943-965行
- **严重程度**：重要
- **改进建议**：明确定义两个机制的交互优先级，建议降级预检（含熔断器）优先

- **问题描述**：CapabilityExecutor 线程安全性未在 §6.1 中覆盖
- **所在位置**：§6.1 线程模型第1073-1084行
- **严重程度**：重要
- **改进建议**：补充 CapabilityExecutor 的线程安全契约，要求无状态线程安全

- **问题描述**：userId 在 ExperimentManager.assign() 管线中的来源未定义
- **所在位置**：§4.1 伪代码第952行 + §3.4 ExperimentManager.assign() 方法签名第285行
- **严重程度**：中等
- **改进建议**：明确 userId 的来源定义，可选方案包括从 SecurityContext 提取或在 AiRequestBase 中增加字段

- **问题描述**：ModelRouter 运行时刷新的线程安全性不完整
- **所在位置**：§6.1 第1081行 + §4.2 第999行
- **严重程度**：中等
- **改进建议**：使用 AtomicReference<Map<...>> 替代直接使用 ConcurrentHashMap

- **问题描述**：AiOrchestrator.handle() 缺少异常捕获
- **所在位置**：§4.1 伪代码第931-936行
- **严重程度**：中等
- **改进建议**：在伪代码中增加 try-catch 块，异常时记录错误指标并返回降级响应
