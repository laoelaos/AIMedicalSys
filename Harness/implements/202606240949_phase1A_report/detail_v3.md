# 详细设计（v3）

## 概述

修复 v1 引入的 2 个集成测试失败。仅修改 `EntityMappingIT.java` 中两个测试方法，不涉及其他文件。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/integration/src/test/java/com/aimedical/integration/EntityMappingIT.java` | 修改 | 修复 2 个测试方法中的 userType 缺失和异常类型错误 |

## 变更详解

### 失败 1：`user_shouldPersistWithPassword`（第 249-260 行）

**根因**：`User.userType` 标注 `@Column(nullable = false)`，测试未设置 userType，H2 DDL 生成 userType NOT NULL 约束，`persist + flush` 抛出 `ConstraintViolationException`。

**修改**：在第 253 行之后插入 `user.setUserType(UserType.ADMIN);`

```java
// 修改前
User user = new User();
user.setUsername("test_user_password");
user.setPassword("pwd123");

// 修改后
User user = new User();
user.setUsername("test_user_password");
user.setPassword("pwd123");
user.setUserType(UserType.ADMIN);
```

### 失败 2：`user_shouldRejectNullPassword`（第 262-271 行）

**根因**（双重问题）：
1. 未设置 userType，实际抛出的是 userType NOT NULL 约束违例而非 password NOT NULL
2. Hibernate NOT NULL 约束违例抛出 `ConstraintViolationException`，而非 `DataIntegrityViolationException`

**修改**：
1. 在第 265 行之后插入 `user.setUserType(UserType.PATIENT);` — 使 password 成为唯一违例字段
2. 第 267 行：`DataIntegrityViolationException.class` → `ConstraintViolationException.class`

```java
// 修改前
User user = new User();
user.setUsername("test_user_null_pwd");

assertThrows(DataIntegrityViolationException.class, () -> {

// 修改后
User user = new User();
user.setUsername("test_user_null_pwd");
user.setUserType(UserType.PATIENT);

assertThrows(ConstraintViolationException.class, () -> {
```

## 已有 import 确认

| 符号 | import 行号 | 状态 |
|------|------------|------|
| `UserType` | 第 7 行 | 已存在 |
| `ConstraintViolationException` | 第 21 行 | 已存在 |
| `DataIntegrityViolationException` | 第 22 行 | 仍需要（其他测试使用） |

**无需新增或删除任何 import 语句。**

## 行为契约

- **前置**：v2 所有变更（H2 依赖、schema.sql、Java 默认值、9 个新增测试）已就位
- **后置**：`user_shouldPersistWithPassword` 正确持久化含 userType 的 User，断言 password 字段映射正确
- **后置**：`user_shouldRejectNullPassword` 设置 userType 后 password 为 null，`persist + flush` 抛出 `ConstraintViolationException`
- **验证命令**：`mvn test -pl integration -am`
- **期待结果**：EntityMappingIT 全部 19 个测试方法通过，integration 模块零失败

## 依赖关系

| 依赖类型 | 说明 |
|---------|------|
| v2 变更（H2 + schema.sql + Java 默认值） | 两个测试方法均依赖 H2 运行时和 enabled/userType 默认值正确 |
| `com.aimedical.modules.commonmodule.api.UserType` | 两个方法均需枚举值 ADMIN 和 PATIENT，已在 import 中 |
| `org.hibernate.exception.ConstraintViolationException` | `user_shouldRejectNullPassword` 需要，已在 import 中 |
