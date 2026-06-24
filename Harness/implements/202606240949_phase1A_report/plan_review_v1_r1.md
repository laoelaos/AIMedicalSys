# 计划审查报告（v1 r1）

## 审查结果
REJECTED

## 发现

### [一般] Issue 1 测试方法数量与诊断报告实际内容不一致
计划路线表第 4 项标注 EntityMappingIT 新增测试为"中(7个测试方法)"，但诊断报告（03_phase1A_report.md）中已提供 **11 个**测试方法示例（User 6 个 + Role 3 个 + Post 2 个）。诊断报告经多轮修订后测试数量已从 v5 的 7 个增长至 11 个（新增 user_shouldEnforceUserTypeNotNull、role_shouldEnforceCodeUniqueConstraint 等），计划未同步更新。该偏差虽不影响 R1 执行，但路线表的概要信息不准确，可能误导后续轮次的资源估算与验证期望。

### [一般] R1 任务范围偏离需求——排除 schema.sql DDL 修改
task_v1.md 声明"仅改 Java 注解，DDL 在生产环境中单独变更"，但诊断报告的 Issue 2 修复指引明确要求"将 schema.sql:16 的 DEFAULT NULL 改为 NOT NULL。两处改动需同步进行"。R1 排除了 schema.sql 的同步修改，导致：
- 开发环境若从头执行 schema.sql 建库，password 列仍允许 NULL，与实体注解不一致
- schema.sql 作为源码文件应随实体注解一同更新，推迟到生产部署阶段单独处理缺乏代码库层面的可追溯性

## 修改要求

1. **Issue 1 测试数量修正**：将路线表中 Issue 1 的复杂度/数量标注从"7个测试方法"修正为与诊断报告一致的 11 个，或改为模糊描述（如"多组测试方法"）避免数字偏差。

2. **R1 范围明确**：在 task_v1.md 中做以下二选一的修正：
   - **方案 A（推荐）**：将 schema.sql 的 DDL 修改纳入 R1 范围，在 task_v1.md 中补充对 schema.sql:16 的修改指令（`DEFAULT NULL` → `NOT NULL`），与 Java 注解修改同批次提交。
   - **方案 B（保守）**：如确有理由将 schema.sql 修改推迟到独立变更，需在 task_v1.md 中明确说明推迟理由和追踪方式（如"将在 Issue 3 的 schema.sql 批量修改中一并处理"或"创建独立缺陷追踪单"），消除与诊断报告修复指引的语义矛盾。
