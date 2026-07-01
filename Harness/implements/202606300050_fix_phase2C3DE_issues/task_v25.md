# 任务指令（v25）

## 动作
RETRY

## 任务描述
修复 PrescriptionAssistServiceImplTest 中 2 个测试失败（R22/R23 异步 AI 调度遗留问题，R24 全量构建首次曝光）

## 选择理由
R24 代码变更（TTL+事件+定时任务）编译正确，但因 PrescriptionAssistServiceImplTest 2 个预存测试失败导致全量构建阻断。这两个失败是 R22/R23（异步 AI 调度 + PENDING→COMPLETED/FAILED 状态映射 + SuggestionStore）的遗留问题，此前因编译错误阻断从未被实际运行过。

## 任务上下文

### 失败 1：asyncSuggestionShouldStoreFailedWithTruncatedReason（L674）
- 测试用 `future.completeExceptionally(new RuntimeException("x".repeat(500)))` 触发的异常
- `scheduleSuggestionAsync` 中 `future.get()` 将 RuntimeException 包装为 ExecutionException
- 生产代码 catch 块使用 `e.getClass().getName()` 作为 failReason 前缀 → `"java.util.concurrent.ExecutionException: "`（42 字符）
- 测试断言写死 `"java.lang.RuntimeException: "`（26 字符）→ 42+200 = 242 > 26+200 = 226 → 断言失败

### 失败 2：asyncSuggestionShouldStoreFailedWhenSerializationFails（L839-841）
- 测试创建 `failingMapper = mock(ObjectMapper.class)`，仅 stub `writeValueAsString` 抛出异常
- 但 `testService` 中 `failingMapper` 替换了 `objectMapper`，而 `assist()` 方法中 `hasDrugsInDraft()` 调用 `objectMapper.readTree(draftJson)` → mock 返回 null
- `root.get("drugs")` 导致 NPE → catch 返回 false → 方法提前 return（empty response）
- `assistConverter.toPrescriptionAssistResponse()`、`allergyCheckRule.check()`、`dosageThresholdService.check()` 均未被调用 → Mockito strict stubbing 报告 UnnecessaryStubbing

## RETRY 说明

### 修复方案

#### 修复 1：asyncSuggestionShouldStoreFailedWithTruncatedReason
L674: `assertTrue(stored.getFailReason().length() <= "java.lang.RuntimeException: ".length() + 200);`
→ `assertTrue(stored.getFailReason().length() <= "java.util.concurrent.ExecutionException: ".length() + 200);`

#### 修复 1b：asyncSuggestionShouldStoreFailedOnTimeoutException
L710-711: `result.getFailReason().contains("TimeoutException")` — `CompletableFuture.completeExceptionally(new TimeoutException("timed out"))` 后 `future.get()` 抛出 `ExecutionException`（包装 TimeoutException），生产代码 catch `ExecutionException | TimeoutException e` 后 `e.getClass().getName()` 返回 `"java.util.concurrent.ExecutionException"`，failReason 中不会包含 "TimeoutException"。

修正方向：L710 `contains("TimeoutException")` → `contains("ExecutionException")`。
同时该测试的 `future.get()` 超时由 `future.get(aiTimeout, TimeUnit.SECONDS)` 触发而非 `completeExceptionally`——注意测试中异步 future 使用 `completeExceptionally(new TimeoutException(...))` 模拟超时场景，正常路径会因 `ExecutionException` 捕获，断言验证 `ExecutionException` 类名前缀即可。

#### 修复 2：asyncSuggestionShouldStoreFailedWhenSerializationFails
L846: `ObjectMapper failingMapper = mock(ObjectMapper.class);`
→ `ObjectMapper failingMapper = spy(new ObjectMapper());`
L847: `when(failingMapper.writeValueAsString(any())).thenThrow(...)`
→ `doThrow(new RuntimeException("serialization failed")).when(failingMapper).writeValueAsString(any());`

使 `readTree` 走真实逻辑（`hasDrugsInDraft` 正确返回 true），仅 `writeValueAsString` 抛出异常。

## 已有代码上下文
- `PrescriptionAssistServiceImplTest.java` — 测试文件，lines 637-675（truncated reason 测试），lines 678-712（timeout 测试），lines 825-862（serialization fails 测试），MockitoExtension strict stubbing 模式
- `PrescriptionAssistServiceImpl.java` — 生产代码，scheduleSuggestionAsync 中 catch ExecutionException|TimeoutException 使用 `e.getClass().getName()` 作为 failReason 前缀

---

## 修订说明（v25 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| Task v25 对 TimeoutException 测试断言问题未给出完整修复方案（仅标注"需同步修复"但未提供具体断言修正方向） | 补充「修复 1b」节，给出 L710 `contains("TimeoutException")` → `contains("ExecutionException")` 的完整修正方案及原理说明 |