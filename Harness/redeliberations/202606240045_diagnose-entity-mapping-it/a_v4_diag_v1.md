# 诊断报告：EntityMappingIT 实体映射集成测试问题

## 1. EntityMappingIT 缺少 User/Role/Post 集成测试

### 现象

`EntityMappingIT.java`（`AIMedical\backend\integration\src\test\java\com\aimedical\integration\EntityMappingIT.java`）包含了多组 JPA 实体映射验证测试（AllergyHistory、HealthProfile、PatientEntity、DoctorEntity、Function、DictData/DictType、TokenStore），但完全没有覆盖 Phase 1 包A 的核心实体 `User`、`Role`、`Post`。

### 根因

`User.java`（`common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/User.java`）、`Role.java`（同包）、`Post.java`（同包）均已存在，且 `integration/pom.xml` 第53-56行已声明 `common-module-impl` 为 test 依赖，因此在集成测试模块中完全可访问。

此问题是**测试遗漏**，不是代码问题。测试类在编写时只覆盖了此前报告过有映射问题的实体，未将 Phase 1 核心实体纳入。

### 需验证的映射点

| 实体 | 需验证的映射点 | 关键代码位置 |
|------|---------------|-------------|
| User | `username` 唯一约束（`unique=true`）、`password` 无 NOT NULL、`userType` 枚举 @Enumerated(EnumType.STRING)、`roles`/`posts` @ManyToMany 关联表 | User.java:25-46 |
| Role | `code` 唯一约束、`enabled` 无默认值、`posts` @OneToMany、`users` @ManyToMany | Role.java:21-37 |
| Post | `code` 唯一约束、`enabled` 无默认值、`role` @ManyToOne / `functions` @ManyToMany、`sort` 字段 | Post.java:23-45 |

实体均继承 `BaseEntity`，需验证 `id` 自增、`deleted` NOT NULL、`createdAt`/`updatedAt` 审计字段的映射行为。

**交叉影响备注**：User.enabled、Role.enabled、Post.enabled 均无 Java 默认值（见 Issue 4）。依赖 Issue 4 的修复时序：若 Issue 4 的方案 A（Java 默认值）在 Issue 1 之前已修复，则新增测试中不需要显式 setEnabled()，因 `enabled = true` 默认值会生效；若 Issue 4 采用方案 C（仅改 DDL）或尚未修复，则测试代码必须在构建实体时显式调用 `setEnabled(true/false)`，否则 `enabled` 列将插入 NULL。

### 修复指引

在 `EntityMappingIT.java` 中新增 User、Role、Post 三组集成测试方法，每组遵循「创建实体→persist→flush→find→断言」模式，覆盖上述需验证的映射点。新增测试中是否需要显式 setEnabled() 取决于 Issue 4 的修复时序和选型（见交叉影响备注），建议在测试基类或工具方法中统一处理以确保可维护性。

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

- **代码路径检查**：common-module-impl 中仅单元测试代码创建 User 实例（UserTest.java），未发现生产代码中直接调用 `new User()` 的路径。但 User 实体的实际创建逻辑位于其他模块（admin 模块、注册流程等），需确认那些模块中创建 User 后是否均设置了 password。若存在未设置 password 就 persist 的路径，修改后将在运行时抛出 `DataIntegrityViolationException`。
- **已有 NULL 数据**：生产数据库中已存在 `password IS NULL` 的记录（见下方预检 SQL），若不先清理，ALTER TABLE 操作将因现有 NULL 值而失败。
- **测试影响**：UserTest.java 中除 `shouldSetAndGetPassword`（第30行）外，其他测试方法创建 User 后均未设置 password。当前这些测试仅验证 getter/setter、不涉及持久化，因此不受影响。一旦后续在这些测试中增加持久化操作，将触发约束异常。

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

根据业务含义，`deleted IS NULL` 应等同于 `deleted = 0`（未删除）：

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

**操作顺序建议**：①预检确认脏数据 → ②UPDATE 清理（步骤 2）→ ③逐表或分批执行 ALTER TABLE（步骤 3，建议在低峰期执行）→ ④验证约束生效（步骤 4）→ ⑤更新 schema.sql 文件

### 修复方案潜在副作用分析

- **已有 NULL 数据若不清理**：ALTER TABLE 将直接失败，无法添加 NOT NULL 约束。
- **大表锁问题**：ALTER TABLE MODIFY COLUMN 在 MySQL 中可能引发表锁或重建表。对于大表（如 sys_user），建议在低峰期执行，或使用 pt-online-schema-change 等工具减少锁时间。
- **Hibernate DDL auto 一致性**：使用 Hibernate DDL auto 生成的数据库会自动带 NOT NULL（因实体标注了 `nullable = false`），与修改后的手工 DDL 一致。此变更是对齐两者，不会引入新不一致。
- **业务代码影响**：BaseEntity 已设置 `deleted = false`，Java 层面新创建的对象不会出现 NULL。业务代码中若存在 SQL 直接 INSERT 且未指定 deleted 列，DEFAULT 0 将生效（MODIFY 不改变 DEFAULT 逻辑），无影响。

### 影响范围

- 使用 Hibernate DDL auto 生成的数据库会自动添加 NOT NULL（因实体标注了 `nullable = false`），与手工 DDL 不一致
- 手工 DDL 创建的数据库可能包含 `deleted IS NULL` 的脏数据
- 由于实体代码设置了 `deleted = false` 作为默认值，Java 层面新创建的对象不会出现 NULL，但历史 SQL 插入或直接数据库操作可能产生 NULL

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

| 实体 | 字段 | 文件行号 | Java 默认值 | DDL 默认值 |
|------|------|---------|------------|-----------|
| User | enabled | User.java:36 | null | DEFAULT 1 |
| Role | enabled | Role.java:28 | null | DEFAULT 1 |
| Post | enabled | Post.java:30 | null | DEFAULT 1 |
| Function | enabled | Function.java:30 | null | DEFAULT 1 |
| Function | visible | Function.java:54 | null | DEFAULT 1 |

实际行为：通过 Java 代码创建实体而不设置 `enabled`，Hibernate 会插入 NULL；通过 SQL 直接插入不指定 `enabled`，数据库使用 DEFAULT 1。两种路径行为不一致，可能导致通过不同入口创建的数据在布尔语义上出现差异。

**交叉影响备注**：详见 Issue 1 的交叉影响备注——Issue 4 的修复选型和时序直接影响 Issue 1 新增测试中是否需要显式 setEnabled()。

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
- **对现有业务逻辑的潜在影响**：如果现有代码依赖 `enabled == null` 作为某种特殊语义（如"未设置"状态），增加默认值后会消除这种区分。需确认现有业务代码中不存在对 `enabled == null` 的特殊判断逻辑。
- **序列化/反序列化兼容性**：如果实体被序列化（如 Redis 缓存、JSON 响应），修改前缓存中的旧数据可能不包含 enabled 字段，反序列化后获取 Java 默认值 `true`，与旧行为（null）不一致。建议在修改后清除相关缓存。

---

## 修复方案完整性逐项检查

| Issue | 修复方案摘要 | 完整性 | 备注 |
|-------|------------|--------|------|
| Issue 1：缺少 User/Role/Post 测试 | 在 EntityMappingIT.java 中新增三组集成测试方法 | 完整 | 已明确测试模式、覆盖点、文件行号，有条件性依赖 Issue 4 |
| Issue 2：password 无 NOT NULL | 添加 @Column(nullable=false) + DDL NOT NULL + 生产脏数据清理 | 完整 | 已提供预检 SQL、分方案清理策略、操作顺序 |
| Issue 3：deleted 列 NOT NULL 不一致 | schema.sql 修改 + 生产数据库迁移方案（预检→清理→ALTER TABLE→验证） | 完整（本版已补充） | 本版补充了生产迁移方案、副作用分析 |
| Issue 4：enabled/visible 无默认值 | 方案 A（Java 默认值）+ 方案 C（DDL DEFAULT 确认） | 完整 | 已提供三种方案权衡分析、副作用分析、已有 NULL 数据清理 SQL |

---

## 优先级排序

### 排序表

| 优先级 | 问题 | 排序理由 |
|--------|------|---------|
| P0（最高） | Issue 2：password 无 NOT NULL | 安全核心字段。password 是用户认证凭据，缺失 NOT NULL 约束可能产生无法登录的脏数据，直接影响用户认证功能，且已有生产脏数据存在。修复需要脏数据清理 + DDL 变更，风险中等但业务影响最高。 |
| P1 | Issue 4：enabled/visible 无默认值 | 影响四个核心实体的布尔语义正确性。通过 Java 代码创建实体后不显式 setEnabled() 会写入 NULL。修复简单（仅改5处 Java 字段初始值），无数据迁移风险。且与 Issue 1 存在时序依赖——若 Issue 4 的方案 A 先修复，Issue 1 的测试代码可简化。 |
| P2 | Issue 3：deleted 列 NOT NULL 不一致 | 影响范围最广（16张表）。但运行时坏影响相对可控：实体层已有 `deleted = false` 默认值兜底，`deleted` 字段本身与核心业务逻辑无直接关联（仅影响软删除判断）。迁移方案涉及 16 张表的 ALTER TABLE，操作复杂度高但业务紧急度低。 | 
| P3（最低） | Issue 1：缺少 User/Role/Post 测试 | 纯测试遗漏，不直接影响生产功能。同时是 Issues 2/3/4 修复后的验证手段——新测试可验证 password NOT NULL、deleted NOT NULL、enabled 默认值等映射是否正确，因此上述三个问题修复后应优先补充测试以覆盖回归防护。 |

### 交叉对比分析

| 对比维度 | Issue 2 (P0) | Issue 4 (P1) | Issue 3 (P2) | Issue 1 (P3) |
|----------|-------------|-------------|-------------|-------------|
| 影响实体数 | 1 | 4 | 16 | 3 |
| 业务影响 | 直接影响用户认证 | 影响布尔语义一致性 | 影响软删除行为 | 无生产影响 |
| 修复复杂度 | 中（需清理脏数据） | 低（改5个 Java 字段） | 高（16张表 ALTER TABLE） | 低（新增测试方法） |
| 是否已有生产脏数据 | 是 | 是 | 可能 | N/A |
| 运行时异常风险 | 有（NULL 写入不受阻） | 有（NULL 写入不受阻） | 低（实体默认值兜底） | 无 |
| 修复风险 | 中（ALTER TABLE 会因脏数据失败） | 低（无 DDL 变更） | 高（16张表锁、大表问题） | 无 |

Issue 3（P2）影响 16 张表但仍排在 Issue 4（P1）之后的量化依据：
- 实体层已有 `deleted = false` 默认值，Java 创建的实体不会产生 NULL，运行时 NULL 写入风险远低于 Issue 4 和 Issue 2
- 生产脏数据需逐表清理，操作复杂度高但业务坏影响低（`deleted IS NULL` 和 `deleted = 0` 在 `@SQLRestriction("deleted = false")` 过滤下均被视为"未删除"）
- 修复需要在低峰期逐表或分批执行，时间窗口依赖运维排期，不宜因复杂度高而提升优先级

### 时序依赖关系

```
Issue 4 (P1)
  └──→ Issue 1 (P3)：Issue 4 方案 A 修复后，Issue 1 测试代码可简化（无需 setEnabled）
      └──→ Issue 2 (P0)：Issue 1 新增测试可验证 password NOT NULL 约束
      └──→ Issue 3 (P2)：Issue 1 新增测试可验证 deleted NOT NULL 约束

Issue 2 (P0)：可独立修复，不依赖其他 Issue
Issue 3 (P2)：可独立修复，不依赖其他 Issue
```

### 建议修复顺序

**推荐执行顺序**：Issue 2 (P0) → Issue 4 (P1) → Issue 3 (P2) → Issue 1 (P3)

说明：Issue 2 作为安全核心问题优先修复。Issue 4 修复简单、影响面可控，且能简化后续 Issue 1 的测试代码。Issue 3 修复复杂度高但业务紧急度最低。Issue 1 作为回归验证手段放在最后，可同时验证前三个问题的修复效果。

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
