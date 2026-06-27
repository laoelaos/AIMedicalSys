# 详细设计（v9）

## 概述

将 `application` 模块的旧 `JwtAuthenticationFilter` 迁移重构至 `common-module-impl/auth/security/`，并按 OOD 3.3 行为契约大幅增强。新增 TokenBlacklist 校验、DB 用户状态验证、type claim 校验、passwordChangeRequired 传递、完整权限装配（roles+posts→functions）。同时为 `UserRepository` 补充 `@EntityGraph` 方法以支持懒加载关联。

## 文件规划

| 文件路径（相对 `AIMedical/backend/`） | 操作 | 职责 |
|---------|------|------|
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/JwtAuthenticationFilter.java` | 新建 | Filter 核心：Token 解析 → 黑名单校验 → DB 用户验证 → 权限装配 → SecurityContext 设置 |
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/UserRepository.java` | 修改 | 新增 `@EntityGraph` 方法 `findWithDetailsById` |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/JwtAuthenticationFilterTest.java` | 新建 | 9 个单元测试，无 Spring 上下文 |
| `application/src/main/java/com/aimedical/config/JwtAuthenticationFilter.java` | 删除 | 旧 Filter 文件迁移移除 |

## 类型定义

### JwtAuthenticationFilter

**形态**：public class
**包路径**：`com.aimedical.modules.commonmodule.auth.security`
**职责**：JWT 认证过滤器。验证请求中的 Authorization header，解析 token，执行黑名单校验、DB 用户状态验证，装配权限并设置 SecurityContext。无 `@Component`，由 SecurityConfigPhase1 显式注册。

**包路径**：`com.aimedical.modules.commonmodule.auth.security`

```java
package com.aimedical.modules.commonmodule.auth.security;

import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.modules.commonmodule.auth.blacklist.TokenBlacklist;
import com.aimedical.modules.commonmodule.jwt.JwtUtil;
import com.aimedical.modules.commonmodule.permission.PermissionFunction;
import com.aimedical.modules.commonmodule.permission.Post;
import com.aimedical.modules.commonmodule.permission.Role;
import com.aimedical.modules.commonmodule.permission.User;
import com.aimedical.modules.commonmodule.permission.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final TokenBlacklist tokenBlacklist;
    private final UserRepository userRepository;

    JwtAuthenticationFilter(JwtUtil jwtUtil, TokenBlacklist tokenBlacklist, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.tokenBlacklist = tokenBlacklist;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // ... 见行为契约
    }

    private String extractToken(String authHeader) {
        return JwtUtil.extractToken(authHeader, jwtUtil.getTokenType());
    }

    private String extractJti(Claims claims) {
        return claims.get("jti", String.class);
    }

    private Collection<SimpleGrantedAuthority> collectAuthorities(User user) {
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        for (Role role : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode()));
        }
        for (Post post : user.getPosts()) {
            for (PermissionFunction func : post.getFunctions()) {
                authorities.add(new SimpleGrantedAuthority("FUNC_" + func.getCode()));
            }
        }
        return authorities;
    }

    private void throwAccountDisabled(String message) {
        throw new AuthenticationException(message) {};
    }

    private Long extractUserId(Claims claims) {
        Object userId = claims.get("userId");
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        } else if (userId instanceof Long) {
            return (Long) userId;
        }
        return null;
    }
}
```

**公开接口**：

| 方法签名 | 返回 | 说明 |
|---------|------|------|
| `JwtAuthenticationFilter(JwtUtil, TokenBlacklist, UserRepository)` | 构造器 | 包级私有，三依赖构造器注入 |
| `void doFilterInternal(HttpServletRequest, HttpServletResponse, FilterChain)` | void | `@Override`，完整认证流程 |

**私有辅助方法**：

| 方法签名 | 返回 | 说明 |
|---------|------|------|
| `extractToken(String)` | `String` | 委托 `JwtUtil.extractToken(authHeader, jwtUtil.getTokenType())` |
| `extractJti(Claims)` | `String` | 从 claims 提取 jti（`claims.get("jti", String.class)`），可能为 null |
| `collectAuthorities(User)` | `Collection<SimpleGrantedAuthority>` | 遍历 roles + posts→functions，Set 去重 |
| `throwAccountDisabled(String)` | `void` | 抛出匿名 `AuthenticationException` 子类 |
| `extractUserId(Claims)` | `Long` | 从 claims 提取 userId，兼容 Integer/Long |

**构造方式**：`new JwtAuthenticationFilter(jwtUtil, tokenBlacklist, userRepository)`，由 SecurityConfigPhase1 的 @Bean 方法显式创建。
**类型关系**：继承 `org.springframework.web.filter.OncePerRequestFilter`。

### UserRepository（修改）

**形态**：interface（已有，追加方法）
**包路径**：`com.aimedical.modules.commonmodule.permission`

新增方法：

```java
import org.springframework.data.jpa.repository.EntityGraph;

@EntityGraph(attributePaths = {"roles", "posts"})
Optional<User> findWithDetailsById(Long id);
```

**职责**：带 `@EntityGraph` 的按 ID 查询，确保 `roles` 和 `posts` 关联在单次查询中加载，避免 `LazyInitializationException`。

## 行为契约

### JwtAuthenticationFilter.doFilterInternal 完整流程

```
输入：HttpServletRequest request, HttpServletResponse response, FilterChain chain
输出：void（直接操作 response 或 chain.doFilter）

算法（步骤编号对应 OOD 3.3）：
1. String authHeader = request.getHeader("Authorization")
   if (authHeader == null || authHeader.isEmpty())
       → chain.doFilter(request, response); return

2. String token = extractToken(authHeader)
   // token 为 null 表示 header 格式不正确（非 "Bearer xxx"）
   → 直接 chain.doFilter(request, response); return

3. Claims claims = jwtUtil.validateTokenAndGetClaims(token)
   a. if (claims == null)
          log.debug("JWT token validation failed")
          SecurityContextHolder.clearContext()
          chain.doFilter(request, response)
          return
   b. String tokenType = claims.get("type", String.class)
      if ("refresh".equals(tokenType))
          log.debug("Refresh token used as access token, rejected")
          SecurityContextHolder.clearContext()
          chain.doFilter(request, response)
          return

4. String jti = extractJti(claims)
   if (jti != null && tokenBlacklist.isBlacklisted(jti))
       log.debug("Token is blacklisted, jti={}", jti)
       SecurityContextHolder.clearContext()
       chain.doFilter(request, response)
       return

5. Long userId = extractUserId(claims)
   if (userId == null)
       log.debug("Failed to extract userId from claims")
       SecurityContextHolder.clearContext()
       chain.doFilter(request, response)
       return

6. Optional<User> userOpt = userRepository.findWithDetailsById(userId)
   if (userOpt.isEmpty())
       log.debug("User not found or deleted, userId={}", userId)
       SecurityContextHolder.clearContext()
       chain.doFilter(request, response)
       return
   User user = userOpt.get()

7. if (!user.getEnabled())
       log.warn("Account disabled, userId={}", userId)
       throwAccountDisabled(GlobalErrorCode.ACCOUNT_DISABLED.getMessage())
       // 由 AuthenticationEntryPoint 处理 401 响应

8. request.setAttribute("passwordChangeRequired", user.getPasswordChangeRequired())

9. Collection<SimpleGrantedAuthority> authorities = collectAuthorities(user)

10. UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(userId, null, authorities)
    SecurityContextHolder.getContext().setAuthentication(authentication)

11. chain.doFilter(request, response)
```

**契约要点**：
- **静默跳过**：步骤 1/2（无 token）、3a（token 无效）、3b（refresh token）、4（黑名单）、5（userId 缺失）、6（用户不存在/已删除）均不清除 SecurityContext（SecurityContextHolder 初始即为 null/empty）→ `clearContext()` 后 `chain.doFilter` 放行，由 ExceptionTranslationFilter 或 @PreAuthorize 触发 401
- **禁用用户特殊处理**：步骤 7 抛出 `AuthenticationException`，不调用 `chain.doFilter`，由 AuthenticationEntryPoint 返回 401 + ACCOUNT_DISABLED
- **jti 为 null 兼容**：`extractJti` 返回 null 时直接跳过黑名单检查（`isBlacklisted` 不会调用），不阻断流程
- **passwordChangeRequired**：始终设置 attribute（true 或 false），PasswordChangeCheckFilter 在后续处理中读取
- **`@SQLRestriction("deleted = false")**：BaseEntity 级软删除过滤，findWithDetailsById 自动排除 deleted=true 的记录，等同于 "用户不存在" 处理路径

## 错误处理

| 场景 | 处理方式 |
|------|---------|
| Authorization header 缺失/为空 | 静默跳过，chain.doFilter 放行 |
| token 解析失败（validateTokenAndGetClaims=null） | SecurityContextHolder.clearContext() → chain.doFilter 放行 |
| token type=refresh | SecurityContextHolder.clearContext() → chain.doFilter 放行 |
| jti 在黑名单中 | SecurityContextHolder.clearContext() → chain.doFilter 放行 |
| claims 中无 jti | 跳过黑名单检查 → 继续后续流程 |
| extractUserId 返回 null | SecurityContextHolder.clearContext() → chain.doFilter 放行 |
| 用户不存在/已删除（findWithDetailsById 返回 empty） | SecurityContextHolder.clearContext() → chain.doFilter 放行 |
| 用户被禁用（enabled=false） | 抛出 `AuthenticationException` 匿名子类，message=ACCOUNT_DISABLED.getMessage()，不调用 chain.doFilter |
| 权限收集 | Set 去重，空集正常，SecurityContext 仍设置 authentication |

## 单元测试设计

### JwtAuthenticationFilterTest

**形态**：class（JUnit 5），无 Spring 上下文
**包路径**：`com.aimedical.modules.commonmodule.auth.security.JwtAuthenticationFilterTest`
**注解**：无（package-private class）

**测试夹具**：

```java
class JwtAuthenticationFilterTest {
    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final TokenBlacklist tokenBlacklist = mock(TokenBlacklist.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, tokenBlacklist, userRepository);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }
}
```

**测试方法清单**（9 用例）：

| # | 测试方法 | 场景设置 | 验证点 |
|---|---------|---------|--------|
| 1 | `shouldSkipWhenNoAuthHeader` | request 无 Authorization header | `chain.doFilter` 被调用，SecurityContext 中 authentication 为 null |
| 2 | `shouldSkipWhenInvalidToken` | header=`Bearer xxx`，`jwtUtil.validateTokenAndGetClaims("xxx")`=null | chain.doFilter 被调用，SecurityContext 未设 authentication |
| 3 | `shouldSkipWhenRefreshTokenType` | header=`Bearer xxx`，claims 含 `type="refresh"`，claims.get("jti")=null | chain.doFilter 被调用，`verify(tokenBlacklist, never()).isBlacklisted(any())`，SecurityContext 未设 authentication |
| 4 | `shouldSkipWhenTokenBlacklisted` | header=`Bearer xxx`，claims 含 `jti="some-jti"`，`tokenBlacklist.isBlacklisted("some-jti")`=true | chain.doFilter 被调用，SecurityContext 未设 authentication |
| 5 | `shouldSkipWhenUserNotFound` | 用户不存在，`userRepository.findWithDetailsById(any())`=Optional.empty() | chain.doFilter 被调用，SecurityContext 未设 authentication |
| 6 | `shouldThrowAccountDisabledWhenUserDisabled` | 用户存在但 `enabled=false` | 抛出 `AuthenticationException`，message 包含 `ACCOUNT_DISABLED.getMessage()` |
| 7 | `shouldAuthenticateSuccessfully` | 全部验证通过，用户有 1 role + 1 post→1 function | `SecurityContextHolder.getContext().getAuthentication()` 非 null，principal=userId，authorities 含 `ROLE_` + `FUNC_` |
| 8 | `shouldSetPasswordChangeRequiredAttribute` | `user.getPasswordChangeRequired()`=true | `request.getAttribute("passwordChangeRequired")`=true |
| 9 | `shouldPopulateAuthoritiesFromRolesAndFunctions` | 用户有 2 roles + 2 posts（每个 post 有 2 functions），其中 1 function 跨 post 重复 | authorities 应含 2 个 `ROLE_` 权限 + 4 个 `FUNC_` 权限（Set 去重后数量） |

**Mock 构建模板**（用例 7/8/9 通用）：

```java
private Claims mockValidClaims(Long userId) {
    Claims claims = mock(Claims.class);
    when(claims.get("type", String.class)).thenReturn(null);
    when(claims.get("jti", String.class)).thenReturn(null);
    when(claims.get("userId")).thenReturn(userId);
    return claims;
}

private User mockUser(Long id, boolean enabled, boolean passwordChangeRequired) {
    User user = mock(User.class);
    when(user.getId()).thenReturn(id);
    when(user.getEnabled()).thenReturn(enabled);
    when(user.getPasswordChangeRequired()).thenReturn(passwordChangeRequired);
    when(user.getRoles()).thenReturn(Set.of());
    when(user.getPosts()).thenReturn(Set.of());
    return user;
}
```

**测试关键细节**：
- 用例 3 中 `claims.get("type", String.class)` 需返回 `"refresh"`，验证 `verify(tokenBlacklist, never()).isBlacklisted(any())`
- 用例 4 中 `claims.get("jti", String.class)` 需返回 `"some-jti"`，`tokenBlacklist.isBlacklisted("some-jti")`=true
- 用例 6 需使用 `assertThrows(AuthenticationException.class, () → filter.doFilterInternal(...))`
- 用例 8 中 `request.getAttribute("passwordChangeRequired")` 使用 Mockito `ArgumentCaptor` 或直接 `verify(request).setAttribute(eq("passwordChangeRequired"), eq(true))`

**测试模板**（用例 7）：

```java
@Test
void shouldAuthenticateSuccessfully() throws Exception {
    Long userId = 1L;
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer valid-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    Claims claims = mockValidClaims(userId);
    when(jwtUtil.validateTokenAndGetClaims("valid-token")).thenReturn(claims);
    when(jwtUtil.getTokenType()).thenReturn("Bearer");

    User user = mockUser(userId, true, false);
    when(userRepository.findWithDetailsById(userId)).thenReturn(Optional.of(user));

    filter.doFilterInternal(request, response, chain);

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    assertNotNull(auth);
    assertEquals(userId, auth.getPrincipal());
    assertTrue(auth.getAuthorities().isEmpty());
    verify(chain).doFilter(request, response);
}
```

## 依赖关系

### 新增依赖（生产代码）
- `org.springframework.web.filter.OncePerRequestFilter` — 父类
- `com.aimedical.modules.commonmodule.jwt.JwtUtil` — 构造器注入，token 解析
- `com.aimedical.modules.commonmodule.auth.blacklist.TokenBlacklist` — 构造器注入，黑名单校验
- `com.aimedical.modules.commonmodule.permission.UserRepository` — 构造器注入，DB 用户查询
- `com.aimedical.modules.commonmodule.permission.User` / `Role` / `Post` / `PermissionFunction` — 权限装配遍历
- `io.jsonwebtoken.Claims` — token claims 提取
- `com.aimedical.common.exception.GlobalErrorCode` — ACCOUNT_DISABLED message 引用
- `org.springframework.security.authentication.UsernamePasswordAuthenticationToken` — SecurityContext 装配
- `org.springframework.security.core.authority.SimpleGrantedAuthority` — 权限表示
- `org.springframework.security.core.context.SecurityContextHolder` — 安全上下文管理
- `jakarta.servlet.*` — Servlet API
- `org.slf4j.Logger` / `LoggerFactory` — 日志
- `org.springframework.data.jpa.repository.EntityGraph` — UserRepository 新增方法注解

### 新增依赖（测试代码）
- `org.junit.jupiter.api.Test` / `@AfterEach`
- `org.springframework.mock.web.MockHttpServletRequest`
- `org.springframework.mock.web.MockHttpServletResponse`
- `org.mockito.Mockito` — mock jwtUtil / tokenBlacklist / userRepository / chain / claims / User
- `jakarta.servlet.FilterChain` — mock
- `org.springframework.security.core.Authentication` — 断言
- `org.springframework.security.core.context.SecurityContextHolder` — 上下文校验

### 已有依赖
- `JwtUtil.extractToken(String, String)` — 静态方法调用
- `TokenBlacklist.isBlacklisted(String)` — 接口方法
- `GlobalErrorCode.ACCOUNT_DISABLED` — message 常量
- `UserRepository` — 接口基础方法

### 暴露给后续任务的公开接口
- `JwtAuthenticationFilter(JwtUtil, TokenBlacklist, UserRepository)` — 供 SecurityConfigPhase1（任务 2.10）的 @Bean 方法创建并注册
- `UserRepository.findWithDetailsById(Long)` — 供本 Filter 及后续需要完整加载 roles/posts 的场景使用
