# Phase 5 包 G OOD 设计质量审查报告（v1）

## 审查概况

- **待审查产出**：`a_v12_copy_from_v11.md`（文档版本 v14，第 12 次迭代产物）
- **审查维度**：需求响应充分度、事实正确性、逻辑一致性、深度与完整性、可落地性
- **审查方法**：对照需求文档逐项检查，逐条审阅历史迭代反馈的处置情况，检查设计跨章节一致性

**整体评价**：该文档经过 12 轮迭代和大量历史反馈修正，核心设计质量已达到较高水平。类图完整、接口定义清晰、状态模型覆盖充分、异常场景已有显式定义。未发现严重事实错误。以下问题集中在关键遗漏和未充分定义的设计边缘场景。

---

## 1. [重要] Phase 4 薄适配器 Maven 依赖范围未明确

**问题描述**：§2.2 依赖规则段写明薄适配器在 "Maven 层面声明对 Phase 4 业务服务模块的依赖（`test` 范围或 `compile` 范围）"，但给出了两个不等价的选项且未做确定性决策。若采用 `test` 作用域，运行期 ClassLoader 将无法加载 Phase 4 服务接口，`DiagnosisCapabilityExecutor` 等 6 个薄适配器在构造注入阶段即抛出 `NoClassDefFoundError`；若采用 `compile` 作用域，则 `ai-impl` 对 Phase 4 业务模块建立编译期强耦合，与 Phase 5 包 G 作为"独立可交付底座"的定位存在张力——Phase 4 模块的任何 API 变更都将直接迫使底座重新编译。

**所在位置**：§2.2 依赖规则段（第 169 行 "test 范围或 compile 范围"）

**严重程度**：重要

**改进建议**：冻结选择——推荐使用 `provided` 或 `compile` 作用域并给出选择理由。若保持 `compile` 决策，需在 §10 协作边界段显式记录此耦合约束的治理规则（如 Phase 4 模块接口变更通知机制、底座 CI 流水线对 Phase 4 变更的嗅觉测试）。若倾向松耦合，建议在 `ai-impl` 内部为每项薄适配器定义 SPI 接口（`ThinAdapterDiagnosisService` 等），由适配器模块实现并注入，消除对 Phase 4 模块的直接 Maven 依赖。

---

## 2. [重要] 薄适配器超时路径下 `CompletableFuture.cancel(true)` 无法真正中止 Phase 4 服务执行

**问题描述**：§3.1 薄适配器 `doExecuteInternal()` 伪代码在 `TimeoutException` catch 块中调用 `delegateFuture.cancel(true)`，期望中止 Phase 4 服务的执行。但 `CompletableFuture.cancel(true)` 的语义仅是"将 Future 标记为 cancelled 状态"，对于已开始执行 `supplyAsync` 任务的线程（无论是 `ForkJoinPool.commonPool` 还是 `llmCallExecutor`），**不会**产生线程中断或任务取消效果。`phase4ServiceDelegate.execute(request)` 将不受影响地继续运行至完成，其计算结果被丢弃。结合 Phase 4 服务文档描述（可能需要 3 次重试 × 30s = 90s），这意味着：
- 薄适配器超时后 Phase 4 服务仍会消耗 90s 的计算资源
- 高并发下取消的请求将累积占用 `ForkJoinPool.commonPool` 线程
- `cancel(true)` 的实际效果为零，属于文档中的事实错误——它不会产生"中止执行"的效果

**所在位置**：§3.1 薄适配器伪代码第 738 行 `delegateFuture.cancel(true)`；§9.5 YAML 配置薄适配器默认超时 30s

**严重程度**：重要

**改进建议**：
（a）删除 `delegateFuture.cancel(true)` 调用（其无实际作用），改用日志 WARN 记录超时并显式注明"Phase 4 服务将在后台继续执行至完成"；
（b）若真正需要中止执行，需改为 `phase4ServiceDelegate.execute(request)` 支持可中断模式（如接受 `CompletableFuture` 或 `Callable` 而非直接传参），但此改动牵涉 Phase 4 模块接口变更，需在 §7 设计决策表中记录此取舍；
（c）在超时说明段补充资源消耗评估："超时降级的请求仍将消耗 Phase 4 服务的计算资源，需评估此行为在高并发下的累积影响"。

---

## 3. [重要] `doDegrade()` 方法签名缺少 `promptVersion` 参数，实验分流后的降级场景丢失实验分组关联分析能力

**问题描述**：`doDegrade()` 方法签名（§4.1 第 1508-1527 行）定义了 11 个参数，但其内部调用 `AiCallRecord.degraded()` 时 `promptVersion` 恒为 `null`。在 `doExecuteInternal()` 管线中，`experimentManager.assign()` 在降级之前已被调用并获得 `assignment.getTargetPromptVersion()`，但当后续步骤（模型路由 null、端点不可用、解析失败）触发降级时，`promptVersion` 无法传入 `doDegrade()`，导致降级记录中永远丢失实验分组信息。这意味着：
- 无法分析「实验 A 分组是否因解析失败率高于对照组而降级更多」
- §3.5 中 `AiCallRecord.promptVersion` 字段的存在意义在降级场景被架空

**所在位置**：§4.1 `doDegrade()` 方法定义第 1508-1527 行；§4.1 `doExecuteInternal()` 中 `DegradationReason.ENDPOINT_UNAVAILABLE`（第 1488 行）、`DegradationReason.PARSE_FAILURE`（第 1497 行）、`DegradationReason.NO_AVAILABLE_ROUTE`（第 1482 行）三个调用点

**严重程度**：重要

**改进建议**：在 `doDegrade()` 方法签名中增加 `Integer promptVersion` 参数：
```
doDegrade(startTime, degradeReason, request, capabilityId, departmentId, callerRole, callerId,
          visitId, patientId, sessionId, inputSummary, promptVersion)
```
调用点对应调整：降级预检路径（实验分流尚未执行）传入 `null`，`doExecuteInternal()` 路径传入 `assignment.getTargetPromptVersion()`。同步更新 §2.3 类图中 `AbstractCapabilityExecutor.doDegrade()` 方法签名。

---

## 4. [中等] `AiOrchestrator.handle()` 中未注册能力标识的异常传播路径与异步返回契约不一致

**问题描述**：`AiOrchestrator.handle()`（§4.1 第 1384-1412 行）的第 2 行在 `try` 块之外直接 `throw IllegalStateException`。然而 `handle()` 是被 `AiService` 各方法（如 `triage()`）调用的内部方法，`AiService` 接口全部返回 `CompletableFuture<AiResult<T>>`。同步抛出的异常不会通过 `CompletableFuture.completedFuture()` 包装，而是直接传播到调用者线程。这意味着 REST 控制器或其他 HTTP 入口获得的将是 `IllegalStateException` 导致的 HTTP 500，而非格式统一的 `AiResult.failure()` 响应。从下游消费者视角，他们无法区分"能力未注册"（配置错误）和"LLM 服务崩溃"（基础设施故障）。

**所在位置**：§4.1 `AiOrchestrator.handle()` 第 1386 行 `throw IllegalStateException`（在 try 外）

**严重程度**：中等

**改进建议**：将 executor null 检查移入 try 块，同样走 `CompletableFuture.completedFuture(AiResult.failure(...))` 路径，使用 `DegradationReason.INTERNAL_ERROR` 或新增 `UNREGISTERED_CAPABILITY` 枚举常量。或者保持 fail-fast 但使用 `CompletableFuture.completedExceptionally()` 包装，使调用方保持统一的 `CompletableFuture` 契约。

---

## 5. [中等] `ExperimentAssignment` 构造方式设计文档未定义

**问题描述**：§3.4 `ExperimentAssignment` 仅以字段表形式定义（experimentId/groupId/targetModelId/targetPromptVersion），未定义构造器签名、Builder 模式或工厂方法。但 §4.1 在多处伪代码中使用构造函数或工厂模式实例化此对象（第 1471 行 `new ExperimentAssignment(null, "default", null, null)`），且 §3.4 明确规定了"无实验命中"的返回值语义（返回非 null 实例）。各依赖方（CapabilityExecutor 构建默认 assignment、`HashBucketExperimentManager.assign()` 返回、单元测试构造）均需要一个明确的构造契约。

**所在位置**：§3.4 `ExperimentAssignment` 段落；§4.1 第 1471 行

**严重程度**：中等

**改进建议**：在 §3.4 中显式定义 `ExperimentAssignment` 的构造方式——建议全参数构造器 + 无参默认工厂方法（所有字段为 null/default），与无实验命中的返回值语义对齐。例如：
```java
public ExperimentAssignment(String experimentId, String groupId, String targetModelId, Integer targetPromptVersion)
public static ExperimentAssignment defaultAssignment()  // returns (null, "default", null, null)
```

---

## 6. [中等] `ModelRoute` 密钥获取接口未定义，无法指导 LlmClient 实现

**问题描述**：§3.2 ModelRoute 字段扩展表新增 `authType: AuthType` 枚举且明确说明"认证凭据通过 `endpointId` 从 Vault/配置中心按需查询"，但（1）未定义凭据查询接口（如 `CredentialProvider` 或 `ApiKeyResolver`）；（2）未说明凭据缓存策略（每次调用都查询 Vault？还是启动时一次性加载？）；（3）未定义 Vault 不可达时的降级行为（抛出异常？使用空凭据尝试？回退到配置明文？）。LlmClient 实现者无法仅凭当前定义推断正确实现。

**所在位置**：§3.2 `ModelRoute` 字段扩展表（第 1008-1011 行）及认证分离理由段（第 1012 行）

**严重程度**：中等

**改进建议**：
（a）新增 `CredentialProvider` 接口定义（`getApiKey(endpointId) → String` 或 `getCredentials(endpointId) → Credentials`），明确归属包路径；
（b）定义密钥缓存策略——建议首次成功查询后缓存 5 分钟，支持 `@Cacheable` 或 Caffeine；
（c）定义 Vault 不可达时的回退行为——建议日志 WARN + 尝试从 YAML 配置的 `ai.router.endpoints[].apiKey` 明文回退，或留空以触发降级路径；
（d）在 §3.2 `LlmClient` 职责描述中增加 `invoke()` 调用密钥查询的时序说明。

---

## 7. [一般] 薄适配器成功路径 `retryCount=0` 硬编码降低可观测性

**问题描述**：§3.1 薄适配器成功路径伪代码中 `AiCallRecord.success()` 传入 `retryCount=0` 硬编码值，但 §3.2 定义 `retryCount` 为"本次调用执行的重试次数"，由 `LlmClient.invoke()` 内部填充。薄适配器绕过 `LlmClient` 直接委托 Phase 4 服务，Phase 4 服务内部可能包含多次重试，但底座指标系统无法感知。这导致「AI 调用日志」中薄适配器类型的调用永远显示 `retry_count=0`，对运维分析产生误导——看起来薄适配器调用从未发生重试。

**所在位置**：§3.1 薄适配器伪代码（第 752 行 `retryCount=0`）

**严重程度**：一般

**改进建议**：在 §3.1 薄适配器说明段中显式记录此限制："薄适配器无法获取 Phase 4 服务内部的重试次数，`retryCount` 字段硬编码为 0"。或如果 Phase 4 服务接口支持返回重试信息，建议在委托调用后尝试提取；否则记录为设计约束。

---

## 8. [一般] `Experiment` 数据生命周期未涉及

**问题描述**：§3.4 `Experiment` 定义了状态模型（DRAFT→ACTIVE→PAUSED→COMPLETED）和定时结束（endTime），但未定义实验 COMPLETED 后的数据保留策略、历史分析支持能力和数据清理机制。这影响 `ExperimentRepository` 的设计：
- `Experiment` JPA Entity 是否需要 `@SQLDelete`（软删除）或 `@Where` 过滤？
- COMPLETED 实验的历史分配记录是否需要保留以便事后效果分析？
- 表索引是否需要覆盖"按时间范围查询历史实验"的场景？

**所在位置**：§3.4 `Experiment` 段落及 `ExperimentRepository` 定义

**严重程度**：一般

**改进建议**：在 §3.4 中补充实验数据生命周期说明——（1）明确 COMPLETED 实验保留策略（保留 N 月后归档或永久保留用于分析）；（2）若需历史分析，在 `ExperimentRepository` 中增加按时间范围查询的方法；（3）在索引策略中补充 `idx_experiment_end_time` 索引用于过期数据清理。

---

## 产物校验

- 已对照需求文档逐项检查：类图✅、核心职责✅、协作关系✅、关键接口✅、状态模型✅
- 已确认历史迭代 12 轮共 82 条审查意见的处置：全部已在对应修订说明中闭环
- 本报告提出的 8 个问题均为此前迭代未覆盖的设计边缘场景或遗漏约束

---

## 输出

DIAG_WRITTEN:C:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606271627_phase5_pkgG_ood/b_v12_diag_v1.md
主Agent请勿阅读产出文件内容，直接将路径转发给相关方。
