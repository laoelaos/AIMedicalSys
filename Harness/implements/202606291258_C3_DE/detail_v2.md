# 详细设计（v2）

## 概述

在 ai-api 模块的 `dto/prescription` 包中扩展处方审核（PrescriptionCheck）和辅助开方（PrescriptionAssist）相关 DTO。涉及 4 个已有空壳类的字段扩展、8 个新增子 DTO、1 个新增辅助 DTO（PatientInfo），所有类均按已有风格采用默认构造器 + getter/setter。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/prescription/PrescriptionCheckRequest.java` | 修改 | 扩展 prescriptionItems、patientInfo、prescriptionId 字段 |
| `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/prescription/PrescriptionCheckResponse.java` | 修改 | 扩展 riskLevel、alerts、interactions、suggestions、fromFallback 字段 |
| `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/prescription/PrescriptionAssistRequest.java` | 修改 | 扩展 diagnosis、examResults、patientInfo、existingPrescription、prescriptionId、encounterId 字段 |
| `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/prescription/PrescriptionAssistResponse.java` | 修改 | 扩展 prescriptionDraft、doseWarnings、allergyWarnings、errorCode、disclaimerRequired 字段 |
| `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/prescription/PrescriptionCheckItem.java` | 新建 | 处方检查项 DTO |
| `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/prescription/AllergyDetailItem.java` | 新建 | 过敏详情项 DTO |
| `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/prescription/DrugInteractionItem.java` | 新建 | 药物相互作用 DTO |
| `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/prescription/AlertItem.java` | 新建 | 风险提示项 DTO |
| `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/prescription/SuggestionItem.java` | 新建 | 用药建议项 DTO |
| `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/prescription/ExamResultItem.java` | 新建 | 检查检验结果项 DTO |
| `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/prescription/DoseWarningItem.java` | 新建 | 剂量告警项 DTO |
| `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/prescription/AllergyWarningItem.java` | 新建 | 过敏告警项 DTO |
| `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/prescription/PatientInfo.java` | 新建 | 患者信息辅助 DTO |

## 类型定义

### PrescriptionCheckItem

**形态**：class
**包路径**：com.aimedical.modules.ai.api.dto.prescription
**职责**：表示处方审核请求中的单条用药项

```java
public class PrescriptionCheckItem {
    private String drugId;
    private String drugName;
    private double dose;
    private String frequency;
    private String duration;
    private String route;
}
```

**公开接口**：
- 默认构造器 `public PrescriptionCheckItem()`
- Getter: `getDrugId()` / `getDrugName()` 返回 `String`；`getDose()` 返回 `double`；`getFrequency()` / `getDuration()` / `getRoute()` 返回 `String`
- Setter: `setDrugId(String)` / `setDrugName(String)` / `setDose(double)` / `setFrequency(String)` / `setDuration(String)` / `setRoute(String)`

**构造方式**：默认构造器
**类型关系**：无

### AllergyDetailItem

**形态**：class
**包路径**：com.aimedical.modules.ai.api.dto.prescription
**职责**：表示患者过敏详情中的单条记录

```java
public class AllergyDetailItem {
    private String allergen;
    private String reactionType;
    private String severity;    // "MILD"/"MODERATE"/"SEVERE"
    private String occurredAt;  // ISO date
}
```

**公开接口**：
- 默认构造器 `public AllergyDetailItem()`
- Getter: `getAllergen()` / `getReactionType()` / `getSeverity()` / `getOccurredAt()` 返回 `String`
- Setter: `setAllergen(String)` / `setReactionType(String)` / `setSeverity(String)` / `setOccurredAt(String)`

**构造方式**：默认构造器
**类型关系**：被 PatientInfo 引用（allergyDetails 字段）

### DrugInteractionItem

**形态**：class
**包路径**：com.aimedical.modules.ai.api.dto.prescription
**职责**：表示药物相互作用信息

```java
public class DrugInteractionItem {
    private String drugPair;
    private String severity;     // "INFO"/"WARNING"/"CRITICAL"
    private String description;
}
```

**公开接口**：
- 默认构造器 `public DrugInteractionItem()`
- Getter: `getDrugPair()` / `getSeverity()` / `getDescription()` 返回 `String`
- Setter: `setDrugPair(String)` / `setSeverity(String)` / `setDescription(String)`

**构造方式**：默认构造器
**类型关系**：被 PrescriptionCheckResponse 引用

### AlertItem

**形态**：class
**包路径**：com.aimedical.modules.ai.api.dto.prescription
**职责**：表示处方审核中的风险提示

```java
public class AlertItem {
    private String alertCode;
    private String alertMessage;
    private String severity;     // "INFO"/"WARNING"/"CRITICAL"
}
```

**公开接口**：
- 默认构造器 `public AlertItem()`
- Getter: `getAlertCode()` / `getAlertMessage()` / `getSeverity()` 返回 `String`
- Setter: `setAlertCode(String)` / `setAlertMessage(String)` / `setSeverity(String)`

**构造方式**：默认构造器
**类型关系**：被 PrescriptionCheckResponse 引用

### SuggestionItem

**形态**：class
**包路径**：com.aimedical.modules.ai.api.dto.prescription
**职责**：表示用药建议

```java
public class SuggestionItem {
    private String suggestionCode;
    private String suggestionText;
}
```

**公开接口**：
- 默认构造器 `public SuggestionItem()`
- Getter: `getSuggestionCode()` / `getSuggestionText()` 返回 `String`
- Setter: `setSuggestionCode(String)` / `setSuggestionText(String)`

**构造方式**：默认构造器
**类型关系**：被 PrescriptionCheckResponse 引用

### ExamResultItem

**形态**：class
**包路径**：com.aimedical.modules.ai.api.dto.prescription
**职责**：表示检查检验结果

```java
public class ExamResultItem {
    private String itemName;
    private String itemValue;
    private String referenceRange;
}
```

**公开接口**：
- 默认构造器 `public ExamResultItem()`
- Getter: `getItemName()` / `getItemValue()` / `getReferenceRange()` 返回 `String`
- Setter: `setItemName(String)` / `setItemValue(String)` / `setReferenceRange(String)`

**构造方式**：默认构造器
**类型关系**：被 PrescriptionAssistRequest 引用

### DoseWarningItem

**形态**：class
**包路径**：com.aimedical.modules.ai.api.dto.prescription
**职责**：表示剂量告警

```java
public class DoseWarningItem {
    private String drugId;
    private String warningType;  // "OVER_SINGLE_DOSE"/"OVER_DAILY_DOSE"/"OVER_DURATION"
    private String message;
    private String severity;     // "INFO"/"WARNING"/"CRITICAL"
}
```

**公开接口**：
- 默认构造器 `public DoseWarningItem()`
- Getter: `getDrugId()` / `getWarningType()` / `getMessage()` / `getSeverity()` 返回 `String`
- Setter: `setDrugId(String)` / `setWarningType(String)` / `setMessage(String)` / `setSeverity(String)`

**构造方式**：默认构造器
**类型关系**：被 PrescriptionAssistResponse 引用

### AllergyWarningItem

**形态**：class
**包路径**：com.aimedical.modules.ai.api.dto.prescription
**职责**：表示过敏告警

```java
public class AllergyWarningItem {
    private String drugId;
    private String allergen;
    private String severity;     // "INFO"/"WARNING"/"HIGH"
}
```

**公开接口**：
- 默认构造器 `public AllergyWarningItem()`
- Getter: `getDrugId()` / `getAllergen()` / `getSeverity()` 返回 `String`
- Setter: `setDrugId(String)` / `setAllergen(String)` / `setSeverity(String)`

**构造方式**：默认构造器
**类型关系**：被 PrescriptionAssistResponse 引用

### PatientInfo

**形态**：class
**包路径**：com.aimedical.modules.ai.api.dto.prescription
**职责**：处方相关请求中内嵌的患者信息

```java
public class PatientInfo {
    private String patientId;
    private Integer age;
    private String gender;
    private String allergyHistory;                 // text, comma-separated
    private List<AllergyDetailItem> allergyDetails;
    private List<String> comorbidities;
}
```

**公开接口**：
- 默认构造器 `public PatientInfo()`
- Getter: `getPatientId()` / `getGender()` / `getAllergyHistory()` 返回 `String`；`getAge()` 返回 `Integer`；`getAllergyDetails()` 返回 `List<AllergyDetailItem>`；`getComorbidities()` 返回 `List<String>`
- Setter: `setPatientId(String)` / `setAge(Integer)` / `setGender(String)` / `setAllergyHistory(String)` / `setAllergyDetails(List<AllergyDetailItem>)` / `setComorbidities(List<String>)`

**构造方式**：默认构造器
**类型关系**：依赖 `AllergyDetailItem`（同包）；被 `PrescriptionCheckRequest` 和 `PrescriptionAssistRequest` 引用

### PrescriptionCheckRequest（修改）

**形态**：class
**包路径**：com.aimedical.modules.ai.api.dto.prescription
**职责**：处方审核请求 DTO

```java
public class PrescriptionCheckRequest {
    private List<PrescriptionCheckItem> prescriptionItems;
    private PatientInfo patientInfo;
    private String prescriptionId;
}
```

**新增字段**（替换空壳，全部为新字段）：

| 字段 | 类型 | 说明 |
|------|------|------|
| prescriptionItems | `List<PrescriptionCheckItem>` | 可选，处方用药项列表 |
| patientInfo | `PatientInfo` | 可选，患者信息 |
| prescriptionId | `String` | 可选，处方 ID |

**新增 getter/setter**：
- `getPrescriptionItems()` / `setPrescriptionItems(List<PrescriptionCheckItem>)`
- `getPatientInfo()` / `setPatientInfo(PatientInfo)`
- `getPrescriptionId()` / `setPrescriptionId(String)`

**新增 import**：`java.util.List`

### PrescriptionCheckResponse（修改）

**形态**：class
**包路径**：com.aimedical.modules.ai.api.dto.prescription
**职责**：处方审核响应 DTO

```java
public class PrescriptionCheckResponse {
    private String riskLevel;                 // "LOW"/"MEDIUM"/"HIGH"
    private List<AlertItem> alerts;
    private List<DrugInteractionItem> interactions;
    private List<SuggestionItem> suggestions;
    private boolean fromFallback;
}
```

**新增字段**（替换空壳，全部为新字段）：

| 字段 | 类型 | 说明 |
|------|------|------|
| riskLevel | `String` | 风险等级 |
| alerts | `List<AlertItem>` | 可选，风险提示列表 |
| interactions | `List<DrugInteractionItem>` | 可选，药物相互作用列表 |
| suggestions | `List<SuggestionItem>` | 可选，用药建议列表 |
| fromFallback | `boolean` | 是否来自降级策略 |

**新增 getter/setter**：
- `getRiskLevel()` / `setRiskLevel(String)`
- `getAlerts()` / `setAlerts(List<AlertItem>)`
- `getInteractions()` / `setInteractions(List<DrugInteractionItem>)`
- `getSuggestions()` / `setSuggestions(List<SuggestionItem>)`
- `isFromFallback()` / `setFromFallback(boolean)`

**新增 import**：`java.util.List`

### PrescriptionAssistRequest（修改）

**形态**：class
**包路径**：com.aimedical.modules.ai.api.dto.prescription
**职责**：辅助开方请求 DTO

```java
public class PrescriptionAssistRequest {
    private String diagnosis;
    private List<ExamResultItem> examResults;
    private PatientInfo patientInfo;
    private String existingPrescription;     // JSON text
    private String prescriptionId;
    private String encounterId;
}
```

**新增字段**（替换空壳，全部为新字段）：

| 字段 | 类型 | 说明 |
|------|------|------|
| diagnosis | `String` | 可选，诊断信息 |
| examResults | `List<ExamResultItem>` | 可选，检查检验结果列表 |
| patientInfo | `PatientInfo` | 可选，患者信息 |
| existingPrescription | `String` | 可选，现有处方的 JSON 文本 |
| prescriptionId | `String` | 可选，处方 ID |
| encounterId | `String` | 可选，就诊 ID |

**新增 getter/setter**：
- `getDiagnosis()` / `setDiagnosis(String)`
- `getExamResults()` / `setExamResults(List<ExamResultItem>)`
- `getPatientInfo()` / `setPatientInfo(PatientInfo)`
- `getExistingPrescription()` / `setExistingPrescription(String)`
- `getPrescriptionId()` / `setPrescriptionId(String)`
- `getEncounterId()` / `setEncounterId(String)`

**新增 import**：`java.util.List`

### PrescriptionAssistResponse（修改）

**形态**：class
**包路径**：com.aimedical.modules.ai.api.dto.prescription
**职责**：辅助开方响应 DTO

```java
public class PrescriptionAssistResponse {
    private String prescriptionDraft;           // JSON text
    private List<DoseWarningItem> doseWarnings;
    private List<AllergyWarningItem> allergyWarnings;
    private String errorCode;
    private boolean disclaimerRequired;
}
```

**新增字段**（替换空壳，全部为新字段）：

| 字段 | 类型 | 说明 |
|------|------|------|
| prescriptionDraft | `String` | 可选，处方草稿 JSON 文本 |
| doseWarnings | `List<DoseWarningItem>` | 可选，剂量告警列表 |
| allergyWarnings | `List<AllergyWarningItem>` | 可选，过敏告警列表 |
| errorCode | `String` | 可选，错误码 |
| disclaimerRequired | `boolean` | 是否需要免责声明 |

**新增 getter/setter**：
- `getPrescriptionDraft()` / `setPrescriptionDraft(String)`
- `getDoseWarnings()` / `setDoseWarnings(List<DoseWarningItem>)`
- `getAllergyWarnings()` / `setAllergyWarnings(List<AllergyWarningItem>)`
- `getErrorCode()` / `setErrorCode(String)`
- `isDisclaimerRequired()` / `setDisclaimerRequired(boolean)`

**新增 import**：`java.util.List`

## 错误处理

纯 DTO 扩展，不涉及业务错误处理。所有字段通过 setter 设置，无受检异常抛出。

## 行为契约

- 所有 DTO 字段通过默认构造器 + setter 设置、getter 读取，无调用顺序约束
- List 类型字段默认值为 null（可选语义）
- String 类型字段默认值为 null
- boolean 类型字段默认值为 false
- double 类型字段默认值为 0.0（PrescriptionCheckItem.dose）
- Integer 类型字段默认值为 null（PatientInfo.age，使用 boxed 类型以支持 null）
- severity、warningType、riskLevel 等枚举语义字段使用 String 承载值（ai-api 层避免引入业务枚举依赖）

## 依赖关系

- **PrescriptionCheckRequest** 依赖 `PrescriptionCheckItem`、`PatientInfo`（同包）
- **PrescriptionCheckResponse** 依赖 `AlertItem`、`DrugInteractionItem`、`SuggestionItem`（同包）
- **PrescriptionAssistRequest** 依赖 `ExamResultItem`、`PatientInfo`（同包）
- **PrescriptionAssistResponse** 依赖 `DoseWarningItem`、`AllergyWarningItem`（同包）
- **PatientInfo** 依赖 `AllergyDetailItem`（同包）
- 所有类均位于 `com.aimedical.modules.ai.api.dto.prescription` 包内，无跨模块依赖
