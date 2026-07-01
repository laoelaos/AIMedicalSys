# 任务指令（v3）

## 动作
NEW

## 任务描述
在 ai-api 模块中扩展病历生成相关 DTO：

**已有文件需扩展字段**（2 个，当前为空壳类）：
- `ai-api/.../dto/medicalrecord/MedicalRecordGenRequest.java`
- `ai-api/.../dto/medicalrecord/MedicalRecordGenResponse.java`

## 选择理由
T1（分诊 DTO）和 T2（处方审核+辅助开方 DTO）均已通过验证。病历生成 DTO 扩展是 medical-record 业务模块开发的前置依赖。T3 依赖 T1（AiResult 框架已就绪），无其他阻塞依赖。

## 任务上下文

### MedicalRecordGenRequest（ai-api 层）— 替换空壳，扩展字段

包：`com.aimedical.modules.ai.api.dto.medicalrecord`

```java
public class MedicalRecordGenRequest {
    private String dialogueText;       // 必填，对话文本，50–10000 字符
    private String patientId;          // 必填，患者 ID
    private String encounterId;        // 可选，就诊标识
    private boolean stream;            // 可选，流式标记，默认 false
    private String departmentId;       // 可选，科室 ID
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| dialogueText | `String` | 是 | 医患对话原始文本 |
| patientId | `String` | 是 | 患者 ID |
| encounterId | `String` | 否 | 就诊标识，后端通过 VisitFacade 映射为 visitId |
| stream | `boolean` | 否 | 流式输出标记，默认 false（Phase 2/3 仅支持非流式） |
| departmentId | `String` | 否 | 科室 ID |

### MedicalRecordGenResponse（ai-api 层）— 替换空壳，扩展字段

包：`com.aimedical.modules.ai.api.dto.medicalrecord`

```java
public class MedicalRecordGenResponse {
    private String chiefComplaint;          // 主诉
    private String symptomDescription;      // 症状描述
    private String presentIllness;          // 现病史
    private String pastHistory;             // 既往史
    private String physicalExam;            // 体格检查
    private String preliminaryDiagnosis;    // 初步诊断
    private String treatmentPlan;           // 治疗意见
    private List<String> missingFields;     // 缺失字段列表
    private Object partialContent;          // 可选，超时降级时携带部分生成结果
}
```

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

### 字段访问约定
- 所有字段通过默认构造器 + getter/setter 访问
- List 类型字段默认值为 null（可选语义）
- String 类型字段默认值为 null
- boolean 类型字段默认值为 false（stream）
- Object 类型字段默认值为 null（partialContent，ai-api 层不引入具体类型以保持低耦合）

### 依赖关系
- MedicalRecordGenRequest 和 MedicalRecordGenResponse 均位于 `com.aimedical.modules.ai.api.dto.medicalrecord` 包内
- 无跨模块依赖
- 需要 import `java.util.List`

### 相关接口（已有，无变更）
```java
// com.aimedical.modules.ai.api.service.AiService 已有方法签名（T1 已定义）：
CompletableFuture<AiResult<MedicalRecordGenResponse>> generateMedicalRecord(MedicalRecordGenRequest request);
```

## 已有代码上下文
- 2 个已有文件（`MedicalRecordGenRequest.java`、`MedicalRecordGenResponse.java`）均为空壳类（仅含默认构造器，无字段）
- 包路径已存在：`com.aimedical.modules.ai.api.dto.medicalrecord`
- 已有 AiResult.java、AiResultFactory.java（T1）、AiService.java（T1）
- T2 新增的 PrescriptionCheck/Assist DTO 与此任务无直接依赖
- 测试框架为 JUnit 5（Jupiter），参考已存在的 PrescriptionDtoTest.java
