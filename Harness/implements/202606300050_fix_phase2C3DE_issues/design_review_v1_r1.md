# 设计审查报告（v1 r1）

## 审查结果
APPROVED

## 发现

### [轻微] C03 回写顺序与 OOD §3.1 "先 DB 后 内存" 策略不一致
设计在 `TriageServiceImpl.triage()` 的 AI 成功分支中先执行 `session.setCorrectedChiefComplaint(...)`（C03 回写），之后再调用 `saveTriageRecord()` 从 session 读取值写入 DB。OOD §3.1 明确要求"先写数据库再更新内存"——先通过 `TriageRecordRepository.save()` 写入 TriageRecord，事务提交成功后，再更新 DialogueSession。当前设计将 session 更新置于 DB 写入之前，若 DB 写入失败则 session 已进入不一致状态。建议考虑：`saveTriageRecord()` 直接从 `aiData` 读取 correctedChiefComplaint 写入 DB，将 session 更新延后至 save 之后执行，以对齐 OOD 策略。

### [轻微] toTriageResponse 签名变更未列出调用方
设计为 `toTriageResponse` 增加第三个参数 `DialogueSession session`，但未说明哪些调用方需要同步更新。建议在设计或实现备注中标注受影响的调用位置.

### [轻微] toAiTriageRequest 行为描述与代码不一致
设计的行为约束注明"仅在 session 中 cc 非空时携带，避免每次请求携带冗余 null 字段"，但代码片段为无条件 `aiRequest.setCorrectedChiefComplaint(session.getCorrectedChiefComplaint())`，未加 null 守卫。请统一行为描述与代码表达。

### [轻微] 新增数据库列未提及 DDL 迁移
TriageRecord 实体新增 `correctedChiefComplaint` 列（`@Column(columnDefinition = "TEXT")`），设计中未说明是否需要对应的 DDL/Liquibase/Flyway 迁移脚本。建议在实现前置条件中备注或确认当前开发环境使用 JPA ddl-auto。

## 修改要求
无 — 以上均为轻微问题，不影响设计可用性及编码实施。
