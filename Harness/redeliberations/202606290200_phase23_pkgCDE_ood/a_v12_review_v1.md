# OOD 设计方案审查报告（v29）

## 审查结果

APPROVED

## 逐维度审查

### 1. 类型系统可行性

**[通过]** 所有类型形态选择（interface 用于服务契约、class 用于 DTO/值对象/管理器、enum 用于固定分类、JPA @Entity 用于持久化实体）均与 Java 类型系统完全匹配。interface 的多实现、enum 的固定枚举、@Entity 的 ORM 映射均为标准用法。

**[通过]** CompletableFuture\<AiResult\<T\>\> 泛型使用方式在 Java 泛型系统能力范围内，T 分别绑定 4 个 ai-api Response DTO，编译期类型安全。

**[通过]** 并发控制使用 ConcurrentHashMap.compute() 原子替换、volatile 策略存储、JPA @Version 乐观锁，均为 Java 标准并发原语，无类型系统风险。

### 2. 标准库与生态覆盖

**[通过]** 设计依赖的标准库基础设施齐全：Spring Boot（@Service/@Controller/@Scheduled/@Retryable/@TransactionalEventListener）、JPA/Hibernate（@Entity/@Repository/@Version/@Lock）、Jackson（JSON 序列化/AttributeConverter）、CompletableFuture、ConcurrentHashMap、Caffeine 缓存——以上均为 Spring Boot 生态标准组件，项目中已有使用。

**[通过]** ScheduledExecutorService 统一管理通过 Spring TaskScheduler 覆盖，优雅关闭通过 @PreDestroy，均为标准生命周期管理。

**[通过]** MockAiService 通过 @Profile("mock") 条件注入，生产环境不激活，属于标准 Profile 隔离模式。

### 3. 语言特性可行性

**[通过]** 错误处理策略兼容——统一错误格式 + GlobalExceptionHandler + ErrorCode 接口 + BusinessException 体系是 Spring Boot 标准实践；BLOCK 阻断使用独立的 BlockResponse + HTTP 422，与业务异常体系正交，设计合理。

**[通过]** 并发设计兼容——主流程同步等待 CompletableFuture，异步 AI 建议通过 @Async/CompletableFuture.runAsync()，定时任务通过 Spring @Scheduled 统一管理，资源清理通过 TTL + 定时扫描，均为 Java 并发模型的标准用法。

**[通过]** 模块/包结构使用扁平 Maven 模块（无 api/impl 子模块），与 patient/doctor/admin 结构一致，符合项目组织方式。

### 4. 设计一致性

**[通过]** SubmitRequest/SubmitResponse DTO 在 §1.3 核心抽象表、§3.2 详细定义、§4.2 行为契约、§4.6 JSON 示例之间保持完全的字段和行为一致性。auditRecordId 必填/可选语义通过边界表清晰定义。

**[通过]** VisitIdReconciledTask 在 §6.1 中完成定义（调度时机、触发条件、实现机制、负责模块），§3.3 RecordGenerateRequest 中 visitIdFallback 标记指向此任务，形成闭环。

**[通过]** DialogueSession 事务一致性策略在 §3.1 扩展为完整的影响评估表，覆盖 aiFailCount/QA 历史/correctedChiefComplaint/roundCount 四项可变状态，逐项评估可接受性并给出补充保障措施（TriageRecord 快照 + 恢复查询）。

**[通过]** AiResultFactory 在 §2.1 目录结构中注册（含包路径），§2.3 给出四个静态工厂方法的完整签名，§1.1c 正确引用兼容性方案，§10 跨阶段风险段落同步更新。

**[通过]** 领域事件统一目录在 §2.2 末尾完整列出 8 种事件的定义位置、发布/消费模块、触发条件、事务边界、Phase 范围。

**[通过]** 错误码→HTTP 状态码映射表在 §5.1 集中声明覆盖 8 类映射路径，错误码表每行新增 HTTP 状态码列。

**[通过]** Level 2 决策表在 §8.4 完整给出 12 行关键路径 + 6 项边界测试用例，覆盖 16 种 null/非 null 组合的关键降级路径。

**[通过]** MockAiService 并发安全在 §2.3 定义 volatile 策略存储 + 局部变量快照，声明"正在执行调用不受策略切换影响"。

**[通过]** 错误格式边界在 §4.6 明确步①/步②→BlockResponse(422)、步③→统一错误格式(409)的划分。

### 5. 设计质量

**[通过]** 所有抽象具有明确的单一职责：SubmitRequest/SubmitResponse 仅承载提交交互契约，SubmitContext 仅作为调用栈内的 CRITICAL 快照值对象，SubmitResponse 聚合多路径返回结果而非让调用方自行拼装。

**[通过]** 抽象层次恰当：DTO 仅描述数据载体不包含业务逻辑，Service interface 定义业务契约不涉及实现细节，策略存储通过 Store 接口隔离不耦合具体后端。

**[通过]** 设计便于实现和测试：所有 Service 均为 interface 可 mock，Converter 将映射逻辑集中便于单测，Store 接口可替换实现便于集成测试切换。

**[通过]** 设计遵循项目既有模式：扁平模块、门面接口在 common-module-api 中定义、事件通过 ApplicationEvent 跨模块传播、错误处理复用 GlobalExceptionHandler。

## 修改要求（REJECTED 时存在）

（无 — 设计方案通过审查，无严重或一般问题）
