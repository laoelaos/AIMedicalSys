# 详细设计（v8）

## 概述

修复 MockAiServiceTest.java 的 13 处 Java 泛型编译错误。问题根因：AiService 各方法返回 `CompletableFuture<AiResult<XxxResponse>>`（精确泛型），测试代码以 `CompletableFuture<AiResult<?>>` 接收，Java 泛型不变性导致赋值失败。修复方式：将 13 处显式泛型赋值改为 `var` 局部变量类型推断。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/mock/MockAiServiceTest.java | 修改 | 13 处 `CompletableFuture<AiResult<?>> future = service.xxx(...)` → `var future = service.xxx(...)`；移除未使用的 `java.util.concurrent.CompletableFuture` import |

## 类型定义

### MockAiServiceTest（v8 修正版）

**形态**：class
**包路径**：com.aimedical.modules.ai.impl.mock
**可见性**：package-private

**变更清单**（13 处）：

| 方法 | 原代码（37 行） | 修改后 |
|------|----------------|--------|
| triageShouldReturnMockData (L37) | `CompletableFuture<AiResult<?>> future = service.triage(new TriageRequest());` | `var future = service.triage(new TriageRequest());` |
| diagnosisShouldReturnMockData (L52) | `CompletableFuture<AiResult<?>> future = service.diagnosis(new DiagnosisRequest());` | `var future = service.diagnosis(new DiagnosisRequest());` |
| prescriptionCheckShouldReturnMockData (L63) | `CompletableFuture<AiResult<?>> future = service.prescriptionCheck(new PrescriptionCheckRequest());` | `var future = service.prescriptionCheck(new PrescriptionCheckRequest());` |
| generateMedicalRecordShouldReturnMockData (L74) | `CompletableFuture<AiResult<?>> future = service.generateMedicalRecord(new MedicalRecordGenRequest());` | `var future = service.generateMedicalRecord(new MedicalRecordGenRequest());` |
| analysisReportForInspectionShouldReturnMockData (L85) | `CompletableFuture<AiResult<?>> future = service.analysisReportForInspection(new InspectionReportRequest());` | `var future = service.analysisReportForInspection(new InspectionReportRequest());` |
| analysisReportForLabTestShouldReturnMockData (L96) | `CompletableFuture<AiResult<?>> future = service.analysisReportForLabTest(new LabTestReportRequest());` | `var future = service.analysisReportForLabTest(new LabTestReportRequest());` |
| imageAnalysisShouldReturnMockData (L107) | `CompletableFuture<AiResult<?>> future = service.imageAnalysis(new ImageAnalysisRequest());` | `var future = service.imageAnalysis(new ImageAnalysisRequest());` |
| knowledgeBaseQueryShouldReturnMockData (L118) | `CompletableFuture<AiResult<?>> future = service.knowledgeBaseQuery(new KbQueryRequest());` | `var future = service.knowledgeBaseQuery(new KbQueryRequest());` |
| recommendExaminationShouldReturnMockData (L129) | `CompletableFuture<AiResult<?>> future = service.recommendExamination(new ExaminationRecommendRequest());` | `var future = service.recommendExamination(new ExaminationRecommendRequest());` |
| prescriptionAssistShouldReturnMockData (L140) | `CompletableFuture<AiResult<?>> future = service.prescriptionAssist(new PrescriptionAssistRequest());` | `var future = service.prescriptionAssist(new PrescriptionAssistRequest());` |
| recommendExecutionOrderShouldReturnMockData (L151) | `CompletableFuture<AiResult<?>> future = service.recommendExecutionOrder(new ExecutionOrderRequest());` | `var future = service.recommendExecutionOrder(new ExecutionOrderRequest());` |
| scheduleShouldReturnMockData (L162) | `CompletableFuture<AiResult<?>> future = service.schedule(new ScheduleRequest());` | `var future = service.schedule(new ScheduleRequest());` |
| discussionConclusionShouldReturnMockData (L173) | `CompletableFuture<AiResult<?>> future = service.discussionConclusion(new DiscussionConclusionRequest());` | `var future = service.discussionConclusion(new DiscussionConclusionRequest());` |

**移除 import（L24）**：
```java
import java.util.concurrent.CompletableFuture;
```

**未变更的代码**：
- `AiResult<?> result = future.join();` — 保持原样。`var future` 推断类型为 `CompletableFuture<AiResult<XxxResponse>>`，`future.join()` 返回 `AiResult<XxxResponse>`，可安全赋值给 `AiResult<?>`。
- `triageShouldReturnMockData` 中的 `(TriageResponse) result.getData()` 转型 — 保持原样，因为 `getData()` 返回 `?`，需显式转型。
- 其他所有 import 语句 — 保持原样。
- `shouldBeAnnotatedWithService` 测试方法 — 不涉及泛型，无需修改。

## 错误处理

无变更。MockAiServiceTest 所有方法不抛出受检异常；`future.isDone()`、`result.isSuccess()`、`result.isDegraded()` 等调用均不涉及受检异常。

## 行为契约

1. 13 处变量声明从显式泛型改为 `var` 后，编译期类型推断结果与原有手动声明的类型含义一致（均为 `CompletableFuture<AiResult<XxxResponse>>`），仅在 Java 类型系统中正确处理了泛型不变性
2. `var future` 后，`future.join()` 返回精确的 `AiResult<XxxResponse>`，赋值给 `AiResult<?>` 是安全的（`?` 是通配符上界，`AiResult<XxxResponse> <: AiResult<?>`）

## 依赖关系

| 模块 | 变更 |
|------|------|
| ai-impl | 无 POM 变更，仅测试 Java 源文件代码风格修改 |
| ai-api | 无变更 |
| common | 无变更 |

不引入新的外部依赖。仅使用 Java 10+ 标准语言特性 `var`（项目 JDK 17），无需新增任何依赖。
DESIGN_WRITTEN:C:\Develop\Software\AIMedicalSys\Harness\implements\202606170026_phase0_skeleton\detail_v8.md
主Agent请勿阅读产出文件内容，直接将路径转发给相关方。
