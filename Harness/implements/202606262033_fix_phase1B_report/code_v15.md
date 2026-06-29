# 实现报告（v15）

## 概述

实现 T11 修复：将 `RestAuthenticationEntryPoint` 中基于字符串包含的账户禁用检测改为基于异常类型判断（`instanceof`），消除国际化/多语言场景下消息文本变更导致检测静默失效的设计级脆弱性。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新建 | `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/exception/AccountDisabledAuthenticationException.java` | 账户禁用专用异常，继承 `AuthenticationException` |
| 修改 | `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/JwtAuthenticationFilter.java` | `throwAccountDisabled()` 抛出 `AccountDisabledAuthenticationException`；替换 import |
| 修改 | `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/RestAuthenticationEntryPoint.java` | 删除 `ACCOUNT_DISABLED_MESSAGE` 常量；改用 `instanceof` 判断；新增 import |
| 修改 | `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/RestAuthenticationEntryPointTest.java` | `shouldReturnAccountDisabledWhenMessageMatches` 使用 `new AccountDisabledAuthenticationException("any message")` |

## 编译验证

编译通过，测试 `RestAuthenticationEntryPointTest` 通过。

## 设计偏差说明

无偏差。
