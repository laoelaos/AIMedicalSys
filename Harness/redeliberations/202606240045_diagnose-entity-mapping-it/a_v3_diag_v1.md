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

**交叉影响备注**：User.enabled、Role.enabled、Post.enabled 均无 Java 默认值（见 Issue 4），新测试中创建实体后不显式调用 `setEnabled(...)` 会导致 `enabled` 列插入 NULL，而非数据库 DEFAULT 1。测试代码需在构建实体时显式设置 `enabled` 值。

### 修复指引

在 `EntityMappingIT.java` 中新增 User、Role、Post 三组集成测试方法，每组遵循「创建实体→persist→flush→find→断言」模式，覆盖上述需验证的映射点。新增测试中需显式调用 `setEnabled(true/false)`（因 Issue 4 的 enabled 无默认值问题）。

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

**交叉影响备注**：Issue 1 中为 User/Role/Post 新增集成测试时，需注意此问题——测试代码必须显式调用 `setEnabled(true/false)`，否则插入的 `enabled` 列值将为 NULL，导致断言失败或语义错误。

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

---

## 优先级排序

| 优先级 | 问题 | 排序理由 |
|--------|------|---------|
| P0（最高） | Issue 2：password 无 NOT NULL | 安全核心字段，缺失约束可能产生无法登录的脏数据，影响用户认证功能 |
| P1 | Issue 4：enabled/visible 无默认值 | 影响四个实体（User/Role/Post/Function）的布尔语义正确性，且与 Issue 1 交叉影响，应在新增测试前修复 |
| P2 | Issue 3：deleted 列 NOT NULL 不一致 | 影响所有16张表的逻辑删除字段，范围最广但运行时坏影响相对可控（实体层有 `deleted = false` 默认值兜底） |
| P3（最低） | Issue 1：缺少 User/Role/Post 测试 | 纯测试遗漏，不直接影响生产功能。**注意**：Issue 1 同时也是 Issues 2/3/4 修复后的验证手段——新测试可验证 password NOT NULL、deleted NOT NULL、enabled 默认值等映射是否正确，因此上述三个问题修复后应优先补充测试以覆盖回归防护 |

---

## 修订说明（v3）

| 质询意见 | 回应 |
|---------|------|
| 1. [严重] 需求响应缺失——"给出修复方案"未被满足 | **接受**。已在每个 Issue 下新增「修复指引」章节，定位到具体文件行号及所需变更方向（如"添加 @Column(nullable = false) 注解"、"将字段改为带初始值的形式"等）。指引精确到文件:行号级别，修复者可据此直接编写代码。 |
| 2. [中等] Issue 4 缺少修复路径的权衡分析 | **接受**。已在 Issue 4 中新增「修复路径权衡分析」表格，对比 A.Java默认值 / B.@ColumnDefault / C.仅改DDL 三种方案的优缺点，并给出推荐组合。 |
| 3. [中等] Issue 2 缺少对生产脏数据的具体行动指引 | **接受**。已在 Issue 2 中新增「生产脏数据行动指引」章节，提供预检 SQL、三种清理策略（人工补录/批量禁用/直接清理）的适用场景及操作、操作顺序建议。 |
| 4. [轻微] admin_profile 行号标注偏差 | **接受**。已修正——`admin_profile` 的 `deleted` 列位于 `schema.sql:242`（而非 v2 版标注的 243），对应行号已在本版所有表格中更正为 242。 |
| 5. [轻微] 优先级排序中 Issue 1 的定位不够精确 | **接受**。已在优先级排序表的 P3 排序理由中补充说明 Issue 1 作为 Issues 2/3/4 修复验证手段的关联定位。 |
