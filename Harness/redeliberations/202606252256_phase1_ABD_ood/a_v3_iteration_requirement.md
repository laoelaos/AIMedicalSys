根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 🔴 严重问题

1. **deleted 列当前状态描述与代码实际不符（事实错误）** — 8.3 节 A2 行声称"16 张表 DDL 缺少 NOT NULL"，实际 schema.sql 中全部 16 张表的 `deleted` 列均已定义 `NOT NULL DEFAULT 0`，无一遗漏。应将当前状态修正为"已完成"。

2. **Phase 1 Refresh Token 轮换的安全补偿机制存在错误逻辑（逻辑矛盾）** — 3.1.3 节 + 4.2 节声称"攻击者使用旧 Refresh Token 时将因 jti 不与新签发的 Refresh Token 冲突而无法通过应用层校验"，但 `JwtTokenProvider.validateToken()` 的校验逻辑仅包含签名验证、有效期验证和 type claim 检查，不存在 jti 比较机制。应彻底修改该描述，如实说明 Phase 1 无 Refresh Token 黑名单的真实风险。

3. **passwordChangeRequired 对其他 API 的访问控制缺失（关键遗漏）** — 3.4 节和 4.4 节只定义了登录时跳转逻辑，未说明当 `passwordChangeRequired=true` 时访问其他认证端点的处理策略。应在 Filter 或 Service 中补充：除 `/api/auth/password` 和 `/api/auth/logout` 外所有 API 返回 403。

4. **Section 4.3 修复方案状态与 Section 5.1 及代码事实相互矛盾（内部矛盾）** — 4.3 节将 Role.java 和 Post.java 补加 `@Column(nullable=false)` 标注为"→ 已完成"，但实际代码中缺少该注解，且 5.1 节也写着"缺 `@Column(nullable=false)`"。应统一移除"→ 已完成"标记。

5. **LoginResponse 未包含 userId/username（可落地性缺陷）** — 5.2 节 LoginResponse 依赖 UserInfoResponse 中的 user 对象，但用户首次登录时 role/permissions 尚未完全可用，且 passwordChangeRequired 场景下用户无法访问 GET /api/auth/me。应在 LoginResponse 中直接包含 userId(Long) 和 username(String) 作为顶层字段。

### 🟡 重要问题

6. **菜单 CRUD 缺少 MenuCreateRequest 和 MenuUpdateRequest DTO 定义（关键遗漏）** — 5.2 节 DTO 列表和 6.1 节接口清单列举了菜单创建/更新端点，但缺少对应的请求 DTO record 定义。

7. **登录错误消息差异化导致用户名可枚举（安全设计遗漏）** — 3.1.1 节步骤 5 返回"用户名或密码错误"，步骤 6 返回"账户已停用"，攻击者可通过差异化的错误消息枚举有效用户名。应统一返回"用户名或密码错误"。

8. **Section 8.3 A2 与 Section 5.1 对 deleted 列的完成状态矛盾（内部矛盾）** — 5.1 节标注 deleted 列"已完成"，8.3 节 A2 行却声称"需要修复"。应统一为"已完成"。

9. **Section 7.5 声称已合并重复条目，但 8.1 节仍保留 M12_P11（执行错误）** — 7.5 节声明已合并前端 M16 和后端 M12_P11，但 8.1 节 M12_P11 行仍然存在。应删除 8.1 节的 M12_P11 行。

10. **多 Tab 并发 token 刷新竞态未处理（可落地性缺陷）** — 3.1.3 节和 7.4 节未考虑多个浏览器 Tab 同时检测到 401 时并发发起刷新请求的问题。应在 7.4 节新增 Promise 单例模式互斥锁，确保同一时间只执行一次刷新。

11. **getPrimaryRoleCode() 未定义主角色判定策略（关键遗漏）** — 7.3 节引入了 `getPrimaryRoleCode()` 但未定义多角色时如何选择主角色、无角色时的降级策略。应补充显式规则（如 User 新增 primaryRole 字段或按权限优先级排序）。

12. **SecurityConfigPhase1 依赖注入的旧 Filter 未纳入迁移方案（可落地性缺陷）** — 2.2 节只描述了删除旧 Filter 引用，未指出 SecurityConfigPhase1 构造函数已通过依赖注入引用了旧位置的 JwtAuthenticationFilter。应补充注入类型从旧包名改为新包名的说明。

## 历史迭代回顾

### 已解决的问题（出现在历史反馈但当前反馈中不再提及的问题）
- 第 1 轮：password NOT NULL 与 Java 默认值状态与实际代码不符 → 第 2 轮 v2 已修正 4.3 节和 5.1 节实体状态描述
- 第 1 轮：密码策略升级缺少过渡方案 → v2 已补充 PasswordChangeService 和密码变更强制策略
- 第 1 轮：PasswordChangeRequest DTO 缺失 → v2 已补全
- 第 1 轮：RefreshTokenRequest 请求体缺失 → v2 已补全
- 第 1 轮：内存黑名单方案 12GB 可行性论证 → v2 已改为仅黑名单 Access Token 策略，提供内存估算
- 第 1 轮：UserInfoResponse 字段与前端不兼容 → v2 已对齐字段名并声明 Breaking Change
- 第 1 轮：LoginResponse 字段变更未声明 → v2 已补充 Breaking Change 声明
- 第 1 轮：JwtAuthenticationFilter 迁移步骤缺失 → v2 已补充
- 第 1 轮：Section 8.3 复用旧报告 → v2 已更新
- 第 1 轮：SidebarBase props 定义缺失 → v2 已补充
- 第 1 轮：前后端重复条目未合并 → v2 7.5 节已合并
- 第 1 轮：convertMenusToRoutes 递归策略问题 → v2 已澄清展平策略
- 第 1 轮：/api/auth/refresh permitAll 不一致 → v2 已统一
- 第 1 轮：Role/Post/Function enabled @Column(nullable=false) 标注缺失 → v2 5.1 节已标注（但本轮问题 4 指出标注与 4.3 节状态矛盾）

### 持续存在的问题（在多轮反馈中反复出现的问题，需重点解决）
- 第 1 轮→第 2 轮：Role/Post enabled 字段 `@Column(nullable=false)` 完成状态的矛盾表述在本轮依然存在（4.3 节标记"已完成"与 5.1 节标记"待修复"矛盾），需统一修正

### 新发现的问题（本轮新识别的问题）
- 问题 1：deleted 列状态描述与代码事实不符
- 问题 2：Refresh Token 轮换安全补偿逻辑错误
- 问题 3：passwordChangeRequired 阻断缺失
- 问题 5：LoginResponse 缺 userId/username
- 问题 6：菜单 CRUD DTO 缺失
- 问题 7：登录错误消息可枚举用户名
- 问题 8：deleted 列状态文档内部矛盾
- 问题 9：7.5 节声明与 8.1 节事实不符
- 问题 10：多 Tab 刷新竞态未处理
- 问题 11：getPrimaryRoleCode 策略未定义
- 问题 12：旧 Filter 迁移遗漏

## 上一轮产出路径
c:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606252256_phase1_ABD_ood/a_v2_design_v1.md

## 用户需求
c:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606252256_phase1_ABD_ood/requirement.md
