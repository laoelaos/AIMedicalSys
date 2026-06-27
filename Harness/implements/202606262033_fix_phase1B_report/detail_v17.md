# 详细设计（v17 r1）

## 概述

实现 `SecurityAuditLogger` 安全审计日志系统（接口 + 事件值对象 + Phase 1 Logback 文件实现），在 `AuthServiceImpl.login()/logout()/refreshToken()/changePassword()` 中调用记录审计事件，同步修改 `AuthService` 接口（`logout` 增加 `refreshToken` 参数）、`AuthController`（透传 refreshToken 给 service）及对应测试。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/audit/SecurityAuditLogger.java` | 新建 | 审计日志接口 `void logAudit(SecurityAuditEvent event)` |
| `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/audit/SecurityAuditEvent.java` | 新建 | 审计事件值对象（record），含所有字段 + `now()` 工厂方法 |
| `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/audit/LoggingSecurityAuditLogger.java` | 新建 | Phase 1 Logback 文件实现，使用专用 `SECURITY_AUDIT` logger |
| `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/audit/SecurityAuditEventType.java` | 新建 | 审计事件类型枚举 |
| `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/config/AuthModuleConfig.java` | 修改 | 新增 `@Bean securityAuditLogger()` 注册 `LoggingSecurityAuditLogger` |
| `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/AuthService.java` | 修改 | `logout(String token)` → `logout(String token, String refreshToken)` |
| `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/impl/AuthServiceImpl.java` | 修改 | 注入 `SecurityAuditLogger`，四个方法记录审计事件 |
| `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/controller/AuthController.java` | 修改 | `logout()` 将 refreshToken 传给 `authService.logout(token, refreshToken)` |
| `AIMedical/backend/application/src/main/resources/logback-spring.xml` | 新建 | 配置 SECURITY_AUDIT logger 的 RollingFileAppender |
| `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/audit/LoggingSecurityAuditLoggerTest.java` | 新建 | 测试日志格式和异常降级 |
| `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/service/AuthServiceTest.java` | 修改 | 适配 `logout` 签名变更 + mock SecurityAuditLogger + 验证审计调用 |
| `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/controller/AuthControllerTest.java` | 修改 | 适配 `logout` 调用签名变更 |

## 类型定义

### SecurityAuditEventType

**形态**：enum
**包路径**：`com.aimedical.modules.commonmodule.auth.audit`
**职责**：审计事件类型枚举
```java
public enum SecurityAuditEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    LOGOUT,
    TOKEN_REFRESH_SUCCESS,
    TOKEN_REFRESH_REJECTED,
    PASSWORD_CHANGED,
    ACCOUNT_LOCKED // reserved for future use (Phase 2 account lockout audit)
}
```

### SecurityAuditEvent

**形态**：record
**包路径**：`com.aimedical.modules.commonmodule.auth.audit`
**职责**：审计事件值对象
```java
public record SecurityAuditEvent(
    SecurityAuditEventType eventType,
    Long userId,
    String username,
    String clientIp,
    boolean success,
    String failureReason,
    String refreshTokenMasked,
    String newJti,
    long timestamp
) {
    public static SecurityAuditEvent now(
        SecurityAuditEventType eventType,
        Long userId,
        String username,
        String clientIp,
        boolean success,
        String failureReason,
        String refreshTokenMasked,
        String newJti
    ) {
        return new SecurityAuditEvent(eventType, userId, username, clientIp,
            success, failureReason, refreshTokenMasked, newJti, System.currentTimeMillis());
    }
}
```

**构造方式**：`SecurityAuditEvent.now(eventType, userId, username, clientIp, success, failureReason, refreshTokenMasked, newJti)` 工厂方法自动填充 `timestamp = System.currentTimeMillis()`
**类型关系**：无继承/实现

### SecurityAuditLogger

**形态**：interface
**包路径**：`com.aimedical.modules.commonmodule.auth.audit`
**职责**：安全审计日志记录契约
```java
public interface SecurityAuditLogger {
    void logAudit(SecurityAuditEvent event);
}
```

**公开接口**：
- `void logAudit(SecurityAuditEvent event)` — 记录一条审计事件，不可抛出异常

### LoggingSecurityAuditLogger

**形态**：class
**包路径**：`com.aimedical.modules.commonmodule.auth.audit`
**职责**：Phase 1 日志文件实现，使用专用 SECURITY_AUDIT logger 写入 Logback RollingFileAppender
```java
public class LoggingSecurityAuditLogger implements SecurityAuditLogger {
    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("SECURITY_AUDIT");
    // 无参构造，文件路径完全由 logback-spring.xml 通过 <springProperty> 从 audit.security.file-path 属性解析
}
```

**公开接口**：`void logAudit(SecurityAuditEvent event)` — 构造格式串 `timestamp=<ISO-8601> eventType=<type> userId=<id> username=<name> clientIp=<ip> success=<bool> [failureReason=<reason>] [refreshTokenMasked=<mask>] [newJti=<jti>]`，以 `AUDIT_LOG.info(msg)` 写入
**构造方式**：`AuthModuleConfig` 中 `@Bean` 方法 `new LoggingSecurityAuditLogger()`
**类型关系**：实现 `SecurityAuditLogger`
**行为细节**：catch 所有异常 → `log.warn("Audit log write failed: {}", e.getMessage())`，不向调用方传播

### AuthModuleConfig（变更）

**形态**：class（修改）
**包路径**：`com.aimedical.modules.commonmodule.auth.config`
**变更**：新增 Bean 方法
```java
@Bean
public SecurityAuditLogger securityAuditLogger() {
    return new LoggingSecurityAuditLogger();
}
```

### AuthService（变更）

**形态**：interface（修改）
**包路径**：`com.aimedical.modules.commonmodule.service`
**变更**：`logout` 签名增加 `refreshToken` 参数

| 方法 | 当前签名 | 新签名 |
|------|---------|-------|
| logout | `void logout(String token)` | `void logout(String token, String refreshToken)` |

### AuthController（变更）

**形态**：class（修改）
**包路径**：`com.aimedical.modules.commonmodule.controller`
**变更**：`logout()` 方法将 `refreshTokenRequest` 的 token 值传给 `authService.logout(token, refreshToken)`
```java
authService.logout(token, refreshTokenRequest != null ? refreshTokenRequest.refreshToken() : null);
```

### logback-spring.xml

**形态**：XML 配置
**包路径**：`application/src/main/resources/logback-spring.xml`
**职责**：配置 SECURITY_AUDIT logger 专属 RollingFileAppender

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProperty name="AUDIT_FILE_PATH" source="audit.security.file-path"
                    defaultValue="logs/audit/security-audit.log"/>

    <appender name="SECURITY_AUDIT_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${AUDIT_FILE_PATH}</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${AUDIT_FILE_PATH}.%d{yyyy-MM-dd}.%i</fileNamePattern>
            <maxFileSize>10MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>500MB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>%msg%n</pattern>
        </encoder>
    </appender>

    <logger name="SECURITY_AUDIT" level="INFO" additivity="false">
        <appender-ref ref="SECURITY_AUDIT_FILE"/>
    </logger>
</configuration>
```

**关键设计决策**：
- 使用 `<springProperty>` 桥接 `application.yml` 中的 `audit.security.file-path` 属性，默认值 `logs/audit/security-audit.log`
- 使用 `SizeAndTimeBasedRollingPolicy` 按日期 + 文件大小滚动，单文件上限 10MB，保留 30 天，总容量上限 500MB
- encoder pattern 仅为 `%msg%n`，因为 `LoggingSecurityAuditLogger` 已在 log message 中完成格式化，无需 Logback 额外添加时间戳等前缀

## 错误处理

- `SecurityAuditLogger.logAudit()` 不抛出任何异常；实现层 catch 所有异常后 `log.warn` 降级
- `AuthServiceImpl` 在各方法中调用 `securityAuditLogger.logAudit(event)` 直接传递事件对象，无需 try-catch 包裹
- 登录失败场景中审计日志在 `throw` 之前记录（先 log → 再 throw），确保异常不吞没日志

## 行为契约

### AuthServiceImpl.audit 调用点

| 方法 | 条件 | eventType | failureReason/reason | 补充字段 |
|------|------|-----------|---------------------|---------|
| login | IP锁定 | LOGIN_FAILED | IP_LOCKED | userId=null, username=null, success=false, clientIp=getClientIp() |
| login | 用户名锁定 | LOGIN_FAILED | USERNAME_LOCKED | userId=null, username=null, success=false, clientIp=getClientIp() |
| login | 用户不存在 | LOGIN_FAILED | USER_NOT_FOUND | userId=null, username=null, success=false, clientIp=getClientIp() |
| login | 用户禁用 | LOGIN_FAILED | ACCOUNT_DISABLED | userId=user.getId(), username=user.getUsername(), success=false, clientIp=getClientIp() |
| login | 用户删除 | LOGIN_FAILED | ACCOUNT_DELETED | userId=user.getId(), username=user.getUsername(), success=false, clientIp=getClientIp() |
| login | 密码错误 | LOGIN_FAILED | BAD_CREDENTIALS | userId=user.getId(), username=user.getUsername(), success=false, clientIp=getClientIp() |
| login | 成功 | LOGIN_SUCCESS | null | userId=user.getId(), username=user.getUsername(), success=true, clientIp=getClientIp() |
| logout | 完成(黑名单后) | LOGOUT | null | userId=claims解析, username=claims解析, success=true, refreshTokenMasked, clientIp=getClientIp() |
| refreshToken | 用户名锁定拒绝 | TOKEN_REFRESH_REJECTED | LOCKED | userId=userId, username=user.getUsername(), success=false, clientIp=getClientIp() |
| refreshToken | 用户禁用 | TOKEN_REFRESH_REJECTED | DISABLED | userId=userId, username=user.getUsername(), success=false, clientIp=getClientIp() |
| refreshToken | 用户删除 | TOKEN_REFRESH_REJECTED | DELETED | userId=userId, username=user.getUsername(), success=false, clientIp=getClientIp() |
| refreshToken | tokenVersion不匹配 | TOKEN_REFRESH_REJECTED | TOKEN_VERSION_MISMATCH | userId=userId, username=user.getUsername(), success=false, clientIp=getClientIp() |
| refreshToken | 成功 | TOKEN_REFRESH_SUCCESS | null | userId=userId, username=user.getUsername(), success=true, newJti, clientIp=getClientIp() |
| changePassword | 成功 | PASSWORD_CHANGED | null | userId, username=user.getUsername(), success=true, clientIp=getClientIp() |

### login 审计注意事项

1. IP/USERNAME_LOCKED 在 `ACCOUNT_LOCKED` 异常抛出前记录，因当前无 userId/username，clientIp 从 `getClientIp()` 获取
2. USER_NOT_FOUND 场景用户名不存在，userId=null，username=null，但 clientIp 可用
3. ACCOUNT_DISABLED vs ACCOUNT_DELETED 的判断：`!user.getEnabled()` → ACCOUNT_DISABLED；`user.getDeleted()` → ACCOUNT_DELETED（注意 deleted 只在 enabled 检查之后，两者不会同时触发）
4. 登录成功的审计日志在 return LoginResponse 之前记录

### refreshToken 审计注意事项

1. 当前代码中 user not found / disabled / deleted 使用统一 `TOKEN_REFRESH_FAILED`，需区分审计原因：
   - `userOpt.isEmpty()` → DELETED
   - `!userOpt.get().getEnabled()` → DISABLED
   - `userOpt.get().getDeleted()` → DELETED
2. TOKEN_REFRESH_REJECTED 各原因在对应 `throw` 之前记录
3. TOKEN_REFRESH_SUCCESS 在 return 之前记录，`newJti` 从 `jwtTokenProvider.getJtiFromToken(newAccessToken)` 获取
4. 所有场景 clientIp 均从 `getClientIp()` 获取

### logout 审计注意事项

1. token 为 null 或验证失败时不记录审计事件（静默返回）
2. refreshTokenMasked：取前 8 字符 + "***"，refreshToken 为 null 时不记录此字段
3. userId/username 从 claims 中获取
4. clientIp 从 `getClientIp()` 获取

### changePassword 审计注意事项

1. 审计日志在 `SecurityContextHolder.clearContext()` 之前记录（此时 userId/username 仍可获取）
2. clientIp 从 `getClientIp()` 获取

### logout 方法调用顺序

```java
// AuthController
authService.logout(token, refreshTokenRequest != null ? refreshTokenRequest.refreshToken() : null);

// AuthService.logout 内部顺序
void logout(String token, String refreshToken) {
    if (token == null) return;            // 1. null 校验
    Claims claims = validateToken(token);  // 2. token 校验
    if (claims == null) return;
    String jti = getJtiFromToken(token);  // 3. 黑名单
    if (jti != null) tokenBlacklist.add(jti, expiration);
    // 4. 记录审计日志
    securityAuditLogger.logAudit(SecurityAuditEvent.now(
        LOGOUT, userId, username, getClientIp(), true, null,
        mask(refreshToken), null));
}
```

## 依赖关系

| 依赖方 | 依赖目标 | 注入方式 |
|-------|---------|---------|
| AuthServiceImpl | SecurityAuditLogger | 构造器注入（新增第10个参数） |
| AuthModuleConfig | LoggingSecurityAuditLogger | `@Bean` 方法 `new LoggingSecurityAuditLogger()` |
| AuthController | AuthService.logout() | 签名变更 + 调用方适配 |
| logback-spring.xml | LoggingSecurityAuditLogger | Logger 名称 `SECURITY_AUDIT` + RollingFileAppender（`<springProperty>` 从 `audit.security.file-path` 读取） |
| AuthServiceTest | SecurityAuditLogger | `@Mock` + 构造器注入 |
| AuthControllerTest | AuthService.logout(token, refreshToken) | 调用签名适配 |

## application.yml 属性

```yaml
audit:
  security:
    file-path: logs/audit/security-audit.log
```

## 修订说明（v17 r1）

| 审查意见 | 修改措施 |
|---------|---------|
| **[严重]** LoggingSecurityAuditLogger 构造方式自相矛盾（文件规划表写 @Value 注入，类设计段写无参构造） | 统一为**无参构造** + `logback-spring.xml` 通过 `<springProperty>` 解析 `audit.security.file-path`。`LoggingSecurityAuditLogger` 仅通过 `LoggerFactory.getLogger("SECURITY_AUDIT")` 获取 Logger，路径由 Logback 配置管理。移除设计中所有 `@Value` 引用。 |
| **[一般]** 非 login 方法中 clientIp 来源未指定 | 四类方法（login/logout/refreshToken/changePassword）统一使用 `AuthServiceImpl` 中已有的 `getClientIp()` 私有方法。在行为契约表中为每行补充 clientIp 列，在注意事项中为每个方法段落添加显式说明。 |
| **[一般]** logback-spring.xml 内容未设计 | 新增完整的 logback-spring.xml 设计小节，包含：`<springProperty>` 桥接 `audit.security.file-path`、RollingFileAppender 名称 `SECURITY_AUDIT_FILE`、`SizeAndTimeBasedRollingPolicy`（10MB/30天/500GB上限）、encoder pattern `%msg%n`。 |
| **[轻微]** ACCOUNT_LOCKED 在当前四种方法中均未使用 | 保留枚举值并追加 `// reserved for future use` 注释，供 Phase 2 账号锁定审计事件使用。 |
| **[轻微]** `SecurityAuditEvent.now()` 8 参数工厂方法 | 维持原方案。8 个参数一一对应 record 字段，类型不同不易混淆；Phase 1 无需 Builder 模式增加复杂度。可读性问题由编码 Agent 在调用处通过局部变量/换行解决。 |
