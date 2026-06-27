# 详细设计（v18）

## 概述

在 `AuthServiceTest` 中新增测试方法 `login_shouldThrowLoginFailed_whenUserDeleted()`，覆盖 `User.deleted=true` 场景下 `AuthServiceImpl.login()` 抛出 `BusinessException(GlobalErrorCode.LOGIN_FAILED)` 的独立测试。新增方法不修改任何已有方法。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/service/AuthServiceTest.java` | 修改 | 在 `login_shouldThrowUserDeleted()` 后新增 `@Test` 方法 |

## 类型定义

### `login_shouldThrowLoginFailed_whenUserDeleted` 方法

**形态**：`@Test` 实例方法
**类**：`AuthServiceTest`
**职责**：验证 `User.deleted=true` 时 `login()` 抛出 `BusinessException(GlobalErrorCode.LOGIN_FAILED)`

**方法签名**：
```java
@Test
void login_shouldThrowLoginFailed_whenUserDeleted()
```

**Mock 设置步骤**：
1. `testUser.setDeleted(true)`
2. `rateLimitGuard.tryAcquire(anyString(), anyInt(), anyLong())` → `true`
3. `loginAttemptTracker.isIpLocked(anyString())` → `false`
4. `loginAttemptTracker.isUsernameLocked(anyString())` → `false`
5. `userRepository.findByUsername("testuser")` → `Optional.of(testUser)`

**验证断言**：
1. `assertThrows(BusinessException.class, () -> authService.login(new LoginRequest("testuser", "password")))` 捕获异常 `ex`
2. `assertEquals(GlobalErrorCode.LOGIN_FAILED, ex.getErrorCode())`
3. `verify(passwordEncoder).matches(eq("dummy"), anyString())`
4. `verify(loginAttemptTracker).recordIpFailure(anyString())`
5. `verify(loginAttemptTracker).recordUsernameFailure(anyString())`
6. 审计事件验证：捕获 `ArgumentCaptor<SecurityAuditEvent>`，断言 `eventType=LOGIN_FAILED, failureReason=ACCOUNT_DELETED, userId=1L, username="testuser", success=false`

**插入位置**：在 `login_shouldThrowUserDeleted()` 方法（当前 L282）之后、`refreshToken_shouldSucceed()` 之前。

## 行为契约

- `testUser` 由 `@BeforeEach setUp()` 重置，方法内 `setDeleted(true)` 对同用例内后续调用生效
- `login_shouldThrowUserDeleted()` 已有方法保持不动，与其形成"前后相邻摆放"的命名对照

## 依赖关系

- **被测对象**：`AuthServiceImpl.login(LoginRequest)`
- **Mock**：`UserRepository`, `RateLimitGuard`, `LoginAttemptTracker`, `PasswordEncoder`（由 `@Mock` 声明）, `SecurityAuditLogger`
- **已有固定桩**：`testUser`（`setUp()` 初始化，`enabled=true, deleted=false`）
- **断言目标**：`BusinessException`、`GlobalErrorCode.LOGIN_FAILED`

## 错误处理

无需自定义错误类型。仅验证目标场景抛出预期的 `BusinessException` 及 `errorCode`。
