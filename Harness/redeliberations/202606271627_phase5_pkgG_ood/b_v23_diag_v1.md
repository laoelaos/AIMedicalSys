# v23 质量审查报告

## 审查概要

审查范围：Phase 5 包 G — AI 进阶底座 架构级 OOD 设计方案 v23
审查视角：从实际落地（编码实现）视角评估
审查重点：事实一致性、编译可行性、上下位矛盾、关键遗漏

整体评价：经过 22 轮迭代，文档在覆盖度、细节深度、设计决策记录方面已达到较高成熟度。核心抽象定义清晰，类图与文本一致度高，管线伪代码覆盖了绝大部分降级路径和异常处理。以下发现聚焦于实际编码中会遇到的阻塞性问题或编译/运行时矛盾。

---

## 发现问题

### 1. reactor-core 依赖性质存在事实矛盾

**描述**：§3.2（LlmChatStreamService）宣称流式接口独立于非流式，通过独立接口将 reactor-core 隔离为"可选依赖、非流式场景无需引入"。但 `LlmChatStreamService` 接口的方法签名直接引用 `Flux<LlmChatResponse>`（§2.3 类图第 316 行、§3.2 第 1127 行），这在 Java 编译器层面要求 reactor-core 必须在 **编译期 classpath** 上。§8.2 依赖清单中 reactor-core 的 `<optional>true</optional>` 被注释掉（实际为强制依赖）。编译事实与设计宣称矛盾——只要 `LlmChatStreamService.java` 存在于 `ai-impl` 模块中，所有对该模块的编译（包括仅依赖同步管线的 CapabilityExecutor）都必须携带 reactor-core。

**位置**：§3.2 LlmChatStreamService 独立理由（第 1133-1136 行） vs §8.2 条件性依赖（第 2691-2699 行）

**严重程度**：严重

**改进建议**：两种消解方案择一：
- (a) 将 `LlmChatStreamService` 接口定义移入独立的 `ai-stream` 子模块（新增 `ai-impl-stream`），reactor-core 作为该子模块的编译依赖，`ai-impl` 主体模块不感知 Flux 类型。该方案代价是新增模块拆分，但彻底隔离 reactor 依赖。
- (b) 如实承认 reactor-core 为 `ai-impl` 的编译期强制依赖，删除 §3.2 中"隔离 reactor-core"的宣称，将 §8.2 中 reactor-core 的 `<optional>` 标记去掉。该方案代价是非流式部署场景下也会引入 reactor-core JAR（但运行时无类加载开销）。

---

### 2. 薄适配器超时配置示例违反自身层级约束

**描述**：§3.1 超时层级关系明确要求"薄适配器场景 `capabilityTimeout` 应设置为 `thinAdapterTimeout` + 合理缓冲值（如 +5 秒），确保内部 `delegateFuture.get(thinAdapterTimeout)` 优先触发"。但 §9.5 YAML 配置示例中薄适配器能力的 `per-capability` 值均设为 `30s`，与 `thin-adapter-default: 30s` 相等，无任何缓冲。这意味着外层 `orTimeout(30s)` 与内部 `delegateFuture.get(30s)` 将在同一时刻超时，两超时路径的竞选条件会导致同一请求可能被降级两次或出现不可预知的超时异常传播。

**位置**：§3.1 超时层级关系段（第 1071-1073 行） vs §9.5 per-capability 配置（第 2867-2872 行）

**严重程度**：严重

**改进建议**：将 §9.5 中 6 项薄适配器能力的 `per-capability` 超时值从 `30s` 修正为 `35s`（30 + 5s 缓冲），并在 YAML 注释中显式标注此层级约束。例如：
```yaml
DIAGNOSIS: 35s  # >= thin-adapter-default(30s) + 5s 缓冲，确保内部超时优先触发
```

---

### 3. §3.7 LocalRuleFallback 泛型方法 vs 实际管线调用类型不一致

**描述**：§2.3 类图将 `LocalRuleFallback<T, R>` 的 `fallback()` 方法签名定义为 `fallback(T request) AiResult<R>`。但 §4.1 `doDegrade()` 伪代码（第 2347 行）调用 `localRuleFallback.fallback(request)` 时传入的 `request` 是 `defensiveCopy`（类型为 `Object`，由 `doDegrade()` 方法签名中的泛型 `request` 参数表示）。实际执行时，由于 `AbstractCapabilityExecutor` 和子类的类型擦除，`localRuleFallback` 注入时使用 `@Autowired(required = false) LocalRuleFallback<T, R>`，Java 的泛型擦除无法在运行时区分 `T` 的具体类型。这导致 `fallback()` 调用存在 unchecked 类型转换，编译器会产生警告，且在 Phase 4 DTO 与 Phase 5 DTO 混合场景下可能抛出 `ClassCastException`。

**位置**：§4.1 doDegrade() 第 2347 行；§3.7 类图第 573-576 行；§3.1 构造器注入第 824-828 行

**严重程度**：重要

**改进建议**：在 §7 设计决策表或 §3.1 中记录此 unchecked 转换风险，并明确约束：`LocalRuleFallback` 的 `T` 必须与所在 `CapabilityExecutor` 的 `T` 类型一致。推荐在 `AbstractCapabilityExecutor` 中以 `Class<T>` 字段显式持有类型信息，在注入时执行 `assert` 校验或在构造器中用 `inputType.cast()` 包装调用。或者在 `doDegrade()` 中增加类型检查日志：
```
// LocalRuleFallback 调用前日志：capabilityId, requestType
// 若 ClassCastException 发生，降级到 AiResult.degraded() 而非传播异常
```

---

### 4. SlidingWindowMetricsStore 惰性淘汰写/读并发竞争条件未定义

**描述**：§3.5 定义了惰性淘汰策略——写方法和读方法均在入口处执行过期事件移除。但文档未覆盖并发场景：线程 A 在 `recordSuccess()` 入口处执行惰性淘汰（遍历 Deque 头部移除过期事件，加锁），线程 B 同时在 `getFailureRate()` 入口处执行惰性淘汰（快照复制前同样移去过期事件，加锁）。两线程的淘汰操作可能针对同一 Deque 产生竞争：线程 A 移除事件后线程 B 的快照复制拿到的是部分更新状态，或者线程 B 移除后线程 A 的写入位置与淘汰边界冲突。文档仅声明"写锁保护队列尾写入"，但惰性淘汰操作涉及队列头和队列尾同时操作，锁范围不足。

**位置**：§3.5 SlidingWindowMetricsStore 线程安全段（第 1800 行）和惰性淘汰规则（第 1811 行）

**严重程度**：重要

**改进建议**：明确惰性淘汰与写入的锁协议——要么统一将所有操作（写 + 惰性淘汰 + 快照复制）纳入同一 `synchronized` 块，要么使用 `ReentrantReadWriteLock` 分离读写路径（读操作加读锁、写操作加写锁，惰性淘汰在 WriteLock 下执行）。同时在 §7 设计决策表中记录所选方案及理由。推荐使用 `ReentrantReadWriteLock`：读路径（`getFailureRate()` 等）无锁化快照 + 写路径加锁，与当前写路径加锁的设计一致，仅需补充读路径的锁范围说明。

---

### 5. §4.1 完整管线中 endpointHealthManager 的 DEGRADED 状态行为空白

**描述**：§3.2 ModelEndpointHealthManager 状态模型定义了 `DEGRADED` 状态（第 1209 行）："仍然尝试调用，但上报告警"。但在 §4.1 `doExecuteInternal()` 伪代码（第 2259-2262 行）的健康检查步骤中，仅处理了 `UNAVAILABLE` 状态分支，`DEGRADED` 状态既无告警日志也无性能指标记录，直接放行进入 LLM 调用。这意味着端点从 `CONNECTED` 退化到 `DEGRADED` 后，运维系统无法感知到退化事件，导致"DEGRADED 上报告警"的设计承诺在管线层未兑现。同时，LLM 调用后未调用 `recordCallResult()` 更新端点健康状态，导致 DEGRADED→CONNECTED 的恢复路径（连续调用正常后状态回升）无法触发。

**位置**：§4.1 doExecuteInternal() 健康检查段（第 2259-2262 行）；§3.2 状态模型 DEGRADED 定义（第 1208-1209 行）；§3.2 状态转换表（第 1217 行）

**严重程度**：重要

**改进建议**：
- 在 `getState()` 返回 `DEGRADED` 时插入 WARN 日志和指标记录（如计数器 `aimedical.ai.endpoint.degraded`）：`log.warn("端点性能退化: endpointId={}, state=DEGRADED", endpointId)`
- 在 LLM 调用成功/失败后补充 `endpointHealthManager.recordCallResult()` 调用，使端点健康管理器可追踪每次调用的结果并触发 DEGRADED→CONNECTED 的状态回升

---

### 6. RequestContextUtils.extractFromRequestContext() 存在 ClassCastException 风险

**描述**：§3.10 `extractFromRequestContext()` 实现直接进行 `(ServletRequestAttributes) RequestContextHolder.getRequestAttributes()` 的强转。当底座运行在非 Spring MVC 环境（如 WebFlux、纯 JAX-RS 或自定义 NIO 容器）下时，`RequestContextHolder.getRequestAttributes()` 返回的实现类型并非 `ServletRequestAttributes`，导致 `ClassCastException`。虽然 §3.10 通过非 HTTP 场景回退路径描述了兜底，但 `extractFromRequestContext()` 方法本身在混合环境中被调用时（如某些 Gateway Filter 在 WebFlux 环境中运行但部分过滤器使用 Servlet API）会直接抛出未捕获的异常。

**位置**：§3.10 RequestContextUtils 核心方法（第 2090-2094 行）

**严重程度**：重要

**改进建议**：使用 `instanceof` 安全检查替代直接强转：
```
RequestAttributes attrs = RequestContextHolder.getRequestAttributes()
if attrs instanceof ServletRequestAttributes:
    return ((ServletRequestAttributes) attrs).getRequest().getHeader(headerName)
return null
```
同步更新 §3.10 伪代码。此防护已在 §4.1 `AiOrchestrator.handle()` catch 块中通过 `instanceof` 分支部分实现，但 §3.10 工具方法本身仍需保护。

---

DIAG_WRITTEN:C:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606271627_phase5_pkgG_ood/b_v23_diag_v1.md
主Agent请勿阅读产出文件内容，直接将路径转发给相关方。
