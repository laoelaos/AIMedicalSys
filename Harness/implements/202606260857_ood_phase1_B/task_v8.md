# 任务指令（v8）

## 动作
NEW

## 任务描述
实现 Stage 2 安全异常处理入口：AuthenticationEntryPoint（统一 401 响应）+ AccessDeniedHandler（统一 403 响应）+ PasswordChangeRequiredException（异常类型）。

新建 3 个文件：

1. `backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/AuthenticationEntryPoint.java`
   - 实现 `org.springframework.security.web.AuthenticationEntryPoint`
   - 在 `commence()` 方法中区分 ACCOUNT_DISABLED 和其他未认证场景，写入统一 JSON 响应

2. `backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/AccessDeniedHandler.java`
   - 实现 `org.springframework.security.web.access.AccessDeniedHandler`
   - 在 `handle()` 方法中区分 PasswordChangeRequiredException 和其他 AccessDeniedException，写入统一 JSON 响应

3. `backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/exception/PasswordChangeRequiredException.java`
   - 继承 `org.springframework.security.access.AccessDeniedException`

包含 3 个对应测试类：
- `AuthenticationEntryPointTest`（无 Spring 上下文，MockHttpServletRequest/Response，Mockito）
- `AccessDeniedHandlerTest`（无 Spring 上下文）
- `PasswordChangeRequiredExceptionTest`（纯单元测试）

## 选择理由
- AuthenticationEntryPoint 和 AccessDeniedHandler 是 SecurityConfigPhase1（任务 2.10）直接依赖的异常处理入口，完成此项后 SecurityConfigPhase1 即可组装完整的 Filter 链
- 仅依赖已有基础设施：GlobalErrorCode（R2 ✅）、Result（R2 ✅）、ObjectMapper 序列化模式（R7 ✅）
- PasswordChangeRequiredException 是简单异常类（`extends AccessDeniedException`），与 AccessDeniedHandler 密切耦合（后者需 `instanceof` 判断），放在同一任务符合"1-3 个紧密相关类型"粒度规则
- 无其他未完成任务依赖，边界清晰，实现风险低

## 任务上下文

### AuthenticationEntryPoint（OOD 3.3 + 10.2）

**行为契约**：
- `AuthenticationException` 携带 `ErrorCode.ACCOUNT_DISABLED` 时 → HTTP 401，body=`Result.fail(GlobalErrorCode.ACCOUNT_DISABLED)`（code="ACCOUNT_DISABLED", message="账户已被管理员停用"）
- 其余未认证场景（token 缺失/无效/过期/黑名单）→ HTTP 401，body=`Result.fail(GlobalErrorCode.UNAUTHORIZED)`（code="UNAUTHORIZED", message="未认证或令牌已失效"）

**ACCOUNT_DISABLED 识别**：
- 方式 1（推荐）：检查 `exception.getMessage()` 是否包含 `ErrorCode.ACCOUNT_DISABLED.getMessage()` 或直接比对字符串 `"账户已被管理员停用"`
- 方式 2：创建自定义 AuthenticationException 子类携带 ErrorCode 字段（但需修改 JwtAuthenticationFilter，增加复杂度，不推荐在 v8 引入）

**实现要点**：
- `@Override public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)` 方法
- `response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)`（401）
- `response.setContentType("application/json")`
- `response.setCharacterEncoding("UTF-8")`
- ObjectMapper 在构造器中私有创建，与 GlobalRateLimitFilter 风格一致
- 不抛出异常（写 response 时 IOException 由调用者处理）
- 无 `@Component` 注解，由 SecurityConfigPhase1 显式注册

### AccessDeniedHandler（OOD 3.3 + 10.2）

**行为契约**：
- 异常类型为 `PasswordChangeRequiredException` → HTTP 403，body=`Result.fail(GlobalErrorCode.PASSWORD_CHANGE_REQUIRED)`（code="PASSWORD_CHANGE_REQUIRED", message="需要修改密码"）
- 其余 `AccessDeniedException` → HTTP 403，body=`Result.fail(GlobalErrorCode.FORBIDDEN)`（code="FORBIDDEN", message="无权限"）

**实现要点**：
- `@Override public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)` 方法
- 使用 `instanceof PasswordChangeRequiredException` 区分异常类型
- 其余实现模式与 AuthenticationEntryPoint 相同
- 无 `@Component` 注解

### PasswordChangeRequiredException（OOD 3.8 + 10.2）

**实现要点**：
- `public class PasswordChangeRequiredException extends org.springframework.security.access.AccessDeniedException`
- 两个构造器：
  - `public PasswordChangeRequiredException(String msg)` → `super(msg)`
  - `public PasswordChangeRequiredException(String msg, Throwable cause)` → `super(msg, cause)`
- 在 PasswordChangeCheckFilter（后续任务）中抛出

### ObjectMapper 使用约定
从 `com.fasterxml.jackson.databind.ObjectMapper`，与 GlobalRateLimitFilter 同为 `private final` 字段，在构造器内新建。不使用 Spring 注入（Filter/Handler 非 @Component，由 SecurityConfigPhase1 显式创建）。

当调用 `objectMapper.writeValueAsString()` 序列化 `Result.fail(...)` 时，Result 的 getter 方法（`getCode()`/`getMessage()`）会被 Jackson 自动调用产生 JSON 字段 `code` 和 `message`。直接写 `objectMapper.writeValueAsString(Result.fail(errorCode))`。

序列化失败时（`JsonProcessingException`），降级为 `response.getWriter().write("{\"code\":\"SYSTEM_ERROR\",\"message\":\"系统异常\"}")` 静态 fallback 字符串，catch 内不抛出异常。

### 测试覆盖

**AuthenticationEntryPointTest**（无 Spring 上下文）：
- ACCOUNT_DISABLED 场景：mock AuthenticationException with message="账户已被管理员停用" → assert status=401, code=ACCOUNT_DISABLED
- 通用未认证场景：mock AuthenticationException with message="其他" → assert status=401, code=UNAUTHORIZED
- Content-Type = application/json
- Response body 可通过 ObjectMapper.readTree() 正确解析 code/message 字段
- null request/response 不测试（由 Spring Security 框架保证非 null）

**AccessDeniedHandlerTest**（无 Spring 上下文）：
- PasswordChangeRequiredException → assert status=403, code=PASSWORD_CHANGE_REQUIRED
- 通用 AccessDeniedException → assert status=403, code=FORBIDDEN
- Content-Type = application/json

**PasswordChangeRequiredExceptionTest**（纯单元测试）：
- 实例化 `PasswordChangeRequiredException("msg")` → assert `getMessage() == "msg"`
- 实例化 `PasswordChangeRequiredException("msg", cause)` → assert `getCause() == cause`
- assert `new PasswordChangeRequiredException("msg") instanceof AccessDeniedException`

### 测试夹具模板
```java
private final AuthenticationEntryPoint entryPoint = new AuthenticationEntryPoint();
private final AccessDeniedHandler deniedHandler = new AccessDeniedHandler();
private final ObjectMapper objectMapper = new ObjectMapper();
```

### 代码风格约定
- 与项目已有 AuthenticationEntryPoint/AccessDeniedHandler 风格一致（若有）：无 Spring 注解，显式 ObjectMapper 构造
- 测试类 package-private（与 GlobalRateLimitFilterTest / InMemoryRateLimitGuardTest 风格一致）
- 标准 JUnit 5 + Mockito（mock AuthenticationException / AccessDeniedException）
- ObjectMapper 序列化使用 `writeValueAsString` 而非 `writeValue`（与 GlobalRateLimitFilter 一致）

## 已有代码上下文

### 依赖的基础设施（已完成）
- `com.aimedical.common.result.Result`（R2）— `Result.fail(ErrorCode errorCode)` 返回 Result 对象
- `com.aimedical.common.exception.GlobalErrorCode`（R2）— `ACCOUNT_DISABLED("ACCOUNT_DISABLED", "账户已被管理员停用")`, `UNAUTHORIZED("UNAUTHORIZED", "未认证")`, `FORBIDDEN("FORBIDDEN", "无权限")`, `PASSWORD_CHANGE_REQUIRED("PASSWORD_CHANGE_REQUIRED", "需要修改密码")`
- `com.aimedical.common.exception.ErrorCode` — interface with `getCode()`/`getMessage()`
- `com.fasterxml.jackson.databind.ObjectMapper` — 已有依赖
- `com.aimedical.modules.commonmodule.auth.security.GlobalRateLimitFilter`（R7）— 示范了 ObjectMapper 私有构造模式

### 测试基础设施
- `org.springframework.mock.web.MockHttpServletRequest`
- `org.springframework.mock.web.MockHttpServletResponse`
- `org.mockito.Mockito`
- `com.fasterxml.jackson.databind.ObjectMapper` — 用于反序列化验证 403/401 body
