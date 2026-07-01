# OOD 设计方案审查报告（v1）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 类型形态选择与 Java 类型系统能力匹配
- 所有实体（AuditRecord、TriageRecord、TriageRule、DrugCompositionDict、DrugAllergyMapping、MedicalRecord、DrugInteractionPair）采用 JPA @Entity 继承 BaseEntity，与已有 patient 模块实体风格一致（AllergyHistory extends BaseEntity），单继承约束无冲突
- 所有业务契约接口（TriageService、PrescriptionAuditService、MedicalRecordService、PrescriptionAssistService、LocalRuleEngine、TriageRuleEngine、DepartmentFallbackProvider、PrescriptionAuditEnforcer、DoctorFacade、TemplateConfigManager、MissingFieldDetector、DosageThresholdService）均为普通 interface，实现类可自由实现多接口，Java 多接口实现能力完全覆盖
- 枚举类型（AuditRiskLevel、DosageAlertLevel、AllergySeverity、DosageUnitGroup、MedicalRecordField、AiSuggestionStatus）均为固定有限值分类，Java enum 完全支持
- DTO 类均为普通 class（含无参构造器 + getter/setter），与已有 Result、UserInfoResponse 风格一致，Java 类型系统无约束

**[通过]** 泛型使用在 Java 泛型系统能力范围内
- AiResult\<T\> 已有实现，泛型参数用于承载不同 AI 响应类型（AiResult\<TriageResponse\>、AiResult\<PrescriptionCheckResponse\> 等），Java 泛型擦除模型满足此场景
- ConcurrentHashMap\<String, DialogueSession\>、ConcurrentHashMap\<String, AiSuggestionResult\>、ConcurrentHashMap\<String, List\<DosageAlert\>\> 均为标准 Java 泛型用法

**[通过]** 抽象间继承/实现关系在约束范围内
- 所有接口实现均为单实现或多接口实现，无多重类继承
- DosageStandard 迁移至 common 模块作为独立实体，prescription 和 admin 通过 Repository 访问而非继承，无跨模块继承依赖

**[通过]** 协作关系中描述的类型交互模式可在 Java 中实现
- Converter 类负责业务层 DTO 与 ai-api 层 DTO 的映射/转换，方法签名以不同类型参数区分同名类（如 AuditConverter.toAiPrescriptionCheckRequest(AuditRequest) vs toAuditResponse(AiResult\<PrescriptionCheckResponse\>)），Java 类型系统通过不同 import 路径和方法签名区分，可行
- AllergySeverity 枚举设计为复用 patient 模块已有枚举（MILD/MODERATE/SEVERE），与已有 AllergySeverity implements BaseEnum 一致

**[轻微]** AiResult 当前实现仅含 success/data/errorCode/degraded/fallbackReason 五个字段，设计方案提及"AiResult.data 可承载部分生成结果"和"AiResult 新增 partialData 字段"两种方式，但 §10.3 MedicalRecordGenResponse 中使用 partialContent 字段承载部分结果。建议统一——要么在 AiResult 基类增加 partialData 字段供所有 AI 能力共享，要么在具体 Response DTO 内部定义，当前两种方式并存虽不阻塞但增加实现时的选择负担

### 2. 标准库与生态覆盖

**[通过]** 设计中需要的能力在标准库或常用库覆盖范围内
- JPA/Hibernate：所有实体持久化、Repository 定义、@Convert(AttributeConverter)、事务管理均在使用范围内，与已有项目 spring-boot-starter-data-jpa 依赖一致
- Jackson：JSON TEXT 序列化/反序列化（MedicalRecord.contentJson、AuditRecord.originalPrescription、TriageRule.conditions、DrugCompositionDict.ingredients），Spring Boot 默认集成 Jackson，标准使用
- Spring ApplicationEvent / @TransactionalEventListener：跨模块事件传播，Spring Framework 核心能力
- CompletableFuture：AiService 所有方法返回 CompletableFuture\<AiResult\<T\>\>，Java 标准库
- ConcurrentHashMap + ScheduledExecutorService：内存存储和 TTL 管理，Java 标准库
- Caffeine 缓存：TriageRuleEngine 和 TemplateConfigManager 使用，Spring Boot 生态常用库

**[通过]** 设计中假设的库能力合理
- @Async + CompletableFuture.runAsync() 用于包 E 异步 AI 建议，Spring 框架核心能力
- @Convert(JPA AttributeConverter)：MedicalRecord.contentJson 和 AuditRecord.originalPrescription 的 JSON TEXT 转换，JPA 标准机制
- HttpStatus 422 (UNPROCESSABLE_ENTITY)：Spring HttpStatus 枚举包含此值，可直接使用

**[通过]** 无过度自定义抽象
- DosageUnitGroup 枚举为单位换算提供分组依据，简化了通用单位转换库的需求
- AllergyDetail DTO 直接复用 patient 模块 AllergyHistory 实体的字段结构（allergen/reactionType/severity/occurredAt），结构一致

### 3. 语言特性可行性

**[通过]** 错误处理策略与 Java/Spring 能力匹配
- BusinessException + ErrorCode + GlobalExceptionHandler 体系完全复用已有框架（已验证 ErrorCode 接口、BusinessException 类、GlobalExceptionHandler 类均存在且功能完整）
- BLOCK 阻断不经过异常框架，Controller 直接返回 ResponseEntity.status(422)，与已有 Result\<T\> 响应模式兼容——Section 4.2 使用 BlockResponse 包装，HTTP 422 状态码在 Spring 中可用
- 模块级错误码体系（TRIAGE_、RX_AUDIT_、MR_、RX_ASSIST_ 前缀）与 ErrorCode 接口的 getCode()/getMessage() 契约一致，可枚举实现

**[通过]** 并发设计与 Java 并发模型兼容
- ConcurrentHashMap：DialogueSessionManager、AiSuggestionResult 存储、PrescriptionDraftContext 均使用，线程安全性由 ConcurrentHashMap.compute() 保证
- CompletableFuture：AiService 所有方法返回类型，Service 层同步等待（.join() 或 .get()），Java 并发模型完全支持
- @Async + CompletableFuture.runAsync()：异步 AI 建议调用，Spring @Async 和 Java 线程池均可用
- 部署约束已明确：Phase 2/3 假设单实例/sticky session，Phase 5 迁移分布式缓存

**[通过]** 资源管理方案在 Java/Spring 管理模式内可行
- JPA 实体生命周期由 Hibernate EntityManager 管理，与已有项目持久化层一致
- ScheduledExecutorService 用于 TTL 清理（DialogueSession 30 分钟、AiSuggestionResult 30 分钟、PrescriptionDraftContext 60 分钟），Java 标准资源管理
- Caffeine refreshAfterWrite 缓存刷新由 Caffeine 内部调度器管理，Spring Bean 生命周期内自动关闭

**[通过]** 模块/包结构设计符合 Maven + Spring Boot 项目组织方式
- 三个新模块为扁平 Maven 模块（不拆分 api/impl），与 patient、doctor、admin 模块结构一致
- 各模块内按职责分包（api/service/repository/entity/dto/converter/rule/fallback/template/parser/context/dialogue），组织清晰
- DosageStandard 迁移至 common 模块，解决跨模块依赖，与 Maven 依赖规则一致
- DoctorFacade 定义在 common-module-api 中，与 UserFacade 位置和模式一致

### 4. 设计一致性

**[通过]** 各抽象的职责描述清晰无歧义
- 每个 Service interface 的"职责"段落均明确描述了业务边界、协作对象和降级路径
- DTO 类均明确对齐了需求文档 3.4.x 的输入/输出契约
- Converter 类的映射方向和转换逻辑在 §4.5 逐一定义，包含映射方法签名

**[通过]** 协作关系形成闭环，无缺失环节
- 分诊：TriageController → TriageService → AiService.triage() / TriageRuleEngine.match() / DepartmentFallbackProvider → DoctorFacade → TriageRecord 持久化，全链路闭环
- 处方审核：PrescriptionAuditController → PrescriptionAuditService → AiService.prescriptionCheck() / LocalRuleEngine → AuditRecord 持久化 → PrescriptionAuditEnforcer(BLOCK) / 处方提交端点(WARN 强制提交) → 闭环
- 病历生成：MedicalRecordController → MedicalRecordService → AiService.generateMedicalRecord() / MissingFieldDetector → MedicalRecord 持久化，闭环
- 辅助开方：PrescriptionAssistController → PrescriptionAssistService → AiService.prescriptionAssist() / DosageThresholdService → PrescriptionDraftContext → 处方提交校验，闭环
- 处方提交端点 POST /api/prescription/submit 已补充简要契约（§4.2），含 forceSubmit + auditRecordId + 版本校验，WARN→强制提交→处方落单端到端闭环完整

**[通过]** 行为契约描述完整到足以指导后续实现
- §4.1-4.4 四个场景的行为契约包含完整的请求/响应结构、降级路径、错误处理和边界条件
- §4.5 Converter 映射机制定义了各模块的映射方向和转换方法签名
- §10 ai-api 层 DTO 扩展规格列出了所有 DTO 的完整字段集

**[通过]** 模块间依赖方向合理，无循环依赖
- 三个新模块均单向依赖 common、common-module-api 和 ai-api
- 三个新模块之间无互相依赖，跨模块协作通过 DoctorFacade（common-module-api）解耦
- application 模块聚合所有模块，Spring 自动注入门面实现
- DosageStandard 位于 common 模块，prescription 只读、admin 写入，单向依赖

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则
- TriageService 封装分诊业务，DialogueSessionManager 封装会话管理，TriageRuleEngine 封装规则匹配，DepartmentFallbackProvider 封装兜底逻辑——各司其职
- PrescriptionAuditService 封装审核流程，LocalRuleEngine 封装规则链，PrescriptionAuditEnforcer 封装阻断行为——审核与阻断分离
- PrescriptionAssistService 封装辅助开方，DosageThresholdService 封装剂量阈值校验——业务与校验分离
- MedicalRecordService 封装病历生成，TemplateConfigManager 封装模板管理，MissingFieldDetector 封装缺失检测——生成/配置/校验分离

**[通过]** 抽象层次恰当
- interface 用于存在多实现可能的契约（TriageService、TriageRuleEngine、LocalRuleEngine、PrescriptionAuditEnforcer、DoctorFacade、TemplateConfigManager），class 用于职责稳定的实现（DialogueSessionManager、DosageThresholdService）
- DTO 类无接口抽象，避免过度设计
- Converter 类为具体实现类，无接口抽象，符合映射器通常不需要多实现的判断

**[通过]** 设计便于后续详细设计和实现
- 目录结构（§2.1）已定义到文件级，为实现提供直接指导
- ai-api 层 DTO 字段扩展规格（§10）为 DTO 扩展提供完整字段列表
- 种子数据脚本路径（§8.1）为数据初始化提供具体方案
- 错误码前缀分类（§5.1）为实现错误码枚举提供框架

**[通过]** 设计便于单元测试
- 所有业务契约均为 interface，可通过 mock 实现隔离测试
- LocalRuleEngine 每条规则独立实现/测试/启用/禁用，满足可隔离性
- Converter 类为纯映射逻辑，可独立测试
- DoctorFacade 在 common-module-api 中定义，consultation 模块测试可 mock DoctorFacade
- DosageThresholdService 为 class，可通过注入 mock DosageStandardRepository 隔离测试
- DialogueSessionManager 为 class 且内部使用 ConcurrentHashMap，可独立测试并发语义

**[轻微]** TriageConverter.toTriageResponse() 接收 AiResult\<ai-api TriageResponse\> 和 List\<RecommendedDoctor\> 两个参数——其中 List\<RecommendedDoctor\> 来自 DoctorFacade 调用结果。建议在 Converter 方法签名中明确参数来源注释，避免实现时误解为 ai-api 层返回。此为轻微问题不阻塞通过

**[轻微]** §4.2 处方提交端点 POST /api/prescription/submit 的契约为"简要定义"，未指定对应 Controller 类名和包路径。按目录结构（§2.1）推断应在 prescription/api/ 下新增 SubmitController 或追加到现有 Controller，但设计未显式说明。此为细微遗漏不阻塞通过——实现阶段可由编码者根据端点语义自行归类

## 修改要求

无严重或一般问题，无需修改。
