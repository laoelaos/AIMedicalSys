# 测试报告（v3）

## 概述
基于详细设计 v3 行为契约，为 7 个变更点编写/补充单元测试。新增测试文件 1 个，补充测试 3 个已有文件，共新增 20 个测试用例。全部通过。

## 测试文件清单

### 新建
| 文件路径 | 用例数 | 说明 |
|---------|--------|------|
| `common-module-api/.../store/impl/DraftContextStoreImplTest.java` | 16 | DraftContextStoreImpl 全量行为契约测试 |

### 修改
| 文件路径 | 新增用例数 | 说明 |
|---------|-----------|------|
| `common-module-api/.../store/impl/ConcurrentHashMapStoreTest.java` | 4 | createIfNotExists NPE + 并发 + 不实现 DraftContextStore |
| `prescription/.../dto/assist/AiSuggestionResultTest.java` | 6 | SuggestionStoreEntry 契约测试 |
| `prescription/.../service/assist/DedupTaskSchedulerTest.java` | 8 | COMPLETED+consumed、非 AiSuggestionResult、compute 兜底路径、Step 4 复用路径 |

### 未修改（已有测试覆盖充分）
| 文件路径 | 说明 |
|---------|------|
| `prescription/.../task/SuggestionCleanupTaskTest.java` | 已有 8 个用例覆盖全部清理逻辑，StubEntry 已适配独立 SuggestionStoreEntry 接口 |

## 用例覆盖矩阵

### ConcurrentHashMapStore.createIfNotExists 契约
| 行为契约 | 用例 | 状态 |
|---------|------|------|
| key 不存在时写入并返回 null | `shouldCreateIfNotExistsWhenKeyAbsent` | 已有 |
| key 已存在时返回旧值不修改 | `shouldReturnOldValueWhenCreateIfNotExistsOnExistingKey` | 已有 |
| null key 抛 NPE | `shouldThrowNpeWhenCreateIfNotExistsWithNullKey` | **新增** |
| null value 抛 NPE | `shouldThrowNpeWhenCreateIfNotExistsWithNullValue` | **新增** |
| 并发 createIfNotExists 仅一个线程胜出 | `shouldHandleConcurrentCreateIfNotExists` | **新增** |

### ConcurrentHashMapStore 不实现 DraftContextStore
| 行为契约 | 用例 | 状态 |
|---------|------|------|
| 不实现 DraftContextStore | `shouldNotImplementDraftContextStore` | **新增** |

### DraftContextStoreImpl 契约
| 行为契约 | 用例 | 状态 |
|---------|------|------|
| 实现 DraftContextStore | `shouldImplementDraftContextStore` | **新增** |
| 实现 SessionStore | `shouldImplementSessionStore` | **新增** |
| get/put/remove/containsKey/keySet 正常路径 | 5 个用例 | **新增** |
| null key/value 抛 NPE | 5 个用例 | **新增** |
| 并发安全 | `shouldHandleConcurrentPutsAndGets` | **新增** |
| 与 ConcurrentHashMapStore 键空间隔离 | `shouldNotShareKeyspaceWithConcurrentHashMapStore` | **新增** |

### SuggestionStoreEntry 接口契约（AiSuggestionResult 实现）
| 行为契约 | 用例 | 状态 |
|---------|------|------|
| 实现 SuggestionStoreEntry | `shouldImplementSuggestionStoreEntry` | **新增** |
| getStatusName() 返回枚举名称 | `shouldReturnStatusNameFromStatusEnum` | **新增** |
| getStatusName() status 为 null 时返回 null | `shouldReturnNullStatusNameWhenStatusIsNull` | **新增** |
| isConsumed() 返回 consumed 字段 | `shouldReturnConsumedFromField` | **新增** |
| getTimestamp() 从 createTime 转换 | `shouldReturnTimestampFromCreateTime` | **新增** |
| getTimestamp() createTime 为 null 时返回 null | `shouldReturnNullTimestampWhenCreateTimeIsNull` | **新增** |

### DedupTaskScheduler.schedule() 状态判定矩阵
| dedupKey 当前值 | 用例 | 状态 |
|---------|------|------|
| 不存在 → candidateTaskId | `shouldCreateNewTaskWhenNoExisting` | 已有 |
| PENDING → 现有 taskId（快速路径） | `shouldReusePendingTask` | 已有 |
| COMPLETED+unconsumed → 现有 taskId（快速路径） | `shouldReuseCompletedNotConsumedTask` | 已有 |
| COMPLETED+unconsumed → 现有 taskId（快速路径，独立验证） | `shouldReuseTaskWhenFastPathReturnsCompletedUnconsumed` | **新增** |
| FAILED → candidateTaskId（compute 替换） | `shouldCreateNewTaskWhenFailed` | 已有 |
| COMPLETED+consumed → candidateTaskId（compute 替换） | `shouldCreateNewTaskWhenCompletedAndConsumed` | **新增** |
| 非 AiSuggestionResult → candidateTaskId（compute 替换） | `shouldCreateNewTaskWhenNonAiSuggestionResultInStore` | **新增** |
| createIfNotExists 返回 PENDING → 现有 taskId（Step 4） | `shouldReuseTaskViaCreateIfNotExistsWhenOldValueIsPending` | **新增** |
| createIfNotExists 返回 COMPLETED+unconsumed → 现有 taskId（Step 4） | `shouldReuseTaskViaCreateIfNotExistsWhenOldValueIsCompletedUnconsumed` | **新增** |
| compute 返回其他线程的可复用值 → winner taskId | `shouldReuseExistingTaskWhenComputeFindsReusableValue` | **新增** |
| compute 返回非 AiSuggestionResult → IllegalStateException | `shouldThrowIllegalStateExceptionWhenComputeReturnsNonAiSuggestionResult` | **新增** |
| 首次创建时 put candidateTaskId 为 key | `shouldPutCandidateTaskIdKeyWhenCreateNewTask` | **新增** |

## 执行结果

| 模块 | 测试类 | 用例数 | 结果 |
|------|--------|--------|------|
| common-module-api | ConcurrentHashMapStoreTest | 22 | PASS |
| common-module-api | DraftContextStoreImplTest | 16 | PASS |
| prescription | AiSuggestionResultTest | 7 | PASS |
| prescription | DedupTaskSchedulerTest | 12 | PASS |
| prescription | SuggestionCleanupTaskTest | 8 | PASS |

**总计：65 个用例，全部通过。**

## 设计偏差说明

无偏差。所有测试基于详细设计 v3 行为契约编写，覆盖正常路径、边界条件、错误路径和状态交互四个维度。
