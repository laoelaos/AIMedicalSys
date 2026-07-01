# 详细设计（v2）

## 概述

修复 consultation 模块两个 P0 并发安全问题：
- **C20/S04**：`DialogueSessionManager.restoreSession` 缺少 `synchronized`，get + put 非原子，并发时可能丢数据
- **E02**：`TriageServiceImpl.saveTriageRecord` 并发首次 INSERT 时 H2 不支持 gap lock → 唯一约束冲突

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/dialogue/DialogueSessionManager.java` | 修改 | `restoreSession` 方法签名加 `synchronized` |
| `AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/service/impl/TriageServiceImpl.java` | 修改 | `saveTriageRecord` 外层按 sessionId 粒度的 `ReentrantLock` 串行化 |
| `AIMedical/backend/modules/consultation/src/test/java/com/aimedical/modules/consultation/DialogueSessionManagerTest.java` | 修改 | 新增 `shouldProtectRestoreSessionWithSynchronized` |
| `AIMedical/backend/modules/consultation/src/test/java/com/aimedical/modules/consultation/TriageServiceImplTest.java` | 修改 | 新增 `shouldHandleConcurrentSaveTriageRecord`、`shouldNotLeakLockEntries` |

## 类型定义

### `DialogueSessionManager`（已有类，修改）

**形态**：class
**包路径**：`com.aimedical.modules.consultation.dialogue`
**职责**：分诊会话管理；为 restoreSession 增加同步保护

**修改的方法**：

```
public synchronized DialogueSession restoreSession(String sessionId)
```

- **修改点**：方法签名添加 `synchronized` 关键字（第50行）
- **理由**：`restoreSession` 内 `sessionStore.get()`（第54行）与 `sessionStore.put()`（第68行）非原子。与 `createSession`（已有 `synchronized`，第33行）保持一致的同步级别。`cancelSession`（第46行）单 put 操作在 `SessionStore` 实现（`ConcurrentHashMap` 或 `HashMap`）下不变动；`evictExpiredSessions`（第73行）已使用 `new ArrayList<>(sessionStore.keySet())` 快照，保持不动。
- **无其他方法变更**：构造器、`cancelSession`、`evictExpiredSessions` 保持不变

### `TriageServiceImpl`（已有类，修改）

**形态**：class
**包路径**：`com.aimedical.modules.consultation.service.impl`
**职责**：分诊服务实现；为 saveTriageRecord 增加 per-sessionId 锁，防止并发唯一约束冲突

**新增导入**：
```
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
```

**新增字段**：
```
private final Map<String, Lock> triageLocks = new ConcurrentHashMap<>();
```

**修改的方法**：`saveTriageRecord(DialogueCreateRequest, DialogueSession, List<RecommendedDepartment>, List<RecommendedDoctor>, AiResult<TriageResponse>, TriageResponse)`

**修改详情（第246-300行）**：
- 方法体整体包裹 per-sessionId `ReentrantLock`：
- 第250-261行 JSON 序列化（可能抛出 `JsonProcessingException`）也在锁内——若 JSON 异常则不必执行事务
- 第265-299行 `transactionTemplate.execute(...)` 在锁内
- 解锁后做内存泄漏防护：`if (triageLocks.size() > 1000) triageLocks.remove(request.getSessionId());`

```
private final Map<String, Lock> triageLocks = new ConcurrentHashMap<>();

private void saveTriageRecord(DialogueCreateRequest request, DialogueSession session,
                                List<RecommendedDepartment> departments, List<RecommendedDoctor> doctors,
                                AiResult<TriageResponse> aiResult,
                                com.aimedical.modules.consultation.dto.TriageResponse response) {
    Lock lock = triageLocks.computeIfAbsent(request.getSessionId(), k -> new ReentrantLock());
    lock.lock();
    try {
        // 第250-299行：原方法的完整逻辑（JSON序列化 + transactionTemplate.execute）
        // ... 与现有代码完全一致，无其他修改
    } finally {
        lock.unlock();
        if (triageLocks.size() > 1000) {
            triageLocks.remove(request.getSessionId());
        }
    }
}
```

**不修改的内容**：
- 构造器不变（`doctorFacadeTimeout` 等参数已在 v1 增加）
- `triage()` 方法不变
- `selectDepartment()` 方法不变（其 `findBySessionId` + `save` 在同一 `@Transactional` 方法内，后续可能需考虑并发，但不属于本任务范围）
- `findDoctorsForDepartments()` 方法不变

## 错误处理

- **E02 场景**：并发首次 INSERT 时唯一约束冲突 → per-sessionId 锁保证同一 sessionId 的 `saveTriageRecord` 串行执行，第一个线程 INSERT 后第二个线程 `findBySessionId` 必能找到记录 → UPDATE 而非 INSERT
- **锁异常**：`lock.lock()` 不可中断（`ReentrantLock` 默认），但 `transactionTemplate.execute` 内已有 `RuntimeException` 传播机制，外层 try-finally 保证 unlock
- **内存泄漏**：`triageLocks.size() > 1000` 时清理当前条目，保留其他高频 sessionId 的锁缓存

## 行为契约

- **restoreSession 同步**：任意时刻最多一个线程执行 `restoreSession` 的 get-check-put 序列，消除 `createSession` 与 `restoreSession` 间的竞态
- **saveTriageRecord 锁粒度**：同一 sessionId 的 `saveTriageRecord` 串行执行；不同 sessionId 的调用互不阻塞
- **锁清理**：`triageLocks` 大小不超过 1001（触发清理阈值为 >1000）；偶尔的锁回收后下次同 sessionId 调用会重新创建 `ReentrantLock`，不影响正确性

## 依赖关系

- **新增依赖**：`java.util.concurrent.locks.Lock`、`java.util.concurrent.locks.ReentrantLock`、`java.util.concurrent.ConcurrentHashMap`、`java.util.Map`（均为 JDK 内置，无需外部依赖）
- **已有依赖不变**：`SessionStore`、`TriageRecordRepository`、`TransactionTemplate` 等
- **暴露接口**：无变化（`TriageService` 接口不变）

## 修订说明（v2 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| `DialogueSessionManagerTest.java` 路径/状态与实际不符：路径包含 `dialogue/` 子目录但实际文件在 `consultation/` 下，且文件已存在（369行），不应标注「已有」 | 修正文件路径为 `.../consultation/DialogueSessionManagerTest.java`，操作类型改为「修改」（向现有文件追加测试方法） |
