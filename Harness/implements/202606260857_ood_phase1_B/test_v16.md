# 测试报告（v16）

## 概述

对 `AuthServiceImpl` 执行单元测试，验证 6 个认证方法的行为契约，共 21 个用例全数通过。

## 执行结果

| 项 | 值 |
|---|-----|
| 运行环境 | Windows, OpenJDK 17 |
| 构建工具 | Maven 3.9+ |
| 测试框架 | JUnit 5 + Mockito |
| 测试类 | `AuthServiceTest` |
| 测试总数 | 21 |
| 通过 | 21 |
| 失败 | 0 |
| 错误 | 0 |
| 跳过 | 0 |
| 耗时 | 2.126 s |

## 测试用例明细

### login（8 用例）

| # | 测试方法 | 场景 | 结果 |
|---|---------|------|------|
| 1 | `login_shouldSucceed` | 正常登录 | PASS |
| 2 | `login_shouldThrowRateLimited` | IP 限流触发 | PASS |
| 3 | `login_shouldThrowIpLocked` | IP 维度锁定 | PASS |
| 4 | `login_shouldThrowUsernameLocked` | 用户名维度锁定 | PASS |
| 5 | `login_shouldThrowUserNotFound` | 用户不存在（含 dummy BCrypt 验证） | PASS |
| 6 | `login_shouldThrowUserDisabled` | 用户已禁用（含 dummy BCrypt、双维度 recordFailure） | PASS |
| 7 | `login_shouldThrowPasswordMismatch` | 密码错误 | PASS |
| 8 | `login_shouldSetPasswordChangeRequired` | 首次登录需改密 | PASS |

### refreshToken（7 用例）

| # | 测试方法 | 场景 | 结果 |
|---|---------|------|------|
| 9 | `refreshToken_shouldSucceed` | 正常刷新 | PASS |
| 10 | `refreshToken_shouldThrowOnInvalidToken` | Refresh Token 无效 | PASS |
| 11 | `refreshToken_shouldThrowOnDisabledUser` | 用户已禁用 | PASS |
| 12 | `refreshToken_shouldThrowOnTokenVersionMismatch` | tokenVersion 不一致 | PASS |
| 13 | `refreshToken_shouldThrowPasswordChangeRequired` | passwordChangeRequired=true | PASS |
| 14 | `refreshToken_shouldThrowOnUsernameLocked` | 用户名维度锁定 | PASS |
| 15 | `refreshToken_shouldThrowOnTokenVersionNotFound` | tokenVersion 数据不存在 | PASS |

### logout（1 用例）

| # | 测试方法 | 场景 | 结果 |
|---|---------|------|------|
| 16 | `logout_shouldBlacklistToken` | 正常登出，jti 加入黑名单 | PASS |

### getCurrentUser（1 用例）

| # | 测试方法 | 场景 | 结果 |
|---|---------|------|------|
| 17 | `getCurrentUser_shouldSucceed` | 正常获取当前用户 | PASS |

### updateProfile（1 用例）

| # | 测试方法 | 场景 | 结果 |
|---|---------|------|------|
| 18 | `updateProfile_shouldUpdateFieldsWithoutSave` | 更新资料，脏检查自动持久化 | PASS |

### changePassword（3 用例）

| # | 测试方法 | 场景 | 结果 |
|---|---------|------|------|
| 19 | `changePassword_shouldSucceed` | 正常改密（含 SecurityContextHolder.clearContext 验证） | PASS |
| 20 | `changePassword_shouldThrowOnOldPasswordMismatch` | 旧密码错误 | PASS |
| 21 | `changePassword_shouldThrowOnPolicyViolation` | 密码策略不满足 | PASS |

## 测试命令

```bash
mvn test -pl modules/common-module/common-module-impl -am \
  -Dtest="AuthServiceTest" -Dsurefire.failIfNoSpecifiedTests=false
```

## 审核总结

- **测试覆盖**：6 个方法、21 个行为契约，覆盖正常路径、边界条件、错误路径、状态交互
- **关键验证点**：
  - dummy BCrypt 时序防护（用例 5-6）
  - 双维度 recordFailure（用例 6）
  - `save()` 未被调用（用例 18）
  - `SecurityContextHolder.clearContext()` 被调用（用例 19）
  - tokenVersion 一致性校验（用例 12）
- **独立性**：所有用例使用 `@Mock` + `@BeforeEach` 初始化，无执行顺序依赖
- **设计偏差**：无偏差，实现与详细设计 v16 完全一致
