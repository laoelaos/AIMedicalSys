# 设计审查报告（v4 r1）

## 审查结果
APPROVED

## 发现
- **[轻微]** 索引 `columnList` 中使用驼峰命名（`drugCode, routeOfAdministration`）。`SpringPhysicalNamingStrategy` 默认将字段名映射为蛇形（`drug_code, route_of_administration`），`@Index.columnList` 是否自动应用命名策略取决于 Hibernate 版本实现细节。建议同步使用蛇形或确认 Hibernate 6 的解析行为，以避免 DDL 生成时索引引用列名不匹配的风险。
- **[轻微]** 缺少 `@EqualsAndHashCode(callSuper = true)`。`@Data` 生成的 `equals()/hashCode()` 不包含继承的 `id` 字段，可能导致 JPA 实体在跨会话比较时出现标识不一致。注意：项目中 `DoctorEntity` 也存在同类写法，属已有代码惯例问题。

## 修改要求
无需修改。
