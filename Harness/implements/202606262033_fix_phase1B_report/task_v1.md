# 任务指令（v1）

## 动作
NEW

## 任务描述
修复 T26：删除 `SecurityConfigPhase1.java` 中与 `AuthModuleConfig.java` 重复的 `TokenBlacklist` Bean 定义。

**涉及文件：**
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/SecurityConfigPhase1.java`：删除第 41-44 行的 `tokenBlacklist()` Bean 方法
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/config/AuthModuleConfig.java`：无需修改，已有无 profile 限制的 `tokenBlacklist()` Bean

**验证方式：**
1. 运行 `SecurityConfigPhase1Test` 确认测试通过（测试中 `shouldCreateAllBeans` 和 `shouldReturnInMemoryTokenBlacklist` 从 `config.tokenBlacklist()` 获取 Bean，删除方法后需调整为从容器或 `AuthModuleConfig` 获取）
2. 启动应用并激活 phase1 profile，确认不再抛出 `BeanDefinitionOverrideException`

## 选择理由
- 批次 1：阻塞修复，使应用可启动
- P0 优先级，无前置依赖
- 为后续所有修复（T1-T23, T25-T27）提供可启动的应用基线

## 任务上下文
### 问题描述
`AuthModuleConfig.java:19-22` 定义了 `@Bean TokenBlacklist tokenBlacklist()`（无 profile 限制），`SecurityConfigPhase1.java:41-44` 也定义了同名的 `tokenBlacklist()` Bean（`@Profile("phase1")`）。Spring Boot 3.x 默认禁止 bean 覆盖（`spring.main.allow-bean-definition-overriding=false`），导致 phase1 profile 激活时 `BeanDefinitionOverrideException` 启动失败。

### 修复方案（选项 A，推荐）
删除 `SecurityConfigPhase1.java` 第 41-44 行的 `tokenBlacklist()` Bean 定义，依赖 `AuthModuleConfig` 中已有的全局 Bean。

### 影响分析
- `SecurityConfigPhase1.jwtAuthenticationFilter()` 方法（第 52-55 行）的参数注入 `TokenBlacklist` 不受影响，Spring 容器仍能从 `AuthModuleConfig` 中获取 TokenBlacklist
- 后续 T2/T12 修复将修改 `jwtAuthenticationFilter()` 方法签名（`JwtUtil` → `JwtTokenProvider`），本项变更与该修改无冲突

## 已有代码上下文

### AuthModuleConfig.java（保留）
```java
@Configuration
public class AuthModuleConfig {
    @Bean
    public RateLimitGuard rateLimitGuard() { return new InMemoryRateLimitGuard(); }
    @Bean
    public TokenBlacklist tokenBlacklist() { return new InMemoryTokenBlacklist(); }  // 保留此定义
    @Bean
    public LoginAttemptTracker loginAttemptTracker() { return new LoginAttemptTracker(); }
}
```

### SecurityConfigPhase1.java（待修改）
```java
@Configuration
@Profile("phase1")
public class SecurityConfigPhase1 {
    // ... 其他 Bean ...

    @Bean
    public TokenBlacklist tokenBlacklist() {           // ← 删除此方法（第 41-44 行）
        return new InMemoryTokenBlacklist();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtUtil jwtUtil, TokenBlacklist tokenBlacklist, UserRepository userRepository) {
        return new JwtAuthenticationFilter(jwtUtil, tokenBlacklist, userRepository);
    }
    // ...
}
```

### SecurityConfigPhase1Test.java（测试调整）
当前测试 `shouldReturnInMemoryTokenBlacklist()` 调用 `config.tokenBlacklist()`，删除 Bean 方法后该测试将编译失败。
```java
// 测试调整方案 1：改为通过 AuthModuleConfig 获取
// 测试调整方案 2：注入 TokenBlacklist 而非直接调用 config.tokenBlacklist()
// 测试调整方案 3：删除该测试（因 shouldCreateAllBeans 中已有 assertNotNull(blacklist)）
```
建议采用方案 3（删除冗余测试方法），或方案 1（新增 AuthModuleConfig 实例）。
