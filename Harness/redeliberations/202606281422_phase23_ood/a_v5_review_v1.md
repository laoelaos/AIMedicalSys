# OOD 设计方案审查报告（v1）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 类型形态选择合理：interface 用于多实现扩展点（TriageService、TriageRuleEngine、DepartmentFallbackProvider、PrescriptionAuditService、LocalRuleEngine、PrescriptionAuditEnforcer、TemplateConfigManager、MedicalRecordService、MissingFieldDetector），class 用于单一实现或需维护可变状态的抽象（DialogueSession、DialogueSessionManager、DosageThresholdService），enum 用于固定有限分类（AuditRiskLevel、DosageAlertLevel、MedicalRecordField），JPA @Entity 用于持久化实体（AuditRecord、MedicalRecord、DosageStandard）。DTO 一律为 class，符合 Java 习惯。

**[通过]** 继承与实现关系在 Java 类型系统约束内：enum 实现 ErrorCode/BaseEnum 接口（Java enum 可实现接口），JPA 实体继承 BaseEntity（abstract class，Java 单继承），interface 之间无继承链。所有多实现场景使用 interface，不存在需要多继承 class 的场景。

**[通过]** 泛型使用限于 Result<T>、PageResponse<T>、AiResult<T> 等已有框架类，设计方案自身未引入新的泛型抽象，无泛型系统能力风险。

**[通过]** 协作关系中的类型交互模式在 Java 中完全可实现：ConcurrentHashMap 存储 session/result、Spring 事件机制解耦模块间通知、CompletableFuture 异步回调、Caffeine 缓存刷新等均为 Spring Boot 3 生态标准模式。

**[轻微]** DosageStandard 实体迁移至 common 模块后，admin 模块写入、prescription 模块只读的跨模块共享模式依赖 Maven 编译期隔离（impl 不可见）和运行期 Repository 访问控制。设计方案未显式说明 prescription 模块的 DosageStandardRepository 如何在 Spring Data JPA 下仅开放查询方法（如通过定义仅含 find 方法的自定义 Repository 接口）。建议在实现阶段自定义只读 Repository 接口以强化只读语义，但当前设计级别不构成阻塞。

### 2. 标准库与生态覆盖

**[通过]** 设计中所需能力均在 Spring Boot 3 标准库及常用库覆盖范围内：
- ConcurrentHashMap + ScheduledExecutorService：JDK 标准库
- Caffeine 缓存（refreshAfterWrite）：Spring Boot 3 常用缓存库，与项目 Maven 依赖管理兼容
- CompletableFuture：JDK 标准库异步编程模型
- Spring ApplicationEventPublisher / @EventListener：Spring 框架标准事件机制
- Spring Data JPA Repository：项目已选用的 ORM 框架
- @Async：Spring 框架标准异步注解

**[通过]** 设计中关于 Caffeine refreshAfterWrite 缓存刷新策略的假设是合理的——Caffeine 确实支持 refreshAfterWrite（异步刷新，不阻塞读取），与设计方案中"定时刷新规则缓存"的需求匹配。事件驱动缓存失效（ApplicationEventPublisher）同样可行。

**[通过]** 设计中未引入任何非标准库假设。本地规则校验、单位换算、差集比对等逻辑均为纯 Java 业务逻辑，不依赖外部库。

**[轻微]** §4.4 中前端通过 GET 端点轮询 AI 建议状态，设计方案未探讨 SSE/WebSocket 推送替代方案。当前轮询模式满足 Phase 2/3 需求，但高频轮询可能带来不必要的请求开销。建议在 Phase 5 进阶阶段评估 SSE 推送替代，当前设计级别不阻塞。

### 3. 语言特性可行性

**[通过]** 错误处理策略与 Java/Spring 能力匹配：BusinessException + ErrorCode 体系复用 Phase 0 已建框架；BLOCK 阻断不经过异常框架而直接通过 Controller 返回 422 响应，与全局异常处理器正交，实现可行；CompletableFuture.exceptionally() 捕获异步异常更新 AiSuggestionResult 为 FAILED，是 Java 异步编程的标准做法。

**[通过]** 并发设计与 Java 并发模型兼容：
- DialogueSessionManager 使用 ConcurrentHashMap + 同 session 串行化（可由前端保证顺序或后端加锁），不同 session 独立
- AiSuggestionResult 预创建→更新模式使用 ConcurrentHashMap，put/update 操作为原子操作
- ScheduledExecutorService 定期清理过期 session/result，标准 JDK 并发工具

**[通过]** 资源管理方案在 Spring Boot 管理模式内可行：DialogueSession 和 AiSuggestionResult 使用内存存储 + TTL 定时清理，由 ScheduledExecutorService 管理；AuditRecord 和 MedicalRecord 使用 JPA 持久化；DosageStandard 通过 admin 模块管理、prescription 模块只读。

**[通过]** 模块/包结构设计遵循 Spring Boot 多模块的最佳实践：扁平 Maven 模块结构与 Phase 0 的 patient/doctor/admin 一致；分包方式（api/service/repository/entity/dto/converter）与 Phase 0 骨架一致；Spring Boot 组件扫描由 application 聚合层统一覆盖。

**[轻微]** DialogueSessionManager 中"同 session 请求串行"的具体实现方式在设计层面未确定（是前端等待响应后再发下一条，还是后端对同一 sessionId 加锁）。两种方式在 Java 中均可实现，建议在详细设计阶段明确。当前设计级别不阻塞。

### 4. 设计一致性

**[通过]** 各抽象的职责描述清晰无歧义。§1.3 核心抽象一览表与 §3.x 各节的详细定义一致，每个抽象均有明确的职责定位、协作对象说明和类型形态选择理由。

**[通过]** 协作关系形成闭环，无缺失环节：
- 包C：TriageController → TriageService → (AiService → TriageRuleEngine → DepartmentFallbackProvider)，DialogueSessionManager 管理 session，完整覆盖单轮/多轮/降级/超时
- 包D-AI1：PrescriptionAuditController → PrescriptionAuditService → (AiService | LocalRuleEngine) → AuditRecord + PrescriptionAuditEnforcer，完整覆盖正常/降级/BLOCK 阻断
- 包D-AI2：MedicalRecordController → MedicalRecordService → (AiService + TemplateConfigManager + MissingFieldDetector)，完整覆盖正常/兜底/降级
- 包E：PrescriptionAssistController → PrescriptionAssistService → DosageThresholdService + AiService (async)，完整覆盖同步告警/异步建议/四分支查询

**[通过]** 行为契约描述完整，足以指导后续实现：
- §4.1-4.4 分别描述了四个包的关键行为契约，含请求/响应格式、降级链路、异常分支
- §5.1 错误码表覆盖各模块的主要错误场景
- §6.1-6.3 并发设计明确内存存储策略和并发控制方式
- §8.4 五级匹配优先级策略描述完整

**[通过]** 模块间依赖方向合理，无循环依赖：
- 三个新模块均依赖 common、common-module-api、ai-api，不互相依赖
- prescription 模块内部 PrescriptionAssistServiceImpl 可注入 PrescriptionAuditService（同模块，方向合理）
- medical-record 模块通过 Spring ApplicationEvent 接收 admin 模块事件，无编译期依赖
- admin 模块为 DosageStandard 唯一写入者（common 实体 → admin 写入、prescription 只读），方向合理

**[轻微]** §3.4 中 AiSuggestionResult 的 status 字段描述为"AiSuggestionStatus 枚举：PENDING / COMPLETED / FAILED"，但 §1.3 核心抽象一览表中 AiSuggestionResult 的描述仅提到"status（状态枚举：PENDING / COMPLETED / FAILED）"而未给出枚举类型名称。建议统一使用 AiSuggestionStatus 作为枚举类型名，或在§1.3 中显式引用。不影响设计可行性，仅影响文本一致性。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则：
- TriageService 封装分诊业务逻辑，DialogueSessionManager 独立承担会话生命周期和并发控制
- PrescriptionAuditService 封装审核业务逻辑，LocalRuleEngine 独立封装本地规则，PrescriptionAuditEnforcer 独立封装阻断执行策略
- DosageThresholdService 独立封装剂量阈值校验逻辑，与 PrescriptionAssistService（协调同步告警+异步建议）分离
- TemplateConfigManager 封装模板配置管理，MissingFieldDetector 独立封装字段缺失检测逻辑

**[通过]** 抽象层次恰当：
- interface 用于合理的扩展点（审核策略、规则引擎、阻断策略），不过度设计
- class 用于无多实现需求的服务（DosageThresholdService、DialogueSessionManager），不设计不足
- DTO 与 entity 分层清晰，converter 负责转换

**[通过]** 设计便于后续详细设计和实现：每个抽象的职责、协作对象和接口方法语义已明确到足以指导详细设计。行为契约章节提供了端到端的交互序列，错误处理和降级路径完整。

**[通过]** 设计便于单元测试：
- TriageService、PrescriptionAuditService、MedicalRecordService、PrescriptionAssistService 均为 interface，实现类可独立测试
- 降级链路的各环节（AiService、TriageRuleEngine、DepartmentFallbackProvider、LocalRuleEngine）均为 interface，可 mock 验证降级行为
- AiSuggestionResult 预创建→更新模式可独立验证四分支查询逻辑
- DosageThresholdService 的五级匹配策略可通过 DosageStandard Repository mock 验证各匹配优先级
- PrescriptionAuditEnforcer 为 interface，阻断策略可独立测试
