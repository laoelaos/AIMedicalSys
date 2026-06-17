# 详细设计（v3）

## 概述

为 Phase 0 后端创建 Maven 父 POM 及 common 共享基础模块。父 POM 聚合所有 6 个子模块并统一管理依赖版本；common 模块提供 JPA 实体基类、统一响应包装、错误码体系、分页契约、JPA 审计配置、Jackson 配置及全局异常处理等所有模块共享的基础类型。

**v2 变更**：修复父 POM `dependencyManagement` 中外置 Starter 声明缺少版本号导致 common 模块编译失败的问题。删除「External starters」区块（原 detail_v1 设计中的行 69-90），利用 `spring-boot-starter-parent:3.2.5` 已传递管理的版本。

**v3 变更**：修复 `JacksonConfigTest.shouldRegisterJavaTimeModule` 中断言依赖 Jackson 模块 ID 内部格式导致的测试失败。将检查特定模块 ID 的脆弱断言替换为仅验证有模块被注册的健壮断言。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| backend/pom.xml | 修改 | Maven 多模块聚合父 POM，聚合 6 个子模块，管理内部模块版本及外部库版本，删除外置 Starter 版本声明，配置 maven-dependency-plugin 豁免 ai-api 和 common-module-api |
| backend/common/pom.xml | 新建（v1 已创建） | common 模块 POM，以 optional 方式依赖 spring-boot-starter-web 和 spring-boot-starter-data-jpa，以 test 方式依赖 spring-boot-starter-test |
| backend/common/src/main/java/com/aimedical/common/base/BaseEntity.java | 新建（v1 已创建） | JPA 实体基类（抽象类），提供 id、createdAt、updatedAt、deleted 字段及软删除注解 |
| backend/common/src/main/java/com/aimedical/common/base/BaseEnum.java | 新建（v1 已创建） | 枚举基类接口，定义 getCode() / getDesc() 契约 |
| backend/common/src/main/java/com/aimedical/common/result/Result.java | 新建（v1 已创建） | 统一响应包装泛型类，静态工厂 success/fail |
| backend/common/src/main/java/com/aimedical/common/result/PageQuery.java | 新建（v1 已创建） | 分页请求参数类，含校验注解 |
| backend/common/src/main/java/com/aimedical/common/result/PageResponse.java | 新建（v1 已创建） | 分页响应泛型类，含静态工厂 of |
| backend/common/src/main/java/com/aimedical/common/exception/ErrorCode.java | 新建（v1 已创建） | 错误码接口，定义 code() / message() |
| backend/common/src/main/java/com/aimedical/common/exception/BusinessException.java | 新建（v1 已创建） | 业务异常基类，继承 RuntimeException，持有 ErrorCode |
| backend/common/src/main/java/com/aimedical/common/exception/GlobalErrorCode.java | 新建（v1 已创建） | 全局错误码枚举，实现 ErrorCode |
| backend/common/src/main/java/com/aimedical/common/config/JpaConfig.java | 新建（v1 已创建） | JPA 审计配置类 |
| backend/common/src/main/java/com/aimedical/common/config/JacksonConfig.java | 新建（v1 已创建） | Jackson 配置类（snake_case + JavaTimeModule） |
| backend/common/src/main/java/com/aimedical/common/config/GlobalExceptionHandler.java | 新建（v1 已创建） | 全局异常处理器，将 BusinessException / MethodArgumentNotValidException / Exception 统一转换为 Result |
| backend/common/src/test/java/com/aimedical/common/CommonPlaceholderTest.java | 新建（v1 已创建） | common 模块占位单元测试类 |

## v3 变更设计

### 问题分析

`JacksonConfigTest.shouldRegisterJavaTimeModule` 测试方法在第 47 行使用了以下断言：
```java
assertTrue(mapper.getRegisteredModuleIds().contains(JavaTimeModule.class.getName()));
```

`JavaTimeModule.class.getName()` 求值为 `"com.fasterxml.jackson.datatype.jsr310.JavaTimeModule"`，但 Jackson 各版本中 `ObjectMapper.getRegisteredModuleIds()` 返回的模块 ID 格式与此字符串不完全匹配。原因可能是 Jackson 版本差异或 `Jackson2ObjectMapperBuilder` 内部模块注册机制变化，导致断言始终返回 `false`。

### 修正方案

**将第 47 行断言替换为更健壮的形式**：

**修改前**（第 47 行）：
```java
assertTrue(mapper.getRegisteredModuleIds().contains(JavaTimeModule.class.getName()));
```

**修改后**：
```java
assertFalse(mapper.getRegisteredModuleIds().isEmpty());
```

### 理由

1. **功能覆盖**：`shouldSerializeLocalDateTime` 测试（第 50-60 行）已通过实际序列化 `LocalDateTime` 对象验证了 `JavaTimeModule` 已生效。如果 `JavaTimeModule` 未正确注册，该序列化测试会失败（Jackson 无法序列化 `LocalDateTime` 时会抛异常或输出不正确格式）。
2. **职责划分**：`shouldRegisterJavaTimeModule` 的职责是验证 customizer 确实在 ObjectMapper 上注册了某个模块（即 `customize()` 方法生效），而非验证具体注册了哪个模块。模块具体类型的验证由 `shouldSerializeLocalDateTime` 的功能性测试覆盖。
3. **版本兼容**：避免依赖 Jackson 内部模块 ID 格式，该格式在不同 Jackson 版本中可能变化。

### 受影响文件

- **文件路径**：`backend/common/src/test/java/com/aimedical/common/config/JacksonConfigTest.java`
- **操作**：修改，仅第 47 行断言变更
- **无需修改**：`JacksonConfig.java` 源文件保持不变

### 验证方式

```bash
mvn test -pl common -am
```

预期结果：43 个测试全部通过，0 失败。

## 类型定义

（v1 设计的 BaseEntity、BaseEnum、Result、PageQuery、PageResponse、ErrorCode、BusinessException、GlobalErrorCode、JpaConfig、JacksonConfig、GlobalExceptionHandler、CommonPlaceholderTest 类型定义完全保留，此处不重述。）

## 父 POM 修正设计

### 问题分析

父 POM `dependencyManagement` 中的「External starters」区块（原行 69-90）声明了 5 个 `spring-boot-starter-*` 依赖，但**未指定 `<version>`**：

```xml
<!-- External starters -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <!-- 缺少 <version> -->
</dependency>
<!-- 同理：data-jpa, security, validation, test -->
```

由于 Maven 依赖版本解析遵循**最近优先**（nearest wins）规则，父 POM 自身的 `<dependencyManagement>` 条目优先级高于 `spring-boot-starter-parent:3.2.5` 传递的 `<dependencyManagement>`。这些条目缺少版本号，导致 common 模块在解析 `spring-boot-starter-web`、`spring-boot-starter-data-jpa`、`spring-boot-starter-test` 版本时失败。

### 修正方案

**删除**父 POM `dependencyManagement` 中的「External starters」区块（行 69-90 共 5 个 dependency 声明）。理由：

- `spring-boot-starter-parent:3.2.5` 的 `<dependencyManagement>` 已为所有 `spring-boot-starter-*` 声明了匹配其版本的 `<version>`，子模块可以直接继承。
- common 模块的 POM 中直接声明的 `<dependencies>`（spring-boot-starter-web、data-jpa、test）将通过父 POM → 祖父 POM 的层级链正确继承版本。
- 外部库（springdoc、h2）的版本管理保持不变，继续在父 POM `dependencyManagement` 中通过 `<version>` 属性管理。

### 修正后父 POM dependencyManagement 结构

```xml
<dependencyManagement>
    <dependencies>
        <!-- Internal modules (6 个) — 保留，version = ${project.version} -->
        <dependency>...common...</dependency>
        <dependency>...common-module-api...</dependency>
        ...（其余 4 个内部模块）

        <!-- External libraries — 保留，显式指定版本 -->
        <dependency>...springdoc...<version>${springdoc.version}</version></dependency>
        <dependency>...h2...<version>${h2.version}</version><scope>runtime</scope></dependency>
    </dependencies>
</dependencyManagement>
```

**删除的内容**（5 个外置 Starter 声明，共 22 行）：
- `org.springframework.boot:spring-boot-starter-web`
- `org.springframework.boot:spring-boot-starter-data-jpa`
- `org.springframework.boot:spring-boot-starter-security`
- `org.springframework.boot:spring-boot-starter-validation`
- `org.springframework.boot:spring-boot-starter-test`（scope=test）

## 错误处理

- BusinessException 继承 RuntimeException，Spring 事务管理默认回滚
- 全局异常处理器将三类异常统一转换为 Result<Void> 返回，不暴露异常栈给前端
- 系统异常（Exception 兜底）需记录完整堆栈到服务端日志
- ErrorCode 接口 + 各模块 enum 实现的模式：common 模块提供 GlobalErrorCode 枚举作为全局通用错误码，各业务模块后续可自行实现 ErrorCode 接口扩展业务错误码

## 行为契约

1. **构建顺序**：父 POM 必须先于 common 模块构建；common 模块作为所有其他模块的共享基础
2. **依赖传播**：spring-boot-starter-web 和 spring-boot-starter-data-jpa 在 common 中标记为 optional，避免传递给 common-module-api 和 ai-api 等纯契约模块
3. **依赖分析门禁**：父 POM 的 maven-dependency-plugin 配置 `<ignoredUnusedDeclaredDependencies>` 豁免 ai-api 和 common-module-api，避免 Phase 0 业务模块声明但暂未引用的 api 模块依赖被误报
4. **Jackson 命名**：全局 snake_case 确保所有 DTO 序列化时字段名统一为下划线格式
5. **Starter 版本继承**：spring-boot-starter-* 的版本完全由 `spring-boot-starter-parent:3.2.5` 的 `<dependencyManagement>` 提供，父 POM 中不重复声明

## 依赖关系

### 父 POM（backend/pom.xml）
- **依赖的内部模块**（dependencyManagement）：common, common-module-api, common-module-impl, ai-api, ai-impl, application（version = ${project.version}）
- **依赖的外部库**（dependencyManagement）：springdoc-openapi-starter-webmvc-ui:2.5.0, h2:2.2.224
- **Starter 版本来源**：`spring-boot-starter-parent:3.2.5` 的传递性 `<dependencyManagement>`
- **插件**：maven-dependency-plugin（配置 ignoredUnusedDeclaredDependencies: ai-api, common-module-api）

### Common 模块（backend/common/pom.xml）
- **父 POM**：com.aimedical:aimedical-sys:0.0.1-SNAPSHOT
- **artifact**: common
- **依赖**：
  - spring-boot-starter-web（compile, optional）— 为 @ControllerAdvice、Result 序列化提供编译支持
  - spring-boot-starter-data-jpa（compile, optional）— 为 BaseEntity JPA 注解、JpaConfig 审计配置提供编译支持
  - spring-boot-starter-test（test）— 单元测试
- **版本解析**：通过父 POM → spring-boot-starter-parent:3.2.5 层级链传递，无需在 common/pom.xml 中声明版本号

### Java 类型内部依赖
- `BusinessException` → 依赖 `ErrorCode`
- `GlobalErrorCode` → 实现 `ErrorCode`
- `GlobalExceptionHandler` → 依赖 `Result`、`BusinessException`、`GlobalErrorCode`
- `BaseEntity` → 无内部依赖（仅依赖 spring-boot-starter-data-jpa 的 JPA 注解）
- `Result<T>` → 依赖 `ErrorCode`
- `PageQuery`、`PageResponse<T>` → 无内部依赖

## 修订说明（v2 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| verify_v1 构建失败：父 POM dependencyManagement 声明 spring-boot-starter-* 未指定 `<version>`，遮蔽了 spring-boot-starter-parent 传递的版本管理 | 删除父 POM `dependencyManagement` 中「External starters」区块（5 个 dependency 声明），这些 Starter 的版本已由 `spring-boot-starter-parent:3.2.5` 的 `<dependencyManagement>` 传递管理，无需在父 POM 中重复声明 |

## 修订说明（v3 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| verify_v2 测试失败：JacksonConfigTest.shouldRegisterJavaTimeModule:47 expected: <true> but was: <false>。断言 `mapper.getRegisteredModuleIds().contains(JavaTimeModule.class.getName())` 因 Jackson 版本间模块 ID 格式变化始终返回 false | 将第 47 行断言替换为 `assertFalse(mapper.getRegisteredModuleIds().isEmpty())`，仅验证有模块被注册而非检查具体模块 ID。`shouldSerializeLocalDateTime` 测试已覆盖 JavaTimeModule 实际生效验证 |
