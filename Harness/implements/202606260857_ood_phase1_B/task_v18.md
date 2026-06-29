# 任务指令（v18 r1）

## 动作
RETRY

## 任务描述
完成 OOD Phase 1 B 实施最后两道收尾工作。

**修改文件 1** — `application/src/main/resources/application.yml`
- 移除 `phase0` 激活 profile（当前为 `phase0,phase1,dev` → 改为 `phase1,dev`）
- 按 OOD 4.7 更新 JWT 配置格式：
  - `jwt.expiration` → `jwt.access-token-expiration: 900`
  - 新增 `jwt.refresh-token-expiration: 604800`

**修改文件 2** — `JwtConfig.java`（`modules/common-module/common-module-impl/src/main/java/.../jwt/JwtConfig.java`）
- 将单一 `expiration` 字段拆分为 `accessTokenExpiration`（long, default 900L）和 `refreshTokenExpiration`（long, default 604800L）
- 保留 `secret` 和 `tokenType` 字段不变
- 添加对应的 getter/setter

**修改文件 3** — `JwtUtil.java`（`modules/common-module/common-module-impl/src/main/java/.../jwt/JwtUtil.java`）
- 将 `generateToken()` 中的 `jwtConfig.getExpiration()` 改为 `jwtConfig.getAccessTokenExpiration()`
- 将 `getExpirationTime()` 中的 `jwtConfig.getExpiration()` 改为 `jwtConfig.getAccessTokenExpiration()`

**修改文件 4** — `integration/src/test/java/.../integration/EntityMappingIT.java`
- 在 `// ==================== User ====================` 区域新增两个测试方法：
  - `user_shouldMapPasswordChangeRequired()`：验证 `passwordChangeRequired` 字段默认值为 false，可设置为 true 后持久化并正确读取
  - `user_shouldMapTokenVersion()`：验证 `tokenVersion` 字段默认值为 0，递增后持久化并正确读取

## 选择理由
- R1-R17 完成了所有生产代码实现（Stage 1-3），545 测试全部通过
- 根据审查反馈，`JwtConfig.expiration` 拆分为 access/refresh 两个字段是正确实现 OOD 4.7 的必要前提
- application.yml 配置清理（M10）和 EntityMappingIT 字段覆盖（A4）是 OOD 任务分解中最后两个未完成项

## 任务上下文
- `application.yml` 当前内容：
  ```yaml
  spring:
    profiles:
      active: phase0,phase1,dev
  jwt:
    secret: ${JWT_SECRET}
    expiration: 86400
  ```
- 目标内容（移除 phase0，JWT 属性名对齐 OOD 4.7）：
  ```yaml
  spring:
    profiles:
      active: phase1,dev
  jwt:
    secret: ${JWT_SECRET}
    access-token-expiration: 900
    refresh-token-expiration: 604800
  ```
- `JwtConfig` 当前仅有一个 `expiration`（默认 86400L），需拆分为 `accessTokenExpiration`（默认 900L）和 `refreshTokenExpiration`（默认 604800L）
- `JwtUtil.generateToken()` 当前使用 `jwtConfig.getExpiration()`（line 81），需改为 `jwtConfig.getAccessTokenExpiration()`
- `JwtUtil.getExpirationTime()` 当前使用 `jwtConfig.getExpiration()`（line 229），需改为 `jwtConfig.getAccessTokenExpiration()`
- `JwtTokenProvider` 使用硬编码常量（ACCESS_TOKEN_EXPIRATION_MS=900_000, REFRESH_TOKEN_EXPIRATION_MS=604_800_000），不受 JwtConfig 变更影响
- `SecurityConfigPhase0` 使用 `@Profile("phase0 & !phase1")`，在 Phase 1 中不会激活，因此移除 profile 是安全的清理操作
- EntityMappingIT 中 User 字段 `passwordChangeRequired`（`@Column(nullable=false, columnDefinition="BIT(1) DEFAULT 0")`）和 `tokenVersion`（`@Column(nullable=false) private Integer tokenVersion = 0`）

## 已有代码上下文
- `JwtConfig.java`：`@Configuration @ConfigurationProperties(prefix="jwt")`，当前字段：`secret`（String）、`expiration`（long, default 86400L）、`tokenType`（String, default "Bearer"）
- `JwtUtil.generateToken()` line 81：`Date expiration = new Date(now.getTime() + jwtConfig.getExpiration() * 1000)`
- `JwtUtil.getExpirationTime()` line 228-229：`public Long getExpirationTime() { return jwtConfig.getExpiration(); }`
- EntityMappingIT 已有 `user_shouldMapUsernameField()`、`user_shouldEnforceUserTypeNotNull()`、`user_shouldMapManyToManyWithRoles()` 等方法，新增测试方法可参考其 `entityManager.persist()` / `entityManager.flush()` / `entityManager.find()` 模式

## RETRY 说明
审查意见（v18 r1）：计划上下文错误描述 `JwtConfig` 已具备 `access-token-expiration` / `refresh-token-expiration` 属性绑定，与实际代码（仅 `expiration` 字段）不符。

修正措施：采用方案 A，在 R18 任务中补充更新 `JwtConfig`（`expiration` → `accessTokenExpiration` + `refreshTokenExpiration`），并更新 `JwtUtil` 中对 `jwtConfig.getExpiration()` 的调用点。

## 修订说明（v18 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [一般] 计划关于 JwtConfig 属性绑定的描述与代码库事实不符 — 声称已使用 access-token-expiration / refresh-token-expiration，实际仅有一个 expiration 字段 | 修正任务描述：补充 JwtConfig 拆分（expiration→accessTokenExpiration+refreshTokenExpiration）+ JwtUtil 调用点适配；修正上下文描述以反映真实代码状态 |
