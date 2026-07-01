# 诊断报告 — Phase 23 C/3/DE 验收问题定位

## 一、Consultation 分诊模块

### C01 — TriageRecord 实体缺失 correctedChiefComplaint 快照字段
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageRecord.java`(14-144) 无 `correctedChiefComplaint` 字段；OOD §3.1 TriageRecord 设计明确要求"增加 chiefComplaint 原始主诉快照字段和 correctedChiefComplaint 快照字段"
- **修改建议**：修改编码实现（`AIMedical/`）— 在 `TriageRecord.java` 中增加 `correctedChiefComplaint` 字段并同步 DDL

### C02 — TriageRecordRepository 缺少 findTopBySessionIdOrderByTriageTimeDesc 恢复查询方法
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageRecordRepository.java`(11-17) 只有 `findTopByPatientIdOrderByTriageTimeDesc`，无 sessionId 版本；OOD §3.1 明确要求新增 `findTopBySessionIdOrderByTriageTimeDesc(String sessionId)`
- **修改建议**：修改编码实现 — 在 Repository 接口中增加此查询方法

### C03 — AI 隐式 correctedChiefComplaint 路径未实现
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageConverter.java:54-90` `toTriageResponse()` 从未读取 ai-api `TriageResponse.correctedChiefComplaint`（ai-api DTO 已有此字段行 93-98）。OOD §3.1 设计隐式路径要求检测并写入 `DialogueSession.correctedChiefComplaint`
- **修改建议**：修改编码实现 — `toTriageResponse()` 中读取 `aiData.getCorrectedChiefComplaint()` 并回写 session

### C04 — TriageServiceImpl.triage/selectDepartment/saveTriageRecord 缺少 @Transactional
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageServiceImpl.java`(34-35) 类级别无 `@Transactional`，`triage()` `selectDepartment()` `saveTriageRecord()` 均无事务注解。OOD §3.1 要求"先写数据库再更新内存"的策略需要事务保证
- **修改建议**：修改编码实现 — 在 `triage()` 和 `selectDepartment()` 方法上增加 `@Transactional`

### C05 — chiefComplaint 与 additionalResponses 互斥校验缺失
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageServiceImpl.java:64-80` `triage()` 入口处无对 `chiefComplaint` 和 `additionalResponses` 的组合校验。OOD §3.1 要求"同时提供二者或均未提供时返回 HTTP 400 + TRIAGE_FIELD_COMBINATION_INVALID"
- **修改建议**：修改编码实现 — 在方法入口增加互斥校验逻辑

### C06 — DoctorFacade 跨模块调用无 try/catch、无超时、无 WARN 日志
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageServiceImpl.java:169-179` `findDoctorsForDepartments()` 直接调用 `doctorFacade.findAvailableDoctorsByDepartment()` 无 try/catch。OOD §4.1 要求"DoctorFacade 调用超时/异常时捕获并记录 WARN 日志，TriageResponse.doctors 置为空列表"
- **修改建议**：修改编码实现 — 增加 try/catch 包裹门面调用，异常时记录 WARN 日志并返回空列表

### C07 — AI 调用 future.get() 无超时参数
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageServiceImpl.java:87` `future.get()` 无超时参数。OOD §2.3 要求"ai.timeout.triage=8s"
- **修改建议**：修改编码实现 — 改为 `future.get(8, TimeUnit.SECONDS)` 并处理超时异常

### C08 — selectDepartment 签名为 4 参、RegistrationEventListener 未调用该方法
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageService.java:10` 签名为 `selectDepartment(String sessionId, String departmentId, String departmentName, boolean overwrite)` 4 参；OOD §3.1 设计为 3 参（无 overwrite）。`RegistrationEventListener.java:38-44` 直接操作 repository 而非调用 `TriageService.selectDepartment()`，OOD 要求统一调用此方法
- **修改建议**：修改编码实现 — 接口改为 3 参（overwrite 逻辑下沉），并让 `RegistrationEventListener` 注入 `TriageService` 调用 `selectDepartment()`

### C09 — selectDepartment 使用 GlobalErrorCode.NOT_FOUND 而非业务级错误码
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageServiceImpl.java:148-150` 使用 `GlobalErrorCode.NOT_FOUND`。OOD §3.1 要求"记录不存在时返回 TRIAGE_SESSION_NOT_FOUND"
- **修改建议**：修改编码实现 — 替换为业务级错误码

### C10 — DialogueSessionManager 未校验 sessionId 的 UUID v4 格式
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DialogueSessionManager.java:21-25,31-37` `createSession()` 和 `restoreSession()` 均无 UUID 格式校验。OOD §3.1 要求"接受前端传入的 UUID v4 格式，验证格式有效性"
- **修改建议**：修改编码实现 — 增加 UUID v4 格式正则校验，失效时返回 `TRIAGE_SESSION_INVALID`

### C11 — TTL 清理周期为 1 分钟而非设计要求的 5 分钟
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DialogueSessionManager.java:39` `@Scheduled(fixedRate = 60000)`。OOD §3.1 要求统一 Spring @Scheduled 任务每 5 分钟扫描清理
- **修改建议**：修改编码实现 — 改为 `fixedRate = 300000`

### C12 — AI 上下文未按模板拼接全量历史，未实现 correctedChiefComplaint 替换 + 3000 字符截断
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageConverter.java:22-52` `toAiTriageRequest()` 仅单向映射，未实现全量拼接、correctedChiefComplaint 替换、上下文截断策略。OOD §3.1 要求"全量拼接策略"和"上下文截断策略"
- **修改建议**：修改编码实现 — 实现全量上下文拼接逻辑

### C13 — TriageRuleEngine.match 无快照失效回退逻辑，缺 ruleVersionMismatch 输出标记
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DefaultTriageRuleEngine.java:36-56` `match()` 仅过滤后返回规则结果，未实现快照版本无结果时的降级回退，未返回 `ruleVersionMismatch` 标记。OOD §3.1 要求"降级使用当前最新版本规则集重新匹配，并在 TriageResponse 中标记 ruleVersionMismatch=true"
- **修改建议**：修改编码实现 — 在 match() 返回空结果时降级到最新版本重新匹配并输出标记

### C14 — DeadLetterCompensationService 未检查 retryCount >= maxRetryCount
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DeadLetterCompensationService.java:42-46` `compensateDeadLetters()` 捕获异常后仅递增 retryCount，未判断 `retryCount >= maxRetryCount` 时将状态迁移到 `EXPIRED`。OOD §3.1 要求"状态迁移规则：FAILED →（重试耗尽/超出最大重试次数）→ EXPIRED"
- **修改建议**：修改编码实现 — 在递增 retryCount 后对比 maxRetryCount，超出时设置 state=EXPIRED

### C15 — RegistrationEventListener @Retryable 使用 retryFor = Exception.class
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`RegistrationEventListener.java:36` `@Retryable(retryFor = Exception.class, ...)`. OOD §3.1 要求"仅对 DataAccessException、TimeoutException 等可治愈临时异常触发重试"
- **修改建议**：修改编码实现 — 限定 retryFor 为可治愈异常类型，增加 noRetryFor 排除不可治愈异常

### C16 — DefaultTriageRuleEngine.match 未实现 TriageRule.conditions JSON 关键词解析
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DefaultTriageRuleEngine.java:36-56` 仅按规则版本和集标识过滤，返回全量命中规则，未对 `conditions` JSON 做关键词解析与 AND/OR 逻辑匹配。OOD §3.1 TriageRule 设计 `conditions` 为关键词匹配条件
- **修改建议**：修改编码实现 — 实现 keywords 关键词匹配 + AND/OR 逻辑解析

### C17 — findDoctorsForDepartments 未按匹配评分排序取前 5 名
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageServiceImpl.java:169-179` 无条件追加所有医生到列表，无排序、无限制取前 5。OOD §3.1 要求"按匹配评分排序取前 5 名"
- **修改建议**：修改编码实现 — 按 score 降序排序后取前 5

### C18 — saveTriageRecord 中 catch JsonProcessingException 完全静默
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageServiceImpl.java:212-213` `catch (JsonProcessingException e) { // ignore }`. OOD 要求 WARN 日志记录
- **修改建议**：修改编码实现 — 增加 WARN 级别日志

### C19 — saveTriageRecord 未读取 session.getCorrectedChiefComplaint() 写入实体
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageServiceImpl.java:181-217` `saveTriageRecord()` 未将 `session.getCorrectedChiefComplaint()` 写入 `TriageRecord`。OOD §3.1 要求 TriageRecord 存储 correctedChiefComplaint 快照
- **修改建议**：修改编码实现 — 在 saveTriageRecord 参数中传递 session 并写入 correctedChiefComplaint

### C20 — createSession 使用 put 而非 putIfAbsent，并发覆盖风险
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DialogueSessionManager.java:23` `sessionStore.put(sessionId, session)`. OOD §3.1 要求原子创建，put 存在并发覆盖风险
- **修改建议**：修改编码实现 — 改为 computeIfAbsent/putIfAbsent

### C21 — 降级路径使用 request 参数而非 session 快照的 ruleVersion/ruleSetId
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageServiceImpl.java:114-115` `triageRuleEngine.match(request.getChiefComplaint(), request.getRuleVersion(), request.getRuleSetId())` 使用请求参数而非 session 快照值。OOD §3.1 要求使用 DialogueSession 中记录的规则版本快照
- **修改建议**：修改编码实现 — 降级时从 session 获取快照版本号

### C22 — TriageController.selectDepartment 写死 overwrite=true
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageController.java:34` `triageService.selectDepartment(sessionId, departmentId, departmentName, true)` 硬编码 overwrite=true。OOD §4.1 要求手动选科端点始终覆盖写入（此为正确行为），但接口应隐藏此实现细节（参见 C08）
- **修改建议**：修改编码实现 — selectDepartment 接口改为 3 参，Controller 内部不做硬编码行为假设

### C23 — 主流程在调用 AI 前修改 session，违背"先写数据库再更新内存"策略
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageServiceImpl.java:72-80` 在 AI 调用前修改 `session.setChiefComplaint()`/`setAdditionalResponses()`/`setRoundCount()`。OOD §3.1 要求"先写数据库再更新内存"策略
- **修改建议**：修改编码实现 — 将业务数据操作移至 AI 调用和 TriageRecord 持久化之后

---

## 二、Prescription 处方审核 + 辅助开方模块

### P01 — 异步 AI 调度机制未实现
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DedupTaskScheduler.java:35-41` `schedule()` 创建 PENDING 条目后立即返回，无任何代码触发 `AiService.prescriptionAssist()` 异步调用并回填结果。OOD §3.4 要求异步 AI 调用完成后更新 AiSuggestionResult 状态
- **修改建议**：修改编码实现 — 在 schedule() 中或之后触发异步 AI 调用（如通过 @Async CompletableFuture）

### P02 — DrugFacade 在 PrescriptionAuditServiceImpl 和 PrescriptionAssistServiceImpl 均未注入
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`PrescriptionAuditServiceImpl.java:46-71` 和 `PrescriptionAssistServiceImpl.java:37-64` 构造器均无 `DrugFacade` 参数。OOD §2.2 明确要求 DrugFacade 的注入和使用
- **修改建议**：修改编码实现 — 注入 DrugFacade，在审核和辅助开方流程中调用

### P03 — AiSuggestionResult + PrescriptionDraftContext TTL 清理任务完全未实现
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：prescription 模块中无任何 `@Scheduled` 方法用于 TTL 清理。OOD §3.4 要求"统一 Spring @Scheduled 任务每 5 分钟扫描清理 TTL 超过 60 分钟的过期条目"
- **修改建议**：修改编码实现 — 在 prescription 模块增加 TTL 清理调度任务

### P04 — 规则变更事件未定义、未监听
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：prescription 模块下 `event/` 包不存在，无 `DrugContraindicationChangeEvent` / `DrugAllergyMappingChangeEvent` / `DrugCompositionDictChangeEvent` 定义或消费端。OOD §2.2 要求事件驱动缓存失效
- **修改建议**：修改编码实现 — 定义 3 类事件 + @EventListener 消费端 + Caffeine invalidate

### P05 — WARN 路径使用设计未定义的错误码 RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT；SubmitResponse 缺 warnResult 字段
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`PrescriptionAuditServiceImpl.java:205` 使用 `RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT`；`SubmitResponse.java`(1-44) 无 `warnResult` 字段（无 `riskLevel/alerts/auditRecordId/prescriptionHash`）。OOD §3.2 SubmitResponse 要求返回 `riskLevel/alerts/auditRecordId/prescriptionHash`
- **修改建议**：修改编码实现 — SubmitResponse 补充 `warnResult` 字段；此错误码设计上合理

### P06 — 降级路径 LocalRuleResult 未转换为 AuditAlert
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`PrescriptionAuditServiceImpl.java:96-103` 降级路径 `response.setAlerts(Collections.emptyList())`，不转换 LocalRuleResult。OOD §4.2 要求降级路径同样输出风险提示列表
- **修改建议**：修改编码实现 — 将 LocalRuleResult 列表转换为 AuditAlert 列表

### P07 — AuditRecord.auditIssues 字段从未写入
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`AuditRecord.java:50` 定义了 `auditIssues` 字段但 `PrescriptionAuditServiceImpl.java:307-342` `persistAuditRecord()` 不写入该字段。OOD §3.2 AuditRecord 设计含 auditIssues 用于持久化审核结果
- **修改建议**：修改编码实现 — 在 persistAuditRecord() 中序列化审计问题并写入

### P08 — forceSubmit 路径在生成 prescriptionOrderId 后未回写到 AuditRecord
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`PrescriptionAuditServiceImpl.java:231-245` forceSubmit 成功后 `latestRecord.setForceSubmitted(true)` 保存，但 `latestRecord.setPrescriptionOrderId(...)` 未被调用。OOD §4.2 要求"写入 forceSubmitted/forceSubmitTime → 处方落单"
- **修改建议**：修改编码实现 — 同步回写 prescriptionOrderId 到 AuditRecord

### P09 — PrescriptionItem.unit 字段为设计文档未定义的扩展字段
- **真实性**：真实存在
- **根因分类**：OOD 设计问题
- **证据**：`PrescriptionItem.java:11` 有 `unit` 字段，但 OOD §3.2 规定的 PrescriptionItem 仅 6 字段（drugId/drugName/dose/frequency/duration/route）。unit 字段在剂量校验中实际需要
- **修改建议**：修改 OOD 文档 — 在 PrescriptionItem 中正式补充 `unit` 字段定义

### P10 — DosageThresholdService.matchByPriority 第一、二重循环逻辑重复
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DosageThresholdService.java:103-121` 第一重循环（行 103-113）与第二重循环（行 115-121）逻辑几乎完全重复：都检查 age + weight 范围，区别仅在于第一重要求 exactAge && exactWeight。OOD §8.4 要求 6 级匹配优先级
- **修改建议**：修改编码实现 — 重构为清晰的 6 级降级匹配链

### P11 — SpecialPopulationDosageRule 与 DosageLimitRule 使用相同通用 DosageStandard 查询
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`SpecialPopulationDosageRule.java:36` 和 `DosageLimitRule` 均使用 `dosageStandardRepository.findByDrugCodeAndRouteOfAdministration()`，未区分特殊人群分级剂量标准，会产生重复告警。OOD §3.2 要求各自的独立查询路径
- **修改建议**：修改编码实现 — SpecialPopulationDosageRule 应针对特殊人群分级查询

### P12 — DrugInteractionRule 无 Phase 4 预留标注
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DrugInteractionRule.java:12-14` 的 `check()` 直接返回 PASS，无 `@ConditionalOnProperty` 控制运行时启用/禁用。OOD §3.2 要求标注为 Phase 4 预留
- **修改建议**：修改编码实现 — 增加 `@ConditionalOnProperty(name = "prescription.rule.drug-interaction.enabled", havingValue = "true", matchIfMissing = false)`

### P13 — AllergyCheckRule 结构化匹配未命中时不回退到文本匹配
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`AllergyCheckRule.java:43-60` 当 `allergyDetails` 存在且非空时（行 43），结构化过敏原匹配未命中则直接进入下一药品（行 62 返回 PASS），不回退到 `allergyHistory` 文本匹配。OOD §3.2 要求"当 allergyDetails 缺失时回退到 allergyHistory 文本匹配"——但当前代码在 allergyDetails 存在但未命中时不回退
- **修改建议**：修改编码实现 — 结构化匹配未命中后补充文本回退逻辑

### P14 — PrescriptionAssistServiceImpl 各失败路径未写入 PrescriptionDraftContext
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`PrescriptionAssistServiceImpl.java:81-99` 的 catch 块和 `buildEmptyResponse()`（行 205-214）直接返回空响应，不写入 CRITICAL 阻断到 PrescriptionDraftContext。OOD §3.4 要求 AI 不可用时 CRITICAL 阻断需持久化
- **修改建议**：修改编码实现 — 在降级/失败路径中写入 PrescriptionDraftContext

### P15 — AuditResponse.fromFallback 未序列化到 JSON 响应；降级 BLOCK 路径 reasons 固定字符串
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`PrescriptionAuditController.java:36-48`/`PrescriptionAuditServiceImpl.java:144-152` BLOCK 路径返回固定字符串 `"Prescription audit blocked"`，`fromFallback` 状态未在前端可区分的字段中暴露。OOD §4.2 要求降级路径的响应包含足够信息
- **修改建议**：修改编码实现 — 完善降级响应中的 fromFallback 序列化和阻断原因具体化

### P16 — AuditRecordRepository 没有 List 版本 findByPrescriptionOrderIdAndIsLatestTrue
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`AuditRecordRepository.java:18` 仅有 `findTopByPrescriptionOrderIdAndIsLatestTrue` 返回 Optional，无 List 版本。persistAuditRecord 按 prescriptionId 而非 prescriptionOrderId 分组清理 isLatest。OOD §3.2 要求按 prescriptionOrderId 分组清理、auditSequence 递增
- **修改建议**：修改编码实现 — 增加 List 版本查询方法，清理逻辑增加 prescriptionOrderId 分组

---

## 三、Medical-Record 病历生成模块

### M01 — 4 个错误码缺失
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`MedicalRecordErrorCode.java`(5-9) 当前仅 4/8 个错误码，缺少 `MR_GEN_AI_UNAVAILABLE` / `MR_GEN_AI_INPUT_INVALID` / `MR_GEN_AI_OUTPUT_INCOMPLETE` / `MR_GEN_TEMPLATE_LOAD_FAILED`。OOD §4.3 需要这些错误码覆盖全部降级场景
- **修改建议**：修改编码实现 — 补充缺失错误码

### M02 — TemplateConfigChangeEvent 事件定义 + @EventListener + Caffeine invalidate 全未实现
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DatabaseTemplateConfigManager.java:31-41` 只有 Caffeine 定时刷新（60s），无事件驱动失效机制。OOD §2.2 要求事件驱动立即失效 + 定时刷新兜底
- **修改建议**：修改编码实现 — 定义 TemplateConfigChangeEvent + @EventListener + invalidate()

### M03 — VisitIdReconciledTask 定时调和任务未实现
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：medical-record 模块无 `task/` 包，无定时任务。OOD §4.3 要求每 30 分钟扫描 visitIdFallback=true 记录反查正确 visitId
- **修改建议**：修改编码实现 — 创建定时调和任务

### M04 — MR_GEN_CONCURRENT_MODIFICATION 在当前 generate() 主流程中无法触发
- **真实性**：真实存在
- **根因分类**：OOD 设计问题
- **证据**：`MedicalRecordServiceImpl.java:102-110` 在 INSERT 路径（new MedicalRecord）上捕获 `OptimisticLockException`。但 JPA 新实体 INSERT 不会触发乐观锁异常（@Version 为 null）。OOD §3.3 将此错误码归入 INSERT 路径在技术原理上不可达
- **修改建议**：修改 OOD 文档 — 修正 MR_GEN_CONCURRENT_MODIFICATION 的触发条件描述为 UPDATE 路径

### M05 — RecordGenerateRequest 缺少验证注解
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`RecordGenerateRequest.java`(3-49) 无 `@NotNull/@Size(min=50,max=10000)` 等验证注解。OOD §4.3 要求"对话文本字符数 50–10000"
- **修改建议**：修改编码实现 — 增加 Jakarta Validation 注解

### M06 — 超时硬编码在源码中，未通过 @Value 注入
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`MedicalRecordServiceImpl.java:124,138` AI 超时 12s 和 VisitFacade 超时 2s 硬编码。OOD §4.3 要求通过配置注入
- **修改建议**：修改编码实现 — 通过 @Value 注入超时配置

### M07 — MedicalRecordConverter.toRecordGenerateResponse 在非超时失败时仍设 success=true
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`MedicalRecordConverter.java:56` `response.setSuccess(true)` 在所有路径下均为 true，即使 AI 超时失败。OOD §4.3 要求前端可区分降级与硬失败
- **修改建议**：修改编码实现 — 根据实际结果设置 success 状态

### M08 — 三处 catch 块手工 new AiResult<>() + setter，未使用 AiResultFactory
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`MedicalRecordServiceImpl.java:141-163` 手工 `new AiResult<>()` 然后逐个 setter。OOD §2.3 要求使用 `AiResultFactory` 的静态工厂方法
- **修改建议**：修改编码实现 — 替换为 `AiResultFactory.failure(errorCode, partialData)`

### M09 — MedicalRecord.doctorId 在 generate() 流程中从未被赋值
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`MedicalRecordServiceImpl.java:94-101` 创建 `MedicalRecord` 实体时设置 patientId/visitId/departmentId/content，唯独不设 doctorId。OOD §3.3 要求 doctorId 为必填字段
- **修改建议**：修改编码实现 — 从当前用户上下文获取并设置 doctorId

### M10 — MedicalRecord.content 字段无 @Column(name="content_json")
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`MedicalRecord.java:36-38` `@Column(columnDefinition = "TEXT")` 而非 `@Column(name="content_json", columnDefinition = "TEXT")`。OOD §3.3 要求数据库列名为 `content_json`
- **修改建议**：修改编码实现 — 增加 `name="content_json"`

### M11 — ai-api MedicalRecordGenResponse.missingFields 与 partialContent 未被业务层消费
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`MedicalRecordConverter.java:21-31` `toFieldsMap()` 仅提取 7 个业务字段，未消费 `missingFields` 和 `partialContent`。OOD §4.3 要求利用这些字段完善降级 Detection
- **修改建议**：修改编码实现 — 消费 missingFields 和 partialContent

---

## 四、Store 抽象层 + 并发安全（跨切面）

### S01 — SuggestionStore 接口缺少 createIfNotExists 原子方法
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`SuggestionStore.java`(6-8) 仅有 `compute()` 方法（需外部封装），缺少 `createIfNotExists(taskId, prescriptionId, supplier)` 原子方法。OOD §3.4 要求此方法消除 TOCTOU 竞态
- **修改建议**：修改编码实现 — 在 SuggestionStore 增加 createIfNotExists 方法

### S02 — AiSuggestionResult + PrescriptionDraftContext TTL 清理任务完全缺失（同 P03）
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：全局 scope 均无 TTL 清理 `@Scheduled`。OOD §3.4、§6.1 要求每 5 分钟扫描清理
- **修改建议**：修改编码实现 — 增加统一 TTL 清理调度任务

### S03 — DedupTaskScheduler.schedule() 在 compute lambda 内嵌套 suggestionStore.put(candidateTaskId, ...)
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DedupTaskScheduler.java:39` 在 `compute()` 的 remappingFunction 内调用 `suggestionStore.put(candidateTaskId, newResult)`，跨两个 key 操作，原子性失效。OOD §3.4 要求 compute 闭包内仅操作同一 key
- **修改建议**：修改编码实现 — 将跨 key 操作移出 compute lambda

### S04 — 同 session 并发访问无串行化保护
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DialogueSessionManager.java:21-25` createSession 使用 put 非 putIfAbsent；`DialogueSession` 为可变 POJO 无锁；`TriageServiceImpl.java:66-80` 同 session 并发时可能并发修改共享状态。OOD §3.1 要求 DialogueSessionManager 承担并发控制
- **修改建议**：修改编码实现 — 增加 session 级锁或串行化保护

### S05 — PrescriptionDraftContext.updateCriticalAlerts 使用 get-check-then-put 模式（误报）
- **真实性**：误报
- **根因分类**：N/A（误报条目）
- **证据**：`PrescriptionDraftContext.java:34-41` `updateCriticalAlerts()` 方法仅检查入参 `alerts` 是否为 null/empty，是则 `draftContextStore.remove(key)`，否则 `draftContextStore.put(key, alerts)`。方法中不存在从 Store 读取的操作（无"get"步骤），判断基于入参而非当前存储状态。`put` 和 `remove` 在 `ConcurrentHashMap` 上均为原子操作。OOD §3.4 要求的"全量覆盖"语义为本方法的设计意图，当前实现与此语义一致，无需 `compute()` 原子化改造。
- **修改建议**：无需修改（误报）

### S06 — DedupTaskScheduler.schedule() 返回值从 Object 强制转换为 AiSuggestionResult，类型不安全
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DedupTaskScheduler.java:43` `return ((AiSuggestionResult) result).getTaskId()` 在 compute lambda 外强制转换，若旧值为其他类型（如旧版数据残留）会抛 ClassCastException
- **修改建议**：修改编码实现 — 增加 instanceof 检查或类型安全封装

### S07 — ConcurrentHashMapStore 同时实现 SuggestionStore 和 DraftContextStore，共用同一 ConcurrentHashMap
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`ConcurrentHashMapStore.java:11` 同时实现 `SuggestionStore` 和 `DraftContextStore`；`ConcurrentHashMapStore.java:13` 单 `ConcurrentHashMap<String, Object>` 实例，遍历清理会混淆两类数据
- **修改建议**：修改编码实现 — 分离为两个独立 Store 实例（如通过 key prefix 或分离 Map）

---

## 五、AI 集成 + 降级策略（跨切面）

### A01 — AiResultFactory 在全部 4 个业务 Service 实现中零引用
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageServiceImpl.java`（无引用 AiResultFactory）、`MedicalRecordServiceImpl.java:141-163` 手工 `new AiResult<>()`、`PrescriptionAuditServiceImpl.java` 无引用、`PrescriptionAssistServiceImpl.java` 无引用。OOD §2.3 要求使用 AiResultFactory 静态工厂方法
- **修改建议**：修改编码实现 — 所有业务 Service 改用 AiResultFactory

### A02 — 所有 AI 调用超时完全未外化
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageServiceImpl.java:87` future.get() 无超时；`PrescriptionAuditServiceImpl.java:81` future.get() 无超时；`PrescriptionAssistServiceImpl.java:78` future.get() 无超时；`MedicalRecordServiceImpl.java:124,138` 硬编码 2s/12s。OOD §2.3 要求各操作均配置独立超时
- **修改建议**：修改编码实现 — Triage/PrescriptionAudit/PrescriptionAssist 均增加超时参数；所有超时通过 @Value 注入

### A03 — AiSuggestionResult 5 状态映射表全部未实现
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DedupTaskScheduler.java` 仅创建 PENDING 条目（无回填）；`PrescriptionAssistServiceImpl.java` checkDose 调用 `dedupTaskScheduler.schedule()` 后不启动异步 AI。OOD §3.4 要求 AI 异步调用完成后更新状态
- **修改建议**：修改编码实现 — 实现异步 AI 调用管线，完成 AiResult → AiSuggestionResult 映射

### A04 — correctedChiefComplaint 显式透传路径未生效
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageConverter.java:24` `toAiTriageRequest()` 不读取 `session.getCorrectedChiefComplaint()`。OOD §3.1 要求显式路径将被 correctedChiefComplaint 覆盖写入 TriageRequest
- **修改建议**：修改编码实现 — 在 toAiTriageRequest 中检测 session.getCorrectedChiefComplaint 并优先使用

### A05 — MockAiService 与设计契约严重不符
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`MockAiService.java:40` 使用 `@ConditionalOnProperty(name = "ai.mock.enabled")` 而非 `@Profile("mock")`，无三种返回模式（STATIC/AI_UNAVAILABLE/TIMEOUT），无 `MockAdminController`，无 `ai.mock.response-strategy` 配置键。OOD §2.3 MockAiService 实现契约完全要求了以上全部
- **修改建议**：修改编码实现 — 按 OOD 契约重写 MockAiService

### A06 — DegradationStrategy/DegradationContext 为空壳；FallbackAiService 仅取 delegates.get(0)，无重试链/降级链
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`DegradationStrategy.java`(3-6) 仅接口定义；`DegradationContext.java`(3-7) 空类；`FallbackAiService.java:183-194` applyStrategies() 仅迭代 strategy.shouldDegrade()。OOD §2.3 要求具备完整的降级链/重试链
- **修改建议**：修改编码实现 — 实现降级链（配置降级策略 + DegradationContext 状态追踪）

### A07 — AiResult.success(data) 允许 data=null，违反契约
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`AiResult.java:22` `AiResult.success(T data)` 不校验 data=null。OOD §2.3 要求"当 success=true 时 data 必须非 null"
- **修改建议**：修改编码实现 — success(data) 中增加 data != null 断言

### A08 — 降级路径 fallback 文案硬编码英文，设计要求中文
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageServiceImpl.java:127,132` 英文文案 `"AI service unavailable, using rule engine fallback"` / `"AI service has been continuously unavailable"`。OOD §4.1 要求中文文案
- **修改建议**：修改编码实现 — 替换为中文

### A09 — AuditConverter.toAuditResponse 在 aiData == null 时退化为 PASS + 空 alerts
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`AuditConverter.java:48-56` `aiData == null` 时设为 `PASS` + 空 alerts，而非触发 fallback。OOD §4.2 要求 AI 数据为空时走降级路径
- **修改建议**：修改编码实现 — aiData == null 时调用方应触发降级而非默认 PASS

### A10 — application.yml 完全缺失 ai.timeout.* / facade.*.timeout / ai.mock.* 等配置项
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`application.yml`(1-14) 仅含 JWT 配置，无任何 AI 超时/门面超时/Mock 配置项。OOD §2.3、§4.1-4.4 要求这些配置项
- **修改建议**：修改编码实现 — 在 application.yml 中补充完整配置项

### A11 — 业务层普遍在 isSuccess() 后额外做 && getData() != null 防御性检查，但 AiResult.success(null) 被测试用例断言合法
- **真实性**：真实存在
- **根因分类**：OOD 设计问题 + 实现编码问题
- **证据**：`TriageServiceImpl.java:98` `aiResult.isSuccess() && aiResult.getData() != null`；`PrescriptionAuditServiceImpl.java:92` 同样防御；`AiResult.java` 无 data 非空约束。OOD §2.3 契约"success=true → data 非 null"目前不被信任
- **修改建议**：修改编码实现 — 强制 AiResult.success(data) 使 data 非 null；移除业务层冗余判空。OOD 需澄清契约执行边界

---

## 六、跨模块事件 + Facade 模式（跨切面）

### E01 — RegistrationEventListener @Retryable 对所有 Exception.class 重试（同 C15）
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`RegistrationEventListener.java:36` `retryFor = Exception.class`
- **修改建议**：修改编码实现 — 限制 retryFor 为 DataAccessException/TimeoutException

### E02 — TriageRecord 同 sessionId 第二次分诊违反 @Column(unique = true) 约束
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`TriageServiceImpl.java:185-216` `saveTriageRecord()` 始终 `new TriageRecord()` 执行 INSERT，不先查已有记录做 UPDATE。OOD §3.1 要求"同一 sessionId 第二次发起分诊时 update 覆盖同 sessionId 记录"
- **修改建议**：修改编码实现 — saveTriageRecord 先查后改（查 sessionId 是否存在，存在则 UPDATE）

### E03 — DoctorFacade 跨模块调用完全无降级保护（同 C06）
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：同 C06 证据
- **修改建议**：修改编码实现 — 同 C06

### E04 — 5 类 Phase 2/3 ChangeEvent 的事件定义 + 消费端全部未实现（同 P04, M02）
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：全局无事件定义/消费端（参见 P04、M02）
- **修改建议**：修改编码实现 — 逐一实现各 ChangeEvent 及其消费端

### E05 — RegistrationEventListener.recover() 手工构造含 3 字段的 HashMap 作为 eventPayload
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：`RegistrationEventListener.java:50-57` 手工构建只有 3 字段的 HashMap，缺少完整事件对象序列化（如 registrationId、patientId、doctorId 等）。OOD §3.1 要求完整序列化 RegistrationEvent
- **修改建议**：修改编码实现 — 完整序列化 RegistrationEvent 对象而非手动构建

### E06 — DrugFacade 在 prescription 模块未注入（同 P02）
- **真实性**：真实存在
- **根因分类**：实现编码问题
- **证据**：同 P02 证据
- **修改建议**：修改编码实现 — 同 P02

## 修订说明（v2）

| 质询意见 | 回应 |
|---------|------|
| S05：诊断声称 `updateCriticalAlerts` 采用"get→check→put"模式与实际代码不符，不存在 get 步骤；put 对 ConcurrentHashMap 是原子的；compute() 在此场景下无实际效用 | 接受。经核实，`updateCriticalAlerts()` 方法（`PrescriptionDraftContext.java:34-41`）仅检查入参 `alerts` 后执行 `put()` 或 `remove()`，不存在从 Store 读取的操作，`put`/`remove` 在 ConcurrentHashMap 上均为原子操作。原诊断对代码行为的描述与事实不符，且 `compute()` 建议不适用于此方法。S05 从"真实存在 / 实现编码问题"修订为"误报"，修改建议更正为"无需修改"。 |
