# 实现报告（v5）

## 概述

删除 AuthServiceTest 中两个测试方法（`getCurrentUser_shouldThrowWhenUserNotFound` 和 `getCurrentUser_shouldThrowWhenUserIdNull`）里多余的 `userConverter.toUserInfoResponse(any())` stub 行，修复 UnnecessaryStubbingException。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/service/AuthServiceTest.java` | 删除两行多余的 mock stub |

## 编译验证

未执行编译验证。

## 设计偏差说明

无偏差。
