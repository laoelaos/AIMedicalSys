# 详细设计（v10）

## 概述

实现 `PasswordChangeCheckFilter`（OncePerRequestFilter），位于 `common-module-impl/auth/security/` 包下，在 JwtAuthenticationFilter 之后检查用户 `passwordChangeRequired` 状态，对白名单之外的 API 抛出 `PasswordChangeRequiredException`，由 RestAccessDeniedHandler 返回 403 + PASSWORD_CHANGE_REQUIRED。

## 文件规划

| 文件路径（相对 `AIMedical/backend/`） | 操作 | 职责 |
|---------|------|------|
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/PasswordChangeCheckFilter.java` | 新建 | 密码变更检查过滤器 |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/PasswordChangeCheckFilterTest.java` | 新建 | 7 个单元测试，无 Spring 上下文 |

## 类型定义

### PasswordChangeCheckFilter

**形态**：public class
**包路径**：`com.aimedical.modules.commonmodule.auth.security`
**职责**：OncePerRequestFilter 实现，在 JwtAuthenticationFilter 之后检查 `passwordChangeRequired` request attribute，对白名单之外且需要修改密码的 API 返回 403 + PASSWORD_CHANGE_REQUIRED。

```java
package com.aimedical.modules.commonmodule.auth.security;

import com.aimedical.modules.commonmodule.auth.exception.PasswordChangeRequiredException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class PasswordChangeCheckFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(PasswordChangeCheckFilter.class);

    private final AntPathRequestMatcher passwordMatcher;
    private final AntPathRequestMatcher logoutMatcher;
    private final AntPathRequestMatcher refreshMatcher;

    PasswordChangeCheckFilter() {
        this.passwordMatcher = new AntPathRequestMatcher("/api/auth/password", "PUT");
        this.logoutMatcher = new AntPathRequestMatcher("/api/auth/logout", "POST");
        this.refreshMatcher = new AntPathRequestMatcher("/api/auth/refresh", "POST");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // 见行为契约
    }

    private boolean isWhitelisted(HttpServletRequest request) {
        return passwordMatcher.matches(request)
                || logoutMatcher.matches(request)
                || refreshMatcher.matches(request);
    }
}
```

**公开接口**：

| 方法签名 | 返回 | 说明 |
|---------|------|------|
| `PasswordChangeCheckFilter()` | 构造器 | 包级私有，初始化三个 AntPathRequestMatcher 实例 |
| `void doFilterInternal(HttpServletRequest, HttpServletResponse, FilterChain)` | void | `@Override`，密码变更检查流程 |

**私有辅助方法**：

| 方法签名 | 返回 | 说明 |
|---------|------|------|
| `isWhitelisted(HttpServletRequest)` | `boolean` | 依次调用三个 AntPathRequestMatcher.matches(request)，任一匹配返回 true |

**构造方式**：`new PasswordChangeCheckFilter()`，由 SecurityConfigPhase1 通过 `addFilterAfter(filter, JwtAuthenticationFilter.class)` 注册。
**类型关系**：继承 `org.springframework.web.filter.OncePerRequestFilter`。

## 错误处理

| 场景 | 处理方式 |
|------|---------|
| SecurityContext 无 Authentication 或未认证 | chain.doFilter 放行（由下游 ExceptionTranslationFilter 处理） |
| passwordChangeRequired attribute 为 false 或缺失 | chain.doFilter 放行 |
| 请求路径在白名单中 | chain.doFilter 放行 |
| passwordChangeRequired=true 且不在白名单中 | SecurityContextHolder.clearContext()，抛出 `PasswordChangeRequiredException("Password change required")`，由 RestAccessDeniedHandler 捕获返回 403 + PASSWORD_CHANGE_REQUIRED |

## 行为契约

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

**契约要点**：
- **无依赖检查**：Filter 无需注入任何依赖，构造器无参数
- **静默跳过**：步骤 1（无认证）、2（无需改密）、3（白名单路径）均直接 `chain.doFilter` 放行
- **阻断行为**：步骤 4 先清除 SecurityContext（确保后续无法绕过认证），再抛出 `PasswordChangeRequiredException`
- **异常的传播**：`PasswordChangeRequiredException` 继承 `AccessDeniedException`，由注册在 SecurityConfig 中的 `RestAccessDeniedHandler.handle()` 捕获，返回 403 + `PASSWORD_CHANGE_REQUIRED`
- **白名单使用 AntPathRequestMatcher**：三个 matcher 在构造器中初始化，每个绑定具体 HTTP method，通过 `matches(request)` 进行精确匹配，而非字符串路径比较

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
- `org.springframework.mock.web.MockHttpServletRequest`
- `org.springframework.mock.web.MockHttpServletResponse`
- `org.mockito.Mockito` — mock FilterChain
- `org.springframework.security.core.context.SecurityContextHolder` — 上下文校验
- `org.springframework.security.authentication.UsernamePasswordAuthenticationToken` — 设置认证

### 已有关联类型
- `JwtAuthenticationFilter` — 前序 Filter，负责设置 `passwordChangeRequired` attribute
- `RestAccessDeniedHandler` — 已注册，能识别 `PasswordChangeRequiredException` 返回 403 + PASSWORD_CHANGE_REQUIRED
- `PasswordChangeRequiredException` — 已存在于 `com.aimedical.modules.commonmodule.auth.exception`
- `GlobalErrorCode.PASSWORD_CHANGE_REQUIRED` — RestAccessDeniedHandler 中已引用

## 单元测试设计

### PasswordChangeCheckFilterTest

**形态**：class（JUnit 5），无 Spring 上下文
**包路径**：`com.aimedical.modules.commonmodule.auth.security.PasswordChangeCheckFilterTest`
**注解**：无（package-private class）

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

| # | 测试方法 | 场景设置 | 验证点 |
|---|---------|---------|--------|
| 1 | `shouldSkipWhenNoAuthentication` | SecurityContext 无 Authentication | `chain.doFilter` 被调用 |
| 2 | `shouldSkipWhenPasswordChangeRequiredFalse` | attribute=false | `chain.doFilter` 被调用 |
| 3 | `shouldSkipWhenNoAttribute` | attribute 缺失 | `chain.doFilter` 被调用 |
| 4 | `shouldSkipForPasswordPath` | attr=true, path=/api/auth/password, method=PUT | `chain.doFilter` 被调用 |
| 5 | `shouldSkipForLogoutPath` | attr=true, path=/api/auth/logout, method=POST | `chain.doFilter` 被调用 |
| 6 | `shouldSkipForRefreshPath` | attr=true, path=/api/auth/refresh, method=POST | `chain.doFilter` 被调用 |
| 7 | `shouldThrowPasswordChangeRequiredForOtherPaths` | attr=true, path=/api/auth/me, method=GET | 抛出 `PasswordChangeRequiredException`，SecurityContext 被清除 |

**测试关键细节**：
- 所有用例使用 `MockHttpServletRequest` + `MockHttpServletResponse`，`FilterChain` 用 `mock(FilterChain.class)`
- 需设置认证的用例：`SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(1L, null, List.of()))`
- 设置 request attribute：`request.setAttribute("passwordChangeRequired", true)`
- 用例 7 验证异常类型和 SecurityContext cleared：`assertThrows(PasswordChangeRequiredException.class, ...)` + `assertNull(SecurityContextHolder.getContext().getAuthentication())`
- 用例 4/5/6 需设置 `request.setRequestURI("/api/auth/password")` 和 `request.setMethod("PUT")` 等
- `@AfterEach tearDown` 清理 SecurityContext，确保用例间隔离

**测试模板**（用例 4）：

```java
@Test
void shouldSkipForPasswordPath() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/auth/password");
    request.setMethod("PUT");
    request.setAttribute("passwordChangeRequired", true);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(1L, null, List.of()));

    filter.doFilterInternal(request, response, chain);

    verify(chain).doFilter(request, response);
}
```

**测试模板**（用例 7）：

```java
@Test
void shouldThrowPasswordChangeRequiredForOtherPaths() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/auth/me");
    request.setMethod("GET");
    request.setAttribute("passwordChangeRequired", true);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(1L, null, List.of()));

    assertThrows(PasswordChangeRequiredException.class,
            () -> filter.doFilterInternal(request, response, chain));
    assertNull(SecurityContextHolder.getContext().getAuthentication());
}
```

### 暴露给后续任务的公开接口
- `PasswordChangeCheckFilter()` — 包级私有构造器，供 SecurityConfigPhase1（后续任务）使用 `addFilterAfter(new PasswordChangeCheckFilter(), JwtAuthenticationFilter.class)` 注册
