# 任务指令（v5）

## 动作
NEW

## 任务描述
在 common-module-api 中新增三个 Store 接口（SessionStore、SuggestionStore、DraftContextStore）及 ConcurrentHashMapStore 实现类，提供线程安全的内存存储抽象层。

预期文件：
- `AIMedical/backend/modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/store/SessionStore.java`
- `AIMedical/backend/modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/store/SuggestionStore.java`
- `AIMedical/backend/modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/store/DraftContextStore.java`
- `AIMedical/backend/modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/store/impl/ConcurrentHashMapStore.java`

## 选择理由
T6（DosageStandard）已通过验证。T4 为底层抽象层，是 consultation（T8）和 prescription（T9/T10）模块的前置依赖，无其他上游依赖，按底层优先策略推进。

## 任务上下文
摘自 OOD（07_ood_phase2_C_3_DE.md）：

### SessionStore<K, V>
- 泛型接口，键值存储
- 方法：V get(K key)、void put(K key, V value)、V remove(K key)、boolean containsKey(K key)、Set<K> keySet()
- 供 DialogueSessionManager 管理对话会话

### SuggestionStore
- 继承 SessionStore<String, AiSuggestionResult> 语义特化（或独立接口）
- 供 DedupTaskScheduler 存储 AI 建议结果
- 需支持原子替换（compute() 语义）

### DraftContextStore
- 继承 SessionStore<String, PrescriptionDraftContext> 语义特化（或独立接口）
- 供 PrescriptionDraftContext 存储处方草稿上下文

### ConcurrentHashMapStore
- 同时实现三个 Store 接口
- 基于 ConcurrentHashMap 提供线程安全的内存存储
- 包路径：com.aimedical.modules.commonmodule.store.impl.ConcurrentHashMapStore
- Phase 2/3 使用；Phase 5 替换为 RedisStore

## 已有代码上下文
- common-module-api 已有包结构：`auth/`（UserFacade, UserInfoResponse, CurrentUser）、`api/`（UserType, PositionEnum）
- 包路径基础：`com.aimedical.modules.commonmodule`
- common-module-api 的 pom.xml 继承 common-module 父 pom，依赖 common、spring-boot-starter、spring-boot-starter-test
- 三个 Store 接口无需新增外部依赖
