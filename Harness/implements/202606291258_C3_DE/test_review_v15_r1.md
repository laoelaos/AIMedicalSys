# 测试审查报告（v15 R1）

## 审查结果
REJECTED

## 发现

- **[严重]** `MedicalRecordServiceImplTest.java:192-203` — 测试 `shouldSetVisitIdFallbackWhenEncounterIdFallbackUsed` 使用 `fallback=true` 标志让 StubVisitFacade 直接返回 encounterId（不抛异常）。但服务端 `resolveVisitId()` 仅在 catch `TimeoutException`/`Exception` 后才设置 `fallback=true`（`VisitResolveResult(encounterId, true)`）。Stub 正常返回值时，服务返回 `VisitResolveResult(visitId, false)`，导致 `response.isFromFallback()` 实际为 `false`，断言 `assertTrue(response.isFromFallback())` 必然失败。测试对生产实现无效。

- **[严重]** `MedicalRecordServiceImplTest.java:107-120` — 测试 `shouldReturnDegradedWhenAiTimesOut` 使用 `new CompletableFuture<>()`（永不完结的 Future）模拟 AI 超时。服务端 `callAiWithTimeout()` 调用 `future.get(12, TimeUnit.SECONDS)` 会阻塞完整 12 秒才抛出 `TimeoutException`。单条测试耗时 12 秒，使测试套件不可靠且无法作为单元测试快速反馈。

- **[一般]** `MedicalRecordServiceImplTest.java:78-89` — 测试 `shouldUseFallbackWhenVisitFacadeTimesOut` 中 StubVisitFacade 做 `Thread.sleep(3000)` 导致最少等待 2 秒（`future.get(2, TimeUnit.SECONDS)` 超时）。Stub 可直接抛出异常以模拟超时，避免不必要的实际等待。

- **[轻微]** `MedicalRecordControllerTest.java` — 未验证 `stream=true` 时 Service 确实未被调用（仅验证返回值为 Result.fail）。若未来实现修改为在 `stream=true` 时仍调用 Service，测试仍可通过，形成假阴性。

- **[轻微]** `MedicalRecordContentConverterTest.java:63-65` — 仅测试全部 key 为未知枚举时的反序列化降级（全部返回空 Map），未覆盖部分 key 有效、部分 key 无效的混合场景。

## 修改要求（仅 REJECTED 时）

1. **`MedicalRecordServiceImplTest.java:192-203`** — 问题：`fallback=true` 标志导致 Stub 正常返回而非抛异常，服务端不会进入降级路径。修正方向：移除 `fallback` 标志，改为设置 `throwException=true` 以触发 `resolveVisitId()` 的 `catch (Exception e) → VisitResolveResult(encounterId, true)` 路径。

2. **`MedicalRecordServiceImplTest.java:107-120`** — 问题：`new CompletableFuture<>()` 导致 12 秒的阻塞等待。修正方向：使用可提前完成的 Future，如 `CompletableFuture.supplyAsync(() -> { throw new RuntimeException("timeout"); })`，使 `future.get(12s)` 立即抛出 `ExecutionException`，被 `callAiWithTimeout()` 捕获后依旧走降级路径。

3. **`MedicalRecordServiceImplTest.java:78-89`** — 问题：不必要的 `Thread.sleep(3000)` 延长测试执行时间。修正方向：直接抛异常模拟超时（如 `throw new RuntimeException("模拟超时")`），无需 sleep。
