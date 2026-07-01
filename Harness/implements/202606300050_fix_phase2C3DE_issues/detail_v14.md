# 详细设计（v14）

## 概述
修复 medical-record 模块 16 个测试失败（5 个测试文件），仅调整测试断言/数据以匹配现有生产代码行为，不修改生产代码。

## 文件规划
| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/medical-record/src/test/java/com/aimedical/modules/medicalrecord/detector/MissingFieldDetectorImplTest.java` | 修改 | A组8个失败：同步 MissingFieldDetectorImplTest 测试预期以匹配 MedicalRecordField 9 个枚举值 |
| `AIMedical/backend/modules/medical-record/src/test/java/com/aimedical/modules/medicalrecord/template/DatabaseTemplateConfigManagerTest.java` | 修改 | B组5个失败：更新 DEFAULT_TEMPLATE 字段数 7→9；修正 3 个测试的 departmentId 断言为 "dept-01" |
| `AIMedical/backend/modules/medical-record/src/test/java/com/aimedical/modules/medicalrecord/dto/RecordGenerateRequestTest.java` | 修改 | C组1个失败：验证测试对话文本补足至 50+ 字符 |
| `AIMedical/backend/modules/medical-record/src/test/java/com/aimedical/modules/medicalrecord/service/impl/MedicalRecordServiceImplTest.java` | 修改 | D组1个失败：通过 ReflectionTestUtils 注入 @Value 字段默认值 |
| `AIMedical/backend/modules/medical-record/src/test/java/com/aimedical/modules/medicalrecord/converter/MedicalRecordContentConverterTest.java` | 修改 | E组1个失败：调整混合键测试期望值从 1→0 |

## 类型定义

（无需新增类型，仅修改现有测试文件中的断言和测试数据）

## 错误处理
所有修复不涉及生产代码，纯测试断言/数据调整。无需考虑错误处理。

## 行为契约

### A组：MissingFieldDetectorImplTest（8 个失败）

| 测试方法 | 当前（错误） | 修正（正确） | 原因 |
|---------|-------------|-------------|------|
| `shouldDetectMultipleMissingFields` | `assertEquals(3, ...)` | `assertEquals(5, ...)` | fullResponse 只设 7 个临床字段，MISSING_FIELDS/PARTIAL_CONTENT 不在 response 中 → 共 5 个缺失（3 个显式设为 null + 2 个新字段 null） |
| `shouldResolveAllPlaceholdersForAllFields` | `assertEquals(7, ...)` <br>expectedPrompts 只有 7 条 | `assertEquals(9, ...)` <br>expectedPrompts 增加 MISSING_FIELDS→"MISSING_FIELDS字段缺失"、PARTIAL_CONTENT→"PARTIAL_CONTENT字段缺失" | 枚举值 7→9 |
| `shouldResolvePlaceholderInPromptMessage` | `assertEquals("主诉字段缺失", ...)` | 不变 | 修正 fullResponse 后，仅 CHIEF_COMPLAINT 缺失 |
| `shouldReturnEmptyHintsWhenAllFieldsAreFilled` | `assertTrue(hints.isEmpty())` | 不变 | 修正 fullResponse 后，所有 9 个字段均有值 |
| `shouldReturnHintForBlankStringField` | `assertEquals(1, ...)` | 不变 | 修正 fullResponse 后，仅 CHIEF_COMPLAINT 缺失 |
| `shouldReturnHintForEmptyStringField` | `assertEquals(1, ...)` | 不变 | 同上 |
| `shouldReturnHintForNullField` | `assertEquals(1, ...)` | 不变 | 同上 |
| `shouldReturnHintsForAllFieldsWhenAllNull` | `assertEquals(7, ...)` | `assertEquals(9, ...)` | 枚举值 7→9 |

**fullResponse() 修改**：增加 `resp.setMissingFields(List.of("none"));` 和 `resp.setPartialContent(Collections.emptyMap());`，使 MISSING_FIELDS → "none"（非空）、PARTIAL_CONTENT → "{}"（非 null），不被 detector 标记为缺失。

**shouldResolveAllPlaceholdersForAllFields 修改**：增加 expectedPrompts 条目：
- `MedicalRecordField.MISSING_FIELDS` → `"MISSING_FIELDS字段缺失"`
- `MedicalRecordField.PARTIAL_CONTENT` → `"PARTIAL_CONTENT字段缺失"`

### B组：DatabaseTemplateConfigManagerTest（5 个失败）

| 测试方法 | 当前（错误） | 修正（正确） | 原因 |
|---------|-------------|-------------|------|
| `shouldReturnDefaultTemplateWhenDepartmentNotFound` | `assertEquals(7, ...)` | `assertEquals(9, ...)` | DEFAULT_TEMPLATE 使用 `MedicalRecordField.values()` 包含全部 9 个枚举 |
| `defaultTemplateShouldHaveAllSevenFieldsWithPlaceholders` | `assertEquals(7, ...)` | `assertEquals(9, ...)` | 同上 |
| `shouldReturnDefaultTemplateOnParseErrorInRequiredFields` | `assertEquals("DEFAULT", ...)` | `assertEquals("dept-01", ...)` | `parseRequiredFields` 内部 catch 返回 emptySet，`loadFromDatabase` 不抛异常，使用 entity 的 departmentId="dept-01" |
| `shouldReturnDefaultTemplateWhenEnumNameInvalid` | `assertEquals("DEFAULT", ...)` | `assertEquals("dept-01", ...)` | 同上 |
| `shouldReturnDefaultTemplateWhenNullRequiredFields` | `assertEquals("DEFAULT", ...)` | `assertEquals("dept-01", ...)` | 同上 |

### C组：RecordGenerateRequestTest（1 个失败）

| 测试方法 | 修改点 | 原因 |
|---------|-------|------|
| `shouldPassValidationWithValidDialogueText` | 第 95 行 `dialogueText` 改为 `"A".repeat(50)` 或 50+ 字符文本 | `@Size(min=50)` 验证，原文本约 37 字符不足 |

### D组：MedicalRecordServiceImplTest（1 个失败）

| 测试方法 | 修改点 | 原因 |
|---------|-------|------|
| `shouldReturnSuccessOnNormalFlow` | `setUp()` 中增加 `ReflectionTestUtils.setField(service, "aiTimeout", 12)` 和 `ReflectionTestUtils.setField(service, "visitFacadeTimeout", 2)` | @Value 字段在纯 JUnit 中默认为 0，导致 `resolveVisitId` 的 `future.get(0, ...)` 立即 TimeoutException → `visitIdFallback=true` → `isFromFallback()=true`，与 `assertFalse` 断言相反 |

需增加 import: `org.springframework.test.util.ReflectionTestUtils`

### E组：MedicalRecordContentConverterTest（1 个失败）

| 测试方法 | 当前（错误） | 修正（正确） | 原因 |
|---------|-------------|-------------|------|
| `convertToEntityAttributeShouldHandleMixedKnownAndUnknownKeys` | `assertEquals(1, map.size())` | `assertEquals(0, map.size())` | `MedicalRecordField.valueOf("UNKNOWN_FIELD")` 抛出 IAE，外层 catch 捕获后返回 `Collections.emptyMap()`，丢弃整张 map |

## 依赖关系

- `org.springframework.test.util.ReflectionTestUtils`：D 组修复需要，检查项目是否已有该依赖（spring-test 传递依赖，通常可用）
- 所有修改均为测试文件，无生产代码依赖变更

## 验证条件
所有 5 个测试文件各自通过（16 个失败清零），全量 `mvn test -pl modules/medical-record -am` 通过。
