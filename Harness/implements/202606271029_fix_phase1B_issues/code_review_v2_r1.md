# 代码审查报告（v2 r1）

## 审查结果
APPROVED

## 发现

无严重、无一般问题。实现与详细设计（`detail_v2.md`）完全一致：

- `AuthServiceImpl.java:270-285` — `compute` 闭包重构：`now` 移入闭包、过期清理前置、`deque.addLast(now)` 后置、`>=` 判定、审计日志+异常阻断替代 `log.warn`
- `AuthServiceTest.java:507-538` — 使用 `ReflectionTestUtils` 预填充 `refreshTimestamps` 方式测试，正确断言 `BusinessException` 及审计事件内容
- 所需 import（`ReflectionTestUtils`, `ConcurrentHashMap`, `ArrayDeque`, `Deque`）均存在
- `@BeforeEach` 每次创建新实例，无状态泄漏风险
