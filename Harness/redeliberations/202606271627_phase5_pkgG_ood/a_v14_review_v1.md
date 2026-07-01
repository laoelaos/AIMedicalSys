# OOD 设计方案审查报告（v14）

## 审查结果

REJECTED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 所有类型形态选择（interface / abstract class / class / enum / JPA @Entity）均与 Java 类型系统完全匹配，单继承 + 多接口实现约束被正确遵守；泛型 `CapabilityExecutor<T, R>` 使用方式在 Java 泛型系统能力范围内；协作关系描述的类型交互模式可在 Java 中实现。

### 2. 标准库与生态覆盖

**[通过]** 设计中依赖的标准库/框架（CompletableFuture、JPA/Hibernate、Jackson、Spring Security、Micrometer、Caffeine/Guava Cache、Spring AI）均为 Java/Spring 生态中成熟且广泛使用的组件；假设的库能力（@Async 异步队列、ObjectMapper 防御性拷贝、ConcurrentHashMap 并发安全等）合理；没有需要但无法用标准库覆盖的缺口。

### 3. 语言特性可行性

**[通过]** 错误处理策略（AiResult 包装 + DegradationReason 枚举 + try-catch 兜底）与 Java 异常处理机制匹配；并发设计（CompletableFuture 异步管线 + ThreadPoolExecutor + ConcurrentHashMap + AtomicReference CAS）在 Java 并发模型内完全可行；资源管理（Spring DI 生命周期管理）符合 Spring 框架惯例；模块/包结构遵守 Maven 多模块标准布局。

### 4. 设计一致性

**[一般]** `AbstractCapabilityExecutor.execute()` 模板方法中两处 `doDegrade()` 调用缺少 `promptVersion` 参数（§4.1 第 1521 行降级预检命中路径、第 1535 行整体超时降级路径）。`doDegrade()` 方法签名已明确包含 13 个参数（含 `outputSummary`、`promptVersion`），且文档注释（第 1596-1597 行）明确说明"预检降级路径和超时降级路径中 `promptVersion=null`"，但这两处调用仅传入 12 个参数，缺失最后一个 `promptVersion` 实参。对比之下，`doExecuteInternal()` 内 3 处降级调用（第 1566、1572、1582 行）以及薄适配器的 2 处降级调用（第 746、758 行）均已正确传入全部 13 个参数。此不一致同时在 §2.3 类图中 `AbstractCapabilityExecutor.doDegrade()` 方法签名（13 参数）与类图描述一致，进一步确认文本伪代码中存在参数遗漏。

**[轻度]** §6.4 执行时序图（第 1761 行）中使用 `sessionId = request.getSessionId()` 提取会话标识，与 §4.1 `execute()` 伪代码（第 1505 行）实际使用的 `sessionId = doExtractSessionId(request)` 不一致。Phase 4 薄适配器的 DTO 尚未继承 `AiRequestBase`，`request.getSessionId()` 在编译期不可用，时序图的写法对薄适配器场景有误导性。而 `doExtractSessionId()` 方法有默认实现（从 AiRequestBase 获取）和薄适配器重写（从 HTTP Header 提取），时序图应与之保持一致。

**[通过]** 其余协作关系描述形成闭环；行为契约描述完整度足以指导后续实现；模块间依赖方向合理，无循环依赖。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则：`AiOrchestrator` 仅负责路由委托、`AbstractCapabilityExecutor` 封装公共骨架、各 `CapabilityExecutor` 特化单能力管线、各基础设施组件（`ModelRouter`、`LlmClient`、`PromptTemplateManager` 等）各有明确定界。抽象层次恰当——模板方法模式不引入过度工程化的 Step 链抽象（已留 Phase 6）。设计便于后续详细实现和单元测试（`@MockBean` 可模拟全部下游依赖），§11 测试策略章节提供了具体的可测试性指导和验证案例。

## 修改要求（REJECTED）

### 问题 1：`execute()` 中 `doDegrade()` 调用缺少 `promptVersion` 参数

- **问题**：`AbstractCapabilityExecutor.execute()` 模板方法中，降级预检命中路径（第 1521 行）和整体超时降级路径（第 1535 行）的 `doDegrade()` 调用仅传入 12 个参数，但 `doDegrade()` 方法签名包含 13 个参数（含最后的 `promptVersion`）。
- **原因**：此不一致使设计文档内的伪代码不可执行；在实际 Java 编码中会导致编译错误（方法调用参数数量不匹配）。尤其影响降级预检路径——熔断器 OPEN 状态下被降级的请求，其 `AiCallRecord` 中的 `promptVersion` 字段将因参数缺失而非显式传 `null` 来决定值，取决于后续编码中的偶然行为。
- **建议方向**：在两处 `doDegrade()` 调用末尾补充 `, null` 作为 `promptVersion` 实参（与第 1597 行注释"预检降级路径和超时降级路径中 `promptVersion=null`"语义一致），使调用签名与方法定义对齐。
