# Phase 5 包 G OOD 设计文档质量审查报告（v13 外部审议）— 修订版

## 审查范围与视角

- 审查对象：Phase5 包G AI进阶底座 架构级OOD设计方案（文档标题 v15，文件名 a_v13_copy_from_v12）
- 审查轮次：第 13 次迭代（外部审议）
- 审查维度：需求响应充分度、事实正确性、逻辑一致性、深度与完整性（落地视角）
- 注意：内部审议（组件A的设计-验证循环）已覆盖技术可行性、类图完整性、线程安全等维度，本审查侧重上述未充分覆盖的角度

---

## 发现的问题

### 问题1：AiOrchestrator.handle() 的 catch 块中薄适配器场景的就诊上下文提取失效

- **问题描述**：handle() 的 catch 块（§4.1 伪代码第 1465-1474 行）通过 `request instanceof AiRequestBase` 判断并提取 `departmentId`/`visitId`/`patientId`/`sessionId`。但根据 §3.5 过渡策略，6 项 Phase 4 薄适配器的请求 DTO 暂不继承 `AiRequestBase`。当 `AbstractCapabilityExecutor.execute()` 模板方法外层步骤（防御性拷贝 `ObjectMapper.convertValue()`、`inputSummary` 预计算 `request.toString()` 等，第 1498-1501 行）抛出未预期异常并传播至 `handle()` 时，catch 块中的 `instanceof` 检查将为 false，导致这些就诊上下文字段全部传入 null。此场景与 Problem 4 所述 `doExecuteInternal()` 内异常已被捕获的场景互不重叠——Problem 4 覆盖的是 `doExecuteInternal()` 内部的异常捕获路径，而本问题覆盖的是 `execute()` 外层步骤（`supplyAsync` 之前）的异常传播路径。
- **所在位置**：§4.1 AiOrchestrator.handle() 伪代码第 1465-1474 行；§4.1 AbstractCapabilityExecutor.execute() 第 1498-1501 行
- **严重程度**：中等（外层步骤异常概率较低，但命中后丢失所有就诊上下文）
- **改进建议**：在 catch 块中增加对 Phase 4 DTO 的兼容提取路径：当 `request instanceof AiRequestBase` 为 false 时，通过 `RequestContextHolder` / HTTP Header 独立提取 `departmentId`/`visitId`/`patientId`/`sessionId`（与薄适配器 `doExtractDepartmentId()` 等方法的策略一致），或要求薄适配器 CapabilityExecutor 在最外层 catch 中自行降级兜底（确保不向 `handle()` 传播未预期异常）。

---

### 问题2：ParseFailure 降级路径丢失原始 LLM 响应摘要

- **问题描述**：在 `doExecuteInternal()` 中若 `structuredOutputParser.parse()` 抛出 `ParseException`（第 1568 行），代码直接进入 `doDegrade()` 降级路径。此时 `outputSummary` 已在第 1564 行赋值为 `StringUtils.truncate(llmResponse.getText(), 500)`（原始 LLM 响应文本摘要），但此值未传入 `doDegrade()`——`doDegrade()` 方法签名（第 1584 行）不含 `outputSummary` 参数。在无 `LocalRuleFallback` 场景下，`doDegrade()` 内部对 `outputSummary` 传 null（第 1597 行），导致 AiCallRecord 中该条降级记录的 `outputSummary` 为空。在"LLM 实际返回了合法文本但格式不符"的解析失败场景下，输出摘要丢失对实验分析和调试有负面影响。
- **所在位置**：§4.1 doExecuteInternal() 第 1564-1569 行，doDegrade() 第 1584-1601 行
- **严重程度**：中等（不影响主流程，但降低可观测性）
- **改进建议**：在 `doDegrade()` 签名中增加可选的 `String outputSummary` 参数，或改为在 parse() 失败后、调用 doDegrade() 之前先将 `outputSummary` 记录到一个局部变量并传入工厂方法。具体路径：修改 `doDegrade()` 签名为 `doDegrade(..., String inputSummary, String outputSummary, Integer promptVersion)`，解析失败降级时传入 LLM 原始响应的摘要。

---

### 问题3：Prompt 模板渲染契约未定义"指定版本已废弃"的回退行为

- **问题描述**：§4.4 模板渲染契约第 1636-1637 行规定："`promptVersion` 不为 null 且有对应版本 → 使用指定版本模板；`promptVersion` 为 null 或无对应版本 → 退回到当前 ACTIVE 模板检索策略"。但未覆盖 "`promptVersion` 对应版本已处于 DEPRECATED 状态" 的场景。A/B 实验的实验配置可能引用一个已被后续实验废弃的 Prompt 版本号，此时 `DatabasePromptTemplateManager` 的行为未冻结——是找到已废弃版本继续渲染，还是跳过该版本回退到 ACTIVE 模板？两种行为将产生不同的实验结果，影响实验分析的准确性。
- **所在位置**：§4.4 Prompt 模板渲染契约第 1636-1637 行
- **严重程度**：中等（运行时路径不同可能导致实验结果偏差）
- **改进建议**：在渲染契约第 2 步与第 3 步之间增加一条规则："若 `promptVersion` 对应版本存在但其 `status = DEPRECATED`，按"版本不存在"处理——输出 WARN 日志并回退到当前 ACTIVE 模板检索策略"。同时在 §3.3 PromptTemplate 状态模型中明确标注此行为。

---

### 问题4：薄适配器 doExecuteInternal() 中 ExecutionException 包裹的原始异常类型丢失

- **问题描述**：薄适配器 `doExecuteInternal()` 伪代码（第 730-754 行区域）使用 `CompletableFuture.supplyAsync()` + `future.get(timeout)` 包裹 Phase 4 委托调用。catch 块分别捕获 `TimeoutException`、`BusinessException`、`Exception`。当 Phase 4 委托调用抛出未预期的 `RuntimeException`（如 NPE、`IndexOutOfBoundsException`、序列化异常等），它将被 Java 的 `CompletableFuture` 包装为 `ExecutionException`，然后被 `catch (Exception e)` 捕获。此时降级原因记录的是 `DegradationReason.INFRASTRUCTURE_ERROR + ":ExecutionException"` 而非原始异常类型（如 NPE），导致运维人员无法从降级原因中识别真正的 Bug 类别。需注意：此异常已被 `doExecuteInternal()` 捕获，不会传播到 `AiOrchestrator.handle()`——这与 Problem 1 覆盖的外层步骤异常路径互斥互补。
- **所在位置**：§3.1 薄适配器 doExecuteInternal() 伪代码第 751-753 行区域
- **严重程度**：中等（生产环境调试信息降级影响问题定位效率）
- **改进建议**：在 catch(Exception) 分支中将原始 cause 的类型名拼接到降级原因中：`degradeReason = DegradationReason.INFRASTRUCTURE_ERROR + ":" + (e.getCause() != null ? e.getCause().getClass().getSimpleName() : e.getClass().getSimpleName())`。或区分 `ExecutionException` 与普通 `RuntimeException`：若是 `ExecutionException`，提取 `getCause()` 的类型和消息作为降级原因。

---

### 问题5：Experiment PAUSED 状态下 `assign()` 的返回值未冻结

- **问题描述**：§3.4 定义 PAUSED 语义为"暂停分流，所有流量回退到默认模型"（第 1104 行），但 §4.3 实验分流契约（第 1622-1629 行）未包含 PAUSED 状态的专用分支。当前契约仅描述"检索 ACTIVE 实验"——PAUSED 状态的实验不会被检索到，因此 `assign()` 将返回 `createDefault()`。但这意味着所谓"暂停分流"是通过"不检索到实验"间接实现的，而非显式处理。若未来实现了缓存预热或实验预加载逻辑（PAUSED 实验仍可能被加载到缓存的 active 实验列表中），当前契约的隐含假设将被破坏。
- **所在位置**：§3.4 Experiment PAUSED 语义（第 1104 行）vs §4.3 实验分流契约（第 1622-1625 行）
- **严重程度**：中等（当前实现正确，但契约语义被弱化，未来变更易产生 Bug）
- **改进建议**：在 §4.3 实验分流契约中显式增加 PAUSED 状态分支：步骤 1 中检索 ACTIVE 实验时，过滤掉 status = PAUSED 的实验（而非隐式依赖"检索不到"）。同时在步骤 2 前补充注释："PAUSED 状态的实验将不被纳入分流检索，可直接返回 assign() default 结果"。

---

### 问题6：PromptTemplate 状态模型缺少 DEPRECATED→ACTIVE 回退路径

- **问题描述**：§3.3 PromptTemplate 状态模型（第 1081-1086 行）定义了 `DRAFT → ACTIVE → DEPRECATED` 的单向转换。但在实际运维中，若新版本模板上线后发现 Bug，运维人员可能需要将上一个 ACTIVE（现为 DEPRECATED）版本紧急回滚。当前状态模型不允许此回退——唯一路径是新建一个内容相同的 DRAFT 版本再发布为 ACTIVE。此限制在紧急回滚场景下增加了操作步骤和发布风险。
- **所在位置**：§3.3 PromptTemplate 状态模型第 1081-1086 行
- **严重程度**：一般（不影响一期功能，但生产运维需提前规划）
- **改进建议**：在状态模型中增加 `DEPRECATED → ACTIVE` 转换路径，或至少在 §10/§11 中记录回滚策略：建议通过管理端"基于 DEPRECATED 版本创建新 DRAFT 版本"的工作流实现，并评估 DRAFT→ACTIVE 的审批时效是否满足紧急回滚的 SLA。

---

### 问题7：doc 中缺少对"不可变 DTO + 防御性拷贝共存时 ObjectMapper 兼容性"的说明

- **问题描述**：§3.1 第 626 行推荐 DTO 设计为不可变对象（所有字段 final，无 setter，Jackson 反序列化通过 `@ConstructorProperties` 或 `@JsonCreator`）。但 §4.1 第 1498 行对所有 request 执行 `objectMapper.convertValue(request, request.getClass())` 防御性拷贝。如果某个能力 DTO 已按推荐设计为不可变但未标注 `@JsonCreator`/`@ConstructorProperties`，`ObjectMapper.convertValue()` 将因无法构造实例而抛出异常。设计文档未说明此情况下如何处理——是跳过拷贝（有条件拷贝）、还是要求所有 DTO 必须支持 Jackson 双向转换。
- **所在位置**：§3.1 第 626 行（不可变 DTO 推荐）vs §4.1 第 1498 行（无条件防御性拷贝）
- **严重程度**：一般（编码阶段可调整，但设计文档应提供指引）
- **改进建议**：在 §3.1 的 Request DTO 线程安全契约段补充：若 DTO 已实现为不可变（全 final 字段 + 无 setter），应确保实现 Jackson 兼容反序列化（`@JsonCreator`/`@ConstructorProperties`/`@JsonPOJOBuilder`），否则防御性拷贝步骤会抛出异常。或在 AbstractCapabilityExecutor 中对不可变 DTO 跳过拷贝步骤（考虑引入 `@Immutable` 注解标记或通过构造函数参数数量判断）。

---

## 整体质量评价

该文档整体深度和完整性很高（2231 行），覆盖了需求文档要求的全部 OOD 核心要素（类图、核心职责、协作关系、关键接口、状态模型），在内部审议中通过 15 轮迭代修正了大量技术细节。未发现对用户需求（完成 Phase 5 包 G OOD 设计）的根本性偏离。

从外部审查视角出发，上述 7 个问题主要集中在：(1) 异常路径和边界条件在薄适配器场景下的覆盖缺口与可观测性数据传递（问题 1、2、4）；(2) 状态模型和行为契约中未冻结的边缘语义（问题 3、5、6）；(3) 设计约束之间的潜在冲突（问题 7）。建议在后续迭代中优先处理问题 1~3，其他问题可排入 Phase 6 规划或设计文档的待办清单。

---

## 修订说明（v2）

本修订版基于质询文件 `b_v13_challenge_v1.md` 中的质疑意见进行修订。

| 质询意见 | 回应 |
|---------|------|
| Problem 1（文档版本标识矛盾）违反"不要关注文档版本号"指令，要求删除或降级 | **已采纳**。删除原 Problem 1（版本标识不一致）。该问题涉及文档元数据格式而非设计质量本身，不属于本次审查的核心关注范围。其余 7 个问题重新编号。 |
| Problem 2 与 Problem 5 存在逻辑矛盾——Problem 2 假定薄适配器异常可传播至 handle()，但 Problem 5 论证薄适配器已捕获所有异常类型，互斥路径未做调和 | **已调和**。经重新分析，两条路径实为互补而非互斥：(1) Problem 1（原 Problem 2）覆盖 `AbstractCapabilityExecutor.execute()` 模板方法外层步骤（防御性拷贝 `ObjectMapper.convertValue()`、`inputSummary` 预计算 `request.toString()` 等，位于 `supplyAsync()` 调用之前，第 1498-1501 行）——这些步骤若抛出异常，将直接传播到 `AiOrchestrator.handle()` 的 catch 块，未被 `doExecuteInternal()` 的 catch 捕获；(2) Problem 4（原 Problem 5）覆盖 `doExecuteInternal()` 内部通过 `CompletableFuture.supplyAsync()` + `future.get()` 包裹的 Phase 4 委托调用——此路径中 `ExecutionException` 已被捕获但原始异常类型丢失。两条路径的异常来源（外层步骤 vs 内层委托）不重叠。已相应更新 Problem 1 和 Problem 4 的问题描述，明确各自的适用范围和互斥关系，标注"与 Problem X 覆盖的场景互斥互补"。 |

（注：其余未被质询的问题保持 v1 版本内容不变。上述修订仅涉及删除原 Problem 1 和调和原 Problem 2/Problem 5 的逻辑矛盾，其他 5 个问题的问题描述、严重程度和改进建议均未实质性变更。）
