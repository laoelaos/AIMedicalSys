# Phase 5 包 G — AI 进阶底座 OOD 设计 质量审查报告（v2）

审查轮次：第 2 次
审查时间：2026-06-27
审查范围：需求响应充分度、事实错误/逻辑矛盾、深度与完整性（侧重内部审议未覆盖维度）

---

## 1. 事实错误与逻辑矛盾

### 1.1 [CRITICAL] 降级路径伪代码中指标采集语句位于 return 之后不可达

- **问题描述**：§4.1 降级路径伪代码（第 843-850 行）中，`degrade:` 标签下的两条指标采集语句（`metricsCollector.record()` 和 `slidingWindowMetricsStore.recordFailure()`）被放置在两个分支的 `return` 语句之后。无论走 `localRuleFallback.fallback(request)` 分支还是 `AiResult.degraded(reason)` 分支，指标采集代码均不可达。这会导致每次降级场景下的调用记录和滑动窗口数据完全丢失，降级率统计失真，熔断器参数被污染。
- **所在位置**：§4.1 降级路径伪代码（原文第 843-850 行）
- **严重程度**：严重（CRITICAL）
- **改进建议**：将指标采集和滑动窗口记录移到 `return` 语句之前，或重构为 try-finally 确保无论走哪个降级分支均能记录指标。

### 1.2 [CRITICAL] MockAiService 的 @ConditionalOnProperty 属性名与现有代码不一致

- **问题描述**：§3.1 Bean 装配策略声明 `MockAiService` 标注 `@ConditionalOnProperty(name = "ai.platform.enabled", havingValue = "false", matchIfMissing = true)`。但现有代码（`MockAiService.java:40`）实际使用 `@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "true", matchIfMissing = true)`。将配置属性从 `ai.mock.enabled` 更换为 `ai.platform.enabled` 是破坏性变更：
  - 已部署环境中使用 `ai.mock.enabled=true` 的配置将静默失效，MockAiService 不再激活；
  - 未显式配置 `ai.platform.enabled` 的现有环境因 `matchIfMissing = true` 会意外激活 Mock 行为；
  - 设计未给出任何迁移方案（如配置兼容层、启动期告警或转换建议）。
- **所在位置**：§3.1 Bean 装配策略 + §9.2 配置切换 + 对比现有 MockAiService.java:40
- **严重程度**：严重（CRITICAL）
- **改进建议**：方案 A（推荐）：保留 `ai.mock.enabled` 属性，在 `AiPlatformConfig` 中通过 `@ConditionalOnProperty` 读取 `ai.platform.enabled`，内部转发给 `ai.mock.enabled`（或做逻辑映射），确保存量配置兼容。方案 B：在 §9.2 配置切换中显式说明迁移方案，对旧属性提供弃用告警。

### 1.3 [CRITICAL] DegradationStrategy 接口新增 getOrder() 方法破坏现有实现

- **问题描述**：§3.1 和 §3.8 多处提及 DegradationStrategy 接口包含 `getOrder()` 方法（用于按优先级排序策略链），类图中也明确该接口声明 `+getOrder() int`。但现有 `DegradationStrategy.java` 中仅有 `shouldDegrade(DegradationContext context)` 方法，`getOrder()` 不存在。向已发布的接口新增非 default 抽象方法会破坏所有现有实现（至少包括 `NoOpDegradationStrategy`），导致编译失败。设计未讨论此变更的向后兼容性。
- **所在位置**：§3.1 降级判定流程 + §3.8 DegradationStrategy 类图 + 现有 DegradationStrategy.java:3-6
- **严重程度**：严重（CRITICAL）
- **改进建议**：改为 Java 8 `default` 方法（`default int getOrder() { return 0; }`）保持二进制兼容，或使用 Spring `@Order` 注解 + `Ordered` 接口替代。

### 1.4 [MAJOR] 类图中 AiService 方法签名缺少 CompletableFuture 异步包装

- **问题描述**：§2.3 类图中 AiService 接口的方法签名全部显示为同步返回类型 `AiResult<T>`（如 `+triage(TriageRequest) AiResult~TriageResponse~`）。但实际 `AiService.java` 中全部 13 个方法均返回 `CompletableFuture<AiResult<T>>`。这一错误沿用到全篇：AiOrchestrator 类图、CapabilityExecutor 的 `execute()` 方法签名（设计为 `AiResult<R>`）均未考虑异步包装。若 AiOrchestrator 要实现 AiService 接口，其方法签名必须返回 `CompletableFuture<AiResult<T>>`，但当前设计中执行管线是同步的，缺少异步包装层的设计说明。
- **所在位置**：§2.3 类图 AiService、AiOrchestrator、CapabilityExecutor 方法签名
- **严重程度**：重要（MAJOR）
- **改进建议**：类图中 AiService 和 AiOrchestrator 的方法签名应修正为 `CompletableFuture<AiResult<T>>`。同步管线通过 `CompletableFuture.supplyAsync()` 或 `CompletableFuture.completedFuture()` 桥接异步接口的设计决策应在 §3.1 或 §6 中明确。

### 1.5 [MEDIUM] 降级路径中 localRuleFallback 成功场景被错误记录为 recordFailure

- **问题描述**：§4.1 降级路径第 850 行 `slidingWindowMetricsStore.recordFailure(capabilityId)` 对所有降级场景统一标记为失败。但当 `localRuleFallback` 存在且成功返回业务结果（如处方审核的本地规则校验产出的 `AiResult.success(...)`）时，该场景是"部分降级——仍产出有效业务结果"，不应记为失败。将其计入失败率会抬高熔断器的误触发概率，导致本可正常工作的能力被过早熔断。
- **所在位置**：§4.1 降级路径伪代码
- **严重程度**：中等（MEDIUM）
- **改进建议**：区分"完全退化（无本地规则/本地规则也失败）→ recordFailure"与"降级到本地规则并成功 → recordSuccess（标记 degraded=true）"两种情况。

---

## 2. 深度与完整性不足

### 2.1 [MEDIUM] AiRequestBase 基类在设计和代码库中均未定义

- **问题描述**：§3.5 AiCallRecord 字段填充策略中写道"各能力 DTO 均继承 `AiRequestBase` 基类携带这些字段（`visitId`、`patientId`、`sessionId`）"。但 `AiRequestBase` 在 §1.3 核心抽象列表、§2.3 类图、整个文档以及现有代码库中均不存在（grep 全局确认无匹配）。实现者无法确定：
  - 该基类是 Phase 0/1ABD 已有但未引用的类型，还是本阶段需要新建的类型；
  - 如果是新建，归属哪个包（ai-api/dto/ 还是 common）；
  - 字段类型和注解策略是什么。
- **所在位置**：§3.5 AiCallRecord 字段填充策略
- **严重程度**：中等（MEDIUM）
- **改进建议**：在 §3.5 或 §1.3 中显式定义 `AiRequestBase` 基类（字段、包路径、继承关系），或在类图中补充该类型。

### 2.2 [MEDIUM] 类图中 AiService 方法名与现有接口不匹配

- **问题描述**：§2.3 类图中 AiService 接口的方法命名与 `AiService.java` 实际定义不一致：
  - `medicalRecordGen(MedicalRecordGenRequest)` → 实际 `generateMedicalRecord(MedicalRecordGenRequest)`
  - `kbQuery(KbQueryRequest)` → 实际 `knowledgeBaseQuery(KbQueryRequest)`
  若 AiOrchestrator 按类图生成的签名实现，将无法通过编译。
- **所在位置**：§2.3 类图
- **严重程度**：中等（MEDIUM）
- **改进建议**：将类图中的方法名修正为与实际代码一致。

### 2.3 [MEDIUM] LlmClient 状态归属存在表述矛盾

- **问题描述**：§3.2 将模型端点状态模型（CONNECTED / DEGRADED / UNAVAILABLE）放在 LlmClient 节下描述，暗示 LlmClient 维护此状态。但 §6.1 明确声言"LlmClient：无状态，线程安全"。如果状态由 LlmClient 维护则不可能无状态；如果状态在独立组件中维护（如 CircuitBreakerDegradationStrategy），则应明确删除 LlmClient 的状态模型或在 §3.2 中明确指出状态归属。

  此外，状态自动恢复机制描述不完整：`UNAVAILABLE` 下"每 30 秒允许一次探测调用"由谁触发、通过什么机制（定时器/请求拦截？）均未定义。
- **所在位置**：§3.2 LlmClient 状态模型 vs §6.1 线程模型
- **严重程度**：中等（MEDIUM）
- **改进建议**：明确状态模型归属组件；若保留在 LlmClient 中，需说明状态存储位置和并发控制方案；补充探测调用的触发机制设计。

### 2.4 [MEDIUM] AiCallLogEntity 遗漏字段级 JPA 映射索引覆盖度不足

- **问题描述**：§3.5 AiCallLogEntity 定义了 4 条索引（`idx_call_time`、`idx_capability_call_time`、`idx_visit_id`、`idx_patient_id`），但缺少以下高频查询维度的索引覆盖：
  - `degraded + call_time`：统计降级率按时间段查询（管理端实时/日报场景）；
  - `model_id + call_time`：模型健康度监控需要按模型维度聚合；
  - `caller_role + call_time`：按角色维度审计查询。
  同时，`call_time` 字段在 Java 中为 `LocalDateTime`，对应数据库映射为 `DATETIME`，缺失 `precision` / `columnDefinition` 定义，不同 MySQL 版本默认精度不同可能影响索引排序。
- **所在位置**：§3.5 AiCallLogEntity 表索引策略
- **严重程度**：中等（MEDIUM）
- **改进建议**：补充降级/模型/角色维度的覆盖索引评估；明确 `call_time` 的列定义（如 `DATETIME(3)`）以统一精度。

---

## 3. 整体评价

v2 产出相对于 v1 在类图、状态模型、接口方法签名、Bean 装配策略等维度改进显著，已修复第 1 轮审查识别的全部 8 项问题。但在以下方面仍存在明显的质量问题：

1. **逻辑严谨性不足**：§4.1 降级路径伪代码中指标采集代码位于 return 之后不可达，属于基本逻辑错误，直接导致降级场景下的遥测数据全部丢失。Pipeline 设计虽清晰但该错误表明整体流程的实际执行顺序未经推敲。
2. **与现有代码的一致性检查缺失**：MockAiService 的 `@ConditionalOnProperty` 属性名、DegradationStrategy 的 `getOrder()` 方法、AiService 方法命名和返回类型等多项设计声明与现有代码存在差异，设计未做对照验证。这些差异若在编码阶段才被发现，将导致额外返工。
3. **关键类型定义不全**：`AiRequestBase` 基类、`WindowedEvent` 滑动窗口事件类型未定义；`LlmClient` 状态模型归属模糊。这些缺口使实现者无法直接启动编码。
