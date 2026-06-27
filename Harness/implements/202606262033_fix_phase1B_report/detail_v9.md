# 详细设计（v9）

## 概述

修复 `AuthController.java` 中两个问题：T6（API 路径修正）和 T7（SecurityContext 重构），同步修改 `AuthControllerTest.java`。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/controller/AuthController.java` | 修改 | T6：`@PutMapping("/me")` → `@PutMapping("/profile")`；T7：删除 `JwtTokenProvider` 依赖，新增 `getCurrentUserId()`，重构 `changePassword()` |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/controller/AuthControllerTest.java` | 修改 | 删除 `@Mock JwtTokenProvider`、修改 `setUp()` 构造、删除无关 import、重构 `ChangePasswordTests` mock 策略 |

## 类型定义

### 1. AuthController.getCurrentUserId() — 新增私有方法

**形态**：`private` 方法
**位置**：放在 `extractToken()` 方法之前（约 L92）

```java
private Long getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Object principal = authentication.getPrincipal();
    if (principal instanceof Long) {
        return (Long) principal;
    }
    if (principal instanceof Integer) {
        return ((Integer) principal).longValue();
    }
    throw new IllegalStateException("无法从SecurityContext获取用户ID");
}
```

**与 MenuController.getCurrentUserId() (MenuController.java:148-158) 的模式完全一致**，包括：获取 Authentication、principal 类型判断（Long/Integer）、异常类型（IllegalStateException）、异常消息。

### 2. AuthController.changePassword() — 方法重构

**修改前**：
```java
@PutMapping("/password")
public Result<Void> changePassword(
        @RequestHeader("Authorization") String authHeader,
        @Valid @RequestBody PasswordChangeRequest request) {
    String token = extractToken(authHeader);
    Claims claims = jwtTokenProvider.validateToken(token, null);
    Long userId = jwtTokenProvider.getUserIdFromClaims(claims);
    authService.changePassword(userId, request.oldPassword(), request.newPassword());
    return Result.success(null);
}
```

**修改后**：
```java
@PutMapping("/password")
public Result<Void> changePassword(
        @Valid @RequestBody PasswordChangeRequest request) {
    Long userId = getCurrentUserId();
    authService.changePassword(userId, request.oldPassword(), request.newPassword());
    return Result.success(null);
}
```

**变更清单**：
- 删除 `@RequestHeader("Authorization") String authHeader` 参数
- 删除 `String token = extractToken(authHeader);`
- 删除 `Claims claims = jwtTokenProvider.validateToken(token, null);`
- 删除 `Long userId = jwtTokenProvider.getUserIdFromClaims(claims);`
- 新增 `Long userId = getCurrentUserId();`

### 3. AuthController — 删除字段和构造参数

**修改前**：
```java
private final AuthService authService;
private final JwtTokenProvider jwtTokenProvider;

public AuthController(AuthService authService, JwtTokenProvider jwtTokenProvider) {
    this.authService = authService;
    this.jwtTokenProvider = jwtTokenProvider;
}
```

**修改后**：
```java
private final AuthService authService;

public AuthController(AuthService authService) {
    this.authService = authService;
}
```

**变更清单**：
- 删除 `private final JwtTokenProvider jwtTokenProvider;` 字段（L30）
- 删除构造参数 `JwtTokenProvider jwtTokenProvider`
- 删除 `this.jwtTokenProvider = jwtTokenProvider;` 赋值（L34）

### 4. AuthController — import 变更

**删除的 import**：
- `com.aimedical.modules.commonmodule.auth.jwt.JwtTokenProvider`（L4）
- `io.jsonwebtoken.Claims`（L14）

**新增的 import**：
- `org.springframework.security.core.Authentication`
- `org.springframework.security.core.context.SecurityContextHolder`

### 5. AuthControllerTest — 字段和构造变更

**删除字段**（L37-L38）：
```java
@Mock
private JwtTokenProvider jwtTokenProvider;
```

**修改 setUp()**（L47）：
```java
// 修改前
authController = new AuthController(authService, jwtTokenProvider);
// 修改后
authController = new AuthController(authService);
```

### 6. AuthControllerTest — import 变更

**删除的 import**：
- `com.aimedical.modules.commonmodule.auth.jwt.JwtTokenProvider`（L6）
- `io.jsonwebtoken.Claims`（L15）

**新增的 import**：
- `org.springframework.security.core.Authentication`
- `org.springframework.security.core.context.SecurityContext`
- `org.springframework.security.core.context.SecurityContextHolder`

### 7. AuthControllerTest.ChangePasswordTests — 测试重构

**修改前**（L248-L262）：
```java
@Test
@DisplayName("密码修改端点返回SUCCESS")
void shouldReturnSuccessForChangePassword() {
    Claims claims = mock(Claims.class);
    when(jwtTokenProvider.validateToken(anyString(), isNull())).thenReturn(claims);
    when(jwtTokenProvider.getUserIdFromClaims(claims)).thenReturn(1L);
    doNothing().when(authService).changePassword(anyLong(), anyString(), anyString());

    var request = new PasswordChangeRequest("oldPass", "newPass123");
    Result<Void> result = authController.changePassword("Bearer mock-token", request);

    assertEquals("SUCCESS", result.getCode());
    verify(authService).changePassword(1L, "oldPass", "newPass123");
}
```

**修改后**：
```java
@Test
@DisplayName("密码修改端点返回SUCCESS")
void shouldReturnSuccessForChangePassword() {
    SecurityContext securityContext = mock(SecurityContext.class);
    Authentication authentication = mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(1L);
    SecurityContextHolder.setContext(securityContext);

    doNothing().when(authService).changePassword(anyLong(), anyString(), anyString());

    var request = new PasswordChangeRequest("oldPass", "newPass123");
    Result<Void> result = authController.changePassword(request);

    assertEquals("SUCCESS", result.getCode());
    verify(authService).changePassword(1L, "oldPass", "newPass123");
}
```

**变更清单**：
- 删除 `Claims claims = mock(Claims.class);` 和 JwtTokenProvider mock 设置
- 新增 SecurityContext + Authentication mock，设置 principal=1L，注入 SecurityContextHolder
- 删除 `changePassword("Bearer mock-token", request)` 调用中的 authHeader 参数，改为 `changePassword(request)`
- 验证逻辑不变（验证 `authService.changePassword(1L, "oldPass", "newPass123")` 被调用）

## 错误处理

- `getCurrentUserId()` 在 principal 类型不匹配时抛出 `IllegalStateException("无法从SecurityContext获取用户ID")` — 与 MenuController 一致
- 正常调用路径（JwtAuthenticationFilter 已设置 SecurityContext），principal 为 Long 类型，不会触发异常
- `changePassword()` 在原异常路径不变（`BusinessException` 由 `authService.changePassword()` 抛出）
- 测试中未验证 `IllegalStateException` 场景（与 MenuController 测试策略一致，不测试 SecurityContext 为空时的异常）

## 行为契约

### T6 — updateMe() 路径变更

- **变更**：`@PutMapping("/me")` → `@PutMapping("/profile")`
- **端点映射**：`PUT /api/auth/me` → `PUT /api/auth/profile`
- **方法名**：`updateMe()` 不变
- **方法体**：不变
- **GET 端点**：`@GetMapping("/me")` 不变（即 GET /api/auth/me 保持不变）
- **影响范围**：仅注解字符串变更，无编译期或运行期行为变化

### T7 — changePassword() SecurityContext 重构

- **前置条件**：JwtAuthenticationFilter 已对 `/api/auth/password` 路径执行过滤，在 SecurityContext 中设置了 Authentication（principal = userId Long）
- **方法签名变更**：`changePassword(@RequestHeader String, @RequestBody)` → `changePassword(@RequestBody)`
- **新增调用链**：`SecurityContextHolder.getContext().getAuthentication().getPrincipal()` → `getCurrentUserId()`
- **后置条件**：userId 从 SecurityContext 获取，不再依赖请求头中的 token
- **AuthController 不再注入 JwtTokenProvider**：JwtTokenProvider 不再作为 AuthController 的依赖

## 依赖关系

| 依赖 | 说明 |
|------|------|
| `SecurityContextHolder.getContext()` | Spring Security 静态工具类，新增使用 |
| `Authentication.getPrincipal()` | Spring Security 接口，新增使用 |
| `AuthService.changePassword(Long, String, String)` | 已有依赖，签名不变 |
| `extractToken(String)` | 保留为私有方法，其他方法（logout、me、updateMe）仍使用 |
| `JwtTokenProvider` | 删除，AuthController 不再依赖 |

**不受影响的文件**：
| 文件 | 原因 |
|------|------|
| `AuthService.java` | 接口签名不变 |
| `AuthServiceImpl.java` | 实现不变 |
| `JwtAuthenticationFilter.java` | 已覆盖 `/api/auth/password` 路径 |
| `SecurityConfigPhase1.java` | 路径保护 `/api/auth/**` 已到位 |

## 方法签名说明

| 方法 | 签名 | 来源 |
|------|------|------|
| `getCurrentUserId()` | `private Long getCurrentUserId()` | AuthController 新增私有方法 |
| `changePassword(PasswordChangeRequest)` | `public Result<Void> changePassword(@Valid @RequestBody PasswordChangeRequest)` | 重构后的公开方法（删除 authHeader 参数） |
| `updateMe(ProfileUpdateRequest)` | `public Result<UserInfoResponse> updateMe(@RequestHeader String, @Valid @RequestBody ProfileUpdateRequest)` | 注解变更（/me → /profile），方法签名不变 |

## 修订说明（v9 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| AuthControllerTest import 变更缺少 SecurityContextHolder 导入 | 在"类型定义 6"的新增 import 列表中添加 `org.springframework.security.core.context.SecurityContextHolder`，与测试代码中 `SecurityContextHolder.setContext(securityContext)` 调用匹配 |
