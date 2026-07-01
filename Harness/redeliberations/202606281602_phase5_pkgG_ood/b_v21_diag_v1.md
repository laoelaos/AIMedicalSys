# 质量审查报告 — v21 产出诊断

## 审查范围与视角

本次审查基于用户需求（Phase 5 包G OOD 设计）和第1~20轮历史迭代反馈，对 v21 最终版本进行质量审查。审查侧重内部审议未充分覆盖的需求响应充分度、整体深度和完整性，从落地实施角度评估设计是否可直接指导编码。

---

## 发现问题

### [严重] 问题1：v21修订声明与§4.1伪代码内容矛盾——CallContext迁移未落地

**问题描述**：v21 修订说明第 6 条（行 4453）宣称"`doDegrade()` 方法签名和体更新为使用 callContext""所有伪代码中的 doDegrade 调用点示例同步更新为新签名模式"，但实际上 §4.1 全文伪代码仍使用旧的多参数签名，未做任何更新。

具体矛盾点：
- **`doDegrade()` 方法定义**（行 3351）：签名仍为 15 个独立参数 `(startTime, degradeReason, request, capabilityId, departmentId, callerRole, callerId, visitId, patientId, sessionId, inputSummary, outputSummary, modelId, promptVersion, sentinelReason)`，而非修订说明宣称的 7 参数 `(startTime, degradeReason, request, capabilityId, modelId, sentinelReason, callContext)`
- **`execute()` 模板方法中所有 `doDegrade()` 调用点**（行 3086、3109、3149、3163、3210、3243、3255、3265、3295、3298、3304、3313、3318、3328、3335）：均传递 15 个独立参数，而非 CallContext
- **`doExecuteInternal()` 签名**（行 3117）：仍为 11 个独立参数，而非 6 参数的 CallContext 形式
- **`AiCallRecord` 工厂方法调用点**（行 3339、3340、3341、3342 等）：仍传递独立字段参数，未使用 CallContext
- **`DiscussionConclusionCapabilityExecutor` 特化伪代码**（行 3410-3419、3513-3515）：同样使用旧参数列表

同时，文档正文多处已描述 CallContext：
- §1.3 核心抽象一览表已新增 `CallContext` 条目
- §2.1 目录结构 `ai-api/dto/base/` 已列 `CallContext.java`
- §3.1 行 1482-1517 已描述 CallContext 作为"Phase 5 实施期立即引入"的方案
- §3.5 行 2161-2175 已使用 CallContext 简化签名展示工厂方法

**所在位置**：
- 修订声明：行 4453（修订说明 v21 第 6 条）
- doDegrade 定义：行 3351-3395
- doExecuteInternal 定义：行 3117
- doDegrade 调用点：行 3086、3109、3149、3163、3210、3243、3255、3265、3295、3298、3304、3313、3318、3328、3335

**严重程度**：严重

**改进建议**：
- 方案 A（推荐）：保持旧参数签名，同步修正 v21 修订说明第 6 条以如实反映当前伪代码状态，删除 §1.3/§2.1/§3.1/§3.5 中的 CallContext 描述或将其标注为"Phase 5 第二阶段重构目标"
- 方案 B：真正将 §4.1 全部伪代码中的 doDegrade()/doExecuteInternal()/AiCallRecord 工厂方法签名和所有调用点更新为 CallContext 新签名模式，确保修订说明与实际内容一致。实施者据此编码时才不会产生歧义

---

### [重要] 问题2：parseTimeout配置引用路径三处不一致

**问题描述**：parseTimeout 的配置路径在文档注释、伪代码逻辑、YAML 定义三处分别引用不同的路径名，实施者在配置时无法确定正确路径。

- §4.1 行 3284 注释：`@Value("${ai.execution.timeout.parse:5s}")` — 引用路径 `parse`（无 `default` 后缀）
- §4.1 行 3288-3292 伪代码：使用 `parseTimeoutConfig.getOrDefault(capabilityId, parseTimeoutDefault)` — Map 查找模式，配置键为 `ai.execution.timeout.parse.per-capability`；`parseTimeoutDefault` 通过 `@Value("${ai.execution.timeout.parse.default:5s}")` 注入
- §9.5 行 4100 YAML 配置：`default: 5s`，注释标注"通过 `@Value("${ai.execution.timeout.parse.default:5s}")` 注入" — 路径为 `parse.default`

三处路径不完全一致：注释少写了 `.default` 后缀，伪代码使用了不同的取值机制（Map），YAML 定义了正确的路径。如果实施者按注释的路径配置，YAML 中实际绑定的是 `parse.default`，配置不会生效（绑定到错误的 key）。

**所在位置**：§4.1 行 3284、行 3288-3292；§9.5 行 4100

**严重程度**：重要

**改进建议**：
- 将 §4.1 行 3284 注释中的 `${ai.execution.timeout.parse:5s}` 修正为 `${ai.execution.timeout.parse.default:5s}`，与 YAML 定义和伪代码引用的 `parseTimeoutDefault` 注入路径保持一致
- 或统一使用一种取值机制：若保留 Map 模式，注释中应说明默认值来源为 `parseTimeoutDefault` 而非 `@Value`

---

### [重要] 问题3：版本标记(v{N})残留——v4轮次已要求清理但未执行

**问题描述**：v4 迭代（轮次 4 问题 2）明确要求"全文搜索并清除所有 `(v{N} 新增/修正/补充/修订)` 类标记"，但至少以下 4 处存在残留：

- §3.1 行 1313：`"AbstractCapabilityExecutor — 能力执行器抽象骨架（abstract class，v8 新增`"
- §3.5 行 2368：`"SlidingWindowMetricsStore — 调用指标滑动窗口存储（class，v2 新增）"`
- §3.7 行 2441：`"PrescriptionLocalRuleFallback — 处方审核本地规则降级实现（class，v4 迭代补充完整行为契约）"`
- §3.8 行 2554：`"DegradationContext — 降级判定上下文（class implements Serializable，v2 新增，扩展）"`

这些标记属于过程性版本注记，与最终交付物的"稳定版本"定位不符。v4 已明确要求剥离，但后续轮次未执行到位。虽然不直接影响编码，但这类"已声明但未执行"的清理工作的累积会降低文档的可信度。

**所在位置**：§3.1 行 1313、§3.5 行 2368、§3.7 行 2441、§3.8 行 2554

**严重程度**：重要

**改进建议**：
- 执行一次全文搜索 `\(v\d+`，清理所有 `(v{N} ...)` 形式的过程性标记，替换为简洁描述（如 "v8 新增" → 直接删除该标注），或统一移至各组件末尾的版本备注字段

---

### [中等] 问题4：AiOrchestrator.handle() 就诊上下文字段提取的临界条件缺陷

**问题描述**：§4.1 行 3037 的兜底提取触发条件为：

```
if departmentId == null && visitId == null && patientId == null && sessionId == null:
```

使用 `&&`（AND）而非 `||`（OR）。当 DTO 继承 `AiRequestBase` 但仅部分字段为 null（如 CGLIB 代理场景下 `departmentId` 提取失败为 null，但 `visitId` 有值）时，第二重 HTTP Header 兜底提取不会触发。虽然 catch 块是异常恢复路径，但丢失任意维度的就诊上下文都会影响下游的可观测性分析（如按科室维度聚合的指标缺失 `departmentId` 行数据）。

真实场景：CGLIB 代理导致 `request instanceof AiRequestBase` 返回 true（行 3029），但代理对象的 `getDepartmentId()` 因增强行为异常返回 null，而 `getVisitId()`/`getPatientId()`/`getSessionId()` 正常返回值。此时条件 `all are null` 不满足，HTTP Header 提取被跳过，`departmentId` 保持 null——但实际上 HTTP Header 中可能携带了 `X-Department-ID`。

**所在位置**：§4.1 行 3037

**严重程度**：中等

**改进建议**：
- 将条件改为 `if departmentId == null || visitId == null || patientId == null || sessionId == null`，对每个为 null 的字段从 HTTP Header 独立提取（使用对应的 Header name），其他非 null 字段保持原值。提取优先级：DTO 字段值 > HTTP Header 值 > null

---

### [中等] 问题5：estimateTokens() 高假阳性率在伪代码层面无应对

**问题描述**：§4.1 行 3417-3440 详细分析了 `estimateTokens()` 基于字符数的保守估算方法在中文文本场景下会导致约 30%~70% 的假阳性触发率（行 3433）。文档提出"建议使用轻量 Tokenizer（如 tiktoken/jtokkit）对 transcripts 做精确计数"（行 3434-3436），但紧接其后的伪代码（行 3441）仍使用 `estimateTokens(transcripts)` 的字符估算方法，未体现自述的改进建议。

实施者按伪代码编码将继承此假阳性问题，导致讨论结论能力频繁触发不必要的 LLM 前置压缩调用（每次压缩耗时 ~15s 超时窗口），使 P99 延迟显著高于预期。

**所在位置**：§4.1 行 3417-3441

**严重程度**：中等

**改进建议**：
- 方案 A：在 §4.1 伪代码中增加精确 Tokenizer 分支——先字符估算预检（快速过滤短文本），超过阈值后再用 jtokkit 精确计数，仅在精确计数 > 3000 时执行压缩
- 方案 B：在 §4.1 行 3441 的注释中补充"⚠️ 实施者建议在字符估算通过后增加 jtokkit 精确计数，将假阳性率降至接近 0%"

---

## 总结

本文档经过 21 轮迭代已非常成熟，整体质量较高。以上 5 个问题中，**问题 1** 最为严重——它涉及修订声明的可信度和实施者在 CallContext 上的编码方向选择，建议在下一轮修复中优先处理。**问题 2** 和 **问题 3** 属于文档一致性缺陷，修复成本低但影响实施效率。**问题 4** 和 **问题 5** 属于边界条件下的行为缺陷，在极端场景下影响可观测性和 P99 延迟。
