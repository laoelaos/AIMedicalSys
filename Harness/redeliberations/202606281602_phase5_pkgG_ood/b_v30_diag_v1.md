# 质量审查报告 — Phase 5 包 G OOD 设计文档（v30）

> 审查范围：需求响应充分度、事实/逻辑正确性、深度与完整性（从实际落地视角）
> 审查日期：第 30 轮迭代
> 待审文件：`a_v30_copy_from_v29.md`

---

## 1. 薄适配器委托超时未使用 per-capability 覆盖值

**问题描述**：§4.2 薄适配器 `doExecuteInternal()` 伪代码中，委托调用的超时控制仅使用全局默认值 `thinAdapterTimeout`（行 1303），未从已注入的 `thinAdapterPerCapabilityConfig` Map 中按 `capabilityId` 查找 per-capability 覆盖值。§3.12.4 `ImageAnalysisCapabilityExecutor` 特化表标注其委托超时应为 45s（行 3310），§3.12.5 `RecommendExaminationCapabilityExecutor` 标注为 20s（行 3324），但伪代码实现中这些差异化配置未被引用。同时行 1310 的 WARN 日志输出的也是 `thinAdapterTimeout` 而非实际生效的 per-capability 值，延续了迭代 29 第 1 条指出的问题。

**所在位置**：§4.2 薄适配器特化管线伪代码（行 1303、行 1310）；§3.1 薄适配器构造器参数（行 1201-1202）

**严重程度**：严重

**改进建议**：在 `doExecuteInternal()` 入口处从 `thinAdapterPerCapabilityConfig.getOrDefault(capabilityId, thinAdapterTimeout)` 解析 `effectiveThinAdapterTimeout`，将行 1303 的 `thinAdapterTimeout` 和行 1310 的日志值均替换为此有效值。同时在各薄适配器特化表（§3.12）中检查超时声明值与伪代码实际行为的一致性。

---

## 2. `executeStandardPipeline()` 与 `doExecuteInternal()` 的参数签名不一致导致实施歧义

**问题描述**：`executeStandardPipeline()` 方法签名含 `variables`、`promptVersion`、`sentinelReason` 三个参数（类图行 503），但 `doExecuteInternal()` 签名不含这些参数（类图行 496）。子类（如 `DiscussionConclusionCapabilityExecutor`）在完成前置逻辑后调用 `executeStandardPipeline()` 时，需自行获取 `variables`（通过 `extractVariables()`）、`promptVersion`（通过 `experimentManager.assign()`）和 `sentinelReason`。但伪代码中 `doExecuteInternal()` 的参数列表未完整包含这些值作为传递介质——`variables` 在 `doExecuteInternal()` 内部由 `extractVariables(request)` 获取（行 3492），`promptVersion` 来自实验分流（行 3510），`sentinelReason` 同样来自实验分流（行 3511）。这是正确的：这三个参数在 `doExecuteInternal()` 内部计算而非外部传入。但 `executeStandardPipeline()` 作为非子类特化的 protected final 方法，其签名要求调用方提供这三值——这意味着 `executeStandardPipeline()` 的调用方（特化子类）需重复计算或重新传入这些值，与 `doExecuteInternal()` 中已有计算产生职责重叠，实施者面临"谁负责计算"的歧义。

**所在位置**：§2.3 类图行 503（`executeStandardPipeline` 签名）；§3.1 行 1439（方法定义）；§4.1 行 3490-3511（`doExecuteInternal` 中的变量计算）

**严重程度**：重要

**改进建议**：在 `doExecuteInternal()` 的标准管线实现中将 `executeStandardPipeline()` 作为子程序调用，并在 `doExecuteInternal()` 的伪代码中明确指出："子类在完成前置逻辑后，只需调用 `executeStandardPipeline(startTime, request, capabilityId, departmentId, userId, sessionId, callerRole, callerId, visitId, patientId, inputSummary, variables, promptVersion, sentinelReason)`，其中 `variables`、`promptVersion`、`sentinelReason` 由本方法在调用前通过 `extractVariables()`、`experimentManager.assign()` 计算"。或在标准管线实现中直接内联这些计算步骤，使子类无需接触三个计算逻辑。

---

## 3. §3.11.4 PrescriptionAssist 模板变量与 DTO 结构的映射关系未明确定义

**问题描述**：迭代 29 第 3 条已指出 `PrescriptionAssistCapabilityExecutor` 的模板变量 `{{patientAge}}`、`{{patientWeight}}`、`{{allergyInfo}}` 与内嵌 `PatientInfo` 值对象结构之间的提取不匹配问题。v30 虽已将变量提取策略改为"方式 B（自定义）"（§3.11.4 行 3151），并声明与 `PrescriptionCheckCapabilityExecutor` 使用一致的自定义展开方式，但以下映射细节未定义：(a) `PatientInfo.getAge()` → `{{patientAge}}` 的单向映射契约（字段名转换规则、null 处理、单位说明）；(b) `allergyInfo: List<String>` → `{{allergyInfo}}` 的列表格式化格式（逗号分隔/编号列表/分号分隔）；(c) 提取失败时的回退策略（某字段为 null 时模板变量保留占位符或跳过）。

**所在位置**：§3.11.4 行 3147-3151（DTO 字段定义与提取策略）

**严重程度**：中等

**改进建议**：在 §3.11.4 或 §3.1 变量提取约定部分补充 `PatientInfo` → 模板变量的显式映射表（含字段名、模板变量名、转换规则、null 处理、格式化方式），并标注与 `PrescriptionCheckCapabilityExecutor` 共享的映射逻辑。

---

## 4. `refineTimeoutReason()` 中 `elapsedInDoExecuteInternal` 存在竞态读取风险

**问题描述**：`elapsedInDoExecuteInternal` 为 `volatile long` 类型（类图行 491），在 `supplyAsync()` lambda 入口处赋值（§4.1 行 3465），在 `exceptionally()` 回调中由 `refineTimeoutReason()` 读取（行 3479）。若 `orTimeout()` 在 `supplyAsync` lambda 开始执行**之前**触发（即任务在线程池队列中等待时超时），则 `elapsedInDoExecuteInternal` 仍为默认值 0，`refineTimeoutReason()` 将返回 `DegradationReason.TIMEOUT` 而非任何细化标记。虽然此场景下超时根因本就不需要细化（未进入管线），但 0 值被传入 `refineTimeoutReason()` 可能导致该方法的实现（如 `DiscussionConclusionCapabilityExecutor` 重写版本）误判为"前置压缩挤占"或"主 LLM 超时"——这两种情况都要求管线已部分执行。

**所在位置**：§4.1 行 3465（赋值）、行 3479（读取）；§3.1 行 1477（`refineTimeoutReason` 定义）；类图行 491（字段声明）

**严重程度**：中等

**改进建议**：在 `refineTimeoutReason()` 的默认实现和 `DiscussionConclusionCapabilityExecutor` 的重写中增加 `elapsedInDoExecuteInternal <= 0` 守卫，返回 `DegradationReason.TIMEOUT`（不拼接细分标识）。或在 `orTimeout().exceptionally()` 回调中先读取 `elapsedInDoExecuteInternal`，若为 0 或小于某个阈值则直接使用默认原因而不调用 `refineTimeoutReason()`。

---

## 5. 薄适配器不经过实验分流但伪代码中 `doDegrade()` 调用点传 `promptVersion=null` 和 `sentinelReason=null` 的语义解释分散

**问题描述**：薄适配器 `doExecuteInternal()` 的 3 个 `doDegrade()` 调用点均传入 `null` 作为 `promptVersion` 和 `sentinelReason`（行 1312、行 1334（此处的 failure 调用不涉及 promptVersion，但行 1343 的 degrade 调用传 null））。薄适配器不经过实验分流步骤，此行为的理由在 §3.12 共同约束或 §3.1 薄适配器说明中未集中声明。实施者需在散布的文本中找到"薄适配器不包含实验分流"的说明才能理解为何传 null。当未来某位维护者考虑为薄适配器引入实验分流时，需要定位散落在 §3.1（行 1125）、§3.12 共同约束中的修改点。

**所在位置**：§4.2 行 1312、1343（调用点）；§3.1 行 1125（薄适配器管线说明）；§3.12 共同约束

**严重程度**：轻微

**改进建议**：在 §3.12 共同约束部分（行 3344 附近）新增一条显式声明："薄适配器不经过实验分流，所有 `doDegrade()` 调用点中 `promptVersion` 和 `sentinelReason` 恒为 `null`。若未来引入薄适配器实验分流，需同步修改此处的 null 传递及 §4.2 伪代码。"并在 §4.2 伪代码中对应的 null 参数处添加行尾注释。

---

## 6. `remainingBeforeOrTimeout` 的计算时机与变量命名存在语义偏差

**问题描述**：§4.1 行 3584 变量名为 `elapsedBeforeLambda`，赋值为 `System.currentTimeMillis() - startTime`。但代码执行至此已在 `supplyAsync` lambda 内部（`doExecuteInternal()` 中、实验分流/模板渲染/模型路由/健康检查/Options 构造等步骤之后），命名中的"BeforeLambda"与实际作用域不符。该变量实为"结构化调用前的已消耗时间"。虽不影响功能正确性，但增加实施者的认知负担——当后续维护者看到 `elapsedBeforeLambda` 引用时，需要通过上下文推断其实际含义。

**所在位置**：§4.1 行 3584

**严重程度**：轻微

**改进建议**：将变量名改为 `elapsedBeforeStructuredChat`，并在行 3584 旁添加注释说明其与 `elapsedInDoExecuteInternal` 的差异（后者为 lambda 入口处的时间快照，前者为结构化调用前的累计耗时，包含前置步骤的耗时）。

---

## 7. §4.1 伪代码中 `[shared_success_handler]` 作为隐式 fall-through 目标，控制流不直观

**问题描述**：structuredChat 成功路径（行 3614）和 chat() 回退成功路径（对应行未完整读取）均通过注释 `// → [shared_success_handler]` 指向共用成功处理段，但伪代码中没有显式的标签、goto 或 return 语句，共用处理段实际上通过两条路径各自的 fall-through 到达。这种隐式控制流使实施者在阅读时无法从单条路径段独立判断执行结束点——必须同时阅读两条路径的完整代码才能确认不会发生"结构化成功后被 chat 回退覆盖"的问题。

**所在位置**：§4.1 `doExecuteInternal()` 伪代码（行 3614 附近）

**严重程度**：轻微

**改进建议**：在共用成功处理段之前添加显式注释锚点 `<shared_success_handler>`，或在两条路径的末尾添加 `// fall-through to shared_success_handler` 注释，将 fall-through 意图显式化。如有条件，可考虑将共用成功处理段提取为辅助方法（如 `handleSuccess(...)`）以消除 fall-through。

---

## 8. 综合评估

**需求响应充分度**：产出充分响应了用户需求——完成了 Phase 5 包 G 的完整 OOD 设计，覆盖类图、核心职责、协作关系、关键接口、状态模型等 OOD 核心要素，且与 Phase0/Phase1ABD 的设计风格保持一致性。

**事实/逻辑正确性**：整体质量高，绝大多数此前 29 轮迭代中发现的事实错误和逻辑矛盾已在 v30 中得到修正。上述第 1、4 条属于本轮审查新发现的残留问题。

**深度与完整性**：深度充分——覆盖了从配置装配到运行时行为的全链路设计。完整性方面，7 项底座能力和 6 项薄适配器能力的特化设计表已覆盖（§3.11、§3.12），非功能性质量分析（§1.8）和启动期 Bean 初始化顺序依赖图（§3.9.1）提供了实施者所需的额外细节。上述第 2、3、5 条属于可实施但存在认知负担的细节缺口。

**落地可行性评估**：文档整体可直接指导编码实现，接口定义足以支持下游消费者（AiService 接口签名明确、DTO 字段逐能力定义），异常场景和边界条件已系统化覆盖（降级路径、超时管理、实验分流异常、空 DTO 过渡期）。上述 1、2 条为实施过程中可能引发返工或编码错误的实际风险点。
