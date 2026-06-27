# 详细设计（v2）

## 概述

修复 **T1**（JwtTokenProvider @PostConstruct 缺少启动验证）和 **T10**（JwtConfig.validate() 检查的是原始字符串长度而非解码后字节长度），同时修复 **integration 测试 JWT secret 为非法 Base64** 导致 ApplicationContextIT/EntityMappingIT 启动失败的问题。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/jwt/JwtTokenProvider.java` | 修改 | `init()` 增加 null/空值检查、Base64 字符集检查、解码后字节长度 >= 32 检查、decode 异常包装 |
| `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/jwt/JwtConfig.java` | 修改 | `validate()` 将 `secret.length() < 32` 改为解码后字节长度检查，增加 decode 异常包装 |
| `AIMedical/backend/integration/src/test/resources/application-test.yml` | 修改 | JWT secret 改为合法 Base64 字符串 |
| `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/jwt/JwtTokenProviderTest.java` | 修改 | 补充启动验证场景测试 |
| `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/jwt/JwtConfigTest.java` | 修改 | 更新解码后字节长度检查测试 |

## 类型定义

### 1. JwtTokenProvider

**形态**：已有 class（修改）
**包路径**：`com.aimedical.modules.commonmodule.auth.jwt`
**职责**：JWT 令牌提供者，修改 `init()` 方法增加完整的启动验证

**修改方法**：

```java
@PostConstruct
public void init()
```

**修改后行为**：

1. 获取 `jwtConfig.getSecret()`
2. null 或空值检查 → `IllegalStateException("JWT secret must be configured")`
3. Base64 字符集检查（正则 `^[A-Za-z0-9+/]+=*$`）→ `IllegalStateException("JWT secret contains invalid Base64 characters")`
4. `Base64.getDecoder().decode(secret)` 调用，若抛出 `IllegalArgumentException` 则捕获并包装为 `IllegalStateException("JWT secret is not a valid Base64 string: " + e.getMessage())`
5. 解码后字节长度检查（`keyBytes.length < 32`）→ `IllegalStateException("JWT secret must decode to at least 32 bytes (256 bits), got: " + keyBytes.length)`
6. `this.secretKey = Keys.hmacShaKeyFor(keyBytes)`

**新增 import**：无（`Base64` 已存在，`Keys` 已用全限定名）

### 2. JwtConfig

**形态**：已有 class（修改）
**包路径**：`com.aimedical.modules.commonmodule.jwt`
**职责**：JWT 配置，修改 `validate()` 方法使用解码后字节长度检查

**修改方法**：

```java
@PostConstruct
public void validate()
```

**修改后行为**（仅第2点变化，其余不变）：

1. null/空值检查 → `IllegalStateException`（不变）
2. ~~`secret.length() < 32`~~ → 改为解码后字节长度检查：
   - try-catch 包裹 `Base64.getDecoder().decode(secret)`
   - 捕获 `IllegalArgumentException` → 抛出 `IllegalStateException("JWT secret is not a valid Base64 string: " + e.getMessage())`
   - `keyBytes.length < 32` → 抛出 `IllegalStateException("JWT密钥解码后字节长度必须至少为32字节（256位）。当前解码后长度: " + keyBytes.length)`

**新增 import**：
- `import java.util.Base64;`

### 3. application-test.yml

**形态**：已有配置文件（修改）
**职责**：提供合法的 Base64 JWT secret 用于集成测试

**修改内容**：

```yaml
jwt:
  secret: dGVzdC1zZWNyZXQta2V5LWZvci1pbnRlZ3JhdGlvbi10ZXN0cy1taW4tMzI=
```

替换原值 `test-secret-key-for-integration-tests-minimum-32-chars`（含非法 `-` 字符）。

## 错误处理

| 场景 | 异常类型 | 异常消息 |
|------|---------|---------|
| JwtTokenProvider: secret 为 null/空 | `IllegalStateException` | "JWT secret must be configured" |
| JwtTokenProvider: secret 含非法字符 | `IllegalStateException` | "JWT secret contains invalid Base64 characters" |
| JwtTokenProvider: decode 失败 | `IllegalStateException` | "JWT secret is not a valid Base64 string: {原始异常消息}" |
| JwtTokenProvider: 解码后字节 < 32 | `IllegalStateException` | "JWT secret must decode to at least 32 bytes (256 bits), got: {实际长度}" |
| JwtConfig: secret 为 null/空 | `IllegalStateException` | "JWT密钥未配置！请通过环境变量 JWT_SECRET 或配置项 jwt.secret 提供密钥。"（不变）|
| JwtConfig: decode 失败 | `IllegalStateException` | "JWT secret is not a valid Base64 string: {原始异常消息}" |
| JwtConfig: 解码后字节 < 32 | `IllegalStateException` | "JWT密钥解码后字节长度必须至少为32字节（256位）。当前解码后长度: {实际长度}" |

两处异常类型均保持 `IllegalStateException` 一致。

## 行为契约

### JwtTokenProvider.init() 调用顺序

1. `jwtConfig.getSecret()` 返回 null/空 → 抛出 `IllegalStateException`，不继续执行
2. 正则不匹配 → 抛出 `IllegalStateException`，不继续执行
3. `Base64.getDecoder().decode(secret)` 抛出 `IllegalArgumentException` → 捕获并包装为 `IllegalStateException`
4. `keyBytes.length < 32` → 抛出 `IllegalStateException`
5. 通过所有检查 → `this.secretKey` 被正确赋值

### JwtConfig.validate() 调用顺序

1. `!StringUtils.hasText(secret)` → 抛出 `IllegalStateException`
2. `Base64.getDecoder().decode(secret)` 抛出 `IllegalArgumentException` → 捕获并包装为 `IllegalStateException`
3. `keyBytes.length < 32` → 抛出 `IllegalStateException`
4. 通过 → 静默返回

### 职责分工

- **JwtConfig.validate()**（配置层）：检查 secret 存在性 + Base64 decode 异常包装 + 解码后字节长度。初始化优先级高（`@ConfigurationProperties` 优先），作为配置的快速反馈
- **JwtTokenProvider.init()**（密码学层）：完整的启动验证（null + 字符集 + 解码后长度 + 密钥构建）。作为 Bean 初始化的最终保障
- 两处检查都保留，不移除任一校验

## 依赖关系

### JwtTokenProvider 修改

- 依赖 `JwtConfig`（已有，构造注入）
- 依赖 `java.util.Base64`（已有）
- 依赖 `io.jsonwebtoken.security.Keys`（已有，使用全限定名）
- 依赖 `jakarta.annotation.PostConstruct`（已有）

### JwtConfig 修改

- 新增依赖 `java.util.Base64`
- 已有依赖 `jakarta.annotation.PostConstruct`、`org.springframework.util.StringUtils`

### 测试依赖

| 测试类 | 依赖类型 | 说明 |
|--------|---------|------|
| JwtTokenProviderTest | `JwtConfig` | mock 或真实对象注入不同 secret 值 |
| JwtTokenProviderTest | `org.junit.jupiter.api` | 标准 JUnit 5 断言 |
| JwtConfigTest | `JwtConfig` | 直接创建实例并设置 secret |
| JwtConfigTest | `org.junit.jupiter.api` | 标准 JUnit 5 断言 |

## 测试设计

### JwtTokenProviderTest 新增测试

| 测试方法 | 场景 | mock 设置 | 验证 |
|---------|------|----------|------|
| `shouldThrowWhenSecretIsNull` | secret 为 null | `jwtConfig.getSecret()` 返回 null | `assertThrows(IllegalStateException.class, ...)`，消息包含 "JWT secret must be configured" |
| `shouldThrowWhenSecretIsEmpty` | secret 为空字符串 | `jwtConfig.getSecret()` 返回 `""` | `assertThrows(IllegalStateException.class, ...)`，消息包含 "JWT secret must be configured" |
| `shouldThrowWhenSecretContainsInvalidChars` | secret 含 `-` | `jwtConfig.getSecret()` 返回 `"test-secret-with-dash"` | `assertThrows(IllegalStateException.class, ...)`，消息包含 "invalid Base64 characters" |
| `shouldThrowWhenDecodedKeyTooShort` | 解码后 < 32 字节 | `jwtConfig.getSecret()` 返回 `"dGVzdA=="`（"test" 的 Base64，4 字节） | `assertThrows(IllegalStateException.class, ...)`，消息包含 "at least 32 bytes" |
| `shouldInitSuccessfullyWithValidSecret` | 合法长 Base64 | `jwtConfig.getSecret()` 返回 `"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="`（43 字符，解码后 32 字节） | `assertDoesNotThrow(...)` |

**注意**：原有 8 个测试方法均使用 `TEST_SECRET = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="`（32 字节解码后），继续有效。新增的 5 个测试方法中 `shouldInitSuccessfullyWithValidSecret` 使用相同 secret，其余 4 个使用不同的 mock JwtConfig 实例。

**实现方式**：每个异常场景单独创建 `JwtConfig` 实例（匿名类或 mock），不修改 `@BeforeEach` 中的全局 setup。

### JwtConfigTest 修改

**删除测试方法**：
- `shouldThrowExceptionWhenSecretLengthLessThan32` — 原有逻辑基于字符串长度，改为解码后字节长度后语义不同

**修改/新增测试**：

| 测试方法 | 场景 | secret 值 | 验证 |
|---------|------|----------|------|
| `shouldThrowWhenDecodedKeyTooShort` | 解码后 < 32 字节 | `"dGVzdA=="`（"test" 的 Base64，4 字节） | `assertThrows(IllegalStateException.class, ...)`，消息包含 "至少32字节" |
| `shouldPassWithValidLongSecret` | 合法长 Base64（已有 `shouldNotThrowExceptionWhenSecretIsValid` 改用此名） | `"AIMedicalSysJwtSecretKey2026Phase1DevelopmentTestSecretKey"`（50 字符，纯字母，有效 Base64，解码后 37 字节 >= 32） | `assertDoesNotThrow(...)` |

**保留不变**：
- `shouldThrowExceptionWhenSecretIsNull`
- `shouldThrowExceptionWhenSecretIsEmpty`
- 所有 DefaultValueTests 和 GetterSetterTests 测试

## 修订说明（v2 r0）

本为首轮设计，无审查反馈。
