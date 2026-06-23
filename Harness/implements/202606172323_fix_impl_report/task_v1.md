# 任务指令（v1）

## 动作
NEW

## 任务描述
修复 POM 依赖治理问题（T3 + T4 + T7）：删除父POM中Starter的冗余版本号、删除h2的scope、删除ai-impl的冗余common依赖。

涉及文件：
1. `AIMedical/backend/pom.xml`（T3+T4）
2. `AIMedical/backend/modules/ai/ai-impl/pom.xml`（T7）

### T3 — 父POM dependencyManagement中Starter冗余显式版本号
**位置**：`AIMedical/backend/pom.xml:84-109`
**改动**：删除五个Starter条目的 `<version>3.2.5</version>` 行
- `spring-boot-starter-web`（第84-88行）
- `spring-boot-starter-data-jpa`（第89-93行）
- `spring-boot-starter-security`（第94-98行）
- `spring-boot-starter-validation`（第99-103行）
- `spring-boot-starter-test`（第104-109行）

### T4 — 父POM dependencyManagement中h2误设scope
**位置**：`AIMedical/backend/pom.xml:78-83`
**改动**：删除h2条目的 `<scope>runtime</scope>`（第82行），仅保留版本号声明

### T7 — ai-impl POM冗余common依赖
**位置**：`AIMedical/backend/modules/ai/ai-impl/pom.xml:17-20`
**改动**：删除 `<dependency>` 块中包含 `<groupId>com.aimedical</groupId>` 和 `<artifactId>common</artifactId>` 的整个依赖条目

## 选择理由
三项均为POM级别的依赖管理修复，纯删除操作，无运行时风险。作为首批修复有三个好处：
1. 消除 mvn dependency:analyze 的噪音干扰
2. 简化后续任务的依赖基线
3. 高优先级（T3/T4为High，T7为Medium），修复窗口期开放中

## 任务上下文
- T3：父POM继承自 `spring-boot-starter-parent:3.2.5`（第6-11行），其BOM已统一管理所有Spring Boot Starter版本。dependencyManagement中重复标注版本号违反"版本由BOM统一管理"原则，增加升级时遗漏同步的风险
- T4：OOD §2.2及§9.1约定h2及其scope仅由application模块自行声明，父POM的dependencyManagement只负责管理版本号。当前写法强制所有模块继承runtime scope
- T7：ai-impl的所有Java源码import均来自 `com.aimedical.modules.ai.api.*`、`java.*`、`org.*`，无一导入 `com.aimedical.common`。`common` 作为传递性依赖可通过 `ai-api` 获得

## 已有代码上下文
### `backend/pom.xml`（当前120行）
- 第6-11行：继承自 spring-boot-starter-parent:3.2.5
- 第72-110行：`<dependencyManagement>` 节
  - 第78-83行：h2依赖含 `<scope>runtime</scope>`
  - 第84-88行：spring-boot-starter-web 含 `<version>3.2.5</version>`
  - 第89-93行：spring-boot-starter-data-jpa 含 `<version>3.2.5</version>`
  - 第94-98行：spring-boot-starter-security 含 `<version>3.2.5</version>`
  - 第99-103行：spring-boot-starter-validation 含 `<version>3.2.5</version>`
  - 第104-109行：spring-boot-starter-test 含 `<version>3.2.5</version>` + `<scope>test</scope>`

### `ai-impl/pom.xml`（当前31行）
- 第17-20行：声明 `com.aimedical:common` 依赖
- 第13-16行：声明 `com.aimedical:ai-api` 依赖（已在ai-api/pom.xml中声明common依赖）
