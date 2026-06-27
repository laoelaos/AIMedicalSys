# 实现报告（v14）

## 概述

修复 T23：`MenuServiceImpl.getUserMenuTree()` N+1 查询问题。在 `UserRepository` 中新增 `findWithDetailsForMenuById` 方法（`@EntityGraph` 含 `posts.functions`），`MenuServiceImpl.getUserMenuTree()` 改用该方法，并同步更新测试 mock。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/UserRepository.java` | 新增 `findWithDetailsForMenuById` 方法，插入在 `findWithDetailsById` 之后、`findTokenVersionById` 之前 |
| 修改 | `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/impl/MenuServiceImpl.java` | L44 `findById(userId)` → `findWithDetailsForMenuById(userId)` |
| 修改 | `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/service/MenuServiceTest.java` | 3 处 mock 从 `findById` 改为 `findWithDetailsForMenuById` |

## 编译验证

未执行编译验证。

## 设计偏差说明

无偏差。实际文件路径包含 `AIMedical/backend/` 前缀，已在变更中正确映射。
