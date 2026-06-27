# 计划审查报告（v4 r1）

## 审查结果
REJECTED

## 发现

- **[严重]** RateLimitGuard 接口方法签名与任务定义不一致。计划第 53 行描述为 `boolean tryAcquire(String key)`，但 task_v4.md 第 26 行明确要求 `boolean tryAcquire(String key, int limit, long windowMs)`。此错误将导致：
  - InMemoryRateLimitGuard 无法正确实现 RateLimitGuard 接口（编译失败）
  - 后续 GlobalRateLimitFilter 等调用方按参数化签名调用时产生类型不匹配
  - 便捷方法 `boolean tryAcquire(String key)`（task_v4 第 33 行）应是 InMemoryRateLimitGuard 的附加重载，不应是接口的唯一方法

- **[一般]** InMemoryRateLimitGuard 描述中提及 "`ReentrantLock` 保护窗口内排序集合"，与真实设计不一致：
  - task_v4 第 16 行明确 `ReentrantLock` 属于 SlidingWindowCounter（保护跨窗口操作），而非 InMemoryRateLimitGuard
  - InMemoryRateLimitGuard 不持有任何排序集合，线程安全完全由 SlidingWindowCounter 的 `ConcurrentHashMap.compute` 保证
  - 此描述会误导 Designer 在设计 InMemoryRateLimitGuard 时引入不必要的锁

## 修改要求（仅 REJECTED 时）

1. **严重**：将计划第 53 行 RateLimitGuard 方法签名修正为 `boolean tryAcquire(String key, int limit, long windowMs)`，与 task_v4 第 26 行保持一致。InMemoryRateLimitGuard 上下文应说明：实现接口方法（带参数），并提供便捷重载 `boolean tryAcquire(String key)` 使用默认阈值 5 次/10 秒。

2. **一般**：将计划第 54 行 InMemoryRateLimitGuard 中 "`ReentrantLock` 保护窗口内排序集合" 移除；将 `ReentrantLock` 提及移至 SlidingWindowCounter 上下文，说明它保护多 key 窗口清理操作的原子性（与 task_v4 第 16 行一致）。
