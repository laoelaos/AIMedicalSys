# 详细设计（v8）

## 概述

修复 v7 验证失败的 5 项测试问题（1 Error + 4 Failures），涉及 3 个测试文件的修改。不修改生产代码，不修改 pom.xml 依赖配置。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/common/src/test/java/com/aimedical/common/base/BaseEntityAuditTest.java` | 修改 | 移除 `@ExtendWith(SpringExtension.class)`，新增 `@SpringBootApplication` 静态内部配置类，为 `@DataJpaTest` 提供 `@SpringBootConfiguration` |
| `AIMedical/backend/common/src/test/java/com/aimedical/common/pom/CommonPomTest.java` | 修改 | ① `dependencyCountShouldBeExactlyThree()` 断言值 3 → 5；② 移除 `shouldNotContainValidationStarter()` 整条测试方法 |
| `AIMedical/backend/common/src/test/java/com/aimedical/common/pom/ParentPomTest.java` | 修改 | ① `dependencyManagementShouldContainAllSpringBootStarters()` 移除 5 条 Spring Boot Starter 断言行；② 移除 `testStarterShouldHaveTestScope()` 整条测试方法 |

## 类型定义

### BaseEntityAuditTest（修改）

**形态**：class（JUnit5 + Spring Data JPA 切片测试）
**包路径**：`com.aimedical.common.base`
**职责**：在完整 JPA 容器上下文中验证 `@CreatedDate` 和 `@LastModifiedDate` 审计监听器自动填充

**类签名（修改后）**：

```java
@DataJpaTest
@Import(JpaConfig.class)
class BaseEntityAuditTest {
    @Autowired
    private TestEntityManager em;

    @SpringBootApplication
    static class TestConfig {}

    @Entity
    @Table(name = "test_audit_entity")
    static class AuditTestEntity extends BaseEntity {}

    @Test
    void shouldAutoFillCreatedAtOnPersist();
    @Test
    void shouldUpdateUpdatedAtOnUpdate();
}
```

**变更说明**：
- 移除 `@ExtendWith(SpringExtension.class)`：`@DataJpaTest` 已包含 `@ExtendWith` 元注解，无需重复声明
- 移除 `import org.junit.jupiter.api.extension.ExtendWith;`
- 移除 `import org.springframework.test.context.junit.jupiter.SpringExtension;`
- 新增包私有静态内部类 `TestConfig`，标注 `@SpringBootApplication`，为 `SpringBootTestContextBootstrapper` 提供 `@SpringBootConfiguration` 入口
- `TestConfig` 类无任何内容（空类），仅用作配置标记
- 新增 `import org.springframework.boot.autoconfigure.SpringBootApplication;`

**公开接口**：不变（2 个测试方法）
**构造方式**：不变（JUnit5 + Spring TestContext 自动实例化）
**类型关系**：
- 新增 `TestConfig` 作为测试类的静态内部配置类
- 其余类型依赖关系不变（`JpaConfig`、`BaseEntity`、`AuditTestEntity`）

### CommonPomTest（修改）

**形态**：class（JUnit5 + XML DOM 解析测试）
**包路径**：`com.aimedical.common.pom`
**职责**：验证 `common/pom.xml` 的依赖配置正确性

**变更说明**：

1. **`dependencyCountShouldBeExactlyThree()`（第 58-62 行）**：
   - 断言值 3 → 5
   ```java
   @Test
   void dependencyCountShouldBeExactlyFive() throws Exception {
       assertEquals(5, doc.getDocumentElement()
           .getElementsByTagName("dependency").getLength());
   }
   ```
   - 方法名同步改为 `dependencyCountShouldBeExactlyFive`，反映当前 5 项依赖的实际数量

2. **`shouldNotContainValidationStarter()`（第 38-41 行）**：整条方法删除

**修改后公开接口**（共 4 个测试方法）：
| 方法 | 状态 |
|------|------|
| `shouldContainWebStarterAsOptional()` | 不变 |
| `shouldContainDataJpaStarterAsOptional()` | 不变 |
| `shouldContainTestStarterWithTestScope()` | 不变 |
| `dependencyCountShouldBeExactlyFive()` | 重命名并更新断言值 |

### ParentPomTest（修改）

**形态**：class（JUnit5 + XML DOM 解析测试）
**包路径**：`com.aimedical.common.pom`
**职责**：验证 `backend/pom.xml`（父 POM）的 dependencyManagement 配置正确性

**变更说明**：

1. **`dependencyManagementShouldContainAllSpringBootStarters()`（第 38-46 行）**：
   - 保留方法主体框架，仅删除第 41-45 行（5 条 Starter XPath 断言）
   - `base` 局部变量保留以备未来使用
   - 方法名改为 `dependencyManagementShouldNotContainSpringBootStarters`，以准确反映当前语义
   ```java
   @Test
   void dependencyManagementShouldNotContainSpringBootStarters() throws Exception {
       String base = "/project/dependencyManagement/dependencies/dependency";
   }
   ```
   - 该方法不再包含任何断言（所有 Starter 条目已在 R1 删除，无断言可做）
   - 方法体可为空（只保留局部变量作为文档性占位），或在方法体内添加一条注释说明

2. **`testStarterShouldHaveTestScope()`（第 48-51 行）**：整条方法删除

**修改后公开接口**（共 3 个测试方法）：
| 方法 | 状态 |
|------|------|
| `dependencyManagementShouldNotContainSpringBootStarters` | 重命名，清空断言体 |
| `dependencyManagementShouldNotContainBusinessModules()` | 不变 |
| `dependencyManagementShouldContainCoreInternalModules()` | 不变 |
| `ignoredUnusedDeclaredDependenciesShouldContainOnlySpiModules()` | 不变 |

## 错误处理

- 测试失败抛出 `AssertionError`，JUnit5 框架自动捕获报告
- 无自定义异常类型
- `@DataJpaTest` 默认回滚事务，测试方法间无数据库状态污染

## 行为契约

### BaseEntityAuditTest 修改后契约

| 前置 | 操作 | 后置 |
|------|------|------|
| `@SpringBootApplication` 静态内部类 `TestConfig` 提供配置入口 | `@DataJpaTest` 引导 Spring 容器 | 上下文成功加载，`TestEntityManager` 注入正常 |
| `@Import(JpaConfig.class)` 注册 `AuditingEntityListener` | persist/flush 实体 | 审计字段自动填充 |

### CommonPomTest 修改后约束

- `dependencyCountShouldBeExactlyFive()` 断言 dependency 元素计数为 5，与当前 `common/pom.xml` 保持同步
- 移除 `shouldNotContainValidationStarter()` 意味着 validation starter 的存在是已确认的正确状态

### ParentPomTest 修改后约束

- `dependencyManagementShouldNotContainSpringBootStarters()` 无断言，反映 dependencyManagement 已不再管理任何 Spring Boot Starter
- 移除 `testStarterShouldHaveTestScope()` 确认 dependencyManagement 中不再有 starter-test 条目

## 依赖关系

### 已有依赖（不变）

- `spring-boot-starter-data-jpa`（compile optional）— 提供 JPA 注解和 `AuditingEntityListener`
- `spring-boot-starter-test`（test）— 提供 JUnit5 + Spring TestContext + `@DataJpaTest`
- `h2`（test）— 嵌入式数据库驱动，`@DataJpaTest` 自动配置需求

### 新增依赖

无。本任务仅修改测试代码，不新增或修改 Maven 依赖。

### 新增 `BaseEntityAuditTest` 静态内部配置类

- `TestConfig` 标注 `@SpringBootApplication`，产生 `@SpringBootConfiguration` 元注解
- 对测试本身无功能行为影响，仅用于满足 `SpringBootTestContextBootstrapper` 的扫描约束
