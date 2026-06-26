根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

以下问题由组件B在上一轮诊断中发现，质询报告结论为 LOCATED（全部确认）：

1. **【重要】Role.sort 字段缺少 Java 默认值与 NOT NULL 约束，JPA 持久化存在不一致风险**  
   - 位置：5.1 节 Role.java 变更表；4.3 节 NOT NULL 约束状态确认表  
   - 改进建议：在 Role.java 变更表中补充 `@Column(nullable=false) private Integer sort = 0;`，DDL 中 `sys_role.sort INT NOT NULL DEFAULT 0`

2. **【重要】SlidingWindowCounter 声明为"包级私有"但被跨包复用，存在可见性矛盾**  
   - 位置：4.1 节「SlidingWindowCounter 契约」段  
   - 改进建议：三选一：(a) 提升为 public 类放在共享包；(b) 将两限流器归入同一包；(c) 各自独立实现

3. **【重要】密码变更流程中清除 SecurityContext 操作无实际安全效果，说明不准确**  
   - 位置：3.1.6 节步骤 10  
   - 改进建议：二选一：(a) 删除步骤 10；(b) 修正说明为仅清除当前请求上下文，不影响后续请求

4. **【重要】PasswordPolicy 接口缺少方法签名定义，无法直接指导编码**  
   - 位置：1.3 节核心抽象一览；4.3 节密码策略  
   - 改进建议：在 1.3 节或 4.3 节补充 `ErrorCode validate(String password, String username)` 方法签名

5. **【中等】菜单删除操作未定义子菜单处理策略（级联/拦截/置空）**  
   - 位置：6.1 节接口清单，DELETE /api/menu/{id}  
   - 改进建议：有子菜单时阻止删除返回 400 + CHILDREN_EXIST 错误码

6. **【中等】expiresIn 字段语义自相矛盾——"剩余有效秒数"与"从签发时计算"冲突**  
   - 位置：5.2 节 LoginResponse 及 TokenRefreshResponse 定义  
   - 改进建议：统一语义为固定 TTL 或真实剩余时间并修正描述

7. **【中等】`/api/auth/refresh` 端点调用建议未文档化——推荐不携带 Authorization header**  
   - 位置：3.1.3 节 Token 刷新流程；7.2 节包 B → 包 D 契约  
   - 改进建议：在 7.2 节和 3.1.3 节补充不携带 Authorization header 的约束说明

8. **【轻微】刷新端点未加入 PasswordChangeCheckFilter 白名单，存在理论防护盲区**  
   - 位置：3.3 节 PasswordChangeCheckFilter 行为契约  
   - 改进建议：将 `/api/auth/refresh`（POST）加入 PasswordChangeCheckFilter 白名单

## 历史迭代回顾

### 已解决的问题（出现在历史反馈但当前反馈中不再提及）

以下问题已在第 1-12 轮迭代中逐步修复，当前诊断中不再出现：
- **第 1-3 轮**：实体变更表当前状态描述与代码不符、密码策略过渡方案缺失、DTO 定义遗漏、令牌黑名单内存估算、前端接口字段兼容性、Breaking Change 声明、Filter 跨模块迁移、错误消息差异化用户名可枚举、Refresh Token 轮换安全补偿逻辑矛盾、passwordChangeRequired 访问控制缺失、ACCOUNT_DISABLED 与 LOGIN_FAILED 错误码体系、主角色判定策略、JWT 密钥配置约束、CORS 生产安全、passwordChangeRequired 抽离独立 Filter、登录流程步骤 5/6/7 失败计数维度
- **第 4-5 轮**：ACCOUNT_DISABLED 触发路径、PasswordChangeCheckFilter 冗余查询、Token 刷新响应 Breaking Change、MenuResponse 定义缺失、GlobalRateLimitFilter 实现机制、登录步骤 5 时序侧信道、密码变更 tokenVersion 撤销机制、密码变更后前端恢复流程、菜单递归展平路由冲突、PASSWORD_CHANGE_REQUIRED 前端处理
- **第 6-7 轮**：Refresh Token claims 遗漏 tokenVersion、缺陷追踪表条目遗漏、TOKEN_REFRESH_FAILED 错误码、AuthenticationEntryPoint 行为契约、nickname NOT NULL 描述不一致、登录步骤 6 时序侧信道、异常刷新检测可落地实现、H6 循环依赖修复方案、管理员标记密码过期 API 决策、ProfileUpdateRequest 校验、MenuUpdateRequest PUT 语义、登出 token 过期场景、LoginAttemptCleaner 行为说明
- **第 8-10 轮**：密码变更语义自相矛盾、PASSWORD_COMMON 死分支、tokenVersion 闭环断裂、登录步骤 8 IP 维度失败计数、速率限制表头错误、登出端点 @RequestBody(required=false)、密码变更前端恢复流程异常场景、8.3 节格式不一致
- **第 11 轮**：登录锁定返回行为、IP 维度锁定检查、M17/M18/M19 追踪条目

### 持续存在的问题（在多轮反馈中反复出现，需重点解决）

当前诊断中的 8 个问题全部为持续性问题（均在第 12 轮诊断中已出现且本轮仍未修复）：

| 问题 | 首次出现轮次 | 严重度变化 | 说明 |
|------|------------|-----------|------|
| Role.sort 缺少默认值/NOT NULL | 第 12 轮 | 一般→重要 | 严重度升级，因其影响 JPA 持久化一致性 |
| SlidingWindowCounter 包可见性 | 第 10 轮（隐含）→第 12 轮明确 | 严重→重要 | 严重度合理下调但仍需解决 |
| SecurityContext 清除描述不准确 | 第 9 轮→第 12 轮 | 一般→重要 | 反复讨论仍未准确描述或删除 |
| PasswordPolicy 缺少方法签名 | 第 12 轮 | 一般→重要 | 严重度升级，OOD 核心抽象必须定义 |
| 菜单删除子菜单策略 | 第 12 轮 | 一般→中等 | — |
| expiresIn 语义矛盾 | 第 8 轮→第 12 轮 | 一般→中等 | 历经多轮仍未统一 |
| refresh Authorization header | 第 12 轮 | 一般→中等 | — |
| 刷新端点白名单 | 第 12 轮 | 轻微 | — |

**重点关注**：问题 1-4 严重度已升级为"重要"，表明这些设计疏漏在多轮迭代后仍未得到有效修复，影响文档的编码可指导性和设计准确性。

### 新发现的问题

无。本轮 8 个问题均已在第 12 轮诊断中提出。

## 上一轮产出路径
c:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606252256_phase1_ABD_ood/a_v12_copy_from_v11.md

## 用户需求
c:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606252256_phase1_ABD_ood/requirement.md
