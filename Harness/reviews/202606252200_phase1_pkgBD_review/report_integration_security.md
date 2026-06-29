# Phase 1 跨切面集成与安全验证报告

## 认证流程端到端映射

```
┌────────────────────────────────────────────────────────────────────────────┐
│ 1. 登录流程                                                               │
└────────────────────────────────────────────────────────────────────────────┘

[前端 Login.vue]
  ↓ authStore.login({username, password})
[shared/stores/auth.ts:84]
  ↓ authApi.login(request)
[shared/api/index.ts:116]
  ↓ POST /api/auth/login  (axios → apiClient)
[shared/api/index.ts:27-55 响应拦截器]
  ├─ HTTP 2xx + body.code === "SUCCESS" → 提取 body.data → resolve
  └─ HTTP 2xx + body.code !== "SUCCESS" → reject(BusinessError)
     HTTP 非2xx → reject(BusinessError)
[apiPost 的 try-catch]
  └─ 返回 T | BusinessError
[stores/auth.ts:86-88]
  ├─ isBusinessError(response) ? return { success: false, errorMessage }
  └─ saveToken(response.token) + saveUser(response.user)
       └─ localStorage.setItem(`aimedical_${appType}_token`, token)
       └─ localStorage.setItem(`aimedical_${appType}_user`, JSON.stringify(user))
       └─ setAuthToken(token) → apiClient.defaults.headers.common['Authorization'] = `Bearer ${token}`
↓
[后端 AuthController.java:47] @PostMapping("/login")
  └─ authService.login(LoginRequest)
[AuthServiceImpl.java:59] @Transactional(readOnly=true)
  ├─ userRepository.findByUsername(request.getUsername())
  ├─ user == null → throw BusinessException(UNAUTHORIZED, "用户名或密码错误")
  ├─ !user.enabled → throw BusinessException(FORBIDDEN, "用户已被禁用")
  ├─ !passwordEncoder.matches(password, user.password) → throw BusinessException(UNAUTHORIZED, "用户名或密码错误")
  ├─ 构建 position (仅 DOCTOR 且有 posts)
  └─ jwtUtil.generateToken(userId, username, userType.code, position)
       ↓
[JwtUtil.java:51]
  ├─ claims: {userId, username, role, position?}
  ├─ SecretKey = Keys.hmacShaKeyFor(secret.getBytes(UTF_8))
  ├─ expiration = now + jwtConfig.expiration * 1000
  └─ Jwts.builder().claims(claims).subject(username).issuedAt(now).expiration(expiration).signWith(key).compact()
       ↓
[AuthServiceImpl.java:92-98]
  └─ LoginResponse{ token, tokenType="Bearer", expiresIn=86400, user=UserInfoResponse{id,username,realName,role,position?,permissions[]} }
       ↓
  [Result.success(response)] → JSON: { code:"SUCCESS", message:"成功", data:{...} }
       ↓
[前端 响应拦截器] body.code === "SUCCESS" → 提取 data
[stores/auth.ts:90-92] saveToken() + saveUser()
└→ 路由守卫 [router/index.ts:49-79] (doctor/admin 相同)
     ├─ to.meta.requiresAuth && !authStore.isAuthenticated → redirect /login
     ├─ to.meta.requiresAuth && !menuStore.hasMenus → await menuStore.fetchMenus()
     └─ else → next()

┌────────────────────────────────────────────────────────────────────────────┐
│ 2. 已认证请求流程                                                         │
└────────────────────────────────────────────────────────────────────────────┘

[前端 apiClient] Authorization: Bearer <token> (由 setAuthToken() 设置)
  ↓
[后端 JwtAuthenticationFilter.java:43] OncePerRequestFilter.doFilterInternal()
  ├─ request.getHeader("Authorization")
  ├─ null/empty → chain.doFilter() 放行
  ├─ extractToken() → 以 "Bearer " 开头 ? 截取 : null
  ├─ null → chain.doFilter() 放行
  ├─ jwtUtil.validateTokenAndGetClaims(token)
  │    ├─ null/empty → return null
  │    └─ parseToken() → catch ExpiredJwtException/UnsupportedJwtException/MalformedJwtException/SignatureException → log.warn → return null
  ├─ claims == null → chain.doFilter() 放行
  ├─ extractUserId(claims) == null → chain.doFilter()
  ├─ extractRole(claims) == null → chain.doFilter()
  └─ UsernamePasswordAuthenticationToken(principal=userId, credentials=null, authorities=[ROLE_{role}])
       ↓
  SecurityContextHolder.getContext().setAuthentication(authentication)
       ↓
  chain.doFilter(request, response)
       ↓
[Controller] 通过 @PreAuthorize 或 SecurityContextHolder.getContext().getAuthentication() 获取当前用户

┌────────────────────────────────────────────────────────────────────────────┐
│ 3. Token 刷新流程                                                         │
└────────────────────────────────────────────────────────────────────────────┘

[前端 stores/auth.ts:106] refreshToken()
  └─ authApi.refresh() → apiPost<LoginResponse>('/auth/refresh')
     (依赖 axios 拦截器自动附带 Authorization header)
       ↓
[后端 AuthController.java:74] @PostMapping("/refresh")
  ├─ extractToken(authHeader)
  ├─ null → Result.fail("UNAUTHORIZED", "未提供令牌")
  └─ authService.refreshToken(token)
[AuthServiceImpl.java:109]
  ├─ jwtUtil.validateTokenAndGetClaims(token) → null → throw BusinessException
  ├─ 解析 userId (Integer→Long 兼容)
  ├─ userRepository.findById(userId)
  └─ jwtUtil.generateToken(...) 生成全新 token (claims 从 DB 刷新)
  └─ 返回新 LoginResponse
[前端]
  ├─ isBusinessError → clearAuthData(), return false
  └─ saveToken()+saveUser(), return true

┌────────────────────────────────────────────────────────────────────────────┐
│ 4. 登出流程                                                              │
└────────────────────────────────────────────────────────────────────────────┘

[前端 stores/auth.ts:98] logout()
  ├─ authApi.logout() → apiPost<void>('/auth/logout')
  │    ↓ 后端 AuthController.java:59
  │   ├─ extractToken(authHeader)
  │   └─ authService.logout(token) → NO-OP (Phase1 无状态 JWT)
  └─ clearAuthData()
       ├─ localStorage.removeItem(TOKEN_KEY)
       ├─ localStorage.removeItem(USER_KEY)
       └─ clearAuthToken() → delete axios header

┌────────────────────────────────────────────────────────────────────────────┐
│ 5. 菜单获取与动态路由流程                                                 │
└────────────────────────────────────────────────────────────────────────────┘

[路由守卫 router/index.ts] to.meta.requiresAuth && !menuStore.hasMenus
  └─ menuStore.fetchMenus()
[shared/stores/menu.ts:110]
  └─ menuApi.tree() → apiGet<MenuItem[]>('/menu/tree')
       ↓
[MenuController.java:59] @GetMapping("/tree") @PreAuthorize("isAuthenticated()")
  ├─ getCurrentUserId() 从 SecurityContext 获取
  └─ menuService.getUserMenuTree(userId)
[MenuServiceImpl.java:52]
  ├─ userRepository.findById(userId)
  ├─ 聚合用户所有 post 的 functions
  ├─ filter(enabled == true)
  ├─ map(Function → MenuResponse)
  └─ buildMenuTree() → 树形嵌套 (children=null 为叶子)
       ↓
[前端 menu.ts:89] registerDynamicRoutes(response)
  ├─ convertMenusToRoutes() → 过滤 / /dashboard /login
  ├─ 每条路由: { path, name=permission||menu_{id}, component: DynamicPage, meta:{requiresAuth:true} }
  └─ routerInstance.addRoute('Layout', route) (去重检查)
```

---

## 各维度评价

### 1. 认证完整性
| 维度 | 评价 |
|------|------|
| 登录 | **好** — 完整的 username+password → BCrypt 校验 → JWT 签发 → 前端存储 → 路由跳转 |
| 已认证请求 | **好** — OncePerRequestFilter → JWT 提取 → Claims 验证 → SecurityContext 设置 |
| Token 刷新 | **中** — 存在，但缺乏 refresh token 机制（直接用 access token 刷新），旧 token 依然有效 |
| 登出 | **差** — 仅前端清除 token，后端无黑名单，已签发 token 持续有效到过期 |
| 菜单动态化 | **好** — 完整的 permission→menu→route 管线，延迟注册避免循环依赖，登出清理 |

### 2. 安全性
| 维度 | 评价 |
|------|------|
| JWT 秘密管理 | **好** — `${JWT_SECRET}` 环境变量，启动时校验 ≥32 字符 |
| Token 存储 | **差 (Phase1 已知)** — localStorage，XSS 可窃取（标注为 Phase2 迁移到 httpOnly cookie） |
| 密码哈希 | **好** — BCryptPasswordEncoder |
| 暴力破解防护 | **无** — 无速率限制、无验证码、无账户锁定 |
| CSRF 防护 | **无 (已知)** — csrf.disable()，API 使用 Bearer token 可接受 |
| API 端点保护 | **好** — 仅 `/api/auth/login`、`/ping`、actuator health/info 公开，其余需认证 |
| 错误信息泄露 | **好** — 登录失败统一返回"用户名或密码错误"(防用户枚举) |
| Refresh Token 轮换 | **差** — 无 refresh token，access token 自己刷新自己，旧 token 不失效 |

### 3. 集成一致性
| 检查项 | 结果 |
|--------|------|
| 前后端 API 路径 | **一致** — `/api/auth/*` 和 `/api/menu/*` 完全匹配 |
| Token 格式 | **一致** — 前端 `Authorization: Bearer <token>`，后端 JwtUtil 提取 "Bearer " 前缀 |
| 错误码映射 | **一致** — GlobalErrorCode (UNAUTHORIZED/FORBIDDEN/NOT_FOUND/PARAM_INVALID/SYSTEM_ERROR) 与前端 axios 拦截器处理的 HTTP 状态码一致 |
| 字段命名 | **一致** — `realName`(后端 nickname→realName) ↔ `realName`(前端 UserInfo) |
| 角色枚举 | **一致** — DOCTOR/ADMIN/PATIENT，前后端均使用 String code 传递 |
| 令牌过期处理缺口 | **差** — 前端无 401 自动刷新拦截器(仅在 initializeAuth 中有刷新尝试) |

---

## 集成可信度评分: **78/100**

| 分类 | 扣分项 |
|------|--------|
| -10% | 无服务端 token 黑名单，登出后 token 仍有效 |
| -5% | 无 refresh token 机制，access token 自刷新 |
| -4% | 前端无全局 401 自动刷新中间件（token 过期后静默丢失会话） |
| -3% | localStorage XSS 风险（Phase1 已知） |

---

## 安全发现

### 🔴 CRITICAL (必须修复)

| # | 文件位置 | 问题 | 影响 |
|---|---------|------|------|
| C1 | `AuthServiceImpl.java:102-106` | **登出 No-Op** — `logout()` 方法体为空，不做任何 token 黑名单/失效处理 | Token 一旦签发，即使服务端登出也无法废除。配合 C2，窃取的 token 可持续使用 24h |
| C2 | `AuthServiceImpl.java:109-150` | **Access Token 自刷新** — `/refresh` 接受 access token 本身作为凭据签发新 token，旧 token 不失效 | 无 refresh token 意味着一旦 access token 泄露，攻击者可以无限次刷新；且旧 token 持续有效直到过期 |
| C3 | `SecurityConfigPhase1.java:49` | **CSRF 完全禁用** — `csrf.disable()` | 虽然 Bearer token 方案部分缓解 CSRF，但当迁移到 httpOnly cookie（Phase2 计划）时 CSRF 防护缺失将变为可利用漏洞 |

### 🟡 WARNING (建议修复)

| # | 文件位置 | 问题 | 影响 |
|---|---------|------|------|
| W1 | `AuthServiceImpl.java:59-75` | **无暴力破解防护** — `/api/auth/login` 无速率限制、无验证码、无账户锁定 | 攻击者可尝试无限次密码猜测 |
| W2 | `LoginRequest.java:20,27` | **密码复杂度弱** — 仅 `@Size(min=6)`，无大小写/数字/特殊字符要求 | 用户可能设置弱密码 |
| W3 | `SecurityConfigPhase1.java:61-62` | **Swagger/API 文档暴露** — `/swagger-ui/**` 和 `/v3/api-docs/**` 设置为 `permitAll()` | 攻击者可获取全部 API 定义 |
| W4 | `SecurityConfigPhase1.java:58-60` | **Actuator 暴露** — `/actuator/**` 需要认证，但 health/info 公开；且 metrics 在 application-dev.yml 中被启用 | 信息泄露风险 |
| W5 | `frontend/api/index.ts:27-55` | **全局 401 拦截未触发自动刷新** — 当任意 API 返回 401 时，前端直接 reject(BusinessError)，不尝试 refresh | 过期 token 导致静默登出，用户体验差 |
| W6 | `JwtAuthenticationFilter.java:62-65,71-73` | **静默跳过失效 token** — 当 token 过期/无效时，过滤器仅跳过（不返回 401），由后续 Controller 或过滤器链自行决定 | 可能导致某些未受 @PreAuthorize 保护的端点意外放行（虽然 anyRequest().authenticated() 提供了兜底） |

### 🔵 INFO (注意项)

| # | 文件位置 | 问题 | 说明 |
|---|---------|------|------|
| I1 | `stores/auth.ts:40-41` | localStorage token 存储 | 已知 Phase1 过渡方案，Phase2 将迁移到 httpOnly cookie (注释 T12) |
| I2 | `AuthServiceImpl.java:77-81` | 医生岗位取第一个 | 当医生有多个岗位时只取 `iterator().next()`，不受控排序可能产生非预期行为 |
| I3 | `MenuServiceImpl.java:55` | 用户不存在时返回空列表 | 用户不存在时返回空菜单而不是 401，可能模糊权限问题 |
| I4 | `application.yml:5` | profiles: phase0,phase1,dev | phase0 和 phase1 同时激活，需确保两个 SecurityConfig 不冲突 (@Profile 隔离 + @Order？未使用 @Order 注解) |
| I5 | `JwtUtil.java:60` | 每次生成/解析都重新计算密钥 | 每次调用 `Keys.hmacShaKeyFor()`，性能可接受但可缓存 |
| I6 | `SecurityConfigPhase1.java:49` | Phase1 禁用 CSRF | 对于使用 Bearer token 的无状态 API 是合理选择 |
| I7 | `auth.ts:106-115` | refreshToken 在失败时清除认证数据 | 正确行为：refresh 失败 → 清除所有状态 → 用户需重新登录 |

---

## 集成不匹配项

| # | 严重度 | 描述 | 位置 |
|---|--------|------|------|
| M1 | 🟡 中 | **前端无自动 Token 刷新拦截器** — 后端返回 401 时，前端 axios 拦截器直接 reject(BusinessError)，不尝试调用 `/auth/refresh`。仅 `initializeAuth()` 中串联了 fetchCurrentUser → refreshToken 的重试逻辑。这意味着用户在正常使用过程中，如果 token 过期（例如 24h 后），下一次 API 调用将直接失败并清除所有状态，用户需要重新登录。 | `frontend/api/index.ts:42-44` vs `stores/auth.ts:149-161` |
| M2 | 🟢 低 | **用户不存在时返回空菜单而非 401** — `MenuServiceImpl.java:54` getUserMenuTree 在用户不存在时返回空数组而非抛出 BusinessException。这与 AuthController 其他端点的行为不一致（其他端点会抛出 UNAUTHORIZED）。 | `MenuServiceImpl.java:52-56` |
| M3 | 🟢 低 | **SecurityConfigPhase0 与 Phase1 并发激活** — `application.yml` 中 profiles 同时包含 `phase0` 和 `phase1`，两个配置类均定义 `SecurityFilterChain bean` 和 `PasswordEncoder bean`。虽然 `@Profile` 隔离理论上可行，但重复的 `PasswordEncoder bean` 可能导致冲突。 | `application.yml:5` + `SecurityConfigPhase1.java:84-87` + `SecurityConfigPhase0.java` |
| M4 | 🟢 低 | **401 响应体一致性** — JwtAuthenticationFilter 在 token 无效时仅跳过过滤器（不写 response），依赖 Spring Security 的 ExceptionTranslationFilter 或后续过滤器生成 401。而 AuthController 的 `/me` 和 `/refresh` 端点通过 `Result.fail("UNAUTHORIZED")` 手动返回 401。两者最终均产生 HTTP 401，但响应体结构不同（前者为 Spring Security 默认，后者为 `Result` 格式）。 | `JwtAuthenticationFilter.java:62-65` vs `AuthController.java:94-95` |

---

## 改进建议优先级排序

1. **🔴 P0 — 实现 Refresh Token 机制**：引入单独的 refresh token（长有效期，仅用于获取 access token），access token 改为短有效期（15-30分钟），refresh token 支持轮换（rotation，每次使用签发新 refresh token 并使旧 token 失效）。
2. **🔴 P0 — 服务端 Token 黑名单**：Phase2 引入 Redis，登出时将 token 加入黑名单直到过期，验证时检查黑名单。
3. **🟡 P1 — 前端全局 401 自动刷新**：在 axios 响应拦截器中添加 401 → silent refresh → retry original request 的中间件逻辑（带重试次数上限防死循环）。
4. **🟡 P1 — 登录接口速率限制**：使用 Spring Cloud Gateway / Redis 限流 / Bucket4j 对 `/api/auth/login` 实施 IP 级别速率限制。
5. **🟡 P2 — 密码复杂度策略**：增加 `@Pattern` 正则校验，要求密码包含大写字母、小写字母、数字、特殊字符中至少两种。
6. **🟡 P2 — Swagger 文档加上认证**：设置 `springdoc.api-docs.enabled` 在生产环境为 false，或增加安全约束。
7. **🟢 P3 — 排查 SecurityConfigPhase0 和 Phase1 的 bean 冲突**：检查 `SecurityConfigPhase0.java` 是否定义重复的 `PasswordEncoder` 或 `SecurityFilterChain` bean。
