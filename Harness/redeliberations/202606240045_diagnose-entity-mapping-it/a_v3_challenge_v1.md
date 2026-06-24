# 诊断质询报告（v1）

## 质询结果

LOCATED

## 逐维度审查

### 1. 证据充分性

**[通过]** 所有根因判定均有对应代码引用支撑，关键推断均经实际文件比对验证：

- Issue 1：EntityMappingIT.java 无 User/Role/Post 测试 → 已读文件确认；依赖可访问性 → 已读 integration/pom.xml:53-56 确认
- Issue 2：User.java:28 缺少 `@Column(nullable = false)` → 已读确认；schema.sql:16 DEFAULT NULL → 已读确认
- Issue 3：BaseEntity.java:37 `@Column(nullable = false)` → 已读确认；16 张表 deleted 列均缺少 NOT NULL → 逐行核对 schema.sql 确认，admin_profile 行号已修正为 242
- Issue 4：User.java:36 / Role.java:28 / Post.java:30 / Function.java:30 / Function.java:54 均无 Java 默认值 → 已逐一确认；对应 DDL 列均为 DEFAULT 1 → 已读确认

**[通过]** 对 Hibernate 写入行为（INSERT 包含所有列导致 DDL DEFAULT 不生效）的描述符合 JPA/Hibernate 规范，非推测。

**[通过]** 代码行为的描述与实际代码一致。

### 2. 逻辑完整性

**[通过]** 从问题现象到根因的因果链完整：

- Issue 1：EntityMappingIT 有测试但未覆盖 User/Role/Post → 测试遗漏，逻辑连贯
- Issue 2：password 无注解约束 → 实体与 DDL 均允许 NULL → 可产生 NULL 脏数据，因果链完整
- Issue 3：BaseEntity 声明 NOT NULL 但 DDL 遗漏 → 手工 DDL 与实体不一致，因果链清晰
- Issue 4：Java 字段无默认值 → Hibernate INSERT 含该列 → DDL DEFAULT 不触发 → NULL 写入，因果链完整

**[通过]** 跨问题交叉影响分析合理（Issue 4 影响 Issue 1 的新测试代码需显式 setEnabled）。

**[通过]** 影响范围判定合理，无矛盾线索。

### 3. 覆盖完备性

**[通过]** 原始需求的 4 个问题（EntityMappingIT 缺测试、password 无 NOT NULL、deleted NOT NULL 不一致、enabled/visible 缺默认值）全部覆盖。

**[通过]** 每个问题均包含现象描述、根因分析、证据引用、影响范围、交叉影响说明。

**[通过]** 诊断结论完整回答了「问题是什么」（4 个具体问题）和「为什么发生」（每项的根因）。

## 质询要点

（无 — 结果 LOCATED，不存在严重或一般问题）
