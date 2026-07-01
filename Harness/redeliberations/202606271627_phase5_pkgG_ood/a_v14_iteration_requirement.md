根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 问题1：AiOrchestrator.handle() 的 catch 块中薄适配器场景的就诊上下文提取失效
- **描述**：handle() catch 块通过 `request instanceof AiRequestBase` 判断并提取就诊上下文字段，但 6 项 Phase 4 薄适配器的请求 DTO 暂不继承 `AiRequestBase`，当 `AbstractCapabilityExecutor.execute()` 模板方法外层步骤（防御性拷贝、inputSummary 预计算等）抛出异常并传播至 handle() 时，instanceof 检查为 false，就诊上下文字段全部传入 null。此场景与 Problem 4 所述 doExecuteInternal() 内异常已被捕获的场景互不重叠。
- **位置**：§4.1 AiOrchestrator.handle() 伪代码第 1465-1474 行；§4.1 AbstractCapabilityExecutor.execute() 第 1498-1501 行
- **严重程度**：中等
- **改进建议**：在 catch 块中增加对 Phase 4 DTO 的兼容提取路径：当 `request instanceof AiRequestBase` 为 false 时，通过 `RequestContextHolder` / HTTP Header 独立提取就诊上下文字段，或要求薄适配器 CapabilityExecutor 在最外层 catch 中自行降级兜底。

### 问题2：ParseFailure 降级路径丢失原始 LLM 响应摘要
- **描述**：`doExecuteInternal()` 中 `structuredOutputParser.parse()` 抛出 `ParseException`，此时 `outputSummary` 已赋值为原始 LLM 响应摘要（第 1564 行），但此值未传入 `doDegrade()`——`doDegrade()` 方法签名不含 `outputSummary` 参数，导致 AiCallRecord 中该条降级记录的 `outputSummary` 为空。
- **位置**：§4.1 doExecuteInternal() 第 1564-1569 行，doDegrade() 第 1584-1601 行
- **严重程度**：中等
- **改进建议**：在 `doDegrade()` 签名中增加可选的 `String outputSummary` 参数，解析失败降级时传入 LLM 原始响应的摘要。

### 问题3：Prompt 模板渲染契约未定义"指定版本已废弃"的回退行为
- **描述**：渲染契约规定"promptVersion 不为 null 且有对应版本→使用指定版本模板；为 null 或无对应版本→回退到 ACTIVE 模板"。但未覆盖"promptVersion 对应版本已处于 DEPRECATED 状态"的场景。A/B 实验的实验配置可能引用已被废弃的 Prompt 版本号，行为未冻结。
- **位置**：§4.4 Prompt 模板渲染契约第 1636-1637 行
- **严重程度**：中等
- **改进建议**：在渲染契约中增加规则：若 promptVersion 对应版本存在但 status = DEPRECATED，输出 WARN 日志并回退到当前 ACTIVE 模板。同时在 §3.3 PromptTemplate 状态模型中明确标注此行为。

### 问题4：薄适配器 doExecuteInternal() 中 ExecutionException 包裹的原始异常类型丢失
- **描述**：薄适配器 `doExecuteInternal()` 使用 `CompletableFuture.supplyAsync()` + `future.get(timeout)` 包裹 Phase 4 委托调用。catch(Exception) 分支中降级原因记录的是 `DegradationReason.INFRASTRUCTURE_ERROR + ":ExecutionException"` 而非原始异常类型（如 NPE），导致运维人员无法从降级原因中识别真正的 Bug 类别。此异常已被 doExecuteInternal() 捕获，不会传播到 AiOrchestrator.handle()——与 Problem 1 互斥互补。
- **位置**：§3.1 薄适配器 doExecuteInternal() 伪代码第 751-753 行区域
- **严重程度**：中等
- **改进建议**：在 catch(Exception) 分支中将原始 cause 的类型名拼接到降级原因中：`degradeReason = DegradationReason.INFRASTRUCTURE_ERROR + ":" + (e.getCause() != null ? e.getCause().getClass().getSimpleName() : e.getClass().getSimpleName())`。

### 问题5：Experiment PAUSED 状态下 `assign()` 的返回值未冻结
- **描述**：§3.4 定义 PAUSED 语义为"暂停分流，所有流量回退到默认模型"，但 §4.3 实验分流契约未包含 PAUSED 状态的专用分支。当前通过"检索不到实验"间接实现暂停，而非显式处理。若未来实现缓存预热或实验预加载，当前契约的隐含假设将被破坏。
- **位置**：§3.4 Experiment PAUSED 语义（第 1104 行）vs §4.3 实验分流契约（第 1622-1625 行）
- **严重程度**：中等
- **改进建议**：在 §4.3 实验分流契约中显式增加 PAUSED 状态分支：检索 ACTIVE 实验时过滤掉 status = PAUSED 的实验，补充注释说明 PAUSED 状态的实验不被纳入分流检索。

### 问题6：PromptTemplate 状态模型缺少 DEPRECATED→ACTIVE 回退路径
- **描述**：§3.3 PromptTemplate 状态模型定义了 `DRAFT → ACTIVE → DEPRECATED` 的单向转换。运维中若新版本模板上线后发现 Bug，需要将上一个 ACTIVE（现为 DEPRECATED）版本紧急回滚，当前状态模型不允许此回退。
- **位置**：§3.3 PromptTemplate 状态模型第 1081-1086 行
- **严重程度**：一般
- **改进建议**：在状态模型中增加 `DEPRECATED → ACTIVE` 转换路径，或至少在 §10/§11 中记录回滚策略：通过管理端"基于 DEPRECATED 版本创建新 DRAFT 版本"的工作流实现。

### 问题7：doc 中缺少对"不可变 DTO + 防御性拷贝共存时 ObjectMapper 兼容性"的说明
- **描述**：§3.1 推荐 DTO 设计为不可变对象（全 final 字段，无 setter），但 §4.1 对所有 request 执行 `objectMapper.convertValue(request, request.getClass())` 防御性拷贝。若 DTO 已按推荐设计为不可变但未标注 `@JsonCreator`/`@ConstructorProperties`，防御性拷贝将因无法构造实例而抛出异常。设计文档未说明处理方式。
- **位置**：§3.1 第 626 行（不可变 DTO 推荐）vs §4.1 第 1498 行（无条件防御性拷贝）
- **严重程度**：一般
- **改进建议**：在 §3.1 Request DTO 线程安全契约段补充：若 DTO 已实现为不可变，应确保实现 Jackson 兼容反序列化，或在 AbstractCapabilityExecutor 中对不可变 DTO 跳过拷贝步骤。

## 历史迭代回顾

分析历史反馈（迭代第 1~12 轮）与当前 7 个问题之间的关系：

### 已解决的问题
以下问题出现在历史反馈中，当前诊断报告不再提及，可确认已解决：
- **Bean 装配二义性**（迭代 1、5）：`@Primary` + `ObjectProvider` 方案已冻结，不再被质疑
- **异步上下文传播**（迭代 8）：在 execute() 入口处提取上下文再传入 lambda 的机制已被认可
- **LlmCallExecutor 拒绝策略**（迭代 8）：已在 §3.2 定义并解决
- **AiCallRecord 工厂方法签名完整性**（迭代 6、8、9）：callerRole/callerId/promptVersion 等字段均已补充
- **inputSummary 闭包捕获问题**（迭代 11）：已改为显式参数传入 doDegrade()/doExecuteInternal()
- **promptVersion 参数缺失**（迭代 12）：doDegrade() 签名已增加 Integer promptVersion 参数
- **薄适配器 Maven 依赖作用域**（迭代 12）：已选定 provided 并给出理由
- **CompletableFuture.cancel() 事实错误**（迭代 12）：已删除并改用 WARN 日志

### 持续存在的问题
以下问题在多轮反馈中反复出现，本轮以新角度再次暴露，需重点解决：
- **AiOrchestrator.handle() catch 块就诊上下文丢失**：迭代 8（薄适配器 departmentId 异步提取）、迭代 10（catch 块丢失就诊上下文）、本轮问题 1（薄适配器 DTO 不继承 AiRequestBase 时 instanceof 判误）。问题根源在于 catch 块对 Phase 4 DTO 的特殊性未做兼容处理，需从提取策略层面根治。
- **doDegrade() 参数传递缺口**：迭代 8（缺少 departmentId）、迭代 11（inputSummary 闭包捕获）、迭代 12（promptVersion 缺失）、本轮问题 2（outputSummary 在 ParseFailure 降级路径中丢失）。每轮填补一个参数缺口，本轮需彻底端到端审视 doDegrade() 参数完备性。
- **Experiment PAUSED 语义与契约脱节**：迭代 5（PAUSED 语义与实际哈希分桶矛盾，已修正语义为"暂停分流"）、本轮问题 5（修正后的语义仍未被 §4.3 分流契约显式覆盖）。需在 §4.3 中增加显式 PAUSED 分支，避免依赖隐含假设。
- **防御性拷贝与 DTO 设计约束冲突**：迭代 9（防御性拷贝合约未在伪代码中兑现，已补充拷贝步骤）、本轮问题 7（拷贝步骤与不可变 DTO 的 Jackson 兼容性冲突）。需在 §3.1 线程安全契约中明确 Jackson 兼容要求，或增加有条件的拷贝跳过机制。

### 新发现的问题
以下问题为本轮首次识别，之前迭代未提及：
- **问题 3**：Prompt 模板渲染契约未定义 DEPRECATED 版本的回退行为——之前迭代关注的是实验分流传参链路打通（迭代 6），未触及版本废弃后的运行时行为
- **问题 4**：薄适配器 ExecutionException 包裹的原始异常类型丢失——迭代 10/12 关注了薄适配器异常处理和超时降级，但未分析 catch(Exception) 内部的异常链拆包与类型降级问题
- **问题 6**：PromptTemplate 状态模型缺少 DEPRECATED→ACTIVE 回退路径——此前迭代覆盖了状态模型定义本身（迭代 1）和实验/端点健康等状态模型，但未涉及运维回滚需求

## 上一轮产出路径
C:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606271627_phase5_pkgG_ood/a_v13_copy_from_v12.md

## 用户需求
C:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606271627_phase5_pkgG_ood/requirement.md
