# 计划审查报告（v7 r1）

## 审查结果
APPROVED

## 发现

- **[轻微]** 计划中 `ConcurrentHashMap<String, SlidingWindowCounter>` 的描述与 SlidingWindowCounter 实际内部结构（`ConcurrentHashMap<String, Deque<Long>>`）不一致。若按字面实现将导致每个 IP 创建独立的 SlidingWindowCounter 实例（产生多余清理线程），更简洁的做法是直接复用注入的 `counter.tryAcquire(ip, 100, 60_000)`。不影响计划可行性，实现阶段注意即可。

- **[轻微]** JSON 序列化方式未明确。响应体需写入 `Result.fail(RATE_LIMITED_GLOBAL)` 的 JSON 字符串，项目依赖 `spring-boot-starter-web`（含 Jackson），实现时直接使用 `ObjectMapper` 或委托已有工具类即可，不构成计划风险。

## 结论
无严重或一般问题，计划覆盖 R7 GlobalRateLimitFilter 全部需求，依赖已验证存在（SlidingWindowCounter ✅、GlobalErrorCode.RATE_LIMITED_GLOBAL ✅、spring-boot-starter-web/security ✅），测试覆盖充分，通过。
