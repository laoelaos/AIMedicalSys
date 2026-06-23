# 详细设计（v2）

## 概述

修复 v1 验证失败的三个根因：父 POM `dependencyManagement` 中 5 个 Spring Boot starter 缺少 `<version>`、application/pom.xml 中 patient/doctor/admin 缺少 `<version>`、ParentPomTest XPath 谓词语法错误。目标：`mvn compile` 通过。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/pom.xml` | 修改 | `dependencyManagement` 中 5 个 Spring Boot starter 添加 `<version>${spring-boot.version}</version>` |
| `AIMedical/backend/application/pom.xml` | 修改 | patient/doctor/admin 依赖添加 `<version>${project.version}</version>` |
| `AIMedical/backend/common/src/test/java/com/aimedical/common/pom/ParentPomTest.java` | 修改 | 修复 `dependencyManagementShouldNotContainBusinessModules` 和 `dependencyManagementShouldContainCoreInternalModules` 的 XPath 谓词语法 |

## 修改详规

### 修改 A：父 POM dependencyManagement 补充 starter 版本号

**文件**：`AIMedical/backend/pom.xml`

**操作**：在 `<dependencyManagement><dependencies>` 中，对以下 5 个 `<dependency>` 条目各添加 `<version>${spring-boot.version}</version>`，位于 `artifactId` 之后、`</dependency>` 之前。

| 行号 | artifactId | 当前内容 | 修改后 |
|------|-----------|---------|--------|
| 84-87 | spring-boot-starter-web | 无 `<version>` | 插入 `<version>${spring-boot.version}</version>` 于 line 86 后 |
| 88-91 | spring-boot-starter-data-jpa | 无 `<version>` | 插入 `<version>${spring-boot.version}</version>` 于 line 90 后 |
| 92-95 | spring-boot-starter-security | 无 `<version>` | 插入 `<version>${spring-boot.version}</version>` 于 line 94 后 |
| 96-99 | spring-boot-starter-validation | 无 `<version>` | 插入 `<version>${spring-boot.version}</version>` 于 line 98 后 |
| 100-104 | spring-boot-starter-test | 仅有 `<scope>test</scope>` | 先插入 `<version>${spring-boot.version}</version>`，再保留 `<scope>test</scope>` |

**scope 与 version 顺序**：严格按照 `<version>` 在 `<scope>` 之前的顺序。最终 block：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <version>${spring-boot.version}</version>
    <scope>test</scope>
</dependency>
```

**根因**：Maven `dependencyManagement` 最近声明优先规则——子 POM 匹配到自己的条目后停止向父 BOM 查找，无版本号导致不可解析。`${spring-boot.version}` 由 `spring-boot-starter-parent` 定义，值 3.2.5。

### 修改 B：application/pom.xml 业务模块版本补偿

**文件**：`AIMedical/backend/application/pom.xml`

**操作**：在以下 3 个 `<dependency>` 块中各添加 `<version>${project.version}</version>`，位于 `artifactId` 之后、`</dependency>` 之前。

| 行号 | artifactId | 当前内容 | 修改后 |
|------|-----------|---------|--------|
| 34-37 | patient | 仅有 `<artifactId>patient</artifactId>` | 其后插入 `<version>${project.version}</version>` |
| 38-41 | doctor | 仅有 `<artifactId>doctor</artifactId>` | 其后插入 `<version>${project.version}</version>` |
| 42-45 | admin | 仅有 `<artifactId>admin</artifactId>` | 其后插入 `<version>${project.version}</version>` |

**最终 block**：
```xml
<dependency>
    <groupId>com.aimedical</groupId>
    <artifactId>patient</artifactId>
    <version>${project.version}</version>
</dependency>
```

**根因**：父 POM `dependencyManagement` 已删除 patient/doctor/admin 条目（OOD §2.1），application 作为直接消费方需自行提供版本。

### 修改 C：ParentPomTest XPath 谓词语法修复

**文件**：`AIMedical/backend/common/src/test/java/com/aimedical/common/pom/ParentPomTest.java`

**操作**：将 `dependencyManagementShouldNotContainBusinessModules` 和 `dependencyManagementShouldContainCoreInternalModules` 两个方法中的 XPath 表达式从错误模式修正为正确模式。

**错误模式**（当前）：
```java
// 在谓词括号外使用 and
String base = "/project/dependencyManagement/dependencies/dependency[groupId='com.aimedical']";
assertFalse(exists(base + " and artifactId='patient'"));
```

该表达式等价于 `boolean(path) and boolean(artifactId='patient')`，其中 `path` 匹配到节点时求值为 true，而 `artifactId='patient'` 无上下文始终为 false，导致 assertion 语义完全错误。

**正确模式**（修正后）：
```java
// and 在谓词括号内
String base = "/project/dependencyManagement/dependencies/dependency";
assertFalse(exists(base + "[groupId='com.aimedical' and artifactId='patient']"));
```

**方法级修改**：

1. `dependencyManagementShouldNotContainBusinessModules` (line 54-58)：
   - 将 `base` 从 `".../dependency[groupId='com.aimedical']"` 改为 `".../dependency"`
   - 将三个 `exists(base + " and artifactId='...'")` 改为 `exists(base + "[groupId='com.aimedical' and artifactId='...']")`

2. `dependencyManagementShouldContainCoreInternalModules` (line 62-69)：
   - 将 `base` 从 `".../dependency[groupId='com.aimedical']"` 改为 `".../dependency"`
   - 将六个 `exists(base + " and artifactId='...'")` 改为 `exists(base + "[groupId='com.aimedical' and artifactId='...']")`

**参考模式**（已验证正确的现有方法 line 39-46）：
```java
String base = "/project/dependencyManagement/dependencies/dependency";
assertTrue(exists(base + "[groupId='org.springframework.boot' and artifactId='spring-boot-starter-web']"));
```

## 行为契约

### 操作顺序约束

三项修改相互独立，无严格顺序依赖。

### 验证方法

修改完成后在 `AIMedical/backend/` 目录下执行：

```bash
mvn compile -DskipTests
```

预期：`BUILD SUCCESS`，0 errors。

单元测试（验证 XPath 修复）：
```bash
mvn test -pl common -Dtest=ParentPomTest
```

预期：13/13 测试通过。

### 回滚策略

`git diff` 确认仅修改目标行，无意外变更后提交。若验证失败，`git checkout -- <file>` 逐个回退修正。

## 依赖关系

- 本任务仅修改 POM XML 文件和测试 Java 文件
- 无新增外部依赖
- 无运行时行为变化
- `${spring-boot.version}` 属性已由 `spring-boot-starter-parent` 提供
- `${project.version}` 属性由 Maven 自动设为 `0.0.1-SNAPSHOT`
