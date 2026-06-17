# R5: application 模块 + integration 模块 + POM 配置 + OOD Sections 9/10/11 一致性审查

审查时间：2026-06-17 19:40

### 审查范围

- `AIMedical/backend/application/src/main/java/com/aimedical/Application.java`
- `AIMedical/backend/application/src/main/java/com/aimedical/HealthController.java`
- `AIMedical/backend/application/src/main/java/com/aimedical/config/SecurityConfigPhase0.java`
- `AIMedical/backend/application/src/main/resources/application.yml`
- `AIMedical/backend/application/src/main/resources/application-dev.yml`
- `AIMedical/backend/application/src/main/resources/application-prod.yml`
- `AIMedical/backend/application/pom.xml`
- `AIMedical/backend/integration/src/test/java/com/aimedical/integration/ApplicationContextIT.java`
- `AIMedical/backend/integration/src/test/java/com/aimedical/integration/HealthCheckIT.java`
- `AIMedical/backend/integration/pom.xml`
- `AIMedical/backend/pom.xml` (父 POM)
- `AIMedical/backend/modules/common-module/pom.xml`
- `AIMedical/backend/modules/ai/pom.xml`
- `AIMedical/backend/modules/patient/pom.xml`
- `AIMedical/backend/modules/doctor/pom.xml`
- `AIMedical/backend/modules/admin/pom.xml`
- `AIMedical/backend/common/pom.xml`
- `AIMedical/backend/modules/ai/ai-api/pom.xml`
- `AIMedical/backend/modules/common-module/common-module-api/pom.xml`
- 审查依据：`Docs/04_ood_phase0.md` §2.1, §2.2, §4.1, §4.5, §8.3, §9, §10, §10.1

### 发现

#### [一般] 父 POM dependencyManagement 中 Starter 依赖冗余显式版本号

- **位置**：`AIMedical/backend/pom.xml:84-109`
- **描述**：§2.1 设计原文中 `spring-boot-starter-web/data-jpa/security/validation/test` 在 `dependencyManagement` 内不设 version（由 `spring-boot-starter-parent` BOM 统一管理），但实现中每个条目都显式标注了 `<version>3.2.5</version>`，与 BOM 管理原则重复。
- **建议**：移除 dependencyManagement 中所有 spring-boot-starter-* 的 `<version>3.2.5</version>`，让 BOM 统一管控版本，消除版本漂移风险。

#### [一般] 父 POM dependencyManagement 中 h2 依赖误设 scope

- **位置**：`AIMedical/backend/pom.xml:82`
- **描述**：§2.1 设计约定 `dependencyManagement` 仅管理版本，scope 由各消费模块的 `dependency` 自行声明；h2 的 scope 明确标注在 `application/pom.xml:72`。但父 POM 的 `dependencyManagement` 中 h2 条目已设 `<scope>runtime</scope>`，与 consumer 决定 scope 的设计约定不符。
- **建议**：移除父 POM 中 h2 条目的 `<scope>runtime</scope>`，保持 `dependencyManagement` 仅管理版本，scope 由 `application/pom.xml` 等消费模块各自声明。

#### [一般] common 模块缺少 MeterRegistryCustomizer 占位配置

- **位置**：`AIMedical/backend/common/src/main/java/com/aimedical/common/config/`
- **描述**：§10.1 要求在 common 模块的 `com.aimedical.common.config` 包中声明 `MeterRegistryCustomizer` 配置类占位（设置通用标签如 `application=aimedical-sys`）。该包下仅有 `JpaConfig.java`、`JacksonConfig.java`、`GlobalExceptionHandler.java`，未发现任何 Micrometer/MeterRegistry 相关 Bean 声明。
- **建议**：按 §10.1 补充 `MeterRegistryCustomizer` 占位配置类，注册通用标签 `application=aimedical-sys`，确保 Micrometer 核心依赖可正常注入。

#### [轻微] Failsafe 插件配置缺少显式 `<include>` 声明

- **位置**：`AIMedical/backend/integration/pom.xml:70-82`
- **描述**：§10.1 的 Failsafe 配置骨架显式包含 `<include>**/*IT.java</include>`，但实际配置仅有 `<skip>${skipITs}</skip>` 和 execution 绑定，未显式声明 include。虽 Failsafe 默认 includes 已覆盖 `**/*IT.java`，但不与设计文档对齐。
- **建议**：补充 `<includes><include>**/*IT.java</include></includes>` 与 OOD §10.1 骨架一致。

#### [轻微] 父 POM 缺少 `<name>` 元素

- **位置**：`AIMedical/backend/pom.xml:17`
- **描述**：§2.1 父 POM 骨架示例包含 `<name>aimedical-sys</name>`，实际父 POM 未声明该元素。
- **建议**：补充 `<name>aimedical-sys</name>` 与设计文档对齐。

### 通过项

| 检查项 | 结果 |
|--------|------|
| Application.java @SpringBootApplication(scanBasePackages="com.aimedical") + @EntityScan + @EnableJpaRepositories (§9.2) | 正确 |
| HealthController.java GET /api/ping 返回 Result.success("pong") (§4.1) | 正确 |
| SecurityConfigPhase0 @Profile("phase0") permitAll 无额外认证组件 (§4.5) | 正确 |
| application.yml name=aimedical-sys, profiles=phase0,dev, port=8080 (§9.1) | 正确 |
| application-prod.yml H2 console disabled, Actuator health+info, springdoc disabled (§9.1/§10.1) | 正确 |
| application-dev.yml datasource(h2:mem:aimedical), ddl-auto=update, show-sql, ai.mock.enabled=true, springdoc配置, actuator配置 (§8.3/§9.1/§10.1) | 正确 |
| application/pom.xml spring-boot-maven-plugin classifier=exec (§10) | 正确 |
| application/pom.xml H2 dependency runtime scope (§9.1) | 正确 |
| ApplicationContextIT.java Spring 上下文加载验证 (§10.1) | 正确 |
| HealthCheckIT.java /api/ping 端点验证 (§10.1) | 正确 |
| 父 POM 模块聚合（common, common-module-api/impl, ai-api/impl, patient, doctor, admin, application, integration）(§2.1) | 正确 |
| 父 POM maven-dependency-plugin ignoredUnusedDeclaredDependencies (ai-api, common-module-api) (§2.2) | 正确 |
| 内部模块 version=${project.version} 统一管理 (§2.1) | 正确 |
| common-module/pom.xml 聚合 common-module-api + impl, parent→aimedical-sys, relativePath=../../pom.xml (§2.1) | 正确 |
| ai/pom.xml 聚合 ai-api + impl, parent→aimedical-sys, relativePath=../../pom.xml (§2.1) | 正确 |
| patient/doctor/admin pom.xml 包含 common, common-module-api, ai-api, spring-boot-starter-web, data-jpa, validation (§2.2) | 正确 |
| common/pom.xml spring-boot-starter-web/data-jpa optional=true (§2.2) | 正确 |
| ai-api/pom.xml 仅依赖 common（compile）(§2.2) | 正确 |
| common-module-api/pom.xml 仅依赖 common（compile）(§2.2) | 正确 |
| integration 依赖 application(test) + spring-boot-starter-test(test) 及传递性补充 (§10.1) | 正确 |
| application/pom.xml 显式引入 patient/doctor/admin/ai-impl/common-module-impl (§2.2) | 正确 |

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 0 |
| 一般 | 3 |
| 轻微 | 2 |

### 总评

application 模块骨架代码、配置和集成测试实现准确对齐 OOD 设计（§4.1/§4.5/§9/§10.1），业务模块 POM 依赖方向符合 §2.2 约定。发现 3 个一般性问题：父 POM dependencyManagement 中 starter 版本号冗余（与 BOM 管理原则重复）、h2 作用域误设在 dependencyManagement 层级、common 模块缺少 MeterRegistryCustomizer 占位配置（§10.1 推荐补齐项）。2 个轻微问题：Failsafe 缺少显式 include 声明、父 POM 缺少 name 元素。核心骨架代码和测试覆盖质量良好。
