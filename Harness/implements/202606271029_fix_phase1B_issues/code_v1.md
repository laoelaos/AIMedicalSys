# 实现报告（v1）

## 概述

修复 T1 — Access Token 缺少 `type` claim 问题。在 `JwtTokenProvider.generateAccessToken()` 中添加 `.claim("type", "access")`，并在对应测试中验证该 claim 存在。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/jwt/JwtTokenProvider.java | 在 `generateAccessToken()` 的四参重载中添加 `.claim("type", "access")` |
| 修改 | AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/jwt/JwtTokenProviderTest.java | 1) `generateAccessToken_shouldCreateValidToken()` 新增 type 断言；2) `validateToken_withCorrectType_shouldReturnClaims()` 新增 `validateToken(token, "access")` 断言 |

## 编译验证

`mvn compile test-compile -pl common-module-impl -am -q` 通过，无编译错误。

## 设计偏差说明

无偏差。
