# OOD 设计方案审查报告（v23）

## 审查结果

REJECTED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** class / interface / abstract class / enum 的类型形态选择与 Java 类型系统能力匹配：单继承（AbstractCapabilityExecutor → 13 个子类）与多接口实现（AiService 被 AiOrchestrator 等实现）均符合 Java 约束

**[通过]** 泛型抽象 `CapabilityExecutor<T, R>` 的使用方式在 Java 泛型系统能力范围内

**[通过]** 协作关系中的类型交互模式（CompletableFuture 异步委托、supplyAsync 线程池编排、ObjectProvider 可选注入）均为 Java/Spring 成熟模式

**[通过]** 不可变 DTO（final 字段 + @JsonCreator + @ConstructorProperties）搭配 Jackson 反序列化在 Java 生态中可可靠实现

### 2. 标准库与生态覆盖

**[通过]** 设计依赖的 Spring Boot 组件（DI、JPA、@Async、@Scheduled、@EventListener、@ConditionalOnProperty）及第三方库（Caffeine、Guava、Jackson、Micrometer）均为标准生态覆盖

**[通过]** reactor-core 和 Spring AI 作为条件性依赖的隔离策略合理，LlmChatStreamService 的 Flux 引用已明确标注 reactor-core 为编译期强制依赖

**[通过]** 凭据通过 Vault 查询的假设合理，Caffeine Expiry 接口实现动态 TTL 的方式可行

### 3. 语言特性可行性

**[一般]** `@ConditionalOnClass` 的 `name` 数组语义声明错误——设计在 §3.2（DelegatingLlmChatService）中声称 `@ConditionalOnClass(name = {"org.springframework.ai.chat.model.ChatModel", "org.springframework.ai.chat.ChatModel"})` 的 `name` 数组为 OR 语义（类路径上存在任意一个即满足条件），但 Spring Boot 的 `@ConditionalOnClass` 对所有指定 class name 使用的是 AND 语义（所有名称必须同时存在）。因此该注解**永远不会匹配成功**（两个包路径的 ChatModel 不可能同时存在于类路径上），导致 SpringAiLlmChatService 永不被注册，SPRING_AI 客户端类型在设计中形同虚设

- **原因**：Spring AI 版本兼容性条件注册机制基于错误的框架语义假设，导致该 Bean 永远无法激活，破坏了 DelegatingLlmChatService 分发层对 SPRING_AI 客户端类型的完整支持
- **建议方向**：使用两个独立的 `@ConditionalOnClass` 注解（但同样为 AND 语义），或用自定义 `@Conditional` 实现 OR 语义检测，或使用 SpEL `@ConditionalOnExpression`，或仅使用 Spring AI 1.0.x 的目标包路径（`org.springframework.ai.chat.model.ChatModel`）并指定项目必须使用的版本

**[通过]** 错误处理策略（CompletableFuture exception handling + AiResult 包装 + 特定异常分类）与 Java/Spring 的异常处理模型一致

**[通过]** 并发设计（入口处 ThreadLocal 提取 + 闭包捕获 + 线程池隔离）在 Java 并发模型内可行

**[通过]** 模块/包结构（ai-api / ai-impl 分层，provided 作用域依赖 Phase 4 模块）符合 Maven 项目约定，uber-JAR 风险已显式记录

### 4. 设计一致性

**[通过]** 各抽象职责清晰（四元组格式），协作关系形成闭环（AiService → FallbackAiService → AiOrchestrator → CapabilityExecutor → LlmChatService/ModelRouter/...）

**[通过]** 模块间依赖方向合理，无循环依赖（ai-api ← ai-impl，ai-impl 内部 orchestrator → 其余子包，thin-adapter → Phase 4 模块为出向依赖）

**[通过]** 迭代要求中列出的 4 项问题均已确认修复：
1. `doDegrade` 签名一致：类图（§2.3 第 461 行）、§4.1 方法定义（第 3363 行）、§3.1 预检/超时降级路径（第 3091/3114 行）、§4.2 薄适配器降级路径（第 3565/3590 行）等全部 ~15 处调用点一致使用 15 参数签名，参数位置与数量完全匹配
2. 薄适配器两阶段异常检测：§4.2 第 3575–3577 行实现了 `instanceof Phase4BusinessException`（主判定）+ `isKnownPhase4BusinessException()`（过渡期回退），与 §3.1 第 1279–1283 行一致
3. v22 修订 5 项变更已落地：CallContext 标注为"Phase 5 第二阶段重构目标"、parseTimeout 路径使用 `${ai.execution.timeout.parse.default:5s}`、版本标记已清理、AiOrchestrator 临界条件已修正（`&&`→`||` + 逐字段 if-null 守卫）、estimateTokens 已增加精确 Tokenizer 分支
4. 预检降级路径参数完整：第 3091 行传入 15 参数，最后 4 个 `null` 对应 outputSummary/promptVersion/modelId/sentinelReason

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则（AiOrchestrator 仅路由、CapabilityExecutor 持有管线、LlmChatService 仅封装 LLM 通信）

**[通过]** 抽象层次恰当：AbstractCapabilityExecutor 模板方法模式使子类仅需特化 doExecuteInternal()

**[通过]** 设计便于后续详细设计及实现：13 种能力统一规约，新增能力仅需新增 XxxCapabilityExecutor 实现即自动注册

**[通过]** 设计便于单元测试：接口注入实现可 mock，降级路径触发条件明确，状态机可预置后验证转换

## 修改要求

### 问题 1（Dimension 3 - 一般）：`@ConditionalOnClass` OR 语义假设错误

- **问题**：§3.2 DelegatingLlmChatService 中 `@ConditionalOnClass(name = {"org.springframework.ai.chat.model.ChatModel", "org.springframework.ai.chat.ChatModel"})` 被设计描述为 OR 语义（存在任意一个即匹配），但 Spring Boot 的 `@ConditionalOnClass.name` 数组实际使用 **AND** 语义（所有 class 必须同时存在）。两个包路径的 `ChatModel` 不可能同时存在于类路径上，故该条件永不为 true，`SpringAiLlmChatService` 永远不被注册
- **原因**：`@ConditionalOnClass` 的 `name` 属性对所有指定名称使用 AND 语义，而非设计文档中声明的 OR 语义。此错误导致 SPRING_AI 客户端类型在运行时永远不可用，破坏了 Delegate 分发层的设计完整性
- **建议方向**：
  1. **方案 A（推荐，零额外依赖）**：仅保留一个目标包路径（如 `org.springframework.ai.chat.model.ChatModel`），在项目 pom.xml 中锁定 Spring AI 版本，消除版本兼容性检测需求
  2. **方案 B**：编写自定义 `@ConditionalOnClassOr` 注解 + `OnClassOrCondition`，覆盖 `getMatchOutcome` 实现 OR 语义匹配
  3. **方案 C**：将两个 `springAiLlmChatService` 的 `@Bean` 方法各标注一个独立的 `@ConditionalOnClass`，但需注意两个方法可能同时注册（两者在 AND 语义下仍无法分别匹配各自目标版本）
  4. **方案 D**：使用 `@ConditionalOnExpression(T("{org.springframework.ai.chat.model.ChatModel}") or T({org.springframework.ai.chat.ChatModel}))` 实现 OR 语义检测
