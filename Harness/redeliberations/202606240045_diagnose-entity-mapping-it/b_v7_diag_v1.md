# 质量审查报告：a_v7_diag_v1.md

审查时间：2026-06-24
当前迭代：第 7 轮
审查维度：需求响应充分度 / 事实准确性 / 逻辑一致性 / 深度完整性 / 可操作性

---

## 1. [中等] Issue 2 交叉对比表中"已有生产脏数据"表述与根因分析结论不一致

**所在位置**：优先级排序「交叉对比分析」表（a_v7_diag_v1.md:658）及排序理由（:646）

**问题描述**：
交叉对比表"是否已有生产脏数据"维度将 Issue 2 评为"是"。排序理由中也断言"已有生产脏数据存在"。但 Issue 2 根因分析（:323）明确结论——"当前不存在任何生产代码路径或自动化的数据脚本会向 sys_user 表插入 password 为 NULL 的记录"。后续脏数据来源说明（:325）使用"可能已存在"（推测语气），而非确认语气。因此：

- 证据链只能支撑"暂未发现代码路径会导致新脏数据，生产库中**可能**存在遗留脏数据"，无法直接支撑"已有生产脏数据存在"这一肯定性断言
- P0 排序理由中"已存在"的表述夸大了当前可验证的事实

**改进建议**：
1. 将交叉对比表中 Issue 2 该维度值改为"可能（未经验证）"
2. 在排序理由中将"已有生产脏数据存在"改为"约束缺失本身是安全合规问题，且脏数据若存在则业务影响大"
3. 建议在执行者修复前优先执行预检 SQL 确认脏数据实际量，以消除该不确定性

---

## 2. [一般] `user_shouldMapUsernameUniqueConstraint` 方法名称与实际验证内容不匹配

**所在位置**：a_v7_diag_v1.md:52-70（测试方法体及声明）

**问题描述**：
方法名包含"UniqueConstraint"暗示对 username 唯一约束的实际验证（预期插入重复值后抛出 DataIntegrityViolationException）。但实际测试体仅执行 persist/flush/find 基本映射验证，唯一约束是否生效并未被验证。虽然策略说明（:271）标注了完整验证需单独测试，但：

- 方法名会产生误导——未来维护者可能认为该方法已覆盖唯一约束的异常验证
- 缺少一个实际测试唯一约束的独立方法作为模板（例如 `user_shouldEnforceUsernameUniqueConstraint`）

**改进建议**：
二选一：
- (A) 将方法重命名为 `user_shouldMapUsernameField`，移除名称中的"UniqueConstraint"误导
- (B) 补充一个独立的测试方法 `user_shouldEnforceUsernameUniqueConstraint`，通过 persist 两个相同 username 的 User 来验证 DataIntegrityViolationException

推荐 (B)，因为 Issue 1 的修复指引正是要提供"完整的方法示例作为模板"，包含唯一约束验证模板是合理的。

---

## 3. [轻微] Issue 4 缺少 DDL 行号定位

**所在位置**：a_v7_diag_v1.md:562-626（Issue 4 全文）

**问题描述**：
Issue 4 为实体字段提供了 Java 文件行号（如 User.java:36），但未提供 DDL 中对应 enabled/visible 列的行号。相比之下，Issue 3 为 16 张表逐行标注了 schema.sql 行号。可操作性上不对称。

经查证，DDL 行号分别为：
- sys_user.enabled → schema.sql:21
- sys_role.enabled → schema.sql:40
- sys_post.enabled → schema.sql:60
- sys_function.enabled → schema.sql:90
- sys_function.visible → schema.sql:86

**改进建议**：
在 Issue 4 的「现象」或「修复指引」节中，补充各字段在 schema.sql 中的行号，便于修复者快速交叉验证。

---

## 4. [轻微] 交叉对比表"运行时异常风险"维度标签与 Issue 2 描述语义不匹配

**所在位置**：a_v7_diag_v1.md:659

**问题描述**：
交叉对比表中"运行时异常风险"行对 Issue 2 的描述为"有（NULL 写入不受阻）"。NULL 写入不受阻描述的是**数据完整性风险**（脏数据可能进入数据库），而非运行时异常风险（即不会导致程序在执行过程中抛出异常）。维度标签与描述语义不匹配。

Issue 2 实际的运行时异常风险是：修复后，若存在未设置 password 的代码路径 persist User，会抛出 DataIntegrityViolationException——但报告已排查确认当前不存在此类路径。

**改进建议**：
将该行标签调整为"数据完整性风险"或"数据质量风险"，并将 Issue 2 的描述改为"有（NULL 可写入数据库，产生脏数据）"，与"运行时异常风险"分离。

---

## 5. [轻微] Role/Post 测试组对映射点表中列出的部分验证点缺少显式覆盖

**所在位置**：a_v7_diag_v1.md:165-265（Role/Post 测试组）

**问题描述**：
「需验证的映射点」表（:18-34）列出了各实体的完整验证点。但 Role 测试组（2 个方法）和 Post 测试组（2 个方法）的显式覆盖率低于 User 测试组（5 个方法）：

| 映射点 | 实体 | Role 是否覆盖 | Post 是否覆盖 |
|--------|------|-------------|-------------|
| enabled 字段映射（M8/M12） | Role/Post | 否（测试中设置了 enabled 但未断言） | 同左 |
| @ManyToMany users（M10） | Role | 否（仅 User 侧有测试） | N/A |
| code 唯一约束映射（M11） | Post | N/A | 否（无 persist/find 测试验证 code 唯一约束对应的 @Column 注解映射） |

虽然 User 侧测试部分间接覆盖了 Role→User 的双向关系，策略说明也提及完整约束验证可在单独测试中实现，但作为"完整的方法示例作为模板"，Role/Post 组的覆盖率明显偏低，且映射点表列出了而未覆盖的点没有显式标注遗漏原因。

**改进建议**：
- 为 Role 补充一个简单的 `role_shouldMapEnabledField` 测试，验证 enabled 字段可正常持久化和读取
- 为 Post 补充对 code 字段映射的基本验证（参照 Role 的 `role_shouldMapCodeUniqueConstraint` 模式）
- 或在映射点表的备注列补充各点的测试覆盖状态

---

## 整体评价

产出经过 7 轮迭代，在需求响应、技术准确性和可操作性上已达到高质量水平。以上发现的问题均属**非阻塞性**问题——不影响执行者根据报告采取行动。主要改进方向集中在：①证据链严谨性（Issue 2 脏数据存在性表述需与事实对齐）；②测试模板的命名和覆盖率（Issue 1 的方法名和 Post/Role 组覆盖率）；③定位信息完整性（Issue 4 补 DDL 行号）。
