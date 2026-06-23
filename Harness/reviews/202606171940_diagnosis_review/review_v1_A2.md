# R2: AI 模块 (ai-api + ai-impl) 实现与 OOD 设计一致性审查

审查时间：2026-06-17

### 审查范围

```
ai-api/src/main/java/com/aimedical/modules/ai/api/
  ├── AiService.java
  ├── AiResult.java
  ├── degradation/DegradationContext.java
  ├── degradation/DegradationStrategy.java
  └── dto/
      ├── triage/       (TriageRequest, TriageResponse, RecommendedDepartment)
      ├── diagnosis/    (DiagnosisRequest, DiagnosisResponse)
      ├── prescription/ (PrescriptionCheckRequest/Response, PrescriptionAssistRequest/Response)
      ├── medicalrecord/(MedicalRecordGenRequest, MedicalRecordGenResponse)
      ├── inspection/   (InspectionReportRequest, InspectionReportResponse)
      ├── labtest/      (LabTestReportRequest, LabTestReportResponse)
      ├── image/        (ImageAnalysisRequest, ImageAnalysisResponse)
      ├── kb/           (KbQueryRequest, KbQueryResponse)
      ├── examination/  (ExaminationRecommendRequest, ExaminationRecommendResponse)
      ├── execution/    (ExecutionOrderRequest, ExecutionOrderResponse)
      ├── schedule/     (ScheduleRequest, ScheduleResponse)
      └── discussion/   (DiscussionConclusionRequest, DiscussionConclusionResponse)

ai-api/pom.xml
ai-impl/src/main/java/com/aimedical/modules/ai/impl/
  ├── mock/MockAiService.java
  ├── fallback/FallbackAiService.java
  └── degradation/NoOpDegradationStrategy.java
ai-impl/pom.xml

Test files:
ai-api/src/test/ (AiResultTest, AiServiceTest, DegradationStrategyTest, TriageDtoTest)
ai-impl/src/test/ (MockAiServiceTest, FallbackAiServiceTest, NoOpDegradationStrategyTest)
```

### 发现

#### [轻微] ai-impl/pom.xml 存在冗余 common 依赖

- **位置**：`AIMedical/backend/modules/ai/ai-impl/pom.xml:17-20`
- **描述**：ai-impl 的 POM 同时声明了对 `ai-api` 和 `common` 的 compile 依赖。由于 ai-api 已依赖 common，common 作为传递依赖可被 ai-impl 自动解析，显式声明是冗余的。根据 OOD §3.4 模块依赖规则（`modules/ai/ai-impl -> modules/ai/ai-api -> common`），ai-impl 只需声明 ai-api 依赖即可。冗余声明不影响编译运行，但会在依赖分析工具（`dependency:analyze`）中产生 Used undeclared 误报。
- **建议**：移除 ai-impl/pom.xml 中 `com.aimedical:common` 依赖条目，让 common 通过 ai-api 传递解析。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 0 |
| 一般 | 0 |
| 轻微 | 1 |

### 总评

AI 模块实现与 OOD Phase 0 设计高度一致。`AiService` 接口精准定义了 13 个类型化方法，返回 `CompletableFuture<AiResult<T>>`，与 §8.2 方法清单完全吻合。`AiResult` 包含 success/data/errorCode/degraded/fallbackReason 字段并提供工厂方法。Triage 三件套 DTO 按设计携带 `@phase0-mock-field` 字段子集，其余 12 组 DTO 保持空壳类声明。`DegradationContext` 为零值构造器，`DegradationStrategy` 接口定义正确。`MockAiService` 实现全部 13 个方法，条件注解与 OOD §3.4 装配策略一致，triage 方法 Mock 数据填充遵循 `"mock_" + 字段名` 约定。`FallbackAiService` 正确使用构造器注入、排除自身选委托、空委托时返回降解结果。`NoOpDegradationStrategy` 条件注解配置正确。测试覆盖全面（7 个测试类），涵盖接口契约、Mock 数据、降级逻辑、日志行为。唯一发现的问题是 ai-impl/pom.xml 中 `common` 依赖冗余，建议移除以保持依赖声明最小化原则。
