# 诊断质询报告（v2）

## 质询结果

CHALLENGED

## 逐维度审查

### 1. 证据充分性

**[问题-严重]** C01：诊断声称 TriageRecord 实体缺失 `correctedChiefComplaint` 快照字段，称"TriageRecord.java:14-144 实体定义中缺少该字段"。实际代码 `TriageRecord.java:41-42` 已定义 `@Column(columnDefinition = "TEXT") private String correctedChiefComplaint;`，并有完整 getter/setter（第148-154行）。诊断的核心结论——"实体无此属性，编译期应报错或运行时丢失数据"——与实际代码完全矛盾。此为 P0 级问题，错误诊断将导致修复者实施不需要的修复。

**[问题-严重]** C02：诊断声称 TriageRecordRepository 缺少 `findTopBySessionIdOrderByTriageTimeDesc` 方法。实际代码 `TriageRecordRepository.java:25` 已定义 `Optional<TriageRecord> findTopBySessionIdOrderByTriageTimeDesc(String sessionId);`。DialogueSessionManager.java:59-60 也正常调用了此方法。诊断结论与实际代码完全矛盾，且此为 P0 级问题。

**[问题-严重]** C19：诊断称"saveTriageRecord 未读取 session.getCorrectedChiefComplaint()"并称"TriageRecord 实体缺少此字段（C01），赋值无效"。实际代码 TriageServiceImpl.java:271 已调用 `record.setCorrectedChiefComplaint(session.getCorrectedChiefComplaint())`，且 TriageRecord 实体已有该字段。赋值完全有效。此条依赖 C01 的错误诊断，导致结论连锁错误。

**[问题-一般]** C08：诊断称"selectDepartment 签名为 4 参"，但又说实现类为 3 参，结论为"需确认接口是否仍为 4 参"。实际代码 TriageService.java:10 接口定义明确为 3 参，TriageServiceImpl.java:192-193 实现也为 3 参，RegistrationEventListener.java:47 调用也是 3 参，TriageController.java:31-33 也是 3 参。整个调用链均为 3 参，不存在签名不匹配问题。诊断应确认实际代码状态而非留下悬念。

**[问题-一般]** M01：诊断声称 MedicalRecordErrorCode 缺少 4 个错误码（MR_GEN_AI_UNAVAILABLE/MR_GEN_AI_INPUT_INVALID/MR_GEN_AI_OUTPUT_INCOMPLETE/MR_GEN_TEMPLATE_LOAD_FAILED）。实际代码 MedicalRecordErrorCode.java:6-12 已定义全部 4 个错误码。此条与实际代码完全矛盾，应标记为已修复而非仍作为待修问题列出。

**[问题-一般]** P16：诊断声称 AuditRecordRepository 缺少 `findByPrescriptionOrderIdAndIsLatestTrue` 方法（List 版本）。实际代码 AuditRecordRepository.java:23 已定义 `List<AuditRecord> findByPrescriptionOrderIdAndIsLatestTrue(String prescriptionOrderId);`。方法存在，诊断结论与代码矛盾。

**[问题-一般]** C03/A04：诊断称"TriageConverter.java:63-65,107-109 的转换逻辑需验证是否正确处理 correctedChiefComplaint 字段"。此条仅提出验证需求而未完成验证，根因判定停留在假设阶段。对于一个 P0 级问题，应实际验证代码行为而非留待后续确认。

**[问题-轻微]** C05：诊断正确识别出互斥校验逻辑已存在，但以"错误码需确认是否已定义"为由保留此条。实际代码 TriageErrorCode.java:7 已定义 `TRIAGE_FIELD_COMBINATION_INVALID`。此条应明确标记为已修复。

**[问题-轻微]** A10：诊断声称 application.yml 缺少配置项，但又承认已配置 ai.timeout.*/doctor-facade.timeout/visit-facade.timeout/mock.* 等。诊断将"缺少 facade.*.timeout 统一前缀配置"作为未修复项，但 todo.md 中的原始描述是"完全缺失配置项"，而实际配置已存在。诊断的根因判定与实际不符。

### 2. 逻辑完整性

**[问题-严重]** C01→C19→C03 数据流群组的因果链断裂：诊断将 C01→A04→C03→C19→C23 作为一个 correctedChiefComplaint 数据流群组，以 C01（实体缺字段）为群组起点，推导整个数据流群组失效。但 C01 的前提（实体缺字段）已被证伪，整个群组的因果链基础不成立。C19 的结论"赋值无效"依赖 C01，同样不成立。诊断未独立验证群组内各节点的实际状态，导致整个数据流群组被错误判定为失效。

**[问题-一般]** E02：诊断推导了"并发请求同一 sessionId → findBySessionId 在事务外执行 → 两次均未查到 → 两次 INSERT → 唯一约束冲突"的因果链。但 TriageRecordRepository.java:19-20 的 `findBySessionId` 方法标注了 `@Lock(LockModeType.PESSIMISTIC_WRITE)` 和 `@Transactional(propagation = Propagation.MANDATORY)`，这意味着调用此方法必须在事务内执行，否则会抛出 `IllegalTransactionStateException`。而 saveTriageRecord 使用 `transactionTemplate.execute()` 包围整个逻辑（TriageServiceImpl.java:257），findBySessionId 在事务内执行，且带有悲观写锁。诊断的因果链中"findBySessionId 在事务外执行"的假设不成立，并发场景下两个线程不会同时绕过 findBySessionId 直接 INSERT。

**[问题-一般]** S04：诊断称 restoreSession 无 synchronized，并发 restoreSession 可能在第54行同时读到 null，然后第68行同时 put 不同 session 实例。此分析合理，但诊断未提及 restoreSession.java:59-60 调用了 `findTopBySessionIdOrderByTriageTimeDesc`，该方法标注 `@Transactional(propagation = MANDATORY)`，在非事务上下文（如定时任务或事件监听器）中调用 restoreSession 时会直接抛出异常。诊断对影响范围的判定——"restoreSession 可被并发调用导致数据丢失"——忽略了此约束条件，影响范围可能被高估。

**[问题-轻微]** C04：诊断称 triage 方法 `@Transactional` 事务粒度过大，应移除仅依赖 TransactionTemplate。此逻辑方向正确，但未分析移除 `@Transactional` 后对 selectDepartment 方法的影响——selectDepartment 标注了 `@Transactional`，若 triage 内部调用 selectDepartment（当前代码不存在此调用路径，但未来可能存在），事务边界需重新评估。此为轻微遗漏。

### 3. 覆盖完备性

**[问题-一般]** 多个已修复项被当作未修复问题列入诊断报告：C01（TriageRecord 已有 correctedChiefComplaint 字段）、C02（Repository 已有恢复查询方法）、M01（错误码已定义）、P16（Repository 已有 List 版本查询方法）。这些项在 todo.md 中列为待修复，但代码中实际已修复。诊断报告未将它们标记为已修复，而是继续以"缺失"作为根因，导致诊断覆盖了不存在的"问题"而忽略了真正需要关注的问题。

**[问题-一般]** C22：诊断仅称"需验证 TriageController.java:34 的实现"，未完成验证。实际代码 TriageController.java:30-34 的 selectDepartment 端点接收 3 个参数（sessionId, departmentId, departmentName），不存在"写死 overwrite=true"的问题（因为没有 overwrite 参数）。此条应完成验证并给出明确结论，而非留待后续。

**[问题-轻微]** 诊断报告对多个条目标注"已修复"（C07、C15、C21、C09、C14、P06、P07、A02、M06、M09、C11、A07），但仍有部分已修复项（C01、C02、M01、P16、C05）未被识别为已修复。覆盖完备性受到已修复项识别不完整的影响。

**[问题-轻微]** T44（TriageResponse 缺少 correctedChiefComplaint 字段）在 P2 中列出，但既然 TriageRecord 实体已有此字段，且 TriageServiceImpl.java:271 已从 session 读取并设置到 record，前端是否能感知 AI 修正取决于 TriageResponse 是否包含此字段以及 TriageConverter 是否映射。此问题与 C01 数据流群组相关，但诊断未将 T44 纳入 correctedChiefComplaint 数据流群组统一分析。

## 质询要点

### 1. C01/C02/C19 核心事实错误

- **问题**：C01 声称 TriageRecord 缺少 correctedChiefComplaint 字段，C02 声称 TriageRecordRepository 缺少恢复查询方法，C19 声称赋值无效。实际代码中三者均已修复
- **原因**：诊断未验证当前代码的实际状态，直接基于 todo.md 的原始描述推导根因。三个 P0 级问题的核心前提与代码矛盾，导致整个 correctedChiefComplaint 数据流群组的诊断结论不可信
- **建议方向**：重新审视 todo.md 中所有"缺失"类条目，逐条对照实际代码确认当前状态。将已修复项明确标记，重新评估 correctedChiefComplaint 数据流群组的实际影响

### 2. M01/P16 错误码和 Repository 方法事实错误

- **问题**：M01 声称 4 个错误码缺失，P16 声称缺少 List 版本查询方法。实际代码中均已存在
- **原因**：诊断未验证代码当前状态，基于原始问题描述推导结论
- **建议方向**：逐条对照 MedicalRecordErrorCode.java 和 AuditRecordRepository.java 验证当前代码，将已存在项标记为已修复

### 3. E02 因果链中 findBySessionId 事务隔离假设不成立

- **问题**：诊断推导"findBySessionId 在事务外执行"导致唯一约束冲突，但 findBySessionId 标注了 `@Transactional(propagation = MANDATORY)` + `@Lock(PESSIMISTIC_WRITE)`，在 transactionTemplate 包围的上下文中执行时具有事务和锁保护
- **原因**：诊断未检查 TriageRecordRepository.findBySessionId 的事务注解和锁注解，导致因果链中关键环节与代码实际行为不符
- **建议方向**：重新分析 E02 的并发场景，考虑 findBySessionId 的 PESSIMISTIC_WRITE 锁和 MANDATORY 事务传播行为。若并发场景下 findBySessionId 在事务+锁保护下执行，唯一约束冲突的可能性需重新评估

### 4. C08/C22 未完成验证

- **问题**：C08 称"需确认接口是否仍为 4 参"，C22 称"需验证 TriageController.java:34"。实际代码均可直接确认：接口为 3 参，Controller 无 overwrite 参数
- **原因**：诊断以"需验证"替代实际验证，导致 P0/P1 级问题的根因判定悬而未决
- **建议方向**：完成代码验证，给出明确结论。C08 应标记为已修复（接口与实现均为 3 参，无签名不匹配），C22 应明确 TriageController 不存在写死 overwrite 的问题

### 5. 已修复项识别不完整导致覆盖偏差

- **问题**：诊断报告将部分已修复项正确标注（如 C07、C15），但遗漏了 C01、C02、M01、P16、C05 等已修复项，继续以"缺失"作为根因
- **原因**：诊断过程未系统性对照代码验证每一条"缺失"类描述，而是选择性验证部分条目
- **建议方向**：对 todo.md 中所有"缺失"类条目（实体缺字段、接口缺方法、缺错误码等）进行系统性代码验证，将已存在项明确标记为已修复，聚焦于真正未修复的问题
