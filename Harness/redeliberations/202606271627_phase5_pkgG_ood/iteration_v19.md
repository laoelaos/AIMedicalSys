# 再审议判定报告（v19）

## 判定结果

RETRY

## 判定理由

诊断报告识别出6个问题：问题1-3为「重要」（映射为严重等级），涉及接口实现与设计意图矛盾、配置注入机制缺失、条件化Bean注册缺口；问题4-5为「中等」（一般等级），涉及多余依赖和配置一致性；问题6为「一般」等级。质询报告LOCATED，确认所有问题有效。实际轮次（1）小于最大轮次（12），因问题已被定位确认而提前终止。诊断报告包含严重及一般等级问题，故判定RETRY。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：`HttpApiLlmChatService` / `SpringAiLlmChatService` 同时实现两个接口，与 §3.2 独立 `LlmChatStreamService` 的 reactor-core 隔离设计意图矛盾
- **所在位置**：§2.3 类图第 583-586 行；§3.2
- **严重程度**：严重
- **改进建议**：分离两个接口的实现，或修改 §3.2 设计说明承认 reactor-core 为 ai-impl 的非可选编译期依赖

- **问题描述**：`thinAdapterTimeout` 与 `capabilityTimeoutConfig` 的注入/初始化机制未定义
- **所在位置**：§2.3 类图第 239-240 行；§3.1 第 773-784 行
- **严重程度**：严重
- **改进建议**：在 `AbstractCapabilityExecutor` 构造函数中补充 `@Value` 注入或显式 `@Bean` + `@Inject` 方式

- **问题描述**：薄适配器型 CapabilityExecutor 缺少条件化 Bean 注册保护，Phase 4 服务不可用时容器启动失败
- **所在位置**：§3.1 第 769 行；§2.2 第 188 行
- **严重程度**：严重
- **改进建议**：添加 `@ConditionalOnProperty` 或 `@ConditionalOnBean` / `@ConditionalOnClass` 守卫

- **问题描述**：薄适配器构造函数包含不必要的基础设施依赖
- **所在位置**：§3.1 第 773-784 行
- **严重程度**：一般
- **改进建议**：引入独立构造器签名，仅注入实际使用的依赖；或标记为 `required = false`

- **问题描述**：`@Async` 指标采集线程池拒绝策略配置与 `@Bean` 定义不一致（DiscardPolicy 静默丢弃 vs 设计要求"+ 日志 WARN"）
- **所在位置**：§3.5 第 1377 行；§3.9 第 1706 行
- **严重程度**：一般
- **改进建议**：自定义继承 `DiscardPolicy` 并重写 `rejectedExecution()` 输出 WARN 日志，或在 `@Bean` 注释中说明

- **问题描述**：`AiOrchestrator.handle()` catch 块中 `instanceof` 判断未处理 request 被篡改或代理的场景
- **所在位置**：§4.1 第 1807 行
- **严重程度**：一般
- **改进建议**：增加双重提取策略，优先 `instanceof` 提取，降级到 `extractFromRequestContext()` 兜底
