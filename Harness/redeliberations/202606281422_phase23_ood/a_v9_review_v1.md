# OOD 设计方案审查报告（v1）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 类型形态选择——所有类型形态选择与 Java 类型系统能力匹配：JPA @Entity 用于持久化实体（AuditRecord、TriageRecord、MedicalRecord、DrugContraindicationMapping 等）、interface 用于业务契约（TriageService、PrescriptionAuditService、LocalRuleEngine 等）、class 用于 DTO 和具体实现、enum 用于固定分类（AuditRiskLevel、AlertSeverity、DosageAlertLevel 等）。sealed class 未使用但当前设计不需要密封继承层次，选择合理。

**[通过]** 继承和实现关系——Java 单继承 + 多接口实现约束下，所有抽象均通过 interface 定义契约、class 实现接口，未出现类间继承链。LocalRuleEngine 的多种规则实现（AllergyCheckRule、ContraindicationCheckRule、DosageLimitRule 等）均为独立 class 实现，无继承冲突。

**[通过]** 泛型使用——AiResult<T> 是唯一的泛型抽象，使用方式简单（泛型参数承载具体 AI 响应类型），在 Java 泛型擦除模型下完全可行。ConcurrentHashMap<String, List<DosageAlert>> 等嵌套泛型使用也是标准模式。

**[通过]** 协作关系中的类型交互——AiService 返回 CompletableFuture<AiResult<T>>，Service 层同步等待 .join()，符合 Java CompletableFuture 模式。Converter 类负责异构 DTO 映射，类型安全由编译期保证。

**[轻微]** AiResult.failure(String errorCode, T partialData) 和 AiResult.degraded(String fallbackReason, T partialData) 新增重载的设计决策描述中，同时提到了"由 AI 实现直接构造 AiResult(success=false, partialData, errorCode, degraded=false, fallbackReason=null)"的替代方案。当前 AiResult 所有字段均有 setter（源码确认），直接构造 + setter 在 Java 中可行，但两种途径并存可能让实现者困惑。建议在实现阶段统一选择工厂方法重载路径，避免直接构造器的绕路。

### 2. 标准库与生态覆盖

**[通过]** 集合与并发——ConcurrentHashMap、ScheduledExecutorService、CompletableFuture 均为 JDK 标准库能力，设计中使用方式与 Java 并发模型匹配。Caffeine 缓存库（TriageRuleEngine 规则缓存、TemplateConfigManager 模板缓存）是 Spring Boot 生态常用库，引入合理。

**[通过]** JPA 持久化——所有 @Entity 均使用标准 JPA 注解。JSON TEXT 字段通过 JPA @Convert（AttributeConverter）+ Jackson 序列化，是成熟的 Hibernate/JPA 模式（对应 MedicalRecord.contentJson、AuditRecord.originalPrescription、DrugContraindicationMapping.contraindications 等）。

**[通过]** Spring 事件机制——@TransactionalEventListener(AFTER_COMMIT) 和 ApplicationEventPublisher 跨模块传播均为 Spring Framework 标准能力，在 application 模块聚合后可实现跨模块事件传播，设计假设成立。

**[通过]** UUID 生成——DialogueSessionManager 生成 sessionId 采用 UUID v4，Java 标准 UUID.randomUUID() 满足需求。前端首轮传入 sessionId 也是可行的。

**[通过]** 无可简化的自定义抽象——DosageUnitGroup 枚举覆盖 MASS/VOLUME/IU 三组，符合药品剂量标准分类需求，标准库无直接对应能力。MedicalRecordField 枚举对齐需求文档输出契约，自定义合理。

### 3. 语言特性可行性

**[通过]** 错误处理——复用 GlobalExceptionHandler + ErrorCode 接口 + BusinessException 体系（已存在于 common 模块），BLOCK 阻断通过 HTTP 422 + BlockResponse 正交处理。各错误码按前缀+类型分类，AI 相关含 _AI_ 中段，与需求文档命名约定对齐。AiResult 承载降级/失败语义时不走异常框架，而是作为正常返回值，与 Java 异常处理模型兼容。

**[通过]** 并发设计——ConcurrentHashMap 用于 dialogue session / AiSuggestionResult / PrescriptionDraftContext 三项内存存储，Phase 2/3 单实例/sticky session 假设下可行。ConcurrentHashMap.compute() 原子操作保证 AiSuggestionResult 并发安全。TTL 清理竞态通过 ConcurrentHashMap.remove() 原子性覆盖。

**[通过]** 资源管理——TTL 30 分钟的 ScheduledExecutorService 定期清理 session，AiSuggestionResult 同样有 TTL 30 分钟 + 定期清理，PrescriptionDraftContext 有 60 分钟 TTL。均为内存级资源管理，无外部资源（文件句柄、网络连接等）需要显式释放，与 Java 资源管理模式兼容。

**[通过]** 模块/包结构——三个新模块均采用扁平 Maven 模块，内部按 api / service / repository / entity / dto / converter / rule / dialogue / fallback / template / parser / context / event 分包，职责清晰符合 Java 项目组织方式。ai-api 层 DTO 与业务层 DTO 通过包名区分（com.aimedical.modules.ai.api.dto.prescription vs com.aimedical.modules.prescription.dto.audit/assist），同名不同包在 Java 中合法。

**[轻微]** §4.4 check-dose 中预创建→更新模式（PENDING → COMPLETED/FAILED）依赖 ConcurrentHashMap.compute() 原子操作，但 ScheduledExecutorService 清理过期条目和业务写入的时序需注意——清理线程和业务线程可能同时操作同一 taskId。当前设计 AiSuggestionResult TTL 30 分钟后清理，若业务线程恰好在此窗口更新结果，存在清理先于更新完成的可能。建议在清理逻辑中检查 status != PENDING 再删除，或采用 computeIfPresent 保护。

### 4. 设计一致性

**[通过]** 职责描述清晰——各抽象的职责定位在 §1.3 核心抽象一览表中逐条定义，无歧义。AllergyCheckRule 与 ContraindicationCheckCheckRule 的职责拆分（§3.2 检查项 #2）明确——前者负责过敏史冲突，后者负责合并症禁忌，两者独立产出 LocalRuleResult。

**[通过]** 协作关系闭环——分诊降级链（AI → TriageRuleEngine → DepartmentFallbackProvider）、处方审核降级链（AI → LocalRuleEngine 5 条规则）、病历生成降级链（AI → 部分保留 → 空字段+缺失提示）均形成闭环。辅助开方与处方审核同模块强耦合通过 PrescriptionAssistServiceImpl 直接注入 PrescriptionAuditService 实现，无循环依赖。

**[通过]** 行为契约完整性——处方提交端点（§4.2 POST /api/prescription/submit）覆盖了 forceSubmit + auditRecordId + 处方版本校验完整的闭环链路。WARN 级强制提交的后端校验逻辑（AuditRecord.riskLevel=WARN + isLatest=true + 处方版本一致性）完整。BLOCK 阻断通过 PrescriptionAuditEnforcer + HTTP 422 实现闭环。

**[通过]** 模块间依赖方向——三个新模块均依赖 common + common-module-api + ai-api，不依赖其他业务模块 impl 层。跨模块协作通过接口（DoctorFacade、UserFacade）和事件（RegistrationEvent）解耦，依赖方向合理，无循环依赖。

**[通过]** RegistrationEvent 事件契约——定义在 common-module-api 中，包含 registrationId / patientId / departmentId / doctorId / eventTime 五个字段，发布端为 registration 模块，消费端为 consultation 模块，通过 Spring ApplicationEvent 跨模块传播，契约完整。

**[通过]** AiResult 超时降级路径——v9 明确选择使用现有 AiResult.data 字段承载部分结果（删除"新增 partialData"歧义），通过新增 failure(String errorCode, T partialData) 和 degraded(String fallbackReason, T partialData) 两条重载路径覆盖超时（failure+errorCode+partialData）和降级（degraded+data+fallbackReason）两种场景。对需求文档 §3.4.3 的契约响应：非流式超时响应需同时携带 errorCode（MR_GEN_AI_TIMEOUT）和 partial_content，通过 failure 重载可实现 data=partialContent + errorCode=MR_GEN_AI_TIMEOUT 组合，可行。

### 5. 设计质量

**[通过]** 单一职责——AllergyCheckRule 与 ContraindicationCheckRule 职责拆分遵循 SRP，两者独立实现/测试/启用/禁用。DosageThresholdService 封装剂量阈值校验职责，与 PrescriptionAssistService 的 AI 辅助开方职责分离。DialogueSessionManager 集中管理会话生命周期和并发控制，TriageService 聚焦分诊业务逻辑。

**[通过]** 抽象层次恰当——设计为架构级 OOD，未包含完整的方法签名和字段类型定义，但核心抽象的职责、协作关系和行为契约足以指导后续详细设计和实现。Converter 类封装 DTO 映射逻辑、PrescriptionAuditEnforcer 封装阻断策略、TemplateConfigManager 封装模板管理均为恰当的抽象粒度。

**[通过]** 便于后续实现——各 Service interface 的契约定义明确了正常路径和降级路径，Controller 端点的输入/输出契约对齐需求文档 3.4.x，错误码表齐全（含命名规则），ai-api 层 DTO 扩展规格（§10）完整列出了各 DTO 需扩展的字段集。实现者可据此直接编写代码。

**[通过]** 便于单元测试——TriageService / PrescriptionAuditService / MedicalRecordService 均为 interface，可 mock 替换实现。DoctorFacade / UserFacade 为 interface 定义在 common-module-api 中，可在测试中 mock。LocalRuleEngine 为 interface，各规则类独立实现/测试。AiService 为 interface，mock 可控。

**[通过]** TriageRecord 推荐医生快照——v9 新增 recommendedDoctors 字段（JSON TEXT），存储 TriageResponse.doctors 列表快照（含 doctorId / doctorName / departmentId / availableSlotCount / score），对齐需求文档 §5.1 分诊记录核心字段"推荐医生"。

**[通过]** AuditAlert.severity 类型——v9 新增独立 AlertSeverity 枚举（INFO / WARNING / CRITICAL），与 AuditRiskLevel（PASS / WARN / BLOCK）是不同维度，避免混淆。

**[通过]** 错误码表完整性——v9 §5.1 错误码表补齐了需求文档 3.4.x 明确定义的全部 AI 能力错误码（RX_ASSIST_AI_NO_RECOMMENDATION、RX_AUDIT_AI_INPUT_INVALID、MR_GEN_AI_INPUT_INVALID、MR_GEN_AI_OUTPUT_INCOMPLETE 等），并增加了 AI/非 AI 分类命名规则。

**[通过]** DosageAlert 错误码字段——v9 DosageAlert 新增 errorCode 可选字段，统一承载 RX_ASSIST_DOSE_STANDARD_NOT_FOUND 等错误码。

**[通过]** 处方版本校验一致性语义——v9 定义了"按业务字段做结构化比较（drugId + dose + frequency + duration + route 五字段组合比对）"的一致性语义，忽略 JSON 文本级格式差异，可行且合理。

**[通过]** allergyHistory/allergyDetails 数据来源优先级——v9 按需求文档 §3.1.6 过渡方案三层行为定义了统一的数据来源优先级，allergyHistory 后端拼接为 single source of truth，allergyDetails 后端优先提取、前端传入值仅作为 fallback。

**[通过]** 辅助开方过敏告警与处方审核过敏检查关系——v9 明确二者独立执行不互斥，allergyWarnings.severity=HIGH 不直接等价于 AuditRiskLevel=BLOCK，分属不同时机和维度。

**[轻微]** DrugContraindicationMapping.contraindications JSON 中每项含 level（ABSOLUTE_CONTRAINDICATION / RELATIVE_CONTRAINDICATION），但此 level 枚举值域未定义为独立枚举类型（如 ContraindicationLevel），仅作为 JSON 文本中的字符串值。实现时建议将其提取为枚举以保证类型安全，但不阻塞当前架构设计。

**[轻微]** AiSuggestionResult.partialData 字段类型为"可选，JSON 文本"（String），在 v8 AiResult 部分已决定使用 data 字段承载，但 AiSuggestionResult 仍保留了独立的 partialData 字段。二者属于不同抽象（AiResult 是 AI 调用通用结果，AiSuggestionResult 是辅助开方领域的异步结果容器），字段命名不同是合理的，但建议在 §7 设计决策中补充说明 AiSuggestionResult.partialData 与 AiResult.data 的选择差异（AiSuggestionResult 不走 AiResult 路径，它有独立的字段设计）。
