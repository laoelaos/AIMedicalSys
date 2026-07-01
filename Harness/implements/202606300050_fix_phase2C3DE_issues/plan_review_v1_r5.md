# 计划审查报告（v1 r5）

## 审查结果
REJECTED

## 发现

### [一般] R1 task_v1.md 与 plan.md 之间 E02 归属不一致

- **问题**：task_v1.md 第 7 项明确将 E02 UPDATE 逻辑（`saveTriageRecord()` 中先查 `findBySessionId` 然后执行 update）纳入 R1 修复范围。
- **plan.md 分配**：R3 明确将 E02 与 C04（事务边界）、S04（并发控制）、C20（putIfAbsent）编为一组，且 R3 实施说明写着"先 findBySessionId 存在则 update"——同时要求配合 `TransactionTemplate` 编程事务 + `@Lock(PESSIMISTIC_WRITE)`。R1 描述中从未提及 E02 或 UPDATE 逻辑。
- **为什么是问题**：
  1. 如果实现者按 task_v1 在 R1 实现 E02，则 UPDATE 操作将不带事务边界（TransactionTemplate）和悲观锁（`@Lock(PESSIMISTIC_WRITE)`），在并发场景下存在数据竞争风险——这正是 R3 要解决的问题。
  2. 若 R1 先行实现了 update，到 R3 时要么需要重构/覆盖 R1 的代码（重复工作），要么跳过 update 逻辑仅补事务+锁（但 R1 的 update 实现可能假设了不同的上下文）。
  3. Plan 本身在耦合分析（§分组耦合说明）中明确将 E02 归入"事务+并发群组（R3）"，与 R1 的"correctedChiefComplaint 数据流"群组完全分离。task_v1 的分配违背了 plan 自身的耦合分析结论。
- **期望修正方向**：从 task_v1.md 第 7 项移除 E02 UPDATE 逻辑（`findBySessionId` + update），将其约束在 R3 范围内。R1 只做：TriageRecord 加字段、Repository 加查询、Converter 透传/回写、catch→WARN——与 plan.md R1 描述严格一致。
