# R1.3: medical-record + ai-api + common-module-api 代码审查

审查时间：2026-07-01

### 审查范围

#### 第一部分：medical-record 模块（包D-AI2 病历生成）
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/api/MedicalRecordController.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/service/MedicalRecordService.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/service/impl/MedicalRecordServiceImpl.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/template/TemplateConfigManager.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/template/DatabaseTemplateConfigManager.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/template/DepartmentTemplateConfig.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/detector/MissingFieldDetector.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/detector/MissingFieldDetectorImpl.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/converter/MedicalRecordConverter.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/converter/MedicalRecordContentConverter.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/dto/RecordGenerateRequest.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/dto/RecordGenerateResponse.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/dto/FieldMissingHint.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/entity/MedicalRecord.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/entity/DeptTemplateConfig.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/task/VisitIdReconciledTask.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/exception/MedicalRecordErrorCode.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/enums/MedicalRecordField.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/repository/MedicalRecordRepository.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/repository/DeptTemplateConfigRepository.java`
- `AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/event/TemplateConfigChangeEvent.java`
- 测试：`src/test/java/com/aimedical/modules/medicalrecord/` 下全部测试

#### 第二部分：ai-api + ai-impl
- `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/AiService.java`
- `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/AiResult.java`
- `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/AiResultFactory.java`
- `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/degradation/DegradationContext.java`
- `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/degradation/DegradationStrategy.java`
- `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/triage/` (TriageRequest, TriageResponse, RecommendedDepartment, RecommendedDoctor, MatchedRuleItem, AdditionalResponseItem)
- `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/prescription/` (PrescriptionCheckRequest, PrescriptionCheckResponse, PrescriptionCheckItem, PatientInfo, PrescriptionAssistRequest, PrescriptionAssistResponse, AlertItem, DrugInteractionItem, SuggestionItem, DoseWarningItem, AllergyWarningItem, AllergyDetailItem, ExamResultItem)
- `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/medicalrecord/` (MedicalRecordGenRequest, MedicalRecordGenResponse)
- `AIMedical/backend/modules/ai/ai-impl/src/main/java/com/aimedical/modules/ai/impl/fallback/FallbackAiService.java`
- `AIMedical/backend/modules/ai/ai-impl/src/main/java/com/aimedical/modules/ai/impl/mock/MockAiService.java`
- `AIMedical/backend/modules/ai/ai-impl/src/main/java/com/aimedical/modules/ai/impl/mock/MockAdminController.java`
- 测试：`ai-api/src/test/` + `ai-impl/src/test/` 下全部测试

#### 第三部分：common-module-api
- `AIMedical/backend/modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/doctor/DoctorFacade.java`
- `AIMedical/backend/modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/doctor/AvailableDoctor.java`
- `AIMedical/backend/modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/drug/DrugFacade.java`
- `AIMedical/backend/modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/drug/DrugInfo.java`
- `AIMedical/backend/modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/store/SessionStore.java`
- `AIMedical/backend/modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/store/SuggestionStore.java`
- `AIMedical/backend/modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/store/DraftContextStore.java`
- `AIMedical/backend/modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/store/SuggestionStoreEntry.java`
- `AIMedical/backend/modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/store/impl/ConcurrentHashMapStore.java`
- `AIMedical/backend/modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/store/impl/DraftContextStoreImpl.java`
- `AIMedical/backend/modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/event/RegistrationEvent.java`
- `AIMedical/backend/modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/visit/VisitFacade.java`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/entity/DosageStandard.java`
- 测试：`common-module-api/src/test/` 下全部测试

### 发现

#### [一般] MedicalRecordServiceImpl.callAiWithTimeout 泛化异常处理，混淆超时/执行失败/中断语义

- **位置**：`medical-record/.../service/impl/MedicalRecordServiceImpl.java:145-159`
- **描述**：`callAiWithTimeout` 方法将 `TimeoutException`、`InterruptedException`、`ExecutionException` 三种异常统一处理为 `AiResultFactory.degraded("AI medical record generation timeout", "MR_GEN_AI_TIMEOUT", null)`。设计文档明确区分：(a) 超时失败使用 `AiResult.failure(errorCode, partialData)`，(b) 降级使用 `AiResult.degraded(fallbackReason, partialData)`。`ExecutionException` 表示 AI 执行异常（非超时），应用 `failure` 语义；`InterruptedException` 表示线程被中断，应在重设中断状态后返回 `failure`。当前实现用 `degraded` 语义配合 "timeout" 文本掩盖了实际异常类型，且 `partialData = null` 意味着超时场景下无法携带部分已生成字段。
- **建议**：
  - `TimeoutException`：返回 `AiResultFactory.failure("MR_GEN_AI_TIMEOUT", partialData)`，`partialData` 从上下文中获取
  - `ExecutionException`：返回 `AiResultFactory.failure("MR_GEN_AI_EXECUTION_ERROR", null)`
  - `InterruptedException`：`Thread.currentThread().interrupt()` 后返回 `AiResultFactory.failure("MR_GEN_AI_INTERRUPTED", null)`

#### [一般] MedicalRecordConverter.toRecordGenerateResponse 使用字面字符串比较错误码

- **位置**：`medical-record/.../converter/MedicalRecordConverter.java:69`
- **描述**：第 69 行 `"MR_GEN_AI_TIMEOUT".equals(aiResult.getErrorCode())` 使用硬编码字面量字符串比较。若 `AiResultFactory` 中 errorCode 拼写变更或 `MedicalRecordErrorCode` 枚举值改名，此处的字面量会静默失效。此外第 73 行将此逻辑作为 success 判定条件的一部分——超时场景被视为业务成功，这偏离了 `isSuccess()` 的原始语义。
- **建议**：使用枚举常量引用：`MedicalRecordErrorCode.MR_GEN_AI_TIMEOUT.getCode().equals(aiResult.getErrorCode())`。考虑在 `AiResult` 中增加 `isTimeout()` 或 `isDegraded()` 与 `isSuccess()` 的显式组合方法，避免在 Converter 中推导业务成功条件。

#### [一般] MockAiService.TIMEOUT 策略返回永不完成的 CompletableFuture，未使用延迟返回机制

- **位置**：`ai-impl/.../mock/MockAiService.java:67`
- **描述**：`TIMEOUT` 分支返回 `new CompletableFuture<>()`，创建一个永远不会完成的 Future。设计文档 §2.3 明确规定 TIMEOUT 模式应在固定延迟（`ai.mock.timeout-delay`，默认 8s）后返回 `AiResult.failure("MOCK_AI_TIMEOUT", partialData)`。当前实现导致调用方只能依赖业务层 timeout 参数触发超时，无法验证 partial data 传递路径。
- **建议**：使用 `CompletableFuture.supplyAsync(() -> { Thread.sleep(timeoutDelayMs); return AiResult.failure("MOCK_AI_TIMEOUT", emptyDto); })` 或 `ScheduledExecutorService.schedule()` 实现延迟完成。

#### [一般] FallbackAiService.applyStrategies 使用空 DegradationContext，策略无法感知操作上下文

- **位置**：`ai-impl/.../fallback/FallbackAiService.java:290-301`
- **描述**：`applyStrategies` 方法中新建 `new DegradationContext()` 后不设置任何字段，直接传递给 `DegradationStrategy.shouldDegrade(context)`。所有策略实现收到的 context 中 `serviceName = null`、`operationName = null`。这与各方法入口处（如 triage、generateMedicalRecord）构造的含上下文信息的 `DegradationContext`（如 `serviceName="medical-record"`、`operationName="generateMedicalRecord"`）不一致。策略实现无法基于操作类型做降级判定。
- **建议**：将各方法入口处已构造的 `DegradationContext` 实例传入 `applyStrategies` 方法，或重构为 `applyStrategies(AiResult<T> result, DegradationContext context)`。

#### [一般] MedicalRecordConverter.toFieldsMap 将元数据字段 PARTIAL_CONTENT 和 MISSING_FIELDS 放入 content 映射

- **位置**：`medical-record/.../converter/MedicalRecordConverter.java:29-47`
- **描述**：`toFieldsMap` 方法将 `PARTIAL_CONTENT`（AI 部分内容 JSON）和 `MISSING_FIELDS`（AI 自身标记的缺失字段列表）两个非医学内容字段写入 `Map<MedicalRecordField, String>`，该 Map 被双用途使用：(a) 持久化到 MedicalRecord.content_json 数据库列，(b) 供 MissingFieldDetector 检测字段缺失。对于用途(b)，当 DEFAULT_TEMPLATE 的 `requiredFields` 包含 `PARTIAL_CONTENT` 时——因为 `createDefaultTemplate()` 使用 `Arrays.stream(MedicalRecordField.values()).collect(Collectors.toSet())` 包含了全部 9 个枚举值，包括 `PARTIAL_CONTENT` 和 `MISSING_FIELDS`——每次检测都会将 `PARTIAL_CONTENT`（AI 正常返回时为 null）误报为缺失字段。`MISSING_FIELDS` 被转为逗号连接字符串也非正确语义。
- **建议**：将 `toFieldsMap` 拆分为两个方法：(a) `toContentMap` 仅包含 7 个业务字段用于持久化；(b) `toFieldValueMap` 包含全部字段用于缺失检测；或将 `MISSING_FIELDS` 和 `PARTIAL_CONTENT` 从 `MedicalRecordField` 枚举中分离为元数据常量，不在 DEFAULT_TEMPLATE 的 requiredFields 中引用。

#### [一般] MedicalRecordContentConverter 序列化失败时静默返回 null

- **位置**：`medical-record/.../converter/MedicalRecordContentConverter.java:29`
- **描述**：`convertToDatabaseColumn` 在 `objectMapper.writeValueAsString(plain)` 抛出异常时返回 `null`。若 `MedicalRecord.content` 列的 `columnDefinition = "TEXT"` 未设置 NOT NULL 约束（当前实体定义中 `content_json` 列没有 `nullable = false`），写入 null 将丢失已有内容。同时 `convertToEntityAttribute` 在解析失败时返回 `Collections.emptyMap()`，可能导致上游以为病历内容为空而覆盖生成。
- **建议**：异常时至少记录 WARN 日志；`convertToEntityAttribute` 异常时考虑保留原 `dbData` 而非静默返回空 Map。

#### [一般] MockAdminController 使用 POST+JSON body 而非设计约定的 GET+query param

- **位置**：`ai-impl/.../mock/MockAdminController.java:27-31`
- **描述**：设计文档 §2.3 约定切换端点格式为 `GET /api/admin/ai/mock/strategy?mode={STATIC|AI_UNAVAILABLE|TIMEOUT}`（查询参数方式），但实现使用 `POST /api/admin/ai/mock/strategy` 并接收 JSON body。REST 风格上，查询无副作用的当前策略用 GET 更合适，修改策略用 POST 也可接受，但与设计文档约定不一致。
- **建议**：与设计文档对齐——GET 查询当前策略（已有），POST 修改策略时接受 `?mode=` 查询参数或保持 JSON body 并更新设计文档。

#### [轻微] MissingFieldDetectorImpl.resolvePlaceholders 使用固定模板语法 {{fieldName}}，不可从数据库配置

- **位置**：`medical-record/.../detector/MissingFieldDetectorImpl.java:47-49`
- **描述**：占位符替换逻辑硬编码 `{{fieldName}}` 作为模板语法。`DepartmentTemplateConfig` 中的 `promptMessages` 和 `suggestedActions` 虽可从数据库配置，但占位符语法不可配置。若需要支持多语言或多变体模板，应支持可配置的占位符模式或引入模板引擎。
- **建议**：当前方式满足 Phase 2/3 需求，可在后续 Phase 中按需升级。

#### [轻微] RecordGenerateRequest.patientId 和 encounterId 缺少 @NotNull 校验

- **位置**：`medical-record/.../dto/RecordGenerateRequest.java:10-11`
- **描述**：`dialogueText` 有 `@NotNull @Size(min=50)` 校验，但 `patientId` 和 `encounterId` 没有 `@NotNull`。设计文档中 `encounterId` 是必填字段（映射为 VisitFacade 输入），`patientId` 也是必填。`encounterId` 在 Service 层有 null/empty 检查（返回 MR_GEN_VISIT_NOT_FOUND），但 `patientId` 无任何校验。
- **建议**：为 `patientId` 添加 `@NotBlank` 校验，为 `encounterId` 添加 `@NotNull` 校验，确保在 Controller 层尽早拒绝无效请求。

#### [轻微] SuggestionStoreEntry 接口定义位置与用途存在模糊

- **位置**：`common-module-api/.../store/SuggestionStoreEntry.java:1-9`
- **描述**：`SuggestionStoreEntry` 接口定义在 `store` 包下，但 `SuggestionStore` 的接口签名 `extends SessionStore<String, Object>` 使用泛型 `Object` 而非 `SuggestionStoreEntry`，导致 `SuggestionStoreEntry` 在接口层级未被使用。Prescription 模块的 `AiSuggestionResult` 也未引用此接口。该接口成为"孤儿"类型。
- **建议**：确认 `SuggestionStoreEntry` 的用途——若为 Phase 5 预留，添加明确注释说明；若当前不使用则移除或让 `SuggestionStore` 使用 `SuggestionStoreEntry` 替代 `Object` 作为值类型。

#### [轻微] DraftContextStoreImpl 未标记 @Component 或 @Repository

- **位置**：`common-module-api/.../store/impl/DraftContextStoreImpl.java`
- **描述**：`DraftContextStoreImpl` 使用 `@Service` 但不能被 `common-module-api` 的组件扫描覆盖（api 层通常不扫描 impl）。同时 `ConcurrentHashMapStore` 没有任何 Spring 注解。若 `common-module-api` 的包扫描路径未覆盖 `store.impl` 子包，这些 Bean 不会被自动注入。
- **建议**：确认 application 模块的 `@SpringBootApplication` 或 `@ComponentScan` 配置已覆盖 `com.aimedical.modules.commonmodule.store.impl` 包路径。否则 Bean 注入将失败。

### OOD 架构对齐度评估

| 设计要点 | 实现状态 | 说明 |
|---------|---------|------|
| stream=true → MR_GEN_STREAM_NOT_SUPPORTED | ✓ 正确 | Controller 第 28-30 行，在调用 Service 前终止 |
| VisitFacade 降级路径（超时/异常 → fallback encounterId） | ✓ 正确 | Service 第 136-142 行，返回 `VisitResolveResult(encounterId, true)` |
| VisitIdReconciledTask 定时回填 visitId | ✓ 正确 | 每 30 分钟扫描 `visitIdFallback=true` 的记录，使用 PESSIMISTIC_WRITE 锁 |
| TemplateConfigManager 兜底返回 DEFAULT | ✓ 正确 | `loadFromDatabase` 为空时返回 `DEFAULT_TEMPLATE` |
| 事件驱动 + Caffeine 定时刷新双重失效策略 | ✓ 正确 | DatabaseTemplateConfigManager 监听 TemplateConfigChangeEvent + refreshAfterWrite=60s |
| MissingFieldDetector = 差集比对 + FieldMissingHint 组装 | ✓ 正确 | 遍历 requiredFields 检查 fieldsMap 中值是否空/空白 |
| AiResult 五字段结构 (success/data/errorCode/degraded/fallbackReason) | ✓ 正确 | 符合设计 §2.3 契约 |
| AiResultFactory 重载工厂（不与 AiResult 原始签名冲突） | ✓ 正确 | failure/degraded/success 重载方法均存在 |
| MockAiService 三策略 + volatile + @Profile("mock") | Δ 部分偏差 | TIMEOUT 策略未按设计实现延迟返回 |
| DoctorFacade/DrugFacade/VisitFacade 接口定义在 common-module-api | ✓ 正确 | 符合跨模块门面模式 |
| SessionStore/SuggestionStore/DraftContextStore 抽象层 | ✓ 正确 | 符合 Store 隔离设计 |
| RegistrationEvent 含 sessionId 字段 | ✓ 正确 | 匹配设计 §2.2 跨模块事件传递机制 |
| DosageStandard 迁移至 common/entity | ✓ 正确 | 跨模块共享 |

### 测试覆盖

**medical-record 模块测试**: 优 —— `MedicalRecordServiceImplTest` 覆盖 null/empty encounterId、VisitFacade 超时/异常、AI 超时、正常流程、乐观锁冲突、update/insert 双路径、医生 ID 写入、缺失字段检测等 13 个用例；`MedicalRecordControllerTest` 覆盖 stream=true 阻断、success/fail 响应。转换器、DTO、实体、任务均有独立测试。

**ai-api 模块测试**: 良 —— AiResult、AiResultFactory、AiService、DegradationStrategy 均有测试；MockAiService/FallbackAiService 有独立测试；DTO 测试覆盖 triage/prescription/medicalrecord 三类。

**common-module-api 测试**: 良 —— DoctorFacade、AvailableDoctor、DrugFacade、DrugInfo、VisitFacade、ConcurrentHashMapStore、DraftContextStoreImpl、RegistrationEvent 均有测试。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 0 |
| 一般 | 7 |
| 轻微 | 4 |

### 总评

三个模块的代码整体与 OOD 设计对齐度高。核心业务路径（stream 阻断、VisitFacade 降级、AI 超时降级、模板配置兜底、缺失字段检测）均已按设计正确实现，异常路径（乐观锁冲突、VisitFacade 不可用）有妥善处理。测试覆盖全面，关键边界场景均有单元测试覆盖。主要问题集中在 `MedicalRecordServiceImpl.callAiWithTimeout` 的异常处理语义混淆（所有异常类型统一用 "timeout" + "degraded" 处理）、`MedicalRecordConverter` 中错误码字符串比较的脆弱性、`MockAiService` 中 TIMEOUT 策略实现与设计不符（Future 永不完成）、以及 `FallbackAiService.applyStrategies` 的空 Context 问题。`MissingFieldDetector` 因 `toFieldsMap` 包含元数据字段存在潜在误报风险。整体代码质量良好，建议优先修复 7 个一般性问题。
