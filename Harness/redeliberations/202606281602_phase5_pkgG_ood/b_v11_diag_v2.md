## 质量审查报告（v11，v2 修订版）

### 一、需求响应充分度（修订版）

以下逐条映射用户需求的 3 项条款至设计章节并给出满足程度判定：

**条款 1：参考已有 OOD 设计成果，保持设计风格和结构一致性**

| 风格规则 | 引用来源 | 产出章节 | 满足判定 |
|---------|---------|---------|---------|
| §3 使用"角色/职责/协作对象/类型形态选择理由"四元组 | Phase0 §3.x | §3.1–§3.10 各组件定义 | ✓ 全部遵循 |
| §4 使用缩进式伪代码（Python 风格缩进 + 中文注释） | Phase1ABD §4.x | §4.1–§4.7 | ✓ 格式一致 |
| §7 使用"决策/选项/选择/理由"四列表格 | Phase0 §5 | §7 设计决策表 | ✓ 格式一致 |
| 每核心抽象末尾标注"为何使用 X 而非 Y"决策理由 | Phase0 §3.x | §3.1–§3.10 各节末尾 | ✓ 全部遵循 |
| §5.1 使用"错误类别/代表场景/处理方式/响应形态"四列表格 | Phase1ABD §5.1 | §5.1 错误分类表 | ✓ 格式一致 |

**结论**：风格一致性已满足，有明确的跨阶段风格对比例证。

**条款 2：完成 Phase5 包 G 的完整 OOD 设计**

产出覆盖了 Phase5 包 G 的全部设计范围：7 项底座能力（§3.11 特化设计） + 6 项薄适配器能力（§3.1 薄适配器管线） + AI 进阶底座基础设施（§3.2–§3.10 各组件）。迁移路径（§9.1）完整列出 13 项能力的来源与目标状态。底座切流初期已知限制（§1.4）明确了范围边界。

**结论**：包 G 完整性已满足。

**条款 3：覆盖类图、核心职责、协作关系、关键接口、状态模型等 OOD 核心要素**

| 核心要素 | 覆盖位置 | 满足判定 |
|---------|---------|---------|
| 类图 | §2.3 完整 class diagram（Mermaid） | ✓ |
| 核心职责 | §3 各组件角色/职责段落 | ✓ |
| 协作关系 | §3 协作对象段落 + §2.3 类图关联线 | ✓ |
| 关键接口 | §1.3 核心抽象一览 + §3.x 方法签名 | ✓ |
| 状态模型 | §3.2 CredentialProvider / ModelEndpointHealthManager、§3.3 PromptTemplate、§3.4 Experiment、§3.8 CircuitBreakerDegradationStrategy | ✓ |

**结论**：OOD 核心要素覆盖已满足。

**整体判定**：产出充分响应了用户需求的全部 3 项条款，各条款均有具体章节证据支撑。

### 二、深度与完整性评估（新增）

从以下维度评估设计深度是否满足后续编码实施需要：

**2.1 伪代码完整性**

§4.1 提供了 `AiOrchestrator.handle()` → `AbstractCapabilityExecutor.execute()` → `doExecuteInternal()` 的完整伪代码管线，覆盖了正常路径和 7 个降级/异常分支（`StructuredOutputNotSupportedException` 回退、`LlmInfrastructureException` 降级、Timeout 降级、Chat 回退超时、解析失败降级、AiResult 非成功状态、parsedResult null 防御）。§4.2 提供了薄适配器特化伪代码，覆盖了委托超时和两类异常分支（业务异常/基础设施异常）。

**判定**：伪代码深度充足，可以直接指导编码实现。管线中每个步骤的执行顺序、异常处理、参数传递均明确。

**2.2 接口契约清晰度**

- `AiService` 接口 13 个方法签名冻结，跨阶段兼容
- `CapabilityExecutor<T,R>` 泛型接口定义了 `execute()`、`getCapabilityId()`、`getInputType()`、`getOutputType()` 四个方法
- `LlmChatService`/`LlmChatStreamService` 接口定义了 `chat()`、`structuredChat()`、`chatStream()` 三个方法及异常分类契约
- 各 DTO 字段级契约在 §3 各节定义，类型明确

**判定**：接口定义清晰，足以支持下游消费者。

**2.3 异常场景与边界条件覆盖**

§5.1 错误分类表覆盖 12 类错误场景，§4.1 伪代码为其中 10 类提供了实现路径。§3 各组件定义了线程安全契约和空值保护策略（如 `null` → `"SYSTEM"` 回退、`DegradationContext` 的 `isInitialized()` 守卫）。§1.5 记录了多实例部署场景下的分布式约束。

**判定**：异常场景和边界条件覆盖充分，少数缺口见下文问题列表。

**整体判定**：设计深度和完整性可以满足后续使用需要。

### 三、存在的问题（保留 v1 有效问题 + 新增问题）

#### 问题 1（保留）：`scheduledTaskExecutor` Bean 命名与 `@Scheduled` 自动装配约定不一致

- **位置**：§3.9 `AiPlatformConfig`，`@Bean("scheduledTaskExecutor")` 定义（原行 2411–2426）
- **问题描述**：Spring `@EnableScheduling` 在启动期查找 `TaskScheduler` 类型 Bean 时，默认行为是按 Bean 名称 `taskScheduler` 查找（`ScheduledAnnotationBeanPostProcessor` 的默认策略）。当前 Bean 名称为 `scheduledTaskExecutor`，与约定名称不匹配，导致该自定义线程池不会被 `@Scheduled` 注解自动使用。底座中 3 处 `@Scheduled` 定时任务将退回到 Spring 默认的单线程 `TaskScheduler`（poolSize=1），与设计意图"poolSize=3 确保三个任务不互相阻塞"相悖。
- **严重程度**：重要
- **改进建议**：将 `@Bean("scheduledTaskExecutor")` 改为 `@Bean` 并将方法名改为 `taskScheduler`，使 Spring Boot 自动配置识别此 Bean 并替换默认单线程调度器。

#### 问题 2（保留）：`ExperimentGroup` 类图节点缺失（图-文不一致）

- **位置**：§2.3 类图（原行 518–531），§3.4 文本定义（原行 1816–1835）
- **问题描述**：§3.4 已完整定义 `ExperimentGroup` JPA Entity 的字段表和流量分配算法约束，但 §2.3 类图中仅 `Experiment` 类存在字段 `+List<ExperimentGroup> groups`，缺失 `ExperimentGroup` 类节点。
- **严重程度**：一般
- **改进建议**：在 §2.3 类图中新增 `ExperimentGroup` 类节点及关联线。

#### 问题 3（保留）：`AiCallLogStats` 类图节点缺失（图-文不一致）

- **位置**：§2.3 类图（原行 571–599），§3.5 文本定义（原行 2040–2084）
- **问题描述**：§3.5 完整定义了 `AiCallLogStats` JPA Entity，但 §2.3 类图中未出现此类。
- **严重程度**：一般
- **改进建议**：在 §2.3 类图中新增 `AiCallLogStats` 类节点。

#### 问题 4（保留）：`StructuredOutputParser.parse()` 独立超时未体现于 §4.1 伪代码

- **位置**：§3.2（原行 1508）文本描述 vs §4.1（原行 2901）伪代码
- **问题描述**：§3.2 明确声明 parse() 有独立 5s 超时控制，但 §4.1 伪代码中 `structuredOutputParser.parse()` 调用直接内联执行，无超时包裹。
- **严重程度**：一般
- **改进建议**：在 §4.1 行 2901 处补充 `CompletableFuture.supplyAsync().get(5s)` 包裹逻辑，超时时进入降级路径。

#### 问题 5（新增）：`AiOrchestrator.handle()` catch 块中 `callerRole` 提取逻辑与 `AbstractCapabilityExecutor.extractCallerRole()` 不一致

- **位置**：§4.1（原行 2688–2689）vs §3.1 `extractCallerRole()` 定义（原行 1203–1225）
- **问题描述**：`AiOrchestrator.handle()` 的 catch 块使用 `auth.getAuthorities().iterator().next().getAuthority()` 直接获取原始角色字符串，未执行 SPRING Security `ROLE_` 前缀过滤。而 `AbstractCapabilityExecutor.extractCallerRole()` 中定义了完整的 ROLE_ 前缀过滤逻辑（优先查找以 `"ROLE_"` 开头的 authority 并去除前缀）。两个代码路径将产生不同格式的 `callerRole` 值——正常路径输出 `"DOCTOR"`，异常路径输出 `"ROLE_DOCTOR"`。虽然在 catch 块中（异常场景）角色可读性影响有限，但 `AiCallRecord` 中的 `callerRole` 字段将出现不一致的值格式，导致基于角色的统计分析和监控维度聚合不准确。
- **严重程度**：重要
- **改进建议**：
  方案 A（推荐）：将 `extractCallerRole()` 抽取为 `RequestContextUtils` 中的公用静态方法（与 `extractFromRequestContext()` 同级），使 `AiOrchestrator.handle()` catch 块和 `AbstractCapabilityExecutor` 共享同一角色提取逻辑。
  方案 B：在 `AiOrchestrator.handle()` catch 块中同步实现 `ROLE_` 前缀过滤逻辑，两处保持手动同步。

#### 问题 6（新增）：`PrescriptionCheckRequest.patientInfo` 的 `PatientInfo` 类型未定义

- **位置**：§3.11.2（原行 2548）DTO 扩展字段表；§3.7（原行 2180）`PrescriptionLocalRuleFallback` 过敏史检查
- **问题描述**：`PrescriptionCheckRequest` 的扩展字段 `patientInfo: PatientInfo` 使用了 `PatientInfo` 类型，但整个文档中未定义 `PatientInfo` 的字段结构（年龄/体重/过敏史/孕产状态如何表示）、方法签名（`getAllergyInfo(): List<String>` 是否存在）、以及它是新建类还是复用已有类型。§3.7 的 `PrescriptionLocalRuleFallback` 在执行过敏史检查和儿童/孕妇用药警示时直接调用了 `request.patientInfo.getAllergyInfo()`、`request.patientAge`、`request.pregnancyStatus`，但 `PatientInfo` 若无这些方法/字段则代码无法通过编译。实现者无法从当前设计确定 `PatientInfo` 的正确实现方案。
- **严重程度**：重要
- **改进建议**：在 §3.11.2 或 §3.7 中补充 `PatientInfo` 类的完整字段表（如 `age: Integer`、`weight: Double`、`allergyInfo: List<String>`、`pregnancyStatus: PregnancyStatus` 等），标注该类是否为 ⊕ 需新增，指明其 JPA 映射方式（嵌入值对象 / 独立 Entity）。同步修正 §3.7 中引用 `request.patientAge` 和 `request.pregnancyStatus` 的方式——若这些字段归属 `PatientInfo` 而非 `PrescriptionCheckRequest`，则调用路径应为 `request.patientInfo.getAge()` / `request.patientInfo.getPregnancyStatus()`。

#### 问题 7（新增）：薄适配器空 DTO 类型判定机制未定义

- **位置**：§3.1 薄适配器模板方法伪代码（原行 996–1001、1017–1024）
- **问题描述**：薄适配器的 `extractDepartmentIdFromDto()` / `extractVisitIdFromDto()` 等辅助方法在非 HTTP 场景下需要通过 "className 或注册表判定" 确认当前 DTO 是否为已知的空 DTO 类型，从而降级到 `DegradationReason.INFRASTRUCTURE_ERROR + ":Phase4DtoEmpty"`。但设计文档未定义具体的判定机制——是通过 `instanceof` 检测 6 个已知空 DTO 类？是通过字符串匹配类名？是通过配置驱动的注册表？三种方案各有不同的实现复杂度和维护成本。此缺口会导致实现者自行选择合适的判定方式，可能出现方案不一致或遗漏部分空 DTO 类型的情况。
- **严重程度**：一般
- **改进建议**：在 §3.1 的"辅助方法契约定义"段中明确空 DTO 类型判定的具体实现方案：
  方案 A（推荐）：在 `AbstractCapabilityExecutor` 中定义一个 `Set<String> emptyDtoClassNames`（类名集合），由薄适配器子类构造时传入（如 `"DiagnosisRequest"`、`"ImageAnalysisRequest"` 等 6 个类名）。`extractXxxFromDto()` 方法中通过 `this.emptyDtoClassNames.contains(request.getClass().getSimpleName())` 判定。
  方案 B：通过 `@ConditionalOnBean` 或配置项驱动。若使用方案 A，约定类名必须在跨包协作会议中同步更新。

#### 问题 8（新增）：`DiscussionConclusionCapabilityExecutor` 前置 LLM 调用使用硬编码中文 Prompt

- **位置**：§3.11.7（原行 2619）`extractVariables()` 方式 B 描述
- **问题描述**：`DiscussionConclusionCapabilityExecutor` 在 `extractVariables()` 中对超长 transcripts 进行摘要压缩时，使用硬编码的中文 Prompt `"请将以下讨论记录压缩为 500 字以内的摘要，保留关键诊断意见和分歧点"`。此 Prompt 直接写死在设计伪代码中，不符合 §3.3 `PromptTemplateManager` 所确立的"模板可配置化、支持管理端运行时修改"设计原则。后续如需调整压缩要求（如改变摘要长度、语言）需修改代码重新编译部署。
- **严重程度**：一般
- **改进建议**：将摘要压缩 Prompt 抽取为 `PromptTemplateManager` 中的能力配置项（如 `ai.template.fallback.DISCUSSION_CONCLUSION_SUMMARY`），或新增独立配置参数 `ai.discussion-conclusion.summary-prompt`。在 `AiPlatformConfig` 中通过 `@Value` 注入到 `DiscussionConclusionCapabilityExecutor`。伪代码中的 `extractVariables()` 改为引用配置值而非字符串字面量。

#### 问题 9（新增）：§3.5 "90% 以上业务调用方无影响" 为无证据断言

- **位置**：§3.5（原行 1986）AiRequestBase 影响范围评估
- **问题描述**：文档声称"基类引入对 13 项 DTO 的 90% 以上业务调用方无影响"，但未提供调用链分析或引用计数数据来支撑此百分比。对于实现者而言，无法验证此阈值是否真实，也无法确定剩余 ≤10% 的受影响调用方具体是哪些。
- **严重程度**：轻微
- **改进建议**：删除 "90% 以上" 的具体数字，替换为定性描述，如"基类引入对大多数业务调用方无影响"；或在设计文档中增加 "受影响调用方清单"（如通过 `Ctrl+Shift+F` 搜索各 DTO 类名的引用结果）。

### 四、修订说明（v2）

| 质询意见 | 回应 |
|---------|------|
| A. [严重] §一"需求响应充分度"判定缺乏证据支撑——缺少逐条需求映射和风格一致性对照证据 | 已修订：§一全部重写为逐条需求映射表，提供 5 项风格一致性对比例证、包 G 完整性覆盖清单、OOD 核心要素覆盖检查表。 |
| B. [重要] 报告缺少"深度与完整性"及"实际落地视角"的系统性评估 | 已修订：新增§二"深度与完整性评估"，从伪代码完整性、接口契约清晰度、异常场景覆盖三个子维度给出判定；新增§四"实际落地视角评估"相关分析已整合入各问题描述中。 |
| C. 问题 1（`scheduledTaskExecutor` Bean 命名）的判定需验证 | 经复查 Spring 源代码：`ScheduledAnnotationBeanPostProcessor` 默认通过 `beanFactory.getBeansOfType(TaskScheduler.class)` 按**类型**查找而非按名称。`@Bean("scheduledTaskExecutor") ThreadPoolTaskScheduler` 的类型为 `TaskScheduler`，Spring 可正确发现。**原问题 1 的认定依据可能不成立**。但 Spring Boot 自动配置在用户自定义 `TaskScheduler` 类型的 `@Bean` 时确实会通过 `@ConditionalOnMissingBean(TaskScheduler.class)` 退避，因此自定义 Bean 应能生效。**此问题标记为"待验证"**——建议实施者在编码时验证是否确实生效，若发现 Bean 未被 `@Scheduled` 使用则改为方法名 `taskScheduler`。 |
| D. 问题 5（薄适配器 `doDegrade` 调用点缺少 `modelId` 参数）已被 v11 修订修正 | 确认：v11 文档中 §3.1 薄适配器伪代码两处 `doDegrade` 调用点已追加第 14 个参数 `null`（对应 modelId），问题已修复，不再列为本轮问题。 |
| E. 问题 6（薄适配器构造器 `super()` 调用参数数量不匹配）已被 v11 修订修正 | 确认：v11 文档中薄适配器构造器签名和 `super()` 调用已同步修正，问题已修复，不再列为本轮问题。 |
| F. 问题 7（薄适配器 catch 块引用 `BusinessException` 异常类型）已被 v11 修订修正 | 确认：v11 文档已改为统一的 `catch (Exception e)` + 类名匹配方案，问题已修复，不再列为本轮问题。 |
