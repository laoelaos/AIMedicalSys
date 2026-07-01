# 测试报告（v5）

## 测试文件

`common-module-api/src/test/java/com/aimedical/modules/commonmodule/store/impl/ConcurrentHashMapStoreTest.java`

## 测试框架

JUnit 5（spring-boot-starter-test 内置）

## 测试覆盖总览

| 维度 | 用例数 | 说明 |
|------|--------|------|
| 正常路径（SessionStore） | 10 | get/put/remove/containsKey/keySet 正常行为 |
| 正常路径（SuggestionStore compute） | 5 | compute 原子替换、创建、删除、空 key、参数正确性 |
| 类型契约验证 | 3 | 实现三个接口 |
| 错误路径（null 入参） | 7 | 所有公开方法 null 入参 → NPE |
| 并发安全 | 1 | 多线程并发 put/get，验证 keySet 完整性 |
| **合计** | **26** | |

## 行为契约覆盖详情

### SessionStore 接口

| 契约 | 正向用例 | 覆盖 |
|------|---------|------|
| get(key) → value / null | shouldReturnNullWhenKeyNotFound, shouldReturnValueAfterPut, shouldOverwriteExistingValueOnPut | ✓ |
| put(key, value) 存储/覆盖 | shouldReturnValueAfterPut, shouldOverwriteExistingValueOnPut | ✓ |
| remove(key) → value / null | shouldReturnRemovedValue, shouldReturnNullWhenRemovingNonExistentKey, shouldRemoveEntry | ✓ |
| containsKey(key) → boolean | shouldContainKeyAfterPut, shouldNotContainKeyAfterRemove | ✓ |
| keySet() → Set 视图 | shouldReturnEmptyKeySetInitially, shouldReturnAllKeysInKeySet, shouldReflectRemoveInKeySet | ✓ |

### SuggestionStore 接口

| 契约 | 正向用例 | 覆盖 |
|------|---------|------|
| compute(key, remapping) 原子替换 | shouldComputeNewValue | ✓ |
| compute 创建新条目 | shouldComputeCreatesEntryWhenKeyAbsent | ✓ |
| compute 返回 null 删除 | shouldDeleteEntryWhenComputeReturnsNull | ✓ |
| compute 在缺失 key 上返回 null 不创建 | shouldRemainAbsentWhenComputeReturnsNullOnMissingKey | ✓ |
| 参数传递正确性 | shouldPassCorrectArgumentsToRemappingFunction | ✓ |

### 错误路径

| 契约 | 用例 | 覆盖 |
|------|------|------|
| null key → NPE | shouldThrowNpeWhenGet/Put/Remove/ContainsKey/ComputeWithNullKey | ✓ |
| null value → NPE | shouldThrowNpeWhenPutWithNullValue | ✓ |
| null remappingFunction → NPE | shouldThrowNpeWhenComputeWithNullFunction | ✓ |

### 并发保证

| 契约 | 用例 | 覆盖 |
|------|------|------|
| 多线程安全（happens-before） | shouldHandleConcurrentPutsAndGets | ✓ |

## 与详细设计的一致性

- 所有测试基于行为契约（第137-165节），未引用实现细节
- 错误处理覆盖全部 4 条约定（第169-172节）
- 类型关系验证覆盖三个接口实现（第132-133节）
- 并发测试验证线程安全保证（第139、145、150、155、165节）

## 未覆盖说明

- `keySet()` 迭代器弱一致性：本测试通过结构突变后读取验证实时视图，迭代器弱一致语义在单元测试层面难以精确量测，留待集成测试覆盖。
- NullPointerException 的 exact message 内容未断言（依赖 JDK 实现，跨平台可能不同）。
