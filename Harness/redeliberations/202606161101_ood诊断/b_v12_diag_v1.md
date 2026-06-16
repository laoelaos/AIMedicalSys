# 质量审查报告 — v12 诊断报告

## 问题 1：§2.3 第三子点——事实性断言与 OOD 实际内容不符（严重程度：一般）

- **位置**：`a_v12_diag_v1.md` §2.3 第 3 子点（L53-L54），`DiscussionConclusionResponse.discussionText` 部分
- **问题描述**：报告声称"OOD 未在对应错误码或校验逻辑中体现该质量门槛的触发条件"，但 OOD §8.2 L1274 明确写道："`DiscussionConclusionResponse` 对应补充错误码 `DISCUSS_AI_OUTPUT_INCOMPLETE`，用于 `discussionText` 未达到 50 字符最低质量门槛时的返回语义"。该语句已将错误码和触发条件（未达到 50 字符）显式关联。报告断言"未体现"与 OOD 实际内容不符。
- **改进建议**：将"OOD 未在对应错误码或校验逻辑中体现该质量门槛的触发条件"修正为"OOD 已在统一约束补充部分（L1274）将 `DISCUSS_AI_OUTPUT_INCOMPLETE` 映射至 50 字符阈值，但该映射属于文字说明层面而非 DTO 字段级 `@Size` 注解——Phase 0 Mock 模式下无运行时差异，Phase 2+ 实现者需在 Bean Validation 注解或业务校验中显式实现该映射"。

## 问题 2：§3.2——PasswordEncoder Profile 配置假设与 OOD 描述不一致（严重程度：一般）

- **位置**：`a_v12_diag_v1.md` §3.2 风险提示（L73）
- **问题描述**：报告假设"`PasswordEncoder` 配置于 `@Profile("!phase0")` 下"，并据此建议测试需激活 `!phase0` Profile。但 OOD §4.5 L780 明确描述：`AuthenticationEntryPoint`、`AccessDeniedHandler` 和 `PasswordEncoder` 配置"共享给两个 profile 的 SecurityConfig 使用"。若共享于两个 Profile，则该配置不应受 `!phase0` Profile 约束，Phase 0 默认 Profile 下 Bean 即应被创建。报告中关于 Profile 的断言缺乏 OOD 文本支撑，与 OOD 的"共享"描述相矛盾。
- **改进建议**：移除或条件化 Profile 假设。若需保留测试建议，应改为："若 `PasswordEncoder` 定义于非 Profile 约束的共享配置类中，Phase 0 默认 Profile 下即可注入；若实际配置于 `@Profile("!phase0")` 下，则需在测试中激活对应 Profile。请确认 OOD 中 PasswordEncoder 的实际配置位置后选择相应的测试策略。"

## 问题 3：§2.2——分类边界与 OOD 自述范围存在持续摩擦（严重程度：轻微）

- **位置**：`a_v12_diag_v1.md` §2.2（L41-L46）
- **问题描述**：该诊断项归类于"偏离需求文档"标题下，诊断对象是"OOD §3.3 未列出需求 §2.6 矩阵中全部 7 类角色的权限边界映射"。但 OOD §3.3 L531 和 §8.4 L1354 多次明确声明 Phase 0 仅提供实体与 Repository 骨架，不提供 `PermissionServiceImpl` 等门面实现，权限矩阵的完整映射属于 Phase 1 数据播种与实现任务。将 OOD 明确声明不覆盖的内容诊断为"偏离需求文档"（即"OOD 应做而未做"）属于分类判断，但该判断与 OOD 自述范围不一致，且已在 7 轮迭代中被多次质疑。报告虽在修订说明中给出了分类理由（映射缺失 vs 衔接准备），但该理由对"最小化骨架"的 Phase 0 特殊性未予充分权重——骨架文档不要求列出所有后续阶段才落地的完整映射关系。
- **改进建议（二选一）**：（A）维持当前分类，但在 §2.2 开头的偏离说明中补充以下上下文："注：OOD 多处声明 Phase 0 不提供权限门面实现（§3.3 L531、§8.4 L1354），当前诊断不要求 OOD 列出全部映射，而是建议在 §3.3 中补充矩阵对齐验证结果说明，以降低 Phase 1 实现者的交叉对照成本。"（B）将该项整体移入 §8（跨阶段衔接建议），与 §8.3 权限模型 Phase 1 数据种子项合并，不再归入"偏离需求文档"维度。

## 问题 4：§6——事实错误维度审查方法声明仍可改进（严重程度：轻微）

- **位置**：`a_v12_diag_v1.md` §6（L124）
- **问题描述**：报告声明"经逐项排查，未发现 OOD 文档中包含不符合技术事实的描述"，但仅说明了排查范围（OOD §1~§10）和交叉核对文档，未说明排查方法（如：如何验证 Spring Security 配置的正确性、Hibernate 注解行为、Maven 依赖传播规则等技术断言的正确性）。读者无法判断这些技术断言是经过逐条验证确认正确的，还是仅凭审查者自身知识信赖的。v11 修订已补充了排查范围声明，但方法学层面仍有提升空间。
- **改进建议**：补充方法论声明，如："事实错误维度的审查方法包括：（1）将 OOD 中所有显式的技术断言（版本号、框架行为、注解语义等）与对应框架的官方文档或已知行为进行对照验证；（2）对关键框架配置（Spring Security CSRF 行为、Hibernate @SQLRestriction 作用域、Maven optional 依赖传播等）逐一确认其准确性。未发现违背技术事实的描述。" 或简化为"本节技术断言已通过框架官方文档/已知行为验证"。
