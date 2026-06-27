# 测试审查报告（v8 r1）

## 审查结果
APPROVED

## 发现
无严重或一般问题。测试代码完整覆盖了 detail_v8.md 中 T5 和 T13 的所有行为契约场景：

- **T5 — refreshToken() IP 失败记录**：`refreshToken_shouldThrowOnDisabledUser`（L222）和 `refreshToken_shouldThrowOnUserNotFound`（L237）均验证了 `recordIpFailure` 被调用，覆盖用户禁用和用户不存在两条路径。
- **T13 — login() dummy 比对**：`login_shouldThrowUserNotFound`（L127）和 `login_shouldThrowUserDisabled`（L141）均验证了 `matches(eq("dummy"), anyString())` 被调用而非 `encode()`，覆盖用户不存在和用户禁用两条路径。

所有测试与实现代码（`AuthServiceImpl.java`）的行为一致，验证点准确，Mock 设置充分且无冗余。`getClientIp()` 在无请求上下文时返回 `"unknown"` 的行为符合设计文档中"含 'unknown' 情况"的预期。
