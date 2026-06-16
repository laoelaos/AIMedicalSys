# 质量审查报告：a_v2_diag_v1.md

## 审查范围

本轮审查侧重以下维度：
1. 产出是否充分响应了用户需求
2. 产出中是否存在事实错误或逻辑矛盾
3. 产出的深度和完整性是否满足后续使用需要
4. 从可操作性视角评估诊断建议是否足以让执行者直接采取行动

---

## 审查发现

### 问题一（逻辑矛盾 · Medium）：P1 修复提示与推荐方案 A 在 SecurityConfig 处置上相互矛盾

**所在位置**：诊断结论表（P1 行，"修复提示"列）与问题一「修复方案分析」方案 A

**问题描述**：
诊断结论表的 P1 修复提示明确写道：「将 **LoginUser、SecurityConfig**、UserDetailsService、AuthenticationEntryPoint、AccessDeniedHandler、PasswordEncoder **迁移至 Phase 1 OOD**；**Phase 0 仅保留 User/Role/Post/Function 四个实体定义**」。紧接着又写道：「**建议采用方案 A（保留最小 SecurityConfigPhase0 permitAll 占位）**」。

这两条指令存在直接冲突：
- 修复提示要求将 SecurityConfig（包含 SecurityConfigPhase0）迁移至 Phase 1，Phase 0 仅保留四个实体
- 方案 A 要求 Phase 0 保留 SecurityConfigPhase0 占位

SecurityConfigPhase0 本身即是一个 `@Configuration @Profile("phase0")` 标注的 SecurityConfig。如果按修复提示移走它，Phase 0 骨架将因 Spring Security 自动配置而启动时返回 401；如果按方案 A 保留它，则与修复提示的"迁移至 Phase 1"冲突。执行者拿到报告后无法确定应按哪一条指令执行。

**改进建议**：
将修复提示列中的「SecurityConfig」修正为精确的限定表述。例如：
> 将 LoginUser、UserDetailsService **及共享安全配置类**（AuthenticationEntryPoint、AccessDeniedHandler、PasswordEncoder、CorsConfigurationSource）迁移至 Phase 1 OOD；**SecurityConfigPhase0（permitAll 占位）保留在 Phase 0**。Phase 0 仅保留 User/Role/Post/Function 四个实体定义 + SecurityConfigPhase0 permitAll 骨架。

---

### 问题二（关键遗漏 · Medium）：未解释 AiService 接口契约被允许、PermissionService 被禁止的差异依据

**所在位置**：问题二（P2）全文

**问题描述**：
P2 的核心论据是：路线图 Phase 0 "明确不包含"中列有"模块级接口契约冻结"，因此 PermissionService（跨模块门面接口，定义了完整的方法签名、参数类型、返回类型）不应出现在 Phase 0 OOD 中。

然而同一份 OOD 的 §8.2 定义了 AiService 接口的 13 个方法签名及全部 26 个输入/输出 DTO 类名和包路径——这同样是"模块级接口契约冻结"。但路线图 Phase 0 "推荐补齐"中明确允许了**AI 能力模块接口契约与 Mock 数据占位**。

诊断报告未在任何位置讨论这一差异的合理性依据。执行者（或未来轮次的审查者）读到 P2 论证后自然会产生疑问：**"为什么 AiService 的 13 个方法签名可以在 Phase 0 冻结，而 PermissionService 的 2 个方法签名就不行？"** 缺乏区分说明削弱了 P2 论证的说服力，也让执行者难以判断未来类似场景的边界。

**改进建议**：
在 P2 根因分析或修复方案中补充说明区分依据。例如：
> 路线图 Phase 0 "推荐补齐"节明确将 **"AI 能力模块接口契约与 Mock 数据占位"** 列为允许的推荐补齐项（"AI 能力模块 Mock 占位：AI 能力模块接口契约与 Mock 数据占位，支持前端独立开发（可跨阶段持续完善，不阻塞 Phase 0 骨架验收）"）。因此 AiService 的接口契约属于路线图明确允许的例外。PermissionService 不属于该例外，也不属于 AI 能力范畴，且路线图 Phase 0 "明确不包含"中已明确排除"模块级接口契约冻结"，故两者在 Phase 0 的定位不同。

---

### 问题三（关键遗漏 · Low）：P2 修复方案未讨论 UserDTO 和 UserType 的处置策略

**所在位置**：问题二（P2）修复方案分析

**问题描述**：
PermissionService.getUserById() 返回 UserDTO。如果 PermissionService 整体移至 Phase 1 OOD，UserDTO 作为其关联 DTO 也应随同迁移。同时 §3.3 明确指出 **"UserType 枚举定义在 common-module-api 子模块中，由 User 实体和 UserDTO 共同引用"**——UserDTO 移走后，UserType 在 common-module-api 中仅被 common-module-impl 中的 User 实体引用。诊断报告未讨论以下问题：

1. UserDTO 是否应随 PermissionService 一同移至 Phase 1？如果是，common-module-api 在 Phase 0 保留为空壳后，其中是否包含 UserType？
2. UserType 被 User 实体引用（common-module-impl），如果 UserType 留在 common-module-api 中是否构成契约冻结？

虽然 UserType 因被 Phase 0 的 User 实体引用而需要在 Phase 0 就位（不属于契约冻结问题），但诊断报告未做此澄清，执行者可能误将 UserType 也一并移除。

**改进建议**：
补充说明：UserDTO 随 PermissionService 迁至 Phase 1 OOD；UserType 留在 common-module-api 中，因被 Phase 0 的 User 实体引用——其枚举值（如 PATIENT、DOCTOR、ADMIN）属于权限模型骨架的固有属性而非跨模块契约冻结，不违反路线图边界。

---

## 整体评价

报告在以下方面表现良好：
- 对 OOD 文档偏离路线图边界的问题（P1、P2）定位准确，根因分析深入且区分了"有意识越界设计决策"与"概念误扩展"
- 两个 High 级别问题的修复方案分析表格完整，权衡清晰，可操作性强
- P3（spring-boot-starter-web 非 optional）、P4（依赖方向图错误）的诊断准确
- v2 修订说明完整回放了历史质询的修订情况

存在上述三个质量瑕疵，其中问题一（逻辑矛盾）和问题二（关键遗漏）为 Medium 级别，建议在 v3 中优先修正。问题三为 Low 级别，修正优先级较低但不建议忽略。

---
