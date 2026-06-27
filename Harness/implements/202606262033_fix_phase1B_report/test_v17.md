# 测试报告（v17 r1）

## 改动文件

| 操作 | 文件 |
|------|------|
| 修改 | `LoggingSecurityAuditLoggerTest.java` |

## 修改说明

### LoggingSecurityAuditLoggerTest.java

**`logAudit_shouldWriteExpectedFormat`** (line 62-63)：将 `newJti` 参数从 `"jti-xxx"` 改为 `null`。

**理由**：审查发现构造事件时传入了非 null 的 `newJti="jti-xxx"`，但断言 `assertFalse(msg.contains("newJti="))` 期望该字段不出现。根据设计契约，可选字段在非 null 时应输出，且已有独立测试 `logAudit_shouldIncludeOptionalFieldsWhenPresent` 覆盖非 null 可选字段的输出验证。本方法作为"基础格式"测试，应将 `newJti` 置为 `null`，与 `failureReason`、`refreshTokenMasked` 同为 null 的测试意图一致。

## 测试文件列表

- `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/audit/LoggingSecurityAuditLoggerTest.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/service/AuthServiceTest.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/controller/AuthControllerTest.java`
