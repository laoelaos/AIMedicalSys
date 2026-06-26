根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

以下问题从组件B第6轮诊断报告中提取（质询报告确认结论：LOCATED）：

### 🔴 严重

1. **3.2 节 Refresh Token Claims 示例缺少 `tokenVersion` 字段**
   - 位置：3.2 节「Refresh Token」Claims 结构 JSON 示例
   - 改进建议：在 Refresh Token Claims 示例中补充 `"tokenVersion": 0` 字段，并说明其作用

2. **8.1 节缺陷追踪表遗漏 4 条原始问题条目**
   - 位置：8.1 节包 B 后端问题追踪表
   - 改进建议：在 8.1 节补充 M1（buildUserInfoResponse 私有方法不可测试）、M2（updateProfile() 多余 save() 写回）、M5（医生岗位取 iterator().next()）、M8（Function.java 类名冲突）四条目的追踪行，包含当前状态、修复方案、潜在副作用、影响范围四列。M2 具体方案："利用 JPA 脏检查机制，仅 set 变更字段后由 Hibernate 自动 flush，移除显式 save() 调用"；M5 具体方案："将 `iterator().next()` 抽取为 `getFirstPost()` 工具方法，使用 `new ArrayList<>(posts).get(0)` 并判空"

### 🟡 重要

3. **3.1.3 节步骤 6 错误使用 `LOGIN_FAILED` 作为刷新失败错误码**
   - 位置：3.1.3 节步骤 6
   - 改进建议：将步骤 6 的 `LOGIN_FAILED` 改为 `TOKEN_REFRESH_FAILED`

4. **3.3 节 AuthenticationEntryPoint 描述与 10.2 节 ACCOUNT_DISABLED 的矛盾**
   - 位置：3.3 节「SecurityFilterChain」exceptionHandling；10.2 节 ACCOUNT_DISABLED
   - 改进建议：补充 EntryPoint 行为契约：携带 ACCOUNT_DISABLED 时返回 `Result.fail("ACCOUNT_DISABLED", "账户已被管理员停用")`；其余返回 `Result.fail("UNAUTHORIZED", "未认证或令牌已失效")`

5. **4.3 节与 5.1 节对 `nickname NOT NULL` 变更的描述不一致**
   - 位置：4.3 节「修复方案」列表 vs 5.1 节 User.java 变更表
   - 改进建议：在 5.1 节 User.java 变更表中新增 `nickname NOT NULL` 行

6. **8.2 节 H5 展平路由策略缺少 Layout 包容机制说明**
   - 位置：8.2 节 H5「修复方案」列
   - 改进建议：补充 Layout 包容机制的设计决策：Layout 在 App.vue 中包裹 `<router-view>`，或展平路由作为 Layout 子路由注册

7. **登录流程步骤 6 存在时序侧信道缺口**
   - 位置：3.1.1 节步骤 6
   - 改进建议：在步骤 6 中增加 dummy BCrypt 比对，消除与步骤 5/7 的时序差异；或在设计决策中明确承认此残余风险（已禁用账号枚举攻击面较小），并记录计划在 Phase 2 解决

8. **`LoginResponse` 中 `tokenType` 与 `expiresIn` 的定位不明确**
   - 位置：5.2 节 LoginResponse、TokenRefreshResponse；6.2 节 JSON 示例
   - 改进建议：在 5.2 节 LoginResponse 定义下方补充注释，明确 `expiresIn` 为"access token 剩余有效秒数（从签发时计算）"，供前端用于预估下一次刷新时机。同步更新 6.2 节 JSON 示例字段注释

### 🔵 一般

9. **5.1 节 `User.tokenVersion` 缺少 `@Column(nullable=false)` 注解**
   - 位置：5.1 节 User.java 变更表「tokenVersion 新增字段」行
   - 改进建议：补加 `@Column(nullable=false)`

10. **密码变更 API 流程缺少独立完整描述**
    - 位置：3.4 节「密码变更强制策略」、7.4 节
    - 改进建议：新增 3.1.6 节「密码变更流程」，将服务端处理逻辑整理为结构化步骤描述

11. **10.1 节错误分类表未覆盖全部 ErrorCode 的 HTTP 状态映射**
    - 位置：10.1 节「错误分类」表；10.2 节未覆盖 ErrorCode（ACCOUNT_LOCKED、TOKEN_REFRESH_FAILED 等）
    - 改进建议：在 10.1 节补充缺失 ErrorCode 的分类映射

12. **`PUT /api/auth/password` 成功后 `tokenVersion` 递增对已登录 Access Token 的影响未说明**
    - 位置：3.4 节「密码变更强制策略」、4.2 节 tokenVersion 描述
    - 改进建议：在 3.4 节或 4.2 节补充说明：Access Token 不检查 tokenVersion 是已知设计决策（短有效期 + 查库验证 enabled 已提供基本保护），非疏忽

13. **`<script setup>` 或 Options API 使用约定未说明**
    - 位置：8.2 节 H6 修复方案
    - 改进建议：在 8.2 节开头补充前端代码风格约定说明，或在 H6 修复方案中将 `getCurrentInstance()` 示例替换为更适合项目的注入模式

## 历史迭代回顾

### 已解决的问题
以下问题出现在前序历史迭代反馈中，当前诊断报告不再提及，表明已在 v6 产出中修复：
- 密码变更策略过渡方案（v1）、passwordChangeRequest DTO 缺失（v1）、Refresh Token 请求体未定义（v1）
- 内存黑名单可行性论证（v1）、UserInfoResponse 字段未对齐（v1）、Breaking Change 未声明（v1/v4）
- JwtAuthenticationFilter 迁移方案（v1）、Section 8.3 代码状态未复核（v1）、SidebarBase props 未定义（v1）
- deleted 列状态错误（v2）、Refresh Token 安全补偿逻辑矛盾（v2/v3/v4）
- passwordChangeRequired 访问控制缺失（v2）、Section 4.3 与 5.1 完成状态矛盾（v2）
- LoginResponse 缺少 userId/username（v2）、菜单 CRUD DTO 缺失（v2）
- 登录错误消息差异化可枚举（v2）、多 Tab 并发刷新竞态（v2）
- 主角色判定策略未定义（v2/v5）、SecurityConfig 注入旧 Filter（v2）
- Refresh Token 旧 token 不可重复使用错误声明（v3）、passwordChangeRequired 未入实体变更表（v3）
- LOGIN_FAILED ErrorCode 缺失（v3）、LoginResponse.user 可空性不一致（v3）
- primaryRole 字段引用错误（v3/v4）、限流并发方案未决策（v3）
- JWT_SECRET 配置约束缺失（v3）、CORS 生产安全（v3）
- passwordChangeRequired 控制违反单一职责（v3）、登出请求体不一致（v3）
- 角色优先级依赖不存在字段（v4）、ACCOUNT_DISABLED 不可达（v4）
- PasswordChangeCheckFilter 冗余查询（v4）、MenuResponse 缺失（v4）
- 全局 IP 限流未实现（v4）、登录步骤 5 失败计数维度不明确（v4）
- 步骤 7 密码不匹配未指定错误消息（v3）、分页兼容承诺（v3）
- H6 循环依赖修复模糊（v3）、User 查询优化不一致（v3）
- 时序侧信道步骤 5（v5）、tokenVersion 撤销机制（v5）
- 密码变更前端恢复流程（v5）、菜单递归展平方案冲突（v5）
- PASSWORD_CHANGE_REQUIRED 前端处理（v5）、JwtTokenProvider Bean 注册方式（v5）

### 持续存在的问题（需重点解决）
以下问题在多轮反馈中反复出现，当前 v6 诊断报告仍检出：
1. **3.2 节 Refresh Token Claims 示例缺少 tokenVersion** — 自 v6 第一轮起即被检出，至今未修复
2. **8.1 节缺陷追踪表遗漏原始问题条目** — 自 v6 第一轮起即被检出，v6 修订后仍未完全修复（M1/M2/M5/M8 仍缺失）
3. **3.1.3 节步骤 6 LOGIN_FAILED 错误码** — 自 v6 第一轮起即被检出，未修复
4. **3.3 节 AuthenticationEntryPoint 矛盾** — 自 v6 第一轮起即被检出，未修复
5. **4.3 节与 5.1 节 nickname 不一致** — 自 v6 第一轮起即被检出，未修复
6. **8.2 节 H5 Layout 包容机制** — 自 v6 第一轮起即被检出，未修复
7. **登录流程步骤 6 时序侧信道** — 自 v6 第一轮起即被检出，未修复
8. **LoginResponse tokenType/expiresIn 语义** — 自 v6 第一轮起即被检出，未修复

### 新发现的问题
以下问题在本轮诊断中首次识别：
9. **`PUT /api/auth/password` 成功后 tokenVersion 递增对已登录 Access Token 的影响未说明**（问题 12）
10. **`<script setup>` 或 Options API 使用约定未说明**（问题 13）

## 上一轮产出路径
c:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606252256_phase1_ABD_ood/a_v6_copy_from_v5.md

## 用户需求
c:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606252256_phase1_ABD_ood/requirement.md
