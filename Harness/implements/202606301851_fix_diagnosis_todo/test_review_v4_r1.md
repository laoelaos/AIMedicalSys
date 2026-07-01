# 测试审查报告（v4 r1）

## 审查结果
REJECTED

## 发现

- **[严重]** `PrescriptionAssistServiceImplTest.java` — `assistShouldNotCallClearCriticalAlertsOnNormalPath`（line 777）的 `verify(prescriptionDraftContext, never()).updateCriticalAlerts(anyString(), eq(Collections.emptyList()))` 在正常路径下必然失败。正常路径 `dosageThresholdService.check(any())` 返回 `new ArrayList<>()`（无 CRITICAL 警告），导致 `criticalContextAlerts` 为空 `ArrayList`，line 164 调用 `updateCriticalAlerts(prescriptionId, emptyArrayList)`。Mockito 的 `eq()` 使用 `equals()` 匹配，而 `new ArrayList<>().equals(Collections.emptyList())` 为 true，因此 `never()` 验证会与正常路径的调用匹配，导致测试自相矛盾地失败。

- **[严重]** `PrescriptionAssistServiceImplTest.java` — `assistShouldClearCriticalAlertsIdempotently`（line 728-737）名为"幂等性"测试，但实际只调用 `service.assist()` 一次并验证 `updateCriticalAlerts` 被调用一次。未测试幂等性契约（设计文档明确要求"重复调用安全"，后置条件为 `getCriticalAlerts` 返回 `emptyList()`）。测试名与行为不匹配，且未覆盖设计 v4 §clearCriticalAlerts 方法契约中的幂等性后置条件。

- **[一般]** `DraftContextCleanupTaskTest.java` — `cleanupShouldHandleBoundaryTtl`（line 133-141）使用 `Instant.now().minusSeconds(3600)` 作为边界值，与 `Instant.now()` 之间存在纳秒级精度竞争。生产代码使用 `isBefore`（严格小于），虽然逻辑上"刚好 3600 秒不过期"是正确的，但测试中两次 `Instant.now()` 调用之间有时间流逝，可能导致间歇性失败。应使用固定时间点构造测试场景，消除时间依赖。

- **[一般]** `PrescriptionAssistServiceImplTest.java` — `assistShouldNotCallClearCriticalAlertsOnNormalPath` 验证正常路径不调用 `clearCriticalAlerts` 的策略有缺陷。即使修复 `never()` 验证问题，该测试依赖 `dosageThresholdService` 返回空列表（因此 `criticalContextAlerts` 为空），但设计文档的契约矩阵要求"正常路径不调用 clearCriticalAlerts，由 updateCriticalAlerts 处理"——验证的关键是确认正常路径通过 `updateCriticalAlerts(prescriptionId, criticalContextAlerts)` 写入实际（可能非空）的 CRITICAL 警告，而非通过 `clearCriticalAlerts` 写入空列表。应构造至少含一个 CRITICAL 警告的场景来验证。

- **[轻微]** `PrescriptionAssistServiceImplTest.java` — `assistShouldClearCriticalAlertsWhenAiReturnsNullData`（line 788-798）与 `assistShouldClearCriticalAlertsWhenAiResultNotSuccess`（line 162-172）覆盖了设计文档中同一条路径 `aiData == null || !aiResult.isSuccess()`，但两个测试分别触发条件的左右子句。这是合理的覆盖策略，但测试报告覆盖矩阵中两者被列为覆盖不同行（aiData==null 和 !aiResult.isSuccess()），而生产代码 line 108 是一个 if 条件同时检查两者，测试报告暗示它们覆盖不同执行路径是不准确的——它们覆盖的是同一执行路径的不同入口条件。

## 修改要求（仅 REJECTED 时）

1. **`PrescriptionAssistServiceImplTest.java` line 757-778**：`assistShouldNotCallClearCriticalAlertsOnNormalPath` 中 `never()` 验证与正常路径的 `updateCriticalAlerts(prescriptionId, emptyArrayList)` 调用矛盾。应改为：构造 `dosageThresholdService.check()` 返回含至少一个 CRITICAL 级别警报的场景，使 `criticalContextAlerts` 非空，然后用 `ArgumentCaptor` 捕获 `updateCriticalAlerts` 的参数列表，断言该列表非空（即不是 `Collections.emptyList()`），从而验证正常路径确实写入了实际 CRITICAL 告警而非清除。

2. **`PrescriptionAssistServiceImplTest.java` line 728-737**：`assistShouldClearCriticalAlertsIdempotently` 应实际测试幂等性：连续调用两次 `clearCriticalAlerts`（可通过两次 `service.assist()` 触发，或通过反射直接调用两次私有方法），验证第二次调用不抛异常且 `updateCriticalAlerts` 被调用两次（或验证最终状态 `getCriticalAlerts` 返回空列表），而非仅验证单次调用的行为。

3. **`DraftContextCleanupTaskTest.java` line 133-141**：`cleanupShouldHandleBoundaryTtl` 应使用固定时间点消除 `Instant.now()` 的时间竞争。例如用 `Clock.fixed()` 注入固定时间，或在 `recordWrite` 后手动验证 `plusSeconds(3600)` 是否 `isBefore` 该固定时间点，而非依赖两次 `Instant.now()` 调用之间的微秒级差异。
