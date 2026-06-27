# 任务指令（v1）

## 动作
NEW

## 任务描述
修复 T1 — Access Token 缺少 `type` claim 导致 JwtAuthenticationFilter 拒绝所有已认证请求。

具体修改：
1. **JwtTokenProvider.java**（`AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/jwt/JwtTokenProvider.java`）：在 `generateAccessToken()` 方法（第 52-63 行）的 claims 构建中添加 `.claim("type", "access")`
2. **JwtTokenProviderTest.java**（`AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/jwt/JwtTokenProviderTest.java`）：更新现有测试用例，验证 Access Token 包含 type="access" claim

## 选择理由
P0 阻塞级缺陷。无此修复，所有携带有效 Access Token 的请求均被 Filter 当做未认证处理（validateToken 返回 null → 清除 SecurityContext → chain.doFilter 放行），系统核心功能不可用。

## 任务上下文
- `JwtTokenProvider.generateAccessToken()` 当前缺少 type claim，而 Refresh Token 的 `generateRefreshToken()` 已包含 `.claim("type", "refresh")`
- `JwtTokenProvider.validateToken(token, "access")` 在第 96 行执行 `claims.get("type", String.class)` 并与 expectedType 比对
- `JwtAuthenticationFilter.doFilterInternal()` 第 61 行调用 `validateToken(token, "access")`，返回 null 时清除 SecurityContext

## 已有代码上下文
```java
// JwtTokenProvider.java:52-63 — 当前 Access Token 生成（缺少 type claim）
public String generateAccessToken(Long userId, String username, String userType, String jti) {
    long now = System.currentTimeMillis();
    return Jwts.builder()
            .subject(username)
            .claim("userId", userId)
            .claim("userType", userType)
            .claim("jti", jti)
            .issuedAt(new Date(now))
            .expiration(new Date(now + ACCESS_TOKEN_EXPIRATION_MS))
            .signWith(secretKey)
            .compact();
}

// JwtTokenProvider.java:69-82 — Refresh Token 生成（已有 type claim 作为参考）
public String generateRefreshToken(Long userId, String username, String userType, int tokenVersion, String jti) {
    ...
    .claim("type", "refresh")
    ...
}

// JwtTokenProvider.java:88-105 — validateToken 中的 type 校验
public Claims validateToken(String token, String expectedType) {
    ...
    if (expectedType != null) {
        String type = claims.get("type", String.class);
        if (!expectedType.equals(type)) {
            return null;
        }
    }
    ...
}
```

## 测试影响
- `JwtTokenProviderTest.generateAccessToken_shouldCreateValidToken()`：需补充验证 `claims.get("type")` 返回 `"access"`
- `JwtTokenProviderTest.validateToken_withCorrectType_shouldReturnClaims`：可新增参数化场景验证 `validateToken(token, "access")` 返回非 null

## 修订说明（v1 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] task_v1.md 文件路径缺失 `AIMedical/` 前缀 | 所有文件路径补充 `AIMedical/` 前缀：`backend/modules/...` → `AIMedical/backend/modules/...` |
| [一般] T2-OOD 归属存在双重分配（R2 的"OOD + 代码"与 R8 的"T2-OOD"交叉） | 采用方案B：R2 仅实现 T2 代码变更，R2 标注从"（OOD + 代码）"改为"（代码）"；T2-OOD 文档更新推迟至 R8 统一处理 |
