# R2.3: 深度审查 medical-record 模块 + ai-impl fallback 层

审查时间：2026-07-01

### 审查范围

```
AIMedical/backend/modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/
├── service/impl/MedicalRecordServiceImpl.java
├── converter/MedicalRecordConverter.java
├── converter/MedicalRecordContentConverter.java
├── entity/MedicalRecord.java
├── enums/MedicalRecordField.java
├── exception/MedicalRecordErrorCode.java
├── detector/MissingFieldDetector.java
├── detector/MissingFieldDetectorImpl.java
├── template/DepartmentTemplateConfig.java
├── template/TemplateConfigManager.java
├── template/DatabaseTemplateConfigManager.java
├── api/MedicalRecordController.java
├── dto/RecordGenerateRequest.java
├── dto/RecordGenerateResponse.java
├── dto/FieldMissingHint.java
└── repository/MedicalRecordRepository.java

AIMedical/backend/modules/ai/ai-impl/src/main/java/com/aimedical/modules/ai/impl/
├── fallback/FallbackAiService.java
├── mock/MockAiService.java
└── degradation/NoOpDegradationStrategy.java

AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/
├── AiService.java
├── AiResult.java
├── AiResultFactory.java
├── degradation/DegradationContext.java
├── degradation/DegradationStrategy.java
└── dto/medicalrecord/MedicalRecordGenResponse.java

AIMedical/backend/modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/
├── store/DraftContextStore.java
├── store/SessionStore.java
├── store/SuggestionStore.java
├── store/impl/DraftContextStoreImpl.java
├── store/impl/ConcurrentHashMapStore.java
└── visit/VisitFacade.java
```

### 发现

#### [严重] DEFAULT_TEMPLATE 将元数据字段纳入 requiredFields

- **位置**：`DatabaseTemplateConfigManager.java:111`
- **描述**：`createDefaultTemplate()` 使用 `MedicalRecordField.values()` 收集全部枚举值作为 requiredFields，包含 `MISSING_FIELDS` 和 `PARTIAL_CONTENT` 两个元数据字段。MissingFieldDetectorImpl 遍历 requiredFields 并检查对应值是否为空，会为这两个元数据字段生成无意义的缺失提示（如"PARTIAL_CONTENT字段缺失"）。这会导致：
  1. 返回给前端的 missingFieldHints 中包含脏数据
  2. 业务语义污染——元数据字段不应与业务字段（主诉、现病史等）混同校验
- **建议**：将 `MedicalRecordField` 拆分为业务字段与元数据字段两组枚举，或在 `createDefaultTemplate()` 中显式排除 `MISSING_FIELDS` 和 `PARTIAL_CONTENT`。

#### [严重] FallbackAiService.applyStrategies 使用空 DegradationContext

- **位置**：`FallbackAiService.java:290-301`
- **描述**：`applyStrategies` 在结果非成功且非降级时，创建一个全新的空 `DegradationContext`（serviceName 和 operationName 均为 null），然后对所有 DegradationStrategy 执行 `shouldDegrade(context)`。由于 context 中无任何业务上下文信息，任何依赖上下文的策略实现都无法做出正确决策。该空 context 使整个降级策略判定路径失去实际意义。每个 FallbackAiService 方法入口已构建了有意义的 context（如 serviceName="medical-record", operationName="generateMedicalRecord"），但该 context 仅用于 delegate 选择，未传递到 applyStrategies。
- **建议**：将每个方法入口构建的 `DegradationContext` 实例保存为局部变量，在 `.thenApply(this::applyStrategies)` 中通过闭包或参数传递，使 applyStrategies 能获得真实的上下文信息。或将 DegradationContext 作为方法参数传入 applyStrategies。

#### [严重] MedicalRecordConverter.toRecordGenerateResponse 使用字面字符串比较错误码

- **位置**：`MedicalRecordConverter.java:69,73`
- **描述**：第 69 行和第 73 行硬编码字符串 `"MR_GEN_AI_TIMEOUT"` 与 `aiResult.getErrorCode()` 比较，而非使用 `MedicalRecordErrorCode.MR_GEN_AI_TIMEOUT.getCode()` 或枚举常量比较。一旦枚举 code 字段变更但未同步更新本处字面量，将导致条件判断失效。同时第 72-73 行的 success 判定逻辑将超时降级结果也标记为 `success=true`，导致上游（Controller 的 `response.isSuccess()` 检查）将其视为成功路径返回，掩盖了降级事实。
- **建议**：将第 69 行改为 `MedicalRecordErrorCode.MR_GEN_AI_TIMEOUT.getCode().equals(aiResult.getErrorCode())`。重新评估 success 语义：超时降级场景应将 success 设为 false，由 degraded 标记指示降级状态。

#### [严重] MockAiService.TIMEOUT 策略使用永不完成的 CompletableFuture

- **位置**：`MockAiService.java:67`
- **描述**：`case TIMEOUT: return new CompletableFuture<>()` 返回一个永远不完成的 Future。调用方 MedicalRecordServiceImpl.callAiWithTimeout 通过 `future.get(aiTimeout, TimeUnit.SECONDS)` 等待 12 秒后超时抛出 TimeoutException。在这个过程中，该 Future 无法被 cancel（never-completed Future 的 cancel 返回 false），且其包装的线程资源在 ForkJoinPool.commonPool() 中不可释放。对于集成测试场景，每个 TIMEOUT 用例都会强制等待 12 秒超时，且后续其他测试用例无法提前中止该 Future。
- **建议**：改为 `return CompletableFuture.supplyAsync(() -> { try { Thread.sleep(Long.MAX_VALUE); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return null; } })` 或使用可取消的延迟 Future 实现，使 `future.cancel(true)` 能在测试清理时中断等待。

#### [严重] MedicalRecordConverter.toFieldsMap 将元数据字段放入 content 映射

- **位置**：`MedicalRecordConverter.java:38-45`
- **描述**：`toFieldsMap` 将 `MISSING_FIELDS` 和 `PARTIAL_CONTENT` 两个元数据字段写入与业务字段（CHIEF_COMPLAINT 等）相同的 Map 中，一起持久化到数据库的 `content_json` 列。这违反了数据模型设计：业务字段（医患对话内容）与元数据（AI 标记的缺失项、部分内容）应有不同的生命周期和管理策略。尤其 `PARTIAL_CONTENT` 的 ObjectMapper JSON 序列化在异常时静默吞掉异常并写入 null，可能导致数据库中的 content_json 丢失部分数据而调用方不知情。
- **建议**：将元数据字段分离为独立的实体属性（如 `metadataJson` 列），或在 MedicalRecord 实体中增加独立的 `missingFields` 和 `partialContent` 两个字段。ContentConverter 只负责序列化/反序列化纯业务字段。

#### [严重] MedicalRecordServiceImpl.callAiWithTimeout 异常语义混淆

- **位置**：`MedicalRecordServiceImpl.java:145-159`
- **描述**：`InterruptedException` 和 `ExecutionException` 均返回与`TimeoutException` 相同的错误码 `"MR_GEN_AI_TIMEOUT"` 和消息 "AI medical record generation timeout"。`ExecutionException` 可能由多种原因导致（AI 服务异常、业务逻辑异常等），全部归因于"超时"是语义错误。`InterruptedException` 应传播中断状态并返回不同的错误码。此外，第 153 行 `Thread.currentThread().interrupt()` 正确恢复中断状态，但返回值仍被标记为 degraded 而非异常，调用方将中断视为一次普通降级，无法区分。
- **建议**：为 `ExecutionException` 返回 `MR_GEN_AI_UNAVAILABLE` 错误码；为 `InterruptedException` 返回专门的错误码（如 `MR_GEN_AI_INTERRUPTED`）。异常消息也应反映真实原因。

#### [一般] MedicalRecordServiceImpl 缺少模板加载失败保护

- **位置**：`MedicalRecordServiceImpl.java:90`
- **描述**：`templateConfigManager.getTemplate(request.getDepartmentId())` 的结果直接传给 `MissingFieldDetectorImpl.detect()`（第 99 行）。若 DatabaseTemplateConfigManager 加载失败（如数据库异常导致 `loadFromDatabase` 抛出 RuntimeException），则 Caffeine CacheLoader 会将异常传播到调用方；若返回 DEFAULT_TEMPLATE 但 departmentId 为 null，getTemplate 仍能正常返回。但更关键的是：第 90 行的 template 结果在第 99 行传入 detect() 前未被检查是否为 null，若 getTemplate 在某些异常路径下返回 null，则第 28 行的 `template.getRequiredFields()` 会抛出 NPE。
- **建议**：在 `getTemplate` 返回后增加 null 检查，若返回 null 则走降级路径或立即返回错误。

#### [一般] MedicalRecord 实体 `@PrePersist`/`@PreUpdate` 仅设置 createdAt 不设置 updatedAt

- **位置**：`MedicalRecord.java:133-141`
- **描述**：`prePersist()` 仅设置 `this.createdAt`，未设置 `this.updatedAt`；`preUpdate()` 仅设置 `this.updatedAt`。对于新增记录，`updatedAt` 将为 null 直到第一次更新。新建记录的业务场景中访问 `updatedAt` 将返回 null，可能导致前端时间显示异常或排序逻辑空指针。
- **建议**：在 `prePersist()` 中也设置 `this.updatedAt = LocalDateTime.now()`。

#### [一般] MedicalRecordServiceImpl.resolveVisitId 使用默认线程池

- **位置**：`MedicalRecordServiceImpl.java:132`
- **描述**：`CompletableFuture.supplyAsync(...)` 使用 ForkJoinPool.commonPool()。在 Web 容器高并发场景下，commonPool 的线程数受 CPU 核数限制，大量并发请求耗尽池中线程时将导致其他不相关的 CompletableFuture 调用也阻塞。Spring 应用建议使用专用的 `TaskExecutor` bean 管理异步任务。
- **建议**：注入一个专用的 `Executor` bean（如 `@Bean(name = "visitFacadeExecutor")`），替换 `supplyAsync` 的重载版本。

#### [一般] MissingFieldDetectorImpl 对元数据字段进行缺失检测

- **位置**：`MissingFieldDetectorImpl.java:25-35`
- **描述**：`detect()` 方法遍历 `template.getRequiredFields()`（默认包含 MISSING_FIELDS 和 PARTIAL_CONTENT），检查 `toFieldsMap` 返回的 map 中对应值是否为空。由于 `toFieldsMap` 已包含这两个元数据字段，当 MISSING_FIELDS 在 aiResponse 中为 null 时（concat 后为空字符串，但 String.join 返回空字符串不为 null），检测逻辑可能误判。此外，PARTIAL_CONTENT 字段的缺失检测对业务无意义。
- **建议**：与 DEFAULT_TEMPLATE 的修复联动，要求 requiredFields 不包含元数据字段；或在 MissingFieldDetectorImpl 中增加过滤逻辑。

#### [一般] MedicalRecordContentConverter 反序列化时静默吞掉异常

- **位置**：`MedicalRecordContentConverter.java:44-46`
- **描述**：`convertToEntityAttribute` 在 JSON 解析异常或 `MedicalRecordField.valueOf()` 因新增/删除的枚举值反序列化失败时，返回 `Collections.emptyMap()` 且不记录任何日志。若数据库中的 content_json 因升级变更导致枚举值不匹配，数据将静默丢失。`convertToDatabaseColumn` 同名（第 29 行）有同样的问题。
- **建议**：在 catch 块中至少记录警告日志，打印 dbData 和异常信息，便于运维和问题排查。

#### [一般] DraftContextStoreImpl 缺少 compute 原子操作

- **位置**：`DraftContextStoreImpl.java:17`
- **描述**：`put()` 方法直接调用 `store.put(key, value)`，非原子覆盖。对于草稿上下文的创建和更新场景，仅使用 put 可能导致竞争条件：线程 A 和线程 B 同时检查不存在后写入，后写入者覆盖前者。与之对比，`ConcurrentHashMapStore` 额外提供了 `compute()` 和 `createIfNotExists()` 原子方法。
- **建议**：若 DraftContextStore 需要原子创建语义，应增加 createIfNotExists/putIfAbsent 方法。当前实现若能被多线程访问到，需明确设计文档约定其并发安全边界。

#### [轻微] FallbackAiService 各方法大量重复代码

- **位置**：`FallbackAiService.java:82-288`
- **描述**：13 个方法几乎完全相同的实现模式（空检查 → 构建 context → selectDelegate → 委托调用 → applyStrategies），仅有 serviceName/operationName 和方法调用不同。这种重复在 AI 接口增加新方法时容易遗漏，且增加维护成本。
- **建议**：抽取模板方法 `private <T> CompletableFuture<AiResult<T>> executeWithFallback(String serviceName, String operationName, Function<AiService, CompletableFuture<AiResult<T>>> action)` 消除重复。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 6 |
| 一般 | 5 |
| 轻微 | 1 |

### 总评

medical-record 模块整体结构符合 OOD 设计，职责划分清晰，Controller-Service-AiService-Repository 分层正确。但数据模型层面存在严重的设计缺陷：`MedicalRecordField` 枚举将业务字段与元数据字段混用，导致 `DEFAULT_TEMPLATE.requiredFields`、`toFieldsMap`、`MissingFieldDetector` 三条链路均受到污染，产生连锁性的错误提示和数据模型歧义。Converter 层的错误码字面字符串比较（T18）和 success 语义混淆（T17）是需要立即修复的正确性问题。

ai-impl fallback 层方面，`DegradationContext` 在 `selectDelegate` 处正确构造但在 `applyStrategies` 处重新创建空实例（T20），整个降级策略判定路径处于残缺状态。`MockAiService.TIMEOUT` 使用永不完成的 Future（T19）在测试场景下存在资源泄漏风险。

Store 抽象层的设计合理，`ConcurrentHashMapStore` 正确使用了 `ConcurrentHashMap` 的原子方法，但 `DraftContextStoreImpl` 缺少相应的原子操作封装。
