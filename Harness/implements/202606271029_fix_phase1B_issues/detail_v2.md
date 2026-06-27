# 详细设计（v2）

## 概述

修改 `AuthServiceImpl.refresh()` 中异常刷新检测逻辑：检测到异常刷新时抛出 `BusinessException` 拒绝请求，并同步记录审计日志。涉及 `compute` 闭包内部重构（调整时间戳添加顺序、变更比较符、替换 `log.warn` 为阻断+审计）。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/impl/AuthServiceImpl.java` | 修改 | 重构 `refresh()` 中 `refreshTimestamps.compute` 闭包逻辑 |
| `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/service/AuthServiceTest.java` | 修改 | 新增 `refreshToken_shouldThrowOnSuspiciousRefresh()` 测试 |

## 类型定义

无新增类型。修改仅涉及已有类的内部方法实现。

### AuthServiceImpl (已有类)

**修改方法**：`refreshToken(String token)` 第 270-283 行

**变更内容**：

1. 将 `long now = System.currentTimeMillis()` 从闭包外部移至闭包内部首行
2. 将过期条目清理（`while` 循环）置于 `addLast` 之前
3. 将 `deque.addLast(now)` 移至异常检测之后（抛出异常时 deque 不包含本次时间戳）
4. 将 `deque.size() > REFRESH_MAX_COUNT` 改为 `deque.size() >= REFRESH_MAX_COUNT`
5. 将 `log.warn(...)` 替换为：
   - 审计日志：`securityAuditLogger.logAudit(SecurityAuditEvent.now(SecurityAuditEventType.TOKEN_REFRESH_REJECTED, userId, user.getUsername(), clientIp, false, "SUSPICIOUS_REFRESH", null, null))`
   - 抛异常：`throw new BusinessException(GlobalErrorCode.TOKEN_REFRESH_FAILED)`

**修改后方法签名不变**：`public TokenRefreshResponse refreshToken(String token)`

**修改后闭包逻辑**：
```
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

**变更要点**：
- `now` 移入 lambda：避免闭包捕获 mutable 外部变量，语义内聚
- 过期清理前置：检测前保证 deque 中仅包含有效窗口内的条目，避免过期条目导致误判
- `addLast` 后置：检测通过后才添加本次时间戳，确保抛出异常时 deque 状态与调用前一致
- `>=` 替代 `>`：当窗口内已有 `REFRESH_MAX_COUNT` 次记录时即拦截，语义更严格（`REFRESH_MAX_COUNT=2` 表示窗口内最多 1 次历史刷新 + 本次刷新为第 2 次，超过即异常）
- `throw` 从 `compute` 闭包内抛出：`BusinessException` 为 `RuntimeException`，`ConcurrentHashMap.compute` 将直接向上传播，方法正常退出

### AuthServiceTest (已有测试类)

**新增测试方法**：

```
refreshToken_shouldThrowOnSuspiciousRefresh()
```

**测试目的**：验证同一用户在 `REFRESH_WINDOW_MS` (5s) 内发起超过 `REFRESH_MAX_COUNT` (2) 次刷新请求时被拒绝。

**测试策略**：
1. 标准 mock 装配（与 `refreshToken_shouldSucceed` 相同的正常 mock 设置）
2. 首次调用 `authService.refreshToken("valid-token")` — 预期成功
3. 第二次调用 `authService.refreshToken("valid-token")` — 预期成功（窗口内累积 2 条记录，未超过 ≥2 阈值）
4. 第三次调用 `authService.refreshToken("valid-token")` — 预期抛出 `BusinessException(GlobalErrorCode.TOKEN_REFRESH_FAILED)`
5. 验证审计日志：捕获 `SecurityAuditEvent`，断言 `eventType=TOKEN_REFRESH_REJECTED`、`failureReason="SUSPICIOUS_REFRESH"`、`success=false`

**替代方案**（如连续三次真实调用测试不可靠）：
- 使用 `ReflectionTestUtils` 或 `InjectMocks` 后直接操作 `refreshTimestamps` 字段，预填充 `REFRESH_MAX_COUNT` 条时间戳到窗口内，然后单次调用验证阻断
- 建议采用此方案：更可控，不依赖时序

## 错误处理

- 异常刷新检测通过 `throw new BusinessException(GlobalErrorCode.TOKEN_REFRESH_FAILED)` 拒绝请求
- `GlobalExceptionHandler.handleBusinessException()` 将此映射为标准错误响应（JSON 格式 `{code, message}`），与现有 refresh 拒绝路径一致
- 异常在 `compute` 闭包内抛出，通过 `ConcurrentHashMap.compute` 向上传播到 `refreshToken()` 方法外
- `deque` 在抛异常时不包含本次时间戳，状态回滚

## 行为契约

- **前置条件**：`refreshTimestamps.compute` 执行前，`userId`、`user`、`clientIp` 均可用且非空
- **正常路径**：窗口内历史刷新次数 < `REFRESH_MAX_COUNT` 时，`deque.addLast(now)`，返回 `deque`，继续生成新 Token
- **异常路径**：窗口内历史刷新次数 >= `REFRESH_MAX_COUNT` 时，记录审计事件后抛出 `BusinessException`，不生成新 Token，deque 不变
- **窗口精确性**：过期条目在每次 `compute` 时惰性清除，`peekFirst < now - REFRESH_WINDOW_MS` 条件筛选有效窗口
- **并发安全**：`ConcurrentHashMap.compute` 提供 key 级别的原子性，内部操作无需额外同步
- **副作用顺序**：审计日志在抛出异常之前记录，即使 `logAudit` 抛出异常（非预期），原始 `deque` 状态不变（未添加本次时间戳）

## 依赖关系

- 依赖（已有）：`SecurityAuditLogger`、`SecurityAuditEvent.now()`、`SecurityAuditEventType.TOKEN_REFRESH_REJECTED`、`GlobalErrorCode.TOKEN_REFRESH_FAILED`、`BusinessException`
- 依赖（已有）：`ConcurrentHashMap.compute` API、`ArrayDeque`、`userId` / `user` / `clientIp` 等外围变量
- 无新增外部依赖
- 无暴露给后续任务的新接口
