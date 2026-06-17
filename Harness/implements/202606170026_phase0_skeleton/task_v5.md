# 任务指令（v5）

## 动作
RETRY

## 任务描述
修复 common-module-api 的 `pom.xml`，添加缺失的 `spring-boot-starter-test` 测试依赖

涉及文件：
- `backend/common-module-api/pom.xml` — 添加 `<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>`

## 选择理由
R4 代码实现已验证通过（common-module-api 编译成功，common-module-impl 编译成功），仅 common-module-api 的测试编译因缺失 `spring-boot-starter-test` 依赖而失败（23 个编译错误），修复后即可通过本轮验证，继续推进后续任务。

## 任务上下文
- common-module-api 是纯契约模块，不含 JPA/Web 依赖，仅依赖 `common`（获取 BaseEnum）
- 测试文件 `UserTypeTest.java` 已存在，包含 9 个测试用例（验证 3 个枚举常量、code/desc 取值、BaseEnum 实现）
- 父 POM（spring-boot-starter-parent:3.2.5）已管理 `spring-boot-starter-test` 版本，子模块只需声明 groupId/artifactId/scope

## 已有代码上下文
common-module-api 当前 POM（`backend/common-module-api/pom.xml`）：
```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.aimedical</groupId>
        <artifactId>aimedical-sys</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
    <artifactId>common-module-api</artifactId>
    <packaging>jar</packaging>
    <dependencies>
        <dependency>
            <groupId>com.aimedical</groupId>
            <artifactId>common</artifactId>
        </dependency>
    </dependencies>
</project>
```

common-module-impl/pom.xml 已有正确的 spring-boot-starter-test 声明可作为参照。

## RETRY 说明
### 失败原因
verify_v4 执行 `mvn test` 时，common-module-api 的测试编译失败 — `UserTypeTest.java` 使用 JUnit 5 的 `@Test`、`assertEquals`、`assertNotNull`、`assertInstanceOf` 等 API，但 common-module-api 的 POM 未声明 `spring-boot-starter-test` 依赖，导致类路径上缺少 `org.junit.jupiter.api` 包。

### 修正方向
在 common-module-api/pom.xml 的 `<dependencies>` 中添加：
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```
版本由父 POM（spring-boot-starter-parent:3.2.5）管理，无需显式指定 `<version>`。
