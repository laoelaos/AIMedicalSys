# 详细设计（v25）

## 概述

修复 `PrescriptionAssistServiceImplTest` 中 3 个测试断言/设置错误（R22/R23 异步 AI 调度遗留 + R24 首次曝光）。不修改生产代码，仅修正测试代码中对异步异常包装逻辑的误解（`ExecutionException` 包裹原异常）以及 mock 策略导致 NPE 的问题。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `modules/prescription/src/test/java/.../PrescriptionAssistServiceImplTest.java` | 修改 | 修复 3 处测试断言/设置错误 |

## 修改详情

### 修改 1：asyncSuggestionShouldStoreFailedWithTruncatedReason（L674）

**位置**：`PrescriptionAssistServiceImplTest.java` 第 674 行

**问题**：测试用 `future.completeExceptionally(new RuntimeException(...))` 触发异常，`future.get()` 将 `RuntimeException` 包装为 `ExecutionException`。生产代码 `scheduleSuggestionAsync` catch 块使用 `e.getClass().getName()` 获取异常类名，返回 `"java.util.concurrent.ExecutionException"`（42 字符）。断言期望前缀 `"java.lang.RuntimeException: "`（26 字符），导致长度计算不等。

**修改**：`"java.lang.RuntimeException: "` → `"java.util.concurrent.ExecutionException: "`

### 修改 2：asyncSuggestionShouldStoreFailedOnTimeoutException（L710）

**位置**：`PrescriptionAssistServiceImplTest.java` 第 710 行

**问题**：测试用 `future.completeExceptionally(new TimeoutException("timed out"))` 模拟超时，`future.get()` 将 `TimeoutException` 包装为 `ExecutionException`。生产代码 `catch (ExecutionException | TimeoutException e)` 捕获后 `e.getClass().getName()` 返回 `"java.util.concurrent.ExecutionException"`，failReason 中不会包含 `"TimeoutException"` 子串。

**修改**：`contains("TimeoutException")` → `contains("ExecutionException")`

### 修改 3：asyncSuggestionShouldStoreFailedWhenSerializationFails（L846-847）

**位置**：`PrescriptionAssistServiceImplTest.java` 第 846-847 行

**问题**：`mock(ObjectMapper.class)` 使得 `readTree` 返回 null（默认 mock 行为），导致 `assist()` 中 `hasDrugsInDraft()` 调用 `root.get("drugs")` → NPE → catch 返回 false → 方法提前 return，后续 `toPrescriptionAssistResponse`、`allergyCheckRule.check()`、`dosageThresholdService.check()` 均未被执行，触发 Mockito UnnecessaryStubbing。

**修改**：
- L846：`mock(ObjectMapper.class)` → `spy(new ObjectMapper())`，使 `readTree` 走真实逻辑
- L847：`when(failingMapper.writeValueAsString(any())).thenThrow(...)` → `doThrow(new RuntimeException("serialization failed")).when(failingMapper).writeValueAsString(any())`，确保仅 `writeValueAsString` 抛出异常

## 错误处理

无新增错误处理逻辑。测试修改不改变生产代码行为。

## 行为契约

- 修改 1 后断言：`stored.getFailReason().length() <= "java.util.concurrent.ExecutionException: ".length() + 200`（42 + 200 = 242 ≤ 500 源消息截断后 242）→ 通过
- 修改 2 后断言：`result.getFailReason().contains("ExecutionException")` → 通过
- 修改 3 后设置：`spy(ObjectMapper)` 使 `hasDrugsInDraft` 正确解析 drug JSON → `writeValueAsString` 抛出异常 → `scheduleSuggestionAsync` catch 后存入 `FAILED` + `failReason` 包含 `"serialization failed"` → 通过

## 依赖关系

无新增依赖。仅修改测试文件，生产代码不变。
