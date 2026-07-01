# OOD 设计方案审查报告（v4）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 设计中所有类型形态选择（interface / abstract class / class / enum / sealed class / JPA @Entity）与 Java 类型系统能力完全匹配。继承体系遵循 Java 单类继承 + 多接口实现约束：`AbstractCapabilityExecutor` 作为唯一抽象基类，13 个子类 extends 继承；`AiService`、`CapabilityExecutor`、`LlmChatService` 等接口各司其职。泛型使用（`CapabilityExecutor<T,R>`、`StructuredChatResult<T>`、`LocalRuleFallback<T,R>`）均在 Java 泛型系统能力范围内，且明确了 `Class<T>` 运行时类型信息的获取与使用方式。协作关系中描述的类型交互模式（`Map<String, CapabilityExecutor>` 按能力路由、`DelegatingLlmChatService` 按 `ClientType` 分发）均可直接实现。

**[通过]** `DegradationStrategy.getOrder()` 以 Java 8 `default method` 形式扩展，保持二进制兼容。

**[通过]** `AiRequestBase` 抽象基类的引入方案（`extends` + `@JsonIgnoreProperties(ignoreUnknown = true)`）与 Jackson 兼容性分析完整，3 个序列化/反序列化验证场景覆盖了向前向后兼容性风险。

### 2. 标准库与生态覆盖

**[通过]** 设计中依赖的标准库和常用库（Spring Boot DI / Spring Data JPA / Jackson / Caffeine / Guava / Micrometer / Reactor / Spring AI 可选）均为 Java 生态成熟组件，假设合理。`DelegatingLlmChatService` 通过 `ObjectProvider<T> + getIfAvailable()` 处理 Spring AI 可选依赖，规避 `NoSuchBeanDefinitionException`。

**[通过]** 凭据管理采用 `CredentialProvider` 从 Vault/配置中心按 endpointId 查询，Caffeine 缓存 + `Expiry` 接口实现动态 TTL 延长，方案可行。

**[轻微]** §3.5 聚合 SQL 中 `PERCENTILE_CONT()` 为 PostgreSQL/Oracle 语法，MySQL 8.0 原生不支持此函数，需在实现层替换为手动百分位计算（如 `ROW_NUMBER() + COUNT(*)` 方案）。此属实现细节，不影响架构设计可行性。

### 3. 语言特性可行性

**[通过]** 错误处理策略与 Java 异常体系匹配：`StructuredOutputNotSupportedException` / `LlmInfrastructureException` 区分两类异常路径，`CompletableFuture.orTimeout()` 处理超时，`DegradationReason` 枚举集中管理降级原因。

**[通过]** 并发设计完整：`CompletableFuture.supplyAsync()` 异步边界、`ThreadPoolExecutor`+`CallerRunsPolicy` 提供自然背压、`@Async` 隔离指标采集线程池、`synchronized` 保护滑动窗口 Deque 操作、`AtomicReference`+CAS 管理熔断器状态、`ConcurrentHashMap` 支撑缓存和映射表。异步上下文传播采用"入口处提取 + 闭包捕获"模式，方案正确。

**[通过]** 资源管理方案可行：连接池（HTTP / JPA）、Caffeine 缓存 TTL + 事件驱动清除、按月分区 + 180 天 `DROP PARTITION` 清理。

**[通过]** 模块/包结构遵循标准 Maven 多模块 + Spring Boot 目录规范，依赖方向清晰单向。

### 4. 设计一致性

**[通过]** 13 个 `CapabilityExecutor` 实现职责描述清晰无歧义（7 项完整管线 + 6 项薄适配器）。协作关系形成闭环：`AiOrchestrator` 路由 → `CapabilityExecutor` 执行管线 → `ModelRouter` / `LlmChatService` / `PromptTemplateManager` / `AiMetricsCollector` 等子组件协作，无缺失环节。

**[通过]** §4.1~§4.7 行为契约伪代码完整，覆盖正常路径、降级预检、超时兜底、异常降级等场景。各步骤 try-catch 边界定义明确（`experimentManager.assign()` 异常→default 分组、`promptTemplateManager.render()` 异常→兜底 Prompt）。

**[通过]** 模块间依赖方向合理，`ai-api ← ai-impl` 单向依赖，`ai-impl` 内部子包由 `orchestrator` 统一编排。`thin-adapter/` 出向依赖 Phase 4 业务模块使用 `provided` 作用域，治理规则明确。

**[通过]** 第 4 轮审查迭代中全部 12 个问题（问题 1~12）均已在 §3.11 覆盖范围、`@Qualifier` 清理、Map 方案统一、条件注册机制、薄适配器 departmentId 回退、`ExperimentGroup` 定义、`AiCallLogStats` 定义、`PrescriptionLocalRuleFallback` 行为契约、`LlmChatOptions` 扩展字段、`DiscussionConclusion` 前置 LLM 调用契约、`extractCallerRole()` 过滤规则等方面得到系统修正。

### 5. 设计质量

**[通过]** 职责划分遵循 SRP：`AiOrchestrator` 专注路由委托、`AbstractCapabilityExecutor` 封装公共模板方法、各 `CapabilityExecutor` 特化差异化步骤、`DelegatingLlmChatService` 按 `ClientType` 分发、`AiPlatformConfig` 集中管理 Bean 装配。

**[通过]** 抽象层次恰当：未过度设计（如 Pipeline Step 链分析结论"留待 Phase 6"合理），也未设计不足（降级预检、超时兜底、健康检查、实验分流等关键路径均有覆盖）。

**[通过]** §11 测试策略系统完整：单元测试（`@MockBean` 模拟各降级路径）、集成测试（端到端管线 + Bean 装配互斥）、状态恢复路径验证（5 组状态机）、并发竞争验证（5 个竞争场景），设计具有良好的可测试性。

**[通过]** 前瞻性考量（§1.5 多实例行为约束、§10.4 分布式兜底方案、§9.2 FallbackAiService 过渡迁移路径）以设计决策形式记录，未过度工程化。

## 修改要求（REJECTED 时存在）

N/A — 审查通过，无严重或一般问题。
