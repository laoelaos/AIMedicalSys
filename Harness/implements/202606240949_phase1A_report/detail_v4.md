# 详细设计（v4）

## 概述

修复 `UserRepositoryTest.java` 中 2 个集成测试失败的缺陷。仅修改一个文件，无新增文件。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/permission/UserRepositoryTest.java` | 修改 | 修复 2 个测试方法中的异常类型断言和 INFORMATION_SCHEMA 查询大小写 |

## 变更详解

### 失败 1：`shouldRejectNullPassword`（第 47-52 行）

**根因**：Hibernate 6 对 `@Column(nullable = false)` 属性为 null 时在 flush 阶段直接抛出 `org.hibernate.PropertyValueException`（"not-null property references a null or transient value"），而非插入数据库后由数据库抛出约束违例。Spring 的 `DataIntegrityViolationException` 是对数据库层约束违例的包装，不适用于 Hibernate 层的 null 属性校验。

**修改**：
- 第 51 行：`DataIntegrityViolationException.class` → `PropertyValueException.class`
- 新增 import：`import org.hibernate.PropertyValueException;`

### 失败 2：`shouldHaveNotNullConstraintOnPasswordColumn`（第 65-71 行）

**根因**：H2 `INFORMATION_SCHEMA.COLUMNS` 以大写形式存储标识符（`SYS_USER`、`PASSWORD`），而查询中使用小写字符串字面量 `'sys_user'` 和 `'password'`，导致 WHERE 条件不匹配 → `EmptyResultDataAccessException`。

**修改**：
- 第 67-68 行：将字符串字面量改为使用 `UPPER()` 函数与大写常量比较

```java
// 修改前
String sql = "SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS " +
             "WHERE TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'password'";

// 修改后
String sql = "SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS " +
             "WHERE UPPER(TABLE_NAME) = 'SYS_USER' AND UPPER(COLUMN_NAME) = 'PASSWORD'";
```

## Import 变更

| 操作 | import 语句 | 说明 |
|------|------------|------|
| 新增 | `import org.hibernate.PropertyValueException;` | 第 51 行断言需要，排在 `org.springframework.dao.DataIntegrityViolationException` 之后 |
| 保留 | `import org.springframework.dao.DataIntegrityViolationException;` | 第 11 行，其他测试方法（如 `user_shouldEnforceUserTypeNotNull`）仍使用，不可删除 |

## 行为契约

- **前置**：`@Column(nullable = false)` 已在 `User.password` 字段上添加（Issue 2 修复）
- **前置**：`user.setUserType(UserType.ADMIN)` 已正确设置（v1/v2 修复）
- **后置**：`shouldRejectNullPassword` 在 password 为 null 时，`persistAndFlush` 抛出 `PropertyValueException`
- **后置**：`shouldHaveNotNullConstraintOnPasswordColumn` 正确查询 INFORMATION_SCHEMA 并返回 `"NO"`
- **验证命令**：`mvn test -pl common-module-impl,integration -am`
- **预期结果**：common-module-impl 全部 39 个测试方法通过，integration 模块被成功构建和执行

## 依赖关系

| 依赖类型 | 说明 |
|---------|------|
| `org.hibernate.PropertyValueException` | Hibernate ORM 6.4.4.Final 内置异常类，无需额外 Maven 依赖 |
| `User.userType` 已有值 | 依赖 v1/v2 修复已添加的 `user.setUserType(UserType.ADMIN)` |
| `User.password @Column(nullable = false)` | 依赖 Issue 2 已添加的 NOT NULL 注解 |
