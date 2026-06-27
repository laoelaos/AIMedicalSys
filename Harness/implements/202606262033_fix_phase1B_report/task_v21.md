# 任务指令（v21）

## 动作
NEW

## 任务描述
测试补充，`SecurityConfigPhase1Test.java` 中新增测试方法，验证 `SecurityFilterChain` 中三个自定义 Filter 的注册顺序符合 OOD 3.3 节规定：

`GlobalRateLimitFilter` 最先 → `JwtAuthenticationFilter` → `PasswordChangeCheckFilter` 最后

仅修改 `SecurityConfigPhase1Test.java` 一个文件。

具体要求：
1. 利用 `spring-security-test`（6.2.4，已在 common-module-impl pom.xml 提供）创建 `HttpSecurity` 实例并调用 `config.filterChain()`
2. 通过 `SecurityFilterChain.getFilters()` 获取已注册的 Filter 列表
3. 验证三个 Filter 的相对顺序而非绝对索引位置（Spring Security 默认 Filter 会影响绝对索引）
4. 保留原有 3 个测试方法不变

## 选择理由
批次 7 第四个任务，也是全部 26 项缺陷（T1-T23, T25-T27）的最后一项。T15-T17 均已通过验证，T18 无前置依赖。完成后批次 7 全部交付，全部 26 项缺陷修复完成。

## 任务上下文
### 诊断说明（§T18）
- `SecurityConfigPhase1Test`（`auth/security/SecurityConfigPhase1Test.java`）仅验证了各 Bean 是否被创建（非 null 断言）
- OOD 3.3 节明确规定了 Filter 执行顺序：`GlobalRateLimitFilter` 最先 → `JwtAuthenticationFilter` → `PasswordChangeCheckFilter` 最后
- 顺序错误会导致安全漏洞（如限流在 JWT 认证之后执行）
- Filter 链的注册通过 `addFilterBefore` / `addFilterAfter` 实现 — SecurityConfigPhase1.java:97-99

### OOD 3.3 节引用
> 安全配置：...Filter 顺序：GlobalRateLimitFilter → JwtAuthenticationFilter → PasswordChangeCheckFilter

## 已有代码上下文
### SecurityConfigPhase1.filterChain() 关键代码
```java
.addFilterBefore(globalRateLimitFilter, JwtAuthenticationFilter.class)
.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
.addFilterAfter(passwordChangeCheckFilter, JwtAuthenticationFilter.class);
```

### SecurityConfigPhase1Test 现有结构
- 纯单元测试类（无 Spring 上下文注解），使用 `new SecurityConfigPhase1()` + `mock()` 创建依赖
- 现有 3 个测试方法：`shouldCreateAllBeans`、`shouldReturnBCryptPasswordEncoder`、`shouldCreateJwtAuthenticationFilterWithDeps`
- 测试包已包含 `spring-security-test:6.2.4` 依赖

### 可用测试依赖
- `spring-security-test:6.2.4` 提供 `org.springframework.security.test.web.*` 工具类
- 可通过 `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("phase1")` 创建集成测试，或纯单元构造 `HttpSecurity` 实例
- 验证方式：`mvn clean test` 后端全部测试通过

## 风险与注意事项
- 测试应验证三个 Filter 的**相对顺序**而非绝对索引位置（Spring Security 默认插入多个内置 Filter）
- 不要删除或修改现有的 3 个测试方法
- 注意：`SecurityConfigPhase1` 标注 `@Profile("phase1")`，集成测试需设置 `@ActiveProfiles("phase1")`
