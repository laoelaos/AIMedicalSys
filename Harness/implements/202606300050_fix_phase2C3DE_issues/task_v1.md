# 任务指令（v1）

## 动作
NEW

## 任务描述
R1: 修复 correctedChiefComplaint 数据链路（C01, C02, C19, A04, C03, C18, C23）

具体修改：
1. **TriageRecord.java** — 增加 `correctedChiefComplaint` 字段（String，可选），对应 OOD §3.1
2. **TriageRecordRepository.java** — 增加 `findTopBySessionIdOrderByTriageTimeDesc(String sessionId)` 查询方法
3. **TriageServiceImpl.java**:
   - `saveTriageRecord()` 中读取 `session.getCorrectedChiefComplaint()` 写入 TriageRecord.correctedChiefComplaint（C19）
   - `saveTriageRecord()` 中 catch JsonProcessingException 改为 WARN 日志（C18）
   - `triage()` 中 AI 返回后检测 ai-api TriageResponse.correctedChiefComplaint 非空时写入 DialogueSession.correctedChiefComplaint（C03 隐式路径）
4. **TriageConverter.java**:
   - `toAiTriageRequest()` 中读取 `session.getCorrectedChiefComplaint()` 设置到 ai-api TriageRequest（A04 显式透传路径）
   - `toTriageResponse()` 中检测 ai-api TriageResponse.correctedChiefComplaint 非空时写入 DialogueSession（C03 隐式路径）
5. **DialogueSession.java** — 确认 `correctedChiefComplaint` 字段存在（String，可选）
6. **DialogueSessionManager.java** — `restoreSession()` 中从数据库恢复时优先从 TriageRecord 读取 correctedChiefComplaint

## 选择理由
C01(P0)、C02(P0)、C19(P1)、A04(P1)、C03(P1) 属于 correctedChiefComplaint 数据流群组，需同步修复。C18(P2) 静默 catch→WARN 在此轮顺带修复。C23(P2) 为验收约束。

## 任务上下文
- OOD §3.1: TriageRecord 需 correctedChiefComplaint 快照字段
- OOD §3.1: findTopBySessionIdOrderByTriageTimeDesc 恢复查询
- OOD §3.1: 全量拼接策略、correctedChiefComplaint 替换优先级
- OOD §4.1: 会话持久化恢复路径

## 已有代码上下文
- TriageRecord.java:14-144 无 correctedChiefComplaint
- TriageRecordRepository.java:11-17 有 findBySessionId 但无 findTopBySessionIdOrderByTriageTimeDesc
- TriageServiceImpl.java:34-227 无 saveTriageRecord cc 写入、catch 静默
- TriageConverter.java:22-91 无 correctedChiefComplaint 读取/回写
- DialogueSession.java 含 correctedChiefComplaint 字段（已定义但未在 save 中使用）
- DialogueSessionManager.java:21-37 createSession/findOrCreate 基本完备

---

## 修订说明（v1 r3）
| 审查意见 | 修改措施 |
|---------|---------|
| C15/E01(P0) 完全未纳入计划 | 已补入 plan.md R2（与 C08+C09+C22 同轮）；R1 任务内容不受影响 |
| R7(A07/A11) 先于 R12(A09) 违反 A09→A07→A11 修复顺序 | 已将 A09 从 R12 前移至 R7，R1 不受影响 |
| 缺失 P2 可并行修复项说明 | 已在 plan.md 实施路线表后增加排期外说明 |
| 问题计数需核实 | plan.md 标题已改为 55 项（计入 C15/E01） |
| E02 UPDATE 逻辑在 R1 与 R3 之间分配不一致（审查 v1 r5） | 从 task_v1.md R1 任务描述中移除第 7 项（E02），E02 仅保留在 plan.md R3 中；R1 范围缩减为：TriageRecord 加字段、Repository 加查询方法、Converter 透传/回写、catch→WARN |
