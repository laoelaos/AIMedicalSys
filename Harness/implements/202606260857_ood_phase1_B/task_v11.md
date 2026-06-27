# 任务指令（v11）

## 动作
NEW

## 任务描述

在 `common-module-impl/auth/security/` 包下新建 `SecurityConfigPhase1.java`，将 R4-R10 已实现的所有 Filter/Handler 装配为统一的 Spring Security 配置链，然后删除旧 `application/.../config/SecurityConfigPhase1.java`。

### 新建文件（2 个）
1. `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/SecurityConfigPhase1.java`
2. `.../test/.../auth/security/SecurityConfigPhase1Test.java`（4-6 个测试）

### 删除文件（1 个）
3. `application/src/main/java/com/aimedical/config/SecurityConfigPhase1.java`

## 选择理由

- 所有 Filter/Handler 已在 R4-R10 逐一实现，各自通过包级私有构造器解耦，需由 SecurityConfigPhase1 统一装配
- 旧 application 层的 SecurityConfigPhase1 引用的是已不存在的旧 JwtAuthenticationFilter（R9 已将其迁移至 common-module-impl），必须替换
- 根据 OOD 3.3 SecurityFilterChain 配置，Filter 注册顺序为：GlobalRateLimitFilter → JwtAuthenticationFilter → PasswordChangeCheckFilter
- SecurityConfigPhase1 完成后，Stage 2（安全 Filter/限流/黑名单）全部就位，为 Stage 3（业务接口/服务/抽象）铺平道路

## 任务上下文

### 待创建文件路径（相对于项目根 `C:/Develop/Software/AIMedicalSys`）

| 操作 | 文件路径 |
|------|---------|
| 新建 | `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/SecurityConfigPhase1.java` |
| 新建 | `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/SecurityConfigPhase1Test.java` |
| 删除 | `AIMedical/backend/application/src/main/java/com/aimedical/config/SecurityConfigPhase1.java` |

### 类设计

**SecurityConfigPhase1**（位于 `com.aimedical.modules.commonmodule.auth.security` 包）

形态：`@Configuration @Profile("phase1") @EnableMethodSecurity` 类，位于 `auth/security` 包下（与过滤器同包，可访问包级私有构造器）。

```java
@Configuration
@Profile("phase1")
@EnableMethodSecurity
public class SecurityConfigPhase1 {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SlidingWindowCounter slidingWindowCounter() {
        return new SlidingWindowCounter();
    }

    @Bean
    public TokenBlacklist tokenBlacklist() {
        return new InMemoryTokenBlacklist();
    }

    @Bean
    public GlobalRateLimitFilter globalRateLimitFilter(SlidingWindowCounter counter) {
        return new GlobalRateLimitFilter(counter);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtUtil jwtUtil, TokenBlacklist tokenBlacklist, UserRepository userRepository) {
        return new JwtAuthenticationFilter(jwtUtil, tokenBlacklist, userRepository);
    }

    @Bean
    public PasswordChangeCheckFilter passwordChangeCheckFilter() {
        return new PasswordChangeCheckFilter();
    }
}
```

### SecurityFilterChain 配置（OOD 3.3）

```
csrf.disable()
sessionManagement.sessionCreationPolicy(STATELESS)
cors(Customizer.withDefaults())
exceptionHandling:
  authenticationEntryPoint(new RestAuthenticationEntryPoint())
  accessDeniedHandler(new RestAccessDeniedHandler())

authorizeHttpRequests:
  /api/auth/login          → permitAll
  /api/auth/refresh        → permitAll
  /api/auth/logout         → authenticated
  /api/auth/**             → authenticated
  /api/menu/**             → authenticated
  /actuator/health         → permitAll
  /actuator/info           → permitAll
  /actuator/**             → denyAll
  /swagger-ui/**           → denyAll
  /v3/api-docs/**          → denyAll
  /error                   → permitAll
  /h2-console/**           → permitAll (if h2ConsoleEnabled) / denyAll (else)
  /**                      → authenticated

headers.frameOptions.sameOrigin()

addFilterBefore(globalRateLimitFilter, JwtAuthenticationFilter.class)
addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
addFilterAfter(passwordChangeCheckFilter, JwtAuthenticationFilter.class)
```

### 依赖注入说明

| Bean | 来源 | 注入方式 |
|------|------|---------|
| `JwtUtil` | `@Component`（R3） | 参数注入 `@Bean` 方法 |
| `JwtConfig` | `@ConfigurationProperties`（R3） | 由 Spring Boot 自动绑定 |
| `UserRepository` | Spring Data `@Repository`（R1） | 参数注入 `@Bean` 方法 |
| `SlidingWindowCounter` | 本类 `@Bean` | 单例，首次创建 |
| `TokenBlacklist` (InMemoryTokenBlacklist) | 本类 `@Bean` | 单例，首次创建 |

### 旧文件删除注意事项

- 删除 `application/.../config/SecurityConfigPhase1.java` 前确认无其他文件引用它
- 旧文件引用的 `JwtAuthenticationFilter` 路径为 `com.aimedical.config.JwtAuthenticationFilter`（已不存在于 application 包中）
- `SecurityConfigPhase0.java` 保留不变（still valid for phase0 profile）

### 测试设计

**SecurityConfigPhase1Test**（JUnit 5，推荐无 Spring 上下文的纯单元测试或最小 Spring 上下文）

测试要点：
1. 创建 `SecurityConfigPhase1` 实例，调用所有 `@Bean` 方法，验证返回非 null
2. 验证 `passwordEncoder()` 返回 `BCryptPasswordEncoder` 实例
3. 验证 `slidingWindowCounter()` 返回单例（多次调用同一实例）
4. 验证 `tokenBlacklist()` 返回 `InMemoryTokenBlacklist` 实例
5. 验证 `globalRateLimitFilter()` 返回非 null
6. 验证 `jwtAuthenticationFilter()` 返回非 null（Mock 依赖）
7. 验证 `passwordChangeCheckFilter()` 返回非 null
8. 可选：使用 `spring-security-test` 的 `MockHttpSecurity` 验证 Filter 注册顺序

### 依赖清单

**生产代码依赖**：
- `org.springframework.security.config.annotation.web.builders.HttpSecurity`
- `org.springframework.security.config.annotation.web.configuration.EnableWebSecurity`
- `org.springframework.security.config.http.SessionCreationPolicy`
- `org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder`
- `org.springframework.security.crypto.password.PasswordEncoder`
- `org.springframework.security.web.SecurityFilterChain`
- `org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter`
- `org.springframework.context.annotation.Bean`
- `org.springframework.context.annotation.Configuration`
- `org.springframework.context.annotation.Profile`
- `org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity`
- `org.springframework.boot.autoconfigure.condition.ConditionalOnProperty`

**本包内引用**（`com.aimedical.modules.commonmodule.auth.security`）：
- `GlobalRateLimitFilter` — 同包
- `JwtAuthenticationFilter` — 同包
- `PasswordChangeCheckFilter` — 同包
- `RestAuthenticationEntryPoint` — 同包
- `RestAccessDeniedHandler` — 同包

**其他模块引用**：
- `com.aimedical.modules.commonmodule.jwt.JwtUtil` — @Component
- `com.aimedical.modules.commonmodule.blacklist.TokenBlacklist` — 接口
- `com.aimedical.modules.commonmodule.blacklist.InMemoryTokenBlacklist` — 实现
- `com.aimedical.modules.commonmodule.rateLimit.SlidingWindowCounter` — 工具类
- `com.aimedical.modules.commonmodule.permission.UserRepository` — Spring Data

### 成功标准

- 编译通过：`mvn compile test-compile`
- 新建的测试全部通过
- 旧 SecurityConfigPhase1 已删除，application 模块无编译错误
- 全量测试：`mvn test` 通过（仅排除预存 UserRepositoryTest H2 语法失败）
