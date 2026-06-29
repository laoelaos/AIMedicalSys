根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 1. [中等] Issue 2 交叉对比表中"已有生产脏数据"表述与根因分析结论不一致

交叉对比表"是否已有生产脏数据"维度将 Issue 2 评为"是"，排序理由中也断言"已有生产脏数据存在"。但 Issue 2 根因分析结论是"当前不存在任何生产代码路径或自动化的数据脚本会向 sys_user 表插入 password 为 NULL 的记录"，后续脏数据来源说明使用推测语气。证据链只能支撑"暂未发现代码路径会导致新脏数据，生产库中可能存在遗留脏数据"，无法直接支撑"已有生产脏数据存在"这一肯定性断言。

**改进建议**：
1. 将交叉对比表中 Issue 2 该维度值改为"可能（未经验证）"
2. 在排序理由中将"已有生产脏数据存在"改为"约束缺失本身是安全合规问题，且脏数据若存在则业务影响大"
3. 建议在执行者修复前优先执行预检 SQL 确认脏数据实际量

### 2. [一般] `user_shouldMapUsernameUniqueConstraint` 方法名称与实际验证内容不匹配

方法名包含"UniqueConstraint"暗示对 username 唯一约束的实际验证（预期插入重复值后抛出 DataIntegrityViolationException），但实际测试体仅执行 persist/flush/find 基本映射验证，唯一约束是否生效并未被验证。

**改进建议**：二选一：
- (A) 将方法重命名为 `user_shouldMapUsernameField`，移除名称中的"UniqueConstraint"误导
- (B) 补充一个独立的测试方法 `user_shouldEnforceUsernameUniqueConstraint`，通过 persist 两个相同 username 的 User 来验证 DataIntegrityViolationException。推荐 (B)

### 3. [轻微] Issue 4 缺少 DDL 行号定位

Issue 4 为实体字段提供了 Java 文件行号（如 User.java:36），但未提供 DDL 中对应 enabled/visible 列的行号。相比之下，Issue 3 为 16 张表逐行标注了 schema.sql 行号。可操作性上不对称。

DDL 行号：
- sys_user.enabled → schema.sql:21
- sys_role.enabled → schema.sql:40
- sys_post.enabled → schema.sql:60
- sys_function.enabled → schema.sql:90
- sys_function.visible → schema.sql:86

**改进建议**：在 Issue 4 的「现象」或「修复指引」节中补充各字段在 schema.sql 中的行号。

### 4. [轻微] 交叉对比表"运行时异常风险"维度标签与 Issue 2 描述语义不匹配

交叉对比表中"运行时异常风险"行对 Issue 2 的描述为"有（NULL 写入不受阻）"。NULL 写入不受阻描述的是数据完整性风险，而非运行时异常风险。维度标签与描述语义不匹配。

**改进建议**：将该行标签调整为"数据完整性风险"或"数据质量风险"，并将 Issue 2 的描述改为"有（NULL 可写入数据库，产生脏数据）"，与"运行时异常风险"分离。

### 5. [轻微] Role/Post 测试组对映射点表中列出的部分验证点缺少显式覆盖

「需验证的映射点」表列出了各实体的完整验证点，但 Role 测试组（2 个方法）和 Post 测试组（2 个方法）的显式覆盖率低于 User 测试组（5 个方法）：
- enabled 字段映射（M8/M12）：Role/Post 测试中设置了 enabled 但未断言
- @ManyToMany users（M10）：Role 未覆盖
- code 唯一约束映射（M11）：Post 未覆盖

**改进建议**：
- 为 Role 补充一个简单的 `role_shouldMapEnabledField` 测试，验证 enabled 字段可正常持久化和读取
- 为 Post 补充对 code 字段映射的基本验证
- 或在映射点表的备注列补充各点的测试覆盖状态

## 历史迭代回顾

### 已解决的问题（出现在历史反馈但当前反馈中不再提及）
- **R1-R6 全部问题均已解决**：修复方案缺失、优先级排序缺失、跨问题影响分析、可操作性不足、需求响应缺失、Issue 4 权衡分析、Issue 2 脏数据行动指引、Issue 3 生产迁移方案、副作用分析、优先级排序论证、时序依赖矛盾、@SQLRestriction 行为错误、Issue 1 可操作性、Issue 1 与 Issue 2 逻辑矛盾、脏数据陈述断层、clear() 不一致、UserRepository 搜索不完整、Issue 3 业务治理指引 —— 以上问题均已在 v7 版本中响应并通过质询确认（LOCATED）。

### 持续存在的问题（在多轮反馈中反复出现，需重点解决）
- **交叉对比表"已有生产脏数据"表述**：第 7 轮已指出该问题，v7 版本未完全修正，本轮再次检出。
- **`user_shouldMapUsernameUniqueConstraint` 方法名不匹配**：第 7 轮已指出该问题，v7 版本未采纳改进建议，本轮再次检出。

### 新发现的问题（本轮新识别）
- Issue 4 缺少 DDL 行号定位
- 交叉对比表"运行时异常风险"维度标签语义不匹配
- Role/Post 测试组对映射点表中列出的部分验证点缺少显式覆盖

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606240045_diagnose-entity-mapping-it\a_v7_diag_v1.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606240045_diagnose-entity-mapping-it\requirement.md
