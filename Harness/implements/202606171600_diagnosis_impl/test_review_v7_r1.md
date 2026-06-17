# 测试审查报告（v7 r1）

## 审查结果
REJECTED

## 发现
- **[严重]** `AIMedical/backend/common/src/test/java/com/aimedical/common/pom/MovedModulePomTest.java:130` — 模块数量断言值错误：`assertEquals(11, ...)` 断言根 POM 应有 11 个 `<module>` 元素，但实际只有 10 个。该测试必然失败。

- **[轻微]** `AggregatorPomTest.java` 和 `MovedModulePomTest.java` 中所有 parent 断言 — 仅验证了 parent `artifactId` 和 `relativePath`，未验证 parent `groupId` 和 `version`。虽然 `mvn validate` 构建期可捕获这类错误，但单元测试层面存在覆盖缺口。

- **[轻微]** `AggregatorPomTest.java` — 设计规定聚合 POM "不含 dependencyManagement 或 dependencies"，测试未对此进行验证。

## 修改要求
### 严重问题

**`MovedModulePomTest.java:130`** — 模块计数断言错误

- 位置：`rootPomShouldHaveExactlyElevenModules()` 方法，第 130 行
- 问题：`assertEquals(11, rootPom.getDocumentElement().getElementsByTagName("module").getLength())`
- 原因：根 POM `backend/pom.xml` 的 `<modules>` 节只有 10 个 `<module>` 条目（common, modules/common-module/common-module-api, modules/common-module/common-module-impl, modules/ai/ai-api, modules/ai/ai-impl, modules/patient, modules/doctor, modules/admin, application, integration），不符合 11 的断言。该测试若执行必然失败。
- 修正方向：将 11 改为 10，或移除该冗余断言（`rootPomModulesShouldUseLayeredPaths` 已逐项验证了全部 10 个模块路径）。
