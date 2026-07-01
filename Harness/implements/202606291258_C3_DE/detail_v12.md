# 详细设计（v12）

## 概述

本设计实现包E（辅助开方）prescription 模块 assist 子域的全部代码，覆盖 REST 端点、业务层 DTO、Service 层、剂量阈值校验服务、去重调度器、Converter，以及 PrescriptionDraftContext 和 PrescriptionErrorCode 的修改。对齐需求文档 §3.4.10 契约和 OOD §1.3/§3.4/§4.4/§4.5 行为规范。本任务与 T9（处方审核）同属 prescription 模块，可复用同模块的 AllergyCheckRule、DosageStandardRepository、PrescriptionDraftContext 等已有类型。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `api/PrescriptionAssistController.java` | 新建 | 3 个 REST 端点：assist / check-dose / suggestion |
| `dto/assist/PrescriptionAssistRequest.java` | 新建 | 辅助开方主端点请求 DTO（业务层） |
| `dto/assist/PrescriptionAssistResponse.java` | 新建 | 辅助开方主端点响应 DTO（业务层） |
| `dto/assist/DosageCheckRequest.java` | 新建 | 剂量即时校验请求 DTO |
| `dto/assist/DosageCheckResponse.java` | 新建 | 剂量即时校验响应 DTO |
| `dto/assist/DosageAlert.java` | 新建 | 剂量告警值对象（API 响应层，区别于 context.DosageAlert） |
| `dto/assist/DosageAlertLevel.java` | 新建 | 剂量告警级别枚举：INFO / WARNING / CRITICAL |
| `dto/assist/DoseWarningType.java` | 新建 | 剂量告警类型枚举：OVER_SINGLE_DOSE / OVER_DAILY_DOSE / OVER_DURATION |
| `dto/assist/DoseWarning.java` | 新建 | 处方草案剂量告警 DTO（align to OOD §3.4 DoseWarning） |
| `dto/assist/AllergyWarningItem.java` | 新建 | 过敏冲突告警 DTO |
| `dto/assist/AllergyWarningSeverity.java` | 新建 | 过敏告警严重程度枚举：INFO / WARNING / HIGH |
| `dto/assist/AiSuggestionResult.java` | 新建 | 异步 AI 建议结果值对象 |
| `dto/assist/AiSuggestionStatus.java` | 新建 | AI 建议状态枚举：PENDING / COMPLETED / FAILED |
| `service/assist/PrescriptionAssistService.java` | 新建 | 辅助开方业务接口 |
| `service/assist/impl/PrescriptionAssistServiceImpl.java` | 新建 | 辅助开方业务实现 |
| `service/assist/DosageThresholdService.java` | 新建 | 剂量阈值校验服务（class，非 interface） |
| `service/assist/DedupTaskScheduler.java` | 新建 | 异步 AI 去重调度器（class，封装 SuggestionStore.createIfNotExists） |
| `converter/AssistConverter.java` | 新建 | 业务层 DTO ↔ ai-api DTO 双向映射 |
| `context/PrescriptionDraftContext.java` | **修改** | 增加 updateCriticalAlerts 和 getContextCriticalCount 方法 |
| `PrescriptionErrorCode.java` | **修改** | 增加 RX_ASSIST_* 错误码 |

所有文件位于：`prescription/src/main/java/com/aimedical/modules/prescription/` 下（以该路径为前缀）。

## 类型定义

### PrescriptionAssistController

**形态**：class, @RestController
**包路径**：`com.aimedical.modules.prescription.api`
**职责**：辅助开方 REST 端点，提供三个入口：AI 辅助开方、剂量即时校验、异步 AI 建议查询

**构造方式**：Spring 构造器注入 PrescriptionAssistService

**公开接口**：
- `Result<PrescriptionAssistResponse> assist(@Valid @RequestBody PrescriptionAssistRequest request)` — `POST /api/prescription/assist`
- `Result<DosageCheckResponse> checkDose(@Valid @RequestBody DosageCheckRequest request)` — `POST /api/prescription/assist/check-dose`
- `ResponseEntity<Result<AiSuggestionResult>> getSuggestion(@PathVariable String taskId)` — `GET /api/prescription/assist/suggestion/{taskId}`

**行为契约**：
- `assist()`: 同步调用 PrescriptionAssistService.assist()；正常情况返回 200 + Result.success(response)；AI 无推荐（errorCode=RX_ASSIST_AI_NO_RECOMMENDATION）也返回 200 + errorCode 承载在 PrescriptionAssistResponse 中
- `checkDose()`: 同步调用 PrescriptionAssistService.checkDose()；返回 200 + Result.success(response)（即时校验结果 + taskId）
- `getSuggestion()`: 委托 `prescriptionAssistService.getSuggestion(taskId)`；业务异常 RX_ASSIST_SUGGESTION_NOT_FOUND 由全局 @ExceptionHandler 映射为 404；正常返回 200 + 对应状态

### dto/assist/ 各类型

#### PrescriptionAssistRequest

**形态**：class
**包路径**：`com.aimedical.modules.prescription.dto.assist`
**职责**：AI 辅助开方主端点请求值对象

**字段**：
- `diagnosis` — String, @NotBlank — 诊断结论
- `examResults` — List\<com.aimedical.modules.ai.api.dto.prescription.ExamResultItem\>, 可选 — 检查检验结果
- `patientInfo` — com.aimedical.modules.prescription.dto.audit.PatientInfo, @Valid — 患者信息（复用审核子域 PatientInfo，同模块内直接引用）
- `existingPrescription` — String, 可选 — 已有处方文本
- `prescriptionId` — String, 可选 — 空值时后端自动生成 UUID v4 并回写
- `encounterId` — String, 可选 — 就诊标识

#### PrescriptionAssistResponse

**形态**：class
**包路径**：`com.aimedical.modules.prescription.dto.assist`
**职责**：AI 辅助开方主端点响应值对象

**字段**：
- `prescriptionDraft` — String — 处方草案 JSON（AI 生成 drugs 列表的 JSON 文本；AI 无推荐时为空 JSON 对象或 `{"drugs":[]}`）
- `doseWarnings` — List\<DoseWarning\> — 剂量告警列表
- `allergyWarnings` — List\<AllergyWarningItem\> — 过敏冲突告警列表
- `errorCode` — String, 可选 — AI 无推荐时填充 RX_ASSIST_AI_NO_RECOMMENDATION；剂量标准未命中时各 DosageAlert 内部填充 errorCode
- `disclaimerRequired` — boolean — 固定 true
- `prescriptionId` — String, 可选 — 后端自动生成时回写

#### DosageCheckRequest

**形态**：class
**包路径**：`com.aimedical.modules.prescription.dto.assist`
**职责**：剂量阈值即时校验请求值对象

**字段**：
- `prescriptionId` — String, 可选（API 层跳过 @NotNull 校验，空值时后端自动生成并回写）
- `drugCode` — String, @NotBlank
- `dosage` — double, @Positive
- `unit` — String, @NotBlank
- `routeOfAdministration` — String, @NotBlank
- `patientAge` — Integer, 可选
- `patientWeight` — BigDecimal, 可选
- `frequency` — String, 可选 — 用于日剂量校验

#### DosageCheckResponse

**形态**：class
**包路径**：`com.aimedical.modules.prescription.dto.assist`
**职责**：剂量阈值即时校验响应值对象

**字段**：
- `alerts` — List\<DosageAlert\> — 即时校验告警列表
- `taskId` — String — 异步 AI 建议查询 taskId（UUID v4）
- `contextCriticalCount` — Integer — 当前 prescriptionId 下 PrescriptionDraftContext 中 CRITICAL 告警数量
- `prescriptionId` — String, 可选 — 后端自动生成时回写

#### DosageAlert

**形态**：class
**包路径**：`com.aimedical.modules.prescription.dto.assist`
**职责**：剂量告警值对象（API 响应层，区别于 context.DosageAlert 仅用于 PrescriptionDraftContext 存储）

**字段**：
- `alertLevel` — DosageAlertLevel — INFO / WARNING / CRITICAL
- `warningType` — DoseWarningType — OVER_SINGLE_DOSE / OVER_DAILY_DOSE / OVER_DURATION
- `message` — String
- `drugCode` — String
- `currentDose` — double
- `suggestedValue` — BigDecimal, 可选
- `errorCode` — String, 可选 — 如 RX_ASSIST_DOSE_STANDARD_NOT_FOUND

#### DosageAlertLevel

**形态**：enum
**包路径**：`com.aimedical.modules.prescription.dto.assist`
**值域**：`INFO`, `WARNING`, `CRITICAL`

#### DoseWarningType

**形态**：enum
**包路径**：`com.aimedical.modules.prescription.dto.assist`
**值域**：`OVER_SINGLE_DOSE`, `OVER_DAILY_DOSE`, `OVER_DURATION`
**备注**：OVER_DURATION Phase 2/3 预留，暂不实现

#### DoseWarning

**形态**：class
**包路径**：`com.aimedical.modules.prescription.dto.assist`
**职责**：处方草案中的剂量告警 DTO，对齐 OOD §3.4 DoseWarning

**字段**：
- `drugId` — String
- `warningType` — DoseWarningType
- `message` — String
- `severity` — DosageAlertLevel

#### AllergyWarningItem

**形态**：class
**包路径**：`com.aimedical.modules.prescription.dto.assist`
**职责**：过敏冲突告警 DTO

**字段**：
- `drugId` — String
- `allergen` — String
- `severity` — AllergyWarningSeverity

#### AllergyWarningSeverity

**形态**：enum
**包路径**：`com.aimedical.modules.prescription.dto.assist`
**值域**：`INFO`, `WARNING`, `HIGH`

#### AiSuggestionResult

**形态**：class
**包路径**：`com.aimedical.modules.prescription.dto.assist`
**职责**：异步 AI 建议结果值对象

**字段**：
- `taskId` — String
- `suggestion` — String, 可选 — COMPLETED 时填充（JSON 文本）
- `status` — AiSuggestionStatus
- `createTime` — LocalDateTime
- `failReason` — String, 可选
- `consumed` — boolean, 默认 false
- `partialData` — String, 可选 — AI 超时/部分生成时携带部分结果

#### AiSuggestionStatus

**形态**：enum
**包路径**：`com.aimedical.modules.prescription.dto.assist`
**值域**：`PENDING`, `COMPLETED`, `FAILED`

### service/assist/

#### PrescriptionAssistService

**形态**：interface
**包路径**：`com.aimedical.modules.prescription.service.assist`
**职责**：辅助开方业务契约

**公开接口**：
- `PrescriptionAssistResponse assist(PrescriptionAssistRequest request)` — AI 辅助开方主入口
- `DosageCheckResponse checkDose(DosageCheckRequest request)` — 剂量阈值即时校验子入口
- `AiSuggestionResult getSuggestion(String taskId)` — 异步 AI 建议查询

#### PrescriptionAssistServiceImpl

**形态**：class, @Service
**包路径**：`com.aimedical.modules.prescription.service.assist.impl`
**职责**：PrescriptionAssistService 实现

**构造方式**：Spring 构造器注入 AiService, AssistConverter, AllergyCheckRule, DosageThresholdService, PrescriptionDraftContext, DedupTaskScheduler, SuggestionStore, ObjectMapper, DrugFacade（可选，药品名称/规格信息查询）

**行为契约 — assist()**：
1. 若 request.prescriptionId 为空，自动生成 UUID v4 作为 prescriptionId
2. 调用 `assistConverter.toAiPrescriptionAssistRequest(request)` 组装 ai-api 层 PrescriptionAssistRequest
3. 调用 `aiService.prescriptionAssist(aiRequest)` 异步获取 `CompletableFuture<AiResult<ai-api PrescriptionAssistResponse>>`
4. 同步等待 future.get() 获取 AiResult
5. **AI 正常返回**（success=true, data 非空, data.prescriptionDraft.drugs 非空）：
   - 通过 AssistConverter.toPrescriptionAssistResponse(aiResult) 转换为业务层 response
   - 复用 AllergyCheckRule 做过敏冲突检查：构造临时 AuditRequest（patientInfo 从 request 取，prescriptionItems 从 AI 产出的 drugs 转换），调用 check()，将 LocalRuleResult 映射为 List\<AllergyWarningItem\>
   - 调用 DosageThresholdService.check() 对 AI 产出每项药品做剂量校验
   - CRITICAL 级别告警（来自 DosageThresholdService）调用 `prescriptionDraftContext.updateCriticalAlerts(prescriptionId, converted)` 全量覆盖写入
   - 返回 PrescriptionAssistResponse（含 doseWarnings + allergyWarnings 合并，disclaimerRequired=true，prescriptionId 回写）
6. **AI 无推荐**（success=true 但 data.prescriptionDraft.drugs 为空列表）：
   - 返回空 prescriptionDraft（`{"drugs":[]}`）+ 空 doseWarnings + 空 allergyWarnings + errorCode=RX_ASSIST_AI_NO_RECOMMENDATION + disclaimerRequired=true
   - 不清除 PrescriptionDraftContext（无 CRITICAL 告警需要写入）
7. **AI 失败/异常**（success=false 或 future.get 抛异常）：
   - 返回空 prescriptionDraft + errorCode=RX_ASSIST_AI_NO_RECOMMENDATION + disclaimerRequired=true
   - CRITICAL 标记状态不变

**行为契约 — checkDose()**：
1. 若 request.prescriptionId 为空，自动生成 UUID v4
2. 调用 `dosageThresholdService.check(request)` → List\<DosageAlert\>
3. 提取 alerts 中 alertLevel=CRITICAL 的条目，转换为 context.DosageAlert 后调用 `prescriptionDraftContext.updateCriticalAlerts(prescriptionId, criticalAlerts)` 全量覆盖
4. 调用 `dedupTaskScheduler.schedule(prescriptionId)` → 获取 taskId（复用已有或新建）
5. 构造 DosageCheckResponse：alerts（全量原始列表）+ taskId + contextCriticalCount（当前 CRITICAL 数量）
6. 返回 DosageCheckResponse

**行为契约 — getSuggestion()**：
1. 调用 `suggestionStore.findByTaskId(taskId)` 查询 AiSuggestionResult
2. 不存在 → 抛出 `BusinessException(RX_ASSIST_SUGGESTION_NOT_FOUND)`，由全局 @ExceptionHandler 映射为 404
3. COMPLETED → 设置 `result.consumed = true`，调用 `suggestionStore.save(result)` 持久化，返回 result
4. PENDING/FAILED → 直接返回 result（不做 consumed 变更）

**依赖关系**：
- 依赖 AiService（Method #52 prescriptionAssist）
- 依赖 AllergyCheckRule（同模块 rule 包，直接注入）
- 依赖 DosageThresholdService（同模块 service/assist 包）
- 依赖 PrescriptionDraftContext（同模块 context 包）
- 依赖 DedupTaskScheduler（同模块 service/assist 包）

**过敏检查实现细节**：
- 从 request.patientInfo 提取 allergyDetails 和 allergyHistory
- 对 AI 处方草案中每项药品的 drugId，通过 DrugAllergyMappingRepository 查询过敏原
- 匹配优先级：allergyDetails 精确匹配（allergen → 过敏原）→ allergyHistory 文本回退匹配
- 匹配结果映射为 AllergyWarningItem：AllergySeverity.SEVERE → AllergyWarningSeverity.HIGH；MODERATE → WARNING；MILD → INFO
- 此逻辑通过构造一个包含 patientInfo 和 AI 产出的 prescriptionItems 的临时 AuditRequest 对象，调用 `allergyCheckRule.check(auditRequest)` 获取 LocalRuleResult，再转换为 allergyWarnings

#### DosageThresholdService

**形态**：class, @Service
**包路径**：`com.aimedical.modules.prescription.service.assist`
**职责**：剂量阈值校验服务，按六层匹配优先级查询 DosageStandard 比较剂量

**构造方式**：Spring 构造器注入 DosageStandardRepository

**公开接口**：
- `List<DosageAlert> check(DosageCheckRequest request)` — 执行剂量阈值校验，返回全量告警列表

**行为契约 — 六层匹配优先级**：
1. 精确匹配：drugCode + routeOfAdministration + ageRangeStart≤age≤ageRangeEnd + weightRangeStart≤weight≤weightRangeEnd
2. 年龄+体重范围匹配：drugCode + route + age 在范围 + weight 在范围（非精确边界）
3. 年龄范围匹配：drugCode + route + age 在范围（weight 不参与）
4. 体重范围匹配：drugCode + route + weight 在范围（age 不参与）
5. 无分级默认阈值：drugCode + route（age 和 weight 均为 null）
6. 标准不存在：上述 5 级均未命中 → 返回 DosageAlert(alertLevel=CRITICAL, warningType=OVER_SINGLE_DOSE, errorCode=RX_ASSIST_DOSE_STANDARD_NOT_FOUND)

**剂量超限判断**（if/else if 短路优先级）：
- 单次剂量超限判定：
  - 若 `dosage > singleMax * 2` → alertLevel=CRITICAL（短路，不再判定 WARNING）
  - 否则若 `dosage > singleMax` → alertLevel=WARNING
  - 创建 DosageAlert(warningType=OVER_SINGLE_DOSE, alertLevel=对应级别)
- 日剂量校验：当 request.frequency 非空且 DosageStandard.dailyMax 非空时，计算 dosage × frequency 次数 > dailyMax → 额外创建 DosageAlert(warningType=OVER_DAILY_DOSE, alertLevel=WARNING)（日剂量告警独立于单次剂量告警，不互斥，可同时产出两条告警）
- 单位一致性：request.unit 与 DosageStandard.unit 比较，不一致时不做剂量比较，直接返回 WARNING 级告警（单位不匹配）

**查询方法**：调用 dosageStandardRepository.findByDrugCodeAndRouteOfAdministration() 获取候选列表，在内存中按优先级匹配

#### DedupTaskScheduler

**形态**：class, @Service
**包路径**：`com.aimedical.modules.prescription.service.assist`
**职责**：异步 AI 去重调度，封装 SuggestionStore.createIfNotExists() 调用

**构造方式**：Spring 构造器注入 SuggestionStore

**公开接口**：
- `String schedule(String prescriptionId)` — 返回复用或新建的 taskId

**行为契约**：
1. 生成新 task 候选 ID（UUID v4）
2. 调用 `suggestionStore.createIfNotExists(newTaskId, prescriptionId, () -> 新建 PENDING AiSuggestionResult)` 
3. 返回 store 返回的 AiSuggestionResult.taskId（复用已有或新建）
4. Supplier 仅在去重未命中时被调用（惰性求值）

**去重规则（由 SuggestionStore.createIfNotExists 保证原子性）**：
- 同一 prescriptionId 下存在 PENDING 状态的 task → 复用
- 存在 COMPLETED 且 consumed=false 的 task → 复用
- 当前 task 为 FAILED 或 COMPLETED 已被消费 → 创建新 task

**对 SuggestionStore 接口的依赖**：SuggestionStore 需要提供 `createIfNotExists(String taskId, String prescriptionId, Supplier<AiSuggestionResult> supplier)` 原子方法，返回 AiSuggestionResult。若该接口尚未在 common-module-api 中定义，则通过 SuggestionStore.compute() 在 DedupTaskScheduler 内部实现等价逻辑，或以临时扩展接口方式处理。

### converter/AssistConverter

**形态**：class, @Component
**包路径**：`com.aimedical.modules.prescription.converter`
**职责**：业务层 DTO ↔ ai-api DTO 双向映射

**方法签名**：
- `com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistRequest toAiPrescriptionAssistRequest(PrescriptionAssistRequest request)` — 业务层 → ai-api
- `PrescriptionAssistResponse toPrescriptionAssistResponse(com.aimedical.modules.ai.api.AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse> aiResult)` — ai-api → 业务层；当 aiResult.data 为 null 或 success=false 时返回空业务层 response

**映射规则 — toAiPrescriptionAssistRequest**：
- request.diagnosis → aiRequest.diagnosis（透传）
- request.examResults → aiRequest.examResults（透传，类型同为 ai-api ExamResultItem）
- request.patientInfo → aiRequest.patientInfo（从业务层 dto/audit/PatientInfo 转换为 ai-api dto/prescription/PatientInfo，含 allergyDetails 的 AllergyDetail → AllergyDetailItem 映射，与 AuditConverter 中 toAiPatientInfo 逻辑一致）
- request.existingPrescription → aiRequest.existingPrescription（透传）
- request.prescriptionId → aiRequest.prescriptionId（透传）
- request.encounterId → aiRequest.encounterId（透传）

**映射规则 — toPrescriptionAssistResponse**：
- aiResult.success=true 且 data 非空：
  - prescriptionDraft = data.prescriptionDraft（透传 ai-api 返回的 JSON 文本）
  - doseWarnings = mapDoseWarnings(data.doseWarnings)（DoseWarningItem → 业务层 DoseWarning）
  - allergyWarnings = mapAllergyWarnings(data.allergyWarnings)（ai-api AllergyWarningItem → 业务层 AllergyWarningItem，severity 字符串转为 AllergyWarningSeverity 枚举）
  - disclaimerRequired = data.disclaimerRequired（透传）
  - errorCode = data.errorCode（透传）
- aiResult.success=false 或 data 为 null：
  - 返回 PrescriptionAssistResponse(prescriptionDraft="", doseWarnings=空列表, allergyWarnings=空列表, disclaimerRequired=true)

**DoseWarningItem → DoseWarning 映射**：drugId, warningType(字符串→DoseWarningType), message, severity(字符串→DosageAlertLevel)

**ai-api AllergyWarningItem → 业务层 AllergyWarningItem 映射**：drugId, allergen, severity(字符串→AllergyWarningSeverity)

### context/PrescriptionDraftContext（修改）

**形态**：class, @Component（已有类）
**包路径**：`com.aimedical.modules.prescription.context`
**职责**：草稿上下文封装，对 DraftContextStore 提供 prescription 类型化访问；增加写入能力

**新增方法签名**：
- `void updateCriticalAlerts(String prescriptionId, List<context.DosageAlert> alerts)` — 全量覆盖该 prescriptionId 的 CRITICAL 标记；alerts 为空列表或 null 时调用 draftContextStore.remove(prescriptionId + ":criticalAlerts") 清除对应条目；alerts 非空时调用 draftContextStore.put(key, alerts) 覆盖写入
- `int getContextCriticalCount(String prescriptionId)` — 返回 `getCriticalAlerts(prescriptionId).size()`

**行为契约**：
- updateCriticalAlerts 使用与 getCriticalAlerts 一致的 key `prescriptionId + ":criticalAlerts"`
- 每次写入均为全量覆盖，不存在增量合并
- 调用方（PrescriptionAssistServiceImpl、DosageThresholdService）负责在写入前将 `dto.assist.DosageAlert`（API 层）转换为 `context.DosageAlert`（存储层）

### PrescriptionErrorCode（修改）

**形态**：enum（已有枚举）
**包路径**：`com.aimedical.modules.prescription`
**职责**：增加辅助开方子域错误码

**新增枚举值**：
| 枚举常量 | code | message | 场景 |
|---------|------|---------|------|
| RX_ASSIST_AI_NO_RECOMMENDATION | RX_ASSIST_AI_NO_RECOMMENDATION | AI 暂无可推荐药品 | AI 返回空处方草案 |
| RX_ASSIST_SUGGESTION_NOT_FOUND | RX_ASSIST_SUGGESTION_NOT_FOUND | 异步 AI 建议不存在或已过期 | GET suggestion 查询不存在 |
| RX_ASSIST_DOSE_STANDARD_NOT_FOUND | RX_ASSIST_DOSE_STANDARD_NOT_FOUND | 剂量标准未找到 | DosageThresholdService 六级均未命中 |

## 错误处理

**自定义错误码**：新增错误码定义在 PrescriptionErrorCode 枚举（已有文件，新增 3 个枚举值）

**异常处理模式**：
- PrescriptionAssistController 遵循与 PrescriptionAuditController 一致的模式：业务异常通过全局 @ExceptionHandler 处理
- assist 端点：AI 失败/降级/无推荐均不抛出异常，以 errorCode 字段承载于 PrescriptionAssistResponse 中正常返回 200
- check-dose 端点：始终返回 200 + DosageCheckResponse（即使无告警也返回空列表）
- suggestion 端点：不存在 → 404 + BusinessException(RX_ASSIST_SUGGESTION_NOT_FOUND)；其他状态正常返回 200

**异步调用异常处理**：
- `CompletableFuture.get()` 捕获 InterruptedException → 恢复中断标记；捕获 ExecutionException → 视为 AI 调用失败
- 异常时不修改 PrescriptionDraftContext

**剂量校验异常处理**：
- DosageStandardRepository 查询结果为空时走六级末位路径（errorCode=RX_ASSIST_DOSE_STANDARD_NOT_FOUND），不抛异常
- 单位不一致时返回 WARNING 级告警，不抛异常

**错误码清单**：

| 错误码 | 语义 | 场景 |
|--------|------|------|
| RX_ASSIST_AI_NO_RECOMMENDATION | AI 无可推荐药品 | assist 端点 AI 返回空 drugs |
| RX_ASSIST_SUGGESTION_NOT_FOUND | 异步建议不存在/已过期 | GET suggestion 查询不存在 |
| RX_ASSIST_DOSE_STANDARD_NOT_FOUND | 剂量标准未找到 | DosageThresholdService 六级未命中 |

## 行为契约

### assist 主端点流程
1. prescriptionId 空值处理 → 自动生成 UUID v4
2. AssistConverter → ai-api PrescriptionAssistRequest
3. AiService.prescriptionAssist()（同步阻塞 get()）
4. AI 成功且有推荐 → 转换 → 过敏检查（复用 AllergyCheckRule）→ 剂量校验（DosageThresholdService）→ CRITICAL 写入 PrescriptionDraftContext → 返回完整 PrescriptionAssistResponse
5. AI 成功但无推荐 → errorCode=RX_ASSIST_AI_NO_RECOMMENDATION + 空 draft
6. AI 失败/异常 → errorCode=RX_ASSIST_AI_NO_RECOMMENDATION + 空 draft

### check-dose 子端点流程
1. prescriptionId 空值处理 → 自动生成 UUID v4
2. DosageThresholdService.check(request) → List\<DosageAlert\>
3. CRITICAL alerts → PrescriptionDraftContext.updateCriticalAlerts()（全量覆盖，无 CRITICAL 时清除）
4. DedupTaskScheduler.schedule(prescriptionId) → taskId
5. 返回 DosageCheckResponse(alerts, taskId, contextCriticalCount)

### GET suggestion 查询流程
1. 调用 `prescriptionAssistService.getSuggestion(taskId)`
2. 不存在 → 抛出 BusinessException(RX_ASSIST_SUGGESTION_NOT_FOUND)，全局 @ExceptionHandler 映射 404
3. PENDING → 返回 AiSuggestionResult(status=PENDING)（前端轮询 2s/次，30s 超时）
4. COMPLETED → 返回 AiSuggestionResult(status=COMPLETED, suggestion, consumed=true)
5. FAILED → 返回 AiSuggestionResult(status=FAILED, failReason)

### DosageThresholdService 匹配流程
1. 查询 DosageStandardRepository.findByDrugCodeAndRouteOfAdministration()
2. 内存中按六层优先级匹配
3. 命中 → 比较 dosage vs singleMax/dailyMax → 产出 DosageAlert
4. 未命中 → 产出 DosageAlert(alertLevel=CRITICAL, errorCode=RX_ASSIST_DOSE_STANDARD_NOT_FOUND)

## 依赖关系

| 类型 | 依赖的已有类型 | 来源模块 |
|------|--------------|---------|
| PrescriptionAssistController | Result, PrescriptionAssistService | common, 本模块 |
| PrescriptionAssistServiceImpl | AiService, AllergyCheckRule, DosageThresholdService, PrescriptionDraftContext, DedupTaskScheduler, SuggestionStore, AssistConverter, ObjectMapper, DrugFacade | ai-api, 本模块, common-module-api |
| DosageThresholdService | DosageStandardRepository, DosageStandard | 本模块, common |
| DedupTaskScheduler | SuggestionStore, AiSuggestionResult | common-module-api, 本模块 |
| AssistConverter | ai-api PrescriptionAssistRequest/Response, ai-api DoseWarningItem/AllergyWarningItem, ai-api PatientInfo, ai-api ExamResultItem, AiResult | ai-api |
| PrescriptionDraftContext（修改） | DraftContextStore, context.DosageAlert | common-module-api, 本模块 |
| PrescriptionAssistRequest | dto/audit/PatientInfo, ai-api ExamResultItem | 本模块 audit, ai-api |

**暴露给后续任务的公开接口**：
- PrescriptionAssistService.assist(PrescriptionAssistRequest) → PrescriptionAssistResponse
- PrescriptionAssistService.checkDose(DosageCheckRequest) → DosageCheckResponse
- PrescriptionAssistService.getSuggestion(String) → AiSuggestionResult
- PrescriptionDraftContext.updateCriticalAlerts(String, List) — 供 PrescriptionAuditServiceImpl 的 submit 流程消费 CRITICAL 阻断
- PrescriptionDraftContext.getContextCriticalCount(String) — 供 checkDose 端点返回 contextCriticalCount

## 修订说明（v12 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] PrescriptionAssistController 构造依赖不足以支持 getSuggestion 端点 | 在 PrescriptionAssistService 接口中新增 `getSuggestion(String taskId) → AiSuggestionResult` 方法；PrescriptionAssistServiceImpl 增加 SuggestionStore 构造注入并在 getSuggestion() 中实现查询+consumed 写回逻辑；Controller 委托 service.getSuggestion() 处理查询，业务异常由全局 ExceptionHandler 统一映射 |
| [一般] DosageThresholdService 剂量超限判定条件重叠 | 将单次剂量超限判定改为 if/else if 短路优先级：先判 CRITICAL（> singleMax×2），命中则短路不再判 WARNING；否则判 WARNING（> singleMax）|
