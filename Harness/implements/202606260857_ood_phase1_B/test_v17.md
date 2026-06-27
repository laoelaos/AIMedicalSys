# 测试报告（v17）

## 概述

重构 `AuthController`（移除 `JwtUtil` 编译期依赖，内联 `extractToken()`，简化构造函数）后的单元测试验证。

## 测试文件

`modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/controller/AuthControllerTest.java`

## 测试框架

JUnit 5 + Mockito（`@ExtendWith(MockitoExtension.class)`）

## 测试组织结构

| Nested class | 端点 | 用例数 |
|---|---|---|
| `LoginTests` | `POST /api/auth/login` | 2 |
| `LogoutTests` | `POST /api/auth/logout` | 4 |
| `RefreshTests` | `POST /api/auth/refresh` | 2 |
| `MeTests` | `GET /api/auth/me` | 5 |
| `UpdateMeTests` | `PUT /api/auth/me` | 2 |
| `ChangePasswordTests` | `PUT /api/auth/password` | 1 |
| **总计** | | **16** |

## 覆盖维度

- ✅ 正常路径：每个端点至少一个成功用例
- ✅ 边界条件：空 auth header、仅有 `"Bearer "` 前缀、非 Bearer 前缀（`"Basic xxx"`）
- ✅ 错误路径：登录失败抛出 `BusinessException`、刷新 token 无效抛出 `BusinessException`、无 token 返回 `UNAUTHORIZED`
- ✅ 状态交互：`extractToken()` 内联后通过公开端点验证全部 5 种输入情况（`null` / `""` / `"Bearer abc123"` / `"Basic abc123"` / `"Bearer "`）

## 执行结果

```text
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 构造变更验证

- 构造函数调用：`new AuthController(authService, jwtTokenProvider)` — 已验证通过
- `JwtUtil` / `JwtConfig` 引用：已移除 — 编译通过
- 原有 11 个测试用例：全部保留并通过
- 新增 5 个测试用例：覆盖 `extractToken()` 行为契约边界条件 — 全部通过
