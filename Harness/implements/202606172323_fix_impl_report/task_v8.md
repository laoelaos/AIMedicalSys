# 任务指令（v8）

## 动作
RETRY

## 任务描述
修复 R6 验证失败的 5 项测试问题，含 3 个文件修改：

1. **BaseEntityAuditTest.java** — 修复 `@DataJpaTest` 无法找到 `@SpringBootConfiguration` 的错误
2. **CommonPomTest.java** — 更新因 R4/R6 新增依赖而失效的 2 条断言
3. **ParentPomTest.java** — 更新因 R1 删除 dependencyManagement starter 块而失效的 2 条断言

## 选择理由
全部 5 项失败均集中在 common 模块的测试基础设施，一次 RETRY 统一修复效率最高。CommonPomTest/ParentPomTest 的 4 项失败为前序任务的回归（R1 删除 starter、R4 新增 validation、R6 新增 h2），在此轮一并处理。

## 任务上下文
### 文件路径
- `AIMedical/backend/common/src/test/java/com/aimedical/common/base/BaseEntityAuditTest.java`
- `AIMedical/backend/common/src/test/java/com/aimedical/common/pom/CommonPomTest.java`
- `AIMedical/backend/common/src/test/java/com/aimedical/common/pom/ParentPomTest.java`

### 当前 common/pom.xml 依赖（共 5 项）：
1. `spring-boot-starter-web` (optional)
2. `spring-boot-starter-data-jpa` (optional)
3. `spring-boot-starter-validation` (optional) — R4 新增
4. `spring-boot-starter-test` (test)
5. `h2` (test) — R6 新增

### 当前 backend/pom.xml dependencyManagement：
- Spring Boot Starter 条目全部移除（R1），仅保留内部模块（common/common-module-api/common-module-impl/ai-api/ai-impl/application）和外部库（springdoc/h2）

## 已有代码上下文
### BaseEntityAuditTest.java（当前内容）
```java
@ExtendWith(SpringExtension.class)
@DataJpaTest
@Import(JpaConfig.class)
class BaseEntityAuditTest {
    @Autowired private TestEntityManager em;

    @Entity @Table(name = "test_audit_entity")
    static class AuditTestEntity extends BaseEntity {}

    @Test void shouldAutoFillCreatedAtOnPersist() { ... }
    @Test void shouldUpdateUpdatedAtOnUpdate() { ... }
}
```
问题：`@DataJpaTest` 使用 `SpringBootTestContextBootstrapper` 需 `@SpringBootConfiguration`，common 为库模块无入口类。

### CommonPomTest.java 失败断言
```java
// 第59-61行
void dependencyCountShouldBeExactlyThree() {
    assertEquals(3, ...);  // 当前实际为5
}
// 第38-41行
void shouldNotContainValidationStarter() {
    assertFalse(exists("...validation..."));  // validation starter已在R4有意新增
}
```

### ParentPomTest.java 失败断言
```java
// 第38-46行
void dependencyManagementShouldContainAllSpringBootStarters() {
    assertTrue(exists("...starter-web..."));    // 已在R1删除
    assertTrue(exists("...starter-data-jpa...")); // 已在R1删除
    assertTrue(exists("...starter-security...")); // 已在R1删除
    assertTrue(exists("...starter-validation...")); // 已在R1删除
    assertTrue(exists("...starter-test..."));    // 已在R1删除
}
// 第48-51行
void testStarterShouldHaveTestScope() {
    assertEquals("test", xpath("...starter-test.../scope")); // starter已在R1删除
}
```

## RETRY 说明
### 失败原因
- BaseEntityAuditTest: `@DataJpaTest` 内部使用 `SpringBootTestContextBootstrapper`，要求被测模块存在 `@SpringBootConfiguration` 类；common 为纯库模块无此注解
- CommonPomTest: 2 条测试断言未随 R4（新增 validation starter）和 R6（新增 h2 test scope）更新
- ParentPomTest: 2 条测试断言未随 R1（删除 dependencyManagement 中 5 个 starter 块）更新

### 修正方向
1. **BaseEntityAuditTest.java**：
   - 移除 `@ExtendWith(SpringExtension.class)`（`@DataJpaTest` 已包含该元注解）
   - 在类内添加 `@SpringBootApplication` 静态内部配置类，为 `SpringBootTestContextBootstrapper` 提供 `@SpringBootConfiguration`

2. **CommonPomTest.java**：
   - `dependencyCountShouldBeExactlyThree()`: 断言值 3 → 5
   - `shouldNotContainValidationStarter()`: 移除整条测试方法（validation starter 为 R4 有意新增，与 T1 需求一致）

3. **ParentPomTest.java**：
   - `dependencyManagementShouldContainAllSpringBootStarters()`: 移除 5 条 Starter 断言行（第 41-45 行），保留同一方法中 businessModules/ignoredUnusedDeclaredDependencies 等其他断言
   - `testStarterShouldHaveTestScope()`: 移除整条测试方法（dependencyManagement 中 test starter 已于 R1 删除）
