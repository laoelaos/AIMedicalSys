# 设计审查报告（v10 r5）

## 审查结果
REJECTED

## 发现

- **[严重]** `AuditRecord` 继承 `BaseEntity`（含 `@Id private Long id`），同时又声明 `auditId — Long, @Id @GeneratedValue(IDENTITY)`。JPA 禁止单继承层次中存在两个 `@Id` 字段，无法通过编译。所有现有实体（`TriageRecord` 等）均仅使用 BaseEntity 继承的 `id` 作为主键。修正方向：将 `auditId` 改为普通业务字段（非 `@Id`），使用继承的 `id` 作为 JPA 主键；或通过 `@AttributeOverride(name = "id", column = @Column(name = "audit_id"))` 重命名继承的 `id` 列。

- **[一般]** `submit()` 步③ `forceSubmit=false + WARN` 路径中仅写"与 `AuditRecord.originalPrescription` 做五字段比对"，未指明使用哪个 `AuditRecord`。步②已查询出最新 `isLatest=true` 的记录，应明确将步②查询结果传入步③作为比对基准。

- **[一般]** `revoke` 端点返回 `Result<Void>` 与 404/409 状态码不兼容。需要统一使用 `ResponseEntity` 或通过 `BusinessException` + `@ExceptionHandler` 处理非 200 状态码路径。

- **[轻微]** `AuditRecord` 缺少 `@Table(name = ...)` 标注，与项目中 `TriageRecord` 等实体明确声明表名的惯例不一致。

- **[轻微]** `DrugInteractionPair` 使用 `@Table(schema = "PHASE4_PRELOAD")` 依赖特定数据库对不存在 schema 的跳过行为，存在跨数据库兼容性风险。

## 修改要求（仅 REJECTED 时）

1. **[严重]** 修复 `AuditRecord.auditId` 与 BaseEntity 继承的 `id` 之间的 JPA `@Id` 冲突。推荐方案：移除 `auditId` 的 `@Id` 声明，仅使用继承的 `id` 作为主键（与 `TriageRecord` 模式一致），或使用 `@AttributeOverride` 将继承的 `id` 列映射到 `audit_id`。
2. **[一般]** `submit()` 步③ WARN 比对路径中，明确指明比对基准为步②查询到的 `isLatest=true` 的 `AuditRecord.originalPrescription`。
3. **[一般]** 为 `revoke` 端点指定统一的响应类型策略（`ResponseEntity` 或异常委托全局处理器）。
