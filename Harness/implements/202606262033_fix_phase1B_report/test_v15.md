# 测试报告（v15）

## 测试文件清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新建 | `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/exception/AccountDisabledAuthenticationExceptionTest.java` | 账户禁用异常单元测试，继承 `PasswordChangeRequiredExceptionTest` 模式 |
| 已有（已由编码Agent修改） | `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/RestAuthenticationEntryPointTest.java` | `shouldReturnAccountDisabledWhenMessageMatches` 使用真实 `AccountDisabledAuthenticationException` 实例 |
| 已有（未修改） | `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/JwtAuthenticationFilterTest.java` | `shouldThrowAccountDisabledWhenUserDisabled` 中 `assertThrows(AuthenticationException.class, ...)` 因继承关系自动兼容 |

## AccountDisabledAuthenticationExceptionTest

| 测试方法 | 覆盖维度 | 说明 |
|---------|---------|------|
| `shouldPreserveMessage` | 正常路径 | 验证单参构造器正确保存消息 |
| `shouldPreserveCause` | 正常路径 | 验证双参构造器正确保存原因 |
| `shouldBeInstanceOfAuthenticationException` | 类型关系 | 验证继承自 `AuthenticationException` |

## RestAuthenticationEntryPointTest（已修改）

| 测试方法 | 修改前 | 修改后 |
|---------|--------|--------|
| `shouldReturnAccountDisabledWhenMessageMatches` | `mock(AuthenticationException.class)` + `when(getMessage()).thenReturn(...)` | `new AccountDisabledAuthenticationException("any message")` |
| `shouldReturnUnauthorizedForGenericException` | 不变 | 不变 |
| `shouldReturnUnauthorizedWhenMessageIsNull` | 不变 | 不变 |

## 行为契约覆盖

| 行为契约 | 正向用例 | 边界条件 | 错误路径 | 状态交互 |
|---------|---------|---------|---------|---------|
| AccountDisabledAuthenticationException | `shouldPreserveMessage`, `shouldPreserveCause`, `shouldBeInstanceOfAuthenticationException` | — | — | — |
| JwtAuthenticationFilter.throwAccountDisabled | `JwtAuthenticationFilterTest.shouldThrowAccountDisabledWhenUserDisabled` | — | — | — |
| RestAuthenticationEntryPoint.commence | `shouldReturnAccountDisabledWhenMessageMatches` | `shouldReturnUnauthorizedWhenMessageIsNull` | `shouldReturnUnauthorizedForGenericException` | — |
