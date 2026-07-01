# R2.2: consultation 模块对话管理与 correctedChiefComplaint 优先级深度审查

审查时间：2026-07-01

### 审查范围

- `service/impl/TriageServiceImpl.java`
- `converter/TriageConverter.java`
- `event/RegistrationEventListener.java`
- `dialogue/DialogueSessionManager.java`
- `dialogue/DialogueSession.java`
- `rule/DefaultTriageRuleEngine.java`
- `rule/TriageRuleEngine.java`
- `rule/MatchResult.java`
- `rule/entity/TriageRule.java`
- `dto/DialogueCreateRequest.java`
- `dto/TriageResponse.java` (consultation)
- `entity/TriageRecord.java`
- `entity/DeadLetterEvent.java`
- `service/DeadLetterCompensationService.java`
- `fallback/StaticDepartmentFallbackProvider.java`
- `config/SchedulingRetryConfig.java`
- `repository/TriageRecordRepository.java`
- `repository/TriageRuleRepository.java`
- `repository/DeadLetterEventRepository.java`
- `ai-api/dto/triage/TriageRequest.java`
- `ai-api/dto/triage/TriageResponse.java` (ai-api)
- `ai-api/AiResult.java`
- `ai-api/AiResultFactory.java`
- `common-module-api/event/RegistrationEvent.java`
- `common-module-api/store/SessionStore.java`

### 发现

#### [严重] correctedChiefComplaint 显式路径被后续请求隐式覆盖（T1 延续）

- **位置**：`TriageServiceImpl.java:110`
- **描述**：
  第 110 行 `session.setCorrectedChiefComplaint(request.getCorrectedChiefComplaint());` 每次 `triage()` 调用时无条件覆盖 session 中的 `correctedChiefComplaint`。当 AI 在第一轮返回了 `correctedChiefComplaint`（存入 session），而第二轮请求的 `DialogueCreateRequest.correctedChiefComplaint` 为 null（因为 consultation 层的 `TriageResponse` DTO 没有 `correctedChiefComplaint` 字段返回给前端），此时 `request.getCorrectedChiefComplaint()` 返回 null，第 110 行将 session 中已有的 AI 修正值覆盖为 null。
  **后果**：AI 的 correctedChiefComplaint 修正仅在单轮生效，跨轮对话中丢失。`TriageRecord` 持久化时（第 271 行 `record.setCorrectedChiefComplaint(session.getCorrectedChiefComplaint())`）存入 null。
- **建议**：
  将第 110 行改为条件赋值：
  ```java
  if (request.getCorrectedChiefComplaint() != null) {
      session.setCorrectedChiefComplaint(request.getCorrectedChiefComplaint());
  }
  ```
  同时在 `DialogueCreateRequest` → `consultation/dto/TriageResponse` 的返回链路中增加 `correctedChiefComplaint` 字段，使前端能感知修正结果。

#### [严重] 降级路径规则引擎使用原始 chiefComplaint 而非 correctedChiefComplaint（T1 延续）

- **位置**：`TriageServiceImpl.java:158`
- **描述**：
  降级路径中 `triageRuleEngine.match(request.getChiefComplaint(), ...)` 始终使用请求中的原始 `chiefComplaint` 进行规则匹配。如果多轮对话中 AI 已经返回过 `correctedChiefComplaint`（存储在 `session.getCorrectedChiefComplaint()`），后续轮次 AI 调用失败走降级时，规则引擎仍以原始主诉匹配，导致规则匹配结果与 AI 已修正的认知不一致。
- **建议**：
  使用 `session.getCorrectedChiefComplaint()` 作为降级匹配的输入（如非空）。建议改为：
  ```java
  String complaintForMatch = session.getCorrectedChiefComplaint() != null
          ? session.getCorrectedChiefComplaint()
          : request.getChiefComplaint();
  MatchResult matchResult = triageRuleEngine.match(complaintForMatch, ...);
  ```

#### [严重] DialogueSessionManager.restoreSession 缺少并发保护（T3 延续）

- **位置**：`DialogueSessionManager.java:50-71`
- **描述**：
  `createSession()`（第 33 行）声明为 `synchronized`，但 `restoreSession()`（第 50 行）没有同步。当两个线程同时为同一个 sessionId 调用 `restoreSession()`，且该 session 不在缓存但存在于 DB 时，两个线程都会执行 `triageRecordRepository.findTopBySessionIdOrderByTriageTimeDesc(sessionId)`，各自创建 `DialogueSession` 实例，并先后调用 `sessionStore.put(sessionId, session)`。后者覆盖前者，被覆盖的 session 对象失去引用但不会造成数据不一致（因为 DB 状态未变）。但如果在此同时 `triage()` 方法在使用其中一个 session 实例进行修改，则可能出现修改丢失。
- **建议**：
  为 `restoreSession()` 增加 `synchronized` 关键字，或使用更细粒度的锁机制（如 `ConcurrentHashMap.computeIfAbsent` 模式）。

#### [一般] TriageConverter.toTriageResponse 副作用与 TriageServiceImpl 重复赋值（T5 延续）

- **位置**：
  - `TriageConverter.java:107-109`
  - `TriageServiceImpl.java:148-150`
- **描述**：
  `TriageConverter.toTriageResponse()` 在第 107-109 行修改了入参 `session` 对象的 `correctedChiefComplaint`。这是 Converter 的副作用——通常 Converter/Mapper 应是无副作用的纯映射函数。同时，`TriageServiceImpl.triage()` 在第 148-150 行也执行了完全相同的操作（同样在 AI 成功路径中设置 session 的 `correctedChiefComplaint`）。两处重复代码，且 converter 中的副作用可能被调用者忽略。
- **建议**：
  移除 `TriageConverter.toTriageResponse()` 中对 session 的修改（第 107-109 行），由 `TriageServiceImpl` 统一管理 session 状态。Converter 只负责 DTO 映射。

#### [一般] DialogueSession 并发控制模式不一致（T6 延续）

- **位置**：`DialogueSession.java`
- **描述**：
  类中混用了两种并发控制策略：
  1. `synchronized` 方法：用于 `getSessionId()`、`getChiefComplaint()`、`setCorrectedChiefComplaint()` 等大部分字段的 getter/setter。
  2. `AtomicInteger`：用于 `aiFailCount`（第 15 行）和 `roundCount`（第 16 行）。
  这种混用意味着：一个线程通过 `synchronized setX()` 写字段 A 的同时，另一个线程可以通过 `AtomicInteger.get()` 读 `aiFailCount`，两者没有统一的锁边界。更严重的是，`getAiFailCount()`（第 65-67 行）和 `setAiFailCount()`（第 69-71 行）是 `AtomicInteger` 方法但**没有同步**，而 `getRoundCount()`/`setRoundCount()` 同理。其余字段的 getter/setter 却是 `synchronized`。这导致了整体状态的不一致风险。
- **建议**：
  统一使用 `synchronized` 或统一使用 `AtomicReference`/显式锁。推荐将所有字段改为普通字段并统一使用 `synchronized` 方法（保持简单）。

#### [一般] RegistrationEventListener 缺少 sessionId 空值防护

- **位置**：`RegistrationEventListener.java:45`
- **描述**：
  `handleRegistrationEvent` 直接调用 `triageRecordRepository.findBySessionId(event.getSessionId())`，未对 `event.getSessionId()` 进行空值检查。如果上游发布的 `RegistrationEvent.sessionId` 为 null，将导致 DB 查询异常（唯一约束字段传 null），并在重试 3 次后触发 `@Recover` 写入死信队列。死信补偿逻辑也以 `sessionId` 为关键字段，最终补偿调用 `triageService.selectDepartment(null, ...)` 也会失败。
- **建议**：
  在方法入口增加 null/空值校验：
  ```java
  if (event.getSessionId() == null || event.getSessionId().isBlank()) {
      log.warn("RegistrationEvent with null/blank sessionId, skipping");
      return;
  }
  ```

#### [一般] @Recover 方法缺陷：事务缺失 + 异常消息可能为 null + sessionId 回退 JSON 问题

- **位置**：`RegistrationEventListener.java:52-66`
- **描述**：
  1. `@Recover` 方法没有 `@Transactional`（第 52 行），但内部调用 `deadLetterEventRepository.save(deadLetter)`。虽然 JPA repository 默认有事务，但若 `save` 过程中抛出 `DataAccessException`，整个补偿操作无事务回滚能力。
  2. 第 60 行 `e.getMessage()` 可能为 null（某些异常不携带 message），导致 `failReason` 存储为 null，违反 `@Column(length = 500, nullable = false)` 约束，save 时会抛出 `DataAccessException`。
  3. 第 58 行 JSON 序列化失败的兜底分支中 `event.getSessionId()` 为 null 时，生成 `{"sessionId":"null"}` 字符串，死信补偿时从 payload 解析到 `"null"` 字符串而非 null，导致 `selectDepartment("null", ...)` 调用失败。
- **建议**：
  1. 为 `@Recover` 方法增加 `@Transactional`。
  2. 使用 `String.valueOf(e.getMessage())` 或 `e.getClass().getName()` 作为 failReason 的 fallback。
  3. JSON 兜底分支中使用 `Objects.toString(event.getSessionId(), "unknown")` 或更安全的字符串拼接。

#### [一般] DefaultTriageRuleEngine JSON 解析静默失败

- **位置**：`DefaultTriageRuleEngine.java:113-115`
- **描述**：
  当 `conditions` JSON 解析失败时，`matchesConditions` 返回 `true`，该规则无条件匹配所有主诉。这意味着数据库中存在格式错误的规则 JSON 时，该规则会对所有请求生效，可能造成科室推荐错误。该静默失败没有日志记录，难以排查。
- **建议**：
  catch 块中增加 `log.warn()` 记录 ruleId 和解析失败的 JSON 内容，并返回 `false` 而非 `true`：
  ```java
  } catch (JsonProcessingException e) {
      log.warn("Failed to parse conditions JSON for rule, skipping: {}", conditionsJson, e);
      return false;
  }
  ```

#### [一般] DefaultTriageRuleEngine 缓存仅依赖 `refreshAfterWrite` 无主动刷新机制

- **位置**：`DefaultTriageRuleEngine.java:30-37`
- **描述**：
  Caffeine 缓存使用 `refreshAfterWrite(60, TimeUnit.SECONDS)`，但该策略仅在**查询触发**时检查是否需要刷新。如果规则在数据库中发生变更（增/删/改），但 60 秒内没有 `match()` 调用，缓存不会被刷新。没有提供外部触发的刷新方法（如 `@PostConstruct` 初始化加载 + 管理端点/事件监听刷新）。
- **建议**：
  增加 `@PostConstruct` 确保启动时加载，同时提供 `refreshCache()` 方法供管理端点使用。考虑在 `TriageRuleRepository` 写入操作后通过事件机制触发缓存失效。

#### [一般] saveTriageRecord 中 aiFailCount 重置时序与持久化无关但值得注意

- **位置**：`TriageServiceImpl.java:185`
- **描述**：
  AI 成功路径中，`session.setAiFailCount(0)`（第 185 行）在 `saveTriageRecord`（第 186 行）之前执行。当前代码中 `saveTriageRecord` 不读取 `aiFailCount`，因此无实际影响。但如果后续维护者在 `saveTriageRecord` 中增加 `aiFailCount` 持久化，此处的时序会导致错误的计数被保存（始终为 0）。
- **建议**：
  将 `session.setAiFailCount(0)` 移到 `saveTriageRecord` 调用之后，保持"先保存、后重置"的语义。

#### [轻微] consultation/dto/TriageResponse 缺少 correctedChiefComplaint 字段

- **位置**：`consultation/dto/TriageResponse.java`
- **描述**：
  AI 返回的 `correctedChiefComplaint` 经过 `TriageConverter.toTriageResponse()` 映射后只存入 session，但没有映射到 consultation 层的 `TriageResponse` DTO 中返回给前端。前端无法感知 AI 对主诉的修正内容。
- **建议**：
  在 `consultation/dto/TriageResponse` 中增加 `correctedChiefComplaint` 字段，并在 `toTriageResponse()` 中映射。

#### [轻微] DefaultTriageRuleEngine 内 `public static final ObjectMapper` 非 Spring 管理

- **位置**：`DefaultTriageRuleEngine.java:23`
- **描述**：
  使用 `private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();` 而非注入 Spring 容器中的 `ObjectMapper` bean。可能导致与全局 Jackson 配置（如日期格式、模块注册）不一致。
- **建议**：
  通过构造函数注入 Spring 的 `ObjectMapper` bean。

#### [轻微] DeadLetterCompensationService 中 retryCount 边界检查冗余

- **位置**：`DeadLetterCompensationService.java:31-34`
- **描述**：
  `findByCompensableEvents("FAILED")` 的 JPQL 已包含 `e.retryCount < e.maxRetryCount` 条件，因此查询结果集中的所有事件都已满足 retryCount < maxRetryCount。第 31 行的 `if (event.getRetryCount() >= event.getMaxRetryCount())` 是安全冗余检查。
- **建议**：
  可保留作为防御性编程（无必要修改）。

#### [轻微] TriageServiceImpl.handleAiFailure 对 aiFailCount 递增两次的风险

- **位置**：`TriageServiceImpl.java:154-156, 205-211`
- **描述**：
  `handleAiFailure()` 在第 206 行递增了 `aiFailCount`，并返回 `degraded=true` 的 `AiResult`。调用方 `triage()` 在第 154 行检查 `!aiResult.isDegraded()` 后跳过递增，因此不会重复计数。但该逻辑依赖 `isDegraded()` 和 `handleAiFailure` 返回结果的耦合：如果未来有人修改 `handleAiFailure` 返回 `degraded=false`，则会在 `triage()` 中二次递增。
- **建议**：
  简化设计：`handleAiFailure` 只负责递增计数，返回 boolean 或 void，由 `triage()` 统一构造 degraded result。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 3 |
| 一般 | 7 |
| 轻微 | 4 |

### 总评

本轮深度审查对 consultation 模块的 `correctedChiefComplaint` 数据流、对话管理并发控制、事件消费链路和规则引擎机制进行了逐行分析。发现了 T1 优先级反转的两个具体表现（第 110 行的 null 覆盖和第 158 行的原始主诉使用），均为严重级别的数据正确性问题。T3 的 `restoreSession` 缺少 `synchronized` 在并发场景下可能造成 session 状态丢失。T5 和 T6 的设计不一致性问题在代码中清晰可辨。`RegistrationEventListener` 在防御性编程方面存在多个薄弱点。`DefaultTriageRuleEngine` 的静默失效和缓存刷新机制需要补充运维手段。建议优先修复 3 个严重问题，其余一般/轻微问题在后续迭代中逐步治理。
