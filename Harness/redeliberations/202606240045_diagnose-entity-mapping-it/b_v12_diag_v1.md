# 质量审查报告

## 审查概况

- **待审查产出**：`a_v12_diag_v1.md`（第 12 轮诊断报告）
- **审查视角**：可操作性、需求响应充分度、整体深度和完整性
- **审查结论**：产出质量较高，经过 11 轮迭代修正后无重大事实错误，但存在 5 个可操作性问题，其中 1 个严重、1 个中等、3 个一般。

---

## 发现的问题

### 问题 1：[严重] 建议修复顺序与数据清理时序依赖矛盾

- **所在位置**：第 804 行（建议修复顺序）与第 424-433 行（Issue 2 交叉数据冲突分析）
- **问题描述**：第 804 行推荐执行顺序为 "Issue 2 (P0) → Issue 3 (P1) → Issue 4 (P2) → Issue 1 (P3)"。但第 424-433 行的交叉数据冲突分析明确指出：Issue 4 的数据清理 SQL（`SET enabled = 1 WHERE enabled IS NULL`）**必须**在 Issue 2 的方案 B（`SET enabled = 0 WHERE password IS NULL`）之前执行，否则产生安全漏洞（无密码且无 enabled 值的记录会被误启用）。报告将"代码修复"与"数据清理"混合在同一线性顺序中，未区分两个维度。若执行者严格按推荐顺序先处理 Issue 2 数据清理，会绕开交叉分析中已识别的时序约束。

- **改进建议**：
  - 将推荐顺序改为两阶段：**阶段一（代码修复）**：Issue 2 代码修复 → Issue 4 代码修复 → Issue 3 代码修复 → Issue 1 测试新增；**阶段二（数据清理）**：Issue 4 清理 SQL → Issue 2 清理 SQL → Issue 3 迁移 ALTER TABLE。或
  - 在推荐顺序中显式标注 Issue 2 的数据清理步骤必须延后至 Issue 4 数据清理之后，并在建议执行顺序下方补充"数据清理执行顺序"子章节。

---

### 问题 2：[中等] Issue 3 回滚方案未还原 ALTER TABLE 约束变更

- **所在位置**：第 605-647 行（回滚方案）
- **问题描述**：回滚方案仅从备份表恢复 deleted 列的数据值（`SET t.deleted = b.deleted`），但 ALTER TABLE MODIFY COLUMN 已施加的 NOT NULL 约束未被撤销。回滚后列仍为 NOT NULL，新插入数据若不指定 deleted 值虽可走 DEFAULT 0，但数据库 schema 状态与回滚前（允许 NULL）不一致。若需完全回滚至原始状态，应额外执行 ALTER TABLE 移除 NOT NULL。

- **改进建议**：在回滚方案中补充步骤："回滚 ALTER TABLE：对每张表执行 ALTER TABLE MODIFY COLUMN `deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除'，移除 NOT NULL 约束，恢复至迁移前的列定义。"或说明回滚后列约束状态与回滚前存在的差异并确认业务可接受。

---

### 问题 3：[一般] Issue 3 迁移验证步骤遗漏 DEFAULT 行为和 @SQLRestriction 行为验证

- **所在位置**：第 552-566 行（验证约束生效）
- **问题描述**：验证步骤仅包含两项：(A) 查询 INFORMATION_SCHEMA 确认列变更为 NOT NULL；(B) 插入 NULL 值验证被拒绝。缺少两项关键验证：
  - **DEFAULT 行为验证**：ALTER TABLE MODIFY 后，插入不指定 deleted 值的行，验证 DEFAULT 0 是否仍生效（`INSERT INTO sys_user (...) VALUES (...)` 不加 deleted 列）。
  - **@SQLRestriction 后置验证**：迁移后旧数据（deleted=0）和新增数据在 `@SQLRestriction("deleted = false")` 过滤下是否正常返回。这是 Issue 3 被提升至 P1 的核心业务影响维度，验证环节却未覆盖。

- **改进建议**：在验证步骤中补充：(A) 插入不指定 deleted 的行并 verify 其值为 0；(B) 执行 Hibernate 查询（如 entityManager.find）确认 deleted=0 的记录可被正常返回且 deleted IS NULL 的记录已被过滤。

---

### 问题 4：[一般] Issue 2 的 ALTER TABLE 表锁风险未被纳入副作用分析（与 Issue 3 论证标准不对称）

- **所在位置**：第 382-398 行（Issue 2 修复方案潜在副作用分析）
- **问题描述**：Issue 3 的副作用分析（第 654 行）明确提及大表锁问题："ALTER TABLE MODIFY COLUMN 在 MySQL 中可能引发表锁或重建表。对于大表（如 sys_user），建议在低峰期执行，或使用 pt-online-schema-change 等工具减少锁时间。" 但 Issue 2 同样需要 ALTER TABLE MODIFY COLUMN（在第 378-380 行要求将 `DEFAULT NULL` 改为 `NOT NULL`），却完全未提及表锁风险。虽然 Issue 2 只影响 1 张表，但 `sys_user` 可能是系统中最大的表，该风险不应被忽略。两处同类操作的风险分析标准不一致，降低整体可信度。

- **改进建议**：在 Issue 2 副作用分析中补充与 Issue 3 一致的表锁风险说明，标注 `sys_user` 表规模和预估影响，并给出与 Issue 3 相同的执行窗口建议（低峰期执行或使用 pt-online-schema-change）。

---

### 问题 5：[一般] M10（Role.users @ManyToMany 双向映射）历经 12 轮仍标注"未覆盖"但未提供处理策略

- **所在位置**：第 28 行（映射点表 M10 行）、第 313-318 行（@ManyToMany 映射测试策略说明）
- **问题描述**：映射点表 M10（Role.users @ManyToMany(mappedBy = "roles")）备注列标注"未覆盖"。此缺口最早在 v8 修订中被标注（第 868 行第 5 项），历经 12 轮迭代仍未填补。报告未给出任何处理建议——是应补充测试、可安全跳过、还是留待后续。M10 与 M4（User.roles）互为 inverse 端，User 侧的测试已验证正向映射，Reverse 端是否测试取决于项目约定。当前产出将决策压力完全留给执行者。

- **改进建议**：在映射点表之后或 @ManyToMany 策略说明中，针对 M10 提供明确的建议：(A) 补充 `role_shouldMapManyToManyUsers` 测试方法（示例约 10-15 行代码），以验证 mappedBy 双向映射正确性；或 (B) 明确论证"M4 已覆盖正向映射，M10 作为 mappedBy 端由 Hibernate 自动维护，无需单独测试"，并给出跳过理由。当前"未覆盖"标注属于信息陈述而非可操作指引，修复者无法据此决策。

---

## 整体质量评价

经过 11 轮迭代修正后，产出在事实准确性、结构完整性和需求响应覆盖面上已达到较高水准。4 个原始需求均被完整回应，修复指引精细到文件:行号级，副作用分析多数经过实际代码搜索验证，交叉影响分析基本到位。当前残留的 5 个问题主要集中在部分结论之间的逻辑一致性（推荐顺序与清理依赖的冲突）、回滚方案的技术完整性（未还原约束变更）、以及个别验证/分析项目的标准对称性上。修正后该产出达到交付标准。
