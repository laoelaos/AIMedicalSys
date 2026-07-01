# 任务指令（v10）

## 动作
NEW

## 任务描述
实现包D-AI1（处方审核）prescription 模块审核子域的全部代码，包含以下类型（位于 `com.aimedical.modules.prescription` 包下）：

### api/
- **PrescriptionAuditController.java** — REST 端点：
  - `POST /api/prescription/audit` — 接收 `AuditRequest`，返回 `AuditResponse`；同步执行审核（AI → 降级 LocalRuleEngine → 持久化 AuditRecord）
  - `POST /api/prescription/submit` — 接收 `SubmitRequest`，执行三步阻断检查（CRITICAL → BLOCK → forceSubmit 判定），返回 `SubmitResponse`
  - `POST /api/prescription/audit/{auditId}/revoke` — 撤销 WARN 级审核（将对应 AuditRecord.isLatest 回退为 false）

### dto/audit/
所有业务层 DTO，字段对齐需求文档 3.4.2 契约：
- **AuditRequest.java** — prescriptionId(必填), prescriptionItems(含 drugId/drugName/dose/frequency/duration/route), patientInfo(含 patientId/age/gender/allergyHistory/allergyDetails/comorbidities)
- **AuditResponse.java** — riskLevel(AuditRiskLevel), alerts(List\<AuditAlert\>), interactions(List\<DrugInteraction\>), suggestions(List\<Suggestion\>), fromFallback
- **AuditAlert.java** — alertCode, alertMessage, severity(AlertSeverity)
- **AlertSeverity.java** — enum: INFO/WARNING/CRITICAL
- **AuditIssue.java** — fieldName, issueDescription, ruleId, severity
- **BlockResponse.java** — 阻断原因列表、阻断码、阻断时间
- **AllergyDetail.java** — allergen(必填), reactionType(可选), severity(可选), occurredAt(可选)
- **DrugInteraction.java** — drugPair, severity, description
- **Suggestion.java** — suggestionCode, suggestionText
- **SubmitRequest.java** — prescriptionId(必填), prescriptionItems(List), forceSubmit(boolean, 默认false), auditRecordId(Long, 可选)
- **SubmitResponse.java** — submitted(boolean), prescriptionOrderId(可选), blockInfo(可选), errorCode(可选)

### service/audit/
- **AuditRiskLevel.java** — enum: PASS/WARN/BLOCK
- **PrescriptionAuditService.java** (interface) — `AuditResponse audit(AuditRequest)`
- **impl/PrescriptionAuditServiceImpl.java** — 实现：调用 AiService.prescriptionCheck() → 成功返回 AuditResponse，失败降级 LocalRuleEngine → 聚合 LocalRuleResult 为 AuditRiskLevel → 持久化 AuditRecord → 返回
- **PrescriptionAuditEnforcer.java** (interface) — 定义 BLOCK 阻断策略
- **impl/PrescriptionAuditEnforcerImpl.java** — 默认阻断实现：返回 BlockResponse，Controller 组装 HTTP 422

### rule/
- **LocalRuleEngine.java** (interface) — `List<LocalRuleResult> check(AuditRequest)`
- **LocalRuleResult.java** — ruleId, passed(boolean), message, severity(AuditRiskLevel: WARN/BLOCK)
- **AllergyCheckRule.java** — 药品过敏检查（allergyDetails 精确匹配优先 → allergyHistory 文本回退）
- **ContraindicationCheckRule.java** — 合并症禁忌检查（遍历药品 → DrugContraindicationMapping → 与 comorbidities 交集比对）
- **DuplicateCheckRule.java** — 重复用药检查（DrugCompositionDict 成分编码交集）
- **DosageLimitRule.java** — 剂量范围检查（DosageStandard 年龄/体重分级，超上限 2 倍→BLOCK，未达 2 倍→WARN）
- **SpecialPopulationDosageRule.java** — 特殊人群剂量检查（儿童 ≤ 14 岁/老年 ≥ 65 岁，超出特殊上限→BLOCK）
- **DrugInteractionRule.java** (骨架预留，Phase 4 启用，不参与运行时校验)

### rule/entity/
- **DrugAllergyMapping.java** — JPA @Entity, drugCode 主键, allergens(JSON TEXT)
- **DrugContraindicationMapping.java** — JPA @Entity, drugCode 主键, contraindications(JSON TEXT, 每项含 diseaseName/level/description)
- **DrugCompositionDict.java** — JPA @Entity, drugCode 主键, ingredients(JSON TEXT, 每项含 ingredientCode/ingredientName)
- **DrugInteractionPair.java** — JPA @Entity 骨架, Phase 4 预留，标注 `@Table(schema = "PHASE4_PRELOAD")`

### entity/
- **AuditRecord.java** — JPA @Entity, 继承 BaseEntity；含 auditId/@Id/@GeneratedValue, prescriptionId, prescriptionOrderId(提交成功时填充), doctorId, patientId, auditTime, fromFallback, forceSubmitted, forceSubmitTime, auditSequence, isLatest, originalPrescription(JSON TEXT), riskLevel, auditIssues(JSON TEXT), @Version 乐观锁

### repository/
- **AuditRecordRepository.java** — findByPrescriptionOrderIdOrderByAuditSequenceDesc, findTopByPrescriptionIdOrderByAuditSequenceDesc, findTopByPrescriptionOrderIdAndIsLatestTrue
- **DrugCompositionDictRepository.java** — findByDrugCode
- **DrugAllergyMappingRepository.java** — findByDrugCode
- **DrugContraindicationMappingRepository.java** — findByDrugCode
- **DosageStandardRepository.java** (extends Repository, 只读) — findByDrugCodeAndRouteOfAdministration

### converter/
- **AuditConverter.java** — AuditRequest/Response ↔ ai-api 层 PrescriptionCheckRequest/Response 互转

## 选择理由
T1–T8 已全部完成，T9 的前置编译依赖（T2 ai-api DTO、T4 Store 接口、T5 门面接口、T6 DosageStandard 实体、T7 模块骨架）均已就绪。prescription 审核子域是包D-AI1 核心，按实施路线依次推进。

## 任务上下文

### OOD 设计文档
详见 `Docs/07_ood_phase2_C_3_DE.md` 以下节：
- §1.3 包D-AI1 核心抽象（91–118 行）
- §2.1 目录结构—prescription 模块（194–236 行）
- §3.2 包D-AI1 详细设计（537–659 行）
- §4.2 处方审核场景（899–960 行）

### 行为契约（OOD §4.2）
```
正常路径: AiService.prescriptionCheck() → AuditConverter.toAuditResponse() → AuditRecord 持久化
降级路径: LocalRuleEngine.check() (5 规则完整执行) → 聚合 LocalRuleResult → AuditRecord 持久化(fromFallback=true)
审核结果:
  PASS → 建议性意见，不影响提交
  WARN → 允许 forceSubmit=true 强制提交+留痕；支持撤销(auditId/revoke → isLatest=false)
  BLOCK → PrescriptionAuditEnforcer → HTTP 422 + BlockResponse
提交三步检查:
  步① CRITICAL 阻断检查(PrescriptionDraftContext)
  步② BLOCK 审核结果阻断检查(最新 AuditRecord)
  步③ forceSubmit 判定 + 处方版本校验
```

### 已有代码上下文
- **Prescription 模块骨架**：`backend/modules/prescription/pom.xml` 已创建，依赖 common/common-module-api/ai-api，仅含占位测试
- **ai-api DTO（T2 输出）**：`ai-api/.../dto/prescription/` — PrescriptionCheckRequest/Response, PrescriptionCheckItem, PatientInfo, AllergyDetailItem, DrugInteractionItem, AlertItem, SuggestionItem, ExamResultItem, DoseWarningItem, AllergyWarningItem
- **common-module-api（T4/T5 输出）**：SessionStore, SuggestionStore, DraftContextStore, DoctorFacade, DrugFacade, VisitFacade, RegistrationEvent, AvailableDoctor
- **DosageStandard 实体（T6 输出）**：`common/.../entity/DosageStandard.java`
- **Consultation 模块（T8 输出）**：可作为模块结构、Converter 模式、Service 分层参考
- **参考代码模式**：参考 `modules/consultation/` 中 Controller/Service/Impl/Converter/Repository 的分层结构和注解风格
