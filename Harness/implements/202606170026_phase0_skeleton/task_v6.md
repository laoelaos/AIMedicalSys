# 任务指令（v6）

## 动作
NEW

## 任务描述
实现 AI 能力 api 模块（ai-api）的全部源文件，包含 AI 能力接口契约、结果包装、13 组输入/输出 DTO、降级策略框架及其单元测试。

### 需要创建/修改的文件

**POM 依赖更新**：
- `backend/ai-api/pom.xml` — 添加 common（compile scope）依赖和 spring-boot-starter-test（test scope）依赖

**类型文件**（包路径：com.aimedical.modules.ai.api）：
| 文件路径 | 类型形态 | 职责 |
|---------|---------|------|
| `.../AiResult.java` | class | AI 调用结果包装（success, data, errorCode, degraded, fallbackReason） |
| `.../AiService.java` | interface | AI 能力接口集合，定义 13 个方法，全部返回 `CompletableFuture<AiResult<T>>` |
| `.../degradation/DegradationContext.java` | class | 降级判定上下文，Phase 0 仅保留无参构造器，字段取零值 |
| `.../degradation/DegradationStrategy.java` | interface | 降级策略接口：`boolean shouldDegrade(DegradationContext context)` |
| `.../dto/triage/TriageRequest.java` | class | 智能分诊请求 DTO |
| `.../dto/triage/TriageResponse.java` | class | 智能分诊响应 DTO |
| `.../dto/triage/RecommendedDepartment.java` | class | 推荐科室（TriageResponse 内嵌类型） |
| `.../dto/diagnosis/DiagnosisRequest.java` | class | 智能诊断请求 DTO |
| `.../dto/diagnosis/DiagnosisResponse.java` | class | 智能诊断响应 DTO |
| `.../dto/prescription/PrescriptionCheckRequest.java` | class | 处方审核请求 DTO |
| `.../dto/prescription/PrescriptionCheckResponse.java` | class | 处方审核响应 DTO |
| `.../dto/medicalrecord/MedicalRecordGenRequest.java` | class | 病历生成请求 DTO |
| `.../dto/medicalrecord/MedicalRecordGenResponse.java` | class | 病历生成响应 DTO |
| `.../dto/inspection/InspectionReportRequest.java` | class | 检查报告请求 DTO |
| `.../dto/inspection/InspectionReportResponse.java` | class | 检查报告响应 DTO |
| `.../dto/labtest/LabTestReportRequest.java` | class | 检验报告请求 DTO |
| `.../dto/labtest/LabTestReportResponse.java` | class | 检验报告响应 DTO |
| `.../dto/image/ImageAnalysisRequest.java` | class | 影像分析请求 DTO |
| `.../dto/image/ImageAnalysisResponse.java` | class | 影像分析响应 DTO |
| `.../dto/kb/KbQueryRequest.java` | class | 知识库问答请求 DTO |
| `.../dto/kb/KbQueryResponse.java` | class | 知识库问答响应 DTO |
| `.../dto/examination/ExaminationRecommendRequest.java` | class | 检查检验推荐请求 DTO |
| `.../dto/examination/ExaminationRecommendResponse.java` | class | 检查检验推荐响应 DTO |
| `.../dto/prescription/PrescriptionAssistRequest.java` | class | 辅助开方请求 DTO |
| `.../dto/prescription/PrescriptionAssistResponse.java` | class | 辅助开方响应 DTO |
| `.../dto/execution/ExecutionOrderRequest.java` | class | 执行顺序推荐请求 DTO |
| `.../dto/execution/ExecutionOrderResponse.java` | class | 执行顺序推荐响应 DTO |
| `.../dto/schedule/ScheduleRequest.java` | class | 排班请求 DTO |
| `.../dto/schedule/ScheduleResponse.java` | class | 排班响应 DTO |
| `.../dto/discussion/DiscussionConclusionRequest.java` | class | 综合讨论结论请求 DTO |
| `.../dto/discussion/DiscussionConclusionResponse.java` | class | 综合讨论结论响应 DTO |

**测试文件**：
| 文件路径 | 测试内容 |
|---------|---------|
| `.../api/AiResultTest.java` | AiResult 构造、getter/setter、默认值 |
| `.../api/dto/triage/TriageDtoTest.java` | TriageRequest/TriageResponse/RecommendedDepartment 构造与序列化 |
| `.../api/degradation/DegradationStrategyTest.java` | DegradationStrategy 接口默认行为、DegradationContext 构造 |
| `...api/AiServiceTest.java` | AiService 接口方法签名验证（编译期契约检查） |

## 选择理由
- ai-api 是 AI 能力接口契约层，位于 common 之后、业务模块之前
- 所有业务模块（patient, doctor, admin）依赖 ai-api 获取 AI 能力接口和 DTO 类型
- 已完成 common（基类）和 common-module（权限实体），ai-api 是下一层依赖
- ai-api 和 ai-impl 已拆分为独立子模块，ai-api 为接口契约优先实现

## 任务上下文

### 包路径规范
```
com.aimedical.modules.ai.api
├── AiService.java           # AI 能力接口集合
├── AiResult.java            # AI 调用结果包装
├── degradation/
│   ├── DegradationContext.java   # 降级上下文
│   └── DegradationStrategy.java  # 降级策略接口
└── dto/
    ├── triage/
    │   ├── TriageRequest.java
    │   ├── TriageResponse.java
    │   └── RecommendedDepartment.java
    ├── diagnosis/
    │   ├── DiagnosisRequest.java
    │   └── DiagnosisResponse.java
    ├── prescription/
    │   ├── PrescriptionCheckRequest.java
    │   ├── PrescriptionCheckResponse.java
    │   ├── PrescriptionAssistRequest.java
    │   └── PrescriptionAssistResponse.java
    ├── medicalrecord/
    │   ├── MedicalRecordGenRequest.java
    │   └── MedicalRecordGenResponse.java
    ├── inspection/
    │   ├── InspectionReportRequest.java
    │   └── InspectionReportResponse.java
    ├── labtest/
    │   ├── LabTestReportRequest.java
    │   └── LabTestReportResponse.java
    ├── image/
    │   ├── ImageAnalysisRequest.java
    │   └── ImageAnalysisResponse.java
    ├── kb/
    │   ├── KbQueryRequest.java
    │   └── KbQueryResponse.java
    ├── examination/
    │   ├── ExaminationRecommendRequest.java
    │   └── ExaminationRecommendResponse.java
    ├── execution/
    │   ├── ExecutionOrderRequest.java
    │   └── ExecutionOrderResponse.java
    ├── schedule/
    │   ├── ScheduleRequest.java
    │   └── ScheduleResponse.java
    └── discussion/
        ├── DiscussionConclusionRequest.java
        └── DiscussionConclusionResponse.java
```

### AiService 接口方法清单（13 个方法）
所有方法统一返回 `CompletableFuture<AiResult<T>>`，方法名和输入/输出类型如下：

| 方法 | 输入 | 输出类型参数 |
|------|------|------------|
| `triage` | TriageRequest | TriageResponse |
| `diagnosis` | DiagnosisRequest | DiagnosisResponse |
| `prescriptionCheck` | PrescriptionCheckRequest | PrescriptionCheckResponse |
| `generateMedicalRecord` | MedicalRecordGenRequest | MedicalRecordGenResponse |
| `analysisReportForInspection` | InspectionReportRequest | InspectionReportResponse |
| `analysisReportForLabTest` | LabTestReportRequest | LabTestReportResponse |
| `imageAnalysis` | ImageAnalysisRequest | ImageAnalysisResponse |
| `knowledgeBaseQuery` | KbQueryRequest | KbQueryResponse |
| `recommendExamination` | ExaminationRecommendRequest | ExaminationRecommendResponse |
| `prescriptionAssist` | PrescriptionAssistRequest | PrescriptionAssistResponse |
| `recommendExecutionOrder` | ExecutionOrderRequest | ExecutionOrderResponse |
| `schedule` | ScheduleRequest | ScheduleResponse |
| `discussionConclusion` | DiscussionConclusionRequest | DiscussionConclusionResponse |

### AiResult 类型要求
```java
public class AiResult<T> {
    private boolean success;          // 是否成功
    private T data;                   // 成功时的数据载荷
    private String errorCode;         // 失败时的错误码
    private boolean degraded;         // 是否降级
    private String fallbackReason;    // 降级原因
    // 全参构造 + 无参构造 + getter/setter
    // 静态工厂方法: success(data), failure(errorCode), degraded(reason)
}
```

### DegradationStrategy 接口
```java
public interface DegradationStrategy {
    boolean shouldDegrade(DegradationContext context);
}
```

### DegradationContext 类
Phase 0 仅保留无参构造器，字段取语言默认值。无业务字段。

### DTO 两层冻结策略
- **本阶段冻结**：DTO 类名、输入/输出归属关系、Java 包路径
- **延后冻结**：除 Phase 0 Mock 子集外，其余字段在对应 AI 能力首次落地阶段定义
- **Phase 0 Mock 字段子集**：仅 `TriageRequest.chiefComplaint`、`TriageResponse.recommendedDepartments`、`TriageResponse.reason`、`RecommendedDepartment.departmentName` 需声明字段；其余 DTO 保留空壳 class
- 所有 DTO 需要默认无参构造器
- 对外 JSON 统一使用 snake_case（由 Jackson JacksonConfig 全局配置处理）

## 已有代码上下文
- common 模块提供 `com.aimedical.common.base.BaseEnum`、`com.aimedical.common.result.Result`、`com.aimedical.common.exception.ErrorCode` 等基类（ai-api 不直接引用这些基类，仅作为依赖关系提及）
- 父 POM `backend/pom.xml` 已声明 ai-api 模块和版本管理
- ai-api/pom.xml 当前为空壳（仅 artifactId，无依赖声明）
- 项目遵循 `com.aimedical.modules.{module}.{submodule}` 包命名规范
- JacksonConfig 全局配置 PropertyNamingStrategies.SNAKE_CASE
