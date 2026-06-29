# R1: Package B 安全过滤器与配置实现审查

审查时间：2026-06-27

### 审查范围

1. `common-module-impl/auth/security/JwtAuthenticationFilter.java` — JWT 鉴权过滤器
2. `common-module-impl/auth/security/PasswordChangeCheckFilter.java` — 密码变更检查过滤器
3. `common-module-impl/auth/security/GlobalRateLimitFilter.java` — 全局 IP 限流过滤器
4. `common-module-impl/auth/security/SecurityConfigPhase1.java` — Spring Security 配置
5. `common-module-impl/auth/security/RestAuthenticationEntryPoint.java` — 认证入口点
6. `common-module-impl/auth/security/RestAccessDeniedHandler.java` — 拒绝访问处理器
7. `common-module-impl/auth/security/SecurityConfigPhase1CoexistenceTest.java` — 阶段共存测试
8. `common/config/GlobalExceptionHandler.java` — 全局异常处理
9. `common/exception/GlobalErrorCode.java` — 错误码

### 发现

#### [一般] GlobalExceptionHandler.resolveHttpStatus 缺少密码相关错误码映射

- **位置**：`common/config/GlobalExceptionHandler.java:47-71`
- **描述**：设计文档 10.1 节定义 PASSWORD_MISMATCH、PASSWORD_TOO_SHORT、PASSWORD_TOO_LONG、PASSWORD_WEAK、PASSWORD_CONTAINS_USERNAME、PASSWORD_COMMON 应返回 HTTP 200，但 `resolveHttpStatus()` 方法中无这些错误码的分支，默认落入 `HttpStatus.BAD_REQUEST` (400)，与设计契约不一致。
- **建议**：在 `resolveHttpStatus()` 中增加这些错误码到 `HttpStatus.OK` 的映射，或补充设计文档说明改用 400 的决策依据。

#### [轻微] JwtAuthenticationFilter 权限收集存在 N+1 潜在问题

- **位置**：`JwtAuthenticationFilter.java:118-129`、`UserRepository.java:17`
- **描述**：`collectAuthorities()` 中遍历 `user.getPosts()` 后进一步遍历 `post.getFunctions()`。`findWithDetailsById` 的 `@EntityGraph` 仅加载 `{"roles", "posts"}` 未包含 `"posts.functions"`，导致每次访问 `post.getFunctions()` 触发懒加载查询。虽然当前系统用户通常只有 1 个岗位、影响可控，但偏离了设计文档 7.1 节 "使用 @EntityGraph 显式控制 fetch join，避免 N+1" 的原则。
- **建议**：将 `findWithDetailsById` 的 `@EntityGraph` 扩展为 `{"roles", "posts", "posts.functions"}`，或让 `JwtAuthenticationFilter` 调用已有的 `findWithDetailsForMenuById` 方法。

#### [轻微] BCryptPasswordEncoder 未显式 strength 参数

- **位置**：`SecurityConfigPhase1.java:33`
- **描述**：`new BCryptPasswordEncoder()` 虽默认 strength=10，但设计文档 4.3 节显式要求 "BCryptPasswordEncoder (Strength 10)"。建议显式传入参数以增强可读性并与文档保持一致。
- **建议**：改为 `new BCryptPasswordEncoder(10)`。

#### [轻微] SecurityConfigPhase1CoexistenceTest 未覆盖异常处理器注入

- **位置**：`SecurityConfigPhase1CoexistenceTest.java:31-41`
- **描述**：共存测试验证了 Filter Bean 的创建和共存，但未覆盖 `RestAuthenticationEntryPoint` 和 `RestAccessDeniedHandler` 中 `MessageInterpolator` 的依赖注入路径。虽然运行时由 Spring 容器注入，但单元测试缺少对此链路的验证。
- **建议**：增加测试验证通过 `SecurityConfigPhase1.filterChain()` 构造时 `RestAuthenticationEntryPoint` 和 `RestAccessDeniedHandler` 能正确接收 `MessageInterpolator` 参数。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 0 |
| 一般 | 1 |
| 轻微 | 3 |

### 总评

安全过滤器与配置实现整体遵循 Docs/05_ood_phase1_B.md 设计，核心行为契约正确：

1. **Filter 顺序正确**：GlobalRateLimitFilter → JwtAuthenticationFilter → PasswordChangeCheckFilter，与设计 3.3 节一致
2. **SecurityFilterChain 路径配置准确**：permitAll(login/refresh/health/info/error)、authenticated(logout/auth/**/menu/**)、denyAll(actuator非health/info、swagger-ui、v3/api-docs)，与设计合约一致
3. **JwtAuthenticationFilter 行为完整**：token 提取→验证（含 type=access 校验）→黑名单检查→DB 查用户→enabled 检查（抛 AccountDisabledAuthenticationException）→passwordChangeRequired 存入 attribute→权限装配→SecurityContext 设置，与设计 3.3 节一致
4. **静默跳过策略正确**：token 无效/用户不存在/已删除时清除 SecurityContext 并放行
5. **PasswordChangeCheckFilter 白名单路径准确**：password(PUT)/logout(POST)/refresh(POST) 使用 AntPathRequestMatcher
6. **AuthenticationEntryPoint 区分 ACCOUNT_DISABLED 与其余未认证**，类型正确
7. **AccessDeniedHandler 区分 PasswordChangeRequiredException 与其余 AccessDeniedException**，状态码正确
8. **GlobalRateLimitFilter 白名单路径（login/refresh/health/info）和滑动窗口实现正确**
9. **GlobalErrorCode 枚举完整**，包含设计定义的所有错误码（含 `{锁定时间}` 占位符）
10. **两阶段 SecurityConfig 通过 @Profile("phase1") 隔离**

主要问题集中在 `GlobalExceptionHandler.resolveHttpStatus()` 缺少密码相关错误码到 200 的映射，与设计 10.1 节要求的 HTTP 状态码不一致。
