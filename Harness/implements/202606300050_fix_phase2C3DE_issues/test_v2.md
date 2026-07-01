# 测试报告（v2）

## 测试文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `consultation/src/test/java/.../TriageControllerTest.java` | StubTriageService.selectDepartment 签名从 4 参改为 3 参 |
| 修改 | `consultation/src/test/java/.../TriageServiceImplTest.java` | 3 个 selectDepartment 调用改为 3 参；移除 shouldNotOverrideFinalDepartmentWhenOverwriteIsFalse |
| 修改 | `consultation/src/test/java/.../DeadLetterCompensationServiceTest.java` | StubTriageService.selectDepartment 签名改为 3 参，移除 lastOverwrite 字段；移除 shouldCallSelectDepartmentWithOverwriteFalse |
| 变更确认 | `consultation/src/test/java/.../RegistrationEventListenerTest.java` | 新增测试文件（对应 RegistrationEventListener 行为变更） |

## 测试用例清单

### TriageControllerTest（2 个用例）

| 测试方法 | 覆盖维度 | 对应契约 |
|---------|---------|---------|
| `shouldDelegateConsultToService` | 正常路径 | triage 接口委派 |
| `shouldDelegateSelectDepartmentToServiceWithOverwriteTrue` | 正常路径 | selectDepartment 接口委派 |

### TriageServiceImplTest（selectDepartment 相关，4 个用例）

| 测试方法 | 覆盖维度 | 对应契约 |
|---------|---------|---------|
| `shouldSelectDepartmentWithOverwriteTrue` | 正常路径 | selectDepartment 返回 TriageResponse |
| `shouldSelectDepartmentWhenFinalIsNull`（原 `shouldSelectDepartmentWithOverwriteFalseWhenFinalIsNull`，已按审查意见重命名） | 正常路径 | finalDepartmentId 为 null 时 selectDepartment 成功 |
| `shouldThrowBusinessExceptionWhenRecordNotFound` | 错误路径 | sessionId 不存在时抛出 TRIAGE_SESSION_NOT_FOUND |
| `shouldOverwriteExistingFinalDepartment` | 正常路径 / 幂等性 | 当 finalDepartmentId 已存在时无条件覆盖 |

其余 triage 方法（shouldPerformTriageWithAiSuccess、shouldFallbackToRuleEngineWhenAiFails 等共 16 个）不在本次变更范围内，保持原状。

### DeadLetterCompensationServiceTest（4 个用例）

| 测试方法 | 覆盖维度 | 对应契约 |
|---------|---------|---------|
| `shouldCompensateDeadLetterEvents` | 正常路径 | 死信补偿成功，状态标记为 COMPENSATED |
| `shouldIncrementRetryCountOnFailure` | 错误路径 | 补偿失败时递增 retryCount，状态标记为 FAILED |
| `shouldHandleMultipleEvents` | 正常路径 / 批量 | 多事件补偿全部成功 |
| `shouldSkipWhenNoEvents` | 边界条件 | 无待补偿事件时跳过 |

### RegistrationEventListenerTest（5 个用例）

| 测试方法 | 覆盖维度 | 对应契约 |
|---------|---------|---------|
| `shouldDelegateToTriageServiceWhenRecordExistsAndFinalIsNull` | 正常路径 | finalDepartmentId == null 时委托 triageService.selectDepartment |
| `shouldNotCallTriageServiceWhenFinalDepartmentAlreadySet` | 状态交互 | finalDepartmentId 已设置时跳过（防覆盖） |
| `shouldDoNothingWhenRecordNotFound` | 边界条件 | sessionId 无对应 record 时静默忽略 |
| `shouldWriteDeadLetterEventOnRecover` | 错误路径 | @Recover 方法写入死信事件 |
| `shouldSetFailReasonInDeadLetterEvent` | 错误路径 | 死信事件记录异常原因 |

## 覆盖维度汇总

| 维度 | 覆盖情况 |
|------|---------|
| 正常路径 | TriageControllerTest:2 / TriageServiceImplTest:3 / DeadLetterCompensationServiceTest:2 / RegistrationEventListenerTest:1 |
| 边界条件 | DeadLetterCompensationServiceTest:1（无事件）/ RegistrationEventListenerTest:1（record 不存在） |
| 错误路径 | TriageServiceImplTest:1（BusinessException）/ DeadLetterCompensationServiceTest:1（重试）/ RegistrationEventListenerTest:2（@Recover） |
| 状态交互 | RegistrationEventListenerTest:1（finalDepartmentId 已设置） |
| 幂等性 | TriageServiceImplTest:1（shouldOverwriteExistingFinalDepartment） |

## 行为契约映射

| 详细设计契约 | 对应测试文件 | 验证方式 |
|-------------|------------|---------|
| selectDepartment 始终覆盖写入 | TriageServiceImplTest#shouldOverwriteExistingFinalDepartment | 断言 finalDepartmentId/Name 被新值替换 |
| sessionId 不存在则抛 TRIAGE_SESSION_NOT_FOUND | TriageServiceImplTest#shouldThrowBusinessExceptionWhenRecordNotFound | assertThrows + 验证 errorCode |
| 事件处理仅在 finalDepartmentId == null 时调用 selectDepartment | RegistrationEventListenerTest#shouldDelegateToTriageServiceWhenRecordExistsAndFinalIsNull / shouldNotCallTriageServiceWhenFinalDepartmentAlreadySet | 断言 triageService.selectDepartmentCalled |
| @Recover 写入死信事件 | RegistrationEventListenerTest#shouldWriteDeadLetterEventOnRecover / shouldSetFailReasonInDeadLetterEvent | 断言 savedEvent 状态和内容 |
| 死信补偿递增 retryCount | DeadLetterCompensationServiceTest#shouldIncrementRetryCountOnFailure | 断言 retryCount +1 |
| 无事件时跳过 | DeadLetterCompensationServiceTest#shouldSkipWhenNoEvents | 断言 selectDepartmentCalled == false |

## 修订说明（v2 r1）

- **[已采纳]** `TriageServiceImplTest.shouldSelectDepartmentWithOverwriteFalseWhenFinalIsNull` 重命名为 `shouldSelectDepartmentWhenFinalIsNull`——`overwrite` 参数已在 API 中移除，原方法名保留旧语义造成混淆。

## 修订说明（v2 r2）

- **[已采纳]** 已实际修改源代码文件 `TriageServiceImplTest.java:297`，方法名从 `shouldSelectDepartmentWithOverwriteFalseWhenFinalIsNull` 改为 `shouldSelectDepartmentWhenFinalIsNull`（v2 r1 修订说明中已记录"已采纳"但源代码未同步更新，本轮完成代码同步）。
