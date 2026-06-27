# 详细设计（v15）

## 概述

修复 T11：将 `RestAuthenticationEntryPoint` 中基于字符串包含的账户禁用检测（`message.contains(ACCOUNT_DISABLED_MESSAGE)`）改为基于异常类型判断（`instanceof AccountDisabledAuthenticationException`），消除国际化或多语言场景下消息文本变更导致检测静默失效的设计级脆弱性。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/exception/AccountDisabledAuthenticationException.java` | 新建 | 账户禁用专用异常，继承 `AuthenticationException` |
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/JwtAuthenticationFilter.java` | 修改 | `throwAccountDisabled()` 抛出 `AccountDisabledAuthenticationException`（L131-133） |
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/RestAuthenticationEntryPoint.java` | 修改 | 删除 `ACCOUNT_DISABLED_MESSAGE` 常量（L16）；改用 `instanceof` 判断（L27） |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/RestAuthenticationEntryPointTest.java` | 修改 | `shouldReturnAccountDisabledWhenMessageMatches` 使用真实异常实例 |

## 类型定义

### AccountDisabledAuthenticationException

**形态**：class（extends `org.springframework.security.core.AuthenticationException`）
**包路径**：`com.aimedical.modules.commonmodule.auth.exception`
**职责**：表示账户已被禁用的认证异常，与 `PasswordChangeRequiredException`（同包）模式一致

```java
package com.aimedical.modules.commonmodule.auth.exception;

import org.springframework.security.core.AuthenticationException;

public class AccountDisabledAuthenticationException extends AuthenticationException {

    public AccountDisabledAuthenticationException(String msg) {
        super(msg);
    }

    public AccountDisabledAuthenticationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
```

**构造方式**：`new AccountDisabledAuthenticationException(String msg)` 或 `new AccountDisabledAuthenticationException(String msg, Throwable cause)`
**类型关系**：extends `AuthenticationException`（`org.springframework.security.core`）

### JwtAuthenticationFilter.throwAccountDisabled（方法修改）

**形态**：`private` 方法
**包路径**：`com.aimedical.modules.commonmodule.auth.security`
**职责**：抛出账户禁用异常

| 当前（L131-133） | 修改后 |
|-----------------|--------|
| `private void throwAccountDisabled(String message) {` | `private void throwAccountDisabled(String message) {` |
| `    throw new AuthenticationException(message) {};` | `    throw new AccountDisabledAuthenticationException(message);` |
| `}` | `}` |

**import 变更**：
- 删除：`import org.springframework.security.core.AuthenticationException;`（不再直接使用 `AuthenticationException` 于本方法；但该 import 可能仍被其他部分引用，需确认后决定是否保留）
- 新增：`import com.aimedical.modules.commonmodule.auth.exception.AccountDisabledAuthenticationException;`

**确认**：`AuthenticationException` 在 `JwtAuthenticationFilter.java` 中仅 L132 使用（`new AuthenticationException(message) {}`），无其他引用。因此删除 `import org.springframework.security.core.AuthenticationException;`，新增 `import com.aimedical.modules.commonmodule.auth.exception.AccountDisabledAuthenticationException;`。

### RestAuthenticationEntryPoint（字段和方法修改）

**形态**：class（implements `AuthenticationEntryPoint`）
**包路径**：`com.aimedical.modules.commonmodule.auth.security`

**L16 常量删除**：
```java
// 删除以下行
private static final String ACCOUNT_DISABLED_MESSAGE = GlobalErrorCode.ACCOUNT_DISABLED.getMessage();
```

**L25-27 方法体修改**：
```java
// 当前
String message = authException.getMessage();
boolean isAccountDisabled = (message != null && message.contains(ACCOUNT_DISABLED_MESSAGE));

// 修改后（删除 message 变量声明+赋值，直接使用 instanceof）
boolean isAccountDisabled = authException instanceof AccountDisabledAuthenticationException;
```

**整体修改后 commence 方法**：
```java
@Override
public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
    boolean isAccountDisabled = authException instanceof AccountDisabledAuthenticationException;
    GlobalErrorCode errorCode = isAccountDisabled ? GlobalErrorCode.ACCOUNT_DISABLED : GlobalErrorCode.UNAUTHORIZED;

    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    try {
        String body = objectMapper.writeValueAsString(Result.fail(errorCode));
        response.getWriter().write(body);
    } catch (JsonProcessingException e) {
        response.getWriter().write("{\"code\":\"SYSTEM_ERROR\",\"message\":\"系统异常\"}");
    }
}
```

**import 变更**：
- 删除：`import com.aimedical.common.exception.GlobalErrorCode;`（`GlobalErrorCode` 仅用于 `ACCOUNT_DISABLED_MESSAGE` 常量，删除常量后不再使用）
- 新增：`import com.aimedical.modules.commonmodule.auth.exception.AccountDisabledAuthenticationException;`
- 保留：`Result`、`ObjectMapper`、`JsonProcessingException`、`HttpServletRequest`、`HttpServletResponse`、`AuthenticationException`、`AuthenticationEntryPoint`、`IOException`

**确认**：`GlobalErrorCode` 在 `RestAuthenticationEntryPoint.java` 中仅 L16 使用（`GlobalErrorCode.ACCOUNT_DISABLED.getMessage()`），L28 使用 `GlobalErrorCode.ACCOUNT_DISABLED` 和 `GlobalErrorCode.UNAUTHORIZED` 枚举常量（不用 `getMessage()`）。因此删除 `GlobalErrorCode` import 后，L28 的 `GlobalErrorCode.ACCOUNT_DISABLED` 和 `GlobalErrorCode.UNAUTHORIZED` 无法编译。需保留 `GlobalErrorCode` import，保持原样。

**修正**：保留 `import com.aimedical.common.exception.GlobalErrorCode;`，仅删除 L16 常量字段。

### RestAuthenticationEntryPointTest 修改

**形态**：class（package-private test class）
**包路径**：`com.aimedical.modules.commonmodule.auth.security`

**`shouldReturnAccountDisabledWhenMessageMatches`（L20-32）修改**：

| 当前 | 修改后 |
|------|--------|
| `AuthenticationException authException = mock(AuthenticationException.class);` | `AuthenticationException authException = new AccountDisabledAuthenticationException("any message");` |
| `when(authException.getMessage()).thenReturn("账户已被管理员停用");` | 删除此行 |
| 后续断言不变 | 后续断言不变 |

**import 变更**：
- 新增：`import com.aimedical.modules.commonmodule.auth.exception.AccountDisabledAuthenticationException;`
- 保留：`mock` 和 `when` 在 L11-12 仍被其他测试使用，不可删除

**`shouldReturnUnauthorizedForGenericException`（L35-46）**：不变，仍使用 `mock(AuthenticationException.class)`

**`shouldReturnUnauthorizedWhenMessageIsNull`（L49-60）**：不变，仍使用 `mock(AuthenticationException.class)`

## 错误处理

不涉及新错误处理逻辑。`AccountDisabledAuthenticationException` 继承自 `AuthenticationException`，是 `AuthenticationException` 的子类型，`RestAuthenticationEntryPoint.commence()` 的 `authException` 参数类型不变（`AuthenticationException`），`instanceof` 向下转型判断不会抛出新异常。

## 行为契约

### AccountDisabledAuthenticationException

- **前置**：`msg` 非 null（由父类 `AuthenticationException` 的构造器约束）
- **行为**：与 `PasswordChangeRequiredException` 行为一致，无额外字段或逻辑
- **后置**：抛出后由 `RestAuthenticationEntryPoint.commence()` 通过 `instanceof` 检测

### JwtAuthenticationFilter.throwAccountDisabled

- **行为变化**：抛出 `AccountDisabledAuthenticationException` 而非匿名 `AuthenticationException`
- **传播路径**：Spring Security `ExceptionTranslationFilter` 捕获后调用 `AuthenticationEntryPoint.commence(HttpServletRequest, HttpServletResponse, AuthenticationException)`，`authException` 参数类型仍为 `AuthenticationException`，多态使 `instanceof` 检查通过

### RestAuthenticationEntryPoint.commence

- **前置**：不变（`request`、`response`、`authException` 均由框架传入）
- **行为变化**：`isAccountDisabled` 判定从字符串匹配改为类型匹配
- **后置**：返回值/状态码与原行为一致

## 依赖关系

| 依赖 | 说明 |
|------|------|
| `org.springframework.security.core.AuthenticationException` | `AccountDisabledAuthenticationException` 的父类 |
| `com.aimedical.modules.commonmodule.auth.exception.AccountDisabledAuthenticationException` | `JwtAuthenticationFilter` 新增依赖 |
| `com.aimedical.modules.commonmodule.auth.exception.AccountDisabledAuthenticationException` | `RestAuthenticationEntryPoint` 新增依赖 |
| `com.aimedical.common.exception.GlobalErrorCode` | `RestAuthenticationEntryPoint` 保留（L28 使用枚举常量） |

## 测试适配

| 测试方法 | 修改内容 |
|---------|---------|
| `shouldReturnAccountDisabledWhenMessageMatches` | 替换 mock 为 `new AccountDisabledAuthenticationException("any message")`；删除 `when(authException.getMessage())` |
| `shouldReturnUnauthorizedForGenericException` | 不变 |
| `shouldReturnUnauthorizedWhenMessageIsNull` | 不变 |
