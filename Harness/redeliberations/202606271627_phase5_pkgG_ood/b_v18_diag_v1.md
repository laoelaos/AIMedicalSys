# v18 质量审查报告

## 审查范围

- **产出**：Phase 5 包 G — AI 进阶底座 架构级 OOD 设计方案（v18）
- **审查视角**：需求响应充分度、事实准确性、逻辑一致性、完整性（侧重内部审议未充分覆盖的维度）
- **审查方式**：结合代码库中对 `AiService.java`、`AiResult.java`、`DegradationStrategy.java`、`DegradationContext.java`、`FallbackAiService.java`、`NoOpDegradationStrategy.java` 等已有文件的验证

---

## 发现的问题

### 问题1（严重，事实错误）：薄适配器 doExecuteInternal() 中使用未定义变量 `promptVersion`

- **所在位置**：§4.2 薄适配器 CapabilityExecutor 特化管线伪代码，line 1991
- **问题描述**：薄适配器 `doExecuteInternal()` 的方法签名（line 1974）参数列表为 `(startTime, request, capabilityId, departmentId, userId, sessionId, callerRole, callerId, visitId, patientId, inputSummary)`，共 12 个参数，不包括 `promptVersion`。但 line 1991 调用 `doDegrade()` 时传入了第 13 个参数 `promptVersion`，该变量在当前作用域中未定义。编译无法通过。
- **根因分析**：薄适配器管线不执行实验分流（`ExperimentManager.assign()`），因此 `promptVersion` 恒为 null。此前 v12~v17 的迭代聚焦于完整管线的 promptVersion 传递路径，薄适配器路径被遗漏。此外，`doDegrade()` 在 line 1991 的调用还使用了字符串字面量 `"TIMEOUT:ThinAdapterTimeout"`（见问题2）。
- **严重程度**：严重 — 实现者无法直接从伪代码翻译为 Java 代码。
- **改进建议**：
  1. 在薄适配器 `doExecuteInternal()` 入口处添加 `promptVersion = null` 显式声明，或
  2. 在 line 1991 的 `doDegrade()` 调用中将 `promptVersion` 替换为 `null` 字面量，与零底座能力的薄适配器语义一致。

### 问题2（重要，逻辑矛盾）：薄适配器降级原因使用了字符串字面量而非 DegradationReason 枚举常量

- **所在位置**：§4.2，line 1991
- **问题描述**：v18 修订说明（line 2686）已明确将降级原因规范为 `DegradationReason.STRATEGY_TRIGGERED + ":" + strategy.getClass().getSimpleName()` 的枚举常量引用模式，且 §4.1 完整管线的其他降级路径均遵守此约定。但 §4.2 薄适配器管线中 `doDegrade()` 调用仍然使用字符串字面量 `"TIMEOUT:ThinAdapterTimeout"`，未替换为 `DegradationReason.TIMEOUT + ":ThinAdapterTimeout"`。
- **严重程度**：重要 — 虽然不产生编译错误，但会导致运行时期望的降级原因格式与实际不符，影响监控系统按枚举值聚合统计。
- **改进建议**：将 `"TIMEOUT:ThinAdapterTimeout"` 替换为 `DegradationReason.TIMEOUT + ":ThinAdapterTimeout"`。

### 问题3（重要，设计矛盾）：AiPlatformConfig 同时实现 EnvironmentPostProcessor 和 ApplicationContextAware 的生命周期冲突

- **所在位置**：§3.9 AiPlatformConfig 定义；§3.1 Bean 装配策略
- **问题描述**：`AiPlatformConfig` 声明为 `@Configuration @ConfigurationProperties implements EnvironmentPostProcessor, ApplicationContextAware`。`EnvironmentPostProcessor` 在 Spring 上下文创建前由 Spring Boot 直接实例化并调用；`ApplicationContextAware` 仅在 Spring 容器管理的 bean 实例上回调。这两个职责实际由两个不同的实例承担——EPP 实例（非 Spring 管理）处理属性转发，Spring bean 实例处理 @Bean 定义和 @ConfigurationProperties 绑定。设计文档将此呈现为单一对象职责，未说明双实例生命周期。
- **严重程度**：重要 — 开发者可能尝试在 EPP 实例中注入依赖（如 `@Autowired` 其他 bean），该操作将因 EPP 实例非 Spring 管理而失败；也可能误认为 EPP 实例上的 @PostConstruct 会被调用。当前代码可工作但原因隐含，可维护性差。
- **改进建议**：方案 A（推荐）：将 `EnvironmentPostProcessor` 剥离为独立类（如 `AiPlatformEnvironmentPostProcessor`），仅负责 `ai.platform.enabled` → `ai.mock.enabled` 的反向转发。`AiPlatformConfig` 聚焦配置装配职责，不再承担 EPP 角色。方案 B：在 §3.9 中显式说明双实例生命周期机制，包括谁创建哪个实例、各自的职责边界和限制，以及为什么不能在两实例间共享状态。

### 问题4（重要，逻辑矛盾）：orTimeout().exceptionally() 降级路径使用原始 request 而非 defensiveCopy

- **所在位置**：§4.1 `AbstractCapabilityExecutor.execute()` 模板方法，line 1858
- **问题描述**：v18 修订说明（line 2683）已修正防御性拷贝为 `defensiveCopy` 变量，后续降级预检命中路径（line 1844，使用 `defensiveCopy`）和 supplyAsync 路径（line 1848，传入 `defensiveCopy`）均正确使用了拷贝副本。但 `.exceptionally()` 回调中的超时降级路径（line 1858：`return doDegrade(startTime, ..., request, ...)`）仍引用了原始 `request`，违背了"所有下游操作使用防御性拷贝"的设计原则。
- **严重程度**：重要 — 虽然在超时场景下 `request` 仅用于日志和指标记录（不影响数据安全性），但这种不一致性会导致代码审查时产生困惑，且未来修改 `.exceptionally()` 路径的操作可能因使用了原始引用而引入安全隐患。
- **改进建议**：将 line 1858 的 `request` 替换为 `defensiveCopy`，与执行体和其他降级路径保持一致。

### 问题5（一般，完整性不足）：getFallbackPrompt() 的 YAML 配置项在 §9.5 缺失

- **所在位置**：§3.3 PromptTemplateManager 段落（line ~1274-1275）vs §9.5 YAML 配置示例
- **问题描述**：§3.3 承诺兜底 Prompt 通过配置项管理，具体为 `ai.template.fallback.{capabilityId}` 的 YAML 配置格式。但 §9.5 的 YAML 块中没有任何 `template` 相关配置，也没有 `ai.template.fallback` 的配置占位。实现者无法从设计文档中找到完整的配置结构和默认值。
- **严重程度**：一般 — 实现者可以自行推导配置结构，但存在与实际设计意图不一致的风险。
- **改进建议**：在 §9.5 YAML 中补充 `ai.template` 配置块，至少包含一个能力的兜底 Prompt 配置示例（如 `TRIAGE`），并注明可选/必填与默认行为。

### 问题6（一般，伪代码质量问题）：AiOrchestrator.handle() catch 块中存在未使用的变量及行号错乱

- **所在位置**：§4.1 `AiOrchestrator.handle()` 伪代码，line 1803、line 1809
- **问题描述**：两个问题：(1) line 1803 定义了 `requestAttributes = RequestContextHolder.currentRequestAttributes()`，但接下来的 line 1804~1807 并未使用该变量，`extractFromRequestContext()` 方法内部自行调用 `RequestContextHolder.getRequestAttributes()`；(2) catch 块注释中的行号从 else 分支的 line 28 跳到 line 26（line 1809），与前面 line 1801~1808 的行号体系冲突。
- **严重程度**：一般 — 不影响可执行性，但反映伪代码维护不一致，对实现者产生细微干扰。
- **改进建议**：(1) 删除 line 1803 未使用的 `requestAttributes` 变量定义；(2) 统一全文伪代码的行号标注，或移除行号标注（仅作为伪代码无需行号，当前设计文档中多数伪代码无行号）。

---

## 整体质量评价

产出整体质量很高。经过 18 轮迭代，设计在核心抽象（§1.3）、类图（§2.3）、管线行为契约（§4）、错误处理（§5）、并发设计（§6）、设计决策（§7）、迁移路径（§9）、测试策略（§11）等维度已非常完备。本次审查发现的 6 个问题中，1 个严重但局部（薄适配器变量未定义）、3 个重要（但也属于局部矛盾或遗漏）、2 个一般性问题，不构成对整体设计方向的质疑。v18 的主管线设计（7 项底座能力的完整 CapabilityExecutor 执行流程）逻辑清晰、接口契约明确、异常场景全面，可以直接指导实现。主要风险集中在薄适配器路径（问题1 和 问题2）和 AiPlatformConfig 生命周期文档化不足（问题3），建议在下轮迭代中优先修正。
