# v18 质量审查报告（v2）

## 审查范围

- **产出**：Phase 5 包 G — AI 进阶底座 架构级 OOD 设计方案（v18）
- **审查视角**：需求响应充分度、事实准确性、逻辑一致性、完整性（侧重内部审议未充分覆盖的维度）
- **审查方式**：结合代码库中已有文件验证，对照迭代历史追踪问题闭环状态

---

## 发现的问题

### 问题1（严重，事实错误）：薄适配器 doExecuteInternal() 中使用未定义变量 `promptVersion`

- **所在位置**：§4.2 薄适配器 CapabilityExecutor 特化管线伪代码，两处调用（TimeoutException 分支和 catch(Exception) 分支）
- **问题描述**：薄适配器 `doExecuteInternal()` 的方法签名参数列表为 `(startTime, request, capabilityId, departmentId, userId, sessionId, callerRole, callerId, visitId, patientId, inputSummary)`，共 12 个参数，不包括 `promptVersion`。但 TimeoutException 和 catch(Exception) 两处分支的 `doDegrade()` 调用均传入了未定义的变量 `promptVersion`，编译无法通过。此问题在 v1 报告中已指出，v18 修订未覆盖薄适配器路径。
- **严重程度**：严重 — 实现者无法直接从伪代码翻译为 Java 代码。
- **改进建议**：将两处 `promptVersion` 替换为 `null` 字面量，与薄适配器语义一致（薄适配器不执行实验分流，promptVersion 恒为 null）。

### 问题2（重要，逻辑矛盾）：薄适配器降级原因使用了字符串字面量而非 DegradationReason 枚举常量

- **所在位置**：§4.2，TimeoutException 分支
- **问题描述**：v18 修订已明确将降级原因规范为 `DegradationReason.STRATEGY_TRIGGERED + ":" + strategy.getClass().getSimpleName()` 的枚举常量引用模式，且完整管线的其他降级路径均遵守此约定。但薄适配器管线中 `doDegrade()` 调用仍然使用字符串字面量 `"TIMEOUT:ThinAdapterTimeout"`，未替换为 `DegradationReason.TIMEOUT + ":ThinAdapterTimeout"`。此问题在 v1 报告中已指出，v18 仅修复了完整管线路径的降级原因，薄适配器路径被遗漏。
- **严重程度**：重要 — 不产生编译错误，但导致运行时期望的降级原因格式与实际不符，影响监控系统按枚举值聚合统计。
- **改进建议**：将 `"TIMEOUT:ThinAdapterTimeout"` 替换为 `DegradationReason.TIMEOUT + ":ThinAdapterTimeout"`。

### 问题3（重要，设计矛盾）：AiPlatformConfig 同时实现 EnvironmentPostProcessor 和 ApplicationContextAware 的生命周期冲突

- **所在位置**：§3.9 AiPlatformConfig 定义；§3.1 Bean 装配策略
- **问题描述**：`AiPlatformConfig` 声明为 `@Configuration @ConfigurationProperties implements EnvironmentPostProcessor, ApplicationContextAware`。`EnvironmentPostProcessor` 在 Spring 上下文创建前由 Spring Boot 直接实例化并调用；`ApplicationContextAware` 仅在 Spring 容器管理的 bean 实例上回调。这两个职责实际由两个不同的实例承担——EPP 实例（非 Spring 管理）处理属性转发，Spring bean 实例处理 @Bean 定义和 @ConfigurationProperties 绑定。设计文档将此呈现为单一对象职责，未说明双实例生命周期。此问题在 v1 报告中已指出，v18 修订未涉及此重构。
- **严重程度**：重要 — 开发者可能尝试在 EPP 实例中注入依赖（`@Autowired` 其他 bean），该操作将因 EPP 实例非 Spring 管理而失败；也可能误认为 EPP 实例上的 @PostConstruct 会被调用。
- **改进建议**：方案 A（推荐）：将 `EnvironmentPostProcessor` 剥离为独立类（如 `AiPlatformEnvironmentPostProcessor`），仅负责 `ai.platform.enabled` → `ai.mock.enabled` 的反向转发。`AiPlatformConfig` 聚焦配置装配职责。方案 B：在 §3.9 中显式说明双实例生命周期机制。

### 问题4（重要，逻辑矛盾）：orTimeout().exceptionally() 降级路径使用原始 request 而非 defensiveCopy

- **所在位置**：§4.1 `AbstractCapabilityExecutor.execute()` 模板方法，`.exceptionally()` 回调行
- **问题描述**：v18 修订已修正防御性拷贝为 `defensiveCopy` 局部变量，降级预检命中路径和 supplyAsync 路径均正确使用了拷贝副本。但 `.exceptionally()` 回调中的超时降级路径仍引用原始 `request`，违背了"所有下游操作使用防御性拷贝"的设计原则。
- **严重程度**：重要 — 虽然在超时场景下 `request` 仅用于日志和指标记录，但这种不一致性会导致代码审查时产生困惑。
- **改进建议**：将 `.exceptionally()` 回调中的 `request` 替换为 `defensiveCopy`，与执行体和其他降级路径保持一致。

### 问题5（一般，完整性不足）：getFallbackPrompt() 的 YAML 配置项在 §9.5 缺失

- **所在位置**：§3.3 PromptTemplateManager 段落 vs §9.5 YAML 配置示例
- **问题描述**：§3.3 承诺兜底 Prompt 通过配置项管理，具体为 `ai.template.fallback.{capabilityId}` 的 YAML 配置格式。v18 修订新增了 `getFallbackPrompt()` 的返回值契约（非空非空字符串、格式一致性），但 §9.5 的 YAML 块中仍然没有任何 `template` 相关配置。实现者无法从设计文档中找到完整的配置结构和默认值。
- **严重程度**：一般 — 实现者可自行推断配置结构，但存在与实际设计意图不一致的风险。
- **改进建议**：在 §9.5 YAML 中补充 `ai.template.fallback` 配置块，至少包含一个能力的兜底 Prompt 配置示例（如 `TRIAGE`），并注明可选/必填与默认行为。

### 问题6（一般，伪代码质量问题）：AiOrchestrator.handle() catch 块中存在未使用的变量

- **所在位置**：§4.1 `AiOrchestrator.handle()` 伪代码
- **问题描述**：catch 块中定义了 `requestAttributes = RequestContextHolder.currentRequestAttributes()`，但接下来的代码并未使用该变量——`extractFromRequestContext()` 方法内部自行调用 `RequestContextHolder.getRequestAttributes()` 获取 HTTP Header 值。该变量定义不仅无实际作用，还与 `extractFromRequestContext()` 的独立获取行为存在语义歧义（读者可能误以为后续会直接操作 `requestAttributes`）。
- **严重程度**：一般 — 不影响可执行性，但反映伪代码维护不一致，对实现者产生细微干扰。
- **改进建议**：删除未使用的 `requestAttributes` 变量定义。此 catch 块运行于 Tomcat 容器线程，直接调用 `extractFromRequestContext()` 即可正确获取 HTTP Header。

### 问题7（新增，重要，逻辑矛盾）：§3.1 文本对薄适配器实验分流行为描述与 §4.2 伪代码不一致

- **所在位置**：§3.1 薄适配器型 CapabilityExecutor 的管线行为段落 vs §4.2 薄适配器伪代码
- **问题描述**：§3.1 文本（"薄适配器管线行为"段落）声称薄适配器"包含"实验分流，并说明"实验分流仅用于 departmentId 提取"。但 §4.2 的薄适配器 `doExecuteInternal()` 伪代码中完全没有 `experimentManager.assign()` 调用，且 departmentId 已通过 `doExtractDepartmentId()` 独立提取。文本描述与伪代码存在实质性矛盾：(1) 文本声称的实验分流行为在伪代码中未实现；(2) "实验分流用于 departmentId 提取"的表述在逻辑上不成立——experiment 分桶目的是确定模型/Prompt 版本分流，与 departmentId 提取无关。
- **严重程度**：重要 — 实现者阅读文本后可能尝试在薄适配器中错误地插入实验分流步骤，徒增复杂度；若仅阅读伪代码则无法发现文本承诺的行为与代码的不一致。
- **改进建议**：删除或修正 §3.1 中"实验分流仅用于 departmentId 提取"的描述，明确薄适配器管线不包含实验分流步骤（Phase 4 服务内部自行处理模型调用和分流），departmentId 通过 `doExtractDepartmentId()` 独立提取，与实验管理器无关。

---

## 需求响应充分度评估

本产出经 18 轮迭代，用户需求的 OOD 核心要素（类图、核心职责、协作关系、关键接口、状态模型）已**全面覆盖**。具体验证如下：

| 需求要素 | 覆盖状态 | 位置 |
|---------|---------|------|
| 类图（UML classDiagram） | ✅ 完整覆盖 30+ 类/接口的继承、组合、依赖关系 | §2.3 |
| 核心职责 | ✅ 每项抽象均有独立的职责段落，明确"做什么"和"不做什么" | 全文 |
| 协作关系 | ✅ 类图、§3 各小节"协作对象"段落、§4 伪代码三层相互印证 | 多处 |
| 关键接口方法签名 | ✅ CapabilityExecutor、LlmChatService、ModelRouter、PromptTemplateManager、ExperimentManager、AiMetricsCollector 等均已定义 | §3 |
| 状态模型 | ✅ PromptTemplate（DRAFT→ACTIVE→DEPRECATED 含回滚）、Experiment（DRAFT→ACTIVE→PAUSED→COMPLETED）、ModelEndpointHealthManager（CONNECTED→DEGRADED→UNAVAILABLE 含完整转换表）、CircuitBreakerDegradationStrategy（CLOSED→OPEN→HALF_OPEN）均已覆盖 | §3.3、§3.4、§3.2、§3.8 |

13 项 AI 能力全覆盖（7 项底座完整管线 + 6 项 Phase 4 薄适配器），能力标识映射表（§3.1）与迁移路径表（§9.1）一一对应。设计风格与 Phase0/Phase1ABD 保持一致（§1.2 显式声明引用）。

**未闭环的迭代历史问题**：经对照迭代历史第 15~18 轮，原累积的严重问题（异步上下文传播、Maven 依赖作用域矛盾、ModelRoute 缺少 parameters 字段、EndpointRateLimiter 限流保护、StructuredChatResult 包装类型等）已在 v18 迭代中得到修复。当前仍开放的问题集中于本报告问题 1~7。

---

## 整体质量评价

产出整体质量很高。经过 18 轮迭代，设计在核心抽象、类图、管线行为契约、错误处理、并发设计、设计决策、迁移路径、测试策略等维度已非常完备。面向编码实现的核心"设备"（类图轮廓、接口方法签名、模板方法伪代码、YAML 配置结构）均已就位，可直接指导编码启动。

本次审查发现 7 个问题中，1 个严重（薄适配器变量未定义，编译阻塞——问题 1）、3 个重要（降级原因枚举对齐、AiPlatformConfig 生命周期文档化不足、exceptionally 路径引用不一致、文本与伪代码矛盾——问题 2/3/4/7）、2 个一般性问题（配置示例缺口、伪代码未用变量——问题 5/6）。主要风险集中在**薄适配器路径的变量缺失**（问题 1）和**AiPlatformConfig 生命周期双实例的文档缺口**（问题 3），建议在下轮迭代中优先修正。

---

## 修订说明（v2）

| 质询意见 | 回应 |
|---------|------|
| **质询要点1（严重）**：v1 报告未评估"需求响应充分度"，6 个问题全部集中在本地技术细节，缺少宏观层面的需求响应评估。 | 已在本报告的"需求响应充分度评估"新增独立章节，逐项验证类图、核心职责、协作关系、关键接口、状态模型的覆盖度，并对照迭代历史追踪问题闭环状态。结论：v18 产出的 OOD 核心要素已全面覆盖用户需求。 |
| **质询要点2（重要）**：问题6中包含行号错乱这一纯校典型的细节，与设计质量无关，违反"不要关注文档校对、统计行数、数行号等与任务描述关系不大的细节"。 | 已采纳。已将问题6中的行号错乱部分删除，仅保留"未使用变量"这一有实质意义的伪代码质量问题。 |
