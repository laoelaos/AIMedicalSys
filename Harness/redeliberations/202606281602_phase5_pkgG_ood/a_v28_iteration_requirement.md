根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 问题 1（重要）：`DiscussionConclusionCapabilityExecutor` 构造器未完整定义

该能力执行器需要额外的 `compressionLightweightEndpoint`（`@Value("${ai.compression.lightweight-endpoint}")`）和 `compressionLightweightClientType`（`@Value("${ai.compression.lightweight-client-type:HTTP_API}")`）两个配置参数，但文档未给出其完整的构造器签名和注入方式。需要在 §3.11.7 末尾或 §3.1 中补充完整构造器签名和 `super()` 调用示例，同时在 §2.3 类图中补充特有字段声明。

### 问题 2（重要）：`executeStandardPipeline()` 抽象定义与伪代码实现之间不一致

§3.1 定义了 `protected final executeStandardPipeline()` 作为子类复用标准管线的机制，但 §4.1 `DiscussionConclusionCapabilityExecutor.doExecuteInternal()` 伪代码在注释中声称"复用标准流程"之后，实际重新实现了实验分流和模板渲染两个步骤的完整伪代码，并仅以注释说明余下步骤。需统一为方案 A（直接调用 `executeStandardPipeline(variables, ...)`）或方案 B（保留展开版本但标注说明目的）。

### 问题 3（重要）：`transcriptSummaryExecutor` 的 `CallerRunsPolicy` 与线程池隔离目的在语义上存在矛盾

当队列满（容量 20）时，`CallerRunsPolicy` 将压缩任务回退到提交线程——即 `llmCallExecutor` 的 Worker 线程，隔离设计在队列满时完全失效。推荐改为 `DiscardPolicy` + WARN 日志，压缩任务被丢弃时在 catch 块的回退逻辑中已处理截断文本行为。

### 问题 4（中等）：13 项能力 DTO 的 4 个公共字段（`visitId`/`patientId`/`sessionId`/`departmentId`）的调用方填充责任未明确定义

文档未明确定义这些字段在当前各业务模块作为 `AiService` 调用方时的实际填充来源。需新增独立"调用方数据准备指引"小节，集中说明填充方式、过渡期为空时的底座行为、以及现有模块兼容性保护。

### 问题 5（中等）：熔断器-端点健康管理器统一探测机制的潜在全量等待风险

当某能力标识对应的 `ModelRouter.route()` 返回的 `ModelRoute` 指向多个 endpointId 时，统一探测决策表仅以单一 endpointId 为判断维度，未定义多端点场景的处理规则。需补充规则说明熔断器状态的作用域粒度，避免 endpoint 级别不可用升级为能力级别的全局熔断。

### 问题 6（轻微）：`DiscussionConclusionCapabilityExecutor` 前置压缩的 `compressionLightweightEndpoint` YAML 配置项未提供默认实例

`@Value("${ai.compression.lightweight-endpoint}")` 没有提供 `:` 默认值语法，若 YAML 中该配置缺失，Spring 启动期将因配置缺失抛出 `IllegalArgumentException`。需改为 `@Value("${ai.compression.lightweight-endpoint:compress-default}")`。

## 历史迭代回顾

### 已解决的问题（出现在历史反馈但当前反馈中不再提及的问题）

- **Phase4ServiceMetaProvider 架构矛盾**（v18.1 严重，v25）：接口归属从 ai-impl/thin-adapter/ 迁移至 ai-api/dto/base/，已解决
- **SlidingWindowMetricsStore @RefreshScope 清空数据**（v20.1 严重）：已移除 @RefreshScope
- **降级路径系统性双重计数**（v16.1 严重）：已修复，降级记录统一由 doDegrade() 承担
- **`super.doExecuteInternal()` 不可用**（v26.2 严重）：已提取为 `executeStandardPipeline()` 方法
- **多处 doDegrade/构造器签名不一致**（v21.1 严重、v22.1 严重、v24.2/3/4）：已通过 CallContext 或恢复旧签名统一
- **薄适配器异常匹配机制不一致**（v19.1 严重、v22.2 重要）：已统一为 instanceof + 两阶段判定
- **DTO 字段对齐与编译错误**（v5.1/2 严重、v9.2 严重、v10.2 严重、v24.1 严重）：已在历史迭代中逐轮修正
- **修订说明与正文混合**（v4.1 严重、v15.2 一般）：已剥离归档

### 持续存在的问题（在多轮反馈中反复出现的问题，需重点解决）

- **`DiscussionConclusionCapabilityExecutor` 压缩配置注入点**（v19.2 一般 → v27.1 重要）：v19 首次指出注入点未定义，v27 再次确认且升级为重要。本轮需完整解决构造器、super()、类图、@Value 默认值、YAML 配置的全链路定义
- **`executeStandardPipeline()` 定义与使用不一致**（v26.2 严重 → v27.2 重要）：v26 指出 `super.doExecuteInternal()` 不可用并建议提取标准管线，v27 实现了该方法但伪代码使用方式不一致。本轮需统一
- **压缩调用线程隔离**（v13.2 一般、v14.4 一般、v17.3 一般 → v27.3 重要）：多轮反复提出压缩调用的线程池隔离问题，v27 的 CallerRunsPolicy 分析进一步暴露了隔离设计缺陷。本轮需彻底修复

### 新发现的问题（本轮新识别的问题）

- **13 项 DTO 公共字段调用方填充责任**（v27.4 中等）：此前未作为独立问题提出，需新增指引小节
- **熔断器多端点场景探测规则**（v27.5 中等）：此前未覆盖的多端点维度，需补充处理规则
- **compressionLightweightEndpoint @Value 默认值缺失**（v27.6 轻微）：v19.2 指出注入点未定义但未关注默认值，本轮新发现的防御性设计缺陷

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281602_phase5_pkgG_ood\a_v27_copy_from_v26.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281602_phase5_pkgG_ood\requirement.md
