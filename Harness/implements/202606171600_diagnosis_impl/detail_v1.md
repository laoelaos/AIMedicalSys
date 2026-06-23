# 详细设计（v1）

## 概述

修复父 POM 与子模块 POM 之间的 4 项配置偏离问题（问题5/6/7 + 审查问题2），使 `dependencyManagement`、`ignoredUnusedDeclaredDependencies`、common 模块依赖声明与 OOD §2.1/§2.2 的设计规约完全对齐。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/pom.xml` | 修改 | (1) `dependencyManagement` 补充 5 个 starter，删除 patient/doctor/admin；(2) `ignoredUnusedDeclaredDependencies` 删除 patient/doctor/admin |
| `AIMedical/backend/common/pom.xml` | 修改 | 删除 `spring-boot-starter-validation` 整个 `<dependency>` 块 |
| `AIMedical/backend/application/pom.xml` | 修改 | 在 `<build><plugins>` 中新增 `maven-dependency-plugin`，配置含全部 5 个 `ignoredUnusedDeclaredDependency` 条目 |

## 修改详规

### 修改 1：父 POM `dependencyManagement` 补充 5 个 starter（问题5）

**文件**：`AIMedical/backend/pom.xml`

**操作**：在现有 `<dependencyManagement><dependencies>` 末尾（`</dependencies>` 闭合标签之前），于 h2 条目之后追加以下 5 个 `<dependency>` 条目：

| groupId | artifactId | scope | version |
|---------|-----------|-------|---------|
| `org.springframework.boot` | `spring-boot-starter-web` | 无（默认 compile） | 无（由 Spring Boot parent BOM 兜底） |
| `org.springframework.boot` | `spring-boot-starter-data-jpa` | 无（默认 compile） | 无 |
| `org.springframework.boot` | `spring-boot-starter-security` | 无（默认 compile） | 无 |
| `org.springframework.boot` | `spring-boot-starter-validation` | 无（默认 compile） | 无 |
| `org.springframework.boot` | `spring-boot-starter-test` | `test` | 无 |

**插入位置锚点**：h2 条目的 `</dependency>` 闭合标签之后、`</dependencies>` 闭合标签之前。

### 修改 2：父 POM `dependencyManagement` 移除 3 个业务模块（审查问题2）

**文件**：`AIMedical/backend/pom.xml`

**操作**：从 `<dependencyManagement><dependencies>` 中删除以下 3 个 `<dependency>` 块（位于 application 条目之后、springdoc 条目之前）：

- `com.aimedical:patient`
- `com.aimedical:doctor`
- `com.aimedical:admin`

**影响**：删除后 dependencyManagement 共保留 6 个内部模块（common、common-module-api、common-module-impl、ai-api、ai-impl、application）+ springdoc + h2 + 5 个 starter，与 OOD §2.1 骨架对齐。

### 修改 3：common/pom.xml 移除 validation starter（问题6）

**文件**：`AIMedical/backend/common/pom.xml`

**操作**：删除以下整个 `<dependency>` 块（位于 data-jpa 之后、test 之前）：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
    <optional>true</optional>
</dependency>
```

**影响**：删除后 common 模块的依赖列表恢复为 web(optional) + data-jpa(optional) + test(test)，与 OOD §2.2 的 common 模块依赖规约一致。子模块（patient/doctor/admin）已独立声明 validation starter 为 compile 依赖，不受影响。

### 修改 4：父 POM `ignoredUnusedDeclaredDependencies` 移除 3 个业务模块（问题7）

**文件**：`AIMedical/backend/pom.xml`

**操作**：在 `<build><plugins>` 的 `maven-dependency-plugin` 配置中，从 `<ignoredUnusedDeclaredDependencies>` 删除以下 3 个条目，仅保留 ai-api 和 common-module-api：

```xml
<ignoredUnusedDeclaredDependency>com.aimedical:patient</ignoredUnusedDeclaredDependency>
<ignoredUnusedDeclaredDependency>com.aimedical:doctor</ignoredUnusedDeclaredDependency>
<ignoredUnusedDeclaredDependency>com.aimedical:admin</ignoredUnusedDeclaredDependency>
```

**保留条目**：
- `com.aimedical:ai-api`
- `com.aimedical:common-module-api`

### 修改 5：application/pom.xml 新增 maven-dependency-plugin（问题7 方案A）

**文件**：`AIMedical/backend/application/pom.xml`

**操作**：在 `<build><plugins>` 块中新增 `maven-dependency-plugin` 声明，位于 `spring-boot-maven-plugin` 之前或之后均可。使用父 POM `pluginManagement` 中已声明的 Maven 默认版本，**不指定** `<version>`。

**新增插件配置内容**：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-dependency-plugin</artifactId>
    <configuration>
        <ignoredUnusedDeclaredDependencies>
            <ignoredUnusedDeclaredDependency>com.aimedical:ai-api</ignoredUnusedDeclaredDependency>
            <ignoredUnusedDeclaredDependency>com.aimedical:common-module-api</ignoredUnusedDeclaredDependency>
            <ignoredUnusedDeclaredDependency>com.aimedical:patient</ignoredUnusedDeclaredDependency>
            <ignoredUnusedDeclaredDependency>com.aimedical:doctor</ignoredUnusedDeclaredDependency>
            <ignoredUnusedDeclaredDependency>com.aimedical:admin</ignoredUnusedDeclaredDependency>
        </ignoredUnusedDeclaredDependencies>
    </configuration>
</plugin>
```

**理由（方案A）**：Maven 子模块 `<ignoredUnusedDeclaredDependencies>` 整体替换继承自父 POM 的同名元素。若 application 模块仅声明 3 个业务模块条目，父 POM 继承的 ai-api/common-module-api 豁免将被覆盖丢失。因此必须在 application 模块中完整列出全部 5 个条目。

## 行为契约

### 操作顺序约束

4 项修改相互独立，无严格顺序依赖。推荐按文件分组执行：

1. 先完成 `common/pom.xml`（修改 3）——独立无副作用
2. 再完成 `application/pom.xml`（修改 5）——独立无副作用
3. 最后完成 `pom.xml`（修改 1、2、4）——集中处理一个文件减少打开次数

### 验证方法

修改完成后在 `AIMedical/backend/` 目录下执行：

```bash
mvn dependency:analyze -pl application -am
```

确认：
- `dependency:analyze` 门禁通过，无报错
- common 模块编译成功：`mvn compile -pl common -am`
- 全量编译：`mvn compile -DskipTests`

## 依赖关系

- 本任务仅修改 POM XML 文件，无 Java/TypeScript 代码改动
- 无新增外部依赖
- 无运行时行为变化（所有更改均为构建期配置）
- 不涉及新增/修改/删除任何 API
