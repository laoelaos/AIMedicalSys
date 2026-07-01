# 任务指令（v4）

## 动作
NEW

## 任务描述
修复 prescription 模块三个缺陷：
1. **P02**: DrugFacade 注入 — 在 PrescriptionAuditServiceImpl 和 PrescriptionAssistServiceImpl 构造函数注入 DrugFacade 字段
2. **A01**: AiResultFactory 使用 — PrescriptionAuditServiceImpl 的 catch 块中将 `aiResult = null` 替换为 `AiResultFactory.failure(errorCode)`
3. **A03**: AiSuggestionStatus 枚举扩充 — 增加 PROCESSING 和 TIMEOUT 状态

## 选择理由
R3 已完成 consultation 模块全部缺陷修复。按实施路线优先级，转入 prescription 模块 P0 级别缺陷。三个缺陷独立且修改范围小，合并一轮完成。

## 任务上下文
来自需求：
- **P02**: PrescriptionAuditServiceImpl.java:72-87 和 PrescriptionAssistServiceImpl.java:57-75 构造函数均无 DrugFacade 参数。DrugFacade.findByDrugCode() 在 prescription 模块中无法使用
- **A01**: PrescriptionAuditServiceImpl.java:96-105 的三种异常 catch 块均将 aiResult 设为 null，降级语义丢失。应使用 AiResultFactory.failure(errorCode) 构造明确错误码的 AiResult
- **A03**: AiSuggestionStatus 当前仅有 PENDING/COMPLETED/FAILED，缺少 PROCESSING（异步调度中间状态）和 TIMEOUT（AI 超时）

## 已有代码上下文
- `DrugFacade`：`common-module-api/.../commonmodule/drug/DrugFacade.java` — interface，方法 `findByDrugCode(String)`
- `AiResultFactory`：`ai-api/.../ai/api/AiResultFactory.java` — 静态工厂，含 `failure(errorCode)`、`degraded(reason, data)`、`success(data)`
- `AiSuggestionStatus`：`prescription/.../dto/assist/AiSuggestionStatus.java` — 当前 3 值；被 DedupTaskScheduler.schedule 和 PrescriptionAssistServiceImpl.getSuggestion 引用
- `PrescriptionAuditServiceImpl.audit()`：第96-105行三种异常 set aiResult=null

### 变更要点

1. **AiSuggestionStatus.java**: `PENDING, PROCESSING, COMPLETED, FAILED, TIMEOUT`
2. **PrescriptionAuditServiceImpl.java**:
   - 新增 `private final DrugFacade drugFacade;` 字段
   - 构造函数参数新增 `DrugFacade drugFacade`
   - catch InterruptedException: `aiResult = AiResultFactory.failure("RX_AUDIT_AI_INTERRUPTED");`
   - catch ExecutionException: `aiResult = AiResultFactory.failure("RX_AUDIT_AI_EXECUTION_ERROR");`
   - catch TimeoutException: `aiResult = AiResultFactory.failure("RX_AUDIT_AI_TIMEOUT");`
3. **PrescriptionAssistServiceImpl.java**:
   - 新增 `private final DrugFacade drugFacade;` 字段
   - 构造函数参数新增 `DrugFacade drugFacade`
   - scheduleSuggestionAsync() 第341行后插入 `result.setStatus(AiSuggestionStatus.PROCESSING);`

### 涉及文件
- `prescription/.../dto/assist/AiSuggestionStatus.java`
- `prescription/.../service/assist/DedupTaskScheduler.java`
- `prescription/.../service/audit/impl/PrescriptionAuditServiceImpl.java`
- `prescription/.../service/assist/impl/PrescriptionAssistServiceImpl.java`

### 测试文件
- `prescription/.../dto/assist/AiSuggestionStatusTest.java`（枚举值个数断言 3→5，追加 PROCESSING/TIMEOUT valueOf 断言）
- `prescription/.../api/PrescriptionAssistControllerTest.java`（验证状态机变更）
- `prescription/.../service/assist/DedupTaskSchedulerTest.java`（补充 PROCESSING/TIMEOUT 状态的调度行为测试）
- `prescription/.../service/assist/impl/PrescriptionAssistServiceImplTest.java`（构造函数新增 DrugFacade 参数，需补充 mock）

### 修订变更说明

根据 v4 r1 审查意见，补充以下 4 项修改：

1. **AiSuggestionStatusTest.java**：`assertEquals(3, ...)` → `assertEquals(5, ...)`，追加 `assertEquals(AiSuggestionStatus.PROCESSING, AiSuggestionStatus.valueOf("PROCESSING"))` 和 `assertEquals(AiSuggestionStatus.TIMEOUT, AiSuggestionStatus.valueOf("TIMEOUT"))`

2. **DedupTaskScheduler.java**：`schedule()` 中 3 处 `r.getStatus() == AiSuggestionStatus.PENDING` 检查追加 `|| r.getStatus() == AiSuggestionStatus.PROCESSING`，防止 PROCESSING 状态任务被重复调度（第27、45、53行）

3. **PrescriptionAssistServiceImpl.java**：`scheduleSuggestionAsync()` 中拆分 `catch (ExecutionException | TimeoutException e)` 为独立 catch 块——`TimeoutException` 分支设为 `AiSuggestionStatus.TIMEOUT`，`ExecutionException` 分支保持 `FAILED`

4. **测试文件补全**：`DedupTaskSchedulerTest.java` 新增 PROCESSING/TIMEOUT 状态的调度去重测试用例；`PrescriptionAssistServiceImplTest.java` 新增 `DrugFacade` mock 参数

## 修订说明（v4 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] AiSuggestionStatusTest.java 断言在枚举扩充后必然失败 | 补充 AiSuggestionStatusTest.java 修改；R4 变更清单补充此测试文件 |
| [一般] DedupTaskScheduler 未适配 PROCESSING/TIMEOUT 新状态 | 补充 DedupTaskScheduler.schedule() 中 3 处 PENDING 检查追加 PROCESSING 条件 |
| [一般] scheduleSuggestionAsync 中 TimeoutException 未使用 TIMEOUT 状态 | 拆分 catch 块，TimeoutException 分支设为 TIMEOUT |
| [一般] 测试文件修改清单不完整 | 补全测试文件清单：AiSuggestionStatusTest/DedupTaskSchedulerTest/PrescriptionAssistServiceImplTest |
