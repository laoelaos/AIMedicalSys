# 实现计划

任务描述：根据代码审查问题诊断报告（Docs\Diagnosis\impl\07_phase2C3DE_report.md），修复 consultation/prescription/medical-record 三模块的 P0/P1/P2 级别缺陷。
项目根目录：C:\Develop\Software\AIMedicalSys

---

## 实施路线

> 注：Plan round 编号因 R7/R8/R9 的 A05 retry 轮次插入，与路线表序号产生偏移。R10/R11 = 路线表第 8 项。

| 序号 | Plan Round | 任务项 | 工作量预估 | 状态 |
|:---:|:---------:|--------|:---------:|:----:|
| 1 | R1 | **consultation: C06 — DoctorFacade 跨模块调用超时控制** | 2人时 | ✅ DONE |
| 2 | R2 | **consultation: C20+S04 — DialogueSessionManager 并发安全 + E02 — TriageRecord 并发 INSERT** | 5人时 | ✅ DONE |
| 3 | R3 | **consultation: C03/A04/T44 — correctedChiefComplaint 数据流 + C16/C17** | 4人时 | ✅ DONE |
| 4 | R4 | **prescription: P02 — DrugFacade 注入 + A01 — AiResultFactory + A03 — 枚举扩充** | 5人时 | ✅ DONE |
| 5 | R5 | **prescription: P01 — 异步 AI 调度 + S03 — DedupTaskScheduler 跨 key 竞态** | 4人时 | ✅ DONE |
| 6 | R6 | **prescription P03/S02 TTL + A05 MockAiService + T40/E05 @Recover + M04 乐观锁** | 5人时 | ✅ DONE |
| 7 | R9 | **consultation P1+P2 低风险批量** | 2人时 | ✅ DONE |
| 8 | R10/R11 | **prescription 剩余 P1+P2 批量（14项 8a-8n）** | 6人时 | ✅ DONE |
| 9 | R12 | **跨模块 + medical-record P1+P2 批量（17 项 9a-9q）** | 5人时 | ❌ FAILED |
| 10 | R13 | **修复 v12 验证失败的 7 项测试** | 1人时 | ✅ DONE |
| 11 | — | **验证测试 + 全量回归** | 3人时 | ✅ DONE |

---

## R1 PASSED consultation: C06 — DoctorFacade 跨模块调用超时控制

结果：TriageServiceImpl.findDoctorsForDepartments 新增 doctorFacadeTimeout 构造参数注入；findDoctorsForDepartments 中 doctorFacade 调用包裹 CompletableFuture.supplyAsync + .get(doctorFacadeTimeout, SECONDS)；catch 块补充 InterruptedException 中断恢复。
文件：AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/service/impl/TriageServiceImpl.java
测试：TriageServiceImplTest — 52 用例通过，0 失败，含新增超时/中断/混合场景用例。

---

## R2 PASSED consultation: C20/S04 + E02 并发安全修复

结果：
1. DialogueSessionManager.restoreSession 方法签名添加 `synchronized` 关键字，消除 createSession 与 restoreSession 间的竞态
2. TriageServiceImpl.saveTriageRecord 入口包裹 per-sessionId `ReentrantLock`（ConcurrentHashMap<String, Lock>），同 sessionId 串行化防止并发首次 INSERT 唯一约束冲突；`triageLocks.size() > 1000` 时清理当前条目防内存泄漏

涉及文件：
- `DialogueSessionManager.java`：restoreSession 方法加 synchronized
- `TriageServiceImpl.java`：新增 triageLocks 字段 + saveTriageRecord 锁包裹

测试：186 用例通过，0 失败（TriageServiceImplTest 55 用例 + DialogueSessionManagerTest 20 用例 + 其他 consultation 测试），含新增并发场景测试。

---

## R3 PASSED consultation: C03/A04/T44 + C16 + C17 correctedChiefComplaint 数据流 + 规则引擎 JSON 解析 + 医生列表 score 排序

结果：
1. TriageResponse（consultation DTO）新增 `correctedChiefComplaint` 字段 + getter/setter
2. TriageConverter.toTriageResponse() 追加 `response.setCorrectedChiefComplaint()` 透传 AI 修正结果
3. DefaultTriageRuleEngine.matchesConditions() catch 块 `return true` → `return false` + `log.warn`
4. TriageServiceImpl.findDoctorsForDepartments 添加 TODO 注释标记 score 排序设计偏差

涉及文件：
- `consultation/dto/TriageResponse.java`
- `consultation/converter/TriageConverter.java`
- `consultation/rule/DefaultTriageRuleEngine.java`
- `consultation/service/impl/TriageServiceImpl.java`

测试：186 用例通过，0 失败，含 DefaultTriageRuleEngineTest 21 用例 + TriageConverterTest 17 用例。

---

## R4 PASSED prescription: P02 — DrugFacade 注入 + A01 — AiResultFactory 零引用 + A03 — AiSuggestionStatus 缺状态枚举

结果：完成 prescription 模块三个缺陷修复。DrugFacade 注入到 PrescriptionAuditServiceImpl 和 PrescriptionAssistServiceImpl；catch 块替换为 AiResultFactory.failure() 构造降级结果；AiSuggestionStatus 扩充为 PENDING/PROCESSING/COMPLETED/FAILED/TIMEOUT；DedupTaskScheduler 3 处 PENDING 检查追加 PROCESSING；scheduleSuggestionAsync 拆分 TimeoutException 独立 catch 设 TIMEOUT 状态。
涉及文件：4 个源文件（AiSuggestionStatus/PrescriptionAuditServiceImpl/PrescriptionAssistServiceImpl/DedupTaskScheduler）+ 4 个测试文件
测试：308 用例通过，0 失败，通过率 100%。

---

## R5 NEW prescription: P01 — 异步 AI 调度线程池绑定 + S03 — DedupTaskScheduler 跨 key 竞态

任务：修复 prescription 模块两个 P1 级别缺陷——(1) P01: scheduleSuggestionAsync 使用 ForkJoinPool.commonPool() 而非专用线程池，返回的 CompletableFuture 未被消费（异步异常静默丢失）；(2) S03: DedupTaskScheduler.schedule() createIfNotExists 成功后在 compute lambda 外执行 suggestionStore.put(candidateTaskId, ...)，跨 key 写入破坏原子性。

选择理由：R4 已完成 P02/A01/A03 三个 prescription 缺陷修复。P01 和 S03 是 prescription 模块剩余 P1 缺陷中优先级最高的两个：(1) P01 导致异步 AI 调用消耗 ForkJoinPool 公共线程池，高并发下线程耗尽风险；(2) S03 导致 schedule() 线程 A createIfNotExists 成功后、suggestionStore.put 执行前，线程 B 可通过 dedupKey 读到 candidateTaskId 但 get(candidateTaskId) 找不到数据。两者修改范围独立但均为同一模块的同一服务层，合并一轮实现。

### P01 — scheduleSuggestionAsync 线程池绑定 + 异常处理

问题定位：
- `PrescriptionAssistServiceImpl.java:343` `CompletableFuture.supplyAsync(() -> {...})` 未指定 Executor，默认使用 `ForkJoinPool.commonPool()`（所有异步任务共享一个池）
- 第381行 `supplyAsync` 返回的 `CompletableFuture<AiSuggestionResult>` 未被赋值或消费，lambda 内抛出的异常不会被任何代码捕获（仅测试中通过 mock 的 future.get() 模拟异常）
- 生产环境中 AI 调用失败时异常无法被捕获并处理

变更明细：
1. PrescriptionAssistServiceImpl 新增字段 `private final ExecutorService aiTaskExecutor;`
2. 构造函数新增 `@Qualifier("aiTaskExecutor") ExecutorService aiTaskExecutor` 参数
3. `scheduleSuggestionAsync()` 中 `CompletableFuture.supplyAsync(() -> {...})` → `CompletableFuture.supplyAsync(() -> {...}, aiTaskExecutor)`
4. 对 `supplyAsync` 返回的 CF 链式调用 `.exceptionally()` 记录 WARN 日志（防止未捕获异常）
5. 新增配置类 `com.aimedical.modules.prescription.config.PrescriptionThreadPoolConfig`，定义 `@Bean("aiTaskExecutor")`

### S03 — DedupTaskScheduler.schedule() 跨 key 竞态

问题定位：
- `DedupTaskScheduler.java:39-41` createIfNotExists(dedupKey, newResult) 成功后，在 compute lambda 外执行 suggestionStore.put(candidateTaskId, newResult)
- `DedupTaskScheduler.java:64-66` compute(dedupKey, ...) 返回 newResult 后，同样在 lambda 外执行 suggestionStore.put(candidateTaskId, newResult)
- 跨 key 窗口：线程 A createIfNotExists(dedupKey) 成功后，put(candidateTaskId) 前，线程 B 可通过 get(candidateTaskId) 读到 null

变更明细：
- 将 `suggestionStore.put(candidateTaskId, newResult)` 移至 `createIfNotExists` / `compute` **之前**执行，使 candidateTaskId 对应的数据在 dedupKey 被声明前就已可用
- 两个 put 调用点分别迁移：
  1. createIfNotExists 分支（原第41行）：移至第38行 `createIfNotExists` 调用之前
  2. compute 分支（原第65行）：移至第53行 `compute` 调用之前
- 简化 schedule 返回值逻辑（createIfNotExists/compute 失败时 newResult 已存在于 candidateTaskId key 下，但为孤立数据——不清理，因 AiSuggestionResult 体积小且受 TTL 清理）

### 修改后 DedupTaskScheduler.schedule() 伪码

```
public String schedule(String prescriptionId) {
    String dedupKey = DEDUP_KEY_PREFIX + prescriptionId;
    String candidateTaskId = UUID.randomUUID().toString();

    // 快路径检查已有 PENDING/PROCESSING/COMPLETED+!consumed
    Object existing = suggestionStore.get(dedupKey);
    if (existing instanceof AiSuggestionResult r
            && (r.getStatus() == PENDING || r.getStatus() == PROCESSING
                || (r.getStatus() == COMPLETED && !r.isConsumed()))) {
        return r.getTaskId();
    }

    // 构造新结果
    AiSuggestionResult newResult = new AiSuggestionResult();
    newResult.setTaskId(candidateTaskId);
    newResult.setStatus(PENDING);
    newResult.setCreateTime(LocalDateTime.now());

    // ★ 先存入 candidateTaskId（消除跨 key 竞态窗口）
    suggestionStore.put(candidateTaskId, newResult);

    // 尝试原子声明 dedupKey
    Object oldValue = suggestionStore.createIfNotExists(dedupKey, newResult);
    if (oldValue == null) return candidateTaskId;

    // createIfNotExists 返回非 null → 已有任务
    if (oldValue instanceof AiSuggestionResult r
            && (r.getStatus() == PENDING || r.getStatus() == PROCESSING
                || (r.getStatus() == COMPLETED && !r.isConsumed()))) {
        return r.getTaskId();
    }

    // compute 原子判定 winner
    Object result = suggestionStore.compute(dedupKey, (key, currentValue) -> {
        if (currentValue instanceof AiSuggestionResult current
                && (current.getStatus() == PENDING || current.getStatus() == PROCESSING
                    || (current.getStatus() == COMPLETED && !current.isConsumed()))) {
            return current;
        }
        return newResult;
    });

    if (result == newResult) return candidateTaskId;
    if (result instanceof AiSuggestionResult winner) return winner.getTaskId();
    throw new IllegalStateException("Unexpected value type for dedupKey: " + result);
}
```

### 涉及文件
- `prescription/service/assist/impl/PrescriptionAssistServiceImpl.java`（线程池字段 + 构造函数 @Qualifier + supplyAsync 绑定 + exceptionally 处理）
- `prescription/service/assist/DedupTaskScheduler.java`（schedule 方法调整 put 顺序）
- `prescription/config/PrescriptionThreadPoolConfig.java`（新增 @Bean("aiTaskExecutor")）
- `prescription/.../service/assist/impl/PrescriptionAssistServiceImplTest.java`（构造函数新增 @Mock ExecutorService aiTaskExecutor 参数 + `execute(Runnable)` mock 策略 + 参数计数断言更新）
  - 构造函数参数计数 10→11，`constructorShouldAcceptNineParameters()` 测试需修改：`assertEquals(10, ...)` → `assertEquals(11, ...)`
  - 10 个受影响异步测试需添加 `execute(Runnable)` mock：
    `asyncSuggestionShouldStoreCompletedOnAiSuccess`、`asyncSuggestionShouldStoreCompletedWithSerializedSuggestion`、`asyncSuggestionShouldStoreFailedWhenAsyncAiThrows`、`asyncSuggestionShouldStoreFailedWithTruncatedReason`、`asyncSuggestionShouldStoreFailedOnTimeoutException`、`asyncSuggestionShouldStoreFailedWhenAiResultNotSuccess`、`asyncSuggestionShouldStoreFailedWhenAiResultDataIsNull`、`asyncSuggestionShouldStoreTimeoutOnTimeoutException`、`asyncSuggestionShouldStoreFailedOnInterruptedException`、`asyncSuggestionShouldStoreFailedWhenSerializationFails`
  - Mock 策略示例：
    ```java
    doAnswer(invocation -> {
        Runnable r = invocation.getArgument(0);
        r.run();
        return null;
    }).when(aiTaskExecutor).execute(any(Runnable.class));
    ```
- `prescription/.../service/assist/DedupTaskSchedulerTest.java`（S03 竞态场景 + 5 个现有测试 `never().put` → `times(1).put`）
  - 5 个受影响测试：
    `shouldReuseProcessingTaskViaCreateIfNotExists`、`shouldReuseTaskViaCreateIfNotExistsWhenOldValueIsPending`、`shouldReuseTaskViaCreateIfNotExistsWhenOldValueIsCompletedUnconsumed`、`shouldReuseExistingTaskWhenComputeFindsReusableValue`、`shouldReuseProcessingTaskViaCompute`

### 已有代码上下文
- `PrescriptionAssistServiceImpl.scheduleSuggestionAsync()` 第343-382行：创建 CompletableFuture.supplyAsync → 构造 PROCESSING 状态 → 调用 aiService.prescriptionAssist() → COMPLETED/FAILED/TIMEOUT → suggestionStore.put
- `DedupTaskScheduler.schedule()` 第21-72行：快路径检查 → createIfNotExists + put → compute + put 三阶段
- `DedupTaskSchedulerTest` 已有 14 个测试用例覆盖 PENDING/PROCESSING/COMPLETED+consumed/FAILED/TIMEOUT/non-AiSuggestionResult 等调度行为
- `PrescriptionAssistServiceImplTest` 已有 ~30 个测试用例，含 asyncSuggestion 管线测试（使用 Thread.sleep(300) 等待异步完成）
- 线程池 bean 目前无统一配置，需在此轮新增

## 修订说明（v1 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] R1 实施方式不明确（CompletableFuture vs RestTemplate 二选一） | 明确为唯一方案：CompletableFuture.supplyAsync + .get(timeout, SECONDS)，利用现有 catch 块；在 R1 中补充"不采用 RestTemplate 方案的理由" |
| [一般] R1 缺少 task_v1.md 要求的 WARN 日志格式/空列表兜底/保持现有结构三项行为细节 | R1 补充了调用耗时/异常类型/科室ID 的日志说明、异常时跳过该 department 的空列表兜底行为、复用现有 try/catch 结构的确认 |
| [一般] 任务 16「P1 其余低风险项批量修复」粒度过粗（~30 项跨 4 模块） | 拆分为 3 个子任务（按 consultation/prescription/medical-record+application 模块分拆），每项 ≤ 3 人时，附子项清单 |
| [一般] P2 项 T24（ConcurrentHashMapStore 缺少 @Service/接口缺失）未被覆盖 | 将 T24 加入任务 19（P2 批量），子项清单含 C18/T45/T24/M02/M11/P09/P12/P15/A08 |
| [轻微] 任务 16 命名与内容不一致（含 P2 项 P09/P12/P15/A08） | 将 P2 项（P09/P12/P15/A08）从 P1 子任务移出，统一归入任务 19 |

## 修订说明（v1 r2）
| 审查意见 | 修改措施 |
|---------|---------|
| [一般] P1 问题 C13 未被覆盖 | 已将 C13（TriageRuleEngine.match 补充快照失效回退输出标记规范）补入任务 16（consultation P1 批量）子项清单 |

## 修订说明（v2 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] 原计划 20 轮次粒度过细 | 压缩为 10 轮，合并同模块/同主题任务，提升每轮工作量密度 |
| [一般] 缺少实施路线总览表 | 在 plan.md 开头新增「实施路线（10轮合并版）」表格，含序号/任务项/工作量/状态，后续逐轮更新 ✅ 标记 |

## 修订说明（v4 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] AiSuggestionStatusTest.java 断言在枚举扩充后必然失败 | 补充 AiSuggestionStatusTest.java 修改：`assertEquals(3,...) → assertEquals(5,...)`，追加 PROCESSING/TIMEOUT 的 valueOf 断言；R4 变更清单补充此测试文件 |
| [一般] DedupTaskScheduler 未适配 PROCESSING/TIMEOUT 新状态 | 补充 DedupTaskScheduler.java 修改：`schedule()` 中 3 处 `PENDING` 检查追加 `|| r.getStatus() == AiSuggestionStatus.PROCESSING` 条件，防止 PROCESSING 状态任务被重复调度 |
| [一般] scheduleSuggestionAsync 中 TimeoutException 未使用 TIMEOUT 状态 | 拆分 `catch (ExecutionException | TimeoutException)` 为独立 catch 块，TimeoutException 分支设为 `AiSuggestionStatus.TIMEOUT` |
| [一般] 测试文件修改清单不完整 | 补全测试文件清单：AiSuggestionStatusTest.java（枚举值 + valueOf 断言）、PrescriptionAssistServiceImplTest.java（构造函数新增 DrugFacade mock）、DedupTaskSchedulerTest.java（PROCESSING/TIMEOUT 状态调度行为测试） |

## 修订说明（v5 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] Constructor 缺少 @Qualifier | 补 `@Qualifier("aiTaskExecutor")`，新增 import |
| [一般] S03 破坏 5 个现有测试 | 列出 5 个需改 `never().put` → `times(1).put` 的测试 |
| [一般] aiTaskExecutor mock 策略错误（submit → execute） | 修正为 `execute(Runnable)` mock，列出 10 个受影响测试 |
| [轻微] 配置类包路径未指定 | 指定为 `com.aimedical.modules.prescription.config.PrescriptionThreadPoolConfig` |

## 修订说明（v5 r2）
| 审查意见 | 修改措施 |
|---------|---------|
| [一般] P01「变更明细」第2项遗漏 @Qualifier：修订说明声称已补充但正文仍为模糊表述 | 正文第89行 `构造函数新增 ExecutorService` → `构造函数新增 @Qualifier("aiTaskExecutor") ExecutorService`，与 task_v5.md 保持一致 |
| [一般] 配置类包路径未按修订说明修正：正文仍保留「或通过已有 common-module 注入」模糊选项 | 正文第92行删除模糊选项，改为精确路径 `com.aimedical.modules.prescription.config.PrescriptionThreadPoolConfig` |
| [一般] 测试变更描述未体现修订说明：缺少具体测试清单和 mock 策略 | 正文「涉及文件」补充 10 个 PrescriptionAssistServiceImplTest 受影响测试名 + `execute(Runnable)` mock 代码示例；补充 5 个 DedupTaskSchedulerTest 受影响测试名（`never().put` → `times(1).put`） |

## 修订说明（v5 r3）
| 审查意见 | 修改措施 |
|---------|---------|
| [一般] constructorShouldAcceptNineParameters 测试断言 assertEquals(10,...) 在新增 ExecutorService 参数后将失败 | 在「涉及文件 — PrescriptionAssistServiceImplTest」中补充修改说明：assertEquals(10, ...) → assertEquals(11, ...) |

---

## R6 PASSED prescription: P01 — 异步 AI 调度线程池绑定 + S03 — DedupTaskScheduler 跨 key 竞态

结果：PrescriptionAssistServiceImpl 新增 ExecutorService aiTaskExecutor 字段 + 构造函数 @Qualifier 注入 + supplyAsync 绑定 aiTaskExecutor + exceptionally 异常处理。DedupTaskScheduler.schedule() 中 put(candidateTaskId, newResult) 提前无条件执行，消除跨 key 竞态窗口。新增 PrescriptionThreadPoolConfig 配置类 (@Bean("aiTaskExecutor") ThreadPoolExecutor 有界线程池)。
涉及文件：PrescriptionAssistServiceImpl.java / DedupTaskScheduler.java / PrescriptionThreadPoolConfig.java / PrescriptionAssistServiceImplTest.java / DedupTaskSchedulerTest.java
测试：1604 用例通过，0 失败，0 错误，6 跳过。

## R6 NEW prescription/ai-impl/consultation/medical-record: P03/S02 + A05 + T40/E05 + M04 TTL 清理 + MockAiService 配置 + @Recover + 乐观锁

任务：修复 4 个模块的 5 项缺陷，均为 P1/P2 级别，互不依赖，可并行实现。

选择理由：R5 已完成 P01+S03（prescription 异步 AI 调度 + 跨 key 竞态）修复并验证通过（1604 用例，0 失败）。按计划推进路线表第 6 项。

### 1a. P03/S02 — DraftContextCleanupTask.recordWrite() 无调用方

**问题定位**：
- `DraftContextCleanupTask.java:27-29` `recordWrite()` 将时间戳写入 `writeTimestamps`，但无任何调用方
- `cleanupExpiredDrafts()` 第38行遍历 `draftContextStore.keySet()`，第39行 `writeTimestamps.get(key)` 始终返回 null，清理条件永不满足
- `PrescriptionDraftContext.updateCriticalAlerts()` 第34-41行是唯一修改 `draftContextStore` 的位置，但未调用 `recordWrite`

**变更明细**：

1. `PrescriptionDraftContext.java` 新增字段和 import：
   ```java
   import com.aimedical.modules.prescription.task.DraftContextCleanupTask;
   import java.time.Instant;
   ```
   ```java
   private final DraftContextCleanupTask cleanupTask;

   public PrescriptionDraftContext(DraftContextStore draftContextStore,
                                   DraftContextCleanupTask cleanupTask) {
       this.draftContextStore = draftContextStore;
       this.cleanupTask = cleanupTask;
   }
   ```

2. `updateCriticalAlerts()` 追加 recordWrite/removeTimestamp 调用：
   ```java
   public void updateCriticalAlerts(String prescriptionId, List<DosageAlert> alerts) {
       String key = prescriptionId + CRITICAL_ALERTS_SUFFIX;
       if (alerts == null || alerts.isEmpty()) {
           draftContextStore.remove(key);
           cleanupTask.removeTimestamp(key);
       } else {
           draftContextStore.put(key, alerts);
           cleanupTask.recordWrite(key, Instant.now());
       }
   }
   ```

**涉及文件**：
- `prescription/.../context/PrescriptionDraftContext.java`
- `prescription/.../context/PrescriptionDraftContextTest.java`

**测试修改**：
1. `PrescriptionDraftContextTest.java` 新增 `@Mock DraftContextCleanupTask cleanupTask` 字段
2. `setUp()` 中构造函数调用从 `new PrescriptionDraftContext(draftContextStore)` 改为 `new PrescriptionDraftContext(draftContextStore, cleanupTask)`
3. `updateCriticalAlerts` 的 3 个测试追加 `verify(cleanupTask).recordWrite(...)` / `verify(cleanupTask).removeTimestamp(...)` 断言

`DraftContextCleanupTaskTest.java` 无需修改（测试中手动调用 recordWrite 注入 timestamp）

---

### 1b. P03/S02 — SuggestionCleanupTask NPE + FAILED 永不清理

**问题定位**：

问题 1（NPE）：`PrescriptionAssistServiceImpl.java:355-356` — `scheduleSuggestionAsync()` 创建 `AiSuggestionResult result` 仅设 `taskId`，未设 `createTime`。第384行 `suggestionStore.put(taskId, result)` 将无 createTime 的结果存入 store。`SuggestionCleanupTask.isExpiredAndConsumed()` 第46行 `entry.getTimestamp().plusSeconds(...)` 对 null 调用方法抛出 NPE，不被 `catch (ClassCastException)`（第36行）捕获，导致 `cleanupExpiredSuggestions()` 异常终止。

问题 2（FAILED 永不清理）：`isExpiredAndConsumed()` 第44-46行要求 `isCompletedOrFailed && entry.isConsumed() && isExpired`，但 FAILED 条目的 `consumed` 默认 false，且仅 `getSuggestion()` 对 COMPLETED 状态设 `consumed=true`。FAILED 条目永不被清理。

**变更明细**：

1. `PrescriptionAssistServiceImpl.java` 第355行后追加 `result.setCreateTime(LocalDateTime.now())`：
   - 新增 import: `import java.time.LocalDateTime;`（当前文件无此导入，必须显式追加）

2. `SuggestionCleanupTask.java` 第42-47行 `isExpiredAndConsumed()` 重构：
   ```java
   private boolean isExpiredAndConsumed(SuggestionStoreEntry entry, Instant now) {
       String status = entry.getStatusName();
       boolean isCompleted = "COMPLETED".equals(status);
       boolean isFailed = "FAILED".equals(status);
       boolean isExpired = entry.getTimestamp() != null
               && entry.getTimestamp().plusSeconds(TTL_MINUTES * 60).isBefore(now);
       if (isCompleted) {
           return entry.isConsumed() && isExpired;
       }
       if (isFailed) {
           return isExpired;
       }
       return false;
   }
   ```
   - 删除原实现，替换上述逻辑
   - null-safe 处理 `entry.getTimestamp()`

**涉及文件**：
- `prescription/.../service/assist/impl/PrescriptionAssistServiceImpl.java`
- `prescription/.../task/SuggestionCleanupTask.java`

**测试修改**（`SuggestionCleanupTaskTest.java`）：
1. 重命名：`shouldRemoveExpiredFailedAndConsumedEntry` → `shouldRemoveExpiredFailedEntryEvenIfNotConsumed`，测试数据改 `consumed=false`
2. 新增：`shouldNotRemoveFailedEntryWhenNotExpired`

---

### 2. A05 — MockAiService @Profile → @ConditionalOnProperty

**问题定位**：`MockAiService.java:40-41` 使用 `@Service` + `@Profile("mock")`，与 OOD 要求的 `@ConditionalOnProperty` 不一致，需通过配置属性开关而非 profile 控制启用。

**变更明细**：
```java
// 删除 @Profile("mock")
// 新增 @ConditionalOnProperty
@Service
@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "true", matchIfMissing = false)
public class MockAiService implements AiService {
```
- 删除 `import org.springframework.context.annotation.Profile`
- 新增 `import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty`

**涉及文件**：`ai/ai-impl/src/main/java/com/aimedical/modules/ai/impl/mock/MockAiService.java`

---

### 3. T40/E05 — @Recover 方法缺陷

**问题定位**：
1. `RegistrationEventListener.java:52-66` `recover()` 无 `@Transactional`，第65行 `deadLetterEventRepository.save` 无事务保护
2. 第60行 `e.getMessage()` 可能为 null，违反 `failReason` 的 `@Column(nullable = false)`
3. 第58行 JSON 兜底 `"{\"sessionId\":\"" + event.getSessionId() + "\"}"`，sessionId 为 null 时输出 `"null"` 字符串

**变更明细**：

1. 第52行添加 `@Transactional`：
   ```java
   @Recover
   @Transactional
   public void recover(Exception e, RegistrationEvent event) {
   ```

2. 第60行 null failReason 防护：
   ```java
   deadLetter.setFailReason(e.getMessage() != null ? e.getMessage() : "Unknown failure reason");
   ```

3. 第58行 JSON 兜底 null sessionId 防护：
   ```java
   String sid = event.getSessionId() != null ? event.getSessionId() : "unknown";
   deadLetter.setEventPayload("{\"sessionId\":\"" + sid + "\"}");
   ```

**涉及文件**：`consultation/.../event/RegistrationEventListener.java`

---

### 4. M04 — 乐观锁不可触发

**问题定位**：`MedicalRecordServiceImpl.java:102-103` `findByVisitId(visitId).orElseGet(MedicalRecord::new)` — 记录不存在时创建无 ID/version 的新实体，`save()` 执行 INSERT。`@Version` 乐观锁仅在 UPDATE 路径触发，INSERT 路径不触发 `ObjectOptimisticLockingFailureException`，第114行 catch 块不可达。

**变更明细**：

1. `MedicalRecord.java` 第34行 visitId 添加 `unique = true`：
   ```java
   @Column(nullable = false, unique = true)
   private String visitId;
   ```

2. `MedicalRecordServiceImpl.java` 第114行后追加 `DataIntegrityViolationException` catch：
   ```java
   import org.springframework.dao.DataIntegrityViolationException;

   try {
       medicalRecordRepository.save(entity);
   } catch (ObjectOptimisticLockingFailureException e) {
       log.warn("Optimistic lock conflict on medical record save", e);
       RecordGenerateResponse response = medicalRecordConverter.toRecordGenerateResponse(aiResult, hints);
       response.setErrorCode(MedicalRecordErrorCode.MR_GEN_CONCURRENT_MODIFICATION);
       response.setFromFallback(visitIdFallback);
       return response;
   } catch (DataIntegrityViolationException e) {
       log.warn("Concurrent INSERT conflict on medical record for visitId: {}", visitId, e);
       RecordGenerateResponse response = medicalRecordConverter.toRecordGenerateResponse(aiResult, hints);
       response.setErrorCode(MedicalRecordErrorCode.MR_GEN_CONCURRENT_MODIFICATION);
       response.setFromFallback(visitIdFallback);
       return response;
   }
   ```

**涉及文件**：
- `medical-record/.../entity/MedicalRecord.java`
- `medical-record/.../service/impl/MedicalRecordServiceImpl.java`

---

### 涉及文件汇总

| 模块 | 文件路径（相对 `AIMedical/backend/modules/`） | 操作 |
|------|----------------------------------------------|------|
| prescription | `prescription/src/main/java/.../context/PrescriptionDraftContext.java` | 修改 |
| prescription | `prescription/src/main/java/.../service/assist/impl/PrescriptionAssistServiceImpl.java` | 修改 |
| prescription | `prescription/src/main/java/.../task/SuggestionCleanupTask.java` | 修改 |
| prescription | `prescription/src/test/java/.../task/SuggestionCleanupTaskTest.java` | 修改 |
| ai/ai-impl | `ai/ai-impl/src/main/java/.../mock/MockAiService.java` | 修改 |
| consultation | `consultation/src/main/java/.../event/RegistrationEventListener.java` | 修改 |
| medical-record | `medical-record/src/main/java/.../entity/MedicalRecord.java` | 修改 |
| medical-record | `medical-record/src/main/java/.../service/impl/MedicalRecordServiceImpl.java` | 修改 |

### 已有代码上下文

以上所有文件的完整源码已在 workspace 确认，路径前缀 `com.aimedical.modules`，具体路径见上表。

## 修订说明（v6 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] R6 NEW 节缺少实施细节，与 R5 NEW 质量标准严重背离 | 按 R5 NEW 质量标准扩充：为每个缺陷补充问题定位（精确到行号）、变更明细（具体代码改动）、文件清单、测试修改说明 |
| [一般] 缺少 DraftContextCleanupTask 修改方案 | 新增 1a 子节：PrescriptionDraftContext 新增 cleanupTask 字段注入 + updateCriticalAlerts 中 put/remove 后调用 recordWrite/removeTimestamp |
| [一般] 缺少 SuggestionCleanupTaskTest 修改方案 | 1b 子节补充测试修改：测试重命名 + 数据改 consumed=false + 新增 shouldNotRemoveFailedEntryWhenNotExpired |
| [一般] 缺少 T40/E05 null sessionId 防护细节 | 3 子节补充代码细节：String sid = event.getSessionId() != null ? ... : "unknown" |
| [一般] 缺少 M04 完整修改方案（unique=true + DataIntegrityViolationException） | 4 子节补充完整代码：visitId 添加 unique=true + catch DataIntegrityViolationException 处理 INSERT 并发冲突 |
| [轻微] 文件路径使用 `...` 通配符 | MockAiService 路径修正为完整路径 `ai/ai-impl/src/main/java/.../mock/MockAiService.java`；涉及文件汇总表使用固定前缀结构 |

## 修订说明（v6 r2）
| 审查意见 | 修改措施 |
|---------|---------|
| [一般] PrescriptionDraftContext 构造函数变更导致 PrescriptionDraftContextTest 编译失败 | 1a 节补充 PrescriptionDraftContextTest.java 修改清单：新增 cleanupTask mock 字段、构造函数调用改为 2 参数、updateCriticalAlerts 测试追加 verify 断言 |
| [轻微] 缺少 import java.time.LocalDateTime 的明确标注 | 1b 节 PrescriptionAssistServiceImpl.java 变更明细中明确标注「新增 import: import java.time.LocalDateTime;（当前文件无此导入，必须显式追加）」 |

---

## R7 FAILED A05 — MockAiServiceTest 未适配注解变更（@Profile → @ConditionalOnProperty）

结果：MockAiServiceTest 已替换为 shouldBeAnnotatedWithConditionalOnProperty，但注解 name() 返回 String[] 而非 String。

测试日志（verify_v7.md:934）：
```
MockAiServiceTest.shouldBeAnnotatedWithConditionalOnProperty:42 
expected: <ai.mock.enabled> but was: <[ai.mock.enabled]>
```

失败原因：`annotation.name()` 返回 `String[]`，而 test 用 `assertEquals(String, String[])`，类型不匹配。

---

## R8 RETRY A05 — MockAiServiceTest annotation.name() 类型修正

任务：修复 shouldBeAnnotatedWithConditionalOnProperty 中 annotation.name() 的断言类型。

选择理由：R7 测试替换方向正确，但 assertEquals(String, String[]) 类型不匹配，需改为 assertArrayEquals。

上下文：MockAiServiceTest.java:42 — `assertEquals("ai.mock.enabled", annotation.name())` → `assertArrayEquals(new String[]{"ai.mock.enabled"}, annotation.name())`

---

## R9 PASSED A05 — MockAiServiceTest annotation.name() 类型修正（v8）

结果：assertEquals → assertArrayEquals，402 用例通过，0 失败，v8 PASSED。

## R9 NEW consultation: P1+P2 低风险批量修复

任务：修复 consultation 模块剩余的 5 项 P1/P2 缺陷，均为低风险小改动。

选择理由：路线表第 7 项，consultation 模块所有 P0 缺陷已在 R1-R3 修复完毕，剩余 P1/P2 项均为单文件/单方法小改动。

### 子项清单

| 编号 | 诊断编号 | 级别 | 描述 | 涉及文件 | 预估工作量 |
|:---:|:--------:|:----:|------|---------|:---------:|
| 7a | C13 | P1 | DefaultTriageRuleEngine.match 补充快照失效回退输出标记规范 | `DefaultTriageRuleEngine.java` | 0.3人时 |
| 7b | T4 | P1 | TriageServiceImpl 降级路径手工构造 Response → 复用 TriageConverter | `TriageServiceImpl.java` | 0.5人时 |
| 7c | T42 | P1 | DefaultTriageRuleEngine 缓存事件驱动失效（@EventListener on RuleChangeEvent） | `DefaultTriageRuleEngine.java` | 0.5人时 |
| 7d | C18 | P2 | saveTriageRecord catch JsonProcessingException 增强日志（warn→error + 原文） | `TriageServiceImpl.java` | 0.3人时 |
| 7e | T45 | P2 | TriageServiceImpl.selectDepartment / RegistrationEventListener null sessionId 防护 | `TriageServiceImpl.java`, `RegistrationEventListener.java` | 0.4人时 |

### 涉及文件汇总

| 文件路径（相对 `AIMedical/backend/modules/consultation/`） | 操作 | 子项 |
|----------------------------------------------------------|------|:----:|
| `src/main/java/.../rule/DefaultTriageRuleEngine.java` | 修改 | 7a, 7c |
| `src/main/java/.../service/impl/TriageServiceImpl.java` | 修改 | 7b, 7d, 7e |
| `src/main/java/.../event/RegistrationEventListener.java` | 修改 | 7e |

### 已有代码上下文

- `DefaultTriageRuleEngine.java`：第30-37行 Caffeine 缓存 `refreshAfterWrite(60s)`；第55-59行 `ruleVersionMismatch` 检测已实现；第82-116行 `matchesConditions()` JSON 解析已修复
- `TriageServiceImpl.java`：第167-181行降级路径 `if (aiResult == null)` 手工构造 `TriageResponse`；第251-253行 `catch (JsonProcessingException)` 仅 `log.warn`；第45行 `selectDepartment` 参数 `registrationEvent.getSessionId()` 无 null 检查
- `RegistrationEventListener.java`：第45行 `event.getSessionId()` 可能 null；第52-66行 `@Recover` 已修复（R6）

## 修订说明（v9 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [一般] 缺少测试修改计划 | 为每项子任务补充测试方案：7a 新增 log 断言测试(DefaultTriageRuleEngineTest)；7b 新增 toFallbackTriageResponse 测试(TriageConverterTest)；7c 新增缓存配置验证测试(DefaultTriageRuleEngineTest)；7d 新增日志级别断言测试(TriageServiceImplTest)；7e 新增 null sessionId 防护测试(RegistrationEventListenerTest) |
| [一般] 7a(C13) MatchResult 已有 ruleVersionMismatch 字段，计划描述与现状脱节 | 移除"检查字段是否存在"描述；实际剩余工作为在 DefaultTriageRuleEngine.match() 中添加 log.warn 记录回退行为，补充对应测试 |
| [一般] 7c(T42) RuleChangeEvent 不存在，TODO 方案不解决缓存失效问题 | 删除 TODO 方案；改为 refreshAfterWrite(60s) → expireAfterWrite(30s)，缩短缓存不一致窗口；补充缓存配置验证测试 |
| [一般] 7e(T45) 异常处理方案二选一 | 选定方案：null sessionId 时 log.warn + return（静默跳过），异常不冒泡；同时 TriageServiceImpl.selectDepartment 入口追加 Objects.requireNonNull 防御 |
| [轻微] 7b(T4) toTriageResponse 兼容性未验证 | 确认现有签名不可复用（依赖 AiResult 参数）；改为新增 toFallbackTriageResponse 重载方法，接收 departments/doctors/sessionId/reason/ruleVersionMismatch/hint 参数 |

---

## R10 PASSED consultation: P1+P2 低风险批量修复

结果：完成 consultation 模块 5 项 P1/P2 缺陷修复（C13 回退日志 + T4 toFallbackTriageResponse + T42 expireAfterWrite(30s) + C18 日志级别提升 + T45 null sessionId 防护）。
涉及文件：DefaultTriageRuleEngine.java / TriageConverter.java / TriageServiceImpl.java / RegistrationEventListener.java / DefaultTriageRuleEngineTest.java / TriageConverterTest.java / TriageServiceImplTest.java / RegistrationEventListenerTest.java
测试：198 用例通过，0 失败，5 跳过（v9 verify PASSED）。

## R10 NEW prescription: 剩余 P1+P2 批量修复

任务：修复 prescription 模块剩余的 10 项 P1/P2 缺陷，均为单文件/单方法小改动。

选择理由：路线表第 8 项，prescription 模块 P0 缺陷已在 R4-R6 修复完毕，剩余 P1/P2 项均为小改动。

### 子项清单

| 编号 | 诊断编号 | 级别 | 描述 | 涉及文件 | 预估工作量 |
|:---:|:--------:|:----:|------|---------|:---------:|
| 8a | T9 | P1 | DosageThresholdService 频率解析 NumberFormatException 被空 catch 吞掉 | `DosageThresholdService.java` | 0.3人时 |
| 8b | T10 | P1 | PrescriptionDraftContext.getCriticalAlerts @SuppressWarnings("unchecked") | `PrescriptionDraftContext.java` | 0.2人时 |
| 8c | T14 | P1 | PrescriptionItem.dose 使用 double → BigDecimal | `PrescriptionItem.java` | 0.5人时 |
| 8d | T15 | P1 | prescriptionOrderId 使用 System.currentTimeMillis() → UUID | `PrescriptionAuditServiceImpl.java` | 0.5人时 |
| 8e | T28 | P1 | hasNewAlerts 死代码（BLOCK 分支不可达） | `PrescriptionAuditServiceImpl.java` | 0.3人时 |
| 8f | T30 | P1 | DosageLimitRule.findBestMatch 返回 null 静默回退 | `DosageLimitRule.java` | 0.2人时 |
| 8g | T31 | P1 | AllergyCheckRule allergyHistory contains 子串匹配过于激进 | `AllergyCheckRule.java` | 0.3人时 |
| 8h | T35 | P1 | AuditConverter + ai-api DTO 遗漏 weight 和 unit 字段映射 | `AuditConverter.java` | 0.5人时 |
| 8i | P12 | P2 | DrugInteractionRule 无 Phase 4 预留 @ConditionalOnProperty 标注 | `DrugInteractionRule.java` | 0.2人时 |
| 8j | P15 | P2 | 降级 BLOCK 路径 reasons 固定字符串 | `PrescriptionAuditServiceImpl.java`, `PrescriptionAuditController.java` | 0.5人时 |
| 8k | P10 | P1 | DosageThresholdService.matchByPriority 循环逻辑重复 | `DosageThresholdService.java` | 0.5人时 |
| 8l | P14 | P1 | 异步失败路径未清理 DraftContext | `PrescriptionAssistServiceImpl.java` | 0.5人时 |
| 8m | T27 | P1 | submit() 并发提交防护 | `PrescriptionAuditServiceImpl.java` | 0.5人时 |
| 8n | T16 | P1 | hasCriticalAlerts/getCriticalAlerts TOCTOU 窗口 | `PrescriptionDraftContext.java`, `PrescriptionAuditServiceImpl.java` | 0.5人时 |

### 涉及文件汇总

| 文件路径（相对 `AIMedical/backend/modules/`） | 操作 | 子项 | 测试文件 |
|------|------|:----:|---------|
| `prescription/.../service/assist/DosageThresholdService.java` | 修改 | 8a, 8k | `DosageThresholdServiceTest.java` |
| `prescription/.../context/PrescriptionDraftContext.java` | 修改 | 8b, 8n | `PrescriptionDraftContextTest.java` |
| `prescription/.../dto/audit/PrescriptionItem.java` | 修改 | 8c | `PrescriptionItemTest.java`, `AuditConverterTest.java`, `DosageLimitRuleTest.java`, `PrescriptionAuditServiceImplTest.java` |
| `prescription/.../rule/DosageLimitRule.java` | 修改 | 8c, 8f | `DosageLimitRuleTest.java` |
| `prescription/.../converter/AuditConverter.java` | 修改 | 8c, 8h | `AuditConverterTest.java` |
| `prescription/.../service/audit/impl/PrescriptionAuditServiceImpl.java` | 修改 | 8d, 8e, 8j, 8m, 8n | `PrescriptionAuditServiceImplTest.java` |
| `prescription/.../service/assist/impl/PrescriptionAssistServiceImpl.java` | 修改 | 8l | `PrescriptionAssistServiceImplTest.java` |
| `prescription/.../rule/AllergyCheckRule.java` | 修改 | 8g | `AllergyCheckRuleTest.java` |
| `ai/ai-api/.../dto/prescription/PrescriptionCheckItem.java` | 修改 | 8h | — |
| `ai/ai-api/.../dto/prescription/PatientInfo.java` | 修改 | 8h | — |
| `prescription/.../rule/DrugInteractionRule.java` | 修改 | 8i | `DrugInteractionRuleTest.java` |
| `prescription/.../api/PrescriptionAuditController.java` | 修改 | 8j | `PrescriptionAuditControllerTest.java` |

### 已有代码上下文

- `DosageThresholdService.java:76-89`：parseInt 对非数字频率字符串抛 NumberFormatException，空 catch 吞掉异常；第95-147行 matchByPriority 5个循环结构重复
- `PrescriptionDraftContext.java:24-32`：`@SuppressWarnings("unchecked")` 下 `(List<DosageAlert>) value` 无泛型类型校验；hasCriticalAlerts 与 getCriticalAlerts 两次读取存在 TOCTOU 窗口
- `PrescriptionItem.java:7`：dose 字段使用 double，OOD §1.3 要求 BigDecimal
- `PrescriptionAuditServiceImpl.java:218,259,289,298,328`：五处使用 `"RX-" + System.currentTimeMillis()`；第183-192行 BLOCK 分支不可达；第144-152行 reasons 固定字符串；第150-199行 submit 无 per-prescriptionId 并发控制
- `PrescriptionAssistServiceImpl.java:388-393`：scheduleSuggestionAsync exceptionally 回调未清理 DraftContext，异步 AI 失败后 submit 被阻塞
- `DosageLimitRule.java:36-38`：`findBestMatch` 返回 null 时回退到 `standards.get(0)` 无校验
- `AllergyCheckRule.java:54-58`：contains 子串匹配导致否定义命中
- `AuditConverter.java:66-75`：toAiCheckItem 未映射 unit；第77-92行 toAiPatientInfo 未映射 weight
- `DrugInteractionRule.java:1-15`：缺少 @ConditionalOnProperty 标注
- `PrescriptionAuditController.java:36-48`：setFallback reasons 固定字符串

## 修订说明（v10 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [一般] 缺少测试修改计划 | 已为 10 个子项逐项补充：8a DosageThresholdServiceTest 日志断言；8b PrescriptionDraftContextTest 非法类型路径；8c 更新 4 个测试文件断言类型；8d PrescriptionAuditServiceImplTest ID 格式；8e 确认不可达代码无测试；8f 不影响现有测试；8g AllergyCheckRuleTest 否定前缀+单词边界；8h AuditConverterTest unit/weight；8i DrugInteractionRuleTest assertArrayEquals；8j ControllerTest/ServiceImplTest reason 透传 |
| [严重] 8c double→BigDecimal 级联影响 | 完成全量分析：DosageLimitRule.java:40 `BigDecimal.valueOf()` → `item.getDose()`；AuditConverter.java:70 `setDose(item.getDose())` → `.doubleValue()`；4 个测试文件 setDose 参数类型适配 |
| [一般] 8d UUID 方案未明确 | 明确为 `"RX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()`，8 位 hex |
| [一般] 8f null 后 NPE 风险 | 分析确认 `standards` 非空保护已存在，仅增加 `log.warn` |
| [一般] 8j 方法签名/兼容性 | 确认不涉及签名变更，Controller 从 response.getAlerts 提取原因，Service 从 snapshotCriticalAlerts 提取 |

## 修订说明（v10 r2）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] P10、P14、T27 未覆盖 | 已补入 task_v10.md 子项清单：8k（P10 matchByPriority 循环重构）、8l（P14 exceptionally 追加 clearCriticalAlerts）、8m（T27 submit 入口 per-prescriptionId 锁） |
| [一般] P09、T8 归属不明 | P09 确认代码已存在，推迟至第 9 项 OOD 对齐；T8 跨模块依赖推迟至第 9 项 |
| [一般] 第 9 项工作量不足 | task_v10.md 补充工作量重估，执行时建议拆分为 2~3 子轮次 |

## 修订说明（v10 r3）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] T16（P1, prescription）完全未覆盖 | 已补入 8n：PrescriptionDraftContext 新增 snapshotCriticalAlerts 原子快照方法 + doSubmit 中单次快照替代 has/get 两次读取。 |
| [一般] 8l（P14）exceptionally 与 submit() 并发冲突未分析 | 已在 task_v10.md 8l 节补充并发风险分析，结论：风险可接受。 |
| [一般] Round 9 清单遗漏 A11（P2） | 已在「路线表第 9 项工作量重估」中补充 A11。 |
| [一般] 8a T9 缺少 Logger 字段声明 | 已在 task_v10.md 8a 节补充 Logger 字段 + import 声明。 |
| [轻微] Plan round 编号与路线表序号不一致 | 路线表第 8 项对应 R10（因 R7/R8/R9 的 A05 retry 轮次插入）。已在路线表注中加注。 |

## 修订说明（v10 r4 — 同步 plan.md 正文与 task_v10.md 一致性）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] plan.md R10 节仅含 8a-8j（10项），与 task_v10.md 的 8a-8n（14项）不一致 | plan.md 子项清单补入 8k（P10）、8l（P14）、8m（T27）、8n（T16），现与 task_v10.md 完全一致，均为 14 项 |
| [严重] plan.md 文件汇总表落后于 task_v10.md | 替换为完整 12 行文件表，含 PrescriptionAssistServiceImpl（8l）、PrescriptionCheckItem/PatientInfo（8h）等缺失条目及对应测试文件 |
| [一般] plan.md 路线表工作量未反映 14 个子项 | 路线表第 8 项工作量从 4人时 更新为 6人时 |
| [一般] plan.md 已有代码上下文缺少 8k-8n | 补充 DosageThresholdService.matchByPriority 循环重复、PrescriptionAssistServiceImpl exceptionally 未清理、submit 并发控制缺失、TOCTOU 窗口等上下文描述 |

---

## R10 FAILED prescription: 剩余 P1+P2 批量修复（v10 验证失败，5 项错误）

结果：v10 verify 报告 1519 通过 / 5 失败 / 6 跳过，prescription 模块 5 个测试错误。

失败详情：

| # | 测试类 | 方法 | 类型 | 根因 |
|---|--------|------|------|------|
| F1 | PrescriptionDraftContextTest | updateCriticalAlertsShouldPutWhenNonEmpty:116 | ERROR | Mockito matcher 与 raw value 混用：`verify(cleanupTask).recordWrite("rx-001:criticalAlerts", any(Instant.class))` 应改为 `eq("rx-001:criticalAlerts")` |
| F2 | AuditConverterTest | shouldMapWeightFieldToAiPatientInfo:99 | ERROR | PrescriptionItem.dose 为 null（BigDecimal 默认值）→ `getDose().doubleValue()` NPE。测试中 `new PrescriptionItem()` 未设 dose |
| F3 | AuditConverterTest | shouldMapWeightAsNullWhenNotSet:132 | ERROR | 同上 |
| F4 | AllergyCheckRuleTest | shouldSkipWhenNegationPrefixFound:244 | FAILURE | `hasNegationPrefix("No allergy to ")`：方法使用 `contains("no ")` 但文本首字母大写，大小写敏感导致不匹配 |
| F5 | PrescriptionAssistServiceImplTest | asyncSuggestionShouldClearCriticalAlertsOnExceptionally:1017 | FAILURE | verify 期望 `updateCriticalAlerts("rx-001", [])`，但 `assistRequest.getPrescriptionId()` 为 null → assist() 自动生成 UUID，实际参数为 UUID |

修正方向：
- F1: recordWrite 第一个参数改用 `eq()` matcher
- F2/F3: 两条测试中 `new PrescriptionItem()` 补设 dose
- F4: hasNegationPrefix 改为忽略大小写（text.toLowerCase()）
- F5: 测试中 `service.assist()` 前补 `assistRequest.setPrescriptionId("rx-001")`

## R11 PASSED prescription: 剩余 P1+P2 批量修复 — 5 项测试错误修正

结果：PrescriptionDraftContextTest F1 改用 eq() matcher；AuditConverterTest F2/F3 补 BigDecimal setDose；AllergyCheckRule F4 hasNegationPrefix 改为大小写不敏感；PrescriptionAssistServiceImplTest F5 补 setPrescriptionId + verify times(2)。826 用例通过，0 失败，5 跳过。

涉及文件：PrescriptionDraftContextTest.java / AuditConverterTest.java / AllergyCheckRule.java / PrescriptionAssistServiceImplTest.java

## R12 NEW 跨模块 + medical-record P1+P2 批量修复

任务：修复 diagnosis 报告中剩余的 17 项 P1/P2 缺陷（9a-9q），涉及 medical-record、application/跨模块、consultation、ai-impl、prescription 五个模块。

选择理由：路线表第 9 项。R11 已完成所有 prescription 模块 P1/P2 批量修复。剩余缺陷分布在其他模块，集中一轮批量处理。

### 子项清单

| 编号 | 诊断编号 | 级别 | 模块 | 描述 | 涉及文件 | 预估 |
|:---:|:--------:|:----:|:----:|------|---------|:----:|
| 9a | M02 | P1 | medical-record | DatabaseTemplateConfigManager @EventListener 缓存失效使用 invalidateAll() 而非 invalidate(key) | `DatabaseTemplateConfigManager.java` | 0.3h |
| 9b | M11 | P1 | medical-record | ai-api MedicalRecordGenResponse.missingFields 与 partialContent 未被业务层消费（依赖 9e） | `MedicalRecordConverter.java` | 0.3h |
| 9c | T17 | P1 | medical-record | callAiWithTimeout 三种异常统一返回"timeout"错误码，语义混淆 | `MedicalRecordServiceImpl.java` | 0.5h |
| 9d | T18 | P1 | medical-record | MedicalRecordConverter 使用字面字符串而非 MedicalRecordErrorCode 枚举 | `MedicalRecordConverter.java` | 0.3h |
| 9e | T21 | P1 | medical-record | 元数据字段 MISSING_FIELDS/PARTIAL_CONTENT 混入 content_map | `MedicalRecordConverter.java`, `DatabaseTemplateConfigManager.java` | 0.5h |
| 9f | T22 | P1 | medical-record | MedicalRecordContentConverter 序列化异常静默返回 null/emptyMap，无 WARN 日志 | `MedicalRecordContentConverter.java` | 0.3h |
| 9g | T47 | P1 | medical-record | MedicalRecord @PrePersist 仅设置 createdAt，未设置 updatedAt | `MedicalRecord.java` | 0.2h |
| 9h | T48 | P1 | medical-record | MedicalRecordServiceImpl.resolveVisitId 使用 ForkJoinPool.commonPool() | `MedicalRecordServiceImpl.java` | 0.5h |
| 9i | T50 | P1 | medical-record | DraftContextStoreImpl 缺少 compute/putIfAbsent 原子操作 | `DraftContextStoreImpl.java`, `DraftContextStore.java` | 0.5h |
| 9j | A06 | P1 | 跨模块 | DegradationStrategy/DegradationContext 降级决策不生效 | `FallbackAiService.java`, `DegradationContext.java` | 1.0h |
| 9k | A09 | P1 | 跨模块 | AuditConverter.toAuditResponse 在 aiData == null 退化为 PASS，调用方未拦截 null | `PrescriptionAuditServiceImpl.java`, `AuditConverter.java` | 0.3h |
| 9l | T19 | P1 | ai-impl | MockAiService.TIMEOUT 使用永不完成的 CompletableFuture，线程资源泄漏 | `MockAiService.java`, `MockAiServiceTest.java` | 0.3h |
| 9m | T20 | P1 | 跨模块 | FallbackAiService.applyStrategies 使用空 DegradationContext | `FallbackAiService.java` | 0h（9j 一并覆盖） |
| 9n | T5 | P1 | consultation | TriageConverter 副作用修改 Session + DialogueSession 混用同步机制 | `TriageConverter.java`, `TriageServiceImpl.java`, `DialogueSession.java` | 0.5h |
| 9o | T32 | P1 | prescription | DraftContextCleanupTask 迭代 keySet() 与 writeTimestamps 非原子 | `DraftContextCleanupTask.java` | 0.3h |
| 9p | A08/T24 | P2 | 跨模块 | A08: 降级 fallback 文案英文混用；T24: ConcurrentHashMapStore 缺 @Service | `TriageServiceImpl.java`, `MedicalRecordServiceImpl.java`, `ConcurrentHashMapStore.java` | 0.3h |
| 9q | A11 | P2 | 跨模块 | PrescriptionAuditServiceImpl.persistAuditRecord 中 `&& getData() != null` 冗余防御检查 | `PrescriptionAuditServiceImpl.java` | 0.1h |

> 执行次序：9e → 9b（9b 依赖 9e），9a 与 9e 修改同一文件不同区域，需注意合并。其余子项可并行。

### 涉及文件汇总

| 文件路径（相对 `AIMedical/backend/modules/`） | 操作 | 子项 |
|------|------|:----:|
| `medical-record/.../config/DatabaseTemplateConfigManager.java` | 修改 | 9a, 9e |
| `medical-record/.../converter/MedicalRecordConverter.java` | 修改 | 9b, 9d, 9e |
| `medical-record/.../service/impl/MedicalRecordServiceImpl.java` | 修改 | 9c, 9h |
| `medical-record/.../converter/MedicalRecordContentConverter.java` | 修改 | 9f |
| `medical-record/.../entity/MedicalRecord.java` | 修改 | 9g |
| `common-module/common-module-api/.../store/DraftContextStoreImpl.java` | 修改 | 9i |
| `common-module/common-module-api/.../store/DraftContextStore.java` | 修改（接口新增） | 9i |
| `ai/ai-impl/.../mock/MockAiService.java` | 修改 | 9l |
| `ai/ai-impl/.../fallback/FallbackAiService.java` | 修改 | 9j, 9m |
| `ai/ai-api/.../degradation/DegradationContext.java` | 不变 | 9j |
| `consultation/.../converter/TriageConverter.java` | 修改 | 9n |
| `consultation/.../service/impl/TriageServiceImpl.java` | 修改 | 9n, 9p |
| `consultation/.../dialogue/DialogueSession.java` | 修改 | 9n |
| `prescription/.../task/DraftContextCleanupTask.java` | 修改 | 9o |
| `prescription/.../converter/AuditConverter.java` | 不变（已有 null 处理） | 9k |
| `prescription/.../service/audit/impl/PrescriptionAuditServiceImpl.java` | 修改 | 9k, 9q |
| `medical-record/.../service/impl/MedicalRecordServiceImpl.java` | 修改 | 9p |
| `common-module/common-module-api/.../store/impl/ConcurrentHashMapStore.java` | 修改 | 9p |

### 关键变更明细

#### 9a — M02: 缓存失效 key 粒度
- `DatabaseTemplateConfigManager.java:107` `templateCache.invalidateAll()` → 判断 `event.getDepartmentCode()`：非 null 时 `invalidate(departmentCode)`，null 时 `invalidateAll()`
- **测试**：`DatabaseTemplateConfigManagerTest` 断言 `assertEquals(9,...)` → `assertEquals(7,...)`（见 9e 说明）；新增缓存失效 key 粒度测试

#### 9e→9b — T21+M11: 排除元数据字段
- `MedicalRecordConverter.java:38-45` 写入 content_map 时跳过 `MISSING_FIELDS`/`PARTIAL_CONTENT`
- `DatabaseTemplateConfigManager.java:111` `DEFAULT_TEMPLATE` 的 `allFields` 排除这两个元数据字段
- **测试**：3 处 `assertEquals(9,...)` → `assertEquals(7,...)`（MedicalRecordConverterTest x2 + DatabaseTemplateConfigManagerTest x1）

#### 9c — T17: 异常语义拆分
- `MedicalRecordServiceImpl.java:156-165` 三种异常分别返回不同错误码：
  - `TimeoutException` → `MR_GEN_AI_TIMEOUT`
  - `InterruptedException` → 恢复中断标记 + `MR_GEN_AI_INTERRUPTED`
  - `ExecutionException` → 提取 `e.getCause()` 消息 + `MR_GEN_AI_EXECUTION_ERROR`
- `MedicalRecordErrorCode.java` 新增 `MR_GEN_AI_INTERRUPTED` / `MR_GEN_AI_EXECUTION_ERROR`
- **测试**：`MedicalRecordErrorCodeTest.shouldHaveEightConstants` → `assertEquals(10,...)`

#### 9d — T18: 字面字符串 → 枚举
- `MedicalRecordConverter.java:69,73` `"MR_GEN_AI_TIMEOUT"` → `MedicalRecordErrorCode.MR_GEN_AI_TIMEOUT.name()`
- **测试**：无变更（`.name()` 返回相同字符串）

#### 9f — T22: 序列化异常日志
- `MedicalRecordContentConverter.java` catch 块追加 `log.warn()`，新增 Logger 字段
- **测试**：新增日志断言测试

#### 9g — T47: @PrePersist 补 updatedAt
- `MedicalRecord.java:133-136` 追加 `this.updatedAt = LocalDateTime.now()`
- **测试**：新增 `prePersistShouldSetUpdatedAt`

#### 9h — T48: 线程池绑定
- `MedicalRecordServiceImpl` 构造函数新增 `@Qualifier("medicalRecordExecutor") ExecutorService` 参数
- 新增 `MedicalRecordThreadPoolConfig` 配置类 `@Bean("medicalRecordExecutor")`
- `resolveVisitId` 中 `CompletableFuture.supplyAsync` 绑定 `medicalRecordExecutor`
- **测试**：构造函数参数计数 6→7，新增 ExecutorService mock

#### 9i — T50: DraftContextStore 原子操作
- `DraftContextStore` 接口新增 `createIfNotExists(String, Object)` 和 `compute(String, BiFunction)`
- `DraftContextStoreImpl` 实现（基于 ConcurrentHashMap.putIfAbsent / compute）
- **测试**：`DraftContextCleanupTaskTest.StubDraftContextStore` 补充两个方法实现

#### 9j — A06 + 9m — T20: DegradationContext 传递
- `FallbackAiService.applyStrategies()` 签名新增 `DegradationContext` 参数
- 12 个 `.thenApply(this::applyStrategies)` → `.thenApply(result -> applyStrategies(result, context))`
- 每个方法已创建的 context（含 serviceName/operationName）直接传入 lambda
- **线程安全**：每请求独享 context 实例，lambda 捕获绑定引用
- **测试**：适配 applyStrategies 新签名（若存在直接调用）

#### 9k — A09: aiData == null 降级
- `PrescriptionAuditServiceImpl.java:119` 追加 `&& aiResult.getData() != null` 检查
- null data 时走 AI_UNAVAILABLE 降级路径
- **测试**：验证 null data → 降级路径（已有/新增）

#### 9l — T19: TIMEOUT failedFuture
- `MockAiService.java:67` `new CompletableFuture<>()` → `CompletableFuture.failedFuture(new TimeoutException("Mock timeout"))`
- **测试**：`MockAiServiceTest.timeoutStrategyShouldTimeout` — `assertFalse`→`assertTrue`, `assertThrows(TimeoutException,...)`→`assertThrows(ExecutionException,...)`

#### 9n — T5: TriageConverter 副作用移除 + 同步统一
- 移除 `TriageServiceImpl.java:148-150` 重复 `setCorrectedChiefComplaint`
- `DialogueSession.java`：aiFailCount/roundCount 从 `AtomicInteger` 改为 `int` + synchronized
- **测试**：行为等价，无需修改

#### 9o — T32: writeTimestamps 原子迭代
- `DraftContextCleanupTask.java:38-45` 迭代基准从 `draftContextStore.keySet()` 改为 `writeTimestamps.entrySet()`
- **测试**：`shouldNotRemoveEntryWithoutTimestamp` 行为不变（writeTimestamps 无 key 时迭代空集，不删除任何条目）

#### 9p — A08/T24: 小批量
- A08: `TriageServiceImpl.java:171,177` 中文文案保留；`MedicalRecordServiceImpl.java:151,154,157` 英文→中文
- T24: `ConcurrentHashMapStore.java:10` 添加 `@Service` 注解
- **测试**：`ConcurrentHashMapStoreTest` 新增 `shouldBeAnnotatedWithService`

#### 9q — A11: 冗余防御检查
- `PrescriptionAuditServiceImpl.java:410` 移除 `&& aiResult.getData() != null`（AiResult.success() 已 `Objects.requireNonNull(data)`）
- **测试**：无变更（行为等价）

### 已有代码上下文

- `DatabaseTemplateConfigManager.java:100-115`：@EventListener 带 @Async，templateCache 为 Caffeine LoadingCache
- `MedicalRecordConverter.java:35-50`：content_map 构建遍历 MedicalRecordField.values()
- `MedicalRecordServiceImpl.java:128-160`：resolveVisitId → callAiWithTimeout 链
- `MedicalRecordContentConverter.java:25-50`：serialize/deserialize，catch Exception 返回 null/emptyMap
- `MedicalRecord.java:130-145`：@PrePersist/@PreUpdate 生命周期回调
- `DraftContextStoreImpl.java:8-30`：@Service，内部 ConcurrentHashMap
- `DraftContextStore.java`：extends SessionStore（get/put/remove/containsKey/keySet）
- `FallbackAiService.java:60-85,285-305`：selectDelegate 遍历 + applyStrategies（12 处 thenApply）
- `MockAiService.java:60-75`：switch TIMEOUT → `new CompletableFuture<>()`
- `TriageConverter.java:105-112`：toTriageResponse 中 setCorrectedChiefComplaint
- `DialogueSession.java`：synchronized + AtomicInteger + CopyOnWriteArrayList 混用
- `PrescriptionAuditServiceImpl.java:105-115`：aiResult null/isSuccess 检查
- `ConcurrentHashMapStore.java:8-15`：implements SessionStore + SuggestionStore，无 @Service
- `DraftContextCleanupTask.java:35-48`：cleanupExpiredDrafts 遍历 keySet → writeTimestamps.get(key)

### 测试文件汇总

| 测试文件 | 子项 | 变更说明 |
|---------|:----:|---------|
| `DatabaseTemplateConfigManagerTest.java` | 9a, 9e | assertEquals 9→7；新增 key 粒度测试 |
| `MedicalRecordConverterTest.java` | 9b, 9e | assertEquals 9→7 |
| `MedicalRecordServiceImplTest.java` | 9c, 9h | 构造函数 6→7 参数；新增异常测试；ExecutorService mock |
| `MedicalRecordErrorCodeTest.java` | 9c | assertEquals 8→10；新增枚举断言 |
| `MedicalRecordContentConverterTest.java` | 9f | 新增日志断言测试 |
| `MedicalRecordTest.java`（若无则新建） | 9g | 新增 prePersistShouldSetUpdatedAt |
| `DraftContextCleanupTaskTest.java` | 9i, 9o | StubDraftContextStore 补充 compute/createIfNotExists |
| `FallbackAiServiceTest.java` | 9j | 适配 applyStrategies 新签名 |
| `MockAiServiceTest.java` | 9l | assertFalse→assertTrue, TimeoutException→ExecutionException |
| `ConcurrentHashMapStoreTest.java` | 9p | 新增 @Service 注解断言 |
| `DialogueSessionTest.java` | 9n | 无变更（行为等价） |
| `PrescriptionAuditServiceImplTest.java` | 9k, 9q | 新增 null data 降级验证 |

## 修订说明（v12 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] plan.md R12 节缺少实施细节 | 补充完整 R12 实施细节：17 项子项清单、文件汇总、关键变更明细（每项含问题定位+变更+测试）、已有代码上下文、测试文件汇总表 |
| [严重] 所有 16 项子任务均未列出测试修改方案 | 新增「测试修改方案」节，为 9a-9q 逐项补充测试文件变更说明，包括 assertEquals 9→7、构造函数参数计数 6→7、StubDraftContextStore 补充方法、MockAiServiceTest 适配 failedFuture 等 |
| [严重] 9l（T19）修复方案与 MockAiServiceTest 现有测试直接冲突 | 明确采用 `CompletableFuture.failedFuture(new TimeoutException(...))` 方案，标注 `timeoutStrategyShouldTimeout` 测试适配：`assertFalse→assertTrue`, `assertThrows(TimeoutException)→assertThrows(ExecutionException)` |
| [一般] A11（P2）未包含在子项清单中 | 补充为 9q：PrescriptionAuditServiceImpl.persistAuditRecord 中移除 `&& aiResult.getData() != null` 冗余防御检查（AiResult.success() 已 Objects.requireNonNull(data)）。子项总数从 16→17。工作量 0.1h。路线表更新为 5 人时 |
| [一般] 9j+9m: applyStrategies 无法访问 DegradationContext | 明确方案：`applyStrategies` 签名新增 `DegradationContext` 参数，12 个 `.thenApply(this::applyStrategies)` 改为 `.thenApply(result -> applyStrategies(result, context))`，每方法已创建的 context 直接传入 lambda，线程安全。9m 由 9j 一并覆盖 |
| [轻微] 9b 依赖 9e 未标注执行顺序 | 子项清单脚注 + 关键变更明细节补充执行次序：9e → 9b（9b 依赖 9e），9a 与 9e 修改同一文件不同区域需注意合并 |
| [轻微] 9a 和 9e 修改同一文件 | 已标注「9a 与 9e 修改同一文件不同区域，需注意合并」|

---

## R12 FAILED 跨模块 + medical-record P1+P2 批量修复（v12 验证 7 项失败）

结果：1625 通过 / 7 失败 / 6 跳过，medical-record 模块 7 个测试错误。

失败详情：

| # | 测试类 | 方法 | 行号 | 根因 |
|---|--------|------|:----:|------|
| F1 | MissingFieldDetectorImplTest | shouldReturnHintForEmptyStringField | 60 | setUp() 创建包含全部 9 个 MedicalRecordField 的 template，但 toFieldsMap 9e 后仅返回 7 个业务字段，MISSING_FIELDS/PARTIAL_CONTENT 始终被检测为缺失 |
| F2 | MissingFieldDetectorImplTest | shouldReturnHintForNullField | 51 | 同 F1 |
| F3 | MissingFieldDetectorImplTest | shouldResolvePlaceholderInPromptMessage | 97 | 同 F1，PARTIAL_CONTENT 被当作字段名解析 |
| F4 | MissingFieldDetectorImplTest | shouldReturnHintForBlankStringField | 69 | 同 F1 |
| F5 | MissingFieldDetectorImplTest | shouldReturnEmptyHintsWhenAllFieldsAreFilled | 43 | 同 F1，9 field template 中 MISSING_FIELDS/PARTIAL_CONTENT 不在 fieldsMap 中 → detect 返回 2 hint |
| F6 | MedicalRecordServiceImplTest | shouldReturnInterruptedOnInterruptedException | 146 | aiService.resultFuture 使用 `completedFuture(...)` 已预完成，callAiWithTimeout 中 `.get()` 不等待也不检查中断标记 → 返回成功结果而非 degraded |
| F7 | MedicalRecordServiceImplTest | shouldReturnDegradedWhenAiTimesOut | 129 | supplyAsync 抛出 RuntimeException → ExecutionException 路径返回 MR_GEN_AI_EXECUTION_ERROR，但 toRecordGenerateResponse 只识别 MR_GEN_AI_TIMEOUT → success=false |

修正方向：
- F1-F5: MissingFieldDetectorImplTest.setUp() 中 template 排除 MISSING_FIELDS/PARTIAL_CONTENT；同步更新 3 处 assertEquals 断言（9→7, 5→3）
- F6: aiService.resultFuture 从 `completedFuture(...)` 改为 `new CompletableFuture<>()`，使 callAiWithTimeout 中 `.get()` 等待并抛出 InterruptedException
- F7: toRecordGenerateResponse 中 response.setErrorCode 改用 `MedicalRecordErrorCode.valueOf()` 动态解析全部 AI 错误码；测试断言改为 assertFalse(isSuccess) + assertTrue(isDegraded) + assertEquals(MR_GEN_AI_EXECUTION_ERROR)

---

## R13 PASSED 修复 v12 验证失败的 7 项测试

结果：修复 MissingFieldDetectorImplTest 模板字段集（F1-F5）+ MedicalRecordServiceImplTest 中断/超时测试（F6/F7）+ MedicalRecordConverter 动态错误码解析。1635 用例通过，0 失败，6 跳过（v13 verify PASSED）。

涉及文件：
- `MissingFieldDetectorImplTest.java`（setUp 过滤 + 3 处断言更新）
- `MedicalRecordServiceImplTest.java`（SameThreadExecutor + incompletedFuture + 断言修正）
- `MedicalRecordConverter.java`（toRecordGenerateResponse 动态 errorCode 解析）
