# 单元测试报告（v15）

## 测试摘要

| 项目 | 值 |
|------|-----|
| 执行日期 | 2026-06-26 |
| 测试框架 | JUnit 5 (JUnitPlatform) |
| 构建工具 | Maven 3.11.0 / Surefire 3.1.2 |
| 测试命令 | `mvn test -pl modules/common-module/common-module-impl -am -Dtest="...auth.jwt.JwtTokenProviderTest,...auth.converter.UserConverterTest,...auth.config.AuthModuleConfigTest"` |

## 修订记录

| 轮次 | 操作 | 说明 |
|------|------|------|
| r1 | 审查 REJECTED → 修订通过 | 将 `toUserInfoResponse_shouldMapPositionFromFirstPost` 中的 `HashSet` 替换为 `LinkedHashSet`，确保 posts 迭代顺序确定 |

## 测试结果

| 测试类 | 总数 | 通过 | 失败 | 错误 | 跳过 | 耗时 |
|--------|------|------|------|------|------|------|
| `JwtTokenProviderTest` | 8 | 8 | 0 | 0 | 0 | 0.473 s |
| `UserConverterTest` | 5 | 5 | 0 | 0 | 0 | 0.016 s |
| `AuthModuleConfigTest` | 3 | 3 | 0 | 0 | 0 | 0.051 s |
| **合计** | **16** | **16** | **0** | **0** | **0** | **0.540 s** |

**构建结果**: BUILD SUCCESS

## 测试用例清单

### JwtTokenProviderTest（8 用例）

| # | 测试方法 | 结果 | 验证要点 |
|---|---------|------|---------|
| 1 | `generateAccessToken_shouldCreateValidToken` | ✅ 通过 | sub=username, userId=1L, userType=DOCTOR, jti=test-jti, exp-iat=900s |
| 2 | `generateRefreshToken_shouldContainTypeAndVersion` | ✅ 通过 | type=refresh, tokenVersion=0 |
| 3 | `validateToken_withCorrectType_shouldReturnClaims` | ✅ 通过 | expectedType=null 跳过类型检查，返回非 null Claims |
| 4 | `validateToken_withWrongType_shouldReturnNull` | ✅ 通过 | refresh token + expectedType="access" → null |
| 5 | `validateToken_withExpiredToken_shouldReturnNull` | ✅ 通过 | 手动构造已过期 token → validateToken 返回 null |
| 6 | `getUserIdFromClaims_withInteger_shouldReturnLong` | ✅ 通过 | userId=Integer(1) → 1L |
| 7 | `getUserIdFromClaims_withLong_shouldReturnLong` | ✅ 通过 | userId=Long(1L) → 1L |
| 8 | `getJtiFromToken_shouldReturnCorrectJti` | ✅ 通过 | jti 提取与传入值一致 |

### UserConverterTest（5 用例）

| # | 测试方法 | 结果 | 验证要点 |
|---|---------|------|---------|
| 1 | `toUserInfoResponse_shouldMapBasicFields` | ✅ 通过 | 基本字段映射完整；无 roles/posts 时 role/position 为 "", permissions 为空 Set |
| 2 | `toUserInfoResponse_shouldMapRoleBySortPriority` | ✅ 通过 | Role.sort 升序取主角色 (sort=1 → "doctor") |
| 3 | `toUserInfoResponse_shouldMapRoleToEmptyWhenNoRoles` | ✅ 通过 | roles=null → role="" |
| 4 | `toUserInfoResponse_shouldMapPositionFromFirstPost` | ✅ 通过 | posts 中取第一个元素 code |
| 5 | `toUserInfoResponse_shouldCollectPermissions` | ✅ 通过 | posts→functions 聚合；包含 "menu:view"、"user:create"；size=2 |

### AuthModuleConfigTest（3 断言）

| # | 测试方法 | 结果 | 验证要点 |
|---|---------|------|---------|
| 1 | `rateLimitGuard_shouldReturnNonNullInstance` | ✅ 通过 | `rateLimitGuard()` 返回非 null |
| 2 | `tokenBlacklist_shouldReturnNonNullInstance` | ✅ 通过 | `tokenBlacklist()` 返回非 null |
| 3 | `loginAttemptTracker_shouldReturnNonNullInstance` | ✅ 通过 | `loginAttemptTracker()` 返回非 null |

## 设计与实现偏差检查

对照 `detail_v15.md` 与源码及测试，**无偏差**：

- `JwtTokenProvider`: 全部 8 个公开方法签名、行为契约、错误处理策略与设计一致
- `UserConverter`: `toUserInfoResponse` 转换规则（字段映射、角色排序、权限聚合双路径）与设计一致
- `AuthModuleConfig`: 3 个 @Bean 方法签名与设计一致
- 测试用例数量与覆盖维度符合设计要求（8+5+3=16 用例全部通过）
