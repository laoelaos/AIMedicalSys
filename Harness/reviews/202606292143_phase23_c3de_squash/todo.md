# 待办事项 — 验证后的严重/一般问题清单

### 说明
- 来源于 6 份审查文档（R1.1/R1.2/R1.3 + R2.1/R2.2/R2.3），经逐条验证
- 仅包含**成立**的 **严重** 和 **一般** 问题，已去重
- 去掉了"建议"细节，只保留问题描述
- 以下 2 处审查主张经验证不成立，已排除：① R2.1 isLatest 悲观锁已存在；② R2.3 RegistrationEvent 字段计数偏差为审查目标描述错误

---

## 一、Consultation 分诊模块

### [严重]
- [ ] C01. `TriageRecord` 实体缺失 `correctedChiefComplaint` 快照字段 — `TriageRecord.java`
- [ ] C02. `TriageRecordRepository` 缺少 `findTopBySessionIdOrderByTriageTimeDesc` 恢复查询方法 — `TriageRecordRepository.java`
- [ ] C03. AI 隐式 `correctedChiefComplaint` 路径未实现：`TriageConverter.toTriageResponse` 从未读取 ai-api `TriageResponse.correctedChiefComplaint` — `TriageConverter.java:54-90`
- [ ] C04. `TriageServiceImpl.triage/selectDepartment/saveTriageRecord` 缺少 `@Transactional` — `TriageServiceImpl.java`
- [ ] C05. `chiefComplaint` 与 `additionalResponses` 互斥校验缺失（应返回 `TRIAGE_FIELD_COMBINATION_INVALID`）— `TriageServiceImpl.java:64`
- [ ] C06. `DoctorFacade` 跨模块调用 `findDoctorsForDepartments` 无 try/catch、无超时、无 WARN 日志，异常会传播为 500 — `TriageServiceImpl.java:169-179`
- [ ] C07. AI 调用 `future.get()` 无超时参数，AI 挂起时无限阻塞请求线程 — `TriageServiceImpl.java:87`

### [一般]
- [ ] C08. `selectDepartment` 接口签名为 4 参（含 `overwrite`），设计文档要求 3 参；`RegistrationEventListener` 未调用该方法而是直接操作 repository — `TriageService.java:10` / `RegistrationEventListener.java:38-44`
- [ ] C09. `selectDepartment` 找不到记录时使用 `GlobalErrorCode.NOT_FOUND` 而非业务级 `TRIAGE_SESSION_NOT_FOUND` — `TriageServiceImpl.java:148-150`
- [ ] C10. `DialogueSessionManager.createSession/restoreSession` 入口未校验 sessionId 的 UUID v4 格式 — `DialogueSessionManager.java:21-25,31-37`
- [ ] C11. `DialogueSessionManager` TTL 清理周期为 1 分钟（`fixedRate=60000`），设计要求 5 分钟 — `DialogueSessionManager.java:39`
- [ ] C12. AI 上下文未按模板拼接全量历史，未实现 `correctedChiefComplaint` 替换 + 3000 字符截断 — `TriageConverter.java:22-52`
- [ ] C13. `TriageRuleEngine.match` 无快照失效回退逻辑（快照版本无结果时不会降级到当前最新版本），缺 `ruleVersionMismatch` 输出标记 — `DefaultTriageRuleEngine.java:36-56`
- [ ] C14. `DeadLetterCompensationService` 未检查 `retryCount >= maxRetryCount` 并将状态迁移到 `EXPIRED` — `DeadLetterCompensationService.java:42-46`
- [ ] C15. `RegistrationEventListener` @Retryable 使用 `retryFor = Exception.class`（对所有异常重试），未排除不可治愈异常 — `RegistrationEventListener.java:36`
- [ ] C16. `DefaultTriageRuleEngine.match` 未实现 `TriageRule.conditions` JSON 关键词解析与匹配（AND/OR 逻辑），当前等价于按版本过滤后全量返回 — `DefaultTriageRuleEngine.java:36-56`
- [ ] C17. `findDoctorsForDepartments` 未按匹配评分排序取前 5 名 — `TriageServiceImpl.java:169-179`
- [ ] C18. `saveTriageRecord` 中 `catch (JsonProcessingException)` 完全静默，无 WARN 日志 — `TriageServiceImpl.java:212-214`
- [ ] C19. `saveTriageRecord` 未读取 `session.getCorrectedChiefComplaint()` 写入实体 — `TriageServiceImpl.java:181-217`
- [ ] C20. `createSession` 使用 `put` 而非 `putIfAbsent`，并发覆盖风险 — `DialogueSessionManager.java:21-25`
- [ ] C21. 降级路径使用 request 参数而非 session 快照的 `ruleVersion/ruleSetId`，同一会话内不同轮次可能使用不同规则版本 — `TriageServiceImpl.java:114-115`
- [ ] C22. `TriageController.selectDepartment` 写死 `overwrite=true`，对外契约污染内部协作细节 — `TriageController.java:30-36`
- [ ] C23. 主流程在调用 AI 前就修改 `session`（setChiefComplaint/setAdditionalResponses/setRoundCount），违背 §3.1 "**先写数据库再更新内存**" 策略 — `TriageServiceImpl.java:72-80`

---

## 二、Prescription 处方审核 + 辅助开方模块

### [严重]
- [ ] P01. 异步 AI 调度机制未实现：`DedupTaskScheduler.schedule()` 仅创建 PENDING 条目后立即返回，没有任何代码触发 `AiService.prescriptionAssist()` 异步调用并回填结果 — `DedupTaskScheduler.java:35-41`
- [ ] P02. `DrugFacade` 在 `PrescriptionAuditServiceImpl` 和 `PrescriptionAssistServiceImpl` 均未注入、未调用，跨模块门面空转 — 全 prescription 模块
- [ ] P03. `AiSuggestionResult` 60 分钟 TTL + `PrescriptionDraftContext` 60 分钟 TTL 清理任务完全未实现，无 `@Scheduled` 方法 — prescription 模块全缺
- [ ] P04. 5 类规则变更事件（`DrugAllergyMappingChangeEvent` / `DrugContraindicationChangeEvent` / `DrugCompositionDictChangeEvent`）未定义、未监听，本地 Caffeine 缓存无法被事件驱动失效 — prescription 模块无 `event/` 包
- [ ] P05. 提交端点 WARN 路径使用设计未定义的错误码 `RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT`；`SubmitResponse` 缺 `warnResult` 字段（应按设计返回 `riskLevel/alerts/auditRecordId/prescriptionHash`）— `PrescriptionAuditServiceImpl.java:202-207` / `SubmitResponse.java`

### [一般]
- [ ] P06. 降级路径 `LocalRuleResult` 未转换为 `AuditAlert`，`AuditResponse.alerts` 固定为空列表 — `PrescriptionAuditServiceImpl.java:96-103`
- [ ] P07. `AuditRecord.auditIssues` 字段定义但 `persistAuditRecord` 从未写入 — `AuditRecord.java:50` / `PrescriptionAuditServiceImpl.java:307-342`
- [ ] P08. `forceSubmit=true` 路径生成 `prescriptionOrderId` 后未回写到 `AuditRecord.prescriptionOrderId` — `PrescriptionAuditServiceImpl.java:231-245`
- [ ] P09. `PrescriptionItem.unit` 字段为设计文档未定义的扩展字段 — `PrescriptionItem.java:11`
- [ ] P10. `DosageThresholdService.matchByPriority` 第一、二重循环逻辑重复（行 103-121），与 §8.4 6 级决策表的 null 边界场景偏离 — `DosageThresholdService.java:95-147`
- [ ] P11. `SpecialPopulationDosageRule` 与 `DosageLimitRule` 使用相同的通用 `DosageStandard` 查询，未区分特殊人群分级，会产生重复告警 — `SpecialPopulationDosageRule.java:24-56`
- [ ] P12. `DrugInteractionRule` 无 Phase 4 预留标注（`@ConditionalOnProperty`），运行时启用/禁用语义模糊 — `DrugInteractionRule.java:12-14`
- [ ] P13. `AllergyCheckRule` 结构化匹配未命中时不回退到文本匹配，可能漏报过敏冲突 — `AllergyCheckRule.java:43-60`
- [ ] P14. `PrescriptionAssistServiceImpl` 各失败路径未写入 `PrescriptionDraftContext`，AI 不可用时 CRITICAL 阻断无法持久化 — `PrescriptionAssistServiceImpl.java:81-99`
- [ ] P15. `AuditResponse.fromFallback` 未序列化到 JSON 响应；降级 BLOCK 路径 `reasons` 仅为固定字符串 `"Prescription audit blocked"` — `PrescriptionAuditController.java:36-48`
- [ ] P16. `AuditRecordRepository` 没有 List 版本的 `findByPrescriptionOrderIdAndIsLatestTrue`；`persistAuditRecord` 未按 `prescriptionOrderId` 分组清理 `isLatest` — `AuditRecordRepository.java`

---

## 三、Medical-Record 病历生成模块

### [严重]
- [ ] M01. 4 个错误码缺失：`MR_GEN_AI_UNAVAILABLE` / `MR_GEN_AI_INPUT_INVALID` / `MR_GEN_AI_OUTPUT_INCOMPLETE` / `MR_GEN_TEMPLATE_LOAD_FAILED` — `MedicalRecordErrorCode.java`（当前仅 4/8）
- [ ] M02. `TemplateConfigChangeEvent` 事件定义 + `@EventListener` 消费端 + Caffeine `invalidate()` 全未实现，模板变更最长需 60 秒才被感知 — `DatabaseTemplateConfigManager.java:31-41`
- [ ] M03. `VisitIdReconciledTask` 定时调和任务未实现（设计要求每 30 分钟扫描 `visitIdFallback=true` 记录反查正确 visitId）— medical-record 模块无 `task/` 包
- [ ] M04. `MR_GEN_CONCURRENT_MODIFICATION` 错误码路径在当前 `generate()` 主流程中无法触发——INSERT 路径不会抛出 `OptimisticLockException` — `MedicalRecordServiceImpl.java:102-110`

### [一般]
- [ ] M05. `RecordGenerateRequest` 缺少 `@NotNull/@Size(min=50,max=10000)` 等验证注解 — `RecordGenerateRequest.java`
- [ ] M06. AI 超时 12s 和 VisitFacade 超时 2s 均硬编码在源码中，未通过 `@Value` 注入 — `MedicalRecordServiceImpl.java:124,138`
- [ ] M07. `MedicalRecordConverter.toRecordGenerateResponse` 在非超时失败时仍设 `success=true`，前端无法区分降级与硬失败 — `MedicalRecordConverter.java:43-58`
- [ ] M08. 三处 catch 块手工 `new AiResult<>()` + setter，未使用 `AiResultFactory` — `MedicalRecordServiceImpl.java:141-163`
- [ ] M09. `MedicalRecord.doctorId` 字段在 `generate()` 流程中从未被赋值，持久化后为 NULL — `MedicalRecordServiceImpl.java:94-101`
- [ ] M10. `MedicalRecord.content` 字段无 `@Column(name="content_json")`，数据库列名为 `content` 而非设计文档的 `content_json` — `MedicalRecord.java:36-38`
- [ ] M11. ai-api `MedicalRecordGenResponse.missingFields` 与 `partialContent` 未被业务层消费 — `MedicalRecordConverter.java:21-31`

---

## 四、Store 抽象层 + 并发安全（跨切面）

### [严重]
- [ ] S01. `SuggestionStore` 接口缺少 `createIfNotExists(taskId, prescriptionId, supplier)` 原子方法 — `SuggestionStore.java`
- [ ] S02. `AiSuggestionResult` 与 `PrescriptionDraftContext` TTL 清理任务完全缺失（设计要求每 5 分钟扫描，TTL 60 分钟），构成内存泄漏 — 全局
- [ ] S03. `DedupTaskScheduler.schedule()` 在 `compute` lambda 内嵌套 `suggestionStore.put(candidateTaskId, ...)`，跨 key 原子性失效 — `DedupTaskScheduler.java:39`
- [ ] S04. 同 session 并发访问无串行化保护：`DialogueSession` 为可变 POJO，`DialogueSessionManager.createSession/restoreSession` 无锁 — `TriageServiceImpl.java:66-80` / `DialogueSession.java`

### [一般]
- [ ] S05. `PrescriptionDraftContext.updateCriticalAlerts` 使用 get-check-then-put 模式，非原子操作 — `PrescriptionDraftContext.java:34-41`
- [ ] S06. `DedupTaskScheduler.schedule()` 返回值从 `Object` 强制转换为 `AiSuggestionResult`，类型不安全 — `DedupTaskScheduler.java:43`
- [ ] S07. `ConcurrentHashMapStore` 同时实现 `SuggestionStore` 和 `DraftContextStore`，共用同一 `ConcurrentHashMap<String, Object>`，遍历清理会读到另一 Store 的数据 — `ConcurrentHashMapStore.java:13`

---

## 五、AI 集成 + 降级策略（跨切面）

### [严重]
- [ ] A01. `AiResultFactory` 在全部 4 个业务 Service 实现中零引用——`MedicalRecordServiceImpl` 手工 `new AiResult<>()`；`TriageServiceImpl.handleAiFailure` 使用旧 `AiResult.degraded` — `TriageServiceImpl.java` / `MedicalRecordServiceImpl.java` / `PrescriptionAuditServiceImpl.java` / `PrescriptionAssistServiceImpl.java`
- [ ] A02. 所有 AI 调用超时完全未外化：3 处 `future.get()` 无限等待（Triage / PrescriptionAudit / PrescriptionAssist），1 处硬编码 — `TriageServiceImpl.java:87` / `PrescriptionAuditServiceImpl.java:81` / `PrescriptionAssistServiceImpl.java:78` / `MedicalRecordServiceImpl.java:124,138`
- [ ] A03. `AiSuggestionResult` 5 状态映射表全部未实现 —— `PENDING` 条目创建后没有任何代码将结果回填到 `suggestion/failReason/partialData` 字段，`GET /assist/suggestion/{taskId}` 永远等待 — `DedupTaskScheduler.java` / `PrescriptionAssistServiceImpl.java`
- [ ] A04. `correctedChiefComplaint` 显式透传路径未生效——`TriageConverter.toAiTriageRequest` 不读取 `session.getCorrectedChiefComplaint()` — `TriageConverter.java:24`
- [ ] A05. `MockAiService` 与设计契约严重不符：无 `@Profile("mock")`、无三种返回模式（STATIC/AI_UNAVAILABLE/TIMEOUT）、无 `MockAdminController` 切换端点、无 `ai.mock.response-strategy` 配置键 — `MockAiService.java`
- [ ] A06. `DegradationStrategy` / `DegradationContext` 为空壳；`FallbackAiService` 仅取 `delegates.get(0)`，无重试链/降级链 — `DegradationStrategy.java` / `FallbackAiService.java:183-194`

### [一般]
- [ ] A07. `AiResult.success(data)` 允许 `data=null`，违反 §2.3 "success=true → data 非 null" 契约 — `AiResult.java:22` / `AiResultFactory.java:20`
- [ ] A08. 降级路径 fallback 文案硬编码英文（`"AI service unavailable, using rule engine fallback"`），设计要求中文 — `TriageServiceImpl.java:127,132`
- [ ] A09. `AuditConverter.toAuditResponse` 在 `aiData == null` 时退化为 `PASS` + 空 alerts，而非 fallback 路径 — `AuditConverter.java:48-56`
- [ ] A10. `application.yml` 完全缺失 `ai.timeout.*` / `facade.*.timeout` / `ai.mock.*` 等配置项，`application-mock.yml` 不存在 — `application.yml`
- [ ] A11. 业务层普遍在 `isSuccess()` 后额外做 `&& getData() != null` 防御性检查，说明 §2.3 "success=true → data 非 null" 契约不被信任；但 `AiResult.success(null)` 又被测试用例断言合法，契约语义不一致 — `AiResult.java:22` / 各业务 Service

---

## 六、跨模块事件 + Facade 模式（跨切面）

### [严重]
- [ ] E01. `RegistrationEventListener @Retryable` 对所有 `Exception.class` 重试，未按设计限定为 `DataAccessException/TimeoutException`，不可治愈异常也消耗重试资源 — `RegistrationEventListener.java:36`
- [ ] E02. `TriageRecord` 同 sessionId 第二次分诊会违反 `@Column(unique = true)` 约束——`saveTriageRecord` 始终 `new TriageRecord()` 执行 INSERT，未先查已有记录做 UPDATE — `TriageServiceImpl.java:185-216`
- [ ] E03. `DoctorFacade` 跨模块调用完全无降级保护（确认同 C06）— `TriageServiceImpl.java:169-179`
- [ ] E04. 5 类 Phase 2/3 ChangeEvent 的事件定义 + 消费端 `@EventListener` + Caffeine invalidate 全部未实现（确认同 P04）— 全局

### [一般]
- [ ] E05. `RegistrationEventListener.recover()` 手工构造含 3 字段的 `HashMap` 作为 eventPayload，缺少完整事件对象序列化 — `RegistrationEventListener.java:50-57`
- [ ] E06. `DrugFacade` 在 prescription 模块未注入（确认同 P02）— 全局
