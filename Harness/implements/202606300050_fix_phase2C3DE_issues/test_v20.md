# 测试报告（v20）

## 概述
对 `ai-impl/pom.xml` 追加 `spring-boot-starter-web` 依赖变更的验证。该变更修复因缺少 Spring MVC 注解导致的 15 个编译错误（`MockAdminController.java` 中 `@RestController`、`@RequestMapping`、`@GetMapping`、`@PostMapping`、`@RequestBody`、`ResponseEntity` 等无法解析）。

## 测试策略

| 维度 | 方式 | 说明 |
|------|------|------|
| **依赖存在性** | `AiImplPomCleanDependencyTest.shouldContainSpringBootStarterWeb()` | XML DOM 解析验证 POM 中已声明该依赖 |
| **编译验证** | `mvn compile -f ai/pom.xml -pl ai-impl -am` | 确保 classpath 包含 spring-boot-starter-web 传递依赖后编译通过 |
| **MockAdminController 编译** | `MockAdminControllerTest` | 该测试的编译成功隐式验证了 Spring MVC 注解可被解析 |

## 测试文件清单

### 1. AiImplPomCleanDependencyTest
**路径**: `ai-impl/src/test/java/.../pom/AiImplPomCleanDependencyTest.java`
**操作**: 已有（无需修改）
**覆盖维度**:
- `shouldContainSpringBootStarterWeb()` — 直接验证 POM `<dependencies>` 中包含 `groupId=org.springframework.boot, artifactId=spring-boot-starter-web`
- 已有全部 6 个用例不受影响（依赖顺序、个数、其他依赖声明不变性）

### 2. MockAdminControllerTest
**路径**: `ai-impl/src/test/java/.../mock/MockAdminControllerTest.java`
**操作**: 已有（无需修改）
**覆盖维度**:
- `getStrategyShouldReturnStaticByDefault()` — 依赖 `@RestController` 注解编译通过
- `setStrategyAndVerify()` — 依赖 `@PostMapping`/`@RequestBody` 编译通过
- `setStrategyToTimeout()` — 同上

## 行为契约覆盖矩阵

| 契约 | 覆盖测试 | 验证方式 | 状态 |
|------|---------|---------|------|
| 前置条件：pom.xml 存在 `<dependencies>` | AiImplPomCleanDependencyTest（6 个用例通过 XML DOM 访问） | XPath 解析 | ✓ |
| 后置条件：spring-boot-starter-web 在 dependencies 中声明 | `shouldContainSpringBootStarterWeb()` | XPath boolean 断言 | ✓ |
| 后置条件：Spring MVC 注解可正常解析编译 | MockAdminControllerTest（3 个用例编译） | `mvn test-compile` 零错误 | ✓ |
| 不变量：其他依赖不变 | `shouldContainAiApiDependency()`、`shouldContainSpringBootStarter()`、`shouldContainTestStarterWithTestScope()`、`totalDependenciesCountShouldBeFour()`、`shouldNotContainRedundantCommonDependency()` | 全部断言通过 | ✓ |

## 未覆盖说明
- 无。本变更为 POM 单行依赖追加，全部契约已被上述测试覆盖。
