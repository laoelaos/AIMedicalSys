# 详细设计（v3）

## 概述
修复 common-module 和 prescription 模块中 R25 DEFERRED 的 Store 群 4 项问题（S01/S03/S06/S07）+ R29 新增 SuggestionCleanupTask 失效问题，涉及 3 个接口/类修改、2 个新建文件、2 个 prescription 模块文件修改。v3 r2 修正：DedupTaskScheduler 改用 `get + createIfNotExists + compute(兜底)` 混合策略，正确处理 COMPLETED+consumed/FAILED 状态下的原子替换。

## 文件规划
| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `common-module-api/.../store/SuggestionStore.java` | 修改 | 新增 `createIfNotExists(String, Object)` 方法签名 |
| `common-module-api/.../store/SuggestionStoreEntry.java` | **新建** | 独立接口，从 `SuggestionCleanupTask` 内部接口提升 |
| `common-module-api/.../store/impl/ConcurrentHashMapStore.java` | 修改 | 新增 `createIfNotExists` 实现；移除 `DraftContextStore` 实现声明 |
| `common-module-api/.../store/impl/DraftContextStoreImpl.java` | **新建** | 独立 `DraftContextStore` 实现，使用独立 `ConcurrentHashMap` 实例 |
| `prescription/.../dto/assist/AiSuggestionResult.java` | 修改 | 实现 `SuggestionStoreEntry` |
| `prescription/.../service/assist/DedupTaskScheduler.java` | 修改 | 重构 `schedule()`：`get(快速路径) + createIfNotExists(首次创建) + compute(兜底替换)` 混合策略，类型安全转型 |
| `prescription/.../task/SuggestionCleanupTask.java` | 修改 | 导入独立 `SuggestionStoreEntry`；`getStatus()` → `getStatusName()` |

## 类型定义

### S01: SuggestionStore 接口修改
**形态**：interface（已有）
**包路径**：`com.aimedical.modules.commonmodule.store`
**变更**：新增方法签名

```java
public interface SuggestionStore extends SessionStore<String, Object> {
    Object compute(String key, BiFunction<String, Object, Object> remappingFunction);
    Object createIfNotExists(String key, Object value);
}
```

**语义**：仅当 key 不存在时原子写入 value；返回 key 关联的旧值，若之前无关联则返回 null。
**实现（ConcurrentHashMapStore）**：
```java
@Override
public Object createIfNotExists(String key, Object value) {
    return store.putIfAbsent(key, value);
}
```
`ConcurrentHashMap.putIfAbsent(key, value)` 语义：key 已存在时返回旧值且不修改；key 不存在时写入 value 并返回 null。

### SuggestionStoreEntry 独立接口
**形态**：interface（新建，从 `SuggestionCleanupTask` 内部接口提升）
**包路径**：`com.aimedical.modules.commonmodule.store`
**职责**：存储条目查询契约，供 SuggestionCleanupTask 遍历检查

**注意**：方法名从原内部接口的 `getStatus()` 改为 `getStatusName()`，以避免与 `AiSuggestionResult.getStatus()`（返回 `AiSuggestionStatus`）产生 Java 方法签名冲突（同名不同返回类型不合法）。

```java
package com.aimedical.modules.commonmodule.store;

import java.time.Instant;

public interface SuggestionStoreEntry {
    String getStatusName();
    boolean isConsumed();
    Instant getTimestamp();
}
```

### S07: ConcurrentHashMapStore 修改
**形态**：class（已有）
**包路径**：`com.aimedical.modules.commonmodule.store.impl`
**变更**：
1. 移除 `implements DraftContextStore`（从签名中删除 `DraftContextStore`）
2. 移除 import `DraftContextStore`
3. 新增 `createIfNotExists` 方法实现（如上）

```java
public class ConcurrentHashMapStore implements SuggestionStore {
    // 已有字段和方法保持不变
    // 新增：
    @Override
    public Object createIfNotExists(String key, Object value) {
        return store.putIfAbsent(key, value);
    }
}
```

### DraftContextStoreImpl 新建类
**形态**：class（新建）
**包路径**：`com.aimedical.modules.commonmodule.store.impl`
**职责**：独立实现 `DraftContextStore`，使用独立的 `ConcurrentHashMap<String, Object>` 实例，避免与 `SuggestionStore` 共享键空间
**Spring bean 注册**：添加 `@Service` 注解，由 Spring 组件扫描自动注册为 bean。`PrescriptionDraftContext` 和 `DraftContextCleanupTask` 通过构造器注入 `DraftContextStore` 接口时将获得该新实现。

```java
package com.aimedical.modules.commonmodule.store.impl;

import com.aimedical.modules.commonmodule.store.DraftContextStore;
import org.springframework.stereotype.Service;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DraftContextStoreImpl implements DraftContextStore {

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
}
```

### AiSuggestionResult 实现 SuggestionStoreEntry
**形态**：class（已有）
**包路径**：`com.aimedical.modules.prescription.dto.assist`
**变更**：声明实现 `SuggestionStoreEntry`，新增 3 个映射方法

```java
public class AiSuggestionResult implements SuggestionStoreEntry {
    // 已有字段和方法保持不变
    // 新增：
    @Override
    public String getStatusName() {
        return status != null ? status.name() : null;
    }

    @Override
    public boolean isConsumed() {
        return consumed;
    }

    @Override
    public Instant getTimestamp() {
        return createTime != null ? createTime.toInstant(ZoneOffset.UTC) : null;
    }
}
```

**新增 import**：`java.time.Instant`、`java.time.ZoneOffset`、`com.aimedical.modules.commonmodule.store.SuggestionStoreEntry`

### S03+S06: DedupTaskScheduler.schedule() 重构
**形态**：class（已有）
**包路径**：`com.aimedical.modules.prescription.service.assist`
**变更**：采用 `get + createIfNotExists + compute(兜底)` 混合策略；消除跨 key 写入和 unsafe cast；正确处理已过期 dedupKey 的原子替换

```java
public String schedule(String prescriptionId) {
    String dedupKey = DEDUP_KEY_PREFIX + prescriptionId;
    String candidateTaskId = UUID.randomUUID().toString();

    // Step 1: 快速路径——非原子读，减少竞争
    Object existing = suggestionStore.get(dedupKey);
    if (existing instanceof AiSuggestionResult r) {
        if (r.getStatus() == AiSuggestionStatus.PENDING
                || (r.getStatus() == AiSuggestionStatus.COMPLETED && !r.isConsumed())) {
            return r.getTaskId();
        }
    }

    // Step 2: 构造新结果
    AiSuggestionResult newResult = new AiSuggestionResult();
    newResult.setTaskId(candidateTaskId);
    newResult.setStatus(AiSuggestionStatus.PENDING);
    newResult.setCreateTime(LocalDateTime.now());

    // Step 3: 原子创建——仅当 key 不存在时写入
    Object oldValue = suggestionStore.createIfNotExists(dedupKey, newResult);
    if (oldValue == null) {
        // 胜出：首次为该 dedupKey 创建条目
        suggestionStore.put(candidateTaskId, newResult);
        return candidateTaskId;
    }

    // Step 4: key 已存在，检查旧值是否可复用
    if (oldValue instanceof AiSuggestionResult r) {
        if (r.getStatus() == AiSuggestionStatus.PENDING
                || (r.getStatus() == AiSuggestionStatus.COMPLETED && !r.isConsumed())) {
            return r.getTaskId();
        }
    }

    // Step 5: 旧值不可复用（COMPLETED+consumed 或 FAILED 等），原子替换
    // compute 处理 Step 3→4 之间的 TOCTOU 竞态：lambda 内重读当前值
    Object result = suggestionStore.compute(dedupKey, (key, currentValue) -> {
        if (currentValue instanceof AiSuggestionResult current) {
            if (current.getStatus() == AiSuggestionStatus.PENDING
                    || (current.getStatus() == AiSuggestionStatus.COMPLETED && !current.isConsumed())) {
                return current; // 已被其他线程更新为可复用值，不替换
            }
        }
        return newResult;
    });

    // Step 6: 判定替换是否成功
    if (result == newResult) {
        suggestionStore.put(candidateTaskId, newResult);
        return candidateTaskId;
    } else if (result instanceof AiSuggestionResult winner) {
        return winner.getTaskId();
    } else {
        throw new IllegalStateException("Unexpected value type for dedupKey: " + result);
    }
}
```

**逻辑说明**：
- Step 1（快速路径）的 `instanceof` 守卫已安全处理 null 和类型不匹配（S06）
- Step 3（`createIfNotExists`）：仅当 key 不存在时原子写入，返回 null 表示创建成功（S01 语义）
- Step 4：复用判定仅限于 PENDING 和 COMPLETED+unconsumed；其他状态（COMPLETED+consumed、FAILED、非 AiSuggestionResult）均触发 Step 5 替换
- Step 5（`compute` 兜底）：原子地重读并判断当前值，只有仍然不可复用时才替换为 newResult。lambda 内无跨 key `put`，消除原 line 39 的原子性问题（S03）
- Step 6：所有跨 key `put` 均在 lambda 外执行（S03）。兜底 `IllegalStateException` 应对极端的非 AiSuggestionResult 类型值（S06）
- 覆盖原 `compute` 算法的全部状态：PENDING 复用、COMPLETED+unconsumed 复用、COMPLETED+consumed/FAILED 替换、并发写入者判定

### SuggestionCleanupTask 适配
**形态**：class（已有）
**包路径**：`com.aimedical.modules.prescription.task`
**变更**：
1. 删除内部接口 `SuggestionStoreEntry` 定义
2. 导入独立接口：`import com.aimedical.modules.commonmodule.store.SuggestionStoreEntry;`
3. 将 `entry.getStatus()` 调用改为 `entry.getStatusName()`

```java
// 移除 line 48-52 的内部接口定义
// 新增 import
// line 42: 修改
String status = entry.getStatusName();
```

## 错误处理

### S06 类型安全
- Step 1/4/6 的 `instanceof` 守卫消除了原 `ClassCastException` 路径
- Step 6 兜底：`IllegalStateException` 仅在极端的非 `AiSuggestionResult` 类型值写入 dedupKey 时触发（如 `HashMap` 混入），保护调用方免于收到无声错误

### createIfNotExists 空竞争
- 返回 null → 当前线程创建成功
- 返回非 null → 另一线程/请求先创建或 key 已存在
- 两种路径均为正常业务逻辑

### compute 兜底路径
- `compute` 仅在旧值不可复用时执行，属于少发路径
- lambda 内返回 `current`（不修改）表示值已被其他线程更新为可复用状态，属正常竞态

## 行为契约

### DedupTaskScheduler.schedule() 状态机与重入安全

**状态判定矩阵**：

| dedupKey 当前值 | Step 1 快速路径 | Step 3 createIfNotExists | Step 5 compute | 最终返回 taskId |
|---|---|---|---|---|
| 不存在 | N/A（existing=null） | 创建成功（返回 null） | 不执行 | candidateTaskId |
| PENDING | 命中 → 直接返回 | 不会执行 | 不执行 | 现有 taskId |
| COMPLETED+unconsumed | 命中 → 直接返回 | 不会执行 | 不执行 | 现有 taskId |
| COMPLETED+consumed | 未命中 | 创建失败（返回旧值） | 执行 → 替换为 newResult | candidateTaskId |
| FAILED | 未命中 | 创建失败（返回旧值） | 执行 → 替换为 newResult | candidateTaskId |
| 其他类型/非 AiSuggestionResult | 未命中 | 创建失败（返回旧值） | 执行 → 替换为 newResult | candidateTaskId |

**并发正确性**：
- `createIfNotExists` 保证至多一个线程为新 key 创建成功
- `compute` 兜底时原子重读当前值，避免 Step 3→4 之间的 TOCTOU 竞态
- 所有跨 key `put` 在 lambda 外执行，不破坏 compute 的原子性

**幂等性**：
- 同一 `prescriptionId` 的 PENDING 或 COMPLETED+unconsumed 结果被复用
- COMPLETED+consumed 或 FAILED 的结果被原子替换，生成新 task

### SuggestionStore.createIfNotExists 契约
- **前置**：key 非 null，value 非 null
- **后置**：返回 key 之前关联的值（存在时），null（不存在时或 value 刚刚写入后）
- **原子性**：与 `ConcurrentHashMap.putIfAbsent` 一致
- **null 参数**：传入 null key 或 null value 时抛出 `NullPointerException`（由 `ConcurrentHashMap.putIfAbsent` 原生行为保证）

### SuggestionStoreEntry 接口契约
- `getStatusName()` 返回状态枚举的名称字符串（如 `"COMPLETED"`、`"FAILED"`、`"PENDING"`）
- `isConsumed()` 返回是否已被消费
- `getTimestamp()` 返回条目的时间戳（`Instant`）

## 依赖关系

### 新增依赖方向
- `common-module-api` → 新增 `SuggestionStoreEntry` 接口（无额外依赖）
- `prescription` → `common-module-api`（`AiSuggestionResult` 实现 `SuggestionStoreEntry`；`SuggestionCleanupTask` 导入独立接口）
- `DraftContextStoreImpl` 无新增模块间依赖（同包内实现已有接口）

### 暴露给后续任务的接口
- `SuggestionStore.createIfNotExists(String, Object)` — 新增公开方法
- `SuggestionStoreEntry` — 新增独立接口类型
- `AiSuggestionResult` 实现 `SuggestionStoreEntry` — 对清理任务可见

## 修订说明（v3 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| S03: `createIfNotExists(dedupKey, result)` 在 `compute(dedupKey, ...)` 后调用始终是空操作（dead code） | 采纳。移除 `compute()` 调用，改用 `get + createIfNotExists` 两步策略：先非原子读快速路径，再用 `createIfNotExists` 实现真正的原子创建语义。`createIfNotExists` 成为实际生效的原子写入操作而非 dead code。同时附带消除了 S06 的 unsafe cast（全程使用 `instanceof` 模式匹配）。 |

## 修订说明（v3 r2）
| 审查意见 | 修改措施 |
|---------|---------|
| **[严重]** `DedupTaskScheduler.schedule()` 当 dedupKey 已存在 COMPLETED+consumed 或 FAILED 结果时行为错误：v3 r1 的 `get + createIfNotExists` 两步策略在 key 已存在但不可复用时直接返回旧 taskId，而原 `compute` 算法会创建新任务并原子替换；测试 `shouldCreateNewTaskWhenFailed` 明确验证 FAILED 应返回新 taskId | 采纳。改为 `get(快速路径) + createIfNotExists(首次创建) + compute(兜底替换)` 混合策略：当 Step 3 `createIfNotExists` 返回非 null 且旧值不可复用时，Step 5 使用 `compute` 原子替换 dedupKey。跨 key `put` 始终在 lambda 外执行。详细状态机见"行为契约"节的状态判定矩阵。 |
| **[轻微]** `DraftContextStoreImpl` Spring Bean 注册未明确：设计未说明新实现如何注册为 bean | 采纳。`DraftContextStoreImpl` 添加 `@Service` 注解，由组件扫描自动注册。已在 §DraftContextStoreImpl 的类型定义中补充。 |
