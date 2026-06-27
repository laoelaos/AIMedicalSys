# 测试审查报告（v17 r1）

## 审查结果
REJECTED

## 发现

- **[严重]** `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/audit/LoggingSecurityAuditLoggerTest.java:77` — `logAudit_shouldWriteExpectedFormat` 中构造的事件传入 `newJti="jti-xxx"`（非 null），但断言 `assertFalse(msg.contains("newJti="))`。根据设计契约，可选字段在非 null 时应输出，且 `LoggingSecurityAuditLogger.logAudit()` 实现（第33-35行）确实会在 newJti 非 null 时追加该字段。此测试断言与设计和实现均矛盾，测试会失败或给出错误的安全感。

## 修改要求（仅 REJECTED 时）

- `LoggingSecurityAuditLoggerTest.java:61-78` `logAudit_shouldWriteExpectedFormat` 方法：第 63 行将 `"jti-xxx"` 改为 `null`（表示 newJti 可选字段不出现），或保留非 null 值并将第 77 行改为 `assertTrue(msg.contains(" newJti=jti-xxx"))` 以验证可选字段正常输出。二选一，取决于本方法的测试意图。
