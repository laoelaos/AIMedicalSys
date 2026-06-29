# 详细设计（v1）

## 概述

删除 `SecurityConfigPhase1.java` 中与 `AuthModuleConfig.java` 重复的 `tokenBlacklist()` Bean 定义，消除 phase1 profile 激活时的 `BeanDefinitionOverrideException`。同步调整 `SecurityConfigPhase1Test.java` 以适配删除后的编译和语义变化。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/SecurityConfigPhase1.java` | 修改 | 删除第41-44行 `tokenBlacklist()` Bean 方法，删除不再使用的 import |
| `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/SecurityConfigPhase1Test.java` | 修改 | 删除对 `config.tokenBlacklist()` 的调用，删除冗余测试方法，保留必要的 import |
| `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/config/AuthModuleConfig.java` | 不变 | 已有的全局 `tokenBlacklist()` Bean 继续提供 TokenBlacklist |

## 类型定义

本任务不涉及新增/修改类型定义，仅删除 Bean 方法和调整测试。

## 修改详情

### 1. SecurityConfigPhase1.java

**删除内容**：第41-44行（Bean 方法）：

```java
    @Bean
    public TokenBlacklist tokenBlacklist() {
        return new InMemoryTokenBlacklist();
    }
```

**删除 import**（因删除上述方法后不再使用）：

- `import com.aimedical.modules.commonmodule.auth.blacklist.InMemoryTokenBlacklist;`（第3行）

**保留 import**：`import com.aimedical.modules.commonmodule.auth.blacklist.TokenBlacklist;`（第4行）不可删除，因为 `jwtAuthenticationFilter(JwtUtil, TokenBlacklist, UserRepository)` 方法签名仍依赖此类型。

**无影响**：第51-55行 `jwtAuthenticationFilter()` 方法签名参数 `TokenBlacklist tokenBlacklist` 由 Spring 容器按类型注入，容器中仍有 `AuthModuleConfig.tokenBlacklist()` 提供的 Bean，不受影响。

### 2. SecurityConfigPhase1Test.java

#### 源文件当前结构

```java
// 第3行: import com.aimedical.modules.commonmodule.auth.blacklist.InMemoryTokenBlacklist;
// 第4行: import com.aimedical.modules.commonmodule.auth.blacklist.TokenBlacklist;
// 第13行: import static org.mockito.Mockito.mock;

// 第15-33行:
class SecurityConfigPhase1Test {
    private final SecurityConfigPhase1 config = new SecurityConfigPhase1();
    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final UserRepository userRepository = mock(UserRepository.class);

    @Test
    void shouldCreateAllBeans() {
        SlidingWindowCounter counter = config.slidingWindowCounter();
        assertNotNull(counter);
        TokenBlacklist blacklist = config.tokenBlacklist();    // ← 待删除
        assertNotNull(blacklist);                               // ← 待删除
        GlobalRateLimitFilter g = config.globalRateLimitFilter(counter);
        assertNotNull(g);
        JwtAuthenticationFilter j = config.jwtAuthenticationFilter(jwtUtil, blacklist, userRepository); // ← blacklist 待替换
        assertNotNull(j);
        PasswordChangeCheckFilter p = config.passwordChangeCheckFilter();
        assertNotNull(p);
    }

    // 第40-43行:
    @Test
    void shouldReturnInMemoryTokenBlacklist() {               // ← 待删除（整个方法）
        assertInstanceOf(InMemoryTokenBlacklist.class, config.tokenBlacklist());
    }

    // 第45-50行:
    @Test
    void shouldCreateJwtAuthenticationFilterWithDeps() {
        JwtAuthenticationFilter filter = config.jwtAuthenticationFilter(
                jwtUtil, config.tokenBlacklist(), userRepository); // ← config.tokenBlacklist() 待替换
        assertNotNull(filter);
    }
}
```

#### 修改项

**删除测试方法**：`shouldReturnInMemoryTokenBlacklist()`（第40-43行）。该方法仅验证 `config.tokenBlacklist()` 返回类型，`AuthModuleConfigTest.tokenBlacklist_shouldReturnNonNullInstance()` 已覆盖此职责。

**修改 `shouldCreateAllBeans()`**（第21-33行）：

将：
```java
        TokenBlacklist blacklist = config.tokenBlacklist();
        assertNotNull(blacklist);
        // ...
        JwtAuthenticationFilter j = config.jwtAuthenticationFilter(jwtUtil, blacklist, userRepository);
```
改为：
```java
        JwtAuthenticationFilter j = config.jwtAuthenticationFilter(jwtUtil, mock(TokenBlacklist.class), userRepository);
```
说明：
- 删除第25-26行：`config.tokenBlacklist()` Bean 方法已从 `SecurityConfigPhase1` 中删除，不应再验证其返回值
- 第29行原引用变量 `blacklist`，该变量已被删除，故将 `blacklist` 替换为 `mock(TokenBlacklist.class)`
- `mock(TokenBlacklist.class)` 可用，因为 `import static org.mockito.Mockito.mock` 已存在（第13行）

**修改 `shouldCreateJwtAuthenticationFilterWithDeps()`**（第45-50行）：

将：
```java
        JwtAuthenticationFilter filter = config.jwtAuthenticationFilter(
                jwtUtil, config.tokenBlacklist(), userRepository);
```
改为：
```java
        JwtAuthenticationFilter filter = config.jwtAuthenticationFilter(
                jwtUtil, new InMemoryTokenBlacklist(), userRepository);
```
说明：`SecurityConfigPhase1` 不再提供 `tokenBlacklist()` 方法，用 `new InMemoryTokenBlacklist()` 替代。

**保留 `InMemoryTokenBlacklist` import**（第3行）：
`shouldCreateJwtAuthenticationFilterWithDeps()` 中使用了 `new InMemoryTokenBlacklist()`，因此 `import com.aimedical.modules.commonmodule.auth.blacklist.InMemoryTokenBlacklist;` 必须保留，不可删除。

## 错误处理

不涉及。本任务为 Bean 定义删除和测试适配，无运行时错误处理路径变更。

## 行为契约

- 删除 `SecurityConfigPhase1.tokenBlacklist()` 后，phase1 profile 激活时不再出现重复 Bean 定义冲突
- `jwtAuthenticationFilter()` Bean 的参数注入 `TokenBlacklist` 由 Spring 容器自动解析 `AuthModuleConfig.tokenBlacklist()` 提供的 Bean，行为不变
- `SecurityConfigPhase1Test.shouldCreateAllBeans()` 不再包含 tokenBlacklist 的验证（已由 `AuthModuleConfigTest` 覆盖），其中 `jwtAuthenticationFilter` 的 `TokenBlacklist` 参数使用 `mock(TokenBlacklist.class)`
- `SecurityConfigPhase1Test.shouldReturnInMemoryTokenBlacklist()` 被删除（职责已由 `AuthModuleConfigTest.tokenBlacklist_shouldReturnNonNullInstance()` 覆盖）
- `SecurityConfigPhase1Test.shouldCreateJwtAuthenticationFilterWithDeps()` 中的 `TokenBlacklist` 参数使用 `new InMemoryTokenBlacklist()`，保留 `InMemoryTokenBlacklist` import

## 依赖关系

- 依赖已有类型：`TokenBlacklist`（interface）、`InMemoryTokenBlacklist`（class）——均无变化
- 被依赖方：`AuthModuleConfig.tokenBlacklist()` 提供的 Bean 将被 `SecurityConfigPhase1.jwtAuthenticationFilter()` 参数注入使用，无新增依赖
- 测试依赖：
  - `SecurityConfigPhase1Test.shouldCreateAllBeans` 使用 `mock(TokenBlacklist.class)`（已有 `Mockito.mock` import）
  - `SecurityConfigPhase1Test.shouldCreateJwtAuthenticationFilterWithDeps` 使用 `new InMemoryTokenBlacklist()`，需保留 `InMemoryTokenBlacklist` import

## 修订说明（v1 r2）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] `shouldCreateAllBeans()` 删除第25-26行后第29行 `blacklist` 成为未定义符号，导致编译错误 | 在 `shouldCreateAllBeans()` 中将第29行 `blacklist` 替换为 `mock(TokenBlacklist.class)`，并补充说明 `mock` import 已存在无需新增 |
| [一般] `shouldCreateJwtAuthenticationFilterWithDeps()` 中使用 `new InMemoryTokenBlacklist()` 的 import 依赖未明确 | 在修改详情中明确要求保留 `InMemoryTokenBlacklist` import（第3行），并补充说明保留原因 |
