# 诊断报告：Docs/04_ood_phase0.md OOD 文档问题定位

## 检测范围

对照 Docs/03_roadmap.md Phase 0 的阶段边界与范围定义，检查 OOD 文档中的：
- 偏离路线图阶段边界的问题
- 定义矛盾
- 事实错误
- 逻辑错误

---

## 问题一（偏离路线图 · High）：Phase 0 中包含了 Spring Security 认证基础设施

**位置**：§3.3（LoginUser 类定义）、§4.5（SecurityConfigPhase0、UserDetailsService、AuthenticationEntryPoint、AccessDeniedHandler、PasswordEncoder、CORS 配置声明）

**现象**：OOD 文档在 Phase 0 范围中定义了完整的认证基础设施组件，包括：
- `LoginUser` —— User 实体的 Spring Security UserDetails 适配器
- `UserDetailsService` —— 通过 UserRepository 加载 User 构造 LoginUser
- `SecurityConfigPhase0` + Phase 1 的 `SecurityFilterChain` 骨架
- `AuthenticationEntryPoint`、`AccessDeniedHandler`、`PasswordEncoder` 共享配置
- CORS `CorsConfigurationSource` Bean 声明

**路线图约束**：
- Phase 0 范围：**"数据与权限模型骨架：数据实体基类与权限模型就位"**—— 仅包含实体（Entity）层面的骨架定义
- Phase 1 范围：**"统一认证：登录（密码+令牌鉴权）、刷新 Token、登出、当前用户信息接口"** 和 **"权限矩阵：角色—岗位—功能三级权限模型落地"**

**根因**：将"数据权限模型骨架"误扩展为"认证基础设施骨架"。"权限模型"指实体和关系（User-Role-Post-Function 的 JPA Entity 定义），不包括 Spring Security 适配器、认证过滤链、认证入口点等运行时认证基础设施。LoginUser、UserDetailsService、SecurityConfig 等是 Phase 1 "统一认证"的实现基础，应在 Phase 1 OOD 中定义。

**影响**：在 Phase 0 提前冻结认证基础设施的实现方案（如 UserDetailsService 的实现路径、SecurityConfig 的 profile 切换机制），限制了 Phase 1 的架构选择空间；同时造成 Phase 0 骨架中包含其职责范围外的基础设施代码。

---

## 问题二（偏离路线图 · High）：Phase 0 中冻结了跨模块门面接口 PermissionService

**位置**：§3.3（PermissionService 接口定义与注释代码）、§8.4（跨模块调用规范中的 PermissionService 示例）

**现象**：`common-module-api` 子模块中定义了 PermissionService 门面接口，包含 `getUserById(Long userId)` 和 `getUserPermissions(Long userId)` 两个完整方法签名，指定了返回类型 `UserDTO` 和 `Set<String>`、权限编码格式 `"{module}:{action}"`。

**路线图约束**：
- Phase 0 **"明确不包含"**：**"模块级接口契约冻结（在对应阶段启动前冻结）"**
- "模块级接口契约"指模块间跨模块调用的接口合约，PermissionService 作为 common-module-impl 对外暴露的门面接口，是典型的模块级接口契约

**根因**：OOD 试图通过"Phase 0 暂不实现任何跨模块调用"和"Phase 0 仅冻结接口形态"来避免触及"冻结"定义。但接口方法签名、参数类型、返回类型、权限编码格式在 Phase 0 中已确定，这本身就是契约冻结——实现是否就绪不影响契约已冻结的事实。PermissionService 应在 Phase 1（权限矩阵落地阶段）首次定义。

---

## 问题三（定义矛盾 · Medium）：common 模块 spring-boot-starter-web 非可选依赖与"纯接口模块"定位冲突

**位置**：§2.2 模块职责与依赖方向（common 模块依赖描述）

**现象**：`common` 模块将 `spring-boot-starter-web` 声明为 `compile` 且**未标注 `<optional>`**。根据 Maven 传递依赖机制，所有依赖 `common` 的模块都会传递性地获得 `spring-boot-starter-web` 及其全部传递依赖（内嵌 Tomcat、Spring MVC 全套、Jackson 序列化等）。

受影响模块：
- `ai-api` —— 自述 **"仅含接口 + DTO，不含任何业务实现依赖"**，但传递依赖中携带完整 Web 容器
- `common-module-api` —— 同样自述 **"仅含接口 + DTO，不含任何业务实现依赖"**，同样传递依赖中携带完整 Web 容器

**矛盾**："纯接口模块不应携带 Web 容器依赖" 与 "common 将 spring-boot-starter-web 作为非 optional 传递依赖" 之间存在概念矛盾。虽然不影响运行时正确性（Maven 可通过 `<exclusions>` 排除），但违背了 `ai-api` 和 `common-module-api` 作为轻量级契约模块的设计意图，且增加了不必要的依赖管理负担。

---

## 问题四（逻辑错误 · Medium）：依赖方向图中 ai/ai-api 与 common-module-api 的关系表达有误

**位置**：§2.2 依赖方向图（ASCII 图示部分）

**现象**：依赖方向图中，`common-module-api` 的四个向上的箭头（↑）分别指向 `patient`、`doctor`、`admin`、`ai/ai-api`，且图注中的文字说明将 `ai/ai-api` 列在 `common-module-api` 的依赖方中：

```
modules/common-module/common-module-api (依赖 common)
          ↑          ↑          ↑          ↑
    modules/  modules/  modules/  modules/
    patient   doctor    admin      ai/ai-api
```

但同节文字明确说明：
- `modules/ai/ai-api`：依赖 `common` —— 即 ai-api 只依赖 common，不依赖 common-module-api
- `modules/patient`、`modules/doctor`、`modules/admin`：依赖 `common`、`common-module-api` 和 `modules/ai/ai-api` —— 即业务模块依赖 common-module-api，ai-api 不在其列

**根因**：图中 `ai/ai-api` 所在的第四行应只包含 patient、doctor、admin 三个业务模块。ai-api 虽在图中以曲线连接表示"业务模块也依赖 ai-api"，但将其放置在 common-module-api 的箭头指向范围内造成了 ai-api 也依赖 common-module-api 的错误暗示。图表与文字说明不一致，属于表述性逻辑错误。

---

## 诊断结论

| 序号 | 类型 | 严重度 | 问题概要 | 修复提示 |
|------|------|--------|----------|----------|
| P1 | 偏离路线图 | High | Phase 0 包含认证基础设施（LoginUser、SecurityConfig、UserDetailsService 等），超出"数据与权限模型骨架"范围 | 将 LoginUser、SecurityConfig、UserDetailsService、AuthenticationEntryPoint、AccessDeniedHandler、PasswordEncoder、CORS 配置迁移至 Phase 1 OOD；Phase 0 仅保留 User/Role/Post/Function 四个实体定义 |
| P2 | 偏离路线图 | High | Phase 0 冻结跨模块门面接口 PermissionService，违反"不包含模块级接口契约冻结" | 将 PermissionService 接口定义从 Phase 0 OOD 中移除，归入 Phase 1 OOD |
| P3 | 定义矛盾 | Medium | common 模块 spring-boot-starter-web 非 optional 依赖导致 ai-api/common-module-api 传递性携带完整 Web 容器 | 将 spring-boot-starter-web 在 common 中标注为 optional，或另建 common-web 模块承载 @ControllerAdvice 等 Web 基础设施 |
| P4 | 逻辑错误 | Medium | 依赖方向图中 ai/ai-api 被错误置于 common-module-api 的依赖方列表中 | 修正 ASCII 图，将第四行从 patient/doctor/admin/ai-api 改为 patient/doctor/admin 三模块；保留曲线标注以表示业务模块依赖 ai-api 的关系 |
