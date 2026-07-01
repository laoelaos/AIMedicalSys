# 再审议判定报告（v2）

## 判定结果

RETRY

## 判定理由

组件B诊断报告识别出3项严重（CRITICAL）问题、1项重要（MAJOR）问题及4项中等（MEDIUM）问题。质询报告结果为LOCATED，确认了审查结论的有效性（仅指出1项轻微覆盖缺口不影响已识别问题的可信度）。内部循环实际轮次（1）远小于最大轮次（12），说明审查在首轮即定位到明确问题。根据判定标准，审查报告包含严重及一般等级的问题，应判定为RETRY。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：§4.1 降级路径伪代码中指标采集语句位于 return 之后不可达
- **所在位置**：§4.1 降级路径伪代码（第 843-850 行）
- **严重程度**：严重
- **改进建议**：将指标采集和滑动窗口记录移到 return 语句之前，或重构为 try-finally 确保无论走哪个降级分支均能记录指标

- **问题描述**：MockAiService 的 @ConditionalOnProperty 属性名与现有代码不一致
- **所在位置**：§3.1 Bean 装配策略 + §9.2 配置切换 + 对比现有 MockAiService.java:40
- **严重程度**：严重
- **改进建议**：保留 `ai.mock.enabled` 属性兼容存量配置，或显式说明迁移方案

- **问题描述**：DegradationStrategy 接口新增 getOrder() 方法破坏现有实现
- **所在位置**：§3.1 降级判定流程 + §3.8 DegradationStrategy 类图 + 现有 DegradationStrategy.java:3-6
- **严重程度**：严重
- **改进建议**：改为 Java 8 default 方法或使用 Spring @Order 注解 + Ordered 接口替代

- **问题描述**：类图中 AiService 方法签名缺少 CompletableFuture 异步包装
- **所在位置**：§2.3 类图 AiService、AiOrchestrator、CapabilityExecutor 方法签名
- **严重程度**：重要
- **改进建议**：类图中方法签名修正为 CompletableFuture<AiResult<T>>，同步管线桥接异步接口的设计决策应在 §3.1 或 §6 中明确

- **问题描述**：降级路径中 localRuleFallback 成功场景被错误记录为 recordFailure
- **所在位置**：§4.1 降级路径伪代码第 850 行
- **严重程度**：中等
- **改进建议**：区分"完全退化→recordFailure"与"降级到本地规则成功→recordSuccess（标记 degraded=true）"

- **问题描述**：AiRequestBase 基类在设计和代码库中均未定义
- **所在位置**：§3.5 AiCallRecord 字段填充策略
- **严重程度**：中等
- **改进建议**：显式定义 AiRequestBase 基类（字段、包路径、继承关系）或在类图中补充

- **问题描述**：类图中 AiService 方法名与现有接口不匹配
- **所在位置**：§2.3 类图
- **严重程度**：中等
- **改进建议**：将类图中方法名修正为与实际代码一致

- **问题描述**：LlmClient 状态归属存在表述矛盾
- **所在位置**：§3.2 vs §6.1
- **严重程度**：中等
- **改进建议**：明确状态模型归属组件，补充探测调用触发机制设计

- **问题描述**：AiCallLogEntity 遗漏字段级 JPA 映射索引覆盖度不足
- **所在位置**：§3.5 AiCallLogEntity 表索引策略
- **严重程度**：中等
- **改进建议**：补充降级/模型/角色维度的覆盖索引评估，明确 call_time 列定义
