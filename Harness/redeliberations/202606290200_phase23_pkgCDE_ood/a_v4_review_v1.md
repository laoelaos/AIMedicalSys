# OOD 设计方案审查报告（v4）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 设计中的类型形态选择（interface / class / enum / JPA @Entity）与 Java 类型系统能力完全匹配。interface 用于 Service、RuleEngine、FallbackProvider、Facade、Enforcer 等抽象边界，class 用于 Controller、Manager、DTO、具体 Service 实现，enum 用于风险等级、告警级别、字段标识等有限分类，JPA @Entity 用于持久化实体——各选择恰当且与 Spring 框架的惯用模式一致。

**[通过]** 抽象之间的继承和实现关系遵循 Java 单继承 + 多接口实现约束。Service interface + Impl class 模式是 Spring 标准实践，无多继承需求。

**[通过]** 泛型使用方式（`CompletableFuture<AiResult<T>>`、各 AiService 方法泛型绑定四个 ai-api Response DTO）在 Java 泛型系统能力范围内。

**[通过]** 协作关系中的类型交互模式（Controller→Service→Repository、Service→AiService interface、跨模块门面接口 DoctorFacade/DrugFacade）均为 Java/Spring 中可实现的模式。

### 2. 标准库与生态覆盖

**[通过]** 设计所需的能力均在 Java 标准库或 Spring 生态的覆盖范围内：ConcurrentHashMap 用于内存存储、CompletableFuture 用于异步 AI 调用、Spring Data JPA 用于持久化、Spring ApplicationEvent 用于跨模块事件传递、Spring Retry 用于消费失败重试、Caffeine 用于规则/模板缓存、ScheduledExecutorService 用于 TTL 清理、Jackson 用于 JSON 序列化。

**[通过]** 设计假设的库能力（如 `@TransactionalEventListener(phase=AFTER_COMMIT)`、`@Retryable`、Caffeine `refreshAfterWrite`）均为 Spring/Caffeine 生态中成熟可用的特性，假设合理。

**[通过]** 设计中的自定义抽象（Store 接口隔离层、Converter 映射层）有合理的设计理由，没有标准库能力可以显著简化这些抽象。

### 3. 语言特性可行性

**[通过]** 错误处理策略采用 Spring GlobalExceptionHandler + BusinessException + ErrorCode 接口体系，与项目已有异常处理框架一致。BLOCK 阻断使用 HTTP 422 + BlockResponse 与业务异常体系正交——此设计在 Java/Spring 中可实现。

**[通过]** 并发设计使用 ConcurrentHashMap、CompletableFuture、@Async、ScheduledExecutorService——均为 Java/Spring 标准的并发模型。单实例/sticky session 假设明确标注，Phase 5 迁移分布式缓存的路径已规划。

**[通过]** 资源管理方案（TTL 清理通过 ScheduledExecutorService 定时扫描、Spring 管理的 @Component 单例 Bean 生命周期）在 Java 资源管理模式内可行。

**[通过]** 模块/包结构遵循 Maven 多模块 + 标准 Java 包命名约定（com.aimedical.modules.xxx），目录结构清晰。

### 4. 设计一致性

**[通过]** 各抽象职责描述清晰无歧义——Controller 负责 REST 端点、Service 负责业务编排、Repository 负责数据访问、Converter 负责 DTO 映射、各规则类负责独立校验逻辑。

**[通过]** 协作关系形成闭环：分诊（前端→TriageController→TriageService→AiService/TriageRuleEngine/DepartmentFallbackProvider→TriageRecord）、处方审核（前端→PrescriptionAuditController→PrescriptionAuditService→AiService/LocalRuleEngine→AuditRecord）、病历生成（前端→MedicalRecordController→MedicalRecordService→AiService/TemplateConfigManager/MissingFieldDetector→MedicalRecord）、辅助开方（前端→PrescriptionAssistController→PrescriptionAssistService→AiService/DosageThresholdService+AllergyCheckRule→AiSuggestionResult/PrescriptionDraftContext）。跨模块协作通过 DoctorFacade、DrugFacade、RegistrationEvent 解耦。

**[通过]** 行为契约描述完整——§4 详细定义了 4 个场景的 API 端点、请求/响应格式、降级路径、异常处理。§8 定义了剂量标准匹配六级优先级和单位校验规则。§9 定义了模板和规则管理接口契约。

**[通过]** 模块间依赖方向合理：三个新模块仅依赖 common、common-module-api、ai-api，模块间无循环依赖（consultation / prescription / medical-record 互相不依赖）。跨模块协作通过门面接口或事件解耦。

**[通过]** 原型需求迭代中的所有 9 个问题均已在本版设计中闭合：Store 抽象层升级为设计强制项（§1.1/§6.1）、check-dose 去重/节流策略已定义（§3.4/§6.3）、admin 依赖时间线已标注（§1.1a）、AiResult.data=null 契约已约束（§2.3）、撤销审核端点已定义（§3.2/§4.2）、prescriptionId 传递路径已闭环（§1.3/§3.4）、AI Mock 契约已补充（§2.3）、格式问题已修复（§4.5）、unit 枚举约束已补充（§8.4）。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则：Controller 仅处理 HTTP 映射、Service 仅处理业务编排、Converter 仅处理 DTO 映射、规则类仅处理单一校验逻辑、Manager 仅处理生命周期管理。

**[通过]** 抽象层次恰当——AiService 接口抽象 AI 调用、LocalRuleEngine 接口抽象本地规则链、Store 接口抽象存储实现、DoctorFacade 接口抽象跨模块查询。未过度设计（如 DialogueSessionManager 直接使用 class 而非 interface，理由充分）。未设计不足（核心业务边界均定义了接口）。

**[通过]** 设计便于详细设计和实现——目录结构已列出文件级粒度，抽象职责和协作关系已明确定义，Converter 映射方向已指定。§10 列出了 ai-api DTO 扩展的完整字段规格，可直接指导编码。

**[通过]** 设计便于单元测试——Service 依赖接口（可 mock AiService、DoctorFacade、Repository 等），MockAiService 提供三种测试模式（STATIC/AI_UNAVAILABLE/TIMEOUT），规则类独立实现/测试。

## 修改要求（无）

无严重或一般问题。
