# 任务指令（v12）

## 动作
NEW

## 任务描述
实现包E（辅助开方）prescription 模块辅助开方子域的全部代码，包含以下类型（位于 `com.aimedical.modules.prescription` 包下）：

### api/
- **PrescriptionAssistController.java** — REST 端点：
  - `POST /api/prescription/assist` — 接收业务层 `PrescriptionAssistRequest`，返回 `PrescriptionAssistResponse`；调用 PrescriptionAssistService.assist()，同步执行 AI 辅助开方 → 本地即时校验（剂量+过敏）→ CRITICAL 告警写入 PrescriptionDraftContext
  - `POST /api/prescription/assist/check-dose` — 接收 `DosageCheckRequest`，委托 DosageThresholdService 做即时阈值校验；同步将 CRITICAL 级别告警全量覆盖写入 PrescriptionDraftContext；触发异步 AI 建议生成（通过 DedupTaskScheduler 去重后创建 AiSuggestionResult）；返回 `DosageCheckResponse`（含 alerts, taskId, contextCriticalCount）
  - `GET /api/prescription/assist/suggestion/{taskId}` — 异步 AI 建议查询：不存在→404 + RX_ASSIST_SUGGESTION_NOT_FOUND，PENDING→200 + 状态，COMPLETED→200 + 结果 + 自动置 consumed=true，FAILED→200 + failReason

### dto/assist/
- **PrescriptionAssistRequest.java** — diagnosis(必填), examResults(List\<ExamResultItem\>), patientInfo(PatientInfo 含 patientId/age/gender/allergyHistory/allergyDetails/comorbidities), existingPrescription(String 可选), prescriptionId(String 可选, 空值时后端自动生成), encounterId(String 可选)
- **PrescriptionAssistResponse.java** — prescriptionDraft(String, 处方草案 JSON), doseWarnings(List\<DoseWarning\>), allergyWarnings(List\<AllergyWarningItem\>), errorCode(String 可选, AI 无推荐时填充 RX_ASSIST_AI_NO_RECOMMENDATION), disclaimerRequired(boolean), prescriptionId(String 可选, 后端自动生成时回写)
- **DosageCheckRequest.java** — prescriptionId(String 可选, 空值时后端自动生成并回写), drugCode(String), dosage(double), unit(String), routeOfAdministration(String), patientAge(Integer 可选), patientWeight(BigDecimal 可选), frequency(String 可选)
- **DosageCheckResponse.java** — alerts(List\<DosageAlert\>), taskId(String), contextCriticalCount(Integer)
- **DosageAlert.java** — alertLevel(DosageAlertLevel), warningType(DoseWarningType), message(String), drugCode(String), currentDose(double), suggestedValue(BigDecimal 可选), errorCode(String 可选)
- **DosageAlertLevel.java** — enum: INFO/WARNING/CRITICAL
- **DoseWarningType.java** — enum: OVER_SINGLE_DOSE/OVER_DAILY_DOSE/OVER_DURATION(预留)
- **DoseWarning.java** — drugId(String), warningType(DoseWarningType), message(String), severity(DosageAlertLevel)
- **AllergyWarningItem.java** — drugId(String), allergen(String), severity(AllergyWarningSeverity)
- **AllergyWarningSeverity.java** — enum: INFO/WARNING/HIGH
- **AiSuggestionResult.java** — taskId(String), suggestion(String 可选, COMPLETED 时填充), status(AiSuggestionStatus), createTime(LocalDateTime), failReason(String 可选), consumed(boolean, 默认 false), partialData(String 可选)
- **AiSuggestionStatus.java** — enum: PENDING/COMPLETED/FAILED

### service/assist/
- **PrescriptionAssistService.java** (interface) — `PrescriptionAssistResponse assist(PrescriptionAssistRequest)`, `DosageCheckResponse checkDose(DosageCheckRequest)`
- **impl/PrescriptionAssistServiceImpl.java** — 实现：
  - assist(): 调用 AiService.prescriptionAssist() → AssistConverter 转换 → 复用同模块 AllergyCheckRule 做过敏冲突检查 → DosageThresholdService 做剂量校验 → CRITICAL 告警写入 PrescriptionDraftContext → 返回 PrescriptionAssistResponse
  - AI 无推荐场景: AiResult.success=true 且 drugs 为空 → 返回空 prescriptionDraft + RX_ASSIST_AI_NO_RECOMMENDATION
  - checkDose(): 委托 DosageThresholdService.check() → CRITICAL 告警写入 PrescriptionDraftContext(全量覆盖) → DedupTaskScheduler 去重检查 → 创建 PENDING AiSuggestionResult → 返回 DosageCheckResponse
- **DosageThresholdService.java** — class：
  - `List<DosageAlert> check(DosageCheckRequest request)` — 按 drugCode+routeOfAdministration+age+weight 六层匹配优先级查询 DosageStandard → 单次剂量超限→OVER_SINGLE_DOSE, 日剂量超限(frequency+dailyMax)→OVER_DAILY_DOSE; 六级均未命中→errorCode=RX_ASSIST_DOSE_STANDARD_NOT_FOUND
- **DedupTaskScheduler.java** — class：
  - 封装 SuggestionStore.createIfNotExists() 调用：同一 prescriptionId 下存在 PENDING 或 COMPLETED 未消费的 AiSuggestionResult 时复用已有 taskId；否则创建新 task

### converter/
- **AssistConverter.java** — 业务层 DTO ↔ ai-api 层 DTO 双向映射：
  - `toAiPrescriptionAssistRequest(PrescriptionAssistRequest)` → ai-api PrescriptionAssistRequest
  - `toPrescriptionAssistResponse(AiResult<PrescriptionAssistResponse> aiResult)` → 业务层 PrescriptionAssistResponse

### 修改已有文件
- **context/PrescriptionDraftContext.java** — 增加写方法：
  - `void updateCriticalAlerts(String prescriptionId, List<DosageAlert> alerts)` — 全量覆盖该 prescriptionId 的 CRITICAL 标记（alerts 为空时清除对应条目）
  - `int getContextCriticalCount(String prescriptionId)` — 返回当前 CRITICAL 告警数量

## 选择理由
T9（处方审核子域）已通过验证（8 用例全部通过，BUILD SUCCESS）。T10 的前置编译依赖（T2 ai-api DTO、T4 Store 接口、T5 门面接口、T6 DosageStandard 实体、T7 模块骨架、T9 现有代码库）均已就绪；T10 与 T9 同属 prescription 模块，可复用同模块的 AllergyCheckRule、DosageStandardRepository、PrescriptionDraftContext 等已有类型，上下文切换成本最低。按实施路线表依次推进。

## 任务上下文

### OOD 设计文档
详见 `Docs/07_ood_phase2_C_3_DE.md` 以下节：
- §1.3 包E 核心抽象（132–148 行）
- §2.1 目录结构—prescription 模块 assist 子包（203–236 行）
- §3.4 包E 辅助开方详细设计（709–831 行）
- §4.4 辅助开方场景（988–1023 行）
- §4.5 AssistConverter 映射逻辑（1079–1091 行）

### 行为契约（OOD §4.4）
```
主端点 POST /api/prescription/assist:
  输入: { diagnosis(必填), examResults, patientInfo, existingPrescription, encounterId, prescriptionId(可选) }
  流程: AiService.prescriptionAssist() → AllergyCheckRule 过敏检查 → DosageThresholdService 剂量校验 → CRITICAL 写入 PrescriptionDraftContext
  AI 无推荐: errorCode=RX_ASSIST_AI_NO_RECOMMENDATION, 空 prescriptionDraft + 本地校验结果正常返回(非 4xx)

即时校验子端点 POST /api/prescription/assist/check-dose:
  输入: { drugCode, dosage, unit, routeOfAdministration, patientAge, patientWeight, frequency, prescriptionId(可选) }
  流程: DosageThresholdService.check() → CRITICAL 全量覆盖写入 DraftContext → DedupTaskScheduler 去重检查 → 返回 { alerts, taskId, contextCriticalCount }

异步建议查询 GET /api/prescription/assist/suggestion/{taskId}:
  不存在/TTL过期 → RX_ASSIST_SUGGESTION_NOT_FOUND
  PENDING → 前端轮询(2s, 30s 超时)
  COMPLETED → 返回结果 + consumed=true
  FAILED → 返回 failReason
```

### 已有代码上下文
- **Prescription 模块（T9 产出）**：已实现审核子域全部代码（Controller/dto/service/rule/entity/repository/converter/context/PrescriptionErrorCode）
- **ai-api DTO（T2 产出）**：PrescriptionAssistRequest, PrescriptionAssistResponse, DoseWarningItem, AllergyWarningItem, ExamResultItem, PatientInfo 等
- **SuggestionStore 接口（T4 产出）**：在 common-module-api 中，含 createIfNotExists(taskId, prescriptionId, supplier) 原子方法
- **DraftContextStore 接口（T4 产出）**：在 common-module-api 中，prescription 模块已有 PrescriptionDraftContext 封装（只读方法 hasCriticalAlerts/getCriticalAlerts）
- **DosageStandard 实体（T6 产出）**：common/entity/DosageStandard.java
- **AllergyCheckRule（T9 产出）**：同模块 rule/ 包下，可直接复用
- **DrugFacade（T5 产出）**：common-module-api/drug/DrugFacade，供查询药品名称/规格信息
