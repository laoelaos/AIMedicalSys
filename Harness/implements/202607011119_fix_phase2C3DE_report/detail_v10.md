# 详细设计（v10）

## 概述

修复 prescription 模块 14 项 P1/P2 缺陷（8a-8n），涉及 11 个源文件和 2 个 ai-api DTO 文件。均为单文件/单方法小改动，不涉及新增 API 或架构变更。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `prescription/.../service/assist/DosageThresholdService.java` | 修改 | 8a: 空 catch 补充 log.warn；8k: matchByPriority 循环抽取辅助方法 |
| `prescription/.../context/PrescriptionDraftContext.java` | 修改 | 8b: 删除 @SuppressWarnings + instanceof List<?>；8n: 新增 snapshotCriticalAlerts + SnapshotResult |
| `prescription/.../dto/audit/PrescriptionItem.java` | 修改 | 8c: dose double → BigDecimal |
| `prescription/.../rule/DosageLimitRule.java` | 修改 | 8c: dose 级联 BigDecimal.valueOf → 直接引用；8f: findBestMatch null 回退追加 log.warn |
| `prescription/.../converter/AuditConverter.java` | 修改 | 8c: aiItem.setDose(item.getDose().doubleValue())；8h: 追加 unit/weight 映射 |
| `prescription/.../rule/AllergyCheckRule.java` | 修改 | 8g: contains → 单词边界 regex + 否定前缀跳过 |
| `prescription/.../rule/DrugInteractionRule.java` | 修改 | 8i: 新增 @ConditionalOnProperty |
| `prescription/.../service/impl/PrescriptionAuditServiceImpl.java` | 修改 | 8d: UUID ID 生成；8e: 删除 hasNewAlerts 死代码块；8j: reasons 透传；8m: per-prescriptionId 锁；8n: doSubmit 中使用 snapshotCriticalAlerts |
| `prescription/.../api/PrescriptionAuditController.java` | 修改 | 8j: BLOCK 路径 reasons 从 response.getAlerts() 提取 |
| `prescription/.../service/assist/impl/PrescriptionAssistServiceImpl.java` | 修改 | 8l: exceptionally 回调追加 clearCriticalAlerts |
| `ai/ai-api/.../dto/prescription/PrescriptionCheckItem.java` | 修改 | 8h: 新增 unit 字段 + getter/setter |
| `ai/ai-api/.../dto/prescription/PatientInfo.java` | 修改 | 8h: 新增 weight 字段 + getter/setter |

## 类型定义

### 8a/8k. DosageThresholdService

**形态**：已有类，方法级变更
**包路径**：`com.aimedical.modules.prescription.service.assist.DosageThresholdService`
**变更1（8a）**：catch (NumberFormatException e) 块补充 `log.warn("Non-numeric frequency: {}, skipping daily dose check", request.getFrequency())`
- 新增字段：`private static final Logger log = LoggerFactory.getLogger(DosageThresholdService.class);`
- 新增 import：`org.slf4j.Logger` + `org.slf4j.LoggerFactory`
- 不影响控制流，catch 后自然跳过日剂量逻辑

**变更2（8k）**：抽取两个私有辅助方法
- `matchesBothRanges(DosageStandard ds, Integer age, BigDecimal weight): boolean`
  - 合并 L104-106 与 L116-118 的共同条件：age != null && weight != null && isInRange(age, ...) && isInRange(weight, ...)
- `findFirstCandidate(List<DosageStandard> candidates, Predicate<DosageStandard> predicate): DosageStandard`
  - 通用循环遍历模式：for-each 遍历，predicate.test(ds) 为 true 则 return ds，否则 return null
- 5 个循环全部替换为 `findFirstCandidate(candidates, ds -> ...)` 调用
- 新增 import：`java.util.function.Predicate`
- 匹配优先级顺序保持不变：Loop1（精确年龄+精确体重）→ Loop2（年龄范围+体重范围）→ Loop3（年龄范围+体重null）→ Loop4（年龄null+体重范围）→ Loop5（年龄null+体重null）

### 8b/8n. PrescriptionDraftContext

**形态**：已有类，方法级变更
**包路径**：`com.aimedical.modules.prescription.context.PrescriptionDraftContext`

**变更1（8b）**：getCriticalAlerts 方法
- 删除 `@SuppressWarnings("unchecked")`
- `value instanceof List` → `value instanceof List<?>`
- 删除 imports 中的非必要项（无需新 import，`List<?>` 已是泛型通配符语法）

**变更2（8n）**：新增方法及内部类
- `snapshotCriticalAlerts(String prescriptionId): SnapshotResult`
  - 单次读取 draftContextStore.get(key)，一次返回 hasAlerts + alerts
  - 内部使用 `value instanceof List<?>` + `(List<DosageAlert>) value` 转换
  - 无需 @SuppressWarnings（已有 instanceof 保护）

- 内部类 `SnapshotResult`（public static）：
  ```java
  public static class SnapshotResult {
      public final boolean hasAlerts;
      public final List<DosageAlert> alerts;
      public SnapshotResult(boolean hasAlerts, List<DosageAlert> alerts) { ... }
  }
  ```
  - 不可变 public final 字段，无需 getter

- **删除** `hasCriticalAlerts(String prescriptionId)` 方法（仅 submit() 引用，8n 合并为 snapshotCriticalAlerts）
- **保留** `getCriticalAlerts(String prescriptionId)`（其他调用方继续使用）

### 8c. PrescriptionItem

**形态**：已有 DTO，字段类型变更
**包路径**：`com.aimedical.modules.prescription.dto.audit.PrescriptionItem`
**变更**：
- `private double dose` → `private BigDecimal dose`
- `public double getDose()` → `public BigDecimal getDose()`
- `public void setDose(double dose)` → `public void setDose(BigDecimal dose)`
- 新增 import：`import java.math.BigDecimal;`

**级联影响**：
1. `DosageLimitRule.java:40` `BigDecimal dose = BigDecimal.valueOf(item.getDose())` → `BigDecimal dose = item.getDose()`（无需 BigDecimal.valueOf，已是 BigDecimal）
2. `AuditConverter.java:70` `aiItem.setDose(item.getDose())` → `aiItem.setDose(item.getDose().doubleValue())`（PrescriptionCheckItem.dose 仍为 double）
3. `PrescriptionAuditServiceImpl.java:501` `item.getDose()` 在字符串拼接中 → `String.valueOf(item.getDose())` 显式转换（BigDecimal.toString() 行为正确但需显式转换确保可读性）
4. `PrescriptionAssistServiceImpl.java:145` `doseCheckReq.setDosage(item.getDose())` → 需确认 setDosage 参数类型（见下方依赖分析）

### 8d/8e/8j/8m/8n. PrescriptionAuditServiceImpl

**形态**：已有类，多方法变更
**包路径**：`com.aimedical.modules.prescription.service.audit.impl.PrescriptionAuditServiceImpl`

**变更1（8d）**：5 处 `"RX-" + System.currentTimeMillis()` 替换
- 替换为 `"RX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()`
- 涉及方法：`handleStepThree`（L223）、`handleStepThree` forceSubmit 分支（L264）、`buildStepThreeResponse` PASS 分支（L303）、`buildStepThreeResponse` fallback 分支（L333）
- 共 5 处：L223, L264, L294, L303, L333
- 新增 import：`import java.util.UUID;`

**变更2（8e）**：删除死代码
- 删除 L187-197 整个 `if (hasNewAlerts(...))` 块（含前后空行，约 12 行）
- 检查 `hasNewAlerts` 方法（L507-511）：该私有方法仅被删除的死代码块调用 → **一并删除 `hasNewAlerts` 方法**
- 语义：`snapshotCriticalAlerts` 与 `getCriticalAlerts` 在 8n 合并为一次性快照，无需再检测增量

**变更3（8j P15）**：reasons 透传
- `submit()` 中第一处 BlockResponse（L160-163，CRITICAL_DOSE 路径）：
  ```java
  List<String> reasons = snapshot.alerts.stream()
      .map(DosageAlert::getMessage)
      .collect(Collectors.toList());
  if (reasons.isEmpty()) reasons = List.of("Critical dosage alerts detected");
  BlockResponse blockInfo = new BlockResponse(reasons, "RX_BLOCK_CRITICAL_DOSE", LocalDateTime.now());
  ```
- `submit()` 中第二处 BlockResponse（L176-179，AUDIT_BLOCK 路径）：保留固定字符串但改为从 latestRecord 的 auditIssues 中提取原因（可选增强）；简化方案保留 `List.of("Prescription audit blocked")`——该路径无直接告警信息可透传
- `buildStepThreeResponse` 中 BLOCK 路径（L309-313）的 `List.of("Prescription audit blocked")` 保持不动（来自本地规则引擎触发的 BLOCK，alert 信息已存在于响应中但构建 BlockResponse 时无额外上下文）

**变更4（8m T27）**：per-prescriptionId 并发锁
- 新增字段：
  ```java
  private final ConcurrentHashMap<String, ReentrantLock> submitLocks = new ConcurrentHashMap<>();
  ```
- submit() 方法体：
  ```java
  @Override
  @Transactional
  public SubmitResponse submit(SubmitRequest request) {
      String lockKey = request.getPrescriptionId();
      ReentrantLock lock = submitLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());
      lock.lock();
      try {
          return doSubmit(request);
      } finally {
          lock.unlock();
          if (!lock.hasQueuedThreads()) {
              submitLocks.remove(lockKey);
          }
      }
  }
  ```
- 提取私有方法 `doSubmit(SubmitRequest request): SubmitResponse`，内容为原 submit() 方法体
- 新增 imports：`java.util.concurrent.ConcurrentHashMap` + `java.util.concurrent.locks.ReentrantLock`

**变更5（8n T16）**：doSubmit 中 TOCTOU 消除
- 删除 `hasCritical = prescriptionDraftContext.hasCriticalAlerts(...)`（第157行）
- 删除 `ctx.snapshotCriticalAlerts = prescriptionDraftContext.getCriticalAlerts(...)`（第159行）
- 替换为：
  ```java
  PrescriptionDraftContext.SnapshotResult snapshot = prescriptionDraftContext.snapshotCriticalAlerts(request.getPrescriptionId());
  if (snapshot.hasAlerts) {
      List<String> reasons = snapshot.alerts.stream()
          .map(DosageAlert::getMessage)
          .collect(Collectors.toList());
      if (reasons.isEmpty()) reasons = List.of("Critical dosage alerts detected");
      BlockResponse blockInfo = new BlockResponse(reasons, "RX_BLOCK_CRITICAL_DOSE", LocalDateTime.now());
      SubmitResponse resp = new SubmitResponse();
      resp.setSubmitted(false);
      resp.setBlockInfo(blockInfo);
      return resp;
  }
  ```
- 删除 `ctx.snapshotCriticalAlerts` 引用（SubmitContext 类中该字段不再需要，可一并删除）

### 8c/8f. DosageLimitRule

**形态**：已有类，两处修改
**包路径**：`com.aimedical.modules.prescription.rule.DosageLimitRule`

**变更1（8c 级联）**：L40 `BigDecimal dose = BigDecimal.valueOf(item.getDose())` → `BigDecimal dose = item.getDose()`

**变更2（8f）**：L35-38 findBestMatch null 回退追加日志
```java
DosageStandard matched = findBestMatch(standards, patientAge, patientWeight);
if (matched == null) {
    log.warn("findBestMatch returned null for drug {}, route {}, age={}, weight={}; falling back to first standard",
            item.getDrugId(), item.getRoute(), patientAge, patientWeight);
    matched = standards.get(0);
}
```
- 新增字段：`private static final Logger log = LoggerFactory.getLogger(DosageLimitRule.class);`
- 新增 imports：`import org.slf4j.Logger;` + `import org.slf4j.LoggerFactory;`

### 8c/8h. AuditConverter

**形态**：已有类，两处修改
**包路径**：`com.aimedical.modules.prescription.converter.AuditConverter`

**变更1（8c 级联）**：L70 `aiItem.setDose(item.getDose())` → `aiItem.setDose(item.getDose().doubleValue())`
- 理由：ai-api PrescriptionCheckItem.dose 仍为 `double`，需要从 BigDecimal 转换

**变更2（8h T35）**：unit/weight 映射补充
- `toAiCheckItem` 中新增：`aiItem.setUnit(item.getUnit())`
- `toAiPatientInfo` 中新增：`aiPatient.setWeight(bizPatient.getWeight())`

### 8g. AllergyCheckRule

**形态**：已有类，方法级变更
**包路径**：`com.aimedical.modules.prescription.rule.AllergyCheckRule`

**变更**：替换 L55-58 的 `if (allergyHistory.contains(allergen))` 块为：
- 使用 `Pattern.compile("\\b" + Pattern.quote(allergenLower) + "\\b", Pattern.CASE_INSENSITIVE)` 进行单词边界匹配
- 匹配成功后检查匹配位置前 20 字符范围是否存在否定前缀（`hasNegationPrefix`）
- 若存在否定前缀则 `continue` 跳过，否则返回 BLOCK

**新增私有方法**：
```java
private boolean hasNegationPrefix(String text) {
    String[] negations = {"no ", "not ", "without ", "denies ", "no known "};
    for (String neg : negations) {
        if (text.trim().endsWith(neg.trim()) || text.contains(neg)) {
            return true;
        }
    }
    return false;
}
```

**新增 imports**：`java.util.regex.Pattern` + `java.util.regex.Matcher`

### 8h. ai-api DTOs

#### PrescriptionCheckItem

**形态**：已有 DTO，新增字段
**包路径**：`com.aimedical.modules.ai.api.dto.prescription.PrescriptionCheckItem`
**变更**：新增字段
```java
private String unit;
public String getUnit() { return unit; }
public void setUnit(String unit) { this.unit = unit; }
```

#### PatientInfo (ai-api)

**形态**：已有 DTO，新增字段
**包路径**：`com.aimedical.modules.ai.api.dto.prescription.PatientInfo`
**变更**：新增字段
```java
private Double weight;
public Double getWeight() { return weight; }
public void setWeight(Double weight) { this.weight = weight; }
```

### 8i. DrugInteractionRule

**形态**：已有类，注解变更
**包路径**：`com.aimedical.modules.prescription.rule.DrugInteractionRule`
**变更**：类级别新增
```java
@Component
@ConditionalOnProperty(name = "prescription.rule.drug-interaction.enabled", havingValue = "true", matchIfMissing = true)
public class DrugInteractionRule {
```
- 新增 import：`import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;`

### 8j. PrescriptionAuditController

**形态**：已有 Controller，方法级变更
**包路径**：`com.aimedical.modules.prescription.api.PrescriptionAuditController`
**变更**：audit() 方法中 BLOCK 路径 reasons 透传
```java
if (response.getRiskLevel() == AuditRiskLevel.BLOCK) {
    List<String> reasons = response.getAlerts() != null && !response.getAlerts().isEmpty()
            ? response.getAlerts().stream().map(AuditAlert::getAlertMessage).collect(Collectors.toList())
            : List.of("Prescription audit blocked");
    BlockResponse blockInfo = prescriptionAuditEnforcer.enforce(
            request.getPrescriptionId(), reasons, "RX_BLOCK_AUDIT");
    ...
}
```

### 8l. PrescriptionAssistServiceImpl

**形态**：已有类，exceptionally 回调变更
**包路径**：`com.aimedical.modules.prescription.service.assist.impl.PrescriptionAssistServiceImpl`
**变更**：L388-393 exceptionally 回调中追加
```java
}, aiTaskExecutor).exceptionally(ex -> {
    log.warn("Async AI suggestion task failed for taskId={}: {}", taskId,
             ex instanceof CompletionException && ex.getCause() != null
                 ? ex.getCause().getMessage() : ex.getMessage());
    clearCriticalAlerts(request.getPrescriptionId());  // 新增
    return null;
});
```
- `clearCriticalAlerts` 已是本类私有方法（L237-239），无需新增 import

## 错误处理

| 子项 | 变更 | 策略 |
|------|------|------|
| 8a | NumberFormatException 空 catch → log.warn | 仅日志，无异常传播，fallback 返回空 alerts |
| 8b | 非法类型值 → return emptyList | 静默防御，不抛异常 |
| 8c | 无需错误处理 | BigDecim al 构造由调用方保证 |
| 8d | 无需错误处理 | UUID.randomUUID() 不抛受检异常 |
| 8e | 死代码删除 | 无 |
| 8f | findBestMatch null → log.warn + fallback | 不抛异常，不改返回路径 |
| 8g | 正则/否定前缀无新增异常 | Pattern.compile 只抛 PatternSyntaxException（模式由系统常量控制，不应发生） |
| 8h | 无需错误处理 | 纯字段映射 |
| 8i | 无需错误处理 | 编译期注解 |
| 8j | 无需错误处理 | reasons 列表构建 Null-safe（三元表达式 + stream） |
| 8k | 无需错误处理 | 纯重构，行为无变化 |
| 8l | exceptionally 追加清理 | 不影响主线程异常处理 |
| 8m | per-prescriptionId 锁 | lock.lock() 不抛受检异常；finally 确保 unlock |
| 8n | TOCTOU 消除 | 单次快照替代两次读取，消除竞争窗口 |

## 行为契约

### 8a
- 非数字频率时 `log.warn` 输出一次日志，频率参数可见
- 方法继续执行，最终返回的 alerts 不含日剂量告警（仍可能含单次剂量告警）
- 不影响返回值类型和数量

### 8b
- `getCriticalAlerts` 返回 `List<DosageAlert>`，draftContextStore 中存储非 List 类型值时返回 `Collections.emptyList()`
- 行为与之前一致（之前 `value instanceof List` 已检查类型，但 raw type 有 unchecked cast warning）

### 8c
- `PrescriptionItem.dose` 类型从 `double` 变为 `BigDecimal`
- `itemToComparisonKey` 中 `item.getDose()` 参与字符串拼接：BigDecimal.toString() 不会丢失精度
- `DosageLimitRule.check` 中 `BigDecimal dose = BigDecimal.valueOf(item.getDose())` 改为 `BigDecimal dose = item.getDose()`——结果完全相同
- `AuditConverter.toAiCheckItem` 中 `aiItem.setDose(item.getDose())` 改为 `.doubleValue()`——double 值不变
- `PrescriptionAssistServiceImpl.parseDraftItems` 中 `item.setDose(dose.asDouble())` 编译错误——需改为 `item.setDose(BigDecimal.valueOf(dose.asDouble()))`

**已确认**：
- `PrescriptionAssistServiceImpl.java:145` `doseCheckReq.setDosage(item.getDose())` → `doseCheckReq.setDosage(item.getDose().doubleValue())`（DosageCheckRequest.setDosage 参数为 `double`）
- `PrescriptionAssistServiceImpl.java:274` `item.setDose(dose.asDouble())` → `item.setDose(BigDecimal.valueOf(dose.asDouble()))`（PrescriptionItem.setDose 现参数为 `BigDecimal`）

### 8d
- 碰撞概率 ≈ 1/(16^8) ≈ 1/4.3e9

### 8e
- 删除 L187-197 `if (hasNewAlerts(...))` 块后 `submit()` 行为不变（该分支运行时不可达）
- `hasNewAlerts` 方法仅被删除的死代码块调用，一并删除

### 8f
- findBestMatch 返回 null 时：log.warn 后 fallback 到 `standards.get(0)`，行为不变
- `standards` 在调用 findBestMatch 前已通过 isEmpty() 检查，get(0) 不会 NPE

### 8g
- `allergyHistory.contains(allergen)` → 单词边界匹配 + 否定前缀检查
- "No allergy to penicillin" → 匹配到 "penicillin" 但前 20 字符含 "no " → 跳过 → PASS
- "Has penicillin allergy" → 匹配到 "penicillin" 无否定前缀 → BLOCK
- "cillin" 不匹配 "penicillin"（单词边界）→ PASS

### 8h
- `AuditConverter.toAiCheckItem`：setUnit(item.getUnit())，unit 为 String，可为 null
- `AuditConverter.toAiPatientInfo`：setWeight(bizPatient.getWeight())，weight 为 Double，可为 null
- ai-api PatientInfo.getWeight() 返回 Double

### 8i
- `DrugInteractionRule` 仅在 `prescription.rule.drug-interaction.enabled=true` 时注册为 Spring Bean
- `matchIfMissing = true`：未配置时默认启用（兼容现有配置）

### 8j
- Controller: `response.getAlerts()` 非空时提取 alertMessage；空时回退到固定字符串
- Service (CRITICAL_DOSE): 从 snapshot.alerts.stream().map(DosageAlert::getMessage) 提取；空时回退
- Service (AUDIT_BLOCK): 保留固定字符串（无额外上下文可透传）

### 8k
- 行为无变化：5 个循环的匹配条件完全等价于原逻辑
- 仅抽取公共模式，无算法变更

### 8l
- exceptionally 回调中异步 AI 失败时清理 draftContext（清空 critical alerts）
- 不改变 `submit()` 的阻塞行为：submit 已由 8m + 8n 消除与 asynchronous 清理的竞争

### 8m
- 同一 prescriptionId 的 submit() 请求串行执行
- 不同 prescriptionId 的 submit() 互不阻塞
- `doSubmit` 保持原 `@Transactional` 语义

### 8n
- `doSubmit` 中 `hasCriticalAlerts` + `getCriticalAlerts` 两次读取 → 单次 `snapshotCriticalAlerts`
- TOCTOU 窗口消除：snapshot 在锁内获取，assist() 线程即使写入新 alerts 也不影响当前已完成的快照

## 依赖关系

| 依赖 | 说明 |
|------|------|
| `org.slf4j.Logger` + `LoggerFactory` | 8a DosageThresholdService 新增；8f DosageLimitRule 新增 |
| `java.util.UUID` | 8d PrescriptionAuditServiceImpl 新增 |
| `java.util.regex.Pattern` + `Matcher` | 8g AllergyCheckRule 新增 |
| `java.util.function.Predicate` | 8k DosageThresholdService 新增 |
| `java.util.concurrent.ConcurrentHashMap` + `java.util.concurrent.locks.ReentrantLock` | 8m PrescriptionAuditServiceImpl 新增 |
| `org.springframework.boot.autoconfigure.condition.ConditionalOnProperty` | 8i DrugInteractionRule 新增 |
| `com.aimedical.modules.ai.api.dto.prescription.PatientInfo` (ai-api) | 8h 新增 weight 字段，依赖 Jackson 序列化（已有） |
| `com.aimedical.modules.prescription.dto.audit.PatientInfo` (biz) | 8h 依赖 biz PatientInfo.getWeight() 已存在（L66-68） |
| `PrescriptionDraftContext.SnapshotResult` | 8n 新增内部类，被 PrescriptionAuditServiceImpl 使用 |
| `DosageCheckRequest.setDosage()` | 8c 级联影响：需确认参数类型是否为 `BigDecimal`（正查 `DosageCheckRequest.java`） |
| `PrescriptionItem.setDose(double)` (在测试和 parseDraftItems 中) | 8c 级联：所有 `setDose(double)` 调用需改为 `setDose(BigDecimal)`——`parseDraftItems` 中 `item.setDose(BigDecimal.valueOf(dose.asDouble()))` |

## 级联影响总结（8c T14 double→BigDecimal）

已确认的 8c 级联影响完整清单：

| 位置 | 原代码 | 新代码 | 原因 |
|------|--------|--------|------|
| `DosageLimitRule.java:40` | `BigDecimal.valueOf(item.getDose())` | `item.getDose()` | 直接返回 BigDecimal |
| `AuditConverter.java:70` | `aiItem.setDose(item.getDose())` | `aiItem.setDose(item.getDose().doubleValue())` | ai-api PrescriptionCheckItem.dose 仍为 double |
| `PrescriptionAssistServiceImpl.java:145` | `doseCheckReq.setDosage(item.getDose())` | `doseCheckReq.setDosage(item.getDose().doubleValue())` | DosageCheckRequest.dosage 为 double |
| `PrescriptionAssistServiceImpl.java:274` | `item.setDose(dose.asDouble())` | `item.setDose(BigDecimal.valueOf(dose.asDouble()))` | PrescriptionItem.setDose 现为 BigDecimal |
| `PrescriptionAuditServiceImpl.java:501` | `item.getDose()`(字符拼接) | `item.getDose()` + toString() 自动兼容 | BigDecimal.toString() 行为正确 |
