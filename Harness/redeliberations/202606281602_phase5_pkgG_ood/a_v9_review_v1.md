# OOD 设计方案审查报告（v9）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 所有类型形态选择（interface / abstract class / class / enum / @Entity / @Configuration）均与 Java 类型系统能力匹配。抽象继承层次符合单继承 + 多接口实现约束（`AbstractCapabilityExecutor` 作为唯一抽象基类，各具体 Executor 通过 extends 获得共享实现，通过 implements `CapabilityExecutor` 接口获得契约）。泛型使用方式（`CapabilityExecutor<T,R>`、`StructuredChatResult<T>`、`LocalRuleFallback<T,R>`）均在 Java 泛型系统能力范围内，且对类型擦除风险有明确防御（构造器注入 `Class<T> inputType` + `isInstance()` 运行时类型检查）。协作关系中的类型交互模式（`CompletableFuture<AiResult<T>>` 异步管线、`Map<String, CapabilityExecutor>` 查找-委托模式）均可在 Java 中实现。

**[轻微]** `CallerRole` 提取在 `AbstractCapabilityExecutor.extractCallerRole()`（优先 ROLE\_ 前缀过滤后去前缀）与 `AiOrchestrator.handle()` catch 块（直接取第一个 authority 原始值）两处采用了不一致的策略。虽不影响设计可行性，但可能导致同一能力在不同执行路径下 `AiCallRecord.callerRole` 字段值不统一（如 "DOCTOR" vs "ROLE_DOCTOR"），建议对齐为同一种提取逻辑。

### 2. 标准库与生态覆盖

**[通过]** 设计中所需的基础设施能力全部在 Spring Boot 生态标准库覆盖范围内：Spring Data JPA（持久化）、Spring Security（认证/上下文）、Jackson（序列化/防御性拷贝）、Caffeine（本地缓存）、Guava RateLimiter（令牌桶限流）、Micrometer（指标暴露）。Spring AI 和 Reactor 作为可选依赖，通过 `@ConditionalOnClass` + `ObjectProvider` 安全处理缺失场景。所有假设（如 `SecurityContextHolder` 在容器线程可用、`RequestContextHolder` 在 HTTP 场景可用）合理。

### 3. 语言特性可行性

**[通过]** 错误处理策略与 Java 异常体系匹配：`StructuredOutputNotSupportedException` / `LlmInfrastructureException` 作为 `RuntimeException` 子类，按异常类型拆分 catch 分支。并发设计（`CompletableFuture` + 线程池 + `ForkJoinPool`）标准可用，异步上下文通过"入口处提取 + 闭包捕获"模式解决 ThreadLocal 传播问题。资源管理（线程池参数、连接超时、防御性拷贝）在 Java 资源管理模式内可行。模块/包结构清晰，符合 Maven 项目组织规范。

**[轻微]** `ForkJoinPool.commonPool()` 用于薄适配器委托时的阻塞等待（`delegateFuture.get(timeout, unit)`），文档已列出 4 项约束条件并提供了演进方案（独立线程池隔离）。此设计在当前的并发规模和配置假设下可行，但建议在实现期预留线程池替换的扩展点（如通过 `@Qualifier("thinAdapterExecutor")` 注入），避免后续引入独立线程池时需修改 `doExecuteInternal()` 方法体。

### 4. 设计一致性

**[通过]** 各抽象职责描述清晰无歧义，协作关系形成闭环（编排 → 实验分流 → 模板渲染 → 模型路由 → LLM 调用 → 解析 → 采集 → 降级兜底，每条路径完整可追踪）。行为契约完整（§4 伪代码覆盖了完整管线、薄适配器特化、路由、A/B 实验、模板渲染、解析、指标采集共 7 个契约点）。模块间依赖方向合理（ai-api ← ai-impl，orchestrator → 其余子包，子包间不互相依赖），无循环依赖。

所有 5 项审查意见（A-E）均在 v9 中获得修复：
- **问题A**：§4.1 新增 `structuredChatTimeout`（60%）和 `chatFallbackTimeout`（40%）独立内部超时阈值，`CompletableFuture.orTimeout()` 分别绑定
- **问题B**：§1.4 新增"底座切流初期已知限制"小节，集中列表说明 6 项受限能力；§9.1 迁移表每项薄适配器行标注依赖状态
- **问题C**：§4.1 中 4 个实验分流后的降级调用点均已传入 `promptVersion`（非 null）；预检/超时降级路径中 `promptVersion=null` 保留（语义正确）
- **问题D**：§4.1 `AiOrchestrator.handle()` catch 块新增 `SecurityContextHolder` null-safe 提取 `callerRole`/`callerId`
- **问题E**：`doDegrade()` 新增 `modelId` 参数，实验分流后的降级调用点传入 `modelRoute.getModelId()`，预检/超时/薄适配器降级传入 null

### 5. 设计质量

**[通过]** 职责划分符合单一职责原则（编排、路由、LLM 调用、模板管理、实验、指标采集、降级策略各司其职）。抽象层次恰当 — 架构级设计未过度指定实现细节，但通过 §3 核心抽象和 §4 伪代码给出了充分的指导。设计便于后续详细实现（模板方法模式降低子类实现门槛，泛型接口保证类型安全）。设计便于单元测试（接口契约清晰、可 mock、§11 有明确的测试策略和场景覆盖）。

## 修改要求

无严重或一般问题，APPROVED。
