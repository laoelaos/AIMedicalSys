# OOD Phase 0 文档诊断报告

> 被诊断文档：`Docs/04_ood_phase0.md`
> 对照文档：`Docs/03_roadmap.md`、`Docs/01_requirement.md`

---

## 一、定义矛盾（定义矛盾）

### 问题 1：common 模块声明 `spring-boot-starter-security` 依赖但未使用相应类型

- **所在位置**：第 292 行、第 307 行
- **严重程度**：中等
- **问题描述**：
  第 292 行声明 common 模块依赖 `spring-boot-starter-security`（用于可复用的通用安全类型），第 307 行进一步说明该依赖标注为 `<optional>true</optional>`，理由是"部分通用安全类型仍可能被复用"。
  然而通读全文，common 模块中实际放置的内容（`base/`、`result/`、`exception/`、`util/`、`config/`）不包含任何与 Spring Security 相关的类型。文档第 785 行将 `AuthenticationEntryPoint`、`AccessDeniedHandler`、`PasswordEncoder`、`SecurityFilterChain` 等全部划归 `application` 模块，明确声明"common 不承担阶段性安全策略职责"。
  既无实际类型引用 security，则 common 声明该依赖的理由不成立，与第 785 行的职责划分自相矛盾。
- **建议**：
  移除 common 模块对 `spring-boot-starter-security` 的依赖声明。需要 security 的模块在自己的 POM 中直接声明并依赖父 POM 的 dependencyManagement 版本管理即可。

### 问题 2：`spring-boot-starter-data-jpa` 非 optional 传播与纯接口模块的定位矛盾

- **所在位置**：第 292 行、第 293 行、第 296 行、第 306 行
- **严重程度**：中等
- **问题描述**：
  第 306 行声明 `spring-boot-starter-data-jpa` 为 common 的 compile 依赖且未标注 `<optional>`，理由是"所有业务模块均包含 JPA 实体和 Repository"。但第 293 行和第 296 行明确指出 `common-module-api` 和 `ai-api` 是纯接口/DTO 模块，"不含任何业务实现依赖"。
  纯接口模块通过 transitive 依赖获得 spring-boot-starter-data-jpa 的全量依赖树（Hibernate、连接池、Spring Data JPA 基础设施等），与其"轻量接口契约"的定位矛盾。这属于模块职责定义与依赖声明之间的逻辑冲突。
- **建议**：
  将 common 的 data-jpa 依赖标注 `<optional>true</optional>`，或新建独立的 `common-jpa` 子模块，由真正需要 JPA 的模块显式依赖。

### 问题 3：依赖关系图符号歧义

- **所在位置**：第 272–287 行
- **严重程度**：轻微
- **问题描述**：
  ASCII 依赖关系图中 ai-api 同时出现在两个位置（作为 common 的子节点出现在右上方，又作为与 patient/doctor/admin 并列的叶子模块出现在水平连接线的右端），阅读者易误判 ai-api 与其他模块存在循环依赖。第 289–290 行的注释说明虽然澄清了设计意图，但图形表达本身不自洽。
- **建议**：
  重新绘制依赖关系图，消除 ai-api 的双重出现，或采用更清晰的布局（如将 ai-api 单独列出并标注业务模块对它的依赖箭头，避免水平跨度过大的连接线）。

---

## 二、逻辑错误（逻辑错误）

### 问题 4：FallbackAiService 的 `List<AiService>` 自排除逻辑在 Phase 0 的特殊边界下表现为无委托对象

- **所在位置**：第 658–662 行、第 645 行
- **严重程度**：轻微
- **问题描述**：
  FallbackAiService 构造器注入 `List<AiService>` 后在构造器中排除自身，选定委托对象。文档第 645 行已经说明 Phase 0 不应设置 `ai.mock.enabled=false`，但未通过任何编译期或配置校验机制保障这一约束。当有人误配置时，FallbackAiService 降级为直接返回 `AiResult(success=false, degraded=true, data=null)`，所有 AI 调用全部返回降级结果。
  这一行为虽然被文档显式兜底处理了，但兜底后的效果（全部调用返回降级）与"配置 false 期望激活真实实现"的直觉相反，且没有任何日志或启动警告提示运维人员。
- **建议**：
  FallbackAiService 在兜底路径（排除自身后 List 为空）时输出 WARN 级别日志，提示"未找到可用的 AiService 实现，全部调用将返回降级结果"。

---

## 三、偏离路线图 Phase 0 阶段（偏离路线图phase0阶段）

### 问题 5：完整定义 13 项 AI 能力的 DTO 字段级契约，构成模块级接口契约冻结

- **所在位置**：第 882–1270 行（Section 8.2）
- **严重程度**：严重
- **问题描述**：
  Section 8.2 为全部 13 项 AI 能力定义了完整的 DTO 字段结构、字段类型、可空标注、校验约束（字符数范围、枚举值列举），覆盖 Phase 2 到 Phase 5 才首次落地或真实推理的能力（如 `DiagnosisRequest`、`KbQueryRequest`、`ScheduleRequest`、`DiscussionConclusionRequest` 等）。
  `Docs/03_roadmap.md` 第 69 行（Phase 0 0.4 节"本阶段明确不包含"）明确规定：
  > **模块级接口契约冻结（在对应阶段启动前冻结）**

  "模块级接口契约"包括接口方法签名、DTO 字段结构、命名约定等。在 Phase 0 为其后 4 个阶段的 AI 能力预先定义完整的字段级 DTO 契约，实质上是提前冻结了这些模块的接口契约。
  文档自身第 906 行也承认了这一动机："为避免后续 Phase 2~5 出现破坏性 DTO 调整，本节已按需求文档 3.4.x 的字段集收敛"——这恰好是路线图要求在对应阶段启动前才完成的工作。
  这一做法同时带来了实际风险：在 Phase 2~5 开发和需求细化过程中，如果发现需要调整 DTO 字段结构，将面临"破坏 Phase 0 OOD 设计"的心理阻力，使设计灵活性降低。
- **建议**：
  Section 8.2 缩减为 Phase 0 实际需要的 AI 能力占位部分（仅包括 `AiService` 接口方法签名骨架和 `MockAiService` 涉及的少数核心 DTO，如占位响应所需的类型）。各能力的完整 DTO 定义移至对应阶段的 OOD 文档中。如果团队认可"为降低后期返工成本而提前约定"，则应在路线图中明确修改 Phase 0 的范围描述以消除矛盾。

### 问题 6：数据权限扩展点设计（`DataPermissionEvaluator`）超出 Phase 0 骨架范围

- **所在位置**：第 587–593 行（Section 3.3 数据范围扩展点）
- **严重程度**：轻微
- **问题描述**：
  第 587–593 节为数据级权限设计了 `DataPermissionEvaluator` 接口、`DataScopeType` 枚举、与 `PermissionService` 的职责划分约定。路线图 Phase 0 的"数据与权限模型骨架"应仅包含基础实体定义和 Repository 骨架。数据权限属于 Phase 1+ 的权限矩阵落地内容，第 593 行自身也承认为"Phase 1 起"。
  虽然文档以"扩展点"名义预留，但 `DataPermissionEvaluator` 接口的具体方法形态和 `DataScopeType` 的枚举值（`SELF_OWNED`、`SELF_HANDLED`）已构成 Phase 1 权限 OOD 的预设计，超出了 Phase 0 骨架的职责边界。
- **建议**：
  移除 DataPermissionEvaluator 接口声明和 DataScopeType 枚举值定义，改为一句话标注"数据权限扩展点留白，Phase 1 OOD 中定义"，消除预冻结。

---

## 四、汇总

| 编号 | 类别 | 严重程度 | 位置（行号） |
|------|------|---------|-------------|
| 1 | 定义矛盾 | 中等 | 292, 307, 785 |
| 2 | 定义矛盾/逻辑错误 | 中等 | 292, 293, 296, 306 |
| 3 | 定义矛盾 | 轻微 | 272–287 |
| 4 | 逻辑错误 | 轻微 | 658–662, 645 |
| 5 | 偏离路线图phase0阶段 | 严重 | 882–1270 |
| 6 | 偏离路线图phase0阶段 | 轻微 | 587–593 |

---

## 修订说明（v2）

| 质询意见 | 回应 |
|---------|------|
| **问题 4（原诊断编号）**：诊断声称"application 的 runtime scope 依赖（H2）不会被传递到 integration 的 test classpath 中"，该论断与 Maven 官方依赖范围传递规则矛盾。Maven 标准规则：direct scope = test, transitive scope = runtime → effective scope = test。H2 将以 test scope 正确传递到 integration 模块。 | 接受。经查阅 Maven 官方文档（`maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html`）确认：direct=test, transitive=runtime → effective=test。H2 可正确传递至 integration 模块的 test classpath。原诊断的 Maven 规则理解错误，该问题不成立。**已从报告中移除该问题。** |
| **问题 5（原诊断编号）**：诊断将 PageQuery 的 sort 自定义解析定性为"逻辑错误"（中等），但该设计选择有明确权衡（统一契约、额外校验、兼容 Spring Data 格式），并非逻辑错误。诊断未能提供足够证据证明该设计在特定上下文中是错误的而非仅是偏好差异。 | 接受。PageQuery 的 sort 自定义解析虽然在 Spring Data 原生能力之上做了一层包装，但此举提供了额外校验（格式无效时抛出 BusinessException 由 GlobalExceptionHandler 统一处理）和统一的分页契约约束。这是有明确权衡的设计决策，而非逻辑错误。**已从报告中移除该问题。** |
