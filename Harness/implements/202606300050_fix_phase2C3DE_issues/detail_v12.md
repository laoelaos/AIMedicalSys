# 详细设计（v12）

## 概述

修复 medical-record 模块 3 个测试文件的编译错误，以使 `mvn compile test-compile -pl modules/medical-record` 通过：

1. **MissingFieldDetectorImplTest** — R11 M11 引入的 `MedicalRecordConverter` 构造参数变更未同步
2. **MedicalRecordControllerTest** — `Result` 类无 `isSuccess()` 方法，应使用 `getCode()` 判断
3. **MedicalRecordServiceImplTest** — `StubAiService` 返回类型未对齐 `AiService` 接口（缺少 `AiResult<>` 包装），且缺少 `discussionConclusion` 方法（类声明为 abstract 以内化错误）

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/medical-record/src/test/java/com/aimedical/modules/medicalrecord/detector/MissingFieldDetectorImplTest.java` | 修改 | 适配 `MedicalRecordConverter` 构造参数变更 |
| `AIMedical/backend/modules/medical-record/src/test/java/com/aimedical/modules/medicalrecord/api/MedicalRecordControllerTest.java` | 修改 | 消除 `isSuccess()` 调用，改用 `getCode()` 判断 |
| `AIMedical/backend/modules/medical-record/src/test/java/com/aimedical/modules/medicalrecord/service/impl/MedicalRecordServiceImplTest.java` | 修改 | StubAiService 返回类型 AiResult 化 + 新增 `discussionConclusion` 方法 |

## 修改细则

### 1. MissingFieldDetectorImplTest.java

**行号**: L22

**当前代码**:
```java
private final MedicalRecordConverter converter = new MedicalRecordConverter();
```

**修改后**:
```java
private final MedicalRecordConverter converter = new MedicalRecordConverter(new ObjectMapper());
```

**import 变化**: 增加 `com.fasterxml.jackson.databind.ObjectMapper`

**说明**: `MedicalRecordConverter`（v11 M11）构造参数从无参变更为 `ObjectMapper`，测试实例化处需同步传递。

---

### 2. MedicalRecordControllerTest.java

**行号**: L30, L46, L62

| 行号 | 当前代码 | 修改后 |
|------|---------|--------|
| 30 | `assertFalse(result.isSuccess());` | `assertNotEquals("SUCCESS", result.getCode());` |
| 46 | `assertTrue(result.isSuccess());` | `assertEquals("SUCCESS", result.getCode());` |
| 62 | `assertFalse(result.isSuccess());` | `assertNotEquals("SUCCESS", result.getCode());` |

**说明**: `Result<T>`（`com.aimedical.common.result.Result`）仅有 `getCode()` 方法，成功时返回字面量 `"SUCCESS"`。使用 `assertEquals`/`assertNotEquals` 替换 `isSuccess()`。

---

### 3. MedicalRecordServiceImplTest.java — StubAiService

**位置**: L298–L342

**变更点**:

#### 3a. 类声明

```java
// 当前：
private static class StubAiService implements AiService {

// 修改后：
private static class StubAiService implements AiService {
```

无变化（当前已是非 abstract 类，无需改正）。

#### 3b. 方法返回类型 AiResult 化

目前 `StubAiService` 除 `generateMedicalRecord` 外，其余 12 个方法的返回类型均为 `CompletableFuture<X>`，而 `AiService` 接口要求 `CompletableFuture<AiResult<X>>`。将所有方法返回类型包装为 `CompletableFuture<AiResult<X>>`。

| 方法名 | 当前返回类型 | 修改后返回类型 |
|--------|-------------|---------------|
| `triage` | `CompletableFuture<TriageResponse>` | `CompletableFuture<AiResult<TriageResponse>>` |
| `diagnosis` | `CompletableFuture<DiagnosisResponse>` | `CompletableFuture<AiResult<DiagnosisResponse>>` |
| `prescriptionCheck` | `CompletableFuture<PrescriptionCheckResponse>` | `CompletableFuture<AiResult<PrescriptionCheckResponse>>` |
| `analysisReportForInspection` | `CompletableFuture<InspectionReportResponse>` | `CompletableFuture<AiResult<InspectionReportResponse>>` |
| `analysisReportForLabTest` | `CompletableFuture<LabTestReportResponse>` | `CompletableFuture<AiResult<LabTestReportResponse>>` |
| `imageAnalysis` | `CompletableFuture<ImageAnalysisResponse>` | `CompletableFuture<AiResult<ImageAnalysisResponse>>` |
| `knowledgeBaseQuery` | `CompletableFuture<KbQueryResponse>` | `CompletableFuture<AiResult<KbQueryResponse>>` |
| `recommendExamination` | `CompletableFuture<ExaminationRecommendResponse>` | `CompletableFuture<AiResult<ExaminationRecommendResponse>>` |
| `prescriptionAssist` | `CompletableFuture<PrescriptionAssistResponse>` | `CompletableFuture<AiResult<PrescriptionAssistResponse>>` |
| `recommendExecutionOrder` | `CompletableFuture<ExecutionOrderResponse>` | `CompletableFuture<AiResult<ExecutionOrderResponse>>` |
| `schedule` | `CompletableFuture<ScheduleResponse>` | `CompletableFuture<AiResult<ScheduleResponse>>` |
| `discussionConclusion` | `CompletableFuture<DiscussionConclusionResponse>` | `CompletableFuture<AiResult<DiscussionConclusionResponse>>` |

**注意**: 单行写法 `{ return null; }` 不变，仅修改返回类型。

## 错误处理

不涉及错误处理变更。

## 行为契约

测试行为不变，仅修复编译错误：
- `MissingFieldDetectorImplTest` — 全量测试语义不变
- `MedicalRecordControllerTest` — 断言逻辑等价于原 `isSuccess()`：`"SUCCESS".equals(result.getCode())` ↔ `true`, 否则 `false`
- `MedicalRecordServiceImplTest` — Stub 方法返回 `null` 等价，不影响测试流程

## 依赖关系

| 被依赖类型 | 提供方 | 用途 |
|-----------|-------|------|
| `com.fasterxml.jackson.databind.ObjectMapper` | jackson-databind | MissingFieldDetectorImplTest: 构造 `MedicalRecordConverter` |
| `com.aimedical.common.result.Result` | common 模块 | MedicalRecordControllerTest: `getCode()` 方法 |
| `com.aimedical.modules.ai.api.AiResult` | ai-api 模块 | MedicalRecordServiceImplTest: 返回类型包装 |
