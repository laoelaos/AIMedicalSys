# 代码审查报告（v5 r1）

## 审查结果
REJECTED

## 发现

- **[一般]** `PrescriptionThreadPoolConfig.java:25` — `Executors.newCachedThreadPool(factory)` 创建一个无界线程池，在高并发下可能创建大量平台线程导致资源耗尽。设计规格明确要求使用虚拟线程以获得轻量级隔离；因 Java 17 兼容性无法使用虚拟线程时，应改用有界线程池（如 `new ThreadPoolExecutor(0, maxSize, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), factory)`）以保持可控性。

- **[轻微]** `PrescriptionAssistServiceImpl.java:351-353` — 死代码：`result` 对象创建后立即设置 `PROCESSING` 状态，但紧接着创建了 `processingResult` 并存入 store，`result` 的初始状态赋值从未被读取，随后被最终状态覆盖。建议删除 `result` 的初始 PROCESSING 赋值，或直接复用 `processingResult`。

## 修改要求（仅 REJECTED 时）

1. **`PrescriptionThreadPoolConfig.java:25`** — `newCachedThreadPool()` → 有界线程池
   - 问题：无界线程池在 AI 调用耗时较长时可能创建数千个平台线程，导致 OOM 或过度上下文切换，偏离设计规格的轻量级隔离意图。
   - 期望：使用 `new ThreadPoolExecutor(0, CORE_POOL_SIZE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), factory)` 或等效的有界实现，`CORE_POOL_SIZE` 根据并发需求设定合理上限（如 50-100）。

2. **`PrescriptionAssistServiceImpl.java:351-353`** — 消除死代码
   - 问题：`result` 初始 PROCESSING 赋值无用，产生阅读困惑。
   - 期望：删除第 351-353 行对 `result` 的 PROCESSING 赋值，仅保留 `processingResult` 的创建和存储逻辑。
