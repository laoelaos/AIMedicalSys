# 实现报告（v6）

## 概述

修复 GlobalExceptionHandler 中 T3（ACCOUNT_LOCKED 消息模板未插值）和 T25（RATE_LIMITED/ACCOUNT_LOCKED 应返回 HTTP 429）两项 P1 缺陷。涉及 3 个源文件修改。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `common/src/main/java/com/aimedical/common/config/GlobalExceptionHandler.java` | 新增 `formatMessage` 私有方法做消息插值；修改 `handleBusinessException` 调用 `formatMessage` 并传入插值后 message；`resolveHttpStatus` 新增 RATE_LIMITED/ACCOUNT_LOCKED → 429 映射 |
| 修改 | `common-module-impl/.../service/impl/AuthServiceImpl.java:95,99` | ACCOUNT_LOCKED 的 args 从完整短语修正为仅占位符替换值：`"请30分钟后重试"` → `"30分钟"`，`"请15分钟后重试"` → `"15分钟"` |
| 修改 | `common/src/test/java/com/aimedical/common/config/GlobalExceptionHandlerTest.java` | 新增 `shouldInterpolateAccountLockedMessage`、`shouldReturn429ForRateLimited` 测试方法 |

## 编译验证

- **编译**：`mvn compile -pl common -am` 成功（无错误）
- **编译**：`mvn compile -pl modules/common-module/common-module-impl -am` 成功（无错误）
- **测试编译**：`mvn test-compile -pl common -am` 成功
- **测试执行**：`mvn test -pl common -am` → **BUILD SUCCESS**，共执行 123 个测试（0 Failures, 0 Errors, 5 Skipped）
  - `GlobalExceptionHandlerTest`：7 个测试全部通过（含新增 shouldInterpolateAccountLockedMessage、shouldReturn429ForRateLimited）

## 设计偏差说明

无偏差。

## 修订说明（v6 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| 代码未经编译验证即声称"无设计偏差"不合规，无法确认代码可编译和测试可通过 | 执行 `mvn compile`（common + common-module-impl）、`mvn test-compile`、`mvn test`（common），所有编译和测试均通过，更新实现报告补充验证结果 |
