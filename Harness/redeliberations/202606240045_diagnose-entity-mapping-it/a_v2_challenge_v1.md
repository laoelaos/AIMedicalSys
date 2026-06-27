# 诊断质询报告（v1）

## 质询结果

LOCATED

## 逐维度审查

### 1. 证据充分性

**[通过]** Issue 1 中 EntityMappingIT.java 缺少 User/Role/Post 测试的断言与代码实际内容一致（EntityMappingIT.java 仅覆盖 AllergyHistory、HealthProfile、PatientEntity、DoctorEntity、Function、DictData/DictType、TokenStore，无 User/Role/Post）；integration/pom.xml:53-56 声明 common-module-impl 为 test 依赖已核实。

**[通过]** Issue 2 中 User.java:28 缺少 `@Column(nullable = false)`（对比 username 的 `@Column(nullable = false, unique = true)`）、schema.sql:16 password 列仅有 `DEFAULT NULL` 无 NOT NULL（对比 username 列的 `NOT NULL`）均已核实。

**[通过]** Issue 3 中 BaseEntity.java:37 `@Column(nullable = false)` 标注已核实；schema.sql 中 16 张表的 deleted 列（sys_user:25、sys_role:44、sys_post:65、sys_function:94、sys_dict_type:113、sys_dict_data:135、patient_profile:197、doctor_profile:222、admin_profile:242、health_profile:263、allergy_history:284、chronic_disease:303、family_history:322、surgery_history:341、medication_history:361、sys_token:422）全部仅有 `DEFAULT 0` 无 NOT NULL，均已核实。

**[通过]** Issue 4 中 User.java:36（enabled）、Role.java:28（enabled）、Post.java:30（enabled）、Function.java:30（enabled）、Function.java:54（visible）五个布尔字段均无 Java 默认值已核实；DDL 中对应列均为 `DEFAULT 1` 已核实；Hibernate INSERT 包含所有列导致 DEFAULT 不被触发的行为描述与 JPA 规范一致。

**[问题-轻微]** Issue 1 的"需验证的映射点"表格中 User 行的代码位置标注为 `User.java:25-46`，但 `posts` 字段的 `@ManyToMany` 定义位于 User.java:48-52，已超出 25-46 范围。行号引用有微小偏差，但不影响诊断结论的准确性。

### 2. 逻辑完整性

**[通过]** Issue 1：文件存在且依赖可访问 → 未编写对应测试方法 → 测试遗漏。因果链完整，无跳跃。

**[通过]** Issue 2：实体层无 NOT NULL 约束 → DDL 对应列无 NOT NULL → 插入 NULL password 不会报错 → 可能产生无法登录的脏数据。因果链完整。

**[通过]** Issue 3：BaseEntity 声明 `nullable = false` → DDL 手工编写遗漏 NOT NULL → Hibernate DDL auto 与手工 DDL 路径行为不一致 → 脏数据风险。因果链完整。

**[通过]** Issue 4：布尔字段无 Java 默认值（`= true`） → Hibernate INSERT 包含所有列 → NULL 写入数据库 → DDL 的 `DEFAULT 1` 未生效。因果链完整，无矛盾线索。

### 3. 覆盖完备性

**[通过]** 原始需求（requirement.md）中列出的 4 个问题全部覆盖：缺少 User/Role/Post 集成测试、password 无 NOT NULL 约束、deleted 列 NOT NULL 不一致、enabled/visible 布尔字段缺少默认值。

**[通过]** 迭代任务（a_v2_iteration_requirement.md）中的 5 条审查意见均已回应：
- 修复方案缺失 → 回应为超出诊断边界（与质询框架指导一致）
- 优先级排序 → 已在 v2 中新增章节
- 跨问题影响分析 → 已在 Issue 1 和 Issue 4 中增加交叉影响备注
- 可操作性不足 → 回应为超出诊断边界
- 迁移/回滚风险评估 → 回应为超出诊断边界

**[通过]** 诊断结论完整回答了"问题是什么"（各节现象描述）和"为什么发生"（根因分析及代码级定位）。

## 质询要点

无严重或一般问题。仅有 1 项轻微问题：User 映射点表格中行号范围 25-46 未覆盖 `posts` 字段的实际位置 48-52，建议修正为 `User.java:25-52` 以准确反映所有映射点位置。
