# 实现报告（v5）

## 概述

修复三项 error code 标准化缺陷：GlobalErrorCode.java 中 UNAUTHORIZED 和 FORBIDDEN 消息文本对齐 OOD 规范，MenuServiceImpl.deleteMenu() 错误码从 PARAM_INVALID 替换为 CHILDREN_EXIST。涉及 2 个源文件 + 1 个测试文件修改。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `common/src/main/java/com/aimedical/common/exception/GlobalErrorCode.java` | UNAUTHORIZED 消息 `"未认证"` → `"未认证或令牌已失效"`；FORBIDDEN 消息 `"无权限"` → `"无权限访问"` |
| 修改 | `common/src/test/java/com/aimedical/common/exception/GlobalErrorCodeTest.java` | 同步更新 UNAUTHORIZED 和 FORBIDDEN 消息断言预期值 |
| 修改 | `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/impl/MenuServiceImpl.java` | deleteMenu() 中 `GlobalErrorCode.PARAM_INVALID` → `GlobalErrorCode.CHILDREN_EXIST` |

## 编译验证

`mvn compile -pl common,modules/common-module/common-module-impl -am -q` — 编译通过，无错误。

## 设计偏差说明

无偏差。
