# 测试报告（v6）

## 测试文件清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新建 | `common/src/test/java/.../util/SimpleMessageInterpolatorTest.java` | 独立测试 `SimpleMessageInterpolator` 所有插值场景（6 用例） |
| 修改 | `common/src/test/java/.../config/GlobalExceptionHandlerTest.java` | 构造 `GlobalExceptionHandler` 时注入 `SimpleMessageInterpolator`（12 用例） |
| 修改 | `common-module-impl/src/test/java/.../auth/security/RestAuthenticationEntryPointTest.java` | 构造时传入 mock `MessageInterpolator`，新增 message 字段断言（3 用例） |
| 修改 | `common-module-impl/src/test/java/.../auth/security/RestAccessDeniedHandlerTest.java` | 同上（2 用例） |
| 修改 | `common-module-impl/src/test/java/.../auth/security/SecurityConfigPhase1Test.java` | `filterChain()` 调用时传入 mock `MessageInterpolator`（4 用例） |

## 行为契约覆盖

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

## 设计偏差

无偏差。实现与详细设计完全一致。

## 硬性约束确认

- 不修改编码 agent 的源码文件 ✓
- 不依赖执行顺序 ✓
- 每个用例正向至少一个 ✓
