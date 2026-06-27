# 任务指令（v19）

## 动作
NEW

## 任务描述
测试补充：`AuthServiceTest.java` 中现有 `login_shouldThrowIpLocked()` 和 `login_shouldThrowUsernameLocked()` 方法末尾各追加一条 `ex.getArgs()` 断言，验证 `BusinessException` 抛出时携带正确的消息参数：
- IP 锁定 → `assertEquals("30分钟", ex.getArgs()[0])`
- 用户名锁定 → `assertEquals("15分钟", ex.getArgs()[0])`

仅修改 `AuthServiceTest` 一个文件，不新增任何测试方法。

## 选择理由
批次 7 第二个任务（T16）。现有测试已验证 `ACCOUNT_LOCKED` errorCode 和审计事件，但未验证 `BusinessException.getArgs()` —— OOD 10.2 节定义的锁定消息模板为 `"账户已锁定，请{锁定时间}后重试"`，args 的正确传递是 `GlobalExceptionHandler.formatMessage()` 正确插值的必要前提。T16 在 T15 PASSED（609 测试全部通过）之后执行，无其他前置依赖。

## 任务上下文
**OOD 10.2 节**：
> ACCOUNT_LOCKED: `"账户已锁定，请{锁定时间}后重试"`

**AuthServiceImpl.java** 抛出点：
```java
// L101-104: IP 锁定
if (loginAttemptTracker.isIpLocked(clientIp)) {
    ...
    throw new BusinessException(GlobalErrorCode.ACCOUNT_LOCKED, "30分钟");
}

// L107-110: 用户名锁定
if (loginAttemptTracker.isUsernameLocked(request.username())) {
    ...
    throw new BusinessException(GlobalErrorCode.ACCOUNT_LOCKED, "15分钟");
}
```

**诊断报告 §T16 修改建议：**
> AuthServiceTest 中增加锁场景的消息内容断言，需同时验证 IP 锁定和用户名锁定两种插值结果。
> 注意：T3 修复后消息插值逻辑发生变化，T16 的测试应验证插值后的结果而非模板原文。

## 已有代码上下文

### AuthServiceTest.java 现有锁定测试（L122-159）
```java
@Test
void login_shouldThrowIpLocked() {
    when(rateLimitGuard.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(true);
    when(loginAttemptTracker.isIpLocked(anyString())).thenReturn(true);

    BusinessException ex = assertThrows(BusinessException.class,
            () -> authService.login(new LoginRequest("testuser", "password")));
    assertEquals(GlobalErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());

    ArgumentCaptor<SecurityAuditEvent> captor = ArgumentCaptor.forClass(SecurityAuditEvent.class);
    verify(securityAuditLogger).logAudit(captor.capture());
    SecurityAuditEvent event = captor.getValue();
    assertEquals(SecurityAuditEventType.LOGIN_FAILED, event.eventType());
    assertEquals("IP_LOCKED", event.failureReason());
    assertNull(event.userId());
    assertNull(event.username());
    assertFalse(event.success());
    // 缺失：assertEquals("30分钟", ex.getArgs()[0]);
}

@Test
void login_shouldThrowUsernameLocked() {
    when(rateLimitGuard.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(true);
    when(loginAttemptTracker.isIpLocked(anyString())).thenReturn(false);
    when(loginAttemptTracker.isUsernameLocked(anyString())).thenReturn(true);

    BusinessException ex = assertThrows(BusinessException.class,
            () -> authService.login(new LoginRequest("testuser", "password")));
    assertEquals(GlobalErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());

    ArgumentCaptor<SecurityAuditEvent> captor = ArgumentCaptor.forClass(SecurityAuditEvent.class);
    verify(securityAuditLogger).logAudit(captor.capture());
    SecurityAuditEvent event = captor.getValue();
    assertEquals(SecurityAuditEventType.LOGIN_FAILED, event.eventType());
    assertEquals("USERNAME_LOCKED", event.failureReason());
    assertNull(event.userId());
    assertNull(event.username());
    assertFalse(event.success());
    // 缺失：assertEquals("15分钟", ex.getArgs()[0]);
}
```

### 当前测试基线
mvn clean test 全部 609 测试通过，0 失败 5 跳过，BUILD SUCCESS。

### 相关文件路径
- `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/service/AuthServiceTest.java`
