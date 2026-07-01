# 任务指令（v3）

## 动作
NEW

## 任务描述
修复 common-module 和 prescription 模块中 R25 DEFERRED 的 Store 群 4 项问题 + R29 新增 SuggestionCleanupTask 失效问题：

### S01: SuggestionStore 缺少 createIfNotExists 原子方法
- **文件**: `SuggestionStore.java`
- **需求**: 在 `SuggestionStore` 接口中新增 `createIfNotExists(String key, Object value)` 原子方法，语义：仅当 key 不存在时原子写入 value，返回旧值或 null
- **签名**: `Object createIfNotExists(String key, Object value);`

### S03: DedupTaskScheduler compute 内跨 key 写入
- **文件**: `DedupTaskScheduler.java`
- **问题**: `compute()` lambda 内部（line 39）调用了 `suggestionStore.put(candidateTaskId, newResult)`，这是跨 key 写入，原子性保证被打破
- **需求**:
  1. 移除 compute lambda 内部的 `suggestionStore.put(candidateTaskId, newResult)` 调用
  2. 将候选 task 的 AiSuggestionResult 存储与 dedup key 的 compute 原子操作解耦
  3. 使用 `suggestionStore.createIfNotExists(dedupKey, newResult)` + `suggestionStore.put(candidateTaskId, newResult)` 两步（createIfNotExists 保证 dedup key 仅创建一次，put 为独立操作）
  4. 更新现有测试适配新签名

### S06: 不安全的类型转换
- **文件**: `DedupTaskScheduler.java` line 43
- **问题**: `return ((AiSuggestionResult) result).getTaskId();` 在 compute 闭包外强制类型转换，遇 null 或非 AiSuggestionResult 直接 ClassCastException
- **需求**:
  1. 将返回值改为类型安全模式：`SuggestionStore` 新增 `Object get(String key)` 已存在，但应在 DedupTaskScheduler 中增加 null/类型守卫
  2. 改为 `if (result instanceof AiSuggestionResult r) { return r.getTaskId(); }` + 兜底抛出 IllegalStateException

### S07: ConcurrentHashMapStore 混合存储
- **文件**: `ConcurrentHashMapStore.java`
- **问题**: `ConcurrentHashMapStore` 同时实现 `SuggestionStore` 和 `DraftContextStore`，使用单一 `ConcurrentHashMap<String, Object>` 实例，导致 `SuggestionCleanupTask.keySet()` 遍历时误命中 DraftContext 数据
- **需求**:
  1. `ConcurrentHashMapStore` 改为仅实现 `SuggestionStore`（移除 `DraftContextStore`）
  2. 新增 `DraftContextStoreImpl` 类（包路径 `com.aimedical.modules.commonmodule.store.impl`），单独实现 `DraftContextStore`，使用独立 `ConcurrentHashMap<String, Object>` 实例
  3. `DraftContextCleanupTask` 当前注入的 `DraftContextStore` bean 不受影响（Spring 会根据类型注入新的实现）

### SuggestionCleanupTask 类型适配
- **文件**: `AiSuggestionResult.java` + `SuggestionCleanupTask.java`
- **问题**: `SuggestionCleanupTask` 内部接口 `SuggestionStoreEntry` 定义在 `SuggestionCleanupTask.java` 中，但 `AiSuggestionResult`（实际存入 Store 的类型）未实现该接口，`instanceof` 检查永不通过，清理逻辑静默失效
- **需求**:
  1. 将 `SuggestionStoreEntry` 接口从 `SuggestionCleanupTask` 内部接口提升为独立接口（包路径 `com.aimedical.modules.commonmodule.store.SuggestionStoreEntry`）
  2. 让 `AiSuggestionResult` 实现 `SuggestionStoreEntry`，映射 getStatus → getStatus().name()、isConsumed → isConsumed()、getTimestamp → getCreateTime().toInstant(...)（注意 LocalDateTime → Instant 转换）
  3. `SuggestionCleanupTask` 改为导入并使用独立的 `SuggestionStoreEntry`

## 选择理由
T1（基础设施）和 T2（consultation 业务逻辑）均已 PASSED。T3 为模块间共享 Store 基础设施修复，涵盖 5 个文件的修改，均是独立子任务可顺序实现。修复后 SuggestionCleanupTask 将正常触发 TTL 清理，DraftContextStore 数据不再被误清理，为 T4（DraftContextCleanupTask 迁移）铺平道路。

## 任务上下文
### 当前代码结构
- `SuggestionStore`（interface，extends SessionStore<String, Object>）：common-module-api/store/，仅有 `compute()` 方法
- `ConcurrentHashMapStore`（implements SuggestionStore, DraftContextStore）：common-module-api/store/impl/，单 `ConcurrentHashMap<String, Object>`
- `DraftContextStore`（interface，extends SessionStore<String, Object>）：common-module-api/store/，空标记接口
- `DedupTaskScheduler`：prescription 模块，schedule() 方法使用 compute + 跨 key put + 不安全转型
- `AiSuggestionResult`：prescription 模块 DTO，字段包括 taskId/status/createTime/consumed 等
- `SuggestionCleanupTask`：prescription 模块 task，定时遍历 SuggestionStore，instanceof 检查 SuggestionStoreEntry
- `SuggestionCleanupTask.SuggestionStoreEntry`：内部接口，方法 getStatus()/isConsumed()/getTimestamp()

### 现有测试文件
- `ConcurrentHashMapStoreTest.java`（21 用例，含 DraftContextStore instanceof 断言）
- `DedupTaskSchedulerTest.java`（4 用例，mock SuggestionStore.compute）
- `SuggestionCleanupTaskTest.java`（8 用例，使用 StubSuggestionStore + StubEntry）

## 已有代码上下文
所有源文件路径（相对于项目根目录 `C:\Develop\Software\AIMedicalSys`）：
- `AIMedical/backend/modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/store/SuggestionStore.java`
- `AIMedical/backend/modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/store/DraftContextStore.java`
- `AIMedical/backend/modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/store/impl/ConcurrentHashMapStore.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/service/assist/DedupTaskScheduler.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/dto/assist/AiSuggestionResult.java`
- `AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/task/SuggestionCleanupTask.java`

测试文件路径：
- `AIMedical/backend/modules/common-module/common-module-api/src/test/java/com/aimedical/modules/commonmodule/store/impl/ConcurrentHashMapStoreTest.java`
- `AIMedical/backend/modules/prescription/src/test/java/com/aimedical/modules/prescription/service/assist/DedupTaskSchedulerTest.java`
- `AIMedical/backend/modules/prescription/src/test/java/com/aimedical/modules/prescription/task/SuggestionCleanupTaskTest.java`
