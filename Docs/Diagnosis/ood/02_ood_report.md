# Phase 0 最小化骨架 OOD 设计方案 — 诊断报告（v12）

> 诊断对象：`Docs/04_ood_phase0.md`
> 参考文档：`Docs/01_requirement.md`、`Docs/03_roadmap.md`
> 优先级标记：P0（阻塞 Phase 0 三项验收标准：三端前端可一键启动到占位首页、后端基础骨架可独立启动并响应 ping 健康检查、新人按入门引导文档可在 1 小时内完成本地环境搭建）、P1（影响 Phase 1+ 的衔接质量）、P2（建议性改善）

---

## 1. 定义矛盾

### 1.1 `common` 模块 security 依赖的陈述理由与实际架构矛盾（P2）

- **位置**：§2.2 (L293) vs §4.5 (L306-L307, L759, L785)
- **矛盾表述 A**（L293）："`common`：依赖 spring-boot-starter-web、spring-boot-starter-security（**用于 SecurityConfig**）及 spring-boot-starter-data-jpa"
- **矛盾表述 B**（L306-L307）："`common` 中**不放置阶段性 `SecurityConfig`**"
- **矛盾表述 C**（L759, L785）："SecurityConfig 设计骨架（归属 **`application`** 模块）"；"随阶段演进变化的 `SecurityFilterChain`、`AuthenticationEntryPoint`、`AccessDeniedHandler` 等安全策略配置统一放在 **`application`** 模块"
- **分析**：同份文档内对 `common` 模块为何需要 `spring-boot-starter-security` 给出了相互矛盾的理由。L293 声称"用于 SecurityConfig"，但文档多处明确 SecurityConfig 放置在 application 模块而非 common 模块。OOD §2.2 L307 给出的解释是"部分通用安全类型仍可能被复用"，该表述是文档中对 `common` 依赖 security 的正式理由。GlobalExceptionHandler 声明了 @ExceptionHandler 机制但不限于特定异常类型列表（§5.1 L806-L807 的错误分类表列出认证/授权错误类别），而 SecurityConfig 位于 application 模块并负责实际的安全异常处理（§4.5 L784）。因此 common 依赖 security 的具体使用场景在 OOD 中以通用复用需求概括，未细化到特定处理器方法。该矛盾不影响可执行性（optional 依赖的实际编译行为正确），但属于文档内部事实陈述冲突。
- **修复者须知**：L293 中"用于 SecurityConfig"的措辞应修正，与 §2.2 L307"部分通用安全类型仍可能被复用"的表述保持一致。

### 1.2 JDBC 驱动依赖 scope 策略前后不一致（P2）

- **位置**：§9.1 (L1403-L1411) vs §9.1 (L1413)
- **矛盾表述**：H2 内存数据库以 `runtime` scope 引入（L1407-L1411），这是 JDBC 驱动的标准做法。但同节 L1413 说明 Phase 1+ 切换 MySQL/PostgreSQL 时驱动以 `compile`（默认）scope 引入。同一类依赖（JDBC 驱动）使用了不同的 scope 策略。虽然 JDBC 驱动声明在 `application/pom.xml`（叶子模块，无其他模块依赖该模块），transitive 依赖传播在此场景下不构成实质风险，但同一文档中同类依赖使用不同的 scope 策略属于依赖管理风格不一致——JDBC 驱动仅在运行时需要（非编译期 API 引用），统一使用 `runtime` 能使文档体现一致的 Maven 依赖实践约定，也与 §2.2 模块依赖治理目标的精神方向一致。
- **修复者须知**：JDBC 驱动应统一使用 `runtime` scope，无论 H2 还是 MySQL/PostgreSQL。L1413 的 `compile` scope 应修订为 `runtime`。

> **诊断边界说明**：MySQL/PostgreSQL 驱动 scope 不一致项涉及 OOD 对 Phase 1+ 配置的引用描述（Phase 0 仅使用 H2 内存数据库）。该问题不影响 Phase 0 交付物的运行正确性，无 transitive 传播风险，对 Phase 1+ 开发者的实际影响极低——OOD 当前的 scope 不一致不会导致任何编译或运行错误。归类为 P2（建议性改善）而非 P1，因其对 Phase 1+ 衔接质量的实质影响轻微。建议统一为 `runtime` 以保持文档一致性和最佳实践。

---

## 2. 偏离需求文档（需求-OOD 映射缺失）

> **优先级判定统一标准**：P0 为"阻塞 Phase 0 三项验收标准（三端前端可一键启动到占位首页、后端基础骨架可独立启动并响应 ping 健康检查、新人按入门引导文档可在 1 小时内完成本地环境搭建）"；P1 为"影响 Phase 1+ 衔接质量"；P2 为"建议性改善"。以下各项均基于此统一标准判定。该标准直接引用 `03_roadmap.md#L53` 的 Phase 0 实际验收标准，经逐项对照核实。

### 2.1 错误码命名约定衔接缺口（P1）

- **位置**：需求 §3.4 (L814-L818) vs OOD §8.2
- **偏离说明**：需求文档要求在全部 13 项 AI 能力错误码中统一携带 `_AI_` 中段（如 `TRIAGE_AI_TIMEOUT`），并对已有不符合约定的能力完成修订（L818 指出 3.4.2/3.4.3/3.4.9/3.4.10/3.4.11 以及 3.4.1 内部混用的 `TRIAGE_INPUT_INVALID` 均在需求文档中统一修订）。OOD 文档虽在 §5 定义了 `ErrorCode` 接口和各模块 enum 实现策略，但 §8.2 各 AI 能力 DTO 未显式列出对应错误码清单，也未说明 Phase 0 各能力 Mock 实现的错误码返回值需遵循 `_AI_` 命名约定。该缺口属于需求文档中已明确的错误码命名约定在 OOD 中的映射缺失——修复者（Phase 2+ 的 AI 实现开发者）需要返回符合约定的错误码，但 OOD 未要求 Phase 0 的 MockAiService 返回符合命名约定的占位错误码。
- **降级依据**：错误码命名约定不影响 Phase 0 三项验收标准中的任何一项（不阻塞前端启动、ping 健康检查、新人上手）。该缺口属于需求文档中已明确的错误码命名约定在 OOD 中的映射缺失——需求明确要求 `_AI_` 命名约定，OOD 未在 §8.2 和 §5 中将该约定贯穿到各 AI 能力的错误码定义。该映射缺失不阻塞 Phase 0 验收标准，分类为 P1。
- **修复者须知**：OOD 应在 §8.2 各 AI 能力 DTO 中补充对应错误码清单。错误码命名约定变更频率低（在需求层面固化后趋于稳定），且与接口契约天然耦合，嵌入 OOD 文档后维护负担可控。

### 2.2 权限矩阵与需求规格对齐缺口（P1）

- **位置**：需求 §2.6 功能-角色可访问矩阵 vs OOD §3.3 权限模型核心抽象
- **偏离说明**：需求文档定义了完整的"角色—岗位—功能"三级权限可访问矩阵（含 7 类角色 × 28 项功能的 ✓/✗/○¹/○² 映射），并明确定义了 `○¹`（创建者/责任人）与 `○²`（接诊/经手人）两种受限访问语义。OOD §3.3 虽然正确地采用了三级模型骨架，但未列出需求 §2.6 矩阵中全部 7 类角色的权限边界映射，仅以"三级模型"抽象描述，未验证 OOD 设计中的权限判断逻辑能否覆盖矩阵中所有 ✓/○¹/○² 约束。
- **降级依据**：该缺口属于 Phase 1 数据播种与验证清单层面的映射缺失——OOD 已提供正确的三级模型骨架，但未将需求 §2.6 的矩阵映射关系显式贯穿到 OOD 设计中。不影响骨架运行和编译，不阻塞 Phase 0 三项验收标准中的任何一项。分类为 P1。
- **修复者须知**：建议在 OOD §3.3 中补充权限矩阵覆盖率声明，确认设计对需求 §2.6 矩阵（7 类角色 × 28 项功能）中所有 ✓/○¹/○² 约束的覆盖情况（以简要声明确认覆盖率即可，无需展开完整映射表）。该补充变更频率低（权限矩阵在需求层面稳定），与接口契约天然耦合，嵌入 OOD 文档的维护负担可控。

### 2.3 DTO 字段约束与需求契约对齐检查（P1）

- **位置**：需求 §3.4.1–§3.4.13 vs OOD §8.2
- **偏离说明**：OOD §8.2 定义了 13 项 AI 能力的 DTO 结构，整体字段集与需求 §3.4.x 一致，但以下约束未在 OOD 中体现或存在差异：
  - 需求 §3.4.1 `TriageRequest.chiefComplaint` 字符数约束为 5–500（L825），OOD §8.2 的 `TriageRequest` 字段列表未标注该长度约束。
  - 需求 §3.4.3 `MedicalRecordGenRequest.dialogueText` 字符数约束为 50–10000（L861），OOD §8.2 未标注。
  - 需求 §3.4.13 `DiscussionConclusionResponse.discussionText` 最低质量门槛 ≥ 50 字符（L1060），OOD §8.2 在文本层面已将 `DISCUSS_AI_OUTPUT_INCOMPLETE` 错误码与 50 字符阈值建立关联（L1274），并标注了"最低质量门槛 >= 50 字符，建议 >= 100 字符"（L1257），语义与需求的"< 50 返回 `DISCUSS_AI_OUTPUT_INCOMPLETE`"一致。该阈值约束已存在于文本描述层，但未落实为 DTO 字段级的校验注解。
  - 需求 §3.4.4 `DiagnosisRequest.condition.chiefComplaint` 字符数 ≥ 5（L890），OOD 未标注。
- **降级依据**：DTO 字段约束标注缺失不影响 Phase 0 的三项验收标准。Phase 0 的 MockAiService 以固定占位数据运行，不涉及真实的字段校验逻辑——例如 `TriageRequest.chiefComplaint` 的 5–500 字符约束在 Mock 模式下无运行时差异。OOD §8.2 的 DTO 字段集与需求一致，仅缺少约束标注，该缺失属于需求文档中已定义的字段约束在 OOD 中的映射缺漏。分类为 P1。
- **额外验证通过项**：需求 §3.4.x 多处使用 `@Nullable` 或"可选"标记字段（如 `TriageRequest.additionalResponses`、`DiagnosisRequest.encounterId`），OOD DTO 定义在字段级标注了 `@Nullable`，Mock 数据占位约定（§3.4 L629）中对"标注 `@Nullable` 的字段返回 null"的约定与需求的"可选"语义一致，该一致性已验证通过。
- **修复者须知**：OOD 应在 §8.2 的 DTO 字段描述中补充各字段的必填/长度约束（从需求 §3.4.x 直接引用），并在 §8.2 统一约束补充部分（L1272）增加字段约束索引表。字段长度约束与 DTO 接口契约天然耦合（字段及其约束共同构成接口契约），且后续需求变更频率相对较低，嵌入 OOD 文档的维护负担可控。

## 3. 反向边界检查

### 3.1 DTO 超前定义的设计说明（补充说明）

- **位置**：OOD §8.2 — 全部 13 项 AI 能力的完整字段级 DTO 定义
- **说明**：OOD §8.2 为全部 13 项 AI 能力定义了完整的字段级 DTO（含输入/输出契约），其中约半数能力的真实实现归属 Phase 2~5。Phase 0 以 `MockAiService` 的固定占位数据形式存在。这种超前定义在架构层面具有合理性——为模块划分提供完整的编译期契约边界，支撑前端 TypeScript 类型同步。
- **可选的文档补充建议**：可在 §7 设计决策表的备注字段中简要记录"超前定义全部 DTO 的动机"（如"为前端提供完整的编译期契约边界"），不另立独立 ADR。该建议为可选的文档补充项，不影响 Phase 0 交付质量。

### 3.2 SecurityConfig 骨架的 Phase 1+ 预留设计（无偏离）

- **位置**：OOD §4.5 — SecurityConfigPhase0 + Phase 1 SecurityConfig profile 切换机制
- **检查发现**：OOD 为 SecurityConfig 设计了 profile 切换机制（`@Profile("phase0")` vs `@Profile("!phase0")`），使 Phase 1 的认证启用仅需调整 `spring.profiles.active` 即可。该设计属于合理的"超前预留"——OOD 在 Phase 0 骨架中包含了 Phase 1 的配置切换逻辑，但该配置切换的完整代码（Phase 1 SecurityConfig + AuthenticationEntryPoint + AccessDeniedHandler + PasswordEncoder）已在骨架中实现，超出了 Phase 0 "最小化"的边界。然而该超前实现的价值高于其维护成本（避免 Phase 1 重构 SecurityConfig），且 OOD §4.5 已明确标注切换方式。
- **结论**：该设计在架构层面是合理的前瞻性安排，但包含一个 P1 级别的具体实现风险（详见下方风险提示）。
- **风险提示（P1）**：该超前实现中包含的 `PasswordEncoder` Bean 在 Phase 0 的 `permitAll` 模式下不会被实际调用。若 `PasswordEncoder` 配置存在错误（如编码器算法选择不当、BCrypt 强度参数异常），该错误将在 Phase 0 被隐藏，直到 Phase 1 启用认证时才暴露。该风险隐蔽性高、影响范围大（认证环节的核心组件），优先级判定为 P1——虽然不阻塞 Phase 0 验收标准，但配置错误在 Phase 1 启用认证时可能暴露为难以追溯的认证失败问题，且 Phase 0 阶段排查该配置无额外成本。PasswordEncoder 与 AuthenticationEntryPoint、AccessDeniedHandler 同属两个 profile 共享的配置（§4.5 L780："共享给两个 profile 的 SecurityConfig 使用"），在默认 profile 下即可测试 Bean 创建和编码/匹配功能（无需激活 `!phase0` Profile）。建议在 Phase 0 中添加独立的单元测试验证 PasswordEncoder 配置正确性。

---

## 4. 技术风险提示

### 4.1 CSRF 未禁用——Phase 0 应添加 `.csrf(csrf -> csrf.disable())`（P1）

- **位置**：§4.5 (L768-L771)
- **事实**：Phase 0 的 `SecurityConfigPhase0` 配置如下：
  ```java
  http.authorizeHttpRequests(auth -> auth
      .anyRequest().permitAll()
  );
  // 复用 AuthenticationEntryPoint、AccessDeniedHandler、CORS 等基础配置
  return http.build();
  ```
- **风险说明**：对于基于 JWT 的无状态 REST API，Spring Security 官方文档明确建议禁用 CSRF 保护（`csrf.disable()` 是标准配置项）。Phase 0 当前仅有 `GET /api/ping` 端点——仅从 Phase 0 运行时角度看不会触发 CSRF 拒绝问题，但该配置项属于 Phase 0 骨架就应固化到代码中的标准基线配置，而非留给 Phase 1 记忆负担。缺少 `csrf.disable()` 时，若 Phase 1 开发者忘记添加，第一个 POST 端点即出现 403 错误。
- **已有配置上下文**：OOD §4.5（L772-L784）已包含 CORS 配置（`CorsConfigurationSource` Bean，允许前端三端开发服务器域名）、`AuthenticationEntryPoint` 和 `AccessDeniedHandler`（返回与 `GlobalExceptionHandler` 一致的 `Result<T>` 格式）。这些配置在被 `http.build()` 打包时，若未显式调用 `csrf.disable()`，Spring Security 6 的默认行为是启用 CSRF。添加 `csrf.disable()` 后需确认与已有 CORS 配置的兼容性——CSRF 禁用后 CORS 预检请求（OPTIONS）不再受 CSRF 保护影响，但 CORS 配置本身的跨域规则仍需正确设置。该兼容性在 Phase 0 的 `permitAll` 模式下无运行时冲突，但应为 SecurityConfig 基线的一部分固化下来。
- **建议**：Phase 0 SecurityConfig 中应添加 `.csrf(csrf -> csrf.disable())`，并在注释中说明"无状态 JWT 架构，CSRF 不适用"。此举使 SecurityConfig 基线配置与 Spring Security 官方推荐实践对齐，消除 Phase 1 启动时的遗忘风险。
- **远期风险脚注**：若后续阶段引入非 JWT 认证端点（如 OAuth2 回调、Webhook 接收等），需评估这些端点是否需要独立的 CSRF 保护策略。届时建议通过多 SecurityFilterChain 配置区分无状态 API 和有状态端点，各自独立控制 CSRF 策略。

### 4.2 `@SQLRestriction` 仅对 Hibernate 发起的查询生效——Phase 2+ 开发者须知（P2）

- **位置**：§3.2 (L513)：`@SQLRestriction("deleted = false")` … "确保普通查询自动过滤已删除记录"
- **风险说明**：`@SQLRestriction` 是 Hibernate 注解，仅在通过 Hibernate Session 发起的查询（包括 Spring Data JPA Repository 派生方法、`@Query` JPQL 查询）中自动追加过滤条件。原生 SQL 查询（`@Query(nativeQuery = true)`）和 `EntityManager.createNativeQuery()` 不自动受 `@SQLRestriction` 约束。
- **影响评估**：Phase 0 仅提供 BaseEntity 和 Repository 骨架，不包含任何原生 SQL 查询，因此在 **Phase 0 范围内无实际影响**。该风险属于 Phase 2+ 的开发者须知——当后续阶段添加原生 SQL 查询时，若未手动附加 `WHERE deleted = false`，将产生软删除数据泄露的 bug。
- **建议**：OOD 应在 §3.2 的 `@SQLRestriction` 说明中补充注释："该注解仅对 Hibernate JPQL/派生查询生效，原生 SQL 查询需手动附加过滤条件"，使修复者（Phase 2+ 开发者）知晓该限制边界。

---

## 5. 补充说明

### A. `common` 模块 security 依赖的可选标记合理性

`common` 将 `spring-boot-starter-security` 标记为 `<optional>true</optional>` 是合理的——部分通用安全类型在 common 中仍可能被复用（§2.2 L307），同时将 security 标记为 optional 可防止 API 契约子模块（common-module-api、ai-api）获取不必要的 transitive 依赖。问题仅在于 §2.2 L293 中的理由陈述（"用于 SecurityConfig"）错误（见 1.1）。

### B. 项目管理提醒

本项不影响 OOD 架构设计的正确性，属于路线图「推荐补齐」的项目管理提醒：

- **容器化开发部署脚本**（Docker Compose 配置）：属于 DevOps/工程工具配置，建议项目管理者在 Phase 0→Phase 1 衔接期间准备或参考 OOD §9.3 一键启动后补充 Docker Compose 占位路径。
- **本地代码质量检查工具集成**（lint 工具配置）：属于工程工具链配置，建议项目管理者在 Phase 0 验收前在独立仓库配置中完成，或确认 OOD §10 CI 占位后预留本地工具配置位置。
- **协作规范缺失**：路线图 Phase 0 要求交付"协作规范：分支约定、Commit 格式、PR 模板、Code Review 必查项"。协作规范是团队层面的工程纪律文档，应由项目管理者或技术负责人创建独立的协作规范文档（如 `CONTRIBUTING.md`），不属于 OOD 架构设计文档的补充范围。OOD 作者无需为此修改设计文档，但项目管理者应在 Phase 0 验收前确认协作规范文档的创建责任方和时间安排。

---

## 6. 审查维度补充声明

> **排查范围**：OOD 全文 §1~§10；交叉核对文档：`01_requirement.md` 和 `03_roadmap.md` 中所有被 OOD 引用的章节。

- **事实错误维度**：经逐项排查，未发现 OOD 文档中包含不符合技术事实的描述（如错误的版本号、不存在的功能、错误的技术断言等）。**限定说明**：§1.1 中 `common` 模块 security 依赖理由与 SecurityConfig 实际归属的不一致属于"定义矛盾"维度（同一文档内部陈述冲突），已在 §1 相应位置处理，不影响本维度"未发现违背技术事实的内容"的结论。
- **逻辑错误维度**：经逐项排查，未发现 OOD 文档中包含逻辑推理错误（如因果关系倒置、不成立的推导、不符合常识的假设等）。
- **补充说明**：以下两项为审查过程中的辅助确认，已在 §5 历史版本中单独列出，现合并为本声明的补充脚注：
  - **Phase 0 边界自洽性**：OOD 在 §8.4 (L1327, L1354) 和 §3.3 (L531) 多处明确标注 Phase 0 不实现跨模块调用、不提供 `PermissionServiceImpl` 门面实现，这些边界声明与 Phase 0 的组织约束一致，进一步支撑了"未发现偏离路线图"的审查结论。
  - **ASCII 箭头语义澄清**：§2.2 (L273-L287) 的模块依赖关系图中底部折返箭头曾被关注，但该图 L289 已有明确的伴生文本说明折线含义，图示与文本配合后读者可正确理解依赖方向。该图受限于 ASCII 文本介质的表现力，折返路径是空间约束下的合理折中方案，不构成逻辑错误。

---

## 7. 偏离路线图 Phase 0 阶段审查声明

经逐项对照 `03_roadmap.md` 中 Phase 0 的交付物清单（§0.2 "骨架必备"）与排除清单（§0.4 "本阶段明确不包含"），审查结论如下：

- OOD 文档的整体范围与路线图 Phase 0 的骨架定位一致，未包含任何路线图明确排除的内容（如业务功能、真实 AI 接入、微服务化拆分等）。
- 个别设计超出了 Phase 0 最小化骨架的最小范围（如 Phase 1 SecurityConfig profile 切换机制的完整代码——含 AuthenticationEntryPoint、AccessDeniedHandler、PasswordEncoder 的提前实现），但属于合理的前瞻性设计（见 §3.2），不构成对路线图 Phase 0 边界的实质性偏离。
- OOD §8.2 对所有 13 项 AI 能力的 DTO 超前定义，属于路线图 Phase 0 "骨架必备"中"AI 能力模块 Mock 占位"和"接口契约框架"的自然延伸（路线图 §0.2 明确 AI 能力模块 Mock 占位"可跨阶段持续完善，不阻塞 Phase 0 骨架验收"），不构成偏离。
- 路线图 Phase 0 "推荐补齐"中的各项（日志聚合框架占位、基础监控埋点接入、API 文档自动生成、容器化开发部署脚本、本地代码质量检查工具集成）未在 OOD 中强制要求实现，不影响验收标准，不构成偏离。这些项目已在 §5.B 中以项目管理提醒形式记录。

**总体结论**：OOD 文档未偏离路线图 Phase 0 阶段的范围定义，不存在"包含不应在本阶段实现的内容"或"遗漏本阶段应包含的核心内容"的实质性问题。

---

## 8. 跨阶段衔接建议

> 以下各项属于 Phase 0→Phase N 的衔接准备建议，不构成对 Phase 0 OOD 的实质性偏离。修复者可结合 Phase 1+ 的实际启动时间评估处理优先级。

### 8.1 实体交付阶段映射表（P1）

- **位置**：需求 §5.1 核心业务实体 vs OOD 实体设计
- **说明**：需求 §5.1 列出了 18+ 核心业务实体及其关键字段和关系，OOD 在 §3.2（BaseEntity）和 §3.3（权限模型实体）中仅覆盖了 User、Role、Post、Function 四个实体，其余业务实体（挂号记录、处方、病历、检查/检验申请与报告、缴费记录、健康档案、药库/药房库存等）未在 Phase 0 OOD 中定义。Phase 0 作为最小化骨架不需要实现全部实体，但 OOD 未说明哪些实体属于 Phase 0 范围、哪些留待 Phase 1+ 补充。修复者（Phase 1 的开发者）需要自行判断哪些实体需在 Phase 1 起即具备骨架结构。
- **影响评估**：该缺口不影响 Phase 0 的三项验收标准（三端前端可一键启动、后端 ping 健康检查、新人环境搭建）。但直接影响 Phase 1+ 的衔接质量——Phase 1 开发者需自行交叉对照需求文档和路线图来确定实体搭建顺序。
- **修复者须知**：建议在 Phase 1 启动前创建实体交付阶段映射表。优先路径：另建独立文档（如 `Docs/entity_stage_mapping.md`）列出全部 18+ 核心实体及其归属阶段，由项目管理者确认生命周期和更新责任人。备选路径：若独立文档的外部决策流程无法在期望时间线内完成，可在 OOD 附录中增加占位声明（如"18+ 核心实体的阶段归属映射见独立文档，创建前暂以需求 §5.1 为参考"），降低对项目管理决策的依赖风险。

### 8.2 非功能指标对齐检查（P2）

- **位置**：需求 §3.4.x 各节"服务质量要求" vs OOD 非功能设计
- **说明**：需求 §3.4.x 为每项 AI 能力定义了超时阈值（P95 与硬超时两档），如 3.4.1 P95 ≤ 5 秒/硬超时 ≤ 8 秒（L830）、3.4.2 P95 ≤ 3 秒/硬超时 ≤ 6 秒（L844）等。OOD 文档仅在 §6 并发设计中提到"Phase 2+ 引入 Spring Async 等异步机制"，未显式引用需求中的超时阈值。Phase 0 虽不实现真实 AI 调用，但 `AiService` 接口的 `CompletableFuture<AiResult<T>>` 契约已预留异步形态，MockAiService 使用 `CompletableFuture.completedFuture()` 立即返回。
- **影响评估**：不影响 Phase 0 三项验收标准。属于 Phase 2+ 实现者的信息完整性补充。
- **修复者须知**：建议在 §8.2 末尾或附录中增加"AI 能力超时阈值索引表"，集中列出各 AI 能力的超时阈值（引用需求 §3.4.x），同时在 §6 并发设计中标注"各 AI 能力超时阈值见 §8.2 超时阈值索引表"。索引表作为超时阈值的唯一记录点，避免与 DTO 接口契约结构耦合，降低后续需求变更时的维护成本——当需求 §3.4.x 中的超时阈值迭代变化时，仅需更新索引表一处，无需触及各 DTO 定义。

### 8.3 权限模型 Phase 1 数据种子、跨模块授权与扩展点验证（P2）

- **来源**：需求 §2.6 功能-角色可访问矩阵中多项原归类于 §2.2 的缺口，经重新分类确认属于 Phase 1+ 衔接准备项，非 Phase 0 OOD 的实质性偏离。
- **线下接诊医生角色数据播种**：OOD §3.3 的权限模型框架（User/Role/Post/Function 四级模型）已具备承载该子岗位的建模能力，具体角色数据初始化属于 Phase 1 数据种子任务。建议 Phase 1 启动时在数据种子初始化逻辑中将"线下接诊医生"纳入 `Post` 枚举定义，并在 §3.3 末尾补充对 Phase 1 数据种子的指引说明。
- **药房医生跨模块授权路径**：需求矩阵中"药房医生"可访问"查看检查/检验结果"（○²），该路径涉及处方→就诊→检查/检验报告的跨模块权限判断。OOD 多处声明 Phase 0 暂不实现跨模块调用，建议在 Phase 1 实现 `DataPermissionEvaluator` 扩展点时将该跨模块授权路径纳入考虑。
- **DataPermissionEvaluator 接口语义覆盖验证**：需求 §2.6 中 `○¹`（创建者/责任人）与 `○²`（接诊/经手人）属于不同的数据范围判定维度，Phase 1 实现 `DataPermissionEvaluator` 扩展点时需验证其接口签名是否能承载这两种语义差异，可能需独立的 Evaluator 方法或参数区分。
- **修复者须知**：以上三项属于 Phase 1 数据种子和权限扩展的实现参考，不影响 Phase 0 验收标准。建议 Phase 1 开发者将本节作为参考备忘录：（1）在数据种子初始化逻辑中将"线下接诊医生"纳入 `Post` 枚举定义；（2）在实现 `DataPermissionEvaluator` 扩展点时，验证其接口签名能承载 ○¹/○² 的语义差异，并将药房医生查看检查/检验结果的跨模块授权路径纳入测试用例覆盖范围。

---
