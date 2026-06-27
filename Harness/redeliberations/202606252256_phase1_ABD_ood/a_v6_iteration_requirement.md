根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

1. **登录流程时序侧信道攻击风险**（3.1.1节步骤5 vs 步骤7，重要）：步骤5（用户名不存在）不执行BCrypt密码比对，步骤7执行passwordEncoder.matches()，攻击者可通过响应时间差异枚举有效用户名。建议：对虚拟哈希值执行dummy BCrypt比对以消除时序差异，或在设计决策中明确承认此风险并推迟到Phase 2。

2. **密码变更后旧Refresh Token撤销机制未设计**（4.2节，重要）：声称"用户修改密码后所有已签发的Refresh Token在下次使用时因密码变更而被拒绝"，但仅作为TODO占位，未给出具体设计。建议：二选一——(a) User实体新增tokenVersion字段，Refresh Token claims携带版本号，刷新时比对；(b) 删除该条声明，明确说明Phase 1中密码变更后旧Refresh Token仍可使用。

3. **密码变更成功后前端恢复流程未定义**（3.4节、7.4节，重要）：仅定义清除passwordChangeRequired标记，未定义前端后置流程（调用哪些API、如何跳转）。建议：补充完整流程——PUT /api/auth/password成功 → 清除标记 → GET /api/auth/me刷新用户信息 → GET /api/menu/tree获取菜单 → 跳转到首页。

4. **菜单递归展平方案与路由注册策略存在冲突**（8.2节H5，重要）：递归展平生成的完整路径在Vue Router中的实际注册行为未明确（相对/绝对路径？Layout子路由还是根路由？path === '/'跳过滤如何兼容？name唯一性如何保证？）。建议：明确(a)路由注册策略；(b)与跳过滤条件的兼容关系；(c)name唯一性保证策略。

5. **前端缺少PASSWORD_CHANGE_REQUIRED错误码处理**（7.4节，重要）：HTTP 403 + ErrorCode PASSWORD_CHANGE_REQUIRED在axios拦截器中无对应处理规则，不会弹出修改密码提示或跳转。建议：axios响应拦截器识别此错误码时重定向到密码修改页面并终止原始请求。

6. **登录流程步骤7失败计数维度未指定**（3.1.1节步骤7，一般）：步骤7仅说"递增LoginAttemptTracker失败计数"，未指定维度。建议：补充为"递增LoginAttemptTracker用户名维度的失败计数"。

7. **JwtTokenProvider @PostConstruct启动验证依赖未显式声明的Bean类型**（4.7节、2.1节，一般）：文档未说明JwtTokenProvider的Spring Bean注册方式，若以非Spring-managed方式实例化，@PostConstruct不会生效。建议：标注其Spring stereotype（如@Component），或将启动验证移至AuthModuleConfig.@PostConstruct中。

## 历史迭代回顾

- **已解决的问题**（前轮反馈中已识别、本轮不再提及的问题）：
  - 角色优先级依赖不存在的字段（第4轮问题1）：Role新增sort字段已纳入设计
  - ACCOUNT_DISABLED从未被触发（第4轮问题2）：Filter抛出AuthenticationException逻辑已补充
  - PasswordChangeCheckFilter冗余查询用户（第4轮问题3）：request attribute传递方案已实施
  - Token刷新响应变更未声明Breaking Change（第4轮问题4）：6.4节已补充
  - MenuResponse未定义（第4轮问题5）：5.2节已补充
  - 全局IP频率限制缺少实现机制（第4轮问题6）：GlobalRateLimitFilter已纳入设计
  - 步骤5失败计数维度未指定（第4轮问题7）：已明确指定IP维度
  - MenuUpdateRequest.id与路径关系不明确（第4轮问题8）：一致性校验规则已补充
  - PasswordChangeCheckFilter 403消息不一致（第4轮问题9）：已统一
  - 刷新后前端获取用户信息流程（第4轮问题10）：已补充
  - 其他第1-3轮的代码事实错误、内部矛盾、遗漏等问题均已解决

- **持续存在的问题**（在多轮反馈中反复出现的问题，需重点解决）：
  - 上述7个问题均在第5轮（上一轮）诊断中已被识别，但在本轮仍然存在，属于持续性问题。这些问题涉及安全遗漏、完整流程缺失、可落地细节澄清等维度，需重点解决。

- **新发现的问题**：无

## 上一轮产出路径
c:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606252256_phase1_ABD_ood/a_v5_copy_from_v4.md

## 用户需求
c:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606252256_phase1_ABD_ood/requirement.md
