根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 🔴 P1（严重）：角色优先级依赖不存在的字段
5.2 节 UserInfoResponse 字段映射说明（第 650 行）、7.3 节 UserType→role 行（第 841 行）声称"按角色优先级（Role 实体的 `level` 或 `sort` 字段）排序取最高者"来确定主角色。但经代码验证，`Role.java` 仅有 `code`、`name`、`description`、`enabled` 四个业务字段，**不存在 `level`、`sort`、`order`、`priority` 等可用于排序的字段**。这导致整个主角色判定策略无法落地。
- **改进建议**：方案 A：在 `Role` 实体中新增 `sort`（`Integer`）字段，DDL 同步变更；方案 B：改用角色 `code` 的字母序或 `id` 作为隐式排序；方案 C：若角色无优先级语义，则删除"主角色"概念，`role` 字段取关联角色列表中的第一个。**必须选定一种方案并更新角色判定策略说明。**

### 🟡 P2（重要）：`ACCOUNT_DISABLED` 定义了但从未被触发（逻辑矛盾）
10.2 节定义 `ACCOUNT_DISABLED`（code=`ACCOUNT_DISABLED`，message="账户已被管理员停用"），使用范围限定为"已认证请求中 JwtAuthenticationFilter 发现 enabled=false"。但 3.3 节 JwtAuthenticationFilter 行为契约步骤 4a 写明"用户不存在 / enabled=false / deleted=true → **清除 SecurityContext → chain.doFilter 放行**"。该 Filter 没有抛出任何异常或设置特定响应，只是跳过认证。后续请求到达受保护端点时，`ExceptionTranslationFilter` 会返回**通用的 `UNAUTHORIZED`**，而非 `ACCOUNT_DISABLED`。因此 `ACCOUNT_DISABLED` 在当前设计中**一定不可达**。
- **改进建议**：方案 A：在 JwtAuthenticationFilter 步骤 4a 中，当用户被禁用时抛出 `AuthenticationException`（含 `ACCOUNT_DISABLED` 错误码）；方案 B：删除 `ACCOUNT_DISABLED` 条目，禁用场景与无效 token 一样返回 `UNAUTHORIZED`。

### 🟡 P3（重要）：`PasswordChangeCheckFilter` 冗余查询用户（设计效率缺陷）
3.3 节 JwtAuthenticationFilter 行为契约（第 290 行查库加载用户）和 PasswordChangeCheckFilter 行为契约（第 306 行从 DB 检查 `passwordChangeRequired`）之间，每个已认证请求中两 Filter 连续执行两次 DB 查询加载同一用户，第二次查询完全冗余。
- **改进建议**：JwtAuthenticationFilter 加载用户后将 `passwordChangeRequired` 标记存入当前请求的 `ServletRequest` attribute，PasswordChangeCheckFilter 改为从 request attribute 读取。

### 🟡 P4（重要）：Token 刷新响应变更未声明 Breaking Change
6.4 节声明了登录响应的 Breaking Change，但 Token 刷新端点（`POST /api/auth/refresh`）的响应变更未在其中列出。当前后端刷新接口同时返回新 token 和用户信息，而设计文档的 `TokenRefreshResponse` 仅包含 `accessToken`、`refreshToken`、`tokenType`、`expiresIn`，不含用户信息。此外字段名从 `token` 变为 `accessToken`。
- **改进建议**：在 6.4 节新增刷新端点 Breaking Change 条目，列出字段变更（`token`→`accessToken`，移除 `user`）及前端适配说明（刷新后调用 `/api/auth/me`）。

### 🟡 P5（重要）：`MenuResponse` 被引用但从未定义
2.1 节目录结构和 7.2 节契约中引用了 `MenuResponse`，但 5.2 节 DTO 列表中**没有定义 `MenuResponse` record**。
- **改进建议**：在 5.2 节新增 `MenuResponse` record，至少包含 `Long id`、`String name`、`String path`、`String component`、`String icon`、`String permission`、`Integer sort`、`List<MenuResponse> children`，字段需与前端 `MenuItem` 接口对齐。

### 🟡 P6（重要）：全局 IP 频率限制缺少实现机制
4.1 节定义了"同一 IP（任意 API 路径）100 次/60 秒"的全局速率限制，但整个设计只描述了 `RateLimitGuard` 作用于登录端点。没有任何 Filter 或拦截器机制说明如何对**所有 API 路径**实施此限流。
- **改进建议**：方案 A：新增 `GlobalRateLimitFilter` 对所有非白名单路径实施 IP 级别限流；方案 B：明确推迟到 Phase 2 并从设计文档删除该行。

### 🟡 P7（重要）：登录流程步骤 5 未指定失败计数维度
3.1.1 节步骤 5 描述"`Optional` 为空 → 递增 LoginAttemptTracker 失败计数"，但用户名不存在时无法在**用户名维度**递增。LoginAttemptTracker 有用户名和 IP 两个维度，步骤 5 未明确递增哪个维度。
- **改进建议**：明确步骤 5 递增**IP 维度**的失败计数（因无有效用户名作为 key）；步骤 6（用户被禁用）应同时递增用户名和 IP 双维度计数。

### 🟡 P8（重要）：`MenuUpdateRequest.id` 字段与路径 `{id}` 关系不明确
5.2 节 `MenuUpdateRequest` 包含 `Long id` 字段，但端点 `PUT /api/menu/{id}` 已携带路径参数。未说明路径 `{id}` 与 DTO 中的 `id` 是否要求一致。
- **改进建议**：若 DTO 中不应包含 `id` 则删除之；若用于一致性校验则补充 `@NotNull` 并说明"路径 `{id}` 与请求体内的 `id` 必须一致，否则返回 400"。

### 🔵 P9（一般）：`PasswordChangeCheckFilter` 的 403 消息与 ErrorCode 不一致
PasswordChangeCheckFilter 返回 403 时携带消息"请先修改密码"，但 ErrorCode 表中 `FORBIDDEN` 的消息是"无权限访问"，`PASSWORD_CHANGE_REQUIRED` 的消息是"需要修改密码"。实现者不清楚应使用哪个 ErrorCode。
- **改进建议**：统一决策：若使用 `FORBIDDEN`（HTTP 403），则消息应匹配 ErrorCode 表的"无权限访问"；若要体现"请先修改密码"语义，可新增专用 ErrorCode。

### 🔵 P10（一般）：刷新端点未定义成功后前端获取用户信息的流程
`TokenRefreshResponse` 不包含用户信息，但设计文档未说明刷新成功后前端如何获取最新的用户信息。可能导致刷新后前端持有过时的用户角色/权限信息。
- **改进建议**：在 3.1.3 节步骤 8 之后补充说明：前端收到刷新响应后调用 `GET /api/auth/me` 更新本地用户信息。或在 7.4 节补充此流程。

## 历史迭代回顾

### 已解决的问题（出现在历史反馈但当前反馈中不再提及的问题）
- **第 1 轮**（14 个问题）：NOT NULL 约束状态误标（#1）、密码策略过渡方案缺失（#2）、PasswordChangeRequest DTO 缺失（#3）、RefreshTokenRequest 缺失（#4）、黑名单内存可行性（#5）、UserInfoResponse 字段名不兼容（#6）、LoginResponse Breaking Change 未声明（#7）、JwtAuthenticationFilter 迁移步骤缺失（#8）、8.3 节复用旧诊断报告（#9）、SidebarBase props 未定义（#10）、前后端重复条目（#11）、enabled 缺 `@Column(nullable=false)`（#12）、refresh 端点 SecurityConfig 矛盾（#13）、convertMenusToRoutes 递归化方案不完整（#14）——以上问题已在 v2~v4 迭代中逐一修复。
- **第 2 轮**（12 个问题）：deleted 列状态错误（#1）、Refresh Token 安全补偿逻辑错误（#2）、passwordChangeRequired 访问控制缺失（#3）、4.3 vs 5.1 修复状态矛盾（#4）、LoginResponse 缺 userId/username（#5）、菜单 CRUD DTO 缺失（#6）、登录错误消息差异化（#7）、8.3 vs 5.1 deleted 状态矛盾（#8）、7.5 重复条目未清理（#9）、多 Tab 刷新竞态（#10）、主角色策略未定义（#11）、SecurityConfig 依赖注入迁移（#12）——已在 v3/v4 中修复。
- **第 3 轮**（16 个问题）：旧 Refresh Token 不可重复使用的错误描述（#1）、passwordChangeRequired 未入实体变更表（#2）、LOGIN_FAILED ErrorCode 缺失（#3）、LoginResponse.user 可空性三处不一致（#4）、primaryRole 不存在的引用（#5）——其中 #5 虽被修复但引入了新的事实错误（见下方持续问题 P1）；其余问题已在 v4 中修复。

### 持续存在的问题（在多轮反馈中反复出现的问题，需重点解决）
- **P1（primaryRole→level/sort 字段不存在）**：第 3 轮 #5 发现引用不存在的 `primaryRole` 字段，第 4 轮将其改为引用 `level`/`sort` 字段，但未验证这两个字段在 Role 实体中实际是否存在，引入了新的事实错误。本轮需彻底解决角色优先级排序的实现基础。
- **P2（ACCOUNT_DISABLED 不可达）**：第 4 轮 #2 已报告，至今未修复。需在 Filter 行为契约与 ErrorCode 定义之间做一致性决策。
- **P3（PasswordChangeCheckFilter 冗余查询）**：第 4 轮 #3 已报告，至今未修复。需实现 request attribute 传递机制。
- **P4（刷新 Breaking Change 遗漏）**：第 4 轮 #4 已报告，至今未修复。
- **P5（MenuResponse 未定义）**：第 4 轮 #5 已报告，至今未修复。
- **P6（全局限流机制缺失）**：第 4 轮 #6 已报告，至今未修复。
- **P7（步骤 5 失败计数的维度不明确）**：第 4 轮 #7 已报告，至今未修复。
- **P8（MenuUpdateRequest.id 歧义）**：第 4 轮 #8 已报告，至今未修复。
- **P9（403 消息不一致）**：第 4 轮 #9 已报告，至今未修复。
- **P10（刷新后获取用户信息流程缺失）**：第 4 轮 #10 已报告，至今未修复。

以上 10 个问题已至少持续存在两轮，建议本轮一次性解决，否则不应进入编码阶段。

### 新发现的问题（本轮新识别的问题）

本轮无新识别的问题。10 个问题全部为第 4 轮已报告的持续问题。

## 上一轮产出路径
c:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606252256_phase1_ABD_ood\a_v4_design_v1.md

## 用户需求
c:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606252256_phase1_ABD_ood\requirement.md
