# OOD 设计方案审查报告（v5）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 设计中的类型形态选择（interface / class / JPA @Entity / enum / DTO）与 Java 类型系统完全匹配。interface 用于定义业务契约（TriageService、PrescriptionAuditService、MissingFieldDetector 等）和跨模块门面（DoctorFacade、DrugFacade、VisitFacade）——Spring 依赖注入天然适配。JPA @Entity 用于持久化实体（TriageRecord、AuditRecord、MedicalRecord 等），@Id @GeneratedValue 主键策略和 @Version 乐观锁均为标准 JPA 能力。enum 用于有限分类（AuditRiskLevel、AlertSeverity、MedicalRecordField 等）。DTO 用作请求/响应值对象。泛型使用方式（`CompletableFuture<AiResult<T>>`、泛型 T 绑定到各 ai-api Response DTO）均在 Java 泛型系统能力范围内。单继承 + 多接口实现约束被严格遵守——实体不继承其他业务实体，interface 可被多实现。类型系统层面无阻塞。

### 2. 标准库与生态覆盖

**[通过]** 所有设计所需能力均在 Java/Spring 生态的标准覆盖范围内：
- JPA/Hibernate：持久化、乐观锁（@Version）、仓库模式
- Spring Boot：DI、事务（@Transactional）、事件（ApplicationEventPublisher、@TransactionalEventListener）、重试（@Retryable + @Recover）
- ConcurrentHashMap + ScheduledExecutorService：内存存储 + TTL 清理
- Caffeine：定时缓存刷新
- Jackson + JPA @Convert：单列 JSON TEXT 序列化/反序列化
- CompletableFuture：异步 AI 调用 + 超时控制
- GlobalExceptionHandler + BusinessException：统一异常处理框架
MockAiService 通过 @Profile("mock") 条件激活，Spring 原生支持。三项 Store 抽象层（SessionStore、SuggestionStore、DraftContextStore）的 ConcurrentHashMapStore → RedisStore 迁移路径清晰，生态覆盖完备。

### 3. 语言特性可行性

**[通过]** 错误处理策略与 Java 异常体系匹配——业务异常通过 BusinessException 体系 + GlobalExceptionHandler 框架处理，BLOCK 阻断通过 HTTP 422 + BlockResponse 与异常体系正交。并发设计覆盖三种模式：(a) ConcurrentHashMap + 原子操作（compute、remove）处理会话级并发，(b) CompletableFuture 处理 AI 调用异步化，(c) 去重策略 + 前端防抖处理高频 check-dose 调用。资源管理方案可行——内存 TTL 清理由 ScheduledExecutorService 定期扫描，JPA 事务由 Spring @Transactional 管理。模块/包结构按 Maven 模块 + 内部按职责分包（api/service/repository/entity/dto/converter），与项目现有风格一致。

**[轻微]** 三项 Store 接口（SessionStore、SuggestionStore、DraftContextStore）在 v23 中被提升为设计强制项，但核心抽象一览表和目录结构中未显式列出——建议在后续迭代中将三个 Store 接口补入 §1.3 核心抽象一览表和 consultation/prescription/medical-record 模块的目录结构中，使抽象层的强制约束在交付物层面可见。

### 4. 设计一致性

**[通过]** 本版已完整闭合上一轮审查发现的 5 个问题：
- **问题1 (sessionId 跨模块传播)**：§2.2 明确定义了前端侧传递路径——分诊结束后前端保留 sessionId，进入挂号流程作为参数传入 registration 模块，registration 模块填充至 RegistrationEvent。降级路径（前端未传时按 patientId 关联）同步定义。架构路径完全闭合。
- **问题2 (VisitFacade 降级)**：§3.3 补充了完整降级策略——超时阈值 2s、双路径降级（encounterId fallback / MR_GEN_VISIT_NOT_FOUND + 部分内容）、WARN 级日志。与 DoctorFacade 对称设计实现。
- **问题3 (FieldMissingHint 生成规则)**：§3.3 MissingFieldDetector 新增了生成策略段落——从 DepartmentTemplateConfig 读取预定义模板（promptMessage + suggestedAction），未配置时使用默认文案。内容来源和管理员配置路径完整定义。
- **问题4 (错误码遗漏)**：§5.1 错误码表已补充 TRIAGE_SESSION_NOT_FOUND、MR_GEN_VISIT_NOT_FOUND、MR_GEN_CONCURRENT_MODIFICATION、RX_ASSIST_UNIT_MISMATCH，涵盖 AI 和非 AI 错误码全部分类。
- **问题5 (MedicalRecord.contentJson 并发写)**：§3.3 MedicalRecord 实体增加了 @Version 乐观锁字段，更新操作使用版本号校验，写冲突返回 MR_GEN_CONCURRENT_MODIFICATION 错误码。

各抽象的职责描述清晰无歧义，协作关系形成闭环（TriageService → DialogueSessionManager → TriageRuleEngine → DepartmentFallbackProvider，PrescriptionAuditService → LocalRuleEngine → 各规则类），行为契约完整到足以指导后续实现。模块间依赖方向合理（consultation/prescription/medical-record → common + common-module-api + ai-api，模块间无互相依赖），无循环依赖。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则——各 interface 职责边界清晰（TriageService 专注分诊业务、DoctorFacade 专注医生查询、LocalRuleEngine 专注规则校验）。抽象层次恰当——interface 定义业务契约，class 提供具体实现（TriageServiceImpl、DefaultTriageRuleEngine 等），JPA @Entity 聚焦持久化，DTO 聚焦数据传输。设计便于后续详细设计和实现——每个模块的目录结构、职责分工、协作关系均已明确定义。设计便于单元测试——interface 驱动的设计使各组件可独立 Mock 和隔离测试（MockAiService 提供了完整的 AI Mock 支持，各规则类独立实现/测试）。

**[轻微]** §6.1 提出 Phase 2/3 部署约束为单实例或 sticky session，三项内存存储在 Phase 5 才迁移分布式缓存。若 Phase 2/3 上线初期即面临多实例部署需求（如 HA 要求），此约束会成为部署瓶颈——建议在 Phase 2/3 实现阶段预留 Store 接口的 Redis 轻量实现选项（如通过 @Profile("redis") 切换），使架构在多实例场景下具备弹性，不强制等待 Phase 5。

## 修改要求（REJECTED 时存在）

无

