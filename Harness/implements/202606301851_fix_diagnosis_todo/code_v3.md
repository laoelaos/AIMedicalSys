# 实现报告（v3）

## 概述
实现了 SuggestionStore 接口新增 `createIfNotExists` 方法、SuggestionStoreEntry 独立接口、DraftContextStoreImpl 独立实现、AiSuggestionResult 实现 SuggestionStoreEntry、DedupTaskScheduler.schedule() 混合策略重构、SuggestionCleanupTask 适配。共涉及 7 个源码文件（2 新建、5 修改）+ 3 个测试文件修改。

## 文件变更清单
| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | common-module-api/.../store/SuggestionStore.java | 新增 `createIfNotExists(String, Object)` 方法签名 |
| 新建 | common-module-api/.../store/SuggestionStoreEntry.java | 独立接口，定义 `getStatusName()`、`isConsumed()`、`getTimestamp()` |
| 修改 | common-module-api/.../store/impl/ConcurrentHashMapStore.java | 新增 `createIfNotExists` 实现；移除 `DraftContextStore` 实现声明 |
| 新建 | common-module-api/.../store/impl/DraftContextStoreImpl.java | 独立 DraftContextStore 实现，`@Service` 注册，独立 ConcurrentHashMap 实例 |
| 修改 | prescription/.../dto/assist/AiSuggestionResult.java | 实现 `SuggestionStoreEntry`，新增 `getStatusName()`、`isConsumed()`、`getTimestamp()` 映射方法 |
| 修改 | prescription/.../service/assist/DedupTaskScheduler.java | 重构 `schedule()`：`get(快速路径) + createIfNotExists(首次创建) + compute(兜底替换)` 混合策略 |
| 修改 | prescription/.../task/SuggestionCleanupTask.java | 导入独立 `SuggestionStoreEntry`；`getStatus()` → `getStatusName()`；移除内部接口定义 |
| 修改 | prescription/.../task/SuggestionCleanupTaskTest.java | `StubEntry` 改为实现独立 `SuggestionStoreEntry` 接口；`getStatus()` → `getStatusName()`；`StubSuggestionStore` 改用 `ConcurrentHashMap` 并新增 `createIfNotExists` |
| 修改 | common-module-api/.../store/impl/ConcurrentHashMapStoreTest.java | 删除 `shouldImplementDraftContextStore()` 测试及 `DraftContextStore` import；新增 `createIfNotExists` 测试 |
| 修改 | prescription/.../service/assist/DedupTaskSchedulerTest.java | 重写全部测试用例以匹配 `get + createIfNotExists + compute` 混合策略 |

## 编译验证
通过（`mvn compile -q` 无错误输出）

## 测试验证
通过（`ConcurrentHashMapStoreTest`、`SuggestionCleanupTaskTest`、`DedupTaskSchedulerTest` 全部通过）

## 设计偏差说明
无偏差。所有类型定义、方法签名、行为契约和错误处理均与详细设计规格一致。

## 修订说明（v3 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] `SuggestionCleanupTaskTest.java:104` — `StubEntry` 引用已删除的内部接口 `SuggestionCleanupTask.SuggestionStoreEntry`，方法名为 `getStatus()` 而非 `getStatusName()` | 采纳。`StubEntry` 改为实现独立接口 `com.aimedical.modules.commonmodule.store.SuggestionStoreEntry`，`getStatus()` → `getStatusName()` |
| [严重] `SuggestionCleanupTaskTest.java:125` — `StubSuggestionStore` 未实现 `createIfNotExists`，内部 `Map` 为 `HashMap` 不支持 `putIfAbsent` | 采纳。`StubSuggestionStore` 内部 `Map` 改为 `ConcurrentHashMap`，新增 `createIfNotExists` 方法实现 `map.putIfAbsent(key, value)` |
| [严重] `ConcurrentHashMapStoreTest.java:140` — `shouldImplementDraftContextStore()` 断言 `ConcurrentHashMapStore` 实现 `DraftContextStore`，但已移除 | 采纳。删除 `shouldImplementDraftContextStore()` 测试方法及 `DraftContextStore` import；新增 `shouldCreateIfNotExistsWhenKeyAbsent()` 和 `shouldReturnOldValueWhenCreateIfNotExistsOnExistingKey()` 测试 |
| [严重] `DedupTaskSchedulerTest.java` — 全部 4 个测试仅 mock `compute()`，未 mock `get()` 和 `createIfNotExists()`，无法正确驱动新逻辑 | 采纳。重写全部测试用例：`shouldCreateNewTaskWhenNoExisting` mock `get()`→null + `createIfNotExists()`→null；`shouldReusePendingTask` mock `get()`→PENDING 结果验证快速路径；`shouldReuseCompletedNotConsumedTask` mock `get()`→COMPLETED+unconsumed 结果验证快速路径；`shouldCreateNewTaskWhenFailed` mock `get()`→FAILED + `createIfNotExists()`→FAILED + `compute()` 执行替换逻辑 |
