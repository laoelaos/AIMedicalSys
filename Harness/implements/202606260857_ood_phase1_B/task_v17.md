# 任务指令（v17）

## 动作
NEW

## 任务描述
重构 `AuthController`（位于 `common-module-impl/controller/`）移除对 `JwtUtil` 的编译期依赖，将 token 提取逻辑内联，简化构造函数签名。

**修改文件**：
1. `modules/common-module/common-module-impl/src/main/java/.../controller/AuthController.java`
   - 移除 `JwtUtil` 字段声明和构造函数参数
   - 内联 `extractToken()` 方法：直接使用 `"Bearer"` 作为 token type，实现提取逻辑（null/空校验 → 以 "Bearer " 开头 → 截取后半部分 → 返回）
   - 构造函数简化为 `AuthController(AuthService authService, JwtTokenProvider jwtTokenProvider)`
   - `jwtTokenProvider` 保留（changePassword 端点仍需解析 token 提取 userId）

2. `modules/common-module/common-module-impl/src/test/java/.../controller/AuthControllerTest.java`
   - 移除 `JwtUtil` 相关 import、字段声明、`@BeforeEach` 中构造逻辑
   - 构造函数调用改为 `new AuthController(authService, jwtTokenProvider)`
   - 保留 `JwtTokenProvider` mock 和所有现有测试用例

## 选择理由
- 对应 OOD 12 节任务分解 3.7（AuthController 重构，移除 JwtUtil 依赖），是 Stage 3 最后未完成的编码任务
- R16 完成了 AuthServiceImpl 全量重组，AuthController 是最后一个需要集成的 Controller
- AuthController 当前 3 个构造函数参数中，JwtUtil 仅用于获取 tokenType（"Bearer"）和调用静态 extractToken 方法，可简单内联替换
- 移除后构造函数签名简化为 2 参数，减少不必要的依赖注入

## 任务上下文
### 当前代码结构
```java
// AuthController.java — 当前构造函数和 extractToken
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthService authService, JwtUtil jwtUtil, JwtTokenProvider jwtTokenProvider) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    private String extractToken(String authHeader) {
        return JwtUtil.extractToken(authHeader, jwtUtil.getTokenType());
    }
}
```

### 目标代码结构
```java
// AuthController.java — 重构后
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthService authService, JwtTokenProvider jwtTokenProvider) {
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || authHeader.isEmpty()) return null;
        if (authHeader.startsWith("Bearer ")) return authHeader.substring(7);
        return null;
    }
}
```

### 其他注意事项
- `JwtUtil` 类本身保留不变（`JwtAuthenticationFilter` 和 `SecurityConfigPhase1` 仍依赖它）
- `JwtTokenProvider` 依赖保留（changePassword 端点使用 `jwtTokenProvider.validateToken()` 和 `getUserIdFromClaims()`）
- 所有现有 API 端点签名不变：`login`/`logout`/`refresh`/`me`/`updateMe`/`changePassword`
- `AuthControllerTest` 中原有 11 个测试用例应全部保持通过

## 已有代码上下文
- `AuthController.java` 路径：`.../common-module-impl/src/main/java/.../controller/AuthController.java`
- `AuthControllerTest.java` 路径：`.../common-module-impl/src/test/java/.../controller/AuthControllerTest.java`
- `JwtUtil.extractToken()` 静态方法逻辑：
  ```java
  public static String extractToken(String authHeader, String tokenType) {
      if (authHeader == null || authHeader.isEmpty()) return null;
      if (authHeader.startsWith(tokenType + " ")) return authHeader.substring(tokenType.length() + 1);
      return null;
  }
  ```
- `jwtUtil.getTokenType()` 返回 `jwtConfig.getTokenType()` = `"Bearer"`
- 当前 AuthController 测试使用 `JwtUtil jwtUtil = new JwtUtil(jwtConfig)` 创建实例传入构造函数
