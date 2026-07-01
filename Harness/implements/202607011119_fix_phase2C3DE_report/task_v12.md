# 任务指令（v12）

## 动作
NEW

## 任务描述
修复 diagnosis 报告中剩余的 P1/P2 缺陷，涉及 medical-record、application/跨模块、consultation、ai-impl、prescription 五个模块，共 17 项子任务（9a-9q）。预估工作量：5.2人时。

## 选择理由
路线表第 9 项。R11 已完成所有 prescription 模块 P1/P2 批量修复（14 项 8a-8n）。剩余缺陷分布在其他模块，集中一轮批量处理。

## 任务上下文

### 子项清单

| 编号 | 诊断编号 | 级别 | 模块 | 描述 | 涉及文件 | 预估 |
|:---:|:--------:|:----:|:----:|------|---------|:----:|
| 9a | M02 | P1 | medical-record | DatabaseTemplateConfigManager @EventListener 缓存失效使用 invalidateAll() 而非 invalidate(key) | `DatabaseTemplateConfigManager.java` | 0.3h |
| 9b | M11 | P1 | medical-record | ai-api MedicalRecordGenResponse.missingFields 与 partialContent 未被业务层消费（T21 依赖——需先修复 T21） | `MedicalRecordConverter.java` | 0.3h |
| 9c | T17 | P1 | medical-record | callAiWithTimeout 三种异常（Timeout/Interrupted/Execution）统一返回"timeout"错误码，语义混淆 | `MedicalRecordServiceImpl.java` | 0.5h |
| 9d | T18 | P1 | medical-record | MedicalRecordConverter 使用字面字符串 `"MR_GEN_AI_TIMEOUT"` 而非 MedicalRecordErrorCode 枚举 | `MedicalRecordConverter.java` | 0.3h |
| 9e | T21 | P1 | medical-record | 元数据字段 MISSING_FIELDS/PARTIAL_CONTENT 混入 content_map | `MedicalRecordConverter.java`, `DatabaseTemplateConfigManager.java` | 0.5h |
| 9f | T22 | P1 | medical-record | MedicalRecordContentConverter 序列化/反序列化异常时静默返回 null/emptyMap，无 WARN 日志 | `MedicalRecordContentConverter.java` | 0.3h |
| 9g | T47 | P1 | medical-record | MedicalRecord @PrePersist 仅设置 createdAt，未设置 updatedAt | `MedicalRecord.java` | 0.2h |
| 9h | T48 | P1 | medical-record | MedicalRecordServiceImpl.resolveVisitId 使用 ForkJoinPool.commonPool() | `MedicalRecordServiceImpl.java` | 0.5h |
| 9i | T50 | P1 | medical-record | DraftContextStoreImpl 缺少 compute/putIfAbsent 原子操作 | `DraftContextStoreImpl.java`, `DraftContextStore.java` | 0.5h |
| 9j | A06 | P1 | 跨模块 | DegradationStrategy/DegradationContext 降级决策不生效（NoOp 始终返回 false + applyStrategies 使用空 DegradationContext） | `FallbackAiService.java`, `DegradationContext.java` | 1.0h |
| 9k | A09 | P1 | 跨模块 | AuditConverter.toAuditResponse 在 aiData == null 时退化为 PASS + 空 alerts，调用方未拦截 null data | `PrescriptionAuditServiceImpl.java`, `AuditConverter.java` | 0.3h |
| 9l | T19 | P1 | ai-impl | MockAiService.TIMEOUT 策略使用永不完成的 CompletableFuture，测试场景下线程资源泄漏 | `MockAiService.java`, `MockAiServiceTest.java` | 0.3h |
| 9m | T20 | P1 | 跨模块 | FallbackAiService.applyStrategies 使用空 DegradationContext（无 serviceName/operationName） | `FallbackAiService.java` | 0.3h |
| 9n | T5 | P1 | consultation | TriageConverter 副作用修改 Session（setCorrectedChiefComplaint） + DialogueSession 混用同步机制 | `TriageConverter.java`, `TriageServiceImpl.java`, `DialogueSession.java` | 0.5h |
| 9o | T32 | P1 | prescription | DraftContextCleanupTask 迭代 keySet() 与 writeTimestamps 非原子（P03/S02 R6 修复后 recordWrite 已有调用方，但非原子迭代仍存在） | `DraftContextCleanupTask.java` | 0.3h |
| 9p | A08/T24 | P2 | 跨模块 | A08: 降级 fallback 文案英文混用；T24: ConcurrentHashMapStore 缺少 @Service/@Component 注解 | `TriageServiceImpl.java`, `MedicalRecordServiceImpl.java`, `ConcurrentHashMapStore.java` | 0.3h |
| 9q | A11 | P2 | 跨模块 | PrescriptionAuditServiceImpl.persistAuditRecord 中 `&& aiResult.getData() != null` 冗余防御检查（AiResult.success() 已 `Objects.requireNonNull(data)`） | `PrescriptionAuditServiceImpl.java` | 0.1h |

> 执行次序：9e → 9b（9b 依赖 9e），9a 与 9e 修改同一文件不同区域，需注意合并。其余子项无依赖可并行。

### 涉及文件汇总

| 文件路径（相对 `AIMedical/backend/modules/`） | 操作 | 子项 |
|------|------|:----:|
| `medical-record/.../config/DatabaseTemplateConfigManager.java` | 修改 | 9a, 9e |
| `medical-record/.../converter/MedicalRecordConverter.java` | 修改 | 9b, 9d, 9e |
| `medical-record/.../service/impl/MedicalRecordServiceImpl.java` | 修改 | 9c, 9h |
| `medical-record/.../converter/MedicalRecordContentConverter.java` | 修改 | 9f |
| `medical-record/.../entity/MedicalRecord.java` | 修改 | 9g |
| `medical-record/.../store/DraftContextStoreImpl.java` | 修改 | 9i |
| `medical-record/.../store/DraftContextStore.java` | 修改（接口新增方法） | 9i |
| `ai/ai-impl/.../mock/MockAiService.java` | 修改 | 9l |
| `ai/ai-impl/.../fallback/FallbackAiService.java` | 修改 | 9j, 9m |
| `ai/ai-api/.../DegradationContext.java` | 修改 | 9j |
| `consultation/.../converter/TriageConverter.java` | 修改 | 9n |
| `consultation/.../service/impl/TriageServiceImpl.java` | 修改 | 9n, 9p |
| `consultation/.../entity/DialogueSession.java` | 修改 | 9n |
| `prescription/.../task/DraftContextCleanupTask.java` | 修改 | 9o |
| `prescription/.../converter/AuditConverter.java` | 修改 | 9k |
| `prescription/.../service/audit/impl/PrescriptionAuditServiceImpl.java` | 修改 | 9k |
| `medical-record/.../service/impl/MedicalRecordServiceImpl.java` | 修改 | 9p |
| `common/.../cache/ConcurrentHashMapStore.java` | 修改 | 9p |
| `prescription/.../service/audit/impl/PrescriptionAuditServiceImpl.java` | 修改 | 9q |

### 子项详细变更说明

#### 9a — M02: 缓存失效 key 粒度

**问题定位**：`DatabaseTemplateConfigManager.java:105-108` 的 `@EventListener(TemplateConfigChangeEvent.class)` 调用 `templateCache.invalidateAll()`，未按 `event.getDepartmentCode()` 失效指定 key。

**变更明细**：
- `invalidateAll()` → 判断 departmentCode：非 null 时 `invalidate(departmentCode)`，null 时 `invalidateAll()`
- 新增 import: `org.apache.commons.lang3.StringUtils`（或直接 null 判断）

#### 9b — M11: missingFields/partialContent 业务层消费

**问题定位**：`MedicalRecordConverter.java:38-45` 已将 missingFields 写入 `MISSING_FIELDS` 字段、partialContent 写入 `PARTIAL_CONTENT` 字段。但 T21 要求这两个元数据字段不应混入 content_map。

**变更明细**：
- 依赖 9e（T21）先修复
- 从 content_map 写入逻辑中排除 MISSING_FIELDS/PARTIAL_CONTENT 两个 key

#### 9c — T17: 异常语义混淆

**问题定位**：`MedicalRecordServiceImpl.java:145-159` 三种异常统一返回 `AiResultFactory.degraded("AI medical record generation timeout", "MR_GEN_AI_TIMEOUT", null)`。

**变更明细**：
- `TimeoutException` → `MR_GEN_AI_TIMEOUT` + 保留现有 message
- `InterruptedException` → 恢复中断标记 + `MR_GEN_AI_INTERRUPTED`（若不存在则新增）
- `ExecutionException` → 提取 `e.getCause()` 消息 + `MR_GEN_AI_EXECUTION_ERROR`

#### 9d — T18: 字面字符串 → 枚举

**问题定位**：`MedicalRecordConverter.java:69,73` 使用 `"MR_GEN_AI_TIMEOUT"` 字面字符串，应使用 `MedicalRecordErrorCode` 枚举。

**变更明细**：
- `"MR_GEN_AI_TIMEOUT"` → `MedicalRecordErrorCode.MR_GEN_AI_TIMEOUT.name()`

#### 9e — T21: 元数据字段混入 content_map

**问题定位**：`MedicalRecordConverter.java:38-45` 将 `MISSING_FIELDS`/`PARTIAL_CONTENT` 写入 content_map。`DatabaseTemplateConfigManager.java:111` 的 `DEFAULT_TEMPLATE` 包含所有 `MedicalRecordField` 值。

**变更明细**：
- MedicalRecordConverter 写入 content_map 时跳过 `MISSING_FIELDS`/`PARTIAL_CONTENT`
- DatabaseTemplateConfigManager DEFAULT_TEMPLATE 排除这两个元数据字段

#### 9f — T22: 序列化异常静默

**问题定位**：`MedicalRecordContentConverter.java:29,44-46` 异常时返回 null/emptyMap，无 WARN 日志。

**变更明细**：
- catch 块追加 `log.warn("MedicalRecordContentConverter serialization/deserialization failed", e)`
- 新增 Logger 字段：`private static final Logger log = LoggerFactory.getLogger(MedicalRecordContentConverter.class)`

#### 9g — T47: @PrePersist 未设置 updatedAt

**问题定位**：`MedicalRecord.java:133-136` `@PrePersist` 仅设置 `this.createdAt = LocalDateTime.now()`，未设置 `this.updatedAt`。

**变更明细**：
- `@PrePersist` 追加 `this.updatedAt = LocalDateTime.now()`

#### 9h — T48: ForkJoinPool.commonPool()

**问题定位**：`MedicalRecordServiceImpl.java:132` `CompletableFuture.supplyAsync(() -> {...})` 使用默认 ForkJoinPool.commonPool()。

**变更明细**：
- 注入 `ExecutorService medicalRecordExecutor`（@Qualifier）
- 构造函数新增参数
- 新增配置类 `MedicalRecordThreadPoolConfig`（@Bean("medicalRecordExecutor")）
- supplyAsync 绑定 medicalRecordExecutor

#### 9i — T50: DraftContextStoreImpl 缺少原子操作

**问题定位**：`DraftContextStoreImpl.java:11-26` 仅有 get/put/remove/containsKey/keySet，缺少 compute/createIfNotExists。

**变更明细**：
- `DraftContextStore` 接口新增 `createIfNotExists(String key, Object value)` 和 `compute(String key, BiFunction)` 方法
- `DraftContextStoreImpl` 实现这两个方法（基于 ConcurrentHashMap）
- 新增 import: `java.util.function.BiFunction`

#### 9j — A06: 降级决策不生效

**问题定位**：`FallbackAiService.java:66-80` selectDelegate 中 NoOpDegradationStrategy.shouldDegrade 始终返回 false（当前 NoOp 行为保留）；第290-301行 applyStrategies 使用空 DegradationContext，无法通过 context 做降级决策。

**变更明细**：
1. `selectDelegate` 保持当前行为（NoOp），补充 TODO 标注供后续自定义策略注册时覆盖
2. **核心变更**：`applyStrategies` 方法签名新增 `DegradationContext` 参数：
   ```java
   private <T> AiResult<T> applyStrategies(AiResult<T> result, DegradationContext context) {
       // 使用传入的 context 替代 new DegradationContext()
   }
   ```
3. 所有 12 个 thenApply 调用点从 `.thenApply(this::applyStrategies)` 改为：
   ```java
   .thenApply(result -> applyStrategies(result, context))
   ```
   每个方法已创建的 `context`（含 serviceName/operationName）直接传入 lambda。
4. **线程安全性**：每个请求独享 `DegradationContext` 实例（每方法新建），lambda 捕获绑定的 context 引用，无共享状态，线程安全。
5. `DegradationContext` 构造函数保持兼容。**9m 的变更由本子项一并覆盖**（同一 applyStrategies 方法，同一 context 传递路径）。

#### 9k — A09: aiData == null 退化 PASS

**问题定位**：`PrescriptionAuditServiceImpl.java:110-111` 仅检查 `aiResult != null && aiResult.isSuccess()`，未检查 `getData()` null。

**变更明细**：
- 第110行追加 `&& aiResult.getData() != null` 检查
- 当 aiResult.getData() == null 时走 AI_UNAVAILABLE 降级路径

#### 9l — T19: TIMEOUT 永不完成的 CompletableFuture

**问题定位**：`MockAiService.java:67` `new CompletableFuture<>()` 永不完成，测试中线程资源泄漏。

**变更明细**：
- `new CompletableFuture<>()` → `CompletableFuture.failedFuture(new TimeoutException("Mock timeout"))`

**测试修改**（`MockAiServiceTest.java`）：
- `timeoutStrategyShouldTimeout()` 第208-209行需适配 `failedFuture` 语义：
  ```java
  assertTrue(future.isDone());  // was assertFalse(future.isDone())
  assertThrows(ExecutionException.class, () -> future.get(1, TimeUnit.MILLISECONDS));
  // failedFuture 的 .get(timeout, unit) 抛出 ExecutionException（包装 TimeoutException），而非直接 TimeoutException
  ```
- 新增 import: 无需（ExecutionException 已在 `java.util.concurrent` 包，通过已有 import `java.util.concurrent.*` 覆盖）

#### 9m — T20: 空 DegradationContext（由 9j 一并覆盖）

**说明**：本子项与 9j（A06）修改同一方法 `FallbackAiService.applyStrategies()`，且根因相同。已通过 9j 的变更（`applyStrategies` 新增 DegradationContext 参数、lambda 传递）一并修复。无需单独变更。

#### 9n — T5: TriageConverter 副作用 + DialogueSession 同步机制

**问题定位**：
1. `TriageConverter.java:107-109` 与 `TriageServiceImpl.java:148-150` 重复设置 session.correctedChiefComplaint
2. DialogueSession 混用 synchronized/AtomicInteger/CopyOnWriteArrayList

**变更明细**：
1. 移除 `TriageServiceImpl.java:148-150` 重复设置，保留 Converter 为唯一边界
2. `DialogueSession.java` 统一同步策略：保留 synchronized 方法为所有字段的统一同步机制。将 `aiFailCount` 和 `roundCount` 从 `AtomicInteger` 改为 `int` + synchronized getter/setter，与其余字段一致。`CopyOnWriteArrayList` 已线程安全，保留不变。

**测试修改**（`DialogueSessionTest.java`）：
- `shouldSupportAtomicIntegerStateTransitions` 无需修改（行为不变，synchronized 等价）
- 并发测试 `shouldHandleConcurrentReadsAndWrites` 无需修改（synchronized 保证可见性等价于 AtomicInteger）
- `shouldSetAndGetAllFields` 无需修改

#### 9o — T32: writeTimestamps 非原子迭代

**问题定位**：`DraftContextCleanupTask.java:38-45` 遍历 `draftContextStore.keySet()` 与 `writeTimestamps.get(key)` 非原子（R6 已修复 recordWrite 无调用方问题，但非原子迭代仍存在）。

**变更明细**：
- 使用 `writeTimestamps.forEach(...)` 替代 `draftContextStore.keySet().forEach(...)` 作为迭代基准，迭代 `writeTimestamps.entrySet()` 而非 `draftContextStore.keySet()`

#### 9p — A08/T24: P2 小批量

**A08 变更明细**：
- `TriageServiceImpl.java:171,177`：中文文案保留
- `MedicalRecordServiceImpl.java:151,154,157`：英文文案改为中文，与 consultation 统一

**T24 变更明细**：
- `ConcurrentHashMapStore.java:10` 添加 `@Service` 注解

### 测试修改方案

#### 9a — M02: DatabaseTemplateConfigManagerTest
- `shouldReturnDefaultTemplateWhenDepartmentNotFound` 第29行断言 `assertEquals(9, ...)` → 若 9e 排除了 MISSING_FIELDS/PARTIAL_CONTENT，则 DEFAULT_TEMPLATE 的 requiredFields 从 9 个变为 7 个 → 断言改为 `assertEquals(7, ...)`
- 新增测试：`shouldInvalidateByDepartmentCode()` 验证 `invalidate(departmentCode)` 与 `invalidateAll()` 的分支

#### 9b — M11: MedicalRecordConverterTest（依赖 9e 先完成）
- `toFieldsMapShouldMapAllNineFields` 第45行断言 `assertEquals(9, ...)` → 改为 `assertEquals(7, ...)`（排除 MISSING_FIELDS/PARTIAL_CONTENT 后）
- `toFieldsMapShouldPreserveNullValues` 第62行同理 `assertEquals(9, ...)` → `assertEquals(7, ...)`
- `toRecordGenerateResponseShouldBuildResponseFromAiResult` 第130行 `assertEquals(9, ...)` → `assertEquals(7, ...)`

#### 9c — T17: MedicalRecordServiceImplTest + MedicalRecordErrorCodeTest
- `MedicalRecordServiceImplTest`：
  - `shouldReturnDegradedWhenAiTimesOut` 第127行断言 `assertEquals(MedicalRecordErrorCode.MR_GEN_AI_TIMEOUT, response.getErrorCode())` — 正常运行（TimeoutException 分支），无需修改
  - 新增 `shouldReturnInterruptedOnInterruptedException`：mock ExecutionException 抛 InterruptedException，验证 errorCode 为 `MR_GEN_AI_INTERRUPTED`
  - 新增 `shouldReturnExecutionErrorOnExecutionException`：mock ExecutionException，验证 errorCode 为 `MR_GEN_AI_EXECUTION_ERROR`
- `MedicalRecordErrorCodeTest`：
  - `shouldHaveEightConstants` 第19行 `assertEquals(8, ...)` → `assertEquals(10, ...)`（新增 MR_GEN_AI_INTERRUPTED / MR_GEN_AI_EXECUTION_ERROR）
  - `shouldReturnCorrectCodeAndMessage` 新增两个枚举的 code/message 断言

#### 9d — T18: MedicalRecordConverterTest（无测试变更）
- `toRecordGenerateResponseShouldSetTimeoutErrorCode` 第156行 `assertEquals(MedicalRecordErrorCode.MR_GEN_AI_TIMEOUT, response.getErrorCode())` — `.name()` 返回值与字面字符串 `"MR_GEN_AI_TIMEOUT"` 相同，断言继续通过
- `toRecordGenerateResponseShouldReturnSuccessTrueWhenTimeoutEvenWithNullData` 同理

#### 9e — T21: MedicalRecordConverterTest + DatabaseTemplateConfigManagerTest
- 参见 9a/9b 的断言变更（assertEquals 9→7）
- `DatabaseTemplateConfigManagerTest.shouldReturnDefaultTemplateWhenDepartmentNotFound` 第29行同步改为 `assertEquals(7, ...)`

#### 9f — T22: MedicalRecordContentConverterTest
- 新增 `shouldLogWarnOnSerializationFailure`：mock 异常场景，验证 log.warn 被调用
- 新增 `shouldLogWarnOnDeserializationFailure`：验证反序列化异常时 log.warn 输出

#### 9g — T47: MedicalRecordTest（若无现有 updatedAt 测试，新增）
- 新增 `prePersistShouldSetUpdatedAt`：调用 `prePersist()` 后断言 `updatedAt != null`
- 现有 `@PreUpdate` 相关测试不受影响

#### 9h — T48: MedicalRecordServiceImplTest
- `setUp()` 第53行构造函数参数从 6 个变为 7 个（新增 `ExecutorService medicalRecordExecutor`）：
  ```java
  service = new MedicalRecordServiceImpl(visitFacade, templateManager, aiService,
          detector, converter, recordRepository, medicalRecordExecutor);
  ```
- 新增 `@Mock ExecutorService medicalRecordExecutor` 字段（或使用 `Executors.newSingleThreadExecutor()`）
- `shouldReturnDegradedWhenAiTimesOut` 中 resolveVisitId 的 commonPool 调用不受影响（executor 仅用于 callAiWithTimeout）— 需确认测试仍通过

#### 9i — T50: DraftContextCleanupTaskTest
- `StubDraftContextStore`（第143-160行）实现 `DraftContextStore`，当前仅实现 `SessionStore`（get/put/remove/containsKey/keySet）。接口新增 createIfNotExists/compute 后，stub 需补充实现：
  ```java
  @Override
  public Object createIfNotExists(String key, Object value) {
      return map.putIfAbsent(key, value);
  }
  @Override
  public Object compute(String key, BiFunction<String, Object, Object> remappingFunction) {
      return map.compute(key, remappingFunction);
  }
  ```
- 新增 import: `import java.util.function.BiFunction`

#### 9j — A06 + 9m — T20: FallbackAiServiceTest
- `applyStrategies` 方法签名变更（新增 DegradationContext 参数）后，测试中直接调用 `applyStrategies(result)` 的代码需改为 `applyStrategies(result, context)`。需检查 FallbackAiServiceTest 中是否有直接调用 `applyStrategies` 的白盒测试。
- 若测试通过反射调用私有方法，适配新签名
- 建议：将 applyStrategies 改为 package-private 以便测试，或通过 `.thenApply(result -> applyStrategies(result, context))` 的 thenApply 方法引用验证（无需直接调用私有方法）

#### 9k — A09: PrescriptionAuditServiceImplTest
- `auditShouldHandleAiResultDataNull`（R10 8k 已新增）：验证 `aiResult.isSuccess() && aiResult.getData() != null` 检查使 null data 走降级路径
- 新增 `auditShouldPassThroughWhenAiResultDataIsNotNull`：验证正常 data 走 AI 检查路径

#### 9l — T19: MockAiServiceTest
- `timeoutStrategyShouldTimeout` 第208行 `assertFalse(future.isDone())` → `assertTrue(future.isDone())`
- 第209行 `assertThrows(TimeoutException.class, () -> future.get(1, TimeUnit.MILLISECONDS))` → `assertThrows(ExecutionException.class, () -> future.get(1, TimeUnit.MILLISECONDS))`

#### 9n — T5: DialogueSessionTest
- `shouldSupportAtomicIntegerStateTransitions` 无需修改（AtomicInteger → synchronized int，行为等价）
- `shouldHandleConcurrentReadsAndWrites` 无需修改（synchronized 保证可见性）
- 其余测试无需修改

#### 9o — T32: DraftContextCleanupTaskTest
- `cleanupExpiredDrafts` 迭代逻辑从 `draftContextStore.keySet()` 改为 `writeTimestamps.entrySet()`：
  - `shouldRecordWriteTimestamp` 无需修改（已验证 recordWrite 后 key 存在）
  - `shouldRemoveExpiredDraft` 无需修改
  - `shouldNotRemoveEntryWithoutTimestamp` 第70-76行：writeTimestamps 无该 key，迭代基准变更后不会进入该条目 — 行为不变
  - 其他测试无需修改

#### 9p — T24: ConcurrentHashMapStoreTest
- 新增 `shouldBeAnnotatedWithService`：
  ```java
  @Test
  void shouldBeAnnotatedWithService() {
      assertNotNull(ConcurrentHashMapStore.class.getAnnotation(org.springframework.stereotype.Service.class));
  }
  ```

#### 9q — A11: PrescriptionAuditServiceImplTest（无测试变更）
- 仅移除 `&& aiResult.getData() != null`，等价于 `isSuccess()` 已保证 data 非空，不存在可测试的行为变更。

### 已有代码上下文

- `DatabaseTemplateConfigManager.java:100-115`：@EventListener 带 `@Async` 标注，templateCache 为 Caffeine Cache
- `MedicalRecordConverter.java:35-50`：content_map 构建逻辑，遍历 MedicalRecordField.values() 写入
- `MedicalRecordServiceImpl.java:128-160`：resolveVisitId → callAiWithTimeout 链
- `MedicalRecordContentConverter.java:25-50`：serialize/deserialize 方法，catch Exception 返回 null/emptyMap
- `MedicalRecord.java:130-145`：@PrePersist/@PreUpdate 生命周期回调
- `DraftContextStoreImpl.java:8-30`：@Service，内部 ConcurrentHashMap<String, Object>
- `DraftContextStore.java`：extends SessionStore 接口
- `FallbackAiService.java:60-85,285-305`：selectDelegate 遍历 + applyStrategies
- `MockAiService.java:60-75`：switch TIMEOUT 分支
- `TriageConverter.java:105-112`：toTriageResponse 中 setCorrectedChiefComplaint
- `DialogueSession.java`：synchronized fields + AtomicInteger mix
- `PrescriptionAuditServiceImpl.java:105-115`：aiResult null/isSuccess 检查
- `ConcurrentHashMapStore.java:8-15`：implements SessionStore，无 @Service
- `DraftContextCleanupTask.java:35-48`：cleanupExpiredDrafts 方法
