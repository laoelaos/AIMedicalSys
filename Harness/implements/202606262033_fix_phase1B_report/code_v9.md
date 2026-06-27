# 实现报告（v9）

## 概述

按 v9 详细设计修复 AuthController T6（API 路径修正）和 T7（SecurityContext 重构），同步修改 AuthControllerTest。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/controller/AuthController.java` | T6: `@PutMapping("/me")` → `@PutMapping("/profile")`；T7: 删除 `JwtTokenProvider` 依赖，新增 `getCurrentUserId()`，重构 `changePassword()`，更新 import |
| 修改 | `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/controller/AuthControllerTest.java` | 删除 `@Mock JwtTokenProvider`、修改 `setUp()` 构造、删除/新增 import、重构 `ChangePasswordTests` mock 策略 |

## 编译验证

`mvn compile -pl modules/common-module/common-module-impl -am -q` 通过，无错误。

## 设计偏差说明

无偏差。
