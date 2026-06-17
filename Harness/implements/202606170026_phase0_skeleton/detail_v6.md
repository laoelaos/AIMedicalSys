# 详细设计（v6）

## 概述

实现 ai-api 模块的全部源文件，包含 AI 能力接口契约（AiService）、结果包装（AiResult）、降级策略框架（DegradationStrategy/DegradationContext）和 13 组输入/输出 DTO。Phase 0 仅 Triage 相关 DTO 声明 Mock 字段，其余 DTO 为空壳 class。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| backend/ai-api/pom.xml | 修改 | 添加 common（compile scope）和 spring-boot-starter-test（test scope）依赖 |
| backend/ai-api/src/main/java/com/aimedical/modules/ai/api/AiResult.java | 新建 | AI 调用结果包装类 |
| backend/ai-api/src/main/java/com/aimedical/modules/ai/api/AiService.java | 新建 | AI 能力接口集合（13 个方法） |
| backend/ai-api/src/main/java/com/aimedical/modules/ai/api/degradation/DegradationContext.java | 新建 | 降级判定上下文（Phase 0 空壳） |
| backend/ai-api/src/main/java/com/aimedical/modules/ai/api/degradation/DegradationStrategy.java | 新建 | 降级策略接口 |
| backend/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/triage/TriageRequest.java | 新建 | 智能分诊请求 DTO |
| backend/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/triage/TriageResponse.java | 新建 | 智能分诊响应 DTO |
| backend/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/triage/RecommendedDepartment.java | 新建 | TriageResponse 内嵌推荐科室 |
| backend/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/diagnosis/DiagnosisRequest.java | 新建 | 智能诊断请求 DTO |
| backend/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/diagnosis/DiagnosisResponse.java | 新建 | 智能诊断响应 DTO |
| backend/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/prescription/PrescriptionCheckRequest.java | 新建 | 处方审核请求 DTO |
| backend/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/prescription/PrescriptionCheckResponse.java | 新建 | 处方审核响应 DTO |
| backend/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/prescription/PrescriptionAssistRequest.java | 新建 | 辅助开方请求 DTO |
| backend/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/prescription/PrescriptionAssistResponse.java | 新建 | 辅助开方响应 DTO |
| backend/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/medicalrecord/MedicalRecordGenRequest.java | 新建 | 病历生成请求 DTO |
| backend/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/medicalrecord/MedicalRecordGenResponse.java | 新建 | 病历生成响应 DTO |
| backend/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/inspection/InspectionReportRequest.java | 新建 | 检查报告请求 DTO |
| backend/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/inspection/InspectionReportResponse.java | 新建 | 检查报告响应 DTO |
| backend/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/labtest/LabTestReportRequest.java | 新建 | 检验报告请求 DTO |
| backend/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/labtest/LabTestReportResponse.java | 新建 | 检验报告响应 DTO |
| backend/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/image/ImageAnalysisRequest.java | 新建 | 影像分析请求 DTO |
| backend/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/image/ImageAnalysisResponse.java | 新建 | 影像分析响应 DTO |
| backend/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/kb/KbQueryRequest.java | 新建 | 知识库问答请求 DTO |
| backend/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/kb/KbQueryResponse.java | 新建 | 知识库问答响应 DTO |
| backend/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/examination/ExaminationRecommendRequest.java | 新建 | 检查检验推荐请求 DTO |
| backend/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/examination/ExaminationRecommendResponse.java | 新建 | 检查检验推荐响应 DTO |
| backend/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/execution/ExecutionOrderRequest.java | 新建 | 执行顺序推荐请求 DTO |
| backend/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/execution/ExecutionOrderResponse.java | 新建 | 执行顺序推荐响应 DTO |
| backend/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/schedule/ScheduleRequest.java | 新建 | 排班请求 DTO |
| backend/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/schedule/ScheduleResponse.java | 新建 | 排班响应 DTO |
| backend/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/discussion/DiscussionConclusionRequest.java | 新建 | 综合讨论结论请求 DTO |
| backend/ai-api/src/main/java/com/aimedical/modules/ai/api/dto/discussion/DiscussionConclusionResponse.java | 新建 | 综合讨论结论响应 DTO |
| backend/ai-api/src/test/java/com/aimedical/modules/ai/api/AiResultTest.java | 新建 | AiResult 构造、getter/setter、默认值 |
| backend/ai-api/src/test/java/com/aimedical/modules/ai/api/AiServiceTest.java | 新建 | AiService 接口方法签名验证 |
| backend/ai-api/src/test/java/com/aimedical/modules/ai/api/dto/triage/TriageDtoTest.java | 新建 | TriageRequest/TriageResponse/RecommendedDepartment 构造与字段 |
| backend/ai-api/src/test/java/com/aimedical/modules/ai/api/degradation/DegradationStrategyTest.java | 新建 | DegradationStrategy 默认行为、DegradationContext 构造 |

## 类型定义

### AiResult
**形态**：class
**包路径**：com.aimedical.modules.ai.api
**职责**：包装 AI 调用结果，包含成功/失败/降级三种状态
```java
public class AiResult<T> {
    private boolean success;
    private T data;
    private String errorCode;
    private boolean degraded;
    private String fallbackReason;

    public AiResult() {}
    public AiResult(boolean success, T data, String errorCode, boolean degraded, String fallbackReason) {}

    // getter/setter
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public boolean isDegraded() { return degraded; }
    public void setDegraded(boolean degraded) { this.degraded = degraded; }
    public String getFallbackReason() { return fallbackReason; }
    public void setFallbackReason(String fallbackReason) { this.fallbackReason = fallbackReason; }

    // 静态工厂方法
    public static <T> AiResult<T> success(T data) { ... }
    public static <T> AiResult<T> failure(String errorCode) { ... }
    public static <T> AiResult<T> degraded(String fallbackReason) { ... }
}
```
**公开接口**：
- `success(T data)` — 构造成功结果（success=true, data=入参, degraded=false）
- `failure(String errorCode)` — 构造失败结果（success=false, errorCode=入参, degraded=false）
- `degraded(String fallbackReason)` — 构造降级结果（success=false, degraded=true, fallbackReason=入参）
**构造方式**：无参构造 / 全参构造 / 静态工厂
**类型关系**：泛型类 `<T>`

### AiService
**形态**：interface
**包路径**：com.aimedical.modules.ai.api
**职责**：AI 能力接口集合，定义 13 个异步 AI 调用方法
```java
public interface AiService {
    CompletableFuture<AiResult<TriageResponse>> triage(TriageRequest request);
    CompletableFuture<AiResult<DiagnosisResponse>> diagnosis(DiagnosisRequest request);
    CompletableFuture<AiResult<PrescriptionCheckResponse>> prescriptionCheck(PrescriptionCheckRequest request);
    CompletableFuture<AiResult<MedicalRecordGenResponse>> generateMedicalRecord(MedicalRecordGenRequest request);
    CompletableFuture<AiResult<InspectionReportResponse>> analysisReportForInspection(InspectionReportRequest request);
    CompletableFuture<AiResult<LabTestReportResponse>> analysisReportForLabTest(LabTestReportRequest request);
    CompletableFuture<AiResult<ImageAnalysisResponse>> imageAnalysis(ImageAnalysisRequest request);
    CompletableFuture<AiResult<KbQueryResponse>> knowledgeBaseQuery(KbQueryRequest request);
    CompletableFuture<AiResult<ExaminationRecommendResponse>> recommendExamination(ExaminationRecommendRequest request);
    CompletableFuture<AiResult<PrescriptionAssistResponse>> prescriptionAssist(PrescriptionAssistRequest request);
    CompletableFuture<AiResult<ExecutionOrderResponse>> recommendExecutionOrder(ExecutionOrderRequest request);
    CompletableFuture<AiResult<ScheduleResponse>> schedule(ScheduleRequest request);
    CompletableFuture<AiResult<DiscussionConclusionResponse>> discussionConclusion(DiscussionConclusionRequest request);
}
```
**公开接口**：13 个方法，每个接收具体 Request DTO，返回 `CompletableFuture<AiResult<对应 Response DTO>>`
**构造方式**：由实现类提供（Spring @Service）
**类型关系**：依赖全部 Request/Response DTO 类型

### DegradationContext
**形态**：class
**包路径**：com.aimedical.modules.ai.api.degradation
**职责**：降级判定上下文，Phase 0 仅保留无参构造器，字段取语言默认值
```java
public class DegradationContext {
    public DegradationContext() {}
}
```
**公开接口**：无（Phase 0 空壳）
**构造方式**：无参构造

### DegradationStrategy
**形态**：interface
**包路径**：com.aimedical.modules.ai.api.degradation
**职责**：降级策略判定的抽象接口
```java
public interface DegradationStrategy {
    boolean shouldDegrade(DegradationContext context);
}
```
**公开接口**：`shouldDegrade(DegradationContext context) : boolean`
**构造方式**：由实现类提供

### TriageRequest
**形态**：class
**包路径**：com.aimedical.modules.ai.api.dto.triage
**职责**：智能分诊请求，Phase 0 Mock 仅含 chiefComplaint 字段
```java
public class TriageRequest {
    private String chiefComplaint;

    public TriageRequest() {}
    public String getChiefComplaint() { return chiefComplaint; }
    public void setChiefComplaint(String chiefComplaint) { this.chiefComplaint = chiefComplaint; }
}
```
**公开接口**：getter/setter for chiefComplaint
**构造方式**：无参构造

### TriageResponse
**形态**：class
**包路径**：com.aimedical.modules.ai.api.dto.triage
**职责**：智能分诊响应，Phase 0 Mock 含 recommendedDepartments 和 reason 字段
```java
public class TriageResponse {
    private List<RecommendedDepartment> recommendedDepartments;
    private String reason;

    public TriageResponse() {}
    public List<RecommendedDepartment> getRecommendedDepartments() { return recommendedDepartments; }
    public void setRecommendedDepartments(List<RecommendedDepartment> recommendedDepartments) { this.recommendedDepartments = recommendedDepartments; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
```
**公开接口**：getter/setter for recommendedDepartments, reason
**构造方式**：无参构造
**类型关系**：组合 `List<RecommendedDepartment>`

### RecommendedDepartment
**形态**：class
**包路径**：com.aimedical.modules.ai.api.dto.triage
**职责**：TriageResponse 的内嵌推荐科室信息
```java
public class RecommendedDepartment {
    private String departmentName;

    public RecommendedDepartment() {}
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
}
```
**公开接口**：getter/setter for departmentName
**构造方式**：无参构造

### 其余 DTO（空壳，11 组 Request/Response + PrescriptionAssist 请求/响应）

**形态**：class
**包路径**：各自 `com.aimedical.modules.ai.api.dto.{subpackage}`
**职责**：占位 DTO，Phase 0 无字段声明，仅提供无参构造器

下列 22 个类型统一采用以下模式：
```java
public class {ClassName} {
    public {ClassName}() {}
}
```

| 类型 | 包路径 |
|------|--------|
| DiagnosisRequest | com.aimedical.modules.ai.api.dto.diagnosis |
| DiagnosisResponse | com.aimedical.modules.ai.api.dto.diagnosis |
| PrescriptionCheckRequest | com.aimedical.modules.ai.api.dto.prescription |
| PrescriptionCheckResponse | com.aimedical.modules.ai.api.dto.prescription |
| PrescriptionAssistRequest | com.aimedical.modules.ai.api.dto.prescription |
| PrescriptionAssistResponse | com.aimedical.modules.ai.api.dto.prescription |
| MedicalRecordGenRequest | com.aimedical.modules.ai.api.dto.medicalrecord |
| MedicalRecordGenResponse | com.aimedical.modules.ai.api.dto.medicalrecord |
| InspectionReportRequest | com.aimedical.modules.ai.api.dto.inspection |
| InspectionReportResponse | com.aimedical.modules.ai.api.dto.inspection |
| LabTestReportRequest | com.aimedical.modules.ai.api.dto.labtest |
| LabTestReportResponse | com.aimedical.modules.ai.api.dto.labtest |
| ImageAnalysisRequest | com.aimedical.modules.ai.api.dto.image |
| ImageAnalysisResponse | com.aimedical.modules.ai.api.dto.image |
| KbQueryRequest | com.aimedical.modules.ai.api.dto.kb |
| KbQueryResponse | com.aimedical.modules.ai.api.dto.kb |
| ExaminationRecommendRequest | com.aimedical.modules.ai.api.dto.examination |
| ExaminationRecommendResponse | com.aimedical.modules.ai.api.dto.examination |
| ExecutionOrderRequest | com.aimedical.modules.ai.api.dto.execution |
| ExecutionOrderResponse | com.aimedical.modules.ai.api.dto.execution |
| ScheduleRequest | com.aimedical.modules.ai.api.dto.schedule |
| ScheduleResponse | com.aimedical.modules.ai.api.dto.schedule |
| DiscussionConclusionRequest | com.aimedical.modules.ai.api.dto.discussion |
| DiscussionConclusionResponse | com.aimedical.modules.ai.api.dto.discussion |

## 错误处理

AiResult 自身承载错误信息（errorCode 字段），不抛出受检异常。AI 服务调用方的超时/中断由 CompletableFuture 的异常传播链路处理。ai-api 层不定义自定义异常类型。

## 行为契约

1. AiResult.success(data)：success=true，data=入参，errorCode=null，degraded=false，fallbackReason=null
2. AiResult.failure(errorCode)：success=false，data=null，errorCode=入参，degraded=false，fallbackReason=null
3. AiResult.degraded(reason)：success=false，data=null，errorCode=null，degraded=true，fallbackReason=入参
4. 所有 DTO 必须提供无参构造器（Jackson 反序列化需求）
5. 对外 JSON 字段名由 JacksonConfig 全局 PropertyNamingStrategies.SNAKE_CASE 处理，各 DTO 字段使用 Java camelCase 命名
6. DegradationContext Phase 0 不包含业务字段，仅作为策略接口的方法签名占位

## 依赖关系

| 模块 | 依赖 |
|------|------|
| ai-api (compile) | common（无直接类型引用，仅维护模块依赖完整性） |
| ai-api (test) | spring-boot-starter-test |

暴露给后续任务的公开接口：
- `com.aimedical.modules.ai.api.AiService` — ai-impl 需实现该接口
- `com.aimedical.modules.ai.api.AiResult<T>` — 业务模块可构造/读取 AI 调用结果
- `com.aimedical.modules.ai.api.degradation.DegradationStrategy` — ai-impl 需实现降级策略
- `com.aimedical.modules.ai.api.dto.*` — 业务模块可引用所有 DTO 类型
