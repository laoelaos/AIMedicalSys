# 测试报告（v9）

## 概述
根据详细设计 v9 的行为契约，为修复 A07/A09/A11/M01/M08 编写单元测试。涉及 4 个测试文件。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `modules/ai/ai-api/src/test/java/com/aimedical/modules/ai/api/AiResultTest.java` | A07: 删除 `shouldCreateSuccessResultWithNullData`，新增 `shouldThrowNpeWhenSuccessWithNullData` |
| 修改 | `modules/ai/ai-api/src/test/java/com/aimedical/modules/ai/api/AiResultFactoryTest.java` | M08: 新增 2 个测试用例覆盖 3 参数 `degraded(fallbackReason, errorCode, partialData)` |
| 修改 | `modules/prescription/src/test/java/com/aimedical/modules/prescription/service/audit/impl/PrescriptionAuditServiceImplTest.java` | A09: 新增 2 个测试用例验证降级路径 WARN 日志输出 |
| 修改 | `modules/medical-record/src/test/java/com/aimedical/modules/medicalrecord/enums/MedicalRecordErrorCodeTest.java` | M01: 常量计数 4→8，`shouldReturnCorrectCodeAndMessage` 新增 4 组断言 |

## 测试用例明细

### AiResultTest

| 测试方法 | 覆盖契约 | 维度 |
|---------|---------|------|
| `shouldThrowNpeWhenSuccessWithNullData` | A07: success(null) 抛出 NullPointerException | 错误路径 |

### AiResultFactoryTest

| 测试方法 | 覆盖契约 | 维度 |
|---------|---------|------|
| `shouldCreateDegradedResultWithErrorCodeAndPartialData` | M08: 3 参 degraded 含 partialData | 正常路径 |
| `shouldCreateDegradedResultWithErrorCodeAndNullPartialData` | M08: 3 参 degraded 含 null partialData | 边界条件 |

### PrescriptionAuditServiceImplTest

| 测试方法 | 覆盖契约 | 维度 |
|---------|---------|------|
| `auditShouldLogWarnWhenAiResultIsNull` | A09: 异常导致 aiResult=null 时输出 WARN 日志 | 错误路径 |
| `auditShouldLogWarnWhenAiReturnsFailure` | A09: aiResult 为 failure 时输出 WARN 日志（含 errorCode） | 错误路径 |

### MedicalRecordErrorCodeTest

| 测试方法 | 覆盖契约 | 维度 |
|---------|---------|------|
| `shouldHaveEightConstants` | M01: 断言枚举常量数 = 8 | 正常路径 |
| `shouldReturnCorrectCodeAndMessage` | M01: 8 组枚举的 getCode/getMessage 断言（含 4 组新增） | 正常路径 |

## 设计偏差说明

无偏差。
