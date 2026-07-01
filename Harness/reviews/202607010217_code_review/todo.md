# 待办事项

---

## 合并说明

- 本清单由 `todo.md`（48 项 R1/R2 审查产物）与 `Docs/Diagnosis/impl/06_phase2C3DE_report.md`（53 项验收问题）合并
- 优先级 P0/P1/P2 与报告对齐
- 报告已有 ID 的项在 `→报告ID` 中标注
- 误报项已剔除（仅留说明），todo 原编号不连续是历史编号保留
- **来源简写**：T=todo 旧编号，C/E/P/M/S/A=报告前缀

---

## P0 — 必须立即修复

### consultation 模块

- [ ] C01→T1a: TriageRecord 实体缺失 `correctedChiefComplaint` 快照字段。位置：`TriageRecord.java:14-144`。
- [ ] C02→T1b: TriageRecordRepository 缺少 `findTopBySessionIdOrderByTriageTimeDesc` 恢复查询方法。位置：`TriageRecordRepository.java:11-17`。
- [ ] C04: TriageServiceImpl.triage/selectDepartment/saveTriageRecord 缺少 `@Transactional` 边界（推荐 TransactionTemplate 仅包围 save）。位置：`TriageServiceImpl.java:34-35,191-194,238-292`。
- [ ] C05: chiefComplaint 与 additionalResponses 互斥校验缺失（应返回 TRIAGE_FIELD_COMBINATION_INVALID）。位置：`TriageServiceImpl.java:82-90`。
- [ ] C06/E03→T35a: DoctorFacade 跨模块调用无 try/catch、无超时、无 WARN 日志。位置：`TriageServiceImpl.java:213-236`。
- [ ] C07: AI 调用 `future.get()` 无超时参数（应注入 `ai.timeout.triage=8s`）。位置：`TriageServiceImpl.java:124`。
- [ ] C08→T2a: `selectDepartment` 签名为 4 参、RegistrationEventListener 未调用该方法。位置：`TriageService.java:10`,`RegistrationEventListener.java:39-50`。
- [ ] C15/E01: RegistrationEventListener `@Retryable` 对所有 `Exception.class` 重试（应限定 `DataAccessException/TimeoutException` + `noRetryFor`）。位置：`RegistrationEventListener.java:41-43`。
- [ ] C20: createSession 使用 `put` 而非 `putIfAbsent`，并发覆盖风险。位置：`DialogueSessionManager.java:42`。
- [ ] C21→T1c: 降级路径使用 request 参数而非 session 快照的 `ruleVersion/ruleSetId`。位置：`TriageServiceImpl.java:157-158`。
- [ ] E02: TriageRecord 同 sessionId 第二次分诊违反 `@Column(unique=true)` 约束（始终 INSERT 不 UPDATE）。位置：`TriageServiceImpl.java:264,289`。
- [ ] S04→T3: 同 session 并发访问无串行化保护（restoreSession 也缺 synchronized；createSession 用 put 而非 putIfAbsent）。位置：`DialogueSessionManager.java:50-71`。

### prescription 模块

- [ ] A01: AiResultFactory 在全部 4 个业务 Service 实现中零引用（应替换手工 `new AiResult<>()`）。位置：`TriageServiceImpl.java:127,131`、`PrescriptionAuditServiceImpl.java:111`、`MedicalRecordServiceImpl.java:151,154,157`。
- [ ] P01: 异步 AI 调度机制未实现（schedule 创建 PENDING 后无任何代码触发 `AiService.prescriptionAssist()`）。位置：`DedupTaskScheduler.java:21-69`。
- [ ] P02/E06→T11: DrugFacade 在 PrescriptionAuditServiceImpl 和 PrescriptionAssistServiceImpl 均未注入。位置：`PrescriptionAuditServiceImpl.java:72-87`、`PrescriptionAssistServiceImpl.java:57-75`。
- [ ] P03/S02→T33a: AiSuggestionResult + PrescriptionDraftContext TTL 清理任务完全缺失或无效（DraftContextCleanupTask 的 `writeTimestamps` 永远空）。位置：`DraftContextCleanupTask.java:22-46`、`SuggestionCleanupTask.java:25-40`。
- [ ] P04/E04: 规则变更事件未定义、未监听（DrugContraindicationChangeEvent/DrugAllergyMappingChangeEvent/DrugCompositionDictChangeEvent）。位置：`prescription/event/` 包不存在。
- [ ] S03→T34: DedupTaskScheduler.schedule() 在 compute lambda 内嵌套 `suggestionStore.put(candidateTaskId, ...)`，跨 key 操作破坏 compute 原子性。位置：`DedupTaskScheduler.java:51-62`。

### medical-record 模块

- [ ] A02: 所有 AI 调用超时完全未外化。位置：`MedicalRecordServiceImpl.java:132-134,148`。
- [ ] A03: AiSuggestionResult 5 状态映射表全部未实现（异步 AI 完成后无 PENDING→COMPLETED/FAILED 回填）。位置：`DedupTaskScheduler.java` + `PrescriptionAssistServiceImpl.java:340-369`。
- [ ] M01: 4 个错误码缺失（MR_GEN_AI_UNAVAILABLE/MR_GEN_AI_INPUT_INVALID/MR_GEN_AI_OUTPUT_INCOMPLETE/MR_GEN_TEMPLATE_LOAD_FAILED）。位置：`MedicalRecordErrorCode.java:5-9`。
- [ ] M04: MR_GEN_CONCURRENT_MODIFICATION 在当前 generate() INSERT 路径中无法触发（应移至 UPDATE 路径）。位置：`MedicalRecordServiceImpl.java:113-119`。

### application / 跨模块

- [ ] C03→T1d/A04→T5a: AI 隐式 correctedChiefComplaint 路径未实现 + 显式透传路径未生效。位置：`TriageConverter.java:63-65,107-109`。
- [ ] A10: application.yml 完全缺失 `ai.timeout.*` / `facade.*.timeout` / `ai.mock.*` 等配置项（7 个键）。位置：`application.yml:1-14`。

---

## P1 — 严重影响业务逻辑

### consultation 模块

- [ ] C09: selectDepartment 使用 `GlobalErrorCode.NOT_FOUND` 而非业务级错误码 TRIAGE_SESSION_NOT_FOUND。位置：`TriageServiceImpl.java:195`。
- [ ] C10: DialogueSessionManager 未校验 sessionId 的 UUID v4 格式（实际 L34-36, L51-53 已校验 → 误报，保留以核对）。位置：`DialogueSessionManager.java:34-36,51-53`。
- [ ] C12: AI 上下文未按模板拼接全量历史，未实现 correctedChiefComplaint 替换 + 3000 字符截断。位置：`TriageConverter.java:22-68`。
- [ ] C13: TriageRuleEngine.match 无快照失效回退逻辑，缺 `ruleVersionMismatch` 输出标记。位置：`DefaultTriageRuleEngine.java:41-80`。
- [ ] C16→T41a: DefaultTriageRuleEngine.match 未实现 TriageRule.conditions JSON 关键词解析（且 JSON 解析失败时静默返回 true 导致规则无条件匹配所有主诉）。位置：`DefaultTriageRuleEngine.java:82-116`。
- [ ] C17: findDoctorsForDepartments 未按匹配评分排序取前 5 名（当前按 availableSlotCount 排序而非 OOD 要求的 score）。位置：`TriageServiceImpl.java:232-235`。
- [ ] C19→T1e: saveTriageRecord 未读取 `session.getCorrectedChiefComplaint()` 写入实体。位置：`TriageServiceImpl.java:271`。
- [ ] C14: DeadLetterCompensationService 未检查 `retryCount >= maxRetryCount` 时的 EXPIRED 状态迁移。位置：`DeadLetterCompensationService.java:42-46`。
- [ ] C22→T2b: TriageController.selectDepartment 写死 `overwrite=true`（应随 C08 修复移除 4 参）。位置：`TriageController.java:34`。
- [ ] T4: TriageServiceImpl 降级路径手工构造 Response 而非复用 Converter（且遗漏 `matchedRules` 字段）。位置：`TriageServiceImpl.java:167-181`。
- [ ] T5: TriageConverter 副作用修改 Session + DialogueSession 混用 synchronized/AtomicInteger/CopyOnWriteArrayList。位置：`TriageConverter.java:107-109` / `DialogueSession.java:6-79`。
- [ ] T16: PrescriptionDraftContext.hasCriticalAlerts 与 getCriticalAlerts 缺少原子操作（TOCTOU 窗口）。位置：`PrescriptionDraftContext.java:19-22` + `PrescriptionAuditServiceImpl.java:152-154`。
- [ ] T42: DefaultTriageRuleEngine 缓存仅依赖 refreshAfterWrite 无主动刷新（事件驱动失效未实现）。位置：`DefaultTriageRuleEngine.java:30-37`。

### prescription 模块

- [ ] P05: SubmitResponse 缺 `warnResult` 字段；WARN 路径使用未定义的 `RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT`（OOD 需先补充字段定义）。位置：`SubmitResponse.java`、`PrescriptionAuditServiceImpl.java:205`。
- [ ] P06: 降级路径 LocalRuleResult 未转换为 AuditAlert。位置：`PrescriptionAuditServiceImpl.java:116-130`。
- [ ] P07: AuditRecord.auditIssues 字段从未写入。位置：`PrescriptionAuditServiceImpl.java:357-432`。
- [ ] P08→T29a: forceSubmit 路径在生成 prescriptionOrderId 后未回写到 AuditRecord（且 forceSubmit 路径未验证 prescriptionHash，SubmitRequest 无 hash 字段）。位置：`PrescriptionAuditServiceImpl.java:236-285`、`SubmitRequest.java:1-53`。
- [ ] P10: DosageThresholdService.matchByPriority 第一、二重循环逻辑重复。位置：`DosageThresholdService.java:103-121`。
- [ ] P11: SpecialPopulationDosageRule 与 DosageLimitRule 使用相同通用 DosageStandard 查询。位置：`SpecialPopulationDosageRule.java:36`。
- [ ] P13: AllergyCheckRule 结构化匹配未命中时不回退到文本匹配。位置：`AllergyCheckRule.java:43-60`。
- [ ] P14: PrescriptionAssistServiceImpl 各失败路径未写入 PrescriptionDraftContext。位置：`PrescriptionAssistServiceImpl.java:90-99,108-111,114-118`。
- [ ] P16: AuditRecordRepository 没有 List 版本 `findByPrescriptionOrderIdAndIsLatestTrue`。位置：`AuditRecordRepository.java:18`。
- [ ] S01: SuggestionStore 接口缺少 `createIfNotExists` 原子方法（已有 `createIfNotExists(key, value)` 但缺 supplier 形式 → 误报，保留以核对）。位置：`SuggestionStore.java:6-8`。
- [ ] S06: DedupTaskScheduler.schedule() 返回值从 Object 强制转换为 AiSuggestionResult。位置：`DedupTaskScheduler.java:64-67`。
- [ ] S07: ConcurrentHashMapStore 同时实现 SuggestionStore 和 DraftContextStore，共用同一 ConcurrentHashMap。位置：`ConcurrentHashMapStore.java:10-37`。
- [ ] T8: AllergyCheckRule + AllergyDetail 跨模块依赖 patient 模块（AllergySeverity 应在 prescription 模块内）。位置：`rule/AllergyCheckRule.java:3` / `dto/audit/AllergyDetail.java:3`。
- [ ] T9: DosageThresholdService 频率解析 `Integer.parseInt("tid")` 导致日剂量校验静默失效（空 catch 吞 NumberFormatException）。位置：`DosageThresholdService.java:76-89`。
- [ ] T10: PrescriptionDraftContext.getCriticalAlerts unchecked cast（`@SuppressWarnings` 下无泛型类型校验）。位置：`PrescriptionDraftContext.java:24-32`。
- [ ] T14: PrescriptionItem.dose 使用 double 存在 BigDecimal 转换精度风险（OOD §1.3 明确要求 BigDecimal）。位置：`dto/audit/PrescriptionItem.java:7`。
- [ ] T15: prescriptionOrderId 使用 `System.currentTimeMillis()` 存在毫秒级冲突（5 处 submit 路径同模式）。位置：`PrescriptionAuditServiceImpl.java:218,259,289,298,328`。
- [ ] T18: MedicalRecordConverter 使用字面字符串 `"MR_GEN_AI_TIMEOUT"` 比较错误码。位置：`MedicalRecordConverter.java:69,73`。
- [ ] T19: MockAiService.TIMEOUT 策略使用永不完成的 `new CompletableFuture<>()`（测试场景资源泄漏）。位置：`MockAiService.java:67`。
- [ ] T20: FallbackAiService.applyStrategies 使用空 DegradationContext（serviceName/operationName 为 null）。位置：`FallbackAiService.java:290-301`。
- [ ] T21: 元数据字段混入业务字段（MISSING_FIELDS/PARTIAL_CONTENT 写入 content_map，DEFAULT 模板包含这两个元数据字段）。位置：`MedicalRecordConverter.java:29-47` / `DatabaseTemplateConfigManager.java:111`。
- [ ] T22: MedicalRecordContentConverter 序列化/反序列化异常时静默返回 null/emptyMap（无 WARN 日志）。位置：`MedicalRecordContentConverter.java:29,44-46`。
- [ ] T27: submit() 方法缺乏 prescriptionId 级别并发提交防护（两个并发线程可同时通过 BLOCK 检查）。位置：`PrescriptionAuditServiceImpl.java:147-195`。
- [ ] T28: hasNewAlerts 逻辑在正常路径中无实际作用（snapshot=emptyList + current 非空时恒返回 true）。位置：`PrescriptionAuditServiceImpl.java:152-164,182-192,502-506`。
- [ ] T30: DosageLimitRule.findBestMatch 返回 null 时静默回退到 `standards.get(0)`。位置：`DosageLimitRule.java:36-38`。
- [ ] T31: AllergyCheckRule allergyHistory 自由文本 contains 子串匹配过于激进（"No allergy to penicillin" 会命中 penicillin 并直接返回 BLOCK）。位置：`AllergyCheckRule.java:54-58`。
- [ ] T32: DraftContextCleanupTask 迭代 keySet() 与 writeTimestamps 之间非原子（且 writeTimestamps 永远空——recordWrite 无调用方）。位置：`DraftContextCleanupTask.java:38-45`。
- [ ] T35: AuditConverter + ai-api DTO 遗漏 weight 和 unit 字段映射。位置：`AuditConverter.java:66-75,77-92` / `ai-api/PatientInfo.java:5-13` / `ai-api/PrescriptionCheckItem.java:3-11`。
- [ ] T40: @Recover 方法缺陷（无 @Transactional、e.getMessage() 可能为 null 违反 NOT NULL 约束、JSON 兜底生成 "null" 字符串）。位置：`RegistrationEventListener.java:52-66`。

### medical-record 模块

- [ ] M02→T42: TemplateConfigChangeEvent 事件定义 + @EventListener + Caffeine invalidate 全未实现。位置：`DatabaseTemplateConfigManager.java:105-108`。
- [ ] M03: VisitIdReconciledTask 定时调和任务未实现。位置：medical-record `task/` 包不存在。
- [ ] M05: RecordGenerateRequest 缺少 `@NotNull/@Size(min=50,max=10000)` 等验证注解。位置：`RecordGenerateRequest.java:1-49`。
- [ ] M06→T17a: 超时硬编码在源码中未通过 @Value 注入（12s 和 2s 硬编码）。位置：`MedicalRecordServiceImpl.java:40-44,134,148`。
- [ ] M07: MedicalRecordConverter.toRecordGenerateResponse 在非超时失败时仍设 `success=true`。位置：`MedicalRecordConverter.java:72-74`。
- [ ] M09: MedicalRecord.doctorId 在 generate() 流程中从未被赋值。位置：`MedicalRecordServiceImpl.java:104-107`。
- [ ] M11→T21a: ai-api MedicalRecordGenResponse.missingFields 与 partialContent 未被业务层消费。位置：`MedicalRecordConverter.java:38-45`。
- [ ] T17→M08: callAiWithTimeout 异常语义混淆（三种异常统一返回 degraded("timeout")）+ 三处 catch 块手工 new AiResult<>() 未用 AiResultFactory。位置：`MedicalRecordServiceImpl.java:145-159`。
- [ ] T47: MedicalRecord `@PrePersist` 未设置 updatedAt（新增记录 updatedAt 为 null）。位置：`MedicalRecord.java:133-141`。
- [ ] T48: MedicalRecordServiceImpl.resolveVisitId 使用 `ForkJoinPool.commonPool()`（Web 高并发下线程耗尽风险）。位置：`MedicalRecordServiceImpl.java:132`。
- [ ] T50: DraftContextStoreImpl 缺少 compute/putIfAbsent 原子操作（与 S07 合并修复）。位置：`DraftContextStoreImpl.java:11-26`。

### application / 跨模块

- [ ] A05: MockAiService 与设计契约严重不符（`@ConditionalOnProperty` 而非 `@Profile("mock")`，无三种返回模式，无 MockAdminController）。位置：`MockAiService.java:40-42`。
- [ ] A06→T20a: DegradationStrategy/DegradationContext 为空壳；FallbackAiService 仅取 delegates.get(0) 无重试链。位置：`DegradationStrategy.java:3-6`、`DegradationContext.java:3-26`、`FallbackAiService.java:61-80`。
- [ ] A09: AuditConverter.toAuditResponse 在 aiData == null 时退化为 PASS + 空 alerts（应在调用方拦截）。位置：`AuditConverter.java:48-56`。
- [ ] E05→T39a: RegistrationEventListener.recover() 手工构造含 3 字段的 HashMap 作为 eventPayload（且缺 sessionId null 防护）。位置：`RegistrationEventListener.java:45,55-58`。

---

## P2 — 可并行修复

### consultation 模块

- [ ] C11: TTL 清理周期为 1 分钟而非设计要求的 5 分钟。位置：`DialogueSessionManager.java:73`。
- [ ] C18: saveTriageRecord 中 catch JsonProcessingException 完全静默。位置：`TriageServiceImpl.java:251-253`。
- [ ] C23（说明性）: 主流程 AI 输入准备阶段 session 修改的约束边界（已在 correctedChiefComplaint 数据流群组中覆盖）。
- [ ] T44: TriageResponse 缺少 correctedChiefComplaint 字段（前端无法感知 AI 对主诉的修正）。位置：`dto/TriageResponse.java:5-18`。
- [ ] T45: TriageServiceImpl.selectDepartment 缺少自动生成 UUID v4 校验（事件路径中 sessionId 为空时抛出 NPE）。位置：`RegistrationEventListener.java:45`。

### prescription 模块

- [ ] P09（OOD 文档修改）: PrescriptionItem.unit 字段为 OOD 未正式定义的扩展字段——需在 §1.1a/§3.2/§4.6/§8.3 补充。位置：`PrescriptionItem.java:11`。
- [ ] P12（→T25 降级）: DrugInteractionRule 无 Phase 4 预留标注（@ConditionalOnProperty 缺失；空实现本身是 OOD §3.2 line 604 明确允许）。位置：`DrugInteractionRule.java:1-15`。
- [ ] P15: AuditResponse.fromFallback 未序列化到 JSON 响应；降级 BLOCK 路径 reasons 固定字符串。位置：`PrescriptionAuditController.java:36-48`、`PrescriptionAuditServiceImpl.java:144-152`。
- [ ] M10: MedicalRecord.content 字段无 `@Column(name="content_json")`。位置：`MedicalRecord.java:39-41`。
- [ ] A08: 降级路径 fallback 文案硬编码英文。位置：`TriageServiceImpl.java:171,177`、`MedicalRecordServiceImpl.java:151,154,157`。

### application / 跨模块

- [ ] A07: AiResult.success(data) 允许 data=null，违反契约。位置：`AiResult.java:24-26`。
- [ ] A11: 业务层普遍在 isSuccess() 后额外做 `&& getData() != null` 防御性检查（应在 A07 修复后移除）。位置：`TriageServiceImpl.java:137`、`PrescriptionAuditServiceImpl.java:110`。
- [ ] T24（修正描述）: ConcurrentHashMapStore 缺少 Spring 注解（DraftContextStoreImpl 已有 @Service；真正缺注解的是 ConcurrentHashMapStore）。位置：`ConcurrentHashMapStore.java:10`。

### 误报项（已从主清单剔除，仅留说明）

- [ ] S05（误报）: PrescriptionDraftContext.updateCriticalAlerts 实际仅检查入参非空，put/remove 在 ConcurrentHashMap 上原子，无需 compute。位置：`PrescriptionDraftContext.java:34-41`。
- [ ] T25（描述失实）: DrugInteractionRule 空实现是 OOD §3.2 line 604 明确允许的"Phase 2/3 不启用"，非 bug；真实问题见 P12（缺 Phase 4 标注）。
- [ ] T46（不存在）: DatabaseTemplateConfigManager L57+L75 双重兜底返回 `DEFAULT_TEMPLATE`，永不返回 null。位置：`DatabaseTemplateConfigManager.java:46-48,110-119`。

---

## 跨问题耦合约束（修复顺序）

| 群组 | 关联项 | 修复顺序 |
|------|--------|----------|
| correctedChiefComplaint 数据流 | C01 → A04 → C03 → C19 → C23 | 先加实体字段，再透传，再回写，最后持久化 |
| 事务边界+并发控制 | C04 + E02 + S04 | TransactionTemplate 包围 save → UPDATE 替代 INSERT → @Lock + putIfAbsent |
| DoctorFacade + 排序 | C06/E03 + C17 | 同一方法内一次性完成 try/catch + 排序取前 5 |
| DrugFacade + 降级 + 事件 | P02/E06 + P06 + P04/E04 | 无依赖可并行 |
| AI 超时配置 | A02 + A10 | A10 先写配置键，A02 再 @Value 注入 |
| 异步 AI 调度管线 | P01 + A03 | 同步实施（先触发后状态映射） |
| AiResult 契约 | A07 + A09 + A11 | A09 调用方拦截 → A07 断言 → A11 移除冗余判空 |
| AuditResult 字段扩展 | P05 | 编码前先完成 OOD §1.1a SubmitResponse 字段补充 |
| DrugInteractionRule 标注 | P12/T25 | 误报，添加 @ConditionalOnProperty 即可 |
