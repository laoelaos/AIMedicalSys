# OOD 设计方案审查报告（v1）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 全部类型形态选择与 Java 17 类型系统能力匹配：
- 接口（AuthService、RateLimitGuard、TokenBlacklist、PasswordPolicy、CurrentUser）用于定义行为契约 — Java 接口完备支持
- 抽象类/具体类（JwtAuthenticationFilter extends OncePerRequestFilter、JwtTokenProvider、LoginAttemptTracker）— 单继承 + 多接口实现，符合 Java 约束
- Java 17 record 用于 DTO（LoginRequest、LoginResponse、UserInfoResponse 等）— Spring Boot 3.x + Jackson 完全支持 record 序列化/反序列化
- JPA @Entity（User、Role、Post、PermissionFunction）— 标准 JPA 映射，无约束违反
- Optional<User> 返回类型 — Spring Data JPA 原生支持
- 泛型使用限于 Result<T>、ConcurrentHashMap<K,V> 等标准用法，未超出 Java 泛型系统能力

### 2. 标准库与生态覆盖

**[通过]** 所有设计假设的能力均在标准库或常用库覆盖范围内：
- JWT 令牌 — jjwt 库（io.jsonwebtoken），HS256 签名算法，标准实现
- Spring Security — OncePerRequestFilter、SecurityFilterChain、@EnableMethodSecurity、BCryptPasswordEncoder、ExceptionTranslationFilter、AuthenticationEntryPoint，均为框架原生支持
- 并发安全 — ConcurrentHashMap、ScheduledExecutorService、ReentrantLock（java.util.concurrent 标准库）
- 参数校验 — @NotBlank、@Size、@Pattern、@Email、@Valid（Jakarta/Javax Bean Validation），Spring Boot 原生集成
- Jackson JSON — record 序列化兼容（Jackson >= 2.12）
- API 文档/Actuator — SpringDoc OpenAPI + Spring Boot Actuator，denyAll 完全可控

### 3. 语言特性可行性

**[通过]** 错误处理、并发、资源管理、模块组织策略均与 Java/Spring 能力匹配：
- 错误处理：BusinessException + @ControllerAdvice GlobalExceptionHandler + AuthenticationEntryPoint，统一 Result<T> 错误响应体，标准 Spring 异常处理模式
- 并发设计：InMemoryTokenBlacklist 使用 ConcurrentHashMap + ScheduledExecutorService；LoginAttemptTracker 使用 ConcurrentHashMap.compute() 原子更新；InMemoryRateLimitGuard 每 IP 独立锁；均符合 Java 内存模型
- 资源管理：Phase 1 纯内存无外部 I/O，Phase 2 引入 Redis 时 @Profile 切换实现
- 模块结构：common-module-api / common-module-impl 分离，@Profile("phase1") 隔离 Phase 0/1 SecurityConfig，@ComponentScan/@Import 跨模块注册 Filter — 均为 Spring 标准实践

### 4. 设计一致性

**[通过]** 主要设计完整闭环，依赖方向一致。

**[轻微]** 以下细节在一致性和完整性上可进一步优化：
- API 表（6.1）中 `PUT /api/auth/password` 引用了 `PasswordChangeRequest`，但 5.2 数据模型节未定义该 DTO（MenuCreateRequest、MenuUpdateRequest 同理）
- `RefreshTokenRequest` 在目录结构（2.1）中标注 "Phase 2 扩展"，但 `POST /api/auth/refresh` 端点已在 Phase 1 API 表中激活，刷新请求中 Refresh Token 的提交方式（header / body / 自定义头）未明确说明
- 6.1 表中 `/api/auth/refresh` 请求体标记为 `—`，与 3.1.3 流程中 "提交 Refresh Token" 的表述需对齐

### 5. 设计质量

**[通过]**
- 职责划分清晰：AuthService（认证业务）、JwtTokenProvider（令牌生命周期）、JwtAuthenticationFilter（请求拦截装配）、RateLimitGuard（限流）、LoginAttemptTracker（失败计数）、TokenBlacklist（撤销）、PasswordPolicy（密码策略）— 相互正交，单一职责
- 抽象层次恰当：接口契约+流程描述定义行为边界，不包含实现细节（如方法签名、字段定义），符合 OOD 设计粒度
- 可测试性良好：AuthServiceImpl 等核心类依赖接口而非具体实现，构造注入便于 Mock；InMemory* 实现可独立测试
- 扩展路径明确：Section 11 设计决策逐条记录选项与选择理由，Phase 1→Phase 2 迁移路径预先说明（黑名单/限流内存→Redis）

## 修改要求

无。设计方案在五个维度上均无严重或一般级别问题，予以通过。
