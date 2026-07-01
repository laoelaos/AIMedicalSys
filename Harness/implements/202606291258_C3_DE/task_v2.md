# 任务指令（v2）

## 动作
NEW

## 任务描述
在 ai-api 模块中扩展处方审核和辅助开方相关 DTO：

**已有文件需扩展字段**（4 个，当前为空壳类）：
- `ai-api/.../dto/prescription/PrescriptionCheckRequest.java`
- `ai-api/.../dto/prescription/PrescriptionCheckResponse.java`
- `ai-api/.../dto/prescription/PrescriptionAssistRequest.java`
- `ai-api/.../dto/prescription/PrescriptionAssistResponse.java`

**新建 DTO 文件**（8 个，均在 `dto/prescription/` 包下）：
- `PrescriptionCheckItem.java` — 处方检查项，含 drugId/drugName/dose/frequency/duration/route
- `AllergyDetailItem.java` — 过敏详情项，含 allergen/reactionType/severity(枚举)/occurredAt
- `DrugInteractionItem.java` — 药物相互作用，含 drugPair/severity/description
- `AlertItem.java` — 风险提示项，含 alertCode/alertMessage/severity(枚举)
- `SuggestionItem.java` — 用药建议项，含 suggestionCode/suggestionText
- `ExamResultItem.java` — 检查检验结果项，含 itemName/itemValue/referenceRange
- `DoseWarningItem.java` — 剂量告警项，含 drugId/warningType(枚举)/message/severity(枚举)
- `AllergyWarningItem.java` — 过敏告警项，含 drugId/allergen/severity(枚举)

## 选择理由
T1（分诊 DTO）已通过验证。处方审核和辅助开方共享 prescription 模块，DTO 扩展是其前置依赖。T2 和 T3 可并行开发？

## 任务上下文

### PrescriptionCheckRequest（ai-api 层）需扩展字段
```java
// 包：com.aimedical.modules.ai.api.dto.prescription
public class PrescriptionCheckRequest {
    private List<PrescriptionCheckItem> prescriptionItems;
    private PatientInfo patientInfo;
    private String prescriptionId;
}
```

### PrescriptionCheckResponse（ai-api 层）需扩展字段
```java
public class PrescriptionCheckResponse {
    private String riskLevel;             // "LOW"/"MEDIUM"/"HIGH"
    private List<AlertItem> alerts;
    private List<DrugInteractionItem> interactions;
    private List<SuggestionItem> suggestions;
    private boolean fromFallback;
}
```

### PrescriptionAssistRequest（ai-api 层）需扩展字段
```java
public class PrescriptionAssistRequest {
    private String diagnosis;
    private List<ExamResultItem> examResults;
    private PatientInfo patientInfo;
    private String existingPrescription;  // JSON text of existing prescription
    private String prescriptionId;
    private String encounterId;
}
```

### PrescriptionAssistResponse（ai-api 层）需扩展字段
```java
public class PrescriptionAssistResponse {
    private String prescriptionDraft;     // JSON text of draft prescription
    private List<DoseWarningItem> doseWarnings;
    private List<AllergyWarningItem> allergyWarnings;
    private String errorCode;
    private boolean disclaimerRequired;
}
```

### 新增 DTO 定义

**PrescriptionCheckItem** — `com.aimedical.modules.ai.api.dto.prescription`
```java
public class PrescriptionCheckItem {
    private String drugId;
    private String drugName;
    private double dose;
    private String frequency;
    private String duration;
    private String route;                 // route of administration
}
```

**AllergyDetailItem** — `com.aimedical.modules.ai.api.dto.prescription`
```java
public class AllergyDetailItem {
    private String allergen;
    private String reactionType;
    private String severity;              // "MILD"/"MODERATE"/"SEVERE"
    private String occurredAt;            // ISO date
}
```

**DrugInteractionItem** — `com.aimedical.modules.ai.api.dto.prescription`
```java
public class DrugInteractionItem {
    private String drugPair;
    private String severity;              // "INFO"/"WARNING"/"CRITICAL"
    private String description;
}
```

**AlertItem** — `com.aimedical.modules.ai.api.dto.prescription`
```java
public class AlertItem {
    private String alertCode;
    private String alertMessage;
    private String severity;              // "INFO"/"WARNING"/"CRITICAL"
}
```

**SuggestionItem** — `com.aimedical.modules.ai.api.dto.prescription`
```java
public class SuggestionItem {
    private String suggestionCode;
    private String suggestionText;
}
```

**ExamResultItem** — `com.aimedical.modules.ai.api.dto.prescription`
```java
public class ExamResultItem {
    private String itemName;
    private String itemValue;
    private String referenceRange;
}
```

**DoseWarningItem** — `com.aimedical.modules.ai.api.dto.prescription`
```java
public class DoseWarningItem {
    private String drugId;
    private String warningType;           // "OVER_SINGLE_DOSE"/"OVER_DAILY_DOSE"/"OVER_DURATION"
    private String message;
    private String severity;              // "INFO"/"WARNING"/"CRITICAL"
}
```

**AllergyWarningItem** — `com.aimedical.modules.ai.api.dto.prescription`
```java
public class AllergyWarningItem {
    private String drugId;
    private String allergen;
    private String severity;              // "INFO"/"WARNING"/"HIGH"
}
```

### PatientInfo（ai-api 层辅助 DTO）
因 `PrescriptionCheckRequest` 和 `PrescriptionAssistRequest` 均含 patientInfo，需新增内嵌 DTO：

`com.aimedical.modules.ai.api.dto.prescription.PatientInfo.java`
```java
public class PatientInfo {
    private String patientId;
    private Integer age;
    private String gender;
    private String allergyHistory;        // text, comma-separated
    private List<AllergyDetailItem> allergyDetails;
    private List<String> comorbidities;
}
```

### 所有字段通过默认构造器 + getter/setter 访问
- 所有 List 类型字段默认值为 null（可选语义）
- String 类型字段默认值为 null
- boolean 类型字段默认值为 false
- int/double 等原始类型字段默认值为 0/0.0（PrescriptionCheckItem.dose）
- severity 等枚举语义字段使用 String 类型承载值（ai-api 层避免引入业务枚举依赖）

### 依赖关系
- PrescriptionCheckRequest 依赖 PrescriptionCheckItem、PatientInfo（同包）
- PrescriptionCheckResponse 依赖 AlertItem、DrugInteractionItem、SuggestionItem（同包）
- PrescriptionAssistRequest 依赖 ExamResultItem、PatientInfo（同包）
- PrescriptionAssistResponse 依赖 DoseWarningItem、AllergyWarningItem（同包）
- PatientInfo 依赖 AllergyDetailItem（同包）
- 所有类均位于 ai-api 模块内，无跨模块依赖

## 已有代码上下文
- 4 个已有文件均为空壳类（仅含默认构造器）
- 已有 AiResult.java 和 AiResultFactory.java（T1 新增）
- 测试框架为 JUnit 5（Jupiter），参考 TriageDtoTest.java
