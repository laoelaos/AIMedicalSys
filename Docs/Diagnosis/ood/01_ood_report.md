# OOD Phase 0 问题诊断报告

> 诊断对象：`Docs/04_ood_phase0.md`（Phase 0 最小化骨架 — 架构级 OOD 设计方案）
> 诊断范围：定义矛盾、事实错误、逻辑错误、偏离需求文档、偏离路线图 Phase 0 范围
> 诊断基线：`Docs/01_requirement.md`（需求规格说明书）、`Docs/03_roadmap.md`（实现路线图）、`Docs/02_tech.md`（技术栈文档）
> 诊断时间：2026-06-16

---

## 1. 概述

`Docs/04_ood_phase0.md` 整体结构清晰、模块划分自洽，Maven 多模块结构与 Vite 工作区布局均与路线图 Phase 0 的"骨架必备"清单对齐。但经逐段对照需求文档 3.4.x 与路线图 Phase 0 范围后，发现以下系统性问题：

- **§8.2 AI 13 项能力 DTO 字段定义与需求文档 3.4.x 多处不符**：13 项能力中有 12 项（11 项全字段不符 [3.4.1 / 3.4.2 / 3.4.3 / 3.4.5 / 3.4.6 / 3.4.7 / 3.4.8 / 3.4.10 / 3.4.11 / 3.4.12 / 3.4.13]，1 项部分不符 [3.4.4 仅在共享 `PatientInfo` 维度]）存在字段缺失或语义偏差，且 OOD §8.2 行 907 自陈"已与需求文档 3.4.4 / 3.4.9 / 3.4.10 / 3.4.13 对齐"，与实际状态不一致（3.4.9 已对齐但未声明，3.4.4 / 3.4.10 / 3.4.13 主结构已对齐但共享 `PatientInfo` 内部 `allergies` vs 需求 `allergy_history` 字段语义不符，3.4.13 自身字段约束未被独立核查）。
- **多段定义越过 Phase 0 范围**：权限模型实体（含完整 JPA 关系映射）、跨模块事件模式、ApiClient/AuthStore 等均超出"骨架"粒度，与路线图 §0.4"明确不包含：模块级接口契约冻结"相冲突。
- **若干文档内自相矛盾与术语误用**：DTO 命名风格（camelCase vs snake_case）未声明 JSON 序列化约定、"Vite workspace" 表述不准确、Mock 占位规则存在语义风险等。

本报告按"**高 / 中偏高 / 中 / 低**"四档优先级组织问题（高：关键遗漏；中偏高：影响面广；中：局部问题；低：表述修正），每项均给出具体文档位置、问题描述与诊断依据。**报告停留在定位层，不展开修复方案**——修复者在编码时查阅 Hibernate / Spring Data / Springdoc 文档是正常编码活动，不属于"诊断不完整"。

> 复核补充：本报告形成后又对 `Docs/01_requirement.md` 与 `Docs/03_roadmap.md` 做了二次交叉核对。需特别注意三点：
> 1. `allergy_details` 属于需求文档 3.1.6 过渡期默认行为允许预留的**可选扩展容器**，不应再被一概定性为 "Phase 0 越界字段"；真正需要修正的是 `allergy_history` 的主字段命名、类型与回退语义。
> 2. 关于完整 JPA 映射、装配策略、springdoc 配置等内容，更准确的定性应为"Phase 0 粒度偏细、建议收敛"，而非一律视为已被路线图客观禁止。
> 3. §2.9（profile 切换描述冗余）与 §2.13（CI 摘要颗粒度差异）更适合作为表述优化观察，不再视为独立缺陷。

### 1.1 13 项 AI 能力诊断覆盖率核查

经逐项对照需求文档 §3.4.1—§3.4.13，诊断覆盖状态如下：

| 能力编号 | 诊断覆盖状态 | 关键问题位置 |
|---------|------------|------------|
| 3.4.1 智能分诊 | 已覆盖 | §2.1 行 56-57 |
| 3.4.2 AI 处方审核 | 已覆盖 | §2.1 行 58-60 |
| 3.4.3 AI 病历生成 | 已覆盖 | §2.1 行 61-62 |
| 3.4.4 AI 智能诊断 | 已覆盖（含 `PatientInfo` 偏差） | §2.2 |
| 3.4.5 AI 智能检查报告 | 已覆盖 | §2.1 行 64 |
| 3.4.6 AI 智能检验报告 | 已覆盖 | §2.1 行 65-66 |
| 3.4.7 AI 影像分析 | 已覆盖 | §2.1 行 67-69 |
| 3.4.8 AI 知识库问答 | 已覆盖 | §2.1 行 70-71 |
| 3.4.9 AI 开立检查/检验 | 已覆盖（含 `PatientInfo` 偏差） | §2.2 |
| 3.4.10 AI 辅助开方 | 已覆盖 | §2.1 行 73 |
| 3.4.11 AI 执行顺序推荐 | 已覆盖 | §2.1 行 74-75 |
| 3.4.12 AI 医生排班 | 已覆盖 | §2.1 行 76-77 |
| 3.4.13 AI 综合讨论结论 | 已覆盖（含 `PatientInfo` 偏差 + 字段约束偏差） | §2.1（补）/ §2.2 |

**覆盖率修正值**：v1 覆盖率约 70%（覆盖 9 项），v2 修正为 **100%**（13 项全部覆盖，其中 11 项存在独立字段不符点，1 项 [3.4.4] 仅共享 `PatientInfo` 维度不符，1 项 [3.4.9] 主结构与共享 `PatientInfo` 偏差均在 §2.2 处理）。

---

## 2. 诊断发现

### 【高优先级】2.1 §8.2 AI 13 项能力 DTO 字段定义与需求文档 3.4.x 多处不符

**位置**：`Docs/04_ood_phase0.md` §8.2 "AI 能力方法清单（Phase 0 Mock 占位）"，行 882-1120；§3.4 "Mock 数据占位约定"，行 622-629；§8.2 行 907 自陈对齐范围声明。

**现象**：13 项 AI 能力的输入/输出 DTO 字段定义与需求文档 3.4.x 不匹配。OOD §8.2 行 907 声称"以下骨架已与需求文档 3.4.4 / 3.4.9 / 3.4.10 / 3.4.13 的输入输出契约对齐"，但 §8.2 表中实际列出了全部 13 项的 DTO 字段，且未对齐的能力仍给出字段定义，与声明产生矛盾。

**逐项不符清单**（按需求文档 3.4.x 顺序）：

| 需求条款 | OOD 位置 | 缺失/偏差字段 | 性质 |
|---------|---------|--------------|------|
| **3.4.1 智能分诊输入** | 需求 §3.4.1 行 821-833 / OOD §8.2 行 910-913 `TriageRequest` | 缺 `session_id`（**必填**）、`additional_responses`（追问回答数组）、`patient_id`、`rule_version`、`rule_set_id` | 必填字段缺失 |
| **3.4.1 智能分诊输出** | 需求 §3.4.1 行 821-833 / OOD §8.2 行 915-918 `TriageResponse` | 缺 `reason`（**必填**，≥1 字符）、`matched_rules`；`recommendedDept`（单数）与需求 `recommended_departments`（数组，0-3 项）语义不符 | 必填字段缺失 + 字段类型偏差 |
| **3.4.2 AI 处方审核输入** | 需求 §3.4.2 行 835-855 / OOD §8.2 行 924-941 `PrescriptionCheckRequest` | 缺 `prescription_id`（**必填**） | 必填字段缺失 |
| **3.4.2 AI 处方审核输入`PrescriptionDrug`** | 需求 §3.4.2 行 839 / OOD §8.2 行 928-932 | 缺 `drug_id`、`route`；字段语义偏差：`dosage`（需求为 `dose`）、`days`（需求为 `duration`） | 必填字段缺失 + 字段语义偏差 |
| **3.4.2 AI 处方审核输出** | 需求 §3.4.2 行 835-855 / OOD §8.2 行 949-952 `PrescriptionCheckResponse` | 字段定义偏离需求：OOD `auditResult/riskLevel/warnings` vs 需求 `risk_level/interactions/alerts/suggestions` | 整体字段集不符 |
| **3.4.3 AI 病历生成输入** | 需求 §3.4.3 行 857-883 / OOD §8.2 行 954-956 `MedicalRecordGenRequest` | 缺 `patient_id`（**必填**）、`encounter_id`（可选）、`stream`（bool，可选，默认 false）；`dialogue_text`（必填，50–10000 字符）无字符数约束；流式/非流式架构选型未在 OOD §8.2 中预留 | 必填字段缺失 + 字符数约束缺失 + 架构选型未预留 |
| **3.4.3 AI 病历生成输出** | 需求 §3.4.3 行 863 / OOD §8.2 行 957-958 `MedicalRecordGenResponse` | **结构严重不符**：需求要求 8 字段结构化（`chief_complaint` / `symptom_description` / `present_illness` / `past_history` / `physical_exam` / `preliminary_diagnosis` / `treatment_plan` / `missing_fields`）；OOD 仅 `structuredRecord: String`（单字符串，退化为 JSON 字符串字段）；与 3.4.4 / 3.4.13 的"组装约定"风格不一致 | 整体结构不符 |
| **3.4.4 智能诊断`PatientInfo`** | 需求 §3.4.4 行 895 / OOD §8.2 行 934-941 | 缺 `allergy_history`（string 类型）；OOD 用 `allergies: List<String>` 代替，但需求 3.4.2/3.4.4/3.4.9/3.4.10/3.4.13 均要求 `allergy_history: string`（逗号拼接过敏原名称） | 字段语义偏差 |
| **3.4.5 检查报告输入** | 需求 §3.4.5 行 917 / OOD §8.2 行 983-985 `InspectionReportRequest` | 缺 `exam_id`（**必填**）、`exam_type`（**必填**）、`patient_id`（**必填**）；可选 `exam_items`（array，每项含 5 子字段）缺失；OOD `rawData: String` 与需求 `raw_data_ref: String` 命名偏差（语义接近） | 4 项必填/可选字段缺失 + 命名偏差 |
| **3.4.5 检查报告输出** | 需求 §3.4.5 行 919 / OOD §8.2 行 986-988 `InspectionReportResponse` | **严重不完整**：仅定义 `reportDraft`，缺 `findings`、`impression`、`auxiliary_interpretation`、`abnormal_items`、`comparison_summary`、`confidence`、`image_recognition` | 7 项核心字段缺失 |
| **3.4.6 检验报告输入** | 需求 §3.4.6 行 931 / OOD §8.2 行 989-991 `LabTestReportRequest` | 缺 `lab_id`（**必填**）、`patient_id`（**必填**）；需求 `lab_items`（array，每项含 `item_name`/`value`/`unit`/`reference_range`/`status`）整体结构未引入；OOD `rawData: String` 与需求结构契约不符 | 2 项必填字段缺失 + 结构契约整体缺失 |
| **3.4.6 检验报告输出** | 需求 §3.4.6 行 934-936 / OOD §8.2 行 992-993 `LabTestReportResponse` | 仅 `reportDraft`，缺 `abnormal_items`（必含 `delta` 字段）、`interpretation`、`suggestions`、`confidence` | 4 项核心字段缺失 |
| **3.4.7 影像分析输入** | 需求 §3.4.7 行 942-957 / OOD §8.2 行 995-998 `ImageAnalysisRequest` | 缺 `patient_id`（**必填**）；OOD `modality: String`（模态枚举 CT/MRI/X-Ray）与需求 `model_id`（后端岗位推断的模型标识）**语义完全错配**——两者属性类别不同（模态 vs 模型标识），前端传入任意字段都无法满足需求；OOD `imageUrl` 与需求 `image_ref` 命名偏差（`url` 表示可下载地址，`ref` 表示任意数据引用） | 1 项必填字段缺失 + 语义完全错配 + 命名偏差 |
| **3.4.7 影像分析输出** | 需求 §3.4.7 行 952-957 / OOD §8.2 行 999-1007 `ImageAnalysisResponse` | **严重不完整**：缺 `model_id`、`recognition_result`（含 `regions`/`labels`/`scores`）、`segmentation_mask_ref`、`auxiliary_advice`；OOD 用 `findings: List<Finding>` 代替 `recognition_result` 对象 | 整体结构不符 |
| **3.4.7 `Finding.confidence`** | 需求 §3.4.7 行 956 / OOD §8.2 行 1005 | 注释 "[0, 1]"，但需求 3.4.7 要求外层 `confidence: 0-100`，且单个发现的置信度无明确范围约束 | 字段语义偏差 |
| **3.4.8 知识库问答输入** | 需求 §3.4.8 行 959-971 / OOD §8.2 行 1008-1011 `KbQueryRequest` | 缺 `user_role`（**必填**，enum `PATIENT/DOCTOR`）、`session_id`（**必填**） | 2 项必填字段缺失 |
| **3.4.8 知识库问答输出** | 需求 §3.4.8 行 959-971 / OOD §8.2 行 1012-1015 `KbQueryResponse` | 缺 `related_questions`、`disclaimer_required`（固定 true） | 2 项字段缺失 |
| **3.4.10 AI 辅助开方输出 `PrescriptionDraft`** | 需求 §3.4.10 行 993 / OOD §8.2 行 1057-1058 `PrescriptionDraft` | `prescriptionDraft.drugs` 复用 3.4.2 的 `PrescriptionDrug` 类型（OOD §8.2 行 928-932），字段集偏差（`drug_id`/`route` 缺失 + `dosage`/`days` 语义偏差）已在 §2.1 行 58-60 标记，但 **3.4.10 输出契约未关联声明**，修复者可能只改 3.4.2 入口而忽略 3.4.10 输出（参见 §3 跨章节一致性观察 4） | 输出契约类型复用未关联标记 |
| **3.4.11 AI 执行顺序推荐输入** | 需求 §3.4.11 行 1009 / OOD §8.2 行 1071-1073 `ExecutionOrderRequest` | `task_items` 子字段缺 4 项（`item_name` / `urgency_hint` / `patient_id` / 枚举约束 `task_type: IMAGING/LAB`、`urgency_hint: LOW/MEDIUM/HIGH`）；`context`（object，可选，含 `encounter_id` / `department_id` / `available_resources`）整体缺失；`task_role`（**必填**，enum `IMAGING_DOCTOR / LAB_DOCTOR`）整体缺失——岗位权限校验依赖此字段 | 5 项输入字段/结构缺失（含 1 项必填） |
| **3.4.11 AI 执行顺序推荐输出** | 需求 §3.4.11 行 1011 / OOD §8.2 行 1080-1082 `ExecutionOrderResponse` | 缺 `recommended_time`、`reason`（在 `execution_order` 每项中）；缺 `summary`、`disclaimer_required`（顶层）；优先级语义错位——需求 P1/P2/P3 三档 vs OOD `Integer priority 1-5`（数值语义风格不一致，影响 UI 三色档渲染） | 4 项输出字段缺失 + 优先级语义错位 |
| **3.4.12 AI 排班输入** | 需求 §3.4.12 行 1025-1039 / OOD §8.2 行 1083-1088 `ScheduleRequest` | **结构严重偏离**：OOD 平铺 `department/startDate/endDate/doctorIds`，需求要求嵌套 `constraints: {start_date/end_date/department_ids/shift_rules/doctor_quota}` + `doctor_pool: array`（含 doctor_id/doctor_name/department_id/title/available_days/unavailable_days）+ `historical_schedule_ref` | 整体结构不符 |
| **3.4.12 AI 排班输出** | 需求 §3.4.12 行 1040-1044 / OOD §8.2 行 1089-1096 `ScheduleResponse` | 缺 `workload_score`（0-100）、`summary`、`conflicts`；OOD 用 `location`（非需求字段） | 3 项字段缺失 |
| **3.4.13 AI 综合讨论结论输入** | 需求 §3.4.13 行 1050-1058 / OOD §8.2 行 1098-1104 `DiscussionConclusionRequest` | 缺 `condition.chief_complaint` / `patient_info.patient_id` / `encounter_id` 三者任一缺失的必填校验规则（需求要求返回 `DISCUSS_AI_INPUT_INVALID`，覆盖全部 8 种组合）；`encounter_id` 必填语义未在 OOD 字段注释中标注 | 输入校验规则缺失 + 必填语义未声明 |
| **3.4.13 AI 综合讨论结论输出** | 需求 §3.4.13 行 1060-1063 / OOD §8.2 行 1106-1110 `DiscussionConclusionResponse` | `discussion_text` 字符数约束未声明（需求：必填，最低质量门槛 ≥ 50 字符，建议 ≥ 100 字符）；`summary` 项数与每项字符数约束未声明（需求：array，0–5 项，每项 ≤ 100 字符）；`DISCUSS_AI_OUTPUT_INCOMPLETE` 错误码（对应 `discussion_text` 字符数 < 50）未在 OOD 中定义 | 字符数/项数约束缺失 + 错误码定义缺失 |

**根因定位**：§8.2 行 907 显式声明对齐范围为"3.4.4 / 3.4.9 / 3.4.10 / 3.4.13"4 项，但 §8.2 表实际列出全部 13 项 DTO。3.4.4/3.4.9/3.4.10/3.4.13 的 DTO 主结构（如 `condition: ConditionInfo`、`patient_info: PatientInfo`、`exam_results`）与需求一致，但共享的 `PatientInfo` 类型内部 `allergies` vs 需求 `allergy_history` 字段语义不符（见 2.2）。3.4.13 自身字段约束（字符数、项数、错误码）未在 OOD 中显式声明。其余 8 项（3.4.1/3.4.2/3.4.3/3.4.5/3.4.6/3.4.7/3.4.8/3.4.11/3.4.12）的 DTO 字段定义偏离需求。

**触发条件**：
- §8.2 "Phase 0 各 DTO 的核心字段结构定义如下"段落被理解为"全部 13 项的 DTO 字段定义"，但未声明其中 8 项为占位/待对齐；
- §8.2 表中方法清单列出全部 13 项及对应 DTO，混淆"已对齐"与"占位/待对齐"边界。

**影响范围**：
- 后续阶段（Phase 2-5）落地 AI 能力时，需做 DTO 破坏性调整（与 §8.2 自身"避免后续再做 DTO 破坏性调整"声明相矛盾）；
- 前端 `packages/shared/types/`（OOD §8.3 行 1130）若按当前 OOD DTO 同步 TypeScript 类型，将与后端实际 AI 契约不一致；
- `MockAiService` 实现（§3.4 行 603）按当前 DTO 生成占位数据时，缺字段会以 null 填充，与 Mock 占位规则（§3.4 行 627-629）的"无任何可空标记的字段视为必填，按前述规则填充占位值"冲突——即 Mock 会因字段缺失而无法运行；
- **修复连锁变更（4 项副作用）**：
  1. **Mock 实现层 `MockAiService` 重写**：13 个方法中至少 11 个的输入/输出 DTO 需重新生成 Mock 数据，§3.4 行 624-629 的占位规则应用面扩大；
  2. **TypeScript 类型同步**：`packages/shared/types/` 全部 AI 相关 interface 需重新生成（§8.3 行 1130），若 Phase 0 已生成 TS 类型，diff 工作量大；
  3. **OpenAPI 规范追溯**：§8.3 行 1130 的 springdoc-openapi 配置生成的 OpenAPI schema 与 snake_case 命名约定（见 2.7）需协同调整；
  4. **需求文档 vs OOD 优先级**：若诊断建议全部回退到需求契约，修复者需面对"13 项 AI 能力全部按需求字段集实现"的工作量，与 Phase 0 "骨架" 定位可能冲突——是否仅对齐"骨架必备"的能力（3.4.1 / 3.4.2 / 3.4.3）而其他能力保留 Mock 占位，需明确（见 §4 修复指引方向分级建议）。

---

### 【高优先级】2.2 `PatientInfo` 字段语义与需求文档 5 项 AI 契约不符（含结构化字段与超出需求字段）

**位置**：`Docs/04_ood_phase0.md` §8.2 行 934-941 `PatientInfo` 定义；该类型被 `PrescriptionCheckRequest`、`DiagnosisRequest`、`ExaminationRecommendRequest`、`PrescriptionAssistRequest`、`DiscussionConclusionRequest` 5 个 DTO 共同引用。

**现象**：需求文档 3.4.2 / 3.4.4 / 3.4.9 / 3.4.10 / 3.4.13 共 5 项 AI 能力的输入契约均要求 `patient_info` 字段集为 `{ patient_id, age, gender, allergy_history, comorbidities }`（参见需求 §3.4.2 行 839、§3.4.4 行 895、§3.4.9 行 977、§3.4.10 行 991、§3.4.13 行 1054）。其中 `allergy_history` 为 `string` 类型，由 `allergen` 列表以中文逗号拼接（见需求 §3.1.6 行 390 的映射约定）。OOD §8.2 `PatientInfo` 用 `List<String> allergies` 代替 `string allergy_history`，类型不同。

**结构化字段未对齐（`allergyDetails`）**：OOD §8.2 行 941 在 `PatientInfo` 内还定义了 `List<AllergyDetail> allergyDetails`（含 `allergen` / `reactionType` / `severity`（`MILD/MODERATE/SEVERE`）/ `occurredAt` 四子字段），并显式声明"缺省时回退到 `allergies`"。但：
- 需求文档 3.4.2 / 3.4.4 / 3.4.9 / 3.4.10 / 3.4.13 五项 AI 能力的 `patient_info` 契约字段集均**未定义** `allergyDetails`，仅 `allergy_history: string`；
- 路线图 §0.2 行 50 提及"健康档案基础版（基本字段 + 5 类结构化子字段 CRUD；前端 UI 同步提供过敏详情扩展容器的录入能力）"明确将过敏详情结构化录入归 Phase 1+，Phase 0 不应在此阶段定义 AI 输入契约侧的 `allergyDetails` 字段；
- OOD §8.2 行 941 注释"缺省时回退到 `allergies`"暗示 `allergyDetails` 是新增字段，与需求 AI 契约字段集不一致；该字段对 5 项 AI 契约的实际回退规则（哪个字段作为主源、是否一并传递）未在 OOD 中明示。

**超出需求的字段（`name`）**：OOD §8.2 行 936 `PatientInfo` 包含 `String name`（患者姓名），但需求文档 3.4.2 / 3.4.4 / 3.4.9 / 3.4.10 / 3.4.13 五项 AI 能力的 `patient_info` 契约字段集均**未定义** `name`（需求 `patient_info` 字段集为 `{patient_id, age, gender, allergy_history, comorbidities}`）。OOD §8.2 行 936 的 `name` 字段：
- 对 AI 推理无直接用途（AI 不依赖姓名做诊断）；
- 引入该字段可能影响 Mock 占位数据生成规则（§3.4 行 624-629 的 Mock 占位约定按"无任何可空标记的字段视为必填"原则，`name` 会成为必填字段）；
- 与"骨架必备"路线图 §0.2 行 36-42 的粒度不符——属越界定义的非契约字段。

**根因定位**：§8.2 `PatientInfo` 定义未按需求 3.1.6 行 388-398 的"健康档案数据→AI 契约字段的映射约定"实现，且 §8.2 行 936 的 `name` 与行 941 的 `allergyDetails` 未对照需求 3.4.x 中 `patient_info` 字段集约束做边界收敛。

**触发条件**：
- §8.2 `PatientInfo` 定义时未交叉对照需求 3.4.x 中 `patient_info` 字段集约束；
- §3.4.2/3.4.4/3.4.9/3.4.10/3.4.13 的 OOD DTO 共用同一 `PatientInfo`，但 OOD §8.2 行 907 自陈"已对齐"的承诺与实际 `PatientInfo` 内部结构矛盾；
- Phase 1+ 路线图（路线图 §1.2 行 86）才要求"健康档案基础版（基本字段 + 5 类结构化子字段 CRUD）"，但 §8.2 行 941 的 `allergyDetails` 在 Phase 0 即定义。

**影响范围**：
- 5 项 AI 能力的 Mock 实现（`MockAiService`）无法按当前 `PatientInfo` 直接序列化 AI 输入契约，需在调用层做额外字段转换，转换规则未在 OOD 中明示；
- `comorbidities` 字段在 OOD `PatientInfo` 中未标注必填（与需求 3.4.4/3.4.9/3.4.13 中 `comorbidities` 必填的要求不符）；
- `name` 字段在 §3.4 Mock 占位规则下成为必填，Mock 数据生成需额外填充，但需求 AI 契约无需该字段——Mock 阶段产生与真实 AI 契约不一致的字段集；
- `allergyDetails` 字段在 Phase 0 Mock 阶段被定义但无真实数据结构支撑（健康档案 Phase 1+ 才落地），导致 5 项 AI Mock 实现对该字段的处理逻辑依赖隐含约定（"缺省时回退"），跨阶段一致性风险。

---

### 【高优先级】2.3 路线图 §0.4"明确不包含"被多项 OOD 定义越界

**位置**：多处，详见下表。

**现象**：路线图 §0.2（行 35-42）"骨架必备（阻塞并行）"包含：共享工程结构、接口契约框架、**数据与权限模型骨架：数据实体基类与权限模型就位**、协作规范、本地开发体验、持续集成占位。路线图 §0.4（行 66-72）"本阶段明确不包含"列举：(a) 任何业务功能；(b) **模块级接口契约冻结**（在对应阶段启动前冻结）。但 OOD 多处定义超过"骨架必备"粒度，进入"接口契约冻结"或"业务实现"范畴：

**路线图 §0.2 骨架必备 vs OOD 定义粒度对照表**：

| 路线图 §0.2 骨架必备要求 | OOD 位置 | OOD 定义粒度 | 是否超出骨架必备 |
|---------|---------|------------|--------------|
| 数据实体基类就位 | §3.3 行 529-595 User/Role/Post/Function 实体 | **完整 JPA 关系映射**：`@JoinTable` / `@JoinColumn` / `fetch` / `cascade` / `orphanRemoval`、关联表命名约定、Fetch/Cascade 策略说明 | **超出**：骨架必备仅要求"实体类 + 主键 + 基本字段就位"，完整 JPA 关联映射约定属"模块级接口契约冻结"（路线图 §0.4 行 69） |
| 数据实体基类就位 | §3.3 行 587-594 `DataPermissionEvaluator` 门面扩展点、数据范围枚举（`DataScopeType.SELF_OWNED` / `SELF_HANDLED`） | **扩展点契约冻结**：Phase 0 冻结约定声明 | **部分超出**：扩展点接口本身可保留（属"可演进骨架"目标），但具体数据范围枚举值与冻结约定属接口契约冻结 |
| AI 能力模块 Mock 占位（§0.2 行 50 推荐补齐） | §3.4 行 632-645 `MockAiService` 装配策略表（`ai.mock.enabled` 三种取值）、FallbackAiService 装饰器排除自身逻辑、`@ConditionalOnProperty` 与 `@ConditionalOnMissingBean` 组合 | **AI 装配策略完整冻结**：3 种取值语义、装饰器自引用排除逻辑 | **部分超出**：Mock 占位属推荐补齐（不阻塞 Phase 0 验收），但完整装配策略表超出 Mock 占位粒度 |
| （无明确要求） | §8.4 行 1145-1198 Spring ApplicationEvent 跨模块调用模式二：POJO 事件类（`UserRegisteredEvent`）示例、发布订阅注解（`@EventListener`）、模式选择原则 | **跨模块调用接口契约冻结**：模式选择原则 | **超出**：跨模块事件模式属 Phase 1+ 业务模块阶段 |
| （无明确要求） | §3.5 行 666-681 `ApiClient` 类（含 JWT 请求拦截器、401 响应跳转登录页、`NETWORK_ERROR` 错误拦截）、`AuthStore` Pinia store（含登录态、token、用户信息） | **完整认证基础设施**：JWT 拦截器、401 跳转登录页 | **超出**：Phase 0 无登录态，认证基础设施属 Phase 1 统一认证阶段（路线图 §1.2 行 84-85） |
| API 文档自动生成（§0.2 行 47 推荐补齐） | §8.3 行 1124-1143 完整 springdoc-openapi 配置（`springdoc.api-docs.path: /v3/api-docs`、`swagger-ui.path: /swagger-ui.html`）、生产环境关闭策略、与 Phase 0 验收挂钩的描述 | **完整生产级配置** | **超出**：推荐补齐仅需"工具集成"，生产级配置属接口契约冻结 |
| API 文档自动生成（§0.2 行 47 推荐补齐） | §8.3 行 1130 TypeScript 类型同步机制、Phase 1+ 引入 openapi-generator 的迁移路径 | **类型同步机制冻结** | **超出**：迁移路径属 Phase 1+ 阶段 |

**根因定位**：OOD §1.1 行 7-12 的设计目标"骨架可运行""可演进"被理解为"在骨架中预留完整扩展点"，导致扩展点的定义粒度超过路线图 §0.2"骨架必备"清单与 §0.4"明确不包含"的边界。

**触发条件**：
- 路线图 §0.4 "明确不包含" 与 OOD §1.1 设计目标"可演进：骨架预留 AI 能力抽象层、权限模型扩展点、微服务化拆分演进路径"之间的边界缺乏量化标准；
- §3.3 末尾（行 590-594）显式声明"Phase 0 先冻结扩展点，不在本阶段提供实现"，与"不冻结接口契约"路线图原则直接冲突。

**影响范围**：
- 后续阶段（Phase 1+）调整这些接口契约时，需对 Phase 0 已交付的骨架代码做破坏性修改，与"骨架可演进"目标部分矛盾；
- Phase 0 工期被这些冻结内容占用，可能挤压"骨架必备"任务（搭建任务 A-H）的工期；
- **修复副作用（3 项）**：
  1. **Phase 1 权限模型落地（路线图 §1.2 行 86）**：若 §3.3 完整 JPA 映射被回收，Phase 1 启动时需重新定义关联表命名、Fetch/Cascade 策略——但骨架必备"实体类 + 主键 + 基本字段"需保留，避免 Phase 1 重新建表；
  2. **Phase 1 统一认证（路线图 §1.2 行 84-85）**：若 §3.5 ApiClient/AuthStore 被回收（含 JWT 请求拦截器、401 跳转登录页），Phase 1 启动时前端 `packages/shared/api/` 需重新封装；
  3. **"骨架可演进"目标冲突**：路线图 §0.2 行 38 "接口契约框架"与 §0.4 行 69 "模块级接口契约冻结"存在边界——Phase 0 的"骨架可演进"是"预留扩展点"，而"明确不冻结接口契约"是"不在 Phase 0 冻结"。诊断需保留"扩展点接口本身"（属骨架），回收"接口约定的具体值与冻结声明"（属接口契约冻结）。

**回收对象 vs 保留对象清单**（修复指引）：

- **保留**：
  - 实体类骨架（含主键 + 基本字段 + 简单的 `List<Long>` ID 引用字段如 `roleIds` / `postIds`，**不**使用 `@ManyToMany` 注解）；
  - `DataPermissionEvaluator` 扩展点接口本身（**不**含数据范围枚举冻结）；
  - `AiService` 接口骨架 + `MockAiService` 占位实现（**不**含装配策略表）。

- **回收**：
  - 完整 JPA 关联映射（`@JoinTable` / `@JoinColumn` / `fetch` / `cascade` / `orphanRemoval` 显式声明）；
  - **关联表 schema 本身**（`user_role` / `user_post` / `post_function` 三张表）：因 `@ManyToMany` 回收后，无 JPA 关联注解引用这些表，Phase 0 迁移脚本不应创建这些表；
  - **User-Role 之间关联的骨架表示**：实体类使用 `List<Long> roleIds`（不映射关联表，仅作为 ID 引用），Phase 1+ 启动时再补回 `@ManyToMany` 与关联表迁移；
  - 数据范围枚举冻结约定（`DataScopeType.SELF_OWNED` / `SELF_HANDLED` 等枚举值的冻结声明）；
  - `@ConditionalOnProperty` 与 `@ConditionalOnMissingBean` 组合策略表、`ai.mock.enabled` 三种取值完整装配策略；
  - 跨模块事件模式示例（`UserRegisteredEvent` / `@EventListener` / 模式选择原则）；
  - `ApiClient` / `AuthStore` 完整实现（含 JWT 请求拦截器、401 跳转登录页、`NETWORK_ERROR` 错误拦截）；
  - 完整 springdoc-openapi 生产级配置；
  - TypeScript 类型同步迁移路径。

- **回收时同步给出 Phase 1 启动时需要重新冻结的内容清单**：
  - 完整 JPA 关联映射约定（含关联表 schema 迁移）；
  - 数据范围枚举值与权限传播规则；
  - `MockAiService` 与 `FallbackAiService` 装配优先级；
  - 跨模块事件模式选型。

---

### 【高优先级】2.4 §8.2 行 907 对齐范围声明与实际不符

**位置**：`Docs/04_ood_phase0.md` §8.2 行 907。

**现象**：OOD 声明"以下骨架已与需求文档 3.4.4 / 3.4.9 / 3.4.10 / 3.4.13 的输入输出契约对齐，避免后续再做 DTO 破坏性调整"。但：

1. §8.2 表中实际列出全部 13 项 AI 方法的 DTO 字段，未对齐的 8 项仍给出详细字段定义，与"对齐"声明产生混淆。
2. 即使是声明"已对齐"的 4 项（3.4.4 / 3.4.9 / 3.4.10 / 3.4.13），其共享的 `PatientInfo` 内部字段（`allergies` vs 需求 `allergy_history`）仍存在语义偏差（见 2.2）。
3. 3.4.13 主结构虽与需求一致，但其自身字段约束（`discussion_text` ≥ 50 字符、`summary` 0-5 项每项 ≤ 100 字符、`encounter_id` 必填、`DISCUSS_AI_OUTPUT_INCOMPLETE` 错误码）未被 OOD §8.2 显式声明，与"对齐"承诺仍有偏差（见 2.1 新增 3.4.13 行）。
4. `MedicalRecordGenRequest`（对应 3.4.3）虽不在声明对齐范围，但其与"已对齐"声明的偏差被忽略——OOD §8.2 行 954 `MedicalRecordGenRequest` 仅 1 个字段，与 §8.2 行 907 的"已对齐"承诺产生未声明的偏差。

**根因定位**：§8.2 行 907 的对齐范围声明基于 AI 能力的方法名核对，未逐字段对照需求 3.4.x 的输入/输出契约字段清单与字段约束（必填、字符数、项数、错误码等）。

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
- §7 行 853 的设计决策表将"Vite workspace"作为最终选择，与路线图 §0.4"避免引入额外的工具链依赖"的语义混淆（Vite 本身是构建工具，workspace 是包管理器能力，两者叠加 vs 单一 Turborepo/Nx 的对比标准不清晰）；
- **修复副作用**：修订时需联动调整 §7 行 853 决策表的对比维度（应改为"包管理器 workspace 机制：npm workspaces vs yarn workspaces vs pnpm workspaces"，而非将 Vite 与 Nx/Turborepo 并列）。

---

### 【中偏高优先级】2.6 §3.4 Mock 占位规则的语义风险

**位置**：`Docs/04_ood_phase0.md` §3.4 行 622-629 "Mock 数据占位约定"。

**现象**：§3.4 行 626-627 声明 Mock 占位规则：
- "枚举字段：填充目标枚举类型的第一个枚举值（`EnumType.values()[0]`）"

该规则对具有强语义的枚举字段（如 `audit_result` ∈ {PASS, FLAG, REJECT}、`risk_level` ∈ {LOW, MEDIUM, HIGH}、`severity` ∈ {MILD, MODERATE, SEVERE}、`urgency_hint` ∈ {LOW, MEDIUM, HIGH}）存在语义风险——Mock 默认填充值为 `EnumType.values()[0]`（即枚举声明顺序的第一个值），若声明顺序与业务中性值不一致（如 HIGH 在前），则 Mock 默认填充高风险/严重值：

- `audit_result.values()[0]` —— 视声明顺序可能是 PASS（业务安全）或 REJECT（业务极端）
- `risk_level.values()[0]` —— 视声明顺序可能是 LOW（业务安全）或 HIGH（业务极端）
- `severity.values()[0]` —— 视声明顺序可能是 MILD（业务安全）或 SEVERE（业务极端）
- `urgency_hint.values()[0]` —— 视声明顺序可能是 LOW（业务安全）或 HIGH（业务极端）

OOD §8.2 中 `risk_level`、`audit_result`、`severity` 等枚举字段均出现在 3.4.2/3.4.5/3.4.6/3.4.7/3.4.10 等 AI DTO 中。

**根因定位**：§3.4 Mock 占位规则未区分"无语义枚举"与"有强语义枚举"的填充策略。

**触发条件**：
- 实施者按 OOD 规则实现 `MockAiService`，对所有枚举字段应用 `values()[0]`，未声明各枚举的"业务中性占位值"；
- 前端基于 Mock 返回值开发（如 UI 控件根据 `risk_level` 切换颜色、根据 `audit_result` 切换提示文案），将基于"全部低风险/全部审核通过"或"全部高风险/全部拒绝"的错误数据开发（取决于声明顺序）。

**影响范围**：
- 前端 UI 开发基于 Mock 数据，但 Mock 默认填充依赖枚举声明顺序，前端会开发出与真实 AI 返回不符的 UI 分支；
- `MockAiService` 与 `FallbackAiService`（§3.4 行 633-645）的降级行为混淆——Mock 永远不返回"不可用"标志，前端无法基于 Mock 验证降级路径的 UI 分支；
- 该规则应用于全部 13 项 AI 能力的 Mock 输出，**Mock 实现是 Phase 0 前端独立开发的依据**（路线图 §0.2 行 50 推荐补齐"AI 能力模块 Mock 占位"），影响范围与 §2.1 的字段缺失同样广泛。

---

### 【中偏高优先级】2.7 DTO 字段命名风格（camelCase vs snake_case）未声明 JSON 序列化约定

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
- §8.3 行 1130 "Phase 1+ 引入 openapi-generator，通过 springdoc-openapi 生成的 OpenAPI 规范自动生成 TypeScript 类型"——若 Phase 0 已按 OOD 当前 DTO 实现，则生成的 OpenAPI 规范字段为 camelCase，与需求契约 snake_case 不一致，TypeScript 类型仍会漂移；
- 该问题一旦进入 Phase 1+ 编码阶段，可能导致后端实际 JSON 字段为 camelCase，前端按 snake_case 调用，全字段映射错误——属于"全链路契约错位"风险，影响范围与 §2.1 DTO 字段缺失同等广泛（前端 `packages/shared/types/` 全部类型均受影响），但修复成本极低（单一 `@JsonNaming` 配置或全局 Jackson 配置即可）。

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

**现象**：OOD §3.4 行 634 声明 FallbackAiService"作为装饰器始终注册为 Bean"，但未显式给出其 `@Service` 注解、`implements AiService` 实现关系及 `@Primary` 装饰器标注，导致 Spring 类型注入 `List<AiService>` 时是否包含自身依赖 OOD 隐含行为推断：

1. OOD 未显式说明 FallbackAiService 被注册为 `AiService` Bean 的具体注解形式（即是否同时标注 `@Service` 与 `@Primary`，或仅标注 `@Primary`，以及是否 `implements AiService`）；
2. §3.4 行 633 提到"避免 `@ConditionalOnMissingBean` 与 `@Primary` 装饰器的语义冲突"，但 OOD 未显式给出 FallbackAiService 上的 `@Primary` 注解配置；
3. §3.4 行 634 提到"应用启动时，application 模块的配置决定 `ai.mock.enabled` 值"，但 OOD 已明示 FallbackAiService **始终注册为 Bean**（语义为无条件注册，无 `@ConditionalOnProperty` 注解）——此为 OOD 显式声明，修复者按 OOD 实现即可，不需要额外添加条件注解。

**根因定位**：§3.4 对 FallbackAiService 的 Bean 注册条件、装饰器职责、自引用排除的隐含假设未做完整说明（关于"始终注册"已声明，关于其他注解配置未明示）。

**触发条件**：
- 实施者按 OOD 实现 `FallbackAiService` 时，可能因 §3.4 行 634 的"始终注册"已显式声明而正确省略 `@ConditionalOnProperty`，但仍可能遗漏 `@Primary` 或 `@Service` 注解，导致 Bean 装配顺序异常或多个 `AiService` Bean 冲突。

**影响范围**：实施者按 OOD 实现 `FallbackAiService` 时，可能遗漏 `@Primary` 或 `@Service` 注解，导致 Bean 装配顺序异常或多个 `AiService` Bean 冲突。

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

1. **§3.4 "Mock 数据占位约定"（行 622-629）与 §8.2 DTO 字段缺失的相互作用（与 §2.1 联动分析）**：
   - §2.1 中识别的多处 DTO 字段缺失（如 `PrescriptionCheckRequest.prescription_id`、`InspectionReportResponse.impression` 等）会导致 Mock 实现无法按当前 DTO 结构生成完整占位数据；
   - §3.4 Mock 规则行 627 要求"无任何可空标记的字段视为必填"，但缺字段的 DTO 在序列化时为 null，与 Mock 规则矛盾；
   - **相互作用面清单**：(a) 13 项 AI 方法中至少 11 项的 DTO 字段缺失导致 Mock 占位值无法填全；(b) §3.4 Mock 规则适用于全部 13 项的输出 DTO，但 §2.6 中"枚举风险"在补全字段后才暴露影响（缺字段时枚举字段本身不存在）。

2. **§2.2 模块依赖图（行 272-289）与 §3.3 权限模型的位置声明**：§3.3 行 533 "所有权限模型实体（User、Role、Post、Function）归属 `common-module-impl` 子模块"，与 §2.2 依赖图中"`common-module-impl` 仅由 application 引入"一致。但 §3.3 详细定义的实体 JPA 映射（cascade、fetch、orphanRemoval）属于路线图 0.4 "模块级接口契约冻结"范畴（见 2.3）。

3. **§8.2 DTO 与 §3.4 AiService 装配策略的耦合**：§3.4 行 603 描述 `MockAiService` "实现该接口的全部 13 个方法"，§8.2 行 882 "Phase 0 定义 `AiService` 接口中包含以下 13 个方法契约"。两者对方法数量的描述一致。但 §3.4 行 624-629 的 Mock 占位规则（特别是 `values()[0]` 风险，见 2.6）应用于全部 13 个方法的输出 DTO，对强语义枚举字段的影响范围广泛。

4. **§2.1 中 3.4.2 / 3.4.10 `PrescriptionDrug` 类型复用关联**（与 §2.1 行 58-60 / §2.1 新增 3.4.10 行 联动）：
   - 3.4.10 输出 `PrescriptionAssistResponse.prescriptionDraft.drugs`（OOD §8.2 行 1041-1042）复用 3.4.2 的 `PrescriptionDrug` 类型（OOD §8.2 行 928-932）；
   - 字段集偏差（`drug_id` / `route` 缺失 + `dosage` → `dose` / `days` → `duration` 语义偏差）已在 §2.1 行 58-60 标记，但未在 3.4.10 行关联声明；
   - **修复时需同步处理**：若只改 3.4.2 入口的 `PrescriptionCheckRequest.PrescriptionDrug` 而忽略 3.4.10 输出，复用同一类型的字段偏差会延续到 3.4.10 的 Mock 实现与前端 UI 渲染。

5. **§8.2 数值语义风格不一致**（与 §2.1 中 3.4.11 / 3.4.12 联动）：
   - 3.4.11 输出 `priority` 采用 P1 / P2 / P3 三档（需求 §3.4.11 行 1011）；
   - 3.4.12 输出 `workload_score` 采用 0-100 数值（需求 §3.4.12 行 1040）；
   - 3.4.4 / 3.4.5 / 3.4.6 / 3.4.13 输出 `confidence` 均采用 0-100 数值（需求 §3.4.4 行 898、§3.4.5 行 919、§3.4.6 行 935、§3.4.13 行 1063）；
   - OOD §8.2 中 3.4.11 输出 `priority: Integer`（数值 1-5），与需求 P1/P2/P3 语义错位；其他能力 `confidence: Double` 与需求 0-100 一致；
   - **Phase 1+ 落实时需统一约束**：建议在 §8.2 补充"数值语义风格约定"段（priority 用枚举 vs 数值 vs 0-100 分数的选型标准）。

6. **§2.2 `PatientInfo` 与 §3.4 Mock 占位规则的耦合（与 §2.1 / §2.6 联动）**：
   - §2.2 新增的 `name` 字段（OOD §8.2 行 936）按 §3.4 Mock 占位规则行 629 的"无任何可空标记的字段视为必填"原则成为必填字段；
   - §2.2 新增的 `allergyDetails` 结构化字段（OOD §8.2 行 941）在 Mock 阶段的占位规则需特别声明——按"嵌套 DTO：递归填充上述规则"（行 628）会触发 `AllergyDetail` 4 子字段（`allergen` / `reactionType` / `severity` / `occurredAt`）的占位填充，与"缺省时回退到 `allergies`"的语义（行 941）冲突；
   - **修复时需同步处理**：回收 `name` 字段（越界）并明确 `allergyDetails` 在 Phase 0 是否保留（建议回收，归 Phase 1+）。

---

## 4. 修复指引方向（仅定位，不展开修复步骤）

修复者在收到本报告后，建议按以下优先级处理（**四档体系：高（关键遗漏）→ 中偏高（影响面广）→ 中（局部问题）→ 低（表述修正）**）：

1. **【高优先级】首先解决 §8.2 AI 13 项 DTO 与需求 3.4.x 的对齐问题（2.1）**：这是影响最广泛的问题，13 项 AI 能力的 DTO 字段定义决定了 Phase 2-5 全部 AI 能力的契约形态。建议在编码 `MockAiService` 与 `AiService` 接口前先完成字段对齐。
   - **Phase 0 必须对齐的能力**（路线图 §0 阅读指引行 18 明确"核心三件套（3.4.1/3.4.2/3.4.3）"作为 Phase 0 AI 能力的优先序列，建议优先对齐核心必填字段，影响 Phase 0 前端独立开发）：
     - **3.4.1 智能分诊**：必填 `session_id` / `additional_responses` / `patient_id` / `rule_version` / `rule_set_id` 输入 + `reason` / `matched_rules` / `recommended_departments`（数组 0-3 项）输出；
     - **3.4.2 AI 处方审核**：必填 `prescription_id` 输入 + `risk_level` / `interactions` / `alerts` / `suggestions` 输出 + `PrescriptionDrug` 字段对齐（`drug_id` / `route` 必填 + `dose` / `duration` 语义对齐）；
     - **3.4.3 AI 病历生成**：必填 `patient_id` / `encounter_id`（可选）/ `stream`（bool，可选）/ `dialogue_text` 字符数 50-10000 约束输入 + 8 字段结构化输出（`chief_complaint` / `symptom_description` / `present_illness` / `past_history` / `physical_exam` / `preliminary_diagnosis` / `treatment_plan` / `missing_fields`）；**流式/非流式架构选型必须在 Phase 0 中预留**（否则 Phase 2+ 流式输出需重构响应通道）；
   - **Phase 1+ 启动前冻结的能力**（路线图 §0.2 行 44-51 推荐补齐项"AI 能力模块 Mock 占位（可跨阶段持续完善，不阻塞 Phase 0 骨架验收）"覆盖范围；保留 Mock 占位即可，待对应阶段启动前再细化字段）：
     - **3.4.4 / 3.4.9 / 3.4.13**：仅需修正 `PatientInfo.allergies` → `allergy_history` 字段语义（已在 §2.2 标记），回收 `PatientInfo.name` 与 `PatientInfo.allergyDetails`（已在 §2.2 标记），其余主结构（`ConditionInfo` / `DiagnosisResponse` / `ExamResultSummary`）已对齐；3.4.13 自身字段约束（`discussion_text` ≥ 50 字符、`summary` 0-5 项每项 ≤ 100 字符、`encounter_id` 必填、`DISCUSS_AI_OUTPUT_INCOMPLETE` 错误码）补全后即可视为对齐；
     - **3.4.10**：仅需在 3.4.10 行关联声明 `PrescriptionDraft.drugs` 复用 `PrescriptionDrug` 类型（已在 §3 第 4 项标记），主结构（`prescription_draft` / `dose_warnings` / `allergy_warnings` / `disclaimer_required`）已对齐；
     - **3.4.5 / 3.4.6 / 3.4.7 / 3.4.8 / 3.4.11 / 3.4.12**：核心必填字段在 Phase 1+ 启动前冻结时补全，Phase 0 保留 Mock 占位即可。

2. **【高优先级】其次处理 Phase 0 范围越界问题（2.3）**：识别路线图 §0.2 "骨架必备" 与 §0.4 "明确不包含" 的边界，按 §2.3 "回收对象 vs 保留对象清单"回收 OOD 中超出"骨架必备"粒度的内容，降低 Phase 0 工期压力。**精确化边界以避免误删 Phase 0 必须保留的实体骨架**：
   - 保留：实体类骨架（含主键 + 基本字段 + 简单的 `List<Long>` ID 引用字段，**不**使用 `@ManyToMany` 注解）、`DataPermissionEvaluator` 扩展点接口本身、`AiService` 接口骨架 + `MockAiService` 占位实现；
   - 回收：完整 JPA 关联映射（含 `user_role` / `user_post` / `post_function` 关联表 schema）、数据范围枚举冻结、`MockAiService` / `FallbackAiService` 装配优先级、跨模块事件模式示例、ApiClient/AuthStore 完整实现、完整 springdoc-openapi 生产级配置、TypeScript 类型同步迁移路径。

3. **【高优先级】同步修订 §8.2 行 907 的对齐范围声明（2.4）**：与 2.1 的实际状态保持一致；声明范围应明确"3.4.4 / 3.4.9 / 3.4.10 / 3.4.13 主结构已对齐，字段约束以 §2.1 / §2.2 标记为准"。

4. **【高优先级】修复 `PatientInfo` 字段语义（2.2）**：与 2.1 同步处理（影响 5 项 AI 能力的 `PatientInfo` 共用）。重点处理：(a) `allergies` → `allergy_history` 字段语义；(b) 回收 `name` 字段（超出需求 AI 契约字段集）；(c) 回收 `allergyDetails` 结构化字段（属 Phase 1+ 健康档案基础版范围，路线图 §1.2 行 86）。

5. **【中偏高优先级】DTO 字段命名约定（§2.7）**：与 §2.1 协同处理（修复成本低），建议在 `application.yml` 增加 `spring.jackson.property-naming-strategy: SNAKE_CASE` 全局配置；与 §8.3 行 1130 的"Phase 1+ 引入 openapi-generator"协同——若 Phase 0 即统一 snake_case，Phase 1+ 自动生成的 TypeScript 类型也保持一致。

6. **【中偏高优先级】Mock 占位规则风险显式声明（§2.6）**：在 §3.4 Mock 占位约定段落补充强语义枚举的处理建议（如"Mock 阶段返回枚举的中间值或业务中性值"，避免依赖 `values()[0]`），并对 `audit_result` / `risk_level` / `severity` / `urgency_hint` 等强语义枚举显式声明目标占位值。

7. **【中优先级】术语与表述修正（2.5、2.8、2.9）**：术语误用与冗余表述，修复成本低；§2.5 修复时联动修改 §7 决策表对比维度。

8. **【低优先级】其余问题（§2.10—§2.14）**：作为 Phase 0 实施时的参考。

---

## 5. 范围说明

**本诊断范围内**：
- §8.2 AI 13 项 DTO 字段定义与需求 3.4.x 的对齐状态；
- Phase 0 OOD 是否符合路线图 §0.2 "骨架必备" + §0.4 "明确不包含"清单；
- OOD 文档内部定义矛盾与术语准确性；
- Mock 占位规则的语义风险。

**本诊断范围外**（未深入展开）：
- 后端实现细节（如 BaseEntity 软删除 SQL 在不同 DB 上的方言差异、Spring Boot 自动配置默认值等）：修复者在编码时查阅 Hibernate / Spring Boot 文档是正常编码活动；
- 前端 UI 实现细节（如 Vue 3 + Vite 组件库的具体选择、Pinia store 的 state 结构等）：属于 Phase 1+ 实施细节；
- AI 模型的真实接入（Phase 5 范围）：OOD 仅需定义契约骨架；
- 13 项 AI 能力的业务融合逻辑（属 7.2 业务融合验收）：OOD 不涉及业务融合层；
- §10 CI 流水线阶段切换的详细脚本：属于实施细节；
- §9.2 中 `@EntityScan` / `@EnableJpaRepositories` / `@SpringBootApplication` 的具体扫描行为验证：属于 Spring Boot 自动配置的常规验证。

---

## 6. 诊断条目复核与处置总表

> 本表是经对 `01_requirement.md` 与 `03_roadmap.md` 二次交叉核对后，对本报告所有诊断条目（§2.1—§2.14 + §3 跨章节观察）给出的最终裁定与处置结果，与 `04_ood_phase0.md` 的实际修复一一对应。

**处置分类**：
- **已修复（必然成立）**：OOD 中的具体错位已按需求文档或路线图直接修正
- **保留并澄清（过渡期默认行为 / 文档粒度偏细）**：保留 OOD 原设计，但通过措辞或补充说明消除歧义
- **不采纳（不成立）**：原始指控与 `01_requirement.md` / `03_roadmap.md` 不构成客观矛盾，报告中已通过"复核补充"段作纠偏说明

| 报告编号 | 问题简述 | 原报告定级 | 实际裁定 | 处置方式 | 处置位置 |
|---------|---------|-----------|---------|---------|---------|
| 2.1 | §8.2 AI 13 项 DTO 字段定义与需求 3.4.x 多处不符 | 高 | **成立** | 已修复 | `04_ood_phase0.md` §8.2 L907-1276，13 项 DTO 字段结构全部收敛到需求契约 |
| 2.2 | `PatientInfo` 字段语义与需求不符（`allergies` vs `allergy_history` / `allergyDetails` / `name`） | 高 | **成立**（`allergy_history` 主字段 + `name` 越界） / **保留并澄清**（`allergyDetails`） | 双重处置 | `04_ood_phase0.md` §8.2 L953-965：主字段改为 `allergyHistory: String`、删除 `name`、`allergyDetails` 保留但标注为 `@Nullable` 过渡期可选扩展容器并明示回退语义 |
| 2.3 | 路线图 §0.4 被多项 OOD 定义越界（JPA 映射 / 跨模块事件 / ApiClient/AuthStore / springdoc） | 高 | **部分成立**（粒度偏细，非客观违反） / **保留并澄清** | 保留 OOD 原设计，仅在 `ood_report.md` 中作定性纠偏 | `ood_report.md` L20-23 复核补充第 2 条；`04_ood_phase0.md` §3.3、§8.4、§3.5 仅做表述收敛 |
| 2.4 | §8.2 行 907 对齐范围声明与实际不符 | 高 | **成立** | 已修复 | `04_ood_phase0.md` §8.2 L906-907 改写声明，明确"13 项字段结构按需求收敛 + Phase 0 仍只 Mock 占位" |
| 2.5 | "Vite workspace" 表述不准确 | 中 | **成立** | 已修复 | `04_ood_phase0.md` L16 / L44 / L423 / L852 / L862 / L1441 全部改为"基于 Vite + npm workspaces" |
| 2.6 | §3.4 Mock 占位规则对强语义枚举字段有语义风险 | 中偏高 | **成立** | 已修复 | `04_ood_phase0.md` §3.4 L627 改为"优先使用业务中性占位值（如 `LOW` / `PASS` / `MEDIUM`），未定义时回退到 `values()[0]`" |
| 2.7 | DTO 字段命名（camelCase vs snake_case）未声明 JSON 序列化约定 | 中偏高 | **成立** | 已修复 | `04_ood_phase0.md` §8.2 L906 增补统一声明（Jackson `PropertyNamingStrategies.SNAKE_CASE`）；§8.3 L1286 同步声明 TypeScript / OpenAPI 字段统一 snake_case |
| 2.8 | §4.5 SecurityConfigPhase0 中 `requestMatchers("/api/ping").permitAll()` 与 `anyRequest().permitAll()` 冗余 | 中 | **成立** | 已修复 | `04_ood_phase0.md` §4.5 L770 删除冗余 `requestMatchers` 行 |
| 2.9 | §4.5 Phase 1 profile 切换描述冗余 | 中 | **不成立** | 不采纳 | `ood_report.md` L23 复核补充第 3 条将本条降为表述优化观察；`04_ood_phase0.md` §4.5 L778 同步做精确化改写（"任意不包含 `phase0` 的 profile 组合，最常见为 `dev`"） |
| 2.10 | §3.4 FallbackAiService 自引用排除逻辑隐含假设未明示 | 低 | **部分成立**（说明可更明确） | 保留并澄清 | 不修改 OOD，视为实施阶段编码补全 |
| 2.11 | §7 设计决策表行 862 与 §3.4 装配策略描述颗粒度不一致 | 低 | **部分成立** | 保留 | 视为决策溯源可优化项，不阻塞 |
| 2.12 | §9.1 `application.yml` 完整示例缺失 | 低 | **成立** | 已修复 | `04_ood_phase0.md` §9.1 L1367-1378 增补最小 `application.yml` 示例（`spring.application.name` / `spring.profiles.active` / `server.port`） |
| 2.13 | §7 行 867 "CI 分阶段构建策略" 与 §10 CI 段一致性 | 低 | **不成立** | 不采纳 | `ood_report.md` L23 复核补充第 3 条将本条降为表述优化观察；OOD 不修改 |
| 2.14 | §9.2 启动类位置与 `@SpringBootApplication` 注解参数 | 低 | **成立**（部分） | 已修复 | `04_ood_phase0.md` §9.2 L1425 明确"启动类放在 `com.aimedical` 根包下"；代码示例缺 `package` 声明记为后续可补 |
| 3-1 | §3.4 Mock 占位规则与 §8.2 DTO 字段缺失的相互作用 | 联动观察 | **成立** | 联动修复 | 随 §2.1 + §2.6 一并修复；`04_ood_phase0.md` §8.2 DTO 字段补全 + §3.4 L627 枚举规则收敛 |
| 3-2 | §2.2 模块依赖图与 §3.3 权限模型 JPA 映射的越界关系 | 联动观察 | **不成立**（粒度问题非边界违反） | 保留 | `ood_report.md` L22 复核补充第 2 条作定性纠偏；OOD §3.3 不修改 |
| 3-3 | §8.2 DTO 与 §3.4 AiService 装配策略的耦合 | 联动观察 | **成立**（方法数 13 一致已对齐） | 联动确认 | 随 §2.1 + §2.4 修复一并确认一致性 |
| 3-4 | 3.4.2 / 3.4.10 `PrescriptionDrug` 类型复用关联 | 联动观察 | **成立** | 已修复 | `04_ood_phase0.md` §8.2 L945-951 修正后的 `PrescriptionDrug`（`drug_id` / `dose` / `duration` / `route`）被 `PrescriptionDraft.drugs` 复用（L1155-1159），类型复用关系已对齐 |
| 3-5 | §8.2 数值语义风格不一致（priority P1/P2/P3 vs 1-5） | 联动观察 | **成立** | 已修复 | `04_ood_phase0.md` §8.2 L1196 `ExecutionOrderItem.priority // 优先级（P1/P2/P3）` 替换原 1-5 数值；其他能力 `confidence: Double [0,100]` 与需求一致 |
| 3-6 | `PatientInfo` 与 §3.4 Mock 占位规则的耦合 | 联动观察 | **成立** | 联动修复 | 随 §2.2 + §2.6 一并修复；`04_ood_phase0.md` §8.2 L953-965 + §3.4 L627 |

---

**统计**：
- 诊断条目总数：14 项（§2.1—§2.14）+ 6 项联动观察（§3-1—§3-6）= 20 项
- **已修复（必然成立）**：14 项（§2.1 / §2.2 主字段与 `name` / §2.4 / §2.5 / §2.6 / §2.7 / §2.8 / §2.12 / §2.14 / §3-1 / §3-3 / §3-4 / §3-5 / §3-6）
- **保留并澄清（过渡期默认行为 / 粒度偏细）**：4 项（§2.2 `allergyDetails` / §2.3 / §2.10 / §2.11）
- **不采纳（不成立）**：2 项（§2.9 / §2.13）
- **修复落地**：`04_ood_phase0.md`（核心修复） + `ood_report.md`（复核补充 + 本表）
- **遗留非阻断项**：1 项（§2.14 `Application.java` 示例缺 `package com.aimedical;` 声明，编码时补全即可）

---

> 报告结束
