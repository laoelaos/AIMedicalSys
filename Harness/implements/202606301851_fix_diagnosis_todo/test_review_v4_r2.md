# 测试审查报告（v4 r2）

## 审查结果
REJECTED

## 发现

- **[严重]** `prescription/.../task/DraftContextCleanupTaskTest.java:133-142` — `cleanupShouldHandleBoundaryTtl` 使用固定历史时间点 `Instant.parse("2026-06-30T12:00:00Z")` 构造 boundary 时间戳，但生产代码 `cleanupExpiredDrafts()` 内部调用 `Instant.now()` 获取当前时间，测试无法控制该值。当测试在 `2026-06-30T12:00:00Z` 之后执行时，`boundary.plusSeconds(3600) = 2026-06-30T12:00:00Z` 将早于 `Instant.now()`，导致 `isBefore` 返回 `true`，条目被判定为过期并移除，断言 `assertTrue(store.containsKey("boundary-key"))` 失败。r2 修订声称"使用固定时间点消除时间竞争"，但仅修复了时间戳构造端，未解决生产代码 `Instant.now()` 不可控的根本问题。该测试是时间依赖的，在特定日期后将确定性失败，属于不可靠测试。

- **[一般]** `prescription/.../service/assist/impl/PrescriptionAssistServiceImplTest.java:759-787` — `assistShouldNotCallClearCriticalAlertsOnNormalPath` 缺少 `dedupTaskScheduler.schedule(anyString())` 的 mock 配置。生产代码正常路径（line 104）调用 `dedupTaskScheduler.schedule(request.getPrescriptionId())`，未配置 mock 时返回 `null`，导致 `scheduleSuggestionAsync(null, request)` 被调用，异步线程以 `null` taskId 操作 `suggestionStore`。虽然当前 mock 默认行为不会抛异常，但测试遗漏了正常路径必需的 mock 配置，与同类测试 `assistShouldReturnFullResponseWhenAiSuccessWithDrugs`（line 175-195，配置了 `dedupTaskScheduler.schedule`）不一致。若 `DedupTaskScheduler.schedule()` 实现变更增加 null 校验，测试将意外失败。

- **[轻微]** `prescription/.../service/assist/impl/PrescriptionAssistServiceImplTest.java:797-807` — `assistShouldClearCriticalAlertsWhenAiReturnsNullData` 构造 `new AiResult<>(true, null, null, false, null)` 触发 `aiResult.isSuccess()=true` 分支，导致生产代码先进入 line 102-106 的 `scheduleSuggestionAsync` 调用（taskId=null），再进入 line 108-111 的 `clearCriticalAlerts` 路径。测试验证的 `clearCriticalAlerts` 行为正确，但测试隐含地依赖 `scheduleSuggestionAsync(null, request)` 不抛异常的副作用，测试意图与实际执行路径不完全对齐。

## 修改要求（仅 REJECTED 时）

### 严重问题：`cleanupShouldHandleBoundaryTtl` 时间不可控

- **文件**：`prescription/.../task/DraftContextCleanupTaskTest.java`，line 133-142
- **问题**：生产代码 `cleanupExpiredDrafts()` 调用 `Instant.now()`，测试无法控制当前时间，导致 boundary TTL 测试在 `2026-06-30T12:00:00Z` 之后确定性失败
- **为什么是问题**：测试声称消除时间竞争但未真正解决，测试结果依赖执行时刻，不可靠
- **期望修正方向**：方案一（推荐）：为 `DraftContextCleanupTask` 注入 `java.time.Clock` 参数，测试时传入 `Clock.fixed(fixedInstant, ZoneOffset.UTC)`，生产代码使用 `Instant.now(clock)` 替代 `Instant.now()`；方案二：将 boundary 时间戳设为未来时间点（如 `Instant.now().plusSeconds(3600)`），确保 `ts.plusSeconds(3600)` 在 `Instant.now()` 之后，条目不会被判定过期（但此方案无法精确测试 boundary 等值条件）

### 一般问题：`assistShouldNotCallClearCriticalAlertsOnNormalPath` 缺少 mock

- **文件**：`prescription/.../service/assist/impl/PrescriptionAssistServiceImplTest.java`，line 759-787
- **问题**：正常路径测试未配置 `dedupTaskScheduler.schedule(anyString())` mock，导致 `scheduleSuggestionAsync(null, request)` 被意外调用
- **为什么是问题**：测试遗漏了正常路径必需的依赖配置，与同类测试不一致，测试健壮性不足
- **期望修正方向**：在测试中添加 `when(dedupTaskScheduler.schedule(anyString())).thenReturn("test-task-id")`，与 `assistShouldReturnFullResponseWhenAiSuccessWithDrugs` 保持一致
