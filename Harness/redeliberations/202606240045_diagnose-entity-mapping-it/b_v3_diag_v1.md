# 质量审查报告：a_v3_diag_v1.md

审查范围：需求响应充分度、事实准确性、深度/完整性、可操作性

---

## 问题 1：[严重] Issue 3（deleted NOT NULL 不一致）缺少生产数据库迁移方案

**所在位置**：Issue 3 章节「修复指引」（第131-133行）

**问题描述**：Issue 2 提供了完整的生产脏数据行动指引（预检 SQL + 三种清理策略 + 操作顺序建议），但 Issue 3 的修复指引仅提示修改 `schema.sql` 中的 DDL 定义。报告自身在「影响范围」中已指出"手工 DDL 创建的数据库可能包含 `deleted IS NULL` 的脏数据"——但未提供任何脏数据预检、清理策略或 ALTER TABLE 迁移语句。对已存在的生产数据库，直接执行 `ALTER TABLE ... MODIFY COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0` 会因现有 NULL 值而失败。执行者拿到报告后无法直接对生产库操作。

**严重程度**：严重

**改进建议**：为 Issue 3 补充类似 Issue 2 的生产库操作指引——(a) 脏数据预检 SQL（`SELECT COUNT(*) FROM 各表 WHERE deleted IS NULL`）；(b) 清理策略选项（人工补录/批量更新为 0 / 标记作废）；(c) ALTER TABLE 语句模板（按表逐个生成或通过脚本批量生成）；(d) 操作顺序建议（先清理 → 后 DDL → 再验证约束生效）。

---

## 问题 2：[中等] 文件路径格式不统一

**所在位置**：全文多处

**问题描述**：报告中不同文件使用不同的路径表达方式：
- `EntityMappingIT.java` 标注为 `AIMedical\backend\integration\src\test\java\...`（含 `AIMedical\` 前缀 + 反斜杠）
- `User.java` 标注为 `common-module-impl/src/main/java/...`（省略 `AIMedical/backend/modules/common-module/` 前缀 + 正斜杠）
- `schema.sql:16` 仅给出文件名，无任何路径前缀

执行者需要跨文件推断文件位置，增加定位成本，尤其在 IDE 直接搜索时需尝试不同的路径格式。

**严重程度**：中等

**改进建议**：统一文件路径格式，建议所有路径均采用从项目根（`AIMedical/`）开始的相对路径，统一使用正斜杠，例如：
- `AIMedical/backend/integration/src/test/java/com/aimedical/integration/EntityMappingIT.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/User.java`
- `AIMedical/backend/application/src/main/resources/db/schema.sql`

---

## 问题 3：[中等] Issue 1 交叉影响备注存在时序依赖矛盾

**所在位置**：Issue 1「交叉影响备注」（第25行）和 Issue 1「修复指引」（第29行）

**问题描述**：Issue 1 的交叉影响备注及修复指引均要求"测试代码需显式调用 `setEnabled(true/false)`，因 Issue 4 的 enabled 无默认值问题"。然而优先级排序中将 Issue 4 列为 P1（"应在新增测试前修复"），且 Issue 4 的推荐方案 A 正是为 enabled 字段添加 Java 默认值 `= true`。这意味着：**如果按优先级先修复 Issue 4（方案 A），Issue 1 的测试代码根本不需要 `setEnabled()` 调用**。报告未对这两种可能路径做出条件化说明，导致执行者可能在不必要的情况下修改测试代码。

**严重程度**：中等

**改进建议**：在 Issue 1 的交叉影响备注中增加条件描述："如果 Issue 4 尚未修复（enabled 字段仍无 Java 默认值），测试代码需显式调用 `setEnabled()`；如果 Issue 4 的方案 A 已实施（即 enabled 已添加 `= true` 默认值），则无需显式设置，但保持显式设置可提高测试可读性。"

---

## 问题 4：[轻微] pom.xml 行号标注偏差

**所在位置**：Issue 1「根因」（第11行）

**问题描述**：报告标注 `integration/pom.xml` 第53-56行包含 `common-module-impl` 依赖声明。经核实，`<dependency>` 起始标签位于第52行，实际完整声明跨第52-56行。标注"53-56行"漏计了第52行的 `<dependency>` 标签，偏差1行。

**严重程度**：轻微

**改进建议**：将行号修正为"第52-56行"。

---

## 总结

| 问题 | 严重程度 | 类型 |
|------|---------|------|
| Issue 3 缺少生产库迁移方案 | 严重 | 关键遗漏 |
| 文件路径格式不统一 | 中等 | 可操作性 |
| Issue 1/4 交叉影响时序矛盾 | 中等 | 逻辑表述 |
| pom.xml 行号偏差 | 轻微 | 事实偏差 |

产出对用户需求的 4 个问题均有诊断和修复指引，需求响应充分。在深度和完整性上，Issue 2 和 Issue 4 的指引最为完善（含生产数据操作指南、方案权衡分析），但 Issue 3 的修复指引存在重大不对称遗漏。优先级排序整体合理。建议优先修复问题 1（补充 Issue 3 生产库迁移方案）和问题 2（统一路径格式）。
