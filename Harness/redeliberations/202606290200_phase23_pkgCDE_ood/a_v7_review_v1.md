# OOD 设计方案审查报告（v7）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 设计中使用的 interface（TriageService、DrugFacade、LocalRuleEngine 等 16 个接口）、class（DialogueSession、DosageThresholdService 等可实例化类）、enum（AuditRiskLevel、MedicalRecordField、DosageUnitGroup 等 7 个枚举）以及 JPA @Entity（AuditRecord、TriageRecord、MedicalRecord 等）的类型形态选择与 Java 类型系统完全匹配。

**[通过]** 单继承（JPA @Entity 继承体系）+ 多接口实现（Service impl 类）模式在 Java 约束范围内，无菱形继承或冲突问题。

**[通过]** 泛型使用（CompletableFuture<AiResult<T>> 绑定为 4 种 ai-api Response DTO）在 Java 泛型系统的类型擦除与通配符范围内可行。AiResult<T> 作为泛型容器类，其 failure()/degraded() 重载工厂方法中的泛型参数 T 由调用方确定，编译期类型安全。

**[通过]** Store 接口抽象层（SessionStore、SuggestionStore、DraftContextStore）的设计通过 interface 隔离存储实现，ConcurrentHashMapStore 与 RedisStore 可各自独立实现而无编译期冲突，模式可行。

### 2. 标准库与生态覆盖

**[通过]** Spring Boot（Controller、Service、JPA @Entity、@Transactional、@Version、@Retryable）覆盖设计中全部框架级需求。

**[通过]** java.util.concurrent（ConcurrentHashMap、CompletableFuture、ScheduledExecutorService）覆盖内存存储、异步调用和定时清理需求。

**[通过]** Caffeine（refreshAfterWrite 定时缓存刷新）+ Spring ApplicationEventPublisher（变更事件发布）覆盖缓存与事件驱动模式，二者在 Spring Boot 生态中均有成熟集成。

**[通过]** Jackson（JSON TEXT 序列化/反序列化 @Convert）覆盖 contentJson、originalPrescription 等 JSON 字段的存储需求；SpringDoc OpenAPI 覆盖接口文档自动生成。

**[通过]** Spring Retry（@Retryable）覆盖 RegistrationEvent 消费失败重试，为标准 Spring 扩展库。

### 3. 语言特性可行性

**[通过]** 错误处理采用 BusinessException + ErrorCode + GlobalExceptionHandler 模式，与 Spring Boot 异常处理框架一致；BLOCK 阻断通过 HTTP 422 + BlockResponse 独立返回，与业务异常体系正交，不冲突。

**[通过]** 并发设计覆盖了三类场景：ConcurrentHashMap + TTL 清理的会话并发（DialogueSessionManager）、CompletableFuture + @Async 的异步 AI 调用（check-dose 流程）、@Version 乐观锁的病历并发写入（MedicalRecord），均在 Java/Spring 并发模型能力范围内。

**[通过]** 资源管理通过 ScheduledExecutorService 定时清理 TTL 过期条目（DialogueSession 30 分钟、AiSuggestionResult 60 分钟、PrescriptionDraftContext 60 分钟），标准 Java 定时任务模式。

**[通过]** 模块/包结构（扁平 Maven 模块、api/service/repository/entity/dto/converter 分包）与已有 patient/doctor/admin 模块风格一致，依赖方向明确（三新模块仅依赖 common/common-module-api/ai-api，模块间不互依赖）。

### 4. 设计一致性

**[通过]** 各抽象职责描述清晰无歧义。§1.3 核心抽象一览表给出每个抽象的类型形态和职责定位，§3 各节逐项展开协作对象和行为契约。

**[通过]** 协作关系形成完整闭环：
- 分诊流程：前端 → TriageController → TriageService → AiService.triage() / TriageRuleEngine / DepartmentFallbackProvider → DoctorFacade → TriageRecord → RegistrationEvent → finalDepartmentId 写入
- 处方审核流程：前端 → PrescriptionAuditController → PrescriptionAuditService → AiService.prescriptionCheck() / LocalRuleEngine → AuditRecord → 阻断/提交闭环
- 病历生成流程：前端 → MedicalRecordController → MedicalRecordService → VisitFacade → AiService.generateMedicalRecord() → TemplateConfigManager → MissingFieldDetector → MedicalRecord
- 辅助开方流程：前端 → PrescriptionAssistController → PrescriptionAssistService → AiService.prescriptionAssist() / DosageThresholdService / AllergyCheckRule → PrescriptionDraftContext → 提交端点 CRITICAL 阻断检查

**[通过]** 行为契约完整：§4 各节定义了每个 API 端点的输入/输出、超时配置、降级判定逻辑、错误响应码、前端行为指引，足以指导后续实现。

**[通过]** 模块间依赖方向合理，无循环依赖。跨模块协作通过 common-module-api 中的 Facade/Store 接口和 ApplicationEventPublisher 事件解耦，无 impl 层直接依赖。

### 5. 设计质量

**[通过]** 职责划分遵循 SRP：每条本地规则独立类（AllergyCheckRule、ContraindicationCheckRule 等各负责一种检查类型）、每个 Service 聚焦单一业务域、Converter 专注 DTO 映射。

**[通过]** 抽象层次恰当：
- 对可能变化的点引入 interface（规则引擎、门面、存储层、模板配置管理器），预留扩展点
- 对职责边界稳定的管理器使用具体 class（DialogueSessionManager、DosageThresholdService）而非过度抽象
- 迭代需求中的 4 个问题（DrugFacade 降级、§8.4 编号对齐、错误码补充、TTL 间隔定义）已在当前版本中全部解决

**[通过]** 设计便于后续详细设计和实现：接口契约清晰、各组件职责边界明确、依赖方向已固定。§10 ai-api DTO 扩展规格和 §4.6 结构化 API 契约为编码阶段提供了可直接落地的基线。

**[通过]** 设计便于单元测试：Service 依赖注入接口（AiService、Store 接口、Facade 接口、Repository）、Converter 无副作用纯映射、LocalRuleEngine 各规则独立可测、MockAiService 提供三种测试策略模式。

## 修改要求（REJECTED 时存在）

（无 — 审查通过）
