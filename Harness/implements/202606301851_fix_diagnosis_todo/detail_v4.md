# 详细设计（v4）

## 概述
修复 prescription 模块中 3 项问题：P14 CRITICAL 阻断写入（AI 失败/降级路径未清除旧 CRITICAL 告警）、DraftContextCleanupTask 从 consultation 模块迁移到 prescription 模块、移除 enrichWithDrugInfo 死代码及 DrugFacade 注入。涉及 4 个源码文件修改、2 个新建文件、2 个删除文件、4 个测试文件修改。

## 文件规划
| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `prescription/.../service/assist/impl/PrescriptionAssistServiceImpl.java` | 修改 | P14 修复 + 移除 enrichWithDrugInfo + 移除 DrugFacade 注入 |
| `prescription/.../service/audit/impl/PrescriptionAuditServiceImpl.java` | 修改 | 移除 enrichWithDrugInfo + 移除 DrugFacade 注入 |
| `consultation/.../task/DraftContextCleanupTask.java` | 删除 | 迁移到 prescription 模块 |
| `consultation/.../task/DraftContextCleanupTaskTest.java` | 删除 | 迁移到 prescription 模块 |
| `prescription/.../task/DraftContextCleanupTask.java` | 新建 | 从 consultation 迁入，包路径变更 |
| `prescription/.../task/DraftContextCleanupTaskTest.java` | 新建 | 从 consultation 迁入，包路径适配 |
| `prescription/.../service/assist/impl/PrescriptionAssistServiceImplTest.java` | 修改 | 移除 DrugFacade 相关测试 + Mock 字段 + 构造器签名变更 |
| `prescription/.../service/audit/impl/PrescriptionAuditServiceImplTest.java` | 修改 | 移除 DrugFacade 相关测试 + Mock 字段 + 构造器签名变更 |

## 类型定义

### P14: PrescriptionAssistServiceImpl 修改
**形态**：class（已有）
**包路径**：`com.aimedical.modules.prescription.service.assist.impl`
**变更**：

#### 1. 新增私有方法 `clearCriticalAlerts`

```java
private void clearCriticalAlerts(String prescriptionId) {
    prescriptionDraftContext.updateCriticalAlerts(prescriptionId, Collections.emptyList());
}
```

**语义**：封装清除旧 CRITICAL 告警逻辑。`updateCriticalAlerts(prescriptionId, emptyList())` 内部调用 `draftContextStore.remove(key)` 移除该 prescriptionId 下的 CRITICAL 告警条目，与 OOD §4.4 "无 CRITICAL 时清除对应条目"语义一致。

#### 2. assist() 方法失败/降级路径插入 clearCriticalAlerts 调用

5 个调用点（每个在 return 前调用）：

| 路径 | 代码位置 | 修改 |
|------|---------|------|
| catch InterruptedException | line 98-100 | `clearCriticalAlerts(request.getPrescriptionId());` 插入在 `return buildEmptyResponse(...)` 前 |
| catch ExecutionException | line 101-102 | `clearCriticalAlerts(request.getPrescriptionId());` 插入在 `return buildEmptyResponse(...)` 前 |
| catch TimeoutException | line 103-104 | `clearCriticalAlerts(request.getPrescriptionId());` 插入在 `return buildEmptyResponse(...)` 前 |
| AI 返回空结果（aiData == null \|\| !aiResult.isSuccess()） | line 113-114 | `clearCriticalAlerts(request.getPrescriptionId());` 插入在 `return buildEmptyResponse(...)` 前 |
| AI 返回无可推荐药品（!hasDrugs） | line 118-122 | `clearCriticalAlerts(request.getPrescriptionId());` 插入在 `return response` 前 |

**设计决策**：
- `buildEmptyResponse()` 保持纯构建方法语义，不修改
- 5 个路径统一调用 `clearCriticalAlerts()` 避免重复代码
- AI 不可用时不写入伪 CRITICAL 告警（OOD 未要求），仅清除可能残留的旧告警
- 正常 check-dose 路径（line 168）已调用 `updateCriticalAlerts(prescriptionId, criticalContextAlerts)`，不受影响

### 移除 enrichWithDrugInfo 死代码：PrescriptionAssistServiceImpl 修改
**形态**：class（已有）
**包路径**：`com.aimedical.modules.prescription.service.assist.impl`
**变更**：

1. 移除 `enrichWithDrugInfo(List<PrescriptionItem>)` 方法定义（line 230-243）
2. 移除 `enrichWithDrugInfo(items)` 调用（line 132）
3. 移除构造器参数 `DrugFacade drugFacade` 和 `@Value("${prescription.drug-facade.timeout:2}") long drugFacadeTimeout`
4. 移除字段 `private final DrugFacade drugFacade` 和 `private final long drugFacadeTimeout`
5. 移除字段赋值 `this.drugFacade = drugFacade` 和 `this.drugFacadeTimeout = drugFacadeTimeout`
6. 移除 import `com.aimedical.modules.commonmodule.drug.DrugFacade` 和 `com.aimedical.modules.commonmodule.drug.DrugInfo`

**修改后构造器签名**：

```java
public PrescriptionAssistServiceImpl(AiService aiService,
                                      AssistConverter assistConverter,
                                      AllergyCheckRule allergyCheckRule,
                                      DosageThresholdService dosageThresholdService,
                                      PrescriptionDraftContext prescriptionDraftContext,
                                      DedupTaskScheduler dedupTaskScheduler,
                                      SuggestionStore suggestionStore,
                                      ObjectMapper objectMapper,
                                      @Value("${ai.timeout.prescription-assist:8}") long aiTimeout)
```

**修改后字段列表**：

```java
private final AiService aiService;
private final AssistConverter assistConverter;
private final AllergyCheckRule allergyCheckRule;
private final DosageThresholdService dosageThresholdService;
private final PrescriptionDraftContext prescriptionDraftContext;
private final DedupTaskScheduler dedupTaskScheduler;
private final SuggestionStore suggestionStore;
private final ObjectMapper objectMapper;
private final long aiTimeout;
```

### 移除 enrichWithDrugInfo 死代码：PrescriptionAuditServiceImpl 修改
**形态**：class（已有）
**包路径**：`com.aimedical.modules.prescription.service.audit.impl`
**变更**：

1. 移除 `enrichWithDrugInfo(AuditRequest)` 方法定义（line 518-532）
2. 移除 `enrichWithDrugInfo(request)` 调用（line 147）
3. 移除构造器参数 `DrugFacade drugFacade` 和 `@Value("${prescription.drug-facade.timeout:2}") long drugFacadeTimeout`
4. 移除字段 `private final DrugFacade drugFacade` 和 `private final long drugFacadeTimeout`
5. 移除字段赋值 `this.drugFacade = drugFacade` 和 `this.drugFacadeTimeout = drugFacadeTimeout`
6. 移除 import `com.aimedical.modules.commonmodule.drug.DrugFacade` 和 `com.aimedical.modules.commonmodule.drug.DrugInfo`

**修改后构造器签名**：

```java
public PrescriptionAuditServiceImpl(AiService aiService, LocalRuleEngine localRuleEngine,
                                     AuditRecordRepository auditRecordRepository,
                                     AuditConverter auditConverter,
                                     PrescriptionDraftContext prescriptionDraftContext,
                                     CurrentUser currentUser,
                                     ObjectMapper objectMapper,
                                     @Value("${ai.timeout.prescription-audit:6}") long aiTimeout)
```

**修改后字段列表**：

```java
private final AiService aiService;
private final LocalRuleEngine localRuleEngine;
private final AuditRecordRepository auditRecordRepository;
private final AuditConverter auditConverter;
private final PrescriptionDraftContext prescriptionDraftContext;
private final CurrentUser currentUser;
private final ObjectMapper objectMapper;
private final long aiTimeout;
```

### DraftContextCleanupTask 迁移：新建文件
**形态**：class（新建，从 consultation 模块迁入）
**包路径**：`com.aimedical.modules.prescription.task`
**职责**：定时清理 DraftContextStore 中 TTL 过期条目

```java
package com.aimedical.modules.prescription.task;

import com.aimedical.modules.commonmodule.store.DraftContextStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DraftContextCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(DraftContextCleanupTask.class);
    private static final long TTL_MINUTES = 60;

    private final DraftContextStore draftContextStore;
    private final ConcurrentHashMap<String, Instant> writeTimestamps;

    public DraftContextCleanupTask(DraftContextStore draftContextStore) {
        this.draftContextStore = draftContextStore;
        this.writeTimestamps = new ConcurrentHashMap<>();
    }

    public void recordWrite(String key, Instant timestamp) {
        writeTimestamps.put(key, timestamp);
    }

    public void removeTimestamp(String key) {
        writeTimestamps.remove(key);
    }

    @Scheduled(cron = "0 0/5 * * * ?")
    public void cleanupExpiredDrafts() {
        Instant now = Instant.now();
        for (String key : new ArrayList<>(draftContextStore.keySet())) {
            Instant ts = writeTimestamps.get(key);
            if (ts != null && ts.plusSeconds(TTL_MINUTES * 60).isBefore(now)) {
                draftContextStore.remove(key);
                writeTimestamps.remove(key);
                log.info("Removed expired draft context: {}", key);
            }
        }
    }
}
```

**与原文件差异**：仅包声明从 `com.aimedical.modules.consultation.task` 改为 `com.aimedical.modules.prescription.task`，其余代码完全一致。

**Spring bean 注册**：`@Component` 注解 + 组件扫描，迁移后注入 `DraftContextStore`（T3 已创建独立 `DraftContextStoreImpl` bean），自动获得该 bean。consultation 模块删除原文件后，不再有该 bean 的重复定义。

### DraftContextCleanupTaskTest 迁移：新建文件
**形态**：class（新建，从 consultation 模块迁入）
**包路径**：`com.aimedical.modules.prescription.task`
**职责**：DraftContextCleanupTask 单元测试

```java
package com.aimedical.modules.prescription.task;

import com.aimedical.modules.commonmodule.store.DraftContextStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DraftContextCleanupTaskTest {

    private StubDraftContextStore store;
    private DraftContextCleanupTask task;

    @BeforeEach
    void setUp() {
        store = new StubDraftContextStore();
        task = new DraftContextCleanupTask(store);
    }

    // 9 个测试用例与原文件完全一致：
    // shouldRecordWriteTimestamp, shouldRemoveExpiredDraft, shouldKeepNonExpiredDraft,
    // shouldRemoveTimestampWithEntry, shouldNotRemoveEntryWithoutTimestamp,
    // shouldHandleEmptyStore, shouldRemoveOnlyExpiredEntries,
    // shouldRemoveTimestampAfterCleanup, removeTimestampShouldRemoveTracking

    private static class StubDraftContextStore implements DraftContextStore {
        private final Map<String, Object> map = new HashMap<>();

        @Override
        public Object get(String key) { return map.get(key); }

        @Override
        public void put(String key, Object value) { map.put(key, value); }

        @Override
        public Object remove(String key) { return map.remove(key); }

        @Override
        public boolean containsKey(String key) { return map.containsKey(key); }

        @Override
        public Set<String> keySet() { return map.keySet(); }
    }
}
```

**与原文件差异**：仅包声明从 `com.aimedical.modules.consultation.task` 改为 `com.aimedical.modules.prescription.task`，其余代码完全一致。

## 错误处理

### P14 clearCriticalAlerts
- `clearCriticalAlerts` 内部调用 `prescriptionDraftContext.updateCriticalAlerts(prescriptionId, Collections.emptyList())`，该方法内部调用 `draftContextStore.remove(key)`。`DraftContextStore.remove(key)` 不抛异常（`ConcurrentHashMap.remove` 不抛 checked exception）。
- `prescriptionId` 在 `clearCriticalAlerts` 调用时已确保非空（`assist()` 方法 line 87-89 已处理 null/blank）。

### DrugFacade 移除后无遗留风险
- `DrugFacade` 接口定义保留在 common-module-api 中（不删除接口本身），仅移除 prescription 模块中的注入和调用
- `enrichWithDrugInfo` 是唯一使用 `DrugFacade` 的方法，移除后无其他调用点
- `drugFacadeTimeout` 配置项 `prescription.drug-facade.timeout` 在 application.yml 中可保留（Spring 不报错），但建议后续清理

### DraftContextCleanupTask 迁移 bean 冲突
- consultation 模块原文件删除后，`@Component` 注解的 `DraftContextCleanupTask` 仅存在于 prescription 模块，无 bean 重复定义风险
- `DraftContextStoreImpl`（T3 新建，`@Service` 注解）是唯一的 `DraftContextStore` 实现 bean，`DraftContextCleanupTask` 和 `PrescriptionDraftContext` 均通过构造器注入 `DraftContextStore` 接口获得该 bean

## 行为契约

### P14: assist() CRITICAL 告警清除契约

**前置条件**：`prescriptionId` 非 null 非 blank（line 87-89 保证）

**清除路径矩阵**：

| assist() 执行路径 | clearCriticalAlerts 调用 | 语义 |
|---|---|---|
| catch InterruptedException | 调用 | AI 中断，清除旧 CRITICAL 告警 |
| catch ExecutionException | 调用 | AI 执行异常，清除旧 CRITICAL 告警 |
| catch TimeoutException | 调用 | AI 超时，清除旧 CRITICAL 告警 |
| aiData == null \|\| !aiResult.isSuccess() | 调用 | AI 返回空/失败结果，清除旧 CRITICAL 告警 |
| !hasDrugs（AI 无可推荐药品） | 调用 | AI 有效结果但无药品，本地校验结果为空，清除旧 CRITICAL 告警 |
| 正常路径（有药品 + check-dose） | 不调用（已由 line 168 updateCriticalAlerts 处理） | 正常路径写入实际 CRITICAL 告警 |

**后置条件**：
- AI 失败/降级路径返回前，该 prescriptionId 下不存在残留的 CRITICAL 告警条目
- 提交端点步① `hasCriticalAlerts(prescriptionId)` 不会因旧告警残留而错误阻断
- 不引入"AI 不可用"伪 CRITICAL 告警，保持 PrescriptionDraftContext 语义与 OOD 一致

### clearCriticalAlerts 方法契约
- **前置**：`prescriptionId` 非 null
- **后置**：`prescriptionDraftContext.getCriticalAlerts(prescriptionId)` 返回 `Collections.emptyList()`
- **幂等性**：重复调用安全（`remove` 对不存在的 key 无副作用）

### PrescriptionAssistServiceImpl 构造器变更契约
- 移除 `DrugFacade drugFacade` 和 `long drugFacadeTimeout` 参数后，构造器参数从 11 个减少到 9 个
- Spring 自动注入时按类型匹配，`DrugFacade` bean 不再被注入到 `PrescriptionAssistServiceImpl`

### PrescriptionAuditServiceImpl 构造器变更契约
- 移除 `DrugFacade drugFacade` 和 `long drugFacadeTimeout` 参数后，构造器参数从 10 个减少到 8 个
- Spring 自动注入时按类型匹配，`DrugFacade` bean 不再被注入到 `PrescriptionAuditServiceImpl`

### DraftContextCleanupTask 迁移契约
- 迁移后行为与原文件完全一致（仅包路径变更）
- `@Component` 注解确保 Spring 组件扫描注册为 bean
- `@Scheduled(cron = "0 0/5 * * * ?")` 定时任务与原配置一致
- 清理逻辑对所有键格式一视同仁，`prescriptionId+:criticalAlerts` 格式的键无需特殊处理

## 依赖关系

### 移除的依赖
- `PrescriptionAssistServiceImpl` → `DrugFacade`、`DrugInfo`（移除注入和 import）
- `PrescriptionAuditServiceImpl` → `DrugFacade`、`DrugInfo`（移除注入和 import）

### 新增的依赖
- `prescription/.../task/DraftContextCleanupTask` → `com.aimedical.modules.commonmodule.store.DraftContextStore`（从 consultation 迁入，依赖不变）

### 移除的模块间依赖
- `consultation` 模块不再依赖 `commonmodule.store.DraftContextStore`（通过 `DraftContextCleanupTask` 删除实现）

### 暴露给后续任务的接口
- `PrescriptionAssistServiceImpl.clearCriticalAlerts(String prescriptionId)` — 新增私有方法，不暴露
- `PrescriptionAssistServiceImpl` 构造器签名变更 — 影响所有实例化点（Spring 自动注入 + 测试）
- `PrescriptionAuditServiceImpl` 构造器签名变更 — 影响所有实例化点（Spring 自动注入 + 测试）
