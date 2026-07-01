# 任务指令（v11）

## 动作
RETRY

## 任务描述
修复 prescription 模块 R10 验证失败的 5 项测试错误（3 ERROR + 2 FAILURE），仅修改测试代码和 AllergyCheckRule.hasNegationPrefix 方法。

## 选择理由
v10 代码实现本身逻辑正确，5 项失败均为测试代码的 bug 或源文件中的小缺陷（hasNegationPrefix 大小写敏感），无需重做任何完整 8a-8n 子项。

## RETRY 说明

### 失败原因与修正方向

#### F1: PrescriptionDraftContextTest.updateCriticalAlertsShouldPutWhenNonEmpty（line 116）
- **根因**：Mockito matcher 混用。`verify(cleanupTask).recordWrite("rx-001:criticalAlerts", any(Instant.class))` 中，第一个参数为 raw value `"rx-001:criticalAlerts"`，第二个参数为 matcher `any(Instant.class)`。Mockito 不允许 raw value 与 matcher 混用。
- **修正**：改为 `verify(cleanupTask).recordWrite(eq("rx-001:criticalAlerts"), any(Instant.class))`，导入 `eq` 已在 `import static org.mockito.Mockito.*` 中。
- **涉及文件**：`prescription/src/test/java/.../context/PrescriptionDraftContextTest.java`

#### F2/F3: AuditConverterTest.shouldMapWeightFieldToAiPatientInfo（line 97）& shouldMapWeightAsNullWhenNotSet（line 130）
- **根因**：`request.setPrescriptionItems(List.of(new PrescriptionItem()))` 创建的 PrescriptionItem 未设 dose 字段。`PrescriptionItem.dose` 类型从 `double`（默认 0.0）改为 `BigDecimal`（默认 null），导致 `AuditConverter.toAiCheckItem` 第 70 行 `item.getDose().doubleValue()` 抛 NPE。
- **修正**：两条测试中补 `new PrescriptionItem()` → 先设 dose 再放入列表：
  ```java
  PrescriptionItem item = new PrescriptionItem();
  item.setDose(BigDecimal.valueOf(100));
  request.setPrescriptionItems(List.of(item));
  ```
- **涉及文件**：`prescription/src/test/java/.../converter/AuditConverterTest.java`
- **新 import**：`import java.math.BigDecimal;`（已存在，无需新增）

#### F4: AllergyCheckRuleTest.shouldSkipWhenNegationPrefixFound（line 244）
- **根因**：`hasNegationPrefix("No allergy to ")` — 前缀 `"No "` 首字母大写，但方法中 `text.contains("no ")` 使用 `String.contains()`（大小写敏感），`"No allergy to "` 不包含 `"no "`，返回 `false`。测试期望 `true` → 断言失败。
- **修正**：AllergyCheckRule.java 第 75-83 行 `hasNegationPrefix` 方法中，将 `text` 转为小写后再检查：
  ```java
  private boolean hasNegationPrefix(String text) {
      String lower = text.toLowerCase();
      String[] negations = {"no ", "not ", "without ", "denies ", "no known "};
      for (String neg : negations) {
          if (lower.endsWith(neg.trim()) || lower.contains(neg)) {
              return true;
          }
      }
      return false;
  }
  ```
- **涉及文件**：`prescription/src/main/java/.../rule/AllergyCheckRule.java`

#### F5: PrescriptionAssistServiceImplTest.asyncSuggestionShouldClearCriticalAlertsOnExceptionally（line 1017）
- **根因**：测试在 `service.assist(assistRequest)` 调用前未设置 `assistRequest.getPrescriptionId()`。`PrescriptionAssistServiceImpl.assist()` 第 91-93 行自动生成 UUID 作为 prescriptionId，而 `clearCriticalAlerts` 第 393 行使用 `request.getPrescriptionId()` 即此 UUID。测试第 1017 行 `verify(prescriptionDraftContext).updateCriticalAlerts(eq("rx-001"), ...)` 期望硬编码值 `"rx-001"`，不匹配实际 UUID `"c4909155-..."`。
- **修正**：第 985 行 `aiRequest.setPrescriptionId("rx-001")` 已有但不够，还需补：
  ```java
  assistRequest.setPrescriptionId("rx-001");
  ```
  加在 `service.assist(assistRequest)` 调用之前（第 1013 行前）。
- **涉及文件**：`prescription/src/test/java/.../service/assist/impl/PrescriptionAssistServiceImplTest.java`

### 涉及文件汇总

| 文件路径（相对 `AIMedical/backend/modules/`） | 操作 | 修复 |
|------|------|:----:|
| `prescription/src/main/java/.../rule/AllergyCheckRule.java` | 修改 | F4 |
| `prescription/src/test/java/.../context/PrescriptionDraftContextTest.java` | 修改 | F1 |
| `prescription/src/test/java/.../converter/AuditConverterTest.java` | 修改 | F2, F3 |
| `prescription/src/test/java/.../service/assist/impl/PrescriptionAssistServiceImplTest.java` | 修改 | F5 |

### 验证方式
在项目根目录执行：
```bash
mvn test -pl modules/prescription -am -q
```
确认 0 失败，0 错误。
