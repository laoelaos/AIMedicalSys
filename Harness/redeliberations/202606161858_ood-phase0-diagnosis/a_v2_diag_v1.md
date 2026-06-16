# OOD Phase 0 诊断报告（v2）

> 诊断对象：`Docs/04_ood_phase0.md`
> 参考基线：`Docs/03_roadmap.md`（路线图）、`Docs/02_tech.md`（技术方案）、`Docs/01_requirement.md`（需求）
> 实际代码状态：项目尚在设计阶段，仓库无后端/前端脚手架代码
> 诊断类别：定义矛盾、事实错误、逻辑错误、设计争议、偏离路线图、风险提示

---

## 一、定义矛盾

### 1.1 Maven 构建显示名与运行时应用名未对齐

OOD 文档中三个位置使用了不同的项目标识：

| 位置 | 名称 | 文档行号 |
|------|------|---------|
| 父 POM `<name>` | `AIMedicalSys Backend` | 第129行 |
| `spring.application.name` | `aimedical-application` | 第1006行 |
| Monorepo 目录布局顶层目录名 | `aimedical-sys/` | 第54行 |

三者用途不同：Maven `<name>` 仅用于构建报告显示，`spring.application.name` 用于运行期标识（Spring Boot Actuator、日志输出），目录名是文件系统约定。三者不存在"定义矛盾"，但在同一项目中用三种不同形式标识同一系统，可能增加运维排查时的关联成本（开发者在日志中看到 `aimedical-application` 需手动关联到 Maven 构建产物 `AIMedicalSys Backend`）。可考虑统一以提高可识别性。

- **位置**：`Docs/04_ood_phase0.md` 第 54、129、1006 行

### 1.2 "依赖"表述缺乏限定词

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

---

## 三、风险提示

### 3.1 springdoc-openapi v2.5.0 兼容性未显式验证

第201行 `<version>2.5.0</version>` 显式声明了 springdoc-openapi 版本。Spring Boot 3.3.0 的 BOM 不管理 springdoc-openapi 版本，此处显式指定版本是合理的。但该版本与 Spring Boot 3.3.x 的兼容性未经验证——springdoc-openapi v2.5.0 可能不支持 Spring Boot 3.3.x 引入的某些变更。

这不是一个已存在的事实错误，而是一个潜在的风险。建议在 Phase 0 集成验证时确认 springdoc-openapi v2.5.0 与 Spring Boot 3.3.0 的实际兼容性，或在文档中显式标注已验证的兼容性状态。

- **位置**：`Docs/04_ood_phase0.md` 第 201 行

---

## 四、逻辑错误

### 4.2 模块间依赖图中 ai-api 被业务模块直接依赖但 Phase 0 无业务消费方

第309行：业务模块（patient/doctor/admin）依赖 `modules/ai/ai-api`。但 Phase 0 业务模块仅有占位 Controller，零 AI 调用。这本身不是错误——依赖在设计阶段建立便于后续扩展。但 OOD 未明确 Phase 0 是否要求业务模块在 POM 中声明 ai-api 依赖并在 Controller 注入 `AiService`（为空注入），还是只要求 POM 依赖就绪而编译期允许未引用。

如果要求 Phase 0 在业务模块 Controller 中注入 `AiService`，则 Controller 需要有 `private final AiService aiService` 字段和构造器注入代码，增加 Phase 0 占位代码量。如果不要求注入则 POM 声明依赖但无使用，Maven 不会报错（相比 `provided` scope 的依赖引用检查），但会给代码审查者留下"未使用依赖"的疑问。

- **位置**：`Docs/04_ood_phase0.md` 第 309 行

---

## 五、设计争议

### 5.1 AI 降级框架在 Phase 0 过度设计（原 3.2，降级）

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

### 5.2 `BaseEntity.deleted` 使用基本类型 `boolean` 而非包装类型 `Boolean`

第528行将 `deleted` 字段定义为 `boolean` 基本类型，默认值 `false`。JPA 实体中使用基本类型 `boolean` 在大部分场景下可以工作，但在以下情况下存在问题：

- **逻辑删除默认值问题**：如果通过原生 SQL 插入（绕过 Hibernate），数据库端需要独立设置 `DEFAULT false`；如果数据库默认值为 `NULL`，Java `boolean` 会收到 `false`（Hibernate 处理），但如果代码有 `@Column(nullable = false)` 约束，原生 SQL 插入 NULL 会直接报数据库错误
- **与 `@SQLRestriction("deleted = false")` 配合**：`@SQLRestriction` 中的 `false` 是 SQL 字面量，与 Java `boolean` 类型无关，这里不构成类型错误

此问题属于设计争议而非硬错误，但建议使用 `Boolean` 包装类型以便区分"未设置"和"未删除"状态。

- **位置**：`Docs/04_ood_phase0.md` 第 528 行

### 5.3 `GlobalExceptionHandler` 下沉到 common 模块是否应为"聚合层关注点"（原 4.1，从"逻辑错误"降级）

OOD 将 `GlobalExceptionHandler`（标注 `@ControllerAdvice`）放置在 `common` 模块的 `config` 包下（第66行），使最底层的共享模块依赖 `spring-boot-starter-web`。但 OOD 第 320-322 行已通过 `optional=true` 显式声明了依赖传播策略——spring-boot-starter-web 以 `compile + optional` 引入，下游纯契约模块不会被动继承完整 Web 依赖树。

**分析**：
- 将 `GlobalExceptionHandler` 置于 common 并用 `optional=true` 控制传播，是 Spring Boot 多模块项目中的常见实践（Spring 官方示例同样如此处理）。OOD 在依赖传播策略上的声明是清晰且自洽的。
- 此设计并非"逻辑错误"：`GlobalExceptionHandler` 统一拦截所有 Controller 层异常，common 持有它使各业务模块无需各自定义异常处理器，且在 Spring Boot 的单体多模块架构中是自然的放置策略。
- 但 `GlobalExceptionHandler` 本质上是一个"聚合层关注点"（不同运行环境可能需要不同的异常处理策略），放在 application 模块可获得更高的灵活性。common 只需定义 `BusinessException` 和 `ErrorCode` 接口。

**结论**：此问题属于设计倾向选择，而非逻辑错误。与 5.1（AI 降级框架）的降级逻辑保持一致，归类为"设计争议"（低）。

**替代设计方案分析（移至 application 模块）**：
- common 模块对 `spring-boot-starter-web` 的依赖可以去除，但 `Result<T>` 的 JSON 序列化依赖 Jackson（通过 `spring-boot-starter-web` 引入）。如果去除 common 对 `spring-boot-starter-web` 的依赖，common 需要单独引入 `jackson-databind` 和 `jackson-datatype-jsr310` 以维持 `Result<T>` 的序列化能力
- application 模块本身已经依赖 `spring-boot-starter-web`（作为 Spring Boot 启动入口），将 `GlobalExceptionHandler` 移至 application 后无需额外引入依赖
- common 模块的 `config` 包将仅保留 `JpaConfig`（JPA 审计配置）和 Jackson 配置类，不再有 Web MVC 相关配置

- **位置**：`Docs/04_ood_phase0.md` 第 66、306、320、322 行

---

## 六、偏离路线图

### 6.1 [中] 协作规范未被 OOD 规划为关联交付物

`03_roadmap.md` Phase 0.2 "骨架必备"第一项要求："协作规范：分支约定、Commit 格式、PR 模板、Code Review 必查项"。OOD 全文未提及任何协作规范。

**边界辨析**：协作规范（分支约定、Commit 格式、PR 模板、CR 必查项）通常归入项目管理文档（CONTRIBUTING.md、PR template 等），不属于 OOD 架构设计文档的固有职责范围。OOD 的核心产出是模块划分、接口契约、依赖方向、关键抽象等设计决策。协作规范属于**项目级交付物**，而非 OOD 设计文档的遗漏。

**问题定位**：OOD 未规划这些项目级交付物的创建。建议在 OOD 中新增"关联交付物清单"章节，列出需并行产出的协作规范文档路径、责任人和预计完成阶段，但不要求在 OOD 正文中包含协作规范的具体内容。

- **位置**：`Docs/03_roadmap.md` Phase 0.2 "骨架必备"第一项
- **状态**：OOD 未规划该关联交付物的创建

### 6.2 [中] 新人入门引导文档未被 OOD 规划为关联交付物

`03_roadmap.md` Phase 0 验收标准要求："新人按入门引导文档可在 1 小时内完成本地环境搭建"。

**边界辨析**：入门引导文档（QUICKSTART.md）属于项目工程文档，通常由 README 或独立文档承载。OOD 第 9 节"本地开发体验"已包含配置管理说明（9.1）、多模块构建说明（9.2）、一键启动命令（9.3）和 Vite 代理配置示例（9.3）等关键内容，为编写引导文档提供了充分的技术素材。

**问题定位**：OOD 未规划新人引导文档（QUICKSTART.md）的创建，现有启动相关内容分散在各节中。建议在 OOD 中新增"关联交付物清单"章节，明确 QUICKSTART.md 由谁在何时创建，而非要求在 OOD 正文中包含完整的分步骤引导内容。

- **位置**：`Docs/03_roadmap.md` Phase 0 验收标准
- **状态**：OOD 未规划该关联交付物的创建

### 6.3 Phase 0.4 边界：AiService 方法签名冻结 vs "模块级接口契约在对应阶段启动前冻结"

**背景**：`03_roadmap.md` Phase 0.4 声明"模块级接口契约冻结（在对应阶段启动前冻结）"——即 AI 模块的接口契约应在 Phase 2（智能分诊首次落地阶段）启动前冻结，而非 Phase 0。但 OOD §8.2 在 Phase 0 冻结了全部 13 项 AI 能力的方法签名（AiService 接口的 13 个方法）。

**OOD 的立场（L874）**：
> "本节在 Phase 0 只冻结方法签名、DTO 类名、包路径和对外命名策略，不冻结除 Mock 演示子集之外的字段级契约"

**分析**：
- Roadmap Phase 0.4 "模块级接口契约冻结"中的"模块级接口契约"可以理解为模块在对应阶段启动时需要锁定的完整接口规范（含字段级契约、校验规则等），而 OOD 仅在 Phase 0 冻结了方法签名层面，字段级契约延后到各能力首次落地阶段——从这个角度看，OOD 未违反 Phase 0.4 的限制
- 但 Roadmap 附录 A 显示 Phase 0 的 AI 交付为"全部（Mock 占位）"，OOD 冻结 13 个方法签名本质上是为这 13 项能力建立了统一的调用入口。如果后续某个 AI 能力（如 3.4.12 AI 医生排班，Phase 5 首次落地）在 Phase 5 之前发现需要增加或修改方法签名，Phase 0 已冻结的方法签名将成为约束

**建议**：
1. 在 OOD 中显式论证为何方法签名级冻结不违反 Phase 0.4 边界，明确区分"方法签名冻结"与"字段级契约冻结"两个层级

**建议 2（原：对 Phase 5 能力标记"方法签名可调整"——已修订以对齐冻结立场）**：

原有建议"对 Phase 5 才落地的 AI 能力（3.4.8/3.4.12/3.4.13）标记'方法签名可调整直至该能力首次落地阶段启动前'"与 OOD §8.2 第 874 行的显式冻结立场存在矛盾——OOD 明确声明 Phase 0 冻结方法签名是为了"避免越过路线图中'模块级接口契约在对应阶段启动前冻结'的边界"。如果对 Phase 5 能力标注"可调整"，则 Phase 0 的冻结失去意义。

**修订后的建议**：保留弹性设计考量，但限制如下：
- 标注"可调整"的方法签名需在 Javadoc 中显式声明"该接口方法在 Phase N（首次落地阶段）启动前可调整，调整必须走接口变更评审流程"
- 任何此类调整必须由架构评审委员会审批通过，避免被随意修改
- 等同于保留了 Phase 0 冻结立场，但为远期能力预留了显式、可控的弹性通道

- **位置**：`Docs/03_roadmap.md` Phase 0.4 & `Docs/04_ood_phase0.md` §8.2（第872-932行）

### 6.4 [推荐补齐] 路线图"推荐补齐"项在 OOD 中未覆盖或覆盖不充分

`03_roadmap.md` Phase 0.2 "推荐补齐" 列出的 7 项中，以下 5 项在 OOD 中无任何描述或占位规划：

| 路线图推荐补齐项 | OOD 覆盖状态 | 性质归属 | 建议优先级 |
|----------------|-------------|---------|-----------|
| 日志聚合框架占位（日志输出格式规范与采集配置骨架） | 缺失 | **OOD 应覆盖**——日志格式规范属于架构设计范畴，应纳入 OOD | 高——Phase 0 开发期即可受益于统一日志格式 |
| 基础监控埋点接入（关键系统指标的基础埋点预留） | 缺失 | **OOD 应覆盖**——埋点预留属于架构设计范畴 | 中——Phase 1 认证模块上线前补齐即可 |
| 容器化开发部署脚本（Docker Compose / Dockerfile） | 缺失 | **独立文档范畴**——运维基础设施，通常在 DEPLOY.md 或独立配置中定义，不在 OOD 中展开 | 中——有助于统一开发环境，但不是 Phase 0 阻塞项 |
| 本地代码质量检查工具集成（Checkstyle / ESLint / Prettier） | 缺失 | **独立文档范畴**——工具配置文件，通常在独立配置文件中定义；OOD §10 CI 占位已包含构建阶段配置，是代码质量基础设施的一部分 | 高——多人并行开发时自动化代码风格校验可减少 Review 争议 |
| 硬件接入接口占位 | 缺失 | **独立文档范畴**——硬件接入在 Phase 4，Phase 0 可在独立文档中声明"待 Phase 4 定义" | 低——硬件接入在 Phase 4，Phase 0 可在文档中声明"待 Phase 4 定义" |
| API 文档自动生成 | 已覆盖（8.3 节 springdoc-openapi） | — | — |
| AI 能力模块 Mock 占位 | 已覆盖（但降级框架设计偏重，见 5.1） | — | — |

OOD 没有在任何位置声明这些推荐补齐项是否被有意排除，也未说明计划在哪个阶段补齐。建议在 OOD 中新增说明章节，按优先级和性质归属（OOD 应覆盖 vs 独立文档范畴）标注各项的计划补齐阶段。对于属于独立文档范畴的项（Docker、lint 工具、硬件接口），OOD 仅需引用其创建计划而不必包含具体内容。

- **位置**：`Docs/03_roadmap.md` Phase 0.2 "推荐补齐"
- **状态**：OOD 未覆盖 5/7 推荐补齐项（其中 2 项属 OOD 应覆盖，3 项属独立文档范畴）

### 6.5 Roadmap 附录 A "全部（Mock 占位）"与 OOD 实际设计范围的对比

Roadmap 附录 A 第 298 行将 Phase 0 的 AI 能力交付描述为"全部（Mock 占位）"。OOD 的实际设计范围与之对比：

| 维度 | Roadmap 附录 A 声称 | OOD 实际覆盖 | 是否存在偏离 |
|------|-------------------|-------------|-------------|
| AI 能力范围 | 全部 13 项（Mock 占位） | 13 项 AiService 方法签名均已定义，MockAiService 实现全部 13 个方法 | 一致 |
| 接口契约深度 | Mock 占位 | 方法签名 + DTO 类名 + @phase0-mock-field 子集 + 降级策略框架 + 条件装配矩阵 | OOD 超出 Mock 占位范畴，提前涉及降级框架设计（见 5.1）|
| 使用约束 | 不要求 AI 能力在 Phase 0 真正可用 | MockAiService 返回固定占位数据，正常编译和启动 | 一致 |

OOD 的设计范围整体符合 Roadmap 附录 A 的"Mock 占位"定位，但在降级框架维度存在超范围设计（见 5.1）。

- **位置**：`Docs/03_roadmap.md` 附录 A 第 298 行 & `Docs/04_ood_phase0.md` §3.4（第599-667行）

---

## 七、高优先级修复建议及副作用分析

### 建议 1：新增协作规范章节
**内容**：在 OOD 中新增独立章节，定义分支模型（推荐 Trunk-based 或 Git Flow 简化版）、Commit Message 格式（推荐 Conventional Commits）、PR 模板结构、Code Review 必查清单
**前置条件**：需项目负责人确认分支策略
**副作用**：无

### 建议 2：创建独立的新人入门引导文档
**内容**：创建 `docs/QUICKSTART.md`，包含前置条件检查（JDK 17+、Node.js 18+、npm 9+）、完整命令序列（git clone → mvn install -DskipTests → npm ci → npm run dev）、验证步骤（`curl http://localhost:8080/api/ping`）、常见问题排查
**前置条件**：OOD 第 9 节内容已就绪，可直接提取
**副作用**：无

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
- `ai-api` 子模块的 `degradation/` 包路径（含 `DegradationStrategy` 接口和 `DegradationContext`）在 Phase 0 将为空，需保留包路径占位但不包含任何类（或仅保留注释占位文件）
- **AiService 注入策略变更**：当前 OOD 第 855 行的设计决策为"application 模块统一暴露默认 AiService 注入点；FallbackAiService 构造器注入 List<AiService>，排除自身后选定委托对象"。移除 FallbackAiService 后，application 模块的注入点需要简化为直接注册 MockAiService，不再需要装饰器。业务模块代码（`private final AiService aiService;`）本身不需要修改，但注入链路的依赖关系发生变化——从"MockAiService/FallbackAiService 双层注入"变为"仅 MockAiService 直接注入"。OOD 第 636-638 行的 Bean 装配策略需要同步更新：移除 FallbackAiService 的 @Bean 定义，简化 @ConditionalOnProperty 条件为单一 MockAiService 注册。该变更对 application 模块配置代码的影响面积为：删除约 5-8 行 FallbackAiService 注入相关配置，简化 route 表从 3 行缩减为 2 行。
- Phase 2+ 引入降级框架时，需重新设计 `FallbackAiService` 的构造器签名以兼容 Phase 0 已冻结的 `AiService` 接口。由于 `FallbackAiService` 实现的是 `AiService` 接口，而该接口在 Phase 0 已冻结方法签名，Phase 2+ 的设计不能修改 `AiService` 的结构——`FallbackAiService` 只能通过装饰器模式包装已冻结的 `AiService` 方法，降级逻辑不能要求 `AiService` 接口新增任何方法
**前置条件**：
- 确认 route 表中 `ai.mock.enabled=false` 在 Phase 0 是否需要有明确定义的运行时行为（目前只是"ERROR 日志 + 始终返回降级结果"）
- 确认 Phase 2+ OOD 修订负责团队知晓此接口冻结约束

**方案 B（保守）**：保留降级框架骨架但缩小实现范围——移除 `TimeoutDegradationStrategy` 类声明、移除 `DegradationContext` 的非零值字段（保留零值构造器即可）、移除 route 表的 `false` 配置行的编辑器；保留 `FallbackAiService` 的装饰器模式骨架和 `NoOpDegradationStrategy`

### 建议 5：应用名统一
**内容**：选择一个名称并在 `<name>`、`spring.application.name`、目录名三处对齐（建议以 `aimedical-sys` 为基准，与 artifactId 和目录名一致）
**前置条件**：无
**副作用**：无

---

## 八、诊断结论总览

| # | 类别 | 严重度 | 问题摘要 | 涉及位置 |
|---|------|--------|---------|---------|
| 1.1 | 定义矛盾 | 低 | Maven 构建显示名与运行时应用名未对齐 | OOD L54/L129/L1006 |
| 1.2 | 定义矛盾 | 低 | "依赖"表述缺乏 optional 限定词 | OOD L306 vs L320-322 |
| 2.1 | 事实错误 | 低 | `@phase0-mock-field` 在输入/输出 DTO 用途未区分说明 | OOD L628-632 vs L902-906 |
| 3.1 | 风险提示 | 低 | springdoc-openapi v2.5.0 兼容性未显式验证 | OOD L201 |
| 4.2 | 逻辑错误 | 低 | ai-api 依赖已声明但 Phase 0 无消费方注入策略未明确 | OOD L309 |
| 5.1 | 设计争议 | 中 | AI 降级框架在 Phase 0 过度设计（提前就位 vs 最小化骨架） | OOD L634-666 |
| 5.2 | 设计争议 | 低 | `deleted` 用基本类型 boolean 的业务争议 | OOD L528 |
| 5.3 | 设计争议 | 低 | GlobalExceptionHandler 下沉 common 模块（设计偏向选择，非逻辑错误） | OOD L66/L306/L320/L322 |
| 6.1 | 偏离路线图 | 中 | 协作规范（分支/Commit/PR/CR）——OOD 未规划该关联交付物创建（项目级交付物，非 OOD 设计文档遗漏） | Roadmap Phase 0.2 |
| 6.2 | 偏离路线图 | 中 | 新人入门引导文档——OOD 未规划该关联交付物创建（项目级交付物，非 OOD 设计文档遗漏） | Roadmap Phase 0 验收标准 |
| 6.3 | 偏离路线图 | 中 | Phase 0.4 边界：AiService 方法签名冻结未充分论证边界合规性 | Roadmap Phase 0.4 & OOD §8.2 |
| 6.4 | 偏离路线图 | 中 | 5 项"推荐补齐"项在 OOD 中零提及（2 项属 OOD 应覆盖，3 项属独立文档范畴） | Roadmap Phase 0.2 |
| 6.5 | 偏离路线图 | 低 | 降级框架超出 Roadmap 附录 A"Mock 占位"定位 | Roadmap 附录 A & OOD §3.4 |

---

## 修订说明（v2）

| 质询意见 | 回应 |
|---------|------|
| 1. [严重: 高] 2.1 "3.4.x 跨文档编号引用问题" — 误判 | 已删除此条目。按 Roadmap 第 6 行编号约定，"3.4.x" 为 AI 能力编号体系，非跨文档章节引用。 |
| 2. [严重: 中] 2.2 "@phase0-mock-field 语义混淆" — 解读有偏差 | 已修订。改为"该注解在输入/输出 DTO 上用途不同但逻辑自洽，不构成语义冲突"，降低严重度为"低"（事实错误），并建议在文档中补充区分说明。 |
| 3. [严重: 中] 2.3 springdoc-openapi 版本兼容性分类不当 | 已修订。从"事实错误"移至"风险提示"类别，严重度降为"低"。 |
| 4. [严重: 高] 遗漏 Roadmap Phase 0.4 对照检查 | 已新增 6.3 条目。分析 Phase 0.4"模块级接口契约在对应阶段启动前冻结"与 OOD §8.2 冻结 13 项 AiService 方法签名之间的张力，并提出两条改进建议。 |
| 5. [严重: 中] 未评估诊断建议的可操作性和副作用 | 已在"七、高优先级修复建议及副作用分析"中为建议 3（GlobalExceptionHandler 位置调整）和建议 4（AI 降级框架裁剪）补充了完整的副作用分析和前置条件说明。 |
| 6. [严重: 低] 1.1 将不同维度的名称混为一谈 | 已修订。将描述从"应用名不一致"改为"Maven 构建显示名与运行时应用名未对齐，可考虑统一以提高可识别性"。 |
| 7. 深度与完整性补充：Roadmap 附录 A 对比 | 已新增 6.5 条目。对比 Roadmap 附录 A"全部（Mock 占位）"与 OOD 实际设计范围，结论基本一致但降级框架超出 Mock 占位定位。 |
| 7. 深度与完整性补充：3.2 评估尺度偏严 | 已从"逻辑错误"（严重度中）降为"设计争议"（严重度中），分析中补充了"提前设计降级骨架的工程合理性"视角。 |
| 7. 深度与完整性补充：3.1 建议的副作用分析缺失 | 已为 GlobalExceptionHandler 移出建议补充 Jackson 序列化依赖拆分分析。 |
| 7. 深度与完整性补充：4.3 推荐补齐项缺少优先级判断 | 已在 6.4 条目中为每项缺失的推荐补齐项标注了建议优先级（高/中/低）。 |

---

*本报告为第二轮诊断产出，基于 v1 诊断报告（a_v1_diag_v1.md）及审查意见（b_v1_diag_v1.md）修订而成。*

---

## 修订说明（v3）

| 质询意见 | 回应 |
|---------|------|
| 1. [严重: 中] 4.1 "GlobalExceptionHandler 下沉导致 common 耦合 Web 框架" 分类不当，与5.1降级逻辑不一致 | 已修订。将 4.1 从"逻辑错误"（中）降级为"设计争议"（低），移至 §5.3。分析中显式说明了 OOD 第 320-322 行 `optional=true` 的依赖传播策略及其合理性，并指出此为 Spring Boot 多模块常见实践。与 5.1（AI 降级框架）的降级处理保持一致。 |
| 2. [严重: 中] 6.1/6.2 将项目级交付物缺失等同于 OOD "偏离路线图"，未讨论 OOD 的设计边界 | 已修订。将 6.1 和 6.2 的严重度从"高"降为"中"，问题描述从"OOD 未覆盖"修订为"OOD 未规划这些项目级交付物的创建"。分析中增加了边界辨析，明确协作规范（CONTRIBUTING.md）和新人引导文档（QUICKSTART.md）属于项目级交付物，而非 OOD 架构设计文档的固有职责范围。建议在 OOD 中新增"关联交付物清单"章节。 |
| 3. [严重: 低] 6.4 未区分"推荐补齐"项的性质差异 | 已修订。在 6.4 条目的对比表中新增"性质归属"列，区分：日志格式规范和基础监控埋点属 OOD 应覆盖；Docker 配置、lint 工具配置、硬件接口占位属独立文档范畴。 |
| 4. [严重: 低] 建议 4 方案 A 遗漏 AiService 注入策略变更分析 | 已补充。在建议 4 方案 A 的副作用分析中新增"AiService 注入策略变更"段落，分析移除 FallbackAiService 后 application 模块注入点从"MockAiService/FallbackAiService 双层注入"简化为"仅 MockAiService 直接注入"的变更，评估对 OOD 第 636-638 行 Bean 装配策略和 OOD 第 855 行设计决策的影响面积（删除约 5-8 行注入相关配置）。 |
| 5. [严重: 低] 6.3 建议 2 与 OOD 的冻结立场矛盾 | 已修订。将建议 2 从"标记方法签名可调整直至首次落地阶段"修改为：标注"可调整"的方法签名需在 Javadoc 中显式声明调整规则，且任何调整必须走接口变更评审流程。保留了 Phase 0 的冻结立场，同时为远期能力预留了显式、可控的弹性通道。 |
