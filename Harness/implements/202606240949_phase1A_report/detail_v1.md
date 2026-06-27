# 详细设计（v1）

## 概述

为 `User.java` 的 `password` 字段添加 `@Column(nullable = false)` 注解，并在 `schema.sql` 中将对应 DDL 从 `DEFAULT NULL` 改为 `NOT NULL`，使 JPA 实体注解与手工 DDL 一致，确保 password 列在应用和数据库层面均强制 NOT NULL 约束。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/User.java` | 修改 | 第28行 `private String password;` 添加 `@Column(nullable = false)` 注解 |
| `AIMedical/backend/application/src/main/resources/db/schema.sql` | 修改 | 第16行 `` `password` VARCHAR(128) DEFAULT NULL `` 改为 `` `password` VARCHAR(128) NOT NULL `` |

## 类型定义

无新增类型。修改涉及 `com.aimedical.modules.commonmodule.permission.User` 实体类。

### User.password 字段
**形态**：field（实体字段）
**所在类**：`com.aimedical.modules.commonmodule.permission.User`
**当前定义**：
```java
// line 28
private String password;
```
**目标定义**：
```java
// line 28
@Column(nullable = false)
private String password;
```
**对齐模式**：与同类的 `username` 字段（第25行 `@Column(nullable = false, unique = true) private String username;`）保持一致，但 password 不声明 `unique = true`。

**前置条件**：`jakarta.persistence.Column` 已在 `User.java:5` 导入，无需新增 import。

## 错误处理

- 无需自定义错误类型。
- 约束违反由 JPA/Hibernate 在持久化时抛出 `jakarta.persistence.PersistenceException`（通常为 `org.springframework.dao.DataIntegrityViolationException`），沿用现有异常处理机制。
- 当前无生产代码创建 User 对象并持久化（见诊断报告代码路径排查），修改无运行时风险。

## 行为契约

| 条件 | 修改前 | 修改后 |
|------|--------|--------|
| persist User 且 password = null | 成功（password 列写入 NULL） | 抛出 DataIntegrityViolationException |
| persist User 且 password = "xxx" | 成功 | 成功（不变） |
| schema.sql 从头建库 | password 列允许 NULL | password 列 NOT NULL |

**前置条件**：无
**后置条件**：password 字段在实体注解和 DDL 层面均不允许 NULL。
**调用顺序**：无特定顺序要求。
**状态变化**：无。

## 依赖关系

| 依赖 | 说明 |
|------|------|
| `jakarta.persistence.Column` (User.java:5) | 已导入，无需变更 |
| `schema.sql` sys_user 表 DDL | 同步修改，确保从头建库时 password 列为 NOT NULL |
| Issue 1 测试依赖 | `user_shouldRejectNullPassword()` 需此修复生效后才能通过 |
