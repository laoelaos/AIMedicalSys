# Phase 0 最小化骨架 OOD 设计方案 — 诊断报告

> 诊断对象：`Docs/04_ood_phase0.md`
> 参考文档：`Docs/01_requirement.md`、`Docs/03_roadmap.md`

---

## 1. 定义矛盾

### 1.1 `common` 模块 security 依赖的陈述理由与实际架构矛盾

- **位置**：§2.2 (L293) vs §4.5 (L306-L307, L759, L785)
- **矛盾表述 A**（L293）："`common`：依赖 spring-boot-starter-web、spring-boot-starter-security（**用于 SecurityConfig**）及 spring-boot-starter-data-jpa"
- **矛盾表述 B**（L306-L307）："`common` 中**不放置阶段性 `SecurityConfig`**"
- **矛盾表述 C**（L759, L785）："SecurityConfig 设计骨架（归属 **`application`** 模块）"；"随阶段演进变化的 `SecurityFilterChain`、`AuthenticationEntryPoint`、`AccessDeniedHandler` 等安全策略配置统一放在 **`application`** 模块"
- **分析**：同份文档内对 `common` 模块为何需要 `spring-boot-starter-security` 给出了相互矛盾的理由。L293 声称"用于 SecurityConfig"，但文档多处明确 SecurityConfig 放置在 application 模块而非 common 模块。`common` 模块引用 Spring Security 类型的实际原因是 `GlobalExceptionHandler`（common.config 包）中的 `@ExceptionHandler` 方法需要捕获 `AuthenticationException` 和 `AccessDeniedException` 类型（§5.1 L806-L807），而非"用于 SecurityConfig"。该矛盾不影响可执行性（optional 依赖的实际编译行为正确），但属于文档内部事实陈述冲突，修复者需明确 L293 中"用于 SecurityConfig"的措辞应修正为"用于 GlobalExceptionHandler 捕获安全异常类型"。

### 1.2 JDBC 驱动依赖 scope 策略前后不一致

- **位置**：§9.1 (L1403-L1411) vs §9.1 (L1413)
- **矛盾表述**：H2 内存数据库以 `runtime` scope 引入（L1407-L1411），这是 JDBC 驱动的标准做法。但同节 L1413 说明 Phase 1+ 切换 MySQL/PostgreSQL 时驱动以 `compile`（默认）scope 引入。同一类依赖（JDBC 驱动）使用了不同的 scope 策略，且 `compile` 会导致下游模块通过 transitive 依赖引入不必要的 JDBC 驱动包，与 §2.2 模块依赖治理目标不一致。JDBC 驱动应统一使用 `runtime` scope。

---

## 2. 事实错误

### 2.1 Phase 0 SecurityConfig 未禁用 CSRF，将阻塞后续阶段 POST 端点

- **位置**：§4.5 (L768-L771)
- **事实**：Phase 0 的 `SecurityConfigPhase0` 配置如下：
  ```java
  http.authorizeHttpRequests(auth -> auth
      .anyRequest().permitAll()
  );
  ```
- **问题**：Spring Security 6+ 默认启用 CSRF 保护（`CsrfFilter`）。虽然配置了 `permitAll()`，但 CSRF 保护是针对状态变更请求（POST/PUT/DELETE）在 filter 链中先于授权检查执行的。即使所有请求都被允许访问，POST 请求仍会因缺少 CSRF token 而被返回 403。Phase 0 当前仅有 `GET /api/ping` 端点，不会触发该问题，但当 Phase 1 添加 POST 业务端点（如登录、注册）时，第一个集成开发者将直接遇到 CSRF 403 错误且排查方向不直观。
- **修复方向**：在 Phase 0 SecurityConfig 中显式添加 `.csrf(csrf -> csrf.disable())`，或添加注释标记该骨架为 API 无状态认证架构、CSRF 不适用。真实的 CSRF 禁用配置应在骨架中完成而非留到 Phase 1 排查。

### 2.2 `@SQLRestriction` 仅对 Hibernate 发起的查询生效，原生 SQL 不受影响

- **位置**：§3.2 (L513)：`@SQLRestriction("deleted = false")` ... "确保普通查询自动过滤已删除记录"
- **问题**：`@SQLRestriction` 是 Hibernate 注解，仅在通过 Hibernate Session 发起的查询（包括 Spring Data JPA Repository 派生方法、`@Query` JPQL 查询）中自动追加过滤条件。原生 SQL 查询（`@Query(nativeQuery = true)`）和 `EntityManager.createNativeQuery()` 不自动受 `@SQLRestriction` 约束。文档中的"普通查询"措辞过于宽泛，未说明该限制。修复者如后续添加原生 SQL 查询时若未手动附加 `WHERE deleted = false`，将产生软删除数据泄露的 bug。

---

## 3. 逻辑错误

### 3.1 模块依赖关系图 ASCII 箭头语义不清晰

- **位置**：§2.2 (L273-L287)
- **问题**：依赖图的底部（L278-L280）从 `common-module-api` 所属区域引出一条折返箭头指向 `ai-api`：
  ```
                    ↑          ↑          ↑          ↑                        │
              modules/  modules/  modules/  modules/                         │
              patient   doctor    admin      ai/ai-api ←─────────────────────┘
  ```
  该图可能被误读为"`common-module-api` → `ai-api`"存在依赖关系。L289 的文本说明解释该折线实际表示"业务模块同时依赖 ai-api 与 common-module-api"，但图示缺失从业务模块区域到 ai-api 的独立箭头，而是复用了 common-module-api 上方的括弧空间再折返。修复者在后续维护模块依赖时可能因图示歧义引入错误依赖方向。

---

## 4. 偏离需求文档

### 4.1 需求文档 §3.4 错误码命名约定与 OOD 的衔接缺口

- **位置**：需求 §3.4 (L814-L818) 定义 `_AI_` 中段命名约定 vs OOD §8.2
- **偏离说明**：需求文档要求在全部 13 项 AI 能力错误码中统一携带 `_AI_` 中段（如 `TRIAGE_AI_TIMEOUT`），并对已有不符合约定的能力完成修订（L818 指出 3.4.2/3.4.3/3.4.9/3.4.10/3.4.11 以及 3.4.1 内部混用的 `TRIAGE_INPUT_INVALID` 均在需求文档中统一修订）。OOD 文档虽在 §5 定义了 `ErrorCode` 接口和各模块 enum 实现策略，但 §8.2 各 AI 能力 DTO 未显式列出对应错误码清单，也未说明 Phase 0 各能力 Mock 实现的错误码返回值需遵循 `_AI_` 命名约定。这属于接口契约冻结时的遗漏——修复者（Phase 2+ 的 AI 实现开发者）需要返回符合约定的错误码，但 OOD 未要求 Phase 0 的 MockAiService 返回符合命名约定的占位错误码。

### 4.2 其余偏差

- OOD 对各字段级 DTO（§8.2）的设计与需求 §3.4.x 各能力输入/输出契约在字段名、类型、必填/可选标记方面整体一致（通过 Jackson snake_case 映射）。未发现与需求文档相悖的重大偏离。

---

## 5. 偏离路线图 Phase 0 阶段

### 5.1 路线图要求的协作规范在 OOD 中完全缺失

- **位置**：路线图 §Phase 0 "做完后新增什么能力"→ "协作规范"
- **偏离**：路线图将"协作规范：分支约定、Commit 格式、PR 模板、Code Review 必查项"列为 Phase 0 骨架必备交付项之一。OOD 文档未包含任何协作规范内容（分支策略、Commit 规范、PR 模板引用、Code Review 检查单等均未提及）。修复者需在独立文档（如 `CONTRIBUTING.md` 或 `docs/` 下）补充这些规范，或由项目管理者另行协调。

### 5.2 路线图"推荐补齐"项有遗漏

- **位置**：路线图 §Phase 0 "推荐补齐"
- **偏离**：路线图列出以下推荐补齐项，OOD 文档未涉及：
  - "容器化开发部署脚本"（容器编排配置，支持本地一键启动开发环境）
  - "本地代码质量检查工具集成"（代码规范检查工具配置）
- **说明**：上述两项为推荐补齐项（"可在后续阶段首期补齐"），不阻塞骨架验收。但 OOD 作为 Phase 0 设计文档应至少提及或预留扩展点，以便修复者知晓该方向的需求。

---

## 补充说明（不在上述 5 类范围内但对修复者有参考价值）

### A. `common` 模块 security 依赖的可选标记合理性
`common` 将 `spring-boot-starter-security` 标记为 `<optional>true</optional>` 是合理的——这使 `GlobalExceptionHandler` 可以编译引用 `AuthenticationException`/`AccessDeniedException` 类型，同时防止 API 契约子模块（common-module-api、ai-api）获取不必要的 transitive 依赖。问题仅在于 §2.2 L293 中的理由陈述（"用于 SecurityConfig"）错误。

### B. Phase 0 作为最小化骨架的边界自洽性
OOD 文档在 §8.4 (L1327, L1354) 和 §3.3 (L531) 多处明确标注 Phase 0 不实现跨模块调用、不提供 `PermissionServiceImpl` 门面实现。这些边界声明与 Phase 0 的组织约束一致。

---

**诊断完成**：以上 5 类共 8 项定位结果，修复者可依此定位到 OOD 文档中的具体位置并理解"改哪里"和"为什么"。
