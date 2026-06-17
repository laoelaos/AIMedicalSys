# 测试报告（v1）

## 行为契约覆盖矩阵

| 行为契约 | 覆盖状态 | 对应测试文件 | 说明 |
|---------|---------|-------------|------|
| OP-01：父 POM 删除 5 个 Starter 的 version（→修正方案A：完全移除条目） | COVERED | `ParentPomDependencyManagementCleanupTest.java` | 5 条断言分别验证每个 Starter 不再存在于 `<dependencyManagement>` 中 |
| OP-02：父 POM 删除 h2 的 scope | COVERED | `ParentPomDependencyManagementCleanupTest.java` | 断言 h2 条目下无 `<scope>` 子元素 |
| OP-03：ai-impl 移除冗余 common 依赖 | COVERED | `AiImplPomCleanDependencyTest.java` | 断言 common 不在 ai-impl 的 `<dependencies>` 中；同时验证剩余 3 个依赖正确 |
| §行为契约-2：验证 `mvn compile` + `mvn dependency:tree` | NOT_COVERED（设计偏差阻塞） | — | 参见下方设计偏差说明 |

## 测试文件清单

| 文件路径 | 被测类型 | 维度 |
|---------|---------|------|
| `AIMedical/backend/common/src/test/java/com/aimedical/common/pom/ParentPomDependencyManagementCleanupTest.java` | 父 POM `pom.xml`（dependencyManagement 节） | 正常路径：确认删除后 dependencyManagement 中不再包含 Spring Boot Starter 条目 |
| `AIMedical/backend/common/src/test/java/com/aimedical/common/pom/ParentPomVersionTest.java` | 父 POM `pom.xml`（dependencyManagement 节） | 当前状态基线：确认当前 POM 中 Starter 条目仍含版本号 |
| `AIMedical/backend/modules/ai/ai-impl/src/test/java/com/aimedical/modules/ai/impl/pom/AiImplPomCleanDependencyTest.java` | ai-impl `pom.xml`（dependencies 节） | 正常路径 + 边界条件：确认 common 已移除、总条目数正确 |

## 设计偏差说明

实现报告（code_v1.md）指出 OP-01 的设计假设不成立：子 POM 的 `<dependencyManagement>` 条目会覆盖父 BOM 的版本管理，仅删除 `<version>` 会导致 10 个子模块编译失败。

**修正方案 A（推荐）**：将 5 个 Spring Boot Starter 条目**完全移出** `<dependencyManagement>`，由 `spring-boot-starter-parent:3.2.5` 的 BOM 统一管理版本。

`ParentPomDependencyManagementCleanupTest.java` 已按修正方案 A 编写（断言 Starter 条目整体不存在），而非原设计（仅断言无 version 子元素）。该测试在`pom.xml`被实际修改前会失败，属于预期行为。

## 未覆盖项及理由

- **`mvn compile` 编译验证**：属于构建生命周期验证（CI 阶段），不适合在 JUnit 单元测试中通过 ProcessBuilder 调用。建议在 CI pipeline 或独立验证脚本中处理。
- **`mvn dependency:tree` 版本解析验证**：同属构建级验证。POM 结构单元测试已覆盖间接证据——Starter 从 dependencyManagement 移除后，子模块的版本将自动继承自 Spring Boot BOM（前提是设计方确认采用方案 A）。

## 测试运行条件

| 测试文件 | 当前能否通过 | 所需前置条件 |
|---------|------------|-------------|
| `ParentPomDependencyManagementCleanupTest` | ❌ | 设计修正方案 A 实现后（5 个 Starter 条目从 dependencyManagement 移除） |
| `ParentPomVersionTest` | ✅ | 无（验证当前状态，可用作变更前基线） |
| `AiImplPomCleanDependencyTest` | ❌ | ai-impl/pom.xml 中 `com.aimedical:common` 依赖被删除后 |
