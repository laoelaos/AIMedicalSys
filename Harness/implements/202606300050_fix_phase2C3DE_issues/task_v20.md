# 任务指令（v20）

## 动作
RETRY

## 任务描述
修复 R19 编译失败的唯一根因：在 `ai-impl/pom.xml` 追加 `spring-boot-starter-web` 依赖。

**涉及文件**：`modules/ai/ai-impl/pom.xml`（1 个文件）

**具体变更**：在 `ai-impl/pom.xml` 的 `<dependencies>` 内追加：
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```
位置建议放在 `spring-boot-starter` 之后、`spring-boot-starter-test` 之前。

## 选择理由
R19 验证 FAILED 的唯一原因是 `MockAdminController.java`（在 `ai-impl/src/main/java/.../mock/` 包）使用 `@RestController`、`@RequestMapping`、`@GetMapping`、`@PostMapping`、`@RequestBody`、`ResponseEntity` 等 Spring MVC 注解，但 `ai-impl/pom.xml` 仅声明了 `spring-boot-starter`（无 web），导致 15 个编译错误。

R19 其余所有代码变更（TriageServiceImpl 超时注入、MockAiService @Profile 切换、FallbackAiService selectDelegate、DegradationContext 字段扩展、AiResultFactory 替换、application.yml 配置、测试文件构造参数适配等）均已验证正确——common/ai-api 模块全部测试通过。

此轮仅需修复 pom.xml 依赖，无需修改 `MockAdminController.java` 或任何其他代码。

## 任务上下文
**问题来源**：R19 实施了 AI 超时外化+MockAiService+降级框架+配置+AiResultFactory 共 6 项变更。其中 A05 要求在 `ai-impl` 模块创建 `MockAdminController.java` 作为运行时切换 mock 策略的 REST 端点。该文件使用 Spring MVC 注解，但 `ai-impl/pom.xml` 仅有 `spring-boot-starter`（基础自动配置，不含 web）。

**验证报告结论**：`mvn clean test` 在 `ai-impl` 模块 compile 阶段失败，15 个错误全部指向缺少 Spring Web 注解的 import。所有其他模块编译和测试通过。

**修正方案**：在 `ai-impl/pom.xml` 的 `<dependencies>` 追加 `spring-boot-starter-web`。该项目所有包含 REST Controller 的模块（admin、consultation、patient、doctor、prescription、medical-record、common-module-impl、application、integration）都已声明此依赖，`ai-impl` 的添加方式完全一致。

## 已有代码上下文

### ai-impl/pom.xml 当前内容
```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.aimedical</groupId>
        <artifactId>ai</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
    <artifactId>ai-impl</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>com.aimedical</groupId>
            <artifactId>ai-api</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <properties>
        <jacoco.skip>false</jacoco.skip>
        <jacoco.skip.check>false</jacoco.skip.check>
    </properties>
</project>
```

### 参考——其他模块的声明方式（以 admin 为例）
所有包含 REST Controller 的模块都是使用无 scope 的 `spring-boot-starter-web`（版本由 Spring Boot 父 POM 管理）。

## RETRY 说明
**失败轮次**：R19（v19）

**失败原因**：`MockAdminController.java` 使用 Spring MVC 注解（`@RestController`、`@RequestMapping`、`@GetMapping`、`@PostMapping`、`@RequestBody`、`ResponseEntity`），但 `ai-impl/pom.xml` 缺少 `spring-boot-starter-web` 依赖，导致 `javac` 无法解析 `org.springframework.web.bind.annotation.*` 和 `org.springframework.http.ResponseEntity`——共 15 个编译错误。

**修正方向**：`ai-impl/pom.xml` 追加 `spring-boot-starter-web` 依赖（scope 默认 compile，与项目中其他含 Controller 的模块一致）。

**验证预期**：
- `mvn compile -pl modules/ai/ai-impl` 编译通过（无 COMPILATION ERROR）
- `mvn test -pl modules/ai/ai-impl` 测试通过
- `mvn test` 全量构建通过，ai-impl 不阻断后续模块
