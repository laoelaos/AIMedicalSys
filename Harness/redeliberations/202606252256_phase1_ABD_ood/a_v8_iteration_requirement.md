根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 问题 1（严重）：异常刷新检测机制缺少可落地的实现细节
4.2 节「Refresh Token 的安全补偿策略」第三项声称"若检测到同一用户短时间内出现多次刷新"，但以下关键要素全部缺失：未定义具体时间窗口（1秒？5秒？）；未定义阈值（2次？5次？）；未指定检测逻辑放置位置（AOP？AuthServiceImpl.refresh()？独立Filter？）；无对应类/接口定义及架构组件归属；未说明告警输出方式（仅log.warn？是否需对接监控系统？）。**改进建议**：补充完整设计契约：(a)定义时间窗口和阈值；(b)指定检测逻辑实现位置；(c)明确告警方式；或将此机制推迟到Phase 2并从当前补偿策略列表中移除。

### 问题 2（严重）：H6 修复方案推荐的 `inject('router')` 在 Pinia store 中不可用
8.2 节 H6「修复方案」列推荐在 Pinia store 中使用 `inject('router')` 依赖注入模式来打破循环依赖。但 `inject()` 仅在 Vue 组件 `setup()` 或 `<script setup>` 上下文中可用，在 Pinia store 的 action/method 中调用会返回 `undefined`，此方案无法在编码阶段落地。**改进建议**：保留现有 `createMenuStore(router, dynamicPageComponent)` 工厂模式（已在 `shared/src/stores/menu.ts` 中实现），app 专用 store 层通过延迟初始化解决了循环依赖问题（见 `apps/doctor/src/stores/menu.ts` 和 `apps/admin/src/stores/menu.ts`），修复方案应引用现有实现而非推荐不可行的 `inject('router')` 方案。

### 问题 3（重要）：管理员标记密码过期的 API 端点缺失
3.4 节描述"系统管理员通过管理端对特定用户标记密码过期"作为两种密码变更触发场景之一，但 6.1 节接口清单中没有任何管理员端 API 端点用于设置用户的 `passwordChangeRequired = true`，此功能无实现路径。**改进建议**：二选一：(a)在 6.1 节补充管理员专用端点（如 `PUT /api/users/{id}/password-expire`）并说明权限要求（ADMIN 角色）；(b)若此功能属于管理端模块（包 D 的管理后台）或 Phase 2 范围，在 3.4 节场景 2 中明确标注"此功能属于管理端设计范围，本设计不做具体接口定义"。

### 问题 4（一般）：ProfileUpdateRequest.nickname 缺少空白字符串校验
5.2 节 ProfileUpdateRequest record 定义中 `nickname` 字段仅有 `@Size(max = 50)` 但缺少 `@NotBlank`，空字符串（`""`）能通过校验（长度为 0 ≤ 50），导致用户可将昵称设为空字符串。现有后端代码（`ProfileUpdateRequest.java`）也存在同样缺失。**改进建议**：补充 `@NotBlank(message = "昵称不能为空")` 注解。

### 问题 5（一般）：MenuUpdateRequest 的 PUT 语义不明确（全量替换 vs 部分更新）
5.2 节 MenuUpdateRequest 定义中除 `id` 外所有字段均为可选类型（无 `@NotNull`），暗示 PATCH 语义的局部更新，但接口方法为 `PUT /api/menu/{id}`，PUT 通常语义为全量替换。**改进建议**：明确声明更新语义：若采用局部更新（推荐），标注为 PATCH 或补充说明"省略的字段不更新"；若坚持全量替换，为所有字段补充 Java doc 说明空值处理策略。

### 问题 6（一般）：登出端点在 token 过期场景下的行为未定义
3.1.4 节登出流程中 `/api/auth/logout` 要求 `authenticated`，意味着必须携带有效 Access Token。用户在 token 过期后无法调用登出后端接口。文档未说明在 token 过期或无效场景下前端的登出策略——是"尽力而为"（后端失败仍在前端 finally 块清除本地数据），还是需设计无 token 登出机制。**改进建议**：在 3.1.4 节补充说明后端登出错时（token 过期/无效导致 401），前端 finally 块仍需清除本地 token 和用户数据，Token 黑名单的登出记录是"尽力而为"的最优努力，不应阻止本地登出。

### 问题 7（一般）：LoginAttemptCleaner 类定义但无行为说明
2.1 节目录结构中包含 `LoginAttemptCleaner.java` 条目，但全文没有任何地方描述此类的行为、调度策略、与 `LoginAttemptTracker` 的惰性清理之间的配合关系。4.1 节仅描述"超过锁定时间后惰性清除"，未说明 `LoginAttemptCleaner` 是否存在、是否必要。**改进建议**：二选一：(a)在 9.3 节或 4.1 节补充 `LoginAttemptCleaner` 的调度机制说明（如 @Scheduled 定时清理超期记录，清理周期等）；(b)若惰性清理已足够，从目录结构中删除 `LoginAttemptCleaner.java` 条目以消除混淆。

## 历史迭代回顾

### 已解决的问题
以下问题在之前的多轮迭代中被识别并已在上一轮产出（v7）中修复，当前审查中不再提及：
- **迭代 1-2**：代码状态描述与实际的矛盾（enabled NOT NULL、deleted 列等）、PasswordChangeRequest DTO 缺失、RefreshTokenRequest 请求体定义缺失、内存黑名单 12GB 可行性论证、UserInfoResponse 字段对齐、LoginResponse 字段命名、JwtAuthenticationFilter 迁移步骤、8.3 节复用旧诊断报告、SidebarBase props 定义、前端用户信息存储重复
- **迭代 2-3**：Phase 1 Refresh Token 轮换错误逻辑、passwordChangeRequired 访问控制缺失、4.3 vs 5.1 内部矛盾、LoginResponse 缺少 userId/username、Menu CRUD DTO 缺失、登录错误消息差异化导致用户名可枚举、SecurityConfigPhase1 依赖注入旧 Filter
- **迭代 3**：Refresh Token 声称旧 token 不可重复使用的错误、passwordChangeRequired 字段未出现在实体变更表、登录流程 ErrorCode/ACCOUNT_DISABLED 消息冲突、LoginResponse.user 可空性三方不一致、7.3 节 primaryRole 不存在的字段引用、9.2 限流方案未做决策、CORS 生产安全、JWT_SECRET 约束、passwordChangeRequired 检查违反单一职责（已抽离为独立 Filter）、登出请求体不一致、H6 循环依赖修复模糊（部分修复，详见下方持续存在的问题第 2 项）
- **迭代 4**：角色优先级依赖不存在的字段（已新增 sort 字段）、ACCOUNT_DISABLED 未触发（已在 Filter 中抛出）、PasswordChangeCheckFilter 冗余查询（已通过 request attribute 优化）、MenuResponse 未定义（已补充）、GlobalRateLimitFilter 缺失（已补充）、Token 刷新响应 Breaking Change、MenuUpdateRequest.id 一致性校验、刷新后获取用户信息流程
- **迭代 5**：步骤 5 时序攻击（已增加 dummy BCrypt）、tokenVersion 撤销机制（已新增字段+claims）、密码变更前端恢复流程、菜单展平路由冲突、PASSWORD_CHANGE_REQUIRED 前端处理、步骤 7 失败计数维度、JwtTokenProvider @PostConstruct 启动验证
- **迭代 6**：Refresh Token claims 缺少 tokenVersion 示例、8.1 缺陷追踪表遗漏条目、刷新步骤 6 LOGIN_FAILED 错误码、AuthenticationEntryPoint 行为契约、nickname NOT NULL 跨节描述不一致、H5 Layout 包容机制、步骤 6 时序缺口、LoginResponse.expiresIn 定位

### 持续存在的问题（本轮仍出现，需重点解决）
以下问题在上轮（v7）诊断中再次被检出，表明上一轮修复不充分或未修复：
1. **异常刷新检测机制缺少实现细节**（迭代 7 问题 1，本轮问题 1 — 严重）：该问题已连续两轮出现，文档声称"检测到异常刷新"但始终未定义时间窗口、阈值、实现位置、类定义、告警方式。需在本轮彻底补充或明确推迟到 Phase 2。
2. **H6 修复方案的 `inject('router')` 不可行**（迭代 7 问题 2，本轮问题 2 — 严重）：上一轮虽已对 H6 循环依赖进行修复，但修复方案仍引用了在 Pinia store 中不可用的 `inject('router')`。需引用已存在的 `createMenuStore(router, ...)` 工厂模式。
3. **管理员标记密码过期 API 缺失**（迭代 7 问题 3，本轮问题 3 — 重要）：3.4 节描述了该功能但 6.1 节无对应端点，需二选一明确处理。
4. **ProfileUpdateRequest.nickname 缺少 @NotBlank**（迭代 7 问题 4，本轮问题 4 — 一般）：简单注解缺失，可在本轮快速修复。
5. **MenuUpdateRequest PUT 语义不明确**（迭代 7 问题 5，本轮问题 5 — 一般）：需明确声明更新语义（全量替换 vs 局部更新）。
6. **登出端点 token 过期行为未定义**（迭代 7 问题 6，本轮问题 6 — 一般）：需补充前端 finally 块清理策略。
7. **LoginAttemptCleaner 无行为说明**（迭代 7 问题 7，本轮问题 7 — 一般）：需补充调度说明或从目录结构删除。

### 新发现的问题
无。本轮全部 7 个问题均与迭代 7 历史反馈中的问题一一对应，无新增问题。

## 上一轮产出路径
c:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606252256_phase1_ABD_ood/a_v7_design_v2.md

## 用户需求
c:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606252256_phase1_ABD_ood/requirement.md
