# 实现报告（v1）

## 概述

删除 `SecurityConfigPhase1.java` 中与 `AuthModuleConfig.java` 重复的 `tokenBlacklist()` Bean 定义，同步调整 `SecurityConfigPhase1Test.java` 以适配编译。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/SecurityConfigPhase1.java` | 删除 `InMemoryTokenBlacklist` import 和 `tokenBlacklist()` Bean 方法 |
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/SecurityConfigPhase1Test.java` | 删除 `shouldReturnInMemoryTokenBlacklist()` 测试方法；`shouldCreateAllBeans()` 中移除 `config.tokenBlacklist()` 调用，用 `mock(TokenBlacklist.class)` 替代；`shouldCreateJwtAuthenticationFilterWithDeps()` 中用 `new InMemoryTokenBlacklist()` 替代 `config.tokenBlacklist()` |

## 编译验证

`mvn compile test-compile -pl modules/common-module/common-module-impl -am -q` 通过，无编译错误。

## 设计偏差说明

无偏差。
