# 详细设计（v6）

## 概述

修复 4 个模块的 5 项 P1/P2 缺陷，互不依赖，可并行实现。修改范围涉及 8 个源文件和 3 个测试文件。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `prescription/context/PrescriptionDraftContext.java` | 修改 | 新增 `DraftContextCleanupTask` 字段注入，`updateCriticalAlerts()` 中调用 `recordWrite`/`removeTimestamp` |
| `prescription/service/assist/impl/PrescriptionAssistServiceImpl.java` | 修改 | `scheduleSuggestionAsync()` 中 `result` 补充 `setCreateTime(LocalDateTime.now())` |
| `prescription/task/SuggestionCleanupTask.java` | 修改 | `isExpiredAndConsumed()` 重构：FAILED 不要求 consumed，null-safe timestamp 处理 |
| `ai/ai-impl/.../mock/MockAiService.java` | 修改 | `@Profile("mock")` 替换为 `@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "true")` |
| `consultation/event/RegistrationEventListener.java` | 修改 | `recover()` 追加 `@Transactional`；null 防护 `e.getMessage()` 和 `event.getSessionId()` |
| `medical-record/entity/MedicalRecord.java` | 修改 | `visitId` 字段追加 `unique = true` 约束 |
| `medical-record/service/impl/MedicalRecordServiceImpl.java` | 修改 | `save()` try-catch 追加 `DataIntegrityViolationException` 分支 |
| `prescription/context/PrescriptionDraftContextTest.java` | 修改 | 新增 `cleanupTask` mock 字段、构造函数改为 2 参数、`updateCriticalAlerts` 测试追加 verify 断言 |
| `prescription/task/SuggestionCleanupTaskTest.java` | 修改 | 重命名测试、修改 FAILED 测试数据 `consumed=false`、新增未过期 FAILED 不清理测试 |

## 类型定义

### `PrescriptionDraftContext`（已有类，修改）

**形态**：class
**包路径**：`com.aimedical.modules.prescription.context`
**职责**：草稿上下文管理；新增调用 cleanupTask 记录写时间戳，使 TTL 清理可追踪

**现有构造签名变更**（参数计数 1→2）：
```java
// 修改前：
public PrescriptionDraftContext(DraftContextStore draftContextStore)

// 修改后：
public PrescriptionDraftContext(DraftContextStore draftContextStore,
                                 DraftContextCleanupTask cleanupTask)
```

**新增字段**：
```java
private final DraftContextCleanupTask cleanupTask;
```

**新增导入**：
```java
import com.aimedical.modules.prescription.task.DraftContextCleanupTask;
import java.time.Instant;
```

**`updateCriticalAlerts()` 方法修改**（第34-41行）：
- `draftContextStore.put(key, alerts)` 后追加：`cleanupTask.recordWrite(key, Instant.now())`
- `draftContextStore.remove(key)` 后追加：`cleanupTask.removeTimestamp(key)`

**行为契约**：
- 每次 `put` 操作后调用 `recordWrite` 记录写入时间戳，供 `DraftContextCleanupTask.cleanupExpiredDrafts()` 判断过期
- 每次 `remove` 操作后调用 `removeTimestamp` 清除时间戳，避免孤立 timestamp 残留

### `PrescriptionAssistServiceImpl`（已有类，修改）

**包路径**：`com.aimedical.modules.prescription.service.assist.impl`

**`scheduleSuggestionAsync()` 第 355-356 行修改**：
```java
// 修改前：
AiSuggestionResult result = new AiSuggestionResult();
result.setTaskId(taskId);

// 修改后：
AiSuggestionResult result = new AiSuggestionResult();
result.setTaskId(taskId);
result.setCreateTime(LocalDateTime.now());  // ★ 新增
```

**新增导入**：
```java
import java.time.LocalDateTime;
```
（当前源文件 imports 无 `java.time.*`，必须显式追加）

### `SuggestionCleanupTask`（已有类，修改）

**包路径**：`com.aimedical.modules.prescription.task`

**`isExpiredAndConsumed()` 方法重构**（第42-47行 → 新实现）：
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

**行为变化**：
- COMPLETED 条目：要求 `isConsumed() && isExpired`（与原行为一致）
- FAILED 条目：仅要求 `isExpired`，不再要求 `isConsumed()`（修复 FAILED 永不清理缺陷）
- 其他状态（PENDING/PROCESSING/TIMEOUT）：返回 `false`
- null-safe 处理 `entry.getTimestamp()`：防止其他实现返回 null 导致 NPE

### `MockAiService`（已有类，修改）

**包路径**：`com.aimedical.modules.ai.impl.mock`

**注解变更**（第40-41行）：
```java
// 修改前：
@Service
@Profile("mock")

// 修改后：
@Service
@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "true", matchIfMissing = false)
```

**导入变更**：
- 删除 `import org.springframework.context.annotation.Profile`（无其他引用可移除整行）
- 新增 `import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty`

**行为契约**：
- mock 服务不再通过 `@Profile("mock")` 激活，改为通过配置属性 `ai.mock.enabled=true` 控制
- `matchIfMissing = false`：未配置时默认不启用 mock 服务

### `RegistrationEventListener`（已有类，修改）

**包路径**：`com.aimedical.modules.consultation.event`

**修改明细**：

1. **`recover()` 方法添加 `@Transactional` 注解**（第52行）：
   ```java
   @Recover
   @Transactional   // ★ 新增
   public void recover(Exception e, RegistrationEvent event) {
   ```
   `org.springframework.transaction.annotation.Transactional` 已在文件头部 import，无需新增导入。

2. **第60行 `setFailReason` null 防护**：
   ```java
   // 修改前：
   deadLetter.setFailReason(e.getMessage());
   // 修改后：
   deadLetter.setFailReason(e.getMessage() != null ? e.getMessage() : "Unknown failure reason");
   ```
   `DeadLetterEvent.failReason` 标注 `@Column(nullable = false)`，`e.getMessage()` 可能为 null 导致 SQL 约束违反。

3. **第58行 JSON 序列化兜底 null sessionId 防护**：
   ```java
   // 修改前：
   deadLetter.setEventPayload("{\"sessionId\":\"" + event.getSessionId() + "\"}");
   // 修改后：
   String sid = event.getSessionId() != null ? event.getSessionId() : "unknown";
   deadLetter.setEventPayload("{\"sessionId\":\"" + sid + "\"}");
   ```
   `event.getSessionId()` 为 null 时当前输出 `"null"` 字符串，改为输出 `"unknown"`。

### `MedicalRecord`（已有实体，修改）

**包路径**：`com.aimedical.modules.medicalrecord.entity`

**`visitId` 字段约束变更**（第34行）：
```java
// 修改前：
@Column(nullable = false)
private String visitId;

// 修改后：
@Column(nullable = false, unique = true)
private String visitId;
```

**行为契约**：
- 新增 `unique = true` 约束后，同一 `visitId` 的并发 INSERT 会触发 `DataIntegrityViolationException`（唯一约束违反），被 `MedicalRecordServiceImpl` 新增的 catch 分支捕获

### `MedicalRecordServiceImpl`（已有类，修改）

**包路径**：`com.aimedical.modules.medicalrecord.service.impl`

**新增导入**：
```java
import org.springframework.dao.DataIntegrityViolationException;
```

**try-catch 追加 catch 分支**（第114行 `ObjectOptimisticLockingFailureException` 分支之后）：
```java
} catch (DataIntegrityViolationException e) {
    log.warn("Concurrent INSERT conflict on medical record for visitId: {}", visitId, e);
    RecordGenerateResponse response = medicalRecordConverter.toRecordGenerateResponse(aiResult, hints);
    response.setErrorCode(MedicalRecordErrorCode.MR_GEN_CONCURRENT_MODIFICATION);
    response.setFromFallback(visitIdFallback);
    return response;
}
```

**行为契约**：
- 新增的 `DataIntegrityViolationException` catch 分支专用于捕获 INSERT 路径的并发冲突（`unique = true` 约束违反）
- 与原有的 `ObjectOptimisticLockingFailureException` catch 分支（专用于 UPDATE 路径的版本冲突）互补
- 两分支返回相同的 `MR_GEN_CONCURRENT_MODIFICATION` 错误码，调用方无需区分冲突类型

## 错误处理

- **1a (DraftContextCleanupTask)**：无新增错误处理路径。`recordWrite`/`removeTimestamp` 仅操作 `ConcurrentHashMap`，不会抛出受检异常。
- **1b (SuggestionCleanupTask)**：`isExpiredAndConsumed()` 不再抛出 NPE（`entry.getTimestamp()` null-safe 检查）。若 NPE 仍发生在其他方法调用，`cleanupExpiredSuggestions()` 的 `catch (ClassCastException)` 不捕获 NPE——但 null-safe 处理已消除 NPE 触发路径。
- **2 (MockAiService)**：纯配置变更，无运行时错误处理变化。
- **3 (RegistrationEventListener)**：`@Transactional` 确保 `deadLetterEventRepository.save(deadLetter)` 在事务内执行；`e.getMessage()` 和 `event.getSessionId()` null 防护避免 SQL 约束违反。
- **4 (MedicalRecord)**：`DataIntegrityViolationException` catch 分支将并发 INSERT 冲突映射为 `MR_GEN_CONCURRENT_MODIFICATION` 错误码，与 UPDATE 路径的 `ObjectOptimisticLockingFailureException` 行为一致。

## 行为契约

1. **1a 调用顺序**：`PrescriptionDraftContext.updateCriticalAlerts()` 每次修改 `draftContextStore` 后必须调用 `cleanupTask.recordWrite()` 或 `cleanupTask.removeTimestamp()`，确保 `writeTimestamps` 与 `draftContextStore` 内容一致。
2. **1b FAILED 清理**：`SuggestionCleanupTask` 对 FAILED 条目不再要求 `consumed=true`，仅要求 TTL 超期即清理。
3. **1b null-safe**：`isExpiredAndConsumed()` 对 `entry.getTimestamp() == null` 返回 `false`（不过期），不再抛出 NPE。
4. **3 事务**：`recover()` 方法必须在事务内执行，`@Transactional` 注解保证。
5. **4 INSERT 并发保护**：`MedicalRecord.visitId` 的唯一约束 + `DataIntegrityViolationException` catch 构成 INSERT 路径的并发写保护，与 `@Version` 乐观锁的 UPDATE 路径保护互补。

## 依赖关系

- **1a**：`PrescriptionDraftContext` 新增对 `DraftContextCleanupTask`（同模块）、`java.time.Instant` 的编译期依赖。
- **1b**：`PrescriptionAssistServiceImpl` 新增对 `java.time.LocalDateTime` 的编译期依赖。
- **2**：`MockAiService` 新增对 `org.springframework.boot.autoconfigure.condition.ConditionalOnProperty` 的编译期依赖；移除对 `org.springframework.context.annotation.Profile` 的依赖（项目 spring-context 仍引入此包，仅 import 行移除）。
- **3**：无新增依赖（`@Transactional` 已在 import 列表中）。
- **4**：`MedicalRecordServiceImpl` 新增对 `org.springframework.dao.DataIntegrityViolationException` 的编译期依赖。
- `MedicalRecordErrorCode.MR_GEN_CONCURRENT_MODIFICATION` 已在枚举中定义（`MedicalRecordErrorCode.java:10`），无需新增。

## 测试影响

### `PrescriptionDraftContextTest.java`

1. **新增字段和 mock**：
   ```java
   import com.aimedical.modules.prescription.task.DraftContextCleanupTask;
   @Mock private DraftContextCleanupTask cleanupTask;
   ```

2. **构造函数调用修改**（`setUp()` 第25行）：
   ```java
   // 修改前：
   context = new PrescriptionDraftContext(draftContextStore);
   // 修改后：
   context = new PrescriptionDraftContext(draftContextStore, cleanupTask);
   ```

3. **`updateCriticalAlerts` 的 3 个测试新增 verify 断言**：
   - `updateCriticalAlertsShouldPutWhenNonEmpty`：追加 `verify(cleanupTask).recordWrite("rx-001:criticalAlerts", any(Instant.class))`
   - `updateCriticalAlertsShouldRemoveWhenEmpty`：追加 `verify(cleanupTask).removeTimestamp("rx-001:criticalAlerts")`
   - `updateCriticalAlertsShouldRemoveWhenNull`：追加 `verify(cleanupTask).removeTimestamp("rx-001:criticalAlerts")`

### `DraftContextCleanupTaskTest.java`

无需修改。测试中手动调用 `task.recordWrite(key, timestamp)` 注入 timestamp，不受调用方变更影响。

### `SuggestionCleanupTaskTest.java`

1. **`shouldRemoveExpiredFailedAndConsumedEntry` 重命名** → `shouldRemoveExpiredFailedEntryEvenIfNotConsumed`，测试数据 `consumed` 改为 `false`。

2. **新增测试 `shouldNotRemoveFailedEntryWhenNotExpired`**：
   - 创建 FAILED 状态、`consumed=false`、未过期（`Instant.now()`）的 entry
   - 验证 `cleanupExpiredSuggestions()` 后 entry 未被移除
