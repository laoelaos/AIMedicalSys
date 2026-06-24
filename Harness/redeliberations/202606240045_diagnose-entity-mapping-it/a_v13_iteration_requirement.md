根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

1. **[严重] 建议修复顺序与数据清理时序依赖矛盾**：第804行推荐执行顺序为"Issue 2 → Issue 3 → Issue 4 → Issue 1"，但第424-433行的交叉数据冲突分析明确指出Issue 4的数据清理SQL必须在Issue 2的方案B之前执行，否则产生安全漏洞（无密码且无enabled值的记录被误启用）。报告将"代码修复"与"数据清理"混合在同一线性顺序中，未区分两个维度。**改进建议**：将推荐顺序改为两阶段（阶段一：代码修复；阶段二：数据清理），或在推荐顺序中显式标注Issue 2数据清理步骤必须延后至Issue 4数据清理之后，并补充"数据清理执行顺序"子章节。

2. **[中等] Issue 3 回滚方案未还原 ALTER TABLE 约束变更**：第605-647行回滚方案仅从备份表恢复deleted列的数据值，但ALTER TABLE MODIFY COLUMN已施加的NOT NULL约束未被撤销。回滚后列仍为NOT NULL，数据库schema状态与回滚前（允许NULL）不一致。**改进建议**：在回滚方案中补充步骤：对每张表执行ALTER TABLE MODIFY COLUMN移除NOT NULL约束，恢复至迁移前的列定义；或说明回滚后列约束状态与回滚前存在的差异并确认业务可接受。

3. **[一般] Issue 3 迁移验证步骤遗漏 DEFAULT 行为和 @SQLRestriction 行为验证**：第552-566行验证步骤仅包含两项：(A)查询INFORMATION_SCHEMA确认列变更为NOT NULL；(B)插入NULL值验证被拒绝。缺少两项关键验证——DEFAULT行为验证（插入不指定deleted值的行验证DEFAULT 0是否生效）和@SQLRestriction后置验证（迁移后旧数据和新增数据在`deleted = false`过滤下是否正常返回）。后者是Issue 3被提升至P1的核心业务影响维度。**改进建议**：在验证步骤中补充插入不指定deleted的行并verify其值为0；执行Hibernate查询确认deleted=0的记录可被正常返回且deleted IS NULL的记录已被过滤。

4. **[一般] Issue 2 的 ALTER TABLE 表锁风险未被纳入副作用分析（与Issue 3论证标准不对称）**：第382-398行Issue 2副作用分析未提及ALTER TABLE表锁风险，而Issue 3的副作用分析（第654行）明确提及大表锁问题。两处同类操作的风险分析标准不一致，降低整体可信度。**改进建议**：在Issue 2副作用分析中补充与Issue 3一致的表锁风险说明，标注sys_user表规模和预估影响，并给出与Issue 3相同的执行窗口建议（低峰期执行或使用pt-online-schema-change）。

5. **[一般] M10（Role.users @ManyToMany mappedBy端）历经12轮仍标注"未覆盖"但未提供处理策略**：第28行映射点表M10备注列标注"未覆盖"，第313-318行@ManyToMany映射测试策略说明亦未给出处理建议。此缺口最早在v8修订中被标注，历经12轮迭代仍未填补。**改进建议**：针对M10提供明确建议——(A)补充`role_shouldMapManyToManyUsers`测试方法验证双向映射正确性；或(B)明确论证"M4已覆盖正向映射，M10作为mappedBy端由Hibernate自动维护，无需单独测试"并给出跳过理由。

## 历史迭代回顾

- **已解决的问题**（出现在历史反馈但当前反馈中不再提及）：第1-11轮中的各类问题——缺少修复方案、@SQLRestriction行为偏差、测试代码逻辑矛盾、clear()使用风格不一致、UserRepository代码路径排查、命名不一致（如user_shouldMapUsernameUniqueConstraint/role_shouldMapCodeUniqueConstraint）、Post测试缺失deleted断言、交叉对比表论证标准不对称、Issue 4缺少与Issue 2的交叉引用、回滚方案覆盖范围不完整等——均已在之前迭代中解决，当前反馈不再提及。
- **持续存在的问题**（在多轮反馈中反复出现，需重点解决）：以上5个问题均为第12轮已提出但a_v12_diag_v1.md仍未解决的问题，本轮需重点解决。
- **新发现的问题**：无。

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606240045_diagnose-entity-mapping-it\a_v12_diag_v1.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606240045_diagnose-entity-mapping-it\requirement.md
