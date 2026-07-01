# 再审议判定报告（v5）

## 判定结果

RETRY

## 判定理由

组件B诊断报告共识别出 16 个质量问题，其中严重等级 5 个（Q1-Q5）、重要等级 7 个（Q6-Q12）、中等/一般等级 4 个（Q13-Q16）。质询报告结论为 LOCATED，确认审查结论可信，证据充分，各问题改进建议具备可操作性。组件B内部循环实际轮次（1）远低于最大轮次（12），说明提前终止是因审查被确认而非轮次耗尽。根据判定标准——审查报告包含严重或一般等级的问题——应判定为 RETRY，需重新运行组件A以修复所有已确认问题。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：Q1. `AiResult.error()` 工厂方法不存在，§4.1 伪代码第 1042 行调用 `AiResult.error("AI服务暂时不可用，请稍后重试")`，但现有 `AiResult.java` 仅定义了 `success(T)`, `failure(String)`, `degraded(String)` 三个静态工厂方法
- **所在位置**：§4.1 第 1042 行伪代码；`AiResult.java:23-32`
- **严重程度**：严重
- **改进建议**：在 §1.3 或 §3.5 中显式新增 `AiResult.error(String message)` 工厂方法定义，或在伪代码中统一替换为 `AiResult.failure("SERVICE_UNAVAILABLE")`，并在 §7 记录此变更

- **问题描述**：Q2. §4.1 伪代码返回类型与方法签名不一致——`CapabilityExecutor.execute()` 声明返回 `CompletableFuture<AiResult<R>>`，但第 1081 行直接 `return AiResult.success(parsedResult)`、第 1094 行 `return AiResult.degraded(degradeReason)`，返回裸 `AiResult` 而非 `CompletableFuture`
- **所在位置**：§4.1 第 1081、1090、1094 行
- **严重程度**：严重
- **改进建议**：伪代码中所有 `return AiResult.success/degraded(...)` 改为 `return CompletableFuture.completedFuture(AiResult.success/degraded(...))`，或在伪代码开头注释明确"实际实现由 supplyAsync 包装"

- **问题描述**：Q3. Experiment PAUSED 状态语义与实际实现机制矛盾——§3.4 定义 PAUSED 语义为"不再分流新流量；已分配的会话继续按实验分组执行"，但 §4.3 `ExperimentManager.assign()` 使用无状态哈希分桶，无法区分"新流量"和"已分配会话"
- **所在位置**：§3.4 Experiment 状态模型定义 vs §4.3 ExperimentManager.assign() 伪代码
- **严重程度**：严重
- **改进建议**：(a) 修正 PAUSED 语义为"暂停分流，所有流量回退到默认模型（分组=default）"，或 (b) 引入实验分配记录表持久化每次分流结果。Phase 5 建议采用方案 (a)

- **问题描述**：Q4. CapabilityExecutor 执行管线实际为同步阻塞，与异步契约矛盾——§3.1/§3.2 声明使用 `CompletableFuture.supplyAsync()` 包装，但 §4.1 伪代码中管线步骤全部在 `execute()` 内顺序同步执行，未明确实际异步边界
- **所在位置**：§3.1 CapabilityExecutor 异步说明 + §3.2 LlmClient 同步契约 + §4.1 伪代码整体结构
- **严重程度**：严重
- **改进建议**：明确异步策略为"LLM 调用步骤（步骤 5~7）通过 supplyAsync 委派到线程池"或"整个管线通过 supplyAsync 提交"，在 §4.1 伪代码中体现 supplyAsync 调用边界

- **问题描述**：Q5. FallbackAiService 构造器迁移路径缺失——§3.1 要求使用 `@Primary` + `ObjectProvider<AiService>` 延迟解析，但现有代码使用 `List<AiService>` 构造注入并过滤自身，§9.2 迁移路径未覆盖构造器改造
- **所在位置**：§3.1 Bean 装配策略 vs §9.2 迁移路径；`FallbackAiService.java:50-53`
- **严重程度**：严重
- **改进建议**：在 §9.2 迁移路径表格中补充第 0 步：将 `FallbackAiService` 构造器从 `List<AiService>` 改为 `ObjectProvider<AiService>`，同步修改内部过滤逻辑

- **问题描述**：Q6. @Qualifier 命名约定不一致——一处按原始 capabilityId 字符串作为 qualifier value，一处按"能力名+Strategies"模式，Bean name 与 capabilityId 无直接映射规则
- **所在位置**：§3.1 第 597 行、第 608-611 行
- **严重程度**：重要
- **改进建议**：统一约定为 `{capabilityId}Strategies` 模式，在 §3.1 中明确命名规则并在配置示例中展示

- **问题描述**：Q7. 薄适配器型 CapabilityExecutor 缺少委托调用异常处理——§3.1 薄适配器伪代码中 `phase4ServiceDelegate.execute(request)` 无 try-catch 保护，Phase 4 业务异常与基础设施异常混为一谈
- **所在位置**：§3.1 薄适配器伪代码（第 641 行）；§4.1 handle() 伪代码第 1035-1042 行
- **严重程度**：重要
- **改进建议**：薄适配器伪代码中包裹 try-catch，Phase 4 业务异常包装为 `AiResult.failure(bizErrorCode)` 返回，基础设施异常传播或记录为 `InternalError`

- **问题描述**：Q8. AiOrchestrator 协作对象遗漏 AiMetricsCollector——§4.1 伪代码第 1040 行调用 `metricsCollector.record()`，但 §3.1 协作对象列表和 §2.3 类图均未包含 `AiMetricsCollector`
- **所在位置**：§3.1 AiOrchestrator 协作对象段落；§2.3 类图 AiOrchestrator 区域
- **严重程度**：重要
- **改进建议**：在 §3.1 AiOrchestrator 协作对象中补充 `AiMetricsCollector`，在 §2.3 类图中补充依赖关系

- **问题描述**：Q9. `DegradationStrategy` 新增 `getOrder()` default method 的兼容性说明不足——设计文档多处声明此方法，但未记录对 `ai-api` 模块的接口变更影响及对现有实现（`NoOpDegradationStrategy`）的评估
- **所在位置**：§2.3 类图 DegradationStrategy 定义；§3.1 第 953 行；§3.8 第 963 行；`DegradationStrategy.java:3-6`
- **严重程度**：重要
- **改进建议**：在 §7 设计决策表中补充接口扩展行，记录动机、影响范围（3 个现有实现）和向后兼容性保证，在 §9 中补充接口变更编译验证步骤

- **问题描述**：Q10. AiCallLogEntity 和 AiCallRecord 缺少 `departmentId` 字段——`AiRequestBase` 包含 `departmentId`，Prompt 模板按科室维度检索需要该字段，但日志记录和数据模型中均缺失
- **所在位置**：§3.5 AiCallRecord 字段表（第 790-811 行）、AiCallLogEntity 字段表（第 855-877 行）
- **严重程度**：重要
- **改进建议**：在 `AiCallRecord` 和 `AiCallLogEntity` 中补充 `departmentId`（`String`，可为空），在表索引策略中补充 `idx_department_call_time` 覆盖索引

- **问题描述**：Q11. YAML 策略配置到 Bean 装配路径中存在初始化时序风险——`@Bean` 方法内部调用 `ApplicationContext.getBeansOfType(DegradationStrategy.class)` 按名称查找策略 Bean，但容器可能尚未完成所有策略 Bean 创建
- **所在位置**：§3.1 第 604-611 行"YAML 配置到 Bean 引用的装配路径"
- **严重程度**：重要
- **改进建议**：补充时序风险评估，建议改为 `@PostConstruct` 阶段执行或使用 `ListableBeanFactory` 延迟查找

- **问题描述**：Q12. 文档标题版本号与实际迭代轮次不符——标题为 `（v3）` 但修订历史包含 v2 到 v6 的 7 轮修订，实际迭代为第 5 轮
- **所在位置**：第 1 行标题
- **严重程度**：重要
- **改进建议**：修正标题版本号为 `（v5）` 或 `（修订版 v6）`，与文件路径和最新修订说明保持一致
