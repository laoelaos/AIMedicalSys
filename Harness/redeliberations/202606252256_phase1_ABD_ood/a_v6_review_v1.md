# OOD 设计方案审查报告（v6）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 设计中的类型形态选择（interface / class / record / enum / @Component / @Configuration）均与 Java 17 类型系统完全匹配。单继承、多接口实现约束遵守正确。泛型使用标准（如 `ConcurrentHashMap<String, AttemptRecord>`、`Optional<User>` 等），无超范围使用。协作关系中描述的 Filter → Service → Repository 交互模式可在 Java 中直接实现，无技术障碍。

### 2. 标准库与生态覆盖

**[通过]** 所有依赖均在 Spring Boot 生态覆盖范围内：Spring Security（SecurityFilterChain、OncePerRequestFilter、BCryptPasswordEncoder、AuthenticationEntryPoint）、JWT（jjwt 或等效库）、Jackson（record 序列化/反序列化）、Bean Validation（@NotBlank、@Size、@Pattern、@Email）、java.util.concurrent（ConcurrentHashMap、ScheduledExecutorService、ReentrantLock）。无第三方库假设超出合理范围。

### 3. 语言特性可行性

**[通过]** 错误处理策略（BusinessException → GlobalExceptionHandler）与 Spring 异常处理机制匹配。并发设计（ConcurrentHashMap.compute 原子操作、ReentrantLock 滑动窗口、ScheduledExecutorService 定时清理）均使用标准 Java 并发原语。资源管理（内存数据结构 + 定时清理）在 JVM 堆空间内可行。模块/包结构符合 Spring Boot 标准布局。Java 17 record 用于 DTO 与 Jackson 反序列化兼容（构造函数绑定）。

### 4. 设计一致性

**[通过]** 各抽象职责描述清晰无歧义，1.3 节核心抽象一览明确了每个抽象的形态和职责。协作关系形成闭环：登录流程（3.1.1）→ 已认证请求（3.1.2）→ Token 刷新（3.1.3）→ 登出（3.1.4）→ 获取用户（3.1.5）完整无缺。行为契约描述（JwtAuthenticationFilter、PasswordChangeCheckFilter）细化到步骤级，足以指导实现。模块依赖方向（2.2 节）单向清晰，无循环依赖。

迭代需求中的 7 项审查意见全部得到闭环处理：
- 时序侧信道攻击 → dummy BCrypt 比对（3.1.1 步骤 5）
- 旧 Refresh Token 撤销 → tokenVersion 机制（4.2 节、5.1 节）
- 密码变更前端恢复流程 → 完整恢复链定义（3.4 节、7.4 节）
- 菜单展平路由冲突 → 注册策略 + name 唯一性明确（8.2 节 H5）
- PASSWORD_CHANGE_REQUIRED 前端处理 → axios 拦截器规则（7.4 节）
- 步骤 7 失败计数维度 → 指定"用户名维度"（3.1.1 步骤 7）
- JwtTokenProvider Bean 类型 → @Component（1.3 节、4.7 节）

### 5. 设计质量

**[通过]** 职责划分遵循 SRP：JwtAuthenticationFilter 仅负责鉴权、PasswordChangeCheckFilter 仅检查密码变更、GlobalRateLimitFilter 仅处理限流。抽象层次恰当：接口定义契约（AuthService、RateLimitGuard、TokenBlacklist、PasswordPolicy），实现类封装细节，不过度设计。设计便于后续详细设计和实现（Filter 链顺序、DTO 定义、ErrorCode 枚举均已明确）。可测试性考虑充分：CurrentUser 接口可 mock、Filter 行为可独立测试、PasswordPolicy 接口可替换。

## 修改要求

无
