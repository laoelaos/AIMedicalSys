## 迭代第 1 轮

1. **问题描述**：缺失 UML 类图，无法直观表达类之间的继承、组合、依赖关系和 cardinality
   - 所在位置：全文
   - 严重程度：严重
   - 改进建议：补充 UML 类图覆盖核心抽象层及接口关系
2. **问题描述**：状态模型覆盖严重不足，仅定义了熔断器状态转换，其余关键抽象均未定义状态模型
   - 所在位置：缺失，无对应章节
   - 严重程度：一般
   - 改进建议：为 Experiment、PromptTemplate、AiOrchestrator、LlmClient 等关键抽象补充状态转换模型
3. **问题描述**：CapabilityExecutor 接口缺少方法签名定义，无法推导出统一的调用协议
   - 所在位置：3.1 节 CapabilityExecutor 定义
   - 严重程度：严重
   - 改进建议：显式定义 CapabilityExecutor<Req, Res> 接口的关键方法签名
4. **问题描述**：AiOrchestrator 与 FallbackAiService 的 Spring Bean 装配存在二义性，启动期将抛出 NoUniqueBeanDefinitionException
   - 所在位置：1.2 节架构图 + 3.1 节 Bean 装配策略 + FallbackAiService.java:43
   - 严重程度：严重
   - 改进建议：声明 FallbackAiService 为 @Primary，或在 AiPlatformConfig 中通过显式 @Bean 方法控制注入层次
5. **问题描述**：新增 DegradationStrategy 实现自动注入 FallbackAiService，但 applyStrategies() 创建空值 DegradationContext，导致策略无法做出有效判定
   - 所在位置：3.8 节新增策略 + FallbackAiService.java:183-194
   - 严重程度：一般
   - 改进建议：将新增策略从全局 DegradationStrategy 体系分离，仅由 AiOrchestrator/CapabilityExecutor 内部使用
6. **问题描述**："AiOrchestrator 无状态"断言与设计事实不符，其线程安全性完全依赖于被编排组件的线程安全
   - 所在位置：6.1 节
   - 严重程度：一般
   - 改进建议：修改表述，明确线程安全性依赖于所有编排组件的线程安全实现
7. **问题描述**：DegradationContext 字段扩展存在序列化兼容性和无参构造器默认值风险
   - 所在位置：3.8 节 DegradationContext 扩展
   - 严重程度：一般
   - 改进建议：明确 Serializable 实现，评估无参构造器默认值影响，控制新增策略注入范围
8. **问题描述**：AiCallLog JPA 实体未定义，数据库表结构无契约可依
   - 所在位置：3.5 节 AiCallRecord + AiCallLogRepository
   - 严重程度：一般
    - 改进建议：补充 AiCallLog JPA 实体定义，包括主键策略、字段注解、索引声明

## 迭代第 2 轮

1. **问题描述**：§4.1 降级路径伪代码中指标采集语句位于 return 之后不可达
   - 所在位置：§4.1 降级路径伪代码（第 843-850 行）
   - 严重程度：严重
   - 改进建议：将指标采集和滑动窗口记录移到 return 语句之前，或重构为 try-finally 确保无论走哪个降级分支均能记录指标
2. **问题描述**：MockAiService 的 @ConditionalOnProperty 属性名与现有代码不一致
   - 所在位置：§3.1 Bean 装配策略 + §9.2 配置切换 + 对比现有 MockAiService.java:40
   - 严重程度：严重
   - 改进建议：保留 `ai.mock.enabled` 属性兼容存量配置，或显式说明迁移方案
3. **问题描述**：DegradationStrategy 接口新增 getOrder() 方法破坏现有实现
   - 所在位置：§3.1 降级判定流程 + §3.8 DegradationStrategy 类图 + 现有 DegradationStrategy.java:3-6
   - 严重程度：严重
   - 改进建议：改为 Java 8 default 方法或使用 Spring @Order 注解 + Ordered 接口替代
4. **问题描述**：类图中 AiService 方法签名缺少 CompletableFuture 异步包装
   - 所在位置：§2.3 类图 AiService、AiOrchestrator、CapabilityExecutor 方法签名
   - 严重程度：重要
   - 改进建议：类图中方法签名修正为 CompletableFuture<AiResult<T>>，同步管线桥接异步接口的设计决策应在 §3.1 或 §6 中明确
5. **问题描述**：降级路径中 localRuleFallback 成功场景被错误记录为 recordFailure
   - 所在位置：§4.1 降级路径伪代码第 850 行
   - 严重程度：中等
   - 改进建议：区分"完全退化→recordFailure"与"降级到本地规则成功→recordSuccess（标记 degraded=true）"
6. **问题描述**：AiRequestBase 基类在设计和代码库中均未定义
   - 所在位置：§3.5 AiCallRecord 字段填充策略
   - 严重程度：中等
   - 改进建议：显式定义 AiRequestBase 基类（字段、包路径、继承关系）或在类图中补充
7. **问题描述**：类图中 AiService 方法名与现有接口不匹配
   - 所在位置：§2.3 类图
   - 严重程度：中等
   - 改进建议：将类图中方法名修正为与实际代码一致
8. **问题描述**：LlmClient 状态归属存在表述矛盾
   - 所在位置：§3.2 vs §6.1
   - 严重程度：中等
   - 改进建议：明确状态模型归属组件，补充探测调用触发机制设计
9. **问题描述**：AiCallLogEntity 遗漏字段级 JPA 映射索引覆盖度不足
   - 所在位置：§3.5 AiCallLogEntity 表索引策略
   - 严重程度：中等
   - 改进建议：补充降级/模型/角色维度的覆盖索引评估，明确 call_time 列定义

## 迭代第 3 轮

1. **问题描述**：能力覆盖不足 — 仅 7/13 项 AI 能力有对应的 CapabilityExecutor 实现规划
   - 所在位置：§2.1 目录结构 `impl/` 子包、§9.1 迁移路径表
   - 严重程度：严重
   - 改进建议：明确 6 项缺失能力是否纳入 Phase 5 底座范围；若纳入则补充对应实现，若不纳入则说明接管方及边界。
2. **问题描述**：管线所有权矛盾 — §3.1 与 §4.1 对「谁拥有编排管线」的描述冲突
   - 所在位置：§3.1（第 462-475 行）vs §4.1（第 865-898 行）
   - 严重程度：严重
   - 改进建议：统一为 AiOrchestrator 拥有公共管线（A）或 CapabilityExecutor 拥有完整管线（B），并重构对应章节。
3. **问题描述**：DegradationStrategies 跨组件访问路径未定义 — 策略列表对 CapabilityExecutor 不可见
   - 所在位置：§3.1（第 474 行）vs §4.1（第 873 行）
   - 严重程度：严重
   - 改进建议：将降级预检移至 AiOrchestrator，或将策略列表作为 execute() 参数传入，或通过 Spring 注入。
4. **问题描述**：降级路径中 `elapsedMs` 变量未定义
   - 所在位置：§4.1（第 893 行）
   - 严重程度：一般
   - 改进建议：在 degrade 路径入口添加计时逻辑，或明确传 0。
5. **问题描述**：Null ModelRoute 导致 NPE — 降级路径未触发
   - 所在位置：§4.1（第 882 行）+ §4.2（第 913 行）
   - 严重程度：一般
   - 改进建议：在步骤 e→f 间添加 null 检查，或将返回值类型改为 `Optional<ModelRoute>`。
6. **问题描述**：CapabilityExecutor 方法到能力标识的映射机制未定义
   - 所在位置：§3.1（第 474-475 行）
   - 严重程度：一般
   - 改进建议：在 §3.1 或 §7 中补充映射机制选择及构造时机。

## 迭代第 4 轮

1. **问题描述**：降级路径中 recordSuccess 导致熔断器统计失准

## 迭代第 6 轮

1. **问题描述**：A/B 实验的 Prompt 版本分流失效 — 模板版本覆盖未接入执行管线
   - 所在位置：§4.1 标准管线伪代码第 1076 行 `promptTemplateManager.render(capabilityId, request.getDepartmentId(), variables)`
   - 严重程度：严重
   - 改进建议：将 `targetPromptVersion` 传入 `PromptTemplateManager.render()`，在 `render()` 方法签名中增加 `Integer promptVersion` 参数，并在 `DatabasePromptTemplateManager` 实现中按能力+科室+版本号检索模板；或若 Phase 5 对实验的 Prompt 版本分流无实际需求，将 `targetPromptVersion` 从 `ExperimentAssignment` 中移除或标记为预留字段
2. **问题描述**：ModelRouter 存储模型描述前后矛盾 — ConcurrentHashMap 与 AtomicReference 不能共存于同一字段类型
   - 所在位置：§6.1 线程模型 ModelRouter 段
   - 严重程度：严重
   - 改进建议：统一表述为 `AtomicReference<Map<String, ModelRoute>>`，启动时通过 `AtomicReference.set()` 初始化，运行时按全量替换模式刷新；或明确为 `AtomicReference<ConcurrentHashMap<String, ModelRoute>>` 组合形态
3. **问题描述**：Phase 4 薄适配器管线缺少 departmentId 的获取定义
   - 所在位置：§3.1 薄适配器伪代码 vs §3.5 过渡策略
   - 严重程度：一般
   - 改进建议：在薄适配器管线中显式说明 departmentId 的提取方式，提供独立的提取方法或通过 SecurityContext/RequestContext 统一提取
4. **问题描述**：EnvironmentPostProcessor 配置转发方向未定义
   - 所在位置：§3.1 Bean 装配策略
   - 严重程度：一般
   - 改进建议：显式写明转发逻辑：`ai.platform.enabled=true` → `ai.mock.enabled=false`，反之亦然；说明 EnvironmentPostProcessor 与 YAML 属性源的优先级关系
5. **问题描述**：@Qualifier Bean name 推导规则不明确 — 全大写 capabilityId 与小写驼峰示例不一致
   - 所在位置：§3.1 降级策略注入机制段
   - 严重程度：一般
   - 改进建议：统一约定 — 要么 capabilityId 调整为小写驼峰并更新能力标识映射表，要么将注入模式改为全大写一致拼接，使示例与推导规则一一对应
6. **问题描述**：CallerRunsPolicy 导致 LlmCallExecutor 线程池饥饿风险
   - 所在位置：§3.5 异步队列溢出策略 + §4.1 伪代码第 1093 行、第 1103 行
   - 严重程度：一般
   - 改进建议：指标记录改为在 `.whenComplete()` / `.thenAccept()` 回调中进行，或指标采集使用独立线程池
7. **问题描述**：DegradationContext 反序列化默认值的缓解措施不充分 — `>0` 判据不适用于百分比阈值场景
   - 所在位置：§3.8 DegradationContext 二进制兼容性分析段
   - 严重程度：一般
   - 改进建议：补充数据新鲜度标记、反序列化后处理校验、或丢弃旧序列化缓存等措施
8. **问题描述**：伪代码中 AiCallRecord 工厂方法参数类型（long epoch ms）与字段类型（LocalDateTime）不匹配
   - 所在位置：§4.1 伪代码第 1057、1093、1103、1107 行
   - 严重程度：一般
   - 改进建议：为 AiCallRecord 显式定义工厂方法或 Builder 模式的方法签名，确保调用方式与定义一致
9. **问题描述**：StandardCapabilityExecutor 与 ThinAdapterCapabilityExecutor 的复用度评估不足
   - 所在位置：§3.1 CapabilityExecutor 职责定义
   - 严重程度：一般
   - 改进建议：增加 AbstractCapabilityExecutor 抽象骨架类，提供 execute() 的默认模板方法实现
10. **问题描述**：CapabilityExecutor 的线程安全性依赖于隐式 DTO 线程安全
    - 所在位置：§6.1 CapabilityExecutor 线程安全段 + §3.1 变量提取约定
    - 严重程度：一般
    - 改进建议：明确约定 request 对象在管线执行过程中视为只读，推荐 DTO 设计为不可变对象；若方式 B 的 extractVariables() 需要修改请求数据，实现者必须在方法内部进行防御性拷贝
   - 所在位置：§4.1 降级路径伪代码第979行 + §3.5 SlidingWindowMetricsStore 方法签名 + §2.3 类图第368-373行
   - 严重程度：严重
   - 改进建议：为 SlidingWindowMetricsStore 增加重载方法或新增降级标记专用方法，调整失败率计算公式
2. **问题描述**：departmentId 在标准管线中无可用来源
   - 所在位置：§3.5 AiRequestBase 字段定义第737-743行 + §4.1 管线伪代码第951行 + §4.4 PromptTemplateManager.render() 签名
   - 严重程度：严重
   - 改进建议：在 AiRequestBase 中增加 String departmentId 字段，或明确从 RequestContext/SecurityContext 提取
3. **问题描述**：异步边界不明确 — LlmClient.invoke() 同步阻塞与 CompletableFuture 返回值矛盾
   - 所在位置：§2.3 类图 LlmClient.invoke() 返回值类型第248行 + §3.1 CapabilityExecutor.execute() 方法签名 + §4.1 伪代码第966行
   - 严重程度：严重
   - 改进建议：明确异步策略：改 LlmClient.invoke() 为 CompletableFuture，或 execute() 内部包裹 supplyAsync()，或改为同步语义
4. **问题描述**：降级策略 Bean 注入机制从 YAML 配置到 @Qualifier 的转换路径未定义
   - 所在位置：§3.1 第569行 + §3.8 第914行 + §9.2 配置示例
   - 严重程度：严重
   - 改进建议：在 §3.1 或 §7 中补充完整的策略 Bean 装配机制伪代码说明
5. **问题描述**：薄适配器型 CapabilityExecutor 的执行行为未定义
   - 所在位置：§3.1 第505-508行 + §4.1 伪代码
   - 严重程度：重要
   - 改进建议：在 §4.1 中新增薄适配器 CapabilityExecutor 管线小节，提供独立简化伪代码
6. **问题描述**：AiOrchestrator 方法到能力标识的映射约定未定义
   - 所在位置：§3.1 AiOrchestrator 职责 + §4.1 第931-936行
   - 严重程度：重要
   - 改进建议：显式列出 AiService 全部13个方法到 capabilityId 的映射表，建议使用 CapabilityId 常量类或 enum
7. **问题描述**：FallbackAiService.applyStrategies() 残留代码的迁移路径未在主文档中定义
   - 所在位置：§3.8 第864行 + §5 错误处理
   - 严重程度：重要
   - 改进建议：在 §9 迁移路径表中明确 FallbackAiService 的三项迁移操作
8. **问题描述**：ModelEndpointHealthManager 与 CircuitBreakerDegradationStrategy 的交互优先级未定义
   - 所在位置：§3.2 第607行 + §3.8 第882-895行 + §4.1 伪代码第943-965行
   - 严重程度：重要
   - 改进建议：明确定义两个机制的交互优先级，建议降级预检（含熔断器）优先
9. **问题描述**：CapabilityExecutor 线程安全性未在 §6.1 中覆盖
   - 所在位置：§6.1 线程模型第1073-1084行
   - 严重程度：重要
   - 改进建议：补充 CapabilityExecutor 的线程安全契约，要求无状态线程安全
10. **问题描述**：userId 在 ExperimentManager.assign() 管线中的来源未定义
    - 所在位置：§4.1 伪代码第952行 + §3.4 ExperimentManager.assign() 方法签名第285行
    - 严重程度：中等
    - 改进建议：明确 userId 的来源定义，可选方案包括从 SecurityContext 提取或在 AiRequestBase 中增加字段
11. **问题描述**：ModelRouter 运行时刷新的线程安全性不完整
    - 所在位置：§6.1 第1081行 + §4.2 第999行
    - 严重程度：中等
    - 改进建议：使用 AtomicReference<Map<...>> 替代直接使用 ConcurrentHashMap
12. **问题描述**：AiOrchestrator.handle() 缺少异常捕获
    - 所在位置：§4.1 伪代码第931-936行
     - 严重程度：中等
     - 改进建议：在伪代码中增加 try-catch 块，异常时记录错误指标并返回降级响应

## 迭代第 5 轮

1. **问题描述**：Q1. `AiResult.error()` 工厂方法不存在，§4.1 伪代码第 1042 行调用 `AiResult.error("AI服务暂时不可用，请稍后重试")`，但现有 `AiResult.java` 仅定义了 `success(T)`, `failure(String)`, `degraded(String)` 三个静态工厂方法
   - 所在位置：§4.1 第 1042 行伪代码；`AiResult.java:23-32`
   - 严重程度：严重
   - 改进建议：在 §1.3 或 §3.5 中显式新增 `AiResult.error(String message)` 工厂方法定义，或在伪代码中统一替换为 `AiResult.failure("SERVICE_UNAVAILABLE")`，并在 §7 记录此变更
2. **问题描述**：Q2. §4.1 伪代码返回类型与方法签名不一致——`CapabilityExecutor.execute()` 声明返回 `CompletableFuture<AiResult<R>>`，但第 1081 行直接 `return AiResult.success(parsedResult)`、第 1094 行 `return AiResult.degraded(degradeReason)`，返回裸 `AiResult` 而非 `CompletableFuture`
   - 所在位置：§4.1 第 1081、1090、1094 行
   - 严重程度：严重
   - 改进建议：伪代码中所有 `return AiResult.success/degraded(...)` 改为 `return CompletableFuture.completedFuture(AiResult.success/degraded(...))`，或在伪代码开头注释明确"实际实现由 supplyAsync 包装"
3. **问题描述**：Q3. Experiment PAUSED 状态语义与实际实现机制矛盾——§3.4 定义 PAUSED 语义为"不再分流新流量；已分配的会话继续按实验分组执行"，但 §4.3 `ExperimentManager.assign()` 使用无状态哈希分桶，无法区分"新流量"和"已分配会话"
   - 所在位置：§3.4 Experiment 状态模型定义 vs §4.3 ExperimentManager.assign() 伪代码
   - 严重程度：严重
   - 改进建议：(a) 修正 PAUSED 语义为"暂停分流，所有流量回退到默认模型（分组=default）"，或 (b) 引入实验分配记录表持久化每次分流结果。Phase 5 建议采用方案 (a)
4. **问题描述**：Q4. CapabilityExecutor 执行管线实际为同步阻塞，与异步契约矛盾——§3.1/§3.2 声明使用 `CompletableFuture.supplyAsync()` 包装，但 §4.1 伪代码中管线步骤全部在 `execute()` 内顺序同步执行，未明确实际异步边界
   - 所在位置：§3.1 CapabilityExecutor 异步说明 + §3.2 LlmClient 同步契约 + §4.1 伪代码整体结构
   - 严重程度：严重
   - 改进建议：明确异步策略为"LLM 调用步骤（步骤 5~7）通过 supplyAsync 委派到线程池"或"整个管线通过 supplyAsync 提交"，在 §4.1 伪代码中体现 supplyAsync 调用边界
5. **问题描述**：Q5. FallbackAiService 构造器迁移路径缺失——§3.1 要求使用 `@Primary` + `ObjectProvider<AiService>` 延迟解析，但现有代码使用 `List<AiService>` 构造注入并过滤自身，§9.2 迁移路径未覆盖构造器改造
   - 所在位置：§3.1 Bean 装配策略 vs §9.2 迁移路径；`FallbackAiService.java:50-53`
   - 严重程度：严重
   - 改进建议：在 §9.2 迁移路径表格中补充第 0 步：将 `FallbackAiService` 构造器从 `List<AiService>` 改为 `ObjectProvider<AiService>`，同步修改内部过滤逻辑
6. **问题描述**：Q6. @Qualifier 命名约定不一致——一处按原始 capabilityId 字符串作为 qualifier value，一处按"能力名+Strategies"模式，Bean name 与 capabilityId 无直接映射规则
   - 所在位置：§3.1 第 597 行、第 608-611 行
   - 严重程度：重要
   - 改进建议：统一约定为 `{capabilityId}Strategies` 模式，在 §3.1 中明确命名规则并在配置示例中展示
7. **问题描述**：Q7. 薄适配器型 CapabilityExecutor 缺少委托调用异常处理——§3.1 薄适配器伪代码中 `phase4ServiceDelegate.execute(request)` 无 try-catch 保护，Phase 4 业务异常与基础设施异常混为一谈
   - 所在位置：§3.1 薄适配器伪代码（第 641 行）；§4.1 handle() 伪代码第 1035-1042 行
   - 严重程度：重要
   - 改进建议：薄适配器伪代码中包裹 try-catch，Phase 4 业务异常包装为 `AiResult.failure(bizErrorCode)` 返回，基础设施异常传播或记录为 `InternalError`
8. **问题描述**：Q8. AiOrchestrator 协作对象遗漏 AiMetricsCollector——§4.1 伪代码第 1040 行调用 `metricsCollector.record()`，但 §3.1 协作对象列表和 §2.3 类图均未包含 `AiMetricsCollector`
   - 所在位置：§3.1 AiOrchestrator 协作对象段落；§2.3 类图 AiOrchestrator 区域
   - 严重程度：重要
   - 改进建议：在 §3.1 AiOrchestrator 协作对象中补充 `AiMetricsCollector`，在 §2.3 类图中补充依赖关系
9. **问题描述**：Q9. `DegradationStrategy` 新增 `getOrder()` default method 的兼容性说明不足——设计文档多处声明此方法，但未记录对 `ai-api` 模块的接口变更影响及对现有实现（`NoOpDegradationStrategy`）的评估
   - 所在位置：§2.3 类图 DegradationStrategy 定义；§3.1 第 953 行；§3.8 第 963 行；`DegradationStrategy.java:3-6`
   - 严重程度：重要
   - 改进建议：在 §7 设计决策表中补充接口扩展行，记录动机、影响范围（3 个现有实现）和向后兼容性保证，在 §9 中补充接口变更编译验证步骤
10. **问题描述**：Q10. AiCallLogEntity 和 AiCallRecord 缺少 `departmentId` 字段——`AiRequestBase` 包含 `departmentId`，Prompt 模板按科室维度检索需要该字段，但日志记录和数据模型中均缺失
    - 所在位置：§3.5 AiCallRecord 字段表（第 790-811 行）、AiCallLogEntity 字段表（第 855-877 行）
    - 严重程度：重要
    - 改进建议：在 `AiCallRecord` 和 `AiCallLogEntity` 中补充 `departmentId`（`String`，可为空），在表索引策略中补充 `idx_department_call_time` 覆盖索引
11. **问题描述**：Q11. YAML 策略配置到 Bean 装配路径中存在初始化时序风险——`@Bean` 方法内部调用 `ApplicationContext.getBeansOfType(DegradationStrategy.class)` 按名称查找策略 Bean，但容器可能尚未完成所有策略 Bean 创建
    - 所在位置：§3.1 第 604-611 行"YAML 配置到 Bean 引用的装配路径"
    - 严重程度：重要
    - 改进建议：补充时序风险评估，建议改为 `@PostConstruct` 阶段执行或使用 `ListableBeanFactory` 延迟查找
12. **问题描述**：Q12. 文档标题版本号与实际迭代轮次不符——标题为 `（v3）` 但修订历史包含 v2 到 v6 的 7 轮修订，实际迭代为第 5 轮
    - 所在位置：第 1 行标题
    - 严重程度：重要
    - 改进建议：修正标题版本号为 `（v5）` 或 `（修订版 v6）`，与文件路径和最新修订说明保持一致


## 迭代第 8 轮

1. **问题描述**：异步线程上下文传播未定义 — SecurityContext 和 RequestContext 在 supplyAsync 线程池中丢失
   - 所在位置：§4.1 AbstractCapabilityExecutor.execute() 模板方法（第 1144-1157 行）+ §3.1 UserId/SessionId 上下文来源（第 609-612 行）+ §3.5 字段填充策略（第 903 行）+ §3.1 薄适配器 departmentId 提取（第 647 行）
   - 严重程度：严重
   - 改进建议：在 §6 并发设计中补充异步上下文传播机制，推荐方案 (c) 在 execute() 入口处提取完毕再传入 lambda，或方案 (b) 将异步边界下移到 LLM 调用层面
2. **问题描述**：LlmCallExecutor 线程池拒绝策略未定义 — 高并发下线程池满载后的行为不可控
   - 所在位置：§3.2 LlmClient 线程模型（第 735 行）
   - 严重程度：严重
   - 改进建议：在 §3.2 或 §6.1 中为 LlmCallExecutor 显式定义拒绝策略，建议 CallerRunsPolicy 或 DiscardPolicy + 降级，同步补充 queueCapacity 定义
3. **问题描述**：doDegrade() 方法签名缺少 departmentId 参数 — 降级路径下 AiCallRecord 构造缺少科室标识
   - 所在位置：§4.1 第 1153、1170、1176、1183 行调用点；§4.1 第 1189-1200 行方法体
   - 严重程度：重要
   - 改进建议：将 doDegrade() 方法签名扩展为 doDegrade(startTime, degradeReason, request, capabilityId, departmentId)，相应更新所有调用点
4. **问题描述**：AiCallRecord 工厂方法签名不完整 — failure() 和 degraded() 签名未覆盖 callerRole/callerId 字段
   - 所在位置：§3.5 工厂方法签名（第 884-898 行）
   - 严重程度：重要
   - 改进建议：在所有三个工厂方法签名中增加 callerRole 和 callerId 参数（推荐方案 a）
5. **问题描述**：薄适配器 departmentId 提取在 supplyAsync 异步上下文中无效
   - 所在位置：§3.1 第 647 行 doExtractDepartmentId() 伪代码；§4.1 第 1149-1150 行调用位置
   - 严重程度：重要
   - 改进建议：在 AiOrchestrator.handle() 的委托入口处提取 departmentId 并注入到 AiRequestBase.departmentId
6. **问题描述**：ModelEndpointHealthManager 状态机缺少 UNAVAILABLE→DEGRADED 和 CONNECTED→UNAVAILABLE 的直接转换路径
   - 所在位置：§3.2 第 750-756 行
   - 严重程度：重要
   - 改进建议：补充端点健康管理器的状态转换表，明确 CONNECTED → UNAVAILABLE（连续 N 次调用失败）和 UNAVAILABLE → CONNECTED（探测调用成功）路径
7. **问题描述**：熔断器与端点健康管理器独立探测可能产生冲突
   - 所在位置：§3.2 第 756 行 + 第 763-766 行
   - 严重程度：中等
   - 改进建议：统一探测机制，将端点健康探测决策权归并到一个组件
8. **问题描述**：AiCallRecord 未记录 Prompt 版本号 — A/B 实验效果分析缺少关键维度
   - 所在位置：§3.5 AiCallRecord 字段表（第 860-882 行）
   - 严重程度：中等
   - 改进建议：在 AiCallRecord 和 AiCallLogEntity 中补充 promptVersion 字段
9. **问题描述**：AiCallRecord.degraded() 工厂方法缺少 outputSummary 参数 — 本地规则降级结果无法记录
   - 所在位置：§3.5 第 895-898 行
   - 严重程度：一般
   - 改进建议：在 degraded() 工厂方法签名中补充 String outputSummary 参数
10. **问题描述**：未显式验证与 Phase0/Phase1ABD OOD 设计风格的一致性
    - 所在位置：全局
    - 严重程度：一般
    - 改进建议：在 §1 概述部分增加对 Phase0/Phase1ABD 设计风格和结构的引用说明
11. **问题描述**：ModelRoute 缺少端点级认证和超时配置定义
    - 所在位置：§3.2 ModelRoute 定义（第 787 行）
    - 严重程度：一般
    - 改进建议：扩展到 ModelRoute 值对象中增加 authentication、timeout 字段

## 迭代第 7 轮

1. **问题描述**：§4.1伪代码中`structuredOutputParser.parse()`缺少try-catch异常处理，与§5.1错误分类表承诺的矛盾
   - 所在位置：§4.1 CapabilityExecutor.execute() 第1172-1177行；§5.1 错误分类表第1271行
   - 严重程度：严重
   - 改进建议：在parse()调用外围添加try-catch，捕获ParseException后进入降级路径；或在§3.1模板方法注释中阐明此流程，并在§4.1伪代码中标注"由模板方法内部处理"
2. **问题描述**：userId提取链式调用未处理`getAuthentication()`返回null的情况，存在NPE风险
   - 所在位置：§3.1 第607行；§4.1 第1158行
   - 严重程度：一般
   - 改进建议：补充null安全处理，使用Optional或"SYSTEM"占位值；在§4.1伪代码中体现null检查与fallback
3. **问题描述**：§2.3类图中AbstractCapabilityExecutor缺少`doExecuteInternal()`和`doExtractDepartmentId()`声明
   - 所在位置：§2.3 第214-218行；§3.1 第696-724行
   - 严重程度：一般
   - 改进建议：补全类图中AbstractCapabilityExecutor的方法声明
4. **问题描述**：§3.1模板方法委托模式与§4.1内联伪代码的归属关系不明确
   - 所在位置：§3.1 第696-724行；§4.1 第1145-1191行
   - 严重程度：一般
   - 改进建议：将§4.1伪代码按模板方法模式重构为分层结构，明确标注骨架公共步骤与`doExecuteInternal()`的分界
5. **问题描述**：`extractVariables()`在AbstractCapabilityExecutor的方法契约中未定义
   - 所在位置：§3.1 第633-636行；§3.1 第696-724行；§4.1 第1157行
   - 严重程度：一般
   - 改进建议：在AbstractCapabilityExecutor中显式定义`doExtractVariables()`方法，在模板方法模型路由前调用
6. **问题描述**：§3.1薄适配器文本声称含模型路由检查但伪代码未实现
   - 所在位置：§3.1 第638-643行；薄适配器伪代码第646-683行
   - 严重程度：一般
   - 改进建议：统一文本与伪代码：确认薄适配器是否需要模型路由，相应修改文本或补充伪代码
7. **问题描述**：LocalRuleFallback接口返回raw AiResult，降级路径存在unchecked类型转换
   - 所在位置：§2.3 第430-435行；§4.1 第1183-1187行
   - 严重程度：一般
   - 改进建议：将LocalRuleFallback泛型化为`LocalRuleFallback<T, R>`

## 迭代第 9 轮

1. **问题描述**：薄适配器 CapabilityExecutor 的 Phase 4 服务依赖机制未定义，开发者无法确定模块级构建关系，可能破坏模块分层
   - 所在位置：§3.1 薄适配器型 CapabilityExecutor 的管线行为段落及伪代码；§2.2 模块依赖方向图
   - 严重程度：严重
   - 改进建议：在 §2.2 依赖方向图中明确 ai-impl 与 Phase 4 模块的依赖关系，定义 Phase 4 服务的 SPI 接口或明确声明直接依赖，补充注入方式说明
2. **问题描述**：§4.1 核心伪代码中存在 `inputSummary`、`outputSummary`、`retryCount`、`outputType` 四个未定义变量
   - 所在位置：§4.1 `doExecuteInternal()` 伪代码（第 1230-1256 行）
   - 严重程度：严重
   - 改进建议：在伪代码中显式定义四个变量的来源，示例如：`inputSummary = StringUtils.truncate(request.toString(), 500)`
3. **问题描述**：防御性拷贝合约在伪代码中未兑现，§3.1 声明防御性拷贝但 §4.1 `execute()` 伪代码中无拷贝步骤
   - 所在位置：§3.1 vs §4.1 `AbstractCapabilityExecutor.execute()` 伪代码（第 1203-1225 行）
   - 严重程度：一般
   - 改进建议：在 §4.1 `execute()` 伪代码中 `supplyAsync()` 之前增加防御性拷贝步骤
4. **问题描述**：TokenUsage 类未建模，LlmResponse 包含 `getTokenUsage()` 但 TokenUsage 未在核心抽象表、类图或值对象定义中出现
   - 所在位置：§3.2 LlmResponse 类定义，§4.1 伪代码调用点
   - 严重程度：一般
   - 改进建议：在 §1.3 核心抽象表、§2.3 类图中补充 TokenUsage 类（字段：promptTokens、completionTokens、totalTokens）
5. **问题描述**：ExperimentManager 未定义无实验时的返回值语义，"空 assignment" 是 null 引用还是非 null 默认对象未冻结，存在 NPE 风险
   - 所在位置：§3.4 ExperimentAssignment 定义；§4.3 分流契约
   - 严重程度：一般
   - 改进建议：冻结无实验命中的返回值语义：明确返回非 null 的 `ExperimentAssignment`（`group="default"`，其余字段为 null）或使用 `Optional<ExperimentAssignment>`
6. **问题描述**：Phase 4 DTO 过渡策略与实际执行伪代码存在矛盾，声明不继承 AiRequestBase 但伪代码中调用 `getVisitId()`/`getPatientId()` 等不存在的方法
   - 所在位置：§3.5 过渡策略段落 vs §3.1 薄适配器伪代码
   - 严重程度：一般
   - 改进建议：统一薄适配器对各公共字段的提取策略，所有 Phase 4 DTO 上缺失的字段通过独立提取路径获取
7. **问题描述**：熔断器与端点健康管理器的统一探测机制缺少状态转换图，开发者需自行推导各状态组合下的行为
   - 所在位置：§3.2 统一探测机制段落（第 805-810 行）
   - 严重程度：一般
   - 改进建议：补充时序图或决策表，覆盖熔断器状态（OPEN/HALF_OPEN）、端点健康状态（UNAVAILABLE/CONNECTED/DEGRADED）、探测窗口是否到期的组合决策结果

## 迭代第 10 轮

1. **问题描述**：§5.1 错误分类表的异常容错承诺在 §4.1 伪代码中未兑现，`experimentManager.assign()` 和 `promptTemplateManager.render()` 均未包裹 try-catch
   - 所在位置：§4.1 `doExecuteInternal()` 伪代码 vs §5.1 错误分类表
   - 严重程度：严重
   - 改进建议：在 `doExecuteInternal()` 中为这两个调用添加 try-catch，按 §5.1 承诺的降级行为处理；或在 §5.1 中如实修正承诺
2. **问题描述**：CapabilityExecutor 执行管线缺少整体兜底超时机制，仅 `LlmClient.invoke()` 有超时控制
   - 所在位置：§4.1 `doExecuteInternal()` 伪代码；§3.2 LlmCallExecutor 线程池配置
   - 严重程度：严重
   - 改进建议：引入可配置的端到端超时机制，通过 `CompletableFuture.orTimeout()` 附加超时，超时后进入降级路径
3. **问题描述**：AiOrchestrator.handle() catch 块调用 `AiCallRecord.failure()` 时丢失 `departmentId`、`visitId`、`patientId`、`sessionId` 等关键就诊上下文
   - 所在位置：§4.1 catch 块（第 1359-1362 行区域）
   - 严重程度：重要
   - 改进建议：从 `request` 对象中提取可用上下文字段传入 `failure()` 调用
4. **问题描述**：薄适配器型 CapabilityExecutor 对 Phase 4 服务的端到端耗时不可控，底座无法控制其内部超时设置和重试策略
   - 所在位置：§3.1 薄适配器伪代码 `doExecuteInternal()`；§9.5 YAML 配置
   - 严重程度：重要
   - 改进建议：为 `phase4ServiceDelegate.execute(request)` 引入独立超时控制，或明确约定依赖 Phase 4 模块自身的超时机制
5. **问题描述**：降级预检在 `supplyAsync()` lambda 内部执行，线程池排队后才做降级判定，导致熔断器 OPEN 状态下请求仍需等待入队
   - 所在位置：§4.1 `AbstractCapabilityExecutor.execute()` 模板方法伪代码（第 1384-1397 行）
   - 严重程度：中等
     - 改进建议：将降级预检移至 `supplyAsync()` 之前、容器线程执行，降级请求直接返回 `CompletableFuture.completedFuture(doDegrade(...))`，不入线程池排队

## 迭代第 11 轮

1. **问题描述**：`LlmResponse` 缺少 `retryCount` 字段定义，但管线伪代码中直接调用 `getRetryCount()`
   - 所在位置：§2.3 类图第 290-294 行、§3.2 文本定义第 964-966 行、§4.1 伪代码第 1473 行
   - 严重程度：严重
   - 改进建议：在 `LlmResponse` 中补充 `retryCount: int` 字段，说明由 `LlmClient.invoke()` 内部填充；或将 `retryCount` 移至 `AiCallRecord` 构建器参数
2. **问题描述**：`doExecuteInternal()` 中调用 `extractParsedSummary()` 和 `extractOutputSummary()` 但两方法均未定义
   - 所在位置：§4.1 伪代码第 1477 行（`extractParsedSummary`）、第 1497 行（`extractOutputSummary`）
   - 严重程度：严重
   - 改进建议：统一命名并在 `AbstractCapabilityExecutor` 中定义默认实现，或直接使用 `StringUtils.truncate` 内联
3. **问题描述**：薄适配器型 `CapabilityExecutor` 在同一线程池中嵌套 `supplyAsync()` 调用
   - 所在位置：§3.1 第 730-731 行、§4.1 第 1424-1426 行
   - 严重程度：一般
   - 改进建议：薄适配器使用公共 `ForkJoinPool` 而非 `llmCallExecutor`，或在文档中显式说明嵌套模式的设计理由及 `CallerRunsPolicy` 的前提条件
4. **问题描述**：`ModelRoute` 字段扩展表中 `authentication` 类型标注为"(设计占位)"
   - 所在位置：§3.2 第 991 行
   - 严重程度：一般
   - 改进建议：改为具体类型声明（如 `AuthType` 枚举），或删除字段并在 `LlmClient` 说明中描述认证凭据获取机制
5. **问题描述**：§3.1 与 §4.1 对 `AbstractCapabilityExecutor.execute()` 模板方法的描述不一致——降级预检位置相互矛盾
   - 所在位置：§3.1 第 765-793 行 vs §4.1 第 1395-1438 行
   - 严重程度：一般
   - 改进建议：同步 §3.1 伪代码使其与 §4.1 一致，两处指向同一份权威伪代码
6. **问题描述**：`inputSummary` 在 `execute()` 中定义为局部变量，但 `doDegrade()` 作为独立方法依赖闭包捕获，Java 语法上不可行
   - 所在位置：§4.1 第 1413 行、第 1421 行、第 1492 行注释
   - 严重程度：一般
   - 改进建议：将 `inputSummary` 作为参数加入 `doDegrade()` 和 `doExecuteInternal()` 的方法签名，或改为在方法体内重新定义
7. **问题描述**：YAML 配置示例中 7 项底座能力的超时配置只覆盖了 3 项
   - 所在位置：§9.5 第 1822-1832 行
   - 严重程度：一般
   - 改进建议：完整填充全部 13 项能力的超时配置，或在注释中为每种能力给出超时值选择依据
8. **问题描述**：未提供测试策略或可验证性指导
   - 所在位置：全文
   - 严重程度：一般
   - 改进建议：新增"测试策略"章节，覆盖单元测试模式、集成测试模式、管线收敛验证

## 迭代第 12 轮

1. **问题描述**：薄适配器Maven依赖作用域未做确定性决策，开发者无法判断应使用test还是compile作用域
   - 所在位置：§2.2 依赖规则段
   - 严重程度：严重
   - 改进建议：冻结选择——推荐provided或compile并给出理由；若选compile需在§10记录耦合治理规则；若倾向松耦合，在ai-impl内定义SPI接口消除对Phase 4的直接Maven依赖
2. **问题描述**：薄适配器超时路径下CompletableFuture.cancel(true)无法真正中止Phase 4服务执行，属于事实错误
   - 所在位置：§3.1 薄适配器伪代码第738行；§9.5 YAML默认超时30s
   - 严重程度：严重
   - 改进建议：删除cancel(true)调用，改用WARN日志注明"服务将继续执行至完成"；若需真正中止，需Phase 4接口支持可中断模式并在§7记录取舍
3. **问题描述**：doDegrade()方法签名缺少promptVersion参数，降级记录中永远丢失实验分组信息
   - 所在位置：§4.1 doDegrade()方法定义第1508-1527行；三处调用点
   - 严重程度：严重
   - 改进建议：doDegrade()签名增加Integer promptVersion参数，调用点传入assignment.getTargetPromptVersion()；同步更新§2.3类图
4. **问题描述**：AiOrchestrator.handle()中未注册能力的异常在try块外直接throw，破坏CompletableFuture异步契约
   - 所在位置：§4.1 AiOrchestrator.handle()第1386行
   - 严重程度：一般
   - 改进建议：将null检查移入try块，走CompletableFuture.completedFuture(AiResult.failure(...))路径，或使用completedExceptionally()包装
5. **问题描述**：ExperimentAssignment仅以字段表形式定义，未定义构造器/Builder/工厂方法
   - 所在位置：§3.4 ExperimentAssignment段落
   - 严重程度：一般
   - 改进建议：显式定义全参数构造器+无参默认工厂方法（所有字段null/default），与无实验命中返回值语义对齐
6. **问题描述**：ModelRoute密钥获取接口未定义，LlmClient实现者无法推断正确实现
    - 所在位置：§3.2 ModelRoute字段扩展表第1008-1011行
    - 严重程度：一般
    - 改进建议：新增CredentialProvider接口定义，明确密钥缓存策略（首次成功缓存5分钟），定义Vault不可达回退行为

## 迭代第 13 轮

1. **问题描述**：AiOrchestrator.handle() catch 块中薄适配器场景的就诊上下文提取失效，catch 块依赖 instanceof AiRequestBase 判断但薄适配器 DTO 不继承该类，导致部门/就诊/患者/会话ID全部传入 null
   - 所在位置：§4.1 第 1465-1474 行、第 1498-1501 行
   - 严重程度：一般
   - 改进建议：在 catch 块增加 Phase 4 DTO 兼容提取路径，通过 RequestContextHolder/HTTP Header 独立提取就诊上下文
2. **问题描述**：ParseFailure 降级路径丢失原始 LLM 响应摘要，doDegrade() 签名不含 outputSummary 参数，降级记录中对应字段为空
   - 所在位置：§4.1 第 1564-1569 行、第 1584-1601 行
   - 严重程度：一般
   - 改进建议：doDegrade() 签名增加可选 String outputSummary 参数，解析失败降级时传入 LLM 原始响应摘要
3. **问题描述**：Prompt 模板渲染契约未定义"指定版本已废弃"的回退行为，DEPRECATED 版本的处理方式未冻结
   - 所在位置：§4.4 第 1636-1637 行
   - 严重程度：一般
   - 改进建议：在渲染契约中增加规则：promptVersion 对应版本为 DEPRECATED 时输出 WARN 日志并回退到 ACTIVE 模板
4. **问题描述**：薄适配器 doExecuteInternal() 中 ExecutionException 包裹的原始异常类型丢失，降级原因只记录 "ExecutionException" 而非 NPE 等原始类型
   - 所在位置：§3.1 第 751-753 行区域
   - 严重程度：一般
   - 改进建议：在 catch(Exception) 中提取 getCause() 的类型名拼接到降级原因中
5. **问题描述**：Experiment PAUSED 状态下 assign() 的返回值未冻结，当前依赖"检索不到"的隐式行为，未来缓存预热等变更易破坏契约
   - 所在位置：§3.4 第 1104 行 vs §4.3 第 1622-1625 行
   - 严重程度：一般
   - 改进建议：在 §4.3 显式增加 PAUSED 状态分支，过滤掉 status=PAUSED 的实验
6. **问题描述**：PromptTemplate 状态模型缺少 DEPRECATED→ACTIVE 回退路径，紧急回滚场景下需新建 DRAFT 版本再发布
   - 所在位置：§3.3 第 1081-1086 行
   - 严重程度：一般
   - 改进建议：增加 DEPRECATED→ACTIVE 转换路径或在回滚策略文档中说明工作流
7. **问题描述**：不可变 DTO 与防御性拷贝共存时 ObjectMapper 兼容性未说明，按不可变推荐设计但未标注 @JsonCreator 的 DTO 会导致 convertValue() 抛出异常
   - 所在位置：§3.1 第 626 行 vs §4.1 第 1498 行
   - 严重程度：一般
   - 改进建议：在 §3.1 补充 Jackson 兼容反序列化要求，或对不可变 DTO 跳过拷贝步骤

## 迭代第 14 轮

1. **问题描述**：`AbstractCapabilityExecutor.execute()` 中 request 变量重赋值后捕获到 lambda，造成 Java 编译错误
   - 所在位置：§4.1 第 1510、1525、1535 行
   - 严重程度：严重
   - 改进建议：将防御性拷贝结果存入新局部变量而非重赋给 `request`，使用 `finalRequest` 保持原 `request` 不变
2. **问题描述**：薄适配器提取方法命名与基类模板方法签名不匹配，子类无法正确重写
   - 所在位置：§3.1 第 717-723 行 vs §4.1 第 1503-1505 行
   - 严重程度：严重
   - 改进建议：将 `extractVisitId()` / `extractPatientId()` 统一修正为 `doExtractVisitId(request)` / `doExtractPatientId(request)`
3. **问题描述**：Maven 依赖作用域在 §2.2 和 §3.1 中存在矛盾
   - 所在位置：§2.2 第 169 行（`provided`）vs §3.1 第 675 行（`compile`）
   - 严重程度：严重
   - 改进建议：统一为 `provided` 作用域，并在 §10.3 补充运行时风险评估
4. **问题描述**：`ModelRoute` 缺少 `parameters` 字段，管线伪代码引用未定义方法
   - 所在位置：§2.3 第 264-272 行 vs §4.1 第 1574 行
   - 严重程度：严重
   - 改进建议：补充 `parameters: Map<String, Object>` 字段及其 getter，同步更新 §3.2 和 §9.5
5. **问题描述**：降级预检循环的 degrade reason 取值方式与 DegradationReason 枚举体系不一致
   - 所在位置：§4.1 第 1520-1521 行 vs §3.8 第 1437-1447 行
   - 严重程度：重要
   - 改进建议：建立策略类名到 DegradationReason 枚举的映射或增加通用枚举常量
6. **问题描述**：`capabilityTimeoutConfig` 字段在类图和构造器中均未定义
   - 所在位置：§2.3 第 218-229 行 vs §4.1 第 1530 行
   - 严重程度：重要
   - 改进建议：在类图中补充该字段并说明注入方式
7. **问题描述**：`DegradationContext` 类图与 §3.8 文本描述严重不一致
   - 所在位置：§2.3 第 427-435 行 vs §3.8 第 1414-1422 行
   - 严重程度：重要
   - 改进建议：在类图中补充 serializedTimestamp、postDeserializationValidate()、isFresh()、isInitialized()
8. **问题描述**：`PromptTemplateManager` 和 `ExperimentManager` 的缓存失效范围未定义
   - 所在位置：§6.1 第 1729-1730 行，§3.3 第 1074 行，§3.4 第 1100-1104 行
   - 严重程度：重要
   - 改进建议：定义失效事件 Payload 结构、重建策略（惰性加载+分布式锁）、发布消费时序
9. **问题描述**：客户端侧缺少主动限流/速率保护机制
   - 所在位置：§3.2、§3.8、§4.1 均未覆盖
   - 严重程度：重要
   - 改进建议：在 LlmClient 上层或内部增加令牌桶/滑动窗口限流器
10. **问题描述**：薄适配器 `doExecuteInternal()` 在 `llmCallExecutor` 线程中阻塞等待 ForkJoinPool 任务
    - 所在位置：§3.1 第 736-738 行
    - 严重程度：中等
    - 改进建议：评估阻塞等待对线程池可用性影响，或使用独立线程池
11. **问题描述**：`AiCallLogEntity` 缺少数据保留与清理策略
     - 所在位置：§3.5 第 1251-1293 行
     - 严重程度：中等
     - 改进建议：补充数据生命周期管理策略（按月分区、保留期限、归档方案）

## 迭代第 15 轮

1. **问题描述**：AiPlatformConfig 核心配置类缺失正式定义
   - 所在位置：§2.1 目录结构（line 145）、§2.3 类图、§3.1 多处引用
   - 严重程度：严重
   - 改进建议：在 §2.3 类图中补充 AiPlatformConfig 类型，标注 implements ApplicationContextAware 及注解，列出核心 @Bean 方法
2. **问题描述**：LlmCallExecutor 与指标采集线程池的 Spring Bean 定义缺失
   - 所在位置：§3.2（line 919）、§3.5（lines 1215-1219）、§6.1（line 1799）
   - 严重程度：重要
   - 改进建议：在 §3.1 或新增节中补充两个线程池的 @Bean 定义伪代码及对应 YAML 配置块
3. **问题描述**：AiOrchestrator.handle() 与 AiService 13 方法的映射关系未显式定义
   - 所在位置：§4.1（line 1524）vs §2.3 类图（lines 200-208）vs §3.1 映射表（lines 557-572）
   - 严重程度：重要
   - 改进建议：在 §4.1 handle() 伪代码前新增注释块说明委托关系，或补充完整委托示例
4. **问题描述**：薄适配器 CapabilityExecutor doExecuteInternal() 的行为契约仅存在于 §3.1 而非 §4.1
   - 所在位置：§4.1 (lines 1608-1684) vs §3.1 (lines 742-794)
   - 严重程度：重要
   - 改进建议：在 §4.1 补充薄适配器子类特化版伪代码，覆盖超时控制、线程池、异常处理、retryCount=0 差异点
5. **问题描述**：AiOrchestrator 持有 ModelEndpointHealthManager 但 handle() 伪代码未使用
   - 所在位置：§2.3 类图（line 204）vs §4.1 handle()（lines 1524-1560）
   - 严重程度：重要
   - 改进建议：从 AiOrchestrator 类图和协作对象列表中移除 ModelEndpointHealthManager，或在 handle() 伪代码中补充使用场景
6. **问题描述**：AiOrchestrator.handle() catch 块中 extractHeader() 工具方法未定义
   - 所在位置：§4.1（lines 1549-1552）vs §3.1（line 728）
   - 严重程度：一般
    - 改进建议：统一命名为 extractFromRequestContext(String headerName)，在工具方法段中给出默认实现说明

## 迭代第 17 轮

1. **问题描述**：`doExecuteInternal()` 中 `chatResponse` 和 `retryCount` 变量在 `structuredChat` 成功路径中未定义，try-catch 之后的公共指标采集代码引用这两个变量导致变量作用域错误
   - 所在位置：§4.1 doExecuteInternal() 完整管线伪代码 LLM 调用及后续指标采集部分（约 line 1862-1878）
   - 严重程度：严重
   - 改进建议：将 `chatResponse` 和 `retryCount` 的定义移至 try 块之前（初始化为 null/0），在成功路径和回退路径中分别赋值；或设计 `StructuredChatResult<T>` 包裹返回值含 retryCount
2. **问题描述**：`structuredChat(LlmChatRequest, Class<T>)` 返回裸 `T`，不包含 `retryCount` 或 `tokenUsage` 等元数据，成功场景下指标记录的 Token 用量和重试次数恒为 null/0
   - 所在位置：§4.1 doExecuteInternal() 伪代码 LLM 调用段（约 line 1859-1883）
   - 严重程度：一般（重要）
   - 改进建议：将 `structuredChat()` 返回值修改为包装类型 `StructuredChatResult<T>`（含 T data、int retryCount、LlmChatUsage usage），同步更新 §3.2 接口方法签名和 §2.3 类图
3. **问题描述**：§1.3 核心抽象一览表遗漏 `CredentialProvider` 和 `EndpointRateLimiter`
   - 所在位置：§1.3 核心抽象表（约 line 44-72）
   - 严重程度：一般
   - 改进建议：在 §1.3 中补充 `CredentialProvider`、`EndpointRateLimiter` 两行
4. **问题描述**：`LlmChatOptions` 与 `ModelRoute.parameters` 的字段映射覆盖逻辑未定义
   - 所在位置：§3.2 ModelRoute 字段扩展表（约 line 1163-1170）和 LlmChatOptions 定义段（约 line 1126-1131）
   - 严重程度：一般
   - 改进建议：明确两阶段填充策略或合并规则，在 §4.1 伪代码中体现
5. **问题描述**：`PromptTemplateManager.getFallbackPrompt()` 的返回值约束未定义
   - 所在位置：§3.3 PromptTemplateManager 职责描述（约 line 1230）
   - 严重程度：一般
   - 改进建议：补充返回值约定为非 null 非空字符串，格式与 render() 正常输出一致

## 迭代第 18 轮

1. **问题描述**：薄适配器 doExecuteInternal() 中使用未定义变量 `promptVersion`，编译无法通过
   - 所在位置：§4.2 薄适配器 CapabilityExecutor 特化管线伪代码，TimeoutException 分支和 catch(Exception) 分支
   - 严重程度：严重
   - 改进建议：将两处 `promptVersion` 替换为 `null` 字面量
2. **问题描述**：薄适配器降级原因使用字符串字面量 `"TIMEOUT:ThinAdapterTimeout"` 而非 DegradationReason 枚举常量
   - 所在位置：§4.2 ThinAdapterExecutor，TimeoutException 分支
   - 严重程度：重要
   - 改进建议：替换为 `DegradationReason.TIMEOUT + ":ThinAdapterTimeout"`
3. **问题描述**：AiPlatformConfig 同时实现 EnvironmentPostProcessor 和 ApplicationContextAware 导致生命周期冲突（双实例）
   - 所在位置：§3.9 AiPlatformConfig 定义；§3.1 Bean 装配策略
   - 严重程度：重要
   - 改进建议：方案A（推荐）：将 EnvironmentPostProcessor 剥离为独立类；方案B：显式说明双实例生命周期机制
4. **问题描述**：orTimeout().exceptionally() 降级路径使用原始 request 而非 defensiveCopy
   - 所在位置：§4.1 AbstractCapabilityExecutor.execute() 模板方法，.exceptionally() 回调
   - 严重程度：重要
   - 改进建议：将 exceptionally() 回调中的 request 替换为 defensiveCopy
5. **问题描述**：getFallbackPrompt() 的 YAML 配置项在 §9.5 缺失，实现者无法找到完整配置结构和默认值
   - 所在位置：§3.3 vs §9.5
   - 严重程度：一般
   - 改进建议：在 §9.5 YAML 中补充 ai.template.fallback 配置块，至少包含一个能力的兜底 Prompt 配置示例
6. **问题描述**：AiOrchestrator.handle() catch 块中存在未使用的变量 requestAttributes，与 extractFromRequestContext() 的独立获取行为存在语义歧义
   - 所在位置：§4.1 AiOrchestrator.handle() 伪代码
   - 严重程度：一般
   - 改进建议：删除未使用的 requestAttributes 变量定义
7. **问题描述**：§3.1 文本声称薄适配器"包含"实验分流，但 §4.2 伪代码中完全没有 experimentManager.assign() 调用，文本与伪代码实质性矛盾
   - 所在位置：§3.1 薄适配器型 CapabilityExecutor 的管线行为段落 vs §4.2 伪代码
   - 严重程度：重要
   - 改进建议：删除或修正 §3.1 中关于薄适配器包含实验分流的描述，明确薄适配器管线不包含实验分流步骤

## 迭代第 19 轮

1. **问题描述**：`HttpApiLlmChatService` / `SpringAiLlmChatService` 同时实现两个接口，与 §3.2 独立 `LlmChatStreamService` 的 reactor-core 隔离设计意图矛盾
   - 所在位置：§2.3 类图第 583-586 行；§3.2
   - 严重程度：严重
   - 改进建议：分离两个接口的实现，或修改 §3.2 设计说明承认 reactor-core 为 ai-impl 的非可选编译期依赖
2. **问题描述**：`thinAdapterTimeout` 与 `capabilityTimeoutConfig` 的注入/初始化机制未定义
   - 所在位置：§2.3 类图第 239-240 行；§3.1 第 773-784 行
   - 严重程度：严重
   - 改进建议：在 `AbstractCapabilityExecutor` 构造函数中补充 `@Value` 注入或显式 `@Bean` + `@Inject` 方式
3. **问题描述**：薄适配器型 CapabilityExecutor 缺少条件化 Bean 注册保护，Phase 4 服务不可用时容器启动失败
   - 所在位置：§3.1 第 769 行；§2.2 第 188 行
   - 严重程度：严重
   - 改进建议：添加 `@ConditionalOnProperty` 或 `@ConditionalOnBean` / `@ConditionalOnClass` 守卫
4. **问题描述**：薄适配器构造函数包含不必要的基础设施依赖
   - 所在位置：§3.1 第 773-784 行
   - 严重程度：一般
   - 改进建议：引入独立构造器签名，仅注入实际使用的依赖；或标记为 `required = false`
5. **问题描述**：`@Async` 指标采集线程池拒绝策略配置与 `@Bean` 定义不一致（DiscardPolicy 静默丢弃 vs 设计要求"+ 日志 WARN"）
   - 所在位置：§3.5 第 1377 行；§3.9 第 1706 行
   - 严重程度：一般
   - 改进建议：自定义继承 `DiscardPolicy` 并重写 `rejectedExecution()` 输出 WARN 日志，或在 `@Bean` 注释中说明
6. **问题描述**：`AiOrchestrator.handle()` catch 块中 `instanceof` 判断未处理 request 被篡改或代理的场景
   - 所在位置：§4.1 第 1807 行
   - 严重程度：一般
    - 改进建议：增加双重提取策略，优先 `instanceof` 提取，降级到 `extractFromRequestContext()` 兜底

## 迭代第 20 轮

1. **问题描述**：`LlmChatService` 多实现实例的选择机制未定义，导致 Bean 装配二义性（NoUniqueBeanDefinitionException）及 `ModelRoute.clientType` 路由与实际实现脱节
   - 所在位置：§2.3 类图、§3.2 LlmChatService 接口与实现节、§4.1 管线伪代码
   - 严重程度：严重
   - 改进建议：补充 `DelegatingLlmChatService` 分发层，内部持有两个实现并按 `ModelRoute.clientType` 委托；或冻结设计承诺删除 `ModelRoute.clientType`
2. **问题描述**：`structuredChat()` 回退路径的异常捕获粒度过粗，`catch (Exception e)` 未区分格式异常与基础设施异常，与 §5.1 错误分类表矛盾
   - 所在位置：§4.1 `doExecuteInternal()` 管线伪代码第 1972-1978 行
   - 严重程度：一般
   - 改进建议：拆分为两个 catch 分支——格式类异常回退 chat()，基础设施异常直接进入降级路径
3. **问题描述**：`RouteConfigChangedEvent` 事件定义缺失，仅有名称无字段定义
   - 所在位置：§6.1 第 2181 行
   - 严重程度：一般
   - 改进建议：在 §3.2 或 §6 末尾补充 Payload 定义及刷新策略
4. **问题描述**：`CircuitBreakerDegradationStrategy` 状态可观测性缺失，无状态查询接口
   - 所在位置：§3.8、§3.5
   - 严重程度：一般
   - 改进建议：补充 `getState(capabilityId)` 接口，通过 Micrometer Gauge 暴露
5. **问题描述**：X-Department-ID HTTP Header 提取的依赖假设未显式约定，非 HTTP 场景下提取失败
   - 所在位置：§3.10 `extractFromRequestContext()`、§4.1 `AiOrchestrator.handle()`、§3.1 薄适配器
   - 严重程度：一般
   - 改进建议：新增"调用方 Header 契约"小节，补充非 HTTP 场景替代提取路径
6. **问题描述**：`extractFromRequestContext()` 为 `AbstractCapabilityExecutor` 的 `protected` 方法，但在非继承类 `AiOrchestrator.handle()` 中直接调用，编译非法
   - 所在位置：§3.10 第 1804-1808 行、§4.1 第 1862-1868 行
   - 严重程度：严重
   - 改进建议：将 `extractFromRequestContext()` 提取为独立工具类的 `public static` 方法

## 迭代第 21 轮

1. **问题描述**：薄适配器默认超时值在§4.2与§3.1/§9.5之间矛盾
   - 所在位置：§4.2第2176行vs §3.1第1018行vs §9.5第2547行
   - 严重程度：严重
   - 改进建议：将§4.2注释中的"默认60s"修正为"默认30s"，与§3.1和§9.5保持一致
2. **问题描述**：`metricsAsyncExecutor` @Bean伪代码硬编码值与YAML配置值不一致
   - 所在位置：§3.9第1843-1844行vs §9.5第2561-2562行
   - 严重程度：严重
   - 改进建议：统一默认值约定，将@Bean伪代码改为展示从`@ConfigurationProperties`绑定的动态值注入方式
3. **问题描述**：薄适配器`doExtractDepartmentId()`伪代码与§3.10非HTTP回退策略文本描述不一致
   - 所在位置：§3.1第831-834行伪代码vs §3.10第1926-1931行文本描述
   - 严重程度：一般
   - 改进建议：在薄适配器伪代码中补充DTO字段回退逻辑
4. **问题描述**：降级策略Bean name与YAML引用名之间的映射未显式约定
   - 所在位置：§3.1第759-769行vs Spring默认Bean name推导规则
   - 严重程度：一般
   - 改进建议：补充显式Bean name声明方式说明
5. **问题描述**：`DegradationContext.setDepartmentId()`在管线伪代码中被调用但未在核心定义中声明
   - 所在位置：§4.1第2022行vs §3.8扩展字段表vs §2.3类图
   - 严重程度：一般
   - 改进建议：在§3.8补充departmentId字段及setter/getter；同步更新§2.3类图
6. **问题描述**：structuredChat异常类型在核心抽象和类图中缺失
   - 所在位置：§3.2第1051行vs §1.3第44-83行、§2.3类图、§2.1目录结构
   - 严重程度：严重
   - 改进建议：在§1.3补充异常类型；§2.3类图补充异常节点；§2.1指定包位置；§3.2补充@throws文档
7. **问题描述**：CredentialProvider Vault降级行为缺失正式状态模型
   - 所在位置：§3.2第1366-1368行
   - 严重程度：一般
   - 改进建议：将Vault降级行为形式化为状态模型（NORMAL/CACHE_ONLY/BACKOFF），含转换条件表
8. **问题描述**：`extractCallerRole()`与`extractCallerId()`的实现路径未定义
   - 所在位置：§3.1第995-1003行vs §4.1第921-922/2010-2011行
   - 严重程度：一般
    - 改进建议：补充具体提取路径；或在AiRequestBase中增加显式传入字段；同步更新§2.3类图

## 迭代第 22 轮

1. **问题描述**：DelegatingLlmChatService在SpringAiLlmChatService Bean不可用时的装配缺陷，强制注入导致NoSuchBeanDefinitionException
   - 所在位置：§3.2 DelegatingLlmChatService Bean装配伪代码（第1156-1175行）
   - 严重程度：严重
   - 改进建议：将注入改为ObjectProvider或@Autowired(required=false)，运行时判断可用性
2. **问题描述**：ai-impl/pom.xml缺失大量Phase 5必需的编译期/运行期依赖声明（JPA、Actuator/Micrometer、Reactor、Jackson、Caffeine、Guava、HTTP客户端、Web）
   - 所在位置：§2.1目录结构、§3.2/§3.3/§3.5/§3.9等章节
   - 严重程度：严重
   - 改进建议：在§8或新增专节列出全部显式Maven依赖清单，标注作用域和可选性
3. **问题描述**：DegradationContext扩展字段与现有applyStrategies()零值上下文的兼容性过渡路径未冻结
   - 所在位置：§3.8 DegradationContext扩展段；DegradationContext.java；FallbackAiService.java:187
   - 严重程度：重要
   - 改进建议：明确无参构造器必须永久保留，新增字段通过Builder/setter赋值
4. **问题描述**：底座模块JPA配置与多Repository扫描策略未定义
   - 所在位置：§2.1目录结构、§3.9 AiPlatformConfig定义
   - 严重程度：重要
   - 改进建议：补充JPA配置定义，明确@EntityScan和@EnableJpaRepositories的basePackages声明位置
5. **问题描述**：AiPlatformConfig以单一@ConfigurationProperties绑定全部配置组，违反职责分离
   - 所在位置：§3.9 AiPlatformConfig定义（第1894-1977行）
   - 严重程度：重要
   - 改进建议：拆分为多个独立配置属性类，通过@EnableConfigurationProperties逐个引入
6. **问题描述**：SlidingWindowMetricsStore的WindowedEvent类型和过期清理机制未定义
   - 所在位置：§3.5 SlidingWindowMetricsStore线程安全段（第1747-1748行）
   - 严重程度：重要
   - 改进建议：补充WindowedEvent字段定义、窗口边界判定规则、快照策略触发条件、与YAML配置映射关系
7. **问题描述**：Event驱动刷新机制假设单体部署，未记录设计约束也未提供分布式场景适配方案
   - 所在位置：§3.2/§3.3/§3.4事件刷新段、§10协作边界
   - 严重程度：重要
   - 改进建议：在§7设计决策表记录此约束，§10新增分布式部署兜底段
8. **问题描述**：CredentialProvider CACHE_ONLY状态下的TTL延长机制与Caffeine expireAfterWrite的兼容性未说明
   - 所在位置：§3.2 Vault降级状态模型（第1417-1446行）
   - 严重程度：一般
   - 改进建议：补充Caffeine实现方案说明，推荐Expiry接口或重新put刷新TTL
9. **问题描述**：混合完整管线与薄适配器的超时层级存在歧义，两个超时值关系未定义
   - 所在位置：§3.1 AbstractCapabilityExecutor.execute() vs §4.2 薄适配器超时控制；§9.5 YAML配置
   - 严重程度：重要
   - 改进建议：明确薄适配器场景capabilityTimeout应设置为thinAdapterTimeout+缓冲值，YAML添加注释约束
10. **问题描述**：idx_prompt_version独立索引的查询覆盖度不足
    - 所在位置：§3.5 AiCallLogEntity表索引策略（第1706行）
    - 严重程度：一般
    - 改进建议：替换或补充为复合索引(prompt_version, call_time DESC)，新增(capability_id, prompt_version, call_time DESC)

## 迭代第 23 轮

1. **问题描述**：reactor-core依赖性质存在事实矛盾，LlmChatStreamService接口直接引用Flux类型但文档宣称其仅作为可选依赖
   - 所在位置：§3.2 第1133-1136行 vs §8.2 第2691-2699行
   - 严重程度：严重
   - 改进建议：将LlmChatStreamService移入独立子模块，或如实承认reactor-core为编译期强制依赖

2. **问题描述**：薄适配器超时配置示例违反3.1层级约束，per-capability与thin-adapter-default均为30s无缓冲
   - 所在位置：§3.1 第1071-1073行 vs §9.5 第2867-2872行
   - 严重程度：严重
   - 改进建议：将per-capability超时值修正为35s并标注层级约束

3. **问题描述**：LocalRuleFallback泛型方法与管线调用类型不一致，存在ClassCastException风险
   - 所在位置：§4.1 第2347行；§3.7 第573-576行
   - 严重程度：重要
   - 改进建议：记录unchecked转换风险，推荐使用Class<T>显式持有类型信息并在注入时校验

4. **问题描述**：SlidingWindowMetricsStore惰性淘汰写/读并发竞争条件未定义，锁范围不足
   - 所在位置：§3.5 第1800行和第1811行
   - 严重程度：重要
   - 改进建议：统一锁协议或使用ReentrantReadWriteLock分离读写路径

5. **问题描述**：DEGRADED状态在管线层无告警日志和指标记录，LLM调用后未更新健康状态
   - 所在位置：§4.1 第2259-2262行；§3.2 第1208-1209行
   - 严重程度：重要
   - 改进建议：补充DEGRADED状态WARN日志和指标记录，调用后补充recordCallResult()

6. **问题描述**：extractFromRequestContext()直接强转ServletRequestAttributes存在ClassCastException风险
   - 所在位置：§3.10 第2090-2094行
   - 严重程度：重要
   - 改进建议：使用instanceof安全检查替代直接强转
