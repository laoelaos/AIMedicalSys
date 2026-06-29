# 再审议判定报告（v4）

## 判定结果

RETRY

## 判定理由

组件B诊断报告检出 **1 处严重问题（P1）**、**7 处重要问题（P2-P8）**、**2 处一般问题（P9-P10）**。质询报告结果为 LOCATED，确认所有问题均有据可查、逻辑自洽。审查报告含有严重等级事实错误，不符合 PASS 条件，判定重新运行组件A。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：角色优先级依赖不存在的字段。Role.java 实体无 `level`/`sort`/`order`/`priority` 字段，主导色判定策略无法落地。
- **所在位置**：5.2 节 UserInfoResponse 字段映射说明（第 650 行）、7.3 节 UserType→role 行（第 841 行）
- **严重程度**：严重
- **改进建议**：在 Role 实体中新增 `sort` 字段并同步 DDL；或改用角色 `code`/`id` 隐式排序；或删除"主角色"概念，取第一个关联角色。选定方案后更新设计文档。

- **问题描述**：`ACCOUNT_DISABLED` 定义了但从未被触发。JwtAuthenticationFilter 对禁用用户仅放行而非返回禁用错误码。
- **所在位置**：10.2 节 ErrorCode 表 vs 3.3 节 JwtAuthenticationFilter 行为契约
- **严重程度**：一般
- **改进建议**：在 Filter 中抛出 `AuthenticationException`（含 `ACCOUNT_DISABLED`）；或删除 `ACCOUNT_DISABLED` 统一返回 `UNAUTHORIZED`。

- **问题描述**：`PasswordChangeCheckFilter` 冗余查询用户。JwtAuthenticationFilter 已加载用户，后续 Filter 重复查库。
- **所在位置**：3.3 节两 Filter 行为契约
- **严重程度**：一般
- **改进建议**：JwtAuthenticationFilter 将 `passwordChangeRequired` 写入 request attribute，PasswordChangeCheckFilter 直接读取。

- **问题描述**：Token 刷新响应变更未声明 Breaking Change。响应字段从 `token`/`user` 变为 `accessToken`/`refreshToken`（不含 `user`）。
- **所在位置**：6.4 节 Breaking Change 声明表
- **严重程度**：一般
- **改进建议**：在 6.4 节新增刷新端点 Breaking Change 条目。

- **问题描述**：`MenuResponse` 被引用但从未在 5.2 节定义。
- **所在位置**：2.1 节目录结构、7.2 节包 B→包 D 契约
- **严重程度**：一般
- **改进建议**：在 5.2 节新增 `MenuResponse` record，字段与前端 `MenuItem` 接口对齐。

- **问题描述**：全局 IP 频率限制缺少实现机制。仅有定义无 Filter/拦截器实现。
- **所在位置**：4.1 节速率限制表
- **严重程度**：一般
- **改进建议**：新增 `GlobalRateLimitFilter` 注册到 Filter 链；或明确推迟到 Phase 2 并从当前设计删除。

- **问题描述**：登录流程步骤 5 未指定失败计数维度。用户名不存在时无法确定递增哪个维度。
- **所在位置**：3.1.1 节步骤 5
- **严重程度**：一般
- **改进建议**：明确步骤 5 递增 IP 维度计数，步骤 6（用户禁用）递增用户名和 IP 双维度。

- **问题描述**：`MenuUpdateRequest.id` 与路径 `{id}` 关系不明确。
- **所在位置**：5.2 节 MenuUpdateRequest、6.1 节 PUT /api/menu/{id}
- **严重程度**：一般
- **改进建议**：说明 `id` 是冗余字段（删除）或一致性校验用途（加 `@NotNull` 并说明校验规则）。

- **问题描述**：`PasswordChangeCheckFilter` 的 403 消息与 ErrorCode 不一致。
- **所在位置**：3.3 节 vs 10.2 节 ErrorCode 表
- **严重程度**：轻微
- **改进建议**：统一消息文本或新增专用 ErrorCode。

- **问题描述**：刷新端点未定义成功后前端获取用户信息的流程。
- **所在位置**：3.1.3 节 Token 刷新流程、7.4 节前端补偿机制
- **严重程度**：轻微
- **改进建议**：补充说明刷新后调用 `GET /api/auth/me` 更新本地用户信息。
