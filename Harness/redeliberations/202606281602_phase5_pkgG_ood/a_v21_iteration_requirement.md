根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 1. [严重] @RefreshScope 标记 SlidingWindowMetricsStore 将丢失全部滑动窗口数据

§3.9「运行时配置热加载机制」表中将 `SlidingWindowMetricsStore` 标记为 `@RefreshScope` 以支持 `sliding-window.window-seconds` 的热加载。`@RefreshScope` 销毁并重建 Bean 实例，清空全部滑动窗口数据（13 项能力在 60 秒窗口内最多 13 万条事件），刷新后 `getFailureRate()`/`getEffectiveFailureRate()`/`getAverageElapsed()` 全部返回 0，熔断器因无历史数据保持 CLOSED 状态 60 秒。

**改进建议**：
- 方案 A（推荐）：移除 `@RefreshScope`，改为 `AtomicLong` + 定时刷新（与 `refreshCapabilityTimeoutConfig()` 风格一致）
- 方案 B（折衷）：声明为静态配置重启生效
- 方案 C（备选）：拆分配置持有层（`@RefreshScope`）和数据存储层（普通 `@Component`）

### 2. [严重] Phase4BusinessException 缺少过渡期兼容性——未迁移模块的业务异常将被误分类为基础设施异常

§4.2 薄适配器 catch 块使用 `instanceof Phase4BusinessException` 区分业务异常与基础设施异常，但 6 个 Phase 4 模块当前的异常类均未继承此类。过渡期内所有未迁移的业务异常将落入 `else` 分支被归类为 `INFRASTRUCTURE_ERROR`，触发错误降级。

**改进建议**：在 `instanceof Phase4BusinessException` 主分支前增加过渡期回退检查——复用 §3.1 已定义的"异常类名回退"机制，当 `Phase4BusinessException` 检测失败时 fallback 到字符串匹配。

### 3. [重要] LocalRuleFallback.fallback() 空返回值未加 null 保护

`doDegrade()` 方法（line 3287-3288）调用 `localRuleFallback.fallback(request)` 后直接调用 `.toString()`，result 为 null 时抛出 NPE，导致整个方法异常退出，端点健康和指标记录均无法更新。

**改进建议**：增加 null 守卫，在 result == null 时返回 `AiResult.degraded()` 并记录 ERROR 日志。同时在 `LocalRuleFallback` 接口 Javadoc 中明确约定非 null 返回。

### 4. [一般] @ConditionalOnClass 引用的 Spring AI ChatModel 包路径可能不准确

§3.2 使用 `@ConditionalOnClass(name = "org.springframework.ai.chat.ChatModel")`，但在 Spring AI 1.0.x 中实际包路径为 `org.springframework.ai.chat.model.ChatModel`（含 `model` 子包）。条件永不匹配时底座始终使用 `HttpApiLlmChatService`。

**改进建议**：同时兼容两种包路径——`chat.model.ChatModel` 和 `chat.ChatModel`，或在注释中标注准确版本号。

### 5. [一般] DiscussionConclusionCapabilityExecutor 前置压缩失败与主流程超时叠加时降级原因二义性

当 `transcriptSummaryTimeout + 主流程 LLM 调用耗时 > capabilityTimeout` 时，`orTimeout()` 触发 `TIMEOUT` 降级，但无法区分是压缩阶段占用了过多时间还是主 LLM 调用本身超时——两场景根因完全不同。

**改进建议**：在 `exceptionally()` 回调中增加超时原因细化判断：若压缩阶段消耗 >80% 超时窗口则标记 `":transcriptSummaryCrowding"`，否则标记 `":primaryLlmTimeout"`。

### 6. [重要] AiCallRecord 工厂方法 16 参数在实现阶段的高阶编码风险

`AiCallRecord.success()` 16 参数、`failure()` 15 参数、`degraded()` 16 参数、`AbstractCapabilityExecutor` 构造器 13+ 参数、`doDegrade()` 15 参数，参数顺序跨方法不一致（如 `doDegrade()` 与 `AiCallRecord.success()` 的 callerRole/callerId/visitId/patientId 顺序不同），高概率编码错误风险。

**改进建议**：在 Phase 5 实施期立即引入 `CallContext` 值对象，聚合 9~10 个业务上下文参数，工厂方法签名降维至约 7 参数。

## 历史迭代回顾

### 已解决的问题（出现在历史反馈但当前反馈中不再提及）

以下问题已在 v2~v19 迭代中逐步修复，v20 诊断未再检出：
- §1.2 中"不变"声明与实质性变更的矛盾（v2）
- 多实例部署场景的跨实例行为定义（v2）
- API Surface 状态表及新建接口优先级（v2）
- Jackson 兼容性验证场景（v2）
- `@Qualifier("{capabilityId}Strategies")` 注入不可行（v3）
- 降级策略注入机制两套并行描述（v3）
- 实验分流异常哨兵值及 `createErrorFallback()` 设计（v8/v12/v13）
- 修订说明混合与迭代标记残留（v4）
- DTO 字段定义与代码现实脱节（v5）
- 薄适配器构造器 `super()` 参数不匹配（v10）
- `doExecuteInternal()` 中 `catch (TimeoutException)` 死代码（v11）
- 降级路径系统性双重计数（v14/v16）
- `Phase4ServiceMetaProvider` 接口归属与依赖方向矛盾（v17/v18）
- 异常匹配机制 `instanceof` vs 字符串数组矛盾（v19）

### 持续存在的问题（在多轮反馈中反复出现，需重点解决）

- **16 参数工厂方法编码风险**（v13→v14→v20）：v13 首次提出 13~16 参数构造器/工厂方法的高阶编程风险，建议引入上下文对象聚合通用参数，v14 补充了重构方向说明，v20 诊断再次检出此问题未解决，建议在 Phase 5 实施期立即引入，不再推迟到 Phase 6。

### 新发现的问题（本轮新识别的问题）

- **@RefreshScope 清空滑动窗口数据**：本轮首次检出，SlidingWindowMetricsStore 数据完整性问题
- **Phase4BusinessException 过渡期兼容性**：本轮首次检出，异常分类过渡期缺口
- **LocalRuleFallback null 指针风险**：本轮首次检出，空返回值保护缺失
- **Spring AI @ConditionalOnClass 包路径**：本轮首次检出，类路径条件不准确
- **前置压缩超时降级原因二义性**：本轮首次检出，超时边界场景未定义

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281602_phase5_pkgG_ood\a_v20_copy_from_v19.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281602_phase5_pkgG_ood\requirement.md
