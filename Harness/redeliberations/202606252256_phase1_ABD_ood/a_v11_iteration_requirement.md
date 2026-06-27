根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

组件B诊断报告共识别8个问题，质询结论为LOCATED（确认诊断结论可信）：

- **🔴 问题1（严重）**：4.1节速率限制表头声称限流维度为「同一IP（任意API路径）」，但GlobalRateLimitFilter将`/api/auth/login`和`/api/auth/refresh`列入白名单排除；`/api/auth/refresh`既不受全局限流覆盖，也无独立限流策略，且在SecurityConfig中为permitAll
- **🔴 问题2（严重）**：3.1.4/4.4/5.2/6.1节文档一致描述登出端点请求体可选携带refreshToken，但5.2节RefreshTokenRequest record标注@NotBlank；Controller层签名（@RequestBody(required=false)或分端点等）未指定
- **🟡 问题3（重要）**：5.2节MenuUpdateRequest声明局部更新语义（PATCH语义），但端点方法仍为PUT，违反RFC 7231 §4.3.4；此问题在第7轮已发现，修订后仅更改语义描述未改端点方法
- **🟡 问题4（重要）**：3.3/10.1/10.2节PasswordChangeCheckFilter返回403的实现机制未定义——直接HttpServletResponse写JSON绕过GlobalExceptionHandler，抛自定义异常又未定义对应异常类和异常处理路径
- **🟡 问题5（重要）**：5.2节MenuUpdateRequest.id标注@NotNull用于一致性校验，但与局部更新语义冲突（省略id则@NotNull失败，必须传入id又违背"省略字段不更新"语义）
- **🟢 问题6（一般）**：4.1节SlidingWindowCounter接口契约（ReentrantLock？窗口精度？）未定义；两套限流器返回相同ErrorCode RATE_LIMITED时前端无法区分触发来源
- **🟢 问题7（一般）**：3.4/7.4节密码变更后前端恢复流程（清除标记→GET /api/auth/me→GET /api/menu/tree→跳转首页）未覆盖中间步骤失败的异常场景
- **🟢 问题8（一般）**：8.3节A1和A3行缺少「潜在副作用」和「影响范围」列，与8.1/8.2节格式不一致

## 历史迭代回顾

### 已解决的问题（出现在历史反馈但当前反馈中不再提及）
- 密码变更后"强制用户重新登录"语义矛盾（Iteration 8 #1）→ v10已修复
- PASSWORD_COMMON在Phase 1不可达（Iteration 8 #2）→ v10已标注
- tokenVersion闭环断裂（Iteration 8 #3）→ v10已修复
- 展平路由name唯一性运行时竞态（Iteration 8 #4）→ v10已改为确定性方案
- 登录成功仅清除用户名维度失败计数（Iteration 8 #5）→ v10已修复
- 多Tab并发刷新失败计数器策略（Iteration 8 #6）→ v10已定义
- expiresIn语义落差（Iteration 8 #7）→ v10已对齐
- GlobalRateLimitFilter与InMemoryRateLimitGuard委托关系（Iteration 8 #8）→ v10已明确
- "连续"语义定义与实现不匹配（Iteration 9 #1）→ v11(v10修订)已简化
- refresh端点未检查锁定状态（Iteration 9 #2）→ v11已修复
- 版本号不一致（Iteration 9 #3）→ v11已修复
- 清除SecurityContext说法不准确（Iteration 9 #4）→ v11已修正

### 持续存在的问题（在多轮反馈中反复出现，需重点解决）
- **速率限制表头矛盾 + refresh无限流**（Iteration 10 #1 → 本轮Issue 1）：反复出现，本轮仍未解决
- **登出可选请求体与@NotBlank矛盾**（Iteration 10 #2 → 本轮Issue 2）：反复出现，本轮仍未解决
- **PUT菜单局部更新语义违反HTTP规范**（Iteration 7 #5 → 本轮Issue 3）：第7轮已发现，仅改语义描述未改端点方法，问题持续存在
- **MenuUpdateRequest.id @NotNull冲突**（Iteration 4 #8 → 本轮Issue 5）：第4轮已发现，问题持续存在
- **SlidingWindowCounter契约未定义**（Iteration 10 #3 → 本轮Issue 6）：反复出现
- **密码变更恢复流程异常场景缺失**（Iteration 10 #4 → 本轮Issue 7）：反复出现
- **8.3节格式缺失**（Iteration 10 #5 → 本轮Issue 8）：反复出现

### 新发现的问题（本轮新识别）
- **PasswordChangeCheckFilter 403实现机制未定义**（本轮Issue 4）：前10轮迭代中未被识别为独立问题，本轮首次发现

## 上一轮产出路径
c:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606252256_phase1_ABD_ood/a_v10_copy_from_v9.md

## 用户需求
c:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606252256_phase1_ABD_ood/requirement.md
