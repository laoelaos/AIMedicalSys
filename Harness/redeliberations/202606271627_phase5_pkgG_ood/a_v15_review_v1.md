# OOD 设计方案审查报告（v15）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 所有类型形态选择（interface / abstract class / class / enum / @Entity）均与 Java 类型系统能力匹配。CapabilityExecutor<T,R> 和 LocalRuleFallback<T,R> 的泛型使用在 Java 泛型系统能力范围内。抽象继承关系符合单继承多接口实现的约束。DegradationReason、EndpointState、TemplateStatus、ExperimentStatus 等枚举定义合理。

**[通过]** DegradationContext implements Serializable 的声明与 Java 序列化机制兼容，Builder 模式配合 serialVersionUID + serializedTimestamp + postDeserializationValidate() 的三层反序列化防护方案在 Java 中可完整实现。

**[通过]** 类图中所有泛型字段（Map<String,CapabilityExecutor>、Map<String,Duration> 等）均为 Java 类型系统的标准用法。

### 2. 标准库与生态覆盖

**[通过]** 设计中依赖的标准库能力均在 Spring Boot 生态覆盖范围内：CompletableFuture（java.util.concurrent）、Spring AI（chat model 接入）、JPA（持久化）、Micrometer（指标）、Caffeine（缓存）、Guava RateLimiter（令牌桶限流）。Jackson（ObjectMapper.convertValue 防御性拷贝）。EnvironmentPostProcessor（配置转发）。

**[通过]** CredentialProvider 使用 Caffeine Cache 的凭据缓存策略合理，Vault 不可达降级行为（旧凭据延长 TTL → 抛出 CredentialUnavailableException）符合生产环境常见实践。

**[通过]** 自定义 StructuredOutputParser interface 设计合理——Jackson 反序列化为默认 JSON 实现，未来可扩展其他格式无需修改管线代码。

### 3. 语言特性可行性

**[通过]** 错误处理策略（AiResult<T> 包裹成功/失败/降级三种结果，不抛业务异常）在 Java 中可行且符合 CompletableFuture 异步契约。

**[通过]** 并发设计合理：ThreadLocal 上下文在入口处提前提取后以闭包捕获方式传递到 LlmCallExecutor 线程池，消除 SecurityContextHolder/RequestContextHolder 在线程池中丢失的风险。此项设计已在 §6.4 和 §4.1 execute() 伪代码中完整体现。

**[通过]** 资源管理方案可行：指标采集使用独立线程池（DiscardPolicy 拒绝策略不阻塞主路径），LlmCallExecutor 使用 CallerRunsPolicy 提供自然背压。

**[通过]** 模块/包结构符合 Maven + Spring Boot 项目组织方式。薄适配器对 Phase 4 业务模块的 `provided` 依赖以及 uber-JAR 部署风险在 §2.2 和 §10.3 中有明确约束记录。

**[通过]** 防御性拷贝（`ObjectMapper.convertValue(request, request.getClass())`）的语法约束已正确处理——拷贝结果存入 `defensiveCopy` 新局部变量，原始 `request` 保持 effectively final 可被 lambda 捕获。

### 4. 设计一致性

**[通过]** 各抽象职责描述清晰无歧义：AiOrchestrator（路由委托）、CapabilityExecutor（单项能力完整管线）、AbstractCapabilityExecutor（公共模板方法骨架）、LlmClient（统一 LLM 调用协议）、ModelRouter（模型路由）等职责边界明确。

**[通过]** 协作关系形成闭环：AiOrchestrator → CapabilityExecutor → (ExperimentManager / PromptTemplateManager / ModelRouter / LlmClient / StructuredOutputParser / AiMetricsCollector / SlidingWindowMetricsStore / ModelEndpointHealthManager / DegradationStrategy / LocalRuleFallback)，所有路径有明确的顺序和异常分支。

**[通过]** 上一轮 11 个审查问题（v18 修订说明）已全部修正：

| 问题 | 严重程度 | 修正确认 |
|------|---------|---------|
| 1. request 变量重赋值后捕获到 lambda | 严重 | §3.1/§4.1 改为 `defensiveCopy`，原始 request 保持 effectively final |
| 2. 薄适配器方法命名不匹配 | 严重 | `extractVisitId()` → `doExtractVisitId(request)` 等，与基类签名对齐 |
| 3. Maven 作用域矛盾 | 严重 | §2.2 统一为 `provided`，补充 uber-JAR 部署风险说明；§3.1 注释同步修正 |
| 4. DegradationReason 取值方式 | 重要 | 新增 `STRATEGY_TRIGGERED` 枚举常量，降级预检改为此常量拼接策略类名 |
| 5. capabilityTimeoutConfig 未定义 | 重要 | §2.3 类图补充字段，§3.1 补充注入来源说明 |
| 6. 薄适配器线程饥饿风险 | 中等 | §3.1 补充完整约束条件（4条）及独立线程池隔离方向 |
| 7. ModelRoute 缺 parameters 字段 | 严重 | §2.3 类图 + §3.2 字段表 + §9.5 YAML 示例均已补充 |
| 8. DegradationContext 类图不一致 | 重要 | §2.3 类图补充 serializedTimestamp/isFresh/isInitialized/postDeserializationValidate |
| 9. 缓存失效范围未定义 | 重要 | §3.3 TemplateChangedEvent + §3.4 ExperimentChangedEvent 完整定义 |
| 10. 客户端缺限流 | 重要 | §3.2 EndpointRateLimiter + 配置 + §9.5 YAML 配置块 |
| 11. AiCallLogEntity 无清理策略 | 中等 | §3.5 新增分区/保留/清理/监控完整策略 |

**[通过]** 模块间依赖方向合理：ai-api ← ai-impl，ai-impl 内部 orchestrator → 其余子包单向依赖，无循环依赖。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则：AiOrchestrator 仅做路由委托不介入管线细节，AbstractCapabilityExecutor 封装公共模板方法，各 CapabilityExecutor 子类特化差异化步骤，所有基础设施组件职责单一。

**[通过]** 抽象层次恰当：CapabilityExecutor 泛型接口 + AbstractCapabilityExecutor 抽象骨架的组合避免过度设计（13 种能力共享代码抽取而非引入 Pipeline Step 动态链），同时保留子类特化灵活性。

**[通过]** 设计便于后续详细设计和实现：伪代码完整覆盖正常路径 + 降级路径 + 异常边界（`doExecuteInternal()` 中的 `experimentManager.assign()` 和 `promptTemplateManager.render()` 均已包裹 try-catch），实现者可直接作为编码参考。

**[通过]** 设计便于单元测试：§11 测试策略明确给出 `@MockBean` 模式覆盖各类降级路径触发条件，降级预检前置验证、HTTP Header 提取、防御性拷贝、异步上下文传播、orTimeout 兜底、ModelRoute.authType 序列化等收敛验证均有对应测试场景。

## 修改要求

无。全部 11 个问题已在 v18 修订中得到解决，无新增严重或一般级问题。
