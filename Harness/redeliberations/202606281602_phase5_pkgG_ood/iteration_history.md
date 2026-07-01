
## 迭代第 2 轮

1. **问题描述**："不变"声明与实质性变更之间存在系统性矛盾，3 处明确声明 `ai-api` 保持不变但包含 4 项实质性变更（`DegradationStrategy.getOrder()` default method、`DegradationContext` 新增字段、`AiRequestBase` 新抽象类、`DegradationReason` 新枚举）
   - 所在位置：§1.2、§2.1、§2.2（声明处）；§3.5、§3.8（变更处）
   - 严重程度：严重
   - 改进建议：删除绝对化"不变"表述，替换为精确约束声明；新增"ai-api 变更范围总结表"

2. **问题描述**：多实例部署场景下 `SlidingWindowMetricsStore`、`EndpointRateLimiter`、`CircuitBreakerDegradationStrategy` 三个组件的跨实例行为未定义，限流/熔断行为与预期不一致
   - 所在位置：§6.1、§3.5、§3.2、§3.8、§10.4
   - 严重程度：严重
   - 改进建议：新增"多实例行为约束"子章节逐组件分析；补充部署形态章节作为约束依据

3. **问题描述**：伪代码引用约 10 个关键接口调用，未标注哪些已有实现、哪些需新建，实现者需对照代码库确认实施起点
   - 所在位置：§4.1、§4.2
   - 严重程度：重要
   - 改进建议：新增"API Surface 状态表"标注三种状态；补充"新建接口优先级"排序

4. **问题描述**：§3.5 提出的 Jackson 兼容性测试断言无法验证真实兼容性风险，旧 JSON 不包含基类字段则字段当然为 null
   - 所在位置：§3.5
   - 严重程度：重要
   - 改进建议：替换为 3 个真实兼容性验证场景；补充 `@JsonCreator` + `@ConstructorProperties` 继承兼容性说明

5. **问题描述**：`LlmChatService.structuredChat()` 未定义 tool_use/function_call 的 Tool 定义输入传递方式、JSON mode 检测时机、失败回退超时叠加风险
   - 所在位置：§3.2、§4.1
   - 严重程度：重要
   - 改进建议：补充 `tools` 字段定义；明确运行期首次检测+缓存策略；补充超时风险评估

6. **问题描述**：`FallbackAiService.applyStrategies()` 条件开关 `aiPlatformEnabled` 缺少 YAML 配置绑定定义，不同注入方式对测试和部署影响不同
   - 所在位置：§9.2、§9.5
   - 严重程度：重要
    - 改进建议：推荐 `@Value` 构造器注入；标注 `ai.platform.enabled` 与 `ai.mock.enabled` 同时激活的风险

## 迭代第 3 轮

1. **问题描述**：`AbstractCapabilityExecutor` 构造器伪代码中的 `@Qualifier("{capabilityId}Strategies")` 无法在 Spring 容器中正确解析，属编译期不可行的事实错误
   - 所在位置：§3.1（第 834 行、第 1146-1162 行）、§3.8（第 2051-2060 行）
   - 严重程度：严重
   - 改进建议：明确选择一套降级策略注入机制。推荐保留 `AiPlatformConfig` 中的 Map 构建路径，删除或修正构造器中的 `@Qualifier` 伪代码，改为注入 `Map<String, List<DegradationStrategy>>` 后在 `execute()` 中按 `getCapabilityId()` 查找

2. **问题描述**：§3.11 覆盖范围与需求不对称，3 项迁移能力（Triage、PrescriptionCheck、MedicalRecordGen）缺少同等级别的 9 维度特化设计表
   - 所在位置：§3.11（第 3219-3281 行）
   - 严重程度：一般
   - 改进建议：将 §3.11 标题改为"Phase 5 底座能力特化设计"并覆盖全部 7 项底座能力，或新增 §3.12 覆盖 3 项迁移能力的特化设计表

3. **问题描述**：降级策略注入机制存在两套并行且互斥的描述（`@Qualifier` 按能力注入 vs `AiPlatformConfig` 构建 Map）
   - 所在位置：§3.1（第 831-852 行）、§3.8（第 2048-2060 行）、`AbstractCapabilityExecutor` 构造器伪代码（第 1141-1162 行）
   - 严重程度：一般
   - 改进建议：统一为一套机制，推荐 Map 方案，删除所有 `@Qualifier` 按能力注入的表述

4. **问题描述**：底座能力条件化注册机制描述与实际设计不一致——声称"在 AiPlatformConfig 中统一注册条件"但 §3.9 无对应统一注册逻辑
   - 所在位置：§3.1（第 899 行）、§3.9（第 2080-2201 行）
   - 严重程度：一般
   - 改进建议：明确 CapabilityExecutor 的注册策略（`@Component` 自注册 vs `@Bean` 统一注册），修正不一致的表述

5. **问题描述**：薄适配器 `doExtractDepartmentId()` 的非 HTTP 回退描述引用 Prompt 模板回退，但薄适配器不使用 Prompt 模板
   - 所在位置：§3.1（第 903-904 行的 `doExtractDepartmentId()` 说明）
   - 严重程度：一般
   - 改进建议：将"Prompt 模板回退到通用模板"改为"departmentId 保持 null，对应字段在 AiCallRecord 中可空"

6. **问题描述**：§3.11 节标题称"首次落地"但包含迁移能力 PrescriptionAssist，同时 3 项真正的迁移能力未被覆盖
   - 所在位置：§3.11 标题（第 3219 行）
   - 严重程度：一般
   - 改进建议：标题改为"Phase 5 底座能力特化设计"并扩展至 7 项，或保持 4 项并修正标题

7. **问题描述**：`ExperimentGroup` 实体未被正式定义，实现者不知字段结构、JPA 映射或业务约束
   - 所在位置：§3.4 第 1670-1680 行、§4.4 第 2582-2590 行
   - 严重程度：一般
   - 改进建议：在 §3.4 或新增子章节中补充 `ExperimentGroup` 的完整定义（字段表、JPA Entity 注解、关联关系、流量分配算法约束）

8. **问题描述**：`AiCallLogStats` 汇总统计表未被正式定义——无字段结构、无索引策略、无 Entity 类引用
   - 所在位置：§3.5（第 1874 行）
   - 严重程度：一般
   - 改进建议：在 §3.5 的"数据保留与清理策略"子节中补充 `AiCallLogStats` 的完整字段定义、索引策略及写入/查询伪代码

9. **问题描述**：`PrescriptionLocalRuleFallback` 未定义"最小安全规则"的具体内容
   - 所在位置：§3.7（第 1949-1955 行）
   - 严重程度：一般
   - 改进建议：补充 `PrescriptionLocalRuleFallback` 的具体行为契约（至少 3~5 项最小检查规则）、规则来源及执行失败时安全策略

10. **问题描述**：`ModelRoute.parameters` 未定义扩展点，`topP`、`frequencyPenalty`、`presencePenalty`、`seed` 等常见 LLM 参数被静默忽略
   - 所在位置：§3.2（第 1401-1417 行）、§9.5 YAML 示例
   - 严重程度：一般
   - 改进建议：在 `LlmChatOptions` 中补充 `topP`、`frequencyPenalty`、`presencePenalty` 字段，或在 §3.2 新增参数映射扩展点规则说明

11. **问题描述**：`DiscussionConclusionCapabilityExecutor` 的 `transcriptSummary` 前置 LLM 调用未定义超时和失败策略
   - 所在位置：§3.11.4（第 3271 行）、§3.1 的 `extractVariables()` 定义（第 1065-1068 行）
   - 严重程度：一般
   - 改进建议：明确预处理 LLM 调用的契约——使用的方法、独立的超时管理、调用失败时的降级行为

12. **问题描述**：`extractCallerRole()` 未定义 Spring Security `GrantAuthority` 的过滤规则（ROLE_ 前缀处理、多 authority 场景）
   - 所在位置：§3.1（第 1098-1109 行）
   - 严重程度：一般
    - 改进建议：补充显式规则：取第一个以 `"ROLE_"` 开头的 `GrantedAuthority` 并去除前缀；若无则取第一个原始值

## 迭代第 4 轮

1. **问题描述**：修订说明与设计正文混合，产出不是一份可交付的 OOD 设计文档
   - 所在位置：§修订说明（v2）至 §修订说明（v4 — 本轮修订），约行 3443–3833 及所有历史迭代的修订说明段
   - 严重程度：严重
   - 改进建议：将全部修订说明剥离为独立归档文件，产出正文只保留最终设计状态

2. **问题描述**：文档内部散布迭代标记，残留大量过程性注记
   - 所在位置：散布于 §3.1–§3.11 全文
   - 严重程度：重要
   - 改进建议：全文搜索并清除所有 `(v{N} 新增/修正/补充/修订)` 类标记

3. **问题描述**：§3.11 节编号不连续且章节位置异常
   - 所在位置：§3.11.4 编号空缺，§3.11 位于 §11 之后而非 §3.10 之后
   - 严重程度：重要
   - 改进建议：将 §3.11 移至 §3.10 之后，重新编号为连续序列

4. **问题描述**：缺少实施者端的"首次编码引导"信息
   - 所在位置：§1.6.2 状态表及全文缺少编码实施引导
   - 严重程度：重要
   - 改进建议：新增"实施拓扑顺序"小节，给出拓扑排序后的建议实施批次

5. **问题描述**：需求响应中"风格一致性"参照缺乏具体对照
   - 所在位置：§1.2（行 40–41）
   - 严重程度：中等
   - 改进建议：列出 3–5 条具体的风格一致性规则并标注来源

6. **问题描述**：doExecuteInternal() 伪代码中 structuredChat/chat 返回值的两层解包缺乏异常传播说明
   - 所在位置：§4.1（行 2548–2582）
   - 严重程度：中等
   - 改进建议：补充 CompletionException 捕获及 AiResult 状态检查

7. **问题描述**：未定义 AiService 接口自身的线程安全契约
   - 所在位置：§1.3 核心抽象表、§3.1、§6.1
   - 严重程度：中等
   - 改进建议：在接口职责描述中补充线程安全契约

## 迭代第 5 轮

1. **问题描述**：§3.5 声称所有DTO已定义 visitId/patientId/sessionId 字段，实际13个请求DTO中无一存在这些字段
   - 所在位置：§3.5「AiRequestBase」节，行1862–1872
   - 严重程度：严重
   - 改进建议：修正§3.5中对现有DTO状态的描述，明确反映"当前DTO均无visitId/patientId/sessionId/departmentId字段，需逐DTO新增4个公共字段并调整构造器/Jackson注解"

2. **问题描述**：§3.11.1定义的TriageResponse输出字段（recommendedDepartment: String, urgencyLevel）与现有代码（recommendedDepartments: List, 无urgencyLevel）不一致
   - 所在位置：§3.11.1「TriageCapabilityExecutor」特化设计表，行2392–2398
   - 严重程度：严重
   - 改进建议：修正§3.11.1中TriageResponse字段定义与现有代码对齐，或明确标注"改造后字段"并评估对下游消费者的兼容性影响

3. **问题描述**：7项底座能力的DTO设计字段与代码现实脱节，未标注"新增 vs 已有"
   - 所在位置：§3.11.1–§3.11.7各能力特化设计表；§3.5「AiRequestBase」节
   - 严重程度：重要
   - 改进建议：在§3.5过渡策略中补充"DTO业务字段补齐计划"章节，明确7项底座能力DTO的业务字段属于全新设计

4. **问题描述**：§3.1薄适配器伪代码中phase4ServiceDelegate的方法签名与Phase 4 DTO为空的事实矛盾
   - 所在位置：§3.1「薄适配器型CapabilityExecutor」节（行879–1018）；§3.11未覆盖薄适配器DTO定义
   - 严重程度：重要
   - 改进建议：补充Phase 4服务接口的契约定义，或明确标注Phase 4 DTO和Service接口的定义职责归属

5. **问题描述**：`string`类型用于数据库VARCHAR缺乏跨数据库兼容性说明
   - 所在位置：§3.5「AiCallLogEntity」字段表（行1890–1914）及所有JPA Entity字段定义
   - 严重程度：中等
   - 改进建议：增加说明："数据库类型声明以MySQL方言表述，实际实现者需根据项目所选数据库替换为对应方言类型"

6. **问题描述**：§7决策表"异步队列溢出"行描述与§3.5/§6.1指标采集拒绝策略矛盾
   - 所在位置：§7决策表第3045行 vs §3.5行1782 vs §6.1行2965
   - 严重程度：中等
   - 改进建议：在§7决策表对应行补充"[LLM调用线程池]"前缀，或在决策表中增加"适用组件"列

7. **问题描述**：LabTestReportRequest.java和LabTestReportResponse.java文件路径不一致
   - 所在位置：§3.11未覆盖6项薄适配器DTO；§2.1目录结构
   - 严重程度：中等
   - 改进建议：为6项薄适配器能力补充"DTO现状"说明条，标注每个DTO的当前状态和责任归属

8. **问题描述**：DegradationContext扩展后的二进制兼容性验证未覆盖"零值构造器"的Jackson反序列化场景
   - 所在位置：§3.8 binary兼容性段落（行2177–2184）；§4.1执行伪代码
   - 严重程度：中等
   - 改进建议：补充Jackson反序列化场景下的防御措施，如使用包装类型Integer而非int

9. **问题描述**：§3.11共同约束未明确定义7项底座能力DTO的业务字段是否包含在AiRequestBase改造范围内
   - 所在位置：§3.11共同约束段落（行2483–2487）
   - 严重程度：中等
   - 改进建议：在共同约束中新增一条，明确DTO扩展字段均为Phase 5底座新增设计，实现者需同时新增业务字段

## 迭代第 6 轮

1. **问题描述**：薄适配器超时配置自相矛盾——YAML示例违反文档自身定义的层级约束
   - 所在位置：§3.1（行1171-1173）vs §9.5 YAML配置（行3350-3355, 3360-3364）
   - 严重程度：严重
   - 改进建议：修正IMAGE_ANALYSIS和DIAGNOSIS的per-capability值，确保满足"per-capability >= thinAdapterTimeout + 5s"约束；新增启动期配置校验注解

2. **问题描述**：类图与正文契约字段不一致——实现者仅参类图将遗漏4个业务字段
   - 所在位置：§2.3类图（行428-433, 379-387）vs §3.2（行1442-1451, 1512-1520）
   - 严重程度：严重
   - 改进建议：在LlmChatOptions类图中补充topP、frequencyPenalty、presencePenalty三个字段；在ModelRoute类图中补充authType字段及关联线

3. **问题描述**：§9.5 YAML配置示例缺少transcript-summary超时配置项——下游运维消费者无法发现此可调参数
   - 所在位置：§3.11.7（行2492）vs §9.5 YAML配置（行3334-3365 execution.timeout块）
   - 严重程度：严重
   - 改进建议：在§9.5 YAML的execution.timeout块中补充transcript-summary: 15s配置项并添加注释说明用途

4. **问题描述**：状态恢复路径验证缺少CredentialProvider——凭据获取的可靠性验证被遗漏
   - 所在位置：§11.4状态恢复路径验证表（行3496-3503）vs §3.2 CredentialProvider状态机（行1586-1638）
   - 严重程度：一般
   - 改进建议：在§11.4新增CredentialProvider状态恢复路径验证行，覆盖CACHE_ONLY→NORMAL和BACKOFF→NORMAL两条恢复路径

5. **问题描述**：薄适配器非HTTP场景下DTO字段提取路径不可执行——跨包依赖解决方案无时间线
   - 所在位置：§3.1 Phase 4服务接口契约定义（行879-882）及薄适配器伪代码（行941-949, 951-963）
   - 严重程度：一般
    - 改进建议：定义Phase 5实现期临时fallback方案（如底座侧封装DTO包装接口）；明确跨包协作会议截止时间；定义DTO改造最小验收标准

## 迭代第 7 轮

1. **问题描述**：§2.2 引用不存在的 §1.4 章节，版本清理遗留的事实性错误
   - 所在位置：§2.2，line 277
   - 严重程度：严重
   - 改进建议：在 §1.3 之后恢复 §1.4「ai-api 变更范围总结表」，或将 line 277 引用改为指向 §1.6

2. **问题描述**：LlmChatRequest 类图缺失 `tools` 字段，图-文不一致
   - 所在位置：§2.3 类图（line 421–425）；§3.2 文本（line 1443）；§4.1 伪代码（line 2718）
   - 严重程度：一般
   - 改进建议：在 §2.3 类图的 `LlmChatRequest` 中补充 `+List<ChatToolDefinition> tools` 字段及关联线

3. **问题描述**：薄适配器 per-capability 超时覆盖机制伪代码未实现，设计缺口
   - 所在位置：§3.1 AbstractCapabilityExecutor 构造器（line 1219–1236）；§3.1 文本（line 1204–1205）；§4.2（line 2884）；§9.5（line 3399–3406）
   - 严重程度：一般
   - 改进建议：在构造器中新增 per-capability 配置注入，在 `doExecuteInternal()` 中增加按 `capabilityId` 解析覆盖值逻辑

4. **问题描述**：降级策略解析逻辑与文本描述矛盾，`this.degradationStrategies` 字段未定义
   - 所在位置：§3.1 execute()（line 1093）；§3.1 文本（line 863–880）；构造器（line 1228, 1233）
   - 严重程度：一般
   - 改进建议：方案A：将 `this.degradationStrategies` 替换为 `this.degradationStrategyMap.getOrDefault(capabilityId, ...)`；方案B：在构造器或 @PostConstruct 中增加从 Map 解析的步骤

## 迭代第 8 轮

1. **问题描述**：structuredChat 回退路径的超时叠加风险在分析层面已描述，但伪代码（§4.1）未实现 60%/40% 内部超时拆分，导致回退路径可能因剩余时间不足而失败
   - 所在位置：§3.2、§4.1（行 2761–2845）
   - 严重程度：严重
   - 改进建议：在 AbstractCapabilityExecutor 中为 structuredChat 引入独立内部超时（CompletableFuture.orTimeout(capabilityTimeout * 0.6)），超时后直接回退 chat() 路径；在 §4.1 伪代码中标注独立超时阈值

2. **问题描述**：6 项薄适配器能力在底座切流初期的"实际不可用"状态缺少集中预警，信息分散在 §3.1 和 §3.5，§9 迁移路径表未标注依赖状态
   - 所在位置：§3.1、§3.5、§9
   - 严重程度：严重
   - 改进建议：在 §1 新增"底座切流初期已知限制"集中列表；在 §9 迁移路径表每项薄适配器行标注"🟡 依赖 Phase 4 DTO 改造"

3. **问题描述**：promptVersion 在实验分流后的 4 个降级调用点（行 2790、2817、2827、2845）仍传入 null，导致 A/B 实验分流数据丢失
   - 所在位置：§4.1（行 2790、2817、2827、2845）
   - 严重程度：严重
   - 改进建议：4 个降级调用点均传入 promptVersion 而非 null

4. **问题描述**：AiOrchestrator.handle() 的 catch 块（行 2631–2633）传入 null 作为 callerRole/callerId，但 SecurityContextHolder 在此上下文中可用，当前设计选择未说明依据
   - 所在位置：§4.1（行 2631–2633）
   - 严重程度：一般
   - 改进建议：在 catch 块补充 SecurityContextHolder 的 null-safe 提取逻辑，或添加注释说明为何不提取

5. **问题描述**：doDegrade() 方法（行 2868–2895）在构建 AiCallRecord 时传入 null 作为 modelId，但降级前 LLM 调用已完成模型路由，modelId 已确定，导致模型维度降级率分析数据缺失。质询指出问题 E 标题误写为 retryCount 而非 modelId，需同步修正
    - 所在位置：§4.1（行 2868–2895）
    - 严重程度：一般
    - 改进建议：将 modelId 作为 doDegrade() 的可选参数传入，同时将问题 E 标题统一修正为 modelId

## 迭代第 9 轮

1. **问题描述**：§3.7 PrescriptionLocalRuleFallback 引用 PrescriptionCheckRequest 上不存在的字段 allergyInfo，应为 request.patientInfo.getAllergyInfo() 或补充字段定义
    - 所在位置：§3.7 最小安全规则表第 4 行（line 2160）
    - 严重程度：严重
    - 改进建议：方案 A：数据来源改为 request.patientInfo 并注明方法签名；方案 B：在 §3.11.2 补充 allergyInfo 字段定义

2. **问题描述**：§4.1 doExecuteInternal() 伪代码中 parsedResult 变量在 try 块内定义、try 块外引用，真实 Java 代码会编译错误
    - 所在位置：§4.1 伪代码（line 2809~2810、line 2863、line 2906~2916）
    - 严重程度：严重
    - 改进建议：在 try 块前声明 Object parsedResult = null，两个成功路径仅赋值，访问前添加 null 守卫检查

3. **问题描述**：§3.5 聚合 SQL 使用 MySQL 不支持的 PERCENTILE_CONT，与文档声明的 MySQL 方言矛盾
    - 所在位置：§3.5 聚合 SQL（line 2060~2062）
    - 严重程度：一般
    - 改进建议：替换为 MySQL 兼容的百分位计算方式（如 ROW_NUMBER() + COUNT(*) 或 PERCENT_RANK()），或添加注释说明各数据库方言替代函数

4. **问题描述**：多个 @Scheduled 任务（指标清理、CredentialProvider 退避、ModelRouter 轮询）缺少调度线程池配置定义，Spring 默认单线程可能因 DDL 元数据锁阻塞其他任务
    - 所在位置：§3.2、§3.5、§6.1、§3.9
    - 严重程度：一般
    - 改进建议：在 §3.9 AiPlatformConfig 中补充 @Bean("scheduledTaskExecutor") 配置 ThreadPoolTaskScheduler pool size ≥ 2~3

## 迭代第 10 轮

1. **问题描述**：§2.3 类图中 `doDegrade` 方法签名缺少 `modelId` 参数，与 §4.1 伪代码不一致
    - 所在位置：§2.3 类图，line 353
    - 严重程度：一般
    - 改进建议：在 `AbstractCapabilityExecutor` 类图的 `doDegrade` 方法签名末尾追加 `modelId: String` 参数

2. **问题描述**：§3.1 薄适配器构造器中 `super()` 调用参数数量与 `AbstractCapabilityExecutor` 构造器签名不匹配，无法编译
    - 所在位置：§3.1 line 964-967
    - 严重程度：严重
    - 改进建议：在 `super()` 调用中补全缺失的三个参数，同时同步修正所有 6 个薄适配器子类的构造器示例

3. **问题描述**：§4.2 薄适配器 catch 块引用未定义/未确认的 `BusinessException` 异常类型
    - 所在位置：§4.2 line 3025 catch 块，§3.1 line 925-933 异常契约表
    - 严重程度：严重
    - 改进建议：方案 A：确认 Phase 4 模块是否存在公共 `BusinessException` 基类；方案 B：将 catch 类型改为 `catch (Exception e)` 后通过 `instanceof` 区分

## 迭代第 11 轮

1. **问题描述**：`AiOrchestrator.handle()` catch块中`callerRole`提取逻辑与`AbstractCapabilityExecutor.extractCallerRole()`不一致，导致`AiCallRecord.callerRole`字段值格式不统一
   - 所在位置：§4.1（行2687-2689）vs §3.1 `extractCallerRole()`定义（行1202-1225）
   - 严重程度：严重
   - 改进建议：将`extractCallerRole()`抽取为`RequestContextUtils`中的公用静态方法

2. **问题描述**：`PatientInfo`类型未定义且`PrescriptionLocalRuleFallback`引用了不存在的字段路径
   - 所在位置：§3.11.2（行2548）DTO扩展字段表；§3.7（行2180-2181）最小安全规则表
   - 严重程度：严重
   - 改进建议：补充`PatientInfo`类的完整字段表，修正§3.7中`request.patientAge`和`request.pregnancyStatus`的引用路径

3. **问题描述**：`doExecuteInternal()`中`catch (TimeoutException)`因`.join()`包装成为死代码，超时被错误归类为基础设施异常
   - 所在位置：§4.1 `doExecuteInternal()`，行2864和行2915
   - 严重程度：严重
   - 改进建议：将`.join()`替换为`.get()`并在`catch (ExecutionException)`中拆解原始异常，或改用`Future.get(timeout, unit)`

4. **问题描述**：`ExperimentGroup`类图节点缺失（图-文不一致）
   - 所在位置：§2.3 类图
   - 严重程度：一般
   - 改进建议：在§2.3类图中新增`ExperimentGroup`类节点及与`Experiment`的关联线

5. **问题描述**：`AiCallLogStats`类图节点缺失（图-文不一致）
   - 所在位置：§2.3 类图
   - 严重程度：一般
   - 改进建议：在§2.3类图中新增`AiCallLogStats`类节点

6. **问题描述**：`StructuredOutputParser.parse()`独立超时未体现于§4.1伪代码
   - 所在位置：§3.2（文本描述）vs §4.1（伪代码行2901）
   - 严重程度：一般
   - 改进建议：在§4.1行2901处补充超时包裹逻辑

7. **问题描述**：`PrescriptionAssistRequest`与`PrescriptionCheckRequest`的患者数据建模方式不一致
   - 所在位置：§3.11.2（行2548）vs §3.11.4（行2576）
   - 严重程度：一般
   - 改进建议：统一患者数据建模方式，两个DTO均使用`PatientInfo`内嵌对象或明确说明差异是有意为之

8. **问题描述**：`AbstractCapabilityExecutor`构造器中`ObjectMapper`的来源未定义
   - 所在位置：§3.1构造器（行1266-1284）和`execute()`伪代码（行2714）
   - 严重程度：一般
   - 改进建议：在构造器中新增`ObjectMapper`参数，或在注释中说明获取方式

## 迭代第 12 轮

1. **问题描述**：薄适配器能力的 AiCallRecord 中 modelId、promptVersion、retryCount 三个字段永远为空，导致近半数 AI 能力的性能分析无法跨模型聚合
   - 所在位置：§4.2 薄适配器成功路径（行 3116-3120），AiCallRecord 调用处
   - 严重程度：重要
   - 改进建议：为 Phase 4 服务接口新增可选元数据暴露方法；在 AiCallRecord 字段定义表补充薄适配器场景说明；在 §1.4 集中说明分析维度缺口

2. **问题描述**：实验分流异常与无实验命中两种场景的 assignment 返回值语义重叠，下游无法区分正常无实验与异常降级，污染实验效果分析基线
   - 所在位置：§3.4 ExperimentManager.assign() 返回值约定；§4.1 行 2800-2804 catch 块
   - 严重程度：重要
   - 改进建议：将异常场景改为返回 `ExperimentAssignment.createErrorFallback()` 并使用可区分的 groupId；补充区分规则契约；为 AiCallRecord.promptVersion 引入空值守卫标记

3. **问题描述**：execution.timeout、degradation.strategies、sliding-window 等关键运行时配置仅启动期加载，不支持热加载，生产运维需重启实例
   - 所在位置：§9.5 行 3591-3661
   - 严重程度：重要
   - 改进建议：增加 @RefreshScope 支持或自定义定时刷新机制；至少为每项非热加载配置添加显式注释；在 §11 新增配置热刷新集成测试

4. **问题描述**：流量分配使用千分比（0-1000）与常见百分比表述不一致，增加管理端换算心智负担和校验复杂度
   - 所在位置：§3.4.4 ExperimentGroup 字段表
   - 严重程度：一般
   - 改进建议：补充千分比决策理由，或改为百分比（0-100）+ 内部浮点累加比较

## 迭代第 13 轮

1. **问题描述**：§5.1 错误分类表「实验分流异常」处理方式描述为降级到 default 分组，与 v13 实际设计使用 `ExperimentAssignment.createErrorFallback()`（`groupId="experiment-error"`）矛盾，§11.1 单元测试模式亦有相同问题
   - 所在位置：§5.1 错误分类表（行 3301）；§11.1 单元测试模式（行 3836）
   - 严重程度：严重
   - 改进建议：将 §5.1 和 §11.1 中相关描述替换为 `createErrorFallback()` 方式；同步修正测试验证断言

2. **问题描述**：`DiscussionConclusionCapabilityExecutor` 前置 LLM 压缩调用在 `llmCallExecutor` 线程池 Worker 线程内执行，高并发下可导致线程池饥饿，文档仅定义超时阈值但未评估阻塞风险或给出隔离方案
   - 所在位置：§3.11.7 特化设计表；§9.5 YAML 配置 `transcript-summary: 15s`；对比 §6.1 线程模型
   - 严重程度：一般
   - 改进建议：补充线程模型分析；建议引入独立线程池或异步重试机制；在 §9.5 配置注释中补充隔离说明

3. **问题描述**：`doDegrade()` 方法签名 14 参数、`AbstractCapabilityExecutor` 构造器 13+ 参数、`AiCallRecord` 工厂方法 15~16 参数，参数顺序依赖性和调用点复杂度构成编码实施风险
   - 所在位置：§3.1 构造器（行 1324-1344）；§4.1 `doDegrade()`（行 3107-3115）；§3.5 工厂方法（行 1988-2004）
   - 严重程度：一般
   - 改进建议：抽取 `ExecutionContext`/`CallContext` 上下文对象聚合通用参数，将 14 参数方法降维至 6~7 参数

4. **问题描述**：YAML 配置中 `client` 字段到 `ClientType` 枚举的转换约束未说明，实现者可能因 Spring Boot 枚举绑定差异导致启动期异常
   - 所在位置：§9.5 路由配置 YAML（行 3689-3691）；§3.2 `ClientType` 枚举定义（行 447-450）
   - 严重程度：一般
   - 改进建议：在 `ClientType` 枚举或 §9.5 处补充枚举值绑定转换说明，建议实现层增加防御性字符串转换

5. **问题描述**：`LlmChatRequest` 的 `messages`/`options`/`clientType` 用构造器赋值，而 `tools` 用独立 setter 设置，易被遗漏导致结构化调用退化为非结构化
   - 所在位置：§4.1 伪代码（行 2940-2948）；§3.2 字段级契约（行 1558）；类图（行 455-460）
   - 严重程度：一般
   - 改进建议：将 `tools` 纳入构造器改为全参构造；或显式声明 `tools` 为 null 时的默认行为

6. **问题描述**：`DelegatingLlmChatService` 分发层的异常包装策略未定义，底层异常是否透传或在 `CompletableFuture` 中包装后再传播不确定，`CapabilityExecutor` 存在同步/异步两套异常路径中遗漏捕获的风险
   - 所在位置：§3.2 接口契约（行 1356）；分发机制（行 1392-1401）；§4.1 异常处理伪代码（行 2962-3086）
   - 严重程度：一般
   - 改进建议：补充异常传播契约；统一约定所有 LLM 实现层异常在实现层内部捕获并转为 `CompletableFuture` 完成，确保仅在回调中处理异常

## 迭代第 14 轮

1. **问题描述**：DEGRADED 状态下同一次调用被同时记录为失败和成功，导致失败率、降级率、总调用数三个核心指标失真
   - 所在位置：§4.1 `doExecuteInternal()` 伪代码，行 2914-2920
   - 严重程度：严重
   - 改进建议：方案A——DEGRADED 状态下不预记录失败指标，等待 LLM 调用完成后按实际结果记录；方案B——新增专用 `AiCallRecord.degradedAttempt()` 工厂方法避免计数混叠

2. **问题描述**：非功能性维度的系统性分析缺失（冷启动效应、内存占用、连接池压力、启动影响等未集中评估）
   - 所在位置：全文（无集中分析章节）
   - 严重程度：一般
   - 改进建议：新增非功能性质量分析章节，集中分析冷启动惊群效应、数据库连接池压力、滑动窗口内存占用、启动时间影响

3. **问题描述**：ClientType 配置错误静默回退，运维人员配置错误时无告警
   - 所在位置：§1.3 行 55；§3.2 行 1396-1397
   - 严重程度：一般
   - 改进建议：实施防御性转换或启动期 fail-fast，或将 WARN 提升为 ERROR 并发出告警事件

4. **问题描述**：DiscussionConclusionCapabilityExecutor 线程隔离方案未定稿，实现者面临设计缺口
   - 所在位置：§3.11.7 行 2751
   - 严重程度：一般
   - 改进建议：在 §3.9 中补充完整 `@Bean` 定义，将"建议"改为具体设计决策

5. **问题描述**：7 项底座能力 DTO 改造工程量和排期缺乏直观参考
   - 所在位置：§3.5 行 2036-2064；§3.11 行 2757-2762
   - 严重程度：一般
   - 改进建议：新增 DTO 改造工作量概览表，逐项列出字段数、调用方影响、预计代码行数

6. **问题描述**：多级缓存冷启动效应未分析，首次调用的 P99 延迟将远高于稳态值
   - 所在位置：全文（§3.2 CredentialProvider、§3.3 DatabasePromptTemplateManager、§3.4 HashBucketExperimentManager、§3.2 DefaultModelRouter）
   - 严重程度：一般
   - 改进建议：补充冷启动分析，建议在 `@PostConstruct` 中对关键缓存执行预热加载

7. **问题描述**：structuredOutputParser.parse() 超时硬编码为 5 秒，无法按能力差异化
   - 所在位置：§4.1 行 3038-3045
   - 严重程度：轻微
    - 改进建议：抽取为可配置项，与 `capabilityTimeoutConfig` 风格保持一致

## 迭代第 15 轮

1. **问题描述**：`parseTimeoutConfig`/`parseTimeoutDefault`字段和@Bean均未定义，导致§4.1伪代码编译不可行，涉及YAML配置、类图、构造器、配置绑定声明、热加载表、注入方式描述等多处联动遗漏
   - 所在位置：§4.1第3163行；§3.9第2603-2614行、第2534-2539行、第2550行；§3.2第1653行；§9.5第3846-3849行
   - 严重程度：严重
   - 改进建议：在§3.9 `AiPlatformConfig`中新增`@Bean("parseTimeoutConfig")`，在`AbstractCapabilityExecutor`类图和构造器中补充相关字段，更新配置绑定声明、热加载表和注入方式描述；或简化方案将§4.1伪代码改为仅依赖`@Value`注入的`parseTimeoutDefault`

2. **问题描述**：文档头部声明"历史修订说明已剥离归档"与正文仍完整保留v7~v15共9个修订说明表矛盾
   - 所在位置：文档头部第3行 vs 行4044-4138
   - 严重程度：一般
   - 改进建议：将尾部修订说明剥离至`design_evolution_log.md`，或修正头部声明以如实反映保留情况

3. **问题描述**：§4.1 AiOrchestrator.handle()伪代码行号跳跃（37→40→37→38）且重复37、38
   - 所在位置：§4.1第2942-2946行
   - 严重程度：一般
   - 改进建议：修正为连续唯一行号

4. **问题描述**：薄适配器DTO工作量估算表将4个AiRequestBase继承字段计入"新增字段数"但备注和过渡策略均明确暂不继承，造成交接预期错位
   - 所在位置：§3.5 DTO改造工作量概览表（行2166-2171）
   - 严重程度：一般
   - 改进建议：在薄适配器DTO行备注中明确标注继承字段仅在Phase 4决定继承时生效，底座切流初期不纳入改造范围

## 迭代第 16 轮

1. **问题描述**：降级路径系统性双重计数——异常处理分支先预记 metricsCollector.record(failure) + slidingWindowMetricsStore.recordFailure()，再调用 doDegrade() 重复记录 degraded + failure，同一次调用计入"失败"和"降级"两个分类
   - 所在位置：§4.1 结构化超时降级路径、chat回退超时降级路径、LlmInfrastructureException 降级路径
   - 严重程度：严重
   - 改进建议：异常处理分支在调用 doDegrade() 前移除所有预记录调用，将指标和滑动窗口的记录职责完全委托给 doDegrade() 统一承担

2. **问题描述**：AiCallRecord 工厂方法缺少哨兵参数——§3.4 定义的 prompt_version=-1 哨兵值标记方案未在 §3.5 工厂方法签名和 §4.1 伪代码中落地
   - 所在位置：§3.4、§3.5、§4.1
   - 严重程度：重要
   - 改进建议：在 AiCallRecord 工厂方法中增加 sentinelReason 可选参数，使实验分流异常降级路径能标记 promptVersion=-1

3. **问题描述**：structuredChat 内部超时与 orTimeout 的时间竞争——固定 60%/40% 比例未考虑前置步骤消耗的时间，orTimeout 剩余时间可能显著小于 structuredChatTimeout
   - 所在位置：§4.1 doExecuteInternal()、§3.2 structuredChat() 回退路径
   - 严重程度：重要
   - 改进建议：在 supplyAsync() lambda 入口处即时计算剩余时间，按剩余时间分配超时阈值而非使用原始 capabilityTimeout 的固定比例

4. **问题描述**：parseTimeout 与 chatFallbackTimeout 的层级约束未定义——两者无约束关系可能导致超时被掩盖
   - 所在位置：§4.1、§9.5、§3.2
   - 严重程度：一般
   - 改进建议：在设计中补充 parseTimeout <= chatFallbackTimeout 约束，并在配置校验中增加比较逻辑

5. **问题描述**：分布式重构优先级遗漏熔断器-滑动窗口依赖链——CircuitBreakerDegradationStrategy 依赖 SlidingWindowMetricsStore.getFailureRate() 但重构优先级未体现此依赖
   - 所在位置：§1.5.3、§3.8
   - 严重程度：一般
   - 改进建议：在 §1.5.3 中补充依赖链说明，熔断器重构需与滑动窗口同步或顺序在前

6. **问题描述**：DiscussionConclusionCapabilityExecutor 前置压缩调用的超时控制未在伪代码中体现
   - 所在位置：§3.11.7、§4.1、§9.5
   - 严重程度：一般
   - 改进建议：在 §4.1 中补充特化伪代码段覆盖前置压缩调用的超时、截断回退条件、隔离线程池提交逻辑

## 迭代第 17 轮

1. **问题描述**：Phase4ServiceMetaProvider 接口归属与模块依赖方向存在架构矛盾。接口位于 ai-impl/thin-adapter/ 包，但要求 Phase 4 模块实现此接口，导致依赖方向反转
   - 所在位置：§3.1「Phase 4 服务可选元数据接口契约」段；§2.2「模块依赖方向」；§8.3 provided 作用域说明
   - 严重程度：严重
   - 改进建议：方案 A（推荐）：将接口迁移至 ai-api/dto/base/ 包；方案 B：底座侧改用反射 SPI 机制；方案 C：在 §2.2 增加依赖规则例外

2. **问题描述**：§2.3 类图 doDegrade 方法签名缺少 sentinelReason 参数（v17 版本回归）
   - 所在位置：§2.3 类图行 455；对比 §4.1 行 3260 及多处调用点
   - 严重程度：一般
   - 改进建议：在 doDegrade 方法签名末尾追加 sentinelReason: String 参数

3. **问题描述**：伪代码中 experimentAssignFailed 变量未声明类型
   - 所在位置：§4.1 行 3033、行 3352
   - 严重程度：一般
   - 改进建议：补充 boolean 类型声明

4. **问题描述**：文档头部版本说明未同步更新至 v17
   - 所在位置：行 3、行 4253
   - 严重程度：一般
   - 改进建议：将头部说明更新为v7~v17

5. **问题描述**：§2.1 目录结构缺少 Phase4ServiceMetaProvider.java
   - 所在位置：§2.1 行 308-314；§3.1 行 1051-1073
   - 严重程度：一般
   - 改进建议：在目录结构中补入 Phase4ServiceMetaProvider.java 条目

6. **问题描述**：AiCallRecord 工厂方法 callTime 参数缺少时间来源说明
   - 所在位置：§3.5 行 2103、行 2196
   - 严重程度：一般
   - 改进建议：补充 callTime 的时间来源约定说明

7. **问题描述**：热加载定时刷新与事件驱动刷新之间的一致性保证未定义
   - 所在位置：§3.9 热加载机制段落；§6.1 ModelRouter 刷新策略（行 3566）
   - 严重程度：一般
   - 改进建议：补充双机制协同规则说明，明确事件驱动为主动路径、定时轮询为兜底路径

## 迭代第 18 轮

1. **问题描述**：Phase4ServiceMetaProvider并发安全设计缺陷，多个请求线程同时调用同一单例Bean时，getRetryCount()等方法返回的元数据可能跨请求污染
   - 所在位置：§3.1 Phase4ServiceMetaProvider接口定义（lines 1050-1076）、§4.2 薄适配器成功路径元数据提取（lines 3449-3459）
   - 严重程度：严重
   - 改进建议：将元数据返回方式从服务实例级接口改为请求/响应级绑定（响应DTO内嵌元数据字段），或改为请求级上下文对象通过方法参数传递

2. **问题描述**：类图缺少doDegrade方法，v18修订声明声称已补充但实际未修改，类图中AbstractCapabilityExecutor仅包含8个方法，无doDegrade声明
   - 所在位置：§2.3 类图AbstractCapabilityExecutor（lines 448-465）与§4.1 doDegrade()伪代码（lines 3276-3303）
   - 严重程度：一般
   - 改进建议：在§2.3类图AbstractCapabilityExecutor类节点中新增doDegrade方法声明（与§4.1伪代码签名一致），同步修正v18修订说明

3. **问题描述**：DiscussionConclusionCapabilityExecutor前置LLM压缩调用缺少模型路由设计，压缩调用发生在实验分流/模板渲染/模型路由之前，此时无法获知目标模型端点和clientType
   - 所在位置：§4.1 DiscussionConclusionCapabilityExecutor.doExecuteInternal()特化伪代码（lines 3307-3388），特别是line 3339
   - 严重程度：一般
   - 改进建议：为压缩调用引入固定轻量模型配置（硬编码endpoint+低成本摘要模型），或在extractVariables()阶段之前允许独立ModelRouter调用并缓存结果

4. **问题描述**：estimateTokens()方法未定义，设计文档未给出使用的Tokenizer、中文字符Token换算比例、角色标记开销等具体信息，实现者无法直接编码
   - 所在位置：§4.1 DiscussionConclusionCapabilityExecutor.doExecuteInternal()伪代码（line 3326）
   - 严重程度：一般
   - 改进建议：在§3.11.7或§4.1伪代码补充estimateTokens()的具体实现策略——明确Tokenizer方案（推荐tiktoken）、给出中文医疗文本保守换算比例、说明>3000阈值的决策依据

5. **问题描述**：薄适配器异常分类通过字符串数组匹配6个异常类名，Phase4模块重构时重命名异常类或新增Phase模块时，薄适配器将静默错误归类
    - 所在位置：§4.2 薄适配器特化管线伪代码（lines 3424-3445）
    - 严重程度：一般
    - 改进建议：建立Phase4业务异常的公共基类约定，薄适配器通过instanceof匹配；或通过Class.forName()加isInstance()实现可配置的匹配

## 迭代第 19 轮

1. **问题描述**：§3.1 薄适配器异常匹配机制文本描述要求使用 `instanceof Phase4BusinessException`，但§3.1伪代码仍使用字符串数组匹配，与§4.2伪代码不一致
   - 所在位置：§3.1 薄适配器伪代码（行1244–1246）
   - 严重程度：严重
   - 改进建议：将字符串数组匹配替换为 `instanceof Phase4BusinessException`

2. **问题描述**：DiscussionConclusionCapabilityExecutor 的 `compressionLightweightEndpoint` 和 `compressionLightweightClientType` 注入点未在任何地方定义
   - 所在位置：§4.1 行3345–3349（伪代码使用处）；§3.1构造器（行1408–1432）；§9.5 YAML
   - 严重程度：一般
   - 改进建议：在构造器、@Value字段、YAML配置或类图中明确定义配置注入点

3. **问题描述**：§2.1 目录结构 `experiment/` 子包遗漏 `ExperimentGroup.java`
   - 所在位置：§2.1 目录结构 `experiment/` 子包（行347–352）
   - 严重程度：一般
   - 改进建议：在目录文件列表中新增 `ExperimentGroup.java`

4. **问题描述**：`estimateTokens()` 字符数估算方法未讨论英文医学术语场景下的偏差及误触发/漏触发边界条件
   - 所在位置：§3.11.7 行3323–3332；§4.1 行3323–3332
   - 严重程度：一般
   - 改进建议：补充极端场景误差说明或推荐使用实际Tokenizer

5. **问题描述**：structuredChat 回退路径中 retryCount 语义差异对指标聚合的影响未完全定义
   - 所在位置：§4.1 行3191–3194
   - 严重程度：一般
   - 改进建议：补充 retryCount 语义注释或标明回退路径值作为下限估计

## 迭代第 20 轮

1. **问题描述**：SlidingWindowMetricsStore 标注 @RefreshScope 将清空全部滑动窗口数据，刷新后熔断器保护失效 60 秒
   - 所在位置：§3.9「运行时配置热加载机制」表（line 2548）
   - 严重程度：严重
   - 改进建议：移除 @RefreshScope，改用 AtomicLong + 定时刷新方式（方案 A）；或声明为静态配置重启生效（方案 B）；或拆分配置层与存储层（方案 C）

2. **问题描述**：Phase4BusinessException catch 块缺少过渡期回退，未迁移的 Phase 4 模块业务异常被误分类为基础设施异常并触发错误降级
   - 所在位置：§3.1 line 1097-1101、§4.2 line 3462-3479
   - 严重程度：严重
   - 改进建议：在 instanceof 主分支前增加过渡期字符串匹配回退机制

3. **问题描述**：LocalRuleFallback.fallback() 返回值未加 null 保护，result 为 null 时 NPE 导致 doDegrade() 异常退出
   - 所在位置：§4.1 doDegrade() line 3287-3288
   - 严重程度：重要（等价于 一般）
   - 改进建议：增加 null 守卫，明确 LocalRuleFallback 接口 @return 非 null 约定

4. **问题描述**：16 参数工厂方法在实施阶段的高概率编码错误风险，参数顺序跨方法不一致
   - 所在位置：§3.5 line 2081-2098、§3.1 line 1409-1424、§4.1 line 3274
   - 严重程度：重要（等价于 一般）
   - 改进建议：Phase 5 实施期立即引入 CallContext 值对象，降维参数数量

5. **问题描述**：@ConditionalOnClass 引用的 Spring AI ChatModel 包路径可能不准确，条件判断永远不匹配
   - 所在位置：§3.2 line 1486、§3.2 AiPlatformConfig line 1510
   - 严重程度：一般
   - 改进建议：同时兼容 chat.model.ChatModel 和 chat.ChatModel 两种包路径

6. **问题描述**：前置压缩失败与主流程超时叠加时降级原因无法区分根因
   - 所在位置：§4.1 DiscussionConclusionCapabilityExecutor 特化伪代码 line 3317-3422
   - 严重程度：一般
   - 改进建议：在 exceptionally() 回调中细化超时降级原因分类

## 迭代第 21 轮

1. **问题描述**：v21修订声明第6条宣称doDegrade()等已更新为CallContext签名，但§4.1全文伪代码仍使用旧多参数签名，声明与内容严重矛盾
   - 所在位置：修订声明行4453；doDegrade定义行3351-3395；doExecuteInternal定义行3117；调用点行3086/3109/3149/3163/3210/3243/3255/3265/3295/3298/3304/3313/3318/3328/3335
   - 严重程度：严重
   - 改进建议：方案A—保持旧签名，同步修正修订说明并删除CallContext相关描述；方案B—将全部伪代码更新为CallContext新签名模式，确保声明与内容一致

## 迭代第 22 轮

1. **问题描述**：`doDegrade` 方法定义（7 参数 CallContext 签名，§4.1 第 3357 行）与所有调用点（约 17 处，15 参数旧签名）不一致，类图（§2.3 第 461 行）使用 CallContext 签名，三者处于半完成迁移态，不可编译
   - 所在位置：§2.3 类图第 461 行；§4.1 第 3357 行（定义）；§4.1 约 15 处调用点；§4.2 约 2 处调用点
   - 严重程度：严重
   - 改进建议：方案 A（推荐）回退类图和方法定义为旧 15 参数签名，或方案 B 将所有调用点更新为 7 参数 CallContext 签名
2. **问题描述**：§4.2 薄适配器 catch 块仅含单阶段 `instanceof` 检测，缺少 §3.1 契约定义的两阶段异常检测（第 2 阶段 `isKnownPhase4BusinessException()` 包路径前缀回退），过渡期内业务异常将被误分类为基础设施异常
   - 所在位置：§4.2 第 3572–3584 行 vs §3.1 第 1101–1127 行（契约定义）和第 1278–1294 行（模板方法伪代码）
   - 严重程度：重要
   - 改进建议：将 §4.2 catch 块替换为与 §3.1 模板方法一致的两阶段判定
3. **问题描述**：v22 修订说明（第 4462–4471 行）列出 5 项变更（CallContext 旧签名保持、parseTimeout 路径修正、版本标记清理、AiOrchestrator 临界条件修正、estimateTokens 精确 Tokenizer 分支），但正文内容未做对应修改
   - 所在位置：尾部第 4462–4471 行 vs 正文对应位置
   - 严重程度：重要
   - 改进建议：将 5 项变更实际应用到正文，或在头部显式标注修订说明描述的是计划中变更、正文尚未实施
4. **问题描述**：§3.1 第 1344 行 `doDegrade()` 预检降级路径调用点末尾传 `null, null`，在旧 15 参数签名下参数位置存在隐晦的错位风险
   - 所在位置：§3.1 第 1344 行（预检降级路径调用点）与 §4.1 第 3357 行（方法定义）对比
   - 严重程度：一般
   - 改进建议：在三端签名统一后，确保所有调用点参数数量、位置与定义完全一致

## 迭代第 23 轮

1. **问题描述**：degradationStrategyMap 热加载机制与构造器注入方式不一致，热替换无法生效
   - 所在位置：§3.1 降级策略注入机制（行 928~934）及构造器伪代码（行 1341）；§3.9 运行时配置热加载机制表 degradation.strategies 行（行 2428~2429）
   - 严重程度：严重
   - 改进建议：统一设计，推荐 CapabilityExecutor 改为从 `ObjectProvider` 或 `AtomicReference` 获取最新 Map

2. **问题描述**：成功路径指标记录的控制流间接性导致维护脆弱性
   - 所在位置：§4.1 `doExecuteInternal()` 伪代码，结构化 chat 成功分支（行 3235~3240）、chat 回退成功分支（行 3279~3297）、共用成功处理段（行 3341~3351）
   - 严重程度：一般
   - 改进建议：在两条成功路径末尾添加 `// fall-through to shared success handler` 注释，或提取共用段为辅助方法

3. **问题描述**：`estimateTokens()` 的 jtokkit 精确 Tokenizer 分支缺少实现细节
   - 所在位置：§4.1 `DiscussionConclusionCapabilityExecutor` 特化伪代码（行 3445~3451）；§8 Maven 依赖清单
   - 严重程度：一般
    - 改进建议：补充 `tokenizerAvailable` 判定逻辑和 `preciseTokenCount()` 方法签名；在 §8 依赖清单中新增 jtokkit 条目（optional = true）

## 迭代第 24 轮

1. **问题描述**：薄适配器构造器super()调用参数与父类构造器签名不匹配，缺失`parseTimeoutConfig`和`parseTimeoutDefault`两个参数，后续实参位置错位，导致Java代码编译失败
   - 所在位置：§3.1 行1160-1162（super()调用）vs 行1465-1480（构造器签名）
   - 严重程度：严重
   - 改进建议：补齐缺失的两个参数并修正余下实参的位置，同步修正其余5个薄适配器子类的构造器示例

2. **问题描述**：`doDegrade`的15参数签名编码风险高，任何参数变更需同步修改约17处调用点
   - 所在位置：§2.3类图行461、§4.1 doDegrade定义行3373、§4.1约15处调用点、§4.2约2处调用点
   - 严重程度：一般
   - 改进建议：制定分期计划——一期定义CallContext值对象+重载签名、二期逐个迁移调用点、三期移除旧签名，每期设定可验收的里程碑

3. **问题描述**：§3.5工厂方法以CallContext简化签名示出，但§4.1全文仍使用旧多参数签名，两处不一致
   - 所在位置：§3.5行2171-2184 vs §4.1行3354-3359
   - 严重程度：一般
   - 改进建议：在§3.5中补充当前多参数工厂方法的完整签名表，或在本迭代中统一到CallContext签名

4. **问题描述**：`inputType`字段在类图和构造器中均未明确定义，但被doDegrade()引用用于泛型安全检查
   - 所在位置：§4.1行3379、§2.3类图行454、§3.1构造器参数
   - 严重程度：一般
   - 改进建议：在§3.1构造器参数中补充inputType的注入逻辑，或在类图中补充该字段的可见性和声明来源

5. **问题描述**：KbQueryRequest同时以业务字段和继承字段声明departmentId，两处赋值路径可能不一致
    - 所在位置：§3.11.5行2959、§3.5行2203-2208、§3.1行1385-1388
    - 严重程度：一般
    - 改进建议：方案A——业务字段中移除departmentId；方案B——保留并在doExtractDepartmentId()特化中优先从业务字段读取，显式说明语义差异

## 迭代第 25 轮

1. **问题描述**：`LlmChatRequest.tools` 通过独立 setter 构造，与 messages/options/clientType 通过构造器赋值的模式不一致，建议停留在字段级契约注释中未在类图、伪代码或构造器设计中实际落地，实施者遗漏时将静默退化
   - 所在位置：§3.2 LlmChatRequest 字段级契约（行 1739）、§4.1 doExecuteInternal()、§2.3 类图 LlmChatRequest 节点（行 547-552）
   - 严重程度：一般
   - 改进建议：将 tools 纳入全参构造器，同步更新类图构造器签名，添加 @JsonCreator 验证或 @Builder.Default 默认空列表

2. **问题描述**：`estimateTokens()` 默认字符估算路径在中文医疗文本场景下假阳性率 30%~70%，jtokkit 未引入或初始化异常时约 30%~70% 的讨论结论调用将被误压缩，导致 Token 浪费和 P99 延迟增加，§4.1 未设防
   - 所在位置：§4.1 DiscussionConclusionCapabilityExecutor 特化伪代码行（3323-3332 附近）、§3.11.7 分析段落
   - 严重程度：一般
   - 改进建议：将压缩判定阈值改为估算字符数 >5400 字符；字符估算路径增加 WARN 日志；添加命中率指标供运维仪表盘分析

3. **问题描述**：6 项 Phase 4 业务模块异常类中 3 个（ImageAnalysisException、ExaminationException、ExecutionOrderException）标记为需验证且依赖跨包协作会议确认，底座 P0 上线时错误码格式可能不一致，下游指标聚合和告警规则无法统一匹配
   - 所在位置：§3.1 异常契约表（行 1090-1097）、异常处理规则统一契约（行 1099-1127）
   - 严重程度：一般
   - 改进建议：底座侧统一转换为 "{capabilityId}_{errorType}" 格式；明确跨包会议延迟时的默认行为决策规则

4. **问题描述**：文档 4576 行中尾部修订说明占约 210 行（v7~v27），实施者首次阅读无法目测是否可安全跳过，全文搜索时旧行号指代易混淆，与 iteration_history.md 功能重复
   - 所在位置：尾部行 4367~4576（修订说明 v7~v27）
   - 严重程度：一般
   - 改进建议：将 v7~v27 修订说明剥离至 design_evolution_log.md；正文仅保留变更摘要行版本范围记录；或压缩为修订轮次→影响的§节编号→概要对照表

5. **问题描述**：§9.2 详细描述了 FallbackAiService 构造器从 List 到 ObjectProvider 的迁移计划，但 §2.3 类图仅显示旧设计（-AiService delegate），未体现 ObjectProvider 注入或 @Primary 标注，给实施者错误信号
   - 所在位置：§2.3 类图 FallbackAiService 节点（行 426-432）、§9.2 迁移方案（行 4069-4082）
   - 严重程度：一般
   - 改进建议：在 §2.3 类图 FallbackAiService 节点注释标注 ObjectProvider 迁移方案；或在 §1.3 核心抽象表补充注入方式说明

6. **问题描述**：6 项薄适配器 DTO 改造的改造归属、底座临时 fallback 方案、验收标准、跨包协作会议截止时间分散在 §3.1、§3.5、§9.1 三处，实施者需交叉引用才能完整理解跨包依赖
   - 所在位置：§3.1（行 1047-1053）、§3.5 备注列（行 2285-2290）、§9.1（行 4056-4061）
   - 严重程度：一般
   - 改进建议：在 §1.4 新增对应的改造依赖详情子段显式引用三处位置；或新建 §1.4.2 风险视图表格浓缩关键信息

7. **问题描述**：§3.2 中 @ConditionalOnClass 使用单 class 名称修复了 AND/OR 语义问题，但未添加注释说明为何使用单 class 名称而非数组，后续维护者可能重蹈语义混淆
   - 所在位置：§3.2 DelegatingLlmChatService 章节（行 1575 附近）
   - 严重程度：轻微
   - 改进建议：在 @ConditionalOnClass 所在行添加 inline 注释说明 name 属性的 AND 语义及单 class 名称的原因

## 迭代第 26 轮

1. **问题描述**：`extractVariables()` 的职责归属在 §3.11.7 与 §4.1 之间存在事实矛盾
   - 所在位置：§3.11.7「模板变量提取策略」行；§4.1 DiscussionConclusionCapabilityExecutor.doExecuteInternal()
   - 严重程度：严重
   - 改进建议：统一职责归属，采纳 §4.1 伪代码做法，压缩逻辑放在 doExecuteInternal() 中，`extractVariables()` 仅提取简单字段
2. **问题描述**：DiscussionConclusionCapabilityExecutor 的 `super.doExecuteInternal()` 调用不可行——模板方法设计存在结构性缺陷
   - 所在位置：§4.1 行 3578-3580（super.doExecuteInternal() 引用）；行 1393（AbstractCapabilityExecutor.doExecuteInternal() 定义为 abstract）
   - 严重程度：严重
   - 改进建议：将标准管线逻辑从 `abstract doExecuteInternal()` 提取为 `protected` 非抽象方法（如 `executeStandardPipeline(...)`），子类完成前置逻辑后调用
3. **问题描述**：`preciseTokenCount()`/`formatTranscripts()`/`truncateTranscripts()` 三个辅助方法未正式定义
   - 所在位置：§4.1 行 3511、行 3553、行 3567、行 3570
   - 严重程度：一般
   - 改进建议：补充正式方法定义，包括方法签名、入参、返回值和行为契约
4. **问题描述**：字符估算回退分支中 `estimatedTokens = 2000` 的硬编码跳跃值缺乏依据
   - 所在位置：§4.1 行 3521-3527
   - 严重程度：一般
   - 改进建议：将回退分支触发阈值从 3000 提升到 4000，或提供 tokenizer 不可用时简化替代实现
5. **问题描述**：类图未展示 `AbstractCapabilityExecutor` 构造器
   - 所在位置：§2.3 类图 `AbstractCapabilityExecutor` 节点（行 467-485）
   - 严重程度：一般
   - 改进建议：在类图中补充构造器签名或注释指向 §3.1 详细定义
6. **问题描述**：`userId` 与 `callerId` 语义冗余且来源相同
   - 所在位置：§4.1 行 3115-3116；§3.10 `extractCallerId()` 定义
   - 严重程度：一般
   - 改进建议：补充说明两值语义区别，消除实施者疑虑
7. **问题描述**：`convertValue()` 防御性拷贝的失败后果未定义
   - 所在位置：§4.1 行 3125
   - 严重程度：一般
   - 改进建议：增加 try-catch 捕获异常后回退使用原始 request 对象
8. **问题描述**：`structuredChat()` 成功路径中 `fall-through` 到共享处理器但实际控制流不直观
   - 所在位置：§4.1 行 3290、行 3348
   - 严重程度：一般
   - 改进建议：添加显式结构化标记或注释锚点

## 迭代第 27 轮

1. **问题描述**：DiscussionConclusionCapabilityExecutor构造器未完整定义，缺少compressionLightweightEndpoint和compressionLightweightClientType两个配置参数的注入声明
   - 所在位置：§3.11.7 特化设计表；§4.1 行3583~3587
   - 严重程度：严重
   - 改进建议：在§3.11.7末尾或§3.1中补充完整构造器签名和super()调用示例，在§2.3类图中补充特有字段声明

2. **问题描述**：executeStandardPipeline()抽象定义与伪代码实现之间不一致，半展开的伪代码让实现者面临两难
   - 所在位置：§3.1 行1412~1421 vs §4.1 行3621~3652
   - 严重程度：严重
   - 改进建议：方案A：将伪代码改为直接调用executeStandardPipeline(variables, ...)；方案B：保留展开版本但明确说明是出于文档可读性目的展开而非要求子类重新实现

3. **问题描述**：transcriptSummaryExecutor的CallerRunsPolicy与线程池隔离目的在语义上存在矛盾，队列满时隔离设计完全失效
   - 所在位置：§3.9 行2801~2806；§4.1 行3602~3610
   - 严重程度：严重
   - 改进建议：方案A（推荐）：改为DiscardPolicy + WARN日志；方案B：保留CallerRunsPolicy但显式说明队列满时的行为退化及影响

4. **问题描述**：13项能力DTO的4个公共字段（visitId/patientId/sessionId/departmentId）的调用方填充责任未明确定义，业务模块需自行推断填充逻辑
   - 所在位置：§3.5行2276~2305；§3.10行2937~2942；§3.1行2945~2950
   - 严重程度：一般
   - 改进建议：新增独立"调用方数据准备指引"小节，集中说明填充方式、过渡期为空时的底座行为、以及现有模块兼容性保护

5. **问题描述**：熔断器-端点健康管理器统一探测机制在多端点场景下缺少处理规则，存在潜在全量等待风险
   - 所在位置：§3.2行1720~1732；§3.8行2593~2603
   - 严重程度：一般
   - 改进建议：补充多端点场景处理规则，明确熔断器状态按capabilityId还是endpointId绑定，避免endpoint级别不可用升级为能力级别全局熔断

## 迭代第 28 轮

1. **问题描述**：`doDegrade()`参数签名在类图、迁移计划与伪代码之间呈现四态并存
   - 所在位置：§2.3 第477行、§3.1 第1560~1564行（迁移计划）、§3.5 第2238~2252行（简化签名）、§4.1全文（旧签名调用点）
   - 严重程度：严重
   - 改进建议：在§2.3类图中新增CallContext重载签名行并标注「// 二期迁移目标」，或在类图注释中明确指向§3.1迁移计划，说明当前编码应使用旧签名

2. **问题描述**：缺少启动期Bean初始化顺序的整体依赖约束图
   - 所在位置：§3.1（策略Map构建时序）、§1.7（实施拓扑）、§3.9（AiPlatformConfig/AiPlatformEnvironmentPostProcessor），无集中说明
   - 严重程度：一般
   - 改进建议：新增独立子章节（如§3.9.1或附录）提供启动期Bean初始化顺序依赖图，覆盖AiPlatformEnvironmentPostProcessor→@ConditionalOnProperty评估、DegradationStrategy初始化→AiPlatformConfig.@PostConstruct、CapabilityExecutor初始化→AiOrchestrator.@PostConstruct、HikariCP就绪→@PostConstruct预热查询四条约束链

3. **问题描述**：6项薄适配器能力的特化设计缺乏集中视图
   - 所在位置：§3.1、§3.5、§4.2（分散）；缺乏类似§3.11的集中视图
   - 严重程度：一般
   - 改进建议：在§3.11（或新增§3.12）中增加「薄适配器能力特化设计」子节，按与底座能力一致的格式，为6项薄适配器逐一列出能力标识、DTO扩展字段、Phase4服务接口引用、异常契约、依赖状态

4. **问题描述**：`structuredChat()`双降级路径的根因追溯缺口
   - 所在位置：§4.1 `doExecuteInternal()`降级回调、§3.2 structuredChat回退路径超时叠加风险段
   - 严重程度：一般
   - 改进建议：在`DegradationReason.TIMEOUT`中增加细分标识（如`:StructuredChatTimeout` vs `:ChatFallbackTimeout`），或在`CompletableFuture`的`exceptionally()`回调中根据`elapsedInDoExecuteInternal`与`capabilityTimeout`比例关系推断根因

5. **问题描述**：多实例配置生效时间窗口差异未做约束分析
    - 所在位置：§1.5、§3.9（热加载机制）、§10.4
    - 严重程度：一般
    - 改进建议：在§1.5或§3.9中补充配置生效时间不一致的已知约束分析，至少注释说明此差异对降级策略切换、超时阈值调整等运维操作的影响

## 迭代第 29 轮

1. **问题描述**：薄适配器超时WARN日志使用全局默认值（`thinAdapterTimeout`）而非实际生效的per-capability覆盖值（`effectiveThinAdapterTimeout`），误导运维排障
   - 所在位置：§4.2 薄适配器特化管线伪代码，第3927行 vs 第3934行
   - 严重程度：严重
   - 改进建议：将第3934行 `thinAdapterTimeout.toMillis()` 替换为 `effectiveThinAdapterTimeout.toMillis()`

2. **问题描述**：LlmChatOptions阶段一填充伪代码仅映射白名单中6个key的前3个（temperature、maxTokens、stopSequences），遗漏topP、frequencyPenalty、presencePenalty，导致SDK默认值覆盖路由配置值
   - 所在位置：§3.2 第1845行（白名单声明）vs §4.1 第3538-3544行（伪代码映射）
   - 严重程度：严重
   - 改进建议：在§4.1伪代码阶段一填充中补充topP、frequencyPenalty、presencePenalty的映射逻辑

3. **问题描述**：PrescriptionAssist变量提取策略（方式A：ObjectMapper.convertValue）与嵌套DTO结构（PatientInfo内嵌值对象）不匹配，模板变量{{patientAge}}等无法正确填充，且与PrescriptionCheck同类结构的处理方式（方式B：自定义展开）不一致
   - 所在位置：§3.11.4 第3145行（模板变量）、第3147行（变量提取策略）
   - 严重程度：一般
   - 改进建议：统一PrescriptionAssist与PrescriptionCheck的患者数据建模方式和变量提取策略（推荐改为方式B自定义展开，或改为扁平字段结构）

4. **问题描述**：AiCallRecord.capabilityName字段在字段定义表中存在，但三个工厂方法签名（success()/failure()/degraded()）均不包含该参数，管线中所有调用点均未传入，实施者无法获知填充来源
   - 所在位置：§3.5 第2207-2208行（字段定义）、第2232-2252行（工厂方法签名）；§4.1各工厂方法调用点
   - 严重程度：一般
   - 改进建议：方案A（推荐）：在工厂方法签名中补充String capabilityName参数并更新调用点；方案B：在字段定义注释中说明由capabilityId自动映射补全

5. **问题描述**：精确Tokenizer路径与字符估算回退路径在§4.1第3846行共用同一>3000阈值判断，但v27修订说明要求估算路径阈值提升至4000，修订意图未落实
   - 所在位置：§4.1 第3817-3846行；v27修订说明第9条（第4704行）
   - 严重程度：一般
   - 改进建议：按路径区分阈值——精确路径使用3000，估算路径使用4000（或等价于校正后实际Token为3000的字符数阈值）
