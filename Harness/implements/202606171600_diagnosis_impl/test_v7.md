# 测试报告（v7 r1）

## 测试文件

| 文件路径 | 职责 |
|---------|------|
| `AIMedical/backend/common/src/test/java/com/aimedical/common/pom/AggregatorPomTest.java` | 验证新增聚合 POM（common-module, ai）的 parent 引用、packaging、modules 声明，以及无 dependencyManagement 和 dependencies |
| `AIMedical/backend/common/src/test/java/com/aimedical/common/pom/MovedModulePomTest.java` | 验证移动后子模块 POM 的 parent 引用（含 groupId/version）、未移动模块的 relativePath 不变性、根 POM modules 分层路径及数量 |

## 行为契约覆盖

### 目录移动契约
- ✅ 聚合 POM parent groupId/artifactId/version/relativePath 全部正确（com.aimedical, aimedical-sys, 0.0.1-SNAPSHOT, ../../pom.xml）
- ✅ 移动后子模块 parent 引用（groupId/artifactId/version）正确（common-module-* → common-module，ai-* → ai）
- ✅ 移动后 patient/doctor/admin relativePath = ../../pom.xml
- ✅ application/common/integration 相对路径不变（../pom.xml）
- ✅ application/common/integration parent artifactId 保持 aimedical-sys

### module 引用契约
- ✅ 根 POM modules 使用相对于 backend/ 的分层路径（10 个 module）
- ✅ 聚合 POM modules 使用相对于聚合目录的路径

### 验证契约
- 验证契约 1-3 属 Maven 构建期验证（mvn validate / compile），不在单元测试范围内

## 测试方法
- 与项目已有 POM 测试风格一致：JUnit 5 + XML DOM 解析 + XPath 断言
- 每个行为契约对应至少一个独立 @Test 方法
- 测试文件位于 `common` 模块 `com.aimedical.common.pom` 包，与 `ParentPomTest`、`ApplicationPomTest` 等同目录

## 修订说明（v7 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| **[严重]** MovedModulePomTest:130 模块计数断言 `assertEquals(11, ...)` 错误（实际只有 10 个） | 改为 `assertEquals(10, ...)`，方法名同步改为 `rootPomShouldHaveExactlyTenModules` |
| **[轻微]** 所有 parent 断言未验证 groupId 和 version | 全局补充 parent groupId="com.aimedical" 和 version="0.0.1-SNAPSHOT" 断言，覆盖聚合 POM、移动后子模块、patient/doctor/admin、application/common/integration |
| **[轻微]** 聚合 POM 未验证无 dependencyManagement 或 dependencies | 新增 4 个 @Test：`commonModuleShouldHaveNoDependencyManagement`、`commonModuleShouldHaveNoDependencies`、`aiShouldHaveNoDependencyManagement`、`aiShouldHaveNoDependencies` |
