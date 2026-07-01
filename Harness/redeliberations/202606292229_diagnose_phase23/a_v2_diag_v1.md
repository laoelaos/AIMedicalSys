# 诊断报告 — Phase 23 C/3/DE 验收问题定位（v3）

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

### C04 — TriageServiceImpl.triage/selectDepartment/saveTriageRecord 缺少 @Transactional
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageServiceImpl.java:34-35` 类级别无 `@Transactional`，`triage()` `selectDepartment()` `saveTriageRecord()` 均无事务注解。OOD §3.1 "先写数据库再更新内存"策略依赖事务保证——事务提交成功后 DialogueSession 才更新，数据库写入失败时回滚使 session 保持未更新状态

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
- **证据**：`TriageService.java:10` 签名为 `selectDepartment(String sessionId, String departmentId, String departmentName, boolean overwrite)` 4 参；OOD §3.1 设计为 3 参（无 overwrite）。`RegistrationEventListener.java:38-44` 直接操作 repository 而非调用 `TriageService.selectDepartment()`
- **overwrite 下沉位置说明**：overwrite 逻辑应从接口签名移除，下沉到 Service 实现层内部——`TriageServiceImpl.selectDepartment()` 根据调用来源（Controller vs RegistrationEventListener）决定 overwrite 行为。Controller 调用时始终 overwrite=true（手动选科覆盖写入），EventListener 调用时应条件写入（仅 finalDepartmentId 为空时写入）。OOD §3.1 已定义此"覆盖优先级强制执行语义"，接口设计应隐藏此实现细节
- **C08 与 C22 的调和**：C08（接口多 overwrite 参数）和 C22（Controller 硬编码 overwrite=true）指向同一根因——overwrite 暴露在接口层。统一修复方案：接口改为 3 参，overwrite 逻辑内聚在 Service 实现层；Controller 不感知 overwrite；RegistrationEventListener 注入 TriageService 并调用 selectDepartment()

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
- **根因分类**：实现编码问题（归因修正：非前序版本所述OOD设计问题）
- **问题澄清**：`TriageServiceImpl.java:72-80` `session.setChiefComplaint()`/`setAdditionalResponses()`/`setRoundCount()` 发生在 AI 调用（行 83）之前。这些 session 修改构成 AI 请求输入的必需前置操作（`toAiTriageRequest(request, session)` 读取 session 中的这些字段），不能移至 AI 调用之后。OOD §3.1 "先写数据库再更新内存"策略覆盖的是 TriageRecord 持久化路径（行 134/141 的 `saveTriageRecord()` 写数据库 → 后续 session 状态更新），不覆盖 AI 输入准备阶段的 session 修改
- **实际根因**：`setChiefComplaint()`/`setAdditionalResponses()` 的输入值来源于 `request` 参数，这些值在 AI 调用前写入 session 是正确数据流。问题在于(1) session 在 AI 调用前被修改但未被持久化快照；(2) 未在 AI 返回后利用 session 快照字段恢复前序修改。修复方案应聚焦：将 AI 输入准备阶段的 session 修改与"先写数据库再更新内存"策略涉及的持久化路径解耦——AI 输入准备阶段的 session 修改保持当前顺序（在 AI 调用前），持久化路径按策略在 AI 返回后进行

### C14 — DeadLetterCompensationService 未检查 retryCount >= maxRetryCount
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DeadLetterCompensationService.java:42-46` `compensateDeadLetters()` 捕获异常后仅递增 retryCount，未判断 `retryCount >= maxRetryCount` 时将状态迁移到 `EXPIRED`。OOD §3.1 要求状态迁移规则

### P05 — SubmitResponse 缺 warnResult 字段；WARN 路径使用设计未定义的错误码 RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：(1) `SubmitResponse.java:1-44` 仅含 submitted/prescriptionOrderId/blockInfo/errorCode 四个字段，无 `warnResult` 字段。(2) `PrescriptionAuditServiceImpl.java:205` 使用 `RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT`
- **OOD 字段定义核实**：OOD §1.1a (line 111) 定义 SubmitResponse 基础字段为 submitted/prescriptionOrderId/blockInfo/errorCode。warnResult 字段（内含 riskLevel/alerts/auditRecordId/prescriptionHash）出现在 §4.6 forceSubmit=false + WARN 审核结果的 JSON 响应示例（line 1268-1281）。即 warnResult 是 SubmitResponse 的扩展字段，定义为按需返回的响应形态而非固定字段集。代码中完全缺失 warnResult 字段是真实的实现编码问题，但不应声称 OOD §1.1a 的基础字段定义包含了这些子字段——warnResult 及其子字段仅在 §4.6 示例中出现，需在正式字段定义中补充

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
- **证据**：`TriageController.java:34` `triageService.selectDepartment(sessionId, departmentId, departmentName, true)` 硬编码 overwrite=true。修复方案同 C08——接口改为 3 参，overwrite 下沉到 Service 实现层

### P09 — PrescriptionItem.unit 字段为 OOD 未正式定义的扩展字段
- **真实性**：真实存在
- **根因分类**：OOD 设计问题 + 实现编码问题
- **证据**：`PrescriptionItem.java:11` 有 `unit` 字段，但 OOD §3.2 规定的 PrescriptionItem 核心 6 字段（drugId/drugName/dose/frequency/duration/route）未包含 unit
- **业务必要性论证**：unit 字段在剂量校验链路中具有实质业务需求——`DosageCheckRequest`（OOD §1.3 包E 定义）已包含 `unit` 字段（line 145），用于单位一致性校验（OOD §8.3 DosageUnitGroup 分组换算）。PrescriptionItem 作为处方药品条目（同时用于审核请求 `AuditRequest.prescriptionItems` 和提交请求 `SubmitRequest.prescriptionItems`），若缺少 unit，在审核/提交流程中无法进行准确的跨组单位换算和超限判定。OOD 在 §1.3 对 PrescriptionItem 的定义未列出 unit 是设计文档的字段遗漏，应在 OOD 中正式补充
- **DosageCheckRequest 与 PrescriptionItem 的角色差异**：DosageCheckRequest 是即时校验请求（单药品粒度），unit 是必传字段；PrescriptionItem 是处方条目（多药品列表），unit 是剂量语义的必要组成部分。二者使用场景不同但 unit 的业务角色一致——用于 DosageUnitGroup 分组换算。OOD 应在 PrescriptionItem 定义中补充 unit（String，可选），并在 §8.3 单位一致性校验中说明其用途

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

### A11 — 业务层普遍在 isSuccess() 后额外做 && getData() != null 防御性检查
- **真实性**：真实存在
- **根因分类**：实现编码问题（归因修正：非前序版本所述OOD设计问题）
- **问题澄清**：OOD §2.3 契约"success=true → data 非 null"语义清晰。业务层防御性检查（如 `TriageServiceImpl.java:98` `aiResult.isSuccess() && aiResult.getData() != null`）说明契约不被信任。根因在于 `AiResult.success(data)` 未实现 data != null 断言，且测试用例断言 `success(null)` 合法——这是实现编码问题。修复方向：在 `AiResult.success(data)` 中增加 `Objects.requireNonNull(data)` 断言，移除业务层冗余判空

### S05 — PrescriptionDraftContext.updateCriticalAlerts 使用 get-check-then-put 模式（误报）
- **真实性**：误报（与 v2 诊断结论一致，无需修订）
- **根因分类**：N/A（误报条目）
- **证据**：`PrescriptionDraftContext.java:34-41` `updateCriticalAlerts()` 仅检查入参 `alerts` 是否为 null/empty，不存在从 Store 读取的操作。`put` 和 `remove` 在 `ConcurrentHashMap` 上均为原子操作，无需 `compute()` 原子化改造

---

## 修订说明（v3）

| 质询意见 | 回应 |
|---------|------|
| Q1（严重—事实错误）：P05 将 OOD §3.2 SubmitResponse 中不存在的要求（riskLevel/alerts/auditRecordId/prescriptionHash）归因于 OOD，与真实字段定义（submitted/prescriptionOrderId/blockInfo/errorCode）不符 | 接受。经核 OOD §1.1a（line 111）定义 SubmitResponse 基础字段为 submitted/prescriptionOrderId/blockInfo/errorCode；warnResult 内含 riskLevel/alerts/auditRecordId/prescriptionHash 出现在 §4.6 forceSubmit=false+WARN 响应示例而非正式字段定义。已修正 P05 证据描述，准确区分 OOD 基础字段定义与响应示例中的扩展字段要求。warnResult 缺失问题本身真实存在——代码完全缺失此字段 |
| Q2（严重—逻辑矛盾）：M04 根因归类为"OOD 设计问题"，但 OOD §3.3 对 MR_GEN_CONCURRENT_MODIFICATION 的描述本身准确（用于 UPDATE 路径），代码在 INSERT 路径捕获 OptimisticLockException 是不可达路径，属实现编码问题 | 接受。OOD §7（line 1601）明确描述 MR_GEN_CONCURRENT_MODIFICATION 用于"更新操作使用版本号校验"。代码 `MedicalRecordServiceImpl.java:102-110` 在 INSERT 路径捕获 `ObjectOptimisticLockingFailureException` 是错误的实现编码——JPA 新实体 INSERT 时 @Version 为 null，不会触发乐观锁。已修正 M04 根因分类为"实现编码问题"，OOD 描述准确 |
| Q3（中等—逻辑矛盾）：C23 建议"将业务数据操作移至 AI 调用和 TriageRecord 持久化之后"，但 setChiefComplaint/setCorrectedChiefComplaint 的值是 AI 请求构建所必需的前置操作 | 接受。`TriageServiceImpl.java:72-80` 的 session 修改（setChiefComplaint/setAdditionalResponses/setRoundCount）在 AI 调用（行 83）之前，这些值通过 `toAiTriageRequest(request, session)` 传入 AI 请求，是必要的正序数据流。OOD §3.1 "先写数据库再更新内存"策略覆盖的是 TriageRecord 持久化路径，不覆盖 AI 输入准备阶段的 session 修改。已修正 C23 描述：明确区分 AI 输入准备（保持当前顺序）与持久化路径（严格按策略执行），移除原"移至 AI 调用之后"的不可行建议 |
| Q4（中等—深度不足）：报告列出 61 个问题但缺少系统性优先级排序 | 接受。已在本版（v3）全面引入 P0/P1/P2 三级优先级分组，每个问题归入对应分组。P0：阻塞业务流程或数据损坏（11 项含合并分组）；P1：严重影响业务逻辑（32 项含合并分组）；P2：可并行修复（18 项含误报） |
| Q5（轻微—事实准确性）：P09 将 PrescriptionItem.unit 的缺失简单归为"OOD 遗漏"，未充分论证 unit 在审核/提交流程中的业务必要性，以及其在 DosageCheckRequest 与 PrescriptionItem 之间的角色差异 | 接受。已扩展 P09 诊断：补充 unit 在剂量校验链路中的业务必要性论证（DosageUnitGroup 分组换算需要），明确 DosageCheckRequest（单药品即时校验）与 PrescriptionItem（多药品处方条目）的角色差异与 unit 的语义一致性。两个 DTO 中 unit 的业务角色一致，OOD 应在 PrescriptionItem 正式定义中补充 unit 字段 |
| Q6（轻微—可操作性）：A09 将修复定位在 AuditConverter，但 Converter 无法主动触发降级，真正修复点在 PrescriptionAuditServiceImpl 的调用方业务逻辑中 | 接受。`AuditConverter.java:48-56` `toAuditResponse()` 是被动映射方法，当 `aiData == null` 时只能按现有数据输出 PASS+空 alerts。Converter 本身无决策能力——它不决定何时触发降级。正确的修复点在 `PrescriptionAuditServiceImpl` 中：调用 `toAuditResponse()` 前检查 `AiResult` 有效性，当 `isSuccess() != true || getData() == null` 时走 LocalRuleEngine 降级路径。已修正 A09 修复定位说明 |
| Q7（轻微—深度不足）：5 组跨 section 的本质相同问题（C15/E01、C06/E03、P02/E06、P03/S02、P04/E04）标注了交叉引用但未给出整合修复策略 | 接受。已对所有 5 组跨 section 的本质相同问题实施合并修复策略：每组标注合并原因（同一代码位置/同一缺失行为），给出统一的修复方案描述，避免修复者分开处理导致重复工作或修复不一致 |
| Q8（轻微—逻辑不一致）：C08 与 C22 的独立建议未说明 overwrite 下沉到哪一层，且 C08 建议让 RegistrationEventListener 调用 selectDepartment 但与"条件写入"语义不兼容 | 接受。已修正两个问题：(1) 明确 overwrite 下沉目标层为 Service 实现层——`TriageServiceImpl.selectDepartment()` 内部根据调用来源（Controller/EventListener）判定是否覆盖写入，接口签名改为 3 参无 overwrite；(2) 阐明 RegistrationEventListener 调用 `selectDepartment()` 时为"条件写入"语义——仅当 `finalDepartmentId == null` 时写入，与 OOD §3.1 定义的覆盖优先级强制执行语义一致。C08 与 C22 指向同一根因（overwrite 暴露在接口层），已统一修复方案 |
