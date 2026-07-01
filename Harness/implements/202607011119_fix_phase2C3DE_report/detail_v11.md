# 详细设计（v11）

## 概述

修复 prescription 模块 R10 验证失败的 5 项测试错误（3 ERROR + 2 FAILURE）。仅修改测试代码和 `AllergyCheckRule.hasNegationPrefix` 方法，无需重做任何完整 8a-8n 子项。

## 文件规划

| 文件路径（相对 `AIMedical/backend/modules/`） | 操作 | 修复 |
|---------|------|:----:|
| `prescription/src/main/java/.../rule/AllergyCheckRule.java` | 修改 | F4 |
| `prescription/src/test/java/.../context/PrescriptionDraftContextTest.java` | 修改 | F1 |
| `prescription/src/test/java/.../converter/AuditConverterTest.java` | 修改 | F2, F3 |
| `prescription/src/test/java/.../service/assist/impl/PrescriptionAssistServiceImplTest.java` | 修改 | F5 |

## 类型定义

### F1. PrescriptionDraftContextTest.updateCriticalAlertsShouldPutWhenNonEmpty

**形态**：测试方法，单行修改
**包路径**：`com.aimedical.modules.prescription.context.PrescriptionDraftContextTest`
**文件路径**：`prescription/src/test/java/.../context/PrescriptionDraftContextTest.java`
**变更**：第 116 行 `verify(cleanupTask).recordWrite("rx-001:criticalAlerts", any(Instant.class))` 中，raw value 与 Mockito matcher 混用。改为 `verify(cleanupTask).recordWrite(eq("rx-001:criticalAlerts"), any(Instant.class))`。
- `eq` 已包含在 `import static org.mockito.Mockito.*` 中，无需新增 import。

### F2/F3. AuditConverterTest.shouldMapWeightFieldToAiPatientInfo & shouldMapWeightAsNullWhenNotSet

**形态**：两处测试方法，各 2 行修改
**包路径**：`com.aimedical.modules.prescription.converter.AuditConverterTest`
**文件路径**：`prescription/src/test/java/.../converter/AuditConverterTest.java`
**变更**：两处 `request.setPrescriptionItems(List.of(new PrescriptionItem()))` 中的 `new PrescriptionItem()` 未设 dose 字段，`PrescriptionItem.dose` 已从 `double`（默认 0.0）改为 `BigDecimal`（默认 null），导致 `AuditConverter.toAiCheckItem` 第 70 行 `item.getDose().doubleValue()` 抛 NPE。
- 改为：
  ```java
  PrescriptionItem item = new PrescriptionItem();
  item.setDose(BigDecimal.valueOf(100));
  request.setPrescriptionItems(List.of(item));
  ```
- `import java.math.BigDecimal;` 已存在，无需新增。

### F4. AllergyCheckRule.hasNegationPrefix

**形态**：私有方法，逻辑微调
**包路径**：`com.aimedical.modules.prescription.rule.AllergyCheckRule`
**文件路径**：`prescription/src/main/java/.../rule/AllergyCheckRule.java`
**变更**：`hasNegationPrefix` 方法（第 75-83 行）使用 `String.contains()`（大小写敏感），而测试传入 `"No allergy to "` 首字母大写，包含 `"No "` 前缀但不包含 `"no "`，导致返回 false。
- 将输入 `text` 转为小写后再检查：
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
- 无新增 import（所有类型均为 java.lang.String）。

### F5. PrescriptionAssistServiceImplTest.asyncSuggestionShouldClearCriticalAlertsOnExceptionally

**形态**：测试方法，1 行新增
**包路径**：`com.aimedical.modules.prescription.service.assist.impl.PrescriptionAssistServiceImplTest`
**文件路径**：`prescription/src/test/java/.../service/assist/impl/PrescriptionAssistServiceImplTest.java`
**变更**：第 1013 行 `service.assist(assistRequest)` 调用前，`assistRequest.getPrescriptionId()` 为 null。`PrescriptionAssistServiceImpl.assist()` 第 91-93 行自动生成 UUID 作为 prescriptionId，而 `clearCriticalAlerts` 第 393 行使用 `request.getPrescriptionId()` 即此 UUID。测试第 1017 行 `verify(prescriptionDraftContext).updateCriticalAlerts(eq("rx-001"), ...)` 期望硬编码值 `"rx-001"`，不匹配实际 UUID。
- 在第 1013 行前新增：
  ```java
  assistRequest.setPrescriptionId("rx-001");
  ```
- 无新增 import（`PrescriptionAssistRequest.setPrescriptionId` 已是当前类可见）。

## 错误处理

| 修复 | 策略 |
|------|------|
| F1 | Mockito matcher 混用 → 统一使用 `eq()` 包裹 raw value |
| F2/F3 | NPE（BigDecimal null）→ 测试中补 `setDose(BigDecimal.valueOf(100))` |
| F4 | `String.contains()` 大小写敏感 → `toLowerCase()` 统一转换 |
| F5 | prescriptionId 不匹配 → 测试中主动 `setPrescriptionId("rx-001")` |

## 行为契约

- 所有 5 项修改均为纯缺陷修复，不改变功能逻辑或 API 语义。
- `AllergyCheckRule.hasNegationPrefix` 行为：对任何大小写的否定前缀（`"no "`, `"No "`, `"NO "` 等）均生效。
- 修改后执行 `mvn test -pl modules/prescription -am -q` 应 0 失败、0 错误。

## 依赖关系

| 依赖 | 说明 |
|------|------|
| `org.mockito.Mockito.eq` | F1 需要（已通过 `import static org.mockito.Mockito.*` 包含） |
| `java.math.BigDecimal` | F2/F3 需要（已在 AuditConverterTest 中存在） |

无新外部依赖。

## 修订说明（v11 r1）

| 审查意见 | 修改措施 |
|---------|---------|
| 首版设计，无审查意见 | 直接输出 v11 详细设计规格 |
