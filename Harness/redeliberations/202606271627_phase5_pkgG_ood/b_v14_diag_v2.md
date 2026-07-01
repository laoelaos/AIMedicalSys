# Phase 5 包 G OOD 设计审查诊断报告（v14，第二轮）

审查时间：2026-06-27
审查轮次：第 14 轮（第二轮审查，响应质询意见修订）
审查维度：需求响应充分度、事实/逻辑正确性、深度与完整性（侧重内部审议未覆盖维度）

---

## 发现的问题

### 保留问题（上一轮确认有效，本轮保留）

#### 1. [严重] `AbstractCapabilityExecutor.execute()` 中 request 变量重赋值后捕获到 lambda，造成 Java 编译错误

**问题描述**：`AbstractCapabilityExecutor.execute()` 模板方法伪代码（§4.1）执行 `request = objectMapper.convertValue(request, request.getClass())` 对局部变量 `request` 进行了重赋值。随后该变量被 `supplyAsync(() -> { return doExecuteInternal(..., request, ...) })` 和 `.exceptionally(ex -> { ... doDegrade(..., request, ...) })` 两个 lambda 同时捕获。Java 语言规范要求被 lambda 捕获的局部变量必须是 effectively final（初始化后不再赋值），此处因重赋值而不满足此条件，代码将无法通过编译。

**所在位置**：§4.1 `AbstractCapabilityExecutor.execute()` 伪代码，第 1510 行（重赋值）+ 第 1525 行（supplyAsync lambda）+ 第 1535 行（exceptionally lambda）

**严重程度**：严重

**改进建议**：将防御性拷贝结果存入新局部变量而非重赋给 `request`，例如 `finalRequest = objectMapper.convertValue(request, request.getClass())`，所有后续步骤均使用 `finalRequest`，保持原 `request` 不变以符合 effectively final 要求。

---

#### 2. [严重] 薄适配器提取方法命名与基类模板方法签名不匹配，子类无法正确重写

**问题描述**：§3.1 薄适配器 CapabilityExecutor 的公共字段提取策略中定义了 `extractVisitId()`（第 717 行）和 `extractPatientId()`（第 721 行）两个方法，但 `AbstractCapabilityExecutor` 模板方法模式中（§3.1 第 845-857 行）要求重写的方法签名是 `doExtractVisitId(request)` 和 `doExtractPatientId(request)`——带 `do` 前缀且接受 `request` 参数。薄适配器中定义的 `extractVisitId()` 与 `doExtractVisitId(request)` 在名称和参数列表上均不一致，无法通过 Java 方法重写机制正确绑定。实现者若按薄适配器伪代码片段实现，将不会覆盖基类方法的默认行为，导致 Phase 4 薄适配器 DTO 的就诊上下文全部丢失。

同时，`doExtractDepartmentId(request)` 在同段中写对了带 `do` 前缀和 `request` 参数，而 `extractVisitId()`/`extractPatientId()` 缺少，属于同一段落内的笔误。

**所在位置**：§3.1 薄适配器伪代码（第 717-723 行）vs §4.1 execute() 模板方法（第 1503-1505 行）

**严重程度**：严重

**改进建议**：将薄适配器中的 `extractVisitId()` 和 `extractPatientId()` 统一修正为 `doExtractVisitId(request)` 和 `doExtractPatientId(request)`，签名与 `AbstractCapabilityExecutor` 的模板方法保持一致。同时补充 `doExtractSessionId(request)` 的重写说明（当前薄适配器段缺少 `sessionId` 的独立提取方案描述）。

---

#### 3. [严重] Maven 依赖作用域在 §2.2 和 §3.1 中存在矛盾，实现者无法确定正确配置

**问题描述**：§2.2 模块依赖方向（第 169 行）明确冻结决策为 `provided` 作用域，并给出了三条选择理由。但 §3.1 薄适配器 Phase 4 业务服务注入方式代码示例（第 675 行注释）声明"在 Maven pom.xml 中声明对 Phase 4 各业务模块的 **compile** 依赖"。两处对同一依赖的作用域定义直接矛盾，实现者无法判断应使用 `provided` 还是 `compile`。

**所在位置**：§2.2 第 169 行（`provided`）vs §3.1 第 675 行注释（`compile`）

**严重程度**：严重

**改进建议**：统一为 §2.2 决策的 `provided` 作用域，将 §3.1 第 675 行注释中的 `compile` 改为 `provided`，并简要注明引用 §2.2 的依赖规则。同时，`provided` 作用域在标准 Spring Boot uber-JAR 部署中需要额外确认——`provided` 依赖默认不被打包进 BOOT-INF/lib，需确保 Phase 4 模块 JAR 通过其他方式位于运行时类路径上，否则薄适配器在运行时将因 ClassNotFoundException 无法加载。建议在 §10.3 的升级回退方案中补充此运行时风险评估。

---

#### 4. [重要] 降级预检循环的 degrade reason 取值方式与 DegradationReason 枚举体系不一致

**问题描述**：§4.1 降级预检循环中，当策略 `shouldDegrade()` 返回 true 时，degrade reason 取值为 `strategy.getClass().getSimpleName()`——即 Java 类名字符串。但 §3.8 新定义了 `DegradationReason` 枚举（第 1437-1447 行），其中 `CIRCUIT_BREAKER_OPEN`、`TIMEOUT` 等枚举常量专门用于标识相同场景。两套取值体系（类名 vs 枚举常量）并存导致降级日志和指标中的原因值不一致，影响告警聚合和故障定位。

**所在位置**：§4.1 第 1520-1521 行（降级预检 degrade reason 取值）vs §3.8 DegradationReason 枚举定义（第 1437-1447 行）vs §5.1 错误分类表

**严重程度**：重要

**改进建议**：在降级预检循环中维护策略类名到 `DegradationReason` 枚举的映射，或修改 `DegradationReason` 枚举增加 `STRATEGY_TRIGGERED` 通用常量并附加策略类名作为详情。至少应在 §4.1 伪代码注释中说明降级预检路径的策略类名取值方式与 `DegradationReason` 枚举的关系，避免实现者猜测。

---

#### 5. [重要] `capabilityTimeoutConfig` 字段在类图和构造器中均未定义，实现者无法定位配置来源

**问题描述**：`AbstractCapabilityExecutor.execute()` 模板方法（§4.1 第 1530 行）直接引用 `capabilityTimeoutConfig.getOrDefault(capabilityId, Duration.ofSeconds(60))`，但 §2.3 类图中 `AbstractCapabilityExecutor`（第 218-229 行）只声明了方法，未声明任何字段。文档§3.1 文本提到"存储在 `capabilityTimeoutConfig`（`Map<String, Duration>`），通过 `AiPlatformConfig` 从 YAML 绑定"，但未定义该字段如何注入到 `AbstractCapabilityExecutor`（构造器注入？`@Value` 绑定？父类字段？）。实现者在阅读类图和模板方法时将无法确定该变量的来源，需要跨章节拼凑信息才能理解装配路径。

**所在位置**：§2.3 AbstractCapabilityExecutor 类图（第 218-229 行）vs §4.1 第 1530 行（引用未定义字段）

**严重程度**：重要

**改进建议**：在 §2.3 类图 `AbstractCapabilityExecutor` 中补充 `-capabilityTimeoutConfig: Map<String, Duration>` 字段，或在抽象骨架的职责描述中增加一个明确的"字段来源"子段，说明 `capabilityTimeoutConfig` 的注入方式和默认值策略。

---

#### 6. [中等] 薄适配器 `doExecuteInternal()` 在 `llmCallExecutor` 线程中阻塞等待 `ForkJoinPool` 任务，潜在线程饥饿风险

**问题描述**：薄适配器 `doExecuteInternal()`（§3.1 第 736-738 行）使用 `CompletableFuture.supplyAsync(() -> phase4ServiceDelegate.execute(request))`（默认 ForkJoinPool）提交任务后，在当前线程（即 `llmCallExecutor` 线程池中的线程）调用 `delegateFuture.get(thinAdapterTimeout.toMillis(), TimeUnit.MILLISECONDS)` 同步阻塞等待结果。v14 修订解决了"嵌套提交到同一线程池"的死锁问题，但引入了新风险：`llmCallExecutor` 的线程数有限（核心线程 = 可用模型端点数，通常 2~5），如果多个薄适配器请求同时执行，`llmCallExecutor` 线程将全部被 `get()` 阻塞等待 ForkJoinPool 完成委托调用，无法处理普通 LLM 调用任务，造成 LLM 调用路径的线程饥饿。文档未评估此场景。

**所在位置**：§3.1 薄适配器 `doExecuteInternal()` 伪代码（第 736-738 行）

**严重程度**：中等

**改进建议**：在 §3.1 或 §6.1 中评估此阻塞等待模式对 `llmCallExecutor` 线程池可用性的影响，并给出约束条件（如 `corePoolSize >= thinAdapterCount + 1`）。或考虑使用独立线程池处理薄适配器委托，而非共用 `llmCallExecutor`。

---

### 新增问题（本轮审查发现，侧重需求响应充分度、深度完整性、落地就绪度）

#### 7. [严重] `ModelRoute` 缺少 `parameters` 字段，管线伪代码引用未定义方法，直接阻碍编码实现

**问题描述**：§4.1 `doExecuteInternal()` 伪代码第 1574 行调用 `modelRoute.getParameters()` 获取生成参数（temperature、maxTokens 等），但 §2.3 类图中 `ModelRoute`（第 264-272 行）仅声明了 endpointId、modelId、endpoint、clientType、connectionTimeout、readTimeout 六个字段，未定义 `parameters` 字段及 `getParameters()` 方法。`ModelRoute` 无法返回 `Map<String, Object>` 类型的生成参数。实现者按类图实现的 `ModelRoute` 将缺少 `getParameters()` 方法，导致 §4.1 伪代码对应的 Java 代码无法通过编译。这是一个阻碍编码的严重事实缺漏。

**所在位置**：§2.3 ModelRoute 类图（第 264-272 行）vs §4.1 第 1574 行

**严重程度**：严重

**改进建议**：在 §2.3 类图 `ModelRoute` 中补充 `+parameters: Map<String, Object>` 字段及其 getter；同步更新 §3.2 ModelRoute 字段扩展表，增加一行 `parameters` 字段说明其包含的典型条目（temperature、maxTokens、topP 等）和默认值策略；在 §9.5 YAML 路由配置示例中为每条路由补充 `parameters` 块。

---

#### 8. [重要] `DegradationContext` 类图与 §3.8 文本描述严重不一致，实现者无法获得完整类型视图

**问题描述**：§3.8 文本为 `DegradationContext` 定义了丰富的防御性扩展设计——包括 `serializedTimestamp` 字段（用于 TTL 校验）、`postDeserializationValidate()` 后置校验方法、`isFresh()` 和 `isInitialized()` 守卫判定方法、`TTL` 配置等。但 §2.3 类图中的 `DegradationContext`（第 427-435 行）仅列出了 `invocationCount`、`lastFailureTime`、`elapsedTime`、`requestType`、`failureCount` 五个字段和 `builder()` 方法，完全没有体现文本中描述的序列化防护机制字段和方法。实现者仅浏览类图将认为 `DegradationContext` 只是一个简单的 POJO，完全不会了解二进制兼容性防护措施的代码结构。

**所在位置**：§2.3 DegradationContext 类图（第 427-435 行）vs §3.8 文本描述（第 1414-1422 行）

**严重程度**：重要

**改进建议**：在 §2.3 类图 `DegradationContext` 中补充：(1) `-serializedTimestamp: long` 字段；(2) `+postDeserializationValidate(): void` 方法；(3) `+isFresh(): boolean` 方法；(4) `+isInitialized(): boolean` 方法。同步在 §3.8 类图引用的 Mermaid 代码中更新，确保类图始终是完整类型视图的第一入口。

---

#### 9. [重要] `PromptTemplateManager` 和 `ExperimentManager` 的缓存失效范围未定义，运行时可能出现视图不一致

**问题描述**：§6.1 线程模型中提到"管理端变更通过 Spring ApplicationEvent 通知缓存失效"，但未定义缓存失效的具体范围——是全局清除（全量刷新）还是按能力+科室选择性失效？若为全局清除，高并发场景下多线程同时重新加载缓存可能对数据库造成突发查询压力（thundering herd）；若为选择性失效，未明确定义失效键的构成规则和事件发布契约。实现者无法确定缓存失效后的重建行为，可能导致读取到过期数据或性能抖动。此问题直接影响运行时正确性和可维护性。

**所在位置**：§6.1（第 1729-1730 行），§3.3（第 1074 行），§3.4（第 1100-1104 行）

**严重程度**：重要

**改进建议**：在 §3.3 或 §6.1 中显式定义：(1) 失效事件 Payload 结构（包含 capabilityId、departmentId，null 表示全局失效）；(2) 失效后的重建策略（惰性加载 + 分布式锁防 thundering herd，或定时预热）；(3) 失效事件的发布方和消费方时序关系（异步还是同步通知）。

---

#### 10. [重要] 客户端侧缺少主动限流/速率保护机制，高并发下可能触发供应商侧限流惩罚

**问题描述**：整个底座设计了丰富的被动容错机制（熔断、超时降级、端点健康管理、重试），但缺少主动的客户端限流/速率保护（rate limiting / token bucket）。当瞬时流量激增时，`LlmClient` 会以无节制速率向模型供应商 API 发送请求，可能触发供应商侧的 429 Rate Limit 响应。此时 `LlmClient` 内部的重试逻辑（重试 1 次）将短时间窗口内再次发送请求，进一步加剧限流惩罚（某些供应商在 429 后返回 Retry-After Header，无视重试则返回 429 直到窗口结束）。全部请求同时降级后，熔断器窗口期内无法区分"供应商过载"与"供应商故障"，导致不恰当的熔断决策。对于医疗系统，此缺失在高峰时段可能导致大范围 AI 能力不可用。

**所在位置**：全局设计缺口——§3.2 LlmClient、§3.8 降级策略、§4.1 管线伪代码均未覆盖主动限流

**严重程度**：重要

**改进建议**：在 LlmClient 的上层或内部增加令牌桶/滑动窗口限流器：(1) 按 endpointId 或 modelId 维度配置速率限制（如 100 req/min）；(2) 超过限制时请求排队等待或直接降级；(3) 限流触发时记录的指标区别于熔断降级（降级原因应区分 `RATE_LIMITED`）；(4) 在 §7 设计决策表中新增"主动限流"决策行，记录 Phase 5 选择暂不实现（若决定 Phase 6 引入）及其风险接受理由。

---

#### 11. [中等] `AiCallLogEntity` 缺少数据保留与清理策略，长期运行后将产生严重的存储与查询性能问题

**问题描述**：`AiCallLogEntity` 对应 `ai_call_log` 表（§3.5），每次 AI 调用记录一行。以日均 10 万次 AI 调用估算，一年产生约 3650 万行记录，19 个字段（含 VARCHAR(500) 的 input_summary 和 output_summary）的数据量可达数十 GB。设计文档定义了完整的索引策略（9 条索引），但未定义任何数据保留策略（TTL、分区策略、归档方案、数据清理机制）。"永远保留所有记录"将导致：(1) 表体积增长影响写入性能（B+Tree 深度增加）；(2) 按时间范围查询的索引维护成本上升；(3) 无滚动清理机制，长期运维不可控。

**所在位置**：§3.5 AiCallLogEntity 段落（第 1251-1293 行）

**严重程度**：中等

**改进建议**：在 §3.5 中补充数据生命周期管理策略：(1) 建议按 `call_time` 按月分区（RANGE 分区或 Postgres 继承表）；(2) 定义保留期限（如 12 个月），超期数据归档或删除；(3) 增加 `batch_delete` 清理策略及对索引碎片的影响说明；(4) 在 §9.5 YAML 配置中新增 `metrics.retention-days: 365` 配置项。

---

## 需求响应充分度评估

用户需求明确要求覆盖"类图、核心职责、协作关系、关键接口、状态模型等 OOD 核心要素"。以下是逐项对照：

| OOD 核心要素 | 覆盖状态 | 说明 |
|-------------|---------|------|
| **类图** | 已覆盖 | §2.3 提供完整的 Mermaid classDiagram，涵盖全部核心抽象 |
| **核心职责** | 已覆盖 | §3.1-§3.8 各组件均有明确的职责描述段 |
| **协作关系** | 已覆盖 | §3 各节含"协作对象"子段，类图含关联箭头及 cardinality |
| **关键接口** | 已覆盖 | CapabilityExecutor、LlmClient、ModelRouter 等接口均定义了方法签名 |
| **状态模型** | **部分覆盖** | PromptTemplate（§3.3）、Experiment（§3.4）、CircuitBreakerDegradationStrategy（§3.8）、ModelEndpointHealthManager（§3.2）有状态模型；但 AiCallLogEntity、DatabasePromptTemplateManager 缓存、CredentialProvider 缓存、DefaultModelRouter 路由表等关键抽象缺少状态模型或生命周期定义 |

**总体评价**：需求中明确的 5 项 OOD 核心要素中，4 项已完整覆盖。状态模型覆盖了核心的业务与架构抽象（模板、实验、熔断器、端点健康），但基础设施层的抽象（数据持久化、缓存、凭据管理）未覆盖状态/生命周期模型，后者对落地编码和运维同样重要。

## 落地就绪度评估

**可直接指导编码的方面**：
- `CapabilityExecutor` 接口及方法签名定义清晰，泛型参数明确
- `AbstractCapabilityExecutor` 模板方法伪代码覆盖面全，降级预检→超时兜底→doExecuteInternal 的模板步骤完整
- 目录结构和组件归属（包路径、JPA Entity 定义）可直接指导模块创建

**阻碍直接编码的缺陷**：
- 问题 1（request 变量 lambda 捕获编译错误）
- 问题 3（Maven 作用域矛盾，含 Spring Boot uber-JAR 的 `provided` 运行时风险）
- 问题 7（ModelRoute 缺失 `parameters` 字段，getParameters() 调用无法编译）
- 问题 2（薄适配器方法命名不一致，导致字段提取走错路径）

**异常场景与边界条件覆盖缺口**：
- **已覆盖**：LLM 调用超时/失败、熔断、端点不可用、模板渲染异常、实验分流异常、解析失败、薄适配器委托超时、整体管线超时
- **未充分覆盖**：供应商侧限流（429）、数据库完全不可用（DB down，指标写入会如何？模板/实验查询会如何？）、凭据管理器完全不可用（Vault 完全宕机且缓存过期后的行为）、缓存 thundering herd

**接口定义对下游消费者的支撑度**：
- `AiService` 接口完全不变，下游业务模块零感知——设计优秀
- `CapabilityExecutor` 接口定义了完备的泛型协定，新增能力只需新增一个实现——设计优秀
- `LlmClient` 和 `ModelRouter` 接口简洁清晰，实现者可以直接编码
- 但类图跟伪代码/文本的不一致（问题 5、7、8）会误导实现者

## 整体质量评价

设计文档经过 14 轮迭代（含 17 版修订）后，在架构逻辑、组件划分、管线定义等核心维度已达到较高成熟度——类图完整、伪代码详细、设计决策记录规范、迁移路径明确。

当前发现的 **11 个问题中，5 个为严重级别**：问题 1（lambda 编译错误）、问题 2（薄适配器方法命名）、问题 3（Maven 作用域矛盾）、问题 7（ModelRoute 缺少 parameters 字段）直接阻碍编码启动，应在进入实现前全部修复。问题 8 和 9 虽不直接导致编译失败，但会导致实现者按错误的类型视图编码或忽略重要的运行期防护机制。问题 10（限流缺失）和 11（日志表清理策略）属于长期可运维性缺口，建议在本轮或 Phase 6 规划中处理。

**与内部审议（技术可行性维度）的关系说明**：问题 1/2/3/4/5/6 属于代码级实现细节和一致性校验，这些已在内部审议覆盖。问题 7/8/9/10/11 侧重需求响应完整性、接口定义完备性、可运维性等内部审议可能未充分覆盖的维度。建议将问题 1-6 的修复交由实现者/技术审议方处理，本报告的问题 7-11 为本次审查新增的增量价值发现。

---

## 修订说明（v14-v2）

本报告在上一轮（b_v14_diag_v1.md）基础上，根据 b_v14_challenge_v1.md 的质询意见进行以下修订：

| 质询意见 | 回应 |
|---------|------|
| **1. 整体质量评价缺少证据链**："设计文档经过14轮迭代后需求覆盖完成，整体架构合理"的断言无分析支撑，未指明从何处验证需求覆盖完整性 | 已采纳。本报告新增「需求响应充分度评估」章节，以对照表形式逐项检查用户要求的5项OOD核心要素的覆盖状态，并标注"部分覆盖"项。整体质量评价改为更具体的分项评述。 |
| **2. 审查方向与任务要求错位**：全部7个问题属于代码级技术可行性校验，而任务要求侧重"需求响应充分度、整体深度和完整性"，报告对此近乎空白 | 已采纳。本报告新增问题7-11（侧重接口定义完备性、缓存边界、限流缺口、数据生命周期等深度与完整性维度）。保留的问题1-6在整体质量评价中标注了"与内部审议关系说明"，以区分技术可行性细节与增量价值发现。 |
| **3. 需求覆盖矩阵缺失**：未对照用户要求的5项OOD核心要素逐项标注覆盖状态 | 已采纳。本报告新增「需求响应充分度评估」表格，逐项检查类图、核心职责、协作关系、关键接口、状态模型的覆盖情况。 |
| **4. 落地就绪度评估缺失**：未评估设计是否可直接指导编码、接口定义是否足够支撑下游消费者、异常场景和边界条件是否覆盖 | 已采纳。本报告新增「落地就绪度评估」章节，区分可直接编码的方面、阻碍编码的缺陷、异常场景覆盖缺口、接口定叉对下游支撑度。 |
| **5. 问题6（行号错乱）不应列为审查问题**：属于"数行号"细节问题，与任务描述无关 | 已采纳。问题6（原行号错乱问题）已从本报告移除。 |
| **6. 状态模型覆盖再确认**：需重点检查状态模型是否完整覆盖所有关键抽象 | 已采纳。在「需求响应充分度评估」中标注了状态模型为"部分覆盖"，并指出 AiCallLogEntity、缓存抽象、CredentialProvider 等基础设施层抽象缺少状态模型。 |
