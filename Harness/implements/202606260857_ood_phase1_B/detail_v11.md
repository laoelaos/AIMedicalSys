# 详细设计（v11）

## 概述

在 `common-module-impl/auth/security/` 包下新建 `SecurityConfigPhase1`，将 R4-R10 已实现的所有 Filter（GlobalRateLimitFilter、JwtAuthenticationFilter、PasswordChangeCheckFilter）和 Handler（RestAuthenticationEntryPoint、RestAccessDeniedHandler）装配为统一的 Spring Security 配置链。然后删除 `application/.../config/SecurityConfigPhase1.java`（旧文件引用了已不存在的旧 `JwtAuthenticationFilter`）。

## 文件规划

| 文件路径（相对 `AIMedical/backend/`） | 操作 | 职责 |
|---------|------|------|
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/SecurityConfigPhase1.java` | 新建 | Spring Security 配置聚合点：SecurityFilterChain、Filter 装配、密码编码器 Bean |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/SecurityConfigPhase1Test.java` | 新建 | 4 个单元测试，无 Spring 上下文 |
| `application/src/main/java/com/aimedical/config/SecurityConfigPhase1.java` | 删除 | 旧配置引用已不存在的旧 JwtAuthenticationFilter，必须替换 |

## 类型定义

### SecurityConfigPhase1

**形态**：`@Configuration @Profile("phase1") @EnableWebSecurity @EnableMethodSecurity` class
**包路径**：`com.aimedical.modules.commonmodule.auth.security`
**职责**：Spring Security 配置聚合点，装配所有 Filter/Handler 为统一的 SecurityFilterChain，提供 PasswordEncoder、SlidingWindowCounter、TokenBlacklist 等基础 Bean。

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Profile("phase1")
public class SecurityConfigPhase1 {

    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

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

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            GlobalRateLimitFilter globalRateLimitFilter,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            PasswordChangeCheckFilter passwordChangeCheckFilter)
            throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(Customizer.withDefaults())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new RestAuthenticationEntryPoint())
                .accessDeniedHandler(new RestAccessDeniedHandler()))
            .authorizeHttpRequests(auth -> {
                auth
                    .requestMatchers("/api/auth/login").permitAll()
                    .requestMatchers("/api/auth/refresh").permitAll()
                    .requestMatchers("/api/auth/logout").authenticated()
                    .requestMatchers("/api/auth/**").authenticated()
                    .requestMatchers("/api/menu/**").authenticated()
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/actuator/info").permitAll()
                    .requestMatchers("/actuator/**").denyAll()
                    .requestMatchers("/swagger-ui/**").denyAll()
                    .requestMatchers("/v3/api-docs/**").denyAll()
                    .requestMatchers("/error").permitAll();
                if (h2ConsoleEnabled) {
                    auth.requestMatchers("/h2-console/**").permitAll();
                } else {
                    auth.requestMatchers("/h2-console/**").denyAll();
                }
                auth.anyRequest().authenticated();
            })
            .headers(headers -> headers.frameOptions(fo -> fo.sameOrigin()))
            .addFilterBefore(globalRateLimitFilter, JwtAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(passwordChangeCheckFilter, JwtAuthenticationFilter.class);
        return http.build();
    }
}
```

**公开接口**：

| 方法签名 | 返回 | 说明 |
|---------|------|------|
| `PasswordEncoder passwordEncoder()` | `PasswordEncoder` | @Bean，返回 BCryptPasswordEncoder 实例 |
| `SlidingWindowCounter slidingWindowCounter()` | `SlidingWindowCounter` | @Bean，返回单例 SlidingWindowCounter |
| `TokenBlacklist tokenBlacklist()` | `TokenBlacklist` | @Bean，返回 InMemoryTokenBlacklist 实例 |
| `GlobalRateLimitFilter globalRateLimitFilter(SlidingWindowCounter)` | `GlobalRateLimitFilter` | @Bean，注入 counter，返回 GlobalRateLimitFilter |
| `JwtAuthenticationFilter jwtAuthenticationFilter(JwtUtil, TokenBlacklist, UserRepository)` | `JwtAuthenticationFilter` | @Bean，注入三个依赖，返回 JwtAuthenticationFilter |
| `PasswordChangeCheckFilter passwordChangeCheckFilter()` | `PasswordChangeCheckFilter` | @Bean，无参，返回 PasswordChangeCheckFilter |
| `SecurityFilterChain filterChain(HttpSecurity, GlobalRateLimitFilter, JwtAuthenticationFilter, PasswordChangeCheckFilter)` | `SecurityFilterChain` | @Bean，装配完整的 SecurityFilterChain |

**构造方式**：无构造器（Lombok 无参），由 Spring 通过 `@Configuration` 扫描创建实例。

**类型关系**：无继承/实现关系，纯 `@Configuration` 类。

## 错误处理

SecurityConfigPhase1 自身不处理业务错误。它通过 exceptionHandling 配置将错误传播委托给：
- `RestAuthenticationEntryPoint`：捕获未认证请求（AuthenticationException），返回 401
- `RestAccessDeniedHandler`：捕获已认证但无权限请求（AccessDeniedException），返回 403 或 PASSWORD_CHANGE_REQUIRED

## 行为契约

### Bean 创建契约

| Bean | 前置条件 | 后置条件 |
|------|---------|---------|
| `passwordEncoder()` | 无 | 返回非 null 的 BCryptPasswordEncoder |
| `slidingWindowCounter()` | 无 | 返回非 null 的 SlidingWindowCounter，多次调用返回同一实例（单例） |
| `tokenBlacklist()` | 无 | 返回非 null 的 InMemoryTokenBlacklist |
| `globalRateLimitFilter(counter)` | counter 非 null | 返回非 null 的 GlobalRateLimitFilter |
| `jwtAuthenticationFilter(jwtUtil, tokenBlacklist, userRepository)` | 三个参数均非 null | 返回非 null 的 JwtAuthenticationFilter，包级私有构造器可访问 |
| `passwordChangeCheckFilter()` | 无 | 返回非 null 的 PasswordChangeCheckFilter，包级私有构造器可访问 |

### filterChain 契约

**URL 路由规则优先级**（按声明顺序）：

| 路径模式 | 访问规则 |
|---------|---------|
| `/api/auth/login` | permitAll |
| `/api/auth/refresh` | permitAll |
| `/api/auth/logout` | authenticated |
| `/api/auth/**` | authenticated |
| `/api/menu/**` | authenticated |
| `/actuator/health` | permitAll |
| `/actuator/info` | permitAll |
| `/actuator/**` | denyAll |
| `/swagger-ui/**` | denyAll |
| `/v3/api-docs/**` | denyAll |
| `/error` | permitAll |
| `/h2-console/**` | permitAll（h2ConsoleEnabled=true）/ denyAll（else） |
| `/**` | authenticated |

**Filter 注册顺序**：
1. `addFilterBefore(GlobalRateLimitFilter, JwtAuthenticationFilter.class)` — 全局 IP 限流，优先于 JWT 认证
2. `addFilterBefore(JwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)` — JWT 鉴权
3. `addFilterAfter(PasswordChangeCheckFilter, JwtAuthenticationFilter.class)` — 密码变更检查，在 Jwt 之后执行

**跨域**：使用 `cors(Customizer.withDefaults())`，采用 Spring 默认 CORS 配置（由 `@CrossOrigin` 或 `CorsConfigurationSource` Bean 进一步定制）。

**H2 Console 条件逻辑**：读取 `spring.h2.console.enabled` 配置属性（默认 false），true 时放行 `/h2-console/**` 路径供开发调试，false 时完全拒绝。

## 依赖关系

### 本包内引用（`com.aimedical.modules.commonmodule.auth.security`）
- `GlobalRateLimitFilter` — 同包，通过 `@Bean` 方法参数注入
- `JwtAuthenticationFilter` — 同包，通过 `@Bean` 方法参数注入（包级私有构造器可访问）
- `PasswordChangeCheckFilter` — 同包，通过 `@Bean` 方法参数注入（包级私有构造器可访问）
- `RestAuthenticationEntryPoint` — 同包，在 filterChain 中 `new` 创建
- `RestAccessDeniedHandler` — 同包，在 filterChain 中 `new` 创建

### 其他模块引用
- `com.aimedical.modules.commonmodule.jwt.JwtUtil` — @Component，参数注入
- `com.aimedical.modules.commonmodule.jwt.JwtConfig` — @ConfigurationProperties，由 JwtUtil 间接使用
- `com.aimedical.modules.commonmodule.auth.blacklist.TokenBlacklist` — 接口，本类 @Bean 方法返回 InMemoryTokenBlacklist
- `com.aimedical.modules.commonmodule.auth.blacklist.InMemoryTokenBlacklist` — 实现类
- `com.aimedical.modules.commonmodule.auth.rateLimit.SlidingWindowCounter` — 工具类，本类 @Bean 方法返回
- `com.aimedical.modules.commonmodule.permission.UserRepository` — Spring Data @Repository，参数注入

### 框架依赖
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
- `org.springframework.beans.factory.annotation.Value`
- `org.springframework.security.config.Customizer`

### 已删除文件的依赖解除
- 旧 `application/src/main/java/com/aimedical/config/SecurityConfigPhase1.java` 引用了 `com.aimedical.config.JwtAuthenticationFilter`（已不存在），删除后 application 模块不再引用此旧 Filter
- 需确认无其他文件引用旧 `SecurityConfigPhase1` 或旧 `JwtAuthenticationFilter`

## 单元测试设计

### SecurityConfigPhase1Test

**形态**：class（JUnit 5），纯单元测试，无 Spring 上下文
**包路径**：`com.aimedical.modules.commonmodule.auth.security.SecurityConfigPhase1Test`

**测试夹具**：

```java
class SecurityConfigPhase1Test {
    private final SecurityConfigPhase1 config = new SecurityConfigPhase1();
    // Mock 依赖
    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final UserRepository userRepository = mock(UserRepository.class);
}
```

**测试方法清单**（4 用例）：

| # | 测试方法 | 场景设置 | 验证点 |
|---|---------|---------|--------|
| 1 | `shouldCreateAllBeans` | 无 Spring 上下文，手动调用所有 @Bean 方法 | 每个 @Bean 方法返回非 null |
| 2 | `shouldReturnBCryptPasswordEncoder` | 调用 `config.passwordEncoder()` | 返回 `BCryptPasswordEncoder` 实例 |
| 3 | `shouldReturnInMemoryTokenBlacklist` | 调用 `config.tokenBlacklist()` | 返回 `InMemoryTokenBlacklist` 实例 |
| 4 | `shouldCreateJwtAuthenticationFilterWithDeps` | 调用 `config.jwtAuthenticationFilter(jwtUtil, config.tokenBlacklist(), userRepository)` | 返回非 null 的 `JwtAuthenticationFilter` |

**测试关键细节**：
- 用例 1：分别创建 `SlidingWindowCounter counter = config.slidingWindowCounter()`、`TokenBlacklist blacklist = config.tokenBlacklist()`、`GlobalRateLimitFilter g = config.globalRateLimitFilter(counter)`、`JwtAuthenticationFilter j = config.jwtAuthenticationFilter(jwtUtil, blacklist, userRepository)`、`PasswordChangeCheckFilter p = config.passwordChangeCheckFilter()`，逐一 `assertNotNull`
- 用例 4：需要 mock `JwtUtil` 和 `UserRepository`，调用 `config.tokenBlacklist()` 获取真实 tokenBlacklist（InMemoryTokenBlacklist 构造器 public，直接可用），验证返回的 filter 非 null
- 不使用 `MockHttpSecurity` / `spring-security-test`（避免 Spring 上下文启动），`filterChain` 方法由集成测试覆盖
- 剩余 `passwordEncoder`、`passwordChangeCheckFilter`、`globalRateLimitFilter` 的非 null 验证已在用例 1 中覆盖

### 暴露给后续任务的公开接口
- 所有 `@Bean` 方法 — 供 Spring 容器和集成测试使用
- `SecurityFilterChain filterChain(...)` — 供 Spring Security 自动配置使用
- `Deletion` 旧 `SecurityConfigPhase1` — 消除旧路径引用，为 Stage 3 业务接口开发铺平道路

## 修订说明（v11 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] 测试用例 3 `shouldReturnSingleSlidingWindowCounterInstance` 用 `assertSame` 验证 `@Bean` 单例语义，但无 Spring 上下文时 `@Bean` 不被处理，`assertSame` 必然失败 | 删除该测试用例（`@Bean` 单例语义由 Spring 框架保障，无需单元测试验证）。用例数从 5 减至 4，同时删除对应的"用例 3"测试关键细节描述 |
