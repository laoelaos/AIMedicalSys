# 质量审查诊断报告 — v6

## 审查概述

- **审查轮次**：第 6 轮
- **审查范围**：需求响应充分度、事实准确性、逻辑一致性、深度与完整性、可落地性
- **审查方法**：逐节核对需求覆盖、交叉验证内部一致性、评估设计可编码性

## 发现问题

### 🔴 严重

#### 1. 3.2 节 Refresh Token Claims 示例缺少 `tokenVersion` 字段

**问题描述**：修订说明 v6 第 2 条已明确采用 `tokenVersion` 机制实现密码变更后旧 Refresh Token 即时失效，且 3.1.3 节步骤 7、4.2 节安全补偿策略、5.1 节 User.java 变更表均已同步新增 `tokenVersion` 字段。然而 3.2 节 Refresh Token Claims 结构示例未同步更新，仍为 v5 版本的无 `tokenVersion` 状态。这会导致实现者按照错误示例编码，遗漏 claims 中的版本号。

**所在位置**：3.2 节「Refresh Token」Claims 结构 JSON 示例

**严重程度**：严重

**改进建议**：在 Refresh Token Claims 示例中补充 `"tokenVersion": 0` 字段，并在下方设计要点中说明此字段的作用：密码变更后版本号递增，刷新时比对拒绝旧 token。

---

### 🟡 重要

#### 2. 3.1.3 节步骤 6 错误使用 `LOGIN_FAILED` 作为刷新失败错误码

**问题描述**：Token 刷新流程步骤 6 描述"若用户已被禁用或被删除，返回 LOGIN_FAILED 错误"。但 10.2 节已定义 `TOKEN_REFRESH_FAILED`（code="TOKEN_REFRESH_FAILED"，message="令牌刷新失败，请重新登录"）专用于刷新失败场景。`LOGIN_FAILED`（message="用户名或密码错误"）在语义上仅适用于登录流程（3.1.1），用于刷新端点是逻辑矛盾。

**所在位置**：3.1.3 节步骤 6

**严重程度**：重要

**改进建议**：将步骤 6 的 `LOGIN_FAILED` 改为 `TOKEN_REFRESH_FAILED`，确保错误码语义与使用场景一致。

---

#### 3. 3.3 节 AuthenticationEntryPoint 描述与 10.2 节 ACCOUNT_DISABLED 消息存在矛盾

**问题描述**：10.2 节定义 `ACCOUNT_DISABLED` 应返回消息"账户已被管理员停用"，10.1 节指定此场景 HTTP 状态为 401。但 3.3 节 SecurityConfig 的 authenticationEntryPoint 描述为"返回统一 Result.fail('UNAUTHORIZED')"，暗示固定响应内容。若 EntryPoint 不考虑异常中携带的 ErrorCode 而硬编码返回 `UNAUTHORIZED`，则 `ACCOUNT_DISABLED` 的定制消息无法送达前端，违反 10.2 节设计。文档未说明 EntryPoint 如何区分普通 401 和 ACCOUNT_DISABLED 场景。

**所在位置**：3.3 节「SecurityFilterChain」exceptionHandling 配置；10.2 节 ACCOUNT_DISABLED 行

**严重程度**：重要

**改进建议**：在 3.3 节补充 AuthenticationEntryPoint 的行为契约：当捕获到 AuthenticationException 携带 ErrorCode.ACCOUNT_DISABLED 时，返回 `Result.fail("ACCOUNT_DISABLED", "账户已被管理员停用")`；其余情况返回 `Result.fail("UNAUTHORIZED", "未认证或令牌已失效")`。

---

#### 4. 4.3 节与 5.1 节对 nickname NOT NULL 变更的描述不一致

**问题描述**：4.3 节「修复方案」列表要求"`sys_user.nickname` 由 `DEFAULT NULL` 改为 `NOT NULL`"，并声称此项变更"对齐 User.java `@Column(nullable=false)`"。经验证代码，`User.nickname` 确实已带有 `@Column(nullable=false)` 注解，schema 变更合理。但 5.1 节 User.java 实体变更表中未列出此条目，导致实现者参照 5.1 节编码时会遗漏此 schema 变更。且 4.3 与 5.1 对同一变更的描述位置分离，增加了维护风险。

**所在位置**：4.3 节「修复方案」列表 vs 5.1 节 User.java 变更表

**严重程度**：重要

**改进建议**：在 5.1 节 User.java 变更表中新增 `nickname NOT NULL` 行，标注"当前状态：代码已有 `@Column(nullable=false)` | 修复方案：schema.sql 补加 `NOT NULL DEFAULT ''`（或确认现有 NULL 数据处理策略）"。同步在 4.3 节的 nickname 条目中注明"详见 5.1 节 User.java 变更表"。

---

#### 5. 8.2 节 H5 展平路由策略缺少 Layout 包容机制说明

**问题描述**：H5 修复方案描述了对菜单树的递归展平和以 `router.addRoute()` 注册一级路由的策略，并明确说明"不带父级 Layout 嵌套"。但未说明在展平路由作为顶级路由注册后，Layout 组件（SidebarBase + 顶栏）如何包裹内容区域。若每个菜单页面的路由组件不内嵌 Layout，则导航到 `/system/user` 等路径时将缺少侧边栏和顶栏，导致管理端页面布局破损。文档仅提到 path === '/' 的根路由跳转到 Layout，但展开路由如何与 Layout 协同工作未定义。

**所在位置**：8.2 节 H5「修复方案」列

**严重程度**：重要

**改进建议**：补充 Layout 包容机制的设计决策：(a) 若 Layout 在 App.vue 中包裹 `<router-view>`，则展平路由自动在 Layout 内渲染，需显式说明此架构前提；(b) 若 Layout 作为路由的一部分，则说明展平路由作为 Layout 子路由的注册方式（如 `router.addRoute('Layout', childRoute)`）。选定方案后更新 H5 修复方案。

---

### 🔵 一般

#### 6. 5.1 节 `User.tokenVersion` 缺少 `@Column(nullable=false)` 注解

**问题描述**：5.1 节 User.java 变更表显示 Java 字段 `private Integer tokenVersion = 0;` 无 JPA 注解，但 DDL 为 `INT NOT NULL DEFAULT 0`。`Integer` 类型在未设置 `nullable=false` 时，JPA 视为可为空，若业务代码误置 null 或实体未正确初始化，JPA 不会触发数据库约束验证前拦截，可能在 Flush 阶段才暴露错误。

**所在位置**：5.1 节 User.java 变更表「tokenVersion 新增字段」行

**严重程度**：一般

**改进建议**：补加 `@Column(nullable=false)` 注解，同步到 5.1 节变更表的"修复方案"列。

---

#### 7. 密码变更 API 流程缺少独立完整描述

**问题描述**：密码变更 API（`PUT /api/auth/password`）的业务流程分散在 3.1.1 步骤 9（PasswordChangeService 检查）、3.4 节（策略描述与访问控制）、7.4 节（前端恢复流程）等多处，无类似 3.1.1-3.1.5 的独立流程描述。实现者需要跨节拼接：服务端收到请求后的校验顺序（旧密码验证 → 新密码策略校验 → BCrypt 加密 → 保存 → tokenVersion 递增 → 清除 passwordChangeRequired），错误场景处理（旧密码不匹配、新密码复杂度不满足），以及响应格式。虽信息存在但散落，降低了可直接编码性。

**所在位置**：3.4 节「密码变更强制策略」、7.4 节

**严重程度**：一般

**改进建议**：建议新增 3.1.6 节「密码变更流程」，将服务端处理逻辑整理为结构化步骤描述（含旧密码验证、复杂度校验、tokenVersion 递增、passwordChangeRequired 清除、前端恢复流程），并引用 6.1 节对应端点、5.2 节 PasswordChangeRequest、10.2 节 PASSWORD_MISMATCH 等 ErrorCode。

---

#### 8. 10.1 节错误分类表未覆盖全部 ErrorCode 的 HTTP 状态映射

**问题描述**：10.1 节错误分类表为 7 类错误分配了 HTTP 状态码，但 10.2 节 ErrorCode 表中的 `ACCOUNT_LOCKED`、`TOKEN_REFRESH_FAILED`、`PASSWORD_TOO_WEAK`、`PASSWORD_MISMATCH` 等均未被 10.1 覆盖。例如 `ACCOUNT_LOCKED` 应按业务错误（HTTP 200 + ErrorCode）还是按 429/403 处理未指定；`TOKEN_REFRESH_FAILED` 的 HTTP 状态也未分配。

**所在位置**：10.1 节「错误分类」表；10.2 节各未覆盖 ErrorCode

**严重程度**：一般

**改进建议**：在 10.1 节补充缺失 ErrorCode 的分类映射，或为各 ErrorCode 增加 HTTP 状态注释。

---

## 整体评价

v6 相比 v5 显著改进了时序侧信道防御（dummy BCrypt）、密码变更后旧 Refresh Token 撤销（tokenVersion 机制）、前端 PASSWORD_CHANGE_REQUIRED 处理和菜单路由注册策略明确化等关键问题。文档在结构完整性、需求覆盖度和安全设计深度上已达到较高水平。

以上 8 个问题中，问题 1（tokenVersion claims 示例遗漏）和问题 2（错误码逻辑矛盾）为必须修复项，直接影响编码准确性；问题 3-5 需要在设计层面补充决策和描述，否则可能导致下游实现偏离设计意图；问题 6-8 为次要项，建议在编辑时一并进行。
