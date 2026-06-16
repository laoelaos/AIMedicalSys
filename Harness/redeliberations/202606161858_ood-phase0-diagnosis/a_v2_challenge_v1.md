# 诊断质询报告（v2）

## 质询结果

LOCATED

## 逐维度审查

### 1. 证据充分性

**[通过]** 诊断报告中的核心事实性声明（Maven name、`spring.application.name`、目录名、`springdoc-openapi` 版本号、`BaseEntity.deleted` 类型、`@phase0-mock-field` 用途、AiService 13 方法签名、装配条件表等）均已与实际 OOD 文档逐项核对，内容准确。

**[通过]** 对路线图的引用（Phases 0.2/0.4、验收标准、附录 A、推荐补齐项）均与 `03_roadmap.md` 原文一致。

**[通过]** 降级框架各组件的列举（`FallbackAiService`、`DegradationStrategy`、`NoOpDegradationStrategy`、`TimeoutDegradationStrategy`、配置矩阵）与 OOD §3.4 描述匹配。

**[通过]** 修复建议的副作用分析已按要求补充，Jackson 序列化依赖拆分、接口冻结约束、包路径为空状态等均基于 OOD 的实际依赖关系推导。

**[轻微]** 1.1 条所述"Monorepo 目录布局顶层目录名 `aimedical-sys/`"在 OOD 中实际行号为 L52 而非 L54（L54 为 `├── pom.xml`），行号偏移 2 行，不影响实质性判断。

### 2. 逻辑完整性

**[通过]** 从问题现象到根因的因果链完整：以 4.1 为例，`GlobalExceptionHandler`（`@ControllerAdvice`）→ common 需依赖 `spring-boot-starter-web` → 即使 `optional=true`，common 编译期仍绑定 Web 框架 → 非 Web 消费者引入不必要依赖 → 建议移至 application 模块。

**[通过]** v2 对 Phase 0.4 边界（6.3）的处理兼顾了"方法签名冻结 vs 模块级接口契约在对应阶段启动前冻结"的两个视角，逻辑论证平衡。

**[通过]** 6.5 附录 A 对比分析合理：OOD 整体符合"Mock 占位"定位，仅降级框架维度存在超范围设计。

**[轻微]** 1.1 标题仍置于"一、定义矛盾"分类下，但正文已明确说明"三者不存在'定义矛盾'——用途不同"。描述内容已修正（Maven 构建显示名与运行时应用名未对齐），但分类与正文表述不完全一致，可能造成阅读时的认知偏差。

**[轻微]** 4.2 条中"OOD 未明确 Phase 0 是否要求业务模块在 POM 中声明 ai-api 依赖"与 OOD L309"modules/patient、modules/doctor、modules/admin：依赖 common、common-module-api 和 modules/ai/ai-api"的明确表述不完全一致。该条的合理质疑点应为"OOD 未明确 Controller 层是否需要注入 `AiService` 字段或仅 POM 声明即可"，而非 POM 依赖是否明确。

### 3. 覆盖完备性

**[通过]** 原始需求的四项检查（偏离路线图、定义矛盾、事实错误、逻辑错误）均已有对应章节覆盖。

**[通过]** 迭代需求（a_v2_iteration_requirement.md）中 7 项反馈全部得到回应：
- 2.1 "3.4.x 跨文档编号引用"误判 → 已删除 ✅
- 2.2 "@phase0-mock-field 语义混淆" → 已修订描述并降为"低" ✅
- 2.3 springdoc-openapi 分类不当 → 已移至"风险提示" ✅
- 遗漏 Phase 0.4 对照检查 → 已新增 6.3 条 ✅
- 诊断建议缺少副作用分析 → 已在"七"补充 ✅
- 应用名混为一谈 → 已修订描述 ✅
- 深度补充（附录 A 对比、3.2 降级、Jackson 拆分、优先级标注）→ 均已补充 ✅

**[通过]** 诊断结论的"问题是什么"（OOD 中 13 项具体问题）和"为什么发生"（根因分析）均有明确回答。

**[通过]** 修复者可根据诊断结论采取具体行动（新增协作规范章节、创建 QUICKSTART.md、调整 GlobalExceptionHandler 位置、精简降级框架等）。

## 质询要点

本报告无需质询要点（LOCATED）。

无严重或一般问题。上述三处"轻微"问题均不改变诊断结论方向，不影响修复者据此采取行动。
