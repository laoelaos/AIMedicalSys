# 再审议判定报告（v20）

## 判定结果

RETRY

## 判定理由

诊断报告（b_v20_diag_v3）包含 6 个问题：问题 1（LlmChatService 选择机制缺失，Bean 装配二义性，编码阻塞级）、问题 2（structuredChat 回退异常捕获粒度过粗，异常分类表与实现矛盾）、问题 3（RouteConfigChangedEvent 未定义）、问题 4（熔断器可观测性缺失）、问题 5（HTTP Header 隐含假设）、以及修订说明中新增的 protected 方法跨类调用编译错误。其中问题 1、问题 2 及新增编译错误均达到"重要"级别，属于事实错误或关键遗漏，直接影响编码实现；问题 3-5 为"中等"级别，影响产出完整度。

质询报告（b_v20_challenge_v3）结果为 LOCATED，确认了诊断发现的有效性，仅指出主文与修订说明之间的一致性瑕疵。实际轮次（3）< 最大轮次（12），组件 B 在确认 LOCATED 后提前终止。

根据判定标准，诊断报告包含严重/一般等级的问题，不符合 PASS 条件，判定为 RETRY。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：`LlmChatService` 多实现实例的选择机制未定义，导致 Bean 装配二义性（NoUniqueBeanDefinitionException）及 `ModelRoute.clientType` 路由与实际实现脱节
- **所在位置**：§2.3 类图、§3.2 LlmChatService 接口与实现节、§4.1 管线伪代码
- **严重程度**：严重
- **改进建议**：补充 `DelegatingLlmChatService` 分发层，内部持有两个实现并按 `ModelRoute.clientType` 委托；或冻结设计承诺删除 `ModelRoute.clientType`

- **问题描述**：`structuredChat()` 回退路径的异常捕获粒度过粗，`catch (Exception e)` 未区分格式异常与基础设施异常，与 §5.1 错误分类表矛盾
- **所在位置**：§4.1 `doExecuteInternal()` 管线伪代码第 1972-1978 行
- **严重程度**：一般
- **改进建议**：拆分为两个 catch 分支——格式类异常回退 chat()，基础设施异常直接进入降级路径

- **问题描述**：`RouteConfigChangedEvent` 事件定义缺失，仅有名称无字段定义
- **所在位置**：§6.1 第 2181 行
- **严重程度**：一般
- **改进建议**：在 §3.2 或 §6 末尾补充 Payload 定义及刷新策略

- **问题描述**：`CircuitBreakerDegradationStrategy` 状态可观测性缺失，无状态查询接口
- **所在位置**：§3.8、§3.5
- **严重程度**：一般
- **改进建议**：补充 `getState(capabilityId)` 接口，通过 Micrometer Gauge 暴露

- **问题描述**：X-Department-ID HTTP Header 提取的依赖假设未显式约定，非 HTTP 场景下提取失败
- **所在位置**：§3.10 `extractFromRequestContext()`、§4.1 `AiOrchestrator.handle()`、§3.1 薄适配器
- **严重程度**：一般
- **改进建议**：新增"调用方 Header 契约"小节，补充非 HTTP 场景替代提取路径

- **问题描述**：`extractFromRequestContext()` 为 `AbstractCapabilityExecutor` 的 `protected` 方法，但在非继承类 `AiOrchestrator.handle()` 中直接调用，编译非法
- **所在位置**：§3.10 第 1804-1808 行、§4.1 第 1862-1868 行
- **严重程度**：严重
- **改进建议**：将 `extractFromRequestContext()` 提取为独立工具类的 `public static` 方法
