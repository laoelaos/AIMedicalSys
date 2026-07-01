# 质量审查报告 — v14

**审查范围**：Phase 5 包 G AI 进阶底座 OOD 设计方案 (v14)
**审查视角**：需求响应充分度、事实/逻辑正确性、深度与完整性（侧重内部审议未充分覆盖的维度）
**当前迭代**：第 14 次

---

## 1. 需求响应充分度

需求要求：完成 Phase 5 包 G 的完整 OOD 设计，覆盖类图、核心职责、协作关系、关键接口、状态模型等核心要素。产出整体上充分回应了需求，但以下方面可进一步补充：

### 1.1 非功能性维度的系统性分析缺失

**问题描述**：设计文档全面覆盖了功能架构（类图、接口契约、行为伪代码、状态模型等），但缺乏对非功能质量属性的系统性分析章节。具体而言：多级 Caffeine 缓存（模板/实验/凭据/路由）× 滑动窗口 × 多线程池 × 新 JPA Repository 的系统启动冷启动效应、内存占用估算、数据库连接池增量压力、启动时间影响等均未评估。这些非功能维度直接影响部署准备和容量规划，但分散在各节中的碎片化描述不足以形成系统性认知。

**所在位置**：全文（无集中分析章节）

**严重程度**：重要（影响部署准备和容量规划）

**改进建议**：建议新增一个"非功能性质量分析"章节（或扩展 §6/§10），集中分析：(a) 冷启动时多级缓存同时 Miss 的惊群效应及预热建议；(b) 新增 3 个 JPA Repository 对数据库连接池的压力估算；(c) 每个能力标识的滑动窗口在 13 项能力满载下的内存占用估算；(d) `@PostConstruct` 扫描和策略 Map 构建对启动时间的影响。

---

## 2. 事实错误与逻辑矛盾

### 2.1 [Major] DEGRADED 状态下错误记录失败指标导致双重计数

**问题描述**：`§4.1` 行 2914-2920 的伪代码中，当 `endpointState == DEGRADED` 时，先通过 `metricsCollector.record(AiCallRecord.failure(...))` 记录一次失败调用（错误码 "ENDPOINT_DEGRADED"），然后继续执行 LLM 调用。若 LLM 调用成功，后续行 3100-3105 会再次通过 `metricsCollector.record(AiCallRecord.success(...))` 记录一次成功调用。结果是**同一次调用被同时记录为失败和成功**，双重计数既膨胀了失败率指标也膨胀了调用总量，导致监控大盘上降级率、失败率、总调用数三个核心指标同时失真。

此问题的根因是对 `DEGRADED` 状态的语义理解偏差——DEGRADED 仅表示端点性能退化（耗时偏高），**不等于本次调用失败**，不应为尚不知道结果的调用预先记录失败。

**所在位置**：§4.1 `doExecuteInternal()` 伪代码，行 2914-2920

**严重程度**：严重

**改进建议**：方案 A（推荐）——`DEGRADED` 状态下仅执行日志 WARN + 推送到告警/健康检查体系，**不记录 `AiCallRecord.failure()`**，等待 LLM 调用完成后按实际结果（成功/失败/超时）记录一次。方案 B —— 若需要独立统计"退化态下的尝试调用"，新增专门的 `AiCallRecord` 工厂方法（如 `AiCallRecord.degradedAttempt()`）与常规成功/失败分开记录，避免与标准调用计数混叠。

### 2.2 [Important] ClientType 配置错误静默回退风险

**问题描述**：`§1.3` 行 55 描述 YAML 中 `client` 枚举值映射失败时，`DelegatingLlmChatService` 回退到 `HttpApiLlmChatService` 默认实现并仅日志 WARN。这意味着：若运维或开发人员在配置中将 `client: "HTTP_API"` 误写为 `client: "http-api"`（大小写不匹配 Spring Boot Relaxed Binding）或其他拼写错误，所有对该端点的模型调用将**静默**使用 HTTP 客户端而非预期的 Spring AI 客户端。此行为在生产环境中极难被快速发现（日志 WARN 在大量正常日志中容易被淹没），且路由语义发生变化不产生任何告警或启动期失败。

**所在位置**：§1.3 `ClientType` 描述段，行 55；§3.2 `DelegatingLlmChatService` 分发机制，行 1396-1397

**严重程度**：重要

**改进建议**：三种方案推荐至少实施一种：(a) 在 `ClientType` setter 或 `@Converter` 中强制实施防御性转换（`value.toUpperCase().replace("-", "_")`），确保大小写不敏感的常见写法也能正确映射；(b) 转换失败时在启动期 fail-fast（抛出 `IllegalStateException`），而非运行期静默回退；(c) 若保留回退行为，至少将 WARN 提升为 ERROR，并添加**告警通知**（如通过 `ApplicationEventPublisher` 发出 `ClientTypeMappingFailedEvent` 供监控系统捕获）。

---

## 3. 深度与完整性不足

### 3.1 [Important] DiscussionConclusionCapabilityExecutor 线程隔离方案未定稿

**问题描述**：`§3.11.7` 行 2751 的线程模型风险分析段落明确指出前置 LLM 压缩调用存在 `llmCallExecutor` 线程池饥饿风险，并"建议引入独立线程池 `transcriptSummaryExecutor`"，但**未提供该线程池的具体设计**（核心线程数、最大线程数、队列容量、拒绝策略、Bean 注册位置等）。实现者将面临未定义的设计缺口——要么自行决定线程池参数（违背 OOD 定稿意图），要么沿用文档的"建议"而使用默认线程池引入风险。该线程池的实现决策（是独立 `@Bean` 异步线程池还是 `ScheduledExecutorService` 异步重试）在不同选择下对整个 `DiscussionConclusionCapabilityExecutor` 的执行语义有根本影响。

**所在位置**：§3.11.7 特化设计表"模板变量提取策略"风险分析段落，行 2751

**严重程度**：重要

**改进建议**：在 `§3.9 AiPlatformConfig` 中补充 `transcriptSummaryExecutor` 的完整 `@Bean` 定义（建议核心线程数=2、队列容量=10、`CallerRunsPolicy`），并在 `§3.11.7` 的行 2751 处将"建议"改为**具体设计决策**：明确指定线程池类型、参数和注入方式，或选择异步重试机制并给出具体实现方案。YAML 配置 `transcript-summary: 15s` 的注释（`§9.5` 行 3721）中需同步更新，说明其超时控制仅作用于单次压缩调用，线程池隔离由 `transcriptSummaryExecutor` 独立保障。

### 3.2 [Moderate] 7 项底座能力 DTO 改造的工程量未量化

**问题描述**：`§3.5` 行 2036-2064 描述了 AiRequestBase 继承改造和 DTO 业务字段补齐计划，但未将 7 项底座能力 DTO 的改造工作量量化。每项 DTO（`TriageRequest`、`PrescriptionCheckRequest`、`MedicalRecordGenRequest`、`PrescriptionAssistRequest`、`KbQueryRequest`、`ScheduleRequest`、`DiscussionConclusionRequest`）需完成：extends 变更、4 个 AiRequestBase 继承字段、3~6 个业务扩展字段（见 `§3.11`）、全参构造器 + Jackson 注解、`toString()` 敏感字段排除、3 个兼容性测试场景。7 项 DTO 累计约 30~40 个新字段，但文档中没有给出各 DTO 逐一的工作量对比，实现者在排期和分工时缺乏直观参考。

**所在位置**：§3.5「现有 DTO 影响评估与向后兼容策略」段，行 2036-2064；§3.11 共同约束，行 2757-2762

**严重程度**：中等

**改进建议**：在 §3.11 共同约束或 §3.5 过渡策略中新增一个**DT改造工作量概览表**，逐 DTO 列出：新字段数、必须新增的 Jackson 注解、是否需更新现有调用方、预计代码行数。此表可帮助项目经理和实现者快速评估分工和排期。

### 3.3 [Moderate] 多级缓存冷启动效应未分析

**问题描述**：设计引入了 Caffeine 缓存层用于 PromptTemplate、Experiment、Credential、ModelRouter 路由配置等至少 4 个组件，且使用了惰性加载（首次访问时从数据库加载）。在系统冷启动或缓存全部过期时，首次 AI 调用会触发级联的 DB 查询链路——实验配置查询 + 模板查询 + 凭据查询 + 路由查询 + 指标写入准备，全在同一端到端超时窗口内完成。若数据库响应延迟偏高（如启动期连接池预热中），首次调用的 P99 延迟将远高于稳态值。文档未分析此冷启动效应，也未给出预热建议。

**所在位置**：全文（§3.2 CredentialProvider 缓存、§3.3 DatabasePromptTemplateManager、§3.4 HashBucketExperimentManager、§3.2 DefaultModelRouter）

**严重程度**：中等

**改进建议**：在 §3.9 或新增 §6 并发设计章节中补充冷启动分析，包括：(a) 首轮调用的级联 DB 查询耗时估算；(b) 建议在 `AiPlatformConfig` 的 `@PostConstruct` 中为关键缓存（路由配置、凭据）执行一次预热加载（如 `defaultModelRouter.route("TRIAGE", null)`）；(c) 记录冷启动延迟可接受程度，若需保证首调 P99，应补充预热机制设计。

### 3.4 [Minor] structuredOutputParser.parse() 超时硬编码

**问题描述**：`§4.1` 行 3038-3045 中 `structuredOutputParser.parse()` 的超时使用硬编码 5 秒（`.get(5, TimeUnit.SECONDS)`），未通过配置项暴露。若某能力的 LLM 输出 JSON 较长（如病历生成的完整病历 JSON 可达数千 Token），或 JSON 解析逻辑复杂（含自定义反序列化器），5 秒可能不足。且所有能力共享同一硬编码超时，无法按能力差异化。

**所在位置**：§4.1 行 3038-3045

**严重程度**：轻微

**改进建议**：将此超时抽取为可配置项，如 `@Value("${ai.execution.timeout.parse:5s}") Duration parseTimeout`，在 `AbstractCapabilityExecutor` 构造器或 `doExecuteInternal()` 中注入使用，与 `capabilityTimeoutConfig` 的设计风格保持一致。

---

## 4. 整体评价

该设计文档经过 14 轮迭代，在功能架构层面已高度成熟，类图、接口契约、状态模型、行为伪代码等核心要素均完整覆盖，可直接用于指导编码实现。主要质量问题集中在**(a) 运维侧行为准确性**（DEGRADED 状态的双重计数、ClientType 静默回退）和**(b) 非功能维度的设计完备性**（线程隔离方案未定稿、冷启动效应未评估、DTO 工作量未量化）。建议在进入编码阶段前优先修复 2.1 和 2.2 两项关键问题，3.1~3.4 可在首批编码批次中同步补齐。
