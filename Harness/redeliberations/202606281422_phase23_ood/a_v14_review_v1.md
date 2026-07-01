# OOD 设计方案审查报告（v14）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 所有类型形态选择均与 Java 类型系统能力匹配：

- `interface` 用于服务契约（TriageService、PrescriptionAuditService、LocalRuleEngine 等）——Java 接口支持多实现、多继承
- `class` 用于管理器/具体实现（DialogueSessionManager、DosageThresholdService）——职责边界稳定，无 interface 过度抽象
- `enum` 用于固定分类（AuditRiskLevel、AlertSeverity、DosageAlertLevel、MedicalRecordField 等）——Java enum 完全支持
- JPA `@Entity` 用于持久化实体——标准 JPA 规范
- 泛型 `CompletableFuture<AiResult<T>>` 用于 AI 异步调用——Java 泛型系统完全支持
- `@Retryable` / `@Recover` 用于重试和死信处理——Spring Retry 标准注解
- `@TransactionalEventListener(phase=AFTER_COMMIT)` 用于跨模块事件——Spring 框架标准支持

### 2. 标准库与生态覆盖

**[通过]** 设计中依赖的库能力均在 Java/Spring 生态覆盖范围内：

- Spring Boot（@Service、@Component、@ConfigurationProperties）——标准框架
- Spring Data JPA（@Entity、Repository）——标准持久化方案
- Spring Retry（@Retryable、@Recover）——spring-retry 库
- Spring ApplicationEvent / @TransactionalEventListener ——事件机制
- Caffeine（refreshAfterWrite）——Spring Boot 默认缓存选项
- Jackson（@Convert + AttributeConverter）——与 JPA 集成标准方案
- Java 标准库（ConcurrentHashMap、ScheduledExecutorService、CompletableFuture）——JDK 原生

### 3. 语言特性可行性

**[通过]** 错误处理、并发设计、资源管理均与 Java 能力匹配：

- **错误处理**：AiResult 模式封装 AI 调用结果 + BusinessException 体系处理业务异常 + HTTP 422 处理 BLOCK 阻断 — 多级策略清晰可行；**@DltHandler→@Recover 已修正**（§2.2）
- **并发设计**：ConcurrentHashMap 存储 + ScheduledExecutorService TTL 清理 + @Async/CompletableFuture.runAsync() 异步 AI + 原子竞态处理（ConcurrentHashMap.remove()）— 全部在 Java 并发工具范围内
- **资源管理**：内存存储（TTL 清理）+ JPA 数据库持久化 — 常规模式
- **模块结构**：扁平 Maven 模块 + 按职责分包（api/service/repository/entity/dto/converter）— 符合 Java 项目惯例
- **水平扩展预留**：§6.1 明确标注 Phase 2/3 单实例/sticky session 假设，Phase 5 迁移分布式缓存的路径 —— 设计透明

### 4. 设计一致性

**[通过]** 各抽象职责清晰、协作闭环、行为契约完整、依赖方向合理：

- **DuplicateCheckRule 成分编码**：采用 ingredientCode（成分编码）而非 ingredientName 做交叉匹配，已避免字符串匹配的临床漏报和假阳性；DrugCompositionDict.ingredients 每项含 ingredientCode（string，必填） + ingredientName（string，必填）— **已修正**（§3.2）
- **PrescriptionDraftContext TTL 清理**：ScheduledExecutorService 每 5 分钟扫描一次，清理 60 分钟超时条目 — **已实现**（§3.4）
- **dead_letter_event 模块归属**：DeadLetterEvent 实体和 DeadLetterEventRepository 定义在 consultation 模块，DeadLetterCompensationService 由 ScheduledExecutorService 每 30 分钟调度 — **已定义**（§2.2）
- **SpecialPopulationDosageRule 阈值配置化**：通过 @Value / @ConfigurationProperties 注入配置项 special-population.child-age-max 和 special-population.elderly-age-min — **已实现**（§3.2）
- **AiResult 泛型时序依赖**：§2.3 标注泛型 T 绑定的 ai-api DTO 当前为空壳类，§10 明确标注时序依赖关系 — **前提条件已声明**
- **事件丢失补偿覆盖一致性**：DrugContraindicationMapping/DrugAllergyMapping/DrugCompositionDict 三者统一采用"事件驱动刷新 + Caffeine 定时刷新（refreshAfterWrite）"双重失效策略 — **已覆盖**（§9.3）
- **枚举命名统一**：DosageAlertLevel（INFO/WARNING/CRITICAL）、AlertSeverity（INFO/WARNING/CRITICAL）、AllergyWarningSeverity（CRITICAL/WARNING/INFO）三者值名一致 — **已统一**（§1.3）— 仅声明顺序有差异，不影响使用
- 模块间依赖：三个新模块仅依赖 common/common-module-api/ai-api，互不依赖 — 无循环依赖
- Collaboration 闭环：TriageService → TriageRuleEngine → DepartmentFallbackProvider → DoctorFacade，各降级链完整

### 5. 设计质量

**[通过]** 职责划分遵循 SRP、抽象层次恰当、便于后续实现和测试：

- 各抽象单一职责：Converter 仅负责映射、Service 仅负责流程编排、Rule 仅负责校验逻辑
- 接口隔离恰当—有变化预期的角色使用 interface（规则引擎、规则链），稳定的使用 class（管理器）
- 独立规则的 LocalRuleEngine 设计便于单独测试和 mock（每个 rule 有独立 ruleId）
- DTO 为纯 POJO，无业务逻辑，便于序列化和测试
- 降级链清晰（AI → 规则引擎 → 兜底 → 连续失败兜底），便于端到端覆盖测试

## 修改要求

无。无严重或一般问题。
