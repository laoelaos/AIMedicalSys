根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 问题 1（严重）：AiService 方法返回类型不明确
8.2 节「AI 能力方法清单」表格的"输出 DTO"列仅列出 `TriageResponse`、`DiagnosisResponse` 等纯业务 DTO，但 3.4 节定义了 `AiResult<T>`（包含 `success`、`data`、`degraded`、`fallbackReason`），4.3 节明确声明"AI 调用结果统一由 `AiResult<T>` 包装"。产出未明确 AiService 的 13 个方法返回值是 `AiResult<T>` 还是裸 DTO。建议统一返回 `AiResult<T>`（如 `AiResult<TriageResponse> triage(TriageRequest request)`），8.2 节表格输出 DTO 列同步更新为 `AiResult<TriageResponse>` 形式，并在方法清单上方增加方法签名示例显式声明返回类型模式。

### 问题 2（严重）：common-module 接口门面原则无法编译期强制
8.4 节「接口门面」模式要求"其他模块的 POM 仅依赖含接口的模块，不依赖含实现的模块"，但 common-module 是一个**单一 Maven 模块**，其 `api/`、`service/`、`entity/`、`repository/` 全部编译在同一个 artifact 中。业务模块 POM 依赖 common-module 后，可直接访问其 Service 实现类、Repository 和 Entity，绕开门面接口约束。这与 ai-api/ai-impl 的子模块拆分策略形成了架构不一致。推荐方案 A：将 common-module 拆分为 `common-module-api`（仅接口 + DTO）和 `common-module-impl`（实现），业务模块仅依赖前者。方案 B：若坚持单模块，则需在 8.4 节删除"POM 仅依赖含接口的模块"的表述，改为在 Code Review 层面约束，同时补充 ArchUnit 测试规则。

### 问题 3（一般）：FallbackAiService 对 DegradationStrategy 的持有/组合方式未定义
3.4 节「降级策略框架」定义了 `DegradationStrategy` 接口和 `TimeoutDegradationStrategy` 实现，但 FallbackAiService 如何获取 DegradationStrategy 实例完全未定义：由谁注入？构造器注入还是字段注入？单一策略还是支持策略链？TimeoutDegradationStrategy 在 Phase 0 是否自动注册为 Bean？多个策略时的选择/优先级机制？建议明确 FallbackAiService 通过构造器注入 DegradationStrategy（或 List<DegradationStrategy>）；指定 Phase 0 默认注册一个返回 false 的 NoOpDegradationStrategy，TimeoutDegradationStrategy 归 Phase 2+；补充伪代码骨架或构造器签名。

### 问题 4（轻微）：ScheduleItem.date 字段类型与 ScheduleRequest 不一致
ScheduleRequest（8.2 节）使用 `LocalDate startDate` / `LocalDate endDate` 表示日期，但同一个排班领域的 ScheduleItem（响应 DTO）将日期定义为 `String date`。同领域的前后日期类型不一致。建议将 `ScheduleItem.date` 类型统一为 `LocalDate`，与请求端的日期类型保持一致；若因序列化格式原因必须使用 String，需注释明确格式如 `yyyy-MM-dd` 并在 ScheduleResponse 层面说明转换责任方。

### 问题 5（一般）：前端三端占位首页结构未定义
需求明确要求"三端前端可一键启动到占位首页"。文档定义了前端 monorepo 目录结构、共享包依赖关系、Vite 代理配置，但未定义各 `apps/*` 应用的入口文件结构。一个 Vite + Vue 3 应用至少需要 `index.html`、`src/main.ts`、`src/App.vue` 三个入口文件才能启动。建议在 2.4 节末尾补充三端 app 的最简入口文件结构说明，例如各 `apps/*` 包含 `index.html`（挂载点 `#app`）、`src/main.ts`（createApp + router）、`src/App.vue`（占位页面），并注明 Phase 0 的占位页面仅渲染"系统名称 + 占位提示"文本即可。

### 问题 6（轻微）：父 POM 基础结构未给出
文档给出了 `integration/pom.xml` 的完整骨架，但未给出根 POM（父 POM，`backend/pom.xml`）的基础结构。父 POM 需至少包含 `<groupId>`、`<artifactId>`、`<version>`、`<packaging>pom</packaging>`、`<modules>` 列表和 `<dependencyManagement>` 段。建议在 2.1 节目录布局之后或 2.2 节依赖规则之前，补充根 POM 的核心结构骨架。

### 问题 7（轻微）：@Valid 在分页参数上的校验生效条件说明不完整
3.1 节要求"所有分页 Controller 参数需标注 `@Valid` 以触发校验注解"，但 `PageQuery` 作为 GET 请求的查询参数，通常是 `@ModelAttribute` 绑定而非 `@RequestBody`。`@Valid` 在 `@ModelAttribute` 上确实生效，但需要确保 `spring-boot-starter-validation` 在类路径上。此外，`PageQuery.sort` 字段的格式未定义类型和格式说明。建议补充说明 `@Valid` 对 `@ModelAttribute` 和 `@RequestBody` 绑定均有效的前提条件；为 `PageQuery.sort` 补充字段类型（`String`）和格式说明（如 `"fieldName,direction"`，direction 为 asc/desc）。

## 历史迭代回顾
### 已解决的问题
- 迭代 1-9 轮的所有历史问题均已在前序轮次中得到修复，当前 v10 反馈中不再提及。
- 特别地：迭代 2 的"AI 方法输入/输出使用中文自然语言描述，缺乏具体 DTO 类型名"（已修复，当前表格已列出具体 DTO 名）；迭代 4 的"ErrorCode 从 enum 改为 interface"（已修复）；迭代 9 的"PermissionService 返回 JPA 实体"（已修复，改为返回 UserDTO）。

### 持续存在的问题（需重点解决）
- **AiService 相关契约问题**：迭代 2 指出缺乏具体 DTO 类型名（已修复）；迭代 10（本轮）进一步指出返回类型是 `AiResult<T>` 还是裸 DTO 不明确。这是契约定义的深化迭代。
- **common-module 编译期隔离问题**：迭代 1 指出 ai-api 缺乏编译期强制保障（已修复，拆分为 ai-api/ai-impl）；迭代 10（本轮）指出 common-module 存在同样的编译期隔离缺失问题。这是一个架构一致性问题的延续。
- **FallbackAiService 与降级策略的装配问题**：迭代 3/4/7 分别指出 shouldDegrade 无入参、Bean 装配循环依赖、装配顺序未定义（均已修复）；迭代 10（本轮）进一步指出 DegradationStrategy 的持有/组合方式未定义。这是降级策略框架的持续性细化。

### 新发现的问题
- 问题 4（ScheduleItem.date 类型与 ScheduleRequest 不一致）
- 问题 5（前端三端占位首页入口文件结构未定义）
- 问题 6（父 POM 基础结构未给出）
- 问题 7（@Valid 在分页参数上的校验生效条件说明不完整）

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606151714_Phase0_OOD\a_v10_copy_from_v9.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606151714_Phase0_OOD\requirement.md
