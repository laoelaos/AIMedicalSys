# OOD 设计方案审查报告（v4）

## 审查结果

REJECTED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 所有抽象类型形态选择（interface / class / enum / JPA @Entity）均与 Java 21 类型系统完全匹配。继承与实现关系严格遵循单继承+多接口实现约束。泛型使用合理（无出界设计）。协作关系中描述的接口依赖、Spring DI 注入、事件驱动模式均为 Java/Spring 生态标准实践。

**[通过]** TriageService / LocalRuleEngine / TemplateConfigManager / MissingFieldDetector 等核心接口定义为 interface 而非 abstract class，符合 Java 接口的多实现能力与 Spring 面向接口编程惯例。DosageThresholdService / DialogueSessionManager 等职责稳定、无多实现需求的部分以 class 实现，抽象层次恰当。

### 2. 标准库与生态覆盖

**[通过]** 设计依赖的 Spring Boot（Controller/Service/Repository/@Async/@Entity/ApplicationEventPublisher）、JPA/Hibernate、Caffeine 缓存、ConcurrentHashMap、ScheduledExecutorService、CompletableFuture 等均在 Java 21 / Spring Boot 标准库及常用生态范围内。

**[通过]** AiService 接口假设 ai-api 模块已存在（参考 Phase 0 已有模块），该假设基于项目现有架构，合理。DosageStandard 实体迁移至 common 模块、ConfigChangeLog 通过事件处理链写入等机制均为标准 JPA + Spring Event 模式，无需特殊库支持。

### 3. 语言特性可行性

**[通过]** 错误处理策略与 Java/Spring 异常体系一致：模块级 ErrorCode 前缀 + GlobalExceptionHandler + BusinessException 复用现有框架；BLOCK 阻断通过 HTTP 422 + BlockResponse 绕过异常框架直接返回，与业务异常体系正交，设计合理。

**[通过]** 并发设计完全在 Java 并发工具包能力范围内：ConcurrentHashMap + ScheduledExecutorService（session TTL + 清理）、CompletableFuture（AI 调用同步等待）、@Async（异步 AI 建议），均为成熟实践。

**[通过]** 资源管理方案可行：内存存储 + TTL 定时清理，Phase 5 可平滑迁移至数据库物理表。模块/包结构符合 Maven + Spring Boot 项目组织惯例。

### 4. 设计一致性

**[通过]** 各抽象职责描述清晰无歧义。协作关系形成闭环：三级降级链（AI → 规则引擎 → 兜底提供者）在 §3.1 职责说明和 §4.1 行为契约中完全一致。AuditRecord 正常/降级路径统一持久化的要求在 §3.2 职责说明和 §4.2 行为契约中双双明确。模块间依赖方向合理，三个新模块均不互相依赖。

**[一般]** §2.1 目录结构中 ConfigChangeLog.java 位于 prescription 模块（`prescription/.../entity/ConfigChangeLog.java`），但 §9.3 和 §10.1 明确推荐归属 admin 模块（"本设计推荐归属 admin 模块"）。目录与文本推荐不一致，导致实现者在实体归属判断上产生歧义，可能引发模块间依赖错误。若按目录实现，则 prescription 模块拥有了审计实体而 medical-record 模块的事件处理链需要跨模块访问或重复定义；若按文本推荐实现，则目录需修正为 admin 模块路径。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则：TriageService（分诊流程编排）与 TriageRuleEngine（规则匹配）分离、TemplateConfigManager（模板管理）与 MissingFieldDetector（字段缺失检测）分离，各抽象职责边界清晰。

**[通过]** 抽象层次恰当：对可能演化的部分使用 interface（审核策略、阻断策略、规则引擎等），对稳定的部分直接使用 class（DosageThresholdService、DialogueSessionManager），不过度设计。

**[通过]** 设计便于测试：核心业务逻辑通过 interface 定义易于 mock；LocalRuleEngine 拆分为独立规则（DrugInteractionRule、AllergyCheckRule、DosageLimitRule）支持独立单元测试。

## 修改要求（REJECTED 时存在）

- **问题**：§2.1 目录结构与 §9.3/§10.1 对 ConfigChangeLog 实体归属的表述不一致——目录将其放在 prescription 模块 entity 目录下，文本推荐放置在 admin 模块。
- **原因**：该不一致将导致实现者无法确定 ConfigChangeLog 的正确归属模块。若按目录实现，则 admin 模块监听器跨模块写入或 medical-record 模块直接或间接依赖 prescription 模块，违反"三个新模块之间不允许互相依赖"的约束；若按文本推荐实现，则目录结构需要修正。两者矛盾降低了设计对实现的可指导性。
- **建议方向**：统一 ConfigChangeLog 实体的归属表述。若采纳 admin 模块方案（文本推荐方案），则：(a) 将 §2.1 目录下的 `prescription/.../entity/ConfigChangeLog.java` 移至 `admin/.../entity/ConfigChangeLog.java`；(b) 确认 admin 模块的 Repository 和事件监听器可正常写入审计日志；(c) 确认 medical-record 模块的 TemplateConfigManager 发布 TemplateConfigChangeEvent 时不需要依赖 admin 模块的实体类型（通过 Spring ApplicationEvent 解耦即可满足）。若采纳 common 模块方案（§9.3 首次提及的选项），则修订 §10.1 的推荐结论并将 ConfigChangeLog 放入 common/entity/，使所有模块均可直接引用。
