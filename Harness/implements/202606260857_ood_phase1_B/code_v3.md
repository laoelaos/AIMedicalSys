# 实现报告（v3）

## 概述

完成 Phase 1 包 B OOD 第 12 节 Stage 1 剩余 P0 任务，共 4 个子项：
- **子项 A**：`Function` → `PermissionFunction` 重命名（类名、文件名、Repository、Test 及 5 个引用文件级联更新）
- **子项 B**：`JwtUtil` 新增 `@PostConstruct` SecretKey 缓存 + 启动验证
- **子项 C**：`schema.sql` DDL 变更（NOT NULL 归一化 + 新列）
- **子项 D**：`data.sql` 种子数据更新（新复杂度密码 + `password_change_required = 1`）

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新建 | `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/PermissionFunction.java` | Function → PermissionFunction 重命名 |
| 删除 | `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/Function.java` | 原文件 |
| 新建 | `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/PermissionFunctionRepository.java` | FunctionRepository → PermissionFunctionRepository 重命名 |
| 删除 | `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/FunctionRepository.java` | 原文件 |
| 修改 | `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/Post.java` | `Set<Function>` → `Set<PermissionFunction>` 类型引用更新 |
| 修改 | `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/impl/MenuServiceImpl.java` | import 及内联类型引用 `Function` → `PermissionFunction` |
| 修改 | `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/impl/AuthServiceImpl.java` | import 及内联类型引用 `Function` → `PermissionFunction` |
| 修改 | `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/jwt/JwtUtil.java` | 新增 `secretKey` 字段 + `@PostConstruct init()` + 使用缓存密钥 |
| 修改 | `application/src/main/resources/db/schema.sql` | DDL 变更（4 张表） |
| 修改 | `application/src/main/resources/db/data.sql` | 种子数据密码更新 + `password_change_required` + `token_version` |
| 新建 | `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/permission/PermissionFunctionTest.java` | FunctionTest → PermissionFunctionTest 重命名 |
| 删除 | `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/permission/FunctionTest.java` | 原文件 |
| 修改 | `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/permission/PostTest.java` | `Set<Function>` / `new Function()` → `PermissionFunction` |
| 修改 | `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/service/AuthServiceTest.java` | import `Function` → `PermissionFunction` |
| 修改 | `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/service/MenuServiceTest.java` | import 及内联类型引用 `Function` → `PermissionFunction` |
| 修改 | `integration/src/test/java/com/aimedical/integration/EntityMappingIT.java` | import 及内联类型引用 `Function` → `PermissionFunction` |

## 编译验证

`mvn compile -q` 全部模块编译通过，无错误。

## 设计偏差说明

无偏差。
