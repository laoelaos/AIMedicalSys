# 详细设计（v2）

## 概述

完成 Phase 1A 全部剩余代码修复和测试补充，修复测试基础设施（common-module-impl 缺少 H2 依赖），对齐 schema.sql 中 16 张表的 deleted 列定义与 BaseEntity.java 的 `@Column(nullable = false)` 注解，为 User/Role/Post/Function 的 enabled/visible 字段添加 Java 默认初始值，并补充 User/Role/Post 的集成测试方法。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/common-module/common-module-impl/pom.xml` | 修改 | 新增 H2 依赖（runtime scope），解除测试基础设施阻塞 |
| `AIMedical/backend/application/src/main/resources/db/schema.sql` | 修改 | 16 张表 deleted 列 `DEFAULT 0` → `NOT NULL DEFAULT 0`，对齐 BaseEntity |
| `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/User.java` | 修改 | `enabled` 字段添加 `= true` 默认值 |
| `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/Role.java` | 修改 | `enabled` 字段添加 `= true` 默认值 |
| `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/Post.java` | 修改 | `enabled` 字段添加 `= true` 默认值 |
| `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/Function.java` | 修改 | `enabled` 和 `visible` 字段添加 `= true` 默认值 |
| `AIMedical/backend/integration/src/test/java/com/aimedical/integration/EntityMappingIT.java` | 修改 | 新增 import + 9 个测试方法（User 5 + Role 3 + Post 2） |

## 变更详解

### 1. H2 依赖（测试基础设施修复）

**目标 pom.xml 变更**：在 `spring-boot-starter-test` 之后追加：

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

**说明**：root pom.xml `dependencyManagement` 已声明 h2 artifact（version 2.2.224），子模块只需声明 groupId 和 artifactId，无需指定版本。`runtime` scope 满足测试运行时 classpath 需求。

### 2. schema.sql：16 张表 deleted 列添加 `NOT NULL`

下列表列号与行号的对应关系基于当前文件。所有变更模式为 `\`deleted\` TINYINT(1) DEFAULT 0 COMMENT '...'` → `\`deleted\` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '...'`（COMMENT 子句内容不变）：

| # | 表名 | 行号 | COMMENT |
|---|------|------|---------|
| 1 | sys_user | 25 | 逻辑删除 |
| 2 | sys_role | 44 | 逻辑删除 |
| 3 | sys_post | 65 | 逻辑删除 |
| 4 | sys_function | 94 | 逻辑删除 |
| 5 | sys_dict_type | 113 | 逻辑删除 |
| 6 | sys_dict_data | 135 | 逻辑删除 |
| 7 | patient_profile | 197 | 逻辑删除 |
| 8 | doctor_profile | 222 | 逻辑删除 |
| 9 | admin_profile | 242 | 逻辑删除 |
| 10 | health_profile | 263 | 逻辑删除 |
| 11 | allergy_history | 284 | 逻辑删除 |
| 12 | chronic_disease | 303 | 逻辑删除 |
| 13 | family_history | 322 | 逻辑删除 |
| 14 | surgery_history | 341 | 逻辑删除 |
| 15 | medication_history | 361 | 逻辑删除 |
| 16 | sys_token | 422 | 逻辑删除 |

**对齐依据**：BaseEntity.java:37-38 声明 `@Column(nullable = false) private Boolean deleted = false;`，数据库 DDL 应与实体注解一致。

### 3. Java 默认值（Issue 4）

| 文件 | 行号 | 字段 | 当前 | 修改为 |
|------|------|------|------|--------|
| User.java | 37 | `enabled` | `private Boolean enabled;` | `private Boolean enabled = true;` |
| Role.java | 28 | `enabled` | `private Boolean enabled;` | `private Boolean enabled = true;` |
| Post.java | 30 | `enabled` | `private Boolean enabled;` | `private Boolean enabled = true;` |
| Function.java | 30 | `enabled` | `private Boolean enabled;` | `private Boolean enabled = true;` |
| Function.java | 54 | `visible` | `private Boolean visible;` | `private Boolean visible = true;` |

**效果**：JPA 在 new 实体对象时这些字段不再为 null，消除 Java 端 null 写入数据库列的路径。不涉及新增 import，无需其他文件改动。

### 4. EntityMappingIT 补充测试（Issue 1）

**新增 import**：在文件头部已有 import 之后追加：

```java
import com.aimedical.modules.commonmodule.permission.Role;
import com.aimedical.modules.commonmodule.permission.Post;
import com.aimedical.modules.commonmodule.api.UserType;
import java.util.Set;
```

**新增测试方法**（共 9 个，追加在 `patientWithHealthProfileAndAllergy_shouldWorkTogether` 之后）：

**User 测试组（5 个）**：

| 方法名 | 验证点 |
|--------|--------|
| `user_shouldMapUsernameField` | 正向：设置 username/password/userType/enabled → persist/flush → 查询断言各字段映射正确，校验 deleted 为 false、createdAt/updatedAt 非空 |
| `user_shouldEnforceUserTypeNotNull` | 错误路径：不设 userType → persist + flush 抛出 DataIntegrityViolationException |
| `user_shouldMapManyToManyWithRoles` | 关系映射：创建 Role → persist → 创建 User 关联 roles(Set.of(role)) → persist → clear → 查询断言 roles 大小 1，code 匹配 |
| `user_shouldMapManyToManyWithPosts` | 关系映射：创建 Post → persist → 创建 User 关联 posts(Set.of(post)) → persist → clear → 查询断言 posts 大小 1，code 匹配 |
| `user_shouldMapUserTypeEnumAsString` | 枚举存储：设置 UserType.PATIENT → persist → clear → 原生查询 `SELECT user_type` 断言存储值为 "PATIENT"；find 查询断言枚举反序列化正确 |

**Role 测试组（3 个）**：

| 方法名 | 验证点 |
|--------|--------|
| `role_shouldMapCodeField` | 正向：设置 code/name/enabled → persist → 查询断言 code/enabled/deleted 正确 |
| `role_shouldEnforceCodeUniqueConstraint` | 约束验证：新建 2 个同名 code 的 Role → 第二个 persist + flush 抛出 DataIntegrityViolationException |
| `role_shouldMapOneToManyPosts` | 关系映射：创建 Role → persist → 创建 Post 关联 role → persist → 查询断言 Role.posts 含该 Post |

**Post 测试组（2 个）**：

| 方法名 | 验证点 |
|--------|--------|
| `post_shouldMapManyToOneRole` | 关系映射：创建 Role → persist → 创建 Post 关联 role + sort → persist → 查询断言 Post.role.code 匹配、sort 正确、deleted 为 false |
| `post_shouldMapManyToManyFunctions` | 关系映射：创建 Function(type=BUTTON) → persist → 创建 Post 关联 functions(Set.of(function)) → persist → clear → 查询断言 Post.functions 大小 1，code 匹配 |

**测试模式**：所有方法复用现有 `@SpringBootTest + @AutoConfigureTestDatabase + @Transactional` 上下文，通过 `entityManager.persist + flush + find` 完成持久化和查询验证。

## 执行顺序

为避免中间状态导致测试失败，变更应按以下顺序实施：

1. **common-module-impl/pom.xml** → 添加 H2 依赖（解除测试基础设施阻塞）
2. **schema.sql** → 16 张表 deleted 列加 NOT NULL（DDL 对齐）
3. **User.java / Role.java / Post.java / Function.java** → 添加 `= true` 默认值（Issue 4）
4. **EntityMappingIT.java** → 新增 import + 9 个测试方法（Issue 1）

## 行为契约

### 变更 1：H2 依赖
- **前置**：root pom.xml dependencyManagement 已声明 h2
- **后置**：common-module-impl 测试类路径包含 h2 驱动，`@DataJpaTest` / `@AutoConfigureTestDatabase` 可成功创建嵌入式 H2 DataSource

### 变更 2：schema.sql deleted NOT NULL
- **后置**：16 张表的 deleted 列在从头建库时变为 NOT NULL DEFAULT 0，与 BaseEntity.java 注解一致

### 变更 3：Java 默认值
- **后置**：`new User()` / `new Role()` / `new Post()` / `new Function()` 后 enabled 为 true（非 null）；`new Function()` 后 visible 为 true（非 null）
- **无影响**：代码库中无生产代码依赖 enabled == null 的特殊语义

### 变更 4：EntityMappingIT 测试
- **前置**：变更 1（H2 依赖）+ 变更 3（Java 默认值）必须先实施完毕
- **后置**：EntityMappingIT 总测试数由 8（原有）+ 2（v1 新增）= 10 个变为 10 + 9 = 19 个
- **验证命令**：`mvn test -pl integration -am`
- **期待结果**：19 个测试全部通过

## 依赖关系

| 依赖类型 | 说明 |
|---------|------|
| 变更 1 → 变更 4 | EntityMappingIT 依赖 H2 运行时 classpath |
| 变更 3 → 变更 4 | 新增测试方法使用 enabled = true 默认值，依赖 Java 默认值生效 |
| 变更 2 | 独立变更，不依赖其他变更 |
| BaseEntity.deleted (nullable = false) | 变更 2 的对齐依据 |
| `com.aimedical.common.base.MenuType` | `post_shouldMapManyToManyFunctions` 的 Function.type 枚举需要 |
| `com.aimedical.modules.commonmodule.api.UserType` | `user_shouldMapUserTypeEnumAsString` 和 `user_shouldEnforceUserTypeNotNull` 需要，需新增 import |
| `jakarta.persistence.Column` / `java.util.Set` | 现有测试中已使用，Set 需新增 import |
