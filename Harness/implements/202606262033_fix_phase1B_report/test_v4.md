# 测试报告（v4）

## 概述

完成 JwtAuthenticationFilter 依赖切换改造的测试适配。包含 1 个测试文件新增和 3 个测试文件适配验证。

## 测试文件变更

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `JwtAuthenticationFilterTest.java` | 字段/构造从 `JwtUtil` 切换为 `JwtTokenProvider`；mock 方法同步更新；删除 `claims.get("type")` mock；`claims.get("userId")` → `jwtTokenProvider.getUserIdFromClaims(claims)` |
| 修改 | `SecurityConfigPhase1Test.java` | 字段类型从 `JwtUtil` 改为 `JwtTokenProvider`；Bean 调用参数同步 |
| 修改 | `SecurityConfigPhase1CoexistenceTest.java` | 字段类型从 `JwtUtil` 改为 `JwtTokenProvider`；Bean 调用参数同步 |
| 修改 | `JwtTokenProviderTest.java` | 新增 `getTokenType_shouldReturnValueFromJwtConfig` 和 `getTokenType_shouldReflectChangesToJwtConfig` 两个测试方法 |

## JwtTokenProviderTest 新增测试

### getTokenType_shouldReturnValueFromJwtConfig
- **契约**：`getTokenType()` 返回 `jwtConfig.getTokenType()` 的当前值
- **正向用例**：验证默认值 `"Bearer"` 被正确返回
- **覆盖维度**：正常路径

### getTokenType_shouldReflectChangesToJwtConfig
- **契约**：`getTokenType()` 反映 `jwtConfig` 中 `tokenType` 的变更
- **正向用例**：修改 `jwtConfig.tokenType` 为 `"CustomType"`，验证返回值同步更新
- **覆盖维度**：状态交互/配置变更

## JwtAuthenticationFilterTest 适配明细

| 测试方法 | 变更内容 |
|---------|---------|
| `shouldSkipWhenNoAuthHeader` | 无变更 |
| `shouldSkipWhenInvalidToken` | `jwtUtil.getTokenType()` → `jwtTokenProvider.getTokenType()`; `jwtUtil.validateTokenAndGetClaims("xxx")` → `jwtTokenProvider.validateToken("xxx", "access")` |
| `shouldSkipWhenRefreshTokenType` | 删除 `claims` mock 变量和 `claims.get("type")` mock；`validateToken` 返回 null（Provider 拒绝 refresh type） |
| `shouldSkipWhenTokenBlacklisted` | Mock 更新；删除 `claims.get("type")` mock |
| `shouldSkipWhenUserNotFound` | Mock 更新；`extractUserId` → `getUserIdFromClaims` |
| `shouldThrowAccountDisabledWhenUserDisabled` | Mock 更新；删除 `claims.get("type")` mock |
| `shouldAuthenticateSuccessfully` | Mock 更新；删除 `claims.get("type")` mock |
| `shouldSetPasswordChangeRequiredAttribute` | Mock 更新；删除 `claims.get("type")` mock |
| `shouldPopulateAuthoritiesFromRolesAndFunctions` | Mock 更新；删除 `claims.get("type")` mock |

## 测试覆盖分析

### JwtTokenProvider.getTokenType()
- ✅ 正向：返回默认值 "Bearer"
- ✅ 正向：返回自定义值（配置变更后）
- 边界：null 值（JwtConfig.tokenType 默认为 "Bearer"，Spring 绑定不可能为 null，不测）

### JwtAuthenticationFilter 行为契约
- ✅ `validateToken(token, "access")` 返回 null → filter 跳过（covered by `shouldSkipWhenInvalidToken` 和 `shouldSkipWhenRefreshTokenType`）
- ✅ `validateToken(token, "access")` 返回 claims → filter 继续（covered by `shouldSkipWhenTokenBlacklisted` 等）
- ✅ `getUserIdFromClaims(claims)` → filter 继续（covered by `shouldSkipWhenUserNotFound` 等）
- ✅ 删除手动 type 检查：等价行为由 Provider 保证（`shouldSkipWhenRefreshTokenType` 验证 refresh token 被跳过）

### SecurityConfigPhase1 Bean 创建
- ✅ `jwtAuthenticationFilter(jwtTokenProvider, ..., ...)` 正常创建（两个测试均覆盖）
