# 再审议判定报告（v13）

## 判定结果

RETRY

## 判定理由

诊断报告识别出 1 个严重问题（Refresh 端点可绕过 passwordChangeRequired 强制约束）和 4 个重要问题（白名单内部矛盾、接口签名缺失、迁移影响评估不完整、限流器描述模糊），质询报告确认 LOCATED 且全部质询维度通过。组件B内部循环实际轮次（1）未达到最大轮次（12），且存在严重及重要等级问题，根据判定标准应重新运行组件A。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：Refresh 端点可绕过 passwordChangeRequired 强制约束，用户登录后即使不修改密码，也可通过 POST /api/auth/refresh 反复获取新令牌，维持认证状态
- **所在位置**：3.1.3 节 Token 刷新流程；3.3 节 PasswordChangeCheckFilter 白名单；3.4 节 passwordChangeRequired 访问控制
- **严重程度**：严重
- **改进建议**：在 AuthServiceImpl.refresh() 中增加 passwordChangeRequired 检查；若用户在 DB 中 passwordChangeRequired=true，拒绝刷新并返回 PASSWORD_CHANGE_REQUIRED（403）

- **问题描述**：3.4 节白名单与 3.3 节/3.1.2 节白名单不一致，3.4 节未随 v13 修订同步更新
- **所在位置**：3.4 节 passwordChangeRequired 访问控制（白名单列表）
- **严重程度**：重要
- **改进建议**：将 3.4 节白名单补充 POST /api/auth/refresh 条目，与 3.1.2 节和 3.3 节保持一致

- **问题描述**：PasswordChangeService 和 CurrentUser 接口缺少方法签名，无法直接指导编码实现
- **所在位置**：1.3 节核心抽象一览；3.4 节
- **严重程度**：重要
- **改进建议**：补充 PasswordChangeService 接口方法签名（isChangeRequired/markChangeRequired/clearChangeRequired）和 CurrentUser 接口方法签名（getUserId/getUsername/getUserType）

- **问题描述**：包 A 实体移至 common-module-impl 的结构变更影响评估不完整，未评估现有业务模块对包 A 实体的直接引用，未说明业务模块获取用户数据的途径
- **所在位置**：2.1 节、2.2 节、7.1 节、7.2 节
- **严重程度**：重要
- **改进建议**：补充评估现有代码中对 User/Role/Post/PermissionFunction 实体的外部引用；明确业务模块访问用户数据的途径；补充迁移步骤

- **问题描述**：InMemoryRateLimitGuard 和 GlobalRateLimitFilter 均描述为使用 ConcurrentHashMap<String, SlidingWindowCounter>，表述雷同，开发者可能误以为两者共享同一个计数器实例
- **所在位置**：4.1 节 InMemoryRateLimitGuard 和 GlobalRateLimitFilter 实现描述
- **严重程度**：重要
- **改进建议**：明确注释两套计数器实例相互独立，使用不同的 key 空间和窗口参数
