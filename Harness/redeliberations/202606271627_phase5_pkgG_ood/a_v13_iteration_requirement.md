根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### [重要] Phase 4 薄适配器 Maven 依赖范围未明确
- **位置**：§2.2 依赖规则段（第 169 行 "test 范围或 compile 范围"）
- **问题**：文档给出了 `test` 和 `compile` 两个不等价选项且未做确定性决策。`test` 作用域会导致运行期 ClassLoader 无法加载 Phase 4 服务接口；`compile` 作用域则使 `ai-impl` 对 Phase 4 业务模块建立编译期强耦合。
- **建议**：冻结选择——推荐使用 `provided` 或 `compile` 作用域并给出选择理由。若保持 `compile`，需在 §10 协作边界段显式记录耦合约束的治理规则；若倾向松耦合，建议在 `ai-impl` 内部为每项薄适配器定义 SPI 接口。

### [重要] 薄适配器超时路径下 `CompletableFuture.cancel(true)` 无法真正中止 Phase 4 服务执行
- **位置**：§3.1 薄适配器伪代码第 738 行 `delegateFuture.cancel(true)`；§9.5 YAML 配置薄适配器默认超时 30s
- **问题**：`CompletableFuture.cancel(true)` 仅将 Future 标记为 cancelled 状态，不会产生线程中断或任务取消效果。Phase 4 服务将继续运行至完成，资源被浪费。
- **建议**：删除 `delegateFuture.cancel(true)` 调用，改用日志 WARN 记录超时并注明"Phase 4 服务将在后台继续执行至完成"。在超时说明段补充资源消耗评估。

### [重要] `doDegrade()` 方法签名缺少 `promptVersion` 参数，实验分流后的降级场景丢失实验分组关联分析能力
- **位置**：§4.1 `doDegrade()` 方法定义第 1508-1527 行；§4.1 三个调用点（第 1482、1488、1497 行）
- **问题**：`doDegrade()` 在降级记录中恒为 `promptVersion=null`，导致无法分析「实验 A 分组是否因解析失败率高于对照组而降级更多」，§3.5 中 `AiCallRecord.promptVersion` 字段在降级场景被架空。
- **建议**：在 `doDegrade()` 方法签名中增加 `Integer promptVersion` 参数，调用点对应调整。同步更新 §2.3 类图中 `AbstractCapabilityExecutor.doDegrade()` 方法签名。

### [中等] `AiOrchestrator.handle()` 中未注册能力标识的异常传播路径与异步返回契约不一致
- **位置**：§4.1 `AiOrchestrator.handle()` 第 1386 行 `throw IllegalStateException`（在 try 外）
- **问题**：同步抛出的异常不会通过 `CompletableFuture.completedFuture()` 包装，破坏调用方统一的 `CompletableFuture` 契约。
- **建议**：将 executor null 检查移入 try 块，同样走 `CompletableFuture.completedFuture(AiResult.failure(...))` 路径。

### [中等] `ExperimentAssignment` 构造方式设计文档未定义
- **位置**：§3.4 `ExperimentAssignment` 段落；§4.1 第 1471 行
- **问题**：仅以字段表形式定义，未定义构造器签名、Builder 模式或工厂方法，但多处伪代码中使用构造函数实例化。
- **建议**：在 §3.4 中显式定义 `ExperimentAssignment` 的构造方式——建议全参数构造器 + 无参默认工厂方法。

### [中等] `ModelRoute` 密钥获取接口未定义，无法指导 LlmClient 实现
- **位置**：§3.2 `ModelRoute` 字段扩展表（第 1008-1011 行）及认证分离理由段
- **问题**：声明凭据通过 `endpointId` 从 Vault/配置中心按需查询，但未定义凭据查询接口、凭据缓存策略、Vault 不可达时的降级行为。
- **建议**：新增 `CredentialProvider` 接口定义，定义密钥缓存策略，定义 Vault 不可达时的回退行为，在 §3.2 `LlmClient` 职责描述中增加密钥查询时序说明。

### [一般] 薄适配器成功路径 `retryCount=0` 硬编码降低可观测性
- **位置**：§3.1 薄适配器伪代码（第 752 行 `retryCount=0`）
- **问题**：薄适配器绕过 `LlmClient` 直接委托 Phase 4 服务，Phase 4 服务内部重试无法被底座指标系统感知。
- **建议**：在 §3.1 薄适配器说明段中显式记录此限制，或尝试从 Phase 4 服务接口中提取重试信息。

### [一般] `Experiment` 数据生命周期未涉及
- **位置**：§3.4 `Experiment` 段落及 `ExperimentRepository` 定义
- **问题**：定义了状态模型和定时结束，但未定义 COMPLETED 后的数据保留策略、历史分析支持能力和数据清理机制。
- **建议**：补充实验数据生命周期说明——明确保留策略、补充按时间范围查询方法、补充索引策略。

## 历史迭代回顾

- **已解决的问题**：迭代 1~11 共 82 条审查意见已在上一轮（v12）全部在对应修订说明中闭环（组件B诊断报告确认）。
- **持续存在的问题**：
  - retryCount 相关（Issue 7）——迭代 9 #2（未定义变量）、迭代 11 #1（LlmResponse 缺少 retryCount）持续演进后，本轮仍存在薄适配器场景特有问题
  - 异常传播（Issue 4）——迭代 4 #12（handle 缺少异常捕获）、迭代 10 #3（catch 块丢失上下文）有持续关联，本轮从新的异步契约破坏角度切入
- **新发现的问题**：Issue 1/2/3/5/6/8 均为此前迭代未覆盖的设计边缘场景或遗漏约束（组件B诊断报告确认）。

## 上一轮产出路径
C:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606271627_phase5_pkgG_ood/a_v12_copy_from_v11.md

## 用户需求
C:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606271627_phase5_pkgG_ood/requirement.md
