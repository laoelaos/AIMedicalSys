# 质量审查报告 — Phase 23 C/3/DE 问题定位诊断报告（第 2 轮）

审查范围：`a_v2_diag_v1.md`（v3 诊断报告）
审查视角：需求响应充分度、事实准确性、整体深度与完整性、可操作性

---

## 1. 需求响应充分度

### 1.1 整体评价

诊断报告覆盖了用户需求的所有四项核心问题（真实性判定、根因分类、代码证据、修改建议方向），并通过 v3 修订完全吸纳了第 1 轮迭代的 4 项反馈。但存在以下不足：

### 问题 1 — P05/P09 的 OOD 文档修改方向未作为独立任务项标明

- **所在位置**：P05 节、P09 节
- **问题描述**：用户需求第 4 项明确要求"针对每个问题的修改建议：修改 OOD 文档还是修改编码实现"。报告通过"根因分类"字段间接回答了此问题，但对于 P05 和 P09，实际需要**同时**修改 OOD 文档和编码实现。报告将 P05 归为"实现编码问题"，但在证据描述中承认"warnResult 及其子字段应在正式字段定义中补充"——这表明 OOD §1.1a 的定义需要同步补充 field 级规范，但未将此作为一个独立的修改任务项列出。P09 同理，"OOD 应在 PrescriptionItem 定义中补充 unit"的 OOD 修改方向被混合在长段落描述中，执行者容易遗漏文档修改项。
- **严重程度**：中等
- **改进建议**：对每个双修项（同时涉及 OOD 文档和代码），拆分为两个独立任务项或明确标注 `[OOD 文档修改]` 和 `[代码修改]` 标签。例如 P09 应拆为：(1) OOD §1.3 补充 PrescriptionItem.unit 正式定义；(2) 代码端保留现有 unit 字段，加 `@Column` 注解（可选）。

---

## 2. 事实准确性

经过对关键证据的代码溯源（TriageServiceImpl.java、SubmitResponse.java、PrescriptionItem.java、TriageRecordRepository.java、PrescriptionDraftContext.java），报告引用的证据在事实上均准确。但存在以下问题：

### 问题 2 — C08 修复建议中"根据调用来源决定 overwrite"的方案不可行

- **所在位置**：C08 节，第 117 行
- **问题描述**：报告建议"TriageServiceImpl.selectDepartment() 根据调用来源（Controller vs RegistrationEventListener）决定 overwrite 行为"。此方案在实际 Java 代码中不可行——TriageServiceImpl 是一个被注入的单例 Service Bean，它在被调用时无法感知当前的调用者是谁（没有 Spring 提供的自动调用链上下文）。Controller 和 EventListener 都通过接口调用 selectDepartment，Service 实现无法区分调用来源。
- **严重程度**：中等
- **改进建议**：应采用 OOD §3.1（line 469）已描述的方案——RegistrationEventListener 在调用 selectDepartment *之前*自行检查 TriageRecord.finalDepartmentId 是否为空，仅当为空时调用；Controller 调用时始终覆盖写入。Service 接口保持 3 参（无 overwrite），Service 实现内部始终执行覆盖写入。由调用方控制"是否调用"，而非 Service 内部"根据来源决策"。

### 问题 3 — P05 证据中 OOD §4.6 示例中 warnResult 子字段描述不完整

- **所在位置**：P05 节，第 175 行
- **问题描述**：报告称"warnResult 内含 riskLevel/alerts/auditRecordId/prescriptionHash"，但 OOD §4.6 的响应示例（line 1273-1280）中 warnResult 实际包含 `riskLevel`、`alerts`（每项含 `alertCode`/`alertMessage`/`severity`）、`auditRecordId`、`prescriptionHash` 四个字段及子字段结构。报告未完整描述 alerts 数组的子结构（`alertCode`/`alertMessage`/`severity`），可能导致执行者实现 warnResult DTO 时遗漏嵌套结构。
- **严重程度**：轻微
- **改进建议**：在证据描述中补充 alerts 子字段 `alertCode`/`alertMessage`/`severity` 的嵌套结构，或直接引用 OOD 行号而非仅描述字段名列表。

---

## 3. 深度与完整性

### 问题 4 [严重] — C04 修复建议未分析事务边界风险

- **所在位置**：C04 节
- **问题描述**：报告建议 C04 的修复为"为 triage()/selectDepartment()/saveTriageRecord() 添加 @Transactional"。此建议存在两个严重问题：
  (a) **长事务风险**：当前 `triage()` 方法在 session 修改（行 72-80）后执行 AI 调用 future.get()（行 87），该方法可能阻塞最长 8 秒（OOD §2.3 定义 `ai.timeout.triage=8s`）。若对整个 triage() 方法加 @Transactional，AI 调用期间将持有数据库连接，高并发时可能导致连接池耗尽。
  (b) **private 方法问题**：`saveTriageRecord()` 是 private 方法（行 181），Spring AOP 无法拦截自调用，在其上加 @Transactional 无效。
  (c) **OOD 原始意图不符**：OOD §3.1（line 453）明确要求"TriageRecordRepository.save() 写入 TriageRecord（在 @Transactional 事务内）"，事务边界应仅包围 save() 操作，而非整个 triage() 方法。
- **严重程度**：严重
- **改进建议**：修正为"将 saveTriageRecord() 中的持久化逻辑抽取为单独的事务方法（如 `@Transactional persistRecord()`），或使用 TransactionTemplate 编程式事务仅包围 TriageRecordRepository.save() 调用"。在报告中增加对事务边界与 AI 调用时序的约束说明。

### 问题 5 [中等] — C08 与 C22 的合并修复方案与现有 OOD 设计矛盾

- **所在位置**：C08 节，第 117-118 行
- **问题描述**：报告提出的"overwrite 下沉到 Service 实现层"方案与 OOD §3.1（line 469）已有设计矛盾。OOD 设计的方案是：EventListener 自行检查 finalDepartmentId 状态后决定"是否调用"selectDepartment，而非 Service 内部"根据调用来源"决策。报告未指出此矛盾，也未引用 OOD line 469 来对齐修复方案。
- **严重程度**：中等
- **改进建议**：对齐 OOD line 469 描述，修复方案改为：(a) selectDepartment 接口改为 3 参（去除 overwrite），Service 始终覆盖写入；(b) EventListener 调用前检查 finalDepartmentId，仅空时调用；(c) Controller 直接调用（无需传 overwrite）。移除"根据调用来源决定"的不可行描述。

### 问题 6 [中等] — 多修复项之间的耦合副作用未分析

- **所在位置**：C04（@Transactional）、E02（unique constraint 冲突）、C23（session 修改时序）三节
- **问题描述**：这三项修复之间存在相互约束，报告未进行副作用分析：
  - C04 加 @Transactional 与 E02（session 第二次分诊先查后改 INSERT→UPDATE）的交互：若事务边界过宽（@Transactional 在 triage()），E02 的查改操作在事务内执行，同一 sessionId 的并发请求在事务隔离级别下可能出现幻读——两个事务同时查到记录不存在→都执行 INSERT，后者的 unique 约束冲突仍可能发生。需要分析 REPEATABLE_READ 或序列化隔离级别是否足够，以及悲观锁的必要性。
  - C23 的 session 修改时序 + C04 的事务边界决定：AI 输入准备阶段的 session 修改在事务之外、AI 调用在事务之外、持久化在事务内。三者的时序和异常回滚影响需明确说明。
- **严重程度**：中等
- **改进建议**：在 C04 或报告总则中增加跨问题副作用分析章节，明确 (a) @Transactional 的边界位置；(b) 并发冲突场景下 E02 查改的原子性保障策略（如 `@Lock(PESSIMISTIC_WRITE)` 或 `select ... for update`）；(c) C23/AI 调用与事务边界的隔离关系。

---

## 4. 可操作性

### 问题 7 [一般] — C14 修复建议中检查与执行的时序不精确

- **所在位置**：C14 节
- **问题描述**：报告说"未判断 retryCount >= maxRetryCount 时将状态迁移到 EXPIRED"，但未说明判断时机。当前代码先递增 retryCount 后隐式保存，若修复仅在补偿逻辑末尾增加状态迁移判断，则在递增后→迁移前的窗口内若发生进程崩溃，retryCount 已递增但状态未迁移为 EXPIRED，下次启动补偿时会以递增后的 retryCount 继续尝试，超过 maxRetryCount 限制。
- **严重程度**：一般
- **改进建议**：明确建议的执行顺序为：(1) 补偿尝试前先判断 `retryCount >= maxRetryCount`；(2) 若已达上限则直接迁移 EXPIRED 不尝试补偿；(3) 若未达上限则执行补偿；(4) 递增 retryCount。按此顺序可避免超限后仍有补偿尝试的场景。

### 问题 8 [一般] — C23 修复建议中的时序说明仍较抽象

- **所在位置**：C23 节，第 164 行
- **问题描述**："将 AI 输入准备阶段的 session 修改与持久化路径解耦"的表述仍偏抽象。执行者需要更精确的时序描述：哪些 session 操作留在 AI 调用前（行 72-80 的 setChiefComplaint/setCorrectedChiefComplaint/setRoundCount）、哪些操作（correctedChiefComplaint 快照写入、aiFailCount 更新）需要在 saveTriageRecord 之后执行。
- **严重程度**：一般
- **改进建议**：补充精确时序表：`[1] 行 72-80 session 修改（保持原位）→ [2] 行 82-93 AI 调用 + 超时/降级 → [3] 行 95-108 处理 AI 结果 → [4] saveTriageRecord（事务内）→ [5] 行 140 session.setAiFailCount(0) → [6] session.setCorrectedChiefComplaint(aiData.getCorrectedChiefComplaint())（若隐式路径命中）`。

---

## 整体质量评价

报告整体质量较高，v3 修订全面吸收了第 1 轮迭代的 8 项反馈，核心诊断（真实性判定、根因分类、代码证据）准确且充分。主要不足集中在三个方面：**(a)** 修复建议的可操作性——C04 的事务边界存在实际编码陷阱，执行者若直接按字面建议实现会引入长事务风险，是最严重的问题；**(b)** 跨修复项的副作用分析缺失——C04+E02+C23 的约束耦合未识别；**(c)** 需求响应方面，P05/P09 的 OOD 文档修改方向未作为显式任务项分离。建议针对上述问题 1、4、5、6 进行修订后进入下一轮次。
