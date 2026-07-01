# 质量审查报告 — Phase 5 包 G AI 进阶底座 OOD 设计（v22）

## 综述

该设计文档历经 22 轮迭代审议，总体成熟度较高，结构完整，抽象层级合理。需求明确要求的类图、核心职责、协作关系、关键接口、状态模型等 OOD 核心要素均已覆盖。以下聚焦**从落地实现视角**发现的剩余质量问题。

---

## 问题清单

### 问题 1：[事实错误] DelegatingLlmChatService 在 SpringAiLlmChatService Bean 不可用时的装配缺陷

- **问题描述**：§3.2 `AiPlatformConfig` 中 `springAiLlmChatService` Bean 标注了 `@ConditionalOnClass(name = "org.springframework.ai.chat.ChatModel")`，意味着当 Spring AI 不在 classpath 时该 Bean 不会被创建。然而 `delegatingLlmChatService` 的构造方法以 `@Qualifier("springAiLlmChatService") LlmChatService springAi` 形式进行**强制注入**，未使用 `@Autowired(required = false)` 或 `ObjectProvider` 兜底。当项目未引入 Spring AI 依赖时，容器将因找不到 `springAiLlmChatService` Bean 而抛出 `NoSuchBeanDefinitionException`，导致底座完全不可用。
- **所在位置**：§3.2 DelegatingLlmChatService Bean 装配伪代码（第 1156-1175 行）
- **严重程度**：严重
- **改进建议**：将 `delegatingLlmChatService` 方法签章改为 `ObjectProvider<LlmChatService> springAi`（或 `@Autowired(required = false)`），在方法体内判断 `springAi.getIfAvailable()` 是否为 null，仅当存在时才加入分发 Map。同步在 §7 设计决策表中记录此依赖可选性约束。

---

### 问题 2：[关键遗漏] ai-impl pom.xml 缺失大量 Phase 5 必需的编译期/运行期依赖声明

- **问题描述**：当前 `ai-impl/pom.xml` 仅声明了 `ai-api`、`spring-boot-starter`、`spring-boot-starter-test` 三项依赖。而设计文档全篇引用了以下依赖，均未在 POM 中体现：
  - `spring-boot-starter-data-jpa`（AiCallLogRepository、PromptTemplateRepository、ExperimentRepository 等 3 套 JPA Repository）
  - `spring-boot-starter-actuator` + Micrometer（§8 明确要求显式声明）
  - `reactor-core`（LlmChatStreamService 的 Flux 返回值，§3.2）
  - `jackson-databind`（ObjectMapper.convertValue 防御性拷贝，§3.1）
  - `caffeine`（PromptTemplateManager、ExperimentManager、ModelRouter 缓存，§3.2-3.4）
  - `com.google.guava`（EndpointRateLimiter 令牌桶，§3.2）
  - HTTP 客户端（OkHttp 或 WebClient，HttpApiLlmChatService）
  - `spring-boot-starter-web`（RequestContextHolder 依赖，§3.10）
- **所在位置**：§2.1 目录结构、§3.2/§3.3/§3.5/§3.9 等章节；实际 `ai-impl/pom.xml`
- **严重程度**：严重
- **改进建议**：在 §8 或新增专节中列出 Phase 5 底座全部显式 Maven 依赖清单，标注作用域和可选性。对于有条件的依赖（如 `reactor-core` 仅流式场景、`spring-ai` 可选）分别说明。

---

### 问题 3：[逻辑矛盾] `DegradationContext` 扩展字段与现有代码的兼容性过渡路径未冻结

- **问题描述**：§3.8 为 `DegradationContext` 规划了大量字段扩展（invocationCount, failureCount, elapsedTime, departmentId, serializedTimestamp 等）和接口增强（implements Serializable, postDeserializationValidate(), isFresh(), isInitialized()），但实际 `DegradationContext.java` 仅包含一个无参构造器（7 行空类）。更关键的是，现有 `FallbackAiService.applyStrategies()` 在第 187 行通过 `new DegradationContext()` 创建零值实例并传递给策略链。在 Phase 5 过渡期间，`applyStrategies()` 尚未被完全移除（§9.2 迁移路径），此时若框架代码已扩增字段、修改构造器、或新增 `Serializable` 要求，此调用点的零值上下文行为可能被破坏。
- **所在位置**：§3.8 DegradationContext 扩展段；`DegradationContext.java`；`FallbackAiService.java:187`
- **严重程度**：重要
- **改进建议**：在 §3.8 或 §9.2 中补充"过渡期兼容保证"段：明确 `DegradationContext` 的**无参构造器必须永久保留**，新增字段均通过 Builder 或 setter 赋值，构造器内不包含初始化逻辑。`applyStrategies()` 移除以代码冻结为前置条件，而非依赖概念冻结。

---

### 问题 4：[关键遗漏] 底座模块的 JPA 配置与多 Repository 扫描策略未定义

- **问题描述**：设计将 3 套 JPA Repository（`PromptTemplateRepository`、`ExperimentRepository`、`AiCallLogRepository`）散落在 `ai-impl` 的三个不同子包中（`template/`、`experiment/`、`metrics/`）。实现者需要知道 `@EntityScan` 和 `@EnableJpaRepositories` 的 `basePackages` 配置才能正确启动。此外，三个子包各自拥有独立的 JPA Entity 和 Repository，但底座的主配置类 `AiPlatformConfig` 并未标注 `@EnableJpaRepositories`。实现者无法确定 JPA 配置的归属位置——是放在 `AiPlatformConfig` 上，还是由外层 `@SpringBootApplication` 的统一扫描覆盖。
- **所在位置**：§2.1 目录结构、§3.9 AiPlatformConfig 定义；缺失 JPA 配置章节
- **严重程度**：重要
- **改进建议**：在 §3.9 `AiPlatformConfig` 配置段或新增 §8.2 小节中补充 JPA 配置定义：明确 `@EnableJpaRepositories(basePackages = "com.aimedical.modules.ai.impl")` 和 `@EntityScan(basePackages = "com.aimedical.modules.ai.impl")` 的应在何处声明，以及为何不放在 ai-impl 模块内（若由外层聚合工程扫描，需在文档中注明）。

---

### 问题 5：[逻辑矛盾] AiPlatformConfig 以单一 `@ConfigurationProperties(prefix = "ai")` 绑定全部配置组，违反职责分离

- **问题描述**：`AiPlatformConfig` 标注 `@ConfigurationProperties(prefix = "ai")` 意味着它需要持有 `ai.degradation`、`ai.execution`、`ai.rate-limiting`、`ai.metrics.async`、`ai.template.fallback`、`ai.platform`、`ai.router.routes`、`ai.sliding-window` 等所有配置组的嵌套字段。虽在 Java 语法上可行，但将导致 `AiPlatformConfig` 膨胀为数百行的大配置类，违反单一职责原则。Spring Boot 官方推荐以独立 `@ConfigurationProperties` 类承载各配置组（如 `AiMetricsProperties`、`DegradationProperties`、`RateLimitingProperties`），`AiPlatformConfig` 仅负责 `@EnableConfigurationProperties` 和 `@Bean` 装配。设计已为 `metricsAsyncExecutor` 引入了 `MetricsAsyncProperties` 模式但未推广到其他配置组。
- **所在位置**：§3.9 AiPlatformConfig 定义（第 1894-1977 行）
- **严重程度**：重要
- **改进建议**：将 `@ConfigurationProperties(prefix = "ai")` 拆分为多个独立配置属性类（`AiRouterProperties`、`AiExecutionProperties`、`AiDegradationProperties`、`AiRateLimitingProperties`、`AiMetricsAsyncProperties` 等），`AiPlatformConfig` 通过 `@EnableConfigurationProperties` 逐个引入。在 §3.9 中以表格列出每个配置组对应的属性类前缀和字段映射。

---

### 问题 6：[设计缺口] SlidingWindowMetricsStore 的 WindowedEvent 类型和过期清理机制未定义

- **问题描述**：§3.5 在描述线程安全模型时引用了 `WindowedEvent` 类型作为滑动窗口事件单元，但该类型的字段定义、三类事件（NORMAL_SUCCESS / DEGRADED / FAILURE）的数据结构、时间窗口边界判定逻辑（是固定时间窗口还是滑动时间窗口）、过期事件如何从 `Deque` 中移除等关键实现细节均未定义。"通过快照方式（先复制当前窗口的快照数组再计算，读写分离）"的描述暗示了全量复制读策略，但未说明复制定时器频率或写时复制触发条件——若以高频调用下每次都复制全窗口数组，内存和 GC 开销不可忽略。此组件是 `CircuitBreakerDegradationStrategy` 和 `TimeoutDegradationStrategy` 的底层数据源，其内部实现的不确定性将传导至上层策略的行为不可预测。
- **所在位置**：§3.5 SlidingWindowMetricsStore 线程安全段（第 1747-1748 行）
- **严重程度**：重要
- **改进建议**：在 §3.5 中补充：(1) `WindowedEvent` 值对象的字段定义（type、timestamp、elapsedMs）；(2) 滑动窗口边界判定规则（基于 `System.currentTimeMillis() - windowStart > windowSeconds` 惰性淘汰）；(3) 快照策略的触发条件（每次 `getFailureRate()` 调用时惰性复制，而非每次 record 调用）；(4) 与 §9.5 YAML 配置 `sliding-window.window-seconds` 和 `max-events-per-capability` 的映射关系，明确达到 `max-events-per-capability` 上限时是丢弃最旧事件还是拒绝新记录。

---

### 问题 7：[关键遗漏] Event 驱动刷新机制假设管理端与底座同进程部署，未说明分布式场景适配方案

- **问题描述**：§3.2 `RouteConfigChangedEvent`、§3.3 `TemplateChangedEvent`、§3.4 `ExperimentChangedEvent` 三个缓存失效事件均基于 Spring `ApplicationEvent` 机制，该机制是**进程内事件总线**，无法跨 JVM 边界传播。设计在 §2.2 中明确假设"单体架构或同一 Spring Boot 容器"，因此此问题在当前部署模式约束下不直接构成事实错误。但问题在于：文档**未将此约束记录为设计决策，也未提供分布式部署场景的兜底或迁移方案**。若后续需要将管理端（包 A）与底座（包 G）分离部署（如管理端独立为管理后台服务），三套事件驱动缓存失效机制将全部失效，底座只能依赖 Caffeine expireAfterWrite 5 分钟过期兜底，导致 5 分钟的不一致窗口——这对 A/B 实验即时启停和 Prompt 快速回滚场景不可接受。
- **所在位置**：§3.2 路由配置热加载段、§3.3 TemplateChangedEvent 重建策略、§3.4 ExperimentChangedEvent 重建策略、§10 协作边界
- **严重程度**：重要
- **改进建议**：在 §7 设计决策表中新增行记录此约束（部署形态：单体，事件机制：进程内 ApplicationEvent），并在 §10 协作边界中新增"分布式部署兜底"段：明确说明若未来管理端与底座分离，三套事件的替代方案为共享缓存（Redis Pub/Sub 或数据库轮询）以维持 < 1 秒的不一致窗口。

---

### 问题 8：[设计缺口] CredentialProvider CACHE_ONLY 状态下的 TTL 延长机制与 Caffeine expireAfterWrite 的兼容性未说明

- **问题描述**：§3.2 Vault 降级状态模型中定义 "CACHE_ONLY: Vault 连续不可达阈值触发，仅使用缓存数据；缓存中旧凭据的 TTL 延长 30 秒"。Caffeine 的 `expireAfterWrite` 是在写入缓存条目时设置的固定 TTL，无法以条目维度**动态延长**。要实现"延长 TTL"的效果，实现者需使用 Caffeine 的 `Expiry` 接口（在读取时动态计算过期时间），或重新 put 条目来刷新 TTL。文档未说明具体实现路径，也未评估两种方案的优劣（Expiry 接口的读取时计算增加读路径延迟；重新 put 在并发场景下有竞争条件）。
- **所在位置**：§3.2 Vault 降级行为 — 状态模型（第 1417-1446 行）
- **严重程度**：一般
- **改进建议**：在 §3.2 凭据缓存策略段中补充 Caffeine 实现方案的具体说明——推荐使用 `Caffeine.newBuilder().expireAfter(new Expiry<K, V>() { ... })` 在 `expireAfterRead()` 中根据当前 Vault 状态动态返回过期时间，或显式说明采用"重新 put 同一 key 以刷新 TTL"方案。

---

### 问题 9：[设计缺口] 混合完整管线与薄适配器的「超时层级」存在歧义

- **问题描述**：底座能力完整管线的超时由 `AbstractCapabilityExecutor.execute()` 的 `CompletableFuture.orTimeout(capabilityTimeout)` 兜底，薄适配器管线的超时由 `doExecuteInternal()` 内部的 `delegateFuture.get(thinAdapterTimeout, ...)` 控制。但 §9.5 YAML 中为薄适配器能力（DIAGNOSIS 等）配置了 `per-capability` 超时值（30s）**和** `thin-adapter-default`（30s）两层配置。问题在于：当薄适配器管线的 `execute()` 外层 `orTimeout` 和内部 `delegateFuture.get()` 同时存在时，两个超时值的关系未定义——是外层的 `capabilityTimeout`（per-capability 中 30s）先触发还是内部 `thinAdapterTimeout`（也是 30s）先触发？若两者相等，在 `delegateFuture.get()` 超时瞬间抛出的 `TimeoutException` 被内部 catch 捕获处理为降级，同时外层 `orTimeout` 是否还会因 Future 未完成而再次触发超时降级，导致同一请求被降级两次？
- **所在位置**：§3.1 AbstractCapabilityExecutor.execute() 模板方法 orTimeout 段 vs §4.2 薄适配器 doExecuteInternal() 的超时控制；§9.5 YAML 配置
- **严重程度**：重要
- **改进建议**：在 §3.1 超时机制段中明确"薄适配器场景 `capabilityTimeout`（外层 orTimeout）应设置为 `thinAdapterTimeout` + 合理缓冲值（如 +5s），确保内部 `delegateFuture.get()` 超时优先触发并进入已定义的降级路径，外层 `orTimeout` 仅作为兜底保护（捕获内部超时处理之外的非预期挂起）"。在 §9.5 YAML 中为薄适配器能力的 `per-capability` 超时值添加注释说明此约束（如 `DIAGNOSIS: 35s # 薄适配器：thinAdapterTimeout 30s + 5s 缓冲`）。

---

### 问题 10：[设计缺口] `idx_prompt_version` 独立索引的查询覆盖度不足

- **问题描述**：§3.5 为 `ai_call_log` 表定义了 `idx_prompt_version` 索引（`(prompt_version)`）。A/B 实验效果分析的实际查询模式是按 `(capability_id, prompt_version, call_time)` 或 `(experiment_id, prompt_version)` 做范围查询和聚合。仅对 `prompt_version` 建独立索引意味着对任意查询，数据库仍需回表过滤能力标识和时间范围，索引效率极低。特别当表数据量达到百万级时，`(prompt_version)` 的选择性很差（Integer 类型的版本号通常只有几十到几百个不同值），MySQL 优化器可能直接选择全表扫描。
- **所在位置**：§3.5 AiCallLogEntity 表索引策略（第 1706 行）
- **严重程度**：一般
- **改进建议**：将 `idx_prompt_version` 替换或补充为复合索引 `(prompt_version, call_time DESC)`，并在 §3.5 中补充一条覆盖能力维度的复合索引 `(capability_id, prompt_version, call_time DESC)`，直接支撑"按能力+版本+时间范围"的 A/B 实验分析查询。

---

## 整体质量评价

设计文档在需求响应充分度方面表现良好，类图、状态模型、行为契约、迁移路径等需求要素均已覆盖。历经 22 轮迭代后设计质量的收敛度较高。当前剩余质量问题主要集中在以下三个方向：

1. **构建/部署上下文缺失**（问题 2、4、5）：底座模块的依赖清单和 JPA 配置是落地时必须解决的先决条件，目前处于**有概念无契约**状态。
2. **组件间动态装配边界模糊**（问题 1、3、9）：对于可选组件（Spring AI）、过渡期组件（DegradationContext）、嵌套超时组件（薄适配器）的运行时行为，设计存在未冻结的灰色地带。
3. **物理架构假设未显式约束**（问题 7）：三个事件驱动机制隐含单体部署假设，设计缺少约束记录和分布式兜底方案。

建议修复者在迭代 v23 时优先处理构建配置和 JPA 配置两项（问题 2、4），其次为 DelegatingLlmChatService 装配补充可选性（问题 1）和超时层级冻结（问题 9），其余问题可归入后续优化。
