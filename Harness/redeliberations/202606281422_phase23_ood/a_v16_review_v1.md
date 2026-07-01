# OOD 设计方案审查报告（v16）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 设计中的类型形态选择（interface、class、enum、JPA @Entity、DTO）均与 Java 17 类型系统能力完全匹配：单继承 + 多接口实现约束被正确遵循；泛型使用方式（CompletableFuture<AiResult<T>>、泛型 T 绑定各业务 Response DTO）在 Java 泛型系统能力范围内；enum 用于固定分类（AuditRiskLevel、AlertSeverity、DosageAlertLevel 等）符合最佳实践；协作关系中的类型交互模式（ConcurrentHashMap 存储、CompletableFuture 异步回调、interface 依赖注入）均可直接实现。

**[轻微]** DTO 当前使用可变 class 而非 Java 16+ record，虽不影响可行性，但采用 record 可减少样板代码（equals/hashCode/toString、不可变性保障）。建议后续实现时考虑将纯数据载体（如 RecommendedDepartment、MatchedRule、DoseWarning 等）逐步迁移为 record。

### 2. 标准库与生态覆盖

**[通过]** Spring Boot 3.2.5、Spring Data JPA、Spring Web MVC、Spring Cache（Caffeine）、Jackson、H2 等现有依赖完整覆盖设计所需能力。CompletableFuture/ScheduledExecutorService/ConcurrentHashMap 来自 JDK 标准库。@TransactionalEventListener(phase=AFTER_COMMIT) 在 Spring 6.x（Boot 3.x）中完全支持。AiService 接口的 4 个相关方法（triage/prescriptionCheck/generateMedicalRecord/prescriptionAssist）已在 ai-api 模块中存在，AiResult 当前为空壳类，需按 §10 规格扩展。

**[轻微]** @Retryable（RegistrationEventListener 使用）依赖 spring-retry，该依赖不在 spring-boot-starter-parent 的默认依赖管理中。需在 pom.xml 中显式添加 `spring-retry` 依赖并启用 `@EnableRetry`。此为常见 Spring 生态扩展，添加成本低，建议在实现阶段补充。

### 3. 语言特性可行性

**[通过]** 错误处理策略（BusinessException + GlobalExceptionHandler 常规路径、HTTP 422 + BlockResponse 阻断路径）与 Java/Spring 异常处理能力匹配。并发设计（CompletableFuture 异步 AI 调用、ConcurrentHashMap 有状态会话存储、@Async 异步建议、ScheduledExecutorService TTL 清理）均与 Java 并发模型兼容。资源管理（Spring IOC 管理 Bean 生命周期、JPA EntityManager 管理持久化资源）在 Spring Boot 框架内可行。模块/包结构（扁平 Maven 模块 + api/service/repository/entity/dto/converter 分包）与项目已有组织方式一致。

**[轻微]** §6.1 明确标注 Phase 2/3 假设单实例或 sticky session，三项 ConcurrentHashMap 存储（DialogueSessionManager、AiSuggestionResult、PrescriptionDraftContext）在水平扩展时需迁移至分布式缓存。此约束已在设计文档中显式声明并在 Phase 5 迁移路线图中覆盖，建议在模块 README 或架构决策记录（ADR）中补充该约束以防止后续部署时的误判。

### 4. 设计一致性

**[通过]** 各抽象职责描述清晰无歧义：Controller（REST 端点）、Service（业务契约）、Repository（数据访问）、Entity（持久化模型）、DTO（传输对象）、Converter（DTO 映射），分工明确。协作关系形成闭环——TriageService 调用链完整（AiService → TriageRuleEngine → DepartmentFallbackProvider），PrescriptionAuditService 调用链完整（AiService → LocalRuleEngine → 6 条独立规则 + PrescriptionAuditEnforcer）。行为契约描述充分：4 个业务场景（§4.1–§4.4）均包含正常路径、降级路径、边界场景处理。模块间依赖方向清晰且无循环依赖：三个新模块仅依赖 common / common-module-api / ai-api，互不依赖；跨模块协作通过 DoctorFacade 门面接口和 RegistrationEvent 事件解耦。

### 5. 设计质量

**[通过]** 职责划分遵循 SRP——各抽象聚焦单一职责，独立规则类（DosageLimitRule、AllergyCheckRule 等）可独立实现/测试/启用/禁用。抽象层次恰当：对可能存在多种实现的抽象使用 interface（TriageService、TriageRuleEngine、DepartmentFallbackProvider、LocalRuleEngine、PrescriptionAuditEnforcer），对职责边界稳定的使用 class（DialogueSessionManager、DosageThresholdService）。设计便于后续实现和测试——interface 允许 mock，独立规则类可直接单元测试。与 Phase 0/1 已有模块（patient、doctor、admin）的模块结构、分包风格、依赖方向保持一致。

## 修改要求（无）

本设计无严重或一般问题。上述轻微改进建议不阻塞通过。
