根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

1. **reactor-core 依赖性质存在事实矛盾**（严重）：§3.2 宣称流式接口独立于非流式、reactor-core 为可选依赖，但 LlmChatStreamService 接口方法签名直接引用 `Flux<LlmChatResponse>`，编译器层面要求 reactor-core 必须在编译期 classpath 上。§8.2 中的 `<optional>true</optional>` 被注释掉（实际为强制依赖）。改进建议：方案(a)将 LlmChatStreamService 接口定义移入独立 `ai-stream` 子模块彻底隔离 reactor 依赖；方案(b)如实承认 reactor-core 为强制依赖，删除"隔离 reactor-core"的宣称。

2. **薄适配器超时配置示例违反自身层级约束**（严重）：§3.1 要求薄适配器场景 `capabilityTimeout` 应设为 `thinAdapterTimeout` + 缓冲值（如 +5s），但 §9.5 YAML 配置示例中薄适配器能力的 `per-capability` 值均设为 `30s`，与 `thin-adapter-default: 30s` 相等，无任何缓冲。改进建议：将 §9.5 中 6 项薄适配器能力的 `per-capability` 超时值从 `30s` 修正为 `35s`，并在 YAML 注释中显式标注层级约束。

3. **LocalRuleFallback 泛型方法 vs 实际管线调用类型不一致**（重要）：§2.3 类图将 `LocalRuleFallback<T, R>` 的 `fallback()` 签名定义为 `fallback(T request) AiResult<R>`，但 §4.1 调用时传入的 `request` 由于泛型擦除存在 unchecked 转换，可能在 Phase 4/Phase 5 DTO 混合场景下抛出 `ClassCastException`。改进建议：在 §7 或 §3.1 中记录此 unchecked 转换风险，推荐在 `AbstractCapabilityExecutor` 中以 `Class<T>` 字段显式持有类型信息，或在 `doDegrade()` 中增加类型检查日志。

4. **SlidingWindowMetricsStore 惰性淘汰写/读并发竞争条件未定义**（重要）：§3.5 定义的惰性淘汰策略——写方法和读方法均在入口处执行过期事件移除，但两线程的淘汰操作可能针对同一 Deque 产生竞争。文档仅声明"写锁保护队列尾写入"，但惰性淘汰操作涉及队列头和队列尾同时操作，锁范围不足。改进建议：明确惰性淘汰与写入的锁协议——统一纳入同一 `synchronized` 块，或使用 `ReentrantReadWriteLock` 分离读写路径。

5. **ModelEndpointHealthManager 的 DEGRADED 状态行为空白**（重要）：§3.2 状态模型定义 `DEGRADED` 状态语义为"仍然尝试调用，但上报告警"，但 §4.1 `doExecuteInternal()` 伪代码中仅处理了 `UNAVAILABLE` 状态分支，`DEGRADED` 状态直接放行无告警日志也无性能指标记录。同时，LLM 调用后未调用 `recordCallResult()` 更新端点健康状态，导致 DEGRADED→CONNECTED 的恢复路径无法触发。改进建议：在 `DEGRADED` 分支插入 WARN 日志和指标记录；在 LLM 调用成功/失败后补充 `endpointHealthManager.recordCallResult()` 调用。

6. **RequestContextUtils.extractFromRequestContext() 存在 ClassCastException 风险**（重要）：§3.10 `extractFromRequestContext()` 实现直接进行 `(ServletRequestAttributes) RequestContextHolder.getRequestAttributes()` 的强转，当底座运行在非 Spring MVC 环境（如 WebFlux）下时抛出 `ClassCastException`。改进建议：使用 `instanceof` 安全检查替代直接强转。

## 历史迭代回顾

回顾 v1~v22 全部历史反馈，对 v23 诊断的 6 项问题标注如下：

- **已解决的问题**（出现在历史反馈但当前反馈中不再提及的问题）：
  - §4.1 伪代码中未定义变量、返回类型与方法签名不一致、不可达语句等语法级问题（v2/v5/v9/v11/v14/v17/v18 等多轮已修复）
  - Bean 装配二义性、FallbackAiService 构造器兼容性、MockAiService 条件注解等装配问题（v1/v2/v3/v5/v20 已修复）
  - AiOrchestrator 能力标识映射表、薄适配器依赖作用域、模块依赖方向等结构性问题（v3/v4/v12/v13/v14 已修复）
  - 异步边界、线程上下文传播、降级预检位置、CallerRunsPolicy 饥饿等并发问题（v5/v8/v10/v11 已修复）
  - Experiment PAUSED 语义、Prompt 模板版本分流、无实验命中返回值等实验管理问题（v5/v9/v12/v13 已修复）
  - AiCallRecord/AiCallLogEntity 字段缺失（departmentId/promptVersion）、工厂方法签名等数据模型问题（v5/v8 已修复）
  - 类图与文本定义的不一致、类图中方法签名遗漏等图示问题（v4/v7/v14/v15/v17 已修复）
  - DegradationContext 序列化兼容性、零值上下文过渡路径等兼容性问题（v1/v4/v6/v14/v22 已修复）
  - 不可变 DTO 与防御性拷贝的兼容性说明、Jackson 注解要求（v13 已修复）
  - doDegrade() 方法签名缺少参数（departmentId/promptVersion/outputSummary）（v8/v12/v13 已修复）
  - Maven 依赖作用域矛盾（v12/v14 已修复，统一为 provided）
  - ModelRoute 字段缺失（parameters/authentication）（v8/v11/v14 已修复）
  - AiPlatformConfig 生命周期冲突（EnvironmentPostProcessor 与 ApplicationContextAware 双实例）（v18 已修复，剥离为独立类）
  - DelegatingLlmChatService 注入缺陷（v22 已修复，改为 ObjectProvider）
  - ai-impl/pom.xml 依赖声明缺失（v22 已修复，补充 §8 依赖清单）
  - AiPlatformConfig 单一 @ConfigurationProperties 违反职责分离（v22 已修复，拆分为多个配置属性类）
  - extractFromRequestContext() 的 protected 访问修饰符问题（v20 已修复，提取为独立工具类）

- **持续存在的问题**（在多轮反馈中反复出现的问题，需重点解决）：
  - **reactor-core 依赖隔离**（Issue 1）：v19 第 1 项即指出 HttpApiLlmChatService/SpringAiLlmChatService 同时实现两个接口与 reactor-core 隔离设计矛盾，v23 再次从编译事实层面确认同一矛盾仍未解决。建议本轮采用方案(a) 移入独立子模块彻底隔离，或方案(b) 如实承认强制依赖。
  - **LocalRuleFallback 类型安全**（Issue 3）：v7 第 7 项已指出 LocalRuleFallback 的 unchecked 类型转换问题，v23 从新的角度（泛型擦除 + ClassCastException 风险）再次确认，问题核心一直未解决。建议本轮在 §7 设计决策表记录风险并增加 Class<T> 显式类型信息。
  - **超时层级关系一致性问题**（Issue 2 的部分根因）：v22 第 9 项指出混合完整管线与薄适配器的超时层级存在歧义，v23 从 YAML 配置示例层面发现了具体违反约束的实例。建议本轮统一修正 YAML 配置示例。

- **新发现的问题**（本轮新识别的问题）：
  - **SlidingWindowMetricsStore 惰性淘汰并发竞争条件**（Issue 4）：之前仅涉及 WindowedEvent 定义和线程安全段描述，但未从惰性淘汰操作涉及队列头+队列尾同时操作、锁范围不足的角度审查。
  - **ModelEndpointHealthManager DEGRADED 状态行为空白**（Issue 5）：之前反馈覆盖了状态转换路径缺失和统一探测机制，但未深入到管线伪代码中 DEGRADED 分支的处理空白和 recordCallResult() 调用缺失。
  - **extractFromRequestContext() ClassCastException 风险**（Issue 6）：之前反馈覆盖了访问修饰符和命名统一问题，但未从非 Servlet 环境下的运行时 ClassCastException 角度审查。

## 上一轮产出路径
C:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606271627_phase5_pkgG_ood/a_v23_copy_from_v22.md

## 用户需求
C:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606271627_phase5_pkgG_ood/requirement.md
