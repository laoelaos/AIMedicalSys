# 任务指令（v3）

## 动作
NEW

## 任务描述
修复 `AuthServiceImpl.java` 中三项编码缺陷，目标文件 1 个源文件 + 1 个测试文件：

目标文件：
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/impl/AuthServiceImpl.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/service/AuthServiceTest.java`

### T9: logout() 过期 token 跳过审计日志
- 当前位置：第 177-206 行
- 当前代码第 182-185 行：`Claims claims = jwtTokenProvider.validateToken(token, null); if (claims == null) { return; }`
- 问题：token 过期或无效时直接 return，跳过第 202-203 行的审计日志记录
- 修复：移除 `if (claims == null) return;`，改为分支处理：
  - claims 非空：按现有逻辑（获取 jti、userId、username、记录审计日志）
  - claims 为 null：仍然记录审计日志，但 userId/username 为 null，refreshTokenMasked 仍可正常处理
- 注意：第 187 行 `getJtiFromToken(token)` 在 claims 为 null 时需保护（移入 claims 非空分支）

### T10: logout() 二次 JWT 解析
- 当前位置：第 182 行 + 第 187 行
- 当前代码第 182 行 `validateToken(token, null)` 已解析出 claims，第 187 行 `getJtiFromToken(token)` 再次解析
- 修复：从 `claims.get("jti", String.class)` 获取 jti，消除二次解析
- 注意：仅在 claims 非空时执行（T9 分支处理后）

### T18: refreshTimestamps 无过期清理
- 当前位置：第 68 行 `ConcurrentHashMap<Long, Deque<Long>> refreshTimestamps`
- 问题：用户登出或停止刷新后，对应 `Deque<Long>` 从未被移除，导致不活跃条目永久驻留
- 修复方案 A（推荐）：在 `logout()` 方法中调用 `refreshTimestamps.remove(userId)` 清理当前用户的 refreshTimestamps 条目
- 修复方案 B（可选增强）：如果 `logout()` 中无法获取 userId（claims 为 null 时），则不做清理——这与 T9 的"即使 claims=null 也记录审计日志"语义一致
- 验证：T9 修复后 logout 有 claims 非空分支可获取 userId，在此分支中执行 remove

## 选择理由
R2 (T2) 已完成且全量测试通过。R3 三项修复均位于 `AuthServiceImpl.java`，按计划推进。T9 与 T10 位置相邻（同属 logout 方法），T18 所属 refreshTimestamps 的量级适中，三者在同一文件中但操作不同方法区域（logout 与 refresh 方法），无实质文本冲突。合并可减少提交和审查流转次数。

## 任务上下文
```java
// AuthServiceImpl.java:177-206 (logout 方法)
@Override
public void logout(String token, String refreshToken) {
    if (token == null) {
        return;
    }

    Claims claims = jwtTokenProvider.validateToken(token, null);  // ← T9: 需分支处理 claims==null
    if (claims == null) {                                          // ← T9: 移除或改写
        return;                                                    // ← T9: 移除
    }

    String jti = jwtTokenProvider.getJtiFromToken(token);          // ← T10: 改为从 claims 获取
    if (jti != null) {
        tokenBlacklist.add(jti, claims.getExpiration().getTime());
    }

    Long userId = jwtTokenProvider.getUserIdFromClaims(claims);
    String username = claims.getSubject();

    String refreshTokenMasked = null;
    if (refreshToken != null) {
        refreshTokenMasked = refreshToken.length() >= 8
                ? refreshToken.substring(0, 8) + "***"
                : refreshToken + "***";
    }

    securityAuditLogger.logAudit(SecurityAuditEvent.now(           // ← T9: 即使 claims==null 也执行
            SecurityAuditEventType.LOGOUT, userId, username, getClientIp(), true, null, refreshTokenMasked, null));

    log.info("用户登出成功");  // ← T18: 此处可添加 refreshTimestamps.remove(userId)
}
```

```java
// AuthServiceImpl.java:68 (refreshTimestamps 声明)
private final ConcurrentHashMap<Long, Deque<Long>> refreshTimestamps = new ConcurrentHashMap<>();
```

```java
// AuthServiceImpl.java:270-285 (refreshTimestamps.compute — R2 已修复)
refreshTimestamps.compute(userId, (k, deque) -> {
    if (deque == null) { deque = new ArrayDeque<>(); }
    long now = System.currentTimeMillis();
    while (!deque.isEmpty() && deque.peekFirst() < now - REFRESH_WINDOW_MS) { deque.pollFirst(); }
    if (deque.size() >= REFRESH_MAX_COUNT) {
        securityAuditLogger.logAudit(SecurityAuditEvent.now(
                SecurityAuditEventType.TOKEN_REFRESH_REJECTED, userId, user.getUsername(), clientIp, false, "SUSPICIOUS_REFRESH", null, null));
        throw new BusinessException(GlobalErrorCode.TOKEN_REFRESH_FAILED);
    }
    deque.addLast(now);
    return deque;
});
```

## 已有代码上下文
- `AuthServiceImpl.java:177-206` — `logout()` 方法（T9 + T10 目标）
- `AuthServiceImpl.java:68` — `refreshTimestamps` 字段声明（T18 目标）
- `AuthServiceImpl.java:270-285` — `refreshTimestamps.compute` 闭包（R2 已修复，不变更）
- `AuthServiceTest.java` — 存量测试 32 个方法，需：
  1. 修改 `logout_shouldNotAuditWhenTokenInvalid`（现有方法验证"无效 token 不记审计日志"）→ 改为验证过期/无效 token 仍记录 logout 审计事件（userId/username 为 null）
  2. 新增 `logout_shouldRemoveRefreshTimestampsEntry` 验证 logout 后 refreshTimestamps 中 userId 条目被移除
  3. 新增 `logout_shouldGetJtiFromClaims` 验证 jti 从 claims 获取而非二次解析（可选，可通过验证无 `getJtiFromToken` 调用来间接验证）
- 依赖注入：`SecurityAuditLogger` 已注入
