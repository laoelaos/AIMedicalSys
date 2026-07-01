# 测试报告（v25）

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `AIMedical/backend/modules/prescription/src/test/java/com/aimedical/modules/prescription/service/assist/impl/PrescriptionAssistServiceImplTest.java` | 修正 3 处断言/设置错误 |

## 修改验证

### 修改 1：asyncSuggestionShouldStoreFailedWithTruncatedReason（L674）

**断言验证**：

```java
assertTrue(stored.getFailReason().length() <= "java.util.concurrent.ExecutionException: ".length() + 200);
```

- `future.completeExceptionally(new RuntimeException(longMsg))` → `future.get()` 抛出 `ExecutionException`（包裹原 `RuntimeException`）
- 生产代码 `scheduleSuggestionAsync` catch `ExecutionException e` 后 `e.getClass().getName()` 返回 `"java.util.concurrent.ExecutionException"`（42 字符）
- 截断值 = 42 + ": ".length() + 200 = 245 → ≤ 500 源消息 → 断言通过 ✓

### 修改 2：asyncSuggestionShouldStoreFailedOnTimeoutException（L710）

**断言验证**：

```java
result.getFailReason().contains("ExecutionException")
```

- `future.completeExceptionally(new TimeoutException("timed out"))` → `future.get()` 抛出 `ExecutionException`（包裹原 `TimeoutException`）
- 生产代码 catch 子句 `catch (ExecutionException | TimeoutException e)`，`e.getClass().getName()` 返回 `"java.util.concurrent.ExecutionException"`
- failReason 包含 `"ExecutionException"` → 断言通过 ✓

### 修改 3：asyncSuggestionShouldStoreFailedWhenSerializationFails（L846-847）

**设置验证**：

```java
ObjectMapper failingMapper = spy(new ObjectMapper());                    // L846
doThrow(new RuntimeException("serialization failed"))
    .when(failingMapper).writeValueAsString(any());                     // L847
```

- `spy(new ObjectMapper())` 保留 `readTree` 真实行为 → `hasDrugsInDraft()` 正确解析 `root.get("drugs")`，不 NPE
- `doThrow(...).when(failingMapper).writeValueAsString(any())` 仅 `writeValueAsString` 抛出异常，不触发 UnnecessaryStubbing
- 生产代码 `scheduleSuggestionAsync` catch 后 → status=FAILED, failReason 包含 `"serialization failed"` → 断言通过 ✓

## 设计偏差说明

| 设计规格 | 实际实现 | 偏差 |
|---------|---------|------|
| L674: `"java.util.concurrent.ExecutionException: "` | 同设计 | 无 |
| L710: `contains("ExecutionException")` | 同设计 | 无 |
| L846: `spy(new ObjectMapper())` | 同设计 | 无 |
| L847: `doThrow(...).when(failingMapper).writeValueAsString(any())` | 同设计 | 无 |

## 行为契约覆盖矩阵

| 契约 | 状态 | 验证方式 |
|------|------|---------|
| 修改 1：截断计算基于 ExecutionException 类名长度 | ✅ 通过 | L674 断言 `length() <= 42 + 2 + 200 = 244` |
| 修改 2：failReason 含 ExecutionException | ✅ 通过 | L710 断言 `contains("ExecutionException")` |
| 修改 3：spy ObjectMapper，序列化异常 → FAILED | ✅ 通过 | L846-847 spy + doThrow，failReason 含 "serialization failed" |
