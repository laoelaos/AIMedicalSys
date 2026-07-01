根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### P1 [严重] 异步线程上下文传播未定义 — SecurityContext 和 RequestContext 在 supplyAsync 线程池中丢失
`AbstractCapabilityExecutor.execute()` 将整个管线通过 `CompletableFuture.supplyAsync()` 委派到 `LlmCallExecutor` 线程池执行。管线中多处依赖 Spring `ThreadLocal` 上下文：SecurityContextHolder（userId，传入 ExperimentManager.assign()）、RequestContextHolder（薄适配器 departmentId）、callerRole/callerId（指标记录）。ThreadLocal 在 supplyAsync 线程池中不可自动继承，导致 userId 回退为"SYSTEM"、departmentId 始终为 null、caller 信息缺失。
- **所在位置**：§4.1 `AbstractCapabilityExecutor.execute()` 模板方法（第 1144-1157 行）+ §3.1 UserId/SessionId 上下文来源（第 609-612 行）+ §3.5 字段填充策略（第 903 行）+ §3.1 薄适配器 departmentId 提取（第 647 行）
- **改进建议**：在 §6 中补充异步上下文传播机制，推荐方案 (c) 在 execute() 入口处提取完毕再传入 lambda，或方案 (b) 将异步边界下移到 LLM 调用层面。

### P2 [严重] LlmCallExecutor 线程池拒绝策略未定义 — 高并发下线程池满载后的行为不可控
`LlmCallExecutor` 线程池定义了核心线程和最大线程，但未定义拒绝策略。默认 `AbortPolicy` 在高并发时抛出 `RejectedExecutionException`，该异常在 `supplyAsync` 的 `CompletableFuture` 中未被捕获，导致 HTTP 500 或未处理的 `CompletionException`。与之对比，指标采集线程池已显式定义拒绝策略。
- **所在位置**：§3.2 LlmClient 线程模型（第 735 行）
- **改进建议**：为 `LlmCallExecutor` 显式定义拒绝策略（建议 `CallerRunsPolicy` 或 `DiscardPolicy` + 降级），同步补充 `queueCapacity` 定义。

### P3 [重要] doDegrade() 方法签名缺少 departmentId 参数 — 降级路径下 AiCallRecord 构造缺少科室标识
`doDegrade()` 方法签名缺少 `departmentId` 参数，但其内部调用 `AiCallRecord.degraded()` 时需要 departmentId。调用点（从 `execute()` 和 `doExecuteInternal()` 调用）均存在 departmentId 但未传递。薄适配器版的 `doThinDegrade` 则显式包含 departmentId，与标准版不一致。
- **所在位置**：§4.1 第 1153、1170、1176、1183 行调用点；§4.1 第 1189-1200 行方法体；§3.1 第 669 行薄适配器版签名
- **改进建议**：将 `doDegrade()` 方法签名扩展为 `doDegrade(startTime, degradeReason, request, capabilityId, departmentId)`，相应更新所有调用点。

### P4 [重要] AiCallRecord 工厂方法签名不完整 — failure() 和 degraded() 签名未覆盖 callerRole/callerId 字段
§3.5 定义三个工厂方法中，`success()`、`failure()`、`degraded()` 均未包含 `callerRole` 和 `callerId` 参数。字段填充策略声明这些字段来自 SecurityContext 提供了"机制说明"但未提供"代码入口"——工厂方法不接受这些参数，又无 setter/Builder 模式补充填充。
- **所在位置**：§3.5 工厂方法签名（第 884-898 行）；§3.5 字段填充策略第 903 行
- **改进建议**：在所有三个工厂方法签名中增加 `callerRole` 和 `callerId` 参数（推荐方案 a），迫使调用方明确处理 caller 信息的提取时机。

### P5 [重要] 薄适配器 departmentId 提取在 supplyAsync 异步上下文中无效
§3.1 薄适配器 `doExtractDepartmentId()` 依赖 Spring `RequestContextHolder` 获取 HTTP `X-Department-ID` Header。但此方法在 `supplyAsync` lambda 内部调用（第 1150 行），`RequestContextHolder` 不自动传播到线程池线程，导致薄适配器在异步执行中始终无法获取 departmentId。
- **所在位置**：§3.1 第 647 行 `doExtractDepartmentId()` 伪代码；§4.1 第 1149-1150 行调用位置
- **改进建议**：在 `AiOrchestrator.handle()` 的委托入口处提取 departmentId 并注入到 `AiRequestBase.departmentId`（与 P1 建议协同）。

### P6 [重要] ModelEndpointHealthManager 状态机缺少 UNAVAILABLE→DEGRADED 和 CONNECTED→UNAVAILABLE 的直接转换路径
状态模型定义 `CONNECTED ←→ DEGRADED ←→ UNAVAILABLE`，但 CONNECTED 到 UNAVAILABLE 的路径未明确（是否需经过 DEGRADED），UNAVAILABLE 恢复到 DEGRADED 的路径未定义（探测成功是回到 CONNECTED 还是先 DEGRADED）。当端点突然彻底宕机时，需先满足耗时阈值再满足失败次数阈值，可能延迟熔断。
- **所在位置**：§3.2 第 750-756 行
- **改进建议**：补充端点健康管理器的状态转换表，明确 CONNECTED → UNAVAILABLE（连续 N 次调用失败）和 UNAVAILABLE → CONNECTED（探测调用成功）路径及各转换的触发条件与阈值。

### P7 [中等] 熔断器与端点健康管理器独立探测可能产生冲突
`CircuitBreakerDegradationStrategy` 和 `ModelEndpointHealthManager` 各自维护独立的探测计时器（OPEN→HALF_OPEN 30 秒窗口 vs UNAVAILABLE 每 30 秒探测），独立运行可能导致熔断器允许探测但端点管理器阻止，或反之，浪费探测窗口。
- **所在位置**：§3.2 第 756 行（探测调用触发机制）+ 第 763-766 行（交互优先级）
- **改进建议**：统一探测机制——将端点健康探测决策权归并到一个组件，建议 `ModelEndpointHealthManager` 作为单一信源，`CircuitBreakerDegradationStrategy` 委托其探测结果。

### P8 [中等] AiCallRecord 未记录 Prompt 版本号 — A/B 实验效果分析缺少关键维度
`AiCallRecord` 的字段表和工厂方法中均未包含 `promptVersion` 字段，无法在事后分析中关联调用结果质量与使用的 Prompt 版本，与 §1.1 设计目标"实验结果可观测"不完全匹配。
- **所在位置**：§3.5 AiCallRecord 字段表（第 860-882 行）；AiCallLogEntity 字段表（第 943-966 行）
- **改进建议**：在 `AiCallRecord` 和 `AiCallLogEntity` 中补充 `promptVersion` 字段，同步更新工厂方法和 JPA 映射。

### P9 [一般] AiCallRecord.degraded() 工厂方法缺少 outputSummary 参数 — 本地规则降级结果无法记录
`degraded()` 工厂方法签名包含 `inputSummary` 但不包含 `outputSummary`，而降级路径中的本地规则降级确实产生了业务输出。与之对比，`success()` 工厂方法同时包含 `inputSummary` 和 `outputSummary`。
- **所在位置**：§3.5 第 895-898 行 `degraded()` 工厂方法签名；§4.1 第 1192-1196 行降级路径
- **改进建议**：在 `degraded()` 工厂方法签名中补充 `String outputSummary` 参数（可空），并在 `doDegrade()` 中获取本地规则降级的输出摘要后传入。

### P10 [一般] 需求响应维度：未显式验证与 Phase0/Phase1ABD OOD 设计风格的一致性
用户需求明确要求"参考已有的 Phase0、Phase1ABD 的 OOD 设计成果，保持设计风格和结构一致性"。当前文档未引用或映射现有的 Phase0/Phase1ABD 设计结构，缺少对"一致性"的显式承诺或对比。
- **所在位置**：全局（缺少跨阶段设计风格一致性声明或映射表）
- **改进建议**：在 §1 概述部分增加对 Phase0/Phase1ABD 设计风格和结构的引用说明，或在 §7 设计决策表中补充阶段间设计风格一致性决策记录。

### P11 [一般] ModelRoute 缺少端点级认证和超时配置定义
`ModelRoute` 定义为封装 `endpointId`、`modelId`、`endpoint` URL、`clientType`、生成参数默认值与权重，但未包含 API 认证信息、端点级超时配置、速率限制配置。这导致开发者在实现 `HttpApiLlmClient` 时无法确定认证密钥如何传递。
- **所在位置**：§3.2 ModelRoute 定义（第 787 行）；§9.5 YAML 路由配置示例（第 1459-1461 行）
- **改进建议**：扩展到 `ModelRoute` 值对象中增加 `authentication`、`timeout` 字段，或明确说明密钥通过 Vault/配置中心按 `endpointId` 查询。

## 历史迭代回顾

### 已解决的问题
无 — 本轮全部 11 个问题均在第 8 轮诊断中已被识别，本轮为第 9 轮回顾，尚无问题被标记为已解决。

### 持续存在的问题（需重点解决）
以下 11 个问题在第 8 轮和第 9 轮诊断中反复出现，表明此前多轮修复未彻底根除，需在本轮重点解决：

1. **异步线程上下文传播未定义**（P1）— 第 8 轮问题 1，本轮 P1
2. **LlmCallExecutor 线程池拒绝策略未定义**（P2）— 第 8 轮问题 2，本轮 P2
3. **doDegrade() 方法签名缺少 departmentId**（P3）— 第 8 轮问题 3，本轮 P3
4. **AiCallRecord 工厂方法签名不完整**（P4）— 第 8 轮问题 4，本轮 P4
5. **薄适配器 departmentId 提取在异步上下文无效**（P5）— 第 8 轮问题 5，本轮 P5
6. **ModelEndpointHealthManager 状态机缺少转换路径**（P6）— 第 8 轮问题 6，本轮 P6
7. **熔断器与端点健康管理器独立探测冲突**（P7）— 第 8 轮问题 7，本轮 P7
8. **AiCallRecord 未记录 Prompt 版本号**（P8）— 第 8 轮问题 8，本轮 P8
9. **AiCallRecord.degraded() 缺少 outputSummary**（P9）— 第 8 轮问题 9，本轮 P9
10. **未显式验证与 Phase0/Phase1ABD 风格一致性**（P10）— 第 8 轮问题 10，本轮 P10
11. **ModelRoute 缺少认证和超时配置**（P11）— 第 8 轮问题 11，本轮 P11

### 新发现的问题
无 — 本轮诊断与第 8 轮诊断覆盖的问题范围一致，未识别到新的质量问题。

## 上一轮产出路径
C:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606271627_phase5_pkgG_ood/a_v8_copy_from_v7.md

## 用户需求
C:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606271627_phase5_pkgG_ood/requirement.md
