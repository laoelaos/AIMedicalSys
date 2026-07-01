# 详细设计（v7）

## 概述

在 backend/pom.xml 中注册 consultation、prescription、medical-record 三个新 Maven 模块；为每个模块创建 pom.xml 和 src/main/java/src/test/java 目录骨架。三个模块为 T8–T11 业务实现代码提供编译容器，参照现有 patient/doctor/admin 模块的样板结构。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/pom.xml` | 修改 | 追加 `<module>modules/consultation</module>`、`<module>modules/prescription</module>`、`<module>modules/medical-record</module>` 到 `<modules>` 块 |
| `AIMedical/backend/modules/consultation/pom.xml` | 新建 | consultation 模块 Maven 构建描述（参照 patient 模块） |
| `AIMedical/backend/modules/prescription/pom.xml` | 新建 | prescription 模块 Maven 构建描述（参照 patient 模块） |
| `AIMedical/backend/modules/medical-record/pom.xml` | 新建 | medical-record 模块 Maven 构建描述（参照 patient 模块） |
| `AIMedical/backend/modules/consultation/src/main/java/` | 新建目录 | consultation Java 源码根 |
| `AIMedical/backend/modules/consultation/src/test/java/` | 新建目录 | consultation 测试源码根 |
| `AIMedical/backend/modules/prescription/src/main/java/` | 新建目录 | prescription Java 源码根 |
| `AIMedical/backend/modules/prescription/src/test/java/` | 新建目录 | prescription 测试源码根 |
| `AIMedical/backend/modules/medical-record/src/main/java/` | 新建目录 | medical-record Java 源码根 |
| `AIMedical/backend/modules/medical-record/src/test/java/` | 新建目录 | medical-record 测试源码根 |

## 类型定义

### backend/pom.xml（父 POM）

**形态**：XML（pom.xml）
**路径**：`AIMedical/backend/pom.xml`
**职责**：注册三个新子模块

在现有 `<modules>` 块中 `modules/admin` 之后、`application` 之前插入三行：

```xml
<module>modules/consultation</module>
<module>modules/prescription</module>
<module>modules/medical-record</module>
```

插入后 `<modules>` 顺序：

```xml
<modules>
    <module>common</module>
    <module>modules/common-module</module>
    <module>modules/ai</module>
    <module>modules/patient</module>
    <module>modules/doctor</module>
    <module>modules/admin</module>
    <module>modules/consultation</module>     <!-- NEW -->
    <module>modules/prescription</module>     <!-- NEW -->
    <module>modules/medical-record</module>   <!-- NEW -->
    <module>application</module>
    <module>integration</module>
</modules>
```

**不变部分**：`<dependencyManagement>`、`<build>`、`<properties>` 均不变。

### consultation/pom.xml

**形态**：XML（pom.xml）
**路径**：`AIMedical/backend/modules/consultation/pom.xml`
**职责**：consultation 模块构建描述

```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.aimedical</groupId>
        <artifactId>aimedical-sys</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>
    <artifactId>consultation</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>com.aimedical</groupId>
            <artifactId>common</artifactId>
        </dependency>
        <dependency>
            <groupId>com.aimedical</groupId>
            <artifactId>common-module-api</artifactId>
        </dependency>
        <dependency>
            <groupId>com.aimedical</groupId>
            <artifactId>ai-api</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
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

### prescription/pom.xml

**形态**：XML（pom.xml）
**路径**：`AIMedical/backend/modules/prescription/pom.xml`
**职责**：prescription 模块构建描述

与 consultation/pom.xml 完全相同的结构，仅 `<artifactId>` 替换为 `prescription`。

### medical-record/pom.xml

**形态**：XML（pom.xml）
**路径**：`AIMedical/backend/modules/medical-record/pom.xml`
**职责**：medical-record 模块构建描述

与 consultation/pom.xml 完全相同的结构，仅 `<artifactId>` 替换为 `medical-record`。

## 目录骨架

每个模块下创建：

```
modules/{moduleName}/
├── src/
│   ├── main/java/              (空目录，占位)
│   └── test/java/              (空目录，占位)
```

项目已有 `.gitkeep` 惯例——若其他模块在空目录下有 `.gitkeep` 文件，也一并创建以保持 git 跟踪；否则保持空目录即可（Maven compile 不会因空目录报错）。

## 错误处理

- pom.xml 格式错误会导致 Maven 构建失败，需确保 XML 结构合法
- `<relativePath>../../pom.xml</relativePath>` 与其他 modules/ 下模块一致

## 行为契约

- 父 pom modules 列表插入后，`mvn compile` 应成功编译三个空模块（无源码，仅解析 pom 和依赖）
- 每个新模块 pom.xml 的 artifactId 唯一，不与现有模块冲突
- 依赖版本全部继承自父 pom `<dependencyManagement>`，不在子 pom 中重复声明版本号

## 依赖关系

- **编译期**：三个新模块均依赖 common、common-module-api、ai-api（与 patient/doctor/admin 一致）
- **父 pom 管理**：`<dependencyManagement>` 中已包含 common、common-module-api、ai-api 的版本声明
- **构建插件**：JaCoCo 继承父 pom 配置，子模块通过 `<properties>` 覆盖 skip 为 false 以启用覆盖率检查
- **被依赖**：后续 T8–T11 将各模块的 Java 源文件写入 `src/main/java/` 和 `src/test/java/` 对应包路径
