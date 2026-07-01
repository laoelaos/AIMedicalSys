# 质量审查报告 — Phase 5 包 G OOD 设计（v3）

## 审查概要

- 审查对象：Phase 5 包 G — AI 进阶底座 架构级 OOD 设计方案（a_v3_design_v2.md）
- 审查轮次：第 3 次
- 审查视角：需求响应充分度、整体深度与完整性、落地可行性（编码指导力）
- 说明：该产出已通过组件 A 的内部审议（设计-验证），本报告侧重内部审议未充分覆盖的维度

---

## 问题列表

### 问题 1：[严重] 能力覆盖不足 — 仅 7/13 项 AI 能力有对应的 CapabilityExecutor 实现规划

**问题描述**：
产出声称 Phase 5 底座将迁移和统合"全部 AI 能力"，但 §2.1 目录结构和 §9.1 迁移路径仅列了 7 个 CapabilityExecutor 实现（Triage、PrescriptionCheck、MedicalRecordGen、PrescriptionAssist、KbQuery、Schedule、DiscussionConclusion）。AiService 接口实际有 13 个方法，缺失的 6 个（`diagnosis`、`analysisReportForInspection`、`analysisReportForLabTest`、`imageAnalysis`、`recommendExamination`、`recommendExecutionOrder`）没有任何实现计划、时间表或临时策略。

**所在位置**：§2.1 目录结构 `impl/` 子包、§9.1 迁移路径表

**严重程度**：严重

**改进建议**：
- 明确这 6 项能力是否纳入 Phase 5 底座范围
- 若纳入：补充对应的 CapabilityExecutor 实现
- 若不纳入：解释这些能力在 Phase 5 期间由谁接管（仍走旧的独立实现？通过 FallbackAiService 委托给旧实现？），并提供明确边界说明

---

### 问题 2：[严重] 管线所有权矛盾 — §3.1 与 §4.1 对「谁拥有编排管线」的描述冲突

**问题描述**：
§3.1（第 462-471 行）将 5 步管线（模板渲染→实验分流→模型路由→模型调用→结果解析）放在 AiOrchestrator 的方法执行流程中描述，CapabilityExecutor 仅被描述为"封装该能力特有的模板变量映射、输出解析逻辑与本地规则降级逻辑"。

但 §4.1（第 870-898 行）将完整的管线（降级预检→模板渲染→实验分流→模型路由→模型调用→解析→指标采集→滑动窗口记录，以及降级路径的完整处理）全部放在 `CapabilityExecutor.execute()` 内部实现，AiOrchestrator.handle() 仅做"查找 executor → 调用 execute() → 返回结果"的简单路由。

这是两种根本不同的架构形态：
- A) AiOrchestrator 拥有公共管线，CapabilityExecutor 仅做能力特化的回调/钩子
- B) CapabilityExecutor 拥有完整管线，AiOrchestrator 仅做路由

**所在位置**：§3.1（第 462-475 行）vs §4.1（第 865-898 行）

**严重程度**：严重

**改进建议**：
- 统一两种描述，选择一种架构
- 若选 A：重构 §4.1 伪代码，将公共管线移到 AiOrchestrator，CapabilityExecutor.execute() 只做能力特化的模板变量映射 + 输出解析 + 可选降级
- 若选 B：重构 §3.1 文本，明确 CapabilityExecutor 的独立管线职责，并解释为何需要 8+ 依赖注入到每个实现

---

### 问题 3：[严重] DegradationStrategies 跨组件访问路径未定义

**问题描述**：
§3.1（第 474 行）明确 `List<DegradationStrategy> degradationStrategies` 为 AiOrchestrator 的内部持有字段。但 §4.1 伪代码（第 873-876 行）将策略链遍历放在 `CapabilityExecutor.execute()` 内部执行。由于 `execute()` 方法签名（`execute(T request, String capabilityId)`）不接受策略列表参数，且 CapabilityExecutor 不持有 AiOrchestrator 的引用，策略列表对 CapabilityExecutor 不可见。

**所在位置**：§3.1（第 474 行）vs §4.1（第 873 行）

**严重程度**：严重

**改进建议**：
- 降级预检从 CapabilityExecutor 移至 AiOrchestrator，与管线架构选择一致
- 或将 `List<DegradationStrategy>` 作为 `execute()` 方法参数传入
- 或让 CapabilityExecutor 通过 Spring 注入策略列表（需注意这是全局策略列表，不是按能力的白名单）

---

### 问题 4：[重要] §4.1 降级路径中 `elapsedMs` 变量未定义

**问题描述**：
§4.1 降级路径本地规则成功分支（第 893 行）：
```
slidingWindowMetricsStore.recordSuccess(capabilityId, elapsedMs)
```
`elapsedMs` 在此作用域中未定义。当 LLM 调用被跳过时，降级路径没有计时逻辑。是记录为 0？还是记录从请求开始到本地规则执行完的耗时？

同理，无本地规则的分支（第 897 行）调用 `recordFailure(capabilityId)` 没有耗时参数——但 `recordFailure()` 方法签名也无耗时参数，前后一致但 §3.5 未定义该方法签名。

**所在位置**：§4.1（第 893 行）

**严重程度**：重要

**改进建议**：
- 在 degrade 路径入口处添加计时逻辑（`long startTime = System.nanoTime()`）
- 用最后的 elapsedMs 传递给 `recordSuccess()`
- 或明确降级路径不记录耗时，直接传 0

---

### 问题 5：[重要] Null ModelRoute 导致 NPE — 降级路径未触发

**问题描述**：
§4.2（第 913 行）声明：无可用路由→返回 null→触发降级。但 §4.1 伪代码（第 882 行）：
```
f. llmClient.invoke(LlmRequest(renderedPrompt, modelRoute)) → llmResponse
```
若 `modelRoute` 为 null，`LlmRequest` 构造器中将抛 NPE，而管线中没有捕获 NPE 的 try-catch。降级不会触发，异常会传播到上层。

**所在位置**：§4.1（第 882 行）+ §4.2（第 913 行）

**严重程度**：重要

**改进建议**：
- 在步骤 e→f 之间添加 null 检查：若 `modelRoute == null` 直接跳转到 degrade 路径
- 或将 `Optional<ModelRoute>` 作为返回值类型

---

### 问题 6：[重要] CapabilityExecutor 方法到能力标识的映射机制未定义

**问题描述**：
§3.1（第 475 行）说"通过能力标识查找对应的 CapabilityExecutor"，但未定义映射机制。AiOrchestrator 有 13 个方法（triage、diagnosis、prescriptionCheck 等），每个方法需要调用对应能力的 CapabilityExecutor。映射通过什么实现？
- `Map<String, CapabilityExecutor>` 手动注册？
- Spring 自动注入 `List<CapabilityExecutor>` + `getCapabilityId()` 匹配？
- 注解扫描？
- 枚举？

设计没有明确，不同开发人员可能采用不同方式，导致实现不一致。

**所在位置**：§3.1（第 474-475 行）

**严重程度**：重要

**改进建议**：
- 在 §3.1 或 §7 中补充明确的映射机制选择
- 给出构造时机（构造器初始化 / `@PostConstruct` / 自动装配）

---

### 问题 7：[中等] AiRequestBase 基类引入未评估对现有 DTO 的影响

**问题描述**：
§3.5（第 679-690 行）定义 `AiRequestBase` 抽象类作为所有 AI 能力请求 DTO 的基类。当前代码库中 TriageRequest、PrescriptionCheckRequest 等 13 个 DTO 均未继承此基类。引入后需要：
1. 创建 AiRequestBase 类
2. 修改 13 个现有 DTO 继承它
3. 修改各 DTO 的 visitId/patientId/sessionId 字段（当前可能各有不同的字段名或不存在）
4. 确保序列化兼容（Jackson 多态等）

这些工作的范围、风险、兼容性均未评估。

**所在位置**：§3.5（第 679-690 行）

**严重程度**：中等

**改进建议**：
- 评估现有 DTO 中是否已有 visitId/patientId/sessionId 字段（及命名是否一致）
- 提供向后兼容策略（保留旧字段或通过 `@JsonIgnore` 过渡）
- 或将基类改为接口（降低入侵性）

---

### 问题 8：[中等] FallbackAiService.applyStrategies() 残留空值 DegradationContext 代码路径

**问题描述**：
§3.8（第 796 行）说"FallbackAiService.applyStrategies() 不再保留或简化为空方法"，但现有代码（FallbackAiService.java:183-194）中 applyStrategies() 仍在构造空值 DegradationContext 并遍历策略列表。设计稿未明确：
1. 该方法的具体目标形态（空方法？保留 but 跳过 LLM 失败场景？）
2. 构造函数中的 `List<DegradationStrategy> strategies` 注入是否移除
3. `thenApply(this::applyStrategies)` 调用链是否保留

设计阶段的表述（"简化为空方法"）不足以指导编码。

**所在位置**：§3.8（第 796 行）、现有代码 FallbackAiService.java:183-194

**严重程度**：中等

**改进建议**：
- 明确 applyStrategies() 的最终行为：
  - 方案 1：移除 thenApply 链 + 保留空方法占位 + 移除策略列表注入
  - 方案 2：保留 but 仅对 `!isSuccess && !isDegraded` 做降级（LLM 失败时兜底），移除策略遍历

---

### 问题 9：[中等] Prompt 模板变量提取逻辑未定义

**问题描述**：
§4.1（第 879 行）调用 `promptTemplateManager.render(capabilityId, request.getDepartmentId(), variables)`。其中 `variables`（`Map<String, Object>`）从业务请求 DTO（`T`）中提取，但每个能力的 DTO 字段不同（TriageRequest 有症状描述字段，PrescriptionCheckRequest 有处方字段等）。变量提取逻辑是每个 CapabilityExecutor 的专属职责，但设计未定义提取模式（硬编码字段映射？注解驱动？JsonPath？）。

**所在位置**：§4.1（第 879 行）、§3.3 PromptTemplateManager.render 方法

**严重程度**：中等

**改进建议**：
- 定义变量提取的约定模式：
  - Jackson `ObjectMapper.convertValue()` 将 DTO 转 Map 做变量源
  - 或每个 CapabilityExecutor 实现 `Map<String, Object> extractVariables(T request)` 方法
  - 或使用注解标记模板变量字段

---

### 问题 10：[中等] `ai.platform.enabled` → `ai.mock.enabled` 配置转发机制未定义

**问题描述**：
§3.1（第 485 行）描述："AiPlatformConfig：作为统一配置入口，从 ai.platform.enabled 内部转发到 ai.mock.enabled 配置项"。但 Spring Boot 原生不支持属性键之间的自动转发。实现方式可能是：
- 通过 `EnvironmentPostProcessor` 在 Environment 中设置别名
- 通过 `@ConditionalOnExpression("#{environment.getProperty('ai.platform.enabled') != 'true'}")` 替代转发
- 通过自定义 `PropertySource` 实现

设计未指定实现策略，不同实现方式的激活时机和优先级不同，可能导致 MockAiService 条件注解的预期行为不一致。

**所在位置**：§3.1（第 485 行）、§9.2 配置

**严重程度**：中等

**改进建议**：
- 指定转发机制的具体方案并评估对 MockAiService 条件注解的影响
- 或直接统一为单一开关（移除 ai.platform.enabled，直接使用 ai.mock.enabled 的正逻辑/反逻辑）

---

## 整体质量评价

该产出经过两轮内部审议修正后，在设计的一致性和细节丰富度上有显著提升，类图、状态模型、关键接口定义等 OOD 核心要素已基本覆盖。但仍存在两个结构性矛盾（管线所有权冲突、策略访问路径未定义）和一个范围覆盖缺口（6/13 能力无实现规划），这些是当前版本最突出的编码实施障碍。推荐的修复顺序：先解范围缺口（决定 6 个能力归属），再解管线所有权矛盾，最后逐一消解映射和实现细节上的歧义。
