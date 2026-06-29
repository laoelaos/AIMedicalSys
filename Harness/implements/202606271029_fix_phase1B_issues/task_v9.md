# 任务指令（v9）

## 动作
NEW

## 任务描述
实现 R7 三文件独立编码修复：T12（JwtTokenProvider URL-safe Base64 校验）、T14（JwtUtil.generateToken 遗留 claims 清理并添加 jti）、T16（SimpleMessageInterpolator 命名占位符优化，跳过不必要的 MessageFormat 异常开销）

## 选择理由
R6（T13+T15+T19）已全部通过验证（629 tests, 0 failures）。按计划推进下一组 P2 编码修复。三个任务（T12、T14、T16）修改独立文件，无交叉依赖，适合同一轮次实施。

## 任务上下文

### T12: JwtTokenProvider Base64 URL-safe 校验

**问题**（诊断报告 T12）：
- `JwtTokenProvider.init()`（`JwtTokenProvider.java:37-38`）使用 `secret.matches("^[A-Za-z0-9+/]+=*$")` — 标准 Base64 字符集（含 `+`、`/`、`=`）
- OOD 4.7 节要求使用 URL-safe 字符集（`A-Z a-z 0-9 - _`）
- `Base64.getDecoder()` 需同步改为 `Base64.getUrlDecoder()`

**修改**：
1. 正则改为 `^[A-Za-z0-9_\\-]+$`（URL-safe，使用 `-` 和 `_` 替换 `+` 和 `/`，不含 `=` 因为 URL-safe 解码器自动处理 padding）
2. `Base64.getDecoder()` → `Base64.getUrlDecoder()`
3. 错误消息更新为 "JWT secret contains invalid URL-safe Base64 characters"

**测试变更**：
- `shouldThrowWhenSecretContainsInvalidChars`：测试数据改为 `"test-secret!!!"`（`!` 不属于 URL-safe 字符集），断言消息改为包含 "URL-safe"

### T14: JwtUtil.generateToken 遗留 claims 清理

**问题**（诊断报告 T14）：
- `JwtUtil.generateToken()` 仍包含 `role` 和 `position` claims（违反 OOD 3.2 节 Access Token 不含角色/权限 claim 的要求）
- 缺少 jti claim（违反 OOD 3.2 节 Access Token 必须含 jti 的要求）
- 该方法仅被 JwtUtilTest 引用，无生产代码调用方

**修改**：
1. `generateToken()` 中移除 `claims.put("role", role)` 和 `claims.put("position", position)` 及对应参数
2. 添加 `claims.put("jti", UUID.randomUUID().toString())`
3. 保持方法签名不变（向后兼容），添加 `@Deprecated` 注解
4. `getRole()` 和 `getPosition()` 方法保留不动（仅作为 claims 读取器，无害）

**测试变更**：
- `shouldGenerateTokenWithPosition`：移除（position claim 不再写入）
- `shouldParseValidToken`：不再断言 `claims.get("role")` 存在（role claim 不再写入）
- `shouldGetRoleFromValidToken`：更新为验证新生成 token 中 role 为 null

### T16: SimpleMessageInterpolator 命名占位符优化

**问题**（诊断报告 T16）：
- `SimpleMessageInterpolator.interpolate()` 优先尝试 `MessageFormat.format(template, args)`
- 对于命名占位符模板（如 `"账户已锁定，请{锁定时间}后重试"`），`{锁定时间}` 不是合法的 MessageFormat 占位符（合法格式为 `{0}`、`{1,number}`），每次都触发异常抛出+捕获
- 频繁异常产生不必要的性能开销

**修改**：
1. 在 `interpolate()` 中增加预检：如果模板不包含数字占位符（`{0}`、`{1}` 等），直接跳过多余的 `MessageFormat.format()` 尝试，走 `replaceFirst` 路径
2. 检测逻辑：`template.matches(".*\\{\\d+.*\\}.*")` — 判断模板是否包含至少一个数字占位符

**测试变更**：无需变更（现有 6 个用例已覆盖所有路径，行为不变）

## 已有代码上下文

### JwtTokenProvider.java（T12）
```java
@PostConstruct
public void init() {
    String secret = jwtConfig.getSecret();
    if (secret == null || secret.isEmpty()) {
        throw new IllegalStateException("JWT secret must be configured");
    }
    if (!secret.matches("^[A-Za-z0-9+/]+=*$")) {          // ← 改为URL-safe
        throw new IllegalStateException("JWT secret contains invalid Base64 characters");
    }
    byte[] keyBytes;
    try {
        keyBytes = Base64.getDecoder().decode(secret);     // ← 改为getUrlDecoder
    } catch (IllegalArgumentException e) {
        throw new IllegalStateException("JWT secret is not a valid Base64 string: " + e.getMessage());
    }
    if (keyBytes.length < 32) {
        throw new IllegalStateException("JWT secret must decode to at least 32 bytes (256 bits), got: " + keyBytes.length);
    }
    this.secretKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes);
}
```

### JwtTokenProviderTest.java（T12 测试）
- `TEST_SECRET = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="` — 只含 A 和 =，URL-safe 解码器也支持 `=` padding，无需修改
- `shouldThrowWhenSecretContainsInvalidChars` — 当前测试数据 `"test-secret-with-dash"`，URL-safe 下 dash 是合法字符，需要改为含 `!` 的字符串
- 其他用例不受影响

### JwtUtil.java（T14）
```java
public String generateToken(Long userId, String username, String role, String position) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);
    claims.put("username", username);
    claims.put("role", role);              // ← 移除
    if (position != null) {
        claims.put("position", position);  // ← 移除
    }
    // ← 添加 jti
    Date now = new Date();
    Date expiration = new Date(now.getTime() + jwtConfig.getAccessTokenExpiration() * 1000);
    return Jwts.builder()
            .claims(claims)
            .subject(username)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(this.secretKey)
            .compact();
}
```

### JwtUtilTest.java（T14 测试）
- 所有 `jwtUtil.generateToken(...)` 调用不依赖 role/position 返回值（除 `shouldGenerateTokenWithPosition` 和 `getRole` 测试外）
- `shouldParseValidToken` 第 81 行 `assertEquals("ADMIN", claims.get("role"));` — 需要移除
- `shouldGenerateTokenWithPosition` 第 60-65 行 — 需要移除
- `shouldGetRoleFromValidToken` 第 179-185 行 — 需要更新

### SimpleMessageInterpolator.java（T16）
```java
@Override
public String interpolate(String template, Object[] args) {
    if (args == null || args.length == 0) {
        return template;
    }
    try {
        return MessageFormat.format(template, args);  // ← 命名占位符时抛异常
    } catch (IllegalArgumentException e) {
        String result = template;
        for (Object arg : args) {
            result = result.replaceFirst("\\{[^}]+\\}", String.valueOf(arg));
        }
        return result;
    } catch (Exception e) {
        return template;
    }
}
```

### SimpleMessageInterpolatorTest.java（T16 测试）
- 6 个现有用例，无需新增或修改（行为不变）
- `shouldReplaceNumberedPlaceholders` 验证 `{0}` `{1}` 的 MessageFormat 正常路径
- `shouldReplaceNamedPlaceholdersByPosition` 验证命名占位符回退路径
