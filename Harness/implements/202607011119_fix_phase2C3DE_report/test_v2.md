# 测试报告（v2）

## 验证范围

验证 consultation 模块两个 P0 并发安全修复的行为契约覆盖。

## 行为契约覆盖率

| 行为契约 | 验证方式 | 状态 |
|---------|---------|------|
| **restoreSession 同步**：任意时刻最多一个线程执行 get-check-put 序列 | `shouldProtectRestoreSessionWithSynchronized`（已存在）+ `shouldHandleConcurrentCreateAndRestoreSession`（新增） | 已覆盖 |
| **saveTriageRecord 锁粒度**：同 sessionId 串行，不同 sessionId 互不阻塞 | `shouldHandleConcurrentSaveTriageRecord`（已存在）+ `shouldHandleConcurrentSaveTriageRecordWithDifferentSessionIds`（新增） | 已覆盖 |
| **锁清理**：triageLocks 大小不超过 1001 | `shouldNotLeakLockEntries`（已存在） | 已覆盖 |

## 测试文件变更

| 文件路径 | 操作 | 说明 |
|---------|------|------|
| `DialogueSessionManagerTest.java` | 修改 | 新增 `shouldHandleConcurrentCreateAndRestoreSession`：10 线程交替调用 createSession/restoreSession 验证同步互斥 |
| `TriageServiceImplTest.java` | 修改 | 新增 `shouldHandleConcurrentSaveTriageRecordWithDifferentSessionIds`：10 线程不同 sessionId 并发调用验证互不阻塞 |

## 新增测试详情

### shouldHandleConcurrentCreateAndRestoreSession

- **目的**：验证 `createSession` 与 `restoreSession` 的 synchronized 互斥消除竞态（C20/S04）
- **方式**：10 线程交替调用 createSession 和 restoreSession（各 5 次），TriageRecord 预先设置以便 restoreSession 可从 DB 重建会话
- **断言**：无异常抛出，sessionStore 包含目标 sessionId，所有线程获取的 session 均非 null

### shouldHandleConcurrentSaveTriageRecordWithDifferentSessionIds

- **目的**：验证 per-sessionId 锁不同 sessionId 不互相阻塞（E02 锁粒度）
- **方式**：10 线程使用不同 sessionId 并发调用 triage()
- **断言**：无异常抛出

## 设计偏差说明

无偏差。

## 修订说明

无（首版）。
