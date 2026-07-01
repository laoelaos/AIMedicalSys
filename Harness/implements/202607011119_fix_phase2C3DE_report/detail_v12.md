# 详细设计（v12）

## 概述

修复 diagnosis 报告剩余的 17 项 P1/P2 缺陷（9a-9q），涉及 medical-record、ai、consultation、prescription、common-module 五个模块。按执行次序：9e → 9b（依赖），其余并行。

## 文件规划

| 文件路径（相对 `AIMedical/backend/modules/`） | 操作 | 子项 |
|------|------|:----:|
| `medical-record/.../template/DatabaseTemplateConfigManager.java` | 修改 | 9a, 9e |
| `medical-record/.../converter/MedicalRecordConverter.java` | 修改 | 9b, 9d, 9e |
| `medical-record/.../service/impl/MedicalRecordServiceImpl.java` | 修改 | 9c, 9h |
| `medical-record/.../converter/MedicalRecordContentConverter.java` | 修改 | 9f |
| `medical-record/.../entity/MedicalRecord.java` | 修改 | 9g |
| `common-module/common-module-api/.../store/impl/DraftContextStoreImpl.java` | 修改 | 9i |
| `common-module/common-module-api/.../store/DraftContextStore.java` | 修改（接口新增方法） | 9i |
| `ai/ai-impl/.../mock/MockAiService.java` | 修改 | 9l |
| `ai/ai-impl/.../fallback/FallbackAiService.java` | 修改 | 9j, 9m |
| `ai/ai-api/.../DegradationContext.java` | 参考（无需变更） | 9j |
| `consultation/.../converter/TriageConverter.java` | 参考（无需修改） | 9n |
| `consultation/.../service/impl/TriageServiceImpl.java` | 修改 | 9n, 9p |
| `consultation/.../entity/DialogueSession.java` | 修改 | 9n |
| `prescription/.../task/DraftContextCleanupTask.java` | 修改 | 9o |
| `prescription/.../service/audit/impl/PrescriptionAuditServiceImpl.java` | 修改 | 9k, 9q |
| `common/.../store/impl/ConcurrentHashMapStore.java` | 修改 | 9p |
| `medical-record/.../exception/MedicalRecordErrorCode.java` | 修改 | 9c |

## 类型定义

### 9a — DatabaseTemplateConfigManager.handleTemplateConfigChange

**形态**：方法体修改
**文件路径**：`medical-record/.../template/DatabaseTemplateConfigManager.java:105-108`
**变更**：
```java
@EventListener
public void handleTemplateConfigChange(TemplateConfigChangeEvent event) {
    String departmentCode = event.getDepartmentCode();
    if (departmentCode != null) {
        templateCache.invalidate(departmentCode);
    } else {
        templateCache.invalidateAll();
    }
}
```
- `TemplateConfigChangeEvent.getDepartmentCode()` 是已有方法（外部事件 DTO），无需新增
- 无需新增 import（Caffeine Cache API 已通过 `invalidate`/`invalidateAll` 覆盖）

### 9e — MedicalRecordConverter.toFieldsMap 排除元数据字段

**形态**：方法体修改
**文件路径**：`medical-record/.../converter/MedicalRecordConverter.java:29-47`
**变更**：`toFieldsMap` 写入 map 时跳过 `MedicalRecordField.MISSING_FIELDS` 和 `MedicalRecordField.PARTIAL_CONTENT`：
```java
public Map<MedicalRecordField, String> toFieldsMap(MedicalRecordGenResponse aiResponse) {
    Map<MedicalRecordField, String> map = new HashMap<>();
    map.put(MedicalRecordField.CHIEF_COMPLAINT, aiResponse.getChiefComplaint());
    map.put(MedicalRecordField.SYMPTOM_DESCRIPTION, aiResponse.getSymptomDescription());
    map.put(MedicalRecordField.PRESENT_ILLNESS, aiResponse.getPresentIllness());
    map.put(MedicalRecordField.PAST_HISTORY, aiResponse.getPastHistory());
    map.put(MedicalRecordField.PHYSICAL_EXAM, aiResponse.getPhysicalExam());
    map.put(MedicalRecordField.PRELIMINARY_DIAGNOSIS, aiResponse.getPreliminaryDiagnosis());
    map.put(MedicalRecordField.TREATMENT_PLAN, aiResponse.getTreatmentPlan());
    return map;
}
```
- 删除第 38-45 行（MISSING_FIELDS/PARTIAL_CONTENT 写入逻辑）
- 无需新增 import（原 import 已覆盖）

### 9e — DatabaseTemplateConfigManager.createDefaultTemplate 排除元数据字段

**形态**：静态方法修改
**文件路径**：`medical-record/.../template/DatabaseTemplateConfigManager.java:110-119`
**变更**：`createDefaultTemplate` 的 `allFields` 排除 `MISSING_FIELDS` 和 `PARTIAL_CONTENT`：
```java
private static DepartmentTemplateConfig createDefaultTemplate() {
    Set<MedicalRecordField> allFields = Arrays.stream(MedicalRecordField.values())
            .filter(f -> f != MedicalRecordField.MISSING_FIELDS && f != MedicalRecordField.PARTIAL_CONTENT)
            .collect(Collectors.toSet());
    // ... 其余不变
}
```
- 新增 import：无需（`Arrays`、`Collectors` 已存在）

### 9b — MedicalRecordConverter.toRecordGenerateResponse 移除 timing 依赖（无代码变更）

**说明**：9b 要求 "从 content_map 写入逻辑中排除 MISSING_FIELDS/PARTIAL_CONTENT"。该变更在 9e 中已通过删除 `toFieldsMap` 中这两个字段的写入完成。**9b 无独立代码变更**，完全由 9e 覆盖。

### 9c — MedicalRecordServiceImpl.callAiWithTimeout + MedicalRecordErrorCode 新增枚举值

**形态**：枚举新增 2 项 + 方法体修改
**文件路径**：
1. `medical-record/.../exception/MedicalRecordErrorCode.java`：新增两个枚举常量
2. `medical-record/.../service/impl/MedicalRecordServiceImpl.java:152-165`

**MedicalRecordErrorCode 新增**：
```java
MR_GEN_AI_INTERRUPTED("MR_GEN_AI_INTERRUPTED", "AI 病历生成被中断"),
MR_GEN_AI_EXECUTION_ERROR("MR_GEN_AI_EXECUTION_ERROR", "AI 病历生成执行异常"),
```
- 插入位置：`MR_GEN_AI_TIMEOUT` 之后（第 8-9 行位置），共 10 个常量

**callAiWithTimeout 变更**：
```java
private AiResult<MedicalRecordGenResponse> callAiWithTimeout(MedicalRecordGenRequest aiRequest) {
    CompletableFuture<AiResult<MedicalRecordGenResponse>> future = aiService.generateMedicalRecord(aiRequest);
    try {
        return future.get(aiTimeout, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
        log.warn("AI medical record generation timeout");
        return AiResultFactory.degraded("AI medical record generation timeout", "MR_GEN_AI_TIMEOUT", null);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return AiResultFactory.degraded("AI medical record generation interrupted", "MR_GEN_AI_INTERRUPTED", null);
    } catch (ExecutionException e) {
        log.warn("AI medical record generation execution error", e);
        return AiResultFactory.degraded(e.getCause() != null ? e.getCause().getMessage() : "AI medical record generation execution error", "MR_GEN_AI_EXECUTION_ERROR", null);
    }
}
```
- InterruptedException: 保留 `Thread.currentThread().interrupt()`，错误码改为 `MR_GEN_AI_INTERRUPTED`
- ExecutionException: 保留 `log.warn`，使用 `e.getCause().getMessage()` 作为 message，错误码改为 `MR_GEN_AI_EXECUTION_ERROR`
- 无需新增 import

### 9d — MedicalRecordConverter 字面字符串 → 枚举

**形态**：两处字面字符串替换
**文件路径**：`medical-record/.../converter/MedicalRecordConverter.java:69,73`
**变更**：
- 第 69 行：`"MR_GEN_AI_TIMEOUT".equals(aiResult.getErrorCode())` → `MedicalRecordErrorCode.MR_GEN_AI_TIMEOUT.name().equals(aiResult.getErrorCode())`
- 第 73 行：`"MR_GEN_AI_TIMEOUT".equals(aiResult.getErrorCode())` → `MedicalRecordErrorCode.MR_GEN_AI_TIMEOUT.name().equals(aiResult.getErrorCode())`
- `MedicalRecordErrorCode` import 已存在（第 18 行）

### 9f — MedicalRecordContentConverter 新增 WARN 日志

**形态**：新增 Logger 字段 + catch 块追加日志
**文件路径**：`medical-record/.../converter/MedicalRecordContentConverter.java`
**变更**：
- 新增字段：`private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MedicalRecordContentConverter.class);`
- 第 29 行 catch 追加：`log.warn("MedicalRecordContentConverter serialization failed", e);`
- 第 44 行 catch 追加：`log.warn("MedicalRecordContentConverter deserialization failed", e);`
- import 新增：`import org.slf4j.Logger;` 和 `import org.slf4j.LoggerFactory;`

### 9g — MedicalRecord.@PrePersist 设置 updatedAt

**形态**：单行追加
**文件路径**：`medical-record/.../entity/MedicalRecord.java:133-136`
**变更**：
```java
@PrePersist
public void prePersist() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
}
```

### 9h — MedicalRecordServiceImpl 新增 ExecutorService

**形态**：构造参数新增 + 配置类新增 + supplyAsync 绑定
**文件路径**：
1. `medical-record/.../service/impl/MedicalRecordServiceImpl.java`

**变更**：
- 新增字段：`private final java.util.concurrent.ExecutorService medicalRecordExecutor;`
- 构造函数新增参数 `ExecutorService medicalRecordExecutor`（最后一个参数，放在 `MedicalRecordRepository` 之后）
- 第 139 行 `CompletableFuture.supplyAsync(...)` → `CompletableFuture.supplyAsync(..., medicalRecordExecutor)`
- 无需新增 import（`ExecutorService` 在 `java.util.concurrent` 包，已通过通配符 `java.util.concurrent.*` 覆盖）

**新增配置类**：`medical-record/.../config/MedicalRecordThreadPoolConfig.java`
```java
package com.aimedical.modules.medicalrecord.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class MedicalRecordThreadPoolConfig {

    @Bean("medicalRecordExecutor")
    public ExecutorService medicalRecordExecutor() {
        return Executors.newCachedThreadPool();
    }
}
```
- 用途：为 `resolveVisitId` 的 `supplyAsync` 提供独立线程池，避免 ForkJoinPool.commonPool() 竞争
- 新建文件路径：`medical-record/src/main/java/com/aimedical/modules/medicalrecord/config/MedicalRecordThreadPoolConfig.java`

### 9i — DraftContextStore 接口新增 compute/createIfNotExists + DraftContextStoreImpl 实现

**形态**：接口新增方法声明 + 实现类新增方法实现
**文件路径**：
1. `common-module/common-module-api/.../store/DraftContextStore.java`
2. `common-module/common-module-api/.../store/impl/DraftContextStoreImpl.java`

**DraftContextStore 接口变更**：
```java
public interface DraftContextStore extends SessionStore<String, Object> {
    Object compute(String key, java.util.function.BiFunction<String, Object, Object> remappingFunction);
    Object createIfNotExists(String key, Object value);
}
```

**DraftContextStoreImpl 变更**：
```java
import java.util.function.BiFunction;

// 在现有方法后追加：
@Override
public Object compute(String key, BiFunction<String, Object, Object> remappingFunction) {
    return store.compute(key, remappingFunction);
}

@Override
public Object createIfNotExists(String key, Object value) {
    return store.putIfAbsent(key, value);
}
```

### 9j — FallbackAiService.applyStrategies 传递 DegradationContext

**形态**：方法签名变更 + 12 个 thenApply 调用点适配
**文件路径**：`ai/ai-impl/.../fallback/FallbackAiService.java`

**变更**：
1. `applyStrategies` 方法签名：
```java
private <T> AiResult<T> applyStrategies(AiResult<T> result, DegradationContext context) {
    if (result.isSuccess() || result.isDegraded()) {
        return result;
    }
    for (DegradationStrategy strategy : strategies) {
        if (strategy.shouldDegrade(context)) {
            return AiResult.degraded("Degraded by strategy");
        }
    }
    return result;
}
```

2. 所有 12 处 `.thenApply(this::applyStrategies)` 改为 `.thenApply(result -> applyStrategies(result, context))`：
   - triage:94-95
   - diagnosis:110-111
   - prescriptionCheck:126-127
   - generateMedicalRecord:142-143
   - analysisReportForInspection:158-159
   - analysisReportForLabTest:174-175
   - imageAnalysis:190-191
   - knowledgeBaseQuery:206-207
   - recommendExamination:222-223
   - prescriptionAssist:238-239
   - recommendExecutionOrder:254-255
   - schedule:270-271
   - discussionConclusion:286-287

### 9k — PrescriptionAuditServiceImpl.audit 追加 aiResult.getData() null 检查

**形态**：单行条件追加
**文件路径**：`prescription/.../service/audit/impl/PrescriptionAuditServiceImpl.java:119`
**变更**：
- 第 119 行：`if (aiResult != null && aiResult.isSuccess())` → `if (aiResult != null && aiResult.isSuccess() && aiResult.getData() != null)`
- 当 `aiResult.getData() == null` 时走 AI_UNAVAILABLE 降级路径（第 121-138 行）

### 9l — MockAiService.respond TIMEOUT 分支使用 failedFuture

**形态**：单行替换
**文件路径**：`ai/ai-impl/.../mock/MockAiService.java:67`
**变更**：
```java
case TIMEOUT:
    return CompletableFuture.failedFuture(new java.util.concurrent.TimeoutException("Mock timeout"));
```
- import 新增：需要追加 `import java.util.concurrent.TimeoutException;`（当前 import 为 `import java.util.concurrent.CompletableFuture;`，非通配符，TimeoutException 未被覆盖）

### 9n — TriageServiceImpl 移除重复 correctedChiefComplaint 设置 + DialogueSession 统一 synchronized

**形态**：删除 Service 中重复设置 + DialogueSession 替换 AtomicInteger 为 int+synchronized
**文件路径**：
1. `consultation/.../service/impl/TriageServiceImpl.java:157-159`
2. `consultation/.../entity/DialogueSession.java`

**TriageServiceImpl 变更**：
- 删除第 157-159 行（`if (aiData.getCorrectedChiefComplaint() != null) { session.setCorrectedChiefComplaint(aiData.getCorrectedChiefComplaint()); }`），保留 `TriageConverter.toTriageResponse` 中为唯一边界

**DialogueSession 变更**：
- `private AtomicInteger aiFailCount = new AtomicInteger(0);` → `private int aiFailCount = 0;`
- `private AtomicInteger roundCount = new AtomicInteger(0);` → `private int roundCount = 0;`
- `public int getAiFailCount()` → `public synchronized int getAiFailCount()`
- `public void setAiFailCount(int aiFailCount)` → `public synchronized void setAiFailCount(int aiFailCount)`
- `public int getRoundCount()` → `public synchronized int getRoundCount()`
- `public void setRoundCount(int roundCount)` → `public synchronized void setRoundCount(int roundCount)`
- 删除 import：`java.util.concurrent.atomic.AtomicInteger`

### 9o — DraftContextCleanupTask 使用 writeTimestamps.forEach 替代 keySet 迭代

**形态**：迭代基准变更
**文件路径**：`prescription/.../task/DraftContextCleanupTask.java:38-45`
**变更**：
```java
@Scheduled(cron = "0 0/5 * * * ?")
public void cleanupExpiredDrafts() {
    Instant now = Instant.now();
    writeTimestamps.forEach((key, ts) -> {
        if (ts != null && ts.plusSeconds(TTL_MINUTES * 60).isBefore(now)) {
            draftContextStore.remove(key);
            writeTimestamps.remove(key);
            log.info("Removed expired draft context: {}", key);
        }
    });
}
```
- 删除 `import java.util.ArrayList;`
- 迭代 `writeTimestamps` 而非 `draftContextStore.keySet()`

### 9p — A08 中文文案统一 + T24 @Service 注解

**A08 变更**：
- `consultation/.../service/impl/TriageServiceImpl.java:178`：文案已为中文，无需修改
- `medical-record/.../service/impl/MedicalRecordServiceImpl.java:157,160,163`：
  - 第 157 行 `"AI medical record generation timeout"` → `"AI 病历生成超时"`
  - 第 160 行 `"AI medical record generation interrupted"` → `"AI 病历生成被中断"`
  - 第 163 行 `"AI medical record generation execution error"` → `"AI 病历生成执行异常"`

**T24 变更**：
- `common-module/common-module-api/.../store/impl/ConcurrentHashMapStore.java:10` 类级别添加 `@Service` 注解：
```java
import org.springframework.stereotype.Service;

@Service
public class ConcurrentHashMapStore implements SuggestionStore {
```

### 9q — PrescriptionAuditServiceImpl 移除冗余 getData() null 检查

**形态**：删除条件中的冗余检查
**文件路径**：`prescription/.../service/audit/impl/PrescriptionAuditServiceImpl.java:410`
**变更**：
- 第 410 行：`} else if (aiResult != null && aiResult.isSuccess() && aiResult.getData() != null) {` → `} else if (aiResult != null && aiResult.isSuccess()) {`
- 理由：`AiResult.success(T data)` 工厂方法内部使用 `Objects.requireNonNull(data)`，因此正常创建的成功结果 data 不可能为 null；且 9k 已在 audit 入口处拦截 null data，此处不再需要重复检查

## 错误处理

| 子项 | 策略 |
|------|------|
| 9a | departmentCode null 时退化为 invalidateAll（安全兜底） |
| 9c | ExecutionException 提取 cause message，cause null 时用默认文案 |
| 9f | 异常时保留原行为（null/emptyMap），追加 WARN 日志 |
| 9j | applyStrategies 不处理已 degraded/success 的结果 |

## 行为契约

- 9e 修改后 `toFieldsMap` 返回的 map 仅含 7 个业务字段（不含 MISSING_FIELDS/PARTIAL_CONTENT）
- 9e 修改后 `DEFAULT_TEMPLATE.requiredFields` 大小为 7（原 9）
- 9c 新增 `MR_GEN_AI_INTERRUPTED` 和 `MR_GEN_AI_EXECUTION_ERROR` 枚举值，`MedicalRecordErrorCodeTest.shouldHaveEightConstants` 需改为 10
- 9h 新增的 `MedicalRecordThreadPoolConfig` 作为独立配置类，不影响现有 bean 扫描
- 9n `DialogueSession.aiFailCount`/`roundCount` 的 synchronized getter/setter 语义等价于 AtomicInteger

## 测试变更

| 子项 | 测试文件 | 变更内容 |
|:----:|---------|----------|
| 9a+9e | `DatabaseTemplateConfigManagerTest` | `shouldReturnDefaultTemplateWhenDepartmentNotFound` 断言 `assertEquals(9, ...)` → `assertEquals(7, ...)`（排除 MISSING_FIELDS/PARTIAL_CONTENT）；新增 `shouldInvalidateByDepartmentCode` 验证 invalidate(key) 和 invalidateAll() 分支 |
| 9b+9e | `MedicalRecordConverterTest` | `toFieldsMapShouldMapAllNineFields` / `toFieldsMapShouldPreserveNullValues` / `toRecordGenerateResponseShouldBuildResponseFromAiResult` 断言 `assertEquals(9, ...)` → `assertEquals(7, ...)` |
| 9c | `MedicalRecordServiceImplTest` | `shouldReturnDegradedWhenAiTimesOut` 无需修改；新增 `shouldReturnInterruptedOnInterruptedException` 验证 `MR_GEN_AI_INTERRUPTED`；新增 `shouldReturnExecutionErrorOnExecutionException` 验证 `MR_GEN_AI_EXECUTION_ERROR` |
| 9c | `MedicalRecordErrorCodeTest` | `shouldHaveEightConstants` → `assertEquals(10, ...)`；新增两个枚举的 code/message 断言 |
| 9d | `MedicalRecordConverterTest` | 现有断言不变（`.name()` 与字面字符串等价） |
| 9f | `MedicalRecordContentConverterTest` | 新增 `shouldLogWarnOnSerializationFailure`；新增 `shouldLogWarnOnDeserializationFailure` |
| 9g | `MedicalRecordTest` | 新增 `prePersistShouldSetUpdatedAt`：调用 `prePersist()` 后断言 `updatedAt != null` |
| 9h | `MedicalRecordServiceImplTest` | `setUp()` 构造函数从 6 参数变为 7 参数（新增 `ExecutorService`）；新增 `@Mock ExecutorService medicalRecordExecutor` |
| 9i | `DraftContextCleanupTaskTest` | `StubDraftContextStore` 新增 `compute`/`createIfNotExists` 实现 |
| 9j+9m | `FallbackAiServiceTest` | `applyStrategies` 调用改为传入 `DegradationContext` 参数；若通过反射调用私有方法则适配新签名 |
| 9k | `PrescriptionAuditServiceImplTest` | 新增 `auditShouldHandleAiResultDataNull` 验证 null data 走降级路径；新增 `auditShouldPassThroughWhenAiResultDataIsNotNull` 验证正常 data 路径 |
| 9l | `MockAiServiceTest` | `timeoutStrategyShouldTimeout`：`assertFalse(future.isDone())` → `assertTrue(future.isDone())`；`assertThrows(TimeoutException.class, ...)` → `assertThrows(ExecutionException.class, ...)` |
| 9n | `DialogueSessionTest` | 无需修改（AtomicInteger → synchronized int 行为等价） |
| 9o | `DraftContextCleanupTaskTest` | 无需修改（迭代基准变更，行为等价） |
| 9p | `ConcurrentHashMapStoreTest` | 新增 `shouldBeAnnotatedWithService` 验证 `@Service` 注解存在 |
| 9q | `PrescriptionAuditServiceImplTest` | 无需修改（移除冗余 null 检查，行为等价） |

## 依赖关系

| 依赖 | 说明 |
|------|------|
| `java.util.concurrent.TimeoutException` | 9l MockAiService 新增 import |
| `org.slf4j.Logger` / `LoggerFactory` | 9f MedicalRecordContentConverter 新增 import |
| `org.springframework.stereotype.Service` | 9p ConcurrentHashMapStore 新增 import |
| `java.util.function.BiFunction` | 9i DraftContextStoreImpl 新增 import |
| `java.util.concurrent.ExecutorService` | 9h 已通过通配符 import 覆盖 |

## 修订说明（v12 r3）
| 审查意见 | 修改措施 |
|---------|---------|
| **[一般] 9l import 新增列自相矛盾** | 将 9l 条目中 `import 新增：无需` 改为 `需要追加 import java.util.concurrent.TimeoutException;`，删除矛盾的注意段落。 |
| **[一般] 9q 移除 null 检查的技术理由错误** | 修正理由：`AiResult.success(T data)` 工厂方法内部使用 `Objects.requireNonNull(data)`，且 9k 已在入口拦截 null data，此处不再需要重复检查。 |
| **[轻微] 9h 用途描述与实际修改范围不完全对应** | 将用途描述从"为 callAiWithTimeout 和 resolveVisitId 提供独立线程池"修正为"为 resolveVisitId 的 supplyAsync 提供独立线程池"。 |

## 修订说明（v12 r2）
| 审查意见 | 修改措施 |
|---------|---------|
| **[一般] 9p：interrupted 消息中文文案未明确指定** | 将 9p 的 `"AI medical record generation interrupted" → 保留或改为中文` 明确为 `"AI medical record generation interrupted" → "AI 病历生成被中断"`。 |
| **[一般] 文件规划表 TriageConverter.java 标记与描述矛盾** | TriageConverter.java 标记从 `"修改"` 改为 `"参考（无需修改）"`，因 9n 保留 Converter 为唯一边界，不涉及代码改动。 |
| **[轻微] 文件规划表 DegradationContext.java 标记为"修改"但无实际变更** | DegradationContext.java 标记从 `"修改"` 改为 `"参考（无需变更）"`，因 9j 仅修改 FallbackAiService 的方法签名和调用点，DegradationContext 构造函数保持兼容无需改动。 |

## 修订说明（v12 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| **[严重]** 9i 文件路径在文件规划表与类型定义中不一致 | 修正文件规划表 9i 行：`medical-record/.../store/` → `common-module/common-module-api/.../store/`。DraftContextStore 接口和实现均在 `common-module` 模块。 |
| **[一般]** 文件规划表列了 AuditConverter.java（9k）但无对应变更描述 | 移除 AuditConverter.java 条目。经核实 AuditConverter.toAuditResponse 已是安全兜底（aiData==null → PASS+空列表），问题根因在调用方 PrescriptionAuditServiceImpl 未拦截 null data，该文件无需修改。 |
| **[一般]** 设计未覆盖测试变更 | 新增"测试变更"章节，汇总各子项的测试断言适配和新测试用例。 |
