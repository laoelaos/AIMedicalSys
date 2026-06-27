# 任务指令（v2）

## 动作
NEW

## 任务描述
修复 **T1**（JwtTokenProvider @PostConstruct 缺少启动验证）和 **T10**（JwtConfig.validate() 检查的是原始字符串长度而非解码后字节长度），同时修复 **integration 测试 JWT secret 为非法 Base64** 导致 ApplicationContextIT/EntityMappingIT 启动失败的问题。

### 涉及文件

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/jwt/JwtTokenProvider.java` | 修改 | `init()` 增加 null/空值检查 + Base64 decode 异常包装 + 解码后字节长度 >= 32 检查 |
| `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/jwt/JwtConfig.java` | 修改 | `validate()` 将 `secret.length() < 32` 改为解码后字节长度检查 |
| `AIMedical/backend/integration/src/test/resources/application-test.yml` | 修改 | JWT secret 改为合法 Base64 字符串（当前含非法字符 `-`） |
| `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/jwt/JwtTokenProviderTest.java` | 修改/新增 | 补充启动验证场景测试（若文件不存在则新建） |
| `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/jwt/JwtConfigTest.java` | 修改/新增 | 补充解码后字节长度检查测试（若文件不存在则新建） |

## 选择理由
批次 3 首项。ApplicationContextIT 因 `JwtTokenProvider.init()` 在 `Base64.getDecoder().decode(secret)` 时抛出 `IllegalArgumentException`（secret `test-secret-key-for-integration-tests-minimum-32-chars` 含非法字符 `-`）而无法启动——这同时暴露了代码缺陷（T1：缺少启动验证）和测试配置问题。修复两项后 integration 测试通道开放，后续任务可在完整测试环境下验证。

## 任务上下文

### T1 根因分析
`JwtTokenProvider.init()`（`auth/jwt/JwtTokenProvider.java:31-36`）直接调用 `Base64.getDecoder().decode(secret)`，未执行以下启动验证：

1. **null/空值检查**：secret 为 null 时 `decode()` 抛出 NPE，非规范 IllegalStateException
2. **非法 Base64 字符检查**：含非法字符时抛出 `IllegalArgumentException`（如测试配置中的 `-` 字符）
3. **解码后字节长度 >= 32 检查**：`Keys.hmacShaKeyFor(keyBytes)` 虽隐式校验但异常信息为库默认文本

### T10 根因分析
`JwtConfig.validate()`（`jwt/JwtConfig.java:55-58`）检查 `secret.length() < 32`——检查的是 Base64 原始字符串的字符长度。OOD 4.7 节要求"Base64 解码后的字节长度 ≥ 32"。当前检查会误放过短密钥：32 字符 Base64 仅对应约 24 字节。

### 测试配置问题
`application-test.yml` 中 `jwt.secret: test-secret-key-for-integration-tests-minimum-32-chars` 含有 `-` 字符，非法 Base64 字符集。需替换为合法 Base64 字符串（如 Base64 编码的 32 字节随机值）。

## 已有代码上下文

### JwtTokenProvider.init() 当前实现（JwtTokenProvider.java:31-36）
```java
@PostConstruct
public void init() {
    String secret = jwtConfig.getSecret();
    byte[] keyBytes = Base64.getDecoder().decode(secret);
    this.secretKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes);
}
```

### JwtConfig.validate() 当前实现（JwtConfig.java:48-60）
```java
@PostConstruct
public void validate() {
    if (!StringUtils.hasText(secret)) {
        throw new IllegalStateException("JWT密钥未配置！...");
    }
    if (secret.length() < 32) {
        throw new IllegalStateException("JWT密钥长度必须至少32个字符...");
    }
}
```

### JwtUtil.init() 已有正确实现（参考模板，JwtUtil.java:46-60）
```java
@PostConstruct
public void init() {
    String secret = jwtConfig.getSecret();
    if (secret == null || secret.isEmpty()) {
        throw new IllegalStateException("JWT_SECRET must be configured");
    }
    if (!secret.matches("^[A-Za-z0-9+/]+=*$")) {
        throw new IllegalStateException("JWT_SECRET contains invalid characters");
    }
    byte[] keyBytes = Base64.getDecoder().decode(secret);
    if (keyBytes.length < 32) {
        throw new IllegalStateException(
            "JWT_SECRET must be at least 256 bits (32 bytes) after Base64 decoding");
    }
    this.secretKey = Keys.hmacShaKeyFor(keyBytes);
}
```

### 测试配置当前值（application-test.yml:23-24）
```yaml
jwt:
  secret: test-secret-key-for-integration-tests-minimum-32-chars
```

## 设计指导

### JwtTokenProvider.init() 修改要求

参考 `JwtUtil.init()` 的验证模式，在 `JwtTokenProvider.init()` 中添加以下检查（顺序执行）：

1. **null/空值检查**：secret 为 null 或空字符串时抛出 `IllegalStateException("JWT secret must be configured")`
2. **Base64 字符集检查**：通过正则 `^[A-Za-z0-9+/]+=*$` 验证字符集合法性，不合法时抛出 `IllegalStateException("JWT secret contains invalid Base64 characters")`
3. **解码后字节长度检查**：`keyBytes.length < 32` 时抛出 `IllegalStateException("JWT secret must decode to at least 32 bytes (256 bits), got: " + keyBytes.length)`
4. **异常包装**：`Base64.getDecoder().decode(secret)` 若抛出 `IllegalArgumentException`，应捕获并包装为 `IllegalStateException("JWT secret is not a valid Base64 string: " + e.getMessage())`

注意：验证逻辑应放在 `decode()` 之前/之后适当位置，不要在验证前先 decode。

### JwtConfig.validate() 修改要求

将第55行 `if (secret.length() < 32)` 改为解码后字节长度检查：
```java
byte[] keyBytes = Base64.getDecoder().decode(secret);
if (keyBytes.length < 32) {
    throw new IllegalStateException(
        "JWT密钥解码后字节长度必须至少为32字节（256位）。当前解码后长度: " + keyBytes.length);
}
```

注意用 try-catch 包裹 `decode()` 调用——非法 Base64 字符时抛出 `IllegalArgumentException`，应捕获并抛出 `IllegalStateException` 保持异常类型一致。

### 职责分工说明

JwtConfig.validate() 与 JwtTokenProvider.init() 的校验存在重叠，但职责不同：
- **JwtConfig.validate()** (配置层)：检查 secret 存在性 + 解码后字节长度。执行时机更早（`@ConfigurationProperties` 优先初始化），作为配置的快速反馈
- **JwtTokenProvider.init()** (密码学层)：完整的启动验证（null + 字符集 + 解码后长度 + 密钥构建）。作为 Bean 初始化的最终保障

两处检查都保留，不移除任一校验。

### 测试配置修复要求

将 `application-test.yml` 中 JWT secret 替换为合法 Base64 字符串，例如（必须 >= 32 解码后字节，即 44+ 编码后字符）：
```yaml
jwt:
  secret: dGVzdC1zZWNyZXQta2V5LWZvci1pbnRlZ3JhdGlvbi10ZXN0cy1taW4tMzI=
```

验证方式：运行 `echo "dGVzdC1zZWNyZXQta2V5LWZvci1pbnRlZ3JhdGlvbi10ZXN0cy1taW4tMzI=" | base64 -d | wc -c`，（或 Java `Base64.getDecoder().decode("...").length`）确认解码后 >= 32 字节。

### 测试要求

#### JwtTokenProviderTest（新增测试类或扩展现有）

测试以下场景（每个场景验证抛出 `IllegalStateException` 且包含对应描述）：
1. `shouldThrowWhenSecretIsNull` — mock JwtConfig 返回 null secret
2. `shouldThrowWhenSecretIsEmpty` — mock JwtConfig 返回空字符串
3. `shouldThrowWhenSecretContainsInvalidChars` — mock JwtConfig 返回含 `-` 的 secret
4. `shouldThrowWhenDecodedKeyTooShort` — mock JwtConfig 返回短 Base64（解码后 < 32 字节）
5. `shouldInitSuccessfullyWithValidSecret` — mock JwtConfig 返回合法长 Base64 secret

#### JwtConfigTest（新增测试类或扩展现有）

测试以下场景：
1. `shouldThrowWhenDecodedKeyTooShort` — 配置解码后 < 32 字节的 Base64 secret
2. `shouldPassWithValidLongSecret` — 配置合法长 Base64 secret

## 验证标准
- `mvn compile test-compile -pl modules/common-module/common-module-impl -am -q` 通过
- 新增的 JwtTokenProviderTest 和 JwtConfigTest 全部通过
- `mvn test -pl integration -am` 中 ApplicationContextIT 和 EntityMappingIT 通过
