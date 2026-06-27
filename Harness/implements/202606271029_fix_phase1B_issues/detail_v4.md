# 详细设计（v4）

## 概述

修复 T11：去除 `AuthService.getCurrentUser(String token)` 的 token 参数，改为 `getCurrentUser(Long userId)`，由 Controller 从 SecurityContext 获取 userId 后传入。Service 层不再重新解析 JWT。涉及 5 个文件的修改。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/AuthService.java` | 修改 | `getCurrentUser(String token)` → `getCurrentUser(Long userId)` |
| `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/impl/AuthServiceImpl.java` | 修改 | 移除 JWT 解析逻辑，直接通过 userId 查询用户 |
| `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/controller/AuthController.java` | 修改 | `me()` 改用 `getCurrentUserId()` 获取 userId 后传入 Service |
| `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/service/AuthServiceTest.java` | 修改 | `getCurrentUser_shouldSucceed()` 适配新签名 |
| `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/controller/AuthControllerTest.java` | 修改 | `MeTests` 适配新签名和 SecurityContext 方式 |

## 类型定义

### AuthService 接口变更

**形态**：interface 方法签名变更
**文件**：`AuthService.java`

**变更**：
```
- UserInfoResponse getCurrentUser(String token);
+ UserInfoResponse getCurrentUser(Long userId);
```

### AuthServiceImpl.getCurrentUser() 实现变更

**形态**：方法体重构
**文件**：`AuthServiceImpl.java:307-318`

**修改前**：
```
getCurrentUser(String token):
  1. Claims claims = jwtTokenProvider.validateToken(token, null)
  2. if claims == null → throw BusinessException(UNAUTHORIZED, "令牌无效")
  3. Long userId = jwtTokenProvider.getUserIdFromClaims(claims)
  4. User user = userRepository.findById(userId) → orElseThrow(NOT_FOUND, "用户不存在")
  5. return userConverter.toUserInfoResponse(user)
```

**修改后**：
```
getCurrentUser(Long userId):
  1. User user = userRepository.findById(userId) → orElseThrow(NOT_FOUND, "用户不存在")
  2. return userConverter.toUserInfoResponse(user)
```

**移除的依赖**：`jwtTokenProvider.validateToken`、`jwtTokenProvider.getUserIdFromClaims`

### AuthController.me() 签名变更

**形态**：Controller 方法重构
**文件**：`AuthController.java:59-67`

**修改前**：
```java
@GetMapping("/me")
public Result<UserInfoResponse> me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
    String token = extractToken(authHeader);
    if (token == null) {
        return Result.fail("UNAUTHORIZED", "未提供令牌");
    }
    UserInfoResponse response = authService.getCurrentUser(token);
    return Result.success(response);
}
```

**修改后**：
```java
@GetMapping("/me")
public Result<UserInfoResponse> me() {
    Long userId = getCurrentUserId();
    UserInfoResponse response = authService.getCurrentUser(userId);
    return Result.success(response);
}
```

**说明**：
- 不再接收 `Authorization` header 参数——请求认证由 `JwtAuthenticationFilter` 保障
- 与 `changePassword()` 保持一致模式：调用 `getCurrentUserId()` 获取 userId
- 原有的 null token 守卫逻辑移除（JwtAuthenticationFilter 已过滤未认证请求）
- 无需 import `RequestHeader`

### AuthServiceTest.getCurrentUser_shouldSucceed() 变更

**形态**：测试方法重构
**文件**：`AuthServiceTest.java:754-767`

**修改前**：
```
准备：
  - mock jwtTokenProvider.validateToken("token", null) → claims
  - mock jwtTokenProvider.getUserIdFromClaims(claims) → 1L
  - mock userRepository.findById(1L) → Optional.of(testUser)
  - mock userConverter.toUserInfoResponse(testUser) → UserInfoResponse

执行：authService.getCurrentUser("token")
验证：response.id == 1L
```

**修改后**：
```
准备：
  - mock userRepository.findById(1L) → Optional.of(testUser)
  - mock userConverter.toUserInfoResponse(testUser) → UserInfoResponse

执行：authService.getCurrentUser(1L)
验证：response.id == 1L
```

**说明**：移除所有 `jwtTokenProvider` mock，不再需要 `Claims claims = mock(Claims.class)`

### AuthControllerTest.MeTests 变更

**形态**：测试类重构
**文件**：`AuthControllerTest.java` — `MeTests` 嵌套类

| 测试方法 | 操作 | 说明 |
|---------|------|------|
| `shouldReturnSuccessWhenGetCurrentUserSucceeds` | **修改** | 移除 `authService.getCurrentUser("mock-token")` mock，改为 `authService.getCurrentUser(1L)`；添加 `SecurityContextHolder` mock（设 `principal=1L`），同 `ChangePasswordTests` 模式 |
| `shouldReturnUnauthorizedWhenNoToken` | **删除** | 不再有 token 守卫逻辑，测试不适用 |
| `shouldReturnUnauthorizedWhenEmptyAuthHeader` | **删除** | 同上 |
| `shouldReturnUnauthorizedWhenNonBearerAuthHeader` | **删除** | 同上 |
| `shouldCallServiceWithEmptyTokenWhenBearerOnlyPrefix` | **删除** | 同上 |

**修改后保留的测试**：仅 `shouldReturnSuccessWhenGetCurrentUserSucceeds`，需要：
- mock SecurityContext → Authentication → getPrincipal() → 1L
- `when(authService.getCurrentUser(1L)).thenReturn(mockUserInfo)`
- 调用 `authController.me()`（无参数）
- 验证 `result.getData().username()` 为 `"testuser"`
- `verify(authService).getCurrentUser(1L)`

**新增 import**：如果未在测试文件中，可能需要 `import org.springframework.security.core.Authentication;` 和 `import org.springframework.security.core.context.SecurityContext;`

## 错误处理

- `getCurrentUser(Long userId)` 中 `userId` 为 null 时，`userRepository.findById(null)` 返回 `Optional.empty()`，将抛出 `BusinessException(NOT_FOUND, "用户不存在")`
- 若 `SecurityContext` 中无 Authentication 或 principal 类型非 Long/Integer，`getCurrentUserId()` 抛出 `IllegalStateException`
- 不存在用户时抛出 `BusinessException(GlobalErrorCode.NOT_FOUND, "用户不存在")`，与修改前一致

## 行为契约

- **前置条件**：调用 `AuthController.me()` 前需已通过 JwtAuthenticationFilter 认证，SecurityContext 中存在 Authentication
- **getCurrentUser(userId)**：
  - 不再验证 token 有效性（由 Filter 层保障）
  - userId 非 null 且对应用户存在时返回 UserInfoResponse
  - 用户不存在时抛出 BusinessException(NOT_FOUND)
- **Controller.me()**：不再处理 null token 分支（JwtAuthenticationFilter 已拒绝未认证请求）
- **安全影响**：消除了 Service 层二次 JWT 解析风险（Filter 拒绝但 Service 放行 或 反向不一致）

## 依赖关系

- 依赖（已有）：`userRepository.findById(Long)`、`UserConverter.toUserInfoResponse(User)`
- 依赖（已有）：`SecurityContextHolder.getContext().getAuthentication()`（Controller 层）
- **移除的依赖**：`jwtTokenProvider.validateToken(String, null)`、`jwtTokenProvider.getUserIdFromClaims(Claims)`
- 无新增外部依赖
