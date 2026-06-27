# 设计审查报告（v4 r1）

## 审查结果
APPROVED

## 发现

### 实际代码交叉验证摘要

逐文件对照设计、任务文件与实际代码库，确认：

| 文件 | 设计正确性 | 备注 |
|------|-----------|------|
| `JwtTokenProvider.java` | ✅ 正确 | `getTokenType()` 不存在，需新增；`jwtConfig` 字段已存在，委托调用可行 |
| `JwtAuthenticationFilter.java` | ✅ 正确 | 字段/构造/方法变更与源码行号一致；`extractUserId()` 方法体与 `JwtTokenProvider.getUserIdFromClaims()` 完全重复 |
| `SecurityConfigPhase1.java` | ✅ 正确 | Bean 方法参数精确匹配 |
| `JwtAuthenticationFilterTest.java` | ✅ 正确 | 所有 mock 适配、`claims.get("type",...)` 删除、`validateTokenAndGetClaims`→`validateToken` 均覆盖 |
| `SecurityConfigPhase1Test.java` | ✅ 正确 | 字段类型 + Bean 调用处精确匹配 |
| `SecurityConfigPhase1CoexistenceTest.java` | ✅ 正确 | 同上 |

### 关键正确性保障

- `JwtUtil.extractToken()` 是静态方法，import 保留——设计明确标注 ✅
- 删除手动 type 检查（L68-74）后，`claims.get("jti")`/`claims.get("userId")` 仍在后续使用——设计在各测试方法中保留了非 type mock ✅
- `shouldSkipWhenRefreshTokenType` 测试行为等价：原为 `claims.get("type")=="refresh"`→跳过；改为 `validateToken("xxx","access")→null`→跳过，语义一致 ✅

### 轻微观察（不影响批准）

无。
