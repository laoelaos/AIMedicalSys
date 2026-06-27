# 测试报告（v9）

## 执行结果

| 项目 | 值 |
|------|-----|
| 被测类 | `JwtAuthenticationFilter` |
| 测试类 | `JwtAuthenticationFilterTest` |
| 测试框架 | JUnit 5 + Mockito |
| 测试总数 | 9 |
| 通过 | 9 |
| 失败 | 0 |
| 错误 | 0 |
| 跳过 | 0 |
| 耗时 | 1.785 s |

## 测试用例清单

| # | 测试方法 | 覆盖契约 | 结果 |
|---|---------|---------|------|
| 1 | `shouldSkipWhenNoAuthHeader` | 步骤1：Authorization header 缺失 → 静默跳过 | ✅ |
| 2 | `shouldSkipWhenInvalidToken` | 步骤3a：token 验证失败（claims=null）→ 放行 | ✅ |
| 3 | `shouldSkipWhenRefreshTokenType` | 步骤3b：token type=refresh → 放行，不检查黑名单 | ✅ |
| 4 | `shouldSkipWhenTokenBlacklisted` | 步骤4：jti 在黑名单中 → 放行 | ✅ |
| 5 | `shouldSkipWhenUserNotFound` | 步骤5-6：userId 正常但用户不存在 → 放行 | ✅ |
| 6 | `shouldThrowAccountDisabledWhenUserDisabled` | 步骤7：用户被禁用 → 抛出 AuthenticationException | ✅ |
| 7 | `shouldAuthenticateSuccessfully` | 步骤8-11：全部验证通过 → 认证成功，权限空集 | ✅ |
| 8 | `shouldSetPasswordChangeRequiredAttribute` | 步骤8：passwordChangeRequired=true → 设置 request attribute | ✅ |
| 9 | `shouldPopulateAuthoritiesFromRolesAndFunctions` | 步骤9-10：2 roles + 2 posts×2 functions → 6 个权限（Set 去重） | ✅ |

## 覆盖维度

| 维度 | 覆盖情况 |
|------|---------|
| 正常路径 | 用例 7（全流程成功）、8（passwordChangeRequired）、9（权限装配） |
| 边界条件 | 用例 9（函数跨 post 重复 → Set 去重） |
| 错误路径 | 用例 1（无 header）、2（无效 token）、3（refresh type）、4（黑名单）、5（用户不存在）、6（用户禁用） |
| 状态交互 | `@AfterEach tearDown` 清理 SecurityContext，用例间隔离 |

## 测试文件

`modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/JwtAuthenticationFilterTest.java`
