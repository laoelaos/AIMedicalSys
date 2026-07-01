# 详细设计（v10）

## 概述

本设计实现包D-AI1（处方审核）prescription 模块审核子域的全部代码，覆盖 REST 端点、业务层 DTO、Service 层、本地规则引擎、JPA 实体、Repository 和 Converter，对齐需求文档 3.4.2 契约和 OOD §3.2/§4.2 行为规范。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `prescription/src/main/java/com/aimedical/modules/prescription/api/PrescriptionAuditController.java` | 新建 | 3 个 REST 端点：audit/submit/revoke |
| `prescription/src/main/java/com/aimedical/modules/prescription/dto/audit/AuditRequest.java` | 新建 | 审核请求 DTO |
| `prescription/src/main/java/com/aimedical/modules/prescription/dto/audit/AuditResponse.java` | 新建 | 审核响应 DTO |
| `prescription/src/main/java/com/aimedical/modules/prescription/dto/audit/AuditAlert.java` | 新建 | 风险提示值对象 |
| `prescription/src/main/java/com/aimedical/modules/prescription/dto/audit/AlertSeverity.java` | 新建 | 提示严重程度枚举 |
| `prescription/src/main/java/com/aimedical/modules/prescription/dto/audit/AuditIssue.java` | 新建 | 审核问题条目 |
| `prescription/src/main/java/com/aimedical/modules/prescription/dto/audit/BlockResponse.java` | 新建 | 阻断响应 DTO |
| `prescription/src/main/java/com/aimedical/modules/prescription/dto/audit/AllergyDetail.java` | 新建 | 结构化过敏信息 |
| `prescription/src/main/java/com/aimedical/modules/prescription/dto/audit/DrugInteraction.java` | 新建 | 药物相互作用 |
| `prescription/src/main/java/com/aimedical/modules/prescription/dto/audit/Suggestion.java` | 新建 | 用药建议 |
| `prescription/src/main/java/com/aimedical/modules/prescription/dto/audit/SubmitRequest.java` | 新建 | 处方提交请求 DTO |
| `prescription/src/main/java/com/aimedical/modules/prescription/dto/audit/SubmitResponse.java` | 新建 | 处方提交响应 DTO |
| `prescription/src/main/java/com/aimedical/modules/prescription/dto/audit/PrescriptionItem.java` | 新建 | 处方药品条目 |
| `prescription/src/main/java/com/aimedical/modules/prescription/dto/audit/PatientInfo.java` | 新建 | 患者信息 |
| `prescription/src/main/java/com/aimedical/modules/prescription/service/audit/AuditRiskLevel.java` | 新建 | 风险等级枚举 |
| `prescription/src/main/java/com/aimedical/modules/prescription/service/audit/PrescriptionAuditService.java` | 新建 | 审核 + 提交流程业务接口 |
| `prescription/src/main/java/com/aimedical/modules/prescription/service/audit/impl/PrescriptionAuditServiceImpl.java` | 新建 | 审核 + 提交流程业务实现 |
| `prescription/src/main/java/com/aimedical/modules/prescription/service/audit/PrescriptionAuditEnforcer.java` | 新建 | 阻断策略接口 |
| `prescription/src/main/java/com/aimedical/modules/prescription/service/audit/impl/PrescriptionAuditEnforcerImpl.java` | 新建 | 默认阻断实现 |
| `prescription/src/main/java/com/aimedical/modules/prescription/rule/LocalRuleEngine.java` | 新建 | 本地规则引擎接口 |
| `prescription/src/main/java/com/aimedical/modules/prescription/rule/LocalRuleResult.java` | 新建 | 规则结果值对象 |
| `prescription/src/main/java/com/aimedical/modules/prescription/rule/AllergyCheckRule.java` | 新建 | 药品过敏检查规则 |
| `prescription/src/main/java/com/aimedical/modules/prescription/rule/ContraindicationCheckRule.java` | 新建 | 合并症禁忌检查规则 |
| `prescription/src/main/java/com/aimedical/modules/prescription/rule/DuplicateCheckRule.java` | 新建 | 重复用药检查规则 |
| `prescription/src/main/java/com/aimedical/modules/prescription/rule/DosageLimitRule.java` | 新建 | 剂量范围检查规则 |
| `prescription/src/main/java/com/aimedical/modules/prescription/rule/SpecialPopulationDosageRule.java` | 新建 | 特殊人群剂量检查规则 |
| `prescription/src/main/java/com/aimedical/modules/prescription/rule/DrugInteractionRule.java` | 新建 | 骨架预留，Phase 4 启用 |
| `prescription/src/main/java/com/aimedical/modules/prescription/rule/entity/DrugAllergyMapping.java` | 新建 | 药物过敏映射实体 |
| `prescription/src/main/java/com/aimedical/modules/prescription/rule/entity/DrugContraindicationMapping.java` | 新建 | 药品禁忌症映射实体 |
| `prescription/src/main/java/com/aimedical/modules/prescription/rule/entity/DrugCompositionDict.java` | 新建 | 药品成分字典实体 |
| `prescription/src/main/java/com/aimedical/modules/prescription/rule/entity/DrugInteractionPair.java` | 新建 | 药物相互作用骨架，Phase 4 预留，标注 `@Table(schema = "PHASE4_PRELOAD")` |
| `prescription/src/main/java/com/aimedical/modules/prescription/entity/AuditRecord.java` | 新建 | 审核记录 JPA 实体 |
| `prescription/src/main/java/com/aimedical/modules/prescription/repository/AuditRecordRepository.java` | 新建 | 审核记录 Repository |
| `prescription/src/main/java/com/aimedical/modules/prescription/repository/DrugCompositionDictRepository.java` | 新建 | 药品成分字典 Repository |
| `prescription/src/main/java/com/aimedical/modules/prescription/repository/DrugAllergyMappingRepository.java` | 新建 | 药物过敏映射 Repository |
| `prescription/src/main/java/com/aimedical/modules/prescription/repository/DrugContraindicationMappingRepository.java` | 新建 | 药品禁忌症 Repository |
| `prescription/src/main/java/com/aimedical/modules/prescription/repository/DosageStandardRepository.java` | 新建 | 剂量标准只读 Repository |
| `prescription/src/main/java/com/aimedical/modules/prescription/converter/AuditConverter.java` | 新建 | 业务层 DTO ↔ ai-api DTO 转换 |
| `prescription/src/main/java/com/aimedical/modules/prescription/context/PrescriptionDraftContext.java` | 新建 | 草稿上下文封装，对 DraftContextStore 的 prescription 类型化访问 |
| `prescription/src/main/java/com/aimedical/modules/prescription/context/DosageAlert.java` | 新建 | 剂量告警值对象 |
| `prescription/pom.xml` | 修改 | 添加 `com.aimedical:patient` 依赖（AllergyDetail.severity 引用 AllergySeverity 枚举） |

## 类型定义

### PrescriptionAuditController

**形态**：class
**包路径**：`com.aimedical.modules.prescription.api`
**职责**：处方审核 REST 端点，提供审核/提交/撤销三个入口

**公开接口**：
- `Result<AuditResponse> audit(@Valid @RequestBody AuditRequest request)` — `POST /api/prescription/audit`
- `Result<SubmitResponse> submit(@Valid @RequestBody SubmitRequest request)` — `POST /api/prescription/submit`
- `ResponseEntity<Void> revoke(@PathVariable Long auditId)` — `POST /api/prescription/audit/{auditId}/revoke`

**构造方式**：Spring 构造器注入 PrescriptionAuditService, PrescriptionAuditEnforcer

**行为契约**：
- `audit()`: 同步执行审核，BLOCK 时调用 enforcer 组装 422；WARN/PASS 返回 200；均通过 `ResponseEntity` 返回非 200 状态码
- `submit()`: 委托 PrescriptionAuditService.submit() 执行三步检查，步①/步②阻断返回 HTTP 422 + BlockResponse；步③校验失败返回 400 + errorCode；提交成功返回 200
- `revoke()`: 仅允许撤销 WARN 级且 isLatest=true 的 AuditRecord；通过 `BusinessException` + `@ExceptionHandler` 全局处理器统一处理非 200 状态码（404 不存在、409 已撤销、404 非 WARN），Controller 层仅返回 `ResponseEntity.ok().build()`
- 阻断 BLOCK 时采用 `ResponseEntity.status(422).body(Result.fail(errorCode, message))` 方式返回

### dto/audit/ 各类型

#### AuditRequest

**形态**：class
**包路径**：`com.aimedical.modules.prescription.dto.audit`
**职责**：处方审核请求值对象

**字段**：
- `prescriptionId` — String, @NotBlank
- `prescriptionItems` — List\<PrescriptionItem\>, @NotEmpty
- `patientInfo` — PatientInfo, @Valid

#### AuditResponse

**形态**：class
**包路径**：`com.aimedical.modules.prescription.dto.audit`
**职责**：处方审核响应值对象

**字段**：
- `riskLevel` — AuditRiskLevel
- `alerts` — List\<AuditAlert\>
- `interactions` — List\<DrugInteraction\>
- `suggestions` — List\<Suggestion\>
- `fromFallback` — boolean

#### AuditAlert

**形态**：class
**包路径**：`com.aimedical.modules.prescription.dto.audit`
**职责**：单条风险提示

**字段**：`alertCode` String, `alertMessage` String, `severity` AlertSeverity

#### AlertSeverity

**形态**：enum
**包路径**：`com.aimedical.modules.prescription.dto.audit`
**职责**：风险提示严重程度分级

**值域**：`INFO`, `WARNING`, `CRITICAL`

#### AuditIssue

**形态**：class
**包路径**：`com.aimedical.modules.prescription.dto.audit`
**职责**：审核问题条目，持久化至 AuditRecord

**字段**：`fieldName` String, `issueDescription` String, `ruleId` String, `severity` AlertSeverity

#### BlockResponse

**形态**：class
**包路径**：`com.aimedical.modules.prescription.dto.audit`
**职责**：阻断响应值对象

**字段**：
- `blockReasons` — List\<String\>
- `blockCode` — String（如 "RX_BLOCK_CRITICAL_DOSE"、"RX_BLOCK_AUDIT"）
- `blockTime` — LocalDateTime

#### AllergyDetail

**形态**：class
**包路径**：`com.aimedical.modules.prescription.dto.audit`
**职责**：结构化过敏信息

**字段**：`allergen` String @NotBlank, `reactionType` String, `severity` AllergySeverity, `occurredAt` String

**依赖**：`com.aimedical.modules.patient.entity.AllergySeverity`（枚举值 MILD/MODERATE/SEVERE），来自 patient 模块

#### DrugInteraction

**形态**：class
**包路径**：`com.aimedical.modules.prescription.dto.audit`
**职责**：药物相互作用

**字段**：`drugPair` String, `severity` String, `description` String

#### Suggestion

**形态**：class
**包路径**：`com.aimedical.modules.prescription.dto.audit`
**职责**：用药建议

**字段**：`suggestionCode` String, `suggestionText` String

#### SubmitRequest

**形态**：class
**包路径**：`com.aimedical.modules.prescription.dto.audit`
**职责**：处方提交请求

**字段**：
- `prescriptionId` — String, @NotBlank
- `prescriptionItems` — List\<PrescriptionItem\>, @NotEmpty
- `forceSubmit` — boolean, 默认 false
- `auditRecordId` — Long, 可选

#### SubmitResponse

**形态**：class
**包路径**：`com.aimedical.modules.prescription.dto.audit`
**职责**：处方提交响应

**字段**：
- `submitted` — boolean
- `prescriptionOrderId` — String
- `blockInfo` — BlockResponse
- `errorCode` — String

#### PrescriptionItem

**形态**：class
**包路径**：`com.aimedical.modules.prescription.dto.audit`
**职责**：处方单条药品信息

**字段**：`drugId` String, `drugName` String, `dose` double, `frequency` String, `duration` String, `route` String

#### PatientInfo

**形态**：class
**包路径**：`com.aimedical.modules.prescription.dto.audit`
**职责**：审核请求中的患者信息

**字段**：
- `patientId` — String
- `age` — Integer
- `gender` — String
- `allergyHistory` — String
- `allergyDetails` — List\<AllergyDetail\>
- `comorbidities` — List\<String\>

### context/

#### PrescriptionDraftContext

**形态**：class, @Component
**包路径**：`com.aimedical.modules.prescription.context`
**职责**：草稿上下文封装，对 DraftContextStore 提供 prescription 类型化访问

**构造方式**：Spring 构造器注入 DraftContextStore

**公开接口**：
- `boolean hasCriticalAlerts(String prescriptionId)` — 检查是否存在 CRITICAL 级别剂量告警
- `List<DosageAlert> getCriticalAlerts(String prescriptionId)` — 获取 CRITICAL 级别剂量告警快照

**行为契约**：
- 通过 DraftContextStore 查询 prescriptionId 对应上下文中 `criticalAlerts` 键值
- 当 DraftContextStore 中无对应键值或列表为空时，hasCriticalAlerts 返回 false

#### DosageAlert

**形态**：class
**包路径**：`com.aimedical.modules.prescription.context`
**职责**：剂量告警值对象

**字段**：
- `severity` — String（值域 "CRITICAL"/"WARNING"）
- `message` — String
- `drugCode` — String

### service/audit/

#### AuditRiskLevel

**形态**：enum
**包路径**：`com.aimedical.modules.prescription.service.audit`
**职责**：处方风险等级

**值域**：`PASS`, `WARN`, `BLOCK`

**映射规则**：ai-api PrescriptionCheckResponse.riskLevel (LOW/MEDIUM/HIGH) → AuditConverter 映射为 PASS/WARN/BLOCK

#### PrescriptionAuditService

**形态**：interface
**包路径**：`com.aimedical.modules.prescription.service.audit`
**职责**：处方审核与提交流程业务契约

**公开接口**：
- `AuditResponse audit(AuditRequest request)` — 同步执行处方审核
- `SubmitResponse submit(SubmitRequest request)` — 执行三步阻断检查提交流程

**类型关系**：
- `audit()` 被 PrescriptionAuditController 调用；依赖 AiService, LocalRuleEngine, AuditRecordRepository, AuditConverter
- `submit()` 被 PrescriptionAuditController 调用；依赖 PrescriptionDraftContext, AuditRecordRepository, AuditConverter

#### PrescriptionAuditServiceImpl

**形态**：class
**包路径**：`com.aimedical.modules.prescription.service.audit.impl`
**职责**：PrescriptionAuditService 实现

**构造方式**：Spring 构造器注入 AiService, LocalRuleEngine, AuditRecordRepository, AuditConverter, PrescriptionDraftContext, CurrentUser

**行为契约 — audit()**：
1. 调用 `aiService.prescriptionCheck(auditConverter.toAiPrescriptionCheckRequest(request))`
2. 若 `AiResult.isSuccess()` 为 true：通过 `AuditConverter.toAuditResponse(aiResult)` 转换响应，`fromFallback=false`，持久化 AuditRecord（设 originalPrescription = JSON.serialize(request.getPrescriptionItems())，设 doctorId = String.valueOf(currentUser.getUserId())），返回 AuditResponse
3. 若 AI 失败（success=false 或异常）：降级调用 `localRuleEngine.check(request)` → 聚合 LocalRuleResult 列表为 AuditRiskLevel（任一 BLOCK 则整体 BLOCK，有 WARN 无 BLOCK 则 WARN，全部 passed 则 PASS）→ `fromFallback=true` → 持久化 AuditRecord（设 originalPrescription = JSON.serialize(request.getPrescriptionItems())，设 doctorId = String.valueOf(currentUser.getUserId())）→ 返回 AuditResponse
4. 持久化前：使用 `@Lock(PESSIMISTIC_WRITE)` 查询同一 prescriptionId 下已有 AuditRecord.isLatest → false；新记录 auditSequence=+1、isLatest=true
5. 异常路径捕获 ExecutionException/InterruptedException 与 TriageServiceImpl 一致模式

**行为契约 — submit() 三步阻断检查**：
1. **创建 SubmitContext**（线程级闭包 POJO，方法栈内传递）：
   - `List<DosageAlert> snapshotCriticalAlerts` — 步①快照
   - `LocalDateTime submitStartTime` — 步①执行时间戳
2. **步① CRITICAL 阻断检查**：调用 `prescriptionDraftContext.hasCriticalAlerts(request.getPrescriptionId())`
   - 存在 CRITICAL：将当前告警列表快照存入 `SubmitContext.snapshotCriticalAlerts`，返回 `SubmitResponse(submitted=false, blockInfo=BlockResponse(blockCode="RX_BLOCK_CRITICAL_DOSE"))`
   - 不存在：将空快照存入 SubmitContext，继续步②
3. **步② 审核结果阻断检查**：查询 `auditRecordRepository.findTopByPrescriptionIdAndIsLatestTrueOrderByAuditSequenceDesc(request.getPrescriptionId())`
   - 仅考虑 `isLatest=true` 的记录，已撤销（isLatest=false）的 WARN 记录在此步跳过，后续进入无审核结果路径
   - riskLevel=BLOCK：返回 `SubmitResponse(submitted=false, blockInfo=BlockResponse(blockCode="RX_BLOCK_AUDIT"))`
   - 非 BLOCK（PASS/WARN/无结果）：继续步③
4. **二次 CRITICAL 验证**（步②与步③之间）：重新查询 `prescriptionDraftContext.getCriticalAlerts()`
   - 若实时告警列表相对于 SubmitContext.snapshotCriticalAlerts 存在增量（实时列表包含快照中未有的 DosageAlert），立即返回阻断 `SubmitResponse(submitted=false, blockInfo=BlockResponse(blockCode="RX_BLOCK_CRITICAL_DOSE"))`
   - 无增量：继续步③
5. **步③ forceSubmit 判定**：
   - **无最新审核结果**：先调用 `this.audit(request)` 执行审核，按新结果走对应路径
   - **forceSubmit=false + PASS**：直接常规提交（当前骨架返回 submitted=true + prescriptionOrderId 模拟值）
    - **forceSubmit=false + WARN**：以步②查询到的 `isLatest=true` AuditRecord（即最新未撤销的审核记录）的 `originalPrescription` 为比对基准，对当前处方做五字段结构化比对（drugId + dose + frequency + duration + route），若不一致返回 `SubmitResponse(submitted=false, errorCode="RX_AUDIT_PRESCRIPTION_MODIFIED")`；若一致（WARN 仍有效但未授权强制提交）返回 `SubmitResponse(submitted=false, errorCode="RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT")`
    - **forceSubmit=true**：校验 `auditRecordId` 对应 AuditRecord 的 `riskLevel=WARN` 且 `isLatest=true`
     + 五字段结构化比对当前处方与 `originalPrescription`
     + 比对通过：写入 `forceSubmitted=true`、`forceSubmitTime=now`，保存 AuditRecord（`@Version` 乐观锁在此处生效）
     + 乐观锁冲突（`OptimisticLockException`）：捕获后返回 `SubmitResponse(submitted=false, errorCode="RX_AUDIT_CONCURRENT_SUBMIT")`
     + 比对失败：返回 `SubmitResponse(submitted=false, errorCode="RX_AUDIT_PRESCRIPTION_MODIFIED")`
   - **forceSubmit=true + 非 WARN**：返回 `SubmitResponse(submitted=false, errorCode="RX_AUDIT_FORCE_SUBMIT_INVALID")`

**五字段结构化比对规则**：
- 提取 `originalPrescription` JSON 中 prescriptionItems 列表，与 `SubmitRequest.prescriptionItems` 按 `drugId + dose + frequency + duration + route` 五个字段做组合比对
- 双方均为 null 或空列表 → 一致
- 一方为空另一方非空 → 不一致
- 忽略 JSON 字段顺序差异、null 与缺失字段差异、数值精度差异
- null 与 0/空字符串按业务语义等价处理（前端非空校验在前，此阶段不应出现 null 字段）

#### PrescriptionAuditEnforcer

**形态**：interface
**包路径**：`com.aimedical.modules.prescription.service.audit`
**职责**：BLOCK 阻断执行策略

**公开接口**：
- `BlockResponse enforce(String prescriptionId, List<String> reasons, String blockCode)`

#### PrescriptionAuditEnforcerImpl

**形态**：class
**包路径**：`com.aimedical.modules.prescription.service.audit.impl`
**职责**：默认阻断实现

**行为契约**：返回 BlockResponse(blockReasons=reasons, blockCode=blockCode, blockTime=LocalDateTime.now())

### rule/

#### LocalRuleEngine

**形态**：interface
**包路径**：`com.aimedical.modules.prescription.rule`
**职责**：本地规则校验引擎

**公开接口**：
- `List<LocalRuleResult> check(AuditRequest request)` — 执行全部 5 条规则，返回各规则的校验结果

**构造方式**：注入所有 rule 实例的 List（List\<Rule\> 模式）或显式注入 5 条规则

#### LocalRuleResult

**形态**：class
**包路径**：`com.aimedical.modules.prescription.rule`
**职责**：单条规则校验结果

**字段**：`ruleId` String, `passed` boolean, `message` String, `severity` AuditRiskLevel（passed=true 时语义为 PASS；passed=false 时值域 WARN/BLOCK）

#### AllergyCheckRule

**形态**：class
**包路径**：`com.aimedical.modules.prescription.rule`
**职责**：药品过敏检查

**依赖**：DrugAllergyMappingRepository

**行为契约**：
1. 遍历 prescriptionItems，对每个 drugCode 查询 DrugAllergyMappingRepository.findByDrugCode()
2. 若 patientInfo.allergyDetails 非空：按 allergen 精确匹配，匹配项取 `severity`（AllergySeverity 枚举，值域 MILD/MODERATE/SEVERE），SEVERE → BLOCK，MODERATE/MILD → WARN
3. 若 allergyDetails 为空：回退 allergyHistory 文本匹配（contains），命中 → BLOCK
4. ruleId = "ALLERGY_CHECK"

#### ContraindicationCheckRule

**形态**：class
**包路径**：`com.aimedical.modules.prescription.rule`
**职责**：合并症禁忌检查

**依赖**：DrugContraindicationMappingRepository

**行为契约**：
1. 遍历 prescriptionItems，对每个 drugCode 查询 DrugContraindicationMapping
2. 禁忌项 diseaseName 与 patientInfo.comorbidities 交集比对
3. level=ABSOLUTE_CONTRAINDICATION → BLOCK; RELATIVE_CONTRAINDICATION → WARN
4. ruleId = "CONTRAINDICATION_CHECK"

#### DuplicateCheckRule

**形态**：class
**包路径**：`com.aimedical.modules.prescription.rule`
**职责**：重复用药检查

**依赖**：DrugCompositionDictRepository

**行为契约**：
1. 遍历 prescriptionItems，查询 DrugCompositionDict 获取每个药品的 ingredientCode 集合
2. 构建 drugCode → ingredientCodeSet 映射，检测不同药品间的成分交集
3. 存在交集 → WARN
4. ruleId = "DUPLICATE_CHECK"

#### DosageLimitRule

**形态**：class
**包路径**：`com.aimedical.modules.prescription.rule`
**职责**：剂量范围检查

**依赖**：DosageStandardRepository

**行为契约**：
1. 遍历 prescriptionItems，对每个药品按 drugCode + route 查询 DosageStandardRepository.findByDrugCodeAndRouteOfAdministration()
2. 匹配优先级：精确匹配 age/weight 范围 → 年龄范围匹配 → 体重范围匹配 → 无分级默认阈值
3. dose > singleMax * 2 → BLOCK; dose > singleMax → WARN
4. ruleId = "DOSAGE_LIMIT"

#### SpecialPopulationDosageRule

**形态**：class
**包路径**：`com.aimedical.modules.prescription.rule`
**职责**：特殊人群剂量检查

**依赖**：DosageStandardRepository

**行为契约**：
1. 年龄 ≤ 14 岁（儿童）或 ≥ 65 岁（老年）时触发
2. 查询 DosageStandard 中对应年龄/体重分级的剂量标准
3. dose > 特殊上限 → BLOCK
4. ruleId = "SPECIAL_POPULATION_DOSAGE"

#### DrugInteractionRule

**形态**：class
**包路径**：`com.aimedical.modules.prescription.rule`
**职责**：骨架预留，Phase 4 启用

**行为契约**：所有方法返回空列表，不参与运行时校验

### rule/entity/

#### DrugAllergyMapping

**形态**：JPA @Entity
**包路径**：`com.aimedical.modules.prescription.rule.entity`
**职责**：药品-过敏原映射

**字段**：
- `id` — Long, @Id @GeneratedValue
- `drugCode` — String, @Column(nullable=false, unique=true)
- `allergens` — String, @Column(columnDefinition="TEXT") (JSON)

**类型关系**：继承 BaseEntity

#### DrugContraindicationMapping

**形态**：JPA @Entity
**包路径**：`com.aimedical.modules.prescription.rule.entity`
**职责**：药品-禁忌症映射

**字段**：
- `id` — Long, @Id @GeneratedValue
- `drugCode` — String, @Column(nullable=false, unique=true)
- `contraindications` — String, @Column(columnDefinition="TEXT") (JSON，每项含 diseaseName/level/description)

**类型关系**：继承 BaseEntity

#### DrugCompositionDict

**形态**：JPA @Entity
**包路径**：`com.aimedical.modules.prescription.rule.entity`
**职责**：药品成分字典

**字段**：
- `id` — Long, @Id @GeneratedValue
- `drugCode` — String, @Column(nullable=false, unique=true)
- `ingredients` — String, @Column(columnDefinition="TEXT") (JSON，每项含 ingredientCode/ingredientName)

**类型关系**：继承 BaseEntity

#### DrugInteractionPair

**形态**：JPA @Entity 骨架
**包路径**：`com.aimedical.modules.prescription.rule.entity`
**职责**：Phase 4 预留

**实体要求**：
- 标注 `@Entity` + `@Table(name = "drug_interaction_pair", schema = "PHASE4_PRELOAD")`
- 通过 `schema = "PHASE4_PRELOAD"` 标记此表在 Phase 4 前暂不初始化；在 Hibernate DDL auto 模式下，该 schema 不存在时表不会被创建
- Phase 4 启用时创建 `PHASE4_PRELOAD` schema 或移除 schema 属性即可正常建表

**字段**：`id` Long @Id, `drugCodeA` String, `drugCodeB` String, `severity` String, `description` String

### entity/AuditRecord

**形态**：JPA @Entity
**包路径**：`com.aimedical.modules.prescription.entity`
**职责**：审核记录持久化实体

**注解**：
- `@Entity` + `@Table(name = "audit_record", indexes = { @Index(name = "idx_audit_prescription_id", columnList = "prescriptionId"), @Index(name = "idx_audit_order_is_latest", columnList = "prescriptionOrderId,isLatest") })`
- `@AttributeOverride(name = "id", column = @Column(name = "audit_id"))` — 将 BaseEntity 继承的 `id` 主键列重映射为 `audit_id`，与业务语义对齐，消除与独立 `auditId` 字段的 @Id 冲突

**字段**（不含 BaseEntity 继承的 `id`/`createdAt`/`updatedAt`/`deleted`）：
- `prescriptionId` — String, @Column(nullable=false)
- `prescriptionOrderId` — String
- `doctorId` — String
- `patientId` — String
- `auditTime` — LocalDateTime
- `fromFallback` — boolean
- `forceSubmitted` — Boolean
- `forceSubmitTime` — LocalDateTime
- `auditSequence` — int
- `isLatest` — boolean
- `originalPrescription` — String, @Column(columnDefinition="TEXT") (JSON)
- `riskLevel` — String (AuditRiskLevel 名称值)
- `aiResult` — String, @Column(columnDefinition="TEXT") — AI 原始响应 JSON，仅 AI 路径填充，降级路径为 null
- `auditIssues` — String, @Column(columnDefinition="TEXT") (JSON)
- `version` — Integer, @Version 乐观锁

**类型关系**：继承 BaseEntity；主键使用 BaseEntity 继承的 `id`（`@AttributeOverride` 映射至数据库列 `audit_id`），与 TriageRecord 模式一致

### repository/

#### AuditRecordRepository

**形态**：interface
**包路径**：`com.aimedical.modules.prescription.repository`
**职责**：AuditRecord 查询

**方法签名**：
- `List<AuditRecord> findByPrescriptionOrderIdOrderByAuditSequenceDesc(String prescriptionOrderId)`
- `Optional<AuditRecord> findTopByPrescriptionIdOrderByAuditSequenceDesc(String prescriptionId)` — 供撤销后的回查等场景使用
- `Optional<AuditRecord> findTopByPrescriptionIdAndIsLatestTrueOrderByAuditSequenceDesc(String prescriptionId)` — submit 步②专用，仅查询 isLatest=true 的最新记录，与撤销流程隔离
- `Optional<AuditRecord> findTopByPrescriptionOrderIdAndIsLatestTrue(String prescriptionOrderId)`
- `List<AuditRecord> findByPrescriptionIdAndIsLatestTrue(String prescriptionId)` — 用于同一处方多次审核时的 isLatest 清理，标注 `@Lock(PESSIMISTIC_WRITE)` 防止并发冲突

**构造方式**：extends JpaRepository\<AuditRecord, Long\>

#### DrugCompositionDictRepository

**形态**：interface
**包路径**：`com.aimedical.modules.prescription.repository`
**职责**：DrugCompositionDict 查询

**方法签名**：`Optional<DrugCompositionDict> findByDrugCode(String drugCode)`

**构造方式**：extends JpaRepository\<DrugCompositionDict, Long\>

#### DrugAllergyMappingRepository

**形态**：interface
**包路径**：`com.aimedical.modules.prescription.repository`
**职责**：DrugAllergyMapping 查询

**方法签名**：`Optional<DrugAllergyMapping> findByDrugCode(String drugCode)`

**构造方式**：extends JpaRepository\<DrugAllergyMapping, Long\>

#### DrugContraindicationMappingRepository

**形态**：interface
**包路径**：`com.aimedical.modules.prescription.repository`
**职责**：DrugContraindicationMapping 查询

**方法签名**：`Optional<DrugContraindicationMapping> findByDrugCode(String drugCode)`

**构造方式**：extends JpaRepository\<DrugContraindicationMapping, Long\>

#### DosageStandardRepository

**形态**：interface
**包路径**：`com.aimedical.modules.prescription.repository`
**职责**：剂量标准只读查询（仅查询接口，不继承 JpaRepository）

**方法签名**：
- `List<DosageStandard> findByDrugCodeAndRouteOfAdministration(String drugCode, String routeOfAdministration)`

**构造方式**：extends Repository\<DosageStandard, Long\>（只读标记，Spring Data 仅暴露查询方法）

### converter/AuditConverter

**形态**：class
**包路径**：`com.aimedical.modules.prescription.converter`
**职责**：业务层 DTO ↔ ai-api DTO 双向映射

**公开接口**：
- `PrescriptionCheckRequest toAiPrescriptionCheckRequest(AuditRequest request)` — 业务 AuditRequest → ai-api PrescriptionCheckRequest
- `AuditResponse toAuditResponse(AiResult<PrescriptionCheckResponse> aiResult)` — ai-api PrescriptionCheckResponse → 业务 AuditResponse

**映射规则**：
- toAiPrescriptionCheckRequest: auditRequest.prescriptionItems → checkRequest.prescriptionItems (PrescriptionItem → PrescriptionCheckItem); auditRequest.patientInfo → checkRequest.patientInfo (业务 PatientInfo → ai-api PatientInfo + AllergyDetail → AllergyDetailItem)
- toAuditResponse: ai-api riskLevel (LOW→PASS, MEDIUM→WARN, HIGH→BLOCK); AlertItem → AuditAlert; DrugInteractionItem → DrugInteraction; SuggestionItem → Suggestion; fromFallback 透传

## 错误处理

- 自定义错误码定义在 `common/exception/GlobalErrorCode` 已存在；prescription 模块新增错误码在 `PrescriptionErrorCode` 枚举（本任务新建，实现 ErrorCode 接口）
- 阻断场景：Controller 层通过 ResponseEntity.status(422) 返回 BlockResponse
- 业务异常：使用 BusinessException 抛出，全局 @ExceptionHandler 捕获
- Repository 查询空值：使用 Optional.orElseThrow(() -> new BusinessException) 或在规则中做空安全处理
- OptimisticLockException 在 submit() 步③ forceSubmit=true 路径中捕获，转为 RX_AUDIT_CONCURRENT_SUBMIT 错误码返回

**错误码清单**：
| 错误码 | 语义 | 场景 |
|--------|------|------|
| RX_AUDIT_BLOCKED | 审核阻断 | audit 端点返回 BLOCK |
| RX_AUDIT_PRESCRIPTION_MODIFIED | 处方已变更 | submit 步③处方五字段比对不一致 |
| RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT | WARN 审核未确认 | forceSubmit=false + WARN + 五字段比对一致，需 forceSubmit=true 放行 |
| RX_AUDIT_CONCURRENT_SUBMIT | 并发提交冲突 | 乐观锁冲突（@Version） |
| RX_AUDIT_FORCE_SUBMIT_INVALID | 强制提交无效 | forceSubmit=true 但非 WARN 场景 |
| RX_AUDIT_REVOKE_NOT_WARN | 非 WARN 级别无法撤销 | revoke 端点 riskLevel != WARN |
| RX_AUDIT_REVOKE_ALREADY_REVOKED | 审核已撤销 | revoke 端点 isLatest 已为 false |
| RX_AUDIT_REVOKE_NOT_FOUND | 审核记录不存在 | revoke 端点 auditId 不存在 |

## 行为契约

### 审核流程 (audit)
1. 调用 AiService.prescriptionCheck() — 同步阻塞等待 CompletableFuture.get()
2. 成功路径：AiResult.success=true → AuditConverter.toAuditResponse() → 持久化 AuditRecord（originalPrescription = JSON.serialize(request.prescriptionItems)，doctorId = String.valueOf(currentUser.getUserId())，aiResult 写入 AI 原始响应 JSON） → 返回 AuditResponse
3. 失败路径（success=false 或异常）：LocalRuleEngine.check() → 聚合结果 → 持久化 AuditRecord（originalPrescription = JSON.serialize(request.prescriptionItems)，doctorId = String.valueOf(currentUser.getUserId())，fromFallback=true, aiResult=null） → 返回 AuditResponse
4. 持久化时：使用 `@Lock(PESSIMISTIC_WRITE)` 查询同一 prescriptionId 已有记录 → 将其 isLatest→false → 新记录 auditSequence=当前最大+1, isLatest=true

### 提交流程 (submit) — 三步阻断检查
1. **创建 SubmitContext**（线程级闭包 POJO，方法栈内传递，不持久化）
2. **步① CRITICAL 阻断检查**：查询 PrescriptionDraftContext → 存在 CRITICAL 告警 → 快照存入 SubmitContext → 返回 422 + BlockResponse(blockCode=RX_BLOCK_CRITICAL_DOSE)
3. **步② 审核结果阻断检查**：查询 findTopByPrescriptionIdAndIsLatestTrueOrderByAuditSequenceDesc（仅 isLatest=true）→ riskLevel=BLOCK → 返回 422 + BlockResponse(blockCode=RX_BLOCK_AUDIT)
4. **二次 CRITICAL 验证**（步②后、步③前）：重新查询 PrescriptionDraftContext，与 SubmitContext 快照比对 → 存在增量 → 返回 422 + BlockResponse(blockCode=RX_BLOCK_CRITICAL_DOSE)
5. **步③ forceSubmit 判定**：
   - forceSubmit=false + 无审核结果：先执行 audit() 再走后续
   - forceSubmit=false + PASS：直接常规提交（当前骨架返回 submitted=true + prescriptionOrderId 模拟值）
   - forceSubmit=false + WARN：五字段结构化比对 → 变更则返回 RX_AUDIT_PRESCRIPTION_MODIFIED；一致则返回 RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT
   - forceSubmit=true：校验 auditRecordId 对应 WARN + isLatest + 五字段比对 → 写入 forceSubmitted/forceSubmitTime（@Version 乐观锁保护）
   - forceSubmit=true + 非 WARN → RX_AUDIT_FORCE_SUBMIT_INVALID
   - OptimisticLockException 捕获 → RX_AUDIT_CONCURRENT_SUBMIT

### 撤销流程 (revoke)
1. 按 auditId 查询 AuditRecord（`auditRecordRepository.findById(auditId)`，PK 为 `BaseEntity.id` 经 `@AttributeOverride` 映射至列 `audit_id`）
2. 校验：isLatest=true 且 riskLevel=WARN
3. 通过：isLatest=false → save → Controller 返回 `ResponseEntity.ok().build()`
4. 不存在 → 抛出 `BusinessException(RX_AUDIT_REVOKE_NOT_FOUND)`，全局 `@ExceptionHandler` 映射为 404；非 WARN → `BusinessException(RX_AUDIT_REVOKE_NOT_WARN)` 映射为 404；已撤销(isLatest=false) → `BusinessException(RX_AUDIT_REVOKE_ALREADY_REVOKED)` 映射为 409

## 依赖关系

| 类型 | 依赖的已有类型 | 来源模块 |
|------|--------------|---------|
| PrescriptionAuditController | Result, PrescriptionAuditService, PrescriptionAuditEnforcer | common, 本模块 |
| PrescriptionAuditServiceImpl | AiService, AuditConverter, LocalRuleEngine, AuditRecordRepository, PrescriptionDraftContext, CurrentUser | ai-api, common-module-api, 本模块 |
| PrescriptionDraftContext | DraftContextStore | common-module-api |
| LocalRuleEngine | AllergyCheckRule, ContraindicationCheckRule, DuplicateCheckRule, DosageLimitRule, SpecialPopulationDosageRule | 本模块 |
| AllergyDetail (severity 字段) | AllergySeverity | patient 模块 |
| AllergyCheckRule | DrugAllergyMappingRepository | 本模块 |
| ContraindicationCheckRule | DrugContraindicationMappingRepository | 本模块 |
| DuplicateCheckRule | DrugCompositionDictRepository | 本模块 |
| DosageLimitRule | DosageStandardRepository | 本模块 |
| SpecialPopulationDosageRule | DosageStandardRepository | 本模块 |
| AuditConverter | PrescriptionCheckRequest, PrescriptionCheckResponse, PrescriptionCheckItem, PatientInfo(ai-api), AlertItem, DrugInteractionItem, SuggestionItem, AllergyDetailItem | ai-api |
| AuditRecord | BaseEntity | common |
| DosageStandardRepository | DosageStandard | common |

**暴露给后续任务的公开接口**：
- PrescriptionAuditService.audit(AuditRequest) → AuditResponse — 供 PrescriptionAssistServiceImpl 在 submit 流程中调用
- PrescriptionAuditService.submit(SubmitRequest) → SubmitResponse — 供 Controller 及后续编排层调用
- AuditRecordRepository — 供后续模块查询审核记录

## 修订说明（v10 r1）

| 审查意见 | 修改措施 |
|---------|---------|
| [严重] PrescriptionAuditService 仅定义了 audit()，submit 需要 Service 层编排 | PrescriptionAuditService 增加 `submit(SubmitRequest)` 方法；PrescriptionAuditServiceImpl 实现三步阻断检查逻辑，注入 PrescriptionDraftContext、AuditRecordRepository 等依赖；Controller 不再直接编排提交流程 |
| [严重] 提交流程缺少二次 CRITICAL 验证 | 在步②与步③之间增加二次 CRITICAL 验证：重新查询 PrescriptionDraftContext 并与 SubmitContext.snapshotCriticalAlerts 比对增量 |
| [严重] 缺少 PrescriptionDraftContext 声明和 context/ 子包 | 新增 `context/PrescriptionDraftContext.java`（@Component，封装 DraftContextStore）和 `context/DosageAlert.java`（剂量告警值对象）；在文件规划和依赖关系表中声明 |
| [一般] AuditRecord 缺少 aiResult 字段 | AuditRecord 增加 `aiResult` 字段（String, @Column(columnDefinition="TEXT")），AI 路径填充原始响应 JSON |
| [一般] @Table(schema = "PHASE4_PRELOAD") 非标准建表控制 | DrugInteractionPair 移除 `@Table(schema = ...)` 标注，改用排除实体扫描控制 Phase 2/3 不建表 |
| [一般] 缺少具体处方比较逻辑和 OptimisticLockException 处理 | 补充五字段（drugId+dose+frequency+duration+route）结构化比对规则定义；OptimisticLockException → RX_AUDIT_CONCURRENT_SUBMIT 转换在 submit() 步③ forceSubmit=true 路径中处理 |

## 修订说明（v10 r2）

| 审查意见 | 修改措施 |
|---------|---------|
| AllergyDetail.severity 类型改为 AllergySeverity（值域 MILD/MODERATE/SEVERE），在 prescription/pom.xml 中添加 patient 模块依赖，同步调整 AllergyCheckRule 逻辑 | AllergyDetail.severity 字段类型从 `AlertSeverity` 改为 `com.aimedical.modules.patient.entity.AllergySeverity`；文件规划中新增 `prescription/pom.xml` 修改条目，添加 patient 模块依赖；依赖关系表新增 AllergyDetail → AllergySeverity 行；AllergyCheckRule 行为契约更新为以 AllergySeverity.SEVERE→BLOCK、MODERATE/MILD→WARN 判定 |
| DrugInteractionPair 恢复使用 @Table(schema = "PHASE4_PRELOAD") 以符合 task 要求 | DrugInteractionPair 实体恢复标注 `@Table(name = "drug_interaction_pair", schema = "PHASE4_PRELOAD")`；移除排除实体扫描策略，改用 schema 不存在时 Hibernate 跳过建表的机制 |

## 修订说明（v10 r3）
| 审查意见 | 修改措施 |
|---------|---------|
| AuditRecord.isLatest 管理增加 @Lock(PESSIMISTIC_WRITE) 或唯一约束防护说明 | `AuditRecordRepository.findByPrescriptionIdAndIsLatestTrue()` 标注 `@Lock(PESSIMISTIC_WRITE)`；audit() 行为契约第 4 条和审核流程第 4 条均改为使用悲观锁后 flip isLatest |
| audit() 持久化阶段显式加入 originalPrescription = JSON.serialize(request.getPrescriptionItems()) | audit() 成功与降级两条路径的 AuditRecord 持久化均显式赋值 `originalPrescription = JSON.serialize(request.getPrescriptionItems())` |
| 指定 doctorId 从 SecurityContext/@AuthenticationPrincipal 获取并填充 | `PrescriptionAuditServiceImpl` 新增 `CurrentUser` 构造器依赖；audit() 中通过 `String.valueOf(currentUser.getUserId())` 填充 `AuditRecord.doctorId`；依赖关系表同步更新 |

## 修订说明（v10 r4）
| 审查意见 | 修改措施 |
|---------|---------|
| forceSubmit=false + WARN 五字段比对一致后的结果未定义 | 在 step ③ `forceSubmit=false + WARN` 分支补充：比对一致时返回 `SubmitResponse(submitted=false, errorCode="RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT")`，语义为 WARN 审核未确认、需 forceSubmit=true 放行；同步更新错误码表和流程摘要 |
| 提交流程步②未检查 isLatest，撤销后 WARN 记录仍被步②选中进入步③判定 | 步②查询方法改为 `findTopByPrescriptionIdAndIsLatestTrueOrderByAuditSequenceDesc`（仅 isLatest=true）；撤销后的记录 isLatest=false 故跳过，进入无审核结果路径（步③调用 audit() 获取新结果）；AuditRecordRepository 新增该查询方法 |

## 修订说明（v10 r5）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] `AuditRecord` 同时声明 `auditId` 为 `@Id` 且继承 `BaseEntity`（含 `@Id private Long id`），JPA 禁止单继承层次中存在两个 `@Id` 字段 | 移除 `auditId` 字段的 `@Id @GeneratedValue` 声明（`auditId` 字段已完全移除）；改用 `@AttributeOverride(name = "id", column = @Column(name = "audit_id"))` 将 BaseEntity 继承的 `id` 主键列映射为 `audit_id`，与业务语义对齐并消除编译冲突 |
| [一般] submit() 步③ WARN 比对未指明以哪个 AuditRecord 为基准 | 明确步③ `forceSubmit=false + WARN` 分支的比对基准为「步②查询到的 `isLatest=true` AuditRecord（即最新未撤销的审核记录）的 `originalPrescription`」 |
| [一般] revoke 端点返回 `Result<Void>` 与 404/409 状态码不兼容 | revoke 返回类型改为 `ResponseEntity<Void>`，非 200 状态码通过 `BusinessException` + 全局 `@ExceptionHandler` 统一处理；撤销流程中明确各异常场景对应的错误码和 HTTP 状态码映射 |
| [轻微] `AuditRecord` 缺少 `@Table(name = ...)` 标注 | 增加 `@Table(name = "audit_record", indexes = {...})` 标注描述 |
| [轻微] `DrugInteractionPair` 使用 `@Table(schema = "PHASE4_PRELOAD")` 跨数据库兼容性风险 | 维持 task 要求的 `@Table(schema = "PHASE4_PRELOAD")` 不变（实为 Phase 4 预留标记，Phase 2/3 运行时存在时跳过建表；正式启用时创建对应 schema 或移除 schema 属性）；在实体描述中补充跨数据库兼容说明 |
