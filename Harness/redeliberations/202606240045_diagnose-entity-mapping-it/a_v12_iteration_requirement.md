根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 问题 1：Issue 4 章节缺失与 Issue 2 清理策略的交叉引用

- **问题描述**：Issue 2 章节（第 423-433 行）详细分析了其清理 SQL（`SET enabled = 0 WHERE password IS NULL`）与 Issue 4 清理 SQL（`SET enabled = 1 WHERE enabled IS NULL`）之间的执行顺序依赖，明确指出先执行 Issue 4 再执行 Issue 2 的推荐顺序。但 Issue 4 自身的交叉影响备注（第 679 行）仅指向 Issue 1，未提及 Issue 2。如果执行者的工作范围涉及 Issue 4 而尚未阅读 Issue 2 的完整内容（例如按优先级顺序，Issue 4 在 P2 但执行者跳读或被分配单独修复 Issue 4），可能忽略此时序约束，导致清理 SQL 执行顺序颠倒，产生安全漏洞（无密码且无 enabled 值的记录被误置为启用）。
- **所在位置**：Issue 4 章节——交叉影响备注（第 679 行）、清理 SQL（第 697-706 行）
- **严重程度**：中等
- **改进建议**：在 Issue 4 的"交叉影响备注"中增加对 Issue 2 的引用，或直接在清理 SQL 前方以注释形式标注执行前提"此 SQL 应在 Issue 2 的方案 B（`SET enabled = 0 WHERE password IS NULL`）之前执行，参见 Issue 2 交叉数据冲突分析"。

### 问题 2：Issue 3 回滚方案覆盖范围不明确

- **问题描述**：Issue 3 的回滚方案（第 606-615 行）以 `sys_user` 表为例展示了备份创建（`CREATE TABLE sys_user_bak_YYYYMMDD AS SELECT * FROM sys_user`）和回滚 UPDATE 语句，其余 15 张表以"每张表逐一执行类似操作"注释带过。第 608 行仅说"每张表逐一执行类似操作"，第 612-614 行的回滚 SQL 写死了 `sys_user_bak_YYYYMMDD` 表名。从可操作性视角，该方案要求执行者为 16 张表逐一构建备份和回滚 SQL，但未提供完整模板，也无统一备份命名约定说明。实际执行中可能出现部分表遗漏备份、备份表命名不一致导致回滚脚本要逐张调整等风险。
- **所在位置**：Issue 3 生产迁移方案——回滚方案（第 606-615 行）
- **严重程度**：一般
- **改进建议**：仿照步骤 2 中 16 条 UPDATE 语句的完整罗列模式，提供 16 张表的备份和回滚语句模板，或明确标注统一命名约定（如 `{table_name}_bak_YYYYMMDD`）及操作要求"为全部 16 张表逐一执行备份及回滚操作，将上述模板中的 sys_user 替换为对应表名"。

## 历史迭代回顾

### 已解决的问题

- 迭代 1~3 的"缺少修复方案"——已在早期轮次解决
- 迭代 3~4 的"@SQLRestriction 事实错误"——已在 v5 修正
- 迭代 5 的"测试与 Issue 2 互斥"——已在 v6 修正
- 迭代 5~6 的"代码路径排查不完整"——已在 v6~7 补充
- 迭代 6~7 的"缺少业务治理指引"——已在 v7 补充
- 迭代 7~8 的"交叉对比表断言过强"——已在 v8~9 修正
- 迭代 8 的"测试环境能力边界说明缺失"——已在 v9 补充
- 迭代 8~9 的"Issue 4 副作用分析论证不对称"——已在 v9 补充
- 迭代 9 的"role_shouldMapCodeUniqueConstraint 方法名"——已在 v10 修正
- 迭代 10 的"Post 缺少 deleted 断言"——已在 v11 修正
- 迭代 10 的"M11 描述不一致"——已在 v11 修正
- 迭代 10 的"Issue 4 脏数据论证不对称"——已在 v11 修正
- 迭代 11 的"审计字段测试覆盖"——已在 v11 修正

### 持续存在的问题

- **Issue 4 与 Issue 2 交叉引用不完整**（第 8 轮 → 第 11 轮 → 本轮）：第 8 轮要求在报告中标注两条清理策略的时序冲突，已在 Issue 2 章节补充"交叉数据冲突分析"；第 11 轮指出 Issue 4 章节未同步增加对 Issue 2 的回向引用；本轮再次指出 Issue 4 章节仍缺少此交叉引用。属于跨章节追踪一致性问题，需在 Issue 4 章节中同步补充对 Issue 2 的引用。
- **Issue 3 回滚方案覆盖范围不完整**（第 6 轮 → 第 11 轮 → 本轮）：第 6 轮要求补充回滚方案等业务治理指引，已在 v7 补充基础模板（以 sys_user 为例）；第 11 轮指出模板仅覆盖 1 张表而非全部 16 张表；本轮再次指出仍需完整罗列或明确统一命名约定。属于可操作性细节逐步收敛问题。

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606240045_diagnose-entity-mapping-it\a_v11_diag_v1.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606240045_diagnose-entity-mapping-it\requirement.md
