# 设计审查报告（v3 r2）

## 审查结果
REJECTED

## 发现

### **[严重]** `DedupTaskScheduler.schedule()` 当 dedupKey 已存在 COMPLETED+consumed 或 FAILED 结果时行为错误

**问题**：v3 设计的 `get + createIfNotExists` 两步策略无法正确处理 dedupKey 已存在但不可复用的场景。当 dedupKey 已关联一个 COMPLETED+consumed 或 FAILED 的 `AiSuggestionResult` 时：
- Step 1 的快速路径不会触发（条件不满足）
- Step 3 的 `createIfNotExists(dedupKey, newResult)` 发现 key 已存在，返回旧值
- 代码进入 `oldValue instanceof AiSuggestionResult winner` 分支，返回 `winner.getTaskId()`（即旧 taskId）

**为什么是问题**：
- 原 `compute` 算法在此情况下会创建新任务、原子替换 dedupKey、返回新 taskId
- 现有测试 `DedupTaskSchedulerTest.shouldCreateNewTaskWhenFailed()`（line 83-99）明确验证 FAILED 时应返回新 taskId（`assertNotEquals("failed-task", taskId)`），v3 设计无法通过此测试
- 调用方接收到已过期/已完成任务的 taskId，但实际上没有 PENDING 任务在执行，导致业务逻辑静默失效

**修正方向**：
1. 在 `createIfNotExists` 返回非 null 旧值后，增加对旧值可复用性的判断（仅 PENDING 或 COMPLETED+unconsumed 可复用）
2. 若不可复用，需原子替换 dedupKey。可选的实现策略：
   - 保留 `compute` 方法用于原子替换，将 `createIfNotExists` 仅作为快速路径
   - 或在 `createIfNotExists` 返回不可复用的旧值时，显式调用 `compute` 或 `remove + createIfNotExists` 进行替换（需处理 TOCTOU 竞态）
3. 建议方案：使用 `createIfNotExists` 作为首次创建的快速路径，对已存在的 key 使用 `compute` 进行原子检查+替换，且保持 cross-key `put` 在 lambda 之外

### **[轻微]** `DraftContextStoreImpl` Spring Bean 注册未明确

**问题**：设计未说明 `DraftContextStoreImpl` 如何注册为 Spring 管理的 bean。当前 `ConcurrentHashMapStore` 无任何 Spring 注解（`@Component`/`@Service`），说明 bean 由外部 `@Configuration` 或自动配置提供。拆分后 `DraftContextStoreImpl` 需同等注册，否则 `DraftContextCleanupTask` 无法注入新实现。

**建议**：在设计中补充 `DraftContextStoreImpl` 的 bean 注册方式（例如添加 `@Service` 注解，或在设计文档中标注"由 XX 配置类补充 `@Bean` 定义"）。

## 修改要求

以上 **[严重]** 问题必须修正。仅修正 **[轻微]** 问题不足以通过审查。
