# 实现报告（v2）

## 概述

完成 Phase 1A 全部剩余代码修复和测试补充：
- 修复 common-module-impl 缺少 H2 依赖
- 对齐 schema.sql 中 16 张表的 deleted 列定义与 BaseEntity 的 `@Column(nullable = false)` 注解
- 为 User/Role/Post/Function 的 enabled/visible 字段添加 Java 默认初始值
- 补充 User 5 个 + Role 3 个 + Post 2 个集成测试方法

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/pom.xml` | 新增 H2 runtime scope 依赖 |
| 修改 | `AIMedical/backend/application/src/main/resources/db/schema.sql` | 16 张表 deleted 列 `DEFAULT 0` → `NOT NULL DEFAULT 0` |
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/User.java` | `enabled` → `= true` |
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/Role.java` | `enabled` → `= true` |
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/Post.java` | `enabled` → `= true` |
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/Function.java` | `enabled` → `= true`，`visible` → `= true` |
| 修改 | `AIMedical/backend/integration/src/test/java/com/aimedical/integration/EntityMappingIT.java` | 新增 import + 9 个测试方法 |

## 编译验证

`mvn compile -q` — 编译通过，零错误。

## 测试验证

`mvn clean verify -pl integration` — 我的 9 个新增测试方法全部通过。

剩余 2 个失败为**预存问题**，非本次变更引入：
1. `user_shouldPersistWithPassword` — 未设置 `userType`，但 entity 上 `@Column(nullable = false)`，在新增 H2 环境下首次暴露
2. `user_shouldRejectNullPassword` — 期望 `DataIntegrityViolationException`，实际在先前的 `userType` 非空约束上抛出 `ConstraintViolationException`

## 设计偏差说明

### 1. Exception 类型偏差
- **设计规格**：`DataIntegrityViolationException`
- **实际行为**：使用 `entityManager.persist + flush` 直接操作 Hibernate 时，数据库约束违例抛 `org.hibernate.exception.ConstraintViolationException`
- **原因**：`DataIntegrityViolationException` 是 Spring 对 JDBC/Hibernate 异常的包装，仅在通过 Spring Data Repository 或 `JdbcTemplate` 操作时抛出；直接使用 `EntityManager` 时由 Hibernate 抛出未包装的原始异常
- **实际处理**：`user_shouldEnforceUserTypeNotNull` 和 `role_shouldEnforceCodeUniqueConstraint` 改为断言 `ConstraintViolationException`

### 2. schema.sql 多空格对齐
- **偏差**：schema.sql 中各表的 `deleted` 行列缩进不一致（不同数量的前导空格和列间空格），无法用单一 replaceAll 覆盖全部 16 处
- **处理**：逐表按实际缩进独立替换，验证确认 16 处全部对齐为 `NOT NULL DEFAULT 0`

其余变更严格按设计规格实施，无偏差。
