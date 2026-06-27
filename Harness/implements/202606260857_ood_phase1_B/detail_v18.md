# 详细设计（v18）

## 概述

完成 OOD Phase 1 B 实施的四项收尾工作：清理 application.yml 中的 phase0 profile、更新 JWT 配置格式对齐 OOD 4.7、拆分 JwtConfig.expiration 为 accessTokenExpiration + refreshTokenExpiration、适配 JwtUtil 调用点、在 EntityMappingIT 中新增两个 User 字段映射测试。

## 文件规划

| 文件路径（相对 `AIMedical/backend/`） | 操作 | 职责 |
|---------|------|------|
| `application/src/main/resources/application.yml` | 修改 | 移除 phase0 profile；将 `jwt.expiration` 替换为 `jwt.access-token-expiration` 和 `jwt.refresh-token-expiration` |
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/jwt/JwtConfig.java` | 修改 | 将 `expiration` 字段拆分为 `accessTokenExpiration`（900L）和 `refreshTokenExpiration`（604800L） |
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/jwt/JwtUtil.java` | 修改 | 将 `generateToken()` 和 `getExpirationTime()` 中对 `jwtConfig.getExpiration()` 的调用改为 `jwtConfig.getAccessTokenExpiration()` |
| `integration/src/test/java/com/aimedical/integration/EntityMappingIT.java` | 修改 | 在 `// ==================== User ====================` 区域新增 `user_shouldMapPasswordChangeRequired()` 和 `user_shouldMapTokenVersion()` 测试方法 |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/jwt/JwtUtilTest.java` | 修改 | 将全部 5 处 `setExpiration(86400L)` 替换为 `setAccessTokenExpiration(900L)`；将 `getExpiration` 测试中断言值从 `86400L` 更新为 `900L` |

## 类型定义

### JwtConfig

**形态**：`@Configuration @ConfigurationProperties(prefix = "jwt")` class
**包路径**：`com.aimedical.modules.commonmodule.jwt`
**职责**：JWT 配置属性绑定类，管理签名密钥、access/refresh token 过期时间、token 类型。

**变更概要**：
- 移除 `private long expiration = 86400L` 字段及其 getter/setter
- 新增 `private long accessTokenExpiration = 900L` 字段，对应配置项 `jwt.access-token-expiration`
- 新增 `private long refreshTokenExpiration = 604800L` 字段，对应配置项 `jwt.refresh-token-expiration`
- 保留 `secret`（String）、`tokenType`（String, default "Bearer"）不变
- 保留 `@PostConstruct validate()` 方法不变

**新增公开接口**：
| 方法签名 | 说明 |
|---------|------|
| `long getAccessTokenExpiration()` | 返回 access token 过期时间（秒） |
| `void setAccessTokenExpiration(long accessTokenExpiration)` | 设置 access token 过期时间 |
| `long getRefreshTokenExpiration()` | 返回 refresh token 过期时间（秒） |
| `void setRefreshTokenExpiration(long refreshTokenExpiration)` | 设置 refresh token 过期时间 |

**移除的公开接口**：
| 方法签名 | 替代方案 |
|---------|---------|
| `long getExpiration()` | `getAccessTokenExpiration()` |
| `void setExpiration(long expiration)` | `setAccessTokenExpiration(long)` |

### JwtUtil

**形态**：`@Component` class
**包路径**：`com.aimedical.modules.commonmodule.jwt`
**职责**：JWT 令牌生成、解析和验证工具类。

**变更点**（仅两处调用点替换，无结构变化）：
1. `generateToken()` 第 81 行：`jwtConfig.getExpiration() * 1000` → `jwtConfig.getAccessTokenExpiration() * 1000`
2. `getExpirationTime()` 第 229 行：`return jwtConfig.getExpiration()` → `return jwtConfig.getAccessTokenExpiration()`

### JwtUtilTest（适配修改）

**形态**：`class`（JUnit 5）
**包路径**：`com.aimedical.modules.commonmodule.jwt`
**职责**：JwtUtil 单元测试，因 `JwtConfig.expiration` 字段拆除而适配。

**变更点**（共 6 处）：
1. `setUp()` 第 28 行：`jwtConfig.setExpiration(86400L)` → `jwtConfig.setAccessTokenExpiration(900L)`
2. `GetExpirationTests.shouldGetExpiration()` 第 237 行：`assertEquals(86400L, jwtUtil.getExpirationTime())` → `assertEquals(900L, jwtUtil.getExpirationTime())`
3. `InitTests` 中四个子测试（第 256、268、280、292 行）：`config.setExpiration(86400L)` → `config.setAccessTokenExpiration(900L)`

> 上述修改确保 `setExpiration()` 移除后编译通过，且测试断言值与新默认值 900L 一致。

### EntityMappingIT（新增测试方法）

**形态**：`@SpringBootTest` class（JUnit 5）
**包路径**：`com.aimedical.integration`
**职责**：验证 JPA 实体-SQL 映射一致性。

**新增方法 1** — `user_shouldMapPasswordChangeRequired()`
```java
@Test
void user_shouldMapPasswordChangeRequired() {
    // 验证默认值为 false
    User user = new User();
    user.setUsername("test_pcr_default");
    user.setPassword("pwd123");
    user.setNickname("测试PCR默认");
    user.setUserType(UserType.ADMIN);

    entityManager.persist(user);
    entityManager.flush();

    User found = entityManager.find(User.class, user.getId());
    assertFalse(found.getPasswordChangeRequired());

    // 设置为 true 后持久化并正确读取
    found.setPasswordChangeRequired(true);
    entityManager.flush();
    entityManager.clear();

    User reloaded = entityManager.find(User.class, user.getId());
    assertTrue(reloaded.getPasswordChangeRequired());
}
```

**新增方法 2** — `user_shouldMapTokenVersion()`
```java
@Test
void user_shouldMapTokenVersion() {
    // 验证默认值为 0
    User user = new User();
    user.setUsername("test_tv_default");
    user.setPassword("pwd123");
    user.setNickname("测试TV默认");
    user.setUserType(UserType.ADMIN);

    entityManager.persist(user);
    entityManager.flush();

    User found = entityManager.find(User.class, user.getId());
    assertEquals(Integer.valueOf(0), found.getTokenVersion());

    // 递增后持久化并正确读取
    found.setTokenVersion(1);
    entityManager.flush();
    entityManager.clear();

    User reloaded = entityManager.find(User.class, user.getId());
    assertEquals(Integer.valueOf(1), reloaded.getTokenVersion());
}
```

## 错误处理

无变更。JwtConfig 的 `@PostConstruct validate()` 仍会校验 secret 非空且长度 >= 32，与现有行为一致。EntityMappingIT 新增测试方法遵循现有 `ConstraintViolationException` / `DataIntegrityViolationException` 处理模式。

## 行为契约

### application.yml 配置映射

| 配置项 | 绑定字段 | 类型 | 默认值 |
|-------|---------|------|-------|
| `jwt.secret` | `JwtConfig.secret` | String | 无（必须配置） |
| `jwt.access-token-expiration` | `JwtConfig.accessTokenExpiration` | long | 900 |
| `jwt.refresh-token-expiration` | `JwtConfig.refreshTokenExpiration` | long | 604800 |
| `jwt.token-type` | `JwtConfig.tokenType` | String | "Bearer" |

### 调用点变更影响

- `JwtUtil.generateToken()`：`expiration` 计算逻辑不变，仅来源从 `getExpiration()` 改为 `getAccessTokenExpiration()`，默认值从 86400 秒（24h）变为 900 秒（15min）
- `JwtUtil.getExpirationTime()`：返回值来源变更同上
- `JwtTokenProvider` 使用硬编码常量（ACCESS_TOKEN_EXPIRATION_MS=900_000, REFRESH_TOKEN_EXPIRATION_MS=604_800_000），不受 JwtConfig 变更影响

### EntityMappingIT 前置条件

- 每个新增测试方法均使用唯一 username（`test_pcr_default`、`test_tv_default`），避免与已有测试冲突
- `passwordChangeRequired` 字段定义：`@Column(nullable=false) private Boolean passwordChangeRequired = false`
- `tokenVersion` 字段定义：`@Column(nullable=false) private Integer tokenVersion = 0`

## 依赖关系

### 修改文件的依赖关系

| 文件 | 依赖 | 关系 |
|------|------|------|
| `application/src/main/resources/application.yml` | 无（纯配置） | — |
| `JwtConfig.java` | `Spring Boot @ConfigurationProperties` | 属性绑定 |
| `JwtUtil.java` | `JwtConfig` | 构造器注入，通过 `getAccessTokenExpiration()` 获取配置 |
| `EntityMappingIT.java` | `User` 实体 | 直接构造并操作实体实例 |
| `JwtUtilTest.java` | `JwtConfig`, `JwtUtil` | 单元测试，调用 `setAccessTokenExpiration()` 替代已移除的 `setExpiration()` |

## 修订说明（v18 r1）

| 审查意见 | 修改措施 |
|---------|---------|
| [严重] 设计遗漏 JwtUtilTest.java 的适配修改。删除 `expiration` 后原 5 处 `setExpiration(86400L)` 和 1 处 `assertEquals(86400L, ...)` 将编译失败 | 在文件规划中补充 `JwtUtilTest.java` 行；在类型定义中新增 `JwtUtilTest` 章节，明确列出全部 6 处替换点；在依赖关系表中补充 `JwtUtilTest.java` 行 |
