# R2.1: Store 抽象层 + 并发安全 — 横切关注点深度审查

审查时间：2026-06-29 21:43

### 审查范围

- `common-module-api/store/SessionStore.java`
- `common-module-api/store/SuggestionStore.java`
- `common-module-api/store/DraftContextStore.java`
- `common-module-api/store/impl/ConcurrentHashMapStore.java`
- `common-module-api/store/impl/ConcurrentHashMapStoreTest.java`
- `consultation/dialogue/DialogueSessionManager.java`
- `consultation/dialogue/DialogueSession.java`
- `consultation/service/impl/TriageServiceImpl.java`
- `consultation/service/DeadLetterCompensationService.java`
- `prescription/service/assist/DedupTaskScheduler.java`
- `prescription/dto/assist/AiSuggestionResult.java`
- `prescription/context/PrescriptionDraftContext.java`
- `prescription/context/DosageAlert.java`
- `prescription/service/audit/impl/PrescriptionAuditServiceImpl.java`
- `prescription/service/assist/impl/PrescriptionAssistServiceImpl.java`

审查依据：`Docs/07_ood_phase2_C_3_DE.md` §1.1a / §1.3 / §2.1 / §3.1 / §3.4 / §6.1

### 发现

#### [严重] SuggestionStore.createIfNotExists 原子方法未实现

- **位置**：`SuggestionStore.java:7-8`
- **描述**：设计文档 §3.4 明确要求在 `SuggestionStore` 接口新增 `createIfNotExists(taskId, prescriptionId, supplier)` 原子方法，将"去重判定 + 条件创建"合并为存储层原子操作。当前接口仅有 `compute(key, remappingFunction)`，缺少三参 createIfNotExists。DedupTaskScheduler 在 compute lambda 内部同时操作两个 key（dedupKey 和 candidateTaskId），compute 仅保证 dedupKey 的单 key 原子性，candidateTaskId 的 put（`DedupTaskScheduler.java:39`）不在同一个 `ConcurrentHashMap` 锁范围内——虽然 `ConcurrentHashMap` 不会死锁，但双 key 操作破坏了对上层（特别是未来 RedisStore 实现）的原子性契约。
- **建议**：(a) 在 SuggestionStore 接口增加 `AiSuggestionResult createIfNotExists(String taskId, String prescriptionId, Supplier<AiSuggestionResult> supplier)`； (b) DedupTaskScheduler.schedule() 改为调用此方法，移除 compute 内的 put(candidateTaskId, …) 副作用； (c) ConcurrentHashMapStore 中通过嵌套 compute 实现双 key 原子写入； (d) 更新 ConcurrentHashMapStoreTest 覆盖。

#### [严重] AiSuggestionResult 与 PrescriptionDraftContext TTL 清理任务缺失

- **位置**：全局（无对应文件）
- **描述**：设计文档 §6.1 指定三处 TTL 清理任务：DialogueSession（TTL 30min，每 5 分钟扫描）、AiSuggestionResult（TTL 60min，每 5 分钟扫描）、PrescriptionDraftContext（TTL 60min，每 5 分钟扫描）。当前代码仅实现了两处 `@Scheduled`：`DialogueSessionManager.evictExpiredSessions()`（fixedRate=60000ms）和 `DeadLetterCompensationService.compensateDeadLetters()`（fixedRate=1800000ms）。AiSuggestionResult 和 PrescriptionDraftContext 的 TTL 清理任务完全缺失，导致过期条目在 ConcurrentHashMap 中无限累积，构成内存泄漏风险。
- **建议**：新增 `SuggestionCleanupTask` 和 `DraftContextCleanupTask` 两个 `@Component`，各自 `@Scheduled(fixedRate = 300000)` 遍历 `SuggestionStore` / `DraftContextStore` 的 keySet，检查 AiSuggestionResult.createTime / PrescriptionDraftContext 最后更新时间，清除超过 TTL 的条目。

#### [严重] DedupTaskScheduler.schedule() compute lambda 内嵌套 put 操作

- **位置**：`DedupTaskScheduler.java:39`
- **描述**：在 `suggestionStore.compute(dedupKey, …)` 的 remappingFunction 内部调用 `suggestionStore.put(candidateTaskId, newResult)`。compute 仅保证 `dedupKey` 的单 key 原子性，`candidateTaskId` 的 put 操作不同 key，脱离 compute 的锁保护。两个线程在并发路径下可能进入以下竞态：线程 A compute 创建新 task（to put candidateTaskId_A），线程 B 同时对另一个 pending dedupKey 做 compute（to put candidateTaskId_B），此时无跨 key 原子性保证。对于未来 RedisStore 实现，Lua 脚本内也无法表达这种"两个不同 key 的原子写入"。
- **建议**：createIfNotExists 方法应在一个原子操作中同时完成：(a) dedupKey 的状态检查与更新；(b) AiSuggestionResult 在 taskId 下的写入。当前实现将两个操作分离在两个存储操作中，原子性不足。

#### [严重] TriageServiceImpl 同 session 并发访问无串行化保护

- **位置**：`TriageServiceImpl.java:66-80`, `DialogueSession.java:29-107`
- **描述**：设计文档 §3.1 声明"DialogueSessionManager 承担并发控制——同 session 请求串行，不同 session 独立"。但 `DialogueSession` 是一个完全可变 POJO（所有 set 方法无 synchronized），`DialogueSessionManager.createSession()` 和 `restoreSession()` 均无锁保护。在 `TriageServiceImpl.triage()` 中，两个并发请求携带同一 sessionId 时，线程 A 执行 `restoreSession` → setChiefComplaint → setAdditionalResponses → setRoundCount 的序列中可能被线程 B 的 set 操作交错执行，导致会话状态不一致。
- **建议**：(a) DialogueSessionManager 对同 sessionId 加锁（如 `ConcurrentHashMap.compute(sessionId, …)` 包裹读写或使用 `Striped` 锁）；(b) 或将 DialogueSession 改为不可变，每次更新时整体替换。

#### [严重] DialogueSessionManager.createSession 使用 put 而非 putIfAbsent

- **位置**：`DialogueSessionManager.java:21-25`
- **描述**：`sessionStore.put(sessionId, session)` 是无条件覆盖写入。若两个线程同时为同一 sessionId 调用 createSession（在 restoreSession 返回 null 的竞态窗口内），后执行的 put 将覆盖前一个 session，导致前一个 session 对象内存泄露且该线程的后续操作引用的是一个已被替换的旧对象。
- **建议**：改用 `sessionStore.compute(key, (k, existing) -> existing != null ? existing : newSession)` 或 `putIfAbsent` + 检查返回值。

#### [一般] DialogueSessionManager.evictExpiredSessions 调度频率与设计文档不一致

- **位置**：`DialogueSessionManager.java:39`
- **描述**：设计文档 §6.1 指定会话 TTL 清理"每 5 分钟扫描一次"，但实际代码为 `@Scheduled(fixedRate = 60000)`（每 1 分钟）。虽然更频繁不引入功能问题，但与设计约定不一致。
- **建议**：改为 `fixedRate = 300000` 对齐设计。

#### [一般] 缺乏 isLatest 并发写校验（PrescriptionAuditServiceImpl）

- **位置**：`PrescriptionAuditServiceImpl.java:282-305`
- **描述**：`persistAuditRecord()` 中（line 310-314），读取 `findByPrescriptionIdAndIsLatestTrue` 后将所有已有记录设 isLatest=false，再插入新记录 isLatest=true。此"读旧→标记旧→写新"三段操作在独立事务内但未加锁。两个并发线程对同 prescriptionId 执行 audit 时，都可能读到同一批 isLatest=true 的记录，各自标记为 false 后写入各自的新记录且都标记为 true，导致同 prescriptionId 下出现两条 isLatest=true 的记录，违反业务约束。
- **建议**：在 `persistAuditRecord()` 开始时对同 prescriptionId 获取悲观锁（`@Lock(PESSIMISTIC_WRITE)`），或使用数据库层唯一约束（`WHERE isLatest = TRUE` 部分唯一索引）兜底。

#### [一般] PrescriptionDraftContext.updateCriticalAlerts 非原子

- **位置**：`PrescriptionDraftContext.java:34-41`
- **描述**：`updateCriticalAlerts` 使用 get-check-then-put/remove 模式。两线程并发写入同一 prescriptionId 时，后写入完全覆盖前一次写入。此处的"全量覆盖"语义虽为设计意图，但 check-then-act 窗口期可能导致：线程 A 写入 alerts_A，线程 B 线程 A 尚未完成 put 前检测到 alerts不空 执行 put alerts_B，然后线程 A 的 put 再覆盖为 alerts_A——B 的写入丢失。检查该方法的调用者（`PrescriptionAssistServiceImpl.assist()` line 143、`checkDose()` line 177），两处均为从下往上顺序执行后单次写入，生产者-消费者模式下不存在并发冲突，但 method-level 本身不防竞态。
- **建议**：内部用 `draftContextStore.compute(key, …)` 包裹，或在方法签名和注释中明确标注"全量覆盖，调用方确保非并发调用"。

#### [一般] DedupTaskScheduler.schedule 返回值类型不安全

- **位置**：`DedupTaskScheduler.java:43`
- **描述**：`compute` 返回 `Object` 后直接强制转换为 `AiSuggestionResult`。若存储层因某种原因 put 了非 AiSuggestionResult 类型的值，会抛出 `ClassCastException`。
- **建议**：在 `SuggestionStore` 中通过泛型约束（如 `SuggestionStore extends SessionStore<String, AiSuggestionResult>`）消除强制转换。

#### [一般] ConcurrentHashMapStore 类型擦除 — SuggestionStore 与 DraftContextStore 共享同一 Map

- **位置**：`ConcurrentHashMapStore.java:13`
- **描述**：`ConcurrentHashMapStore` 同时实现 `SuggestionStore` 和 `DraftContextStore`，所有数据存储在同一个 `ConcurrentHashMap<String, Object>` 中。虽然当前 key 空间不冲突（suggestion-dedup:XXX / taskId 与 prescriptionId:criticalAlerts），但类型系统层次无法区分两种数据在 Map 中的归属，未来对特定 Store 的遍历或清理会读到另一 Store 的数据。
- **建议**：考虑两个方案之一：(a) 声明 `ConcurrentHashMapStore` 为抽象基类，SuggestionHashMapStore 和 DraftContextConcurrentHashMapStore 分别继承并持有独立 Map；(b) 或通过 Composite key prefix 做逻辑隔离，并在清理时过滤。

#### [一般] DeadLetterCompensationService 未检查 maxRetryCount 和 EXPIRED 迁移

- **位置**：`DeadLetterCompensationService.java:29-44`
- **描述**：设计文档 §3.1 要求补偿任务扫描 `state=FAILED 且 retryCount < maxRetryCount` 的记录，重试耗尽后标记为 EXPIRED。当前代码调用 `findByCompensableEvents("FAILED")` 查询所有 FAILED 记录，不检查 retryCount vs maxRetryCount；捕获异常后仅递增 retryCount 写入，不检查 retryCount 是否已达 maxRetryCount，也不迁移状态到 EXPIRED。
- **建议**：(a) 在补偿函数中增加 `retryCount >= maxRetryCount` 检查并迁移为 EXPIRED； (b) 确认 `findByCompensableEvents` 的查询条件是否包含 `retryCount < maxRetryCount` 过滤。

#### [一般] TriageServiceImpl.triage() 缺少 @Transactional

- **位置**：`TriageServiceImpl.java:64-143`
- **描述**：设计文档 §3.1 规定"先写数据库再更新内存"策略——`saveTriageRecord`（line 134/141）在 `triageRecordRepository.save(record)` 之前更新内存 session。但整个 `triage()` 方法无 `@Transactional` 注解，每个 `save` 操作使用独立自动提交事务。若 `save` 成功后在后续操作（如 line 140 `session.setAiFailCount(0)`）抛出异常，TriageRecord 已持久化但内存未更新，不满足事务一致需求。
- **建议**：在 `triage()` 方法添加 `@Transactional` 并将 session 内存状态更新在 save 之后执行。

#### [轻微] PrescriptionDraftContext.getCriticalAlerts 未检查类型转换

- **位置**：`PrescriptionDraftContext.java:28-30`
- **描述**：`(List<DosageAlert>) value` 为 unchecked cast，若 DraftContextStore 中某 key 存入了非 `List<DosageAlert>` 类型，运行时抛出 ClassCastException。
- **建议**：添加 `instanceof` + 类型擦除后对每元素 `instanceof DosageAlert` 校验。

#### [轻微] TriageServiceImpl.findDoctorsForDepartments 未处理 DoctorFacade 异常

- **位置**：`TriageServiceImpl.java:169-179`
- **描述**：设计文档 §5.1 / §3.1 要求 DoctorFacade 调用超时或异常时捕获异常并记录 WARN 日志、将 doctors 置为空列表。当前 `findDoctorsForDepartments` 直接调用 `doctorFacade.findAvailableDoctorsByDepartment(dept.getDepartmentId())`（line 172），未捕获任何异常。若 DoctorFacade 抛出运行时异常（如超时或连接异常），异常将传播到 `triage()` 方法顶层的 try-catch 之外，导致请求返回 500 而非空 doctors 降级。
- **建议**：在 `findDoctorsForDepartments` 中为每个 `doctorFacade.findAvailableDoctorsByDepartment` 调用添加 try-catch，记录 WARN 日志后继续处理下一科室，最终返回当前已获取的所有医生。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 4 |
| 一般 | 7 |
| 轻微 | 2 |

### 总评

**Store 抽象层**总体上实现了设计文档的核心语义：三个 Store 接口（SessionStore / SuggestionStore / DraftContextStore）定义清晰，ConcurrentHashMapStore 正确委托 ConcurrentHashMap 的内置并发原语。然而存在以下结构性问题：

1. **接口蓝图不完整**：设计文档 §3.4 要求的 `SuggestionStore.createIfNotExists(taskId, prescriptionId, supplier)` 三参原子方法缺失，这是包E 去重策略的顶层契约。当前 DedupTaskScheduler 用 compute + 嵌套 put 绕过了这一缺失，带来了双 key 原子性失效风险。

2. **TTL 清理漏斗**：§6.1 规划的三个 TTL 清理任务只实现了 DialogueSession 的清理（且频率偏差），AiSuggestionResult 和 PrescriptionDraftContext 的清理完全缺失，构成内存泄漏隐患。这是最容易被发现的生产问题入口。

3. **DialogueSession 并发保护裸奔**：虽然 §3.1 明确声明"同 session 请求串行"，但实际没有任何锁或串行化机制——DialogueSession 是纯可变 POJO，DialogueSessionManager.createSession 使用无条件 put 而非 putIfAbsent。在非单线程前置节流场景下，同 session 并发可导致数据结构错乱。

4. **isLatest 写违反业务约束**：`PrescriptionAuditServiceImpl.persistAuditRecord()` 的读-改-写模式在并发下可产生多条 isLatest=true 的记录，直接违反 AuditRecord 的业务约束。

**并发安全方面的整体评价**：现有实现仅依赖 ConcurrentHashMap 的单 key 原子原语（compute/remove），在低并发场景下能正常运转。但面对设计文档要求的更严格语义（同 session 串行化、createIfNotExists 原子性、isLatest 唯一性），保护层不足。建议优先补全 createIfNotExists 接口和 TTL 清理任务，再加锁强化 isLatest 和 DialogueSession 的并发保护。
