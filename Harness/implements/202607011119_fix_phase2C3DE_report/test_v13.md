# 测试报告（v13）

## 概述
对 F1-F7 变更进行基于行为契约的测试覆盖分析，识别并补充遗漏的测试用例。

## 测试文件清单

| 文件 | 覆盖项 | 状态 |
|------|--------|:----:|
| `MissingFieldDetectorImplTest.java` | F1-F5 | 已有测试完整覆盖 |
| `MedicalRecordServiceImplTest.java` | F6, F7b | 已有测试完整覆盖 |
| `MedicalRecordConverterTest.java` | F7a | **新增 3 个测试填补覆盖** |

## 覆盖分析

### F1-F5 — MissingFieldDetectorImplTest 模板字段集修正
| 行为契约 | 已有测试 | 覆盖 |
|---------|---------|:----:|
| `setUp()` 过滤 MISSING_FIELDS/PARTIAL_CONTENT，template.requiredFields 大小 7 | 间接验证于所有测试 | ✓ |
| `shouldDetectMultipleMissingFields` 5 null → 3 hints | `shouldDetectMultipleMissingFields` | ✓ |
| `shouldReturnHintsForAllFieldsWhenAllNull` 返回 7 hints | `shouldReturnHintsForAllFieldsWhenAllNull` | ✓ |
| `shouldResolveAllPlaceholdersForAllFields` 不含 MISSING_FIELDS/PARTIAL_CONTENT 条目 | `shouldResolveAllPlaceholdersForAllFields` | ✓ |

### F6 — SameThreadExecutor + 中断标记传递
| 行为契约 | 已有测试 | 覆盖 |
|---------|---------|:----:|
| SameThreadExecutor 使 `resolveVisitId` 同步完成 | `shouldReturnInterruptedOnInterruptedException` | ✓ |
| 中断标记得以保留到 `callAiWithTimeout` | `shouldReturnInterruptedOnInterruptedException` | ✓ |
| `future.get()` 返回已完成 future 不检查 `Thread.interrupted()` | `shouldReturnInterruptedOnInterruptedException` | ✓ |
| `MR_GEN_AI_INTERRUPTED` 降级结果 | `shouldReturnInterruptedOnInterruptedException` 含 errorCode 断言 | ✓ |
| 预期未被中断的测试不受影响 | `shouldReturnSuccessOnNormalFlow` 等 | ✓ |

### F7a — MedicalRecordConverter 动态错误码解析
| 行为契约 | 已有测试 | 覆盖 |
|---------|---------|:----:|
| `MR_GEN_AI_TIMEOUT` → success=true, errorCode=MR_GEN_AI_TIMEOUT | `toRecordGenerateResponseShouldSetTimeoutErrorCode` | ✓ |
| `MR_GEN_AI_TIMEOUT` + null data → success=true | `toRecordGenerateResponseShouldReturnSuccessTrueWhenTimeoutEvenWithNullData` | ✓ |
| `MR_GEN_AI_INTERRUPTED` → success=false, errorCode=MR_GEN_AI_INTERRUPTED | **新增** `toRecordGenerateResponseShouldSetInterruptedErrorCode` | ✓ |
| `MR_GEN_AI_EXECUTION_ERROR` → success=false, errorCode=MR_GEN_AI_EXECUTION_ERROR | **新增** `toRecordGenerateResponseShouldSetExecutionErrorCode` | ✓ |
| 未知错误码 → errorCode=null（静默忽略） | **新增** `toRecordGenerateResponseShouldIgnoreUnknownErrorCode` | ✓ |

### F7b — MedicalRecordServiceImplTest.shouldReturnDegradedWhenAiTimesOut 断言修正
| 行为契约 | 已有测试 | 覆盖 |
|---------|---------|:----:|
| `assertFalse(response.isSuccess())` | `shouldReturnDegradedWhenAiTimesOut` | ✓ |
| `assertTrue(response.isDegraded())` | `shouldReturnDegradedWhenAiTimesOut` | ✓ |
| `assertEquals(MR_GEN_AI_EXECUTION_ERROR, response.getErrorCode())` | `shouldReturnDegradedWhenAiTimesOut` | ✓ |

## 新增测试

### `MedicalRecordConverterTest.java`

**1. `toRecordGenerateResponseShouldSetInterruptedErrorCode`**
- 场景：`aiResult` 的 errorCode = "MR_GEN_AI_INTERRUPTED"，含 data，degraded=true
- 断言：success=false, degraded=true, errorCode=MR_GEN_AI_INTERRUPTED

**2. `toRecordGenerateResponseShouldSetExecutionErrorCode`**
- 场景：`aiResult` 的 errorCode = "MR_GEN_AI_EXECUTION_ERROR"，含 data，degraded=true
- 断言：success=false, degraded=true, errorCode=MR_GEN_AI_EXECUTION_ERROR

**3. `toRecordGenerateResponseShouldIgnoreUnknownErrorCode`**
- 场景：`aiResult` 的 errorCode = "SOME_UNKNOWN_ERROR"，含 data，degraded=true
- 断言：success=false, degraded=true, errorCode=null（`IllegalArgumentException` 被捕获并忽略）

## 偏差说明
- 无偏差。所有新增测试遵循详细设计 v13 r2 的行为契约，未修改任何编码 agent 的生产源码。
- 已编译验证：`mvn compile test-compile -q` 通过。
