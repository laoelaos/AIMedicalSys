# 质量审查报告 — Phase 5 包 G OOD 设计文档（v7）

审查轮次：第 6 次  
审查视角：需求响应充分度、事实/逻辑正确性、深度与完整性  
审查原则：侧重内部审议未充分覆盖的维度（需求响应充分度、整体深度和完整性），避免重复验证内部审议已确认的技术可行性等问题

---

## 一、需求响应充分度

### 1.1 [重要] A/B 实验的 Prompt 版本分流失效 — 模板版本覆盖未接入执行管线

- **问题描述**：文档定义了 `ExperimentAssignment.targetPromptVersion` 字段（§3.4），并明确实验分流可指定目标 Prompt 版本。然而 `§4.1` 标准管线伪代码中，`promptTemplateManager.render()` 的调用签名仅为 `render(capabilityId, departmentId, variables)`，未传入 `targetPromptVersion` 参数。`ExperimentAssignment` 携带的 Prompt 版本信息在管线中无消费路径，导致 A/B 实验的 Prompt 版本分流能力成为"死功能"——定义了却无法生效。
- **所在位置**：§4.1 标准管线伪代码第 1076 行 `promptTemplateManager.render(capabilityId, request.getDepartmentId(), variables)`
- **严重程度**：严重
- **改进建议**：两者选一——
  - 方案 A：将 `targetPromptVersion` 传入 `PromptTemplateManager.render()`，在 `render()` 方法签名中增加 `Integer promptVersion` 参数，并在 `DatabasePromptTemplateManager` 实现中按能力+科室+版本号检索模板。
  - 方案 B：若 Phase 5 对实验的 Prompt 版本分流无实际需求，应将 `targetPromptVersion` 从 `ExperimentAssignment` 中移除或标记为预留字段，避免误导下游实现者。

### 1.2 [中等] Phase 4 薄适配器管线缺少 departmentId 的获取定义

- **问题描述**：`§3.1` 薄适配器伪代码中通过 `request.getDepartmentId()` 获取科室标识用于指标记录（`AiCallRecord` 字段填充），但薄适配器委托的 Phase 4 业务服务 DTO（如 `DiagnosisRequest`）尚未继承 `AiRequestBase`，这些 DTO 当前无 `getDepartmentId()` 方法。文档在 `§3.5 AiRequestBase 现有 DTO 影响评估` 中明确"6 项 Phase 4 能力的 DTO 暂维持现状，通过 `AiCallRecord` 字段填充逻辑中的独立提取方法获取公共字段"，但薄适配器伪代码并未体现这一"独立提取方法"——伪代码中仍直接调用 `request.getDepartmentId()`。
- **所在位置**：§3.1 薄适配器伪代码（假设的指标记录路径）vs §3.5 过渡策略
- **严重程度**：中等
- **改进建议**：在薄适配器管线中显式说明 departmentId 的提取方式：提供独立的提取方法（如 `extractDepartmentId(request)`）或通过 `SecurityContext`/`RequestContext` 统一提取，避免薄适配器直接依赖 Phase 4 DTO 上不存在的方法。

### 1.3 [轻微] AiService 第 13 个方法 — `discussionConclusion` 的类型参数对齐

- **问题描述**：`§2.3` 类图 `AiService` 接口列出了 8 个显式方法签名，其余 5 项能力仅以 `+... (其余 5 项能力方法)` 概括。虽然能力标识映射表（§3.1）完整覆盖了 13 项能力，但类图作为直观的顶层契约视图，缺少完整的 13 方法签名列表，实现者需交叉核对两处文档才能获取完整视图。
- **所在位置**：§2.3 类图 AiService 部分
- **严重程度**：轻微
- **改进建议**：在类图中完整列出全部 13 个 `AiService` 方法签名，或明确说明"其余 5 项见 §3.1 能力标识映射表"。当前"其余 5 项能力方法"的注释不直观。

---

## 二、事实错误与逻辑矛盾

### 2.1 [严重] ModelRouter 存储模型描述前后矛盾

- **问题描述**：`§6.1` 中 ModelRouter 的存储模型存在两处矛盾描述——(a) "路由表启动时从配置/DB 加载到 `ConcurrentHashMap`" 表明字段类型为 `ConcurrentHashMap`；(b) "通过 `AtomicReference<Map<String, ModelRoute>>` CAS 一次性替换引用" 表明字段类型为 `AtomicReference<Map<...>>"；(c) "读取路径通过 `AtomicReference.get()` 获取...快照" 再次确认使用 `AtomicReference`。`ConcurrentHashMap` 是具体的 `Map` 实现，`AtomicReference` 是引用包装器，二者不能共存于同一个字段类型。实现者无法从设计文档确定最终字段声明形式。
- **所在位置**：§6.1 线程模型 ModelRouter 段
- **严重程度**：严重
- **改进建议**：统一表述为"`AtomicReference<Map<String, ModelRoute>>`，启动时通过 `AtomicReference.set()` 初始化，运行时按全量替换模式刷新"。若坚持保留 `ConcurrentHashMap` 内部分段并发能力，应明确为 `AtomicReference<ConcurrentHashMap<String, ModelRoute>>` 组合形态。

### 2.2 [重要] EnvironmentPostProcessor 配置转发方向未定义

- **问题描述**：`§3.1` Bean 装配策略中提到 "`AiPlatformConfig` 通过 `EnvironmentPostProcessor` 机制在 Spring 启动早期将 `ai.platform.enabled` 转发到 `ai.mock.enabled` 配置项"，但未明确转发方向——是相同值转发（`ai.platform.enabled=true` → `ai.mock.enabled=true`）还是反向转发（`true` → `false`）。结合 `§9.5` 的 YAML 示例（`ai.platform.enabled=true` 且 `ai.mock.enabled=false`），设计意图应为反向转发，但文档未显式说明。这不仅导致实现歧义，更可能使 `EnvironmentPostProcessor` 的转发覆盖 YAML 中 `ai.mock.enabled=false` 的手动设置，造成启动期 Bean 装配错误。
- **所在位置**：§3.1 Bean 装配策略
- **严重程度**：重要
- **改进建议**：显式写明转发逻辑：`ai.platform.enabled=true` → `ai.mock.enabled=false`（正向转发取反），反之亦然。同时说明 `EnvironmentPostProcessor` 与 YAML 属性源的优先级关系，确保手动设置的 `ai.mock.enabled` 不会被意外覆盖。

### 2.3 [重要] @Qualifier Bean name 推导规则不明确

- **问题描述**：`§3.1` 降级策略注入机制统一使用 `@Qualifier("{capabilityId}Strategies")` 模式，示例为 `@Qualifier("triageStrategies")`、`@Qualifier("rxAuditStrategies")`。然而 `capabilityId` 值定义为全大写字符串（如 `"TRIAGE"`、`"RX_AUDIT"`），按 `"{capabilityId}Strategies"` 模板推导的 Bean name 应为 `"TRIAGEStrategies"`，与示例的 `"triageStrategies"` 不一致。Bean name 的大小写敏感性在 Spring 容器中影响注入匹配，此歧义将在启动期导致 `NoSuchBeanDefinitionException`。
- **所在位置**：§3.1 降级策略注入机制段
- **严重程度**：重要
- **改进建议**：统一约定——要么 `capabilityId` 调整为小写驼峰（如 `"triage"`、`"rxAudit"`）并更新 `§3.1` 的能力标识映射表；要么将注入模式改为全大写一致拼接（如 `"TRIAGE_STRATEGIES"` 或 `"TRIAGEStrategies"`），使示例与推导规则一一对应。

---

## 三、深度与完整性

### 3.1 [重要] CallerRunsPolicy 导致 LlmCallExecutor 线程池饥饿风险

- **问题描述**：`§3.5` AiMetricsCollector 使用 `@Async + CallerRunsPolicy` 拒绝策略（core=2, max=4, queue=1000），`§4.1` 管线伪代码在 `CompletableFuture.supplyAsync(() -> { ... }, llmCallExecutor)` 的 lambda 内部同步调用 `metricsCollector.record()`。当指标写入队列填满（1000 条积压），`CallerRunsPolicy` 将指标写入任务回退到调用者线程——即 `llmCallExecutor` 的线程。此时，一个原本应处理 LLM 调用的线程被阻塞执行数据库 INSERT，可能导致线程池活跃线程数降低、LLM 调用响应延迟扩散，极端情况下引发线程池饥饿。指标采集的背压不应传播到 LLM 调用路径。
- **所在位置**：§3.5 异步队列溢出策略 + §4.1 伪代码第 1093 行、第 1103 行
- **严重程度**：重要
- **改进建议**：方案 A：指标记录改为在 `supplyAsync` lambda 执行完毕后的 `.whenComplete()` / `.thenAccept()` 回调中进行，分离指标写入与 LLM 调用线程。方案 B：指标采集使用独立的线程池（非 `llmCallExecutor`）执行，`CallerRunsPolicy` 回退时由该独立线程池的线程执行写入，避免阻塞 LLM 调用线程。

### 3.2 [重要] DegradationContext 反序列化默认值的缓解措施不充分

- **问题描述**：`§3.8` 识别了 `DegradationContext` 新增字段反序列化后取默认值（0/0L/null）的"静默下界问题"，提出的缓解措施包括"降级策略实现中以字段值 > 0 判据，不依赖绝对值"。此缓解措施不能解决实际场景：若熔断阈值设为 50%（`failureCount / invocationCount ≥ 0.5`），当反序列化后 `failureCount=0`、`invocationCount=0`（均为默认值），计算结果为 0/0 或 0.0，熔断器认为一切正常，但实际上旧数据可能已有大量失败。`>0` 判据仅适用于"是否发生过失败"的二值判断，不适用于百分比阈值的退化程度评估。该缓解措施的有效范围被夸大，核心风险未被消除。
- **所在位置**：§3.8 DegradationContext 二进制兼容性分析段
- **严重程度**：重要
- **改进建议**：明确补充以下措施之一：(a) 在 `SlidingWindowMetricsStore.buildDegradationContext()` 中设置一个"数据新鲜度"标记字段，策略实现检测到标记为"默认值"时不下判定（即不触发降级也不通过，走默认的保守路径）；(b) 在反序列化后增加后处理校验，若 `invocationCount=0` 但 `lastFailureTime > 0`，将 `failureCount` 标记为不可信状态；(c) 在接受新序列化数据之前丢弃所有旧序列化缓存，从根本上避免新旧数据混用。

### 3.3 [中等] 伪代码中 AiCallRecord 工厂方法参数类型与字段类型不匹配

- **问题描述**：`§4.1` 伪代码在多处使用 `AiCallRecord.failure(capabilityId, System.currentTimeMillis(), ...)` 和 `AiCallRecord.success(...) / degraded(...)` 等工厂方法。但 (a) `AiCallRecord` 在文档中仅定义了字段表，未定义任何工厂方法签名——实现者需要自行推断参数列表及顺序；(b) 伪代码传入 `System.currentTimeMillis()`（long，epoch ms），但 `AiCallRecord.callTime` 字段类型为 `LocalDateTime`，类型不匹配。真实代码中 `AiResult` 的工厂方法 `failure(String errorCode)` 仅接受一个 `String` 参数，与伪代码中的多参数签名完全不同。
- **所在位置**：§4.1 伪代码第 1057、1093、1103、1107 行
- **严重程度**：中等
- **改进建议**：为 `AiCallRecord` 显式定义工厂方法或 Builder 模式的方法签名，确保伪代码中的调用方式与定义一致。若意图使用 Builder（参考 `DegradationContext` 的 Builder 模式），应在 `§3.5 AiCallRecord` 段落补充 Builder 定义。

### 3.4 [中等] StandardCapabilityExecutor 与 ThinAdapterCapabilityExecutor 的复用度评估不足

- **问题描述**：文档定义了 7 个标准 CapabilityExecutor 和 6 个薄适配器 CapabilityExecutor，共 13 个实现类。标准管线包含 8 步（降级预检→模板渲染→实验分流→模型路由→健康检查→模型调用→结果解析→指标采集），薄适配器管线包含 3 步（降级预检→委托 Phase 4→指标采集）。两者的降级预检和指标采集步骤完全一致，但文档未评估是否可将公共行为抽取为骨架抽象类（如 `AbstractCapabilityExecutor`）以减少 13 个实现类中的重复代码。当前设计下，13 个实现类各自需要实现 `execute()` 方法，标准管线中的 7 个实现共享同一套 8 步流程，但文档未定义流程复用机制——要么每个实现重复编写相同逻辑，要么引入一个默认的模板方法实现。
- **所在位置**：§3.1 CapabilityExecutor 职责定义
- **严重程度**：中等
- **改进建议**：在 `CapabilityExecutor` 接口和具体实现之间增加一个 `AbstractCapabilityExecutor` 抽象骨架类，提供 `execute()` 的默认模板方法实现（含 8 步标准管线），子类仅需重写 `extractVariables()`、`getInputType()`、`getOutputType()` 等差异化步骤。薄适配器实现可直接继承或另建一个 `ThinAdapterCapabilityExecutor` 骨架。

### 3.5 [中等] CapabilityExecutor 的线程安全性依赖于隐式 DTO 线程安全

- **问题描述**：`§6.1` 声明 `CapabilityExecutor` "同一能力的高并发请求以无锁方式并行执行"，即无同步保护。但 `execute(T request, ...)` 的入参 `T` 引用直接传递给 `promptTemplateManager.render()`、`structuredOutputParser.parse()` 等下游组件，这些组件内部若修改 `request` 对象（如 `extractVariables()` 调用 `ObjectMapper.convertValue()` 仅为读取操作，是安全的），但若下游组件意外修改了 `request` 对象的状态，跨线程的可见性问题未讨论。此外，`extractVariables()` 的"方式 B 自定义方法"完全由实现者控制，设计无法保证其线程安全性。文档未就 `request` DTO 的不可变性给出明确契约。
- **所在位置**：§6.1 CapabilityExecutor 线程安全段 + §3.1 变量提取约定
- **严重程度**：中等
- **改进建议**：在 `§6.1` 或 `§3.1` 中明确约定：`CapabilityExecutor.execute()` 接收的 `request` 对象在管线执行过程中应视为只读，下游组件不应修改其状态（推荐 DTO 设计为不可变对象）。若方式 B 的 `extractVariables()` 需要修改请求数据，实现者必须在方法内部进行防御性拷贝。
