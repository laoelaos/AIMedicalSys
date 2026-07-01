根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 问题 1：[严重] 能力覆盖不足 — 仅 7/13 项 AI 能力有对应的 CapabilityExecutor 实现规划
§2.1 目录结构 `impl/` 子包、§9.1 迁移路径表仅列了 7 个 CapabilityExecutor 实现，而 AiService 接口实际有 13 个方法，缺失 6 个（diagnosis、analysisReportForInspection、analysisReportForLabTest、imageAnalysis、recommendExamination、recommendExecutionOrder）没有任何实现计划、时间表或临时策略。

**改进建议**：明确 6 项能力是否纳入 Phase 5 底座范围；若纳入则补充对应 CapabilityExecutor 实现；若不纳入则说明由谁接管并提供边界说明。

### 问题 2：[严重] 管线所有权矛盾 — §3.1 与 §4.1 对「谁拥有编排管线」的描述冲突
§3.1（第 462-475 行）将 5 步管线放在 AiOrchestrator 的方法执行流程中，CapabilityExecutor 仅做能力特化。但 §4.1（第 870-898 行）将完整管线全部放在 CapabilityExecutor.execute() 内部实现，AiOrchestrator.handle() 仅做路由。这是两种根本不同的架构形态。

**改进建议**：统一为一种架构。若选 A（AiOrchestrator 拥有公共管线）则重构 §4.1；若选 B（CapabilityExecutor 拥有完整管线）则重构 §3.1。

### 问题 3：[严重] DegradationStrategies 跨组件访问路径未定义
§3.1（第 474 行）将策略列表列为 AiOrchestrator 的内部持有字段，但 §4.1 伪代码将其遍历放在 CapabilityExecutor.execute() 内部执行。由于 execute() 方法签名不接受策略列表参数且 CapabilityExecutor 不持有 AiOrchestrator 引用，策略列表对 CapabilityExecutor 不可见。

**改进建议**：降级预检移至 AiOrchestrator，或将策略列表作为 execute() 参数传入，或通过 Spring 注入。

### 问题 4：[重要] §4.1 降级路径中 elapsedMs 变量未定义
§4.1 第 893 行降级路径中 `slidingWindowMetricsStore.recordSuccess(capabilityId, elapsedMs)` 的 `elapsedMs` 在降级作用域中未定义。

**改进建议**：在 degrade 路径入口添加计时逻辑后用 elapsedMs 调用 recordSuccess()，或明确传 0。

### 问题 5：[重要] Null ModelRoute 导致 NPE — 降级路径未触发
§4.1（第 882 行）`llmClient.invoke(LlmRequest(renderedPrompt, modelRoute))` 中若 modelRoute 为 null，LlmRequest 构造器将抛 NPE 而非触发降级路径。

**改进建议**：在步骤 e→f 间添加 null 检查直接跳转到 degrade 路径，或将返回值类型改为 `Optional<ModelRoute>`。

### 问题 6：[重要] CapabilityExecutor 方法到能力标识的映射机制未定义
§3.1（第 474-475 行）说"通过能力标识查找对应的 CapabilityExecutor"，但未定义映射机制（Map 手动注册？Spring 自动注入 + getCapabilityId()？注解扫描？枚举？）。

**改进建议**：在 §3.1 或 §7 中补充明确的映射机制选择及构造时机。

### 问题 7：[中等] AiRequestBase 基类引入未评估对现有 DTO 的影响
§3.5（第 679-690 行）定义 AiRequestBase 基类，但当前代码库中 13 个 DTO 均未继承此基类，引入后需创建基类、修改全部 DTO、处理字段差异、确保序列化兼容。

**改进建议**：评估现有 DTO 字段一致性，提供向后兼容策略，或将基类改为接口降低入侵性。

### 问题 8：[中等] FallbackAiService.applyStrategies() 残留空值 DegradationContext 代码路径
§3.8（第 796 行）说 applyStrategies() 不再保留或简化为空方法，但未明确该方法的具体目标形态、策略列表注入是否移除、thenApply 调用链是否保留。

**改进建议**：明确 applyStrategies() 的最终行为：移除 thenApply + 保留空方法 + 移除策略注入，或保留仅对 LLM 失败场景做降级。

### 问题 9：[中等] Prompt 模板变量提取逻辑未定义
§4.1（第 879 行）调用 `promptTemplateManager.render(capabilityId, request.getDepartmentId(), variables)`，但 variables 从业务请求 DTO 中提取的逻辑未定义（硬编码映射？注解驱动？JsonPath？）。

**改进建议**：定义变量提取的约定模式：ObjectMapper.convertValue() 转换，或每个 CapabilityExecutor 实现 `extractVariables()` 方法，或注解标记。

### 问题 10：[中等] ai.platform.enabled → ai.mock.enabled 配置转发机制未定义
§3.1（第 485 行）说 AiPlatformConfig 从 ai.platform.enabled 内部转发到 ai.mock.enabled，但 Spring Boot 原生不支持属性键间自动转发。

**改进建议**：指定转发机制的具体方案（EnvironmentPostProcessor / @ConditionalOnExpression / 自定义 PropertySource），或统一为单一开关。

## 历史迭代回顾

### 已解决的问题（出现在历史反馈但当前反馈中不再提及的问题）
- **第 1 轮全部 8 个问题**已解决：缺失 UML 类图 → §2.3 已补充；状态模型覆盖不足 → 各章节已补充；CapabilityExecutor 接口方法签名 → §3.1 已定义；Bean 装配二义性 → 已明确 @Primary + ObjectProvider；DegradationStrategy 注入问题 → 已分离至编排层；"无状态"断言 → 已修订；DegradationContext 兼容性 → 已评估；AiCallLog Entity → 已定义。
- **第 2 轮全部 9 个问题**已解决：降级路径死代码 → 已重构；MockAiService @ConditionalOnProperty → 已修正；getOrder() 破坏现有实现 → 改为 default 方法；类图 CompletableFuture → 已补充；recordFailure 错误 → 已修正；AiRequestBase → 已定义；类图方法名 → 已修正；LlmClient 状态矛盾 → 已分离 ModelEndpointHealthManager；AiCallLogEntity 索引 → 已补充。

### 持续存在的问题（在多轮反馈中反复出现，需重点解决）
- **问题 1-6** 在第 3 轮反馈中已被识别但未解决，本轮继续要求修复：
  - 能力覆盖缺口（6/13 能力无规划）
  - 管线所有权矛盾（§3.1 vs §4.1 冲突）
  - DegradationStrategies 访问路径未定义
  - 降级路径 elapsedMs 未定义
  - Null ModelRoute NPE 风险
  - 映射机制未定义

### 新发现的问题（本轮新识别的问题）
- **问题 7-10** 为本轮新识别的中等严重度问题：
  - AiRequestBase 引入影响未评估
  - FallbackAiService.applyStrategies() 代码路径不明确
  - Prompt 模板变量提取逻辑未定义
  - 配置转发机制未指定

## 上一轮产出路径
C:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606271627_phase5_pkgG_ood/a_v3_design_v2.md

## 用户需求
C:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606271627_phase5_pkgG_ood/requirement.md
