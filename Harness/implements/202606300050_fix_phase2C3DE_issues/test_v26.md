# 测试报告（v26 r2）

## 修订说明

根据 test_review_v26_r1 审查反馈，修复 `RegistrationEventListenerTest.java` 缺少的 import 语句。

## 修改内容

| 文件 | 修改 |
|------|------|
| `RegistrationEventListenerTest.java` | 添加 `import com.fasterxml.jackson.core.JsonProcessingException;` |

## 测试文件清单

### DeadLetterCompensationServiceTest.java

路径：`AIMedical/backend/modules/consultation/src/test/java/com/aimedical/modules/consultation/DeadLetterCompensationServiceTest.java`

测试方法：

| 方法 | 覆盖场景 |
|------|---------|
| `shouldCompensateDeadLetterEvents` | 正常补偿成功路径 |
| `shouldIncrementRetryCountOnFailure` | 补偿失败后 retryCount 递增，state 保持 FAILED |
| `shouldHandleMultipleEvents` | 多事件处理 |
| `shouldExpireWhenRetryCountExceedsMaxOnPreCheck` | 补偿前检查：retryCount >= maxRetryCount → EXPIRED |
| `shouldExpireWhenRetryCountExceedsMaxOnCatch` | catch 块检查：递增后 retryCount >= maxRetryCount → EXPIRED |
| `shouldSkipWhenNoEvents` | 无事件时跳过 |

### RegistrationEventListenerTest.java

路径：`AIMedical/backend/modules/consultation/src/test/java/com/aimedical/modules/consultation/RegistrationEventListenerTest.java`

测试方法：

| 方法 | 覆盖场景 |
|------|---------|
| `shouldDelegateToTriageServiceWhenRecordExistsAndFinalIsNull` | 记录存在且 finalDepartmentId 为 null 时调用 TriageService |
| `shouldNotCallTriageServiceWhenFinalDepartmentAlreadySet` | finalDepartmentId 已设置时跳过 |
| `shouldDoNothingWhenRecordNotFound` | 无记录时跳过 |
| `shouldWriteDeadLetterEventOnRecover` | recover 正确写入 DeadLetterEvent |
| `shouldContainAllSevenFieldsInEventPayloadOnRecover` | eventPayload 包含全部 7 字段（E05 行为契约验证） |
| `shouldUseFallbackPayloadWhenSerializationFails` | JsonProcessingException 降级到 sessionId fallback |
| `shouldSetFailReasonInDeadLetterEvent` | failReason 正确写入异常信息 |
