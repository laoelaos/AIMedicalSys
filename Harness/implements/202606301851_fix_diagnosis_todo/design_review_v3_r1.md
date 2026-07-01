# 设计审查报告（v3 r1）

## 审查结果
REJECTED

## 发现

### **[一般] S03: `createIfNotExists(dedupKey, result)` 在 `compute(dedupKey, ...)` 之后始终是空操作（dead code）**

**位置**：detail_v3.md §S03 伪代码第 141–144 行

**描述**：
```java
Object result = suggestionStore.compute(dedupKey, (key, oldValue) -> { ... return newResult/oldValue; });
Object old = suggestionStore.createIfNotExists(dedupKey, result);  // ← dead code
```

`ConcurrentHashMap.compute(key, fn)` 的语义是：原子性地将 key 关联到 fn 的返回值（除非 fn 返回 null 删除 key）。在 `compute(dedupKey, ...)` 返回后，`dedupKey` **必定已存在于 Store 中**（lambda 始终返回非 null）。因此紧接着的 `createIfNotExists(dedupKey, result)` 永远会命中已存在的 key，返回旧值而不做任何写入——该调用不可能执行"创建"操作。

设计文档 line 157 的注释说"createIfNotExists 对 dedupKey 无效（已有值）"，只意识到 `result == oldValue` 时无效，但未意识到**无论 result 是 oldValue 还是 newResult，compute 已保证 key 存在**，因此该调用在任何路径下都是空操作。

**影响**：
- 存在 dead code 且意图误导（注释说"保证 dedup key 原子写入"，但原子写入在 compute 中已完成）
- 不会导致运行时错误，但给后续维护者造成困惑

**修正方向**（二选一）：
1. **移除 compute，改用 get + createIfNotExists 实现真正的原子创建语义**：
   - 先 `suggestionStore.get(dedupKey)` 读取旧值
   - 若旧值可复用则直接返回，否则创建新值后用 `createIfNotExists(dedupKey, newResult)` 原子写入
   - 若 `createIfNotExists` 返回非 null（并发写入者先到达），则用返回的旧值存入 candidateTaskId
2. **保留 compute，删掉冗余的 createIfNotExists 调用**，直接用 `compute + put` 两步骤

## 修改要求
1. **[一般]** 修正 S03 中 `createIfNotExists` 在 `compute` 后调用导致 dead code 的问题：重新设计 DedupTaskScheduler.schedule() 中 dedupKey 的写入策略，确保每行代码均有实际作用。
