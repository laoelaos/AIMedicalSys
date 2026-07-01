# 详细设计（v24）

## 概述

基于 OOD 文档 `Docs/07_ood_phase2_C_3_DE.md`，修复实现报告 `Docs/Diagnosis/impl/06_phase2C3DE_report.md` 中列出的问题。本设计覆盖：
- 前置修复：`TriageServiceImplTest.java` 编译错误
- 主任务：定时清理任务（SuggestionStore/DraftContext）、药品/模板变更事件广播与监听、TTL 配置更新

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/config/ScheduledTaskConfig.java` | 新建 | `@Configuration` + `@EnableScheduling` 启用全项目定时的配置类 |
| `modules/prescription/src/main/java/com/aimedical/modules/prescription/task/SuggestionCleanupTask.java` | 新建 | 每 5 分钟清理 SuggestionStore 中 COMPLETED/FAILED 且 `consumed=true` 的过期条目（TTL 60 分钟） |
| `modules/consultation/src/main/java/com/aimedical/modules/consultation/task/DraftContextCleanupTask.java` | 新建 | 每 5 分钟清理 DraftContextStore 中超时草稿（TTL 60 分钟） |
| `modules/prescription/src/main/java/com/aimedical/modules/prescription/event/DrugDictChangeEvent.java` | 新建 | 药品字典变更事件基类 |
| `modules/prescription/src/main/java/com/aimedical/modules/prescription/event/DrugContraindicationChangeEvent.java` | 新建 | 药品禁忌变更事件 |
| `modules/prescription/src/main/java/com/aimedical/modules/prescription/event/DrugAllergyMappingChangeEvent.java` | 新建 | 药品过敏映射变更事件 |
| `modules/prescription/src/main/java/com/aimedical/modules/prescription/event/DrugCompositionDictChangeEvent.java` | 新建 | 药品成分字典变更事件 |
| `modules/prescription/src/main/java/com/aimedical/modules/prescription/event/DrugDictChangeEventListener.java` | 新建 | `@EventListener` 监听所有 DrugDictChangeEvent 子类，调用缓存 invalidate |
| `modules/prescription/src/main/java/com/aimedical/modules/prescription/cache/DrugDictCacheManager.java` | 新建 | 包装三个 Caffeine LoadingCache（Contraindication/AllergyMapping/CompositionDict），提供 get 和 invalidate 方法 |
| `modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/event/TemplateConfigChangeEvent.java` | 新建 | 科室模板配置变更事件 |
| `modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/task/VisitIdReconciledTask.java` | 新建 | 每 30 分钟补偿 reconciliation |
| `modules/consultation/src/main/java/com/aimedical/modules/consultation/dialogue/DialogueSessionManager.java` | 修改 | `@Scheduled(fixedRate)` 60000 → 300000 |
| `modules/medical-record/src/main/java/com/aimedical/modules/medicalrecord/template/DatabaseTemplateConfigManager.java` | 修改 | 添加 `@EventListener` 监听 TemplateConfigChangeEvent，调用 `templateCache.invalidateAll()` |
| `modules/consultation/src/test/java/com/aimedical/modules/consultation/TriageServiceImplTest.java` | 修改 | 将 `setUp()` 中局部变量 `sessionManager` 提升为类字段 |

## 类型定义

### ScheduledTaskConfig
**形态**：class
**包路径**：`com.aimedical.modules.commonmodule.config`
**职责**：启用 `@EnableScheduling`，为全项目提供定时任务能力
```java
@Configuration
@EnableScheduling
public class ScheduledTaskConfig {}
```

### SuggestionCleanupTask
**形态**：class
**包路径**：`com.aimedical.modules.prescription.task`
**职责**：定时清理 SuggestionStore 中已消费的过期条目
```java
@Component
public class SuggestionCleanupTask {
    public SuggestionCleanupTask(SuggestionStore suggestionStore); // 构造注入
    @Scheduled(cron = "0 0/5 * * * ?")
    public void cleanupExpiredSuggestions(); // 扫描 keySet，检查条目状态=COMPLETED|FAILED && consumed=true && 写入时间超过 TTL(60min)，remove
}
```
**构造方式**：Spring 自动装配
**依赖**：`SuggestionStore`（common-module-api）

**行为细节**：
- 条目在 SuggestionStore 中的 value 类型为 `Object`，实际存储结构由 R22 定义，包含派生状态和 `consumed` 标记。本任务假设 value 可转换为 `SuggestionStoreEntry` 或具有 getStatus()/isConsumed()/getTimestamp() 方法。
- 过期判定：`entry.getTimestamp().plusMinutes(60).isBefore(Instant.now())`
- 仅移除同时满足 `(COMPLETED 或 FAILED) && consumed==true && 过期` 的条目

### DraftContextCleanupTask
**形态**：class
**包路径**：`com.aimedical.modules.consultation.task`
**职责**：定时清理 DraftContextStore 中超时草稿（TTL 60 分钟）
```java
@Component
public class DraftContextCleanupTask {
    private final DraftContextStore draftContextStore;
    private final ConcurrentHashMap<String, Instant> writeTimestamps; // 独立时间追踪表
    public DraftContextCleanupTask(DraftContextStore draftContextStore);
    @Scheduled(cron = "0 0/5 * * * ?")
    public void cleanupExpiredDrafts(); // 遍历 keySet，对比 writeTimestamps 判断是否过期，remove 过期项并清除对应时间戳
}
```
**构造方式**：Spring 自动装配
**行为契约**：
- `writeTimestamps` 在构造时初始化
- `DraftContextStore.put(key, value)` 不会被本任务拦截——需要上游调用者（如 `PrescriptionDraftContext`）在 `saveDraft()` 中同步调用 `WriteTimestampRecorder.record(key, Instant.now())`
- 建议：DraftContextCleanupTask 提供 `public void recordWrite(String key, Instant timestamp)` 和 `public void removeTimestamp(String key)` 公开方法，供 `PrescriptionDraftContext` 在 put/remove 时调用
- 清理任务仅判定 `writeTimestamps` 中存在且 `timestamp.plusMinutes(60).isBefore(Instant.now())` 的条目

### DrugDictChangeEvent
**形态**：class（abstract 基类）
**包路径**：`com.aimedical.modules.prescription.event`
**职责**：药品字典变更事件的共享基类
```java
public abstract class DrugDictChangeEvent {
    public enum ChangeType { CREATE, UPDATE, DELETE }
    private final ChangeType changeType;
    private final String drugCode;
    // 构造 + getter
    public DrugDictChangeEvent(ChangeType changeType, String drugCode);
}
```

### DrugContraindicationChangeEvent
**形态**：class
**包路径**：`com.aimedical.modules.prescription.event`
**继承**：`DrugDictChangeEvent`
```java
public class DrugContraindicationChangeEvent extends DrugDictChangeEvent {
    public DrugContraindicationChangeEvent(ChangeType changeType, String drugCode);
}
```

### DrugAllergyMappingChangeEvent
**形态**：class
**包路径**：`com.aimedical.modules.prescription.event`
**继承**：`DrugDictChangeEvent`
```java
public class DrugAllergyMappingChangeEvent extends DrugDictChangeEvent {
    public DrugAllergyMappingChangeEvent(ChangeType changeType, String drugCode);
}
```

### DrugCompositionDictChangeEvent
**形态**：class
**包路径**：`com.aimedical.modules.prescription.event`
**继承**：`DrugDictChangeEvent`
```java
public class DrugCompositionDictChangeEvent extends DrugDictChangeEvent {
    public DrugCompositionDictChangeEvent(ChangeType changeType, String drugCode);
}
```

### DrugDictCacheManager
**形态**：class
**包路径**：`com.aimedical.modules.prescription.cache`
**职责**：药品字典 Caffeine 缓存管理器，提供缓存读取和失效方法
```java
@Component
public class DrugDictCacheManager {
    // 三个独立的 LoadingCache
    private final LoadingCache<String, DrugContraindicationMapping> contraindicationCache;
    private final LoadingCache<String, DrugAllergyMapping> allergyMappingCache;
    private final LoadingCache<String, DrugCompositionDict> compositionDictCache;
    
    public DrugDictCacheManager(
        DrugContraindicationMappingRepository contraindicationRepo,
        DrugAllergyMappingRepository allergyMappingRepo,
        DrugCompositionDictRepository compositionDictRepo
    );
    
    public DrugContraindicationMapping getContraindication(String drugCode);
    public DrugAllergyMapping getAllergyMapping(String drugCode);
    public DrugCompositionDict getCompositionDict(String drugCode);
    public void invalidateAll();                   // 全量失效
    public void invalidateContraindication(String drugCode);
    public void invalidateAllergyMapping(String drugCode);
    public void invalidateCompositionDict(String drugCode);
}
```
**构造方式**：Spring 自动装配，构造函数中初始化三个 Caffeine LoadingCache（`refreshAfterWrite`）
**缓存 TTL**：60 分钟，与各 Repository 查询配合使用
**缓存 key**：`drugCode`（String）

### DrugDictChangeEventListener
**形态**：class
**包路径**：`com.aimedical.modules.prescription.event`
**职责**：监听药品字典变更事件并失效对应缓存
```java
@Component
public class DrugDictChangeEventListener {
    private final DrugDictCacheManager cacheManager;
    public DrugDictChangeEventListener(DrugDictCacheManager cacheManager);
    
    @EventListener
    public void handleContraindicationChange(DrugContraindicationChangeEvent event);
    @EventListener
    public void handleAllergyMappingChange(DrugAllergyMappingChangeEvent event);
    @EventListener
    public void handleCompositionDictChange(DrugCompositionDictChangeEvent event);
}
```

行为契约：
- `handleContraindicationChange` → `cacheManager.invalidateContraindication(event.getDrugCode())`
- `handleAllergyMappingChange` → `cacheManager.invalidateAllergyMapping(event.getDrugCode())`
- `handleCompositionDictChange` → `cacheManager.invalidateCompositionDict(event.getDrugCode())`

### TemplateConfigChangeEvent
**形态**：class
**包路径**：`com.aimedical.modules.medicalrecord.event`
**职责**：科室模板配置变更事件（admin 模块 Service 发布 → DatabaseTemplateConfigManager 监听）
```java
public class TemplateConfigChangeEvent {
    private final String departmentId; // 可为 null，表示全量变更
    public TemplateConfigChangeEvent(String departmentId);
}
```

### DatabaseTemplateConfigManager（修改）
**修改内容**：
- 新增 `@EventListener` 方法监听 `TemplateConfigChangeEvent`
- 接收事件后调用 `templateCache.invalidateAll()`
```java
@EventListener
public void handleTemplateConfigChange(TemplateConfigChangeEvent event) {
    templateCache.invalidateAll(); // 不限 departmentId 全量失效，保证一致性
}
```

### VisitIdReconciledTask
**形态**：class
**包路径**：`com.aimedical.modules.medicalrecord.task`
**职责**：每 30 分钟执行 visit ID 补偿 reconciliation
```java
@Component
public class VisitIdReconciledTask {
    private final VisitFacade visitFacade;
    private final MedicalRecordRepository medicalRecordRepository;
    public VisitIdReconciledTask(VisitFacade visitFacade, MedicalRecordRepository medicalRecordRepository);
    @Scheduled(cron = "0 */30 * * * ?")
    public void reconcileVisitIds(); // 扫描 medical_record 中 visitId 为空的记录，通过 VisitFacade 补偿填充
}
```
**依赖**：`VisitFacade`（common-module-api）、`MedicalRecordRepository`（medical-record）

### DialogueSessionManager（修改）
**修改内容**：第 64 行 `@Scheduled(fixedRate = 60000)` → `@Scheduled(fixedRate = 300000)`
- `SESSION_TTL_MINUTES = 30` 不变（已符合 OOD §6.1 要求）
- `evictExpiredSessions()` 方法体不变

### TriageServiceImplTest（前置修复）
**修改内容**：
1. 在类字段区域（第 57-67 行之间）增加：`private DialogueSessionManager sessionManager;`
2. `setUp()` 第 77 行：`DialogueSessionManager sessionManager = ...` → `sessionManager = ...`
3. 确认第 225 行 `shouldFallbackOnTimeout` 中 `sessionManager` 引用正确

## 错误处理

| 场景 | 处理方式 |
|------|---------|
| 定时任务执行期间抛出异常 | 由 `@Scheduled` 默认策略处理（日志记录，不影响下次调度），不主动 try-catch |
| DrugDictCacheManager 缓存加载失败 | Caffeine CacheLoader 抛出异常时，异常传播给调用方（规则引擎），不静默处理 |
| DrugDictChangeEventListener 处理异常 | Spring `@EventListener` 默认同步抛出，不影响发布方 |
| DatabaseTemplateConfigManager 监听异常 | 同上 |
| VisitIdReconciledTask 某条记录补偿失败 | 单独 try-catch 记录日志，继续处理下一条，不阻断全量扫描 |
| SuggestionCleanupTask 中条目 value 类型不符合预期 | 跳过该条目（try-catch ClassCastException） |

## 行为契约

### SuggestionCleanupTask
- 前置条件：SuggestionStore 非 null，条目 value 可转换为 `SuggestionStoreEntry`（或兼容接口）
- 后置条件：移除所有满足条件的条目
- 幂等：是，多次执行结果相同

### DraftContextCleanupTask
- 前置条件：DraftContextStore 非 null，`writeTimestamps` 已初始化
- 调用顺序：上游（PrescriptionDraftContext）在 `put` 后调用 `recordWrite()`
- 后置条件：移除所有过期条目及其时间戳记录
- 幂等：是

### DrugDictChangeEventListener
- 前置条件：`DrugDictCacheManager` 已初始化
- 后置条件：对应 drugCode 的缓存条目被失效，下次访问重新从 DB 加载
- 一致性级别：最终一致性（Caffeine refresh 机制保证）

### DatabaseTemplateConfigManager.handleTemplateConfigChange
- 前置条件：`templateCache` 非 null
- 后置条件：全部模板缓存被失效，下次 getTemplate 时重新加载
- 一致性级别：最终一致性

## 依赖关系

| 新建类型 | 依赖已有类型 | 暴露接口 |
|---------|-------------|---------|
| ScheduledTaskConfig | 无 | 无（标记注解） |
| SuggestionCleanupTask | `SuggestionStore` | 无 |
| DraftContextCleanupTask | `DraftContextStore` | `recordWrite(key, Instant)`, `removeTimestamp(key)` |
| DrugDictChangeEvent | 无 | 基类供子类继承 |
| DrugContraindicationChangeEvent | `DrugDictChangeEvent` | 构造器 |
| DrugAllergyMappingChangeEvent | `DrugDictChangeEvent` | 构造器 |
| DrugCompositionDictChangeEvent | `DrugDictChangeEvent` | 构造器 |
| DrugDictCacheManager | 三个 Repository 接口 | `getContraindication`, `getAllergyMapping`, `getCompositionDict`, `invalidate*` |
| DrugDictChangeEventListener | `DrugDictCacheManager` | 无 |
| TemplateConfigChangeEvent | 无 | 构造器 |
| DatabaseTemplateConfigManager（修改） | `TemplateConfigChangeEvent` | `handleTemplateConfigChange`（私有 event listener） |
| VisitIdReconciledTask | `VisitFacade`, `MedicalRecordRepository` | 无 |
| DialogueSessionManager（修改） | 无变化 | 无变化 |
| TriageServiceImplTest（修改） | 无变化 | 无变化 |
