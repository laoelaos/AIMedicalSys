# 质量审查报告 — Phase 1 包 A/B/D 统一修复与包 B OOD 设计方案（v12）

审查轮次：第 12 轮
审查视角：需求响应充分度、整体深度与完整性、可落地性、异常场景覆盖
审查日期：2026-06-26

---

## 发现的问题

### 问题 1（重要）：Role.sort 字段缺少 Java 默认值与 NOT NULL 约束，JPA 持久化存在不一致风险

**所在位置**：5.1 节 Role.java 变更表；4.3 节 NOT NULL 约束状态确认表

**问题描述**：
Role.java 新增字段定义为 `private Integer sort;`，无 Java 默认值；schema.sql 中对应列定义为 `INT DEFAULT 0`（有 DB 默认值但无 NOT NULL 约束）。这产生两个问题：
(a) Java 层面 sort 为 null → Hibernate 可能向 DB 写入 NULL 值（覆盖 DEFAULT 0），或遇到下游代码对 sort 的空指针解引用；
(b) DB 层面未指定 NOT NULL，允许 null 值进入数据库，与"排序字段不应为空"的业务预期相悖。
5.1 节和 4.3 节均未标注 `@Column(nullable=false)` 或 Java 默认值 `= 0`。

**严重程度**：重要

**改进建议**：
在 Role.java 变更表中补充：
- Java：`@Column(nullable=false) private Integer sort = 0;`
- DDL：`sys_role.sort INT NOT NULL DEFAULT 0`

---

### 问题 2（重要）：SlidingWindowCounter 声明为"包级私有"但被跨包复用，存在可见性矛盾

**所在位置**：4.1 节「SlidingWindowCounter 契约」段

**问题描述**：
文档声明 `SlidingWindowCounter` 为"包级私有工具类，供 InMemoryRateLimitGuard 和 GlobalRateLimitFilter 共用"。但根据 2.1 节目录结构：
- `InMemoryRateLimitGuard` 位于 `auth/rateLimit/` 包（`com.aimedical.modules.commonmodule.auth.rateLimit`）
- `GlobalRateLimitFilter` 位于 `auth/security/` 包（`com.aimedical.modules.commonmodule.auth.security`）

Java 中"包级私有"（default access modifier）的类仅在**同一包**内可见，跨子包不可见。两个消费者属于不同的子包，无法访问同一个包级私有类。这是 Java 访问控制的基本规则，设计存在矛盾。

**严重程度**：重要

**改进建议**：
三个可选方案择一：
(a) 将 `SlidingWindowCounter` 提升为 public 类，放在共享包（如 `auth/` 根包或新增 `auth/common/` 子包）；
(b) 将 `GlobalRateLimitFilter` 和 `InMemoryRateLimitGuard` 放入同一包中（如统一归入 `auth/security/` 或新增 `auth/ratelimit/`）；
(c) 两限流器各自独立实现滑动窗口（不共享工具类），文档删除"提取公共工具类"的声明。

---

### 问题 3（重要）：密码变更流程中清除 SecurityContext 操作无实际安全效果，说明不准确

**所在位置**：3.1.6 节步骤 10

**问题描述**：
步骤 10 声明"清除当前请求 SecurityContext（清除当前线程的安全上下文，避免密码变更操作后的请求误用旧上下文）"。此描述不准确：
- `SecurityContextHolder` 为请求作用域（`MODE_THREADLOCAL`），每个请求独立持有 SecurityContext；清除当前请求的 SecurityContext 不影响后续请求。
- 密码变更请求（`PUT /api/auth/password`）的响应已返回给客户端，后续请求进入时由 `JwtAuthenticationFilter` 重新装配 SecurityContext，不存在"误用旧上下文"的场景。
- 该操作发生在 `AuthServiceImpl.changePassword()` 内部——如果后续代码（如同一个 Controller 方法的后处理逻辑）需要访问 SecurityContext，此操作反而会造成空指针。

**严重程度**：重要

**改进建议**：
二选一：
(a) 删除步骤 10（清除 SecurityContext），直接在步骤 11 返回成功响应——因密码变更后 Access Token 继续有效至自然过期，无需任何清理动作；
(b) 若坚持保留，须将说明修正为：对当前请求的 SecurityContext 做清理，但明确声明此操作不影响后续请求，后续请求由 JwtAuthenticationFilter 重新鉴权。

---

### 问题 4（重要）：PasswordPolicy 接口缺少方法签名定义，无法直接指导编码

**所在位置**：1.3 节核心抽象一览；4.3 节密码策略

**问题描述**：
`PasswordPolicy` 在 1.3 节中定义为"密码复杂度策略契约，定义校验方法"，但整篇文档未给出该接口的方法签名。下游实现者无法确认：
- 校验方法返回布尔值还是包含错误码的 `ValidationResult` 对象？
- 是否需要 `username` 参数（用于"密码不得包含用户名"规则）？
- 是否需要当前密码（用于新旧密码相似度检查，虽 Phase 1 未要求但接口预留应考虑）？

作为 OOD 设计文档，核心抽象的方法签名属于必要信息，缺失将导致编码阶段的设计走样。

**严重程度**：重要

**改进建议**：
在 1.3 节 `PasswordPolicy` 行或 4.3 节中补充接口方法签名定义。推荐：

```java
public interface PasswordPolicy {
    /**
     * @param password  待校验的密码明文
     * @param username  用户登录名（用于"不得包含用户名"检查）
     * @return null 表示校验通过；非 null 表示失败对应的 ErrorCode
     */
    ErrorCode validate(String password, String username);
}
```

---

### 问题 5（中等）：菜单删除操作未定义子菜单处理策略（级联/拦截/置空）

**所在位置**：6.1 节接口清单，DELETE `/api/menu/{id}`
**涉及**：5.2 节 MenuResponse（递归 children 结构）

**问题描述**：
文档定义了菜单的递归树结构（`MenuResponse.children`），支持多级菜单。但删除菜单（`DELETE /api/menu/{id}`）的操作未定义以下行为：
- 若被删除的菜单有子菜单，子菜单如何处理？（级联删除 / 阻止删除并返回 400 / 子菜单提升为父级同级？）
- 若被删除的菜单被角色关联（权限关联），关联如何处理？
- 是否存在软删除 vs 硬删除的选择？

此遗漏影响 `MenuServiceImpl` 的实现决策和前端行为（删除后菜单树重建）。

**严重程度**：中等

**改进建议**：
在 6.1 节 DELETE `/api/menu/{id}` 行或 5.2 节 MenuResponse 说明中补充删除语义。推荐 Phase 1 策略：有子菜单时阻止删除（返回 400 + CHILDREN_EXIST 错误码），要求先删除子菜单或批量删除。角色关联的菜单删除同理。

---

### 问题 6（中等）：`expiresIn` 字段语义自相矛盾——"剩余有效秒数"与"从签发时计算"冲突

**所在位置**：5.2 节 LoginResponse 及 TokenRefreshResponse 定义

**问题描述**：
`expiresIn` 的语义同时描述为"剩余有效秒数"和"从签发时计算"：
- "剩余有效秒数"暗示动态递减值（token 签发已过去 t 秒则值为 TTL - t）；
- "从签发时计算"暗示固定值 TTL（刚签发时总是 900）。

若为固定 TTL（始终 900），称为"有效秒数"即可，无需"剩余"二字；若为递减值，则"从签发时计算"不准确。JSON 示例中写死了 `expiresIn: 900`，与实际运行时行为不完全对应（运行时若返回真实剩余时间则在签发 5 分钟后应为 600）。

**严重程度**：中等

**改进建议**：
明确选择一个语义并在文档中统一表述：
- 若选择固定 TTL：字段名保持 `expiresIn`，语义改为"access token 的 TTL（有效秒数，始终为配置值 900）"，删除"剩余"和"从签发时计算"的表述；
- 若选择真实剩余时间：修正语义为"access token 距过期的剩余秒数"，并在返回值计算中说明是从签发时刻计算（即 `签发时间 + TTL - 当前时间`）。

---

### 问题 7（中等）：`/api/auth/refresh` 端点调用建议未文档化——推荐不携带 Authorization header

**所在位置**：3.1.3 节 Token 刷新流程；7.2 节包 B → 包 D 契约

**问题描述**：
刷新端点被设计为 `permitAll`（无需 JWT），但前端 axios 拦截器的通用模式会对所有 API 自动附加 `Authorization: Bearer <token>` header。当 `passwordChangeRequired` 用户调用刷新时：
- 若携带过期 Access Token → JwtAuthenticationFilter 跳过（过期不设 SecurityContext）→ PasswordChangeCheckFilter 跳过（无 auth）→ 刷新成功（正常路径）；
- 若携带有效 Access Token → JwtAuthenticationFilter 设置 SecurityContext → PasswordChangeCheckFilter 检测到 `passwordChangeRequired=true` → 返回 403 PASSWORD_CHANGE_REQUIRED，刷新失败。

第二种场景虽在实践中较少触发（刷新通常在 401 后才触发），但设计未文档化此风险，前端实现者可能无意识地传入有效 token 导致踩坑。

**严重程度**：中等

**改进建议**：
在 7.2 节 401 处理契约中补充：`POST /api/auth/refresh` 的 axios 调用不应携带 Authorization header，或确保使用独立的 axios 实例/请求配置（`headers: { Authorization: null }`）。同时在 3.1.3 节"设计要点"或"刷新时机"说明中补充此约束。

---

### 问题 8（轻微）：刷新端点未加入 PasswordChangeCheckFilter 白名单，存在理论防护盲区

**所在位置**：3.3 节 PasswordChangeCheckFilter 行为契约

**问题描述**：
PasswordChangeCheckFilter 的白名单包含 `/api/auth/password` 和 `/api/auth/logout`，但未包含 `/api/auth/refresh`。虽如问题 7 分析，实践中通过不携带 Authorization header 可绕过，但若前端实现失误（携带了有效 token 刷新）或未来有人修改刷新端点为 authenticated（如 Phase 2），导致 PasswordChangeCheckFilter 拦截刷新，用户将被卡在"token 过期无法刷新→无法改密码→无法登录"的死锁中。将 `/api/auth/refresh` 加入白名单是一种"防御性设计"，开销为零。

**严重程度**：轻微

**改进建议**：
将 `/api/auth/refresh`（POST）加入 PasswordChangeCheckFilter 白名单，与 `/api/auth/password` 和 `/api/auth/logout` 并列。此变更不影响任何安全语义（刷新端点本身的认证由 Refresh Token 自验证保证），仅为消除理论死锁路径。

---

## 整体质量评价

文档历经 11 轮迭代修正，主体质量较高：所有原始需求已充分覆盖（认证流程、令牌设计、安全防护、协作边界、问题修复追踪），各节内容一致性好，设计决策记录完整。上述发现的 8 个问题中无严重级别（S0/S1），2 个重要级别（S2）、4 个中等级别（S3）、1 个轻微级别（S4），不影响整体设计的合理性和可实施性。

文档在**可落地性**方面表现良好：Filter 链顺序明确、DTO 定义完整（含 Bean Validation 注解）、API 端点保护清单完整、Breaking Change 声明清晰。核心不足在于个别抽象接口缺少方法签名（PasswordPolicy）和少量 Java 可见性设计矛盾（SlidingWindowCounter），修复后即可直接指导编码实现。
