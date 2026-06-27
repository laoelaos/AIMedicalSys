# 任务指令（v12）

## 动作
NEW

## 任务描述
修复 T14：SlidingWindowCounter 中 ReentrantLock 作用域不完整

代码修复，`auth/rateLimit/SlidingWindowCounter.java` 中在 `tryAcquire()` 方法内添加 `ReentrantLock` 保护，包裹 `windows.compute()` 操作，使锁策略与 OOD 4.1 节设计约定一致。

预期文件路径：`AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/rateLimit/SlidingWindowCounter.java`

## 选择理由
T14 是批次 2（编码规范对齐）唯一遗留项，与 T4/T27/T20 同为编码规范对齐修复。当前处于第 12 轮，前 11 轮已完成 T1-T7、T10、T12、T13、T19-T21、T25-T27 共 16 项修复。T14 涉及独立文件 `SlidingWindowCounter.java`，变更量极小，无前置依赖，适合作为剩余 10 项修复的首个任务。

## 任务上下文

### 问题描述（源自诊断报告 §T14）
- `SlidingWindowCounter` 的 `ReentrantLock` 仅在 `cleanup()` 方法中加锁，`tryAcquire()` 方法完全未使用该锁
- OOD 4.1 节要求"ReentrantLock 保护窗口内的排序集合"
- 实际 `tryAcquire()` 使用 `ConcurrentHashMap.compute()` 已提供原子性保证，功能正确无数据竞争
- 但锁策略与 OOD 设计约定不一致，属于实现与设计约定的偏差

### 修改建议（诊断报告）
在 `tryAcquire` 方法中添加 `ReentrantLock` 保护，包裹 `windows.compute()` 操作。注意因 `ConcurrentHashMap.compute()` 已提供原子性，添加锁不会改变功能行为，仅使锁策略与设计约定一致。

### OOD 设计约束（节选自 OOD 4.1 节）
"滑动窗口以 `ReentrantLock` 保护窗口内的排序集合，确保每个 IP 的窗口对象独立加锁。"

## 已有代码上下文

### 当前文件
`SlidingWindowCounter.java`：
- `lock` 字段：`private final ReentrantLock lock;`（L14），构造器中初始化（L19）
- `tryAcquire()` 方法（L28-52）：使用 `windows.compute(key, ...)` 实现原子性的窗口操作，但未使用 `lock`
- `cleanup()` 方法（L54-61）：正确使用了 `lock.lock()` / `lock.unlock()` 保护 `removeIf` 操作

### 修改要求
在 `tryAcquire()` 方法中，在 `windows.compute()` 之前加 `lock.lock()`，在 `try { ... } finally { lock.unlock(); }` 中包裹 `compute()` 调用。

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
            // ... 现有逻辑不变 ...
        });
    } finally {
        lock.unlock();
    }
    return result[0];
}
```

### 测试
已有 `SlidingWindowCounterTest.java`，应运行确认修改后测试仍全部通过。

## 修订说明（v12 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| 轮次编号前后不一致：plan.md R11 NEW 与 task_v12.md 第 12 轮不一致 | plan.md `## R11 NEW` → `## R12 NEW` |
| 剩余任务缺失路线图 | plan.md 补充后续任务路线图表（R13-R21）及批次 6 内部执行顺序 |
