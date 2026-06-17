# 设计审查报告（v12 r2）

## 审查结果
REJECTED

## 发现

### [严重] integration/pom.xml 缺少 Spring Boot 运行时依赖声明

**设计描述**：`integration/pom.xml` 仅声明了三个依赖：`application`(test)、`spring-boot-starter-test`(test)、`common`(test)。

**问题**：`application` 依赖使用 `test` scope，而 Maven `test` scope 是非传递性的（non-transitive）。这意味着 `application` 的所有传递性编译依赖（`spring-boot-starter-web`、`spring-boot-starter-security`、`spring-boot-starter-data-jpa`、`spring-boot-starter-actuator`、`com.h2database:h2`、`springdoc-openapi-starter-webmvc-ui`）都不会出现在 `integration` 模块的测试 classpath 上。

这会导致以下具体失败：

1. **SecurityConfigPhase0 加载失败**：`SecurityConfigPhase0` 编译后的 class 文件引用了 `org.springframework.security.config.annotation.web.builders.HttpSecurity`、`org.springframework.security.web.SecurityFilterChain` 等类。由于 `spring-boot-starter-security` 不在 classpath 上，JVM 在尝试加载该 class（`phase0`  profile 通过 `application.yml` 默认激活，因此 Spring 会尝试注册该 bean）时将抛出 `NoClassDefFoundError`。

2. **内嵌 Web 容器无法启动**：`@SpringBootTest(webEnvironment = RANDOM_PORT)` 需要一个 Servlet 容器（Tomcat）。没有 `spring-boot-starter-web` 的传递依赖，Spring Boot 无法自动配置 `TomcatServletWebServerFactory`，上下文启动将失败。

3. **数据源 / JPA 自动配置异常**：`spring-boot-starter-data-jpa` 和 `h2` 不在 classpath 上，`@EntityScan("com.aimedical")` 和相关 JPA 自动配置可能无法正常工作。

4. **TestRestTemplate 无法自动配置**：`TestRestTemplate` 的自动配置需要 Spring MVC 在 classpath 上，而 Spring MVC 由 `spring-boot-starter-web` 引入。

**期望的修正方向**：在 `integration/pom.xml` 中显式声明 `application` 模块运行时所需的全部 Spring Boot starter，scope 设为 `test`：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
    <scope>test</scope>
</dependency>
```

<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <scope>test</scope>
</dependency>
```

（后两个可选：actuator 和 springdoc 不影响测试核心逻辑，但推荐包含以匹配 `application` 完整 classpath。）

### [轻微] 父 POM 的 dependencyManagement 条目不必要

`dependencyManagement` 中添加 `com.aimedical:integration` 的 `<dependency>` 条目是不必要的，因为没有其他模块会依赖 `integration`。同理，`ignoredUnusedDeclaredDependency` 中追加 `com.aimedical:integration` 也是冗余的。不阻塞构建，但建议移除以保持配置整洁。

## 修改要求（仅 REJECTED 时）

针对上述 [严重] 问题：

- **问题**：`integration/pom.xml` 缺少 `application` 模块运行时依赖的传递声明，导致 `@SpringBootTest` 无法启动完整的应用上下文
- **为什么是问题**：Maven `test` scope 非传递性，`application` 的编译期依赖（Spring Boot starters、H2 等）不会透传给 `integration`；`SecurityConfigPhase0` 引用了 Spring Security 类型，classpath 上不存在时将引发 `NoClassDefFoundError`
- **期望修正**：在 `integration/pom.xml` 中显式添加 `spring-boot-starter-web`、`spring-boot-starter-security`、`spring-boot-starter-data-jpa`、`h2` 等缺失依赖，scope 标记为 `test`（h2 标记为 `runtime`）
