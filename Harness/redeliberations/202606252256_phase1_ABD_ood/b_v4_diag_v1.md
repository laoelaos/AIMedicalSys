# 质量审查报告 — v4 设计文档

审查对象：a_v4_design_v1.md
审查轮次：第 4 次
审查维度：需求响应充分度、事实准确性、逻辑一致性、落地可执行性

---

## 问题清单

### 🔴 P1（严重）：角色优先级依赖不存在的字段

- **位置**：5.2 节 UserInfoResponse 字段映射说明（第 650 行）、7.3 节 UserType→role 行（第 841 行）
- **问题描述**：设计文档声称"按角色优先级（Role 实体的 `level` 或 `sort` 字段）排序取最高者"来确定主角色。但经代码验证，`Role.java`（`common-module-impl/permission/Role.java`）仅有 `code`、`name`、`description`、`enabled` 四个业务字段，**不存在 `level`、`sort`、`order`、`priority` 等可用于排序的字段**。这导致整个主角色判定策略无法落地。
- **历史背景**：第 3 轮审查（Issue 5）发现原设计引用不存在的 `primaryRole` 字段，修复时将引用替换为 `level`/`sort`，但未验证这两个字段在实体中实际是否存在，引入了新的事实错误。
- **改进建议**：
  - 方案 A：在 `Role` 实体中新增 `sort`（`Integer`）字段，DDL 同步变更，填入种子数据排序值。
  - 方案 B：若无显式排序需求，改用角色 `code` 的字母序或 `id` 作为隐式排序（需文档注明按创建时间/字母序）。
  - 方案 C：若角色无优先级语义，则删除"主角色"概念，`role` 字段取关联角色列表中的第一个（需文档注明场景和局限性）。
  - **必须在设计文档中选定一种方案并更新角色判定策略说明。**

---

### 🟡 P2（重要）：`ACCOUNT_DISABLED` 定义了但从未被触发（逻辑矛盾）

- **位置**：10.2 节 ErrorCode 表 `ACCOUNT_DISABLED` 行 vs 3.3 节 JwtAuthenticationFilter 行为契约
- **问题描述**：10.2 节定义 `ACCOUNT_DISABLED`（code=`ACCOUNT_DISABLED`, message="账户已被管理员停用"），使用范围限定为"已认证请求中 JwtAuthenticationFilter 发现 enabled=false"。但 3.3 节 JwtAuthenticationFilter 行为契约步骤 4a 写明"用户不存在 / enabled=false / deleted=true → **清除 SecurityContext → chain.doFilter 放行**"。该 Filter 没有抛出任何异常或设置特定响应，只是跳过认证。后续请求到达受保护端点时，`ExceptionTranslationFilter` 会返回**通用的 `UNAUTHORIZED`**，而非 `ACCOUNT_DISABLED`。因此 `ACCOUNT_DISABLED` 在当前设计中**一定会被不可达**。
- **改进建议**：二选一：
  - 方案 A：在 JwtAuthenticationFilter 步骤 4a 中，当用户被禁用时抛出 `AuthenticationException`（含 `ACCOUNT_DISABLED` 错误码），由 `AuthenticationEntryPoint` 统一处理。注意此时不应泄露用户存在性信息（与登录流程的 `LOGIN_FAILED` 区分——登录时禁用用户仍返回"用户名或密码错误"）。
  - 方案 B：删除 `ACCOUNT_DISABLED` 条目，禁用场景与无效 token 一样返回 `UNAUTHORIZED`。同步更新 10.2 节的使用边界说明。

---

### 🟡 P3（重要）：`PasswordChangeCheckFilter` 冗余查询用户（设计效率缺陷）

- **位置**：3.3 节 JwtAuthenticationFilter 行为契约（第 290 行查库加载用户）和 PasswordChangeCheckFilter 行为契约（第 306 行从 DB 检查 `passwordChangeRequired`）
- **问题描述**：每个已认证请求的 Filter 链中，JwtAuthenticationFilter 已经从 DB 加载了完整的 `User` 实体（步骤 4），紧接着 PasswordChangeCheckFilter 又执行一次 DB 查询来读取 `passwordChangeRequired` 状态。两 Filter 在同一请求中连续执行，第二次查询完全冗余。使"每次请求一次 DB 查询"的实际代价翻倍。
- **改进建议**：JwtAuthenticationFilter 加载用户后将 `passwordChangeRequired` 标记存入当前请求的 `ServletRequest` attribute（`request.setAttribute("passwordChangeRequired", user.isPasswordChangeRequired())`），PasswordChangeCheckFilter 改为从 request attribute 读取。无需额外 DB 查询。

---

### 🟡 P4（重要）：Token 刷新响应变更未声明 Breaking Change

- **位置**：6.4 节 Breaking Change 声明表
- **问题描述**：设计文档在 6.4 中声明了登录响应的 Breaking Change，但 Token 刷新端点（`POST /api/auth/refresh`）的响应变更未在声明中列出。经代码验证，当前前端 `auth.ts:112-113` 显示刷新后同时保存 `response.token` 和 `response.user`，这意味着当前后端刷新接口同时返回新 token 和用户信息。而设计文档的 `TokenRefreshResponse`（5.2 节）仅包含 `accessToken`、`refreshToken`、`tokenType`、`expiresIn`，不含用户信息。此外字段名从 `token` 变为 `accessToken`。这两项变更未在 Breaking Change 声明中列出，前端团队无法感知。
- **改进建议**：在 6.4 节新增以下条目：

  | 变更项 | Phase 0 | Phase 1 | 说明 |
  |--------|---------|---------|------|
  | `/api/auth/refresh` 响应字段 | `token` / `user` | `accessToken` / `refreshToken`（不含 `user`） | 刷新后不再返回用户信息，前端需在刷新成功后调用 `/api/auth/me` 重新获取用户信息 |
  | 刷新响应字段名 | `token` | `accessToken` | 对齐登录响应字段名规范 |

---

### 🟡 P5（重要）：`MenuResponse` 被引用但从未定义

- **位置**：2.1 节目录结构（`menu/dto/MenuResponse.java`）、7.2 节包 B→包 D 契约（`List<MenuResponse>`）
- **问题描述**：设计文档在目录结构中列出了 `MenuResponse.java`，并在 7.2 节契约中引用了 `List<MenuResponse>`（递归 children），但 5.2 节 DTO 列表中**没有定义 `MenuResponse` record**。实现者不知道该 DTO 包含哪些字段及其类型。
- **改进建议**：在 5.2 节新增 `MenuResponse` record 定义，至少包含：`Long id`、`String name`、`String path`、`String component`、`String icon`、`String permission`、`Integer sort`、`List<MenuResponse> children`。字段定义需与前端 `MenuItem` 接口（`frontend/packages/shared/src/types/index.ts:45-53`）对齐。

---

### 🟡 P6（重要）：全局 IP 频率限制缺少实现机制

- **位置**：4.1 节速率限制表第二行（同一 IP 任意 API 路径 100 次/60 秒）
- **问题描述**：设计文档定义了"同一 IP（任意 API 路径）100 次/60 秒"的全局速率限制，但整个设计只描述了 `RateLimitGuard` 作用于登录端点（3.1.1 步骤 2）。没有任何 Filter 或拦截器机制说明如何对**所有 API 路径**实施此限流。
- **改进建议**：二选一：
  - 方案 A：新增 `GlobalRateLimitFilter`（OncePerRequestFilter），对所有非白名单路径实施 IP 级别限流，在 SecurityFilterChain 中注册在 JwtAuthenticationFilter 之前。
  - 方案 B：明确此全局限流推迟到 Phase 2 实现，从 Phase 1 设计文档中删除该行，避免误导。

---

### 🟡 P7（重要）：登录流程步骤 5 未指定失败计数维度

- **位置**：3.1.1 节步骤 5
- **问题描述**：步骤 5 描述"`Optional` 为空 → 递增 LoginAttemptTracker 失败计数"，但用户名不存在时无法在**用户名维度**递增。LoginAttemptTracker 有用户名和 IP 两个维度，步骤 5 只说了"递增"但不明确递增哪个维度。这会导致实现中的理解分歧。
- **改进建议**：明确步骤 5 递增的是 **IP 维度**的失败计数（因为此时没有有效的用户名作为 key）。同时，步骤 6（用户被禁用）也应说明同时递增用户名和 IP 两个维度的计数。

---

### 🟡 P8（重要）：`MenuUpdateRequest.id` 字段与路径 `{id}` 关系不明确

- **位置**：5.2 节 MenuUpdateRequest record、6.1 节接口清单 `PUT /api/menu/{id}`
- **问题描述**：`MenuUpdateRequest` 包含 `Long id` 字段，但端点路径已携带 `{id}`。没有文档说明是否要求路径 `{id}` 与 DTO 中的 `id` 必须一致，或者 DTO 中的 `id` 是否作为补充校验。如果 `id` 是冗余字段应删除；如果是校验用途应加 `@NotNull` 并说明校验规则。
- **改进建议**：在 6.1 节接口清单的 MenuUpdateRequest 行添加脚注，明确 `id` 字段的使用方式：
  - 若 DTO 中不应包含 `id`，则删除之，仅使用路径参数。
  - 若 DTO 中包含 `id` 用于一致性校验，则补充 `@NotNull` 并说明"路径 `{id}` 与请求体内的 `id` 必须一致，否则返回 400"。

---

### 🔵 P9（一般）：`PasswordChangeCheckFilter` 的 403 消息与 ErrorCode 不一致

- **位置**：3.3 节 PasswordChangeCheckFilter 行为契约（第 312 行）vs 10.2 节 ErrorCode 表
- **问题描述**：PasswordChangeCheckFilter 返回 403 时携带的消息是 "请先修改密码"，但 ErrorCode 表中 `FORBIDDEN` 的消息是 "无权限访问"，`PASSWORD_CHANGE_REQUIRED` 的消息是 "需要修改密码"。三个消息各不相同。实现者不清楚应使用哪个 ErrorCode。
- **改进建议**：统一决策：若使用 `FORBIDDEN`（HTTP 403），则消息应匹配 ErrorCode 表的 "无权限访问"；若要体现"请先修改密码"的语义，可考虑新增专用 ErrorCode（如 `PASSWORD_CHANGE_BLOCKED`）。同步更新 10.2 节。

---

### 🔵 P10（一般）：刷新端点未定义成功后前端获取用户信息的流程

- **位置**：3.1.3 节 Token 刷新流程、7.4 节前端补偿机制
- **问题描述**：P4 指出 `TokenRefreshResponse` 不包含用户信息。但设计文档未说明刷新成功后前端如何获取最新的用户信息（如 roles/permissions 更新）。7.4 节提到"用户信息不在前端持久化权限决策"，但刷新后的用户身份替换流程缺失。可能导致刷新后前端持有过时的用户角色/权限信息。
- **改进建议**：在 3.1.3 节步骤 8 之后补充说明：前端收到刷新响应后，应调用 `GET /api/auth/me` 更新本地用户信息。或在 7.4 节补充此流程。

---

## 整体评价

文档在经历了 3 轮迭代修改后，质量有显著提升：Breaking Change 声明已补充、登录流程安全性已加强、密码策略过渡方案已落地。但仍存在 **1 处事实错误**（P1：角色排序字段不存在）、**1 处逻辑矛盾**（P2：ACCOUNT_DISABLED 不可达）、以及多处可落地性缺陷（P3 冗余查询、P4 未声明 Breaking Change、P5-P6 设计不完整）。其中 P1 最为严重——它直接导致主角色判定策略在代码中无法实现，需要在实体层做变更决策后才能推进。

建议优先处理 🔴 和 🟡 问题后进入编码阶段，🔵 问题可在编码中同步修正。
