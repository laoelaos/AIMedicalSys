# 审查范围

## 依据文档
- `Docs/07_ood_phase2_C_3_DE.md` — Phase 2/3 包C/D-AI1/D-AI2/E 架构级 OOD 设计方案

## 审查目标
审查从分支 `202606301851_fix_diagnosis_todo` 合并到 `feat/task3` 的全部代码变更，验证实现与 OOD 设计的对齐度。

## 覆盖范围
`AIMedical/` 下的全部变更，包含四大业务模块：

### 模块一：consultation（包C 智能分诊）
- TriageController、TriageService/Impl
- DialogueSession、DialogueSessionManager
- TriageRuleEngine、DefaultTriageRuleEngine
- DepartmentFallbackProvider
- TriageRecord、RegistrationEventListener
- DeadLetterEvent、DeadLetterCompensationService
- 全部 DTO、Converter

### 模块二：prescription（包D-AI1 处方审核 + 包E 辅助开方）
- PrescriptionAuditController/Service/Impl
- PrescriptionAssistController/Service/Impl
- 本地规则引擎（AllergyCheckRule、ContraindicationCheckRule、DuplicateCheckRule、DosageLimitRule、SpecialPopulationDosageRule、DrugInteractionRule）
- PrescriptionDraftContext、DosageThresholdService
- AuditRecord、AuditConverter、AssistConverter
- 全部 DTO、枚举、实体

### 模块三：medical-record（包D-AI2 病历生成）
- MedicalRecordController、MedicalRecordService/Impl
- TemplateConfigManager、DatabaseTemplateConfigManager
- MissingFieldDetector/Impl
- 全部 DTO、Converter

### 模块四：ai-api / common-module-api / common
- AiService、AiResult、AiResultFactory
- ai-api DTO（triage/prescription/medicalrecord）
- DoctorFacade、DrugFacade、VisitFacade
- SessionStore/SuggestionStore/DraftContextStore + ConcurrentHashMapStore
- RegistrationEvent、DosageStandard
- DegradationContext

## 审查维度
1. **OOD 对齐度** — 代码实现是否遵循设计文档约定的接口、职责划分、依赖方向
2. **正确性** — 业务逻辑、边界条件、异常处理、并发安全
3. **架构合规性** — 模块间依赖方向、Store 抽象层使用、跨模块门面调用
4. **测试覆盖** — 单元测试是否覆盖关键路径和边界场景

## 排除范围
- `Docs/` 目录变更（非代码）
- `Harness/` 目录变更（审查/实现工作产物）
- `pom.xml` 配置变更（如确认结构正确）
