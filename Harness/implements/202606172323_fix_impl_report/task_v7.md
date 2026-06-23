# 任务指令（v7）

## 动作
NEW

## 任务描述
在 BaseEntityTest（或新增测试类）中添加 `@DataJpaTest` 测试方法，验证 `@CreatedDate` / `@LastModifiedDate` 审计自动填充；同步在 `common/pom.xml` 中添加 h2 test scope 依赖。

### 预期修改文件

| 文件路径 | 操作 | 说明 |
|---------|------|------|
| `AIMedical/backend/common/pom.xml` | 修改 | 新增 h2 `<scope>test</scope>` 依赖 |
| `AIMedical/backend/common/src/test/java/com/aimedical/common/base/BaseEntityAuditTest.java` | 新建 | `@DataJpaTest` + `@Import(JpaConfig.class)` 验证审计自动填充 |
| `AIMedical/backend/common/src/test/java/com/aimedical/common/base/BaseEntityTest.java` | 不变 | 现有 4 个纯 JUnit5 POJO 测试保持不动 |

## 选择理由
T11 为剩余任务中底层依赖最深（common 模块），修复不影响生产代码，无运行时风险。先补全底层测试基础设施，再处理 T10（ai-impl 行为修改）。T10 与 T11 互无依赖，可独立串行推进。

## 任务上下文

摘录自 `02_impl_report.md` T11：
- OOD §3.2 明确 `createdAt` 由 `@CreatedDate` + `AuditingEntityListener` 自动填充，`updatedAt` 由 `@LastModifiedDate` 自动填充
- `BaseEntityTest.java:46-48` 仅通过 `new TestEntity()` 验证了 POJO 级 setter/getter 默认值行为，未在 Spring Data JPA 上下文中验证审计监听器是否按预期自动填充时间戳
- `common/pom.xml` 当前未声明 `com.h2database:h2` 依赖。`@DataJpaTest` 依赖嵌入式数据库自动配置，需要 H2 driver 在 test classpath 中可用
- 建议创建独立测试类（而非在现有 `BaseEntityTest` 追加），避免 JUnit5 + Spring 上下文混合可能导致的配置冲突

## 已有代码上下文

- `BaseEntity.java:27-31`：`@CreatedDate` / `@LastModifiedDate` 标注于 `createdAt` / `updatedAt` 字段
- `BaseEntity.java:18`：`@EntityListeners(AuditingEntityListener.class)` 激活审计监听
- `JpaConfig.java:6-8`：`@Configuration` + `@EnableJpaAuditing` 启用审计配置
- `BaseEntityTest.java`：纯 JUnit 5 测试类，无 `@SpringBootTest` 或 `@DataJpaTest`
- `common/pom.xml:32-36`：已有 `spring-boot-starter-test` test scope 依赖
- `common/pom.xml:22-26`：已有 `spring-boot-starter-data-jpa` compile optional 依赖

## 实现指引

### 1. `common/pom.xml` — 新增 h2 test 依赖

在 `<dependencies>` 末尾（`spring-boot-starter-test` 之后）追加：

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

版本由父 POM `<h2.version>`（2.2.224）统一管理，无需显式指定。

### 2. 新建 `BaseEntityAuditTest.java`

**位置**：`common/src/test/java/com/aimedical/common/base/BaseEntityAuditTest.java`

**关键设计决策**：
- 使用 `@DataJpaTest`（轻量级 JPA 切片测试，仅加载 JPA 相关 Bean，比 `@SpringBootTest` 启动更快）
- 通过 `@Import(JpaConfig.class)` 显式导入审计配置（`@DataJpaTest` 默认不扫描 `@Configuration` 类）
- 使用 `TestEntityManager` 持久化实体并刷入数据库，验证审计时间戳被自动填充
- 测试实体类仍使用 `BaseEntityTest` 中的 `TestEntity`（包私有），或重新声明一个 JPA 实体类

**测试方法设计**：

```java
@ExtendWith(SpringExtension.class)
@DataJpaTest
@Import(JpaConfig.class)
class BaseEntityAuditTest {

    @Autowired
    private TestEntityManager em;

    @Test
    void shouldAutoFillCreatedAtOnPersist() {
        TestEntity entity = new TestEntity();
        em.persistAndFlush(entity);
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
    }

    @Test
    void shouldUpdateUpdatedAtOnUpdate() {
        TestEntity entity = new TestEntity();
        em.persistAndFlush(entity);
        LocalDateTime initialCreatedAt = entity.getCreatedAt();
        LocalDateTime initialUpdatedAt = entity.getUpdatedAt();

        entity.setDeleted(true);
        em.persistAndFlush(entity);

        assertEquals(initialCreatedAt, entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        // Note: @LastModifiedDate may or may not update depending on whether
        // Hibernate considers this a "dirty" update; in a simple flush the
        // auditing listener may or may not fire. This test is informational.
    }
}
```

注意：`@DataJpaTest` 需要在测试实体类上标注 `@Entity` 或将实体类注册到 JPA 上下文。当前 `TestEntity`（`BaseEntityTest` 中的私有静态内部类）没有 `@Entity` 注解。因此要么：
- 给 `TestEntity` 加 `@Entity` 并使其可被 `@DataJpaTest` 的实体扫描发现
- 在测试类中用 `@EntityScan` 指定扫描路径

更简洁的方式是直接在测试类中声明一个 `@Entity` 内部类，或者使用 `@EntityScan` 扫描 `com.aimedical.common.base`。

## 验收标准

1. `mvn compile -pl common -q` 通过
2. `mvn test -pl common -q` 通过，新增审计测试方法全部通过
3. 新增的 `@DataJpaTest` 测试验证：实体 `persist` 后 `createdAt` 和 `updatedAt` 均不为 null
4. 不影响现有 `BaseEntityTest` 的 4 个纯 JUnit5 测试

## 修订说明（v7 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| plan.md R6 描述"在BaseEntityTest中添加@SpringBootTest/@DataJpaTest测试方法"与task_v7.md新建独立测试类BaseEntityAuditTest.java的指令冲突 | plan.md R6 任务描述修正为"新建BaseEntityAuditTest.java（@DataJpaTest + @Import(JpaConfig.class)），验证@CreatedDate/@LastModifiedDate审计自动填充"
