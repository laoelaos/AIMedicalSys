# 代码审查报告（v4 r1）

## 审查结果
REJECTED

## 发现

- **[严重]** `SlidingWindowCounter.java:60` — `cleanup()` 方法中使用 `now - 50`（50ms）作为清理过期时间戳的阈值，这会摧毁有效的速率限制状态。例如：用户以 10秒/5次 窗口发出请求，`tryAcquire` 正确维护窗口内数据，但约 60 秒后首次 `cleanup()` 运行时会移除所有 >50ms 的条目，导致该 key 的 Deque 被清空、key 被移除出 `windows` 映射。后续请求将视为新窗口从而绕过限流。**此缺陷完全破坏速率限制的核心正确性。**

## 修改要求（仅 REJECTED 时）

### 严重问题

**文件**: `SlidingWindowCounter.java` 第 54–68 行 (`cleanup()` 方法)

**问题**: `while (!deque.isEmpty() && deque.peekFirst() < now - 50)` 使用固定 50ms 阈值移除条目。`tryAcquire` 已经在 `ConcurrentHashMap.compute` 闭包内按 `windowMs` 正确清理过期条目，`cleanup()` 的唯一设计意图是「移除空 Deque 对应的 key」（见 detail_v4.md "行为契约 → 清理周期"）。当前实现额外移除了有效窗口内的数据。

**期望的修正方向**: 移除 `cleanup()` 方法中移除旧条目的 while 循环，只保留对空 Deque 的 key 清理：
```java
private void cleanup() {
    lock.lock();
    try {
        windows.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    } finally {
        lock.unlock();
    }
}
```
这样 `cleanup()` 仅负责内存回收（移除空 Deque 的 key），不干预仍在窗口期内的计数数据。
