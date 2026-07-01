# R1.1: 包C 智能分诊（consultation 模块）完整实现审查

审查时间：2026-06-29

### 审查范围

**主依据**：`Docs/07_ood_phase2_C_3_DE.md` §3.1 包C 智能分诊、§6.1 定时任务集中管理、§10 错误码、§1.3 核心抽象

**审查文件（src/main/java）**：
- `AIMedical/backend/modules/consultation/.../api/TriageController.java`
- `AIMedical/backend/modules/consultation/.../converter/TriageConverter.java`
- `AIMedical/backend/modules/consultation/.../dialogue/DialogueSession.java`
- `AIMedical/backend/modules/consultation/.../dialogue/DialogueSessionManager.java`
- `AIMedical/backend/modules/consultation/.../dto/*.java`（DialogueCreateRequest / TriageResponse / RecommendedDepartment / RecommendedDoctor / MatchedRule / AdditionalResponse）
- `AIMedical/backend/modules/consultation/.../entity/TriageRecord.java`
- `AIMedical/backend/modules/consultation/.../entity/DeadLetterEvent.java`
- `AIMedical/backend/modules/consultation/.../event/RegistrationEventListener.java`
- `AIMedical/backend/modules/consultation/.../fallback/DepartmentFallbackProvider.java`
- `AIMedical/backend/modules/consultation/.../fallback/StaticDepartmentFallbackProvider.java`
- `AIMedical/backend/modules/consultation/.../repository/TriageRecordRepository.java`
- `AIMedical/backend/modules/consultation/.../repository/TriageRuleRepository.java`
- `AIMedical/backend/modules/consultation/.../repository/DeadLetterEventRepository.java`
- `AIMedical/backend/modules/consultation/.../rule/TriageRuleEngine.java`
- `AIMedical/backend/modules/consultation/.../rule/DefaultTriageRuleEngine.java`
- `AIMedical/backend/modules/consultation/.../rule/entity/TriageRule.java`
- `AIMedical/backend/modules/consultation/.../service/TriageService.java`
- `AIMedical/backend/modules/consultation/.../service/impl/TriageServiceImpl.java`
- `AIMedical/backend/modules/consultation/.../service/DeadLetterCompensationService.java`
- `AIMedical/backend/modules/consultation/.../config/SchedulingRetryConfig.java`

**对照文档**：`Docs/07_ood_phase2_C_3_DE.md` 行 425–535、§6.1、§10、§1.3 表格

### 发现

#### [严重] TriageRecord 实体缺失 correctedChiefComplaint 快照字段

- **位置**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/entity/TriageRecord.java:14-39`
- **描述**：设计文档 §3.1 明确要求 "TriageRecord 实体增加 chiefComplaint 原始主诉快照字段（已有）和 correctedChiefComplaint 快照字段（新增，String 可选）"（行 462）。当前实体仅包含 `chiefComplaint`、`aiRecommendedDepartments`、`recommendedDoctors`、`ruleMatchedDepartments`、`finalDepartmentId/Name`、`confidence`、`degraded`、`ruleVersion`、`ruleSetId`、`triageTime`，**没有 `correctedChiefComplaint` 字段**。这导致设计文档提出的进程重启后从最近 TriageRecord 恢复 correctedChiefComplaint 写入新 DialogueSession 的补充措施 (a) 无法实现，进而影响 §3.1 行 458 主诉修正信息丢失后的可恢复性评估失效。
- **建议**：在 `TriageRecord` 实体上增加 `private String correctedChiefComplaint;` 字段，对应 `triage_record.corrected_chief_complaint TEXT NULL` 列；同时 `TriageServiceImpl.saveTriageRecord` 中从 `session.getCorrectedChiefComplaint()` 读取写入。

#### [严重] 缺少 `findTopBySessionIdOrderByTriageTimeDesc` 查询方法

- **位置**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/repository/TriageRecordRepository.java:13-17`
- **描述**：设计文档 §3.1 行 462 明确要求 "补充措施：新增 `findTopBySessionIdOrderByTriageTimeDesc(String sessionId)` 查询方法用于恢复"。当前 Repository 仅包含 `findBySessionId`、`findTopByPatientIdOrderByTriageTimeDesc`、`findBySessionIdIn`，**没有按 sessionId 找最近记录的恢复方法**。结合上一条问题，进程重启后 correctedChiefComplaint 的恢复链路完全断裂。
- **建议**：在 `TriageRecordRepository` 中新增 `Optional<TriageRecord> findTopBySessionIdOrderByTriageTimeDesc(String sessionId)`；在 TriageServiceImpl 会话重建路径中调用以恢复 `correctedChiefComplaint`。

#### [严重] AI 隐式 correctedChiefComplaint 路径未实现

- **位置**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/service/impl/TriageServiceImpl.java:64-143`、`converter/TriageConverter.java:54-90`
- **描述**：设计文档 §3.1 行 443 明确要求支持两条主诉修正路径——(a) 显式路径已部分实现（行 73 `session.setCorrectedChiefComplaint(request.getCorrectedChiefComplaint())`），(b) **隐式路径未实现**：AI 返回的 `TriageResponse.correctedChiefComplaint`（ai-api 层 `TriageResponse.java:93-99` 已定义该字段）从未被 `TriageConverter.toTriageResponse` 取出（行 54-90），更未被 `TriageServiceImpl` 在 AI 返回后写入 `DialogueSession.correctedChiefComplaint`。测试 `shouldConvertToTriageResponseWithAiData` 也未覆盖该路径。
- **建议**：
  1. 在 `TriageServiceImpl.triage` AI 成功路径中追加：当 `request.getCorrectedChiefComplaint() == null` 且 `aiData.getCorrectedChiefComplaint() != null` 时，调用 `session.setCorrectedChiefComplaint(aiData.getCorrectedChiefComplaint())`；
  2. `TriageConverter` 暴露 `extractCorrectedChiefComplaint(aiData)` 或在 `toTriageResponse` 中返回中间结果。

#### [严重] TriageServiceImpl 缺少 `@Transactional` 注解

- **位置**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/service/impl/TriageServiceImpl.java:64`、`TriageServiceImpl.java:145`、`TriageServiceImpl.java:181`
- **描述**：设计文档 §3.1 行 453 明确指出 "在 @Transactional 事务内……事务提交成功后，再更新 DialogueSession"（即"先写数据库再更新内存"策略）。当前 `triage()`、`selectDepartment()`、`saveTriageRecord()` 三个方法均**未标注 `@Transactional`**。在没有事务边界的情况下，`saveTriageRecord` 中的 `triageRecordRepository.save(record)` 不会在事务中提交，进程崩溃或异常发生时可能造成：
  1. unique 约束冲突（如同一 sessionId 第二次分诊）直接抛异常而非回滚后重试；
  2. 与 §3.1 行 453 的"先写数据库再更新内存"事务一致性策略无法对齐；
  3. RegistrationEventListener 的事件消费也未启用独立事务（`@EventListener` 默认无事务），与设计文档 §3.1 行 349 "消费端独立事务" 描述不符。
- **建议**：
  1. `TriageServiceImpl` 上标注 `@Transactional`（类级默认）或 `triage()`、`selectDepartment()` 方法级标注；
  2. `RegistrationEventListener.handleRegistrationEvent` 上加 `@Transactional(propagation = REQUIRES_NEW)` 以满足"消费端独立事务"要求。

#### [严重] chiefComplaint 与 additionalResponses 互斥校验缺失（TRIAGE_FIELD_COMBINATION_INVALID）

- **位置**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/api/TriageController.java:24`、`TriageServiceImpl.java:64`
- **描述**：设计文档 §3.1 行 531 与 §3.4.1（行 844）明确要求 "首轮请求仅传 chiefComplaint（必填），additionalResponses 不传；后续多轮追问仅传 additionalResponses + sessionId……当检测到违规（同时提供二者或均未提供）时，返回 HTTP 400 + 错误码 `TRIAGE_FIELD_COMBINATION_INVALID`"。当前实现：
  - `DialogueCreateRequest.chiefComplaint` 仅 `@NotBlank @Size(min=5,max=500)`，**没有针对 `additionalResponses` 的互斥校验**；
  - `TriageController.consult` 没有检测该违规组合；
  - `TriageServiceImpl.triage` 在两种状态都允许继续处理——`(chiefComplaint="" && additionalResponses=null)`、`(chiefComplaint="..." && additionalResponses=[...])` 均直接进入 AI 调用路径。
- **建议**：在 `TriageServiceImpl.triage` 入口或 `TriageController.consult` 增加校验：
  - 同时提供 chiefComplaint 与 additionalResponses → 抛 `BusinessException(TRIAGE_FIELD_COMBINATION_INVALID)`；
  - 二者均未提供 → 同样抛 `TRIAGE_FIELD_COMBINATION_INVALID`。
  建议同时在 `GlobalErrorCode` 中新增 `TRIAGE_FIELD_COMBINATION_INVALID`（§10 行 1454 列出的非 AI 错误码）。

#### [严重] DoctorFacade 跨模块调用缺少超时降级保护

- **位置**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/service/impl/TriageServiceImpl.java:169-179`
- **描述**：设计文档 §3.1 行 517 明确要求 "调用超时或 DoctorFacade 抛出异常时，TriageServiceImpl 捕获异常并将 TriageResponse.doctors 置为空列表（不阻断分诊主流程），在响应中记录日志（WARN 级别，含调用耗时、异常类型和科室 ID）"。当前 `findDoctorsForDepartments` 方法（行 169-179）**直接同步调用 `doctorFacade.findAvailableDoctorsByDepartment`**，没有 try/catch、没有超时配置、没有日志记录。任意 DoctorFacade 实现抛出异常（连接失败、序列化错误、空指针）都会直接抛出至 controller 层并触发 500 错误，破坏"不影响分诊科室推荐的正常返回"的降级承诺。
- **建议**：用 try/catch 包裹 `doctorFacade.findAvailableDoctorsByDepartment` 调用，捕获异常后记录 `log.warn("DoctorFacade unavailable: deptId={}, latency={}ms, ex={}", ...)` 并跳过该科室；同时按设计 §3.1 行 1486 配置 `consultation.doctor-facade.timeout=2s`，可在调用层用 `CompletableFuture.supplyAsync(...).orTimeout(2, SECONDS)` 实现，或在 DoctorFacade 实现侧统一设置 Feign/RestTemplate 超时。

#### [严重] AI 调用缺少超时配置，可能无限阻塞请求线程

- **位置**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/service/impl/TriageServiceImpl.java:82-93`
- **描述**：当前 `aiService.triage()` 返回 `CompletableFuture` 后直接 `future.get()` 无超时阻塞。设计文档 §2.3 强调 AiService 各方法有超时配置；§3.1 行 437 提及 "AI 调用失败（含超时和不可用）"。当前实现将"超时"与"不可用"都归入"异常路径"，但**没有任何超时上限**，当 AI 实现侧 hang 住时，请求线程无限等待，Tomcat 工作线程池被耗尽。
- **建议**：`aiResult = future.get(aiCallTimeoutSeconds, TimeUnit.SECONDS)`，超时配置 `consultation.ai.triage.timeout`（默认值 5-10s），`TimeoutException` 进入 `handleAiFailure` 路径。

#### [一般] selectDepartment 方法签名偏离设计文档

- **位置**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/service/TriageService.java:10`、`TriageServiceImpl.java:145-159`、`TriageController.java:30-36`、`RegistrationEventListener.java:37-45`
- **描述**：设计文档 §3.1 行 431 / 行 467 明确要求接口签名 `selectDepartment(String sessionId, String departmentId, String departmentName)`（三参数），由 TriageController 和 RegistrationEventListener 共同调用。当前实现：
  1. 接口扩展为四参 `selectDepartment(String sessionId, String departmentId, String departmentName, boolean overwrite)`；
  2. `RegistrationEventListener.handleRegistrationEvent` **未调用** `triageService.selectDepartment`，而是直接通过 repository 修改 `finalDepartmentId/Name`——这与设计文档 §3.1 行 467 "RegistrationEventListener 通过 sessionId 关联找到 TriageRecord 后也调用此方法完成写入，避免重复逻辑" 的设计意图背离。
  4. "覆盖优先级强制执行语义"（行 469）"RegistrationEventListener 在调用 selectDepartment 前先检查 TriageRecord.finalDepartmentId 是否为空" 在当前实现中变成了"事件 listener 直接修改时检查 finalDepartmentId 是否为空"，语义保留但抽象层次下移，且与设计文档"调用 selectDepartment"的协作模式不一致。
- **建议**：保持三参设计，将"是否覆盖"语义提取为 `selectDepartmentIfAbsent(sessionId, deptId, deptName)` 命名（覆盖语义留给 controller 端显式覆盖）。`RegistrationEventListener.handleRegistrationEvent` 改为调用 `triageService.selectDepartment(sessionId, deptId, deptName)`，由 service 统一管理 finalDepartmentId 写入与不覆盖逻辑；当前 `TriageServiceImpl.selectDepartment` 的 `overwrite` 分支（行 152-156）也仅在 listener 路径使用，但 listener 实际未走此方法。

#### [一般] selectDepartment 找不到 TriageRecord 时未使用 TRIAGE_SESSION_NOT_FOUND 错误码

- **位置**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/service/impl/TriageServiceImpl.java:148-150`
- **描述**：设计文档 §3.1 行 431 与行 888 明确要求 "记录不存在时返回 TRIAGE_SESSION_NOT_FOUND"。当前抛 `BusinessException(GlobalErrorCode.NOT_FOUND, ...)`，复用通用 NOT_FOUND 错误码而非业务级错误码。§10 行 1454 错误码清单中明确列出 `TRIAGE_SESSION_NOT_FOUND` 作为分诊模块专有错误码。
- **建议**：在 `GlobalErrorCode` 中新增 `TRIAGE_SESSION_NOT_FOUND("TRIAGE_SESSION_NOT_FOUND", "分诊会话不存在")`，`TriageServiceImpl.selectDepartment` 改为抛 `new BusinessException(GlobalErrorCode.TRIAGE_SESSION_NOT_FOUND, ...)`。

#### [一般] DialogueSessionManager 未校验 sessionId 的 UUID v4 格式

- **位置**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/dialogue/DialogueSessionManager.java:21-25`、`DialogueSessionManager.java:31-37`
- **描述**：设计文档 §3.1 行 485 要求 "接受前端传入的 UUID v4 格式（36 字符，含连字符），验证格式有效性后通过 findOrCreate() 完成创建或恢复"。当前 `createSession` 与 `restoreSession` 直接接受任意字符串 sessionId，无格式校验。攻击者/异常前端可传入任意字符串污染内存 key 空间，并绕过 §3.4.1 输入契约。
- **建议**：`createSession`/`restoreSession` 入口加 `if (!SESSION_ID_PATTERN.matcher(sessionId).matches())` 校验（正则 `^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$`），校验失败抛 `BusinessException(TRIAGE_SESSION_INVALID, ...)`。

#### [一般] DialogueSessionManager TTL 清理周期与设计不符

- **位置**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/dialogue/DialogueSessionManager.java:39`
- **描述**：设计文档 §6.1 行 487 + 行 1496 要求 "统一 Spring @Scheduled 任务每 5 分钟扫描清理过期会话"。当前实现 `@Scheduled(fixedRate = 60000)` 即每 1 分钟扫描。频次不一致：高频扫描在 ConcurrentHashMap 上每次遍历全量 keySet（行 42 `for (String key : new ArrayList<>(sessionStore.keySet()))`），会话量较大时增加不必要的 CPU 与 GC 压力。
- **建议**：改为 `@Scheduled(fixedRate = 300000)` 或与设计文档 §6.1 行 1496-1500 的集中调度对齐——提取独立的 `DialogueSessionCleanupTask` bean，由统一 TaskScheduler 管理。

#### [一般] AI 调用上下文未按模板拼接全量历史，未做截断

- **位置**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/converter/TriageConverter.java:22-52`、`TriageServiceImpl.java:64-83`
- **描述**：设计文档 §3.1 行 441 要求 "将主诉+历次 QA 按模板格式拼合为完整推理 prompt"，行 445 要求超 3000 字符时触发截断并插入 `[NOTE: 部分对话内容因长度限制已省略]` 标记。当前 `TriageConverter.toAiTriageRequest`（行 22-52）：
  1. 仅原样设置 `request.getChiefComplaint()`，**未做模板拼合**——AI 仅收到首轮或当轮的原始主诉，无法看到历次 QA 的完整对话脉络；
  2. **未实现 correctedChiefComplaint 替换原始 chiefComplaint**（设计行 441/477 要求）；
  3. **未实现 3000 字符阈值截断与截断标记插入**；
  4. `additionalResponses` 同时包含 session 历史与 request 新增（行 30-48）——这样重复加了历史 QA 与当前轮 QA 两次进入 AI 上下文，与设计文档"全量拼接"虽不冲突但易出现重复。
- **建议**：
  1. 增加 `assembleChiefComplaintWithHistory(session, request)` 方法：当 `session.getCorrectedChiefComplaint() != null` 时使用修正值，否则使用原始主诉；累加字符串总长，超过 3000 字符时按"保留首轮+最近 N 轮"截断，并在中间插入 `[NOTE: 部分对话内容因长度限制已省略]` 标记；
  2. 阈值可通过 `@Value("${triage.max-context-chars:3000}")` 注入（设计 §3.1 行 1485 已声明此配置项）；
  3. 在 TriageServiceImpl.triage 增加调用点。

#### [一般] TriageRuleEngine 规则快照失效降级 + ruleVersionMismatch 标记缺失

- **位置**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/rule/DefaultTriageRuleEngine.java:36-56`
- **描述**：设计文档 §3.1 行 497 明确要求 "当 match() 使用快照版本（ruleVersion + ruleSetId）查询无结果时……降级使用当前最新版本规则集重新匹配，并在 TriageResponse 中标记 ruleVersionMismatch=true（可选）"。当前 `match` 方法（行 36-56）：
  1. 用 `ruleVersion`/`ruleSetId` 过滤后**直接返回结果**（无结果时返回空列表）；
  2. **无快照失效回退到 currentRuleVersion()/currentRuleSetId() 的逻辑**；
  3. `match` 方法返回 `List<RecommendedDepartment>` 无法携带 `ruleVersionMismatch` 标记——签名层面就缺少该输出。
- **建议**：
  1. 扩展返回类型为 `MatchResult(List<RecommendedDepartment> departments, boolean ruleVersionMismatch)` 或新增 `MatchOutcome` 包装类；
  2. `match` 内实现：当过滤结果为空时，去掉 version/setId 过滤再匹配一次，标记 `ruleVersionMismatch=true`；
  3. `TriageServiceImpl` 在降级路径中将 `ruleVersionMismatch` 写入 `TriageResponse`。

#### [一般] DeadLetterCompensationService 缺少 EXPIRED 状态迁移

- **位置**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/service/DeadLetterCompensationService.java:42-46`
- **描述**：设计文档 §3.1 行 337 明确要求 "状态迁移规则：FAILED（初始）→（补偿成功）→COMPENSATED；FAILED →（重试耗尽/超出最大重试次数）→EXPIRED"。当前 catch 分支（行 42-45）仅 `event.setRetryCount(event.getRetryCount() + 1)` 然后 save，**未检查 `retryCount >= maxRetryCount` 并迁移到 EXPIRED 状态**。补偿任务会在 `findByCompensableEvents` 查询（基于 `retryCount < maxRetryCount` 条件）时跳过该事件，但 DeadLetterEvent 记录的 state 字段仍停留在 FAILED，与设计意图不符，运维查询时无法直观区分"重试中"与"永久失败"。
- **建议**：`catch` 分支中追加 `if (event.getRetryCount() + 1 >= event.getMaxRetryCount()) { event.setState("EXPIRED"); }`，然后 save；测试用例 `shouldIncrementRetryCountOnFailure`（`DeadLetterCompensationServiceTest:65`）也应增加 EXPIRED 迁移分支的断言。

#### [一般] RegistrationEventListener @Retryable 缺少不可治愈异常排除

- **位置**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/consultation/event/RegistrationEventListener.java:36`
- **描述**：设计文档 §3.1 行 323 要求 "对 IllegalArgumentException、NullPointerException 等不可治愈异常（不可重试类型）直接进入 @Recover 回调，不浪费重试资源。不可治愈异常列表通过 @Retryable 的 excludes 或 noRetryFor 属性配置"。当前 `@Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))` 对所有 Exception 都触发重试，业务参数错误（如 sessionId 为空）也会重试 3 次延迟 6 秒后才进入死信路径。
- **建议**：将 `retryFor` 收窄为 `DataAccessException.class, TimeoutException.class`（或使用 `noRetryFor = {IllegalArgumentException.class, NullPointerException.class}`）；同时 `backoff` 改为 `@Backoff(delay = 2000, multiplier = 2.0)` 以实现 exponential backoff，对齐设计文档行 323 "重试间隔 2s，最多 3 次，exponential backoff"。

#### [一般] RegistrationEventListener 未委托 TriageService.selectDepartment

- **位置**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/event/RegistrationEventListener.java:35-45`
- **描述**：与"selectDepartment 方法签名偏离设计"问题相关——设计文档 §3.1 行 467 要求 `RegistrationEventListener 共同调用 TriageService.selectDepartment 避免重复逻辑`。当前 listener 直接通过 `triageRecordRepository.findBySessionId(...).ifPresent(record -> { record.setFinalDepartmentId(...); triageRecordRepository.save(record); })` 完成写入，与 `TriageServiceImpl.selectDepartment` 中的"overwrite=false 仅当 finalDepartmentId 为空时才写入"语义等价，但抽象层次下沉且重复实现。
- **建议**：注入 `TriageService` 并改为 `triageService.selectDepartment(event.getSessionId(), event.getDepartmentId(), event.getDepartmentName())`，TriageService 内部判断 `finalDepartmentId == null` 才写入。

#### [一般] findDoctorsForDepartments 未按匹配评分取前 5 名

- **位置**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/service/impl/TriageServiceImpl.java:169-179`
- **描述**：设计文档 §3.1 行 447 要求 "按匹配评分排序取前 5 名，填充 availableSlotCount 和 score 字段"。当前实现遍历每个推荐科室，将该科室下 `findAvailableDoctorsByDepartment` 返回的所有医生**全部添加**，未排序、未截断。如果某科室有 20 名医生，响应中将返回 20 个医生（×3 科室 = 60），超过设计文档"0-5 项"的约束（§3.1 行 525）。
- **建议**：将 `findDoctorsForDepartments` 的累计逻辑改为：
  1. 收集所有 `AvailableDoctor`；
  2. 按 score 降序排序；
  3. 取前 5 转换为 `RecommendedDoctor`。
  同时医生 score 来源应当基于科室匹配评分 + availableSlotCount 排序键，设计文档要求"评分"语义需明确。

#### [一般] TriageServiceImpl.triage 主流程存在多处执行路径耦合，可读性差

- **位置**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/service/impl/TriageServiceImpl.java:64-143`
- **描述**：单方法 80 行混合 AI 调用、失败处理、降级链、会话更新、TriageRecord 写入的多重职责：
  1. 行 82-93 同步阻塞 AI（`future.get()` 无超时）；
  2. 行 98-108 AI 成功路径同时转换 `departments` 和 `doctors`；
  3. 行 110-136 AI 失败路径内联规则引擎 + 兜底 + fallbackHint 判定 + TriageRecord 写入；
  4. 行 138-142 AI 成功路径 TriageRecord 写入；
  5. 行 73-80 在调用 AI **之前**就已修改 `session`（setChiefComplaint、setCorrectedChiefComplaint、setAdditionalResponses、setRoundCount），违背 §3.1 行 453 "**先写数据库再更新内存**" 策略——正确顺序应当是先 save TriageRecord 提交事务后再更新 session。
- **建议**：
  1. 将主流程拆分为 `triage(request)` → `callAiWithTimeout(...)` → `applyFallback(...)` → `persistTriageRecord(...)` → `updateSessionAfterCommit(...)` 多个 private 方法；
  2. 关键调整：把行 72-80 的 `session.setXxx(...)` 调用移到 `triageRecordRepository.save(record)` 成功之后（与 §3.1 行 453 的事务一致性策略对齐）；
  3. AI 超时控制。

#### [一般] TriageController / TriageServiceImpl.selectDepartment 不使用 selectDepartment 公开契约

- **位置**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/api/TriageController.java:30-36`
- **描述**：`TriageController.selectDepartment` 调用 `triageService.selectDepartment(sessionId, deptId, deptName, true)` 写死 `overwrite=true`。这与设计文档 §3.1 行 469 "手动选科端点 TriageController.selectDepartment() 调用 selectDepartment 时始终覆盖写入" 一致；但当前实现将 `overwrite` 标志作为方法参数，对外契约污染了内部协作细节——理想做法是 `TriageController` 调用一个"始终覆盖"语义的方法（如 `forceSelectDepartment`），而 `TriageService` 暴露给 listener 的是 `selectDepartmentIfAbsent`（不覆盖）。
- **建议**：拆分接口为两个明确语义的方法，或在 service 层基于 controller/listener 不同调用方做差异化行为封装。

#### [一般] TriageServiceImpl.saveTriageRecord 序列化异常静默吞没

- **位置**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/service/impl/TriageServiceImpl.java:200-214`
- **描述**：catch 分支（行 212-214）`} catch (JsonProcessingException e) { // ignore serialization errors for optional JSON fields }` 完全静默。`recommendedDoctors`、`aiRecommendedDepartments`、`ruleMatchedDepartments` 是关键审计快照字段，序列化失败将导致 TriageRecord 持久化时丢失科室/医生信息——但上层逻辑继续运行并返回成功响应，造成"前端看到正确数据但数据库记录不完整"的数据完整性裂缝。
- **建议**：至少记录 WARN 日志（`log.warn("TriageRecord JSON serialization failed, sessionId={}", request.getSessionId(), e)`），便于运维排查；或重新设计为失败时整体回滚事务，返回错误响应。

#### [一般] TriageServiceImpl 中 saveTriageRecord 未保存 chiefComplaint 修正值

- **位置**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/service/impl/TriageServiceImpl.java:181-217`
- **描述**：与"实体缺失 correctedChiefComplaint 字段"问题耦合。`saveTriageRecord` 中行 188 `record.setChiefComplaint(request.getChiefComplaint())` 始终使用 request 原始 chiefComplaint，**未读取 `session.getCorrectedChiefComplaint()`**——即便后续补全实体字段，写入逻辑也未实现。

#### [轻微] createSession 使用 put 而非 putIfAbsent，存在并发覆盖风险

- **位置**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/dialogue/DialogueSessionManager.java:21-25`
- **描述**：当前 `createSession` 直接 `sessionStore.put(sessionId, session)`。当两个线程同时调用 `restoreSession` 返回 null 后都进入 `createSession` 分支（`TriageServiceImpl:67-70`），后写入的会话会覆盖先写入的（包括 aiFailCount、ruleVersion 快照等）。设计文档 §3.1 行 481 指出 "DialogueSessionManager 承担并发控制——同 session 请求串行，不同 session 独立"，但当前未实现同 session 串行化。
- **建议**：`createSession` 改为 `sessionStore.putIfAbsent(sessionId, newSession)`，并由调用方二次 `get` 校验实际值；或引入 per-session lock 实现真正串行化。

#### [轻微] RecommendedDoctor.score 全部为 0f

- **位置**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/service/impl/TriageServiceImpl.java:174`
- **描述**：`findDoctorsForDepartments` 中 `new RecommendedDoctor(doc.doctorId(), doc.doctorName(), doc.departmentId(), doc.availableSlotCount(), 0f)`，score 硬编码 0f。设计文档 §3.1 行 447 要求"按匹配评分排序取前 5 名"且"score 字段"应被填充，但当前未基于 `availableSlotCount` 或科室匹配度计算评分。
- **建议**：将 score 计算方式提取为可配置的评分函数（如 `score = availableSlotCount / maxAvailableSlotCount`），便于业务调整排序语义。

#### [轻微] AdditionalResponse.answeredAt 字段类型为 String

- **位置**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/dto/AdditionalResponse.java:7`
- **描述**：设计文档 §3.1 行 530 暗示 `AdditionalResponse` 是简单的 Q/A 结构，但 answeredAt 字段类型为 `String`（行 7 `private String answeredAt;`）。从语义上（带时区的时间戳）应为 `LocalDateTime` 或 `Instant`，当前类型易引发前后端解析时区错误。
- **建议**：改为 `LocalDateTime answeredAt` 或 `Instant answeredAt`，对应 ai-api 层 `AdditionalResponseItem` 同步修改。

#### [轻微] TriageResponse.reason 字段在 fallback 路径硬编码英文文案

- **位置**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/service/impl/TriageServiceImpl.java:127`
- **描述**：行 127 `fallbackResponse.setReason("AI service unavailable, using rule engine fallback")`，AI 成功路径下 reason 由 AI 给出（中文），但降级路径下 reason 为英文硬编码，与 §3.1 行 525 "reason 必填字段，字符数 ≥ 1，AI 无法生成时输出默认文案如'根据症状分析，建议就诊XX科室'" 的语义不符。
- **建议**：使用中文默认文案，如 `"AI 分诊暂时不可用，基于规则匹配推荐以下科室"`，与 fallbackHint 文案风格统一。

#### [轻微] findBySessionIdIn 查询方法无引用方

- **位置**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/repository/TriageRecordRepository.java:17`
- **描述**：`List<TriageRecord> findBySessionIdIn(List<String> sessionIds)` 在 consultation 模块内未发现调用方。当前 Phase 2/3 不需要批量按 sessionId 查询，疑似为 Phase 5 预留；建议在文档或注释中标记"Phase 5 预留 API"以避免后续重构时误删。

#### [轻微] ai-api TriageRequest 缺少 correctedChiefComplaint 字段

- **位置**：`AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/triage/TriageRequest.java:5-13`
- **描述**：设计文档 §3.1 行 441 / 行 477 要求 "若 DialogueSession 中 correctedChiefComplaint 存在，则将其替换原始 chiefComplaint 作为推理上下文起点"，但 ai-api 层 `TriageRequest` 没有 correctedChiefComplaint 字段。当前由 TriageConverter 在 TriageRequest.chiefComplaint 上做替换即可，但显式字段会让 AI 侧能区分"原始主诉"与"修正主诉"，更利于 prompt 模板的精细化构造。
- **建议**：在 `TriageRequest` 增加 `private String correctedChiefComplaint;` 字段，在 TriageConverter.toAiTriageRequest 中填充，并保留 chiefComplaint 的原始值（让 AI 显式知道修正发生）。

#### [轻微] ai-api TriageResponse.correctedChiefComplaint 与 consultation TriageResponse 未透传

- **位置**：`AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/triage/TriageResponse.java:16`、`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/dto/TriageResponse.java:5-18`
- **描述**：ai-api `TriageResponse.correctedChiefComplaint` 是隐式主诉修正路径的载体，但 consultation 层 `TriageResponse` 没有对应字段，AI 识别到的主诉修正信息无法透传给前端。建议在 consultation 层 DTO 中也增加 `correctedChiefComplaint`，由 TriageConverter 完成透传——TriageServiceImpl 据此更新 session。

#### [轻微] DefaultTriageRuleEngine 缺少关键词匹配逻辑

- **位置**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/rule/DefaultTriageRuleEngine.java:36-56`
- **描述**：设计文档 §3.1 行 509 规定 `TriageRule.conditions` 字段为 JSON 文本 `{"keywords": ["胸痛", "胸闷"], "logic": "AND"}`，规则引擎按此结构与患者主诉做**关键词匹配**。当前 `match` 方法仅按 `ruleVersion`/`ruleSetId`/`enabled` 过滤后**直接返回所有规则**——即只要规则启用且版本匹配，无论主诉内容是否匹配关键词，都返回该规则的推荐科室。这与"规则引擎"的语义严重偏离，调用 `match("头痛", "v1", "RS001")` 与 `match("骨折", "v1", "RS001")` 返回结果完全相同。
- **建议**：实现 `parseConditions(conditionsJson)` 解析 JSON 为 `RuleCondition(keywords, logic)`；在 `match` 中基于 `chiefComplaint` 做关键词匹配（AND/OR 逻辑），未命中关键词的规则不进入结果集。这是规则引擎的核心语义，缺失将导致"规则匹配"等价于"全量返回该版本的规则科室"，与设计意图不符。

#### [轻微] TriageRuleEngine.match 未应用 session 内的 ruleVersion 快照

- **位置**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/service/impl/TriageServiceImpl.java:114-115`
- **描述**：设计文档 §3.1 行 475 要求 "后续追问使用该快照版本对应的规则，确保对话内分诊逻辑前后一致"。当前 TriageServiceImpl 在降级路径中调用 `triageRuleEngine.match(request.getChiefComplaint(), request.getRuleVersion(), request.getRuleSetId())`——使用 request 传入的 `ruleVersion`/`ruleSetId`，**未使用 `session.getRuleVersion()`/`session.getRuleSetId()` 快照**。如果对话过程中规则版本升级，前端可能未及时更新 `ruleVersion` 字段，但 session 快照仍是旧版本，导致同一会话内不同轮次使用不同规则版本。
- **建议**：降级路径改用 session 快照：`triageRuleEngine.match(request.getChiefComplaint(), session.getRuleVersion(), session.getRuleSetId())`。

#### [轻微] SchedulingRetryConfig 的 @ConditionalOnProperty matchIfMissing=true 可能与 §6.1 集中调度框架冲突

- **位置**：`AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/config/SchedulingRetryConfig.java:11`
- **描述**：设计文档 §6.1 行 1498 要求 "使用 @EnableScheduling + @Scheduled 注解替代手动创建的 ScheduledExecutorService" 并由统一 TaskScheduler 管理。当前 `SchedulingRetryConfig` 同时启用 `@EnableScheduling` + `@EnableRetry`，但与 §6.1 行 1499 "若需自定义线程池，通过 @Bean(TaskScheduler) 覆盖默认配置为线程池大小 2-4" 的集中调度框架没有显式关联——多个业务模块各自启用 @EnableScheduling 在 Spring 中可并存但调度器线程不共享，与设计文档的"集中线程池"意图不符。
- **建议**：将 `@EnableScheduling`/`@EnableRetry` 上提至 application 启动层（或定义一个 `CommonSchedulingConfig`），consultation 模块仅保留 `@Scheduled` 注解的方法；当前文件只承担 `@ConditionalOnProperty` 开关的角色。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 7 |
| 一般 | 14 |
| 轻微 | 9 |

### 总评

consultation 模块整体实现了 §3.1 包C 智能分诊的核心抽象骨架——DTO 结构、`TriageService`/`TriageRuleEngine`/`DepartmentFallbackProvider` 接口、`DialogueSessionManager`/`DialogueRecord`/`RegistrationEventListener` 等关键类齐全，三级降级链（AI → RuleEngine → FallbackProvider）落地，`TriageResponse.fallbackHint` 在 `aiFailCount >= 3` 时注入的契约经 `TriageServiceImplTest` 验证。但与设计文档 §3.1/§6.1 严格对照，发现**核心契约缺失或偏离**集中体现在以下几方面：

1. **事务一致性策略未落地**：`@Transactional` 缺失、`saveTriageRecord` 序列化异常静默、主流程"先内存后数据库"与设计文档"先数据库后内存"倒置；
2. **主诉修正链路不完整**：TriageRecord 缺少 correctedChiefComplaint 快照字段、Repository 缺少按 sessionId 恢复查询、AI 隐式路径识别未实现；
3. **跨模块 DoctorFacade 降级保护完全缺失**：`findDoctorsForDepartments` 无 try/catch 无超时，违反设计文档"不影响分诊科室推荐的正常返回"承诺；
4. **关键校验缺失**：`chiefComplaint` 与 `additionalResponses` 互斥校验（`TRIAGE_FIELD_COMBINATION_INVALID`）、sessionId 的 UUID v4 格式校验、TRIAGE_SESSION_NOT_FOUND 错误码均未实现；
5. **TTL 调度与 §6.1 集中管理框架不一致**：DialogueSessionManager 1 分钟扫描而非 5 分钟；
6. **规则引擎核心语义（关键词匹配）未实现**：当前 match 等价于"按版本过滤后全量返回"，与设计文档 keywords/logic 解析严重背离；
7. **推荐医生截断/排序/评分缺失**：未实现"取前 5 名 + 评分排序"，score 字段硬编码 0f。

测试方面：核心路径（AI 成功、AI 失败降级、规则引擎空、fallback 三连、RegistrationEvent 写入、DeadLetter 补偿）覆盖较好，但**未覆盖**本次审查发现的多项偏差（correctedChiefComplaint 隐式路径、DoctorFacade 异常降级、关键词匹配逻辑、AI 超时、互斥校验、TTL 周期、EXPIRED 迁移等）。建议针对严重级别问题补齐集成测试用例，并在修复时同步更新对应的单元测试断言。

代码风格基本符合项目规范（统一 `Result.success` 响应、Spring `@Component`/`@Service` 注解、`@RequiredArgsConstructor` 风格构造器注入），DTO/Entity 字段命名一致，`TriageConverter` 职责单一（DTO 转换）。修复后整体质量可达成 §3.1 设计的契约完整度。