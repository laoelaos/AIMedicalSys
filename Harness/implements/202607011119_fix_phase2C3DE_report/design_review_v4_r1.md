# 设计审查报告（v4 r1）

## 审查结果
APPROVED

## 发现

### 已验证的设计要点

1. **AiSuggestionStatus 枚举扩充** ✅ — 当前 `PENDING, COMPLETED, FAILED`（3值），设计目标 `PENDING, PROCESSING, COMPLETED, FAILED, TIMEOUT`（5值），与源文件 `AiSuggestionStatus.java:4` 一致。

2. **DedupTaskScheduler 3 处 PENDING 检查** ✅ — 源文件 `DedupTaskScheduler.java:27,45,53` 确为三处 `r.getStatus() == AiSuggestionStatus.PENDING` 检查，设计要求在每处追加 PROCESSING 条件，符合源文件实际。

3. **PrescriptionAuditServiceImpl catch 块** ✅ — 源文件 `PrescriptionAuditServiceImpl.java:98-105` 三处 catch 均设 `aiResult = null`，设计替换为 `AiResultFactory.failure(errorCode)`，下游代码第110行 `aiResult != null && aiResult.isSuccess()` 可正确感知降级。设计还正确保留了 `Thread.currentThread().interrupt()`（第99行）。

4. **PrescriptionAuditServiceImpl 构造函数** ✅ — 源文件第72-78行构造函数参数列表末尾为 `long aiTimeout`，新增 `DrugFacade drugFacade` 参数位置正确。

5. **PrescriptionAssistServiceImpl 构造函数** ✅ — 源文件第57-65行构造函数参数列表末尾为 `long aiTimeout`，新增 `DrugFacade drugFacade` 参数位置正确。

6. **scheduleSuggestionAsync 拆分 catch 块** ✅ — 源文件第356行确为 `catch (ExecutionException | TimeoutException e)`，设计拆分为独立 TimeoutException（设 TIMEOUT）和 ExecutionException（保持 FAILED），且不影响已有的 `InterruptedException`（第359行）和 `Exception`（第363行）独立 catch 块。

7. **DrugFacade 仅注入不调用** ✅ — 设计明确说明两个服务类仅定义字段+构造函数注入，不引入业务调用，与任务要求一致。`common-module-api` 已在 prescription 模块的 Maven 依赖中（`DrugFacade` 和 `AiResultFactory` 均无需新增依赖）。

8. **测试文件覆盖** ✅ — 4 个测试文件修改均已列出：`AiSuggestionStatusTest`（values length 3→5 + valueOf 断言）、`DedupTaskSchedulerTest`（PROCESSING 去重 + TIMEOUT 重新调度）、`PrescriptionAssistServiceImplTest`（DrugFacade mock）、`PrescriptionAssistControllerTest`（状态机验证）。

9. **v4 r1 审查意见修正** ✅ — 全部 4 项审查意见（AiSuggestionStatusTest 断言修正、DedupTaskScheduler PROCESSING 适配、TimeoutException 独立状态、测试文件补全）均已在设计中修正。

### 潜在考量（**[轻微]**）

- **PROCESSING 在当前代码路径不可达**：`DedupTaskScheduler.schedule()` 存储 dedupKey 条目状态为 PENDING（第35行），而 PROCESSING 在 `scheduleSuggestionAsync` 异步 lambda 内设置后存储于 taskId 键（非 dedupKey 键）。因此 schedule() 中的 PROCESSING 条件在现有代码路径中不会被触发。但这与任务要求的改动一致（任务明确要求追加 PROCESSING 条件），作为防御性安全网有效，且测试通过手动构造 PROCESSING 条目验证行为。

- **行号漂移风险**：设计引用了源文件行号（如第27、45、53行等），实施时若行号变化需按语义匹配。设计已同时提供了语义描述，风险可控。

## 结论

设计完整覆盖了 P02、A01、A03 三个缺陷的全部修改点，4 个源文件和 4 个测试文件的变更规格精确，错误处理与行为契约明确，依赖分析正确，前轮审查意见已全部修正。无严重或一般问题。
