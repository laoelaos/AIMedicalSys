# 详细设计（v1）

## 概述

修复 T1 — Access Token 缺少 `type` claim 导致 JwtAuthenticationFilter 拒绝所有已认证请求。在 `JwtTokenProvider.generateAccessToken()` 的 claims 构建中添加 `.claim("type", "access")`，并更新对应测试验证该 claim 存在。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/jwt/JwtTokenProvider.java` | 修改 | 在 `generateAccessToken()` 的 Jwts.builder() 链中添加 type claim |
| `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/jwt/JwtTokenProviderTest.java` | 修改 | 更新现有测试验证 Access Token 包含 type="access" |

## 类型定义

无新增类型。修改涉及已有类的方法内部实现。

### JwtTokenProvider (已有类)

**修改方法**：`generateAccessToken(Long userId, String username, String userType, String jti)`

**变更**：在 builder 链中 `.claim("jti", jti)` 之后、`.issuedAt(...)` 之前插入 `.claim("type", "access")`

**修改后方法链顺序**：
```
.subject(username)
.claim("userId", userId)
.claim("userType", userType)
.claim("jti", jti)
.claim("type", "access")         ← 新增
.issuedAt(new Date(now))
.expiration(new Date(now + ACCESS_TOKEN_EXPIRATION_MS))
.signWith(secretKey)
.compact()
```

**方法签名不变**：`public String generateAccessToken(Long userId, String username, String userType, String jti)`

**重载方法** `generateAccessToken(Long userId, String username, String userType)` 无变化（委托至四参版本，自动继承 type claim）。

### JwtTokenProviderTest (已有测试类)

**修改方法**：
1. `generateAccessToken_shouldCreateValidToken()`
   - 在现有断言块内新增：`assertEquals("access", claims.get("type"))`
   - 位置：在 `assertEquals("test-jti", claims.get("jti"))` 之后
   - 不改变现有断言，仅增加一条断言

2. `validateToken_withCorrectType_shouldReturnClaims()`
   - 当前传入 `validateToken(token, null)` 验证 token 基本结构
   - 新增一个测试方法（或增强本方法）验证 `validateToken(token, "access")` 返回非 null
   - 建议：在当前方法末尾增加 `assertNotNull(jwtTokenProvider.validateToken(token, "access"))`，或新增独立的 `validateToken_withAccessType_shouldReturnClaims()` 方法

## 错误处理

无变更。`validateToken()` 在 type 不匹配时返回 null 的行为不变。

## 行为契约

- `generateAccessToken()` 生成的 token 必须包含 `type` claim 且值为 `"access"`
- `validateToken(token, "access")` 在 token 包含 type="access" 时必须返回非 null Claims
- 修改不影响 Refresh Token 的 `generateRefreshToken()` 行为（已包含 type="refresh"）
- 修改不影响 `validateToken()` 的过期/签名验证逻辑

## 依赖关系

- 依赖：`io.jsonwebtoken.Jwts.builder().claim(...)` API（jjwt 库，已使用）
- 无新增外部依赖
- 无暴露给后续任务的新接口
