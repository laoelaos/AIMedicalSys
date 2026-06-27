# 任务指令（v10）

## 动作
NEW

## 任务描述
实现 `PasswordChangeCheckFilter`（OncePerRequestFilter），位于 `common-module-impl/auth/security/` 包下，用于在 JwtAuthenticationFilter 之后检查用户 `passwordChangeRequired` 状态，对白名单之外的 API 返回 403 + PASSWORD_CHANGE_REQUIRED。

### 预期文件路径（相对 `AIMedical/backend/`）
1. **新建** `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/PasswordChangeCheckFilter.java`
2. **新建** `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/PasswordChangeCheckFilterTest.java`

## 选择理由
- PasswordChangeCheckFilter 是阶段 2 Filter 链的最后一环，依赖 JwtAuthenticationFilter（R9 ✅）设置的 `passwordChangeRequired` request attribute
- 所有依赖已就位：JwtAuthenticationFilter（R9 ✅ 输出 passwordChangeRequired attribute）、PasswordChangeRequiredException（R8 ✅）、RestAccessDeniedHandler（R8 ✅ 识别 PasswordChangeRequiredException 返回 403 + PASSWORD_CHANGE_REQUIRED）
- SecurityConfigPhase1（后续任务）需要所有 Filter 就位后才能统一注册

## 任务上下文

### 类型定义

```java
package com.aimedical.modules.commonmodule.auth.security;

public class PasswordChangeCheckFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(PasswordChangeCheckFilter.class);

    // 包级私有构造器，无 @Component
    PasswordChangeCheckFilter() {
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // 见行为契约
    }

    private boolean isWhitelisted(HttpServletRequest request) {
        // 使用 AntPathRequestMatcher 匹配白名单路径
    }
}
```

### 行为契约

```
PasswordChangeCheckFilter.doFilterInternal(request, response, chain):
  1. Authentication auth = SecurityContextHolder.getContext().getAuthentication()
     if (auth == null || !auth.isAuthenticated())
         → chain.doFilter(request, response); return

  2. Object passwordChangeRequired = request.getAttribute("passwordChangeRequired")
     if (!Boolean.TRUE.equals(passwordChangeRequired))
         → chain.doFilter(request, response); return

  3. if (isWhitelisted(request))
         → chain.doFilter(request, response); return

  4. log.warn("Password change required for userId={}, blocking request: {} {}",
         auth.getPrincipal(), request.getMethod(), request.getRequestURI())
     SecurityContextHolder.clearContext()
     throw new PasswordChangeRequiredException("Password change required")
```

**白名单路径**：
- `PUT /api/auth/password` — 允许修改密码
- `POST /api/auth/logout` — 允许登出
- `POST /api/auth/refresh` — 允许刷新令牌

### 构造方式
`new PasswordChangeCheckFilter()`，由 SecurityConfigPhase1（后续任务）通过 `addFilterAfter(filter, JwtAuthenticationFilter.class)` 注册。

### 关键设计决策
- 无 `@Component` 注解，由 SecurityConfigPhase1 显式注册
- 包级私有构造器
- 使用 `AntPathRequestMatcher` 进行路径匹配（`antPathRequestMatcher.matches(request)`），而非字符串路径比较
- 从 request attribute（Key="passwordChangeRequired"）读取状态，由 JwtAuthenticationFilter 步骤 8 写入
- 抛出 `PasswordChangeRequiredException`（扩展 `AccessDeniedException`，已有），由 RestAccessDeniedHandler（已有）捕获返回 403 + PASSWORD_CHANGE_REQUIRED

### ErrorCode 引用
`GlobalErrorCode.PASSWORD_CHANGE_REQUIRED` — RestAccessDeniedHandler 已实现识别逻辑：
```java
GlobalErrorCode errorCode = (accessDeniedException instanceof PasswordChangeRequiredException)
    ? GlobalErrorCode.PASSWORD_CHANGE_REQUIRED
    : GlobalErrorCode.FORBIDDEN;
```

### 已有代码上下文

**JwtAuthenticationFilter** (R9) 在步骤 8 写入 attribute：
```java
request.setAttribute("passwordChangeRequired", user.getPasswordChangeRequired());
```

**PasswordChangeRequiredException** (R8) 已存在：
```java
package com.aimedical.modules.commonmodule.auth.exception;
public class PasswordChangeRequiredException extends AccessDeniedException {
    public PasswordChangeRequiredException(String msg) { super(msg); }
    public PasswordChangeRequiredException(String msg, Throwable cause) { super(cause); }
}
```

**RestAccessDeniedHandler** (R8) 已存在，能识别 PasswordChangeRequiredException 并返回：
```json
{ "code": "PASSWORD_CHANGE_REQUIRED", "message": "需要修改密码", "data": null }
```

HttpServletResponse.SC_FORBIDDEN (403)。

### 单元测试设计

**测试夹具**：
```java
class PasswordChangeCheckFilterTest {
    private final PasswordChangeCheckFilter filter = new PasswordChangeCheckFilter();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }
}
```

**测试方法清单**（7 用例）：

| # | 测试方法 | 场景 | 验证点 |
|---|---------|------|--------|
| 1 | `shouldSkipWhenNoAuthentication` | SecurityContext 无 Authentication | `chain.doFilter` 被调用 |
| 2 | `shouldSkipWhenPasswordChangeRequiredFalse` | attribute=false | `chain.doFilter` 被调用 |
| 3 | `shouldSkipWhenNoAttribute` | attribute 缺失 | `chain.doFilter` 被调用 |
| 4 | `shouldSkipForPasswordPath` | attr=true, path=/api/auth/password, method=PUT | `chain.doFilter` 被调用 |
| 5 | `shouldSkipForLogoutPath` | attr=true, path=/api/auth/logout, method=POST | `chain.doFilter` 被调用 |
| 6 | `shouldSkipForRefreshPath` | attr=true, path=/api/auth/refresh, method=POST | `chain.doFilter` 被调用 |
| 7 | `shouldThrowPasswordChangeRequiredForOtherPaths` | attr=true, path=/api/auth/me | 抛出 `PasswordChangeRequiredException`，SecurityContext 被清除 |

### 单元测试编写要点
- 使用 `MockHttpServletRequest` + `MockHttpServletResponse`，`FilterChain` 用 mock
- 设置 SecurityContext：`SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(1L, null, List.of()))`
- 设置 request attribute：`request.setAttribute("passwordChangeRequired", true)`
- 用例 7 验证异常类型和 SecurityContext cleared：`assertThrows(PasswordChangeRequiredException.class, ...)` + `assertNull(SecurityContextHolder.getContext().getAuthentication())`
- `@AfterEach tearDown` 清理 SecurityContext

### 预期测试结果
7/7 或 8/8 单元测试全部通过（`mvn test -pl modules/common-module/common-module-impl -Dtest="com.aimedical.modules.commonmodule.auth.security.PasswordChangeCheckFilterTest"`）。

## 依赖关系

### 新增依赖（生产代码）
- `org.springframework.web.filter.OncePerRequestFilter` — 父类
- `org.springframework.security.core.Authentication` — SecurityContext 读取
- `org.springframework.security.core.context.SecurityContextHolder` — 上下文访问
- `org.springframework.security.web.util.matcher.AntPathRequestMatcher` — 白名单路径匹配
- `com.aimedical.modules.commonmodule.auth.exception.PasswordChangeRequiredException` — 阻断异常
- `jakarta.servlet.*` — Servlet API
- `org.slf4j.Logger` / `LoggerFactory` — 日志

### 新增依赖（测试代码）
- `org.junit.jupiter.api.Test` / `@AfterEach`
- `org.springframework.mock.web.MockHttpServletRequest` / `MockHttpServletResponse`
- `org.mockito.Mockito` — mock FilterChain
- `org.springframework.security.core.context.SecurityContextHolder`
- `org.springframework.security.authentication.UsernamePasswordAuthenticationToken`
