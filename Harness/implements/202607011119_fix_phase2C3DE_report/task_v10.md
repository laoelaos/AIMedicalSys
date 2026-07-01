# 任务指令（v10）

## 动作
NEW

## 任务描述
修复 prescription 模块剩余的 10 项 P1/P2 缺陷（T9, T10, T14, T15, T28, T30, T31, T35, P12, P15），均为单文件/单方法小改动，不涉及新增类型或 API 变更。

### 子项清单

| 编号 | 诊断编号 | 级别 | 描述 | 涉及文件 | 预估工作量 |
|:---:|:--------:|:----:|------|---------|:---------:|
| 8a | T9 | P1 | DosageThresholdService 频率解析 NumberFormatException 被空 catch 吞掉 | `DosageThresholdService.java` | 0.3人时 |
| 8b | T10 | P1 | PrescriptionDraftContext.getCriticalAlerts @SuppressWarnings("unchecked") | `PrescriptionDraftContext.java` | 0.2人时 |
| 8c | T14 | P1 | PrescriptionItem.dose 使用 double → BigDecimal | `PrescriptionItem.java` | 0.5人时 |
| 8d | T15 | P1 | prescriptionOrderId 使用 System.currentTimeMillis() → UUID | `PrescriptionAuditServiceImpl.java` | 0.5人时 |
| 8e | T28 | P1 | hasNewAlerts 死代码（BLOCK 分支不可达） | `PrescriptionAuditServiceImpl.java` | 0.3人时 |
| 8f | T30 | P1 | DosageLimitRule.findBestMatch 返回 null 静默回退 | `DosageLimitRule.java` | 0.2人时 |
| 8g | T31 | P1 | AllergyCheckRule allergyHistory contains 子串匹配过于激进 | `AllergyCheckRule.java` | 0.3人时 |
| 8h | T35 | P1 | AuditConverter + ai-api DTO 遗漏 weight 和 unit 字段映射 | `AuditConverter.java`, `PrescriptionCheckItem.java`, `PatientInfo.java` | 0.5人时 |
| 8i | P12 | P2 | DrugInteractionRule 无 Phase 4 预留 @ConditionalOnProperty 标注 | `DrugInteractionRule.java` | 0.2人时 |
| 8j | P15 | P2 | 降级 BLOCK 路径 reasons 固定字符串 | `PrescriptionAuditServiceImpl.java`, `PrescriptionAuditController.java` | 0.5人时 |

## 选择理由
路线表第 8 项。consultation P1+P2 批量（R10）已全部完成并验证通过（198 用例，0 失败）。prescription 模块 P0 缺陷已在 R4-R6 修复完毕，剩余 P1/P2 项均为单文件小改动，可批量实现。

## 任务上下文

### 8a. T9 — DosageThresholdService 频率解析静默失效
- **问题**：DosageThresholdService.java:76-89 的 parseInt 对非数字频率字符串（如 "tid"）抛 NumberFormatException，被空 catch 吞掉，日剂量校验静默失效
- **变更**：
  - catch 块补充 `log.warn("Non-numeric frequency: {}, skipping daily dose check", request.getFrequency())`
  - 声明 Logger 字段：`private static final Logger log = LoggerFactory.getLogger(DosageThresholdService.class);`（当前类无 Logger），新增 import `import org.slf4j.Logger;` + `import org.slf4j.LoggerFactory;`
  - 返回 fallback 默认值（无需额外操作，catch 后自然跳过日剂量逻辑）
- **涉及文件**：`prescription/.../service/assist/DosageThresholdService.java`
- **测试方案**：
  - 测试文件：`DosageThresholdServiceTest.java`
  - 新增测试 `shouldLogWarningAndReturnNoDailyAlertWhenFrequencyIsNotNumeric()`：
    - 设置 `request.setFrequency("tid")`，标准单次/日剂量均设置
    - 验证返回的 alerts 不包含日剂量告警（仍可能含单次剂量告警）
    - `DosageThresholdService` 使用 `log`（SLF4J Logger），建议通过 `LoggerFactory.getLogger` 获取 logger，测试中可用 `ListAppender` 捕获 log 事件验证 `log.warn` 调用

### 8b. T10 — PrescriptionDraftContext.getCriticalAlerts unchecked cast
- **问题**：PrescriptionDraftContext.java:29-36 的 `@SuppressWarnings("unchecked")` 下 `(List<DosageAlert>) value` 无泛型类型校验
- **变更**：在 cast 前添加 `value instanceof List<?>` 检查，cast 失败时 return `Collections.emptyList()`
  ```java
  public List<DosageAlert> getCriticalAlerts(String prescriptionId) {
      String key = prescriptionId + CRITICAL_ALERTS_SUFFIX;
      Object value = draftContextStore.get(key);
      if (value instanceof List<?>) {
          return (List<DosageAlert>) value;
      }
      return Collections.emptyList();
  }
  ```
  - 删除 `@SuppressWarnings("unchecked")`
- **涉及文件**：`prescription/.../context/PrescriptionDraftContext.java`
- **测试方案**：
  - 测试文件：`PrescriptionDraftContextTest.java`
  - 现有测试 `shouldReturnEmptyListWhenNoAlerts()` 覆盖 null 路径，无需修改
  - 新增测试 `shouldReturnEmptyListWhenValueIsNotList()`：
    - `when(draftContextStore.get(...)).thenReturn("not a list")`
    - `assertTrue(context.getCriticalAlerts("rx-001").isEmpty())`

### 8c. T14 — PrescriptionItem.dose double → BigDecimal
- **问题**：dto/audit/PrescriptionItem.java:7 的 dose 字段使用 `double`，OOD §1.3 要求 BigDecimal
- **变更**：
  ```java
  // PrescriptionItem.java
  private BigDecimal dose;
  
  public BigDecimal getDose() { return dose; }
  public void setDose(BigDecimal dose) { this.dose = dose; }
  ```
  - 添加 import: `import java.math.BigDecimal;`
- **级联影响分析（全部需处理）**：
  1. `PrescriptionAuditServiceImpl.java:501` `itemToComparisonKey()` 中 `item.getDose()` 用于字符串拼接 → 编译无错误（BigDecimal.toString() 可用），行为正确
  2. `DosageLimitRule.java:40` `BigDecimal dose = BigDecimal.valueOf(item.getDose())` → **编译错误**，改为 `BigDecimal dose = item.getDose()`（已为 BigDecimal）
  3. `AuditConverter.java:70` `aiItem.setDose(item.getDose())` → **编译错误**，`PrescriptionCheckItem.setDose(double)` 不接受 BigDecimal，改为 `aiItem.setDose(item.getDose().doubleValue())`
  4. 所有 `item.setDose(double)` 调用的测试 → 改为 `item.setDose(new BigDecimal("..."))`
- **涉及文件**：
  - `prescription/.../dto/audit/PrescriptionItem.java`
  - `prescription/.../rule/DosageLimitRule.java`（`BigDecimal.valueOf(item.getDose())` → `item.getDose()`）
  - `prescription/.../converter/AuditConverter.java`（`aiItem.setDose(item.getDose())` → `item.getDose().doubleValue()`）
- **测试方案**：
  - `PrescriptionItemTest.java`：`setDose(100.0)` → `setDose(new BigDecimal("100.0"))`；`assertEquals(100.0, ..., 0.001)` → `assertEquals(new BigDecimal("100.0"), item.getDose())`
  - `AuditConverterTest.java`：`item.setDose(100)` → `item.setDose(new BigDecimal("100"))`；第62行 `assertEquals(100, checkItem.getDose())` 无需修改（PrescriptionCheckItem.dose 仍为 double，setDose 通过 .doubleValue() 传值）
  - `DosageLimitRuleTest.java`：所有 20+ 处 `item.setDose(int)` 调用改为 `item.setDose(new BigDecimal("..."))`，例如 `item.setDose(500)` → `item.setDose(new BigDecimal("500"))`
  - `PrescriptionAuditServiceImplTest.java`：所有 `PrescriptionItem` 的 `setDose(double)` 调用改为 `setDose(new BigDecimal("..."))`

### 8d. T15 — prescriptionOrderId 冲突
- **问题**：PrescriptionAuditServiceImpl.java 五处使用 `"RX-" + System.currentTimeMillis()` 生成 ID
- **变更**：替换为 `"RX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()`
  - 选择理由：8 位大写十六进制字符提供 32 位随机空间（碰撞概率约 1/4e9），远低于当前毫秒级并发碰撞概率，长度适中便于日志阅读
  - 五处位置：第223、264、294、303、333行
  - 新增 import: `import java.util.UUID;`
- **涉及文件**：`prescription/.../service/impl/PrescriptionAuditServiceImpl.java`
- **测试方案**：
  - `PrescriptionAuditServiceImplTest.java`：验证 `prescriptionOrderId` 以 `"RX-"` 开头且总长度 = 12（"RX-" + 8 位 hex + 1位填充？不对，"RX-" + 8 hex = 11 字符）。通过正则 `^RX-[0-9A-F]{8}$` 匹配验证
  - 搜索 `"RX-" + System.currentTimeMillis()` 匹配的所有测试断言，更新预期值为 UUID 格式

### 8e. T28 — hasNewAlerts 死代码
- **问题**：submit 方法第187-197行 BLOCK 分支不可达（hasCriticalAlerts=true 时已在第157-168行提前返回 BLOCK；false 时 currentAlerts 必然为空）
- **变更**：删除第187-197行整个 `if (hasNewAlerts(...))` 块（包括 `// hasNewAlerts` 注释下方的 ~11 行代码）
- **涉及文件**：`prescription/.../service/impl/PrescriptionAuditServiceImpl.java`
- **测试方案**：
  - 确认 `hasNewAlerts` 私有方法仍保留（`buildWarnResultFromRecord` 等路径可能引用？检查后确认仅被删除的死代码块调用，若已无其他调用者则删除 `hasNewAlerts` 方法本身）
  - 实际检查 `hasNewAlerts` 方法（第507-511行）确认是否仅有死代码处调用。若仅一处，一并删除该方法
  - 验证无测试直接覆盖删除的 BLOCK 分支（该分支运行时不可达，应有测试覆盖）。删除后全量测试不应有新增失败

### 8f. T30 — DosageLimitRule.findBestMatch null 回退
- **问题**：DosageLimitRule.java:36-38 findBestMatch 返回 null 时回退到 `standards.get(0)` 无日志
- **分析**：`findBestMatch` 返回 null 时，`standards` 一定非空（第31-33行已检查 `standards.isEmpty()` 并 continue），所以当前 `standards.get(0)` 不会 NPE。问题在于静默回退可能掩盖标准匹配异常
- **变更**：
  ```java
  // DosageLimitRule.java:36-38
  DosageStandard matched = findBestMatch(standards, patientAge, patientWeight);
  if (matched == null) {
      log.warn("findBestMatch returned null for drug {}, route {}, age={}, weight={}; falling back to first standard",
              item.getDrugId(), item.getRoute(), patientAge, patientWeight);
      matched = standards.get(0);
  }
  ```
  - 新增 field: `private static final Logger log = LoggerFactory.getLogger(DosageLimitRule.class);`
  - 新增 import: `import org.slf4j.Logger;` + `import org.slf4j.LoggerFactory;`
- **涉及文件**：`prescription/.../rule/DosageLimitRule.java`
- **测试方案**：
  - `DosageLimitRuleTest.java`：修改 `shouldFallbackToFirstStandardWhenNoMatch` 测试，验证仍返回 BLOCK（fallback 行为不变），不降级
  - 新增测试或日志验证：该测试中 fallback 路径受保护，不改断言仅验证回归

### 8g. T31 — AllergyCheckRule contains 匹配过于激进
- **问题**：AllergyCheckRule.java:54-58 的 contains 子串匹配导致 "No allergy to penicillin" 命中 "penicillin" 并返回 BLOCK
- **变更**：将 `allergyHistory.contains(allergen)` 替换为单词边界匹配 + 否定前缀跳过
  ```java
  // 在 parseAllergens 之后
  } else if (allergyHistory != null && !allergyHistory.isBlank()) {
      for (String allergen : allergens) {
          // 跳过否定前缀行
          String historyLower = allergyHistory.toLowerCase();
          String allergenLower = allergen.toLowerCase();
          // 单词边界匹配
          Pattern pattern = Pattern.compile("\\b" + Pattern.quote(allergenLower) + "\\b", Pattern.CASE_INSENSITIVE);
          Matcher matcher = pattern.matcher(allergyHistory);
          if (matcher.find()) {
              // 检查是否被否定前缀否定
              int matchStart = matcher.start();
              String beforeMatch = allergyHistory.substring(Math.max(0, matchStart - 20), matchStart).toLowerCase();
              if (hasNegationPrefix(beforeMatch)) {
                  continue;
              }
              return new LocalRuleResult(RULE_ID, false, "Allergy history matched for drug " + item.getDrugId(), AuditRiskLevel.BLOCK);
          }
      }
  }
  
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
  - 新增 imports: `import java.util.regex.Pattern;` + `import java.util.regex.Matcher;`
- **涉及文件**：`prescription/.../rule/AllergyCheckRule.java`
- **测试方案**：
  - `AllergyCheckRuleTest.java`：
  - 修改 `shouldReturnBlockWhenAllergyHistoryTextMatch`：allergyHistory 从 `"Penicillin allergy"` 改为其他无否定的正例
  - 新增测试 `shouldReturnPassWhenAllergyHistoryHasNegationPrefix()`：`allergyHistory = "No allergy to penicillin"`，验证 `result.isPassed()`
  - 新增测试 `shouldMatchAllergenAtWordBoundary()`：`allergyHistory = "Penicillin allergy"`，验证 BLOCK（避免 "cillin" 子串误匹配）
  - 删除或修改现有测试 `shouldReturnBlockWhenAllergyHistoryTextMatch` 以验证新逻辑（如改为 `"Has penicillin allergy"` 仍返回 BLOCK）

### 8h. T35 — AuditConverter 遗漏 unit/weight
- **问题**：AuditConverter.java:66-75 toAiCheckItem 未映射 unit；第77-92行 toAiPatientInfo 未映射 weight
- **分析**：ai-api DTO `PrescriptionCheckItem` 缺少 unit 字段，`PatientInfo` 缺少 weight 字段，需先补全 DTO
- **变更**（分三步）：
  **Step 1**: ai-api DTO 新增字段
  - `PrescriptionCheckItem.java`（ai-api）：新增 `private String unit;` + getter/setter
  - `PatientInfo.java`（ai-api）：新增 `private Double weight;` + getter/setter，新增 import `import com.fasterxml.jackson.annotation.JsonProperty;`（如有序列化需求）
  
  **Step 2**: AuditConverter 映射填充
  - `toAiCheckItem()`：追加 `aiItem.setUnit(item.getUnit())`
  - `toAiPatientInfo()`：追加 `aiPatient.setWeight(bizPatient.getWeight())`
  
  **Step 3**: biz PatientInfo 确认 weight 字段存在
  - `com.aimedical.modules.prescription.dto.audit.PatientInfo`：确认已存在 `getWeight()` 方法（返回 Double），以供 AuditConverter 调用
- **涉及文件**：
  - `ai/ai-api/.../dto/prescription/PrescriptionCheckItem.java`
  - `ai/ai-api/.../dto/prescription/PatientInfo.java`
  - `prescription/.../converter/AuditConverter.java`
- **测试方案**：
  - `AuditConverterTest.java`：
  - `shouldMapAuditRequestToPrescriptionCheckRequest` 中给 `item.setUnit("mg")`，验证 `checkItem.getUnit()` 等 `"mg"`
  - `shouldMapAuditRequestToPrescriptionCheckRequest` 中给 `patientInfo.setWeight(70.0)`，验证 `checkRequest.getPatientInfo().getWeight()` 等 `70.0`
  - 新增 `PrescriptionCheckItem` 和 `PatientInfo` (ai-api) 测试验证新字段 getter/setter 可用性（可选，若团队惯例不测试纯 DTO 则跳过）

### 8i. P12 — DrugInteractionRule Phase 4 预留
- **问题**：DrugInteractionRule.java:1-15 缺少 @ConditionalOnProperty 标注
- **变更**：追加类级别注解
  ```java
  @Component
  @ConditionalOnProperty(name = "prescription.rule.drug-interaction.enabled", havingValue = "true", matchIfMissing = true)
  public class DrugInteractionRule {
  ```
  - 新增 import: `import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;`
- **涉及文件**：`prescription/.../rule/DrugInteractionRule.java`
- **测试方案**：
  - `DrugInteractionRuleTest.java`：新增测试 `shouldBeAnnotatedWithConditionalOnProperty()`
    ```java
    @Test
    void shouldBeAnnotatedWithConditionalOnProperty() {
        ConditionalOnProperty annotation = DrugInteractionRule.class.getAnnotation(ConditionalOnProperty.class);
        assertNotNull(annotation);
        assertArrayEquals(new String[]{"prescription.rule.drug-interaction.enabled"}, annotation.name());
        assertEquals("true", annotation.havingValue());
        assertTrue(annotation.matchIfMissing());
    }
    ```
    - 注意使用 `assertArrayEquals` 而非 `assertEquals`，因 `annotation.name()` 返回 `String[]`

### 8j. P15 — 降级 BLOCK 路径 reasons 固定字符串
- **问题**：PrescriptionAuditController.java:36-48 和 PrescriptionAuditServiceImpl.java 中 BLOCK 路径 reasons 使用 `List.of("Prescription audit blocked")` 等固定字符串
- **分析**：Controller 中 `audit()` 方法收到 BLOCK 级别响应后调用 `prescriptionAuditEnforcer.enforce()` 传固定字符串。Service 中 `submit()` 方法的 BLOCK 路径同样使用固定字符串。不涉及方法签名或 Controller 路由变更
- **变更**：
  1. `PrescriptionAuditServiceImpl.submit()` 中两处 BlockResponse 构造传实际原因：
     - 第160行 `new BlockResponse(List.of("Critical dosage alerts detected"), ...)` → 改为提取 `prescriptionDraftContext.getCriticalAlerts()` 中的 alert message 作为 reasons
       ```java
       List<String> reasons = ctx.snapshotCriticalAlerts.stream()
               .map(DosageAlert::getMessage)
               .collect(Collectors.toList());
       if (reasons.isEmpty()) reasons = List.of("Critical dosage alerts detected");
       BlockResponse blockInfo = new BlockResponse(reasons, "RX_BLOCK_CRITICAL_DOSE", LocalDateTime.now());
       ```
     - 第176行 `new BlockResponse(List.of("Prescription audit blocked"), ...)` → 保留（该处为 latestRecord riskLevel == BLOCK，无额外上下文可透传）或从 latestRecord 提取原因
       ```java
       // 从 latestRecord 的 auditIssues 解析原因（可选增强）
       ```
     简化方案：第一处（CRITICAL_DOSE）透传 alert message；第二处（AUDIT_BLOCK）因无直接告警信息，保留固定字符串但改为从 latestRecord 解析
  2. `PrescriptionAuditController.audit()` 第41行 `List.of("Prescription audit blocked")` → 使用 `response.getAlerts()` 提取原因
     ```java
     List<String> reasons = response.getAlerts() != null && !response.getAlerts().isEmpty()
             ? response.getAlerts().stream().map(AuditAlert::getAlertMessage).collect(Collectors.toList())
             : List.of("Prescription audit blocked");
     BlockResponse blockInfo = prescriptionAuditEnforcer.enforce(
             request.getPrescriptionId(), reasons, "RX_BLOCK_AUDIT");
     ```
- **涉及文件**：`prescription/.../service/impl/PrescriptionAuditServiceImpl.java`、`prescription/.../controller/PrescriptionAuditController.java`
- **测试方案**：
  - `PrescriptionAuditControllerTest.java`：
  - `auditShouldReturn422WhenBlocked`：设置 `response.getAlerts()` 含实际的 alert，验证 `verify(prescriptionAuditEnforcer).enforce(eq("prescriptionId"), argThat(reasons -> reasons.contains("actual alert")), any())`
  - `PrescriptionAuditServiceImplTest.java`：
  - `submit` 测试中 BLOCK 返回路径验证 `blockInfo.getBlockReasons()` 包含实际 alert message

## 已有代码上下文

- `DosageThresholdService.java`：第76-89行 parseInt 吞异常
- `PrescriptionDraftContext.java`：第24-36行 `@SuppressWarnings("unchecked")`
- `PrescriptionItem.java`：第7行 `private double dose;`
- `PrescriptionAuditServiceImpl.java`：第223,264,294,303,333行 `"RX-" + System.currentTimeMillis()`；第187-197行 BLOCK 分支；第144-152行 reasons 固定字符串
- `DosageLimitRule.java`：第36-38行 findBestMatch null → standards.get(0)
- `AllergyCheckRule.java`：第54-58行 allergyHistory contains 匹配
- `AuditConverter.java`：第66-75行 toAiCheckItem；第77-92行 toAiPatientInfo
- `DrugInteractionRule.java`：第1-15行 class 声明，缺少 @ConditionalOnProperty
- `PrescriptionAuditController.java`：第36-41行 enforce reasons 固定字符串

### 8k. P10 — DosageThresholdService.matchByPriority 循环逻辑重复

- **问题**：`DosageThresholdService.java:95-147` 的 `matchByPriority` 方法 5 个循环遍历结构完全一致（均执行「遍历 candidates → 检查范围条件 → 返回首个匹配」模式），Loop1（L103-113）与 Loop2（L115-121）的条件体逻辑重复（均检查 `age != null && weight != null && isInRange(age) && isInRange(weight)`），仅精确度要求不同。代码维护成本高，新增范围维度易遗漏循环。
- **变更**：抽取两个辅助方法消除重复：
  1. `matchesBothRanges(DosageStandard ds, Integer age, BigDecimal weight)`：合并 L104-106 与 L116-118 的共同条件
  2. `findFirstCandidate(List<DosageStandard>, Predicate<DosageStandard>)`：统一循环遍历模式
  ```java
  private boolean matchesBothRanges(DosageStandard ds, Integer age, BigDecimal weight) {
      return age != null && weight != null
          && isInRange(age, ds.getAgeRangeStart(), ds.getAgeRangeEnd())
          && isInRange(weight, ds.getWeightRangeStart(), ds.getWeightRangeEnd());
  }
  
  private DosageStandard findFirstCandidate(List<DosageStandard> candidates, Predicate<DosageStandard> predicate) {
      for (DosageStandard ds : candidates) {
          if (predicate.test(ds)) return ds;
      }
      return null;
  }
  ```
  5 个循环全部替换为 `findFirstCandidate(candidates, ds -> ...)` 调用。
  - 新增 import: `import java.util.function.Predicate;`
- **涉及文件**：`prescription/.../service/assist/DosageThresholdService.java`
- **测试方案**：纯重构，行为无变化。`DosageThresholdServiceTest.java` 中 `matchByPriority` 相关测试预期不变。新增 1 个测试 `shouldMatchByPriorityWithExactAgeAndWeightRange()` 显式验证优先级顺序（精确范围优先于模糊范围）。

### 8l. P14 — PrescriptionAssistServiceImpl 异步失败路径未清理 DraftContext

- **问题**：`PrescriptionAssistServiceImpl.java:388-393` 的 `scheduleSuggestionAsync()` exceptionally 回调在异步 AI 调用失败时仅记录 WARN 日志并返回 null。`assist()` 主流程（L175）写入的 critical alerts 未被清理。当异步 AI 失败后，`submit()` 调用因 `hasCriticalAlerts()` 返回 true 而始终被阻塞，即使异步 AI 确认失败。
- **并发风险分析**：exceptionally 在 `aiTaskExecutor` 线程中执行，`clearCriticalAlerts()` 与 `submit()` 的 `hasCriticalAlerts()`/`getCriticalAlerts()` 存在时序窗口。但此窗口已由 8m（T27）的 per-prescriptionId 锁在 `submit()` 路径中消除——`doSubmit()` 在锁内通过 `snapshotCriticalAlerts()`（8n T16 新增）获取原子快照，exceptionally 的 `clearCriticalAlerts()` 在锁外执行，不会影响锁内已完成的原子快照。剩余风险：`exceptionally` 在 `assist()` 返回之后、`submit()` 加锁之前执行 → 正常，`submit()` 加锁后读取的是最新快照；`exceptionally` 在 `submit()` 已加锁且完成快照之后执行 → exceptionally 的清理延迟不影响当前 submit（已基于锁前快照决策），影响下一轮 submit（会重新读取）。**结论：风险可接受，无需额外同步。**
- **变更**：exceptionally 回调中追加 `clearCriticalAlerts(request.getPrescriptionId())` 清理 draftContext：
  ```java
  }, aiTaskExecutor).exceptionally(ex -> {
      log.warn("Async AI suggestion task failed for taskId={}: {}", taskId,
               ex instanceof CompletionException && ex.getCause() != null
                   ? ex.getCause().getMessage() : ex.getMessage());
      clearCriticalAlerts(request.getPrescriptionId());
      return null;
  });
  ```
  - 无需新增 import（`clearCriticalAlerts` 已是本类私有方法）
- **涉及文件**：`prescription/.../service/assist/impl/PrescriptionAssistServiceImpl.java`
- **测试方案**：
  - `PrescriptionAssistServiceImplTest.java`：
  - 新增测试 `asyncSuggestionShouldClearDraftContextOnExceptionally()`：
    - `doAnswer(r -> { r.getArgument(0, Runnable.class).run(); return null; }).when(aiTaskExecutor).execute(any())`
    - aiService.prescriptionAssist() 抛出 `RuntimeException`
    - 调用 `prescriptionAssistService.assist(request)` → 触发 scheduleSuggestionAsync → exceptionally
    - `verify(prescriptionDraftContext).updateCriticalAlerts(request.getPrescriptionId(), Collections.emptyList())`

### 8m. T27 — submit() 并发提交防护

- **问题**：`PrescriptionAuditServiceImpl.submit()`（L150-199）无 prescriptionId 级别并发控制。多个线程对同一 `prescriptionId` 同时调用 `submit()` 可同时通过 `hasCriticalAlerts`（L157）、`latestRecord`（L171）等检查，进入 `handleStepThree`（L199）后重复生成多个 `prescriptionOrderId`，导致同一处方被多次提交。
- **变更**：在 `submit()` 入口添加 per-prescriptionId `ReentrantLock`，与已有 E02 修复模式一致：
  ```java
  private final ConcurrentHashMap<String, ReentrantLock> submitLocks = new ConcurrentHashMap<>();
  
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
  
  private SubmitResponse doSubmit(SubmitRequest request) {
      // 原 submit() 方法体 + handleStepThree + buildStepThreeResponse 等私有方法保持不动
  }
  ```
  - 新增 imports: `import java.util.concurrent.ConcurrentHashMap;`、`import java.util.concurrent.locks.ReentrantLock;`
- **涉及文件**：`prescription/.../service/audit/impl/PrescriptionAuditServiceImpl.java`
- **测试方案**：
  - `PrescriptionAuditServiceImplTest.java`：
  - 新增测试 `submitShouldSerializeConcurrentCallsForSamePrescriptionId()`：
    - 使用 `CountDownLatch` 构造两个线程同时调用 `submit(同 prescriptionId 的 request)`
    - 验证最终仅生成一条 `prescriptionOrderId`
  - 新增测试 `submitShouldNotBlockDifferentPrescriptionIds()`：
    - 两个线程分别调用不同 prescriptionId 的 submit
    - 验证互不阻塞，均正常提交

### 8n. T16 — hasCriticalAlerts 与 getCriticalAlerts TOCTOU 窗口

- **问题**：`PrescriptionDraftContext.hasCriticalAlerts()` 和 `getCriticalAlerts()` 分别两次读取 `draftContextStore`。`hasCriticalAlerts()` 返回 false 后、`getCriticalAlerts()` 调用前，`assist()` 线程可能通过 `updateCriticalAlerts()` 写入新 alerts，使 `submit()` 基于过时状态决策（TOCTOU）。
- **变更**：在 `submit()` 路径（已由 8m 加 per-prescriptionId 锁）中，将两次独立读取合并为一次原子快照：

  1. `PrescriptionDraftContext` 新增 `SnapshotResult snapshotCriticalAlerts(String prescriptionId)`：
     ```java
     public SnapshotResult snapshotCriticalAlerts(String prescriptionId) {
         String key = prescriptionId + CRITICAL_ALERTS_SUFFIX;
         Object value = draftContextStore.get(key);
         if (value instanceof List<?>) {
             List<DosageAlert> alerts = (List<DosageAlert>) value;
             return new SnapshotResult(!alerts.isEmpty(), alerts);
         }
         return new SnapshotResult(false, Collections.emptyList());
     }
     
     public static class SnapshotResult {
         public final boolean hasAlerts;
         public final List<DosageAlert> alerts;
         public SnapshotResult(boolean hasAlerts, List<DosageAlert> alerts) {
             this.hasAlerts = hasAlerts;
             this.alerts = alerts;
         }
     }
     ```

  2. `PrescriptionDraftContext` 删除 `@SuppressWarnings("unchecked")` 下的单次 `hasCriticalAlerts()` 方法（仅 `submit()` 路径引用，已被合并）。保留 `getCriticalAlerts()` 单次读取方法供其他调用方使用。

  3. `PrescriptionAuditServiceImpl.doSubmit()`（8m 提取的方法）中将：
     ```java
     // 原两次读取 → 一次快照
     SnapshotResult snapshot = ctx.snapshotCriticalAlerts(prescriptionId);
     if (snapshot.hasAlerts) {
         // 使用 snapshot.alerts 而非 getCriticalAlerts()
         List<String> reasons = snapshot.alerts.stream()
             .map(DosageAlert::getMessage)
             .collect(Collectors.toList());
         ...
     }
     ```

- **涉及文件**：
  - `prescription/.../context/PrescriptionDraftContext.java`
  - `prescription/.../service/audit/impl/PrescriptionAuditServiceImpl.java`
- **测试方案**：
  - `PrescriptionDraftContextTest.java`：新增 `shouldReturnSnapshotWithTrueWhenAlertsExist()`、`shouldReturnSnapshotWithFalseWhenNoAlerts()`、`shouldReturnSnapshotWithFalseWhenValueTypeMismatch()`
  - `PrescriptionAuditServiceImplTest.java`：验证 `submit()` 路径中 `hasCriticalAlerts` 与 `getCriticalAlerts` 基于同一快照（通过 spy prescriptionDraftContext 验证 `snapshotCriticalAlerts` 调用且 `hasCriticalAlerts`/`getCriticalAlerts` 各被调用 0 次）

### P09、T8、T16、A11 归属说明

- **P09**（PrescriptionItem.unit 字段 OOD 补充定义，P2）：已检查 biz 层 `PrescriptionItem.java` 中 `unit` 字段已存在（L11、L64-69），代码无需修改。剩余工作为 OOD 文档规格对齐，已推迟至路线表第 9 项（跨模块 + medical-record P1+P2 批量）。
- **T8**（AllergyCheckRule 跨模块依赖，P1）：涉及跨模块依赖重构（`AllergyCheckRule` 依赖 `AuditRequest`/`PrescriptionItem` 等 consultation 模块类型），非 prescription 单模块可独立完成。8g（T31）已在 `AllergyCheckRule` 内解决子串匹配逻辑缺陷。T8 的跨模块重构已推迟至路线表第 9 项。

## 路线表第 9 项工作量重估

路线表第 9 项「跨模块 + medical-record P1+P2 批量」原预估 4 人时，实际对应约 15+ 项缺陷（含 P09、T8、T16、T18、T19、T20、T21、T22、T47、T48、T50、M02、M11、A06、A09、A08、A11、T24、T5 等），预估明显不足。建议执行时由 Plan Agent 按模块拆分为 2~3 个子轮次（medical-record / cross-module / application），每轮 ≤ 5 人时。

## 涉及文件汇总

| 文件路径（相对 `AIMedical/backend/modules/`） | 操作 | 子项 | 测试文件 |
|------|------|:----:|---------|
| `prescription/.../service/assist/DosageThresholdService.java` | 修改 | 8a, 8k | `DosageThresholdServiceTest.java` |
| `prescription/.../context/PrescriptionDraftContext.java` | 修改 | 8b, 8n | `PrescriptionDraftContextTest.java` |
| `prescription/.../dto/audit/PrescriptionItem.java` | 修改 | 8c | `PrescriptionItemTest.java`, `AuditConverterTest.java`, `DosageLimitRuleTest.java`, `PrescriptionAuditServiceImplTest.java` |
| `prescription/.../rule/DosageLimitRule.java` | 修改 | 8c, 8f | `DosageLimitRuleTest.java` |
| `prescription/.../converter/AuditConverter.java` | 修改 | 8c, 8h | `AuditConverterTest.java` |
| `prescription/.../service/audit/impl/PrescriptionAuditServiceImpl.java` | 修改 | 8d, 8e, 8j, 8m, 8n | `PrescriptionAuditServiceImplTest.java` |
| `prescription/.../service/assist/impl/PrescriptionAssistServiceImpl.java` | 修改 | 8l | `PrescriptionAssistServiceImplTest.java` |
| `prescription/.../rule/AllergyCheckRule.java` | 修改 | 8g | `AllergyCheckRuleTest.java` |
| `ai/ai-api/.../dto/prescription/PrescriptionCheckItem.java` | 修改 | 8h | — |
| `ai/ai-api/.../dto/prescription/PatientInfo.java` | 修改 | 8h | — |
| `prescription/.../rule/DrugInteractionRule.java` | 修改 | 8i | `DrugInteractionRuleTest.java` |
| `prescription/.../api/PrescriptionAuditController.java` | 修改 | 8j | `PrescriptionAuditControllerTest.java` |

## 修订说明（v10 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [一般] 缺少测试修改计划 | 为 10 个子项逐项补充测试方案：8a 新增 DosageThresholdServiceTest 日志断言；8b 新增 PrescriptionDraftContextTest 非法类型路径测试；8c 更新 PrescriptionItemTest/AuditConverterTest/DosageLimitRuleTest/PrescriptionAuditServiceImplTest 中断言类型；8d 更新 PrescriptionAuditServiceImplTest ID 格式验证；8e 确认无测试覆盖被删除的死代码（hasNewAlerts）；8f 不影响现有测试断言；8g 新增 AllergyCheckRuleTest 否定前缀和单词边界测试；8h 更新 AuditConverterTest unit/weight 断言验证；8i 新增 DrugInteractionRuleTest 注解反射验证（assertArrayEquals）；8j 更新 ControllerTest 和 ServiceTest 中 reason 透传验证 |
| [严重] 8c (T14) double→BigDecimal 类型变更级联影响未分析 | 完成全量级联分析：(1) DosageLimitRule.java:40 `BigDecimal.valueOf(item.getDose())` 改为 `item.getDose()`；(2) AuditConverter.java:70 `aiItem.setDose(item.getDose())` 改为 `item.getDose().doubleValue()`；(3) PrescriptionAuditServiceImpl.java:501 `item.getDose()` 在字符串拼接中，BigDecimal.toString() 自动兼容无需修改；(4) 所有测试文件 `setDose(double)` → `setDose(new BigDecimal(...))`：PrescriptionItemTest/AuditConverterTest/DosageLimitRuleTest/PrescriptionAuditServiceImplTest |
| [一般] 8d (T15) UUID 方案未明确 | 明确方案：`"RX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()`，共 11 字符。8 位 hex（32 位随机空间），碰撞概率远低于原 System.currentTimeMillis() 毫秒级碰撞 |
| [一般] 8f (T30) findBestMatch null 后 NPE 风险未分析 | 分析确认 `standings` 在调用 `findBestMatch` 前已通过 `standards.isEmpty()` 保护，`standards.get(0)` 不会 NPE。变更改为仅增 `log.warn` 记录回退行为，不改变返回逻辑。测试 `shouldFallbackToFirstStandardWhenNoMatch` 预期不变 |
| [一般] 8j (P15) "透传具体规则原因"方法签名/兼容性未分析 | 分析确认不涉及方法签名变更：Controller 从 `response.getAlerts()` 提取原因列表传 BlockResponse；Service 从 `ctx.snapshotCriticalAlerts` 提取 alert message 传 BlockResponse。`prescriptionAuditEnforcer.enforce()` 签名不变。未破坏 "不涉及 API 变更" 前提 |

## 修订说明（v10 r2）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] P10、P14、T27 未覆盖 | 已补入 R10 子项清单：8k（P10 DosageThresholdService.matchByPriority 循环抽取 findFirstCandidate + matchesBothRanges）、8l（P14 scheduleSuggestionAsync exceptionally 回调追加 clearCriticalAlerts）、8m（T27 submit 入口 per-prescriptionId ReentrantLock + doSubmit 提取） |
| [一般] P09、T8 未明确归属 | P09（PrescriptionItem.unit OOD 对齐，P2）：已确认 biz 层 unit 字段代码已存在，推迟至第 9 项做文档对齐。T8（AllergyCheckRule 跨模块依赖，P1）：非单模块可独立完成，推迟至第 9 项跨模块轮次。详见「归属说明」 |
| [一般] 第 9 项工作量预估不足（~15+ 项，仅估 4 人时） | 已在「路线表第 9 项工作量重估」中记录，建议执行时按模块拆分为 2~3 个子轮次，每轮 ≤ 5 人时 |

## 修订说明（v10 r3）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] T16（P1, prescription）完全未覆盖 | 已补入 8n（PrescriptionDraftContext 新增 snapshotCriticalAlerts 原子快照方法 + doSubmit 中单次快照替代 has/get 两次读取 + 删除原 hasCriticalAlerts 方法）。详见 8n 节。 |
| [一般] 8l（P14）exceptionally 与 submit() 并发冲突未分析 | 已在 8l 节补充并发风险分析：submit() 内通过 8m 的锁 + 8n 的原子快照消除 TOCTOU 窗口；exceptionally 延迟清理不影响当前 submit，仅影响下一轮。结论：风险可接受。 |
| [一般] Round 9 清单遗漏 A11（P2） | 已在「路线表第 9 项工作量重估」中补充 A11。 |
| [一般] 8a T9 缺少 Logger 字段声明 | 已在 8a 节变更明细中补充 Logger 字段声明 + import 说明。 |
| [轻微] Plan round 编号与路线表序号不一致（R10 vs 第 8 项） | 已在 plan.md 路线表注中加注对应关系（R10 = 路线表第 8 项）。 |
