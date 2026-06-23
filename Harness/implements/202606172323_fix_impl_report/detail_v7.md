# 详细设计（v7）

## 概述

在 common 模块中新增 H2 test 依赖及 `@DataJpaTest` 测试，验证 `BaseEntity` 的 `@CreatedDate` / `@LastModifiedDate` 审计注解在 Spring Data JPA 上下文中自动填充行为。不修改任何生产代码，不影响现有纯 JUnit5 测试。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/common/pom.xml` | 修改 | 新增 `com.h2database:h2` test scope 依赖 |
| `AIMedical/backend/common/src/test/java/com/aimedical/common/base/BaseEntityAuditTest.java` | 新建 | `@DataJpaTest` + `@Import(JpaConfig.class)` 验证审计自动填充 |
| `AIMedical/backend/common/src/test/java/com/aimedical/common/base/BaseEntityTest.java` | 不变 | 现有 4 个纯 JUnit5 POJO 测试保持不动 |

## 类型定义

### BaseEntityAuditTest（新增）

**形态**：class（JUnit5 + Spring Data JPA 切片测试）
**包路径**：`com.aimedical.common.base`
**职责**：在完整 JPA 容器上下文中验证 `@CreatedDate` 和 `@LastModifiedDate` 审计监听器自动填充 `BaseEntity.createdAt` / `BaseEntity.updatedAt`

**类签名**：

```java
@ExtendWith(SpringExtension.class)
@DataJpaTest
@Import(JpaConfig.class)
class BaseEntityAuditTest {
    @Autowired
    private TestEntityManager em;

    // ── 测试实体 ──
    // 包私有静态内部类，仅用于测试 JPA 生命周期回调
    @Entity
    @Table(name = "test_audit_entity")
    static class AuditTestEntity extends BaseEntity {
    }

    // ── 测试方法 ──
    @Test
    void shouldAutoFillCreatedAtOnPersist();
    @Test
    void shouldUpdateUpdatedAtOnUpdate();
}
```

**公开接口**：
- `shouldAutoFillCreatedAtOnPersist()` — persist 后验证 `createdAt` 和 `updatedAt` 均不为 null
- `shouldUpdateUpdatedAtOnUpdate()` — persist 后更新实体再 flush，验证 `createdAt` 不变、`updatedAt` 非 null

**构造方式**：由 JUnit5 + Spring TestContext 自动实例化，无手动构造。

**类型关系**：
- 依赖 `JpaConfig`（通过 `@Import` 加载 `@EnableJpaAuditing` 配置）
- 间接依赖 `BaseEntity`（被 `AuditTestEntity` 继承）
- `AuditTestEntity` 为包私有静态内部类，仅本测试类可见

## 错误处理

- 测试失败时抛出 `AssertionError`，由 JUnit5 框架自动捕获报告
- 无自定义异常类型
- `@DataJpaTest` 默认回滚事务，测试方法之间无数据库状态污染

## 行为契约

### 测试方法契约

| 方法 | 前置 | 操作 | 后置断言 |
|------|------|------|---------|
| `shouldAutoFillCreatedAtOnPersist` | 无 | `em.persistAndFlush(entity)` | `entity.createdAt != null && entity.updatedAt != null` |
| `shouldUpdateUpdatedAtOnUpdate` | persist 后记录 `initialCreatedAt`、`initialUpdatedAt` | `entity.setDeleted(true); em.persistAndFlush(entity)` | `entity.createdAt == initialCreatedAt && entity.updatedAt != null` |

### 审计字段行为

1. `@CreatedDate`：实体首次 `persist` 前由 `AuditingEntityListener` 设置，后续 `merge` 不再更新
2. `@LastModifiedDate`：实体每次 `persist`/`merge` 时由 `AuditingEntityListener` 设置
3. `AuditTestEntity` 继承 `BaseEntity` 的全部字段和注解，`@EntityListeners` 继承生效

### JPA 配置

- `@DataJpaTest` 扫描 `com.aimedical.common.base` 包中的 `@Entity` 类（`AuditTestEntity`），自动建表
- `@Import(JpaConfig.class)` 显式导入 `@EnableJpaAuditing`，确保 `AuditingEntityListener` 被注册为 Spring Bean
- 测试结束后事务自动回滚，不残留数据

## 依赖关系

### 新增 Maven 依赖（`common/pom.xml`）

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

- 版本由父 POM `<h2.version>`（2.2.224）统一管理，`dependencyManagement` 中已声明版本号
- 位置：追加在 `spring-boot-starter-test` 依赖之后（`</dependency>` 之后）
- `@DataJpaTest` 自动配置要求嵌入式数据库驱动在 classpath 中，H2 是缺省首选

### 已有依赖不变

- `spring-boot-starter-data-jpa`（compile optional）— 提供 JPA 注解和 `AuditingEntityListener`
- `spring-boot-starter-test`（test）— 提供 JUnit5 + Spring TestContext + `@DataJpaTest`
- `parent POM` 的 `dependencyManagement` 已管理 `com.h2database:h2:2.2.224`

### 暴露给后续任务

- 无。本任务不新增公开 API，也不修改公共类型。

## 与现有 BaseEntityTest 的隔离

| 维度 | BaseEntityTest | BaseEntityAuditTest |
|------|---------------|-------------------|
| 测试框架 | 纯 JUnit5（无 Spring） | `@DataJpaTest`（Spring 容器） |
| 实体类 | `TestEntity`（无 `@Entity`） | `AuditTestEntity`（有 `@Entity`） |
| 验证目标 | POJO setter/getter 默认值 | JPA 生命周期回调自动填充 |
| 运行方式 | `mvn test` 各自独立运行 | `mvn test` 各自独立运行 |
| 配置冲突 | 无 | 无（`@DataJpaTest` 创建独立应用上下文） |
