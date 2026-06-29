# 测试报告（v12）

## 概述

为 `SlidingWindowCounter.java` 中 `tryAcquire()` 方法添加的 `lock.lock()` / `lock.unlock()` 保护编写行为契约测试。

## 测试变更

### 新增测试方法（3 个）

| 测试方法 | 验证的契约 | 覆盖维度 |
|---------|-----------|---------|
| `shouldReleaseLockAfterAcquireReturn()` | 正常路径后 lock 始终释放 | 后置条件 |
| `shouldReleaseLockWhenKeyIsNull()` | 异常路径后 lock 始终释放（finally 保证） | 错误路径 + 后置条件 |
| `shouldNotDeadlockUnderConcurrentAcquire()` | 高并发下无死锁 | 状态交互/并发安全 |

### 测试文件

`AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/rateLimit/SlidingWindowCounterTest.java`

### 新增 import

- `java.lang.reflect.Field`（反射读取私有 lock 字段）
- `java.util.concurrent.locks.ReentrantLock`

## 行为契约覆盖矩阵

| 行为契约 | 已有测试 | 新增测试 |
|---------|---------|---------|
| 参数校验（limit/windowMs ≤ 0）→ false | `shouldReturnFalseWhen*` (4个) | — |
| null key → NPE | `shouldThrowNpeWhenKeyIsNull` | `shouldReleaseLockWhenKeyIsNull` |
| 正常路径 → true（limit 内） | `shouldAcquireWithinLimit` | — |
| 超限 → false | `shouldRejectWhenExceedLimit` | — |
| 不同 key 独立计数 | `shouldMaintainIndependentCountersForDifferentKeys` | — |
| 窗口过期后重新允许 | `shouldAllowNewRequestAfterWindowExpiry` | — |
| 同 key 并发（≤ limit 通过） | `shouldHandleConcurrentRequestsForSameKey` | `shouldNotDeadlockUnderConcurrentAcquire` |
| 不同 key 并发（全部通过） | `shouldHandleConcurrentRequestsForDifferentKeys` | — |
| **lock 正常路径后释放** | — | `shouldReleaseLockAfterAcquireReturn` |
| **lock 异常路径后释放** | — | `shouldReleaseLockWhenKeyIsNull` |
| **高并发无死锁** | — | `shouldNotDeadlockUnderConcurrentAcquire` |

## 未覆盖说明

- `cleanup()` 与 `tryAcquire()` 互斥：无法通过公开 API 直接测试，cleanup 为 private 方法且执行间隔 60s，不满足单元测试时效要求。

## 编译验证

```bash
mvn test -Dtest=SlidingWindowCounterTest
```
预期结果：Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
