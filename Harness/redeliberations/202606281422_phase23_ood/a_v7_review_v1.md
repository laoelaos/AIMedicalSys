# OOD 设计方案审查报告（v1）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 类型形态选择与 Java 类型系统能力匹配——class（可变/不可变 DTO）、interface（业务契约）、enum（固定有限分类）、JPA @Entity（持久化实体）均为 Java 原生支持的类型形态，Spring Data JPA 的实体继承和仓库接口模式与设计一致。

**[通过]** 抽象之间的继承和实现关系在 Java 约束范围内——所有 interface 契约均为单接口多实现（TriageService、PrescriptionAuditService、MedicalRecordService、PrescriptionAssistService 等），不涉及类多继承。DosageThresholdService 为 class 不涉及继承冲突。

**[通过]** 泛型使用方式在 Java 泛型系统能力范围内——Result<T>、Map<MedicalRecordField, String>、ConcurrentHashMap<String, AiSuggestionResult>、List<RecommendedDepartment> 等均为 Java 标准泛型用法，不含通配符捕获或泛型数组创建等边界场景。

**[通过]** 类型交互模式在 Java 中可实现——ConcurrentHashMap + ScheduledExecutorService 的内存存储模式为标准 Java 并发模式；Spring ApplicationEvent 事件驱动解耦、@Async 异步调用均为 Spring 框架标准能力。

**[轻微]** RecordGenerateResponse 的 fields 使用 `Map<MedicalRecordField, String>` 类型——MedicalRecordField 为 enum，Java 的 EnumMap<MedicalRecordField, String> 在类型安全和性能上优于 HashMap，建议后续实现阶段考虑使用 EnumMap 替代。

### 2. 标准库与生态覆盖

**[通过]** 设计中需要的能力均在 Java 标准库或常用库覆盖范围内——ConcurrentHashMap（并发集合）、ScheduledExecutorService（定时任务）、CompletableFuture（异步编排）、Spring ApplicationEvent（事件驱动）、Caffeine（缓存刷新）、Spring Data JPA（持久化）、UUID（ID 生成）均为项目中已使用或常见库。

**[通过]** 设计中对库能力的假设合理——Spring AI ChatClient/ChatModel 的调用模式与项目中已定义的 AiService 接口一致；Spring @Async + CompletableFuture 的异步模式为 Spring Boot 3 标准用法。

**[通过]** 未存在标准库能力可简化设计却未使用的情况——ConcurrentHashMap.compute()/computeIfAbsent() 用于原子更新已是最优选择；Caffeine refreshAfterWrite 用于定时缓存刷新已合理。

**[轻微]** PrescriptionDraftContext 使用 ConcurrentHashMap<String, List<DosageAlert>> 存储——如果需要写入原子性保证（如 List 的并发追加），ConcurrentHashMap 仅保证 map 操作原子性但 List 本身非线程安全。设计中提到"DosageThresholdService 写入"和"PrescriptionAuditService 读取"，若写入和读取可能并发，建议后续实现阶段对 List 使用 CopyOnWriteArrayList 或 synchronized 块保护。

### 3. 语言特性可行性

**[通过]** 错误处理策略与 Java/Spring 能力匹配——GlobalExceptionHandler + ErrorCode 接口 + BusinessException 体系与已有 Phase 0 设计一致；BLOCK 阻断通过 Controller 直接返回 HTTP 422 与异常体系正交，实现可行。

**[通过]** 并发设计与 Java 并发模型兼容——ConcurrentHashMap + ScheduledExecutorService + CompletableFuture 的组合为 Java 标准并发模式；AiSuggestionResult 的 compute() 原子更新和预创建→更新模式在 Java ConcurrentHashMap 语义下可实现。

**[通过]** 资源管理方案在 Java/Spring 资源管理模式内可行——内存存储（DialogueSession、AiSuggestionResult、PrescriptionDraftContext）的 TTL 过期清理由 ScheduledExecutorService 定期扫描实现，与 Spring Bean 生命周期无冲突。

**[通过]** 模块/包结构设计符合 Maven 多模块项目组织方式——三新模块均为扁平 Maven 模块（不拆分为 api/impl），与 patient/doctor/admin 结构一致；模块间依赖方向合理（三层依赖：common → modules → application）。

### 4. 设计一致性

**[通过]** 各抽象的职责描述清晰无歧义——TriageService（分诊业务契约）、DialogueSessionManager（会话生命周期）、LocalRuleEngine（本地规则校验）等核心抽象的职责边界和协作契约均明确定义，无重叠或遗漏。

**[通过]** 协作关系形成闭环，无缺失环节——分诊降级链（AI→规则引擎→兜底提供者）形成完整三级闭环；处方审核降级链（AI→本地规则→AuditRecord 持久化→AuditResponse）闭环完整；辅助开方 CRITICAL→BLOCK 联动机制（check-dose 写入 PrescriptionDraftContext → 提交审核时读取 → BLOCK 判定 → 提交后清理）闭环完整。

**[通过]** 行为契约完整到足以指导后续实现——各场景的行为契约（§4.1–4.4）明确描述了正常路径、降级路径、错误分支和持久化要求，可指导详细设计和编码。

**[通过]** 模块间依赖方向合理，无循环依赖——三个新模块均单向依赖 common/common-module-api/ai-api；prescription 模块内 D-AI1 与 E 共享数据无循环；admin 监听器写入 ConfigChangeLog 通过 Spring ApplicationEvent 解耦，无直接模块间编译期依赖。

**[通过]** 需求文档 3.4.1/3.4.2/3.4.3/3.4.10 全字段对齐已在本轮完成——DialogueCreateRequest 含 chiefComplaint(5-500)/additionalResponses/patientId/sessionId/ruleVersion/ruleSetId；TriageResponse 含 departments(List\<RecommendedDepartment\>)/doctors(List\<RecommendedDoctor\>)/reason/matchedRules(List\<MatchedRule\>)/degraded/fallbackHint；AuditResponse 含 riskLevel/alerts(List\<AuditAlert\>)/interactions/suggestions/fromFallback；RecordGenerateRequest 含 dialogueText/patientId/encounterId/stream；MedicalRecordField 枚举与需求字段名映射表完整；missingFieldHints 与 missing_fields 严格超集关系已说明。对齐审查的系统性遗漏已消除。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则——各抽象职责单一：DialogueSessionManager 仅管会话生命周期、DosageThresholdService 仅管剂量阈值校验、MissingFieldDetector 仅管缺失字段检测、PrescriptionAuditEnforcer 仅管阻断执行策略。

**[通过]** 抽象层次恰当——interface 用于业务契约（TriageService、PrescriptionAuditService、LocalRuleEngine）、class 用于无多实现需求的服务（DosageThresholdService、DialogueSessionManager）、enum 用于固定分类（AuditRiskLevel、DosageAlertLevel、MedicalRecordField），划分合理不过度设计。

**[通过]** 设计便于后续详细设计和实现——各抽象的职责、协作关系、行为契约和数据结构已足够明确，可指导详细设计阶段补充方法签名和字段定义。

**[通过]** 设计便于单元测试——所有 interface 契约均可 mock（TriageService、PrescriptionAuditService、LocalRuleEngine、AiService、TemplateConfigManager、DepartmentFallbackProvider、MissingFieldDetector、PrescriptionAuditEnforcer）；内存存储组件（DialogueSessionManager、PrescriptionDraftContext）可独立实例化测试；enum 类型可穷举覆盖。

**[轻微]** 本地规则校验的 4 条规则（DosageLimitRule、AllergyCheckRule、DuplicateCheckRule、SpecialPopulationDosageRule）聚合风险等级的策略描述为"若任一规则 severity 为 BLOCK 则整体判定为 BLOCK；若无 BLOCK 但存在 WARN 则整体判定为 WARN"，此策略可将聚合逻辑提取为独立的 LocalRuleResultAggregator 便于单独测试和复用，但当前内嵌于 PrescriptionAuditService 实现中也属合理。
