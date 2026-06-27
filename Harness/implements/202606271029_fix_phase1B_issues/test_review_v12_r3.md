# 测试审查报告（v12 r3）

## 审查结果
REJECTED

## 发现

- **[严重]** `AIMedical/backend/integration/src/test/java/com/aimedical/integration/EntityMappingIT.java` — T31 `role_shouldRejectNullEnabled` 即使加入 `role.setEnabled(null)` 后，期望异常类型 `ConstraintViolationException` 仍然错误。`@Column(nullable = false)` 约束违反时 Hibernate 抛出的是 `org.hibernate.PropertyValueException`，而非 `jakarta.validation.ConstraintViolationException`。测试将因异常类型不匹配而失败，无法验证目标行为。

## 修改要求（仅 REJECTED 时）

**严重 — EntityMappingIT.java `role_shouldRejectNullEnabled`**

- **问题**：`@Column(nullable = false)` 运行时由 Hibernate 在 flush 时校验，违反时抛出 `org.hibernate.PropertyValueException`（`HibernateException` 子类）。`ConstraintViolationException`（`jakarta.validation`）是完全不同的异常层级，`assertThrows(ConstraintViolationException.class, ...)` 无法捕获 `PropertyValueException`，测试执行时抛出未捕获异常而失败。
- **期望修正方向**：根据 `@Column(nullable = false)` 校验的实际异常类型，将期望异常改为 `org.hibernate.PropertyValueException`（若允许直接依赖 Hibernate API），或改为 `org.springframework.dao.DataIntegrityViolationException`（若经 Spring 包装），或移除该测试并重新评估设计。
