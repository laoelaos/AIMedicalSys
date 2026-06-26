# 再审议判定报告（v8）

## 判定结果

RETRY

## 判定理由

组件B诊断报告经质询确认为 LOCATED，报告中包含 3 个严重问题（语义矛盾、不可达代码、tokenVersion 闭环断裂）和 5 个重要问题，满足"审查报告包含严重或一般等级的问题"的 RETRY 条件。组件B实际轮次（1）远少于最大轮次（12），且质询已确认诊断有效，应退回组件A修复。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：密码变更后"强制用户重新登录"语义与设计说明自相矛盾
- **所在位置**：3.1.6 节步骤 10 vs 3.1.6 节设计说明第二段
- **严重程度**：严重
- **改进建议**：二选一统一语义：要么删除"强制用户重新登录"表述并补充说明旧 token 可继续使用至自然过期（推荐），要么真正实现强制失效并更新设计说明

- **问题描述**：PASSWORD_COMMON ErrorCode 在 Phase 1 中不可达，形成代码死分支
- **所在位置**：4.3 节密码策略表 vs 10.2 节 ErrorCode 表 PASSWORD_COMMON 行
- **严重程度**：严重
- **改进建议**：从 10.2/10.1 节删除 PASSWORD_COMMON 条目，或在保留时明确标注"Phase 2 启用"

- **问题描述**：登录流程未从 User 实体获取 tokenVersion 用于 Refresh Token 签发，导致闭环断裂
- **所在位置**：3.1.1 节步骤 10 vs 3.2 节 Refresh Token Claims（含 tokenVersion）
- **严重程度**：严重
- **改进建议**：补充 JwtTokenProvider 从 User.tokenVersion 读取并嵌入 Refresh Token claims 的说明

- **问题描述**：展平路由 name 唯一性保证策略存在运行时竞态隐患
- **所在位置**：8.2 节"name 唯一性保证策略"段
- **严重程度**：一般
- **改进建议**：改为确定性命名策略（permissionCode 转驼峰 + 前端应用前缀），或在保留运行时检测时补充冲突拒绝注册及 Fallback 行为

- **问题描述**：登录成功后仅清除用户名维度的失败计数，IP 维度未处理
- **所在位置**：3.1.1 节步骤 8 vs 4.1 节 LoginAttemptTracker 双维度设计
- **严重程度**：一般
- **改进建议**：补充 IP 维度失败计数重置逻辑，或明确采用滑动窗口策略

- **问题描述**：多 Tab 并发刷新失败的连锁错误处理未定义
- **所在位置**：7.4 节"401 静默刷新连续 3 次失败"相关描述
- **严重程度**：一般
- **改进建议**：明确失败计数器为全局共享（按时间窗口），或改为 Promise 链式重试

- **问题描述**：expiresIn 在 TokenRefreshResponse 中语义未明确，与 LoginResponse 语义落差
- **所在位置**：5.2 节 TokenRefreshResponse 定义
- **严重程度**：一般
- **改进建议**：补充与 LoginResponse 一致的 expiresIn 语义说明

- **问题描述**：GlobalRateLimitFilter 与 InMemoryRateLimitGuard 之间的委托关系未定义
- **所在位置**：4.1 节"全局 IP 限流（GlobalRateLimitFilter）"段
- **严重程度**：一般
- **改进建议**：明确委托关系，补充构造注入示例或职责分工边界说明
