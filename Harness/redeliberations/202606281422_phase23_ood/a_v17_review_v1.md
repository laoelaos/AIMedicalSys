# OOD 设计方案审查报告（v17）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 设计中所有类型形态选择（interface / class / enum / JPA @Entity）均与 Java 类型系统能力匹配。

- interface 用于服务契约（TriageService、PrescriptionAuditService、LocalRuleEngine 等），支持多态和 mock 测试
- class 用于可变状态对象（DialogueSession、PrescriptionDraftContext）和 DTO，符合 Java 对象模型
- enum 用于固定分类（AuditRiskLevel、AlertSeverity、DosageAlertLevel 等），Java enum 的字段和方法扩展能力可满足设计需求
- JPA @Entity 用于持久化实体（TriageRecord、AuditRecord、MedicalRecord 等），完全兼容 JPA 规范
- 泛型 `AiResult<T>` + `CompletableFuture<AiResult<T>>` 的使用方式在 Java 泛型系统能力范围内，各方法 T 绑定为不同的 ai-api Response DTO 类型
- 单继承 + 多接口实现约束被正确遵守

**[通过]** 抽象的继承和实现关系在 Java 约束范围内——PrescriptionAuditEnforcer 为 interface + impl 模式，各规则类独立实现 LocalRuleEngine 链。

**[通过]** 泛型抽象 `AiResult<T>` 的泛型使用方式（作为方法返回类型的类型参数嵌套 `CompletableFuture<AiResult<T>>`）在 Java 泛型系统能力范围内。

**[通过]** 协作关系中描述的类型交互模式（interface 注入、事件发布/监听、Converter 双向映射）均可在 Java/Spring 中实现。

### 2. 标准库与生态覆盖

**[通过]** 设计中需要的能力均在 Java 标准库或 Spring 生态常用库的覆盖范围内：

- 并发容器：`ConcurrentHashMap`（标准库）、`CompletableFuture`（标准库）、`ScheduledExecutorService`（标准库）
- 持久化：JPA/Hibernate（Spring Boot 默认集成）
- 序列化：Jackson（Spring Boot 默认集成）
- 缓存：Caffeine（Spring Boot 生态常用）
- 异步/重试：`@Async`、`@Retryable`（Spring 框架）
- 事件驱动：Spring `ApplicationEvent` + `@TransactionalEventListener`
- 模块管理：Maven 多模块结构
- JSON 文本存储：JPA `@Convert` + Jackson 属性转换器

**[通过]** 设计中假设的库能力（Spring Retry、Caffeine 定时刷新、Jackson JSON 处理）均为合理假设，符合 Spring Boot 技术栈惯例。

**[通过]** 标准库能力简化设计中的自定义抽象——设计已充分利用 ConcurrentHashMap 替代自定义并发容器、CompletableFuture 替代自定义异步框架、Spring 事件机制替代自定义消息总线。

### 3. 语言特性可行性

**[通过]** 错误处理策略与 Java 异常处理能力匹配：

- `BusinessException` + `GlobalExceptionHandler` 是 Spring Boot 标准异常处理模式
- BLOCK 阻断使用 HTTP 422 + `BlockResponse` 返回，与业务异常体系正交
- AI 降级作为正常业务流程处理（fromFallback 标记），而非异常路径

**[通过]** 并发设计与 Java 并发模型兼容：

- `CompletableFuture` 封装异步 AI 调用结果，Service 层同步等待
- `ConcurrentHashMap` 管理对话会话、草稿上下文和异步结果，compute() 原子操作保证并发安全
- `@Async` / `CompletableFuture.runAsync()` 处理包E 异步 AI 建议
- 部署约束（单实例/sticky session）在 Phase 2/3 假设下合理，且已标注 Phase 5 迁移至分布式缓存

**[通过]** 资源管理方案在 Java 资源管理模式内可行：

- Spring 管理 Bean 生命周期
- JPA 管理数据库事务和连接
- `ScheduledExecutorService` 管理 TTL 清理定时任务
- 事件事务边界使用 `@TransactionalEventListener(phase=AFTER_COMMIT)` 保证一致性

**[通过]** 模块/包结构设计与 Maven 项目组织方式一致，依赖方向清晰（consultation / prescription / medical-record → common / common-module-api / ai-api），无循环依赖。

### 4. 设计一致性

**[通过]** 各抽象的职责描述清晰无歧义——核心抽象一览表（§1.3）、详细职责说明（§3）、行为契约（§4）三层结构完整覆盖。

**[通过]** 协作关系形成闭环，无缺失环节：

- 分诊链路：TriageController → TriageService → AiService.triage() → TriageRuleEngine → DepartmentFallbackProvider 形成完整降级链
- 审核链路：PrescriptionAuditController → PrescriptionAuditService → AiService.prescriptionCheck() → LocalRuleEngine → PrescriptionAuditEnforcer
- 病历生成链路：MedicalRecordController → MedicalRecordService → AiService.generateMedicalRecord() → TemplateConfigManager → MissingFieldDetector
- 辅助开方链路：PrescriptionAssistController → PrescriptionAssistService → DosageThresholdService + AiService.prescriptionAssist()
- 事件链路：registration 模块 → RegistrationEvent → consultation 模块 RegistrationEventListener → TriageRecord.finalDepartmentId 补充写入

**[通过]** v17 迭代需求中 6 个问题全部得到闭环修复：

1. correctedChiefComplaint 传递路径——DialogueCreateRequest 新增 correctedChiefComplaint 字段（显式路径）；ai-api TriageResponse 新增 correctedChiefComplaint 字段（隐式路径，AI 语义标记"患者主诉有误"、"实为"等触发）；两条路径优先级清晰（显式优先）
2. finalDepartmentId 手动选科场景——新增 POST /api/triage/select-department 端点 + TriageService.selectDepartment() 方法；手动选科值优先于 RegistrationEvent 写入值
3. 提交端点阻断判定时序——三步执行顺序严格定义：步① CRITICAL 阻断 → 步② 审核结果 BLOCK 阻断 → 步③ forceSubmit 判定
4. AiResult 实现归属——AiResult.java 标注在 §2.1 目录结构 ai-api 模块下（com.aimedical.modules.ai.api.AiResult）；§2.3 补充包路径和方法签名
5. TTL 不一致——AiSuggestionResult TTL 调整为 60 分钟，与 PrescriptionDraftContext 一致
6. contextCriticalCount 前端消费——定义四种状态变迁规则（N→0、0→N、0→0、N→M）

**[通过]** 模块间依赖方向合理——三个新模块仅依赖 common、common-module-api、ai-api，不依赖其他业务模块 impl 层；强耦合的包D-AI1 与包E 合并为同一模块消除跨模块依赖；DosageStandard 迁移至 common 模块避免 prescription 与 admin 循环依赖。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则——每个抽象定位明确：
- DialogueSessionManager 仅管理会话生命周期和并发控制，不涉及业务逻辑
- Converter 类仅负责 DTO 映射，不包含业务逻辑
- LocalRuleEngine 中各规则类独立实现/测试，每规则仅负责一个检查维度
- PrescriptionAuditEnforcer 仅负责阻断执行策略，不侵入审核核心流程

**[通过]** 抽象层次恰当——架构级设计提供了足够的设计决策完整性和行为契约细节（§4 关键行为契约），同时避免过度指定实现细节（如具体方法签名参数顺序、数据库表定义等）。Interface 的使用为后续扩展预留空间但不引入不必要的抽象（DialogueSessionManager 使用 class 而非 interface 的决策已论证）。

**[通过]** 设计便于后续的详细设计和实现——§10 ai-api 层 DTO 扩展规格明确列出需扩展的完整字段集；Converter 映射方向和映射逻辑清晰；§9 管理接口定义和种子脚本路径已标注。

**[通过]** 设计便于单元测试——各 Service 通过 interface 定义，可 mock AiService、DoctorFacade、TriageRuleEngine、LocalRuleEngine 等依赖；Converter 为纯映射函数，独立可测；LocalRuleEngine 中各规则类独立实现/测试，不依赖 AI 服务。

## 修改要求

本审查无严重或一般问题，无需修改。
