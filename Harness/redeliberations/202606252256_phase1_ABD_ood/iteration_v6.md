# 再审议判定报告（v6）

## 判定结果

RETRY

## 判定理由

诊断报告共识别出 13 个问题，其中 🔴 严重 2 个（Refresh Token Claims 示例缺 `tokenVersion`、缺陷追踪表遗漏 4 条原始问题），🟡 重要 6 个（错误码误用、EntryPoint 行为未定义、`nickname NOT NULL` 不一致、Layout 包容机制缺失、时序侧信道缺口、`expiresIn` 语义不明确），🔵 一般 5 个。质询报告确认审查结论为 LOCATED（内部循环提前终止，实际 2 轮 < 最大 12 轮）。因存在严重和一般等级问题，不符合 PASS 条件，判定为 RETRY。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：3.2 节 Refresh Token Claims 示例缺少 `tokenVersion` 字段
- **所在位置**：3.2 节「Refresh Token」Claims 结构 JSON 示例
- **严重程度**：严重
- **改进建议**：在 Refresh Token Claims 示例中补充 `"tokenVersion": 0` 字段，并说明其作用

---

- **问题描述**：8.1 节缺陷追踪表遗漏 4 条原始问题条目（M1/M2/M5/M8）
- **所在位置**：8.1 节包 B 后端问题追踪表
- **严重程度**：严重
- **改进建议**：在 8.1 节补充 M1/M2/M5/M8 四条目的追踪行，包含当前状态、修复方案、潜在副作用、影响范围四列

---

- **问题描述**：3.1.3 节步骤 6 错误使用 `LOGIN_FAILED` 作为刷新失败错误码
- **所在位置**：3.1.3 节步骤 6
- **严重程度**：一般
- **改进建议**：将步骤 6 的 `LOGIN_FAILED` 改为 `TOKEN_REFRESH_FAILED`

---

- **问题描述**：3.3 节 AuthenticationEntryPoint 描述与 10.2 节 ACCOUNT_DISABLED 的矛盾
- **所在位置**：3.3 节「SecurityFilterChain」exceptionHandling；10.2 节 ACCOUNT_DISABLED
- **严重程度**：一般
- **改进建议**：补充 EntryPoint 行为契约：携带 ACCOUNT_DISABLED 时返回 `Result.fail("ACCOUNT_DISABLED", "账户已被管理员停用")`；其余返回 `Result.fail("UNAUTHORIZED", "未认证或令牌已失效")`

---

- **问题描述**：4.3 节与 5.1 节对 `nickname NOT NULL` 变更的描述不一致
- **所在位置**：4.3 节「修复方案」列表 vs 5.1 节 User.java 变更表
- **严重程度**：一般
- **改进建议**：在 5.1 节 User.java 变更表中新增 `nickname NOT NULL` 行

---

- **问题描述**：8.2 节 H5 展平路由策略缺少 Layout 包容机制说明
- **所在位置**：8.2 节 H5「修复方案」列
- **严重程度**：一般
- **改进建议**：补充 Layout 包容机制的设计决策：Layout 在 App.vue 中包裹 `<router-view>`，或展平路由作为 Layout 子路由注册

---

- **问题描述**：登录流程步骤 6 存在时序侧信道缺口
- **所在位置**：3.1.1 节步骤 6
- **严重程度**：一般
- **改进建议**：在步骤 6 中增加 dummy BCrypt 比对，消除与步骤 5/7 的时序差异；或在设计决策中明确承认此残余风险

---

- **问题描述**：`LoginResponse` 中 `tokenType` 与 `expiresIn` 的定位不明确
- **所在位置**：5.2 节 LoginResponse、TokenRefreshResponse；6.2 节 JSON 示例
- **严重程度**：一般
- **改进建议**：在 5.2 节补充注释明确 `expiresIn` 为"access token 剩余有效秒数（从签发时计算）"，同步更新 6.2 节 JSON 示例字段注释
