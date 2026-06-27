# 详细设计（v3）

## 概述

修复 `AuthServiceImpl.logout()` 中三项编码缺陷：T9（过期 token 跳过审计日志）、T10（二次 JWT 解析）、T18（refreshTimestamps 无过期清理）。涉及 1 个源文件 + 1 个测试文件的修改。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/impl/AuthServiceImpl.java` | 修改 | 重构 `logout()` 方法：分支处理 claims==null，消除二次 JWT 解析，添加 refreshTimestamps 清理 |
| `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/service/AuthServiceTest.java` | 修改 | 修改 `logout_shouldNotAuditWhenTokenInvalid` + 新增 2 个测试方法 |

## 类型定义

无新增类型。修改仅涉及 `AuthServiceImpl.logout()` 方法体 + 测试类。

### AuthServiceImpl.logout() 修改后结构

**修改前代码**（第 177-206 行）：
```java
public void logout(String token, String refreshToken) {
    if (token == null) { return; }
    Claims claims = jwtTokenProvider.validateToken(token, null);
    if (claims == null) { return; }                          // ← 移除
    String jti = jwtTokenProvider.getJtiFromToken(token);    // ← 改为从 claims 获取
    if (jti != null) { tokenBlacklist.add(jti, claims.getExpiration().getTime()); }
    Long userId = jwtTokenProvider.getUserIdFromClaims(claims);
    String username = claims.getSubject();
    // ... mask refreshToken ...
    securityAuditLogger.logAudit(SecurityAuditEvent.now(...));
    log.info("用户登出成功");
}
```

**修改后方法**（逻辑重构）：

```
logout(String token, String refreshToken):
  1. if token == null → return
  2. Claims claims = validateToken(token, null)
  3. if claims != null:
       a. jti = claims.get("jti", String.class)    // T10: 从 claims 获取，消除二次解析
       b. if jti != null → tokenBlacklist.add(jti, claims.getExpiration().getTime())
       c. userId = getUserIdFromClaims(claims)
       d. username = claims.getSubject()
       e. refreshTimestamps.remove(userId)          // T18: 清理条目
  4. else (claims == null):
       a. userId = null                             // T9: 即使 claims 为 null 也记录审计日志
       b. username = null
  5. refreshTokenMasked = mask(refreshToken)         // 公共路径，不受 claims 是否为空影响
  6. securityAuditLogger.logAudit(LOGOUT, userId, username, clientIp, true, null, refreshTokenMasked, null)
  7. log.info("用户登出成功")
```

**方法签名不变**：`public void logout(String token, String refreshToken)`

### AuthServiceTest 测试变更

#### 修改：logout_shouldNotAuditWhenTokenInvalid（第 665-673 行）

| 项目 | 旧行为 | 新行为 |
|------|--------|--------|
| Mock | `validateToken` 返回 null | `validateToken` 返回 null |
| 验证 | `verify(securityAuditLogger, never()).logAudit(any());` | `verify(securityAuditLogger).logAudit(captor.capture());` — 验证 `eventType=LOGOUT`, `userId=null`, `username=null`, `success=true`, `refreshTokenMasked=null` |
| 验证 | `verify(tokenBlacklist, never()).add(any(), anyLong());` | 不变：仍验证 `tokenBlacklist.add` 未被调用 |
| 测试名 | 不变 | 不变（语义变更：无效 token 不再跳过审计） |

#### 新增：logout_shouldRemoveRefreshTimestampsEntry

```
方法名：logout_shouldRemoveRefreshTimestampsEntry()
目的：验证 logout 后 refreshTimestamps 中 userId 条目被移除

准备：
  1. 正常 mock：validateToken → claims, claims.getSubject() → "testuser", getUserIdFromClaims → 1L
  2. 预填充 refreshTimestamps：{1L → [now, now]}
  3. 注意：claims.get("jti", String.class) → "jti-xxx"（T10 改造后从 claims 获取）
  4. claims.getExpiration() → new Date(future)

执行：authService.logout("valid-token", null)

验证：
  1. verify(tokenBlacklist).add(...) — 正常黑名单
  2. 通过 ReflectionTestUtils.getField 获取 refreshTimestamps
  3. assertNull(refreshTimestamps.get(1L)) — 条目被移除
  4. verify(securityAuditLogger).logAudit(...) — 审计日志正常记录
```

#### 新增：logout_shouldGetJtiFromClaims（可选）

```
方法名：logout_shouldGetJtiFromClaims()
目的：间接验证 jti 从 claims 获取而非二次解析 token

准备：与 logout_shouldBlacklistToken 相同 mock 设置

执行：authService.logout("valid-token", null)

验证：
  1. verify(jwtTokenProvider, never()).getJtiFromToken(anyString()) — 确保未调用二次解析方法
  2. verify(tokenBlacklist).add(anyString(), anyLong()) — jti 黑名单正常工作
```

## 错误处理

- T9/T10 分支中，token 过期/无效（claims==null）时不抛出异常，仅审计日志中 userId/username 为 null
- T18 中 `refreshTimestamps.remove(userId)` 在 ConcurrentHashMap 上是线程安全的原子操作，key 不存在时无副作用
- 审计日志的 `success=true` 字段在 claims==null 时仍为 true（语义：logout 请求本身成功处理）

## 行为契约

- **前置条件**：无变化（token 为 null 时快速返回的守卫不变）
- **claims 非空路径**：
  - jti 从 `claims.get("jti", String.class)` 获取（而非 `jwtTokenProvider.getJtiFromToken(token)`）
  - tokenBlacklist.add 正常执行
  - refreshTimestamps.remove(userId) 正常执行
  - 审计日志含有效 userId/username
- **claims 为空路径**：
  - jti 不获取，黑名单不操作
  - refreshTimestamps 不清理（没有 userId）
  - 审计日志 userId=null, username=null
- **并发安全**：refreshTimestamps.remove 在 ConcurrentHashMap 上安全，无迭代器并发问题
- **副作用顺序**：blacklist → remove → mask → audit，不依赖特定顺序

## 依赖关系

- 依赖（已有）：`jwtTokenProvider.validateToken`、`jwtTokenProvider.getUserIdFromClaims`、`Claims.get("jti", String.class)`、`Claims.getExpiration`、`Claims.getSubject`
- 依赖（已有）：`refreshTimestamps`（`ConcurrentHashMap<Long, Deque<Long>>`）、`refreshTimestamps.remove`
- 依赖（已有）：`SecurityAuditLogger.logAudit`、`SecurityAuditEvent.now`、`SecurityAuditEventType.LOGOUT`
- 无新增外部依赖
- 测试新增依赖：`ReflectionTestUtils.getField`（用于验证 refreshTimestamps 条目移除）
