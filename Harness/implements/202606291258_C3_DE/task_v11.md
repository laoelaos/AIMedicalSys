# 任务指令（v11）

## 动作
RETRY

## 任务描述
修复 prescription 模块 Maven 构建配置问题——在父 POM（`backend/pom.xml`）的 `dependencyManagement` 中添加 `com.aimedical:patient` 依赖版本声明。

具体修改：在 `backend/pom.xml` 的 `<dependencyManagement><dependencies>` 中，在现有内部模块条目之后（`application` 条目之后、`External libraries` 注释之前），添加：
```xml
<dependency>
    <groupId>com.aimedical</groupId>
    <artifactId>patient</artifactId>
    <version>${project.version}</version>
</dependency>
```

## 选择理由
v10 verify 失败：Maven 报错 `'dependencies.dependency.version' for com.aimedical:patient:jar is missing`，原因是 parent POM 的 `dependencyManagement` 未包含 `patient` 模块，导致 `prescription/pom.xml` 中声明的 `com.aimedical:patient` 依赖无法继承版本号。其他内部模块（`common`、`common-module-api`、`ai-api` 等）均已正确注册，`patient` 是唯一缺失项。此修复为纯 POM 配置变更，不涉及 Java 代码改动。

## 任务上下文
### 失败详情
```
[ERROR] 'dependencies.dependency.version' for com.aimedical:patient:jar is missing. @ prescription/pom.xml line 30, column 21
[ERROR] The build could not read 1 project -> [Help 1]
```

### 父 POM 当前状态
`backend/pom.xml` 的 `dependencyManagement` 已包含：`common`、`common-module-api`、`common-module-impl`、`ai-api`、`ai-impl`、`application`，但缺少 `patient`。

### prescription/pom.xml 当前状态
第 30–33 行声明了 `com.aimedical:patient` 依赖，未指定 `<version>`（预期继承自父 POM dependencyManagement）。

## RETRY 说明
失败原因：parent POM 缺少 `patient` 模块的 dependencyManagement 条目。修复后 prescription 模块应能正常编译。该任务仅涉及 POM 配置变更，无需修改 Java 源代码。
