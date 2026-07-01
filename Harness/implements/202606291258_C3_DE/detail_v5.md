# 详细设计（v5）

## 概述

在 common-module-api 中新增三个 Store 接口（SessionStore、SuggestionStore、DraftContextStore）及 ConcurrentHashMapStore 实现类，提供线程安全的内存存储抽象层，供 consultation 模块（DialogueSessionManager）、prescription 模块（DedupTaskScheduler、PrescriptionDraftContext）在 Phase 2/3 中使用，Phase 5 替换为 RedisStore。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/store/SessionStore.java` | 新建 | 泛型键值存储接口 |
| `AIMedical/backend/modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/store/SuggestionStore.java` | 新建 | AI 建议结果存储接口，扩展 SessionStore，增加 compute 原子替换方法 |
| `AIMedical/backend/modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/store/DraftContextStore.java` | 新建 | 处方草稿上下文存储接口，扩展 SessionStore |
| `AIMedical/backend/modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/store/impl/ConcurrentHashMapStore.java` | 新建 | 同时实现三个接口的线程安全内存存储 |

## 类型定义

### SessionStore
**形态**：interface（泛型）
**包路径**：`com.aimedical.modules.commonmodule.store`
**职责**：泛型键值存储基础契约，提供线程安全的存取/删除/存在性检查/键集查询能力

```java
package com.aimedical.modules.commonmodule.store;

import java.util.Set;

public interface SessionStore<K, V> {
    V get(K key);
    void put(K key, V value);
    V remove(K key);
    boolean containsKey(K key);
    Set<K> keySet();
}
```

**公开接口**：
- `V get(K key)` — 根据 key 获取值，key 不存在返回 null
- `void put(K key, V value)` — 存入键值对，已存在则覆盖
- `V remove(K key)` — 删除键值对，返回被删除的值，key 不存在返回 null
- `boolean containsKey(K key)` — 检查 key 是否存在
- `Set<K> keySet()` — 返回所有键的 Set 视图（由 ConcurrentHashMap 支撑），迭代器为弱一致，不提供快照隔离保证

**构造方式**：无（接口）
**类型关系**：SuggestionStore、DraftContextStore 均扩展此接口

### SuggestionStore
**形态**：interface（扩展 SessionStore）
**包路径**：`com.aimedical.modules.commonmodule.store`
**职责**：AI 建议结果存储接口，供 DedupTaskScheduler 使用。值类型暂以 Object 占位（语义绑定点 AiSuggestionResult 在 prescription 模块定义），提供 compute 原子替换方法

```java
package com.aimedical.modules.commonmodule.store;

import java.util.Set;
import java.util.function.BiFunction;

public interface SuggestionStore extends SessionStore<String, Object> {
    Object compute(String key, BiFunction<String, Object, Object> remappingFunction);
}
```

**公开接口**：
- 继承 SessionStore 全部方法（key 绑定 String，value 绑定 Object）
- `Object compute(String key, BiFunction<String, Object, Object> remappingFunction)` — 原子替换，对指定 key 应用 remappingFunction，返回新值；remappingFunction 接收 (key, oldValue) 返回 newValue；若 remappingFunction 返回 null 则删除该条目

**构造方式**：无（接口）
**类型关系**：扩展 SessionStore<String, Object>

### DraftContextStore
**形态**：interface（扩展 SessionStore）
**包路径**：`com.aimedical.modules.commonmodule.store`
**职责**：处方草稿上下文存储接口，供 PrescriptionDraftContext 使用。值类型暂以 Object 占位（语义绑定点 PrescriptionDraftContext 在 prescription 模块定义），当前无需额外方法

```java
package com.aimedical.modules.commonmodule.store;

public interface DraftContextStore extends SessionStore<String, Object> {
}
```

**公开接口**：继承 SessionStore 全部方法（key 绑定 String，value 绑定 Object）
**构造方式**：无（接口）
**类型关系**：扩展 SessionStore<String, Object>

### ConcurrentHashMapStore
**形态**：class
**包路径**：`com.aimedical.modules.commonmodule.store.impl`
**职责**：同时实现 SuggestionStore 和 DraftContextStore 接口（经由各自继承链覆盖 SessionStore 全部方法），基于 ConcurrentHashMap 提供线程安全的内存存储，Phase 2/3 使用，Phase 5 替换为 RedisStore

```java
package com.aimedical.modules.commonmodule.store.impl;

import com.aimedical.modules.commonmodule.store.DraftContextStore;
import com.aimedical.modules.commonmodule.store.SessionStore;
import com.aimedical.modules.commonmodule.store.SuggestionStore;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

public class ConcurrentHashMapStore implements SuggestionStore, DraftContextStore {

    private final ConcurrentHashMap<String, Object> store = new ConcurrentHashMap<>();

    @Override
    public Object get(String key) { return store.get(key); }

    @Override
    public void put(String key, Object value) { store.put(key, value); }

    @Override
    public Object remove(String key) { return store.remove(key); }

    @Override
    public boolean containsKey(String key) { return store.containsKey(key); }

    @Override
    public Set<String> keySet() { return store.keySet(); }

    @Override
    public Object compute(String key, BiFunction<String, Object, Object> remappingFunction) {
        return store.compute(key, remappingFunction);
    }
}
```

**公开接口**：
- 实现 SuggestionStore 全部 6 个方法（get/put/remove/containsKey/keySet + compute）
- 实现 DraftContextStore 全部 5 个方法（get/put/remove/containsKey/keySet）

**构造方式**：默认无参构造器（内部初始化 ConcurrentHashMap）
**类型关系**：实现 SuggestionStore、DraftContextStore（间接实现 SessionStore<String, Object>）

## 方法行为契约

### get(K key)
- **前置条件**：key 不为 null
- **后置条件**：返回 key 对应的 value，或 key 不存在时返回 null
- **并发保证**：根据 ConcurrentHashMap 的语义提供 happens-before 保证

### put(K key, V value)
- **前置条件**：key 不为 null，value 不为 null
- **后置条件**：key-value 被存入 store，已存在则覆盖旧值
- **并发保证**：同 ConcurrentHashMap.put()

### remove(K key)
- **前置条件**：key 不为 null
- **后置条件**：key 对应的条目被删除，返回被删除的 value；key 不存在时返回 null
- **并发保证**：同 ConcurrentHashMap.remove()

### containsKey(K key)
- **前置条件**：key 不为 null
- **后置条件**：返回 true 当且仅当 key 存在于 store 中
- **并发保证**：同 ConcurrentHashMap.containsKey()

### keySet()
- **前置条件**：无
- **后置条件**：返回当前所有键的 Set 视图（由 ConcurrentHashMap 支撑），非不可变快照
- **并发保证**：迭代器为弱一致（weakly consistent），不提供快照隔离保证，不保证反映并发修改

### compute(key, remappingFunction)
- **前置条件**：key 不为 null，remappingFunction 不为 null
- **后置条件**：remappingFunction 在锁保护下原子执行，返回新值；remappingFunction 返回 null 则删除条目
- **并发保证**：同一 key 上的 compute 操作互斥（ConcurrentHashMap 分段锁保证）

## 错误处理

- 所有方法接受 null key 时抛出 `NullPointerException`（ConcurrentHashMap 原生行为）
- put 方法接受 null value 时抛出 `NullPointerException`
- compute 方法接受 null remappingFunction 时抛出 `NullPointerException`
- 无自定义异常类型，依赖 JDK 标准异常

## 依赖关系

- **编译期依赖**：
  - `java.util.Set`（JDK 内置）
  - `java.util.concurrent.ConcurrentHashMap`（JDK 内置）
  - `java.util.function.BiFunction`（JDK 内置）
  - 无需新增外部依赖（common-module-api 现有 spring-boot-starter 和 common 依赖已满足）

- **运行时依赖**：无特殊要求

- **被依赖**（后续任务）：
  - consultation 模块的 `DialogueSessionManager` —— 依赖 `SessionStore<String, DialogueSession>`
  - prescription 模块的 `DedupTaskScheduler` —— 依赖 `SuggestionStore`（具体类型 AiSuggestionResult）
  - prescription 模块的 `PrescriptionDraftContext` —— 依赖 `DraftContextStore`（具体类型 PrescriptionDraftContext）

- **值类型语义绑定说明**：SuggestionStore 和 DraftContextStore 的值类型当前以 Object 占位。待 prescription 模块的 AiSuggestionResult 和 PrescriptionDraftContext 类型创建完成后，使用方（DedupTaskScheduler、PrescriptionDraftContext）在注入处通过类型转换获得具体类型安全。Phase 5 替换为 RedisStore 时，接口签名不变。

## 修订说明（v5 r2）
| 审查意见 | 修改措施 |
|---------|---------|
| [一般] SessionStore 公开接口第42行 `keySet()` 描述仍为"不可变快照集合"，与方法行为契约（第159行）自相矛盾 | 将公开接口 `keySet()` 描述与方法行为契约对齐，改为"返回所有键的 Set 视图（由 ConcurrentHashMap 支撑），迭代器为弱一致，不提供快照隔离保证" |

## 修订说明（v5 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] `ConcurrentHashMapStore.createIfNotExists` 引用了未定义类型 `SuggestionStatus`，代码无法编译；且会在 common-module-api 中引入对 prescription 模块的编译期依赖，违反模块层次结构 | 删除 `createIfNotExists` 方法及其所有引用，消除 `SuggestionStatus` 幽灵类型和跨层依赖 |
| [严重] `createIfNotExists` 将 DedupTaskScheduler 的业务逻辑（PENDING/COMPLETED/consumed 状态判断）嵌入通用 Store 接口，属于过度设计，耦合上层业务语义到底层存储抽象层 | 采纳方案 A：完全移除 `createIfNotExists`，去重业务逻辑由 `DedupTaskScheduler` 在消费侧通过 `compute()` 自行实现 |
| [一般] `createIfNotExists` 参数 `taskId` 在实现中未被使用，属于死参数 | 随 `createIfNotExists` 删除而自然消除 |
| [一般] `keySet()` 契约描述为"不可变快照"，但实现返回 `ConcurrentHashMap.KeySetView` 实时视图，语义不一致 | 将 `keySet()` 行为契约修正为：返回 ConcurrentHashMap 支撑的 Set 视图，迭代器为弱一致，不提供快照隔离保证 |
| [轻微] `compute` 方法中 `BiFunction<? super String, ? super Object, ? extends Object>` 通配符过于复杂 | 简化为 `BiFunction<String, Object, Object>`（String 为 final 类型，Object 为精确类型，无需通配符） |
