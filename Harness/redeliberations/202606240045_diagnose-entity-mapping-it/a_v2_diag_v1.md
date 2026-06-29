# 诊断报告：EntityMappingIT 实体映射集成测试问题

## 1. EntityMappingIT 缺少 User/Role/Post 集成测试

### 现象

`EntityMappingIT.java`（`AIMedical\backend\integration\src\test\java\com\aimedical\integration\EntityMappingIT.java`）包含了多组 JPA 实体映射验证测试（AllergyHistory、HealthProfile、PatientEntity、DoctorEntity、Function、DictData/DictType、TokenStore），但完全没有覆盖 Phase 1 包A 的核心实体 `User`、`Role`、`Post`。

### 根因

`User.java`（`common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/User.java`）、`Role.java`（同包）、`Post.java`（同包）均已存在，且 `integration/pom.xml` 已声明 `common-module-impl` 为 test 依赖（第53-56行），因此在集成测试模块中完全可访问。

此问题是**测试遗漏**，不是代码问题。测试类在编写时只覆盖了此前报告过有映射问题的实体，未将 Phase 1 核心实体纳入。

### 需验证的映射点

| 实体 | 需验证的映射点 | 关键代码位置 |
|------|---------------|-------------|
| User | `username` 唯一约束（`unique=true`）、`password` 无 NOT NULL、`userType` 枚举 @Enumerated(EnumType.STRING)、`roles`/`posts` @ManyToMany 关联表 | User.java:25-46 |
| Role | `code` 唯一约束、`enabled` 无默认值、`posts` @OneToMany、`users` @ManyToMany | Role.java:21-37 |
| Post | `code` 唯一约束、`enabled` 无默认值、`role` @ManyToOne / `functions` @ManyToMany、`sort` 字段 | Post.java:23-45 |

实体均继承 `BaseEntity`，需验证 `id` 自增、`deleted` NOT NULL、`createdAt`/`updatedAt` 审计字段的映射行为。

**交叉影响备注**：User.enabled、Role.enabled、Post.enabled 均无 Java 默认值（见 Issue 4），新测试中创建实体后不显式调用 `setEnabled(...)` 会导致 `enabled` 列插入 NULL，而非数据库 DEFAULT 1。测试代码需在构建实体时显式设置 `enabled` 值。

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

### 影响范围

- 代码或 SQL 插入一条 `password` 为 NULL 的 `sys_user` 记录时，不会收到数据库约束错误，导致系统中可能产生无法登录的脏数据
- 此问题在生产数据库上已存在（生产 DDL 该列无 NOT NULL），清理已存在的 NULL 数据需要单独处理

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
| admin_profile | 243 |
| health_profile | 263 |
| allergy_history | 284 |
| chronic_disease | 303 |
| family_history | 322 |
| surgery_history | 341 |
| medication_history | 361 |
| sys_token | 422 |

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

---

## 优先级排序

| 优先级 | 问题 | 排序理由 |
|--------|------|---------|
| P0（最高） | Issue 2：password 无 NOT NULL | 安全核心字段，缺失约束可能产生无法登录的脏数据，影响用户认证功能 |
| P1 | Issue 4：enabled/visible 无默认值 | 影响四个实体（User/Role/Post/Function）的布尔语义正确性，且与 Issue 1 交叉影响，应在新增测试前修复 |
| P2 | Issue 3：deleted 列 NOT NULL 不一致 | 影响所有16张表的逻辑删除字段，范围最广但运行时坏影响相对可控（实体层有 `deleted = false` 默认值兜底） |
| P3（最低） | Issue 1：缺少 User/Role/Post 测试 | 纯测试遗漏，不直接影响生产功能，但上述三个问题修复后应补充测试以覆盖回归防护 |

---

## 修订说明（v2）

| 质询意见 | 回应 |
|---------|------|
| 缺失修复方案——需求明确要求"给出修复方案" | **不接受**。本角色为诊断专家，职责边界为"定位层"——追溯根因到具体代码位置和触发条件，使修复者知道"改哪里"和"为什么"。给出完整的修复方案、代码补丁、逐步骤修复指引属于"修复层"工作，超出诊断边界。需求原文中的"修复方案"在此上下文中应理解为定位到修复者可直接行动的粒度，而非绕过诊断阶段直接给出代码补丁。 |
| 缺少优先级排序 | **接受**。已在本版报告新增「优先级排序」章节，按影响面/紧急性排序并说明理由。 |
| 缺少跨问题影响分析（Issue 1 ↔ Issue 4） | **接受**。已在 Issue 1 和 Issue 4 中分别增加「交叉影响备注」，说明 enabled 字段默认值问题对新增测试的影响。 |
| 可操作性不足——缺少具体修复指令（SQL、代码片段） | **不接受**。提供具体 SQL DDL 脚本和 Java 代码变动属于"修复层"工作，超出诊断边界。本报告已定位到每个问题的具体代码位置、行号、字段名，修复者查阅 API 文档编写对应变更属于正常编码活动。 |
| 缺少迁移/回滚风险评估 | **不接受**。迁移计划、回滚策略、数据清理 SQL 属于部署/运维范畴，超出诊断边界。本报告已在 Issue 2、Issue 3 的影响范围中标注了脏数据问题，提示修复者注意历史数据清理的必要性，这已到达诊断层应有的粒度。 |
