# 计划审查报告（v12 r1）

## 审查结果
REJECTED

## 发现

### [严重] plan.md R12 节缺少实施细节
plan.md 中 R12 仅含标题 `## R12 NEW 跨模块 + medical-record P1+P2 批量修复`，没有任何实施细节描述。而此前所有轮次（R1-R11）在 plan.md 中均有完整的问题定位、变更明细、文件清单、测试修改说明。task_v12.md 虽有详细内容，但 plan.md 作为项目的纲领性实现计划文件应当同步更新，保持与 task 的一致性，否则后续轮次无法从 plan.md 中获取 R12 的完整记录。

### [严重] 所有 16 项子任务均未列出测试修改方案
task_v12.md 中 16 项子任务（9a-9p）没有任何一项包含测试文件修改说明。而此前所有轮次均明确列出测试修改方案（如 R5 的 PrescriptionAssistServiceImplTest 参数计数断言更新 + execute mock 策略、R6 的 PrescriptionDraftContextTest 构造函数变更 + verify 断言）。缺少测试修改将导致以下可预见的失败：

- **9i（T50）**：DraftContextStore 接口新增 `createIfNotExists` 和 `compute` 方法 → `DraftContextCleanupTaskTest.java:143` 中的 `StubDraftContextStore implements DraftContextStore` 缺少这两个方法的实现，**编译失败**
- **9h（T48）**：MedicalRecordServiceImpl 构造函数新增 `ExecutorService medicalRecordExecutor` 参数 → 对应测试类构造函数参数计数断言需更新（类似 R5 的 `assertEquals(10, ...) → assertEquals(11, ...)` 问题）
- **9c（T17）**：新增 `MR_GEN_AI_INTERRUPTED` 和 `MR_GEN_AI_EXECUTION_ERROR` 到 MedicalRecordErrorCode 枚举 → 测试中断言特定错误码字符串的测试需确认是否受影响
- **9d（T18）**：字面字符串 → MedicalRecordErrorCode 枚举 `.name()` → 测试中若硬编码 `"MR_GEN_AI_TIMEOUT"` 字符串的比较将正常工作（name() 返回值相同），但仍需确认
- **9n（T5）**：DialogueSession 同步策略统一 → 依赖当前同步行为的测试需适配
- **9p（T24）**：ConcurrentHashMapStore 添加 `@Service` → 可能影响 Spring 组件扫描行为，需验证集成测试
- **9a/9e**：DatabaseTemplateConfigManager 修改 → 缓存行为变更需对应测试验证

### [严重] 9l（T19）修复方案与 MockAiServiceTest 现有测试直接冲突
task_v12.md 建议使用 `CompletableFuture.failedFuture(new TimeoutException(...))` 替代 `new CompletableFuture<>()`。但 `MockAiServiceTest.java:205-210` 的 `timeoutStrategyShouldTimeout()` 测试明确预期：
```java
assertFalse(future.isDone());  // failedFuture 立即完成 → isDone=true，断言失败
assertThrows(TimeoutException.class, () -> future.get(1, TimeUnit.MILLISECONDS));
// failedFuture 的 get(timeout, unit) 抛出 ExecutionException 而非 TimeoutException → 断言失败
```
此测试在 9l 实施后必然失败，但计划未提及任何测试修改。

### [一般] A11（P2）未包含在子项清单中
R10 轮次的修订记录（plan.md 第617-618行）已确认 A11 遗漏并声称"已在路线表第 9 项工作量重估中补充"，但 task_v12.md 的 16 项子任务（9a-9p）中不包含 A11。需确认 A11 是被有意排除（因其实际影响轻微）还是被遗漏。

### [一般] 9j+9m: applyStrategies 无法访问 DegradationContext —— 实现路径未明确
`FallbackAiService.applyStrategies()` 当前签名是 `private <T> AiResult<T> applyStrategies(AiResult<T> result)`，该方法不接收任何 serviceName/operationName 信息。task_v12.md 要求传入 `new DegradationContext(serviceName, operationName)` 但**未说明如何将 serviceName/operationName 从各 service 方法传递到 applyStrategies**。可能的方案（存储为字段/lambda 捕获/方法重载）各有不同的线程安全或签名兼容性影响，需明确指定。

同时，9j 和 9m 对 `FallbackAiService.java` 同一文件的重叠修改需要确保变更合并正确。

### [轻微] 9b 依赖 9e 但未标注执行顺序
9b 明确标注"依赖 9e（T21）先修复——从 content_map 写入逻辑中排除 MISSING_FIELDS/PARTIAL_CONTENT 两个 key"。在同一轮次内需要按 9e→9b 的顺序实施，但计划未明确说明执行次序，可能导致实施时合并冲突。

### [轻微] 9a 和 9e 修改同一文件（DatabaseTemplateConfigManager.java）
两处修改作用于同一文件的不同区域（9a: invalidateAll → invalidate(key)；9e: DEFAULT_TEMPLATE 排除元数据字段），虽可安全合并，但应在实施时注意。

## 修改要求（REJECTED）

1. **plan.md 补充 R12 完整实施细节**：按此前轮次（R5 NEW/R6 NEW/R10 NEW）格式，补充问题定位、变更明细、文件清单、测试修改说明。
2. **为所有 16 项子任务补充测试修改方案**：至少包含：
   - 9i: `DraftContextCleanupTaskTest.StubDraftContextStore` 补充 compute/createIfNotExists 实现，或改为 Mockito mock
   - 9h: MedicalRecordServiceImplTest 构造函数参数计数断言更新、ExecutorService mock 注入
   - 9l: MockAiServiceTest.timeoutStrategyShouldTimeout() 适配 failedFuture 语义
   - 9c: MedicalRecordErrorCodeTest（若存在）新增枚举值断言
   - 9n: DialogueSessionTest（若存在）适配统一同步策略
   - 9p: ConcurrentHashMapStoreTest 补充 @Service 注解断言
3. **9l 明确 TIMEOUT 实现方案**：若采用 failedFuture，必须说明对应测试修改（`assertTrue(future.isDone())` + `assertThrows(ExecutionException.class, ...)`）；若不采用，说明替代方案及理由。
4. **明确 A11 是否纳入 R12**：若排除，说明理由；若纳入，补充子项。
5. **9j+9m 补充 applyStrategies 的 DegradationContext 传递方案**：明确使用字段注入/lambda 捕获/方法重载的具体路径，并说明线程安全性。
6. **标注 9a→9e、9e→9b 的执行次序**。
