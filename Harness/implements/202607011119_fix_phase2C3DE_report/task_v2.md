# 任务指令（v2）

## 动作
NEW

## 任务描述

**任务 2：consultation 模块并发安全修复（C20/S04 + E02）**

### 子任务 2a：DialogueSessionManager 并发安全（C20/S04）

**文件**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/dialogue/DialogueSessionManager.java`

**修复点：**
1. `createSession`（第33行）：已有 `synchronized` 保护，containsKey+put 在同步块内原子。**保持不动**，无需修改。
2. `restoreSession`（第50行）：**方法签名加 `synchronized`**，与 createSession 保持一致的同步级别，消除并发 restoreSession 间的竞态（第54行 get + 第68行 put 非原子）。
3. 对 `cancelSession`（第46行）和 `evictExpiredSessions`（第73行）评估是否需要 synchronized：cancelSession 单 put 操作在 ConcurrentHashMap 下安全；evictExpiredSessions 循环迭代期间其他线程可能修改 keySet（需用 `new ArrayList<>(keySet())` 快照，已实现），保持不动。

### 子任务 2b：TriageRecord 并发 INSERT 唯一约束冲突（E02）

**文件**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/service/impl/TriageServiceImpl.java`

**方案：saveTriageRecord 方法内按 sessionId 串行化**

1. 新增导入：`java.util.concurrent.locks.Lock`、`java.util.concurrent.locks.ReentrantLock`、`java.util.concurrent.ConcurrentHashMap`
2. 新增字段：`private final ConcurrentHashMap<String, Lock> triageLocks = new ConcurrentHashMap<>();`
3. **修改 saveTriageRecord**（第246-300行）：在 `transactionTemplate.execute(...)` 外层包装按 sessionId 粒度的 `ReentrantLock`：

```java
private final Map<String, Lock> triageLocks = new ConcurrentHashMap<>();

private void saveTriageRecord(...) {
    Lock lock = triageLocks.computeIfAbsent(request.getSessionId(), k -> new ReentrantLock());
    lock.lock();
    try {
        // ... 现有方法的完整逻辑（JSON 序列化 + transactionTemplate.execute）
    } finally {
        lock.unlock();
        // 清理锁条目，防止内存泄漏（保留最近 N 个以处理高频 sessionId 复用场景）
        if (triageLocks.size() > 1000) {
            triageLocks.remove(request.getSessionId());
        }
    }
}
```

4. 注意：lock 必须在 `transactionTemplate.execute` 之外（外层包装），以保证从 `findBySessionId`（PESSIMISTIC_WRITE + MANDATORY）到 `triageRecordRepository.save()` 的完整事务路径在同一线程串行化。JSON 序列化（可能抛出 JsonProcessingException）也应在锁内——若 JSON 异常则不必执行事务。

### 子任务 2c：测试

**文件**：
- `AIMedical/backend/modules/consultation/src/test/java/com/aimedical/modules/consultation/TriageServiceImplTest.java`
- 新增：`AIMedical/backend/modules/consultation/src/test/java/com/aimedical/modules/consultation/dialogue/DialogueSessionManagerTest.java`

**新增测试用例：**
1. `DialogueSessionManagerTest.shouldProtectRestoreSessionWithSynchronized` — 验证 restoreSession 同步性（并发 restore + createSession 不丢数据）
2. `TriageServiceImplTest.shouldHandleConcurrentSaveTriageRecord` — 验证首次分诊并发 INSERT 不违反唯一约束
3. `TriageServiceImplTest.shouldNotLeakLockEntries` — 验证锁表大小不超阈值

**已有测试修正：** 无需修正（R1 已验证通过 52 用例）。

### 验证标准
- 编译通过：`mvn compile -pl modules/consultation -am -q`
- 测试通过：全部现有测试 + 新增用例 0 失败
- 手动检查：DialogueSessionManager 并发场景下 createSession/restoreSession 不丢数据；saveTriageRecord 并发首次 INSERT 不违反唯一约束

## 选择理由

R1（C06 DoctorFacade 超时控制）已 PASSED，证明了 consultation 模块的修改流程（编译 + 测试）。R2 解决 consultation 模块剩余的两个 P0 并发安全问题：
- C20/S04：session 管理并发竞争，影响首次分诊路径数据完整性
- E02：唯一约束冲突，影响二次分诊路径可用性

两者均属同模块、同主题（并发安全），共享测试基础设施（TriageServiceImplTest 已建立），合并实施可减少上下文切换成本。

## 任务上下文

来自诊断报告：
- C20/S04：createSession 已 synchronized 但 restoreSession 未同步；restoreSession 的 get + put 非原子
- E02：saveTriageRecord 中 findBySessionId 使用 PESSIMISTIC_WRITE，但 H2 数据库不支持 gap lock，首次 INSERT 时并发两线程均找不到记录 → 唯一约束冲突

## 已有代码上下文

- `DialogueSessionManager.java`（84行）：createSession synchronized + containsKey/put；restoreSession 无 synchronized
- `TriageRecord.java`：`@Column(nullable = false, unique = true)` sessionId
- `TriageRecordRepository.java`：`findBySessionId` — `@Lock(PESSIMISTIC_WRITE) @Transactional(propagation = MANDATORY)`
- `TriageServiceImpl.java`：`saveTriageRecord`（第246-299行）— transactionTemplate.execute 内调用 findBySessionId + save
- `TriageServiceImplTest.java`：R1 已通过 52 用例，建立了反射注入 doctorFacadeTimeout 的模式
- `SessionStore` 接口：`get/put/remove/containsKey/keySet`，无 `putIfAbsent`
