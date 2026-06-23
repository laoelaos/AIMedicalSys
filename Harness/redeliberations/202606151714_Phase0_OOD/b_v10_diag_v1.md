# 质量审查报告 — v10

审查对象：`a_v10_copy_from_v9.md`（Phase 0 OOD 设计方案）
审查视角：需求响应充分度、整体深度与完整性、逻辑一致性
审查前提：内部审议已覆盖技术可行性维度，本审查不重复验证

---

## 问题 1：AiService 方法返回类型不明确（严重）

**问题描述**：8.2 节「AI 能力方法清单」表格的"输出 DTO"列仅列出 `TriageResponse`、`DiagnosisResponse` 等纯业务 DTO，但 3.4 节定义了 `AiResult<T>`（包含 `success`、`data`、`degraded`、`fallbackReason`），4.3 节明确声明"AI 调用结果统一由 `AiResult<T>` 包装"。产出未明确以下关键设计决策：

- AiService 的 13 个方法返回值是 `AiResult<T>` 还是裸 DTO？
- 若返回 `AiResult<T>`，8.2 节表格的"输出 DTO"列应更正为 `AiResult<TriageResponse>` 等形式；
- 若返回裸 DTO，则 `degraded` 等调用状态无法传递，4.3 节的 `AiResult` 包装声明与之矛盾。

**所在位置**：3.4 节 `AiService` 职责描述 vs 8.2 节方法清单表格 vs 4.3 节 AI 能力调用契约

**严重程度**：严重

**改进建议**：
- 确定统一策略：建议 AiService 方法统一返回 `AiResult<T>`（如 `AiResult<TriageResponse> triage(TriageRequest request)`），8.2 节表格输出 DTO 列同步更新为 `AiResult<TriageResponse>` 形式；
- 在 8.2 节方法清单上方增加方法签名示例，显式声明返回类型模式；
- 若坚持返回裸 DTO，需在 4.3 节删除"统一由 AiResult<T> 包装"的声明并由其他机制（如异常）传递降级状态，但当前文档无此机制的描述。

---

## 问题 2：common-module 接口门面原则无法编译期强制（严重）

**问题描述**：8.4 节「接口门面」模式要求"其他模块的 POM 仅依赖含接口的模块，不依赖含实现的模块"，但 common-module 是一个**单一 Maven 模块**，其 `api/`（接口）、`service/`（实现）、`entity/`、`repository/` 全部编译在同一个 artifact 中。业务模块 POM 依赖 common-module 后，可直接访问其 Service 实现类、Repository 和 Entity，绕开门面接口约束。这与 ai-api/ai-impl 的子模块拆分策略（2.2 节）形成了架构不一致——AI 能力做到了编译期强制隔离，权限模块反而做不到。

**所在位置**：2.2 节模块依赖规则、8.4 节门面接口模式描述 vs 2.1 节目录布局（common-module 为单一模块）

**严重程度**：严重

**改进建议**：方案 A（推荐）：将 common-module 拆分为 `common-module-api`（仅接口 + DTO）和 `common-module-impl`（实现），业务模块仅依赖前者，application 引入后者；方案 B：若坚持单模块，则需在 8.4 节删除"POM 仅依赖含接口的模块"的表述，改为在 Code Review 层面约束，同时补充 ArchUnit 测试规则确保编译后仍可检查。

---

## 问题 3：FallbackAiService 对 DegradationStrategy 的持有/组合方式未定义（一般）

**问题描述**：3.4 节「降级策略框架」定义了 `DegradationStrategy` 接口和 `TimeoutDegradationStrategy` 实现，但 FallbackAiService 如何获取 DegradationStrategy 实例完全未定义。具体缺失：

- `DegradationStrategy` 由谁注入 FallbackAiService？构造器注入还是字段注入？
- 单一策略还是支持策略链？
- `TimeoutDegradationStrategy` 在 Phase 0 是否自动注册为 Bean？
- 多个 DegradationStrategy 实现时的选择/优先级机制？

**所在位置**：3.4 节「降级策略框架」段落

**严重程度**：一般

**改进建议**：
- 明确 FallbackAiService 通过构造器注入 `DegradationStrategy`（或 `List<DegradationStrategy>`）；
- 指定 Phase 0 默认注册一个返回 false 的 NoOpDegradationStrategy，TimeoutDegradationStrategy 归 Phase 2+；
- 补充伪代码骨架或构造器签名。

---

## 问题 4：ScheduleItem.date 字段类型与 ScheduleRequest 不一致（轻微）

**问题描述**：ScheduleRequest（8.2 节）使用 `LocalDate startDate` / `LocalDate endDate` 表示日期，但同一个排班领域的 ScheduleItem（响应 DTO）将日期定义为 `String date`。同领域的前后日期类型不一致，前端开发者无法确定应使用字符串格式还是 LocalDate 格式进行处理。

**所在位置**：8.2 节 ScheduleRequest DTO（line 753-757）vs ScheduleItem DTO（line 762-766）

**严重程度**：轻微

**改进建议**：将 `ScheduleItem.date` 类型统一为 `LocalDate`，与请求端的日期类型保持一致；若因序列化格式原因必须使用 String，需注释明确格式如 `yyyy-MM-dd` 并在 ScheduleResponse 层面说明转换责任方。

---

## 问题 5：前端三端占位首页结构未定义（一般）

**问题描述**：需求明确要求"三端前端可一键启动到占位首页"（Phase 0 验收标准之一）。文档定义了前端 monorepo 目录结构、共享包依赖关系、Vite 代理配置，但未定义各 `apps/*` 应用的入口文件结构。一个 Vite + Vue 3 应用至少需要 `index.html`、`src/main.ts`、`src/App.vue` 三个入口文件才能启动，当前文档对这些文件的存在和内容均未说明。开发者拿到文档后仍需自行推断前端占位页面的完整实现方案。

**所在位置**：2.4 节前端模块划分（只有目录结构，无入口文件定义）

**严重程度**：一般

**改进建议**：在 2.4 节末尾补充三端 app 的最简入口文件结构说明，例如：各 `apps/*` 包含 `index.html`（挂载点 `#app`）、`src/main.ts`（createApp + router）、`src/App.vue`（占位页面），并注明 Phase 0 的占位页面仅渲染"系统名称 + 占位提示"文本即可。

---

## 问题 6：父 POM 基础结构未给出（轻微）

**问题描述**：文档给出了 `integration/pom.xml` 的完整骨架，但未给出根 POM（父 POM，`backend/pom.xml`）的基础结构。父 POM 是 Maven 多模块项目的核心骨架文件，需至少包含 `<groupId>`、`<artifactId>`、`<version>`、`<packaging>pom</packaging>`、`<modules>` 列表和 `<dependencyManagement>` 段。虽然各章节分散描述了依赖版本管理策略，但无一份完整的父 POM 结构参考。

**所在位置**：第 2 节模块划分、第 10 节 CI 占位

**严重程度**：轻微

**改进建议**：在 2.1 节目录布局之后或 2.2 节依赖规则之前，补充根 POM 的核心结构骨架（至少包含 modules 列表和 dependencyManagement 声明）。

---

## 问题 7：`@Valid` 在分页参数上的校验生效条件说明不完整（轻微）

**问题描述**：3.1 节要求"所有分页 Controller 参数需标注 `@Valid` 以触发校验注解"，但 `PageQuery` 作为 GET 请求的查询参数，通常是 `@ModelAttribute` 绑定而非 `@RequestBody`。`@Valid` 在 `@ModelAttribute` 上确实生效，但 Spring Boot 需要同时确保 `org.springframework.boot:spring-boot-starter-validation` 在类路径上。2.2 节已声明业务模块引入 `spring-boot-starter-validation`，但未说明仅对 `@RequestBody` 场景生效的常见误解。此外，`PageQuery.sort` 字段的格式（如 `"createdAt,desc"`）未定义类型和格式说明。

**所在位置**：3.1 节 PageQuery 字段描述

**严重程度**：轻微

**改进建议**：
- 在 3.1 节补充说明 `@Valid` 对 `@ModelAttribute` 绑定和 `@RequestBody` 绑定均有效，前提是 spring-boot-starter-validation 在类路径上；
- 为 `PageQuery.sort` 补充字段类型（`String`）和格式说明（如 `"fieldName,direction"`，direction 为 asc/desc）。

---

## 整体评价

文档经过 9 轮迭代修正，在技术可行性、内部一致性上已达到较高水准。当前仍需解决的 7 个问题集中在：**返回值契约的明确性**（问题 1）、**架构一致性与编译期强制保障**（问题 2）、以及**完整落地所需的细节补充**（问题 3-7）。其中问题 1 和问题 2 为严重级别，需要在下一轮修订中优先处理。
