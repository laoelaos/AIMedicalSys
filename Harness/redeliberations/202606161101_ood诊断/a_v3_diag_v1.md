# Phase 0 最小化骨架 OOD 设计方案 — 诊断报告（v3）

> 诊断对象：`Docs/04_ood_phase0.md`
> 参考文档：`Docs/01_requirement.md`、`Docs/03_roadmap.md`
> 优先级标记：P0（阻塞骨架验收或运行时错误）、P1（影响 Phase 1+ 的衔接质量）、P2（建议性改善）

---

## 1. 定义矛盾

### 1.1 `common` 模块 security 依赖的陈述理由与实际架构矛盾（P1）

- **位置**：§2.2 (L293) vs §4.5 (L306-L307, L759, L785)
- **矛盾表述 A**（L293）："`common`：依赖 spring-boot-starter-web、spring-boot-starter-security（**用于 SecurityConfig**）及 spring-boot-starter-data-jpa"
- **矛盾表述 B**（L306-L307）："`common` 中**不放置阶段性 `SecurityConfig`**"
- **矛盾表述 C**（L759, L785）："SecurityConfig 设计骨架（归属 **`application`** 模块）"；"随阶段演进变化的 `SecurityFilterChain`、`AuthenticationEntryPoint`、`AccessDeniedHandler` 等安全策略配置统一放在 **`application`** 模块"
- **分析**：同份文档内对 `common` 模块为何需要 `spring-boot-starter-security` 给出了相互矛盾的理由。L293 声称"用于 SecurityConfig"，但文档多处明确 SecurityConfig 放置在 application 模块而非 common 模块。`common` 模块引用 Spring Security 类型的实际原因是 `GlobalExceptionHandler`（common.config 包）中的 `@ExceptionHandler` 方法需要捕获 `AuthenticationException` 和 `AccessDeniedException` 类型（§5.1 L806-L807），而非"用于 SecurityConfig"。该矛盾不影响可执行性（optional 依赖的实际编译行为正确），但属于文档内部事实陈述冲突。
- **修复者须知**：L293 中"用于 SecurityConfig"的措辞应修正为"用于 GlobalExceptionHandler 捕获安全异常类型"。

### 1.2 JDBC 驱动依赖 scope 策略前后不一致（P1）

- **位置**：§9.1 (L1403-L1411) vs §9.1 (L1413)
- **矛盾表述**：H2 内存数据库以 `runtime` scope 引入（L1407-L1411），这是 JDBC 驱动的标准做法。但同节 L1413 说明 Phase 1+ 切换 MySQL/PostgreSQL 时驱动以 `compile`（默认）scope 引入。同一类依赖（JDBC 驱动）使用了不同的 scope 策略，且 `compile` 会导致下游模块通过 transitive 依赖引入不必要的 JDBC 驱动包，与 §2.2 模块依赖治理目标不一致。
- **修复者须知**：JDBC 驱动应统一使用 `runtime` scope，无论 H2 还是 MySQL/PostgreSQL。L1413 的 `compile` scope 应修订为 `runtime`。

> **诊断边界说明**：MySQL/PostgreSQL 驱动 scope 不一致项涉及 OOD 对 Phase 1+ 配置的引用描述（Phase 0 仅使用 H2 内存数据库）。该问题不影响 Phase 0 交付物的运行正确性，但属于 OOD 文档内跨阶段的技术引用错误——Phase 1 开发者若按文档使用 `compile` scope 引入 JDBC 驱动，将引入不必要的 transitive 依赖，与 §2.2 模块依赖治理目标不符。建议同步修正以避免误导 Phase 1 开发者。

---

## 2. 偏离需求文档

> **优先级判定统一标准**：P0 为"阻塞 Phase 0 三项验收标准（骨架可编译运行、接口契约冻结、基础设施验证）"；P1 为"影响 Phase 1+ 衔接质量"；P2 为"建议性改善"。以下各项均基于此统一标准判定。

### 2.1 错误码命名约定衔接缺口（P0）

- **位置**：需求 §3.4 (L814-L818) vs OOD §8.2
- **偏离说明**：需求文档要求在全部 13 项 AI 能力错误码中统一携带 `_AI_` 中段（如 `TRIAGE_AI_TIMEOUT`），并对已有不符合约定的能力完成修订（L818 指出 3.4.2/3.4.3/3.4.9/3.4.10/3.4.11 以及 3.4.1 内部混用的 `TRIAGE_INPUT_INVALID` 均在需求文档中统一修订）。OOD 文档虽在 §5 定义了 `ErrorCode` 接口和各模块 enum 实现策略，但 §8.2 各 AI 能力 DTO 未显式列出对应错误码清单，也未说明 Phase 0 各能力 Mock 实现的错误码返回值需遵循 `_AI_` 命名约定。这属于接口契约冻结时的遗漏——修复者（Phase 2+ 的 AI 实现开发者）需要返回符合约定的错误码，但 OOD 未要求 Phase 0 的 MockAiService 返回符合命名约定的占位错误码。
- **升级依据**：路线图 Phase 0 "骨架必备"明确要求"统一响应包装、错误码命名空间"，该缺口直接影响 Phase 0 契约冻结质量——Phase 0 冻结的 Mock 返回值若不遵循约定，Phase 2+ 开发者将缺少可参照的约定示例，属于 Phase 0 验收范围内必须对齐的事项。

### 2.2 权限矩阵与需求规格对齐缺口（P1）

- **位置**：需求 §2.6 功能-角色可访问矩阵 vs OOD §3.3 权限模型核心抽象
- **偏离说明**：需求文档定义了完整的"角色—岗位—功能"三级权限可访问矩阵（含 7 类角色 × 28 项功能的 ✓/✗/○¹/○² 映射），并明确定义了 `○¹`（创建者/责任人）与 `○²`（接诊/经手人）两种受限访问语义。OOD 的权限模型设计（§3.3）虽然正确地采用了三级模型骨架，但存在以下缺口：
  - OOD §3.3 未列出需求 §2.6 矩阵中全部 7 类角色的权限边界映射，仅以"三级模型"抽象描述，未验证 OOD 设计中的权限判断逻辑能否覆盖矩阵中所有 ✓/○¹/○² 约束。
  - 需求 §2.6 中"线下接诊医生"角色（2.3.5）在 OOD 中没有对应的角色定义或实体预留说明，OOD 的权限模型部分仅提及 patient/doctor/admin 三个模块相，未显式覆盖线下接诊医生这一子岗位。
  - 需求 §2.6 矩阵中"药房医生"可访问"查看检查/检验结果"（○²），OOD 未在权限模型或数据流中描述该跨模块授权路径（处方→就诊→检查/检验报告）。
  - OOD §3.3 将 `DataPermissionEvaluator` 预留为扩展点，但未确认该扩展点的接口签名是否能承载需求中 `○¹`/`○²` 的全部语义差异（创建者 vs 经手人属于不同的数据范围判定维度，可能需独立的 Evaluator 方法或参数区分）。
- **降级依据**：权限矩阵的角色映射缺口属于数据种子和验证清单层面，不影响骨架运行和编译，不阻塞 Phase 0 三项验收标准中的任何一项。该缺口属于 Phase 0→Phase 1 衔接准备项。
- **修复者须知**：建议在 OOD §3.3 中补充权限矩阵与需求 §2.6 的对齐验证结果，明确各角色访问路径的覆盖情况，并将线下接诊医生纳入角色映射表。

### 2.3 DTO 字段约束与需求契约对齐检查（P1）

- **位置**：需求 §3.4.1–§3.4.13 vs OOD §8.2
- **偏离说明**：OOD §8.2 定义了 13 项 AI 能力的 DTO 结构，整体字段集与需求 §3.4.x 一致，但以下约束未在 OOD 中体现或存在差异：
  - 需求 §3.4.1 `TriageRequest.chiefComplaint` 字符数约束为 5–500（L825），OOD §8.2 的 `TriageRequest` 字段列表未标注该长度约束。
  - 需求 §3.4.3 `MedicalRecordGenRequest.dialogueText` 字符数约束为 50–10000（L861），OOD §8.2 未标注。
  - 需求 §3.4.13 `DiscussionConclusionResponse.discussionText` 最低质量门槛 ≥ 50 字符（L1060），OOD §8.2 虽标注了"最低质量门槛 >= 50 字符"，但在 L1257 写为"最低质量门槛 >= 50 字符，建议 >= 100 字符"，与需求的"< 50 返回 `DISCUSS_AI_OUTPUT_INCOMPLETE`"语义一致，但 OOD 未在对应错误码或校验逻辑中体现该质量门槛的触发条件。
  - 需求 §3.4.4 `DiagnosisRequest.condition.chiefComplaint` 字符数 ≥ 5（L890），OOD 未标注。
  - 需求 §3.4.x 多处使用 `@Nullable` 或"可选"标记字段（如 `TriageRequest.additionalResponses`、`DiagnosisRequest.encounterId`），OOD DTO 定义在字段级标注了 `@Nullable`，但在 Mock 数据占位约定（§3.4 L629）中对"标注 `@Nullable` 的字段返回 null"的约定与需求的"可选"语义一致，该一致性已验证通过。
- **修复者须知**：OOD 应在 §8.2 的 DTO 字段描述中补充各字段的必填/长度约束（从需求 §3.4.x 直接引用），或在 §8.2 统一约束补充部分（L1272）增加字段约束索引表。

### 2.4 实体关系与需求数据规格对齐检查（P2）

- **位置**：需求 §5.1 核心业务实体 vs OOD 实体设计
- **偏离说明**：需求 §5.1 列出了 18+ 核心业务实体及其关键字段和关系，OOD 在 §3.2（BaseEntity）和 §3.3（权限模型实体）中仅覆盖了 User、Role、Post、Function 四个实体，其余业务实体（挂号记录、处方、病历、检查/检验申请与报告、缴费记录、健康档案、药库/药房库存等）未在 Phase 0 OOD 中定义。Phase 0 作为最小化骨架不需要实现全部实体，但 OOD 未说明哪些实体属于 Phase 0 范围、哪些留待 Phase 1+ 补充。修复者（Phase 1 的开发者）需要自行判断哪些实体需在 Phase 1 起即具备骨架结构。
- **说明**：此偏离属于 Phase 0 骨架边界的合理裁剪，但 OOD 应补充一份"实体交付阶段映射表"——列出全部 18+ 核心实体及其归属的阶段，避免后续开发者遗漏 Phase 1 需要准备的关键实体骨架。

### 2.5 非功能指标对齐检查（P2）

- **位置**：需求 §3.4.x 各节"服务质量要求" vs OOD 非功能设计
- **偏离说明**：需求 §3.4.x 为每项 AI 能力定义了超时阈值（P95 与硬超时两档），如 3.4.1 P95 ≤ 5 秒/硬超时 ≤ 8 秒（L830）、3.4.2 P95 ≤ 3 秒/硬超时 ≤ 6 秒（L844）等。OOD 文档仅在 §6 并发设计中提到"Phase 2+ 引入 Spring Async 等异步机制"，未显式引用需求中的超时阈值。Phase 0 虽不实现真实 AI 调用，但 `AiService` 接口的 `CompletableFuture<AiResult<T>>` 契约已预留异步形态，MockAiService 使用 `CompletableFuture.completedFuture()` 立即返回。OOD 应补充说明：Phase 0 不实现超时控制，但超时阈值作为契约约束应在 §6 中标注引用位置，确保 Phase 2+ 实现者知晓。
- **修复者须知**：建议在 OOD §6 或 §8.2 中增加超时阈值索引表，引用需求 §3.4.x 的各能力超时阈值。

---

## 4. 反向边界检查

### 4.1 DTO 超前定义的设计笔记（补充说明）

- **位置**：OOD §8.2 — 全部 13 项 AI 能力的完整字段级 DTO 定义
- **说明**：OOD §8.2 为全部 13 项 AI 能力定义了完整的字段级 DTO（含输入/输出契约），其中约半数能力的真实实现归属 Phase 2~5。Phase 0 以 `MockAiService` 的固定占位数据形式存在。这种超前定义在架构层面具有合理性——为模块划分提供完整的编译期契约边界，支撑前端 TypeScript 类型同步。
- **设计权衡记录建议**：OOD 应在 §8.2 或 §7 设计决策中补充一条 ADR，说明"为何在 Phase 0 超前定义全部 13 项 AI 能力的 DTO"，并标注需求变更时的应对策略（如"需求变更时同步更新 ai-api DTO 和 MockAiService，前端通过 openapi-generator 同步类型定义"）。此举非违规或风险，而是常态化维护工作的文档化补充。

### 4.2 SecurityConfig 骨架的 Phase 1+ 预留设计（无偏离）

- **位置**：OOD §4.5 — SecurityConfigPhase0 + Phase 1 SecurityConfig profile 切换机制
- **检查发现**：OOD 为 SecurityConfig 设计了 profile 切换机制（`@Profile("phase0")` vs `@Profile("!phase0")`），使 Phase 1 的认证启用仅需调整 `spring.profiles.active` 即可。该设计属于合理的"超前预留"——OOD 在 Phase 0 骨架中包含了 Phase 1 的配置切换逻辑，但该配置切换的完整代码（Phase 1 SecurityConfig + AuthenticationEntryPoint + AccessDeniedHandler + PasswordEncoder）已在骨架中实现，超出了 Phase 0 "最小化"的边界。然而该超前实现的价值高于其维护成本（避免 Phase 1 重构 SecurityConfig），且 OOD §4.5 已明确标注切换方式。
- **结论**：此项超前实现属于合理的前瞻性设计，不构成风险，无需修复。

---

## 5. 技术风险提示

### 5.1 CSRF 未禁用——Phase 0 应添加 `.csrf(csrf -> csrf.disable())`（P1）

- **位置**：§4.5 (L768-L771)
- **事实**：Phase 0 的 `SecurityConfigPhase0` 配置如下：
  ```java
  http.authorizeHttpRequests(auth -> auth
      .anyRequest().permitAll()
  );
  ```
- **风险说明**：对于基于 JWT 的无状态 REST API，Spring Security 官方文档明确建议禁用 CSRF 保护（`csrf.disable()` 是标准配置项）。Phase 0 当前仅有 `GET /api/ping` 端点——仅从 Phase 0 运行时角度看不会触发 CSRF 拒绝问题，但该配置项属于 Phase 0 骨架就应固化到代码中的标准基线配置，而非留给 Phase 1 记忆负担。缺少 `csrf.disable()` 时，若 Phase 1 开发者忘记添加，第一个 POST 端点即出现 403 错误。
- **建议**：Phase 0 SecurityConfig 中应添加 `.csrf(csrf -> csrf.disable())`，并在注释中说明"无状态 JWT 架构，CSRF 不适用"。此举使 SecurityConfig 基线配置与 Spring Security 官方推荐实践对齐，消除 Phase 1 启动时的遗忘风险。

### 5.2 `@SQLRestriction` 仅对 Hibernate 发起的查询生效——Phase 2+ 开发者须知（P2）

- **位置**：§3.2 (L513)：`@SQLRestriction("deleted = false")` … "确保普通查询自动过滤已删除记录"
- **风险说明**：`@SQLRestriction` 是 Hibernate 注解，仅在通过 Hibernate Session 发起的查询（包括 Spring Data JPA Repository 派生方法、`@Query` JPQL 查询）中自动追加过滤条件。原生 SQL 查询（`@Query(nativeQuery = true)`）和 `EntityManager.createNativeQuery()` 不自动受 `@SQLRestriction` 约束。
- **影响评估**：Phase 0 仅提供 BaseEntity 和 Repository 骨架，不包含任何原生 SQL 查询，因此在 **Phase 0 范围内无实际影响**。该风险属于 Phase 2+ 的开发者须知——当后续阶段添加原生 SQL 查询时，若未手动附加 `WHERE deleted = false`，将产生软删除数据泄露的 bug。
- **建议**：OOD 应在 §3.2 的 `@SQLRestriction` 说明中补充注释："该注解仅对 Hibernate JPQL/派生查询生效，原生 SQL 查询需手动附加过滤条件"，使修复者（Phase 2+ 开发者）知晓该限制边界。

---

## 6. 补充说明

### A. `common` 模块 security 依赖的可选标记合理性

`common` 将 `spring-boot-starter-security` 标记为 `<optional>true</optional>` 是合理的——这使 `GlobalExceptionHandler` 可以编译引用 `AuthenticationException`/`AccessDeniedException` 类型，同时防止 API 契约子模块（common-module-api、ai-api）获取不必要的 transitive 依赖。问题仅在于 §2.2 L293 中的理由陈述（"用于 SecurityConfig"）错误（见 1.1）。

### B. Phase 0 作为最小化骨架的边界自洽性

OOD 文档在 §8.4 (L1327, L1354) 和 §3.3 (L531) 多处明确标注 Phase 0 不实现跨模块调用、不提供 `PermissionServiceImpl` 门面实现。这些边界声明与 Phase 0 的组织约束一致。

### C. 模块依赖关系图 ASCII 箭头语义说明

**说明**：§2.2 (L273-L287) 的模块依赖关系图中，底部折返箭头（从 `common-module-api` 区域指向 `ai-api`）曾被归类为"逻辑错误"。经复查，该图 L289 已有明确的伴生文本说明该折线表示"业务模块同时依赖 ai-api 与 common-module-api"，图示与文本配合后读者可正确理解依赖方向。该图使用 ASCII 字符绘制，受限于文本介质的表现力，折返路径是空间约束下的合理折中方案。**该问题不应归类为逻辑错误**，建议后续如有条件（如生成式文档工具）替换为基于 PlantUML/Mermaid 的图形化依赖图。

### D. 项目管理提醒：容器化部署脚本与本地 lint 工具配置

本项不影响 OOD 架构设计的正确性，属于路线图「推荐补齐」的项目管理提醒：

- **容器化开发部署脚本**（Docker Compose 配置）：属于 DevOps/工程工具配置，建议项目管理者在 Phase 0→Phase 1 衔接期间准备或参考 OOD §9.3 一键启动后补充 Docker Compose 占位路径。
- **本地代码质量检查工具集成**（lint 工具配置）：属于工程工具链配置，建议项目管理者在 Phase 0 验收前在独立仓库配置中完成，或确认 OOD §10 CI 占位后预留本地工具配置位置。

### E. 协作规范缺失——项目管理提醒

路线图 Phase 0 要求交付"协作规范：分支约定、Commit 格式、PR 模板、Code Review 必查项"。协作规范是团队层面的工程纪律文档，应由项目管理者或技术负责人创建独立的协作规范文档（如 `CONTRIBUTING.md`），不属于 OOD 架构设计文档的补充范围。本项作为项目管理提醒记录于此——OOD 作者无需为此修改设计文档，但项目管理者应在 Phase 0 验收前确认协作规范文档的创建责任方和时间安排。

---

## 7. 审查维度补充声明

- **事实错误维度**：经逐项排查，未发现 OOD 文档中包含不符合技术事实的描述（如错误的版本号、不存在的功能、错误的技术断言等）。
- **逻辑错误维度**：经逐项排查，未发现 OOD 文档中包含逻辑推理错误（如因果关系倒置、不成立的推导、不符合常识的假设等）。

---

## 修订说明（v3）

| 质询意见 | 回应 |
|---------|------|
| §2.2 权限矩阵 P0 优先级偏高，不阻塞 Phase 0 验收标准 | 已采纳。将 §2.2 优先级从 P0 降级为 P1，归类为 Phase 0→Phase 1 衔接准备项，并补充降级依据说明其不影响骨架运行和编译。 |
| §2.1（P1）与 §2.2（P0）存在优先级倒挂，§2.1 直接影响 Phase 0 契约冻结质量 | 已采纳。将 §2.1 优先级从 P1 升级为 P0，与 §2.2 降级构成修正，同步调整了全文中优先级标记的逻辑自洽性。升级依据引用路线图"骨架必备"对错误码命名空间的要求。 |
| §3.2 推荐补齐项（容器化脚本、lint 工具配置）超出 OOD 架构设计文档诊断边界，与 §3.1 的内部边界定义不一致 | 已采纳。将 §3.2 从"偏离路线图"类别移除，移至 §6.D 项目管理提醒，明确标注该内容属于 DevOps/工程工具配置而非 OOD 架构设计职责范围。 |
| §5.1 CSRF 建议（无需添加）与 Spring Security REST API 标准实践相悖，`csrf.disable()` 应为标准配置 | 已采纳。修订 §5.1 的建议方向：由"当前无需添加"改为"Phase 0 应添加 `.csrf(csrf -> csrf.disable())` 作为标准基线配置"，并在注释中标注 JWT 无状态架构原因。同时将优先级从 P2 调整为 P1（影响 Phase 1 启动安全）。 |
| §4.1 将 Phase 0 的正确实现描述为"风险"存在偏差，与 §4.2 对同类超前行为的判断标准不一致；全量 DTO 定义是路线图"接口契约框架"要求的核心交付物 | 已采纳。将 §4.1 从"反向边界检查－风险"降级为 §4.1 "反向边界检查－设计笔记（补充说明）"，移除 P1 优先级标记，将措辞从"需求稳定性风险"改为"设计权衡记录建议"，与 §4.2 对超前行为的判断标准保持一致。 |

---

## 修订说明（v4）

| 质询意见 | 回应 |
|---------|------|
| §2.1 与 §2.2 优先级判定采用双重标准（§2.1 以"路线图骨架必备"升级、§2.2 以"不阻塞验收标准"降级），构成内部逻辑矛盾 | 已采纳。在 §2 开头补充显式优先级判定统一标准（P0=阻塞验收标准/P1=影响衔接质量/P2=建议性改善），§2.1 和 §2.2 均基于同一标准重新评估——§2.1 阻塞"接口契约冻结"验收标准维持 P0、§2.2 不阻塞任何验收标准维持 P1，统一标准下差值合理 |
| 缺少"事实错误"和"逻辑错误"维度的显式审查结论 | 已采纳。新增 §7 "审查维度补充声明"，分别声明事实错误维度和逻辑错误维度的排查结论 |
| §3.1 协作规范缺失项对 OOD 修复者缺乏可操作性——责任方不是 OOD 作者，却以"偏离"问题呈现 | 已采纳。从 §3 "偏离路线图"中移除 §3.1，移至 §6.E "项目管理提醒"，明确标注该项由项目管理者负责，OOD 作者无需修改设计文档 |
| §1.2 JDBC 驱动 scope 不一致项涉及 Phase 1+ 配置，诊断边界交代不清 | 已采纳。在 §1.2 补充"诊断边界说明"，明确标注该问题涉及 OOD 对 Phase 1+ 配置的引用，不影响 Phase 0 交付物正确性，但属于跨阶段技术引用错误 |
