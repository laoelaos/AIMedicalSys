# 诊断报告 — Phase 23 C/3/DE 验收问题定位（v6）

## 优先级分组说明

- **P0**（必须立即修复）：阻塞业务流程或导致数据损坏的问题，上线前必须解决
- **P1**（严重影响业务逻辑）：影响功能正确性或业务规则完整性的问题，应在迭代内解决
- **P2**（可并行修复）：语义、日志、配置、文档等不影响核心流程的问题，可与 P0/P1 并行

---

## P0 — 必须立即修复

### C01 — TriageRecord 实体缺失 correctedChiefComplaint 快照字段
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageRecord.java:14-144` 无 `correctedChiefComplaint` 字段；OOD §3.1 明确要求"增加 correctedChiefComplaint 快照字段"，该字段是进程崩溃后从数据库恢复 DialogueSession 主诉修正状态的唯一持久化副本

### C02 — TriageRecordRepository 缺少 findTopBySessionIdOrderByTriageTimeDesc 恢复查询方法
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageRecordRepository.java:11-17` 有 `findBySessionId`（行 13），但缺少 OOD §3.1（line 462）要求新增的 `findTopBySessionIdOrderByTriageTimeDesc(String sessionId)`（按分诊时间排序取最新记录）。该方法用于进程崩溃后从数据库恢复 DialogueSession 快照字段，仅有 `findBySessionId` 无法获取"最新"分诊记录

### C04 — TriageServiceImpl.triage/selectDepartment/saveTriageRecord 缺少 @Transactional；事务边界风险需在修复时严格控制
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageServiceImpl.java:34-35` 类级别无 `@Transactional`，`triage()` `selectDepartment()` `saveTriageRecord()` 均无事务注解。OOD §3.1 "先写数据库再更新内存"策略依赖事务保证——事务提交成功后 DialogueSession 才更新，数据库写入失败时回滚使 session 保持未更新状态
- **事务边界风险说明（修复时必须控制）**：(a) 对 `triage()` 加 `@Transactional` 会使 AI 调用期间持有数据库连接（最长 8 秒），高并发时可能导致连接池耗尽；(b) `saveTriageRecord()` 是 private 方法，Spring AOP 无法拦截自调用；(c) OOD §3.1（line 453）要求事务边界仅包围 save() 操作。
- **推荐修复方案**：使用 TransactionTemplate 编程式事务仅包围 `triageRecordRepository.save()` 调用，原因：(1) 避免 @Transactional 在 private 方法上失效的 Spring AOP 自调用限制；(2) 事务边界精确可控（仅包围 save()，不覆盖 AI 调用）；(3) 无需额外抽取 public 方法破坏现有封装

### C06 / E03 — DoctorFacade 跨模块调用无 try/catch、无超时、无 WARN 日志（本质相同 — 合并修复策略）
- **合并修复策略**：在 `TriageServiceImpl.findDoctorsForDepartments()` 中增加统一 try/catch，捕获所有异常后记录 WARN 日志（含调用耗时、异常类型、departmentId），将 TriageResponse.doctors 置为空列表。超时通过 `consultation.doctor-facade.timeout=2s` 配置注入（`@Value`）并传递给 future.get()。合并原因：C06 与 E03 指向同一个代码位置和同一缺失行为。
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageServiceImpl.java:169-179` `findDoctorsForDepartments()` 直接调用 `doctorFacade.findAvailableDoctorsByDepartment()` 无 try/catch。OOD §4.1 要求"DoctorFacade 调用超时/异常时捕获并记录 WARN 日志，TriageResponse.doctors 置为空列表"

### C08 — selectDepartment 签名为 4 参、RegistrationEventListener 未调用该方法
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageService.java:10` 签名为 `selectDepartment(String sessionId, String departmentId, String departmentName, boolean overwrite)` 4 参；OOD §3.1（line 467）设计为 3 参（无 overwrite）。`RegistrationEventListener.java:38-44` 直接操作 repository 而非调用 `TriageService.selectDepartment()`
- **修复方向（对齐 OOD §3.1 line 467/469）**：`TriageService.selectDepartment` 接口改为 3 参移除 overwrite，Service 内部始终覆盖写入；`RegistrationEventListener` 调用前自行检查 `TriageRecord.finalDepartmentId` 是否为空，仅当为空时调用（事件写入仅在手动选科未发生时生效）；Controller 直接调用 3 参版本。C22（Controller 硬编码 overwrite=true）随此修复自动消除。

### C15 / E01 — RegistrationEventListener @Retryable 对所有 Exception.class 重试（本质相同 — 合并修复策略）
- **合并修复策略**：`@Retryable` 的 `retryFor` 限定为 `{DataAccessException.class, TimeoutException.class}`，增加 `noRetryFor={IllegalArgumentException.class, NullPointerException.class}`。合并原因：C15 与 E01 为同一行代码的同一问题在不同审查文档中的重复记录。
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`RegistrationEventListener.java:36` `@Retryable(retryFor = Exception.class, ...)`。OOD §3.1 要求"仅对 DataAccessException、TimeoutException 等可治愈临时异常触发重试"

### E02 — TriageRecord 同 sessionId 第二次分诊违反 @Column(unique = true) 约束
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageServiceImpl.java:185-216` `saveTriageRecord()` 始终 `new TriageRecord()` 执行 INSERT，不先查已有记录做 UPDATE。OOD §3.1 要求"同一 sessionId 第二次发起分诊时 update 覆盖同 sessionId 记录"——sessionId 标注 `@Column(unique = true)`，第二次 INSERT 将抛出 DataIntegrityViolationException

### M01 — 4 个错误码缺失
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`MedicalRecordErrorCode.java:5-9` 当前仅 4/8 个错误码，缺少 `MR_GEN_AI_UNAVAILABLE` / `MR_GEN_AI_INPUT_INVALID` / `MR_GEN_AI_OUTPUT_INCOMPLETE` / `MR_GEN_TEMPLATE_LOAD_FAILED`

### P01 — 异步 AI 调度机制未实现
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DedupTaskScheduler.java:35-41` `schedule()` 创建 PENDING 条目后立即返回，无任何代码触发 `AiService.prescriptionAssist()` 异步调用并回填结果。OOD §3.4 要求异步 AI 调用完成后更新 AiSuggestionResult 状态
- **修复方向**：在 `DedupTaskScheduler` 或新增异步协调类中实现异步 AI 调度机制——`schedule()` 创建 PENDING 条目后通过 `@Async` 或 `ThreadPoolTaskExecutor` 异步调用 `AiService.prescriptionAssist()`；调用完成后根据结果更新 AiSuggestionResult 状态为 COMPLETED（成功）或 FAILED（超时/异常）。此修复必须与 A03 同步实施，二者共同构成异步 AI 调度管线的完整实现。

### A02 — 所有 AI 调用超时完全未外化
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageServiceImpl.java:87` `future.get()` 无超时；`PrescriptionAuditServiceImpl.java:81` `future.get()` 无超时；`PrescriptionAssistServiceImpl.java:78` `future.get()` 无超时；`MedicalRecordServiceImpl.java:124,138` 硬编码 2s/12s。OOD §2.3 要求各操作均配置独立超时并通过 `@Value` 注入

### A03 — AiSuggestionResult 5 状态映射表全部未实现
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DedupTaskScheduler.java` 仅创建 PENDING 条目（无回填）；`PrescriptionAssistServiceImpl.java` checkDose 调用 `dedupTaskScheduler.schedule()` 后不启动异步 AI。OOD §3.4 要求异步 AI 调用管线完整：PENDING → COMPLETED/FAILED 状态映射 + consumed 标记
- **修复方向**：在异步 AI 调用完成后实现 5 状态映射：(1) PENDING → COMPLETED：AI 返回成功结果后设置 status=COMPLETED，写入返回数据，consumed=false；(2) PENDING → FAILED：AI 调用超时或异常时设置 status=FAILED，errorDetail 记录失败原因；(3) COMPLETED → consumed=true：业务层读取结果后标记 consumed，供 TTL 清理任务判别。此修复必须与 P01 同步实施。

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
- **证据**：`TriageServiceImpl.java:169-179` 无条件追加所有医生到列表，无排序、无限制取前 5。OOD §4.1 要求"按匹配评分排序取前 5 名"

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

### C14 — DeadLetterCompensationService 未检查 retryCount >= maxRetryCount
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DeadLetterCompensationService.java:42-46` `compensateDeadLetters()` 捕获异常后仅递增 retryCount，未判断 `retryCount >= maxRetryCount` 时将状态迁移到 `EXPIRED`。OOD §3.1 要求状态迁移规则
- **精确执行顺序**：(1) 补偿前先判断 `retryCount >= maxRetryCount`；(2) 已达上限则直接设 state=EXPIRED 并保存，跳过补偿；(3) 未达上限则执行补偿逻辑（`triageService.selectDepartment()`）；(4) 补偿失败时递增 retryCount 并保存（保持当前 state=FAILED）；(5) 补偿成功时设 state=COMPENSATED 并保存。

### P05 — SubmitResponse 缺 warnResult 字段；WARN 路径使用设计未定义的错误码 RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT
- **真实性**：真实存在
- **根因分类**：实现编码问题 + OOD 文档修改
- **[编码前先完成 OOD 文档修改]** OOD §1.1a（line 111）SubmitResponse 基础字段定义为 submitted/prescriptionOrderId/blockInfo/errorCode，warnResult 字段（含 riskLevel/alerts/auditRecordId/prescriptionHash）仅在 §4.6 响应 JSON 示例中出现（line 1268-1281）。warnResult 是 SubmitResponse 的扩展字段，但应将其从"示例级描述"提升为"正式字段定义"——在 OOD §1.1a SubmitResponse 定义中补充 warnResult 字段（WarnResult 类型，可选），并定义 WarnResult 类的子字段：
  - riskLevel（AuditRiskLevel 枚举，必填）
  - alerts（List\<WarnAlert\>，每项含 alertCode/alertMessage/severity 三个字段，severity 为 AlertSeverity 枚举）
  - auditRecordId（Long，必填）
  - prescriptionHash（String，必填）
- **[OOD 文档确认后实施]** `SubmitResponse.java:1-44` 仅含 submitted/prescriptionOrderId/blockInfo/errorCode 四个字段，无 `warnResult` 字段。需要新增 `warnResult` 字段（WarnResult 类型）及对应的 DTO 类，实现 warnResult 的按需返回逻辑。同时，`PrescriptionAuditServiceImpl.java:205` 使用 `RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT`——此错误码在 OOD 任何位置均未定义（OOD §5.1 错误码表无此条目），且 WARN 路径不应通过 errorCode 承载风险语义。修复方向：WARN 路径不使用 errorCode，改为通过 SubmitResponse.warnResult 承载风险信息（riskLevel/alerts/auditRecordId/prescriptionHash），errorCode=null；RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT 错误码从代码侧移除，或保留用于 forceSubmit=true 校验失败且 warnResult 不可用时的兜底降级路径。

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

### C23 — 主流程 AI 输入准备阶段 session 修改的约束边界（说明性条目）
- **真实性**：真实存在（约束说明，非独立代码缺陷）
- **根因分类**：约束说明
- **说明**：`TriageServiceImpl.java:72-80` `session.setChiefComplaint()`/`setAdditionalResponses()`/`setRoundCount()` 是 AI 输入准备阶段必需前置操作，不可移至 AI 调用之后。OOD §3.1 "先写数据库再更新内存"策略覆盖的是 TriageRecord 持久化路径，不覆盖 AI 输入准备阶段的 session 修改。AI 返回后 `session.setCorrectedChiefComplaint(aiData.getCorrectedChiefComplaint())` 回写缺失已在 C03/A04 中捕获。C23 无独立代码修改，作为 correctedChiefComplaint 数据流群组（见跨问题耦合分析）的完整性验收约束。

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
- **证据**：`TriageController.java:34` `triageService.selectDepartment(sessionId, departmentId, departmentName, true)` 硬编码 overwrite=true。随 C08 修复（接口改为 3 参移除 overwrite）一并消除。

### P09 — PrescriptionItem.unit 字段为 OOD 未正式定义的扩展字段
- **真实性**：真实存在
- **根因分类**：OOD 文档修改 + 实现编码问题
- **[编码前先完成 OOD 文档修改]** OOD §3.2 PrescriptionItem 核心 6 字段（drugId/drugName/dose/frequency/duration/route）未包含 unit。unit 字段在剂量校验链路中具有实质业务需求——`DosageCheckRequest`（OOD §1.1b 定义）已包含 `unit` 字段（line 145），用于单位一致性校验（OOD §8.3 DosageUnitGroup 分组换算）。PrescriptionItem 作为处方药品条目（同时用于审核请求 `AuditRequest.prescriptionItems` 和提交请求 `SubmitRequest.prescriptionItems`），若缺少 unit，在审核/提交流程中无法进行准确的跨组单位换算和超限判定。OOD 应在 PrescriptionItem 定义中补充 unit（String，可选），并在 §8.3 单位一致性校验中说明其用途。DosageCheckRequest（单药品即时校验）与 PrescriptionItem（多药品处方条目）使用场景不同但 unit 的业务角色一致——用于 DosageUnitGroup 分组换算。
- **需要添加 unit 的 OOD 完整位置清单**：(1) §1.1a SubmitRequest/SubmitResponse 中的 PrescriptionItem 引用（line 110，字段表描述"每项含 drugId/drugName/dose/frequency/duration/route"——应追加 unit）；(2) §3.2 PrescriptionItem 正式定义——AuditRequest 中的 prescriptionItems（line 101/618）、PrescriptionAssistResponse 中的 drugs 数组（line 144/809，每项含 drugId/drugName/dose/frequency/duration/route）；(3) §4.6 提交流程中的 PrescriptionItem 引用（line 928 请求 DTO 描述、line 1193/1226/1247/1259 响应 JSON 示例中的 prescriptionItems 数组）；(4) §8.3 单位一致性校验中的字段说明（line 1652 DosageStandard.unit 字段定义——此处已在说明 unit，需补充与 PrescriptionItem.unit 的映射关系）。
- **[可编码并行]** `PrescriptionItem.java:11` 已有 `unit` 字段（当前属于 OOD 未定义的扩展字段）。OOD 正式补充后，unit 字段在 PrescriptionItem 中保持现有实现即可，无需额外代码修改。

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

以下各组修复项之间存在相互约束，必须协调实施顺序和设计决策：

### A07（AiResult.success(data) 不校验 data=null）× A09（Converter 空降级）× A11（业务层冗余防御判空）

**问题**：A07（success(data) 缺少 data=null 断言）、A09（toAuditResponse 在 aiData==null 时退化 PASS）、A11（业务层普遍 `&& getData() != null` 防御检查）三者指向同一根因修复——在 `AiResult.success(data)` 中增加 `Objects.requireNonNull(data)` 断言。当前耦合分析只覆盖了 A07×A09，遗漏 A11 导致以下风险：

1. **重复分配**：A07 和 A11 的根因修复完全相同，可能被分配给不同开发者导致同一处代码修改两次。

2. **修复顺序风险**：A09 的调用方前置检查（PrescriptionAuditServiceImpl 在调用 toAuditResponse() 前拦截 null）当前依赖业务层存在 `getData() != null` 防御检查作为兜底。若 A11 先于 A09 移除业务层判空且 A09 降级路径尚未就绪，在 A07 断言生效前 PrescriptionAuditServiceImpl 会短暂暴露 NPE 风险。

3. **约定的修复实施顺序**：(1) 先修复 A09——PrescriptionAuditServiceImpl 在调用 toAuditResponse() 前检查 `aiResult.isSuccess() != true || aiResult.getData() == null`，走降级路径；(2) 再修复 A07——在 `AiResult.success(data)` 中增加 `Objects.requireNonNull(data)` 断言；(3) 最后修复 A11——断言生效后可安全移除业务层所有 `&& getData() != null` 冗余检查。A11 不可先于 A09 步骤(1)执行。AiResult.failure() 的 partialData 可能为 null，不受 success() 断言约束，无需修改。

### C01 × C03 × C19 × A04 × C23 — correctedChiefComplaint 数据流群组

**问题**：C01、C03、C19、A04、C23 五条问题均涉及 correctedChiefComplaint 的同一数据流链路：AI 返回 → session 回写 → TriageRecord 持久化 → 进程崩溃恢复。

**耦合关系与修复顺序**：
1. **C01（P0）**：在 TriageRecord 实体中新增 `correctedChiefComplaint` 快照字段——这是后续所有持久化操作的前提。
2. **A04（P1）**：修复 `TriageConverter.toAiTriageRequest()` 显式透传 `session.getCorrectedChiefComplaint()` 至 AI 请求——确保下一次 AI 调用携带前序修正结果。
3. **C03（P1）**：在 `TriageConverter.toTriageResponse()` 中将 AI 返回的 `correctedChiefComplaint` 写入 `DialogueSession`——实现隐式主诉修正回写。
4. **C19（P1）**：在 `saveTriageRecord()` 中将 `session.getCorrectedChiefComplaint()` 写入 `TriageRecord.correctedChiefComplaint`——实现持久化快照。
5. **C23（P2，说明性条目）**：验证上述数据流完整性的约束——AI 输入准备阶段的 session 修改（行 72-80 setChiefComplaint/setAdditionalResponses/setRoundCount）是必需前置操作不可移动；AI 返回后持久化阶段的 session 更新（setAiFailCount(0)、setCorrectedChiefComplaint）确保持久化事务提交后执行。C23 不涉及独立代码修改，作为修复完成后的验收约束。

### C04（@Transactional 事务边界）× E02（唯一约束冲突）× S04（并发控制）

**问题**：C04 要求在 `saveTriageRecord()` 外包事务，E02 要求同 sessionId 第二次分诊时 UPDATE 而非 INSERT（避免唯一约束冲突），S04 要求同 session 并发访问串行化。三者存在以下耦合：

1. **事务边界与并发隔离**：若事务边界过宽（覆盖 AI 调用），则 AI 调用期间持有数据库连接，高并发下连接池耗尽；若事务边界仅包围 save()，则 E02 的 UPDATE 操作在并发场景下需要 `@Lock(PESSIMISTIC_WRITE)` 防止不可重复读或幻读——虽然 `@Column(unique = true)` 提供了唯一约束保障，但 UPDATE 在并发写入相同 sessionId 时仍可能在应用层读到旧数据后写回错误值。建议：`findBySessionId` 查询使用 `@Lock(PESSIMISTIC_WRITE)`，事务边界仅包围 save 操作（使用 TransactionTemplate 编程式事务）。
   - **悲观锁性能影响评估**：`@Lock(PESSIMISTIC_WRITE)` 对 `findBySessionId` 加行级写锁，同一 sessionId 的并发分诊/选科操作在事务提交前阻塞。由于 sessionId 绑定单一患者会话，同一 sessionId 的并发冲突概率极低——同一患者不会同时发起两次分诊。悲观锁的实际争用频率低，等待时长取决于 save() 事务耗时（ms 级），不会出现秒级阻塞，对吞吐量影响可忽略。
   - **替代方案评估**：(a) OPTIMISTIC 锁 + 重试：需在 TriageRecord 增加 `@Version` 字段，UPDATE 时通过版本号检测并发修改，冲突时抛 `ObjectOptimisticLockingFailureException`，需调用方实现重试逻辑——增加复杂度且重试期间 session 状态可能已变化。(b) 应用层队列串行化：按 sessionId 哈希分发到单线程执行器消除并发——引入异步复杂度和 session 状态一致性问题。**结论**：当前场景（低冲突概率、短事务）下 `@Lock(PESSIMISTIC_WRITE)` 是最简方案，OPTIMISTIC 锁可作为长期演进选项。

2. **C23 session 修改时序与 C04 事务边界的交互**：AI 输入准备阶段的 session 修改（行 72-80）在持久化事务外执行，不受回滚保护——若 AI 调用成功但 saveTriageRecord 失败回滚，session 中已写入的 chiefComplaint/additionalResponses 不会自动回退。此场景下 session 处于"已修改但未持久化"的不一致状态。这是 OOD §3.1 策略有意接受的边界情况——下一次同 session 请求时，session 中的值被重新覆盖（行 72-80 始终从 request 读取最新值写入），不依赖 session 持久化后的一致性。

3. **修复实施顺序**：
   - 第一步：使用 TransactionTemplate 编程式事务仅包围 `triageRecordRepository.save()` 调用，确保 AI 调用不在事务内。
   - 第二步：在 `saveTriageRecord()` 中实现 UPDATE 逻辑（先查 `findBySessionId`，存在则 update，不存在则 insert），替代当前始终 `new TriageRecord()` 的 INSERT 路径。
   - 第三步：在 `findBySessionId` 查询上加 `@Lock(PESSIMISTIC_WRITE)` 防止并发 UPDATE 冲突。
   - 第四步：`DialogueSessionManager` 的 `createSession` 使用 `putIfAbsent` 替代 `put`，确保同 session 串行创建。

### C06/E03（DoctorFacade try/catch 缺失）× C17（排序取前 5 缺失）

**问题**：C06（无 try/catch）与 C17（无条件追加医生、无排序取前 5）均位于 `TriageServiceImpl.findDoctorsForDepartments()` 同一方法（行 169-179），修复时需一并处理。

**耦合关系**：若先修复 C17（排序+取前 5）但未修复 C06（无 try/catch），则 doctorFacade 调用抛异常时排序逻辑提前中断，C17 修复不可达；若先修复 C06（try/catch 包裹）但未修复 C17，则降级路径返回全量无序列表。**约定的修复方式**：在 `findDoctorsForDepartments()` 方法内一次性完成——(a) 外层 try/catch 包裹 doctorFacade 调用；(b) catch 中记录 WARN 日志 + 返回空列表；(c) 正常路径中对返回的医生列表按 score 排序，取前 5 名。此三项修改在同一方法内，建议作为单次提交完成。

### P02/E06（DrugFacade 注入缺失）× P06（降级路径不转换 LocalRuleResult）× P04/E04（缓存失效事件缺失）

**问题**：此三项功能位于 prescription 模块的不同 Service 和配置类中，分别涉及审核流程、降级逻辑和缓存管理，**无顺序约束依赖**。

**无耦合确认**：(1) P02（DrugFacade 注入）影响审核/辅助开方流程中获取药品外部数据，P06（降级路径转换）影响 AuditConverter 在 AI 不可用时的输出格式，二者不存在先后依赖——即使 DrugFacade 注入在降级路径上线后才完成，降级路径仍可使用空药品信息工作；(2) P04（缓存失效事件）影响 Caffeine 缓存的即时性，与 P02（外部数据来源）和 P06（输出格式转换）均无共享状态。**建议**：三项可并行修复，由不同开发者同时实施，无需协调顺序。

### A02（AI 调用超时未外化）× A10（application.yml 配置键缺失）

**问题**：A02 要求将 `future.get()` 的超时参数通过 `@Value` 从配置注入，A10 要求 application.yml 中存在对应的配置键和默认值。两项存在明显的先后依赖关系：A10 必须先行（或同期）提供配置键和默认值，A02 才能通过 `@Value` 成功注入。

**约定的实施顺序**：(1) 先（或同步）在 application.yml 中添加所有 ai.timeout.* 和 facade.*.timeout 配置键及默认值（见 A10 完整列表）；(2) 再在各 Service 中通过 `@Value("${ai.timeout.triage:8s}")` 等形式注入并传递给 future.get(timeout, TimeUnit)。两步不可倒置——若先修改代码添加 @Value 注解但配置文件中无对应键，Spring 启动时抛 MissingEnvironmentVariableException（除非提供了 `:` 后的默认值兜底）。建议在同一迭代中作为同一开发任务的两个子步骤完成，先配置后代码。

- **优先级不匹配风险**：A02 为 P0（必须立即修复），A10 为 P2（可并行修复）。若 A10 因 P2 优先级被长期搁置，`ai.timeout.*` 和 `facade.*.timeout` 共 7 个配置键不会写入 application.yml——A02 即使通过 `@Value` 的 `:` 默认值兜底避免启动异常，`future.get()` 仍使用硬编码默认值而非可配置值，修复效果被架空。**缓解方案**：将 A10 中上述 7 个配置项标注为「建议与 A02 同迭代完成」，在任务分配中明确标为 A02 的前置子步骤，不因 A10 的 P2 优先级而被推迟。

### P01（异步 AI 调度未实现）× A03（状态映射表未实现）

**问题**：P01 和 A03 共同指向 `DedupTaskScheduler` 的异步 AI 调度管线，是同一段缺失业务逻辑的两个视图。若只修复 P01（增加异步调用触发）而未实现 A03 的状态映射，则异步调用完成后结果无处回填，AiSuggestionResult 永远停在 PENDING；若只修复 A03（状态映射 + consumed 标记）而未修复 P01，则状态机虽有定义但无触发入口。两者必须同步实施。

**约定的修复实施顺序**：(1) 在 DedupTaskScheduler 或新增异步协调类中实现异步 AI 调用触发机制（P01 载荷）；(2) 在 AI 调用回调中实现 COMPLETED/FAILED 状态回填和 consumed 标记（A03 载荷）。两项作为同一开发任务单次提交完成，不可拆分给不同开发者或不同迭代。

---

## 修订说明（v6）

| 质询意见 | 回应 |
|---------|------|
| C02 — 证据描述存在事实误差：report 称"只有 findTopByPatientIdOrderByTriageTimeDesc，无 sessionId 版本"，但实际代码行 13 已存在 findBySessionId | 接受。已将证据修正为"TriageRecordRepository.java:11-17 有 findBySessionId（行 13），但缺少 OOD §3.1（line 462）要求新增的 findTopBySessionIdOrderByTriageTimeDesc(String sessionId)"，精确描述真实缺失的方法 |
| C23 — "恢复前序修改"表述存在逻辑矛盾：同时称 session 修改是"必需前置操作"和需要"恢复前序修改" | 接受。已将"实际根因"中"利用 session 快照字段恢复前序修改"移除，改为明确区分两类 session 修改的约束边界：AI 输入准备阶段（不可移动）与持久化后更新阶段（受策略约束），并说明 C23 不构成需独立修复的缺陷（AI 返回后 correctedChiefComplaint 回写缺失已由 C03/A04 捕获） |
| C08 — 修复建议篇幅过度详细，与 P1 优先级不协调 | 接受。已(1)将 C08 升级至 P0（对齐 Issue #6 优先级调整建议）；(2)精简修复方向描述，移除详细 (a)(b)(c)(d) 子步骤分解、时序假设前提、防御性处理、C22 调和说明段落；C22 修复随 C08 一并标注 |
| C04 — 修复方案未明确推荐偏好 | 接受。已明确推荐使用 TransactionTemplate 编程式事务仅包围 triageRecordRepository.save() 调用，并给出三条理由 |
| 跨问题耦合分析覆盖不全：(a) C06/E03 × C17 同一方法调用；(b) P02/E06 × P06 × P04/E04 无顺序约束；(c) A02 × A10 存在先后依赖 | 接受。已补充三组分析：C06×C17 建议单次提交一次性修复；P02×P06×P04 无顺序约束可并行；A02×A10 先配置后代码的明确顺序 |
| 部分 P1 条目有提升至 P0 的潜力：C01、M01、C08 | 接受。已将其余 P1 条目中影响面符合 P0 标准的 C01（缺少 correctedChiefComplaint 快照字段，阻塞业务恢复路径）、M01（4 个错误码缺失，导致病历模块无法区分失败原因）、C08（接口签名错误 + EventListener 绕过，影响选科流程正确性）上调至 P0 |
| P05/P09 的 [OOD 文档修改] 子项未独立标注优先级 | 接受。已为 P05 和 P09 的 [OOD 文档修改] 子项添加"[编码前先完成 OOD 文档修改]"标注，[代码修改] 子项标注"[可编码并行]" |

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
| Q4（严重—C04 修复建议未分析事务边界风险） | 接受。已在 C04 中补充事务边界风险分析：(a) 对 triage() 加 @Transactional 会使 AI 调用期间持有数据库连接 8 秒，高并发时连接池耗尽；(b) saveTriageRecord() 是 private 方法，Spring AOP 无法拦截自调用；(c) OOD §3.1（line 453）要求事务边界仅包围 save() 操作。修复方案：使用 TransactionTemplate 编程式事务仅包围 save() 调用 |
| Q5（中等—C08+C22 合并修复方案与 OOD 设计矛盾） | 接受。OOD §3.1（line 469）明确设计为"EventListener 自行判断 finalDepartmentId 后决定是否调用 selectDepartment"，而非"Service 内部根据来源决定"。已修正 C08 修复方向全量对齐 OOD line 469，并更新 C22 修复方案与之一致 |
| Q6（中等—C04+E02+C23 多修复项耦合副作用未分析） | 接受。已新增「跨问题耦合副作用分析」章节，详细分析 C04（@Transactional）× E02（唯一约束冲突）× S04（并发控制）三者的相互约束：事务边界与并发隔离方案（@Lock(PESSIMISTIC_WRITE)）、C23 session 修改时序与事务边界交互的可能性评估、以及四步修复实施顺序 |
| Q7（一般—C14 检查与执行时序不精确） | 接受。已在 C14 中补充精确执行顺序：(1) 补偿前先判断 retryCount >= maxRetryCount；(2) 已达上限直接 EXPIRED；(3) 未达上限则执行补偿；(4) 补偿失败递增 retryCount；(5) 成功设 COMPENSATED |
| Q8（一般—C23 时序说明仍较抽象） | 接受。已在 C23 中补充精确时序表，标注每个操作在 `TriageServiceImpl.java` 中的行号范围：[1] 行 72-80 session 修改 → [2] 行 82-83 AI 调用 → [3] 行 95-108 处理 AI 结果 → [4] 行 134/141 saveTriageRecord（事务内）→ [5] 事务提交后 setAiFailCount(0) → [6] 事务提交后 setCorrectedChiefComplaint() |

## 修订说明（v7）

| 质询意见 | 回应 |
|---------|------|
| 问题 1（中等）：C23 优先级 P0 与问题性质不匹配 | 接受。已将 C23 从 P0 章节移除：(1) 降级至 P2 作为「说明性条目」，标题标注"说明性条目"避免误认为需独立编码修改；(2) 在跨问题耦合分析中新增 C01×C03×C19×A04×C23 群组，C23 在该群组中作为完整性验收约束 |
| 问题 2（一般）：correctedChiefComplaint 数据流链路未纳入耦合分析群组 | 接受。已在耦合分析章节新增 C01×C03×C19×A04×C23 耦合群组，约定修复顺序：(1) C01 新增实体字段 → (2) A04 显式透传 → (3) C03 隐式回写 → (4) C19 将 session 值写入实体 → (5) C23 作为说明性约束验证完整性 |
| 问题 3（一般）：P05 的"可编码并行"标注与实际依赖矛盾 | 接受。已将 P05 代码修改子项标注从「[可编码并行]」修正为「[OOD 文档确认后实施]」，明确代码侧实施前提是 OOD 文档已正式定义 WarnResult 子结构 |

## 修订说明（v8）

| 质询意见 | 回应 |
|---------|------|
| 问题 1（中等）：P01 和 A03 两个 P0 条目缺失修复建议 | 接受。已在 P01 添加修复方向（@Async/ThreadPoolTaskExecutor 异步触发 AiService.prescriptionAssist()，完成后更新状态）；在 A03 添加修复方向（5 状态映射表：PENDING→COMPLETED/FAILED 回填 + consumed 标记）。两条目均标注「必须与对方同步实施」 |
| 问题 2（一般）：A02×A10 优先级划分隐含矛盾 | 接受。已在 A02×A10 耦合分析中追加「优先级不匹配风险」警告段落：若 A10 因 P2 被搁置则 7 个配置键永不写入 application.yml，A02 修复效果被架空。缓解方案：将 A10 中 ai.timeout.* 和 facade.*.timeout 共 7 个配置项标注为「建议与 A02 同迭代完成」，作为 A02 前置子步骤不因 P2 优先级推迟 |
| 问题 3（一般）：P01×A03 异步 AI 管线耦合未分析 | 接受。已新增 P01×A03 耦合分析章节，说明双向依赖（P01 无 A03 则结果无回填、A03 无 P01 则状态机无触发），约定作为同一开发任务单次提交完成 |
| 问题 4（一般）：A07×A11 重复修复未被关联 | 接受。已将 A07×A09 耦合分析扩展为 A07×A09×A11：(1) 根因修复完全相同（Objects.requireNonNull(data) 断言）；(2) 警示 A11 先于 A09 移除判空导致的 NPE 暴露窗口；(3) 修复顺序扩展为三步：A09→A07→A11，明确 A11 不可先于 A09 执行 |
| 问题 5（轻微）：C04 PESSIMISTIC_WRITE 建议缺少性能影响评估 | 接受。已在 C04×E02×S04 耦合分析的事务边界子项中追加悲观锁性能评估（低冲突概率、ms 级等待、吞吐量影响可忽略）和替代方案分析（OPTIMISTIC 锁+重试、应用层队列串行化），结论为当前场景下 PESSIMISTIC_WRITE 最简方案 |
