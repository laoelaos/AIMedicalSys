# 任务指令（v1）

## 动作
NEW

## 任务描述
修复父 POM 与子模块 POM 配置偏离（问题5/6/7），涉及 3 个文件：

### 1. 父 POM `<dependencyManagement>` 补充 5 个 starter（问题5）
**文件**：`AIMedical/backend/pom.xml`
**操作**：在 `<dependencyManagement><dependencies>` 内部依赖声明后方补充以下 5 个 starter 条目（版本由 Spring Boot parent BOM 兜底管理，无需显式 version）：
- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-security`
- `spring-boot-starter-validation`
- `spring-boot-starter-test`（scope: test）

### 2. common/pom.xml 移除 validation starter（问题6）
**文件**：`AIMedical/backend/common/pom.xml`
**操作**：移除 `<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId><optional>true</optional></dependency>` 整个块。

### 3. 父 POM 移出 patient/doctor/admin 的 ignoredUnusedDeclaredDependency 至 application/pom.xml（问题7，方案A）
**文件**：`AIMedical/backend/pom.xml` + `AIMedical/backend/application/pom.xml`
**操作**：
- **pom.xml**：从 `<ignoredUnusedDeclaredDependencies>` 中删除 `com.aimedical:patient`、`com.aimedical:doctor`、`com.aimedical:admin` 三个条目，保留 `com.aimedical:ai-api` 和 `com.aimedical:common-module-api`（供其他子模块继承）
- **application/pom.xml**：在 `<build><plugins>` 块中新增 `maven-dependency-plugin` 声明（使用父 POM pluginManagement 中已声明的版本，无需指定 `version`），在其 `<configuration>` 中放置 `<ignoredUnusedDeclaredDependencies>` 包含**全部 5 个条目**（ai-api、common-module-api、patient、doctor、admin），避免 Maven 子模块插件配置替换合并语义导致 ai-api/common-module-api 豁免丢失

### 4. 父 POM dependencyManagement 移除 patient/doctor/admin 业务模块声明（审查问题2）
**文件**：`AIMedical/backend/pom.xml`
**操作**：从 `<dependencyManagement><dependencies>` 中删除以下三个业务模块条目：
- `com.aimedical:patient`
- `com.aimedical:doctor`
- `com.aimedical:admin`
**理由**：OOD §2.1 骨架的 dependencyManagement 仅包含 6 个基础设施内部模块（common、common-module-api、common-module-impl、ai-api、ai-impl、application），不含业务模块。当前 POM 多出这 3 项冗余偏离。

## 选择理由
底层基础设施优先。四个 POM 修正相互独立、无逻辑变更风险，适合作为首轮任务。

## 任务上下文
- OOD §2.2 要求 dependencyManagement 统一声明 5 个外部 starter 版本
- OOD §2.2 规约 common 模块仅含 web (optional) 和 data-jpa (optional)，不含 validation
- OOD §2.2 的 ignoredUnusedDeclaredDependencies 仅包含 ai-api 和 common-module-api
- 用户意见已确认：问题5→代码向设计对齐；问题6→从 common/pom.xml 移除；问题7→将三个条目移入 application/pom.xml
- OOD §2.1 dependencyManagement 不包含 patient/doctor/admin 三个业务模块
- **方案A（审查决议）**：application/pom.xml 的 `<ignoredUnusedDeclaredDependencies>` 包含全部 5 个条目（ai-api、common-module-api、patient、doctor、admin），避免 Maven 子模块插件配置替换合并语义导致 ai-api/common-module-api 豁免丢失

## 已有代码上下文
- `AIMedical/backend/pom.xml` (120行)：dependencyManagement 含 9 内部模块 + springdoc + h2；maven-dependency-plugin 的 ignoredUnusedDeclaredDependencies 含 ai-api、common-module-api、patient、doctor、admin
- `AIMedical/backend/common/pom.xml` (38行)：dependencies 含 web(optional)、data-jpa(optional)、validation(optional)、test(test)
- `AIMedical/backend/application/pom.xml` (97行)：dependencies 含所有内部模块和 starter，build 中仅有 spring-boot-maven-plugin，无 maven-dependency-plugin
- Maven 插件配置合并规则：子模块 `<ignoredUnusedDeclaredDependencies>` 整体替换继承自父 POM 的同名元素，而非合并追加

## 修订说明（v1 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] 问题7 实现方案未考虑 Maven 插件配置合并语义，application 模块将丢失 ai-api/common-module-api 豁免 | 采用方案A：application/pom.xml 的 `<ignoredUnusedDeclaredDependencies>` 包含全部 5 个条目（ai-api、common-module-api、patient、doctor、admin），而非仅 3 个业务模块 |
| [一般] dependencyManagement 中的 patient/doctor/admin 条目未计划清理 | 新增第 4 项操作：从 `<dependencyManagement>` 中移除 `com.aimedical:patient`、`com.aimedical:doctor`、`com.aimedical:admin` 三个条目 |
