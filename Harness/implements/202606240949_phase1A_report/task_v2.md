# 任务指令（v2）

## 动作
NEW

## 任务描述
完成 Phase 1A 全部剩余代码修复和测试补充，包括：修复测试基础设施（添加 H2 依赖）、Issue 3（schema.sql deleted NOT NULL）、Issue 4（Java 默认值）、Issue 1（EntityMappingIT 集成测试）。

### 具体变更清单

#### 1. 修复测试基础设施
- **文件**: `AIMedical/backend/modules/common-module/common-module-impl/pom.xml`
- **操作**: 在 `spring-boot-starter-test` 依赖之后添加 H2 依赖（test scope）
- **代码**:
  ```xml
  <dependency>
      <groupId>com.h2database</groupId>
      <artifactId>h2</artifactId>
      <scope>runtime</scope>
  </dependency>
  ```

#### 2. Issue 3：schema.sql 16 张表 deleted 列添加 NOT NULL
- **文件**: `AIMedical/backend/application/src/main/resources/db/schema.sql`
- **操作**: 将以下 16 张表的 `` `deleted` TINYINT(1) DEFAULT 0 `` 改为 `` `deleted` TINYINT(1) NOT NULL DEFAULT 0 ``（注意保留 COMMENT 子句不变）
- **涉及表与行号**:
  | 表名 | 当前行 | 修改后 |
  |------|-------|--------|
  | sys_user | 25 | `` `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除', `` |
  | sys_role | 44 | 同上 |
  | sys_post | 65 | 同上 |
  | sys_function | 94 | 同上 |
  | sys_dict_type | 113 | 同上 |
  | sys_dict_data | 135 | 同上 |
  | patient_profile | 197 | 同上 |
  | doctor_profile | 222 | 同上 |
  | admin_profile | 242 | 同上 |
  | health_profile | 263 | 同上 |
  | allergy_history | 284 | 同上 |
  | chronic_disease | 303 | 同上 |
  | family_history | 322 | 同上 |
  | surgery_history | 341 | 同上 |
  | medication_history | 361 | 同上 |
  | sys_token | 422 | 同上 |

#### 3. Issue 4：enabled/visible Java 默认值
- **文件与修改**:
  - `User.java:37`: `private Boolean enabled;` → `private Boolean enabled = true;`
  - `Role.java:28`: `private Boolean enabled;` → `private Boolean enabled = true;`
  - `Post.java:30`: `private Boolean enabled;` → `private Boolean enabled = true;`
  - `Function.java:30`: `private Boolean enabled;` → `private Boolean enabled = true;`
  - `Function.java:54`: `private Boolean visible;` → `private Boolean visible = true;`

#### 4. Issue 1：EntityMappingIT 补充 User/Role/Post 集成测试
- **文件**: `AIMedical/backend/integration/src/test/java/com/aimedical/integration/EntityMappingIT.java`
- **操作**: 在文件末尾（最后一个测试方法 `patientWithHealthProfileAndAllergy_shouldWorkTogether` 之后）追加完整的 User/Role/Post 测试方法组
- **需新增的 import**:
  ```java
  import com.aimedical.modules.commonmodule.permission.Role;
  import com.aimedical.modules.commonmodule.permission.Post;
  import com.aimedical.modules.commonmodule.api.UserType;
  import java.util.Set;
  ```
- **需新增的测试方法**（共 9 个）:

  **--- User 测试组 (5 个，补充现有 2 个) ---**

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
  void user_shouldEnforceUserTypeNotNull() {
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

  **--- Role 测试组 (3 个) ---**

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

  **--- Post 测试组 (2 个) ---**

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

## 选择理由
v1 已修复 Issue 2（password NOT NULL）并通过编译。验证失败是因 pre-existing 测试基础设施问题（common-module-impl 缺少 H2 依赖），而非修复代码本身问题。v2 将所有剩余变更整合为单一任务：先修复 H2 依赖解除测试阻塞，再依次完成 Issue 3（DDL 对齐）、Issue 4（Java 默认值）、Issue 1（集成测试），确保一次交付即可通过完整验证。

## 任务上下文
- **测试基础设施问题**：common-module-impl/pom.xml 未声明 h2 依赖。root pom.xml 第 35 行已定义 `<h2.version>2.2.224</h2.version>` 且在 dependencyManagement 中声明了 h2 artifact，子模块只需添加 `<groupId>com.h2database</groupId><artifactId>h2</artifactId>` 即可无需指定版本。
- **Issue 3 变更**：16 张表的 deleted 列定义均使用 `DEFAULT 0` 而缺失 `NOT NULL`（BaseEntity.java 已标注 `@Column(nullable = false)`），变更为 `NOT NULL DEFAULT 0`，COMMENT 子句保持不变。
- **Issue 4 变更**：5 个字段改为带初始值声明 `= true`，消除 Java 端 null 写入路径。诊断报告确认代码库中无生产代码依赖 `enabled == null` 作为特殊语义。
- **Issue 1 变更**：EntityMappingIT.java 已导入 User（第8行），需补充导入 Role、Post、UserType、Set。测试方法遵循"创建→persist→flush→find→断言"模式，与现有测试风格一致。现有测试类已标注 `@SpringBootTest(classes = com.aimedical.Application.class)` + `@AutoConfigureTestDatabase` + `@Transactional`，新增方法复用此上下文。
- **现有 EntityMappingIT 中已有 v1 添加的 2 个测试**：`user_shouldPersistWithPassword`（第244-255行）、`user_shouldRejectNullPassword`（第257-266行）。

## 验证方式
- 编译验证：`mvn compile -pl modules/common-module/common-module-impl -am`
- 集成测试：`mvn test -pl integration -am`
- 期待结果：common-module-impl 的 UserRepositoryTest 全部通过 + EntityMappingIT 全部测试通过（原有 8 个 + v1 新增 2 个 + v2 新增 9 个 = 19 个）
