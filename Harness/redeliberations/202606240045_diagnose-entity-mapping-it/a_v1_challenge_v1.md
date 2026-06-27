# 诊断质询报告（v1）

## 质询结果

LOCATED

## 逐维度审查

### 1. 证据充分性

**[通过]** 各事实性陈述均可与代码交叉验证：

- Issue 1：User.java、Role.java、Post.java 实际存在于 common-module-impl 中，EntityMappingIT.java 确实未包含这三个实体的测试用例，integration/pom.xml 第52-56行确已声明 common-module-impl 为 test 依赖
- Issue 2：User.java:28 确无 `@Column(nullable = false)`，username 字段（第25行）确有该注解，schema.sql 第16行 password 列确为 `DEFAULT NULL`
- Issue 3：BaseEntity.java:37 确标注了 `@Column(nullable = false)`，schema.sql 中所有16张表的 deleted 列均缺少 NOT NULL（各表行号与报告一致）
- Issue 4：User.java:36、Role.java:28、Post.java:30、Function.java:30 的 enabled 字段均无 Java 默认值，Function.java:54 的 visible 字段同理，DDL 中对应列均配置了 `DEFAULT 1`

**[问题-轻微]** Issue 1 中对测试遗漏原因的描述"测试类在编写时只覆盖了此前报告过有映射问题的实体"缺乏证据支持，属推测性陈述。但不影响根因认定（测试遗漏本身即为根因）。

**[通过]** Issue 4 关于 Hibernate 在 INSERT 语句中包含所有列的断言符合常见 Hibernate 行为，对论点支撑充分。

### 2. 逻辑完整性

**[通过]** 四项问题的因果链均完整：

- Issue 1：现象→测试遗漏→根因（无代码问题，仅覆盖缺口），逻辑清晰
- Issue 2：字段无注解 → 实体/DDL 均允许 NULL → 可能产生无法登录的脏数据，链条完整
- Issue 3：BaseEntity 声明 NOT NULL 约束 → DDL 遗漏 → 手工 DDL 路径数据可能含 NULL，链条完整
- Issue 4：字段无 Java 默认值 → Hibernate 写入 NULL → DDL DEFAULT 未生效 → 不同入口数据行为不一致，链条完整

**[通过]** 无逻辑矛盾。各问题独立且诊断结论互不冲突。

### 3. 覆盖完备性

**[通过]** 四项任务问题全部覆盖：

1. EntityMappingIT 缺少 User/Role/Post 集成测试 ✓
2. password 无 NOT NULL 约束 ✓
3. DDL deleted 列 NOT NULL 与 BaseEntity 不一致 ✓
4. enabled/visible 布尔字段跨实体缺少默认值 ✓

**[通过]** 每项问题均给出了问题现象、根因定位和影响范围分析，完整回答了"问题是什么"和"为什么发生"。

## 质询要点（LOCATED 时此节可为空）

无严重/一般问题。
