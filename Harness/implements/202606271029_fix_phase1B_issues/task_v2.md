# 任务指令（v2）

## 动作
NEW

## 任务描述
修改 `AuthServiceImpl.refresh()` 中异常刷新检测逻辑：检测到异常刷新时抛出 `BusinessException` 拒绝请求，而非仅 `log.warn` 后继续返回 Token。

目标文件：
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/impl/AuthServiceImpl.java`

## 选择理由
P0 安全隐患。R1 (T1) 已完成，当前推进到计划中 R2。T2 为 P0 级别修复，与 T1 无代码级依赖，可独立实施。

## 任务上下文
- 当前代码 `AuthServiceImpl.java:270-283`：`refreshTimestamps.compute` 闭包中检测 `deque.size() > REFRESH_MAX_COUNT` 时仅 `log.warn(...)`，然后继续执行 refresh 流程生成新 Token
- OOD Docs/05_ood_phase1_B.md 4.2 节第 502 行：异常刷新检测仅定义 `log.warn` 告警，未定义阻断逻辑——本次修复同时构成 OOD 设计补充的编码实现
- 修改目标：在第 279-281 行将 `log.warn(...)` 改为抛出 `BusinessException(GlobalErrorCode.TOKEN_REFRESH_FAILED)`，并记录审计日志（`SecurityAuditEventType.TOKEN_REFRESH_REJECTED`）
- 审计日志记录需参考同一方法中已有 token 版本不匹配时的做法（第 265-267 行）：`securityAuditLogger.logAudit(SecurityAuditEvent.now(SecurityAuditEventType.TOKEN_REFRESH_REJECTED, ...))` 后抛异常
- 注意：抛出异常后 `deque` 的状态应保持与抛异常前一致——当前实现中 `deque.addLast(now)` 和过期清理已执行，建议在检测到异常时从 deque 中移除刚添加的时间戳，或将抛异常置于 `addLast` 之前

## 已有代码上下文
```java
// AuthServiceImpl.java:270-283
long now = System.currentTimeMillis();
refreshTimestamps.compute(userId, (k, deque) -> {
    if (deque == null) {
        deque = new ArrayDeque<>();
    }
    deque.addLast(now);
    while (!deque.isEmpty() && deque.peekFirst() < now - REFRESH_WINDOW_MS) {
        deque.pollFirst();
    }
    if (deque.size() > REFRESH_MAX_COUNT) {
        log.warn("异常刷新检测: userId={}, 5秒内刷新{}次", userId, deque.size());
    }
    return deque;
});
// ...继续生成新 Token
```

修改后预期伪代码：
```java
refreshTimestamps.compute(userId, (k, deque) -> {
    if (deque == null) {
        deque = new ArrayDeque<>();
    }
    long now = System.currentTimeMillis();
    while (!deque.isEmpty() && deque.peekFirst() < now - REFRESH_WINDOW_MS) {
        deque.pollFirst();
    }
    if (deque.size() >= REFRESH_MAX_COUNT) {
        securityAuditLogger.logAudit(SecurityAuditEvent.now(
                SecurityAuditEventType.TOKEN_REFRESH_REJECTED, userId, user.getUsername(), clientIp, false, "SUSPICIOUS_REFRESH", null, null));
        throw new BusinessException(GlobalErrorCode.TOKEN_REFRESH_FAILED);
    }
    deque.addLast(now);
    return deque;
});
```
