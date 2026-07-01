# 质量审查报告 — Phase 5 包 G OOD 设计（v26）

## 审查范围

- 审查轮次：第 26 次（在当前审议框架中的首轮质量审查）
- 审查重点：需求响应充分度、深度与完整性、事实错误与逻辑矛盾
- 审查视角：实际落地可行性评估，侧重内部审议（Agent A 设计-验证循环）未充分覆盖的维度

---

## 质量问题清单

### 问题 1（严重）：`extractVariables()` 的职责归属在 §3.11.7 与 §4.1 之间存在事实矛盾

- **问题描述**：§3.11.7 能力特化表（`DiscussionConclusionCapabilityExecutor`）的「模板变量提取策略」行明确要求 "`extractVariables()` ...若 `transcripts` 原始文本超过 3000 tokens，使用首轮 LLM 调用压缩为摘要后注入 `transcriptSummary` 变量"。但 §4.1 `DiscussionConclusionCapabilityExecutor.doExecuteInternal()` 伪代码（行 3464-3576）将 transcript 压缩逻辑实现于方法体层面，并自行调用 `variables.put("transcriptSummary", transcriptSummary)` 向 variables 注入——而非由 `extractVariables()` 内部完成。两处描述给出截然不同的职责分配：按特化表实现，则 `extractVariables()` 需承担 LLM 压缩调用（约 15s 阻塞操作）并使用独立线程池；按伪代码实现，则 `extractVariables()` 仅为简单字段提取，压缩由 doExecuteInternal() 完成。实现者无法判断哪个是设计意图。

- **所在位置**：§3.11.7「模板变量提取策略」行（行 3031）；§4.1 DiscussionConclusionCapabilityExecutor.doExecuteInternal()（行 3464-3576，特别是行 3576 `variables.put("transcriptSummary", transcriptSummary)`）
- **严重程度**：严重
- **改进建议**：统一职责归属。推荐方案：采纳 §4.1 伪代码的做法——将压缩逻辑放在 doExecuteInternal() 中，`extractVariables()` 仅提取 `discussionId`/`discussionType`/`speakerCount` 等简单字段。同步修正 §3.11.7 特化表的描述，确保两处一致。

### 问题 2（严重）：DiscussionConclusionCapabilityExecutor 的 `super.doExecuteInternal()` 调用不可行——模板方法设计存在结构性缺陷

- **问题描述**：§4.1（行 3578-3580）注释写道 "后续复用标准 doExecuteInternal 流程（实验分流→模板渲染→模型路由→LLM 调用→结构化解析→指标采集）...实际实现中通过 super.doExecuteInternal() 调用父类公共模板方法"。但 `AbstractCapabilityExecutor.doExecuteInternal()` 声明为 `abstract`（行 1393），无法通过 `super.doExecuteInternal()` 调用——子类重写后 `super` 无法直接调用抽象方法。DiscussionConclusionCapabilityExecutor 重写 `doExecuteInternal()` 加入前置压缩后，无法在压缩完成后再调用父类的标准管线实现，导致标准管线代码在讨论结论能力中必须被重复实现（或从父类复制），严重违背 DRY 原则。

- **所在位置**：§4.1 行 3578-3580（super.doExecuteInternal() 引用）；行 1393（AbstractCapabilityExecutor.doExecuteInternal() 定义为 abstract）；行 3464-3576（DiscussionConclusionCapabilityExecutor 重写范围）
- **严重程度**：严重
- **改进建议**：修正设计方案。推荐方案：将标准管线逻辑从 `abstract doExecuteInternal()` 中提取为 `protected` 非抽象方法（如 `executeStandardPipeline(...)`），DiscussionConclusionCapabilityExecutor 在 `doExecuteInternal()` 中完成前置压缩后调用 `executeStandardPipeline()`。同步更新类图引入新方法。

### 问题 3（重要）：`preciseTokenCount()`/`formatTranscripts()`/`truncateTranscripts()` 三个辅助方法未正式定义

- **问题描述**：DiscussionConclusionCapabilityExecutor 的伪代码在行 3511 调用 `preciseTokenCount(transcripts)`，行 3567 调用 `truncateTranscripts(transcripts, 2000)`，行 3553/3570 调用 `formatTranscripts(transcripts)`。三个方法在类图中无声明、在文档中无方法签名、在伪代码外无行为契约。实现者无从得知其返回值、参数类型、内部行为或线程安全约定。特别是 `preciseTokenCount()` 的线程安全性已在伪代码内注释中有所涉及（行 3509-3510），但缺乏正式的方法定义支撑。

- **所在位置**：§4.1 行 3511、行 3553、行 3567、行 3570；§2.3 类图 DiscussionConclusionCapabilityExecutor 节点（行 499-500）；§3.11.7 特化设计表（行 3022-3035）
- **严重程度**：重要
- **改进建议**：在 §3.11.7 中为三个方法补充正式定义，包括方法签名、入参、返回值和行为契约。`preciseTokenCount()` 还需补充线程安全说明。或在类图中新增 private method 声明（即使 UML 中不常显式给出 private 方法，但在实现指导性文档中应予以明确）。

### 问题 4（重要）：字符估算回退分支中 `estimatedTokens = 2000` 的硬编码跳跃值缺乏依据

- **问题描述**：§4.1 行 3526-3527 的逻辑为：字符估算值在 3001~4000 区间时，强制将 `estimatedTokens` 设为 2000，以抑制假阳性触发。此调整引入两个问题：(1) 2000 是任意值，与该场景下实际文本长度无对应关系；(2) 若 transcripts 实际 tokens 恰好在 3000+ 但因字符估算偏差被报为 4000（文档声称"不会漏触发"但未提供严格数学证明），强制设为 2000 将跳过必要的压缩，可能导致超出 LLM 上下文窗口。此跳跃值在 §3.11.7 分析段落（行 3486-3490）中未被讨论或理论支撑。

- **所在位置**：§4.1 行 3521-3527（阈值调整逻辑）；§3.11.7 字符估算分析段落（行 3471-3494）
- **严重程度**：重要
- **改进建议**：方案 A（推荐）：将回退分支的触发阈值直接从 3000 提升到 4000（而非在 3000 触发后再用 2000 抑制），消除硬编码跳跃值。方案 B：提供 `tokenizerAvailable=false` 时 `preciseTokenCount()` 的简化替代实现（如正则拆分 + 加权估算），从根本上降低假阳性而非用跳跃值掩盖。

### 问题 5（重要）：类图未展示 `AbstractCapabilityExecutor` 构造器——在大量构造器参数篇幅下的关键缺失

- **问题描述**：文档用很大篇幅（§3.1 行 1514-1542 三期迁移计划、行 1487-1512 构造器伪代码）讨论 `AbstractCapabilityExecutor` 的 16 参数构造器问题和 CallContext 重构计划，但 §2.3 类图（行 467-485）的 `AbstractCapabilityExecutor` 节点中完全没有显示构造器。实现者需要跨三个章节（类图 + §3.1 的构造器定义 + §3.9 Bean 定义）交叉引用才能拼出构造器签名全貌。特别是超类构造器的参数顺序在各处表示不一致（类图无构造器、§3.1 构造器伪代码有 16 参、薄适配器 super() 调用有 16 参），跨章节验证困难。

- **所在位置**：§2.3 类图 `AbstractCapabilityExecutor` 节点（行 467-485）；§3.1 构造器伪代码（行 1487-1512）；§3.1 薄适配器 super() 调用（行 1180-1182）；§3.1 三期迁移计划（行 1514-1542）
- **严重程度**：重要
- **改进建议**：在 §2.3 类图的 `AbstractCapabilityExecutor` 节点中补充构造器签名（可使用 `+AbstractCapabilityExecutor(inputType, ...)` 的简化表示，参数较多时标注注释指向 §3.1 详细定义），使实现者无需多章节交叉引用即可确认参数数量和顺序。

### 问题 6（一般）：`userId` 与 `callerId` 语义冗余且来源相同

- **问题描述**：`execute()` 伪代码（行 3115-3116）从 `auth.getName()` 提取 `userId`，`RequestContextUtils.extractCallerId()`（行 2895-2898）同样从 `auth.getName()` 提取 `callerId`。两值在非匿名场景下必然相同（均来自 `Authentication.getName()`），但文档未解释为何需要两个不同名称的同一值，也未说明两值在何种场景下可能不同。`callerId` 用于 `AiCallRecord`（可观测性维度），`userId` 用于 `ExperimentManager.assign()`（实验分流入参），职责不同但值相同。实现者在编码时会产生认知负担：是否需要确保两值同步？为何不能复用一个字段？

- **所在位置**：§4.1 行 3115-3116（userId 提取）；§3.10 `extractCallerId()` 定义（行 2895-2898）；§6.4 上下文传播机制（行 3824-3864）；§3.5 `AiCallRecord` 字段表（行 2161-2184）
- **严重程度**：一般
- **改进建议**：在 §6.4 或 §3.1 的 "UserId、SessionId、callerRole、callerId 的上下文来源与提取时机" 段中增加一条说明，明确两值的语义区别：`userId` 是实验分流的入参（作为用户标识），`callerId` 是可观测性维度（作为调用方标识），当前从同一来源提取但语义独立，若未来引入"调用方代理"场景（如管理员代操作），两值可不同。此说明消除实施者的"是否存在同步需求"疑虑。

### 问题 7（一般）：`convertValue()` 防御性拷贝的失败后果未定义

- **问题描述**：§4.1 行 3125 执行 `objectMapper.convertValue(request, request.getClass())` 进行防御性拷贝。但当 `request` 为不可变 DTO 且未标注 Jackson 注解时，此调用将抛出异常（§3.1 行 1022 已承认此风险）。异常传播路径为：`execute()` 中未包裹 try-catch 的 `convertValue()` 调用 → 异常传播到 `AiOrchestrator.handle()` 的 catch 块 → 被记录为 `DegradationReason.INTERNAL_ERROR` 并返回失败。防御性拷贝失败导致本该正常执行的能力调用直接失败，且文档未提供明确的构件级解耦方案——"实现者须在子类中自行完成"（§3.1 行 1022）的表述过于模糊。

- **所在位置**：§4.1 行 3125（`objectMapper.convertValue()` 调用）；§3.1 行 1022（兼容性说明）；§4.1 行 3064-3108（AiOrchestrator.handle() catch 块）
- **严重程度**：一般
- **改进建议**：在 `execute()` 的防御性拷贝步骤（行 3125）增加 try-catch，捕获 `ConversionException` 后回退到使用原始 `request` 对象（日志 WARN），而非让异常传播到 handle() 层面导致整个调用失败。同步在 §3.1 的兼容性说明中补充此回退路径。

### 问题 8（一般）：`structuredChat()` 成功路径中 `fall-through` 到共享处理器但实际控制流不直观

- **问题描述**：§4.1 行 3290 注释 `// fall-through to shared success handler` 使用自然语言描述控制流，但在 indentation-based 伪代码中没有对应的语法结构支持。实际控制流依赖于结构化编程语义（if-else 的 else 分支有 return，if 分支无 return 则继续），读者需要解析缩进块结构才能理解"fall-through"的含义。在后续的 chat 回退路径中也有类似情况（行 3348）。对不熟悉此伪代码约定的新实现者，此模式增加了阅读障碍。

- **所在位置**：§4.1 行 3290（structuredChat 成功路径 fall-through 注释）和行 3348（chat 回退成功路径 fall-through 注释）
- **严重程度**：一般
- **改进建议**：在伪代码中添加显式的结构化标记，如注释 `// fall-through to line N` 或使用 `goto shared_success_handler` 模式的注释锚点。或在 doExecuteInternal() 末尾提取 `shared_success_handler:` 标签注释，与 fall-through 注释一一对应，使读者可快速定位。

---

## 整体质量评价

本产出经过 26 轮迭代审议和修正，在技术深度、异常场景覆盖、组件间协作和可追溯性方面已达到较高成熟度。上述 8 个问题集中在以下三个领域：
- **跨章节一致性**（问题 1、5）：因文档规模较大（~4400 行），迭代修正过程中产生了各章节间的局部不一致
- **模板方法结构的适配缺陷**（问题 2）：核心设计模式（模板方法）在被非标准子类扩展时出现结构性冲突，属于设计层面而非文档层面问题
- **辅助方法的定义完备性**（问题 3、4）：在反复迭代聚焦于核心管线纠错的过程中，边界辅助方法的正式定义被滞后
- **细节定义精度**（问题 6、7、8）：在文档收敛至稳定版本后，可进一步打磨细节定义以降低实现时的歧义

建议修复完成后将优先级标记为「严重」的 2 个问题作为版本发布前的阻塞项，其余问题可在实施初期补齐。
