# 测试报告（v8）

## 变更概要

更新 `AuthServiceTest.java` 中 3 个现有测试，新增 1 个测试。

## 测试文件

`common-module-impl/src/test/java/com/aimedical/modules/commonmodule/service/AuthServiceTest.java`

## 修改清单

| 测试方法 | 操作 | 说明 |
|---------|------|------|
| `login_shouldThrowUserNotFound` | 修改 | `verify(passwordEncoder).encode("dummy")` → `verify(passwordEncoder).matches(eq("dummy"), anyString())` |
| `login_shouldThrowUserDisabled` | 修改 | `verify(passwordEncoder).encode("dummy")` → `verify(passwordEncoder).matches(eq("dummy"), anyString())` |
| `refreshToken_shouldThrowOnDisabledUser` | 修改 | 新增 `verify(loginAttemptTracker).recordIpFailure(anyString())`，补充 `getJtiFromToken` mock |
| `refreshToken_shouldThrowOnUserNotFound` | 新增 | 验证用户不存在时抛出 `TOKEN_REFRESH_FAILED` 并调用 `recordIpFailure` |

## 行为契约覆盖

### T5 — refreshToken() IP 失败记录

| 场景 | 测试方法 | 验证点 |
|------|---------|--------|
| 用户禁用 | `refreshToken_shouldThrowOnDisabledUser` | `recordIpFailure` 被调用 |
| 用户不存在 | `refreshToken_shouldThrowOnUserNotFound` | `recordIpFailure` 被调用 |

### T13 — login() dummy 比对

| 场景 | 测试方法 | 验证点 |
|------|---------|--------|
| 用户不存在 | `login_shouldThrowUserNotFound` | `matches` 被调用 |
| 用户禁用 | `login_shouldThrowUserDisabled` | `matches` 被调用 |

## 不修改的文件

- `AuthServiceImpl.java`（编码 agent 源码，禁止修改）
