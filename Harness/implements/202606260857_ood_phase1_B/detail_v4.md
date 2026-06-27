# 详细设计（v4）

## 概述

实现 Stage 2 速率限制基础设施：新建 3 个类型（SlidingWindowCounter、RateLimitGuard、InMemoryRateLimitGuard）及对应单元测试，位于公共模块的 `auth.rateLimit` 子包下。

## 文件规划

| 文件路径（相对 `AIMedical/backend/`） | 操作 | 职责 |
|---------|------|------|
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/rateLimit/SlidingWindowCounter.java` | 新建 | 滑动窗口计数器工具类 |
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/rateLimit/RateLimitGuard.java` | 新建 | 速率限制策略接口 |
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/rateLimit/InMemoryRateLimitGuard.java` | 新建 | 内存限流实现 |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/rateLimit/InMemoryRateLimitGuardTest.java` | 新建 | 限流单元测试 |

## 类型定义

### SlidingWindowCounter

**形态**：public class（工具类，非 Spring Bean）
**包路径**：`com.aimedical.modules.commonmodule.auth.rateLimit`
**职责**：滑动窗口计数器工具类，跨包复用（InMemoryRateLimitGuard + GlobalRateLimitFilter）

```java
public class SlidingWindowCounter {
    private final ConcurrentHashMap<String, Deque<Long>> windows;
    private final ReentrantLock lock;
    private final ScheduledExecutorService cleanupExecutor;
}
```

**公开接口**：
```java
public SlidingWindowCounter()
public boolean tryAcquire(String key, int limit, long windowMs)
```

**构造方式**：`new SlidingWindowCounter()` — 默认创建空的 `ConcurrentHashMap`、`ReentrantLock`、单线程 `ScheduledExecutorService`（daemon 线程，60 秒初始延迟 + 60 秒间隔清理）

**类型关系**：独立工具类，无继承/实现

**关键实现约束**：
- `tryAcquire` 内部使用 `ConcurrentHashMap.compute(key, (k, deque) -> { ... })` 实现原子性
- `compute` 闭包内：移除 `Deque` 中超出窗口（`now - windowMs`）的旧时间戳；若队列长度 < limit 则添加 `now` 并返回 true；否则返回 false
- `ReentrantLock` 保护 `cleanup()` 方法的全局遍历（遍历 `windows.entrySet()`、移除空 Deque 的 key）
- 清理精度 ±50ms：`Deque.peekFirst()` 比较时使用 `now - windowMs` 作阈值
- `cleanup()` 由 `ScheduledExecutorService.scheduleWithFixedDelay` 每 60 秒执行

### RateLimitGuard

**形态**：public interface
**包路径**：`com.aimedical.modules.commonmodule.auth.rateLimit`
**职责**：速率限制策略契约，支持 IP 级别的限流判定

```java
public interface RateLimitGuard {
    boolean tryAcquire(String key, int limit, long windowMs);
}
```

**公开接口**：
| 方法 | 参数 | 返回 | 说明 |
|------|------|------|------|
| `tryAcquire` | `String key` — 限流 key（IP 地址）<br>`int limit` — 窗口内最大次数<br>`long windowMs` — 窗口时间（毫秒） | `boolean` | true=放行，false=拒绝 |

### InMemoryRateLimitGuard

**形态**：public class（implements RateLimitGuard）
**包路径**：`com.aimedical.modules.commonmodule.auth.rateLimit`
**职责**：内存限流实现，默认阈值 5 次 / 10 秒 / IP

```java
public class InMemoryRateLimitGuard implements RateLimitGuard {
    private static final int DEFAULT_LIMIT = 5;
    private static final long DEFAULT_WINDOW_MS = 10_000L;

    private final SlidingWindowCounter counter;
    private final int defaultLimit;
    private final long defaultWindowMs;
}
```

**公开接口**：
```java
public InMemoryRateLimitGuard()
public InMemoryRateLimitGuard(SlidingWindowCounter counter)
public InMemoryRateLimitGuard(SlidingWindowCounter counter, int defaultLimit, long defaultWindowMs)
public boolean tryAcquire(String key)
public boolean tryAcquire(String key, int limit, long windowMs)
```

**构造方式**：
- 无参构造：创建新的 `SlidingWindowCounter` 实例，使用默认限流参数（5/10s）
- `counter` 注入构造：允许外部传入共享的 `SlidingWindowCounter` 实例（如 GlobalRateLimitFilter 使用独立计数器时不共享）
- 全参构造：允许自定义默认 limit 和 windowMs

**方法说明**：
- `tryAcquire(String key)` — 使用 `DEFAULT_LIMIT`（5）和 `DEFAULT_WINDOW_MS`（10_000L）调用 `counter.tryAcquire(key, defaultLimit, defaultWindowMs)`
- `tryAcquire(String key, int limit, long windowMs)` — 委托给 `counter.tryAcquire(key, limit, windowMs)`

**行为契约**：
- 超出限制时，由调用方（AuthServiceImpl / Controller）根据返回 false 抛出 `BusinessException(GlobalErrorCode.RATE_LIMITED)`
- InMemoryRateLimitGuard 自身不抛出 BusinessException，仅返回 boolean 值

## 错误处理

| 类型 | 错误场景 | 处理方式 |
|------|---------|---------|
| SlidingWindowCounter | `limit <= 0` | `tryAcquire` 直接返回 false |
| SlidingWindowCounter | `windowMs <= 0` | `tryAcquire` 直接返回 false |
| SlidingWindowCounter | `key` 为 null | `ConcurrentHashMap.compute` 抛出 NPE，自然传播 |
| InMemoryRateLimitGuard | 超限 | 返回 false，由调用方决定是否抛出 `BusinessException(GlobalErrorCode.RATE_LIMITED, ...)` |
| InMemoryRateLimitGuard | `SlidingWindowCounter` 构造注入为 null | `tryAcquire` 中 NPE 自然传播 |

## 行为契约

### SlidingWindowCounter
- **前置条件**：`tryAcquire` 的 `limit > 0`、`windowMs > 0`，否则返回 false
- **后置条件**：返回 true 表示当前窗口内的请求计数已递增；返回 false 表示已达上限，计数不变
- **不变量**：每个 key 的 Deque 按时间戳升序排列；Deque 中元素数量 <= limit
- **并发安全**：单条 key 的读写由 `ConcurrentHashMap.compute` 保证原子性；全局清理由 `ReentrantLock` 保护
- **清理周期**：`ScheduledExecutorService` 每 60 秒执行一次 `cleanup()`，移除空 Deque 对应的 key；cleanup 不保证每次恰好清理完毕，精度 ±50ms

### RateLimitGuard
- **前置条件**：无
- **后置条件**：返回 true 表示当前允许请求；返回 false 表示超出速率限制

### InMemoryRateLimitGuard
- **前置条件**：`tryAcquire(String key)` 的 key 不能为 null
- **后置条件**：同 RateLimitGuard 契约；第 6 次及之后在 10 秒窗口内调用返回 false
- **窗口重置**：停止请求 10 秒后，窗口内计数归零，下个请求返回 true

## 依赖关系

### 新增依赖
- `java.util.concurrent.ConcurrentHashMap` — SlidingWindowCounter 数据结构
- `java.util.Deque` / `ArrayDeque` — SlidingWindowCounter 滑动窗口容器
- `java.util.concurrent.locks.ReentrantLock` — SlidingWindowCounter 全局清理锁
- `java.util.concurrent.ScheduledExecutorService` / `Executors` / `TimeUnit` — SlidingWindowCounter 过期清理

### 已有依赖（不变）
- `com.aimedical.common.exception.GlobalErrorCode` — InMemoryRateLimitGuard 的调用方使用其中的 `RATE_LIMITED`
- `com.aimedical.common.exception.BusinessException` — 调用方（AuthServiceImpl）用于抛出限流异常

### 暴露给后续任务的公开接口
- `SlidingWindowCounter.tryAcquire(key, limit, windowMs)` — 供 GlobalRateLimitFilter（后续任务 2.2）独立实例使用
- `RateLimitGuard.tryAcquire(key, limit, windowMs)` — 供 AuthServiceImpl（Stage 3）编程式调用
- `InMemoryRateLimitGuard.tryAcquire(key)` — 供 AuthServiceImpl 使用默认限流参数的便捷方法
- `InMemoryRateLimitGuard.tryAcquire(key, limit, windowMs)` — 供需要自定义参数的调用方使用

### 不在此范围
- GlobalRateLimitFilter（后续任务 2.2）
- AuthServiceImpl 集成限流调用（Stage 3）
- Redis 分布式限流实现
- 限流参数的配置化（当前硬编码为常量）

## 单元测试设计

### InMemoryRateLimitGuardTest

**形态**：class（JUnit 5）
**包路径**：`com.aimedical.modules.commonmodule.auth.rateLimit.InMemoryRateLimitGuardTest`

**测试方法**：

| 用例 | 名称 | 覆盖维度 |
|------|------|---------|
| 正常路径 | `shouldAllowUpToLimit` | 5 次以内请求均返回 true |
| 限流触发 | `shouldRejectWhenExceedLimit` | 第 6 次请求返回 false |
| 窗口重置 | `shouldAllowAfterWindowExpiry` | 等待 >10 秒后恢复 true |
| 并发安全 | `shouldHandleConcurrentRequests` | 多线程并发请求同一 key，总放行数 <= limit |
| 默认参数 | `shouldUseDefaultLimitAndWindow` | 无参 `tryAcquire(key)` 使用 5/10s 默认值 |
| 自定义参数 | `shouldRespectCustomLimitAndWindow` | 全参构造 + 自定义参数 |

**测试策略**：
- 使用 `CountDownLatch` + `CyclicBarrier` 测试并发场景
- 窗口重置用例使用 `Thread.sleep(10_100)` 确保完全超出 10 秒窗口
- 不 mock `SlidingWindowCounter`，直接构造真实实例验证端到端行为
- 测试使用 `@Test` 标注，不引入 Spring 上下文

**并发测试设计**：
```java
void shouldHandleConcurrentRequests() throws InterruptedException {
    int threadCount = 10;
    int limit = 5;
    InMemoryRateLimitGuard guard = new InMemoryRateLimitGuard(
        new SlidingWindowCounter(), limit, 10_000);
    AtomicInteger allowed = new AtomicInteger(0);
    CountDownLatch latch = new CountDownLatch(threadCount);
    CyclicBarrier barrier = new CyclicBarrier(threadCount);

    for (int i = 0; i < threadCount; i++) {
        new Thread(() -> {
            try { barrier.await(); } catch (Exception e) { Thread.currentThread().interrupt(); }
            if (guard.tryAcquire("test-ip")) allowed.incrementAndGet();
            latch.countDown();
        }).start();
    }
    latch.await();
    assertTrue(allowed.get() <= limit);
}
```
