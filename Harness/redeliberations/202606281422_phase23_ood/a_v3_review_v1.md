# OOD 设计方案审查报告（v3）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 设计方案中所有类型形态选择（interface/class/enum/@Entity）均在 Java 类型系统能力范围内：
- Service 层统一使用 interface 定义业务契约，impl 类提供实现——标准 Spring Bean 多态模式
- AuditRiskLevel 使用 enum 定义固定三个风险等级——Java enum 完全胜任
- JPA @Entity 用于持久化实体（AuditRecord、MedicalRecord 等）——Spring Data JPA 标准用法
- Value object 以 class 形态封装（DosageAlert、AiSuggestionResult、DepartmentTemplateConfig 等）——恰当
- DialogueSessionManager 和 DosageThresholdService 以 class（而非 interface）形态提供——作者已说明理由（职责边界稳定、无多实现需求），设计层面可接受
- 泛型使用（CompletableFuture<AiResult<T>>）在 Java 泛型系统范围内
- 继承/实现关系符合 Java 单继承+多接口实现约束

### 2. 标准库与生态覆盖

**[通过]** 设计方案依赖的能力均在 Java 标准库或 Spring Boot 生态常用库覆盖范围内：
- ConcurrentHashMap + ScheduledExecutorService —— java.util.concurrent 标准库
- CompletableFuture / @Async —— Java 8+ / Spring 异步支持
- Caffeine 缓存 —— Spring Boot 项目广泛使用的本地缓存库
- Spring Data JPA —— 持久化标准方案
- ApplicationEventPublisher —— Spring 事件机制，标准方案
- GlobalExceptionHandler + ErrorCode + BusinessException —— Spring Boot 异常处理标准模式
- HTTP 422 —— Spring MVC 完全支持自定义状态码
- 无需引入非常规外部依赖

### 3. 语言特性可行性

**[通过]** 设计中的各种策略与 Java/Spring Boot 语言特性匹配：
- 错误处理策略：模块级错误码前缀 + GlobalExceptionHandler + BusinessException —— 与 Spring Boot 异常处理能力完全兼容；BLOCK 阻断绕过异常框架直接通过 Controller 返回 422 是合理的设计决策
- 并发设计：同 session 请求串行（ConcurrentHashMap + 前端等待）+ 不同 session 独立 —— 无需锁机制，可行；AI 调用统一 CompletableFuture 同步等待，包E 异步 AI 建议用 @Async/runAsync —— 标准 Java 并发工具
- 资源管理：内存存储 + TTL 定时清理 —— 对话短时完成场景可行；Phase 5 迁移数据库的演进路径已明确
- 模块/包结构：扁平 Maven 模块 + 包内按职责分层（api/service/repository/entity/dto/converter）—— 与项目中已有模块（patient/doctor/admin）风格一致
- 跨模块数据共享：DosStandard 迁移至 common 模块避免编译期依赖 —— 正确的 Maven 依赖管理策略
- 模块间依赖仅限 compile-scope 依赖 common、common-module-api、ai-api，无跨业务模块 impl 依赖 —— 干净

### 4. 设计一致性

**[通过]** 各抽象的职责描述清晰，协作关系完整，行为契约无缺失环节：
- 包C 分诊流程完整覆盖：consult → AiService → 规则回退 → 兜底科室 → 超时错误
- 包D-AI1 审核流程完整：审核 → AI/本地规则 → BLOCK/WARN/PASS → Enforcer 阻断 → Controller 422
- 包D-AI2 病历生成流程完整：模板查询 → AI 生成 → 缺失字段检测 → 分层保护降级
- 包E 辅助开方流程完整：剂量同步检查 → 异步 AI 建议 → 三分支查询
- AuditRecord 三个关联标识（prescriptionOrderId/doctorId/patientId）支撑完整的业务追溯
- 依赖方向合理无循环：三模块均只依赖 common/common-module-api/ai-api，互不依赖
- 行为契约（§4.1-§4.4）以伪代码形式描述端点语义、输入输出、降级路径，足以指导后续实现

### 5. 设计质量

**[通过]** 职责划分遵循单一职责原则，抽象层次恰当，便于测试和实现：
- 职责分离清晰：PrescriptionAuditEnforcer 仅负责 BLOCK 策略（不侵入审核核心流程）；MissingFieldDetector 仅做差集比对（不修改 AI 产出）；DialogueSessionManager 仅管理会话生命周期+并发控制
- 抽象层次合理：可能变化的行为提取为 interface（规则引擎、阻断策略、模板管理、缺失字段检测）；稳定行为使用 class 封装（剂量阈值查找逻辑稳定）
- 便于测试：interface 契约可 mock；独立 Rule（DrugInteractionRule/AllergyCheckRule/DosageLimitRule）可独立单元测试；MissingFieldDetector 的差集比对逻辑纯函数可测
- 设计决策记录完备（§7）：每个决策有选择项和理由，便于后续维护者理解
- 第 2 轮审查的 5 条反馈已全部正确修复（BLOCK 阻断机制、AuditRecord 关联标识、AiSuggestionResult 三分支+TTL、DosageStandard 年龄/体重分级+四级优先级、病历降级分层保护）
