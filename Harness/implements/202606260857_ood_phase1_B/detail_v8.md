# 详细设计（v8）

## 概述

实现 Stage 2 安全异常处理入口：RestAuthenticationEntryPoint（统一 401 响应）、RestAccessDeniedHandler（统一 403 响应）、PasswordChangeRequiredException（异常类型）。新建 3 个生产类、3 个测试类。

设计目标：
- RestAuthenticationEntryPoint 实现 `org.springframework.security.web.AuthenticationEntryPoint`，在 `commence()` 中区分 ACCOUNT_DISABLED 和其他未认证场景，写入统一 JSON 401 响应
- RestAccessDeniedHandler 实现 `org.springframework.security.web.access.AccessDeniedHandler`，在 `handle()` 中区分 PasswordChangeRequiredException 和其他 AccessDeniedException，写入统一 JSON 403 响应
- PasswordChangeRequiredException 继承 `org.springframework.security.access.AccessDeniedException`，两个构造器
- 无 `@Component` 注解，由后续 SecurityConfigPhase1 显式注册
- 与 GlobalRateLimitFilter 保持一致的 ObjectMapper 使用风格（构造器内 `private final` 新建）

不在范围：SecurityConfigPhase1 的 Filter 链组装、JwtAuthenticationFilter 迁移、自定义 AuthenticationException 子类设计。

## 文件规划

| 文件路径（相对 `AIMedical/backend/`） | 操作 | 职责 |
|---------|------|------|
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/RestAuthenticationEntryPoint.java` | 新建 | 统一 401 响应入口，区分 ACCOUNT_DISABLED |
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/RestAccessDeniedHandler.java` | 新建 | 统一 403 响应入口，区分 PasswordChangeRequiredException |
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/exception/PasswordChangeRequiredException.java` | 新建 | AccessDeniedException 子类，携带密码变更原因 |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/RestAuthenticationEntryPointTest.java` | 新建 | RestAuthenticationEntryPoint 单元测试 |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/RestAccessDeniedHandlerTest.java` | 新建 | RestAccessDeniedHandler 单元测试 |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/exception/PasswordChangeRequiredExceptionTest.java` | 新建 | PasswordChangeRequiredException 纯单元测试 |

## 类型定义

### RestAuthenticationEntryPoint

**形态**：public class
**包路径**：`com.aimedical.modules.commonmodule.auth.security`
**职责**：Spring Security 认证异常入口点，对未认证请求写入统一 JSON 401 响应。在 JwtAuthenticationFilter 抛出 AuthenticationException 时由 SecurityConfigPhase1 注册调用。

```java
package com.aimedical.modules.commonmodule.auth.security;

import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.common.result.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String ACCOUNT_DISABLED_MESSAGE = GlobalErrorCode.ACCOUNT_DISABLED.getMessage();

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint() {
        this.objectMapper = new ObjectMapper();
    }
}
```

**公开接口**：

| 方法签名 | 返回 | 说明 |
|---------|------|------|
| `public RestAuthenticationEntryPoint()` | 构造器 | ObjectMapper 在构造器内新建 |
| `public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)` | void | `@Override`，写入统一 JSON 401 响应 |

**构造方式**：由 SecurityConfigPhase1 的 @Bean 方法显式创建：`new RestAuthenticationEntryPoint()`。
**类型关系**：实现 `org.springframework.security.web.AuthenticationEntryPoint`。

### RestAccessDeniedHandler

**形态**：public class
**包路径**：`com.aimedical.modules.commonmodule.auth.security`
**职责**：Spring Security 授权拒绝入口点，对无权限请求写入统一 JSON 403 响应。区分 PasswordChangeRequiredException 与其他 AccessDeniedException。

```java
package com.aimedical.modules.commonmodule.auth.security;

import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.auth.exception.PasswordChangeRequiredException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler() {
        this.objectMapper = new ObjectMapper();
    }
}
```

**公开接口**：

| 方法签名 | 返回 | 说明 |
|---------|------|------|
| `public RestAccessDeniedHandler()` | 构造器 | ObjectMapper 在构造器内新建 |
| `public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)` | void | `@Override`，写入统一 JSON 403 响应 |

**构造方式**：由 SecurityConfigPhase1 的 @Bean 方法显式创建：`new RestAccessDeniedHandler()`。
**类型关系**：实现 `org.springframework.security.web.access.AccessDeniedHandler`，依赖 `PasswordChangeRequiredException`。

### PasswordChangeRequiredException

**形态**：public class
**包路径**：`com.aimedical.modules.commonmodule.auth.exception`
**职责**：表示用户需要修改密码的授权异常。在 PasswordChangeCheckFilter（后续任务）中抛出，由 RestAccessDeniedHandler 识别并返回特定 403 响应。

```java
package com.aimedical.modules.commonmodule.auth.exception;

import org.springframework.security.access.AccessDeniedException;

public class PasswordChangeRequiredException extends AccessDeniedException {

    public PasswordChangeRequiredException(String msg) {
        super(msg);
    }

    public PasswordChangeRequiredException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
```

**公开接口**：

| 方法签名 | 返回 | 说明 |
|---------|------|------|
| `public PasswordChangeRequiredException(String msg)` | 构造器 | 仅消息 |
| `public PasswordChangeRequiredException(String msg, Throwable cause)` | 构造器 | 消息 + 原因 |

**构造方式**：`new PasswordChangeRequiredException("msg")` 或 `new PasswordChangeRequiredException("msg", cause)`。
**类型关系**：继承 `org.springframework.security.access.AccessDeniedException`。

## 行为契约

### RestAuthenticationEntryPoint.commence

```
输入：HttpServletRequest request, HttpServletResponse response, AuthenticationException authException
输出：void（直接操作 response 响应）

算法：
1. 异常类型判断：
   String message = authException.getMessage()
   boolean isAccountDisabled = (message != null && message.contains(ACCOUNT_DISABLED_MESSAGE))
   GlobalErrorCode errorCode = isAccountDisabled ? GlobalErrorCode.ACCOUNT_DISABLED : GlobalErrorCode.UNAUTHORIZED
2. 响应写入：
   response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)  // 401
   response.setContentType("application/json")
   response.setCharacterEncoding("UTF-8")
   try {
       String body = objectMapper.writeValueAsString(Result.fail(errorCode))
       response.getWriter().write(body)
   } catch (JsonProcessingException e) {
       response.getWriter().write("{\"code\":\"SYSTEM_ERROR\",\"message\":\"系统异常\"}")
   }
```

**契约要点**：
- `ACCOUNT_DISABLED_MESSAGE` 取 `GlobalErrorCode.ACCOUNT_DISABLED.getMessage()` 的值即 `"账户已被管理员停用"`，在 commence() 中用 `String.contains()` 判断是否命中
- 非 ACCOUNT_DISABLED 的场景（token 缺失/无效/过期/黑名单等）统一返回 UNAUTHORIZED
- 序列化失败时降级为静态 fallback 字符串，catch 内不抛出异常
- 不调用 `filterChain.doFilter()`（AuthenticationEntryPoint 无 filterChain 参数）
- 不抛出 IOException（写入时由 Servlet 容器处理）
- `response.getWriter()` 在 catch 块中被调用时可能抛出 `IllegalStateException`（响应已提交或 `getOutputStream()` 已被调用），此风险在正常 Filter/Handler 调用流程中极难触发（响应尚未提交），可接受由 Servlet 容器处理

### RestAccessDeniedHandler.handle

```
输入：HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException
输出：void（直接操作 response 响应）

算法：
1. 异常类型判断：
   GlobalErrorCode errorCode = (accessDeniedException instanceof PasswordChangeRequiredException)
       ? GlobalErrorCode.PASSWORD_CHANGE_REQUIRED
       : GlobalErrorCode.FORBIDDEN
2. 响应写入（同 RestAuthenticationEntryPoint 模式）：
   response.setStatus(HttpServletResponse.SC_FORBIDDEN)  // 403
   response.setContentType("application/json")
   response.setCharacterEncoding("UTF-8")
   try {
       String body = objectMapper.writeValueAsString(Result.fail(errorCode))
       response.getWriter().write(body)
   } catch (JsonProcessingException e) {
       response.getWriter().write("{\"code\":\"SYSTEM_ERROR\",\"message\":\"系统异常\"}")
   }
```

**契约要点**：
- 使用 `instanceof PasswordChangeRequiredException` 判断异常类型
- 非 PasswordChangeRequiredException 的 AccessDeniedException 统一返回 FORBIDDEN
- 序列化失败 fallback 同 RestAuthenticationEntryPoint
- `response.getWriter()` 在 catch 块中抛出 `IllegalStateException` 的风险同 RestAuthenticationEntryPoint，可接受

## 错误处理

| 场景 | 处理方式 |
|------|---------|
| ACCOUNT_DISABLED 未认证 | HTTP 401，body=`Result.fail(GlobalErrorCode.ACCOUNT_DISABLED)` |
| 其他未认证（token 缺失/无效/过期） | HTTP 401，body=`Result.fail(GlobalErrorCode.UNAUTHORIZED)` |
| PasswordChangeRequiredException | HTTP 403，body=`Result.fail(GlobalErrorCode.PASSWORD_CHANGE_REQUIRED)` |
| 其他 AccessDeniedException | HTTP 403，body=`Result.fail(GlobalErrorCode.FORBIDDEN)` |
| JSON 序列化失败 | 降级写入静态字符串 `{"code":"SYSTEM_ERROR","message":"系统异常"}`，不抛出异常 |

## 单元测试设计

### RestAuthenticationEntryPointTest

**形态**：class（JUnit 5），无 Spring 上下文
**包路径**：`com.aimedical.modules.commonmodule.auth.security.RestAuthenticationEntryPointTest`
**注解**：无（package-private class）

**测试夹具**：
```java
class RestAuthenticationEntryPointTest {
    private final RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint();
    private final ObjectMapper objectMapper = new ObjectMapper();
}
```

**测试方法清单**（3 用例）：

| # | 测试方法 | 覆盖维度 | 验证点 |
|---|---------|---------|--------|
| 1 | `shouldReturnAccountDisabledWhenMessageMatches` | ACCOUNT_DISABLED | status=401, contentType=application/json, body.code="ACCOUNT_DISABLED" |
| 2 | `shouldReturnUnauthorizedForGenericException` | 通用未认证 | status=401, contentType=application/json, body.code="UNAUTHORIZED" |
| 3 | `shouldReturnUnauthorizedWhenMessageIsNull` | message 为 null | status=401, code=UNAUTHORIZED（null.getMessage() 不抛 NPE） |

**关键实现细节**：
- 使用 `Mockito.mock(AuthenticationException.class)` 创建 mock，`when(authException.getMessage()).thenReturn(...)`
- 对于 message 为 null 的场景：`when(authException.getMessage()).thenReturn(null)` → 验证 NPE 安全
- 响应体解析：`objectMapper.readTree(response.getContentAsString()).get("code").asText()`

**测试模板**（用例 1）：
```java
@Test
void shouldReturnAccountDisabledWhenMessageMatches() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    AuthenticationException authException = mock(AuthenticationException.class);
    when(authException.getMessage()).thenReturn("账户已被管理员停用");

    entryPoint.commence(request, response, authException);

    assertEquals(401, response.getStatus());
    assertEquals("application/json", response.getContentType());
    JsonNode body = objectMapper.readTree(response.getContentAsString());
    assertEquals("ACCOUNT_DISABLED", body.get("code").asText());
}
```

### RestAccessDeniedHandlerTest

**形态**：class（JUnit 5），无 Spring 上下文
**包路径**：`com.aimedical.modules.commonmodule.auth.security.RestAccessDeniedHandlerTest`
**注解**：无（package-private class）

**测试夹具**：
```java
class RestAccessDeniedHandlerTest {
    private final RestAccessDeniedHandler deniedHandler = new RestAccessDeniedHandler();
    private final ObjectMapper objectMapper = new ObjectMapper();
}
```

**测试方法清单**（3 用例）：

| # | 测试方法 | 覆盖维度 | 验证点 |
|---|---------|---------|--------|
| 1 | `shouldReturnPasswordChangeRequiredForPasswordChangeException` | PasswordChangeRequiredException | status=403, body.code="PASSWORD_CHANGE_REQUIRED" |
| 2 | `shouldReturnForbiddenForGenericAccessDenied` | 通用 AccessDeniedException | status=403, body.code="FORBIDDEN" |
| 3 | `shouldSetJsonContentType` | Content-Type | contentType=application/json（可在用例 1/2 中组合断言） |

**关键实现细节**：
- PasswordChangeRequiredException 直接用 `new PasswordChangeRequiredException("msg")` 创建（非 mock）
- 通用 AccessDeniedException 用 `Mockito.mock(AccessDeniedException.class)` 或 `new AccessDeniedException("msg")` 创建
- 内容类型断言可合并到用例 1

**测试模板**（用例 1）：
```java
@Test
void shouldReturnPasswordChangeRequiredForPasswordChangeException() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    PasswordChangeRequiredException ex = new PasswordChangeRequiredException("需要修改密码");

    deniedHandler.handle(request, response, ex);

    assertEquals(403, response.getStatus());
    assertEquals("application/json", response.getContentType());
    JsonNode body = objectMapper.readTree(response.getContentAsString());
    assertEquals("PASSWORD_CHANGE_REQUIRED", body.get("code").asText());
}
```

### PasswordChangeRequiredExceptionTest

**形态**：class（JUnit 5），纯单元测试
**包路径**：`com.aimedical.modules.commonmodule.auth.exception.PasswordChangeRequiredExceptionTest`
**注解**：无（package-private class）

**测试方法清单**（3 用例）：

| # | 测试方法 | 覆盖维度 | 验证点 |
|---|---------|---------|--------|
| 1 | `shouldPreserveMessage` | 消息构造 | `new PasswordChangeRequiredException("msg")`.getMessage() == "msg" |
| 2 | `shouldPreserveCause` | 原因构造 | `new PasswordChangeRequiredException("msg", cause)`.getCause() == cause |
| 3 | `shouldBeInstanceOfAccessDeniedException` | 继承关系 | `new PasswordChangeRequiredException("msg") instanceof AccessDeniedException` |

**测试模板**：
```java
class PasswordChangeRequiredExceptionTest {
    @Test
    void shouldPreserveMessage() {
        PasswordChangeRequiredException ex = new PasswordChangeRequiredException("test msg");
        assertEquals("test msg", ex.getMessage());
    }

    @Test
    void shouldPreserveCause() {
        RuntimeException cause = new RuntimeException("cause");
        PasswordChangeRequiredException ex = new PasswordChangeRequiredException("msg", cause);
        assertSame(cause, ex.getCause());
    }

    @Test
    void shouldBeInstanceOfAccessDeniedException() {
        PasswordChangeRequiredException ex = new PasswordChangeRequiredException("msg");
        assertInstanceOf(AccessDeniedException.class, ex);
    }
}
```

## 依赖关系

### 新增依赖（生产代码）
- `org.springframework.security.web.AuthenticationEntryPoint` — 接口
- `org.springframework.security.web.access.AccessDeniedHandler` — 接口
- `org.springframework.security.access.AccessDeniedException` — 父类
- `org.springframework.security.core.AuthenticationException` — commence 方法参数类型
- `com.aimedical.common.exception.GlobalErrorCode` — ACCOUNT_DISABLED, UNAUTHORIZED, FORBIDDEN, PASSWORD_CHANGE_REQUIRED
- `com.aimedical.common.result.Result` — `Result.fail(ErrorCode)` 统一响应格式
- `com.fasterxml.jackson.databind.ObjectMapper` — JSON 序列化
- `com.fasterxml.jackson.core.JsonProcessingException` — 序列化异常捕获
- `jakarta.servlet.*` — Servlet API
- `com.aimedical.modules.commonmodule.auth.exception.PasswordChangeRequiredException` — RestAccessDeniedHandler 的 instanceof 判断目标

### 新增依赖（测试代码）
- `org.junit.jupiter.api.Test`
- `org.springframework.mock.web.MockHttpServletRequest`
- `org.springframework.mock.web.MockHttpServletResponse`
- `org.mockito.Mockito` — mock AuthenticationException
- `com.fasterxml.jackson.databind.ObjectMapper` — 反序列化验证响应 body
- `com.fasterxml.jackson.databind.JsonNode` — 读取响应字段
- `org.springframework.security.access.AccessDeniedException` — 泛型断言
- `org.springframework.security.core.AuthenticationException` — mock 目标

### 已有依赖（不变）
- `GlobalErrorCode` — R2 已完成
- `Result.fail(ErrorCode)` — 已有
- `ObjectMapper` — Jackson 已有依赖

### 暴露给后续任务的公开接口
- `RestAuthenticationEntryPoint()` — 供 SecurityConfigPhase1（任务 2.10）通过 @Bean 创建
- `RestAccessDeniedHandler()` — 供 SecurityConfigPhase1（任务 2.10）通过 @Bean 创建
- `PasswordChangeRequiredException(String, Throwable)` — 供 PasswordChangeCheckFilter（后续任务）抛出

### 不在此范围
- SecurityConfigPhase1 的 Bean 注册和 Filter 链组装（任务 2.10）
- JwtAuthenticationFilter 中的 AuthenticationException 抛出（已有）
- 自定义 AuthenticationException 子类（按任务推荐方式 1，直接检查 message 字符串）
- PasswordChangeCheckFilter 的创建（后续任务）

## 修订说明（v8 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] AuthenticationEntryPoint / AccessDeniedHandler 类名与导入接口名冲突：自定义类 `AuthenticationEntryPoint` 与 `org.springframework.security.web.AuthenticationEntryPoint` 同名导致编译错误；`AccessDeniedHandler` 同理 | 自定义类重命名为 `RestAuthenticationEntryPoint` / `RestAccessDeniedHandler`，避免与 Spring Security 接口名冲突；同步更新文件路径、所有引用处及测试类名 |
| [轻微] 骨架代码缺少 `JsonProcessingException` 导入 | 在 `RestAuthenticationEntryPoint` 和 `RestAccessDeniedHandler` 的骨架代码 import 列表中增加 `import com.fasterxml.jackson.core.JsonProcessingException;` |
| [轻微] catch 块中 `response.getWriter()` 可能抛出 `IllegalStateException` | 在两个行为契约的契约要点中增加说明：`response.getWriter()` 在 catch 块抛出 `IllegalStateException` 的风险极低（响应尚未提交），可接受由 Servlet 容器处理，不做额外防御 |
