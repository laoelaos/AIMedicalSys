# 测试审查报告（v3 r1）

## 审查结果
APPROVED

## 发现
无严重、无一般问题。

### 验证摘要
- **T9（claims==null 审计路径）** — `logout_shouldNotAuditWhenTokenInvalid` 验证 `validateToken` 返回 null 时审计日志仍记录，userId=null, username=null, success=true, refreshTokenMasked=null，黑名单未调用。 ✅
- **T10（消除二次 JWT 解析）** — `logout_shouldGetJtiFromClaims` 验证 `getJtiFromToken(anyString())` 未被调用，jti 黑名单通过 `claims.get("jti", String.class)` 正常工作。 ✅
- **T18（refreshTimestamps 清理）** — `logout_shouldRemoveRefreshTimestampsEntry` 通过 `ReflectionTestUtils` 预填充 `refreshTimestamps`，验证 logout 后条目被移除（`assertNull(after.get(1L))`）。 ✅
- **现有测试完整性** — `logout_shouldBlacklistToken`、`logout_shouldNotAuditWhenTokenNull`、`logout_shouldAuditWithRefreshTokenMasked` 均未受影响，mock 与断言与设计一致。
- **测试隔离** — `@BeforeEach` 每次创建新 `authService` 实例，`ReflectionTestUtils.setField` 修改不影响其他测试。
