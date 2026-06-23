# Phase 0 OOD 深度诊断报告

## 诊断范围

- **文档**: `Docs/04_ood_phase0.md`（Phase 0 最小化骨架 — 架构级 OOD 设计方案）
- **参考**: `Docs/diagnose/01_ood_phase_diagnosis.md`（此前初步诊断记录）
- **方法**: 独立深度审查，逐节验证，追溯因果链

## 诊断结论

该文档整体骨架思路清晰，契约先行、模块隔离的核心方向正确。但经逐节推演，当前存在 **4 个阻断性问题（P1）**、**8 个主要问题（P2）** 和 **3 个次要问题（P3）**，不建议在未修订时直接进入编码阶段。

---

## 一、阻断性问题（P1 — 直接影响编码正确性）

### P1-1. AiService 同步返回契约与 Phase 2+ 异步演进直接冲突

| 属性 | 内容 |
|------|------|
| **位置** | §3.4（AiService interface 定义）、§6（并发设计）、§8.2（AI 能力方法清单） |
| **现象** | §8.2 全线定义为同步返回 `AiResult<T>`（如 `AiResult<TriageResponse> triage(...)`）；§6 第 812 行明确写 "Phase 2+ 引入 Spring Async + CompletableFuture 实现异步非阻塞调用，AiService 实现类返回 `CompletableFuture<AiResult<T>>`"。§7 第 831 行的设计决策也完全未提及此冲突。 |
| **根因** | 同步 `AiResult<T>` 与 `CompletableFuture<AiResult<T>>` 在 Java 类型系统中不可互赋，变更返回类型即破坏接口契约。所有业务模块在 Phase 0 按 `AiResult<T>` 编写的调用代码，在 Phase 2+ 将全面编译失败。 |
| **因果链** | 文档宣称"契约先行"（§1.1）→ AiService 是最核心的跨阶段契约 → 但文档自己规划了破坏性接口变更 → 所有依赖 AiService 的模块在 Phase 2+ 需全部重写调用方代码 → "契约先行"前提无法成立 |
| **影响范围** | 所有业务模块（patient/doctor/admin）中涉及 AI 调用的 Service 代码、前端 DTO 类型定义。估计 Phase 2+ 需重写 13 个方法的全部调用方。 |
| **修复方向** | 在 Phase 0 即将 AiService 接口返回类型固定为 `CompletableFuture<AiResult<T>>`，MockAiService 返回 `CompletableFuture.completedFuture(result)`，同步调用方在 Phase 0 直接 `.get()`/`.join()`，Phase 2+ 自然支持异步编排。 |

---

### P1-2. BaseEntity 软删除 `@SQLDelete` 模板不可直接复用

| 属性 | 内容 |
|------|------|
| **位置** | §3.2（BaseEntity 字段定义） |
| **现象** | §3.2 第 494 行定义 `@SQLDelete("UPDATE entity SET deleted = true WHERE id = ?")` 在 BaseEntity 上。`entity` 为字面量字符串，Hibernate 不会自动替换为子类对应的实际表名。 |
| **根因** | Java 注解在编译期即为常量字符串，`@SQLDelete` 不支持在抽象基类级别使用通用占位符自动解析为子类表名。Hibernate 6 的 `@SQLDelete` 注解本身不提供表名占位符替换机制。 |
| **因果链** | 文档期望"所有实体继承 BaseEntity 即获得软删除能力"→ `@SQLDelete` 中 `entity` 不是真实表名 → 运行时生成的 SQL 为 `UPDATE entity SET ...` 而非 `UPDATE user SET ...` → SQL 语法错误或更新错表 → 软删除完全不可用 |
| **影响范围** | 所有继承 BaseEntity 的实体（User、Role、Post、Function 及未来业务实体）的删除操作。 |
| **修复方向** | 选项 A：将 `@SQLDelete` + `@SQLRestriction` 从 BaseEntity 移除，改为各实体自行配置（加注释模板）。选项 B：Phase 0 暂不落地软删除，`deleted` 字段仅作为标记存在，删除操作由 Service 层手动 `setDeleted(true)` + `save()`，Phase 1+ 再通过 Hibernate 拦截器统一处理。 |

---

### P1-3. POM 中遗漏关键依赖声明，直接导致构建失败

| 属性 | 内容 |
|------|------|
| **位置** | §2.1（父 POM dependencyManagement）、§8.3（springdoc-openapi）、§9.1（H2 数据库） |
| **现象** | §8.3 第 1023 行声明 `springdoc-openapi-starter-webmvc-ui` 在父 POM 的 `<dependencyManagement>` 中统一管理，但 §2.1 第 156-207 行给出的父 POM XML 片段中未包含该依赖。§9.1 第 1133 行说 "POM 中 h2 数据库驱动以 runtime scope 声明，仅 application 模块引入"，但父 POM 和 application 模块的 POM XML 片段中均未出现 H2 依赖。 |
| **根因** | 文档表述与附带的 XML 代码不一致。文档正文承诺了依赖的存在，但 XML 代码片段中遗漏。 |
| **因果链** | 实现者按文档编码 → 在 Controller 中引入 `@Tag`/`@Operation` 注解 → 编译错误（缺少 springdoc-openapi 依赖）→ 或运行时后端无法启动（缺少 H2 驱动） |
| **影响范围** | springdoc-openapi：所有含 Controller 的模块（patient/doctor/admin）。H2：application 模块的启动阶段。 |
| **修复方向** | 在父 POM 的 `<dependencyManagement>` 中补全 `springdoc-openapi-starter-webmvc-ui`（及版本号）和 `com.h2database:h2` 依赖声明。在 `application/pom.xml` 中补全 H2 的 `runtime` scope 依赖。 |

---

### P1-4. 业务模块（patient/doctor/admin）缺少 API/impl 分层，与"模块边界即微服务边界"矛盾

| 属性 | 内容 |
|------|------|
| **位置** | §2.1（目录布局）、§2.2（模块依赖）、§7（设计决策表第 820 行） |
| **现象** | common-module 和 ai 模块拆分为 `-api`/`-impl` 两个 Maven 子模块，实现了编译期接口隔离。但 patient、doctor、admin 是**扁平单模块**（Controller/Service/Entity/Repository 全部在一个 Maven 模块内），无 API 契约子模块。 |
| **根因** | 文档采用了两种不同的模块分割策略：common-module 和 ai 采用 api/impl 子模块拆分，但 patient/doctor/admin 直接作为叶子模块。§7 第 820 行宣称"模块边界即为未来微服务拆分边界"，但 patient/doctor/admin 的模块边界是**整个堆叠体**，没有"纯接口"的隔离层。 |
| **因果链** | Phase 1+ 需跨模块调用（如 patient 需要查询 admin 的数据）→ 按 §8.4 规范必须通过接口门面 → 但 patient/doctor/admin 没有 ``-api`` 子模块 → 无法定义门面接口 → 要么在 common-module-api 中膨胀所有模块的门面，要么大规模重构 patient/doctor/admin 拆分为 api/impl |
| **影响范围** | Phase 1+ 所有需要 patient/doctor/admin 之间或向它们发起跨模块调用的场景。 |
| **修复方向** | 统一策略：选项 A — patient/doctor/admin 也拆分为 `-api`/`-impl` 子模块（`modules/patient/patient-api` + `patient-impl`），Phase 0 时 impl 仅含占位 Controller；选项 B — 接受 patient/doctor/admin 暂不分层，但在文档中明确"这些模块不对外提供门面接口，仅作为微服务拆分终端节点"，并说明何时需要重构。 |

---

## 二、主要问题（P2 — 影响设计一致性和可编码性）

### P2-1. AI 装配策略泄漏实现细节到业务模块

| 属性 | 内容 |
|------|------|
| **位置** | §3.4（Bean 装配策略第 605 行、装配条件汇总表） |
| **现象** | 要求业务模块通过 `@Qualifier("fallbackAiService")` 按 Bean 名称注入，且 §7 第 833 行的设计决策再次确认此方案。但 §2.2 和 §8.4 反复强调 ai-impl（含 FallbackAiService）对业务模块不可见。 |
| **根因** | `@Qualifier` 引用的 Bean 名称 `"fallbackAiService"` 是 ai-impl 中 `FallbackAiService` 类的 Spring Bean 名称。业务模块的 Java 代码中硬编码此字符串即构成对 ai-impl 实现细节的编译期依赖（虽非 Maven 依赖，但代码逻辑上耦合）。 |
| **因果链** | 设计原则要求"业务模块不感知 ai-impl"→ 但注入代码 `@Qualifier("fallbackAiService")` 要求业务模块知道 impl 中的 Bean 类名 → 如果 FallbackAiService 重命名或 Bean 名称变更，所有业务模块需同步修改 → 隔离名存实亡 |
| **影响范围** | 所有注入 AiService 的业务模块（patient/doctor/admin 的 Service 层）。 |
| **修复方向** | 业务模块只注入 `AiService`（无 Qualifier）。在 application 模块中通过 `@Configuration` 将 FallbackAiService 包装为默认的 `AiService` Bean（标注 `@Primary`）。FallbackAiService 内部通过 `ObjectProvider<AiService>` 排除自身。这样业务模块 POM 中只需依赖 ai-api，代码中 `@Autowired private AiService aiService`，编译期零触达 ai-impl。 |

---

### P2-2. Maven 聚合结构存在两套不可兼得的 reactor 组织

| 属性 | 内容 |
|------|------|
| **位置** | §2.1 父 POM 第 143-154 行（modules 声明）、中间聚合 POM（第 213-256 行） |
| **现象** | 根 `backend/pom.xml` 直接列出的子模块包含 `modules/common-module/common-module-api`、`modules/common-module/common-module-impl`、`modules/ai/ai-api`、`modules/ai/ai-impl` 四个叶子模块。但同时 §2.1 又定义了两个中间聚合 POM：`modules/common-module/pom.xml` 和 `modules/ai/pom.xml`，各自声明其子模块。但根 POM 从未引用这两个中间聚合 POM。 |
| **根因** | 根 POM 的 `<module>` 列表遵循扁平结构（直接列出所有叶子模块），中间聚合 POM 试图构建嵌套聚合层次。两者是互斥的——如果根 POM 引用了 `modules/common-module`（聚合 POM），就不应再列出其内部的子模块。当前写法导致中间聚合 POM 在 reactor 中永远不会被执行。 |
| **因果链** | 实现者看到两套写法 → 不确定采用哪种 → 如果采用扁平风格，中间聚合 POM 成为死代码；如果修改根 POM 引用聚合 POM，CI 流水线的 `-pl` 参数路径需全部调整 → 实现分歧 |
| **影响范围** | Maven 构建流程、CI 流水线配置（§10 第 1190-1194 行）中使用的 `-pl` 路径。 |
| **修复方向** | 明确采用一种风格。建议：保持根 POM 扁平风格（直接列出所有叶子模块），删除中间聚合 POM 或将其标记为注释/参考。同时同步更新 CI 流水线的 `-pl` 参数。 |

---

### P2-3. common-module-impl 的 Phase 0 范围前后矛盾

| 属性 | 内容 |
|------|------|
| **位置** | §3.3（权限模型第 512 行、PermissionServiceImpl 实现示例 §8.4 第 1058-1062 行）vs §8.4 第 1097 行 |
| **现象** | §3.3 第 512 行说"门面接口（如 PermissionService）和 DTO 归属 common-module-api；common-module-impl 仅由 application 模块引入"，暗示 common-module-impl 包含实现。§8.4 第 1058-1062 行给出了 `PermissionServiceImpl` 的 `@Service` 注解实现示例。但 §8.4 第 1097 行（Phase 0 约束）写 "Phase 0 的 common-module-impl 仅提供权限实体定义和 Repository，不提供门面接口实现"。 |
| **根因** | 文档不同章节对 Phase 0 common-module-impl 的范围给出了互斥的定义：§3.3 暗示包含实现，§8.4 给出实现代码示例，§8.4 末尾又说 Phase 0 不提供实现。 |
| **因果链** | 实现者阅读到 §3.3 认为需要实现 `PermissionServiceImpl` → 读到 §8.4 看到完整实现示例 → 读到 §8.4 末尾又说 Phase 0 不做 → 不确定是否需要编码 `PermissionServiceImpl` |
| **影响范围** | common-module-impl 模块的 Phase 0 编码范围界定。 |
| **修复方向** | 明确 Phase 0 common-module-impl 是否包含 `PermissionServiceImpl`。若包含：删除第 1097 行的限制表述，并确保 Phase 0 的 SecurityConfig (`permitAll`) 与 PermissionService 共存时无冲突。若不包含：删除 §3.3 中的 Service 实现提及和 §8.4 的实现示例。 |

---

### P2-4. common 依赖传播的表述自相矛盾

| 属性 | 内容 |
|------|------|
| **位置** | §2.2 第 277 行（依赖方向说明注释） |
| **现象** | §2.2 的图中明确绘制了"业务模块（patient/doctor/admin）→ common"的直接依赖箭头。但紧接的注释第 277 行写"业务模块（patient/doctor/admin）的实际依赖集为 common（**通过 common-module-api 传递获取**）"。两者矛盾。 |
| **根因** | 图形化依赖图与文字注释采用了两套依赖传播假设。图显示直接依赖，文字假设传递依赖。 |
| **因果链** | 实现者按图在 POM 中声明 `common` 依赖 → 重声明但功能正确（Maven 允许）→ 但文档评审查不出此项"不必要的冗余依赖" → 或者实现者按文字不声明 common → 如果 common-module-api 的 POM 调整了某依赖的 scope，可能导致 common 的某些依赖不可达 → 不稳定 |
| **影响范围** | patient、doctor、admin 模块的 POM 依赖声明。 |
| **修复方向** | 统一表述为一种。建议采用"业务模块 POM 直接依赖 common + common-module-api + ai-api"（图所示），因为在 Maven 中显式声明直接依赖比依赖传递更清晰且不受上游 POM 变动影响。同时将 §2.2 注释中的"通过 common-module-api 传递获取"修正。 |

---

### P2-5. SecurityConfig 的 Phase 0 → Phase 1 切换方案不完善

| 属性 | 内容 |
|------|------|
| **位置** | §4.5（第 732-751 行） |
| **现象** | Phase 0 使用 `@Profile("phase0")` 定义 `SecurityConfigPhase0`，全员 permitAll。§4.5 第 749 行说 Phase 1 切换方式为"将 `spring.profiles.active` 从 `phase0` 改为无（或新 profile 名），激活 Phase 1 的 SecurityConfig（标注 `@Profile("!phase0")` 或使用 `@ConditionalOnProperty`）"。此处存在三个问题：1) 切换方式存在两个可选方案（`@Profile("!phase0")` vs `@ConditionalOnProperty`），设计决策未收敛；2) Phase 1 的 SecurityConfig 定义位置未指定（仍在 common 中？还是在某个业务模块中？）；3) `spring.profiles.active=phase0,dev` 在 `application.yml` 中硬编码，修改此值将影响所有 profile 相关的配置（数据源、日志级别等）。 |
| **根因** | 硬编码 profile 名称 `phase0` 于 `application.yml`，且 Phase 1 切换方案存在二义性。 |
| **因果链** | 实现者面临二选一 → 选 `@Profile("!phase0")` 则 SecurityConfigPhase1 需要放在与 Phase0 不同的配置类或同包下 → 若在 common 中，Spring 会加载两个 SecurityFilterChain Bean → 需要额外的 `@Order` 或 `@ConditionalOnMissingBean` 控制优先级 |
| **影响范围** | Phase 0 → Phase 1 过渡时的认证配置切换。 |
| **修复方向** | 明确 Phase 1 SecurityConfig 采用 `@Profile("!phase0")` 且置于 application 模块（非 common），因为 application 是唯一聚合模块，切换 profile 粒度由它控制。移除"或使用 @ConditionalOnProperty"的二义性表述。 |

---

### P2-6. ErrorCode interface 的方法契约未明确定义

| 属性 | 内容 |
|------|------|
| **位置** | §3.1（ErrorCode 设计要点第 467-468 行）、§7 第 828 行（设计决策） |
| **现象** | §3.1 第 467 行定义 "ErrorCode 定义为 interface"，"每个错误码包含 code（字符串）和 message（用户可读描述）"。但未明确 `code` 和 `message` 在 Java interface 中是**方法签名**还是**常量字段**。若实现者为常量字段（`String code = "XXX"`），则 enum 实现时无法通过 `implements ErrorCode` 满足接口契约。 |
| **根因** | Java interface 中变量隐式为 `public static final`，要使各模块的 enum 实例提供不同的 code/message，ErrorCode 必须声明两个抽象方法：`String code(); String message();`。文档未指出这一关键实现细节。 |
| **因果链** | 实现者看到"interface + 各模块 enum 实现"→ 若理解成常量字段则编译错误（enum 的字段与 interface 的 `static final` 冲突）→ 或理解成方法但不确定签名（返回 String？返回 Serializable？）→ 各模块实现不一致 |
| **影响范围** | ErrorCode 的所有 enum 实现（各业务模块）。 |
| **修复方向** | 明确定义 ErrorCode 接口的方法签名：`String code(); String message();`。给出完整的 interface 代码示例而非仅文字描述。 |

---

### P2-7. `sort` 参数设计为自定义 String 格式，与 Spring Data 原生机制不兼容

| 属性 | 内容 |
|------|------|
| **位置** | §3.1（PageQuery 第 457 行） |
| **现象** | 定义 `sort` 为 `String` 类型，格式为 `"fieldName,direction"`。但 Spring Data 内建的 `PageableHandlerMethodArgumentResolver` 原生支持 `sort=field1,asc&sort=field2,desc` 格式，可直接自动解析为 `Pageable` 对象。 |
| **根因** | 文档选择了与 Spring Data 默认机制不同的自定义格式，增加了不必要的解析成本。`PageQuery` 作为 POJO 通过 `@ModelAttribute` 绑定，其 `sort` 字段需要实现者手动编写解析逻辑（拆分逗号，构造 `Sort.by(Sort.Direction.ASC, "fieldName")`）。 |
| **因果链** | Controller 参数声明 `@Valid PageQuery query` → Spring 自动绑定 → `query.sort` 得到原始字符串 `"createdAt,desc"` → 实现者必须手动编写解析代码 → 多字段排序需要额外的分隔符约定（分号？竖线？）→ 契约不完整 |
| **影响范围** | 所有分页接口的 Controller 和 Service 层实现。 |
| **修复方向** | 方案 A：直接使用 Spring Data 的 `@PageableDefault` + `Sort`/`Pageable` 参数，废弃自定义 PageQuery。方案 B：如保留 PageQuery，将 `sort` 定义为 `List<String>`，与 Spring 多值绑定兼容（`?sort=createdAt,desc&sort=id,asc`），内部转换为 `Sort` 对象。 |

---

### P2-8. JPA 多对多关联约定未包含级联清理策略

| 属性 | 内容 |
|------|------|
| **位置** | §3.3（JPA 关系映射约定第 547 行） |
| **现象** | 所有 `@ManyToMany` 两端均不设 cascade，`orphanRemoval = false`。但未约定 Service 层的级联操作模式。例如删除 User 时，是否需要手动清理 `user_role` 和 `user_post` 关联表中的记录？如果不清洗直接删除 User，数据库会报外键约束违反（如设了级联删除约束）或留下孤儿记录（如未设外键约束）。 |
| **根因** | 文档仅约定了 JPA 注解层面的关系映射，未约定业务层和数据层的关联记录管理策略。`@ManyToMany` 的 `cascade` 和 `orphanRemoval` 不控制关联表物理外键约束的行为。 |
| **因果链** | 删除 User → JPA 因无 cascade 不会自动删除 user_role 关联 → 若数据库设了 `ON DELETE CASCADE` 则自动删除（但文档未约定 DDL 策略）→ 或数据库无外键约束则 user_role 留下孤儿记录 → 数据不一致 |
| **影响范围** | 所有涉及多对多关联的实体操作（User↔Role、User↔Post、Post↔Function）。 |
| **修复方向** | 补充约定：在 Service 层通过 Repository 显式清理关联记录（先删除关联再删除实体），或在数据库 DDL 中约定 `ON DELETE CASCADE` 以由数据库保证一致性。文档需明确选择其中一种。 |

---

## 三、次要问题（P3 — 表述不准确或影响可读性）

### P3-1. `-am`（also-make）的解释不准确

| 属性 | 内容 |
|------|------|
| **位置** | §9.3 第 1162 行 |
| **现象** | "`-am` 即 `--also-make`，自动编译依赖模块并安装到本地仓库"。`-am` 的作用是自动**构建**依赖模块（即添加到 reactor 中），而非"安装到本地仓库"。安装到本地仓库是 `mvn install` 的行为（`-DskipTests` 跳过测试）。`spring-boot:run -pl application -am` 会将依赖模块**编译**但不会**安装**到本地仓库（`spring-boot:run` 不属于 `install` 生命周期）。文中"避免首次构建时因依赖模块未安装导致解析失败"是错误归因。 |
| **根因** | `-am` 和 `install` 的概念混淆。 |
| **修复方向** | 修正为"`-am` 即 `--also-make`，将依赖模块一并纳入 reactor 构建，`spring-boot:run` 在执行时会先编译依赖模块确保类路径完整"。 |

---

### P3-2. `common` 依赖 `spring-boot-starter-web` 的表述不严谨

| 属性 | 内容 |
|------|------|
| **位置** | §2.2 第 293 行 |
| **现象** | "所有业务模块均包含 Controller，均需嵌入式 Tomcat 容器"。common 本身不含 Controller，但声明 web starter 是因为其 `GlobalExceptionHandler`（标注 `@ControllerAdvice`）和 `Result<T>` 都需要 web 上下文。表述应从"common 自身的需要"出发而非"业务模块的需要"。 |
| **修复方向** | 修正为"common 中的 `@ControllerAdvice`、`Result<T>` 响应序列化及 CORS 配置需要 `spring-boot-starter-web` 的 `Jackson`、嵌入式 Tomcat 及 Spring MVC 基础设施"。 |

---

### P3-3. springdoc-openapi 与 Knife4j 混用但仅配置了 springdoc

| 属性 | 内容 |
|------|------|
| **位置** | §8.3 第 1021 行 |
| **现象** | 说"通过 `swagger-ui.html` 或 `doc.html`（Knife4j 增强）查阅 API 契约"。但 Knife4j 需要额外的依赖 `com.github.xiaoymin:knife4j-openapi3-jakarta-spring-boot-starter`，此依赖未在文档或 POM 中声明。仅配置 springdoc-openapi 无法访问 `doc.html`（仅 `swagger-ui.html` 可用）。 |
| **修复方向** | 去掉 `doc.html`（Knife4j）的提及，或补全 Knife4j 的依赖声明和配置说明。 |

---

## 四、诊断汇总

| 优先级 | 问题 | 所属章节 | 类型 |
|--------|------|---------|------|
| P1 | AiService 同步/异步契约冲突 | §3.4 / §6 / §8.2 | 契约设计缺陷 |
| P1 | BaseEntity @SQLDelete 模板不可复用 | §3.2 | 实现不可行 |
| P1 | POM 遗漏 springdoc-openapi 和 H2 依赖声明 | §2.1 / §8.3 / §9.1 | 构建阻断 |
| P1 | 业务模块缺少 API/impl 分层 | §2.1 / §2.2 / §7 | 架构不一致 |
| P2 | AI 装配泄漏 @Qualifier 到业务模块 | §3.4 / §7 | 接口隔离违反 |
| P2 | Maven 聚合结构两套不可兼得 | §2.1 | 构建组织分歧 |
| P2 | common-module-impl Phase 0 范围矛盾 | §3.3 / §8.4 | 范围界定歧义 |
| P2 | common 依赖传播表述矛盾 | §2.2 | 表述自相矛盾 |
| P2 | SecurityConfig 切换方案不完善 | §4.5 | 演进路径未收敛 |
| P2 | ErrorCode interface 方法契约未定义 | §3.1 / §7 | 实现歧义 |
| P2 | sort 参数与 Spring Data 不兼容 | §3.1 | 设计选择不当 |
| P2 | JPA 多对多级联清理策略未约定 | §3.3 | 行为约定缺失 |
| P3 | `-am` 的解释不准确 | §9.3 | 事实错误 |
| P3 | `spring-boot-starter-web` 理由不严谨 | §2.2 | 表述不准确 |
| P3 | Knife4j 提及但依赖未声明 | §8.3 | 表述不完整 |

---

## 五、可编码性判断

当前 `Docs/04_ood_phase0.md` 的 **4 个 P1 问题必须全部修订**方可进入编码阶段。P2 问题中的 #1（AI 装配泄漏）和 #2（Maven 聚合结构）在 P1 修复时建议一并解决，其余 P2/P3 可在 Phase 1 编码前修订。

最优先处理的三项：
1. 固定 AiService 契约（P1-1）—— 影响所有业务模块的 AI 调用代码
2. 补全 POM 依赖声明（P1-3）—— 直接阻断构建
3. 统一 Maven 聚合结构（P2-2）—— 决定 CI 流水线和模块组织
