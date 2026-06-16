# OOD Phase 0 诊断报告

> 诊断对象：`Docs/04_ood_phase0.md`
> 参考基线：`Docs/03_roadmap.md`（路线图）、`Docs/02_tech.md`（技术方案）、`Docs/01_requirement.md`（需求）
> 实际代码状态：项目尚在设计阶段，仓库无后端/前端脚手架代码
> 诊断类别：定义矛盾、事实错误、逻辑错误、偏离路线图

---

## 一、定义矛盾

### 1.1 应用名不一致

OOD 文档中三个位置使用了不同的应用/项目名称：

| 位置 | 名称 | 文档行号 |
|------|------|---------|
| 父 POM `<name>` | `AIMedicalSys Backend` | 第129行 |
| `spring.application.name` | `aimedical-application` | 第1006行 |
| Monorepo 目录布局顶层目录名 | `aimedical-sys/` | 第54行 |

- **影响**：开发者在 IDE、日志、监控面板中会看到三种不同的应用标识，增加排查关联成本。父 POM name 与 spring application name 不一致时，Spring Boot Actuator 和日志输出使用 application name，与 Maven artifact name 无法对应。
- **位置**：`Docs/04_ood_phase0.md` 第 54、129、1006 行

### 1.2 "依赖"表述缺乏限定词

第306行称 common "依赖 spring-boot-starter-web 及 spring-boot-starter-data-jpa"，第320-322行说明实际以 `compile + optional` 方式引入（不传递下游模块）。

"依赖"二字不带 `optional` 限定，可能误导开发者认为 common 会传递完整 Web/JPA 依赖树给所有下游模块（包括 common-module-api、ai-api 等纯契约模块）。流水线验证人员或新加入的开发者读到第306行时可能错误地添加显式依赖来覆盖 optional 传播。

- **位置**：`Docs/04_ood_phase0.md` 第 306 行 vs 第 320-322 行

---

## 二、事实错误

### 2.1 跨文档编号引用导致 OOD 内无法定位

第928行声明："3.4.x AI 能力错误码统一采用 `<能力前缀>_AI_<错误类型>` 命名规则"。

此处的 "3.4.x" 是需求文档（`Docs/01_requirement.md`）第 3.4 节（AI 能力服务）的子节编号体系。OOD 文档自身没有 3.4.x 编号——OOD 的节编号为 1~10，"3.4 AI 能力模块抽象"是 OOD 自己的第 3.4 节，与需求文档的 3.4.x 编号体系不同。OOD 读者在本文档内无法定位到"3.4.x"所指的段落。

- **位置**：`Docs/04_ood_phase0.md` 第 928 行

### 2.2 `@phase0-mock-field` 的定位不清晰

第902-906 行定义了两层冻结策略，"本阶段冻结"包含 DTO 类名和归属关系；"延后冻结"包含除 Mock 演示子集外的字段结构。但第628-632 行描述的 Mock 数据占位规则（集合字段 1-2 条、字符串 `"mock_" + 字段名`、数值 0/1 等）同时作用于 `TriageRequest.chiefComplaint` 这类**输入 DTO** 字段。输入 DTO 的字段在请求端由调用方填充，MockAiService 不会对输入 DTO 做"Mock 数据填充"。

`@phase0-mock-field` 的语义在第902-906行被描述为"Phase 0 子集冻结"但在第628-632行又被当作 Mock 数据填充标记使用，二者混淆。

- **位置**：`Docs/04_ood_phase0.md` 第 628-632 行 vs 第 902-906 行

### 2.3 springdoc-openapi 版本号非 BOM 管理

第201行 `<version>2.5.0</version>` 显式声明了 springdoc-openapi 版本。Spring Boot 3.3.0 的 BOM 不管理 springdoc-openapi 版本，这里显式指定版本是合理的。但是该版本与 Spring Boot 3.3.x 的兼容性未经验证——springdoc-openapi v2.5.0 可能不支持 Spring Boot 3.3.x 引入的某些变更。

- **位置**：`Docs/04_ood_phase0.md` 第 201 行

---

## 三、逻辑错误

### 3.1 `GlobalExceptionHandler` 下沉到 common 模块导致 Web 框架耦合到零层模块

OOD 将 `GlobalExceptionHandler`（标注 `@ControllerAdvice`）放置在 `common` 模块的 `config` 包下（第66行），导致最底层的共享模块必须依赖 `spring-boot-starter-web`。

**因果链**：
- `GlobalExceptionHandler` 使用 `@ControllerAdvice` → 需要 `spring-webmvc` → 需要 `spring-boot-starter-web`
- common 是其他所有模块的直接或间接依赖 → common 的依赖策略影响全局
- 虽然标记了 `optional=true`，但 common 自身的编译已绑定到 Web 框架

**问题**：
- 如果后续引入非 Web 的消费者（如消息队列监听器、定时任务批处理模块）依赖 common，会在类路径上不必要地引入 Web 容器相关 jar（即使标记 optional，编译期仍会拉取依赖树进行编译校验）
- `GlobalExceptionHandler` 本质上是一个"聚合层关注点"（不同运行环境可能需要不同的异常处理策略），放在 application 模块更合适。common 只需定义 `BusinessException` 和 `ErrorCode` 接口。
- **位置**：`Docs/04_ood_phase0.md` 第 66、306、320 行

### 3.2 AI 降级框架在 Phase 0 过度设计

路线图（`03_roadmap.md`）将 AI Mock 列为"推荐补齐"，明确标注"可跨阶段持续完善，不阻塞 Phase 0 骨架验收"。但 OOD 在 Phase 0 冻结了完整的降级架构：

**OOD 在 Phase 0 设计的组件**：
- `FallbackAiService`：装饰器模式实现，构造器注入 `List<AiService>` 排除自身后选委托对象（自引用排除逻辑）
- `DegradationStrategy` 接口 + `DegradationContext` 上下文类
- `NoOpDegradationStrategy`（`@ConditionalOnMissingBean` 条件装配）
- `TimeoutDegradationStrategy`（类声明 + 未来实现占位）
- 完整的六种 `ai.mock.enabled` 配置状态与装配矩阵表（第642-647行）
- `FallbackAiService` 的兜底保护逻辑（`List<AiService>` 为空时的 ERROR/WARN 日志分支）

**问题**：
- Phase 0 没有任何业务模块调用 AI 能力，这些降级机制在 Phase 0 零执行路径覆盖
- 第648行注脚承认"Phase 0 不存在真实 AiService 实现"且"Phase 0 应始终使用 `ai.mock.enabled=true`"——这意味着 FallbackAiService 和 DegradationStrategy 在 Phase 0 从未进入真实降级判定路径
- 复杂的条件化装配逻辑（`@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "true", matchIfMissing = true)` 与反向条件互斥）增加了 Phase 0 的测试和调试成本
- 与路线图"最小化骨架"定位有张力：被路线图定位为"可选、可跨阶段"的模块，在 OOD 中冻结了比某些"骨架必备"项（如协作规范）更详细的实现设计
- **位置**：`Docs/04_ood_phase0.md` 第 634-666 行

### 3.3 `BaseEntity.deleted` 使用基本类型 `boolean` 而非包装类型 `Boolean`

第528行将 `deleted` 字段定义为 `boolean` 基本类型，默认值 `false`。JPA 实体中使用基本类型 `boolean` 在大部分场景下可以工作，但在以下情况下存在问题：

- **逻辑删除默认值问题**：如果通过原生 SQL 插入（绕过 Hibernate），数据库端需要独立设置 `DEFAULT false`；如果数据库默认值为 `NULL`，Java `boolean` 会收到 `false`（Hibernate 处理），但如果代码有 `@Column(nullable = false)` 约束，原生 SQL 插入 NULL 会直接报数据库错误
- **与 `@SQLRestriction("deleted = false")` 配合**：`@SQLRestriction` 中的 `false` 是 SQL 字面量，与 Java `boolean` 类型无关，这里不构成类型错误

此问题属于设计争议而非硬错误，但建议使用 `Boolean` 包装类型以便区分"未设置"和"未删除"状态。

- **位置**：`Docs/04_ood_phase0.md` 第 528 行

### 3.4 模块间依赖图中 ai-api 被业务模块直接依赖但 Phase 0 无业务消费方

第309行：业务模块（patient/doctor/admin）依赖 `modules/ai/ai-api`。但 Phase 0 业务模块仅有占位 Controller，零 AI 调用。这本身不是错误——依赖在设计阶段建立便于后续扩展。但 OOD 未明确 Phase 0 是否要求业务模块在 POM 中声明 ai-api 依赖并在 Controller 注入 `AiService`（为空注入），还是只要求 POM 依赖就绪而编译期允许未引用。

如果要求 Phase 0 在业务模块 Controller 中注入 `AiService`，则 Controller 需要有 `private final AiService aiService` 字段和构造器注入代码，增加 Phase 0 占位代码量。如果不要求注入则 POM 声明依赖但无使用，Maven 不会报错（相比 `provided` scope 的依赖引用检查），但会给代码审查者留下"未使用依赖"的疑问。

- **位置**：`Docs/04_ood_phase0.md` 第 309 行

---

## 四、偏离路线图

### 4.1 [阻塞] 协作规范缺失

`03_roadmap.md` Phase 0.2 "骨架必备"第一项要求："协作规范：分支约定、Commit 格式、PR 模板、Code Review 必查项"。这是阻塞多人并行开发的必需项。

OOD 全文未提及任何协作规范。对于需要多人并行开发（路线图 Phase 0.3 列出的 8 个并行搭建任务 A~H）的项目，缺乏分支模型（如 Git Flow / Trunk-based）、Commit Message 格式（如 Conventional Commits）、PR 模板和 Code Review 检查清单，会导致以下风险：

- 分支命名冲突、合并策略不一致
- Commit Message 格式不统一，无法自动生成 Changelog
- PR 缺少模板，审查者不知审查重点
- 未见 Code Review 必查项定义，低质量代码可能合入骨架

- **位置**：`Docs/03_roadmap.md` Phase 0.2 "骨架必备"第一项
- **状态**：OOD 未覆盖

### 4.2 [阻塞] 新人入门引导文档缺失

`03_roadmap.md` Phase 0 验收标准要求："新人按入门引导文档可在 1 小时内完成本地环境搭建"。

OOD 第 9 节"本地开发体验"包含：
- 配置管理说明（9.1）
- 多模块构建说明（9.2）
- 一键启动命令（9.3）
- Vite 代理配置示例（9.3）

但 OOD 没有提供**独立的新人引导文档**（如 QUICKSTART.md），也没有在 README.md 中给出完整的分步骤引导。现有内容分散在 OOD 各节中，新人需要阅读全文并手动汇总：
- 缺少环境前置条件检查清单（JDK、Node.js、npm 版本确认命令）
- 缺少 git clone 后的完整命令序列
- 缺少常见问题排查指南（如 Maven 依赖下载失败、端口冲突等）
- 缺少"验证环境搭建成功"的确认步骤

- **位置**：`Docs/03_roadmap.md` Phase 0 验收标准
- **状态**：OOD 仅有启动命令片段，无独立引导文档

### 4.3 [推荐补齐] 以下路线图 "推荐补齐" 项在 OOD 中完全未提及

`03_roadmap.md` Phase 0.2 "推荐补齐" 列出的 7 项中，以下 5 项在 OOD 中无任何描述或占位规划：

| 路线图推荐补齐项 | OOD 覆盖状态 |
|----------------|-------------|
| 日志聚合框架占位（日志输出格式规范与采集配置骨架） | 缺失 |
| 基础监控埋点接入（关键系统指标的基础埋点预留） | 缺失 |
| 容器化开发部署脚本（Docker Compose / Dockerfile） | 缺失 |
| 本地代码质量检查工具集成（Checkstyle / ESLint / Prettier） | 缺失 |
| 硬件接入接口占位 | 缺失 |
| API 文档自动生成 | 已覆盖（8.3 节 springdoc-openapi） |
| AI 能力模块 Mock 占位 | 已覆盖（但过度设计，见 3.2） |

OOD 没有在任何位置声明这些推荐补齐项是否被有意排除，也未说明计划在哪个阶段补齐。

- **位置**：`Docs/03_roadmap.md` Phase 0.2 "推荐补齐"
- **状态**：OOD 未覆盖 5/7 推荐补齐项

---

## 五、诊断结论总览

| # | 类别 | 严重度 | 问题摘要 | 涉及位置 |
|---|------|--------|---------|---------|
| 1.1 | 定义矛盾 | 低 | 应用名三处不一致 | OOD L54/L129/L1006 |
| 1.2 | 定义矛盾 | 低 | "依赖"表述缺乏 optional 限定词 | OOD L306 vs L320-322 |
| 2.1 | 事实错误 | 中 | "3.4.x"跨文档编号在 OOD 内无法定位 | OOD L928 |
| 2.2 | 事实错误 | 低 | `@phase0-mock-field` 语义在输入 DTO 上不适用 | OOD L628-632 vs L902-906 |
| 2.3 | 事实错误 | 低 | springdoc-openapi 版本兼容性未验证 | OOD L201 |
| 3.1 | 逻辑错误 | 中 | GlobalExceptionHandler 下沉导致 common 耦合 Web 框架 | OOD L66/L306/L320 |
| 3.2 | 逻辑错误 | 中 | AI 降级框架在 Phase 0 过度设计 | OOD L634-666 |
| 3.3 | 逻辑错误 | 低 | `deleted` 用基本类型 boolean 的业务争议 | OOD L528 |
| 3.4 | 逻辑错误 | 低 | ai-api 依赖已声明但 Phase 0 无消费方注入策略未明确 | OOD L309 |
| 4.1 | 偏离路线图 | **高** | 协作规范（分支/Commit/PR/CR）缺失——"骨架必备"阻塞项 | Roadmap Phase 0.2 |
| 4.2 | 偏离路线图 | **高** | 新人入门引导文档缺失——验收标准项 | Roadmap Phase 0 验收标准 |
| 4.3 | 偏离路线图 | 中 | 5 项"推荐补齐"项（日志/监控/Docker/lint/硬件）在 OOD 中零提及 | Roadmap Phase 0.2 |

### 高优先级修复建议摘要

修复者需关注以下方向：
1. **4.1 协作规范**：在 OOD 中新增独立章节，定义分支模型、Commit Message 格式（推荐 Conventional Commits）、PR 模板结构、Code Review 必查清单
2. **4.2 入门引导**：创建 `docs/QUICKSTART.md`，包含前置条件检查、完整命令序列、验证步骤和常见问题排查
3. **3.1 GlobalExceptionHandler 位置**：将 `GlobalExceptionHandler` 从 common 移至 application 模块，common 仅持有异常类和 ErrorCode
4. **3.2 AI 降级框架裁剪**：将 FallbackAiService / DegradationStrategy 体系推迟至 Phase 2+，Phase 0 仅保留 MockAiService + AiService 接口和 DTO 类声明
5. **1.1 应用名统一**：选择一个名称并在 `<name>`、`spring.application.name`、目录名三处对齐
