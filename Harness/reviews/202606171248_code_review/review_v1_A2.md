# R1-A2: POM Structure & Dependency Management

审查时间：2026-06-17

### 审查范围

- `AIMedical/backend/pom.xml` — Parent POM
- `AIMedical/backend/common/pom.xml` — Common module
- `AIMedical/backend/common-module-api/pom.xml` — common-module-api
- `AIMedical/backend/common-module-impl/pom.xml` — common-module-impl
- `AIMedical/backend/ai-api/pom.xml` — AI API module
- `AIMedical/backend/ai-impl/pom.xml` — AI Impl module
- `AIMedical/backend/patient/pom.xml` — Patient module
- `AIMedical/backend/doctor/pom.xml` — Doctor module
- `AIMedical/backend/admin/pom.xml` — Admin module
- `AIMedical/backend/application/pom.xml` — Application module
- `AIMedical/backend/integration/pom.xml` — Integration test module
- — Aggregator POMs (modules/common-module/pom.xml, modules/ai/pom.xml): **NOT FOUND / DO NOT EXIST**

### 发现

#### [一般] 父 POM modules 路径与设计 §2.1 不一致

- **位置**：`AIMedical/backend/pom.xml:18-29`
- **描述**：设计文档 §2.1 明确使用分层目录结构 `modules/common-module/common-module-api`、`modules/ai/ai-api`、`modules/patient` 等，但实际 modules 声明为扁平路径 `backend/common-module-api`、`backend/ai-api`、`backend/patient`（即模块直接位于 `backend/` 根目录，无 `modules/` 中间层）。实际目录结构也证实不存在 `modules/` 目录。
- **建议**：根据设计 §2.1 重构目录布局，将业务模块统一移至 `backend/modules/` 下。若设计已变更为扁平布局，需同步更新 OOD 文档并说明理由。

#### [一般] 聚合 POM 缺失

- **位置**：`modules/common-module/pom.xml`、`modules/ai/pom.xml`（均不存在）
- **描述**：设计 §2.1 明确指定 `modules/common-module/pom.xml` 聚合 common-module-api 和 common-module-impl，以及 `modules/ai/pom.xml` 聚合 ai-api 和 ai-impl。两个聚合 POM 均未创建。由于实际采用扁平目录结构且根 POM 直接引用叶子模块，聚合 POM 的功能完全缺失。
- **建议**：若保留扁平布局则无需聚合 POM，需要更新设计文档移除该约定；若回归设计则按 §2.1 代码片段创建聚合 POM 并调整目录。

#### [一般] Spring Boot 版本与设计不一致

- **位置**：`AIMedical/backend/pom.xml:9`
- **描述**：设计文档 §2.1 父 POM 骨架指定 `spring-boot-starter-parent` 版本为 `3.3.0`，实际 POM 使用 `3.2.5`。
- **建议**：更新为 `3.3.0` 或更新设计文档以反映实际版本决策。

#### [一般] dependencyManagement 缺少外部 starter 统一声明

- **位置**：`AIMedical/backend/pom.xml:39-101`
- **描述**：设计 §2.2 "依赖管理（父 POM）"要求父 POM 的 `<dependencyManagement>` 统一声明 `spring-boot-starter-web`、`spring-boot-starter-data-jpa`、`spring-boot-starter-security`、`spring-boot-starter-validation`、`spring-boot-starter-test` 的版本。实际 `<dependencyManagement>` 仅声明了 `springdoc-openapi-starter-webmvc-ui`、`h2` 和内部模块，完全缺少上述外部 starter 条目。虽然 Spring Boot parent POM 的 BOM 默认管理这些版本，但设计意图是将它们显式列出作为"统一管理清单"。
- **建议**：按设计 §2.1 骨架补充上述 starter 的 `<dependency>` 条目到 `<dependencyManagement>`（不带 `<version>`，由 Spring Boot BOM 管理）。

#### [一般] Common POM 引入 spring-boot-starter-validation (optional)

- **位置**：`AIMedical/backend/common/pom.xml:27-31`
- **描述**：设计 §2.2 的 common 模块依赖规约仅指定 `spring-boot-starter-web (optional)` 和 `spring-boot-starter-data-jpa (optional)`，但实际 POM 额外引入了 `spring-boot-starter-validation` 作为 optional。设计文档未授权 common 模块声明 validation 依赖。
- **建议**：将 validation 依赖移出 common 模块，由实际需要 `@Valid` 的业务模块（patient/doctor/admin）自行声明（这些模块已正确声明）。

#### [一般] 父 POM maven-dependency-plugin 额外忽略 business 模块

- **位置**：`AIMedical/backend/pom.xml:109-115`
- **描述**：设计 §2.2 的 `<ignoredUnusedDeclaredDependencies>` 仅包含 `ai-api` 和 `common-module-api` 两个条目。实际 POM 额外添加了 `patient`、`doctor`、`admin` 三个豁免条目。这些 business 模块是 application 模块的真实编译依赖，不应豁免。该配置会继承到所有子模块，可能导致合法的 unused dependency 问题被掩盖。
- **建议**：移除 `com.aimedical:patient`、`com.aimedical:doctor`、`com.aimedical:admin` 三个豁免条目。保留 `ai-api` 和 `common-module-api` 豁免（设计 §2.2 约定的 Phase 0 延迟引用策略）。

#### [轻微] 父 POM modules 顺序与设计不一致

- **位置**：`AIMedical/backend/pom.xml:18-29`
- **描述**：设计 §2.1 的 modules 顺序为 `common → common-module-api → common-module-impl → ai-api → ai-impl → patient → doctor → admin → application → integration`。实际顺序为 `common → common-module-api → common-module-impl → ai-api → ai-impl → application → patient → doctor → admin → integration`（application 在 business 模块之前）。不影响构建依赖解析（Maven reactor 排序由依赖关系决定），但与设计文档清单不一致。
- **建议**：调整 modules 列表顺序与设计一致，或更新设计文档。

#### [轻微] ai-impl POM 额外依赖 common 和 spring-boot-starter

- **位置**：`AIMedical/backend/ai-impl/pom.xml:17-24`
- **描述**：设计仅要求 ai-impl 依赖 ai-api。实际 POM 额外声明了 `common` 和 `spring-boot-starter` 依赖。`common` 已通过 ai-api 传递获得，`spring-boot-starter` 为 Bean 注解所需。此实践合理，但未被设计明确授权。
- **建议**：确认是否需要这些额外依赖；若为配注则可在设计文档中补充说明。

#### [轻微] Application POM 额外声明 common、common-module-api、ai-api、springdoc 依赖

- **位置**：`AIMedical/backend/application/pom.xml:14-29, 73-76`
- **描述**：设计 §2.2 指定 application 依赖 business 模块 + ai-impl + common-module-impl + h2 + security。实际 POM 额外声明了 `common`、`common-module-api`、`ai-api`、`springdoc-openapi-starter-webmvc-ui`。这些额外依赖合理但未在设计文档中体现。
- **建议**：更新设计文档以匹配实际依赖清单，或移除额外依赖（若可通过传递性获得）。

#### [轻微] Integration POM 依赖清单超出设计骨架

- **位置**：`AIMedical/backend/integration/pom.xml:16-63`
- **描述**：设计 §10.1 的 integration 骨架仅包含 `application (test)`、`spring-boot-starter-test (test)`、`maven-failsafe-plugin`。实际 POM 额外声明了 `common`、`web`、`security`、`data-jpa`、`actuator`、`springdoc`（均 test scope）和 `h2 (runtime)`。这些是 `test` scope 非传递性所迫的必要补充，但与设计骨架不一致。此外，Failsafe 配置使用 `<skip>${skipITs}</skip>` 而非设计骨架中的 `<skipTests>false</skipTests>` + `<includes>**/*IT.java</includes>`，且缺少 Failsafe 的 `<include>` 配置（依赖于 Surefire 默认不匹配 `*IT.java` 的命名约定）。
- **建议**：更新设计文档以反映实际 integration 模块配置。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 0 |
| 一般 | 6 |
| 轻微 | 4 |

### 总评

POM 结构与依赖管理的实际实现与 OOD 设计 §2.1/§2.2 存在若干重要偏离：

**核心结构偏离**：实际采用扁平目录布局（所有模块在 `backend/` 根目录）替换了设计的分层 `modules/` 布局，导致聚合 POM 缺失。这是最大的结构性差异，需在代码和设计文档之间二择其一。

**版本与依赖管理**：Spring Boot 版本 3.2.5 vs 3.3.0 不一致；`dependencyManagement` 缺少设计要求的 starter 显式声明。Common 模块多引入了 `spring-boot-starter-validation`。`maven-dependency-plugin` 的豁免范围被不必要地扩大。

**模块依赖方向**：各业务模块（patient/doctor/admin）的依赖结构与设计 §2.2 精确匹配，这是本审查中最合规的部分。Application 和 Integration 模块的依赖清单合理但超出设计骨架描述。

**整改建议**：优先对齐目录结构设计或更新设计文档（二选一），同步修复版本号、dependencyManagement、common POM validation 依赖和 maven-dependency-plugin 豁免配置。
