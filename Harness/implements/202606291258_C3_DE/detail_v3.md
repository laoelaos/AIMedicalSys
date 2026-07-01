# 详细设计（v3）

## 概述

在 ai-api 模块的 `dto/medicalrecord` 包中扩展病历生成相关 DTO。涉及 2 个已有空壳类的字段扩展，无新增子 DTO。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/medicalrecord/MedicalRecordGenRequest.java` | 修改 | 扩展 dialogueText、patientId、encounterId、stream、departmentId 字段 |
| `AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/medicalrecord/MedicalRecordGenResponse.java` | 修改 | 扩展 chiefComplaint、symptomDescription、presentIllness、pastHistory、physicalExam、preliminaryDiagnosis、treatmentPlan、missingFields、partialContent 字段 |

## 类型定义

### MedicalRecordGenRequest（修改）

**形态**：class
**包路径**：com.aimedical.modules.ai.api.dto.medicalrecord
**职责**：病历生成请求 DTO

```java
package com.aimedical.modules.ai.api.dto.medicalrecord;

public class MedicalRecordGenRequest {
    private String dialogueText;
    private String patientId;
    private String encounterId;
    private boolean stream;
    private String departmentId;
}
```

**公开接口**：
- 默认构造器 `public MedicalRecordGenRequest()`
- Getter: `getDialogueText()` / `getPatientId()` / `getEncounterId()` / `getDepartmentId()` 返回 `String`；`isStream()` 返回 `boolean`
- Setter: `setDialogueText(String)` / `setPatientId(String)` / `setEncounterId(String)` / `setStream(boolean)` / `setDepartmentId(String)`

**新增字段**（替换空壳，全部为新字段）：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| dialogueText | `String` | 是 | 医患对话原始文本 |
| patientId | `String` | 是 | 患者 ID |
| encounterId | `String` | 否 | 就诊标识 |
| stream | `boolean` | 否 | 流式输出标记，默认 false |
| departmentId | `String` | 否 | 科室 ID |

**构造方式**：默认构造器
**类型关系**：无

### MedicalRecordGenResponse（修改）

**形态**：class
**包路径**：com.aimedical.modules.ai.api.dto.medicalrecord
**职责**：病历生成响应 DTO

```java
package com.aimedical.modules.ai.api.dto.medicalrecord;

import java.util.List;

public class MedicalRecordGenResponse {
    private String chiefComplaint;
    private String symptomDescription;
    private String presentIllness;
    private String pastHistory;
    private String physicalExam;
    private String preliminaryDiagnosis;
    private String treatmentPlan;
    private List<String> missingFields;
    private Object partialContent;
}
```

**公开接口**：
- 默认构造器 `public MedicalRecordGenResponse()`
- Getter: `getChiefComplaint()` / `getSymptomDescription()` / `getPresentIllness()` / `getPastHistory()` / `getPhysicalExam()` / `getPreliminaryDiagnosis()` / `getTreatmentPlan()` 返回 `String`；`getMissingFields()` 返回 `List<String>`；`getPartialContent()` 返回 `Object`
- Setter: `setChiefComplaint(String)` / `setSymptomDescription(String)` / `setPresentIllness(String)` / `setPastHistory(String)` / `setPhysicalExam(String)` / `setPreliminaryDiagnosis(String)` / `setTreatmentPlan(String)` / `setMissingFields(List<String>)` / `setPartialContent(Object)`

**新增字段**（替换空壳，全部为新字段）：

| 字段 | 类型 | 说明 |
|------|------|------|
| chiefComplaint | `String` | 主诉 |
| symptomDescription | `String` | 症状描述 |
| presentIllness | `String` | 现病史 |
| pastHistory | `String` | 既往史 |
| physicalExam | `String` | 体格检查 |
| preliminaryDiagnosis | `String` | 初步诊断 |
| treatmentPlan | `String` | 治疗意见 |
| missingFields | `List<String>` | 可选，缺失字段名称列表 |
| partialContent | `Object` | 可选，超时降级时携带部分已生成字段 |

**新增 import**：`java.util.List`

**构造方式**：默认构造器
**类型关系**：无

## 错误处理

纯 DTO 扩展，不涉及业务错误处理。所有字段通过 setter 设置，无受检异常抛出。

## 行为契约

- 所有字段通过默认构造器 + setter 设置、getter 读取，无调用顺序约束
- String 类型字段默认值为 null
- boolean 类型字段默认值为 false（stream）
- List 类型字段默认值为 null（可选语义）
- Object 类型字段默认值为 null（partialContent，ai-api 层不引入具体类型以保持低耦合）

## 依赖关系

- MedicalRecordGenRequest 和 MedicalRecordGenResponse 均位于 `com.aimedical.modules.ai.api.dto.medicalrecord` 包内，无跨模块依赖
- MedicalRecordGenResponse 需引入 `java.util.List`
- 已存在的 `AiService.generateMedicalRecord(MedicalRecordGenRequest)` 方法签名无需变更
