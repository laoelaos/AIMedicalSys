# 诊断质询报告（v1）

## 质询结果

LOCATED

## 逐维度审查

### 1. 证据充分性

**[通过]** 所有关键证据均可通过源代码验证：
- User.java:28 `private String password` 无 `@Column(nullable = false)` ✓
- BaseEntity.java:37-38 `@Column(nullable = false)` + `private Boolean deleted = false` ✓
- schema.sql 中 16 张表的 `deleted` 列均仅有 `DEFAULT 0` 无 `NOT NULL` ✓
- User/Role/Post/Function 中 `enabled`/`visible` 均无 Java 默认值 ✓
- `application.yml:31` 确认 `sql.init.mode: never` ✓
- `JpaConfig.java:7` 确认 `@EnableJpaAuditing` ✓
- `new User()` 仅出现在测试代码中（13处），无生产代码路径 ✓
- 全局搜索 `@Autowired UserRepository` / `userRepository.save()` 无生产代码匹配 ✓
- `@SQLRestriction("deleted = false")` 的三值逻辑分析（`NULL = 0` → UNKNOWN → 被过滤）技术正确 ✓
- 对 Issue 2 的代码路径排查方法和结果描述与实际代码一致 ✓

**[问题-轻微]** 路径描述采用相对路径缩写（`common-module-impl/src/main/java/...`），可精确定位但首次阅读时需结合项目结构理解对应关系。不影响诊断准确性和可操作性。

### 2. 逻辑完整性

**[通过]** 从问题现象到根因的因果链完整，无逻辑跳跃：
- Issue 1：EntityMappingIT 缺 User/Role/Post → 追溯至测试编写时仅覆盖已知问题实体 → 结论为测试遗漏，逻辑自洽
- Issue 2：password 无 NOT NULL → 追溯至 User.java:28 缺 `@Column(nullable = false)` → DDL 与之对齐但约束本身缺失 → 逻辑完整
- Issue 3：16 表 deleted 列 DDL 缺 NOT NULL → 追溯至 BaseEntity 已有 `nullable = false` 而手工 DDL 未对齐 → 逻辑完整
- Issue 4：enabled/visible 无默认值 → 追溯至 Java 字段声明未设置初始值 → Hibernate INSERT 包含所有列导致 DDL DEFAULT 不生效 → 逻辑完整

**[通过]** Issue 2 与 Issue 4 清理策略的交叉数据冲突分析（同一条记录同时满足 `password IS NULL` 和 `enabled IS NULL` 时的执行顺序依赖）完整覆盖了两个方向的因果影响，结论与安全策略一致。

**[通过]** Issue 3 的 `@SQLRestriction` 行为分析从 v5 修正后保持一致，三值逻辑分析准确，优先级排序（P0→P1→P2→P3）及排序依据系统完整。

### 3. 覆盖完备性

**[通过]** 原始需求的 4 个问题全部覆盖：
1. EntityMappingIT 缺少 User/Role/Post 集成测试 ✓ — 含 M1-M15 映射点追溯表及测试示例
2. password 无 NOT NULL 约束 ✓ — 含代码路径排查、脏数据清理策略
3. DDL deleted 列 NOT NULL 不一致 ✓ — 含 16 表逐一罗列、完整迁移方案
4. enabled/visible 布尔字段缺少默认值 ✓ — 含三方案权衡分析、副作用分析

**[通过]** 迭代需求中的 2 个改进点全部在 v12 中响应：
1. Issue 4 交叉影响备注中新增对 Issue 2 清理策略的引用（第 711-714 行）✓
2. Issue 3 回滚方案已从单表示例扩展为 16 张表逐一列出备份和回滚 SQL（第 605-647 行）✓

**[通过]** 诊断结论完整回答了"问题是什么"（现象描述）和"为什么发生"（根因分析），修复者可根据当前交付物识别问题范围并采取行动。

## 质询要点

无。（LOCATED — 无严重/一般问题）
