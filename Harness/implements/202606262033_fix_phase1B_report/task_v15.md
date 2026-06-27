# 任务指令（v15）

## 动作
NEW

## 任务描述
修复 T11：RestAuthenticationEntryPoint 改用异常类型判断

涉及 4 个文件：
1. **新增** `auth/exception/AccountDisabledAuthenticationException.java` — extends `AuthenticationException`
2. **修改** `auth/security/JwtAuthenticationFilter.java:131-133` — `throwAccountDisabled()` 改为抛出 `AccountDisabledAuthenticationException`
3. **修改** `auth/security/RestAuthenticationEntryPoint.java:16,27` — 用 `instanceof AccountDisabledAuthenticationException` 替代 `message.contains(ACCOUNT_DISABLED_MESSAGE)` 字符串匹配
4. **修改** `auth/security/RestAuthenticationEntryPointTest.java` — `shouldReturnAccountDisabledWhenMessageMatches` 改为创建真实 `AccountDisabledAuthenticationException` 实例而非 mock

## 选择理由
P2 批次 6 第三个任务，独立无前置依赖。当前 RestAuthenticationEntryPoint 使用消息字符串匹配识别 ACCOUNT_DISABLED，若消息文本因国际化或多语言需求变更，检测逻辑静默失效。改用异常类型判断（instanceof）与 OOD 3.3 节行为契约一致，消除设计级脆弱性。

## 任务上下文
### AccountDisabledAuthenticationException
- 继承 `org.springframework.security.core.AuthenticationException`
- 与 `PasswordChangeRequiredException` 同包（`auth/exception/`），后者继承 `AccessDeniedException`
- 构造器：`AccountDisabledAuthenticationException(String msg)` + `AccountDisabledAuthenticationException(String msg, Throwable cause)`
- 无需额外字段或注解

### JwtAuthenticationFilter 改动
当前 `throwAccountDisabled()`：
```java
private void throwAccountDisabled(String message) {
    throw new AuthenticationException(message) {};
}
```
改为：
```java
private void throwAccountDisabled(String message) {
    throw new AccountDisabledAuthenticationException(message);
}
```
删除原有匿名内部类花括号。

### RestAuthenticationEntryPoint 改动
当前 L16：`private static final String ACCOUNT_DISABLED_MESSAGE = GlobalErrorCode.ACCOUNT_DISABLED.getMessage();`
当前 L27：`boolean isAccountDisabled = (message != null && message.contains(ACCOUNT_DISABLED_MESSAGE));`

改为：
- 删除 L16 的 `ACCOUNT_DISABLED_MESSAGE` 常量
- L27 改为：`boolean isAccountDisabled = authException instanceof AccountDisabledAuthenticationException;`
- 需要新增 import: `import com.aimedical.modules.commonmodule.auth.exception.AccountDisabledAuthenticationException;`

### RestAuthenticationEntryPointTest 改动
- `shouldReturnAccountDisabledWhenMessageMatches`（L20-32）：将 mock `AuthenticationException` 替换为 `new AccountDisabledAuthenticationException("any message")`，移除 `when(authException.getMessage())` mock 调用，其他断言不变
- `shouldReturnUnauthorizedForGenericException` 和 `shouldReturnUnauthorizedWhenMessageIsNull`：无需修改，仍使用 mock `AuthenticationException`

## 已有代码上下文
### PasswordChangeRequiredException（参考模式）
```java
// auth/exception/PasswordChangeRequiredException.java
public class PasswordChangeRequiredException extends AccessDeniedException {
    public PasswordChangeRequiredException(String msg) { super(msg); }
    public PasswordChangeRequiredException(String msg, Throwable cause) { super(msg, cause); }
}
```

### 当前 RestAuthenticationEntryPoint
- 路径：`auth/security/RestAuthenticationEntryPoint.java`
- 当前通过 `authException.getMessage().contains(ACCOUNT_DISABLED_MESSAGE)` 判断
- `ACCOUNT_DISABLED_MESSAGE = GlobalErrorCode.ACCOUNT_DISABLED.getMessage()`（"账户已被管理员停用"）

### 当前 RestAuthenticationEntryPointTest
- 路径：`auth/security/RestAuthenticationEntryPointTest.java`
- 3 个测试：`shouldReturnAccountDisabledWhenMessageMatches`、`shouldReturnUnauthorizedForGenericException`、`shouldReturnUnauthorizedWhenMessageIsNull`
- 当前全部通过 mock `AuthenticationException` 并控制 `getMessage()` 返回值

## 文件路径（含 backend 前缀）
- 新增: `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/exception/AccountDisabledAuthenticationException.java`
- 修改: `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/JwtAuthenticationFilter.java`
- 修改: `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/RestAuthenticationEntryPoint.java`
- 修改: `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/RestAuthenticationEntryPointTest.java`

## 验证方式
- mvn clean test 验证后端全部 587+ 测试通过
- 前端 Vitest 失败为预存问题，不影响本任务验收
