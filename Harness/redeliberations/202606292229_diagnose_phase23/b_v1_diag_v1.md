# 质量审查报告 — 第 1 轮

## 审查范围

审查 `a_v1_diag_v2.md` 诊断报告的质量问题，侧重内部审议未充分覆盖的维度：需求响应充分度、事实准确性、逻辑一致性、深度完整性、可操作性。

---

## 问题清单

### Q1 — P05 将不存在的要求归因于 OOD（事实错误）

- **位置**：P05 条目（a_v1_diag_v2.md:173）
- **严重程度**：严重
- **问题描述**：诊断原文称"OOD §3.2 SubmitResponse 要求返回 riskLevel/alerts/auditRecordId/prescriptionHash"。经查验 OOD §3.2（07_ood_phase2_C_3_DE.md:652-659），SubmitResponse 仅定义 submitted/prescriptionOrderId/blockInfo/errorCode 四个字段，**不存在**关于 riskLevel/alerts/auditRecordId/prescriptionHash 的要求。诊断将未定义的字段集归因于 OOD，构成事实错误。warnResult 的缺失确实是问题，但诊断对 OOD 契约范围的描述与事实不符。
- **改进建议**：修正 P05 中关于 OOD 要求的描述，准确引用 OOD 中 SubmitResponse 的实际字段定义。warnResult 缺失作为代码与 OOD 的偏差保留，但不应声称 OOD 要求了不存在的字段集。

### Q2 — M04 根因分类错误（逻辑矛盾）

- **位置**：M04 条目（a_v1_diag_v2.md:267-270）
- **严重程度**：严重
- **问题描述**：诊断将 M04 根因归类为"OOD 设计问题"，建议修改 OOD 文档。但 OOD §3.3（07_ood_phase2_C_3_DE.md:668）明确说明 MR_GEN_CONCURRENT_MODIFICATION 用于**更新操作**（"若更新时版本号与数据库中的版本号不一致"），指向的是 UPDATE 路径。代码在 `MedicalRecordServiceImpl.java:102-110` 的 INSERT 路径（新实体 save）捕获 OptimisticLockException，而新实体 @Version=null 在 JPA persist 时不会抛出此异常——这是**实现编码问题**（错误地将异常处理放在了不可达路径），而非 OOD 文档的设计缺陷。OOD 的描述本身是准确的。
- **改进建议**：将根因分类修改为"实现编码问题"，修改建议改为"将 OptimisticLockException 捕获移至 UPDATE/merge 路径，INSERT 路径移除此类异常处理"。

### Q3 — C23 修复建议与代码实际依赖矛盾（逻辑矛盾）

- **位置**：C23 条目（a_v1_diag_v2.md:141）
- **严重程度**：中等
- **问题描述**：诊断建议"将业务数据操作移至 AI 调用和 TriageRecord 持久化之后"。但 `TriageServiceImpl.java:72-80`中的 session 修改（setChiefComplaint、setCorrectedChiefComplaint、setAdditionalResponses、setRoundCount）中，setChiefComplaint 和 setCorrectedChiefComplaint 的值来自 request 参数，而 session 对象在第 82-83 行被传给 `toAiTriageRequest(request, session)` 以构建 AI 请求的完整上下文。将这部分移至 AI 调用之后将导致 AI 请求缺失必要数据。OOD 的"先写数据库再更新内存"策略针对的是 TriageRecord 持久化与 DialogueSession 状态更新的**相对顺序**，不应被解读为禁止所有前置内存操作。
- **改进建议**：细化修复建议——区分两类 session 操作：(a) AI 请求构建**必需的**前置操作（setChiefComplaint 等）保留在 AI 调用前；(b) 仅将非 AI 必需的累积状态更新（如 roundCount、QA 历史追加等）在 TriageRecord 持久化后执行，或按 OOD "先写数据库"策略在写入 TriageRecord 之后再更新 session。

### Q4 — 缺少系统性的优先级排序（深度不足）

- **位置**：全文（a_v1_diag_v2.md）
- **严重程度**：中等
- **问题描述**：诊断报告列出了 61 个问题，但未提供任何优先级排序，也未按影响面/修复成本对问题分组。以下三类问题具有明显不同的紧急程度：
  - **P0（必须立即修复）**：E02（同 sessionId 二次分诊违反 @Column(unique=true) 约束）、C04（缺少 @Transactional 导致数据不一致风险）、S04（同 session 并发访问无串行化保护）
  - **P1（严重影响业务逻辑）**：P01（异步 AI 调度完全未实现）、A03（AiSuggestionResult 5 状态映射全未实现）、C03/C12/A04（correctedChiefComplaint 路径完全未连通）
  - **P2（可并行修复）**：C10（UUID 格式校验）、M06（超时外化到配置）、A08（英文文案替换）等
  
  执行者拿到报告后需要自行梳理优先级，增加决策成本。
- **改进建议**：在报告开头增加优先级分组表，按严重程度和依赖关系对全部问题分群，标注修复前的先决条件。

### Q5 — P09 的 OOD 问题归因不够准确（事实准确性）

- **位置**：P09 条目（a_v1_diag_v2.md:197-199）
- **严重程度**：轻微
- **问题描述**：诊断称`PrescriptionItem.unit`"在剂量校验中实际需要"属 OOD 遗漏。但更准确地说，DosageThresholdService 的 `check()` 方法（`DosageThresholdService.java:40`）在比较 `request.getUnit()` 与 `matched.getUnit()` 时，传入的 request 是 `DosageCheckRequest`类型而非 `PrescriptionItem` 类型。`PrescriptionItem` 的 `unit` 字段存在于代码中但其用途并非直接服务于 DosageThresholdService——它处于 submit 端点 `SubmitRequest.prescriptionItems` 中，在处方提交时提供给审核环节。OOD 的 PrescriptionItem 6 字段定义集中于审核所需的核心字段，`unit` 作为扩展字段需要评估是否应在 OOD 和 submit 路径中正式支持。当前的诊断将"代码中存在"等同于"OOD 应补全"，未充分论证 unit 字段在审核/提交流程中的业务必要性。
- **改进建议**：补充 unit 字段的业务使用场景描述，澄清其在 DosageCheckRequest 与 PrescriptionItem 之间的角色差异，为 OOD 修订提供更充分的依据。

### Q6 — A09 修复定位偏差（可操作性问题）

- **位置**：A09 条目（a_v1_diag_v2.md:415-416）
- **严重程度**：轻微
- **问题描述**：诊断将问题归位于 `AuditConverter.toAuditResponse`（`AuditConverter.java:48-56`），称"AI 数据为空时走降级路径"。但 Converter 的职责是映射/转换，它无法主动触发降级——降级决策应由调用方（`PrescriptionAuditServiceImpl`）在调用 converter 之前做出。Converter 中 `aiData == null → PASS` 的设计是因为调用方已将调用链走到了"使用 AI 结果"路径，Converter 只能按照传入的 data 做映射。真正的修复点应在 `PrescriptionAuditServiceImpl` 的业务逻辑中：在调用 converter 前检测 aiResult 状态并选择降级路径。
- **改进建议**：修正修复定位，指明修复点在 `PrescriptionAuditServiceImpl` 中的调用方代码而非 `AuditConverter`。

### Q7 — 缺少重复问题的整合建议（深度不足）

- **位置**：C15/E01、C06/E03、P02/E06、P03/S02、P04/E04（a_v1_diag_v2.md）
- **严重程度**：轻微
- **问题描述**：诊断报告中 5 组问题横跨多个section 但本质相同（如 C15=E01 都是 RegistrationEventListener 重试范围问题，P03=S02 都是 TTL 清理缺失）。诊断虽标注了"同 X"交叉引用，但未给出**整合修复策略**——例如 TTL 清理可抽象为统一调度服务而非各模块各自实现，事件定义可合并到公共模块而非 prescription/medical-record 各自实现。执行者按 section 逐个修复会产生重复工作。
- **改进建议**：对每组重复问题，增加"合并修复建议"：说明应统一实现还是各自实现，以及推荐的抽象层级（公共模块 vs 模块内部）。

### Q8 — C22 与 C08 的诊断建议冲突未调和（逻辑不一致）

- **位置**：C08（a_v1_diag_v2.md:48-52）与 C22（a_v1_diag_v2.md:134-136）
- **严重程度**：轻微
- **问题描述**：C08 建议 selectDepartment 接口改为 3 参"overwrite 逻辑下沉"；C22 也说"接口改为 3 参"。但 OOD §4.1 要求"手动选科端点始终覆盖写入"，即 overwrite=true 是业务强制要求。如果接口改为 3 参不暴露 overwrite，则 overwrite=true 必须硬编码在某处（实现层或 controller 层）。两个条目的独立建议没有说明 overwrite 下沉到哪一层（Impl 还是 Controller），且 C08 建议让 RegistrationEventListener 也调用 selectDepartment——但 RegistrationEventListener 的调用场景是"仅当 finalDepartmentId 为空时写入"，这与"始终覆盖"的手动选科语义不同。诊断未说明同一接口如何在两种调用语义（覆盖 vs 条件写入）下工作。
- **改进建议**：合并 C08 与 C22 为单一修复项，明确说明：(a) 接口保持 3 参，手动选科路径在 Impl 层硬编码 overwrite=true；(b) RegistrationEventListener 不调用 selectDepartment（因为语义不同），而是独立使用条件写入逻辑。

---

## 整体评价

诊断报告对代码问题的定性（真实存在/误报）整体准确，版本修订（v2 将 S05 改为误报）正确。证据引用充分，逐行定位精确。主要质量问题集中在上述 8 个方面：

- **2 个事实错误**（Q1 - P05 归因不存在的要求；Q2 - M04 根因分类错误）
- **2 个逻辑矛盾**（Q3 - C23 建议与代码依赖冲突；Q8 - C08/C22 建议未调和）
- **2 个深度不足**（Q4 - 缺少优先级排序；Q7 - 缺少合并修复策略）
- **2 个可操作性问题**（Q5 - 论证不充分；Q6 - 修复定位偏差）

修复上述问题后，诊断报告将具备从"指出问题"到"指导执行"的完整可操作性。
