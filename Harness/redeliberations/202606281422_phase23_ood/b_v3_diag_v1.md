# 质量审查报告：Phase 2/3 OOD 设计（v3）

## 审查概述

审查轮次：第 3 次（首轮质量审查）
审查对象：`a_v3_design_v1.md`（设计产出，已通过组件 A 两轮内部审议）
审查视角：需求响应充分度、整体深度和完整性、异常场景和边界条件
审查维度侧重：内部审议未充分覆盖的问题（避免重复验证已确认维度）

---

## 发现问题

### 问题 1：AuditRecord 落库在降级路径中被遗漏，与 §3.2 行为契约矛盾

- **所在位置**：§4.2 "处方审核场景"——降级流程描述
- **问题描述**：§4.2 降级路径只写了 `LocalRuleEngine.check()`，未提及 `AuditRecord` 持久化。但 §3.2 `PrescriptionAuditService` 的协作描述明确写有"写入 AuditRecord"。两处行为契约不一致：实现者将不清楚降级场景下是否需要将本地规则校验结果写入审核记录。若只写 `LocalRuleEngine.check()` 而不落库，则下文中"按医生/患者/处方单号三维追溯"的能力在 AI 超时场景下将产生数据缺口，影响合规审计。
- **严重程度**：严重
- **改进建议**：在 §4.2 降级流程中显式补充 AuditRecord 持久化步骤，或与正常路径合并为一个统一表述以消除歧义。同时确认：本地规则校验结果写入 `AuditRecord` 的 `riskLevel` 字段时，是否需额外标记 `fromFallback=true` 以区分 AI 结果与本地规则结果。

### 问题 2：分诊降级链路不完整——AI 为空且规则也为空时的行为未定义

- **所在位置**：§4.1 "智能分诊场景"
- **问题描述**：§4.1 定义了三条并行路径——正常（AI 返回推荐科室）、AI 空（规则匹配科室）、AI 不可用（静态兜底）。但第二条路径 `AI 空 → TriageRuleEngine.match()` 未定义 `TriageRuleEngine` 也无法匹配时的行为：规则库无匹配科室时，是返回空列表、抛异常、还是继续走 `DepartmentFallbackProvider`？§3.1 `TriageRuleEngine` 的职责描述也未覆盖该场景。这导致实现分支不完整，可能出现静默返回空列表的意外行为。
- **严重程度**：一般
- **改进建议**：将三条路径改为线性降级链：AI 无结果 → 规则匹配无结果 → FallbackProvider 兜底。在 §4.1 中明确 `TriageRuleEngine.match()` 返回空列表时，应继续调用 `DepartmentFallbackProvider.getFallbackDepartments()`；`TriageService` 的协作描述也需同步更新。

### 问题 3：§2.1 目录中存在 `AgeBandedDosage.java` 残留条目，与已确定的实现方案矛盾

- **所在位置**：§2.1 目录结构 `backend/common/src/main/java/com/aimedical/common/entity/AgeBandedDosage.java`
- **问题描述**：注释标注为"年龄/体重分级的剂量子条目（可选，与内联字段方案二选一）"。但 §8.4 和修订说明第 4 条已明确选择了内联字段方案（`DosageStandard` 直接增加 `ageMin/ageMax/weightMin/weightMax` 字段），且匹配优先级四级算法也完全基于内联字段设计。`AgeBandedDosage` 作为备选方案不应出现在目录中，会造成实现者困惑：究竟要不要创建这个子实体？它是否与内联方案共存？
- **严重程度**：轻微
- **改进建议**：从 §2.1 目录中移除 `AgeBandedDosage.java` 条目，或明确标注"本阶段不使用，Phase 5 扩展时按需引入"。

### 问题 4：TriageResponse DTO 字段结构未定义，无法支撑编码实现

- **所在位置**：§3.1 包C 核心抽象；§2.1 目录
- **问题描述**：目录中列出 `dto/TriageResponse.java`，但 §3.1 和 §4.1 均未定义其字段。与 Phase 1B OOD 中 `LoginResponse`、`UserInfoResponse` 等 DTO 明确列出字段结构的惯例不一致。实现者无法确定分诊响应中应包含哪些信息：推荐科室列表、置信度、sessionId、追问标识等。同样，`DialogueCreateRequest.java` 在目录中存在但文档中未出现。
- **严重程度**：一般
- **改进建议**：补充 `TriageResponse` 的字段定义，至少包含：`departments`（推荐科室列表）、`sessionId`（多轮场景）、`needFollowUp`（是否需要追问）、`followUpQuestion`（追问内容）、`confidence`（可选置信度标量）。同时明确 `DialogueCreateRequest` 是否为首轮请求 DTO，若是则说明其字段。

### 问题 5：剂量单位转换规则集未定义，实现面临随意假设风险

- **所在位置**：§8.3 "单位一致性校验"
- **问题描述**：文档仅以示例说明"mg↔g 可转换、mg↔ml 不可转换"，未定义完整的兼容单位分组表。实际药品剂量涉及 mcg、mg、g、IU、ml、L、% 等多种单位，哪些单位组内可转换、转换系数由谁维护、是否支持自定义换算规则，设计未给出。实现者需要自行定义兼容性分组和换算系数，不同开发者的假设可能不一致，导致运行时剂量校验结果不统一。
- **严重程度**：一般
- **改进建议**：补充剂量单位兼容分组的枚举或表格定义。推荐方案：定义一个 `DosageUnitGroup` 枚举（如 MASS_GROUP: mcg↔mg↔g、VOLUME_GROUP: ml↔L 等），各组内支持自动换算，跨组返回 `RX_ASSIST_UNIT_MISMATCH`。对于 IU 等非质量/体积单位可单独分组或定义为不可换算。转换系数以常量形式固化在 `DosageThresholdService` 中，后续可扩展为数据库可配置。

### 问题 6：MedicalRecord 实体字段未定义，病历结构化输出模型缺失

- **所在位置**：§3.3 包D-AI2；§2.1 目录 `entity/MedicalRecord.java`
- **问题描述**：`MedicalRecord` 作为包D-AI2 的核心持久化实体，目录中已列出文件但文档从未定义其字段结构。病历的 AI 结构化输出应包含哪些字段（主诉、现病史、既往史、体格检查、辅助检查、初步诊断、治疗意见等）未明确，`RecordGenerateRequest` 和 `RecordGenerateResponse` 的 DTO 字段也未定义。`DepartmentTemplateConfig` 中的"字段映射"和"必填字段"缺少字段标识符的枚举定义，使模板配置与 `MissingFieldDetector` 的差集比对逻辑无法精确实施。
- **严重程度**：一般
- **改进建议**：在 §3.3 中补充核心字段枚举或值对象定义（如 `MedicalRecordField` 枚举），列出病历结构化输出的顶层字段标识。`RecordGenerateRequest` 和 `RecordGenerateResponse` 补充字段定义。`DepartmentTemplateConfig.requiredFields` 的类型明确为 `List<MedicalRecordField>`。这与 §9.1 中 DEFAULT 模板字段列表形成呼应，使字段标识符与模板内容一致。

### 问题 7：规则/模板配置变更缺少审计溯源能力

- **所在位置**：§3.1 `TriageRuleEngine`（规则热加载）；§3.3 `TemplateConfigManager`（模板事件驱动刷新）；§9（模板更新）
- **问题描述**：文档定义了规则和模板的运行时热加载机制（Caffeine 定时刷新 + 事件驱动缓存失效），但未提及配置变更的审计记录。在医疗系统中，分诊规则和病历模板的每笔变更应记录：变更人、变更时间、变更内容（新旧值对比）。当前设计缺少此能力，如发生因规则配置错误导致分诊失误的场景，无法追溯。
- **严重程度**：一般
- **改进建议**：在规则变更和模板更新的事件处理中添加配置变更日志记录。推荐方案：(a) 定义 `ConfigChangeLog` 实体（归属 admin 模块或 common 模块）；(b) 在 `TemplateConfigChangeEvent` 和规则变更的处理链中写入审计日志；(c) 事件发布者提供新旧值语义。Phase 2 可先以日志文件方式实现，后续迁移至数据库审计表。

---

## 整体质量评价

设计文档在 v3 轮次已较好地修复了前两轮审议指出的 15 项问题，核心抽象和架构方向正确。但本审查发现在以下方面仍存在影响编码实施的缺口：

1. **行为契约一致性**（问题 1、2）：降级回退路径的关键行为存在逻辑缺口或前后矛盾，直接影响实现正确性
2. **接口完备性**（问题 4、6）：部分 DTO 和实体字段未定义，达不到"直接指导编码实现"的粒度要求
3. **文档准确性**（问题 3）：目录中存在与已选定方案矛盾的残留条目
4. **合规与可运维性**（问题 5、7）：单位转换规则和配置变更审计在医疗系统场景下属于必要约束

建议修复所有"严重"和"一般"级别问题后进入编码阶段，其中问题 3 为轻微级文档清理，可低优先级处理。
