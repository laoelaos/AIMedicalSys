# 质量审查报告：a_v5_diag_v1.md

## 总体评价

v5 版本已系统性地修正了前四轮审查指出的主要问题（@SQLRestriction 行为纠正、生产脏数据清理方案、副作用分析、代码路径排查等），整体质量较高。但仍存在以下问题：

---

## 问题 1：[严重] Issue 1 的测试代码与 Issue 2 的修复方案存在逻辑矛盾

**问题描述**：Issue 1 的测试示例中包含 `user_shouldAllowNullPassword()` 测试方法（a_v5_diag_v1.md:70-83），其注释明确说明"验证 password 列允许 NULL（当前设计如此，非缺陷）"。但 Issue 2（P0）的修复方案要求添加 `@Column(nullable = false)` + DDL NOT NULL。按推荐的修复执行顺序（Issue 2 → Issue 3 → Issue 4 → Issue 1），Issue 1 的测试编写应在 Issue 2 修复之后，届时该测试将因 password 列不接受 NULL 而 FAIL。

更严重的是，Issue 1 的映射点表 M2 行（a_v5_diag_v1.md:21）描述为"password 无 NOT NULL 约束（**应为** NULL 可接受）"，这与 Issue 2"password 字段缺少 NOT NULL 约束"的缺陷定性直接对立。同一份文档内部，M2 将当前行为定性为"应然"、Issue 2 将其定性为"缺陷"，两者互斥。

**所在位置**：
- a_v5_diag_v1.md:21（M2 映射点描述）
- a_v5_diag_v1.md:70-83（`user_shouldAllowNullPassword` 测试方法）
- a_v5_diag_v1.md:319（Issue 2 修复指引：添加 NOT NULL）

**严重程度**：严重

**改进建议**：
（A）删除 `user_shouldAllowNullPassword()`，替换为一个验证 NOT NULL 约束的测试（persist 不带 password 的 User，预期抛出 `DataIntegrityViolationException`），并在测试注释中说明"该测试验证 Issue 2 修复后的 NOT NULL 约束"。
（B）同步修正 M2 行的描述，将"应为 NULL 可接受"改为"缺少 NOT NULL 约束"，使之与 Issue 2 的缺陷定性一致。
（C）在 Issue 1 的交叉影响备注中补充 Issue 2 对此处测试的影响说明。

---

## 问题 2：[中等] 生产脏数据存在性与"无生产代码路径"的陈述缺乏关联说明

**问题描述**：Issue 2 的副作用分析（a_v5_diag_v1.md:325-329）明确结论为"当前不存在生产代码创建 User 对象的路径"，但同时在同一章节（a_v5_diag_v1.md:333）指出"生产数据库中已存在 password IS NULL 的记录"。两个陈述并置而无任何过渡说明，造成事实断层：如果当前代码库中没有任何生产模块创建 User，这些已存在的生产脏数据是如何产生的？

**所在位置**：a_v5_diag_v1.md:325-333

**严重程度**：中等

**改进建议**：补充过渡说明，澄清可能的原因：（a）生产脏数据可能来自前期数据导入/种子脚本（早于模块拆分），（b）当前代码库已重构但生产环境运行的是旧版本代码，（c）脏数据由其他系统直接写入数据库。此说明有助于修复者正确评估风险范围和修复紧迫性。

---

## 问题 3：[一般] Issue 1 测试示例在 `entityManager.clear()` 的使用上不一致

**问题描述**：Issue 1 的 7 个测试方法中，`user_shouldMapManyToManyWithRoles`、`user_shouldMapManyToManyWithPosts`、`user_shouldMapUserTypeEnumAsString` 这 3 个调用了 `entityManager.clear()` 后再 `find()`，而 `user_shouldMapUsernameUniqueConstraint`、`user_shouldAllowNullPassword`、`role_shouldMapCodeUniqueConstraint`、`post_shouldMapManyToOneRole` 这 4 个没有调用。而现有 `EntityMappingIT.java` 中所有已有测试均 **不** 使用 `clear()`。

**所在位置**：a_v5_diag_v1.md:105、132、153（使用 clear）；a_v5_diag_v1.md:56-67、70-83、172-185、218-240（未使用 clear）

**严重程度**：一般

**改进建议**：建议统一约定。方案一：所有测试均使用 `clear()` 以验证完整的 DB 读写路径（更严格的集成测试）；方案二：说明为何部分测试需要 clear（如 @ManyToMany 关联需要清除缓存以验证 LAZY 加载），并保持 `EntityMappingIT` 中现有风格。无论选哪种，应在测试代码中或策略说明中明确约定。

---

## 问题 4：[一般] Issue 2 代码路径排查未确认是否存在通过 UserRepository 间接写入的路径

**问题描述**：Issue 2 的副作用分析通过搜索 `new User()` 来确认无生产代码路径，但并未搜索是否存在通过 `UserRepository`（或其他 JPA repository）的 `save()` 操作，也未搜索是否通过 SQL 脚本（如 data.sql、flyway migration）直接插入用户数据。`UserRepository` 即使在当前模块未创建 User 对象，也可能在其他模块中被注入并调用 `save()`。

**所在位置**：a_v5_diag_v1.md:325-329

**严重程度**：一般

**改进建议**：补充对 `UserRepository` 引用的全局搜索，确认没有任何模块通过 repository 间接写入 User 数据。若确认无此类路径，应在报告中明确说明"已搜索 UserRepository 引用，确认无间接写入路径"作为补充证据。
