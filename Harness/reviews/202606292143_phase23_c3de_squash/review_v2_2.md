# R2.2: AI 集成 + 降级策略 — 横切关注点深度审查

审查时间：2026-06-29
依据设计文档：`Docs/07_ood_phase2_C_3_DE.md` §1.1c / §2.3 / §3.1-§3.4 / §7

## 审查范围

### AI 抽象层（ai-api 模块）
- `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/AiService.java`
- `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/AiResult.java`
- `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/AiResultFactory.java`
- `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/degradation/DegradationStrategy.java`
- `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/degradation/DegradationContext.java`
- `AIMedical/backend/modules/ai/ai-api/src/test/java/com/aimedical/modules/ai/api/AiResultFactoryTest.java`
- `AIMedical/backend/modules/ai/ai-api/src/test/java/com/aimedical/modules/ai/api/AiResultTest.java`
- `AIMedical/backend/modules/ai/ai-api/src/test/java/com/aimedical/modules/ai/api/AiServiceTest.java`
- `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/triage/*.java`
- `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/prescription/*.java`
- `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/medicalrecord/*.java`

### ai-impl 模块
- `AIMedical/backend/modules/ai/ai-impl/src/main/java/com/aimedical/modules/ai/impl/mock/MockAiService.java`
- `AIMedical/backend/modules/ai/ai-impl/src/main/java/com/aimedical/modules/ai/impl/fallback/FallbackAiService.java`
- `AIMedical/backend/modules/ai/ai-impl/src/main/java/com/aimedical/modules/ai/impl/degradation/NoOpDegradationStrategy.java`
- `AIMedical/backend/modules/ai/ai-impl/src/test/java/com/aimedical/modules/ai/impl/mock/MockAiServiceTest.java`

### 业务层 AI 调用
- `AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/service/impl/TriageServiceImpl.java`
- `AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/converter/TriageConverter.java`
- `AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/dialogue/DialogueSession.java`
- `AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/dto/TriageResponse.java`
- `AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/dto/DialogueCreateRequest.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/service/audit/impl/PrescriptionAuditServiceImpl.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/service/assist/impl/PrescriptionAssistServiceImpl.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/service/assist/DedupTaskScheduler.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/converter/AuditConverter.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/converter/AssistConverter.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/dto/assist/AiSuggestionResult.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/service/impl/MedicalRecordServiceImpl.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/converter/MedicalRecordConverter.java`

### 配置与上下文
- `AIMedical/backend/application/src/main/resources/application.yml`

---

## 发现

### [严重] 1. AiResultFactory 在业务层完全未被使用 — 设计意图失效

**位置**：
- `AIMedical/backend/modules/consultation/.../TriageServiceImpl.java:87,90-93,161-167`
- `AIMedical/backend/modules/medical-record/.../MedicalRecordServiceImpl.java:138-163`
- `AIMedical/backend/modules/prescription/.../PrescriptionAuditServiceImpl.java:79-87`
- `AIMedical/backend/modules/prescription/.../PrescriptionAssistServiceImpl.java:76-92`
- `AIMedical/backend/modules/ai/ai-impl/.../FallbackAiService.java:63,190`

**描述**：设计文档 §1.1c / §2.3 / §7 明确说明，新增 `AiResultFactory` 的目的是为了在不修改 `AiResult.java` 原始签名的前提下提供 `failure(String errorCode, T partialData)` 和 `degraded(String fallbackReason, T partialData)` 重载方法，让业务层统一使用工厂创建降级/超时结果，从而保留 Phase 5 包G 的兼容约束。全项目 grep `AiResultFactory\.` 在 main 代码中匹配 0 处（仅测试代码 `AiResultFactoryTest.java` 中使用）。具体表现：
- `MedicalRecordServiceImpl.callAiWithTimeout` 三处超时分支（`TriageServiceImpl.java:139-163`）直接 `new AiResult<>()` 并手动调用 setter 设置 success/errorCode/degraded/data，绕开了工厂。
- `TriageServiceImpl.handleAiFailure` 使用的是 `AiResult.degraded(fallbackReason)`（AiResult.java 上的旧签名），而非 `AiResultFactory.degraded(fallbackReason, partialData)`。
- `FallbackAiService.handleEmptyDelegates()` 调用的是 `AiResult.degraded`，未使用 `AiResultFactory`。
- 业务层需要的 `failure(...)` 异步降级路径完全不存在。

**建议**：
1. 重构 `MedicalRecordServiceImpl.callAiWithTimeout` 使用 `AiResultFactory.failure("MR_GEN_AI_TIMEOUT", partialData)`；
2. 重构 `TriageServiceImpl.handleAiFailure` 使用 `AiResultFactory.degraded(fallbackReason, partialData)`（partialData 为空 T 时传入 `null`）；
3. 重构 `FallbackAiService` 的 `handleEmptyDelegates` 与 `applyStrategies`；
4. 业务流程的降级后续应保留 partialData 流转能力。

---

### [严重] 2. AI 超时配置完全未外化，硬编码或缺失

**位置**：
- `AIMedical/backend/modules/consultation/.../TriageServiceImpl.java:87` — `future.get()` 无超时
- `AIMedical/backend/modules/prescription/.../PrescriptionAuditServiceImpl.java:81` — `future.get()` 无超时
- `AIMedical/backend/modules/prescription/.../PrescriptionAssistServiceImpl.java:78` — `.get()` 无超时
- `AIMedical/backend/modules/medical-record/.../MedicalRecordServiceImpl.java:138` — `future.get(12, TimeUnit.SECONDS)` 硬编码
- `AIMedical/backend/modules/medical-record/.../MedicalRecordServiceImpl.java:124` — `future.get(2, TimeUnit.SECONDS)` 硬编码

**描述**：设计文档 §2.3 / §5.5 明确要求所有 AI 调用超时通过 `@Value("ai.timeout.triage")` / `@Value("ai.timeout.prescription-audit")` / `@Value("ai.timeout.medical-record-generate")` / `@Value("ai.timeout.prescription-assist")` 注入（默认值 8s/6s/12s/10s），并将 `consultation.doctor-facade.timeout` / `medical-record.visit-facade.timeout` / `prescription.drug-facade.timeout` 也通过 `@Value` 注入（默认 2s）。当前实现：
1. 全 backend 无任何 `@Value("ai.timeout.*")` 注入；
2. `TriageServiceImpl`、`PrescriptionAuditServiceImpl`、`PrescriptionAssistServiceImpl` 使用 `future.get()` 无限等待——若 AI 实现 Hang/慢响应将无限期阻塞主请求线程，雪崩风险严重；
3. `MedicalRecordServiceImpl` 将 `12s` 与 `2s` 直接硬编码在源码中，无法通过 `application.yml` 调整。

**建议**：
1. 在四个 Service 实现类中分别注入 `@Value("${ai.timeout.triage:8000}")`、`@Value("${ai.timeout.prescription-audit:6000}")`、`@Value("${ai.timeout.prescription-assist:10000}")`、`@Value("${ai.timeout.medical-record-generate:12000}")` long 字段；
2. 将 `future.get(timeoutMs, TimeUnit.MILLISECONDS)` 替换 `future.get()`；
3. 同步为 `VisitFacade` 调用注入 `@Value("${medical-record.visit-facade.timeout:2000}")`、`DoctorFacade` / `DrugFacade` 使用统一配置键。

---

### [严重] 3. AiSuggestionResult 异步 AI 流程不完整 — 5 状态映射全部缺失

**位置**：
- `AIMedical/backend/modules/prescription/.../service/assist/DedupTaskScheduler.java:21-44`
- `AIMedical/backend/modules/prescription/.../service/assist/impl/PrescriptionAssistServiceImpl.java:67-188`

**描述**：设计文档 §3.4 AiSuggestionResult 与 §4.4 异步建议查询端点定义了完整的 5 状态映射表（正常完成/降级完成/明确失败/超时/DEGRADED+partialData），并要求 `DedupTaskScheduler` 创建 `PENDING` 预创建条目后，异步调用 `AiService.prescriptionAssist()` 完成时填充 `suggestion/failReason/partialData` 字段并通过 `SuggestionStore.compute()` 原子写入。当前实现：
1. `DedupTaskScheduler.schedule` 仅创建一条 `AiSuggestionResult`（`status=PENDING`），未触发任何异步 AI 调用（无 `@Async`、无 `CompletableFuture.runAsync`、无 `Thread.start`）；
2. 全 backend grep `setSuggestion\(|setFailReason\(|setPartialData\(` 在 main 代码中匹配 0 处——没有任何代码将异步 AI 结果回写到 `AiSuggestionResult`；
3. `check-dose` 主流程直接同步返回 taskId，但 `AiSuggestionResult` 永远停留在 `PENDING` 状态，导致前端 `GET /assist/suggestion/{taskId}` 进入死循环/超时；
4. 设计"DEGRADED 归入 COMPLETED 语义"映射规则完全未实现。

**建议**：新增异步任务执行组件（例如 `AsyncAiAssistExecutor` 或在 `DedupTaskScheduler.schedule` 内追加 `CompletableFuture.runAsync(...)` 链式调用），实现以下映射：

| AiResult 状态 | 判定 | 写入字段 |
|--------------|------|---------|
| success=true, !degraded | COMPLETED | suggestion=data JSON、failReason=null、partialData=null |
| success=true, degraded=true | COMPLETED | suggestion=data JSON、failReason=null、partialData=null |
| success=false, !degraded | FAILED | suggestion=null、failReason=fallbackReason/errorCode 描述、partialData=若 data 非空则 JSON 序列化 |
| 超时（success=false, partialData 非空） | FAILED | suggestion=null、failReason=timeout 描述、partialData=JSON 序列化 |
| success=false, degraded=true, partialData 非空 | COMPLETED | suggestion=partialData 序列化 |

通过 `suggestionStore.compute(taskId, (k, existing) -> updatedResult)` 原子替换。

---

### [严重] 4. correctedChiefComplaint 透传逻辑缺失 — 主诉修正"显式路径"未生效

**位置**：`AIMedical/backend/modules/consultation/.../converter/TriageConverter.java:24`

**描述**：设计文档 §3.1 主诉修正支持明确定义：TriageConverter 在调用 AiService.triage() 前，若 `session.getCorrectedChiefComplaint()` 非空，则以其替换原始 `chiefComplaint` 作为推理上下文起点。当前 `TriageConverter.toAiTriageRequest` 第 24 行无条件将 `request.getChiefComplaint()` 写入 `aiRequest.setChiefComplaint()`，从未读取 `session.getCorrectedChiefComplaint()`。后果：前端提交 `correctedChiefComplaint` 字段后被静默忽略，AI 仍然基于原始主诉推理。

**建议**：在 `TriageConverter.toAiTriageRequest` 第 24 行改为 `aiRequest.setChiefComplaint(session.getCorrectedChiefComplaint() != null ? session.getCorrectedChiefComplaint() : request.getChiefComplaint())`。

---

### [严重] 5. MockAiService 行为契约与设计严重不符 — 切换机制、Profile、Controller 均缺失

**位置**：`AIMedical/backend/modules/ai/ai-impl/.../mock/MockAiService.java`

**描述**：设计文档 §2.3 "MockAiService 实现契约" 定义了完整的策略机制：
1. 通过 `@Profile("mock")` 激活；
2. 支持三种返回模式 `STATIC`/`AI_UNAVAILABLE`/`TIMEOUT`，通过 `ai.mock.response-strategy` 切换；
3. 通过 `GET /api/admin/ai/mock/strategy?mode=...` 端点（注册于 `MockAdminController`）运行时动态切换；
4. 策略字段使用 `volatile` 保证可见性，正在执行的调用读取调用时刻的策略快照值；
5. `TIMEOUT` 模式在 `ai.mock.timeout-delay=8s` 后返回 `AiResult.failure("MOCK_AI_TIMEOUT", partialData)`。

当前实现：
1. `MockAiService` 仅在方法上标注 `@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "true", matchIfMissing = true)`，未使用 `@Profile("mock")`，与设计要求不一致；
2. 所有 13 个方法一律返回 `AiResult.success(new XxxResponse())`，无 STATIC/AI_UNAVAILABLE/TIMEOUT 三模式支持；
3. 全 backend grep `MockAdminController|ResponseStrategy|ai.mock.response-strategy|ai.mock.timeout-delay|application-mock.yml` 全部 0 命中——切换端点、配置键、profile 配置文件全部缺失；
4. `MockAiServiceTest` 仅断言"返回 mock 数据"，未覆盖三种策略模式。

**建议**：按设计 §2.3 实现完整 Mock 契约，包括 `MockAdminController`、`ResponseStrategy` 枚举、volatile 策略字段、profile 隔离与配置键。

---

### [严重] 6. DegradationStrategy / DegradationContext 是空壳 — FallbackAiService.applyStrategies 永远降级

**位置**：
- `AIMedical/backend/modules/ai/ai-api/.../degradation/DegradationStrategy.java:3-6`
- `AIMedical/backend/modules/ai/ai-api/.../degradation/DegradationContext.java:3-7`
- `AIMedical/backend/modules/ai/ai-impl/.../fallback/FallbackAiService.java:183-194`
- `AIMedical/backend/modules/ai/ai-impl/.../degradation/NoOpDegradationStrategy.java`

**描述**：设计文档 §1.2 / §1.1c 假设 Phase 5 底座 `ai-impl` 内部存在 `AiOrchestrator → CapabilityExecutor → ModelRouter/LlmChatService` 多层管线，并通过 `FallbackAiService` 等组件组合不同的 `DegradationStrategy`。当前实现：

1. `DegradationStrategy` 仅有 `shouldDegrade(DegradationContext)` 一行接口签名；`DegradationContext` 是空类（无任何字段）；
2. `NoOpDegradationStrategy`（虽未读取）作为空实现；
3. `FallbackAiService.applyStrategies` 第 184 行对 success 或 degraded 结果直接返回原结果，第 188-190 行遍历策略列表，对每个 `strategy.shouldDegrade(new DegradationContext())` 调用——`DegradationContext` 为空时所有策略实现只能返回 hard-coded false/true；
4. 由于 `applyStrategies` 总是从单一 delegate `delegates.get(0)` 取调用，没有任何重试链、降级链或管线路由——与设计"多层管线"假设不一致；
5. `FallbackAiService` 只取 `delegates.get(0)`，未实现按策略列表的扇出或回退。

**建议**：实现 `DegradationContext` 至少携带 `errorCode/latency/exception/healthScore` 等字段，`DegradationStrategy` 实现如 `TimeoutBasedStrategy`、`ErrorRateStrategy` 等具体降级触发器；同时让 `FallbackAiService` 按优先级遍历 delegate 而非仅取首个。

---

### [一般] 7. AiResult.success(null) 违反 §2.3 success=true → data 非 null 契约

**位置**：`AIMedical/backend/modules/ai/ai-api/.../AiResultFactory.java:20`、`AIMedical/backend/modules/ai/ai-api/.../AiResult.java:22`、`AIMedical/backend/modules/ai/ai-api/src/test/.../AiResultFactoryTest.java:69-74`、`AiResultTest.java:40-44`

**描述**：设计文档 §2.3 "契约约束"明确："当 success=true 时 data 必须非 null（AI 成功返回有效结果）；data=null 仅允许在 success=false 场景下出现。"但：
1. `AiResult.success(T data)` 与 `AiResultFactory.success(T data)` 均允许 `data=null` 调用（无 `Objects.requireNonNull`）；
2. 测试 `shouldCreateSuccessResultWithNullData`（AiResultTest 40-44 / AiResultFactoryTest 69-74）反而断言 `AiResult.success(null)` 合法——测试反向强化了违反契约的行为；
3. 若未来 mock AI 返回 `success=true, data=null`，业务层按契约直接解引用将触发 NPE。

**建议**：
1. 修改 `AiResult.success` 与 `AiResultFactory.success` 在 data == null 时抛出 `IllegalArgumentException("success requires non-null data")`；
2. 删除 `shouldCreateSuccessResultWithNullData` 与 `AiResultFactoryTest.shouldCreateSuccessResultWithNullData` 两个违反契约的测试用例，或改为参数化测试（期望抛异常）。

---

### [一般] 8. 降级 fallback 文案硬编码英文 — 与需求文档 3.1.3.1 不一致

**位置**：`AIMedical/backend/modules/consultation/.../TriageServiceImpl.java:127,132`

**描述**：设计文档 §3.1 与设计决策表"降级时前端行为"定义 fallback 文案应为中文——"AI 分诊暂时不可用" / "建议直接联系线下接诊窗口"。当前 `TriageServiceImpl.java:127` 使用 `"AI service unavailable, using rule engine fallback"`、`:132` 使用 `"AI service has been continuously unavailable"` 全为英文。`fallbackHint` 设计 §3.1 描述为"AI 连续失败 3 次时携带\"建议直接联系线下接诊窗口\"兜底文案"——文案与具体触发条件不符合。

**建议**：将两处硬编码英文文案替换为中文（或抽到 `MessageSource` 配置项）：
- `reason` 默认："根据症状分析，建议就诊XX科室"（其中 XX 由规则引擎/兜底科室填充）；
- `fallbackHint`：`session.getAiFailCount() >= 3` 时填充 "建议直接联系线下接诊窗口"。

---

### [一般] 9. AiResult.isSuccess() && data == null 的健壮性检查不一致

**位置**：
- `TriageServiceImpl.java:98` — `aiResult != null && aiResult.isSuccess() && aiResult.getData() != null`
- `MedicalRecordServiceImpl.java:90,98` — `aiResponse != null`（基于 `aiResult.getData()`）
- `PrescriptionAuditServiceImpl.java:92` — `aiResult != null && aiResult.isSuccess() && aiResult.getData() != null`
- `PrescriptionAssistServiceImpl.java:86-90` — `aiResult.isSuccess() && aiResult.getData() != null`

**描述**：虽然 §2.3 契约规定 `success=true → data 非 null`，但所有业务层调用点都额外做 `&& getData() != null` 的健壮性检查，说明调用方已预期实现可能违反契约。这种"契约信任+空指针兜底"的混合风格源于文档未提供 `Optional` 化或强制 null-check 的明确指导——若 §2.3 契约被严格执行（参见本文档第 7 条），业务层冗余检查可移除；反之，应对契约做弱化（`success=true → data 可能为 null`）。

**建议**：与第 7 条统一决策。建议路径是在 `AiResult.success` 中强制 data 非 null（与设计文档契约一致），业务层 `&& getData() != null` 检查可移除。

---

### [一般] 10. AuditConverter.toAuditResponse 弱化为仅返回 PASS

**位置**：`AIMedical/backend/modules/prescription/.../converter/AuditConverter.java:48-56`

**描述**：当 `aiData == null` 时，`AuditConverter.toAuditResponse` 第 50 行返回 `AuditRiskLevel.PASS` 与空 alerts 列表。但 null data 应当意味着 AI 调用不可用（fallback 路径），不应直接给出 PASS。此路径被 `PrescriptionAuditServiceImpl.audit` 上层判断（`aiResult.isSuccess() && aiData == null`）短路而不会走到 Converter——但作为 Converter 独立可调用组件，第 50 行的默认行为是"不区分 AI 不可用与 PASS"，语义不安全。`response.setFromFallback(aiResult.isDegraded())` 也仅在 null data 路径设置，未与上层 `fromFallback` 同步。

**建议**：在 Converter 上添加 `@VisibleForTesting` 注释或 Assert 校验调用前提（aiData 非 null）；或者在 null data 路径改为抛 `IllegalStateException`，由上层 Catch 后转 fallback。

---

### [一般] 11. AiService 共有 13 方法，但任务清单仅要求验证 4 个 — 接口签名扩展部分未在范围

**位置**：`AIMedical/backend/modules/ai/ai-api/.../AiService.java:32-58`

**描述**：当前 `AiService` 包含 13 个方法（triage/diagnosis/prescriptionCheck/generateMedicalRecord/analysisReportForInspection/analysisReportForLabTest/imageAnalysis/knowledgeBaseQuery/recommendExamination/prescriptionAssist/recommendExecutionOrder/schedule/discussionConclusion）。设计文档 §2.3 仅声明 4 个目标方法（triage/prescriptionCheck/generateMedicalRecord/prescriptionAssist），本次审查范围也明确聚焦这 4 个。但实际接口还包含 diagnosis 等 9 个额外方法——本轮审查不评估这 9 个方法的契约一致性，仅记录"接口范围超出本设计 §2.3 定义"。此现象不影响 Phase 5 兼容性（Phase 5 包G 同样支持 4 个目标方法，多余方法对业务模块透明）。

**建议**：在下一轮 R2.3 / R3 中可针对 diagnosis 等其他 9 个方法单独审查其对应业务模块的覆盖度（如 imageAnalysis 是否调用方实现、Doctor 端是否引用 knowledgeBaseQuery 等）；或确认这 9 个方法是否属于"Phase 5 + 后续阶段预留"，业务模块当前不依赖。

---

### [一般] 12. DedupTaskScheduler 仅同步预创建，无并发安全 verify

**位置**：`AIMedical/backend/modules/prescription/.../DedupTaskScheduler.java:25-41`

**描述**：设计文档 §3.4 要求"createIfNotExists"在存储层原子完成（Phase 2/3 用 `ConcurrentHashMap.compute`，Phase 5 用 Lua 脚本）。当前 `DedupTaskScheduler.schedule` 使用 `suggestionStore.compute(...)` 完成原子合并，**逻辑与契约一致**。但：
1. 接口方法名 `compute` 而非 `createIfNotExists`，与设计文档 §3.4 命名不一致；
2. `AiSuggestionResult candidateTaskId = UUID.randomUUID().toString()`（每次都生成 UUID）后即使最终走"复用已有 task"，新生成的 UUID 已丢失——此为可接受的副作用（hash 检查一般能命中复用路径），但代码可读性差；
3. 测试 `DedupTaskSchedulerTest` 的 `shouldCreateNewTaskWhenNoExisting` 验证时 `verify(suggestionStore, times(1)).put(anyString(), any(AiSuggestionResult.class))` 但 `newResult` 中的 `taskId` 是固定 UUID，无法在测试中断言具体值——测试覆盖率有缺口。

**建议**：方法名保留 `compute`，但 JavaDoc 注明与设计 §3.4 `createIfNotExists` 对应；或实现 `SuggestionStore.createIfNotExists(...)` 的方法并在 `DedupTaskScheduler` 内调用以提升语义显式性。

---

### [一般] 13. 应用配置缺失 ai.mock / ai.timeout / facade.timeout 等配置项

**位置**：`AIMedical/backend/application/src/main/resources/application.yml:1-14`

**描述**：当前 `application.yml` 仅包含 `spring.application.name`、`spring.profiles.active: phase1,dev`、`server.port`、`jwt.*` 配置块，**完全缺失**：
1. `ai.timeout.triage=8s` / `prescription-audit=6s` / `medical-record-generate=12s` / `prescription-assist=10s`；
2. `consultation.doctor-facade.timeout=2s` / `medical-record.visit-facade.timeout=2s` / `prescription.drug-facade.timeout=2s`；
3. `triage.max-context-chars=3000`；
4. `ai.mock.enabled` / `ai.mock.response-strategy` / `ai.mock.timeout-delay`。

`application-mock.yml` 文件本身就不存在。

**建议**：在 `application.yml` 中补充完整的 ai.* / facade.* 配置块（以默认值初始化，作为运行期调整入口），并新增 `application-mock.yml` 用于 Mock profile。

---

### [轻微] 14. TriageServiceImpl.saveTriageRecord JSON 序列化异常被吞掉

**位置**：`AIMedical/backend/modules/consultation/.../TriageServiceImpl.java:212-214`

**描述**：`saveTriageRecord` 第 212-214 行 `catch (JsonProcessingException e) { // ignore serialization errors for optional JSON fields }` 静默吞掉异常。虽然 JSON 字段是 best-effort，但完全无日志会让运营期问题排查困难。建议改为 `log.warn("failed to serialize JSON field", e)`。

---

### [轻微] 15. Consultation 模块的 DoctorFacade 调用无超时保护

**位置**：`AIMedical/backend/modules/consultation/.../service/impl/TriageServiceImpl.java:172`

**描述**：第 172 行 `doctorFacade.findAvailableDoctorsByDepartment(dept.getDepartmentId())` 同步调用，无超时控制也无 `try/catch`。设计文档 §3.1 "DoctorFacade 跨模块调用降级保护"明确定义 2s 超时 + 异常时 doctors 置空。当前实现完全未对齐——若 DoctorFacade 实现异常/挂起，分诊主流程将无限等待。

**建议**：要么改造 DoctorFacade 接口本身具备超时能力（Feign/RestTemplate 配置），要么在 `findDoctorsForDepartments` 外层 try/catch + future.get(2s) 模式。

---

### [轻微] 16. AiService 13 方法在 Mock 中全部硬编码 success — 返回数据缺失真实字段值

**位置**：`AIMedical/backend/modules/ai/ai-impl/.../MockAiService.java:44-111`

**描述**：第 94-96 行 `prescriptionAssist` 返回 `AiResult.success(new PrescriptionAssistResponse())` 是空对象——`prescriptionDraft=null` 而非 JSON 串。`PrescriptionAssistServiceImpl.assist` 第 94 行调用 `hasDrugsInDraft(aiData.getPrescriptionDraft())` 会因 null 进入 JSON 解析失败的 catch 直接返回 false，进入 `RX_ASSIST_AI_NO_RECOMMENDATION` 分支。Mock 不应误触发"无可推荐药品"——业务集成测试无法跑通正常路径。

**建议**：在 Mock 中填充 `prescriptionDraft = "{\"drugs\":[{\"drugId\":\"DRG001\",...}]}"` 等标准 mock 数据，至少做到让业务层能跑通 happy-path。

---

### [轻微] 17. TriageConverter 未透传 correctedChiefComplaint 至 ai-api TriageResponse 主诉修正隐式路径

**位置**：`AIMedical/backend/modules/consultation/.../converter/TriageConverter.java:54-90`

**描述**：设计文档 §3.1 "主诉修正识别策略 (b) 隐式路径" 要求 ai-api 层 `TriageResponse.correctedChiefComplaint` 字段应在 AI 推理输出时由 AI 填充，由 TriageServiceImpl 检测后写入 DialogueSession。当前 `TriageConverter.toTriageResponse` (54-90 行) 完全未读取/写入 ai-api `TriageResponse.correctedChiefComplaint`（ai-api 第 16 行字段已存在）。后果：AI 输出的主诉修正信号被静默忽略。

**建议**：`TriageConverter.toTriageResponse` 在转换 business TriageResponse 时同步设置 correctedChiefComplaint（来自 aiData.getCorrectedChiefComplaint()），让上层 TriageServiceImpl 能够检测。

---

### [轻微] 18. AuditConverter.toAiPrescriptionCheckRequest 缺少 patientInfo 的疾病/合并症域映射

**位置**：`AIMedical/backend/modules/prescription/.../converter/AuditConverter.java:77-92`

**描述**：`toAiPatientInfo` 转换 patientId/age/gender/allergyHistory/comorbidities/allergyDetails 字段基本完整。但 ai-api 的 `PatientInfo.comorbidities` 字段类型应与业务 `PatientInfo.comorbidities` 完全一致（List<String>），当前看转换正确。**问题不在字段而在顺序/一致性**：业务层 `AllergyDetail.severity` 是枚举，转换后通过 `severity.name()` 转为 String；ai-api `AllergyDetailItem.severity` 字段类型若也是 String 则 OK。但全项目未确认 ai-api 与业务层 `severity` 字段的具体类型匹配——属本轮范围之外的下游验证项。

**建议**：在下一轮 R3.1（DTO 字段对齐审查）专项验证 `severity` 类型一致性（枚举 vs String）。

---

## 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 6 |
| 一般 | 8 |
| 轻微 | 4 |

## 总评

本轮审查聚焦 AI 集成 + 降级策略的契约对齐。结果显示：

**契约完整性方面**：(a) `AiResult.java` 原始签名未修改，Phase 5 兼容性 §1.1c 满足；(b) `AiResultFactory` 4 个静态工厂方法与 §2.3 规范完全一致（类级测试 `AiResultFactoryTest` 覆盖完整）；(c) ai-api 层 DTO（分诊/审核/病历/辅助开方）字段与设计 §10 一致。

**但实现层严重背离设计意图**：
1. `AiResultFactory` 在 4 个业务 Service 实现中**零引用**——`MedicalRecordServiceImpl` 手动 `new AiResult<>()` + setter；`TriageServiceImpl.handleAiFailure` 使用旧 `AiResult.degraded`；`PrescriptionAuditServiceImpl.audit` 超时时直接 `aiResult = null` 丢失错误码语义；`PrescriptionAssistServiceImpl.assist` 同样为 null。这使得 §1.1c 兼容性方案的核心代码（工厂模式）沦为纯接口死代码。
2. **超时配置 0 外化**——四个业务 Service 均未通过 `@Value` 注入 `ai.timeout.*`，导致 §5.5 表中所有超时配置成为文档承诺而无运行时保障；其中三处完全无超时（`future.get()` 无限等待）将引发线程雪崩风险。
3. **异步 AI 流程断链**——`DedupTaskScheduler.schedule` 仅创建 `PENDING` 条目后即返回，全工程无任何代码将异步 AI 结果回填到 `AiSuggestionResult.suggestion/failReason/partialData` 字段，§3.4 定义的 5 状态映射表完全未实现，`GET /assist/suggestion/{taskId}` 端点将永远等待。
4. **MockAiService / FallbackAiService 是空壳**——与 §2.3 实现契约严重不符（无 Profile/Strategy/Controller/配置键），同时 `DegradationStrategy/Context` 接口无实质内容，无法支撑 Phase 5 多层管线假设。
5. **correctedChiefComplaint 显式路径未生效**（`TriageConverter` 不读取 session 中修正值）；fallback 文案为硬编码英文而非设计指定的中文文案。

**契约本身的小瑕疵**：
- `AiResult.success(null)` 违反 §2.3 "success=true → data 非 null" 契约，测试反向强化违规行为，建议在工厂中抛异常并删除违例测试。
- `AuditConverter.toAuditResponse` 在 aiData 为 null 时退化为 PASS+空 alerts 而非 fallback 路径，语义不安全。

总体而言，**设计文档的核心架构（AiService 接口/AiResult 工厂/AiResult 五字段契约）骨架正确**，但实现层在超时控制、降级落地、异步 AI 闭环三处存在严重缺口，需在 Phase 2/3 上线前补齐。建议：
- 修复 issue 1（强制使用 AiResultFactory）/ issue 2（@Value 外化超时）/ issue 3（异步 AI 闭环回填）/ issue 5（MockAiService 完整实现）/ issue 6（DegradationStrategy 实质化）后方可进入集成测试阶段；
- issue 4 / 8 / 16 等影响 happy-path 演示的问题需在 mock profile 启用前解决。
