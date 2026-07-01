# 代码审查问题诊断报告

## P0 — 必须立即修复

### consultation 模块

#### C06: DoctorFacade 跨模块调用无超时

- **现象**：TriageServiceImpl.java:213-236 的 findDoctorsForDepartments 方法无超时控制
- **根因**：try/catch（第226行）和 WARN 日志（第228-229行）已存在，但 `application.yml:29` 配置的 `consultation.doctor-facade.timeout: 2` 未被 TriageServiceImpl 注入，`doctorFacade.findAvailableDoctorsByDepartment()` 调用无超时保护
- **因果链**：doctorFacade 调用 → 无超时 → 下游服务阻塞 → 线程耗尽

#### C20: createSession 使用 put 而非 putIfAbsent

- **现象**：DialogueSessionManager.java:42 使用 `sessionStore.put(sessionId, session)` 覆盖已有 session
- **根因**：createSession（第33行）已加 `synchronized`，且第37-39行先检查 `containsKey` 再 put，synchronized 内 containsKey+put 是原子的。但 restoreSession（第50行）无 synchronized，第68行的 put 可能与 createSession 的 put 产生竞态。putIfAbsent 语义更清晰且不依赖 synchronized
- **因果链**：并发 restoreSession → 第54行读到 null → 第68行 put → 覆盖 createSession 刚创建的 session → 数据丢失

#### E02: TriageRecord 同 sessionId 第二次分诊违反 @Column(unique=true) 约束

- **现象**：saveTriageRecord 在并发场景下可能违反 sessionId 唯一约束
- **根因**：TriageServiceImpl.java:257-291 的 saveTriageRecord 使用 `transactionTemplate.execute()` 包围整个逻辑。第258行 `findBySessionId` 标注了 `@Lock(LockModeType.PESSIMISTIC_WRITE)` + `@Transactional(propagation = MANDATORY)`（TriageRecordRepository.java:19-20），在 transactionTemplate 创建的事务内执行，具有悲观写锁保护
- **因果链修正**：由于 findBySessionId 在事务+锁保护下执行，并发场景下两个线程不会同时绕过 findBySessionId 直接 INSERT。但以下场景仍可能导致唯一约束冲突：
  - **场景 A**：第一次分诊（sessionId 无已有记录）→ 两个并发请求同时进入 saveTriageRecord → 线程1 findBySessionId 获取锁 → 未查到 → 释放锁 → INSERT → 线程2 findBySessionId 获取锁 → 未查到（线程1 INSERT 后事务未提交？取决于事务隔离级别）→ 若隔离级别为 READ_UNCOMMITTED 则可能看到未提交数据，否则两个线程均 INSERT → 唯一约束冲突
  - **场景 B**：findBySessionId 使用 PESSIMISTIC_WRITE 锁，但锁仅对已存在的行生效。当行不存在时，不同数据库对"锁空行"的行为不同（MySQL InnoDB 使用 gap lock，PostgreSQL 不锁定不存在的行）。若数据库不支持锁不存在的行，并发 INSERT 仍可能违反唯一约束
- **影响范围**：当前数据库使用 H2（内存模式）。H2 在 MVCC 模式下，PESSIMISTIC_WRITE 对不存在的行不产生 gap lock（与 PostgreSQL 行为类似，与 MySQL InnoDB 不同）。故并发首次 INSERT 场景下唯一约束冲突风险真实存在。saveTriageRecord 第261-265 行已有"查到则 UPDATE"的逻辑，但首次 INSERT 无记录时两线程均走新建路径
- **修复方案（备选）**：
  1. 改用 `INSERT ... ON DUPLICATE KEY UPDATE` / `MERGE` 语义（H2 支持 `MERGE INTO`）；
  2. 在 saveTriageRecord 入口加 `ConcurrentHashMap<String, Lock>` 按 sessionId 串行化；
  3. 在 `TriageRecord` 上加 DB 层 `@SQLInsert` 重写为 upsert

#### S04: 同 session 并发访问无串行化保护

- **现象**：restoreSession 缺 synchronized，并发恢复可能导致数据丢失
- **根因**：DialogueSessionManager.java:50 的 restoreSession 无 synchronized 标注。并发 restoreSession 可能在第54行同时读到 null，然后第68行同时 put 不同 session 实例
- **约束条件**：restoreSession.java:59-60 调用 `findTopBySessionIdOrderByTriageTimeDesc`，该方法无事务标注（Spring Data JPA 默认 REQUIRED），从非事务上下文调用会**正常开启新事务**而非抛异常。但 saveTriageRecord（被 triage 调用）入口由 `transactionTemplate.execute()` 包围，restoreSession 在此事务内调用 `findTopBySessionIdOrderByTriageTimeDesc` 会复用同一事务，导致保存与读取在同一事务中（一般情况下可接受）。需关注的是 restoreSession 的非同步性才是核心问题
- **影响范围**：核心竞态为 restoreSession 缺 synchronized；MANDATORY 传播约束不适用于此方法

### prescription 模块

#### A01: AiResultFactory 在 Prescription 业务 Service 中零引用

- **根因**：
  - PrescriptionAuditServiceImpl.java:96-105 AI 调用失败时 `aiResult` 被设为 `null`，而非使用 AiResultFactory 构造降级结果
  - PrescriptionAssistServiceImpl.java:89-99 AI 调用失败时未使用 AiResultFactory，调用 `clearCriticalAlerts` 旁路

#### P01: 异步 AI 调度机制未实现

- **现象**：schedule 创建 PENDING 后无代码触发 AiService.prescriptionAssist()
- **根因**：DedupTaskScheduler.java:21-69 的 schedule 方法仅创建 PENDING 状态的 AiSuggestionResult 并存入 SuggestionStore。PrescriptionAssistServiceImpl.java:105 调用 `scheduleSuggestionAsync(taskId, request)` 启动异步 AI 调用，但 scheduleSuggestionAsync（第339-369行）使用 `CompletableFuture.supplyAsync()` 启动异步任务，未使用线程池，且返回的 CompletableFuture 未被任何代码消费（第369行无 return 或赋值），异步任务的异常无法被捕获
- **因果链**：assist() → scheduleSuggestionAsync 启动异步任务 → 异步任务内部调用 aiService.prescriptionAssist() → 结果写入 suggestionStore → 但异步任务未绑定线程池（使用 ForkJoinPool.commonPool()）且异常未处理

#### P02: DrugFacade 在 PrescriptionAuditServiceImpl 和 PrescriptionAssistServiceImpl 均未注入

- **根因**：PrescriptionAuditServiceImpl.java:72-87 构造函数无 DrugFacade 参数；PrescriptionAssistServiceImpl.java:57-75 构造函数无 DrugFacade 参数

#### P03/S02: AiSuggestionResult + PrescriptionDraftContext TTL 清理任务失效

**DraftContextCleanupTask 失效原因**：
- DraftContextCleanupTask.java:20 的 `writeTimestamps` 是本地 ConcurrentHashMap，第27行的 `recordWrite()` 方法无任何调用方。writeTimestamps 永远为空
- cleanupExpiredDrafts（第36-46行）遍历 draftContextStore.keySet()，对每个 key 查 writeTimestamps.get(key)，ts 始终为 null，第40行条件 `ts != null` 永远不满足，无任何条目被清理
- **根因定位**：DraftContextCleanupTask.recordWrite() 无调用方 → writeTimestamps 永远空 → 清理条件永远不满足

**SuggestionCleanupTask 失效原因**：
- AiSuggestionResult.java:8 已声明 `implements SuggestionStoreEntry`，instanceof 检查会成功
- scheduleSuggestionAsync（PrescriptionAssistServiceImpl.java:339-369）中创建的 AiSuggestionResult（第341行）未调用 `setCreateTime()`，createTime 为 null。AiSuggestionResult.getTimestamp()（第83-85行）在 createTime 为 null 时返回 null
- isExpiredAndConsumed 第46行 `entry.getTimestamp().plusSeconds(...)` 对 null 调用方法，抛出 NullPointerException。此 NPE 被 cleanupExpiredSuggestions 第36行的 `catch (ClassCastException e)` 捕获——但 NPE 不是 ClassCastException，不会被此 catch 块捕获，导致整个清理方法异常终止，后续条目不会被处理
- 即使 NPE 被修复，FAILED 状态的 consumed 永远为 false（仅 getSuggestion 对 COMPLETED 设置 consumed=true），FAILED 条件不会被清理
- **根因定位**：(a) scheduleSuggestionAsync 未设置 createTime → getTimestamp() 返回 null → NPE → 清理方法异常终止；(b) FAILED 状态 consumed 永远为 false

#### S03: DedupTaskScheduler.schedule() 在 createIfNotExists 后嵌套跨 key 操作

- **根因**：DedupTaskScheduler.java:38-40 的 `createIfNotExists(dedupKey, newResult)` 成功后（返回 null），第40行执行 `suggestionStore.put(candidateTaskId, newResult)`。createIfNotExists 与 put 不在同一原子操作中，存在竞态：createIfNotExists 成功后、put 执行前，另一个线程可能操作 candidateTaskId。compute lambda（第51-59行）操作 dedupKey，第62行的 put 在 compute 外部，不在 compute lambda 内部

### medical-record 模块

#### A03: AiSuggestionResult 状态映射不完整

- **根因**：AiSuggestionStatus 枚举仅定义 PENDING/COMPLETED/FAILED 三种状态，缺少 OOD 要求的 PROCESSING 和 TIMEOUT 状态。scheduleSuggestionAsync 在异步任务中直接设置 COMPLETED 或 FAILED，无中间状态转换

#### M04: MR_GEN_CONCURRENT_MODIFICATION 在当前 INSERT 路径中无法触发

- **根因**：MedicalRecordServiceImpl.java:102-103 使用 `findByVisitId().orElseGet(MedicalRecord::new)`，当记录不存在时创建新实体（无 ID），save 执行 INSERT 而非 UPDATE。INSERT 不会触发乐观锁冲突。此错误码仅在 UPDATE 路径（记录已存在）中有意义

### application / 跨模块

#### C03/A04: correctedChiefComplaint 数据流群组 — API 响应层断裂

- **状态**：数据流 AI → session → TriageRecord 持久化已打通（实体字段、隐式路径、显式路径、持久化路径均已实现）。但 consultation TriageResponse（dto/TriageResponse.java:5-18）缺少 correctedChiefComplaint 字段（详见 P2 — T44）
- **冗余副作用**：TriageConverter.java:107-109 与 TriageServiceImpl.java:148-150 重复设置 session.correctedChiefComplaint，Converter 修改 Session 是副作用。建议移除 Service 重复段，由 Converter 作为唯一边界（或反之）
- **因果链**：AI 返回 correctedChiefComplaint → session 被设置 → 持久化到 TriageRecord → 但前端 TriageResponse 无此字段 → 前端无法感知修正


## P1 — 严重影响业务逻辑

### consultation 模块

#### C13: TriageRuleEngine.match 无快照失效回退逻辑

- **根因**：DefaultTriageRuleEngine.java:55-59 已实现 ruleVersionMismatch 检测（当版本过滤后无匹配规则时设 ruleVersionMismatch=true 并回退到全部规则），MatchResult 携带 ruleVersionMismatch 标记。但 TriageServiceImpl 降级路径（第175行）读取了此标记并设置到 response 中。此条已部分修复——ruleVersionMismatch 检测已实现，但缺少 OOD 要求的输出标记规范

#### C16: DefaultTriageRuleEngine.match conditions JSON 解析失败时静默返回 true

- **根因**：DefaultTriageRuleEngine.java:82-116 的 matchesConditions 方法中，当 conditionsJson 为 null/空（第83-85行）返回 true，keywords 为 null/空数组（第89-91行）返回 true，JSON 解析失败（第113-115行）返回 true。这意味着规则条件解析失败时，规则无条件匹配所有主诉，可能导致所有患者被分诊到同一科室

#### C17: findDoctorsForDepartments 未按匹配评分排序取前 5 名

- **根因**：TriageServiceImpl.java:232-235 使用 `availableSlotCount` 降序排序，而非 OOD 要求的 score 排序。RecommendedDoctor 构造时 score 硬编码为 0f（第224行）

#### T4: TriageServiceImpl 降级路径手工构造 Response 而非复用 Converter

- **根因**：TriageServiceImpl.java:167-181 降级路径手工构造 TriageResponse，遗漏 matchedRules 字段

#### T5: TriageConverter 副作用修改 Session + DialogueSession 混用同步机制

- **根因**：TriageConverter.java:107-109 修改 Session 状态（副作用），与 TriageServiceImpl.java:148-150 重复设置 session.correctedChiefComplaint。DialogueSession 混用 synchronized/AtomicInteger/CopyOnWriteArrayList

#### T16: PrescriptionDraftContext.hasCriticalAlerts 与 getCriticalAlerts 缺少原子操作

- **根因**：PrescriptionDraftContext.java:19-22 的 hasCriticalAlerts 调用 getCriticalAlerts 获取列表后判空，PrescriptionAuditServiceImpl.java:152-154 先调 hasCriticalAlerts 再调 getCriticalAlerts，两次调用间列表可能被并发修改（TOCTOU 窗口）

#### T42: DefaultTriageRuleEngine 缓存仅依赖 refreshAfterWrite 无主动刷新

- **根因**：DefaultTriageRuleEngine.java:30-37 的 Caffeine 缓存仅配置 refreshAfterWrite(60s)，无事件驱动失效机制。规则变更后最长需 60 秒才生效

### prescription 模块

#### P10: DosageThresholdService.matchByPriority 循环逻辑重复

- **根因**：DosageThresholdService.java:103-121 第一重循环（exactAge && exactWeight）和第二重循环（ageInRange && weightInRange）中，当 exactAge 和 ageInRange 同时为 true 时（age 在范围内但不精确等于边界），两重循环匹配到相同结果。第一重循环的精确匹配条件 `as.equals(ae) && as.equals(age)` 过于严格，实际效果与第二重循环等价

#### P14: PrescriptionAssistServiceImpl 各失败路径未写入 PrescriptionDraftContext

- **根因**：PrescriptionAssistServiceImpl.java:90-99,108-111,114-118 的失败路径调用 clearCriticalAlerts（清空 alerts），而非写入失败状态到 DraftContext

#### T8: AllergyCheckRule + AllergyDetail 跨模块依赖

- **根因**：rule/AllergyCheckRule.java:3 和 dto/audit/AllergyDetail.java:3 依赖 patient 模块的 AllergySeverity

#### T9: DosageThresholdService 频率解析导致静默失效

- **根因**：DosageThresholdService.java:76-89 的 parseInt 对非数字频率字符串（如 "tid"）抛 NumberFormatException，被空 catch 吞掉，日剂量校验静默失效

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

- **根因**：FallbackAiService.java:290-301 的 applyStrategies 方法使用空 DegradationContext（第294行 `new DegradationContext()` 无 serviceName/operationName）。delegate 选择阶段（selectDelegate 第87-89行）正确设置了 DegradationContext，但结果后处理阶段使用空 context，降级策略无法根据服务名/操作名做差异化决策。结合 NoOpDegradationStrategy 始终返回 false，applyStrategies 永远不触发降级标记

#### T21: 元数据字段混入业务字段

- **根因**：MedicalRecordConverter.java:38-45 将 MISSING_FIELDS/PARTIAL_CONTENT 元数据字段写入 content_map，DatabaseTemplateConfigManager.java:111 的 DEFAULT_TEMPLATE 包含所有 MedicalRecordField（含这两个元数据字段）

#### T22: MedicalRecordContentConverter 序列化/反序列化异常时静默返回

- **根因**：MedicalRecordContentConverter.java:29,44-46 异常时返回 null/emptyMap，无 WARN 日志

#### T27: submit() 缺乏 prescriptionId 级别并发提交防护

- **根因**：PrescriptionAuditServiceImpl.java:147-195 的 submit 方法无 prescriptionId 级别锁，两个并发线程可同时通过 BLOCK 检查

#### T28: hasNewAlerts 逻辑在正常路径中是死代码

- **根因**：submit 方法第152-163行的执行路径——hasCriticalAlerts=true 时提前返回 BLOCK 不执行到 hasNewAlerts 检查；hasCriticalAlerts=false 时 snapshotCriticalAlerts 为 emptyList，currentAlerts 也必然为空（因 hasCriticalAlerts=false），hasNewAlerts 返回 false。第183-192行的 BLOCK 分支不可达

#### T30: DosageLimitRule.findBestMatch 返回 null 时静默回退

- **根因**：DosageLimitRule.java:36-38 findBestMatch 返回 null 时回退到 `standards.get(0)`，无校验

#### T31: AllergyCheckRule allergyHistory 自由文本 contains 匹配过于激进

- **根因**：AllergyCheckRule.java:54-58 的 contains 子串匹配导致 "No allergy to penicillin" 命中 penicillin 并返回 BLOCK

#### T32: DraftContextCleanupTask 迭代 keySet() 与 writeTimestamps 非原子 + writeTimestamps 永远空

- **根因**：DraftContextCleanupTask.java:38-45 遍历 draftContextStore.keySet() 与 writeTimestamps 之间非原子。更关键的是 recordWrite() 无调用方，writeTimestamps 永远为空，所有条目的 ts 均为 null，清理永远不执行

#### T35: AuditConverter + ai-api DTO 遗漏 weight 和 unit 字段映射

- **根因**：AuditConverter.java:66-75 的 toAiCheckItem 未映射 unit 字段（PrescriptionItem 有 unit 而 PrescriptionCheckItem 无 unit）；AuditConverter.java:77-92 的 toAiPatientInfo 未映射 weight 字段（prescription PatientInfo 有 weight 而 ai-api PatientInfo 无 weight）

#### T40: @Recover 方法缺陷

- **根因**：RegistrationEventListener.java:52-66 的 recover 方法：(1) 无 @Transactional（deadLetterEventRepository.save 无事务保护）；(2) 第60行 `e.getMessage()` 可能为 null，违反 DeadLetterEvent.failReason 的 NOT NULL 约束；(3) 第58行 JSON 兜底生成含 event.getSessionId() 的字符串，sessionId 为 null 时输出 "null"

### medical-record 模块

#### M02: 模板变更事件 invalidateAll 清除所有缓存而非指定 key

- **根因**：DatabaseTemplateConfigManager.java:105-108 的 `@EventListener` 调用 `templateCache.invalidateAll()`，未按 `TemplateConfigChangeEvent.getDepartmentCode()` 失效指定 key。其他科室缓存会被一并刷新
- **影响**：功能正确（无 bug），但缓存命中率下降。未携带 departmentCode 的事件可继续使用 invalidateAll

#### M11: ai-api MedicalRecordGenResponse.missingFields 与 partialContent 未被业务层消费

- **根因**：MedicalRecordConverter.java:38-45 已消费 missingFields（写入 MISSING_FIELDS 字段）和 partialContent（写入 PARTIAL_CONTENT 字段）。但如 T21 所述，这两个元数据字段不应混入 content_map

#### T17: callAiWithTimeout 异常语义混淆

- **根因**：MedicalRecordServiceImpl.java:145-159 的三种异常（TimeoutException/InterruptedException/ExecutionException）统一返回 `AiResultFactory.degraded("AI medical record generation timeout", "MR_GEN_AI_TIMEOUT", null)`，语义混淆——ExecutionException 不是超时

#### T47: MedicalRecord @PrePersist 未设置 updatedAt

- **根因**：MedicalRecord.java:133-136 的 @PrePersist 仅设置 createdAt，未设置 updatedAt，新增记录 updatedAt 为 null。@PreUpdate（第138-141行）仅在 UPDATE 时设置 updatedAt

#### T48: MedicalRecordServiceImpl.resolveVisitId 使用 ForkJoinPool.commonPool()

- **根因**：MedicalRecordServiceImpl.java:132 的 `CompletableFuture.supplyAsync()` 使用默认 ForkJoinPool.commonPool()，Web 请求高并发下线程耗尽风险

#### T50: DraftContextStoreImpl 缺少 compute/putIfAbsent 原子操作

- **根因**：DraftContextStoreImpl.java:11-26 仅有 get/put/remove/containsKey/keySet 五个方法，缺少 compute 和 createIfNotExists。DraftContextStore 接口（继承 SessionStore）也未定义这些方法。当前代码中 DedupTaskScheduler 使用 SuggestionStore（有 compute/createIfNotExists），不依赖 DraftContextStore。若未来 DraftContextStore 需要原子操作，则需补充

### application / 跨模块

#### A05: MockAiService 与设计契约部分不符

- **实际状态**：MockAiService.java:40-42 使用 `@Profile("mock")`（非 `@ConditionalOnProperty`），已有三种返回模式（STATIC/AI_UNAVAILABLE/TIMEOUT），MockAdminController.java 已实现动态策略切换。此条已部分修复——`@Profile` 与 OOD 要求的 `@ConditionalOnProperty` 不一致

#### A06: DegradationStrategy/DegradationContext 降级决策不生效

- **根因**：
  - **delegate 选择阶段**：FallbackAiService.java:66-80 的 selectDelegate 遍历 delegates 检查 shouldDegrade，NoOpDegradationStrategy 始终返回 false，降级筛选形同虚设。@ConditionalOnMissingBean 语义正确——自定义策略注册时 NoOp 自动退让
  - **结果后处理阶段**：FallbackAiService.java:290-301 的 applyStrategies 使用空 DegradationContext，且 NoOp 不触发降级标记，AI 调用失败的结果不会被自动降级
  - **DegradationContext/DegradationStrategy 设计**：DegradationContext 仅有 serviceName/operationName 两个属性（无请求级元数据如时间窗口、错误计数），DegradationStrategy 接口仅一个 shouldDegrade 方法，策略实现缺乏决策依据

#### A09: AuditConverter.toAuditResponse 在 aiData == null 时退化为 PASS + 空 alerts

- **根因**：AuditConverter.java:48-56 在 aiData == null 时返回 PASS + 空 alerts，应在调用方拦截（调用方 PrescriptionAuditServiceImpl.java:110-111 仅检查 `aiResult != null && aiResult.isSuccess()`，未检查 getData() 是否为 null）

#### E05: RegistrationEventListener.recover() 手工构造含 3 字段的 HashMap

- **根因**：RegistrationEventListener.java:55-58 的 recover 方法手工构造 eventPayload，缺 sessionId null 防护

---

## P2 — 可并行修复

### consultation 模块

#### C18: saveTriageRecord 中 catch JsonProcessingException 完全静默

- **根因**：TriageServiceImpl.java:251-253 的 catch 块有 `log.warn(...)` 日志输出，非完全静默。但 warn 级别可能不够，且未记录原始 JSON 数据用于排查

#### T44: TriageResponse 缺少 correctedChiefComplaint 字段

- **根因**：dto/TriageResponse.java:5-18 缺少 correctedChiefComplaint 字段。ai-api TriageResponse.java:16 已有此字段，但 consultation 模块的 TriageResponse 未透传。TriageConverter.toTriageResponse():107-109 仅将 AI 修正结果写回 session，未设置到 consultation TriageResponse 中
- **因果链**：AI 返回 correctedChiefComplaint → session 被设置 → TriageResponse 无此字段 → 前端无法感知修正。此问题与 C03/A04 correctedChiefComplaint 数据流群组相关，是数据流链路中"API 响应层"的断裂点

#### T45: TriageServiceImpl.selectDepartment 缺少自动生成 UUID v4 校验

- **根因**：RegistrationEventListener.java:45 的 event.getSessionId() 可能为 null，传入 selectDepartment 后在 findBySessionId 中可能导致 NPE

### prescription 模块

#### P09: PrescriptionItem.unit 字段为 OOD 未正式定义的扩展字段

- **根因**：PrescriptionItem.java:11 的 unit 字段需在 OOD §1.1a/§3.2/§4.6/§8.3 补充定义

#### P12: DrugInteractionRule 无 Phase 4 预留标注

- **根因**：DrugInteractionRule.java:1-15 缺少 @ConditionalOnProperty 标注

#### P15: 降级 BLOCK 路径 reasons 固定字符串

- **根因**：PrescriptionAuditController.java:36-48 和 PrescriptionAuditServiceImpl.java:144-152 的降级 BLOCK 路径 reasons 使用 `List.of("Prescription audit blocked")` 固定字符串（Controller line 41），未透传具体规则触发的告警原因
- **备注**：`AuditResponse.fromFallback` 字段已存在并通过 Jackson 序列化，"未序列化到 JSON 响应"一项不成立

#### A08: 降级路径 fallback 文案硬编码英文

- **根因**：TriageServiceImpl.java:171,177 使用中文（"AI 服务不可用，已切换至规则引擎降级"），MedicalRecordServiceImpl.java:151,154,157 使用英文。文案语言不统一

### application / 跨模块

#### A11: 个别业务处过度防御性检查

- **根因**：grep `aiResult.isSuccess() && aiResult.getData() != null` 在 backend 范围内仅 1 处匹配（`PrescriptionAuditServiceImpl.java:401`）。其他位置（如 `TriageServiceImpl.java:137`、`PrescriptionAuditServiceImpl.java:110,140,383`）仅 `aiResult != null && aiResult.isSuccess()`，未做额外的 getData() null 检查。AiResult.success() 已 `Objects.requireNonNull(data)`，data 非 null 已保证；aiResult 自身的 null 检查仍需保留（AI 异常时可能为 null）
- **说明**：原诊断"普遍"描述与实际不符，仅 1 处；其他位置不构成冗余

#### T24: ConcurrentHashMapStore 缺少 Spring 注解

- **根因**：ConcurrentHashMapStore.java:10 无 @Service/@Component 注解，不会被 Spring 自动扫描注册。DraftContextStoreImpl.java:8 有 @Service 注解

