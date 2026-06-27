# 详细设计（v6）

## 概述

抽取 `GlobalExceptionHandler.formatMessage()` 内联方法为独立 `MessageInterpolator` 组件，统一注入 `GlobalExceptionHandler`、`RestAuthenticationEntryPoint`、`RestAccessDeniedHandler` 三个出口，确保所有错误响应都经过同一消息插值管线。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `common/src/main/java/com/aimedical/common/util/MessageInterpolator.java` | 新建 | 消息插值接口 |
| `common/src/main/java/com/aimedical/common/util/SimpleMessageInterpolator.java` | 新建 | `@Component` 实现，复制现有 `formatMessage()` 逻辑 |
| `common/src/main/java/com/aimedical/common/config/GlobalExceptionHandler.java` | 修改 | 注入 `MessageInterpolator`，删除 `formatMessage()` |
| `common-module-impl/src/main/java/.../auth/security/RestAuthenticationEntryPoint.java` | 修改 | 注入 `MessageInterpolator`，插值后调用 `Result.fail(code, message)` |
| `common-module-impl/src/main/java/.../auth/security/RestAccessDeniedHandler.java` | 修改 | 同上 |
| `common-module-impl/src/main/java/.../auth/security/SecurityConfigPhase1.java` | 修改 | `filterChain()` 增加 `MessageInterpolator` 参数，传参构造两个 handler |
| `common/src/test/java/.../config/GlobalExceptionHandlerTest.java` | 修改 | 构造 `GlobalExceptionHandler` 时注入 `SimpleMessageInterpolator` |
| `common-module-impl/src/test/java/.../auth/security/RestAuthenticationEntryPointTest.java` | 修改 | 构造时传入 mock `MessageInterpolator`，验证插值生效 |
| `common-module-impl/src/test/java/.../auth/security/RestAccessDeniedHandlerTest.java` | 修改 | 同上 |
| `common/src/test/java/.../util/SimpleMessageInterpolatorTest.java` | 新建 | 独立测试 `SimpleMessageInterpolator` 所有插值场景 |

## 类型定义

### MessageInterpolator
**形态**：interface
**包路径**：`com.aimedical.common.util`
**职责**：定义消息模板插值契约

```java
public interface MessageInterpolator {
    String interpolate(String template, Object[] args);
}
```

**公开接口**：
- `String interpolate(String template, Object[] args)` — 对 template 进行占位符替换；args 为 null 或空数组时直接返回 template

### SimpleMessageInterpolator
**形态**：class
**包路径**：`com.aimedical.common.util`
**职责**：`MessageInterpolator` 的 Spring 组件实现，逻辑与现有 `GlobalExceptionHandler.formatMessage()` 一致
**注解**：`@Component`
**构造方式**：默认无参构造器，Spring 自动扫描为 Bean

```java
@Component
public class SimpleMessageInterpolator implements MessageInterpolator {
    @Override
    public String interpolate(String template, Object[] args) { ... }
}
```

### GlobalExceptionHandler（修改后）
**形态**：class
**包路径**：`com.aimedical.common.config`（不变）
**注入方式**：构造器注入 `MessageInterpolator`

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = ...;
    private final MessageInterpolator messageInterpolator;

    public GlobalExceptionHandler(MessageInterpolator messageInterpolator) {
        this.messageInterpolator = messageInterpolator;
    }

    // handleBusinessException:
    //   String message = messageInterpolator.interpolate(errorCode.getMessage(), e.getArgs());
    //
    // formatMessage() 方法整块删除
}
```

### RestAuthenticationEntryPoint（修改后）
**形态**：class
**包路径**：`com.aimedical.modules.commonmodule.auth.security`（不变）

```java
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;
    private final MessageInterpolator messageInterpolator;

    public RestAuthenticationEntryPoint(MessageInterpolator messageInterpolator) {
        this.objectMapper = new ObjectMapper();
        this.messageInterpolator = messageInterpolator;
    }

    // commence():
    //   String message = messageInterpolator.interpolate(errorCode.getMessage(), null);
    //   String body = objectMapper.writeValueAsString(Result.fail(errorCode.getCode(), message));
}
```

### RestAccessDeniedHandler（修改后）
**形态**：class
**包路径**：`com.aimedical.modules.commonmodule.auth.security`（不变）

```java
public class RestAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;
    private final MessageInterpolator messageInterpolator;

    public RestAccessDeniedHandler(MessageInterpolator messageInterpolator) {
        this.objectMapper = new ObjectMapper();
        this.messageInterpolator = messageInterpolator;
    }

    // handle():
    //   String message = messageInterpolator.interpolate(errorCode.getMessage(), null);
    //   String body = objectMapper.writeValueAsString(Result.fail(errorCode.getCode(), message));
}
```

### SecurityConfigPhase1（修改后）
**形态**：class（不变）

```java
@Bean
public SecurityFilterChain filterChain(
        HttpSecurity http,
        GlobalRateLimitFilter globalRateLimitFilter,
        JwtAuthenticationFilter jwtAuthenticationFilter,
        PasswordChangeCheckFilter passwordChangeCheckFilter,
        MessageInterpolator messageInterpolator)  // ← 新增参数
        throws Exception {
    // ...
    .exceptionHandling(ex -> ex
        .authenticationEntryPoint(new RestAuthenticationEntryPoint(messageInterpolator))
        .accessDeniedHandler(new RestAccessDeniedHandler(messageInterpolator)))
    // ...
}
```

## 错误处理

- `SimpleMessageInterpolator.interpolate()` 内部：`MessageFormat.format()` 失败后降级为 regex 替换，最终兜底直接返回原模板（与现有行为一致）
- `RestAuthenticationEntryPoint/RestAccessDeniedHandler` 的 `JsonProcessingException` 捕获保持不变，写入 fallback JSON 字符串

## 行为契约

### 不变
- `GlobalExceptionHandler.handleBusinessException()` 输出的 message 内容不变（插值逻辑完全相同，仅通过委托调用）
- 无动态参数的 ErrorCode（UNAUTHORIZED、FORBIDDEN、ACCOUNT_DISABLED、PASSWORD_CHANGE_REQUIRED）消息内容不变
- `SecurityConfigPhase1.filterChain()` 的 filter 链行为不变
- HTTP 状态码映射不变
- 所有现有测试行为不变（仅调整构造方式）

### 变更
- `GlobalExceptionHandler` 无法再直接 `new`（需注入 `MessageInterpolator`）；测试中需传入实例
- `RestAuthenticationEntryPoint` 和 `RestAccessDeniedHandler` 的无参构造器删除，统一使用有参构造器
- `RestAuthenticationEntryPoint/RestAccessDeniedHandler` 输出的 JSON 中 `message` 字段改为通过插值管线获取（当前无动态参数时与原 message 一致）

## 依赖关系

- `SimpleMessageInterpolator` → `MessageInterpolator`（实现）
- `GlobalExceptionHandler` → `MessageInterpolator`（构造器注入）
- `RestAuthenticationEntryPoint` → `MessageInterpolator`（构造器注入）
- `RestAccessDeniedHandler` → `MessageInterpolator`（构造器注入）
- `SecurityConfigPhase1` → `MessageInterpolator`（Bean 方法参数注入）
