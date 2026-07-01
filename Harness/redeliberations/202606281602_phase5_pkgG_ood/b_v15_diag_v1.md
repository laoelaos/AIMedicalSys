# 质量审查诊断报告 — v15

## 审查范围

审查文件：`a_v15_copy_from_v14.md`（Phase 5 包 G OOD 设计文档，第 15 次迭代）
审查视角：实际落地可实施性、设计完整性、逻辑一致性
维度侧重：需求响应充分度、深度与完整性、事实/逻辑一致性（避免重复验证内部审议已覆盖的技术可行性维度）

---

## 发现的问题

### 问题 1：[严重] `parseTimeoutConfig`/`parseTimeoutDefault` 字段和 @Bean 均未定义，导致 §4.1 伪代码编译不可行

**问题描述**：§4.1 第 3163 行伪代码引用了 `parseTimeoutConfig.getOrDefault(capabilityId, parseTimeoutDefault)`，但：

1. **§3.9 `AiPlatformConfig` 缺少对应的 `@Bean("parseTimeoutConfig")` 定义** — §3.9 第 2603-2614 行定义了 `capabilityTimeoutConfig` 和 `thinAdapterPerCapabilityConfig` 两个 `Map<String, Duration>` 类型的 @Bean，但未定义同类型的 `parseTimeoutConfig` @Bean。YAML 中已定义的 `execution.timeout.parse.per-capability`（§9.5 第 3847-3849 行）没有对应的装配代码。
2. **§2.3 类图和 §3.1 构造器均未声明 `parseTimeoutConfig` 和 `parseTimeoutDefault` 字段** — 这两个符号在 `AbstractCapabilityExecutor` 的任何结构定义中都不存在，实现者将面临编译错误。
3. **§3.9 第 2550 行 `AiExecutionProperties` 的绑定范围声明遗漏 `timeout.parse.*`** — 配置属性类的绑定范围表中只列出 `timeout.per-capability` 和 `timeout.thin-adapter-default`，未包含 `timeout.parse.default` 和 `timeout.parse.per-capability`。
4. **§3.9 第 2534-2539 行热加载机制表未包含 `execution.timeout.parse.*`** — §9.5 第 3846 行注释声称 parse 超时"与 capabilityTimeoutConfig 共享同一自定义定时刷新机制"，但热加载机制表未列出此配置组，实现者无法确定其热加载行为。
5. **§3.2 第 1653 行的注入方式描述与 §4.1 伪代码不一致** — §3.2 称"通过 `@Value` 注入默认值"，而 §4.1 使用 Map 查找模式 (`parseTimeoutConfig.getOrDefault`)，两种方式涉及的 Bean 装配和配置绑定机制完全不同，实现者无法确定应采用哪种方案。

**所在位置**：§4.1 第 3163 行；§3.9 第 2603-2614 行、第 2534-2539 行、第 2550 行；§3.2 第 1653 行；§9.5 第 3846-3849 行

**严重程度**：严重 — 伪代码引用了未定义的结构体，直接导致编码阶段编译失败（`parseTimeoutConfig` 和 `parseTimeoutDefault` 未定义），且修复涉及多处联动修改（添加 @Bean、更新类图、更新构造器、统一注入方式）。

**改进建议**：
- 方案 A：在 §3.9 `AiPlatformConfig` 中新增 `@Bean("parseTimeoutConfig")` 方法，从 `AiExecutionProperties` 的新增嵌套字段 `timeout.parse.perCapability` 绑定；在 `AbstractCapabilityExecutor` 类图和构造器中补充 `parseTimeoutConfig` 和 `parseTimeoutDefault` 字段；在 §3.9 热加载表中补充 `execution.timeout.parse.*` 行；统一 §3.2 和 §4.1 的注入方式描述为 Map 查找模式。
- 方案 B（简化）：若仅需单默认值而不需要按能力差异化，将 §4.1 的 `parseTimeoutConfig.getOrDefault()` 简化为 `parseTimeoutDefault`（`@Value` 注入），删除 YAML 中的 `parse.per-capability` 配置块，保持与 §3.2 描述一致。

---

### 问题 2：[重要] 文档头部声明"历史修订说明已剥离归档"与正文结构矛盾

**问题描述**：文档头部（第 3 行）变更摘要明确声明"历史修订说明已剥离归档（见 `design_evolution_log.md`）"，但文档末尾（行 4044-4138）仍完整保留 `## 修订说明（v7）` 至 `## 修订说明（v15）` 共 9 个修订说明表。头部的"已剥离"声明与正文事实直接矛盾。

**所在位置**：文档头部第 3 行 vs 行 4044-4138

**严重程度**：重要 — 实现者或后续审阅者对文档状态产生疑惑：这到底是一份"最终稳定版本"还是一份仍附着大量修订说明的过渡版本？若视为最终版本，应确实剥离；若保留修订说明作为归档，头部应修正声明。

**改进建议**：
- 方案 A：将尾部所有 `## 修订说明（v7）~v15` 剥离至 `design_evolution_log.md`，保持正文为纯净的最终设计状态。
- 方案 B：修正头部声明，改为"历史修订说明（v2~v6）已剥离归档，v7~v15 修订说明保留于尾部作为变更追踪参考"。

---

### 问题 3：[中等] §4.1 AiOrchestrator.handle() 伪代码行号跳跃且重复

**问题描述**：§4.1 第 2940-2946 行伪代码的行号序列违反逻辑顺序：
- 第 36 行：`callerRole = RequestContextUtils.extractCallerRole()`
- 第 37 行：`callerId = RequestContextUtils.extractCallerId()`
- **第 40-42 行**：`metricsCollector.record(...)` 带参数换行（行号跳跃 37→40）
- **第 37 行**：`slidingWindowMetricsStore.recordFailure(capabilityId)`（行号 37 重复）
- **第 38 行**：`return CompletableFuture.completedFuture(...)`（行号 38 重复）

行号 37-38 出现了两次，且第 40-42 行出现在第 36-37 行之后、第 37-38 行之前，破坏了伪代码行号的连续性和唯一性。

**所在位置**：§4.1 第 2942-2946 行

**严重程度**：中等 — 不影响代码正确性，但行号重复和跳跃会导致实现者阅读伪代码时产生混淆，尤其在按行号引用或讨论此段逻辑时。

**改进建议**：修正为连续行号：36（callerRole）、37（callerId）、38（metricsCollector.record）、39（slidingWindowMetricsStore.recordFailure）、40（return...）。

---

### 问题 4：[一般] 薄适配器 DTO 工作量估算与过渡策略存在分歧

**问题描述**：§3.5 DTO 改造工作量概览表（行 2166-2171）中，6 项薄适配器 DTO 的"新增字段数"列均计入"4 AiRequestBase 继承字段"（如 `DiagnosisRequest: 1 + 4 AiRequestBase 继承字段`）。但备注栏同时标注"由 Phase 4 模块自行决定是否继承 AiRequestBase"，且 §3.1 过渡策略（行 1037-1043）明确指出 6 项薄适配器 DTO 暂不继承 AiRequestBase、通过独立提取方法获取公共字段。

这意味着：
- 若 Phase 4 按过渡策略暂不继承，工作量表中的 4 个继承字段不需新增，实际工作量少于估算
- 若 Phase 4 后期选择继承，则 4 个字段需在 Phase 4 后续迭代中补齐，不属 Phase 5 底座切流的首批工作量
- 估算表中将这四个字段计入"新增字段数"但不计入底座承担范围（行 2173），容易造成交接方的预期错位

**所在位置**：§3.5 DTO 改造工作量概览表（行 2166-2171）

**严重程度**：一般 — 不影响设计正确性，但可能导致 Phase 4 与 Phase 5 团队之间对工作范围的理解不一致。

**改进建议**：
- 在薄适配器 DTO 行的备注栏中明确标注"4 AiRequestBase 继承字段仅在 Phase 4 模块决定继承时生效，底座切流初期不纳入改造范围"
- 或新增独立列"Phase 5 底座承担"，用 ✓/✗ 标记明确每一行的责任归属

---

## 整体质量评价

该产出经过 15 轮审议迭代，整体设计深度和完整性极高（4138 行），覆盖架构总览、模块划分、类图、13 个核心抽象的详细设计、关键行为契约伪代码、错误处理、并发设计、设计决策、依赖清单、迁移路径、跨包协作和测试策略等 OOD 核心要素。需求响应充分——Phase 5 包 G 的完整 OOD 设计已按要求完成，风格与 Phase0/Phase1ABD 保持一致。

**核心短板**：v15 新增的 parse() 超时可配置化改造存在"伪代码先于结构定义"的问题——YAML 配置和 §4.1 伪代码已完成，但对应的 @Bean 定义、类图字段、构造器参数、配置属性类绑定声明均未联动更新。这是一个典型的"局部修改、全局遗漏"问题，必须在编码前修复。

剩余问题（伪代码行号错乱、修订说明未剥离、工作量争议）均不改变设计的正确性，但影响文档的可信度和协作效率，建议一并清理。
