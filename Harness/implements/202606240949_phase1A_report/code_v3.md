# 实现报告（v3）

## 概述

修复 `EntityMappingIT.java` 中 2 个集成测试失败：`user_shouldPersistWithPassword` 和 `user_shouldRejectNullPassword`。仅修改一个文件，无新增文件。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `AIMedical/backend/integration/src/test/java/com/aimedical/integration/EntityMappingIT.java` | 修复 2 个测试方法中的 userType 缺失和异常类型错误 |

## 编译验证

未执行编译验证。

## 设计偏差说明

无偏差。
