# 详细设计（v2）

## 概述

基于 v1 实现失败验证（R1：子 POM dependencyManagement 条目覆盖父 BOM 版本，仅删除 version 导致编译失败），采用修正方案 A：**完全移除**父 POM dependencyManagement 中 5 个 Spring Boot Starter 条目，而非仅删除 version。同时保留 v1 中已验证的 OP-02（h2 scope 删除）和 OP-03（ai-impl common 删除），并新增 OP-04（标记 ParentPomVersionTest 基线测试为废弃）。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/pom.xml` | 编辑 | 删除 5 个 Starter 的整个 dependency 块 + 删除 h2 的 `<scope>runtime</scope>` |
| `AIMedical/backend/modules/ai/ai-impl/pom.xml` | 编辑 | 删除 `com.aimedical:common` 整个 dependency 块 |
| `AIMedical/backend/common/src/test/java/com/aimedical/common/pom/ParentPomVersionTest.java` | 编辑 | 添加类级别 `@Disabled` 注解 + 添加 `org.junit.jupiter.api.Disabled` import |

## 类型定义

（本任务为纯 XML 和注解操作，无新增 Java 类型）

## 操作定义

### OP-01（T3，修正）：完全移除父 POM dependencyManagement 中 5 个 Starter 条目

**文件**：`AIMedical/backend/pom.xml`

**删除范围**：第 84-109 行，即以下 5 个完整 dependency 块：

| 目标依赖 | 行范围 | 说明 |
|---------|-------|------|
| `spring-boot-starter-web` | 84-88 | 含 `<version>3.2.5</version>` |
| `spring-boot-starter-data-jpa` | 89-93 | 含 `<version>3.2.5</version>` |
| `spring-boot-starter-security` | 94-98 | 含 `<version>3.2.5</version>` |
| `spring-boot-starter-validation` | 99-103 | 含 `<version>3.2.5</version>` |
| `spring-boot-starter-test` | 104-109 | 含 `<version>3.2.5</version>` 和 `<scope>test</scope>` |

**理由**：完全移出 dependencyManagement 后，子模块在 `<dependencies>` 中声明这些 starter 时，版本自动从 `spring-boot-starter-parent:3.2.5` 的 BOM 继承，不再被子 POM 的 dependencyManagement 覆盖。

**影响**：10 个子模块（common, common-module-api, common-module-impl, ai-api, ai-impl, patient, doctor, admin, application, integration）的 starter 版本解析将从 parent BOM 获取，而非 dependencyManagement。

### OP-02（T4，与 v1 一致）：删除父 POM 中 h2 的 runtime scope

**文件**：`AIMedical/backend/pom.xml`

**删除行**：第 82 行 `<scope>runtime</scope>`

**约束**：仅删除 scope 行，保留 `<version>${h2.version}</version>`（第 81 行）。h2 的 scope 约定由各子模块在模块级 pom.xml 自行声明（仅 application 模块使用 runtime scope）。

### OP-03（T7，与 v1 一致）：删除 ai-impl POM 中冗余 common 依赖

**文件**：`AIMedical/backend/modules/ai/ai-impl/pom.xml`

**删除范围**：第 17-20 行，即：

```xml
<dependency>
    <groupId>com.aimedical</groupId>
    <artifactId>common</artifactId>
</dependency>
```

**理由**：ai-impl Java 源码无 `com.aimedical.common.*` 导入。common 已通过 ai-api 传递获得。

**删除后预期**：ai-impl/pom.xml `<dependencies>` 内剩余 3 个条目：ai-api、spring-boot-starter、spring-boot-starter-test（test scope）。

### OP-04（审查修订新增）：标记 ParentPomVersionTest 基线测试为废弃

**文件**：`AIMedical/backend/common/src/test/java/com/aimedical/common/pom/ParentPomVersionTest.java`

**操作**：
1. 在类级别添加 `@Disabled` 注解
2. 新增 import：`import org.junit.jupiter.api.Disabled;`
3. 添加注释：`// @Disabled: 变更前基线测试。5 个 Spring Boot Starter 条目已从父 POM dependencyManagement 移除，本测试的 5 条 XPath 断言不再适用。保留文件以作历史参考。`

**理由**：该测试的 5 条 XPath 断言均验证 Starter 条目在 dependencyManagement 中存在且含 version。方案 A 完全移除这些条目后该测试必然全量失败。作为变更前基线测试不再有效，标记为废弃而非删除以保留历史参考。

## 错误处理

无新增错误处理逻辑。操作为纯 XML 行删除 + 单注解添加，不涉及运行时代码变更。验证手段为 `mvn compile` 及专用单元测试。

## 行为契约

1. **操作顺序**：无依赖关系，四个操作可任意顺序执行
2. **原子性**：建议单次 PR 提交，保持变更集原子性
3. **验证标准**：
   - `mvn compile` — 所有 10 个子模块依赖解析正常，无 missing version 错误
   - `mvn test -pl common -Dtest=ParentPomDependencyManagementCleanupTest` — 6 项测试全部通过（5 个 starter 不在 dependencyManagement 中 + h2 无 scope）
   - `mvn test -pl ai-impl -Dtest=AiImplPomCleanDependencyTest` — 5 项测试全部通过（common 已移除、条目数正确）
   - `ParentPomVersionTest` 因 `@Disabled` 不再纳入 surefire 测试套件
4. **回滚**：若验证失败，通过 `git checkout -- <file>` 恢复原始文件

## 依赖关系

- 受影响的文件：
  - `AIMedical/backend/pom.xml`（OP-01 + OP-02）
  - `AIMedical/backend/modules/ai/ai-impl/pom.xml`（OP-03）
  - `AIMedical/backend/common/src/test/java/com/aimedical/common/pom/ParentPomVersionTest.java`（OP-04）
- 无新增外部依赖
- 无运行时代码变更

## 修订说明（v2 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| R1：v1 设计仅删除 Starter 的 version 标签，但 Maven 子 POM dependencyManagement 条目会覆盖父 BOM 中相同条目的版本管理，删除 version 后条目变为无版本状态，导致 10 个子模块编译失败 | 采用方案 A：完全移除 5 个 Starter 条目（整个 dependency 块），不再覆盖 spring-boot-starter-parent BOM 的版本管理 |
| R1 验证需新增测试：ParentPomVersionTest 的 5 条 XPath 断言将因 Starter 条目被移除而全部失败 | 新增 OP-04：在 ParentPomVersionTest 类级别添加 `@Disabled` 注解，标记该测试为变更前基线（废弃），保留文件以作历史参考 |
| v1 验证方式中 mvn 命令格式错误（Dtest 参数未加引号等） | 设计阶段只关注设计规格；验证命令格式由编码/验证阶段处理 |
