# 测试审查报告（v12 r2）

## 审查结果
REJECTED

## 发现

- **[严重]** `AIMedical/backend/integration/src/test/java/com/aimedical/integration/EntityMappingIT.java:492-501` — `role_shouldRejectNullEnabled` 测试无效。`Role.java:25` 中 `enabled` 字段有字段初始值 `= true`，因此 `new Role()` 后 `enabled` 始终为 `true`，永远不会为 `null`。`entityManager.persist/flush` 不会抛出任何异常（`@Column(nullable = false)` 约束满足），测试静默通过但未实际验证 null 约束。必须修正测试才能使其真正测试目标行为。

## 修改要求（仅 REJECTED 时）

**严重 — EntityMappingIT.java `role_shouldRejectNullEnabled`（第 492-501 行）**

- **问题**：`Role.enabled` 字段声明为 `private Boolean enabled = true;`，字段初始值导致 `new Role()` 后 enabled 始终为 `true`，测试无法达到 enabled=null 的场景。`entityManager.persist(role); entityManager.flush()` 将正常执行，不会抛出 `ConstraintViolationException`。
- **期望修正方向**：修改测试使 enabled 确实为 null，例如：
  - 方式一：通过反射或 new 后 `role.setEnabled(null)` 显式置 null（但注意 `@Column(nullable = false)` 可能在 insert 前被 Hibernate 拒绝而非抛出 `ConstraintViolationException`）。
  - 方式二：先确认 `Role.enabled` 应移除字段初始值 `= true`（若业务允许 null），使 `new Role()` 后 enabled 默认为 null。
  - 方式三：如果业务要求 enabled 必须有默认值 true，则此测试的前提不成立，应删除此测试并调整设计。
  - **关键**：修正前必须确认 `@Column(nullable = false)` 被违反时实际抛出的异常类型（`PropertyValueException` / `ConstraintViolationException` / `DataIntegrityViolationException`），并修正断言。
