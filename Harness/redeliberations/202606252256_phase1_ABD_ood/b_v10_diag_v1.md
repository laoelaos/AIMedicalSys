# 质量审查报告 — Phase 1 包 A/B/D 统一修复与包 B OOD 设计方案（v10）

## 审查范围

本报告是对产出文件 `a_v10_copy_from_v9.md` 的质量审查。审查视角侧重内部审议（10轮迭代修订）未充分覆盖的维度：
需求响应充分度、事实错误与逻辑矛盾、深度与完整性。已通过查阅代码确认多项事实性陈述的准确性。

---

## 发现的问题

### 🔴 问题 1（严重）：速率限制表头与 GlobalRateLimitFilter 行为矛盾

**所在位置**：4.1 节「第一层：IP 级速率限制（RateLimitGuard）」速率限制表，以及紧随其后的 GlobalRateLimitFilter 描述

**问题描述**：
速率限制表（4.1 节第二行）声称限流维度为「同一 IP（任意 API 路径）」阈值 100 次/60 秒。但同一节中 GlobalRateLimitFilter 的描述明确将 `/api/auth/login` 和 `/api/auth/refresh` 列入白名单排除——即 refresh 和 login 端点不受此全局限流覆盖。

更深层的问题：login 端点有其独立的 InMemoryRateLimitGuard 限流（5 次/10 秒），但 `/api/auth/refresh` 端点既不受全局限流覆盖，也无独立的限流策略，且在 SecurityConfig 中为 `permitAll`（无需 JWT 认证）。

**严重性评估**：
1. 表头「任意 API 路径」与 Filter 白名单行为直接矛盾，属于文档内部事实不一致，编码实现人员无法判断哪一个是正确的设计意图
2. refresh 端点在 Phase 1（旧 Refresh Token 未入黑名单、可重复使用）的背景下完全无速率限制，持有有效 Refresh Token 的攻击者可无限次调用。4.2 节的异常刷新检测仅输出 `log.warn` 安全日志，不具备自动阻断能力

**改进建议**：
- 将速率限制表第二行维度修正为「同一 IP（除 login/refresh 外的一般 API 路径）」，消除文档内部矛盾
- 为 `/api/auth/refresh` 增加独立的限流策略（建议：同一 IP 30 次/60 秒，或同一 userId 5 次/10 秒），或在设计决策中明确承认此风险并在表的「限流」列标注「无（见 4.2 节异常刷新检测）」

---

### 🔴 问题 2（严重）：POST /api/auth/logout 的可选请求体与 DTO 约束不兼容，Controller 层行为悬空

**所在位置**：3.1.4 节步骤 1（可选携带 refreshToken）、4.4 节保护清单备注列、5.2 节 RefreshTokenRequest 定义、6.1 节接口清单

**问题描述**：
三处文档一致描述登出端点的请求体为可选（「请求体可选携带 refreshToken 字段」「RefreshTokenRequest（可选）」）。但 5.2 节定义的 `RefreshTokenRequest` record 为：

```java
public record RefreshTokenRequest(@NotBlank String refreshToken) {}
```

`@NotBlank` 要求 `refreshToken` 字段非空。若客户端不发送请求体（或发送 `{}`），Spring MVC 在反序列化 `@Valid` 参数时会触发 `MethodArgumentNotValidException`。

文档未指定 Controller 层的对应处理方式：
- 是用 `@RequestBody(required=false) RefreshTokenRequest request` 接受 null？
- 还是分两个端点（一个带 body，一个不带）？
- 或者将 `refreshToken` 改为 `@Nullable`？

**改进建议**：在 5.2 节 RefreshTokenRequest 定义处或 3.1.4 节补充 Controller 层签名说明。推荐方案：(a) 使用 `@RequestBody(required=false)` 并判断 null，或 (b) 将 refreshToken 设计为独立 header（如 `X-Refresh-Token`），消除请求体可选性带来的复杂性。

---

### 🟡 问题 3（重要）：PUT /api/menu/{id} 的局部更新语义违反 HTTP 规范

**所在位置**：5.2 节 MenuUpdateRequest 定义后的「更新语义」说明

**问题描述**：
文档声明 MenuUpdateRequest 采用「局部更新语义，请求体中省略的字段保持不变，不覆盖为空值」，但端点方法为 `PUT /api/menu/{id}`。RFC 7231 §4.3.4 将 PUT 定义为全量替换（the target resource SHOULD be replaced by the representation enclosed in the request）。使用 PUT 进行局部更新构成 HTTP 语义违规。

迭代第 7 轮审查已发现此问题（问题 5：「MenuUpdateRequest 的 PUT 语义不明确」），修订说明（v9）选择「采用局部更新（PATCH 语义），省略的字段保持不变」，但端点方法仍保留为 PUT。

这一决策对下游消费者的影响：
- 前端开发者需额外适配这一非标准行为（其他 PUT API 为全量替换时，菜单 PUT 却为部分更新）
- 自动生成的 API 文档（如 Swagger）不会标注语义差异

**改进建议**：
- **推荐**：将端点改为 `PATCH /api/menu/{id}`，与 HTTP 规范对齐
- **备选**：若坚持使用 PUT，在 6.1 节接口清单和 5.2 节明确标注「此为局部更新 PUT，非全量替换」，并在 Breaking Change 声明表（6.4 节）中补充说明

---

### 🟡 问题 4（重要）：PasswordChangeCheckFilter 返回 403 的实现机制未定义，异常处理链路断裂

**所在位置**：3.3 节 PasswordChangeCheckFilter 行为契约、10.1 节错误分类表、10.2 节 ErrorCode 表

**问题描述**：
PasswordChangeCheckFilter 的行为契约描述为「清除 SecurityContext，返回 403 + ErrorCode.PASSWORD_CHANGE_REQUIRED」，但未指定具体的实现机制：

1. 若直接通过 `HttpServletResponse` 写 JSON 响应（setStatus + getWriter），则绕过了 `GlobalExceptionHandler`，与 6.3 节「由 GlobalExceptionHandler 和 AuthenticationEntryPoint 共同保证响应结构一致」的声明矛盾
2. 若抛出自定义异常，文档未定义对应的异常类和异常处理路径（10.1 节错误分类表中 PASSWORD_CHANGE_REQUIRED 没有对应异常类型和 HTTP 状态码）
3. 无论是哪种方式，`@ResponseBody` / 统一响应体格式的保证机制均未说明

对比其他端点（如 `ACCOUNT_DISABLED` 通过 `AuthenticationException` → `AuthenticationEntryPoint` 处理，`RATE_LIMITED` 通过 `RateLimitExceededException` 处理），PasswordChangeCheckFilter 的异常处理路径是一个设计空白。

**改进建议**：
- 在 10.1 节为 PASSWORD_CHANGE_REQUIRED 补充异常类型和 HTTP 状态码映射
- 定义 `PasswordChangeRequiredException` 或复用 `BusinessException`，通过 `AccessDeniedHandler` 或 `GlobalExceptionHandler` 统一处理
- 在 3.3 节行为契约中明确说明异常处理路径

---

### 🟡 问题 5（重要）：MenuUpdateRequest.id 的 @NotNull 与局部更新语义冲突

**所在位置**：5.2 节 MenuUpdateRequest record 定义及一致性校验说明

**问题描述**：
MenuUpdateRequest 声明局部更新语义——省略的字段不覆盖为空值。但同时声明 `id` 字段标注 `@NotNull` 并用于与路径参数 `{id}` 的一致性校验：

- 若客户端遵循局部更新模式省略 `id`（仅通过路径参数传递），则 `@NotNull` 校验失败
- 若客户端必须传入 `id`，则又违背了「省略的字段不更新」的局部更新语义——`id` 成了唯一强制字段，语义不纯粹
- 一致性校验的触发时机（在 `@Valid` 之后还是 Controller 方法体内）未说明

**改进建议**：
- **推荐**：从 MenuUpdateRequest 中移除 `id` 字段，Controller 直接使用路径参数 `{id}`，消除校验歧义
- **备选**：将 `id` 改为 `@Null`（不在请求体中携带），仅在 Controller 方法体内显式比对路径参数与请求体中的任何标识

---

### 🟢 问题 6（一般）：速率限制表与 GlobalRateLimitFilter 的滑动窗口算法实现关系不清晰

**所在位置**：4.1 节「第一层：IP 级速率限制」与「全局 IP 限流（GlobalRateLimitFilter）」两段

**问题描述**：
文档分别描述了 InMemoryRateLimitGuard（登录端点专用，5 次/10 秒）和 GlobalRateLimitFilter（一般 API 路径，100 次/60 秒）两套独立的限流机制，并说明两者「职责分工明确，计数器实例独立，互不干扰」。但文档未说明：

- 公共工具类 `SlidingWindowCounter` 的接口契约（是否统一使用 `ReentrantLock`？窗口精度的默认配置？）
- 当同一 IP 同时触发了 InMemoryRateLimitGuard 和 GlobalRateLimitFilter 的两套计数器时，响应中的 ErrorCode 如何确定（两者都可能返回 429 + RATE_LIMITED，但 ErrorCode 相同，前端无法区分触发了哪个限流）
- 若后续需要将通用限流（GlobalRateLimitFilter）的计数器也用于登录端点的限流决策，两套独立计数器的数据无法合并

**改进建议**：
- 补充 `SlidingWindowCounter` 的接口契约说明
- 明确两套计数器独立时的 ErrorCode 覆盖行为（哪个过滤器先返回？是否统一使用 RATE_LIMITED？）
- 或在设计决策中明确两套计数器在 Phase 1 保持独立，Phase 2 合并到 Redis

---

### 🟢 问题 7（一般）：密码变更后前端恢复流程的异常场景未覆盖

**所在位置**：3.4 节「密码变更强制策略」、7.4 节「包 D 前端对包 B 的补偿机制」

**问题描述**：
文档在第 10 轮修订中补充了密码变更后的前端恢复流程（PUT /api/auth/password 成功 → 清除 passwordChangeRequired → GET /api/auth/me → GET /api/menu/tree → 跳转首页），但未定义以下异常场景：

1. `GET /api/auth/me` 失败（网络错误/服务端异常）：此时前端已清除 passwordChangeRequired 标记（本地状态），但未获取到最新用户信息，登录页和菜单的渲染状态出现不一致
2. `GET /api/menu/tree` 失败：用户已进入系统但菜单不可用，导航被悬挂
3. 部分步骤成功、部分失败时的重试/回滚策略未定义（例如已清除标记但菜单获取失败后，下一次请求是否因 passwordChangeRequired=false 而被放行到正常页面？若进入正常页面而菜单数据不可用，出现白屏或空导航）

**改进建议**：
- 在 7.4 节补充异常场景说明：`GET /api/auth/me` 和 `GET /api/menu/tree` 任一失败时，应显示全局 loading/错误状态而非跳转到首页（或跳转到首页但菜单区域显示加载失败状态）
- 定义重试机制：是自动重试固定次数，还是显示重试按钮让用户手动触发？

---

### 🟢 问题 8（一般）：8.3 节包 A 数据建模问题的「潜在副作用」和「影响范围」列缺失

**所在位置**：8.3 节 A1 行和 A3 行

**问题描述**：
8.1 节和 8.2 节的缺陷追踪表统一使用 6 列格式（#、问题、当前状态、修复方案、潜在副作用、影响范围）。但 8.3 节中：

- A1（password NOT NULL）：修复方案列填写为「已完成」，缺少「潜在副作用」和「影响范围」
- A3（enabled/visible 默认值）：修复方案列填入了完整描述，但缺少「潜在副作用」和「影响范围」

与 A2（有 6 列完整）和 A4（有 6 列完整）的格式不一致，读者无法判断 A1/A3 是「无副作用」还是「未说明」。

**改进建议**：
- 为 A1 补充「潜在副作用：无（DDL 约束已存在）」和「影响范围：schema.sql」
- 为 A3 补充「潜在副作用：Role、Post 补加 @Column(nullable=false) 后，JPA 自动 DDL 验证可能报现有数据 NULL 错误，需确保迁移前数据已清理」和「影响范围：Role.java、Post.java、schema.sql」

---

## 整体评价

产出已通过 10 轮迭代修订，在技术细节、安全设计、API 契约等方面已趋于完善。本报告补充了内部审议未充分覆盖的 8 个问题：

- **需求响应充分度**：产出覆盖了要求的全部主题（OOD 文档、缺陷修复方案、三包协作边界），格式要求（当前状态、修复方案、潜在副作用、影响范围）在 8.1/8.2 节得到执行，8.3 节有局部遗漏
- **事实错误/逻辑矛盾**：4.1 节速率限制表与 Filter 行为描述存在内部矛盾（问题 1），5.2 节 PUT 语义与 HTTP 规范不符（问题 3），5.2 节 MenuUpdateRequest 内部存在语义冲突（问题 5）
- **深度与完整性**：主要缺口在异常处理路径的明确性——PasswordChangeCheckFilter 的 403 返回机制（问题 4）、登出端点可选请求体的 Controller 处理（问题 2）、密码变更后恢复流程的异常场景（问题 7）均未到达可直接编码的详细程度

**建议优先修复**：问题 1（表头矛盾 + refresh 无限流）、问题 2（请求体悬空）、问题 4（异常处理链路断裂）。
