# 质量审查报告 — v2 设计文档

## 审查概述

审查范围：Phase 0 最小化骨架 OOD 设计方案 v2（a_v2_design_v1.md）
审查视角：需求响应充分度、事实与逻辑正确性、深度与完整性（侧重内部审议未覆盖的维度）
审查依据：用户需求文档（requirement.md）、迭代历史记录（iteration_history.md）

---

## 发现的问题

### 问题 1：[严重] `ai.mock.enabled` 未配置时 Phase 0 无 AiService Bean 可用

**所在位置**：3.4 节「Bean 装配策略」及「装配条件汇总表」

**问题描述**：`MockAiService` 标注为 `@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "true", matchIfMissing = false)`。当 `ai.mock.enabled` 未出现在任何配置文件中时，`matchIfMissing=false` 使条件求值为 false，MockAiService 不被创建。Phase 0 尚无真实 AiService 实现（标注 `@ConditionalOnMissingBean` 也无 Bean 可替补），因此最终**没有任何 AiService Bean 注册到容器**。任何注入 `AiService` 的业务模块在启动时均会因找不到 Bean 而失败。

装配条件汇总表第三行"未配置 → MockAiService（matchIfMissing=false）"的表述存在误导：`matchIfMissing=false` 的含义恰是"属性缺失时不创建"，但表格将该行列为"MockAiService"并标注"等价于 false，需显式启用"，读者容易误解为"未配置时用 Mock"。且第一行的"（默认）"标签与注解实际行为不吻合——注解的默认行为是"不创建"，而非"true"。

**改进建议**：在 `application.yml` 中统一显式设置 `ai.mock.enabled: true` 作为全局默认值，消除对 `matchIfMissing` 的依赖。或改用 `matchIfMissing = true` 使缺省即启用 Mock。同时修正装配条件汇总表使其准确反映注解语义。

---

### 问题 2：[严重] AI 方法输入/输出使用中文自然语言描述，缺乏具体 DTO 类型名

**所在位置**：8.2 节「AI 能力方法清单」表格（第 3-4 列）

**问题描述**：需求要求设计"可直接指导编码实现"。但 8.2 节为 13 个 AI 方法定义的输入/输出列均为中文自然语言描述（如"患者主诉"、"推荐科室、医生列表"、"检查原始数据"等），而非具体的 DTO 类名。文档 3.4 节已定义 `dto/` 子包，但未给出任何一个具体的 DTO 类型名。

这意味着：13 个 AI 方法的输入/输出 DTO 没有明确的类型标识，不同开发者会自行创建不同的 DTO 结构（命名、字段、层次各异），造成平行开发的一致性问题；前端团队也无法根据类型定义生成类型安全的 API 调用代码。

**改进建议**：为每个 AI 方法的输入和输出指定具体的 DTO 类名（如 `TriageRequest`/`TriageResponse`、`PrescriptionCheckRequest`/`PrescriptionCheckResponse` 等），并在 3.4 节或 8.2 节以类图或伪代码骨架形式定义各 DTO 的核心字段结构。Phase 0 可省略字段级完整定义，但至少需锁定 DTO 类型名及其结构骨架。

---

### 问题 3：[严重] 事实错误——"配置加载失败"无法被 GlobalExceptionHandler 捕获

**所在位置**：5.1 节错误分类表，"配置加载失败"行

**问题描述**：该行描述为"启动时抛出 `IllegalStateException`，运行时由 `GlobalExceptionHandler` 统一捕获 → `Result`"。这是事实性错误。`GlobalExceptionHandler` 由 `@ControllerAdvice` 驱动，仅在 Spring MVC **请求处理生命周期**内生效。配置加载失败发生在 `ApplicationContext` refresh 阶段（启动阶段），此时 DispatcherServlet 尚未初始化，`GlobalExceptionHandler` 并未注册到容器。启动阶段的任何异常都会导致应用启动失败（`ApplicationContext` refresh 异常），不会被 `GlobalExceptionHandler` 捕获，也无法返回 `Result` 格式的 HTTP 响应。

**改进建议**：删除该行中"运行时由 GlobalExceptionHandler 统一捕获 → Result"的描述，替换为"应用启动失败，由 Spring Boot 的失败分析器（FailureAnalyzer）输出诊断信息"。或将此类错误从运行时异常分类中移除，标注为"启动期异常，不可恢复"。

---

### 问题 4：[中等] 前端 `ui-core` 包出现在目录树中但完全未定义

**所在位置**：2.1 节目录布局（`packages/ui-core/`）、2.4 节前端模块划分

**问题描述**：2.1 节目录布局定义了 `packages/ui-core/`（注释"共享 UI 组件库"），但 2.4 节前端模块划分仅描述了 `packages/shared/` 的三个子目录（api/types/utils），完全未提及 `ui-core` 的存在。开发者在落地时无法知晓：
- `ui-core` 与 `shared` 的依赖关系（ui-core 是否依赖 shared？shared 是否依赖 ui-core？）
- `ui-core` 包含哪些内容（UI 组件、布局、主题还是样式变量？）
- 三端应用如何引用 ui-core（通过 workspace 依赖？需要额外配置？）

**改进建议**：在 2.4 节补充 `packages/ui-core/` 的定义，明确其内容职责、与 shared 包的依赖关系，以及三端应用的引用方式。

---

### 问题 5：[中等] SecurityConfig 模块归属存在内部矛盾

**所在位置**：4.5 节 `SecurityConfig 设计骨架`（"归属 common.config 或 common-module.config"）与 2.1 节目录布局（SecurityConfig 列于 common/config 下）

**问题描述**：4.5 节使用"或"字保留了 SecurityConfig 归属的歧义（common.config **或** common-module.config），但 2.1 节目录布局已明确将其置于 `common/config/` 下。两处描述不一致。

此外，如果 SecurityConfig 放置于 `common` 模块，则 common 必须引入 `spring-boot-starter-security` 依赖——这与 common 模块"零依赖（仅依赖 Spring Boot Starter 基础库）"的定位存在张力（spring-security 是否属于"基础库"未声明）。如放置在 `common-module` 则无此问题，因为 common-module 本就是业务级共享模块。

**改进建议**：统一 SecurityConfig 的归属决策（推荐 `common.config` 以保持框架配置集中），并在 common 模块的依赖描述中明确标注 spring-boot-starter-security 为其必需依赖。同时消除 4.5 节与 2.1 节间的歧义。

---

### 问题 6：[中等] CI 流水线未体现模块依赖构建顺序

**所在位置**：第 10 节「CI 占位」

**问题描述**：需求明确要求"构建流水线中的模块依赖关系"作为架构层面的设计维度。当前 CI 流水线描述为简单的线性步骤（checkout → mvn compile → mvn test → 构建前端 → 归档制品），未体现 9.2 节所述的多模块构建依赖顺序（common/ai-api 优先构建 → 业务模块 → ai-impl → application 最后构建）。`mvn compile` 确实能自动处理 Maven 模块构建顺序，但 CI 设计应明确各阶段的模块集合以及并行构建策略，否则在后续扩展模块数量后无法有效优化流水线构建时间。

**改进建议**：在 CI 流水线中标注各 Maven 模块的构建阶段归属，体现 common/ai-api 等基础模块优先编译、业务模块并行编译、application 统一聚合的分阶段策略。

---

### 问题 7：[轻微] `DegradationStrategy.fallback` 输入参数类型未定义

**所在位置**：3.4 节降级策略框架

**问题描述**：`DegradationStrategy` 接口定义了 `fallback(input)` 方法，但未指定 `input` 参数的类型。开发者无法直接实现该接口，因为不知道方法是泛型化的（如 `<T, R> R fallback(T input)`）还是固定类型的。这直接影响降级策略的可实现性。

**改进建议**：明确 `fallback` 方法的泛型签名，如 `<T, R> R fallback(T input)`，或在接口级声明类型参数。

---

## 整体质量评价

设计文档在大部分维度上质量较高，尤其在模块依赖关系、接口契约框架、权限模型设计等方面经过两轮修订已达到较好深度。核心问题集中在三方面：

1. **部分设计断言存在事实性错误**（问题 3：GlobalExceptionHandler 不能处理启动期异常），这些问题表明某些错误处理场景的推演停留在"理想态"而未经过运行时生命周期验证。
2. **关键细节的"最后一公里"未打通**（问题 1：Bean 装配在缺省配置下失效；问题 2：AI 方法无具体 DTO 类型），直接影响了"可直接指导编码实现"的承诺。
3. **文档内部存在一处事实矛盾**（问题 5）和**一处定义遗漏**（问题 4），虽不致命但增加了落地歧义。

建议优先修复前三个问题（严重级别），其余问题作为下一步迭代改进项。
