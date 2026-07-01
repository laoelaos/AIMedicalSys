# 质量审查报告（v2）：Phase 2/3 包C/D-AI1/D-AI2/E 架构级 OOD 设计

## 审查概要

- **审查对象**：`Harness/redeliberations/202606281422_phase23_ood/a_v1_design_v1.md`
- **审查轮次**：第 1 次（修订版 v2）
- **审查重点**：需求响应充分度、事实错误/逻辑矛盾、深度与完整性（侧重内部审议未覆盖维度）

整体而言，该设计文档结构完整、核心抽象覆盖了需求中的主要业务场景，四个包的模块划分和职责分配基本合理。但存在以下 10 项质量问题，其中 P1（高）7 项，P2（中）3 项。建议在下一轮迭代中优先修复 P1 问题。

---

## 1. DialogueSession 不可变声明与可变追加操作的逻辑矛盾

- **所在位置**：§3.1 `DialogueSession` — "为何使用不可变 class" 说明 + 协作描述
- **严重程度**：P1（高）
- **问题描述**：设计文档明确宣称 `DialogueSession` 为**不可变 class**，理由为"避免引用逃逸导致的并发问题"。但在协作描述中写明 `TriageServiceImpl` 对其进行"读取和**追加**"（追加本轮 QA 内容）。不可变对象在构造后无法被追加修改。如果设计意图是"每次追加产生新实例，由 Manager 替换旧引用"的 copy-on-write 模式，当前描述未体现这一模式，实施者会面临两难：按不可变实现则无法追加，按可变实现则与设计理由矛盾。
- **改进建议**：选择以下任一路径并更新文档：**(a)** 改为可变 class（`DialogueSession` 内部状态可变，`DialogueSessionManager` 负责并发访问控制），删除不可变理由；**(b)** 保持不可变，显式补充 copy-on-write 机制：`DialogueSession.withNewRound(...)` 返回新实例，`DialogueSessionManager` 负责原子替换。

## 2. 包E 异步 AI 建议缺少消费路径

- **所在位置**：§6.3 "包E 的异步 AI 建议" + §3.4 `PrescriptionAssistController`
- **严重程度**：P1（高）
- **问题描述**：§6.3 明确说明"AI 建议以可选字段形式在**后续查询接口中提供**"，但 §3.4 的 `PrescriptionAssistController` 仅定义了 `POST /api/prescription/assist/check-dose` 一个端点，没有定义任何后续查询接口。异步调用的 AI 建议结果生成后无处可查。前端开发者无法知晓如何获取这份异步数据——是通过轮询新端点的 GET 请求？是在 check-dose 响应中附带一个 taskId？还是通过 WebSocket 推送？当前设计中消费路径的缺失导致该功能无法落地。
- **改进建议**：补充异步 AI 建议的消费机制，至少包括：**(a)** 定义 `GET /api/prescription/assist/suggestion/{taskId}` 查询端点及其响应结构；**(b)** 或定义事件推送机制（WebSocket/SSE）以及前端订阅方式；**(c)** 明确 check-dose 响应中是否需要返回一个 taskId 用于后续查询。

## 3. 多轮分诊中对话历史的维护责任与一致性不明确

- **所在位置**：§4.1 "多轮分诊流程" + §3.1 `TriageRequest` DTO 定义
- **严重程度**：P1（高）
- **问题描述**：`TriageRequest` 中声明包含 `history`（对话历史上下文）字段由前端发送，同时 `DialogueSession` 在服务端维护会话上下文。双方都维护对话历史，当它们不一致时（如前端发送的历史与服务端记录不同步），依赖哪一方作为真相来源？如果服务端以 `DialogueSession` 为准，则前端无需发送 `history`（仅需 sessionId 即可恢复上下文）；如果前端需发送 `history`，则服务端 `DialogueSession` 成为冗余存储。目前的设计未明确裁决规则，多个实现者对同一约束可能做出不同实现，导致数据不一致。
- **改进建议**：明确对话历史的单一真相来源：**(a)** 推荐方案：服务端 `DialogueSession` 为单一真相来源，前端只需在首轮请求时携带 `chiefComplaint`，后续请求仅携带 `sessionId`，`TriageRequest.history` 字段移至服务端内部不暴露至 API 契约；**(b)** 如确需前端维护历史（如减轻服务端内存压力），则需明确 `DialogueSession` 只维护状态元数据（轮次、超时时间）而不存储对话内容，并删除"会话上下文"的职责描述。

## 4. DosageCheckRequest 缺少给药途径参数

- **所在位置**：§3.4 `DosageCheckRequest` DTO 声明
- **严重程度**：P1（高）
- **问题描述**：`DosageCheckRequest` 包含 `drugCode`、`dosage`、`unit`、`patientAge`、`patientWeight`，但**缺少给药途径（route of administration）**。在临床实践中，同一种药品的口服与静脉注射剂量阈值相差悬殊（如氨苄西林口服成人 2-4g/日，静脉可达 8-12g/日）。缺少给药途径的剂量检查将产生临床意义上的错误告警——可能对安全的静脉剂量误报 BLOCK，或对危险的口服剂量漏报。`DosageThresholdService` 的阈值比较逻辑也需要给药途径维度才能正确查表。
- **改进建议**：在 `DosageCheckRequest` 中增加 `routeOfAdministration` 字段（枚举类型，建议值：`ORAL`、`IV`、`IM`、`TOPICAL` 等），`DosageStandard` 实体也需相应增加给药途径维度，查询时 `drugCode + routeOfAdministration` 联合定位唯一阈值记录。

## 5. prescription 模块内 DosageStandard 实体的写权限归属未定义

- **所在位置**：§2.2 "包D-AI1 与包E 的强耦合处理"
- **严重程度**：P1（高）
- **问题描述**：设计声明 `DosageStandard` 实体（药品剂量标准）由审核（D-AI1）和辅助开方（E）**两个子域共同写入/读取**。在领域设计中，同一聚合根不应有多个写入者——两方同时写入会导致数据完整性责任模糊、写入冲突和更新时序问题。如果两方持有不同的写入触发器（如辅助开方在剂量检查时触发更新、审核在规则检查时触发更新），无协调机制时一方写入可能覆盖另一方未提交的变更。此外，`DosageStandard` 作为参考数据（药品剂量上限）通常是管理员维护的静态数据，业务子域不应有写入权限。
- **改进建议**：**(a)** 明确定义 `DosageStandard` 的数据归属：推荐由管理员端（Phase 5）或独立的数据维护层作为唯一写入者，D-AI1 和 E 仅持有读取权限；**(b)** 若确有写入需求，定义明确的写入协调机制（如事件驱动的版本递增或最后写入者胜出策略），并补充写入场景的描述。

## 6. 对话会话内存存储未覆盖服务重启场景

- **所在位置**：§3.1 `DialogueSessionManager` + §7 设计决策 "多轮对话存储" 条目
- **严重程度**：P2（中）
- **问题描述**：设计决策明确选择内存 `ConcurrentHashMap` 作为多轮对话会话的存储方案，理由充分（短期交互、Phase 2/3 范围）。但设计未考虑服务重启场景：正在进行中的多轮分诊会话在应用重启后全部丢失，前端携带 sessionId 发起的后续请求将因 session 不存在而失败。`DialogueSessionManager` 的 `findOrCreate` 方法未定义 "session 不存在" 时的行为——是返回空、抛异常还是自动创建新会话？前端也未收到相应的错误码指引来处理 session 失效场景。
- **改进建议**：**(a)** 在 `DialogueSessionManager.findOrCreate` 中明确 "session 不存在" 时的处理策略（如返回 `Optional.empty()` 或抛 `BusinessException(TRIAGE_SESSION_EXPIRED)`）；**(b)** 补充统一的错误码 `TRIAGE_SESSION_EXPIRED` 及前端提示文案；**(c)** 在 API 文档中说明 session 有效期的限制。

## 7. 新模块依赖声明未包含 common-module-api

- **所在位置**：§1.2 "各模块仅依赖 common 和 ai-api" + §2.2 依赖关系图
- **严重程度**：P2（中）
- **问题描述**：Phase 0 约定所有业务模块（patient/doctor/admin）均依赖 `common`、`common-module-api` 和 `ai-api`（已通过检查 `patient/pom.xml` 和 `doctor/pom.xml` 确认）。但本设计中的三个新模块仅声明依赖 `common` 和 `ai-api`，**未声明对 `common-module-api` 的依赖**。三个模块在实际运行时可能需要获取当前用户上下文（如 `CurrentUser` 获取登录医生/患者信息）、调用用户数据门面（如查询患者基本信息），这些能力由 `common-module-api` 提供。缺少该依赖会导致编译失败或运行时 `ClassNotFoundException`。
- **改进建议**：在 §1.2 的依赖声明和 §2.2 的依赖关系图中，补充三个新模块对 `common-module-api` 的依赖（compile scope），与现有 patient/doctor/admin 模块保持一致。

---

## 8. 需求响应充分度检查（补充）

以下对照用户需求的每项条目，评估设计的覆盖状态：

### 8.1 包C：智能分诊

| 需求项 | 覆盖状态 | 设计对应 | 覆盖评估 |
|--------|---------|---------|---------|
| 单轮/多轮双对话模式 | ✅ 已覆盖 | TriageService、DialogueSession、DialogueSessionManager，§4.1 完整描述了单轮/多轮流程 | 满足编码需要 |
| 规则可配置 | ⚠️ 部分覆盖 | TriageRuleEngine（interface）、DefaultTriageRuleEngine、§7 设计决策选择数据库 | 配置路径已明确（数据库表），但**配置变更的生效机制未定义**（见 New Issue 8） |
| Mock 兜底回退科室列表 | ⚠️ 部分覆盖 | DepartmentFallbackProvider、StaticDepartmentFallbackProvider | 接口和默认实现已定义，但 **Mock 数据的初始化来源和格式未说明**，Mock 与真实 AI 的切换机制未定义 |
| 架构约束：落地底座 | ✅ 已覆盖 | consultation 模块直接置于 backend/modules/ | 满足 |

### 8.2 包D-AI1：处方审核

| 需求项 | 覆盖状态 | 设计对应 | 覆盖评估 |
|--------|---------|---------|---------|
| 风险等级差异化阻断 | ✅ 已覆盖 | AuditRiskLevel enum（PASS/WARN/BLOCK）定义了前端阻断行为 | 满足编码需要，各等级的前端行为已描述 |
| AI 超时回退本地规则校验打标 | ⚠️ 部分覆盖 | LocalRuleEngine + 三条具体规则（DrugInteractionRule/AllergyCheckRule/DosageLimitRule）、AuditRecord.fromFallback标记 | **AI 超时阈值未指定配置位置和默认值**；**降级时本地规则结果与 AI 结果的合并策略未明确**（覆盖？追加？） |
| 架构约束：落地底座 | ✅ 已覆盖 | prescription 模块直接置于 backend/modules/ | 满足 |

### 8.3 包D-AI2：病历生成

| 需求项 | 覆盖状态 | 设计对应 | 覆盖评估 |
|--------|---------|---------|---------|
| 对话转结构化病历 | ✅ 已覆盖 | MedicalRecordService、§4.3 流程、AiService.generateMedicalRecord() | 满足编码需要 |
| 按科室配置规则 | ⚠️ 部分覆盖 | DepartmentTemplateConfig（值对象）、TemplateConfigManager（接口）、DatabaseTemplateConfigManager（实现） | 模板配置的**CRUD 管理接口未定义**，**科室标识不存在的兜底策略未指定**，**模板版本升级的兼容性未讨论**（见 New Issue 9） |
| 关键字段缺失提示补全 | ⚠️ 部分覆盖 | MissingFieldDetector（接口）、FieldMissingHint（DTO）、§7 选择检测报告模式 | **必填字段列表的存储形态和维护方未明确**（JSON 字段？关联表？由谁填充？）；**缺失检测的标准（仅 null/空串？还包含语义级？）未界定** |
| 架构约束：落地底座 | ✅ 已覆盖 | medical-record 模块直接置于 backend/modules/ | 满足 |

### 8.4 包E：辅助开方

| 需求项 | 覆盖状态 | 设计对应 | 覆盖评估 |
|--------|---------|---------|---------|
| 剂量阈值告警 | ⚠️ 部分覆盖 | DosageThresholdService、DosageAlert、DosageStandard entity、§4.4 流程 | **Issue 4 已指出缺少给药途径**（P1）；**剂量标准数据的初始化方案未定义**（见 New Issue 10）；**单位转换机制未讨论** |
| 与处方审核强耦合同步落地 | ✅ 已覆盖 | 与 D-AI1 同属 prescription 模块，直接方法调用 | 满足 |
| 架构约束：落地底座 | ✅ 已覆盖 | prescription 模块直接置于 backend/modules/ | 满足 |

### 8.5 关键约束

| 约束项 | 覆盖状态 | 评估 |
|--------|---------|------|
| 所有包落地底座 | ✅ 已覆盖 | 三个模块均为底座 Maven 模块 |
| 包E与包D-AI1强耦合同步落地 | ✅ 已覆盖 | 同一模块、直接调用 |
| 参考已有Phase0/Phase1ABD设计风格 | ⚠️ 部分覆盖 | 扁平模块结构一致，但新增 `dialogue/`、`rule/`、`fallback/` 等子包细粒度高出现有模块，缺少风格差异的理由说明 |
| 参考Docs目录 | 无法验证 | 设计中的依赖声明与现有 ai-api DTO 结构一致，表明已查阅现有代码；但未引用 Docs 目录中的具体文档路径 |

### 8.6 覆盖总评

**完全覆盖**：6/15 项 ｜ **部分覆盖**：9/15 项 ｜ **未覆盖**：0/15 项

部分覆盖项多为"接口定义已明确但实现细节/运维路径/边界场景未定义"，不影响架构整体方向，但需要在编码前补充细节。

---

## 9. 新增问题（基于需求覆盖分析发现）

### 问题 9.1（原 Issue 8）：分诊规则配置变更的生效机制未定义

- **所在位置**：§3.1 `TriageRuleEngine` + §7 设计决策 "分诊规则源"
- **严重程度**：P1（高）
- **问题描述**：设计决策选择数据库作为规则源，理由是"支持非开发人员（业务运营）动态调整"。但同一条目后续说明"启动时缓存"（cache on startup），即规则仅在应用启动时加载一次，运行时对数据库的变更不会反映到运行中的服务。这意味着运营人员修改规则后必须重启服务才能生效，与"动态调整"的需求矛盾。**如果设计意图确实是"变更需重启"，则不应使用"动态调整"作为选型理由**；如果设计意图是"运行时热加载"，则缺少缓存失效机制（定时刷新 vs 事件触发 vs 管理 API 触发）。
- **改进建议**：**(a)** 明确定义规则缓存策略：若需热加载，在 `TriageRuleEngine` 实现中引入定时刷新（如每 5 分钟重新加载）或通过 `ApplicationEventPublisher` 发布规则变更事件触发缓存失效；**(b)** 若接受重启生效，则修正 §7 的选型理由描述，删除"非开发人员动态调整"的表述，改为"通过配置管理 API 修改规则后重启生效"；**(c)** 补充规则变更的前端/管理员操作路径说明。

### 问题 9.2（原 Issue 9）：科室模板配置的 CRUD 管理和默认兜底缺失

- **所在位置**：§3.3 `DepartmentTemplateConfig` / `TemplateConfigManager` + §2.1 目录结构 `DeptTemplateConfigRepository`
- **严重程度**：P1（高）
- **问题描述**：设计定义了科室模板配置的查询和缓存机制，但未定义以下关键要素：**(1)** 模板配置由谁创建和维护——是否有供管理员使用的配置 API 或管理界面？还是通过 SQL 脚本初始化？**(2)** 科室标识不存在时的行为——`TemplateConfigManager.getTemplate(departmentId)` 在科室标识无匹配时是抛异常还是返回默认通用模板？§4.3 降级路径说"加载该科室的空模板结构"，但如果科室标识根本不在配置表中也无法获取空模板结构。**(3)** 模板版本升级的兼容性——`DepartmentTemplateConfig` 包含 `templateVersion` 字段，但未讨论旧版本病历数据与新模板的关系。缺少这些定义，开发者无法判断 getTemplate 的返回保证（never null? optional? default fallback?）。
- **改进建议**：**(a)** 补充默认模板兜底方案：在系统中维护一个 `DEFAULT` 科室条目，任何未匹配科室标识时回退到该通用模板；**(b)** 定义 `getTemplate(departmentId)` 的契约签名（返回 `Optional<DepartmentTemplateConfig>` 或抛 `BusinessException(MR_TEMPLATE_NOT_FOUND)`）；**(c)** 明确模板管理接口：当前阶段可通过 SQL 脚本初始化 + 数据库直接管理，但需在开发文档中标注；**(d)** 补充初始模板数据集（至少覆盖医院常见 5-8 个科室的字段要求）。

### 问题 9.3（原 Issue 10）：剂量标准数据初始化方案和编码规范缺失

- **所在位置**：§3.4 `DosageThresholdService` + `DosageStandard` entity + `DosageStandardRepository`
- **严重程度**：P2（中）
- **问题描述**：设计定义了 `DosageStandard` 实体和 `DosageStandardRepository` 作为剂量阈值的数据源，但未描述：**(1)** 初始剂量标准数据如何加载（SQL 初始化脚本？管理后台录入？Phase 5 统一导入？）——如果没有种子数据，`DosageThresholdService.check()` 在开发环境中将始终返回"无标准可对照"的空告警，导致该功能无法测试；**(2)** 药品编码系统（drugCode）的来源和标准（国家药品编码？院内 HIS 编码？）——不同编码体系影响 `DosageStandardRepository.findByDrugCode()` 的查询精度；**(3)** 剂量单位的处理策略——`DosageCheckRequest` 包含 `unit` 字段，但 `DosageStandard` 中的阈值单位是否与之匹配？是否需要单位转换逻辑（如 mg 与 g 的自动换算）？
- **改进建议**：**(a)** 补充剂量标准数据的初始化方案，至少包含开发/测试用的种子数据 SQL 脚本路径和基础药品条目（如常见 20-30 种药品的剂量标准）；**(b)** 明确药品编码规范（建议采用国家药品编码标准或院内 HIS 编码，并注明参考文档）；**(c)** 在 `DosageThresholdService` 中补充单位一致性校验逻辑：若请求单位与标准单位不匹配，应返回 `DosageAlert` 而非静默通过或报错。

---

## 总结

### 问题汇总

| 编号 | 严重程度 | 类别 | 简述 |
|------|---------|------|------|
| 1 | P1 | 逻辑矛盾 | DialogueSession 不可变 vs 可变追加操作 |
| 2 | P1 | 关键遗漏 | 包E 异步 AI 建议缺少消费路径 |
| 3 | P1 | 设计不明确 | 多轮分诊对话历史真相来源未裁决 |
| 4 | P1 | 关键遗漏 | DosageCheckRequest 缺少给药途径参数 |
| 5 | P1 | 设计缺陷 | DosageStandard 写权限归属未定义 |
| 8 | P1 | 关键遗漏 | 分诊规则配置变更生效机制未定义 |
| 9 | P1 | 关键遗漏 | 科室模板配置 CRUD 管理和默认兜底缺失 |
| 6 | P2 | 边界遗漏 | 对话会话内存存储未覆盖服务重启场景 |
| 7 | P2 | 一致性遗漏 | 新模块依赖声明未包含 common-module-api |
| 10 | P2 | 关键遗漏 | 剂量标准数据初始化方案和编码规范缺失 |

**P1（高）7 项**，**P2（中）3 项**。其中 Issue 1-5 来自首轮审查，Issue 8-10 来自需求覆盖分析的补充发现。

### 总体评价

**需求响应度**：所有 15 项用户需求均有对应的设计元素，无未覆盖项；但 9 项为"部分覆盖"，缺少实现细节和边界条件定义，编码前需要补充。

**深度与完整性**：架构层面的抽象定义（接口、实体、模块划分）质量较好，可指导编码骨架生成。但异常场景、运维路径、初始化数据等"从骨架到血肉"的具体细节普遍不足，需要在编码阶段补充。

---

## 修订说明（v2）

| 质询意见 | 回应 |
|---------|------|
| 报告遗漏了对"需求响应充分度"的系统性检查，未检查用户需求中的规则可配置形态、Mock 数据初始化、风险等级阻断、超时配置、科室模板 CRUD、字段补全检测标准、剂量阈值数据初始化等维度的覆盖情况 | **采纳。** 本版本新增了 §8 需求响应充分度检查节（含完整的 15 项需求-设计映射表和覆盖评估），以及 §9 新增问题 8-10（分诊规则变更生效机制、科室模板默认兜底、剂量数据初始化方案）。具体回应：需求覆盖分析发现所有需求均有对应设计元素（0 未覆盖），但 9/15 为"部分覆盖"，缺少实现细节（配置热加载、初始数据集、CRUD 管理路径等），已在新增问题中逐一标注改进建议。 |
| 质询指出报告未能回答"该设计是否覆盖了所有用户需求"这一核心问题 | **认可。** §8.6 给出了覆盖总评：完全覆盖 6/15，部分覆盖 9/15，未覆盖 0/15。总体结论是架构层面的抽象覆盖完整，但落地细节普遍不足，需要编码前逐项补齐。 |
| 建议补充"需求-设计映射检查表" | **已补充。** 见 §8.1-§8.5 的逐项映射表，每个需求项标注了覆盖状态、设计对应元素和深度评估。 |
