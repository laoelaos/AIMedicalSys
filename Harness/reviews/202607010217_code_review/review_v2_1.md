# R2.1: prescription 模块规则引擎和提交流程深度审查

审查时间：2026-07-01

### 审查范围

- `rule/DefaultLocalRuleEngine.java`
- `rule/LocalRuleEngine.java`
- `rule/LocalRuleResult.java`
- `rule/AllergyCheckRule.java`
- `rule/ContraindicationCheckRule.java`
- `rule/DuplicateCheckRule.java`
- `rule/DosageLimitRule.java`
- `rule/SpecialPopulationDosageRule.java`
- `rule/DrugInteractionRule.java`
- `service/audit/impl/PrescriptionAuditServiceImpl.java`
- `service/audit/AuditRiskLevel.java`
- `service/assist/DosageThresholdService.java`
- `service/assist/DedupTaskScheduler.java`
- `converter/AuditConverter.java`
- `converter/AssistConverter.java`
- `context/PrescriptionDraftContext.java`
- `context/DosageAlert.java`
- `dto/audit/SubmitRequest.java` / `SubmitResponse.java` / `WarnResult.java` / `BlockResponse.java`
- `dto/audit/AuditResponse.java` / `AuditAlert.java` / `AuditIssue.java` / `AlertSeverity.java`
- `dto/audit/PrescriptionItem.java` / `PatientInfo.java` / `AllergyDetail.java`
- `entity/AuditRecord.java`
- `task/DraftContextCleanupTask.java`
- `task/SuggestionCleanupTask.java`

### 发现

#### [严重] DrugInteractionRule 为空实现，本地降级时药物相互作用检查完全缺失

- **位置**: `rule/DrugInteractionRule.java:12-13`
- **描述**: `DrugInteractionRule.check()` 始终返回 `new LocalRuleResult(RULE_ID, true, null, AuditRiskLevel.PASS)`，没有任何实际检查逻辑。当 AI 服务不可用触发本地降级时，药物相互作用维度的审查完全跳过，产生假阴性。
- **建议**: 实现基于 `DrugInteractionPair` 的交叉药物检查逻辑，或明确记录当前为 stub 并设定实现优先级。

#### [严重] DosageThresholdService 频率解析使用 Integer.parseInt 导致日剂量校验静默失效（T9 追踪）

- **位置**: `service/assist/DosageThresholdService.java:76-89`
- **描述**: 第 76 行 `Integer.parseInt(request.getFrequency())` 对 `frequency="tid"` / `"bid"` / `"qid"` 等临床常用频率文本会抛出 `NumberFormatException`，而第 88-89 行的空 catch 块静默吞噬异常，导致日剂量超限校验（第 78 行 `dailyDose.compareTo(matched.getDailyMax()) > 0`）永远不执行。
- **建议**: 增加常见频率文本到数值的映射表（如 "qd"→1, "bid"→2, "tid"→3, "qid"→4），无法映射时记录警告日志而非静默忽略。

#### [严重] AllergyCheckRule 和 AllergyDetail 跨模块依赖 patient 模块的 AllergySeverity 枚举（T8 追踪）

- **位置**: `rule/AllergyCheckRule.java:3`; `dto/audit/AllergyDetail.java:3`
- **描述**: `AllergyCheckRule` 和 `AllergyDetail` 直接导入 `com.aimedical.modules.patient.entity.AllergySeverity`，违反了 prescription 模块不应直接依赖 patient 实体层的架构约定。prescription DTO 中混入 patient 模块的实体类型（`AllergyDetail.severity` 字段类型为 `AllergySeverity`），导致强耦合。
- **建议**: 在 prescription 模块内定义独立的严重度枚举（如 `PrescriptionAllergySeverity`），通过 Converter 映射，消除跨模块实体依赖。

#### [严重] PrescriptionDraftContext unchecked cast 类型安全风险（T10 追踪）

- **位置**: `context/PrescriptionDraftContext.java:28-29`
- **描述**: `@SuppressWarnings("unchecked")` 下直接将 `Object` 强转为 `List<DosageAlert>`，无 `instanceof` 泛型类型校验。若 `DraftContextStore` 中存储的类型不是 `List<DosageAlert>`，将抛出 `ClassCastException` 且难以排查。
- **建议**: 执行类型擦除后的泛型安全检查，例如序列化为 JSON 后反序列化，或定义包装类型 `CriticalAlerts` 对 Store 做类型安全封装。

#### [严重] submit 方法中 prescriptionOrderId 使用 System.currentTimeMillis() 存在毫秒级冲突风险

- **位置**: `service/audit/impl/PrescriptionAuditServiceImpl.java:218, 259, 289, 298, 328`
- **描述**: 所有生成 `prescriptionOrderId` 的地方均使用 `"RX-" + System.currentTimeMillis()`。同一毫秒内的两个并发 submit 将产生相同 orderId，违反业务上对订单 ID 唯一性的要求。
- **建议**: 使用 `UUID.randomUUID()`、Snowflake 算法或数据库序列生成唯一订单号。

#### [严重] submit() 方法缺乏 prescriptionId 级别的并发提交防护

- **位置**: `service/audit/impl/PrescriptionAuditServiceImpl.java:147-195`
- **描述**: `submit()` 方法未使用悲观锁或分布式锁保护 `prescriptionId`。两个并发线程可同时通过 BLOCK 检查（第 166-180 行），且 PASS 路径（第 215-220 行）不保存 AuditRecord（无乐观锁），导致同一 prescription 产生多个成功提交结果。
- **建议**: 使用 `SELECT ... FOR UPDATE`（悲观写锁）或在业务层对 `prescriptionId` 加分布式锁。

#### [严重] hasNewAlerts 方法在正常路径中是死代码，且存在竞态条件

- **位置**: `service/audit/impl/PrescriptionAuditServiceImpl.java:152-164, 182-192, 502-506`
- **描述**: `hasNewAlerts(snapshot, current)` 在 snapshot=emptyList 且 current=emptyList 时逻辑：
  1. 第 152 行 `hasCritical = true` 时已在第 153 行 return，不执行第 164 行。
  2. 第 152 行 `hasCritical = false` 时，第 164 行设 `snapshotCriticalAlerts = emptyList`。
  3. 第 182 行重新查询 `currentAlerts`，其间没有代码修改 draft context，结果仍为空。
  4. `hasNewAlerts(emptyList, emptyList)` → `current.isEmpty()` → 返回 false。
  → 该检查始终无实际作用。此外，若并发线程在第 152 行和第 182 行之间写入了 critical alert，将产生一个意外的 BLOCK（竞态条件）。
- **建议**: 以原子方式（如锁或 CAS）完成：查询→判断→决定流程，或移除冗余的二次检查。

#### [严重] forceSubmit 路径未验证 prescriptionHash

- **位置**: `service/audit/impl/PrescriptionAuditServiceImpl.java:236-285`
- **描述**: WarnResult 虽然携带 `prescriptionHash` 返回给客户端（第 508-524, 526-541 行），但 `SubmitRequest` 中没有用于回传该 hash 的字段（`SubmitRequest.java:17` 只有 `auditRecordId`）。forceSubmit 仅检查 `auditRecordId` 和 `prescriptionsMatch`，不验证 hash，弱化了客户端确认处方未篡改的安全保障。
- **建议**: SubmitRequest 增加 `prescriptionHash` 字段，forceSubmit 时与数据库中的 JSON 重新计算的 hash 比对。

#### [一般] DosageLimitRule 无匹配标准时静默使用首个标准

- **位置**: `rule/DosageLimitRule.java:37`
- **描述**: `findBestMatch` 返回 null 时回退到 `standards.get(0)`，该标准可能与患者的年龄/体重完全无关，导致剂量阈值校验不准确。
- **建议**: 无匹配时应返回 WARN/BLOCK 级别的结果，提示"未找到匹配的剂量标准"，而非静默使用可能不相关的标准。

#### [一般] AllergyCheckRule 中 allergyHistory 自由文本匹配过于激进

- **位置**: `rule/AllergyCheckRule.java:56-58`
- **描述**: `allergyHistory.contains(allergen)` 在自由文本中做子串匹配，例如 "No allergy to penicillin" 会匹配 allergen="penicillin"，且直接返回 BLOCK 级别阻断。该方法无法区分否定表述和肯定表述。
- **建议**: 结构化存储过敏史（与 `allergyDetails` 一致），或使用 NLP 做否定检测，至少降级为 WARN 级别。

#### [一般] DraftContextCleanupTask 迭代与清理非原子性

- **位置**: `task/DraftContextCleanupTask.java:38-45`
- **描述**: 第 38 行迭代 `draftContextStore.keySet()` 和第 39-44 行操作 `writeTimestamps` 之间无同步机制。`draftContextStore` 和 `writeTimestamps` 可能因并发写入而不同步，导致已删除的 key 残留时间戳或刚写入的 key 被误清理。
- **建议**: 考虑在 `DraftContextStore` 中嵌入时间戳，或将 `writeTimestamps` 与 Store 操作绑定到同一个 `ConcurrentHashMap` 的原子方法中。

#### [一般] SuggestionCleanupTask 仅清理 COMPLETED/FAILED 条目，PENDING 条目永不清理

- **位置**: `task/SuggestionCleanupTask.java:42-47`
- **描述**: `isExpiredAndConsumed` 要求 `status` 为 COMPLETED 或 FAILED 且 `consumed=true`。如果某条建议卡在 PENDING 状态（如 AI 服务超时后未更新状态），该条目永远不会被清理，造成 Store 内存泄漏。
- **建议**: 增加超时兜底策略：若 PENDING 状态超过 N 小时，也应清理。

#### [一般] DedupTaskScheduler 中 candidateTaskId 写入时序存在不一致窗口

- **位置**: `service/assist/DedupTaskScheduler.java:39-40`
- **描述**: 第 38 行 `createIfNotExists(dedupKey, newResult)` 成功后，第 40 行才 `suggestionStore.put(candidateTaskId, newResult)`。若第 39-40 行间抛出异常（如 OOM），则 dedupKey 已占位但 candidateTaskId 映射缺失。后续 `schedule` 调用会返回该 taskId（第 28-29 行），但 `suggestionStore.get(taskId)` 返回 null。
- **建议**: 先 `put(candidateTaskId, newResult)` 再 `createIfNotExists(dedupKey, ...)`，或使用事务性操作。

#### [一般] AuditConverter.PatientInfo 映射遗漏 weight 字段，ai-api PatientInfo 缺失 weight

- **位置**: `converter/AuditConverter.java:77-92`; `ai-api/.../prescription/PatientInfo.java`
- **描述**: biz 层 `PatientInfo` 有 `weight` 字段，但 `AuditConverter.toAiPatientInfo()` 未映射体重；ai-api 的 `PatientInfo` DTO 也无 `weight` 属性。导致剂量相关 AI 审核缺少体重参考。
- **建议**: ai-api PatientInfo 增加 `weight` 字段，AuditConverter 补充映射。

#### [一般] PrescriptionCheckItem 映射遗漏 unit 字段

- **位置**: `converter/AuditConverter.java:66-75`; `PrescriptionCheckItem.java`
- **描述**: biz 层 `PrescriptionItem` 有 `unit` 字段，但 `PrescriptionCheckItem` 未映射，且 ai-api DTO 也无 `unit` 属性。AI 审核无法获知剂量单位。
- **建议**: PrescriptionCheckItem 增加 `unit` 字段，AuditConverter 补充映射。

#### [一般] WARN 路径中 prescriptionHash 仅生成返回但不在服务端验证

- **位置**: `service/audit/impl/PrescriptionAuditServiceImpl.java:508-524, 526-541`
- **描述**: `buildWarnResultFromRecord` 和 `buildWarnResultFromAuditResponse` 均计算 SHA-256 hash 返回给客户端。但后端没有任何接口或逻辑通过该 hash 验证客户端提交的处方一致性，`forceSubmit` 仅使用 `prescriptionsMatch`（字段级比较）。
- **建议**: SubmitRequest 增加 `prescriptionHash` 字段，forceSubmit 时与服务端计算的 hash 比对。

#### [一般] forceSubmit 路径中 findByPrescriptionOrderId 操作在 orderId 刚生成时必然为空

- **位置**: `service/audit/impl/PrescriptionAuditServiceImpl.java:264-271`
- **描述**: `orderId = "RX-" + System.currentTimeMillis()`（第 259 行），然后第 264-265 行立即按新 orderId 查询 `findByPrescriptionOrderIdAndIsLatestTrue(orderId)`。因为 order 未被其他记录使用过，结果必然为空，第 269-271 行的 `saveAll` 和 `setLatest(false)` 是空操作。这段代码可能是从其他流程复制的残余逻辑。
- **建议**: 移除第 264-271 行（对于新订单无需将其他记录的 isLatest 置 false），或澄清设计意图并补充注释。

#### [轻微] AllergyCheckRule 每次调用 new ObjectMapper()

- **位置**: `rule/AllergyCheckRule.java:72`
- **描述**: `parseAllergens()` 方法中每次调用 `new com.fasterxml.jackson.databind.ObjectMapper()`，未复用单例。同理 `ContraindicationCheckRule.java:61` 和 `DuplicateCheckRule.java:60`。频繁创建 ObjectMapper 实例存在轻微性能开销。
- **建议**: 注入或定义静态共享的 ObjectMapper 实例。

#### [轻微] DosageAlert 是可变的但用于 HashSet.containsAll 比较

- **位置**: `context/DosageAlert.java:36-49`; `PrescriptionAuditServiceImpl.java:505`
- **描述**: `DosageAlert` 重写了 `equals`/`hashCode` 但所有 setter 均为公开的。在 `hasNewAlerts` 中使用 `HashSet.containsAll`，若任何 DosageAlert 在被加入 Set 后被修改，则 `containsAll` 行为未定义。
- **建议**: 将 DosageAlert 设计为不可变（final 字段+构造器初始化），或不在 HashSet 中使用。

#### [轻微] AssistConverter 警告类型映射默认值可能误导

- **位置**: `converter/AssistConverter.java:114, 123, 132`
- **描述**: `mapDoseWarningType` 默认返回 `OVER_SINGLE_DOSE`，`mapDosageAlertLevel` 默认返回 `INFO`，`mapAllergyWarningSeverity` 默认返回 `INFO`。若 AI 侧新增枚举值，将被静默映射为不正确的默认值。
- **建议**: 无法映射时记录日志，或抛出可识别的异常供上层处理。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 9 |
| 一般 | 8 |
| 轻微 | 3 |

### 总评

本轮深度审查发现 9 个严重问题、8 个一般问题、3 个轻微问题。严重问题集中在：**DrugInteractionRule 空实现导致本地降级时药物相互作用检查完全缺失**、**DosageThresholdService 频率解析静默失败导致日剂量校验失效**、**submit 流程中 prescriptionOrderId 的并发冲突风险**、**submit 方法缺乏 prescriptionId 级别的并发防护**、以及 **AllergyCheckRule 和 AllergyDetail 仍未解决的跨模块依赖问题**（T8）。**hasNewAlerts 逻辑形同虚设** 作为一个严重的逻辑冗余问题需要优先处理。此外，AuditConverter 和 ai-api DTO 之间存在 `weight` 和 `unit` 字段的映射遗漏，影响 AI 审核的输入完整性。建议对严重问题逐条修复后进入第三轮回归审查。
