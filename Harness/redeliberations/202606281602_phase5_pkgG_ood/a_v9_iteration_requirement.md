根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 问题A：[重要] structuredChat 回退路径的超时叠加风险仅在分析层面描述，伪代码层面未实现对应保护
- **所在位置**：§3.2（"structuredChat() 回退路径超时叠加风险"段）、§4.1（行 2761–2845）
- **问题描述**：§3.2 详细分析了 structuredChat 回退到 chat()+parse() 时存在端到端超时叠加风险，并提出了"60% + 40%"的缓解措施。但 §4.1 的伪代码中完全没有实现这一内部超时拆分：structuredChat 调用是简单的 `.join()` 阻塞等待，chat 回退调用同样是 `.join()` 阻塞等待，两者共享同一个 `capabilityTimeout` 整体超时。实际效果：若 structuredChat 在超时前已消耗 50 秒，回退到 chat 时仅剩 10 秒可用，导致本来可成功的回退路径也被迫超时降级。
- **改进建议**：1. 在 `AbstractCapabilityExecutor` 或子类中为 structuredChat 调用引入独立的内部超时控制（如 `CompletableFuture.orTimeout(capabilityTimeout * 0.6)`），超时后直接回退到 chat() 路径；2. 在 `LlmChatService` 实现层配合：若 JSON mode 缓存已确认"不支持"，直接抛出 `StructuredOutputNotSupportedException`；3. 在 §4.1 伪代码中明确标注 structuredChat 和 chat 的独立超时阈值。

### 问题B：[重要] 6 项薄适配器能力在底座切流初期的"实际不可用"状态缺少集中预警
- **所在位置**：§3.1、§3.5、§9
- **问题描述**：文档多处分散描述了 Phase 4 DTO 为空类的现状和临时 fallback 方案，但缺少一个集中的"底座切流初期已知限制"预警。在跨包协作会议完成和 Phase 4 DTO 补齐之前，6 项薄适配器能力的 `doExecuteInternal()` 将跳过委托调用直接降级。此信息分散在 §3.1 和 §3.5，§9 迁移路径表中未标注薄适配器的不可用状态。
- **改进建议**：1. 在 §1 概述中新增"底座切流初期已知限制"小节，集中列表说明：7 项底座能力可用，6 项薄适配器能力不可用（依赖 Phase 4 DTO 补齐）；2. 在 §9 迁移路径表的每项薄适配器行中标注依赖状态（如"🟡 依赖 Phase 4 DTO 改造"）。

### 问题C：[重要] promptVersion 在实验分流后的降级路径中被传入 null（保留）
- **所在位置**：§4.1 完整管线伪代码（行 2790、2817、2827、2845）
- **问题描述**：多个降级调用点在实验分流完成后（`promptVersion` 已从 `assignment.getTargetPromptVersion()` 获取）仍传入 `null` 作为 `promptVersion` 参数：行 2790（structuredChat AiResult 非成功路径）、行 2817（chat 路径的 ParseFailure 降级）、行 2827（chat AiResult 非成功路径）、行 2845（LlmInfrastructureException 降级路径）。这些降级场景下 `promptVersion` 已经可用，导致降级场景的 `AiCallRecord` 丢失 A/B 实验分流信息。
- **改进建议**：4 个降级调用点均传入 `promptVersion` 而非 `null`。

### 问题D：[中等] AiOrchestrator.handle() 异常场景丢失 callerRole/callerId 指标
- **所在位置**：§4.1（行 2631–2633）
- **问题描述**：`AiOrchestrator.handle()` 的 catch 块在记录 `AiCallRecord.failure()` 时传入 `null, null, null` 作为 `callerRole, callerId, promptVersion`。但此 catch 块在 Tomcat 容器线程上执行，`SecurityContextHolder` 可用。当前设计选择不提取的原因未在文档中说明。
- **改进建议**：在 catch 块中补充 `SecurityContextHolder` 的 null-safe 提取逻辑，与 `AbstractCapabilityExecutor.execute()` 入口处的提取方式一致；或添加注释说明为何不提取。

### 问题E：[中等] 降级路径中 modelId 未传入 AiCallRecord
- **所在位置**：§4.1 `doDegrade()` 方法（行 2868–2895）
- **问题描述**：`doDegrade()` 方法在构建 `AiCallRecord` 时，`degraded` 路径和纯降级路径均传入 `null` 作为 `modelId` 参数。但降级前的 LLM 调用已经完成了模型路由，`modelId` 实际已经确定，降级场景丢失模型标识信息，影响按模型维度的降级率分析。注意：问题标题应使用 modelId 而非 retryCount。
- **改进建议**：将 `modelId` 作为 `doDegrade()` 的可选参数传入，或调整 `AiCallRecord.degraded()` 工厂方法的参数顺序使 `modelId` 在降级路径中也可填充。

## 历史迭代回顾

### 已解决的问题（出现在历史反馈但当前反馈中不再提及的问题）
以下多个轮次的问题已修复并稳定：
- **第7轮**：§2.2引用不存在的§1.4、LlmChatRequest类图缺失tools字段、薄适配器per-capability超时覆盖伪代码未实现、降级策略解析逻辑与文本描述矛盾
- **第6轮**：薄适配器超时配置自相矛盾、类图与正文契约字段不一致、YAML配置缺少transcript-summary超时、状态恢复路径验证缺少CredentialProvider、薄适配器非HTTP场景DTO提取路径不可执行
- **第5轮**：§3.5DTO字段描述与实际不符、TriageResponse字段与代码不一致、底座能力DTO设计字段与代码脱节、薄适配器DTO方法签名与Phase 4 DTO为空矛盾、string类型数据库兼容性、决策表矛盾、文件路径不一致、兼容性验证、共同约束
- **第4轮**：修订说明混合正文、迭代标记散布、§3.11编号不连续、缺少实施拓扑、风格一致性引用、异常传播说明、线程安全契约
- **第3轮**：@Qualifier不可解析、§3.11覆盖范围、降级策略注入机制并行、条件化注册不一致、薄适配器doExtractDepartmentId()、ExperimentGroup未定义、AiCallLogStats未定义、PrescriptionLocalRuleFallback未定义、ModelRoute参数扩展点、前置LLM超时策略、extractCallerRole()规则
- **第2轮**："不变"声明矛盾、多实例行为未定义、API Surface状态表、Jackson兼容性测试、structuredChat工具定义传递、FallbackAiService条件开关

### 持续存在的问题（在多轮反馈中反复出现的问题，需重点解决）
- **问题A（structuredChat超时叠加）**：第2轮已首次提出structuredChat相关问题；第8轮聚焦为超时叠加风险未被伪代码实现；本轮仍然存在。这是跨多轮未彻底解决的设计实现深度问题。
- **问题B（薄适配器不可用集中预警）**：第5轮已指出薄适配器DTO与Phase 4的依赖问题；第8轮要求集中"已知限制"预警；本轮仍然需要补充。
- **问题C（promptVersion降级null）**：第1轮（问题6）已报告此问题，第8轮保留为问题C，本轮仍然存在。
- **问题D（AiOrchestrator异常指标丢失）**：第8轮首次报告，本轮未解决。
- **问题E（降级路径modelId丢失）**：第8轮首次报告，本轮未解决。

### 新发现的问题（本轮新识别的问题）
本轮诊断报告无全新问题。质询报告（LOCATED确认）提出了两点元反馈供设计时参考：
1. 问题E标题应使用"modelId"而非"retryCount"——修复问题E时注意标题准确性
2. 建议在后续诊断中增加"事实正确性检查"维度（非本轮A产出直接修复项）

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281602_phase5_pkgG_ood\a_v8_copy_from_v7.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281602_phase5_pkgG_ood\requirement.md
