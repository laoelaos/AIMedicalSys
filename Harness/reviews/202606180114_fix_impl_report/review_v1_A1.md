# R1: 后端核心基础设施层实现与 OOD 一致性审查

审查时间：2026-06-18

### 审查范围

- `AIMedical/backend/pom.xml`
- `AIMedical/backend/common/pom.xml`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/base/BaseEntity.java`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/base/BaseEnum.java`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/config/GlobalExceptionHandler.java`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/config/JacksonConfig.java`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/config/JpaConfig.java`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/exception/BusinessException.java`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/exception/ErrorCode.java`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/exception/GlobalErrorCode.java`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/result/Result.java`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageQuery.java`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageResponse.java`
- `AIMedical/backend/application/pom.xml`
- `AIMedical/backend/application/src/main/java/com/aimedical/Application.java`
- `AIMedical/backend/application/src/main/java/com/aimedical/HealthController.java`
- `AIMedical/backend/application/src/main/java/com/aimedical/config/SecurityConfigPhase0.java`
- `AIMedical/backend/application/src/main/resources/application.yml`
- `AIMedical/backend/application/src/main/resources/application-prod.yml`
- `AIMedical/backend/integration/pom.xml`
- `AIMedical/backend/integration/src/test/java/com/aimedical/integration/ApplicationContextIT.java`
- `AIMedical/backend/integration/src/test/java/com/aimedical/integration/HealthCheckIT.java`

### 发现

#### [一般] 父 POM 缺少 `<name>` 元素

- **位置**：`AIMedical/backend/pom.xml:1-100`
- **描述**：OOD §2.1 父 POM 骨架中包含 `<name>aimedical-sys</name>`，实际父 POM 未声明该元素。虽然不影响构建功能，但与设计文档不一致。
- **建议**：补充 `<name>aimedical-sys</name>` 与 OOD 对齐。

#### [一般] 父 POM `<dependencyManagement>` 缺少设计文档中列出的 Spring Boot Starter 依赖条目

- **位置**：`AIMedical/backend/pom.xml:39-84`
- **描述**：OOD §2.1 父 POM 骨架的 `<dependencyManagement>` 中显式列出了 `spring-boot-starter-web`、`spring-boot-starter-data-jpa`、`spring-boot-starter-security`、`spring-boot-starter-validation`、`spring-boot-starter-test` 五个依赖条目，但实际父 POM 仅包含 `springdoc-openapi-starter-webmvc-ui` 和 `h2`。虽然这些 Starter 的版本已由 `spring-boot-starter-parent` BOM 统一管理，运行时无实际错误，但 OOD 设计意图是显式声明以强化可视化管理。
- **建议**：按 OOD §2.1 示例，在 `<dependencyManagement>` 中补充上述五个 Starter 依赖声明（无需指定 version，由 parent BOM 管理）。

#### [轻微] 父 POM 缺少 `<name>` 及部分 `<dependencyManagement>` 条目的 Spring Boot 依赖

- 同上一项综合归入一般级别。

#### [一般] Integration POM 的 Failsafe 插件配置与 OOD §10.2 骨架不一致

- **位置**：`AIMedical/backend/integration/pom.xml:68-83`
- **描述**：OOD §10.2 的 Failsafe 配置骨架包含 `<includes><include>**/*IT.java</include></includes>` 和 `<skipTests>false</skipTests>`，但实际实现使用 `<skip>${skipITs}</skip>` 且未配置 `<includes>`。Failsafe 默认 include 模式包含 `**/*IT.java`，因此功能上无影响，但配置方式与 OOD 不一致。
- **建议**：考虑按 OOD §10.2 统一配置风格，补充显式 `<includes>` 声明。

#### [轻微] Integration POM 依赖了 `springdoc` 等 application 的传递性依赖

- **位置**：`AIMedical/backend/integration/pom.xml:54-58`
- **描述**：Integration 模块显式声明了 `springdoc-openapi-starter-webmvc-ui`（test scope），OOD 中仅预期 `application`（test）、`spring-boot-starter-test`（test）及必要的传递依赖补充（web、security、jpa、actuator）。虽然不影响功能，但这些依赖通过 application 的 transitive 依赖已经可达（前提是 application 的普通 JAR 正确生成），额外显式声明增加了维护负担。
- **建议**：验证 `application` 模块的普通 JAR（非 exec classifier）是否完整传递了 transitive 依赖；若已验证通过，可移除重复声明的 `springdoc-openapi-starter-webmvc-ui`。

#### [轻微] CI 文件 `.github/workflows/phase0-ci.yml` 不存在

- **位置**：`.github/workflows/phase0-ci.yml`
- **描述**：OOD §10.1 强烈推荐创建该 CI 文件并定义了六阶段流水线骨架，同时 §11.4 将其列为 Phase 0 交付完成判定项之一。当前仓库无此文件。
- **建议**：按 OOD §10.1 骨架创建 `.github/workflows/phase0-ci.yml`。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 0 |
| 一般 | 3 |
| 轻微 | 2 |

### 总评

后端核心基础设施层的实现与 OOD 设计文档整体一致，核心抽象（`BaseEntity`、`BaseEnum`、`ErrorCode`、`GlobalErrorCode`、`BusinessException`、`Result`、`PageQuery`、`PageResponse`）均完全符合 OOD §3 的定义。Common 模块的 optional 依赖声明、JpaConfig 的 `@EnableJpaAuditing`、SecurityConfigPhase0 的 `@Profile("phase0")`、Application.java 的扫描注解、HealthController 的 `/api/ping` 端点、spring-boot-maven-plugin 的 classifier=exec、application.yml 配置等关键项均与 OOD 一致。集成测试文件存在且结构正确。

发现的问题均为非功能性偏离：父 POM 缺少 OOD 定义的 `<name>` 元素和部分 dependencyManagement 条目，Integration POM 的 Failsafe 配置风格与 OOD 骨架略有差异，以及 CI 文件尚未创建。所有发现不影响骨架的核心正确性和可运行性，建议在后续迭代中逐步对齐。
