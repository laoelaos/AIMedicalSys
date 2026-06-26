## 迭代第 1 轮

1. **问题描述**：多处声称代码"当前状态"缺失 NOT NULL 约束/Java 默认值，实际代码已存在
   - 所在位置：5.1 实体变更表（4 张表）、8.3 包 A 数据建模问题
   - 严重程度：严重
   - 改进建议：基于当前代码实际状态编写设计，已满足的标注"已完成"
2. **问题描述**：密码策略升级缺少过渡方案——强制弱密码用户下次登录修改密码的机制未定义
   - 所在位置：4.3 密码策略；8.1 H8 修复方案
   - 严重程度：重要
   - 改进建议：登录流程中增加密码策略检查，不满足则要求设置新密码
3. **问题描述**：`PUT /api/auth/password` 缺少对应的 `PasswordChangeRequest` DTO 定义
   - 所在位置：6.1 接口清单；5.2 DTO 列表
   - 严重程度：严重
   - 改进建议：补充 PasswordChangeRequest record 定义
4. **问题描述**：`POST /api/auth/refresh` 缺少请求体定义，前端无法确定如何传递 Refresh Token
   - 所在位置：6.1 接口清单；3.1.3 Token 刷新流程
   - 严重程度：重要
   - 改进建议：明确请求体为 RefreshTokenRequest，或通过 header 传递
5. **问题描述**：内存黑名单方案未进行可行性论证，6048 万条约需 12GB+ 堆内存
   - 所在位置：4.2 Token 黑名单设计
   - 严重程度：严重
   - 改进建议：改用 Redis 方案，或设置上限+预衰退策略
6. **问题描述**：`UserInfoResponse` 字段（nickname/userType）与前端 `UserInfo` 接口（realName/role）不兼容
   - 所在位置：5.2 UserInfoResponse；7.2 包 B → 包 D 契约
   - 严重程度：重要
   - 改进建议：对齐前端接口字段名，或显式声明 Breaking Change
7. **问题描述**：`LoginResponse` 字段名变更（token→accessToken）未声明 Breaking Change
   - 所在位置：5.2 LoginResponse；8.1 C5 修复方案
   - 严重程度：重要
   - 改进建议：声明 Breaking Change 并说明前端同步修改方案
8. **问题描述**：`JwtAuthenticationFilter` 移动位置但无跨模块迁移步骤
   - 所在位置：2.1 目录结构；2.2 模块依赖方向
   - 严重程度：重要
   - 改进建议：补充过滤器 Bean 声明方式和 JwtUtil→JwtTokenProvider 过渡方案
9. **问题描述**：8.3 节完全复用旧诊断报告，未复核当前代码状态
   - 所在位置：8.3 包 A 数据建模问题
   - 严重程度：严重
   - 改进建议：复核代码后重新填写"当前状态"列
10. **问题描述**：`SidebarBase` 用 `useRoute()` 修复方案未说明 props 名称、类型及影响范围
    - 所在位置：8.2 M12
    - 严重程度：一般
    - 改进建议：补充 props 定义和受影响父组件清单
11. **问题描述**：前端用户信息存储修复方案前后端重复列出
    - 所在位置：8.1 M12 vs 8.2 M16
    - 严重程度：一般
    - 改进建议：合并为一个条目
12. **问题描述**：Role.java/Post.java/Function.java enabled 字段缺少 @Column(nullable = false)
    - 所在位置：5.1 实体变更表
    - 严重程度：一般
    - 改进建议：补加注解，同步更新 schema.sql
13. **问题描述**：`/api/auth/refresh` 在 SecurityConfig 中为 permitAll，但在保护清单中标注需 JWT 认证
    - 所在位置：3.3 SecurityFilterChain；4.4 API 端点保护清单
    - 严重程度：重要
    - 改进建议：统一为 permitAll，4.4 节修正
14. **问题描述**：`convertMenusToRoutes` 递归化方案与路由注册策略的矛盾未解决
    - 所在位置：8.2 H5
    - 严重程度：重要
    - 改进建议：澄清递归化后的路由注册策略（展平 vs 嵌套 children）

## 迭代第 2 轮

1. **问题描述**：`deleted` 列当前状态描述与代码实际不符（事实错误）
   - 所在位置：8.3 节 A2 行的「当前状态」列
   - 严重程度：严重
   - 改进建议：将当前状态修正为"16 张表 DDL 已为 `NOT NULL DEFAULT 0`，已完成"，删除 A2 条目或更新为"无需修改"

2. **问题描述**：Phase 1 Refresh Token 轮换的安全补偿机制存在错误逻辑（逻辑矛盾）
   - 所在位置：3.1.3 节第 5 步 + 4.2 节「Refresh Token 轮换的安全补偿」
   - 严重程度：严重
   - 改进建议：删除或彻底修改该段描述，如实说明 Phase 1 不做 Refresh Token 黑名单的真实风险
3. **问题描述**：`passwordChangeRequired` 对其他 API 的访问控制缺失（关键遗漏）
   - 所在位置：3.4 节「密码变更强制策略」、4.4 节 API 端点保护清单
   - 严重程度：重要
   - 改进建议：在 `JwtAuthenticationFilter` 或 `AuthServiceImpl` 中补充当 `passwordChangeRequired=true` 时除 `/api/auth/password` 和 `/api/auth/logout` 外所有 API 返回 403
4. **问题描述**：Section 4.3 修复方案状态与 Section 5.1 及代码事实相互矛盾（内部矛盾）
   - 所在位置：4.3 节「修复方案」列表 vs 5.1 节 Role.java/Post.java 变更表
   - 严重程度：严重
   - 改进建议：统一移除"→ 已完成"标记，如实标注为待修复
5. **问题描述**：`LoginResponse` 未包含 `userId`/`username`（可落地性缺陷）
   - 所在位置：5.2 节 `LoginResponse` record 定义
   - 严重程度：重要
   - 改进建议：在 `LoginResponse` 中直接包含 `userId`（Long）和 `username`（String）作为顶层字段
6. **问题描述**：菜单 CRUD 缺少 `MenuCreateRequest` 和 `MenuUpdateRequest` DTO 定义（关键遗漏）
   - 所在位置：5.2 节 DTO 列表；6.1 节接口清单
   - 严重程度：重要
   - 改进建议：补充 `MenuCreateRequest` 和 `MenuUpdateRequest` 的 record 定义到 5.2 节
7. **问题描述**：登录错误消息差异化导致用户名可枚举（安全设计遗漏）
   - 所在位置：3.1.1 节登录流程步骤 5 和步骤 6
   - 严重程度：重要
   - 改进建议：统一错误消息，步骤 6 也应返回"用户名或密码错误"
8. **问题描述**：Section 8.3 A2 与 Section 5.1 对 `deleted` 列的完成状态矛盾（内部矛盾）
   - 所在位置：5.1 节 User.java 变更表 vs 8.3 节 A2 行
   - 严重程度：重要
   - 改进建议：统一为"已完成"，删除 8.3 A2 行中不需要的修复方案
9. **问题描述**：Section 7.5 声称已合并重复条目，但 8.1 节仍保留 M12_P11（执行错误）
   - 所在位置：7.5 节 vs 8.1 节 M12_P11 行
   - 严重程度：一般
   - 改进建议：删除 8.1 节的 M12_P11 行
10. **问题描述**：多 Tab 并发 token 刷新竞态未处理（可落地性缺陷）
    - 所在位置：3.1.3 节 Token 刷新流程；7.4 节前端补偿机制
    - 严重程度：重要
    - 改进建议：7.4 节新增前端 refresh 逻辑使用互斥锁（Promise 单例模式）
11. **问题描述**：`getPrimaryRoleCode()` 未定义主角色判定策略（关键遗漏）
    - 所在位置：7.3 节包 A → 包 B 影响分析表最后一行
    - 严重程度：重要
    - 改进建议：补充主角色选择策略的显式规则及无角色场景的处理方案
12. **问题描述**：SecurityConfigPhase1 依赖注入的旧 Filter 未纳入迁移方案（可落地性缺陷）
    - 所在位置：2.2 节模块依赖方向；12 节修订说明 P8
    - 严重程度：重要
    - 改进建议：2.2 节补充说明 SecurityConfigPhase1 的注入类型需从旧包名改为新包名

## 迭代第 3 轮

1. **问题描述**：4.2 节"Refresh Token 轮换的安全补偿"声称旧 token 不可重复使用，与 Phase 1 设计矛盾
   - 所在位置：4.2 节第一个 bullet
   - 严重程度：严重
   - 改进建议：将此描述修正为准确表述，明确 Phase 1 中旧 token 在技术上仍可被重复使用

2. **问题描述**：`passwordChangeRequired` 字段未出现在 5.1 节实体变更表中
   - 所在位置：5.1 节 User.java 变更表
   - 严重程度：严重
   - 改进建议：在 5.1 节 User.java 变更表中新增该字段的 Java 注解和 DDL 变更

3. **问题描述**：登录流程统一错误消息缺少对应的 ErrorCode，且 ACCOUNT_DISABLED 消息与设计冲突
   - 所在位置：3.1.1 步骤 5-7，10.2 节 ACCOUNT_DISABLED
   - 严重程度：严重
   - 改进建议：新增通用 `LOGIN_FAILED` ErrorCode，保留 `ACCOUNT_DISABLED` 但限制使用范围

4. **问题描述**：LoginResponse.user 字段可空性在 DTO 定义、流程描述和 JSON 示例中三方不一致
   - 所在位置：5.2 LoginResponse 定义，3.1.1 step 11，6.2 JSON 示例
   - 严重程度：严重
   - 改进建议：统一决策，确保三处描述一致

5. **问题描述**：7.3 节主角色判定策略引用不存在的 `primaryRole` 字段
   - 所在位置：7.3 节最后一行
   - 严重程度：严重
   - 改进建议：补充 `primaryRole` 字段或删除该引用

6. **问题描述**：9.2 节限流并发方案使用"或"连接两个选项，未做最终设计决策
   - 所在位置：9.2 节
   - 严重程度：重要
   - 改进建议：选定一种方案并给出理由

7. **问题描述**：2.1 节目录结构未包含 MenuCreateRequest / MenuUpdateRequest
   - 所在位置：2.1 节 `menu/dto/` 目录
   - 严重程度：重要
   - 改进建议：在目录结构中补充两个 DTO 的路径

8. **问题描述**：8.3 节 A4 引用"诊断报告中的模板"无具体路径
   - 所在位置：8.3 节 A4 "修复方案"列
   - 严重程度：重要
   - 改进建议：明确引用文件路径

9. **问题描述**：缺少 JWT_SECRET 配置参考与约束
   - 所在位置：3.2 节签名算法说明
   - 严重程度：重要
   - 改进建议：补充 JWT_SECRET 的强约束、配置示例和启动验证逻辑

10. **问题描述**：CORS 配置使用默认值存在生产安全隐患
    - 所在位置：3.3 节 SecurityFilterChain 代码段
    - 严重程度：重要
    - 改进建议：显式标注生产环境必须配置 CORS 白名单

11. **问题描述**：passwordChangeRequired 访问控制逻辑放置在 JwtAuthenticationFilter 违反单一职责
    - 所在位置：3.4 节「passwordChangeRequired 访问控制」子节
    - 严重程度：重要
    - 改进建议：将密码策略检查抽离为独立 Filter

12. **问题描述**：`/api/auth/logout` 的请求体在接口清单和流程描述中不一致
    - 所在位置：6.1 节接口清单 vs 3.1.4 节步骤 3
    - 严重程度：重要
    - 改进建议：统一决策登出端点是否接受 Refresh Token

13. **问题描述**：步骤 7（密码不匹配）未指定错误消息
    - 所在位置：3.1.1 step 7
    - 严重程度：一般
    - 改进建议：补上统一消息

14. **问题描述**：7.2 节分页兼容承诺与接口清单不匹配
    - 所在位置：7.2 节兼容承诺第 4 条，6.1 节接口清单
    - 严重程度：一般
    - 改进建议：删除此条承诺或预留分页查询端点

15. **问题描述**：8.2 节 H6 循环依赖修复方案过于模糊
    - 所在位置：8.2 节 H6 "修复方案"列
    - 严重程度：一般
    - 改进建议：给出具体实现模式或代码示例

16. **问题描述**：User 查询优化在 7.1 节依赖图和 8.1 节 M9 之间不一致
    - 所在位置：7.1 节依赖图 vs 8.1 节 M9
    - 严重程度：一般
    - 改进建议：在 7.1 节依赖图中添加查询优化脚注

## 迭代第 4 轮

1. **问题描述**：角色优先级依赖不存在的字段。Role.java 实体无 `level`/`sort`/`order`/`priority` 字段，主导色判定策略无法落地。
   - 所在位置：5.2 节 UserInfoResponse 字段映射说明（第 650 行）、7.3 节 UserType→role 行（第 841 行）
   - 严重程度：严重
   - 改进建议：在 Role 实体中新增 `sort` 字段并同步 DDL；或改用角色 `code`/`id` 隐式排序；或删除"主角色"概念，取第一个关联角色。选定方案后更新设计文档。

2. **问题描述**：`ACCOUNT_DISABLED` 定义了但从未被触发。JwtAuthenticationFilter 对禁用用户仅放行而非返回禁用错误码。
   - 所在位置：10.2 节 ErrorCode 表 vs 3.3 节 JwtAuthenticationFilter 行为契约
   - 严重程度：一般
   - 改进建议：在 Filter 中抛出 `AuthenticationException`（含 `ACCOUNT_DISABLED`）；或删除 `ACCOUNT_DISABLED` 统一返回 `UNAUTHORIZED`。

3. **问题描述**：`PasswordChangeCheckFilter` 冗余查询用户。JwtAuthenticationFilter 已加载用户，后续 Filter 重复查库。
   - 所在位置：3.3 节两 Filter 行为契约
   - 严重程度：一般
   - 改进建议：JwtAuthenticationFilter 将 `passwordChangeRequired` 写入 request attribute，PasswordChangeCheckFilter 直接读取。

4. **问题描述**：Token 刷新响应变更未声明 Breaking Change。响应字段从 `token`/`user` 变为 `accessToken`/`refreshToken`（不含 `user`）。
   - 所在位置：6.4 节 Breaking Change 声明表
   - 严重程度：一般
   - 改进建议：在 6.4 节新增刷新端点 Breaking Change 条目。

5. **问题描述**：`MenuResponse` 被引用但从未在 5.2 节定义。
   - 所在位置：2.1 节目录结构、7.2 节包 B→包 D 契约
   - 严重程度：一般
   - 改进建议：在 5.2 节新增 `MenuResponse` record，字段与前端 `MenuItem` 接口对齐。

6. **问题描述**：全局 IP 频率限制缺少实现机制。仅有定义无 Filter/拦截器实现。
   - 所在位置：4.1 节速率限制表
   - 严重程度：一般
   - 改进建议：新增 `GlobalRateLimitFilter` 注册到 Filter 链；或明确推迟到 Phase 2 并从当前设计删除。

7. **问题描述**：登录流程步骤 5 未指定失败计数维度。用户名不存在时无法确定递增哪个维度。
   - 所在位置：3.1.1 节步骤 5
   - 严重程度：一般
   - 改进建议：明确步骤 5 递增 IP 维度计数，步骤 6（用户禁用）递增用户名和 IP 双维度。

8. **问题描述**：`MenuUpdateRequest.id` 与路径 `{id}` 关系不明确。
   - 所在位置：5.2 节 MenuUpdateRequest、6.1 节 PUT /api/menu/{id}
   - 严重程度：一般
   - 改进建议：说明 `id` 是冗余字段（删除）或一致性校验用途（加 `@NotNull` 并说明校验规则）。

9. **问题描述**：`PasswordChangeCheckFilter` 的 403 消息与 ErrorCode 不一致。
   - 所在位置：3.3 节 vs 10.2 节 ErrorCode 表
   - 严重程度：轻微
   - 改进建议：统一消息文本或新增专用 ErrorCode。

10. **问题描述**：刷新端点未定义成功后前端获取用户信息的流程。
    - 所在位置：3.1.3 节 Token 刷新流程、7.4 节前端补偿机制
    - 严重程度：轻微
    - 改进建议：补充说明刷新后调用 `GET /api/auth/me` 更新本地用户信息。

## 迭代第 5 轮

1. **问题描述**：登录流程步骤 5 不执行 BCrypt 密码比对，存在时序侧信道攻击风险
   - 所在位置：3.1.1 节步骤 5 vs 步骤 7
   - 严重程度：严重
   - 改进建议：在步骤 5 中对虚拟哈希值执行 dummy BCrypt 比对以消除时序差异，或在设计决策中明确承认此风险并推迟到 Phase 2
2. **问题描述**：密码变更后旧 Refresh Token 撤销机制未设计，文档仅作 TODO 占位
   - 所在位置：4.2 节「Refresh Token 的安全补偿策略」末条
   - 严重程度：严重
   - 改进建议：User 实体新增 tokenVersion，Refresh Token claims 携带版本号，刷新时比对；或删除该条声明并承认此风险
3. **问题描述**：密码变更成功后前端恢复流程未定义
   - 所在位置：3.4 节「密码变更强制策略」、7.4 节「包 D 前端对包 B 的补偿机制」
   - 严重程度：严重
   - 改进建议：补充密码变更后完整前后端交互流程：PUT /api/auth/password 成功 → 前端清除 passwordChangeRequired → 调用 GET /api/auth/me → 调用 GET /api/menu/tree → 跳转到首页
4. **问题描述**：菜单递归展平方案与现有路由注册策略存在冲突
   - 所在位置：8.2 节 H5、「修订说明（v5）」P10
   - 严重程度：严重
   - 改进建议：明确路由注册策略（根路由 vs Layout 子路由，path 绝对/相对路径）、与跳过滤条件的兼容关系、name 唯一性保证策略
5. **问题描述**：前端缺少 PASSWORD_CHANGE_REQUIRED 错误码处理
   - 所在位置：7.4 节「包 D 前端对包 B 的补偿机制」
   - 严重程度：严重
   - 改进建议：在 axios 响应拦截器中识别 HTTP 403 + ErrorCode PASSWORD_CHANGE_REQUIRED 时重定向到密码修改页面
6. **问题描述**：登录流程步骤 7 的失败计数维度未指定
   - 所在位置：3.1.1 节步骤 7
   - 严重程度：一般
   - 改进建议：补充为"递增 LoginAttemptTracker 用户名维度的失败计数"
7. **问题描述**：JwtTokenProvider 的 @PostConstruct 启动验证依赖未显式声明的 Bean 类型
    - 所在位置：4.7 节「启动验证逻辑」、2.1 节目录结构
    - 严重程度：一般
    - 改进建议：标注其 Spring stereotype（如 @Component），或将启动验证逻辑移至 AuthModuleConfig.@PostConstruct

## 迭代第 6 轮

1. **问题描述**：3.2 节 Refresh Token Claims 示例缺少 `tokenVersion` 字段
   - 所在位置：3.2 节「Refresh Token」Claims 结构 JSON 示例
   - 严重程度：严重
   - 改进建议：在 Refresh Token Claims 示例中补充 `"tokenVersion": 0` 字段，并说明其作用

2. **问题描述**：8.1 节缺陷追踪表遗漏 4 条原始问题条目（M1/M2/M5/M8）
   - 所在位置：8.1 节包 B 后端问题追踪表
   - 严重程度：严重
   - 改进建议：在 8.1 节补充 M1/M2/M5/M8 四条目的追踪行，包含当前状态、修复方案、潜在副作用、影响范围四列

3. **问题描述**：3.1.3 节步骤 6 错误使用 `LOGIN_FAILED` 作为刷新失败错误码
   - 所在位置：3.1.3 节步骤 6
   - 严重程度：一般
   - 改进建议：将步骤 6 的 `LOGIN_FAILED` 改为 `TOKEN_REFRESH_FAILED`

4. **问题描述**：3.3 节 AuthenticationEntryPoint 描述与 10.2 节 ACCOUNT_DISABLED 的矛盾
   - 所在位置：3.3 节「SecurityFilterChain」exceptionHandling；10.2 节 ACCOUNT_DISABLED
   - 严重程度：一般
   - 改进建议：补充 EntryPoint 行为契约：携带 ACCOUNT_DISABLED 时返回 `Result.fail("ACCOUNT_DISABLED", "账户已被管理员停用")`；其余返回 `Result.fail("UNAUTHORIZED", "未认证或令牌已失效")`

5. **问题描述**：4.3 节与 5.1 节对 `nickname NOT NULL` 变更的描述不一致
   - 所在位置：4.3 节「修复方案」列表 vs 5.1 节 User.java 变更表
   - 严重程度：一般
   - 改进建议：在 5.1 节 User.java 变更表中新增 `nickname NOT NULL` 行

6. **问题描述**：8.2 节 H5 展平路由策略缺少 Layout 包容机制说明
   - 所在位置：8.2 节 H5「修复方案」列
   - 严重程度：一般
   - 改进建议：补充 Layout 包容机制的设计决策：Layout 在 App.vue 中包裹 `<router-view>`，或展平路由作为 Layout 子路由注册

7. **问题描述**：登录流程步骤 6 存在时序侧信道缺口
   - 所在位置：3.1.1 节步骤 6
   - 严重程度：一般
   - 改进建议：在步骤 6 中增加 dummy BCrypt 比对，消除与步骤 5/7 的时序差异；或在设计决策中明确承认此残余风险

8. **问题描述**：`LoginResponse` 中 `tokenType` 与 `expiresIn` 的定位不明确
   - 所在位置：5.2 节 LoginResponse、TokenRefreshResponse；6.2 节 JSON 示例
   - 严重程度：一般
   - 改进建议：在 5.2 节补充注释明确 `expiresIn` 为"access token 剩余有效秒数（从签发时计算）"，同步更新 6.2 节 JSON 示例字段注释


## 迭代第 7 轮

1. **问题描述**：异常刷新检测机制缺少可落地的实现细节（时间窗口、阈值、实现位置、类定义、告警方式均未定义）
   - 所在位置：4.2 节「Refresh Token 的安全补偿策略」第三项
   - 严重程度：严重
   - 改进建议：补充完整设计契约；定义时间窗口和阈值；指定检测逻辑实现位置；明确告警方式；或将此机制推迟到 Phase 2 并从当前设计的补偿策略列表中移除

2. **问题描述**：H6 修复方案推荐的 `inject('router')` 在 Pinia store 中不可用
   - 所在位置：8.2 节 H6「修复方案」列、17 节修订说明（v7）第 13 项
   - 严重程度：严重
   - 改进建议：保留现有 `createMenuStore(router, dynamicPageComponent)` 工厂模式，该模式已在 `shared/src/stores/menu.ts` 中实现并解决了循环依赖问题。修复方案应引用现有实现作为参考

3. **问题描述**：管理员标记密码过期的 API 端点缺失
   - 所在位置：3.4 节「密码变更强制策略」场景 2 vs 6.1 节接口清单
   - 严重程度：重要
   - 改进建议：二选一：(a) 在 6.1 节补充管理员专用端点 `PUT /api/users/{id}/password-expire`；(b) 在 3.4 节场景 2 中明确标注此功能属于管理端设计范围

4. **问题描述**：ProfileUpdateRequest.nickname 缺少空白字符串校验
   - 所在位置：5.2 节 ProfileUpdateRequest record 定义
   - 严重程度：一般
   - 改进建议：在 `nickname` 字段上补充 `@NotBlank(message = "昵称不能为空")` 注解

5. **问题描述**：MenuUpdateRequest 的 PUT 语义不明确（全量替换 vs 部分更新）
   - 所在位置：5.2 节 MenuUpdateRequest 定义及一致性校验说明
   - 严重程度：一般
   - 改进建议：明确声明更新语义：若采用局部更新，标注为 PATCH 或补充说明"省略的字段不更新"；若坚持全量替换，为所有字段补充说明空值处理策略

6. **问题描述**：登出端点在 token 过期场景下的行为未定义
   - 所在位置：3.1.4 节登出流程、4.4 节保护清单
   - 严重程度：一般
   - 改进建议：在 3.1.4 节补充说明后端登出错时前端 finally 块仍需清除本地 token 和用户数据

7. **问题描述**：LoginAttemptCleaner 类定义但无行为说明
   - 所在位置：2.1 节目录结构中 `LoginAttemptCleaner.java` 条目
   - 严重程度：一般
   - 改进建议：二选一：(a) 在 9.3 节或 4.1 节补充 `LoginAttemptCleaner` 的调度机制说明；(b) 若惰性清理已足够，从目录结构中删除该条目

## 迭代第 8 轮

1. **问题描述**：密码变更后"强制用户重新登录"语义与设计说明自相矛盾
   - 所在位置：3.1.6 节步骤 10 vs 3.1.6 节设计说明第二段
   - 严重程度：严重
   - 改进建议：二选一统一语义：要么删除"强制用户重新登录"表述并补充说明旧 token 可继续使用至自然过期（推荐），要么真正实现强制失效并更新设计说明
2. **问题描述**：PASSWORD_COMMON ErrorCode 在 Phase 1 中不可达，形成代码死分支
   - 所在位置：4.3 节密码策略表 vs 10.2 节 ErrorCode 表 PASSWORD_COMMON 行
   - 严重程度：严重
   - 改进建议：从 10.2/10.1 节删除 PASSWORD_COMMON 条目，或在保留时明确标注"Phase 2 启用"
3. **问题描述**：登录流程未从 User 实体获取 tokenVersion 用于 Refresh Token 签发，导致闭环断裂
   - 所在位置：3.1.1 节步骤 10 vs 3.2 节 Refresh Token Claims（含 tokenVersion）
   - 严重程度：严重
   - 改进建议：补充 JwtTokenProvider 从 User.tokenVersion 读取并嵌入 Refresh Token claims 的说明
4. **问题描述**：展平路由 name 唯一性保证策略存在运行时竞态隐患
   - 所在位置：8.2 节"name 唯一性保证策略"段
   - 严重程度：一般
   - 改进建议：改为确定性命名策略（permissionCode 转驼峰 + 前端应用前缀），或在保留运行时检测时补充冲突拒绝注册及 Fallback 行为
5. **问题描述**：登录成功后仅清除用户名维度的失败计数，IP 维度未处理
   - 所在位置：3.1.1 节步骤 8 vs 4.1 节 LoginAttemptTracker 双维度设计
   - 严重程度：一般
   - 改进建议：补充 IP 维度失败计数重置逻辑，或明确采用滑动窗口策略
6. **问题描述**：多 Tab 并发刷新失败的连锁错误处理未定义
   - 所在位置：7.4 节"401 静默刷新连续 3 次失败"相关描述
   - 严重程度：一般
   - 改进建议：明确失败计数器为全局共享（按时间窗口），或改为 Promise 链式重试
7. **问题描述**：expiresIn 在 TokenRefreshResponse 中语义未明确，与 LoginResponse 语义落差
   - 所在位置：5.2 节 TokenRefreshResponse 定义
   - 严重程度：一般
   - 改进建议：补充与 LoginResponse 一致的 expiresIn 语义说明
8. **问题描述**：GlobalRateLimitFilter 与 InMemoryRateLimitGuard 之间的委托关系未定义
   - 所在位置：4.1 节"全局 IP 限流（GlobalRateLimitFilter）"段
   - 严重程度：一般
    - 改进建议：明确委托关系，补充构造注入示例或职责分工边界说明

## 迭代第 9 轮

1. **问题描述**："连续"语义定义与实现机制不匹配——文档定义了严格的"连续失败"语义（中间不得插入非登录请求），但ConcurrentHashMap+AttemptRecord的实现仅支持时间窗口内累计计数，无法检测非登录请求插入的中断条件
   - 所在位置：4.1节「第二层：登录失败计数（LoginAttemptTracker）」实现方式描述及"连续"语义定义
   - 严重程度：严重
   - 改进建议：二选一：(a) 简化语义定义为"时间窗口内累计失败次数"，移除"中间不得插入非登录请求"表述；(b) 若坚持严格连续语义，需设计额外检测机制（如感知非登录请求或在Filter中插入心跳重置逻辑）并在文档中描述具体实现方案

2. **问题描述**：刷新端点未检查账户锁定状态形成绕过路径——账户锁定后`AuthServiceImpl.login()`拒绝登录，但`POST /api/auth/refresh`仅检查enabled/deleted和tokenVersion，未检查锁定状态，攻击者持有有效Refresh Token仍可绕过锁定
   - 所在位置：3.1.3节Token刷新流程、4.1节第三层「临时账户锁定」
   - 严重程度：一般
   - 改进建议：在3.1.3节步骤5之后增加账户锁定检查，若被锁定返回ACCOUNT_LOCKED；若为有意设计决策需在11节设计决策表中记录

3. **问题描述**：版本号不一致——文档标题为v9，修订说明递增至v10，文件名暗示v9_copy_from_v8，三处版本号不一致，多人协作或后续迭代时易造成混淆
   - 所在位置：文档标题vs修订说明vs文件名
   - 严重程度：一般
   - 改进建议：统一版本号体系，将修订说明最后一节编号与文档版本号对齐，或明确说明两者对应关系

4. **问题描述**："清除SecurityContext作为安全最佳实践"说法不准确——SecurityContext为请求作用域，在Service层手动清除实际不产生额外安全效果，夸大描述可能误导读者
- 所在位置：3.1.6节步骤10
- 严重程度：一般
- 改进建议：移除"作为安全最佳实践"表述，修改为对实际效果的准确描述，或直接删除此步骤

## 迭代第 10 轮

1. **问题描述**：速率限制表头声称限流维度为「同一IP（任意API路径）」，但GlobalRateLimitFilter将login/refresh端点列入白名单排除，且refresh端点无独立限流策略
   - 所在位置：4.1节速率限制表及GlobalRateLimitFilter描述
   - 严重程度：严重
   - 改进建议：修正表头为「同一IP（除login/refresh外的一般API路径）」；为refresh端点增加独立限流策略（如30次/60秒），或在设计决策中明确此风险
2. **问题描述**：登出端点文档描述请求体可选携带refreshToken，但RefreshTokenRequest record标注@NotBlank，Controller层处理方式未指定
   - 所在位置：3.1.4节、4.4节、5.2节、6.1节
   - 严重程度：严重
   - 改进建议：补充Controller层签名说明，推荐使用@RequestBody(required=false)并判断null，或改为独立header传递refreshToken
3. **问题描述**：SlidingWindowCounter的接口契约（锁机制、窗口精度）未定义；两套限流器返回相同ErrorCode时前端无法区分触发来源
   - 所在位置：4.1节
   - 严重程度：一般
   - 改进建议：补充SlidingWindowCounter接口契约说明；明确ErrorCode覆盖行为或确认Phase 2合并到Redis
4. **问题描述**：密码变更后前端恢复流程（GET /api/auth/me → GET /api/menu/tree → 跳转首页）未覆盖中间步骤失败的异常场景
   - 所在位置：3.4节、7.4节
   - 严重程度：一般
   - 改进建议：补充异常场景处理策略：任一失败时显示loading/错误状态而非直接跳转首页；定义重试机制
5. **问题描述**：8.3节A1和A3行缺少「潜在副作用」和「影响范围」列，与8.1/8.2节格式不一致
   - 所在位置：8.3节
   - 严重程度：一般
   - 改进建议：为A1补充「潜在副作用：无（DDL约束已存在）」「影响范围：schema.sql」；为A3补充副作用说明和影响范围

## 迭代第 11 轮

1. **问题描述**：登录流程未定义账户锁定时的返回行为，步骤2检查锁定状态后无锁定命中分支
   - 所在位置：3.1.1节步骤2-3；4.1节第三层
   - 严重程度：严重
   - 改进建议：在步骤2后补充锁定命中分支，返回ErrorCode.ACCOUNT_LOCKED（HTTP 429），消息"账户已锁定，请15分钟后重试"

2. **问题描述**：IP维度锁定检查未在登录流程中显式体现，IP被锁定后攻击者可切换用户名继续尝试
   - 所在位置：3.1.1节步骤2；4.1节第二层锁定表+第三层描述
   - 严重程度：严重
   - 改进建议：在3.1.1节步骤1和步骤2之间或步骤2中补充IP维度锁定检查，明确登录入口处同时校验用户名和IP双维度锁定状态

3. **问题描述**：M17/M18/M19未出现在缺陷追踪表，实施阶段可能遗漏回归验证
   - 所在位置：8.1节、8.2节、8.3节
   - 严重程度：一般
   - 改进建议：在8.1节补充M17/M18/M19三条目的追踪行，包含状态、修复方案、潜在副作用、影响范围四列

## 迭代第 12 轮

1. **问题描述**：Role.sort 字段缺少 Java 默认值与 NOT NULL 约束，JPA 持久化存在不一致风险
   - 所在位置：5.1 节 Role.java 变更表；4.3 节 NOT NULL 约束状态确认表
   - 严重程度：一般
   - 改进建议：在 Role.java 变更表中补充 `@Column(nullable=false) private Integer sort = 0;`，DDL 中 `sys_role.sort INT NOT NULL DEFAULT 0`
2. **问题描述**：SlidingWindowCounter 声明为"包级私有"但被跨包复用，存在可见性矛盾
   - 所在位置：4.1 节「SlidingWindowCounter 契约」段
   - 严重程度：严重
   - 改进建议：将 SlidingWindowCounter 提升为 public 类放在共享包，或将两限流器归入同一包，或各自独立实现
3. **问题描述**：密码变更流程中清除 SecurityContext 操作无实际安全效果，说明不准确
   - 所在位置：3.1.6 节步骤 10
   - 严重程度：一般
   - 改进建议：删除该步骤或将说明修正为仅清除当前请求上下文，不影响后续请求
4. **问题描述**：PasswordPolicy 接口缺少方法签名定义，无法直接指导编码
   - 所在位置：1.3 节核心抽象一览；4.3 节密码策略
   - 严重程度：一般
   - 改进建议：在 1.3 节或 4.3 节补充 `ErrorCode validate(String password, String username)` 方法签名
5. **问题描述**：菜单删除操作未定义子菜单处理策略（级联/拦截/置空）
   - 所在位置：6.1 节接口清单，DELETE /api/menu/{id}
   - 严重程度：一般
   - 改进建议：有子菜单时阻止删除返回 400 + CHILDREN_EXIST 错误码，要求先删除子菜单
6. **问题描述**：`expiresIn` 字段语义自相矛盾——"剩余有效秒数"与"从签发时计算"冲突
   - 所在位置：5.2 节 LoginResponse 及 TokenRefreshResponse 定义
   - 严重程度：一般
   - 改进建议：统一语义为固定 TTL 或真实剩余时间并修正描述
7. **问题描述**：`/api/auth/refresh` 端点调用建议未文档化——推荐不携带 Authorization header
   - 所在位置：3.1.3 节 Token 刷新流程；7.2 节包 B → 包 D 契约
   - 严重程度：一般
   - 改进建议：在 7.2 节和 3.1.3 节补充不携带 Authorization header 的约束说明
8. **问题描述**：刷新端点未加入 PasswordChangeCheckFilter 白名单，存在理论防护盲区
   - 所在位置：3.3 节 PasswordChangeCheckFilter 行为契约
   - 严重程度：轻微
   - 改进建议：将 /api/auth/refresh（POST）加入 PasswordChangeCheckFilter 白名单

## 迭代第 13 轮

1. **问题描述**：Refresh 端点可绕过 passwordChangeRequired 强制约束，用户登录后即使不修改密码，也可通过 POST /api/auth/refresh 反复获取新令牌，维持认证状态
   - 所在位置：3.1.3 节 Token 刷新流程；3.3 节 PasswordChangeCheckFilter 白名单；3.4 节 passwordChangeRequired 访问控制
   - 严重程度：严重
   - 改进建议：在 AuthServiceImpl.refresh() 中增加 passwordChangeRequired 检查；若用户在 DB 中 passwordChangeRequired=true，拒绝刷新并返回 PASSWORD_CHANGE_REQUIRED（403）
2. **问题描述**：3.4 节白名单与 3.3 节/3.1.2 节白名单不一致，3.4 节未随 v13 修订同步更新
   - 所在位置：3.4 节 passwordChangeRequired 访问控制（白名单列表）
   - 严重程度：重要
   - 改进建议：将 3.4 节白名单补充 POST /api/auth/refresh 条目，与 3.1.2 节和 3.3 节保持一致
3. **问题描述**：PasswordChangeService 和 CurrentUser 接口缺少方法签名，无法直接指导编码实现
   - 所在位置：1.3 节核心抽象一览；3.4 节
   - 严重程度：重要
   - 改进建议：补充 PasswordChangeService 接口方法签名（isChangeRequired/markChangeRequired/clearChangeRequired）和 CurrentUser 接口方法签名（getUserId/getUsername/getUserType）
4. **问题描述**：包 A 实体移至 common-module-impl 的结构变更影响评估不完整，未评估现有业务模块对包 A 实体的直接引用，未说明业务模块获取用户数据的途径
   - 所在位置：2.1 节、2.2 节、7.1 节、7.2 节
   - 严重程度：重要
    - 改进建议：补充评估现有代码中对 User/Role/Post/PermissionFunction 实体的外部引用；明确业务模块访问用户数据的途径；补充迁移步骤
 5. **问题描述**：InMemoryRateLimitGuard 和 GlobalRateLimitFilter 均描述为使用 ConcurrentHashMap<String, SlidingWindowCounter>，表述雷同，开发者可能误以为两者共享同一个计数器实例
    - 所在位置：4.1 节 InMemoryRateLimitGuard 和 GlobalRateLimitFilter 实现描述
    - 严重程度：重要

## 迭代第 14 轮

1. **问题描述**：`UserFacade`门面接口缺少完整定义，未出现在核心抽象一览表、目录结构中，未定义任何方法签名，未说明与`CurrentUser`的职责分工
   - 所在位置：2.2节依赖规则、1.3节核心抽象一览、2.1节目录结构
   - 严重程度：严重
   - 改进建议：在1.3节补充`UserFacade`条目，2.1节目录树增加`UserFacade.java`，定义至少2-3个方法签名，补充与`CurrentUser`的分工说明
2. **问题描述**：ACCOUNT_LOCKED错误消息在认证流程描述（3.1.1节）与ErrorCode表（10.2节）存在语义矛盾——流程描述按锁定维度返回不同时长消息，ErrorCode表仅有一条固定消息
   - 所在位置：3.1.1节步骤3 vs 10.2节ErrorCode表
   - 严重程度：一般
   - 改进建议：二选一统一：(a) 在10.2节为ACCOUNT_LOCKED新增IP维度消息行，标注"根据锁定维度返回对应消息"；(b) 在10.2节补充注释说明消息动态生成
3. **问题描述**：tokenVersion比对步骤（3.1.3节步骤5 vs 步骤9）存在实现歧义——未明确步骤9是从DB重新查询还是复用步骤5已加载的实体值，影响密码变更即时生效的安全保证
   - 所在位置：3.1.3节步骤5 vs 步骤9
   - 严重程度：一般
   - 改进建议：明确实现决策：(a) 若复用步骤5值，在设计决策表记录此权衡；(b) 若重新查询，步骤9改为"重新从DB加载用户当前tokenVersion并比对"，移除步骤5的冗余预加载
4. **问题描述**：`PasswordChangeCheckFilter`白名单路径匹配策略未定义
   - 所在位置：3.3节PasswordChangeCheckFilter行为契约
   - 严重程度：一般
   - 改进建议：明确指定使用`AntPathRequestMatcher`进行路径匹配，并在SecurityConfig中给出Filter注册示例代码段
5. **问题描述**：`MenuUpdateRequest`的PATCH语义与Java record反序列化存在歧义
   - 所在位置：5.2节MenuUpdateRequest
   - 严重程度：一般
   - 改进建议：二选一：(a) 改用传统POJO+字段标记组；(b) 正式声明降级为全量替换语义（PUT）
6. **问题描述**：Refresh端点在禁用用户场景不递增IP维度失败计数
   - 所在位置：3.1.3节步骤7
   - 严重程度：一般
   - 改进建议：在步骤7中增加IP维度的失败计数递增
7. **问题描述**：登录成功后IP维度计数器清空前置条件未定义，NAT/代理共享IP场景下存在局限性
   - 所在位置：3.1.1节步骤8
   - 严重程度：一般
   - 改进建议：在11节设计决策表增加一行记录此局限性，补充IP来源说明（取`X-Forwarded-For`或`X-Real-IP`头）
8. **问题描述**：`ProfileUpdateRequest.phone`可选性未声明
   - 所在位置：5.2节ProfileUpdateRequest
   - 严重程度：轻微
   - 改进建议：在`phone`和`email`字段补充注释说明"可选字段"
9. **问题描述**：`PasswordChangeRequest.oldPassword`缺少`@Size(max=128)`约束
   - 所在位置：5.2节PasswordChangeRequest
   - 严重程度：轻微
   - 改进建议：为`oldPassword`补充`@Size(max=128)`约束
   - 改进建议：明确注释两套计数器实例相互独立，使用不同的 key 空间和窗口参数
