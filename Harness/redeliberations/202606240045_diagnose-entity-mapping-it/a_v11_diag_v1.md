# 诊断报告：EntityMappingIT 实体映射集成测试问题

## 1. EntityMappingIT 缺少 User/Role/Post 集成测试

### 现象

`EntityMappingIT.java`（`AIMedical\backend\integration\src\test\java\com\aimedical\integration\EntityMappingIT.java`）包含了多组 JPA 实体映射验证测试（AllergyHistory、HealthProfile、PatientEntity、DoctorEntity、Function、DictData/DictType、TokenStore），但完全没有覆盖 Phase 1 包A 的核心实体 `User`、`Role`、`Post`。

### 根因

`User.java`（`common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/User.java`）、`Role.java`（同包）、`Post.java`（同包）均已存在，且 `integration/pom.xml` 第53-56行已声明 `common-module-impl` 为 test 依赖，因此在集成测试模块中完全可访问。

此问题是**测试遗漏**，不是代码问题。测试类在编写时只覆盖了此前报告过有映射问题的实体，未将 Phase 1 核心实体纳入。

### 需验证的映射点

| 编号 | 实体 | 需验证的映射点 | 关键代码位置 | 备注 |
|------|------|---------------|-------------|------|
| M1 | User | `username` 字段映射（含 `unique=true` 注解声明） | User.java:25, schema.sql:27 (UNIQUE KEY `uk_username`) | 仅验证基本字段映射，约束强制力验证需另加测试 |
| M2 | User | `password` NOT NULL 约束 | User.java:28, schema.sql:16 | 关联 Issue 2 |
| M3 | User | `userType` 枚举 `@Enumerated(EnumType.STRING)` → VARCHAR(20) NOT NULL | User.java:38-40, schema.sql:20 | |
| M4 | User | `roles` `@ManyToMany` 关联表 `user_role` | User.java:42-46, schema.sql:145-151 | |
| M5 | User | `posts` `@ManyToMany` 关联表 `user_post` | User.java:48-52, schema.sql:157-163 | |
| M6 | User | `enabled` 无 Java 默认值（NULL 可写入） | User.java:36, schema.sql:21 (DEFAULT 1) | |
| M7 | Role | `code` 唯一约束 | Role.java:21-22, schema.sql:46 (UNIQUE KEY `uk_code`) | role_shouldMapCodeField（字段映射）+ role_shouldEnforceCodeUniqueConstraint（唯一约束验证） |
| M8 | Role | `enabled` 无 Java 默认值 | Role.java:28, schema.sql:40 (DEFAULT 1) | 已在 Role 测试中设置并断言 |
| M9 | Role | `posts` `@OneToMany(mappedBy = "role")` | Role.java:33-34, schema.sql:59 (role_id FK) | role_shouldMapOneToManyPosts |
| M10 | Role | `users` `@ManyToMany(mappedBy = "roles")` | Role.java:36-37, user_role 关联表 | 未覆盖 |
| M11 | Post | `code` 字段映射（含 `unique=true` 注解声明） | Post.java:23-24, schema.sql:67 (UNIQUE KEY `uk_code`) | 仅验证基本字段映射，约束强制力验证需另加测试（与 M1/M7 模式对齐） |
| M12 | Post | `enabled` 无 Java 默认值 | Post.java:30, schema.sql:60 (DEFAULT 1) | 已在 Post 测试中设置并断言 |
| M13 | Post | `role` `@ManyToOne` + `role_id` 外键 | Post.java:34-36, schema.sql:59 | post_shouldMapManyToOneRole |
| M14 | Post | `functions` `@ManyToMany` 关联表 `post_function` | Post.java:38-42, schema.sql:169-179 | post_shouldMapManyToManyFunctions |
| M15 | Post | `sort` 字段映射 | Post.java:32, schema.sql:61 (DEFAULT 0) | post_shouldMapManyToOneRole 中验证 |

实体均继承 `BaseEntity`，需验证 `id` 自增、`deleted` NOT NULL 等继承字段的映射行为。`createdAt`/`updatedAt` 审计字段由 `@CreatedDate`/`@LastModifiedDate` + `AuditingEntityListener` 管理，测试已验证其自动填充行为（见各实体基础映射测试中的断言）。

**交叉影响备注**：

1. **Issue 2（password NOT NULL）对 Issue 1 测试的影响**：Issue 2 要求在 User.password 上添加 NOT NULL 约束。因此 Issue 1 新增的测试中，所有 persist User 的操作均必须设置 password（调用 `setPassword()`），否则持久化时将抛出 `DataIntegrityViolationException`。Issue 1 的测试示例已全部遵循此要求。

2. **Issue 4（enabled 默认值）对 Issue 1 测试的影响**：User.enabled、Role.enabled、Post.enabled 均无 Java 默认值。若 Issue 4 的方案 A（Java 默认值）在 Issue 1 之前已修复，则新增测试中不需要显式 setEnabled()，因 `enabled = true` 默认值会生效；若 Issue 4 采用方案 C（仅改 DDL）或尚未修复，则测试代码必须在构建实体时显式调用 `setEnabled(true/false)`，否则 `enabled` 列将插入 NULL。

### 修复指引

在 `EntityMappingIT.java` 中新增 User、Role、Post 三组集成测试方法。以下提供完整的方法示例作为模板，每组测试应遵循「创建实体→persist→flush→find→断言」模式。

#### User 测试示例（组）

```java
// ==================== User ====================

@Test
void user_shouldMapUsernameField() {
    User user = new User();
    user.setUsername("testuser_mapping");
    user.setPassword("pwd123");
    user.setUserType(UserType.DOCTOR);
    user.setEnabled(true);

    entityManager.persist(user);
    entityManager.flush();

    User found = entityManager.find(User.class, user.getId());
    assertEquals("testuser_mapping", found.getUsername());
    assertNotNull(found.getPassword());
    assertEquals(UserType.DOCTOR, found.getUserType());
    assertTrue(found.getEnabled());
    assertNotNull(found.getDeleted());
    assertFalse(found.getDeleted());
    assertNotNull(found.getCreatedAt());
    assertNotNull(found.getUpdatedAt());
}

@Test
void user_shouldRejectNullPassword() {
    // 验证 password 列不允许 NULL（Issue 2：password 缺少 NOT NULL 约束）
    // 注意：此测试仅在 Issue 2（password NOT NULL DDL 已应用）后通过。
    // 若在修复前运行，因 password 列允许 NULL，不会抛出异常。
    // 不设置 password → password 为 null，预期抛出 DataIntegrityViolationException
    User user = new User();
    user.setUsername("testuser_nonullpwd");
    user.setUserType(UserType.PATIENT);
    user.setEnabled(true);

    assertThrows(DataIntegrityViolationException.class, () -> {
        entityManager.persist(user);
        entityManager.flush();
    });
}

@Test
void user_shouldEnforceUserTypeNotNull() {
    // 验证 userType 列 NOT NULL 约束——userType 标注了 @Column(nullable = false)
    // 不设置 userType → userType 为 null，预期抛出 DataIntegrityViolationException
    User user = new User();
    user.setUsername("testuser_notnulltype");
    user.setPassword("pwd123");
    user.setEnabled(true);

    assertThrows(DataIntegrityViolationException.class, () -> {
        entityManager.persist(user);
        entityManager.flush();
    });
}

@Test
void user_shouldMapManyToManyWithRoles() {
    Role role = new Role();
    role.setCode("TEST_ROLE_MAPPING");
    role.setName("测试角色");
    role.setEnabled(true);
    entityManager.persist(role);
    entityManager.flush();

    User user = new User();
    user.setUsername("testuser_roles");
    user.setPassword("pwd123");
    user.setUserType(UserType.ADMIN);
    user.setEnabled(true);
    user.setRoles(Set.of(role));
    entityManager.persist(user);
    entityManager.flush();

    entityManager.clear();

    User found = entityManager.find(User.class, user.getId());
    assertNotNull(found.getRoles());
    assertEquals(1, found.getRoles().size());
    assertTrue(found.getRoles().stream().anyMatch(r -> "TEST_ROLE_MAPPING".equals(r.getCode())));
}

@Test
void user_shouldMapManyToManyWithPosts() {
    Post post = new Post();
    post.setCode("TEST_POST_MAPPING");
    post.setName("测试岗位");
    post.setEnabled(true);
    entityManager.persist(post);
    entityManager.flush();

    User user = new User();
    user.setUsername("testuser_posts");
    user.setPassword("pwd123");
    user.setUserType(UserType.ADMIN);
    user.setEnabled(true);
    user.setPosts(Set.of(post));
    entityManager.persist(user);
    entityManager.flush();

    entityManager.clear();

    User found = entityManager.find(User.class, user.getId());
    assertNotNull(found.getPosts());
    assertEquals(1, found.getPosts().size());
    assertTrue(found.getPosts().stream().anyMatch(p -> "TEST_POST_MAPPING".equals(p.getCode())));
}

@Test
void user_shouldMapUserTypeEnumAsString() {
    User user = new User();
    user.setUsername("testuser_enum");
    user.setPassword("pwd123");
    user.setUserType(UserType.PATIENT);
    user.setEnabled(true);

    entityManager.persist(user);
    entityManager.flush();

    entityManager.clear();

    String storedType = (String) entityManager
        .createNativeQuery("SELECT user_type FROM sys_user WHERE id = :id")
        .setParameter("id", user.getId())
        .getSingleResult();
    assertEquals("PATIENT", storedType);

    User found = entityManager.find(User.class, user.getId());
    assertEquals(UserType.PATIENT, found.getUserType());
}
```

#### Role 测试示例（组）

```java
// ==================== Role ====================

@Test
void role_shouldMapCodeField() {
    Role role = new Role();
    role.setCode("TEST_ROLE_UNIQUE");
    role.setName("测试唯一角色");
    role.setEnabled(true);

    entityManager.persist(role);
    entityManager.flush();

    Role found = entityManager.find(Role.class, role.getId());
    assertEquals("TEST_ROLE_UNIQUE", found.getCode());
    assertTrue(found.getEnabled());
    assertNotNull(found.getDeleted());
    assertFalse(found.getDeleted());
}

@Test
void role_shouldEnforceCodeUniqueConstraint() {
    Role role = new Role();
    role.setCode("DUPLICATE_CODE");
    role.setName("第一个角色");
    role.setEnabled(true);
    entityManager.persist(role);
    entityManager.flush();

    Role duplicate = new Role();
    duplicate.setCode("DUPLICATE_CODE");
    duplicate.setName("重复角色");
    duplicate.setEnabled(true);

    assertThrows(DataIntegrityViolationException.class, () -> {
        entityManager.persist(duplicate);
        entityManager.flush();
    });
}

@Test
void role_shouldMapOneToManyPosts() {
    Role role = new Role();
    role.setCode("ROLE_WITH_POSTS");
    role.setName("有岗位的角色");
    role.setEnabled(true);
    entityManager.persist(role);
    entityManager.flush();

    Post post = new Post();
    post.setCode("POST_UNDER_ROLE");
    post.setName("角色下岗位");
    post.setEnabled(true);
    post.setRole(role);
    entityManager.persist(post);
    entityManager.flush();

    Role found = entityManager.find(Role.class, role.getId());
    assertTrue(found.getEnabled());
    assertNotNull(found.getPosts());
    assertEquals(1, found.getPosts().size());
    assertEquals("POST_UNDER_ROLE", found.getPosts().iterator().next().getCode());
}
```

#### Post 测试示例（组）

```java
// ==================== Post ====================

@Test
void post_shouldMapManyToOneRole() {
    Role role = new Role();
    role.setCode("ROLE_FOR_POST");
    role.setName("岗位关联角色");
    role.setEnabled(true);
    entityManager.persist(role);
    entityManager.flush();

    Post post = new Post();
    post.setCode("TEST_POST_MTO");
    post.setName("测试岗位-多对一");
    post.setEnabled(true);
    post.setRole(role);
    post.setSort(1);

    entityManager.persist(post);
    entityManager.flush();

    Post found = entityManager.find(Post.class, post.getId());
    assertTrue(found.getEnabled());
    assertNotNull(found.getRole());
    assertEquals("ROLE_FOR_POST", found.getRole().getCode());
    assertEquals(Integer.valueOf(1), found.getSort());
    assertNotNull(found.getDeleted());
    assertFalse(found.getDeleted());
}

@Test
void post_shouldMapManyToManyFunctions() {
    Function function = new Function();
    function.setCode("TEST_FUNC_POST");
    function.setName("岗位功能");
    function.setType(MenuType.BUTTON);
    function.setEnabled(true);
    entityManager.persist(function);
    entityManager.flush();

    Post post = new Post();
    post.setCode("POST_WITH_FUNCS");
    post.setName("有功能的岗位");
    post.setEnabled(true);
    post.setFunctions(Set.of(function));
    entityManager.persist(post);
    entityManager.flush();

    entityManager.clear();

    Post found = entityManager.find(Post.class, post.getId());
    assertTrue(found.getEnabled());
    assertEquals("POST_WITH_FUNCS", found.getCode());
    assertNotNull(found.getFunctions());
    assertEquals(1, found.getFunctions().size());
    assertTrue(found.getFunctions().stream().anyMatch(f -> "TEST_FUNC_POST".equals(f.getCode())));
    assertNotNull(found.getDeleted());
    assertFalse(found.getDeleted());
}
```

#### @ManyToMany 映射测试策略说明

- **级联策略**：User.roles 和 User.posts 均未设置 `cascade`（默认无级联操作），因此测试中需要先 persist Role/Post 再 persist User，否则 `TransientObjectException` 将抛出。如果后续为这些关系添加了级联配置，可简化测试流程。
- **关联表验证**：@ManyToMany 使用 `@JoinTable` 指定中间表，entityManager.clear() 用于清除一级缓存，强制 entityManager.find() 从数据库重新加载，从而验证关联表记录确实已写入数据库。上述测试中，涉及 @ManyToMany 关系的测试（user_shouldMapManyToManyWithRoles、user_shouldMapManyToManyWithPosts、post_shouldMapManyToManyFunctions）以及 @Enumerated 原生 SQL 验证测试（user_shouldMapUserTypeEnumAsString）使用了 clear()；其他简单映射测试未使用 clear()，与现有 EntityMappingIT.java 中其他测试的风格一致。现有测试中（如 dictType_shouldHaveOneToManyDictDataList）验证关系映射时也未使用 clear()，这是因为 @Transactional 保持持久化上下文开放，LAZY 集合在事务内可正常加载。@ManyToMany 测试中额外使用 clear() 是出于"验证关联表确实持久化到数据库"的审慎性考虑，不是必须的——如果你偏好完全统一的风格，可移除所有 clear() 调用。
- **unique 约束验证策略**：对于 `username`、`code` 等 unique 约束，基本的 persist/flush/find 已能确认 DDL 中存在唯一索引，充分验证需要用重复值触发 `DataIntegrityViolationException`。可在单独测试中实现。
- **NOT NULL 约束验证策略**：User.userType 标注了 `@Column(nullable = false)`，通过独立的 `user_shouldEnforceUserTypeNotNull()` 测试验证——persist 一个未设置 userType 的 User 预期抛出 `DataIntegrityViolationException`。同理，BaseEntity.deleted 的 NOT NULL 约束可通过 persist 后检查值来验证（因 BaseEntity 已设 `deleted = false` 默认值，不会出现 null）。password 的 NOT NULL 约束验证见 `user_shouldRejectNullPassword()`。

### 测试环境能力边界说明

EntityMappingIT 的测试环境配置为 `@AutoConfigureTestDatabase` + H2 内存数据库 + `ddl-auto: create-drop` + `sql.init.mode: never`。此环境决定了测试的能力边界：

| 可验证的能力 | 不可验证的能力 |
|-------------|---------------|
| 实体 `@Column` 注解级别的映射正确性（列名、nullable、unique、length、precision、枚举映射、关系映射） | `schema.sql` 中的 DDL 定义（NOT NULL、DEFAULT、UNIQUE KEY 等）是否与实体注解一致 |
| Hibernate 根据实体注解自动生成的 DDL 在 H2 中的行为 | MySQL 特定 DDL 语法的正确性（如 `ENGINE=InnoDB`、`CHARSET utf8mb4`） |
| CRUD 操作在 `@Transactional` 下的基本正确性 | `schema.sql` 中手工编写的约束与实体注解之间的一致性校验 |
| BaseEntity 继承字段（deleted/createdAt/updatedAt）的映射行为 | — |

**审计功能前提**：`createdAt`/`updatedAt` 的自动填充依赖 `@EnableJpaAuditing`（位于 `JpaConfig.java:7`）。EntityMappingIT 使用 `@SpringBootTest(classes = com.aimedical.Application.class)`，Application 的 `@SpringBootApplication` 组件扫描会加载 `JpaConfig`，因此审计功能在测试环境中已启用。测试示例中的 `assertNotNull(found.getCreatedAt())` / `assertNotNull(found.getUpdatedAt())` 正是依赖此前提。若后续改用 `@DataJpaTest`，需显式添加 `@Import(JpaConfig.class)`。

**关键约束**：由于 `sql.init.mode: never`，`schema.sql` 在测试中从未被加载。Hibernate 根据实体注解自动生成 DDL，因此：
- Issue 3 的 deleted NOT NULL 约束在测试环境中始终为真——`BaseEntity.java` 已有 `@Column(nullable = false)`，Hibernate 在 H2 中自动为该列生成 NOT NULL。测试**无法**验证 `schema.sql` 中 deleted 列 NOT NULL 缺失的问题。
- 映射点表中列出的验证目标混合了 entity-annotation 级别与 schema.sql 级别的目标（如 M2 password NOT NULL 约束既需验证实体注解又需验证 DDL），实际测试仅能覆盖前者。

**对优先级排序的影响**：Issue 1 新增测试可验证的内容应重新表述为：
1. Issue 2 的 Java 注解修复（`@Column(nullable = false)` on password）
2. Issue 4 的 Java 默认值修复（`enabled = true` / `visible = true`）
3. 实体映射的基本正确性（含 BaseEntity 继承的 deleted/createdAt/updatedAt）
4. 可间接捕获 `schema.sql` 与实体不一致的线索（如 H2 生成的 DDL 与手工 DDL 在列类型上的差异），但无法做确定性断言

如需验证 `schema.sql` 的 DDL 定义与实体注解一致，应增加 Testcontainers MySQL 集成测试或独立的 schema 校验脚本。当前测试环境的能力边界建议在测试类的类级注释中显式声明，避免后续维护者产生错误预期。

### 修复方案潜在副作用分析

无功能副作用。仅影响测试覆盖完备性，生产代码不受影响。

### 影响范围

仅影响测试覆盖完备性。生产代码功能不受影响，但缺少集成测试意味着 User/Role/Post 的 JPA 映射如果出现退化（regression），不会被测试捕获。

---

## 2. password 字段缺少 NOT NULL 约束

### 现象

`User.java:28`：
```java
private String password;
```
未标注 `@Column(nullable = false)`。

`schema.sql:16` 对应 DDL：
```sql
`password` VARCHAR(128) DEFAULT NULL COMMENT '密码',
```

实体和 DDL 均允许 `password` 为 NULL。

### 根因

`User.java:28` 缺少 `@Column(nullable = false)` 注解。对比同实体的 `username` 字段（第25行：`@Column(nullable = false, unique = true)`）有显式 NOT NULL 约束，`password` 作为系统核心字段（用户认证凭据）理应有同样约束却没有设置。

DDL 与之对齐（`DEFAULT NULL`），因此实体与 DDL 之间不存在不一致，但约束本身缺失。

### 修复指引

在 `User.java:28` 的 `password` 字段上添加 `@Column(nullable = false)` 注解，并将 `schema.sql:16` 的 `DEFAULT NULL` 改为 `NOT NULL`。两处改动需同步进行。

### 修复方案潜在副作用分析

添加 `@Column(nullable = false)` + DDL NOT NULL 后可能的影响：

- **代码路径排查结果**：已搜索整个代码库中 `new User()` 和 `UserRepository` 的使用情况：

  - `new User()` 调用（13 处）：全部出现在测试代码中——`UserTest.java`（11 处，仅 `shouldSetAndGetPassword` 设置了 password，其余仅验证 getter/setter 不涉及持久化）、`RoleTest.java:63` 和 `PostTest.java:71`（创建 User 仅用于填充 `Set<User>` 集合，不涉及持久化）。

   - `UserRepository` 引用：全局搜索 `UserRepository`，全部匹配均为：接口定义（`UserRepository.java`）、单元测试（`UserRepositoryTest.java`，仅验证接口形态和注解声明）、以及 OOD 设计文档中的规划描述（如"自定义 UserDetailsService 通过 UserRepository 加载 User"）。**当前没有任何生产代码通过 `@Autowired UserRepository` 注入或调用 `userRepository.save()` 来持久化 User 对象。**

  - SQL 脚本插入 User 数据：`db/data.sql:81-84` 插入了 3 条种子用户，均包含非 NULL 的 password 值（bcrypt 哈希）。但该脚本的 `spring.sql.init.mode` 配置为 `never`（`application.yml:31`），因此这些种子数据不会被 Spring Boot 自动加载执行，仅作为参考文档存在。

  - **结论**：当前整个代码库不存在任何生产代码路径或自动化的数据脚本会向 `sys_user` 表插入 password 为 NULL 的记录。因此添加 NOT NULL 约束在当前阶段无运行时 `DataIntegrityViolationException` 风险。

- **已有 NULL 数据的来源说明**：尽管当前不存在生产代码路径创建 NULL password 的记录，但生产数据库中可能已存在 `password IS NULL` 的脏数据。这种情况可能来源于：① 旧版本代码或已移除的模块曾创建 User 时未设置 password；② 前期数据导入/种子脚本在约束未完善时执行；③ 其他系统或运维人员直接通过 SQL INSERT 写入数据库。这些脏数据的存在不影响"添加 NOT NULL 约束"这一修复决定，但要求在 ALTER TABLE 之前必须先清理。

- **测试影响**：UserTest.java 中除 `shouldSetAndGetPassword` 外，其他测试方法创建 User 后均未设置 password。当前这些测试仅验证 getter/setter、不涉及持久化，因此不受影响。一旦后续在这些测试中增加持久化操作，将触发约束异常。

### 影响范围

- 代码或 SQL 插入一条 `password` 为 NULL 的 `sys_user` 记录时，不会收到数据库约束错误，导致系统中可能产生无法登录的脏数据
- 此问题在生产数据库上已存在（生产 DDL 该列无 NOT NULL），清理已存在的 NULL 数据需要单独处理

### 生产脏数据行动指引

**预检 SQL**（在生产数据库执行前确认脏数据分布）：
```sql
SELECT COUNT(*) AS null_password_count FROM sys_user WHERE password IS NULL;
SELECT id, username FROM sys_user WHERE password IS NULL;
```

**清理策略**——按脏数据严重程度分三种方案：

| 方案 | 适用场景 | 操作 |
|------|---------|------|
| A. 人工补录 | 少量记录（<10条）且可联系用户 | 逐一联系用户重置密码，或由管理员在可审计流程下设置临时密码 |
| B. 批量置为不可用 | 大量记录但无法逐一处理 | `UPDATE sys_user SET enabled = 0 WHERE password IS NULL;` 将无密码账号禁用，防止意外登录 |
| C. 直接清理 | 确认为测试/无效数据 | `DELETE FROM sys_user WHERE password IS NULL AND created_at < '阈值日期';` |

**操作顺序建议**：①预检确认脏数据量 → ②业务决策选择清理方案 → ③执行数据清理 → ④在变更窗口执行 DDL 变更（添加 NOT NULL）→ ⑤验证约束生效（尝试插入 NULL 应被拒绝）

**与 Issue 4 清理策略的交叉数据冲突**：

当一条 `sys_user` 记录同时满足 `password IS NULL` 和 `enabled IS NULL` 时：
- Issue 2 的方案 B（`UPDATE sys_user SET enabled = 0 WHERE password IS NULL`）将其 enabled 置为 0（禁用）
- Issue 4 的清理 SQL（`UPDATE sys_user SET enabled = 1 WHERE enabled IS NULL`）将其 enabled 置为 1（启用）

两个更新的执行顺序决定该记录的最终 enabled 值。建议操作顺序：
1. 先执行 Issue 4 的清理（`SET enabled = 1 WHERE enabled IS NULL`），将所有 NULL 置为启用
2. 再执行 Issue 2 的方案 B（`SET enabled = 0 WHERE password IS NULL`），将无密码账号禁用

这样确保最终语义为"无密码账号被禁用"，与安全策略一致。若反向执行（先 Issue 2 再 Issue 4），无密码且无 enabled 值的记录会被置为启用，形成安全漏洞。

---

## 3. DDL 中 deleted 列 NOT NULL 约束与 BaseEntity 不一致

### 现象

`BaseEntity.java:37`：
```java
@Column(nullable = false)
private Boolean deleted = false;
```
实体声明 `deleted` 为 NOT NULL（`nullable = false`）。

`schema.sql` 中所有表的 `deleted` 列均缺少 `NOT NULL`，例如 `sys_user`（第25行）：
```sql
`deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
```
没有 `NOT NULL`。

### 根因

`schema.sql` 中所有表的 `deleted` 列定义时只设置了 `DEFAULT 0`，但遗漏了 `NOT NULL` 约束。`BaseEntity` 中已正确标注 `nullable = false`，DDL 手工编写时未与之对齐。

涉及的全部表：

| 表名 | schema.sql 行号 |
|------|----------------|
| sys_user | 25 |
| sys_role | 44 |
| sys_post | 65 |
| sys_function | 94 |
| sys_dict_type | 113 |
| sys_dict_data | 135 |
| patient_profile | 197 |
| doctor_profile | 222 |
| admin_profile | 242 |
| health_profile | 263 |
| allergy_history | 284 |
| chronic_disease | 303 |
| family_history | 322 |
| surgery_history | 341 |
| medication_history | 361 |
| sys_token | 422 |

### 修复指引

在 `schema.sql` 中上述16张表的 `deleted` 列定义中，在 `DEFAULT 0` 之前添加 `NOT NULL`，例如将 `` `deleted` TINYINT(1) DEFAULT 0 `` 改为 `` `deleted` TINYINT(1) NOT NULL DEFAULT 0 ``。无需改动 `BaseEntity.java`（已正确）。

### 生产数据库迁移方案

对已存在的生产数据库，直接执行 ALTER TABLE 会因现有 NULL 值而失败。需按以下步骤操作：

**步骤 1：预检脏数据**

```sql
-- 检查每张表的 deleted IS NULL 记录数
SELECT 'sys_user' AS table_name, COUNT(*) AS null_count FROM sys_user WHERE deleted IS NULL
UNION ALL SELECT 'sys_role', COUNT(*) FROM sys_role WHERE deleted IS NULL
UNION ALL SELECT 'sys_post', COUNT(*) FROM sys_post WHERE deleted IS NULL
UNION ALL SELECT 'sys_function', COUNT(*) FROM sys_function WHERE deleted IS NULL
UNION ALL SELECT 'sys_dict_type', COUNT(*) FROM sys_dict_type WHERE deleted IS NULL
UNION ALL SELECT 'sys_dict_data', COUNT(*) FROM sys_dict_data WHERE deleted IS NULL
UNION ALL SELECT 'patient_profile', COUNT(*) FROM patient_profile WHERE deleted IS NULL
UNION ALL SELECT 'doctor_profile', COUNT(*) FROM doctor_profile WHERE deleted IS NULL
UNION ALL SELECT 'admin_profile', COUNT(*) FROM admin_profile WHERE deleted IS NULL
UNION ALL SELECT 'health_profile', COUNT(*) FROM health_profile WHERE deleted IS NULL
UNION ALL SELECT 'allergy_history', COUNT(*) FROM allergy_history WHERE deleted IS NULL
UNION ALL SELECT 'chronic_disease', COUNT(*) FROM chronic_disease WHERE deleted IS NULL
UNION ALL SELECT 'family_history', COUNT(*) FROM family_history WHERE deleted IS NULL
UNION ALL SELECT 'surgery_history', COUNT(*) FROM surgery_history WHERE deleted IS NULL
UNION ALL SELECT 'medication_history', COUNT(*) FROM medication_history WHERE deleted IS NULL
UNION ALL SELECT 'sys_token', COUNT(*) FROM sys_token WHERE deleted IS NULL;
```

**步骤 2：清理 NULL 数据**

```sql
-- 批量修复：将现有 NULL 值置为 0
UPDATE sys_user        SET deleted = 0 WHERE deleted IS NULL;
UPDATE sys_role        SET deleted = 0 WHERE deleted IS NULL;
UPDATE sys_post        SET deleted = 0 WHERE deleted IS NULL;
UPDATE sys_function    SET deleted = 0 WHERE deleted IS NULL;
UPDATE sys_dict_type   SET deleted = 0 WHERE deleted IS NULL;
UPDATE sys_dict_data   SET deleted = 0 WHERE deleted IS NULL;
UPDATE patient_profile SET deleted = 0 WHERE deleted IS NULL;
UPDATE doctor_profile  SET deleted = 0 WHERE deleted IS NULL;
UPDATE admin_profile   SET deleted = 0 WHERE deleted IS NULL;
UPDATE health_profile  SET deleted = 0 WHERE deleted IS NULL;
UPDATE allergy_history SET deleted = 0 WHERE deleted IS NULL;
UPDATE chronic_disease SET deleted = 0 WHERE deleted IS NULL;
UPDATE family_history  SET deleted = 0 WHERE deleted IS NULL;
UPDATE surgery_history SET deleted = 0 WHERE deleted IS NULL;
UPDATE medication_history SET deleted = 0 WHERE deleted IS NULL;
UPDATE sys_token       SET deleted = 0 WHERE deleted IS NULL;
```

**步骤 3：批量 ALTER TABLE 迁移**

```sql
ALTER TABLE sys_user        MODIFY COLUMN `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除';
ALTER TABLE sys_role        MODIFY COLUMN `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除';
ALTER TABLE sys_post        MODIFY COLUMN `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除';
ALTER TABLE sys_function    MODIFY COLUMN `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除';
ALTER TABLE sys_dict_type   MODIFY COLUMN `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除';
ALTER TABLE sys_dict_data   MODIFY COLUMN `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除';
ALTER TABLE patient_profile MODIFY COLUMN `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除';
ALTER TABLE doctor_profile  MODIFY COLUMN `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除';
ALTER TABLE admin_profile   MODIFY COLUMN `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除';
ALTER TABLE health_profile  MODIFY COLUMN `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除';
ALTER TABLE allergy_history MODIFY COLUMN `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除';
ALTER TABLE chronic_disease MODIFY COLUMN `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除';
ALTER TABLE family_history  MODIFY COLUMN `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除';
ALTER TABLE surgery_history MODIFY COLUMN `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除';
ALTER TABLE medication_history MODIFY COLUMN `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除';
ALTER TABLE sys_token       MODIFY COLUMN `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除';
```

**步骤 4：验证约束生效**

```sql
-- 确认所有表的 deleted 列已变更为 NOT NULL
SELECT TABLE_NAME, COLUMN_NAME, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND COLUMN_NAME = 'deleted'
  AND IS_NULLABLE = 'NO';

-- 尝试插入 NULL 应被拒绝（在测试环境验证）
INSERT INTO sys_user (username, password, user_type, deleted)
VALUES ('test_null_deleted', 'pwd', 'ADMIN', NULL);
-- 预期结果：ERROR 1048 (23000): Column 'deleted' cannot be null
```

### 业务影响评估

执行迁移前，业务方需逐项确认以下事项：

**① 预审隐藏记录清单**：执行以下 SELECT 列出所有 `deleted IS NULL` 的记录，供业务方逐一审视记录内容，评估 UPDATE 后突然可见对业务流程的具体影响：

```sql
SELECT 'sys_user' AS table_name, id, username AS biz_key, deleted FROM sys_user WHERE deleted IS NULL
UNION ALL SELECT 'sys_role', id, code, deleted FROM sys_role WHERE deleted IS NULL
UNION ALL SELECT 'sys_post', id, code, deleted FROM sys_post WHERE deleted IS NULL
UNION ALL SELECT 'sys_function', id, code, deleted FROM sys_function WHERE deleted IS NULL
UNION ALL SELECT 'sys_dict_type', id, name, deleted FROM sys_dict_type WHERE deleted IS NULL
UNION ALL SELECT 'sys_dict_data', id, label, deleted FROM sys_dict_data WHERE deleted IS NULL
UNION ALL SELECT 'patient_profile', id, CAST(id AS CHAR), deleted FROM patient_profile WHERE deleted IS NULL
UNION ALL SELECT 'doctor_profile', id, CAST(id AS CHAR), deleted FROM doctor_profile WHERE deleted IS NULL
UNION ALL SELECT 'admin_profile', id, CAST(id AS CHAR), deleted FROM admin_profile WHERE deleted IS NULL
UNION ALL SELECT 'health_profile', id, CAST(id AS CHAR), deleted FROM health_profile WHERE deleted IS NULL
UNION ALL SELECT 'allergy_history', id, CAST(id AS CHAR), deleted FROM allergy_history WHERE deleted IS NULL
UNION ALL SELECT 'chronic_disease', id, CAST(id AS CHAR), deleted FROM chronic_disease WHERE deleted IS NULL
UNION ALL SELECT 'family_history', id, CAST(id AS CHAR), deleted FROM family_history WHERE deleted IS NULL
UNION ALL SELECT 'surgery_history', id, CAST(id AS CHAR), deleted FROM surgery_history WHERE deleted IS NULL
UNION ALL SELECT 'medication_history', id, CAST(id AS CHAR), deleted FROM medication_history WHERE deleted IS NULL
UNION ALL SELECT 'sys_token', id, CAST(id AS CHAR), deleted FROM sys_token WHERE deleted IS NULL;
```

**② 分类处理策略**：业务方根据记录实际语义逐条判断，按以下分类处理：

| 分类 | 含义 | 处理方式 |
|------|------|---------|
| 确认为已删除数据 | 记录对应业务实体已失效/废弃 | `DELETE FROM {table} WHERE id = {id}` 硬删除 |
| 不应删除但可恢复可见 | 记录应保留，`deleted IS NULL` 为误写入 | `UPDATE {table} SET deleted = 0 WHERE id = {id}` |
| 语义不确定需保留审计 | 无法确定原始状态，但需保留数据 | `UPDATE {table} SET deleted = 0, remark = CONCAT(IFNULL(remark,''),'[迁移审计：原值为NULL，已恢复为可见]') WHERE id = {id}` |

**③ 执行窗口与通知**：
- 选择业务低峰期执行（建议凌晨 02:00-05:00）
- 提前至少 24 小时向相关业务方发出变更通知，内容包括：变更范围（16 张表）、预计影响时间窗口、回滚方案

**④ 回滚方案**：

命名约定：所有备份表统一命名为 `{table_name}_bak_YYYYMMDD`（YYYYMMDD 替换为执行当天的日期）。

备份 SQL（执行步骤 2 前执行，为全部 16 张表逐一创建备份）：
```sql
CREATE TABLE sys_user_bak_YYYYMMDD         AS SELECT * FROM sys_user;
CREATE TABLE sys_role_bak_YYYYMMDD         AS SELECT * FROM sys_role;
CREATE TABLE sys_post_bak_YYYYMMDD         AS SELECT * FROM sys_post;
CREATE TABLE sys_function_bak_YYYYMMDD     AS SELECT * FROM sys_function;
CREATE TABLE sys_dict_type_bak_YYYYMMDD    AS SELECT * FROM sys_dict_type;
CREATE TABLE sys_dict_data_bak_YYYYMMDD    AS SELECT * FROM sys_dict_data;
CREATE TABLE patient_profile_bak_YYYYMMDD  AS SELECT * FROM patient_profile;
CREATE TABLE doctor_profile_bak_YYYYMMDD   AS SELECT * FROM doctor_profile;
CREATE TABLE admin_profile_bak_YYYYMMDD    AS SELECT * FROM admin_profile;
CREATE TABLE health_profile_bak_YYYYMMDD   AS SELECT * FROM health_profile;
CREATE TABLE allergy_history_bak_YYYYMMDD  AS SELECT * FROM allergy_history;
CREATE TABLE chronic_disease_bak_YYYYMMDD  AS SELECT * FROM chronic_disease;
CREATE TABLE family_history_bak_YYYYMMDD   AS SELECT * FROM family_history;
CREATE TABLE surgery_history_bak_YYYYMMDD  AS SELECT * FROM surgery_history;
CREATE TABLE medication_history_bak_YYYYMMDD AS SELECT * FROM medication_history;
CREATE TABLE sys_token_bak_YYYYMMDD        AS SELECT * FROM sys_token;
```

回滚 SQL（从备份恢复 deleted 原始值，全部 16 张表逐一执行）：
```sql
UPDATE sys_user t         JOIN sys_user_bak_YYYYMMDD b         ON t.id = b.id SET t.deleted = b.deleted;
UPDATE sys_role t         JOIN sys_role_bak_YYYYMMDD b         ON t.id = b.id SET t.deleted = b.deleted;
UPDATE sys_post t         JOIN sys_post_bak_YYYYMMDD b         ON t.id = b.id SET t.deleted = b.deleted;
UPDATE sys_function t     JOIN sys_function_bak_YYYYMMDD b     ON t.id = b.id SET t.deleted = b.deleted;
UPDATE sys_dict_type t    JOIN sys_dict_type_bak_YYYYMMDD b    ON t.id = b.id SET t.deleted = b.deleted;
UPDATE sys_dict_data t    JOIN sys_dict_data_bak_YYYYMMDD b    ON t.id = b.id SET t.deleted = b.deleted;
UPDATE patient_profile t  JOIN patient_profile_bak_YYYYMMDD b  ON t.id = b.id SET t.deleted = b.deleted;
UPDATE doctor_profile t   JOIN doctor_profile_bak_YYYYMMDD b   ON t.id = b.id SET t.deleted = b.deleted;
UPDATE admin_profile t    JOIN admin_profile_bak_YYYYMMDD b    ON t.id = b.id SET t.deleted = b.deleted;
UPDATE health_profile t   JOIN health_profile_bak_YYYYMMDD b   ON t.id = b.id SET t.deleted = b.deleted;
UPDATE allergy_history t  JOIN allergy_history_bak_YYYYMMDD b  ON t.id = b.id SET t.deleted = b.deleted;
UPDATE chronic_disease t  JOIN chronic_disease_bak_YYYYMMDD b  ON t.id = b.id SET t.deleted = b.deleted;
UPDATE family_history t   JOIN family_history_bak_YYYYMMDD b   ON t.id = b.id SET t.deleted = b.deleted;
UPDATE surgery_history t  JOIN surgery_history_bak_YYYYMMDD b  ON t.id = b.id SET t.deleted = b.deleted;
UPDATE medication_history t JOIN medication_history_bak_YYYYMMDD b ON t.id = b.id SET t.deleted = b.deleted;
UPDATE sys_token t        JOIN sys_token_bak_YYYYMMDD b        ON t.id = b.id SET t.deleted = b.deleted;
```

**操作顺序建议**：①业务预审（本评估步骤）→ ②创建备份 → ③UPDATE 清理（步骤 2）→ ④验证清理结果并通知业务方 → ⑤逐表或分批执行 ALTER TABLE（步骤 3，建议在低峰期执行）→ ⑥验证约束生效（步骤 4）→ ⑦更新 schema.sql 文件

### 修复方案潜在副作用分析

- **已有 NULL 数据若不清理**：ALTER TABLE 将直接失败，无法添加 NOT NULL 约束。
- **大表锁问题**：ALTER TABLE MODIFY COLUMN 在 MySQL 中可能引发表锁或重建表。对于大表（如 sys_user），建议在低峰期执行，或使用 pt-online-schema-change 等工具减少锁时间。
- **Hibernate DDL auto 一致性**：使用 Hibernate DDL auto 生成的数据库会自动带 NOT NULL（因实体标注了 `nullable = false`），与修改后的手工 DDL 一致。此变更是对齐两者，不会引入新不一致。
- **业务代码影响**：BaseEntity 已设置 `deleted = false`，Java 层面新创建的对象不会出现 NULL。业务代码中若存在 SQL 直接 INSERT 且未指定 deleted 列，DEFAULT 0 将生效（MODIFY 不改变 DEFAULT 逻辑），无影响。

### 影响范围

**在 `@SQLRestriction("deleted = false")` 下的实际行为**：

`BaseEntity.java:24` 声明 `@SQLRestriction("deleted = false")`，Hibernate 自动为所有继承 BaseEntity 的实体追加 `WHERE deleted = false`。在 SQL 三值逻辑（three-valued logic）中：

- `NULL = 0` 的结果是 UNKNOWN，而非 TRUE
- WHERE 子句将 UNKNOWN 视为 FALSE，因此 `deleted IS NULL` 的记录会被 `@SQLRestriction` 过滤掉——**实际被当作"已删除"处理**
- 只有 `deleted = 0` 的记录才满足 `WHERE deleted = false`，被查询返回

这意味着：

1. 当前 `schema.sql` 缺少 NOT NULL，导致数据库中可能已存在 `deleted IS NULL` 的脏数据
2. **在修复 DDL 之前**，这些脏数据被 `@SQLRestriction` 静默隐藏，不会被任何查询返回——影响范围超出"软删除行为不一致"
3. **执行迁移步骤 2（UPDATE NULL → 0）后**，这些记录将突然出现在查询结果中，这是一个业务可见的行为变化
4. 使用 Hibernate DDL auto 生成的数据库会自动添加 NOT NULL（因实体标注了 `nullable = false`），与手工 DDL 不一致

---

## 4. enabled/visible 布尔字段跨实体缺少默认值

### 现象

四个实体中的 `enabled` 字段均未设置 Java 默认值：
- `User.java:36`：`private Boolean enabled;`
- `Role.java:28`：`private Boolean enabled;`
- `Post.java:30`：`private Boolean enabled;`
- `Function.java:30`：`private Boolean enabled;`

`Function.java:54` 的 `visible` 同样未设置默认值：
- `private Boolean visible;`

DDL 中为所有 `enabled` 和 `visible` 列配置了 `DEFAULT 1`。

### 根因

Java 实体通过 `@Column` 未指定 `columnDefinition`，且字段直接声明为 `private Boolean enabled;` 而非 `private Boolean enabled = true;`，导致：
- 新实体在 Java 中创建后不显式 setEnabled(...) → enabled 字段为 null
- Hibernate 持久化时会将 null 写入数据库列
- DDL DEFAULT 值只在 SQL INSERT 未指定该列时才生效，但 Hibernate 通常会在 INSERT 语句中包含所有列，因此 DEFAULT 不会被触发

### 影响范围

| 实体 | 字段 | Java 行号 | DDL 行号 | Java 默认值 | DDL 默认值 |
|------|------|---------|---------|------------|-----------|
| User | enabled | User.java:36 | schema.sql:21 | null | DEFAULT 1 |
| Role | enabled | Role.java:28 | schema.sql:40 | null | DEFAULT 1 |
| Post | enabled | Post.java:30 | schema.sql:60 | null | DEFAULT 1 |
| Function | enabled | Function.java:30 | schema.sql:90 | null | DEFAULT 1 |
| Function | visible | Function.java:54 | schema.sql:86 | null | DEFAULT 1 |

实际行为：通过 Java 代码创建实体而不设置 `enabled`，Hibernate 会插入 NULL；通过 SQL 直接插入不指定 `enabled`，数据库使用 DEFAULT 1。两种路径行为不一致，可能导致通过不同入口创建的数据在布尔语义上出现差异。

**交叉影响备注**：

1. 详见 Issue 1 的交叉影响备注——Issue 4 的修复选型和时序直接影响 Issue 1 新增测试中是否需要显式 setEnabled()。
2. **Issue 2（password NOT NULL）清理时序依赖**：见 Issue 2 的"与 Issue 4 清理策略的交叉数据冲突"分析。当一条记录同时满足 `enabled IS NULL` 和 `password IS NULL` 时，Issue 4 的清理 SQL（`SET enabled = 1 WHERE enabled IS NULL`）与 Issue 2 的方案 B（`SET enabled = 0 WHERE password IS NULL`）存在执行顺序依赖。此 SQL 应在 Issue 2 的方案 B 之前执行，顺序为：先执行 Issue 4 的 enabled 清理（将所有 NULL 置为启用），再执行 Issue 2 的 password 清理（将无密码账号禁用），确保最终语义为"无密码账号被禁用"。若反向执行则产生安全漏洞。

### 修复路径权衡分析

三个可行的修复路径：

| 方案 | 操作 | 优点 | 缺点 |
|------|------|------|------|
| A. Java 端设默认值 | 将5个字段改为 `private Boolean enabled = true;` / `private Boolean visible = true;` | 最直观，Java 语义自文档化；新创建实体自动有正确值 | 不影响 DDL（数据库列仍可为 NULL）；需逐个修改5处 |
| B. @ColumnDefault 注解 | 在字段上添加 `@ColumnDefault("true")` | Hibernate 生成 DDL 时会包含 DEFAULT；语义显式 | 不改变 Java 端行为——通过代码创建实体后不 setEnabled 仍然为 null，仅影响 DDL 生成 |
| C. 仅改 DDL | 不修改 Java，仅确认 DDL 已有 DEFAULT 1 | 无代码变更成本 | Java 端行为不变，实体 null 写入问题仍然存在 |

**推荐方案 A + C 组合**：在 Java 端设置字段默认值（消除 null 写入路径），同时确认 DDL 已存在 DEFAULT 1（兜底 SQL 直接插入）。方案 B 可选作为补充，但 A 已能解决核心问题，B 提供的增量价值有限。

### 修复指引

方案 A：将5个字段声明改为带初始值的形式——`User.java:36`、`Role.java:28`、`Post.java:30`、`Function.java:30`、`Function.java:54` 分别改为 `private Boolean enabled = true;`（前三者）以及 `private Boolean visible = true;`（Function.visible）。

### 修复方案潜在副作用分析

- **已有 NULL 数据不受影响**：方案 A 只影响修改后新建的 Java 对象。数据库中已存在的 `enabled IS NULL` 记录不会被自动修复，新旧数据在布尔语义上将持续不一致。需另行执行数据清理：
  ```sql
  UPDATE sys_user SET enabled = 1 WHERE enabled IS NULL;
  UPDATE sys_role SET enabled = 1 WHERE enabled IS NULL;
  UPDATE sys_post SET enabled = 1 WHERE enabled IS NULL;
  UPDATE sys_function SET enabled = 1 WHERE enabled IS NULL;
  UPDATE sys_function SET visible = 1 WHERE visible IS NULL;
  ```
- **对现有业务逻辑的潜在影响**：如果现有代码依赖 `enabled == null` 作为某种特殊语义（如"未设置"状态），增加默认值后会消除这种区分。已搜索整个代码库中 `getEnabled()`、`enabled == null`、`isEnabled()`、`.enabled` 的使用情况：

  - `getEnabled()` 调用（4 处）：全部出现在单元测试中——`UserTest.java:60,62`、`RoleTest.java:45,47`、`PostTest.java:45,47`、`FunctionTest.java:45,47`，仅验证 getter/setter 的布尔值返回，不涉及持久化或业务逻辑分支。
  - `enabled == null` / `enabled != null`：全局搜索无匹配结果，不存在任何生产或测试代码判断 enabled 是否为 null。
  - `.enabled` 引用（1 处）：`MockAiService.java:40` 的 `@ConditionalOnProperty(name = "ai.mock.enabled")`，此为 Spring 配置属性，与实体字段无关。
  - **结论**：当前整个代码库不存在任何生产代码路径依赖 `enabled == null` 作为特殊语义。增加 Java 默认值不会改变现有业务逻辑行为。
- **序列化/反序列化兼容性**：如果实体被序列化（如 Redis 缓存、JSON 响应），修改前缓存中的旧数据可能不包含 enabled 字段，反序列化后获取 Java 默认值 `true`，与旧行为（null）不一致。建议在修改后清除相关缓存。

---

## 修复方案完整性逐项检查

| Issue | 修复方案摘要 | 完整性 | 备注 |
|-------|------------|--------|------|
| Issue 1：缺少 User/Role/Post 测试 | 在 EntityMappingIT.java 中新增三组集成测试方法 | 完整 | 已提供完整方法示例（含签名、断言、@ManyToMany 策略）、映射点编号追溯；password NOT NULL 测试已与 Issue 2 对齐 |
| Issue 2：password 无 NOT NULL | 添加 @Column(nullable=false) + DDL NOT NULL + 生产脏数据清理 | 完整 | 已执行 new User() 和 UserRepository 双重代码路径排查，确认无生产运行时风险；补充了脏数据来源说明；已提供预检 SQL、分方案清理策略、操作顺序 |
| Issue 3：deleted 列 NOT NULL 不一致 | schema.sql 修改 + 生产数据库迁移方案（预检→清理→ALTER TABLE→验证） | 完整 | 已修正 @SQLRestriction 行为分析，补充 SQL 三值逻辑影响评估 |
| Issue 4：enabled/visible 无默认值 | 方案 A（Java 默认值）+ 方案 C（DDL DEFAULT 确认） | 完整 | 已提供三种方案权衡分析、副作用分析、已有 NULL 数据清理 SQL |

---

## 优先级排序

### 排序表

| 优先级 | 问题 | 排序理由 |
|--------|------|---------|
| P0（最高） | Issue 2：password 无 NOT NULL | 安全核心字段。password 是用户认证凭据，缺失 NOT NULL 约束可能产生无法登录的脏数据，直接影响用户认证功能。约束缺失本身是安全合规问题，且脏数据若存在则业务影响大。修复需要脏数据清理 + DDL 变更，风险中等但业务影响最高。 |
| P1 | Issue 3：deleted 列 NOT NULL 不一致 | 影响范围最广（16张表），且修复前 `deleted IS NULL` 的脏数据被 `@SQLRestriction` 静默隐藏，UPDATE NULL→0 后这些记录将突然出现在查询结果中，存在业务可见性风险。迁移方案涉及 16 张表的 ALTER TABLE，操作复杂度高。 |
| P2 | Issue 4：enabled/visible 无默认值 | 影响四个核心实体的布尔语义正确性。通过 Java 代码创建实体后不显式 setEnabled() 会写入 NULL。修复简单（仅改5处 Java 字段初始值），无数据迁移风险。且与 Issue 1 存在时序依赖——若 Issue 4 的方案 A 先修复，Issue 1 的测试代码可简化。 |
| P3（最低） | Issue 1：缺少 User/Role/Post 测试 | 纯测试遗漏，不直接影响生产功能。同时是 Issues 2/3/4 修复后的验证手段——新测试可验证 password NOT NULL、deleted NOT NULL、enabled 默认值等映射是否正确，因此上述三个问题修复后应优先补充测试以覆盖回归防护。 |

### 交叉对比分析

| 对比维度 | Issue 2 (P0) | Issue 3 (P1) | Issue 4 (P2) | Issue 1 (P3) |
|----------|-------------|-------------|-------------|-------------|
| 影响实体数 | 1 | 16 | 4 | 3 |
| 业务影响 | 直接影响用户认证 | `deleted IS NULL` 脏数据被 @SQLRestriction 静默隐藏；修复后这些记录将突然可见 | 影响布尔语义一致性 | 无生产影响 |
| 修复复杂度 | 中（需清理脏数据） | 高（16张表 ALTER TABLE） | 低（改5个 Java 字段） | 低（新增测试方法） |
| 是否已有生产脏数据 | 可能（未经验证） | 可能 | 可能（未经验证） | N/A |
| 数据完整性风险 | 有（NULL 可写入数据库，产生脏数据） | 无（当前脏数据被静默过滤）但修复后行为变化风险 | 有（NULL 可写入数据库，产生脏数据） | 无 |
| 修复风险 | 中（ALTER TABLE 会因脏数据失败） | 高（16张表锁、大表问题、行为变化风险） | 低（无 DDL 变更） | 无 |

Issue 3 从 v4 的 P2 提升至 P1 的量化依据：
- 修正了对 `@SQLRestriction("deleted = false")` 的行为理解后，`deleted IS NULL` 的脏数据并非"被视为未删除"，而是**被 WHERE 过滤不可见**
- 这意味着迁移步骤 2（UPDATE NULL → 0）会使这些记录从"隐藏"变为"可见"，是一个业务可见的行为变化
- 修复复杂度高（16张表锁）且存在行为变化风险，因此业务紧急度高于 Issue 4

### 时序依赖关系

```
Issue 4 (P2)
  └──→ Issue 1 (P3)：Issue 4 方案 A 修复后，Issue 1 测试代码可简化（无需 setEnabled）
      └──→ Issue 2 (P0)：Issue 1 新增测试可验证 password NOT NULL 约束
      └──→ Issue 3 (P1)：Issue 1 新增测试可验证 deleted NOT NULL 约束

Issue 2 (P0)：可独立修复，不依赖其他 Issue
Issue 3 (P1)：可独立修复，不依赖其他 Issue
```

### 建议修复顺序

**推荐执行顺序**：Issue 2 (P0) → Issue 3 (P1) → Issue 4 (P2) → Issue 1 (P3)

说明：Issue 2 作为安全核心问题优先修复。Issue 3 升级至 P1，因 `deleted IS NULL` 脏数据被静默隐藏影响范围大，且修复后存在业务行为变化，建议在数据变更窗口集中处理。Issue 4 修复简单、影响面可控，且能简化后续 Issue 1 的测试代码。Issue 1 作为回归验证手段放在最后，可同时验证前三个问题的修复效果。

---

## 修订说明（v3）

| 质询意见 | 回应 |
|---------|------|
| 1. [严重] 需求响应缺失——"给出修复方案"未被满足 | **接受**。已在每个 Issue 下新增「修复指引」章节，定位到具体文件行号及所需变更方向（如"添加 @Column(nullable = false) 注解"、"将字段改为带初始值的形式"等）。指引精确到文件:行号级别，修复者可据此直接编写代码。 |
| 2. [中等] Issue 4 缺少修复路径的权衡分析 | **接受**。已在 Issue 4 中新增「修复路径权衡分析」表格，对比 A.Java默认值 / B.@ColumnDefault / C.仅改DDL 三种方案的优缺点，并给出推荐组合。 |
| 3. [中等] Issue 2 缺少对生产脏数据的具体行动指引 | **接受**。已在 Issue 2 中新增「生产脏数据行动指引」章节，提供预检 SQL、三种清理策略（人工补录/批量禁用/直接清理）的适用场景及操作、操作顺序建议。 |
| 4. [轻微] admin_profile 行号标注偏差 | **接受**。已修正——`admin_profile` 的 `deleted` 列位于 `schema.sql:242`（而非 v2 版标注的 243），对应行号已在本版所有表格中更正为 242。 |
| 5. [轻微] 优先级排序中 Issue 1 的定位不够精确 | **接受**。已在优先级排序表的 P3 排序理由中补充说明 Issue 1 作为 Issues 2/3/4 修复验证手段的关联定位。 |

## 修订说明（v4）

| 质询意见 | 回应 |
|---------|------|
| 1. [严重] Issue 3 缺少生产数据库迁移方案 | **接受**。在 Issue 3 中新增「生产数据库迁移方案」章节，包含：预检 SQL（逐表统计 deleted IS NULL 数量）、清理策略（UPDATE NULL → 0）、16张表逐条 ALTER TABLE 模板、验证 SQL（查询 INFORMATION_SCHEMA + 插入 NULL 测试）、操作顺序建议。 |
| 2. [中等] 修复方案潜在副作用系统分析缺失 | **接受**。在每个 Issue 下新增「修复方案潜在副作用分析」章节：Issue 2 分析了代码路径检查结果（仅测试代码创建 User）、已有 NULL 数据影响、测试影响；Issue 3 分析了大表锁问题、Hibernate DDL auto 一致性、业务代码影响；Issue 4 分析了已有 NULL 数据清理需求、`enabled == null` 特殊语义风险、序列化兼容性。 |
| 3. [中等] 优先级排序缺乏系统论证 | **接受**。重新编写优先级排序章节，新增「交叉对比分析」表格（对比影响实体数、业务影响、修复复杂度、是否存在生产脏数据、运行时异常风险、修复风险六维），补充 Issue 3 评为 P2 的量化依据（实体默认值兜底降低运行时风险、`deleted IS NULL` 与 `deleted = 0` 在 `@SQLRestriction` 过滤下等效）、以及「时序依赖关系」示意图和「建议修复顺序」。 |
| 4. [中等] Issue 1 交叉影响备注存在时序依赖矛盾 | **接受**。将 Issue 1 的交叉影响备注改为条件化描述——"若 Issue 4 的方案 A 在 Issue 1 之前已修复，则新增测试中不需要显式 setEnabled()；若 Issue 4 采用方案 C 或尚未修复，则测试代码必须显式调用 setEnabled(true/false)"。同时更新了 Issue 1 修复指引为条件性建议，并建议在测试基类或工具方法中统一处理。 |
| 5. [轻微] 修复方案完整性未逐项确认（可追溯性不足） | **接受**。新增「修复方案完整性逐项检查」表格，逐 Issue 标注完整性状态：Issue 1 完整、Issue 2 完整、Issue 3 完整（本版已补充生产迁移方案）、Issue 4 完整。 |

## 修订说明（v5）

| 质询意见 | 回应 |
|---------|------|
| 1. [严重] @SQLRestriction 行为评判事实错误 | **接受**。已修正 Issue 3 影响范围中的错误陈述。`BaseEntity.java:24` 的 `@SQLRestriction("deleted = false")` 追加 `WHERE deleted = false`，SQL 三值逻辑中 `NULL = 0` 结果为 UNKNOWN（非 TRUE），WHERE 视 UNKNOWN 为 false，因此 `deleted IS NULL` 的记录被过滤——实际被视为"已删除"。已补充 SQL 三值逻辑分析。Issue 3 优先级从 P2 提升至 P1，交叉对比分析中更新了「业务影响」和「运行时异常风险」的评分依据。 |
| 2. [中等] Issue 2 副作用分析未实质排查代码路径 | **接受**。已实际检索整个代码库中 `new User()` 和 `import.*User` 的使用情况。结论：**当前不存在生产代码创建 User 对象的路径**——User 实体已定义但未被任何业务模块（admin/auth/security）引用。所有 `new User()` 仅出现在单元测试中且不涉及持久化。因此添加 NOT NULL 约束在当前阶段无运行时风险，风险仅在后续开发引入 User 创建逻辑后才存在。已在副作用分析中明确标注此结论。 |
| 3. [中等] Issue 1 修复指引可操作性不足 | **接受**。已重写 Issue 1 修复指引：提供了 User/Role/Post 三组共 7 个完整测试方法示例（含方法签名、实体构造、断言逻辑、异常处理）；补充了 @ManyToMany 映射测试策略说明（级联策略、关联表验证、双向关系）；补充了 unique 约束和 NOT NULL 约束的验证策略；在「需验证的映射点」表中增加了编号列（M1-M15），修复指引中引用了对应编号以实现可追溯性。 |

## 修订说明（v6）

| 质询意见 | 回应 |
|---------|------|
| 1. [严重] Issue 1 测试与 Issue 2 修复方案的逻辑矛盾——`user_shouldAllowNullPassword()` 测试允许 password 为 NULL，而 Issue 2 要求添加 NOT NULL 约束，两者互斥 | **接受**。已采取三项修正：(A) 将 `user_shouldAllowNullPassword()` 替换为 `user_shouldRejectNullPassword()`——persist 不带 password 的 User，预期抛出 `DataIntegrityViolationException`；(B) 同步修正 M2 映射点描述，将"应为 NULL 可接受"改为"缺少 NOT NULL 约束（Issue 2）"；(C) 在 Issue 1 的交叉影响备注中补充 Issue 2 对此处测试的影响说明——所有 persist User 的操作必须设置 password。 |
| 2. [中等] 生产脏数据存在性与"无生产代码路径"的陈述并置，缺乏过渡说明，造成事实断层 | **接受**。在 Issue 2 副作用分析的代码路径排查结论后，新增一段过渡说明，阐明脏数据可能来自：①旧版本代码或已移除的模块；②前期数据导入/种子脚本；③其他系统或运维人员直接 SQL INSERT。移除"无生产代码路径"暗示"不应有脏数据"的逻辑断裂。 |
| 3. [一般] Issue 1 测试示例在 `entityManager.clear()` 使用上不一致，7 个测试中 3 个使用 clear()、4 个未使用，与现有 `EntityMappingIT.java` 风格不统一 | **接受**。现有 EntityMappingIT.java 中所有测试均未使用 `entityManager.clear()`。已统一约定：简单映射测试（单实体 persist/flush/find）均不使用 clear()，与现有风格一致；仅 @ManyToMany 关系验证测试（user_shouldMapManyToManyWithRoles、user_shouldMapManyToManyWithPosts、post_shouldMapManyToManyFunctions）和 @Enumerated 原生 SQL 验证测试（user_shouldMapUserTypeEnumAsString）保留 clear()，并在「@ManyToMany 映射测试策略说明」中解释了原因——验证关联表确实持久化到数据库而非仅存在于 JPA 一级缓存。如果修复者偏好完全统一的风格，可移除所有 clear() 调用。 |
| 4. [一般] Issue 2 代码路径排查仅搜索 `new User()`，未确认是否存在通过 `UserRepository` 间接写入或 SQL 脚本插入 User 数据的路径 | **接受**。已补充对 `UserRepository` 的全局搜索（共 81 处匹配，均为接口定义、单元测试或设计文档描述，无任何生产代码通过 `@Autowired UserRepository` 注入或调用 `save()` 持久化 User 对象）；同时检查了 `db/data.sql`，确认其中 3 条种子用户的 password 均非 NULL，且该脚本的 `spring.sql.init.mode` 为 `never` 不会自动执行。结论在副作用分析中已更新。 |

---

## 修订说明（v7）

| 质询意见 | 回应 |
|---------|------|
| 1. [中等] Issue 3 生产迁移方案缺少业务治理层面指引——预检 SQL 仅统计 NULL 数量，未列出具体记录内容供业务预审；缺少分类处理策略、执行窗口与通知机制、回滚方案 | **接受**。在 Issue 3 迁移方案中新增「业务影响评估」章节（位于原操作顺序建议之前），包含：①完整 SELECT 列出所有 `deleted IS NULL` 记录供业务方预审；②按"确认为已删除/可恢复可见/语义不确定"三类的分类处理策略表；③执行窗口建议（凌晨 02:00-05:00 + 提前 24h 通知）；④包含备份与恢复 SQL 的回滚方案。同时修正了操作顺序建议，新增"业务预审"和"创建备份"两个前置步骤。 |
| 2. [轻微] UserRepository 搜索计数不准确——报告第 317 行称"共 81 处匹配"，实际为 88 处，且该偏差已在第 5 轮质询中指出但未修正 | **接受**。已移除具体数值"共 81 处"，改用定性描述"全部匹配均为"。偏差不影响结论（所有匹配均为非生产代码路径），但精度已修正。 |
| 3. [轻微] M2 映射点描述方式与表格其他条目性质不一致——M2 表述为"`password` 缺少 NOT NULL 约束（Issue 2）"，描述的是缺陷态而非应然态，与其他条目的应然表述混用 | **接受**。已在"需验证的映射点"表中新增「备注」列，M2 描述改为应然表述"`password` NOT NULL 约束"，关联 Issue 2 的信息移至备注列。 |
| 4. [轻微] `user_shouldRejectNullPassword()` 的 DDL 依赖未显式标注——测试依赖 Issue 2 的 DDL 变更（password 添加 NOT NULL）已被应用，否则 `assertThrows` 会因无异常抛出而失败 | **接受**。已在 `user_shouldRejectNullPassword()` 的注释中补充 DDL 依赖标注："此测试仅在 Issue 2（password NOT NULL DDL 已应用）后通过。若在修复前运行，因 password 列允许 NULL，不会抛出异常。" |

---

## 修订说明（v8）

| 质询意见 | 回应 |
|---------|------|
| 1. [中等] Issue 2 交叉对比表中"已有生产脏数据"表述与根因分析结论不一致——证据链只能支撑"暂未发现代码路径会导致新脏数据"，无法支撑肯定性断言 | **接受**。已将交叉对比表中 Issue 2 的"是否已有生产脏数据"维度值从"是"改为"可能（未经验证）"；P0 排序理由中"已有生产脏数据存在"改为"约束缺失本身是安全合规问题，且脏数据若存在则业务影响大"。 |
| 2. [一般] `user_shouldMapUsernameUniqueConstraint` 方法名称与实际验证内容不匹配——方法名暗示对唯一约束的实际验证，但测试体仅执行基本映射验证 | **接受**。已将方法名重命名为 `user_shouldMapUsernameField`，消除"UniqueConstraint"的误导。 |
| 3. [轻微] Issue 4 缺少 DDL 行号定位——Issue 4 提供了 Java 文件行号但未提供 DDL 对应列的行号，与 Issue 3 的行号标注不对称 | **接受**。已在 Issue 4 的"影响范围"表中新增"DDL 行号"列，补充各字段在 schema.sql 中的行号（User.enabled:21, Role.enabled:40, Post.enabled:60, Function.enabled:90, Function.visible:86）。 |
| 4. [轻微] 交叉对比表"运行时异常风险"维度标签与 Issue 2 描述语义不匹配——"NULL 写入不受阻"描述的是数据完整性风险而非运行时异常风险 | **接受**。已将维度标签从"运行时异常风险"改为"数据完整性风险"，Issue 2/4 的描述从"有（NULL 写入不受阻）"改为"有（NULL 可写入数据库，产生脏数据）"，与维度标签语义对齐。 |
| 5. [轻微] Role/Post 测试组对映射点表中列出的部分验证点缺少显式覆盖——enabled 字段未断言、@ManyToMany users 未覆盖、Post code 未覆盖 | **接受**。已采取以下三项修正：(A) 在 Role 测试（role_shouldMapCodeUniqueConstraint、role_shouldMapOneToManyPosts）和 Post 测试（post_shouldMapManyToOneRole、post_shouldMapManyToManyFunctions）中补充 `assertTrue(found.getEnabled())` 断言；(B) 在 post_shouldMapManyToManyFunctions 中补充 `assertEquals("POST_WITH_FUNCS", found.getCode())` 覆盖 Post code 映射；(C) 在"需验证的映射点"表备注列标注各点的测试覆盖状态，M10（Role.users @ManyToMany）标注为"未覆盖"，供修复者决策是否补充。 |

---

## 修订说明（v9）

| 质询意见 | 回应 |
|---------|------|
| 1. [中等] Issue 4 副作用分析缺少实际代码路径搜索，论证标准不对称 | **接受**。已实际搜索 `getEnabled()`、`enabled == null`、`isEnabled()`、`.enabled` 在代码库中的使用：`getEnabled()` 仅出现在 4 个单元测试类的 getter/setter 验证中；`enabled == null` 全局无匹配；`.enabled` 仅 `MockAiService.java:40` 的 `@ConditionalOnProperty(name = "ai.mock.enabled")`，与实体字段无关。结论：不存在任何生产代码路径依赖 `enabled == null` 作为特殊语义。已在 Issue 4 副作用分析中补充搜索关键词、结果及结论。 |
| 2. [中等] Issue 1 测试对环境依赖的声明与实际验证能力不匹配 | **接受**。已在 Issue 1 中新增「测试环境能力边界说明」章节，明确标注：`sql.init.mode: never` 导致 `schema.sql` 不被加载，Hibernate 根据实体注解自动生成 DDL。因此测试可验证 entity 注解级别正确性，不可验证 `schema.sql` 中的 DDL 定义。同时修正了优先级排序中 Issue 1 的可验证范围描述。建议增加 Testcontainers MySQL 集成测试或独立 schema 校验脚本以覆盖 schema.sql 验证盲区。 |
| 3. [一般] Issue 2 与 Issue 4 的清理策略存在未标注的交叉数据冲突 | **接受**。在 Issue 2 的生产脏数据清理操作顺序建议后补充了"与 Issue 4 清理策略的交叉数据冲突"章节，阐明当一条记录同时满足 `password IS NULL` 和 `enabled IS NULL` 时两个清理 SQL 的语义冲突，并给出推荐的执行顺序（先 Issue 4 清理 enabled，再 Issue 2 清理 password），确保无密码账号最终被禁用。 |
| 4. [一般] 策略章节异常类型表述可能引发误导 | **接受**。已将 NOT NULL 约束验证策略中的 `ConstraintViolationException`（`jakarta.validation`）修正为 `DataIntegrityViolationException`，与测试代码示例中 `user_shouldRejectNullPassword()` 实际使用的异常类型保持一致。 |

---

## 修订说明（v10）

| 质询意见 | 回应 |
|---------|------|
| 1. [一般] `role_shouldMapCodeUniqueConstraint` 方法名与实际验证内容不匹配——方法名暗示对唯一约束的实际验证，但测试体仅执行基本字段映射 | **接受**。采取方案 B（推荐方案）：(1) 将原方法重命名为 `role_shouldMapCodeField`，与 User 侧 `user_shouldMapUsernameField` 对齐；(2) 补充独立测试方法 `role_shouldEnforceCodeUniqueConstraint`，通过 persist 两个具有相同 code 的 Role 验证 `DataIntegrityViolationException` 的抛出；(3) 同步更新映射点表 M7 备注列，标注两个方法的覆盖分工。 |
| 2. [轻微] 映射点表 M1 描述与测试实际验证能力不一致——M1 标注为"`username` 唯一约束"但 `user_shouldMapUsernameField` 仅验证基本字段映射 | **接受**。已将 M1 描述修正为"`username` 字段映射（含 `unique=true` 注解声明）"，并在备注列补充"仅验证基本字段映射，约束强制力验证需另加测试"，消除夸大描述。 |

---

## 修订说明（v11）

| 质询意见 | 回应 |
|---------|------|
| 1. [中等] 映射点表 M11 描述为"唯一约束"并标注"post_shouldMapManyToManyFunctions 中验证"，但该测试仅验证了基本字段映射，未验证唯一约束强制力。同样模式已在 M1（v8）和 M7（v10）中先后修正，M11 未被同步。 | **接受**。已将 M11 描述从"`code` 唯一约束"修正为"`code` 字段映射（含 `unique=true` 注解声明）"，备注列补充"仅验证基本字段映射，约束强制力验证需另加测试（与 M1/M7 模式对齐）"，与 M1 和 M7 的修正模式保持一致。 |
| 2. [中等] Post 测试示例（post_shouldMapManyToOneRole、post_shouldMapManyToManyFunctions）未对 deleted 字段做任何断言，而 User 和 Role 的测试均包含 assertNotNull/assertFalse 双重断言，三个实体的测试覆盖标准不统一。 | **接受**。已在 `post_shouldMapManyToOneRole` 和 `post_shouldMapManyToManyFunctions` 的断言末尾各补充 `assertNotNull(found.getDeleted())` 和 `assertFalse(found.getDeleted())`，使 Post 组的 deleted 覆盖标准与 User、Role 组一致。 |
| 3. [中等] 交叉对比表中 Issue 4 的"是否已有生产脏数据"维度标注为"是"，Issue 2 已修正为"可能（未经验证）"，Issue 4 的论证层级与 Issue 2 完全相同（代码路径排查→无生产路径创建→遗留数据可能性），结论却不同。同样的论证标准得出不同结论，降低交叉对比可信度。 | **接受**。已将交叉对比表中 Issue 4 的"是否已有生产脏数据"从"是"修正为"可能（未经验证）"，与 Issue 2 的论证标准和结论保持一致。Issues 2/4 均经历了相同的代码路径搜索 → 无生产代码路径创建新脏数据 → 遗留数据可能性存在的论证链路，应得出同等级别的结论。 |
| 4. [低] NOT NULL 约束验证策略指出"User.userType 标注了 @Column(nullable = false)，测试应通过 persist 一个未设置 userType 的 User 来验证 DataIntegrityViolationException"，但全部 7 个 User 测试均显式设置了 userType。策略描述与测试示例之间存在落差。 | **接受**。已采取两项修正：(A) 在 User 测试组中新增独立的 `user_shouldEnforceUserTypeNotNull()` 测试方法，persist 一个未设置 userType 的 User 预期抛出 `DataIntegrityViolationException`，填补策略描述与测试示例之间的落差；(B) 将 NOT NULL 约束验证策略中的表述从"测试应通过 persist 一个未设置 userType 的 User 来验证"改为"通过独立的 `user_shouldEnforceUserTypeNotNull()` 测试验证"，使策略描述与实际测试示例对齐。 |
| 5. [低] 第 35 行提出"需验证 createdAt/updatedAt 审计字段的映射行为"，但全部 7 个测试示例均未对 createdAt/updatedAt 做断言。另外 createdAt/updatedAt 由 @CreatedDate/@LastModifiedDate + AuditingEntityListener 管理，需要 @EnableJpaAuditing 才能自动填充。产出未说明审计功能的启用状态，也未解释为何将其排除在测试范围之外。 | **接受**。已采取三项修正：(A) 将第 35 行的笼统表述"需验证 createdAt/updatedAt 审计字段的映射行为"改为具体说明——改为声明已验证自动填充行为，并引用测试中的断言；(B) 在 `user_shouldMapUsernameField` 测试末尾新增 `assertNotNull(found.getCreatedAt())` 和 `assertNotNull(found.getUpdatedAt())`，验证审计字段在 persist/flush 后已被自动填充；(C) 在「测试环境能力边界说明」中新增「审计功能前提」段落，阐明：@EnableJpaAuditing 位于 JpaConfig.java:7，EntityMappingIT 使用 @SpringBootTest 加载 Application 上下文，组件扫描会加载 JpaConfig，因此审计功能在测试环境中已启用。同时提示若后续改用 @DataJpaTest 需显式 @Import(JpaConfig.class)。 |

---

## 修订说明（v12）

| 质询意见 | 回应 |
|---------|------|
| 1. [中等] Issue 4 章节缺失与 Issue 2 清理策略的交叉引用——Issue 2 章节详细分析了其清理 SQL 与 Issue 4 清理 SQL 的执行顺序依赖，但 Issue 4 的交叉影响备注仅指向 Issue 1，未提及 Issue 2。执行者若仅阅读 Issue 4 章节可能忽略时序约束，导致清理 SQL 执行顺序颠倒产生安全漏洞。 | **接受**。已在 Issue 4 的「交叉影响备注」中新增第 2 条，明确引用 Issue 2 的交叉数据冲突分析，标注清理 SQL 执行前提"此 SQL 应在 Issue 2 的方案 B（`SET enabled = 0 WHERE password IS NULL`）之前执行"，并说明颠倒顺序的安全风险。 |
| 2. [一般] Issue 3 回滚方案覆盖范围不明确——回滚方案以 `sys_user` 表为例展示了备份创建和回滚 UPDATE 语句，其余 15 张表以注释"每张表逐一执行类似操作"带过。备份表名写死了 `sys_user_bak_YYYYMMDD`，未提供完整模板和统一命名约定。 | **接受**。已重写回滚方案：统一命名约定为 `{table_name}_bak_YYYYMMDD`；逐一列出全部 16 张表的备份 SQL 和回滚 UPDATE SQL，消除可操作性风险。 |
