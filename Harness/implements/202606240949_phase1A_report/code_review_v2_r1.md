# 代码审查报告（v2 r1）

## 审查结果
APPROVED

## 发现

### 变更 1：H2 依赖 — 通过
- **AIMedical/backend/modules/common-module/common-module-impl/pom.xml** — 在 `spring-boot-starter-test` 之后按设计追加了 H2 runtime scope 依赖，groupId/artifactId 正确，与 root `dependencyManagement` 版本管控一致。✅

### 变更 2：schema.sql 16 张表 deleted 列 NOT NULL — 通过
全部 16 张表（sys_user、sys_role、sys_post、sys_function、sys_dict_type、sys_dict_data、patient_profile、doctor_profile、admin_profile、health_profile、allergy_history、chronic_disease、family_history、surgery_history、medication_history、sys_token）的 `deleted` 列均已从 `DEFAULT 0` 改为 `NOT NULL DEFAULT 0`，与 BaseEntity.java 的 `@Column(nullable = false)` 对齐。✅

### 变更 3：Java 默认值 — 通过
- **User.java:37** — `private Boolean enabled = true;` ✅
- **Role.java:28** — `private Boolean enabled = true;` ✅
- **Post.java:30** — `private Boolean enabled = true;` ✅
- **Function.java:30** — `private Boolean enabled = true;` ✅
- **Function.java:54** — `private Boolean visible = true;` ✅

### 变更 4：EntityMappingIT 测试 — 通过
- 新增 import（UserType、Role、Post、Set、ConstraintViolationException）全部正确引入 ✅
- **User 组（5 个）**：`user_shouldMapUsernameField`、`user_shouldEnforceUserTypeNotNull`、`user_shouldMapManyToManyWithRoles`、`user_shouldMapManyToManyWithPosts`、`user_shouldMapUserTypeEnumAsString` — 逻辑完整，验证点覆盖设计规格 ✅
- **Role 组（3 个）**：`role_shouldMapCodeField`、`role_shouldEnforceCodeUniqueConstraint`、`role_shouldMapOneToManyPosts` — 正确校验 ✅
- **Post 组（2 个）**：`post_shouldMapManyToOneRole`、`post_shouldMapManyToManyFunctions` — 正确校验 ✅

### 设计偏差说明
1. **[轻微]** **EntityMappingIT.java** — 设计规格要求 9 个新测试方法追加在 `patientWithHealthProfileAndAllergy_shouldWorkTogether` 之后，实际放置在 `user_shouldRejectNullPassword` 之后、`patientWithHealthProfileAndAllergy_shouldWorkTogether` 之前。该变更不影响功能正确性，但未在实现报告中记录。建议在实现报告中注明此结构调整。

### 已记录的设计偏差（经审查可接受）
- **Exception 类型偏差**：设计要求断言 `DataIntegrityViolationException`，实际使用 `ConstraintViolationException`。原因成立（EntityManager 直接操作时 Hibernate 抛出未包装的原始异常），不影响正确性。
- **schema.sql 多空格对齐**：由于各表缩进不一致，逐表独立替换。最终结果正确。

### 预存问题（非本次变更引入）
- `user_shouldPersistWithPassword` — 未设 userType，在 H2 DDL 生成环境下触发非空约束
- `user_shouldRejectNullPassword` — 期望 `DataIntegrityViolationException`，实际抛出 `ConstraintViolationException`

## 修改要求（仅 REJECTED 时）
N/A
