根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 问题 1：[事实错误] DelegatingLlmChatService 在 SpringAiLlmChatService Bean 不可用时的装配缺陷
`springAiLlmChatService` Bean 标注了 `@ConditionalOnClass(name = "org.springframework.ai.chat.ChatModel")`，但 `delegatingLlmChatService` 构造方法以 `@Qualifier("springAiLlmChatService") LlmChatService springAi` 进行强制注入。当项目未引入 Spring AI 依赖时，容器将抛出 `NoSuchBeanDefinitionException`，导致底座完全不可用。
- 位置：§3.2 DelegatingLlmChatService Bean 装配伪代码
- 严重程度：严重
- 建议：将注入改为 `ObjectProvider<LlmChatService>` 或 `@Autowired(required = false)`，方法体内判断 `getIfAvailable()` 是否为 null 后加入分发 Map；同步在 §7 设计决策表中记录此依赖可选性约束。

### 问题 2：[关键遗漏] ai-impl pom.xml 缺失大量 Phase 5 必需的编译期/运行期依赖声明
当前 `pom.xml` 仅声明了 `ai-api`、`spring-boot-starter`、`spring-boot-starter-test` 三项依赖，而设计文档全篇引用了以下依赖均未体现：`spring-boot-starter-data-jpa`、`spring-boot-starter-actuator` + Micrometer、`reactor-core`、`jackson-databind`、`caffeine`、`com.google.guava`、OkHttp/WebClient、`spring-boot-starter-web`。
- 位置：§2.1 目录结构、§3.2/§3.3/§3.5/§3.9 等章节
- 严重程度：严重
- 建议：在 §8 或新增专节中列出 Phase 5 底座全部显式 Maven 依赖清单，标注作用域和可选性；有条件的依赖（如 `reactor-core` 仅流式场景、`spring-ai` 可选）分别说明。

### 问题 3：[逻辑矛盾] DegradationContext 扩展字段与现有代码的兼容性过渡路径未冻结
设计为 `DegradationContext` 规划了大量字段扩展（invocationCount, failureCount, elapsedTime, departmentId, serializedTimestamp 等）和接口增强（implements Serializable, postDeserializationValidate(), isFresh(), isInitialized()），但现有 `FallbackAiService.applyStrategies()` 在第 187 行通过 `new DegradationContext()` 创建零值实例。在 Phase 5 过渡期间若框架代码已扩增字段、修改构造器、新增 `Serializable` 要求，此调用点的零值上下文行为可能被破坏。
- 位置：§3.8 DegradationContext 扩展段、DegradationContext.java、FallbackAiService.java:187
- 严重程度：重要
- 建议：在 §3.8 或 §9.2 中补充"过渡期兼容保证"段——明确 `DegradationContext` 的无参构造器必须永久保留，新增字段均通过 Builder 或 setter 赋值，构造器内不包含初始化逻辑；`applyStrategies()` 移除以代码冻结为前置条件，而非依赖概念冻结。

### 问题 4：[关键遗漏] 底座模块的 JPA 配置与多 Repository 扫描策略未定义
3 套 JPA Repository 散落在 `ai-impl` 的三个不同子包中（`template/`、`experiment/`、`metrics/`），实现者需要知道 `@EntityScan` 和 `@EnableJpaRepositories` 的 `basePackages` 配置才能正确启动。底座主配置类 `AiPlatformConfig` 并未标注 `@EnableJpaRepositories`。
- 位置：§2.1 目录结构、§3.9 AiPlatformConfig 定义
- 严重程度：重要
- 建议：在 §3.9 `AiPlatformConfig` 或新增 §8.2 小节中补充 JPA 配置定义——明确 `@EnableJpaRepositories(basePackages = "com.aimedical.modules.ai.impl")` 和 `@EntityScan(basePackages = "com.aimedical.modules.ai.impl")` 的声明位置及理由。

### 问题 5：[逻辑矛盾] AiPlatformConfig 以单一 @ConfigurationProperties 绑定全部配置组，违反职责分离
`AiPlatformConfig` 标注 `@ConfigurationProperties(prefix = "ai")` 需要持有 `ai.degradation`、`ai.execution`、`ai.rate-limiting`、`ai.metrics.async`、`ai.template.fallback`、`ai.platform`、`ai.router.routes`、`ai.sliding-window` 等所有配置组的嵌套字段，将导致数百行的大配置类，违反单一职责原则。
- 位置：§3.9 AiPlatformConfig 定义
- 严重程度：重要
- 建议：将 `@ConfigurationProperties(prefix = "ai")` 拆分为多个独立配置属性类（`AiRouterProperties`、`AiExecutionProperties`、`AiDegradationProperties`、`AiRateLimitingProperties`、`AiMetricsAsyncProperties` 等），`AiPlatformConfig` 通过 `@EnableConfigurationProperties` 逐个引入；在 §3.9 中以表格列出每个配置组对应的属性类前缀和字段映射。

### 问题 6：[设计缺口] SlidingWindowMetricsStore 的 WindowedEvent 类型和过期清理机制未定义
§3.5 引用 `WindowedEvent` 类型作为滑动窗口事件单元，但(1)该类型的字段定义未给出，(2)三类事件的数据结构未定义，(3)时间窗口边界判定逻辑未说明，(4)过期事件从 Deque 中移除的机制未定义。"通过快照方式..."的描述暗示全量复制读策略，但未说明复制定时器频率或写时复制触发条件。
- 位置：§3.5 SlidingWindowMetricsStore 线程安全段
- 严重程度：重要
- 建议：在 §3.5 中补充：(1) `WindowedEvent` 值对象的字段定义（type、timestamp、elapsedMs）；(2) 滑动窗口边界判定规则（基于 `System.currentTimeMillis() - windowStart > windowSeconds` 惰性淘汰）；(3) 快照策略的触发条件（每次 `getFailureRate()` 调用时惰性复制，而非每次 record 调用）；(4) 与 §9.5 YAML 配置的映射关系，明确达到 `max-events-per-capability` 上限时是丢弃最旧事件还是拒绝新记录。

### 问题 7：[关键遗漏] Event 驱动刷新机制假设管理端与底座同进程部署，未说明分布式场景适配方案
§3.2 `RouteConfigChangedEvent`、§3.3 `TemplateChangedEvent`、§3.4 `ExperimentChangedEvent` 三个缓存失效事件均基于 Spring `ApplicationEvent`（进程内事件总线），无法跨 JVM 传播。文档未将此约束记录为设计决策，也未提供分布式部署场景的兜底或迁移方案。
- 位置：§3.2 路由配置热加载段、§3.3/§3.4 事件重建策略、§10 协作边界
- 严重程度：重要
- 建议：在 §7 设计决策表中新增行记录此约束（部署形态：单体，事件机制：进程内 ApplicationEvent），并在 §10 协作边界中新增"分布式部署兜底"段——说明若未来管理端与底座分离，三套事件的替代方案为共享缓存（Redis Pub/Sub 或数据库轮询）。

### 问题 8：[设计缺口] CredentialProvider CACHE_ONLY 状态下的 TTL 延长机制与 Caffeine expireAfterWrite 的兼容性未说明
§3.2 Vault 降级状态模型中定义"CACHE_ONLY：缓存中旧凭据的 TTL 延长 30 秒"。Caffeine 的 `expireAfterWrite` 是固定 TTL，无法动态延长。实现者需使用 Caffeine `Expiry` 接口或重新 put 条目刷新 TTL，文档未说明具体实现路径。
- 位置：§3.2 Vault 降级行为 — 状态模型
- 严重程度：一般
- 建议：在 §3.2 凭据缓存策略段中补充 Caffeine 实现方案——推荐使用 `Caffeine.newBuilder().expireAfter(new Expiry<K, V>() { ... })` 在 `expireAfterRead()` 中动态返回过期时间，或显式说明采用"重新 put 同一 key 以刷新 TTL"方案。

### 问题 9：[设计缺口] 混合完整管线与薄适配器的「超时层级」存在歧义
底座完整管线超时由 `orTimeout(capabilityTimeout)` 兜底，薄适配器管线超时由 `delegateFuture.get(thinAdapterTimeout, ...)` 控制。§9.5 YAML 中两者均配置 30s，但两超时值的关系未定义——外层 `orTimeout` 与内部 `get()` 可能同时触发，导致同一请求被降级两次。
- 位置：§3.1 AbstractCapabilityExecutor.execute() vs §4.2 薄适配器 doExecuteInternal()；§9.5 YAML
- 严重程度：重要
- 建议：在 §3.1 超时机制段中明确"薄适配器场景 `capabilityTimeout` 应设置为 `thinAdapterTimeout` + 合理缓冲值（如 +5s），确保内部超时优先触发并进入已定义的降级路径，外层 `orTimeout` 仅作为兜底保护"；在 §9.5 YAML 中为薄适配器能力的 `per-capability` 超时值添加注释说明此约束。

### 问题 10：[设计缺口] idx_prompt_version 独立索引的查询覆盖度不足
`ai_call_log` 表当前索引 `idx_prompt_version(prompt_version)` 按 `prompt_version` 单列建索引，但 A/B 实验分析的实际查询模式是 `(capability_id, prompt_version, call_time)` 或 `(experiment_id, prompt_version)` 范围查询。`prompt_version` 选择性差（Integer 类型通常仅几十到几百个不同值），MySQL 优化器可能选择全表扫描。
- 位置：§3.5 AiCallLogEntity 表索引策略
- 严重程度：一般
- 建议：将 `idx_prompt_version` 替换或补充为复合索引 `(prompt_version, call_time DESC)`，并补充一条覆盖能力维度的复合索引 `(capability_id, prompt_version, call_time DESC)`。

## 历史迭代回顾

### 已解决的问题（出现在历史反馈但当前反馈中不再提及）
- 缺失 UML 类图（迭代 1）：已补充完整类图覆盖全部核心抽象及接口关系。
- CapabilityExecutor 接口缺少方法签名（迭代 1）：已补充完整方法签名定义。
- AiOrchestrator 与 FallbackAiService 的 Bean 装配二义性（迭代 1）：已通过 `@Primary` + `ObjectProvider` 解决。
- Bean 装配中的初始化时序风险（迭代 5、8）：已通过 `@PostConstruct` + `ApplicationContextAware` 解决。
- 异步上下文传播丢失（迭代 8）：已通过 execute() 入口处提取闭包参数的策略解决。
- `doDegrade()` 方法签名缺少参数（迭代 8、11、12、13）：已完整覆盖 departmentId、callerRole、callerId、visitId、patientId、sessionId、inputSummary、outputSummary、promptVersion。
- 伪代码中变量作用域/未定义变量错误（迭代 2、3、9、11、14、17、18）：已逐轮修正。
- 薄适配器 Maven 依赖作用域矛盾（迭代 12、14、19）：已统一为 `provided` 并补充运行时风险评估。
- AiPlatformConfig 生命周期冲突（双实例）（迭代 18）：已将 EnvironmentPostProcessor 剥离为独立类 `AiPlatformEnvironmentPostProcessor`。
- `LlmChatService` 多实现 Bean 装配二义性（迭代 20）：已通过 `DelegatingLlmChatService` 分发层解决。
- `structuredChat()` 异常类型缺失（迭代 21）：已补充 `StructuredOutputNotSupportedException` 和 `LlmInfrastructureException`。
- 薄适配器默认超时值矛盾（迭代 21）：已统一为 30s。

### 持续存在的问题（在多轮反馈中反复出现，需重点解决）
- **DegradationContext 兼容性**（迭代 1、4、14，当前问题 3）：`DegradationContext` 无参构造器、序列化兼容性、扩展字段与现有 `applyStrategies()` 的过渡期冲突持续多轮未从根本上解决。需在本轮补充明确的过渡期兼容保证段和代码冻结前置条件。
- **构建/部署上下文缺失**（迭代 12、14、19，当前问题 2）：依赖清单、作用域等构建配置信息持续不完整。从最初的 Maven 作用域争议发展到本轮缺少完整依赖声明，需在 §8 中系统化补充。

### 新发现的问题（本轮新识别）
- 问题 1：DelegatingLlmChatService 的 `@Qualifier` 强制注入未处理 Spring AI 可选依赖场景。
- 问题 4：底座模块 JPA 多 Repository 扫描策略和 `@EnableJpaRepositories` 归属未定义。
- 问题 5：`@ConfigurationProperties(prefix = "ai")` 单一绑定导致 AiPlatformConfig 违反单一职责。
- 问题 6：SlidingWindowMetricsStore 的 WindowedEvent 类型、过期清理机制、快照触发条件等实现细节未定义。
- 问题 7：三套事件驱动刷新机制隐含单体部署假设，缺少约束记录和分布式兜底方案。
- 问题 8：CredentialProvider CACHE_ONLY 状态下 Caffeine TTL 延长机制实现路径未说明。
- 问题 9：完整管线 orTimeout 与薄适配器 delegateFuture.get() 的超时层级关系未冻结。
- 问题 10：idx_prompt_version 单列索引不满足 A/B 实验分析查询模式，需补充复合索引。

## 上一轮产出路径
C:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606271627_phase5_pkgG_ood/a_v22_copy_from_v21.md

## 用户需求
C:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606271627_phase5_pkgG_ood/requirement.md
