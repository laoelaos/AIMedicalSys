# R1.2: 包D-AI1 处方审核 + 包E 辅助开方（prescription 模块）完整实现审查

审查时间：2026-06-29

## 审查范围

处方审核（包D-AI1）与辅助开方（包E）核心实现，对照设计文档 `Docs/07_ood_phase2_C_3_DE.md` §3.2、§3.3 与第 4-6 章的审核/辅助开方核心抽象进行契约级一致性审查。

主要审查文件：

### 处方审核主代码
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/api/PrescriptionAuditController.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/service/audit/PrescriptionAuditService.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/service/audit/AuditRiskLevel.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/service/audit/PrescriptionAuditEnforcer.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/service/audit/impl/PrescriptionAuditServiceImpl.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/service/audit/impl/PrescriptionAuditEnforcerImpl.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/dto/audit/*.java`（13 个）
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/entity/AuditRecord.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/repository/AuditRecordRepository.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/converter/AuditConverter.java`

### 辅助开方主代码
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/api/PrescriptionAssistController.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/service/assist/PrescriptionAssistService.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/service/assist/impl/PrescriptionAssistServiceImpl.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/service/assist/DosageThresholdService.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/service/assist/DedupTaskScheduler.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/dto/assist/*.java`（12 个）
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/context/PrescriptionDraftContext.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/context/DosageAlert.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/converter/AssistConverter.java`

### 共享规则代码
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/rule/LocalRuleEngine.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/rule/DefaultLocalRuleEngine.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/rule/LocalRuleResult.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/rule/AllergyCheckRule.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/rule/ContraindicationCheckRule.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/rule/DuplicateCheckRule.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/rule/DosageLimitRule.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/rule/SpecialPopulationDosageRule.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/rule/DrugInteractionRule.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/rule/entity/*.java`（4 个）
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/repository/*.java`（5 个）

### 错误码
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/PrescriptionErrorCode.java`

### 跨模块引用验证
- `AIMedical/backend/modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/store/{SessionStore,SuggestionStore,DraftContextStore}.java`
- `AIMedical/backend/modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/drug/DrugFacade.java`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/entity/DosageStandard.java`
- `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/{AiService,AiResult,AiResultFactory}.java`
- `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/prescription/*.java`

## 发现

### 严重

#### [严重] 异步 AI 调度机制未实现，AiSuggestionResult 永远停留在 PENDING 状态

- **位置**：`AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/service/assist/DedupTaskScheduler.java:35-41` 及 `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/service/assist/impl/PrescriptionAssistServiceImpl.java:179`
- **描述**：设计文档 §3.4 / §4.4 / §6.3 明确要求 check-dose 主端点返回 taskId 后，异步触发 `AiService.prescriptionAssist()` 调用并在完成后将 `AiSuggestionResult.status` 更新为 `COMPLETED` 或 `FAILED`。当前 `DedupTaskScheduler.schedule()` 仅在 `SuggestionStore` 中预创建 PENDING 状态的 `AiSuggestionResult`，随后立即返回 taskId——没有任何 `@Async` 注解、`CompletableFuture.runAsync()` 或独立线程在后续触发 AI 调用。`grep` 全模块源码确认 `@Async`/`runAsync` 出现 0 次。
- **影响**：`GET /api/prescription/assist/suggestion/{taskId}` 端点（`PrescriptionAssistController.java:42-45`）返回的状态将永远为 `PENDING`，前端 30 秒超时后将只能看到"AI 建议生成超时"提示。包E 异步 AI 建议的整个端到端流程在生产环境中不可用。
- **建议**：在 `DedupTaskScheduler.schedule()` 返回前增加 `CompletableFuture.runAsync(() -> { AiResult<...> r = aiService.prescriptionAssist(...).get(); // 更新 SuggestionStore 中的 status/suggestion/partialData })`，或在 `PrescriptionAssistServiceImpl` 中以 `@Async` 注解包装异步任务。

#### [严重] DrugFacade 跨模块门面完全未注入未调用，违反 §2.2 跨模块数据获取机制契约

- **位置**：整个 `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription` 目录（`grep -n "DrugFacade"` 0 命中）；接口定义于 `AIMedical/backend/modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/drug/DrugFacade.java:1-5`
- **描述**：设计文档 §1.3 DrugFacade / §2.2 跨模块数据获取机制 / §4.2 处方审核场景 明确要求 prescription 模块通过 `DrugFacade.findByDrugCode(drugCode)` 查询药品名称/规格信息，并配置 2s 超时，超时时返回空药品信息 + WARN 日志，不阻断主流程。当前实现中 `DrugFacade` 在 `PrescriptionAuditServiceImpl`、`PrescriptionAssistServiceImpl`、`DosageThresholdService` 任何一个都未被注入或调用。
- **影响**：辅助开方与处方审核流程中药品名称仅依赖前端传入或 AI 返回的 `drugName`，缺少与 drug 主数据模块的实名校验和规格关联；drug 模块的 `DrugInfo` 数据在 prescription 模块流程中完全无作用，跨模块门面模式空转。
- **建议**：在 `PrescriptionAuditServiceImpl.audit()` 与 `PrescriptionAssistServiceImpl.assist()` 入口处遍历 `request.getPrescriptionItems()`，对每项 `drugId` 调用 `DrugFacade.findByDrugCode`，用返回的 `DrugInfo.drugName` 覆盖或补充 `PrescriptionItem.drugName`，并按设计要求捕获超时/异常记录 WARN 日志。

#### [严重] AiSuggestionResult 的 60 分钟 TTL 清理任务完全未实现

- **位置**：设计要求位于 `AIMedical/backend/modules/prescription` 下的 `config/` 或 `task/` 包，但 `ls src/main/java/com/aimedical/modules/prescription/` 仅显示 `PrescriptionErrorCode.java`、`api`、`context`、`converter`、`dto`、`entity`、`repository`、`rule`、`service` 9 个条目，无 `config/` 或 `task/` 包；`grep -rn "@Scheduled" src/main/java/` 0 命中
- **描述**：设计文档 §3.4 AiSuggestionResult TTL 60 分钟 + §6.1 定时任务集中管理 明确要求"由统一 Spring @Scheduled 任务每 5 分钟扫描清理 TTL 超过 60 分钟的过期条目"，且应封装为 `@Component` Bean 方法（如 `SuggestionCleanupTask.cleanup()`、`DraftContextCleanupTask.cleanup()`）。当前 prescription 模块无任何 `@EnableScheduling` 配置类或 `@Scheduled` 注解方法。
- **影响**：`AiSuggestionResult` 永远不会被自动清理，`DraftContextStore` 中的 CRITICAL 告警也不会按 60 分钟 TTL 过期清理；`ConcurrentHashMapStore` 内存将无限累积。`PrescriptionDraftContext` 设计明确"TTL 过期清理由统一 Spring @Scheduled 任务每 5 分钟扫描清理"——当前 CRITICAL 阻断标记即使在患者离开编辑器 60 分钟后仍会保留，可被旧任务复用导致误判。
- **建议**：在 prescription 模块下新增 `config/ScheduledTaskConfig.java`（使用 `@EnableScheduling`）和 `task/SuggestionCleanupTask.java`、`task/DraftContextCleanupTask.java` 两个 `@Component`，按设计要求每 5 分钟扫描 `SuggestionStore.keySet()` 与 `DraftContextStore` 中 `createTime`/`updateTime` 超过 60 分钟的条目并删除。

#### [严重] 规则变更事件（DrugAllergyMappingChangeEvent / DrugContraindicationChangeEvent / DrugCompositionDictChangeEvent）未定义、未消费、未监听

- **位置**：`ls AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/` 无 `event/` 包；`grep -rn "@EventListener\|ApplicationEvent\|ChangeEvent" src/main/java/` 0 命中
- **描述**：设计文档 §2.1 目录结构、§2.2 数据源变更事件通知、§9 规则管理接口定义、§9.4 配置变更审计日志与事务边界 明确要求：
  - admin 模块在 `DrugAllergyMapping`/`DrugContraindicationMapping`/`DrugCompositionDict` 变更后通过 `ApplicationEventPublisher` 发布 `DrugAllergyMappingChangeEvent` / `DrugContraindicationChangeEvent` / `DrugCompositionDictChangeEvent`
  - prescription 模块的 `AllergyCheckRule` / `ContraindicationCheckRule` / `DuplicateCheckRule` 监听事件刷新本地 Caffeine 缓存
  - 事件在 application 模块通过 `@TransactionalEventListener(phase=AFTER_COMMIT)` 跨模块传播
  - 当前 prescription 模块无任何 event 包/类/监听器
- **影响**：规则数据变更后端不能即时失效缓存；admin 模块发布的变更事件若无消费端会被静默丢弃；"事件驱动 + 定时刷新"双重失效策略在 prescription 模块的本地规则链上完全失效。
- **建议**：在 prescription 模块下创建 `event/` 包，定义 3 个事件类（payload 含 entityType/entityId），并对 `AllergyCheckRule` / `ContraindicationCheckRule` / `DuplicateCheckRule` 各加一层 Caffeine 缓存包装（`@Cacheable(key="#drugCode")` + 监听事件触发 `cache.invalidate(drugCode)`）。

#### [严重] 提交端点 WARN 路径内容匹配成功时返回设计未定义的错误码，且 SubmitResponse 缺失 warnResult 字段

- **位置**：`AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/service/audit/impl/PrescriptionAuditServiceImpl.java:202-207`、`AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/PrescriptionErrorCode.java:9`、`AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/dto/audit/SubmitResponse.java:1-44`
- **描述**：设计文档 §4.2 步③ forceSubmit=false + WARN 内容未变更场景要求"直接返回当前 WARN 审核结果并提示医生选择强制提交或修改处方，避免无意义重复审核"——§4.6 响应 JSON 示例 1273-1280 给出 `warnResult: { riskLevel, alerts, auditRecordId, prescriptionHash }` 字段设计。当前实现:
  - 当 `forceSubmit=false` + WARN + 内容匹配时返回 `errorCode=RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT`（`PrescriptionErrorCode.java:9`）——该错误码未在 §5.1 错误码表中定义
  - `SubmitResponse` 类无 `warnResult` 字段，前端无法获取当前 WARN 详情
  - 步③设计要求的"提示医生选择"语义被错误码取代，与 §4.2 设计不符
- **影响**：WARN 路径下前端必须再次调用 `/api/prescription/audit` 端点才能拿到告警详情，违反"避免无意义重复审核"设计目标；新增的 `RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT` 错误码与 §10 错误码命名规范不一致（按规范应为 `RX_AUDIT_FORCE_SUBMIT_REQUIRED` 或类似命名，且与 `_AI_` 命名约定冲突——非 AI 业务码却使用了中段空格语义）。
- **建议**：(1) 在 `SubmitResponse` 中新增 `warnResult` 字段（DTO 含 riskLevel/alerts/auditRecordId/prescriptionHash 子结构）；(2) 当 forceSubmit=false + WARN + 内容匹配时，填充 `warnResult` 并设 `submitted=false`、`errorCode=null`；(3) 移除或重新命名 `RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT` 错误码，对齐 §5.1 错误码表。

### 一般

#### [一般] 降级路径下 LocalRuleResult 未转换为 AuditAlert，导致 AuditResponse.alerts 始终为空

- **位置**：`AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/service/audit/impl/PrescriptionAuditServiceImpl.java:96-103`
- **描述**：当 AI 调用失败/超时触发降级时，`PrescriptionAuditServiceImpl.audit()` 调用 `localRuleEngine.check(request)` 获取 6 条 `LocalRuleResult`，但随后 `AuditResponse` 的 `alerts`、`interactions`、`suggestions` 三个字段均被硬编码为 `Collections.emptyList()`（100-102 行）。未将 `LocalRuleResult` 转换为 `AuditAlert` 上报给前端。
- **影响**：降级路径下医生完全看不到哪条规则触发了 BLOCK/WARN，仅看到 `riskLevel=BLOCK` 这样的整体判定，无法定位具体问题（药物相互作用/过敏/重复用药/剂量超限等）；违反 §4.2 处方审核场景"降级: LocalRuleEngine.check()（5 条规则完整执行）"语义。
- **建议**：在降级分支中遍历 `ruleResults`，为 `passed=false` 的结果构造 `AuditAlert(ruleId, message, mapLocalSeverityToAlertSeverity(...))`，填充到 `response.setAlerts(...)`。`AuditIssue` DTO 已存在可复用。

#### [一般] AuditRecord.auditIssues 字段定义但 persistAuditRecord 从未写入，持久化层语义未闭环

- **位置**：`AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/entity/AuditRecord.java:50`、`AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/service/audit/impl/PrescriptionAuditServiceImpl.java:307-342`
- **描述**：`AuditRecord` 实体定义了 `auditIssues`（TEXT，JSON 序列化）字段，对应 `AuditIssue` DTO（`dto/audit/AuditIssue.java`，含 `fieldName/issueDescription/ruleId/severity`）。`persistAuditRecord` 方法仅写入 `aiResult`，未将降级路径的 6 条 `LocalRuleResult` 序列化为 `AuditIssue[]` JSON 写入 `auditIssues` 字段。整个 codebase 中 `setAuditIssues` 仅在 `AuditRecordTest.java:27`（测试）被调用，主代码 0 命中。
- **影响**：审核后端持久化数据缺失关键审计字段；`AuditIssue` DTO 与 `AuditAlert` 的映射关系（设计 §1.3 "alertCode ← ruleId + issueDescription 的编码化表达；alertMessage ← issueDescription；severity 共用"）未在持久化层落地。
- **建议**：在 `persistAuditRecord` 末尾增加 `if (fromFallback) { record.setAuditIssues(objectMapper.writeValueAsString(toAuditIssues(ruleResults))); }`，构造 `AuditIssue` 列表写入。

#### [一般] AuditRecord.prescriptionOrderId 字段在 forceSubmit=true 强制提交场景未回填业务单据号

- **位置**：`AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/service/audit/impl/PrescriptionAuditServiceImpl.java:231-245`
- **描述**：设计 §3.2 明确 `prescriptionOrderId: 处方单号（业务单据号，如 RX202606280001，提交成功时分配——BLOCK 阻断的审核记录此字段可为空；prescriptionId 与 prescriptionOrderId 的关系为 1:1）`——即提交成功时该 AuditRecord 应同时持有 prescriptionId + prescriptionOrderId。当前 forceSubmit=true 路径在 `auditRecordRepository.save(latestRecord)` 时只设置了 `forceSubmitted` 与 `forceSubmitTime`（232-234 行），未将 `SubmitResponse.prescriptionOrderId`（238 行生成的 `RX-{timestamp}`）回写到 `AuditRecord.prescriptionOrderId`。
- **影响**：审核记录与处方单号的 1:1 关系在持久化层断裂；后续按 `prescriptionOrderId` 检索（`AuditRecordRepository.findByPrescriptionOrderIdOrderByAuditSequenceDesc`）查不到该提交；处方追溯链断裂。
- **建议**：在第 238 行生成 `prescriptionOrderId` 后，调用 `latestRecord.setPrescriptionOrderId(prescriptionOrderId)` 并重新 save，或先 save 再更新。

#### [一般] PrescriptionItem 引入 `unit` 字段未在设计文档 §3.2 §1.3 业务层契约中定义

- **位置**：`AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/dto/audit/PrescriptionItem.java:11,64-70`
- **描述**：设计文档 §1.3 / §3.2 对 `PrescriptionItem` 字段定义为 `drugId/drugName/dose/frequency/duration/route` 六个字段；当前 `PrescriptionItem` 新增了 `unit` 字段（`PrescriptionItem.java:11`）。`PrescriptionAssistServiceImpl.parseDraftItems()` 从 AI 返回的 `prescriptionDraft.drugs[]` 中读取 `unit` 并填充（`PrescriptionAssistServiceImpl.java:245-246`）。
- **影响**：业务层契约与设计文档偏离——前端需额外传入 `unit` 字段才能正确填充 `unit`，但 `AuditRequest` 文档示例（`07_ood_phase2_C_3_DE.md:1194`）未列出 `unit` 字段；剂量校验（`DosageThresholdService.java:40`）按 `request.getUnit()` 与 `matched.getUnit()` 做字符串比较，缺失 `unit` 时会触发"单位不匹配"误报。
- **建议**：评估 `unit` 是否应作为 `PrescriptionItem` 一等字段（若是，则在 §3.2 文档中补充）；或者将其从 `PrescriptionItem` 中移除，由 `AuditRequest` 显式以 `unit` 顶级字段承载处方单位约定（所有药品共享）。

#### [一般] DosageThresholdService.matchByPriority 实现 6 级匹配优先级与设计 §8.4 决策表存在偏离

- **位置**：`AIMedical/backend/modules/prescription/src/main/java/com/aimedical\modules\prescription\service\assist\DosageThresholdService.java:95-147`
- **描述**：设计文档 §8.4 给出 6 级匹配优先级与部分 null 边界决策表（#5、#6 等场景），关键场景为"ageRange 部分 null + weightRange 均非 null 时直接降级至 Level 5"（决策表行 #5）。当前 `matchByPriority` 实现：
  - 第一重循环（102-113 行）：age + weight 双重非 null 时检查 ageRange+weightRange 全匹配——但此处 `exactAge`/`exactWeight` 分支判断的是"ageRangeStart==ageRangeEnd"而非"ageRangeStart==patientAge"，与 Level 1（精确匹配）语义不符
  - 第二重循环（114-121 行）：age + weight 双重非 null 范围匹配——与第一重循环逻辑完全相同（重复）
  - 第三重循环（Level 3 年龄范围匹配）：要求 `weightRangeStart==null && weightRangeEnd==null`，与决策表 #2/#3/#4 行（部分 null）不一致
  - 第四重循环（Level 4 体重范围匹配）：要求 `ageRangeStart==null && ageRangeEnd==null`，与决策表 #9/#11/#12 不一致
  - 第五重循环（Level 5 无分级默认阈值）：同时要求 age/weight 范围均 null
- **影响**：部分 null 边界场景（#2/#3/#4/#5/#6/#7/#8 等）下命中优先级与设计预期不同；特别是第一重循环（102-113 行）中 `if (exactAge && exactWeight) return ds;` 在 age+weight 双重精确匹配时正确返回，但第二重循环会重复处理同一组记录。
- **建议**：重写 `matchByPriority` 为顺序 6 级（1→2→3→4→5→6），每级按 §8.4 决策表精确实现；移除重复的第二重循环；增加对边界场景（#5/#6/#7/#8 等）的覆盖测试。

#### [一般] SpecialPopulationDosageRule 缺少与 DosageLimitRule 的差异化逻辑（重叠检查）

- **位置**：`AIMedical/backend/modules/prescription/src/main/java/com/aimedical\modules\prescription\rule\SpecialPopulationDosageRule.java:24-56`
- **描述**：设计文档 §3.2 明确规定 SpecialPopulationDosageRule 在特殊人群场景"超出该人群特殊剂量上限时输出 BLOCK 级别 LocalRuleResult"——逻辑上应查询"特殊人群分级对应的 DosageStandard"（如儿童/老人专项分级），而非与 `DosageLimitRule` 同样查询通用 DosageStandard。当前实现：
  - 第 35-39 行调用 `findByDrugCodeAndRouteOfAdministration` 查询所有 DosageStandard
  - 第 41-52 行遍历时仅判断 `age >= ageRangeStart && age <= ageRangeEnd`——这会同时命中"通用"和"特殊人群"分级，逻辑上与 DosageLimitRule 重叠
  - 未区分特殊人群分级（如 DosageStandard 中标记 isSpecialPopulation 或 ageRange=特殊值），导致对每个 age 在 14 岁/65 岁边界的患者，DosageLimitRule 和 SpecialPopulationDosageRule 会输出相同告警
- **影响**：与 DosageLimitRule 重复检查，未体现"特殊人群专项剂量上限"的设计意图；可能产生重复告警。
- **建议**：在 DosageStandard 实体增加 `specialPopulation` 标记字段（boolean 或 enum：CHILD/ELDERLY/GENERAL），或按 `ageRangeStart=0 && ageRangeEnd=14` 等约定识别儿童专项分级；`SpecialPopulationDosageRule` 仅查询特殊人群分级并校验。

#### [一般] DrugInteractionRule 缺少 Phase 4 预留标注，运行时启用/禁用语义模糊

- **位置**：`AIMedical/backend/modules/prescription\src\main\java\com\aimedical\modules\prescription\rule\DrugInteractionRule.java:12-14`
- **描述**：设计文档 §3.2 明确 "DrugInteractionRule（DDI）不在 Phase 2/3 本地规则范围内……DrugInteractionPair 实体标注为'Phase 4 预留，当前版本不建表'——在 @Entity 上增加 `@Table(schema = 'PHASE4_PRELOAD')` 条件建表控制"。当前 `DrugInteractionRule.check()` 简单返回 PASS：
  - 类级别无 `@Component` 限定 Phase 的元数据（如 `@ConditionalOnProperty(name = "phase.drug-interaction.enabled", havingValue = "true")`）
  - 方法注释缺失"Phase 4 预留"说明
  - `DefaultLocalRuleEngine.check()` 仍然无条件调用 `drugInteractionRule.check(request)`（`DefaultLocalRuleEngine.java:41`）
- **影响**：未来 Phase 4 启用 DDI 时缺少统一的开关机制；当前实现虽"行为正确"（返回 PASS）但语义与设计不匹配——设计 §7 设计决策表要求"DrugInteractionRule 运行时启用推迟至 Phase 4"。
- **建议**：在 `DrugInteractionRule` 类级别增加 `@ConditionalOnProperty(prefix = "prescription.rule", name = "drug-interaction.enabled", havingValue = "true", matchIfMissing = false)`；在 javadoc 中明确"Phase 4 启用，Phase 2/3 不参与运行时校验"。

#### [一般] AllergyCheckRule.文本回退与结构化匹配在传入数据上的语义耦合需更清晰

- **位置**：`AIMedical/backend/modules/prescription\src\main\java\com\aimedical\modules\prescription\rule\AllergyCheckRule.java:43-60`
- **描述**：设计文档 §3.2 明确 "AllergyCheckRule 严重程度分级：当 patientInfo 中存在 allergyDetails 时优先按结构化过敏信息做精确匹配；缺失时回退到 allergyHistory 文本匹配，文本匹配命中时一律输出 BLOCK 级别（保守策略）"。当前实现：
  - 第 43 行 `if (allergyDetails != null && !allergyDetails.isEmpty())`：当 allergyDetails 非空时进入结构化匹配分支，但即使结构化匹配未命中（如 allergen 不在 mapping 中），不会回退到文本匹配
  - 当 allergyDetails 为空或 null 时进入文本匹配分支（第 54 行 `else if (allergyHistory != null && !allergyHistory.isBlank())`）
  - 但若 allergyDetails 存在但患者该药品无对应 allergen（即结构化匹配未命中），代码不会进一步尝试文本匹配
- **影响**：当 allergyDetails 中 allergen 列表不全（如医生未录入全部过敏原）但 allergyHistory 文本中含有关键过敏原时，会漏报过敏冲突。
- **建议**：将结构化匹配与文本匹配改为"或"关系——遍历每个药品，先尝试结构化匹配，命中按分级输出；未命中时再尝试文本匹配，命中则按 BLOCK 输出。

#### [一般] PrescriptionAssistServiceImpl 阻断/降级路径未触发 PrescriptionDraftContext 写入

- **位置**：`AIMedical/backend/modules/prescription\src\main\java\com\aimedical\modules\prescription\service\assist\impl\PrescriptionAssistServiceImpl.java:81-99`
- **描述**：设计文档 §3.4 PrescriptionAssistService 职责明确"对 AI 产出执行本地即时校验（DosageThresholdService 剂量校验 + AllergyCheckRule 过敏冲突检查），将 CRITICAL 级别告警写入 PrescriptionDraftContext"。当前 `assist()` 方法在以下路径不写入 `PrescriptionDraftContext`：
  - 第 81 行 `InterruptedException` 路径 → 直接返回 `buildEmptyResponse`
  - 第 83 行 `ExecutionException` 路径 → 直接返回 `buildEmptyResponse`
  - 第 91 行 `aiResult.isSuccess() && aiResult.getData() != null` 为 false 时
  - 第 96 行 `!hasDrugs` 时（AI 返回空处方）
- **影响**：AI 不可用/失败/无推荐时，CRITICAL 阻断信息无法持久化到 PrescriptionDraftContext；前端在 AI 失败重试前无法看到阻断信息。
- **建议**：在 `assist()` 失败路径中即使 `aiData==null` 仍应将"AI 不可用"事件记录到上下文（用特殊 severity），便于处方提交时阻断语义统一。

#### [一般] AuditRequest 端点 PrescriptionAuditController.audit 未消费 fromFallback 标记即在响应中丢弃

- **位置**：`AIMedical/backend/modules\prescription\src\main\java\com\aimedical\modules\prescription\api\PrescriptionAuditController.java:36-48`
- **描述**：设计 §4.2 要求 AuditResponse 携带 fromFallback 标记，当前 PrescriptionAuditController.audit 直接在 `AuditResponse response = prescriptionAuditService.audit(request)` 后用 `response.getRiskLevel()` 判断 BLOCK，未将 `response.isFromFallback()` 透传给前端；且当降级路径下 BLOCK 时，`prescriptionAuditEnforcer.enforce()` 仅传入 `"Prescription audit blocked"` 这一个固定字符串作为 reasons，未包含 LocalRuleResult 触发的具体规则 ID 或告警消息。
- **影响**：前端无法区分"AI 判定 BLOCK"与"AI 不可用降级到规则引擎后判定 BLOCK"；阻断原因仅显示固定文本"Prescription audit blocked"，违反"前端弹窗展示阻断原因列表"的设计目标。
- **建议**：(1) AuditResponse 应确保 `fromFallback` 字段被序列化到 JSON 响应；(2) 当降级路径下 BLOCK 时，将 `LocalRuleResult` 列表（ruleId+message）转换为 reasons 列表传入 enforcer。

#### [一般] AuditRecordRepository 缺少 prescriptionOrderId 分组清理 isLatest 的方法

- **位置**：`AIMedical/backend\modules\prescription\src\main\java\com\aimedical\modules\prescription\repository\AuditRecordRepository.java:1-22`、`AIMedical/backend/modules\prescription/src/main/java/com/aimedical/modules/prescription/service/audit/impl/PrescriptionAuditServiceImpl.java:310-315`
- **描述**：设计文档 §3.2 AuditRecord 明确"同一处方多次审核时的 isLatest 管理：采用'按业务主键分组'模式——优先按 prescriptionOrderId 分组清理 isLatest 标记；当 prescriptionOrderId 为空（BLOCK 阻断场景）时，按 prescriptionId 分组执行相同的 isLatest 清理逻辑"。当前：
  - `AuditRecordRepository` 仅提供 `findByPrescriptionIdAndIsLatestTrue`（按 prescriptionId 分组），无 `findByPrescriptionOrderIdAndIsLatestTrue` 对应的清理方法
  - `persistAuditRecord` 仅按 `prescriptionId` 分组清理 isLatest（`PrescriptionAuditServiceImpl.java:310-315`），未实现按 `prescriptionOrderId` 分组清理
  - 当同一 prescriptionOrderId 下有不同 prescriptionId（极端情况：处方版本多次提交）时，isLatest 标记会错乱
- **影响**：提交成功后 prescriptionOrderId 已分配，第二次审核时仍按 prescriptionId 清理 isLatest，可能与 prescriptionOrderId 分组清理逻辑不一致。
- **建议**：在 `AuditRecordRepository` 增加 `findByPrescriptionOrderIdAndIsLatestTrue(prescriptionOrderId)` 方法；在 `persistAuditRecord` 中先尝试按 prescriptionOrderId 清理（若非空），回退按 prescriptionId 清理。

### 轻微

#### [轻微] PrescriptionDraftContext 内部 key 命名约定（prescriptionId + ":criticalAlerts"）暴露在业务代码而非集中常量

- **位置**：`AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/context/PrescriptionDraftContext.java:11`
- **描述**：`CRITICAL_ALERTS_SUFFIX = ":criticalAlerts"` 是私有 static final 常量，但 `DraftContextStore` 接受 `String key` 作为通用 key，与设计 §6.1 期望的"按 prescriptionId 关联"语义强耦合；其它模块若需访问同样 key 需硬编码。
- **建议**：保留当前实现（私有常量已封装），但可在代码注释中明确"此 key 命名仅供 DraftContextStore 内部使用"。

#### [轻微] DedupTaskScheduler 中 DEDUP_KEY_PREFIX 与 prescriptionId 拼接无字符转义

- **位置**：`AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/service/assist/DedupTaskScheduler.java:22`
- **描述**：`String dedupKey = DEDUP_KEY_PREFIX + prescriptionId;`——若 prescriptionId 含 `:` 等特殊字符（虽然 UUID v4 不含）可能导致 key 命名冲突。当前前端生成 UUID v4 不会触发，但 DosageCheckRequest.prescriptionId 后端兜底时也用 `UUID.randomUUID().toString()`，无注入风险。
- **建议**：保持现状即可；如后续支持非 UUID 格式的 prescriptionId（如业务单号），应做 key 编码（如 `Base64.getEncoder().encodeToString(...)`）。

#### [轻微] AuditResponse 缺少数组字段的非空保护

- **位置**：`AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/dto/audit/AuditResponse.java:9-11`
- **描述**：`alerts/interactions/suggestions` 字段为 `List<...>` 但未在 setter 中做 null 保护。`AuditConverter.toAuditResponse()` 可能在某些边界（如 aiResult.data.alerts==null）下未显式调用 `setAlerts(null)`，导致 JSON 序列化时字段缺失（应序列化为 `null` 而非 `[]`）。
- **建议**：AuditResponse 字段在构造器或 setter 中做 null-safe 默认值（`setAlerts(List.of())`），或保持当前 JSON null 语义并文档化。

#### [轻微] PrescriptionAssistServiceImpl.checkAllergies 字符串解析 DrugId 逻辑脆弱

- **位置**：`AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/service/assist/impl/PrescriptionAssistServiceImpl.java:269-298`
- **描述**：`checkAllergies()` 从 `LocalRuleResult.message` 中通过字符串分割（`msg.split(" to ")`、`indexOf(" for drug ")`）解析 drugId 和 allergen——若 AllergyCheckRule 输出消息格式变更（如国际化或多语言），本解析将失效。
- **建议**：复用 AllergyCheckRule 输出结构化结果（如扩展 LocalRuleResult 增加 drugId 字段），或 AllergyCheckRule 增加 `checkAll(AuditRequest) -> List<LocalRuleResult>` 多结果方法。

#### [轻微] AuditRiskLevel 持久化为 String 而非枚举

- **位置**：`AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/entity/AuditRecord.java:44`、`AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/service/audit/impl/PrescriptionAuditServiceImpl.java:184,294`
- **描述**：`AuditRecord.riskLevel` 存储为 `String`（写入时调用 `response.getRiskLevel().name()`），读取时反向通过 `AuditRiskLevel.valueOf(...)` 解析；如 `riskLevel` 数据库中拼写错误或新增枚举值未同步，会抛 `IllegalArgumentException`。
- **建议**：考虑使用 JPA `AttributeConverter<AuditRiskLevel, String>` 或保持 String 但在读取失败时回退到 PASS（`PrescriptionAuditServiceImpl.java:186` 已实现此回退，但 `revoke()` 中第 295-297 行的回退是抛 BusinessException——两处行为不一致）。

#### [轻微] PrescribeAssistRequest 业务层 prescriptionId 字段在 /assist 端点无 @NotBlank 校验

- **位置**：`AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/dto/assist/PrescriptionAssistRequest.java:21`
- **描述**：设计文档 §3.4 "prescriptionId 在处方编辑生命周期开始前由前端预创建（UUID v4），首次 check-dose 调用时即传入……若极端场景下前端未预创建 prescriptionId（如离线客户端异常），后端在首次 check-dose 调用检测到 prescriptionId 为空时自动生成 UUID v4"。但 `PrescriptionAssistRequest.prescriptionId` 在 /assist 主端点也未做 API 层 @NotBlank 校验（设计 §3.4 明确"主路径校验由业务层在 Service 方法内检查非空而非 API 层拦截"）——当前 Service 层在 `assist()` 入口直接 `setPrescriptionId(UUID.randomUUID().toString())`（`PrescriptionAssistServiceImpl.java:68-70`），符合设计"业务层兜底"语义，但应增加显式注释说明此处非 API 校验。
- **建议**：在 `prescriptionId` 字段 javadoc 注释中明确"主路径下为空时由 Service 层兜底生成 UUID v4，参见 PrescriptionAssistServiceImpl.assist() 第 68-70 行"。

#### [轻微] DosageCheckRequest.frequency 为 String 类型，按 Integer 解析易触发 NumberFormatException 静默吞掉

- **位置**：`AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/service/assist/DosageThresholdService.java:74-90`
- **描述**：`DosageCheckRequest.frequency` 字段类型为 `String`，但日剂量校验分支按 `Integer.parseInt(request.getFrequency())` 解析。`catch (NumberFormatException e) {}` 静默吞掉异常——若前端传 `"bid"`（每日两次的标准医学缩写），日剂量校验直接跳过而不给提示。
- **建议**：将 `frequency` 字段类型改为 `Integer` 并增加 `@Positive` 校验；或保留 String 类型但显式支持 `bid/tid/qid` 等标准缩写映射（2/3/4），解析失败时记录 WARN 日志。

## 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 5 |
| 一般 | 10 |
| 轻微 | 8 |

## 总评

prescription 模块整体结构清晰，与设计文档 §2.1 目录结构、§2.2 模块依赖方向、§3.2/§3.3 抽象层次基本一致——`PrescriptionAuditService`/`PrescriptionAssistService` interface + impl 模式、`LocalRuleEngine` 6 条规则拆分（AllergyCheckRule / ContraindicationCheckRule / DuplicateCheckRule / DosageLimitRule / SpecialPopulationDosageRule / DrugInteractionRule 预留骨架）、`DosageThresholdService` 单药品剂量校验、`DraftContextStore`/`SuggestionStore` Store 抽象层引用、AuditRecord `@Version` 乐观锁 + isLatest 分组、AuditResponse/PrescriptionAssistResponse 与 ai-api DTO 命名空间隔离（通过 Converter）等核心契约均落地。

但本轮发现 5 个**严重**问题集中在"运行时机制未实现"：异步 AI 调度（`@Async` 缺失）、DrugFacade 跨模块门面未注入、TTL 清理（`@Scheduled` 缺失）、规则变更事件（`@EventListener` 缺失）、提交端点 WARN 路径契约偏离（错误码未定义 + warnResult 字段缺失）。这 5 项均直接破坏包E 端到端流程与 prescription ↔ admin/drug 跨模块协作，建议作为 Phase 2/3 上线前的必修 blocker。

`LocalRuleEngine` 6 条规则的实现整体符合设计：AllergyCheckRule 结构化+文本回退分级、ContraindicationCheckRule ABSOLUTE/RELATIVE 分级、DuplicateCheckRule ingredientCode 匹配、DosageLimitRule 2 倍阈值分级、SpecialPopulationDosageRule 儿童/老年边界、DosageThresholdService 6 级匹配优先级基本对齐；DrugInteractionRule 骨架预留（设计 §3.2 标注 Phase 4 不启用）。但 DosageThresholdService.matchByPriority 6 级实现与设计 §8.4 决策表部分 null 场景存在偏离，SpecialPopulationDosageRule 与 DosageLimitRule 重复检查。

`PrescriptionDraftContext` 全量覆盖 CRITICAL 告警、`getContextCriticalCount()` 接口、`contextCriticalCount` 在 DosageCheckResponse 回写、`prescriptionId` 兜底生成 UUID v4（`PrescriptionAssistServiceImpl.assist()/checkDose()`）等关键行为均按设计实现；AiResult 降级判定（`success=false` 或 `degraded=true`）在 PrescriptionAuditServiceImpl.audit() 第 92-104 行正确处理。

错误码命名符合 §10 规范（`RX_AUDIT_*` / `RX_ASSIST_*` 前缀），但 `RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT`（`PrescriptionErrorCode.java:9`）为设计外新增，需在 §5.1 错误码表中补登或移除。

DTO 字段与设计文档 §1.3 / §3.2 / §3.3 基本对齐——所有 §1.3 列出的抽象均有对应实现，命名空间严格区分（`com.aimedical.modules.prescription.dto.audit.*` vs `com.aimedical.modules.ai.api.dto.prescription.*`）。PrescriptionItem 新增 `unit` 字段（设计未列）需评估是否在 §3.2 中补登或回退。
