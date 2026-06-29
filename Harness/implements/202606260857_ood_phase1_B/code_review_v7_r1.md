# 代码审查报告（v7 r1）

## 审查结果
APPROVED

## 发现
- **[轻微]** `GlobalRateLimitFilterTest.java:162` — 测试方法 `shouldUseXForwardedForHeader` 无法有效区分「正确使用 X-Forwarded-For header」与「错误回退到 getRemoteAddr()」两种行为。请求设置了 `X-Forwarded-For: 10.0.0.1` 和 `remoteAddr: 192.168.1.1`，但两种实现路径都会对同一个底层 key 累积 100 次请求后在第 101 次返回 429，测试通过而无法证明 header 优先。建议补充一个验证步骤：在耗尽 X-Forwarded-For 衍生 IP 的配额后，发起一个不含 X-Forwarded-For header 但使用相同 remoteAddr 的请求，断言其仍返回 200，以证明计数器实际使用的是 X-Forwarded-For 中的 IP。

## 修改要求（仅 REJECTED 时）
（无）
