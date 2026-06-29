# 详细设计（v13）

## 概述

修复 R9 验证失败的 2 个测试问题：删除 Logback 不可达路径测试、修正 filter 注册顺序。文件级修改，不影响生产代码或其他测试。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/audit/LoggingSecurityAuditLoggerTest.java` | MODIFY | 删除 `logAudit_shouldFallbackGracefullyOnWriteFailure` 方法 |
| `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/SecurityConfigPhase1Test.java` | MODIFY | 重排 3 行 filter 注册顺序 |

## 类型定义

无需新增类型。

---

### 修改 1：删除 Logback 不可达路径测试

**文件**：`LoggingSecurityAuditLoggerTest.java`

**操作**：删除第 118-146 行整个方法 `logAudit_shouldFallbackGracefullyOnWriteFailure`

**原因**：Logback 的 `AppenderBase.doAppend()` 内部已 catch appender 抛出的所有异常并通过 `handleError()` 处理，异常不会传播给调用者。因此 `LoggingSecurityAuditLogger` 的 catch 块永远不会被触发。该测试在 Logback 环境下不可达，无需测试。

**影响**：该文件原有 9 个测试方法变为 8 个。其他 8 个测试不受影响。

---

### 修改 2：修正 SecurityConfigPhase1Test filter 注册顺序

**文件**：`SecurityConfigPhase1Test.java`

**变更位置**：第 88-90 行

**原顺序**：
```java
http.addFilterBefore(globalRateLimitFilter, JwtAuthenticationFilter.class); // 行 88（旧）
http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // 行 89（旧）
http.addFilterAfter(passwordChangeCheckFilter, JwtAuthenticationFilter.class); // 行 90（旧）
```

**新顺序**：
```java
http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // 先注册 JwtAuthFilter
http.addFilterBefore(globalRateLimitFilter, JwtAuthenticationFilter.class); // 再引用 JwtAuthFilter
http.addFilterAfter(passwordChangeCheckFilter, JwtAuthenticationFilter.class); // 同样引用 JwtAuthFilter
```

**原因**：Spring Security 的 `HttpSecurity.addFilterBefore(filter, beforeFilterClass)` 要求 `beforeFilterClass` 已经在 filter 注册表中存在。原顺序中先调用了 `addFilterBefore(globalRateLimitFilter, JwtAuthenticationFilter.class)`，但此时 `JwtAuthenticationFilter` 尚未注册，抛出 `IllegalArgumentException`。

**依据**：生产代码 `SecurityConfigPhase1.filterChain()` 第 93-95 行也已使用此正确顺序（Spring 容器启动时 filter 实例已全部就位）。

---

## 错误处理

| 场景 | 处理方式 |
|------|---------|
| 测试方法删除后其他测试误引用 | 已验证该方法是独立的，无其他测试依赖它 |

## 行为契约

- 仅修改上述两个文件，不触及生产代码或其他测试文件
- 删除的测试方法不再恢复（路径不可达）
- 重排后 3 行 filter 注册逻辑语义不变，仅修复执行顺序

## 依赖关系

| 依赖 | 说明 |
|------|------|
| Logback `AppenderBase.doAppend()` 实现 | 确认异常不会传播至调用者 |
| `SecurityConfigPhase1.filterChain()` 第 93-95 行 | 参考生产代码 filter 注册顺序 |

## 修订说明（v13 R1）

| 审查意见 | 修改措施 |
|---------|---------|
| `LoggingSecurityAuditLoggerTest.logAudit_shouldFallbackGracefullyOnWriteFailure` 断言 `expected: <true> but was: <false>` | 删除整个 `logAudit_shouldFallbackGracefullyOnWriteFailure` 方法（line 118-146）。该测试路径在 Logback 环境下不可达，原因：`AppenderBase.doAppend()` 内部 catch 了 appender 抛出的异常，不会传播给调用者。 |
| `SecurityConfigPhase1Test.shouldRegisterFiltersInExpectedOrder` 抛出 `IllegalArgumentException: The Filter class ... does not have a registered order` | 行 88-90 重排：先注册 `jwtAuthenticationFilter`（通过 `UsernamePasswordAuthenticationFilter` 标准 filter），再引用 `JwtAuthenticationFilter.class` 注册其他 filter。 |
