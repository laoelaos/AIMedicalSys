# 诊断质询报告（v9）

## 质询结果

LOCATED

## 逐维度审查

### 1. 证据充分性

**[通过]** Issue 1（测试遗漏）：User/Role/Post 实体存在性及 integration/pom.xml 依赖声明已确认，M1-M15 映射点表格提供精确的文件:行号追溯。✅

**[通过]** Issue 2（password NOT NULL）：已实际搜索 `new User()`（13 处，仅测试代码）、`UserRepository`（无生产代码注入或 save() 调用）、`db/data.sql`（3 条种子用户均非 NULL，且 sql.init.mode=never 不自动执行），证据链完整。✅

**[通过]** Issue 3（deleted NOT NULL 不一致）：16 张表全部列出并标注 schema.sql 行号，经代码验证行号准确。@SQLRestriction 三值逻辑分析正确。✅

**[通过]** Issue 4（enabled/visible 默认值）：已搜索 `getEnabled()`（8 处，全部在单元测试）、`enabled == null`（无匹配）、`.enabled`（仅 MockAiService 配置属性），确认无生产代码依赖 `enabled == null` 特殊语义。✅

**[问题-轻微]** Issue 4 副作用分析中，代码路径搜索覆盖了 `enabled` 的所有相关模式（getEnabled/isEnabled/.enabled），但未搜索 `visible` 对应模式（getVisible/isVisible/.visible），而 `visible` 同样是 Issue 4 的问题字段（Function.java:54）。若 `visible` 的 null 语义风险需评估，搜索范围应一致。但鉴于 `visible` 仅存在于单个实体且 enabled 的搜索已排除全库 null 语义依赖，此遗漏不影响结论方向。

### 2. 逻辑完整性

**[通过]** 从现象到根因的因果链完整：每个 Issue 均以「现象 → 根因 → 修复指引 → 副作用分析 → 影响范围」结构展开，无逻辑跳跃。✅

**[通过]** Issue 2 与 Issue 4 的清理策略交叉冲突已明确标注（380-391 行），并给出推荐的执行顺序。✅

**[通过]** Issue 3 的 @SQLRestriction 行为分析纠正了 SQL 三值逻辑错误，正确解释了 `deleted IS NULL` 被静默过滤而非被"视为未删除"的行为差异，逻辑自洽。✅

**[通过]** 优先级排序的交叉对比分析（影响实体数、业务影响、修复复杂度、数据完整性风险等六维）逻辑一致。时序依赖关系图示清晰。✅

### 3. 覆盖完备性

**[通过]** 原始需求的四个问题全部覆盖：
- EntityMappingIT 缺少集成测试 → Issue 1
- password 无 NOT NULL 约束 → Issue 2
- DDL deleted 列 NOT NULL 不一致 → Issue 3
- enabled/visible 布尔字段缺少默认值 → Issue 4 ✅

**[通过]** v9 迭代需求中的四个改进点全部落实：
1. Issue 4 代码路径搜索 → 已补充（664-669 行）
2. 测试环境能力边界说明 → 已补充（279-300 行）
3. 清理策略交叉数据冲突 → 已补充（380-391 行）
4. 异常类型表述修正 → 已修正（277 行）✅

**[通过]** 诊断结论完整回答了"问题是什么"（四个现象描述）和"为什么发生"（根因分析），修复者可根据修复指引直接采取行动。✅

## 质询要点

无严重/一般问题，无需质询要点。
