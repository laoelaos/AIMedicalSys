# Phase 1 包 ABD 修复 + 包 B OOD 补全

## 背景

基于 Docs/03_roadmap.md，Phase 0（最小化骨架）已完成，Phase 1（基础设施+统一认证与权限）中：

- **包 A**（用户/角色/岗位/菜单/字典数据建模与初始 SQL）：已完成，但存在数据建模缺陷（password NOT NULL 约束缺失、deleted 列 NOT NULL 不一致、enabled/visible 字段缺 Java 默认值、缺少集成测试）
- **包 B**（统一认证模块实现：登录与令牌鉴权）：已实现骨架代码，但无独立的 OOD 设计文档；且代码层面存在多处安全和设计缺陷（用户禁用后 token 仍有效、无防暴力破解、登出 No-Op、Access Token 自刷新、userId 提取逻辑重复等）
- **包 D**（医生端与管理端登录/菜单动态化）：已有 Docs/Phase1-PackageD-Design.md 设计文档，但前端实现存在缺陷（logout 缺 try-finally、导航守卫竞态、401不触发自动刷新、菜单转换仅支持两级等）

包 A 的数据实体（User、Role、Post、Function）与包 B 的认证模块有强依赖关系；包 D 的前端功能依赖包 B 的后端 API。三个包交织在一起，需统一修复。

## 已审查的问题列表

来源：Harness/reviews/202606252200_phase1_pkgBD_review/todo.md

### 🔴 严重问题
1. 用户禁用后 token 仍然有效（B 包后端，JwtAuthenticationFilter/AuthServiceImpl 未检查 enabled）
2. 登录接口无防暴力破解（B 包后端）
3. userId 提取逻辑重复 4 次（B 包后端，DRY 违规）
4. 后端登出为 No-Op，无 token 黑名单（B 包后端）
5. Access Token 自刷新，无 refresh token 轮换（B 包后端）
6. 前端 logout() 缺少 try-finally（D 包前端）
7. 导航守卫竞态（race condition）（D 包前端）
8. CSRF 完全禁用且无说明（B 包后端）

### 🟡 重要问题
1. UserRepository.findByUsername() 返回非 Optional（B 包后端）
2. JWT SecretKey 应缓存（B 包后端）
3. 401 拦截器不会触发自动 refresh（D 包前端）
4. convertMenusToRoutes 硬编码父路由名 'Layout'（D 包前端）
5. convertMenusToRoutes 仅处理两级菜单（D 包前端）
6. 循环依赖脆弱（D 包前端）
7. 导航守卫阻塞 UI（D 包前端）
8. 密码复杂度弱（B 包后端）
9. Swagger/API 文档公开暴露（整体）
10. JWT Filter 静默跳过无效 token（B 包后端）
11. ProfileUpdateRequest 无手机号格式校验（B 包后端）
12. 用户/角色变更后 token 仍然有效（B 包后端）

### 🔵 一般问题
1. buildUserInfoResponse 私有方法不可测试（B 包后端）
2. updateProfile() 多余 save() 写回（B 包后端）
3. TokenStore 死代码（B 包后端 - admin 模块）
4. AuthController 直接依赖 JwtUtil（B 包后端）
5. 医生岗位取 iterator().next()（B 包后端）
6. DTO 未用 Java 17 record（B 包后端）
7. Entity 风格混用（B 包后端）
8. Function.java 类名冲突（B 包后端）
9. login() 全量查询 + 懒加载（B 包后端）
10. Phase0/Phase1 SecurityConfig 并发激活（整体）
11. activeMenu 状态冗余（D 包前端）
12. SidebarBase 直接用 useRoute()（D 包前端）
13. apiGet<T> catch 分支类型不安全（D 包前端）
14. BusinessError.isBusinessError 可选属性不严谨（D 包前端）
15. 无前端输入校验（D 包前端）
16. 用户信息明文存 localStorage（D 包前端）
17. Actuator 端点暴露（整体）
18. 401 响应体结构不一致（整体）
19. 用户不存在时菜单返回空数组（B 包后端）

### 包 A 的已知问题（来源于 Docs/Diagnosis/impl/03_phase1A_report.md）
1. password 字段缺少 NOT NULL 约束（User.java, schema.sql）
2. DDL 中 deleted 列 NOT NULL 约束与 BaseEntity 不一致（16张表）
3. enabled/visible 布尔字段跨实体缺少默认值（User/Role/Post/Function）
4. 缺少 User/Role/Post 集成测试（EntityMappingIT）

## 任务要求

### 1. 产出包 B 的完整 OOD 设计文档
参照 Docs/04_ood_phase0.md（Phase 0 的架构级 OOD 风格），为 Phase 1 包 B（统一认证模块）编写独立的 OOD 设计文档，包括：

- **概述**：设计目标、整体架构思路、核心抽象一览
- **模块划分**：包 B 子模块的目录结构与依赖方向
- **核心设计**
  - 认证流程（登录、登出、Token 刷新、获取当前用户）
  - JWT 令牌设计（Claims 结构、签名算法、过期策略）
  - Spring Security 配置（SecurityFilterChain、JwtAuthenticationFilter、认证入口点）
  - 用户状态管理（enabled 禁用检查、角色刷新策略）
- **安全设计**
  - 防暴力破解方案（速率限制、失败计数、账户锁定）
  - Token 黑名单/轮换设计
  - 密码策略（复杂度、加密、NOT NULL 约束）
  - API 端点保护清单
- **数据模型**：新增/变更的实体与 DTO
- **API 接口设计**：完整接口清单、请求/响应格式
- **与包 A 和包 D 的协作关系**

### 2. 修复包 A、B、D 中发现的设计缺陷
基于审查报告中的问题清单，在 OOD 层面给出修复方案。对于每个问题需说明：
- 当前状态
- 修复方案
- 潜在副作用
- 影响范围

### 3. 包 A/B/D 间的协作边界
在 OOD 中明确说明三个包的交互关系：
- 包 A（数据实体）→ 包 B（认证服务）的依赖
- 包 B（后端 API）→ 包 D（前端消费）的契约
- 包 A 实体变更对包 B 认证逻辑的影响

## 现有参考文档
- Docs/04_ood_phase0.md - Phase 0 OOD 设计方案（风格参考）
- Docs/Phase1-PackageD-Design.md - 包 D 现有设计文档
- Docs/Diagnosis/impl/03_phase1A_report.md - 包 A 问题诊断
- Harness/reviews/202606252200_phase1_pkgBD_review/todo.md - 问题追踪
- Harness/reviews/202606252200_phase1_pkgBD_review/report_backend_pkgB.md - B 包审查详报
- Harness/reviews/202606252200_phase1_pkgBD_review/report_frontend_pkgD.md - D 包审查详报
- Harness/reviews/202606252200_phase1_pkgBD_review/report_integration_security.md - 集成安全审查
