# 详细设计（v12）

## 概述

修复 `SlidingWindowCounter.java` 中 `ReentrantLock` 作用域不完整问题（T14）：在 `tryAcquire()` 方法内添加 `lock.lock()` / `lock.unlock()` 保护，包裹 `windows.compute()` 调用，使锁策略与 OOD 4.1 节设计约定一致。

功能行为无变化（`ConcurrentHashMap.compute()` 已提供原子性保证），仅对准实现与设计约定的偏差。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/rateLimit/SlidingWindowCounter.java` | 修改 | `tryAcquire()` 内使用 `lock` 包裹 `windows.compute()` |

## 类型定义

无新增类型。`ReentrantLock` 字段 `lock` 已存在（L14），构造器中已初始化（L19）。

## 变更详述

### `tryAcquire()` 方法锁保护

**修改位置**：`SlidingWindowCounter.java:35-50`

**修改方式**：在 `windows.compute(key, ...)` 调用前加 `lock.lock()`，用 `try { ... } finally { lock.unlock(); }` 包裹 `compute()` 调用。

**修改后代码结构**：

```java
public boolean tryAcquire(String key, int limit, long windowMs) {
    if (limit <= 0 || windowMs <= 0) {
        return false;
    }
    long now = System.currentTimeMillis();
    long threshold = now - windowMs;

    boolean[] result = new boolean[1];
    lock.lock();
    try {
        windows.compute(key, (k, deque) -> {
            if (deque == null) {
                deque = new ArrayDeque<>();
            }
            while (!deque.isEmpty() && deque.peekFirst() < threshold) {
                deque.pollFirst();
            }
            if (deque.size() < limit) {
                deque.addLast(now);
                result[0] = true;
            } else {
                result[0] = false;
            }
            return deque;
        });
    } finally {
        lock.unlock();
    }
    return result[0];
}
```

**不涉及 import 变更**：`java.util.concurrent.locks.ReentrantLock` 已导入（L9）。

## 错误处理

不涉及。`lock.lock()` / `lock.unlock()` 在 `finally` 块中确保释放，不抛出受检异常。`ConcurrentHashMap.compute()` 的 `ConcurrentModificationException` 等未受检异常由 JVM 正常传播。

## 行为契约

### `tryAcquire(String key, int limit, long windowMs)`

**前置**：
- `key` 非 null（由 `ConcurrentHashMap` 约束）
- `limit > 0` 且 `windowMs > 0`（方法体头部守卫）

**正常路径**：
1. 参数校验 → 失败返回 `false`
2. 获取 `lock`（阻塞直到锁可用）
3. `windows.compute(key, ...)` 执行窗口滑动与限流判断
4. `finally` 块释放 `lock`
5. 返回 `result[0]`

**后置**：
- `lock` 在退出时始终释放（`finally` 保证）
- 业务语义不变：返回 `true` 表示请求被允许，`false` 表示被限流
- 与 `cleanup()` 方法共享同一 `lock` 实例，确保 `removeIf` 与 `compute` 互斥

## 依赖关系

| 依赖 | 说明 |
|------|------|
| `java.util.concurrent.locks.ReentrantLock` | 已导入（L9），无变更 |
| `java.util.concurrent.ConcurrentHashMap.compute()` | 已使用，无变更 |

## 测试

已有 `SlidingWindowCounterTest.java`，修改后运行确认全部测试通过。当前预期：无测试需要新增或修改，因为功能行为未变。
