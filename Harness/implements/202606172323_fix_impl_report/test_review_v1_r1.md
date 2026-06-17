# 测试审查报告（v1 r1）

## 审查结果
REJECTED

## 发现

- **[严重]** `AIMedical/backend/common/src/test/java/com/aimedical/common/pom/ParentPomDependencyManagementCleanupTest.java` — 测试断言 5 个 Spring Boot Starter 的 `<version>` 从 dependencyManagement 中移除。但实现报告（code_v1.md）已证明该设计假设错误：移除版本后 `mvn compile` 失败，10 个子模块失去版本解析。测试会在构建损坏状态下通过，属于误导性测试，验证了错误的状态而非正确性。

- **[严重]** `ParentPomDependencyManagementCleanupTest.java` — 设计的行为契约（§行为契约-2）明确要求验证 `mvn compile` 成功和 `mvn dependency:tree` 版本解析正确，但测试未覆盖任何编译或依赖解析验证，仅检查 XML 结构。

- **[严重]** 两个测试文件 — 实现报告（code_v1.md）确认所有 POM 变更已回退，且当前 git 工作树中 `pom.xml` 未修改。测试断言的目标状态在代码库中不存在；运行测试会失败。

- **[一般]** `ParentPomDependencyManagementCleanupTest.java:36,41,46,51,58` — 使用 `xpath.evaluate(expr, doc)` 返回的 String 做 `isEmpty()` 判断，无法区分"元素不存在"和"元素存在但值为空"。应使用 `xpath.evaluate(expr, doc, XPathConstants.BOOLEAN)` 做精确的存在性判断（如 ai-impl 测试的做法）。

- **[一般]** `ParentPomDependencyManagementCleanupTest.java:25` — 使用相对路径 `../pom.xml`，依赖从 `common` 模块目录运行的隐式假设，跨构建环境脆弱。

- **[一般]** `AiImplPomCleanDependencyTest.java:56` — `getElementsByTagName("dependency")` 统计文档中所有 `<dependency>` 标签而非仅 `<dependencies>` 下的直接子元素。若 POM 含有 `<dependencyManagement>` 或其他含 `<dependency>` 的节，计数会不正确。

## 修改要求

1. **ParentPomDependencyManagementCleanupTest.java**：不应对当前设计尚不成立的状态编写测试。该设计本身有缺陷（版本移除导致编译失败），需等待设计修正（如方案 A：完全移除 dependencyManagement 条目而非仅移除版本号）。测试应在正确设计确定后重写。

2. **两个测试文件**：增加 `mvn compile` 编译验证断言（可通过 Maven Embedder 或 ProcessBuilder 调用），确保编译通过是测试的前提条件而非仅 XML 结构检查。

3. **ParentPomDependencyManagementCleanupTest.java**：XPath 查询改用 `XPathConstants.BOOLEAN` 做存在性判断，避免 `isEmpty()` 的语义歧义。

4. **ParentPomDependencyManagementCleanupTest.java**：POM 路径改用 Maven 项目基址属性或绝对路径注入，消除相对路径脆弱性。

5. **AiImplPomCleanDependencyTest.java**：改用 XPath 精确计数 `count(/project/dependencies/dependency)` 替代 `getElementsByTagName`。
