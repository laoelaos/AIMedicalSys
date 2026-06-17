# 诊断报告：Docs/04_ood_phase0.md OOD 文档问题定位

## 检测范围

对照 Docs/03_roadmap.md Phase 0 的阶段边界与范围定义，检查 OOD 文档中的：
- 偏离路线图阶段边界的问题
- 定义矛盾 / 概念表述偏差
- 事实错误
- 逻辑错误

---

## 事实错误检查结果

经逐节核查 OOD 文档中的可验证技术事实（模块依赖关系、构建命令、类/接口命名、包路径、配置项等），仅发现一项事实错误：§2.2 依赖方向图中 ai/ai-api 被错误置于 common-module-api 的箭头指向范围内。该问题已在下文问题四 P4 中作为逻辑错误记录，P4 同时标注为文档表述性事实错误。其余已核验的技术事实（包括但不限于 §10 的 CI 分阶段构建命令——Stage 1 构建 common/common-module-api/ai-api，Stage 2 构建 common-module-impl/patient/doctor/admin/ai-impl，Stage 3 构建 application——模块列表完整、顺序与依赖关系一致；§2.2 的模块依赖规则与 §2.3 的包命名规范中模块归属关系一致；§4.5 的 PasswordEncoder 声明为 BCryptPasswordEncoder 与 Spring Security 标准实践一致；§8.2 的 13 项 AI 能力方法签名与附录 C 的落地阶段索引一致）均未发现事实性错误。

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
- Phase 0 范围：**"数据与权限模型骨架：数据实体基类与权限模型就位"** —— 仅包含实体（Entity）层面的骨架定义
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
| **A（推荐）**：Phase 0 保留最小 SecurityConfigPhase0 占位 | 仅保留 `SecurityConfigPhase0` 的 `permitAll` 骨架，移除 AuthenticationEntryPoint、AccessDeniedHandler、PasswordEncoder、CORS 共享配置；这些归属 Phase 1 OOD。application 模块 POM 中需显式声明 spring-boot-starter-security 依赖（版本由父 POM 统一管理） | ① Phase 0 骨架可启动无 401；② Phase 1 安全策略不受 Phase 0 冻结影响；③ 改动范围最小 | ① Phase 0 与 Phase 1 间 SecurityConfig 共用 `@Profile("!phase0")` 的反选机制需在 Phase 1 OOD 中重新约定；② 移除 CORS 配置后 Phase 0 三端前端联调需另配代理解决跨域（但 Vite 代理本身可处理前端调用场景）；③ 共享配置存在一定重复定义风险；**④ Spring Security 自动配置风险**：若 UserDetailsService 未按推荐同步移入 Phase 1 而保留在 Phase 0，则 `spring-boot-starter-security` 保留在 Phase 0 类路径上但 PasswordEncoder Bean 被移除时，Spring Security 自动配置可能因找不到 PasswordEncoder 报错。Phase 0 的 SecurityConfigPhase0 需确保 `permitAll` 方案下无任何认证流程间接触发 PasswordEncoder 调用（如 User 实体初始化时不会调用密码编码）。若 UserDetailsService 保留在 Phase 0，建议添加一个占位 PasswordEncoder（返回 null 或直接 throw UnsupportedOperationException），或通过 `@EnableWebSecurity` 抑制自动配置。若 UserDetailsService 按推荐移入 Phase 1，则此风险自动消除（Phase 0 不存在触发 PasswordEncoder 查找的认证流程）。移除 PasswordEncoder 前需验证 Phase 0 Spring 上下文在无 PasswordEncoder Bean 时可正常启动。 **⑤ SecurityConfigPhase0 与共享配置 Bean 的耦合风险**：方案 A 假设 SecurityConfigPhase0 在代码层面不通过 `@Autowired` 或构造器注入引用被移除的共享 Bean（AuthenticationEntryPoint、AccessDeniedHandler、PasswordEncoder、CORS 配置）。当前 OOD §4.5 中 SecurityConfigPhase0 仅定义了 `filterChain(HttpSecurity http)` 方法，未见 `@Autowired` 字段或构造器注入；`// 复用` 注释为设计意图标记，非代码级注入。但实际 Java 代码尚未实现，Phase 0 编码阶段需验证 SecurityConfigPhase0 不通过自动装配引用这些共享 Bean；若有引用，则需为 Phase 0 保留这些 Bean 的骨架占位或调整 SecurityConfigPhase0 的实现以消除耦合。 |
| **B**：Phase 0 完全移除 spring-boot-starter-security | 从 application 模块 POM 中移除 `spring-boot-starter-security` 依赖，不保留任何安全相关 Bean 或配置 | ① 最彻底的阶段分离；② Phase 1 安全架构选择空间最大 | ① Phase 0 骨架启动时无任何安全机制（不含 permitAll，对于骨架阶段可接受）；② Phase 1 需从零添加 security 依赖及全部配置；③ Phase 1 切换时有更多"从无到有"的工作量 |

选择建议：如果团队判断 Phase 0→Phase 1 的切换间隔较短且 Phase 1 负责人已知，选 A 更为务实；如果 Phase 1 的架构选择仍存在不确定性，选 B 更安全。

**修复提示**（精确限定版）：

将 LoginUser、UserDetailsService 迁移至 Phase 1 OOD；将 AuthenticationEntryPoint、AccessDeniedHandler、PasswordEncoder、CORS 配置等 Phase 1 专属的共享安全配置类从 Phase 0 移出，归入 Phase 1 OOD。Phase 0 仅保留 User/Role/Post/Function 四个实体定义及 SecurityConfigPhase0（permitAll 骨架占位，用于避免 Phase 0 启动时 Spring Security 自动配置 401 错误）。SecurityConfigPhase0 与共享安全配置类的分离依据：

| 组件 | Phase 0 处置 | 依据 |
|------|------------|------|
| `SecurityConfigPhase0`（permitAll FilterChain） | 保留 | 骨架可运行的必要占位，仅含 permitAll 逻辑，不携带真实安全策略 |
| `AuthenticationEntryPoint`、`AccessDeniedHandler`、`PasswordEncoder`、CORS 配置 | 迁移至 Phase 1 | 属于 Phase 1 "统一认证"的实现细节，在 Phase 0 冻结将限制 Phase 1 架构选择 |

推荐采用方案 A（保留最小 SecurityConfigPhase0 permitAll 占位）以保持骨架可运行，同时将 Phase 1 专属的共享安全配置移至 Phase 1 OOD。若选方案 A，需额外处理 PasswordEncoder 占位问题以避免 Spring Security 自动配置在缺少 PasswordEncoder Bean 时启动失败（见上方方案 A 风险栏）。

---

## 问题二（偏离路线图 · High）：Phase 0 中冻结了跨模块门面接口 PermissionService

**位置**：§3.3（PermissionService 接口定义与注释代码）、§8.4（跨模块调用规范中的 PermissionService 示例）

**现象**：`common-module-api` 子模块中定义了 PermissionService 门面接口，包含 `getUserById(Long userId)` 和 `getUserPermissions(Long userId)` 两个完整方法签名，指定了返回类型 `UserDTO` 和 `Set<String>`、权限编码格式 `"{module}:{action}"`。

**路线图约束**：
- Phase 0 **"明确不包含"**：**"模块级接口契约冻结（在对应阶段启动前冻结）"**
- "模块级接口契约"指模块间跨模块调用的接口合约，PermissionService 作为 common-module-impl 对外暴露的门面接口，是典型的模块级接口契约

**根因**：OOD 试图通过"Phase 0 暂不实现任何跨模块调用"（§8.4：**"Phase 0 暂不实现任何跨模块调用（各业务模块仅有占位 Controller），上述规范为 Phase 1+ 预留"**）和"Phase 0 仅冻结接口形态，不在业务模块占位 Service 中实际注入这些跨模块门面"（§8.4：**"Phase 0 仅冻结接口形态，不在业务模块占位 Service 中实际注入这些跨模块门面"**）来避免触及"冻结"定义。OOD 作者在 §8.4 中已意识到跨模块调用不应在 Phase 0 实现并添加了自约束声明。但**接口方法签名、参数类型、返回类型、权限编码格式在 Phase 0 中已确定，这本身就是契约冻结**——实现是否就绪不影响契约已冻结的事实。PermissionService 应在 Phase 1（权限矩阵落地阶段）首次定义。OOD 的自约束虽反映了作者的合理意图，但契约冻结的实质后果不受自约束影响。

**AiService 与 PermissionService 的差异依据**：

Phase 0 OOD 同时包含了 AiService（13 个方法签名，§8.2）和 PermissionService（2 个方法签名）两份接口契约，但路线图仅禁止前者。这一差异的依据来源于路线图本身的显式约定：

| 对比维度 | AiService（允许） | PermissionService（禁止） |
|---------|-----------------|------------------------|
| 路线图定位 | §0.2「推荐补齐」中明确列出：**"AI 能力模块 Mock 占位：AI 能力模块接口契约与 Mock 数据占位，支持前端独立开发"** | §0.4「明确不包含」中列出：**"模块级接口契约冻结（在对应阶段启动前冻结）"** |
| 契约性质 | 抽象能力层接口，定义"系统能调用什么 AI 能力"，不涉及业务模块间的数据查询约定 | 跨模块业务门面接口，定义"业务模块如何获取用户/权限数据"，属于模块间数据访问契约 |
| 冻结影响面 | 后续阶段 AI 接入方案可从 Mock 切换为真实实现，接口方法签名作为类型化调用入口不变 | 在 Phase 0 冻结后，common-module 的数据访问路径和返回值格式被固定，限制 Phase 1 的模块内部分设计 |
| 前端依赖 | 前端需要 AiService 的类型定义以完成占位 UI 联调 | 前端在 Phase 0 不需要 PermissionService 类型（无认证/权限页面） |

结论：AiService 的接口契约属于路线图 §0.2 明确许可的"推荐补齐"例外，不在"模块级接口契约冻结"的禁止范围内。PermissionService 属于后者，应在 Phase 1 首次定义。

**修复方案分析**：

**操作**：将 PermissionService 接口定义从 Phase 0 OOD 中移除，归入 Phase 1 OOD。

**UserDTO 与 UserType 的处置策略**：

移除 PermissionService 后，其依赖的两个类型需要分别处置：

| 类型 | 归属模块 | 处置 | 依据 |
|------|---------|------|------|
| `UserDTO` | common-module-api.dto | 随 PermissionService 迁至 Phase 1 | UserDTO 是 PermissionService 方法 `getUserById()` 的返回类型，在 Phase 0 无独立引用方；common-module-api 保留空壳模块期间不包含 UserDTO 类 |
| `UserType` | common-module-api（共享枚举） | **留在 Phase 0 common-module-api 中** | §3.3 明确 UserType 被 User 实体（common-module-impl）引用；User 实体在 Phase 0 已定义，UserType 作为其 `userType` 字段的枚举类型必须在 Phase 0 就位。UserType 不属于"模块级接口契约"，而是 Phase 0 实体的共享类型依赖 |

UserType 的"留守"逻辑：common-module-api 作为空壳模块保留时，仅包含 UserType 枚举（及可能的其他 Phase 0 实体依赖的共享枚举），不包含 PermissionService 接口和 UserDTO。Phase 1 在 common-module-api 中恢复 UserDTO 和 PermissionService 接口定义。

**对前端 TypeScript 类型同步的影响**：Phase 0 由前端团队人工维护 `packages/shared/types/` 中的 TypeScript 类型定义（§8.3）。PermissionService 和 UserDTO 从 Phase 0 移除后，前端 `packages/shared/types/` 中需同步删除对应的 TypeScript 类型定义（PermissionService 接口类型、UserDTO 接口定义），避免人工维护的类型与后端代码不一致。Phase 1 引入 PermissionService 和 UserDTO 时，将通过 openapi-generator 自动生成 TypeScript 类型（§8.3），消除人工同步问题。

**common-module-api 模块在 Phase 0 的保留策略**：

| 选项 | 做法 | 对模块结构的影响 | 对 CI 分阶段构建的影响 |
|------|------|-----------------|----------------------|
| **保留空壳模块** | common-module-api 在 Phase 0 保留 pom.xml + 占位类（如一个空 interface 或 README），不包含 PermissionService | 无影响，模块结构不变；common-module-impl 的 POM 中仍有 `<dependency>` 指向 common-module-api（编译期间可解析到空模块） | CI Stage 1 不变：仍 `mvn install -DskipTests -pl ... common-module-api`，空模块编译安装到本地仓库；Stage 2 不变 |
| **移除 common-module-api 模块** | 从父 POM 的 `<modules>` 和 `<dependencyManagement>` 中移除 common-module-api；common-module-impl 移除对 common-module-api 的依赖 | ① common-module-impl 不再依赖 common-module-api（其自身 Entity/Repository/Service 独立性需验证）；② Phase 0 父 POM 少一个 module，Phase 1 需重新加入；③ 若 common-module-impl 在 Phase 0 不需要 common-module-api 的类，此方案可行 | ① CI Stage 1 移除 common-module-api；② common-module-impl 移入 Stage 2 时，其 POM 依赖变化需验证编译链完整性 |

**推荐**：保留 common-module-api 空壳模块。理由是：① common-module-impl 在 Phase 0 已包含权限实体（User、Role、Post、Function），其 POM 明确声明依赖 common-module-api，若移除 common-module-api 则需重构 common-module-impl 的 POM 结构，Phase 1 再恢复；② 保留空壳对 CI 和模块结构影响最小；③ UserType 枚举需在 common-module-api 中为 Phase 0 User 实体提供共享类型，空壳方案可容纳 UserType 留守。

**Phase 1 恢复 PermissionService 的具体操作**：

| 步骤 | 模块 | 操作 |
|------|------|------|
| 1 | common-module-api | 在 `com.aimedical.modules.commonmodule.api` 包中新建 `PermissionService` 接口，恢复 `getUserById(Long userId)` 和 `getUserPermissions(Long userId)` 方法签名 |
| 2 | common-module-api | 在 `com.aimedical.modules.commonmodule.api.dto` 包中新建 `UserDTO` 类，恢复 userId、userName、userType 等字段 |
| 3 | common-module-impl | 在 `com.aimedical.modules.commonmodule.permission` 包中新建 `PermissionServiceImpl` 类，实现 `PermissionService` 接口，注入 `UserRepository` 等依赖 |
| 4 | common-module-impl | 检查 `PermissionServiceImpl` 是否有新外部依赖（如缓存、配置等），如有则在 `common-module-impl/pom.xml` 中补充声明；`common-module-api` 依赖不变（仅依赖 common） |
| 5 | application 模块 | 确保 application 模块 POM 中 `spring-boot-starter-security` 显式声明（原方案 A 前提），以支持 PermissionService 的认证流程 |
| 6 | 前端 | 删除 Phase 0 人工维护的 PermissionService/UserDTO TypeScript 类型（Phase 0 人工同步），启用 openapi-generator 自动生成（§8.3） |

**"先拆后装"对团队认知成本的影响评估**：
采用"先冻结为空壳 → Phase 1 恢复"的策略对开发团队的影响如下：
- **正向**：① Phase 0 骨架阶段职责清晰，不持有 Phase 1 才使用的接口类型和 DTO，减少团队在骨架阶段处理"不属于本阶段内容"的认知负担；② Phase 1 恢复时接口签名不变，无需重新设计，仅需补齐实现类和 POM 依赖，操作路径明确
- **摩擦**：① Phase 1 开发者需留意 common-module-api 空壳中的 UserType 留守——不要误删 UserType（该枚举同时被 User 实体引用，归属骨架而非 Phase 1 契约）；② 如果 Phase 1 开发者不熟悉 Phase 0 OOD 的"空壳待恢复"设计意图，可能误以为 common-module-api 在 Phase 0 被"废弃"而非"暂留空壳"，导致追加模块级结构调整
- **缓解措施**：在 Phase 0 OOD 中明确记录 common-module-api 的"空壳待恢复"定位，并在 Phase 1 OOD 中引用本诊断报告的恢复操作表；Phase 1 kick-off 时由架构师口头同步此遗留事项

---

## 问题三（概念表述偏差 · Medium）：common 模块 spring-boot-starter-web 非可选依赖与"纯接口模块"定位存在概念表述偏差

**位置**：§2.2 模块职责与依赖方向（common 模块依赖描述及依赖传播决策）

**现象**：`common` 模块将 `spring-boot-starter-web` 声明为 `compile` 且**未标注 `<optional>`**。根据 Maven 传递依赖机制，所有依赖 `common` 的模块都会传递性地获得 `spring-boot-starter-web` 及其全部传递依赖（内嵌 Tomcat、Spring MVC 全套、Jackson 序列化等）。

受影响模块：
- `ai-api` —— 自述 **"仅含接口 + DTO，不含任何业务实现依赖"**，但传递依赖中携带完整 Web 容器
- `common-module-api` —— 同样自述 **"仅含接口 + DTO，不含任何业务实现依赖"**，同样传递依赖中携带完整 Web 容器

**根因诊断**：OOD 文档 §2.2 的"Common 模块依赖传播决策"中已**明确说明**该选择是有意识的设计决策——`@ControllerAdvice`、`Result<T>` JSON 序列化及 Spring MVC 基础设施需要该依赖，且明确区分了 `spring-boot-starter-web`（无 optional）和 `spring-boot-starter-data-jpa`（标注 optional）两种策略。因此**OOD 内部的依赖传播策略定义是一致的，不存在自相矛盾的定义**。

问题的实质是**概念表述的偏差**："纯接口模块不应携带 Web 容器依赖"是模块定位的理想描述，而"common 将 web 作为非 optional 传递"是 OOD 作者明确定义的依赖策略——两者之间存在认知张力。`ai-api` 和 `common-module-api` 的"仅含接口 + DTO，不含任何业务实现依赖"表述在 Maven 传递依赖层面不完全准确（因通过 common 间接获得了 Web 容器依赖），但 OOD 已通过依赖传播决策章节充分说明了这一选择的理由。不属于"OOD 内部自相矛盾"，而属于"理想定位陈述与传递依赖现实之间的表述偏差"。

**修复方案分析**：

| 方案 | 做法 | 优点 | 缺点 |
|------|------|------|------|
| **A（标记 optional）** | 在 common 的 `pom.xml` 中将 `spring-boot-starter-web` 标注 `<optional>true</optional>` | ① 改动最小，仅一行 POM 变更；② ai-api/common-module-api 立即恢复纯接口模块定位；③ 与 common 中 data-jpa 的 optional 策略一致（§2.2 已明确描述） | ① 业务模块（patient/doctor/admin）及 application 模块需在自己 POM 中显式声明 spring-boot-starter-web 依赖（版本由父 POM 的 dependencyManagement 统一管理），因为 application 模块启动时同样需要 `@ControllerAdvice`、内嵌 Tomcat 等 Web 容器能力；② 现有业务模块代码无需修改，仅 POM 补声明 |
| **B（新建 common-web 模块）** | 从 common 中分离出 common-web 子模块，承载 @ControllerAdvice、Jackson 配置等 Web 基础设施；common 保持纯 POJO + util + JPA（不依赖 spring-boot-starter-web） | ① 架构分离更彻底，Web 与非 Web 关注点解耦；② ai-api/common-module-api 直接依赖 common（不含 web），pure-interface 定位更强 | ① 新增一个 Maven 子模块，增加模块复杂度；② 需调整所有业务模块的 POM 依赖（从 common 改为 common + common-web）；③ common 现有 GlobalExceptionHandler、Result 序列化等代码需拆分至 common-web |

**推荐**：Phase 0 优先选择方案 A（标记 optional）。理由是：① 与 data-jpa 的 optional 策略一致，不增加架构复杂度；② Phase 0 是骨架阶段，不引入过多模块拆分为宜；③ 若后续需要更彻底的分离，可在 Phase 1+ 调整。

---

## 问题四（逻辑错误 · Medium-，同时属于文档表述性事实错误）：依赖方向图中 ai/ai-api 与 common-module-api 的关系表达有误

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

**根因**：图中 `ai/ai-api` 所在的第四行应只包含 patient、doctor、admin 三个业务模块。ai-api 虽在图中以曲线连接表示"业务模块也依赖 ai-api"，但将其放置在 common-module-api 的箭头指向范围内造成了 ai-api 也依赖 common-module-api 的错误暗示。图表与文字说明不一致，属于表述性逻辑错误，同时属于文档表述性事实错误。

**修复方式**：修正 ASCII 依赖方向图，将第四行从 `patient/doctor/admin/ai-api` 改为 `patient/doctor/admin` 三模块。原有曲线标注（从第一行 ai-api 指向第四行 ai/ai-api 的水平连线）因终点消失需重新设计布局。替代方案包括：将曲线标注改为从第一行 ai-api 直接指向第三行业务模块区域的标注；或重新组织整体图结构使其清晰表达"业务模块依赖 ai-api"的关系。核心原则是图与文字说明保持一致。若图结构调整较大，执笔者应重新绘制完整的 ASCII 依赖方向图，确保图与 §2.2 正文的依赖规则说明完全一致。

---

## 诊断结论

| 序号 | 类型 | 严重度 | 问题概要 | 修复提示 |
|------|------|--------|----------|----------|
| P1 | 偏离路线图 | **High** | Phase 0 包含认证基础设施（LoginUser、SecurityConfig 共享配置、UserDetailsService 等），超出"数据与权限模型骨架"范围 | 将 LoginUser、UserDetailsService 迁移至 Phase 1 OOD；将 AuthenticationEntryPoint、AccessDeniedHandler、PasswordEncoder、CORS 配置等 Phase 1 专属的共享安全配置类从 Phase 0 移出，归入 Phase 1 OOD。Phase 0 仅保留 User/Role/Post/Function 四个实体定义及 SecurityConfigPhase0（permitAll 骨架占位）以保持骨架可运行。建议采用方案 A：application 模块 POM 中需显式声明 spring-boot-starter-security 依赖（版本由父 POM 统一管理），Phase 0 保留最小 SecurityConfigPhase0。需额外处理 PasswordEncoder 占位问题以避免 Spring Security 自动配置缺少 PasswordEncoder Bean 时启动失败，同时需验证 SecurityConfigPhase0 不通过 @Autowired/构造器注入引用被移除的共享 Bean（见上方方案 A 风险栏⑤）。 |
| P2 | 偏离路线图 | **High** | Phase 0 冻结跨模块门面接口 PermissionService，违反"不包含模块级接口契约冻结"。OOD 作者在 §8.4 中已意识到此约束并添加了自约束声明（"Phase 0 暂不实现跨模块调用"），但接口方法签名、参数类型、返回类型已在 Phase 0 确定，实质构成契约冻结。 | 将 PermissionService 接口定义及 UserDTO 从 Phase 0 OOD 中移除，归入 Phase 1 OOD。UserType 因被 Phase 0 User 实体引用而留在 common-module-api 中。common-module-api 模块在 Phase 0 保留为空壳模块（仅含 UserType 等 Phase 0 必需的共享类型），不影响 CI 分阶段构建结构。前端 `packages/shared/types/` 中的 TypeScript 类型定义需同步删除 PermissionService 和 UserDTO 相关类型。AiService 接口契约属于路线图 §0.2 明确许可的"推荐补齐"例外，与 PermissionService 性质不同。 |
| P3 | 概念表述偏差 | **Medium** | common 模块 spring-boot-starter-web 非 optional 依赖导致 ai-api/common-module-api 传递性携带完整 Web 容器。"纯接口模块不应携带 Web 容器依赖"的理想定位与该依赖策略之间存在表述偏差，但 OOD §2.2 已明确文档化了该设计决策，不属于 OOD 内部定义矛盾。 | 将 spring-boot-starter-web 在 common 中标注为 optional（方案 A，优先推荐），或另建 common-web 模块承载 @ControllerAdvice 等 Web 基础设施（方案 B）。标记 optional 后的影响：业务模块（patient/doctor/admin）及 application 模块需在自己 POM 中显式声明 spring-boot-starter-web 依赖（版本由父 POM 统一管理），因为 application 模块也需要 spring-boot-starter-web 以启动内嵌 Tomcat 和生效 @ControllerAdvice。 |
| P4 | 逻辑错误 / 事实错误 | **Medium-** | 依赖方向图中 ai/ai-api 被错误置于 common-module-api 的依赖方列表中，同时属于文档表述性事实错误。 | 修正 ASCII 依赖方向图，将第四行从 patient/doctor/admin/ai-api 改为 patient/doctor/admin 三模块。原曲线标注因终点消失需重新设计：改为从 ai-api 指向业务模块区域的标注，或重新绘制完整依赖方向图。纯文档错误，不影响代码编译或运行时。 |

**严重度分层说明**：
- **High**（P1、P2）：偏离路线图阶段边界，可能导致后续阶段架构选择空间被压缩，需要跨阶段协调修正
- **Medium**（P3）：概念表述偏差，有实际的依赖传递污染后果，但 OOD 内部定义一致，可通过 POM 标注修复
- **Medium-**（P4）：表述性逻辑/事实错误，仅影响文档可读性，不影响代码编译、运行时或后续阶段决策

---

## 修订说明（v2）

| 质询意见 | 回应 |
|---------|------|
| P1 修复建议缺少副作用分析，降低可操作性 | 已补充「修复方案分析」表格，给出方案 A（保留最小 SecurityConfigPhase0 占位）和方案 B（完全移除 spring-boot-starter-security）两种方案，并注明各自的权衡、优点与风险。 |
| P2 修复建议未评估对模块结构的影响 | 已补充「common-module-api 模块在 Phase 0 的保留策略」表格，分析"保留空壳模块"与"移除模块"两种选项对模块结构和 CI 分阶段构建的影响，并给出推荐（保留空壳）。 |
| P1 根因分析未充分回应该 OOD 的设计权衡 | 已修订根因分析，明确区分"概念误扩展"与"有意识但越界的设计决策"，分析 OOD 设计者选择引入安全基础设施的动机（骨架可运行、减少 Phase 1 重构），指出这是一次有意识但越界的决策。 |
| 未发现 P3 和 P4 之间的优先级层次差异 | 已将 P3 标注为 Medium+、P4 标注为 Medium-，在「诊断结论」末尾增加严重度分层说明。同时为 P3 补充了两种修复方案（标记 optional vs 新建 common-web 模块）各自的优缺点表格。 |

## 修订说明（v3）

| 质询意见 | 回应 |
|---------|------|
| P1 修复提示与方案 A 在 SecurityConfig 处置上相互矛盾：修复提示要求将 SecurityConfig 迁移至 Phase 1，但方案 A 要求 Phase 0 保留 SecurityConfigPhase0 占位 | 已修订修复提示，将笼统的"SecurityConfig"拆分为精确限定表述：SecurityConfigPhase0（permitAll 骨架）保留在 Phase 0，AuthenticationEntryPoint、AccessDeniedHandler、PasswordEncoder、CORS 配置等 Phase 1 专属的共享安全配置类迁移至 Phase 1 OOD。在问题一末尾新增「修复提示（精确限定版）」表格，逐一说明各组件的 Phase 0 处置与依据。同时在诊断结论表的 P1 行中同步修复措辞。 |
| 未解释 AiService 接口契约（13 个方法签名，§8.2）被允许而 PermissionService 接口契约被禁止的差异依据 | 已补充「AiService 与 PermissionService 的差异依据」表格，从路线图定位、契约性质、冻结影响面、前端依赖四个维度进行对比分析。核心依据：路线图 §0.2「推荐补齐」显式将"AI 能力模块接口契约与 Mock 数据占位"列为 Phase 0 许可项，AiService 属于该例外；PermissionService 属于 §0.4「明确不包含」中的"模块级接口契约冻结"，两者性质不同。 |
| P2 修复方案未讨论 UserDTO 和 UserType 的处置策略 | 已补充「UserDTO 与 UserType 的处置策略」表格：UserDTO 随 PermissionService 迁至 Phase 1（作为 PermissionService 的返回类型，Phase 0 无独立引用方）；UserType 因被 Phase 0 User 实体引用而留在 common-module-api 中（UserType 不属于"模块级接口契约"，而是 Phase 0 实体的共享类型依赖）。同时更新诊断结论表的 P2 行以反映这两类处置。 |

## 修订说明（v4）

| 质询意见 | 回应 |
|---------|------|
| 未覆盖"事实错误"审查维度（高）—— 用户需求明确要求覆盖该维度 | 已新增「事实错误检查结果」独立小节（位于"检测范围"之后、问题列表之前）。经逐节核查，OOD 文档中仅发现一项事实错误：§2.2 依赖方向图中 ai-api 被错误置于 common-module-api 的箭头指向范围内（已作为 P4 记录）。CI 分阶段构建命令（§10）模块列表完整、顺序与依赖关系一致，未发现遗漏或错误。其余可验证技术事实均准确无误。 |
| P3"定义矛盾"分类不精确，OOD 中该行为为有意识设计决策（中） | 已修订：将 P3 标题从"定义矛盾"改为"概念表述偏差（定义与实践的不一致）"；重写根因分析，明确肯定 OOD §2.2 的依赖传播策略文档是清晰且一致的，问题在于"纯接口模块"的理想定位与该依赖策略之间存在认知张力，而非 OOD 内部定义矛盾。同步更新诊断结论表中 P3 的类型和概述措辞。 |
| P1 修复方案 A 未评估 PasswordEncoder 移除后 Spring Security 自动配置的潜在断裂风险（中） | 已在「修复方案分析」表格的方案 A 风险栏中补充风险项④：`spring-boot-starter-security` 保留在 Phase 0 类路径上但 PasswordEncoder Bean 被移除时，Spring Security 自动配置可能因缺少 PasswordEncoder 报错。建议为 Phase 0 添加占位 PasswordEncoder，或通过 `@EnableWebSecurity` 抑制自动配置；移除前需验证无 PasswordEncoder 时上下文可正常启动。 |
| P2 分析未提及 OOD 已自约束"Phase 0 不实现跨模块调用"（低） | 已在 P2 根因分析中补充 OOD §8.4 的两项自约束声明（"Phase 0 暂不实现任何跨模块调用"和"Phase 0 仅冻结接口形态"），肯定 OOD 作者的合理意图，同时说明接口方法签名、参数类型、返回类型在 Phase 0 已确定，实质构成契约冻结，实现是否就绪不影响契约已冻结的事实。同步更新诊断结论表的 P2 行以提及自约束。 |
| P2 修复建议未评估对 Phase 0 前端类型维护的影响（低） | 已在 P2「修复方案分析」中新增"对前端 TypeScript 类型同步的影响"段落：Phase 0 由前端人工维护 `packages/shared/types/` 中的 TypeScript 类型（§8.3），PermissionService 和 UserDTO 移除后需同步删除对应的 TypeScript 类型定义。Phase 1 引入时将通过 openapi-generator 自动生成，消除人工同步问题。 |

## 修订说明（v5）

| 质询意见 | 回应 |
|---------|------|
| F1 与 P4 指向同一问题导致分类混乱（中）——第4轮已报告未解决 | 采用方案（a）：删除 F1 独立条目（将事实错误检查结果起始段改为"仅发现一项事实错误……该问题已在下文问题四 P4 中作为逻辑错误记录，P4 同时标注为文档表述性事实错误"）；P4 标题和根因分析中均标注"同时属于文档表述性事实错误"；诊断结论表 P4 行类型改为"逻辑错误 / 事实错误" |
| F1 对 §2.3 的引用不准确（低）——第4轮已报告未解决 | 因 F1 条目已删除，原 §2.3 引用问题自然消除，不需单独处理 |
| P4 修复建议不完整，曲线标注终点消失后未说明替代方案（低）——第4轮已报告未解决 | 已重写 P4「修复方式」，给出明确替代方案：将曲线标注改为从 ai-api 直接指向业务模块区域的标注，或重新绘制完整 ASCII 依赖方向图；补充核心原则"图与文字说明保持一致" |
| P1 风险项④未说明与 UserDetailsService 移出方案的条件关系（低）——第4轮已报告未解决 | 已修订风险项④：前置条件限定为"若 UserDetailsService 未按推荐同步移入 Phase 1 而保留在 Phase 0"；注明"若 UserDetailsService 按推荐移入 Phase 1，则此风险自动消除" |

## 修订说明（v6）

| 质询意见 | 回应 |
|---------|------|
| P4 严重等级标题与结论表不一致——第145行标题标注"Medium"，而第175行结论表 P4 严重度为 Medium-（低） | 已统一：将 P4 问题标题中的"Medium"改为"Medium-"（`## 问题四（逻辑错误 · Medium-，同时属于文档表述性事实错误）`）。严重度分层说明不变。 |
| P3 方案 A 遗漏 application 模块声明 spring-boot-starter-web 的影响分析（低） | 已修订方案 A 缺点栏：在"业务模块（patient/doctor/admin）"之后补充"及 application 模块"，并注明原因——application 模块自身也需要 spring-boot-starter-web 以启动内嵌 Tomcat 和生效 @ControllerAdvice/@SpringBootApplication。同步更新诊断结论表的 P3 行。 |
| P1 方案 A「保留 spring-boot-starter-security 在 application 模块依赖中」表述不精确（低） | 已修订为"application 模块 POM 中需显式声明 spring-boot-starter-security 依赖（版本由父 POM 统一管理）"，以对应 OOD §2.1 父 POM 中该依赖仅在 `<dependencyManagement>` 层声明、非直接属于 application 模块依赖的实际情况。同步更新诊断结论表的 P1 行。 |
| P1 修复方案对 SecurityConfigPhase0 与共享配置 Bean 间的耦合未被验证（一般） | 已补充风险项⑤至方案 A 风险栏：要求 Phase 0 编码阶段验证 SecurityConfigPhase0 不通过 `@Autowired`/构造器注入引用被移除的共享 Bean。经查阅 OOD §4.5，SecurityConfigPhase0 仅定义了 `filterChain(HttpSecurity http)` 方法，未见 `@Autowired` 字段或构造器注入；`// 复用` 注释为设计意图标记，非代码级注入。但因实际 Java 代码尚未实现，编码阶段需确认。若有引用，则需为 Phase 0 保留骨架占位或调整实现。 |
| P2 修复方案未评估 UserType 留守对 common-module-api 模块 Phase 1 恢复策略的影响（低） | 已补充「Phase 1 恢复 PermissionService 的具体操作」表格（6 步：common-module-api 新建接口和 UserDTO → common-module-impl 新建实现 → POM 依赖检查 → application 模块 security 依赖确认 → 前端类型同步），以及「"先拆后装"对团队认知成本的影响评估」（正向收益 + 两处摩擦点 + 缓解措施）。 |
