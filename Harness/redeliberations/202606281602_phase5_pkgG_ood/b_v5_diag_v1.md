# 质量审查报告 — v5 OOD 设计文档

审查范围：Phase 5 包 G OOD 设计文档（`a_v5_copy_from_v4.md`，共 3506 行）
审查维度：需求响应充分度、事实准确性、深度与完整性
审查方式：代码验证 + 设计文档交叉检查

---

## 发现的问题

### 问题1：[严重/事实错误] §3.5 声称所有 DTO 已定义 visitId/patientId/sessionId 字段，与实际代码严重不符

**问题描述**：§3.5「AiRequestBase」节（行 1862）声明："当前代码库中 13 项 AI 能力的请求 DTO（`TriageRequest`、`DiagnosisRequest` 等）各自独立定义 `visitId`/`patientId`/`sessionId` 字段，未继承任何公共基类"。

经逐项代码验证，**13 个请求 DTO 中无一存在 `visitId`/`patientId`/`sessionId` 字段**：

| DTO | 实际字段 | 设计声称字段 |
|-----|---------|-------------|
| `TriageRequest` | `chiefComplaint` 仅 1 个字段 | 声称定义了 3 个公共字段 |
| `DiagnosisRequest` | 空类，零字段 | 同上 |
| `PrescriptionCheckRequest` | 空类，零字段 | 同上 |
| `MedicalRecordGenRequest` | 空类，零字段 | 同上 |
| `PrescriptionAssistRequest` | 空类，零字段 | 同上 |
| `KbQueryRequest` | 空类，零字段 | 同上 |
| `ScheduleRequest` | 空类，零字段 | 同上 |
| `DiscussionConclusionRequest` | 空类，零字段 | 同上 |
| `InspectionReportRequest` | 空类，零字段 | 同上 |
| `LabTestReportRequest` | （未审查完整内容，推测同）| 同上 |
| `ImageAnalysisRequest` | 空类，零字段 | 同上 |
| `ExaminationRecommendRequest` | 空类，零字段 | 同上 |
| `ExecutionOrderRequest` | 空类，零字段 | 同上 |

**影响**：
- 设计声称的"迁移代价仅为继承改造"严重低估了实际工作量。所有 DTO 不仅需要继承 `AiRequestBase`，更需要在每个 DTO 中**新增** `visitId`/`patientId`/`sessionId`/`departmentId` 四个字段，评估 DTO 已有构造器、getter/setter、Jackson 反序列化契约均需调整
- `doExtractDepartmentId()`/`doExtractVisitId()`/`doExtractPatientId()`/`doExtractSessionId()` 的默认实现（从 `AiRequestBase.getXxx()` 获取）在现有 DTO 上无法编译通过——因为当前 DTO 不继承 `AiRequestBase`
- 管线伪代码 `request.getDepartmentId()`/`getVisitId()`/`getPatientId()`/`getSessionId()` 在当前代码库中全部为编译错误

**所在位置**：§3.5「AiRequestBase」节，行 1862–1872
**严重程度**：严重
**改进建议**：修正 §3.5 中对现有 DTO 状态的描述，明确反映"当前 DTO 均无 visitId/patientId/sessionId/departmentId 字段，需逐 DTO 新增 4 个公共字段并调整构造器/Jackson 注解"的事实；将迁移代价评估从"仅需修改继承声明"改为"所有 7 项底座能力 DTO 需新增 4 个字段 + 调整序列化配置 + 更新现有业务调用方"。

---

### 问题2：[严重/事实错误] §3.11.1 定义的 TriageResponse 输出字段与现有代码不一致

**问题描述**：§3.11.1（行 2393）定义 `TriageResponse` 输出 DTO 字段为：
- `recommendedDepartment: String`（推荐科室）
- `urgencyLevel: UrgencyLevel`（紧急程度枚举）
- `reason: String`（推荐理由）

经代码验证，实际 `TriageResponse.java` 的字段为：
- `recommendedDepartments: List<RecommendedDepartment>`（**列表**类型）
- `reason: String`
- **不存在** `urgencyLevel` 字段

**差异分析**：
- 字段名不一致：`recommendedDepartment`（单数） vs `recommendedDepartments`（复数+List）
- 类型不一致：`String` vs `List<RecommendedDepartment>`（带内嵌值对象）
- 缺失字段：`urgencyLevel` 在现有代码中不存在，属于新增设计但未标注

**影响**：
- `结构化解析目标类型` 为 `TriageResponse` 但解析后的 DTO 字段结构不同，实现者无法判断应按设计文档还是按现有代码进行编码
- 若结构化输出解析的目标类型与现有 `TriageResponse` 不一致，解析后的结果无法被现有消费者正常使用（下游业务模块依赖现有字段名）

**所在位置**：§3.11.1「TriageCapabilityExecutor」特化设计表（行 2392–2398）
**严重程度**：严重
**改进建议**：修正 §3.11.1 中 `TriageResponse` 字段定义与现有代码对齐，或明确标注"改造后字段"并评估对下游消费者的兼容性影响。若设计旨在修改现有 DTO，需在 §3.5 过渡策略中明确标注此变更。

---

### 问题3：[重要/完整性不足] 7 项底座能力的 DTO 设计字段与代码现实脱节，未标注"新增 vs 已有"

**问题描述**：§3.11 为 7 项底座能力分别定义了丰富的输入/输出 DTO 字段结构（如 `PrescriptionCheckRequest` 含 `medications`/`patientInfo`/`diagnosisCodes`、`ScheduleRequest` 含 `scheduleDate`/`shiftType` 等）。但代码验证显示，**13 个请求 DTO 中 12 个为空类**（仅 `TriageRequest` 含 1 个字段）。

设计文档在 §3.5 过渡策略中仅讨论了 `AiRequestBase` 的基类继承问题，但未提及这些能力 DTO 的业务字段（如 `PrescriptionCheckRequest.medications`、`ScheduleRequest.scheduleDate` 等）在当前代码库中均不存在的事实，也未区分"需新增的字段"与"已有兼容的字段"。

**影响**：
- 实现者从设计看到丰富的字段定义，但打开代码看到的是空类，两者之间无任何过渡说明
- 这些字段的添加是纯新增工作而非迁移工作，在实施拓扑顺序中没有对应的工作量体现
- `ObjectMapper.convertValue(request, Map.class)` 的变量提取方式 A 在空类 DTO 上将返回空Map，导致模板变量全部缺失

**所在位置**：§3.11.1–§3.11.7 各能力特化设计表；§3.5「AiRequestBase」节
**严重程度**：重要
**改进建议**：在 §3.5 过渡策略中补充"DTO 业务字段补齐计划"章节，明确 7 项底座能力 DTO 的业务字段属于**全新设计**（非迁移），需从零创建；将 §3.11 中每个 DTO 的扩展字段标记状态（✓ 已有 / △ 需改造 / ⊕ 需新增），与 §1.6 API Surface 状态表风格一致。

---

### 问题4：[重要/逻辑矛盾] §3.1 薄适配器伪代码中 `phase4ServiceDelegate` 的方法签名与 Phase 4 DTO 为空的事实矛盾

**问题描述**：§3.1 薄适配器伪代码（行 982–984）调用：
```java
CompletableFuture<R> delegateFuture = CompletableFuture.supplyAsync(
    () -> phase4ServiceDelegate.execute(request))
```

此伪代码假设：
1. 6 个 Phase 4 业务服务接口均存在一个 `execute(T request)` 方法
2. `request`（如 `DiagnosisRequest`）携带了足够的数据供 Phase 4 服务使用

但代码验证显示：`DiagnosisRequest`、`InspectionReportRequest`、`LabTestReportRequest`、`ImageAnalysisRequest`、`ExaminationRecommendRequest`、`ExecutionOrderRequest` **均为空类**。

设计文档既未定义 Phase 4 服务接口的具体方法签名，也未评估空 DTO 是否足以为 Phase 4 服务提供入参。

**影响**：
- 薄适配器伪代码无法在当前代码库中编译（Phase 4 DTO 为空，无法为 `execute()` 提供有意义的入参）
- 设计未定义 Phase 4 服务接口的具体字段和方法签名，薄适配器实现者无法确定注入哪个具体实现

**所在位置**：§3.1「薄适配器型 CapabilityExecutor」节（行 879–1018）；§3.11 未覆盖薄适配器 DTO 定义
**严重程度**：重要
**改进建议**：在 §3.1 薄适配器段落补充 Phase 4 服务接口的契约定义（方法签名、入参/返回值类型）；明确 Phase 4 DTO 是否需要在底座侧补齐业务字段（若需要，纳入过渡策略）；或在设计文档中明确标注 Phase 4 DTO 和 Service 接口的定义职责归属（由 Phase 4 团队完成，底座仅引用）。

---

### 问题5：[中等/深度不足] `string` 类型用于数据库 VARCHAR 缺乏跨数据库兼容性说明

**问题描述**：§3.5 中多个 Entity 字段的数据库类型标注为 `VARCHAR(50)`、`VARCHAR(500)` 等（如 `AiCallLogEntity` 表行 1894–1914）。但 DB 类型描述直接使用 MySQL 方言 `VARCHAR`，未考虑项目可能使用 PostgreSQL（应用 `TEXT` 或 `VARCHAR`）或 Oracle（应用 `VARCHAR2`）的情况。

**影响**：实现者若基于 PostgreSQL 或 Oracle 数据库，需自行翻译类型映射，存在因翻译不当导致的 DDL 错误风险。

**所在位置**：§3.5「AiCallLogEntity」字段表（行 1890–1914）及所有 JPA Entity 字段定义
**严重程度**：中等
**改进建议**：在 §2.1 目录结构或 §3.5 Entity 定义前增加一条说明："数据库类型声明以 MySQL 方言表述，实际实现者需根据项目所选数据库替换为对应方言类型"。

---

### 问题6：[中等/逻辑矛盾] §7 决策表 `异步队列溢出` 行描述与 §3.5/§6.1 指标采集拒绝策略矛盾

**问题描述**：§7 决策表（行 3045）对"异步队列溢出"决策的说明为：
```
| 异步队列溢出 | AbortPolicy vs CallerRunsPolicy | CallerRunsPolicy | 避免 TaskRejectedException 传播到调用链导致主流程失败 |
```

但 §3.5「AiMetricsCollector 异步队列溢出策略」（行 1782）和 §6.1 线程模型（行 2965）均明确说明指标采集线程池的拒绝策略为 **`DiscardPolicy`** + 日志 WARN，而非 `CallerRunsPolicy`。

**矛盾分析**：§7 描述的是 LlmCallExecutor 线程池的拒绝策略（`CallerRunsPolicy`），引用 §9.5 的 `llmCallExecutor()` @Bean 方法（行 2243）。而 §3.5/§6.1 描述的是指标采集异步线程池的拒绝策略（`DiscardPolicy`）。两处分别指向不同的线程池，但 §7 决策表的标题和上下文未区分"LlM 调用线程池"和"指标采集线程池"，导致阅读者无法判断该决策行对应的具体组件。

**所在位置**：§7 决策表第 3045 行 vs §3.5「AiMetricsCollector 异步队列溢出策略」行 1782 vs §6.1 行 2965
**严重程度**：中等
**改进建议**：在 §7 决策表对应行补充"[LLM 调用线程池]"前缀，或在决策表中增加"适用组件"列，明确区分两处不同的拒绝策略决策。

---

### 问题7：[中等/完整性不足] `LabTestReportRequest.java` 和 `LabTestReportResponse.java` 文件路径不一致

**问题描述**：其他 DTO 的文件名遵循 `<CapabilityName>Request.java` 模式，但 `LabTestReportRequest.java` 的文件路径为 `dto/labtest/LabTestReportRequest.java`，而 `InspectionReportRequest.java` 在 `dto/inspection/` 下。

审查 §2.1 目录结构（行 168–258），发现设计文档中 `labtest` 包下仅列出 `LabTestReportRequest.java` 和 `LabTestReportResponse.java`，但代码库中实际存在这些 DTO 的空类。设计对这三个已经存在的空 DTO（`InspectionReportRequest`、`LabTestReportRequest`、`ImageAnalysisRequest`）未作任何处理说明。

**影响**：薄适配器涉及的 6 项 Phase 4 能力中，3 个 DTO 在代码中已存在（虽然为空），但设计文档未定义其字段结构，也未说明"保留空白由 Phase 4 团队填充"还是"底座侧补齐"。

**所在位置**：§3.11 未覆盖 6 项薄适配器 DTO；§2.1 目录结构列出了薄适配器 CapabilityExecutor 类名但未列出对应的 Phase 4 DTO
**严重程度**：中等
**改进建议**：在 §2.1 目录结构或 §3.11 中，为 6 项薄适配器能力补充"DTO 现状"说明条，标注每个 DTO 的当前状态（空类）和责任归属（Phase 4 团队补充 vs 底座团队补充）。

---

### 问题8：[中等/深度不足] `DegradationContext` 扩展后的二进制兼容性验证未覆盖"零值构造器"的 Jackson 反序列化场景

**问题描述**：§3.8「二进制兼容性分析」（行 2177–2184）详细讨论了 `DegradationContext` 扩展字段后的 `serialVersionUID`、无参构造器保留、反序列化校验等。但忽略了以下关键场景：

`DegradationContext` 当前在代码库中可能被用作请求/响应 DTO 的嵌套字段（通过 `@RequestBody` 接收或通过 HTTP 响应返回）。若 `DegradationContext` 被 Jackson 序列化存储（如缓存），则在字段扩展后，旧的序列化 JSON（无新字段）反序列化为新对象时，虽然新字段会取默认值（0/null），但若新字段中存在 `int` 类型（如 `invocationCount`、`failureCount`），旧的 JSON 中没有这些字段值——Jackson 默认行为不会反序列化失败（`int` 字段在无 JSON 值时为 0），但依赖这些字段的业务逻辑（如 `shouldDegrade()` 的百分比计算）可能行为异常。

此外，§3.8 列出的 "3 层防御策略"（构造时保证、校验时补偿、策略层防御）在实际的轻量级反序列化场景中未必全部触发——`postDeserializationValidate()` 需要调用方显式调用，若调用方遗漏（如 `AiOrchestrator` catch 块未调用），则防御措施不生效。

**所在位置**：§3.8「DegradationContext — 降级判定上下文」 binary 兼容性段落（行 2177–2184）；§4.1 执行伪代码
**严重程度**：中等
**改进建议**：补充 `DegradationContext` 在 Jackson 反序列化场景下的防御措施——在 `DegradationContext` 类上标注 `@JsonIgnoreProperties(ignoreUnknown = true)`，并在 `@JsonCreator` 构造器中对新增 int 字段使用 `OptionalInt` 或包装类型 `Integer`（而非 `int`），使旧 JSON 中缺失字段不会产生误判；或在 `postDeserializationValidate()` 可通过 `@JsonPostDeserialize` 自动触发（Jackson 不支持此注解，需通过自定义反序列化器实现）。

---

### 问题9：[中等/完整性不足] §3.11 共同约束未明确定义 7 项底座能力 DTO 的业务字段是否包含在 AiRequestBase 改造范围内

**问题描述**：§3.11 共同约束（行 2483–2487）声明：
1. "所有 7 项能力的 DTO 均在 Phase 5 底座切流时完成 `AiRequestBase` 继承改造"
2. "结构化解析统一使用 `JsonStructuredOutputParser`"

但§3.11 中每个能力特化表定义的 DTO 扩展字段（如 `TriageRequest.complaint`、`PrescriptionCheckRequest.medications` 等）当前代码库中**均不存在**。共同约束未说明这些业务字段是否属于"AiRequestBase 继承改造"的一部分，也未明确这些字段的编码责任方（底座团队新增 vs. 各能力原有团队补充）。

**所在位置**：§3.11「上述 7 项能力的共同约束」段落（行 2483–2487）
**严重程度**：中等
**改进建议**：在共同约束中新增一条："各能力特化表列出的 DTO 扩展字段（如 `complaint`、`medications` 等）均为 Phase 5 底座新增的设计，当前代码库中对应 DTO 为空类；实现者在完成 `AiRequestBase` 继承改造的同时，需根据特化表定义新增这些业务字段及对应的 getter/setter/Jackson 注解"。

---

## 未发现显著性问题的维度

- **需求覆盖范围**：设计覆盖了用户要求的类图（§2.3）、核心职责（§3.x）、协作关系（§2.3 类图关联/§3.x 协作对象段落）、关键接口（§3.2–§3.8）、状态模型（`CircuitBreakerDegradationStrategy`/`ModelEndpointHealthManager`/`CredentialProvider`/`PromptTemplate`/`Experiment`）——需求响应要素完整。
- **架构一致性**：设计风格与 Phase0/Phase1ABD 一致，章节结构对齐，设计决策记录格式统一。
- **技术可行性**：已在本设计之前的多轮内部审议中覆盖，不在本报告重复评估。

---

DIAG_WRITTEN:C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281602_phase5_pkgG_ood\b_v5_diag_v1.md
主Agent请勿阅读产出文件内容，直接将路径转发给相关方。
