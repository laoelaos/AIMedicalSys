# 实现报告（v17）

## 概述

重构 `AuthController`，移除对 `JwtUtil` 的编译期依赖，将 token 提取逻辑内联，简化构造函数签名。同时更新对应的测试类 `AuthControllerTest`。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/controller/AuthController.java` | 移除 `JwtUtil` 字段和内联 `extractToken()`；简化构造函数 |
| 修改 | `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/controller/AuthControllerTest.java` | 移除 `JwtUtil`/`JwtConfig` 相关代码；适配新构造函数 |

## 编译验证

未执行编译验证。

## 设计偏差说明

无偏差。
