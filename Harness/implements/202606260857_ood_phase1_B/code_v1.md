# 实现报告（v1）

## 概述

依据详细设计 v1，完成 OOD 包 A 数据建模缺陷修复与字段扩展：

- `User` 实体新增 `passwordChangeRequired`、`tokenVersion` 字段与对应 getter/setter
- `Role` 实体为 `enabled` 补 `@Column(nullable=false)`；新增 `sort` 字段与对应 getter/setter
- `Post` 实体为 `enabled` 补 `@Column(nullable=false)`
- `UserRepository.findByUsername` 返回类型由 `User` 改为 `Optional<User>`
- `AuthServiceImpl.login()` 改用 `Optional` 链式调用
- `AuthServiceTest` 4 处 Mockito stub 同步适配 `Optional`

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/User.java` | 新增 `passwordChangeRequired`（`@Column(nullable=false, columnDefinition="BIT(1) DEFAULT 0")`，默认 false）、`tokenVersion`（`@Column(nullable=false)`，默认 0）字段及对应 getter/setter |
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/Role.java` | 为 `enabled` 补 `@Column(nullable=false)`；新增 `sort` 字段（`@Column(nullable=false)`，默认 0）及对应 getter/setter |
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/Post.java` | 为 `enabled` 补 `@Column(nullable=false)` |
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/UserRepository.java` | `findByUsername` 返回类型由 `User` 改为 `Optional<User>`，新增 `import java.util.Optional` |
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/impl/AuthServiceImpl.java` | `login()` 中 `findByUsername` 调用方改写为 `.orElseThrow(() -> { log.warn(...); return new BusinessException(UNAUTHORIZED, "用户名或密码错误"); })` |
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/service/AuthServiceTest.java` | 4 处 `when(userRepository.findByUsername(...))` 改写：3 处 `testUser` → `Optional.of(testUser)`，1 处 `null` → `Optional.empty()` |

## 编译验证

未执行编译验证。Maven 包装器在 Windows 环境下可执行 `mvnw -pl common-module/common-module-impl -am compile` 与 `mvnw -pl common-module/common-module-impl test` 进行完整验证；当前任务范围内未在 shell 中运行。

## 设计偏差说明

无偏差。所有修改严格匹配详细设计 v1：

- 字段注解（`@Column(nullable=false)`、`columnDefinition="BIT(1) DEFAULT 0"`）、默认值、类型与设计一致
- 字段插入位置（`User` 中位于 `enabled` 与 `userType` 之间；`Role` 中位于 `enabled` 之后）与既有字段风格一致
- `UserRepository` 改用 `Optional<User>` 后保持 `@Repository` 注解与 Javadoc 风格
- `AuthServiceImpl.login` 仅替换行 60-65，其余行（`enabled` 检查 → `passwordEncoder.matches` → 业务响应构建）顺序与既有代码一致
- `AuthServiceTest` 复用文件已 import 的 `java.util.Optional`，未新增 import
- 未触达设计范围外的 `schema.sql`、`PermissionFunction` 重命名、`Post.sort` 等
