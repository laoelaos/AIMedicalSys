# 代码审查问题诊断报告

## 1. 诊断范围

本报告对 `Harness/reviews/202607010217_code_review/todo.md` 中列出的 80+ 项审查问题进行根因诊断，覆盖 consultation、prescription、medical-record 三大业务模块及跨模块公共层。OOD 参考文档为 `Docs/05_ood_phase1_B.md`。

诊断目标：定位每项问题的根因所在的具体代码位置和触发条件，建立从现象到根因的因果链，使修复者明确"改哪里"和"为什么"。

---

## 2. P0 问题根因诊断

### 2.1 consultation 模块

#### C01: TriageRecord 实体缺失 correctedChiefComplaint 快照字段

- **现象**：审查报告称 TriageRecord 缺少 `correctedChiefComplaint` 字段
- **代码事实**：`TriageRecord.java:42` 已声明 `private String correctedChiefComplaint` 字段，含 getter/setter（第148-154行）
- **根因**：**此问题已修复**。当前代码中 `correctedChiefComplaint` 字段已存在于实体中，且 `TriageServiceImpl.java:271` 的 `saveTriageRecord` 方法已执行 `record.setCorrectedChiefComplaint(session.getCorrectedChiefComplaint())` 写入
- **结论**：C01 为已修复项，无需行动

#### C02: TriageRecordRepository 缺少 findTopBySessionIdOrderByTriageTimeDesc 恢复查询方法

- **现象**：审查报告称 Repository 缺少此方法
- **代码事实**：`TriageRecordRepository.java:25` 已声明 `Optional<TriageRecord> findTopBySessionIdOrderByTriageTimeDesc(String sessionId)`
- **根因**：**此问题已修复**。该方法已存在，且被 `DialogueSessionManager.java:60` 正确调用用于 session 恢复
- **结论**：C02 为已修复项，无需行动

#### C04: TriageServiceImpl.triage/selectDepartment/saveTriageRecord 缺少 @Transactional 边界

- **现象**：三个方法缺少事务边界
- **代码事实**：
  - `TriageServiceImpl.java:191` — `selectDepartment` 已标注 `@Transactional`
  - `TriageServiceImpl.java:257` — `saveTriageRecord` 已使用 `transactionTemplate.execute()` 包围 save 操作
  - `TriageServiceImpl.java:82` — `triage()` 方法未标注 `@Transactional`，但该方法内部仅 `saveTriageRecord` 需要事务（已通过 TransactionTemplate 实现），triage 本身包含 AI 调用等长耗时操作，不应整体包裹事务
- **根因**：**此问题已修复**。selectDepartment 有 @Transactional，saveTriageRecord 通过 TransactionTemplate 实现事务边界，triage 方法的事务策略合理（长事务拆分为局部事务）
- **结论**：C04 为已修复项，无需行动

#### C05: chiefComplaint 与 additionalResponses 互斥校验缺失

- **现象**：应返回 TRIAGE_FIELD_COMBINATION_INVALID
- **代码事实**：`TriageServiceImpl.java:85-90` 已实现互斥校验逻辑——`hasChiefComplaint == hasAdditional` 时抛出 `BusinessException(TriageErrorCode.TRIAGE_FIELD_COMBINATION_INVALID, request.getSessionId())`
- **根因**：**此问题已修复**。互斥校验已实现
- **结论**：C05 为已修复项，无需行动

#### C06/E03: DoctorFacade 跨模块调用无 try/catch、无超时、无 WARN 日志

- **现象**：DoctorFacade 调用缺少容错保护
- **代码事实**：`TriageServiceImpl.java:220-230` — `findDoctorsForDepartments` 方法中 DoctorFacade 调用已有 try/catch 包围，catch 块中记录了 WARN 日志（含耗时 ms）
- **根因**：**部分已修复**。try/catch 和 WARN 日志已实现，但**超时控制缺失**——`doctorFacade.findAvailableDoctorsByDepartment()` 调用无超时参数，若 DoctorFacade 实现端阻塞，当前线程将无限等待。application.yml 中 `consultation.doctor-facade.timeout: 2` 配置已存在但 TriageServiceImpl 未注入也未使用
- **触发条件**：DoctorFacade 远程调用阻塞或响应极慢时，triage 线程被挂起
- **结论**：超时控制仍需修复

#### C07: AI 调用 future.get() 无超时参数

- **现象**：AI 调用缺少超时
- **代码事实**：`TriageServiceImpl.java:124` — `future.get(aiTimeout, TimeUnit.SECONDS)` 已使用注入的 `aiTimeout` 参数（构造函数 `@Value("${ai.timeout.triage:8}")` 注入，第68行）
- **根因**：**此问题已修复**。超时已通过 @Value 注入并使用
- **结论**：C07 为已修复项，无需行动

#### C08: selectDepartment 签名为 4 参、RegistrationEventListener 未调用该方法

- **现象**：selectDepartment 签名与事件监听器不匹配
- **代码事实**：
  - `TriageService.java:10` — 当前签名为 3 参：`selectDepartment(String sessionId, String departmentId, String departmentName)`
  - `RegistrationEventListener.java:47` — 调用 `triageService.selectDepartment(event.getSessionId(), event.getDepartmentId(), event.getDepartmentName())`，3 参匹配
- **根因**：**此问题已修复**。签名已改为 3 参，事件监听器调用匹配
- **结论**：C08 为已修复项，无需行动

#### C15/E01: RegistrationEventListener @Retryable 对所有 Exception.class 重试

- **现象**：重试范围过宽
- **代码事实**：`RegistrationEventListener.java:41-43` — 当前配置为 `retryFor = {DataAccessException.class, TimeoutException.class}`，`noRetryFor = {IllegalArgumentException.class, NullPointerException.class}`
- **根因**：**此问题已修复**。重试范围已限定为 DataAccessException 和 TimeoutException
- **结论**：C15 为已修复项，无需行动

#### C20: createSession 使用 put 而非 putIfAbsent，并发覆盖风险

- **现象**：并发创建 session 可能覆盖已有 session
- **代码事实**：`DialogueSessionManager.java:33-43` — `createSession` 方法已标注 `synchronized`，且在 put 前通过 `sessionStore.containsKey(sessionId)` 检查存在性（第37行），存在时返回已有 session
- **根因**：**此问题已修复**。synchronized + containsKey 检查已消除并发覆盖风险。但需注意：synchronized 依赖实例锁，若 DialogueSessionManager 为多实例则锁失效；当前为 @Component 单例，锁有效
- **结论**：C20 为已修复项，无需行动

#### C21: 降级路径使用 request 参数而非 session 快照的 ruleVersion/ruleSetId

- **现象**：降级路径应使用 session 中的快照值
- **代码事实**：`TriageServiceImpl.java:157-158` — 降级路径调用 `triageRuleEngine.match(request.getChiefComplaint(), session.getRuleVersion(), session.getRuleSetId())`，已使用 `session.getRuleVersion()` 和 `session.getRuleSetId()`
- **根因**：**此问题已修复**。降级路径已使用 session 快照值
- **结论**：C21 为已修复项，无需行动

#### E02: TriageRecord 同 sessionId 第二次分诊违反 @Column(unique=true) 约束

- **现象**：始终 INSERT 不 UPDATE
- **代码事实**：`TriageServiceImpl.java:258-265` — `saveTriageRecord` 方法在 TransactionTemplate 内先查询 `findBySessionId`，若存在则获取已有 record 进行 UPDATE，否则新建
- **根因**：**此问题已修复**。saveTriageRecord 已实现 INSERT/UPDATE 分支逻辑。且 `TriageRecordRepository.findBySessionId` 标注了 `@Lock(PESSIMISTIC_WRITE)` + `@Transactional(propagation = MANDATORY)`，确保并发安全
- **结论**：E02 为已修复项，无需行动

#### S04: 同 session 并发访问无串行化保护

- **现象**：restoreSession 缺 synchronized，createSession 用 put 而非 putIfAbsent
- **代码事实**：
  - `DialogueSessionManager.java:33` — `createSession` 已标注 `synchronized`
  - `DialogueSessionManager.java:50` — `restoreSession` 未标注 `synchronized`
  - `DialogueSessionManager.java:68` — restoreSession 中恢复的 session 通过 `sessionStore.put(sessionId, session)` 写入，无原子性保护
- **根因**：**部分已修复**。createSession 的并发问题已通过 synchronized 解决。但 `restoreSession` 仍缺少同步保护——两个线程可同时进入 restoreSession，对同一 sessionId 并发执行 DB 查询 + put，导致后写入的 session 覆盖先写入的 session（丢失先恢复的 correctedChiefComplaint 等状态）
- **触发条件**：同一 sessionId 的两个并发请求在 session 过期后同时触发 restoreSession
- **结论**：restoreSession 仍需加 synchronized 或改用 putIfAbsent

### 2.2 prescription 模块

#### A01: AiResultFactory 在全部 4 个业务 Service 实现中零引用

- **现象**：手工 `new AiResult<>()` 而非使用 AiResultFactory
- **代码事实**：
  - `TriageServiceImpl.java:208,210` — 已使用 `AiResultFactory.degraded()`
  - `MedicalRecordServiceImpl.java:151,154,157` — 已使用 `AiResultFactory.degraded()`
  - `PrescriptionAuditServiceImpl.java` — 未直接构造 AiResult，使用 `AiResult.degraded()` 静态方法（来自 AiResult 类自身）
- **根因**：**大部分已修复**。TriageServiceImpl 和 MedicalRecordServiceImpl 已使用 AiResultFactory。PrescriptionAuditServiceImpl 的 AI 调用失败路径返回 null（第100-104行），不涉及 AiResult 构造。但需注意 `AiResult.java:24-26` 的静态 `success(T data)` 方法使用 `Objects.requireNonNull(data)` 做空值断言，而 `AiResultFactory.success(T data)` 第25行未做此断言——两处 success 语义不一致
- **结论**：AiResultFactory 引用问题已基本修复；AiResult 与 AiResultFactory 的 success 方法空值语义不一致仍需统一

#### P01: 异步 AI 调度机制未实现

- **现象**：schedule 创建 PENDING 后无代码触发 AiService.prescriptionAssist()
- **代码事实**：`PrescriptionAssistServiceImpl.java:339-369` — `scheduleSuggestionAsync` 方法已实现：在 `assist()` 方法第105行调用 `scheduleSuggestionAsync(taskId, request)`，内部通过 `CompletableFuture.supplyAsync()` 异步调用 `aiService.prescriptionAssist()`，完成后将结果写入 suggestionStore
- **根因**：**此问题已修复**。异步 AI 调度已通过 scheduleSuggestionAsync 实现
- **结论**：P01 为已修复项，无需行动

#### P02/E06: DrugFacade 在 PrescriptionAuditServiceImpl 和 PrescriptionAssistServiceImpl 均未注入

- **现象**：DrugFacade 未注入
- **代码事实**：`PrescriptionAuditServiceImpl.java:72-87` 和 `PrescriptionAssistServiceImpl.java:57-75` — 构造函数中均无 DrugFacade 参数
- **根因**：**问题确认**。DrugFacade 未注入，意味着处方审核和辅助服务无法查询药品信息（药品禁忌、成分、相互作用数据）。当前 AllergyCheckRule 通过 DrugAllergyMappingRepository 直接查询过敏映射，绕过了 DrugFacade，但药品成分查询、禁忌查询等仍需 DrugFacade
- **触发条件**：任何需要查询药品详情的审核场景
- **结论**：DrugFacade 注入缺失仍需修复

#### P03/S02: AiSuggestionResult + PrescriptionDraftContext TTL 清理任务完全缺失或无效

- **现象**：DraftContextCleanupTask 的 writeTimestamps 永远空
- **代码事实**：
  - `DraftContextCleanupTask.java:27-29` — `recordWrite` 方法存在但**无任何调用方**。全局搜索未发现任何代码调用 `recordWrite()`
  - `DraftContextCleanupTask.java:39` — `writeTimestamps.get(key)` 始终返回 null，导致 cleanupExpiredDrafts 永远不会清理任何条目
  - `SuggestionCleanupTask.java:25-40` — 该任务依赖 `SuggestionStoreEntry` 接口判断过期，但 `AiSuggestionResult` 未实现 `SuggestionStoreEntry` 接口，ClassCastException 被 catch 后跳过，实际也无法清理
- **根因**：**问题确认**。DraftContextCleanupTask 的 recordWrite 方法无调用方，writeTimestamps 永远为空，TTL 清理完全无效。SuggestionCleanupTask 因类型不匹配同样无效
- **触发条件**：系统长时间运行后，内存中的 DraftContext 和 Suggestion 数据持续累积，最终 OOM
- **结论**：TTL 清理机制仍需修复——需在 PrescriptionDraftContext.updateCriticalAlerts 中调用 recordWrite，且 SuggestionCleanupTask 需适配 AiSuggestionResult 类型

#### P04/E04: 规则变更事件未定义、未监听

- **现象**：prescription/event/ 包不存在
- **代码事实**：全局搜索未发现 DrugContraindicationChangeEvent、DrugAllergyMappingChangeEvent、DrugCompositionDictChangeEvent 的定义或监听器
- **根因**：**问题确认**。药品规则变更事件完全缺失。当管理员修改药品禁忌、过敏映射或成分字典时，缓存中的旧数据不会失效，审核服务继续使用过期规则
- **触发条件**：管理员修改药品规则数据后，审核服务仍使用缓存中的旧规则
- **结论**：规则变更事件仍需实现

#### S03: DedupTaskScheduler.schedule() 在 compute lambda 内嵌套 suggestionStore.put

- **现象**：跨 key 操作破坏 compute 原子性
- **代码事实**：`DedupTaskScheduler.java:51-62` — compute lambda 内（第61-62行）当 `result == newResult` 时执行 `suggestionStore.put(candidateTaskId, newResult)`，这是跨 key 操作（dedupKey → candidateTaskId），不在 compute 的原子性保护范围内
- **根因**：**问题确认**。compute 闭包内的 put 操作破坏了 ConcurrentHashMap.compute 对 dedupKey 的原子性保证。并发场景下，两个线程可能同时通过 compute 写入 newResult，然后都执行 put(candidateTaskId, newResult)，导致 candidateTaskId 对应的值被覆盖
- **触发条件**：同一 prescriptionId 的两个并发 schedule 调用，在 compute 判定结果为 newResult 后并发执行 put
- **结论**：compute 内嵌套跨 key put 仍需修复

### 2.3 medical-record 模块

#### A02: 所有 AI 调用超时完全未外化

- **现象**：AI 超时硬编码
- **代码事实**：`MedicalRecordServiceImpl.java:40-44` — 已通过 `@Value("${ai.timeout.medical-record-generate:12}")` 和 `@Value("${medical-record.visit-facade.timeout:2}")` 注入超时值
- **根因**：**此问题已修复**。超时已外化到配置文件，application.yml 中已有对应配置项
- **结论**：A02 为已修复项，无需行动

#### A03: AiSuggestionResult 5 状态映射表全部未实现

- **现象**：异步 AI 完成后无 PENDING→COMPLETED/FAILED 回填
- **代码事实**：`PrescriptionAssistServiceImpl.java:339-369` — `scheduleSuggestionAsync` 已实现状态映射：成功时设 `AiSuggestionStatus.COMPLETED`（第351行），失败时设 `AiSuggestionStatus.FAILED`（第353/358/362/365行）
- **根因**：**此问题已修复**。PENDING→COMPLETED/FAILED 状态映射已在 scheduleSuggestionAsync 中实现
- **结论**：A03 为已修复项，无需行动

#### M01: 4 个错误码缺失

- **现象**：MR_GEN_AI_UNAVAILABLE/MR_GEN_AI_INPUT_INVALID/MR_GEN_AI_OUTPUT_INCOMPLETE/MR_GEN_TEMPLATE_LOAD_FAILED 缺失
- **代码事实**：`MedicalRecordErrorCode.java:6-13` — 全部 4 个错误码已定义：MR_GEN_AI_INPUT_INVALID（第6行）、MR_GEN_AI_OUTPUT_INCOMPLETE（第7行）、MR_GEN_AI_UNAVAILABLE（第9行）、MR_GEN_TEMPLATE_LOAD_FAILED（第12行）
- **根因**：**此问题已修复**。4 个错误码均已定义
- **结论**：M01 为已修复项，无需行动

#### M04: MR_GEN_CONCURRENT_MODIFICATION 在当前 generate() INSERT 路径中无法触发

- **现象**：该错误码应在 UPDATE 路径而非 INSERT 路径
- **代码事实**：`MedicalRecordServiceImpl.java:102-120` — generate() 方法先通过 `findByVisitId` 查询，若存在则获取已有 entity（UPDATE 路径），若不存在则 `new MedicalRecord()`（INSERT 路径）。ObjectOptimisticLockingFailureException catch 在第114行，仅在 UPDATE 路径（version 冲突）时可能触发
- **根因**：**此问题已修复**。当前代码中 findByVisitId 返回已有记录时走 UPDATE 路径，此时乐观锁冲突可正确触发 MR_GEN_CONCURRENT_MODIFICATION。INSERT 路径（新记录）不会触发此异常
- **结论**：M04 为已修复项，无需行动

### 2.4 application / 跨模块

#### C03/A04: AI 隐式 correctedChiefComplaint 路径未实现 + 显式透传路径未生效

- **现象**：TriageConverter 未透传 correctedChiefComplaint
- **代码事实**：
  - `TriageConverter.java:63-65` — 已实现隐式路径：`if (session != null && session.getCorrectedChiefComplaint() != null) { aiRequest.setCorrectedChiefComplaint(session.getCorrectedChiefComplaint()); }`
  - `TriageServiceImpl.java:110` — 显式透传：`session.setCorrectedChiefComplaint(request.getCorrectedChiefComplaint())`
  - `TriageServiceImpl.java:148-150` — AI 返回后回写 session：`if (aiData.getCorrectedChiefComplaint() != null) { session.setCorrectedChiefComplaint(aiData.getCorrectedChiefComplaint()); }`
- **根因**：**此问题已修复**。隐式路径（session→AI request）和显式透传路径（request→session→AI request）均已实现
- **结论**：C03/A04 为已修复项，无需行动

#### A10: application.yml 完全缺失 ai.timeout.* / facade.*.timeout / ai.mock.* 等配置项

- **现象**：7 个配置键缺失
- **代码事实**：`application.yml:17-32` — 已包含：
  - `ai.timeout.triage: 8`（第19行）
  - `ai.timeout.prescription-audit: 6`（第20行）
  - `ai.timeout.medical-record-generate: 12`（第21行）
  - `ai.timeout.prescription-assist: 8`（第22行）
  - `ai.mock.response-strategy: STATIC`（第24行）
  - `consultation.doctor-facade.timeout: 2`（第29行）
  - `medical-record.visit-facade.timeout: 2`（第32行）
- **根因**：**此问题已修复**。7 个配置键均已定义
- **结论**：A10 为已修复项，无需行动

---

## 3. P1 问题根因诊断

### 3.1 consultation 模块

#### C09: selectDepartment 使用 GlobalErrorCode.NOT_FOUND 而非业务级错误码

- **代码事实**：`TriageServiceImpl.java:195` — 已使用 `TriageErrorCode.TRIAGE_SESSION_NOT_FOUND`
- **结论**：**已修复**

#### C10: DialogueSessionManager 未校验 sessionId 的 UUID v4 格式

- **代码事实**：`DialogueSessionManager.java:34,51` — createSession 和 restoreSession 均已校验 UUID v4 格式
- **结论**：**误报确认**（todo.md 已标注）

#### C12: AI 上下文未按模板拼接全量历史，未实现 correctedChiefComplaint 替换 + 3000 字符截断

- **代码事实**：`TriageConverter.java:51-61` — 已实现 3000 字符截断（第57-59行：`if (sb.length() > 3000) { sb.setLength(3000); sb.append(" [TRUNCATED]"); }`）。correctedChiefComplaint 替换已在第63-65行实现
- **根因**：**部分已修复**。3000 字符截断和 correctedChiefComplaint 透传已实现。但"按模板拼接全量历史"的完整模板格式（如包含既往分诊结果、历史对话等）未实现——当前仅拼接 additionalResponses 的 Q&A 文本
- **结论**：全量历史模板拼接仍需完善

#### C13: TriageRuleEngine.match 无快照失效回退逻辑

- **代码事实**：`DefaultTriageRuleEngine.java:53-59` — 已实现版本过滤 + 回退逻辑：当 versionFiltered 为空且 version/setId 非空时，回退到全部启用规则，并设置 `ruleVersionMismatch = true`
- **结论**：**已修复**。ruleVersionMismatch 标记已通过 MatchResult 传递

#### C16: DefaultTriageRuleEngine.match 未实现 TriageRule.conditions JSON 关键词解析

- **代码事实**：`DefaultTriageRuleEngine.java:82-116` — `matchesConditions` 方法已实现 JSON 解析：解析 keywords 数组，支持 AND/OR 逻辑，按关键词匹配 chiefComplaint
- **根因**：**部分已修复**。JSON 关键词解析已实现。但第113-115行：`catch (JsonProcessingException e) { return true; }` — JSON 解析失败时静默返回 true，导致规则无条件匹配所有主诉。这是审查报告指出的核心问题
- **触发条件**：TriageRule.conditions 字段包含非法 JSON 字符串时，该规则将匹配所有主诉
- **结论**：JSON 解析失败时的静默返回 true 仍需修复

#### C17: findDoctorsForDepartments 未按匹配评分排序取前 5 名

- **代码事实**：`TriageServiceImpl.java:232-234` — 排序使用 `Integer.compare(b.getAvailableSlotCount(), a.getAvailableSlotCount())`，按可用号源数降序排序，而非按 OOD 要求的 score 排序
- **根因**：**问题确认**。RecommendedDoctor 的 score 字段（第224行构造时硬编码为 `0f`）未被使用，排序依据是 availableSlotCount 而非 score
- **触发条件**：所有分诊推荐医生场景
- **结论**：排序逻辑仍需改为按 score 降序

#### C19: saveTriageRecord 未读取 session.getCorrectedChiefComplaint() 写入实体

- **代码事实**：`TriageServiceImpl.java:271` — 已执行 `record.setCorrectedChiefComplaint(session.getCorrectedChiefComplaint())`
- **结论**：**已修复**

#### C14: DeadLetterCompensationService 未检查 retryCount >= maxRetryCount 时的 EXPIRED 状态迁移

- **代码事实**：`DeadLetterCompensationService.java:31-34` — 已实现：`if (event.getRetryCount() >= event.getMaxRetryCount()) { event.setState("EXPIRED"); ... }`
- **结论**：**已修复**

#### C22: TriageController.selectDepartment 写死 overwrite=true

- **代码事实**：`TriageController.java:30-35` — 当前 selectDepartment 端点仅接收 sessionId、departmentId、departmentName 三个参数，无 overwrite 参数
- **结论**：**已修复**（随 C08 签名变更一并解决）

#### T4: TriageServiceImpl 降级路径手工构造 Response 而非复用 Converter

- **代码事实**：`TriageServiceImpl.java:168-181` — 降级路径手工构造 TriageResponse，设置 departments、doctors、reason、sessionId、degraded、confidence、ruleVersionMismatch、fallbackHint 等字段
- **根因**：**问题确认**。降级路径未复用 TriageConverter.toTriageResponse()，且遗漏 `matchedRules` 字段。手工构造导致：(1) 字段遗漏风险（当前缺 matchedRules）；(2) 与正常路径的响应结构不一致
- **触发条件**：AI 服务不可用时触发降级路径
- **结论**：降级路径仍需改为复用 Converter

#### T5: TriageConverter 副作用修改 Session + DialogueSession 混用 synchronized/AtomicInteger/CopyOnWriteArrayList

- **代码事实**：
  - `TriageConverter.java:107-109` — toTriageResponse 方法中执行 `session.setCorrectedChiefComplaint(aiData.getCorrectedChiefComplaint())`，这是副作用修改
  - `DialogueSession.java` — 混用 synchronized（sessionId/chiefComplaint/correctedChiefComplaint 等用 synchronized getter/setter）、AtomicInteger（aiFailCount/roundCount）、CopyOnWriteArrayList（additionalResponses）
- **根因**：**问题确认**。TriageConverter 作为转换器不应修改 session 状态（违反单一职责）。DialogueSession 的混用同步策略本身不是 bug（synchronized 保护普通字段，AtomicInteger 保护计数器，CopyOnWriteArrayList 保护列表），但 TriageConverter 中的副作用修改使得 session 状态变更散落在多个位置，增加并发推理难度
- **触发条件**：TriageConverter.toTriageResponse 被调用时，session 被意外修改
- **结论**：TriageConverter 副作用仍需移除

#### T16: PrescriptionDraftContext.hasCriticalAlerts 与 getCriticalAlerts 缺少原子操作

- **代码事实**：`PrescriptionDraftContext.java:19-22` — `hasCriticalAlerts` 调用 `getCriticalAlerts` 后检查非空，两步操作间存在 TOCTOU 窗口
- **根因**：**问题确认**。hasCriticalAlerts 和 getCriticalAlerts 之间，另一个线程可能通过 updateCriticalAlerts 清空 alerts，导致 hasCriticalAlerts 返回 true 但后续 getCriticalAlerts 返回空列表。在 `PrescriptionAuditServiceImpl.java:152-154` 中，submit 方法先调 hasCriticalAlerts 再调 getCriticalAlerts，受此窗口影响
- **触发条件**：并发 submit 和 assist 请求操作同一 prescriptionId 时
- **结论**：TOCTOU 窗口仍需修复

#### T42: DefaultTriageRuleEngine 缓存仅依赖 refreshAfterWrite 无主动刷新

- **代码事实**：`DefaultTriageRuleEngine.java:30-37` — 缓存配置 `refreshAfterWrite(60, TimeUnit.SECONDS)`，无事件驱动失效机制
- **根因**：**问题确认**。规则变更后最长需 60 秒才能生效。无 TemplateConfigChangeEvent 等事件监听机制触发缓存失效
- **触发条件**：管理员修改分诊规则后 60 秒内的分诊请求仍使用旧规则
- **结论**：事件驱动缓存失效仍需实现

### 3.2 prescription 模块

#### P05: SubmitResponse 缺 warnResult 字段

- **代码事实**：`SubmitResponse.java:9` — 已声明 `private WarnResult warnResult`，含 getter/setter
- **结论**：**已修复**。但 WARN 路径使用的 `RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT` 错误码未在 PrescriptionErrorCode 中定义（需确认）

#### P06: 降级路径 LocalRuleResult 未转换为 AuditAlert

- **代码事实**：`PrescriptionAuditServiceImpl.java:116-130` — 降级路径已将 LocalRuleResult 转换为 AuditAlert：遍历 ruleResults，对 `!result.isPassed()` 的项构造 `new AuditAlert(result.getRuleId(), result.getMessage(), toAlertSeverity(result.getSeverity()))`
- **结论**：**已修复**

#### P07: AuditRecord.auditIssues 字段从未写入

- **代码事实**：`PrescriptionAuditServiceImpl.java:391-429` — `persistAuditRecord` 方法中已实现 auditIssues 写入逻辑：从 LocalRuleResult 或 AI AlertItem 构造 AuditIssue 列表，序列化后通过 `record.setAuditIssues(objectMapper.writeValueAsString(issues))` 写入
- **结论**：**已修复**

#### P08: forceSubmit 路径在生成 prescriptionOrderId 后未回写到 AuditRecord

- **代码事实**：`PrescriptionAuditServiceImpl.java:258-273` — forceSubmit 路径已执行 `latestRecord.setPrescriptionOrderId(orderId)` 和 `latestRecord.setForceSubmitted(true)`，并通过 `auditRecordRepository.save(latestRecord)` 持久化
- **根因**：**部分已修复**。prescriptionOrderId 回写已实现。但 SubmitRequest 无 prescriptionHash 字段（`SubmitRequest.java:7-53` 无 hash 相关字段），forceSubmit 路径未验证 prescriptionHash
- **结论**：SubmitRequest 缺 hash 字段仍需修复

#### P10: DosageThresholdService.matchByPriority 第一、二重循环逻辑重复

- **代码事实**：`DosageThresholdService.java:103-121` — 第一重循环（第103-113行）检查 `exactAge && exactWeight`，第二重循环（第115-121行）检查 `ageInRange && weightInRange`。第一重是第二重的特例（exact 是 inRange 的子集）
- **根因**：**问题确认**。第一重循环的精确匹配条件是第二重循环范围匹配的子集，逻辑重复。当第一重循环找到 level1 匹配时，第二重循环的 level2 也会匹配到同一条记录（因为 exactAge 满足 ageInRange）
- **结论**：循环逻辑重复仍需优化

#### P11: SpecialPopulationDosageRule 与 DosageLimitRule 使用相同通用 DosageStandard 查询

- **根因**：**问题确认**。两个规则使用相同的 `dosageStandardRepository.findByDrugCodeAndRouteOfAdministration()` 查询，无法区分通用剂量标准和特殊人群剂量标准。应在 DosageStandard 实体中增加人群类型标识，或使用不同的查询方法
- **结论**：查询策略区分仍需实现

#### P13: AllergyCheckRule 结构化匹配未命中时不回退到文本匹配

- **代码事实**：`AllergyCheckRule.java:43-60` — 当 `allergyDetails` 非空时走结构化匹配（第43-53行），否则走文本匹配（第54-59行）。结构化匹配未命中时直接 continue 到下一个药品，不回退到文本匹配
- **根因**：**问题确认**。当患者有结构化过敏详情但未覆盖当前药品的过敏原时，应回退到 allergyHistory 文本匹配，但当前代码在 allergyDetails 非空时完全跳过文本匹配路径
- **触发条件**：患者有 allergyDetails 但不包含当前药品的过敏原，同时 allergyHistory 中包含相关文本
- **结论**：结构化→文本回退逻辑仍需修复

#### P14: PrescriptionAssistServiceImpl 各失败路径未写入 PrescriptionDraftContext

- **代码事实**：`PrescriptionAssistServiceImpl.java:90-99,108-111,114-118` — 各失败路径调用 `clearCriticalAlerts(request.getPrescriptionId())`，即 `updateCriticalAlerts(prescriptionId, Collections.emptyList())`
- **根因**：**部分已修复**。失败路径已清除 criticalAlerts。但未写入失败状态本身到 DraftContext（如失败原因、时间戳），后续 submit 检查无法感知 assist 失败
- **结论**：失败状态写入仍需完善

#### P16: AuditRecordRepository 没有 List 版本 findByPrescriptionOrderIdAndIsLatestTrue

- **代码事实**：`AuditRecordRepository.java:22` — 已声明 `List<AuditRecord> findByPrescriptionOrderIdAndIsLatestTrue(String prescriptionOrderId)`
- **结论**：**已修复**

#### S01: SuggestionStore 接口缺少 createIfNotExists 原子方法

- **代码事实**：`ConcurrentHashMapStore.java:35-37` — 已实现 `createIfNotExists(String key, Object value)` 基于 `store.putIfAbsent(key, value)`
- **结论**：**误报确认**（todo.md 已标注缺 supplier 形式，但当前 putIfAbsent 形式已满足 DedupTaskScheduler 使用需求）

#### S06: DedupTaskScheduler.schedule() 返回值从 Object 强制转换为 AiSuggestionResult

- **代码事实**：`DedupTaskScheduler.java:44-48,64-65` — compute 返回 Object，第64行 `result instanceof AiSuggestionResult winner` 使用 pattern matching，非强制转换
- **结论**：**已修复**（使用 instanceof pattern matching 替代强制转换）

#### S07: ConcurrentHashMapStore 同时实现 SuggestionStore 和 DraftContextStore

- **代码事实**：`ConcurrentHashMapStore.java:10` — 仅实现 `SuggestionStore` 接口，未实现 DraftContextStore
- **根因**：**部分问题**。ConcurrentHashMapStore 仅实现 SuggestionStore，但内部使用单一 ConcurrentHashMap 存储所有数据（suggestion 和 draft context 共享同一 map），key 通过前缀区分。DraftContextStore 由独立的 DraftContextStoreImpl 实现
- **结论**：共享 ConcurrentHashMap 的设计仍存在（suggestion 和 draft context 数据混在同一 map），但接口分离已实现

#### T8: AllergyCheckRule + AllergyDetail 跨模块依赖 patient 模块

- **代码事实**：`AllergyCheckRule.java:3` — `import com.aimedical.modules.patient.entity.AllergySeverity`
- **根因**：**问题确认**。prescription 模块直接依赖 patient 模块的 AllergySeverity 枚举，违反模块依赖方向。应在 prescription 模块内定义自己的 AllergySeverity 或通过 common-module-api 传递
- **结论**：跨模块依赖仍需解耦

#### T9: DosageThresholdService 频率解析 Integer.parseInt 导致日剂量校验静默失效

- **代码事实**：`DosageThresholdService.java:76-89` — `Integer.parseInt(request.getFrequency())` 被 try/catch 包围，NumberFormatException 被**空 catch 块**（第88-89行）吞掉
- **根因**：**问题确认**。频率字段值如 "tid"、"bid"、"qd" 等文本格式无法解析为整数，parseInt 抛出 NumberFormatException 被静默吞掉，导致日剂量校验被完全跳过，无任何告警
- **触发条件**：处方频率使用文本格式（如 "tid"）时
- **结论**：频率解析和空 catch 仍需修复

#### T10: PrescriptionDraftContext.getCriticalAlerts unchecked cast

- **代码事实**：`PrescriptionDraftContext.java:24-29` — `@SuppressWarnings("unchecked")` 下执行 `(List<DosageAlert>) value` 强制转换
- **根因**：**问题确认**。DraftContextStore 存储的是 Object 类型，取出时无泛型类型校验。若存入非 List<DosageAlert> 类型的值，运行时不会报错但后续使用时可能抛出 ClassCastException
- **结论**：unchecked cast 仍需修复

#### T14: PrescriptionItem.dose 使用 double 存在 BigDecimal 转换精度风险

- **代码事实**：`PrescriptionItem` 的 dose 字段类型为 double（在 PrescriptionAuditServiceImpl.java:263 解析为 `dose.asDouble()`），而 DosageLimitRule.java:40 和 DosageThresholdService.java:52 使用 `BigDecimal.valueOf(item.getDose())` 转换
- **根因**：**问题确认**。double → BigDecimal 转换存在精度丢失（如 0.1d → BigDecimal.valueOf(0.1) = 0.1000000000000000055511151231257827021181583404541015625）。OOD §1.3 明确要求 BigDecimal
- **触发条件**：剂量值为无法精确表示为 double 的小数时
- **结论**：dose 字段类型仍需改为 BigDecimal

#### T15: prescriptionOrderId 使用 System.currentTimeMillis() 存在毫秒级冲突

- **代码事实**：`PrescriptionAuditServiceImpl.java:218,259,289,298,328` — 5 处使用 `"RX-" + System.currentTimeMillis()` 生成 orderId
- **根因**：**问题确认**。同一毫秒内的并发请求将生成相同的 orderId，违反唯一性约束
- **触发条件**：同一毫秒内两个并发 submit 请求
- **结论**：orderId 生成策略仍需改为 UUID 或序列

#### T18: MedicalRecordConverter 使用字面字符串比较错误码

- **代码事实**：`MedicalRecordConverter.java:69,73` — 使用 `"MR_GEN_AI_TIMEOUT".equals(aiResult.getErrorCode())` 字面字符串比较
- **根因**：**问题确认**。应使用 `MedicalRecordErrorCode.MR_GEN_AI_TIMEOUT.getCode()` 枚举值比较，避免字符串硬编码与枚举定义不一致
- **结论**：字面字符串比较仍需改为枚举引用

#### T19: MockAiService.TIMEOUT 策略使用永不完成的 CompletableFuture

- **代码事实**：`MockAiService.java:67` — `return new CompletableFuture<>()` 永不完成
- **根因**：**问题确认**。TIMEOUT 策略返回的 CompletableFuture 永远不会 complete，调用方的 `future.get(timeout)` 虽然会超时返回，但该 Future 对象及其关联的线程资源永远不会被释放，造成资源泄漏
- **触发条件**：MockAiService 使用 TIMEOUT 策略时
- **结论**：资源泄漏仍需修复

#### T20: FallbackAiService.applyStrategies 使用空 DegradationContext

- **代码事实**：`FallbackAiService.java:290-301` — `applyStrategies` 方法中构造 `new DegradationContext()`（第294行），serviceName 和 operationName 均为 null
- **根因**：**问题确认**。applyStrategies 在 thenApply 中被调用，此时无法获取原始请求的 serviceName/operationName 上下文。DegradationStrategy.shouldDegrade() 接收的 context 无有效信息，无法做出正确的降级决策
- **触发条件**：AI 调用失败触发降级策略评估时
- **结论**：DegradationContext 上下文传递仍需修复

#### T21: 元数据字段混入业务字段

- **代码事实**：`MedicalRecordConverter.java:38-45` — MISSING_FIELDS 和 PARTIAL_CONTENT 作为 MedicalRecordField 枚举值写入 content map
- **根因**：**问题确认**。MISSING_FIELDS 和 PARTIAL_CONTENT 是元数据（描述 AI 输出的质量状态），不应与业务字段（CHIEF_COMPLAINT、SYMPTOM_DESCRIPTION 等）混在同一 content map 中。DatabaseTemplateConfigManager 的 DEFAULT_TEMPLATE 包含所有 MedicalRecordField 值（第111行），也包含这两个元数据字段
- **结论**：元数据与业务字段分离仍需实现

#### T22: MedicalRecordContentConverter 序列化/反序列化异常时静默返回 null/emptyMap

- **代码事实**：`MedicalRecordContentConverter.java:29,44-46` — 序列化异常返回 null（第29行），反序列化异常返回 emptyMap（第45行），均无 WARN 日志
- **根因**：**问题确认**。静默返回导致数据丢失不可追踪——序列化失败时数据库写入 null（覆盖原有数据），反序列化失败时业务层拿到空 map（丢失已存储的病历内容）
- **触发条件**：content map 中包含无法序列化的对象，或数据库中存储了非法 JSON
- **结论**：异常日志仍需添加

#### T27: submit() 方法缺乏 prescriptionId 级别并发提交防护

- **代码事实**：`PrescriptionAuditServiceImpl.java:147-195` — submit 方法标注 `@Transactional`，但无 prescriptionId 级别的显式并发控制
- **根因**：**问题确认**。两个并发线程可同时通过 BLOCK 检查（第170行），因为 `findTopByPrescriptionIdAndIsLatestTrueOrderByAuditSequenceDesc` 未加悲观锁。虽然 AuditRecord 有 @Version 乐观锁，但乐观锁仅在 save 时检测冲突，不阻止两个线程同时通过业务逻辑检查
- **触发条件**：同一 prescriptionId 的两个并发 submit 请求
- **结论**：prescriptionId 级别并发防护仍需实现

#### T28: hasNewAlerts 逻辑在正常路径中无实际作用

- **代码事实**：`PrescriptionAuditServiceImpl.java:152-164,182-192` — submit 方法第152行获取 snapshotCriticalAlerts（此时为当前 alerts 快照），第182行再次获取 currentAlerts，第183行调用 hasNewAlerts 比较
- **根因**：**问题确认**。snapshotCriticalAlerts 和 currentAlerts 在同一方法内连续获取，中间无其他操作改变 alerts（updateCriticalAlerts 仅在 assist 方法中调用），因此 hasNewAlerts 在 submit 方法内部几乎总是返回 false（除非有并发 assist 在两次 getCriticalAlerts 之间修改了 alerts）。但更关键的是：第152行 hasCriticalAlerts 为 true 时直接返回 BLOCK，不会执行到第182行；hasCriticalAlerts 为 false 时 snapshotCriticalAlerts 为 emptyList，currentAlerts 若非空则 hasNewAlerts 恒返回 true——但此时 hasCriticalAlerts 也应为 true，逻辑矛盾
- **结论**：hasNewAlerts 逻辑仍需重新设计

#### T30: DosageLimitRule.findBestMatch 返回 null 时静默回退到 standards.get(0)

- **代码事实**：`DosageLimitRule.java:36-38` — `if (matched == null) { matched = standards.get(0); }`
- **根因**：**问题确认**。findBestMatch 返回 null 表示无匹配的剂量标准，静默回退到 standards.get(0) 意味着使用可能完全不相关的剂量标准进行校验，可能产生误判
- **触发条件**：患者年龄/体重不在任何标准的范围内时
- **结论**：静默回退仍需改为显式告警或跳过

#### T31: AllergyCheckRule allergyHistory 自由文本 contains 子串匹配过于激进

- **代码事实**：`AllergyCheckRule.java:54-58` — `allergyHistory.contains(allergen)` 子串匹配
- **根因**：**问题确认**。`"No allergy to penicillin".contains("penicillin")` 返回 true，导致误判为过敏。应使用单词边界匹配或否定语境识别
- **触发条件**：allergyHistory 包含否定语境的过敏原文（如 "无青霉素过敏"）
- **结论**：文本匹配策略仍需改进

#### T32: DraftContextCleanupTask 迭代 keySet() 与 writeTimestamps 之间非原子

- **代码事实**：`DraftContextCleanupTask.java:38-45` — 先迭代 `draftContextStore.keySet()`，再对每个 key 查 `writeTimestamps.get(key)`
- **根因**：**问题确认**。与 P03/S02 根因相同——writeTimestamps 永远为空（recordWrite 无调用方），cleanup 永远不会执行删除。即使 recordWrite 被调用，keySet 迭代与 writeTimestamps.get 之间也存在 TOCTOU 窗口
- **结论**：与 P03/S02 合并修复

#### T35: AuditConverter + ai-api DTO 遗漏 weight 和 unit 字段映射

- **代码事实**：`AuditConverter.java:77-92` — toAiPatientInfo 未映射 weight 字段；`AuditConverter.java:66-75` — toAiCheckItem 未映射 unit 字段
- **根因**：**问题确认**。PatientInfo（ai-api）有 weight 字段但 bizPatient.getWeight() 未被映射；PrescriptionCheckItem（ai-api）可能无 unit 字段需确认，但 PrescriptionItem 有 unit 字段
- **结论**：字段映射遗漏仍需修复

#### T40: @Recover 方法缺陷

- **代码事实**：`RegistrationEventListener.java:52-66` — recover 方法：
  1. 无 @Transactional（第52行）— deadLetterEventRepository.save() 无事务保护
  2. `e.getMessage()` 可能为 null（第60行）— DeadLetterEvent.failReason 标注 NOT NULL 时将违反约束
  3. JSON 兜底生成 `"null"` 字符串（第58行）— 当 event.getSessionId() 为 null 时，拼接结果为 `"\"sessionId\":\"null\""`
- **根因**：**问题确认**。三个缺陷均存在
- **结论**：@Recover 方法仍需修复

### 3.3 medical-record 模块

#### M02: TemplateConfigChangeEvent 事件定义 + @EventListener + Caffeine invalidate 全未实现

- **代码事实**：`DatabaseTemplateConfigManager.java:105-108` — 已实现 `@EventListener` 监听 `TemplateConfigChangeEvent`，调用 `templateCache.invalidateAll()`
- **根因**：**部分已修复**。@EventListener 和 invalidateAll 已实现。但 TemplateConfigChangeEvent 本身需确认是否已定义（import 路径 `com.aimedical.modules.medicalrecord.event.TemplateConfigChangeEvent`），以及事件发布方（DeptTemplateConfig 修改时是否发布事件）是否已实现
- **结论**：事件监听已实现，事件发布方需确认

#### M03: VisitIdReconciledTask 定时调和任务未实现

- **根因**：**问题确认**。medical-record task/ 包不存在该任务类。VisitFacade 调用失败时使用 encounterId 作为 fallback visitId，但无后续调和任务将 fallback visitId 修正为真实 visitId
- **结论**：调和任务仍需实现

#### M05: RecordGenerateRequest 缺少验证注解

- **代码事实**：`RecordGenerateRequest.java:7-9` — dialogueText 已有 `@NotNull` 和 `@Size(min = 50, max = 10000)` 注解
- **结论**：**已修复**

#### M06: 超时硬编码在源码中未通过 @Value 注入

- **代码事实**：`MedicalRecordServiceImpl.java:40-44` — 已通过 @Value 注入
- **结论**：**已修复**

#### M07: MedicalRecordConverter.toRecordGenerateResponse 在非超时失败时仍设 success=true

- **代码事实**：`MedicalRecordConverter.java:72-74` — success 判定逻辑：`(aiResult.isSuccess() && aiResult.getData() != null) || "MR_GEN_AI_TIMEOUT".equals(aiResult.getErrorCode())`
- **根因**：**问题确认**。当 aiResult 为 degraded（非超时原因）时，`isSuccess()` 为 false 但 `getErrorCode()` 不为 "MR_GEN_AI_TIMEOUT"，此时 success 为 false——此路径正确。但当 aiResult.getErrorCode() 为其他错误码（如 "AI_UNAVAILABLE"）且 aiResult.getData() 非 null 时，`isSuccess() && getData() != null` 为 false，success 仍为 false——此路径也正确。但 `"MR_GEN_AI_TIMEOUT".equals(aiResult.getErrorCode())` 条件使得超时降级时 success=true，这与 OOD 中"超时属于降级应标记 success=false"的语义可能不一致
- **结论**：超时场景的 success 语义仍需确认

#### M09: MedicalRecord.doctorId 在 generate() 流程中从未被赋值

- **代码事实**：`MedicalRecordServiceImpl.java:107` — `entity.setDoctorId(request.getDoctorId())` 已赋值
- **结论**：**已修复**

#### M11: ai-api MedicalRecordGenResponse.missingFields 与 partialContent 未被业务层消费

- **代码事实**：`MedicalRecordConverter.java:38-45` — missingFields 已被消费（第38-39行：写入 MISSING_FIELDS 字段），partialContent 已被消费（第41-44行：写入 PARTIAL_CONTENT 字段）
- **根因**：**部分已修复**。字段已被写入 content map，但与 T21 问题关联——这两个元数据字段不应混在业务 content map 中
- **结论**：与 T21 合并处理

#### T17: callAiWithTimeout 异常语义混淆 + 未用 AiResultFactory

- **代码事实**：`MedicalRecordServiceImpl.java:145-159` — 三种异常（TimeoutException、InterruptedException、ExecutionException）统一返回 `AiResultFactory.degraded("AI medical record generation timeout", "MR_GEN_AI_TIMEOUT", null)`
- **根因**：**问题确认**。ExecutionException（AI 服务内部错误）和 InterruptedException（线程中断）不应使用与 TimeoutException 相同的降级消息和错误码。语义混淆导致调用方无法区分超时和其他故障
- **结论**：异常语义区分仍需修复

#### T47: MedicalRecord @PrePersist 未设置 updatedAt

- **代码事实**：`MedicalRecord.java:133-136` — `@PrePersist` 仅设置 `this.createdAt = LocalDateTime.now()`，未设置 updatedAt
- **根因**：**问题确认**。新增记录的 updatedAt 为 null，与 @PreUpdate（第139-141行）仅更新 updatedAt 的设计不一致。首次插入后 updatedAt 为 null，查询时可能引发 NPE
- **结论**：@PrePersist 仍需补充 updatedAt 设置

#### T48: MedicalRecordServiceImpl.resolveVisitId 使用 ForkJoinPool.commonPool()

- **代码事实**：`MedicalRecordServiceImpl.java:132` — `CompletableFuture.supplyAsync(() -> visitFacade.findVisitIdByEncounterId(encounterId))` 未指定 Executor，默认使用 ForkJoinPool.commonPool()
- **根因**：**问题确认**。Web 请求线程（Tomcat）在高并发下已接近饱和，ForkJoinPool.commonPool() 的线程数默认为 CPU 核心数-1，被 CompletableFuture 和并行流共享。VisitFacade 的阻塞式调用会占用 commonPool 线程，导致其他并行操作饥饿
- **触发条件**：高并发下多个请求同时调用 generate()
- **结论**：应注入专用 Executor 仍需修复

#### T50: DraftContextStoreImpl 缺少 compute/putIfAbsent 原子操作

- **代码事实**：ConcurrentHashMapStore 已实现 compute 和 createIfNotExists（基于 putIfAbsent），但 DraftContextStore 接口需确认是否暴露这些方法
- **结论**：与 S07 合并处理

### 3.4 application / 跨模块

#### A05: MockAiService 与设计契约严重不符

- **代码事实**：`MockAiService.java:40-42` — 使用 `@Profile("mock")` 而非 `@ConditionalOnProperty`；已有三种返回模式（STATIC/AI_UNAVAILABLE/TIMEOUT，第44-46行）；无 MockAdminController
- **根因**：**部分已修复**。三种返回模式已实现。但：(1) 使用 @Profile 而非 @ConditionalOnProperty——@Profile 是 Spring 标准方式，与 OOD 要求的 @ConditionalOnProperty 不一致，但功能等价；(2) 无 MockAdminController——运行时无法动态切换策略，需重启修改配置
- **结论**：MockAdminController 仍需实现

#### A06: DegradationStrategy/DegradationContext 为空壳

- **代码事实**：
  - `DegradationStrategy.java:3-6` — 仅定义 `boolean shouldDegrade(DegradationContext context)` 方法，无实现类
  - `DegradationContext.java:3-26` — 仅含 serviceName 和 operationName 两个 String 字段
  - `FallbackAiService.java:61-80` — selectDelegate 遍历 delegates，对每个 delegate 检查所有 strategy，若任一 strategy.shouldDegrade 返回 true 则跳过。无 strategy 实现时，所有 delegate 都不会被跳过，总是选择第一个非 FallbackAiService 的 delegate
- **根因**：**问题确认**。DegradationStrategy 无实现类，FallbackAiService 的降级决策逻辑实际为空——总是选择 delegates.get(0)（第一个非自身的 AiService），无重试链
- **结论**：降级策略实现仍需补全

#### A09: AuditConverter.toAuditResponse 在 aiData == null 时退化为 PASS + 空 alerts

- **代码事实**：`AuditConverter.java:48-56` — aiData == null 时返回 PASS + 空 alerts
- **根因**：**问题确认**。AI 返回成功但 data 为 null 时（如 AiResult.success(null)），审核结果被错误地标记为 PASS，跳过了所有安全检查。应在调用方拦截此场景
- **触发条件**：AiResult.isSuccess()=true 但 getData()=null
- **结论**：aiData null 检查仍需改为调用方拦截

#### E05: RegistrationEventListener.recover() 手工构造含 3 字段的 HashMap

- **代码事实**：`RegistrationEventListener.java:55-58` — 使用 `objectMapper.writeValueAsString(event)` 序列化，catch 中兜底为 `"{"sessionId":"" + event.getSessionId() + ""}"`
- **根因**：**部分已修复**。主路径使用 ObjectMapper 序列化完整事件对象。但兜底路径仍有问题：(1) event.getSessionId() 可能为 null，拼接结果为 `"sessionId":"null"`；(2) 兜底 JSON 仅含 sessionId，丢失 departmentId 和 departmentName，DeadLetterCompensationService 补偿时无法获取完整信息
- **结论**：兜底路径 null 防护和信息完整性仍需修复

---

## 4. P2 问题根因诊断

### 4.1 consultation 模块

#### C11: TTL 清理周期为 1 分钟而非设计要求的 5 分钟

- **代码事实**：`DialogueSessionManager.java:73` — `@Scheduled(fixedRate = 300000)` = 300,000ms = 5 分钟
- **结论**：**已修复**

#### C18: saveTriageRecord 中 catch JsonProcessingException 完全静默

- **代码事实**：`TriageServiceImpl.java:251-253` — `catch (JsonProcessingException e) { log.warn("Failed to serialize triage record JSON fields for sessionId: {}", request.getSessionId(), e); }`
- **结论**：**已修复**（已有 WARN 日志）

#### T44: TriageResponse 缺少 correctedChiefComplaint 字段

- **根因**：**问题确认**。TriageResponse DTO 未包含 correctedChiefComplaint 字段，前端无法感知 AI 对主诉的修正
- **结论**：字段仍需添加

#### T45: TriageServiceImpl.selectDepartment 缺少自动生成 UUID v4 校验

- **代码事实**：`RegistrationEventListener.java:45` — `triageRecordRepository.findBySessionId(event.getSessionId())` — event.getSessionId() 可能为 null
- **根因**：**问题确认**。RegistrationEvent 的 sessionId 无 null 防护，传入 null 时 findBySessionId 的 @Lock(PESSIMISTIC_WRITE) + MANDATORY 传播将抛出异常
- **结论**：null 防护仍需添加

### 4.2 prescription 模块

#### P09: PrescriptionItem.unit 字段为 OOD 未正式定义的扩展字段

- **根因**：**问题确认**。PrescriptionItem 有 unit 字段但 OOD 文档未正式定义，需在 OOD 中补充
- **结论**：OOD 文档补充仍需完成

#### P12: DrugInteractionRule 无 Phase 4 预留标注

- **代码事实**：`DrugInteractionRule.java:1-15` — 空实现，无 @ConditionalOnProperty
- **根因**：**问题确认**。空实现本身是 OOD 允许的，但缺少 @ConditionalOnProperty 标注用于 Phase 4 启用
- **结论**：@ConditionalOnProperty 标注仍需添加

#### P15: AuditResponse.fromFallback 未序列化到 JSON 响应

- **根因**：**问题确认**。降级 BLOCK 路径的 reasons 使用固定字符串，fromFallback 标记可能未正确传递到前端
- **结论**：降级响应序列化仍需修复

#### M10: MedicalRecord.content 字段无 @Column(name="content_json")

- **代码事实**：`MedicalRecord.java:39` — 已有 `@Column(name = "content_json", columnDefinition = "TEXT")`
- **结论**：**已修复**

#### A08: 降级路径 fallback 文案硬编码英文

- **代码事实**：`TriageServiceImpl.java:171,177` — "AI 服务不可用，已切换至规则引擎降级" 和 "AI 服务持续不可用，建议稍后重试" 为中文
- **结论**：**已修复**（文案为中文，非英文硬编码）

### 4.3 application / 跨模块

#### A07: AiResult.success(data) 允许 data=null

- **代码事实**：`AiResult.java:24-26` — 静态方法 `success(T data)` 使用 `Objects.requireNonNull(data)` 断言，不允许 null。但 `AiResultFactory.success(T data)` 第25行 `return new AiResult<>(true, data, null, false, null)` **未做 null 检查**，允许 data=null
- **根因**：**问题确认**。AiResult 静态方法和 AiResultFactory 的 success 方法对 null data 的处理不一致
- **结论**：AiResultFactory.success 仍需添加 null 检查

#### A11: 业务层普遍在 isSuccess() 后额外做 getData() != null 防御性检查

- **代码事实**：`TriageServiceImpl.java:137` — `if (aiResult != null && aiResult.isSuccess())` 后第138行 `aiResult.getData()` 未做 null 检查（直接使用）；`PrescriptionAuditServiceImpl.java:110` — `if (aiResult != null && aiResult.isSuccess())` 后第111行直接调用 `auditConverter.toAuditResponse(aiResult)`
- **根因**：**部分问题**。当前代码中 isSuccess() 后未额外做 getData() != null 检查，依赖 AiResult.success() 的 Objects.requireNonNull 保证。但 AiResultFactory.success() 无此保证，若通过 AiResultFactory 构造 success result 传入 null data，调用方将拿到 null
- **结论**：与 A07 合并修复

#### T24: ConcurrentHashMapStore 缺少 Spring 注解

- **代码事实**：`ConcurrentHashMapStore.java:10` — 无 @Component 或 @Service 注解
- **根因**：**问题确认**。ConcurrentHashMapStore 未注册为 Spring Bean，需通过其他方式（如 @Bean 方法或 @Configuration）注册
- **结论**：Spring 注解仍需添加

---

## 5. 诊断总结

### 5.1 已修复项（无需行动）

以下问题经代码验证已修复：C01, C02, C04, C05, C07, C08, C15, C20, C21, E02, A01(部分), P01, A02, A03, M01, M04, C03/A04, A10, C09, C10(误报), C13, C19, C14, C22, P05(部分), P06, P07, P16, S01(误报), S06, M05, M06, M09, C11, C18, M10, A08

### 5.2 仍需修复项（按模块分组）

#### consultation 模块
| ID | 根因摘要 | 代码位置 |
|----|---------|---------|
| C06(部分) | DoctorFacade 调用无超时控制 | TriageServiceImpl.java:221 |
| S04(部分) | restoreSession 缺 synchronized | DialogueSessionManager.java:50 |
| C16(部分) | JSON 解析失败静默返回 true | DefaultTriageRuleEngine.java:113-115 |
| C17 | 排序按 availableSlotCount 而非 score | TriageServiceImpl.java:232-234 |
| T4 | 降级路径手工构造 Response，缺 matchedRules | TriageServiceImpl.java:168-181 |
| T5 | TriageConverter 副作用修改 Session | TriageConverter.java:107-109 |
| T42 | 缓存无事件驱动失效 | DefaultTriageRuleEngine.java:30-37 |
| T44 | TriageResponse 缺 correctedChiefComplaint 字段 | dto/TriageResponse.java |
| T45 | RegistrationEvent.sessionId 无 null 防护 | RegistrationEventListener.java:45 |

#### prescription 模块
| ID | 根因摘要 | 代码位置 |
|----|---------|---------|
| P02 | DrugFacade 未注入 | PrescriptionAuditServiceImpl.java:72-87, PrescriptionAssistServiceImpl.java:57-75 |
| P03/S02/T32 | TTL 清理完全无效（recordWrite 无调用方 + 类型不匹配） | DraftContextCleanupTask.java:27-29, SuggestionCleanupTask.java:31 |
| P04 | 药品规则变更事件缺失 | prescription/event/ 包不存在 |
| S03 | compute 内嵌套跨 key put | DedupTaskScheduler.java:61-62 |
| P08(部分) | SubmitRequest 缺 prescriptionHash 字段 | SubmitRequest.java:7-53 |
| P10 | matchByPriority 循环逻辑重复 | DosageThresholdService.java:103-121 |
| P11 | 通用/特殊人群剂量标准查询未区分 | DosageLimitRule.java:30, SpecialPopulationDosageRule |
| P13 | 结构化匹配未命中时不回退文本匹配 | AllergyCheckRule.java:43-60 |
| P14(部分) | 失败路径未写入失败状态到 DraftContext | PrescriptionAssistServiceImpl.java:90-118 |
| T8 | AllergySeverity 跨模块依赖 patient | AllergyCheckRule.java:3 |
| T9 | 频率解析空 catch 吞异常 | DosageThresholdService.java:76-89 |
| T10 | getCriticalAlerts unchecked cast | PrescriptionDraftContext.java:24-29 |
| T14 | dose 使用 double 非 BigDecimal | PrescriptionItem.dose |
| T15 | orderId 用 System.currentTimeMillis() | PrescriptionAuditServiceImpl.java:218,259,289,298,328 |
| T16 | hasCriticalAlerts/getCriticalAlerts TOCTOU | PrescriptionDraftContext.java:19-22 |
| T27 | submit 无 prescriptionId 级并发防护 | PrescriptionAuditServiceImpl.java:147-195 |
| T28 | hasNewAlerts 逻辑矛盾 | PrescriptionAuditServiceImpl.java:152-164,182-192 |
| T30 | findBestMatch null 时静默回退 standards.get(0) | DosageLimitRule.java:36-38 |
| T31 | allergyHistory contains 子串匹配过激进 | AllergyCheckRule.java:54-58 |
| T35 | AuditConverter 遗漏 weight/unit 映射 | AuditConverter.java:66-92 |
| P09 | PrescriptionItem.unit OOD 未定义 | PrescriptionItem.unit |
| P12 | DrugInteractionRule 缺 @ConditionalOnProperty | DrugInteractionRule.java |
| P15 | 降级 BLOCK 路径 reasons 固定字符串 | PrescriptionAuditServiceImpl.java:144-152 |

#### medical-record 模块
| ID | 根因摘要 | 代码位置 |
|----|---------|---------|
| M02(部分) | TemplateConfigChangeEvent 发布方未确认 | DatabaseTemplateConfigManager.java:105-108 |
| M03 | VisitIdReconciledTask 未实现 | medical-record task/ 包 |
| M07 | 超时降级时 success=true 语义待确认 | MedicalRecordConverter.java:72-74 |
| T17 | callAiWithTimeout 三种异常统一返回 timeout | MedicalRecordServiceImpl.java:145-159 |
| T18 | 字面字符串比较错误码 | MedicalRecordConverter.java:69,73 |
| T19 | MockAiService TIMEOUT 策略资源泄漏 | MockAiService.java:67 |
| T20 | applyStrategies 使用空 DegradationContext | FallbackAiService.java:290-301 |
| T21 | 元数据字段混入业务 content map | MedicalRecordConverter.java:38-45 |
| T22 | ContentConverter 异常静默返回 null/emptyMap | MedicalRecordContentConverter.java:29,44-46 |
| T47 | @PrePersist 未设置 updatedAt | MedicalRecord.java:133-136 |
| T48 | resolveVisitId 使用 ForkJoinPool.commonPool() | MedicalRecordServiceImpl.java:132 |

#### 跨模块
| ID | 根因摘要 | 代码位置 |
|----|---------|---------|
| A05(部分) | 缺 MockAdminController | MockAiService.java |
| A06 | DegradationStrategy 无实现，FallbackAiService 无重试链 | DegradationStrategy.java, FallbackAiService.java:61-80 |
| A07 | AiResultFactory.success 允许 null data | AiResultFactory.java:25 |
| A09 | aiData==null 退化为 PASS | AuditConverter.java:48-56 |
| E05(部分) | recover 兜底路径 null 防护缺失 | RegistrationEventListener.java:55-58 |
| T24 | ConcurrentHashMapStore 缺 Spring 注解 | ConcurrentHashMapStore.java:10 |
| T40 | @Recover 无事务 + getMessage 可 null + JSON 兜底 "null" | RegistrationEventListener.java:52-66 |

### 5.3 跨问题耦合约束（修复顺序建议）

| 群组 | 关联项 | 修复顺序 | 根因说明 |
|------|--------|---------|---------|
| correctedChiefComplaint 数据流 | C01→A04→C03→C19→C23 | 全部已修复 | 数据流已贯通 |
| 事务边界+并发控制 | C04+E02+S04 | C04/E02 已修复，S04(restoreSession) 仍需修复 | restoreSession 缺同步导致并发恢复覆盖 |
| DoctorFacade+排序 | C06+C17 | 先加超时，再改排序 | 同一方法 findDoctorsForDepartments 内两处修改 |
| DrugFacade+降级+事件 | P02+P06+P04 | P02 先注入 DrugFacade | 降级路径和事件监听依赖 DrugFacade 数据 |
| AI 超时配置 | A02+A10 | 全部已修复 | 配置已外化 |
| 异步 AI 调度管线 | P01+A03 | 全部已修复 | scheduleSuggestionAsync 已实现 |
| AiResult 契约 | A07+A09+A11 | A09 调用方拦截→A07 断言→A11 移除冗余判空 | AiResultFactory.success 允许 null 是根因 |
| TTL 清理 | P03+S02+T32 | 先修复 recordWrite 调用，再修 SuggestionCleanupTask 类型适配 | recordWrite 无调用方是根因 |
| 降级策略 | A06+T20 | 先实现 DegradationStrategy，再修复 applyStrategies 上下文传递 | DegradationStrategy 无实现是根因 |
