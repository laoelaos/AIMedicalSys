# 代码审查问题诊断报告

## P0 — 必须立即修复

### consultation 模块

#### C01: TriageRecord 实体缺失 correctedChiefComplaint 快照字段

- **现象**：TriageRecord 实体无 `correctedChiefComplaint` 字段，AI 修正后的主诉无法持久化
- **根因**：`TriageRecord.java:14-144` 实体定义中缺少该字段。TriageServiceImpl.java:271 调用 `session.getCorrectedChiefComplaint()` 赋值给 record，但实体无此属性，编译期应报错或运行时丢失数据
- **因果链**：AI 修正主诉 → session.setCorrectedChiefComplaint() → saveTriageRecord 写入 → 实体无字段 → 数据丢失
- **影响**：correctedChiefComplaint 数据流群组（C01→A04→C03→C19→C23）的起点缺失，后续所有依赖此字段的逻辑均无法工作

#### C02: TriageRecordRepository 缺少恢复查询方法

- **现象**：DialogueSessionManager.restoreSession 无法从 DB 恢复会话
- **根因**：`TriageRecordRepository.java:11-17` 缺少 `findTopBySessionIdOrderByTriageTimeDesc` 方法。DialogueSessionManager.java:60 调用此方法，但 Repository 未定义
- **因果链**：sessionStore 中无 session → restoreSession 查 DB → Repository 无此查询方法 → 启动失败或返回空

#### C04: TriageServiceImpl 缺少 @Transactional 边界

- **现象**：saveTriageRecord 中 DB 操作无事务保护
- **根因**：`TriageServiceImpl.java:34-35` 的 triage 方法标注了 `@Transactional`，但 saveTriageRecord 内部使用 `transactionTemplate.execute()`（第257行）包围 save 操作。triage 方法本身的 `@Transactional` 覆盖了整个方法（含 AI 调用），事务粒度过大。selectDepartment 方法（第191行）也标注了 `@Transactional`，但 saveTriageRecord 已独立使用 TransactionTemplate
- **修复方向**：triage 方法应移除 `@Transactional`，仅依赖 TransactionTemplate 包围 save；selectDepartment 的 `@Transactional` 保留

#### C05: chiefComplaint 与 additionalResponses 互斥校验缺失

- **现象**：TriageServiceImpl.java:82-90 的互斥校验逻辑：`hasChiefComplaint == hasAdditional` 时抛异常。此逻辑在两者同时为空或同时非空时触发
- **根因**：当前代码已实现互斥校验（第87-90行），但错误码 `TRIAGE_FIELD_COMBINATION_INVALID` 需确认是否已定义。校验逻辑本身已存在，此条可能为已修复项

#### C06: DoctorFacade 跨模块调用无 try/catch、无超时、无 WARN 日志

- **现象**：TriageServiceImpl.java:213-236 的 findDoctorsForDepartments 方法
- **根因**：代码验证——第226行已有 `catch (Exception e)` 和第228-229行的 WARN 日志（含耗时）。但超时未实现：`application.yml:29` 配置了 `consultation.doctor-facade.timeout: 2`，但 TriageServiceImpl 未注入此值，doctorFacade.findAvailableDoctorsByDepartment() 调用无超时控制
- **因果链**：doctorFacade 调用 → 无超时 → 下游服务阻塞 → 线程耗尽
- **已修复**：try/catch 和 WARN 日志已存在；**未修复**：超时未注入

#### C07: AI 调用 future.get() 无超时参数

- **现象**：TriageServiceImpl.java:124 的 `future.get(aiTimeout, TimeUnit.SECONDS)` 已使用注入的超时值
- **根因**：代码验证——第68行 `@Value("${ai.timeout.triage:8}") long aiTimeout` 已注入，第124行已使用。`application.yml:19` 配置 `ai.timeout.triage: 8`。此条已修复

#### C08: selectDepartment 签名为 4 参、RegistrationEventListener 未调用该方法

- **现象**：TriageService 接口 selectDepartment 签名与调用方不匹配
- **根因**：TriageServiceImpl.java:192-193 的 selectDepartment 方法签名为 3 参（sessionId, departmentId, departmentName），RegistrationEventListener.java:47 调用的也是 3 参版本。TriageService.java:10 的接口定义需确认是否仍为 4 参。若接口仍为 4 参（含 overwrite），则实现类与接口不一致

#### C15: RegistrationEventListener @Retryable 对所有 Exception.class 重试

- **现象**：@Retryable 重试范围过宽
- **根因**：RegistrationEventListener.java:41-43 已限定 `retryFor = {DataAccessException.class, TimeoutException.class}`，并添加 `noRetryFor = {IllegalArgumentException.class, NullPointerException.class}`。此条已修复

#### C20: createSession 使用 put 而非 putIfAbsent

- **现象**：DialogueSessionManager.java:42 使用 `sessionStore.put(sessionId, session)` 覆盖已有 session
- **根因**：createSession 方法（第33行）已加 `synchronized`，且第37-39行先检查 `containsKey` 再 put。但在并发场景下，synchronized 保护了整个方法，containsKey+put 在 synchronized 内是原子的。不过 putIfAbsent 语义更清晰且不依赖 synchronized 的正确性。restoreSession（第50行）无 synchronized，第68行的 put 可能与 createSession 的 put 产生竞态

#### C21: 降级路径使用 request 参数而非 session 快照的 ruleVersion/ruleSetId

- **现象**：TriageServiceImpl.java:157-158 降级路径调用 `triageRuleEngine.match(request.getChiefComplaint(), session.getRuleVersion(), session.getRuleSetId())`
- **根因**：代码验证——第157-158行实际使用的是 `session.getRuleVersion()` 和 `session.getRuleSetId()`，而非 request 参数。此条已修复

#### E02: TriageRecord 同 sessionId 第二次分诊违反 @Column(unique=true) 约束

- **现象**：saveTriageRecord 始终 INSERT 不 UPDATE
- **根因**：TriageServiceImpl.java:258-265 的 saveTriageRecord 逻辑——先 `findBySessionId` 查询，若存在则复用 existing record（第262行），否则新建。第289行 `triageRecordRepository.save(record)` 使用 Spring Data JPA 的 save，对已有 ID 的实体执行 merge（UPDATE）。但若 sessionId 字段有 `@Column(unique=true)` 约束，且 findBySessionId 查询因事务隔离级别问题未查到已有记录，则 INSERT 会违反唯一约束
- **因果链**：并发请求同一 sessionId → findBySessionId 在事务外执行（TransactionTemplate 仅包围 save）→ 两次均未查到 → 两次 INSERT → 唯一约束冲突

#### S04: 同 session 并发访问无串行化保护

- **现象**：restoreSession 缺 synchronized，createSession 用 put 而非 putIfAbsent
- **根因**：DialogueSessionManager.java:50 的 restoreSession 无 synchronized 标注。createSession（第33行）有 synchronized，但 restoreSession（第50行）无。并发 restoreSession 可能在第54行同时读到 null，然后第68行同时 put 不同 session 实例，导致数据丢失

### prescription 模块

#### A01: AiResultFactory 在业务 Service 中零引用

- **现象**：4 个业务 Service 手工构造 AiResult 而非使用 AiResultFactory
- **根因**：
  - TriageServiceImpl.java:30 已 import AiResultFactory，第208/210行使用 `AiResultFactory.degraded()`。**TriageServiceImpl 已修复**
  - PrescriptionAuditServiceImpl.java:96-105 AI 调用失败时 `aiResult` 被设为 `null`，而非使用 `AiResult.degraded()` 或 `AiResultFactory`。降级路径（第116行）直接使用 localRuleEngine，不构造 AiResult
  - MedicalRecordServiceImpl.java:18 已 import AiResultFactory，第151/154/157行使用 `AiResultFactory.degraded()`。**MedicalRecordServiceImpl 已修复**
- **未修复**：PrescriptionAuditServiceImpl 的 AI 失败路径设 aiResult=null 而非使用 AiResultFactory 构造降级结果

#### P01: 异步 AI 调度机制未实现

- **现象**：schedule 创建 PENDING 后无代码触发 AiService.prescriptionAssist()
- **根因**：DedupTaskScheduler.java:21-69 的 schedule 方法仅创建 PENDING 状态的 AiSuggestionResult 并存入 SuggestionStore，无任何代码触发实际的 AI 调用。PrescriptionAssistServiceImpl.java:105 调用 `scheduleSuggestionAsync(taskId, request)` 启动异步 AI 调用，但 scheduleSuggestionAsync（第339-369行）使用 `CompletableFuture.supplyAsync()` 启动异步任务，未使用线程池，且返回的 CompletableFuture 未被任何代码消费（第369行无 return 或赋值），异步任务的异常无法被捕获
- **因果链**：assist() 调用 → scheduleSuggestionAsync 启动异步任务 → 异步任务内部调用 aiService.prescriptionAssist() → 结果写入 suggestionStore → 但异步任务未绑定线程池（使用 ForkJoinPool.commonPool()）且异常未处理

#### P02: DrugFacade 在 PrescriptionAuditServiceImpl 和 PrescriptionAssistServiceImpl 均未注入

- **现象**：两个 Service 均缺少 DrugFacade 依赖
- **根因**：PrescriptionAuditServiceImpl.java:72-87 构造函数无 DrugFacade 参数；PrescriptionAssistServiceImpl.java:57-75 构造函数无 DrugFacade 参数。DrugFacade 的药品数据查询功能在两个 Service 中均不可用

#### P03/S02: AiSuggestionResult + PrescriptionDraftContext TTL 清理任务失效

- **现象**：SuggestionCleanupTask 和 DraftContextCleanupTask 均无法有效清理过期数据
- **根因（需分别诊断两个独立清理任务）**：

**DraftContextCleanupTask 失效原因**：
- DraftContextCleanupTask.java:20 的 `writeTimestamps` 是本地 ConcurrentHashMap，第27行的 `recordWrite()` 方法无任何调用方（全项目搜索确认）。因此 writeTimestamps 永远为空
- cleanupExpiredDrafts（第36-46行）遍历 draftContextStore.keySet()，对每个 key 查 writeTimestamps.get(key)，因 writeTimestamps 为空，ts 始终为 null，第40行条件 `ts != null` 永远不满足，无任何条目被清理
- **根因定位**：DraftContextCleanupTask.recordWrite() 无调用方 → writeTimestamps 永远空 → 清理条件永远不满足

**SuggestionCleanupTask 失效原因**：
- AiSuggestionResult.java:8 已声明 `implements SuggestionStoreEntry`，完整实现了 `getStatusName()`、`isConsumed()`、`getTimestamp()` 三个接口方法。`instanceof SuggestionStoreEntry` 检查会成功，不会抛出 ClassCastException
- SuggestionCleanupTask.isExpiredAndConsumed（第42-47行）要求三个条件同时满足：(1) status=COMPLETED 或 FAILED；(2) isConsumed()=true；(3) getTimestamp() + TTL < now
- **条件(2)分析**：consumed 标志仅在 PrescriptionAssistServiceImpl.getSuggestion():219 中对 COMPLETED 状态设置 `result.setConsumed(true)`。FAILED 状态的 AiSuggestionResult 的 consumed 永远为 false（boolean 默认值，无代码设置）。因此 FAILED 状态的条目永远不会被清理
- **条件(3)分析**：scheduleSuggestionAsync（PrescriptionAssistServiceImpl.java:339-369）中创建的 AiSuggestionResult（第341行）未调用 `setCreateTime()`，因此 `createTime` 为 null。AiSuggestionResult.getTimestamp()（第83-85行）在 createTime 为 null 时返回 null。isExpiredAndConsumed 第46行 `entry.getTimestamp().plusSeconds(...)` 对 null 调用方法，抛出 NullPointerException。此 NPE 被 cleanupExpiredSuggestions 第36行的 `catch (ClassCastException e)` 捕获——但 NPE 不是 ClassCastException，不会被此 catch 块捕获，而是向上传播到 for 循环外，导致整个清理方法异常终止，后续条目不会被处理
- **根因定位**：SuggestionCleanupTask 失效有两个独立原因：(a) scheduleSuggestionAsync 未设置 createTime → getTimestamp() 返回 null → isExpiredAndConsumed 中 NPE → 清理方法异常终止；(b) 即使 NPE 被修复，FAILED 状态的 consumed 永远为 false，FAILED 条件不会被清理

**P03/S02 与 T32 的关系**：T32 描述的是 DraftContextCleanupTask 的清理失效（writeTimestamps 永远空），P03/S02 描述的是 SuggestionCleanupTask 的清理失效。两者是独立的清理任务，失效原因不同，不应合并为同一根因

#### P04: 规则变更事件未定义、未监听

- **现象**：DrugContraindicationChangeEvent/DrugAllergyMappingChangeEvent/DrugCompositionDictChangeEvent 未定义
- **根因**：prescription/event/ 包不存在，无事件定义和监听器。规则变更后缓存无法主动失效

#### S03: DedupTaskScheduler.schedule() 在 compute lambda 内嵌套跨 key 操作

- **现象**：compute lambda 内嵌套 suggestionStore.put(candidateTaskId, ...) 破坏原子性
- **根因**：DedupTaskScheduler.java:51-62 的 compute lambda（第51-59行）操作 dedupKey，但第62行在 compute 返回后执行 `suggestionStore.put(candidateTaskId, newResult)`。实际上第62行的 put 在 compute 外部（第61-63行的 if 块），不在 compute lambda 内部。但第40行的 `suggestionStore.put(candidateTaskId, newResult)` 在 createIfNotExists 成功后执行，与 createIfNotExists 不在同一原子操作中，存在竞态：createIfNotExists 成功（返回 null）后、put 执行前，另一个线程可能操作 candidateTaskId

### medical-record 模块

#### A02: 所有 AI 调用超时完全未外化

- **现象**：MedicalRecordServiceImpl 中超时硬编码
- **根因**：MedicalRecordServiceImpl.java:40-44 已使用 `@Value` 注入超时值（`ai.timeout.medical-record-generate:12` 和 `medical-record.visit-facade.timeout:2`），application.yml:21-22 和 32-33 已配置。此条已修复

#### A03: AiSuggestionResult 5 状态映射表全部未实现

- **现象**：异步 AI 完成后无 PENDING→COMPLETED/FAILED 回填
- **根因**：AiSuggestionStatus 枚举仅定义 PENDING/COMPLETED/FAILED 三种状态（AiSuggestionStatus.java:3-4），缺少 OOD 要求的 PROCESSING 和 TIMEOUT 状态。scheduleSuggestionAsync（PrescriptionAssistServiceImpl.java:339-369）在异步任务中直接设置 COMPLETED 或 FAILED，无中间状态转换

#### M01: 4 个错误码缺失

- **现象**：MedicalRecordErrorCode 缺少 MR_GEN_AI_UNAVAILABLE/MR_GEN_AI_INPUT_INVALID/MR_GEN_AI_OUTPUT_INCOMPLETE/MR_GEN_TEMPLATE_LOAD_FAILED
- **根因**：MedicalRecordErrorCode.java:5-9 定义中缺少这 4 个错误码

#### M04: MR_GEN_CONCURRENT_MODIFICATION 在当前 INSERT 路径中无法触发

- **现象**：MedicalRecordServiceImpl.java:113-119 的 ObjectOptimisticLockingFailureException catch 块在 INSERT 路径中不会触发
- **根因**：MedicalRecordServiceImpl.java:102-103 使用 `findByVisitId().orElseGet(MedicalRecord::new)`，当记录不存在时创建新实体（无 ID），save 执行 INSERT 而非 UPDATE。INSERT 不会触发乐观锁冲突。此错误码仅在 UPDATE 路径（记录已存在）中有意义

### application / 跨模块

#### C03/A04: AI 隐式 correctedChiefComplaint 路径未实现 + 显式透传路径未生效

- **现象**：TriageConverter 未透传 correctedChiefComplaint
- **根因**：TriageConverter.java:63-65,107-109 的转换逻辑需验证是否正确处理 correctedChiefComplaint 字段

#### A10: application.yml 缺失配置项

- **现象**：application.yml 缺少 ai.timeout.*/facade.*.timeout/ai.mock.* 等配置
- **根因**：代码验证——application.yml:17-33 已配置 `ai.timeout.triage/prescription-audit/medical-record-generate/prescription-assist`、`ai.mock.response-strategy`、`consultation.doctor-facade.timeout`、`medical-record.visit-facade.timeout`。但缺少 `facade.*.timeout` 的统一前缀配置（当前使用 `consultation.doctor-facade.timeout` 和 `medical-record.visit-facade.timeout` 分散配置）

---

## P1 — 严重影响业务逻辑

### consultation 模块

#### C09: selectDepartment 使用 GlobalErrorCode.NOT_FOUND 而非业务级错误码

- **根因**：TriageServiceImpl.java:195 使用 `TriageErrorCode.TRIAGE_SESSION_NOT_FOUND`（代码验证已使用业务错误码）。此条已修复

#### C12: AI 上下文未按模板拼接全量历史

- **根因**：TriageConverter.java:22-68 的 toAiTriageRequest 方法未实现 correctedChiefComplaint 替换和 3000 字符截断逻辑

#### C13: TriageRuleEngine.match 无快照失效回退逻辑

- **根因**：DefaultTriageRuleEngine.match 方法缺 ruleVersionMismatch 输出标记

#### C16: DefaultTriageRuleEngine.match 未实现 conditions JSON 关键词解析

- **根因**：DefaultTriageRuleEngine.java:82-116 的 JSON 解析失败时静默返回 true，导致规则无条件匹配所有主诉

#### C17: findDoctorsForDepartments 未按匹配评分排序取前 5 名

- **根因**：TriageServiceImpl.java:232-235 使用 `availableSlotCount` 降序排序，而非 OOD 要求的 score 排序。RecommendedDoctor 构造时 score 硬编码为 0f（第224行）

#### C19: saveTriageRecord 未读取 session.getCorrectedChiefComplaint()

- **根因**：TriageServiceImpl.java:271 已调用 `record.setCorrectedChiefComplaint(session.getCorrectedChiefComplaint())`。但 TriageRecord 实体缺少此字段（C01），赋值无效

#### C14: DeadLetterCompensationService 未检查 retryCount >= maxRetryCount 时的 EXPIRED 状态迁移

- **根因**：DeadLetterCompensationService.java:31-34 已在循环开头检查 `retryCount >= maxRetryCount` 并迁移为 EXPIRED。第49-50行在 catch 块中也检查并迁移。此条已修复

#### C22: TriageController.selectDepartment 写死 overwrite=true

- **根因**：需验证 TriageController.java:34 的实现

#### T4: TriageServiceImpl 降级路径手工构造 Response 而非复用 Converter

- **根因**：TriageServiceImpl.java:167-181 降级路径手工构造 TriageResponse，遗漏 matchedRules 字段

#### T5: TriageConverter 副作用修改 Session + DialogueSession 混用同步机制

- **根因**：TriageConverter.java:107-109 修改 Session 状态（副作用），DialogueSession 混用 synchronized/AtomicInteger/CopyOnWriteArrayList

#### T16: PrescriptionDraftContext.hasCriticalAlerts 与 getCriticalAlerts 缺少原子操作

- **根因**：PrescriptionDraftContext.java:19-22 的 hasCriticalAlerts 调用 getCriticalAlerts 获取列表后判空，PrescriptionAuditServiceImpl.java:152-154 先调 hasCriticalAlerts 再调 getCriticalAlerts，两次调用间列表可能被并发修改（TOCTOU 窗口）

#### T42: DefaultTriageRuleEngine 缓存仅依赖 refreshAfterWrite 无主动刷新

- **根因**：DefaultTriageRuleEngine.java:30-37 的 Caffeine 缓存仅配置 refreshAfterWrite，无事件驱动失效机制

### prescription 模块

#### P05: SubmitResponse 缺 warnResult 字段

- **根因**：SubmitResponse 缺少 warnResult 字段定义，WARN 路径使用未定义的错误码

#### P06: 降级路径 LocalRuleResult 未转换为 AuditAlert

- **根因**：PrescriptionAuditServiceImpl.java:116-130 降级路径中 LocalRuleResult 已转换为 AuditAlert（第121-125行）。此条已修复

#### P07: AuditRecord.auditIssues 字段从未写入

- **根因**：PrescriptionAuditServiceImpl.java:391-429 的 persistAuditRecord 方法中，auditIssues 字段在 fromFallback 和 AI 成功路径下均有写入逻辑（第392-429行）。此条已修复

#### P08: forceSubmit 路径未回写 prescriptionOrderId 到 AuditRecord

- **根因**：PrescriptionAuditServiceImpl.java:258-285 的 forceSubmit 路径中，第260行 `latestRecord.setPrescriptionOrderId(orderId)` 已回写。但 SubmitRequest 无 prescriptionHash 字段，prescriptionsMatch 使用 JSON 序列化比较而非 hash

#### P10: DosageThresholdService.matchByPriority 循环逻辑重复

- **根因**：DosageThresholdService.java:103-121 第一、二重循环逻辑重复

#### P11: SpecialPopulationDosageRule 与 DosageLimitRule 使用相同通用查询

- **根因**：SpecialPopulationDosageRule.java:36 使用与 DosageLimitRule 相同的 DosageStandard 查询，未区分特殊人群与通用剂量标准

#### P13: AllergyCheckRule 结构化匹配未命中时不回退到文本匹配

- **根因**：AllergyCheckRule.java:43-60 结构化匹配失败后无文本匹配回退

#### P14: PrescriptionAssistServiceImpl 各失败路径未写入 PrescriptionDraftContext

- **根因**：PrescriptionAssistServiceImpl.java:90-99,108-111,114-118 的失败路径调用 clearCriticalAlerts（清空 alerts），而非写入失败状态到 DraftContext

#### P16: AuditRecordRepository 没有 List 版本查询方法

- **根因**：AuditRecordRepository.java:18 缺少 `List<AuditRecord> findByPrescriptionOrderIdAndIsLatestTrue(String orderId)` 方法。PrescriptionAuditServiceImpl.java:264 调用了此方法

#### S06: DedupTaskScheduler.schedule() 返回值从 Object 强制转换

- **根因**：DedupTaskScheduler.java:64-67 的 `result instanceof AiSuggestionResult winner` 使用 pattern matching，非强制转换。但 compute 返回 Object 类型，语义上仍依赖类型假设

#### S07: ConcurrentHashMapStore 同时实现 SuggestionStore 和 DraftContextStore

- **根因**：ConcurrentHashMapStore.java:10 仅实现 SuggestionStore，未实现 DraftContextStore。但两者共用同一 ConcurrentHashMap 实例的问题需验证——DraftContextStoreImpl.java:11 有独立的 ConcurrentHashMap，两者数据隔离

#### T8: AllergyCheckRule + AllergyDetail 跨模块依赖

- **根因**：rule/AllergyCheckRule.java:3 和 dto/audit/AllergyDetail.java:3 依赖 patient 模块的 AllergySeverity

#### T9: DosageThresholdService 频率解析 Integer.parseInt("tid") 导致静默失效

- **根因**：DosageThresholdService.java:76-89 的 parseInt 对非数字频率字符串抛 NumberFormatException，被空 catch 吞掉，日剂量校验静默失效

#### T10: PrescriptionDraftContext.getCriticalAlerts unchecked cast

- **根因**：PrescriptionDraftContext.java:24-32 的 `@SuppressWarnings("unchecked")` 下 `(List<DosageAlert>) value` 无泛型类型校验

#### T14: PrescriptionItem.dose 使用 double 存在精度风险

- **根因**：dto/audit/PrescriptionItem.java:7 的 dose 字段使用 double，OOD §1.3 要求 BigDecimal

#### T15: prescriptionOrderId 使用 System.currentTimeMillis() 存在冲突风险

- **根因**：PrescriptionAuditServiceImpl.java:218,259,289,298,328 五处使用 `"RX-" + System.currentTimeMillis()` 生成 ID，毫秒级并发可能重复

#### T18: MedicalRecordConverter 使用字面字符串比较错误码

- **根因**：MedicalRecordConverter.java:69,73 使用 `"MR_GEN_AI_TIMEOUT"` 字面字符串比较，应使用 MedicalRecordErrorCode 枚举

#### T19: MockAiService.TIMEOUT 策略使用永不完成的 CompletableFuture

- **根因**：MockAiService.java:67 的 `new CompletableFuture<>()` 永不完成，调用方的 future.get() 会一直阻塞直到超时。测试场景下若未设置超时，线程资源泄漏

#### T20: FallbackAiService.applyStrategies 使用空 DegradationContext

- **现象**：FallbackAiService.java:290-301 的 applyStrategies 方法使用空 DegradationContext（第294行 `new DegradationContext()` 无 serviceName/operationName）
- **根因**：需区分两个独立的 DegradationContext 使用场景：
  1. **delegate 选择阶段**（selectDelegate，第66-80行）：各业务方法（如 triage 第87-89行）在调用 selectDelegate 前正确设置了 DegradationContext 的 serviceName/operationName。selectDelegate 使用正确的 context 做降级筛选
  2. **结果后处理阶段**（applyStrategies，第290-301行）：在 thenApply 中对已获取的 AiResult 做降级标记，使用空 DegradationContext。此空 context 的影响仅限于"降级标记无法基于服务名/操作名做精细化决策"，不影响 delegate 选择逻辑
- **影响范围**：applyStrategies 的空 DegradationContext 导致：(a) 降级策略无法根据 serviceName/operationName 做差异化决策；(b) 结合 NoOpDegradationStrategy 始终返回 false，applyStrategies 实际上永远不会将结果标记为降级

#### T21: 元数据字段混入业务字段

- **根因**：MedicalRecordConverter.java:29-47 和 DatabaseTemplateConfigManager.java:111 将 MISSING_FIELDS/PARTIAL_CONTENT 元数据字段写入 content_map

#### T22: MedicalRecordContentConverter 序列化/反序列化异常时静默返回

- **根因**：MedicalRecordContentConverter.java:29,44-46 异常时返回 null/emptyMap，无 WARN 日志

#### T27: submit() 缺乏 prescriptionId 级别并发提交防护

- **根因**：PrescriptionAuditServiceImpl.java:147-195 的 submit 方法无 prescriptionId 级别锁，两个并发线程可同时通过 BLOCK 检查

#### T28: hasNewAlerts 逻辑在正常路径中无实际作用

- **现象**：PrescriptionAuditServiceImpl.java:152-164,182-192,502-506 的 hasNewAlerts 逻辑
- **根因**：submit 方法第152-163行的执行路径分析：
  1. hasCriticalAlerts=true → 第153行进入 if 块 → 获取 snapshotCriticalAlerts → 返回 BLOCK → **不执行到第182行**
  2. hasCriticalAlerts=false → 第164行 `snapshotCriticalAlerts = Collections.emptyList()` → 第182行 `currentAlerts = prescriptionDraftContext.getCriticalAlerts(...)` → 因 hasCriticalAlerts=false，currentAlerts 也为空 → hasNewAlerts(emptyList, emptyList) 返回 false
- **结论**：hasNewAlerts 在当前代码路径中是**死代码**——hasCriticalAlerts=true 时提前返回不执行到此处，hasCriticalAlerts=false 时 currentAlerts 必然为空。hasNewAlerts 永远返回 false，第183-192行的 BLOCK 分支不可达

#### T30: DosageLimitRule.findBestMatch 返回 null 时静默回退

- **根因**：DosageLimitRule.java:36-38 findBestMatch 返回 null 时回退到 `standards.get(0)`，无校验

#### T31: AllergyCheckRule allergyHistory 自由文本 contains 匹配过于激进

- **根因**：AllergyCheckRule.java:54-58 的 contains 子串匹配导致 "No allergy to penicillin" 命中 penicillin 并返回 BLOCK

#### T32: DraftContextCleanupTask 迭代 keySet() 与 writeTimestamps 非原子 + writeTimestamps 永远空

- **根因**：DraftContextCleanupTask.java:38-45 遍历 draftContextStore.keySet() 与 writeTimestamps 之间非原子。更关键的是 recordWrite() 无调用方，writeTimestamps 永远为空，所有条目的 ts 均为 null，清理永远不执行

#### T35: AuditConverter + ai-api DTO 遗漏 weight 和 unit 字段映射

- **根因**：AuditConverter.java:66-75,77-92 和 ai-api DTO 缺少 weight/unit 字段映射

#### T40: @Recover 方法缺陷

- **根因**：RegistrationEventListener.java:52-66 的 recover 方法：(1) 无 @Transactional（deadLetterEventRepository.save 无事务保护）；(2) 第60行 `e.getMessage()` 可能为 null，违反 DeadLetterEvent.failReason 的 NOT NULL 约束；(3) 第58行 JSON 兜底生成含 event.getSessionId() 的字符串，sessionId 为 null 时输出 "null"

### medical-record 模块

#### M02: TemplateConfigChangeEvent 事件 + @EventListener + Caffeine invalidate 未实现

- **根因**：DatabaseTemplateConfigManager.java:105-108 无事件监听和缓存失效机制

#### M03: VisitIdReconciledTask 定时调和任务未实现

- **根因**：medical-record task/ 包不存在

#### M05: RecordGenerateRequest 缺少验证注解

- **根因**：RecordGenerateRequest.java:1-49 缺少 @NotNull/@Size(min=50,max=10000) 等验证注解

#### M06: 超时硬编码在源码中

- **根因**：MedicalRecordServiceImpl.java:40-44 已使用 @Value 注入。此条已修复

#### M07: MedicalRecordConverter.toRecordGenerateResponse 在非超时失败时仍设 success=true

- **根因**：MedicalRecordConverter.java:72-74 非超时失败路径未正确设置 success=false

#### M09: MedicalRecord.doctorId 在 generate() 流程中从未被赋值

- **根因**：MedicalRecordServiceImpl.java:104-107 中 `entity.setDoctorId(request.getDoctorId())` 已赋值。此条已修复

#### M11: ai-api MedicalRecordGenResponse.missingFields 与 partialContent 未被业务层消费

- **根因**：MedicalRecordConverter.java:38-45 未消费 missingFields 和 partialContent 字段

#### T17: callAiWithTimeout 异常语义混淆

- **根因**：MedicalRecordServiceImpl.java:145-159 的三种异常（TimeoutException/InterruptedException/ExecutionException）统一返回 `AiResultFactory.degraded("AI medical record generation timeout", "MR_GEN_AI_TIMEOUT", null)`，语义混淆——ExecutionException 不是超时

#### T47: MedicalRecord @PrePersist 未设置 updatedAt

- **根因**：MedicalRecord.java:133-141 的 @PrePersist 仅设置 createdAt，未设置 updatedAt，新增记录 updatedAt 为 null

#### T48: MedicalRecordServiceImpl.resolveVisitId 使用 ForkJoinPool.commonPool()

- **根因**：MedicalRecordServiceImpl.java:132 的 `CompletableFuture.supplyAsync()` 使用默认 ForkJoinPool.commonPool()，Web 请求高并发下线程耗尽风险

#### T50: DraftContextStoreImpl 缺少 compute/putIfAbsent 原子操作

- **根因**：DraftContextStoreImpl.java:11-26 仅有 get/put/remove/containsKey/keySet 五个方法，缺少 compute 和 createIfNotExists。DraftContextStore 接口（继承 SessionStore）也未定义这些方法。DedupTaskScheduler 使用的是 SuggestionStore（有 compute/createIfNotExists），而非 DraftContextStore，因此 T50 的根因是 DraftContextStore 接口和实现均缺少原子操作方法，但当前代码中 DedupTaskScheduler 不依赖 DraftContextStore

### application / 跨模块

#### A05: MockAiService 与设计契约不符

- **根因**：MockAiService.java:40-42 使用 `@Profile("mock")` 而非 `@ConditionalOnProperty`，无三种返回模式的动态切换（当前有 STATIC/AI_UNAVAILABLE/TIMEOUT 三种策略但通过 @Value 配置，非 MockAdminController 动态切换），无 MockAdminController

#### A06: DegradationStrategy/DegradationContext 为空壳；FallbackAiService 降级决策不生效

- **现象**：FallbackAiService 的降级机制形同虚设
- **根因（需区分两个层面）**：

**层面一：delegate 选择阶段（selectDelegate）降级筛选不生效**
- FallbackAiService.java:66-80 的 selectDelegate 遍历 delegates，对每个 delegate 检查所有 strategy 的 `shouldDegrade(context)`
- NoOpDegradationStrategy.java:9-10 使用 `@Component` + `@ConditionalOnMissingBean(DegradationStrategy.class)` 注册。当无其他 DegradationStrategy Bean 时，NoOp 被注册且始终返回 false
- `@ConditionalOnMissingBean` 的语义：当容器中无 DegradationStrategy 类型的 Bean 时才注册 NoOp。若自定义 DegradationStrategy 实现（标注 @Component/@Service），NoOp 不会加载。因此"永远无法注册有意义策略"的判断不成立——自定义策略注册时 NoOp 自动退让
- 当前 NoOp 始终返回 false → selectDelegate 的降级筛选形同虚设 → 总是返回第一个 delegate
- **根因**：无有意义的 DegradationStrategy 实现，selectDelegate 无法根据服务健康状态跳过降级的 delegate

**层面二：结果后处理阶段（applyStrategies）降级标记不生效**
- FallbackAiService.java:290-301 的 applyStrategies 使用空 DegradationContext（无 serviceName/operationName），且 NoOpDegradationStrategy 始终返回 false
- applyStrategies 的职责是对已获取的 AiResult 做降级标记（将失败结果标记为 degraded），而非做 delegate 选择
- **根因**：空 DegradationContext + NoOp 策略 → applyStrategies 永远不触发降级标记 → AI 调用失败的结果不会被自动降级

**FallbackAiService 的设计模型**：FallbackAiService 的设计是"降级选择"模型（根据策略选择不被降级的 delegate），而非"重试链"模型（依次尝试多个 delegate 直到成功）。"无重试链"不是 FallbackAiService 的问题，而是其设计意图。真正的问题是降级决策逻辑不生效（两个层面均不生效）

#### A09: AuditConverter.toAuditResponse 在 aiData == null 时退化为 PASS + 空 alerts

- **根因**：AuditConverter.java:48-56 在 aiData == null 时返回 PASS + 空 alerts，应在调用方拦截

#### E05: RegistrationEventListener.recover() 手工构造含 3 字段的 HashMap

- **根因**：RegistrationEventListener.java:55-58 的 recover 方法手工构造 eventPayload，缺 sessionId null 防护

---

## P2 — 可并行修复

### consultation 模块

#### C11: TTL 清理周期为 1 分钟而非设计要求的 5 分钟

- **根因**：DialogueSessionManager.java:73 的 `fixedRate = 300000`（5 分钟），非 1 分钟。此条已修复

#### C18: saveTriageRecord 中 catch JsonProcessingException 完全静默

- **根因**：TriageServiceImpl.java:251-253 的 catch 块仅有 `log.warn(...)` 日志输出，非完全静默。但 warn 级别可能不够，且未记录原始 JSON 数据用于排查

#### T44: TriageResponse 缺少 correctedChiefComplaint 字段

- **根因**：dto/TriageResponse.java:5-18 缺少 correctedChiefComplaint 字段，前端无法感知 AI 修正

#### T45: TriageServiceImpl.selectDepartment 缺少自动生成 UUID v4 校验

- **根因**：RegistrationEventListener.java:45 的 event.getSessionId() 可能为 null，传入 selectDepartment 后在 findBySessionId 中可能导致 NPE

### prescription 模块

#### P09: PrescriptionItem.unit 字段为 OOD 未正式定义的扩展字段

- **根因**：PrescriptionItem.java:11 的 unit 字段需在 OOD §1.1a/§3.2/§4.6/§8.3 补充定义

#### P12: DrugInteractionRule 无 Phase 4 预留标注

- **根因**：DrugInteractionRule.java:1-15 缺少 @ConditionalOnProperty 标注

#### P15: AuditResponse.fromFallback 未序列化到 JSON 响应

- **根因**：PrescriptionAuditController.java:36-48 和 PrescriptionAuditServiceImpl.java:144-152 的降级 BLOCK 路径 reasons 固定字符串

#### M10: MedicalRecord.content 字段无 @Column(name="content_json")

- **根因**：MedicalRecord.java:39-41 缺少列名映射注解

#### A08: 降级路径 fallback 文案硬编码英文

- **根因**：TriageServiceImpl.java:171,177 和 MedicalRecordServiceImpl.java:151,154,157 的降级文案硬编码中文/英文

### application / 跨模块

#### A07: AiResult.success(data) 允许 data=null

- **根因**：AiResult.java:24-26 的 `success()` 方法使用 `Objects.requireNonNull(data)` 断言 data 非 null。此条已修复——AiResult.success() 已拒绝 null data

#### A11: 业务层普遍在 isSuccess() 后额外做 getData() != null 防御性检查

- **根因**：TriageServiceImpl.java:137 和 PrescriptionAuditServiceImpl.java:110 的 `aiResult != null && aiResult.isSuccess()` 检查。由于 AiResult.success() 已断言 data 非 null，isSuccess()=true 时 data 必然非 null。但 aiResult 可能为 null（AI 调用异常时），null 检查仍需保留

#### T24: ConcurrentHashMapStore 缺少 Spring 注解

- **根因**：ConcurrentHashMapStore.java:10 无 @Service/@Component 注解，不会被 Spring 自动扫描注册。DraftContextStoreImpl.java:8 有 @Service 注解

---

## 修订说明（v2）

| 质询意见 | 回应 |
|---------|------|
| P03/S02：诊断称 AiSuggestionResult 未实现 SuggestionStoreEntry 接口，ClassCastException 被 catch 后跳过。实际 AiSuggestionResult.java:8 已声明 implements SuggestionStoreEntry | **接受，修订根因判定。** AiSuggestionResult 确实实现了 SuggestionStoreEntry，instanceof 检查会成功，不会抛出 ClassCastException。SuggestionCleanupTask 的真正失效原因已重新调查：(1) scheduleSuggestionAsync 未设置 createTime → getTimestamp() 返回 null → isExpiredAndConsumed 第46行对 null 调用 plusSeconds() 抛出 NPE → NPE 不是 ClassCastException，不被第36行 catch 捕获 → 清理方法异常终止；(2) FAILED 状态的 consumed 永远为 false（仅 getSuggestion 对 COMPLETED 设置 consumed=true），FAILED 条件不会被清理 |
| A06：诊断称 FallbackAiService 总是选择 delegates.get(0)，无重试链。实际 selectDelegate 遍历 delegates 检查 shouldDegrade，NoOpDegradationStrategy 始终返回 false 导致返回第一个 delegate | **接受，修订诊断描述。** FallbackAiService 的设计模型是"降级选择"而非"重试链"，"无重试链"不是问题。真正的问题是降级决策逻辑不生效，分两个层面：(1) delegate 选择阶段——NoOpDegradationStrategy 使降级筛选形同虚设；(2) 结果后处理阶段——applyStrategies 使用空 DegradationContext 且 NoOp 不触发降级标记。NoOpDegradationStrategy 的 @ConditionalOnMissingBean 语义正确——自定义策略注册时 NoOp 自动退让，不存在"永远无法注册有意义策略"的问题 |
| T20：诊断称 applyStrategies 使用空 DegradationContext，未区分与 selectDelegate 中的 DegradationContext 是两个独立使用点 | **接受，修订影响范围判定。** selectDelegate 在各业务方法中正确设置了 DegradationContext（serviceName/operationName），delegate 选择不受空 DegradationContext 影响。applyStrategies 的空 DegradationContext 仅影响"结果后处理阶段的降级标记无法基于服务名/操作名做精细化决策"，不影响 delegate 选择逻辑 |
| A01：诊断称 PrescriptionAuditServiceImpl 使用 AiResult.degraded()。实际 AI 调用失败时 aiResult 被设为 null | **接受，修订细节。** PrescriptionAuditServiceImpl.java:96-105 AI 调用失败时 aiResult=null，而非使用 AiResult.degraded()。此细节不影响核心结论（AiResultFactory 未被 PrescriptionAuditServiceImpl 使用），但描述不准确已修正 |
| P03/S02 + T32 合并诊断：两者失效原因不同，不应合并 | **接受，修订为独立诊断。** DraftContextCleanupTask 失效因 recordWrite() 无调用方 → writeTimestamps 永远空；SuggestionCleanupTask 失效因 getTimestamp() 返回 null 导致 NPE + FAILED 状态 consumed 永远为 false。两者根因独立，已分别诊断 |
| T28：hasNewAlerts 逻辑矛盾分析结论过于笼统，实际是死代码 | **接受，修订结论。** hasNewAlerts 在当前代码路径中是死代码——hasCriticalAlerts=true 时提前返回 BLOCK 不执行到 hasNewAlerts 检查，hasCriticalAlerts=false 时 currentAlerts 必然为空使 hasNewAlerts 返回 false。第183-192行的 BLOCK 分支不可达 |
| NoOpDegradationStrategy 的 @ConditionalOnMissingBean 未被诊断提及 | **接受，补充诊断。** NoOpDegradationStrategy 使用 @ConditionalOnMissingBean(DegradationStrategy.class) 注册，当无其他 DegradationStrategy Bean 时自动注册。自定义策略注册时 NoOp 不加载，不存在 Bean 加载顺序问题。已纳入 A06 诊断 |
| T50 DraftContextStoreImpl 缺少 compute/createIfNotExists 方法，诊断未验证 DedupTaskScheduler 是否实际使用 DraftContextStore | **接受，补充验证。** DedupTaskScheduler 使用 SuggestionStore（有 compute/createIfNotExists），不依赖 DraftContextStore。T50 的根因是 DraftContextStore 接口和实现均缺少原子操作方法，但当前无代码依赖这些缺失方法。若未来 DraftContextStore 需要原子操作，则需补充 |
