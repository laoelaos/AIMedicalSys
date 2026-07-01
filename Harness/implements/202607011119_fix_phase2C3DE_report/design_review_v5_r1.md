# 设计审查报告（v5 r1）

## 审查结果
APPROVED

## 发现

### 已验证的设计要点

1. **P01 — PrescriptionThreadPoolConfig** ✅ — 新建配置类 `com.aimedical.modules.prescription.config.PrescriptionThreadPoolConfig`，使用 JDK 21 虚拟线程 `Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("ai-task-").factory())`，`@Bean("aiTaskExecutor")` 名称正确，prescription 模块已有 spring-context 依赖无需新增。

2. **P01 — PrescriptionAssistServiceImpl 字段与注入** ✅ — 新增 `ExecutorService aiTaskExecutor` 字段，构造函数末尾添加 `@Qualifier("aiTaskExecutor") ExecutorService aiTaskExecutor` 参数（参数计数 10→11），新增导入 `ExecutorService` / `CompletionException` / `Qualifier`，均正确。

3. **P01 — supplyAsync 绑定 + exceptionally** ✅ — `scheduleSuggestionAsync()` 中 `CompletableFuture.supplyAsync(supplier, aiTaskExecutor).exceptionally(...)` 正确绑定虚拟线程池；`exceptionally` 以 `log.warn` 记录未捕获异常并返回 null，确保异常不静默丢失；lambda body 保持不变。

4. **S03 — DedupTaskScheduler.schedule() 重排** ✅ — `put(candidateTaskId, newResult)` 提前至 `createIfNotExists(dedupKey, ...)` 之前无条件执行，消除跨 key 竞态窗口；createIfNotExists/compute 分支中不再重复 put，返回值逻辑简化；孤立 candidateTaskId 条目的边界行为已明确记录。

5. **PrescriptionAssistServiceImplTest 覆盖** ✅ — 构造函数 10→11 参数、`@Mock ExecutorService aiTaskExecutor`、`execute(Runnable)` mock 策略（`doAnswer(r.run())`）、`constructorShouldAcceptNineParameters` 断言 `assertEquals(11, ...)`、10 个异步测试清单、`lenient()` 建议、`asyncSuggestionShouldStoreFailedWhenSerializationFails` 特殊处理、`assistShouldTriggerAsyncSchedulingWhenSyncAiSucceeds` 分析，均完整正确。

6. **DedupTaskSchedulerTest 覆盖** ✅ — 新增 `shouldGuaranteeCandidateTaskIdVisibilityAfterCreateIfNotExists` 竞态测试（`InOrder` 验证 put 先于 createIfNotExists）；5 个现有测试 `never().put` → `times(1).put` 修正清单完整。

7. **错误处理与行为契约** ✅ — P01 `exceptionally` 兜底 + S03 孤立条目 `get(key)` 返回 null 时调用方抛出 `BusinessException` 不导致 NPE，均已记录。

### v5 r1/r2/r3 审查修正验证
| 审查意见 | 修正状态 |
|---------|---------|
| [严重] 构造函数缺少 @Qualifier | ✅ 已修正 |
| [一般] S03 破坏 5 个现有测试 | ✅ 已修正 |
| [一般] mock 策略 submit → execute | ✅ 已修正 |
| [轻微] 配置类包路径未指定 | ✅ 已指定 |
| [一般] constructorShouldAcceptNineParameters 断言值 | ✅ 已修正 |

### 潜在考量（**[轻微]**）

- **`exceptionally` 中的 `CompletionException` 检查为死代码路径**：`CompletableFuture.supplyAsync(supplier, executor)` 的 supplier 抛出异常时，`completeExceptionally(x)` 保存的是原始异常，`exceptionally` 回调直接接收该原始异常而非 `CompletionException`。`instanceof CompletionException` 分支在运行时永不为真，可简化直接使用 `ex.getMessage()`。不影响功能正确性。
- **`constructorShouldAcceptNineParameters` 方法名与断言值不一致**：方法名含 "Nine" 而断言值为 `assertEquals(11, ...)`，可考虑重命名为 `constructorShouldAcceptElevenParameters`。不影响测试正确性。
