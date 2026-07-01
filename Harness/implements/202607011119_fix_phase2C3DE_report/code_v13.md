# 实现报告（v13）

## 概述
修复 v12 验证遗留的 3 类 side-effect（F1-F7），涉及 medical-record 模块内的 2 个测试文件 + 1 个生产文件。无新增功能。

## 文件变更清单
| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `medical-record/src/test/java/.../detector/MissingFieldDetectorImplTest.java` | F1-F5：模板字段集过滤 MISSING_FIELDS/PARTIAL_CONTENT，调整断言值 |
| 修改 | `medical-record/src/main/java/.../converter/MedicalRecordConverter.java` | F7a：errorCode 动态解析使用 `MedicalRecordErrorCode.valueOf()` |
| 修改 | `medical-record/src/test/java/.../service/impl/MedicalRecordServiceImplTest.java` | F6：新增 `SameThreadExecutor` 内部类、替换 executor、future 改为未完成、补充 errorCode 断言；F7b：修正 success/errorCode 断言 |

### MissingFieldDetectorImplTest.java 变更明细
- `setUp()`：`Stream.of(MedicalRecordField.values())` 增加 `filter(f -> f != MISSING_FIELDS && f != PARTIAL_CONTENT)`
- `shouldDetectMultipleMissingFields`：`assertEquals(5, hints.size())` → `assertEquals(3, hints.size())`
- `shouldReturnHintsForAllFieldsWhenAllNull`：`assertEquals(9, hints.size())` → `assertEquals(7, hints.size())`
- `shouldResolveAllPlaceholdersForAllFields`：`assertEquals(9, hints.size())` → `assertEquals(7, hints.size())`，移除 expectedPrompts 中 MISSING_FIELDS/PARTIAL_CONTENT 条目

### MedicalRecordConverter.java 变更明细
- `toRecordGenerateResponse`：硬编码 `MR_GEN_AI_TIMEOUT` 比较替换为 `MedicalRecordErrorCode.valueOf(aiResult.getErrorCode())` 动态解析，未知错误码 catch `IllegalArgumentException` 静默忽略

### MedicalRecordServiceImplTest.java 变更明细
- 新增 `SameThreadExecutor`（private static inner class，implements ExecutorService），同步执行 `execute/submit/invokeAll/invokeAny`
- `setUp()`：`Executors.newSingleThreadExecutor()` → `new SameThreadExecutor()`
- `shouldReturnInterruptedOnInterruptedException`：`aiService.resultFuture` 从 `CompletableFuture.completedFuture(...)` → `new CompletableFuture<>()`，补充 `assertEquals(MR_GEN_AI_INTERRUPTED, response.getErrorCode())`
- `shouldReturnDegradedWhenAiTimesOut`：`assertTrue(response.isSuccess())` → `assertFalse(response.isSuccess())`，`assertEquals(MR_GEN_AI_TIMEOUT, ...)` → `assertEquals(MR_GEN_AI_EXECUTION_ERROR, ...)`
- 更新 imports：移除 `Executors`，新增 `ArrayList`、`Collection`、`Callable`、`ExecutionException`、`Future`、`TimeUnit`、`TimeoutException`

## 编译验证
编译通过（`mvn compile test-compile -q`，无错误输出）。

## 设计偏差说明
无偏差。所有变更严格遵循详细设计 v13 r2 的接口签名、行为契约和错误处理规范。
