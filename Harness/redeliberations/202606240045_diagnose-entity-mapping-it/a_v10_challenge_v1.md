# 诊断质询报告（v10）

## 质询结果

LOCATED

## 逐维度审查

### 1. 证据充分性

**[通过]** Issue 1（缺少 User/Role/Post 测试）：报告已确认 EntityMappingIT.java 中现有测试覆盖范围、pom.xml 依赖关系、实体存在性，证据链完整。

**[通过]** Issue 2（password 无 NOT NULL）：已核实 User.java:28 确为 `private String password;` 无 `@Column(nullable = false)`，schema.sql:16 为 `DEFAULT NULL`。代码路径搜索（new User() / UserRepository / 数据脚本）已执行。

**[通过]** Issue 3（deleted 列 NOT NULL 不一致）：已核实 BaseEntity.java:37-38 有 `@Column(nullable = false)` 和 `private Boolean deleted = false;`，schema.sql 中 16 张表的 deleted 列均为 `TINYINT(1) DEFAULT 0` 无 NOT NULL。

**[通过]** Issue 4（enabled/visible 无默认值）：已核实 User.java:36、Role.java:28、Post.java:30、Function.java:30 的 `private Boolean enabled;` 均无初始值。代码路径搜索已覆盖 getEnabled()、enabled == null 等模式。

### 2. 逻辑完整性

**[通过]** 各 Issue 从现象到根因的因果链完整无跳跃。Issue 2-3-4 的交叉影响（password + enabled 数据冲突时序、@SQLRestriction 三值逻辑分析、Issue 4 修复时序对 Issue 1 测试的影响）均被系统分析。

**[通过]** 优先级排序的交叉对比表量化依据充分，Issue 3 因三值逻辑纠正从 P2 提升至 P1 的理由合理。

**[通过]** 测试环境能力边界分析正确：`sql.init.mode: never` 导致 schema.sql 不在测试中加载，已明确标注。

### 3. 覆盖完备性

**[通过]** 原始需求的 4 个问题现象均有解释，无遗漏。

**[通过]** v10 迭代需求的 2 个问题均已修复：
- `role_shouldMapCodeUniqueConstraint` 已重命名为 `role_shouldMapCodeField`（与 User 侧对齐），并补充了 `role_shouldEnforceCodeUniqueConstraint` 独立测试；M7 映射点备注已同步更新。
- M1 描述已从"唯一约束"修正为"字段映射"，备注标注了约束边界。

**[通过]** 映射点表（M1-M15）提供了完整的可追溯性，未覆盖点（M10 Role.users）已显式标注。

## 质询要点

无。无严重/一般问题。
