# Phase 5 包 G — AI 进阶底座 架构级 OOD 设计方案（v3）

## 1. 概述

### 1.1 设计目标

Phase 5 包 G 交付 AI 进阶底座（AI Advanced Platform），为平台全部 AI 能力提供统一的运行时基础设施。设计目标如下：

- **能力迁移统一化**：将 Phase 2~4 各阶段独立接入的核心 AI 能力（3.4.1/3.4.2/3.4.3/3.4.10）以及 Phase 5 首次落地的能力（3.4.8/3.4.12/3.4.13）统一迁移至本底座，消除分散接入导致的重复代码和不一致的降级/超时/重试策略；Phase 4 已独立接入的 6 项 AI 能力（3.4.4/3.4.5/3.4.6/3.4.7/3.4.9/3.4.11）不迁移至底座，通过薄适配器集成到 AiOrchestrator 路由框架
- **模型对接标准化**：实现大模型统一对接层，支持多供应商模型路由与切换，业务层不感知具体模型实现
- **对话模板可配置化**：AI 对话模板（Prompt Template）按能力/科室维度可配置、可版本化管理，支持运行时热加载
- **A/B 实验可控化**：提供轻量级 A/B 实验框架，支持按能力维度分配流量到不同模型或 Prompt 版本，实验结果可观测
- **性能观测内建化**：为全部 AI 能力提供统合的调用指标采集、耗时分布、降级率统计与告警能力

### 1.2 整体架构思路

AI 进阶底座定位为 **ai-impl 子模块内部的分层架构**，在现有 `AiService` 接口不变的前提下，将原来 `MockAiService` 的扁平实现替换为多层管线：

```
业务模块 → AiService 接口（ai-api，不变）
              ↓
         FallbackAiService（装饰器，不变接口但内部装配策略变更）
              ↓
         AiOrchestrator（编排层）
              ↓
         ┌───────────────────────────────┐
         │  AI 进阶底座                   │
         │  ├── ModelRouter              │  模型路由
         │  ├── PromptTemplateManager    │  Prompt 模板管理
         │  ├── ExperimentManager         │  A/B 实验
         │  ├── AiMetricsCollector       │  性能观测
         │  └── LlmClient                │  大模型统一客户端
         └───────────────────────────────┘
              ↓
         外部大模型服务（HTTP API / Spring AI ChatModel）
```

### 1.3 核心抽象一览

| 抽象 | 类型形态 | 职责定位 |
|------|---------|---------|
| `AiOrchestrator` | class | 统一编排路由层，实现 `AiService` 接口全部 13 个方法，通过能力标识映射表查找并委托给对应的 `CapabilityExecutor` 实例；持有共享基础设施（`SlidingWindowMetricsStore`、`ModelEndpointHealthManager`），不介入管线内部步骤 |
| `CapabilityExecutor<T, R>` | interface | 单项 AI 能力的泛型执行契约，定义各能力完整执行管线（降级预检→模板渲染→实验分流→模型路由→模型调用→结果解析→指标采集→降级兜底），由实现类特化各步骤行为 |
| `ModelRouter` | interface | 模型路由契约，根据能力标识与实验分组决定本次调用使用哪个模型配置 |
| `ModelRoute` | class | 模型路由条目值对象，封装模型标识、端点地址、权重等路由元数据 |
| `LlmClient` | interface | 大模型统一调用客户端，屏蔽 HTTP API / Spring AI ChatModel 等底层差异 |
| `LlmRequest` | class | 大模型统一请求值对象，携带渲染后的 Prompt 文本、模型参数与能力标识 |
| `LlmResponse` | class | 大模型统一响应值对象，携带原始文本输出、Token 用量与模型标识 |
| `PromptTemplateManager` | interface | Prompt 模板管理契约，支持按能力/科室/版本检索与渲染模板 |
| `PromptTemplate` | class | Prompt 模板值对象，含模板标识、能力标识、科室标识、模板内容、版本号与启用状态 |
| `ExperimentManager` | interface | A/B 实验管理契约，根据能力标识与上下文决定实验分组 |
| `ExperimentAssignment` | class | 实验分组结果值对象，含实验标识、分组标识与目标模型/Prompt版本 |
| `AiMetricsCollector` | interface | AI 性能指标采集契约，记录每次调用的能力标识、耗时、是否降级、Token 用量等 |
| `AiCallRecord` | class | 单次 AI 调用记录值对象，作为 `AiMetricsCollector` 的入参与 `AI 调用日志`（5.2）持久化的数据源 |
| `AiCallLogEntity` | JPA @Entity | AI 调用日志 JPA 实体，与 `AiCallRecord` 字段对等 |
| `AiRequestBase` | abstract class | AI 能力请求 DTO 基类，封装 visitId/patientId/sessionId 等跨能力通用字段，归属 `ai-api/dto/base/` |
| `StructuredOutputParser` | interface | 结构化输出解析契约，将 LLM 原始文本输出解析为各能力对应的 Java DTO |
| `DegradationStrategy` | interface | 降级策略契约（Phase 0 已定义，本阶段扩展 `DegradationContext` 字段） |
| `DegradationContext` | class | 降级判定上下文（Phase 0 已定义骨架，本阶段扩展字段，含 Builder 模式） |
| `LocalRuleFallback` | interface | 本地规则降级契约，为 3.4.2 处方审核等需要本地规则兜底的能力提供降级执行入口 |
| `SlidingWindowMetricsStore` | class | 每个能力标识的调用指标滑动窗口存储，为降级策略提供数据源 |
| `ModelEndpointHealthManager` | class | 模型端点健康状态管理器，维护每个端点的 CONNECTED/DEGRADED/UNAVAILABLE 状态与探测触发逻辑 |

---

## 2. 模块划分

### 2.1 目录结构

```
backend/modules/ai/
├── ai-api/ src/main/java/com/aimedical/modules/ai/api/
│   ├── AiService.java                      # 不变
│   ├── AiResult.java                       # 不变
│   ├── degradation/
│   │   ├── DegradationStrategy.java         # 不变（接口签名冻结）
│   │   └── DegradationContext.java          # 扩展字段（保持二进制兼容）
│   └── dto/                                # 不变
│
├── ai-impl/ src/main/java/com/aimedical/modules/ai/impl/
│   ├── orchestrator/
│   │   ├── AiOrchestrator.java             # 统一编排层，实现 AiService
│   │   ├── CapabilityExecutor.java         # 能力执行器泛型接口（含 execute 方法签名）
│   │   └── impl/
│   │       ├── TriageCapabilityExecutor.java
│   │       ├── PrescriptionCheckCapabilityExecutor.java
│   │       ├── MedicalRecordGenCapabilityExecutor.java
│   │       ├── PrescriptionAssistCapabilityExecutor.java
│   │       ├── KbQueryCapabilityExecutor.java
│   │       ├── ScheduleCapabilityExecutor.java
│   │       ├── DiscussionConclusionCapabilityExecutor.java
│   │       ├── DiagnosisCapabilityExecutor.java
│   │       ├── AnalysisReportForInspectionCapabilityExecutor.java
│   │       ├── AnalysisReportForLabTestCapabilityExecutor.java
│   │       ├── ImageAnalysisCapabilityExecutor.java
│   │       ├── RecommendExaminationCapabilityExecutor.java
│   │       └── RecommendExecutionOrderCapabilityExecutor.java
│   ├── router/
│   │   ├── ModelRouter.java                # 模型路由接口
│   │   ├── DefaultModelRouter.java         # 默认路由实现（基于能力标识 + 配置映射）
│   │   └── ModelRoute.java                # 路由条目值对象
│   ├── client/
│   │   ├── LlmClient.java                 # 大模型统一客户端接口
│   │   ├── HttpApiLlmClient.java          # HTTP API 调用实现
│   │   ├── SpringAiLlmClient.java         # Spring AI ChatModel 实现
│   │   ├── LlmRequest.java               # 统一请求值对象
│   │   └── LlmResponse.java              # 统一响应值对象
│   ├── template/
│   │   ├── PromptTemplateManager.java      # 模板管理接口
│   │   ├── PromptTemplate.java            # 模板值对象（JPA Entity）
│   │   ├── DatabasePromptTemplateManager.java # 数据库持久化实现
│   │   └── PromptTemplateRepository.java  # JPA Repository
│   ├── experiment/
│   │   ├── ExperimentManager.java          # 实验管理接口
│   │   ├── ExperimentAssignment.java       # 分组结果值对象
│   │   ├── Experiment.java                # 实验配置值对象（JPA Entity）
│   │   ├── ExperimentRepository.java      # JPA Repository
│   │   └── HashBucketExperimentManager.java # 哈希分桶实现
│   ├── metrics/
│   │   ├── AiMetricsCollector.java         # 指标采集接口
│   │   ├── AiCallRecord.java              # 调用记录值对象
│   │   ├── AiCallLogEntity.java           # AI 调用日志 JPA 实体（新增）
│   │   ├── AiCallLogRepository.java       # AI 调用日志 JPA Repository
│   │   ├── LoggingMetricsCollector.java   # 日志输出实现
│   │   ├── SlidingWindowMetricsStore.java # 调用指标滑动窗口存储（新增）
│   │   └── ModelEndpointHealthManager.java # 模型端点健康状态管理器（新增）
│   ├── parser/
│   │   ├── StructuredOutputParser.java     # 结构化输出解析接口
│   │   └── JsonStructuredOutputParser.java # JSON 结构化解析实现
│   ├── fallback/
│   │   ├── LocalRuleFallback.java         # 本地规则降级接口
│   │   └── PrescriptionLocalRuleFallback.java # 处方审核本地规则实现
│   ├── mock/
│   │   └── MockAiService.java             # 保留，Phase 5+ 仅用于开发/测试
│   ├── degradation/
│   │   ├── NoOpDegradationStrategy.java    # 保留
│   │   ├── TimeoutDegradationStrategy.java # 超时降级策略
│   │   └── CircuitBreakerDegradationStrategy.java # 熔断降级策略
│   ├── config/
│   │   └── AiPlatformConfig.java         # 底座 Bean 装配与配置属性绑定
│   └── FallbackAiService.java             # 装饰器（装配策略变更）
```

### 2.2 模块依赖方向

```
ai-api ←──────────────────────────── ai-impl
  │                                      │
  │  (interface + DTO, no impl dep)     ├── orchestrator/ ──> router/, template/, experiment/, client/, fallback/, degradation/, metrics/
  │                                     ├── router/ ──> config (YaML/DB)
  │                                     ├── client/ ──> Spring AI / HTTP (外部依赖)
  │                                     ├── template/ ──> JPA Repository
  │                                     ├── experiment/ ──> JPA Repository
  │                                     ├── metrics/ ──> JPA Repository + Micrometer
  │                                     ├── parser/ ──> ai-api DTO
  │                                     └── fallback/ ──> ai-api DTO + 业务规则
```

**依赖规则**：
- `ai-api` 保持不变，业务模块仅依赖 `ai-api`
- `ai-impl` 内部各子包之间按单向依赖：orchestrator 为顶层编排，依赖其余子包；其余子包之间不互相依赖
- `client/` 是唯一引入外部大模型依赖（Spring AI / HTTP 客户端）的子包
- `template/`、`experiment/`、`metrics/` 各自拥有一套 JPA Repository + Entity，数据持久化独立

### 2.3 类图

```mermaid
classDiagram
    class AiService {
        <<interface>>
        +triage(TriageRequest) CompletableFuture~AiResult~TriageResponse~~
        +prescriptionCheck(PrescriptionCheckRequest) CompletableFuture~AiResult~PrescriptionCheckResponse~~
        +generateMedicalRecord(MedicalRecordGenRequest) CompletableFuture~AiResult~MedicalRecordGenResponse~~
        +prescriptionAssist(PrescriptionAssistRequest) CompletableFuture~AiResult~PrescriptionAssistResponse~~
        +knowledgeBaseQuery(KbQueryRequest) CompletableFuture~AiResult~KbQueryResponse~~
        +schedule(ScheduleRequest) CompletableFuture~AiResult~ScheduleResponse~~
        +discussionConclusion(DiscussionConclusionRequest) CompletableFuture~AiResult~DiscussionConclusionResponse~~
        +imageAnalysis(ImageAnalysisRequest) CompletableFuture~AiResult~ImageAnalysisResponse~~
        +... (其余 5 项能力方法)
    }

    class FallbackAiService {
        <<decorator>>
        -AiService delegate
        +triage() CompletableFuture~AiResult~TriageResponse~~
        +prescriptionCheck() CompletableFuture~AiResult~PrescriptionCheckResponse~~
        +... (委托给 delegate + 降级兜底)
    }

    class AiOrchestrator {
        -Map~String, CapabilityExecutor~ executorMap
        -SlidingWindowMetricsStore metricsStore
        -ModelEndpointHealthManager endpointHealthManager
        +triage() CompletableFuture~AiResult~TriageResponse~~
        +prescriptionCheck() CompletableFuture~AiResult~PrescriptionCheckResponse~~
        +... (按能力标识路由到对应 CapabilityExecutor)
    }

    class CapabilityExecutor~T, R~ {
        <<interface>>
        +execute(T request, String capabilityId) CompletableFuture~AiResult~R~~
        +getCapabilityId() String
        +getInputType() Class~T~
        +getOutputType() Class~R~
    }

    class TriageCapabilityExecutor {
    }
    class PrescriptionCheckCapabilityExecutor {
    }
    class MedicalRecordGenCapabilityExecutor {
    }
    class PrescriptionAssistCapabilityExecutor {
    }
    class KbQueryCapabilityExecutor {
    }
    class ScheduleCapabilityExecutor {
    }
    class DiscussionConclusionCapabilityExecutor {
    }
    class DiagnosisCapabilityExecutor {
    }
    class AnalysisReportForInspectionCapabilityExecutor {
    }
    class AnalysisReportForLabTestCapabilityExecutor {
    }
    class ImageAnalysisCapabilityExecutor {
    }
    class RecommendExaminationCapabilityExecutor {
    }
    class RecommendExecutionOrderCapabilityExecutor {
    }

    class ModelRouter {
        <<interface>>
        +route(String capabilityId, ExperimentAssignment assignment) ModelRoute
    }
    class DefaultModelRouter {
    }
    class ModelRoute {
        +String modelId
        +String endpoint
        +ClientType clientType
    }

    class LlmClient {
        <<interface>>
        +invoke(LlmRequest request) LlmResponse
    }
    class HttpApiLlmClient {
    }
    class SpringAiLlmClient {
    }
    class LlmRequest {
        +String prompt
        +String modelId
        +Map~String,Object~ parameters
    }
    class LlmResponse {
        +String text
        +TokenUsage tokenUsage
        +String modelId
    }

    class PromptTemplateManager {
        <<interface>>
        +render(String capabilityId, String departmentId, Map~String,Object~ variables) String
    }
    class DatabasePromptTemplateManager {
    }
    class PromptTemplate {
        +Long id
        +String capabilityId
        +String departmentId
        +String content
        +int version
        +TemplateStatus status
    }
    class PromptTemplateRepository {
        <<JPA Repository>>
    }

    class ExperimentManager {
        <<interface>>
        +assign(String capabilityId, String userId, String sessionId) ExperimentAssignment
    }
    class HashBucketExperimentManager {
    }
    class Experiment {
        +Long id
        +String capabilityId
        +List~ExperimentGroup~ groups
        +ExperimentStatus status
        +LocalDateTime startTime
        +LocalDateTime endTime
    }
    class ExperimentAssignment {
        +String experimentId
        +String groupId
        +String targetModelId
        +Integer targetPromptVersion
    }

    class AiMetricsCollector {
        <<interface>>
        +record(AiCallRecord record) void
    }
    class LoggingMetricsCollector {
    }
    class AiRequestBase {
        <<abstract>>
        +String visitId
        +String patientId
        +String sessionId
    }

    class AiCallRecord {
        +LocalDateTime callTime
        +String capabilityId
        +String capabilityName
        +String visitId
        +String patientId
        +String callerRole
        +String callerId
        +String inputSummary
        +String outputSummary
        +boolean degraded
        +String degradationReason
        +long elapsedMs
        +String errorCode
        +String errorMessage
        +String modelId
        +int retryCount
        +String sessionId
        +Integer promptTokens
        +Integer completionTokens
        +Integer totalTokens
    }
    class AiCallLogEntity {
        <<JPA @Entity>>
        +Long id
        +LocalDateTime callTime
        +String capabilityId
        +String capabilityName
        +String visitId
        +String patientId
        +String callerRole
        +String callerId
        +String inputSummary
        +String outputSummary
        +boolean degraded
        +String degradationReason
        +long elapsedMs
        +String errorCode
        +String errorMessage
        +String modelId
        +int retryCount
        +String sessionId
        +Integer promptTokens
        +Integer completionTokens
        +Integer totalTokens
    }
    class AiCallLogRepository {
        <<JPA Repository>>
    }

    class SlidingWindowMetricsStore {
        +recordSuccess(String capabilityId, long elapsedMs) void
        +recordFailure(String capabilityId) void
        +getFailureRate(String capabilityId) double
        +getAverageElapsed(String capabilityId) double
        +buildDegradationContext(String capabilityId, String requestType) DegradationContext
    }

    class ModelEndpointHealthManager {
        +getState(String endpointId) EndpointState
        +recordCallResult(String endpointId, boolean success, long elapsedMs) void
        +tryProbe(String endpointId) boolean
    }

    class DegradationStrategy {
        <<interface>>
        +shouldDegrade(DegradationContext context) boolean
        +getOrder() int [default method returns 0]
    }
    class DegradationContext {
        <<Serializable>>
        +int invocationCount
        +long lastFailureTime
        +long elapsedTime
        +String requestType
        +int failureCount
        +Builder builder()
    }
    class TimeoutDegradationStrategy {
        +shouldDegrade() boolean
    }
    class CircuitBreakerDegradationStrategy {
        +shouldDegrade() boolean
    }
    class NoOpDegradationStrategy {
    }

    class StructuredOutputParser {
        <<interface>>
        +parse(LlmResponse response, Class~T~ targetClass) T
    }
    class JsonStructuredOutputParser {
    }

    class LocalRuleFallback {
        <<interface>>
        +fallback(Object request) AiResult
    }
    class PrescriptionLocalRuleFallback {
    }

    AiService <|.. FallbackAiService : implements
    AiService <|.. AiOrchestrator : implements
    AiService <|.. MockAiService : implements

    FallbackAiService o--> AiService : wraps (delegate)

    AiOrchestrator o--> "1..*" CapabilityExecutor : routes via Map
    AiOrchestrator o--> "1" SlidingWindowMetricsStore : has
    AiOrchestrator o--> "1" ModelEndpointHealthManager : has

    CapabilityExecutor <|.. TriageCapabilityExecutor
    CapabilityExecutor <|.. PrescriptionCheckCapabilityExecutor
    CapabilityExecutor <|.. MedicalRecordGenCapabilityExecutor
    CapabilityExecutor <|.. PrescriptionAssistCapabilityExecutor
    CapabilityExecutor <|.. KbQueryCapabilityExecutor
    CapabilityExecutor <|.. ScheduleCapabilityExecutor
    CapabilityExecutor <|.. DiscussionConclusionCapabilityExecutor
    CapabilityExecutor <|.. DiagnosisCapabilityExecutor
    CapabilityExecutor <|.. AnalysisReportForInspectionCapabilityExecutor
    CapabilityExecutor <|.. AnalysisReportForLabTestCapabilityExecutor
    CapabilityExecutor <|.. ImageAnalysisCapabilityExecutor
    CapabilityExecutor <|.. RecommendExaminationCapabilityExecutor
    CapabilityExecutor <|.. RecommendExecutionOrderCapabilityExecutor

    CapabilityExecutor --> ModelRouter : uses
    CapabilityExecutor --> LlmClient : uses
    CapabilityExecutor --> PromptTemplateManager : uses
    CapabilityExecutor --> ExperimentManager : uses
    CapabilityExecutor --> StructuredOutputParser : uses
    CapabilityExecutor --> AiMetricsCollector : uses
    CapabilityExecutor --> LocalRuleFallback : optional uses
    CapabilityExecutor --> SlidingWindowMetricsStore : uses
    CapabilityExecutor --> DegradationStrategy : uses (per-capability injection)
    CapabilityExecutor --> ModelEndpointHealthManager : optional uses

    ModelRouter <|.. DefaultModelRouter
    ModelRouter --> ModelRoute : returns

    LlmClient <|.. HttpApiLlmClient
    LlmClient <|.. SpringAiLlmClient
    LlmClient --> LlmRequest : receives
    LlmClient --> LlmResponse : returns

    PromptTemplateManager <|.. DatabasePromptTemplateManager
    DatabasePromptTemplateManager --> PromptTemplateRepository : reads/writes
    PromptTemplateRepository --> PromptTemplate : manages

    ExperimentManager <|.. HashBucketExperimentManager
    HashBucketExperimentManager --> ExperimentRepository : reads
    ExperimentRepository --> Experiment : manages

    AiMetricsCollector <|.. LoggingMetricsCollector
    LoggingMetricsCollector --> AiCallLogRepository : writes
    AiCallLogRepository --> AiCallLogEntity : persists

    DegradationStrategy <|.. NoOpDegradationStrategy
    DegradationStrategy <|.. TimeoutDegradationStrategy
    DegradationStrategy <|.. CircuitBreakerDegradationStrategy
    TimeoutDegradationStrategy --> SlidingWindowMetricsStore : reads
    CircuitBreakerDegradationStrategy --> SlidingWindowMetricsStore : reads

    StructuredOutputParser <|.. JsonStructuredOutputParser

    LocalRuleFallback <|.. PrescriptionLocalRuleFallback
```

---

## 3. 核心抽象

### 3.1 编排层

#### `AiOrchestrator` — 统一编排路由器（class，归属 `ai-impl/orchestrator/`）

**职责**：替代原 `MockAiService` 成为 `FallbackAiService` 的实际委托对象（`ai.platform.enabled=true` 时激活），实现 `AiService` 全部 13 个方法。每个方法的执行流程为按能力标识查找对应的 `CapabilityExecutor` 并委托其完成完整执行管线：

1. **查找执行器**：按能力标识从内部映射表（`Map<String, CapabilityExecutor>`）中查找匹配的 `CapabilityExecutor`
2. **委托执行**：调用 `executor.execute(request, capabilityId)` 返回 `CompletableFuture<AiResult<R>>`
3. **返回结果**：将执行器返回的 `AiResult` 直接返回给调用方

`AiOrchestrator` 不介入管线内部步骤（模板渲染、实验分流、模型路由、LLM 调用、结果解析、指标采集、降级判定与降级兜底），上述步骤由 `CapabilityExecutor.execute()` 在其内部完整执行。

**能力标识到 CapabilityExecutor 的映射机制**（v4 新增）：
- 所有 `CapabilityExecutor` 实现注册为 Spring Bean（`@Component`），通过 `getCapabilityId()` 返回能力标识
- `AiOrchestrator` 在 `@PostConstruct` 阶段扫描 `List<CapabilityExecutor>` 自动注入，按 `getCapabilityId()` 构建 `Map<String, CapabilityExecutor>` 映射表
- 未注册对应执行器的能力标识在被 `AiOrchestrator` 接收时将抛出明确的配置异常（启动期 fail-fast 而非运行时静默降级）

**Phase 4 能力的处理策略**（v4 新增）：
- 13 项 AI 能力中，7 项归属 Phase 5 底座范围（3.4.1/3.4.2/3.4.3/3.4.8/3.4.10/3.4.12/3.4.13），使用完整 CapabilityExecutor 管线
- 其余 6 项（3.4.4 AI 智能诊断 / 3.4.5 AI 智能检查报告 / 3.4.6 AI 智能检验报告 / 3.4.7 AI 影像分析 / 3.4.9 AI 开立检查检验 / 3.4.11 AI 执行顺序推荐）已在 Phase 4 完成独立接入，不迁移至 Phase 5 底座
- 这 6 项能力在 `AiOrchestrator` 中注册为薄适配器型 CapabilityExecutor，其 `execute()` 方法直接委托给 Phase 4 的现有业务服务接口，不做底座管线绕行；底座仅为它们提供统一的降级判定入口与指标采集入口

**协作对象**：
- 实现 `AiService`，被 `FallbackAiService` 委托调用
- 内部持有 `Map<String, CapabilityExecutor>`（按能力标识索引的映射表）、`SlidingWindowMetricsStore`、`ModelEndpointHealthManager`
- 每个 `AiService` 方法的实现通过能力标识从映射表中查找对应的 `CapabilityExecutor` 并调用其 `execute()` 方法

**为何使用 class 而非 interface**：编排器是唯一的运行时实现实例，不需要多态；其核心职责是路由委托而非管线编排。Bean 装配通过 `AiPlatformConfig` 显式完成。

**线程安全模型**：AiOrchestrator 内部持有可变状态（通过 `SlidingWindowMetricsStore` 维护的滑动窗口），其线程安全性取决于被编排组件的线程安全性。`AiOrchestrator` 本身不引入 `synchronized` 大锁；并发瓶颈由各子组件独立承担。编码层面：`SlidingWindowMetricsStore` 内部使用 `ConcurrentHashMap` 和 `AtomicLong` 保证并发安全；`Map<String, CapabilityExecutor>` 在初始化后不再变更，读操作无竞争。

**Bean 装配策略**（v2 修订，解决二义性）：
- `FallbackAiService`：标注 `@Primary`，通过 `ObjectProvider<AiService>` 延迟解析被装饰的 `AiService` 实例。由于 `@ConditionalOnProperty` 保证同时只有一个非装饰器 `AiService` 实现有效，`ObjectProvider.getIfUnique()` 可正确解析
- `AiOrchestrator`：标注 `@ConditionalOnProperty(name = "ai.platform.enabled", havingValue = "true")`
- `MockAiService`：标注 `@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "true", matchIfMissing = true)`，与现有代码兼容
- `AiPlatformConfig`：作为统一配置入口，通过 `EnvironmentPostProcessor` 机制在 Spring 启动早期将 `ai.platform.enabled` 转发到 `ai.mock.enabled` 配置项，确保 `MockAiService` 的条件注解与现有代码兼容
- `FallbackAiService` 不参与 `ai.platform.enabled` 条件，始终存在，且通过 `@Primary` 确保业务模块注入时优先选择

```yaml
ai:
  platform:
    enabled: true                     # true → AiOrchestrator 激活；false → 底座关闭
  mock:
    enabled: false                    # false → MockAiService 不激活（true 时激活，仅开发/测试）
```

#### `CapabilityExecutor<T, R>` — 能力执行泛型接口（interface，归属 `ai-impl/orchestrator/`）

**职责**：定义单项 AI 能力的完整执行管线契约。每个 AI 能力（如智能分诊、处方审核等）对应一个 `CapabilityExecutor` 实现，封装该能力的完整执行流程：

1. **降级预检**：通过 `SlidingWindowMetricsStore.buildDegradationContext()` 构建降级判定上下文，遍历注入的降级策略链；任一策略判定降级则直接跳至第 8 步降级兜底
2. **模板渲染**：委托 `PromptTemplateManager` 按能力标识与科室标识检索模板，将业务 DTO 字段注入模板变量，生成渲染后 Prompt
3. **实验分流**：委托 `ExperimentManager` 判定当前请求是否命中 A/B 实验，若命中则返回分组信息
4. **模型路由**：委托 `ModelRouter` 根据能力标识与实验分组选择目标模型配置（`ModelRoute`）；若返回 null 则直接跳至第 8 步降级兜底
5. **模型调用**：委托 `LlmClient` 发送渲染后 Prompt 到目标模型，获取原始文本输出；调用前设置硬超时
6. **结果解析**：委托 `StructuredOutputParser` 将原始文本输出解析为对应能力的 Java DTO
7. **指标采集**：委托 `AiMetricsCollector` 记录本次调用的能力标识、耗时、是否降级、Token 用量、错误码等；同时将耗时和结果记录到 `SlidingWindowMetricsStore`
8. **降级兜底**：若有 `LocalRuleFallback` 实现则执行本地规则降级，否则返回 `AiResult.degraded()`

**方法签名**（v2 新增）：
```
CompletableFuture<AiResult<R>> execute(T request, String capabilityId)
    入参: T request — 能力对应的业务请求 DTO（如 TriageRequest）
    入参: String capabilityId — 能力标识（如 "TRIAGE"），用于路由、模板检索和指标上报
    返回值: CompletableFuture<AiResult<R>> — 异步返回包裹能力对应的业务响应 DTO，成功时通过 CompletableFuture.complete() 返回，失败或降级时以 CompletableFuture.complete(降级结果) 完成
    异常: 不抛出业务异常；LLM 调用失败、解析失败等均通过降级路径以 CompletableFuture 完成（非异常路径）

String getCapabilityId()
    返回值: 该执行器对应的能力标识字符串

Class<T> getInputType()
Class<R> getOutputType()
    返回值: 输入/输出 DTO 的 Class 对象，用于 AiOrchestrator 的泛型路由与参数校验
```

**协作对象**：
- 被 `AiOrchestrator` 在对应方法中通过 `getCapabilityId()` 匹配查找并调用 `execute()`
- 内部使用注入的 `PromptTemplateManager`、`ModelRouter`、`LlmClient`、`StructuredOutputParser`、`AiMetricsCollector`、`SlidingWindowMetricsStore`、`ModelEndpointHealthManager` 完成管线
- 内部持有注入的 `List<DegradationStrategy>`（按能力标识配置的策略列表，由 `AiPlatformConfig` 按白名单分配，各实现不共享策略列表实例）
- 可选关联 `LocalRuleFallback`（仅处方审核等需要本地规则降级的能力）

**降级策略注入机制**（v4 新增）：每个 `CapabilityExecutor` 实现注入各自的 `List<DegradationStrategy>`，策略列表通过 Spring `@Qualifier("capabilityId")` 按能力标识注入，由 `AiPlatformConfig` 中的 `ai.degradation.strategies` 配置驱动。降级预检时，`execute()` 内部按 `getOrder()` 升序遍历策略链。

**变量提取约定**（v4 新增）：`CapabilityExecutor` 实现从业务请求 DTO 中提取 Prompt 模板变量，采用以下两种方式之一：
- **方式 A（默认）**：通过 Jackson `ObjectMapper.convertValue(request, Map.class)` 将 DTO 转为扁平键值对，适用于字段结构简单的 DTO
- **方式 B**：实现自定义 `extractVariables(T request)` 方法，适用于需要字段变换、拼接或条件过滤的场景
- 选择规则：若 DTO 字段名与模板变量名直接对应且无需预处理，使用方式 A；否则实现方式 B

**为何使用泛型 interface + 独立实现**：
- 13 项能力输入/输出类型各不相同，泛型 `T` / `R` 使 `execute()` 方法签名类型安全，避免 Object 强制转型
- 独立实现使每个能力的降级预检、模板变量映射、输出解析逻辑可独立定制与单元测试
- 新增 AI 能力只需新增一个 `CapabilityExecutor` 实现即可自动注册到 `AiOrchestrator` 的映射表中

### 3.2 模型对接层

#### `LlmClient` — 大模型统一客户端（interface，归属 `ai-impl/client/`）

**职责**：定义大模型调用的统一协议。屏蔽 HTTP API 直接调用与 Spring AI ChatModel 两种底层接入方式的差异，为上层（`CapabilityExecutor`）提供一致的调用体验。

**线程模型**：`LlmClient` 本身**无状态，线程安全**。HTTP 客户端基于连接池实现，每次调用不持有端点级别可变状态。

**为何使用 interface**：模型接入方式存在 HTTP API 和 Spring AI 两种异构实现，且未来可能新增 gRPC 或其他协议接入方式。

#### `ModelEndpointHealthManager` — 模型端点健康状态管理器（class，归属 `ai-impl/metrics/`）

**职责**：为每个模型端点（由 `ModelRoute.endpointId` 标识）维护独立的健康状态，供 `CapabilityExecutor` 在管线执行中、模型调用之前判定目标模型端点是否健康。`LlmClient` 本身不持有此状态，仅做调用转发。

**状态模型**：
```
每个模型端点维护独立的状态：
  CONNECTED ←→ DEGRADED ←→ UNAVAILABLE
  - CONNECTED: 正常状态，调用直接发送
  - DEGRADED: 近窗口内连续 3 次调用耗时 > 阈值，启动降级（仍尝试调用但上报告警）
  - UNAVAILABLE: 连续 5 次调用失败（HTTP 5xx / 连接拒绝），不再发送调用，直接返回失败
  状态恢复: DEGRADED 下若 1 次调用正常 → 回到 CONNECTED；
          UNAVAILABLE 下每 30 秒允许一次探测调用（类似 HALF_OPEN），成功则回到 CONNECTED
```

**探测调用触发机制**：`CapabilityExecutor` 在管线执行中（模型路由之后、LLM 调用之前）调用 `ModelEndpointHealthManager.getState()` 检查目标模型端点的健康状态。若为 UNAVAILABLE 且距离上次探测超过 30 秒，`tryProbe()` 返回 true，允许一次探测调用（`LlmClient.invoke()` 正常发送，但超时阈值缩短为正常值的 50%）；探测成功则将状态回退至 CONNECTED，探测失败则重置 30 秒等待计时。若 `tryProbe()` 返回 false（未到探测窗口），则跳过 LLM 调用直接触发降级。该逻辑与 `CircuitBreakerDegradationStrategy` 的 HALF_OPEN 状态协作但不耦合。

#### `LlmRequest` — 大模型统一请求（class，归属 `ai-impl/client/`）

**职责**：封装一次大模型调用的全部入参，包括渲染后的 Prompt 文本、模型标识、生成参数（temperature、maxTokens 等）与能力标识。

#### `LlmResponse` — 大模型统一响应（class，归属 `ai-impl/client/`）

**职责**：封装一次大模型调用的原始输出，包括文本内容、Token 用量、使用的模型标识与响应时间戳。

#### `ModelRouter` — 模型路由契约（interface，归属 `ai-impl/router/`）

**职责**：根据能力标识与可选的实验分组信息，决定本次调用应使用哪个模型配置（端点地址、模型名称、客户端类型）。

**协作对象**：
- 被 `CapabilityExecutor` 在模型路由步骤中调用
- 返回 `ModelRoute` 值对象供 `LlmClient` 选择实例与构建 `LlmRequest`
- 与 `ExperimentManager` 协作：若实验分组指定覆盖模型，优先采用分组的模型配置

#### `ModelRoute` — 模型路由条目（class，归属 `ai-impl/router/`）

**职责**：封装一条模型路由的目标信息，包括模型标识、端点 URL、客户端类型（HTTP_API / SPRING_AI）、生成参数默认值与权重。

### 3.3 Prompt 模板管理层

#### `PromptTemplateManager` — Prompt 模板管理契约（interface，归属 `ai-impl/template/`）

**职责**：
- 按能力标识与可选科室标识检索当前生效的 Prompt 模板
- 支持模板渲染：将业务 DTO 字段注入模板变量占位符，生成最终 Prompt 文本
- 模板版本管理：同一能力+科室可存在多个版本，同一时刻仅一个版本为启用状态
- 支持运行时热加载：管理员在管理端修改模板后通过 Spring ApplicationEvent 通知缓存失效

**协作对象**：
- 被 `CapabilityExecutor` 在模板渲染步骤中调用
- 从 `PromptTemplateRepository` 加载模板（数据库持久化）

#### `PromptTemplate` — Prompt 模板值对象 / JPA Entity（class，归属 `ai-impl/template/`）

**状态模型**：
```
DRAFT → ACTIVE → DEPRECATED
- DRAFT: 草稿状态，仅管理端可见，不参与运行时渲染
- ACTIVE: 启用状态，运行时以此版本渲染。同一 capabilityId + departmentId 组合同时仅一个 ACTIVE
- DEPRECATED: 废弃状态，历史数据保留但不再用于渲染
状态转换: DRAFT → ACTIVE（管理端发布）；ACTIVE → DEPRECATED（新版本发布后旧版本自动废弃）
```

### 3.4 A/B 实验管理层

#### `ExperimentManager` — A/B 实验管理契约（interface，归属 `ai-impl/experiment/`）

**职责**：
- 判断当前请求是否命中某个 A/B 实验
- 若命中，返回实验分组信息（`ExperimentAssignment`），包括应使用的模型标识或 Prompt 版本
- 支持按能力标识维度配置实验，按用户标识或会话标识做确定性分桶

#### `Experiment` — 实验配置值对象 / JPA Entity（class，归属 `ai-impl/experiment/`）

**状态模型**：
```
DRAFT → ACTIVE → PAUSED → COMPLETED
- DRAFT: 编辑中，不分流
- ACTIVE: 运行中，参与流量分配
- PAUSED: 暂停，不再分流新流量；已分配的会话继续按实验分组执行
- COMPLETED: 结束，所有流量回到默认模型
状态转换: DRAFT → ACTIVE（管理端启动）；ACTIVE → PAUSED（管理端暂停）；ACTIVE/PAUSED → COMPLETED（到达 endTime 或管理端手动结束）
```

#### `ExperimentAssignment` — 实验分组结果值对象（class，归属 `ai-impl/experiment/`）

**职责**：封装一次实验分流的结果。未命中任何实验时返回空值对象（分组标识为 "default"）。

### 3.5 性能观测层

#### `AiMetricsCollector` — AI 性能指标采集契约（interface，归属 `ai-impl/metrics/`）

**职责**：
- 接收每次 AI 能力调用的完整记录（`AiCallRecord`），执行异步持久化写入 `AI 调用日志`
- 同时将关键指标推送到 Micrometer 指标体系
- Phase 5 默认实现为日志输出 + 数据库持久化

**异步队列溢出策略**（v2 新增）：
- `AiMetricsCollector` 使用 Spring `@Async` + 自定义线程池
- 线程池配置：核心线程 2，最大线程 4，队列容量 1000
- **拒绝策略**：`CallerRunsPolicy`（调用者线程执行）替代 Spring 默认的 `AbortPolicy`，避免 `TaskRejectedException` 异常传播到调用链导致主流程失败。如果调用者线程也执行异步写，调用链延迟增加但不会中断
- 未来 Phase 6 可替换为独立的消息队列（Kafka / RabbitMQ）

#### `AiCallRecord` — AI 调用记录值对象（class，归属 `ai-impl/metrics/`）

**职责**：封装一次 AI 能力调用的全部可观测性字段，与 `AiCallLogEntity` 字段对等。作为 `AiMetricsCollector` 的输入参数与持久化数据源。

**字段定义**（与 `AiCallLogEntity` 对等，不包含 JPA 主键）：

| 字段 | 类型 | 说明 |
|------|------|------|
| callTime | LocalDateTime | 调用时间 |
| capabilityId | String | 能力标识 |
| capabilityName | String | 能力名称 |
| visitId | String | 关联就诊标识（可空） |
| patientId | String | 患者标识（可空） |
| callerRole | String | 调用方角色 |
| callerId | String | 调用方标识 |
| inputSummary | String | 输入摘要 |
| outputSummary | String | 输出摘要 |
| degraded | boolean | 是否降级 |
| degradationReason | String | 降级原因（可空） |
| elapsedMs | long | 耗时毫秒 |
| errorCode | String | 错误码（可空） |
| errorMessage | String | 错误消息（可空） |
| modelId | String | 模型标识 |
| retryCount | int | 重试次数 |
| sessionId | String | 会话标识（可空） |
| promptTokens | Integer | Prompt Token 用量 |
| completionTokens | Integer | Completion Token 用量 |
| totalTokens | Integer | 总 Token 用量 |

**字段填充策略**：
- `capabilityId`、`modelId`、`elapsedMs`、`degraded`：由管线执行过程中直接获取
- `callerRole`、`callerId`：从 Spring `SecurityContext` / `RequestContext` 中提取当前操作用户
- `inputSummary`：取业务请求 DTO 的 `toString()` 截断（前 500 字符）
- `outputSummary`：取 LLM 响应的结构化输出后摘要截断
- `visitId`、`patientId`、`sessionId`：从业务请求 DTO 中提取（各能力 DTO 均继承 `AiRequestBase` 基类携带这些字段）
- `errorCode`、`errorMessage`：LLM 调用失败或解析失败时记录，成功时为空
- `tokenUsage` 系列：从 `LlmResponse` 中提取
- `retryCount`：由 `LlmClient` 内部重试计数器提供

#### `AiRequestBase` — AI 能力请求基类（abstract class，归属 `ai-api/dto/base/`）

**职责**：所有 AI 能力业务请求 DTO 的基类，封装跨能力通用的上下文字段。各能力的具体请求 DTO（如 `TriageRequest`、`PrescriptionCheckRequest` 等）均继承此类。

**公共字段**：
| 字段 | 类型 | 说明 |
|------|------|------|
| visitId | String | 关联就诊标识（可空） |
| patientId | String | 患者标识（可空） |
| sessionId | String | 会话标识（可空） |

**类型定位**：使用抽象类而非接口，因为字段需要被具体子类直接继承且允许增加公共构造/工厂方法。归属 `ai-api/dto/base/` 子包。

**现有 DTO 影响评估与向后兼容策略**（v4 新增）：
- **现状**：当前代码库中 13 项 AI 能力的请求 DTO（`TriageRequest`、`DiagnosisRequest` 等）各自独立定义 `visitId`/`patientId`/`sessionId` 字段，未继承任何公共基类
- **迁移代价**：引入 `AiRequestBase` 需要完成以下操作：
  1. 在 `ai-api/dto/base/` 包中创建 `AiRequestBase` 抽象类，声明 `visitId`/`patientId`/`sessionId` 三个 protected 字段
  2. 将现有 13 个请求 DTO 的公共字段声明改为 `extends AiRequestBase`，删除重复字段
  3. 检查各 DTO 是否存在字段名/类型不兼容（如某 DTO 的 `visitId` 类型为 `Long` 而非 `String`），若发现则统一为 `String`
  4. 确保 Jackson 序列化/反序列化兼容（`@JsonIgnoreProperties(ignoreUnknown = true)` 在基类级别有效）
- **过渡策略**：采用分步迁移而非一次性全量修改：
  - Phase 5 底座切流时仅要求 7 项底座能力的 DTO 完成基类继承改造
  - 其余 6 项 Phase 4 能力的 DTO 暂维持现状，通过 `AiCallRecord` 字段填充逻辑中的独立提取方法获取公共字段，待后续统一改造
  - 基类引入后不影响现有业务方接口（新增父类对接口契约无变更），序列化兼容性通过 Jackson 多态注解保障
- **风险缓解**：引入基类前在单元测试层添加反序列化兼容性测试，验证新旧 DTO 形态下的 JSON 互读

#### `AiCallLogEntity` — AI 调用日志 JPA 实体（新增 `@Entity`，归属 `ai-impl/metrics/`）

**职责**：与 `AiCallRecord` 字段一一对应的 JPA Entity，映射到 `ai_call_log` 表，为 AI 调用日志提供持久化契约。

**字段定义**（与 `AiCallRecord` 对等，含 JPA 主键）：
| 字段名 | Java 类型 | 数据库类型 | 说明 |
|--------|----------|-----------|------|
| id | Long | BIGINT (PK, AUTO) | 主键 |
| call_time | LocalDateTime | DATETIME(3) | 调用时间（毫秒精度，`@Column(columnDefinition = "DATETIME(3)")`） |
| capability_id | String | VARCHAR(50) | 能力标识 |
| capability_name | String | VARCHAR(100) | 能力名称 |
| visit_id | String | VARCHAR(50) | 关联就诊标识（可空） |
| patient_id | String | VARCHAR(50) | 患者标识（可空） |
| caller_role | String | VARCHAR(20) | 调用方角色 |
| caller_id | String | VARCHAR(50) | 调用方标识 |
| input_summary | String | VARCHAR(500) | 输入摘要 |
| output_summary | String | VARCHAR(500) | 输出摘要 |
| degraded | boolean | TINYINT(1) | 是否降级 |
| degradation_reason | String | VARCHAR(200) | 降级原因（可空） |
| elapsed_ms | long | BIGINT | 耗时毫秒 |
| error_code | String | VARCHAR(20) | 错误码（可空） |
| error_message | String | VARCHAR(500) | 错误消息（可空） |
| model_id | String | VARCHAR(50) | 模型标识 |
| retry_count | int | INT | 重试次数 |
| session_id | String | VARCHAR(50) | 会话标识（可空） |
| prompt_tokens | Integer | INT | Prompt Token 用量（可空） |
| completion_tokens | Integer | INT | Completion Token 用量（可空） |
| total_tokens | Integer | INT | 总 Token 用量（可空） |

**表索引策略**：
- `idx_call_time` — `(call_time DESC)`：按时间降序查询近期调用记录
- `idx_capability_call_time` — `(capability_id, call_time DESC)`：按能力标识 + 时间查询
- `idx_degraded_call_time` — `(degraded, call_time DESC)`：降级记录维度查询
- `idx_model_call_time` — `(model_id, call_time DESC)`：模型维度查询与统计
- `idx_caller_role_call_time` — `(caller_role, call_time DESC)`：角色维度查询与统计
- `idx_visit_id` — `(visit_id)`：按就诊标识查询关联的 AI 调用
- `idx_patient_id` — `(patient_id)`：按患者标识查询

**为何定义为独立 Entity 而非共用 `AiCallRecord`**：`AiCallRecord` 是值对象，作为内存中的数据传输载体。JPA Entity 需要携带 JPA 注解和索引声明，与纯值对象的职责分离，避免污染 `ai-api` 模块的依赖。

#### `SlidingWindowMetricsStore` — 调用指标滑动窗口存储（class，归属 `ai-impl/metrics/`，v2 新增）

**职责**：为每个能力标识维护独立的调用指标滑动窗口（时间窗口可配置，默认 60 秒），供 `DegradationStrategy` 实现读取实时调用数据。

**核心方法**：
```
recordSuccess(capabilityId, elapsedMs)
    记录一次成功调用（耗时毫秒）

recordFailure(capabilityId)
    记录一次失败调用

getFailureRate(capabilityId) → double
    返回当前窗口内的失败率

getAverageElapsed(capabilityId) → double
    返回当前窗口内的平均耗时

buildDegradationContext(capabilityId, requestType) → DegradationContext
    从窗口数据构建完整的 DegradationContext 实例
```

**线程安全**：内部使用 `ConcurrentHashMap<String, Deque<WindowedEvent>>`（滑动窗口事件队列），事件添加使用 `synchronized` 保护队列尾写入，统计读取通过快照方式（先复制当前窗口的快照数组再计算，读写分离）。

**为何新增此类**：v1 设计中 `DegradationStrategy` 无法获取实时调用数据，导致新增策略（超时/熔断）成为死代码。`SlidingWindowMetricsStore` 作为数据中枢，使所有策略可基于统一的实时指标窗口做出判定。

### 3.6 结构化输出解析层

#### `StructuredOutputParser` — 结构化输出解析契约（interface，归属 `ai-impl/parser/`）

**职责**：定义从 LLM 原始文本输出中解析出 Java DTO 的统一协议。

**协作对象**：
- 被 `CapabilityExecutor` 在结果解析步骤中调用
- `JsonStructuredOutputParser`（默认实现）：假设 LLM 输出为 JSON 格式，基于 Jackson 反序列化

### 3.7 本地规则降级层

#### `LocalRuleFallback` — 本地规则降级契约（interface，归属 `ai-impl/fallback/`）

**职责**：定义 AI 能力降级到本地规则校验时的执行入口。仅特定能力需要实现此接口（当前仅为 3.4.2 AI 处方审核）。

**协作对象**：
- 被 `CapabilityExecutor` 在降级兜底步骤中调用
- `PrescriptionLocalRuleFallback`（实现）：执行 3.4.2 规定的本地规则校验最小检查项

### 3.8 降级策略扩展

#### `SlidingWindowMetricsStore` 与降级策略的协作模式（v2 新增）

v1 设计中，`FallbackAiService.applyStrategies()` 创建空值 `DegradationContext`，导致策略实现无法获取调用耗时窗口数据或失败率数据，形成死代码。

v2 修正：降级判定移入编排层管线内部，`FallbackAiService` 剥离策略调用职责。流程如下：

```
CapabilityExecutor.execute(request):
  1. 调用前: SlidingWindowMetricsStore.buildDegradationContext(capabilityId, requestType) → context
  2. 遍历 DegradationStrategy 链（按 `default` 方法 `getOrder()` 升序）:
     - 任一 shouldDegrade(context) 返回 true → 跳过 LLM 调用，直接降级
     - 全部返回 false → 继续 LLM 调用
  3. LLM 调用后: 记录结果到 SlidingWindowMetricsStore.recordSuccess/Failure()
```

此模式下：
- `DegradationContext` 由 `SlidingWindowMetricsStore` 根据实时窗口数据填充，策略实现获取到真实的 `invocationCount`、`failureCount`、`elapsedTime`、`lastFailureTime`
- `FallbackAiService.applyStrategies()` 被移除，`FallbackAiService` 不再持有或管理 `DegradationStrategy` 列表，其职责收敛为：`AiService` 降级装饰（调用失败时返回 `AiResult.degraded()`） + `@Primary` Bean 装配协调。降级判定统一在 `CapabilityExecutor` 管线内完成

#### `TimeoutDegradationStrategy` — 超时降级策略（class，归属 `ai-impl/degradation/`）

**职责**：基于 `DegradationContext` 中的最近调用耗时信息判定是否触发降级。若某能力的最近 N 次（可配置）调用平均耗时超过其硬超时阈值的 80%，触发降级。

**协作对象**：
- 实现 `DegradationStrategy`
- 读取 `DegradationContext` 中的 `elapsedTime`、`requestType`、`invocationCount`、`lastFailureTime`
- 数据的实时性由 `SlidingWindowMetricsStore` 确保

#### `CircuitBreakerDegradationStrategy` — 熔断降级策略（class，归属 `ai-impl/degradation/`）

**职责**：当某能力的最近调用失败率超过阈值（可配置，默认 50%）时，触发熔断——在熔断窗口（默认 30 秒）内所有对该能力的调用直接走降级路径。

**协作对象**：
- 实现 `DegradationStrategy`
- 读取 `DegradationContext` 中的 `failureCount` 和 `invocationCount`
- 内部维护每个能力标识的熔断状态与失败计数滑动窗口

**状态模型**（v2 明确）：
```
CLOSED ←→ OPEN ←→ HALF_OPEN
  - CLOSED: 正常通过，记录成功/失败
  - OPEN: 所有请求直接降级，不尝试 LLM 调用
  - HALF_OPEN: 允许一次探测请求通过
    - 探测成功 → 回到 CLOSED
    - 探测失败 → 回到 OPEN
  触发条件:
  - CLOSED → OPEN: getFailureRate(capabilityId) ≥ 阈值（默认 50%）
  - OPEN → HALF_OPEN: 熔断窗口到期（默认 30 秒），无需人工干预
```

#### `DegradationContext` — 降级判定上下文（class implements `Serializable`，归属 `ai-api/degradation/`，扩展）

**职责**：Phase 0 定义为零值构造器骨架。Phase 5 扩展以下字段（保持向后二进制兼容）。声明 `implements Serializable` 以确保在分布式缓存、事件序列化等场景下的二进制兼容性。

**扩展字段**（v2）：
| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| invocationCount | int | 0 | 该能力近窗口内调用次数 |
| lastFailureTime | long | 0L | 最近一次失败时间戳 epoch ms |
| elapsedTime | long | 0L | 最近一次调用耗时 ms |
| requestType | String | null | 能力标识，如 "TRIAGE"/"RX_AUDIT" |
| failureCount | int | 0 | 该能力近窗口内失败次数 |

**二进制兼容性分析**（v2 新增）：
- **serialVersionUID**：声明 `private static final long serialVersionUID = 1L`（或显式声明与 Phase 0 生成值一致），确保序列化兼容
- **无参构造器保留**：新增字段取语言默认值（0 / 0L / null），旧代码反序列化后不会因缺少字段而抛出异常
- **新增字段默认值静默下界问题**：若某字段（如 `failureCount`）在旧序列化数据反序列后默认为 0，可能导致 `shouldDegrade()` 统计失准（熔断器认为失败率为 0%）。**缓解措施**：（1）`builder()` 工厂方法生成实例，确保字段显式赋值；（2）新增字段加入 `equals()` / `hashCode()` 不影响现有比较逻辑；（3）降级策略实现中以字段值 > 0 判据，不依赖绝对值
- **策略自动注册抑制**（v4 修订）：新增的 `TimeoutDegradationStrategy` 和 `CircuitBreakerDegradationStrategy` 作为 `@Component` 不会自动注入 `FallbackAiService` 的策略列表（降级判定已移至 `CapabilityExecutor` 管线内部），而是由 `AiPlatformConfig` 按能力配置的策略白名单驱动，通过 Spring `@Qualifier("capabilityId")` 注入到各 `CapabilityExecutor` 实现。各能力的策略列表在 `AiPlatformConfig` 中按能力标识配置：

```yaml
ai:
  degradation:
    strategies:
      TRIAGE: [timeout, circuit-breaker]
      RX_AUDIT: [timeout, noop]
      # 未配置的能力默认使用 [noop]
```

---

## 4. 关键行为契约

### 4.1 AI 能力统合调用管线

```
AiOrchestrator.handle(capabilityId, request):
  1. executor = executorMap.get(capabilityId)    // 从 Map<String, CapabilityExecutor> 查找
  2. if executor == null: throw IllegalStateException("未注册能力标识: " + capabilityId)  // 启动期 fail-fast
  3. result = executor.execute(request, capabilityId)
  4. return result

CapabilityExecutor.execute(request, capabilityId):
  startTime = System.currentTimeMillis()        // 计时起点，降级路径也使用此值计算耗时
  degradeReason = null

  // 降级预检（遍历注入的策略链，策略列表通过 @Qualifier("capabilityId") 按能力配置注入）
  context = slidingWindowMetricsStore.buildDegradationContext(capabilityId, this.getClass().getSimpleName())
  for each strategy in this.degradationStrategies (sorted by default getOrder() asc):
    if strategy.shouldDegrade(context):
      degradeReason = strategy.getClass().getSimpleName()  // 使用第一优先命中策略名称作为降级原因
      goto degrade

  // 正常管线
  variables = extractVariables(request)          // 变量提取：方式 A（ObjectMapper.convertValue）或方式 B（自定义 extractVariables）
  renderedPrompt = promptTemplateManager.render(capabilityId, request.getDepartmentId(), variables)
  assignment = experimentManager.assign(capabilityId, userId, sessionId)
  modelRoute = modelRouter.route(capabilityId, assignment)
  if modelRoute == null:                         // 无可用路由 → 直接降级，避免 LlmRequest 构造时 NPE
    degradeReason = "NoAvailableRoute"
    goto degrade

  // 健康检查（模型路由之后、LLM 调用之前）
  endpointState = endpointHealthManager.getState(modelRoute.getEndpointId())
  if endpointState == UNAVAILABLE:
    if not endpointHealthManager.tryProbe(modelRoute.getEndpointId()):
      degradeReason = "EndpointUnavailable"
      goto degrade
    // tryProbe 成功：允许探测调用（缩短超时）

  llmResponse = llmClient.invoke(LlmRequest(renderedPrompt, modelRoute.getModelId(), modelRoute.getParameters()))
  parsedResult = structuredOutputParser.parse(llmResponse, outputType)
  elapsedMs = System.currentTimeMillis() - startTime
  metricsCollector.record(AiCallRecord.success(capabilityId, elapsedMs, ...))
  slidingWindowMetricsStore.recordSuccess(capabilityId, elapsedMs)
  return AiResult.success(parsedResult)

  // 降级路径
  degrade:
  elapsedMs = System.currentTimeMillis() - startTime   // 降级路径也记录耗时
  if localRuleFallback != null:
    result = localRuleFallback.fallback(request)
    metricsCollector.record(AiCallRecord.degraded(capabilityId, elapsedMs, degradeReason, ...))
    slidingWindowMetricsStore.recordSuccess(capabilityId, elapsedMs)  // 降级到本地规则成功，标记 degraded=true
    return result
  else:
    metricsCollector.record(AiCallRecord.degraded(capabilityId, elapsedMs, degradeReason, ...))
    slidingWindowMetricsStore.recordFailure(capabilityId)
    return AiResult.degraded(degradeReason)
```

**LLM 调用层重试逻辑**：
- `llmClient.invoke()` 内部对可重试异常（HTTP 5xx、连接超时、网络抖动）自动重试 1 次
- 幂等性由各能力的 DTO 输入保证（查询类天然幂等，指令类上游去重）
- 重试后仍然失败 → 返回 `LlmResponse` 带异常标记 → `CapabilityExecutor` 捕获后进入降级路径

### 4.2 模型路由契约

```
ModelRouter.route(capabilityId, assignment):
  1. 若 assignment 指定了覆盖模型 → 返回该模型对应的 ModelRoute
  2. 否则从路由映射表中查找 capabilityId 对应的默认 ModelRoute
  3. 若存在多个 ModelRoute（多模型负载均衡）：按权重随机选择
  4. 无可用路由 → 返回 null → 触发降级
```

### 4.3 A/B 实验分流契约

```
ExperimentManager.assign(capabilityId, userId, sessionId):
  1. 检索该能力标识下当前状态为 ACTIVE 的实验列表
  2. 无 ACTIVE 实验 → 返回空 assignment（分组=default）
  3. 有实验 → 按 userId/sessionId 哈希值 % 1000 映射到 [0, 1000) 区间
  4. 遍历分组列表，找到哈希值落在其流量百分比区间的分组
  5. 返回 ExperimentAssignment(实验标识, 分组标识, 目标模型, 目标Prompt版本)
```

### 4.4 Prompt 模板渲染契约

```
PromptTemplateManager.render(capabilityId, departmentId, variables):
  1. 按 capabilityId + departmentId 查询 ACTIVE 模板
  2. departmentId 有值且存在科室级模板 → 优先使用科室级模板
  3. departmentId 无值或无科室级模板 → 使用通用模板
  4. 将 variables 中的键值对替换模板中的 {{key}} 占位符
  5. 缺失变量保留原始占位符文本 + 日志 WARN
  6. 返回渲染后 Prompt 文本
```

### 4.5 结构化输出解析契约

```
StructuredOutputParser.parse(llmResponse, targetClass):
  1. 从 llmResponse.text() 提取 JSON 片段
  2. 若 LLM 输出为 Markdown 代码块包裹的 JSON → 提取代码块内容
  3. 若 LLM 输出为纯 JSON → 直接使用
  4. Jackson 反序列化到 targetClass
  5. 反序列化失败 → 抛出 ParseException → 由 CapabilityExecutor 捕获并触发降级
```

### 4.6 性能指标采集契约

```
AiMetricsCollector.record(AiCallRecord):
  1. @Async 异步写入 AiCallLogRepository（JPA INSERT）
  2. 同步推送 Micrometer 指标:
     - aimedical.ai.request.duration (Timer, tags: ability, status, model)
     - aimedical.ai.request.count (Counter, tags: ability, status)
     - aimedical.ai.degradation.count (Counter, tags: ability, reason)
     - aimedical.ai.token.usage (DistributionSummary, tags: ability, model, type)
```

---

## 5. 错误处理策略

### 5.1 错误分类（AI 底座层面新增）

| 错误类别 | 代表场景 | 处理方式 | 响应形态 |
|---------|---------|---------|---------|
| 模板缺失/渲染失败 | 模板不存在、变量缺失致渲染异常 | 日志 WARN + 使用能力内硬编码兜底 Prompt | 继续调用 LLM |
| 实验分流异常 | 实验配置非法 | 日志 WARN + 降级到 default 分组 | 继续调用 LLM |
| 模型路由失败 | 无可用模型路由 | 直接触发降级 | `AiResult.degraded()` |
| LLM 调用超时 | 超过能力硬超时阈值 | 重试 1 次→仍失败则降级 | `AiResult.degraded()` |
| LLM 调用不可用 | HTTP 5xx / 连接拒绝 | 重试 1 次→仍失败则降级 | `AiResult.degraded()` |
| 结构化输出解析失败 | LLM 返回非 JSON | 提取 JSON 片段重试→仍失败降级 | `AiResult.degraded()` |
| 熔断触发 | 失败率超阈值 | 跳过 LLM 调用，直接降级 | `AiResult.degraded()` |

### 5.2 降级优先级

1. **熔断** > **超时** > **LLM 不可用** > **解析失败**
2. 有 `LocalRuleFallback` 的能力降级时返回本地规则结果；其余返回 `AiResult.degraded()`

---

## 6. 并发设计

### 6.1 线程模型（v2 修订）

- `AiOrchestrator`：**非无状态**。线程安全性依赖于：
  - `Map<String, CapabilityExecutor>` 初始化后不变（无竞争读）
  - `SlidingWindowMetricsStore` 内部使用 `ConcurrentHashMap` + 队列写锁保证并发安全
  - 核心编排逻辑（按 capabilityId 查找 executor → 调用）无共享可变状态，线程安全
- `AiOrchestrator` 本身不引入 `synchronized` 大锁；并发瓶颈由各子组件独立承担
- `LlmClient`：无状态，线程安全。HTTP 客户端基于连接池
- `ModelRouter`：路由表启动时从配置/DB 加载到 `ConcurrentHashMap`，支持运行时刷新
- `PromptTemplateManager`：模板缓存使用 `ConcurrentHashMap`，管理端变更通过 Spring ApplicationEvent 通知缓存失效
- `ExperimentManager`：实验配置缓存同理
- `AiMetricsCollector`：异步写入，使用 Spring `@Async` + 自定义线程池（`CallerRunsPolicy`拒绝策略）

### 6.2 熔断器线程安全（v2 保留并强化）

- `CircuitBreakerDegradationStrategy` 内部每个能力标识维护独立的熔断状态实例
- 状态转换使用 `AtomicReference<CircuitState>` + CAS 保证原子性
- 滑动窗口使用 `ConcurrentHashMap<String, Deque<Long>>`
- 状态读取（`shouldDegrade()`）与状态转换（`recordSuccess/Failure()`）之间不设全局锁，转换失败（CAS 冲突）时重试

### 6.3 数据库写入竞争

- `AiCallLogRepository` 的写入为追加操作（INSERT），无竞争
- `PromptTemplateRepository` 和 `ExperimentRepository` 读写分离：读走缓存，写走管理端 API + 事件通知缓存失效

---

## 7. 设计决策

| 决策 | 选项 | 选择 | 理由（v2 增补） |
|------|------|------|------|
| 编排层形态 | 全新 AiService 实现 vs 在 FallbackAiService 内扩展 | 全新 `AiOrchestrator` 实现 `AiService`，替代 `MockAiService` | FallbackAiService 职责是降级装饰，不应混入编排逻辑 |
| 管线所有权（v4 新增） | AiOrchestrator 持有公共管线 vs CapabilityExecutor 持有完整管线 | `CapabilityExecutor` 持有完整管线 | 13 项能力的管线步骤高度一致但变量提取、策略白名单存在差异；CapabilityExecutor 持有管线使每项能力可独立定制局部步骤而不影响其他能力，同时保持管线骨架统一 |
| 降级策略注入目标（v4 新增） | 注入到 AiOrchestrator 全局 vs 注入到 CapabilityExecutor 按能力隔离 | 按能力注入到 `CapabilityExecutor` | 各能力的降级策略白名单不同（如 TRIAGE 使用 timeout+熔断，RX_AUDIT 使用 timeout+noop），全局注入导致策略对所有能力无条件生效；按能力注入使策略配置与能力绑定，且各实现不共享策略列表实例 |
| 能力映射机制（v4 新增） | 手动 Map 注册 vs @PostConstruct 扫描 vs 注解扫描 | `@PostConstruct` 扫描 `List<CapabilityExecutor>` 自动构建 `Map<String, CapabilityExecutor>` | 零配置——新增能力实现后自动注册，无需修改 AiOrchestrator 代码；@PostConstruct 时机构造期 fail-fast，未注册能力标识在启动时即暴露而非运行时静默失败 |
| 配置转发机制（v4 新增） | EnvironmentPostProcessor vs @ConditionalOnExpression vs 自定义 PropertySource | `EnvironmentPostProcessor` | Spring 启动早期执行，在 @ConditionalOnProperty 评估之前完成属性转发；单一职责、配置与代码分离 |
| 变量提取方式（v4 新增） | ObjectMapper.convertValue vs 自定义方法 vs 注解驱动 | 双模式：默认 `ObjectMapper.convertValue` + 可选的 `extractVariables()` 方法重写 | ObjectMapper 无需额外注解或配置即可工作；自定义方法为复杂场景提供扩展点；两种模式通过在 CapabilityExecutor 中重写方法切换，无需修改框架代码 |
| LLM 客户端抽象 | 直接使用 Spring AI vs 自定义抽象层 | 自定义 `LlmClient` interface，Spring AI 作为可选实现 | 支持 HTTP API 调用和 Spring AI 两种方式共存 |
| Prompt 模板存储 | 配置文件 vs 数据库 | 数据库（JPA Entity）+ 内存缓存 | 管理端运行时修改需求要求；缓存解决查询性能 |
| A/B 实验分桶 | 哈希分桶 vs 多臂老虎机 | 哈希分桶（Phase 5） | 简单满足确定性分桶需求；高级策略留 Phase 6 |
| 指标采集方式 | 仅 Micrometer vs 仅数据库 vs 双写 | 双写：数据库 + Micrometer | 数据库满足可追溯性；Micrometer 满足实时指标需求 |
| 降级策略扩展 | 保留 NoOp + 新增超时/熔断 vs 全部替换 | 新增超时/熔断，按能力策略白名单配置 | v2：策略调用移入 CapabilityExecutor 管线，数据源由 `SlidingWindowMetricsStore` 提供，解决 v1 死代码问题；v4：按能力注入到各 CapabilityExecutor 实现 |
| 结构化输出方式 | 强制 JSON vs Spring AI vs 自定义 | 自定义 `StructuredOutputParser` interface | 可适配 JSON/Markdown/自由文本多种输出格式 |
| 能力执行器粒度 | 每能力一个 vs 全能力同管 | 每能力一个 `CapabilityExecutor<T,R>` 泛型实现 | 13 项能力输入/输出类型各不相同；泛型保证类型安全 |
| DegradationContext 扩展方式 | 新增字段 vs 新增子类 | 新增字段 + Builder 模式 | Phase 0 冻结方法签名；Builder 确保新字段显式赋值；声明 serialVersionUID 保序列化兼容 |
| Bean 装配策略（v2 新增） | `@ConditionalOnProperty` 互斥 vs 其他 | `FallbackAiService` 使用 `@Primary` + `ObjectProvider<AiService>` | 解决 v1 中的 NoUniqueBeanDefinitionException；互斥策略通过 `ai.platform.enabled` 配置实现 |
| 降级策略数据源（v2 新增） | FallbackAiService 创建空 DegradationContext vs SlidingWindowMetricsStore | `SlidingWindowMetricsStore` 提供实时指标数据 | v1 中策略实现无法获取调用数据形成死代码；滑动窗口存储使超时/熔断可基于真实数据判定 |
| 异步队列溢出（v2 新增） | AbortPolicy vs CallerRunsPolicy | `CallerRunsPolicy` | 避免 `TaskRejectedException` 传播到调用链导致主流程失败 |
| Micrometer 依赖（v2 新增） | 依赖 Spring Boot Actuator 自动配置 vs 显式声明 | 在 `ai-impl/pom.xml` 中显式声明 `spring-boot-starter-actuator` | 确保不可用依赖的底座功能不会因缺失 Actuator 而静默失败 |

---

## 8. Micrometer 依赖声明（v2 新增）

`ai-impl/pom.xml` 必须显式声明以下依赖（不依赖 Spring Boot 自动配置的传递性），确保 `AiMetricsCollector` 的 Micrometer 推送功能在任何部署环境下均可用：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

`application.yml` 中暴露指标端点（可被 Prometheus 抓取）：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    tags:
      application: aimedical-ai
```

---

## 9. 迁移路径

### 9.1 从 Phase 2~4 独立接入迁移至底座

| 迁移项 | Phase 2~4 现状 | Phase 5 目标 | 迁移策略 |
|--------|---------------|-------------|---------|
| 3.4.1 智能分诊 | 独立 HTTP 调用或 Spring AI 实现 | 迁移至 `AiOrchestrator` 管线 | 新增 `TriageCapabilityExecutor` |
| 3.4.2 AI 处方审核 | 独立实现 + 本地规则降级 | 迁移至 `AiOrchestrator` 管线 | 新增 `PrescriptionCheckCapabilityExecutor` |
| 3.4.3 AI 病历生成 | 独立实现 | 迁移至 `AiOrchestrator` 管线 | 新增 `MedicalRecordGenCapabilityExecutor` |
| 3.4.10 AI 辅助开方 | 独立实现 | 迁移至 `AiOrchestrator` 管线 | 新增 `PrescriptionAssistCapabilityExecutor` |
| 3.4.8 AI 知识库问答 | Phase 2 Mock | 首次真实实现于底座 | 新增 `KbQueryCapabilityExecutor` |
| 3.4.12 AI 医生排班 | Phase 5 首次落地 | 首次真实实现于底座 | 新增 `ScheduleCapabilityExecutor` |
| 3.4.13 AI 综合讨论结论 | Phase 5 首次落地 | 首次真实实现于底座 | 新增 `DiscussionConclusionCapabilityExecutor` |
| 3.4.4 AI 智能诊断 | Phase 4 独立接入 | 不迁移至底座，保留 Phase 4 实现 | 新增 `DiagnosisCapabilityExecutor`（薄适配器，委托至 Phase 4 业务服务） |
| 3.4.5 AI 智能检查报告 | Phase 4 独立接入 | 不迁移至底座，保留 Phase 4 实现 | 新增 `AnalysisReportForInspectionCapabilityExecutor`（薄适配器） |
| 3.4.6 AI 智能检验报告 | Phase 4 独立接入 | 不迁移至底座，保留 Phase 4 实现 | 新增 `AnalysisReportForLabTestCapabilityExecutor`（薄适配器） |
| 3.4.7 AI 影像分析 | Phase 4 独立接入 | 不迁移至底座，保留 Phase 4 实现 | 新增 `ImageAnalysisCapabilityExecutor`（薄适配器） |
| 3.4.9 AI 开立检查/检验 | Phase 4 独立接入 | 不迁移至底座，保留 Phase 4 实现 | 新增 `RecommendExaminationCapabilityExecutor`（薄适配器） |
| 3.4.11 AI 执行顺序推荐 | Phase 4 独立接入 | 不迁移至底座，保留 Phase 4 实现 | 新增 `RecommendExecutionOrderCapabilityExecutor`（薄适配器） |

### 9.2 配置切换

```yaml
ai:
  platform:
    enabled: true                     # true → AiOrchestrator 激活；false → 底座关闭
  mock:
    enabled: false                    # true → MockAiService 激活（开发/测试），与现有代码兼容
  router:
    routes:
      TRIAGE: { model: "qwen-plus", endpoint: "https://api.example.com/v1/chat/completions", client: "HTTP_API" }
      RX_AUDIT: { model: "qwen-turbo", endpoint: "https://api.example.com/v1/chat/completions", client: "HTTP_API" }
  degradation:
    strategies:
      TRIAGE: [timeout, circuit-breaker]
      RX_AUDIT: [timeout, noop]
    circuit-breaker:
      failure-rate-threshold: 0.5
      window-seconds: 60
      open-duration-seconds: 30
    timeout:
      threshold-percentage: 0.8
      sample-size: 10
  metrics:
    async:
      core-pool-size: 2
      max-pool-size: 4
      queue-capacity: 1000
      rejection-policy: CallerRuns
```

---

## 10. 与其他包的协作边界

### 10.1 包 G 与包 F（AI 分诊台规则）的协作

- 包 F 在管理端维护分诊规则（症状-科室映射），产出 `rule_version` / `rule_set_id`
- 包 G 的 `PromptTemplateManager` 在渲染分诊 Prompt 时，允许模板引用当前规则版本号变量

### 10.2 包 G 与包 E（AI 影像模型管理面）的协作

- 包 E 管理影像模型注册、岗位授权与调用审计
- 包 G 的 `ModelRouter` 路由表中，3.4.7 影像分析的 `ModelRoute` 由包 E 管理的模型注册数据驱动

### 10.3 包 G 与其他业务模块的协作

- 业务模块仍通过 `AiService` 接口调用 AI 能力，不感知底座内部变化
- `AiMetricsCollector` 写入的 AI 调用日志可被管理端统计分析
- `PromptTemplateManager` 的模板维护入口在管理端，归包 A（管理员端基础数据维护）

---

## 修订说明（v2）

| 审查意见 | 修改措施 |
|---------|---------|
| [CRITICAL] 缺失类图 — 需求明确要求"类图、核心职责、协作关系、关键接口、状态模型等OOD核心要素"，当前产出以文本表格+目录结构描述抽象，未提供任何 UML 类图 | 在 §2.3 新增完整的 Mermaid classDiagram，覆盖全部核心抽象，包含 interface/class 关系、继承箭头（`<|..`）、组合关联（`o-->` 带 cardinality）、依赖关系 |
| [MAJOR] 状态模型覆盖严重不足 — 仅提供了熔断器状态转换模型，其余关键抽象均未定义状态模型 | 在关联章节为以下抽象新增状态模型：`AiOrchestrator`（§3.1 线程安全模型）、`PromptTemplate`（§3.3 DRAFT→ACTIVE→DEPRECATED）、`Experiment`（§3.4 DRAFT→ACTIVE→PAUSED→COMPLETED）、`LlmClient`（§3.2 CONNECTED→DEGRADED→UNAVAILABLE）、`CircuitBreakerDegradationStrategy`（§3.8 CLOSED→OPEN→HALF_OPEN 细化）、`DegradationContext` 演变路径（§3.8 二进制兼容性分析） |
| [CRITICAL] CapabilityExecutor 接口缺少方法签名定义 — 核心接口未提供任何方法签名，缺少 execute() 入参类型、返回值类型、异常声明 | 在 §3.1 `CapabilityExecutor<T, R>` 中新增完整方法签名：`execute(T request, String capabilityId) → AiResult<R>`、`getCapabilityId()`、`getInputType()`、`getOutputType()`，明确泛型参数 T/R 的职责 |
| [CRITICAL] AiOrchestrator 与 FallbackAiService 的 Spring Bean 装配存在二义性 | §3.1 和 §7 修订 Bean 装配策略：`FallbackAiService` 使用 `@Primary` + `ObjectProvider<AiService>` 延迟解析被装饰实现；互斥条件改为 `ai.platform.enabled`（`AiOrchestrator` → true / `MockAiService` → false）；消除 NoUniqueBeanDefinitionException |
| [MAJOR] 新增 DegradationStrategy 实现与 FallbackAiService.applyStrategies() 不兼容 | §3.8 重构降级判定流程：新增 `SlidingWindowMetricsStore` 作为实时指标数据中枢；降级判定移入 `AiOrchestrator` 管线内部（§4.1），策略通过 `buildDegradationContext()` 获取真实数据；`FallbackAiService` 不再承担策略调用职责 |
| [MEDIUM] "AiOrchestrator 无状态"断言与设计事实不符 — 编排的组件具有显著可变状态 | §3.1 和 §6.1 修订：删除"无状态"断言，明确描述 `AiOrchestrator` 的线程安全模型——依赖各子组件的并发安全实现，`SlidingWindowMetricsStore` 使用 `ConcurrentHashMap` + 写锁，`Map<String, CapabilityExecutor>` 初始化后不变 |
| [MEDIUM] DegradationContext 字段扩展的二进制兼容性风险未评估 | §3.8 新增二进制兼容性分析段：声明 serialVersionUID；保留无参构造器；Builder 模式确保新字段显式赋值；分析新增字段默认值（0/0L/null）对降级判定的静默影响并给出缓解措施；配套策略白名单配置抑制自动注册 |
| [MEDIUM] AiCallLog JPA 实体未定义 — AiCallRecord 字段对应但 JPA Entity 未建模，数据库表结构无契约可依 | §3.5 新增 `AiCallLogEntity` JPA @Entity 定义，包含完整字段表、表名（`ai_call_log`）、4 条索引策略（`idx_call_time`、`idx_capability_call_time`、`idx_visit_id`、`idx_patient_id`）；明确与 `AiCallRecord` 值对象的职责分离理由 |
| [LOW] AiMetricsCollector 异步队列溢出策略未定义 — @Async 线程池未指定拒绝策略 | §3.5 和 §7 新增异步队列溢出策略：自定义线程池配置（core=2, max=4, queue=1000），拒绝策略使用 `CallerRunsPolicy` 替代默认 `AbortPolicy`，避免 `TaskRejectedException` 传播到调用链 |
| [LOW] Micrometer 依赖未确认 — 当前项目 POM 中未显式声明 micrometer-core 或 spring-boot-starter-actuator | §8 新增 Micrometer 依赖声明段：`ai-impl/pom.xml` 显式添加 `spring-boot-starter-actuator` 依赖，并配置 `management.endpoints.web.exposure.include` 端点暴露策略 |

## 修订说明（v2.2）

| 审查意见 | 修改措施 |
|---------|---------|
| [一般] AiCallRecord 类图字段集合（8 个）与 AiCallLogEntity 字段集合（19 个）不一致，设计文本声称"字段一一对应"但实际不符 | §3.5 扩展 `AiCallRecord` 字段列表至 20 个（不含 JPA 主键），与 `AiCallLogEntity` 字段一一对等；新增字段填充策略表，明确各字段的填充来源（SecurityContext / 业务请求基类 / LlmResponse / 重试计数器）；更新 §2.3 Mermaid 类图中 `AiCallRecord` 和 `AiCallLogEntity` 的字段定义使二者一致 |
| [轻微] DegradationContext 提及 serialVersionUID 暗示实现 Serializable，但设计中未显式声明 | §3.8 将 `DegradationContext` 的类型定位明确为 `class implements Serializable` |
| [轻微] CapabilityExecutor 典型实现注入依赖过多（可达 8 个），建议抽取可组合 Pipeline Step 链 | 经评估暂不采纳为设计约束。理由：各能力实现所需的组件依赖已是固定管线（模板/实验/路由/调用/解析/指标/降级/滑动窗口），抽取 Step 链会增加运行时的动态装配复杂度，对 Phase 5 初始冒烟阶段的 13 种能力而言是过度设计。留待 Phase 6（能力数量超过 20 或出现跨能力差异需求时）引入 Pipeline Step 抽象 |

## 修订说明（v3）

| 审查意见 | 修改措施 |
|---------|---------|
| [CRITICAL] §4.1 降级路径伪代码中指标采集语句位于 return 之后不可达 | §4.1 重构 degrade 路径：将 `metricsCollector.record()` 和 `slidingWindowMetricsStore.recordFailure()` 移至对应分支的 `return` 语句之前，消除死代码 |
| [CRITICAL] MockAiService 的 @ConditionalOnProperty 属性名与现有代码不一致 | §3.1 Bean 装配策略：MockAiService 使用 `@ConditionalOnProperty(name = "ai.mock.enabled")` 保持与现有代码兼容；AiPlatformConfig 新增内部转发从 `ai.platform.enabled` 映射到 `ai.mock.enabled`；§9.2 配置同步更新 |
| [CRITICAL] DegradationStrategy 接口新增 getOrder() 方法破坏现有实现 | §2.3 类图、§3.1、§3.8、§4.1：统一改为 Java 8 `default int getOrder() { return 0; }`，现有实现无需修改即可编译通过 |
| [MAJOR] 类图中 AiService 方法签名缺少 CompletableFuture 异步包装 | §2.3 类图：AiService、FallbackAiService、AiOrchestrator、CapabilityExecutor 的方法签名全部修正为 `CompletableFuture<AiResult<T>>`，所有方法名同步修正（`medicalRecordGen`→`generateMedicalRecord`、`kbQuery`→`knowledgeBaseQuery`）。§3.1 CapabilityExecutor 方法签名文本同步修正，新增异步桥接说明段 |
| [MEDIUM] 降级路径中 localRuleFallback 成功场景被错误记录为 recordFailure | §4.1 degrade 路径中 `localRuleFallback` 成功分支改用 `recordSuccess(degraded=true)`，仅无本地规则时的完全退化分支保留 `recordFailure` |
| [MEDIUM] AiRequestBase 基类在设计和代码库中均未定义 | §1.3 核心抽象表新增 AiRequestBase 行；§3.5 新增 AiRequestBase 定义（abstract class，含 visitId/patientId/sessionId 字段、包路径 `ai-api/dto/base/`、继承关系说明）；§2.3 类图补充该类型 |
| [MEDIUM] 类图中 AiService 方法名与现有接口不匹配 | §2.3 类图：`medicalRecordGen`→`generateMedicalRecord`，`kbQuery`→`knowledgeBaseQuery` |
| [MEDIUM] LlmClient 状态归属存在表述矛盾 | §3.2：LlmClient 职责段明确为"无状态，线程安全"，原状态模型移至新增 `ModelEndpointHealthManager` 组件（归属 `ai-impl/metrics/`），新增探测调用触发机制说明。§1.3 核心抽象表、§2.1 目录结构、§2.3 类图、§6.1 线程模型同步补充该新增组件 |
| [MEDIUM] AiCallLogEntity 遗漏字段级 JPA 映射索引覆盖度不足 | §3.5：`call_time` 数据库类型从 `DATETIME` 改为 `DATETIME(3)`，注明 `@Column(columnDefinition)`；新增 3 条覆盖索引 `idx_degraded_call_time`（降级+时间）、`idx_model_call_time`（模型+时间）、`idx_caller_role_call_time`（角色+时间） |

## 修订说明（v3.1）

| 审查意见 | 修改措施 |
|---------|---------|
| MockAiService @ConditionalOnProperty 属性名未按修订更新 — §3.1 仍写 `ai.platform.enabled` | §3.1 Bean 装配策略：MockAiService 改为 `@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "true", matchIfMissing = true)`；新增 `AiPlatformConfig` 从 `ai.platform.enabled` 到 `ai.mock.enabled` 的内部转发；同步修正 YAML 配置块 |
| CapabilityExecutor.execute() 方法签名文本与类图不一致 — §3.1 文本仍写同步 `AiResult<R>` 返回值 | §3.1 方法签名修正为 `CompletableFuture<AiResult<R>> execute(T request, String capabilityId)`，返回值说明同步更新为异步语义 |
| AiOrchestrator 线程安全模型自相矛盾 — §3.1 称使用 `synchronized`/`ReentrantLock`，§6.1 称不引入大锁 | §3.1 删除 `synchronized`/`ReentrantLock` 表述，统一采用"各子组件独立承担并发安全，编排层不引入大锁"的策略，与 §6.1 保持一致 |

## 修订说明（v4）

| 审查意见 | 修改措施 |
|---------|---------|
| [严重] 能力覆盖不足 — 仅 7/13 项 AI 能力有对应的 CapabilityExecutor 实现规划 | §1.1 明确 6 项 Phase 4 能力不迁移至底座，通过薄适配器集成；§2.1 目录结构新增 6 个 CapabilityExecutor 实现文件（Diagnosis / AnalysisReportForInspection / AnalysisReportForLabTest / ImageAnalysis / RecommendExamination / RecommendExecutionOrder）；§2.3 类图补充全部 13 个实现类的继承关系；§3.1 AiOrchestrator 新增"Phase 4 能力的处理策略"段落说明薄适配器委托模式；§9.1 迁移路径表补充 6 项能力的行（标注为"薄适配器，委托至 Phase 4 业务服务"） |
| [严重] 管线所有权矛盾 — §3.1 与 §4.1 对"谁拥有编排管线"的描述冲突 | 统一为 CapabilityExecutor 持有完整管线：§1.3 核心抽象表更新 AiOrchestrator 为"统一编排路由层"、CapabilityExecutor 为"完整执行管线"；§3.1 AiOrchestrator 重写为路由层（路由→委托→返回），管线 8 步流程移至 CapabilityExecutor 章节；§3.1 CapabilityExecutor 新增 8 步完整管线流程描述；§2.3 类图 AiOrchestrator 关联改为 `o--> "1..*" CapabilityExecutor : routes via Map` |
| [严重] DegradationStrategies 跨组件访问路径未定义 | §3.1 CapabilityExecutor 新增"降级策略注入机制"段落：每个 CapabilityExecutor 注入各自的 `List<DegradationStrategy>`，通过 Spring `@Qualifier("capabilityId")` 按能力标识注入，由 `AiPlatformConfig` 配置驱动；§2.3 类图新增 `CapabilityExecutor --> DegradationStrategy : uses (per-capability injection)`；§3.8 策略自动注册抑制段落修订为注入到 CapabilityExecutor |
| [重要] §4.1 降级路径中 elapsedMs 变量未定义 | §4.1 伪代码重构：在 execute() 入口添加 `startTime = System.currentTimeMillis()`，降级路径入口重新计算 `elapsedMs = System.currentTimeMillis() - startTime`，确保 degrade 路径下 elapsedMs 正确定义 |
| [重要] Null ModelRoute 导致 NPE — 降级路径未触发 | §4.1 伪代码在 modelRouter.route() 与 llmClient.invoke() 之间添加 `if modelRoute == null: degradeReason = "NoAvailableRoute"; goto degrade` 空值检查，避免 LlmRequest 构造时 NPE |
| [重要] CapabilityExecutor 方法到能力标识的映射机制未定义 | §3.1 AiOrchestrator 新增"能力标识到 CapabilityExecutor 的映射机制"段落：`@PostConstruct` 扫描 `List<CapabilityExecutor>` 按 `getCapabilityId()` 构建 `Map<String, CapabilityExecutor>`；启动期 fail-fast 检查；§7 设计决策表新增对应行 |
| [中等] AiRequestBase 基类引入未评估对现有 DTO 的影响 | §3.5 AiRequestBase 段落新增"现有 DTO 影响评估与向后兼容策略"：列出 4 项迁移操作（创建基类 / 修改 DTO 继承 / 兼容检查 / 序列化验证）；明确过渡策略——7 项底座能力 DTO 优先改造，6 项 Phase 4 能力 DTO 暂维持现状 |
| [中等] FallbackAiService.applyStrategies() 残留空值 DegradationContext 代码路径 | §3.8 明确 `applyStrategies()` 被移除，FallbackAiService 职责收敛为：降级装饰（调用失败时返回 `AiResult.degraded()`）+ `@Primary` Bean 装配协调；不再持有或管理 DegradationStrategy 列表 |
| [中等] Prompt 模板变量提取逻辑未定义 | §3.1 CapabilityExecutor 新增"变量提取约定"段落：方式 A（ObjectMapper.convertValue 默认）和方式 B（自定义 extractVariables() 重写），附选择规则 |
| [中等] ai.platform.enabled → ai.mock.enabled 配置转发机制未定义 | §3.1 Bean 装配策略修订：明确使用 `EnvironmentPostProcessor` 机制在 Spring 启动早期完成属性转发；§7 设计决策表新增对应行 |

## 修订说明（v5）

| 审查意见 | 修改措施 |
|---------|---------|
| ModelEndpointHealthManager 健康检查在管线中无执行路径 — §3.2 描述由 AiOrchestrator 执行，但 v4 已将管线执行全部移至 CapabilityExecutor，健康检查职责无归属方，导致该组件的状态管理和探测触发机制成为死代码 | §3.2 ModelEndpointHealthManager 职责描述：将"供 AiOrchestrator 在降级预检时判定"修改为"供 CapabilityExecutor 在管线执行中、模型调用之前判定"；§3.2 探测调用触发机制：将执行角色从 AiOrchestrator 改为 CapabilityExecutor，明确调用时机为"模型路由之后、LLM 调用之前"，补充 `tryProbe()` 返回 false 时的降级路径；§3.1 CapabilityExecutor 协作对象列表新增 `ModelEndpointHealthManager`；§4.1 伪代码：在模型路由空值检查之后、`llmClient.invoke()` 之前插入健康检查步骤——检查端点状态，UNAVAILABLE 时判断 `tryProbe()` 结果，失败则走降级路径 |
