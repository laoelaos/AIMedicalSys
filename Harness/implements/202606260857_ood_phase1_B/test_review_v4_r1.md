# 测试审查报告（v4 r1）

## 审查结果
APPROVED

## 发现

- **[轻微]** `InMemoryRateLimitGuardTest.java:34-39` — `shouldAllowUpToLimit` 与 `shouldUseDefaultLimitAndWindow`（89-95）功能高度重叠：前者仅断言 5 次成功，后者已完整覆盖「5 次成功 + 第 6 次失败」+ 默认参数验证。两者使用相同的无参构造 + 默认 key，前者未提供任何额外覆盖。可视为设计冗余（detail_v4.md 第 168 行已列出），非测试代码缺陷。
- **[轻微]** `InMemoryRateLimitGuardTest` — 未直接覆盖 `InMemoryRateLimitGuard(SlidingWindowCounter counter)` 单参构造（设计语义为「共享 counter 注入」）与 `tryAcquire(String key, int limit, long windowMs)` 三参重载。`SlidingWindowCounterTest` 间接验证了 `counter.tryAcquire(key, limit, windowMs)` 的行为，但 `InMemoryRateLimitGuard` 透传至 `counter.tryAcquire` 的契约未被独立测试。
- **[轻微]** `InMemoryRateLimitGuardTest.java:51-59` — `shouldAllowAfterWindowExpiry` 使用 `Thread.sleep(10_100)`，单测耗时 ≥10 秒。`SlidingWindowCounterTest.shouldAllowNewRequestAfterWindowExpiry`（72-79）采用 500ms 短窗口实现更快验证。两种风格并存可接受，但 10 秒级别的测试在 CI 抖动场景下属于脆弱点。
- **[轻微]** `code_v4.md:15` — 「编译通过，6 个单元测试全部通过」与实际不一致：当前 `InMemoryRateLimitGuardTest` 8 用例 + `SlidingWindowCounterTest` 11 用例 = 19 用例。`test_v4.md` 的 19 用例与实际一致，`code_v4.md` 的 6 用例数字陈旧。属文档漂移，不影响测试代码正确性。
- **[轻微]** `SlidingWindowCounterTest.java`（整文件）— 超出 detail_v4.md 设计范围（设计仅指定 `InMemoryRateLimitGuardTest`），但补充了 `SlidingWindowCounter` 的边界条件/错误路径/并发覆盖，属正向超出。仅作记录。
- **[轻微]** `SlidingWindowCounterTest.java` / `InMemoryRateLimitGuardTest.java` — 每个 `new SlidingWindowCounter()` 都启动一个 daemon `ScheduledExecutorService`（参见 `SlidingWindowCounter.java:20-25`），但测试方法结束未 `shutdown`。daemon 线程在 JVM 退出时自动回收，运行期间累积 ~19 个空闲 executor 但无功能性副作用。属测试卫生问题，非正确性问题。

## 验证摘要

- `InMemoryRateLimitGuardTest.java` — 8 用例覆盖默认参数、限流触发、窗口重置、并发、错误路径、独立 key；断言类型与边界值正确
- `SlidingWindowCounterTest.java` — 11 用例覆盖 `limit<=0`/`windowMs<=0`/null key 边界、跨 key 独立性、窗口过期、并发（同/异 key）；`shouldHandleConcurrentRequestsForDifferentKeys` 中 lambda 捕获 per-iteration `key` 变量语义正确（Java 8+ 规范）
- 测试不依赖 Spring 上下文、不 mock `SlidingWindowCounter`，与设计 `detail_v4.md` 第 178-179 行一致
- 测试断言与生产代码契约一致：`compute()` 原子性保证 `allowed <= limit`；`cleanup()` 仅清理空 Deque（v4 r1 修订后已修正）
- 无严重或一般级别问题

## 修改要求（仅 REJECTED 时）
无（APPROVED）