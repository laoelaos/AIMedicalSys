# 再审议判定报告（v12）

## 判定结果

RETRY

## 判定理由

组件B诊断报告（第12轮）识别出8个问题，其中Issue 1~4为"重要"等级、Issue 5~7为"中等"等级，对应判定框架中的"一般/严重"等级。质询报告结果为LOCATED，确认全部问题均有充分证据且逻辑自洽。最大轮次12、实际轮次1，说明审查在首轮即被确认有效。由于审查报告包含一般及以上等级问题，满足RETRY条件。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：Role.sort 字段缺少 Java 默认值与 NOT NULL 约束，JPA 持久化存在不一致风险
- **所在位置**：5.1 节 Role.java 变更表；4.3 节 NOT NULL 约束状态确认表
- **严重程度**：一般
- **改进建议**：在 Role.java 变更表中补充 `@Column(nullable=false) private Integer sort = 0;`，DDL 中 `sys_role.sort INT NOT NULL DEFAULT 0`

- **问题描述**：SlidingWindowCounter 声明为"包级私有"但被跨包复用，存在可见性矛盾
- **所在位置**：4.1 节「SlidingWindowCounter 契约」段
- **严重程度**：严重
- **改进建议**：将 SlidingWindowCounter 提升为 public 类放在共享包，或将两限流器归入同一包，或各自独立实现

- **问题描述**：密码变更流程中清除 SecurityContext 操作无实际安全效果，说明不准确
- **所在位置**：3.1.6 节步骤 10
- **严重程度**：一般
- **改进建议**：删除该步骤或将说明修正为仅清除当前请求上下文，不影响后续请求

- **问题描述**：PasswordPolicy 接口缺少方法签名定义，无法直接指导编码
- **所在位置**：1.3 节核心抽象一览；4.3 节密码策略
- **严重程度**：一般
- **改进建议**：在 1.3 节或 4.3 节补充 `ErrorCode validate(String password, String username)` 方法签名

- **问题描述**：菜单删除操作未定义子菜单处理策略（级联/拦截/置空）
- **所在位置**：6.1 节接口清单，DELETE /api/menu/{id}
- **严重程度**：一般
- **改进建议**：有子菜单时阻止删除返回 400 + CHILDREN_EXIST 错误码，要求先删除子菜单

- **问题描述**：`expiresIn` 字段语义自相矛盾——"剩余有效秒数"与"从签发时计算"冲突
- **所在位置**：5.2 节 LoginResponse 及 TokenRefreshResponse 定义
- **严重程度**：一般
- **改进建议**：统一语义为固定 TTL 或真实剩余时间并修正描述

- **问题描述**：`/api/auth/refresh` 端点调用建议未文档化——推荐不携带 Authorization header
- **所在位置**：3.1.3 节 Token 刷新流程；7.2 节包 B → 包 D 契约
- **严重程度**：一般
- **改进建议**：在 7.2 节和 3.1.3 节补充不携带 Authorization header 的约束说明

- **问题描述**：刷新端点未加入 PasswordChangeCheckFilter 白名单，存在理论防护盲区
- **所在位置**：3.3 节 PasswordChangeCheckFilter 行为契约
- **严重程度**：轻微
- **改进建议**：将 /api/auth/refresh（POST）加入 PasswordChangeCheckFilter 白名单
