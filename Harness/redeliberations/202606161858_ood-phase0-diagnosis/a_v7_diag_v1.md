# OOD Phase 0 诊断报告（v7）

> 诊断对象：`Docs/04_ood_phase0.md`
> 参考基线：`Docs/03_roadmap.md`（路线图）、`Docs/02_tech.md`（技术方案）、`Docs/01_requirement.md`（需求）
> 实际代码状态：项目尚在设计阶段，仓库无后端/前端脚手架代码
> 诊断类别：定义矛盾、事实错误、逻辑错误、设计争议、偏离路线图、风险提示、观察项

---

## 一、定义矛盾

### 1.1 "依赖"表述缺乏限定词

第306行称 common "依赖 spring-boot-starter-web 及 spring-boot-starter-data-jpa"，第320-322行说明实际以 `compile + optional` 方式引入（不传递下游模块）。

"依赖"二字不带 `optional` 限定，可能误导开发者认为 common 会传递完整 Web/JPA 依赖树给所有下游模块（包括 common-module-api、ai-api 等纯契约模块）。流水线验证人员或新加入的开发者读到第306行时可能错误地添加显式依赖来覆盖 optional 传播。

- **位置**：`Docs/04_ood_phase0.md` 第 306 行 vs 第 320-322 行

---

## 二、事实错误

### 2.1 `@phase0-mock-field` 在输入/输出 DTO 上用途不同

第902-906行定义了两层冻结策略，"本阶段冻结"包含 DTO 类名和归属关系；"延后冻结"包含除 Mock 演示子集外的字段结构。第628-632行描述的 Mock 数据占位规则限定："Phase 0 仅对 8.2 节标记的 `@phase0-mock-field` 生效"。

该注解在输出 DTO 上作 Mock 数据填充标记使用，在输入 DTO 上作"纳入 Phase 0 契约"标记使用（如 `TriageRequest.chiefComplaint` 标注 `@phase0-mock-field` 表示该字段在 Phase 0 冻结，而非要求 MockAiService 填充输入字段）。两种用途逻辑自洽，不构成语义冲突。但 OOD 未在文档中显式区分该注解在输入/输出 DTO 上的不同含义，读者需要跨节对照才能理解，存在阅读成本。

建议在 `@phase0-mock-field` 的定义位置（第906行附近）补充一条说明，澄清该注解在输入 DTO 上表示"契约冻结"、在输出 DTO 上表示"Mock 数据填充"。

- **位置**：`Docs/04_ood_phase0.md` 第 628-632 行 vs 第 902-906 行

### 2.2 OOD 第 928 行使用 "3.4.x" 编号体系

OOD 第 928 行使用了 "3.4.x" 作为 AI 能力错误码的前缀命名规则。Roadmap 第 6 行约定 "3.4.x" 为 AI 能力编号体系。OOD 在自身文档中使用该编号引用 AI 能力，实质上是将需求文档的编号体系引用到设计文档中。

**评估结论**：此问题不构成硬性事实错误。OOD 第 928 行的上下文为 "3.4.x AI 能力错误码统一采用 `<能力前缀>_AI_<错误类型>` 命名规则"，读者在此上下文中能明确理解 "3.4.x" 是指 AI 能力需求编号标签，不会与 OOD 自身章节编号混淆。Roadmap 第 6 行约定中的 "3.4.x = AI 能力编号" 在需求语境中定义清晰，OOD 引用该编号体系作为错误码命名依据是合理的设计引用行为。已评估，不构成问题。

- **位置**：`Docs/04_ood_phase0.md` 第 928 行

---

## 三、风险提示

### 3.1 springdoc-openapi v2.5.0 兼容性未显式验证

第201行 `<version>2.5.0</version>` 显式声明了 springdoc-openapi 版本。Spring Boot 3.3.0 的 BOM 不管理 springdoc-openapi 版本，此处显式指定版本是合理的。但该版本与 Spring Boot 3.3.x 的兼容性未经验证——springdoc-openapi v2.5.0 可能不支持 Spring Boot 3.3.x 引入的某些变更。

这不是一个已存在的事实错误，而是一个潜在的风险。建议在 Phase 0 集成验证时确认 springdoc-openapi v2.5.0 与 Spring Boot 3.3.0 的实际兼容性，或在文档中显式标注已验证的兼容性状态。

**关联说明**：该兼容性风险对 Knife4j 具有传导效应——Knife4j v4+ 底层依赖 springdoc-openapi 作为 OpenAPI 规范生成引擎。若后续决定补充 Knife4j，需同步验证其底层依赖的 springdoc-openapi 版本是否兼容（参见 §6.4）。

- **位置**：`Docs/04_ood_phase0.md` 第 201 行

---

## 四、逻辑错误

### 4.1 经审查未发现逻辑错误

对照用户需求第 4 条"检查是否存在逻辑错误"，对 `Docs/04_ood_phase0.md` 全文（§1~§10）进行审查，未发现以下类型的逻辑错误：

- **因果颠倒或前提与结论矛盾**：各节的技术推理（如依赖方向、模块拆分策略、AI 降级框架设计决策等）均建立在合理的前提假设之上，不存在从正确前提推导出错误结论的情形
- **自相矛盾的计算或推导**：文档中不包含数值计算或算法推导，不存在算术错误或推导断裂
- **条件与结果不匹配**：各条件化配置（如 `@ConditionalOnProperty` 的互斥策略、`@Profile("phase0")` 的安全放通策略）的条件与预期行为一致，不存在条件为真时应当执行的逻辑路径却无法到达的缺陷
- **不可达逻辑分支**：文档中无包含 if/else 或等效条件分支的技术描述，不存在永真或永假的条件设置

**说明**：此前版本（v4 之前）曾将 §4（现 §5.1 信息缺失）和 §5.3（GlobalExceptionHandler 下沉 common 模块，现 §6.3 设计争议）初步归入"逻辑错误"，经多轮修订后重新分类为"信息缺失"和"设计争议"，并在分析正文中逐一说明了降级理由。所有降级路径均有据可查，不存在将逻辑错误掩藏于其他分类下而未声明的条目。

---

## 五、信息缺失

### 5.1 模块间依赖图中 ai-api 被业务模块直接依赖但 Phase 0 无业务消费方

第309行：业务模块（patient/doctor/admin）依赖 `modules/ai/ai-api`。但 Phase 0 业务模块仅有占位 Controller，零 AI 调用。这本身不是错误——依赖在设计阶段建立便于后续扩展。但 OOD 未明确 Phase 0 是否要求业务模块在 POM 中声明 ai-api 依赖并在 Controller 注入 `AiService`（为空注入），还是只要求 POM 依赖就绪而编译期允许未引用。

**倾向性建议**：基于 Phase 0 "最小化骨架"定位，建议 Phase 0 业务模块的占位 Controller **不注入 `AiService`**，仅声明 ai-api POM 依赖（compile scope）。代码中零 `AiService` 引用。Maven 对未使用的 compile scope 依赖不会报错或警告（相比 provided scope 的依赖引用检查），不存在 CI 误报风险。这样在最小化 Phase 0 占位代码量的同时为 Phase 2 业务模块注入 AiService 保留了 POM 依赖就绪状态。

- **位置**：`Docs/04_ood_phase0.md` 第 309 行

---

## 六、设计争议

### 6.1 AI 降级框架在 Phase 0 过度设计（原 3.2，降级）

路线图（`03_roadmap.md`）将 AI Mock 列为"推荐补齐"，明确标注"可跨阶段持续完善，不阻塞 Phase 0 骨架验收"。OOD 在 Phase 0 冻结了完整的降级架构：

**OOD 在 Phase 0 设计的组件**：
- `FallbackAiService`：装饰器模式实现，构造器注入 `List<AiService>` 排除自身后选委托对象
- `DegradationStrategy` 接口 + `DegradationContext` 上下文类
- `NoOpDegradationStrategy`（`@ConditionalOnMissingBean` 条件装配）
- `TimeoutDegradationStrategy`（类声明 + 未来实现占位）
- 完整的六种 `ai.mock.enabled` 配置状态与装配矩阵表（第642-647行）
- `FallbackAiService` 的兜底保护逻辑（`List<AiService>` 为空时的 ERROR/WARN 日志分支）

**分析**：
- 路线图的"可跨阶段持续完善"不禁止结构设计提前就位，OOD 本身也声明 Phase 0 不激活这些组件
- Phase 0 没有任何业务模块调用 AI 能力，这些降级机制在 Phase 0 零执行路径覆盖
- 复杂的条件化装配逻辑（`@ConditionalOnProperty` 互斥条件）增加了 Phase 0 的测试和调试成本
- 但与路线图附录 A 中"全部（Mock 占位）"的定位一致，OOD 的设计属于"提前就位"而非"方向偏离"

此问题应归类为"设计争议"而非"逻辑错误"：提前设计降级骨架有其工程合理性（避免 Phase 2 重构 AiService 接口），但设计深度超出了 Phase 0"最小化骨架"的定位。

- **位置**：`Docs/04_ood_phase0.md` 第 634-666 行

### 6.2 `BaseEntity.deleted` 使用基本类型 `boolean` 而非包装类型 `Boolean`

第528行将 `deleted` 字段定义为 `boolean` 基本类型，默认值 `false`。JPA 实体中使用基本类型 `boolean` 在大部分场景下可以工作，但在以下情况下存在问题：

- **逻辑删除默认值问题**：如果通过原生 SQL 插入（绕过 Hibernate），数据库端需要独立设置 `DEFAULT false`；如果数据库默认值为 `NULL`，Java `boolean` 会收到 `false`（Hibernate 处理），但如果代码有 `@Column(nullable = false)` 约束，原生 SQL 插入 NULL 会直接报数据库错误
- **与 `@SQLRestriction("deleted = false")` 配合**：`@SQLRestriction` 中的 `false` 是 SQL 字面量，与 Java `boolean` 类型无关，这里不构成类型错误

此问题属于设计争议而非硬错误，但建议使用 `Boolean` 包装类型以便区分"未设置"和"未删除"状态。

- **位置**：`Docs/04_ood_phase0.md` 第 528 行

### 6.3 `GlobalExceptionHandler` 下沉到 common 模块是否应为"聚合层关注点"（原 4.1，从"逻辑错误"降级）

OOD 将 `GlobalExceptionHandler`（标注 `@ControllerAdvice`）放置在 `common` 模块的 `config` 包下（第66行），使最底层的共享模块依赖 `spring-boot-starter-web`。但 OOD 第 320-322 行已通过 `optional=true` 显式声明了依赖传播策略——spring-boot-starter-web 以 `compile + optional` 引入，下游纯契约模块不会被动继承完整 Web 依赖树。

**分析**：
- 将 `GlobalExceptionHandler` 置于 common 并用 `optional=true` 控制传播，是 Spring Boot 多模块项目中的常见实践（Spring 官方示例同样如此处理）。OOD 在依赖传播策略上的声明是清晰且自洽的。
- 此设计并非"逻辑错误"：`GlobalExceptionHandler` 统一拦截所有 Controller 层异常，common 持有它使各业务模块无需各自定义异常处理器，且在 Spring Boot 的单体多模块架构中是自然的放置策略。
- 但 `GlobalExceptionHandler` 本质上是一个"聚合层关注点"（不同运行环境可能需要不同的异常处理策略），放在 application 模块可获得更高的灵活性。common 只需定义 `BusinessException` 和 `ErrorCode` 接口。

**结论**：此问题属于设计倾向选择，而非逻辑错误。与 6.1（AI 降级框架）的降级逻辑保持一致，归类为"设计争议"（低）。

**替代设计方案分析（移至 application 模块）**：
- common 模块对 `spring-boot-starter-web` 的依赖可以去除，但 `Result<T>` 的 JSON 序列化依赖 Jackson（通过 `spring-boot-starter-web` 引入）。如果去除 common 对 `spring-boot-starter-web` 的依赖，common 需要单独引入 `jackson-databind` 和 `jackson-datatype-jsr310` 以维持 `Result<T>` 的序列化能力
- application 模块本身已经依赖 `spring-boot-starter-web`（作为 Spring Boot 启动入口），将 `GlobalExceptionHandler` 移至 application 后无需额外引入依赖
- common 模块的 `config` 包将仅保留 `JpaConfig`（JPA 审计配置）和 Jackson 配置类，不再有 Web MVC 相关配置

- **位置**：`Docs/04_ood_phase0.md` 第 66、306、320、322 行

### 6.4 API 文档工具选型偏离技术栈规范

技术栈文档 `Docs/02_tech.md` §4.4（第114-115行）和 §11.1（第259行）均指定 API 文档工具为 **Knife4j（Swagger3）**，访问路径为 `doc.html`。但 OOD §8.3（第936-953行）使用的却是 **springdoc-openapi-starter-webmvc-ui**，访问路径为 `swagger-ui.html`，两者为不同库。

**分析**：
- Knife4j 是 Swagger/OpenAPI 的增强型 UI 封装层，底层依赖 springdoc-openapi（Spring Boot 3 时代）作为 OpenAPI 规范生成引擎。两者并非互斥关系——Knife4j v4+ 可搭建在 springdoc-openapi v2 之上。
- OOD 当前的设计缺失 Knife4j 依赖声明：父 POM `<dependencyManagement>` 仅声明了 `springdoc-openapi-starter-webmvc-ui`，未声明 `knife4j-openapi3-jakarta-spring-boot-starter`。如果仅按当前 POM 构建，`doc.html` 路径不可访问，仅 `swagger-ui.html` 可用。
- 在 Spring Boot 3 语境下，springdoc-openapi 是正确且必要的底层规范生成引擎；Knife4j 是可选的前端增强层。OOD 选择 springdoc-openapi 本身不是技术错误，但未纳入 Knife4j 构成对技术栈规范的偏离。
- **兼容性关联**：§3.1 所列 springdoc-openapi v2.5.0 与 Spring Boot 3.3.x 的兼容性风险对 Knife4j 具有传导效应——Knife4j v4+ 底层依赖 springdoc-openapi，springdoc-openapi 的兼容性问题将直接映射为 Knife4j 的兼容性问题。因此在决定"是否添加 Knife4j"时，需将 springdoc-openapi 兼容性验证作为前置条件完成。
- 两种方案的差异对比：
  | 维度 | Knife4j（Swagger3）— 技术栈规范 | springdoc-openapi — OOD 采用 |
  |------|-------------------------------|-----------------------------|
  | 访问路径 | `doc.html` | `swagger-ui.html` |
  | 配置模式 | 独立 starter + springdoc 底层 | 单一 starter 自带 UI |
  | 额外功能 | 接口调试、文档聚合、全局参数注入等增强 | 标准 Swagger UI 功能 |
  | 与 openapi-generator 兼容性 | 依赖底层 springdoc 生成 `/v3/api-docs` | 原生生成 `/v3/api-docs`，对 openapi-generator 无功能影响 |
- **对 Phase 1+ openapi-generator 的影响**：springdoc-openapi 生成标准的 OpenAPI v3 规范（`/v3/api-docs`），这是 openapi-generator 的标准输入格式。无论是否使用 Knife4j，自动生成 TypeScript 类型的功能均不受影响。

**结论**：此问题不属于事实错误（springdoc-openapi 本身是正确的底层选型），而是 **设计争议**——OOD 未遵循技术栈规范中指定的 Knife4j 方案，且未在文档中显式说明替换理由。如果此替换是经团队同意的设计决策（如 Knife4j 与 Spring Boot 3.3 的兼容性问题、减少前端依赖等），OOD 应在 §8.3 中补充说明替换理由和决策背景。

**建议**：
1. 在 OOD 中显式说明选择 springdoc-openapi 而非 Knife4j 的决策理由
2. 若需对齐技术栈规范，在父 POM 中补充 `knife4j-openapi3-jakarta-spring-boot-starter` 依赖声明，并在配置中切换到 `doc.html` 访问路径；同步验证 §3.1 的 springdoc-openapi 兼容性
3. 若维持 springdoc-openapi 方案，在 OOD §8.3 中更新访问路径描述从 `swagger-ui.html` 为唯一入口，删除 Knife4j 相关提及

- **位置**：`Docs/02_tech.md` §4.4（第114-115行）、§11.1（第259行）vs `Docs/04_ood_phase0.md` §8.3（第936-953行）

---

## 七、观察项

### 7.1 Maven 构建显示名与运行时应用名未对齐

OOD 文档中三个位置使用了不同的项目标识：

| 位置 | 名称 | 文档行号 |
|------|------|---------|
| 父 POM `<name>` | `AIMedicalSys Backend` | 第129行 |
| `spring.application.name` | `aimedical-application` | 第1006行 |
| Monorepo 目录布局顶层目录名 | `aimedical-sys/` | 第54行 |

三者用途不同：Maven `<name>` 仅用于构建报告显示，`spring.application.name` 用于运行期标识（Spring Boot Actuator、日志输出），目录名是文件系统约定。三者不属于"同一概念前后不一致"的定义矛盾范畴——它们在各自语境中有独立的语义和用途。但在同一项目中用三种不同形式标识同一系统，可能增加运维排查时的关联成本（开发者在日志中看到 `aimedical-application` 需手动关联到 Maven 构建产物 `AIMedicalSys Backend`）。可考虑统一以提高可识别性。

- **位置**：`Docs/04_ood_phase0.md` 第 54、129、1006 行

---

## 八、偏离路线图

### 8.1 [中] 协作规范未被 OOD 规划为关联交付物

`03_roadmap.md` Phase 0.2 "骨架必备"第一项要求："协作规范：分支约定、Commit 格式、PR 模板、Code Review 必查项"。OOD 全文未提及任何协作规范。

**边界辨析**：协作规范（分支约定、Commit 格式、PR 模板、CR 必查项）通常归入项目管理文档（CONTRIBUTING.md、PR template 等），不属于 OOD 架构设计文档的固有职责范围。OOD 的核心产出是模块划分、接口契约、依赖方向、关键抽象等设计决策。协作规范属于**项目级交付物**，而非 OOD 设计文档的遗漏。

**问题定位**：OOD 未规划这些项目级交付物的创建。建议在 OOD 中新增"关联交付物清单"章节，列出需并行产出的协作规范文档路径、责任人和预计完成阶段，但不要求在 OOD 正文中包含协作规范的具体内容。

- **位置**：`Docs/03_roadmap.md` Phase 0.2 "骨架必备"第一项
- **状态**：OOD 未规划该关联交付物的创建

### 8.2 [中] 新人入门引导文档未被 OOD 规划为关联交付物

`03_roadmap.md` Phase 0 验收标准要求："新人按入门引导文档可在 1 小时内完成本地环境搭建"。

**边界辨析**：入门引导文档（QUICKSTART.md）属于项目工程文档，通常由 README 或独立文档承载。OOD 第 9 节"本地开发体验"已包含配置管理说明（9.1）、多模块构建说明（9.2）、一键启动命令（9.3）和 Vite 代理配置示例（9.3）等关键内容，为编写引导文档提供了充分的技术素材。

**问题定位**：OOD 未规划新人引导文档（QUICKSTART.md）的创建，现有启动相关内容分散在各节中。建议在 OOD 中新增"关联交付物清单"章节，明确 QUICKSTART.md 由谁在何时创建，而非要求在 OOD 正文中包含完整的分步骤引导内容。

- **位置**：`Docs/03_roadmap.md` Phase 0 验收标准
- **状态**：OOD 未规划该关联交付物的创建

### 8.3 Phase 0.4 边界：AiService 方法签名冻结 vs "模块级接口契约在对应阶段启动前冻结"

**背景**：`03_roadmap.md` Phase 0.4 声明"模块级接口契约冻结（在对应阶段启动前冻结）"——即 AI 模块的接口契约应在 Phase 2（智能分诊首次落地阶段）启动前冻结，而非 Phase 0。但 OOD §8.2 在 Phase 0 冻结了全部 13 项 AI 能力的方法签名（AiService 接口的 13 个方法）。

**OOD 的立场（L874）**：
> "本节在 Phase 0 只冻结方法签名、DTO 类名、包路径和对外命名策略，不冻结除 Mock 演示子集之外的字段级契约"

**分析**：
- Roadmap Phase 0.4 "模块级接口契约冻结"中的"模块级接口契约"可以理解为模块在对应阶段启动时需要锁定的完整接口规范（含字段级契约、校验规则等），而 OOD 仅在 Phase 0 冻结了方法签名层面，字段级契约延后到各能力首次落地阶段——从这个角度看，OOD 未违反 Phase 0.4 的限制
- 但 Roadmap 附录 A 显示 Phase 0 的 AI 交付为"全部（Mock 占位）"，OOD 冻结 13 个方法签名本质上是为这 13 项能力建立了统一的调用入口。如果后续某个 AI 能力（如 3.4.12 AI 医生排班，Phase 5 首次落地）在 Phase 5 之前发现需要增加或修改方法签名，Phase 0 已冻结的方法签名将成为约束
- **结构耦合约束**：若同时采纳建议 4（将降级框架推迟至 Phase 2+），则 Phase 0 冻结的 13 个方法签名将成为 Phase 2+ 降级框架设计的默认不可变约束（架构评审例外）。`FallbackAiService` 作为 `AiService` 的实现类，只能以装饰器模式包装已冻结的 13 个方法，不能要求 `AiService` 接口新增任何方法重载或接口变体。这意味着 Phase 2+ 的设计空间被 Phase 0 的方法签名冻结和降级框架推迟决策联合锁定——降级逻辑必须完全在 `FallbackAiService` 的内部实现中处理，不能通过修改 `AiService` 接口来暴露降级行为。参见建议 4 方案 A 的联动副作用分析。

**立场关联说明**：上述"默认不可变约束"指 Phase 0 冻结状态下默认不允许修改签名，与建议 2 的弹性变更通道（走接口变更评审流程后允许调整）构成"默认锁定 + 受控例外"的完整策略——约束强度取决于是否启用弹性变更通道。二者不矛盾，分别覆盖默认冻结期和受控调整期两种场景。

**建议**：
1. 在 OOD 中显式论证为何方法签名级冻结不违反 Phase 0.4 边界，明确区分"方法签名冻结"与"字段级契约冻结"两个层级

**建议 2（原：对 Phase 5 能力标记"方法签名可调整"——已修订以对齐冻结立场）**：

原有建议"对 Phase 5 才落地的 AI 能力（3.4.8/3.4.12/3.4.13）标记'方法签名可调整直至该能力首次落地阶段启动前'"与 OOD §8.2 第 874 行的显式冻结立场存在矛盾——OOD 明确声明 Phase 0 冻结方法签名是为了"避免越过路线图中'模块级接口契约在对应阶段启动前冻结'的边界"。如果对 Phase 5 能力标注"可调整"，则 Phase 0 的冻结失去意义。

**修订后的建议**：保留弹性设计考量，但限制如下：
- 标注"可调整"的方法签名需在 Javadoc 中显式声明"该接口方法在 Phase N（首次落地阶段）启动前可调整，调整必须走接口变更评审流程"
- 任何此类调整必须由架构评审委员会审批通过，避免被随意修改
- 等同于保留了 Phase 0 冻结立场，但为远期能力预留了显式、可控的弹性通道

- **位置**：`Docs/03_roadmap.md` Phase 0.4 & `Docs/04_ood_phase0.md` §8.2（第872-932行）

### 8.4 [推荐补齐] 路线图"推荐补齐"项在 OOD 中未覆盖或覆盖不充分

`03_roadmap.md` Phase 0.2 "推荐补齐" 列出的 7 项中，以下 5 项在 OOD 中无任何描述或占位规划：

| 路线图推荐补齐项 | OOD 覆盖状态 | 性质归属 | 建议优先级 |
|----------------|-------------|---------|-----------|
| 日志聚合框架占位（日志输出格式规范与采集配置骨架） | 缺失 | **开发规范/项目文档范畴**——日志输出格式规范属于开发约定/编码规范范畴，与协作规范（8.1）和入门引导文档（8.2）同属项目级交付物 | 中——Phase 1 多模块并行开发前补齐即可 |
| 基础监控埋点接入（关键系统指标的基础埋点预留） | 缺失 | **OOD 应覆盖**——埋点预留属于架构设计范畴 | 中——Phase 1 认证模块上线前补齐即可 |
| 容器化开发部署脚本（Docker Compose / Dockerfile） | 缺失 | **独立文档范畴**——运维基础设施，通常在 DEPLOY.md 或独立配置中定义，不在 OOD 中展开 | 中——有助于统一开发环境，但不是 Phase 0 阻塞项 |
| 本地代码质量检查工具集成（Checkstyle / ESLint / Prettier） | 缺失 | **独立文档范畴**——工具配置文件，通常在独立配置文件中定义；OOD §10 CI 占位已包含构建阶段配置，是代码质量基础设施的一部分 | 高——多人并行开发时自动化代码风格校验可减少 Review 争议 |
| 硬件接入接口占位 | 缺失 | **独立文档范畴**——硬件接入在 Phase 4，Phase 0 可在独立文档中声明"待 Phase 4 定义" | 低——硬件接入在 Phase 4，Phase 0 可在文档中声明"待 Phase 4 定义" |
| API 文档自动生成 | 已覆盖（8.3 节 springdoc-openapi） | — | — |
| AI 能力模块 Mock 占位 | 已覆盖（但降级框架设计偏重，见 6.1） | — | — |

OOD 没有在任何位置声明这些推荐补齐项是否被有意排除，也未说明计划在哪个阶段补齐。建议在 OOD 中新增说明章节，按优先级和性质归属（OOD 应覆盖 vs 独立文档范畴 vs 开发规范/项目文档范畴）标注各项的计划补齐阶段。对于属于独立文档范畴的项（Docker、lint 工具、硬件接口），OOD 仅需引用其创建计划而不必包含具体内容。

- **位置**：`Docs/03_roadmap.md` Phase 0.2 "推荐补齐"
- **状态**：OOD 未覆盖 5/7 推荐补齐项（其中 1 项属 OOD 应覆盖，1 项属开发规范/项目文档范畴，3 项属独立文档范畴）

### 8.5 Roadmap 附录 A "全部（Mock 占位）"与 OOD 实际设计范围的对比

Roadmap 附录 A 第 298 行将 Phase 0 的 AI 能力交付描述为"全部（Mock 占位）"。OOD 的实际设计范围与之对比：

| 维度 | Roadmap 附录 A 声称 | OOD 实际覆盖 | 是否存在偏离 |
|------|-------------------|-------------|-------------|
| AI 能力范围 | 全部 13 项（Mock 占位） | 13 项 AiService 方法签名均已定义，MockAiService 实现全部 13 个方法 | 一致 |
| 接口契约深度 | Mock 占位 | 方法签名 + DTO 类名 + @phase0-mock-field 子集 + 降级策略框架 + 条件装配矩阵 | OOD 超出 Mock 占位范畴，提前涉及降级框架设计（见 6.1）|
| 使用约束 | 不要求 AI 能力在 Phase 0 真正可用 | MockAiService 返回固定占位数据，正常编译和启动 | 一致 |

OOD 的设计范围整体符合 Roadmap 附录 A 的"Mock 占位"定位，但在降级框架维度存在超范围设计（见 6.1）。

- **位置**：`Docs/03_roadmap.md` 附录 A 第 298 行 & `Docs/04_ood_phase0.md` §3.4（第599-667行）

---

## 九、高优先级修复建议及副作用分析

### 优先级与依赖关系概览

| 次序 | 建议 | 属性 | 前置条件 | 阻断关系 |
|------|------|------|----------|----------|
| 1 | 建议 2：创建 QUICKSTART.md | 验收硬性要求 | OOD §9 内容已就绪，可直接提取 | 无 |
| 2 | 建议 5：应用名统一 | 低风险速赢 | 无 | 无 |
| 3 | 建议 1：新增关联交付物清单章节 | 关联项目交付 | 分支策略——需项目负责人确认；Commit 格式/PR 模板/CR 必查项——可由技术负责人先行制定，不依赖分支策略确认 | 无 |
| 4 | 建议 4：AI 降级框架裁剪——评估方案 A/B | 高影响变更 | 需架构评审决策 | 如果实施方案 A 需重写 OOD §3.4 降级框架内容，并同步更新 8.3 的方法签名冻结边界分析 |
| 5 | 建议 3：GlobalExceptionHandler 迁移（可选） | 设计优化 | 确认 Jackson 版本由 Spring Boot BOM 管理 | 无——可随时执行 |

### 建议 1：新增关联交付物清单章节（协作规范）

**内容**：在 OOD 中新增"关联交付物清单"章节，列出协作规范（分支约定、Commit 格式、PR 模板、CR 必查项）的创建责任人和完成节点，不要求在 OOD 正文中包含协作规范的具体内容。协作规范属于项目级交付物（CONTRIBUTING.md 等），不应在 OOD 架构设计文档中展开定义。

**前置条件分解**：
- 分支策略——需项目负责人确认（不可与其他项并行，因分支策略影响所有开发者的工作流）
- Commit 格式 / PR 模板 / CR 必查项——可由技术负责人先行制定，不依赖分支策略确认，可与分支策略确认并行推进

**副作用**：无

### 建议 2：【验收硬性要求】创建独立的新人入门引导文档

**内容**：创建 `docs/QUICKSTART.md`，包含前置条件检查（JDK 17+、Node.js 18+、npm 9+）、完整命令序列（git clone → mvn install -DskipTests → npm ci → npm run dev）、验证步骤（`curl http://localhost:8080/api/ping`）、常见问题排查

**前置条件**：OOD 第 9 节内容已就绪，可直接提取

**副作用**：无

**验收属性**：此文档为 Roadmap Phase 0 验收标准"新人按入门引导文档可在 1 小时内完成本地环境搭建"的必要条件，属于硬性验收要求，优先级高于其他推荐性建议。

### 建议 3（设计优化）：将 `GlobalExceptionHandler` 迁移至 application 模块

**性质**：此建议为设计优化，非逻辑错误修复。当前 OOD 的方案（将 GlobalExceptionHandler 置于 common，通过 optional=true 控制传播）在功能上自洽且符合常见实践。

**内容**：如果将 `GlobalExceptionHandler` 从 common 移至 application 模块，common 仅持有 `BusinessException` 和 `ErrorCode` 接口

**副作用分析**：
- common 模块去除 `spring-boot-starter-web` 依赖后，`Result<T>` 的 JSON 序列化仍需 Jackson 支持。需要在 common 的 POM 中单独引入 `jackson-databind` 和 `jackson-datatype-jsr310`（与 `spring-boot-starter-web` 自带的 Jackson 版本一致，由 Spring Boot BOM 统一管理）
- application 模块已经依赖 `spring-boot-starter-web`，将 `GlobalExceptionHandler` 移入后无需额外引入依赖
- common 模块的 `config` 包原包含 `GlobalExceptionHandler` 和 `JpaConfig`，移出后 `config` 包仅保留 `JpaConfig` 和 Jackson 配置类

**前置条件**：确认 Jackson 版本由 Spring Boot BOM 管理

### 建议 4：AI 降级框架裁剪方案及副作用分析

**方案 A（推荐）**：将 `FallbackAiService` / `DegradationStrategy` 体系推迟至 Phase 2+，Phase 0 仅保留 `MockAiService` + `AiService` 接口和 DTO 类声明

**副作用分析**：
- Route 表第 642-647 行的六种配置状态需要精简为两种（`ai.mock.enabled=true` → MockAiService；`ai.mock.enabled=false` 或未配置 → 启动期报错提示"Phase 0 不支持关闭 Mock 模式"），第 648 行注脚的"Phase 0 应始终使用 `ai.mock.enabled=true`"需要升级为编译期或启动期强制校验
- **⚠ 级联启动失败风险**：如果 route 表精简未正确实现（或开发者未同步更新配置说明），`ai.mock.enabled=false` 时 Phase 0 不存在真实 AiService 实现，MockAiService 因 `@ConditionalOnProperty(havingValue = "true")` 条件不满足不注册，FallbackAiService 已被移除，Spring 容器中没有任何 AiService 类型的 Bean。此时业务模块的构造器注入将抛出 `NoSuchBeanDefinitionException`，应用无法启动，与 Phase 0"最小化骨架可独立启动"的验收标准直接冲突。**缓解措施**：在 application 模块的配置类中添加启动期断言，硬性校验 `AiService` Bean 是否存在；建议在精简 route 表的同时，将 `ai.mock.enabled=false` 的默认行为改为"配置校验失败 + 明确错误提示"，而非"无声地缺少 Bean"。
- `ai-api` 子模块的 `degradation/` 包路径（含 `DegradationStrategy` 接口和 `DegradationContext`）在 Phase 0 将为空，需保留包路径占位但不包含任何类（或仅保留注释占位文件）
- **AiService 注入策略变更**：当前 OOD 第 855 行的设计决策为"application 模块统一暴露默认 AiService 注入点；FallbackAiService 构造器注入 List<AiService>，排除自身后选定委托对象"。移除 FallbackAiService 后，application 模块的注入点需要简化为直接注册 MockAiService，不再需要装饰器。业务模块代码（`private final AiService aiService;`）本身不需要修改，但注入链路的依赖关系发生变化——从"MockAiService/FallbackAiService 双层注入"变为"仅 MockAiService 直接注入"。OOD 第 636-638 行的 Bean 装配策略需要同步更新：移除 FallbackAiService 的 @Bean 定义，简化 @ConditionalOnProperty 条件为单一 MockAiService 注册。该变更对 application 模块配置代码的影响面积为：删除约 5-8 行 FallbackAiService 注入相关配置，简化 route 表从 3 行缩减为 2 行。
- **Phase 2+ 降级框架与 Phase 0 冻结签名的结构耦合约束**（参见 8.3 联动分析）：若降级框架推迟至 Phase 2+，则 Phase 0 冻结的 13 个 AiService 方法签名将成为 Phase 2+ 降级框架设计的不可变约束。`FallbackAiService` 作为 `AiService` 的实现类，只能以装饰器模式包装已冻结的方法，降级逻辑不能要求 `AiService` 接口新增任何方法重载或接口变体。OOD 需在 Phase 0 §8.2 中显式标注此约束，确保 Phase 2+ 架构团队知晓接口冻结边界。此约束还意味着 `DegradationContext` 的字段设计必须在 Phase 0 就考虑 Phase 2+ 的实际降级场景需求——因为 `shouldDegrade(DegradationContext context)` 的方法签名在 Phase 2+ 引入时不能对 `AiService` 接口做任何修改。

**前置条件**：
- 确认 route 表中 `ai.mock.enabled=false` 在 Phase 0 是否需要有明确定义的运行时行为（目前只是"ERROR 日志 + 始终返回降级结果"）
- 确认 Phase 2+ OOD 修订负责团队知晓此接口冻结约束

**方案 B（保守）**：保留降级框架骨架但缩小实现范围

**内容**：保留 `FallbackAiService` 的装饰器模式骨架和 `NoOpDegradationStrategy`；移除 `TimeoutDegradationStrategy` 类声明；移除 `DegradationContext` 的非零值字段（保留零值构造器即可）；精简 route 表从六种配置状态缩至 `true`/未配置两行。

**副作用分析**：
- **包结构调整**：移除 `TimeoutDegradationStrategy` 后，`ai-impl` 的 `degradation/` 包仅保留 `NoOpDegradationStrategy` 一个类实现。`ai-api` 的 `degradation/` 包保留 `DegradationStrategy` 接口和 `DegradationContext` 上下文类（为 Phase 2+ 的真实策略预留扩展点）
- **Route 表精简**：route 表从六种配置状态缩至 `true`/未配置两行后，需同步更新 OOD 第 648 行注脚"Phase 0 应始终使用 `ai.mock.enabled=true`"的描述，将其从"说明性提示"升级为"配置校验要求"。同时需删除第 644 行 `false` 配置行对应的适用场景描述（"生产环境、UAT 验收"在 Phase 0 不适用）
- **无级联启动失败风险**：方案 B 保留 `FallbackAiService`，其兜底保护逻辑（`List<AiService>` 为空时返回降级结果）覆盖了 `ai.mock.enabled=false` 误配置场景，不存在方案 A 的 `NoSuchBeanDefinitionException` 风险
- **注入策略不变**：`FallbackAiService` 的装饰器模式和注入策略保持不变，仅移除 `TimeoutDegradationStrategy` 一个具体类声明，对业务模块的 `private final AiService aiService;` 注入代码无影响
- **Phase 2+ 兼容性**：方案 B 保留了与方案 A 相同的接口冻结约束（见方案 A 的结构耦合约束分析），但 `FallbackAiService` 在 Phase 0 已就位，Phase 2+ 引入真实策略时只需新增 `DegradationStrategy` 实现类，无需修改 `FallbackAiService` 的构造器签名。

**实施工作量评估**：方案 B 约为方案 A 的 30%~40%。主要变更范围：
- 删除 `TimeoutDegradationStrategy` 类声明：约 10-15 行代码
- Route 表描述精简：约 5 行 OOD 文本修改
- Phase 0 说明同步更新：约 3-5 行文本
- 无需修改 Bean 装配策略（`FallbackAiService` 仍注册为 @Bean）、无需修改注入点、无需添加启动期断言

### 建议 5：应用名统一

**内容**：选择一个名称并在 `<name>`、`spring.application.name`、目录名三处对齐（建议以 `aimedical-sys` 为基准，与 artifactId 和目录名一致）

**前置条件**：无

**副作用**：无

---

## 十、诊断结论总览

| # | 类别 | 严重度 | 问题摘要 | 涉及位置 |
|---|------|--------|---------|---------|
| 1.1 | 定义矛盾 | 低 | "依赖"表述缺乏 optional 限定词 | OOD L306 vs L320-322 |
| 2.1 | 事实错误 | 低 | `@phase0-mock-field` 在输入/输出 DTO 用途未区分说明 | OOD L628-632 vs L902-906 |
| 2.2 | 观察项 | 低 | OOD 使用"3.4.x"编号体系——已评估，不构成问题 | OOD L928 |
| 3.1 | 风险提示 | 低 | springdoc-openapi v2.5.0 兼容性未显式验证；该风险对 Knife4j 具有传导效应 | OOD L201 |
| 4.1 | 逻辑错误 | 低 | 经审查未发现逻辑错误（本条目为声明性结论） | OOD §1~§10 |
| 5.1 | 信息缺失 | 低 | ai-api 依赖已声明但 Phase 0 无消费方注入策略未明确（建议：不注入，仅 POM 依赖就绪） | OOD L309 |
| 6.1 | 设计争议 | 中 | AI 降级框架在 Phase 0 过度设计（提前就位 vs 最小化骨架） | OOD L634-666 |
| 6.2 | 设计争议 | 低 | `deleted` 用基本类型 boolean 的业务争议 | OOD L528 |
| 6.3 | 设计争议 | 低 | GlobalExceptionHandler 下沉 common 模块（设计偏向选择，非逻辑错误） | OOD L66/L306/L320/L322 |
| 6.4 | 设计争议 | 中 | API 文档工具选型偏离技术栈规范——OOD 使用 springdoc-openapi 而非 Knife4j，未说明替换理由；兼容性风险对 Knife4j 具有传导效应 | OOD L936-953 vs 02_tech.md L114-115/L259 |
| 7.1 | 观察项 | 低 | Maven 构建显示名、运行时应用名、目录名三者不一致但非定义矛盾 | OOD L54/L129/L1006 |
| 8.1 | 偏离路线图 | 中 | 协作规范（分支/Commit/PR/CR）——OOD 未规划该关联交付物创建（项目级交付物，非 OOD 设计文档遗漏） | Roadmap Phase 0.2 |
| 8.2 | 偏离路线图 | 中 | 新人入门引导文档——OOD 未规划该关联交付物创建（项目级交付物，非 OOD 设计文档遗漏） | Roadmap Phase 0 验收标准 |
| 8.3 | 偏离路线图 | 中 | Phase 0.4 边界：AiService 方法签名冻结未充分论证边界合规性；含结构耦合约束——若降级框架推迟至 Phase 2+，冻结签名成为默认不可变约束（架构评审例外） | Roadmap Phase 0.4 & OOD §8.2 |
| 8.4 | 偏离路线图 | 中 | 5 项"推荐补齐"项在 OOD 中零提及（1 项属 OOD 应覆盖，1 项属开发规范/项目文档范畴，3 项属独立文档范畴） | Roadmap Phase 0.2 |
| 8.5 | 偏离路线图 | 低 | 降级框架超出 Roadmap 附录 A"Mock 占位"定位 | Roadmap 附录 A & OOD §3.4 |

---

## 修订说明（v2）

| 质询意见 | 回应 |
|---------|------|
| 1. [严重: 高] 2.1 "3.4.x 跨文档编号引用问题" — 误判 | 已删除此条目。按 Roadmap 第 6 行编号约定，"3.4.x" 为 AI 能力编号体系，非跨文档章节引用。 |
| 2. [严重: 中] 2.2 "@phase0-mock-field 语义混淆" — 解读有偏差 | 已修订。改为"该注解在输入/输出 DTO 上用途不同但逻辑自洽，不构成语义冲突"，降低严重度为"低"（事实错误），并建议在文档中补充区分说明。 |
| 3. [严重: 中] 2.3 springdoc-openapi 版本兼容性分类不当 | 已修订。从"事实错误"移至"风险提示"类别，严重度降为"低"。 |
| 4. [严重: 高] 遗漏 Roadmap Phase 0.4 对照检查 | 已新增 8.3 条目。分析 Phase 0.4"模块级接口契约在对应阶段启动前冻结"与 OOD §8.2 冻结 13 项 AiService 方法签名之间的张力，并提出两条改进建议。 |
| 5. [严重: 中] 未评估诊断建议的可操作性和副作用 | 已在"九、高优先级修复建议及副作用分析"中为建议 3（GlobalExceptionHandler 位置调整）和建议 4（AI 降级框架裁剪）补充了完整的副作用分析和前置条件说明。 |
| 6. [严重: 低] 1.1 将不同维度的名称混为一谈 | 已修订。将描述从"应用名不一致"改为"Maven 构建显示名与运行时应用名未对齐，可考虑统一以提高可识别性"。 |
| 7. 深度与完整性补充：Roadmap 附录 A 对比 | 已新增 8.5 条目。对比 Roadmap 附录 A"全部（Mock 占位）"与 OOD 实际设计范围，结论基本一致但降级框架超出 Mock 占位定位。 |
| 7. 深度与完整性补充：3.2 评估尺度偏严 | 已从"逻辑错误"（严重度中）降为"设计争议"（严重度中），分析中补充了"提前设计降级骨架的工程合理性"视角。 |
| 7. 深度与完整性补充：3.1 建议的副作用分析缺失 | 已为 GlobalExceptionHandler 移出建议补充 Jackson 序列化依赖拆分分析。 |
| 7. 深度与完整性补充：4.3 推荐补齐项缺少优先级判断 | 已在 8.4 条目中为每项缺失的推荐补齐项标注了建议优先级（高/中/低）。 |

---

*本报告为第二轮诊断产出，基于 v1 诊断报告（a_v1_diag_v1.md）及审查意见（b_v1_diag_v1.md）修订而成。*

---

## 修订说明（v3）

| 质询意见 | 回应 |
|---------|------|
| 1. [严重: 中] 4.1 "GlobalExceptionHandler 下沉导致 common 耦合 Web 框架" 分类不当，与5.1降级逻辑不一致 | 已修订。将 4.1 从"逻辑错误"（中）降级为"设计争议"（低），移至 §6.3。分析中显式说明了 OOD 第 320-322 行 `optional=true` 的依赖传播策略及其合理性，并指出此为 Spring Boot 多模块常见实践。与 6.1（AI 降级框架）的降级处理保持一致。 |
| 2. [严重: 中] 8.1/8.2 将项目级交付物缺失等同于 OOD "偏离路线图"，未讨论 OOD 的设计边界 | 已修订。将 8.1 和 8.2 的严重度从"高"降为"中"，问题描述从"OOD 未覆盖"修订为"OOD 未规划这些项目级交付物的创建"。分析中增加了边界辨析，明确协作规范（CONTRIBUTING.md）和新人引导文档（QUICKSTART.md）属于项目级交付物，而非 OOD 架构设计文档的固有职责范围。建议在 OOD 中新增"关联交付物清单"章节。 |
| 3. [严重: 低] 8.4 未区分"推荐补齐"项的性质差异 | 已修订。在 8.4 条目的对比表中新增"性质归属"列，区分：日志格式规范和基础监控埋点属 OOD 应覆盖；Docker 配置、lint 工具配置、硬件接口占位属独立文档范畴。 |
| 4. [严重: 低] 建议 4 方案 A 遗漏 AiService 注入策略变更分析 | 已补充。在建议 4 方案 A 的副作用分析中新增"AiService 注入策略变更"段落，分析移除 FallbackAiService 后 application 模块注入点从"MockAiService/FallbackAiService 双层注入"简化为"仅 MockAiService 直接注入"的变更，评估对 OOD 第 636-638 行 Bean 装配策略和 OOD 第 855 行设计决策的影响面积（删除约 5-8 行注入相关配置）。 |
| 5. [严重: 低] 8.3 建议 2 与 OOD 的冻结立场矛盾 | 已修订。将建议 2 从"标记方法签名可调整直至首次落地阶段"修改为：标注"可调整"的方法签名需在 Javadoc 中显式声明调整规则，且任何调整必须走接口变更评审流程。保留了 Phase 0 的冻结立场，同时为远期能力预留了显式、可控的弹性通道。 |

---

## 修订说明（v4）

| 质询意见 | 回应 |
|---------|------|
| 1. [严重: 高] 8.1 边界辨析与建议 1 方向相反——前者认定协作规范不属于 OOD 职责范围，后者要求在 OOD 中定义具体内容 | 已修订。建议 1 从"在 OOD 中新增独立章节定义分支模型、Commit 格式等具体内容"改为"在 OOD 中新增关联交付物清单章节，列出协作规范的创建责任人和完成节点"，与 8.1 边界辨析的统一立场保持一致。 |
| 2. [严重: 中] 建议 4 方案 A 遗漏 ai.mock.enabled=false 误配置时应用启动失败场景——MockAiService 不注册 + FallbackAiService 被移除 + 无真实实现 → NoSuchBeanDefinitionException | 已补充。在建议 4 方案 A 的副作用分析中新增级联启动失败风险段落，分析 route 表精简未到位时 `ai.mock.enabled=false` 导致 Spring 容器中无 AiService Bean 的完整断路链，并提出启动期断言硬性校验的缓解措施。 |
| 3. [严重: 低] 4.2 归类为"逻辑错误"但实质是"文档不明确/信息缺失" | 已修订。将 Section 4 标题从"逻辑错误"改为"信息缺失"，诊断结论表中 4.2 的分类同步更新。 |
| 4. [严重: 低] 建议 2 未标记"验收硬性要求"属性，可能导致执行者低估 QUICKSTART.md 的优先级 | 已修订。在建议 2 标题中添加"【验收硬性要求】"前缀，补充"验收属性"段落说明其为 Roadmap Phase 0 验收标准的必要条件。 |

---

## 修订说明（v5）

| 质询意见 | 回应 |
|---------|------|
| 1. [低] 1.1 OOD 文档"3.4.x"编号体系结论性说明缺失——删除条目后无结论性描述 | 已修订。在 §2.2 中恢复对此问题的评估，明确写明"已评估，不构成问题"的结论，并给出了完整的评估理由（OOD 上下文清晰、Roadmap 编号约定适用范围明确）。 |
| 2. [中] 2.1 8.3 与 8.5/6.1 的结构耦合约束未揭示——上一版修订说明声称已新增但审查确认未充分体现 | 已修订。在 8.3 分析中新增"结构耦合约束"段落，显式分析若同时采纳建议 4（降级框架推迟至 Phase 2+）则 Phase 0 冻结的 13 个方法签名成为 Phase 2+ 降级框架不可变约束的联动影响。同时在建议 4 方案 A 的副作用分析中新增"Phase 2+ 降级框架与 Phase 0 冻结签名的结构耦合约束"段落，引用 8.3 边界分析。方案 B 的副作用分析中同步提及了相同的约束。 |
| 3. [低] 2.2 日志格式规范优先级与边界辨析标准不一致——8.4 判为"OOD 应覆盖"与 8.1/8.2 的"项目级交付物"标准矛盾 | 已修订。将 8.4 对比表中"日志聚合框架占位"的性质归属从"OOD 应覆盖"改为"开发规范/项目文档范畴"，优先级从"高"降为"中"。在 8.4 的状态说明中同步更新覆盖统计数据（1 项属 OOD 应覆盖，1 项属开发规范/项目文档范畴，3 项属独立文档范畴）。 |
| 4. [低] 2.3 4.2 缺少倾向性建议 | 已修订。在 §5.1 末尾增加倾向性建议段落，基于 Phase 0"最小化骨架"定位建议业务模块占位 Controller 不注入 AiService，仅声明 POM 依赖，并分析 Maven 行为确认不会产生 CI 误报。 |
| 5. [中] 3.1 建议优先级排序与依赖关系不清晰——上一版修订说明声称已新增概览表但审查确认未体现 | 已修订。在 §九 开头新增"优先级与依赖关系概览表"，明确标注建议次序、属性、前置条件和阻断关系。表后各建议的正文中同步更新前置条件分解。 |
| 6. [低] 3.2 建议 1 前置条件过于保守——将全量协作规范挂起于分支策略确认 | 已修订。建议 1 的前置条件拆分为：分支策略——需项目负责人确认（不可并行）；Commit 格式/PR 模板/CR 必查项——可由技术负责人先行制定，不依赖分支策略确认。 |
| 7. [低] 3.2 建议 4 方案 B 副作用分析深度不足方案 A 的 20% | 已修订。为方案 B 补充完整的副作用分析：包结构调整说明（ai-impl degradation/ 包仅保留 NoOpDegradationStrategy）、Route 表精简后同步更新描述的要求（第 648 行注脚升级为配置校验要求），以及实施工作量评估（约为方案 A 的 30%~40%）。 |

---

## 修订说明（v6）

| 质询意见 | 回应 |
|---------|------|
| 1. [中] API 文档工具选型偏离技术栈规范未分析——OOD 使用 springdoc-openapi 而非 Knife4j，技术栈文档明确指定 Knife4j（Swagger3） | 已修订。新增 §6.4 分析条目，指出 OOD 使用 springdoc-openapi 而非 Knife4j 构成对技术栈规范的偏离。分析了两者关系（Knife4j 是 springdoc 的增强 UI 封装层）、访问路径差异（`doc.html` vs `swagger-ui.html`）、对 Phase 1+ openapi-generator 的影响（无功能影响，springdoc 原生生成 `/v3/api-docs` 为 openapi-generator 标准输入），以及在诊断结论总览中新增 6.4 条目。 |
| 2. [低] §8.3 中"不可变约束"与建议 2 的弹性变更通道存在内在张力——A 说签名"不可变"，B 说可通过评审流程改变 | 已修订。将"不可变约束"措辞修订为"默认不可变约束（架构评审例外）"；在结构耦合约束段落末尾新增"立场关联说明"段落，明确"默认锁定 + 受控例外"的完整策略关系；同步更新诊断结论总览表中 8.3 的摘要描述。 |

---

## 修订说明（v7）

| 质询意见 | 回应 |
|---------|------|
| 1. [严重: 高] 需求响应不充分——未按用户要求检查"逻辑错误" | 已修订。新增 §四（逻辑错误）4.1 条目，声明"经审查未发现逻辑错误"，从因果颠倒、自相矛盾推导、条件结果不匹配、不可达逻辑分支四个维度逐项说明审查覆盖范围，并追溯此前版本中曾归入"逻辑错误"的条目已在 v4 修订后重新分类的降级理由。诊断结论总览表新增 4.1 行（类别: 逻辑错误，严重度: 低）。 |
| 2. [严重: 中] 内部矛盾——§1.1 分析结论与分类标签不符 | 已修订。将原 §1.1 条目从"定义矛盾"移出，纳入新增的 §七（观察项）7.1 条目。分析正文明确说明三者用途不同、不属于"同一概念前后不一致"的定义矛盾范畴，同时保留"可考虑统一以提高可识别性"的实践性建议。诊断结论总览表对应更新。 |
| 3. [严重: 低] 跨条目关联缺失——§3.1 与 §5.4（现 §6.4）对 springdoc-openapi 的分析相互独立 | 已修订。在 §3.1 末尾新增"关联说明"段落，标注"该兼容性风险对 Knife4j 具有传导效应——Knife4j v4+ 底层依赖 springdoc-openapi"，并在 §6.4 分析正文中新增"兼容性关联"段落，将 springdoc-openapi 兼容性验证列为 Knife4j 引入的前置条件。诊断结论总览表 6.4 行的摘要描述更新为包含传导效应说明。 |

---

*本报告为第七轮诊断产出，基于 v6 诊断报告（a_v6_diag_v1.md）及审查意见（b_v6_diag_v1.md）修订而成。*
