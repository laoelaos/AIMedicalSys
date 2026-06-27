# Phase 1 Package B + D 问题追踪

> 来源：3 个子 agent 联合审查（后端包B、前端包D、跨切面集成与安全）
> 分级：🔴 严重（Critical）= 必须修 / 🟡 重要（High）= 尽快修 / 🔵 一般（Medium）= 建议修

---

## 🔴 严重（Critical）

| # | 模块 | 问题 | 位置 | 说明 |
|---|------|------|------|------|
| C1 | 后端 B | **用户禁用后 token 仍然有效** | `JwtAuthenticationFilter.java:60-93`, `AuthServiceImpl.getCurrentUser()` | Filter 和 Service 均未从 DB 检查 `enabled` 状态，禁用用户后旧 token 仍可访问全部接口 |
| C2 | 后端 B | **登录接口无防暴力破解** | `AuthController.java:47-51`, `AuthServiceImpl.java:59-75` | `/api/auth/login` 无速率限制、验证码、失败锁定，攻击者可无限次密码猜测 |
| C3 | 后端 B | **`userId` 提取逻辑重复 4 次** | `AuthServiceImpl.java:116-124,160-168,185-193`, `JwtAuthenticationFilter.java:105-113` | Claims Integer→Long 兼容代码完全重复，违反 DRY，抽到 `JwtUtil.getUserIdFromClaims()` |
| C4 | 后端 B | **后端登出为 No-Op，无 token 黑名单** | `AuthServiceImpl.java:102-106` | `logout()` 方法体为空，Token 签发后服务端无法废除，窃取的 token 可用至过期 |
| C5 | 后端 B | **Access Token 自刷新，无 refresh token 轮换** | `AuthServiceImpl.java:109-150` | 用 access token 自身刷新，旧 token 不失效；泄露后可无限续期 |
| C6 | 前端 D | **前端 `logout()` 缺少 `try-finally`** | `shared/stores/auth.ts:98-101` | API 抛异常时 `clearAuthData()` 不执行，认证数据残留 |
| C7 | 前端 D | **导航守卫竞态（race condition）** | `doctor/router/index.ts:49-78`, `admin/router/index.ts:49-78` | 快速触发路由跳转可并发调用 `fetchMenus()`，导致重复 `addRoute` |
| C8 | 后端 B | **CSRF 完全禁用且无说明** | `SecurityConfigPhase1.java:49` | `csrf.disable()` 虽在 Bearer token 模式下合理，但 Phase2 Cookie 迁移时存在漏洞 |

---

## 🟡 重要（High）

| # | 模块 | 问题 | 位置 | 说明 |
|---|------|------|------|------|
| H1 | 后端 B | **`UserRepository.findByUsername()` 返回非 Optional** | `UserRepository.java:15` | 返回原始 `User` 依赖 null 检查，不符合 Spring Data JPA 惯例 |
| H2 | 后端 B | **JWT `SecretKey` 应缓存** | `JwtUtil.java:60,87` | `Keys.hmacShaKeyFor()` 每次 Token 操作重新构造，应 `@PostConstruct` 初始化一次 |
| H3 | 前端 D | **401 拦截器不会触发自动 refresh** | `shared/api/index.ts:42-43` | 活跃会话中 token 过期不会尝试刷新，用户静默登出 |
| H4 | 前端 D | **`convertMenusToRoutes` 硬编码父路由名 `'Layout'`** | `shared/stores/menu.ts:99` | 若 Layout 路由 name 变更，`addRoute` 静默回根，动态路由不生效 |
| H5 | 前端 D | **`convertMenusToRoutes` 仅处理两级菜单** | `shared/stores/menu.ts:68-80` | 三级+ 子菜单不会变成路由，需改为递归 |
| H6 | 前端 D | **循环依赖脆弱** | `doctor/stores/menu.ts:13`, `doctor/router/index.ts:3` | 顶层 `import router` 形成 A→B→A 循环，scope hoisting 可能改变执行顺序 |
| H7 | 前端 D | **导航守卫阻塞 UI** | `doctor/router/index.ts:62`, `admin/router/index.ts:62` | `await fetchMenus()` 期间所有导航挂起，慢网络下无反馈 |
| H8 | 后端 B | **密码复杂度弱** | `LoginRequest.java:20,27` | 仅 `@Size(min=6)`，无大小写/数字/特殊字符要求 |
| H9 | 整体 | **Swagger/API 文档公开暴露** | `SecurityConfigPhase1.java:61-62` | `/swagger-ui/**` 和 `/v3/api-docs/**` 为 `permitAll()`，攻击者可获取完整 API 定义 |
| H10 | 整体 | **JWT Filter 静默跳过无效 token** | `JwtAuthenticationFilter.java:62-65,71-73` | token 过期/无效时不返回 401，由后续 filter 链自行决定 |
| H11 | 后端 B | **`ProfileUpdateRequest` 无手机号格式校验** | `ProfileUpdateRequest.java` | 仅限制最大长度，无格式正则 |
| H12 | 后端 B | **用户/角色变更后 token 仍然有效** | `JwtAuthenticationFilter.java` | JWT payload 角色不会随 DB 更新而撤销 |

---

## 🔵 一般（Medium）

| # | 模块 | 问题 | 位置 | 说明 |
|---|------|------|------|------|
| M1 | 后端 B | **`buildUserInfoResponse` 私有方法不可测试** | `AuthServiceImpl.java` | 不可单独 Mock 测试，也无法复用 |
| M2 | 后端 B | **`updateProfile()` 多余 `save()` 写回** | `AuthServiceImpl.java` | `@Transactional` 下脏检查自动 flush |
| M3 | 后端 B | **`TokenStore` 死代码** | `admin/entity/TokenStore.java` | 存在于 admin 模块但 Phase 1 完全不引用 |
| M4 | 后端 B | **`AuthController` 直接依赖 `JwtUtil`** | `AuthController.java` | 不应在 Controller 层做 token 提取，应从 `SecurityContextHolder` 获取 |
| M5 | 后端 B | **医生岗位取 `iterator().next()`** | `AuthServiceImpl.java:77-81` | 多岗位时返回顺序不可靠 |
| M6 | 后端 B | **DTO 未用 Java 17 `record`** | 全部 DTO | 增加了约 40% 样板代码 |
| M7 | 后端 B | **Entity 风格混用** | `User/Role/Post/Function` vs `TokenStore` | 手写 getter/setter 与 Lombok `@Data` 不一致 |
| M8 | 后端 B | **`Function.java` 类名冲突** | `permission/Function.java` | 与 `java.util.function.Function` 冲突 |
| M9 | 后端 B | **`login()` 全量查询 + 懒加载** | `AuthServiceImpl.java` | 查询全量 User 实体及关联，可考虑投影 DTO |
| M10 | 后端 B | **Phase0/Phase1 SecurityConfig 并发激活** | `application.yml`, `SecurityConfigPhase1/0.java` | 两个 PasswordEncoder bean 可能导致冲突 |
| M11 | 前端 D | **`activeMenu` 状态冗余** | `shared/stores/menu.ts:23-24`, `SidebarBase.vue:112-115` | 完全可依赖 `route.path` |
| M12 | 前端 D | **`SidebarBase` 直接用 `useRoute()`** | `SidebarBase.vue:86` | 与注入 `routerInstance` 模式不一致 |
| M13 | 前端 D | **`apiGet<T>` catch 分支类型不安全** | `shared/api/index.ts:62` | `error as BusinessError` 是 type assertion 而非收窄 |
| M14 | 前端 D | **`BusinessError.isBusinessError` 可选属性不严谨** | `shared/types/index.ts:24-28` | 编译期无法强制校验 |
| M15 | 前端 D | **无前端输入校验** | `shared/components/LoginBase.vue:64-67` | username/password 无任何前端校验，增加攻击面 |
| M16 | 前端 D | **用户信息明文存 localStorage** | `shared/stores/auth.ts:57-59` | 完整 UserInfo（含 role/permissions）可由 XSS 读取 |
| M17 | 整体 | **Actuator 端点暴露** | `SecurityConfigPhase1.java:58-60` | health/info 公开，metrics 在 dev 中启用 |
| M18 | 整体 | **401 响应体结构不一致** | `JwtAuthenticationFilter.java:62-65` vs `AuthController.java:94-95` | Filter 跳过不写 response 与 Controller 手动 `Result.fail` 格式不同 |
| M19 | 整体 | **用户不存在时菜单返回空数组** | `MenuServiceImpl.java:52-56` | 与 AuthController 返回 UNAUTHORIZED 的行为不一致 |
