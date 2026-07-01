根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### [问题 1] 一般 — consumed 标记设置职责在 §3.4 和 §4.4 之间矛盾
- 位置：§3.4 AiSuggestionResult 状态图（line 665–666）vs §4.4 异步建议查询端点（line 940–943）
- 描述：§3.4 陈述"查询端点返回结果时将 consumed 置为 true"（后端自动设置），而 §4.4 陈述"前端标记 consumed=true 通知后端"（前端显式通知）。两者语义矛盾，将导致实现分歧。
- 改进建议：统一为后端在返回 COMPLETED 状态时自动设 consumed=true（PENDING/FAILED/NOT_FOUND 路径不设），并在 §3.4 中补充文档说明"前端崩溃后重新发起 check-dose 将创建新 task"作为可接受的边界行为。

### [问题 2] 一般 — DosageStandard 缺少变更事件通知，与同级配置实体的事件驱动刷新机制不一致
- 位置：§9.3（line 1526）；§8 DosageStandard 定义
- 描述：DrugAllergyMapping、DrugContraindicationMapping、DrugCompositionDict、TriageRule、DepartmentTemplateConfig 等配置实体均定义了变更事件 + Caffeine 定时刷新的双重缓存失效策略。DosageStandard 与这些实体同属 admin 模块管理，但未定义任何 DosageStandardChangeEvent。
- 改进建议：在 §9.3 事件声明表中补充 DosageStandardChangeEvent（可标注 Phase 2/3 预留），或在 §8 中显式声明"DosageStandard 当前无缓存，变更事件推迟至引入缓存时实现"。

### [问题 3] 轻微 — DoctorFacade 返回值 AvailableDoctor 缺少正式 DTO 定义
- 位置：§1.3 DoctorFacade 条目（line 73）；§3.1 DoctorFacade（line 463–470）
- 描述：AvailableDoctor 作为 DoctorFacade.findAvailableDoctorsByDepartment() 的返回值类型被引用（含 doctorId / doctorName / departmentId / availableSlotCount 四个字段），但未在 §1.3 核心抽象表或 §2.1 目录结构中作为正式 DTO 列出。
- 改进建议：在 §1.3 包C 核心抽象表或跨模块门面部分新增 AvailableDoctor DTO 条目，与 RecommendedDoctor、MatchedRule 等并列定义。

### [问题 4] 轻微 — VisitFacade fallback 路径中"visitId 格式约束"未定义
- 位置：§3.3 RecordGenerateRequest（line 627）
- 描述：fallback 路径 (a) 定义为"将 encounterId 直接作为 visitId 的 fallback 写入（当 encounterId 非空且满足 visitId 格式约束时）"。但"visitId 格式约束"的具体规则未定义。
- 改进建议：定义 visitId 格式约束规则（如"符合 UUID v4 格式"或"长度 8–36 的字母数字字符串"），或显式移除该条件约束，直接使用 encounterId 作为 visitId fallback。

## 历史迭代回顾

### 已解决的问题（出现在历史反馈但当前反馈中不再提及的问题）
第 6-8 轮识别的 17 个问题（DrugFacade 超时配置、§8.4 数字表述不符、RX_AUDIT_FORCE_SUBMIT_INVALID 错误码缺失、AiSuggestionResult TTL 清理参数、PrescriptionDraftContext CRITICAL 告警写入路径、DedupTaskScheduler 去重原子操作、SubmitContext 快照定义、DeadLetterCompensationService 投递目标、partialData 语义、DeadLetterCompensationService departmentName 缺失、AI 输入校验错误码归属、核心业务 Service 方法签名缺失、patientId 降级查询排序标准、RegistrationEvent departmentName 字段、@Retryable 异常类型过滤、TriageRecord.sessionId 唯一约束、ScheduledExecutorService 统一管理）均已在前序轮次解决，当前诊断中不再提及。

### 持续存在的问题（在多轮反馈中反复出现的问题，需重点解决）
- consumed 标记设置职责矛盾：第 9 轮历史反馈（问题 1）与本轮诊断（问题 1）均识别，表明上一轮迭代的修复未能彻底解决，需在本轮重点修复。
- DosageStandard 变更事件通知：第 9 轮历史反馈（问题 2）与本轮诊断（问题 2）均识别，表明上一轮迭代的修复未能彻底解决，需在本轮重点修复。

### 新发现的问题（本轮新识别的问题）
- AvailableDoctor 缺少正式 DTO 定义（问题 3）
- visitId 格式约束未定义（问题 4）

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606290200_phase23_pkgCDE_ood\a_v9_copy_from_v8.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606290200_phase23_pkgCDE_ood\requirement.md
