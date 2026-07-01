# Phase 5 包 G OOD 设计文档 质量审查报告

## 审查概况

- **审查目标**：`a_v27_copy_from_v26.md`（v27，自 v26 复制后应用 13 项修订）
- **审查视角**：需求响应充分度、整体深度与完整性、实施就绪度
- **审查范围限定**：侧重内部审议（技术可行性）未充分覆盖的维度，避免重复验证已确认项
- **产出状态**：经过 26 轮迭代审查后形成的稳定版本，整体成熟度较高

---

## 审查发现

### 问题 1（重要）：`DiscussionConclusionCapabilityExecutor` 构造器未完整定义

- **问题描述**：该能力执行器需要额外的 `compressionLightweightEndpoint`（`@Value("${ai.compression.lightweight-endpoint}")`）和 `compressionLightweightClientType`（`@Value("${ai.compression.lightweight-client-type:HTTP_API}")`）两个配置参数，但文档未给出其完整的构造器签名和注入方式。相比之下，薄适配器 `DiagnosisCapabilityExecutor` 在 §3.1 中展示了完整的 16 参数构造器示例，并声明"其余 5 个薄适配器子类构造器按此模式实现"。`DiscussionConclusionCapabilityExecutor` 作为 7 项底座能力中唯一有额外构造参数的特殊执行器，缺少同等级别的构造器定义，实现者需自行推断 `super()` 调用中 `transcriptSummaryTimeout`、`compressionLightweightEndpoint`、`compressionLightweightClientType` 参数的传递方式。

- **所在位置**：§3.11.7 特化设计表（整个节无构造器签名）；§4.1 行 3583~3587（仅注释提及 `@Value` 注入，无完整签名）

- **改进建议**：在 §3.11.7 末尾或 §3.1 中补充 `DiscussionConclusionCapabilityExecutor` 的完整构造器签名和 `super()` 调用示例，包括 `@Value` 注入的 `transcriptSummaryTimeout`、`compressionLightweightEndpoint`、`compressionLightweightClientType` 参数的显式声明，以及这些参数在 YAML 中的对应配置路径。同时在 §2.3 类图的 `DiscussionConclusionCapabilityExecutor` 节点中补充其特有字段声明。

---

### 问题 2（重要）：`executeStandardPipeline()` 抽象定义与伪代码实现之间不一致

- **问题描述**：§3.1 定义了 `protected final executeStandardPipeline()` 作为子类复用标准管线的机制（解决 v26 中 `super.doExecuteInternal()` 调用 abstract 方法的编译错误），但 §4.1 `DiscussionConclusionCapabilityExecutor.doExecuteInternal()` 伪代码（行 3628~3652）在注释中声称"复用标准流程"之后，实际重新实现了实验分流和模板渲染两个步骤的完整伪代码，并仅以注释"后续步骤复用父类标准流程"说明余下步骤。这种"半展开"的伪代码让实现者面临两难：是直接调用 `executeStandardPipeline()`（简洁但无法在伪代码中展示），还是按照展开的伪代码重新实现实验分流和模板渲染（但这样做就绕过了 `executeStandardPipeline()` 的设计意图）。文档未明确说明设计预期。

- **所在位置**：§3.1 行 1412~1421（`executeStandardPipeline()` 定义） vs §4.1 行 3621~3652（伪代码使用方式）

- **改进建议**：方案 A：将伪代码改为直接调用 `executeStandardPipeline(variables, ...)` 并给出完整调用示例，删除实验中转步骤的展开代码。方案 B：保留展开版本，但在设计注释中明确说明此处是出于文档可读性目的展开而非要求子类重新实现，并标注实际实现时需替换为单行 `executeStandardPipeline()` 调用。

---

### 问题 3（重要）：`transcriptSummaryExecutor` 的 `CallerRunsPolicy` 与线程池隔离目的在语义上存在矛盾

- **问题描述**：`transcriptSummaryExecutor` 的设计初衷是将讨论结论的前置压缩调用从 `llmCallExecutor` 线程池隔离出去（避免压缩调用阻塞其他能力的 LLM 调用），但拒绝策略配置为 `CallerRunsPolicy`。当队列满（容量 20）时，`CallerRunsPolicy` 将压缩任务回退到提交线程——即 `llmCallExecutor` 的 Worker 线程。这意味着隔离设计在队列满时完全失效：压缩任务仍会在 `llmCallExecutor` 线程上同步执行，且此时队列已满意味着系统处于高负载状态，Worker 线程的阻塞将进一步加剧线程池饥饿。文档在 §3.9 行 2801~2806 的注释"自然背压，压缩失败时降级为截断文本"中的"压缩失败"语义模糊——`CallerRunsPolicy` 本身不会导致任务失败，因此在 `compressedFuture.get()` 的 catch 块中捕获到的 `Exception` 并非因队列满而产生，真正的队列满场景反而静默绕过了隔离。

- **所在位置**：§3.9 行 2801~2806（`transcriptSummaryExecutor` Bean 定义及注释）；§4.1 行 3602~3610（压缩调用超时捕获逻辑）

- **改进建议**：方案 A（推荐）：将拒绝策略改为 `DiscardPolicy` + WARN 日志，压缩任务被丢弃时在 catch 块的回退逻辑中已处理截断文本行为（行 3609），不需要 `CallerRunsPolicy` 的"背压"。此方案确保无论队列是否满，压缩调用都不会在 `llmCallExecutor` 线程上执行。方案 B：若保留 `CallerRunsPolicy`，需在文档中显式说明队列满时的行为退化为"在 `llmCallExecutor` 线程同步执行压缩"，并评估此退化对高并发场景下的影响。

---

### 问题 4（中等）：13 项能力 DTO 的 4 个公共字段（`visitId`/`patientId`/`sessionId`/`departmentId`）的调用方填充责任未明确定义

- **问题描述**：§3.5 `AiRequestBase` 定义 7 项底座能力 DTO 需继承并携带 4 个公共字段，§3.10 定义了 HTTP Header 映射（`X-Department-ID` 等），§3.1 非 HTTP 场景下通过 MQ/定时任务调用方填充。但文档未明确定义这些字段在**当前各业务模块作为 `AiService` 调用方时**的实际填充来源——当前 Phase 2~4 调用方在调用 `triage()` 等方法时传入的 DTO 中这些字段为新增字段，文档未说明调用方应从何处获取这些就诊上下文字段（从 HTTP Request？从 Session？从 Token Claims？），也未说明过渡期内这些字段为空时底座的行为。这导致业务模块在底座切流时需要自行推断填充逻辑。

- **所在位置**：§3.5「AiRequestBase」过渡策略（行 2276~2305）；§3.10「调用方 Header 契约」表（行 2937~2942）；§3.1「非 HTTP 场景的上下文提取路径」（行 2945~2950）

- **改进建议**：新增一个独立的"调用方数据准备指引"小节（或子节），集中说明：(1) 现有业务模块在调用 `AiService` 各方法时，`AiRequestBase` 继承字段的推荐填充方式（HTTP 场景从 Header 提取/通过 `RequestContextUtils` 工具类填充；非 HTTP 场景从业务上下文中提取）；(2) 7 项底座能力 DTO 的 4 个公共字段在切流过渡期内为空时的底座行为（底座不会因此降级/报错，AiCallRecord 中字段为空）；(3) 现有业务模块**无需修改**代码即可兼容新增字段（通过 `@JsonIgnoreProperties(ignoreUnknown = true)` 保护）。

---

### 问题 5（中等）：熔断器-端点健康管理器统一探测机制的潜在全量等待风险

- **问题描述**：§3.2 设计了熔断器 `CircuitBreakerDegradationStrategy` 与 `ModelEndpointHealthManager` 的统一探测机制——HALF_OPEN 状态下熔断器通过 `ModelEndpointHealthManager.tryProbe()` 判断是否允许探测。但当某能力标识对应的 `ModelRouter.route()` 返回的 `ModelRoute` 指向多个 endpointId（多条路由）时，统一探测决策表中（行 1722~1732）仅以单一 endpointId 为判断维度，未定义"熔断器状态为 OPEN 但该能力有多个可用端点"场景的处理——一个端点 UNAVAILABLE 不应阻止熔断器在另一个端点上执行探测。文档中"统一探测决策表"仅列出单一熔断状态+单一端点健康状态的组合，缺少"多端点"维度的覆盖。

- **所在位置**：§3.2「统一探测决策表」（行 1720~1732）；§3.8 `CircuitBreakerDegradationStrategy` 状态模型（行 2593~2603）

- **改进建议**：在统一探测决策表或 §3.8 中补充多端点场景的处理规则：(1) 熔断器状态绑定的对象应明确——是按 `capabilityId` 还是按 `endpointId`？(2) 若一个能力有多个路由端点，其中部分端点 UNAVAILABLE，熔断器 OPEN 判定是否应只针对特定端点而非全局能力；(3) 补充规则说明熔断器状态的作用域粒度，避免实现者错误地将 endpoint 级别不可用升级为能力级别的全局熔断。

---

### 问题 6（轻微）：`DiscussionConclusionCapabilityExecutor` 前置压缩的 `compressionLightweightEndpoint` YAML 配置项未提供默认实例

- **问题描述**：§9.5 YAML 中 `ai.compression.lightweight-endpoint` 配置值设为 `"compress-default"`（行 4301），但 §4.1 行 3584 的 `@Value("${ai.compression.lightweight-endpoint}")` 没有提供 `:` 默认值语法。若 YAML 中该配置缺失（如初始部署时被省略），Spring 启动期将因配置缺失抛出 `IllegalArgumentException`，导致底座失败。而该配置仅影响讨论结论能力的前置压缩，不应导致整个底座不可用。

- **所在位置**：§4.1 行 3584（`@Value` 无默认值）；§9.5 行 4296~4302（YAML 配置项）；类图未展示 `compressionLightweightEndpoint` 字段

- **改进建议**：将 `@Value("${ai.compression.lightweight-endpoint}")` 改为 `@Value("${ai.compression.lightweight-endpoint:compress-default}")`，使配置缺失时自动使用默认值。同时在 §2.3 类图的 `DiscussionConclusionCapabilityExecutor` 节点中补充该字段定义（当前类图中为空类，行 501~502）。

---

## 整体质量评价

该文档经过 26 轮迭代审查，整体成熟度高：OOD 核心要素（类图、核心职责、协作关系、关键接口、状态模型）覆盖完整，伪代码详实，YAML 配置完整，测试策略覆盖全面（含并发竞争和配置热刷新测试）。从需求响应充分度看，已充分响应"参考 Phase0/Phase1ABD 设计风格一致性"和"完成完整 OOD 设计"两项要求。

发现的 6 个问题均为新迭代版本（v27）中引入或迭代审查未充分覆盖的维度，不影响文档整体可用性但需要在实施前确认。
