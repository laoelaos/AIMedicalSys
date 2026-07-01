# 详细设计（v4）

## 概述

修复 prescription 模块三个缺陷：P02（DrugFacade 注入）、A01（AiResultFactory 使用）、A03（AiSuggestionStatus 枚举扩充），以及四项审查反馈补充修改。涉及 4 个源文件修改和 4 个测试文件更新。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `prescription/.../dto/assist/AiSuggestionStatus.java` | 修改 | 枚举值从 3 个扩充至 5 个：追加 PROCESSING、TIMEOUT |
| `prescription/.../service/audit/impl/PrescriptionAuditServiceImpl.java` | 修改 | 新增 DrugFacade 字段+构造函数参数；catch 块 AiResultFactory.failure() |
| `prescription/.../service/assist/impl/PrescriptionAssistServiceImpl.java` | 修改 | 新增 DrugFacade 字段+构造函数参数；PROCESSING 状态插入；拆分 catch 块 |
| `prescription/.../service/assist/DedupTaskScheduler.java` | 修改 | PENDING 检查追加 PROCESSING 条件 |
| `prescription/.../dto/assist/AiSuggestionStatusTest.java` | 修改 | 断言 3→5，追加 valueOf 断言 |
| `prescription/.../service/assist/DedupTaskSchedulerTest.java` | 修改 | 新增 PROCESSING/TIMEOUT 调度去重测试 |
| `prescription/.../service/assist/impl/PrescriptionAssistServiceImplTest.java` | 修改 | 构造函数新增 DrugFacade mock 参数 |
| `prescription/.../api/PrescriptionAssistControllerTest.java` | 修改 | 验证状态机变更 |

## 类型定义

### `AiSuggestionStatus`（prescription DTO，已有枚举，修改）

**形态**：enum
**包路径**：`com.aimedical.modules.prescription.dto.assist`
**职责**：AI 建议任务生命周期状态标志

**修改点**：枚举常量列表末尾追加 `, PROCESSING, TIMEOUT`

```
// 修改前：
PENDING, COMPLETED, FAILED

// 修改后：
PENDING, PROCESSING, COMPLETED, FAILED, TIMEOUT
```

**引用检查**：
- `DedupTaskScheduler.schedule()`：第27、45、53行 `r.getStatus() == AiSuggestionStatus.PENDING` 需补充 `|| r.getStatus() == AiSuggestionStatus.PROCESSING`
- `PrescriptionAssistServiceImpl.getSuggestion()`：第218行 `result.getStatus() == AiSuggestionStatus.COMPLETED` — 不受影响
- `PrescriptionAssistServiceImpl.scheduleSuggestionAsync()`：第341行后插入 `result.setStatus(AiSuggestionStatus.PROCESSING)`；第356行 `catch (ExecutionException | TimeoutException)` 拆分为独立 catch，TimeoutException 分支设 `AiSuggestionStatus.TIMEOUT`

### `PrescriptionAuditServiceImpl`（已有类，修改）

**包路径**：`com.aimedical.modules.prescription.service.audit.impl`
**职责**：处方审核服务实现；注入 DrugFacade + 使用 AiResultFactory 显式构造降级结果

**新增导入**：
```
import com.aimedical.modules.commonmodule.drug.DrugFacade;
import com.aimedical.modules.ai.api.AiResultFactory;
```

**新增字段**：
```java
private final DrugFacade drugFacade;
```

**修改构造函数**：参数列表末尾新增 `DrugFacade drugFacade`；赋值 `this.drugFacade = drugFacade;`

构造函数签名变化：
```java
// 当前（第72-78行）：
public PrescriptionAuditServiceImpl(AiService aiService, LocalRuleEngine localRuleEngine,
                                     AuditRecordRepository auditRecordRepository,
                                     AuditConverter auditConverter,
                                     PrescriptionDraftContext prescriptionDraftContext,
                                     CurrentUser currentUser,
                                     ObjectMapper objectMapper,
                                     @Value("${ai.timeout.prescription-audit:6}") long aiTimeout)

// 修改后：
public PrescriptionAuditServiceImpl(AiService aiService, LocalRuleEngine localRuleEngine,
                                     AuditRecordRepository auditRecordRepository,
                                     AuditConverter auditConverter,
                                     PrescriptionDraftContext prescriptionDraftContext,
                                     CurrentUser currentUser,
                                     ObjectMapper objectMapper,
                                     @Value("${ai.timeout.prescription-audit:6}") long aiTimeout,
                                     DrugFacade drugFacade)
```

**修改 catch 块**（audit() 方法第96-105行）：三个 catch 块 `aiResult = null` 替换为：

```java
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    aiResult = AiResultFactory.failure("RX_AUDIT_AI_INTERRUPTED");
} catch (ExecutionException e) {
    aiResult = AiResultFactory.failure("RX_AUDIT_AI_EXECUTION_ERROR");
} catch (TimeoutException e) {
    aiResult = AiResultFactory.failure("RX_AUDIT_AI_TIMEOUT");
}
```

**不修改**：`submit()`、`revoke()`、`persistAuditRecord()` 等方法；DrugFacade 字段在当前任务中仅定义+注入，不引入 drugFacade 的业务调用。

### `PrescriptionAssistServiceImpl`（已有类，修改）

**包路径**：`com.aimedical.modules.prescription.service.assist.impl`
**职责**：辅助开方服务实现；注入 DrugFacade + 调度异步建议时设置 PROCESSING 状态 + TimeoutException 独立处理

**新增导入**：
```
import com.aimedical.modules.commonmodule.drug.DrugFacade;
```

**新增字段**：
```java
private final DrugFacade drugFacade;
```

**修改构造函数**：参数列表末尾新增 `DrugFacade drugFacade`；赋值 `this.drugFacade = drugFacade;`

构造函数签名变化：
```java
// 当前（第57-65行）：
public PrescriptionAssistServiceImpl(AiService aiService,
                                      AssistConverter assistConverter,
                                      AllergyCheckRule allergyCheckRule,
                                      DosageThresholdService dosageThresholdService,
                                      PrescriptionDraftContext prescriptionDraftContext,
                                      DedupTaskScheduler dedupTaskScheduler,
                                      SuggestionStore suggestionStore,
                                      ObjectMapper objectMapper,
                                      @Value("${ai.timeout.prescription-assist:8}") long aiTimeout)

// 修改后：参数列表末尾新增 DrugFacade drugFacade
```

**修改 `scheduleSuggestionAsync()` 方法**（当前第339-369行）：

1. **第341行后插入**：
```java
result.setStatus(AiSuggestionStatus.PROCESSING);
```

2. **拆分 catch 块**（第356行）：当前 `catch (ExecutionException | TimeoutException e)` 拆分为两个独立 catch：

```java
} catch (TimeoutException e) {
    result.setStatus(AiSuggestionStatus.TIMEOUT);
    result.setFailReason(e.getClass().getName() + ": " + (e.getMessage() != null && e.getMessage().length() > 200 ? e.getMessage().substring(0, 200) : e.getMessage()));
} catch (ExecutionException e) {
    result.setStatus(AiSuggestionStatus.FAILED);
    result.setFailReason(e.getClass().getName() + ": " + (e.getMessage() != null && e.getMessage().length() > 200 ? e.getMessage().substring(0, 200) : e.getMessage()));
}
```

**不修改**：`assist()`、`checkDose()`、`getSuggestion()` 等方法；DrugFacade 字段在当前任务中仅定义+注入，不引入 drugFacade 的业务调用。

### `DedupTaskScheduler`（已有类，修改）

**包路径**：`com.aimedical.modules.prescription.service.assist`
**职责**：异步任务去重调度器；补充 PROCESSING 状态的去重检查

**修改点**（3 处 `PENDING` 检查各追加 `PROCESSING`）：

1. 第27行（首次 get 后检查）：
```java
// 修改前：
if (r.getStatus() == AiSuggestionStatus.PENDING
        || (r.getStatus() == AiSuggestionStatus.COMPLETED && !r.isConsumed()))
// 修改后：
if (r.getStatus() == AiSuggestionStatus.PENDING
        || r.getStatus() == AiSuggestionStatus.PROCESSING
        || (r.getStatus() == AiSuggestionStatus.COMPLETED && !r.isConsumed()))
```

2. 第45行（createIfNotExists 后重读检查）：同上追加 `PROCESSING` 条件

3. 第53行（compute lambda 内部）：同上追加 `PROCESSING` 条件

## 错误处理

- **P02**：DrugFacade 注入后，当前任务仅定义+注入字段，不引入 drugFacade 的调用代码。后续使用者负责捕获 druFacade 调用异常并输出 WARN 日志，不阻断主流程。
- **A01**：三种异常（InterruptedException/ExecutionException/TimeoutException）分别构造携带不同错误码的 AiResult，降级路径（第112行 `if (aiResult != null && aiResult.isSuccess())`）可正确感知 `aiResult.isSuccess() == false` 并读取 `aiResult.getErrorCode()` 用于日志。
- **A03**：PROCESSING 中间状态保证 `getSuggestion()` 在异步任务未完成时不会误判为 COMPLETED；TIMEOUT 状态区分异步任务超时与执行失败，使前端可展示不同提示。

## 行为契约

1. **AiSuggestionStatus.A03**：`AiSuggestionStatus.valueOf("PROCESSING")` 和 `AiSuggestionStatus.valueOf("TIMEOUT")` 必须成功。`AiSuggestionStatus.values().length == 5`。
2. **PrescriptionAuditServiceImpl.A01**：audit() 中 AI 调用时段异常不抛至调用方，降级路径的 `log.warn` 现在可读取有意义的 `aiResult.getErrorCode()`（之前为 "null"）。
3. **PrescriptionAssistServiceImpl.A03**：`scheduleSuggestionAsync()` 先设 PROCESSING（表示正在执行）再调用 AI；超时设 TIMEOUT（而非 FAILED），执行异常设 FAILED。
4. **DedupTaskScheduler.PROCESSING**：PROCESSING 状态条目被视为"正在执行中"——不应被重复调度。与 PENDING 语义等价（均表示"任务尚未完成"）。
5. **DrugFacade**：仅注入字段，当前任务不引入业务调用。后续任务调用 drugFacade.findByDrugCode() 时需自行处理 null 返回 + WARN 日志。

## 依赖关系

- **P02**：新增对 `common-module-api` 中 `DrugFacade` 接口的编译期依赖（prescription 模块已依赖 common-module-api，故无需新增 Maven 依赖）
- **A01**：新增对 `ai-api` 中 `AiResultFactory` 类的编译期依赖（prescription 模块已依赖 ai-api，故无需新增 Maven 依赖）
- **A03**：新增值 `PROCESSING` 被 `DedupTaskScheduler.schedule()` 引用；新增值 `TIMEOUT` 被 `PrescriptionAssistServiceImpl.scheduleSuggestionAsync()` 引用
- 测试文件：`PrescriptionAssistServiceImplTest` 需 mock `DrugFacade` 参数；`AiSuggestionStatusTest` 需补充 valueOf 断言

## 测试影响

### `AiSuggestionStatusTest`（需修改）

- `assertEquals(3, AiSuggestionStatus.values().length)` → `assertEquals(5, ...)`
- 追加 `assertEquals(AiSuggestionStatus.PROCESSING, AiSuggestionStatus.valueOf("PROCESSING"))`
- 追加 `assertEquals(AiSuggestionStatus.TIMEOUT, AiSuggestionStatus.valueOf("TIMEOUT"))`

### `DedupTaskSchedulerTest`（需新增用例）

- 构造 PROCESSING 状态的任务条目 → 调用 `schedule()` → 断言返回已有 taskId（不去重调度）
- 构造 TIMEOUT 状态的任务条目 → 调用 `schedule()` → 断言调度新 taskId（TIMEOUT 等同于失败，应允许重新调度）

### `PrescriptionAssistServiceImplTest`（需修改）

- 构造函数调用新增 `DrugFacade` 参数，需 mock `DrugFacade` 实例传入

### `PrescriptionAssistControllerTest`（需修改）

- 验证状态机变更：异步调度后状态为 PROCESSING，超时后为 TIMEOUT

## 修订说明（v4 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] AiSuggestionStatusTest.java 断言在枚举扩充后必然失败 | 补充 AiSuggestionStatusTest.java 修改；变更清单补充此测试文件 |
| [一般] DedupTaskScheduler 未适配 PROCESSING/TIMEOUT 新状态 | 补充 DedupTaskScheduler.schedule() 中 3 处 PENDING 检查追加 PROCESSING 条件 |
| [一般] scheduleSuggestionAsync 中 TimeoutException 未使用 TIMEOUT 状态 | 拆分 catch 块，TimeoutException 分支设为 TIMEOUT |
| [一般] 测试文件修改清单不完整 | 补全测试文件清单：AiSuggestionStatusTest/DedupTaskSchedulerTest/PrescriptionAssistServiceImplTest |
