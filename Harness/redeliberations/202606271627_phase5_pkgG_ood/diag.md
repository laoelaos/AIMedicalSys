根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

基于对 v16 文档（`a_v16_copy_from_v15.md`）的逐节核查，识别出以下 v17 需要解决的问题。本轮审查覆盖 v15 遗留问题在 v16 中的修复状态评估 + v16 中新发现的设计缺口（含用户新提出的 LlmChatService 采纳需求）。

### 严重问题

**问题 1【严重，v15 遗留】：AiPlatformConfig 核心配置类缺失正式定义**

- 描述：v16 文档中 `AiPlatformConfig` 被引用 15+ 次（§2.1 目录结构、§3.1 Bean 装配策略、§3.1 降级策略注入机制、§3.1 EnvironmentPostProcessor 配置转发、§3.5 异步上下文传播、§3.8 策略自动注册抑制等），但 v16 仍未提供该类的正式类图、核心 @Bean 方法签名、@ConfigurationProperties 绑定前缀、EnvironmentPostProcessor 实现细节等。v15 审查提出此问题，v16 仅在 §3.1 段落文本中描述其职责，**未提供正式定义**。
- 位置：v16 §2.1 line 145（仅文件名）、§3.1 多处段落（line 584, 638, 645, 647, 652, 1314, 1724 等）、类图缺失
- 建议：在 v17 §2.3 类图中补充 `AiPlatformConfig` 类型（标注 `@Configuration`、`@ConfigurationProperties(prefix = "ai")`、`implements EnvironmentPostProcessor, ApplicationContextAware`），列出核心 @Bean 方法签名（`@Bean("llmCallExecutor")`、`@Bean("metricsAsyncExecutor")`、`@Bean("degradationStrategyMap")`、`@Bean("capabilityTimeoutConfig")` 等）；或在 §3 中新增 §3.9 AiPlatformConfig 专节，给出完整类定义与装配伪代码

**问题 2【严重，新增】：LlmClient 仍为扁平化设计，未采纳 v5 LlmChatService 的多轮消息/流式/结构化输出契约**

- 描述：v16 的 `LlmClient` 接口仅含一个同步方法 `invoke(LlmRequest request) → LlmResponse`，`LlmRequest` 字段为 `prompt: String` + `modelId: String` + `parameters: Map<String, Object>`。该扁平化设计缺失：
  1. **多轮对话支持**：无 `messages: List<LlmChatMessage>`（SYSTEM/USER/ASSISTANT 角色枚举），多轮对话上下文被迫由 CapabilityExecutor 通过 DB session 维护，复杂度下放
  2. **流式输出**：roadmap §6.4 明确"流式病历输出归 Phase 6"，但 Phase 5 真实接入 LLM 时若无 `LlmChatStreamService` 接口（返回 `Flux<LlmChatResponse>`），Phase 6 仍需返工引入 Reactor 依赖
  3. **结构化输出方法**：v16 把结构化输出责任外置为 `StructuredOutputParser` 接口，要求 LLM 返回 JSON 文本后再解析。当现代 LLM（GPT-4o/Claude 3/Qwen 等）原生支持 tool_use / function_call / JSON mode 时，v16 的"文本+解析"路径绕路且不通用
  4. **强类型 Options**：v16 的 `parameters: Map<String, Object>` 是弱类型，与 v5 的 `LlmChatOptions { modelId, temperature, maxTokens, stopSequences }` 相比丢失编译期类型保护
- 位置：v16 §1.3 line 49-52（LlmClient/LlmRequest/LlmResponse/TokenUsage）、§2.3 类图 line 277-300、§3.2 line 873-989
- 建议：采纳 v5 OOD（`Harness/redeliberations/202606271636_five_core_abilities_ood/a_v5_copy_from_v4.md`）中 §3.1 的 LlmChatService 设计。具体改造：
  - `LlmClient` → `LlmChatService`（含 `chat(LlmChatRequest)` + `structuredChat(LlmChatRequest, Class<T>)` 两个方法）
  - 新增独立 `LlmChatStreamService` 接口（`chatStream(LlmChatRequest) → Flux<LlmChatResponse>`，隔离 Reactor 依赖）
  - `LlmRequest` → `LlmChatRequest { messages: List<LlmChatMessage>, options: LlmChatOptions }`
  - 新增 `LlmChatMessage { role: LlmChatMessageRole, content: String }`
  - 新增 `LlmChatMessageRole enum { SYSTEM, USER, ASSISTANT }`
  - 新增 `LlmChatOptions { modelId, temperature, maxTokens, stopSequences }`
  - `StructuredOutputParser` 保留为"JSON 文本 → DTO"后备方案，但 `structuredChat` 优先走模型原生
  - `HttpApiLlmClient` → `HttpApiLlmChatService`；`SpringAiLlmClient` → `SpringAiLlmChatService`
  - CapabilityExecutor 调用方从 `llmClient.invoke(LlmRequest)` 改为 `llmChatService.chat(LlmChatRequest)`（构造时将 DTO 转为单条 USER message，system prompt 通过 PromptTemplateManager 渲染到 SYSTEM message）
  - §2.3 类图同步更新；§3.2 模型对接层章节重写；§9.2 YAML 配置同步更新
- 参考依据：v5 OOD §3.1 LlmChatService 完整设计（Harness/redeliberations/202606271636_five_core_abilities_ood/a_v5_copy_from_v4.md line 263-381）

### 重要问题

**问题 3【重要，v15 遗留】：LlmCallExecutor 与指标采集线程池的 Spring Bean 定义缺失**

- 描述：v16 §3.2 line 844 / §3.5 line 1049 / §6.1 line 1564 多处描述 LlmCallExecutor 与指标采集线程池（`metricsAsyncExecutor`），但**未给出任何 @Bean 定义伪代码**——在哪个 @Configuration 类中定义？Bean name 是什么？构造参数如何注入？指标采集线程池的 @Async 注解引用方式是什么？YAML 配置如何对应？v15 提出此问题，v16 未解决。
- 位置：v16 §3.2 line 844（LlmCallExecutor 描述）、§3.5 line 1049（指标线程池描述）、§6.1 line 1564
- 建议：与问题 1 合并修复——在 v17 §3.9 AiPlatformConfig 专节中给出两个线程池的 @Bean 定义伪代码：
  ```
  @Bean("llmCallExecutor")
  public Executor llmCallExecutor() {
      return new ThreadPoolExecutor(
          coreSize, 2 * coreSize, 60L, TimeUnit.SECONDS,
          new LinkedBlockingQueue<>(100),
          new ThreadPoolExecutor.CallerRunsPolicy());
  }
  
  @Bean("metricsAsyncExecutor")
  public Executor metricsAsyncExecutor() {
      ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
      executor.setCorePoolSize(2);
      executor.setMaxPoolSize(4);
      executor.setQueueCapacity(1000);
      executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
      executor.setThreadNamePrefix("metrics-async-");
      executor.initialize();
      return executor;
  }
  ```
  YAML 配置补充对应 `ai.platform.executor.llm-call.*` 与 `ai.platform.executor.metrics-async.*` 配置块；AiMetricsCollector 实现类标注 `@Async("metricsAsyncExecutor")` 引用方式

**问题 4【重要，v15 遗留】：AiOrchestrator.handle() 与 AiService 13 方法的映射关系未显式定义**

- 描述：v16 §4.1 line 1349 定义了 `AiOrchestrator.handle(capabilityId, request)` 通用方法，但 §2.3 类图 lines 200-208 列出 13 个 AiService 具体方法（triage/prescriptionCheck/generateMedicalRecord/...），v16 §3.1 line 564 的能力标识映射表描述了方法到 capabilityId 的对应关系，但**未显式说明 13 个 AiService 方法如何委托到 handle()**。读者必须自行推断"每个 AiService 方法的实现都是 `return handle("TRIAGE", request)`"的模式。v15 提出此问题，v16 未解决。
- 位置：v16 §4.1 line 1349 (handle 伪代码) vs §2.3 类图 line 200-208 (13 个方法) vs §3.1 line 564 (capabilityId 映射表)
- 建议：在 v17 §4.1 handle() 伪代码前新增注释块，显式表述委托关系：
  ```
  // AiOrchestrator 的 13 个 AiService 方法实现遵循统一委托模式：
  //   @Override public CompletableFuture<AiResult<TriageResponse>> triage(TriageRequest req) {
  //       return handle("TRIAGE", req);
  //   }
  //   @Override public CompletableFuture<AiResult<PrescriptionCheckResponse>> prescriptionCheck(...) {
  //       return handle("RX_AUDIT", req);
  //   }
  //   ... 其余 11 个方法同理
  // handle() 是 AiOrchestrator 的唯一实际执行入口，13 个 AiService 方法仅为
  // 委托入口与方法签名实现。
  ```
  或新增完整委托示例伪代码（以 triage() 为例展示完整链路）

**问题 5【重要，v15 遗留】：薄适配器 CapabilityExecutor doExecuteInternal() 的行为契约仅存在于 §3.1 而非 §4.1**

- 描述：v16 §4.1 的 doExecuteInternal() 伪代码（lines 1608-1684）仅展示完整管线（7 项底座能力）的实现，薄适配器子类（6 项 Phase 4 能力）的简化和特化行为仅出现在 §3.1 文本描述（line 742-794）。v15 提出此问题，v16 未解决。
- 位置：v16 §4.1 完整管线伪代码 vs §3.1 薄适配器文本描述
- 建议：在 v17 §4.1 doExecuteInternal() 伪代码之后（或新增 §4.2 薄适配器管线）补充薄适配器子类的特化版伪代码，覆盖：
  1. 公共 ForkJoinPool 而非 llmCallExecutor（避免嵌套提交到同一线程池）
  2. 独立 thinAdapterTimeout 超时控制（thinAdapterTimeout: Duration，默认 60s）
  3. Phase 4 业务异常（catch BusinessException）包装为 `AiResult.failure(bizErrorCode)` 返回
  4. 基础设施异常（catch Exception）记录 `DegradationReason.INFRASTRUCTURE_ERROR` 进入降级
  5. retryCount=0 限制注释（薄适配器不重试，重试由 Phase 4 服务内部控制）

**问题 6【重要，v15 遗留】：AiOrchestrator 持有 ModelEndpointHealthManager 但 handle() 伪代码未使用**

- 描述：v16 §2.3 类图 line 204 与 §3.1 协作对象列表将 `ModelEndpointHealthManager` 列为 AiOrchestrator 的字段，但在 §4.1 handle() 伪代码（lines 1349-1560）中未做任何调用，形成**死字段**。v15 提出此问题，v16 未解决。
- 位置：v16 §2.3 line 204 vs §4.1 handle() lines 1349-1560 vs §4.1 doExecuteInternal() lines 1637-1640
- 建议：v17 二选一：
  - **方案 A（推荐）**：从 AiOrchestrator 类图和协作对象列表移除 `ModelEndpointHealthManager` 字段；将端点健康检查逻辑下沉到 `CapabilityExecutor.doExecuteInternal()` 中（模型路由后、LLM 调用前调用 `endpointHealthManager.getState()`），使 AiOrchestrator 保持"纯路由委托"职责单一
  - **方案 B**：在 handle() 伪代码中补充使用场景——在 handle() 入口处对 capabilityId 对应的所有 endpointId 做批量健康检查，若全部 UNAVAILABLE 则跳过 CapabilityExecutor 直接降级；但此方案会引入 AiOrchestrator 的"预判逻辑"，与"纯路由"职责冲突

### 一般问题

**问题 7【一般，v15 遗留】：extractHeader() 工具方法命名与薄适配器使用的 extractFromRequestContext() 不一致**

- 描述：v16 §3.1 薄适配器伪代码 line 712, 716, 720 统一使用 `extractFromRequestContext(String headerName)` 方法，但 §4.1 handle() catch 块（lines 1549-1552）Phase 4 DTO 兼容提取路径仍调用未定义的 `extractHeader()` 方法。v15 提出此问题，v16 未解决。
- 位置：v16 §4.1 lines 1549-1552 vs §3.1 line 712-720
- 建议：v17 §4.1 统一为 `extractFromRequestContext(String headerName)`，并在 §3.5 工具方法段（或新增 §3.10）中给出默认实现：
  ```
  protected String extractFromRequestContext(String headerName) {
      ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attrs == null) return null;
      return attrs.getRequest().getHeader(headerName);
  }
  ```

## 历史迭代回顾

### 已解决的问题（出现在历史反馈但当前反馈中不再提及）
- 类图缺失（第1轮）→ v2 补充完整 Mermaid 类图
- 状态模型不足（第1轮）→ v2 补充各组件状态模型
- Bean 装配二义性（第1轮）→ v3 使用 @Primary + ObjectProvider
- execute() 返回类型问题（第2轮/第5轮）→ v5 改为 CompletableFuture 包装
- 降级预检位置问题（第10轮/第11轮）→ v13 移至 supplyAsync 之前
- 异步上下文传播（第8轮/第11轮）→ v11 采用入口处提取 + 闭包捕获
- 数字工厂方法缺失（第3轮/第5轮）→ 历次迭代已解决
- 防御性拷贝（第9轮/第14轮）→ v14 修正变量重赋值编译错误
- Maven 作用域矛盾（第12轮/第14轮）→ v15 统一为 provided
- retryCount 字段未定义（第11轮）→ v14 补充
- Phase 4 DTO 过渡策略矛盾（第9轮/第12轮）→ v12 统一提取策略
- CallerRunsPolicy 导致 LlmCallExecutor 线程池饥饿（第6轮）→ v15 改为 DiscardPolicy + 独立 metricsAsyncExecutor

### 持续存在的问题（在多轮反馈中反复出现）
- **薄适配器 CapabilityExecutor 的行为描述不足**：第4轮/第7轮/第10轮/第11轮/第12轮/第13轮/第14轮/第15轮 → 当前 v17 问题 5 是同一脉络的延续
- **AiOrchestrator 与组件的协作关系不一致**：第4轮/第7轮/第13轮/第15轮 → 当前问题 6 是同一脉络的延续
- **AiPlatformConfig 类定义缺失**：第15轮首次提出 → 当前问题 1 仍未解决
- **LlmCallExecutor / 线程池 Bean 定义缺失**：第15轮首次提出 → 当前问题 3 仍未解决

### 新发现的问题（本轮 v17 新识别）
- 问题 2（LlmClient 扁平化设计缺多轮/流式/结构化输出契约）—— v5 OOD 已完整设计 LlmChatService，v16 未采纳。用户审查 v16 时识别此缺口，要求采纳 v5 设计
- 问题 4（handle() 映射桥接说明缺失）—— 第15轮首次提出，v16 未显式补充

## 用户补充需求

采纳 v5 OOD（`Harness/redeliberations/202606271636_five_core_abilities_ood/a_v5_copy_from_v4.md`）中 LlmChatService 的完整设计：
- `LlmChatService` 接口（含 chat + structuredChat）
- `LlmChatStreamService` 接口（独立，Flux 流式）
- `LlmChatRequest { messages, options }`
- `LlmChatMessage { role, content }`
- `LlmChatMessageRole enum { SYSTEM, USER, ASSISTANT }`
- `LlmChatOptions { modelId, temperature, maxTokens, stopSequences }`

替换 v16 中的 `LlmClient` + `LlmRequest` 扁平化设计。详细改造方案见问题 2。

## 上一轮产出路径
`C:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606271627_phase5_pkgG_ood/a_v16_copy_from_v15.md`

## 用户原始需求
`C:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606271627_phase5_pkgG_ood/requirement.md`