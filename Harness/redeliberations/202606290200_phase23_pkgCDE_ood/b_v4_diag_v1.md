# 质量审查诊断报告 — a_v4_copy_from_v3.md（第 4 轮）

## 审查总览

- **审查范围**：Phase 2/3 包C/D-AI1/D-AI2/E 架构级 OOD 设计文档 v4（基于 v3 修订，整合 3 轮内部审议反馈）
- **审查维度**：需求响应充分度、事实错误与逻辑矛盾、深度与完整性（侧重内部审议未充分覆盖的维度）
- **审查结论**：产出质量较高，前 3 轮内部审议已覆盖大量技术细节问题并完成修复。本节仍存在若干尚未解决的结构性缺口和完整性不足问题，以下详述。

---

## 1. 需求响应充分度问题

### 问题 1：RegistrationEvent sessionId 跨模块传播路径在架构层面未闭合

- **问题描述**：产出在 §2.2 及 §1.1a 中定义了 RegistrationEvent 携带 sessionId 字段并由 registration 模块消费，设计文案描述为"由 registration 模块从分诊上下文获取并填充"。但 consultation 模块与 registration 模块是独立模块（按 §2.2 依赖规则不允许互相依赖），产出未定义 sessionId 从 consultation 模块的 DialogueSession 传递到 registration 模块的具体路径。前端在分诊流程结束后进入挂号流程时，分诊 sessionId 如何到达挂号模块未被覆盖。这是一个跨模块、跨流程的数据传递缺口——sessionId 的传播路径在架构层面不闭合，RegistrationEvent-driven finalDepartmentId 自动写入路径因此无法在实现时落地。

- **所在位置**：§1.1a 外部依赖表（registration 模块行，"sessionId 填充逻辑"）；§2.2 "跨模块事件传递机制"段（line 293-294）；§2.2 RegistrationEvent 字段描述（sessionId "由 registration 模块从分诊上下文获取并填充"）

- **严重程度**：严重

- **改进建议**：明确定义 sessionId 从分诊流程到挂号流程的跨模块传播机制。可选方案：
  (a) 前端侧传递：前端在分诊结束后保留 sessionId，进入挂号界面时将 sessionId 作为请求参数传给 registration 模块，registration 模块将其写入 RegistrationEvent；
  (b) 后端侧查询：consultation 模块暴露轻量级查询接口（如根据 patientId + 最近分诊时间查询 sessionId），供 registration 模块在发事件前调用；
  (c) 方案 (a) 与 (b) 结合，前端传递为主路径、后端查询为 fallback。
  选定后同步更新 §1.1a 外部依赖表和 §2.2 事件传递机制描述。

---

## 2. 深度与完整性问题

### 问题 2：VisitFacade 调用失败无降级策略

- **问题描述**：产出为 DoctorFacade 定义了完整的跨模块调用降级保护（§3.1：独立超时阈值 2s、调用超时/异常时捕获并记录 WARN 日志、doctors 置为空列表不阻断主流程），但 VisitFacade（encounterId → visitId 转换）作为同样是强制同步跨模块调用路径，未定义任何故障处理行为。产出仅描述"MedicalRecordService 依赖 VisitFacade 接口"（§3.3），当 visit 模块不可用或超时时，病历生成流程的行为未定义——是整体失败返回错误、降级跳过 visitId 字段、还是使用 encounterId 作为 fallback 标识。此缺口导致 MedicalRecordService 无法直接编码：开发者在处理 VisitFacade 调用异常时无设计决策可遵循。

- **所在位置**：§3.3 RecordGenerateRequest（line 593-595，"MedicalRecordService 依赖 VisitFacade 接口而非实现"）；§1.1a 外部依赖表（visit 模块行，仅标注"实现缺失时病历存储失败"未规定具体降级行为）；§4.3 病历生成场景行为契约

- **严重程度**：一般

- **改进建议**：为 VisitFacade 补充与 DoctorFacade 对称的降级保护设计，包括：
  (a) 定义超时阈值（默认值，如 2s）；
  (b) 定义调用失败时的行为：返回 RX_MR_GEN_VISIT_NOT_FOUND 错误码 + 病历内容仍部分返回（保留已生成字段），或将 encounterId 直接作为 visitId 的 fallback 写入；
  (c) 同步更新 §4.3 行为契约和 §5.1 错误码表（新增 MR_GEN_VISIT_NOT_FOUND）。

### 问题 3：FieldMissingHint 字段生成规则未定义，无法直接指导"提示补全"实现

- **问题描述**：需求 3.4.3 要求"关键字段缺失提示补全"，产出中 MissingFieldDetector 仅定义了缺失检测逻辑（差集比对 + 非空非 null 判定），FieldMissingHint 包含 promptMessage 和 suggestedAction 字段但未定义其内容来源和生成规则。提示补全的"补全"部分——即如何建议用户补充缺失字段——在设计中未被具体化，开发者在实现时不知道 promptMessage 和 suggestedAction 应该写什么、从哪里获取。这与内部审议已确认的"AI 超时降级"等路径不同，属于功能核心路径上的缺口。

- **所在位置**：§1.3 FieldMissingHint 条目（line 111-112）；§3.3 MissingFieldDetector 职责描述（只定义检测不定义补全内容）；§3.3 MedicalRecordService 描述（仅提及"MissingFieldDetector"调用）

- **严重程度**：一般

- **改进建议**：定义 FieldMissingHint 字段的生成策略，至少明确以下两者之一：
  (a) 基于 DepartmentTemplateConfig 中每个 MedicalRecordField 的预定义提示模板（promptMessage + suggestedAction 静态文本，由管理员在科室模板中配置）；
  (b) 或由 AI 在生成病历的同时返回缺失字段的补全建议（需同步扩展 ai-api MedicalRecordGenResponse 字段）。
  如采用 (a) 为主要策略，需同步在 §3.3 中补充提示内容的加载/缓存机制，并在 §2.1 目录结构中明确 DeptTemplateConfig 是否扩展为包含提示模板字段。

### 问题 4：部分错误码遗漏于 §5.1 错误码表，影响错误处理的完整落地

- **问题描述**：产出正文中定义或引用了若干错误码但未出现在 §5.1 错误码表中，导致错误码表与正文不一致，开发者按错误码表编码时会遗漏这些错误码的处理分支：
  - `RX_ASSIST_UNIT_MISMATCH`：§8.3 定义了跨组单位比较时的输出规则，但未进表
  - `TRIAGE_SESSION_NOT_FOUND`：§3.1 和 §4.1 手动选科端点描述中定义了 selectDepartment 在 TriageRecord 不存在时返回此错误码，但未进表
  - `RX_MR_GEN_VISIT_NOT_FOUND`（若采纳问题 2 建议需新增）

- **所在位置**：§5.1 模块级错误码表（line 965-977）

- **严重程度**：一般

- **改进建议**：将上述遗漏错误码补充至 §5.1 错误码表中，归类到对应模块的合适分类行。

---

## 3. 边界场景与异常处理未覆盖

### 问题 5：MedicalRecord.contentJson 并发写更新丢失未处理

- **问题描述**：产出在 §3.3 中采用"单列 JSON TEXT + 读取→合并→写回"模式实现病历更新，但未定义并发写入时的 lost-update 防护。当两个请求同时读取同一病历、各自合并不同字段后写回时，后提交的写入会覆盖前一个写入的变更（丢失一个请求的字段更新）。产出描述了"增量更新语义"但未提供并发控制手段（如乐观锁 @Version、CAS 写、或行级锁），在临床场景中此问题可能导致病历字段被静默丢弃。

- **所在位置**：§3.3 MedicalRecordRepository 描述（line 567，"病历更新方法支持增量更新语义——读取 contentJson、合并变更字段、写回"）；§3.3 MedicalRecord 实体（line 561-567）

- **严重程度**：一般

- **改进建议**：在 MedicalRecord 实体中增加 `@Version` 乐观锁字段，Repository 更新操作使用版本号校验，写冲突时返回并发错误码（如 MR_GEN_CONCURRENT_MODIFICATION），由前端提示用户刷新后重试。同步在 §3.3 和 §7 设计决策中补充此边界防护。

---

## 4. 事实逻辑审查

经审查，产出中未发现事实性错误（如引用标准冲突、数据源与需求文档定义不一致、组件间数据流逆转等显著矛盾）。前 3 轮审议中发现的 Phase 5 迁移断言矛盾（v15 P2）、BLOCK 阻断 prescriptionOrderId 为空时的分组边际（v15 P3）、流式输出与非流式输出矛盾（v15 P4）等均已修复。

---

## 5. 整体评价

产出整体质量较高，经过 3 轮内部审议和累计 73 项修订后，在技术可行性、模块划分、并发设计、错误处理等维度已达到较完善状态。当前遗留的 5 个问题集中于：(1) 跨模块数据流架构层面的未闭合路径（RegistrationEvent sessionId 传播）；(2) 与非对称设计不一致的降级策略缺失（VisitFacade）；(3) 部分核心功能被"肤浅定义"而未深及实现层面（FieldMissingHint 内容来源）；(4) 文档一致性维护遗漏（错误码表）；(5) 并发场景下的数据完整性防护缺失（contentJson 写丢失）。建议修复者优先聚焦问题 1 和问题 5（架构闭合和边界防护），其次按序处理问题 2-4。
