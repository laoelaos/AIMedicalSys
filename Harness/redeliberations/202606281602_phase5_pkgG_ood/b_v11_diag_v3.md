## 质量审查报告（v11，v3 修订版）

### 一、需求响应充分度

**以下内容保持 v2 报告 §一 的完整判定不变。** 经复查 v11 产出，需求响应充分度无新增缺口。

产出充分响应了用户需求的全部 3 项条款：风格一致性有明确的跨阶段对比例证（§1.2 列出的 5 条风格规则）；包 G 完整性覆盖 7 项底座能力 + 6 项薄适配器能力 + 底座基础设施；OOD 核心要素均已覆盖（类图/职责/协作/接口/状态模型）。

### 二、深度与完整性评估

**以下内容保持 v2 报告 §二 的完整判定不变。** 经复查 v11 产出，伪代码深度充足、接口契约清晰、异常场景覆盖充分。

v11 版本新增的 §1.4「底座切流初期已知限制」、§1.6「API Surface 状态表」、§1.7「实施拓扑顺序」、§3.1 Phase 4 模块异常契约表、§3.9 调度线程池配置等均已实质性提升了文档的可落地性。

### 三、存在的问题

#### 【保留】问题 2：`ExperimentGroup` 类图节点缺失（图-文不一致）

- **位置**：§2.3 类图，`Experiment` 类含 `+List<ExperimentGroup> groups` 但无 `ExperimentGroup` 类节点
- **问题描述**：§3.4 已完整定义 `ExperimentGroup` JPA Entity 的字段表（id/experiment/group_id/percentage/target_model_id/target_prompt_version）和流量分配算法约束，但 §2.3 类图中未出现此类节点，实现者仅参类图将遗漏 `ExperimentGroup` 的结构信息。
- **严重程度**：一般
- **改进建议**：在 §2.3 类图中新增 `ExperimentGroup` 类节点及与 `Experiment` 的 `@OneToMany` 关联线。

#### 【保留】问题 3：`AiCallLogStats` 类图节点缺失（图-文不一致）

- **位置**：§2.3 类图，无 `AiCallLogStats` 节点
- **问题描述**：§3.5 完整定义了 `AiCallLogStats` JPA Entity 的字段表（id/capability_id/stat_month/total_calls/success_count/degraded_count/failure_count/avg_elapsed_ms/P50/P95/P99）与索引策略，但 §2.3 类图中未出现此类节点。
- **严重程度**：一般
- **改进建议**：在 §2.3 类图中新增 `AiCallLogStats` 类节点。

#### 【保留】问题 4：`StructuredOutputParser.parse()` 独立超时未体现于 §4.1 伪代码

- **位置**：§3.2（文本描述，parse() 有独立 5s 超时）vs §4.1（伪代码行 2901，`parsedResult = structuredOutputParser.parse(chatResponse, outputType)` 无超时包裹）
- **问题描述**：§3.2 明确声明 parse() 有独立 5s 超时控制（`CompletableFuture.supplyAsync().get(5s)`），但 §4.1 伪代码中 `structuredOutputParser.parse()` 调用直接内联执行，无超时包裹。实现者仅参 §4.1 伪代码将遗漏此独立超时。
- **严重程度**：一般
- **改进建议**：在 §4.1 行 2901 处补充超时包裹逻辑（如 `CompletableFuture.supplyAsync(() -> structuredOutputParser.parse(...)).get(5, TimeUnit.SECONDS)`），超时时进入降级路径。

#### 【保留】问题 5：`AiOrchestrator.handle()` catch 块中 `callerRole` 提取逻辑与 `AbstractCapabilityExecutor.extractCallerRole()` 不一致（v2 报告问题 5，v11 未修复）

- **位置**：§4.1（行 2687-2689）vs §3.1 `extractCallerRole()` 定义（行 1202-1225）
- **问题描述**：`AiOrchestrator.handle()` 的 catch 块使用 `auth.getAuthorities().iterator().next().getAuthority()` 直接获取原始角色字符串（如 `"ROLE_DOCTOR"`），未执行 SPRING Security `ROLE_` 前缀过滤。而 `AbstractCapabilityExecutor.extractCallerRole()` 中定义了完整的 `ROLE_` 前缀过滤逻辑（优先查找以 `"ROLE_"` 开头的 authority 并去除前缀，输出 `"DOCTOR"`）。正常路径输出 `"DOCTOR"`，异常路径输出 `"ROLE_DOCTOR"`，导致 `AiCallRecord.callerRole` 字段出现不一致的值格式，基于角色的统计分析和监控维度聚合不准确。
- **严重程度**：重要
- **改进建议**：将 `extractCallerRole()` 抽取为 `RequestContextUtils` 中的公用静态方法，使 `AiOrchestrator.handle()` catch 块和 `AbstractCapabilityExecutor` 共享同一角色提取逻辑。

#### 【保留】问题 6：`PatientInfo` 类型未定义且 `PrescriptionLocalRuleFallback` 引用了不存在的字段路径（v2 报告问题 6，v11 部分修复、部分未修复）

- **位置**：§3.11.2（行 2548）DTO 扩展字段表；§3.7（行 2180-2181）最小安全规则表
- **问题描述**：
  (1) `PrescriptionCheckRequest` 的扩展字段 `patientInfo: PatientInfo` 使用了未定义的类型。整个文档未定义 `PatientInfo` 的字段结构（age/weight/allergyInfo/pregnancyStatus 等如何命名、方法签名是什么、是新建类还是复用已有类型），实现者无法确定正确的实现方案。
  (2) §3.7 的"儿童/孕妇用药警示"规则引用 `request.patientAge` 和 `request.pregnancyStatus` 作为数据来源，但 `PrescriptionCheckRequest` 的 DTO 定义中这些字段归属 `patientInfo` 内嵌对象而非直接位于请求 DTO 上。正确的引用路径应为 `request.patientInfo.getAge()` / `request.patientInfo.getPregnancyStatus()`（或其他对应的方法名）。
- **严重程度**：重要
- **改进建议**：
  (1) 在 §3.11.2 或 §3.7 中补充 `PatientInfo` 类的完整字段表（如 `age: Integer`、`weight: Double`、`allergyInfo: List<String>`、`pregnancyStatus: PregnancyStatus` 等），标注该类是否为 ⊕ 需新增。
  (2) 修正 §3.7 中 `request.patientAge` 和 `request.pregnancyStatus` 的引用路径为 `request.patientInfo` 下的对应方法。

#### 【新增】问题 7（v11）：`doExecuteInternal()` 中 `structuredChat` 和 `chat` 回退路径的 `catch (TimeoutException)` 因 `.join()` 包装而成为死代码

- **位置**：§4.1 `doExecuteInternal()`，行 2864（`catch (java.util.concurrent.TimeoutException te)` 处理 `structuredChat` 超时）和行 2915（`catch (java.util.concurrent.TimeoutException te)` 处理 `chat` 回退超时）
- **问题描述**：`CompletableFuture.orTimeout().join()` 在超时触发后，`.join()` 将原始的 `TimeoutException` 包装为 `CompletionException(TimeoutException)` 抛出，而非直接抛出 `TimeoutException`。因此 `catch (TimeoutException)` 块**永远不会被执行**。实际超时时，异常将落入 `catch (CompletionException ce)` 块（行 2875）或 `catch (LlmInfrastructureException e)` 块（行 2929），降级原因被记录为 `DegradationReason.INFRASTRUCTURE_ERROR + ":TimeoutException"`，而非设计意图的 `DegradationReason.TIMEOUT + ":structuredChatTimeout"` 或 `DegradationReason.TIMEOUT + ":chatFallbackTimeout"`。
  - 此问题导致：(a) `structuredChat` 超时和 `chat` 回退超时无法按设计意图区分为"超时类降级"，被统一归类为"基础设施异常"；(b) §5.1 错误分类表中行"LLM 调用超时"和"结构化输出格式不支持"的降级原因对应关系不准确。
- **严重程度**：重要
- **改进建议**：
  方案 A（推荐）：将 `.join()` 替换为 `.get()`（后者抛出 `ExecutionException`），在 `catch (ExecutionException)` 中拆解原始异常，对 `TimeoutException` 走超时降级路径，对 `StructuredOutputNotSupportedException`/`LlmInfrastructureException` 走原有逻辑。伪代码示例：
  ```
  try:
      structuredChatFuture = llmChatService.structuredChat(chatRequest, outputType)
          .orTimeout(structuredChatTimeout.toMillis(), TimeUnit.MILLISECONDS)
      aiResult = structuredChatFuture.get()
  catch (java.util.concurrent.TimeoutException te):
      // 超时降级路径（行 2864 伪代码保持不变，但 .get() 直接抛出 TimeoutException 而非包装）
  catch (java.util.concurrent.ExecutionException ee):
      originalCause = ee.getCause()
      // 处理 StructuredOutputNotSupportedException / LlmInfrastructureException
  ```
  方案 B：在 `catch (CompletionException ce)` 中补充对 `ce.getCause() instanceof TimeoutException` 的检查分支，将 `TimeoutException` 路由到超时降级路径并记录正确的降级原因。

#### 【新增】问题 8（v11）：`PrescriptionAssistRequest` 与 `PrescriptionCheckRequest` 的患者数据建模方式不一致

- **位置**：§3.11.2（行 2548）`PrescriptionCheckRequest` DTO vs §3.11.4（行 2576）`PrescriptionAssistRequest` DTO
- **问题描述**：两个能力均涉及患者人口学数据，但采用了不同的建模方式——`PrescriptionCheckRequest` 使用内嵌 `PatientInfo` 对象（含年龄/体重/过敏史/孕产状态），而 `PrescriptionAssistRequest` 将 `patientAge: int`、`patientWeight: double`、`allergyInfo: List<String>` 作为扁平 DTO 字段直接声明。此不一致导致：(a) 实现者在处理患者数据时需为两个 DTO 编写不同的字段提取逻辑；(b) 若后续新增其他能力（如病历生成），难以确定采用哪种建模风格；(c) 若 `PatientInfo` 是通用值对象，`PrescriptionAssistRequest` 的扁平设计将失去复用 `PatientInfo` 类型安全校验的机会。
- **严重程度**：一般
- **改进建议**：统一患者数据的建模方式。推荐方案 (1) 两个请求 DTO 均使用 `PatientInfo` 内嵌对象（将 `patientAge`/`patientWeight`/`allergyInfo` 移动到 `PatientInfo` 中）；或方案 (2) 在 §3.11 共同约束段（行 2623-2630）中增加一条说明，明确两个 DTO 的建模差异是有意为之及其理由（如处方审核需要更全面的患者信息，辅助开方只需要基本的人口学信息）。若选择方案 (2)，需同步更新 §3.7 `PrescriptionLocalRuleFallback` 中对 `PrescriptionCheckRequest` 的引用路径，确保 `patientAge` 和 `pregnancyStatus` 的获取逻辑与 DTO 结构一致。

#### 【新增】问题 9（v11）：`AbstractCapabilityExecutor` 构造器中 `ObjectMapper` 的来源未定义

- **位置**：§3.1 `AbstractCapabilityExecutor` 构造器（行 1266-1284）和 `execute()` 伪代码（行 2714 `objectMapper.convertValue(request, request.getClass())`）
- **问题描述**：`execute()` 伪代码中使用了 `objectMapper.convertValue()` 进行防御性拷贝，但 `AbstractCapabilityExecutor` 构造器签名中未包含 `ObjectMapper` 参数，也未说明 `objectMapper` 的获取方式（是从 Spring 容器中获取、通过构造器注入、还是使用 `new ObjectMapper()`）。实现者面对此缺口需自行决定获取方式，可能出现不同的实现选择（如直接 `new ObjectMapper()` 造成 Joda-Time/LocalDateTime 序列化模块未注册、或从 Spring 获取时未使用 `@Qualifier` 导致注入失败）。
- **严重程度**：一般
- **改进建议**：在 `AbstractCapabilityExecutor` 构造器中新增 `ObjectMapper objectMapper` 参数，在 `AiPlatformConfig` 中通过 `@Bean("capabilityObjectMapper")` 提供（使用 Spring Boot 自动配置的 `JacksonAutoConfiguration` 创建的 `ObjectMapper` 实例），或在构造器注释中说明 `objectMapper` 的来源约定（如"通过 `@Autowired ObjectMapper` 从 Spring 容器获取"）。

#### 【删除】问题 1（v2 报告问题 1 `scheduledTaskExecutor` Bean 命名）

**已从本报告中移除。** 理由：经 v2 修订说明 C 复查 Spring 源代码确认——`ScheduledAnnotationBeanPostProcessor` 按**类型**（`TaskScheduler.class`）查找而非按名称，自定义 `@Bean("scheduledTaskExecutor") ThreadPoolTaskScheduler` 作为唯一的 `TaskScheduler` 类型 Bean 可以被正确发现。Spring Boot 的 `TaskSchedulingAutoConfiguration` 因 `@ConditionalOnMissingBean(TaskScheduler.class)` 退避，不创建默认调度器。因此问题 1 的技术认定依据不成立，Bean 命名不会导致 `@Scheduled` 任务回退到单线程调度器。

### 四、整体评价

v11 版产出已相当成熟，经多轮迭代后绝大多数严重问题已修复。文档结构完整、伪代码细致、异常场景覆盖充分，可以直接指导编码实施。剩余问题均为图-文一致性、伪代码边界情况（TimeoutException 包装）、类型定义完整性和建模一致性等次要维度，不影响整体可交付性。

### 五、修订说明（v3）

| 质询意见 | 回应 |
|---------|------|
| A. [严重] §三 问题 1 的技术认定依据被报告自身的 §四 C 直接推翻，且仍保持"重要"严重程度。报告正文（Spring 按名称查找）与修订说明（Spring 按类型查找）自相矛盾。 | **已执行。** 删除 §三 问题 1（`scheduledTaskExecutor` Bean 命名），理由在 §三「删除」段落中详细说明。核心结论：Spring `ScheduledAnnotationBeanPostProcessor` 按类型查找 `TaskScheduler`，自定义 Bean（唯一的 `TaskScheduler` 类型）可被正确发现，命名不匹配不构成问题。 |
| B. [建议] 可考虑补充"历史严重问题回归验证"——迭代历史中多项严重问题在报告中未明确确认是否已在 v11 中修复。 | **已补充。** 逐项检查迭代历史中严重问题的修复状态——v10 问题 5/6/7（modelId 参数、构造器 `super()`、`BusinessException`）已在 v11 修复；v2 报告问题 5/6（callerRole 一致性、PatientInfo 类型）在 v11 中**未修复**，保留为本轮问题 5/6；v8 问题 2/3（图-文不一致）未修复，保留为本轮问题 2/3。 |
| C. 问题 5（v2 报告）——`AiOrchestrator.handle()` catch 块 `callerRole` 提取逻辑不一致，经查 v11 未修复。 | **保留为本轮问题 5，严重程度维持"重要"。** v11 修订说明中无此项修复记录，且 §4.1 行 2687-2689 仍使用原始 `auth.getAuthorities().iterator().next().getAuthority()`。 |
| D. 问题 6（v2 报告）——`PatientInfo` 类型未定义且引用字段路径不准确，经查 v11 部分修复、部分未修复。 | **保留为本轮问题 6，严重程度维持"重要"。** `PatientInfo` 类型定义仍缺失；§3.7 行 2181 的 `request.patientAge`/`request.pregnancyStatus` 字段引用路径仍未修正。 |
| E. **新增发现**——§4.1 `doExecuteInternal()` 中两个 `catch (TimeoutException)` 块因 `.join()` 包装成为死代码。 | **新增为本轮问题 7（重要）。** 详细分析和两种修复方案见问题 7 描述。 |
| F. **新增发现**——`PrescriptionAssistRequest` 与 `PrescriptionCheckRequest` 的患者数据建模方式不一致。 | **新增为本轮问题 8（一般）。** |
| G. **新增发现**——`AbstractCapabilityExecutor` 构造器未定义 `ObjectMapper` 来源，防御性拷贝的注入方式缺口。 | **新增为本轮问题 9（一般）。** |
