# 诊断质询报告（v14）

## 质询结果

LOCATED

## 逐维度审查

### 1. 证据充分性

**[通过]** Issue 1（缺少 User/Role/Post 测试）：与 `EntityMappingIT.java` 实际代码一致——该文件确实仅包含 AllergyHistory、HealthProfile、PatientEntity、DoctorEntity、Function、DictData/DictType、TokenStore 测试，不含 User/Role/Post。`integration/pom.xml:52-56` 确已声明 `common-module-impl` 为 test 依赖。

**[通过]** Issue 2（password 无 NOT NULL）：`User.java:28` 确为 `private String password;` 无 `@Column(nullable = false)`。`schema.sql:16` 确为 `DEFAULT NULL`。`User.java:25` username 确有 `@Column(nullable = false, unique = true)`。`new User()` 全局搜索确仅 13 处，全部在测试类中且不涉及持久化。`enabled == null` 全局搜索无匹配。`application.yml:31` 确为 `spring.sql.init.mode: never`。

**[通过]** Issue 3（deleted NOT NULL 不一致）：`BaseEntity.java:37` 确有 `@Column(nullable = false)`。`BaseEntity.java:24` 确有 `@SQLRestriction("deleted = false")`。已逐表查看 schema.sql 中全部 16 张表，`deleted` 列均仅有 `DEFAULT 0` 无 `NOT NULL`，且报告标注的行号全部精确匹配（sys_user:25, sys_role:44, sys_post:65, sys_function:94, sys_dict_type:113, sys_dict_data:135, patient_profile:197, doctor_profile:222, admin_profile:242, health_profile:263, allergy_history:284, chronic_disease:303, family_history:322, surgery_history:341, medication_history:361, sys_token:422）。

**[通过]** Issue 4（enabled/visible 无默认值）：`User.java:36`、`Role.java:28`、`Post.java:30`、`Function.java:30` 确均为 `private Boolean enabled;` 无初始值。`Function.java:54` 确为 `private Boolean visible;`。`schema.sql` 各表 enabled 列均确为 `DEFAULT 1`。`JpaConfig.java:7` 确有 `@EnableJpaAuditing`。测试 `application.yml` 确配置 `ddl-auto: create-drop` + `sql.init.mode: never`。

### 2. 逻辑完整性

**[通过]** Issue 1→2/3/4 的依赖关系分析（交叉影响备注、时序依赖图）逻辑一致——测试依赖 NOT NULL 约束和默认值修复后方可验证约束行为，与代码实际行为一致。

**[通过]** Issue 2 与 Issue 4 的清理时序交叉冲突分析正确——当记录同时满足 `password IS NULL` 和 `enabled IS NULL` 时，先清理 enabled 再清理 password 的语义（无密码账号被禁用）正确，反之则产生安全漏洞。

**[通过]** SQL 三值逻辑分析正确——`@SQLRestriction("deleted = false")` 下 `NULL = false` 为 UNKNOWN，WHERE 视 UNKNOWN 为 FALSE，`deleted IS NULL` 的记录确实被过滤。

**[通过]** 建议修复顺序的两阶段策略（代码修复 → 数据清理）区分合理，数据清理第 1/2 步的先后顺序约束（先 Issue 4 enabled 清理 → 再 Issue 2 password 清理）及其安全理由陈述完整。

### 3. 覆盖完备性

**[通过]** 原始需求（requirement.md）的 4 个问题全部覆盖：Issue 1（缺少集成测试）、Issue 2（password NOT NULL）、Issue 3（deleted NOT NULL 不一致）、Issue 4（enabled/visible 默认值）。

**[通过]** 迭代评审（a_v14_iteration_requirement.md）指出的 4 个问题已全部在本版修正：
- Issue 2 DDL 变更已移除"（可选）"标注
- Issue 2 主操作顺序已显式纳入与 Issue 4 的交叉数据清理依赖
- `@ColumnDefault("true")` 已改为 `@ColumnDefault("1")`
- Issue 3 验证步骤注释已修正为"确认清理完成：所有 NULL 已被 UPDATE 为 0"

**[通过]** 当前诊断报告给出了每个问题的根因（代码/DDL 行级定位）、影响范围（含三值逻辑分析）、生产数据迁移方案（预检→清理→ALTER→验证→回滚）、修复追踪建议，根因定位准确，修复者可据此采取行动。

## 质询要点

无（LOCATED，无严重/一般问题）
