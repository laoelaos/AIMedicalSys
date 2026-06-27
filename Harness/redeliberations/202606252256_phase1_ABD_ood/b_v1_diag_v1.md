# 质量审查报告：Phase 1 包 A/B/D 统一修复与包 B OOD 设计方案 (v1)

## 审查范围

审查针对 `a_v1_design_v1.md` 进行。任务需求为产出包 B 的完整 OOD 设计文档，修复包 A/B/D 的设计缺陷，并明确三包的协作边界。审查侧重需求响应充分度、事实性、深度完整性及落地可实施性。

---

## 发现的问题

### P1. 多处事实错误：声称 "当前状态" 缺失的约束和默认值，实际代码已存在

**所在位置**：5.1 实体变更表（4 张表）、8.3 包 A 数据建模问题

**问题描述**：设计文档在多处声称代码"当前状态"缺少 NOT NULL 约束或 Java 默认值，但实际代码中这些约束和默认值均已存在：

| 设计文档声称 | 实际代码状态 |
|---|---|
| `User.password` 缺少 `@Column(nullable = false)` | `User.java:32-33` 已有 `@Column(nullable = false)` |
| `schema.sql` 中 `password` 列缺少 `NOT NULL` | `schema.sql:16` 已有 `NOT NULL` |
| `enabled` 跨实体缺少 Java 默认值 | `User.java:43`、`Role.java:24`、`Post.java:27`、`Function.java:32` 全部已有 `= true` |
| `visible` 缺少 Java 默认值 | `Function.java:51` 已有 `= true` |
| `deleted` 列 NOT NULL 不一致（16 张表） | `BaseEntity.java:33-34` 已有 `@Column(nullable = false)` + `= false`；schema.sql 中 **全部 16 张含 `deleted` 列的表** 均已使用 `NOT NULL DEFAULT 0` |

**严重程度**：严重

**改进建议**：设计应基于当前代码的实际状态编写，而非照搬历史诊断报告。对于确实需要修复的问题应如实描述当前状态；对于已满足的问题应标注"已完成"而非"缺失"。错误的状态描述会误导开发者做不需要的变更。

---

### P2. 密码策略对已有弱密码用户无过渡方案

**所在位置**：4.3 密码策略；8.1 H8 修复方案

**问题描述**：设计新增了"至少包含大写字母、小写字母、数字、特殊字符中的 **3 种**"的密码复杂度要求，但在"潜在副作用"中仅说"已有弱密码用户修改密码时需适应性变更"。对于已有不满足新策略的密码的用户，在修改密码之前他们的旧密码仍然是合法的——这是正确的安全实践。但设计缺少以下关键机制：如何强制弱密码用户在下次登录时修改密码？如果仅在修改密码时校验，则弱密码将永久存在。设计未定义"首次登录强制修改密码"或"管理员标记密码过期"的机制，尽管在 ErrorCode 表（10.2）中定义了 `PASSWORD_CHANGE_REQUIRED` 枚举，但全文未在任何流程中使用该枚举。

**严重程度**：重要

**改进建议**：在认证流程（3.1.1 登录）中增加一步：密码策略升级过渡期内，登录成功后检查密码是否满足新策略，不满足则要求用户设置新密码（返回 `PASSWORD_CHANGE_REQUIRED`）。或在首次登录/管理员强制要求时触发。

---

### P3. API 中 `PUT /api/auth/password` 缺少对应的 DTO 定义

**所在位置**：6.1 接口清单 → `PUT /api/auth/password`；5.2 DTO 列表

**问题描述**：API 接口清单列出了 `PUT /api/auth/password` 端点，请求体为 `PasswordChangeRequest`，但在 5.2 节"DTO 列表"中未定义 `PasswordChangeRequest` DTO。开发者无法从该设计文档获知该 DTO 的字段结构。

**严重程度**：严重

**改进建议**：在 5.2 节中补充 `PasswordChangeRequest` 的 Java 17 record 定义，至少包含 `oldPassword`、`newPassword` 字段及其校验注解。

---

### P4. `POST /api/auth/refresh` 缺少请求体定义

**所在位置**：6.1 接口清单；3.1.3 Token 刷新流程

**问题描述**：Token 刷新流程（3.1.3）描述需要前端提交 Refresh Token，但在接口清单（6.1）中 `POST /api/auth/refresh` 的"请求体/参数"列为 `—`（空）。刷新操作需要提交 Refresh Token（通过请求体或 header），无请求体定义的 API 设计让下游消费者（包 D 前端）无法确定如何传递 Refresh Token。

**严重程度**：重要

**改进建议**：明确请求体为 `RefreshTokenRequest { String refreshToken }`，或在 SecurityConfig 中允许 Refresh Token 通过 header 传递（需设计说明理由）。`RefreshTokenRequest` 在目录结构（2.1）中标注为"Phase 2 扩展"但 Phase 1 的 refresh 流程就需要使用。

---

### P5. 内存黑名单方案严重低估风险：60M+ 条目的可行性未论证

**所在位置**：4.2 Token 黑名单设计

**问题描述**：设计估算 Refresh Token 黑名单峰值约 6048 万条，结论为"Phase 1 可接受"但未提供任何内存占用估算。按保守估计（每条 String+Long ≈ 100 字节 + ConcurrentHashMap 开销 ≈ 200 字节），6048 万条需要约 12GB+ 堆内存，远超典型 JVM 堆配置（通常 1-4GB）。**这不是可接受方案**，即使按更合理的 10% 请求为刷新操作估算（约 600 万条），也需要 1.2GB+。

**严重程度**：严重

**改进建议**：方案二选一：(a) 将 Refresh Token 黑名单下放到 Phase 2（Redis）并修改设计——Phase 1 直接使用 Redis 或放弃 Refresh Token 黑名单仅做 Access Token 黑名单（Access Token 15 分钟窗口仅 9 万条，内存可承受）；(b) 如果 Phase 1 强制使用内存方案，需设置最大条目上限 + 预衰退策略（如不缓存 Refresh Token 黑名单，改用 Refresh Token 轮换时仅拒绝旧 jti + 在 AuthServiceImpl 层短期缓存拒绝记录）。

---

### P6. `UserInfoResponse` 与前端现有 `UserInfo` 接口存在字段不兼容

**所在位置**：5.2 `UserInfoResponse`；7.2 包 B → 包 D 契约

**问题描述**：设计定义的 `UserInfoResponse` 字段（`nickname`、`userType`）与前端现有 `UserInfo` TypeScript 接口（`realName`、`role`）不匹配。前端 `types/index.ts:33-40` 使用 `realName` 和 `role`，而设计改用 `nickname` 和 `userType`。这是一个破坏性契约变更，但设计文档在 7.2 节的契约表中仅笼统地说"UserInfoResponse ↔ UserInfo TypeScript interface"，未声明此变更。

**严重程度**：重要

**改进建议**：与前端接口对齐——保持使用 `realName` 和 `role` 字段名，或将变更显式标记为 Breaking Change 并在文档中说明前端同步修改方案。

---

### P7. `LoginResponse` 字段名变更（`token` → `accessToken`）未声明为 Breaking Change

**所在位置**：5.2 `LoginResponse`；8.1 C5 修复方案

**问题描述**：设计将 `LoginResponse` 的 `token` 字段改为 `accessToken` 并新增 `refreshToken` 字段，但现有前端 `LoginResponse` 接口（`types/index.ts:66-71`）使用 `token` 字段名。这是一次破坏性的 API 契约变更，但 7.2 节的协作契约表中未声明此变更，8.1 C5 的"影响范围"中也只说了"前后端同步改造"而未指出具体需修改的接口字段名。

**严重程度**：重要

**改进建议**：在 7.2 节或 8.1 C5 中明确标注字段名变更及其对前端的影响范围（需同步修改 `LoginResponse` 接口、auth store、axios 拦截器中的 token 提取逻辑）。

---

### P8. `JwtAuthenticationFilter` 移动位置但无迁移步骤描述

**所在位置**：2.1 目录结构；2.2 模块依赖方向

**问题描述**：设计将 `JwtAuthenticationFilter` 从当前 `application/config/` 移动到 `common-module-impl/auth/security/` 下。当前 `SecurityConfigPhase1` 通过直接 `@Component` 扫描 + `@Import` 使用该 Filter。移动后，`SecurityConfigPhase1`（位于 `application` 模块）和 `JwtAuthenticationFilter`（位于 `common-module-impl`）的跨模块依赖关系如何建立？设计仅说"通过 `@ComponentScan` 或 `@Import` 引入"，但未说明具体方案。此外，`JwtAuthenticationFilter` 当前直接依赖 `JwtUtil`（`JwtAuthenticationFilter.java:37` `private final JwtUtil jwtUtil`），设计将 `JwtUtil` 重构为 `JwtTokenProvider` 也需考虑 Filter 的构造依赖变更。

**严重程度**：重要

**改进建议**：补充 `JwtAuthenticationFilter` 的迁移步骤：当前 `application/config/JwtAuthenticationFilter.java` → 删除/移动 → 新位置类定义 → `SecurityConfigPhase1` 中声明过滤器 Bean 的方式（`@Import`/`@ComponentScan`/`@Bean` 方法）。同时说明 `JwtUtil` 到 `JwtTokenProvider` 的过渡方案。

---

### P9. 包 A 数据建模问题重复诊断已修复项（事实性复核缺失）

**所在位置**：8.3 包 A 数据建模问题

**问题描述**：设计完全复用了任务需求中"包 A 已知问题"列表，但未对当前代码状态进行复核。实际代码已满足 password NOT NULL、deleted NOT NULL、enabled/visible 默认值等要求。设计文档作为 OOD 文档，应当基于当前代码基线给出差异分析（gap analysis），而非机械引用旧诊断报告。

**严重程度**：严重

**改进建议**：复核代码后重新填写 8.3 表的"当前状态"列。对于已修复的项标注"已完成"，仅有未修复项（如缺少集成测试 A4）保留修复计划。或者，将这些误判项整体移至"已验证通过，无需变更"段落。

---

### P10. `SidebarBase` 直接用 `useRoute()` 修复方案不完整

**所在位置**：8.2 M12

**问题描述**：设计描述"SidebarBase 通过 props 接收当前路径，由父组件通过 useRoute() 传入，父组件工作量增加"。但未说明具体 props 名称、类型，也未评估此变更对 LayoutBase 等所有使用 SidebarBase 的组件的影响范围。提案过于模糊，无法直接指导编码实现。

**严重程度**：一般

**改进建议**：补充 props 定义示例（如 `interface SidebarBaseProps { currentPath: string }`），并列出所有受影响的父组件文件清单。

---

### P11. 前端用户信息存储修复方案前后端不一致

**所在位置**：8.1 M12（后端问题） vs 8.2 M16（前端问题）

**问题描述**：同一个问题"用户信息明文存 localStorage"被分别在包 B 后端问题和包 D 前端问题中列出（各占一个条目）。修复方案基本相同（"仅存储最小标识"），但角度不同。这不构成严重错误，但说明问题分类整理不够清晰。

**严重程度**：一般

**改进建议**：合并为一个条目（归属包 D 前端），在包 B 后端问题表格中引用前端条目即可。

---

### P12. `Role.java`/`Post.java`/`Function.java` 的 `enabled` 字段缺少 `@Column(nullable = false)`

**所在位置**：5.1 实体变更表

**问题描述**：设计修复方案仅关注 Java 默认值，未检查 `@Column(nullable = false)` 注解一致性。`User.java:42-43` 既有 `@Column(nullable = false)` 又有 `= true`，但 `Role.java:24`、`Post.java:27`、`Function.java:32` 只有 `= true` 而没有 `@Column(nullable = false)`。相应的 schema.sql 中 `enabled` 列仅有 `DEFAULT 1` 没有 `NOT NULL`。这是一个遗漏的约束一致性修复点。

**严重程度**：一般

**改进建议**：为 Role/Post/Function 的 enabled 字段补加 `@Column(nullable = false)`，同步更新 schema.sql 对应列添加 `NOT NULL`。

---

### P13. `refresh` 端点的认证要求矛盾

**所在位置**：3.3 SecurityFilterChain；4.4 API 端点保护清单

**问题描述**：SecurityConfig 中将 `/api/auth/refresh` 设为 `permitAll`（3.3 节第 236 行），因为刷新端点需要在 token 过期后仍可访问（用户需通过 Refresh Token 获取新 Access Token）。但 4.4 节保护清单中 refresh 的"认证要求"为 `JWT`——这两处描述矛盾。若 refresh 需要有效 JWT，那么 Access Token 过期后如何获取新的？

**严重程度**：重要

**改进建议**：统一为 `permitAll`（3.3 节是正确的），在流程说明（3.1.3）中明确 Refresh Token 通过请求体（而非 Authorization header）传递，Filter 不拦截该端点。4.4 节需修正。

---

### P14. `convertMenusToRoutes` 递归化方案与路由注册策略的矛盾未解决

**所在位置**：8.2 H5

**问题描述**：设计建议将 `convertMenusToRoutes` 改为递归以支持任意深度菜单。但当前路由注册策略是 `routerInstance.addRoute('Layout', route)`——所有路由被展平为 Layout 的子路由。对于三层以上菜单，正确的做法是使用嵌套的 `children` 而非 `addRoute('Layout', ...)`。设计仅说"递归"而未说明路由注册策略是否需要同步变更。H5 与 H4（Layout 名称参数化）存在耦合——若改为嵌套 children，则 H4 的参数化方案也可能需要调整。

**严重程度**：重要

**改进建议**：澄清递归化后的路由注册策略：是继续使用 `addRoute('Layout', ...)` 展平所有中间节点，还是改用嵌套 `children` 声明？二者对前端路由结构的影响不同，需给出明确方案。

---

## 整体质量评价

设计文档在架构覆盖面上较为全面（认证流程、模块划分、安全设计、协作关系均有涉及），但其核心问题在于**事实性验证不足**和**深度不足以直接指导实现**：

1. **事实性错误集中（P1, P9）**：多处"当前状态"未检验实际代码，直接引用历史诊断报告，这会间接导致开发者做无意义的工作。设计文档应反映「当前代码的状态是什么、需要改成什么」，而非「报告说过什么不对」。
2. **关键 API 契约遗漏（P3, P4, P6, P7）**：缺少 PasswordChangeRequest DTO、refresh 无请求体定义、UserInfo 和 LoginResponse 字段与前端不兼容。这些问题会导致前后端联调阶段发现重大契约断裂。
3. **方案可行性质疑（P5, P14）**：6048 万条黑名单内存占用未论证，递归菜单与路由注册的耦合未解决。这些在编码阶段才会暴露的设计矛盾应在上游解决。
4. **过渡方案缺失（P2, P8）**：密码策略升级的过渡期处理、Filter 跨模块迁移的具体步骤均未说明。

修复优先级：P1/P3/P5/P9（严重）> P2/P4/P6/P7/P8/P13/P14（重要）> P10/P11/P12（一般）。

---

## 完成信号

DIAG_WRITTEN:c:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606252256_phase1_ABD_ood/b_v1_diag_v1.md
主Agent请勿阅读产出文件内容，直接将路径转发给相关方。
