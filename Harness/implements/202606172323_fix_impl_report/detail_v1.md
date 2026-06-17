# 详细设计（v1）

## 概述

修复 POM 依赖治理问题（T3 + T4 + T7），三项均为纯删除操作，无运行时风险。涉及两个 POM 文件，共 7 处删除。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/pom.xml` | 编辑 | 删除 5 个 Starter 的 `<version>3.2.5</version>` + 删除 h2 的 `<scope>runtime</scope>` |
| `AIMedical/backend/modules/ai/ai-impl/pom.xml` | 编辑 | 删除 `com.aimedical:common` 整个 dependency 块 |

## 操作定义

### OP-01：删除父 POM 中五个 Starter 的版本号（T3）

**文件**：`AIMedical/backend/pom.xml`

| 目标依赖 | 所在行范围 | 删除行 | 行内容 |
|---------|-----------|--------|--------|
| `spring-boot-starter-web` | 84-88 | 87 | `<version>3.2.5</version>` |
| `spring-boot-starter-data-jpa` | 89-93 | 92 | `<version>3.2.5</version>` |
| `spring-boot-starter-security` | 94-98 | 97 | `<version>3.2.5</version>` |
| `spring-boot-starter-validation` | 99-103 | 102 | `<version>3.2.5</version>` |
| `spring-boot-starter-test` | 104-109 | 107 | `<version>3.2.5</version>` |

**约束**：版本继承自 `spring-boot-starter-parent:3.2.5` 的 BOM，无需显式指定。`spring-boot-starter-test` 的 `<scope>test</scope>`（第 108 行）保留不变。

### OP-02：删除父 POM 中 h2 的 scope（T4）

**文件**：`AIMedical/backend/pom.xml`

**删除行**：第 82 行 `<scope>runtime</scope>`

**约束**：仅删除 scope 行，保留 `<version>${h2.version}</version>`（第 81 行）。h2 的 scope 约定由各子模块在模块级 pom.xml 自行声明（仅 application 模块使用 runtime scope）。

### OP-03：删除 ai-impl POM 中冗余 common 依赖（T7）

**文件**：`AIMedical/backend/modules/ai/ai-impl/pom.xml`

**删除范围**：第 17-20 行，即：

```xml
<dependency>
    <groupId>com.aimedical</groupId>
    <artifactId>common</artifactId>
</dependency>
```

**理由**：ai-impl 的 Java 代码无任何 `com.aimedical.common.*` 导入。`common` 已作为 compile scope 依赖通过 `ai-api` 传递获得。删除后第 12 行 `<dependencies>` 内的剩余条目为 `ai-api`、`spring-boot-starter`、`spring-boot-starter-test`。

## 错误处理

无新增错误处理逻辑。操作为纯 XML 行删除，不涉及 Java 代码变更。验证手段为 `mvn compile` 和 `mvn dependency:tree`。

## 行为契约

1. **操作顺序**：无依赖关系，三个操作可任意顺序执行，也可并行执行
2. **验证**：删除后运行 `mvn compile` 确认依赖解析正常；`mvn dependency:tree` 确认版本号正确继承自 Spring Boot BOM
3. **回滚**：若验证失败，通过 `git checkout -- <file>` 恢复原始文件

## 依赖关系

- 受影响的文件：`AIMedical/backend/pom.xml`、`AIMedical/backend/modules/ai/ai-impl/pom.xml`
- 无新增外部依赖
- 无运行时代码变更
