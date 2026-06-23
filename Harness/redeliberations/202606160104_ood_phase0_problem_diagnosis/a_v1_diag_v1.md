# OOD Phase 0 问题诊断报告

> 诊断对象：`Docs/04_ood_phase0.md`（Phase 0 最小化骨架 — 架构级 OOD 设计方案）
> 诊断范围：定义矛盾、事实错误、逻辑错误、偏离需求文档、偏离路线图 Phase 0 范围
> 诊断基线：`Docs/01_requirement.md`（需求规格说明书）、`Docs/03_roadmap.md`（实现路线图）、`Docs/02_tech.md`（技术栈文档）
> 诊断时间：2026-06-16
> 报告版本：v1（首轮诊断）

---

## 1. 概述

`Docs/04_ood_phase0.md` 整体结构清晰、模块划分自洽，Maven 多模块结构与 Vite 工作区布局均与路线图 Phase 0 的"骨架必备"清单对齐。但经逐段对照需求文档 3.4.x 与路线图 Phase 0 范围后，发现以下系统性问题：

- **§8.2 AI 13 项能力 DTO 字段定义与需求文档 3.4.x 严重不符**：13 项能力中有 8 项（3.4.1 / 3.4.2 / 3.4.5 / 3.4.6 / 3.4.7 / 3.4.8 / 3.4.12 / 部分 3.4.4）存在字段缺失或语义偏差，且 OOD §8.2 行 907 自陈"已与需求文档 3.4.4 / 3.4.9 / 3.4.10 / 3.4.13 对齐"，与实际状态不一致。
- **多段定义越过 Phase 0 范围**：权限模型实体（含完整 JPA 关系映射）、跨模块事件模式、ApiClient/AuthStore 等均超出"骨架"粒度，与路线图 §0.4"明确不包含：模块级接口契约冻结"相冲突。
- **若干文档内自相矛盾与术语误用**：DTO 命名风格（camelCase vs snake_case）未声明 JSON 序列化约定、"Vite workspace" 表述不准确、Mock 占位规则存在语义风险等。

本报告按"高/中/低"三级优先级组织问题，每项均给出具体文档位置、问题描述与诊断依据。**报告停留在定位层，不展开修复方案**——修复者在编码时查阅 Hibernate / Spring Data / Springdoc 文档是正常编码活动，不属于"诊断不完整"。

---

## 2. 诊断发现

### 【高优先级】2.1 §8.2 AI 13 项能力 DTO 字段定义与需求文档 3.4.x 多处严重不符

**位置**：`Docs/04_ood_phase0.md` §8.2 "AI 能力方法清单（Phase 0 Mock 占位）"，行 882-1120；§3.4 "Mock 数据占位约定"，行 622-629；§8.2 行 907 自陈对齐范围声明。

**现象**：13 项 AI 能力的输入/输出 DTO 字段定义与需求文档 3.4.x 不匹配。OOD §8.2 行 907 声称"以下骨架已与需求文档 3.4.4 / 3.4.9 / 3.4.10 / 3.4.13 的输入输出契约对齐"，但 §8.2 表中实际列出了全部 13 项的 DTO 字段，且未对齐的能力仍给出字段定义，与声明产生矛盾。

**逐项不符清单**（按需求文档 3.4.x 顺序）：

| 需求条款 | OOD 位置 | 缺失/偏差字段 | 性质 |
|---------|---------|--------------|------|
| **3.4.1 智能分诊输入** | 行 910-913 `TriageRequest` | 缺 `session_id`（**必填**）、`additional_responses`（追问回答数组）、`patient_id`、`rule_version`、`rule_set_id` | 必填字段缺失 |
| **3.4.1 智能分诊输出** | 行 915-918 `TriageResponse` | 缺 `reason`（**必填**，≥1 字符）、`matched_rules`；`recommendedDept`（单数）与需求 `recommended_departments`（数组，0-3 项）语义不符 | 必填字段缺失 + 字段类型偏差 |
| **3.4.2 AI 处方审核输入** | 行 924-941 `PrescriptionCheckRequest` | 缺 `prescription_id`（**必填**） | 必填字段缺失 |
| **3.4.2 AI 处方审核输入`PrescriptionDrug`** | 行 928-932 | 缺 `drug_id`、`route`；字段语义偏差：`dosage`（需求为 `dose`）、`days`（需求为 `duration`） | 必填字段缺失 + 字段语义偏差 |
| **3.4.2 AI 处方审核输出** | 行 949-952 `PrescriptionCheckResponse` | 字段定义偏离需求：OOD `auditResult/riskLevel/warnings` vs 需求 `risk_level/interactions/alerts/suggestions` | 整体字段集不符 |
| **3.4.4 智能诊断`PatientInfo`** | 行 934-941 | 缺 `allergy_history`（string 类型）；OOD 用 `allergies: List<String>` 代替，但需求 3.4.2/3.4.4/3.4.9/3.4.10/3.4.13 均要求 `allergy_history: string`（逗号拼接过敏原名称） | 字段语义偏差 |
| **3.4.5 检查报告输入** | 行 983-987 `InspectionReportRequest` | `rawData: String` vs 需求 `raw_data_ref: String`（语义接近，但缺 `exam_items: array` 可选字段） | 必填字段缺失 |
| **3.4.5 检查报告输出** | 行 986-988 `InspectionReportResponse` | **严重不完整**：仅定义 `reportDraft`，缺 `findings`、`impression`、`auxiliary_interpretation`、`abnormal_items`、`comparison_summary`、`confidence`、`image_recognition` | 7 项核心字段缺失 |
| **3.4.6 检验报告输入** | 行 989-990 `LabTestReportRequest` | `rawData: String` vs 需求 `lab_items: array`（必填，每项含 `item_name/value/unit/reference_range/status`） | 必填结构偏差 |
| **3.4.6 检验报告输出** | 行 992-993 `LabTestReportResponse` | 仅 `reportDraft`，缺 `abnormal_items`（必含 `delta` 字段）、`interpretation`、`suggestions`、`confidence` | 4 项核心字段缺失 |
| **3.4.7 影像分析输入** | 行 995-997 `ImageAnalysisRequest` | `modality: String` 代替 `model_id`（**必填**，由后端根据岗位推断）；需求要求 `model_id` 而非 `modality`（模态） | 必填字段偏差 |
| **3.4.7 影像分析输出** | 行 998-1006 `ImageAnalysisResponse` | **严重不完整**：缺 `model_id`、`recognition_result`（含 `regions/labels/scores`）、`segmentation_mask_ref`、`auxiliary_advice`；OOD 用 `findings: List<Finding>` 代替 `recognition_result` 对象 | 整体结构不符 |
| **3.4.7 `Finding.confidence`** | 行 1005 | 注释 "[0, 1]"，但需求 3.4.7 要求外层 `confidence: 0-100`，且单个发现的置信度无明确范围约束 | 字段语义偏差 |
| **3.4.8 知识库问答输入** | 行 1008-1010 `KbQueryRequest` | 缺 `user_role`（**必填**，enum `PATIENT/DOCTOR`）、`session_id`（**必填**） | 2 项必填字段缺失 |
| **3.4.8 知识库问答输出** | 行 1012-1014 `KbQueryResponse` | 缺 `related_questions`、`disclaimer_required`（固定 true） | 2 项字段缺失 |
| **3.4.9 开立检查/检验输出** | 行 1022-1026 `ExaminationRecommendResponse` | 缺 `disclaimer_required`（固定 true） | 1 项字段缺失 |
| **3.4.12 AI 排班输入** | 行 1083-1087 `ScheduleRequest` | **结构严重偏离**：OOD 平铺 `department/startDate/endDate/doctorIds`，需求要求嵌套 `constraints: {start_date/end_date/department_ids/shift_rules/doctor_quota}` + `doctor_pool: array`（含 doctor_id/doctor_name/department_id/title/available_days/unavailable_days）+ `historical_schedule_ref` | 整体结构不符 |
| **3.4.12 AI 排班输出** | 行 1089-1096 `ScheduleResponse` | 缺 `workload_score`（0-100）、`summary`、`conflicts`；OOD 用 `location`（非需求字段） | 3 项字段缺失 |

**根因定位**：§8.2 行 907 显式声明对齐范围为"3.4.4 / 3.4.9 / 3.4.10 / 3.4.13"4 项，但 §8.2 表实际列出全部 13 项 DTO。3.4.4/3.4.9/3.4.10/3.4.13 的 DTO 主结构（如 `condition: ConditionInfo`、`patient_info: PatientInfo`、`exam_results`）与需求一致，但共享的 `PatientInfo` 类型内部 `allergies` vs 需求 `allergy_history` 字段语义不符（见 2.2）。其余 9 项（3.4.1/3.4.2/3.4.5/3.4.6/3.4.7/3.4.8/3.4.12）的 DTO 字段定义偏离需求。

**触发条件**：
- §8.2 "Phase 0 各 DTO 的核心字段结构定义如下"段落被理解为"全部 13 项的 DTO 字段定义"，但未声明其中 9 项为占位/待对齐；
- §8.2 表中方法清单列出全部 13 项及对应 DTO，混淆"已对齐"与"占位/待对齐"边界。

**影响范围**：
- 后续阶段（Phase 2-5）落地 AI 能力时，需做 DTO 破坏性调整（与 §8.2 自身"避免后续再做 DTO 破坏性调整"声明相矛盾）；
- 前端 `packages/shared/types/`（OOD §8.3 行 1130）若按当前 OOD DTO 同步 TypeScript 类型，将与后端实际 AI 契约不一致；
- `MockAiService` 实现（§3.4 行 603）按当前 DTO 生成占位数据时，缺字段会以 null 填充，与 Mock 占位规则（§3.4 行 627-629）的"无任何可空标记的字段视为必填，按前述规则填充占位值"冲突——即 Mock 会因字段缺失而无法运行。

---

### 【高优先级】2.2 `PatientInfo.allergies` 字段语义与需求文档 5 项 AI 契约不符

**位置**：`Docs/04_ood_phase0.md` §8.2 行 934-941 `PatientInfo` 定义；该类型被 `PrescriptionCheckRequest`、`DiagnosisRequest`、`ExaminationRecommendRequest`、`PrescriptionAssistRequest`、`DiscussionConclusionRequest` 5 个 DTO 共同引用。

**现象**：需求文档 3.4.2 / 3.4.4 / 3.4.9 / 3.4.10 / 3.4.13 共 5 项 AI 能力的输入契约均要求 `patient_info` 字段集为 `{ patient_id, age, gender, allergy_history, comorbidities }`（参见需求 §3.4.2 行 839、§3.4.4 行 895、§3.4.9 行 977、§3.4.10 行 991、§3.4.13 行 1054）。其中 `allergy_history` 为 `string` 类型，由 `allergen` 列表以中文逗号拼接（见需求 §3.1.6 行 390 的映射约定）。OOD §8.2 `PatientInfo` 用 `List<String> allergies` 代替 `string allergy_history`，类型不同。

**根因定位**：§8.2 `PatientInfo` 定义未按需求 3.1.6 行 388-398 的"健康档案数据→AI 契约字段的映射约定"实现。

**触发条件**：
- §8.2 `PatientInfo` 定义时未交叉对照需求 3.4.x 中 `patient_info` 字段集约束；
- §3.4.2/3.4.4/3.4.9/3.4.10/3.4.13 的 OOD DTO 共用同一 `PatientInfo`，但 OOD §8.2 行 907 自陈"已对齐"的承诺与实际 `PatientInfo` 内部结构矛盾。

**影响范围**：
- 5 项 AI 能力的 Mock 实现（`MockAiService`）无法按当前 `PatientInfo` 直接序列化 AI 输入契约，需在调用层做额外字段转换，转换规则未在 OOD 中明示；
- `comorbidities` 字段在 OOD `PatientInfo` 中未标注必填（与需求 3.4.4/3.4.9/3.4.13 中 `comorbidities` 必填的要求不符）。

---

### 【中优先级】2.3 路线图 §0.4"明确不包含"被多项 OOD 定义越界

**位置**：多处，详见下表。

**现象**：路线图 §0.4"本阶段明确不包含"列举：(a) 任何业务功能；(b) **模块级接口契约冻结**（在对应阶段启动前冻结）。但 OOD 多处定义超过"骨架"粒度，进入"接口契约冻结"或"业务实现"范畴：

| OOD 位置 | 定义内容 | 越界性质 |
|---------|---------|---------|
| §3.3 行 529-595 | User、Role、Post、Function 实体的全部字段（含 `userType` 枚举）、JPA 关系映射（`@JoinTable`、`@JoinColumn`、`fetch`、`cascade`、`orphanRemoval`）、关联表命名约定、Fetch/Cascade 策略说明 | 完整 JPA 关系映射 = 模块级接口契约冻结 |
| §3.3 行 587-594 | `DataPermissionEvaluator` 门面接口扩展点、数据范围枚举（`DataScopeType.SELF_OWNED` / `SELF_HANDLED`）、Phase 0 冻结约定 | 数据范围扩展点契约冻结 |
| §8.4 行 1145-1198 | Spring ApplicationEvent 跨模块调用模式二：POJO 事件类（`UserRegisteredEvent`）示例、发布订阅注解（`@EventListener`）、模式选择原则 | 跨模块调用接口契约冻结 |
| §3.5 行 666-681 | `ApiClient` 类（含 JWT 请求拦截器、401 响应跳转登录页、`NETWORK_ERROR` 错误拦截）、`AuthStore` Pinia store（含登录态、token、用户信息） | Phase 0 无登录态，但定义了完整认证基础设施 |
| §8.3 行 1124-1143 | 完整 springdoc-openapi 配置（`springdoc.api-docs.path: /v3/api-docs`、`swagger-ui.path: /swagger-ui.html`）、生产环境关闭策略、与 Phase 0 验收挂钩的描述 | §8.3 自陈"推荐补齐项"，却给出完整生产级配置 |
| §8.3 行 1130 | TypeScript 类型同步机制、Phase 1+ 引入 openapi-generator 的迁移路径 | 类型同步机制冻结 |
| §3.4 行 632-645 | `MockAiService` 装配策略表（`ai.mock.enabled` 三种取值）、FallbackAiService 装饰器排除自身逻辑、`@ConditionalOnProperty` 与 `@ConditionalOnMissingBean` 组合 | AI 装配策略完整冻结 |

**根因定位**：OOD §1.1 行 7-12 的设计目标"骨架可运行""可演进"被理解为"在骨架中预留完整扩展点"，导致扩展点的定义粒度超过路线图 §0.4"骨架必备"清单。

**触发条件**：
- 路线图 §0.4 "明确不包含" 与 OOD §1.1 设计目标"可演进：骨架预留 AI 能力抽象层、权限模型扩展点、微服务化拆分演进路径"之间的边界缺乏量化标准；
- §3.3 末尾（行 590-594）显式声明"Phase 0 先冻结扩展点，不在本阶段提供实现"，与"不冻结接口契约"路线图原则直接冲突。

**影响范围**：
- 后续阶段（Phase 1+）调整这些接口契约时，需对 Phase 0 已交付的骨架代码做破坏性修改，与"骨架可演进"目标部分矛盾；
- Phase 0 工期被这些冻结内容占用，可能挤压"骨架必备"任务（搭建任务 A-H）的工期。

---

### 【中优先级】2.4 §8.2 行 907 对齐范围声明与实际不符

**位置**：`Docs/04_ood_phase0.md` §8.2 行 907。

**现象**：OOD 声明"以下骨架已与需求文档 3.4.4 / 3.4.9 / 3.4.10 / 3.4.13 的输入输出契约对齐，避免后续再做 DTO 破坏性调整"。但：

1. §8.2 表中实际列出全部 13 项 AI 方法的 DTO 字段，未对齐的 9 项仍给出详细字段定义，与"对齐"声明产生混淆。
2. 即使是声明"已对齐"的 4 项（3.4.4 / 3.4.9 / 3.4.10 / 3.4.13），其共享的 `PatientInfo` 内部字段（`allergies` vs 需求 `allergy_history`）仍存在语义偏差（见 2.2）。
3. `ExaminationRecommendResponse`（对应 3.4.9）缺 `disclaimer_required`（需求固定 true，见需求 §3.4.9 行 979）。

**根因定位**：§8.2 行 907 的对齐范围声明基于 AI 能力的方法名核对，未逐字段对照需求 3.4.x 的输入/输出契约字段清单。

**触发条件**：
- OOD §8.2 "Phase 0 各 DTO 的核心字段结构定义如下"段落被理解为"全量 DTO 定义"，但实际范围声明仅覆盖 4 项；
- §8.2 末尾未声明"其余 9 项 DTO 为占位/待对齐"的边界条件。

**影响范围**：
- 读者基于"已对齐"声明信任 §8.2 DTO 字段，但在 Phase 2-5 落地 AI 能力时发现不符，需做破坏性调整；
- 与 §8.2 行 907 自身的"避免后续再做 DTO 破坏性调整"目标自相矛盾。

---

### 【中优先级】2.5 "Vite workspace" 表述不准确

**位置**：`Docs/04_ood_phase0.md` 行 19、44、353、363、372-373、422-423、425、699、853、863、1272。

**现象**：OOD 多处使用"Vite workspace"表述，包括：
- §1.2 行 16-19 "前端为 Vite 多应用单仓"
- §1.4 行 44 `npm | 9+ | 与 Vite workspace 兼容`
- §2.4 行 353、363 "三端各自独立路由""通过 `packages/shared` 共享"
- §2.4 行 422-423 "Vite 通过 workspaces 内置的 node_modules 解析机制自动定位内部包路径"
- §7 行 853 "前端 Monorepo 工具：Nx vs Turborepo vs Vite"
- §7 行 863 "前端共享包结构：独立 npm 包 vs workspace 内部包"
- §10 行 1272 "前端：在各 `apps/*` 目录执行 `npm run dev`"

实际：Vite 本身**不提供 workspace 功能**。workspace 是包管理器（npm/yarn/pnpm）的能力，由 `package.json` 的 `workspaces` 字段驱动。Vite 仅作为构建工具，受益于包管理器通过 node_modules 提供的包解析机制。

**根因定位**：OOD 将"前端构建工具 + workspace 机制"统称为"Vite workspace"，未区分两者职责。

**触发条件**：
- §2.4 行 372-373 的根 `package.json` `workspaces` 字段实际是 npm workspaces 配置（与 Vite 无关）；
- §7 行 853 设计决策表将 Nx / Turborepo / Vite 并列作为 Monorepo 工具选项，暗示 Vite 是 Monorepo 工具。

**影响范围**：
- 实施者可能误解为 Vite 内置 workspace 特性，在配置 `vite.config.ts` 时查找不存在的 `workspace` 选项；
- §7 行 853 的设计决策表将"Vite workspace"作为最终选择，与路线图 §0.4"避免引入额外的工具链依赖"的语义混淆（Vite 本身是构建工具，workspace 是包管理器能力，两者叠加 vs 单一 Turborepo/Nx 的对比标准不清晰）。

---

### 【中优先级】2.6 §3.4 Mock 占位规则的语义风险

**位置**：`Docs/04_ood_phase0.md` §3.4 行 622-629 "Mock 数据占位约定"。

**现象**：§3.4 行 626-627 声明 Mock 占位规则：
- "枚举字段：填充目标枚举类型的第一个枚举值（`EnumType.values()[0]`）"

该规则对具有强语义的枚举字段（如 `audit_result` ∈ {PASS, FLAG, REJECT}、`risk_level` ∈ {LOW, MEDIUM, HIGH}、`severity` ∈ {MILD, MODERATE, SEVERE}、`urgency_hint` ∈ {LOW, MEDIUM, HIGH}）存在语义风险：

- `audit_result.values()[0] = PASS` —— 所有 Mock 返回"审核通过"
- `risk_level.values()[0] = LOW` —— 所有 Mock 返回"低风险"
- `severity.values()[0] = MILD` —— 所有 Mock 返回"轻度"
- `urgency_hint.values()[0] = LOW` —— 所有 Mock 返回"低紧急度"

若枚举定义顺序与"高/严重"语义对齐（如 HIGH 在前），则 Mock 默认填充高风险值，与业务期望相反。OOD §8.2 中 `risk_level`、`audit_result`、`severity` 等枚举字段均出现在 3.4.2/3.4.5/3.4.6/3.4.7/3.4.10 等 AI DTO 中。

**根因定位**：§3.4 Mock 占位规则未区分"无语义枚举"与"有强语义枚举"的填充策略。

**触发条件**：
- 实施者按 OOD 规则实现 `MockAiService`，对所有枚举字段应用 `values()[0]`；
- 前端基于 Mock 返回值开发（如 UI 控件根据 `risk_level` 切换颜色、根据 `audit_result` 切换提示文案），将基于"全部低风险/全部审核通过"的错误数据开发。

**影响范围**：
- 前端 UI 开发基于 Mock 数据，但 Mock 默认填充"通过"语义值，前端会开发出与真实 AI 返回不符的 UI 分支；
- `MockAiService` 与 `FallbackAiService`（§3.4 行 633-645）的降级行为混淆——Mock 永远不返回"不可用"标志，前端无法基于 Mock 验证降级路径的 UI 分支。

---

### 【中优先级】2.7 DTO 字段命名风格（camelCase vs snake_case）未声明 JSON 序列化约定

**位置**：`Docs/04_ood_phase0.md` §8.2 整段（行 882-1120）；§3.4 行 603 提及 `MockAiService`。

**现象**：
- OOD §8.2 中 DTO 字段命名采用 Java camelCase 风格（如 `chiefComplaint`、`patientInfo`、`prescriptionDraft`、`examType`、`encounterId`）。
- 需求文档 3.4.x 中输入/输出契约字段命名采用 snake_case 风格（如 `chief_complaint`、`patient_info`、`prescription_draft`、`exam_type`、`encounter_id`）。
- OOD 全文未声明 JSON 序列化时字段命名转换约定（如 `@JsonProperty`、`@JsonNaming(PropertyNamingStrategies.SNAKE_CASE)`）。

**根因定位**：OOD §8.2 DTO 定义按 Java 实体类风格编写，未声明对外 API 契约的字段命名约定。

**触发条件**：
- Spring Boot 默认使用 Jackson 序列化，字段名直接采用 Java 字段名（即 camelCase），与需求 snake_case 契约不一致；
- 前端 TypeScript 类型定义（§8.3 行 1130 提及 `packages/shared/types/`）若按 OOD 当前 DTO 同步，将得到 camelCase 类型，与后端实际 JSON 字段名（camelCase）一致但与需求契约（snake_case）不一致。

**影响范围**：
- 后端 API 实际 JSON 字段为 camelCase，前端按需求文档 snake_case 期望调用，导致字段映射错误；
- §8.3 行 1130 "Phase 1+ 引入 openapi-generator，通过 springdoc-openapi 生成的 OpenAPI 规范自动生成 TypeScript 类型"——若 Phase 0 已按 OOD 当前 DTO 实现，则生成的 OpenAPI 规范字段为 camelCase，与需求契约 snake_case 不一致，TypeScript 类型仍会漂移。

---

### 【中优先级】2.8 §4.5 SecurityConfigPhase0 中 `requestMatchers("/api/ping").permitAll()` 与 `anyRequest().permitAll()` 冗余

**位置**：`Docs/04_ood_phase0.md` §4.5 行 770-776 `SecurityConfigPhase0` 示例代码。

**现象**：示例代码第一行 `requestMatchers("/api/ping").permitAll()` 被第二行 `anyRequest().permitAll()` 完全覆盖。两行同时存在的语义效果等价于单行 `anyRequest().permitAll()`，但 OOD §4.5 行 770-776 同时给出，暗示 `/api/ping` 端点存在特殊鉴权需求。

**根因定位**：§4.5 示例代码未说明为何单独列出 `/api/ping` 规则。

**触发条件**：
- 实施者按 OOD 示例代码实现 SecurityConfigPhase0，可能误以为 `/api/ping` 未来要单独鉴权；
- §4.5 行 779 "Phase 1 切换方式：将 `spring.profiles.active` 从 `phase0,dev` 调整为 `dev`（或其他不包含 `phase0` 的组合），激活 application 模块中的 Phase 1 SecurityConfig（标注 `@Profile("!phase0")`），即可启用认证规则 `anyRequest().authenticated()`"——Phase 1 的 `.anyRequest().authenticated()` 会覆盖示例中两行 permitAll 规则。

**影响范围**：实施歧义，不影响 Phase 0 功能正确性。

---

### 【中优先级】2.9 §4.5 Phase 1 profile 切换描述冗余

**位置**：`Docs/04_ood_phase0.md` §4.5 行 779。

**现象**：OOD 描述"将 `spring.profiles.active` 从 `phase0,dev` 调整为 `dev`（或其他不包含 `phase0` 的组合），激活 application 模块中的 Phase 1 SecurityConfig"。其中"或其他不包含 `phase0` 的组合"是冗余表述——Phase 1 不需要 `phase0` profile，调整为 `dev` 即可。

**根因定位**：§4.5 profile 切换描述未明确目标 profile 集合。

**触发条件**：
- 实施者可能保留 `phase0` 在 profiles 列表中（如 `phase0,dev,prod`），导致 SecurityConfigPhase0 仍被激活；
- 或在 profile 切换时增加其他 profile，未确认是否会激活 SecurityConfigPhase0。

**影响范围**：Phase 1 切换时的实施歧义，可能导致认证规则未生效。

---

### 【低优先级】2.10 §3.4 FallbackAiService 自引用排除逻辑的隐含假设未明示

**位置**：`Docs/04_ood_phase0.md` §3.4 行 633-645 "FallbackAiService" 说明。

**现象**：§3.4 行 633-634 描述 FallbackAiService 的委托对象选择逻辑："在构造器中排除自身（`! (s instanceof FallbackAiService)`）后选定委托对象"。但：
1. OOD 未说明 FallbackAiService 是否被注册为 `AiService` Bean（即通过 `@Service` 标注、`implements AiService`，同时 Spring 通过类型注入 `List<AiService>` 时会包含 FallbackAiService 自身）；
2. §3.4 行 633 提到"避免 `@ConditionalOnMissingBean` 与 `@Primary` 装饰器的语义冲突"，但 OOD 未显式给出 FallbackAiService 上的 `@Primary` 注解配置；
3. §3.4 行 634 提到"应用启动时，application 模块的配置决定 `ai.mock.enabled` 值"，但 OOD 未明示 FallbackAiService 自身的 `@ConditionalOnProperty` 配置（仅 §3.4 行 632-633 描述 MockAiService 的条件）。

**根因定位**：§3.4 对 FallbackAiService 的 Bean 注册条件、装饰器职责、自引用排除的隐含假设未做完整说明。

**影响范围**：实施者按 OOD 实现 `FallbackAiService` 时，可能遗漏 `@Primary` 或 `@ConditionalOn*` 注解，导致 Bean 装配顺序异常或多个 `AiService` Bean 冲突。

---

### 【低优先级】2.11 §7 设计决策表行 862 与 §3.4 装配策略描述的颗粒度不一致

**位置**：`Docs/04_ood_phase0.md` §7 行 862；§3.4 行 632-645。

**现象**：
- §7 行 862 设计决策表："AI Mock 实现形态：Spring @Profile vs 条件注解 → `@ConditionalOnProperty`"。仅提到 `@ConditionalOnProperty`。
- §3.4 行 632-633 实际装配策略：`MockAiService` 用 `@ConditionalOnProperty`；`NoOpDegradationStrategy` 用 `@ConditionalOnMissingBean(DegradationStrategy.class)`。

§3.4 装配策略同时使用 `@ConditionalOnProperty` 与 `@ConditionalOnMissingBean`，但 §7 设计决策表仅提到前者，未说明两者组合使用的决策依据。

**影响范围**：设计决策溯源不完整，§7 与 §3.4 描述颗粒度差异。

---

### 【低优先级】2.12 §9.1 `application.yml` 完整示例缺失

**位置**：`Docs/04_ood_phase0.md` §9.1 行 1206 "统一配置管理"。

**现象**：OOD 描述"application.yml：存放通用配置及多环境共享配置，设置 `spring.profiles.active: phase0,dev`"，但未给出 `application.yml` 的完整示例。仅给出 `application-dev.yml`（行 1213-1232）与 `application-prod.yml` 的存在提示。

**触发条件**：
- 实施者创建 `application.yml` 时，需自行推断基础配置（如 `spring.application.name`、`server.port=8080` 等）；
- §9.1 行 1290 提到"后端统一监听 `localhost:8080`"，但 `server.port` 配置未在 `application.yml` 或 `application-dev.yml` 中显式给出。

**影响范围**：实施者创建 `application.yml` 时存在配置缺失风险。

---

### 【低优先级】2.13 §7 行 867 "CI 分阶段构建策略" 与 §10 CI 段一致性

**位置**：`Docs/04_ood_phase0.md` §7 行 867；§10 行 1296-1304。

**现象**：§7 行 867 设计决策："CI 分阶段构建策略：`mvn install -DskipTests` 分阶段构建"。理由是 "`mvn compile` 不安装产物到本地仓库，后续阶段依赖解析失败"。

但 §10 行 1296 描述五阶段构建，前三阶段均使用 `mvn install -DskipTests -pl <modules>`；第四阶段用 `mvn verify -pl integration`（含 Failsafe）；第五阶段用 `mvn test` + 前端构建。

§7 设计决策的颗粒度（"mvn install -DskipTests"）未涵盖第四、五阶段的 `mvn verify` 与 `mvn test`，且未说明为何第四阶段从 `install` 切换到 `verify`，第五阶段再切换到 `test`。

**影响范围**：CI 实施时存在阶段切换的隐含逻辑未明示。

---

### 【低优先级】2.14 §9.2 application 模块启动类位置与 `@SpringBootApplication` 注解参数

**位置**：`Docs/04_ood_phase0.md` §9.2 行 1256-1267。

**现象**：§9.2 示例显示 `@SpringBootApplication(scanBasePackages = "com.aimedical")` 等三个注解，但 §2.3 包命名约定（行 346）显示"└── Application # 启动类在 application 模块的根包"。未明示启动类 `Application.java` 的具体包路径（如 `com.aimedical` 或 `com.aimedical.application`）。

若启动类在 `com.aimedical` 包下，则 `scanBasePackages = "com.aimedical"` 与默认扫描行为一致，可省略；若启动类在子包（如 `com.aimedical.application`）下，则 `scanBasePackages` 是必需的。

**影响范围**：实施歧义，不影响 Phase 0 功能正确性。

---

## 3. 跨章节一致性观察（非独立问题，作为修复时的交叉参考）

以下观察不构成独立问题，但与上述多项诊断发现交叉相关，修复时需一并考虑：

1. **§3.4 "Mock 数据占位约定"（行 622-629）与 §8.2 DTO 字段缺失的相互作用**：2.1 中识别的多处 DTO 字段缺失（如 `PrescriptionCheckRequest.prescription_id`、`InspectionReportResponse.impression` 等）会导致 Mock 实现无法按当前 DTO 结构生成完整占位数据。Mock 规则行 627 要求"无任何可空标记的字段视为必填"，但缺字段的 DTO 在序列化时为 null，与 Mock 规则矛盾。

2. **§2.2 模块依赖图（行 272-289）与 §3.3 权限模型的位置声明**：§3.3 行 533 "所有权限模型实体（User、Role、Post、Function）归属 `common-module-impl` 子模块"，与 §2.2 依赖图中"`common-module-impl` 仅由 application 引入"一致。但 §3.3 详细定义的实体 JPA 映射（cascade、fetch、orphanRemoval）属于路线图 0.4 "模块级接口契约冻结"范畴（见 2.3）。

3. **§8.2 DTO 与 §3.4 AiService 装配策略的耦合**：§3.4 行 603 描述 `MockAiService` "实现该接口的全部 13 个方法"，§8.2 行 882 "Phase 0 定义 `AiService` 接口中包含以下 13 个方法契约"。两者对方法数量的描述一致。但 §3.4 行 624-629 的 Mock 占位规则（特别是 `values()[0]` 风险，见 2.6）应用于全部 13 个方法的输出 DTO，对强语义枚举字段的影响范围广泛。

---

## 4. 修复指引方向（仅定位，不展开修复步骤）

修复者在收到本报告后，建议按以下优先级处理：

1. **首先解决 §8.2 AI 13 项 DTO 与需求 3.4.x 的对齐问题（2.1）**：这是影响最广泛的问题，13 项 AI 能力的 DTO 字段定义决定了 Phase 2-5 全部 AI 能力的契约形态。建议在编码 `MockAiService` 与 `AiService` 接口前先完成字段对齐。
2. **其次处理 Phase 0 范围越界问题（2.3）**：识别路线图 §0.4 "明确不包含"的边界，回收 OOD 中超出"骨架必备"粒度的内容（完整 JPA 关系映射、跨模块事件模式、ApiClient/AuthStore 等），降低 Phase 0 工期压力。
3. **同步修订 §8.2 行 907 的对齐范围声明（2.4）**：与 2.1 的实际状态保持一致。
4. **修复 `PatientInfo` 字段语义（2.2）**：与 2.1 同步处理。
5. **术语与表述修正（2.5、2.7、2.8、2.9）**：术语误用与冗余表述，修复成本低。
6. **Mock 占位规则风险显式声明（2.6）**：在 §3.4 Mock 占位约定段落补充强语义枚举的处理建议。
7. **其余低优先级问题（2.10-2.14）**：作为 Phase 0 实施时的参考。

---

## 5. 范围说明

**本诊断范围内**：
- §8.2 AI 13 项 DTO 字段定义与需求 3.4.x 的对齐状态；
- Phase 0 OOD 是否符合路线图 §0.2 "骨架必备/推荐补齐/明确不包含"清单；
- ODD 文档内部定义矛盾与术语准确性；
- Mock 占位规则的语义风险。

**本诊断范围外**（未深入展开）：
- 后端实现细节（如 BaseEntity 软删除 SQL 在不同 DB 上的方言差异、Spring Boot 自动配置默认值等）：修复者在编码时查阅 Hibernate / Spring Boot 文档是正常编码活动；
- 前端 UI 实现细节（如 Vue 3 + Vite 组件库的具体选择、Pinia store 的 state 结构等）：属于 Phase 1+ 实施细节；
- AI 模型的真实接入（Phase 5 范围）：OOD 仅需定义契约骨架；
- 13 项 AI 能力的业务融合逻辑（属 7.2 业务融合验收）：OOD 不涉及业务融合层；
- §10 CI 流水线阶段切换的详细脚本：属于实施细节；
- §9.2 中 `@EntityScan` / `@EnableJpaRepositories` / `@SpringBootApplication` 的具体扫描行为验证：属于 Spring Boot 自动配置的常规验证。

**未涉及但读者可能关注的相邻问题**：
- 需求文档自身的内部一致性（如 §3.4.x 之间字段约束的不完全对齐）：不在本诊断范围；
- 路线图与需求文档的覆盖关系（如 Phase 5 "13 项 AI 全部首次落地"是否完全覆盖需求 1.4 的 13 项）：属于路线图层面的审视，不在本 OOD 诊断范围。

---

> 报告结束
