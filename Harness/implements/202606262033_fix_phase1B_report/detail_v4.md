# 详细设计（v4）

## 概述

将 JwtAuthenticationFilter 从 JwtUtil 依赖切换为 JwtTokenProvider，消除重复的 userId 提取逻辑。涉及 6 个文件：3 个源文件 + 3 个测试文件。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `common-module-impl/.../auth/jwt/JwtTokenProvider.java` | 修改（+1 方法） | 新增 `getTokenType()` 委托至 `jwtConfig.getTokenType()` |
| `common-module-impl/.../auth/security/JwtAuthenticationFilter.java` | 修改（重构） | 注入/方法从 JwtUtil 切换为 JwtTokenProvider；删除手动 type 检查；删除 extractUserId() 委托至 Provider |
| `common-module-impl/.../auth/security/SecurityConfigPhase1.java` | 修改（注入适配） | Bean 方法参数类型从 JwtUtil 改为 JwtTokenProvider |
| `common-module-impl/.../auth/security/JwtAuthenticationFilterTest.java` | 修改（Mock 适配） | 字段类型、构造参数、mock 方法签名同步更新 |
| `common-module-impl/.../auth/security/SecurityConfigPhase1Test.java` | 修改（Mock 适配） | 字段类型从 JwtUtil 改为 JwtTokenProvider |
| `common-module-impl/.../auth/security/SecurityConfigPhase1CoexistenceTest.java` | 修改（Mock 适配） | 字段类型从 JwtUtil 改为 JwtTokenProvider |

## 类型定义

### 1. JwtTokenProvider

**形态**：class（已有，新增方法）
**包路径**：`com.aimedical.modules.commonmodule.auth.jwt`
**职责**：JWT 令牌生成、解析、验证的集中提供者

**新增方法签名**：
```java
public String getTokenType()
```
- 返回 `jwtConfig.getTokenType()`
- 直接委托，无额外逻辑
- 位置：在 `getJtiFromToken` 与 `getTokenVersionFromClaims` 之间（或同类委托方法区）

**已有相关签名（本任务使用）**：
```java
public Claims validateToken(String token, String expectedType)
public Long getUserIdFromClaims(Claims claims)
```

### 2. JwtAuthenticationFilter

**形态**：class（修改）
**包路径**：`com.aimedical.modules.commonmodule.auth.security`
**职责**：从 HTTP Authorization 头中提取 JWT、验证、设置 SecurityContext

**字段变化**：
```java
// 删除
private final JwtUtil jwtUtil;
// 新增
private final JwtTokenProvider jwtTokenProvider;
```

**构造方法**：
```java
// 原
JwtAuthenticationFilter(JwtUtil jwtUtil, TokenBlacklist tokenBlacklist, UserRepository userRepository)
// 改为
JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, TokenBlacklist tokenBlacklist, UserRepository userRepository)
```

**方法变化**：

| 方法 | 原实现 | 修改后 |
|------|--------|--------|
| `extractToken(String authHeader)` | `JwtUtil.extractToken(authHeader, jwtUtil.getTokenType())` | `JwtUtil.extractToken(authHeader, jwtTokenProvider.getTokenType())` |
| `doFilterInternal()` L60 | `jwtUtil.validateTokenAndGetClaims(token)` | `jwtTokenProvider.validateToken(token, "access")` |
| `doFilterInternal()` L68-74（手动 type 检查） | `claims.get("type", String.class)` → 判断 refresh → chain.doFilter | **整块删除** |
| `doFilterInternal()` L84 | `extractUserId(claims)` | `jwtTokenProvider.getUserIdFromClaims(claims)` |
| `extractUserId(Claims)` L142-150 | 私有方法，含 Integer/Long 转换 | **整方法删除** |

**更新后的 doFilterInternal 伪代码流程**（删除 type 检查后）：
1. `authHeader` null/empty → chain.doFilter
2. `extractToken()` → null → chain.doFilter
3. `jwtTokenProvider.validateToken(token, "access")` → null → clearContext + chain.doFilter
4. `extractJti(claims)` → blacklisted → clearContext + chain.doFilter
5. `jwtTokenProvider.getUserIdFromClaims(claims)` → null → clearContext + chain.doFilter
6. `userRepository.findWithDetailsById(userId)` → empty → clearContext + chain.doFilter
7. 后续：enabled 检查、权限收集、authentication 设置、chain.doFilter

**Import 变更**：
- 保留：`import com.aimedical.modules.commonmodule.jwt.JwtUtil;`（用于静态方法 `extractToken`）
- 新增：`import com.aimedical.modules.commonmodule.auth.jwt.JwtTokenProvider;`
- 无需删除其他 import（原有 `JwtUtil` 的 import 继续保留）

### 3. SecurityConfigPhase1

**形态**：class（修改）
**包路径**：`com.aimedical.modules.commonmodule.auth.security`

**Import 变更**：
```java
// 删除
import com.aimedical.modules.commonmodule.jwt.JwtUtil;
// 新增
import com.aimedical.modules.commonmodule.auth.jwt.JwtTokenProvider;
```

**Bean 方法变更**：
```java
// 原
public JwtAuthenticationFilter jwtAuthenticationFilter(
        JwtUtil jwtUtil, TokenBlacklist tokenBlacklist, UserRepository userRepository) {
    return new JwtAuthenticationFilter(jwtUtil, tokenBlacklist, userRepository);
}
// 改为
public JwtAuthenticationFilter jwtAuthenticationFilter(
        JwtTokenProvider jwtTokenProvider, TokenBlacklist tokenBlacklist, UserRepository userRepository) {
    return new JwtAuthenticationFilter(jwtTokenProvider, tokenBlacklist, userRepository);
}
```

### 4. JwtAuthenticationFilterTest

**形态**：class（修改）
**包路径**：`com.aimedical.modules.commonmodule.auth.security`

**字段变更**：
```java
// 删除
private final JwtUtil jwtUtil = mock(JwtUtil.class);
// 新增
private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
```
- 移除 `import com.aimedical.modules.commonmodule.jwt.JwtUtil;`
- 新增 `import com.aimedical.modules.commonmodule.auth.jwt.JwtTokenProvider;`

**过滤机构造调用**：
```java
// 原
private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, tokenBlacklist, userRepository);
// 改为
private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, tokenBlacklist, userRepository);
```

**各测试方法 Mock 变更**：

| 测试方法 | 变更内容 |
|---------|---------|
| `shouldSkipWhenNoAuthHeader` | 无变更 |
| `shouldSkipWhenInvalidToken` | `when(jwtUtil.getTokenType())` → `when(jwtTokenProvider.getTokenType())`; `when(jwtUtil.validateTokenAndGetClaims("xxx"))` → `when(jwtTokenProvider.validateToken("xxx", "access"))` |
| `shouldSkipWhenRefreshTokenType` | `when(jwtUtil.getTokenType())` → `when(jwtTokenProvider.getTokenType())`; `when(jwtUtil.validateTokenAndGetClaims("xxx")).thenReturn(claims)` → `when(jwtTokenProvider.validateToken("xxx", "access")).thenReturn(null)`（因 type 不匹配，Provider 返回 null）；**删除** `when(claims.get("type", String.class)).thenReturn("refresh")`；`claims` mock 变量可删除（不再使用） |
| `shouldSkipWhenTokenBlacklisted` | `when(jwtUtil.getTokenType())` → `when(jwtTokenProvider.getTokenType())`; `when(jwtUtil.validateTokenAndGetClaims("xxx"))` → `when(jwtTokenProvider.validateToken("xxx", "access"))`; **删除** `when(claims.get("type", String.class)).thenReturn(null)` |
| `shouldSkipWhenUserNotFound` | 同上；**删除** `when(claims.get("type", String.class)).thenReturn(null)` |
| `shouldThrowAccountDisabledWhenUserDisabled` | 同上；**删除** `when(claims.get("type", String.class)).thenReturn(null)` |
| `shouldAuthenticateSuccessfully` | 同上；**删除** `when(claims.get("type", String.class)).thenReturn(null)` |
| `shouldSetPasswordChangeRequiredAttribute` | 同上；**删除** `when(claims.get("type", String.class)).thenReturn(null)` |
| `shouldPopulateAuthoritiesFromRolesAndFunctions` | 同上；**删除** `when(claims.get("type", String.class)).thenReturn(null)` |

### 5. SecurityConfigPhase1Test

**形态**：class（修改）

**字段变更**：
```java
// 删除
private final JwtUtil jwtUtil = mock(JwtUtil.class);
// 新增
private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
```
- 移除 `import com.aimedical.modules.commonmodule.jwt.JwtUtil;`
- 新增 `import com.aimedical.modules.commonmodule.auth.jwt.JwtTokenProvider;`

**Bean 调用处变更（全部 3 处引用）**：
```java
// 原
config.jwtAuthenticationFilter(jwtUtil, mock(TokenBlacklist.class), userRepository);
config.jwtAuthenticationFilter(jwtUtil, new InMemoryTokenBlacklist(), userRepository);
// 改为
config.jwtAuthenticationFilter(jwtTokenProvider, mock(TokenBlacklist.class), userRepository);
config.jwtAuthenticationFilter(jwtTokenProvider, new InMemoryTokenBlacklist(), userRepository);
```

### 6. SecurityConfigPhase1CoexistenceTest

**形态**：class（修改）

**字段/Mock 变更**：
```java
// 删除
JwtUtil jwtUtil = mock(JwtUtil.class);
// 新增
JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
```
- 移除 `import com.aimedical.modules.commonmodule.jwt.JwtUtil;`
- 新增 `import com.aimedical.modules.commonmodule.auth.jwt.JwtTokenProvider;`

**Bean 调用处变更**：
```java
// 原
securityConfigPhase1.jwtAuthenticationFilter(jwtUtil, tokenBlacklist, userRepository)
// 改为
securityConfigPhase1.jwtAuthenticationFilter(jwtTokenProvider, tokenBlacklist, userRepository)
```

## 错误处理

无新增错误处理逻辑。原有 Filter 中的错误处理保持不变：
- `validateToken` 返回 null → clearContext + chain.doFilter（由 JwtTokenProvider 内部封装异常）
- `getUserIdFromClaims` 返回 null → clearContext + chain.doFilter（由 Provider 处理 Integer/Long 转换）
- blacklist 检查、userRepository 查询等异常处理完全不变

## 行为契约

### JwtTokenProvider.getTokenType()
- **前置**：`jwtConfig` 非 null（构造已注入）
- **后置**：返回 `jwtConfig.getTokenType()` 的返回值
- **副效应**：无

### JwtAuthenticationFilter 生命周期
- `validateToken(token, "access")` 替换 `validateTokenAndGetClaims(token)`：
  - 输入 token 不变
  - 输出语义一致：null 表示验证失败，Claims 表示成功
  - 额外验证 `type` claim 是否为 `"access"`（此前由 Filter 手动完成）
- `getUserIdFromClaims(claims)` 替换 `extractUserId(claims)`：
  - 输入 Claims 对象不变
  - 输出语义一致：null/Integer/Long 转换逻辑完全相同（原 extractUserId 与 JwtTokenProvider.getUserIdFromClaims 方法体一致）

### 删除手动 type 检查（原 L68-74）的影响域
- 删除后 `claims.get("jti", ...)` 和 `claims.get("userId")` 在后续代码中仍被使用
- type 验证委托给 `JwtTokenProvider.validateToken(token, "access")`，若 type 不是 "access" 则返回 null，Filter 在 claims == null 时已跳过
- 行为等价：refresh token 不会被误认为 access token

## 依赖关系

| 类型 | 依赖方 | 依赖类型 |
|------|--------|---------|
| `JwtTokenProvider` | JwtAuthenticationFilter（构造注入 + 方法调用） | 新增 |
| `JwtUtil`（静态方法） | JwtAuthenticationFilter.extractToken() | 保留（仅静态调用） |
| `JwtUtil`（实例） | JwtAuthenticationFilter | 删除 |
| `JwtUtil` | SecurityConfigPhase1 Bean 方法参数 | 删除 |
| `JwtTokenProvider` | SecurityConfigPhase1 Bean 方法参数 | 新增 |

### 对测试的影响

- JwtTokenProvider 已有完整的单元测试（JwtTokenProviderTest，14 tests）
- validateToken 的 type 检查、getUserIdFromClaims 的 Integer/Long 转换均已通过测试
- Filter 测试仅需确认：当 Provider 返回 null 时 Filter 跳过；当 Claims 正确时 Filter 继续流程
