# 质量审查报告：a_v11_diag_v1.md

## 审查概况

审查轮次：第 11 轮
审查视角：需求响应充分度、事实准确性、深度与完整性、可操作性

## 发现问题

### 问题 1：Issue 3 缺失 `@SQLDelete` 交互分析

- **问题描述**：产出在第 628-643 行详细分析了 `@SQLRestriction("deleted = false")` 对 Issue 3 的影响（三值逻辑、脏数据静默隐藏、UDPATE NULL→0 后的行为变化），但完全未提及 `BaseEntity.java:23` 上同一级别的 `@SQLDelete(sql = "UPDATE {h-table} SET deleted = true WHERE id = ?")` 注解。`@SQLRestriction` 与 `@SQLDelete` 共同构成 Hibernate 的软删除机制，缺少后者导致上下文不完整：
  - 读者无法判断 `@SQLDelete` 在当前 DDL（允许 NULL）下的行为是否会意外产生新的脏数据（实际上不会——`SET deleted = true` 写的是 1 而非 NULL）
  - 读者也无法确认迁移到 NOT NULL 后 `@SQLDelete` 是否受影响（实际上不受影响）
  - 在 Issue 3 的"修复方案潜在副作用分析"（第 619-624 行）中也没有提及 `@SQLDelete`
- **所在位置**：Issue 3 章节——影响范围（第 628-643 行）、潜在副作用分析（第 619-624 行）
- **严重程度**：轻微（不影响修复方案正确性，但上下文完整性有缺憾）
- **改进建议**：在 Issue 3 的"修复方案潜在副作用分析"中补充 `@SQLDelete` 行为说明：当前 DDL 下 `@SQLDelete` 将软删除置为 1（非 NULL），不会因 DDL 允许 NULL 而产生新的脏数据；迁移添加 NOT NULL 后 `@SQLDelete` 行为一致、不受影响。

### 问题 2：NOT NULL 约束测试的预期异常类型可能需要验证

- **问题描述**：`user_shouldRejectNullPassword()`（第 75-89 行）和 `user_shouldEnforceUserTypeNotNull()`（第 91-104 行）使用 `assertThrows(DataIntegrityViolationException.class, ...)` 验证 `@Column(nullable = false)` 的约束行为。在 Spring Boot 3.2.5（Hibernate 6.4.x）+ H2 + `@PersistenceContext EntityManager` 的测试环境中，此异常类型的准确性需要验证：
  - 如果 Hibernate 的 null 检查在 flush 时先于数据库约束生效，抛出的是 `org.hibernate.PropertyValueException`（该异常不经过 `@Repository` 的异常转换，因为测试类未标注 `@Repository`）
  - 如果 Hibernate 将约束检查推迟到数据库层面，H2 抛出 SQL 异常，Hibernate 包装为 `org.hibernate.exception.ConstraintViolationException`，同样不会自动转为 `DataIntegrityViolationException`
  - 两种路径下实际异常类型均可能与产出的预期类型不一致
- **所在位置**：Issue 1 User 测试示例——`user_shouldRejectNullPassword()`（第 85 行）、`user_shouldEnforceUserTypeNotNull()`（第 100 行）
- **严重程度**：中等（测试行为在确定性验证前存在 FAIL 风险，且 11 轮审议均未关注此细节）
- **改进建议**：执行者在编写测试前应在当前环境下验证实际抛出异常类型。如果确实是 `PropertyValueException`，将 `assertThrows` 的预期类型改为 `PropertyValueException` 或更宽泛的 `PersistenceException`。统一约束验证测试的异常类型约定并在策略说明中记录。

### 问题 3：createdAt/updatedAt 断言覆盖范围表述与实际不一致

- **问题描述**：第 35 行称"测试已验证其自动填充行为（见各实体基础映射测试中的断言）"，但实际提供的测试代码中仅 `user_shouldMapUsernameField`（第 71-72 行）包含 `assertNotNull(found.getCreatedAt())` 和 `assertNotNull(found.getUpdatedAt())`。Role 组的基础映射测试 `role_shouldMapCodeField`（第 188-202 行）和 Post 组的 `post_shouldMapManyToOneRole`（第 255-280 行）均未包含 createdAt/updatedAt 断言。"各实体"的表述过度概括。
- **所在位置**：Issue 1 ——"需验证的映射点"表格后的段落（第 35 行）
- **严重程度**：轻微
- **改进建议**：将"各实体基础映射测试"改为"User 基础映射测试"，或在 Role/Post 测试中也补全 createdAt/updatedAt 断言以使覆盖标准在各实体间对齐。

## 整体质量评价

产出经过 11 轮迭代审议，质量已达到较高水平。需求四个方面均被充分响应，事实错误已基本消除，优先级排序和时序依赖分析合理，修复指引具体可操作。上述三个问题均不是阻碍执行的重大问题：问题 1 为上下文完整性补充，问题 2 需执行者验证确认，问题 3 为表述精度调整。
