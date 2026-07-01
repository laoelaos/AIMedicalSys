# OOD 设计方案审查报告（v4）

## 审查结果

REJECTED

## 逐维度审查

### 1. 类型系统可行性

**[通过]**
- class、interface、abstract class（AiRequestBase）、enum、JPA @Entity 等类型形态选择均与 Java 类型系统完全匹配
- 单继承（AiRequestBase）+ 多接口实现符合 Java 约束
- CapabilityExecutor<T, R> 泛型接口的使用在 Java 泛型系统能力范围内（类型擦除不影响架构可行性）
- DegradationStrategy.getOrder() 采用 Java 8 default method，兼容现有实现
- CompletableFuture<AiResult<R>> 异步返回类型标准可行

### 2. 标准库与生态覆盖

**[通过]**
- Spring Boot（Spring AI、Spring Data JPA、Spring @Async）+ Micrometer 组成成熟生态，覆盖设计中的模板管理、实验分流、指标采集、异步写入全部需求
- Jackson ObjectMapper、ConcurrentHashMap、AtomicReference/CAS 等 JDK/Spring 标准组件覆盖降级判定、滑动窗口存储等需求
- EnvironmentPostProcessor 为 Spring 标准扩展点，用于配置转发可行
- @ConditionalOnProperty + @Primary + ObjectProvider 的 Bean 装配策略在 Spring Boot 生态中标准可行

### 3. 语言特性可行性

**[一般]** **ModelEndpointHealthManager 的健康检查与探测调用在管线中无执行路径**
§3.2 描述 "AiOrchestrator 在管线执行前检查目标模型端点的健康状态" 并定义了探测触发机制，但 v4 已将 AiOrchestrator 定位为纯路由层（§3.1，"AiOrchestrator 不介入管线内部步骤"），管线执行全部在 CapabilityExecutor.execute() 内部完成。而 CapabilityExecutor 的 execute() 伪代码（§4.1）中未包含任何对 ModelEndpointHealthManager 的调用，导致该组件的健康状态检查、状态转换和探测调用触发机制无代码执行入口。

**原因**：v4 将管线所有权从 AiOrchestrator 移至 CapabilityExecutor 时，§3.2 中健康检查的执行角色未同步修订，导致功能设计（ModelEndpointHealthManager 的状态管理能力）与架构职责划分（谁触发检查）脱节。

**建议方向**：将健康检查明确归入 CapabilityExecutor.execute() 管线步骤之一（例如在模型路由步骤之后、LLM 调用之前检查 ModelEndpointHealthManager.getState()），或归入 ModelRouter.route() 实现内部（无健康路由时返回 null 自然触发降级路径），并在 §4.1 伪代码中补充相应步骤。

**[通过]**（其余部分）：
- 错误处理策略（AiResult.degraded() 非异常路径）与 Java 异常处理机制兼容
- 并发设计（ConcurrentHashMap + AtomicReference/CAS + 读写分离快照）在 Java 并发工具集内完全可行
- @ConditionalOnProperty 互斥切换 + ObjectProvider 延迟解析的 Bean 装配方案可行
- 模块/包结构符合 Maven 单体仓库组织方式

### 4. 设计一致性

**[通过]**
- 各抽象职责描述清晰无歧义：AiOrchestrator 管路由注册，CapabilityExecutor 管管线执行，职责边界明确
- 协作关系形成完整闭环：业务模块 → AiService → FallbackAiService → AiOrchestrator（按能力标识查找）→ CapabilityExecutor（完整 8 步管线）→ 各子组件
- 行为契约（§4 伪代码）详细到足以指导后续实现
- 模块间依赖方向单向清晰（ai-api ← ai-impl；impl 内部 orchestrator → 其余子包），无循环依赖
- v4 版本中，迭代反馈的全部 10 个问题均已完成针对性修正

### 5. 设计质量

**[通过]**
- 职责划分遵循单一职责原则：每项能力独立 CapabilityExecutor 实现，路由与执行分离
- 抽象层次恰当：泛型接口保证类型安全，同时每能力保留定制扩展点；Step 链抽象留待 Phase 6，不过度设计
- 便于后续详细设计和实现：目录结构（§2.1）、方法签名（§3.1）、伪代码管线（§4.1）均已提供
- 便于单元测试：CapabilityExecutor 接口通过 Mock 子组件可独立测试；SlidingWindowMetricsStore 作为独立数据源可单独验证

## 修改要求（REJECTED）

### 问题：ModelEndpointHealthManager 健康检查在管线中无执行路径

- **问题**：§3.2 定义 ModelEndpointHealthManager 的健康状态检查由 AiOrchestrator 执行，但 v4 已将管线执行全部移至 CapabilityExecutor，健康检查职责无归属方
- **原因**：该脱节将导致在运行时 ModelEndpointHealthManager 维护的端点状态（CONNECTED/DEGRADED/UNAVAILABLE）从未被读取，状态转换逻辑（DEGRADED → CONNECTED 的自动恢复、UNAVAILABLE 下每 30 秒探测）从未被触发，设计中的健康管理能力成为死代码
- **建议方向**：将健康检查明确纳入 CapabilityExecutor.execute() 管线（作为第 4.5 步——在模型路由之后、LLM 调用之前），或在 ModelRouter.route() 内部集成无健康路由自动降级逻辑，同步更新 §3.2 和 §4.1 伪代码
