# 诊断质询报告（v13）

## 质询结果

LOCATED

## 逐维度审查

### 1. 证据充分性

**[通过]** Issue 1（EntityMappingIT 缺少测试）的证据充分——实际读取 EntityMappingIT.java（277 行）确认确实未包含 User/Role/Post 测试；integration/pom.xml:53-56 确认 common-module-impl 已声明为 test 依赖；映射点表中所有行号引用均与实际代码一致（User.java、Role.java、Post.java、BaseEntity.java、schema.sql）。

**[通过]** Issue 2（password 无 NOT NULL）的证据充分——User.java:28 确认缺少 `@Column(nullable = false)`，schema.sql:16 确认 `DEFAULT NULL`；`new User()` 全局搜索确认 13 处全部位于测试代码（UserTest 11 处、RoleTest 1 处、PostTest 1 处）；`UserRepository` 引用搜索确认无生产代码路径（仅接口定义和单元测试）。

**[通过]** Issue 3（deleted 列 NOT NULL 不一致）的证据充分——BaseEntity.java:37 确认 `@Column(nullable = false)`；schema.sql 中全部 16 张表的 deleted 列均仅为 `DEFAULT 0` 无 `NOT NULL`；SQL 三值逻辑分析正确（`NULL = 0` → UNKNOWN → WHERE 视为 FALSE）。

**[通过]** Issue 4（enabled/visible 缺少默认值）的证据充分——User:36、Role:28、Post:30、Function:30/54 均确认无 Java 默认值；DDL 行号引用准确（21/40/60/90/86）；`enabled == null` 全局搜索无匹配。

**[问题-轻微]** Issue 4 副作用分析中 getEnabled() 调用数量标注不精确——报告称"getEnabled() 调用（4 处）"但后续列举了 8 处调用（各测试类各 2 处），计数与实际不符。不过不影响结论（全部在测试代码中）。

### 2. 逻辑完整性

**[通过]** 各 Issue 从现象到根因的因果链完整——Issue 1（测试遗漏→未覆盖）；Issue 2（缺注解+DDL → 可写 NULL 密码）；Issue 3（实体 NOT NULL vs DDL 无 NOT NULL → 不一致 + @SQLRestriction 静默隐藏 NULL 记录）；Issue 4（无 Java 默认值 → Hibernate 写入 NULL 绕过 DDL DEFAULT）。

**[通过]** 跨 Issue 的交叉影响分析逻辑一致——Issue 1 测试依赖 Issue 2/4 修复时序；Issue 2 与 Issue 4 的清理 SQL 执行顺序依赖正确分析（先 Issue 4 清理 enabled，再 Issue 2 禁用无密码账号，确保安全语义）。

**[通过]** v13 两阶段执行策略（阶段一：代码修复；阶段二：数据清理+DDL）正确解决 v12 审查中指出的时序矛盾，且第 2 阶段内部的清理顺序标注了安全理由。

**[通过]** M10（Role.users mappedBy 端）跳过理由充分——JPA 双向 `@ManyToMany` 的 mappedBy 端与 owning 端（M4）共享同一关联表，M4 已覆盖正向映射。

### 3. 覆盖完备性

**[通过]** 所有 4 个原始需求均被覆盖：
- EntityMappingIT 缺少 User/Role/Post 测试 → 提供了完整映射点表和 7 个测试方法示例
- password 无 NOT NULL 约束 → 根因定位 + 代码路径排查 + 脏数据清理指引
- deleted 列 NOT NULL 不一致 → 16 张表逐表分析 + 完整迁移方案
- enabled/visible 缺少默认值 → 5 个字段全部列出 + 三种方案权衡

**[通过]** v12 审查提出的 5 个改进要求均在 v13 中落实：
1. [严重] 修复顺序与清理时序矛盾 → 两阶段策略（Lines 852-869）
2. [中等] Issue 3 回滚未还原约束 → 新增约束回滚步骤（Lines 679-697）
3. [一般] Issue 3 验证遗漏 DEFAULT/@SQLRestriction 行为 → 补充验证步骤 B/C（Lines 573-594）
4. [一般] Issue 2 表锁风险未分析 → 新增表锁风险条目（Line 399）
5. [一般] M10 未覆盖无处理策略 → 标注跳过理由 + 策略说明（Lines 28, 319）

## 质询要点（LOCATED 时不存在）
