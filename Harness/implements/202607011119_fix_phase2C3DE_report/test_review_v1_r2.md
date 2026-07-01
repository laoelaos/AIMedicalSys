# 测试审查报告（v1 r2）

## 审查结果
REJECTED

## 发现

- **[严重]** `TriageServiceImplTest.java:837-854` — `shouldRestoreInterruptFlagOnDoctorFacadeInterrupt` 测试因时序依赖不可靠（flaky）。该测试预中断当前线程后调用 `triage()`，期望 `CompletableFuture.get(timeout, SECONDS)` 抛出 `InterruptedException`，从而触发 catch 块的中断标志恢复逻辑。但 `StubDoctorFacade.findAvailableDoctorsByDepartment()` 仅做快速 HashMap 查询（无延迟），`supplyAsync` 中的任务可能在主线程调用 `get()` 前即完成。当 Future 已先完成时，`get()` 直接返回结果而不检查中断标志，导致：
   1. 医生列表不为空 → `assertTrue(result.getDoctors().isEmpty())` 失败
   2. 实际上未测试到 doctorFacade 调用路径的中断恢复逻辑
   3. 该测试在快速 CI 环境或单次运行时可能间歇性失败

## 修改要求（仅 REJECTED 时）

1. **`TriageServiceImplTest.java:837-854` `shouldRestoreInterruptFlagOnDoctorFacadeInterrupt`**：
   - **问题**：未确保 `supplyAsync` 中的 Future 在 `get()` 调用时尚未完成，导致测试结果依赖线程调度时序，属于 flaky test。
   - **原因**：`doctorFacade.addDoctors("dept-01", ...)` 提供的 stub 操作极其快速（HashMap 查找），`supplyAsync` 可能在主线程执行 `get()` 前已完成并设置 `result`，使 `CompletableFuture.get()` 返回结果而不检查中断标志。
   - **修正方向**：调用 `doctorFacade.addDelay("dept-01", 5000L)` 或 `doctorFacade.addBlocking(...)` 使 supplyAsync 任务延迟完成，确保 `get()` 进入等待路径并触发中断检查。例如在 `doctorFacade.addDoctors(...)` 后追加 `doctorFacade.addDelay("dept-01", 5000L);`，同时保持 `doctorFacadeTimeout=10`（10秒 > 5秒延迟，确保超时不触发）。
