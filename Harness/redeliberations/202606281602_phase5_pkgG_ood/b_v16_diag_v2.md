# 质量审查报告（v2）

## 审查概况

- **审查对象**：Phase 5 包 G — AI 进阶底座 架构级 OOD 设计方案（v16）
- **审查维度**：需求响应充分度、事实正确性、逻辑一致性、深度与完整性、落地可执行性
- **审查原则**：侧重内部审议（组件A设计-验证循环）未充分覆盖的维度，避免重复验证已确认项

---

## 需求响应充分度评估

对照用户需求（requirement.md）的三项核心要求进行评估：

### 需求1：参考Phase0/Phase1ABD设计成果，保持风格和结构一致性

**评估结果：已满足**

产出§1.2显式列出5条具体的风格一致性规则，并标注各规则的来源阶段。§3各核心抽象使用"角色/职责/协作对象/类型形态选择理由"四元组格式，§4使用缩进式伪代码，§7使用"决策/选项/选择/理由"四列表格，§5.1使用"错误类别/代表场景/处理方式/响应形态"四列表格——均与Phase0和Phase1ABD的格式一致。

### 需求2：完成Phase5包G的完整OOD设计

**评估结果：已满足**

产出覆盖了Phase5包G（AI进阶底座）的完整设计范围：
- 13项AI能力的统一编排架构（7项底座完整管线 + 6项薄适配器）
- 模型路由、Prompt模板管理、A/B实验、性能观测四大基础服务
- 大模型对话客户端层及多实现分发
- 降级策略体系（Timeout/CircuitBreaker）及滑动窗口数据源
- 结构化输出解析、本地规则降级、凭据管理、端点健康管理等配套组件
- 非功能性分析（冷启动、内存、连接池、启动时间）
- 迁移路径、测试策略、依赖清单、协作边界

### 需求3：覆盖类图、核心职责、协作关系、关键接口、状态模型等OOD核心要素

**评估结果：已满足**

| OOD要素 | 覆盖位置 | 说明 |
|---------|---------|------|
| 类图 | §2.3 | 完整的Mermaid类图，含50+类节点及关联关系 |
| 核心职责 | §3.1-§3.11 | 每个核心抽象以角色/职责格式描述 |
| 协作关系 | §3.x各协作对象段 | 每个抽象明确列出协作组件及协作方式 |
| 关键接口 | §1.3核心抽象表、§2.3类图、§3.x方法签名 | 接口方法签名、入参、返回值、异常均显式定义 |
| 状态模型 | CredentialProvider、ModelEndpointHealthManager、Experiment、PromptTemplate、CircuitBreaker | 5个组件具有完整状态机定义及状态转换表 |

### 总体需求响应结论

产出对三项用户需求的响应是完整的。设计风格经15轮迭代已与Phase0/Phase1ABD对齐，包G的OOD设计范围完整覆盖全部13项AI能力及配套基础设施，类图、职责、协作、接口、状态模型等OOD核心要素均有形式化定义。需求响应维度不存在缺口。

---

## 审查发现

### 问题A：[严重] 降级路径系统性双重计数

**问题描述**：§4.1 doExecuteInternal() 中的多条降级路径存在指标双重计数问题。异常处理分支先主动记录 metricsCollector.record(AiCallRecord.failure(...)) + slidingWindowMetricsStore.recordFailure(capabilityId)，然后调用 doDegrade()，后者在无 LocalRuleFallback 时又记录 metricsCollector.record(AiCallRecord.degraded(...)) + slidingWindowMetricsStore.recordFailure(capabilityId)。导致同一次调用在指标系统中同时记入"失败"和"降级"两个分类，失败率统计偏高，总调用计数失真。

**受影响位置**：
- §4.1 结构化超时降级路径：先预记 failure，再调用 doDegrade() 记 degraded+failure
- §4.1 chat回退超时降级路径：同上
- §4.1 LlmInfrastructureException 降级路径：同上

**严重程度**：严重

**改进建议**：对齐 v15 已修复的 DEGRADED 预记问题方案——异常处理分支在调用 doDegrade() 前移除所有 metricsCollector.record() 和 slidingWindowMetricsStore.recordFailure() 的预记录调用，将指标和滑动窗口的记录职责完全委托给 doDegrade() 统一承担。确保同一次调用在整个降级路径中仅执行一次指标记录。

---

### 问题B：[重要] AiCallRecord 工厂方法缺少哨兵参数

**问题描述**：§3.4 定义了 AiCallRecord.promptVersion 空值守卫标记方案——实验分流异常场景写入 prompt_version=-1 哨兵值以区分正常未命中的 null 值。但 §3.5 AiCallRecord 的工厂方法签名中既无 sentinelReason 参数也无区分异常降级和正常未命中的其他机制。转换逻辑仅停留在设计描述层面，未在接口定义和伪代码中落地，实现者无从知晓如何区分"实验分流异常"与"无实验命中"两种场景。

**受影响位置**：
- §3.4 AiCallRecord.promptVersion 空值守卫标记方案段落
- §3.5 AiCallRecord 工厂方法签名
- §4.1 降级调用点中 promptVersion 的传递逻辑

**严重程度**：重要

**改进建议**：在 AiCallRecord 工厂方法中增加 sentinelReason 可选参数（可通过 Builder 模式或重载方法），使实验分流异常降级路径能标记 promptVersion=-1；或在 AiCallRecord 中增加 promptVersion 与 sentinelReason 的字段级关联逻辑。同步更新 §4.1 中所有实验分流异常降级调用点的伪代码。

---

### 问题C：[重要] structuredChat 内部超时与 orTimeout 的时间竞争

**问题描述**：§4.1 中 structuredChat 使用内部超时 structuredChatTimeout（capabilityTimeout 的 60%），但 orTimeout() 从 supplyAsync() 启动时即开始计时。若 doExecuteInternal() 中模板渲染、实验分流等前置步骤消耗了可观时间，则 orTimeout 的剩余时间可能已显著小于 structuredChatTimeout。此时结构化调用将被 orTimeout 而非内部超时接管，超时分类和降级原因记录失准（降级原因被记为整体超时而非 structuredChat 内部超时）。

**受影响位置**：
- §4.1 doExecuteInternal() 内外超时的时序关系
- §3.2 structuredChat() 回退路径超时叠加风险段落

**严重程度**：重要

**改进建议**：在 supplyAsync() lambda 入口处（doExecuteInternal() 首行）即时计算 remainingBeforeOrTimeout = capabilityTimeout - elapsedBeforeLambda，然后按剩余时间分配 structuredChat 和 chat 回退的内部超时阈值，而非使用原始 capabilityTimeout 的固定比例。同步更新 §4.1 中结构化超时的拆分配置逻辑和 §3.2 的时序说明。

---

### 问题D：[一般] parseTimeout 与 chatFallbackTimeout 的层级约束未定义

**问题描述**：§4.1 中 StructuredOutputParser.parse() 使用独立 parseTimeout（默认 5s，可逐能力配置），而 chat 回退使用 chatFallbackTimeout（capabilityTimeout 的 40%）。当 parseTimeout > chatFallbackTimeout 时，整体超时先到，降级原因被记为 chat 回退超时而非解析超时；当 parseTimeout < chatFallbackTimeout 时解析先超时。两超时的层级关系和优先级约束未在设计中定义，实现者无法判断合规的配置范围。

**受影响位置**：
- §4.1 parseTimeout 与 chatFallbackTimeout 的时序关系
- §9.5 execution.timeout.parse 配置块
- §3.2 structuredChat() 回退路径超时叠加风险段落

**严重程度**：一般

**改进建议**：在 §3.2 或 §3.9 中补充 parseTimeout 与 chatFallbackTimeout 的约束关系——parseTimeout 应 <= chatFallbackTimeout，防止解析超时被外层超时掩盖。在 §9.5 YAML 配置注释中为 parse.per-capability 添加约束说明。在 AiPlatformConfig 的 @PostConstruct 配置校验中增加两者比较逻辑。

---

### 问题E：[一般] 分布式重构优先级遗漏熔断器-滑动窗口依赖链

**问题描述**：§1.5.3 列出分布式重构优先级为 CircuitBreakerDegradationStrategy（最高）> EndpointRateLimiter（重要）> SlidingWindowMetricsStore（可选）。但 CircuitBreakerDegradationStrategy 的状态判定依赖 SlidingWindowMetricsStore.getFailureRate() 提供实时失败率数据。若优先重构熔断器为分布式而保持滑动窗口为单体，熔断器将失去数据源。此依赖关系未在重构优先级段或 §3.8 中记录。

**受影响位置**：
- §1.5.3 迁移路径
- §3.8 CircuitBreakerDegradationStrategy 描述

**严重程度**：一般

**改进建议**：在 §1.5.3 中补充依赖链说明——CircuitBreakerDegradationStrategy 的分布式重构需与 SlidingWindowMetricsStore 的分布式升级同步或顺序在前。在 §3.8 的"协作对象"段补充数据源依赖说明。

---

### 问题F：[一般] DiscussionConclusionCapabilityExecutor 前置压缩调用的超时控制未在伪代码中体现

**问题描述**：§3.11.7 已定稿 transcriptSummaryExecutor 线程池隔离方案和 transcriptSummaryTimeout（15s）配置项，但 §4.1 的 doExecuteInternal() 伪代码中未体现 DiscussionConclusionCapabilityExecutor 的前置 LLM 压缩调用逻辑。实现者无法从 §4.1 的通用 doExecuteInternal() 伪代码中推导出讨论结论能力的特殊流程（前置压缩--截断回退--异步等待）。

**受影响位置**：
- §3.11.7 前置 LLM 调用契约和线程模型隔离方案
- §4.1 doExecuteInternal() 通用伪代码
- §9.5 YAML transcript-summary 配置项

**严重程度**：一般

**改进建议**：在 §4.1 中为 DiscussionConclusionCapabilityExecutor 补充特化的 doExecuteInternal() 伪代码段（或作为独立子节），覆盖前置压缩调用的超时、截断回退条件、隔离线程池提交逻辑。

---

## 总体评价

本产出经过 15 轮迭代审查和修改，已解决绝大多数前期发现的事实错误、逻辑矛盾和设计缺口，整体成熟度较高。需求响应充分度评估确认三项用户需求均已完整覆盖，无缺口。仍存在的 6 项问题中，1 项为严重（降级路径异常处理分支的双重计数——与 v15 已修复的同类问题同根，但 3 处异常处理分支未被覆盖），2 项为重要（接口定义遗漏、超时时序竞争），3 项为一般（未定义的约束关系、遗漏的依赖记录、伪代码缺口）。建议产出作者优先修复问题A和B，其余可在实施期同步处理。

---

## 修订说明（v2）

| 质询意见 | 回应 |
|---------|------|
| 1. [严重] 诊断报告缺失"需求响应充分度"评估维度 | **已修订**：在「审查概况」之后新增「需求响应充分度评估」独立章节，逐项对照 requirement.md 的三项核心要求进行通过/未通过评价，评估结论为"均已满足，无缺口"。 |
| 2. [重要] 问题A（双重计数）和问题C（超时时序竞争）属于技术可行性维度，与组件A内部审议存在维度重叠 | **接受方向但保留问题**：两问题的技术可行性属性与内部审议存在部分重叠，但并非"内部审议已确认并解决"的重复项——问题A的降级路径异常处理分支在 v15 修复中未被覆盖（属于同一根因的不同实例），问题C的60%/40%固定比例超时分配逻辑在 v16 中仍未基于剩余时间计算，内部审议未涉及此精度。维度重叠已在本修订说明中记录，问题A和C保留原严重级别不变。 |
