# R2.3: 跨模块事件 + 门面（Facade）模式 深度审查

审查时间：2026-06-29

### 审查范围

- `common-module-api/doctor/DoctorFacade.java`
- `common-module-api/doctor/AvailableDoctor.java`
- `common-module-api/drug/DrugFacade.java`
- `common-module-api/drug/DrugInfo.java`
- `common-module-api/visit/VisitFacade.java`
- `common-module-api/event/RegistrationEvent.java`
- `consultation/event/RegistrationEventListener.java`
- `consultation/service/DeadLetterCompensationService.java`
- `consultation/config/SchedulingRetryConfig.java`
- `consultation/entity/DeadLetterEvent.java`
- `consultation/repository/DeadLetterEventRepository.java`
- `consultation/service/impl/TriageServiceImpl.java`
- `consultation/entity/TriageRecord.java`
- `consultation/repository/TriageRecordRepository.java`
- `medical-record/service/impl/MedicalRecordServiceImpl.java`
- `prescription/service/audit/impl/PrescriptionAuditServiceImpl.java`
- `prescription/service/assist/impl/PrescriptionAssistServiceImpl.java`
- `consultation/event/RegistrationEventListenerTest.java`
- `Docs/07_ood_phase2_C_3_DE.md`（参照设计文档）

### 发现

#### [严重] RegistrationEventListener @Retryable 异常范围与设计不符

- **位置**：`consultation/event/RegistrationEventListener.java:36`
- **描述**：`@Retryable(retryFor = Exception.class)` 对所有异常类型均重试，设计文档 §2.2 明确要求"仅对 DataAccessException、TimeoutException 等可治愈临时异常触发重试；对 IllegalArgumentException、NullPointerException 等不可治愈异常（不可重试类型）直接进入 @Recover 回调"。当前配置会导致不可治愈异常也浪费 3 次重试资源。
- **建议**：将 `retryFor` 限定为 `{DataAccessException.class, TimeoutException.class}`，并通过 `noRetryFor` / `excludes` 排除 `{IllegalArgumentException.class, NullPointerException.class}`。

#### [严重] 7 类领域事件驱动缓存刷新全部缺失（0/5 Phase 2/3 已实现）

- **位置**：`Docs/07_ood_phase2_C_3_DE.md:350-356`
- **描述**：设计文档 §2.2 领域事件统一目录列出 7 类事件。Phase 2/3 应实现的 5 类 ChangeEvent（`TriageRuleChangeEvent`、`TemplateConfigChangeEvent`、`DrugContraindicationChangeEvent`、`DrugAllergyMappingChangeEvent`、`DrugCompositionDictChangeEvent`）在代码库中均未定义。事件消费端（`DefaultTriageRuleEngine` 各规则类、`TemplateConfigManager`）对应的 `@EventListener` 方法和 Caffeine `invalidate()` 也均不存在。这意味着设计文档 §9 所述"事件驱动缓存刷新 + 定时刷新双重失效策略"完全未落地，5 类规则/模板数据源变更的实时性依赖于初始种子数据从不变化。
- **建议**：在 `consultation/event/` 和 `prescription/event/` 下分别定义 5 个 ChangeEvent 类；在 `DefaultTriageRuleEngine`、`DatabaseTemplateConfigManager` 及各规则类中添加 `@EventListener` 消费方法并调用 Caffeine `invalidate()`；消费端增加 Caffeine `refreshAfterWrite(60s)` 定时刷新兜底。此问题为 R1.2"规则变更事件缺失"确认的跨切面严重问题。

#### [严重] TriageRecord 同 sessionId 重新分诊将导致数据库约束违反

- **位置**：`consultation/service/impl/TriageServiceImpl.java:185-216`
- **描述**：`saveTriageRecord()` 每次创建 `new TriageRecord()`（无主键 ID，由 `@GeneratedValue` 自增）后调用 `save()`。TriageRecord.sessionId 标注 `@Column(unique = true)`。同一前端 sessionId 第二次发起分诊（如用户重试同一会话）时，JPA `save()` 执行 INSERT，sessionId 唯一约束冲突抛出 `DataIntegrityViolationException`。设计文档 §1.3 明确指出"同一 sessionId 第二次发起分诊时...通过 update 覆盖同 sessionId 记录（update 模式为推荐实现）"。
- **建议**：在 `saveTriageRecord()` 中先通过 `findBySessionId(sessionId)` 查询是否存在已有记录，若存在则复用（更新已有实体字段）而非创建新实例。

#### [严重] DoctorFacade 跨模块调用缺少降级保护

- **位置**：`consultation/service/impl/TriageServiceImpl.java:169-179`
- **描述**：`findDoctorsForDepartments()` 方法直接调用 `doctorFacade.findAvailableDoctorsByDepartment()` 未包裹 try-catch。设计文档 §4.1 明确要求"DoctorFacade 调用超时或异常时捕获并记录 WARN 日志，TriageResponse.doctors 置为空列表，不阻断分诊主流程"。当前实现若 DoctorFacade 抛出异常，异常将传播至 `triage()` 方法层，导致整个分诊请求失败（HTTP 5xx）。
- **建议**：在 `findDoctorsForDepartments()` 的 for 循环内对 `doctorFacade.findAvailableDoctorsByDepartment()` 调用包裹 try-catch，记录 WARN 日志并跳过该科室，确保其他科室的医生列表仍可正常返回。

#### [一般] RegistrationEventListener 未复用 TriageService.selectDepartment 方法

- **位置**：`consultation/event/RegistrationEventListener.java:38-44` vs `consultation/service/impl/TriageServiceImpl.java:146-158`
- **描述**：设计文档 §2.2 明确说明"RegistrationEventListener 通过 sessionId 关联找到 TriageRecord 后也调用此方法完成写入，避免重复逻辑"。当前实现直接在 Listener 中操作 `TriageRecordRepository` 完成写入（检查 `finalDepartmentId == null` → set → save），与 `TriageServiceImpl.selectDepartment(..., false)` 逻辑重复。且 `selectDepartment` 在记录不存在时抛出 `TRIAGE_SESSION_NOT_FOUND`，而 Listener 静默跳过。
- **建议**：Listener 注入 `TriageService`，调用 `selectDepartment(event.getSessionId(), event.getDepartmentId(), event.getDepartmentName(), false)`。这样复用业务层方法、统一错误处理逻辑。

#### [一般] DeadLetterCompensationService 缺少 EXPIRED 状态迁移

- **位置**：`consultation/service/DeadLetterCompensationService.java:42-44`
- **描述**：设计文档 §2.2 定义状态迁移规则：`FAILED →（补偿成功）→ COMPENSATED`；`FAILED →（重试耗尽/超出最大重试次数）→ EXPIRED`。当前 catch 块仅递增 `retryCount` 并重新保存，当 `retryCount >= maxRetryCount` 时记录应从 DB 查询结果中消失（`findByCompensableEvents` 含 `retryCount < maxRetryCount` 条件），但 state 始终为 `FAILED` 不会迁移至 `EXPIRED`，导致状态机不完整、运维无法区分"待重试"和"已放弃"的死信记录。
- **建议**：在 catch 块中 increment retryCount 后增加判断：若 `retryCount >= maxRetryCount`，将 state 设为 `"EXPIRED"`；否则保持 `"FAILED"`。同时记录最终失败日志。

#### [一般] eventPayload 序列化与设计文档不一致

- **位置**：`consultation/event/RegistrationEventListener.java:50-57`
- **描述**：设计文档 §2.2 定义 eventPayload 为"事件原始载荷（JSON 文本，序列化的 RegistrationEvent）"——即完整的事件对象序列化。当前 `recover()` 方法仅手工构造含 `sessionId`/`departmentId`/`departmentName` 三个字段的 `HashMap`。缺缺少 `registrationId`、`patientId`、`doctorId`、`eventTime` 等字段，若未来补偿逻辑需扩展依赖这些字段，需修改序列化格式。
- **建议**：直接使用 `objectMapper.writeValueAsString(event)` 序列化完整 `RegistrationEvent` 对象；反序列化时使用 `objectMapper.readValue(payload, RegistrationEvent.class)` 还原。

#### [一般] DrugFacade 未在 prescription 模块注入（R1.2 已知严重问题跨切确认）

- **位置**：`prescription/service/audit/impl/PrescriptionAuditServiceImpl.java:46-71`、`prescription/service/assist/impl/PrescriptionAssistServiceImpl.java:37-64`
- **描述**：两个 Service 类的构造器均未注入 `DrugFacade`。设计文档 §2.2 明确要求 DrugFacade 供 prescription 模块查询药品名称/规格信息，"调用超时或 DrugFacade 抛出异常时，调用方（PrescriptionAssistServiceImpl、PrescriptionAuditServiceImpl）捕获异常并返回空药品信息 + WARN 日志，不阻断主流程"。当前两个 Service 完全未引用 DrugFacade。此问题从 R1.2 即已发现，本轮跨切视角确认：**根因**为 DrugFacade API 已就绪但调用方实现未完成；**影响域**为 AI 返回的药品名称/规格信息缺少本地数据校验，可能将无效药品编码传递至审核/开方流程。
- **建议**：在两个 Service 的构造参数中注入 `DrugFacade`；在组装 ai-api 层 Request DTO 前通过 `DrugFacade.findByDrugCode()` 校验药品存在性；未找到时记录 WARN 日志并跳过该药品（不阻断全流程）。

#### [轻微] RegistrationEvent 字段计数偏差

- **位置**：注册目标描述与 `common-module-api/event/RegistrationEvent.java:7-13`
- **描述**：审查目标要求检查"9 个字段齐全性"，但设计文档 §1.3 和 §2.2 均定义 7 个字段（`registrationId`、`patientId`、`sessionId`、`departmentId`、`departmentName`、`doctorId`、`eventTime`），代码实现也恰为这 7 个字段。不存在 9 个字段的规格来源。
- **建议**：无（审查目标描述不准确，设计和代码一致）。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 4 |
| 一般 | 4 |
| 轻微 | 1 |

### 总评

**3 个门面接口契约**：DoctorFacade / DrugFacade / VisitFacade 方法签名和返回类型均与设计文档一致，`AvailableDoctor` 和 `DrugInfo` 使用 Java 16+ record 类型定义，简洁且契约明确。

**RegistrationEvent 事件契约**：7 个字段齐全，`sessionId` 可选字段和 `departmentName` 由发布端填充的设计语义清晰，代码实现正确。

**RegistrationEventListener 重试与补偿**：核心链路（@EventListener → @Retryable → @Recover 死信写入 → DeadLetterCompensationService 定时补偿）骨架完整且 `RegistrationEventListenerTest` 覆盖了 4 个测试用例覆盖了正常更新、已覆盖跳过的、记录不存在、死信写入等场景。**但存在 3 项设计偏差**严重削弱了健壮性：(1) `retryFor = Exception.class` 重试所有异常违反设计指定的可治愈/不可治愈异常区分原则；(2) Listener 未复用 `TriageService.selectDepartment` 导致逻辑重复；(3) eventPayload 未完整序列化事件对象。

**领域事件缓存刷新全面缺失**（最严重的系统性缺口）：5 类 Phase 2/3 ChangeEvent + 对应的 `@EventListener` 消费端 + Caffeine invalidate 全部未实现。设计文档 §9 所定义的双重失效策略（事件驱动 + 定时刷新）完全未落地。虽然在 Phase 2/3 种子脚本初始化后管理员不修改数据时不影响功能，但这是生产环境上线前的必须补齐项。

**DoctorFacade 降级保护缺失**：TriageServiceImpl 的门面调用无 try-catch，违反设计文档的明确降级规范。

**DrugFacade 未注入**（R1.2 确认的已知问题）：跨切视角确认影响域限于药品编码本地校验缺失，不影响正常 AI 返回结果下的功能路径。

**DeadLetter 补偿状态机不完整**：缺少 EXPIRED 状态迁移，运维可观测性不足。

**变更建议优先级**：
1. 立即修复：@Retryable 异常范围限定（严重-阻断性）
2. 立即修复：TriageRecord 同 sessionId 约束冲突（严重-阻断性）
3. 立即修复：DoctorFacade 降级保护 try-catch（严重-阻断性）
4. 上线前补齐：5 类 ChangeEvent + 消费端缓存刷新（严重-系统性缺口）
5. 上线前修复：RegistrationEventListener 复用 selectDepartment（一般-重构）
6. 上线前修复：eventPayload 完整序列化（一般-健壮性）
7. 上线前修复：DrugFacade 注入（一般-完整性）
8. 可优化：EXPIRED 状态迁移（一般-运维可观测性）
