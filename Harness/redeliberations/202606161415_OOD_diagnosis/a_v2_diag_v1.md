# OOD Phase 0 文档诊断报告

> 被诊断文档：`Docs/04_ood_phase0.md`
> 对照文档：`Docs/03_roadmap.md`、`Docs/01_requirement.md`

---

## 一、定义矛盾

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
  纯接口模块通过 transitive 依赖获得 spring-boot-starter-data-jpa 的全量依赖树（Hibernate、HikariCP 连接池、Spring Data JPA 基础设施等），与其"轻量接口契约"的定位矛盾。这属于模块职责定义与依赖声明之间的逻辑冲突。
- **建议（含副作用分析）**：
  提供两种可行方案，各方案的权衡如下：

  **方案 A：将 data-jpa 标注 `<optional>true</optional>`**
  - 操作：在 common/pom.xml 中将 spring-boot-starter-data-jpa 标记为 optional
  - 正面效果：纯接口模块不再继承 JPA 全量依赖树；需要 JPA 的模块（patient、doctor、admin、common-module-impl）在各自 POM 中显式声明
  - 副作用：显式声明需要在 4 个业务模块的 POM 中各增加一行依赖。common-module-impl 本身已经是 common-module-api + common 的叠加，增加声明仍在可接受范围。整体 CI 流水线无变化
  - 推荐度：★★★（推荐，改动量小，副作用可控）

  **方案 B：新建 `common-jpa` 子模块**
  - 操作：从 common 中抽离 JPA 相关内容（BaseEntity、JpaConfig）至独立的 common-jpa 子模块，需要 JPA 的模块显式依赖 common-jpa
  - 正面效果：职责边界最清晰，common-jpa 仅暴露 JPA 基类契约
  - 副作用：新增 Maven 子模块（需在父 POM 注册、CI 分阶段构建中增加第一阶段构建项、POM 版本管理条目增加）。在当前所有业务模块均含 JPA 实体的前提下，额外模块隔离的必要性有限。POM 维护负担和 CI 流水线时间增加
  - 推荐度：★★（架构整洁但过度设计，Phase 0 骨架阶段收益小于成本）

  **结论：推荐方案 A。** 标注 optional + 各业务模块显式声明，以最小改动消除矛盾。若后续 Phase 2 发现新模块（如硬件接入模块）无需 JPA，optional 机制已可满足隔离需求。

---

## 二、事实错误

### 问题 3：common 依赖传播的事实描述自相矛盾

- **所在位置**：第 304 行、第 307 行
- **严重程度**：中等
- **问题描述**：
  第 304 行描述 common 的依赖传播策略时写道："common 模块以 compile scope 声明了三个 Starter 依赖（spring-boot-starter-web、spring-boot-starter-security、spring-boot-starter-data-jpa），所有业务模块引入 common 后将无条件获得全部 transitive 依赖"。
  但第 307 行随后说明 spring-boot-starter-security 标注了 `<optional>true</optional>`。在 Maven 依赖传递规则中，标记为 optional 的依赖**不参与 transitive 传播**。第 304 行"无条件获得全部 transitive 依赖"的陈述构成事实性错误——security 不会无条件传递。
  读者在未读到第 307 行时，会误认为 security 也会随 common 自动传递到所有下游模块；只有继续读到第 307 行才能纠正这一理解。文档同一节内的先后陈述不一致，属于对依赖行为的事实描述错误。
- **建议**：
  将第 304 行"无条件获得全部 transitive 依赖"修正为"以下三个 Starter 依赖的传播策略按需配配置：web 和 data-jpa 为 compile scope 不限传递，security 为 compile scope + optional（不参与传递）"。明确区分三类依赖的实际传递行为，消除事实性误导。

---

## 三、逻辑错误

### 问题 4：FallbackAiService 的 `List<AiService>` 自排除逻辑在 Phase 0 的特殊边界下表现为无委托对象

- **所在位置**：第 658–662 行、第 645 行
- **严重程度**：轻微
- **问题描述**：
  FallbackAiService 构造器注入 `List<AiService>` 后在构造器中排除自身，选定委托对象。文档第 645 行已经说明 Phase 0 不应设置 `ai.mock.enabled=false`，但未通过任何编译期或配置校验机制保障这一约束。当有人误配置时，FallbackAiService 降级为直接返回 `AiResult(success=false, degraded=true, data=null)`，所有 AI 调用全部返回降级结果。
  这一行为虽然被文档显式兜底处理了，但兜底后的效果（全部调用返回降级）与"配置 false 期望激活真实实现"的直觉相反，且没有任何日志或启动警告提示运维人员。
- **建议**：
  FallbackAiService 在兜底路径（排除自身后 List 为空）时输出 WARN 级别日志，提示"未找到可用的 AiService 实现，全部调用将返回降级结果"。

---

## 四、偏离路线图 Phase 0 阶段

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

  **关于编译期依赖约束的补充分析**：
  OOD 文档中 `AiService` 接口设计为类型化方法签名模式，如：
  ```
  CompletableFuture<AiResult<TriageResponse>> triage(TriageRequest request);
  ```
  这意味着所有方法签名中引用的 DTO 类型（`TriageRequest`、`TriageResponse` 等）必须在编译期存在，否则接口无法编译。因此"仅保留方法签名骨架而不保留 DTO 类型"的简化方案是**不可行的**——类型擦除无法应用于编译期类型检查。
- **建议（含可行方案评估）**：

  **方案 A：保留所有 DTO 类型定义但缩减为"空壳占位"（推荐）**
  - 将 13 项能力的 DTO 从完整的字段级定义缩减为**仅保留类声明+默认构造器**（class 骨架，不含任何字段），确保 AiService 接口可编译。字段级定义推迟到对应阶段的 OOD 文档中补齐
  - 同时保留 AiService 接口的全部 13 个方法签名骨架（类型化返回值，引用上述空壳 DTO）
  - 优势：满足编译期约束、不提前冻结接口契约、最小改动
  - 权衡：后续阶段需要逐阶段补充 DTO 字段定义，存在文档工作分散的成本

  **方案 B：保持当前完整字段定义，同时在路线图中明确修改 Phase 0 范围**
  - 在 `Docs/03_roadmap.md` Phase 0 0.4 节中将"AI 能力的 DTO 字段级契约提前约定"明确声明为设计决策之例外
  - 优势：保留当前文档不动
  - 权衡：路线图的"本阶段不包含"清单与设计行为不一致，需同步修改。若后续字段调整，仍面临心理阻力。修改路线图的门槛高于修改 OOD

  **方案 C：将 AiService 改为无类型签名（不推荐）**
  - 改为统一泛型方法：`CompletableFuture<AiResult<?>> invoke(String methodName, Map<String, Object> params)`
  - 优势：无需任何 DTO 类型，完全规避编译期约束
  - 权衡：丧失编译期类型安全性，各调用点需要运行时类型断言；与 OOD 整体"类型安全优先"的设计原则冲突

  **结论：推荐方案 A。** 字段定义全面推迟，保留类型安全接口契约，路线图不受影响。MockAiService 在 Phase 0 只需用 `new XXXDto()` 返回占位数据即可。

### 问题 6：数据权限扩展点设计（`DataPermissionEvaluator`）超出 Phase 0 骨架范围

- **所在位置**：第 587–593 行（Section 3.3 数据范围扩展点）
- **严重程度**：轻微
- **问题描述**：
  第 587–593 节为数据级权限设计了 `DataPermissionEvaluator` 接口、`DataScopeType` 枚举、与 `PermissionService` 的职责划分约定。路线图 Phase 0 的"数据与权限模型骨架"应仅包含基础实体定义和 Repository 骨架。数据权限属于 Phase 1+ 的权限矩阵落地内容，第 593 行自身也承认为"Phase 1 起"。
  但需区分对待：
  - **`DataPermissionEvaluator` 接口声明**：属于扩展点预留（"可演进"设计目标的一部分），仅声明接口名称占位未来扩展，不构成接口契约冻结。与路线图 Phase 0 的"可演进"目标一致，**应予保留**
  - **`DataScopeType` 枚举值定义（`SELF_OWNED`、`SELF_HANDLED`）**：已构成 Phase 1 权限 OOD 的预设计——枚举值和语义将在 Phase 1 直接使用，实质上提前冻结了数据权限的语义分类。超出 Phase 0 骨架的职责边界，**应予移除**
- **建议**：
  保留 `DataPermissionEvaluator` 接口声明（仅保留接口名和方法签名占位），移除 `DataScopeType` 枚举值定义。将第 591 行改为"Phase 0 标注数据权限扩展点留白，数据范围语义分类在 Phase 1 OOD 中定义"，消除预冻结。

---

## 五、文档表达问题

### 问题 7：依赖关系图符号歧义

- **所在位置**：第 272–287 行
- **严重程度**：轻微
- **问题描述**：
  ASCII 依赖关系图中 ai-api 同时出现在两个位置（作为 common 的子节点出现在右上方，又作为与 patient/doctor/admin 并列的叶子模块出现在水平连接线的右端），阅读者易误判 ai-api 与其他模块存在循环依赖。第 289–290 行的注释说明虽然澄清了设计意图，但图形表达本身不自洽。该问题属于文档表达清晰度问题，不涉及定义矛盾或逻辑错误。
- **建议**：
  重新绘制依赖关系图，消除 ai-api 的双重出现，或采用更清晰的布局（如将 ai-api 单独列出并标注业务模块对它的依赖箭头，避免水平跨度过大的连接线）。

---

## 六、汇总与修复优先级

| 优先级 | 编号 | 类别 | 严重程度 | 位置（行号） | 实施说明 |
|--------|------|------|---------|-------------|---------|
| P0 | 5 | 偏离路线图 | 严重 | 882–1270 | 先决条件：需确认方案 A 的空壳 DTO 策略。独立可执行，不依赖其他问题修复 |
| P1 | 1 | 定义矛盾 | 中等 | 292, 307, 785 | 移除无关依赖声明，改动范围局限于 common/pom.xml 的描述。可与 P2 并行 |
| P1 | 2 | 定义矛盾 | 中等 | 292, 293, 296, 306 | 标注 optional + 业务模块显式声明。P1 完成后可验证 common 的依赖树是否干净 |
| P1 | 3 | 事实错误 | 中等 | 304, 307 | 修正依赖传播事实描述文本。P1/P0 完成后文档描述需与新的依赖声明保持一致 |
| P2 | 7 | 文档表达 | 轻微 | 272–287 | 重新绘制 ASCII 图，独立可执行。不依赖其他问题 |
| P3 | 4 | 逻辑错误 | 轻微 | 658–662, 645 | 添加 WARN 日志。独立可执行，代码级改动无需等待其他问题 |
| P3 | 6 | 偏离路线图 | 轻微 | 587–593 | 移除 DataScopeType 枚举值，保留接口声明。建议在 P1 完成后做，避免文本修订范围冲突 |

**依赖关系说明**：
- P0 独立，无前置依赖
- P1 组（问题 1、2、3）均涉及 common 模块的依赖描述和 POM 配置，建议同步修订以避免冲突
- P2 和 P3 均不依赖 P0/P1，可随时并行执行

---

## 修订说明（v3）

| 质询意见 | 回应 |
|---------|------|
| **缺失"事实错误"检查维度（严重）**：报告缺失维度 2"事实错误"。 | **已补充。** 新增"二、事实错误"章节，定位到第 304 行依赖传播描述与第 307 行 optional 声明自相矛盾的问题。 |
| **问题 5 改进建议忽略编译期依赖约束（中等）**：建议缩减为仅包括 AiService 方法签名骨架和少数 DTO，但 OOD 的类型化签名模式要求所有 DTO 类型在编译期存在。 | **已修订。** 新增"关于编译期依赖约束的补充分析"段落，给出三方案选型（推荐方案 A：保留 DTO 空壳类声明）。 |
| **问题 6 改进建议缺少粒度区分（中等）**：接口声明属扩展点预留（应保留），枚举值属接口契约冻结（应移除），不应不加区分全部移除。 | **已修订。** 将建议拆分为两条独立建议：保留 DataPermissionEvaluator 接口声明，移除 DataScopeType 枚举值。 |
| **问题 2 改进建议缺乏可行性及副作用评估（中等）**：未评估各选项的副作用。 | **已修订。** 新增副作用分析表格，给出两个可行方案的正面效果和副作用对比，推荐方案 A（标注 optional）。 |
| **问题 3 的分类不匹配（轻微）**：ASCII 图歧义被归为"定义矛盾"，实际是文档表达清晰度问题。 | **已修订。** 将 ASCII 图问题移至新增的"五、文档表达问题"章节，从"定义矛盾"中移除。 |
| **缺乏明确的修复优先级排序（轻微）**：汇总表缺少实施顺序建议和依赖关系说明。 | **已修订。** 汇总表新增"优先级"列和"实施说明"列，补充依赖关系说明段落（P0→P1→P2→P3 的实施路线）。 |
