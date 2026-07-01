# 质量审查诊断报告 — Phase5 包G OOD 设计 (v25)

## 审查范围

- **审查视角**：落地实现视角，侧重需求响应充分度、整体深度与完整性、内部审议未充分覆盖的维度
- **待审查产出**：`a_v2_copy_from_v1.md`（v25，3516 行）
- **用户需求**：Phase5 包G 的完整 OOD 设计（类图、核心职责、协作关系、关键接口、状态模型），参考已有 Phase0/Phase1ABD 保持风格一致

---

## 发现问题

### 问题1：[严重] `ai-api` "不变"声明与实质性变更之间存在系统性矛盾

**位置**：
- §1.2：「在现有 `AiService` 接口不变的前提下」
- §2.1 目录结构：`DegradationStrategy.java` 标注「不变（接口签名冻结）」、`AiService.java`/`AiResult.java` 标注「不变」
- §2.2：「`ai-api` 保持不变」
- §3.8：要求新增 `DegradationStrategy.getOrder()` default method
- §3.8：要求扩展 `DegradationContext` 新增字段
- §3.5：要求新建 `AiRequestBase` 抽象类（归属 `ai-api/dto/base/`）
- §3.8：要求新建 `DegradationReason` 枚举（归属 `ai-api/degradation/`）

**问题描述**：
设计在 3 处（§1.2、§2.1、§2.2）明确声明 `ai-api` 保持不变、接口签名冻结，但实际包含 4 项实质性变更：
1. `DegradationStrategy` — 新增 `getOrder()` default method（即使二进制兼容，仍是接口变更）
2. `DegradationContext` — 新增字段（`requestType`, `invocationCount`, `failureCount` 等）及 `Serializable` 实现
3. `AiRequestBase` — 全新的抽象类（影响 13 个 DTO 的继承体系）
4. `DegradationReason` — 全新的枚举

其中第 3 项 `AiRequestBase` 的引入将导致 `ai-api` 模块的 DTO 继承结构改变，影响 7 项底座能力 DTO 的类声明和 Jackson 注解，这与「`ai-api` 保持不变」的声明直接矛盾。实现者将困惑于到底哪些部分可以修改、哪些需要冻结。

**影响**：实现者无法从「不变」声明中准确判断 `ai-api` 的修改边界，可能导致两种极端——不敢修改（delay）或随意修改（打破冻结契约）。

**严重程度**：严重

**改进建议**：
- 删除「不变」「接口签名冻结」的绝对化表述，替换为精确的约束声明：
  - `AiService` 接口签名：完全冻结（方法签名、返回值、异常声明均不可修改）
  - `DegradationStrategy`：仅允许新增 `default` 方法，不允许新增抽象方法
  - `DegradationContext`：允许扩展字段（保持无参构造器 + Builder 模式）
  - `AiRequestBase` / `DegradationReason`：新文件，不涉及现有接口变更
- 在 §1.1 新增「ai-api 变更范围总结表」，明确列出每个 `ai-api` 类型的变更类型（不变/扩展/新增）

---

### 问题2：[严重] 多实例部署场景下的关键组件行为缺口

**位置**：
- §6.1 线程模型
- §3.5 `SlidingWindowMetricsStore`
- §3.2 `EndpointRateLimiter`, `CredentialProvider`
- §3.8 `CircuitBreakerDegradationStrategy`
- §10.4 分布式部署兜底

**问题描述**：
设计在 §10.4 仅讨论了事件驱动缓存失效的分布式局限（`ApplicationEvent` 进程内），但未评估以下 3 个同样受多实例部署影响的组件的跨实例行为：

1. **`SlidingWindowMetricsStore`（§3.5）**：每个实例维护独立的 `ConcurrentHashMap<String, Deque<WindowedEvent>>`。在多实例部署下，同一能力的熔断/超时判定基于单实例局部窗口数据。若某实例突然承受高流量，其局部失败率可能触发熔断，而其他实例仍正常处理。设计未说明该非对称状态是否可接受，也未提供跨实例聚合窗口的演进方向。

2. **`EndpointRateLimiter`（§3.2）**：Guava 令牌桶为进程内实现，每实例独立限流。若部署 2 个实例，每个配置 `permits-per-second: 10`，则集群总限流为 20 req/s，而非 10 req/s。设计未指出此约束，运维按文档配置限流参数可能导致供应商侧限流触发。

3. **`CircuitBreakerDegradationStrategy`（§3.8）**：熔断器状态（CLOSED/OPEN/HALF_OPEN）为实例本地状态。在 HALF_OPEN 场景下，若实例 A 已发送探测请求，实例 B 也可能因状态转换独立发送探测请求，导致探测调用量翻倍。设计未讨论此竞争条件。

**影响**：运维按设计文档配置多实例部署后，限流/熔断行为与预期不一致，需要在生产环境中打补丁而非在设计层面提前约束。

**严重程度**：严重

**改进建议**：
- 在 §10.4 分布式兜底段下方新增「多实例行为约束」子章节，逐组件分析：
  - `SlidingWindowMetricsStore`：当前 Per-instance 设计可接受（Phase 5 单实例阶段）；Phase 6 集群阶段需引入 Redis 滑动窗口或基于时间戳的准实时聚合
  - `EndpointRateLimiter`：Per-instance 限流，配置注释需注明「单实例维度」；集群限流通过 Nginx 均匀分发或集中式 Redis 限流器实现
  - `CircuitBreakerDegradationStrategy`：Per-instance 允许探测竞争，探测成功率应由业务监控而非设计文档保证
- 在 §1.1 部署形态章节（当前缺失）中说明 Phase 5 的部署目标（单实例/多实例/Docker/K8s），作为上述约束的判断依据

---

### 问题3：[重要] 伪代码与新/旧 API 表面无区分标记，实现者难以确定实施起点

**位置**：
- §4.1 完整管线伪代码（约 130 行）
- §4.2 薄适配器特化伪代码（约 40 行）

**问题描述**：
§4.1 和 §4.2 的伪代码引用了约 10 个关键接口调用，这些接口在代码库中的存在状态各不相同：

| 伪代码中调用的接口 | 代码库现状 | 创建类型 |
|---|---|---|
| `experimentManager.assign()` | 不存在 | 新接口 + 新实现 |
| `promptTemplateManager.render()` | 不存在 | 新接口 + 新实现 |
| `modelRouter.route()` | 不存在 | 新接口 + 新实现 |
| `llmChatService.structuredChat()` | 不存在（LlmChatService 整体不存在） | 新接口 + 新实现 |
| `llmChatService.chat()` | 不存在 | 新接口 + 新实现 |
| `endpointHealthManager.getState()` | 不存在 | 新类 + 新实现 |
| `slidingWindowMetricsStore.buildDegradationContext()` | 不存在 | 新类 + 新实现 |
| `structuredOutputParser.parse()` | 不存在 | 新接口 + 新实现 |
| `metricsCollector.record()` | 不存在（AiMetricsCollector 整体不存在） | 新接口 + 新实现 |
| `endpointRateLimiter.tryAcquire()` | 不存在 | 新类 + 新实现 |
| `credentialProvider.getCredential()` | 不存在 | 新接口 + 新实现 |
| `RequestContextUtils.extractFromRequestContext()` | 不存在 | 新类 |

设计将所有调用写成统一的伪代码风格，未标注哪些接口已有实现、哪些需要新建。实现者需要逐一检查代码库才能分清工作边界，增加了实施门槛。

**对比**：§3.1 中 `DiagnosisCapabilityExecutor` 的构造器注入示例明确列出了必须和可选的依赖，伪代码部分反而缺少这种区分。

**影响**：实现者入手困难——必须通读全篇设计文档 + 对照代码库才能确定先创建哪些类、后创建哪些类。项目排期受影响。

**严重程度**：重要

**改进建议**：
- 在 §4.1 伪代码段前新增「API Surface 状态表」：列出所有被引用的接口/类，标注「已存在-需扩展」「需新建」「需迁移」三种状态
- 在 §9 迁移路径中补充「新建接口优先级」排序：基础设施层（CredentialProvider → EndpointRateLimiter → SlidingWindowMetricsStore）→ 模型对接层（LlmChatService → LlmChatRequest 等 DTO）→ 编排层（ModelRouter → PromptTemplateManager → ExperimentManager）→ CapabilityExecutor 管线 → AiOrchestrator

---

### 问题4：[重要] §3.5 AiRequestBase 的 Jackson 兼容性测试要求与设计描述自相矛盾

**位置**：
- §3.5 「AiRequestBase — AI 能力请求基类」段，「现有 DTO 影响评估与向后兼容策略」子段落

**问题描述**：
设计在 §3.5 中提出 Jackson 兼容性测试要求：「在引入基类的同一 Changelist 中，为 7 项底座能力 DTO 各添加一条反序列化兼容性单元测试——将旧形态 JSON（无 AiRequestBase 字段）反序列化为新 DTO 实例，断言 `departmentId`/`visitId`/`patientId`/`sessionId` 为 null」。

此要求存在自相矛盾：旧形态 JSON **不包含**这些字段，反序列化后它们当然为 null（Java 对象字段默认值），此测试的断言本身无法验证「Jackson 兼容性」——它验证的是「忽略未知字段」行为（依赖 `@JsonIgnoreProperties(ignoreUnknown = true)`）。真正的兼容性风险在于：
- 旧 DTO 的 JSON **包含**了同名字段但类型不同的场景（如某个 DTO 的 `visitId` 是 `Long` 而 `AiRequestBase` 声明为 `String`）
- Jackson 的 `@JsonTypeInfo` 多态序列化产生 `@class` 属性引入后对现有序列化数据的破坏
- `@JsonCreator` + `@ConstructorProperties` 与继承基类的兼容性（父类字段在构造器参数中的位置）

设计已隐含此风险（§3.5 第 1736 行「若发现则统一为 `String`」），但测试用例设计未覆盖真实的兼容性风险点。

**影响**：实现者按文档要求编写无判别力的测试，覆盖了虚假的安全感而未覆盖真实风险。上线后再发现序列化兼容性 Bug。

**严重程度**：重要

**改进建议**：
- 替换测试用例设计为真正验证兼容性的 3 个场景：
  1. 旧 JSON（无基类字段）→ 新 DTO + `@JsonIgnoreProperties` → 通过（字段填 null）
  2. 新 JSON（含基类字段）→ 旧代码（无 `AiRequestBase` 的旧 DTO）→ 通过（`ignoreUnknown = true`）
  3. 新 JSON（含基类字段）→ 新 DTO → 正确映射（字段值不丢失）
- 补充对 `@JsonCreator` + `@ConstructorProperties` 继承场景的兼容性说明：基类字段在 JSON 中的 key 名必须与子类构造器参数名匹配，否则 Jackson 无法注入

---

### 问题5：[重要] §3.2 LlmChatService 接口的流式(FunctionCall/ToolUse)路径未定义

**位置**：
- §3.2 `LlmChatService.structuredChat()` 职责描述
- §4.1 `doExecuteInternal()` 中 LLM 调用段

**问题描述**：
设计将 `structuredChat()` 定义为结构化输出入口，支持「模型原生 tool_use / function_call / JSON mode」。但未定义以下关键实现路径：

1. **tool_use/function_call 的 Tool 定义输入如何传递**：`LlmChatRequest` 仅包含 `messages` 和 `options`，没有 `tools`/`functions` 字段。若模型使用 tool_use，Tool 定义（JSON Schema）需通过 `options` Map 透传还是通过 `messages` 中的 SYSTEM 消息嵌入？两种方式对实现层面影响不同——前者需要 `LlmChatOptions` 增加 `tools: List<JsonNode>` 字段，后者需在模板渲染时嵌入。

2. **JSON mode 模式下模型的响应格式约束**：`structuredChat()` 通过 `Class<T>` 指定目标类型，但 JSON mode 的正确性依赖于模型能力。设计将「模型不支持 JSON mode」归类为 `StructuredOutputNotSupportedException`，但未定义此异常在实现层的检测时机——是启动期通过探测调用判断，还是运行期正常调用返回错误时发现？两种策略对容错设计的要求不同。

3. **结构化输出失败路径的 Token 浪费**：`structuredChat()` 失败后，回退到 `chat()` + `parse()`。但 `structuredChat()` 的失败调用（如 JSON mode 输出了格式正确的 JSON 但内容不符合 schema）已消耗了 prompt tokens + completion tokens，回退路径等同于发起新的完整 LLM 调用。设计未评估这种失败叠加是否会导致超时阈值被突破（嵌套超时的可能）。

**影响**：实现 structuredChat 的实现者需要在设计中无依据的两个选项之间自行决策，导致同一设计被不同实现者理解出不同的实现。

**严重程度**：重要

**改进建议**：
- 在 `LlmChatOptions` 或 `LlmChatRequest` 中补充 `tools: List<ToolDefinition>` 字段（可空），定义为结构化输出的 Tool Schema 输入；或明确要求通过 Prompt 模板的 SYSTEM message 嵌入 Tool 定义
- 定义 `StructuredOutputNotSupportedException` 的检测时机：采用**运行期首次检测 + 缓存结果**策略——首次调用 `structuredChat()` 返回不支持错误后，将该能力+模型的组合标记为不支持，后续请求直接走 `chat()` + `parse()` 路径，避免每次重复探测
- 在 §5.1 错误分类表中增加「结构化调用失败回退超时风险」行，标注 `capabilityTimeout` 需要考虑 `structuredChat` + `chat` 两阶段调用时间之和

---

### 问题6：[重要] `FallbackAiService` 迁移路径的 `applyStrategies()` 条件开关缺少 YAML 配置绑定定义

**位置**：
- §9.2 阶段二：`applyStrategies()` 迁移
- §9.5 YAML 配置

**问题描述**：
§9.2 阶段二中设计使用 `if (aiPlatformEnabled)` 条件开关隔离新旧降级路径：
```java
if (aiPlatformEnabled) {
    // v5+: 不做任何操作，降级由 CapabilityExecutor 管线处理
} else {
    applyStrategies(context);
}
```

但设计未定义 `aiPlatformEnabled` 字段的注入方式。`FallbackAiService` 现有代码（代码库 `FallbackAiService.java:43`）是普通的 `@Service`，未与 `AiPlatformConfig` 关联。设计未说明这个 boolean 字段如何从 YAML 的 `ai.platform.enabled` 绑定到 `FallbackAiService` 实例——通过 `@Value("${ai.platform.enabled}")` 注入、通过构造器参数传入、还是通过 `Environment` 运行时查询？

不同的绑定方式对测试和部署的影响不同：
- `@Value` 注入：需要在测试中设置 PropertySource，Spring Boot 集成测试自动处理
- 构造器参数：需要在 FallbackAiService 的 bean 构造时传入，而设计中的 §9.3 构造器迁移到 `ObjectProvider<AiService>` 未包含此参数
- `Environment` 运行时查询：每次调用时读取，与 `AiPlatformEnvironmentPostProcessor` 的转发时序存在先后依赖

**影响**：实现者在编写条件开关代码时缺乏明确指引，可能在 `FallbackAiService` 中通过 `@Autowired Environment` 直接读取属性，但未注意到 `EnvironmentPostProcessor` 写入的属性为最低优先级——若用户在 YAML 中同时设置了 `ai.platform.enabled=true` 和 `ai.mock.enabled=true`，转发逻辑因 `ai.mock.enabled` 已存在而不生效，导致两个开关同时为 true，两个 `AiService` 实现同时激活。

**严重程度**：重要

**改进建议**：
- 在 §9.2 阶段二中补充 `aiPlatformEnabled` 字段的注入方式：推荐使用 `@Value("${ai.platform.enabled:false}")` 构造器注入，与 `ObjectProvider<AiService>` 一同在构造器中注入
- 在 §9.5 配置说明中补充：若 `ai.platform.enabled=true` 且同时显式设置 `ai.mock.enabled=true`，`AiPlatformEnvironmentPostProcessor` 不会覆盖显式值，可能导致两个开关同时激活。应在配置文档中标注此风险，要求运维人员不要在启用底座时显式设置 mock.enabled

---

### 问题7：[中等] §2.1 目录结构中 `thin-adapter/` 子包存在但未在目录中体现

**位置**：
- §2.1 目录结构（`ai-impl/` 下无 `thin-adapter/` 条目）
- §2.2 模块依赖方向中引用了 `thin-adapter/` → Phase 4 模块

**问题描述**：
§2.2 依赖关系图中显示 `thin-adapter/` 作为 `ai-impl` 的子包存在依赖 Phase 4 业务模块的关系：
```
├── thin-adapter/ ──> Phase 4 业务服务模块
```

但 §2.1 的完整目录结构中，`ai-impl/` 下**无** `thin-adapter/` 子包。6 个薄适配器型 CapabilityExecutor（`DiagnosisCapabilityExecutor` 等）直接列在 `orchestrator/impl/` 下：
```
├── orchestrator/impl/
│   ├── ... (其他)
│   ├── DiagnosisCapabilityExecutor.java
│   ├── ... (各薄适配器)
```

这意味着薄适配器不是独立的 `thin-adapter/` 子包，而是 `orchestrator/impl/` 中的一部分。但 §2.2 的依赖规则中 `thin-adapter/` → Phase 4 的依赖方向需要物理路径支持。

**影响**：按 §2.1 目录结构创建项目后，发现 `thin-adapter/` 路径不存在。实现者需自行决定是将薄适配器抽出为独立子包（与 §2.1 矛盾）还是留在 `orchestrator/impl/` 下（与 §2.2 矛盾）。

**严重程度**：中等

**改进建议**：
- 方案 A（推荐）：在 §2.1 `ai-impl/src/main/java/.../` 下新增 `thin-adapter/` 子包，将 6 个薄适配器 CapabilityExecutor 移入此包，§2.2 依赖关系图保持有效
- 方案 B：删除 §2.2 中对 `thin-adapter/` 子包的引用，将薄适配器依赖约束说明放入 §3.1 薄适配器段落的 `thinAdapterTimeout` 或 `@ConditionalOnProperty` 保护说明中
- 两方案择一，确保 §2.1 与 §2.2 的一致

---

### 问题8：[中等] `StructuredOutputParser.parse()` 方法签名缺失 LLM 原始响应的结构化访问方式

**位置**：
- §3.6 `StructuredOutputParser` 接口定义
- §4.1 `doExecuteInternal()` 中 `parse()` 调用点（第 2372 行）

**问题描述**：
§3.6 将 `StructuredOutputParser` 的 `parse()` 方法签名定义为：
```
parse(LlmResponse response, Class<T> targetClass) → T
```

但设计中的 `LlmResponse` 已在 v17 替换为 `LlmChatResponse`。§4.1 伪代码中实际的调用方式为：
```
structuredOutputParser.parse(chatResponse, outputType)
```

其中 `chatResponse` 为 `LlmChatResponse` 类型。而 `LlmChatResponse` 包含 `content`（助手消息文本）、`usage`（Token 用量）、`modelId`、`retryCount` 四个字段。`parse()` 从 `chatResponse` 中提取文本进行解析，但方法签名使用了 `LlmResponse`（已不存在的旧类型名）而非 `LlmChatResponse`。§3.6 的接口定义未随 v17 的 LlmResponse→LlmChatResponse 重构同步更新。

**影响**：按 §3.6 定义编码的 `StructuredOutputParser` 接口与 §4.1 伪代码中的调用点不匹配，编译失败。

**严重程度**：中等

**改进建议**：
- 将 §3.6 `parse()` 方法签名中的 `LlmResponse` 更新为 `LlmChatResponse`，与 §1.3、§2.3、§3.2、§4.1 保持一致的命名
- 确认 §3.6 协作对象列表中的「被 `CapabilityExecutor` 在结果解析步骤中调用」引用的是 LLM 响应体而非结构化调用的完整返回值

---

### 问题9：[中等] 测试策略中缺少状态恢复路径和并发竞争验证

**位置**：
- §11 测试策略

**问题描述**：
§11 测试策略覆盖了各类降级/失败路径的触发条件验证，但缺少以下两类关键验证：

1. **状态恢复路径验证**：
   - 熔断器 OPEN → HALF_OPEN → CLOSED 的恢复路径（探测成功后熔断器状态正确回退）
   - `ModelEndpointHealthManager` 的 UNAVAILABLE → CONNECTED 恢复路径（`tryProbe()` 成功后的状态转换）
   - `CredentialProvider` 的 CACHE_ONLY/BACKOFF → NORMAL 恢复路径（Vault 恢复后连续失败计数器清零）
   - `PromptTemplate` 的状态回滚路径（DEPRECATED → ACTIVE 时另一个 ACTIVE 自动降级为 DEPRECATED）
   - 设计在 §3.2、§3.3、§3.8 中为这些组件定义了完整的状态机，但测试策略未要求验证状态机的回退路径

2. **并发竞争验证**：
   - `SlidingWindowMetricsStore` 写锁协议（`synchronized`）的并发正确性——多线程并发写入同一 Deque 时数据不丢失、统计值正确
   - `CircuitBreakerDegradationStrategy` 的 `AtomicReference` + CAS 在熔断窗口边界处的正确性——OPEN 到 HALF_OPEN 转换与请求进入的时序竞争
   - `CredentialProvider` Caffeine 缓存的多线程安全——并发读取 Vault 不可达状态下的缓存读写竞争

**影响**：实现者仅验证了降级触发条件就认为测试覆盖充分，但状态机无法回退的 Bug 要到生产环境连续运行数小时（Vault 故障恢复、模型端点抖动）后才暴露。

**严重程度**：中等

**改进建议**：
- 在 §11.1 单元测试模式中新增「状态恢复验证」子项：为每个含状态机的组件（CircuitBreakerDegradationStrategy、ModelEndpointHealthManager、CredentialProvider、PromptTemplate）编写状态机全路径遍历测试
- 在 §11.1 中新增「并发竞争验证」子项：为 SlidingWindowMetricsStore 编写多线程并发写入/读取测试（10 线程并发 1000 次记录），验证失败率统计值与预期窗口一致

---

## 整体质量评价

设计文档在技术细节上极为详尽（线程安全模型、序列化兼容性、迁移路径等均深度覆盖），**但存在 3 个层面的结构性缺口**：

1. **声明一致性**：多处「不变」声明被后续章节中的实质性变更否定，实现者无法建立对设计约束的可靠认知（问题1）
2. **架构完整性**：单实例假设贯穿设计，多实例部署场景下 3 个关键组件（滑动窗口、限流器、熔断器）的行为未定义（问题2）
3. **可实施性**：伪代码未区分新/旧 API 表面，实现者需要对照代码库逐一确认实施起点（问题3）

上述 3 个缺口均不属于内部审议已覆盖的「技术可行性」维度，而是在「需求响应充分度」和「整体深度与完整性」层面存在的问题。建议在进入编码阶段前优先修复问题 1~3。
