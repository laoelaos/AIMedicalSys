# 设计审查报告（v13 r1）

## 审查结果
REJECTED

## 发现

### **[严重] F6 设计缺陷：resolveVisitId 抢先消费中断标记，callAiWithTimeout 无法感知中断**

**问题**：F6 方案将 `aiService.resultFuture` 从 `completedFuture(...)` 改为 `new CompletableFuture<>()`，期望 `callAiWithTimeout` 中的 `.get(12, SECONDS)` 检测到线程中断标记并抛出 `InterruptedException`。但 `generate()` 方法在调用 `callAiWithTimeout` **之前**先调用了 `resolveVisitId()`（`MedicalRecordServiceImpl.java:81`），而 `resolveVisitId()` 内部也使用 `future.get(visitFacadeTimeout, SECONDS)`（第144行），该调用在等待时同样会检查 `Thread.interrupted()`。

因此实际执行路径为：
1. `Thread.currentThread().interrupt()` 设置中断标记
2. `resolveVisitId()` 的 `future.get()` 检测到中断标记 → 消费标记并抛出 `InterruptedException`
3. `resolveVisitId()` 的 `catch (Exception e)` 捕获后返回 fallback 结果（中断标记已清除）
4. `callAiWithTimeout()` 的 `future.get()` 再执行时中断标记已被清除，且 `CompletableFuture<>()` 未完成 → **阻塞 12 秒**后抛出 `TimeoutException`
5. `TimeoutException` 路径返回 `MR_GEN_AI_TIMEOUT`，该错误码在 `success` 条件中被视为成功 → `response.isSuccess() = true`
6. 测试断言 `assertFalse(response.isSuccess())` **必然失败**

即使线程调度使 `resolveVisitId` 的 `supplyAsync` 在中断检查前完成（此时 `get()` 返回已完成结果不检查中断），该行为也**依赖于竞态条件**，不是一个确定性正确的设计。

**期望的修正方向**：F6 方案必须解决 `resolveVisitId` 抢先消费中断标记的问题。可能的方案包括：
- 在 `resolveVisitId` 成功返回后、`callAiWithTimeout` 调用前再次设置中断标记（需在测试中通过 spy/partial mock 或重构实现）
- 或调整 `resolveVisitId` 的 mock 使其不使用 future-based 调用（如直接返回）
- 或采用其他不依赖中断标记跨方法传递的测试策略

### **[轻微] F6 + F7a 组合场景：shouldReturnInterruptedOnInterruptedException 缺少错误码断言**

若 F6 被正确修复使中断到达 `callAiWithTimeout`，则 `InterruptedException` 路径的 `AiResult` 携带 `errorCode="MR_GEN_AI_INTERRUPTED"`。F7a 的 `valueOf()` 动态解析将正确设置 `response.errorCode = MedicalRecordErrorCode.MR_GEN_AI_INTERRUPTED`，且 `success=false`。现有断言（`assertFalse(isSuccess)` + `assertTrue(isDegraded)`）均能通过，但该测试未对 `errorCode` 进行断言。建议补充 `assertEquals(MedicalRecordErrorCode.MR_GEN_AI_INTERRUPTED, response.getErrorCode())` 以覆盖此路径。

## 修改要求

1. **[严重]** F6 方案必须修正 `resolveVisitId` 抢先消费中断标记的问题。具体方向见上。
2. **[轻微]** 建议在 `shouldReturnInterruptedOnInterruptedException` 中补充 `errorCode` 断言以验证中断路径的错误码映射。
