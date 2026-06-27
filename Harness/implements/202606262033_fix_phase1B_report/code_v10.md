# 实现报告（v10）

## 概述

修改 `AuthControllerTest.java` 第 273-274 行的 Mockito 语法：将 `when(authService.changePassword(...)).thenThrow(...)` 替换为 `doThrow(...).when(authService).changePassword(...)`，修复 void 方法在使用 `when()` 时产生的编译错误。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/controller/AuthControllerTest.java` | 将 L273-L274 `when().thenThrow()` 替换为 `doThrow().when()` |

## 编译验证

通过。`mvn compile test-compile -pl modules/common-module/common-module-impl -am -q` 无错误输出。

## 设计偏差说明

无偏差。

不过实际文件路径与设计文档中的路径略有差异（设计文档路径偏短，少了 `AIMedical/backend/` 前缀），已在文件变更清单中使用实际路径。
