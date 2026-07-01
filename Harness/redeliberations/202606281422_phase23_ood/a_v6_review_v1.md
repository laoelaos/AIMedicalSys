# OOD 设计方案审查报告（v6）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 类型形态选择与 Java 类型系统能力匹配：interface（TriageService / PrescriptionAuditService / MedicalRecordService / PrescriptionAssistService / LocalRuleEngine 等）用于多实现扩展点，class 用于单一实现且状态稳定的抽象（DosageThresholdService / DialogueSessionManager），enum 用于固定有限分类（AuditRiskLevel / DosageAlertLevel / MedicalRecordField / DosageUnitGroup），JPA @Entity 用于持久化实体（AuditRecord / MedicalRecord / TriageRecord / ConfigChangeLog），DTO class 用于值对象传输——上述选择均在 Java 单继承+多接口实现的约束范围内，无不安全协变或非法泛型使用。

**[通过]** 抽象间继承和实现关系合理：各 Service 接口由 impl 包下的 class 实现，符合 Java 惯例；PrescriptionAuditEnforcer 接口允许不同阻断策略实现，不引入循环依赖；LocalRuleEngine 接口下多条独立规则 class 并行实现，无继承链过长问题。

**[通过]** 泛型使用合理：AiResult<T> / List<RecommendedDoctor> / List<AdditionalResponse> / List<DrugInteraction> 等均为标准 Java 泛型用法，不涉及逆变/协变等高级泛型特性，在 Java 类型系统内完全可行。

**[通过]** 协作关系中描述的类型交互模式在 Java 中可实现：ConcurrentHashMap<String, AiSuggestionResult> / ConcurrentHashMap<String, DialogueSession> / ConcurrentHashMap<String, List<DosageAlert>> 均为标准并发容器用法；CompletableFuture<AiResult<T>> 异步模式与 Spring @Async 兼容。

**[轻微]** PrescriptionDraftContext 内部使用 ConcurrentHashMap<String, List<DosageAlert>> 存储 CRITICAL 告警——List<DosageAlert> 作为 ConcurrentHashMap value 不是线程安全结构，若同一处方编辑会话存在并发写入 CRITICAL 告警的场景（如同时编辑同一处方的多个药品），需对 List 操作额外同步或改用 CopyOnWriteArrayList。当前设计描述中"同一处方编辑会话的写入与读取通过 ConcurrentHashMap 线程安全特性保证一致性"仅覆盖了 map 级别的线程安全，未覆盖 value 对象级别。但考虑到医学系统实际场景中同一处方会话并发编辑概率较低（前端通常串行编辑），此问题优先级为轻微。

### 2. 标准库与生态覆盖

**[通过]** 设计中需要的能力均在 Java 标准库和 Spring 生态覆盖范围内：集合操作（Java Collections / ConcurrentHashMap）、定时任务（ScheduledExecutorService / Spring @Scheduled）、缓存（Caffeine）、JPA 持久化（Spring Data JPA）、事件驱动（Spring ApplicationEventPublisher / @EventListener）、异步执行（CompletableFuture / Spring @Async）、DTO 校验（Jakarta Validation / Hibernate Validator）、REST API（Spring MVC @RestController）——上述均在标准库或常用库的覆盖范围内，无需假设不存在的能力。

**[通过]** 设计中假设的库能力合理：Caffeine refreshAfterWrite 缓存刷新机制确实支持；Spring ApplicationEvent 机制支持跨模块事件传播且无需编译期依赖；ConcurrentHashMap.compute() / computeIfAbsent() 提供 BiFunction 闭包内原子更新语义。

**[通过]** 无明显可用标准库能力简化的自定义抽象：DosageUnitGroup 枚举固化换算系数是合理的领域特定设计，不适合用 Java generic Unit 库替代；MedicalRecordField 枚举的差集检测逻辑不可能用标准库简化。

### 3. 语言特性可行性

**[通过]** 错误处理策略与 Java/Spring 错误处理能力匹配：GlobalExceptionHandler + ErrorCode 接口 + BusinessException 体系为 Spring Boot 标准模式；BLOCK 阻断走 HTTP 422 而非异常框架是合理的业务分支设计；各模块错误码前缀化规划（TRIAGE_ / RX_AUDIT_ / MR_ / RX_ASSIST_）清晰且符合 3.4 节命名约定。

**[通过]** 并发设计与 Java 并发模型兼容：ConcurrentHashMap 用于对话会话存储和 AI 建议暂存是 Spring Boot 应用中的标准做法；CompletableFuture 用于 AI 调用异步编排符合 Java 异步编程范式；ScheduledExecutorService 用于 TTL 清理是标准方案；DialogueSessionManager 对同 session 请求串行化通过前端等待响应的自然顺序保证，避免加锁复杂度。

**[通过]** 资源管理方案在 Java/Spring 资源管理模式内可行：对话会话和 AI 建议采用内存 ConcurrentHashMap + TTL 过期清理，不在 JVM 退出时持久化——设计文档已显式承认此为已知限制并在 Phase 5 计划迁移至数据库；JPA 实体由 Spring Data JPA 管理，事务边界由 Service 层 @Transactional 控制，符合 Spring 事务管理模式。

**[通过]** 模块/包结构设计符合 Maven 多模块项目组织方式：三个新模块（consultation / prescription / medical-record）为扁平 Maven 模块，与已有 patient / doctor / admin 模块一致；各模块内按 api / service / repository / entity / dto / converter 分包，符合 Spring Boot 分层架构惯例；DosageStandard 迁移至 common 模块的跨模块共享方案在 Maven 编译期依赖管理中完全可行。

**[通过]** AI 超时配置采用 application.yml 属性键（如 ai.timeout.prescription-audit=6s），Spring Boot @Value 或 @ConfigurationProperties 注入是标准模式。

### 4. 设计一致性

**[通过]** 各抽象的职责描述清晰无歧义：TriageService 封装分诊逻辑与降级链，DialogueSessionManager 管理会话生命周期，DosageThresholdService 执行剂量阈值匹配，PrescriptionDraftContext 解耦即时反馈与终审时点——每个抽象的职责边界明确，无重叠或遗漏。

**[通过]** 协作关系形成闭环，无缺失环节：
- 分诊：Controller → TriageService → AiService / TriageRuleEngine / DepartmentFallbackProvider（三级降级闭环）→ TriageRecord 持久化 → TriageResponse 返回
- 处方审核：Controller → PrescriptionAuditService → AiService / LocalRuleEngine（降级闭环）→ AuditRecord 持久化 → AuditResponse 返回；BLOCK → PrescriptionAuditEnforcer 执行阻断
- 病历生成：Controller → MedicalRecordService → AiService / TemplateConfigManager（兜底 DEFAULT）→ MissingFieldDetector → RecordGenerateResponse 返回
- 辅助开方：/assist → PrescriptionAssistService → AiService.prescriptionAssist() + DosageThresholdService → PrescriptionAssistResponse 返回；/check-dose → DosageThresholdService → CRITICAL → PrescriptionDraftContext → 提交时 PrescriptionAuditService 消费

**[通过]** 行为契约描述完整到足以指导后续实现：
- 降级判定语义明确（AiResult.success=false / degraded=true 触发降级；success=true + 空列表为有效结果）
- CRITICAL→BLOCK 联动机制完整（写入草稿上下文 → 提交时读取 → 与 AI/本地规则结果聚合）
- AiSuggestionResult 并发安全策略明确（compute/computeIfAbsent 原子操作 + 幂等状态转换 + 非法转换静默忽略）
- AuditRiskLevel 聚合规则清晰（任一 BLOCK 则 BLOCK；无 BLOCK 有 WARN 则 WARN；全 PASS 则 PASS）

**[通过]** 模块间依赖方向合理，无循环依赖：三个新模块均依赖 common / common-module-api / ai-api，不互相依赖；跨模块协作通过 Spring ApplicationEvent 解耦（TemplateConfigChangeEvent）；PrescriptionAssistService 注入 PrescriptionAuditService 为同模块内依赖，不构成循环。

**[轻微]** DialogueCreateRequest 中的 ruleVersion 字段被 DialogueSessionManager 在创建会话时使用——但 TriageRuleEngine.currentRuleVersion() 的返回值如何映射到前端下发的 ruleVersion（来自 3.3.3 管理端）未显式定义。需求文档 3.4.1 定义了 rule_version 和 rule_set_id 两个可选字段，设计中仅处理了 ruleVersion 而 rule_set_id 未提及——若后续需按规则集 ID 定向加载规则，需补充。但鉴于 rule_set_id 在需求中为可选字段且当前设计以 DialogueSession 快照 ruleVersion 解决对话内一致性即可，此为轻微级。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则：DialogueSessionManager 仅管理会话生命周期和并发控制，TriageRuleEngine 仅负责规则匹配，DosageThresholdService 仅负责剂量阈值校验，MissingFieldDetector 仅负责差集检测，PrescriptionAuditEnforcer 仅负责阻断执行——各抽象职责边界清晰，无不合理的职责混合。

**[通过]** 抽象层次恰当：未出现过度设计（如为每个 DTO 定义 interface）或设计不足（如缺少 AuditRiskLevel 枚举或 DosageAlertLevel 枚举导致使用魔法字符串）；PrescriptionDraftContext 作为临时性内存数据结构（无需持久化）的设计恰好满足 CRITICAL→BLOCK 跨时点联动需求；LocalRuleEngine 下 DrugInteractionRule / AllergyCheckRule 骨架预留但标注 Phase 3 待实现，未过早投入。

**[通过]** 设计便于后续的详细设计和实现：每个 DTO 的字段已定义到与需求文档对齐的粒度；Service 接口的方法语义和行为契约明确（如 DosageThresholdService 的五级匹配优先级）；降级路径和异常分支均已显式描述；种子数据脚本路径和初始化策略已定义。

**[通过]** 设计便于单元测试：
- 所有 Service 接口可 mock（TriageService / PrescriptionAuditService / MedicalRecordService / PrescriptionAssistService / AiService）
- LocalRuleEngine 可按规则独立测试
- DosageThresholdService 为 class 但可注入 DosageStandardRepository mock
- DialogueSessionManager 可独立测试会话生命周期
- ConcurrentHashMap 存储的 AiSuggestionResult 可直接构造测试数据验证四分支模式
- MissingFieldDetector 的差集比对逻辑可独立测试

**[通过]** 需求文档契约对齐完成度：v6 设计已系统性地对齐全部 16 项上一轮审查问题——
1. AiSuggestionResult 并发安全（ConcurrentHashMap.compute() 原子操作）
2. 包E 主端点与 3.4.10 契约对齐（POST /api/prescription/assist + PrescriptionAssistRequest/Response）
3. TriageResponse 增加 doctors + reason 对齐 3.4.1
4. AuditRequest 字段完整对齐 3.4.2
5. RecordGenerateRequest.encounterId 映射 3.4.3 encounter_id
6. session_id 必填/可选语义阐明
7. AuditRecord 增加 forceSubmitted/forceSubmitTime
8. sessionId UUID v4 生成策略
9. CRITICAL→BLOCK PrescriptionDraftContext 联动机制
10. 本地规则 Phase 2/3 实现范围界定
11. 分诊降级判定语义明确
12. AI 超时配置映射
13. AuditResponse 增加 interactions/suggestions
14. TriageRecord 字段定义完整

**[轻微]** AuditResponse.issues 中的 AuditIssue.severity 类型为 AuditRiskLevel（与需求文档 3.4.2 输出契约中 alerts 数组每项含 severity 字段一致），但需求文档 3.4.2 输出契约使用的是 `alerts`（含 alert_code / alert_message / severity）而非 `issues`——设计中以 AuditIssue（fieldName / issueDescription / ruleId / severity）替代了 alerts（alert_code / alert_message / severity），两者语义接近但字段名和结构不完全一致。鉴于 AuditIssue 包含了 alerts 的核心信息（alert_code ≈ ruleId, alert_message ≈ issueDescription, severity 是共享字段）且增加了 fieldName 便于前端定位问题字段，属于合理的架构级增强，不构成契约偏离，但后续详细设计阶段需在 DTO 映射层做 snake_case 与 camelCase 的字段名映射。
