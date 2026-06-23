#### [一般] 父 POM 缺少 `<name>` 元素

- **位置**：`AIMedical/backend/pom.xml:1-100`
- **描述**：OOD §2.1 父 POM 骨架中包含 `<name>aimedical-sys</name>`，实际父 POM 未声明该元素。虽然不影响构建功能，但与设计文档不一致。
- **建议**：补充 `<name>aimedical-sys</name>` 与 OOD 对齐。

#### [一般] 父 POM `<dependencyManagement>` 缺少设计文档中列出的 Spring Boot Starter 依赖条目

- **位置**：`AIMedical/backend/pom.xml:39-84`
- **描述**：OOD §2.1 父 POM 骨架的 `<dependencyManagement>` 中显式列出了 `spring-boot-starter-web`、`spring-boot-starter-data-jpa`、`spring-boot-starter-security`、`spring-boot-starter-validation`、`spring-boot-starter-test` 五个依赖条目，但实际父 POM 仅包含 `springdoc-openapi-starter-webmvc-ui` 和 `h2`。虽然这些 Starter 的版本已由 `spring-boot-starter-parent` BOM 统一管理，运行时无实际错误，但 OOD 设计意图是显式声明以强化可视化管理。
- **建议**：按 OOD §2.1 示例，在 `<dependencyManagement>` 中补充上述五个 Starter 依赖声明（无需指定 version，由 parent BOM 管理）。

#### [一般] Integration POM 的 Failsafe 插件配置与 OOD §10.2 骨架不一致

- **位置**：`AIMedical/backend/integration/pom.xml:68-83`
- **描述**：OOD §10.2 的 Failsafe 配置骨架包含 `<includes><include>**/*IT.java</include></includes>` 和 `<skipTests>false</skipTests>`，但实际实现使用 `<skip>${skipITs}</skip>` 且未配置 `<includes>`。Failsafe 默认 include 模式包含 `**/*IT.java`，因此功能上无影响，但配置方式与 OOD 不一致。
- **建议**：考虑按 OOD §10.2 统一配置风格，补充显式 `<includes>` 声明。