# 测试审查报告（v2 r1）

## 审查结果
APPROVED

## 发现
无严重、无一般问题。

### 验证摘要
- **`refreshToken_shouldThrowOnSuspiciousRefresh()`**：使用 `ReflectionTestUtils` 预填充 `refreshTimestamps` 中 2 条时间戳，单次调用验证 `BusinessException(TOKEN_REFRESH_FAILED)` 抛出，审计事件 `eventType=TOKEN_REFRESH_REJECTED`、`failureReason=SUSPICIOUS_REFRESH`、`success=false`，与详细设计行为契约完全一致
- **`refreshToken_shouldSucceedWhenBelowMaxCount()`**：预填充 1 条时间戳后调用成功，验证阈值边界正确（1 < 2 时通过）
- **`refreshToken_shouldSucceedAfterExpiredEntriesCleaned()`**：预填充 2 条过期时间戳，验证惰性清理后刷新成功
- **`refreshToken_shouldThrowOnSuspiciousRefreshWithExpiredAndFresh()`**：预填充 1 条过期 + 2 条有效时间戳，验证清理后阈值命中
- **测试隔离**：`@BeforeEach` 每次创建新 `authService` 实例，`ReflectionTestUtils.setField` 替换 map 不影响其他测试
- **mock 装配**：与设计一致，无需新增依赖

所有测试与详细设计的行为契约完全一致，覆盖了异常刷新检测的全部路径与边界条件。
