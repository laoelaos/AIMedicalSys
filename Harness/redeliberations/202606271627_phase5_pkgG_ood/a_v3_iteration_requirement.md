根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### [CRITICAL] §4.1 降级路径伪代码中指标采集语句位于 return 之后不可达
降级路径伪代码的 `degrade:` 标签下，两条指标采集语句（`metricsCollector.record()` 和 `slidingWindowMetricsStore.recordFailure()`）被放置在两个分支的 `return` 语句之后。无论走 `localRuleFallback.fallback(request)` 分支还是 `AiResult.degraded(reason)` 分支，指标采集代码均不可达。改进建议：将指标采集和滑动窗口记录移到 `return` 语句之前，或重构为 try-finally。

### [CRITICAL] MockAiService 的 @ConditionalOnProperty 属性名与现有代码不一致
设计声明 `MockAiService` 标注 `@ConditionalOnProperty(name = "ai.platform.enabled", havingValue = "false", matchIfMissing = true)`，但现有代码实际使用 `@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "true", matchIfMissing = true)`。改进建议：方案 A（推荐）保留 `ai.mock.enabled`，在 `AiPlatformConfig` 中通过 `@ConditionalOnProperty` 读取 `ai.platform.enabled` 内部转发；方案 B 在 §9.2 显式说明迁移方案。

### [CRITICAL] DegradationStrategy 接口新增 getOrder() 方法破坏现有实现
向已发布的接口新增非 default 抽象方法会破坏所有现有实现（包括 `NoOpDegradationStrategy`），导致编译失败。改进建议：改为 Java 8 `default` 方法，或使用 Spring `@Order` 注解 + `Ordered` 接口替代。

### [MAJOR] 类图中 AiService 方法签名缺少 CompletableFuture 异步包装
类图中 AiService 接口的方法签名全部显示为同步返回类型 `AiResult<T>`，但实际 `AiService.java` 中全部方法均返回 `CompletableFuture<AiResult<T>>`。此错误沿用到 AiOrchestrator、CapabilityExecutor 的类图。改进建议：类图中方法签名修正为 `CompletableFuture<AiResult<T>>`，同步管线桥接异步接口的设计决策应在 §3.1 或 §6 中明确。

### [MEDIUM] 降级路径中 localRuleFallback 成功场景被错误记录为 recordFailure
降级路径第 850 行对所有降级场景统一标记为失败。当 `localRuleFallback` 存在且成功返回业务结果时，不应记为失败。改进建议：区分"完全退化→recordFailure"与"降级到本地规则成功→recordSuccess（标记 degraded=true）"。

### [MEDIUM] AiRequestBase 基类在设计和代码库中均未定义
AiCallRecord 字段填充策略提到"各能力 DTO 均继承 AiRequestBase 基类"，但该基类在类图、整个文档以及现有代码库中均不存在。改进建议：显式定义 AiRequestBase 基类（字段、包路径、继承关系），或在类图中补充该类型。

### [MEDIUM] 类图中 AiService 方法名与现有接口不匹配
`medicalRecordGen` → 实际 `generateMedicalRecord`；`kbQuery` → 实际 `knowledgeBaseQuery`。若 AiOrchestrator 按类图生成的签名实现，将无法通过编译。改进建议：将类图中的方法名修正为与实际代码一致。

### [MEDIUM] LlmClient 状态归属存在表述矛盾
§3.2 将模型端点状态模型放在 LlmClient 节下描述，暗示 LlmClient 维护此状态；但 §6.1 明确声言"LlmClient：无状态，线程安全"。改进建议：明确状态模型归属组件，补充探测调用触发机制设计。

### [MEDIUM] AiCallLogEntity 遗漏字段级 JPA 映射索引覆盖度不足
缺少降级/模型/角色维度的覆盖索引（如 `degraded + call_time`、`model_id + call_time`、`caller_role + call_time`），`call_time` 字段缺失 `precision`/`columnDefinition` 定义。改进建议：补充维度的覆盖索引评估，明确 `call_time` 的列定义（如 `DATETIME(3)`）。

## 历史迭代回顾

### 已解决的问题
以下为第 1 轮迭代识别但第 2 轮审查中不再提及的问题（已解决）：
- 缺失 UML 类图 — v2 已补充完整 Mermaid classDiagram
- 状态模型覆盖严重不足 — v2 已为 PromptTemplate、Experiment、LlmClient、CircuitBreaker 等补充状态模型
- CapabilityExecutor 接口缺少方法签名 — v2 已补充完整方法签名（execute、getCapabilityId、getInputType、getOutputType）
- Bean 装配二义性 — v2 已使用 `@Primary` + `ObjectProvider<AiService>` + `ai.platform.enabled` 互斥策略
- 新增策略自动注入问题 — v2 已将降级判定移入编排层通过 `SlidingWindowMetricsStore` 提供数据
- AiOrchestrator"无状态"断言不符 — v2 已修订线程模型说明
- DegradationContext 序列化兼容性 — v2 已补充二进制兼容性分析
- AiCallLog JPA 实体未定义 — v2 已新增 AiCallLogEntity
- AiMetricsCollector 异步队列溢出策略未定义 — v2 已补充 CallerRunsPolicy
- Micrometer 依赖未确认 — v2 已显式声明
- AiCallRecord 与 AiCallLogEntity 字段不一致 — v2 已对齐字段集合

### 持续存在的问题
以下为第 2 轮审查中识别、v2 设计**未修正**的问题，需在本轮重点解决：
- §4.1 降级路径伪代码中指标采集语句位于 return 之后不可达
- MockAiService 的 @ConditionalOnProperty 属性名与现有代码不一致
- DegradationStrategy 接口新增 getOrder() 方法破坏现有实现
- 类图中 AiService 方法签名缺少 CompletableFuture 异步包装
- 降级路径中 localRuleFallback 成功场景被错误记录为 recordFailure
- AiRequestBase 基类在设计和代码库中均未定义
- 类图中 AiService 方法名与现有接口不匹配
- LlmClient 状态归属存在表述矛盾
- AiCallLogEntity 遗漏字段级 JPA 映射索引覆盖度不足

### 新发现的问题
无。本轮审查未识别出第 2 轮诊断覆盖范围之外的新问题。

## 上一轮产出路径
C:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606271627_phase5_pkgG_ood/a_v2_design_v2.md

## 用户需求
C:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606271627_phase5_pkgG_ood/requirement.md
