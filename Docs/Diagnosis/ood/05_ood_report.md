# OOD Phase 0 诊断报告（v8）

> 诊断对象：`Docs/04_ood_phase0.md`
> 参考基线：`Docs/03_roadmap.md`（路线图）、`Docs/02_tech.md`（技术方案）、`Docs/01_requirement.md`（需求）
> 实际代码状态：项目尚在设计阶段，仓库无后端/前端脚手架代码
> 诊断类别：定义矛盾、事实错误、逻辑错误、设计争议、偏离路线图、风险提示、观察项

---

## 一、事实错误

### 1.1 `@phase0-mock-field` 在输入/输出 DTO 上用途不同

第902-906行定义了两层冻结策略，"本阶段冻结"包含 DTO 类名和归属关系；"延后冻结"包含除 Mock 演示子集外的字段结构。第628-632行描述的 Mock 数据占位规则限定："Phase 0 仅对 8.2 节标记的 `@phase0-mock-field` 生效"。

该注解在输出 DTO 上作 Mock 数据填充标记使用，在输入 DTO 上作"纳入 Phase 0 契约"标记使用（如 `TriageRequest.chiefComplaint` 标注 `@phase0-mock-field` 表示该字段在 Phase 0 冻结，而非要求 MockAiService 填充输入字段）。两种用途逻辑自洽，不构成语义冲突。但 OOD 未在文档中显式区分该注解在输入/输出 DTO 上的不同含义，读者需要跨节对照才能理解，存在阅读成本。

建议在 `@phase0-mock-field` 的定义位置（第906行附近）补充一条说明，澄清该注解在输入 DTO 上表示"契约冻结"、在输出 DTO 上表示"Mock 数据填充"。

- **位置**：`Docs/04_ood_phase0.md` 第 628-632 行 vs 第 902-906 行

### 1.2 版本声明系统性审查

用户需求第 3 项要求"检查是否存在事实错误（如版本号、技术规范等）"。以下对 OOD 中全部版本声明进行系统性核查：

| 声明位置 | 声明内容 | 结论 | 说明 |
|---------|---------|------|------|
| L41 | JDK 17+（最低要求） | ✓ 正确 | Spring Boot 3.3.0 最低要求 JDK 17，与 Spring Boot 官方文档一致 |
| L42 | Node.js 18+ 或 20 LTS | ✓ 正确 | Vue 3 + Vite 的最低 Node 版本要求，推荐版本确认无误 |
| L43 | npm 9+ | ✓ 正确 | npm workspaces 在 npm 7+ 支持，9+ 为合理的最低要求 |
| L137 | Spring Boot 3.3.0（parent） | ✓ 正确 | 2024年5月发布的正式版本，非快照版本；与 JDK 17 兼容 |
| L201 | springdoc-openapi v2.5.0 | ⚠ 已识别为风险（§3.1） | 该版本与 Spring Boot 3.3.x 的兼容性未显式验证，已在 §3.1 中归类为风险提示而非事实错误 |
| L207 | H2 v2.2.224 | ✓ 正确 | H2 v2.2.224（2024年3月发布）与 Spring Boot 3.3.0（2024年5月）版本发布时间接近，Spring Boot 3.3.x BOM 管理 H2 v2.2.224，兼容性由 Spring Boot 官方验证 |
| L929 | snake_case 命名规则（字段级） | ✓ 正确 | 与 JPA DTO JSON 规范一致，无版本编号错误 |
| 其余依赖（spring-boot-starter-web、spring-boot-starter-data-jpa 等） | 版本由 Spring Boot 3.3.0 BOM 管理 | ✓ 正确 | BOM 统一管理的版本无需、也不应在子模块中重复声明；父 POM `<dependencyManagement>` 中不指定 version 是正确实践 |

**审查结论**：OOD 中所有版本声明均为正确或合理范围，不存在版本的硬性事实错误。springdoc-openapi v2.5.0 与 Spring Boot 3.3.0 的兼容性已在 §3.1 以风险提示覆盖，不属于事实错误。

**补充说明**：OOD 前端部分（Vue 3、Vite、TypeScript 等）仅做架构层面描述，无显式版本号声明，无需版本核查。

**版本兼容性联动关系**：由于 Spring Boot 3.3.0 BOM 统一管理了 spring-boot-starter-web、spring-boot-starter-data-jpa、spring-boot-starter-security、spring-boot-starter-validation、spring-boot-starter-test 及 H2、Jackson 等依赖的版本，OOD 通过父 POM 继承 BOM 的做法确保了这些依赖间的版本兼容性。唯一不受 BOM 管理的显式声明版本（springdoc-openapi v2.5.0）已标注为风险项。

- **位置**：`Docs/04_ood_phase0.md` 第 41-43、137、201、207 行

---

## 二、风险提示

### 2.1 springdoc-openapi v2.5.0 兼容性未显式验证

第201行 `<version>2.5.0</version>` 显式声明了 springdoc-openapi 版本。Spring Boot 3.3.0 的 BOM 不管理 springdoc-openapi 版本，此处显式指定版本是合理的。但该版本与 Spring Boot 3.3.x 的兼容性未经验证——springdoc-openapi v2.5.0 可能不支持 Spring Boot 3.3.x 引入的某些变更。

这不是一个已存在的事实错误，而是一个潜在的风险。建议在 Phase 0 集成验证时确认 springdoc-openapi v2.5.0 与 Spring Boot 3.3.0 的实际兼容性，或在文档中显式标注已验证的兼容性状态。

**关联说明**：该兼容性风险对 Knife4j 具有传导效应——Knife4j v4+ 底层依赖 springdoc-openapi 作为 OpenAPI 规范生成引擎。若后续决定补充 Knife4j，需同步验证其底层依赖的 springdoc-openapi 版本是否兼容（参见 §6.4）。

- **位置**：`Docs/04_ood_phase0.md` 第 201 行

---

## 三、逻辑错误

### 3.1 经审查未发现逻辑错误；"3.4.x" 编号引用结论性说明

对照用户需求第 4 条"检查是否存在逻辑错误"，对 `Docs/04_ood_phase0.md` 全文（§1~§10）进行审查，未发现以下类型的逻辑错误：

- **因果颠倒或前提与结论矛盾**：各节的技术推理（如依赖方向、模块拆分策略、AI 降级框架设计决策等）均建立在合理的前提假设之上，不存在从正确前提推导出错误结论的情形
- **自相矛盾的计算或推导**：文档中不包含数值计算或算法推导，不存在算术错误或推导断裂
- **条件与结果不匹配**：各条件化配置（如 `@ConditionalOnProperty` 的互斥策略、`@Profile("phase0")` 的安全放通策略）的条件与预期行为一致，不存在条件为真时应当执行的逻辑路径却无法到达的缺陷
- **不可达逻辑分支**：文档中无包含 if/else 或等效条件分支的技术描述，不存在永真或永假的条件设置

**已查验的关键逻辑链交叉验证**：

1. **CI 分阶段构建策略与模块依赖图一致性验证**：§9（第1104-1108行）CI 第一阶段构建 `common, common-module-api, ai-api`，与 §2（第306-310行）模块依赖图一致——`common-module-api` 和 `ai-api` 均依赖 `common`，第一阶段构建零依赖基础模块符合 Maven reactor 构建顺序要求。第二阶段构建 `common-module-impl、patient、doctor、admin、ai-impl`，其中 `common-module-impl` 依赖第一阶段已安装的 `common-module-api`，`ai-impl` 依赖 `ai-api`，业务模块依赖 `ai-api`——这些依赖在第二阶段均可从本地 Maven 仓库解析第一阶段产物，不存在 reactor 循环依赖或缺失依赖。"

2. **模块依赖方向与 CI 构建顺序的约束关系验证**：§2 第309行规定业务模块（patient/doctor/admin）依赖 `ai-api` 而非 `ai-impl`。§9 CI 第二阶段构建 `ai-impl` 的时间点晚于第二阶段作为整体构建的业务模块——但业务模块的 POM 只声明了对 `ai-api`（第一阶段产物）的依赖，对 `ai-impl` 的依赖仅存在于 application 模块（第三阶段），因此第二阶段构建业务模块时不会因 `ai-impl` 尚未安装而导致解析失败。此约束关系成立。

**历史遗留结论声明**：OOD 第928行使用 "3.4.x" 作为 AI 能力错误码前缀命名规则。此问题曾在 v1 被误判为"跨文档编号引用问题"，经多轮修订后确认不构成逻辑错误或事实错误——OOD 第 928 行的上下文为"3.4.x AI 能力错误码统一采用 `<能力前缀>_AI_<错误类型>` 命名规则"，读者在此上下文中能明确理解"3.4.x"是指 AI 能力需求编号标签，不会与 OOD 自身章节编号混淆。Roadmap 第 6 行约定中的"3.4.x = AI 能力编号"在需求语境中定义清晰，OOD 引用该编号体系作为错误码命名依据是合理的设计引用行为。

**说明**：此前版本（v4 之前）曾将 §4（现 §5.1 信息缺失）和 §5.3（GlobalExceptionHandler 下沉 common 模块，现 §6.3 设计争议）初步归入"逻辑错误"，经多轮修订后重新分类为"信息缺失"和"设计争议"，并在分析正文中逐一说明了降级理由。所有降级路径均有据可查，不存在将逻辑错误掩藏于其他分类下而未声明的条目。

---

## 四、信息缺失

### 4.1 模块间依赖图中 ai-api 被业务模块直接依赖但 Phase 0 无业务消费方

第309行：业务模块（patient/doctor/admin）依赖 `modules/ai/ai-api`。但 Phase 0 业务模块仅有占位 Controller，零 AI 调用。这本身不是错误——依赖在设计阶段建立便于后续扩展。但 OOD 未明确 Phase 0 是否要求业务模块在 POM 中声明 ai-api 依赖并在 Controller 注入 `AiService`（为空注入），还是只要求 POM 依赖就绪而编译期允许未引用。

**倾向性建议**：基于 Phase 0 "最小化骨架"定位，建议 Phase 0 业务模块的占位 Controller **不注入 `AiService`**，仅声明 ai-api POM 依赖（compile scope）。代码中零 `AiService` 引用。Maven 对未使用的 compile scope 依赖不会报错或警告（相比 provided scope 的依赖引用检查），不存在 CI 误报风险。这样在最小化 Phase 0 占位代码量的同时为 Phase 2 业务模块注入 AiService 保留了 POM 依赖就绪状态。

- **位置**：`Docs/04_ood_phase0.md` 第 309 行

---

## 五、设计争议

### 5.1 AI 降级框架在 Phase 0 过度设计（原 3.2，降级；含路线图对照维度）

路线图（`03_roadmap.md`）将 AI Mock 列为"推荐补齐"，明确标注"可跨阶段持续完善，不阻塞 Phase 0 骨架验收"。OOD 在 Phase 0 冻结了完整的降级架构：

**OOD 在 Phase 0 设计的组件**：
- `FallbackAiService`：装饰器模式实现，构造器注入 `List<AiService>` 排除自身后选委托对象
- `DegradationStrategy` 接口 + `DegradationContext` 上下文类
- `NoOpDegradationStrategy`（`@ConditionalOnMissingBean` 条件装配）
- `TimeoutDegradationStrategy`（类声明 + 未来实现占位）
- 完整的三种 `ai.mock.enabled` 配置状态与装配矩阵表（第642-647行）
- `FallbackAiService` 的兜底保护逻辑（`List<AiService>` 为空时的 ERROR/WARN 日志分支）

**分析**：
- 路线图的"可跨阶段持续完善"不禁止结构设计提前就位，OOD 本身也声明 Phase 0 不激活这些组件
- Phase 0 没有任何业务模块调用 AI 能力，这些降级机制在 Phase 0 零执行路径覆盖
- 复杂的条件化装配逻辑（`@ConditionalOnProperty` 互斥条件）增加了 Phase 0 的测试和调试成本
- 但与路线图附录 A 中"全部（Mock 占位）"的定位一致，OOD 的设计属于"提前就位"而非"方向偏离"

此问题应归类为"设计争议"而非"逻辑错误"：提前设计降级骨架有其工程合理性（避免 Phase 2 重构 AiService 接口），但设计深度超出了 Phase 0"最小化骨架"的定位。

- **位置**：`Docs/04_ood_phase0.md` 第 634-666 行

#### §5.1.1 路线图附录 A 对照

Roadmap 附录 A 第 298 行将 Phase 0 的 AI 能力交付描述为"全部（Mock 占位）"。与 OOD 实际设计范围对比：

| 维度 | Roadmap 附录 A 声称 | OOD 实际覆盖 | 是否存在偏离 |
|------|-------------------|-------------|-------------|
| AI 能力范围 | 全部 13 项（Mock 占位） | 13 项 AiService 方法签名均已定义，MockAiService 实现全部 13 个方法 | 一致 |
| 接口契约深度 | Mock 占位 | 方法签名 + DTO 类名 + @phase0-mock-field 子集 + 降级策略框架 + 条件装配矩阵 | OOD 超出 Mock 占位范畴，提前涉及降级框架设计（见 §5.1 主体分析）|
| 使用约束 | 不要求 AI 能力在 Phase 0 真正可用 | MockAiService 返回固定占位数据，正常编译和启动 | 一致 |

OOD 的设计范围整体符合 Roadmap 附录 A 的"Mock 占位"定位，但在降级框架维度存在超范围设计，此为本节主体分析的路线图对照维度。

- **位置**：`Docs/03_roadmap.md` 附录 A 第 298 行 & `Docs/04_ood_phase0.md` §3.4（第599-667行）

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

### 5.4 API 文档工具选型偏离技术栈规范

技术栈文档 `Docs/02_tech.md` §4.4（第114-115行）和 §11.1（第259行）均指定 API 文档工具为 **Knife4j（Swagger3）**，访问路径为 `doc.html`。但 OOD §8.3（第936-953行）使用的却是 **springdoc-openapi-starter-webmvc-ui**，访问路径为 `swagger-ui.html`，两者为不同库。

**分析**：
- Knife4j 是 Swagger/OpenAPI 的增强型 UI 封装层，底层依赖 springdoc-openapi（Spring Boot 3 时代）作为 OpenAPI 规范生成引擎。两者并非互斥关系——Knife4j v4+ 可搭建在 springdoc-openapi v2 之上。
- OOD 当前的设计缺失 Knife4j 依赖声明：父 POM `<dependencyManagement>` 仅声明了 `springdoc-openapi-starter-webmvc-ui`，未声明 `knife4j-openapi3-jakarta-spring-boot-starter`。如果仅按当前 POM 构建，`doc.html` 路径不可访问，仅 `swagger-ui.html` 可用。
- 在 Spring Boot 3 语境下，springdoc-openapi 是正确且必要的底层规范生成引擎；Knife4j 是可选的前端增强层。OOD 选择 springdoc-openapi 本身不是技术错误，但未纳入 Knife4j 构成对技术栈规范的偏离。
- **兼容性关联**：§2.1 所列 springdoc-openapi v2.5.0 与 Spring Boot 3.3.x 的兼容性风险对 Knife4j 具有传导效应——Knife4j v4+ 底层依赖 springdoc-openapi，springdoc-openapi 的兼容性问题将直接映射为 Knife4j 的兼容性问题。因此在决定"是否添加 Knife4j"时，需将 springdoc-openapi 兼容性验证作为前置条件完成。
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
2. 若需对齐技术栈规范，在父 POM 中补充 `knife4j-openapi3-jakarta-spring-boot-starter` 依赖声明，并在配置中切换到 `doc.html` 访问路径；同步验证 §2.1 的 springdoc-openapi 兼容性
3. 若维持 springdoc-openapi 方案，在 OOD §8.3 中更新访问路径描述从 `swagger-ui.html` 为唯一入口，删除 Knife4j 相关提及

- **位置**：`Docs/02_tech.md` §4.4（第114-115行）、§11.1（第259行）vs `Docs/04_ood_phase0.md` §8.3（第936-953行）

---

## 六、观察项

### 6.1 "依赖"表述缺乏限定词（原 §1.1，从"定义矛盾"移入）

第306行首句称 common "依赖 spring-boot-starter-web 及 spring-boot-starter-data-jpa"，同一行后续补充了"两者均在 common 中按 `compile + optional` 使用"——限定信息虽在同行，但以"依赖"开头的表述可能使快速扫读的开发者忽略后半句的 optional 限定，误认为 common 会传递完整 Web/JPA 依赖树给所有下游模块（包括 common-module-api、ai-api 等纯契约模块）。第320-322行对此做了更详细的展开说明。

**分类依据**：第306行与第320-322行的技术事实是一致的（common 确实依赖这两个 starter，只是传播策略不同），不构成"同一概念前后不一致"的定义矛盾。此条目属于"文档表述不精确"——措辞位置靠后，存在被快速扫读者误解的可能。归类为观察项。

- **位置**：`Docs/04_ood_phase0.md` 第 306 行 vs 第 320-322 行

### 6.2 Maven 构建显示名与运行时应用名未对齐

OOD 文档中三个位置使用了不同的项目标识：

| 位置 | 名称 | 文档行号 |
|------|------|---------|
| 父 POM `<name>` | `AIMedicalSys Backend` | 第132行 |
| `spring.application.name` | `aimedical-application` | 第1008行 |
| Monorepo 目录布局顶层目录名 | `aimedical-sys/` | 第52行 |

三者用途不同：Maven `<name>` 仅用于构建报告显示，`spring.application.name` 用于运行期标识（Spring Boot Actuator、日志输出），目录名是文件系统约定。三者不属于"同一概念前后不一致"的定义矛盾范畴——它们在各自语境中有独立的语义和用途。但在同一项目中用三种不同形式标识同一系统，可能增加运维排查时的关联成本（开发者在日志中看到 `aimedical-application` 需手动关联到 Maven 构建产物 `AIMedicalSys Backend`）。可考虑统一以提高可识别性。

- **位置**：`Docs/04_ood_phase0.md` 第 52、132、1008 行

### 6.3 边界论证缺失：方法签名冻结与 Phase 0.4

`03_roadmap.md` Phase 0.4 声明"模块级接口契约冻结（在对应阶段启动前冻结）"——即 AI 模块的接口契约应在 Phase 2（智能分诊首次落地阶段）启动前冻结，而非 Phase 0。但 OOD §8.2 在 Phase 0 冻结了全部 13 项 AI 能力的方法签名（AiService 接口的 13 个方法）。

**OOD 的立场（L874）**：
> "本节在 Phase 0 只冻结方法签名、DTO 类名、包路径和对外命名策略，不冻结除 Mock 演示子集之外的字段级契约"

**分析**：
- Roadmap Phase 0.4 "模块级接口契约冻结"中的"模块级接口契约"可以理解为模块在对应阶段启动时需要锁定的完整接口规范（含字段级契约、校验规则等），而 OOD 仅在 Phase 0 冻结了方法签名层面，字段级契约延后到各能力首次落地阶段——**从这个角度看，OOD 未违反 Phase 0.4 的限制**
- 但 OOD 未在文档中显式论证此边界合规性——读者需自行跨文档对照才能得出"未违反"结论
- **结构耦合约束**：若同时采纳 §8 优先 4（将降级框架推迟至 Phase 2+），则 Phase 0 冻结的 13 个方法签名将成为 Phase 2+ 降级框架设计的默认不可变约束（架构评审例外）。`FallbackAiService` 作为 `AiService` 的实现类，只能以装饰器模式包装已冻结的 13 个方法，不能要求 `AiService` 接口新增任何方法重载或接口变体

**立场关联说明**：上述"默认不可变约束"指 Phase 0 冻结状态下默认不允许修改签名，与弹性变更通道（走接口变更评审流程后允许调整）构成"默认锁定 + 受控例外"的完整策略——约束强度取决于是否启用弹性变更通道。二者不矛盾，分别覆盖默认冻结期和受控调整期两种场景。

**分类依据**：OOD 的方法签名冻结行为经分析未违反 Phase 0.4 限制，但 OOD 缺少显式的边界合规论证。此条目属于"文档论证完备性缺失"——读者需自行跨文档对照才能确认合规性，归类为观察项而非偏离路线图。

**建议**：
1. 在 OOD 中显式论证为何方法签名级冻结不违反 Phase 0.4 边界，明确区分"方法签名冻结"与"字段级契约冻结"两个层级
2. 保留弹性设计考量，但限制如下：
   - 标注"可调整"的方法签名需在 Javadoc 中显式声明"该接口方法在 Phase N（首次落地阶段）启动前可调整，调整必须走接口变更评审流程"
   - 任何此类调整必须由架构评审委员会审批通过，避免被随意修改
   - 等同于保留了 Phase 0 冻结立场，但为远期能力预留了显式、可控的弹性通道

- **位置**：`Docs/03_roadmap.md` Phase 0.4 & `Docs/04_ood_phase0.md` §8.2（第872-932行）

---

## 七、偏离路线图

### 7.1 [中] 协作规范未被 OOD 规划为关联交付物

`03_roadmap.md` Phase 0.2 "骨架必备"第一项要求："协作规范：分支约定、Commit 格式、PR 模板、Code Review 必查项"。OOD 全文未提及任何协作规范。

**边界辨析**：协作规范（分支约定、Commit 格式、PR 模板、CR 必查项）通常归入项目管理文档（CONTRIBUTING.md、PR template 等），不属于 OOD 架构设计文档的固有职责范围。OOD 的核心产出是模块划分、接口契约、依赖方向、关键抽象等设计决策。协作规范属于**项目级交付物**，而非 OOD 设计文档的遗漏。

**问题定位**：OOD 未规划这些项目级交付物的创建。建议在 OOD 中新增"关联交付物清单"章节，列出需并行产出的协作规范文档路径、责任人和预计完成阶段，但不要求在 OOD 正文中包含协作规范的具体内容。

- **位置**：`Docs/03_roadmap.md` Phase 0.2 "骨架必备"第一项
- **状态**：OOD 未规划该关联交付物的创建

### 7.2 [中] 新人入门引导文档未被 OOD 规划为关联交付物

`03_roadmap.md` Phase 0 验收标准要求："新人按入门引导文档可在 1 小时内完成本地环境搭建"。

**边界辨析**：入门引导文档（QUICKSTART.md）属于项目工程文档，通常由 README 或独立文档承载。OOD 第 9 节"本地开发体验"已包含配置管理说明（9.1）、多模块构建说明（9.2）、一键启动命令（9.3）和 Vite 代理配置示例（9.3）等关键内容，为编写引导文档提供了充分的技术素材。

**问题定位**：OOD 未规划新人引导文档（QUICKSTART.md）的创建，现有启动相关内容分散在各节中。建议在 OOD 中新增"关联交付物清单"章节，明确 QUICKSTART.md 由谁在何时创建，而非要求在 OOD 正文中包含完整的分步骤引导内容。

- **位置**：`Docs/03_roadmap.md` Phase 0 验收标准
- **状态**：OOD 未规划该关联交付物的创建

### 7.3 [推荐补齐] 路线图"推荐补齐"项在 OOD 中未覆盖或覆盖不充分

`03_roadmap.md` Phase 0.2 "推荐补齐" 列出的 7 项中，以下 5 项在 OOD 中无任何描述或占位规划：

| 路线图推荐补齐项 | OOD 覆盖状态 | 性质归属 | 建议优先级 |
|----------------|-------------|---------|-----------|
| 日志聚合框架占位（日志输出格式规范与采集配置骨架） | 缺失 | **开发规范/项目文档范畴**——日志输出格式规范属于开发约定/编码规范范畴，与协作规范（7.1）和入门引导文档（7.2）同属项目级交付物 | 中——Phase 1 多模块并行开发前补齐即可 |
| 基础监控埋点接入（关键系统指标的基础埋点预留） | 缺失 | **OOD 应覆盖**——埋点预留属于架构设计范畴 | 中——Phase 1 认证模块上线前补齐即可 |
| 容器化开发部署脚本（Docker Compose / Dockerfile） | 缺失 | **独立文档范畴**——运维基础设施，通常在 DEPLOY.md 或独立配置中定义，不在 OOD 中展开 | 中——有助于统一开发环境，但不是 Phase 0 阻塞项 |
| 本地代码质量检查工具集成（Checkstyle / ESLint / Prettier） | 缺失 | **独立文档范畴**——工具配置文件，通常在独立配置文件中定义；OOD §10 CI 占位已包含构建阶段配置，是代码质量基础设施的一部分 | 高——多人并行开发时自动化代码风格校验可减少 Review 争议 |
| 硬件接入接口占位 | 缺失 | **独立文档范畴**——硬件接入在 Phase 4，Phase 0 可在独立文档中声明"待 Phase 4 定义" | 低——硬件接入在 Phase 4，Phase 0 可在文档中声明"待 Phase 4 定义" |
| API 文档自动生成 | 已覆盖（8.3 节 springdoc-openapi） | — | — |
| AI 能力模块 Mock 占位 | 已覆盖（但降级框架设计偏重，见 5.1） | — | — |

OOD 没有在任何位置声明这些推荐补齐项是否被有意排除，也未说明计划在哪个阶段补齐。建议在 OOD 中新增说明章节，按优先级和性质归属（OOD 应覆盖 vs 独立文档范畴 vs 开发规范/项目文档范畴）标注各项的计划补齐阶段。对于属于独立文档范畴的项（Docker、lint 工具、硬件接口），OOD 仅需引用其创建计划而不必包含具体内容。

- **位置**：`Docs/03_roadmap.md` Phase 0.2 "推荐补齐"
- **状态**：OOD 未覆盖 5/7 推荐补齐项（其中 1 项属 OOD 应覆盖，1 项属开发规范/项目文档范畴，3 项属独立文档范畴）

---

## 八、高优先级修复建议及副作用分析

### 优先级与依赖关系概览

| 优先次序 | 建议内容 | 属性 | 前置条件 | 阻断关系 |
|----------|---------|------|----------|----------|
| 1 | 创建 QUICKSTART.md（原 建议 2） | 验收硬性要求 | OOD §9 内容已就绪，可直接提取 | 无 |
| 2 | 应用名统一（原 建议 5） | 低风险速赢 | 无 | 无 |
| 3 | 新增关联交付物清单章节（原 建议 1） | 关联项目交付 | 需项目负责人确认是否在 OOD 中新增关联交付物清单章节，以及关联交付物的创建时间点和责任归属 | 无 |
| 4 | AI 降级框架裁剪——评估方案 A/B（原 建议 4） | 高影响变更 | 需架构评审决策 | 如果实施方案 A 需重写 OOD §3.4 降级框架内容，并同步更新 8.3 的方法签名冻结边界分析 |
| 5 | GlobalExceptionHandler 迁移（原 建议 3，可选） | 设计优化 | 确认 Jackson 版本由 Spring Boot BOM 管理 | 无——可随时执行 |
| 6 | 基础监控埋点接入（原 建议 6） | 推荐补齐 | 待 §8 决策是否在 Phase 0 还是 Phase 1 补齐 | 无 |

### 优先次序 1（原 建议 2）：【验收硬性要求】创建独立的新人入门引导文档

**内容**：创建 `docs/QUICKSTART.md`，包含前置条件检查（JDK 17+、Node.js 18+、npm 9+）、完整命令序列（git clone → mvn install -DskipTests → npm ci → npm run dev）、验证步骤（`curl http://localhost:8080/api/ping`）、常见问题排查

**前置条件**：OOD 第 9 节内容已就绪，可直接提取

**副作用**：无

**验收属性**：此文档为 Roadmap Phase 0 验收标准"新人按入门引导文档可在 1 小时内完成本地环境搭建"的必要条件，属于硬性验收要求，优先级高于其他推荐性建议。

### 优先次序 2（原 建议 5）：应用名统一

**内容**：选择一个名称并在 `<name>`、`spring.application.name`、目录名三处对齐（建议以 `aimedical-sys` 为基准，与 artifactId 和目录名一致）。具体变更清单：

| 修改位置 | 当前值 | 建议值 | 文件 |
|---------|-------|-------|------|
| 父 POM `<name>` | `AIMedicalSys Backend` | `aimedical-sys` | `backend/pom.xml` L132 |
| `spring.application.name` | `aimedical-application` | `aimedical-sys` | `application/src/main/resources/application.yml` L1008 |
| 目录名 | `aimedical-sys/` | OOD 文档中的目录布局描述已为 `aimedical-sys/`（无需代码层面的目录变更） | Monorepo 根目录 |

**前置条件**：无

**副作用**：无

### 优先次序 3（原 建议 1）：新增关联交付物清单章节（协作规范）

**内容**：在 OOD 中新增"关联交付物清单"章节，列出协作规范（分支约定、Commit 格式、PR 模板、CR 必查项）的创建责任人和完成节点，不要求在 OOD 正文中包含协作规范的具体内容。协作规范属于项目级交付物（CONTRIBUTING.md 等），不应在 OOD 架构设计文档中展开定义。

**前置条件分解**：
- 需项目负责人确认是否在 OOD 中新增关联交付物清单章节（文档层面的决策依赖）
- 关联交付物的具体内容（分支策略、Commit 格式、PR 模板、CR 必查项）可由技术负责人先行制定，与"是否在 OOD 中新增清单章节"这一文档决策可并行推进

**副作用**：无

### 优先次序 4（原 建议 4）：AI 降级框架裁剪方案及副作用分析

**方案 A（推荐）**：将 `FallbackAiService` / `DegradationStrategy` 体系推迟至 Phase 2+，Phase 0 仅保留 `MockAiService` + `AiService` 接口和 DTO 类声明

**权衡说明**：§5.1 已承认降级框架提前设计具有工程合理性（避免 Phase 2 重构 AiService 接口）。本推荐方案 A 基于以下综合权衡：(1) Phase 0「最小化骨架」定位；(2) 降级机制在 Phase 0 零执行路径覆盖；(3) 复杂条件装配的测试/调试成本。若项目方认为工程合理性优先于最小化定位，可保留方案 B（保守保留骨架）。

**副作用分析**：
- Route 表第 642-647 行的六种配置状态需要精简为两种（`ai.mock.enabled=true` → MockAiService；`ai.mock.enabled=false` 或未配置 → 启动期报错提示"Phase 0 不支持关闭 Mock 模式"），第 648 行注脚的"Phase 0 应始终使用 `ai.mock.enabled=true`"需要升级为编译期或启动期强制校验
- **⚠ 级联启动失败风险**：如果 route 表精简未正确实现（或开发者未同步更新配置说明），`ai.mock.enabled=false` 时 Phase 0 不存在真实 AiService 实现，MockAiService 因 `@ConditionalOnProperty(havingValue = "true")` 条件不满足不注册，FallbackAiService 已被移除，Spring 容器中没有任何 AiService 类型的 Bean。此时业务模块的构造器注入将抛出 `NoSuchBeanDefinitionException`，应用无法启动，与 Phase 0"最小化骨架可独立启动"的验收标准直接冲突。**缓解措施**：在 application 模块的配置类中添加启动期断言，硬性校验 `AiService` Bean 是否存在；建议在精简 route 表的同时，将 `ai.mock.enabled=false` 的默认行为改为"配置校验失败 + 明确错误提示"，而非"无声地缺少 Bean"。**实现层参考建议**：建议在 application 模块的 `com.aimedical.config` 包中新增 `AiServiceAvailabilityChecker` 配置类，使用 `@PostConstruct` + `applicationContext.getBeanNamesForType(AiService.class).length == 0` → `throw new IllegalStateException("Phase 0: no AiService bean available — ensure ai.mock.enabled=true")` 实现硬性校验；需同步更新 OOD §9.1 配置说明中增加此校验类的声明，确保 `ai.mock.enabled` 配置说明同步修订，明确标注"Phase 0 禁用 Mock 模式将导致应用启动失败"。

**工作量估算**：`AiServiceAvailabilityChecker` 实现约 20-30 行 Java 代码（配置类骨架 + `@PostConstruct` 校验逻辑 + 异常消息），需配套 1 个单元测试类约 30-50 行测试代码，总计约 50-80 行新增代码。测试可覆盖 `ai.mock.enabled=true`（正常启动）和 `ai.mock.enabled=false`（触发 `IllegalStateException`）两种场景。
- `ai-api` 子模块的 `degradation/` 包路径（含 `DegradationStrategy` 接口和 `DegradationContext`）在 Phase 0 将为空，需保留包路径占位但不包含任何类（或仅保留注释占位文件）
- **AiService 注入策略变更**：当前 OOD 第 855 行的设计决策为"application 模块统一暴露默认 AiService 注入点；FallbackAiService 构造器注入 List<AiService>，排除自身后选定委托对象"。移除 FallbackAiService 后，application 模块的注入点需要简化为直接注册 MockAiService，不再需要装饰器。业务模块代码（`private final AiService aiService;`）本身不需要修改，但注入链路的依赖关系发生变化——从"MockAiService/FallbackAiService 双层注入"变为"仅 MockAiService 直接注入"。OOD 第 636-638 行的 Bean 装配策略需要同步更新：移除 FallbackAiService 的 @Bean 定义，简化 @ConditionalOnProperty 条件为单一 MockAiService 注册。该变更对 application 模块配置代码的影响面积为：删除约 5-8 行 FallbackAiService 注入相关配置，简化 route 表从 3 行缩减为 2 行。
- **Phase 2+ 降级框架与 Phase 0 冻结签名的结构耦合约束**（参见 §6.3 联动分析）：若降级框架推迟至 Phase 2+，则 Phase 0 冻结的 13 个 AiService 方法签名将成为 Phase 2+ 降级框架设计的不可变约束。`FallbackAiService` 作为 `AiService` 的实现类，只能以装饰器模式包装已冻结的方法，降级逻辑不能要求 `AiService` 接口新增任何方法重载或接口变体。OOD 需在 Phase 0 §8.2 中显式标注此约束，确保 Phase 2+ 架构团队知晓接口冻结边界。此约束还意味着 `DegradationContext` 的字段设计必须在 Phase 0 就考虑 Phase 2+ 的实际降级场景需求——因为 `shouldDegrade(DegradationContext context)` 的方法签名在 Phase 2+ 引入时不能对 `AiService` 接口做任何修改。

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

### 优先次序 5（原 建议 3，设计优化）：将 `GlobalExceptionHandler` 迁移至 application 模块

**性质**：此建议为设计优化，非逻辑错误修复。当前 OOD 的方案（将 GlobalExceptionHandler 置于 common，通过 optional=true 控制传播）在功能上自洽且符合常见实践。

**内容**：如果将 `GlobalExceptionHandler` 从 common 移至 application 模块，common 仅持有 `BusinessException` 和 `ErrorCode` 接口

**副作用分析**：
- common 模块去除 `spring-boot-starter-web` 依赖后，`Result<T>` 的 JSON 序列化仍需 Jackson 支持。需要在 common 的 POM 中单独引入 `jackson-databind` 和 `jackson-datatype-jsr310`（与 `spring-boot-starter-web` 自带的 Jackson 版本一致，由 Spring Boot BOM 统一管理）
- application 模块已经依赖 `spring-boot-starter-web`，将 `GlobalExceptionHandler` 移入后无需额外引入依赖
- common 模块的 `config` 包原包含 `GlobalExceptionHandler` 和 `JpaConfig`，移出后 `config` 包仅保留 `JpaConfig` 和 Jackson 配置类

**前置条件**：确认 Jackson 版本由 Spring Boot BOM 管理

### 优先次序 6（原 建议 6）：【推荐补齐】基础监控埋点接入

**内容**：在 OOD §8（模块间 API 通信规范）之后新增或扩展一节，补充基础监控埋点接入方案。该缺失项已在 §7.3 中确认性质归属为"OOD 应覆盖"。

**建议补充位置**：在 §8.4 跨模块调用规范之后新增 §8.5 基础监控埋点接入，或在现有 §10 CI 占位中扩展一节 §10.1 可观测性骨架。

**最小内容**：
- `MeterRegistry` 预留声明：在 common 模块的 `com.aimedical.common.config` 包中声明一个 `MeterRegistry` 配置类占位（或通过 `@Bean` 暴露 `MeterRegistryCustomizer`），确保 Micrometer 核心依赖已在父 POM 中声明
- status 指标路径定义占位：定义关键系统指标（如 `/actuator/metrics/api.request.count`、`/actuator/metrics/api.request.duration`）的命名约定和标签规范
- Micrometer + Spring Boot Actuator 配置骨架：在 `application-dev.yml` 中配置 Actuator 暴露端点（`management.endpoints.web.exposure.include=health,info,metrics`），确保 Phase 0 即可通过 `/actuator/metrics` 查看基础指标

**补充阶段**：归属 Phase 0 推荐补齐——建议在 Phase 0 末期或 Phase 1 首个含实际业务的 OOD 中补齐。优先级中。

**副作用**：无（配置骨架新增不改变现有功能的编译和行为）

---

## 九、诊断结论总览

### 需求维度与报告章节映射

本报告对照用户需求 4 项检查维度，覆盖关系如下：

| 需求维度 | 覆盖章节 | 结论摘要 |
|---------|---------|---------|
| ① 偏离路线图 | §七（偏离路线图） | 发现 3 项偏离/未规划项（§7.1~7.3），含协作规范、入门引导文档、推荐补齐项等 |
| ② 定义矛盾 | §六（观察项）+ 本表末行 | **经审查，OOD 中未发现同一概念前后不一致的定义矛盾**（原疑似条目已移入观察项，见 §6.1/6.2/6.3） |
| ③ 事实错误 | §一（事实错误） | 发现 1 项低严重度事实错误（§1.1）；版本声明系统性审查结论为无硬性事实错误（§1.2） |
| ④ 逻辑错误 | §三（逻辑错误） | 经审查未发现逻辑错误（§3.1），含交叉验证证据和遗留结论声明 |

| # | 类别 | 严重度 | 问题摘要 | 涉及位置 |
|---|------|--------|---------|---------|
| 1.1 | 事实错误 | 低 | `@phase0-mock-field` 在输入/输出 DTO 用途未区分说明 | OOD L628-632 vs L902-906 |
| 1.2 | 事实错误 | 低 | 版本声明系统性审查——未发现硬性事实错误（参见审查汇总） | OOD L41-43/L137/L201/L207 |
| 2.1 | 风险提示 | 低 | springdoc-openapi v2.5.0 兼容性未显式验证；该风险对 Knife4j 具有传导效应 | OOD L201 |
| 3.1 | 逻辑错误 | 低 | 经审查未发现逻辑错误（含历史遗留结论声明：3.4.x 编号引用不构成问题） | OOD §1~§10 |
| 4.1 | 信息缺失 | 低 | ai-api 依赖已声明但 Phase 0 无消费方注入策略未明确（建议：不注入，仅 POM 依赖就绪） | OOD L309 |
| 5.1 | 设计争议 | 中 | AI 降级框架在 Phase 0 过度设计（含路线图对照维度，提前就位 vs 最小化骨架） | OOD L634-666 & Roadmap 附录 A |
| 5.2 | 设计争议 | 低 | `deleted` 用基本类型 boolean 的业务争议 | OOD L528 |
| 5.3 | 设计争议 | 低 | GlobalExceptionHandler 下沉 common 模块（设计偏向选择，非逻辑错误） | OOD L66/L306/L320/L322 |
| 5.4 | 设计争议 | 中 | API 文档工具选型偏离技术栈规范——OOD 使用 springdoc-openapi 而非 Knife4j，未说明替换理由；兼容性风险对 Knife4j 具有传导效应 | OOD L936-953 vs 02_tech.md L114-115/L259 |
| 6.1 | 观察项 | 低 | "依赖"表述缺乏 optional 限定词（从"定义矛盾"移入；属于文档表述不精确） | OOD L306 vs L320-322 |
| 6.2 | 观察项 | 低 | Maven 构建显示名、运行时应用名、目录名三者不一致但非定义矛盾 | OOD L54/L129/L1006 |
| 6.3 | 观察项 | 中 | 边界论证缺失：AiService 方法签名冻结与 Phase 0.4 边界未显式论证合规性；含结构耦合约束——若降级框架推迟至 Phase 2+，冻结签名成为默认不可变约束（架构评审例外） | Roadmap Phase 0.4 & OOD §8.2 |
| 7.1 | 偏离路线图 | 中 | 协作规范（分支/Commit/PR/CR）——OOD 未规划该关联交付物创建（项目级交付物，非 OOD 设计文档遗漏） | Roadmap Phase 0.2 |
| 7.2 | 偏离路线图 | 中 | 新人入门引导文档——OOD 未规划该关联交付物创建（项目级交付物，非 OOD 设计文档遗漏） | Roadmap Phase 0 验收标准 |
| 7.3 | 偏离路线图 | 中 | 5 项"推荐补齐"项在 OOD 中零提及（1 项属 OOD 应覆盖，1 项属开发规范/项目文档范畴，3 项属独立文档范畴） | Roadmap Phase 0.2 |
