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

- **问题描述**：§3.1 YAML 配置到 Bean 的装配路径（第 759-769 行）中，YAML 的 `strategies` 列表使用简名 `"timeout"`、`"circuit-breaker"`、`"noop"` 引用策略实现。但设计未说明这些简名如何映射到实际的 Spring Bean——`@Component` 默认生成的 Bean name 为 `timeoutDegradationStrategy`（类名首字母小写），与 `"timeout"` 不匹配。实现者需自行推断需使用 `@Component("timeout")` 显式命名。此缺口在 21 轮内部审议中未被捕获，可能直接导致 YAML 配置到 Bean 查找失败（`getBean("timeout")` 返回 null）。
- **所在位置**：§3.1 第 759-769 行 → `"timeout"` 等引用名 vs Spring 默认 Bean name 推导规则
- **严重程度**：一般
- **改进建议**：在 §3.1 降级策略装配路径的步骤 3 或步骤 4 中补充说明：`@Component("timeout")`、`@Component("circuit-breaker")`、`@Component("noop")` 的显式 Bean name 声明方式；或在 §9.5 YAML 配置示例旁注明 Bean name 匹配约定。

---

### 问题 5（一般 — 完整性不足）`DegradationContext.setDepartmentId()` 在管线伪代码中被调用但未在核心定义中声明

- **问题描述**：§4.1 降级预检伪代码（第 2022 行）调用 `context.setDepartmentId(departmentId)`，将科室标识写入降级判定上下文。但 §3.8 `DegradationContext` 的扩展字段表（第 1769-1776 行）和 §2.3 类图（第 527-539 行）均未列出 `departmentId` 字段或 `setDepartmentId()` 方法。调用点存在但定义缺失，实现者不清楚 `DegradationContext` 是否应包含 `departmentId`。
- **所在位置**：§4.1 第 2022 行 `context.setDepartmentId(departmentId)` vs §3.8 扩展字段表 vs §2.3 类图 `DegradationContext` 节点
- **严重程度**：一般
- **改进建议**：在 §3.8 `DegradationContext` 扩展字段表中补充 `departmentId: String` 字段及对应 setter/getter 声明；同步更新 §2.3 类图。

---

### 问题 6（轻微 — 完整性不足）`LlmChatOptions` 两阶段填充策略在管线伪代码中无对应实现步骤

- **问题描述**：§3.2 定义了详细的「LlmChatOptions 与 ModelRoute.parameters 的两阶段填充策略」（基值填充 + 调用方覆盖），但 §4.1 管线伪代码（第 2080-2084 行）仅展示了 `new LlmChatOptions(modelId: modelRoute.getModelId(), ...)` 的简化写法，未体现从 `modelRoute.getParameters()` Map 提取字段的步骤，也未展示阶段二的调用方覆盖赋值。管线实现者从伪代码中无法得知两阶段策略的实际编码方式。
- **所在位置**：§3.2 第 1270-1276 行文本定义 vs §4.1 第 2080-2084 行伪代码 `LlmChatOptions` 构造
- **严重程度**：轻微
- **改进建议**：在 §4.1 管线伪代码中的 `LlmChatOptions` 构造处展开为两阶段显式步骤：先 `options = LlmChatOptions.fromParameters(modelRoute.getParameters())` 完成基值填充，再 `options.setModelId(assignment.getTargetModelId())`（若需要覆盖）完成调用方覆盖。

---

## 整体质量评价

产出经过 21 轮内部审议，在技术可行性、接口设计、组件协作、异常处理等方面已达到较高成熟度。本审查发现的问题主要集中在跨章节一致性（超时值、线程池配置值的矛盾）以及文本承诺与伪代码实现之间的缺口（非 HTTP 回退路径、两阶段参数合并）。这些问题不影响总体架构的正确性，但会在实现阶段引发歧义或 Bug，建议在下一轮修订中修复。

DIAG_WRITTEN:C:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606271627_phase5_pkgG_ood/b_v21_diag_v1.md
主Agent请勿阅读产出文件内容，直接将路径转发给相关方。
