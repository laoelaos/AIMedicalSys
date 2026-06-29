# 详细设计（v17）

## 概述

重构 `AuthController`（`common-module-impl`），移除对 `JwtUtil` 的编译期依赖，将 token 提取逻辑内联，简化构造函数签名。

## 文件规划

| 文件路径（相对 `AIMedical/backend/`） | 操作 | 职责 |
|---------|------|------|
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/controller/AuthController.java` | 修改 | 移除 `JwtUtil` 字段；内联 `extractToken()`；简化构造函数 |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/controller/AuthControllerTest.java` | 修改 | 移除 `JwtUtil` 相关代码；适配新构造函数 |

## 类型定义

### AuthController

**形态**：`@RestController` class
**包路径**：`com.aimedical.modules.commonmodule.controller`
**职责**：认证相关 REST API 入口。

```java
package com.aimedical.modules.commonmodule.controller;

import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.auth.jwt.JwtTokenProvider;
import com.aimedical.modules.commonmodule.auth.UserInfoResponse;
import com.aimedical.modules.commonmodule.dto.request.LoginRequest;
import com.aimedical.modules.commonmodule.dto.request.PasswordChangeRequest;
import com.aimedical.modules.commonmodule.dto.request.ProfileUpdateRequest;
import com.aimedical.modules.commonmodule.dto.request.RefreshTokenRequest;
import com.aimedical.modules.commonmodule.dto.response.LoginResponse;
import com.aimedical.modules.commonmodule.dto.response.TokenRefreshResponse;
import com.aimedical.modules.commonmodule.service.AuthService;

import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthService authService, JwtTokenProvider jwtTokenProvider) {
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) { /* 不变 */ }

    @PostMapping("/logout")
    public Result<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) RefreshTokenRequest refreshTokenRequest) { /* 不变 */ }

    @PostMapping("/refresh")
    public Result<TokenRefreshResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) { /* 不变 */ }

    @GetMapping("/me")
    public Result<UserInfoResponse> me(@RequestHeader(value = "Authorization", required = false) String authHeader) { /* 不变 */ }

    @PutMapping("/me")
    public Result<UserInfoResponse> updateMe(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody ProfileUpdateRequest request) { /* 不变 */ }

    @PutMapping("/password")
    public Result<Void> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody PasswordChangeRequest request) { /* 不变 */ }

    private String extractToken(String authHeader) {
        if (authHeader == null || authHeader.isEmpty()) return null;
        if (authHeader.startsWith("Bearer ")) return authHeader.substring(7);
        return null;
    }
}
```

**变更明细**：
1. **移除** `JwtUtil jwtUtil` 字段声明
2. **构造函数**从 `AuthController(AuthService, JwtUtil, JwtTokenProvider)` 简化为 `AuthController(AuthService, JwtTokenProvider)`
3. **移除** `import com.aimedical.modules.commonmodule.jwt.JwtUtil;`
4. **`extractToken()` 方法内联**：原为 `JwtUtil.extractToken(authHeader, jwtUtil.getTokenType())`，改为直接使用字符串 `"Bearer "` 进行前缀匹配和截取

**保留不变**：所有 API 端点（`login`/`logout`/`refresh`/`me`/`updateMe`/`changePassword`）签名、`JwtTokenProvider` 字段及其在 `changePassword` 中的使用。

---

### AuthControllerTest

**形态**：class（JUnit 5 + Mockito）
**包路径**：`com.aimedical.modules.commonmodule.controller`

**变更明细**：
1. **移除 import**：`import com.aimedical.modules.commonmodule.jwt.JwtConfig;` 和 `import com.aimedical.modules.commonmodule.jwt.JwtUtil;`
2. **移除字段**：`private JwtUtil jwtUtil;`
3. **移除 `@BeforeEach` 中 `JwtConfig` 创建和 `new JwtUtil(jwtConfig)` 逻辑**
4. **构造函数调用改为**：`new AuthController(authService, jwtTokenProvider)`
5. **保留**：`@Mock JwtTokenProvider jwtTokenProvider`、`@Mock AuthService authService`、所有现有 11 个测试用例

**测试类结构要点**：
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController测试")
class AuthControllerTest {

    @Mock private AuthService authService;
    @Mock private JwtTokenProvider jwtTokenProvider;

    private AuthController authController;
    private LoginResponse mockLoginResponse;
    private UserInfoResponse mockUserInfo;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authService, jwtTokenProvider);

        mockUserInfo = new UserInfoResponse(1L, "testuser", "测试用户",
                null, null, "DOCTOR", null, Set.of());

        mockLoginResponse = new LoginResponse(1L, "testuser", "mock-jwt-token", null,
                "Bearer", 86400L, false, mockUserInfo);
    }

    // 以下 11 个测试用例保持不变（LoginTests×2, LogoutTests×2, RefreshTests×2,
    // MeTests×2, UpdateMeTests×2, ChangePasswordTests×1）
}
```

## 错误处理

无变更。`BusinessException` 由 `GlobalExceptionHandler` 统一处理，`extractToken()` 内联后行为与原实现完全一致（null/空 → null；不以 "Bearer " 开头 → null；匹配 → 截取后半部分返回）。

## 行为契约

### extractToken(String)

| 输入 | 输出 | 说明 |
|------|------|------|
| `null` | `null` | null 输入 |
| `""` | `null` | 空字符串 |
| `"Bearer abc123"` | `"abc123"` | 标准格式 |
| `"Basic abc123"` | `null` | 非 Bearer 开头 |
| `"Bearer "` | `""` | 仅有前缀（返回空字符串） |

内联实现与原 `JwtUtil.extractToken(authHeader, "Bearer")` 行为完全一致。

## 依赖关系

### AuthController 依赖

| 类型 | 方式 | 说明 |
|------|------|------|
| `AuthService` | 构造器注入 | 保留 |
| `JwtTokenProvider` | 构造器注入 | 保留 |
| ~~`JwtUtil`~~ | ~~构造器注入~~ | **已移除** |

### AuthControllerTest 依赖

| 类型 | 方式 | 说明 |
|------|------|------|
| `AuthService` | `@Mock` | 保留 |
| `JwtTokenProvider` | `@Mock` | 保留 |
| ~~`JwtUtil`~~ | ~~直接构造~~ | **已移除** |
| ~~`JwtConfig`~~ | ~~临时对象~~ | **已移除** |

## 测试验证

所有 11 个现有 `AuthControllerTest` 测试用例应在变更后保持通过。测试命令：

```bash
mvn test -pl modules/common-module/common-module-impl -am \
  -Dtest="AuthControllerTest" -Dsurefire.failIfNoSpecifiedTests=false
```
