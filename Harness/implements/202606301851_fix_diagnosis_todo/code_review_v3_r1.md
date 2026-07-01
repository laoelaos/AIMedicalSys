# 代码审查报告（v3 r1）

## 审查结果
REJECTED

## 发现

- **[严重]** `prescription/src/test/.../SuggestionCleanupTaskTest.java:104` — `StubEntry implements SuggestionCleanupTask.SuggestionStoreEntry` 引用了已删除的内部接口 `SuggestionCleanupTask.SuggestionStoreEntry`，且方法名为 `getStatus()` 而非新的 `getStatusName()`。此文件无法编译。

- **[严重]** `prescription/src/test/.../SuggestionCleanupTaskTest.java:125` — `StubSuggestionStore` 未实现 `SuggestionStore` 接口新增的 `createIfNotExists(String, Object)` 方法。此文件无法编译。

- **[严重]** `common-module-api/src/test/.../ConcurrentHashMapStoreTest.java:140` — `shouldImplementDraftContextStore()` 断言 `assertInstanceOf(DraftContextStore.class, store)`，但 `ConcurrentHashMapStore` 已移除 `implements DraftContextStore`。此测试将失败。

- **[严重]** `prescription/src/test/.../DedupTaskSchedulerTest.java` — 全部 4 个测试仅 mock `suggestionStore.compute()`，未 mock `get()` 和 `createIfNotExists()`。新 `schedule()` 实现先调用 `get()`（快速路径），再调用 `createIfNotExists()`（首次创建），`compute()` 仅在兜底路径执行。当前 mock 方式无法正确驱动新逻辑：
  - `shouldCreateNewTaskWhenNoExisting`：`get()` 未 mock 返回 null，Mockito 默认返回 null → 快速路径不命中 → `createIfNotExists()` 未 mock 返回 null → 走 Step 3 成功路径（`oldValue==null`），`compute()` 不会被调用。测试虽可能意外通过但验证逻辑完全偏离设计意图。
  - `shouldReusePendingTask`/`shouldReuseCompletedNotConsumedTask`/`shouldCreateNewTaskWhenFailed`：`get()` 未 mock，Mockito 默认返回 null → 快速路径永远不命中，所有测试走向 `createIfNotExists` 路径而非预期的 `compute` 路径。

## 修改要求

1. **SuggestionCleanupTaskTest.java:104** — `StubEntry` 应改为实现独立接口 `com.aimedical.modules.commonmodule.store.SuggestionStoreEntry`，并将 `getStatus()` 重命名为 `getStatusName()`。

2. **SuggestionCleanupTaskTest.java:125** — `StubSuggestionStore` 必须新增 `createIfNotExists` 方法实现：
   ```java
   @Override
   public Object createIfNotExists(String key, Object value) {
       return map.putIfAbsent(key, value);
   }
   ```
   同时 `StubSuggestionStore` 内部的 `Map` 需从 `HashMap` 改为 `ConcurrentHashMap`（`putIfAbsent` 需要），或改用手动 `containsKey + put` 模拟语义。

3. **ConcurrentHashMapStoreTest.java:140-142** — 删除 `shouldImplementDraftContextStore()` 测试方法及相关的 `DraftContextStore` import（`ConcurrentHashMapStore` 不再实现 `DraftContextStore`）。新增 `DraftContextStoreImpl` 的独立测试类或在现有测试中补充 `DraftContextStoreImpl` 的验证。

4. **DedupTaskSchedulerTest.java** — 重写全部测试用例以匹配新的 `get + createIfNotExists + compute` 混合策略：
   - `shouldCreateNewTaskWhenNoExisting`：mock `get()` 返回 null，mock `createIfNotExists()` 返回 null。
   - `shouldReusePendingTask`：mock `get()` 返回 PENDING 结果，验证快速路径命中，`createIfNotExists` 和 `compute` 不被调用。
   - `shouldReuseCompletedNotConsumedTask`：mock `get()` 返回 COMPLETED+unconsumed 结果，验证快速路径命中。
   - `shouldCreateNewTaskWhenFailed`：mock `get()` 返回 FAILED 结果（快速路径不命中），mock `createIfNotExists()` 返回该 FAILED 结果（Step 4 不复用），mock `compute()` 执行替换逻辑。
