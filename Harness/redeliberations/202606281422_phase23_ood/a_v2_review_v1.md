# OOD 设计方案审查报告（v1）

## 审查结果

REJECTED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 设计中所有类型形态选择（interface / class / enum / JPA @Entity / immutable class）均与 Java 17+ 的类型系统能力匹配，抽象之间的继承和实现关系（单继承、多接口实现）均在语言约束范围内。

**[通过]** 泛型使用方式（`Result<T>`、`CompletableFuture<AiResult<T>>`、`List<T>`）均在 Java 泛型系统能力范围内。

**[通过]** `DialogueSession` 不可变 class + copy-on-write（`withNewRound()` 返回新实例）在 Java 中可行——`final` class、`final` 字段、defensive copying 是标准模式。

**[通过]** enum 实现接口（如各模块 ErrorCode enum implements 已有 ErrorCode 接口）是项目中已验证的模式。

**[通过]** interface + 多实现（TriageService、LocalRuleEngine、TemplateConfigManager 等）完全支持。

### 2. 标准库与生态覆盖

**[通过]** `ConcurrentHashMap`、`ScheduledExecutorService`、`CompletableFuture` 均为 Java 标准库提供。

**[通过]** Spring Data JPA 覆盖实体持久化需求；Spring MVC 覆盖 REST 端点；Caffeine 缓存（`refreshAfterWrite`）是广泛使用的 Spring 兼容方案，生态成熟。

**[通过]** `@Async` / `CompletableFuture.runAsync()` 在 Spring 生态中良好支持，Feasible。

**[通过]** `spring-boot-starter-validation` 覆盖入参校验需求；Spring Security 覆盖管理员端权限控制。

**[通过]** 设计引用的 AiService 接口方法名（`triage()`、`prescriptionCheck()`、`generateMedicalRecord()`、`prescriptionAssist()`）与 `ai-api` 模块中现有契约完全匹配。

### 3. 语言特性可行性

**[通过]** 错误处理复用已有 `BusinessException` + `ErrorCode` + `GlobalExceptionHandler` 体系，各模块自定义 ErrorCode enum 实现 ErrorCode 接口，符合项目现有模式。

**[通过]** 并发设计（`ConcurrentHashMap` 存储 session、前端同 session 串行请求消除写竞争、`ScheduledExecutorService` 定时清理、`CompletableFuture` 编排 AI 调用）均与 Java 并发模型兼容。

**[通过]** 资源管理方案（30 分钟 TTL 自动过期清理、`ConcurrentHashMap` 配合定时清理线程避免内存泄漏）在实践中可行。

**[通过]** 模块/包结构（flat module + api/dto/service/repository/entity/converter 分包）与已有 patient/doctor/admin 模块完全一致，符合项目组织规范。

### 4. 设计一致性

**[一般]** `DosageStandard` 实体放置位置与写入权限归属矛盾。该 JPA 实体定义在 `prescription` 模块中（`prescription/src/.../entity/DosageStandard.java`），但设计明确声明"管理员模块作为唯一写入者"（§8 设计决策）、"处方审核和辅助开方子域仅持有读取权限"（§3.4.4）。管理员模块若要运行时写入该实体，必须能访问该实体类或其 Repository——这将产生 `admin → prescription` 模块的编译期依赖，直接违反 §2.2 中"三个新模块之间不允许互相依赖"的约束。仅靠 SQL 种子脚本可完成初始加载，但无法满足运行期 CRUD 管理需求。

**[通过]** 其他协作关系形成了完整闭环：分诊场景的 Controller → Service → AiService/RuleEngine/DepartmentFallbackProvider → DialogueSessionManager；审核场景的 Controller → Service → AiService/LocalRuleEngine → AuditRecord；病历生成场景的 Controller → Service → TemplateConfigManager/MissingFieldDetector；辅助开方场景的 Controller → Service → DosageThresholdService + AiService（异步）+ 查询端点。

**[通过]** 模块间依赖方向合理，无循环依赖。三个新模块均只依赖 `common`、`common-module-api`、`ai-api`，互相不依赖。

**[通过]** 行为契约描述完整，足以指导后续实现（包含正常路径、AI 降级路径、会话过期处理、模板兜底策略、异步结果查询流程）。

**[通过]** 各模块 ErrorCode 按前缀分段（`TRIAGE_`、`RX_AUDIT_`、`MR_`、`RX_ASSIST_`），与已有异常处理框架一致。

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则：每个接口/类有明确的职责边界（TriageService 负责分诊流程编排、DialogueSessionManager 负责会话生命周期、TriageRuleEngine 负责规则匹配、DepartmentFallbackProvider 负责兜底回退等）。

**[通过]** 抽象层次恰当——interface 用于确实需要扩展性的场景（TriageService、TriageRuleEngine、TemplateConfigManager 等），concrete class 用于逻辑稳定的场景（DialogueSessionManager、DosageThresholdService、DosageAlert）。

**[通过]** 设计便于后续的详细设计和实现——清晰的分包结构、协作描述和行为契约提供了较完整的实现指引。

**[通过]** 设计便于单元测试——核心业务逻辑通过 interface 定义（可 Mock），规则独立实现（可单独测试），静态兜底/内存存储可隔离测试。

**[轻微]** `DosageStandardRepository` 声明为"仅读操作"但未指定 Repository 基接口类型。若直接继承 `JpaRepository` 将自动暴露 `save()`/`delete()` 等写方法。建议在详细设计阶段限制为继承 `Repository<DosageStandard, Long>` 并仅声明查询方法，或使用只读接口模式。

**[轻微]** 包E 异步 AI 建议结果暂存类型（`ConcurrentHashMap<String, AiSuggestionResult>`）中的 `AiSuggestionResult` 未在核心抽象或 DTO 中定义。建议在 DTO 层补齐该类型定义，明确其字段（suggestion、status、createTime 等）。

## 修改要求（REJECTED 时存在）

### 一般问题

- **问题**：`DosageStandard` 实体位于 `prescription` 模块内，但设计指定管理员模块为该实体的唯一写入者。管理员模块在运行期进行 CRUD 操作时需要访问该实体或其 Repository，将产生 `admin → prescription` 的编译依赖，违反模块间互相不依赖的约束（§2.2）。
- **原因**：该设计矛盾导致管理员模块无法在不违反模块依赖规则的前提下对该实体执行写入操作，DosageStandard 的管理功能无法按设计描述落地。
- **建议方向**：将 `DosageStandard` 实体迁移至 `common` 模块或 `common-module-api` 共享契约模块，使管理员模块和 prescription 模块均可引用而不产生跨模块依赖；或由 admin 模块独立持有映射到同一表的实体定义。
