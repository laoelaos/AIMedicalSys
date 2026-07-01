# Phase 5 包 G — AI 进阶底座 架构级 OOD 设计方案

## 1. 概述

### 1.1 设计目标

Phase 5 包 G 交付 AI 进阶底座（AI Advanced Platform），为平台全部 AI 能力提供统一的运行时基础设施。设计目标如下：

- **能力迁移统一化**：将 Phase 2~4 各阶段独立接入的 AI 能力（3.4.1/3.4.2/3.4.3/3.4.10 等）统一迁移至本底座，消除分散接入导致的重复代码和不一致的降级/超时/重试策略
- **模型对接标准化**：实现大模型统一对接层，支持多供应商模型路由与切换，业务层不感知具体模型实现
- **对话模板可配置化**：AI 对话模板（Prompt Template）按能力/科室维度可配置、可版本化管理，支持运行时热加载
- **A/B 实验可控化**：提供轻量级 A/B 实验框架，支持按能力维度分配流量到不同模型或 Prompt 版本，实验结果可观测
- **性能观测内建化**：为全部 AI 能力提供统合的调用指标采集、耗时分布、降级率统计与告警能力

### 1.2 整体架构思路

AI 进阶底座定位为 **ai-impl 子模块内部的分层架构**，在现有 `AiService` 接口不变的前提下，将原来 `MockAiService` 的扁平实现替换为多层管线：

```
业务模块 → AiService 接口（ai-api，不变）
              ↓
         FallbackAiService（装饰器，不变）
              ↓
         AiOrchestrator（新增：编排层）
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
| `AiOrchestrator` | class | 统一编排层，实现 `AiService` 接口的全部 13 个方法，内部分别委托给对应的能力执行管线；协调模板渲染、实验分流、模型调用、指标采集、降级判定五步流程 |
| `CapabilityExecutor` | interface | 单项 AI 能力的执行契约，定义"模板渲染 → 实验分流 → 模型调用 → 结果解析 → 降级兜底"的执行骨架 |
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
| `StructuredOutputParser` | interface | 结构化输出解析契约，将 LLM 原始文本输出解析为各能力对应的 Java DTO |
| `DegradationStrategy` | interface | 降级策略契约（Phase 0 已定义，本阶段扩展 `DegradationContext` 字段） |
| `DegradationContext` | class | 降级判定上下文（Phase 0 已定义骨架，本阶段扩展字段） |
| `LocalRuleFallback` | interface | 本地规则降级契约，为 3.4.2 处方审核等需要本地规则兜底的能力提供降级执行入口 |

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
│   │   └── DegradationContext.java          # 扩展字段（签名兼容）
│   └── dto/                                # 不变
│
├── ai-impl/ src/main/java/com/aimedical/modules/ai/impl/
│   ├── orchestrator/
│   │   ├── AiOrchestrator.java             # 统一编排层，实现 AiService
│   │   └── CapabilityExecutor.java         # 能力执行器接口
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
│   │   ├── PromptTemplate.java            # 模板值对象
│   │   ├── DatabasePromptTemplateManager.java # 数据库持久化实现
│   │   └── PromptTemplateRepository.java  # JPA Repository
│   ├── experiment/
│   │   ├── ExperimentManager.java          # 实验管理接口
│   │   ├── ExperimentAssignment.java       # 分组结果值对象
│   │   ├── Experiment.java                # 实验配置值对象
│   │   ├── ExperimentRepository.java      # JPA Repository
│   │   └── HashBucketExperimentManager.java # 哈希分桶实现
│   ├── metrics/
│   │   ├── AiMetricsCollector.java         # 指标采集接口
│   │   ├── AiCallRecord.java              # 调用记录值对象
│   │   ├── LoggingMetricsCollector.java   # 日志输出实现（Phase 5）
│   │   └── AiCallLogRepository.java       # AI 调用日志 JPA Repository
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
│   │   ├── TimeoutDegradationStrategy.java # 新增：超时降级策略
│   │   └── CircuitBreakerDegradationStrategy.java # 新增：熔断降级策略
│   ├── config/
│   │   └── AiPlatformConfig.java         # 底座 Bean 装配与配置属性绑定
│   └── FallbackAiService.java             # 保留，装饰器不变
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

---

## 3. 核心抽象

### 3.1 编排层

#### `AiOrchestrator` — 统一编排器（class，归属 `ai-impl/orchestrator/`）

**职责**：替代原 `MockAiService` 成为 `FallbackAiService` 的实际委托对象（`ai.mock.enabled=false` 时激活），实现 `AiService` 全部 13 个方法。每个方法的执行流程为：

1. **模板渲染**：委托 `PromptTemplateManager` 按能力标识与科室标识检索模板，将业务 DTO 字段注入模板变量，生成渲染后 Prompt
2. **实验分流**：委托 `ExperimentManager` 判定当前请求是否命中 A/B 实验，若命中则返回分组信息（覆盖默认模型路由或 Prompt 版本）
3. **模型路由**：委托 `ModelRouter` 根据能力标识与实验分组选择目标模型配置（`ModelRoute`）
4. **模型调用**：委托 `LlmClient` 发送渲染后 Prompt 到目标模型，获取原始文本输出；调用前设置硬超时
5. **结果解析**：委托 `StructuredOutputParser` 将原始文本输出解析为对应能力的 Java DTO
6. **指标采集**：委托 `AiMetricsCollector` 记录本次调用的能力标识、耗时、是否降级、Token 用量、错误码等
7. **降级兜底**：若步骤 4 失败（超时/异常/模型不可用），先委托 `DegradationStrategy` 判定是否应触发降级，再委托 `LocalRuleFallback` 执行本地规则降级（若该能力有本地规则实现）

**协作对象**：
- 实现 `AiService`，被 `FallbackAiService` 委托调用
- 内部持有 `PromptTemplateManager`、`ExperimentManager`、`ModelRouter`、`LlmClient`、`StructuredOutputParser`、`AiMetricsCollector`、`List<LocalRuleFallback>`、`List<DegradationStrategy>` 的引用
- 每个 `AiService` 方法的实现委托给一个内部 `CapabilityExecutor` 实例

**为何使用 class 而非 interface**：编排器是唯一的运行时实现实例，不需要多态；其核心职责是协调各子组件完成管线执行，属于"固定骨架 + 可插拔步骤"的模板方法模式，但鉴于各能力方法签名差异大（输入/输出 DTO 类型各不相同），不适合用泛型模板方法统一，而是为每个能力方法独立编排管线步骤。

**Bean 装配策略**：
- `AiOrchestrator`：标注 `@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "false")`
- `MockAiService`：保留现有 `@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "true", matchIfMissing = true)`
- 二者通过同一配置键互斥，延续 Phase 0 的装配策略，`FallbackAiService` 无需变更

#### `CapabilityExecutor` — 能力执行接口（interface，归属 `ai-impl/orchestrator/`）

**职责**：定义单项 AI 能力的执行管线契约。每个 AI 能力（如智能分诊、处方审核等）对应一个 `CapabilityExecutor` 实现，封装该能力特有的 Prompt 模板变量映射、结构化输出解析逻辑与本地规则降级逻辑。

**协作对象**：
- 被 `AiOrchestrator` 在对应方法中调用
- 内部使用 `PromptTemplateManager`、`ModelRouter`、`LlmClient`、`StructuredOutputParser`、`AiMetricsCollector` 完成管线
- 可选关联 `LocalRuleFallback`（仅处方审核等需要本地规则降级的能力）

**为何使用 interface**：不同 AI 能力的执行细节差异大（输入字段映射、输出解析规则、降级策略各不同），用 interface 抽象使 `AiOrchestrator` 的每个方法可委托给独立实现，便于单能力替换与单元测试。若未来新增 AI 能力，只需新增一个 `CapabilityExecutor` 实现并在 `AiOrchestrator` 中注册。

### 3.2 模型对接层

#### `LlmClient` — 大模型统一客户端（interface，归属 `ai-impl/client/`）

**职责**：定义大模型调用的统一协议。屏蔽 HTTP API 直接调用与 Spring AI ChatModel 两种底层接入方式的差异，为上层（`CapabilityExecutor`）提供一致的调用体验。

**协作对象**：
- 被 `CapabilityExecutor` 在模型调用步骤中使用
- `HttpApiLlmClient`（实现）：通过 HTTP REST API 调用外部大模型服务端点（如国产大模型 API），自行管理请求构建、签名认证、重试与超时
- `SpringAiLlmClient`（实现）：基于 Spring AI 的 `ChatModel` / `ChatClient` 封装，用于接入 Spring AI 原生支持的模型供应商

**为何使用 interface**：模型接入方式存在 HTTP API 和 Spring AI 两种异构实现，且未来可能新增 gRPC 或其他协议接入方式。interface 使路由层可按 `ModelRoute` 配置选择不同的客户端实例，运行时动态切换。

#### `LlmRequest` — 大模型统一请求（class，归属 `ai-impl/client/`）

**职责**：封装一次大模型调用的全部入参，包括渲染后的 Prompt 文本、模型标识（用于审计与路由回溯）、生成参数（temperature、maxTokens 等）与能力标识（用于指标关联）。

**为何使用 class**：纯数据传输对象，不需要多态，class 即可。

#### `LlmResponse` — 大模型统一响应（class，归属 `ai-impl/client/`）

**职责**：封装一次大模型调用的原始输出，包括文本内容、Token 用量（promptTokens / completionTokens / totalTokens）、使用的模型标识与响应时间戳。

**为何使用 class**：纯数据传输对象，与 `LlmRequest` 对称。

#### `ModelRouter` — 模型路由契约（interface，归属 `ai-impl/router/`）

**职责**：根据能力标识与可选的实验分组信息，决定本次调用应使用哪个模型配置（端点地址、模型名称、客户端类型）。默认实现为基于配置映射的路由——在 `application.yml` 或数据库中维护"能力标识 → 模型配置"映射表，运行时按表查找。

**协作对象**：
- 被 `CapabilityExecutor` 在模型路由步骤中调用
- 返回 `ModelRoute` 值对象供 `CapabilityExecutor` 选择 `LlmClient` 实例与构建 `LlmRequest`
- 与 `ExperimentManager` 协作：若实验分组指定了覆盖模型，优先采用实验分组的模型配置

**为何使用 interface**：路由策略可能从简单映射演进为基于负载/延迟/成本的动态路由，interface 使路由逻辑可替换。

#### `ModelRoute` — 模型路由条目（class，归属 `ai-impl/router/`）

**职责**：封装一条模型路由的目标信息，包括模型标识、端点 URL、客户端类型（HTTP_API / SPRING_AI）、生成参数默认值与权重。路由表可由配置文件或数据库维护。

**为何使用 class**：纯值对象，承载路由元数据。

### 3.3 Prompt 模板管理层

#### `PromptTemplateManager` — Prompt 模板管理契约（interface，归属 `ai-impl/template/`）

**职责**：
- 按能力标识与可选科室标识检索当前生效的 Prompt 模板
- 支持模板渲染：将业务 DTO 字段注入模板变量占位符，生成最终 Prompt 文本
- 模板版本管理：同一能力+科室可存在多个版本，同一时刻仅一个版本为启用状态
- 支持运行时热加载：管理员在管理端修改模板后，下一次 AI 调用即生效，无需重启服务

**协作对象**：
- 被 `CapabilityExecutor` 在模板渲染步骤中调用
- 从 `PromptTemplateRepository` 加载模板（数据库持久化）
- 与 3.3.3 AI 分诊台规则配置的 `rule_version` / `rule_set_id` 联动：分诊能力模板可引用当前规则版本号

**为何使用 interface**：模板存储可能从数据库扩展到版本控制仓库或配置中心，interface 使存储后端可替换。

#### `PromptTemplate` — Prompt 模板值对象（class，归属 `ai-impl/template/`）

**职责**：封装一个 Prompt 模板的完整元数据，包括模板标识、能力标识（如 `TRIAGE`、`RX_AUDIT`）、科室标识（可选，科室维度覆盖通用模板）、模板内容（含 `{{变量名}}` 占位符）、版本号、启用状态与创建时间。

**为何使用 class**：纯值对象，承载模板元数据，同时作为 JPA Entity 持久化到数据库。

### 3.4 A/B 实验管理层

#### `ExperimentManager` — A/B 实验管理契约（interface，归属 `ai-impl/experiment/`）

**职责**：
- 判断当前请求是否命中某个 A/B 实验
- 若命中，返回实验分组信息（`ExperimentAssignment`），包括应使用的模型标识或 Prompt 版本
- 支持按能力标识维度配置实验，实验范围限定在单一 AI 能力内
- 支持按用户标识（患者/医生 ID）或会话标识做确定性分桶（保证同一用户/会话始终分配到同一分组）

**协作对象**：
- 被 `CapabilityExecutor` 在实验分流步骤中调用
- 从 `ExperimentRepository` 加载实验配置
- 分组结果影响 `ModelRouter` 的路由决策和 `PromptTemplateManager` 的模板选择

**为何使用 interface**：分桶算法可能从哈希分桶演进为更复杂的策略（如多臂老虎机/贝叶斯优化），interface 使分桶逻辑可替换。

#### `ExperimentAssignment` — 实验分组结果值对象（class，归属 `ai-impl/experiment/`）

**职责**：封装一次实验分流的结果，包括实验标识、分组标识、目标模型标识（覆盖默认路由）、目标 Prompt 版本号（覆盖默认模板）。未命中任何实验时返回空值对象（分组标识为 "default"）。

**为何使用 class**：纯值对象。

#### `Experiment` — 实验配置值对象（class，归属 `ai-impl/experiment/`）

**职责**：封装一个 A/B 实验的配置元数据，包括实验标识、关联能力标识、分组列表（每项含分组名、流量百分比、目标模型/Prompt 版本）、启用状态与时间窗口（开始/结束时间）。同时作为 JPA Entity 持久化。

**为何使用 class**：值对象 + Entity 双重角色。

### 3.5 性能观测层

#### `AiMetricsCollector` — AI 性能指标采集契约（interface，归属 `ai-impl/metrics/`）

**职责**：
- 接收每次 AI 能力调用的完整记录（`AiCallRecord`），执行异步持久化写入 `AI 调用日志`（5.2）
- 同时将关键指标推送到 Micrometer 指标体系（`aimedical.ai.request.duration`、`aimedical.ai.degradation.count`），供 `/actuator/metrics` 端点或 Prometheus 消费
- Phase 5 默认实现为日志输出 + 数据库持久化；Phase 6 可扩展为完整可观测性平台集成

**协作对象**：
- 被 `CapabilityExecutor` 在指标采集步骤中调用
- 写入 `AiCallLogRepository`（JPA）实现持久化
- 通过 Micrometer `MeterRegistry` 注册指标

**为何使用 interface**：采集实现可能从数据库+日志迁移到专业可观测性平台，interface 使采集后端可替换。

#### `AiCallRecord` — AI 调用记录值对象（class，归属 `ai-impl/metrics/`）

**职责**：封装一次 AI 能力调用的全部可观测性字段，与 5.2 AI 调用日志实体字段一一对应。作为 `AiMetricsCollector` 的输入参数与持久化数据源，包括：调用时间、能力标识、能力名称、关联就诊标识、患者标识、调用方角色、调用方标识、输入摘要、输出摘要、是否降级、降级原因、耗时毫秒、错误码、错误消息、模型标识、重试次数、会话标识。

**为何使用 class**：纯值对象，字段集冻结。

### 3.6 结构化输出解析层

#### `StructuredOutputParser` — 结构化输出解析契约（interface，归属 `ai-impl/parser/`）

**职责**：定义从 LLM 原始文本输出中解析出 Java DTO 的统一协议。不同 AI 能力的输出格式各异（部分为 JSON，部分为自由文本+JSON 混合），由各能力的 `CapabilityExecutor` 实现自行选择或实现解析策略。

**协作对象**：
- 被 `CapabilityExecutor` 在结果解析步骤中调用
- `JsonStructuredOutputParser`（默认实现）：假设 LLM 输出为 JSON 格式，基于 Jackson 反序列化到目标 DTO 类型

**为何使用 interface**：不同能力可能需要不同的解析策略（JSON / Markdown 代码块提取 / 自定义正则），interface 使解析逻辑可按能力定制。

### 3.7 本地规则降级层

#### `LocalRuleFallback` — 本地规则降级契约（interface，归属 `ai-impl/fallback/`）

**职责**：定义 AI 能力降级到本地规则校验时的执行入口。仅特定能力需要实现此接口（当前仅为 3.4.2 AI 处方审核），其余能力降级时返回 `AiResult.degraded()` 或要求用户手动操作。

**协作对象**：
- 被 `CapabilityExecutor` 在降级兜底步骤中调用（仅当 `DegradationStrategy.shouldDegrade()` 返回 true 或 LLM 调用失败时）
- `PrescriptionLocalRuleFallback`（实现）：执行 3.4.2 规定的本地规则校验最小检查项（剂量范围检查、药品禁忌检查、重复用药检查、儿童/老年人群特殊剂量检查），返回带降级标记的 `PrescriptionCheckResponse`

**为何使用 interface**：本地规则降级仅适用于部分能力，interface 使能力级别的降级实现可插拔，不强制所有能力都实现。

### 3.8 降级策略扩展

#### `TimeoutDegradationStrategy` — 超时降级策略（class，归属 `ai-impl/degradation/`）

**职责**：基于 `DegradationContext` 中的最近调用耗时信息判定是否触发降级。若某能力的最近 N 次（可配置）调用平均耗时超过其硬超时阈值的 80%，触发降级以主动避免后续调用继续超时。

**协作对象**：
- 实现 `DegradationStrategy`（Phase 0 已定义接口）
- 读取扩展后的 `DegradationContext` 中的 `elapsedTime`、`requestType`、`invocationCount`、`lastFailureTime` 字段
- 内部维护每个能力标识的调用耗时滑动窗口

**为何使用 class**：具体策略实现，不需要多态。

#### `CircuitBreakerDegradationStrategy` — 熔断降级策略（class，归属 `ai-impl/degradation/`）

**职责**：当某能力的最近调用失败率超过阈值（可配置，默认 50%）时，触发熔断——在熔断窗口（可配置，默认 30 秒）内所有对该能力的调用直接走降级路径，不再尝试 LLM 调用。

**协作对象**：
- 实现 `DegradationStrategy`
- 读取 `DegradationContext` 中的失败率信息
- 内部维护每个能力标识的熔断状态（CLOSED / OPEN / HALF_OPEN）与失败计数滑动窗口

**为何使用 class**：具体策略实现。

#### `DegradationContext` — 降级判定上下文（class，归属 `ai-api/degradation/`，扩展）

**职责**：Phase 0 定义为零值构造器骨架。Phase 5 扩展以下字段（方法签名不变，保持向后兼容）：
- `invocationCount`（int，该能力近窗口内调用次数）
- `lastFailureTime`（long，最近一次失败时间戳 epoch ms）
- `elapsedTime`（long，最近一次调用耗时 ms）
- `requestType`（String，能力标识，如 "TRIAGE"/"RX_AUDIT"）
- `failureCount`（int，该能力近窗口内失败次数）

**扩展方式**：在现有无参构造器基础上新增全参构造器与 Builder 模式，无参构造器保留（字段取语言默认值），确保 `DegradationStrategy.shouldDegrade(DegradationContext)` 方法签名不变。

---

## 4. 关键行为契约

### 4.1 AI 能力统合调用管线

```
AiOrchestrator.triage(request):
  1. CapabilityExecutor.execute(request):
     a. PromptTemplateManager.render("TRIAGE", 科室, request字段) → renderedPrompt
     b. ExperimentManager.assign("TRIAGE", userId, sessionId) → assignment
     c. ModelRouter.route("TRIAGE", assignment) → modelRoute
     d. LlmClient.invoke(LlmRequest(renderedPrompt, modelRoute)) → llmResponse
     e. StructuredOutputParser.parse(llmResponse, TriageResponse.class) → parsedResult
     f. AiMetricsCollector.record(AiCallRecord) [异步]
     g. 返回 AiResult.success(parsedResult)
  2. 若步骤 d/e 失败:
     a. DegradationStrategy.shouldDegrade(context):
        - true → LocalRuleFallback.fallback(request) 或 AiResult.degraded(reason) [异步记录指标]
        - false → 重试一次（若 LlmClient 支持重试）→ 仍然失败 → AiResult.degraded(reason)
```

所有 13 个能力方法遵循相同管线，差异仅在模板变量映射、输出 DTO 类型和是否有 `LocalRuleFallback` 实现。

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
  1. 检索该能力标识下当前启用的实验列表
  2. 无实验 → 返回空 assignment（分组=default）
  3. 有实验 → 按 userId/sessionId 哈希值 % 1000 映射到 [0, 1000) 区间
  4. 遍历分组列表，找到哈希值落在其流量百分比区间的分组
  5. 返回 ExperimentAssignment(实验标识, 分组标识, 目标模型, 目标Prompt版本)
```

**确定性保证**：同一 userId/sessionId + 同一实验配置，始终分配到同一分组，除非实验配置变更。

### 4.4 Prompt 模板渲染契约

```
PromptTemplateManager.render(capabilityId, departmentId, variables):
  1. 按 capabilityId + departmentId 查询启用模板
  2. departmentId 有值且存在科室级模板 → 优先使用科室级模板
  3. departmentId 无值或无科室级模板 → 使用通用模板（departmentId=null）
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
  5. 反序列化失败 → 抛出 parse 异常 → 由 CapabilityExecutor 捕获并触发降级
```

### 4.6 性能指标采集契约

```
AiMetricsCollector.record(AiCallRecord):
  1. 异步写入 AiCallLogRepository（JPA）
  2. 同步推送 Micrometer 指标:
     - aimedical.ai.request.duration (Timer, tags: ability, status, model)
     - aimedical.ai.request.count (Counter, tags: ability, status)
     - aimedical.ai.degradation.count (Counter, tags: ability, reason)
     - aimedical.ai.token.usage (DistributionSummary, tags: ability, model, type[prompt/completion])
```

### 4.7 熔断器状态转换契约

```
CircuitBreakerDegradationStrategy:
  状态: CLOSED → OPEN → HALF_OPEN → CLOSED
  - CLOSED: 正常通过，记录成功/失败
  - OPEN: 所有请求直接降级，不尝试 LLM 调用
  - HALF_OPEN: 允许一次探测请求通过
    - 探测成功 → 闭合为 CLOSED
    - 探测失败 → 重新打开为 OPEN
  触发条件:
  - CLOSED → OPEN: 滑动窗口内失败率 ≥ 50%（窗口 60 秒）
  - OPEN → HALF_OPEN: 熔断窗口到期（默认 30 秒）
```

---

## 5. 错误处理策略

### 5.1 错误分类（AI 底座层面新增）

| 错误类别 | 代表场景 | 处理方式 | 响应形态 |
|---------|---------|---------|---------|
| 模板缺失/渲染失败 | 模板不存在、变量缺失致渲染异常 | 日志 WARN + 使用能力内硬编码兜底 Prompt | 继续调用 LLM（降级体验） |
| 实验分流异常 | 实验配置非法 | 日志 WARN + 降级到 default 分组 | 继续调用 LLM（无实验干预） |
| 模型路由失败 | 无可用模型路由 | 直接触发降级 | `AiResult.degraded()` |
| LLM 调用超时 | 超过能力硬超时阈值 | 超时策略 + 熔断 + 本地规则降级 | `AiResult.degraded()` 或 `LocalRuleFallback` 结果 |
| LLM 调用不可用 | 模型服务 HTTP 5xx / 连接拒绝 | 重试 1 次 → 仍失败则降级 | `AiResult.degraded()` |
| 结构化输出解析失败 | LLM 返回非 JSON / 格式错误 | 尝试提取 JSON 片段重试 → 仍失败则降级 | `AiResult.degraded()` |
| 熔断触发 | 失败率超阈值 | 跳过 LLM 调用，直接降级 | `AiResult.degraded()` |

### 5.2 降级优先级

当多种降级条件同时满足时，按以下优先级处理：
1. **熔断** > **超时** > **LLM 不可用** > **解析失败**
2. 熔断触发时跳过 LLM 调用，不进入超时判定
3. 有 `LocalRuleFallback` 实现的能力（如处方审核），降级时返回本地规则结果；其余能力返回 `AiResult.degraded()`

---

## 6. 并发设计

### 6.1 线程模型

- `AiOrchestrator`：无状态，所有方法线程安全，由 Spring 默认单例管理
- `LlmClient`：无状态，线程安全。HTTP 客户端基于连接池（Apache HttpClient / OkHttp），连接数由配置控制
- `ModelRouter`：路由表启动时从配置/DB 加载到内存 `ConcurrentHashMap`，支持运行时刷新（管理端触发 → 刷新事件 → 路由表热更新）
- `PromptTemplateManager`：模板缓存使用 `ConcurrentHashMap<CacheKey, PromptTemplate>`，管理端变更后通过 Spring ApplicationEvent 通知缓存失效
- `ExperimentManager`：实验配置缓存同理，基于事件驱动的缓存失效
- `AiMetricsCollector`：异步写入，使用 Spring `@Async` + 线程池（核心线程 2，最大线程 4，队列容量 1000），不阻塞调用链路

### 6.2 熔断器线程安全

- `CircuitBreakerDegradationStrategy` 内部每个能力标识维护独立的 `CircuitBreakerState` 实例
- 状态转换使用 `AtomicReference<CircuitState>` + CAS 保证原子性
- 失败计数滑动窗口使用 `ConcurrentHashMap<String, Deque<Long>>`，与 `LoginAttemptTracker` 同构

### 6.3 数据库写入竞争

- `AiCallLogRepository` 的写入为追加操作（INSERT），无竞争
- `PromptTemplateRepository` 和 `ExperimentRepository` 的读写分离：读走内存缓存，写走管理端 API + 事件通知缓存失效

---

## 7. 设计决策

| 决策 | 选项 | 选择 | 理由 |
|------|------|------|------|
| 编排层形态 | 全新 AiService 实现 vs 在 FallbackAiService 内扩展 | 全新 `AiOrchestrator` 实现 `AiService`，替代 `MockAiService` | FallbackAiService 职责是降级装饰，不应混入编排逻辑；独立编排层便于单独测试和渐进替换 |
| LLM 客户端抽象 | 直接使用 Spring AI vs 自定义抽象层 | 自定义 `LlmClient` interface，Spring AI 作为可选实现 | 技术栈文档要求支持 HTTP API 调用大模型和 Spring AI 两种方式；自定义抽象层允许两种方式共存与按能力切换 |
| Prompt 模板存储 | 配置文件 vs 数据库 | 数据库（JPA Entity）+ 内存缓存 | 管理端需要在运行时修改模板（3.3.3 分诊台规则配置联动），配置文件无法热更新；缓存解决查询性能 |
| A/B 实验分桶 | 简单哈希分桶 vs 多臂老虎机 vs 贝叶斯优化 | 哈希分桶（Phase 5） | Phase 5 目标为"基础 A/B 实验框架可冒烟"，哈希分桶实现简单且满足确定性分桶要求；高级策略留 Phase 6 |
| 指标采集方式 | 仅 Micrometer vs 仅数据库 vs 双写 | 双写：数据库持久化 + Micrometer 指标推送 | 数据库满足 5.2 AI 调用日志的可追溯性要求；Micrometer 满足 Phase 6 可观测性平台的实时指标需求 |
| 降级策略扩展 | 保留 NoOp + 新增超时/熔断 vs 全部替换 | 在 NoOp 基础上新增超时/熔断，按优先级链式判定 | NoOp 作为兜底保留（Phase 2 无 NoOp 场景不出现）；超时/熔断按能力标识独立配置；链式判定确保高级别降级条件优先触发 |
| 结构化输出方式 | 强制 JSON 输出 vs Spring AI Structured Output vs 自定义解析 | 自定义 `StructuredOutputParser` interface + JSON 解析默认实现 | Spring AI Structured Output 依赖特定模型能力且与 LlmClient 抽象耦合；自定义解析器可适配 JSON/Markdown/自由文本等多种输出格式 |
| 能力执行器粒度 | 每能力一个 CapabilityExecutor vs 全能力同管 | 每能力一个 `CapabilityExecutor` 实现 | 13 项能力输入/输出类型各不同，降级策略各异，统一管会导致巨型类；独立实现便于单能力替换与测试 |
| DegradationContext 扩展方式 | 新增字段 vs 新增子类 | 新增字段（保持无参构造器兼容） | Phase 0 已冻结 `shouldDegrade(DegradationContext)` 方法签名，子类方案需改变参数类型，违反冻结约定 |

---

## 8. 迁移路径

### 8.1 从 Phase 2~4 独立接入迁移至底座

| 迁移项 | Phase 2~4 现状 | Phase 5 目标 | 迁移策略 |
|--------|---------------|-------------|---------|
| 3.4.1 智能分诊 | 独立 HTTP 调用或 Spring AI 实现 | 迁移至 `AiOrchestrator` 管线 | 新增 `TriageCapabilityExecutor`，Prompt 模板化 |
| 3.4.2 AI 处方审核 | 独立实现 + 本地规则降级 | 迁移至 `AiOrchestrator` 管线 | 新增 `PrescriptionCheckCapabilityExecutor` + `PrescriptionLocalRuleFallback` |
| 3.4.3 AI 病历生成 | 独立实现 | 迁移至 `AiOrchestrator` 管线 | 新增 `MedicalRecordGenCapabilityExecutor`，支持流式输出扩展点 |
| 3.4.10 AI 辅助开方 | 独立实现 | 迁移至 `AiOrchestrator` 管线 | 新增 `PrescriptionAssistCapabilityExecutor`，与 3.4.2 共享药品知识库查询 |
| 3.4.8 AI 知识库问答 | Phase 2 Mock → Phase 5 真实落地 | 首次真实实现于底座 | 新增 `KbQueryCapabilityExecutor`，集成 RAG 检索 |
| 3.4.12 AI 医生排班 | Phase 5 首次落地 | 首次真实实现于底座 | 新增 `ScheduleCapabilityExecutor` |
| 3.4.13 AI 综合讨论结论 | Phase 5 首次落地 | 首次真实实现于底座 | 新增 `DiscussionConclusionCapabilityExecutor` |

**迁移原则**：
- `AiService` 接口签名不变，业务模块注入代码不变
- 迁移通过替换 `FallbackAiService` 下的委托对象完成——原来委托 `MockAiService`，迁移后委托 `AiOrchestrator`
- 逐能力迁移，不要求一次性全部切换；未迁移的能力继续走原独立实现（通过 `@ConditionalOnProperty` 或配置路由表区分）

### 8.2 配置切换

```yaml
ai:
  mock:
    enabled: false                    # Phase 5: 关闭 Mock
  platform:
    enabled: true                     # 激活 AI 进阶底座
  router:
    routes:
      TRIAGE: { model: "qwen-plus", endpoint: "https://api.example.com/v1/chat/completions", client: "HTTP_API" }
      RX_AUDIT: { model: "qwen-turbo", endpoint: "https://api.example.com/v1/chat/completions", client: "HTTP_API" }
      # ... 其余能力按需配置
  circuit-breaker:
    enabled: true
    failure-rate-threshold: 0.5
    window-seconds: 60
    open-duration-seconds: 30
  timeout:
    enabled: true
    threshold-percentage: 0.8          # 耗时达硬超时 80% 触发超时降级策略
    sample-size: 10
```

---

## 9. 与其他包的协作边界

### 9.1 包 G 与包 F（AI 分诊台规则）的协作

- 包 F 在管理端维护分诊规则（症状-科室映射），产出 `rule_version` / `rule_set_id`
- 包 G 的 `PromptTemplateManager` 在渲染分诊 Prompt 时，允许模板引用当前规则版本号变量
- 规则版本变更后，分诊 Prompt 自动携带最新版本号传入 LLM

### 9.2 包 G 与包 E（AI 影像模型管理面）的协作

- 包 E 管理影像模型注册、岗位授权与调用审计
- 包 G 的 `ModelRouter` 路由表中，3.4.7 影像分析的 `ModelRoute` 由包 E 管理的模型注册数据驱动
- 包 E 的岗位授权校验在 `ImageAnalysisCapabilityExecutor` 中通过 `CurrentUser` 接口获取当前医生岗位后执行

### 9.3 包 G 与其他业务模块的协作

- 业务模块（patient/doctor/admin）仍通过 `AiService` 接口调用 AI 能力，不感知底座内部变化
- `AiMetricsCollector` 写入的 AI 调用日志（5.2）可被管理端的综合管理（3.3.4）统计分析
- `PromptTemplateManager` 的模板维护入口在管理端，归包 A（管理员端基础数据维护）
