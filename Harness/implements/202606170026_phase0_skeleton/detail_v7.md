# 详细设计（v7）

## 概述

实现 ai-impl 子模块的全部源文件：MockAiService（AiService 13 个方法的 Mock 占位实现）、NoOpDegradationStrategy（默认降级策略，始终不降级）、FallbackAiService（AiService 降级装饰器）及对应的单元测试。ai-impl 是 ai-api 的直接下游实现层。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| backend/ai-impl/pom.xml | 修改 | 添加 ai-api（compile scope）、common（compile scope）、spring-boot-starter（compile scope）、spring-boot-starter-test（test scope）依赖 |
| backend/ai-impl/src/main/java/com/aimedical/modules/ai/impl/mock/MockAiService.java | 新建 | 实现 AiService 13 个方法，返回 Mock 占位数据 |
| backend/ai-impl/src/main/java/com/aimedical/modules/ai/impl/degradation/NoOpDegradationStrategy.java | 新建 | 实现 DegradationStrategy，shouldDegrade() 始终返回 false |
| backend/ai-impl/src/main/java/com/aimedical/modules/ai/impl/fallback/FallbackAiService.java | 新建 | AiService 装饰器，遍历降级策略判定 + 兜底保护 |
| backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/mock/MockAiServiceTest.java | 新建 | MockAiService 13 方法返回非 null CompletableFuture、success=true、degraded=false、data 不为 null |
| backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/degradation/NoOpDegradationStrategyTest.java | 新建 | NoOpDegradationStrategy 始终返回 false |
| backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/fallback/FallbackAiServiceTest.java | 新建 | FallbackAiService 正常委托、降级触发、兜底保护 |

## 类型定义

### pom.xml 修改

**文件路径**：`backend/ai-impl/pom.xml`
**操作**：添加 ai-api（compile）、common（compile）、spring-boot-starter-test（test scope）依赖
**修改后依赖区块**：
```xml
<dependencies>
    <dependency>
        <groupId>com.aimedical</groupId>
        <artifactId>ai-api</artifactId>
    </dependency>
    <dependency>
        <groupId>com.aimedical</groupId>
        <artifactId>common</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### MockAiService

**形态**：class
**包路径**：com.aimedical.modules.ai.impl.mock
**职责**：AiService 的 Mock 占位实现，13 个方法返回固定结构 Mock 数据

```java
@Service
@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "true", matchIfMissing = true)
public class MockAiService implements AiService {

    @Override
    public CompletableFuture<AiResult<TriageResponse>> triage(TriageRequest request) { ... }

    @Override
    public CompletableFuture<AiResult<DiagnosisResponse>> diagnosis(DiagnosisRequest request) { ... }

    @Override
    public CompletableFuture<AiResult<PrescriptionCheckResponse>> prescriptionCheck(PrescriptionCheckRequest request) { ... }

    @Override
    public CompletableFuture<AiResult<MedicalRecordGenResponse>> generateMedicalRecord(MedicalRecordGenRequest request) { ... }

    @Override
    public CompletableFuture<AiResult<InspectionReportResponse>> analysisReportForInspection(InspectionReportRequest request) { ... }

    @Override
    public CompletableFuture<AiResult<LabTestReportResponse>> analysisReportForLabTest(LabTestReportRequest request) { ... }

    @Override
    public CompletableFuture<AiResult<ImageAnalysisResponse>> imageAnalysis(ImageAnalysisRequest request) { ... }

    @Override
    public CompletableFuture<AiResult<KbQueryResponse>> knowledgeBaseQuery(KbQueryRequest request) { ... }

    @Override
    public CompletableFuture<AiResult<ExaminationRecommendResponse>> recommendExamination(ExaminationRecommendRequest request) { ... }

    @Override
    public CompletableFuture<AiResult<PrescriptionAssistResponse>> prescriptionAssist(PrescriptionAssistRequest request) { ... }

    @Override
    public CompletableFuture<AiResult<ExecutionOrderResponse>> recommendExecutionOrder(ExecutionOrderRequest request) { ... }

    @Override
    public CompletableFuture<AiResult<ScheduleResponse>> schedule(ScheduleRequest request) { ... }

    @Override
    public CompletableFuture<AiResult<DiscussionConclusionResponse>> discussionConclusion(DiscussionConclusionRequest request) { ... }
}
```

**公开接口**：实现 AiService 全部 13 个方法

**方法行为契约**：
- 全部返回 `CompletableFuture.completedFuture(AiResult.success(mockData))`
- `AiResult.success(data)` 中：`success=true`, `degraded=false`, `errorCode=null`, `fallbackReason=null`
- `triage()`：构造 `TriageResponse`，设置 `recommendedDepartments` = 含 1 条 `RecommendedDepartment`（`departmentName="mock_departmentName"`），`reason="mock_reason"`
- 其余 12 个方法：new Response DTO 空实例直接传入 `AiResult.success(new XxxResponse())`

**装配方式**：通过 `@Service` 和 `@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "true", matchIfMissing = true)` 注册为 Spring Bean

**类型关系**：实现 `com.aimedical.modules.ai.api.AiService`

### NoOpDegradationStrategy

**形态**：class
**包路径**：com.aimedical.modules.ai.impl.degradation
**职责**：默认降级策略，`shouldDegrade()` 始终返回 false

```java
@Component
@ConditionalOnMissingBean(DegradationStrategy.class)
public class NoOpDegradationStrategy implements DegradationStrategy {

    @Override
    public boolean shouldDegrade(DegradationContext context) {
        return false;
    }
}
```

**公开接口**：`shouldDegrade(DegradationContext context) : boolean` — 始终返回 false

**装配方式**：通过 `@Component` 和 `@ConditionalOnMissingBean(DegradationStrategy.class)` 注册，当无其他 DegradationStrategy Bean 时生效

**类型关系**：实现 `com.aimedical.modules.ai.api.degradation.DegradationStrategy`

### FallbackAiService

**形态**：class
**包路径**：com.aimedical.modules.ai.impl.fallback
**职责**：AiService 装饰器，注入 AiService 委托列表和 DegradationStrategy 列表，遍历策略判定降级，兜底保护

```java
@Service
public class FallbackAiService implements AiService {

    private final List<AiService> delegates;
    private final List<DegradationStrategy> strategies;

    public FallbackAiService(List<AiService> aiServiceList,
                             List<DegradationStrategy> strategies) { ... }

    @Override
    public CompletableFuture<AiResult<TriageResponse>> triage(TriageRequest request) { ... }

    // ... 其余 12 个方法委托逻辑相同
}
```

**构造器签名**：
```java
public FallbackAiService(List<AiService> aiServiceList, List<DegradationStrategy> strategies)
```

**构造器行为契约**：
1. 从 `aiServiceList` 中排除自身实例：`aiServiceList.stream().filter(s -> !(s instanceof FallbackAiService)).collect(toList())`
2. 保存排除后的委托列表 `delegates`（可为空列表）和 `strategies` 列表

**委托分发逻辑**（13 个方法共享同一模式）：
1. 若 `delegates` 为空 → 启动期输出 ERROR 日志、运行期输出 WARN 日志，返回 `AiResult.degraded("No available AiService delegate")`
2. 若 `delegates` 非空 → 取第一个委托（`delegates.get(0)`）调用同名方法，获取返回的 `AiResult<T>`：
   - 若 `result.isSuccess()` 为 true 或 `result.isDegraded()` 为 true → 直接返回原 `result`
   - 若 `success` 为 false 且 `degraded` 为 false → 遍历 `strategies`，任一 `strategy.shouldDegrade(context)` 返回 true 则返回 `AiResult.degraded("Degraded by strategy")`
   - 若未触发降级 → 返回原 `result`

**日志说明**：
- 兜底空委托：首次（构造后首次调用任一方法）输出 ERROR 级别，后续调用输出 WARN 级别
- 使用 `LoggerFactory.getLogger(getClass())` 获取 logger

**装配方式**：通过 `@Service` 始终注册

**类型关系**：实现 `com.aimedical.modules.ai.api.AiService`，依赖 `com.aimedical.modules.ai.api.degradation.DegradationStrategy`

## 测试文件设计

### MockAiServiceTest

**包路径**：com.aimedical.modules.ai.impl.mock
**测试类**：`MockAiServiceTest`（class，package-private）

**测试方法**（14 个）：

| 测试方法 | 断言 |
|---------|------|
| `shouldBeAnnotatedWithService` | MockAiService 类标注 @Service |
| `triageShouldReturnMockData` | future 非 null & isDone, result.success=true, degraded=false, data != null, recommendedDepartments[0].departmentName="mock_departmentName", reason="mock_reason" |
| `diagnosisShouldReturnMockData` | future 非 null & isDone, result.success=true, degraded=false, data != null |
| `prescriptionCheckShouldReturnMockData` | 同上 |
| `generateMedicalRecordShouldReturnMockData` | 同上 |
| `analysisReportForInspectionShouldReturnMockData` | 同上 |
| `analysisReportForLabTestShouldReturnMockData` | 同上 |
| `imageAnalysisShouldReturnMockData` | 同上 |
| `knowledgeBaseQueryShouldReturnMockData` | 同上 |
| `recommendExaminationShouldReturnMockData` | 同上 |
| `prescriptionAssistShouldReturnMockData` | 同上 |
| `recommendExecutionOrderShouldReturnMockData` | 同上 |
| `scheduleShouldReturnMockData` | 同上 |
| `discussionConclusionShouldReturnMockData` | 同上 |

### NoOpDegradationStrategyTest

**包路径**：com.aimedical.modules.ai.impl.degradation
**测试类**：`NoOpDegradationStrategyTest`（class，package-private）

| 测试方法 | 断言 |
|---------|------|
| `shouldAlwaysReturnFalse` | strategy.shouldDegrade(new DegradationContext()) 始终返回 false |
| `shouldBeAnnotatedWithComponent` | NoOpDegradationStrategy 类标注 @Component |

### FallbackAiServiceTest

**包路径**：com.aimedical.modules.ai.impl.fallback
**测试类**：`FallbackAiServiceTest`（class，package-private）

| 测试方法 | 场景 | 断言 |
|---------|------|------|
| `shouldDelegateToFirstAvailableService` | 单委托返回 success AiResult | 结果 success=true, data 不为 null |
| `shouldReturnDegradedWhenStrategyTriggers` | 委托返回失败且策略判定降级 | 结果 degraded=true, fallbackReason="Degraded by strategy" |
| `shouldReturnOriginalResultWhenNoDegradationTriggered` | 委托返回失败且策略不降级 | 结果 success=false, degraded=false |
| `shouldReturnFallbackResultWhenNoDelegateAvailable` | 委托列表为空 | 结果 degraded=true, fallbackReason="No available AiService delegate" |
| `shouldExcludeSelfFromDelegates` | 构造时注入包含自身 | 委托列表不包含 FallbackAiService 实例 |
| `shouldReturnOriginalResultWhenDelegateAlreadyDegraded` | 委托返回 degraded=true | 结果 degraded=true, 直接返回原结果 |

## 错误处理

ai-impl 层不抛出受检异常。MockAiService 所有方法通过 `CompletableFuture.completedFuture()` 返回成功 CompletableFuture。FallbackAiService 在委托列表为空时返回 `AiResult.degraded("No available AiService delegate")`，降级触发时返回 `AiResult.degraded("Degraded by strategy")`，不抛出异常。

## 行为契约

1. MockAiService 所有方法返回已完成的 CompletableFuture，避免异步超时
2. MockAiService.triage() 返回的 TriageResponse 中 recommendedDepartments 含 1 条 departmentName="mock_departmentName"，reason="mock_reason"
3. MockAiService 其余 12 个方法返回的 Response DTO 为 new 空实例
4. NoOpDegradationStrategy.shouldDegrade() 始终返回 false
5. FallbackAiService 构造时自动排除自身实例，避免递归委托
6. FallbackAiService 委托分发优先检查 success 和 degraded 状态，短路策略遍历
7. FallbackAiService 兜底保护：无可用委托时返回降级结果而非抛出 NPE

## 依赖关系

| 模块 | 依赖 | scope |
|------|------|-------|
| ai-impl | ai-api | compile |
| ai-impl | common | compile |
| ai-impl | spring-boot-starter | compile |
| ai-impl | spring-boot-starter-test | test |

暴露给后续任务的公开接口：
- `com.aimedical.modules.ai.impl.mock.MockAiService` — 业务模块启动时作为 AiService 默认实现
- `com.aimedical.modules.ai.impl.degradation.NoOpDegradationStrategy` — 默认降级策略，可被业务模块替换
- `com.aimedical.modules.ai.impl.fallback.FallbackAiService` — 降级装饰器，业务模块可选注入

## 修订说明（v7 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| pom.xml 依赖区块遗漏 spring-boot-starter（compile scope） | 在"修改后依赖区块"中添加 spring-boot-starter 依赖项；更新"文件规划"表格和"依赖关系"表格 |
| 设计文档错误描述 pom.xml 当前状态 | 删除"当前状态：仅 artifactId，无依赖声明"描述，仅保留"修改后依赖区块"作为目标 |
