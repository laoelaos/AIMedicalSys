# 详细设计（v12）

## 概述

创建 `backend/integration/` 集成测试模块，补齐 Phase 0 后端骨架的最后缺失模块。设计目标：使 `integration` 模块通过 maven-failsafe-plugin 在 `integration-test` 和 `verify` 生命周期阶段自动执行集成测试，验证后端应用上下文可加载、健康检查端点可达。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `backend/pom.xml` | 修改 | modules 追加 `<module>integration</module>`（无需其他变更，integration 为纯测试模块） |
| `backend/integration/pom.xml` | 新建 | 集成测试模块 POM，声明依赖与 failsafe 插件 |
| `backend/integration/src/test/java/com/aimedical/integration/HealthCheckIT.java` | 新建 | 健康检查集成测试：验证 `GET /api/ping` 返回 HTTP 200 且 `Result.code` 为 `"SUCCESS"`、`Result.data` 为 `"pong"` |
| `backend/integration/src/test/java/com/aimedical/integration/ApplicationContextIT.java` | 新建 | 应用上下文加载集成测试：验证 Spring 容器可成功启动 |

## 父 POM 变更（backend/pom.xml）

### modules 列表追加

在 `<modules>` 块末尾追加 `integration`，与现有模块平级：
```xml
<module>integration</module>
```

### dependencyManagement 与豁免条目

`integration` 模块是纯测试模块，无其他模块依赖它，因此无需在父 POM 的 `dependencyManagement` 中添加声明，也无需在 `maven-dependency-plugin` 的 `ignoredUnusedDeclaredDependencies` 中添加豁免条目。

## 类型定义

### integration/pom.xml — 集成测试模块 POM

**形态**：Maven POM（`pom.xml`）

**职责**：声明模块元信息、依赖关系和 failsafe 插件配置，使集成测试可独立或随 reactor 执行。

**parent**：`com.aimedical:aimedical-sys:0.0.1-SNAPSHOT`（relativePath = `../pom.xml`）

**artifactId**：`integration`

**packaging**：`jar`

**依赖清单**：

| 依赖 | scope | 用途 |
|------|-------|------|
| `com.aimedical:application` | test | 提供 @SpringBootTest 所需的 Spring Bean 定义和配置 |
| `org.springframework.boot:spring-boot-starter-test` | test | 提供 `@SpringBootTest`、`TestRestTemplate`、`@Autowired` 等集成测试基础设施 |
| `com.aimedical:common` | test | 引用 `Result` 类型验证响应结构 |
| `org.springframework.boot:spring-boot-starter-web` | test | 提供内嵌 Tomcat 容器和 Spring MVC，使 `@SpringBootTest(webEnvironment=RANDOM_PORT)` 可启动完整 Web 上下文 |
| `org.springframework.boot:spring-boot-starter-security` | test | 使 `SecurityConfigPhase0` 类可被加载并注册 |
| `org.springframework.boot:spring-boot-starter-data-jpa` | test | 使 JPA 实体和数据源自动配置可完成 |
| `com.h2database:h2` | runtime | 集成测试运行时所需的内存数据库驱动 |
| `org.springframework.boot:spring-boot-starter-actuator` | test | （可选）使 Actuator 自动配置可加载，保持 classpath 与 application 一致 |
| `org.springdoc:springdoc-openapi-starter-webmvc-ui` | test | （可选）使 OpenAPI 自动配置可加载，保持 classpath 与 application 一致 |

> Maven `test` scope 非传递性：`application` 模块编译期依赖（`spring-boot-starter-web`、`spring-boot-starter-security`、`spring-boot-starter-data-jpa`、`h2` 等）不会透传给 `integration`。因此这些依赖必须在 `integration/pom.xml` 中显式声明，scope 标记为 `test`（`h2` 标记为 `runtime`），以确保 `@SpringBootTest` 能启动完整应用上下文。

**maven-failsafe-plugin 配置要点**：
- 绑定生命周期阶段：`integration-test` 和 `verify`
- 跳过属性：`skipITs`
- `@SpringBootTest` 在测试 JVM 进程内启动内嵌容器（Tomcat），无需 failsafe 在 `pre-integration-test` 阶段启动外部进程

**完整 POM**：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.aimedical</groupId>
        <artifactId>aimedical-sys</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
    <artifactId>integration</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>com.aimedical</groupId>
            <artifactId>application</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.aimedical</groupId>
            <artifactId>common</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- 显式声明 application 模块的传递性编译依赖（test scope 非传递性） -->
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
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-failsafe-plugin</artifactId>
                <configuration>
                    <skip>${skipITs}</skip>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>integration-test</goal>
                            <goal>verify</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

### ApplicationContextIT — 应用上下文加载集成测试（class）

**形态**：class，标注 `@SpringBootTest`，包 `com.aimedical.integration`

**职责**：验证 Spring 应用上下文可成功加载，不抛出 `ApplicationContextException` 或 `BeanDefinitionStoreException`。

**行为**：
- 空测试方法 `void contextLoads()`，标记 `@Test`
- 无断言——测试通过的唯一条件是上下文加载阶段不抛出异常
- 不注入任何 Bean，不发起 HTTP 请求

### HealthCheckIT — 健康检查集成测试（class）

**形态**：class，标注 `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)`，包 `com.aimedical.integration`

**职责**：验证 `GET /api/ping` 端点可达且返回 HTTP 200，响应体可反序列化为 `Result<String>`，`code` 为 `"SUCCESS"`、`data` 为 `"pong"`。

**行为**：
- `@Autowired private TestRestTemplate restTemplate`
- `@Test void shouldPingSuccess()`：`restTemplate.getForEntity("/api/ping", Result.class)` → 验证状态码为 `HttpStatus.OK`（200）、`result.getCode()` 等于 `"SUCCESS"`、`result.getData()` 等于 `"pong"`
- 使用 `RANDOM_PORT` 避免端口冲突，每次测试分配随机端口

## 集成测试命名约定

使用 `*IT.java` 后缀（而非 `*Test.java`）确保 surefire（单元测试）和 failsafe（集成测试）互不干扰：
- surefire 默认包含模式 `**/Test*.java`、`**/*Test.java`、`**/*Tests.java`、`**/*TestCase.java`，不匹配 `*IT.java`
- failsafe 默认包含模式包含 `**/*IT.java`，无需显式配置即可匹配集成测试类

## 行为契约

### 构建执行流程

```
mvn verify -pl integration -am
  → Maven reactor 解析 integration 依赖链（application → common/common-module-*/ai-*/patient/doctor/admin）
  → 编译所有依赖模块
  → Failsafe 插件绑定到 integration-test 阶段
    → @SpringBootTest 在测试 JVM 进程内启动 Spring 容器 + 内嵌 Tomcat（随机端口）
    → ApplicationContextIT.contextLoads(): 验证上下文加载（空测试，仅确保不抛出异常）
    → HealthCheckIT.shouldPingSuccess(): 通过 TestRestTemplate 验证 /api/ping 可达
  → Failsafe 插件绑定到 verify 阶段（检查测试结果）
  → BUILD SUCCESS / BUILD FAILURE
```

### 健康检查验证契约

```
GET /api/ping（通过 TestRestTemplate，随机端口）
→ HTTP 200 OK
→ 响应体反序列化为 Result<String>，result.code == "SUCCESS"，result.data == "pong"
```

### 应用上下文加载契约

```
@SpringBootTest 启动
→ 所有 @Component、@Service、@Repository、@Controller 被扫描注册
→ 所有 JPA 实体被 @EntityScan 发现
→ H2 数据源初始化
→ SecurityConfigPhase0 激活（permitAll）
→ 上下文加载完成，不抛出异常
```

## 错误处理

integration 模块不定义独立的异常处理逻辑：

| 场景 | 行为 | 责任方 |
|------|------|--------|
| `@SpringBootTest` 上下文加载失败 | Failsafe 报告 BUILD FAILURE，测试日志输出 ApplicationContextException 堆栈 | Spring Test 框架 + Failsafe |
| `TestRestTemplate` 请求超时或连接失败 | JUnit 断言失败，Failsafe 报告 BUILD FAILURE | JUnit + Failsafe |
| application 可执行 JAR 不在 classpath | Maven 构建失败，提示缺少依赖 | Maven 依赖解析 |
| `GET /api/ping` 返回非 200 | `assertEquals(statusCode, HttpStatus.OK)` 断言失败 | JUnit |
| `GET /api/ping` 返回的 Result.code 或 Result.data 不匹配 | JUnit assertEquals 断言失败 | JUnit |

## 依赖关系

### 依赖的已有类型

| 类型 | 模块 | 用途 |
|------|------|------|
| `HealthController`（`GET /api/ping`） | application | HealthCheckIT 验证的目标端点 |
| `Application.java`（`@SpringBootApplication`） | application | ApplicationContextIT 验证的启动入口 |
| `SecurityConfigPhase0`（permitAll） | application | 确保集成测试无需认证即可访问 `/api/ping` |
| `Result<T>` | common | 用于验证健康检查响应格式 |

### 暴露给后续任务的公开接口

integration 模块不暴露任何 API 或 SPI 接口。其唯一产出是 Failsafe 测试报告，供 CI 门禁或验收流程判断后端是否满足健康检查标准。

## 设计决策

| 决策 | 选项 | 选择 | 理由 |
|------|------|------|------|
| application 依赖方式 | `scope=test, classifier=exec` vs `scope=test` | `scope=test` | `exec` classifier 对应 BOOT-INF 布局的 fat JAR，`@SpringBootTest` 无法扫描其内部 classpath。Maven reactor 构建时直接使用 `target/classes` 中的编译类，无需通过 JAR artifact 解析。仅声明 `scope=test` 即可将 application 模块的类加入测试 classpath |
| 测试类命名后缀 | `*Test.java` vs `*IT.java` | `*IT.java` | Failsafe 默认包含模式 `**/IT*.java`、`**/*IT.java`、`**/*ITCase.java`；`*IT.java` 与 surefire 的 `*Test.java` 互不干扰，确保单元测试和集成测试可同时在 reactor 中执行 |
| failsafe 绑定阶段 | `integration-test` + `verify` vs `test` | `integration-test` + `verify` | Maven 生命周期约定：`test` 绑定 surefire（单元测试），`integration-test` 绑定 failsafe（集成测试）；`verify` 阶段检查集成测试结果并决定 BUILD SUCCESS/FAILURE |
| 测试数量 | 2 个 vs 1 个合并 | 2 个 | ApplicationContextIT 验证上下文可加载（快速失败检测），HealthCheckIT 验证端点可达（功能验证）。两个职责正交，拆分便于未来扩展集成测试集时保持结构清晰 |

## 修订说明（v12 r1）

| 审查意见 | 修改措施 |
|---------|---------|
| `classifier=exec` 导致 `@SpringBootTest` 无法扫描 BOOT-INF 内组件 | 移除 `classifier=exec`，仅声明 `scope=test`；Maven reactor 构建时直接使用 `target/classes` 编译类，无需 JAR artifact |
| 构建流程描述虚构 failsafe 启动应用的动作 | 修正构建流程图：删除 pre-integration-test/post-integration-test 阶段，改为 `@SpringBootTest` 在测试 JVM 进程内启动内嵌容器 |
| `common` 依赖与测试使用的响应类型不一致（String 包含检查） | HealthCheckIT 改用 `Result.class` 反序列化响应，断言 `code=="SUCCESS"` 和 `data=="pong"`，保留 `common` 依赖 |
| Failsafe 包含模式 `**/*IT.java` 重复默认值 | 从配置要点中移除显式 include 模式，在命名约定中说明依赖 failsafe 默认包含规则 |
| 缺少完整的 integration/pom.xml 示例 | 在 failsafe 配置后补充完整 POM XML，含依赖和插件声明 |

## 修订说明（v12 r2）

| 审查意见 | 修改措施 |
|---------|---------|
| `integration/pom.xml` 仅声明 application(test)、spring-boot-starter-test(test)、common(test) 三个依赖，但 Maven test scope 非传递性，application 模块的编译期依赖（spring-boot-starter-web、spring-boot-starter-security、spring-boot-starter-data-jpa、h2 等）不会透传给 integration，导致 `@SpringBootTest` 无法启动完整应用上下文 | 在 `integration/pom.xml` 中显式添加 `spring-boot-starter-web`(test)、`spring-boot-starter-security`(test)、`spring-boot-starter-data-jpa`(test)、`spring-boot-starter-actuator`(test)、`springdoc-openapi-starter-webmvc-ui`(test)、`h2`(runtime)；同时在依赖清单后添加说明段落解释 test scope 非传递性及显式声明的必要性 |
| 父 POM 的 dependencyManagement 中追加 integration 条目不必要，ignoredUnusedDeclaredDependency 追加 integration 也冗余 | 从「父 POM 变更」章节中删除 `dependencyManagement` 和 `ignoredUnusedDeclaredDependency` 的追加操作，改为说明 integration 为纯测试模块无需这些声明 |
