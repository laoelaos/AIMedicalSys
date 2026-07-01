# 质量审查报告（v2） — a_v23_design_v3.md

审查执行时间：第 23 轮迭代
审查范围：需求响应充分度、事实/逻辑正确性、深度与完整性、落地可行性
版本说明：本报告为 v1 的修订版本，已根据 v1 质询意见逐条回应（见尾部「修订说明」）

---

## 问题1：成功路径指标记录的控制流间接性导致维护脆弱性

- **问题描述**：§4.1 标准 `doExecuteInternal()` 伪代码中，structuredChat 成功路径（行 3235~3240）和 chat 回退成功路径（行 3279~3297）均通过控制流 fall-through 到达行 3341~3351 的共用成功处理段执行 `metricsCollector.record()`、`slidingWindowMetricsStore.recordSuccess()` 和 `return AiResult.success(parsedResult)`。**功能完整性无问题**——指标记录和返回逻辑确实存在、可被两条成功路径到达。但此控制流模式存在以下维护风险：
  - 两个成功路径的 if 块末尾均未显式标注 `// fall-through to shared success handler` 或类似注释，后续维护者可能误以为代码"坠落"无返回值
  - 若后续在某条成功路径中直接添加 `return AiResult.success(parsedResult)`（常见重构模式），将绕过共用段，导致另一条路径的指标记录被遗漏
  - 共用段依赖对嵌套 try-catch 结构的完整理解才能判断可达性，新手维护者容易误判
- **所在位置**：§4.1 `doExecuteInternal()` 伪代码，结构化 chat 成功分支（行 3235~3240）、chat 回退成功分支（行 3279~3297）、共用成功处理段（行 3341~3351）
- **严重程度**：一般（功能完整性不受影响，属可维护性改进）
- **改进建议**：在两条成功路径的 if 块末尾添加明确注释 `// fall-through to shared success handler at lines 3341-3351`；或提取共用段为辅助方法（如 `recordSuccessAndReturn(parsedResult, modelRoute, retryCount, tokenUsage, ...)`）并在两条成功路径末尾显式调用，消除隐式控制流依赖。

---

## 问题2：degradationStrategyMap 热加载机制与构造器注入方式不一致，热替换无法生效

- **问题描述**：§3.9（行 2428~2429）明确规定 `degradation.strategies` 支持运行时热加载——"自定义定时刷新 + 策略列表热替换"，通过 `refreshDegradationStrategies()` 方法"替换 `Map<String, List<DegradationStrategy>>` 实例后通过 `AtomicReference` 发布"。然而 §3.1（行 928~934）明确 CapabilityExecutor 通过构造器注入 `Map<String, List<DegradationStrategy>> degradationStrategyMap`，该 Map 引用在 Bean 初始化时一次性捕获为实例字段（行 1341: `this.degradationStrategyMap = degradationStrategyMap`）。即使 `AiPlatformConfig` 通过 `AtomicReference` 重建了新 Map 实例，已有 CapabilityExecutor 实例持有的 `this.degradationStrategyMap` 仍然指向旧 Map。两处描述自相矛盾：热加载机制声称可运行时替换，但注入机制决定了替换无法生效。
- **所在位置**：§3.1 降级策略注入机制（行 928~934）及构造器伪代码（行 1341）；§3.9 运行时配置热加载机制表 degradation.strategies 行（行 2428~2429）
- **严重程度**：重要
- **改进建议**：统一为一种可实现的设计。推荐以下方案：
  - **方案 A（推荐）**：CapabilityExecutor 改为从 `ObjectProvider<Map<String, List<DegradationStrategy>>>` 或 `AtomicReference<Map<String, List<DegradationStrategy>>>` 获取最新 Map，在每次 `execute()` 调用时通过 `get()` 读取当前引用，使热刷新即时生效。构造器注入 `Map` 的一次性捕获模式与 `AtomicReference` 发布模式不兼容，建议二选一。
  - **方案 B**：放弃 `degradation.strategies` 的运行态热刷新，改为重启生效，在 §9.5 注释中标注为 `[静态·重启生效]`。
  - **方案 C（过渡）**：接受当前启动期一次性绑定的现状，移除 §3.9 中关于 `degradation.strategies` 热加载的描述与实际伪代码，避免误导实现者。

---

## 问题3：`estimateTokens()` 的 jtokkit 精确 Tokenizer 分支缺少实现细节

- **问题描述**：§4.1 `DiscussionConclusionCapabilityExecutor.doExecuteInternal()` 特化伪代码中引入了精确 Tokenizer 分支（行 3445~3448 `if tokenizerAvailable: preciseTokenCount(transcripts)`），但存在以下缺口：
  - `tokenizerAvailable` 判定变量的定义与赋值方式未给出——它应作为类字段在 `@PostConstruct` 中通过 `try Class.forName("com.knuddels.jtokkit.Encodings")` 检测，或通过 `@ConditionalOnClass` 实现条件化注册？当前伪代码直接引用未声明变量，实现者无从判断如何初始化
  - `preciseTokenCount()` 的方法签名和行为契约未定义——输入参数是 `List<DiscussionTranscript>` 还是 `String`，返回值类型是 `int` 还是 `long`
  - §8 Maven 依赖清单中未列出 jtokkit（或任何 tiktoken 的 JVM 实现），实现者需自行发现和引入此可选依赖
- **所在位置**：§4.1 `DiscussionConclusionCapabilityExecutor` 特化伪代码（行 3445~3451）；§8 Maven 依赖清单（未包含 jtokkit 条目）
- **严重程度**：一般
- **改进建议**：在 §4.1 伪代码中补充 `tokenizerAvailable` 的判定逻辑（如 `try { Encodings.newInstance() }` 或 `Class.forName`），为 `preciseTokenCount()` 补充方法签名和返回值契约。在 §8 依赖清单的"条件性依赖"段中新增 jtokkit 条目（`optional = true`），明确说明其在 `doExecuteInternal()` 中的用途。

---

## 需求响应充分度评估

用户需求要求：(1) 参考已有 Phase0/Phase1ABD OOD 保持风格和结构一致性；(2) 完成 Phase5 包G 的完整 OOD 设计；(3) 覆盖类图、核心职责、协作关系、关键接口、状态模型等 OOD 核心要素。

经审查：
- **风格一致性**：§1.2 明确列出 5 条具体规则（四元组格式、缩进式伪代码、四列表格等），可追溯至 Phase0/Phase1ABD 的设计范式。通过抽样对比，格式和术语体系确实对齐。✅
- **类图**：§2.3 以完整的 Mermaid classDiagram 覆盖全部 13 项能力的类层次关系（`AiService` → `FallbackAiService` → `AiOrchestrator` → `CapabilityExecutor` → `AbstractCapabilityExecutor` → 7 底座 + 6 薄适配器子类），以及 `ModelRouter`、`LlmChatService`、`PromptTemplateManager`、`ExperimentManager`、`AiMetricsCollector`、`CredentialProvider` 等 37+ 个类的属性、方法和关联线。✅
- **核心职责**：§1.3 核心抽象一览表覆盖全部 38 个核心抽象；§3.x 每个抽象以角色/职责/协作对象/类型形态选择理由四元组展开。✅
- **协作关系**：§3.x 每个抽象标注协作对象；§2.2 模块依赖方向图；§10 与其他包的协作边界。✅
- **关键接口**：§1.6 API Surface 状态表标注所有新增/变更接口的优先级和依赖前提；§3.x 对每个核心接口给出完整方法签名与行为契约。✅
- **状态模型**：各组件状态机分散但完整覆盖——`ModelEndpointHealthManager`（§3.2，CONNECTED/DEGRADED/UNAVAILABLE）、`CredentialProvider`（§3.5，NORMAL/CACHE_ONLY/BACKOFF）、`PromptTemplate`（§3.3，DRAFT/ACTIVE/DEPRECATED）、`Experiment`（§3.4，DRAFT/ACTIVE/PAUSED/COMPLETED）、`CircuitBreakerDegradationStrategy`（§3.8，CLOSED/OPEN/HALF_OPEN）。✅
- **整体深度**：§1.8 非功能性质量分析（冷启动、连接池压力、内存占用、启动时间）的量化深度表现出色。§4 行为契约伪代码对异常/边界场景覆盖完备。

**结论**：产出的需求响应充分度满足要求，全部 OOD 核心要素已覆盖且深度可支撑编码实施。

---

## 整体评价

产出在历经 25 轮迭代修正后，**整体质量高**，是经过充分打磨的可交付级 OOD 设计文档。需求响应完整（全部 OOD 要素覆盖）、结构一致性达标（与 Phase0/Phase1ABD 对齐）、§1.8 非功能性分析和 §4 伪代码的深度达到直接指导编码的程度。

遗留的主要质量风险集中在一处 **重要级** 问题（问题2：degradationStrategyMap 热加载声明与注入机制矛盾），该问题若按当前文档两套描述实施，热刷新将静默失效。其余为可维护性改进（问题1）和接口规范补齐（问题3），不阻塞编码实施。

---

## 修订说明（v2）

| 质询意见 | 回应 |
|---------|------|
| **问题1：成功路径缺失指标记录与返回——事实误判** | 已接受。重新审查控制流后确认行 3341~3351 的共用成功处理段确实通过 fall-through 被两条成功路径到达。已修正描述为"控制流间接性导致维护脆弱性"，严重程度从「严重」降为「一般」。 |
| **问题3：文档版本号不一致——违反审查指令** | 已接受。按质量审查指令"不要关注文档校对、版本号等细节"的要求，将问题3从报告中删除。 |
| **问题4：TimeoutException 分析——已验证正确不应列为问题** | 已接受。将问题4从问题列表中移除。已验证正确的设计不应混入待修复清单。 |
| **缺失需求响应充分度评估** | 已补充。在修订版中新增了「需求响应充分度评估」章节，逐项评估类图、核心职责、协作关系、关键接口、状态模型、风格一致性的覆盖情况。 |
