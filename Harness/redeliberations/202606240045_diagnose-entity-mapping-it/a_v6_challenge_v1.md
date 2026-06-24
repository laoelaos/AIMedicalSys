# 诊断质询报告（v6）

## 质询结果

LOCATED

## 逐维度审查

### 1. 证据充分性

**[通过]** Issue 1（缺少 User/Role/Post 集成测试）：M1-M15 映射点均标注了精确的 file:line 引用，经验证与实际代码一致（User.java、Role.java、Post.java、schema.sql）。`integration/pom.xml:53-56` 的 test scope 依赖确认已核实。提供的测试样例中 persist User 均设置了 password，与 Issue 2 要求一致，消除了 v5 中的逻辑矛盾。

**[通过]** Issue 2（password 无 NOT NULL）：User.java:28（`private String password` 无 `@Column(nullable=false)`）及 schema.sql:16（`DEFAULT NULL`）已验证。`new User()` 搜索 13 处仅出现在测试文件中，不涉及持久化——已验证。`@Autowired UserRepository` 搜索 0 处匹配——已验证。`UserRepository\.*` 方法调用仅出现在 `UserRepositoryTest.java`（3 处，均为接口形态验证）——已验证。`data.sql:81-84` 种子用户 password 均非 NULL 且 `application.yml:31` 中 `spring.sql.init.mode=never`——已验证。

**[问题-轻微]** 诊断报告称 UserRepository 全局搜索共 81 处匹配，实际搜索结果为 85 处（含跨类型文件的全部引用）。该差异不影响结论——所有匹配项均为接口定义、单元测试或设计文档，确无生产代码路径。

**[通过]** Issue 3（deleted NOT NULL 不一致）：BaseEntity @Column(nullable=false) 已确认。schema.sql 中 16 张表的 deleted 列行号（line 25/44/65/94/113/135/197/222/242/263/284/303/322/341/361/422）经逐一核验均正确。@SQLRestriction 三值逻辑分析准确。

**[通过]** Issue 4（enabled/visible 无默认值）：User.java:36、Role.java:28、Post.java:30、Function.java:30/54 均已确认为 `private Boolean enabled/visible;` 无初始值。DDL 中各表的 DEFAULT 1 已确认。三种修复方案权衡分析完整。

### 2. 逻辑完整性

**[通过]** 各 Issue 的因果链完整：从现象（测试缺失/约束缺失/DDL不一致/无默认值）到根因（人为遗漏/设计忽视）均有明确推导路径，无逻辑跳跃。

**[通过]** 无矛盾线索：Issue 1 测试样例与 Issue 2 的 NOT NULL 约束已协调（`user_shouldRejectNullPassword` 替代了 v5 中的 `user_shouldAllowNullPassword`）。Issue 4 与 Issue 1 的时序依赖已做条件化分析。

**[通过]** 影响范围判定合理：Issue 2 区分了"无生产代码路径"与"已有脏数据"两个独立维度；Issue 3 正确分析了 @SQLRestriction 下 `deleted IS NULL` 被静默过滤的隐含业务影响；Issue 4 分析了 null 写入路径与 DDL DEFAULT 之间的行为不一致。

**[通过]** 生产脏数据来源说明已补充过渡段落，消除了"无生产代码路径"与"脏数据可能存在"之间的事实断层。

### 3. 覆盖完备性

**[通过]** 原始需求中的全部 4 个问题现象均已覆盖并给出根因定位。

**[通过]** v6 迭代要求的 4 项审查意见均已回应：(A) `user_shouldAllowNullPassword` → `user_shouldRejectNullPassword` + M2 描述修正 + 交叉影响备注；(B) 脏数据过渡说明；(C) `entityManager.clear()` 统一约定并附解释；(D) 补充 UserRepository + data.sql 双重路径排查。

**[通过]** 优先级排序包含交叉对比分析和时序依赖关系，完整回答了"What"和"Why"。

## 质询要点（CHALLENGED 时存在）

（无严重/一般问题）
