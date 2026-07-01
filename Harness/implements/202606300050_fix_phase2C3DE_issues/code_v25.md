# 实现报告（v25）

## 概述
修复 `PrescriptionAssistServiceImplTest.java` 中 3 处测试断言/设置错误，不修改生产代码。

## 文件变更清单
| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `AIMedical/backend/modules/prescription/src/test/java/com/aimedical/modules/prescription/service/assist/impl/PrescriptionAssistServiceImplTest.java` | 修复 3 处断言/设置错误 |

## 修改详情

### 修改 1：L674 — asyncSuggestionShouldStoreFailedWithTruncatedReason
- **原**：`"java.lang.RuntimeException: ".length()`
- **改**：`"java.util.concurrent.ExecutionException: ".length()`
- **原因**：`future.completeExceptionally(RuntimeException)` → `future.get()` 抛出 `ExecutionException`，生产代码取该类名。

### 修改 2：L710 — asyncSuggestionShouldStoreFailedOnTimeoutException
- **原**：`contains("TimeoutException")`
- **改**：`contains("ExecutionException")`
- **原因**：`future.completeExceptionally(TimeoutException)` → `future.get()` 抛出 `ExecutionException`，生产代码 catch `ExecutionException` 后取该类名。

### 修改 3：L846-847 — asyncSuggestionShouldStoreFailedWhenSerializationFails
- **原**：`mock(ObjectMapper.class)` + `when(...).thenThrow(...)`
- **改**：`spy(new ObjectMapper())` + `doThrow(...).when(failingMapper).writeValueAsString(any())`
- **原因**：mock 默认 `readTree` 返回 null → NPE → 提前 return → UnnecessaryStubbing。spy 保留真实 `readTree` 行为，仅 `writeValueAsString` 抛出异常。

## 编译验证
未执行编译验证（无构建工具链）。

## 设计偏差说明
无偏差。
