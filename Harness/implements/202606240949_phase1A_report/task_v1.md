# 任务指令（v1 r1）

## 动作
NEW

## 任务描述
在 `User.java:28` 的 `private String password;` 字段上添加 `@Column(nullable = false)` 注解，使 password 字段在 JPA 层面强制 NOT NULL 约束；同时同步修改 `schema.sql:16` 的 DDL 定义，将 `DEFAULT NULL` 改为 `NOT NULL`，确保实体注解与 DDL 一致。

涉及文件：
- `AIMedical\backend\modules\common-module\common-module-impl\src\main\java\com\aimedical\modules\commonmodule\permission\User.java`
- `AIMedical\backend\application\src\main\resources\db\schema.sql`

## 选择理由
Issue 2 被诊断为 P0 最高优先级——password 是用户认证凭据，缺少 NOT NULL 约束可能产生无法登录的脏数据。该修复：
1. 不依赖其他任何 Issue，可独立完成
2. 经代码路径排查，当前无生产代码创建 User 对象，无运行时风险
3. 与 Issue 1 新增测试中的 `user_shouldRejectNullPassword()` 存在依赖关系（该测试需要此约束生效后才能通过）

## 任务上下文
- 当前 `User.java:28`：`private String password;`（无约束注解）
- 对比 `User.java:25`：`@Column(nullable = false, unique = true) private String username;`（已正确标注）
- DDL 中对应列（schema.sql:16）：`` `password` VARCHAR(128) DEFAULT NULL ``，需改为 `` `password` VARCHAR(128) NOT NULL ``
- 当前 `password` 字段缺少 `import jakarta.persistence.Column;` 引用？—— 已有，User.java:5 已导入
- **同步修改要求**：Java 注解与 schema.sql DDL 两处改动需同步进行（诊断报告 Issue 2 修复指引明确要求），确保开发环境从头执行 schema.sql 建库时 password 列也为 NOT NULL

## 修订说明（v1 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [一般] Issue 1 测试方法数量与诊断报告实际内容不一致 — 计划路线表标注"7个测试方法"，实际为 11 个 | 路线表 Issue 1 复杂度标注从"中(7个测试方法)"修正为"中(11个测试方法)"，与诊断报告一致 |
| [一般] R1 任务范围偏离需求 — task_v1.md 声明"仅改 Java 注解，DDL 在生产环境中单独变更"，但诊断报告要求两处同步修改 | 采用审查方案 A：将 schema.sql:16 DDL 修改纳入 R1 范围，补充 `DEFAULT NULL` → `NOT NULL` 的修改指令；任务上下文增加同步修改说明，涉及文件列表补充 schema.sql |
