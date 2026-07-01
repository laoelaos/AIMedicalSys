# Phase 5 包 G OOD 设计审查诊断报告（v14）

审查时间：2026-06-27
审查轮次：第 14 轮
审查维度：需求响应充分度、事实/逻辑正确性、深度与完整性（侧重内部审议未覆盖维度）

---

## 发现问题

### 1. [严重] `AbstractCapabilityExecutor.execute()` 中 request 变量重赋值后捕获到 lambda，造成 Java 编译错误

**问题描述**：`AbstractCapabilityExecutor.execute()` 模板方法伪代码（§4.1 第 1510 行）执行 `request = objectMapper.convertValue(request, request.getClass())` 对局部变量 `request` 进行了重赋值。随后该变量在第 1525 行被 `supplyAsync(() -> { return doExecuteInternal(..., request, ...) })` 和第 1535 行被 `.exceptionally(ex -> { ... doDegrade(..., request, ...) })` 两个 lambda 同时捕获。Java 语言规范要求被 lambda 捕获的局部变量必须是 effectively final（初始化后不再赋值），此处因重赋值而不满足此条件，代码将无法通过编译。

此问题已在迭代历史（v14 修订说明 "问题6" 中因 `inputSummary` 捕获问题触及过类似场景，但 `request` 自身的捕获问题被遗漏）。

**所在位置**：§4.1 `AbstractCapabilityExecutor.execute()` 伪代码，第 1510 行（重赋值）+ 第 1525 行（supplyAsync lambda）+ 第 1535 行（exceptionally lambda）

**严重程度**：严重

**改进建议**：将防御性拷贝结果存入新局部变量而非重赋给 `request`，例如 `defensiveCopy = objectMapper.convertValue(request, request.getClass())`，所有后续步骤（inputSummary 计算、降级预检、doExecuteInternal 调用、exceptionally 回调）均使用 `defensiveCopy`，保持原 `request` 不变以符合 effectively final 要求。

---

### 2. [严重] 薄适配器提取方法命名与基类模板方法签名不匹配，子类无法正确重写

**问题描述**：§3.1 薄适配器 CapabilityExecutor 的公共字段提取策略中定义了 `extractVisitId()`（第 717 行）和 `extractPatientId()`（第 721 行）两个方法，但 `AbstractCapabilityExecutor` 模板方法模式中（§3.1 第 845-857 行以及 §4.1 第 1503-1504 行）要求重写的方法签名是 `doExtractVisitId(request)` 和 `doExtractPatientId(request)`——带 `do` 前缀且接受 `request` 参数。薄适配器中定义的 `extractVisitId()` 与 `doExtractVisitId(request)` 在名称和参数列表上均不一致，无法通过 Java 方法重写机制正确绑定。实现者若按薄适配器伪代码片段实现，将不会覆盖基类方法的默认行为（从 `AiRequestBase` 获取字段），导致 Phase 4 薄适配器 DTO 的就诊上下文全部丢失。

同时，`doExtractDepartmentId(request)` 在同段中写对了带 `do` 前缀和 `request` 参数，而 `extractVisitId()`/`extractPatientId()` 缺少，属于同一段落内的笔误。

**所在位置**：§3.1 薄适配器伪代码（第 717-723 行）vs §4.1 execute() 模板方法（第 1503-1505 行）

**严重程度**：严重

**改进建议**：将薄适配器中的 `extractVisitId()` 和 `extractPatientId()` 统一修正为 `doExtractVisitId(request)` 和 `doExtractPatientId(request)`，签名与 `AbstractCapabilityExecutor` 的模板方法保持一致。同时补充 `doExtractSessionId(request)` 的重写说明（当前薄适配器段缺少 `sessionId` 的独立提取方案描述）。

---

### 3. [严重] Maven 依赖作用域在 §2.2 和 §3.1 中存在矛盾，实现者无法确定正确配置

**问题描述**：§2.2 模块依赖方向（第 169 行）明确冻结决策为 `provided` 作用域，并给出了三条选择理由。但 §3.1 薄适配器 Phase 4 业务服务注入方式代码示例（第 675 行注释）声明"在 Maven pom.xml 中声明对 Phase 4 各业务模块的 **compile** 依赖"。两处对同一依赖的作用域定义直接矛盾，实现者无法判断应使用 `provided` 还是 `compile`。若按 `compile` 实现则违背 §2.2 的松耦合设计决策；若按 `provided` 实现则与 §3.1 的示例注释冲突。

**所在位置**：§2.2 第 169 行（`provided`）vs §3.1 第 675 行注释（`compile`）

**严重程度**：严重

**改进建议**：统一为 §2.2 决策的 `provided` 作用域，将 §3.1 第 675 行注释中的 `compile` 改为 `provided`，并简要注引用 §2.2 的依赖规则。

---

### 4. [重要] 降级预检循环的 degrade reason 取值方式与 DegradationReason 枚举体系不一致

**问题描述**：§4.1 降级预检循环（第 1519-1521 行）中，当策略 `shouldDegrade()` 返回 true 时，degrade reason 取值为 `strategy.getClass().getSimpleName()`——即 Java 类名字符串（如 `"CircuitBreakerDegradationStrategy"`、`"TimeoutDegradationStrategy"`）。但 §3.8 新定义了 `DegradationReason` 枚举（第 1437-1447 行），其中 `CIRCUIT_BREAKER_OPEN`、`TIMEOUT` 等枚举常量专门用于标识相同场景。§5.1 错误分类表也引用这些枚举常量作为正式原因。两套取值体系（类名 vs 枚举常量）并存导致降级日志和指标中的原因值不一致，影响告警聚合和故障定位。开发者无法确定降级预检路径中应使用哪个值。

**所在位置**：§4.1 第 1520-1521 行（降级预检 degrade reason 取值）vs §3.8 DegradationReason 枚举定义（第 1437-1447 行）vs §5.1 错误分类表

**严重程度**：重要

**改进建议**：在降级预检循环中维护策略类名到 `DegradationReason` 枚举的映射，或修改 `DegradationReason` 枚举增加 `STRATEGY_TRIGGERED` 通用常量并附加策略类名作为详情。至少应在 §4.1 伪代码注释中说明降级预检路径的策略类名取值方式与 `DegradationReason` 枚举的关系，避免实现者猜测。

---

### 5. [重要] `capabilityTimeoutConfig` 字段在类图和构造器中均未定义，实现者无法定位配置来源

**问题描述**：`AbstractCapabilityExecutor.execute()` 模板方法（§4.1 第 1530 行）直接引用 `capabilityTimeoutConfig.getOrDefault(capabilityId, Duration.ofSeconds(60))`，但 §2.3 类图中 `AbstractCapabilityExecutor`（第 218-229 行）只声明了方法，未声明任何字段。文档§3.1 文本（第 885 行）提到"存储在 `capabilityTimeoutConfig`（`Map<String, Duration>`），通过 `AiPlatformConfig` 从 YAML 绑定"，但未定义该字段如何注入到 `AbstractCapabilityExecutor`（构造器注入？`@Value` 绑定？父类字段？）。实现者在阅读类图和模板方法时将无法确定该变量的来源，需要跨章节拼凑信息才能理解装配路径。

**所在位置**：§2.3 AbstractCapabilityExecutor 类图（第 218-229 行）vs §4.1 第 1530 行（引用未定义字段）

**严重程度**：重要

**改进建议**：在 §2.3 类图 `AbstractCapabilityExecutor` 中补充 `-capabilityTimeoutConfig: Map<String, Duration>` 字段，或在抽象骨架的职责描述中（§3.1 第 777-886 行）增加一个明确的"字段来源"子段，说明 `capabilityTimeoutConfig` 的注入方式和默认值策略。

---

### 6. [重要] `AiOrchestrator.handle()` 伪代码行号错乱，指示编辑失误

**问题描述**：§4.1 `AiOrchestrator.handle()` 伪代码第 1489 行的行号标记为"26."，但前一行第 1488 行已到"29."，行号倒退回 26 重新开始。这表明在历次修订中该段伪代码经过多次插入/删除操作后行号未正确更新。虽然不直接影响逻辑，但伪代码行号不一致降低了契约作为"可参考代码"的权威性，实现者可能质疑文档内部引用（如 §5.1 错误分类表对"§4.1 对应实现位置"的交叉引用）的准确性。

**所在位置**：§4.1 `AiOrchestrator.handle()` 伪代码第 1489 行

**严重程度**：重要

**改进建议**：修正行号，使 catch 块内的步骤编号从 10 (try-catch) 之后连续排列（10→10.1, 10.2...或直接 11, 12...），确保编号单调递增不重复。

---

### 7. [中等] 薄适配器 `doExecuteInternal()` 在 `llmCallExecutor` 线程中阻塞等待 `ForkJoinPool` 任务，潜在线程饥饿风险

**问题描述**：薄适配器 `doExecuteInternal()`（§3.1 第 736-738 行）使用 `CompletableFuture.supplyAsync(() -> phase4ServiceDelegate.execute(request))`（默认 ForkJoinPool）提交任务后，在当前线程（即 `llmCallExecutor` 线程池中的线程）调用 `delegateFuture.get(thinAdapterTimeout.toMillis(), TimeUnit.MILLISECONDS)` 同步阻塞等待结果。v14 修订解决了"嵌套提交到同一线程池"的死锁问题，但引入了新风险：`llmCallExecutor` 的线程数有限（核心线程 = 可用模型端点数，通常 2~5），如果多个薄适配器请求同时执行，`llmCallExecutor` 线程将全部被 `get()` 阻塞等待 ForkJoinPool 1完成委托调用，无法处理普通 LLM 调用任务，造成 LLM 调用路径的线程饥饿。文档未评估此场景。

**所在位置**：§3.1 薄适配器 `doExecuteInternal()` 伪代码（第 736-738 行）

**严重程度**：中等

**改进建议**：在 §3.1 或 §6.1 中评估此阻塞等待模式对 `llmCallExecutor` 线程池可用性的影响，并给出约束条件（如 `corePoolSize >= thinAdapterCount + 1`）。或考虑使用独立线程池处理薄适配器委托，而非共用 `llmCallExecutor`。

---

## 整体质量评价

设计文档经过 14 轮迭代后需求覆盖完成，整体架构合理。以上问题中，**问题 1**（request 变量重赋值导致的 lambda 捕获编译错误）和 **问题 3**（Maven 作用域矛盾）是阻碍启动编码的严重缺陷；**问题 2**（薄适配器方法命名不一致）将直接导致 Phase 4 降级路径就诊上下文丢失。建议修复后进入下一轮审议。
