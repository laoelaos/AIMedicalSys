# 设计审查报告（v5 r1）

## 审查结果
REJECTED

## 发现

- **[严重]** `ConcurrentHashMapStore.createIfNotExists` 引用了未定义类型 `SuggestionStatus`（第133-134行）。该类型在代码库中不存在，在本设计中未定义，也不是 OOD 中定义的 `AiSuggestionStatus` 枚举。代码将无法编译。同时，设计说明称该类型由 prescription 模块的 `AiSuggestionResult` 实现，但这会在 common-module-api 中引入对 prescription 模块的编译期依赖，违反模块层次结构（下层不能依赖上层）。

- **[严重]** `createIfNotExists` 方法将 DedupTaskScheduler 的业务逻辑（PENDING/COMPLETED/consumed 状态判断）嵌入通用 Store 接口。任务要求仅需 "compute() 语义" 原子替换，`createIfNotExists` 属于过度设计，将上层业务语义（task 生命周期）耦合到底层存储抽象层，导致：① `SuggestionStatus` 幽灵类型；② common-module-api 需感知 prescription 领域概念。

- **[一般]** `createIfNotExists` 方法参数 `taskId` 在实现中未被使用（第132行仅使用 `prescriptionId` 作为 compute key），属于死参数。

- **[一般]** `keySet()` 契约描述为"快照集合"（不可变快照），但实现 `store.keySet()` 返回的是 `ConcurrentHashMap.KeySetView` 实时视图。两者语义不同：视图反映并发修改，快照则不。契约与实现不一致，可能导致调用方产生意外行为。

- **[轻微]** `compute` 方法中 `BiFunction<? super String, ? super Object, ? extends Object>` 通配符过于复杂。`String` 为 final 类型，`Object` 为精确类型，应简化为 `BiFunction<String, Object, Object>`。

## 修改要求（仅 REJECTED 时）

1. **`createIfNotExists` 与 `SuggestionStatus` 问题**：
   - 方案 A（推荐）：删除 `createIfNotExists` 方法及其所有引用，`SuggestionStore` 只需提供 `compute()` 原子替换即可满足任务要求。去重业务逻辑由 `DedupTaskScheduler` 在消费侧通过 `compute()` 自行实现。
   - 方案 B（如需保留）：将 `createIfNotExists` 简化为 `computeIfAbsent(key, supplier)` 语义，移除所有 `SuggestionStatus` 依赖和 task 状态判断；或将该方法移至 prescription 模块的具体实现类中。

2. **`taskId` 死参数**：如保留 `createIfNotExists`，删除未使用的 `taskId` 参数或说明其用途并在实现中使用。

3. **`keySet()` 契约修正**：将描述修正为"返回当前所有键的 `Set` 视图（由 ConcurrentHashMap 支撑），迭代器为弱一致（weakly consistent），不提供快照隔离保证"。
