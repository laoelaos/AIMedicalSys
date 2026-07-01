根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

1. **【严重】§5.1 错误分类表的异常容错承诺在 §4.1 伪代码中未兑现**：§5.1 声明模板缺失/渲染失败应"日志WARN + 使用能力内硬编码兜底Prompt"，实验分流异常应"日志WARN + 降级到 default 分组"，但 §4.1 `doExecuteInternal()` 伪代码中 `promptTemplateManager.render()` 和 `experimentManager.assign()` 均未包裹 try-catch，异常将穿透 `supplyAsync` lambda 边界被 `AiOrchestrator.handle()` 统一拦截并返回 `AiResult.failure("AI服务暂时不可用")`，与 §5.1 承诺矛盾。改进建议：在 `doExecuteInternal()` 中为两处调用分别添加 try-catch，按 §5.1 承诺的降级行为处理。

2. **【严重】CapabilityExecutor 执行管线缺少整体兜底超时机制**：§4.1 `doExecuteInternal()` 中仅 `LlmClient.invoke()` 有超时控制，但 `experimentManager.assign()`（JPA 查询）、`promptTemplateManager.render()`（数据库+渲染）、`extractVariables()`（Jackson 转换）等均无超时保护，若挂起将无限占用线程池线程且 `CallerRunsPolicy` 可能导致 Tomcat 容器线程阻塞。改进建议：通过 `CompletableFuture.orTimeout()` 为整体管线引入可配置端到端超时，超时后进入降级路径记录 `DegradationReason.TIMEOUT`，各能力独立配置阈值（默认60秒）。

3. **【重要】AiOrchestrator.handle() 异常记录丢失关键就诊上下文**：§4.1 `AiOrchestrator.handle()` catch 块调用 `AiCallRecord.failure()` 时对 `departmentId`、`visitId`、`patientId`、`sessionId`、`callerRole`、`callerId`、`inputSummary` 均传入 null，导致异常场景下日志丢失当前就诊上下文，运维无法追踪受影响患者/就诊/科室。改进建议：从 `request` 对象提取可用上下文（`instanceof AiRequestBase` 检查后提取，否则使用 `toString()` 截取作为 `inputSummary`），更新 `failure()` 调用参数。

4. **【重要】薄适配器型的 CapabilityExecutor 对 Phase 4 服务的端到端耗时不可控**：§3.1 薄适配器 `phase4ServiceDelegate.execute(request)` 委托 Phase 4 服务，底座无法控制其内部超时和重试策略（如 3 次重试每次30秒可达90秒），而底座无此维度超时保护。改进建议：为薄适配器委托调用引入独立超时控制（`CompletableFuture.orTimeout(thinAdapterTimeout)`），超时后走降级路径，`thinAdapterTimeout` 在 YAML 中配置（默认30秒），或在文档中显式声明约束。

5. **【中等】降级预检在线程池排队之后执行，高并发下降级响应延迟**：§4.1 `AbstractCapabilityExecutor.execute()` 中降级预检放置在 `supplyAsync()` lambda 内部执行，熔断器 OPEN 状态下的请求仍需排队等待线程池线程执行预检后才能被降级，导致本应在容器线程即返回降级结果的请求经历队列等待。改进建议：将降级预检移至 `supplyAsync()` 之前、容器线程执行；若任一策略判定降级直接返回 `CompletableFuture.completedFuture(doDegrade(...))`，不入线程池排队；仅正常请求入池。

## 历史迭代回顾

### 已解决的问题
- 类图完整性、方法签名、状态模型定义（v1~v2 反复修复）
- 能力覆盖不全问题（v3 补充6项薄适配器实现）
- Bean 装配二义性和 `@Qualifier` 命名规则（v3~v6 逐步冻结）
- 降级路径指标记录与熔断器统计失准（v4~v6 修复）
- `structuredOutputParser.parse()` 缺少 try-catch（v7 修复）
- 异步线程上下文传播（v8 修复——提取前移至supplyAsync之前）
- `doDegrade()` 签名缺少 departmentId（v8 修复）
- Phase 4 服务依赖机制（v9 修复——构造器注入+依赖说明）
- 防御性拷贝合约（v9 修复——supplyAsync前深拷贝）
- TokenUsage 类建模（v9 修复）
- Experiment 无实验命中返回值冻结（v9 修复）

### 持续存在的问题
- **异常容错与承诺一致性**：v5 Q7（薄适配器缺少异常处理）→ v7 Q1（parse()缺少try-catch）→ 本轮 Q1（experiment/template缺少try-catch）。反复出现管线步骤未按错误分类表承诺处理异常的模式，表明缺乏系统性的异常容错自查机制。
- **异常场景的上下文记录完整性**：v8 Q3（doDegrade缺departmentId）→ v8 Q4（工厂方法签名不完整）→ 本轮 Q3（catch块丢失上下文）。异常/降级路径下的日志记录参数完整性反复遗漏，表明 try-catch 块的参数填充缺乏统一检查清单。
- **线程池与降级时序**：v6 Q5（降级路径recordSuccess）→ v8 Q1（异步上下文传播）→ v8 Q2（拒绝策略未定义）→ 本轮 Q5（预检在排队后）。降级判定与线程池交互的时序问题反复出现，涉及 supplyAsync 边界放置的内容。

### 新发现的问题
- Q2（整体管线兜底超时）——首次系统性提出非LLM步骤的超时治理
- Q4（薄适配器委托超时控制）——首次明确 thin adapter 场景的超时责任划分
- Q1（错误分类表与伪代码承诺一致性）——首次从 §5.1 vs §4.1 交叉验证角度发现矛盾
- Q5（降级预检位置优化）——首次提出 pre-supplyAsync 降级预检模式

## 上一轮产出路径
C:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606271627_phase5_pkgG_ood/a_v10_copy_from_v9.md

## 用户需求
C:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606271627_phase5_pkgG_ood/requirement.md
