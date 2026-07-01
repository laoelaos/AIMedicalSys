# 质量审查报告 — Phase 5 包 G OOD 设计（v6）

## 审查概要

**审查范围**：a_v6_copy_from_v5.md（3538行）
**审查视角**：实际落地评估——设计是否可直接指导编码实现、接口定义是否足以支持下游消费者、异常场景和边界条件是否已考虑
**迭代背景**：第6轮审查，v2~v5已修复37项问题（其中严重7项、重要5项），当前v6已剥离修订说明并清理过程性标记

---

## 发现问题

### 1. [严重] 薄适配器构造器的 `super()` 调用参数数量与父类构造器不匹配

**问题描述**：`DiagnosisCapabilityExecutor`（§3.1 第916行）的构造器伪代码中 `super(null, null, null, null, metricsCollector, metricsStore, null, degradationStrategyMap, localRuleFallback)` 仅传递9个参数，但 `AbstractCapabilityExecutor` 的构造器（§3.1 第1182-1193行）定义接受11个参数。缺失的两个参数为 `@Qualifier("capabilityTimeoutConfig") Map<String, Duration> capabilityTimeoutConfig` 和 `@Value("${ai.execution.timeout.thin-adapter-default:30s}") Duration thinAdapterTimeout`。此问题导致薄适配器子类无法编译通过，且 `thinAdapterTimeout` 字段注入失败后将导致后续"薄适配器独立超时"机制（§3.1 第1169行描述的 `delegateFuture.get(thinAdapterTimeout.toMillis(), ...)`）在运行时因NPE而崩溃。

**所在位置**：§3.1 第910-919行（薄适配器构造器） vs §3.1 第1182-1193行（AbstractCapabilityExecutor构造器）

**严重程度**：严重

**改进建议**：在薄适配器构造器的 `super()` 调用中补全两个缺失参数——增加 `capabilityTimeoutConfig` 和 `thinAdapterTimeout` 参数注入，或将 `thinAdapterTimeout` 的 `@Value` 注入移至薄适配器子类构造器内并在 `super()` 中传入。同时修正所有6项薄适配器的构造器伪代码（`DiagnosisCapabilityExecutor`、`AnalysisReportForInspectionCapabilityExecutor` 等）。

---

### 2. [重要] 类图 `LlmChatOptions` 缺失 `topP`/`frequencyPenalty`/`presencePenalty` 三个字段

**问题描述**：§2.3 类图中 `LlmChatOptions` 仅显示 `modelId`、`temperature`、`maxTokens`、`stopSequences` 四个字段。但 §3.2 第1447-1449行的字段级契约中已明确定义了 `topP`、`frequencyPenalty`、`presencePenalty` 三个额外字段并在参数映射扩展点规则中一并阐述。类图与正文描述不一致，实现者若仅参照类图进行编码将遗漏这三个字段。

**所在位置**：§2.3 类图 `LlmChatOptions`（行428-433） vs §3.2 LlmChatOptions 字段级契约（行1447-1451）

**严重程度**：重要

**改进建议**：在 §2.3 类图的 `LlmChatOptions` 中补充 `topP: Double`、`frequencyPenalty: Double`、`presencePenalty: Double` 三个字段声明。

---

### 3. [重要] 类图 `ModelRoute` 缺失 `authType` 字段

**问题描述**：§3.2 第1512-1520行的 `ModelRoute` 字段扩展表中包含 `authType: AuthType enum` 字段，但在 §2.3 类图中 `ModelRoute` 仅显示 `endpointId`、`modelId`、`endpoint`、`clientType`、`connectionTimeout`、`readTimeout`、`parameters` 七个字段，未包含 `authType`。同样，`Credential` 值对象和 `CredentialProvider` 接口在类图中也存在但 `authType` 枚举的关联关系不完整——`Credential` 类图中已声明 `AuthType authType` 字段，但 `ModelRoute` 与 `AuthType` 的关联未补充。

**所在位置**：§2.3 类图 `ModelRoute`（行379-387） vs §3.2 ModelRoute 字段扩展表（行1512-1520）

**严重程度**：重要

**改进建议**：在 §2.3 类图的 `ModelRoute` 中补充 `authType: AuthType` 字段声明，并添加从 `ModelRoute` 到 `AuthType` 的使用关联线（`ModelRoute --> AuthType : uses`）。

---

### 4. [中等] 类图缺少 `RecommendedDepartment` 等内嵌值对象定义

**问题描述**：§3.11.1 第2408行定义了 `TriageResponse` 包含 `recommendedDepartments: List<RecommendedDepartment>`，并明确定义了 `RecommendedDepartment` 的内嵌字段（`departmentId`、`departmentName`、`recommendationWeight`）。但 §2.3 类图未包含任何 `RecommendedDepartment` 定义。类似地，§3.11.2 的 `CheckResult`、§3.11.3 的 `RecordSection` 和 `SectionType`、§3.11.4 的 `MedicationRecommendation` 和 `ConfidenceLevel` 等内嵌类型在类图中均缺失。设计文档正文定义了这些类型但在表达整体架构的核心图示中未见，实现者依赖类图作为编码起点时将忽略这些业务类型。

**所在位置**：§2.3 类图 vs §3.11.1–§3.11.7 各能力特化表

**严重程度**：中等

**改进建议**：在 §2.3 类图中新增 `RecommendedDepartment`、`CheckResult`、`RecordSection`、`SectionType`、`MedicationRecommendation`、`ConfidenceLevel`、`DiscussionTranscript`、`ShiftAssignment`、`ScheduleConstraint`、`KbSource` 等关键内嵌值对象的类定义，或至少在 §2.3 末尾添加注释说明"各能力 DTO 的内嵌值对象定义见 §3.11 特化表"以避免遗漏。

---

### 5. [中等] `execute()` 伪代码引用未明确定义的 `this.degradationStrategies` 字段

**问题描述**：`AbstractCapabilityExecutor.execute()` 模板方法伪代码（§4.1 第2590行）中引用 `this.degradationStrategies` 作为策略遍历的迭代源，但在 `AbstractCapabilityExecutor` 的构造器（§3.1 第1196行）和字段声明中仅提及 `this.degradationStrategyMap`（`Map<String, List<DegradationStrategy>>`）。两个伪代码之间缺少一个"按 `getCapabilityId()` 从 Map 中查找对应策略列表并赋值给 `degradationStrategies` 字段"的中间步骤。实现者需要自行发现此断点，可能导致按能力配置的策略白名单不生效，所有能力实际使用同一套策略。

**所在位置**：§4.1 第2590行（`execute()` 伪代码中的 `this.degradationStrategies`） vs §3.1 第1192-1196行（构造器仅存储 `this.degradationStrategyMap`）

**严重程度**：中等

**改进建议**：在 `execute()` 伪代码中新增显式查找步骤，如 `this.degradationStrategies = degradationStrategyMap.getOrDefault(capabilityId, List.of())`，或在构造器伪代码参数说明处补充"execute() 中按 getCapabilityId() 从 Map 中查找对应策略列表"的实现指引。

---

### 6. [中等] §4.1 结构化输出回退的 `structuredChat().join()` 阻塞未建立超时保护

**问题描述**：§4.1 第2695行伪代码中 `llmChatService.structuredChat(chatRequest, outputType).join()` 在 `llmCallExecutor` 线程内执行阻塞等待。`orTimeout()` 设于 `supplyAsync()` 外部，能触发外层 `CompletionException` 超时，但此时 `llmCallExecutor` 内的线程仍被 `join()` 阻塞，成为"僵尸线程"——超时后不会释放线程池资源。§3.2 第1416-1417行描述的缓解措施（"内部较短超时如 capabilityTimeout 的 60%"）在 §4.1 的伪代码中完全没有体现。实现者仅按 §4.1 伪代码编码，将产生与结构化输出回退路径相关的潜在线程池耗尽风险。

**所在位置**：§4.1 第2695行（`structuredChat().join()` 无内部超时） vs §3.2 第1414-1417行（描述的超时叠加缓解措施未体现在伪代码中）

**严重程度**：中等

**改进建议**：在 §4.1 伪代码中为 `structuredChat().join()` 补充内部超时机制——例如使用 `chatFuture.get(capabilityTimeout * 60%, TimeUnit.MILLISECONDS)` 替代 `.join()`，并在超时后直接降级而非继续回退到 `chat()`。同时在 §4.1 伪代码注释中标注与 §3.2 超时叠加风险缓解措施的关联关系。

---

### 7. [中等] `PrescriptionLocalRuleFallback` 全跳过时返回 `AiResult.success()` 与降级语义矛盾

**问题描述**：§3.7 第2098-2101行伪代码中，当 `PrescriptionLocalRuleFallback` 所有检查项均因数据源不可用而跳过时，返回 `AiResult.success(result)`。但此处是降级路径中的执行分支——管线已因 LLM 调用失败或降级预检命中而进入 `doDegrade()`，最终调用此 Fallback。返回 `success()` 使得 `AiMetricsCollector` 将此次调用记录为"成功"而非"降级"，指标统计失准。此外，返回 `success()` 意味着 `AiResult.isSuccess()=true`，下游消费者无法通过标准 `AiResult` 状态判定区分"正常 AI 审核通过"与"降级后因数据不可用默认放行"两种场景。

**所在位置**：§3.7 第2098-2101行（fallback 伪代码的 allChecksSkipped 分支）

**严重程度**：中等

**改进建议**：将此场景的返回值改为 `AiResult.degraded("LOCAL_RULE_DATA_UNAVAILABLE")`（语义明确为降级），并在 `result` 中保留 `dataSourceFailed: true` 供业务方按需检查。同时在 §5.1 错误分类表中新增此降级场景的描述。若业务确实要求"不阻塞处方流程"的语义，应在 `AiResult.success()` 返回前通过 `AiCallRecord` 单独记录降级状态，而非让降级路径返回成功标记。

---

### 8. [一般] `AiCallLogStats` Entity 缺少 Repository 定义与目录结构登记

**问题描述**：§3.5 第1948-1965行明确定义了 `AiCallLogStats` JPA Entity（与 `ai_call_log_stats` 表映射）及其字段定义、索引策略和写入查询伪代码。但： (1) §2.1 目录结构中 `metrics/` 子包未包含 `AiCallLogStats.java` 或 `AiCallLogStatsRepository.java`； (2) §2.3 类图中无 `AiCallLogStats` 类及相关 Repository； (3) §3.9 的 JPA 扫描配置行 `basePackages = "com.aimedical.modules.ai.impl"` 虽然能覆盖到 `AiCallLogStats`，但未明确提及。实现者清点工作量时容易遗漏此 Entity 的创建。

**所在位置**：§3.5 第1948-1965行（Entity 定义） vs §2.1 目录结构（metrics/ 子包） vs §2.3 类图

**严重程度**：一般

**改进建议**：在 §2.1 目录结构中 `metrics/` 子包补充 `AiCallLogStats.java` 和 `AiCallLogStatsRepository.java` 文件条目；在 §2.3 类图中补充 `AiCallLogStats` 类及与 `AiCallLogStatsRepository` 的关联关系。

---

### 9. [一般] YAML 配置示例缺少 `transcript-summary` 超时配置项

**问题描述**：§3.11.4 第2492行定义了 `DiscussionConclusionCapabilityExecutor` 的前置 LLM 调用超时 `transcriptSummaryTimeout`，默认15秒，注入方式为 `@Value("${ai.execution.timeout.transcript-summary:15s}")`。但 §9.5 的 YAML 配置示例中 `execution.timeout` 块下未包含 `transcript-summary` 配置项。运维人员查阅配置示例时无法知悉此参数的存在与如何调整。

**所在位置**：§3.11.4 第2492行（transcriptSummaryTimeout 定义） vs §9.5 YAML 配置（第3334-3365行，execution.timeout 块）

**严重程度**：一般

**改进建议**：在 §9.5 YAML 的 `execution.timeout` 块中补充 `transcript-summary: 15s` 配置项，并添加注释说明其用途（讨论结论能力的前置 LLM 摘要调用超时）。

---

### 10. [一般] `AiOrchestrator.handle()` 异常 catch 块中 `aiResult.isSuccess()` 的拼写与上下文不一致

**问题描述**：§4.1 第2697行伪代码中 `aiResult.isSuccess()` 的命名风格与其他 `AiResult` 工厂方法（`success()`/`failure()`/`degraded()`）和 `AiCallRecord` 工厂方法（`success()`/`failure()`/`degraded()`）不统一——§1.3 核心抽象表中 `AiResult<T>` 的职责描述未定义 `isSuccess()` 方法，其他伪代码中均使用 `isSuccess()`（第2697、2730、2740行等）但 `AiResult` 类本身未在 §1.3 或 §3.x 中正式定义其方法签名（除工厂方法外）。实现者不知 `isSuccess()` 是 `AiResult` 接口的成员方法还是通过某种状态枚举推导。

**所在位置**：§4.1 第2697、2730、2740行（aiResult.isSuccess()） vs §1.3 AiResult 描述（仅提到"完成后 AiResult 为不可变对象"，未定义 isSuccess() 方法）

**严重程度**：一般

**改进建议**：在 §1.3 核心抽象表的 `AiResult<T>` 行补充方法签名定义：`isSuccess(): boolean`（区分 success/failure/degraded 三种状态），或明确说明 `isSuccess()` 的语义——"返回 true 当且仅当结果为 success 状态"。同时建议增加 `getStatus(): AiResultStatus` 方法（AiResultStatus 枚举：SUCCESS/FAILURE/DEGRADED）以支持更精确的三态判断。

---

### 11. [一般] `PromptTemplate` 状态模型中 `DEPRECATED → ACTIVE` 回滚操作的并发安全性未定义

**问题描述**：§3.3 第1688行描述了 `DEPRECATED → ACTIVE` 的回滚操作："回滚操作自动将当前 ACTIVE 版本降级为 DEPRECATED，保持'同一 capabilityId + departmentId 组合同时仅一个 ACTIVE'的不变量"。但此操作的原子性未定义——若两个管理员同时对同一能力+科室执行回滚操作，可能出现两个 ACTIVE 版本的并发竞争。涉及数据库 UPDATE 操作和非幂等的版本状态转换，在管理端并发场景下存在数据不一致风险。

**所在位置**：§3.3 PromptTemplate 状态模型（行1688） vs §11 测试策略中缺少此场景的并发验证

**严重程度**：一般

**改进建议**：在 §3.3 中补充回滚操作的并发安全约束——推荐使用数据库行级锁（`SELECT ... FOR UPDATE`）或乐观锁（`@Version`）保护状态转换，确保同一时刻仅一个 ACTIVE 版本的不变量。同时，在 §11 测试策略中补充回滚操作的并发竞争验证场景。

---

## 整体质量评价

文档经过5轮迭代审查后，核心的质量问题（事实错误、删除冗余修订标记、类图-正文不一致等）已基本修复。当前文档在以下方面达到较高成熟度：

- **需求响应充分度**：覆盖了需求中全部OOD设计要素（类图、职责、协作、接口、状态模型），并完成了5次迭代修正
- **深度与完整性**：§3.x 对13项CapabilityExecutor、降级策略、LLM客户端抽象、模板管理、实验管理等核心抽象的设计粒度足以指导编码实现
- **跨阶段一致性**：§1.2明确列出5条风格规则并标注来源

但本报告指出的11项问题中，**1项严重**（薄适配器构造器参数不匹配 → 编译失败）、**2项重要**（类图字段缺失 → 编码参考遗漏）、**4项中等**（伪代码/语义/超时保护缺陷），合计7项需要修复者关注。建议优先修复问题1（编译阻断）和问题2~3（类图完整度），其次修复问题5~7（行为正确性），其余问题可编排至后续迭代处理。
