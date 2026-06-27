# 实现报告（v4）

## 概述

完成 JwtAuthenticationFilter 从 JwtUtil 依赖切换为 JwtTokenProvider 的改造，涉及 3 个源文件修改和 3 个测试文件修改。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `common-module-impl/.../auth/jwt/JwtTokenProvider.java` | 新增 `getTokenType()` 方法，委托至 `jwtConfig.getTokenType()` |
| 修改 | `common-module-impl/.../auth/security/JwtAuthenticationFilter.java` | 字段/构造从 `JwtUtil` 切换为 `JwtTokenProvider`；`validateTokenAndGetClaims` → `validateToken(token, "access")`；删除手动 type 检查块；`extractUserId` → `jwtTokenProvider.getUserIdFromClaims`；删除 `extractUserId` 方法 |
| 修改 | `common-module-impl/.../auth/security/SecurityConfigPhase1.java` | Bean 方法参数从 `JwtUtil` 改为 `JwtTokenProvider`；import 替换 |
| 修改 | `common-module-impl/.../auth/security/JwtAuthenticationFilterTest.java` | 字段/构造从 `JwtUtil` 切换为 `JwtTokenProvider`；所有 mock 方法同步更新；删除 `claims.get("type")` mock；`claims.get("userId")` → `jwtTokenProvider.getUserIdFromClaims(claims)` |
| 修改 | `common-module-impl/.../auth/security/SecurityConfigPhase1Test.java` | 字段类型从 `JwtUtil` 改为 `JwtTokenProvider`；Bean 调用参数同步 |
| 修改 | `common-module-impl/.../auth/security/SecurityConfigPhase1CoexistenceTest.java` | 字段类型从 `JwtUtil` 改为 `JwtTokenProvider`；Bean 调用参数同步 |

## 编译验证

- `mvn compile -pl common-module-impl -am` — 通过
- `mvn test-compile -pl common-module-impl -am` — 通过

## 设计偏差说明

无偏差。测试代码中 `claims.get("userId")` → `jwtTokenProvider.getUserIdFromClaims(claims)` 的 mock 变更为 Filter 代码修改的必然连带调整，设计未明确列出但属正确实现。
