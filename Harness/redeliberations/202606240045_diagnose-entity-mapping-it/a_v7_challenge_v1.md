# 诊断质询报告（v7）

## 质询结果

LOCATED

## 逐维度审查

### 1. 证据充分性

**[通过]** `User.java:28` 缺少 `@Column(nullable = false)` 的根因判定与代码一致，已交叉验证实体源码。

**[通过]** `BaseEntity.java:37` 标注 `@Column(nullable = false)` 而 `schema.sql` 16 张表的 `deleted` 列均缺少 `NOT NULL` 的判定，经逐表核对 schema.sql 确认无误。

**[通过]** Issue 2 代码路径排查结论（无生产代码通过 `UserRepository.save()` 持久化 User）已通过读取 `UserRepository.java`（空接口，仅继承 JpaRepository）和 `integration/pom.xml:53-56`（common-module-impl 声明为 test scope）验证。

**[通过]** Issue 4 中 `Function.java:30`（enabled）和 `Function.java:54`（visible）缺少 Java 默认值的判定与实际源码一致。

**[通过]** `user_shouldRejectNullPassword()` 测试注释已包含 DDL 依赖标注，明确了 Issue 2 修复前提条件。

### 2. 逻辑完整性

**[通过]** 从问题现象到根因的因果链完整：Issue 2（字段缺少注解 → 约束缺失 → 脏数据可写入），Issue 3（DDL 遗漏 NOT NULL → 与实体不一致 → @SQLRestriction 下 NULL 被静默过滤 → 修复后行为变化），逻辑无跳跃。

**[通过]** `@SQLRestriction("deleted = false")` 的 SQL 三值逻辑行为分析正确（`NULL = 0` → UNKNOWN → WHERE 视为 FALSE），迁移方案已据此评估业务影响。

**[通过]** Issue 3 迁移方案已在 v7 中补充业务治理指引，包含预审清单、分类处理策略、执行窗口通知、回滚方案，逻辑完整性满足业务治理要求。

**[通过]** 优先级排序（P0-P3）有量化依据和交叉对比，时序依赖关系清晰合理。

### 3. 覆盖完备性

**[通过]** 原始用户需求的 4 个问题（EntityMappingIT 缺少测试、password 无 NOT NULL、deleted NOT NULL 不一致、enabled/visible 无默认值）全部覆盖。

**[通过]** 本轮迭代要求的 4 个反馈项全部得到响应：
- Issue 3 业务治理指引 → 新增「业务影响评估」章节（SELECT 清单、分类策略、执行窗口、回滚方案）
- UserRepository 计数偏差 → 已移除具体数值改用定性描述
- M2 映射点语义 → 改为应然表述，关联 Issue 2 移至备注列
- DDL 依赖标注 → 测试注释已补充

**[通过]** 已覆盖第 5 轮质询指出的 UserRepository 计数偏差修复。

**[通过]** 所有 15 个映射点（M1-M15）的代码位置与实体源码、schema.sql 完全吻合。

## 质询要点

（无严重/一般问题）
