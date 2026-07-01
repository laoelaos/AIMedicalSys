# 再审议判定报告（v28）

## 判定结果

RETRY

## 判定理由

组件B诊断报告识别出5个问题：问题1为「逻辑矛盾」（重要）——`doDegrade()`参数签名在类图（15参数旧签名）、迁移计划（描述三期过渡方案）、简化签名示例（CallContext重载）、伪代码（仍使用旧签名）之间四态并存，给实施者带来"应按哪个版本编码"的决策负担；问题2~3为「深度不足」（中等）——缺少启动期Bean初始化顺序整体依赖约束图、6项薄适配器特化设计分散在三处缺乏集中视图；问题4~5为「深度不足」（一般）——`structuredChat()`双降级路径根因追溯缺口、多实例配置生效时间窗口差异未做约束分析。质询报告结论为LOCATED，确认诊断有效。问题1（逻辑矛盾/重要）对应判定标准中「严重」等级（逻辑矛盾），问题2~5（中等/一般）对应「一般」等级，满足RETRY条件。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：`doDegrade()`参数签名在类图、迁移计划与伪代码之间呈现四态并存
- **所在位置**：§2.3 第477行、§3.1 第1560~1564行（迁移计划）、§3.5 第2238~2252行（简化签名）、§4.1全文（旧签名调用点）
- **严重程度**：严重
- **改进建议**：在§2.3类图中新增CallContext重载签名行并标注「// 二期迁移目标」，或在类图注释中明确指向§3.1迁移计划，说明当前编码应使用旧签名

- **问题描述**：缺少启动期Bean初始化顺序的整体依赖约束图
- **所在位置**：§3.1（策略Map构建时序）、§1.7（实施拓扑）、§3.9（AiPlatformConfig/AiPlatformEnvironmentPostProcessor），无集中说明
- **严重程度**：一般
- **改进建议**：新增独立子章节（如§3.9.1或附录）提供启动期Bean初始化顺序依赖图，覆盖AiPlatformEnvironmentPostProcessor→@ConditionalOnProperty评估、DegradationStrategy初始化→AiPlatformConfig.@PostConstruct、CapabilityExecutor初始化→AiOrchestrator.@PostConstruct、HikariCP就绪→@PostConstruct预热查询四条约束链

- **问题描述**：6项薄适配器能力的特化设计缺乏集中视图
- **所在位置**：§3.1、§3.5、§4.2（分散）；缺乏类似§3.11的集中视图
- **严重程度**：一般
- **改进建议**：在§3.11（或新增§3.12）中增加「薄适配器能力特化设计」子节，按与底座能力一致的格式，为6项薄适配器逐一列出能力标识、DTO扩展字段、Phase4服务接口引用、异常契约、依赖状态

- **问题描述**：`structuredChat()`双降级路径的根因追溯缺口
- **所在位置**：§4.1 `doExecuteInternal()`降级回调、§3.2 structuredChat回退路径超时叠加风险段
- **严重程度**：一般
- **改进建议**：在`DegradationReason.TIMEOUT`中增加细分标识（如`:StructuredChatTimeout` vs `:ChatFallbackTimeout`），或在`CompletableFuture`的`exceptionally()`回调中根据`elapsedInDoExecuteInternal`与`capabilityTimeout`比例关系推断根因

- **问题描述**：多实例配置生效时间窗口差异未做约束分析
- **所在位置**：§1.5、§3.9（热加载机制）、§10.4
- **严重程度**：一般
- **改进建议**：在§1.5或§3.9中补充配置生效时间不一致的已知约束分析，至少注释说明此差异对降级策略切换、超时阈值调整等运维操作的影响
