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

**根因**：OOD 设计者在"骨架可运行"（§1.1 明确提及的设计目标）与"严格遵循阶段边界"之间做出了有意识的权衡选择，最终选择了前者：通过引入 SecurityConfigPhase0（permitAll）避免 Phase 0 启动时 Spring Security 自动配置 401 错误，并在此过程中将 PasswordEncoder、AuthenticationEntryPoint、AccessDeniedHandler、CORS 等 Phase 1 才需落地的真实安全配置一并纳入以"减少重构"。这种选择属于"有意识但越界的设计决策"（conscious but boundary-crossing design decision），而非"概念误扩展"（即并非 OOD 作者分不清"权限模型骨架"和"认证基础设施"）。但即使从设计决策角度理解其动机，结果上仍然偏离了路线图的阶段边界：

| OOD 设计动机 | 对应的路线图越界 |
|-------------|----------------|
| 骨架需可运行，需避免 401 | SecurityConfigPhase0 的 permitAll 本身已属安全框架配置 |
| 共享配置一次定义，后续复用 | PasswordEncoder、AuthEntryPoint、AccessDeniedHandler 是 Phase 1 "统一认证"的实现细节 |
| CORS 在前端开发时即需要 | CORS 配置虽在前端联调时确实需要，但其属于认证基础设施的一部分 |
| LoginUser 紧邻 User 实体，放在同一处自然 | LoginUser（UserDetails 适配器）是认证流程组件，不属于"数据权限模型骨架" |

**影响**：在 Phase 0 提前冻结认证基础设施的实现方案（如 UserDetailsService 的实现路径、SecurityConfig 的 profile 切换机制），限制了 Phase 1 的架构选择空间；同时造成 Phase 0 骨架中包含其职责范围外的基础设施代码。

**修复方案分析**：两种可行方案的权衡

| 方案 | 做法 | 优点 | 风险/代价 |
|------|------|------|-----------|
| **A（推荐）**：Phase 0 保留最小 SecurityConfigPhase0 占位 | 仅保留 `SecurityConfigPhase0` 的 `permitAll` 骨架，移除 AuthenticationEntryPoint、AccessDeniedHandler、PasswordEncoder、CORS 共享配置；这些归属 Phase 1 OOD。`spring-boot-starter-security` 保留在 application 模块依赖中 | ① Phase 0 骨架可启动无 401；② Phase 1 安全策略不受 Phase 0 冻结影响；③ 改动范围最小 | ① Phase 0 与 Phase 1 间 SecurityConfig 共用 `@Profile("!phase0")` 的反选机制需在 Phase 1 OOD 中重新约定；② 移除 CORS 配置后 Phase 0 三端前端联调需另配代理解决跨域（但 Vite 代理本身可处理前端调用场景）；③ 共享配置存在一定重复定义风险 |
| **B**：Phase 0 完全移除 spring-boot-starter-security | 从 application 模块 POM 中移除 `spring-boot-starter-security` 依赖，不保留任何安全相关 Bean 或配置 | ① 最彻底的阶段分离；② Phase 1 安全架构选择空间最大 | ① Phase 0 骨架启动时无任何安全机制（不含 permitAll，对于骨架阶段可接受）；② Phase 1 需从零添加 security 依赖及全部配置；③ Phase 1 切换时有更多"从无到有"的工作量 |

选择建议：如果团队判断 Phase 0→Phase 1 的切换间隔较短且 Phase 1 负责人已知，选 A 更为务实；如果 Phase 1 的架构选择仍存在不确定性，选 B 更安全。

---

## 问题二（偏离路线图 · High）：Phase 0 中冻结了跨模块门面接口 PermissionService

**位置**：§3.3（PermissionService 接口定义与注释代码）、§8.4（跨模块调用规范中的 PermissionService 示例）

**现象**：`common-module-api` 子模块中定义了 PermissionService 门面接口，包含 `getUserById(Long userId)` 和 `getUserPermissions(Long userId)` 两个完整方法签名，指定了返回类型 `UserDTO` 和 `Set<String>`、权限编码格式 `"{module}:{action}"`。

**路线图约束**：
- Phase 0 **"明确不包含"**：**"模块级接口契约冻结（在对应阶段启动前冻结）"**
- "模块级接口契约"指模块间跨模块调用的接口合约，PermissionService 作为 common-module-impl 对外暴露的门面接口，是典型的模块级接口契约

**根因**：OOD 试图通过"Phase 0 暂不实现任何跨模块调用"和"Phase 0 仅冻结接口形态"来避免触及"冻结"定义。但接口方法签名、参数类型、返回类型、权限编码格式在 Phase 0 中已确定，这本身就是契约冻结——实现是否就绪不影响契约已冻结的事实。PermissionService 应在 Phase 1（权限矩阵落地阶段）首次定义。

**修复方案分析**：

**操作**：将 PermissionService 接口定义从 Phase 0 OOD 中移除，归入 Phase 1 OOD。

**common-module-api 模块在 Phase 0 的保留策略**：

| 选项 | 做法 | 对模块结构的影响 | 对 CI 分阶段构建的影响 |
|------|------|-----------------|----------------------|
| **保留空壳模块** | common-module-api 在 Phase 0 保留 pom.xml + 占位类（如一个空 interface 或 README），不包含 PermissionService | 无影响，模块结构不变；common-module-impl 的 POM 中仍有 `<dependency>` 指向 common-module-api（编译期间可解析到空模块） | CI Stage 1 不变：仍 `mvn install -DskipTests -pl ... common-module-api`，空模块编译安装到本地仓库；Stage 2 不变 |
| **移除 common-module-api 模块** | 从父 POM 的 `<modules>` 和 `<dependencyManagement>` 中移除 common-module-api；common-module-impl 移除对 common-module-api 的依赖 | ① common-module-impl 不再依赖 common-module-api（其自身 Entity/Repository/Service 独立性需验证）；② Phase 0 父 POM 少一个 module，Phase 1 需重新加入；③ 若 common-module-impl 在 Phase 0 不需要 common-module-api 的类，此方案可行 | ① CI Stage 1 移除 common-module-api；② common-module-impl 移入 Stage 2 时，其 POM 依赖变化需验证编译链完整性 |

**推荐**：保留 common-module-api 空壳模块。理由是：① common-module-impl 在 Phase 0 已包含权限实体（User、Role、Post、Function），其 POM 明确声明依赖 common-module-api，若移除 common-module-api 则需重构 common-module-impl 的 POM 结构，Phase 1 再恢复；② 保留空壳对 CI 和模块结构影响最小，Phase 1 在空壳内加入 PermissionService 即可完成过渡。

---

## 问题三（定义矛盾 · Medium）：common 模块 spring-boot-starter-web 非可选依赖与"纯接口模块"定位冲突

**位置**：§2.2 模块职责与依赖方向（common 模块依赖描述）

**现象**：`common` 模块将 `spring-boot-starter-web` 声明为 `compile` 且**未标注 `<optional>`**。根据 Maven 传递依赖机制，所有依赖 `common` 的模块都会传递性地获得 `spring-boot-starter-web` 及其全部传递依赖（内嵌 Tomcat、Spring MVC 全套、Jackson 序列化等）。

受影响模块：
- `ai-api` —— 自述 **"仅含接口 + DTO，不含任何业务实现依赖"**，但传递依赖中携带完整 Web 容器
- `common-module-api` —— 同样自述 **"仅含接口 + DTO，不含任何业务实现依赖"**，同样传递依赖中携带完整 Web 容器

**矛盾**："纯接口模块不应携带 Web 容器依赖" 与 "common 将 spring-boot-starter-web 作为非 optional 传递依赖" 之间存在概念矛盾。虽然不影响运行时正确性（Maven 可通过 `<exclusions>` 排除），但违背了 `ai-api` 和 `common-module-api` 作为轻量级契约模块的设计意图，且增加了不必要的依赖管理负担。

**修复方案分析**：

| 方案 | 做法 | 优点 | 缺点 |
|------|------|------|------|
| **A（标记 optional）** | 在 common 的 `pom.xml` 中将 `spring-boot-starter-web` 标注 `<optional>true</optional>` | ① 改动最小，仅一行 POM 变更；② ai-api/common-module-api 立即恢复纯接口模块定位；③ 与 common 中 data-jpa 的 optional 策略一致（§2.2 已明确描述） | ① 业务模块（patient/doctor/admin）需在自己 POM 中显式声明 spring-boot-starter-web 依赖（版本由父 POM 的 dependencyManagement 统一管理）；② 现有业务模块代码无需修改，仅 POM 补声明 |
| **B（新建 common-web 模块）** | 从 common 中分离出 common-web 子模块，承载 @ControllerAdvice、Jackson 配置等 Web 基础设施；common 保持纯 POJO + util + JPA（不依赖 spring-boot-starter-web） | ① 架构分离更彻底，Web 与非 Web 关注点解耦；② ai-api/common-module-api 直接依赖 common（不含 web），pure-interface 定位更强 | ① 新增一个 Maven 子模块，增加模块复杂度；② 需调整所有业务模块的 POM 依赖（从 common 改为 common + common-web）；③ common 现有 GlobalExceptionHandler、Result 序列化等代码需拆分至 common-web |

**推荐**：Phase 0 优先选择方案 A（标记 optional）。理由是：① 与 data-jpa 的 optional 策略一致，不增加架构复杂度；② Phase 0 是骨架阶段，不引入过多模块拆分为宜；③ 若后续需要更彻底的分离，可在 Phase 1+ 调整。

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

**修复方式**：修正 ASCII 图，将第四行从 `patient/doctor/admin/ai-api` 改为 `patient/doctor/admin` 三模块；保留曲线标注（横向箭头）表示业务模块依赖 ai-api 的关系。

---

## 诊断结论

| 序号 | 类型 | 严重度 | 问题概要 | 修复提示 |
|------|------|--------|----------|----------|
| P1 | 偏离路线图 | **High** | Phase 0 包含认证基础设施（LoginUser、SecurityConfig、UserDetailsService 等），超出"数据与权限模型骨架"范围 | 将 LoginUser、SecurityConfig、UserDetailsService、AuthenticationEntryPoint、AccessDeniedHandler、PasswordEncoder 迁移至 Phase 1 OOD；Phase 0 仅保留 User/Role/Post/Function 四个实体定义。建议采用方案 A（保留最小 SecurityConfigPhase0 permitAll 占位）以保持骨架可运行，同时移除 Phase 1 专属的共享安全配置。参见上方修复方案分析。 |
| P2 | 偏离路线图 | **High** | Phase 0 冻结跨模块门面接口 PermissionService，违反"不包含模块级接口契约冻结" | 将 PermissionService 接口定义从 Phase 0 OOD 中移除，归入 Phase 1 OOD。common-module-api 模块在 Phase 0 保留为空壳模块，不影响 CI 分阶段构建结构。 |
| P3 | 定义矛盾 | **Medium+** | common 模块 spring-boot-starter-web 非 optional 依赖导致 ai-api/common-module-api 传递性携带完整 Web 容器 | 将 spring-boot-starter-web 在 common 中标注为 optional（方案 A，优先推荐），或另建 common-web 模块承载 @ControllerAdvice 等 Web 基础设施（方案 B）。标记 optional 后的影响：业务模块需显式声明 spring-boot-starter-web 依赖。 |
| P4 | 逻辑错误 | **Medium-** | 依赖方向图中 ai/ai-api 被错误置于 common-module-api 的依赖方列表中 | 修正 ASCII 图，将第四行从 patient/doctor/admin/ai-api 改为 patient/doctor/admin 三模块；保留曲线标注以表示业务模块依赖 ai-api 的关系。纯文档错误，不影响代码编译或运行时。 |

**严重度分层说明**：
- **High**（P1、P2）：偏离路线图阶段边界，可能导致后续阶段架构选择空间被压缩，需要跨阶段协调修正
- **Medium+**（P3）：定义矛盾，有实际的依赖传递污染后果，但可通过 POM 标注或模块拆分修复，不改变已有模块结构
- **Medium-**（P4）：表述性逻辑错误，仅影响文档可读性，不影响代码编译、运行时或后续阶段决策

---

## 修订说明（v2）

| 质询意见 | 回应 |
|---------|------|
| P1 修复建议缺少副作用分析，降低可操作性 | 已补充「修复方案分析」表格，给出方案 A（保留最小 SecurityConfigPhase0 占位）和方案 B（完全移除 spring-boot-starter-security）两种方案，并注明各自的权衡、优点与风险。 |
| P2 修复建议未评估对模块结构的影响 | 已补充「common-module-api 模块在 Phase 0 的保留策略」表格，分析"保留空壳模块"与"移除模块"两种选项对模块结构和 CI 分阶段构建的影响，并给出推荐（保留空壳）。 |
| P1 根因分析未充分回应该 OOD 的设计权衡 | 已修订根因分析，明确区分"概念误扩展"与"有意识但越界的设计决策"，分析 OOD 设计者选择引入安全基础设施的动机（骨架可运行、减少 Phase 1 重构），指出这是一次有意识但越界的决策。 |
| 未发现 P3 和 P4 之间的优先级层次差异 | 已将 P3 标注为 Medium+、P4 标注为 Medium-，在「诊断结论」末尾增加严重度分层说明。同时为 P3 补充了两种修复方案（标记 optional vs 新建 common-web 模块）各自的优缺点表格。 |
