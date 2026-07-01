# 质量审查报告

**产出文件**：`a_v8_copy_from_v7.md`
**审查视角**：需求响应充分度、事实/逻辑一致性、深度与完整性（侧重内部审议未覆盖维度）
**审查轮次**：第 8 轮

---

## 问题清单

### P1. [严重] 事实错误：DeadLetterCompensationService 补偿路径缺少 departmentName

**问题描述**：DeadLetterCompensationService 直接调用 `TriageService.selectDepartment(sessionId, departmentId, departmentName)` 完成补偿，但该方法的调用数据来源于 dead_letter_event 表中序列化的 RegistrationEvent 载荷。RegistrationEvent 定义（§1.3 "跨模块门面与事件"表）仅包含 `registrationId / patientId / sessionId / departmentId / doctorId / eventTime` 六个字段，**不含 departmentName**。补偿任务从事件载荷中反序列化后无法提供 departmentName，导致 `selectDepartment` 调用必然因缺少 departmentName 而失败。

**所在位置**：§2.2 第 320 行（DeadLetterCompensationService 补偿描述）；§1.3 RegistrationEvent 字段定义（line 144）；§3.1 TriageService.selectDepartment 方法定义（line 408）

**严重程度**：严重

**改进建议**：二选一修正路径：(a) RegistrationEvent 补充 departmentName 字段（String，必填），在事件发布时由 registration 模块从挂号请求的 departmentId 关联获取后填充；或 (b) TriageService.selectDepartment 方法增加仅基于 departmentId 的重载版本（如 `selectDepartment(sessionId, departmentId)`），内部通过部门字典查询 departmentName。建议采用路径 (a)，因为最终写入 TriageRecord.finalDepartmentName 也需要此数据。

---

### P2. [严重] 逻辑矛盾：AI 输入校验错误码无消费路径

**问题描述**：§5.1 错误码表定义了 4 个 `_AI_INPUT_INVALID` 类错误码（TRIAGE_AI_INPUT_INVALID、RX_AUDIT_AI_INPUT_INVALID、MR_GEN_AI_INPUT_INVALID、RX_ASSIST_AI_INPUT_INVALID），但全文中未定义任何"AI 输入校验"的触发位置、校验规则或错误码生产者。AiService 接口定义（§2.3）中 4 个方法仅接收 ai-api 层 DTO 并返回 `CompletableFuture<AiResult<T>>`，未提及输入校验。各业务 Service 的降级路径描述也未涉及此错误码。这些错误码在系统中处于"已定义但无生产者"的状态。

**所在位置**：§5.1 错误码表（lines 1261-1268）；§2.3 AiService 接口定义（lines 330-357）

**严重程度**：严重

**改进建议**：二选一：(a) 在 §2.3 的 AiService 方法契约中补充输入校验职责归属——明确由 ai-impl 模块在生产实现中校验并产出这些错误码，各业务 Service 按 `AiResult.errorCode` 消费；或 (b) 若 Phase 2/3 不实现 AI 输入校验，则将这些错误码标注为"Phase 4 预留"并从当前错误码表中移除，避免误导。

---

### P3. [严重] 深度不足：业务 Service 接口缺少显式方法签名

**问题描述**：§2.3 的 AiService 接口有完整的 Java 方法签名定义（方法名、参数类型、返回类型、泛型绑定、超时配置），但核心业务 Service 接口（TriageService、PrescriptionAuditService、MedicalRecordService、PrescriptionAssistService）仅在 §3.x 中以自然语言描述职责和行为，**没有给出任何方法签名**。编码实现者需要自行推断每个接口应暴露哪些方法、参数类型和返回值类型，不同实现者可能产生不同的接口设计。

**所在位置**：§3.1 TriageService（lines 382-412）；§3.2 PrescriptionAuditService（lines 480-486）；§3.3 MedicalRecordService（lines 596-606）；§3.4 PrescriptionAssistService（lines 620-633）

**严重程度**：严重

**改进建议**：为四个业务 Service 接口补充显式方法签名，至少包括：方法名、主要参数类型、返回类型、抛出异常（如有）。例如 TriageService 至少应包含：`CompletableFuture<TriageResponse> consult(DialogueCreateRequest request)`、`TriageResponse selectDepartment(String sessionId, String departmentId, String departmentName)` 等。参考 §2.3 AiService 的方法签名格式。

---

### P4. [严重] 完整性缺口：patientId 兜底查询"最近分诊记录"缺少选择标准

**问题描述**：§2.2 及 §1.1a 描述当 RegistrationEvent.sessionId 为空时降级为"按 patientId 查询最近分诊记录进行关联"（line 22: "事件-driven finalDepartmentId 写入路径降级为按 patientId 关联最近分诊记录"）。但 TriageRecord 实体定义（§3.1，line 474-476）中未定义按 patientId 查询时按哪个字段排序选取"最近"记录——是按 triageTime 降序？按 recordId 自增降序？若同一患者在同一天内发起两次分诊（多见场景），"最近"的选择标准不明确可能导致错误的关联。

**所在位置**：§1.1a registration 模块行（line 22）；§2.2 RegistrationEvent 消费描述（line 304）；§3.1 TriageRecord 实体定义（line 474-476）；§2.1 TriageRecordRepository（line 172）

**严重程度**：严重

**改进建议**：(a) 在 TriageRecordRepository 中明确补充 `findTopByPatientIdOrderByTriageTimeDesc(String patientId)` 或等价查询方法的定义；(b) 在 §2.2 或 §3.1 中显式说明排序依据（建议按 triageTime DESC，以 triageTime 相同时按 recordId DESC 作为二次排序）；(c) TriageRecord 的 patientId 字段补充数据库索引说明。

---

### P5. [一般] 事实错误：RegistrationEvent 缺少 departmentName，TriageRecord.finalDepartmentName 无法赋值

**问题描述**：RegistrationEvent 定义（§1.3 跨模块事件表）包含 `departmentId` 但不含 `departmentName`。而 RegistrationEventListener 通过 selectDepartment 写入 TriageRecord 时最终需要填充 `finalDepartmentName`。即使修正 P1 的补偿路径，在 RegistrationEvent 正常消费路径（§2.2，line 302-304）下也存在同样问题——EventListener 从事件中只能获得 departmentId，无法获得 departmentName。

**所在位置**：§1.3 RegistrationEvent 字段定义（line 144）；§2.2 RegistrationEvent 消费路径（lines 302-304）

**严重程度**：一般

**改进建议**：RegistrationEvent 补充 `departmentName` 字段（String，必填），由 registration 模块在发布事件前通过 departmentId 查询名称后填充。同时 §2.2 事件传播描述中补充 departmentName 的填充路径说明。

---

### P6. [一般] 深度不足：@Retryable 配置未定义重试触发条件

**问题描述**：§2.2（line 306）描述 "RegistrationEventListener 采用 @Retryable（Spring Retry，重试间隔 2s，最多 3 次，exponential backoff）处理事件消费失败"，但未指定哪些异常类型触发重试。默认情况下 Spring Retry 对所有异常触发重试，但某些异常（如校验失败、非法参数）重试无法治愈，只会浪费资源并延迟死信写入。未定义 `include`/`exclude` 异常类型列表会导致实现者选择分歧。

**所在位置**：§2.2 RegistrationEvent 消费失败补偿策略（line 306）

**严重程度**：一般

**改进建议**：补充 `@Retryable` 的异常过滤器定义。建议：重试仅针对可治愈的临时异常（如 `DataAccessException`、`TimeoutException`、`OptimisticLockException`），`IllegalArgumentException` 等不可治愈异常直接进入 @Recover 路径。给出具体的异常类型列表或 exclude 策略。

---

### P7. [一般] 完整性缺口：TriageRecord.sessionId 缺少唯一约束定义

**问题描述**：§3.1（line 402）声明 "TriageRecord 按 sessionId 一对一映射"，§1.3 TriageRecord 注释也标注"按 sessionId 一对一映射"。但 §3.1 的 TriageRecord JPA @Entity 字段定义中 sessionId 仅为普通字段，未添加 `@Column(unique = true)` 或 uniqueConstraint 定义。若 delete+insert 模式在并发下失效（如第一条 insert 成功但 delete 因外键约束失败），sessionId 可能出现多条记录，破坏"一对一"语义。

**所在位置**：§3.1 TriageRecord 实体定义（line 474-476）

**严重程度**：一般

**改进建议**：在 TriageRecord 实体的 sessionId 字段或 `@Table` 注解中增加唯一约束定义（`@Column(unique = true)` 或 `@Table(uniqueConstraints = ...)`）。同时在 §3.1 的"重新分诊生命周期行为"段落（line 402）明确 update 模式为推荐实现（delete+insert 模式下唯一约束可防止并发重复插入）。

---

### P8. [一般] 完整性缺口：三处 ScheduledExecutorService 独立运行，缺少集中管理

**问题描述**：文档中三处地方各自使用独立的 ScheduledExecutorService 执行定时任务——DialogueSessionManager（§6.1，TTL 30 分钟清理，每 5 分钟扫描）、AiSuggestionResult（§3.4，line 659，TTL 60 分钟清理，每 5 分钟扫描）、PrescriptionDraftContext（§3.4，line 687，TTL 60 分钟清理，每 5 分钟扫描）、DeadLetterCompensationService（§2.2，line 320，每 30 分钟扫描）。每处均自行创建和管理 scheduler 线程，缺乏统一的线程池管理和优雅关闭机制。

**所在位置**：§3.4 AiSuggestionResult TTL 清理（line 659）；§3.4 PrescriptionDraftContext 生命周期（line 687）；§6.1 对话会话清理（line 1301）；§2.2 DeadLetterCompensationService（line 320）

**严重程度**：轻微

**改进建议**：引入统一的 `ScheduledTaskRegistry`（或使用 Spring 的 `@Scheduled` 注解 + `@EnableScheduling`），将所有定时任务委托给同一个 `ScheduledExecutorService` Bean 管理。各组件声明自身的调度需求（周期、任务逻辑），由框架统一接管线程池生命周期。建议在 §1.1 的设计目标或 §6 的并发设计章节中补充此项基础设施约定。

---

### P9. [轻微] 概念歧义：api/ 子包同时放置 Controller 和 DTO

**问题描述**：§2.1 目录结构中，各模块的 `api/` 子包同时包含 Controller 类和 DTO 类（如 prescription/api/ 下同时有 `PrescriptionAuditController.java` 和 `audit/PrescriptionAssistController.java`，以及 `dto/` 子目录）。通常 `api/` 包名约定为放置对外接口/控制器，DTO 放在独立的 `dto/` 包下。当前的层级将 Controller 直接放在 api 下、DTO 放在 api/dto 下，导致 api 包容纳两类职责不同的组件。此约定虽不影响编译但会造成新开发者理解的困惑。

**所在位置**：§2.1 prescription 模块目录结构（lines 180-193）

**严重程度**：轻微

**改进建议**：将 Controller 从 `api/` 平级提升或将 DTO 移至模块根的 `dto/` 包（与 consultation 模块的 `api/` + 平行的 `dto/` 结构保持一致）。当前 prescription 模块的 dto 是 api 的子目录（api/dto/...），而 consultation 模块的 dto 是与 api 平行的（consultation/dto/...），两模块风格不一致。

---

### P10. [轻微] 边界条件遗漏：SuggestionStore.createIfNotExists 的 Phase 2/3 实现细节

**问题描述**：§3.4（line 647）定义了 SuggestionStore 新增原子方法 `createIfNotExists(taskId, prescriptionId, supplier)`，但参数 `supplier` 的类型（`Supplier<AiSuggestionResult>` vs `Function<prescriptionId, AiSuggestionResult>`?）和 `prescriptionId` 在 ConcurrentHashMap 中的存储键策略未定义——是按 `taskId` 还是 `prescriptionId` 还是复合键做去重检查？仅描述"ConcurrentHashMap.compute() 在锁保护的函数内完成检查+创建"未提供足够的精度确保 Phase 2/3 实现与 Phase 5 的 Redis Lua 脚本语义一致。

**所在位置**：§3.4 "去重原子性保障"段落（line 647）

**严重程度**：轻微

**改进建议**：补充 `SuggestionStore.createIfNotExists` 方法的精确契约：明确 prescriptionId 的去重范围语义（同一 prescriptionId 下只能有一个 PENDING 或 COMPLETED 未消费的 task），明确 supplier 惰性调用的触发条件，补充 ConcurrentHashMapStore 实现的伪代码示例。同时标注 Phase 5 Redis 实现的等价 Lua 脚本模板，确保两个实现的语义一致。

---

## 整体评价

产出整体质量高，经过多轮内部审议（v8→v26 修订），在架构设计的完整性、边界条件覆盖和跨模块协作定义方面表现扎实。所有用户在需求文档中列明的功能需求均已覆盖。主要问题集中在以下三方面：

1. **数据模型字段一致性**（P1/P5）：RegistrationEvent 的字段定义与 DeadLetterCompensationService/RegistrationEventListener 的消费需求不匹配——事件缺少 departmentName 使得补偿路径和正常消费路径均无法完成 finalDepartmentName 写入。这是高优先级的事实错误。

2. **接口定义精度**（P3/P6）：业务 Service 接口缺乏方法签名、@Retryable 缺乏异常分类定义，导致编码实现者面临过多自由裁量空间。建议补充到与 §2.3 AiService 同等精度级别。

3. **补全路径标准化**（P2/P4）：降级/兜底路径中的关键决策点（AI 输入校验归属、patientId 关联的"最近"定义）未标准化，各实现者对相同场景可能做出不同实现决策。
