# 质量审查报告（v13）

审查对象：Phase 1 包 A/B/D 统一修复与包 B OOD 设计方案（v13）
审查轮次：第 13 次
审查维度：需求响应充分度、事实错误与逻辑矛盾、深度与完整性、可落地性

---

## 问题清单

### 🔴 严重问题

#### 1. Refresh 端点可绕过 passwordChangeRequired 强制约束

**问题描述**：v13 修订说明第 8 项将 `/api/auth/refresh` 加入 PasswordChangeCheckFilter 白名单后，`passwordChangeRequired=true` 的用户可通过 Refresh 端点无限续期 Access Token，始终不修改密码。PasswordChangeCheckFilter 白名单包含 `/api/auth/refresh`，但 `AuthServiceImpl.refresh()` 方法中不存在任何 `passwordChangeRequired` 状态检查。用户登录后即使不修改密码，也可通过 `POST /api/auth/refresh` 反复获取新令牌，维持认证状态。

此问题是 v13 修订（将 refresh 加入白名单）引入的回归——迭代第 2 轮第 3 条审查意见原本要求 passwordChangeRequired 阻断所有除 `password` 和 `logout` 外的端点，v13 的"修复"放宽了此约束但未在 refresh 逻辑中补充补偿性检查。

**所在位置**：3.1.3 节 Token 刷新流程（缺少 passwordChangeRequired 检查步骤）；3.3 节 PasswordChangeCheckFilter 白名单；3.4 节 passwordChangeRequired 访问控制

**严重程度**：严重

**改进建议**：在 `AuthServiceImpl.refresh()` 中增加 `passwordChangeRequired` 检查：若用户在 DB 中 `passwordChangeRequired=true`，拒绝刷新并返回 `PASSWORD_CHANGE_REQUIRED`（403），强制前端引导用户修改密码。若此为有意设计决策，需在 11 节设计决策表中记录并说明理由。

---

### 🟡 重要问题

#### 2. 3.4 节白名单与 3.3 节/3.1.2 节白名单不一致（内部矛盾）

**问题描述**：3.4 节「passwordChangeRequired 访问控制」的白名单仅列出两个端点：`PUT /api/auth/password` 和 `POST /api/auth/logout`。但 3.1.2 节已认证请求流程步骤 7 和 3.3 节 PasswordChangeCheckFilter 行为契约中的白名单包含三个端点：`/api/auth/password`、`/api/auth/logout`、`/api/auth/refresh`。3.4 节未随 v13 修订同步更新，形成内部矛盾。

**所在位置**：3.4 节 passwordChangeRequired 访问控制（白名单列表）

**严重程度**：重要

**改进建议**：将 3.4 节白名单补充 `POST /api/auth/refresh` 条目，与 3.1.2 节和 3.3 节保持一致。

#### 3. PasswordChangeService 和 CurrentUser 接口缺少方法签名（可落地性缺陷）

**问题描述**：文档声称可直接指导编码实现，但两个接口仅有职责描述而无完整方法签名：
- `PasswordChangeService`（1.3 节、3.4 节）：定义了两种触发场景，未给出方法签名（参数、返回值、异常）
- `CurrentUser`（1.3 节）：描述为"轻量级类型化访问器"，未给出方法签名

v13 已修复 PasswordPolicy 缺少方法签名的问题（修订说明第 4 项），但 PasswordChangeService 和 CurrentUser 遗漏。

**所在位置**：1.3 节核心抽象一览；3.4 节

**严重程度**：重要

**改进建议**：补充 `PasswordChangeService` 接口方法签名，例如：
```java
boolean isChangeRequired(Long userId);
void markChangeRequired(Long userId);
void clearChangeRequired(Long userId);
```
补充 `CurrentUser` 接口方法签名，例如：
```java
Long getUserId();
String getUsername();
UserType getUserType();
```

#### 4. 包 A 实体移至 common-module-impl 的结构变更影响评估不完整

**问题描述**：2.1 节将 User、Role、Post、PermissionFunction 等包 A 实体放在 `common-module-impl/permission/` 下，暗示这些实体从原包 A 模块迁移至 common-module-impl。2.2 节声称业务模块（patient/doctor/admin）"不依赖 common-module-impl"，但未评估现有代码中这些模块是否已直接引用包 A 实体。迁移路径缺少对现有依赖的识别和处理方案。

7.2 节明确表示业务模块通过 `common-module-api` 访问共享类型（UserType、PositionEnum），但 User 等实体不属于 API 模块的共享类型——业务模块如何获取用户数据？未说明。

**所在位置**：2.1 节、2.2 节、7.1 节、7.2 节

**严重程度**：重要

**改进建议**：补充评估现有代码中对 User/Role/Post/PermissionFunction 实体的外部引用；明确业务模块访问用户数据的途径（通过 AuthService 门面 vs 直接使用 Repository）；补充迁移步骤。

#### 5. SlidingWindowCounter 声明为 public 但包结构仍存在困惑

**问题描述**：v13 修订说明第 2 项将 SlidingWindowCounter 声明为 public 以解决跨包可见性问题。但 4.1 节描述 InMemoryRateLimitGuard "使用 `ConcurrentHashMap<String, SlidingWindowCounter>`"，同时描述 GlobalRateLimitFilter "独立维护自己的滑动窗口计数器（同样基于 `ConcurrentHashMap<String, SlidingWindowCounter>`）"。SlidingWindowCounter 本身是基于 ConcurrentHashMap 的计数器工具类，"ConcurrentHashMap<String, SlidingWindowCounter>" 意味着每个限流 key 对应一个窗口对象。两限流器描述雷同但职责不同，开发者可能误以为两者共享同一个计数器实例。

**所在位置**：4.1 节 InMemoryRateLimitGuard 和 GlobalRateLimitFilter 实现描述

**严重程度**：重要

**改进建议**：明确注释两套计数器实例相互独立，使用不同的 key 空间和窗口参数。例如："InMemoryRateLimitGuard 和 GlobalRateLimitFilter 各自持有独立的 `ConcurrentHashMap<String, SlidingWindowCounter>` 实例，两者互不干扰。"

---

### 🔵 一般问题

#### 6. 3.4 节 "403 FORBIDDEN" 与 PasswordChangeCheckFilter 的 PASSWORD_CHANGE_REQUIRED 描述不一致

**问题描述**：3.4 节「passwordChangeRequired 访问控制」描述为"返回 403 FORBIDDEN"。但 PasswordChangeCheckFilter 行为契约（3.3 节）定义的返回值是 ErrorCode.PASSWORD_CHANGE_REQUIRED + HTTP 403，且 10.2 节 ErrorCode 表明确区分了 FORBIDDEN（角色权限不足）和 PASSWORD_CHANGE_REQUIRED（密码变更阻断）。"403 FORBIDDEN"的表述在 3.4 节会误导开发者使用标准 FORBIDDEN 而非专用错误码。

**所在位置**：3.4 节 passwordChangeRequired 访问控制

**严重程度**：一般

**改进建议**：将"返回 403 FORBIDDEN"修正为"返回 403 + ErrorCode.PASSWORD_CHANGE_REQUIRED"，与 PasswordChangeCheckFilter 行为契约保持一致。

#### 7. refresh 端点携带 Authorization header 时的服务端行为未完整定义

**问题描述**：3.1.3 节步骤 1 和 7.2 节要求 refresh 请求不应携带 Authorization header，但未定义前端或攻击者「携带了」时的服务端处理策略。若携带了有效的旧 Access Token，JwtAuthenticationFilter 会照常装配 SecurityContext，PasswordChangeCheckFilter 因 refresh 在白名单中而放行。此路径虽功能正常，但文档未说明在此场景下 passwordChangeRequired 的检查行为（见问题 1）。

**所在位置**：3.1.3 节步骤 1、7.2 节 refresh 端点调用约束

**严重程度**：一般

**改进建议**：补充说明若前端误携带 Authorization header，服务端的行为是"JwtAuthenticationFilter 正常处理，SecurityContext 被装配但 refresh 逻辑不依赖前置认证"；同时应等待问题 1 的修复确认此场景的 passwordChangeRequired 行为。

#### 8. login() 锁检查的 IP 维度与 GlobalRateLimitFilter 重复提及，分工不够清晰

**问题描述**：登录流程步骤 2（3.1.1 节）由 RateLimitGuard 做 IP 级速率限制（5 次/10 秒），步骤 3 由 LoginAttemptTracker 做 IP 维度锁定检查（20 次/30 分钟）。但步骤 2 的 RateLimitGuard 是 AuthServiceImpl 内调用的"登录专用限流"，而 GlobalRateLimitFilter 是 Filter 层面的"全局限流"。文档虽说明了两者分工，但登录流程步骤 2 称为"RateLimitGuard 检查请求来源 IP 是否触发限流"，未明确标注这是 InMemoryRateLimitGuard（登录专用限流），还是 AuthServiceImpl 内部调用逻辑与 Filter 层面共同作用。实现者不清楚 IP 限流在哪个层级完成。

**所在位置**：3.1.1 节步骤 2、4.1 节

**严重程度**：一般

**改进建议**：在 3.1.1 节步骤 2 明确标注"AuthServiceImpl 内部调用 InMemoryRateLimitGuard.tryAcquire()"而非模糊的"RateLimitGuard 检查"，区分 Filter 级别的 GlobalRateLimitFilter 和 Service 级别的登录专用限流。

---

## 整体质量评价

文档经过 12 轮迭代审议，在需求覆盖度、安全设计深度、前后端协作边界等方面已达到较高成熟度，主要质量问题已在前序轮次中得到有效修复。

当前 v13 中存在的主要风险集中在一个**回归性问题**（Issue 1）——将 refresh 加入 PasswordChangeCheckFilter 白名单但未补充 refresh 逻辑中的 passwordChangeRequired 检查，导致强制密码变更约束可被绕过。此外存在少量内部矛盾（Issue 2）、接口完整性不足（Issue 3）和影响评估不完整（Issue 4）。

建议优先修复 Issue 1 后重新审查，其余问题可在同轮次中一并修正。
