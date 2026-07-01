# 任务指令（v6）

## 动作
NEW

## 任务描述

修复 4 个模块的 5 项缺陷，均为 P1/P2 级别，互不依赖，可并行实现。

### 1. P03/S02: TTL 清理任务失效（prescription 模块）

两个清理任务均因设计缺陷而失效。

#### 1a. DraftContextCleanupTask — recordWrite() 无调用方

**问题**：`DraftContextCleanupTask.java:27` 的 `recordWrite()` 方法无任何调用方，`writeTimestamps` 始终为空，`cleanupExpiredDrafts()` 遍历 `draftContextStore.keySet()` 时 `writeTimestamps.get(key)` 始终返回 null，清理条件永不满足。

**修改**：
- 文件：`prescription/context/PrescriptionDraftContext.java`
  - 新增 `DraftContextCleanupTask` 字段注入
  - `updateCriticalAlerts()` 中：在 `draftContextStore.put(key, alerts)` 后调用 `cleanupTask.recordWrite(key, Instant.now())`
  - `updateCriticalAlerts()` 中：在 `draftContextStore.remove(key)` 后调用 `cleanupTask.removeTimestamp(key)`
  - 新增 import: `com.aimedical.modules.prescription.task.DraftContextCleanupTask`, `java.time.Instant`

**测试文件修改**：

1. `prescription/context/PrescriptionDraftContextTest.java`：
   - 新增 `@Mock DraftContextCleanupTask cleanupTask` 字段（import: `com.aimedical.modules.prescription.task.DraftContextCleanupTask`）
   - `setUp()` 中构造函数调用改为 `new PrescriptionDraftContext(draftContextStore, cleanupTask)`
   - `updateCriticalAlerts` 的 3 个测试追加 `verify(cleanupTask).recordWrite(...)` / `verify(cleanupTask).removeTimestamp(...)` 断言

2. `prescription/task/DraftContextCleanupTaskTest.java`（无需修改，因测试中手动调用 recordWrite 注入 timestamp）

#### 1b. SuggestionCleanupTask — NPE + FAILED 永不清理

**问题 1**（NPE）：`PrescriptionAssistServiceImpl.scheduleSuggestionAsync()` 第 355 行创建的 `AiSuggestionResult result` 未设置 `createTime`（仅设置 taskId、status），第 384 行 `suggestionStore.put(taskId, result)` 将无 createTime 的 result 存入 store。`SuggestionCleanupTask.isExpiredAndConsumed()` 第 46 行 `entry.getTimestamp().plusSeconds(...)` 对 null 调用方法，抛出 NPE。该 NPE 未被 `catch (ClassCastException)` 捕获（NPE ≠ ClassCastException），导致 `cleanupExpiredSuggestions()` 方法异常终止，后续条目不被处理。

**问题 2**（FAILED 永不清理）：`isExpiredAndConsumed()` 第 44 行要求 `isCompletedOrFailed && entry.isConsumed()`，但 FAILED 条目的 `consumed` 默认 false，且仅 `getSuggestion()`（第 229 行）对 COMPLETED 状态设 `consumed=true`。FAILED 条目永不被清理。

**修改明细**：

1. 文件：`prescription/service/assist/impl/PrescriptionAssistServiceImpl.java`
   - `scheduleSuggestionAsync()` 第 356 行 `result.setTaskId(taskId)` 后追加 `result.setCreateTime(LocalDateTime.now())`
   - 新增 `import java.time.LocalDateTime`（当前源文件 import 列表无此条目，必须显式追加）

2. 文件：`prescription/task/SuggestionCleanupTask.java`
   - `isExpiredAndConsumed()` 中 FAILED 条目不要求 consumed：
     ```java
     private boolean isExpiredAndConsumed(SuggestionStoreEntry entry, Instant now) {
         String status = entry.getStatusName();
         boolean isCompleted = "COMPLETED".equals(status);
         boolean isFailed = "FAILED".equals(status);
         boolean isExpired = entry.getTimestamp() != null
                 && entry.getTimestamp().plusSeconds(TTL_MINUTES * 60).isBefore(now);
         if (isCompleted) {
             return entry.isConsumed() && isExpired;
         }
         if (isFailed) {
             return isExpired;  // FAILED 条目不要求 consumed
         }
         return false;
     }
     ```
   - 删除第 42-47 行原实现，替换为上述逻辑
   - 注意 null-safe 处理 `entry.getTimestamp()`（防止其他实现返回 null 导致 NPE）

3. 文件：`prescription/task/SuggestionCleanupTaskTest.java`
   - `shouldRemoveExpiredFailedAndConsumedEntry()` — 当前测试名与问题 2 不符（FAILED 现无需 consumed），重命名为 `shouldRemoveExpiredFailedEntryEvenIfNotConsumed`，修改测试数据 `consumed=false`
   - 新增测试：`shouldNotRemoveFailedEntryWhenNotExpired`

### 2. A05: MockAiService 配置不一致（ai-impl 模块）

**问题**：`MockAiService.java:41` 使用 `@Profile("mock")`，与 OOD 要求的 `@ConditionalOnProperty` 不一致。需通过配置属性开关而非 profile 来控制 mock 服务的启用。

**修改**：
- 文件：`ai/ai-impl/.../mock/MockAiService.java`
  - 删除 `import org.springframework.context.annotation.Profile`（无其他引用可移除整行）
  - 新增 `import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty`
  - 第 40-41 行：
    ```java
    @Service
    @ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "true", matchIfMissing = false)
    ```
  - 删除 `@Profile("mock")` 注解

### 3. T40/E05: @Recover 方法缺陷（consultation 模块）

**问题 1**：`RegistrationEventListener.recover()` 第 52-66 行无 `@Transactional` 注解，`deadLetterEventRepository.save(deadLetter)` 无事务保护。

**问题 2**：第 60 行 `e.getMessage()` 可能为 null，违反 `DeadLetterEvent.failReason` 的 `@Column(nullable = false)` 约束。

**问题 3**：第 58 行 JSON 序列化失败兜底 `"{\"sessionId\":\"" + event.getSessionId() + "\"}"`，当 `event.getSessionId()` 为 null 时输出 `"null"` 字符串。

**修改**：
- 文件：`consultation/event/RegistrationEventListener.java`
  1. 第 52 行 `recover` 方法添加 `@Transactional` 注解（import 已存在 `org.springframework.transaction.annotation.Transactional`）
  2. 第 60 行 `setFailReason` null 防护：
     ```java
     deadLetter.setFailReason(e.getMessage() != null ? e.getMessage() : "Unknown failure reason");
     ```
  3. 第 58 行 JSON 兜底 null sessionId 防护：
     ```java
     String sid = event.getSessionId() != null ? event.getSessionId() : "unknown";
     deadLetter.setEventPayload("{\"sessionId\":\"" + sid + "\"}");
     ```

### 4. M04: 乐观锁不可触发（medical-record 模块）

**问题**：`MedicalRecordServiceImpl.java:102-103` 使用 `findByVisitId(visitId).orElseGet(MedicalRecord::new)`，当记录不存在时创建无 ID/version 的新实体，`save()` 执行 INSERT。`@Version` 乐观锁仅在 UPDATE 路径生效，INSERT 路径不触发 `ObjectOptimisticLockingFailureException`。

**修改**：

1. 文件：`medical-record/entity/MedicalRecord.java`
   - 第 34 行 `visitId` 字段添加 `unique = true`：
     ```java
     @Column(nullable = false, unique = true)
     private String visitId;
     ```

2. 文件：`medical-record/service/impl/MedicalRecordServiceImpl.java`
   - 新增 import: `org.springframework.dao.DataIntegrityViolationException`
   - `save(entity)` 的 try-catch 块追加 catch 分支（第 114 行现有 `catch (ObjectOptimisticLockingFailureException e)` 之后）：
     ```java
     } catch (DataIntegrityViolationException e) {
         log.warn("Concurrent INSERT conflict on medical record for visitId: {}", visitId, e);
         RecordGenerateResponse response = medicalRecordConverter.toRecordGenerateResponse(aiResult, hints);
         response.setErrorCode(MedicalRecordErrorCode.MR_GEN_CONCURRENT_MODIFICATION);
         response.setFromFallback(visitIdFallback);
         return response;
     }
     ```

## 选择理由

R5 已完成 P01+S03（prescription 异步 AI 调度 + 跨 key 竞态）修复并验证通过（1604 用例，0 失败）。按计划推进任务 6（路线表第 6 项），涵盖 prescription/ai-impl/consultation/medical-record 四模块剩余 P1/P2 缺陷。各缺陷修改范围独立且互不依赖，可并行实现，预计工作量 5 人时。

## 任务上下文

### P03/S02 — 相关文件源码关键行

**DraftContextCleanupTask.java:27-29** — recordWrite() 定义但无外部调用：
```java
public void recordWrite(String key, Instant timestamp) {
    writeTimestamps.put(key, timestamp);
}
```

**PrescriptionDraftContext.java:34-41** — updateCriticalAlerts() 唯一修改 draftContextStore 的位置：
```java
public void updateCriticalAlerts(String prescriptionId, List<DosageAlert> alerts) {
    String key = prescriptionId + CRITICAL_ALERTS_SUFFIX;
    if (alerts == null || alerts.isEmpty()) {
        draftContextStore.remove(key);
    } else {
        draftContextStore.put(key, alerts);
    }
}
```

**PrescriptionAssistServiceImpl.java:350-356** — scheduleSuggestionAsync 创建 result 但不设 createTime：
```java
AiSuggestionResult result = new AiSuggestionResult();
result.setTaskId(taskId);
// ★ 缺少 result.setCreateTime(LocalDateTime.now());
```

**SuggestionCleanupTask.java:42-47** — isExpiredAndConsumed 要求 FAILED + consumed（但 FAILED 永不被置 consumed）：
```java
private boolean isExpiredAndConsumed(SuggestionStoreEntry entry, Instant now) {
    String status = entry.getStatusName();
    boolean isCompletedOrFailed = "COMPLETED".equals(status) || "FAILED".equals(status);
    return isCompletedOrFailed && entry.isConsumed()
            && entry.getTimestamp().plusSeconds(TTL_MINUTES * 60).isBefore(now);
}
```

### A05 — 相关文件

**MockAiService.java:40-42** — 当前注解：
```java
@Service
@Profile("mock")
```

### T40/E05 — 相关文件

**RegistrationEventListener.java:52-66** — recover 方法：
```java
@Recover
public void recover(Exception e, RegistrationEvent event) {
    DeadLetterEvent deadLetter = new DeadLetterEvent();
    try {
        deadLetter.setEventPayload(objectMapper.writeValueAsString(event));
    } catch (JsonProcessingException ex) {
        deadLetter.setEventPayload("{\"sessionId\":\"" + event.getSessionId() + "\"}");
    }
    deadLetter.setFailReason(e.getMessage());           // null 风险
    deadLetter.setFailTime(LocalDateTime.now());
    deadLetter.setState("FAILED");
    deadLetter.setRetryCount(0);
    deadLetter.setMaxRetryCount(3);
    deadLetterEventRepository.save(deadLetter);          // 无事务
}
```

### M04 — 相关文件

**MedicalRecord.java:33-35** — visitId 当前无 unique 约束：
```java
@Column(nullable = false)
private String visitId;
```

**MedicalRecordServiceImpl.java:102-119** — INSERT 路径无乐观锁保护（仅 catch ObjectOptimisticLockingFailureException）：
```java
MedicalRecord entity = medicalRecordRepository.findByVisitId(visitId)
        .orElseGet(MedicalRecord::new);
// ... set fields ...
try {
    medicalRecordRepository.save(entity);
} catch (ObjectOptimisticLockingFailureException e) {
    // 仅在 UPDATE 路径触发，INSERT 路径不可达
    ...
}
```

## 已有代码上下文

以上所有文件的完整源码已在 plan agent 开发环境中读取，见当前 workspace 对应路径。

## 修订说明（v6 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] R6 NEW 节缺少实施细节，与 R5 NEW 质量标准严重背离 | plan.md R6 NEW 节已按 R5 NEW 质量标准扩充：为每个缺陷补充问题定位（精确到行号）、变更明细（具体代码改动）、文件清单、测试修改说明 |
| [一般] 缺少 DraftContextCleanupTask 修改方案 | plan.md 新增 1a 子节：PrescriptionDraftContext 新增 cleanupTask 字段注入 + updateCriticalAlerts 中调用 recordWrite/removeTimestamp |
| [一般] 缺少 SuggestionCleanupTaskTest 修改方案 | plan.md 1b 子节补充测试修改说明 |
| [一般] 缺少 T40/E05 null sessionId 防护细节 | plan.md 3 子节补充代码细节 |
| [一般] 缺少 M04 完整修改方案 | plan.md 4 子节补充完整代码 |
| [轻微] 文件路径使用 `...` 通配符 | plan.md 涉及文件汇总表使用固定前缀结构，MockAiService 路径修正为完整路径 |

## 修订说明（v6 r2）
| 审查意见 | 修改措施 |
|---------|---------|
| [一般] PrescriptionDraftContext 构造函数变更导致 PrescriptionDraftContextTest 编译失败 | 1a 节补充 PrescriptionDraftContextTest.java 测试修改清单：新增 cleanupTask mock 字段、构造函数改为 2 参数、updateCriticalAlerts 测试追加 verify 断言 |
| [轻微] 缺少 import java.time.LocalDateTime 的明确标注 | 1b 节第 37 行修改为「新增 import java.time.LocalDateTime（当前源文件 import 列表无此条目，必须显式追加）」 |
