# 质量审查报告 — Phase 1 ABD OOD 设计文档（v14）

**审查对象**：`a_v14_design_v2.md`（Phase 1 包 A/B/D 统一修复与包 B OOD 设计方案）
**审查视角**：需求响应充分度、整体深度和完整性、可落地性
**迭代轮次**：第 14 次
**审查日期**：2026-06-26

---

## 审查结论

文档经过 15 轮修订迭代，主体内容已较为成熟。核心认证流程、安全策略、模块划分等方面设计完整。以下问题聚焦于当前版本中仍存在的设计缺陷。

---

## 发现问题

### 1. `UserFacade` 门面接口缺少完整定义（严重）

- **位置**：2.2 节依赖规则（用户数据访问途径段）、1.3 节核心抽象一览、2.1 节目录结构
- **问题描述**：2.2 节引入了 `UserFacade` 作为业务模块访问用户数据的门面接口，宣称"应通过 `common-module-api` 中新增的 `UserFacade` 门面接口查询"，但该接口存在以下完整性问题：
  - 未出现在 1.3 节「核心抽象一览」表中，与同属 `common-module-api` 的 `CurrentUser` 形成对比（后者已收录）
  - 未出现在 2.1 节目录结构中，无明确定位
  - 未定义任何方法签名、返回类型、参数列表
  - 未说明与 `CurrentUser` 的职责分工（都是业务模块获取用户数据的途径）
- **严重程度**：严重 — 实施者无法据此编码门面接口；缺乏方法签名意味着无法评估业务模块的适配工作量和接口稳定性
- **改进建议**：
  - 在 1.3 节补充 `UserFacade` 行：`UserFacade` | interface（位于 `common-module-api` 中）| 业务模块获取完整用户数据的门面
  - 在 2.1 节 `common-module-api` 目录树中增加 `UserFacade.java` 条目
  - 定义至少 2-3 个方法签名，如：`Optional<UserInfo> getUserById(Long userId)`、`List<UserInfo> getUsersByIds(Collection<Long> userIds)`，并定义 `UserInfo` 返回 DTO 的结构
  - 补充 `UserFacade` 实现类的定位：位于 `common-module-impl`，通过 `UserRepository` 委托查询
  - 说明 `UserFacade` 与 `CurrentUser` 的分工：`CurrentUser` 仅提供当前登录用户的轻量级标识，`UserFacade` 提供任意用户的完整数据

---

### 2. `PasswordChangeCheckFilter` 白名单路径匹配策略未定义（重要）

- **位置**：3.3 节 PasswordChangeCheckFilter 行为契约（白名单列表）
- **问题描述**：白名单列出了路径 + HTTP 方法的组合（如 `/api/auth/password（PUT）`），但未指定服务端实现使用何种路径匹配策略。Spring Security 生态中至少存在三种常见匹配方式：
  - `AntPathRequestMatcher("/api/auth/password")`（Ant 风格）
  - `request.getRequestURI().equals("/api/auth/password")`（精确字符串比较）
  - `request.getRequestURI().startsWith("/api/auth/password")`（前缀匹配）
  
  不同策略在不同场景下表现不同：精确匹配拒绝带尾部斜杠的路径，前缀匹配可能意外放行 `/api/auth/password-other` 等路径。
- **严重程度**：重要 — 实现者选择的匹配策略若与文档撰写者的预期不一致，可导致白名单放行不足（误 403）或过多（绕过安全控制）
- **改进建议**：明确指定使用 `AntPathRequestMatcher` 进行路径匹配，并列出完整匹配规则：仅比对路径和 HTTP 方法，查询参数不参与匹配。并在 SecurityConfig 中给出 Filter 注册的示例代码段，展示如何构造 AntPathRequestMatcher 列表

---

### 3. `MenuUpdateRequest` 的 PATCH 语义与 Java record 反序列化存在歧义（重要）

- **位置**：5.2 节 MenuUpdateRequest record 定义及「更新语义」说明
- **问题描述**：文档声明"采用局部更新语义（PATCH），请求体中省略的字段保持不变，不覆盖为空值"，但 Java 17 record 经 Jackson 反序列化后，JSON 中省略的字段和显式设为 null 的字段均映射为 Java null，服务端无法区分两者。对于 `Boolean visible`、`Long parentId` 等具有三态语义（true / false / 不更新）的字段，当前设计存在本质性歧义：前端无法表达"将 visible 设为 false"（Set-Clear 需求），也无法区分"省略 parentId"和"清空 parentId"。
- **严重程度**：重要 — 直接导致 PATCH 接口的可实现性与文档描述不一致，实施阶段可能被迫降级为全量替换（PUT）或引入非标准约定
- **改进建议**：二选一：
  - (a) 将 `MenuUpdateRequest` 改为传统 POJO + `Map<String, Boolean> fieldPresence` 标志组，或改用 `JsonNode` / `Map<String, Object>` 接收原始请求体，在 Service 层逐字段判断是否提供
  - (b) 正式声明降级为全量替换语义（PUT），接收完整菜单对象，所有字段必填，消除歧义。同时在设计决策表（第 11 节）记录此决策，并评估对前端的兼容性影响

---

### 4. Refresh 端点在禁用用户场景不递增失败计数，存在低频率无限重放风险（重要）

- **位置**：3.1.3 节步骤 7（禁用/删除用户拒绝刷新流程）
- **问题描述**：当已禁用用户的 Refresh Token 被拒绝时（步骤 7），`LoginAttemptTracker` 不递增用户名或 IP 维度的失败计数。结合 Refresh 端点无速率限制（4.1 节的有意设计决策），攻击者若持有有效旧 Refresh Token，可以低于异常刷新检测阈值（5 秒内 2 次）的频率无限重放请求，既不会被 IP 限流拦截，也不会触发异常告警。异常刷新检测（4.2 节）仅检测滑动窗口内的突发刷新行为（>2 次/5 秒），无法覆盖低频持续攻击场景。
- **严重程度**：重要 — 虽不直接导致数据泄露（请求被拒绝），但攻击者可利用此路径持续消耗服务端资源并绕过安全监控
- **改进建议**：在步骤 7 中增加 IP 维度的失败计数递增（与登录流程步骤 6 行为对齐），或为 Refresh 端点增加独立失败计数器（如每个 userId 在 60 秒窗口内最多 5 次失败的 Refresh 请求，超出后返回 TOKEN_REFRESH_FAILED 并强制前端清除 token）。同时在 4.1 节和 11 节设计决策表中记录此变更

---

### 5. `ProfileUpdateRequest.phone` 可选性未声明（一般）

- **位置**：5.2 节 ProfileUpdateRequest record 定义
- **问题描述**：`phone` 字段仅标注 `@Pattern(regexp = "^1[3-9]\\d{9}$")`，无 `@NotBlank`、`@NotNull` 或明确的可选性声明。虽然 Jakarta Validation 规范中 `@Pattern` 在 null 值时不会触发校验（这恰好是"可选"的预期行为），但未显式说明 `phone` 和 `email` 的可选性，实现者可能误加 `@NotBlank` 导致接口拒绝不含手机号的个人资料提交。
- **严重程度**：一般 — 可通过查阅规范理解行为，但文档应减少此类隐式依赖
- **改进建议**：在 `phone` 和 `email` 字段上补充 `@Size(max = 50)` 或注释说明"可选字段，null 或空字符串时不校验格式"。同步在 6.1 节接口清单的 profile 端点说明中标注"phone 和 email 为可选字段"

---

### 6. `ProfileUpdateRequest.oldPassword` 缺少极端长度约束（一般）

- **位置**：5.2 节 PasswordChangeRequest record 定义
- **问题描述**：`oldPassword` 仅标注 `@NotBlank`，无 `@Size` 约束。BCryptPasswordEncoder.matches() 的输入长度会影响计算时间，虽然 BCrypt 内部有固定轮数（Strength 10 = 2^10 rounds），但极长密码输入可能导致 DOS 风险或 Service Layer 的不必要计算开销。`newPassword` 已有 `@Size(min = 8, max = 64)` 约束而形成对比。
- **严重程度**：一般 — 实际攻击面有限（需已登录用户的口令），但字段间约束不一致反映设计规范性问题
- **改进建议**：为 `oldPassword` 补充 `@Size(max = 128)` 约束，既限制极端输入又不影响正常用户（BCrypt 输出固定为 60 字符，128 足够容纳密文和非密文输入）

---

## 整体质量评价

文档主体设计质量较高，经过 15 轮迭代后核心逻辑已较为完善。主要问题集中在 v14 新增部分（`UserFacade` 门面）的集成不完整，以及 PATCH 语义与 Java record 类型之间的设计冲突。上述 6 个问题中，1 个为严重、3 个为重要、2 个为一般，均不影响文档的整体架构正确性，但应在编码实施前修复。
