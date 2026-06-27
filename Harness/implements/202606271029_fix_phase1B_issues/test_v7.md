# 测试报告（v7）

## 概述

v7 为纯验证轮次，所有源文件已在 v6 中正确实现，无需新建或修改任何源文件或测试文件。本报告确认 v6 已编写的全部测试文件均已就位，覆盖所有行为契约。

## 测试文件清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 确认 | `common/src/test/java/.../util/SimpleMessageInterpolatorTest.java` | 6 用例，已存在 |
| 确认 | `common/src/test/java/.../config/GlobalExceptionHandlerTest.java` | 12 用例，已存在 |
| 确认 | `common-module-impl/src/test/java/.../auth/security/RestAuthenticationEntryPointTest.java` | 3 用例，已存在 |
| 确认 | `common-module-impl/src/test/java/.../auth/security/RestAccessDeniedHandlerTest.java` | 2 用例，已存在 |
| 确认 | `common-module-impl/src/test/java/.../auth/security/SecurityConfigPhase1Test.java` | 4 用例，已存在 |

## 行为契约覆盖确认

### SimpleMessageInterpolator（6 用例）

| 测试 | 验证内容 | 覆盖维度 |
|------|---------|---------|
| `shouldReturnTemplateWhenArgsNull` | args 为 null 时直接返回 template | 边界 |
| `shouldReturnTemplateWhenArgsEmpty` | args 为空数组时直接返回 template | 边界 |
| `shouldReplaceNumberedPlaceholders` | `{0}`, `{1}` 按序替换（MessageFormat 路径） | 正常路径 |
| `shouldReplaceNamedPlaceholdersByPosition` | `{锁定时间}` 按位置替换（regex 降级路径） | 正常路径 |
| `shouldReuseSameArgForMultiplePlaceholders` | `{0}` 多处出现均被替换 | 正常路径 |
| `shouldReturnTemplateForNoPlaceholdersWithArgs` | 无占位符但有 args 时模板不变 | 边界 |

### GlobalExceptionHandler（12 用例）

涵盖 BusinessException（含动态参数、空参数、编号占位符）、ValidationException、MessageNotReadable、MessageNotWritable、通用 Exception 以及对应日志输出验证。

### RestAuthenticationEntryPoint（3 用例）

| 测试 | 验证内容 | 覆盖维度 |
|------|---------|---------|
| `shouldReturnAccountDisabledWhenMessageMatches` | AccountDisabledAuthenticationException → 401 + ACCOUNT_DISABLED + 插值消息 | 正常路径 |
| `shouldReturnUnauthorizedForGenericException` | 泛化 AuthenticationException → 401 + UNAUTHORIZED + 插值消息 | 正常路径 |
| `shouldReturnUnauthorizedWhenMessageIsNull` | 异常 message 为 null → 401 + UNAUTHORIZED + 插值消息 | 错误路径 |

### RestAccessDeniedHandler（2 用例）

| 测试 | 验证内容 | 覆盖维度 |
|------|---------|---------|
| `shouldReturnPasswordChangeRequiredForPasswordChangeException` | PasswordChangeRequiredException → 403 + PASSWORD_CHANGE_REQUIRED + 插值消息 | 正常路径 |
| `shouldReturnForbiddenForGenericAccessDenied` | 泛化 AccessDeniedException → 403 + FORBIDDEN + 插值消息 | 正常路径 |

### SecurityConfigPhase1（4 用例）

Bean 创建、BCryptPasswordEncoder、JwtAuthenticationFilter 依赖注入、filter 注册顺序。验证 `MessageInterpolator` 通过 filterChain 参数传入两个 handler。

## 设计偏差确认

无偏差。所有现有测试文件已覆盖 v7 设计规格中列出的全部类型和契约。

## 硬性约束确认

- 不修改编码 agent 的源码文件 ✓
- 不依赖执行顺序 ✓
- 每个用例正向至少一个 ✓
- 所有测试文件均为 v6 已建，v7 无新增 ✓
