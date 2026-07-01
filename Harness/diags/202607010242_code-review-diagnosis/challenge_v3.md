# 诊断质询报告（v3）

## 质询结果

LOCATED

## 逐维度审查

### 1. 证据充分性

**[通过]** C04: `@Transactional` 边界问题——TriageServiceImpl.java:82 的 triage 方法确实标注了 `@Transactional`，saveTriageRecord 使用 transactionTemplate，selectDepartment 标注 `@Transactional`。证据与代码一致。

**[通过]** C06: DoctorFacade 超时——TriageServiceImpl.java:213-236 的 findDoctorsForDepartments 无超时注入，try/catch 和 WARN 日志存在于第226-229行，application.yml 的 `consultation.doctor-facade.timeout: 2` 未被 TriageServiceImpl 读取。证据充分。

**[通过]** C20: createSession put vs putIfAbsent——DialogueSessionManager.java:33-44 的 createSession 使用 synchronized + containsKey + put。restoreSession (第50行) 无 synchronized，第68行 put 可能与 createSession 竞态。因果链合理。

**[通过]** E02: TriageRecord 唯一约束——findBySessionId 的 PESSIMISTIC_WRITE + MANDATORY 传播在 transactionTemplate 事务内执行，对已存在行有锁保护，对不存在的行锁行为依赖数据库。因果链修正准确，场景分析（场景 A/B）合理。

**[通过]** S04: restoreSession 缺 synchronized + MANDATORY 传播约束——restoreSession 无 synchronized，且 findTopBySessionIdOrderByTriageTimeDesc 的 MANDATORY 传播在非事务上下文调用会抛异常。约束条件补充准确。

**[通过]** A01: AiResultFactory 零引用——PrescriptionAuditServiceImpl.java:96-105 AI 失败时 aiResult 设为 null（非 AiResultFactory），PrescriptionAssistServiceImpl.java:89-99 同理。与代码一致。

**[通过]** P01: 异步 AI 调度——PrescriptionAssistServiceImpl.java:339-369 的 scheduleSuggestionAsync 使用 CompletableFuture.supplyAsync() 无线程池，返回值未消费。与代码一致。

**[通过]** P03/S02: TTL 清理失效——DraftContextCleanupTask.recordWrite() 无调用方（已确认代码中无调用者），writeTimestamps 永远空；SuggestionCleanupTask 中 scheduleSuggestionAsync 创建的 AiSuggestionResult 未设置 createTime，getTimestamp() 返回 null，NPE 不会被 ClassCastException catch 捕获。证据链完整。

**[通过]** S03: DedupTaskScheduler 跨 key 操作——DedupTaskScheduler.java:38-40 createIfNotExists 后第40行 put(candidateTaskId)，第51-59行 compute 操作 dedupKey，第62行 put 在 compute 外部。证据与代码一致。

**[通过]** T44: TriageResponse 缺少 correctedChiefComplaint 字段——consultation/dto/TriageResponse.java 确认无此字段，TriageConverter.toTriageResponse 仅将 AI 修正结果写回 session 而非 response。证据充分。

**[通过]** T42: Caffeine 缓存仅 refreshAfterWrite——DefaultTriageRuleEngine.java:30-37 确认仅 refreshAfterWrite(60s)，无事件驱动失效。证据充分。

**[通过]** M07: 非超时失败时 success 设置——MedicalRecordConverter.java:72-74 的逻辑 `(aiResult.isSuccess() && aiResult.getData() != null) || "MR_GEN_AI_TIMEOUT".equals(aiResult.getErrorCode())` 在 isSuccess()=false 且 errorCode≠TIMEOUT 时，success 正确为 false。但诊断称"success 仍为 true（因 aiResult.getData() != null 部分可能为真）"——此推断有误。当 isSuccess()=false 时，`aiResult.isSuccess() && aiResult.getData() != null` 整体为 false，不会因 getData()!=null 而为 true。然而，此推断错误不影响诊断结论方向（该行逻辑确实存在设计缺陷：TIMEOUT 场景下 success=true 违反语义），且诊断最终结论"非超时失败时仍可能设 success=true"在 TIMEOUT 路径下确实成立。标记为**轻微**。

**[问题-轻微]** M07: 诊断称"当 aiResult.isSuccess()=false 且 errorCode != 'MR_GEN_AI_TIMEOUT' 时，success 仍为 true（因 `aiResult.getData() != null` 部分可能为真）"——实际上当 isSuccess()=false 时，`aiResult.isSuccess() && ...` 短路求值为 false，不会因 getData()!=null 而为 true。但诊断最终关注的问题（TIMEOUT 路径下 success=true）确实存在，不影响结论方向。

**[通过]** T17: callAiWithTimeout 异常语义混淆——MedicalRecordServiceImpl.java:145-159 确认三种异常统一返回 `degraded("AI medical record generation timeout", "MR_GEN_AI_TIMEOUT", null)`。证据充分。

**[通过]** T47: MedicalRecord @PrePersist 未设置 updatedAt——MedicalRecord.java:133-136 确认 @PrePersist 仅设置 createdAt，@PreUpdate 设置 updatedAt。新增记录 updatedAt 为 null。证据充分。

**[通过]** T19: MockAiService TIMEOUT 策略——MockAiService.java:67 确认 `new CompletableFuture<>()` 永不完成。证据充分。

**[通过]** T20: FallbackAiService.applyStrategies 空 DegradationContext——FallbackAiService.java:290-301 确认 applyStrategies 中 `new DegradationContext()` 无 serviceName/operationName。证据充分。

**[通过]** A06: 降级决策不生效——FallbackAiService.java:66-80 selectDelegate 检查 shouldDegrade，NoOpDegradationStrategy 始终返回 false；applyStrategies 使用空 context。证据充分。

**[通过]** T40: @Recover 方法缺陷——RegistrationEventListener.java:52-66 确认无 @Transactional、e.getMessage() 可能为 null、JSON 兜底含 sessionId。证据充分。

**[通过]** A09: AuditConverter aiData==null 时退化为 PASS——AuditConverter.java:48-56 确认 aiData==null 时返回 PASS + 空 alerts。PrescriptionAuditServiceImpl.java:110-111 仅检查 `aiResult != null && aiResult.isSuccess()`，未检查 getData() 是否为 null。证据充分。

**[通过]** C16: conditions JSON 解析失败返回 true——DefaultTriageRuleEngine.java:82-116 确认 null/空返回 true，keywords 空/空数组返回 true，JSON 解析异常返回 true。证据充分。

**[通过]** T24: ConcurrentHashMapStore 缺少 Spring 注解——ConcurrentHashMapStore.java:10 无 @Service/@Component 注解。证据充分。

**[通过]** 已修复项批量验证——C01/C02/C05/C07/C08/C09/C14/C15/C19/C21/C22 等标记为已修复的条目，经代码验证均与实际状态一致。

### 2. 逻辑完整性

**[通过]** C04 因果链：triage 方法 @Transactional 包含 AI 调用 → AI 阻塞时 DB 连接被长时间占用 → 连接池耗尽。逻辑完整。

**[通过]** E02 因果链修正：findBySessionId 在 transactionTemplate 事务+悲观写锁保护下执行，但首次 INSERT（无已有记录）时两线程可能均走新建路径。场景 A/B 分析逻辑自洽。

**[通过]** S04 因果链 + MANDATORY 约束：restoreSession 从 triage 方法调用时，若 triage 移除 @Transactional，findTopBySessionIdOrderByTriageTimeDesc 的 MANDATORY 传播会抛异常。跨问题约束识别准确。

**[通过]** P03/S02 因果链：(a) recordWrite() 无调用方 → writeTimestamps 永远空 → 清理不执行；(b) scheduleSuggestionAsync 未设置 createTime → getTimestamp() 返回 null → NPE → ClassCastException catch 不捕获 → 清理方法异常终止。逻辑完整。

**[通过]** correctedChiefComplaint 数据流群组：AI → session → TriageRecord 持久化已打通，前端响应层断裂（T44）。群组分析逻辑自洽。

**[通过]** T28 因果链：hasCriticalAlerts=true 时提前返回 BLOCK，不执行到 hasNewAlerts；hasCriticalAlerts=false 时 snapshot 和 current 均为空，hasNewAlerts 返回 false。第183-192行 BLOCK 分支不可达。逻辑正确。

**[通过]** S03 因果链：createIfNotExists 与 put 不在同一原子操作中，compute 操作 dedupKey 但 put(candidateTaskId) 在 compute 外部。竞态存在。逻辑合理。

**[问题-轻微]** A01 中 PrescriptionAuditServiceImpl 的诊断——诊断称"AI 调用失败时 aiResult 被设为 null，而非使用 AiResultFactory 构造降级结果"。但 PrescriptionAuditServiceImpl.java:96-105 中 AI 失败后直接进入本地规则引擎降级路径（第112-130行），降级逻辑本身是完整的。不使用 AiResultFactory 是代码风格一致性问题，不影响功能正确性。不影响"未使用 AiResultFactory"这一事实判定，但因果链暗示此为功能缺陷略显牵强。

### 3. 覆盖完备性

**[通过]** 任务描述（todo.md）中的所有问题现象在诊断报告中均有对应条目或标记为已修复。

**[通过]** P0 级所有条目已覆盖：C01-C08, C15, C20-C21, E02, S04, A01, P01-P04, S03, A02-A03, M01, M04, C03/A04, A10。

**[通过]** P1 级所有条目已覆盖。

**[通过]** P2 级所有条目已覆盖。

**[通过]** 跨问题耦合约束群组已在诊断中体现：correctedChiefComplaint 数据流群组（C01/A04/C03/C19/C23/T44）、事务边界+并发控制（C04/E02/S04）、DoctorFacade+排序（C06/C17）。

**[通过]** 诊断对"已修复"与"未修复"的判定完整，v3 修订说明逐一回应了 v2 质询意见。

**[通过]** 诊断正确回答了"问题是什么"和"为什么发生"：每个未修复条目均提供了现象、根因和因果链。

**[问题-轻微]** 诊断未单独列出 C10（todo.md 标注为误报）和 S01（todo.md 标注为误报）。但 todo.md 已明确标注为误报并保留以核对，诊断省略这些条目是合理的。不影响覆盖完备性。

## 质询要点（CHALLENGED 时存在）

无严重/一般问题，不适用。
