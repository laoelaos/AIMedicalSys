# 测试报告（v8）

## 测试概要

| 项目 | 值 |
|------|-----|
| 执行时间 | 2026-06-26T13:13:33+08:00 |
| 构建工具 | Maven 3.x + Surefire 3.1.2 |
| 测试框架 | JUnit 5 (JUnitPlatformProvider) |
| 总用例数 | 8 |
| 通过 | 8 |
| 失败 | 0 |
| 错误 | 0 |
| 跳过 | 0 |

## 测试结果明细

### PasswordChangeRequiredExceptionTest (3 用例)

| 测试方法 | 结果 | 耗时 |
|---------|------|------|
| `shouldPreserveMessage` | 通过 | — |
| `shouldPreserveCause` | 通过 | — |
| `shouldBeInstanceOfAccessDeniedException` | 通过 | — |

### RestAccessDeniedHandlerTest (2 用例)

| 测试方法 | 结果 | 验证点 |
|---------|------|--------|
| `shouldReturnPasswordChangeRequiredForPasswordChangeException` | 通过 | status=403, contentType=application/json;charset=UTF-8, code=PASSWORD_CHANGE_REQUIRED |
| `shouldReturnForbiddenForGenericAccessDenied` | 通过 | status=403, contentType=application/json;charset=UTF-8, code=FORBIDDEN |

### RestAuthenticationEntryPointTest (3 用例)

| 测试方法 | 结果 | 验证点 |
|---------|------|--------|
| `shouldReturnAccountDisabledWhenMessageMatches` | 通过 | status=401, contentType=application/json;charset=UTF-8, code=ACCOUNT_DISABLED |
| `shouldReturnUnauthorizedForGenericException` | 通过 | status=401, code=UNAUTHORIZED |
| `shouldReturnUnauthorizedWhenMessageIsNull` | 通过 | status=401, code=UNAUTHORIZED (NPE 安全) |

## 构建结果

```
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 测试文件清单

| 文件 | 路径 |
|------|------|
| PasswordChangeRequiredExceptionTest | `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/exception/PasswordChangeRequiredExceptionTest.java` |
| RestAccessDeniedHandlerTest | `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/RestAccessDeniedHandlerTest.java` |
| RestAuthenticationEntryPointTest | `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/RestAuthenticationEntryPointTest.java` |
