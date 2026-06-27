# 质量审查报告（第 2 轮）

## 审查发现

### 🔴 问题 1：`deleted` 列当前状态描述与代码实际不符（事实错误）

- **位置**：8.3 节 A2 行的「当前状态」列
- **描述**：报告声称当前状态为"16 张表 DDL 缺少 NOT NULL"，但实际 `schema.sql`（`backend/application/src/main/resources/db/schema.sql`）中全部 16 张表的 `deleted` 列均已定义 `NOT NULL DEFAULT 0`，无一遗漏。该说法与代码事实严重不符。
- **严重程度**：严重（若据此判断，会导致不必要的工作量估算，且削弱了报告作者对代码现状的了解可信度）
- **改进建议**：将当前状态修正为"16 张表 DDL 已为 `NOT NULL DEFAULT 0`，已完成"，删除 A2 条目或更新为"无需修改"

---

### 🔴 问题 2：Phase 1 Refresh Token 轮换的安全补偿机制存在错误逻辑（逻辑矛盾）

- **位置**：3.1.3 节第 5 步 + 4.2 节「Refresh Token 轮换的安全补偿」
- **描述**：报告声称 Phase 1 中（未黑名单 Old Refresh Token 的情况下），"攻击者使用旧 Refresh Token 时将因 jti 不与新签发的 Refresh Token 冲突而无法通过应用层校验"。这是不成立的——`JwtTokenProvider.validateToken()` 的校验逻辑仅包括签名验证、有效期验证和 type claim 检查，并不存在将旧 Refresh Token 的 jti 与最新签发 jti 进行比较的机制（不依赖黑名单就根本无法判断哪个 jti 是最新的）。因此，在 Phase 1 无 Refresh Token 黑名单的设计下，旧 Refresh Token 在被轮换后仍然有效，攻击者与合法用户可同时持有有效 Refresh Token，安全补偿机制无效。
- **严重程度**：严重（虚假的安全承诺会误导实现者和审查者，导致安全事故预判错误）
- **改进建议**：删除或彻底修改该段描述，如实说明 Phase 1 不做 Refresh Token 黑名单的真实风险（旧 Refresh Token 在被轮换后仍然有效，攻击者在窃取 Refresh Token 后可在合法用户使用之前或之后任意刷新）。可考虑在 Phase 1 就引入简化的服务端状态跟踪（如数据库存储 Refresh Token jti 最新状态），否则应如实标注为已知风险。

---

### 🔴 问题 3：`passwordChangeRequired` 对其他 API 的访问控制缺失（关键遗漏）

- **位置**：3.4 节「密码变更强制策略」、4.4 节 API 端点保护清单
- **描述**：报告引入了 `passwordChangeRequired` 字段，但只定义了登录时前端跳转到修改密码页的流程，完全没有说明：**当用户已登录、`passwordChangeRequired=true` 时，访问其他认证端点（如 `/api/auth/me`、`/api/menu/tree` 等）应该如何处理**。如果无任何限制，用户可以通过直接 URL 访问、书签或保持旧页面等方式绕过密码修改强制要求，密码过渡策略失去意义。这是完整的强制密码修改需求中不可遗漏的关键设计。
- **严重程度**：重要
- **改进建议**：在 `JwtAuthenticationFilter` 或 `AuthServiceImpl` 中补充：当 `passwordChangeRequired=true` 时，除 `/api/auth/password` 和 `/api/auth/logout` 外，所有 API 返回 403 禁止访问。在 API 端点保护清单中明确标注此规则。

---

### 🔴 问题 4：Section 4.3 修复方案状态与 Section 5.1 及代码事实相互矛盾（内部矛盾）

- **位置**：4.3 节「修复方案」列表 vs 5.1 节 Role.java/Post.java 变更表
- **描述**：4.3 节底部修复方案列表将 Role.java 和 Post.java 补加 `@Column(nullable=false)` 标注为"→ 已完成"。然而：① 实际代码中 Role.java:24 和 Post.java:27 的 `enabled` 字段确实缺少 `@Column(nullable=false)`，尚未完成；② 5.1 节 Role.java 变更表也明确写着"缺 `@Column(nullable=false)`"。同一个文档中两处信息冲突，一处说已完成、一处说待修复，且均与实际代码不符（实际是待修复）。
- **严重程度**：严重（文档自相矛盾会使实现者无法判断正确状态）
- **改进建议**：统一移除"→ 已完成"标记，如实标注为待修复。修复方案应明确指出是"设计方案包含此修复项"，而非"代码已完成"。

---

### 🔴 问题 5：`LoginResponse` 未包含 `userId`/`username`（可落地性缺陷）

- **位置**：5.2 节 `LoginResponse` record 定义
- **描述**：`LoginResponse` 包含 `UserInfoResponse user` 对象，但 `UserInfoResponse` 中的字段在用户首次登录时尚未完全可用（`role` 需要计算主角色、`permissions` 需要收集）。前端在登录后可能需要立即可用的用户标识（如 `userId` 和 `username`），尤其在 `passwordChangeRequired=true` 场景下，前端需要用户 ID 来完成密码修改 API 调用。当前设计依赖于通过 `GET /api/auth/me` 获取用户信息，但在 passwordChangeRequired 场景中用户可能无法访问该端点。这形成了一个循环依赖。
- **严重程度**：重要
- **改进建议**：在 `LoginResponse` 中直接包含 `userId`（Long）和 `username`（String）作为顶层字段，确保前端在登录后即可获得基本用户标识，无需额外 API 调用。

---

### 🟡 问题 6：菜单 CRUD 缺少 `MenuCreateRequest` 和 `MenuUpdateRequest` DTO 定义（关键遗漏）

- **位置**：5.2 节 DTO 列表；6.1 节接口清单（`POST /api/menu`、`PUT /api/menu/{id}`）
- **描述**：接口清单列举了菜单创建和更新端点，但 5.2 节的 DTO 定义中完全没有包含这两个请求 DTO。`MenuCreateRequest` 和 `MenuUpdateRequest` 仅在目录结构中列出（`dto/request/` 包下），未提供字段定义。对于一份 OOD 设计文档来说，关键接口的入参 DTO 缺失意味着下游消费者（前端、测试、其他后端模块）无法开展工作。
- **严重程度**：重要
- **改进建议**：补充 `MenuCreateRequest`（至少包含 code、name、parentId、type、path、icon、sort、visible、enabled）和 `MenuUpdateRequest` 的 record 定义到 5.2 节。

---

### 🟡 问题 7：登录错误消息差异化导致用户名可枚举（安全设计遗漏）

- **位置**：3.1.1 节登录流程步骤 5 和步骤 6
- **描述**：步骤 5 对不存在的用户名返回"用户名或密码错误"，而步骤 6 对已存在但 `enabled=false` 的用户返回"账户已停用"。这两种不同的错误消息可以让攻击者枚举有效用户名：收到"账户已停用"即可确认该用户名存在。对于医疗系统，用户名枚举风险不可忽视。
- **严重程度**：重要
- **改进建议**：统一错误消息：步骤 6 也应返回"用户名或密码错误"（流程走到步骤 6 意味着用户名已存在，但无需向前端泄露这个信息），配合安全日志记录真实原因供运维排查。

---

### 🟡 问题 8：Section 8.3 A2 与 Section 5.1 对 `deleted` 列的完成状态矛盾（内部矛盾）

- **位置**：5.1 节 User.java 变更表 vs 8.3 节 A2 行
- **描述**：5.1 节 User.java 变更表第 4 行明确标注 `deleted` 列状态为"`deleted` 列已由 BaseEntity 提供，schema.sql 中 `deleted` 已为 NOT NULL DEFAULT 0，**已完成**"。但 8.3 节 A2 行「当前状态」列声称"16 张表 DDL 缺少 NOT NULL"，「修复方案」列声称需要"schema.sql 全部修正为 NOT NULL DEFAULT 0"。两者矛盾：一处说已完成，一处说需要修复。
- **严重程度**：重要
- **改进建议**：统一为"已完成"，删除 8.3 A2 行中不需要的修复方案。若保留是为了记录历史问题，应标注"已修复"。

---

### 🟡 问题 9：Section 7.5 声称已合并重复条目，但 8.1 节仍保留 M12_P11（执行错误）

- **位置**：7.5 节 vs 8.1 节 M12_P11 行
- **描述**：7.5 节明确声明"8.1 节和 8.2 节中仅保留一条记录（前端 M16），删除后端重复条目"。但 8.1 节中 M12_P11（用户信息明文存 localStorage）仍然存在。7.5 节的说法与文档实际内容不符。
- **严重程度**：一般（合并策略正确，但执行不彻底）
- **改进建议**：删除 8.1 节的 M12_P11 行，保持与 7.5 节声明一致。

---

### 🟡 问题 10：多 Tab 并发 token 刷新竞态未处理（可落地性缺陷）

- **位置**：3.1.3 节 Token 刷新流程；7.4 节前端补偿机制
- **描述**：报告定义了 401 静默刷新，但未考虑用户同时打开多个浏览器 Tab 时的并发刷新问题。多个 Tab 同时检测到 401 会并行发起多次 refresh 请求，其中只有一个会成功（因为 Refresh Token 轮换后旧 token 失效），其余 Tab 的刷新会失败，导致用户被错误地踢出登录页。这是一个常见的前端 token 管理竞态场景。
- **严重程度**：重要
- **改进建议**：7.4 节新增：前端 refresh 逻辑应使用互斥锁（Promise 单例模式），保证同一时间只执行一次 refresh 操作，所有等待的请求共享同一个刷新 Promise 的结果。

---

### 🟡 问题 11：`getPrimaryRoleCode()` 未定义主角色判定策略（关键遗漏）

- **位置**：7.3 节包 A → 包 B 影响分析表最后一行
- **描述**：报告引入了 `getPrimaryRoleCode()` 辅助方法来将用户的多角色映射为单一 `role` 字段（对应前端期望的角色标识），但未定义：① 当用户有多个角色时如何选择"主角色"（第一个？最高权限？显式标记？）；② 用户无角色时的降级策略。不同业务模块（doctor/admin/patient）对此可能有不同期望。
- **严重程度**：重要
- **改进建议**：补充主角色选择策略的显式规则（如：User 实体新增 `primaryRole` 字段，或按角色优先级排序取最高优先级），并说明无角色场景的处理方案。

---

### 🟡 问题 12：SecurityConfigPhase1 依赖注入的旧 Filter 未纳入迁移方案（可落地性缺陷）

- **位置**：2.2 节模块依赖方向；12 节修订说明 P8
- **描述**：修订说明 P8 声称已补充 Filter 迁移方案，但实际 2.2 节只描述了"application 模块中原有 JwtAuthenticationFilter 的引用全部删除"，未指出 `SecurityConfigPhase1.java` 第 37 行已通过构造函数依赖注入引用了旧位置（`com.aimedical.config`）的 `JwtAuthenticationFilter`。该 Filter 移动到 `common-module-impl/auth/security/` 后，`SecurityConfigPhase1` 的 import 和注入类型需要同步变更，忽略此细节会导致编译错误。
- **严重程度**：重要
- **改进建议**：2.2 节补充说明：`SecurityConfigPhase1` 的注入类型需从 `com.aimedical.config.JwtAuthenticationFilter` 改为 `com.aimedical.modules.commonmodule.auth.security.JwtAuthenticationFilter`；或新增一个配置类统一管理 common-module-impl 中 Filter 的注册。

---

## 整体质量评价

文档在需求响应方面覆盖了包 B OOD 的主要维度，第 1 轮发现的多数问题已在 v2 中修订。但在以下维度仍存在不足：

1. **准确性**：多处"当前状态"判断与代码实际不符（deleted NOT NULL、Role/Post `@Column` 标注），降低了整体可信度
2. **逻辑一致性**：文档内部存在多处自相矛盾（4.3 vs 5.1 的完成状态、7.5 vs 8.1 的条目重复）
3. **安全设计完整性**：passwordChangeRequired 的阻断机制未定义、登录错误消息差异化导致枚举风险、Refresh Token 轮换安全补偿声明有误
4. **可落地性**：菜单 DTO 定义遗漏、多 Tab 并发竞态未处理、getPrimaryRoleCode 策略未定义、旧 Filter 过渡方案有遗漏

以上问题中，问题 1-5 对后续编码实现和系统安全有直接影响，建议优先在下一轮修订中处理。
