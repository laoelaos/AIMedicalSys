# 任务指令（v2）

## 动作
RETRY

## 任务描述
修正 POM 版本解析缺陷和测试 XPath 语法错误，使 `mvn compile` 通过。涉及 3 个文件：

### 修正A：父 POM dependencyManagement 补充版本号
**文件**：`AIMedical/backend/pom.xml`
**操作**：在 `<dependencyManagement>` 的 5 个 Spring Boot starter 条目中各添加 `<version>${spring-boot.version}</version>`，分别位于 `artifactId` 之后、闭合 `</dependency>` 之前：

```xml
<!-- spring-boot-starter-web -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>${spring-boot.version}</version>
</dependency>
<!-- spring-boot-starter-data-jpa -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
    <version>${spring-boot.version}</version>
</dependency>
<!-- spring-boot-starter-security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
    <version>${spring-boot.version}</version>
</dependency>
<!-- spring-boot-starter-validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
    <version>${spring-boot.version}</version>
</dependency>
<!-- spring-boot-starter-test -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <version>${spring-boot.version}</version>
    <scope>test</scope>
</dependency>
```

**根因**：Maven `dependencyManagement` 的最近声明优先规则——子 POM 匹配到自己的条目后停止向父 BOM 查找，无版本号导致不可解析。`${spring-boot.version}` 由 `spring-boot-starter-parent` 在属性区定义，值等于 parent version（3.2.5）。

**scope 与 version 顺序**：`<scope>`（或 `<optional>`）应在 `<version>` 之后、`</dependency>` 之前，以匹配项目 POM 的 XML 约定风格。

### 修正B：application/pom.xml 内部版本补偿
**文件**：`AIMedical/backend/application/pom.xml`
**操作**：为 patient/doctor/admin 三个依赖添加 `<version>${project.version}</version>`：

```xml
<dependency>
    <groupId>com.aimedical</groupId>
    <artifactId>patient</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>com.aimedical</groupId>
    <artifactId>doctor</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>com.aimedical</groupId>
    <artifactId>admin</artifactId>
    <version>${project.version}</version>
</dependency>
```

**根因**：父 POM `<dependencyManagement>` 已按 OOD §2.1 对齐删除 patient/doctor/admin 条目，这些业务模块不再受父 POM 统一版本管理。application 作为直接消费方需自行提供版本。使用 `${project.version}` 确保与主项目版本一致。

### 修正C：ParentPomTest XPath 谓词语法修复
**文件**：`AIMedical/backend/common/src/test/java/com/aimedical/common/pom/ParentPomTest.java`
**操作**：将 `dependencyManagementShouldNotContainBusinessModules` 和 `dependencyManagementShouldContainCoreInternalModules` 两个方法中错误的 XPath 表达式修正为正确的谓词语法。

**错误模式**（line 55-58 & 63-69）：
```java
// 当前（错误）—— `and` 在谓词括号外
String base = "/project/dependencyManagement/dependencies/dependency[groupId='com.aimedical']";
assertFalse(exists(base + " and artifactId='patient'"));  // 等效于 boolean(path) and boolean(artifactId='patient')
```

**修正后**：
```java
// 修正—— `and` 在谓词括号内
String base = "/project/dependencyManagement/dependencies/dependency";
assertFalse(exists(base + "[groupId='com.aimedical' and artifactId='patient']"));
```

**影响分析**：
- `dependencyManagementShouldContainCoreInternalModules`：当前 XPath 求值为 `true and false = false`，`assertTrue` 失败（即为 verify_v1.md 报告的 1 个测试失败）
- `dependencyManagementShouldNotContainBusinessModules`：当前 XPath 同样求值为 `true and false = false`，但 `assertFalse` 期待 `false`，因此"偶然通过"——实际上该测试的 XPath 存在相同错误

## 选择理由
同一任务（R1 POM 基础设施修复）的修正重试。三个修正方向分别对应验证报告中列出的三个缺陷，互为独立、低风险，适合同轮修复。

## 任务上下文
- OOD §2.2 要求 dependencyManagement 统一声明 5 个外部 starter 版本——新增 `<version>${spring-boot.version}</version>` 对齐设计意图
- OOD §2.1 dependencyManagement 不包含 patient/doctor/admin——application 独立提供版本补偿
- Maven `dependencyManagement` 最近声明优先规则：子 POM 条目完全覆盖父 POM 同 G:A 条目
- `${spring-boot.version}` 属性由 `spring-boot-starter-parent` 定义，值恒等于 parent version
- XPath 谓词语法：多个条件需在 `[...]` 内使用 `and` 连接，形如 `[condition1 and condition2]`

## 已有代码上下文
- `pom.xml:84-104`：5 个 starter 条目当前无 `<version>`（验证失败根因 A）
- `application/pom.xml:34-45`：patient/doctor/admin 依赖当前无 `<version>`（验证失败根因 B）
- `ParentPomTest.java:53-70`：两个测试方法的 XPath `and` 在谓词括号外（验证失败点 3）
- `ParentPomTest.java:39-46`：正确示范——`dependencyManagementShouldContainAllSpringBootStarters` 的 XPath 谓词正确使用 `[groupId='...' and artifactId='...']`

## RETRY 说明
**失败原因摘要**：
1. **严重**：dependencyManagement 中 5 个 Spring Boot starter 无 `<version>`，Maven 子 POM 覆盖父 BOM 导致版本不可解析，波及所有 10 个子模块
2. **严重**：application/pom.xml 依赖 patient/doctor/admin 无版本（父 POM 删除三者后丢失版本来源）
3. **一般**：ParentPomTest XPath 语法错误（`and` 在谓词括号外）

**修正方向**：
- A：pom.xml → 5 starter 添加 `<version>${spring-boot.version}</version>`
- B：application/pom.xml → patient/doctor/admin 添加 `<version>${project.version}</version>`
- C：ParentPomTest.java → 将 `and` 条件移入谓词括号内
