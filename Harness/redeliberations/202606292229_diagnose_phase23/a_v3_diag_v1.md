# 诊断报告 — Phase 23 C/3/DE 验收问题定位（v4）

## 优先级分组说明

- **P0**（必须立即修复）：阻塞业务流程或导致数据损坏的问题，上线前必须解决
- **P1**（严重影响业务逻辑）：影响功能正确性或业务规则完整性的问题，应在迭代内解决
- **P2**（可并行修复）：语义、日志、配置、文档等不影响核心流程的问题，可与 P0/P1 并行

---

## P0 — 必须立即修复

### C02 — TriageRecordRepository 缺少 findTopBySessionIdOrderByTriageTimeDesc 恢复查询方法
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageRecordRepository.java:11-17` 只有 `findTopByPatientIdOrderByTriageTimeDesc`，无 sessionId 版本；OOD §3.1 TriageRecord 设计明确要求新增 `findTopBySessionIdOrderByTriageTimeDesc(String sessionId)` 用于进程崩溃后从数据库恢复 DialogueSession 快照字段

### C04 — TriageServiceImpl.triage/selectDepartment/saveTriageRecord 缺少 @Transactional；事务边界风险需在修复时严格控制
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageServiceImpl.java:34-35` 类级别无 `@Transactional`，`triage()` `selectDepartment()` `saveTriageRecord()` 均无事务注解。OOD §3.1 "先写数据库再更新内存"策略依赖事务保证——事务提交成功后 DialogueSession 才更新，数据库写入失败时回滚使 session 保持未更新状态
- **事务边界风险说明**（修复时必须控制）：(a) 对 `triage()` 加 `@Transactional` 会使 AI 调用期间持有数据库连接（最长 8 秒），高并发时可能导致连接池耗尽；(b) `saveTriageRecord()` 是 private 方法，Spring AOP 无法拦截自调用；(c) OOD §3.1（line 453）要求事务边界仅包围 save() 操作。修复方案：持久化逻辑抽取为单独 `@Transactional` 方法（如 `persistTriageRecord()`），或使用 TransactionTemplate 编程式事务仅包围 `triageRecordRepository.save()` 调用，确保事务边界不覆盖 AI 调用。

### C06 / E03 — DoctorFacade 跨模块调用无 try/catch、无超时、无 WARN 日志（本质相同 — 合并修复策略）
- **合并修复策略**：在 `TriageServiceImpl.findDoctorsForDepartments()` 中增加统一 try/catch，捕获所有异常后记录 WARN 日志（含调用耗时、异常类型、departmentId），将 TriageResponse.doctors 置为空列表。超时通过 `consultation.doctor-facade.timeout=2s` 配置注入（`@Value`）并传递给 future.get()。合并原因：C06 与 E03 指向同一个代码位置和同一缺失行为。
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageServiceImpl.java:169-179` `findDoctorsForDepartments()` 直接调用 `doctorFacade.findAvailableDoctorsByDepartment()` 无 try/catch。OOD §4.1 要求"DoctorFacade 调用超时/异常时捕获并记录 WARN 日志，TriageResponse.doctors 置为空列表"

### C15 / E01 — RegistrationEventListener @Retryable 对所有 Exception.class 重试（本质相同 — 合并修复策略）
- **合并修复策略**：`@Retryable` 的 `retryFor` 限定为 `{DataAccessException.class, TimeoutException.class}`，增加 `noRetryFor={IllegalArgumentException.class, NullPointerException.class}`。合并原因：C15 与 E01 为同一行代码的同一问题在不同审查文档中的重复记录。
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`RegistrationEventListener.java:36` `@Retryable(retryFor = Exception.class, ...)`。OOD §3.1 要求"仅对 DataAccessException、TimeoutException 等可治愈临时异常触发重试"

### E02 — TriageRecord 同 sessionId 第二次分诊违反 @Column(unique = true) 约束
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageServiceImpl.java:185-216` `saveTriageRecord()` 始终 `new TriageRecord()` 执行 INSERT，不先查已有记录做 UPDATE。OOD §3.1 要求"同一 sessionId 第二次发起分诊时 update 覆盖同 sessionId 记录"——sessionId 标注 `@Column(unique = true)`，第二次 INSERT 将抛出 DataIntegrityViolationException

### P01 — 异步 AI 调度机制未实现
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DedupTaskScheduler.java:35-41` `schedule()` 创建 PENDING 条目后立即返回，无任何代码触发 `AiService.prescriptionAssist()` 异步调用并回填结果。OOD §3.4 要求异步 AI 调用完成后更新 AiSuggestionResult 状态

### A02 — 所有 AI 调用超时完全未外化
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageServiceImpl.java:87` `future.get()` 无超时；`PrescriptionAuditServiceImpl.java:81` `future.get()` 无超时；`PrescriptionAssistServiceImpl.java:78` `future.get()` 无超时；`MedicalRecordServiceImpl.java:124,138` 硬编码 2s/12s。OOD §2.3 要求各操作均配置独立超时并通过 `@Value` 注入

### A03 — AiSuggestionResult 5 状态映射表全部未实现
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DedupTaskScheduler.java` 仅创建 PENDING 条目（无回填）；`PrescriptionAssistServiceImpl.java` checkDose 调用 `dedupTaskScheduler.schedule()` 后不启动异步 AI。OOD §3.4 要求异步 AI 调用管线完整：PENDING → COMPLETED/FAILED 状态映射 + consumed 标记

### M04 — MR_GEN_CONCURRENT_MODIFICATION 在当前 generate() INSERT 路径中无法触发
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`MedicalRecordServiceImpl.java:102-110` 在 INSERT 路径（`new MedicalRecord()` 后 `save()`）上捕获 `ObjectOptimisticLockingFailureException`。JPA 新实体 INSERT 时 `@Version` 字段为 null，不会触发乐观锁异常——OOD §7 描述 MR_GEN_CONCURRENT_MODIFICATION 用于"更新操作使用版本号校验"，即在 UPDATE 路径（读取→合并→写回）中捕获。错误码路径本身设计正确，问题是代码放错了位置
- **OOD 描述核实**：OOD §7 明确写"MedicalRecord 实体增加 @Version 乐观锁字段，更新操作使用版本号校验，写冲突时返回 MR_GEN_CONCURRENT_MODIFICATION 错误码"——OOD 描述的是 UPDATE 路径下的正确语义。代码在 INSERT 路径捕获此异常属于实现编码错误，OOD 描述本身准确

### P02 / E06 — DrugFacade 在 PrescriptionAuditServiceImpl 和 PrescriptionAssistServiceImpl 均未注入（本质相同 — 合并修复策略）
- **合并修复策略**：在两个 Service 的构造器注入 `DrugFacade`，在审核和辅助开方流程中调用。调用超时或异常时捕获返回空药品信息 + WARN 日志，不阻断主流程。
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`PrescriptionAuditServiceImpl.java:46-71` 和 `PrescriptionAssistServiceImpl.java:37-64` 构造器均无 `DrugFacade` 参数。OOD §2.2 明确要求 DrugFacade 的注入和使用

### P03 / S02 — AiSuggestionResult + PrescriptionDraftContext TTL 清理任务完全缺失（本质相同 — 合并修复策略）
- **合并修复策略**：在 prescription 模块 `config/` 包增加 `ScheduledTaskConfig`，注册两个独立 `@Scheduled(fixedRate = 300000)` 清理任务：`SuggestionCleanupTask.cleanup()` 扫描 AiSuggestionResult 删除 TTL > 60 分钟的条目；`DraftContextCleanupTask.cleanup()` 扫描 PrescriptionDraftContext 删除 TTL > 60 分钟的条目。
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：prescription 模块和全局 scope 均无 `@Scheduled` TTL 清理方法。OOD §3.4、§6.1 要求每 5 分钟扫描清理

### P04 / E04 — 规则变更事件未定义、未监听（本质相同 — 合并修复策略）
- **合并修复策略**：在 prescription 模块 `event/` 包定义 3 类事件（`DrugContraindicationChangeEvent`、`DrugAllergyMappingChangeEvent`、`DrugCompositionDictChangeEvent`）+ `@EventListener` 消费端 + Caffeine `invalidate()`。3 类事件可设计为共享一个基类 `BaseDrugDataChangeEvent` 以减少样板代码。
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：prescription 模块下 `event/` 包不存在，无任何 ChangeEvent 定义或消费端。OOD §2.2 要求事件驱动缓存失效

### S03 — DedupTaskScheduler.schedule() 在 compute lambda 内嵌套 suggestionStore.put(candidateTaskId, ...)
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DedupTaskScheduler.java:39` 在 `compute()` 的 remappingFunction 内调用 `suggestionStore.put(candidateTaskId, newResult)`，跨两个 key 操作，compute 的原子性保证被跨 key 写入破坏。OOD §3.4 要求 compute 闭包内仅操作同一 key

### S04 — 同 session 并发访问无串行化保护
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DialogueSessionManager.java:21-25` createSession 使用 `put` 非 `putIfAbsent`；`DialogueSession` 为可变 POJO 无锁；`TriageServiceImpl.java:66-80` 同 session 并发时可能并发修改共享状态。OOD §3.1 要求 DialogueSessionManager 承担并发控制

---

## P1 — 严重影响业务逻辑

### C01 — TriageRecord 实体缺失 correctedChiefComplaint 快照字段
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageRecord.java:14-144` 无 `correctedChiefComplaint` 字段；OOD §3.1 TriageRecord 设计明确要求"增加 chiefComplaint 原始主诉快照字段和 correctedChiefComplaint 快照字段"

### C03 — AI 隐式 correctedChiefComplaint 路径未实现
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageConverter.java:54-90` `toTriageResponse()` 从未读取 ai-api `TriageResponse.correctedChiefComplaint`（ai-api DTO 已有此字段行 93-98）。OOD §3.1 要求检测并写入 `DialogueSession.correctedChiefComplaint`

### C05 — chiefComplaint 与 additionalResponses 互斥校验缺失
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageServiceImpl.java:64-80` `triage()` 入口处无组合校验。OOD §3.1 要求"同时提供二者或均未提供时返回 HTTP 400 + TRIAGE_FIELD_COMBINATION_INVALID"

### C07 — AI 调用 future.get() 无超时参数
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageServiceImpl.java:87` `future.get()` 无超时参数。OOD §2.3 要求`ai.timeout.triage=8s`

### C08 — selectDepartment 签名为 4 参、RegistrationEventListener 未调用该方法
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageService.java:10` 签名为 `selectDepartment(String sessionId, String departmentId, String departmentName, boolean overwrite)` 4 参；OOD §3.1（line 467）设计为 3 参（无 overwrite）。`RegistrationEventListener.java:38-44` 直接操作 repository 而非调用 `TriageService.selectDepartment()`
- **修复方向（对齐 OOD §3.1 line 469）**：(a) `TriageService.selectDepartment` 接口改为 3 参，移除 `overwrite` 参数；Service 实现内部始终覆盖写入 `finalDepartmentId/finalDepartmentName`。(b) `RegistrationEventListener.handleRegistrationEvent` 在调用 `selectDepartment()` 前自行检查 `TriageRecord.finalDepartmentId` 是否为空，仅当为空时调用（事件写入仅在手动选科未发生时生效）。(c) `TriageController.selectDepartment` 直接调用 `selectDepartment()` 3 参版本，因 Service 内部始终覆盖写入，Controller 不需要传递 overwrite 语义。(d) `DeadLetterCompensationService.compensateDeadLetters()` 行 38 调用 `selectDepartment()` 时移除 false 参数。此方案与 OOD §3.1（line 469）"RegistrationEventListener 在调用 selectDepartment 前先检查 TriageRecord.finalDepartmentId 是否为空，仅当为空时调用；手动选科端点始终覆盖写入"完全对齐。注意：不可在 Service 实现内根据调用来源决策——TriageServiceImpl 是单例 Service Bean，无法感知当前调用者身份，且 OOD 明确要求判断逻辑在 EventListener 侧。
- **时序假设前提**：三阶段完成顺序由前端流程保证——(a) triage 流程完成并持久化 TriageRecord → (b) 前端进入挂号界面 → (c) registration 模块发布 RegistrationEvent。若 RegistrationEvent 在 triage 完成前触发（罕见时序竞争），TriageRecord 尚不存在，findBySessionId 返回 null，EventListener 无法判断是该跳过还是等待。如需防御性处理，可在 EventListener 中增加 TriageRecord 不存在时静默跳过、记录 WARN 日志。
- **C08 与 C22 的调和**：C08（接口多 overwrite 参数）和 C22（Controller 硬编码 overwrite=true）指向同一根因——overwrite 暴露在接口层。统一修复方案：接口改为 3 参，移除 overwrite，Service 内部始终覆盖写入；EventListener 调用前自行判断 finalDepartmentId 状态；Controller 直接调用。全量对齐 OOD line 469 设计。

### C09 — selectDepartment 使用 GlobalErrorCode.NOT_FOUND 而非业务级错误码
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageServiceImpl.java:148-150` 使用 `GlobalErrorCode.NOT_FOUND`。OOD §3.1 要求"记录不存在时返回 TRIAGE_SESSION_NOT_FOUND"

### C12 — AI 上下文未按模板拼接全量历史，未实现 correctedChiefComplaint 替换 + 3000 字符截断
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageConverter.java:22-52` `toAiTriageRequest()` 仅单向映射，未实现全量拼接、correctedChiefComplaint 替换、上下文截断策略。OOD §3.1 要求"全量拼接策略"和"上下文截断策略"

### C13 — TriageRuleEngine.match 无快照失效回退逻辑，缺 ruleVersionMismatch 输出标记
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DefaultTriageRuleEngine.java:36-56` `match()` 仅过滤后返回，未实现快照版本无结果时的降级回退，未返回 `ruleVersionMismatch` 标记。OOD §3.1 要求"降级使用当前最新版本规则集重新匹配，并在 TriageResponse 中标记 ruleVersionMismatch=true"

### C16 — DefaultTriageRuleEngine.match 未实现 TriageRule.conditions JSON 关键词解析
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DefaultTriageRuleEngine.java:36-56` 仅按规则版本和集标识过滤返回全量命中规则，未对 `conditions` JSON 做关键词解析与 AND/OR 逻辑匹配。OOD §3.1 TriageRule 设计 `conditions` 为关键词匹配条件（`{"keywords": ["胸痛", "胸闷"], "logic": "AND"}`）

### C17 — findDoctorsForDepartments 未按匹配评分排序取前 5 名
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageServiceImpl.java:169-179` 无条件追加所有医生到列表，无排序、无限制取前 5。OOD §3.1 要求"按匹配评分排序取前 5 名"

### C19 — saveTriageRecord 未读取 session.getCorrectedChiefComplaint() 写入实体
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageServiceImpl.java:181-217` `saveTriageRecord()` 未将 `session.getCorrectedChiefComplaint()` 写入 `TriageRecord`。OOD §3.1 要求 TriageRecord 存储 correctedChiefComplaint 快照

### C20 — createSession 使用 put 而非 putIfAbsent，并发覆盖风险
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DialogueSessionManager.java:23` `sessionStore.put(sessionId, session)`。OOD §3.1 要求原子创建

### C21 — 降级路径使用 request 参数而非 session 快照的 ruleVersion/ruleSetId
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageServiceImpl.java:114-115` `triageRuleEngine.match(request.getChiefComplaint(), request.getRuleVersion(), request.getRuleSetId())` 使用请求参数而非 session 快照值。OOD §3.1 要求使用 DialogueSession 中记录的规则版本快照

### C23 — 主流程在调用 AI 前修改 session，与"先写数据库再更新内存"策略的适用边界
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **问题澄清**：`TriageServiceImpl.java:72-80` `session.setChiefComplaint()`/`setAdditionalResponses()`/`setRoundCount()` 发生在 AI 调用（行 83）之前。这些 session 修改构成 AI 请求输入的必需前置操作（`toAiTriageRequest(request, session)` 读取 session 中的这些字段），不能移至 AI 调用之后。OOD §3.1 "先写数据库再更新内存"策略覆盖的是 TriageRecord 持久化路径（行 134/141 的 `saveTriageRecord()` 写数据库 → 后续 session 状态更新），不覆盖 AI 输入准备阶段的 session 修改
- **实际根因**：`setChiefComplaint()`/`setAdditionalResponses()` 的输入值来源于 `request` 参数，这些值在 AI 调用前写入 session 是正确数据流。问题在于(1) session 在 AI 调用前被修改但未被持久化快照；(2) 未在 AI 返回后利用 session 快照字段恢复前序修改。修复方案应聚焦：将 AI 输入准备阶段的 session 修改与"先写数据库再更新内存"策略涉及的持久化路径解耦——AI 输入准备阶段的 session 修改保持当前顺序（在 AI 调用前），持久化路径按策略在 AI 返回后进行。
- **精确时序表**：
  - [1] 行 72-80：`session.setChiefComplaint()`/`setAdditionalResponses()`/`setRoundCount()` — AI 输入准备，保持正序，不可移动
  - [2] 行 82-83：`aiService.triage(toAiTriageRequest(request, session))` — AI 调用，含超时/降级
  - [3] 行 95-108：处理 AI 结果（`aiResult.getData()` 读取 recommendedDepartments、correctedChiefComplaint 等）
  - [4] 行 134/141：`saveTriageRecord()` — 写数据库（TriageRecord 持久化），此步骤应在事务保护下执行
  - [5] 事务提交成功后：`session.setAiFailCount(0)`（行 140，AI 成功路径）
  - [6] 事务提交成功后：`session.setCorrectedChiefComplaint(aiData.getCorrectedChiefComplaint())` — 隐式主诉修正回写 session

### C14 — DeadLetterCompensationService 未检查 retryCount >= maxRetryCount
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DeadLetterCompensationService.java:42-46` `compensateDeadLetters()` 捕获异常后仅递增 retryCount，未判断 `retryCount >= maxRetryCount` 时将状态迁移到 `EXPIRED`。OOD §3.1 要求状态迁移规则
- **精确执行顺序**：(1) 补偿前先判断 `retryCount >= maxRetryCount`；(2) 已达上限则直接设 state=EXPIRED 并保存，跳过补偿；(3) 未达上限则执行补偿逻辑（`triageService.selectDepartment()`）；(4) 补偿失败时递增 retryCount 并保存（保持当前 state=FAILED）；(5) 补偿成功时设 state=COMPENSATED 并保存。

### P05 — SubmitResponse 缺 warnResult 字段；WARN 路径使用设计未定义的错误码 RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT
- **真实性**：真实存在
- **根因分类**：实现编码问题 + OOD 文档修改
- **[OOD 文档修改]** OOD §1.1a（line 111）SubmitResponse 基础字段定义为 submitted/prescriptionOrderId/blockInfo/errorCode，warnResult 字段（含 riskLevel/alerts/auditRecordId/prescriptionHash）仅在 §4.6 响应 JSON 示例中出现（line 1268-1281）。warnResult 是 SubmitResponse 的扩展字段，但应将其从"示例级描述"提升为"正式字段定义"——在 OOD §1.1a SubmitResponse 定义中补充 warnResult 字段（WarnResult 类型，可选），并定义 WarnResult 类的子字段：
  - riskLevel（AuditRiskLevel 枚举，必填）
  - alerts（List\<WarnAlert\>，每项含 alertCode/alertMessage/severity 三个字段，severity 为 AlertSeverity 枚举）
  - auditRecordId（Long，必填）
  - prescriptionHash（String，必填）
- **[代码修改]** `SubmitResponse.java:1-44` 仅含 submitted/prescriptionOrderId/blockInfo/errorCode 四个字段，无 `warnResult` 字段。需要新增 `warnResult` 字段（WarnResult 类型）及对应的 DTO 类，实现 warnResult 的按需返回逻辑。同时，`PrescriptionAuditServiceImpl.java:205` 使用 `RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT`——此错误码在 OOD 任何位置均未定义（OOD §5.1 错误码表无此条目），且 WARN 路径不应通过 errorCode 承载风险语义。修复方向：WARN 路径不使用 errorCode，改为通过 SubmitResponse.warnResult 承载风险信息（riskLevel/alerts/auditRecordId/prescriptionHash），errorCode=null；RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT 错误码从代码侧移除，或保留用于 forceSubmit=true 校验失败且 warnResult 不可用时的兜底降级路径。

### P06 — 降级路径 LocalRuleResult 未转换为 AuditAlert
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`PrescriptionAuditServiceImpl.java:96-103` 降级路径 `response.setAlerts(Collections.emptyList())`，不转换 LocalRuleResult。OOD §4.2 要求降级路径同样输出风险提示列表

### P07 — AuditRecord.auditIssues 字段从未写入
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`AuditRecord.java:50` 定义了 `auditIssues` 字段但 `PrescriptionAuditServiceImpl.java:307-342` `persistAuditRecord()` 不写入该字段

### P08 — forceSubmit 路径在生成 prescriptionOrderId 后未回写到 AuditRecord
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`PrescriptionAuditServiceImpl.java:231-245` forceSubmit 成功后 `latestRecord.setForceSubmitted(true)` 保存，但 `latestRecord.setPrescriptionOrderId(...)` 未被调用

### P10 — DosageThresholdService.matchByPriority 第一、二重循环逻辑重复
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DosageThresholdService.java:103-121` 第一重循环（行 103-113）与第二重循环（行 115-121）逻辑几乎完全重复。OOD §8.4 要求 6 级匹配优先级

### P11 — SpecialPopulationDosageRule 与 DosageLimitRule 使用相同通用 DosageStandard 查询
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`SpecialPopulationDosageRule.java:36` 和 `DosageLimitRule` 均使用 `dosageStandardRepository.findByDrugCodeAndRouteOfAdministration()`，未区分特殊人群分级，可能产生重复告警

### P13 — AllergyCheckRule 结构化匹配未命中时不回退到文本匹配
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`AllergyCheckRule.java:43-60` 当 `allergyDetails` 存在且非空时结构化过敏原匹配未命中则直接进入下一药品（行 62 返回 PASS），不回退到 `allergyHistory` 文本匹配。OOD §3.2 要求"结构化匹配优先，缺省回退文本匹配"

### P14 — PrescriptionAssistServiceImpl 各失败路径未写入 PrescriptionDraftContext
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`PrescriptionAssistServiceImpl.java:81-99` 的 catch 块和 `buildEmptyResponse()`（行 205-214）直接返回空响应，不写入 CRITICAL 阻断到 PrescriptionDraftContext。OOD §3.4 要求 AI 不可用时 CRITICAL 阻断需持久化

### P16 — AuditRecordRepository 没有 List 版本 findByPrescriptionOrderIdAndIsLatestTrue
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`AuditRecordRepository.java:18` 仅有 `findTopByPrescriptionOrderIdAndIsLatestTrue` 返回 Optional，无 List 版本。persistAuditRecord 未按 prescriptionOrderId 分组清理 isLatest

### M01 — 4 个错误码缺失
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`MedicalRecordErrorCode.java:5-9` 当前仅 4/8 个错误码，缺少 `MR_GEN_AI_UNAVAILABLE` / `MR_GEN_AI_INPUT_INVALID` / `MR_GEN_AI_OUTPUT_INCOMPLETE` / `MR_GEN_TEMPLATE_LOAD_FAILED`

### M02 — TemplateConfigChangeEvent 事件定义 + @EventListener + Caffeine invalidate 全未实现
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DatabaseTemplateConfigManager.java:31-41` 只有 Caffeine 定时刷新（60s），无事件驱动失效机制。OOD §2.2 要求事件驱动立即失效 + 定时刷新兜底

### M03 — VisitIdReconciledTask 定时调和任务未实现
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：medical-record 模块无 `task/` 包，无定时任务。OOD §4.3 要求每 30 分钟扫描 visitIdFallback=true 记录反查正确 visitId

### M05 — RecordGenerateRequest 缺少验证注解
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`RecordGenerateRequest.java:3-49` 无 `@NotNull/@Size(min=50,max=10000)` 等验证注解

### M06 — 超时硬编码在源码中，未通过 @Value 注入
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`MedicalRecordServiceImpl.java:124,138` AI 超时 12s 和 VisitFacade 超时 2s 硬编码

### M07 — MedicalRecordConverter.toRecordGenerateResponse 在非超时失败时仍设 success=true
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`MedicalRecordConverter.java:56` `response.setSuccess(true)` 在所有路径下均为 true

### M09 — MedicalRecord.doctorId 在 generate() 流程中从未被赋值
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`MedicalRecordServiceImpl.java:94-101` 创建实体时不设 doctorId

### M11 — ai-api MedicalRecordGenResponse.missingFields 与 partialContent 未被业务层消费
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`MedicalRecordConverter.java:21-31` `toFieldsMap()` 仅提取 7 个业务字段，未消费 missingFields 和 partialContent

### S01 — SuggestionStore 接口缺少 createIfNotExists 原子方法
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`SuggestionStore.java:6-8` 仅有 `compute()` 方法（需外部封装），缺少 `createIfNotExists(taskId, prescriptionId, supplier)` 原子方法。OOD §3.4 要求此方法消除 TOCTOU 竞态

### S06 — DedupTaskScheduler.schedule() 返回值从 Object 强制转换为 AiSuggestionResult，类型不安全
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DedupTaskScheduler.java:43` `return ((AiSuggestionResult) result).getTaskId()` 在 compute lambda 外强制转换

### S07 — ConcurrentHashMapStore 同时实现 SuggestionStore 和 DraftContextStore，共用同一 ConcurrentHashMap
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`ConcurrentHashMapStore.java:11` 同时实现两个 Store 接口；`ConcurrentHashMapStore.java:13` 单 `ConcurrentHashMap<String, Object>` 实例，遍历清理会混淆两类数据

### A01 — AiResultFactory 在全部 4 个业务 Service 实现中零引用
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：四个业务 Service 均手工 `new AiResult<>()`，未引用 AiResultFactory。OOD §2.3 要求使用 AiResultFactory 静态工厂方法

### A04 — correctedChiefComplaint 显式透传路径未生效
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageConverter.java:24` `toAiTriageRequest()` 不读取 `session.getCorrectedChiefComplaint()`

### A05 — MockAiService 与设计契约严重不符
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`MockAiService.java:40` 使用 `@ConditionalOnProperty` 而非 `@Profile("mock")`，无三种返回模式，无 `MockAdminController`，无 `ai.mock.response-strategy` 配置键。OOD §2.3 MockAiService 实现契约完整要求了以上全部

### A06 — DegradationStrategy/DegradationContext 为空壳；FallbackAiService 仅取 delegates.get(0)
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DegradationStrategy.java:3-6` 仅接口定义；`DegradationContext.java:3-7` 空类；`FallbackAiService.java:183-194` 无重试链/降级链

### A09 — AuditConverter.toAuditResponse 在 aiData == null 时退化为 PASS + 空 alerts
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **修复定位说明**：`AuditConverter.java:48-56` 中 `aiData == null` 时设 PASS + 空 alerts 是被动映射行为。Converter 无决策能力——它仅将 aiResult 的状态翻译为业务 DTO。真正的修复点在调用方 `PrescriptionAuditServiceImpl` 中——调用 AuditConverter.toAuditResponse() 前应检查 `aiResult` 的有效性：当 `aiResult.isSuccess() != true || aiResult.getData() == null` 时，应走降级路径（LocalRuleEngine 校验 + AuditRecord.fromFallback=true），而非将 null data 传入 Converter 让 Converter 默认 PASS

### E05 — RegistrationEventListener.recover() 手工构造含 3 字段的 HashMap 作为 eventPayload
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`RegistrationEventListener.java:50-57` 手工构建只有 3 字段的 HashMap，缺少完整事件对象序列化

---

## P2 — 可并行修复

### C10 — DialogueSessionManager 未校验 sessionId 的 UUID v4 格式
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DialogueSessionManager.java:21-25,31-37` `createSession()` 和 `restoreSession()` 均无 UUID 格式校验。OOD §3.1 要求"接受前端传入的 UUID v4 格式，验证格式有效性"

### C11 — TTL 清理周期为 1 分钟而非设计要求的 5 分钟
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DialogueSessionManager.java:39` `@Scheduled(fixedRate = 60000)`。OOD §3.1 要求统一 Spring @Scheduled 任务每 5 分钟扫描清理

### C18 — saveTriageRecord 中 catch JsonProcessingException 完全静默
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageServiceImpl.java:212-213` `catch (JsonProcessingException e) { // ignore }`。OOD 要求 WARN 日志记录

### C22 — TriageController.selectDepartment 写死 overwrite=true（与 C08 合并修复, 见 C08）
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageController.java:34` `triageService.selectDepartment(sessionId, departmentId, departmentName, true)` 硬编码 overwrite=true。修复方案同 C08——接口改为 3 参，移除 overwrite，Service 内部始终覆盖写入；EventListener 调用前自行判断。

### P09 — PrescriptionItem.unit 字段为 OOD 未正式定义的扩展字段
- **真实性**：真实存在
- **根因分类**：OOD 文档修改 + 实现编码问题
- **[OOD 文档修改]** OOD §3.2 PrescriptionItem 核心 6 字段（drugId/drugName/dose/frequency/duration/route）未包含 unit。unit 字段在剂量校验链路中具有实质业务需求——`DosageCheckRequest`（OOD §1.1b 定义）已包含 `unit` 字段（line 145），用于单位一致性校验（OOD §8.3 DosageUnitGroup 分组换算）。PrescriptionItem 作为处方药品条目（同时用于审核请求 `AuditRequest.prescriptionItems` 和提交请求 `SubmitRequest.prescriptionItems`），若缺少 unit，在审核/提交流程中无法进行准确的跨组单位换算和超限判定。OOD 应在 PrescriptionItem 定义中补充 unit（String，可选），并在 §8.3 单位一致性校验中说明其用途。DosageCheckRequest（单药品即时校验）与 PrescriptionItem（多药品处方条目）使用场景不同但 unit 的业务角色一致——用于 DosageUnitGroup 分组换算。
- **需要添加 unit 的 OOD 完整位置清单**：(1) §1.1a SubmitRequest/SubmitResponse 中的 PrescriptionItem 引用（line 110，字段表描述"每项含 drugId/drugName/dose/frequency/duration/route"——应追加 unit）；(2) §3.2 PrescriptionItem 正式定义——AuditRequest 中的 prescriptionItems（line 101/618）、PrescriptionAssistResponse 中的 drugs 数组（line 144/809，每项含 drugId/drugName/dose/frequency/duration/route）；(3) §4.6 提交流程中的 PrescriptionItem 引用（line 928 请求 DTO 描述、line 1193/1226/1247/1259 响应 JSON 示例中的 prescriptionItems 数组）；(4) §8.3 单位一致性校验中的字段说明（line 1652 DosageStandard.unit 字段定义——此处已在说明 unit，需补充与 PrescriptionItem.unit 的映射关系）。
- **[代码修改]** `PrescriptionItem.java:11` 已有 `unit` 字段（当前属于 OOD 未定义的扩展字段）。OOD 正式补充后，unit 字段在 PrescriptionItem 中保持现有实现即可，无需额外代码修改。

### P12 — DrugInteractionRule 无 Phase 4 预留标注
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DrugInteractionRule.java:12-14` 的 `check()` 直接返回 PASS，无 `@ConditionalOnProperty` 控制运行时启用/禁用

### P15 — AuditResponse.fromFallback 未序列化到 JSON 响应；降级 BLOCK 路径 reasons 固定字符串
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`PrescriptionAuditController.java:36-48`/`PrescriptionAuditServiceImpl.java:144-152` BLOCK 路径返回固定字符串

### M08 — 三处 catch 块手工 new AiResult<>() + setter，未使用 AiResultFactory
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`MedicalRecordServiceImpl.java:141-163` 手工 `new AiResult<>()` 然后逐个 setter

### M10 — MedicalRecord.content 字段无 @Column(name="content_json")
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`MedicalRecord.java:36-38` `@Column(columnDefinition = "TEXT")` 而非 `@Column(name="content_json", columnDefinition = "TEXT")`

### A07 — AiResult.success(data) 允许 data=null，违反契约
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`AiResult.java:22` `AiResult.success(T data)` 不校验 data=null。OOD §2.3 要求"success=true → data 非 null"

### A08 — 降级路径 fallback 文案硬编码英文，设计要求中文
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageServiceImpl.java:127,132` 英文文案

### A10 — application.yml 完全缺失 ai.timeout.* / facade.*.timeout / ai.mock.* 等配置项
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`application.yml:1-14` 仅含 JWT 配置
- **需要添加的完整配置键列表及默认值**：
  - `ai.timeout.triage=8s`（OOD §2.3 TriageServiceImpl 超时配置）
  - `ai.timeout.prescription-audit=6s`（OOD §3.2 处方审核超时配置）
  - `ai.timeout.medical-record-generate=12s`（OOD §3.3 病历生成超时配置）
  - `ai.timeout.prescription-assist=8s`（OOD §3.4 辅助开方超时配置）
  - `consultation.doctor-facade.timeout=2s`（OOD §4.1 DoctorFacade 超时配置）
  - `medical-record.visit-facade.timeout=2s`（OOD §3.3 VisitFacade 超时配置）
  - `ai.mock.response-strategy=STATIC`（OOD §2.3 MockAiService 响应策略配置）

### A11 — 业务层普遍在 isSuccess() 后额外做 && getData() != null 防御性检查
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **问题澄清**：OOD §2.3 契约"success=true → data 非 null"语义清晰。业务层防御性检查（如 `TriageServiceImpl.java:98` `aiResult.isSuccess() && aiResult.getData() != null`）说明契约不被信任。根因在于 `AiResult.success(data)` 未实现 data != null 断言，且测试用例断言 `success(null)` 合法——这是实现编码问题。修复方向：在 `AiResult.success(data)` 中增加 `Objects.requireNonNull(data)` 断言，移除业务层冗余判空

### S05 — PrescriptionDraftContext.updateCriticalAlerts 使用 get-check-then-put 模式（误报）
- **真实性**：误报
- **根因分类**：N/A（误报条目）
- **证据**：`PrescriptionDraftContext.java:34-41` `updateCriticalAlerts()` 仅检查入参 `alerts` 是否为 null/empty，不存在从 Store 读取的操作。`put` 和 `remove` 在 `ConcurrentHashMap` 上均为原子操作，无需 `compute()` 原子化改造

---

## 跨问题耦合副作用分析

以下三组修复项之间存在相互约束，必须协调实施顺序和设计决策：

### A07（AiResult.success(data) 不校验 data=null）× A09（AuditConverter.toAuditResponse 在 aiData==null 时退化为 PASS+空alerts）

**问题**：A07 修复（在 AiResult.success(data) 中增加 Objects.requireNonNull(data) 断言）与 A09 修复（调用方前置检查+降级路径）之间存在相互约束：

1. **修复顺序依赖**：若 A07 先修复（添加断言），则 null-data 路径被消除，A09 的 null-handling 变为不可达代码（AiResult.success(null) 会直接抛 NPE，Converter 层永远不会收到 null data）。若 A09 先修复（调用方前置检查+降级路径），则 A07 变为低优先级——因为调用方在传入 Converter 前已拦截 null 情况。

2. **约定的修复实施顺序**：(1) 先修复 A09 调用方前置检查+降级路径——PrescriptionAuditServiceImpl 在调用 AuditConverter.toAuditResponse() 前检查 aiResult.isSuccess() != true || aiResult.getData() == null，走降级路径；(2) 再修复 A07 的 Objects.requireNonNull(data) 断言，使契约强制生效；(3) AiResult.failure() 的 partialData 可能为 null，不受 success() 断言约束，无需修改。

### C04（@Transactional 事务边界）× E02（唯一约束冲突）× S04（并发控制）

**问题**：C04 要求在 `saveTriageRecord()` 外包事务，E02 要求同 sessionId 第二次分诊时 UPDATE 而非 INSERT（避免唯一约束冲突），S04 要求同 session 并发访问串行化。三者存在以下耦合：

1. **事务边界与并发隔离**：若事务边界过宽（覆盖 AI 调用），则 AI 调用期间持有数据库连接，高并发下连接池耗尽；若事务边界仅包围 save()，则 E02 的 UPDATE 操作在并发场景下需要 `@Lock(PESSIMISTIC_WRITE)` 防止不可重复读或幻读——虽然 `@Column(unique = true)` 提供了唯一约束保障，但 UPDATE 在并发写入相同 sessionId 时仍可能在应用层读到旧数据后写回错误值。建议：`findBySessionId` 查询使用 `@Lock(PESSIMISTIC_WRITE)`，事务边界仅包围 save 操作（编程式事务或独立 `@Transactional` 方法）。

2. **C23 session 修改时序与 C04 事务边界的交互**：AI 输入准备阶段的 session 修改（行 72-80）在持久化事务外执行，不受回滚保护——若 AI 调用成功但 saveTriageRecord 失败回滚，session 中已写入的 chiefComplaint/addtionalResponses 不会自动回退。此场景下 session 处于"已修改但未持久化"的不一致状态。这是 OOD §3.1 策略有意接受的边界情况——下一次同 session 请求时，session 中的值被重新覆盖（行 72-80 始终从 request 读取最新值写入），不依赖 session 持久化后的一致性。

3. **修复实施顺序**：
   - 第一步：抽取 `persistTriageRecord()` 为独立 `@Transactional` 方法（或使用 TransactionTemplate），事务仅包围 `triageRecordRepository.save()` 调用，确保 AI 调用不在事务内。
   - 第二步：在 `saveTriageRecord()` 中实现 UPDATE 逻辑（先查 `findBySessionId`，存在则 update，不存在则 insert），替代当前始终 `new TriageRecord()` 的 INSERT 路径。
   - 第三步：在 `findBySessionId` 查询上加 `@Lock(PESSIMISTIC_WRITE)` 防止并发 UPDATE 冲突。
   - 第四步：`DialogueSessionManager` 的 `createSession` 使用 `putIfAbsent` 替代 `put`，确保同 session 串行创建。

---

## 修订说明（v5）

| 质询意见 | 回应 |
|---------|------|
| Q1（轻微—OOD §3.3 节号引用错误） | 接受。已将 P05 [代码修改] 中 "OOD §3.3 错误码表" 修正为 "OOD §5.1 错误码表"。经核实，报告中仅有 line 188 一处实际引用 §3.3，修订说明 Q3 引用的是 §4.6 而非 §3.3——已精确修正唯一错误引用处 |
| Q2（一般—P05 错误码处理方向二选一模糊建议） | 接受。已将 P05 [代码修改] 中的二选一表述改为明确的单向建议：WARN 路径不使用 errorCode，通过 SubmitResponse.warnResult 承载风险信息（riskLevel/alerts/auditRecordId/prescriptionHash），errorCode=null；RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT 从代码侧移除，或保留为 forceSubmit=true 校验失败且 warnResult 不可用时的兜底降级路径 |
| Q3（一般—A07×A09 跨问题耦合未分析） | 接受。已新增 A07（AiResult.success(data) 不校验 data=null）× A09（AuditConverter 在 aiData==null 时退化为 PASS+空alerts）耦合条目，约定修复实施顺序：(1) 先修复 A09 调用方前置检查+降级路径；(2) 再修复 A07 的 Objects.requireNonNull(data) 断言；(3) 说明 AiResult.failure() 的 partialData 可能为 null，不受 success() 断言约束 |
| Q4（轻微—A10 缺少具体配置键列表） | 接受。已在 A10 条目中补充完整配置键列表及默认值：ai.timeout.triage=8s、ai.timeout.prescription-audit=6s、ai.timeout.medical-record-generate=12s、ai.timeout.prescription-assist=8s、consultation.doctor-facade.timeout=2s、medical-record.visit-facade.timeout=2s、ai.mock.response-strategy=STATIC |
| Q5（轻微—C08 时序依赖未分析） | 接受。已在 C08 修复方向中补充时序假设前提：三阶段完成顺序（triage 完成并持久化 TriageRecord → 前端进入挂号界面 → registration 发布 RegistrationEvent）由前端流程保证；TriageRecord 不存在时 EventListener 应静默跳过并记录 WARN 日志 |
| Q6（轻微—P09 OOD 受影响位置枚举不完整） | 接受。已在 P09 [OOD 文档修改] 中枚举完整位置清单：(1) §1.1a SubmitRequest/SubmitResponse 中的 PrescriptionItem 引用；(2) §3.2 PrescriptionItem 正式定义（AuditRequest.prescriptionItems、PrescriptionAssistResponse.drugs 数组）；(3) §4.6 提交流程中的 PrescriptionItem 引用；(4) §8.3 DosageStandard.unit 字段定义及与 PrescriptionItem.unit 的映射关系 |

## 修订说明（v4）

| 质询意见 | 回应 |
|---------|------|
| Q1（中等—P05/P09 OOD 文档修改方向未作为独立任务项标明） | 接受。已将 P05 和 P09 各拆分为 `[OOD 文档修改]` 和 `[代码修改]` 两个独立子项，并在标题中标注 `根因分类` 分别显示。P05：OOD 文档修改（warnResult 正式字段定义 + WarnResult 子结构）和代码修改（SubmitResponse 新增 warnResult 字段 + RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT 错误码处理）；P09：OOD 文档修改（PrescriptionItem 补充 unit 字段）和代码修改（已有 unit 字段保持不动） |
| Q2（中等—C08"根据调用来源决定 overwrite"方案不可行） | 接受。经核实 TriageServiceImpl 是单例 Service Bean，无法感知当前调用者（Controller vs RegistrationEventListener）。已修正 C08 修复方向为 OOD §3.1（line 469）方案——Service 接口改为 3 参无 overwrite，Service 内部始终覆盖写入；EventListener 调用前自行检查 finalDepartmentId 是否为空，仅当为空时调用；Controller 直接调用 3 参版本。不可在 Service 实现内根据调用来源决策 |
| Q3（轻微—P05 证据中 alerts 子字段描述不完整） | 接受。已在 P05 `[OOD 文档修改]` 中完整描述 WarnResult 的 alerts 子字段（List\<WarnAlert\>），每项含 alertCode/alertMessage/severity（AlertSeverity 枚举），并增加对 OOD §4.6 响应示例（line 1268-1281）的直接引用 |
| Q4（严重—C04 修复建议未分析事务边界风险） | 接受。已在 C04 中补充事务边界风险分析：(a) 对 triage() 加 @Transactional 会使 AI 调用期间持有数据库连接 8 秒，高并发时连接池耗尽；(b) saveTriageRecord() 是 private 方法，Spring AOP 无法拦截自调用；(c) OOD §3.1（line 453）要求事务边界仅包围 save() 操作。修复方案：抽取持久化逻辑为独立 @Transactional 方法或使用 TransactionTemplate 编程式事务 |
| Q5（中等—C08+C22 合并修复方案与 OOD 设计矛盾） | 接受。OOD §3.1（line 469）明确设计为"EventListener 自行判断 finalDepartmentId 后决定是否调用 selectDepartment"，而非"Service 内部根据来源决定"。已修正 C08 修复方向全量对齐 OOD line 469，并更新 C22 修复方案与之一致 |
| Q6（中等—C04+E02+C23 多修复项耦合副作用未分析） | 接受。已新增「跨问题耦合副作用分析」章节，详细分析 C04（@Transactional）× E02（唯一约束冲突）× S04（并发控制）三者的相互约束：事务边界与并发隔离方案（@Lock(PESSIMISTIC_WRITE)）、C23 session 修改时序与事务边界交互的可能性评估、以及四步修复实施顺序 |
| Q7（一般—C14 检查与执行时序不精确） | 接受。已在 C14 中补充精确执行顺序：(1) 补偿前先判断 retryCount >= maxRetryCount；(2) 已达上限直接 EXPIRED；(3) 未达上限则执行补偿；(4) 补偿失败递增 retryCount；(5) 成功设 COMPENSATED |
| Q8（一般—C23 时序说明仍较抽象） | 接受。已在 C23 中补充精确时序表，标注每个操作在 `TriageServiceImpl.java` 中的行号范围：[1] 行 72-80 session 修改 → [2] 行 82-83 AI 调用 → [3] 行 95-108 处理 AI 结果 → [4] 行 134/141 saveTriageRecord（事务内）→ [5] 事务提交后 setAiFailCount(0) → [6] 事务提交后 setCorrectedChiefComplaint() |
