# 质量审查诊断报告 — Phase5 包G OOD v21

## 审查范围

- **待审查产出**：a_v21_copy_from_v20.md（Phase 5 包 G 架构级 OOD 设计方案 v21）
- **审查维度**：需求响应充分度、事实错误/逻辑矛盾、深度与完整性（侧重内部审议未充分覆盖的方面）
- **内部审议已覆盖**：技术可行性、组件间协作正确性、接口设计合理性、异常处理完备性等（经 21 轮迭代）

---

## 发现的问题

### 问题 1（重要 — 事实错误）薄适配器默认超时值在 §4.2 与 §3.1/§9.5 之间矛盾

- **问题描述**：§4.2（薄适配器 CapabilityExecutor 特化管线）注释声称薄适配器委托调用的默认超时为 **60s**，但 §3.1（AbstractCapabilityExecutor 整体管线超时机制）明确 thinAdapterTimeout 默认值为 **30s**（通过 `@Value("${ai.execution.timeout.thin-adapter-default:30s}")` 注入），§9.5 YAML 配置同样声明 `thin-adapter-default: 30s`。三处不一致，实现者无法确定正确默认值。
- **所在位置**：§4.2 第 2176 行注释 `// 差异 3：独立 thinAdapterTimeout 超时控制（默认 60s，可通过 YAML 按能力配置）` vs §3.1 第 1018 行 vs §9.5 第 2547 行
- **严重程度**：重要
- **改进建议**：将 §4.2 注释中的 "默认 60s" 修正为 "默认 30s"，与 §3.1 和 §9.5 保持一致。

---

### 问题 2（重要 — 事实错误）`metricsAsyncExecutor` @Bean 伪代码硬编码值与 YAML 配置值不一致

- **问题描述**：§3.9 `AiPlatformConfig` 中 `metricsAsyncExecutor()` @Bean 方法硬编码 `corePoolSize=2`、`maxPoolSize=4`，但 §9.5 YAML 配置中 `metrics.async` 块声明 `core-pool-size: 1`、`max-pool-size: 2`。两组值相互矛盾，且 @Bean 方法未展示从 YAML 动态绑定的逻辑。若 @Bean 为硬编码默认值，则 YAML 配置成为摆设；若 YAML 应生效，则 @Bean 方法应展示配置绑定机制。
- **所在位置**：§3.9 第 1843-1844 行 `executor.setCorePoolSize(2); executor.setMaxPoolSize(4);` vs §9.5 第 2561-2562 行 `core-pool-size: 1; max-pool-size: 2`
- **严重程度**：重要
- **改进建议**：统一两处的默认值约定——建议以 YAML 配置值为准，将 @Bean 伪代码改为展示从 `@ConfigurationProperties(prefix = "ai.metrics.async")` 绑定的动态值注入方式，或统一硬编码值与 YAML 值一致（推荐前者，避免维护双份默认值）。

---

### 问题 3（一般 — 逻辑矛盾）薄适配器 `doExtractDepartmentId()` 伪代码与 §3.10 非 HTTP 回退策略文本描述不一致

- **问题描述**：§3.10 明确声明薄适配器在非 HTTP 场景下 `doExtractDepartmentId()` 应"回退到 `request` 对象中由调用方显式传递的对应字段"，但 §3.1 薄适配器 `doExtractDepartmentId()` 伪代码仅实现了 `RequestContextUtils.extractFromRequestContext("X-Department-ID")` 单一路径，未展示 DTO 字段回退的逻辑。文本承诺的"三层非 HTTP 回退路径"中的薄适配器 DTO 回退层在伪代码中未体现，导致非 HTTP 场景（MQ 消费者、定时任务）下部门标识提取路径不可追踪。
- **所在位置**：§3.1 第 831-834 行伪代码 vs §3.10 第 1926-1931 行文本描述
- **严重程度**：一般
- **改进建议**：在 §3.1 薄适配器 `doExtractDepartmentId()`/`doExtractVisitId()` 等方法的伪代码中补充 DTO 字段回退逻辑（如 `if (request.hasDepartmentId()) → return request.getDepartmentId(); else → return RequestContextUtils.extractFromRequestContext(...)`），使伪代码与文本描述的承诺一致。

---

### 问题 4（一般 — 设计缺口）降级策略 Bean name 与 YAML 引用名之间的映射未显式约定

- **问题描述**：§3.1 YAML 配置到 Bean 的装配路径中，YAML 的 `strategies` 列表使用简名 `"timeout"`、`"circuit-breaker"`、`"noop"` 引用策略实现。但设计未说明这些简名如何映射到实际的 Spring Bean——`@Component` 默认生成的 Bean name 为 `timeoutDegradationStrategy`（类名首字母小写），与 `"timeout"` 不匹配。实现者需自行推断需使用 `@Component("timeout")` 显式命名。此缺口在 21 轮内部审议中未被捕获，可能直接导致 YAML 配置到 Bean 查找失败（`getBean("timeout")` 返回 null）。
- **所在位置**：§3.1 第 759-769 行 → `"timeout"` 等引用名 vs Spring 默认 Bean name 推导规则
- **严重程度**：一般
- **改进建议**：在 §3.1 降级策略装配路径的步骤 3 或步骤 4 中补充说明：`@Component("timeout")`、`@Component("circuit-breaker")`、`@Component("noop")` 的显式 Bean name 声明方式；或在 §9.5 YAML 配置示例旁注明 Bean name 匹配约定。

---

### 问题 5（一般 — 完整性不足）`DegradationContext.setDepartmentId()` 在管线伪代码中被调用但未在核心定义中声明

- **问题描述**：§4.1 降级预检伪代码调用 `context.setDepartmentId(departmentId)`，将科室标识写入降级判定上下文。但 §3.8 `DegradationContext` 的扩展字段表和 §2.3 类图均未列出 `departmentId` 字段或 `setDepartmentId()` 方法。调用点存在但定义缺失，实现者不清楚 `DegradationContext` 是否应包含 `departmentId`。
- **所在位置**：§4.1 第 2022 行 `context.setDepartmentId(departmentId)` vs §3.8 扩展字段表 vs §2.3 类图 `DegradationContext` 节点
- **严重程度**：一般
- **改进建议**：在 §3.8 `DegradationContext` 扩展字段表中补充 `departmentId: String` 字段及对应 setter/getter 声明；同步更新 §2.3 类图。

---

### 问题 6（轻微 — 完整性不足）`LlmChatOptions` 两阶段填充策略在管线伪代码中无对应实现步骤

- **问题描述**：§3.2 定义了详细的「LlmChatOptions 与 ModelRoute.parameters 的两阶段填充策略」（基值填充 + 调用方覆盖），但 §4.1 管线伪代码仅展示了 `new LlmChatOptions(modelId: modelRoute.getModelId(), ...)` 的简化写法，未体现从 `modelRoute.getParameters()` Map 提取字段的步骤，也未展示阶段二的调用方覆盖赋值。管线实现者从伪代码中无法得知两阶段策略的实际编码方式。
- **所在位置**：§3.2 第 1270-1276 行文本定义 vs §4.1 第 2084 行伪代码 `LlmChatOptions` 构造
- **严重程度**：轻微
- **改进建议**：在 §4.1 管线伪代码中的 `LlmChatOptions` 构造处展开为两阶段显式步骤：先 `options = LlmChatOptions.fromParameters(modelRoute.getParameters())` 完成基值填充，再 `options.setModelId(assignment.getTargetModelId())`（若需要覆盖）完成调用方覆盖。

---

### 问题 7（重要 — 完整性不足）structuredChat 异常类型在核心抽象和类图中缺失

- **问题描述**：v21 新增的 `structuredChat()` 异常分类在 §3.2、§4.1、§5.1、§7 中跨章节使用 `StructuredOutputNotSupportedException` 和 `LlmInfrastructureException` 两个异常类型作为管线分支判定依据。然而这两个异常类型均未出现在 §1.3 核心抽象一览表、§2.3 类图、§2.1 目录结构中。具体缺口包括：未定义异常类的包路径（归属哪个子包）、继承层次（是否继承 RuntimeException）、构造器签名。尤其 `LlmInfrastructureException` 在 §4.1 伪代码第 2114 行中调用 `e.getCause()` 和 `e.getMessage()`，暗示其具有标准异常构造函数，但未冻结这些约定。管线伪代码对这两个异常类型有明确的 catch 行为差异（回退 vs 降级），但未在 `LlmChatService.structuredChat()` 接口签名中声明这些异常（即使为 unchecked 异常，也应在 Javadoc @throws 中说明）。实现阶段的异常类型选择分歧将导致管线行为不一致。
- **所在位置**：§3.2 第 1051 行 vs §1.3 第 44-83 行、§2.3 类图、§2.1 目录结构
- **严重程度**：重要
- **改进建议**：在 §1.3 核心抽象一览表中补充 `StructuredOutputNotSupportedException` 和 `LlmInfrastructureException` 两行；在 §2.3 类图中补充异常类型节点（或注明 `LlmChatService.structuredChat()` 的 @throws 声明）；在 §2.1 目录结构中指定异常类包位置（建议 `ai-impl/client/exception/`）；在 §3.2 `LlmChatService` 接口方法签名处补充 @throws 文档说明。

---

### 问题 8（一般 — 完整性不足）CredentialProvider Vault 降级行为缺失正式状态模型

- **问题描述**：§3.2 `CredentialProvider` 的 Vault 不可达降级行为（第 1366-1368 行）具有明显的多状态行为特征——正常查询 → 超时使用缓存 + 延长 TTL → 连续 5 次失败后启动 30 秒退避窗口 → 窗口到期后重试。然而此行为仅以三点文本描述，未像 `ModelEndpointHealthManager`（§3.2 状态转换表）或 `CircuitBreakerDegradationStrategy`（§3.8 CLOSED→OPEN→HALF_OPEN）那样以正式状态模型定义。具体缺口包括：退避窗口期间是否有 Vault 查询请求时如何响应（静默丢弃还是异常传播）、故障计数在退避窗口到期后是否清零、TTL 延长与退避窗口同时触发时的优先级。缺少正式状态模型导致该组件的可测试性和实现一致性低于文档中其他同类组件。
- **所在位置**：§3.2 第 1366-1368 行（Vault 不可达降级行为描述）vs 同节 ModelEndpointHealthManager 的状态转换表
- **严重程度**：一般
- **改进建议**：将 Vault 不可达降级行为形式化为与 `ModelEndpointHealthManager` 一致的状态模型，包括状态定义（NORMAL / CACHE_ONLY / BACKOFF）、转换条件表（超时/连续 N 次失败/窗口到期/探测成功）、退避窗口期间的并发请求处理策略、故障计数清除时机。

---

### 问题 9（一般 — 完整性不足）`extractCallerRole()` 与 `extractCallerId()` 的实现路径未定义

- **问题描述**：`extractCallerRole()` 和 `extractCallerId()` 作为 `AbstractCapabilityExecutor` 的辅助方法，在 §4.1 `execute()` 伪代码入口处被调用，返回值用于填充 `AiCallRecord` 的 `callerRole` 和 `callerId` 字段。然而 §3.1 仅以注释形式说明"从 `SecurityContextHolder.getContext()` 提取角色/标识"，未指定具体提取路径——`SecurityContext.getAuthentication()` 返回的 `Authentication` 对象同时包含 `getName()`（返回 principal 名称）、`getAuthorities()`（返回 `Collection<? extends GrantedAuthority>`，包含多个角色）、`getPrincipal()`（返回 Object，可能是自定义类型）。`extractCallerRole()` 是取首个 GrantedAuthority 的字符串值、拼接全部角色、还是从自定义 principal 类型中提取特定字段？当前设计未冻结此语义，13 个 CapabilityExecutor 若各自实现将导致 `AiCallRecord.callerRole` 数据格式不统一，影响监控报表的可消费性。
- **所在位置**：§3.1 第 995-1003 行（辅助方法注释说明）vs §4.1 第 921-922/2010-2011 行（调用点）vs §3.5 工厂方法签名
- **严重程度**：一般
- **改进建议**：在 §3.1 辅助方法定义处补充具体的实现路径——例如 `extractCallerRole()` 取第一个 `GrantedAuthority.getAuthority()` 返回值，`extractCallerId()` 取 `Authentication.getName()` 返回值。或在 `AiRequestBase` 中增加 `callerRole`/`callerId` 字段由调用方显式传入。同步更新 §2.3 类图补充这两个方法声明。

---

## 需求响应充分度专项评估

针对 v21 产出对用户初始需求的响应程度，逐项评估如下：

| 需求项 | 评估结论 | 说明 |
|--------|---------|------|
| 保持与 Phase0/Phase1ABD 的设计风格和结构一致性 | **已满足** | §1.2 末段显式声明设计风格一致性；章节结构（概述→模块划分→核心抽象→行为契约→错误处理→并发设计→设计决策）与 Phase0/Phase1ABD 对齐；§7 设计决策表格式一致；类型形态选择逻辑（interface vs abstract class vs class vs enum）与既有设计保持统一 |
| 完成 Phase5 包G 的完整 OOD 设计 | **已满足** | 设计覆盖了 AI 进阶底座的全部 6 个核心子域（编排/模型对接/Prompt 模板/A/B 实验/性能观测/结构化输出）+ 3 个支撑子域（降级策略/本地规则降级/配置装配）；13 项 AI 能力的迁移策略明确（7 项底座 + 6 项薄适配器）；覆盖迁移路径、并发设计、测试策略等非功能性维度 |
| OOD 核心要素覆盖 | **基本满足，存在 3 项局部缺口** | 类图（✓ §2.3 Mermaid classDiagram）、核心职责（✓ 各组件职责段）、协作关系（✓ 协作对象段 + §2.2 依赖方向图）、关键接口（✓ 方法签名显式定义）、状态模型（✓ PromptTemplate/Experiment/ModelEndpointHealthManager/CircuitBreakerDegradationStrategy 均有状态模型）。**缺口**：§3.2 structuredChat 异常类型未建模（问题 7）；CredentialProvider Vault 降级行为缺少正式状态模型（问题 8）；extractCallerRole()/extractCallerId() 实现路径未冻结（问题 9） |

---

## 整体质量评价

产出经过 21 轮内部审议，在技术可行性、接口设计、组件协作、异常处理等方面已达到较高成熟度。本审查报告在保留 v1 审查发现的 6 个问题基础上，新增 3 个问题，主要集中在异常类型建模缺口（问题 7）、状态模型覆盖不完整（问题 8）以及辅助方法实现路径未冻结（问题 9）。这些问题不影响总体架构的正确性和可落地性，但会在实现阶段引发歧义——异常类型建模缺口可能导致两个 `CapabilityExecutor` 实现采用不同的异常类层次结构，`extractCallerRole()` 语义未冻结可能导致 `AiCallRecord` 数据格式不统一。

需求响应充分度的专项评估确认产出对用户三项需求均已满足，仅 OOD 核心要素覆盖存在 3 项局部缺口（已作为问题 7/8/9 描述）。整体来说，该产出可以作为实现阶段的可靠输入。

---

## 修订说明（v2）

| 质询意见 | 回应 |
|---------|------|
| **[严重] 审查维度遗漏**：报告声明覆盖"需求响应充分度"但实际未评估，导致该核心审查维度完全缺失。产出作者无法据此判断需求响应层面的质量 | 已补充"需求响应充分度专项评估"章节，逐项评估产出对三项用户需求的满足程度。结论：需求响应充分度基本满足，存在 3 项局部缺口（已列为问题 7/8/9）。同时保留现有 6 个事实错误/一致性问题 |
| **[一般] 深度和完整性评估不系统**：仅通过局部缺口片段式触及完整性，缺少整体层面的完整性评估 | 已在"需求响应充分度专项评估"中系统评估类图、核心职责、协作关系、关键接口、状态模型五大 OOD 核心要素的覆盖状况；在"整体质量评价"中新增对设计完整性全局状况的结论性评价。新增 3 个完整性相关问题（问题 7/8/9）覆盖异常类型建模、状态模型覆盖和辅助方法定义三个具体缺口 |
