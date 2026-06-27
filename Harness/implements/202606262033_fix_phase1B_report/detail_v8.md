# 详细设计（v8）

## 概述

修复 `AuthServiceImpl` 中两个与安全相关的问题：

1. **T5**：`refreshToken()` 方法在用户禁用/删除时直接抛出异常，未调用 `loginAttemptTracker.recordIpFailure(clientIp)`，导致刷新场景下 IP 维度攻击检测不完整。
2. **T13**：`login()` 方法在用户不存在和用户禁用/删除场景中使用 `passwordEncoder.encode("dummy")`，语义不符合"比对"的设计意图，应使用 `matches()` 消除响应时间差异。

仅修改 1 个源文件（`AuthServiceImpl.java`），不涉及测试或其他文件变更。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/impl/AuthServiceImpl.java` | 修改 | T5：`refreshToken()` 中用户禁用/删除时插入 `loginAttemptTracker.recordIpFailure(getClientIp())`；T13：`login()` 中两处 `passwordEncoder.encode("dummy")` 替换为 `passwordEncoder.matches("dummy", DUMMY_HASH)` 并添加常量 `DUMMY_HASH` |

## 类型定义

### 1. AuthServiceImpl 新增常量

**形态**：`private static final String` 字段
**位置**：与已有常量（`REFRESH_WINDOW_MS`、`REFRESH_MAX_COUNT` 等）同区域，约 L44-L51 之间

```java
private static final String DUMMY_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
```

### 2. login() — 用户不存在分支（当前 L104-L108）

**当前代码**：
```java
if (userOpt.isEmpty()) {
    passwordEncoder.encode("dummy");
    loginAttemptTracker.recordIpFailure(clientIp);
    throw new BusinessException(GlobalErrorCode.LOGIN_FAILED);
}
```

**修改后**：将第 105 行 `passwordEncoder.encode("dummy")` 替换为：
```java
passwordEncoder.matches("dummy", DUMMY_HASH);
```

### 3. login() — 用户禁用/删除分支（当前 L112-L117）

**当前代码**：
```java
if (!Boolean.TRUE.equals(user.getEnabled()) || Boolean.TRUE.equals(user.getDeleted())) {
    passwordEncoder.encode("dummy");
    loginAttemptTracker.recordIpFailure(clientIp);
    loginAttemptTracker.recordUsernameFailure(user.getUsername());
    throw new BusinessException(GlobalErrorCode.LOGIN_FAILED);
}
```

**修改后**：将第 113 行 `passwordEncoder.encode("dummy")` 替换为：
```java
passwordEncoder.matches("dummy", DUMMY_HASH);
```

### 4. refreshToken() — 用户不存在/禁用/删除分支（当前 L180-L184）

**当前代码**：
```java
java.util.Optional<User> userOpt = userRepository.findById(userId);
if (userOpt.isEmpty() || !Boolean.TRUE.equals(userOpt.get().getEnabled())
        || Boolean.TRUE.equals(userOpt.get().getDeleted())) {
    throw new BusinessException(GlobalErrorCode.TOKEN_REFRESH_FAILED);
}
```

**修改后**：在 `throw` 之前插入 `loginAttemptTracker.recordIpFailure(getClientIp())`：
```java
java.util.Optional<User> userOpt = userRepository.findById(userId);
if (userOpt.isEmpty() || !Boolean.TRUE.equals(userOpt.get().getEnabled())
        || Boolean.TRUE.equals(userOpt.get().getDeleted())) {
    loginAttemptTracker.recordIpFailure(getClientIp());
    throw new BusinessException(GlobalErrorCode.TOKEN_REFRESH_FAILED);
}
```

## 方法签名说明

| 方法 | 签名 | 来源 |
|------|------|------|
| `loginAttemptTracker.recordIpFailure(String)` | `public void recordIpFailure(String ip)` | `LoginAttemptTracker.java:42`（已有注入字段 `loginAttemptTracker`） |
| `getClientIp()` | `private String getClientIp()` | `AuthServiceImpl.java:298`（已有方法，从 `RequestContextHolder` 获取客户端 IP） |
| `passwordEncoder.matches(CharSequence, String)` | `boolean matches(CharSequence rawPassword, String encodedPassword)` | `PasswordEncoder` 接口（已有注入字段 `passwordEncoder`） |

## 错误处理

- `loginAttemptTracker.recordIpFailure()` 和 `passwordEncoder.matches()` 均不抛出受检异常
- `getClientIp()` 内部捕获所有异常，失败时返回 `"unknown"`（已有实现，L307-L308）
- 所有修改均在 `throw` 之前，不影响原有的异常传播路径

## 行为契约

### T5 — refreshToken() 用户禁用/删除分支

- **前置条件**：`refreshToken()` 被调用，`jwtTokenProvider.validateToken()` 通过，`userId` 非空，`jti` 已获取
- **正常路径跳过条件**：`userOpt.isPresent() && user.getEnabled() == true && user.getDeleted() == false`
- **新增行为**：当用户不存在/禁用/删除时，先调用 `loginAttemptTracker.recordIpFailure(getClientIp())`，再抛出 `BusinessException(TOKEN_REFRESH_FAILED)`
- **后置条件**：IP 失败计数器递增（含 `"unknown"` 情况）

### T13 — login() 两处 dummy 比对

- **前置条件**：`login()` 被调用，`clientIp` 已获取
- **用户不存在分支**：调用 `passwordEncoder.matches("dummy", DUMMY_HASH)`，然后 `recordIpFailure(clientIp)`，然后抛出 `LOGIN_FAILED`
- **用户禁用/删除分支**：调用 `passwordEncoder.matches("dummy", DUMMY_HASH)`，然后 `recordIpFailure(clientIp)`，然后 `recordUsernameFailure(username)`，然后抛出 `LOGIN_FAILED`
- **后置条件**：`matches()` 调用消耗与真实密码比对接近的时间，消除响应时间侧信道差异

## 依赖关系

| 依赖 | 说明 |
|------|------|
| `LoginAttemptTracker.recordIpFailure(String)` | 已有注入字段 `loginAttemptTracker`，无需新增依赖 |
| `PasswordEncoder.matches(CharSequence, String)` | 已有注入字段 `passwordEncoder`，无需新增依赖 |
| `getClientIp()` | 已有私有方法，无需修改 |
| `DUMMY_HASH` 常量 | 类内部新增静态常量，不对外暴露 |

**不受影响的文件**：
| 文件 | 原因 |
|------|------|
| `AuthService.java` | 接口签名不变 |
| 所有测试文件 | 新增行为与原有接口语义一致，无需修改现有测试 |

## 验证方式

```bash
mvn test -pl common-module/common-module-impl -am
```

预期：T5 场景——mock 用户 enabled=false 或 deleted=true，调用 refreshToken，验证 loginAttemptTracker.recordIpFailure() 被调用且参数为合理 IP；T13 场景——对不存在的用户名和禁用用户场景，验证 login() 调用 passwordEncoder.matches() 而非 encode()。
