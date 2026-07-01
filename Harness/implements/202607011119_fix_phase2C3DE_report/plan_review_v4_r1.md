# 计划审查报告（v4 r1）

## 审查结果
REJECTED

## 发现

### [严重] AiSuggestionStatusTest.java 断言在枚举扩充后必然失败
- **文件**：`prescription/src/test/java/.../dto/assist/AiSuggestionStatusTest.java:10`
- **问题**：`assertEquals(3, AiSuggestionStatus.values().length)` — 当前断言值为 3（PENDING/COMPLETED/FAILED），扩充至 5 个枚举值（+PROCESSING/TIMEOUT）后该断言将断言失败
- **Plan 覆盖**：R4 节仅列出 3 个源文件，未列出此测试文件的必要修改

### [一般] DedupTaskScheduler 未适配 PROCESSING/TIMEOUT 新状态
- **问题**：`DedupTaskScheduler.schedule()` 中 3 处（L27-28、L45-46、L53-54）检查 `r.getStatus() == PENDING` 或 `COMPLETED && !consumed` 来决定是否跳过重新调度。PROCESSING 状态（异步 AI 处理中）未被包含，将导致处理中的任务被重复调度，违反状态机语义。TIMEOUT 状态在调度去重逻辑中的处理也未定义。
- **Plan 覆盖**：Plan L108 仅写"需确认"但未给出明确的适配方向和具体修改点

### [一般] scheduleSuggestionAsync 中 TimeoutException 未使用 TIMEOUT 状态
- **文件**：`PrescriptionAssistServiceImpl.java:356`
- **问题**：`catch (ExecutionException | TimeoutException e)` 统一设为 `FAILED`。既然 A03 新增了 `TIMEOUT` 枚举值，`TimeoutException` 分支应拆分并设为 `result.setStatus(AiSuggestionStatus.TIMEOUT)` 以匹配状态机语义。当前 Plan 仅提及枚举扩充和 PROCESSING 设置，未覆盖此修改。
- **Plan 覆盖**：缺失

### [一般] 测试文件修改清单不完整
- **问题**：Plan R4 节仅列出 3 个源文件，未提及以下测试文件所需的必要修改：
  1. `AiSuggestionStatusTest.java` — 枚举值个数断言从 3→5，补充 PROCESSING/TIMEOUT 的 valueOf 断言
  2. `PrescriptionAssistServiceImplTest.java` — 构造函数新增 `DrugFacade` 参数，测试中若手动构造 service 需补充 mock 参数
  3. `DedupTaskSchedulerTest.java` — 需补充 PROCESSING/TIMEOUT 状态的调度行为测试
- **task_v4.md** 已明确指明了前两个测试文件，但 Plan 未将其纳入修改清单

## 修改要求（仅 REJECTED 时）

1. **[严重]** 在 Plan R4 节补充 `AiSuggestionStatusTest.java` 的修改：`assertEquals(3, ...)` → `assertEquals(5, ...)`，追加 `assertEquals(AiSuggestionStatus.PROCESSING, AiSuggestionStatus.valueOf("PROCESSING"))` 和 `assertEquals(AiSuggestionStatus.TIMEOUT, AiSuggestionStatus.valueOf("TIMEOUT"))` 断言

2. **[一般]** 在 Plan R4 节补充 `DedupTaskScheduler.java` 的修改：`schedule()` 中所有 `r.getStatus() == AiSuggestionStatus.PENDING` 的检查追加 `|| r.getStatus() == AiSuggestionStatus.PROCESSING` 条件，防止 PROCESSING 状态的任务被重复调度

3. **[一般]** 在 Plan R4 节补充 `scheduleSuggestionAsync` 的 TimeoutException 拆分：将 `catch (ExecutionException | TimeoutException e)` 拆分为独立 catch 块，`TimeoutException` 分支设为 `AiSuggestionStatus.TIMEOUT`

4. **[一般]** 在 Plan R4 节补全测试文件清单：`AiSuggestionStatusTest.java`、`PrescriptionAssistServiceImplTest.java`、`DedupTaskSchedulerTest.java` 的修改要点
