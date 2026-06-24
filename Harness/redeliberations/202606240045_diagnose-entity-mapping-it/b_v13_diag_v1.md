# 质量审查报告：a_v13_diag_v1.md

## 审查范围

当前审查针对第 13 轮迭代产出 `a_v13_diag_v1.md`，从可操作性视角评估诊断报告的质量。审查维度包括：事实准确性、内部一致性、关键遗漏、逻辑矛盾。

---

## 发现的问题

### 问题 1：Issue 2 DDL 变更在建议修复顺序中被标注为"可选"，与 Issue 2 自身修复指引矛盾

- **问题描述**：建议修复顺序的 Phase 2 第 5 步写为"Issue 2 DDL 变更（可选）：ALTER TABLE sys_user MODIFY COLUMN password VARCHAR(128) NOT NULL"，将 ALTER TABLE 标记为"可选"。但 Issue 2 的修复指引（第 381 行）明确要求"将 schema.sql:16 的 DEFAULT NULL 改为 NOT NULL"，且明确指出"两处改动需同步进行"。对于生产数据库，schema.sql 与实体注解不一致正是本 Issue 的核心问题之一，仅改 Java 注解而不变更生产 DDL 则约束在数据库中始终未生效。标注"可选"会误导执行者跳过必要步骤，导致修复不完整。
- **所在位置**：a_v13_diag_v1.md:867
- **严重程度**：中等
- **改进建议**：移除"（可选）"标注，直接写为"Issue 2 DDL 变更：ALTER TABLE sys_user MODIFY COLUMN password VARCHAR(128) NOT NULL"。或在标注同时明确说明何种条件下可跳过（如仅在新初始化数据库且 schema.sql 已更新时可跳过 ALTER TABLE），否则应默认必选。

### 问题 2：Issue 2 主操作顺序建议未显式纳入与 Issue 4 的交叉数据清理时序依赖

- **问题描述**：Issue 2 的"操作顺序建议"（第 424 行）仅列出"①预检 → ②决策 → ③数据清理 → ④DDL → ⑤验证"五步，未在此处提及与 Issue 4 清理的顺序依赖。尽管第 426-436 行的"交叉数据冲突"章节已详细分析，但该章节是独立于主操作顺序之外的补充说明。仅阅读 Issue 2 章节的执行者可能直接按主操作顺序执行，在第③步运行 Issue 2 方案 B（`SET enabled = 0 WHERE password IS NULL`）而遗漏 Issue 4 的前置清理，导致安全漏洞。可操作性取决于执行者是否能从 Issue 2 的主流程中自然发现交叉依赖。
- **所在位置**：a_v13_diag_v1.md:422-424
- **严重程度**：一般
- **改进建议**：在"操作顺序建议"第③步之前增加一句显式引用，例如："⚠ 执行数据清理前，必须确认 Issue 4 的 enabled 清理已完成（顺序：先 Issue 4 enabled 清理 → 再 Issue 2 数据清理，详见下节'与 Issue 4 清理策略的交叉数据冲突'）。"让交叉依赖在主操作流中可见，降低遗漏风险。

### 问题 3：@ColumnDefault 示例值 `"true"` 与项目 DDL 约定不一致

- **问题描述**：方案 B 的表格中写 `@ColumnDefault("true")`（第 773 行），但该项目 schema.sql 中所有 Boolean 字段的 DDL 默认值均使用 `DEFAULT 1`（数字字面量），而非 `DEFAULT true`。虽然 MySQL 可接受 `DEFAULT true`，但该值和项目现有 DDL 风格不一致——所有 `enabled`/`visible` 列的 DDL 均为 `DEFAULT 1`。使用 `@ColumnDefault("true")` 生成的 DDL 会变成 `DEFAULT true`，与手工维护的 schema.sql 出现格式差异。加之项目代码中不存在任何 `@ColumnDefault` 使用，引入新注解时应与现有 DDL 风格对齐。
- **所在位置**：a_v13_diag_v1.md:773
- **严重程度**：轻微
- **改进建议**：将 `@ColumnDefault("true")` 改为 `@ColumnDefault("1")`，以匹配项目 DDL 中 `DEFAULT 1` 的约定，避免生成不一致的 DDL。

### 问题 4：Issue 3 的 @SQLRestriction 验证步骤注释存在表述落差

- **问题描述**：步骤 4(C) 的第二条 SQL 查询（`SELECT COUNT(*) AS still_null FROM sys_user WHERE deleted IS NULL;`）注释写为"确认 deleted IS NULL 的记录已被过滤"。该查询实际验证的是 UPDATE 清理后不再存在 NULL 记录，属于"数据清理结果确认"而非"@SQLRestriction 过滤行为验证"。"已被过滤"一词暗示被 @SQLRestriction 的 WHERE 子句过滤，实则是 UPDATE 语句将 NULL 更新为 0 的结果。虽然该段末尾（第 594 行）的补充说明提及了应用层 Hibernate 查询验证，但 SQL 查询自身的注释与真实语义存在落差。
- **所在位置**：a_v13_diag_v1.md:589-591
- **严重程度**：轻微
- **改进建议**：将该行注释改为"确认清理完成：所有 NULL 已被 UPDATE 为 0，再无隐藏记录"。将 SQL 级别的验证和 Hibernate 层面的过滤验证在语义上明确区分。

---

## 整体质量评价

经过 13 轮迭代，产出质量已达到较高水平。四个 Issue 的诊断和分析均完整，修复方案可操作性强，跨问题交叉影响已被系统性排查标注。本次审查发现的 4 个问题中，1 个属中等严重度（直接影响修复完整性），其余为一般至轻微。建议修复者优先处理问题 1 和问题 2，问题 3 和问题 4 可在修订时顺带修正。
