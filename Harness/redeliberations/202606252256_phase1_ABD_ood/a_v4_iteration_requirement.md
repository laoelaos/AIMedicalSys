根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 🔴 严重问题

1. **事实错误：4.2 节"Refresh Token 轮换的安全补偿"声称旧 token 不可重复使用，与 Phase 1 设计矛盾** — 3.1.3 节步骤 5 和 4.2 节均明确声明 Phase 1 中"跳过"将旧 Refresh Token jti 加入黑名单，旧 Refresh Token 在 Phase 1 中技术上仍然可用。改进建议：修正为准确表述，说明旧 token 在 Phase 1 中仍可被重复使用；若检测到重复使用，可记录安全日志并触发告警。

2. **关键遗漏：`passwordChangeRequired` 字段未出现在 5.1 节实体变更表中** — 3.4 节明确说明 User 实体新增该字段，但 5.1 节完全未列出。改进建议：在 5.1 节 User.java 变更表中新增该字段的 Java 注解和 DDL 变更。

3. **关键遗漏：登录流程统一错误消息缺少对应的 ErrorCode，且 ACCOUNT_DISABLED 消息与设计冲突** — 3.1.1 步骤 5/6/7 已修复为统一消息，但 10.2 节仍保留 `ACCOUNT_DISABLED`（message="账户已停用"），泄露账户状态；且无适合"登录失败"场景的 ErrorCode。改进建议：新增通用 `LOGIN_FAILED` ErrorCode，登录流程三个失败步骤全部使用此 code；保留 `ACCOUNT_DISABLED` 但限制使用范围为"已认证请求中账号被禁用"。

4. **逻辑矛盾：LoginResponse.user 字段可空性在 DTO 定义、流程描述和 JSON 示例中三方不一致** — 5.2 节注释"首次登录 passwordChangeRequired=true 时 user 可能为 null"，3.1.1 步骤 11 未提及可空，6.2 JSON 示例 user 始终存在。改进建议：统一决策，推荐始终返回 user 对象，删除可空注释。

5. **逻辑矛盾：7.3 节主角色判定策略引用不存在的 `primaryRole` 字段** — 当前 User.java 无此字段，5.1 节也未计划新增。改进建议：方案一：在 5.1 节新增 `primaryRole` 字段；方案二：删除该引用，仅保留"按角色优先级排序"方案。

### 🟡 重要问题

6. **深度不足：9.2 节限流并发方案使用"或"连接两个选项，未做最终设计决策** — `synchronized` 或 `ReentrantLock` 行为差异明显。改进建议：选定一种方案并给出理由，推荐 `ReentrantLock`。

7. **关键遗漏：2.1 节目录结构未包含 MenuCreateRequest / MenuUpdateRequest** — 5.2 节已定义这两个 DTO，但 2.1 节 `menu/dto/` 下仅列出 MenuResponse.java。改进建议：补充两个 DTO 的路径。

8. **深度不足：8.3 节 A4 引用"诊断报告中的模板"无具体路径** — 阅读者无法确定"诊断报告"指哪个文档。改进建议：明确引用文件路径。

9. **深度不足：缺少 JWT_SECRET 配置参考与约束** — 未说明最小长度要求（HMAC-SHA256 至少 256 位）、合法字符集、启动时校验失败处理方式及配置示例。改进建议：补充强约束、application.yml 配置示例和启动验证逻辑。

10. **深度不足：CORS 配置使用默认值存在生产安全隐患** — `cors(Customizer.withDefaults())` 在生产环境存在安全风险。改进建议：显式标注生产环境必须通过 `CorsConfigurationSource` bean 配置白名单，或直接给出 recommended CORS 配置。

11. **设计问题：passwordChangeRequired 访问控制逻辑放置在 JwtAuthenticationFilter 违反单一职责** — 过滤器核心职责是 JWT 鉴权，不应承担业务规则检查。改进建议：抽离为独立 Filter（如 `PasswordChangeCheckFilter`）或将检查逻辑移至 `AuthServiceImpl`。

12. **逻辑矛盾：`/api/auth/logout` 的请求体在接口清单和流程描述中不一致** — 6.1 节标注请求体为"—"（无请求体），3.1.4 步骤 3 说存在可选请求体。改进建议：统一决策登出端点是否接受 Refresh Token。

### 🔵 一般问题

13. **步骤 7（密码不匹配）未指定错误消息** — 3.1.1 步骤 5/6 已指定统一消息，步骤 7 仅说"抛出 BusinessException"。改进建议：补上统一消息"用户名或密码错误"。

14. **7.2 节分页兼容承诺与接口清单不匹配** — 兼容承诺第 4 条声明分页查询格式不变，但 6.1 节无任何分页查询端点。改进建议：删除此条承诺或预留分页查询端点。

15. **8.2 节 H6 循环依赖修复方案过于模糊** — "使用延迟加载函数替代顶层 import"不够明确。改进建议：给出具体实现示例。

16. **User 查询优化在 7.1 节依赖图和 8.1 节 M9 之间不一致** — 7.1 节未提及查询优化要求，8.1 节 M9 建议使用 `@EntityGraph`。改进建议：在 7.1 节依赖图中添加脚注标注查询优化要求。

## 历史迭代回顾

- **已解决的问题**（出现在历史反馈但当前反馈中不再提及的问题）：
  - 代码"当前状态"事实错误（Iter1 #1 / Iter2 #1/#4/#8）— 已在 v3 中修正
  - 密码过渡方案缺失 / PasswordChangeRequest DTO 缺失（Iter1 #2/#3）— 已在 v2 中补充
  - RefreshTokenRequest DTO / refresh 端点请求体未定义（Iter1 #4）— 已在 v2 中补充
  - 内存黑名单可行性论证（Iter1 #5）— 已在 v2 中修正为仅 Access Token 方案
  - UserInfoResponse 字段对齐 / LoginResponse Breaking Change 声明（Iter1 #6/#7）— 已在 v2 中修正
  - JwtAuthenticationFilter 迁移步骤缺失（Iter1 #8）— 已在 v2 中补充
  - 8.3 节复用旧诊断报告 / deleted 列状态矛盾（Iter1 #9 / Iter2 #1/#8）— 已在 v3 中修正
  - SidebarBase props 定义 / 重复条目合并（Iter1 #10/#11）— 已在 v2 中修正
  - Role/Post/Function enabled @Column 缺失（Iter1 #12）— 已在 v2 中修正
  - refresh 端点 permitAll 矛盾（Iter1 #13）— 已在 v2 中修正
  - convertMenusToRoutes 递归策略矛盾（Iter1 #14）— 已在 v2 中澄清
  - LoginResponse userId/username 缺失（Iter2 #5）— 已在 v3 中补充
  - MenuCreateRequest/MenuUpdateRequest DTO 定义（Iter2 #6）— DTO 已在 v3 中补充（但目录结构遗漏→当前 #7）
  - 登录错误消息差异化导致枚举（Iter2 #7）— 已在 v3 中修正（但 ErrorCode 问题遗留→当前 #3）
  - getPrimaryRoleCode 策略（Iter2 #11）— 已在 v3 中补充（但 primaryRole 字段不存在→当前 #5）
  - 多 Tab 刷新竞态 / SecurityConfig 注入迁移（Iter2 #10/#12）— 已在 v3 中补充

- **持续存在的问题**（在多轮反馈中反复出现的问题，需重点解决）：
  本轮全部 16 个问题在第 3 轮迭代的诊断报告中均已提出，但在 a_v3_design_v1.md 中仍未完全修复。说明前一轮迭代中修复不彻底或修复方案本身引入了新的矛盾。需系统性地审查每个问题的改进建议是否已准确落实。

- **新发现的问题**：无（本轮全部问题均为第 3 轮已有问题）

## 上一轮产出路径
c:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606252256_phase1_ABD_ood/a_v3_design_v1.md

## 用户需求
c:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606252256_phase1_ABD_ood/requirement.md
