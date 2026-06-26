# 质量审查报告 - 第 8 轮

## 审查范围

待审查产出：Phase 1 包 A/B/D 统一修复与包 B OOD 设计方案（a_v8_copy_from_v7.md）
审查视角：需求响应充分度、事实正确性、逻辑一致性、深度与完整性
审查轮次：第 1 次质量审查

---

## 发现的问题清单

### 🔴 问题 1（严重）：密码变更后"强制用户重新登录"语义与设计说明自相矛盾

- **所在位置**：3.1.6 节步骤 10 vs 3.1.6 节设计说明第二段
- **问题描述**：步骤 10 描述为"清除 SecurityContext（强制用户重新登录获取新令牌）"，暗示旧 Access Token 立即失效。但紧随其后的设计说明明确写道"密码变更后不清除已登录用户的 Access Token。用户可继续使用当前会话至 Access Token 过期（最长 15 分钟）。这是有意为之的权衡"——两段描述构成了直接矛盾：前者声称强制重新登录，后者明确不清除。实际运行时清除 SecurityContext 不影响已发出的响应，客户端凭旧 token 仍然可以通过 JwtAuthenticationFilter（Access Token 不检查 tokenVersion），"强制重新登录"的预期效果不成立。
- **严重程度**：严重
- **改进建议**：二选一统一语义：要么删除步骤 10 的"强制用户重新登录"表述，补充为"清除当前请求 SecurityContext（作为安全最佳实践），客户端可继续使用旧 Access Token 至其自然过期（最长 15 分钟）"；要么真正实现强制重新登录（将旧 Access Token 加入黑名单），但需在设计说明中删除"不清除旧 Access Token"的表述。推荐前者，与设计说明保持一致。

### 🔴 问题 2（严重）：PASSWORD_COMMON ErrorCode 在 Phase 1 中不可达，形成代码死分支

- **所在位置**：4.3 节密码策略表 vs 10.2 节 ErrorCode 表 PASSWORD_COMMON 行
- **问题描述**：4.3 节在"常见密码"规则行明确标注"不得为 Top 10000 常见密码（**Phase 2 实现**）"，但 10.2 节 ErrorCode 表中 PASSWORD_COMMON 已被定义（触发场景："密码在Top 10000常见密码列表中"），且状态码 200 映射已在 10.1 节表中列出。Phase 1 中此错误码永远不会被触发——`PasswordPolicy` 接口不会实现常见密码检查，代码中没有任何代码路径能抛出此 ErrorCode。这将导致错误码枚举中产生无法到达的分支。
- **严重程度**：严重
- **改进建议**：从 10.2 节和 10.1 节中删除 PASSWORD_COMMON 条目（Phase 1 范围），并在 4.3 节对应规则行标注"Phase 2 引入后同步补充 ErrorCode"。若在 Phase 1 保留枚举值作为占位，须在 10.2 节明确标注"Phase 2 启用，Phase 1 中不可达"。

### 🔴 问题 3（严重）：登录流程（3.1.1 步骤 10）未从 User 实体获取 tokenVersion 用于 Refresh Token 签发，导致 tokenVersion 闭环断裂

- **所在位置**：3.1.1 节步骤 10 vs 3.2 节 Refresh Token Claims（含 tokenVersion）vs 3.1.3 节步骤 7（验证 tokenVersion）
- **问题描述**：3.1.3 节刷新流程步骤 7 要求验证 Refresh Token claims 中的 tokenVersion 与 DB 中当前值一致，以确保密码变更后旧 Refresh Token 失效。3.2 节 Refresh Token Claims 定义了 `tokenVersion` 字段。但 3.1.1 节登录流程步骤 10 仅提到"携带唯一 jti 标识"和长短有效期，**未说明签发 Refresh Token 时从何处获取 tokenVersion 值**（应从 User.tokenVersion 读取）。该断裂意味着实现者可能不知道登录时生成的 Refresh Token 中需要嵌入 tokenVersion，或者错误地使用默认值 0 而非 User 实体的当前值。这会导致 tokenVersion 验证机制的完整闭环被打破——登录时未正确嵌入 tokenVersion 值，刷新时比对永远正确或永远错误。
- **严重程度**：严重
- **改进建议**：在 3.1.1 节步骤 10 中补充"JwtTokenProvider 从 `User.tokenVersion` 读取当前版本号，嵌入 Refresh Token claims 中的 `tokenVersion` 字段"。同时建议在 JwtTokenProvider 生成 Refresh Token 的方法签名中明确 `tokenVersion` 入参。

### 🟡 问题 4（重要）：展平路由 name 唯一性保证策略存在运行时竞态隐患

- **所在位置**：8.2 节 H5 修复方案"name 唯一性保证策略"段
- **问题描述**：策略描述为"使用 permissionCode 按驼峰映射（如 `system:user` 映射为 `SystemUser`），运行时检测冲突时在后追加数字序号（如 `SystemUser_1`）"。但 Vue Router 的 `router.addRoute()` 在 name 冲突时会**静默覆盖**已有路由而非抛出异常。策略依赖"运行时检测冲突"意味着：首次调用 addRoute 时冲突导致覆盖（恶意或意外的 name 重用），然后检测到冲突再追加序号第二次 addRoute。这会导致以下几个问题：(a) 首次注册的覆盖不会被回滚，同名路由丢失；(b) 若 name 检测是 `router.hasRoute()` 前置检查，则根菜单和子菜单可能出现跨权限的 name 冲突（如 doctor 端和 admin 端同权限不同组件）；(c)"追加数字序号"生成的 name 不可预测，影响通过 name 编程导航的可靠性。
- **严重程度**：重要
- **改进建议**：改为确定性策略——在 `convertMenusToRoutes` 的递归遍历过程中，使用 `permissionCode` 映射为唯一 name 时，直接通过 `/` 转驼峰 + 添加前端应用前缀（如 `Doctor_SystemUser` 或 `Admin_SystemUser`）消除跨权限冲突，避免运行时按需修正。若仍保留运行时检测，须补充说明"检测到冲突时，拒绝注册而非覆盖"，并给出 name 冲突时的 Fallback 行为。

### 🟡 问题 5（重要）：登录成功（3.1.1 步骤 8）仅清除用户名维度的失败计数，IP 维度未处理

- **所在位置**：3.1.1 节步骤 8 vs 4.1 节 LoginAttemptTracker 双维度设计
- **问题描述**：4.1 节 LoginAttemptTracker 定义为用户名和 IP 双维度（用户名 5 次/15 分钟锁定，IP 20 次/30 分钟锁定）。3.1.1 节步骤 8（登录成功）仅说"LoginAttemptTracker 清除该用户名的失败计数"，未提及 IP 维度。若 IP 维度的连续失败计数不清除，则对于共享 IP（如企业 NAT、医院内网），一个用户登录成功不会重置该 IP 的失败计数，其他用户从同一 IP 登录会更快被锁定。此外，"连续 20 次失败"的语义需要明确——中途有成功登录时是否视为"断开连续性"重置 IP 维度计数。
- **严重程度**：重要
- **改进建议**：在步骤 8 后补充"同时清除 LoginAttemptTracker 中该请求来源 IP 的失败计数"，或明确 IP 维度采用滑动窗口策略（依赖定时过期而非登录成功重置）。同时在 4.1 节 LoginAttemptTracker 表中补充"登录成功时重置"作为重置时机。

### 🟡 问题 6（重要）：多 Tab 并发刷新失败的连锁错误处理未定义

- **所在位置**：7.4 节"401 静默刷新连续 3 次失败后清除所有认证数据并跳转登录页"和"多 Tab 并发刷新互斥"
- **问题描述**：Promise 单例模式下，如果一次刷新请求失败，所有等待同一 Promise 的并发请求也会被拒绝。文档说"连续 3 次失败后清除"，但对 Promise 单例模式而言，一次失败会传递给所有等待者——这是算作 1 次失败还是 N 次失败（N=等待请求数）？若每个等待请求各自递增失败计数，则一次刷新失败可能导致跳转登录页（N≥3）。若共用一个失败计数器，则需说明计数器放置位置（在 Promise 之外，按时间窗口计算，而非按重试请求计数）。
- **严重程度**：重要
- **改进建议**：补充失败计数策略——明确失败计数器是全局共享（按时间窗口，如 60 秒内单 Promise 失败次数），而非按每个等待请求独立计数。或改为 Promise 链式重试：刷新失败 → 等待指数退避 → 重新发起刷新（使用新 Promise 而非共享单例）。

### 🟡 问题 7（重要）：`expiresIn` 在 TokenRefreshResponse 中的语义未明确，与 LoginResponse 的"从签发时计算"语义形成落差

- **所在位置**：5.2 节 TokenRefreshResponse 定义（无 expiresIn 语义说明）vs 5.2 节 LoginResponse 定义（有语义说明）
- **问题描述**：5.2 节在 LoginResponse 定义前明确说明了 `expiresIn` 的语义为"access token 剩余有效秒数（从签发时计算），供前端预估刷新时机"。但 TokenRefreshResponse 的 `expiresIn` 字段缺少同样的语义说明。TokenRefreshResponse 中的 access token 是新签发的（剩余有效秒数应为 900），但若缺失语义说明，前端实现者可能误以为 expiresIn 是"从当前时刻到过期的时间差"（与"从签发时计算"不同），导致刷新时机计算偏差。
- **严重程度**：重要
- **改进建议**：在 TokenRefreshResponse record 定义前补充与 LoginResponse 一致的 expiresIn 语义说明，明确"access token 剩余有效秒数（从签发时计算），典型值 900"，或在 API 接口清单中添加说明。

### 🟡 问题 8（重要）：GlobalRateLimitFilter 与 InMemoryRateLimitGuard 之间的委托关系未定义

- **所在位置**：4.1 节"全局 IP 限流（GlobalRateLimitFilter）"段
- **问题描述**：4.1 节描述 GlobalRateLimitFilter "检查滑动窗口计数器（100 次/60 秒/IP）"并"与 RateLimitGuard 共用同一声明式接口"，但未说明 GlobalRateLimitFilter 是独立实现滑动窗口还是委托给 InMemoryRateLimitGuard。目录结构（2.1 节）将 GlobalRateLimitFilter 放在 `auth/security/` 包下，InMemoryRateLimitGuard 放在 `auth/rateLimit/` 包下。如果 GlobalRateLimitFilter 自管理滑动窗口，则其限流逻辑与 InMemoryRateLimitGuard 重复（均实现滑动窗口），违反 DRY。如果委托给 InMemoryRateLimitGuard，则需补充构造注入说明。
- **严重程度**：重要
- **改进建议**：明确 GlobalRateLimitFilter 是否委托 InMemoryRateLimitGuard。若委托，补充构造函数注入示例和接口调用说明；若独立实现，说明与 InMemoryRateLimitGuard 的职责分工边界（全局限流 vs 登录限流）并考虑提取公共滑动窗口工具类。

### 🔵 问题 9（一般）：IP 维度 LoginAttemptTracker 的"连续"语义未定义

- **所在位置**：4.1 节 LoginAttemptTracker 表 IP 维度行（"同一 IP，连续 20 次失败，30 分钟"）
- **问题描述**：使用"连续"一词但未定义何种操作会"中断连续"。用户的"登录成功"是否中断 IP 维度的连续失败计数？若登录成功不中断（当前 3.1.1 步骤 8 只清除用户名维度），则该 IP 的失败计数会持续累积，不受用户成功登录的影响。"连续"的定义歧义可能导致实现者按无中断窗口累计（简单计数器）或按有中断窗口（连续失败序列）两种方式实现，结果完全不同。
- **严重程度**：一般
- **改进建议**：在 4.1 节 LoginAttemptTracker 表或下方注释中补充"连续"的定义：任何非登录请求或登录成功均视为中断连续序列。同时明确 IP 维度的计数重置条件。

### 🔵 问题 10（一般）：密码策略的 Phase 1 过渡方案缺少种子数据处理说明

- **所在位置**：4.3 节"修复方案"列表及 8.3 节 A1 行
- **问题描述**：4.3 节定义了新的密码复杂度要求（8-64 字符、3/4 字符种类、不得包含用户名）。但 Phase 1 已有的种子数据（如初始化脚本中的用户密码）可能不满足新策略。文档未说明这些种子数据是否需要更新以符合新策略，也未说明种子数据的密码变更是否触发 `passwordChangeRequired`。8.3 节 A1 将 password NOT NULL 标注为"已完成"——但只检查了 NOT NULL，未涉及密码复杂度。
- **严重程度**：一般
- **改进建议**：在 4.3 节或 8.3 节补充 Phase 1 种子数据密码的迁移策略：确保种子密码满足新复杂度要求（必要时更新种子 SQL），并说明是否需要设置 passwordChangeRequired=true 以强制变更。

### 🔵 问题 11（一般）：登录步骤 5 对无 username 的 IP 维度计数使用了"虚拟哈希"但不占用 key 空间

- **所在位置**：3.1.1 节步骤 5
- **问题描述**：步骤 5 在用户名不存在时递增 IP 维度的失败计数，但说明是"因无有效用户名作为 key"。该 IP 维度计数与步骤 7（密码错误）的 IP 维度计数是否共用同一个 IP 维度计数器？若共用，则攻击者对不存在用户名的暴力攻击会同时增加步骤 5（无效用户）和步骤 7（有效用户但密码错误）的 IP 失败计数，导致有效用户被误锁定的风险升高。文档未说明步骤 5 和步骤 7 的 IP 维度计数关系。
- **严重程度**：一般
- **改进建议**：文档补充说明步骤 5 的 IP 维度计数与步骤 6/7 的 IP 维度是共用计数器还是独立计数器。若共用，在 4.1 节登录失败计数表中明确"用户名维度按用户名独立计数，IP 维度全局共享计数"。

---

## 整体评价

产出经过 9 轮迭代（含 7 轮审议修正），整体质量较高，核心认证逻辑、安全设计、协作契约已较为完善。但本轮审查发现 3 个严重问题（语义矛盾、不可达代码、闭环断裂）、5 个重要问题和 3 个一般问题，主要集中在三个方面：（1）密码变更后 token 状态管理存在自相矛盾，（2）tokenVersion 闭环在登录流程中存在关键步骤遗漏，（3）Phase 1/Phase 2 边界管理不严谨导致死代码。建议修复者优先处理问题 1-3，解决语义矛盾后补全登录流程的 tokenVersion 说明。

---

```
DIAG_WRITTEN:C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606252256_phase1_ABD_ood\b_v8_diag_v1.md
主Agent请勿阅读产出文件内容，直接将路径转发给相关方。
```
