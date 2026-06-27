根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 🔴 问题 1（严重）：密码变更后"强制用户重新登录"语义与设计说明自相矛盾
**所在位置**：3.1.6 节步骤 10 vs 3.1.6 节设计说明第二段
**改进建议**：二选一统一语义。推荐删除步骤 10 的"强制用户重新登录"表述，补充为"清除当前请求 SecurityContext（作为安全最佳实践），客户端可继续使用旧 Access Token 至其自然过期（最长 15 分钟）"。

### 🔴 问题 2（严重）：PASSWORD_COMMON ErrorCode 在 Phase 1 中不可达，形成代码死分支
**所在位置**：4.3 节密码策略表 vs 10.2 节 ErrorCode 表 PASSWORD_COMMON 行
**改进建议**：从 10.2 节和 10.1 节中删除 PASSWORD_COMMON 条目（Phase 1 范围），或在保留时明确标注"Phase 2 启用，Phase 1 中不可达"。

### 🔴 问题 3（严重）：登录流程（3.1.1 步骤 10）未从 User 实体获取 tokenVersion 用于 Refresh Token 签发，导致 tokenVersion 闭环断裂
**所在位置**：3.1.1 节步骤 10 vs 3.2 节 Refresh Token Claims（含 tokenVersion）vs 3.1.3 节步骤 7（验证 tokenVersion）
**改进建议**：在 3.1.1 节步骤 10 中补充"JwtTokenProvider 从 `User.tokenVersion` 读取当前版本号，嵌入 Refresh Token claims 中的 `tokenVersion` 字段"。同时建议在 JwtTokenProvider 生成 Refresh Token 的方法签名中明确 `tokenVersion` 入参。

### 🟡 问题 4（重要）：展平路由 name 唯一性保证策略存在运行时竞态隐患
**所在位置**：8.2 节 H5 修复方案"name 唯一性保证策略"段
**改进建议**：改为确定性策略——在 `convertMenusToRoutes` 的递归遍历过程中，使用 `permissionCode` 映射为唯一 name 时，直接通过 `/` 转驼峰 + 添加前端应用前缀（如 `Doctor_SystemUser` 或 `Admin_SystemUser`）消除跨权限冲突，避免运行时按需修正。若仍保留运行时检测，须补充说明"检测到冲突时，拒绝注册而非覆盖"。

### 🟡 问题 5（重要）：登录成功（3.1.1 步骤 8）仅清除用户名维度的失败计数，IP 维度未处理
**所在位置**：3.1.1 节步骤 8 vs 4.1 节 LoginAttemptTracker 双维度设计
**改进建议**：在步骤 8 后补充"同时清除 LoginAttemptTracker 中该请求来源 IP 的失败计数"，或明确 IP 维度采用滑动窗口策略（依赖定时过期而非登录成功重置）。同时在 4.1 节 LoginAttemptTracker 表中补充"登录成功时重置"作为重置时机。

### 🟡 问题 6（重要）：多 Tab 并发刷新失败的连锁错误处理未定义
**所在位置**：7.4 节"401 静默刷新连续 3 次失败后清除所有认证数据并跳转登录页"和"多 Tab 并发刷新互斥"
**改进建议**：补充失败计数策略——明确失败计数器是全局共享（按时间窗口，如 60 秒内单 Promise 失败次数），而非按每个等待请求独立计数。或改为 Promise 链式重试。

### 🟡 问题 7（重要）：`expiresIn` 在 TokenRefreshResponse 中的语义未明确，与 LoginResponse 的"从签发时计算"语义形成落差
**所在位置**：5.2 节 TokenRefreshResponse 定义（无 expiresIn 语义说明）vs 5.2 节 LoginResponse 定义（有语义说明）
**改进建议**：在 TokenRefreshResponse record 定义前补充与 LoginResponse 一致的 expiresIn 语义说明，或在 API 接口清单中添加说明。

### 🟡 问题 8（重要）：GlobalRateLimitFilter 与 InMemoryRateLimitGuard 之间的委托关系未定义
**所在位置**：4.1 节"全局 IP 限流（GlobalRateLimitFilter）"段
**改进建议**：明确 GlobalRateLimitFilter 是否委托 InMemoryRateLimitGuard。若委托，补充构造函数注入示例和接口调用说明；若独立实现，说明与 InMemoryRateLimitGuard 的职责分工边界并考虑提取公共滑动窗口工具类。

### 🔵 问题 9（一般）：IP 维度 LoginAttemptTracker 的"连续"语义未定义
**所在位置**：4.1 节 LoginAttemptTracker 表 IP 维度行（"同一 IP，连续 20 次失败，30 分钟"）
**改进建议**：在 4.1 节 LoginAttemptTracker 表或下方注释中补充"连续"的定义：任何非登录请求或登录成功均视为中断连续序列。同时明确 IP 维度的计数重置条件。

### 🔵 问题 10（一般）：密码策略的 Phase 1 过渡方案缺少种子数据处理说明
**所在位置**：4.3 节"修复方案"列表及 8.3 节 A1 行
**改进建议**：在 4.3 节或 8.3 节补充 Phase 1 种子数据密码的迁移策略：确保种子密码满足新复杂度要求（必要时更新种子 SQL），并说明是否需要设置 passwordChangeRequired=true 以强制变更。

### 🔵 问题 11（一般）：登录步骤 5 对无 username 的 IP 维度计数使用了"虚拟哈希"但不占用 key 空间
**所在位置**：3.1.1 节步骤 5
**改进建议**：文档补充说明步骤 5 的 IP 维度计数与步骤 6/7 的 IP 维度是共用计数器还是独立计数器。若共用，在 4.1 节登录失败计数表中明确"用户名维度按用户名独立计数，IP 维度全局共享计数"。

## 历史迭代回顾

### 已解决的问题（出现在历史反馈但当前反馈中不再提及的问题）
以下问题在之前的迭代中已被修复，当前诊断报告中不再出现：
- 第 7 轮问题 1：异常刷新检测机制缺少实现细节（已补充完整设计契约）
- 第 7 轮问题 2：H6 `inject('router')` 在 Pinia store 中不可用（已引用现有工厂模式）
- 第 7 轮问题 3：管理员标记密码过期的 API 端点缺失（已标注属于管理端设计范围）
- 第 7 轮问题 4：ProfileUpdateRequest.nickname 缺少空白字符串校验（已补充 `@NotBlank`）
- 第 7 轮问题 5：MenuUpdateRequest PUT 语义不明确（已明确为局部更新语义）
- 第 7 轮问题 6：登出端点在 token 过期场景下的行为未定义（已补充 finally 块处理）
- 第 7 轮问题 7：LoginAttemptCleaner 无行为说明（已从目录结构删除）
- 第 1-6 轮中的大部分问题也已解决（密码约束、DTO 定义、黑名单内存论证、UserInfoResponse 字段对齐、deleted 列描述修正、安全补偿逻辑修正、统一错误消息等）

### 持续存在的问题（在多轮反馈中反复出现的问题，需重点解决）
以下 8 个问题自第 8 轮即已识别，在当前诊断报告中再次出现，是 v9 迭代中需优先处理的核心问题：
1. **语义矛盾**（🔴 #1）：密码变更后"强制重新登录"语义与设计说明矛盾
2. **不可达代码**（🔴 #2）：PASSWORD_COMMON ErrorCode 在 Phase 1 不可达
3. **闭环断裂**（🔴 #3）：登录流程缺少 tokenVersion 嵌入环节
4. **路由竞态**（🟡 #4）：展平路由 name 唯一性保证存在运行时竞态隐患
5. **IP 维度过期未重置**（🟡 #5）：登录成功 IP 失败计数未清除
6. **并发刷新失败计数**（🟡 #6）：多 Tab 并发刷新失败的连锁错误处理未定义
7. **expiresIn 语义落差**（🟡 #7）：TokenRefreshResponse 缺少 expiresIn 语义说明
8. **委托关系缺失**（🟡 #8）：GlobalRateLimitFilter 与 InMemoryRateLimitGuard 关系未定义

### 新发现的问题（本轮新识别的问题）
以下 3 个问题为第 9 轮诊断报告中新发现：
1. **"连续"语义歧义**（🔵 #9）：IP 维度 LoginAttemptTracker 的"连续"未定义中断条件
2. **种子数据迁移遗漏**（🔵 #10）：密码策略过渡方案未涉及种子数据处理
3. **IP 维度计数器关系未定义**（🔵 #11）：步骤 5 与步骤 7 的 IP 维度计数关系模糊

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606252256_phase1_ABD_ood\a_v8_copy_from_v7.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606252256_phase1_ABD_ood\requirement.md
